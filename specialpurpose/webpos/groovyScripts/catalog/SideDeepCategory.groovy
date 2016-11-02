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
import org.apache.ofbiz.product.catalog.CatalogWorker
import org.apache.ofbiz.product.category.CategoryWorker

CategoryWorker.getRelatedCategories(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, CatalogWorker.getCurrentCatalogId(request)), true)
curCategoryId = parameters.category_id ?: parameters.CATEGORY_ID ?: ""
request.setAttribute("curCategoryId", curCategoryId)
CategoryWorker.setTrail(request, curCategoryId)

categoryList = request.getAttribute("topLevelList")
if (categoryList) {
    catContentWrappers = [:]
    CategoryWorker.getCategoryContentWrappers(catContentWrappers, categoryList, request)
    context.catContentWrappers = catContentWrappers
}
if (!curCategoryId && categoryList) {
    curCategoryId = categoryList[0].productCategoryId
    request.setAttribute("topCategoryId", curCategoryId)
}