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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.product.catalog.*
import org.apache.ofbiz.product.category.CategoryWorker
import org.apache.ofbiz.product.category.CategoryContentWrapper
import org.apache.ofbiz.product.store.ProductStoreWorker

detailScreen = "categorydetail"
catalogName = CatalogWorker.getCatalogName(request)

productCategoryId = request.getAttribute("productCategoryId") ?: parameters.category_id
context.productCategoryId = productCategoryId

context.productStore = ProductStoreWorker.getProductStore(request)

pageTitle = null
metaDescription = null
metaKeywords = null

category = from("ProductCategory").where("productCategoryId", productCategoryId).cache(true).queryOne()
if (category) {
    if (category.detailScreen) {
        detailScreen = category.detailScreen
    }
    categoryPageTitle = from("ProductCategoryContentAndInfo").where("productCategoryId", productCategoryId, "prodCatContentTypeId", "PAGE_TITLE").cache(true).queryList()
    if (categoryPageTitle) {
        pageTitle = from("ElectronicText").where("dataResourceId", categoryPageTitle.get(0).dataResourceId).cache(true).queryOne()
    }
    categoryMetaDescription = from("ProductCategoryContentAndInfo").where("productCategoryId", productCategoryId, "prodCatContentTypeId", "META_DESCRIPTION").cache(true).queryList()
    if (categoryMetaDescription) {
        metaDescription = from("ElectronicText").where("dataResourceId", categoryMetaDescription.get(0).dataResourceId).cache(true).queryOne()
    }
    categoryMetaKeywords = from("ProductCategoryContentAndInfo").where("productCategoryId", productCategoryId, "prodCatContentTypeId", "META_KEYWORD").cache(true).queryList()
    if (categoryMetaKeywords) {
        metaKeywords = from("ElectronicText").where("dataResourceId", categoryMetaKeywords.get(0).dataResourceId).cache(true).queryOne()
    }
    categoryContentWrapper = new CategoryContentWrapper(category, request)
    
    categoryDescription = categoryContentWrapper.get("DESCRIPTION", "html")

    if (pageTitle) {
        context.title = pageTitle.textData
    } else {
        context.title = categoryContentWrapper.get("CATEGORY_NAME", "html")
    }

    if (metaDescription) {
        context.metaDescription = metaDescription.textData
    } else {
        if (categoryDescription) {
            context.metaDescription = categoryDescription
        }
    }

    if (metaKeywords) {
        context.metaKeywords = metaKeywords.textData
    } else {
        if (categoryDescription) {
            context.metaKeywords = categoryDescription + ", " + catalogName
        } else {
            context.metaKeywords = catalogName
        }
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
