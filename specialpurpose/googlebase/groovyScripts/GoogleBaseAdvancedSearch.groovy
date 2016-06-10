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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.store.ProductStoreWorker;

categoryIds = [];
prodCatalogList = [];
categoryList = [];

if (parameters.productStoreId) {
    productStoreId = parameters.productStoreId;
} else {
    productStoreId = ProductStoreWorker.getProductStoreId(request);
}
googleBaseConfigList = from("GoogleBaseConfig").queryList();
if (!productStoreId) {
    googleBaseProductStore = EntityUtil.getFirst(googleBaseConfigList);
    productStoreId = googleBaseProductStore.productStoreId;
}
if (productStoreId) {
    productStoreCatalogs = CatalogWorker.getStoreCatalogs(delegator, productStoreId);
    if (productStoreCatalogs) {
        productStoreCatalogs.each { productStoreCatalog ->
            prodCatalog = from("ProdCatalog").where("prodCatalogId", productStoreCatalog.prodCatalogId).cache(true).queryOne();
            prodCatalogList.add(prodCatalog);
        }
    }
}
currentCatalogId = null;
if (parameters.SEARCH_CATALOG_ID) {
    currentCatalogId = parameters.SEARCH_CATALOG_ID;
} else if (prodCatalogList) {
    catalog = EntityUtil.getFirst(prodCatalogList);
    currentCatalogId = catalog.prodCatalogId;
}
topCategory = CatalogWorker.getCatalogTopCategoryId(request, currentCatalogId);
if (topCategory) {
    CategoryWorker.getRelatedCategories(request, "topLevelList", topCategory, true);
    if (request.getAttribute("topLevelList")) {
        categoryList = request.getAttribute("topLevelList");
    } else {
        categoryIds.add(topCategory);
    }
}
if (categoryList) {
    categoryIds = EntityUtil.getFieldListFromEntityList(categoryList, "productCategoryId", true);
}

context.googleBaseConfigList = googleBaseConfigList;
context.categoryIds = categoryIds;
context.productStoreId = productStoreId;
context.prodCatalogList = prodCatalogList;
context.searchCatalogId = currentCatalogId;
