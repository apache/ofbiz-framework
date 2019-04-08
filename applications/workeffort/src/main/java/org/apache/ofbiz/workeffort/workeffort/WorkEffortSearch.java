/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.workeffort.workeffort;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.KeywordSearchUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;



/**
 *  Utilities for WorkEffort search based on various constraints including assocs, features and keywords.
 *
 *  Search:
 *      WorkEffort fields: workEffortTypeId,workEffortPurposeTypeId,scopeEnumId, ??others
 *      WorkEffortKeyword - keyword search
 *      WorkEffortAssoc.workEffortIdTo,workEffortIdFrom,workEffortAssocTypeId
 *          Sub-tasks: WorkEffortAssoc.workEffortIdTo,workEffortIdFrom,workEffortAssocTypeId=WORK_EFF_BREAKDOWN for sub-tasks OR: (specific assoc and all sub-tasks)
 *          Sub-tasks: WorkEffort.workEffortParentId tree
 *      WorkEffortGoodStandard.productId
 *      WorkEffortPartyAssignment.partyId,roleTypeId
 *  Planned for later:
 *      WorkEffortFixedAssetAssign.fixedAssetId
 *      WorkEffortContent.contentId,workEffortContentTypeId
 *      WorkEffortBilling.invoiceId,invoiceItemSeqId
 *      CommunicationEventWorkEff.communicationEventId
 *      TimeEntry.partyId,rateTypeId,timesheetId,invoiceId,invoiceItemSeqId
 */
public class WorkEffortSearch {

    public static final String module = WorkEffortSearch.class.getName();
    public static final String resource = "WorkEffortUiLabels";

    public static ArrayList<String> searchWorkEfforts(List<? extends WorkEffortSearchConstraint> workEffortSearchConstraintList, ResultSortOrder resultSortOrder, Delegator delegator, String visitId) {
        WorkEffortSearchContext workEffortSearchContext = new WorkEffortSearchContext(delegator, visitId);

        workEffortSearchContext.addWorkEffortSearchConstraints(workEffortSearchConstraintList);
        workEffortSearchContext.setResultSortOrder(resultSortOrder);

        ArrayList<String> workEffortIds = workEffortSearchContext.doSearch();
        return workEffortIds;
    }

    public static void getAllSubWorkEffortIds(String workEffortId, Set<String> workEffortIdSet, Delegator delegator, Timestamp nowTimestamp) {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // first make sure the current id is in the Set
        workEffortIdSet.add(workEffortId);

        // now find all sub-categories, filtered by effective dates, and call this routine for them
        try {
            // Find WorkEffortAssoc, workEffortAssocTypeId=WORK_EFF_BREAKDOWN
            List<GenericValue> workEffortAssocList = EntityQuery.use(delegator).from("WorkEffortAssoc").where("workEffortIdFrom", workEffortId, "workEffortAssocTypeId", "WORK_EFF_BREAKDOWN").cache(true).queryList();
            for (GenericValue workEffortAssoc: workEffortAssocList) {
                String subWorkEffortId = workEffortAssoc.getString("workEffortIdTo");
                if (workEffortIdSet.contains(subWorkEffortId)) {
                    // if this category has already been traversed, no use doing it again; this will also avoid infinite loops
                    continue;
                }

                // do the date filtering in the loop to avoid looping through the list twice
                if (EntityUtil.isValueActive(workEffortAssoc, nowTimestamp)) {
                    getAllSubWorkEffortIds(subWorkEffortId, workEffortIdSet, delegator, nowTimestamp);
                }
            }

            // Find WorkEffort where current workEffortId = workEffortParentId; only select minimal fields to keep the size low
            List<GenericValue> childWorkEffortList = EntityQuery.use(delegator).select("workEffortId", "workEffortParentId").from("WorkEffort").where("workEffortParentId", workEffortId).cache(true).queryList();
            for (GenericValue childWorkEffort: childWorkEffortList) {
                String subWorkEffortId = childWorkEffort.getString("workEffortId");
                if (workEffortIdSet.contains(subWorkEffortId)) {
                    // if this category has already been traversed, no use doing it again; this will also avoid infinite loops
                    continue;
                }

                // do the date filtering in the loop to avoid looping through the list twice
                getAllSubWorkEffortIds(subWorkEffortId, workEffortIdSet, delegator, nowTimestamp);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding sub-categories for workEffort search", module);
        }
    }

    public static class WorkEffortSearchContext {
        public int index = 1;
        public List<EntityCondition> entityConditionList = new LinkedList<EntityCondition>();
        public List<String> orderByList = new LinkedList<String>();
        public List<String> fieldsToSelect = UtilMisc.toList("workEffortId");
        public DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        public boolean workEffortIdGroupBy = false;
        public boolean includedKeywordSearch = false;
        public Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        public List<Set<String>> keywordFixedOrSetAndList = new LinkedList<Set<String>>();
        public Set<String> orKeywordFixedSet = new HashSet<String>();
        public Set<String> andKeywordFixedSet = new HashSet<String>();
        public List<GenericValue> workEffortSearchConstraintList = new LinkedList<GenericValue>();
        public ResultSortOrder resultSortOrder = null;
        public Integer resultOffset = null;
        public Integer maxResults = null;
        protected Delegator delegator = null;
        protected String visitId = null;
        protected Integer totalResults = null;

        public WorkEffortSearchContext(Delegator delegator, String visitId) {
            this.delegator = delegator;
            this.visitId = visitId;
            dynamicViewEntity.addMemberEntity("WEFF", "WorkEffort");
        }

        public Delegator getDelegator() {
            return this.delegator;
        }

        public void addWorkEffortSearchConstraints(List<? extends WorkEffortSearchConstraint> workEffortSearchConstraintList) {
            // Go through the constraints and add them in
            for (WorkEffortSearchConstraint constraint: workEffortSearchConstraintList) {
                constraint.addConstraint(this);
            }
        }

        public void setResultSortOrder(ResultSortOrder resultSortOrder) {
            this.resultSortOrder = resultSortOrder;
        }

        public void setResultOffset(Integer resultOffset) {
            this.resultOffset = resultOffset;
        }

        public void setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
        }

        public Integer getTotalResults() {
            return this.totalResults;
        }

        public ArrayList<String> doSearch() {
            long startMillis = System.currentTimeMillis();

            // do the query
            EntityListIterator eli = this.doQuery(delegator);
            ArrayList<String> workEffortIds = this.makeWorkEffortIdList(eli);
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error closing WorkEffortSearch EntityListIterator");
                }
            }

            long endMillis = System.currentTimeMillis();
            double totalSeconds = ((double)endMillis - (double)startMillis)/1000.0;

            // store info about results in the database, attached to the user's visitId, if specified
            this.saveSearchResultInfo(Long.valueOf(workEffortIds.size()), Double.valueOf(totalSeconds));

            return workEffortIds;
        }

        public void finishKeywordConstraints() {
            if (orKeywordFixedSet.size() == 0 && andKeywordFixedSet.size() == 0 && keywordFixedOrSetAndList.size() == 0) {
                return;
            }

            // we know we have a keyword search to do, so keep track of that now...
            this.includedKeywordSearch = true;

            // if there is anything in the orKeywordFixedSet add it to the keywordFixedOrSetAndList
            if (orKeywordFixedSet.size() > 0) {
                // put in keywordFixedOrSetAndList to process with other or lists where at least one is required
                keywordFixedOrSetAndList.add(orKeywordFixedSet);
            }

            // remove all or sets from the or set and list where the or set is size 1 and put them in the and list
            Iterator<Set<String>> keywordFixedOrSetAndTestIter = keywordFixedOrSetAndList.iterator();
            while (keywordFixedOrSetAndTestIter.hasNext()) {
                Set<String> keywordFixedOrSet = keywordFixedOrSetAndTestIter.next();
                if (keywordFixedOrSet.size() == 0) {
                    keywordFixedOrSetAndTestIter.remove();
                } else if (keywordFixedOrSet.size() == 1) {
                    // treat it as just another and
                    andKeywordFixedSet.add(keywordFixedOrSet.iterator().next());
                    keywordFixedOrSetAndTestIter.remove();
                }
            }

            boolean doingBothAndOr = (keywordFixedOrSetAndList.size() > 1) || (keywordFixedOrSetAndList.size() > 0 && andKeywordFixedSet.size() > 0);

            Debug.logInfo("Finished initial setup of keywords, doingBothAndOr=" + doingBothAndOr + ", andKeywordFixedSet=" + andKeywordFixedSet + "\n keywordFixedOrSetAndList=" + keywordFixedOrSetAndList, module);

            ComplexAlias relevancyComplexAlias = new ComplexAlias("+");
            if (andKeywordFixedSet.size() > 0) {
                // add up the relevancyWeight fields from all keyword member entities for a total to sort by

                for (String keyword: andKeywordFixedSet) {
                    // make index based values and increment
                    String entityAlias = "PK" + index;
                    String prefix = "pk" + index;
                    index++;

                    dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
                    entityConditionList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, null));
                }

                //TODO: find out why Oracle and other dbs don't like the query resulting from this and fix: workEffortIdGroupBy = true;

                if (!doingBothAndOr) {
                    dynamicViewEntity.addAlias(null, "totalRelevancy", null, null, null, null, null, relevancyComplexAlias);
                }
            }
            if (keywordFixedOrSetAndList.size() > 0) {
                for (Set<String> keywordFixedOrSet: keywordFixedOrSetAndList) {
                    // make index based values and increment
                    String entityAlias = "PK" + index;
                    String prefix = "pk" + index;
                    index++;

                    dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
                    List<EntityExpr> keywordOrList = new LinkedList<EntityExpr>();
                    for (String keyword: keywordFixedOrSet) {
                        keywordOrList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));
                    }
                    entityConditionList.add(EntityCondition.makeCondition(keywordOrList, EntityOperator.OR));

                    workEffortIdGroupBy = true;

                    if (doingBothAndOr) {
                        relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, "sum"));
                    } else {
                        dynamicViewEntity.addAlias(entityAlias, "totalRelevancy", "relevancyWeight", null, null, null, "sum");
                    }
                }
            }

            if (doingBothAndOr) {
                dynamicViewEntity.addAlias(null, "totalRelevancy", null, null, null, null, null, relevancyComplexAlias);
            }
        }

        public EntityListIterator doQuery(Delegator delegator) {
            // handle the now assembled or and and keyword fixed lists
            this.finishKeywordConstraints();

            if (resultSortOrder != null) {
                resultSortOrder.setSortOrder(this);
            }
            dynamicViewEntity.addAlias("WEFF", "workEffortId", null, null, null, Boolean.valueOf(workEffortIdGroupBy), null);

            EntityListIterator eli = null;
            try {
                int maxRows = 0;
                if (maxResults != null) {
                    maxRows = maxResults;
                }
                eli = EntityQuery.use(delegator).select(UtilMisc.toSet(fieldsToSelect))
                        .from(dynamicViewEntity)
                        .where(entityConditionList)
                        .orderBy(orderByList)
                        .distinct()
                        .cursorScrollInsensitive()
                        .maxRows(maxRows)
                        .queryIterator();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error in workEffort search", module);
                return null;
            }

            return eli;
        }

        public ArrayList<String> makeWorkEffortIdList(EntityListIterator eli) {
            ArrayList<String> workEffortIds = new ArrayList<String>(maxResults == null ? 100 : maxResults.intValue());
            if (eli == null) {
                Debug.logWarning("The eli is null, returning zero results", module);
                return workEffortIds;
            }

            try {
                boolean hasResults = false;
                Object initialResult = null;

                initialResult = eli.next();
                if (initialResult != null) {
                    hasResults = true;
                }
                if (resultOffset != null && resultOffset.intValue() > 1) {
                    if (Debug.infoOn()) Debug.logInfo("Before relative, current index=" + eli.currentIndex(), module);
                    hasResults = eli.relative(resultOffset.intValue() - 1);
                    initialResult = null;
                }

                // get the first as the current one
                GenericValue searchResult = null;
                if (hasResults) {
                    if (initialResult != null) {
                        searchResult = (GenericValue) initialResult;
                    } else {
                        searchResult = eli.currentGenericValue();
                    }
                }

                if (searchResult == null) {
                    // nothing to get...
                    int failTotal = 0;
                    if (this.resultOffset != null) {
                        failTotal = this.resultOffset.intValue() - 1;
                    }
                    this.totalResults = Integer.valueOf(failTotal);
                    return workEffortIds;
                }


                // init numRetreived to one since we have already grabbed the initial one
                int numRetreived = 1;
                int duplicatesFound = 0;

                Set<String> workEffortIdSet = new HashSet<String>();

                workEffortIds.add(searchResult.getString("workEffortId"));
                workEffortIdSet.add(searchResult.getString("workEffortId"));

                while (((searchResult = eli.next()) != null) && (maxResults == null || numRetreived < maxResults.intValue())) {
                    String workEffortId = searchResult.getString("workEffortId");
                    if (!workEffortIdSet.contains(workEffortId)) {
                        workEffortIds.add(workEffortId);
                        workEffortIdSet.add(workEffortId);
                        numRetreived++;
                    } else {
                        duplicatesFound++;
                    }
                }

                if (searchResult != null) {
                    this.totalResults = eli.getResultsSizeAfterPartialList();
                }
                if (this.totalResults == null || this.totalResults.intValue() == 0) {
                    int total = numRetreived;
                    if (this.resultOffset != null) {
                        total += (this.resultOffset.intValue() - 1);
                    }
                    this.totalResults = Integer.valueOf(total);
                }

                Debug.logInfo("Got search values, numRetreived=" + numRetreived + ", totalResults=" + totalResults + ", maxResults=" + maxResults + ", resultOffset=" + resultOffset + ", duplicatesFound(in the current results)=" + duplicatesFound, module);

            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting results from the workEffort search query", module);
            }
            return workEffortIds;
        }

        public void saveSearchResultInfo(Long numResults, Double secondsTotal) {
            // uses entities: WorkEffortSearchResult and WorkEffortSearchConstraint

            try {
                // make sure this is in a transaction
                boolean beganTransaction = TransactionUtil.begin();

                try {

                    GenericValue workEffortSearchResult = delegator.makeValue("WorkEffortSearchResult");
                    String workEffortSearchResultId = delegator.getNextSeqId("WorkEffortSearchResult");

                    workEffortSearchResult.set("workEffortSearchResultId", workEffortSearchResultId);
                    workEffortSearchResult.set("visitId", this.visitId);
                    if (this.resultSortOrder != null) {
                        workEffortSearchResult.set("orderByName", this.resultSortOrder.getOrderName());
                        workEffortSearchResult.set("isAscending", this.resultSortOrder.isAscending() ? "Y" : "N");
                    }
                    workEffortSearchResult.set("numResults", numResults);
                    workEffortSearchResult.set("secondsTotal", secondsTotal);
                    workEffortSearchResult.set("searchDate", nowTimestamp);
                    workEffortSearchResult.create();

                    int seqId = 1;
                    for (GenericValue workEffortSearchConstraint: workEffortSearchConstraintList) {
                        workEffortSearchConstraint.set("workEffortSearchResultId", workEffortSearchResultId);
                        workEffortSearchConstraint.set("constraintSeqId", Integer.toString(seqId));
                        workEffortSearchConstraint.create();
                        seqId++;
                    }

                    TransactionUtil.commit(beganTransaction);
                } catch (GenericEntityException e1) {
                    String errMsg = "Error saving workEffort search result info/stats";
                    Debug.logError(e1, errMsg, module);
                    TransactionUtil.rollback(beganTransaction, errMsg, e1);
                }
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error saving workEffort search result info/stats", module);
            }
        }
    }

    // ======================================================================
    // Search Constraint Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class WorkEffortSearchConstraint implements java.io.Serializable {
        public WorkEffortSearchConstraint() { }

        public abstract void addConstraint(WorkEffortSearchContext workEffortSearchContext);
        /** pretty print for log messages and even UI stuff */
        public abstract String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale);
    }


    @SuppressWarnings("serial")
    public static class WorkEffortAssocConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "WorkEffortAssoc";
        protected String workEffortId;
        protected String workEffortAssocTypeId;
        protected boolean includeSubWorkEfforts;

        public WorkEffortAssocConstraint(String workEffortId, String workEffortAssocTypeId, boolean includeSubWorkEfforts) {
            this.workEffortId = workEffortId;
            this.workEffortAssocTypeId = workEffortAssocTypeId;
            this.includeSubWorkEfforts = includeSubWorkEfforts;
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            Set<String> workEffortIdSet = new HashSet<String>();
            if (includeSubWorkEfforts) {
                // find all sub-categories recursively, make a Set of workEffortId
                WorkEffortSearch.getAllSubWorkEffortIds(workEffortId, workEffortIdSet, workEffortSearchContext.getDelegator(), workEffortSearchContext.nowTimestamp);
            } else {
                workEffortIdSet.add(workEffortId);
            }

            // allow assoc from or to the current WE and the workEffortId on this constraint

            // make index based values and increment
            String entityAlias;
            String prefix;

            // do workEffortId = workEffortIdFrom, workEffortIdTo IN workEffortIdSet
            entityAlias = "WFA" + workEffortSearchContext.index;
            prefix = "wfa" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortAssoc");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortIdFrom", "workEffortIdFrom", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortIdTo", "workEffortIdTo", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortAssocTypeId", "workEffortAssocTypeId", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.TRUE, ModelKeyMap.makeKeyMapList("workEffortId","workEffortIdFrom"));

            List<EntityExpr> assocConditionFromTo = new LinkedList<EntityExpr>();
            assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "WorkEffortIdTo", EntityOperator.IN, workEffortIdSet));
            if (UtilValidate.isNotEmpty(workEffortAssocTypeId)) {
                assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "WorkEffortAssocTypeId", EntityOperator.EQUALS, workEffortAssocTypeId));
            }
            assocConditionFromTo.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

            // do workEffortId = workEffortIdTo, workEffortIdFrom IN workEffortIdSet
            entityAlias = "WFA" + workEffortSearchContext.index;
            prefix = "wfa" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortAssoc");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortIdFrom", "workEffortIdFrom", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortIdTo", "workEffortIdTo", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "WorkEffortAssocTypeId", "workEffortAssocTypeId", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.TRUE, ModelKeyMap.makeKeyMapList("workEffortId","workEffortIdTo"));

            List<EntityExpr> assocConditionToFrom = new LinkedList<EntityExpr>();
            assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "WorkEffortIdFrom", EntityOperator.IN, workEffortIdSet));
            if (UtilValidate.isNotEmpty(workEffortAssocTypeId)) {
                assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "WorkEffortAssocTypeId", EntityOperator.EQUALS, workEffortAssocTypeId));
            }
            assocConditionToFrom.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

            // now create and add the combined constraint
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(assocConditionFromTo, EntityOperator.AND), EntityOperator.OR, EntityCondition.makeCondition(assocConditionToFrom, EntityOperator.AND)));


            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.workEffortId + "," + this.workEffortAssocTypeId, "includeSubWorkEfforts", this.includeSubWorkEfforts ? "Y" : "N")));
        }


        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue workEffort = null;
            GenericValue workEffortAssocType = null;
            try {
                workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", this.workEffortId).cache().queryOne();
                workEffortAssocType = EntityQuery.use(delegator).from("WorkEffortAssocType").where("workEffortAssocTypeId", this.workEffortAssocTypeId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up WorkEffortAssocConstraint pretty print info: " + e.toString(), module);
            }

            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortAssoc", locale) + ": ");
            if (workEffort != null) {
                ppBuf.append(workEffort.getString("workEffortName"));
            }
            if (workEffort == null || detailed) {
                ppBuf.append(" [");
                ppBuf.append(workEffortId);
                ppBuf.append("]");
            }
            if (UtilValidate.isNotEmpty(this.workEffortAssocTypeId)) {
                if (workEffortAssocType != null) {
                    ppBuf.append(workEffortAssocType.getString("description"));
                }
                if (workEffortAssocType == null || detailed) {
                    ppBuf.append(" [");
                    ppBuf.append(workEffortAssocTypeId);
                    ppBuf.append("]");
                }
            }
            if (this.includeSubWorkEfforts) {
                ppBuf.append(" (").append(UtilProperties.getMessage(resource, "WorkEffortIncludeAllSubWorkEfforts", locale)).append(")");
            }
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof WorkEffortAssocConstraint) {
                WorkEffortAssocConstraint that = (WorkEffortAssocConstraint) psc;
                if (this.includeSubWorkEfforts != that.includeSubWorkEfforts) {
                    return false;
                }
                if (this.workEffortId == null) {
                    if (that.workEffortId != null) {
                        return false;
                    }
                } else {
                    if (!this.workEffortId.equals(that.workEffortId)) {
                        return false;
                    }
                }
                if (this.workEffortAssocTypeId == null) {
                    if (that.workEffortAssocTypeId != null) {
                        return false;
                    }
                } else {
                    if (!this.workEffortAssocTypeId.equals(that.workEffortAssocTypeId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }
    
    @SuppressWarnings("serial")
    public static class WorkEffortReviewConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "WorkEffortReview";
        protected String reviewTextString;

        public WorkEffortReviewConstraint(String reviewTextString) {
            this.reviewTextString = reviewTextString;
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            String entityAlias = "WFR" + workEffortSearchContext.index;
            String prefix = "wfr" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortReview");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ReviewText", "reviewText", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(prefix + "ReviewText"), EntityOperator.LIKE, EntityFunction.UPPER("%" + reviewTextString + "%")));
            Map<String, String> valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.reviewTextString);
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", valueMap));
        }


        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortReviews", locale) + ": \"");
            ppBuf.append(this.reviewTextString).append("\", ").append(UtilProperties.getMessage(resource, "WorkEffortKeywordWhere", locale)).append(" ");
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof WorkEffortReviewConstraint) {
                WorkEffortReviewConstraint that = (WorkEffortReviewConstraint) psc;
                if (this.reviewTextString == null) {
                    if (that.reviewTextString != null) {
                        return false;
                    }
                } else {
                    if (!this.reviewTextString.equals(that.reviewTextString)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("serial")
    public static class PartyAssignmentConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "PartyAssignment";
        protected String partyId;
        protected String roleTypeId;

        public PartyAssignmentConstraint(String partyId, String roleTypeId) {
            this.partyId = partyId;
            this.roleTypeId = roleTypeId;
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            // make index based values and increment
            String entityAlias = "WEPA" + workEffortSearchContext.index;
            String prefix = "wepa" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortPartyAssignment");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "PartyId", "partyId", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "RoleTypeId", "roleTypeId", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));

            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "PartyId", EntityOperator.EQUALS, partyId));
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));
            if (UtilValidate.isNotEmpty(this.roleTypeId)) {
                workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "RoleTypeId", EntityOperator.EQUALS, roleTypeId));
            }

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.partyId + "," + this.roleTypeId)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue partyNameView = null;
            GenericValue roleType = null;
            try {
                partyNameView = EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyId).cache().queryOne();
                roleType = EntityQuery.use(delegator).from("RoleType").where("roleTypeId", roleTypeId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding PartyAssignmentConstraint information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append("WorkEffort Assignment: ");
            if (partyNameView != null) {
                if (UtilValidate.isNotEmpty(partyNameView.getString("firstName"))) {
                    ppBuf.append(partyNameView.getString("firstName"));
                    ppBuf.append(" ");
                }
                if (UtilValidate.isNotEmpty(partyNameView.getString("middleName"))) {
                    ppBuf.append(partyNameView.getString("middleName"));
                    ppBuf.append(" ");
                }
                if (UtilValidate.isNotEmpty(partyNameView.getString("lastName"))) {
                    ppBuf.append(partyNameView.getString("lastName"));
                }
                if (UtilValidate.isNotEmpty(partyNameView.getString("groupName"))) {
                    ppBuf.append(partyNameView.getString("groupName"));
                }
            } else {
                ppBuf.append("[");
                ppBuf.append(this.partyId);
                ppBuf.append("] ");
            }

            if (roleType != null) {
                ppBuf.append(roleType.getString("description"));
            } else {
                if (UtilValidate.isNotEmpty(this.roleTypeId)) {
                    ppBuf.append("[");
                    ppBuf.append(this.roleTypeId);
                    ppBuf.append("]");
                }
            }
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof PartyAssignmentConstraint) {
                PartyAssignmentConstraint that = (PartyAssignmentConstraint) psc;
                if (this.partyId == null) {
                    if (that.partyId != null) {
                        return false;
                    }
                } else {
                    if (!this.partyId.equals(that.partyId)) {
                        return false;
                    }
                }
                if (this.roleTypeId == null) {
                    if (that.roleTypeId != null) {
                        return false;
                    }
                } else {
                    if (!this.roleTypeId.equals(that.roleTypeId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("serial")
    public static class ProductSetConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "ProductSet";
        protected Set<String> productIdSet;

        public ProductSetConstraint(Collection<String> productIdSet) {
            this.productIdSet = UtilMisc.makeSetWritable(productIdSet);
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            // make index based values and increment
            String entityAlias = "WEGS" + workEffortSearchContext.index;
            String prefix = "wegs" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortGoodStandard");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductId", "productId", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));

            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductId", EntityOperator.IN, productIdSet));
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            workEffortSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            StringBuilder productIdInfo = new StringBuilder();
            Iterator<String> productIdIter = this.productIdSet.iterator();
            while (productIdIter.hasNext()) {
                String productId = productIdIter.next();
                productIdInfo.append(productId);
                if (productIdIter.hasNext()) {
                    productIdInfo.append(",");
                }
            }

            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", productIdInfo.toString())));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder infoOut = new StringBuilder();
            try {
                Iterator<String> productIdIter = this.productIdSet.iterator();
                while (productIdIter.hasNext()) {
                    String productId = productIdIter.next();
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        infoOut.append("[");
                        infoOut.append(productId);
                        infoOut.append("]");
                    } else {
                        infoOut.append(product.getString("productName"));
                    }

                    if (productIdIter.hasNext()) {
                        infoOut.append(", ");
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductSetConstraint information for constraint pretty print", module);
            }

            return infoOut.toString();
        }

        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof ProductSetConstraint) {
                ProductSetConstraint that = (ProductSetConstraint) psc;
                if (this.productIdSet == null) {
                    if (that.productIdSet != null) {
                        return false;
                    }
                } else {
                    if (!this.productIdSet.equals(that.productIdSet)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("serial")
    public static class KeywordConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "Keyword";
        protected String keywordsString;
        protected boolean anyPrefix;
        protected boolean anySuffix;
        protected boolean isAnd;
        protected boolean removeStems;

        public KeywordConstraint(String keywordsString, boolean anyPrefix, boolean anySuffix, Boolean removeStems, boolean isAnd) {
            this.keywordsString = keywordsString;
            this.anyPrefix = anyPrefix;
            this.anySuffix = anySuffix;
            this.isAnd = isAnd;
            if (removeStems != null) {
                this.removeStems = removeStems.booleanValue();
            } else {
                this.removeStems = UtilProperties.propertyValueEquals("keywordsearch", "remove.stems", "true");
            }
        }

        public Set<String> makeFullKeywordSet(Delegator delegator) {
            Set<String> keywordSet = KeywordSearchUtil.makeKeywordSet(this.keywordsString, null, true);
            Set<String> fullKeywordSet = new TreeSet<String>();

            // expand the keyword list according to the thesaurus and create a new set of keywords
            for (String keyword: keywordSet) {
                Set<String> expandedSet = new TreeSet<String>();
                boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, delegator);
                fullKeywordSet.addAll(expandedSet);
                if (!replaceEntered) {
                    fullKeywordSet.add(keyword);
                }
            }

            return fullKeywordSet;
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            // just make the fixed keyword lists and put them in the context
            if (isAnd) {
                // when isAnd is true we need to make a list of keyword sets where each set corresponds to one
                //incoming/entered keyword and contains all of the expanded keywords plus the entered keyword if none of
                //the expanded keywords are flagged as replacements; now the tricky part: each set should be or'ed together,
                //but then the sets should be and'ed to produce the overall expression; create the SQL for this
                //needs some work as the current method only support a list of and'ed words and a list of or'ed words, not
                //a list of or'ed sets to be and'ed together
                Set<String> keywordSet = KeywordSearchUtil.makeKeywordSet(this.keywordsString, null, true);

                // expand the keyword list according to the thesaurus and create a new set of keywords
                for (String keyword: keywordSet) {
                    Set<String> expandedSet = new TreeSet<String>();
                    boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, workEffortSearchContext.getDelegator());
                    if (!replaceEntered) {
                        expandedSet.add(keyword);
                    }
                    Set<String> fixedSet = KeywordSearchUtil.fixKeywordsForSearch(expandedSet, anyPrefix, anySuffix, removeStems, isAnd);
                    Set<String> fixedKeywordSet = new HashSet<String>();
                    fixedKeywordSet.addAll(fixedSet);
                    workEffortSearchContext.keywordFixedOrSetAndList.add(fixedKeywordSet);
                }
            } else {
                // when isAnd is false, just add all of the new entries to the big list
                Set<String> keywordFirstPass = makeFullKeywordSet(workEffortSearchContext.getDelegator()); // includes keyword expansion, etc
                Set<String> keywordSet = KeywordSearchUtil.fixKeywordsForSearch(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);
                workEffortSearchContext.orKeywordFixedSet.addAll(keywordSet);
            }

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            Map<String, String> valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.keywordsString);
            valueMap.put("anyPrefix", this.anyPrefix ? "Y" : "N");
            valueMap.put("anySuffix", this.anySuffix ? "Y" : "N");
            valueMap.put("isAnd", this.isAnd ? "Y" : "N");
            valueMap.put("removeStems", this.removeStems ? "Y" : "N");
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", valueMap));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortKeywords", locale)).append(": \"");
            ppBuf.append(this.keywordsString).append("\", ").append(UtilProperties.getMessage(resource, "WorkEffortKeywordWhere", locale)).append(" ");
            ppBuf.append(isAnd ? UtilProperties.getMessage(resource, "WorkEffortKeywordAllWordsMatch", locale) : UtilProperties.getMessage(resource, "WorkEffortKeywordAnyWordMatches", locale));
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof KeywordConstraint) {
                KeywordConstraint that = (KeywordConstraint) psc;
                if (this.anyPrefix != that.anyPrefix) {
                    return false;
                }
                if (this.anySuffix != that.anySuffix) {
                    return false;
                }
                if (this.isAnd != that.isAnd) {
                    return false;
                }
                if (this.removeStems != that.removeStems) {
                    return false;
                }
                if (this.keywordsString == null) {
                    if (that.keywordsString != null) {
                        return false;
                    }
                } else {
                    if (!this.keywordsString.equals(that.keywordsString)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("serial")
    public static class LastUpdatedRangeConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "LastUpdatedRange";
        protected Timestamp fromDate;
        protected Timestamp thruDate;

        public LastUpdatedRangeConstraint(Timestamp fromDate, Timestamp thruDate) {
            this.fromDate = fromDate;
            this.thruDate = thruDate;
        }

        @Override
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            workEffortSearchContext.dynamicViewEntity.addAlias("WEFF", "lastModifiedDate", "lastModifiedDate", null, null, null, null);

            EntityConditionList<EntityExpr> dateConditions = null;
            EntityExpr dateCondition=null;
            if (fromDate !=null && thruDate!=null) {
            dateConditions= EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("lastModifiedDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate),
                    EntityCondition.makeCondition("lastModifiedDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate)), EntityOperator.AND);
            } if (fromDate !=null) {
                dateCondition=EntityCondition.makeCondition("lastModifiedDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate);
            } else if (thruDate != null) {
                dateCondition = EntityCondition.makeCondition("lastModifiedDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate);
            }
            EntityConditionList<? extends EntityCondition> conditions = null;
            if (fromDate !=null && thruDate!=null) {
                conditions=EntityCondition.makeCondition(UtilMisc.toList(
                    dateConditions,
                    EntityCondition.makeCondition("lastModifiedDate", EntityOperator.EQUALS, null)),
                    EntityOperator.OR);
            } else {
                conditions=EntityCondition.makeCondition(UtilMisc.toList(
                        dateCondition,
                        EntityCondition.makeCondition("lastModifiedDate", EntityOperator.EQUALS, null)),
                        EntityOperator.OR);
            }

            workEffortSearchContext.entityConditionList.add(conditions);

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString","fromDate : " + fromDate + " thruDate : " + thruDate)));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortLastModified", locale)).append(": \"");
            ppBuf.append(fromDate).append("-").append(thruDate).append("\", ").append(UtilProperties.getMessage(resource, "WorkEffortLastModified", locale)).append(" ");
            return ppBuf.toString();
        }


        @Override
        public boolean equals(Object obj) {
            WorkEffortSearchConstraint psc = (WorkEffortSearchConstraint) obj;
            if (psc instanceof LastUpdatedRangeConstraint) {
                LastUpdatedRangeConstraint that = (LastUpdatedRangeConstraint) psc;
                if (this.fromDate == null) {
                    if (that.fromDate != null) {
                        return false;
                    }
                } else {
                    if (!this.fromDate.equals(that.fromDate)) {
                        return false;
                    }
                }
                if (this.thruDate == null) {
                    if (that.thruDate != null) {
                        return false;
                    }
                } else {
                    if (!this.thruDate.equals(that.thruDate)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    // ======================================================================
    // Result Sort Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class ResultSortOrder implements java.io.Serializable {
        public ResultSortOrder() {
        }

        public abstract void setSortOrder(WorkEffortSearchContext workEffortSearchContext);
        public abstract String getOrderName();
        public abstract String prettyPrintSortOrder(boolean detailed, Locale locale);
        public abstract boolean isAscending();
    }

    @SuppressWarnings("serial")
    public static class SortKeywordRelevancy extends ResultSortOrder {
        public SortKeywordRelevancy() {
        }

        @Override
        public void setSortOrder(WorkEffortSearchContext workEffortSearchContext) {
            if (workEffortSearchContext.includedKeywordSearch) {
                // we have to check this in order to be sure that there is a totalRelevancy to sort by...
                workEffortSearchContext.orderByList.add("-totalRelevancy");
                workEffortSearchContext.fieldsToSelect.add("totalRelevancy");
            }
        }

        @Override
        public String getOrderName() {
            return "KeywordRelevancy";
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "WorkEffortKeywordRelevancy", locale);
        }

        @Override
        public boolean isAscending() {
            return false;
        }
    }

    @SuppressWarnings("serial")
    public static class SortWorkEffortField extends ResultSortOrder {
        protected String fieldName;
        protected boolean ascending;

        /** Some good field names to try might include:
         * [workEffortName]
         * [totalQuantityOrdered] for most popular or most purchased
         * [lastModifiedDate]
         *
         *  You can also include any other field on the WorkEffort entity.
         */
        public SortWorkEffortField(String fieldName, boolean ascending) {
            this.fieldName = fieldName;
            this.ascending = ascending;
        }

        @Override
        public void setSortOrder(WorkEffortSearchContext workEffortSearchContext) {
            if (workEffortSearchContext.getDelegator().getModelEntity("WorkEffort").isField(fieldName)) {
                workEffortSearchContext.dynamicViewEntity.addAlias("WEFF", fieldName);
            }
            if (ascending) {
                workEffortSearchContext.orderByList.add("+" + fieldName);
            } else {
                workEffortSearchContext.orderByList.add("-" + fieldName);
            }
            workEffortSearchContext.fieldsToSelect.add(fieldName);
        }

        @Override
        public String getOrderName() {
            return "WorkEffortField:" + this.fieldName;
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            if ("workEffortName".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortName", locale);
            } else if ("totalQuantityOrdered".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortPopularityByOrders", locale);
            } else if ("totalTimesViewed".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortPopularityByViews", locale);
            } else if ("averageCustomerRating".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortCustomerRating", locale);
            }
            return this.fieldName;
        }

        @Override
        public boolean isAscending() {
            return this.ascending;
        }
    }
}
