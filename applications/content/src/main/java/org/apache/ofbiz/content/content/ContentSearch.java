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
package org.apache.ofbiz.content.content;

import java.sql.Timestamp;
import java.util.ArrayList;
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

public class ContentSearch {

    public static final String module = ContentSearch.class.getName();
    public static final String resource = "ContentUiLabels";

    public static ArrayList<String> searchContents(List<? extends ContentSearchConstraint> contentSearchConstraintList, ResultSortOrder resultSortOrder, Delegator delegator, String visitId) {
        ContentSearchContext contentSearchContext = new ContentSearchContext(delegator, visitId);

        contentSearchContext.addContentSearchConstraints(contentSearchConstraintList);
        contentSearchContext.setResultSortOrder(resultSortOrder);

        ArrayList<String> contentIds = contentSearchContext.doSearch();
        return contentIds;
    }

    public static void getAllSubContentIds(String contentId, Set<String> contentIdSet, Delegator delegator, Timestamp nowTimestamp) {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // first make sure the current id is in the Set
        contentIdSet.add(contentId);

        // now find all sub-categories, filtered by effective dates, and call this routine for them
        try {
            List<GenericValue> contentAssocList = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentId).cache().queryList();
            for (GenericValue contentAssoc: contentAssocList) {
                String subContentId = contentAssoc.getString("contentIdTo");
                if (contentIdSet.contains(subContentId)) {
                    // if this category has already been traversed, no use doing it again; this will also avoid infinite loops
                    continue;
                }

                // do the date filtering in the loop to avoid looping through the list twice
                if (EntityUtil.isValueActive(contentAssoc, nowTimestamp)) {
                    getAllSubContentIds(subContentId, contentIdSet, delegator, nowTimestamp);
                }
            }

            // Find Content where current contentId = contentParentId; only select minimal fields to keep the size low
            List<GenericValue> childContentList = EntityQuery.use(delegator)
                    .select("contentId", "ownerContentId").from("Content")
                    .where("ownerContentId", contentId)
                    .cache().queryList();
            for (GenericValue childContent: childContentList) {
                String subContentId = childContent.getString("contentId");
                if (contentIdSet.contains(subContentId)) {
                    // if this category has already been traversed, no use doing it again; this will also avoid infinite loops
                    continue;
                }

                // do the date filtering in the loop to avoid looping through the list twice
                getAllSubContentIds(subContentId, contentIdSet, delegator, nowTimestamp);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding sub-categories for content search", module);
        }
    }

    public static class ContentSearchContext {
        public int index = 1;
        public List<EntityCondition> entityConditionList = new LinkedList<EntityCondition>();
        public List<String> orderByList = new LinkedList<String>();
        public Set<String> fieldsToSelect = UtilMisc.toSet("contentId");
        public DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        public boolean contentIdGroupBy = false;
        public boolean includedKeywordSearch = false;
        public Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        public List<Set<String>> keywordFixedOrSetAndList = new LinkedList<Set<String>>();
        public Set<String> orKeywordFixedSet = new HashSet<String>();
        public Set<String> andKeywordFixedSet = new HashSet<String>();
        public List<GenericValue> contentSearchConstraintList = new LinkedList<GenericValue>();
        public ResultSortOrder resultSortOrder = null;
        public Integer resultOffset = null;
        public Integer maxResults = null;
        protected Delegator delegator = null;
        protected String visitId = null;
        protected Integer totalResults = null;

        public ContentSearchContext(Delegator delegator, String visitId) {
            this.delegator = delegator;
            this.visitId = visitId;
            dynamicViewEntity.addMemberEntity("CNT", "Content");
        }

        public Delegator getDelegator() {
            return this.delegator;
        }

        public void addContentSearchConstraints(List<? extends ContentSearchConstraint> contentSearchConstraintList) {
            // Go through the constraints and add them in
            for (ContentSearchConstraint constraint: contentSearchConstraintList) {
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
            ArrayList<String> contentIds = null;
            try (EntityListIterator eli = this.doQuery(delegator)) {
                contentIds = this.makeContentIdList(eli);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error closing ContentSearch EntityListIterator");
            }

            long endMillis = System.currentTimeMillis();
            double totalSeconds = ((double)endMillis - (double)startMillis)/1000.0;

            // store info about results in the database, attached to the user's visitId, if specified
            this.saveSearchResultInfo(Long.valueOf(contentIds.size()), Double.valueOf(totalSeconds));

            return contentIds;
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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ContentKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("CNT", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("contentId"));
                    entityConditionList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, null));
                }

                //TODO: find out why Oracle and other dbs don't like the query resulting from this and fix: contentIdGroupBy = true;

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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ContentKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("CNT", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("contentId"));
                    List<EntityExpr> keywordOrList = new LinkedList<EntityExpr>();
                    for (String keyword: keywordFixedOrSet) {
                        keywordOrList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));
                    }
                    entityConditionList.add(EntityCondition.makeCondition(keywordOrList, EntityOperator.OR));

                    contentIdGroupBy = true;

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

        /**
         * @param delegator the delegator
         * @return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
         *      DONE WITH IT (preferably in a finally block), 
         *      AND DON'T LEAVE IT OPEN TOO LONG BECAUSE IT WILL MAINTAIN A DATABASE CONNECTION.
         */
        public EntityListIterator doQuery(Delegator delegator) {
            // handle the now assembled or and and keyword fixed lists
            this.finishKeywordConstraints();

            if (resultSortOrder != null) {
                resultSortOrder.setSortOrder(this);
            }
            dynamicViewEntity.addAlias("CNT", "contentId", null, null, null, Boolean.valueOf(contentIdGroupBy), null);
            EntityCondition whereCondition = EntityCondition.makeCondition(entityConditionList, EntityOperator.AND);

            EntityListIterator eli = null;
            try {
                eli = EntityQuery.use(delegator)
                        .select(fieldsToSelect).from(dynamicViewEntity)
                        .where(whereCondition)
                        .cursorScrollInsensitive()
                        .distinct()
                        .maxRows(maxResults)
                        .queryIterator();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error in content search", module);
                return null;
            }

            return eli;
        }

        public ArrayList<String> makeContentIdList(EntityListIterator eli) {
            ArrayList<String> contentIds = new ArrayList<String>(maxResults == null ? 100 : maxResults.intValue());
            if (eli == null) {
                Debug.logWarning("The eli is null, returning zero results", module);
                return contentIds;
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
                    return contentIds;
                }


                // init numRetreived to one since we have already grabbed the initial one
                int numRetreived = 1;
                int duplicatesFound = 0;

                Set<String> contentIdSet = new HashSet<String>();

                contentIds.add(searchResult.getString("contentId"));
                contentIdSet.add(searchResult.getString("contentId"));

                while (((searchResult = eli.next()) != null) && (maxResults == null || numRetreived < maxResults.intValue())) {
                    String contentId = searchResult.getString("contentId");
                    if (!contentIdSet.contains(contentId)) {
                        contentIds.add(contentId);
                        contentIdSet.add(contentId);
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
                Debug.logError(e, "Error getting results from the content search query", module);
            }
            return contentIds;
        }

        public void saveSearchResultInfo(Long numResults, Double secondsTotal) {
            // uses entities: ContentSearchResult and ContentSearchConstraint

            try {
                // make sure this is in a transaction
                boolean beganTransaction = TransactionUtil.begin();

                try {

                    GenericValue contentSearchResult = delegator.makeValue("ContentSearchResult");
                    String contentSearchResultId = delegator.getNextSeqId("ContentSearchResult");

                    contentSearchResult.set("contentSearchResultId", contentSearchResultId);
                    contentSearchResult.set("visitId", this.visitId);
                    if (this.resultSortOrder != null) {
                        contentSearchResult.set("orderByName", this.resultSortOrder.getOrderName());
                        contentSearchResult.set("isAscending", this.resultSortOrder.isAscending() ? "Y" : "N");
                    }
                    contentSearchResult.set("numResults", numResults);
                    contentSearchResult.set("secondsTotal", secondsTotal);
                    contentSearchResult.set("searchDate", nowTimestamp);
                    contentSearchResult.create();

                    int seqId = 1;
                    for (GenericValue contentSearchConstraint: contentSearchConstraintList) {
                        contentSearchConstraint.set("contentSearchResultId", contentSearchResultId);
                        contentSearchConstraint.set("constraintSeqId", Integer.toString(seqId));
                        contentSearchConstraint.create();
                        seqId++;
                    }

                    TransactionUtil.commit(beganTransaction);
                } catch (GenericEntityException e1) {
                    String errMsg = "Error saving content search result info/stats";
                    Debug.logError(e1, errMsg, module);
                    TransactionUtil.rollback(beganTransaction, errMsg, e1);
                }
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error saving content search result info/stats", module);
            }
        }
    }

    // ======================================================================
    // Search Constraint Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class ContentSearchConstraint implements java.io.Serializable {
        public ContentSearchConstraint() { }

        public abstract void addConstraint(ContentSearchContext contentSearchContext);
        /** pretty print for log messages and even UI stuff */
        public abstract String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale);
    }


    @SuppressWarnings("serial")
    public static class ContentAssocConstraint extends ContentSearchConstraint {
        public static final String constraintName = "ContentAssoc";
        protected String contentId;
        protected String contentAssocTypeId;
        protected boolean includeSubContents;

        public ContentAssocConstraint(String contentId, String contentAssocTypeId, boolean includeSubContents) {
            this.contentId = contentId;
            this.contentAssocTypeId = contentAssocTypeId;
            this.includeSubContents = includeSubContents;
        }

        @Override
        public void addConstraint(ContentSearchContext contentSearchContext) {
            Set<String> contentIdSet = new HashSet<String>();
            if (includeSubContents) {
                // find all sub-categories recursively, make a Set of contentId
                ContentSearch.getAllSubContentIds(contentId, contentIdSet, contentSearchContext.getDelegator(), contentSearchContext.nowTimestamp);
            } else {
                contentIdSet.add(contentId);
            }

            // allow assoc from or to the current WE and the contentId on this constraint

            // make index based values and increment
            String entityAlias;
            String prefix;

            // do contentId = contentIdFrom, contentIdTo IN contentIdSet
            entityAlias = "CNT" + contentSearchContext.index;
            prefix = "cnt" + contentSearchContext.index;
            contentSearchContext.index++;

            contentSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "ContentAssoc");
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentIdFrom", "contentId", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentIdTo", "contentIdTo", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentAssocTypeId", "contentAssocTypeId", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addViewLink("CNT", entityAlias, Boolean.TRUE, ModelKeyMap.makeKeyMapList("contentId"));

            List<EntityExpr> assocConditionFromTo = new LinkedList<EntityExpr>();
            assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "ContentIdTo", EntityOperator.IN, contentIdSet));
            if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
                assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "ContentAssocTypeId", EntityOperator.EQUALS, contentAssocTypeId));
            }
            assocConditionFromTo.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, contentSearchContext.nowTimestamp)));
            assocConditionFromTo.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, contentSearchContext.nowTimestamp));

            // do contentId = contentIdTo, contentIdFrom IN contentIdSet
            entityAlias = "CNT" + contentSearchContext.index;
            prefix = "cnt" + contentSearchContext.index;
            contentSearchContext.index++;

            contentSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "ContentAssoc");
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentIdFrom", "contentId", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentIdTo", "contentIdTo", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ContentAssocTypeId", "contentAssocTypeId", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            contentSearchContext.dynamicViewEntity.addViewLink("CNT", entityAlias, Boolean.TRUE, ModelKeyMap.makeKeyMapList("contentId","contentIdTo"));

            List<EntityExpr> assocConditionToFrom = new LinkedList<EntityExpr>();
            assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "ContentIdFrom", EntityOperator.IN, contentIdSet));
            if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
                assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "ContentAssocTypeId", EntityOperator.EQUALS, contentAssocTypeId));
            }
            assocConditionToFrom.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, contentSearchContext.nowTimestamp)));
            assocConditionToFrom.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, contentSearchContext.nowTimestamp));

            // now create and add the combined constraint
            contentSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(assocConditionFromTo, EntityOperator.AND), EntityOperator.OR, EntityCondition.makeCondition(assocConditionToFrom, EntityOperator.AND)));


            // add in contentSearchConstraint, don't worry about the contentSearchResultId or constraintSeqId, those will be fill in later
            contentSearchContext.contentSearchConstraintList.add(contentSearchContext.getDelegator().makeValue("ContentSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.contentId + "," + this.contentAssocTypeId)));
        }


        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue content = null;
            GenericValue contentAssocType = null;
            try {
                content = EntityQuery.use(delegator).from("Content").where("contentId", this.contentId).cache().queryOne();
                contentAssocType = EntityQuery.use(delegator).from("ContentAssocType").where("contentAssocTypeId", this.contentAssocTypeId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up ContentAssocConstraint pretty print info: " + e.toString(), module);
            }

            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ContentAssoc", locale) + ": ");
            if (content != null) {
                ppBuf.append(content.getString("contentName"));
            }
            if (content == null || detailed) {
                ppBuf.append(" [");
                ppBuf.append(contentId);
                ppBuf.append("]");
            }
            if (UtilValidate.isNotEmpty(this.contentAssocTypeId)) {
                if (contentAssocType != null) {
                    ppBuf.append(contentAssocType.getString("description"));
                }
                if (contentAssocType == null || detailed) {
                    ppBuf.append(" [");
                    ppBuf.append(contentAssocTypeId);
                    ppBuf.append("]");
                }
            }
            if (this.includeSubContents) {
                ppBuf.append(" (").append(UtilProperties.getMessage(resource, "ContentIncludeAllSubContents", locale)).append(")");
            }
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ContentSearchConstraint)) return false;
            ContentSearchConstraint psc = (ContentSearchConstraint) obj;
            if (psc instanceof ContentAssocConstraint) {
                ContentAssocConstraint that = (ContentAssocConstraint) psc;
                if (this.includeSubContents != that.includeSubContents) {
                    return false;
                }
                if (this.contentId == null) {
                    if (that.contentId != null) {
                        return false;
                    }
                } else {
                    if (!this.contentId.equals(that.contentId)) {
                        return false;
                    }
                }
                if (this.contentAssocTypeId == null) {
                    if (that.contentAssocTypeId != null) {
                        return false;
                    }
                } else {
                    if (!this.contentAssocTypeId.equals(that.contentAssocTypeId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((contentAssocTypeId == null) ? 0 : contentAssocTypeId.hashCode());
            result = prime * result + ((contentId == null) ? 0 : contentId.hashCode());
            result = prime * result + (includeSubContents ? 1231 : 1237);
            return result;
        }
    }

    @SuppressWarnings("serial")
    public static class KeywordConstraint extends ContentSearchConstraint {
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
        public void addConstraint(ContentSearchContext contentSearchContext) {
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
                    boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, contentSearchContext.getDelegator());
                    if (!replaceEntered) {
                        expandedSet.add(keyword);
                    }
                    Set<String> fixedSet = KeywordSearchUtil.fixKeywordsForSearch(expandedSet, anyPrefix, anySuffix, removeStems, isAnd);
                    Set<String> fixedKeywordSet = new HashSet<String>();
                    fixedKeywordSet.addAll(fixedSet);
                    contentSearchContext.keywordFixedOrSetAndList.add(fixedKeywordSet);
                }
            } else {
                // when isAnd is false, just add all of the new entries to the big list
                Set<String> keywordFirstPass = makeFullKeywordSet(contentSearchContext.getDelegator()); // includes keyword expansion, etc
                Set<String> keywordSet = KeywordSearchUtil.fixKeywordsForSearch(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);
                contentSearchContext.orKeywordFixedSet.addAll(keywordSet);
            }

            // add in contentSearchConstraint, don't worry about the contentSearchResultId or constraintSeqId, those will be fill in later
            Map<String, String> valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.keywordsString);
            valueMap.put("anyPrefix", this.anyPrefix ? "Y" : "N");
            valueMap.put("anySuffix", this.anySuffix ? "Y" : "N");
            valueMap.put("isAnd", this.isAnd ? "Y" : "N");
            valueMap.put("removeStems", this.removeStems ? "Y" : "N");
            contentSearchContext.contentSearchConstraintList.add(contentSearchContext.getDelegator().makeValue("ContentSearchConstraint", valueMap));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ContentKeywords", locale)).append(": \"");
            ppBuf.append(this.keywordsString).append("\", ").append(UtilProperties.getMessage(resource, "ContentKeywordWhere", locale)).append(" ");
            ppBuf.append(isAnd ? UtilProperties.getMessage(resource, "ContentKeywordAllWordsMatch", locale) : UtilProperties.getMessage(resource, "ContentKeywordAnyWordMatches", locale));
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj instanceof ContentSearchConstraint)) {
                ContentSearchConstraint psc = (ContentSearchConstraint) obj;
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
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (anyPrefix ? 1231 : 1237);
            result = prime * result + (anySuffix ? 1231 : 1237);
            result = prime * result + (isAnd ? 1231 : 1237);
            result = prime * result + ((keywordsString == null) ? 0 : keywordsString.hashCode());
            result = prime * result + (removeStems ? 1231 : 1237);
            return result;
        }
    }

    @SuppressWarnings("serial")
    public static class LastUpdatedRangeConstraint extends ContentSearchConstraint {
        public static final String constraintName = "LastUpdatedRange";
        protected Timestamp fromDate;
        protected Timestamp thruDate;

        public LastUpdatedRangeConstraint(Timestamp fromDate, Timestamp thruDate) {
            this.fromDate = fromDate != null ? (Timestamp) fromDate.clone() : null;
            this.thruDate = thruDate != null ? (Timestamp) thruDate.clone() : null;
        }

        @Override
        public void addConstraint(ContentSearchContext contentSearchContext) {
            contentSearchContext.dynamicViewEntity.addAlias("CNT", "lastModifiedDate", "lastModifiedDate", null, null, null, null);

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

            contentSearchContext.entityConditionList.add(conditions);

            // add in contentSearchConstraint, don't worry about the contentSearchResultId or constraintSeqId, those will be fill in later
            contentSearchContext.contentSearchConstraintList.add(contentSearchContext.getDelegator().makeValue("ContentSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString","fromDate : " + fromDate + " thruDate : " + thruDate)));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ContentLastModified", locale)).append(": \"");
            ppBuf.append(fromDate).append("-").append(thruDate).append("\", ").append(UtilProperties.getMessage(resource, "ContentLastModified", locale)).append(" ");
            return ppBuf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ContentSearchConstraint) {
                ContentSearchConstraint psc = (ContentSearchConstraint) obj;
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
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fromDate == null) ? 0 : fromDate.hashCode());
            result = prime * result + ((thruDate == null) ? 0 : thruDate.hashCode());
            return result;
        }
    }

    // ======================================================================
    // Result Sort Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class ResultSortOrder implements java.io.Serializable {
        public ResultSortOrder() {
        }

        public abstract void setSortOrder(ContentSearchContext contentSearchContext);
        public abstract String getOrderName();
        public abstract String prettyPrintSortOrder(boolean detailed, Locale locale);
        public abstract boolean isAscending();
    }

    @SuppressWarnings("serial")
    public static class SortKeywordRelevancy extends ResultSortOrder {
        public SortKeywordRelevancy() {
        }

        @Override
        public void setSortOrder(ContentSearchContext contentSearchContext) {
            if (contentSearchContext.includedKeywordSearch) {
                // we have to check this in order to be sure that there is a totalRelevancy to sort by...
                contentSearchContext.orderByList.add("-totalRelevancy");
                contentSearchContext.fieldsToSelect.add("totalRelevancy");
            }
        }

        @Override
        public String getOrderName() {
            return "KeywordRelevancy";
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ContentKeywordRelevancy", locale);
        }

        @Override
        public boolean isAscending() {
            return false;
        }
    }

    @SuppressWarnings("serial")
    public static class SortContentField extends ResultSortOrder {
        protected String fieldName;
        protected boolean ascending;

        /** Some good field names to try might include:
         * [contentName]
         * [totalQuantityOrdered] for most popular or most purchased
         * [lastModifiedDate]
         *
         *  You can also include any other field on the Content entity.
         */
        public SortContentField(String fieldName, boolean ascending) {
            this.fieldName = fieldName;
            this.ascending = ascending;
        }

        @Override
        public void setSortOrder(ContentSearchContext contentSearchContext) {
            if (contentSearchContext.getDelegator().getModelEntity("Content").isField(fieldName)) {
                contentSearchContext.dynamicViewEntity.addAlias("CNT", fieldName);
            }
            if (ascending) {
                contentSearchContext.orderByList.add("+" + fieldName);
            } else {
                contentSearchContext.orderByList.add("-" + fieldName);
            }
            contentSearchContext.fieldsToSelect.add(fieldName);
        }

        @Override
        public String getOrderName() {
            return "ContentField:" + this.fieldName;
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            if ("contentName".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ContentName", locale);
            } else if ("totalQuantityOrdered".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ContentPopularityByOrders", locale);
            } else if ("totalTimesViewed".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ContentPopularityByViews", locale);
            } else if ("averageCustomerRating".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ContentCustomerRating", locale);
            }
            return this.fieldName;
        }

        @Override
        public boolean isAscending() {
            return this.ascending;
        }
    }
}
