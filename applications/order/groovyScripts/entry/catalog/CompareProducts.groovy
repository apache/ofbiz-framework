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

import org.apache.ofbiz.product.product.*
import org.apache.ofbiz.order.shoppingcart.*
import org.apache.ofbiz.product.catalog.*
import org.apache.ofbiz.product.store.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.base.util.*

compareList = ProductEvents.getProductCompareList(request)
context.compareList = compareList
cart = ShoppingCartEvents.getCartObject(request)
catalogId = CatalogWorker.getCurrentCatalogId(request)
productStore = ProductStoreWorker.getProductStore(request)
productStoreId = productStore.productStoreId

productDataMap = [:]
context.productDataMap = productDataMap
productFeatureTypeIds = [] as LinkedHashSet
context.productFeatureTypeIds = productFeatureTypeIds
productFeatureTypeMap = [:]
context.productFeatureTypeMap = productFeatureTypeMap


compareList.each { product ->
    productData = [:]
    productDataMap[product.productId] = productData
    
    productData.productContentWrapper = ProductContentWrapper.makeProductContentWrapper(product, request)

    priceContext = [product : product, currencyUomId : cart.getCurrency(),
        autoUserLogin : autoUserLogin, userLogin : userLogin]
    priceContext.webSiteId = webSiteId
    priceContext.prodCatalogId = catalogId
    priceContext.productStoreId = productStoreId
    priceContext.agreementId = cart.getAgreementId()
    priceContext.partyId = cart.getPartyId() // IMPORTANT: otherwise it'll be calculating prices using the logged in user which could be a CSR instead of the customer
    priceContext.checkIncludeVat = "Y"
    productData.priceMap = runService('calculateProductPrice', priceContext)
    
    condList = [
                EntityCondition.makeCondition("productId", product.productId),
                EntityUtil.getFilterByDateExpr(),
                EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.IN, ["STANDARD_FEATURE", "DISTINGUISHING_FEAT", "SELECTABLE_FEATURE"])
               ]
    productFeatureAppls = from("ProductFeatureAppl").where(condList).orderBy("sequenceNum").cache(true).queryList()
    productFeatureAppls.each { productFeatureAppl ->
        productFeature = productFeatureAppl.getRelatedOne("ProductFeature", true)
        if (!productData[productFeature.productFeatureTypeId]) {
            productData[productFeature.productFeatureTypeId] = [:]
        }
        if (!productData[productFeature.productFeatureTypeId][productFeatureAppl.productFeatureApplTypeId]) {
            productData[productFeature.productFeatureTypeId][productFeatureAppl.productFeatureApplTypeId] = []
        }
        productData[productFeature.productFeatureTypeId][productFeatureAppl.productFeatureApplTypeId] << productFeature
        productFeatureTypeIds << productFeature.productFeatureTypeId
    } 
}
productFeatureTypeIds.each { productFeatureTypeId ->
    productFeatureTypeMap[productFeatureTypeId] = from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeId).cache(true).queryOne()
}
