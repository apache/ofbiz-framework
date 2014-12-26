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

import org.ofbiz.base.util.*
import org.ofbiz.entity.*
import org.ofbiz.product.category.*
import org.ofbiz.entity.util.EntityUtilProperties;

state = request.getParameter("BrowseCategoriesState");
isOpen = true;
if (state) {
    session.setAttribute("BrowseCategoriesState", state);
    isOpen = "open".equals(state);
} else {
    state = (String) session.getAttribute("BrowseCategoriesState");
    if (state) {
        isOpen = "open".equals(state);
    }
}
context.isOpen = isOpen;

requestParameters = UtilHttp.getParameterMap(request);
defaultTopCategoryId = requestParameters.TOP_CATEGORY ? requestParameters.TOP_CATEGORY : EntityUtilProperties.getPropertyValue("catalog", "top.category.default", delegator);
currentTopCategoryId = CategoryWorker.getCatalogTopCategory(request, defaultTopCategoryId);
currentTopCategory = null;
if (isOpen) {
    CategoryWorker.getRelatedCategories(request, "topLevelList", currentTopCategoryId, false);
    currentTopCategory = from("ProductCategory").where("productCategoryId", currentTopCategoryId).cache(true).queryOne();
    context.topLevelList = request.getAttribute("topLevelList");
}
curCategoryId = UtilFormatOut.checkNull(requestParameters.productCategoryId);
CategoryWorker.setTrail(request, curCategoryId);

context.curCategoryId = curCategoryId;
context.currentTopCategory = currentTopCategory;

categoryList = request.getAttribute("topLevelList");
if (categoryList) {
    catContentWrappers = [:];
    CategoryWorker.getCategoryContentWrappers(catContentWrappers, categoryList, request);
    context.catContentWrappers = catContentWrappers;
}
