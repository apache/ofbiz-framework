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
 import org.ofbiz.base.util.UtilValidate;
 import org.ofbiz.entity.util.EntityUtil;
 import org.ofbiz.product.catalog.CatalogWorker;
 
 prodCatalog = null;
 prodCatalogId = parameters.prodCatalogId;
 showScreen = "origin";
 List errMsgList = [];
 
 productStore = EntityUtil.getFirst(delegator.findByAnd("ProductStore", [payToPartyId: partyId], null, false));
 if(productStore){
     context.productStoreId = productStore.productStoreId;
 }
 if(UtilValidate.isEmpty(productStore)){
     errMsgList.add("Product Store not set!");
     showScreen = "message";
 } else {
     facility = delegator.findOne("Facility", [facilityId : productStore.inventoryFacilityId], false);
     webSite = EntityUtil.getFirst(delegator.findByAnd("WebSite", [productStoreId: productStore.productStoreId], null, false));
     
     if(UtilValidate.isEmpty(facility)){
         errMsgList.add("Facility not set!");
         showScreen = "message";
     }
     if(UtilValidate.isEmpty(webSite)){
         errMsgList.add("WebSite not set!");
         showScreen = "message";
     }
 }
 if (errMsgList) {
    request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
    return;
 }
 
 productStoreCatalog = EntityUtil.getFirst(delegator.findByAnd("ProductStoreCatalog", [productStoreId: productStore.productStoreId], null, false));
 if(productStoreCatalog){
     prodCatalog = productStoreCatalog.getRelatedOne("ProdCatalog", false);
     prodCatalogId = prodCatalog.prodCatalogId;
 }
 context.prodCatalog = prodCatalog;
 context.prodCatalogId = prodCatalogId
 context.showScreen = showScreen;

 if(("productcategory".equals(tabButtonItem)) || ("product".equals(tabButtonItem))){
     productCategory = null;
     productCategoryId = parameters.productCategoryId;
     showErrorMsg = "N";
     
     if(UtilValidate.isEmpty(prodCatalogId)){
         errMsgList.add("Product Catalog not set!");
         showErrorMsg = "Y";
     }
     
     prodCatalogCategory  = EntityUtil.getFirst(delegator.findByAnd("ProdCatalogCategory", [prodCatalogId: prodCatalogId, sequenceNum: new Long(1)], null, false));
     if(prodCatalogCategory){
         productCategory = EntityUtil.getFirst(delegator.findByAnd("ProductCategory", [primaryParentCategoryId : prodCatalogCategory.productCategoryId], null, false));
         if(productCategory){
             productCategoryId = productCategory.productCategoryId;
         }
     }
     context.productCategoryId = productCategoryId;
     context.productCategory = productCategory;
     
     if("product".equals(tabButtonItem)){
         productId = parameters.productId;
         product = null;
         
         if(UtilValidate.isEmpty(productCategoryId)){
             errMsgList.add("Product Category not set!");
             showErrorMsg = "Y";
         }
         /**************** get product from ProductCategory ******************/
         productCategoryMember  = EntityUtil.getFirst(delegator.findByAnd("ProductCategoryMember", [productCategoryId: productCategoryId], null, false));
         if(productCategoryMember){
             product = productCategoryMember.getRelatedOne("Product", false);
             productId = product.productId;
             // Average cost
             averageCostValues = delegator.findByAnd("ProductPrice", [productId : productId, productPricePurposeId : "PURCHASE", productPriceTypeId : "AVERAGE_COST"], null, false);
             if(averageCostValues){
                 averageCostValue = EntityUtil.getFirst(EntityUtil.filterByDate(averageCostValues));
                 if (averageCostValue?.price != null) {
                     context.averageCost = averageCostValue.price;
                 }
             }
             //    Default cost
             defaultPriceValues = delegator.findByAnd("ProductPrice", [productId : productId, productPricePurposeId : "PURCHASE", productPriceTypeId : "DEFAULT_PRICE"], null, false);
             if(defaultPriceValues){
                 defaultPrice = EntityUtil.getFirst(EntityUtil.filterByDate(defaultPriceValues));
                 if (defaultPrice?.price != null) {
                     context.defaultPrice = defaultPrice.price;
                 }
             }
         }
         // get promotion category
         promoCat = CatalogWorker.getCatalogPromotionsCategoryId(request, prodCatalogId);
         context.productId = productId;
         context.product = product;
         context.promoCat = promoCat;
     }
     
     if (errMsgList) {
        request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
        return;
     }
     context.showErrorMsg = showErrorMsg;
 }
