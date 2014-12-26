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

import java.util.List;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;
import org.ofbiz.product.store.ProductStoreWorker;

categoryIds = [];
prodCatalogList = [];
categoryList = [];
categoryIdsTemp = []

if (parameters.productStoreId) {
    productStoreId = parameters.productStoreId;
}
googleBaseConfigList = from("GoogleBaseConfig").where("productStoreId", productStoreId).queryList();
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
prodCatalogList.each { prodCatalogList -> 
    currentCatalogId = prodCatalogList.prodCatalogId
    prodCatalogCategoryList = from("ProdCatalogCategory").where("prodCatalogId", currentCatalogId, "prodCatalogCategoryTypeId", "PCCT_BROWSE_ROOT").queryList();
    topCategory = prodCatalogCategoryList.productCategoryId[0];
    if (topCategory){
        relatedCategories = runService('getRelatedCategories', [parentProductCategoryId: topCategory]);
        categoryList = relatedCategories.categories
    } else {
        categoryIdsTemp.clear()
    }
    if (categoryList) {
        categoryIdsTemp = EntityUtil.getFieldListFromEntityList(categoryList, "productCategoryId", true);
    }
    categoryIds.add(categoryIdsTemp)
}

context.googleBaseConfigList = googleBaseConfigList;
context.categoryIds = categoryIds;
context.productStoreId = productStoreId;
