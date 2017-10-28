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

/*
 * This script is also referenced by the ecommerce's screens and
 * should not contain order component's specific code.
 */

import org.apache.ofbiz.product.catalog.CatalogWorker
import org.apache.ofbiz.product.feature.ParametricSearch
import org.apache.ofbiz.product.product.ProductSearchSession
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

searchCategoryId = parameters.SEARCH_CATEGORY_ID
if (!searchCategoryId) {
    currentCatalogId = CatalogWorker.getCurrentCatalogId(request)
    searchCategoryId = CatalogWorker.getCatalogSearchCategoryId(request, currentCatalogId)
}
searchCategory = from("ProductCategory").where("productCategoryId", searchCategoryId).queryOne()

productFeaturesByTypeMap = ParametricSearch.makeCategoryFeatureLists(searchCategoryId, delegator)
productFeatureTypeIdsOrdered = new TreeSet(productFeaturesByTypeMap.keySet()) as List
if(productFeatureTypeIdsOrdered) {
    context.productFeatureTypes = from("ProductFeatureType").where(EntityCondition.makeCondition("productFeatureTypeId", EntityOperator.IN, productFeatureTypeIdsOrdered)).queryList()
}

searchOperator = parameters.SEARCH_OPERATOR
if (!"AND".equals(searchOperator) && !"OR".equals(searchOperator)) {
  searchOperator = "OR"
}

searchConstraintStrings = ProductSearchSession.searchGetConstraintStrings(false, session, delegator)
searchSortOrderString = ProductSearchSession.searchGetSortOrderString(false, request)

context.searchCategoryId = searchCategoryId
context.searchCategory = searchCategory
context.productFeaturesByTypeMap = productFeaturesByTypeMap
context.productFeatureTypeIdsOrdered = productFeatureTypeIdsOrdered
context.searchOperator = searchOperator
context.searchConstraintStrings = searchConstraintStrings
context.searchSortOrderString = searchSortOrderString
