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
package org.ofbiz.product.product;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityConditionSubSelect;
import org.ofbiz.entity.condition.EntityConditionValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.product.category.CategoryContentWrapper;


/**
 *  Utilities for product search based on various constraints including categories, features and keywords.
 */
public class ProductSearch {

    public static final String module = ProductSearch.class.getName();
    public static final String resource = "ProductUiLabels";

    public static ArrayList parametricKeywordSearch(Map featureIdByType, String keywordsString, GenericDelegator delegator, String productCategoryId, String visitId, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        Set featureIdSet = FastSet.newInstance();
        if (featureIdByType != null) {
            featureIdSet.addAll(featureIdByType.values());
        }

        return parametricKeywordSearch(featureIdSet, keywordsString, delegator, productCategoryId, true, visitId, anyPrefix, anySuffix, isAnd);
    }

    public static ArrayList parametricKeywordSearch(Set featureIdSet, String keywordsString, GenericDelegator delegator, String productCategoryId, boolean includeSubCategories, String visitId, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        List productSearchConstraintList = FastList.newInstance();

        if (UtilValidate.isNotEmpty(productCategoryId)) {
            productSearchConstraintList.add(new CategoryConstraint(productCategoryId, includeSubCategories, null));
        }

        if (UtilValidate.isNotEmpty(keywordsString)) {
            productSearchConstraintList.add(new KeywordConstraint(keywordsString, anyPrefix, anySuffix, null, isAnd));
        }

        if (featureIdSet != null && featureIdSet.size() > 0) {
            Iterator featureIdIter = featureIdSet.iterator();
            while (featureIdIter.hasNext()) {
                String productFeatureId = (String) featureIdIter.next();
                productSearchConstraintList.add(new FeatureConstraint(productFeatureId, null));
            }
        }

        return searchProducts(productSearchConstraintList, new SortKeywordRelevancy(), delegator, visitId);
    }

    public static ArrayList searchProducts(List productSearchConstraintList, ResultSortOrder resultSortOrder, GenericDelegator delegator, String visitId) {
        ProductSearchContext productSearchContext = new ProductSearchContext(delegator, visitId);

        productSearchContext.addProductSearchConstraints(productSearchConstraintList);
        productSearchContext.setResultSortOrder(resultSortOrder);

        ArrayList productIds = productSearchContext.doSearch();
        return productIds;
    }

    public static void getAllSubCategoryIds(String productCategoryId, Set productCategoryIdSet, GenericDelegator delegator, Timestamp nowTimestamp) {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // this will use the GenericDelegator cache as much as possible, but not a dedicated cache because it would get stale to easily and is too much of a pain to maintain in development and production

        // first make sure the current category id is in the Set
        productCategoryIdSet.add(productCategoryId);

        // now find all sub-categories, filtered by effective dates, and call this routine for them
        try {
            List productCategoryRollupList = delegator.findByAndCache("ProductCategoryRollup", UtilMisc.toMap("parentProductCategoryId", productCategoryId));
            Iterator productCategoryRollupIter = productCategoryRollupList.iterator();
            while (productCategoryRollupIter.hasNext()) {
                GenericValue productCategoryRollup = (GenericValue) productCategoryRollupIter.next();

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
        public List entityConditionList = FastList.newInstance();
        public List orderByList = FastList.newInstance();
        public List fieldsToSelect = UtilMisc.toList("mainProductId");
        public DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        public boolean productIdGroupBy = false;
        public boolean includedKeywordSearch = false;
        public Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        public List keywordFixedOrSetAndList = FastList.newInstance();
        public Set orKeywordFixedSet = FastSet.newInstance();
        public Set andKeywordFixedSet = FastSet.newInstance();
        public List productSearchConstraintList = FastList.newInstance();
        public ResultSortOrder resultSortOrder = null;
        public Integer resultOffset = null;
        public Integer maxResults = null;
        protected GenericDelegator delegator = null;
        protected String visitId = null;
        protected Integer totalResults = null;

        public Set includeCategoryIds = FastSet.newInstance();
        public Set excludeCategoryIds = FastSet.newInstance();
        public Set alwaysIncludeCategoryIds = FastSet.newInstance();

        public List includeCategoryIdOrSetAndList = FastList.newInstance();
        public List alwaysIncludeCategoryIdOrSetAndList = FastList.newInstance();
        
        public Set includeFeatureIds = FastSet.newInstance();
        public Set excludeFeatureIds = FastSet.newInstance();
        public Set alwaysIncludeFeatureIds = FastSet.newInstance();

        public List includeFeatureIdOrSetAndList = FastList.newInstance();
        public List alwaysIncludeFeatureIdOrSetAndList = FastList.newInstance();
        
        public Set includeFeatureCategoryIds = FastSet.newInstance();
        public Set excludeFeatureCategoryIds = FastSet.newInstance();
        public Set alwaysIncludeFeatureCategoryIds = FastSet.newInstance();

        public Set includeFeatureGroupIds = FastSet.newInstance();
        public Set excludeFeatureGroupIds = FastSet.newInstance();
        public Set alwaysIncludeFeatureGroupIds = FastSet.newInstance();

        public ProductSearchContext(GenericDelegator delegator, String visitId) {
            this.delegator = delegator;
            this.visitId = visitId;
            dynamicViewEntity.addMemberEntity("PROD", "Product");
            dynamicViewEntity.addMemberEntity("PRODCI", "ProductCalculatedInfo");
            dynamicViewEntity.addViewLink("PROD", "PRODCI", Boolean.TRUE, ModelKeyMap.makeKeyMapList("productId"));
        }

        public GenericDelegator getDelegator() {
            return this.delegator;
        }

        public void addProductSearchConstraints(List productSearchConstraintList) {
            // Go through the constraints and add them in
            Iterator productSearchConstraintIter = productSearchConstraintList.iterator();
            while (productSearchConstraintIter.hasNext()) {
                ProductSearchConstraint constraint = (ProductSearchConstraint) productSearchConstraintIter.next();
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
            ArrayList productIds = this.makeProductIdList(eli);
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error closing ProductSearch EntityListIterator");
                }
            }
            
            long endMillis = System.currentTimeMillis();
            double totalSeconds = ((double)endMillis - (double)startMillis)/1000.0;

            // store info about results in the database, attached to the user's visitId, if specified
            this.saveSearchResultInfo(new Long(productIds.size()), new Double(totalSeconds));

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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    entityConditionList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    relevancyComplexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight", null, null));
                }

                //TODO: find out why Oracle and other dbs don't like the query resulting from this and fix: productIdGroupBy = true;

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

                    dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    List keywordOrList = FastList.newInstance();
                    Iterator keywordIter = keywordFixedOrSet.iterator();
                    while (keywordIter.hasNext()) {
                        String keyword = (String) keywordIter.next();
                        keywordOrList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));
                    }
                    entityConditionList.add(new EntityConditionList(keywordOrList, EntityOperator.OR));

                    productIdGroupBy = true;

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
            List incExcCondList = FastList.newInstance();
            EntityCondition incExcCond = null;
            
            List alwIncCondList = FastList.newInstance();
            EntityCondition alwIncCond = null;
            
            EntityCondition topCond = null;
            
            if (includeCategoryIds.size() > 0) {
                Iterator includeCategoryIdIter = includeCategoryIds.iterator();
                while (includeCategoryIdIter.hasNext()) {
                    String includeCategoryId = (String) includeCategoryIdIter.next();
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(new EntityExpr(new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(categoryPrefix + "ProductCategoryId", EntityOperator.EQUALS, includeCategoryId));
                }
            }
            if (includeFeatureIds.size() > 0) {
                Iterator includeFeatureIdIter = includeFeatureIds.iterator();
                while (includeFeatureIdIter.hasNext()) {
                    String includeFeatureId = (String) includeFeatureIdIter.next();
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(featurePrefix + "ProductFeatureId", EntityOperator.EQUALS, includeFeatureId));
                }
            }
            if (includeFeatureCategoryIds.size() > 0) {
                Iterator includeFeatureCategoryIdIter = includeFeatureCategoryIds.iterator();
                while (includeFeatureCategoryIdIter.hasNext()) {
                    String includeFeatureCategoryId = (String) includeFeatureCategoryIdIter.next();
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
                    incExcCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(otherFeaturePrefix + "ProductFeatureCategoryId", EntityOperator.EQUALS, includeFeatureCategoryId));
                }
            }
            if (includeFeatureGroupIds.size() > 0) {
                Iterator includeFeatureGroupIdIter = includeFeatureGroupIds.iterator();
                while (includeFeatureGroupIdIter.hasNext()) {
                    String includeFeatureGroupId = (String) includeFeatureGroupIdIter.next();
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
                    incExcCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(new EntityExpr(otherFeaturePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(otherFeaturePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(otherFeaturePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(otherFeaturePrefix + "ProductFeatureGroupId", EntityOperator.EQUALS, includeFeatureGroupId));
                }
            }
            
            if (excludeCategoryIds.size() > 0) {
                List idExcludeCondList = FastList.newInstance();
                idExcludeCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(new EntityExpr("productCategoryId", EntityOperator.IN, excludeCategoryIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductCategoryMember", "productId", new EntityConditionList(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(new EntityExpr("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureIds.size() > 0) {
                List idExcludeCondList = FastList.newInstance();
                idExcludeCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(new EntityExpr("productFeatureId", EntityOperator.IN, excludeFeatureIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductFeatureAppl", "productId", new EntityConditionList(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(new EntityExpr("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureCategoryIds.size() > 0) {
                List idExcludeCondList = FastList.newInstance();
                idExcludeCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(new EntityExpr("productFeatureCategoryId", EntityOperator.IN, excludeFeatureCategoryIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProductFeatureAndAppl", "productId", new EntityConditionList(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(new EntityExpr("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
            }
            if (excludeFeatureGroupIds.size() > 0) {
                List idExcludeCondList = FastList.newInstance();
                idExcludeCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(new EntityExpr(new EntityExpr("groupThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("groupThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                idExcludeCondList.add(new EntityExpr("groupFromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                idExcludeCondList.add(new EntityExpr("productFeatureGroupId", EntityOperator.IN, excludeFeatureGroupIds));
                EntityConditionValue subSelCond = new EntityConditionSubSelect("ProdFeaGrpAppAndProdFeaApp", "productId", new EntityConditionList(idExcludeCondList, EntityOperator.AND), true, delegator);
                incExcCondList.add(new EntityExpr("mainProductId", EntityOperator.NOT_EQUAL, subSelCond));
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
                alwIncCondList.add(new EntityExpr(new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                alwIncCondList.add(new EntityExpr(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                alwIncCondList.add(new EntityExpr(categoryPrefix + "ProductCategoryId", EntityOperator.IN, alwaysIncludeCategoryIds)); 
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
                alwIncCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                alwIncCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                alwIncCondList.add(new EntityExpr(featurePrefix + "ProductFeatureId", EntityOperator.IN, alwaysIncludeFeatureIds)); 
            }
            if (alwaysIncludeFeatureCategoryIds.size() > 0) {
                Iterator alwaysIncludeFeatureCategoryIdIter = alwaysIncludeFeatureCategoryIds.iterator();
                while (alwaysIncludeFeatureCategoryIdIter.hasNext()) {
                    String alwaysIncludeFeatureCategoryId = (String) alwaysIncludeFeatureCategoryIdIter.next();
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
                    alwIncCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(new EntityExpr(otherFeaturePrefix + "ProductFeatureCategoryId", EntityOperator.EQUALS, alwaysIncludeFeatureCategoryId));
                }
            }
            if (alwaysIncludeFeatureGroupIds.size() > 0) {
                Iterator alwaysIncludeFeatureGroupIdIter = alwaysIncludeFeatureGroupIds.iterator();
                while (alwaysIncludeFeatureGroupIdIter.hasNext()) {
                    String alwaysIncludeFeatureGroupId = (String) alwaysIncludeFeatureGroupIdIter.next();
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
                    alwIncCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(new EntityExpr(new EntityExpr(otherFeaturePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(otherFeaturePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(new EntityExpr(otherFeaturePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(new EntityExpr(otherFeaturePrefix + "ProductFeatureGroupId", EntityOperator.EQUALS, alwaysIncludeFeatureGroupId));
                }
            }

            // handle includeFeatureIdOrSetAndList and alwaysIncludeFeatureIdOrSetAndList
            if (includeFeatureIdOrSetAndList.size() > 0) {
                Iterator includeFeatureIdOrSetAndIter = includeFeatureIdOrSetAndList.iterator();
                while (includeFeatureIdOrSetAndIter.hasNext()) {
                    Set includeFeatureIdOrSet = (Set) includeFeatureIdOrSetAndIter.next();
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(featurePrefix + "ProductFeatureId", EntityOperator.IN, includeFeatureIdOrSet));
                }
            }
            if (alwaysIncludeFeatureIdOrSetAndList.size() > 0) {
                Iterator alwaysIncludeFeatureIdOrSetAndIter = alwaysIncludeFeatureIdOrSetAndList.iterator();
                while (alwaysIncludeFeatureIdOrSetAndIter.hasNext()) {
                    Set alwaysIncludeFeatureIdOrSet = (Set) alwaysIncludeFeatureIdOrSetAndIter.next();
                    String featurePrefix = "pfa" + this.index;
                    String entityAlias = "PFA" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, featurePrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    alwIncCondList.add(new EntityExpr(new EntityExpr(featurePrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(featurePrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(new EntityExpr(featurePrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(new EntityExpr(featurePrefix + "ProductFeatureId", EntityOperator.IN, alwaysIncludeFeatureIdOrSet));
                }
            }

            // handle includeCategoryIdOrSetAndList and alwaysIncludeCategoryIdOrSetAndList
            if (includeCategoryIdOrSetAndList.size() > 0) {
                Iterator includeCategoryIdOrSetAndIter = includeCategoryIdOrSetAndList.iterator();
                while (includeCategoryIdOrSetAndIter.hasNext()) {
                    Set includeCategoryIdOrSet = (Set) includeCategoryIdOrSetAndIter.next();
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    incExcCondList.add(new EntityExpr(new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    incExcCondList.add(new EntityExpr(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    incExcCondList.add(new EntityExpr(categoryPrefix + "ProductCategoryId", EntityOperator.IN, includeCategoryIdOrSet));
                }
            }
            if (alwaysIncludeCategoryIdOrSetAndList.size() > 0) {
                Iterator alwaysIncludeCategoryIdOrSetAndIter = alwaysIncludeCategoryIdOrSetAndList.iterator();
                while (alwaysIncludeCategoryIdOrSetAndIter.hasNext()) {
                    Set alwaysIncludeCategoryIdOrSet = (Set) alwaysIncludeCategoryIdOrSetAndIter.next();
                    String categoryPrefix = "pcm" + this.index;
                    String entityAlias = "PCM" + this.index;
                    this.index++;

                    this.dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "FromDate", "fromDate", null, null, null, null);
                    this.dynamicViewEntity.addAlias(entityAlias, categoryPrefix + "ThruDate", "thruDate", null, null, null, null);
                    this.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    alwIncCondList.add(new EntityExpr(new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(categoryPrefix + "ThruDate", EntityOperator.GREATER_THAN, this.nowTimestamp)));
                    alwIncCondList.add(new EntityExpr(categoryPrefix + "FromDate", EntityOperator.LESS_THAN, this.nowTimestamp));
                    alwIncCondList.add(new EntityExpr(categoryPrefix + "ProductCategoryId", EntityOperator.IN, alwaysIncludeCategoryIdOrSet));
                }
            }

            if (incExcCondList.size() > 0) {
                incExcCond = new EntityConditionList(incExcCondList, EntityOperator.AND);
            }
            if (alwIncCondList.size() > 0) {
                alwIncCond = new EntityConditionList(alwIncCondList, EntityOperator.AND);
            }

            if (incExcCond != null && alwIncCond != null) {
                topCond = new EntityExpr(incExcCond, EntityOperator.OR, alwIncCond);
            } else if (incExcCond != null) {
                topCond = incExcCond;
            } else if (alwIncCond != null) {
                topCond = alwIncCond;
            }
            
            this.entityConditionList.add(topCond);
            
            Debug.logInfo("topCond=" + topCond.makeWhereString(null, FastList.newInstance(), EntityConfigUtil.getDatasourceInfo(delegator.getEntityHelperName("Product"))), module);
        }
        
        public EntityListIterator doQuery(GenericDelegator delegator) {
            // handle the now assembled or and and keyword fixed lists
            this.finishKeywordConstraints();

            this.finishCategoryAndFeatureConstraints();
            
            if (resultSortOrder != null) {
                resultSortOrder.setSortOrder(this);
            }

            dynamicViewEntity.addAlias("PROD", "mainProductId", "productId", null, null, new Boolean(productIdGroupBy), null);
            EntityCondition whereCondition = new EntityConditionList(entityConditionList, EntityOperator.AND);
            EntityFindOptions efo = new EntityFindOptions();
            efo.setDistinct(true);
            efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);

            EntityListIterator eli = null;
            try {
                eli = delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, null, fieldsToSelect, orderByList, efo);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error in product search", module);
                return null;
            }

            return eli;
        }

        public ArrayList makeProductIdList(EntityListIterator eli) {
            ArrayList productIds = new ArrayList(maxResults == null ? 100 : maxResults.intValue());
            if (eli == null) {
                Debug.logWarning("The eli is null, returning zero results", module);
                return productIds;
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
                    return productIds;
                }

                
                // init numRetreived to one since we have already grabbed the initial one
                int numRetreived = 1;
                int duplicatesFound = 0;

                Set productIdSet = FastSet.newInstance();
                
                productIds.add(searchResult.getString("mainProductId"));
                productIdSet.add(searchResult.getString("mainProductId"));

                while (((searchResult = (GenericValue) eli.next()) != null) && (maxResults == null || numRetreived < maxResults.intValue())) {
                    String productId = searchResult.getString("mainProductId");
                    if (!productIdSet.contains(productId)) {
                        productIds.add(productId);
                        productIdSet.add(productId);
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

                    GenericValue productSearchResult = delegator.makeValue("ProductSearchResult", null);
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

                    Iterator productSearchConstraintIter = productSearchConstraintList.iterator();
                    int seqId = 1;
                    while (productSearchConstraintIter.hasNext()) {
                        GenericValue productSearchConstraint = (GenericValue) productSearchConstraintIter.next();
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

    public static abstract class ProductSearchConstraint implements java.io.Serializable {
        public ProductSearchConstraint() { }

        public abstract void addConstraint(ProductSearchContext productSearchContext);
        /** pretty print for log messages and even UI stuff */
        public abstract String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale);
    }
    

    public static class CatalogConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Catalog";
        protected String prodCatalogId;                
        protected List productCategories;
                        
        public CatalogConstraint(String prodCatalogId, List productCategories) {
            this.prodCatalogId = prodCatalogId;                   
            this.productCategories = productCategories;          
        }
                 
        public void addConstraint(ProductSearchContext productSearchContext) {           
            List productCategoryIds = FastList.newInstance();
            Iterator itCat = productCategories.iterator();
            while (itCat.hasNext()) {
                GenericValue category = (GenericValue)itCat.next();
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
            productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "ProductCategoryId", EntityOperator.IN, productCategoryIds));
            productSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, productSearchContext.nowTimestamp));
          
            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.prodCatalogId)));
        }
                
        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue prodCatalog = null;
            try {
                prodCatalog = delegator.findByPrimaryKeyCache("ProdCatalog", UtilMisc.toMap("prodCatalogId", prodCatalogId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProdCatalog information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();            
            ppBuf.append(UtilProperties.getMessage(resource, "ProductCatalog", locale)+": ");
            if (prodCatalog != null) {                
                ppBuf.append(prodCatalog.getString("catalogName"));
            }            
            return ppBuf.toString();
        }
        
        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof CatalogConstraint) {
                CatalogConstraint that = (CatalogConstraint) psc;                
                if (this.prodCatalogId == null) {
                    if (that.prodCatalogId != null) {
                        return false;
                    }
                } else {
                    if (!this.prodCatalogId.equals(that.prodCatalogId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }
          
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

        public void addConstraint(ProductSearchContext productSearchContext) {
            Set productCategoryIdSet = FastSet.newInstance();
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
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue productCategory = null;
            try {
                productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductCategory information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();            
            ppBuf.append(UtilProperties.getMessage(resource, "ProductCategory", locale)+": ");
            if (productCategory != null) {
                String catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "CATEGORY_NAME", locale, null);
                if (UtilValidate.isEmpty(catInfo)) {
                    catInfo = CategoryContentWrapper.getProductCategoryContentAsText(productCategory, "DESCRIPTION", locale, null);
                }
                ppBuf.append(catInfo);
            }
            if (productCategory == null || detailed) {
                ppBuf.append(" [");
                ppBuf.append(productCategoryId);
                ppBuf.append("]");
            }
            if (includeSubCategories) {
                ppBuf.append(" ("+UtilProperties.getMessage(resource, "ProductIncludeAllSubCategories", locale)+")");
            }
            return ppBuf.toString();
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof CategoryConstraint) {
                CategoryConstraint that = (CategoryConstraint) psc;
                if (this.includeSubCategories != that.includeSubCategories) {
                    return false;
                }
                if (this.productCategoryId == null) {
                    if (that.productCategoryId != null) {
                        return false;
                    }
                } else {
                    if (!this.productCategoryId.equals(that.productCategoryId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

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

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeature = null;
            GenericValue productFeatureType = null;
            try {
                productFeature = delegator.findByPrimaryKeyCache("ProductFeature", UtilMisc.toMap("productFeatureId", productFeatureId));
                productFeatureType = productFeature == null ? null : productFeature.getRelatedOne("ProductFeatureType");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeature and Type information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();
            if (productFeatureType == null) {                                
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeature", locale)+": ");
                ppBuf.append("[" + this.productFeatureId + "]");
            } else {
                // TODO getString to be localized like get("description", locale)
                ppBuf.append(productFeatureType.getString("description") + ": ");
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

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof FeatureConstraint) {
                FeatureConstraint that = (FeatureConstraint) psc;
                if (this.productFeatureId == null) {
                    if (that.productFeatureId != null) {
                        return false;
                    }
                } else {
                    if (!this.productFeatureId.equals(that.productFeatureId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    
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

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeatureCategory = null;
            try {
                productFeatureCategory = delegator.findByPrimaryKeyCache("ProductFeatureCategory", UtilMisc.toMap("productFeatureCategoryId", productFeatureCategoryId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeatureCategory and Type information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();
            if (productFeatureCategory != null) {                                
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeatureCategory", locale)+": ");
                if(productFeatureCategory.get("description") != null) {
                    ppBuf.append(productFeatureCategory.get("description"));    
                } else {
                    ppBuf.append("[" + this.productFeatureCategoryId + "]");
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

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof FeatureCategoryConstraint) {
                FeatureCategoryConstraint that = (FeatureCategoryConstraint) psc;
                if (this.productFeatureCategoryId == null) {
                    if (that.productFeatureCategoryId != null) {
                        return false;
                    }
                } else {
                    if (!this.productFeatureCategoryId.equals(that.productFeatureCategoryId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

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

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            GenericValue productFeatureGroup = null;
            try {
                productFeatureGroup = delegator.findByPrimaryKeyCache("ProductFeatureGroup", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeatureGroup and Type information for constraint pretty print", module);
            }
            StringBuffer ppBuf = new StringBuffer();
            if (productFeatureGroup != null) {
                ppBuf.append(UtilProperties.getMessage(resource, "ProductFeatureGroup", locale) + ": ");
                if(productFeatureGroup.get("description") != null) {
                    ppBuf.append(productFeatureGroup.get("description"));    
                } else {
                    ppBuf.append("[" + this.productFeatureGroupId + "]");
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

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof FeatureGroupConstraint) {
                FeatureGroupConstraint that = (FeatureGroupConstraint) psc;
                if (this.productFeatureGroupId == null) {
                    if (that.productFeatureGroupId != null) {
                        return false;
                    }
                } else {
                    if (!this.productFeatureGroupId.equals(that.productFeatureGroupId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    
    public static class FeatureSetConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Feature Set";
        protected Set productFeatureIdSet;
        /** This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude */
        protected Boolean exclude;

        /**
         * 
         * @param productFeatureIdSet
         * @param exclude This is a tri-state variable: null = Include, true = Exclude, false = AlwaysInclude
         */
        public FeatureSetConstraint(Collection productFeatureIdSet, Boolean exclude) {
            this.productFeatureIdSet = FastSet.newInstance();
            this.productFeatureIdSet.addAll(productFeatureIdSet);
            this.exclude = exclude;
        }

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
            StringBuffer featureIdInfo = new StringBuffer();
            Iterator featureIdIter = this.productFeatureIdSet.iterator();
            while (featureIdIter.hasNext()) {
                String featureId = (String) featureIdIter.next();
                featureIdInfo.append(featureId);
                if (featureIdIter.hasNext()) {
                    featureIdInfo.append(",");
                }
            }
            
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", featureIdInfo.toString())));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer infoOut = new StringBuffer();
            try {
                Iterator featureIdIter = this.productFeatureIdSet.iterator();
                while (featureIdIter.hasNext()) {
                    String featureId = (String) featureIdIter.next();
                    GenericValue productFeature = delegator.findByPrimaryKeyCache("ProductFeature", UtilMisc.toMap("productFeatureId", featureId));
                    GenericValue productFeatureType = productFeature == null ? null : productFeature.getRelatedOneCache("ProductFeatureType");
                    if (productFeatureType == null) {
                        infoOut.append(UtilProperties.getMessage(resource, "ProductFeature", locale)+": ");
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
                    
                    if (featureIdIter.hasNext()) {
                        infoOut.append(", ");
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProductFeature and Type information for constraint pretty print", module);
            }
            
            return infoOut.toString();
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof FeatureConstraint) {
                FeatureSetConstraint that = (FeatureSetConstraint) psc;
                if (this.productFeatureIdSet == null) {
                    if (that.productFeatureIdSet != null) {
                        return false;
                    }
                } else {
                    if (!this.productFeatureIdSet.equals(that.productFeatureIdSet)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

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

        public void addConstraint(ProductSearchContext productSearchContext) {
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
                    boolean replaceEntered = KeywordSearchUtil.expandKeywordForSearch(keyword, expandedSet, productSearchContext.getDelegator());
                    if (!replaceEntered) {
                        expandedSet.add(keyword);
                    }
                    Set fixedSet = KeywordSearchUtil.fixKeywordsForSearch(expandedSet, anyPrefix, anySuffix, removeStems, isAnd);
                    Set fixedKeywordSet = FastSet.newInstance();
                    fixedKeywordSet.addAll(fixedSet);
                    productSearchContext.keywordFixedOrSetAndList.add(fixedKeywordSet);
                }
            } else {
                // when isAnd is false, just add all of the new entries to the big list
                Set keywordFirstPass = makeFullKeywordSet(productSearchContext.getDelegator()); // includes keyword expansion, etc
                Set keywordSet = KeywordSearchUtil.fixKeywordsForSearch(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);
                productSearchContext.orKeywordFixedSet.addAll(keywordSet);
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            Map valueMap = UtilMisc.toMap("constraintName", constraintName, "infoString", this.keywordsString);
            valueMap.put("anyPrefix", this.anyPrefix ? "Y" : "N");
            valueMap.put("anySuffix", this.anySuffix ? "Y" : "N");
            valueMap.put("isAnd", this.isAnd ? "Y" : "N");
            valueMap.put("removeStems", this.removeStems ? "Y" : "N");
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", valueMap));
        }

        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            StringBuffer ppBuf = new StringBuffer();
            ppBuf.append(UtilProperties.getMessage(resource, "ProductKeywords", locale) + ": \"");
            ppBuf.append(this.keywordsString + "\", " + UtilProperties.getMessage(resource, "ProductKeywordWhere", locale) + " ");
            ppBuf.append(isAnd ? UtilProperties.getMessage(resource, "ProductKeywordAllWordsMatch", locale) : UtilProperties.getMessage(resource, "ProductKeywordAnyWordMatches", locale));            
            return ppBuf.toString();
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
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

    public static class LastUpdatedRangeConstraint extends ProductSearchConstraint {
        public static final String constraintName = "LastUpdatedRange";
        protected Timestamp fromDate;
        protected Timestamp thruDate;

        public LastUpdatedRangeConstraint(Timestamp fromDate, Timestamp thruDate) {
            this.fromDate = fromDate;
            this.thruDate = thruDate;
        }

        public void addConstraint(ProductSearchContext productSearchContext) {
            // TODO: implement LastUpdatedRangeConstraint makeEntityCondition
        }

        /** pretty print for log messages and even UI stuff */
        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            // TODO: implement the pretty print for log messages and even UI stuff
            return null;
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
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

    public static class ListPriceRangeConstraint extends ProductSearchConstraint {
        public static final String constraintName = "ListPriceRange";
        protected Double lowPrice;
        protected Double highPrice;
        protected String currencyUomId;

        public ListPriceRangeConstraint(Double lowPrice, Double highPrice, String currencyUomId) {
            this.lowPrice = lowPrice;
            this.highPrice = highPrice;
            this.currencyUomId = UtilValidate.isNotEmpty(currencyUomId) ? currencyUomId : "USD";
        }

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
            
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "ProductPriceTypeId", EntityOperator.EQUALS, "LIST_PRICE"));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "ProductPricePurposeId", EntityOperator.EQUALS, "PURCHASE"));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "CurrencyUomId", EntityOperator.EQUALS, currencyUomId));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "ProductStoreGroupId", EntityOperator.EQUALS, "_NA_"));
            productSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, productSearchContext.nowTimestamp));
            if (this.lowPrice != null) {
                productSearchContext.entityConditionList.add(new EntityExpr(prefix + "Price", EntityOperator.GREATER_THAN_EQUAL_TO, this.lowPrice));
            }
            if (this.highPrice != null) {
                productSearchContext.entityConditionList.add(new EntityExpr(prefix + "Price", EntityOperator.LESS_THAN_EQUAL_TO, this.highPrice));
            }

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", 
                    UtilMisc.toMap("constraintName", constraintName, "infoString", "low [" + this.lowPrice + "] high [" + this.highPrice + "] currency [" + this.currencyUomId + "]")));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            if (this.lowPrice == null && this.highPrice == null) {
                // dummy constraint, no values
                return null;
            }
            StringBuffer msgBuf = new StringBuffer();
            msgBuf.append(UtilProperties.getMessage(resource, "ListPriceRange", locale));
            msgBuf.append(": ");
            
            // NOTE: at this point we know that only one or none are null
            if (this.lowPrice == null) {
                msgBuf.append(UtilProperties.getMessage(resource, "CommonLessThan", locale));
                msgBuf.append(" ");
                msgBuf.append(this.highPrice);
            } else if (this.highPrice == null) {
                msgBuf.append(UtilProperties.getMessage(resource, "CommonMoreThan", locale));
                msgBuf.append(" ");
                msgBuf.append(this.lowPrice);
            } else {
                msgBuf.append(this.lowPrice);
                msgBuf.append(" - ");
                msgBuf.append(this.highPrice);
            }
            return msgBuf.toString();
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof ListPriceRangeConstraint) {
                ListPriceRangeConstraint that = (ListPriceRangeConstraint) psc;
                if (this.lowPrice == null) {
                    if (that.lowPrice != null) {
                        return false;
                    }
                } else {
                    if (!this.lowPrice.equals(that.lowPrice)) {
                        return false;
                    }
                }
                if (this.highPrice == null) {
                    if (that.highPrice != null) {
                        return false;
                    }
                } else {
                    if (!this.highPrice.equals(that.highPrice)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static class SupplierConstraint extends ProductSearchConstraint {
        public static final String constraintName = "Supplier";
        protected String supplierPartyId;

        public SupplierConstraint(String supplierPartyId) {
            this.supplierPartyId = supplierPartyId;
        }

        public void addConstraint(ProductSearchContext productSearchContext) {
            // make index based values and increment
            String entityAlias = "SP" + productSearchContext.index;
            String prefix = "sp" + productSearchContext.index;
            productSearchContext.index++;

            productSearchContext.dynamicViewEntity.addMemberEntity(entityAlias, "SupplierProduct");
            productSearchContext.dynamicViewEntity.addAlias(entityAlias, prefix + "SupplierPartyId", "partyId", null, null, null, null);
            productSearchContext.dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
            productSearchContext.entityConditionList.add(new EntityExpr(prefix + "SupplierPartyId", EntityOperator.EQUALS, supplierPartyId));

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", this.supplierPartyId)));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductSupplier", locale)+": " + PartyHelper.getPartyName(delegator, supplierPartyId, false);
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof SupplierConstraint) {
                SupplierConstraint that = (SupplierConstraint) psc;
                if (this.supplierPartyId == null) {
                    if (that.supplierPartyId != null) {
                        return false;
                    }
                } else {
                    if (!this.supplierPartyId.equals(that.supplierPartyId)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static class ExcludeVariantsConstraint extends ProductSearchConstraint {
        public static final String constraintName = "ExcludeVariants";

        public ExcludeVariantsConstraint() {
        }

        public void addConstraint(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodIsVariant", "isVariant", null, null, null, null);
            productSearchContext.entityConditionList.add(new EntityExpr("prodIsVariant", EntityOperator.NOT_EQUAL, "Y"));

            // add in productSearchConstraint, don't worry about the productSearchResultId or constraintSeqId, those will be fill in later
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", "")));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductExcludeVariants", locale);
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof ExcludeVariantsConstraint) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class AvailabilityDateConstraint extends ProductSearchConstraint {
        public static final String constraintName = "AvailabilityDate";

        public AvailabilityDateConstraint() {
        }

        public void addConstraint(ProductSearchContext productSearchContext) {
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodIntroductionDate", "introductionDate", null, null, null, null);
            productSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr("prodIntroductionDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("prodIntroductionDate", EntityOperator.LESS_THAN_EQUAL_TO, productSearchContext.nowTimestamp)));
            productSearchContext.dynamicViewEntity.addAlias("PROD", "prodSalesDiscontinuationDate", "salesDiscontinuationDate", null, null, null, null);
            productSearchContext.entityConditionList.add(new EntityExpr(new EntityExpr("prodSalesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("prodSalesDiscontinuationDate", EntityOperator.GREATER_THAN, productSearchContext.nowTimestamp)));
            productSearchContext.productSearchConstraintList.add(productSearchContext.getDelegator().makeValue("ProductSearchConstraint", UtilMisc.toMap("constraintName", constraintName, "infoString", "")));
        }

        public String prettyPrintConstraint(GenericDelegator delegator, boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductFilterByAvailabilityDates", locale);
        }

        public boolean equals(Object obj) {
            ProductSearchConstraint psc = (ProductSearchConstraint) obj;
            if (psc instanceof AvailabilityDateConstraint) {
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

        public abstract void setSortOrder(ProductSearchContext productSearchContext);
        public abstract String getOrderName();
        public abstract String prettyPrintSortOrder(boolean detailed, Locale locale);
        public abstract boolean isAscending();
    }

    public static class SortKeywordRelevancy extends ResultSortOrder {
        public SortKeywordRelevancy() {
        }

        public void setSortOrder(ProductSearchContext productSearchContext) {
            if (productSearchContext.includedKeywordSearch) {
                // we have to check this in order to be sure that there is a totalRelevancy to sort by...
                productSearchContext.orderByList.add("-totalRelevancy");
                productSearchContext.fieldsToSelect.add("totalRelevancy");
            }
        }

        public String getOrderName() {
            return "KeywordRelevancy";
        }

        public String prettyPrintSortOrder(boolean detailed, Locale locale) {
            return UtilProperties.getMessage(resource, "ProductKeywordRelevancy", locale);
        }

        public boolean isAscending() {
            return false;
        }
    }

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

        public String getOrderName() {
            return "ProductField:" + this.fieldName;
        }

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

        public boolean isAscending() {
            return this.ascending;
        }
    }

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
            
            productSearchContext.entityConditionList.add(new EntityExpr("sortProductPriceTypeId", EntityOperator.EQUALS, this.productPriceTypeId));
            productSearchContext.entityConditionList.add(new EntityExpr("sortCurrencyUomId", EntityOperator.EQUALS, this.currencyUomId));
            productSearchContext.entityConditionList.add(new EntityExpr("sortProductStoreGroupId", EntityOperator.EQUALS, this.productStoreGroupId));
            productSearchContext.entityConditionList.add(new EntityExpr("sortFromDate", EntityOperator.LESS_THAN_EQUAL_TO, productSearchContext.nowTimestamp));
            productSearchContext.entityConditionList.add(new EntityExpr(
                    new EntityExpr("sortThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, 
                    new EntityExpr("sortThruDate", EntityOperator.GREATER_THAN_EQUAL_TO, productSearchContext.nowTimestamp)));

            if (ascending) {
                productSearchContext.orderByList.add("+sortPrice");
            } else {
                productSearchContext.orderByList.add("-sortPrice");
            }
            productSearchContext.fieldsToSelect.add("sortPrice");
        }

        public String getOrderName() {
            return "ProductPrice:" + productPriceTypeId;
        }

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

        public boolean isAscending() {
            return this.ascending;
        }
    }

    /** A rather large and verbose method that doesn't use the cool constraint and sort order objects */
    /*
    public static ArrayList parametricKeywordSearchStandAlone(Set featureIdSet, String keywordsString, GenericDelegator delegator, String productCategoryId, boolean includeSubCategories, String visitId, boolean anyPrefix, boolean anySuffix, boolean isAnd) {
        // TODO: implement this for the new features
        boolean removeStems = UtilProperties.propertyValueEquals("prodsearch", "remove.stems", "true");

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // make view-entity & EntityCondition
        int index = 1;
        List entityConditionList = new LinkedList();
        List orderByList = new LinkedList();
        List fieldsToSelect = UtilMisc.toList("productId");
        DynamicViewEntity dynamicViewEntity = new DynamicViewEntity();
        dynamicViewEntity.addMemberEntity("PROD", "Product");
        dynamicViewEntity.addAlias("PROD", "productName");
        boolean productIdGroupBy = false;

        // Category
        if (productCategoryId != null && productCategoryId.length() > 0) {
            List productCategoryIdList = null;
            if (includeSubCategories) {
                // find all sub-categories recursively, make a Set of productCategoryId
                Set productCategoryIdSet = new HashSet();
                getAllSubCategoryIds(productCategoryId, productCategoryIdSet, delegator, nowTimestamp);
                productCategoryIdList = new ArrayList(productCategoryIdSet);
            } else {
                productCategoryIdList = UtilMisc.toList(productCategoryId);
            }

            // make index based values and increment
            String entityAlias = "PCM" + index;
            String prefix = "pcm" + index;
            index++;

            dynamicViewEntity.addMemberEntity(entityAlias, "ProductCategoryMember");
            dynamicViewEntity.addAlias(entityAlias, prefix + "ProductCategoryId", "productCategoryId", null, null, null, null);
            dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
            dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
            dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
            entityConditionList.add(new EntityExpr(prefix + "ProductCategoryId", EntityOperator.IN, productCategoryIdList));
            entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
            entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, nowTimestamp));
        }

        // Keyword
        List keywordFirstPass = KeywordSearch.makeKeywordList(keywordsString);
        List keywordList = KeywordSearch.fixKeywords(keywordFirstPass, anyPrefix, anySuffix, removeStems, isAnd);

        if (keywordList.size() > 0) {
            if (isAnd) {
                // add up the relevancyWeight fields from all keyword member entities for a total to sort by
                ComplexAlias complexAlias = new ComplexAlias("+");

                Iterator keywordIter = keywordList.iterator();
                while (keywordIter.hasNext()) {
                    String keyword = (String) keywordIter.next();

                    // make index based values and increment
                    String entityAlias = "PK" + index;
                    String prefix = "pk" + index;
                    index++;

                    dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                    dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                    dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                    entityConditionList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));

                    //don't add an alias for this, will be part of a complex alias: dynamicViewEntity.addAlias(entityAlias, prefix + "RelevancyWeight", "relevancyWeight", null, null, null, null);
                    complexAlias.addComplexAliasMember(new ComplexAliasField(entityAlias, "relevancyWeight"));
                }
                dynamicViewEntity.addAlias(null, "totalRelevancy", null, null, null, null, null, complexAlias);
                orderByList.add("-totalRelevancy");
                fieldsToSelect.add("totalRelevancy");
            } else {
                // make index based values and increment
                String entityAlias = "PK" + index;
                String prefix = "pk" + index;
                index++;

                dynamicViewEntity.addMemberEntity(entityAlias, "ProductKeyword");
                dynamicViewEntity.addAlias(entityAlias, "totalRelevancy", "relevancyWeight", null, null, null, "sum");
                dynamicViewEntity.addAlias(entityAlias, prefix + "Keyword", "keyword", null, null, null, null);
                dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                orderByList.add("-totalRelevancy");
                fieldsToSelect.add("totalRelevancy");
                List keywordOrList = new LinkedList();
                Iterator keywordIter = keywordList.iterator();
                while (keywordIter.hasNext()) {
                    String keyword = (String) keywordIter.next();
                    keywordOrList.add(new EntityExpr(prefix + "Keyword", EntityOperator.LIKE, keyword));
                }
                entityConditionList.add(new EntityConditionList(keywordOrList, EntityOperator.OR));

                productIdGroupBy = true;
            }
        }

        // Features
        if (featureIdSet != null && featureIdSet.size() > 0) {
            Iterator featureIdIter = featureIdSet.iterator();
            while (featureIdIter.hasNext()) {
                String productFeatureId = (String) featureIdIter.next();

                // make index based values and increment
                String entityAlias = "PFA" + index;
                String prefix = "pfa" + index;
                index++;

                dynamicViewEntity.addMemberEntity(entityAlias, "ProductFeatureAppl");
                dynamicViewEntity.addAlias(entityAlias, prefix + "ProductFeatureId", "productFeatureId", null, null, null, null);
                dynamicViewEntity.addAlias(entityAlias, prefix + "FromDate", "fromDate", null, null, null, null);
                dynamicViewEntity.addAlias(entityAlias, prefix + "ThruDate", "thruDate", null, null, null, null);
                dynamicViewEntity.addViewLink("PROD", entityAlias, Boolean.FALSE, ModelKeyMap.makeKeyMapList("productId"));
                entityConditionList.add(new EntityExpr(prefix + "ProductFeatureId", EntityOperator.EQUALS, productFeatureId));
                entityConditionList.add(new EntityExpr(new EntityExpr(prefix + "ThruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr(prefix + "ThruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
                entityConditionList.add(new EntityExpr(prefix + "FromDate", EntityOperator.LESS_THAN, nowTimestamp));
            }
        }

        dynamicViewEntity.addAlias("PROD", "productId", null, null, null, new Boolean(productIdGroupBy), null);
        EntityCondition whereCondition = new EntityConditionList(entityConditionList, EntityOperator.AND);
        EntityFindOptions efo = new EntityFindOptions();
        efo.setDistinct(true);

        EntityListIterator eli = null;
        try {
            eli = delegator.findListIteratorByCondition(dynamicViewEntity, whereCondition, null, fieldsToSelect, orderByList, efo);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error in product search", module);
            return null;
        }

        ArrayList productIds = new ArrayList(100);
        Set productIdSet = new HashSet();
        GenericValue searchResult = null;
        while ((searchResult = (GenericValue) eli.next()) != null) {
            String productId = searchResult.getString("productId");
            if (!productIdSet.contains(productId)) {
                productIds.add(productId);
                productIdSet.add(productId);
            }
        }

        return productIds;
    }
     */
}
