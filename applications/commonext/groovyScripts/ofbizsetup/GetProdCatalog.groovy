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
 import org.apache.ofbiz.base.util.UtilValidate
 import org.apache.ofbiz.entity.util.EntityUtil
 import org.apache.ofbiz.product.catalog.CatalogWorker
 
 prodCatalog = null
 prodCatalogId = parameters.prodCatalogId
 showScreen = "origin"
 List errMsgList = []
 
 productStore = from("ProductStore").where("payToPartyId", partyId).queryFirst();
 if(productStore){
     context.productStoreId = productStore.productStoreId
 }
 if(!productStore){
     errMsgList.add("Product Store not set!")
     showScreen = "message"
 } else {
     facility =from("Facility").where("facilityId", productStore.inventoryFacilityId).queryOne();
     webSite = from("WebSite").where("productStoreId", productStore.productStoreId).queryFirst();
     
     if(!facility){
         errMsgList.add("Facility not set!")
         showScreen = "message"
     }
     if(!webSite){
         errMsgList.add("WebSite not set!")
         showScreen = "message"
     }
 }
 if (errMsgList) {
    request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList)
    return
 }
 
 productStoreCatalog = from("ProductStoreCatalog").where("productStoreId", productStore.productStoreId).queryFirst();
 if(productStoreCatalog){
     prodCatalog = productStoreCatalog.getRelatedOne("ProdCatalog", false)
     prodCatalogId = prodCatalog.prodCatalogId
 }
 context.prodCatalog = prodCatalog
 context.prodCatalogId = prodCatalogId
 context.showScreen = showScreen

 if(("productcategory".equals(tabButtonItem)) || ("product".equals(tabButtonItem))){
     productCategory = null
     productCategoryId = parameters.productCategoryId
     showErrorMsg = "N"
     
     if(!prodCatalogId){
         errMsgList.add("Product Catalog not set!")
         showErrorMsg = "Y"
     }
     
     prodCatalogCategory  = from("ProdCatalogCategory").where("prodCatalogId", prodCatalogId, "sequenceNum", new Long(1)).queryFirst();
     if(prodCatalogCategory){
         productCategory = from("ProductCategory").where("primaryParentCategoryId", prodCatalogCategory.productCategoryId).queryFirst();
         if(productCategory){
             productCategoryId = productCategory.productCategoryId
         }
     }
     context.productCategoryId = productCategoryId
     context.productCategory = productCategory
     
     if("product".equals(tabButtonItem)){
         productId = parameters.productId
         product = null
         
         if(!productCategoryId){
             errMsgList.add("Product Category not set!")
             showErrorMsg = "Y"
         }
         /**************** get product from ProductCategory ******************/
         productCategoryMember  = from("ProductCategoryMember").where("productCategoryId", productCategoryId).queryFirst();
         if(productCategoryMember){
             product = productCategoryMember.getRelatedOne("Product", false)
             productId = product.productId
             // Average cost
             averageCostValues = from("ProductPrice").where("productId", productId, "productPricePurposeId", "PURCHASE", "productPriceTypeId", "AVERAGE_COST").queryList()
             if(averageCostValues){
                 averageCostValue = EntityUtil.getFirst(EntityUtil.filterByDate(averageCostValues))
                 if (averageCostValue?.price != null) {
                     context.averageCost = averageCostValue.price
                 }
             }
             //    Default cost
             defaultPriceValues = from("ProductPrice").where("productId", productId, "productPricePurposeId", "PURCHASE", "productPriceTypeId", "DEFAULT_PRICE").queryList();
             if(defaultPriceValues){
                 defaultPrice = EntityUtil.getFirst(EntityUtil.filterByDate(defaultPriceValues))
                 if (defaultPrice?.price != null) {
                     context.defaultPrice = defaultPrice.price
                 }
             }
         }
         // get promotion category
         promoCat = CatalogWorker.getCatalogPromotionsCategoryId(request, prodCatalogId)
         context.productId = productId
         context.product = product
         context.promoCat = promoCat
     }
     
     if (errMsgList) {
        request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList)
        return
     }
     context.showErrorMsg = showErrorMsg
 }
