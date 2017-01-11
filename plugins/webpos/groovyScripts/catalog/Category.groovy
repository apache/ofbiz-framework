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
import org.apache.ofbiz.product.category.CategoryContentWrapper

detailScreen = "categorydetail"
catalogName = CatalogWorker.getCatalogName(request)

productCategoryId = parameters.productCategoryId
if (!(productCategoryId) && request.getAttribute("topCategoryId")) {
    productCategoryId = request.getAttribute("topCategoryId")
}

category = from("ProductCategory").where("productCategoryId", productCategoryId).cache(true).queryOne()
if (category) {
    if (category.detailScreen) {
        detailScreen = category.detailScreen
    }
    categoryContentWrapper = new CategoryContentWrapper(category, request)
    context.title = categoryContentWrapper.get("CATEGORY_NAME", "html")
    categoryDescription = categoryContentWrapper.get("DESCRIPTION", "html")
    if (categoryDescription) {
        context.metaDescription = categoryDescription
        context.metaKeywords = categoryDescription + ", " + catalogName
    } else {
        context.metaKeywords = catalogName
    }
    context.productCategory = category
}

// check the catalogs template path and update
templatePathPrefix = CatalogWorker.getTemplatePathPrefix(request)
if (templatePathPrefix) {
    detailScreen = templatePathPrefix + detailScreen
}
context.detailScreen = detailScreen

request.setAttribute("productCategoryId", productCategoryId)
request.setAttribute("defaultViewSize", 10)
request.setAttribute("limitView", true)
