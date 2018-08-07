/*
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
 */
package org.apache.ofbiz.product.product;

import java.math.BigDecimal;
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
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionParam;
import org.apache.ofbiz.entity.condition.EntityConditionSubSelect;
import org.apache.ofbiz.entity.condition.EntityConditionValue;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.product.category.CategoryContentWrapper;
import org.apache.ofbiz.service.LocalDispatcher;


/**
 *  Utilities for product search based on various constraints including categories, features and keywords.
 */
public class ProductSearch {

    public static final String module = ProductSearch.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final String resourceCommon = "CommonUiLabels";

    public static ArrayList<String> parametricKeywordSearch(Map<?, String> featureIdByType, String keywordsString, Delegator delegator, String productCategoryId, String visitId, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        Set<String> featureIdSet = new HashSet<>();
        if (featureIdByType != null) {
            featureIdSet.addAll(featureIdByType.values());
        }

        return parametricKeywordSearch(featureIdSet, keywordsString, delegator, productCategoryId, true, visitId, anyPrefix, anySuffix, isAnd);
    }

    public static ArrayList<String> parametricKeywordSearch(Set<String> featureIdSet, String keywordsString, Delegator delegator, String productCategoryId, boolean includeSubCategories, String visitId, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        List<ProductSearchConstraint> productSearchConstraintList = new LinkedList<>();

        if (UtilValidate.isNotEmpty(productCategoryId)) {
            productSearchConstraintList.add(new CategoryConstraint(productCategoryId, includeSubCategories, null));
        }

        if (UtilValidate.isNotEmpty(keywordsString)) {
            productSearchConstraintList.add(new KeywordConstraint(keywordsString, anyPrefix, anySuffix, null, isAnd));
        }

        if (UtilValidate.isNotEmpty(featureIdSet)) {
            for (String productFeatureId: featureIdSet) {
                productSearchConstraintList.add(new FeatureConstraint(productFeatureId, null));
            }
        }

        return searchProducts(productSearchConstraintList, new SortKeywordRelevancy(), delegator, visitId);
    }

    public static ArrayList<String> searchProducts(List<ProductSearchConstraint> productSearchConstraintList, ResultSortOrder resultSortOrder, Delegator delegator, String visitId) {
        ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);

        productSearchContext.addProductSearchConstraints(productSearchConstraintList);
        productSearchContext.setResultSortOrder(resultSortOrder);

        ArrayList<String> productIds = productSearchContext.doSearch();
        return productIds;
    }

    public static void getAllSubCategoryIds(String productCategoryId, Set<String> productCategoryIdSet, Delegator delegator, Timestamp nowTimestamp) {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // this will use the Delegator cache as much as possible, but not a dedicated cache because it would get stale to easily and is too much of a pain to maintain in development and production

        // first make sure the current category id is in the Set
        productCategoryIdSet.add(productCategoryId);

        // now find all sub-categories, filtered by effective dates, and call this routine for them
        try {
            List<GenericValue> productCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId", productCategoryId).cache(true).queryList();
            for (GenericValue productCategoryRollup: productCategoryRollupList) {
                String subProductCategoryId = productCategoryRollup.getString("productCategoryId");
                if (productCategoryIdSet.contains(subProductCategoryId)) {
                    // if this category has already been traversed, no use doing it again; this will also avoid infinite loops
                    continue;
                }

                // do the date filtering in the loop to avoid looping through the list twice
                if (EntityUtil.isValueActive(productCategoryRollup, nowTimestamp)) {
                    getAllSubCategoryIds(subProductCategoryId, productCategoryIdSet, delegator, nowTimestamp);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding sub-categories for product search", module);
        }
    }

    public static class ProductSearchContext {
        public int index = 1;
        public List<EntityCondition> entityConditionList = new LinkedList<>();
        public List<String> orderByList = new LinkedList<>();
        public List<String> fieldsToSelect = UtilMisc.toList("mainProductId");
        public DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        public boolean productIdGroupBy = false;
        public boolean includedKeywordSearch = false;
        public Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        public List<Set<String>> keywordFixedOrSetAndList = new LinkedList<>();
        public Set<String> orKeywordFixedSet = new HashSet<>();
        public Set<String> andKeywordFixedSet = new HashSet<>();
        public List<GenericValue> productSearchConstraintList = new LinkedList<>();
        public ResultSortOrder resultSortOrder = null;
        public Integer resultOffset = null;
        public Integer maxResults = null;
        protected Delegator delegator = null;
        protected String visitId = null;
        protected Integer totalResults = null;

        public Set<String> includeCategoryIds = new HashSet<>();
        public Set<String> excludeCategoryIds = new HashSet<>();
        public Set<String> alwaysIncludeCategoryIds = new HashSet<>();

        public List<Set<String>> includeCategoryIdOrSetAndList = new LinkedList<>();
        public List<Set<String>> alwaysIncludeCategoryIdOrSetAndList = new LinkedList<>();

        public Set<String> includeFeatureIds = new HashSet<>();
        public Set<String> excludeFeatureIds = new HashSet<>();
        public Set<String> alwaysIncludeFeatureIds = new HashSet<>();

        public List<Set<String>> includeFeatureIdOrSetAndList = new LinkedList<>();
        public List<Set<String>> alwaysIncludeFeatureIdOrSetAndList = new LinkedList<>();

        public Set<String> includeFeatureCategoryIds = new HashSet<>();
        public Set<String> excludeFeatureCategoryIds = new HashSet<>();
        public Set<String> alwaysIncludeFeatureCategoryIds = new HashSet<>();

        public Set<String> includeFeatureGroupIds = new HashSet<>();
        public Set<String> excludeFeatureGroupIds = new HashSet<>();
        public Set<String> alwaysIncludeFeatureGroupIds = new HashSet<>();

        public List<String> keywordTypeIds = new LinkedList<>();
        public String statusId = null;

        public ProductSearchContext(Delegator delegator, String visitId) {
            this.delegator = delegator;
            this.visitId = visitId;
            dynamicViewEntity.addMemberEntity("PROD", "Product");
            dynamicViewEntity.addMemberEntity("PRODCI", "ProductCalculatedInfo");
            dynamicViewEntity.addViewLink("PROD", "PRODCI", Boolean.TRUE, ModelKeyMap.makeKeyMapList("productId"));
        }

        public Delegator getDelegator() {
            return this.delegator;
        }

        public void addProductSearchConstraints(List<ProductSearchConstraint> productSearchConstraintList) {
            // Go through the constraints and add them in
            for (ProductSearchConstraint constraint: productSearchConstraintList) {
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
            ArrayList<String> productIds = null;
            try (EntityListIterator eli = this.doQuery(delegator)) {
                productIds = this.makeProductIdList(eli);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return null;
            }

            long endMillis = System.currentTimeMillis();
            double totalSeconds = ((double)endMillis - (double)startMillis)/1000.0;

            // store info about results in the database, attached to the user's visitId, if specified
            this.saveSearchResultInfo((long) productIds.size(), totalSeconds);

            return productIds;
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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);

                    // keyword type filter
                    if (UtilValidate.isNotEmpty(keywordTypeIds)) {
                        dynamicViewEntity.addAlias(entityAlias, "keywordTypeId");
                    }

                    // keyword status filter
                    if (UtilValidate.isNotEmpty(statusId)) {
                        dynamicViewEntity.addAlias(entityAlias, "statusId");
                    }

                    dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    entityConditionList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    // keyword type filter
                    if (UtilValidate.isNotEmpty(keywordTypeIds)) {
                        List<EntityCondition> keywordTypeCons = new LinkedList<>();
                        for (String keywordTypeId : keywordTypeIds) {
                            keywordTypeCons.add(EntityCondition.makeCondition("keywordTypeId", EntityOperator.EQUALS, keywordTypeId));
                        }
                        entityConditionList.add(EntityCondition.makeCondition(keywordTypeCons, EntityOperator.OR));
                    }

                    // keyword status filter
                    if (UtilValidate.isNotEmpty(statusId)) {
                        entityConditionList.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
                    }

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    //needed when doingBothAndOr or will get an SQL error
                    if (doingBothAndOr) {
                        dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, Boolean.TRUE, null);
                    }
                    relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, null));
                }

                //TODO: find out why Oracle and other dbs don't like the query resulting from this and fix: productIdGroupBy = true;

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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    List<EntityCondition> keywordOrList = new LinkedList<>();
                    for (String keyword: keywordFixedOrSet) {
                        keywordOrList.add(EntityCondition.makeCondition(prefix + "Keyword", EntityOperator.LIKE, keyword));
                    }
                    entityConditionList.add(EntityCondition.makeCondition(keywordOrList, EntityOperator.OR));

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

        public void finishCategoryAndFeatureConstraints() {
            if (includeCategoryIds.size() == 0 && excludeCategoryIds.size() == 0 && alwaysIncludeCategoryIds.size() == 0 &&
                    includeCategoryIdOrSetAndList.size() == 0 && alwaysIncludeCategoryIdOrSetAndList.size() == 0 &&
                    includeFeatureIds.size() == 0 && excludeFeatureIds.size() == 0 && alwaysIncludeFeatureIds.size() == 0 &&
                    includeFeatureIdOrSetAndList.size() == 0 && alwaysIncludeFeatureIdOrSetAndList.size() == 0 &&
                    includeFeatureCategoryIds.size() == 0 && excludeFeatureCategoryIds.size() == 0 && alwaysIncludeFeatureCategoryIds.size() == 0 &&
                    includeFeatureGroupIds.size() == 0 && excludeFeatureGroupIds.size() == 0 && alwaysIncludeFeatureGroupIds.size() == 0) {
                return;
            }

            // create new view members with logic:
            // ((each Id = category includes AND Id IN feature includes) AND (Id NOT IN category excludes AND Id NOT IN feature excludes))
            // OR (each Id = category alwaysIncludes AND each Id = feature alwaysIncludes)
            List<EntityCondition> incExcCondList = new LinkedList<>();
            EntityCondition incExcCond = null;

            List<EntityCondition> alwIncCondList = new LinkedList<>();
            EntityCondition alwIncCond = null;

            EntityCondition topCond = null;

            if (includeCategoryIds.size() > 0) {
                for (String includeCategoryId: includeCategoryIds) {
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(categoryPrefix + "ProductCategoryId", EntityOperator.EQUALS, includeCategoryId));
                }
            }
            if (includeFeatureIds.size() > 0) {
                for (String includeFeatureId: includeFeatureIds) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "ProductFeatureId", EntityOperator.EQUALS, includeFeatureId));
                }
            }
            if (includeFeatureCategoryIds.size() > 0) {
                for (String includeFeatureCategoryId: includeFeatureCategoryIds) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    String otherFeaturePrefix = "pfe" + this.index;
                    String otherEntityAlias = "PFE" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addMemberEntity(otherEntityAlias, "ProductFeature");
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ProductFeatureCategoryId", "productFeatureCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    this.dynamicViewEntity.addViewLink(entityAlias, otherEntityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productFeatureId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "ProductFeatureCategoryId", EntityOperator.EQUALS, includeFeatureCategoryId));
                }
            }
            if (includeFeatureGroupIds.size() > 0) {
                for (String includeFeatureGroupId: includeFeatureGroupIds) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    String otherFeaturePrefix = "pfga" + this.index;
                    String otherEntityAlias = "PFGA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addMemberEntity(otherEntityAlias, "ProductFeatureGroupAppl");
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ProductFeatureGroupId", "productFeatureGroupId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    this.dynamicViewEntity.addViewLink(entityAlias, otherEntityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productFeatureId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(otherFeaturePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(otherFeaturePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "ProductFeatureGroupId", EntityOperator.EQUALS, includeFeatureGroupId));
                }
            }

            if (excludeCategoryIds.size() > 0) {
                List<EntityCondition> idExcludeCondList = new LinkedList<>();
                idExcludeCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(EntityCondition.makeCondition("productCategoryId", EntityOperator.IN, excludeCategoryIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductCategoryMember", "productId", EntityCondition.makeCondition(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(EntityCondition.makeCondition("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureIds.size() > 0) {
                List<EntityCondition> idExcludeCondList = new LinkedList<>();
                idExcludeCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(EntityCondition.makeCondition("productFeatureId", EntityOperator.IN, excludeFeatureIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductFeatureAppl", "productId", EntityCondition.makeCondition(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(EntityCondition.makeCondition("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureCategoryIds.size() > 0) {
                List<EntityCondition> idExcludeCondList = new LinkedList<>();
                idExcludeCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(EntityCondition.makeCondition("productFeatureCategoryId", EntityOperator.IN, excludeFeatureCategoryIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductFeatureAndAppl", "productId", EntityCondition.makeCondition(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(EntityCondition.makeCondition("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureGroupIds.size() > 0) {
                List<EntityCondition> idExcludeCondList = new LinkedList<>();
                idExcludeCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("groupThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("groupThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(EntityCondition.makeCondition("groupFromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(EntityCondition.makeCondition("productFeatureGroupId", EntityOperator.IN, excludeFeatureGroupIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProdFeaGrpAppAndProdFeaApp", "productId", EntityCondition.makeCondition(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(EntityCondition.makeCondition("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }

            if (alwaysIncludeCategoryIds.size() > 0) {
                String categoryPrefix = "pcm" + this.index;
                String entityAlias = "PCM" + this.index;
                this.index++;

                this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                alwIncCondList.add(EntityCondition.makeCondition(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                alwIncCondList.add(EntityCondition.makeCondition(categoryPrefix + "ProductCategoryId", EntityOperator.IN, alwaysIncludeCategoryIds));
            }
            if (alwaysIncludeFeatureIds.size() > 0) {
                String featurePrefix = "pfa" + this.index;
                String entityAlias = "PFA" + this.index;
                this.index++;

                this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "ProductFeatureId", EntityOperator.IN, alwaysIncludeFeatureIds));
            }
            if (alwaysIncludeFeatureCategoryIds.size() > 0) {
                for (String alwaysIncludeFeatureCategoryId: alwaysIncludeFeatureCategoryIds) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    String otherFeaturePrefix = "pfe" + this.index;
                    String otherEntityAlias = "PFE" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addMemberEntity(otherEntityAlias, "ProductFeature");
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ProductFeatureCategoryId", "productFeatureCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    this.dynamicViewEntity.addViewLink(entityAlias, otherEntityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productFeatureId"));
                    alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "ProductFeatureCategoryId", EntityOperator.EQUALS, alwaysIncludeFeatureCategoryId));
                }
            }
            if (alwaysIncludeFeatureGroupIds.size() > 0) {
                for (String alwaysIncludeFeatureGroupId: alwaysIncludeFeatureGroupIds) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    String otherFeaturePrefix = "pfga" + this.index;
                    String otherEntityAlias = "PFGA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addMemberEntity(otherEntityAlias, "ProductFeatureGroupAppl");
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ProductFeatureGroupId", "productFeatureGroupId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(otherEntityAlias, otherFeaturePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    this.dynamicViewEntity.addViewLink(entityAlias, otherEntityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productFeatureId"));
                    alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(otherFeaturePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(otherFeaturePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(EntityCondition.makeCondition(otherFeaturePrefix + "ProductFeatureGroupId", EntityOperator.EQUALS, alwaysIncludeFeatureGroupId));
                }
            }

            // handle includeFeatureIdOrSetAndList and alwaysIncludeFeatureIdOrSetAndList
            if (includeFeatureIdOrSetAndList.size() > 0) {
                for (Set<String> includeFeatureIdOrSet: includeFeatureIdOrSetAndList) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(featurePrefix + "ProductFeatureId", EntityOperator.IN, includeFeatureIdOrSet));
                }
            }
            if (alwaysIncludeFeatureIdOrSetAndList.size() > 0) {
                for (Set<String> alwaysIncludeFeatureIdOrSet: alwaysIncludeFeatureIdOrSetAndList) {
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(EntityCondition.makeCondition(featurePrefix + "ProductFeatureId", EntityOperator.IN, alwaysIncludeFeatureIdOrSet));
                }
            }

            // handle includeCategoryIdOrSetAndList and alwaysIncludeCategoryIdOrSetAndList
            if (includeCategoryIdOrSetAndList.size() > 0) {
                for (Set<String> includeCategoryIdOrSet: includeCategoryIdOrSetAndList) {
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(EntityCondition.makeCondition(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(EntityCondition.makeCondition(categoryPrefix + "ProductCategoryId", EntityOperator.IN, includeCategoryIdOrSet));
                }
            }
            if (alwaysIncludeCategoryIdOrSetAndList.size() > 0) {
                for (Set<String> alwaysIncludeCategoryIdOrSet: alwaysIncludeCategoryIdOrSetAndList) {
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    alwIncCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(EntityCondition.makeCondition(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(EntityCondition.makeCondition(categoryPrefix + "ProductCategoryId", EntityOperator.IN, alwaysIncludeCategoryIdOrSet));
                }
            }

            if (incExcCondList.size() > 0) {
                incExcCond = EntityCondition.makeCondition(incExcCondList, EntityOperator.AND);
            }
            if (alwIncCondList.size() > 0) {
                alwIncCond = EntityCondition.makeCondition(alwIncCondList, EntityOperator.AND);
            }

            if (incExcCond != null && alwIncCond != null) {
                topCond = EntityCondition.makeCondition(incExcCond, EntityOperator.OR, alwIncCond);
            } else if (incExcCond != null) {
                topCond = incExcCond;
            } else if (alwIncCond != null) {
                topCond = alwIncCond;
            }

            this.entityConditionList.add(topCond);

            if (Debug.infoOn()) {
                Debug.logInfo("topCond=" + topCond.makeWhereString(null, new LinkedList<EntityConditionParam>(), EntityConfig.getDatasource(delegator.getEntityHelperName("Product"))), module);
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

            this.finishCategoryAndFeatureConstraints();

            if (resultSortOrder != null) {
                resultSortOrder.setSortOrder(this);
            }

            dynamicViewEntity.addAlias("PROD", "mainProductId", "productId", null, null, productIdGroupBy, null);

            EntityListIterator eli = null;
            try {
                int queryMaxResults = 0;
                if (maxResults != null) {
                    queryMaxResults = maxResults;
                    if (resultOffset != null) {
                        queryMaxResults += resultOffset - 1;
                    }
                }
                eli = EntityQuery.use(delegator).select(UtilMisc.toSet(fieldsToSelect))
                        .from(dynamicViewEntity).where(entityConditionList)
                        .orderBy(orderByList)
                        .distinct(true)
                        .maxRows(queryMaxResults)
                        .cursorScrollInsensitive()
                        .queryIterator();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error in product search", module);
                return null;
            }

            return eli;
        }

        public ArrayList<String> makeProductIdList(EntityListIterator eli) {
            ArrayList<String> productIds = new ArrayList<>(maxResults == null ? 100 : maxResults);
            if (eli == null) {
                Debug.logWarning("The eli is null, returning zero results", module);
                return productIds;
            }

            try {
                boolean hasResults = false;
                Object initialResult = null;
                initialResult = eli.next();
                if (initialResult != null) {
                    hasResults = true;
                }
                if (resultOffset != null && resultOffset > 1) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Before relative, current index=" + eli.currentIndex(), module);
                    }
                    hasResults = eli.relative(resultOffset - 1);
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
                        failTotal = this.resultOffset - 1;
                    }
                    this.totalResults = failTotal;
                    return productIds;
                }


                // init numRetreived to one since we have already grabbed the initial one
                int numRetreived = 1;
                int duplicatesFound = 0;

                Set<String> productIdSet = new HashSet<>();

                productIds.add(searchResult.getString("mainProductId"));
                productIdSet.add(searchResult.getString("mainProductId"));

                while ((maxResults == null || numRetreived < maxResults) && ((searchResult = eli.next()) != null)) {
                    String productId = searchResult.getString("mainProductId");
                    if (!productIdSet.contains(productId)) {
                        productIds.add(productId);
                        productIdSet.add(productId);
                        numRetreived++;
                    } else {
                        duplicatesFound++;
                    }
                }

                if (searchResult != null) {
                    this.totalResults = eli.getResultsSizeAfterPartialList();
                }
                if (this.totalResults == null || this.totalResults == 0) {
                    int total = numRetreived;
                    if (this.resultOffset != null) {
                        total += (this.resultOffset - 1);
                    }
                    this.totalResults = total;
                }

                Debug.logInfo("Got search values, numRetreived=" + numRetreived + ", totalResults=" + totalResults + ", maxResults=" + maxResults + ", resultOffset=" + resultOffset + ", duplicatesFound(in the current results)=" + duplicatesFound, module);

            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting results from the product search query", module);
            }
            return productIds;
        }

        public void saveSearchResultInfo(Long numResults, Double secondsTotal) {
            // uses entities: ProductSearchResult and ProductSearchConstraint

            try {
                // make sure this is in a transaction
                boolean beganTransaction = TransactionUtil.begin();

                try {

                    GenericValue productSearchResult = delegator.makeValue("ProductSearchResult");
                    String productSearchResultId = delegator.getNextSeqId("ProductSearchResult");

                    productSearchResult.set("productSearchResultId", productSearchResultId);
                    productSearchResult.set("visitId", this.visitId);
                    if (this.resultSortOrder != null) {
                        productSearchResult.set("orderByName", this.resultSortOrder.getOrderName());
                        productSearchResult.set("isAscending", this.resultSortOrder.isAscending() ? "Y" : "N");
                    }
                    productSearchResult.set("numResults", numResults);
                    productSearchResult.set("secondsTotal", secondsTotal);
                    productSearchResult.set("searchDate", nowTimestamp);
                    productSearchResult.create();

                    int seqId = 1;
                    for (GenericValue productSearchConstraint: productSearchConstraintList) {
                        productSearchConstraint.set("productSearchResultId", productSearchResultId);
                        productSearchConstraint.set("constraintSeqId", Integer.toString(seqId));
                        productSearchConstraint.create();
                        seqId++;
                    }

                    TransactionUtil.commit(beganTransaction);
                } catch (GenericEntityException e1) {
                    String errMsg = "Error saving product search result info/stats";
                    Debug.logError(e1, errMsg, module);
                    TransactionUtil.rollback(beganTransaction, errMsg, e1);
                }
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error saving product search result info/stats", module);
            }
        }
    }

    // ======================================================================
    // Search Constraint Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class ProductSearchConstraint implements java.io.Serializable {
        public ProductSearchConstraint() { }

        public abstract void addConstraint(ProductSearchContext productSearchContext);
        /** pretty print for log messages and even UI stuff */
        public abstract String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale);
        public abstract String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale);
    }


    @SuppressWarnings("serial")
    public static class CatalogConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Catalog";
        protected String prodCatalogId;
        protected List<GenericValue> productCategories;

        public CatalogConstraint(String prodCatalogId, List<GenericValue> productCategories) {
            this.prodCatalogId = prodCatalogId;
            this.productCategories = productCategories;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            List<String> productCategoryIds = new LinkedList<>();
            for (GenericValue category: productCategories) {
                productCategoryIds.add(category.getString("productCategoryId"));
            }

            // make index based values and increment
            String entityAlias = "PCM" + productSearchContext.index;
            String prefix = "pcm" + productSearchContext.index;
            productSearchContext.index++;

            productSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductCategoryId", EntityOperator.IN, productCategoryIds));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, productSearchContext.nowTimestamp));

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.prodCatalogId)));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue prodCatalog = null;
            try {
                prodCatalog = EntityQuery.use(delegator).from("ProdCatalog").where("prodCatalogId", prodCatalogId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProdCatalog information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ProductCatalog", locale)).append(": ");
            if (prodCatalog != null) {
                ppBuf.append(prodCatalog.getString("catalogName"));
            }
            return ppBuf.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((prodCatalogId == null) ? 0 : prodCatalogId.hashCode());
            result = prime * result + ((productCategories == null) ? 0 : productCategories.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CatalogConstraint)) {
                return false;
            }
            CatalogConstraint other = (CatalogConstraint) obj;
            if (prodCatalogId == null) {
                if (other.prodCatalogId != null) {
                    return false;
                }
            } else if (!prodCatalogId.equals(other.prodCatalogId)) {
                return false;
            }
            if (productCategories == null) {
                if (other.productCategories != null) {
                    return false;
                }
            } else if (!productCategories.equals(other.productCategories)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class CategoryConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Category";
        protected String productCategoryId;
        protected boolean includeSubCategories;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         *
         * @param productCategoryId
         * @param includeSubCategories
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public CategoryConstraint(String productCategoryId, boolean includeSubCategories, Boolean exclude) {
            this.productCategoryId = productCategoryId;
            this.includeSubCategories = includeSubCategories;
            this.exclude = exclude;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            Set<String> productCategoryIdSet = new HashSet<>();
            if (includeSubCategories) {
                // find all sub-categories recursively, make a Set of productCategoryId
                ProductSearch.getAllSubCategoryIds(productCategoryId, productCategoryIdSet, productSearchContext.getDelegator(), productSearchContext.nowTimestamp);
            } else {
                productCategoryIdSet.add(productCategoryId);
            }

            // just add to global sets
            if (exclude == null) {
                productSearchContext.includeCategoryIdOrSetAndList.add(productCategoryIdSet);
            } else if (exclude.equals(Boolean.TRUE)) {
                productSearchContext.excludeCategoryIds.addAll(productCategoryIdSet);
            } else if (exclude.equals(Boolean.FALSE)) {
                productSearchContext.alwaysIncludeCategoryIdOrSetAndList.add(productCategoryIdSet);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.productCategoryId, "includeSubCategories", this.includeSubCategories ? "Y" : "N")));
        }

        /** pretty print for log messages and even UI stuff */
        // TODO This is not used OOTB since OFBIZ-9164 and could simply return null, kept for custom projects
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue productCategory = null;
            try {
                productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductCategory information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ProductCategory", locale)).append(": ");
            if (productCategory != null) {
                String catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "CATEGORY_NAME", locale, null, "html");
                if (UtilValidate.isEmpty(catInfo)) {
                    catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "DESCRIPTION", locale, null, "html");
                }
                ppBuf.append(catInfo);
            }
            if (productCategory == null || detailed) {
                ppBuf.append(" [");
                ppBuf.append(productCategoryId);
                ppBuf.append("]");
            }
            if (includeSubCategories) {
                ppBuf.append(" (").append(UtilProperties.getMessage(resource, "ProductIncludeAllSubCategories", locale)).append(")");
            }
            return ppBuf.toString();
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            GenericValue productCategory = null;
            GenericDelegator delegator = (GenericDelegator) dispatcher.getDelegator();
            try {
                productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductCategory information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ProductCategory", locale)).append(": ");
            if (productCategory != null) {
                String catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "CATEGORY_NAME", locale, dispatcher, "html");
                if (UtilValidate.isEmpty(catInfo)) {
                    catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "DESCRIPTION", locale, dispatcher, "html");
                }
                ppBuf.append(catInfo);
            }
            if (productCategory == null || detailed) {
                ppBuf.append(" [");
                ppBuf.append(productCategoryId);
                ppBuf.append("]");
            }
            if (includeSubCategories) {
                ppBuf.append(" (").append(UtilProperties.getMessage(resource, "ProductIncludeAllSubCategories", locale)).append(")");
            }
            return ppBuf.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exclude == null) ? 0 : exclude.hashCode());
            result = prime * result + (includeSubCategories ? 1231 : 1237);
            result = prime * result + ((productCategoryId == null) ? 0 : productCategoryId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CategoryConstraint)) {
                return false;
            }
            CategoryConstraint other = (CategoryConstraint) obj;
            if (exclude == null) {
                if (other.exclude != null) {
                    return false;
                }
            } else if (!exclude.equals(other.exclude)) {
                return false;
            }
            if (includeSubCategories != other.includeSubCategories) {
                return false;
            }
            if (productCategoryId == null) {
                if (other.productCategoryId != null) {
                    return false;
                }
            } else if (!productCategoryId.equals(other.productCategoryId)) {
                return false;
            }
            return true;
        }

    }

    @SuppressWarnings("serial")
    public static class FeatureConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Feature";
        protected String productFeatureId;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         *
         * @param productFeatureId
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public FeatureConstraint(String productFeatureId, Boolean exclude) {
            this.productFeatureId = productFeatureId;
            this.exclude = exclude;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // just add to global sets
            if (exclude == null) {
                productSearchContext.includeFeatureIds.add(productFeatureId);
            } else if (exclude.equals(Boolean.TRUE)) {
                productSearchContext.excludeFeatureIds.add(productFeatureId);
            } else if (exclude.equals(Boolean.FALSE)) {
                productSearchContext.alwaysIncludeFeatureIds.add(productFeatureId);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.productFeatureId)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeature = null;
            GenericValue productFeatureType = null;
            try {
                productFeature = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureId", productFeatureId).cache().queryOne();
                productFeatureType = productFeature == null ? null : productFeature.getRelatedOne("ProductFeatureType", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeature and Type information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            if (productFeatureType == null) {
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeature", locale)).append(": ");
                ppBuf.append("[").append(this.productFeatureId).append("]");
            } else {
                // TODO getString to be localized like get("description", locale)
                ppBuf.append(productFeatureType.getString("description")).append(": ");
                ppBuf.append(productFeature.getString("description"));
            }
            if (this.exclude != null) {
                if (Boolean.TRUE.equals(this.exclude)) {
                    ppBuf.append(" (Exclude)");
                } else {
                    ppBuf.append(" (Always Include)");
                }
            }
            return (ppBuf.toString());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exclude == null) ? 0 : exclude.hashCode());
            result = prime * result + ((productFeatureId == null) ? 0 : productFeatureId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FeatureConstraint)) {
                return false;
            }
            FeatureConstraint other = (FeatureConstraint) obj;
            if (exclude == null) {
                if (other.exclude != null) {
                    return false;
                }
            } else if (!exclude.equals(other.exclude)) {
                return false;
            }
            if (productFeatureId == null) {
                if (other.productFeatureId != null) {
                    return false;
                }
            } else if (!productFeatureId.equals(other.productFeatureId)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }


    @SuppressWarnings("serial")
    public static class FeatureCategoryConstraint extends ProductSearchConstraint {
        public static final String constraintName = "FeatureCategory";
        protected String productFeatureCategoryId;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         *
         * @param productFeatureCategoryId
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public FeatureCategoryConstraint(String productFeatureCategoryId, Boolean exclude) {
            this.productFeatureCategoryId = productFeatureCategoryId;
            this.exclude = exclude;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // just add to global sets
            if (exclude == null) {
                productSearchContext.includeFeatureCategoryIds.add(productFeatureCategoryId);
            } else if (exclude.equals(Boolean.TRUE)) {
                productSearchContext.excludeFeatureCategoryIds.add(productFeatureCategoryId);
            } else if (exclude.equals(Boolean.FALSE)) {
                productSearchContext.alwaysIncludeFeatureCategoryIds.add(productFeatureCategoryId);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.productFeatureCategoryId)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeatureCategory = null;
            try {
                productFeatureCategory = EntityQuery.use(delegator).from("ProductFeatureCategory").where("productFeatureCategoryId", productFeatureCategoryId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeatureCategory and Type information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            if (productFeatureCategory != null) {
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeatureCategory", locale)).append(": ");
                if (productFeatureCategory.get("description") != null) {
                    ppBuf.append(productFeatureCategory.get("description"));
                } else {
                    ppBuf.append("[").append(this.productFeatureCategoryId).append("]");
                }

            }
            if (this.exclude != null) {
                if (Boolean.TRUE.equals(this.exclude)) {
                    ppBuf.append(" (Exclude)");
                } else {
                    ppBuf.append(" (Always Include)");
                }
            }
            return (ppBuf.toString());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exclude == null) ? 0 : exclude.hashCode());
            result = prime * result + ((productFeatureCategoryId == null) ? 0 : productFeatureCategoryId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FeatureCategoryConstraint)) {
                return false;
            }
            FeatureCategoryConstraint other = (FeatureCategoryConstraint) obj;
            if (exclude == null) {
                if (other.exclude != null) {
                    return false;
                }
            } else if (!exclude.equals(other.exclude)) {
                return false;
            }
            if (productFeatureCategoryId == null) {
                if (other.productFeatureCategoryId != null) {
                    return false;
                }
            } else if (!productFeatureCategoryId.equals(other.productFeatureCategoryId)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class FeatureGroupConstraint extends ProductSearchConstraint {
        public static final String constraintName = "FeatureGroup";
        protected String productFeatureGroupId;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         *
         * @param productFeatureGroupId
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public FeatureGroupConstraint(String productFeatureGroupId, Boolean exclude) {
            this.productFeatureGroupId = productFeatureGroupId;
            this.exclude = exclude;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // just add to global sets
            if (exclude == null) {
                productSearchContext.includeFeatureGroupIds.add(productFeatureGroupId);
            } else if (exclude.equals(Boolean.TRUE)) {
                productSearchContext.excludeFeatureGroupIds.add(productFeatureGroupId);
            } else if (exclude.equals(Boolean.FALSE)) {
                productSearchContext.alwaysIncludeFeatureGroupIds.add(productFeatureGroupId);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.productFeatureGroupId)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeatureGroup = null;
            try {
                productFeatureGroup = EntityQuery.use(delegator).from("ProductFeatureGroup").where("productFeatureGroupId", productFeatureGroupId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeatureGroup and Type information for constraint pretty print", module);
            }
            StringBuilder ppBuf = new StringBuilder();
            if (productFeatureGroup != null) {
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeatureGroup", locale)).append(": ");
                if (productFeatureGroup.get("description") != null) {
                    ppBuf.append(productFeatureGroup.get("description"));
                } else {
                    ppBuf.append("[").append(this.productFeatureGroupId).append("]");
                }
            }
            if (this.exclude != null) {
                if (Boolean.TRUE.equals(this.exclude)) {
                    ppBuf.append(" (Exclude)");
                } else {
                    ppBuf.append(" (Always Include)");
                }
            }
            return (ppBuf.toString());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exclude == null) ? 0 : exclude.hashCode());
            result = prime * result + ((productFeatureGroupId == null) ? 0 : productFeatureGroupId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FeatureGroupConstraint)) {
                return false;
            }
            FeatureGroupConstraint other = (FeatureGroupConstraint) obj;
            if (exclude == null) {
                if (other.exclude != null) {
                    return false;
                }
            } else if (!exclude.equals(other.exclude)) {
                return false;
            }
            if (productFeatureGroupId == null) {
                if (other.productFeatureGroupId != null) {
                    return false;
                }
            } else if (!productFeatureGroupId.equals(other.productFeatureGroupId)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }


    @SuppressWarnings("serial")
    public static class FeatureSetConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Feature Set";
        protected Set<String> productFeatureIdSet;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         *
         * @param productFeatureIdSet
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public FeatureSetConstraint(Collection<String> productFeatureIdSet, Boolean exclude) {
            this.productFeatureIdSet = new HashSet<>();
            this.productFeatureIdSet.addAll(productFeatureIdSet);
            this.exclude = exclude;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // just add to global sets
            if (exclude == null) {
                productSearchContext.includeFeatureIdOrSetAndList.add(productFeatureIdSet);
            } else if (exclude.equals(Boolean.TRUE)) {
                productSearchContext.excludeFeatureIds.addAll(productFeatureIdSet);
            } else if (exclude.equals(Boolean.FALSE)) {
                productSearchContext.alwaysIncludeFeatureIdOrSetAndList.add(productFeatureIdSet);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            StringBuilder featureIdInfo = new StringBuilder();
            for (String featureId: this.productFeatureIdSet) {
                if (featureIdInfo.length() > 0) {
                    featureIdInfo.append(",");
                }
                featureIdInfo.append(featureId);
            }

            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", featureIdInfo.toString())));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder infoOut = new StringBuilder();
            try {
                for (String featureId: this.productFeatureIdSet) {
                    if (infoOut.length() > 0) {
                        infoOut.append(", ");
                    }
                    GenericValue productFeature = EntityQuery.use(delegator).from("ProductFeature").where("productFeatureId", featureId).cache().queryOne();
                    GenericValue productFeatureType = productFeature == null ? null : productFeature.getRelatedOne("ProductFeatureType", true);
                    if (productFeatureType == null) {
                        infoOut.append(UtilProperties.getMessage(resource, "ProductFeature", locale)).append(": ");
                    } else {
                        infoOut.append(productFeatureType.getString("description"));
                        infoOut.append(": ");
                    }
                    if (productFeature == null) {
                        infoOut.append("[");
                        infoOut.append(featureId);
                        infoOut.append("]");
                    } else {
                        infoOut.append(productFeature.getString("description"));
                    }

                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeature and Type information for constraint pretty print", module);
            }

            return infoOut.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exclude == null) ? 0 : exclude.hashCode());
            result = prime * result + ((productFeatureIdSet == null) ? 0 : productFeatureIdSet.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FeatureSetConstraint)) {
                return false;
            }
            FeatureSetConstraint other = (FeatureSetConstraint) obj;
            if (exclude == null) {
                if (other.exclude != null) {
                    return false;
                }
            } else if (!exclude.equals(other.exclude)) {
                return false;
            }
            if (productFeatureIdSet == null) {
                if (other.productFeatureIdSet != null) {
                    return false;
                }
            } else if (!productFeatureIdSet.equals(other.productFeatureIdSet)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class KeywordConstraint extends ProductSearchConstraint {
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
                this.removeStems = removeStems;
            } else {
                this.removeStems = UtilProperties.propertyValueEquals("keywordsearch", "remove.stems", "true");
            }
        }

        public Set<String> makeFullKeywordSet(Delegator delegator) {
            Set<String> keywordSet = KeywordSearchUtil.makeKeywordSet(this.keywordsString, null, true);
            Set<String> fullKeywordSet = new TreeSet<>();

            // expand the keyword list according to the thesaurus and create a new set of keywords
            for (String keyword: keywordSet) {
                Set<String> expandedSet = new TreeSet<>();
                boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, delegator);
                fullKeywordSet.addAll(expandedSet);
                if (!replaceEntered) {
                    fullKeywordSet.add(keyword);
                }
            }

            return fullKeywordSet;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
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
                    Set<String> expandedSet = new TreeSet<>();
                    boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, productSearchContext.getDelegator());
                    if (!replaceEntered) {
                        expandedSet.add(keyword);
                    }
                    Set<String> fixedSet = KeywordSearchUtil.fixKeywordsForSearch(expandedSet, anyPrefix, anySuffix, removeStems, isAnd);
                    Set<String> fixedKeywordSet = new HashSet<>();
                    fixedKeywordSet.addAll(fixedSet);
                    productSearchContext.keywordFixedOrSetAndList.add(fixedKeywordSet);
                }
            } else {
                // when isAnd is false, just add all of the new entries to the big list
                Set<String> keywordFirstPass = makeFullKeywordSet(productSearchContext.getDelegator()); // includes keyword expansion, etc
                Set<String> keywordSet = KeywordSearchUtil.fixKeywordsForSearch(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);
                productSearchContext.orKeywordFixedSet.addAll(keywordSet);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            Map<String, String> valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.keywordsString);
            valueMap.put("anyPrefix", this.anyPrefix ? "Y" : "N");
            valueMap.put("anySuffix", this.anySuffix ? "Y" : "N");
            valueMap.put("isAnd", this.isAnd ? "Y" : "N");
            valueMap.put("removeStems", this.removeStems ? "Y" : "N");
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", valueMap));
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder ppBuf = new StringBuilder();
            ppBuf.append(UtilProperties.getMessage(resource, "ProductKeywords", locale)).append(": \"");
            ppBuf.append(this.keywordsString).append("\", ").append(UtilProperties.getMessage(resource, "ProductKeywordWhere", locale)).append(" ");
            ppBuf.append(isAnd ? UtilProperties.getMessage(resource, "ProductKeywordAllWordsMatch", locale) : UtilProperties.getMessage(resource, "ProductKeywordAnyWordMatches", locale));
            return ppBuf.toString();
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof KeywordConstraint)) {
                return false;
            }
            KeywordConstraint other = (KeywordConstraint) obj;
            if (anyPrefix != other.anyPrefix) {
                return false;
            }
            if (anySuffix != other.anySuffix) {
                return false;
            }
            if (isAnd != other.isAnd) {
                return false;
            }
            if (keywordsString == null) {
                if (other.keywordsString != null) {
                    return false;
                }
            } else if (!keywordsString.equals(other.keywordsString)) {
                return false;
            }
            if (removeStems != other.removeStems) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class LastUpdatedRangeConstraint extends ProductSearchConstraint {
        public static final String constraintName = "LastUpdatedRange";
        protected Timestamp fromDate;
        protected Timestamp thruDate;

        public LastUpdatedRangeConstraint(Timestamp fromDate, Timestamp thruDate) {
            this.fromDate = fromDate;
            this.thruDate = thruDate;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // TODO: implement LastUpdatedRangeConstraint makeEntityCondition
        }

        /** pretty print for log messages and even UI stuff */
        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            // TODO: implement the pretty print for log messages and even UI stuff
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fromDate == null) ? 0 : fromDate.hashCode());
            result = prime * result + ((thruDate == null) ? 0 : thruDate.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof LastUpdatedRangeConstraint)) {
                return false;
            }
            LastUpdatedRangeConstraint other = (LastUpdatedRangeConstraint) obj;
            if (fromDate == null) {
                if (other.fromDate != null) {
                    return false;
                }
            } else if (!fromDate.equals(other.fromDate)) {
                return false;
            }
            if (thruDate == null) {
                if (other.thruDate != null) {
                    return false;
                }
            } else if (!thruDate.equals(other.thruDate)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class StoreGroupPriceConstraint extends ProductSearchConstraint {
        public static final String constraintName = "StoreGroupPrice";
        protected String productStoreGroupId;
        protected String productPriceTypeId;
        protected String currencyUomId;

        public StoreGroupPriceConstraint(String productStoreGroupId, String productPriceTypeId, String currencyUomId) {
            this.productStoreGroupId = productStoreGroupId;
            this.productPriceTypeId = productPriceTypeId == null ? "LIST_PRICE" : productPriceTypeId;
            this.currencyUomId = currencyUomId;
        }

        @Override
        public void addConstraint(ProductSearchContext context) {
            String entityAlias = "PSPP" + context.index;
            String prefix = "PSPP" + context.index;
            context.dynamicViewEntity.addMemberEntity(entityAlias, "ProductPrice");

            context.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductPriceTypeId", "productPriceTypeId", null, null, null, null);
            context.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductPricePurposeId", "productPricePurposeId", null, null, null, null);
            context.dynamicViewEntity.addAlias(entityAlias, prefix + "CurrencyUomId", "currencyUomId", null, null, null, null);
            context.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductStoreGroupId", "productStoreGroupId", null, null, null, null);
            context.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            context.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);

            context.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));

            context.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductPriceTypeId", EntityOperator.EQUALS, productPriceTypeId));
            context.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductPricePurposeId", EntityOperator.EQUALS, "PURCHASE"));
            context.entityConditionList.add(EntityCondition.makeCondition(prefix + "CurrencyUomId", EntityOperator.EQUALS, currencyUomId));
            context.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductStoreGroupId", EntityOperator.EQUALS, productStoreGroupId));
            context.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, context.nowTimestamp)));
            context.entityConditionList.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, context.nowTimestamp));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            StringBuilder buff = new StringBuilder();
            buff.append("Product Store Mandatory Price Constraint: ");
            buff.append("Product Store Group [").append(productStoreGroupId).append("], ");
            buff.append("Product Price Type [").append(productPriceTypeId).append("], ");
            buff.append("Currency [").append(currencyUomId).append("].");
            return buff.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((currencyUomId == null) ? 0 : currencyUomId.hashCode());
            result = prime * result + ((productPriceTypeId == null) ? 0 : productPriceTypeId.hashCode());
            result = prime * result + ((productStoreGroupId == null) ? 0 : productStoreGroupId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof StoreGroupPriceConstraint)) {
                return false;
            }
            StoreGroupPriceConstraint other = (StoreGroupPriceConstraint) obj;
            if (currencyUomId == null) {
                if (other.currencyUomId != null) {
                    return false;
                }
            } else if (!currencyUomId.equals(other.currencyUomId)) {
                return false;
            }
            if (productPriceTypeId == null) {
                if (other.productPriceTypeId != null) {
                    return false;
                }
            } else if (!productPriceTypeId.equals(other.productPriceTypeId)) {
                return false;
            }
            if (productStoreGroupId == null) {
                if (other.productStoreGroupId != null) {
                    return false;
                }
            } else if (!productStoreGroupId.equals(other.productStoreGroupId)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class ListPriceRangeConstraint extends ProductSearchConstraint {
        public static final String constraintName = "ListPriceRange";
        protected BigDecimal lowPrice;
        protected BigDecimal highPrice;
        protected String currencyUomId;

        public ListPriceRangeConstraint(BigDecimal lowPrice, BigDecimal highPrice, String currencyUomId) {
            this.lowPrice = lowPrice;
            this.highPrice = highPrice;
            this.currencyUomId = UtilValidate.isNotEmpty(currencyUomId) ? currencyUomId : "USD";
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // make index based values and increment
            String entityAlias = "PP" + productSearchContext.index;
            String prefix = "pp" + productSearchContext.index;
            productSearchContext.index++;

            productSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "ProductPrice");

            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductPriceTypeId", "productPriceTypeId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductPricePurposeId", "productPricePurposeId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "CurrencyUomId", "currencyUomId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ProductStoreGroupId", "productStoreGroupId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "Price", "price", null, null, null, null);

            productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));

            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductPriceTypeId", EntityOperator.EQUALS, "LIST_PRICE"));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductPricePurposeId", EntityOperator.EQUALS, "PURCHASE"));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "CurrencyUomId", EntityOperator.EQUALS, currencyUomId));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "ProductStoreGroupId", EntityOperator.EQUALS, "_NA_"));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition(prefix + "ThruDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "FromDate", EntityOperator.LESS_THAN, productSearchContext.nowTimestamp));
            if (this.lowPrice != null) {
                productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "Price", EntityOperator.GREATER_THAN_EQUAL_TO, this.lowPrice));
            }
            if (this.highPrice != null) {
                productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "Price", EntityOperator.LESS_THAN_EQUAL_TO, this.highPrice));
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint",
                    UtilMisc.toMap("constraintName", constraintName, "infoString", "low [" + this.lowPrice + "] high [" + this.highPrice + "] currency [" + this.currencyUomId + "]")));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            if (this.lowPrice == null && this.highPrice == null) {
                // dummy constraint, no values
                return null;
            }
            StringBuilder msgBuf = new StringBuilder();
            msgBuf.append(UtilProperties.getMessage(resource, "ProductListPriceRange", locale));
            msgBuf.append(": ");

            // NOTE: at this point we know that only one or none are null
            if (this.lowPrice == null) {
                msgBuf.append(UtilProperties.getMessage(resourceCommon, "CommonLessThan", locale));
                msgBuf.append(" ");
                msgBuf.append(this.highPrice);
            } else if (this.highPrice == null) {
                msgBuf.append(UtilProperties.getMessage(resourceCommon, "CommonMoreThan", locale));
                msgBuf.append(" ");
                msgBuf.append(this.lowPrice);
            } else {
                msgBuf.append(this.lowPrice);
                msgBuf.append(" - ");
                msgBuf.append(this.highPrice);
            }
            return msgBuf.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((currencyUomId == null) ? 0 : currencyUomId.hashCode());
            result = prime * result + ((highPrice == null) ? 0 : highPrice.hashCode());
            result = prime * result + ((lowPrice == null) ? 0 : lowPrice.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ListPriceRangeConstraint)) {
                return false;
            }
            ListPriceRangeConstraint other = (ListPriceRangeConstraint) obj;
            if (currencyUomId == null) {
                if (other.currencyUomId != null) {
                    return false;
                }
            } else if (!currencyUomId.equals(other.currencyUomId)) {
                return false;
            }
            if (highPrice == null) {
                if (other.highPrice != null) {
                    return false;
                }
            } else if (!highPrice.equals(other.highPrice)) {
                return false;
            }
            if (lowPrice == null) {
                if (other.lowPrice != null) {
                    return false;
                }
            } else if (!lowPrice.equals(other.lowPrice)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class SupplierConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Supplier";
        protected String supplierPartyId;

        public SupplierConstraint(String supplierPartyId) {
            this.supplierPartyId = supplierPartyId;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            // make index based values and increment
            String entityAlias = "SP" + productSearchContext.index;
            String prefix = "sp" + productSearchContext.index;
            productSearchContext.index++;

            productSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "SupplierProduct");
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "SupplierPartyId", "partyId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "SupplierPartyId", EntityOperator.EQUALS, supplierPartyId));

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.supplierPartyId)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductSupplier", locale)+": " + PartyHelper.getPartyName(delegator, supplierPartyId, false);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((supplierPartyId == null) ? 0 : supplierPartyId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof SupplierConstraint)) {
                return false;
            }
            SupplierConstraint other = (SupplierConstraint) obj;
            if (supplierPartyId == null) {
                if (other.supplierPartyId != null) {
                    return false;
                }
            } else if (!supplierPartyId.equals(other.supplierPartyId)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    @SuppressWarnings("serial")
    public static class ExcludeVariantsConstraint extends ProductSearchConstraint {
        public static final String constraintName = "ExcludeVariants";

        public ExcludeVariantsConstraint() {
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodIsVariant", "isVariant", null, null, null, null);
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("prodIsVariant", EntityOperator.NOT_EQUAL, "Y"));

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", "")));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductExcludeVariants", locale);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ExcludeVariantsConstraint)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }
    }

    @SuppressWarnings("serial")
    public static class AvailabilityDateConstraint extends ProductSearchConstraint {
        public static final String constraintName = "AvailabilityDate";

        public AvailabilityDateConstraint() {
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodIntroductionDate", "introductionDate", null, null, null, null);
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("prodIntroductionDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("prodIntroductionDate", EntityOperator.LESS_THAN_EQUAL_TO, productSearchContext.nowTimestamp)));
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodSalesDiscontinuationDate", "salesDiscontinuationDate", null, null, null, null);
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("prodSalesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("prodSalesDiscontinuationDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", "")));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductFilterByAvailabilityDates", locale);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof AvailabilityDateConstraint)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }
    }

    @SuppressWarnings("serial")
    public static class GoodIdentificationConstraint extends ProductSearchConstraint {
        public static final String constraintName = "GoodIdentification";
        protected String goodIdentificationTypeId;
        protected String goodIdentificationValue;
        protected Boolean include;

        public GoodIdentificationConstraint(String goodIdentificationTypeId, String goodIdentificationValue, Boolean include) {
            this.goodIdentificationTypeId = goodIdentificationTypeId;
            this.goodIdentificationValue = goodIdentificationValue;
            this.include = include;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            if (UtilValidate.isNotEmpty(goodIdentificationTypeId) ||
                UtilValidate.isNotEmpty(goodIdentificationValue) ||
                UtilValidate.isNotEmpty(include)) {

                // make index based values and increment
                String entityAlias = "GI" + productSearchContext.index;
                String prefix = "gi" + productSearchContext.index;
                productSearchContext.index++;


                EntityComparisonOperator<?,?> operator = EntityOperator.EQUALS;

                if (UtilValidate.isNotEmpty(include) && !include) {
                    operator = EntityOperator.NOT_EQUAL;
                }

                productSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "GoodIdentification");

                if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
                    productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "GoodIdentificationTypeId", "goodIdentificationTypeId", null, null, null, null);
                    productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "GoodIdentificationTypeId", operator, goodIdentificationTypeId));
                }

                if (UtilValidate.isNotEmpty(goodIdentificationValue)) {
                    productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "GoodIdentificationValue", "idValue", null, null, null, null);
                    productSearchContext.entityConditionList.add(EntityCondition.makeCondition(prefix + "GoodIdentificationValue", operator, goodIdentificationValue));
                }

                productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));

                productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint",
                        UtilMisc.toMap("constraintName", constraintName, "infoString", "goodIdentificationTypeId [" + this.goodIdentificationTypeId + "] goodIdentificationValue [" + this.goodIdentificationValue + "] include [" + this.include + "]")));
            }
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            if (UtilValidate.isEmpty(goodIdentificationTypeId) &&
                UtilValidate.isEmpty(goodIdentificationValue) &&
                UtilValidate.isEmpty(include)) {
                return null;
            }

            StringBuilder msgBuf = new StringBuilder();

            if (UtilValidate.isNotEmpty(include) && !include) {
                msgBuf.append(UtilProperties.getMessage(resourceCommon, "CommonExclude", locale));
                msgBuf.append(" ");
            } else {
                msgBuf.append(UtilProperties.getMessage(resourceCommon, "CommonInclude", locale));
                msgBuf.append(" ");
            }

            if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
                msgBuf.append(UtilProperties.getMessage(resource, "ProductIdType", locale));
                msgBuf.append(": ");
                msgBuf.append(goodIdentificationTypeId);
                msgBuf.append(" ");
            }

            if (UtilValidate.isNotEmpty(goodIdentificationValue)) {
                msgBuf.append(UtilProperties.getMessage(resource, "ProductIdValue", locale));
                msgBuf.append(" ");
                msgBuf.append(goodIdentificationValue);
            }
            return msgBuf.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if (!(obj instanceof GoodIdentificationConstraint)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }
    }

    @SuppressWarnings("serial")
    public static class ProductFieldConstraint extends ProductSearchConstraint {
        public static final String constraintName = "ProductField";
        protected String keyword;
        protected String productFieldName;

        public ProductFieldConstraint(String keyword, String productFieldName) {
            this.keyword = keyword;
            this.productFieldName = productFieldName;
        }

        @Override
        public void addConstraint(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addAlias("PROD", productFieldName, null, null, null, null, null);
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(productFieldName ,EntityOperator.LIKE, keyword + "%"));
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.keyword)));
        }

        @Override
        public String prettyPrintConstraint(Delegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductKeywords", locale);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
            result = prime * result + ((productFieldName == null) ? 0 : productFieldName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ProductFieldConstraint)) {
                return false;
            }
            ProductFieldConstraint other = (ProductFieldConstraint) obj;
            if (keyword == null) {
                if (other.keyword != null) {
                    return false;
                }
            } else if (!keyword.equals(other.keyword)) {
                return false;
            }
            if (productFieldName == null) {
                if (other.productFieldName != null) {
                    return false;
                }
            } else if (!productFieldName.equals(other.productFieldName)) {
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.apache.ofbiz.product.product.ProductSearch.ProductSearchConstraint#prettyPrintConstraint(org.apache.ofbiz.service.LocalDispatcher, boolean, java.util.Locale)
         */
        @Override
        public String prettyPrintConstraint(LocalDispatcher dispatcher, boolean detailed, Locale locale) {
            return null;
        }

    }

    // ======================================================================
    // Result Sort Classes
    // ======================================================================

    @SuppressWarnings("serial")
    public static abstract class ResultSortOrder implements java.io.Serializable {
        public ResultSortOrder() {
        }

        public abstract void setSortOrder(ProductSearchContext productSearchContext);
        public abstract String getOrderName();
        public abstract String prettyPrintSortOrder(boolean detailed, Locale locale);
        public abstract boolean isAscending();
    }

    @SuppressWarnings("serial")
    public static class SortKeywordRelevancy extends ResultSortOrder {
        public SortKeywordRelevancy() {
        }

        @Override
        public void setSortOrder(ProductSearchContext productSearchContext) {
            if (productSearchContext.includedKeywordSearch) {
                // we have to check this in order to be sure that there is a totalRelevancy to sort by...
                if(productSearchContext.keywordFixedOrSetAndList.size() > 0 || productSearchContext.andKeywordFixedSet.size() > 0) {
                    productSearchContext.orderByList.add("-totalRelevancy");
                    productSearchContext.fieldsToSelect.add("totalRelevancy");
                }
                if (productSearchContext.keywordFixedOrSetAndList.size() > 0) {
                    productSearchContext.productIdGroupBy = true;
                }
            }
        }

        @Override
        public String getOrderName() {
            return "KeywordRelevancy";
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductKeywordRelevancy", locale);
        }

        @Override
        public boolean isAscending() {
            return false;
        }
    }

    @SuppressWarnings("serial")
    public static class SortProductField extends ResultSortOrder {
        protected String fieldName;
        protected boolean ascending;

        /** Some good field names to try might include:
         * [productName]
         * [totalQuantityOrdered] for most popular or most purchased
         * [lastModifiedDate]
         *
         *  You can also include any other field on the Product entity.
         */
        public SortProductField(String fieldName, boolean ascending) {
            this.fieldName = fieldName;
            this.ascending = ascending;
        }

        @Override
        public void setSortOrder(ProductSearchContext productSearchContext) {
            if (productSearchContext.getDelegator().getModelEntity("Product").isField(fieldName)) {
                productSearchContext.dynamicViewEntity.addAlias("PROD", fieldName);
            } else if (productSearchContext.getDelegator().getModelEntity("ProductCalculatedInfo").isField(fieldName)) {
                productSearchContext.dynamicViewEntity.addAlias("PRODCI", fieldName);
            }
            if (ascending) {
                productSearchContext.orderByList.add("+" + fieldName);
            } else {
                productSearchContext.orderByList.add("-" + fieldName);
            }
            productSearchContext.fieldsToSelect.add(fieldName);
        }

        @Override
        public String getOrderName() {
            return "ProductField:" + this.fieldName;
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            if ("productName".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ProductProductName", locale);
            } else if ("totalQuantityOrdered".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ProductPopularityByOrders", locale);
            } else if ("totalTimesViewed".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ProductPopularityByViews", locale);
            } else if ("averageCustomerRating".equals(this.fieldName)) {
                return UtilProperties.getMessage(resource, "ProductCustomerRating", locale);
            }
            return this.fieldName;
        }

        @Override
        public boolean isAscending() {
            return this.ascending;
        }
    }

    @SuppressWarnings("serial")
    public static class SortProductPrice extends ResultSortOrder {
        protected String productPriceTypeId;
        protected String currencyUomId;
        protected String productStoreGroupId;
        protected boolean ascending;

        public SortProductPrice(String productPriceTypeId, boolean ascending) {
            this.productPriceTypeId = productPriceTypeId;
            this.ascending = ascending;
        }

        public SortProductPrice(String productPriceTypeId, String currencyUomId, String productStoreGroupId, boolean ascending) {
            this.productPriceTypeId = productPriceTypeId;
            this.currencyUomId = currencyUomId;
            this.productStoreGroupId = productStoreGroupId;
            this.ascending = ascending;
        }

        @Override
        public void setSortOrder(ProductSearchContext productSearchContext) {
            if (this.currencyUomId == null) {
                this.currencyUomId = UtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD");
            }
            if (this.productStoreGroupId == null) {
                this.productStoreGroupId = "_NA_";
            }

            // SortProductPrice, this will be a bit more complex, need to add a ProductPrice member entity
            productSearchContext.dynamicViewEntity.addMemberEntity("SPPRC", "ProductPrice");
            productSearchContext.dynamicViewEntity.addViewLink("PROD", "SPPRC", Boolean.TRUE, UtilMisc.toList(new ModelKeyMap("productId", "productId")));
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortProductPriceTypeId", "productPriceTypeId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortCurrencyUomId", "currencyUomId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortProductStoreGroupId", "productStoreGroupId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortFromDate", "fromDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortThruDate", "thruDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("SPPRC", "sortPrice", "price", null, null, null, null);

            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortProductPriceTypeId", EntityOperator.EQUALS, this.productPriceTypeId));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortCurrencyUomId", EntityOperator.EQUALS, this.currencyUomId));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortProductStoreGroupId", EntityOperator.EQUALS, this.productStoreGroupId));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortFromDate", EntityOperator.LESS_THAN_EQUAL_TO, productSearchContext.nowTimestamp));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(
                    EntityCondition.makeCondition("sortThruDate", EntityOperator.EQUALS, null), EntityOperator.OR,
                    EntityCondition.makeCondition("sortThruDate", EntityOperator.GREATER_THAN_EQUAL_TO, productSearchContext.nowTimestamp)));

            if (ascending) {
                productSearchContext.orderByList.add("+sortPrice");
            } else {
                productSearchContext.orderByList.add("-sortPrice");
            }
            productSearchContext.fieldsToSelect.add("sortPrice");
        }

        @Override
        public String getOrderName() {
            return "ProductPrice:" + productPriceTypeId;
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            String priceTypeName = null;
            if ("LIST_PRICE".equals(this.productPriceTypeId)) {
                priceTypeName = UtilProperties.getMessage(resource, "ProductListPrice", locale);
            } else if ("DEFAULT_PRICE".equals(this.productPriceTypeId)) {
                priceTypeName = UtilProperties.getMessage(resource, "ProductDefaultPrice", locale);
            } else if ("AVERAGE_COST".equals(this.productPriceTypeId)) {
                priceTypeName = UtilProperties.getMessage(resource, "ProductAverageCost", locale);
            }
            if (priceTypeName == null) {
                priceTypeName = UtilProperties.getMessage(resource, "ProductPrice", locale) + " (";
                if (this.ascending) {
                    priceTypeName += UtilProperties.getMessage(resource, "ProductLowToHigh", locale)+")";
                } else {
                    priceTypeName += UtilProperties.getMessage(resource, "ProductHighToLow", locale)+")";
                }
            }

            return priceTypeName;
        }

        @Override
        public boolean isAscending() {
            return this.ascending;
        }
    }

    @SuppressWarnings("serial")
    public static class SortProductFeature extends ResultSortOrder {
        protected String productFeatureTypeId;
        protected boolean ascending;

        public SortProductFeature(String productFeatureTypeId, boolean ascending) {
            this.productFeatureTypeId = productFeatureTypeId;
            this.ascending = ascending;
        }

        @Override
        public void setSortOrder(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addMemberEntity("PFAPPL", "ProductFeatureAndAppl");
            productSearchContext.dynamicViewEntity.addAlias("PFAPPL", "sortProductFeatureTypeId", "productFeatureTypeId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("PFAPPL", "sortProductFeatureId", "productFeatureId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("PFAPPL", "sortFromDate", "fromDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addAlias("PFAPPL", "sortThruDate", "thruDate", null, null, null, null);
            productSearchContext.dynamicViewEntity.addViewLink("PROD", "PFAPPL", Boolean.TRUE, UtilMisc.toList(new ModelKeyMap("productId", "productId")));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortProductFeatureTypeId", EntityOperator.EQUALS, this.productFeatureTypeId));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition("sortFromDate", EntityOperator.LESS_THAN_EQUAL_TO, productSearchContext.nowTimestamp));
            productSearchContext.entityConditionList.add(EntityCondition.makeCondition(
                    EntityCondition.makeCondition("sortThruDate", EntityOperator.EQUALS, null), EntityOperator.OR,
                    EntityCondition.makeCondition("sortThruDate", EntityOperator.GREATER_THAN_EQUAL_TO, productSearchContext.nowTimestamp)));
            if (ascending) {
                productSearchContext.orderByList.add("+sortProductFeatureId");
            } else {
                productSearchContext.orderByList.add("-sortProductFeatureId");
            }
            productSearchContext.fieldsToSelect.add("sortProductFeatureId");
        }

        @Override
        public String getOrderName() {
            return "ProductFeature:" + this.productFeatureTypeId;
        }

        @Override
        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            String featureTypeName = null;
            if (this.productFeatureTypeId != null) {
                featureTypeName = this.productFeatureTypeId;
            }
            return featureTypeName;
        }

        @Override
        public boolean isAscending() {
            return this.ascending;
        }
    }
}
