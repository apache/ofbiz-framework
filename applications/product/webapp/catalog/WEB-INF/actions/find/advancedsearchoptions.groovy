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

import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.*
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.feature.*
import org.ofbiz.product.product.*
import org.ofbiz.product.store.ProductStoreWorker;

categoryIds = [];
prodCatalogList = [];
categoryList = [];

searchCategoryId = parameters.SEARCH_CATEGORY_ID;
if (parameters.productStoreId) {
    productStoreId = parameters.productStoreId;
} else {
    productStoreId = ProductStoreWorker.getProductStoreId(request);
}
if ((!searchCategoryId || searchCategoryId.length() == 0) && !productStoreId) {
    currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
    searchCategoryId = CatalogWorker.getCatalogSearchCategoryId(request, currentCatalogId);
}
searchCategory = delegator.findOne("ProductCategory", [productCategoryId : searchCategoryId], false);

if (searchCategoryId) {
    productFeaturesByTypeMap = ParametricSearch.makeCategoryFeatureLists(searchCategoryId, delegator, 2000);
} else {
    productFeaturesByTypeMap = ParametricSearch.getAllFeaturesByType(delegator, 2000);
}
productFeatureTypeIdsOrdered = new ArrayList(new TreeSet(productFeaturesByTypeMap.keySet()));

searchOperator = parameters.SEARCH_OPERATOR;
if (!"AND".equals(searchOperator) && !"OR".equals(searchOperator)) {
  searchOperator = "OR";
}

searchConstraintStrings = ProductSearchSession.searchGetConstraintStrings(false, session, delegator);
searchSortOrderString = ProductSearchSession.searchGetSortOrderString(false, request);

// get suppliers in system
supplerPartyRoleAndPartyDetails = delegator.findList("PartyRoleAndPartyDetail", EntityCondition.makeCondition([roleTypeId : 'SUPPLIER']), null, ['groupName', 'firstName'], null, false);

// get the GoodIdentification types
goodIdentificationTypes = delegator.findList("GoodIdentificationType", null, null, ['description'], null, false);

//get all productStoreIds used in EbayConfig
ebayConfigList = delegator.findList("EbayConfig", null, null, null, null, false);

//get all productStoreIds used in GoogleBaseConfig
googleBaseConfigList = delegator.findList("GoogleBaseConfig", null, null, null, null, false);

if (productStoreId) {
    productStoreCatalogs = CatalogWorker.getStoreCatalogs(delegator, productStoreId);
    if (productStoreCatalogs) {
        productStoreCatalogs.each { productStoreCatalog ->
            prodCatalog = delegator.findOne("ProdCatalog", [prodCatalogId : productStoreCatalog.prodCatalogId], true);
            prodCatalogList.add(prodCatalog);
        }
    }
}
if (parameters.SEARCH_CATALOG_ID) {
    CategoryWorker.getRelatedCategories(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, parameters.SEARCH_CATALOG_ID), true);
    if (request.getAttribute("topLevelList")) {
       categoryList = request.getAttribute("topLevelList");
    }
    context.searchCatalogId = parameters.SEARCH_CATALOG_ID;
} else if (prodCatalogList) {
    catalog = EntityUtil.getFirst(prodCatalogList);
    context.searchCatalogId = catalog.prodCatalogId;
    CategoryWorker.getRelatedCategories(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, catalog.prodCatalogId), true);
    if (request.getAttribute("topLevelList")) {
       categoryList = request.getAttribute("topLevelList");
    }
}
if (categoryList) {
    categoryIds = EntityUtil.getFieldListFromEntityList(categoryList, "productCategoryId", true);
}

context.searchCategoryId = searchCategoryId;
context.searchCategory = searchCategory;
context.productFeaturesByTypeMap = productFeaturesByTypeMap;
context.productFeatureTypeIdsOrdered = productFeatureTypeIdsOrdered;
context.searchOperator = searchOperator;
context.searchConstraintStrings = searchConstraintStrings;
context.searchSortOrderString = searchSortOrderString;
context.supplerPartyRoleAndPartyDetails = supplerPartyRoleAndPartyDetails;
context.goodIdentificationTypes = goodIdentificationTypes;
context.ebayConfigList = ebayConfigList;
context.googleBaseConfigList = googleBaseConfigList;
context.categoryIds = categoryIds;
context.productStoreId = productStoreId;
context.prodCatalogList = prodCatalogList;