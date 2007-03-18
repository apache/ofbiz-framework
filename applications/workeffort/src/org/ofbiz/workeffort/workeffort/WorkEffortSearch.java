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
package org.ofbiz.workeffort.workeffort;

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

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.KeywordSearchUtil;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;



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

    public static ArrayList searchWorkEfforts(List workEffortSearchConstraintList, ResultSortOrder resultSortOrder, GenericDelegator delegator, String visitId) {
        WorkEffortSearchContext workEffortSearchContext = new WorkEffortSearchContext(delegator, visitId);

        workEffortSearchContext.addWorkEffortSearchConstraints(workEffortSearchConstraintList);
        workEffortSearchContext.setResultSortOrder(resultSortOrder);

        ArrayList workEffortIds = workEffortSearchContext.doSearch();
        return workEffortIds;
    }

    public static void getAllSubWorkEffortIds(String workEffortId, Set workEffortIdSet, GenericDelegator delegator, Timestamp nowTimestamp) {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // first make sure the current id is in the Set
        workEffortIdSet.add(workEffortId);

        // now find all sub-categories, filtered by effective dates, and call this routine for them
        try {
            // Find WorkEffortAssoc, workEffortAssocTypeId=WORK_EFF_BREAKDOWN
            List workEffortAssocList = delegator.findByAndCache("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", workEffortId, "workEffortAssocTypeId", "WORK_EFF_BREAKDOWN"));
            Iterator workEffortAssocIter = workEffortAssocList.iterator();
            while (workEffortAssocIter.hasNext()) {
                GenericValue workEffortAssoc = (GenericValue) workEffortAssocIter.next();

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
            List childWorkEffortList = delegator.findByConditionCache("WorkEffort", new EntityExpr("workEffortParentId", EntityComparisonOperator.EQUALS, workEffortId), 
                    UtilMisc.toList("workEffortId", "workEffortParentId"), null);
            Iterator childWorkEffortIter = childWorkEffortList.iterator();
            while (childWorkEffortIter.hasNext()) {
                GenericValue childWorkEffort = (GenericValue) childWorkEffortIter.next();

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
        public List entityConditionList = new LinkedList();
        public List orderByList = new LinkedList();
        public List fieldsToSelect = UtilMisc.toList("workEffortId");
        public DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        public boolean workEffortIdGroupBy = false;
        public boolean includedKeywordSearch = false;
        public Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        public List keywordFixedOrSetAndList = new LinkedList();
        public Set orKeywordFixedSet = new HashSet();
        public Set andKeywordFixedSet = new HashSet();
        public List workEffortSearchConstraintList = new LinkedList();
        public ResultSortOrder resultSortOrder = null;
        public Integer resultOffset = null;
        public Integer maxResults = null;
        protected GenericDelegator delegator = null;
        protected String visitId = null;
        protected Integer totalResults = null;

        public WorkEffortSearchContext(GenericDelegator delegator, String visitId) {
            this.delegator = delegator;
            this.visitId = visitId;
            dynamicViewEntity.addMemberEntity("WEFF", "WorkEffort");
        }

        public GenericDelegator getDelegator() {
            return this.delegator;
        }

        public void addWorkEffortSearchConstraints(List workEffortSearchConstraintList) {
            // Go through the constraints and add them in
            Iterator workEffortSearchConstraintIter = workEffortSearchConstraintList.iterator();
            while (workEffortSearchConstraintIter.hasNext()) {
                WorkEffortSearchConstraint constraint = (WorkEffortSearchConstraint) workEffortSearchConstraintIter.next();
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

        public ArrayList doSearch() {
            long startMillis = System.currentTimeMillis();

            // do the query
            EntityListIterator eli = this.doQuery(delegator);            
            ArrayList workEffortIds = this.makeWorkEffortIdList(eli);
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
            this.saveSearchResultInfo(new Long(workEffortIds.size()), new Double(totalSeconds));

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
            Iterator keywordFixedOrSetAndTestIter = keywordFixedOrSetAndList.iterator();
            while (keywordFixedOrSetAndTestIter.hasNext()) {
                Set keywordFixedOrSet = (Set) keywordFixedOrSetAndTestIter.next();
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

                Iterator keywordIter = andKeywordFixedSet.iterator();
                while (keywordIter.hasNext()) {
                    String keyword = (String) keywordIter.next();

                    // make index based values and increment
                    String entityAlias = "PK" + index;
                    String prefix = "pk" + index;
                    index++;

                    dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
                    entityConditionList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, null));
                }

                //TODO: find out why Oracle and other dbs don't like the query resulting from this and fix: workEffortIdGroupBy = true;

                if (!doingBothAndOr) {
                    dynamicViewEntity.addAlias(null, "totalRelevancy", null, null, null, null, null, relevancyComplexAlias);
                }
            }
            if (keywordFixedOrSetAndList.size() > 0) {
                Iterator keywordFixedOrSetAndIter = keywordFixedOrSetAndList.iterator();
                while (keywordFixedOrSetAndIter.hasNext()) {
                    Set keywordFixedOrSet = (Set) keywordFixedOrSetAndIter.next();
                    // make index based values and increment
                    String entityAlias = "PK" + index;
                    String prefix = "pk" + index;
                    index++;

                    dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
                    List keywordOrList = new LinkedList();
                    Iterator keywordIter = keywordFixedOrSet.iterator();
                    while (keywordIter.hasNext()) {
                        String keyword = (String) keywordIter.next();
                        keywordOrList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));
                    }
                    entityConditionList.add(new EntityConditionList(keywordOrList, EntityOperator.OR));

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

        public EntityListIterator doQuery(GenericDelegator delegator) {
            // handle the now assembled or and and keyword fixed lists
            this.finishKeywordConstraints();

            if (resultSortOrder != null) {
                resultSortOrder.setSortOrder(this);
            }
            dynamicViewEntity.addAlias("WEFF", "workEffortId", null, null, null, new Boolean(workEffortIdGroupBy), null);
            EntityCondition whereCondition = new EntityConditionList(entityConditionList, EntityOperator.AND);
            
            // Debug.logInfo("WorkEffortSearch, whereCondition = " + whereCondition.toString(), module);
            
            EntityFindOptions efo = new EntityFindOptions();
            efo.setDistinct(true);
            efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);

            EntityListIterator eli = null;
            try {
                eli = delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, null, fieldsToSelect, orderByList, efo);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error in workEffort search", module);
                return null;
            }

            return eli;
        }

        public ArrayList makeWorkEffortIdList(EntityListIterator eli) {
            ArrayList workEffortIds = new ArrayList(maxResults == null ? 100 : maxResults.intValue());
            if (eli == null) {
                Debug.logWarning("The eli is null, returning zero results", module);
                return workEffortIds;
            }

            try {
                boolean hasResults = false;
                Object initialResult = null;
                
                /* this method has been replaced by the following to address issue with SAP DB and possibly other DBs
                if (resultOffset != null) {
                    Debug.logInfo("Before relative, current index=" + eli.currentIndex(), module);
                    hasResults = eli.relative(resultOffset.intValue());
                } else {
                    initialResult = eli.next();
                    if (initialResult != null) {
                        hasResults = true;
                    }
                }
                 */

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
                    this.totalResults = new Integer(failTotal);
                    return workEffortIds;
                }

                
                // init numRetreived to one since we have already grabbed the initial one
                int numRetreived = 1;
                int duplicatesFound = 0;

                Set workEffortIdSet = new HashSet();
                
                workEffortIds.add(searchResult.getString("workEffortId"));
                workEffortIdSet.add(searchResult.getString("workEffortId"));

                while (((searchResult = (GenericValue) eli.next()) != null) && (maxResults == null || numRetreived < maxResults.intValue())) {
                    String workEffortId = searchResult.getString("workEffortId");
                    if (!workEffortIdSet.contains(workEffortId)) {
                        workEffortIds.add(workEffortId);
                        workEffortIdSet.add(workEffortId);
                        numRetreived++;
                    } else {
                        duplicatesFound++;
                    }
                    
                    /*
                    StringBuffer lineMsg = new StringBuffer("Got search result line: ");
                    Iterator fieldsToSelectIter = fieldsToSelect.iterator();
                    while (fieldsToSelectIter.hasNext()) {
                        String fieldName = (String) fieldsToSelectIter.next();
                        lineMsg.append(fieldName);
                        lineMsg.append("=");
                        lineMsg.append(searchResult.get(fieldName));
                        if (fieldsToSelectIter.hasNext()) {
                            lineMsg.append(", ");
                        }
                    }
                    Debug.logInfo(lineMsg.toString(), module);
                    */
                }

                if (searchResult != null) {
                    // we weren't at the end, so go to the end and get the index
                    //Debug.logInfo("Getting totalResults from ending index - before last() currentIndex=" + eli.currentIndex(), module);
                    if (eli.last()) {
                        this.totalResults = new Integer(eli.currentIndex());
                        //Debug.logInfo("Getting totalResults from ending index - after last() currentIndex=" + eli.currentIndex(), module);
                    }
                }
                if (this.totalResults == null || this.totalResults.intValue() == 0) {
                    int total = numRetreived;
                    if (this.resultOffset != null) {
                        total += (this.resultOffset.intValue() - 1);
                    }
                    this.totalResults = new Integer(total);
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

                    GenericValue workEffortSearchResult = delegator.makeValue("WorkEffortSearchResult", null);
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

                    Iterator workEffortSearchConstraintIter = workEffortSearchConstraintList.iterator();
                    int seqId = 1;
                    while (workEffortSearchConstraintIter.hasNext()) {
                        GenericValue workEffortSearchConstraint = (GenericValue) workEffortSearchConstraintIter.next();
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

    public static abstract class WorkEffortSearchConstraint implements java.io.Serializable {
        public WorkEffortSearchConstraint() { }

        public abstract void addConstraint(WorkEffortSearchContext workEffortSearchContext);
        /** pretty print for log messages and even UI stuff */
        public abstract String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale);
    }
    
    
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

        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            Set workEffortIdSet = FastSet.newInstance();
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
            
            List assocConditionFromTo = FastList.newInstance();
            assocConditionFromTo.add(new EntityExpr(prefix + "WorkEffortIdTo", EntityOperator.IN, workEffortIdSet));
            if (UtilValidate.isNotEmpty(workEffortAssocTypeId)) {
                assocConditionFromTo.add(new EntityExpr(prefix + "WorkEffortAssocTypeId", EntityOperator.EQUALS, workEffortAssocTypeId));
            }
            assocConditionFromTo.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            assocConditionFromTo.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

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
            
            List assocConditionToFrom = FastList.newInstance();
            assocConditionToFrom.add(new EntityExpr(prefix + "WorkEffortIdFrom", EntityOperator.IN, workEffortIdSet));
            if (UtilValidate.isNotEmpty(workEffortAssocTypeId)) {
                assocConditionToFrom.add(new EntityExpr(prefix + "WorkEffortAssocTypeId", EntityOperator.EQUALS, workEffortAssocTypeId));
            }
            assocConditionToFrom.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            assocConditionToFrom.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

            // now create and add the combined constraint
            workEffortSearchContext.entityConditionList.add(new EntityExpr(new EntityConditionList(assocConditionFromTo, EntityOperator.AND), EntityOperator.OR, new EntityConditionList(assocConditionToFrom, EntityOperator.AND)));
            
            
            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.workEffortId + "," + this.workEffortAssocTypeId, "includeSubWorkEfforts", this.includeSubWorkEfforts ? "Y" : "N")));
        }

        
        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue workEffort = null;
            GenericValue workEffortAssocType = null;
            try {
                workEffort = delegator.findByPrimaryKeyCache("WorkEffort", UtilMisc.toMap("workEffortId", this.workEffortId));
                workEffortAssocType = delegator.findByPrimaryKeyCache("WorkEffortAssocType", UtilMisc.toMap("workEffortAssocTypeId", this.workEffortAssocTypeId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up WorkEffortAssocConstraint pretty print info: " + e.toString(), module);
            }

            StringBuffer ppBuf = new StringBuffer();            
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
                ppBuf.append(" (" + UtilProperties.getMessage(resource, "WorkEffortIncludeAllSubWorkEfforts", locale) + ")");
            }
            return ppBuf.toString();
        }

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
    public static class WorkEffortReviewConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "WorkEffortReview";
        protected String reviewTextString;
        
        public WorkEffortReviewConstraint(String reviewTextString) {
            this.reviewTextString = reviewTextString;        
        }        
               
        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            String entityAlias = "WFR" + workEffortSearchContext.index;
            String prefix = "wfr" + workEffortSearchContext.index;
            workEffortSearchContext.index++;

            workEffortSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "WorkEffortReview");
            workEffortSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ReviewText", "reviewText", null, null, null, null);
            workEffortSearchContext.dynamicViewEntity.addViewLink("WEFF", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("workEffortId"));
            workEffortSearchContext.entityConditionList.add( new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue(prefix + "ReviewText")), EntityOperator.LIKE ,new EntityFunction.UPPER(("%"+ reviewTextString) + "%")));
            Map valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.reviewTextString);
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", valueMap));
        }

        
        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer ppBuf = new StringBuffer();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortReviews", locale) + ": \"");
            ppBuf.append(this.reviewTextString + "\", " + UtilProperties.getMessage(resource, "WorkEffortKeywordWhere", locale) + " ");                        
            return ppBuf.toString();
        }

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
    
    public static class PartyAssignmentConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "PartyAssignment";
        protected String partyId;
        protected String roleTypeId;

        public PartyAssignmentConstraint(String partyId, String roleTypeId) {
            this.partyId = partyId;
            this.roleTypeId = roleTypeId;
        }

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
            
            workEffortSearchContext.entityConditionList.add(new EntityExpr(prefix + "PartyId", EntityOperator.EQUALS, partyId));
            workEffortSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            workEffortSearchContext.entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));
            if (UtilValidate.isNotEmpty(this.roleTypeId)) {
                workEffortSearchContext.entityConditionList.add(new EntityExpr(prefix + "RoleTypeId", EntityOperator.EQUALS, roleTypeId));
            }

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.partyId + "," + this.roleTypeId)));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue partyNameView = null;
            GenericValue roleType = null;
            try {
                partyNameView = delegator.findByPrimaryKeyCache("PartyNameView", UtilMisc.toMap("partyId", partyId));
                roleType = delegator.findByPrimaryKeyCache("RoleType", UtilMisc.toMap("roleTypeId", roleTypeId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding PartyAssignmentConstraint information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();
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

    public static class ProductSetConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "ProductSet";
        protected Set productIdSet;

        public ProductSetConstraint(Collection productIdSet) {
            this.productIdSet = new HashSet(productIdSet);
        }

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
            
            workEffortSearchContext.entityConditionList.add(new EntityExpr(prefix + "ProductId", EntityOperator.IN, productIdSet));
            workEffortSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, workEffortSearchContext.nowTimestamp)));
            workEffortSearchContext.entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, workEffortSearchContext.nowTimestamp));

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            StringBuffer productIdInfo = new StringBuffer();
            Iterator productIdIter = this.productIdSet.iterator();
            while (productIdIter.hasNext()) {
                String productId = (String) productIdIter.next();
                productIdInfo.append(productId);
                if (productIdIter.hasNext()) {
                    productIdInfo.append(",");
                }
            }
            
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", productIdInfo.toString())));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer infoOut = new StringBuffer();
            try {
                Iterator productIdIter = this.productIdSet.iterator();
                while (productIdIter.hasNext()) {
                    String productId = (String) productIdIter.next();
                    GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
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

        public Set makeFullKeywordSet(GenericDelegator delegator) {
            Set keywordSet = KeywordSearchUtil.makeKeywordSet(this.keywordsString, null, true);
            Set fullKeywordSet = new TreeSet();

            // expand the keyword list according to the thesaurus and create a new set of keywords
            Iterator keywordIter = keywordSet.iterator();
            while (keywordIter.hasNext()) {
                String keyword = (String) keywordIter.next();
                Set expandedSet = new TreeSet();
                boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, delegator);
                fullKeywordSet.addAll(expandedSet);
                if (!replaceEntered) {
                    fullKeywordSet.add(keyword);
                }
            }

            return fullKeywordSet;
        }

        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            // just make the fixed keyword lists and put them in the context
            if (isAnd) {
                // when isAnd is true we need to make a list of keyword sets where each set corresponds to one
                //incoming/entered keyword and contains all of the expanded keywords plus the entered keyword if none of
                //the expanded keywords are flagged as replacements; now the tricky part: each set should be or'ed together,
                //but then the sets should be and'ed to produce the overall expression; create the SQL for this
                //needs some work as the current method only support a list of and'ed words and a list of or'ed words, not
                //a list of or'ed sets to be and'ed together
                Set keywordSet = KeywordSearchUtil.makeKeywordSet(this.keywordsString, null, true);

                // expand the keyword list according to the thesaurus and create a new set of keywords
                Iterator keywordIter = keywordSet.iterator();
                while (keywordIter.hasNext()) {
                    String keyword = (String) keywordIter.next();
                    Set expandedSet = new TreeSet();
                    boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, workEffortSearchContext.getDelegator());
                    if (!replaceEntered) {
                        expandedSet.add(keyword);
                    }
                    Set fixedSet = KeywordSearchUtil.fixKeywordsForSearch(expandedSet, anyPrefix, anySuffix, removeStems, isAnd);
                    Set fixedKeywordSet = new HashSet();
                    fixedKeywordSet.addAll(fixedSet);
                    workEffortSearchContext.keywordFixedOrSetAndList.add(fixedKeywordSet);
                }
            } else {
                // when isAnd is false, just add all of the new entries to the big list
                Set keywordFirstPass = makeFullKeywordSet(workEffortSearchContext.getDelegator()); // includes keyword expansion, etc
                Set keywordSet = KeywordSearchUtil.fixKeywordsForSearch(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);
                workEffortSearchContext.orKeywordFixedSet.addAll(keywordSet);
            }

            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            Map valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.keywordsString);
            valueMap.put("anyPrefix", this.anyPrefix ? "Y" : "N");
            valueMap.put("anySuffix", this.anySuffix ? "Y" : "N");
            valueMap.put("isAnd", this.isAnd ? "Y" : "N");
            valueMap.put("removeStems", this.removeStems ? "Y" : "N");
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", valueMap));
        }

        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer ppBuf = new StringBuffer();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortKeywords", locale) + ": \"");
            ppBuf.append(this.keywordsString + "\", " + UtilProperties.getMessage(resource, "WorkEffortKeywordWhere", locale) + " ");
            ppBuf.append(isAnd ? UtilProperties.getMessage(resource, "WorkEffortKeywordAllWordsMatch", locale) : UtilProperties.getMessage(resource, "WorkEffortKeywordAnyWordMatches", locale));            
            return ppBuf.toString();
        }

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

    public static class LastUpdatedRangeConstraint extends WorkEffortSearchConstraint {
        public static final String constraintName = "LastUpdatedRange";
        protected Timestamp fromDate;
        protected Timestamp thruDate;

        public LastUpdatedRangeConstraint(Timestamp fromDate, Timestamp thruDate) {
            this.fromDate = fromDate;
            this.thruDate = thruDate;            
        }

        public void addConstraint(WorkEffortSearchContext workEffortSearchContext) {
            workEffortSearchContext.dynamicViewEntity.addAlias("WEFF", "lastModifiedDate", "lastModifiedDate", null, null, null, null);
            
            EntityConditionList dateConditions = null;
            EntityExpr dateCondition=null;
            if(fromDate !=null && thruDate!=null) {
            dateConditions= new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("lastModifiedDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate),
                    new EntityExpr("lastModifiedDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate)), EntityOperator.AND);
            } if(fromDate !=null) {
                dateCondition=new EntityExpr("lastModifiedDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate);
            } else if (thruDate != null) {
                dateCondition = new EntityExpr("lastModifiedDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate);
            }
            EntityConditionList conditions = null;
            if(fromDate !=null && thruDate!=null) {
                conditions=new EntityConditionList(UtilMisc.toList(
                    dateConditions,
                    new EntityExpr("lastModifiedDate", EntityOperator.EQUALS, null)),
                    EntityOperator.OR);
            } else {
                conditions=new EntityConditionList(UtilMisc.toList(
                        dateCondition,
                        new EntityExpr("lastModifiedDate", EntityOperator.EQUALS, null)),
                        EntityOperator.OR);
            }
             
            workEffortSearchContext.entityConditionList.add(conditions);
           
            // add in workEffortSearchConstraint, don't worry about the workEffortSearchResultId or constraintSeqId, those will be fill in later
            workEffortSearchContext.workEffortSearchConstraintList.add(workEffortSearchContext.getDelegator().makeValue("WorkEffortSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString","fromDate : " + fromDate + " thruDate : " + thruDate)));
        }

        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer ppBuf = new StringBuffer();
            ppBuf.append(UtilProperties.getMessage(resource, "WorkEffortLastModified", locale) + ": \"");
            ppBuf.append(fromDate +"-" +thruDate + "\", " + UtilProperties.getMessage(resource, "WorkEffortLastModified", locale) + " ");                        
            return ppBuf.toString();
        }
        

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

    public static abstract class ResultSortOrder implements java.io.Serializable {
        public ResultSortOrder() {
        }

        public abstract void setSortOrder(WorkEffortSearchContext workEffortSearchContext);
        public abstract String getOrderName();
        public abstract String prettyPrintSortOrder(boolean detailed, Locale locale);
        public abstract boolean isAscending();
    }

    public static class SortKeywordRelevancy extends ResultSortOrder {
        public SortKeywordRelevancy() {
        }

        public void setSortOrder(WorkEffortSearchContext workEffortSearchContext) {
            if (workEffortSearchContext.includedKeywordSearch) {
                // we have to check this in order to be sure that there is a totalRelevancy to sort by...
                workEffortSearchContext.orderByList.add("-totalRelevancy");
                workEffortSearchContext.fieldsToSelect.add("totalRelevancy");
            }
        }

        public String getOrderName() {
            return "KeywordRelevancy";
        }

        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "WorkEffortKeywordRelevency", locale);
        }

        public boolean isAscending() {
            return false;
        }
    }

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

        public String getOrderName() {
            return "WorkEffortField:" + this.fieldName;
        }

        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            if ("workEffortName".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortWorkEffortName", locale);
            } else if ("totalQuantityOrdered".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortPopularityByOrders", locale);
            } else if ("totalTimesViewed".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortPopularityByViews", locale);
            } else if ("averageCustomerRating".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "WorkEffortCustomerRating", locale);
            }
            return this.fieldName;
        }

        public boolean isAscending() {
            return this.ascending;
        }
    }
}
