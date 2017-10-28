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

import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.product.catalog.CatalogWorker

productId = parameters.productId
if (productId) {

    quantityOnHandTotal = parameters.quantityOnHandTotal
    if (!quantityOnHandTotal) {
        quantityOnHandTotal = 0
    }
    context.quantityOnHandTotal = quantityOnHandTotal

    availableToPromiseTotal = parameters.availableToPromiseTotal
    if (!availableToPromiseTotal) {
        availableToPromiseTotal = 0
    }
    context.availableToPromiseTotal = availableToPromiseTotal
}
context.productId = productId

// Just in case we are here from the choosecatalog form, the
// following call will save in the session the new catalogId
CatalogWorker.getCurrentCatalogId(request)

// Get the Cart and Prepare Size
shoppingCart = ShoppingCartEvents.getCartObject(request)
context.shoppingCartSize = shoppingCart.size()
context.shoppingCart = shoppingCart
context.currencyUomId = shoppingCart.getCurrency()
context.orderType = shoppingCart.getOrderType()

orderItems = shoppingCart.makeOrderItems(dispatcher)
orderAdjustments = shoppingCart.makeAllAdjustments()
orderItemShipGroupInfo = shoppingCart.makeAllShipGroupInfos()
if (orderItemShipGroupInfo) {
    orderItemShipGroupInfo.each { osiInfo ->
        if ("OrderAdjustment".equals(osiInfo.getEntityName())) {
            // shipping / tax adjustment(s)
            orderAdjustments.add(osiInfo)
        }
    }
}
orderHeaderAdjustments = OrderReadHelper.getOrderHeaderAdjustments(orderAdjustments, null)
orderSubTotal = OrderReadHelper.getOrderItemsSubTotal(orderItems, orderAdjustments)
shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true)
shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true))
context.orderShippingTotal = shippingAmount
taxAmount = OrderReadHelper.getOrderTaxByTaxAuthGeoAndParty(orderAdjustments).taxGrandTotal
context.orderTaxTotal = taxAmount

// get all the possible gift wrap options
allgiftWraps = from("ProductFeature").where("productFeatureTypeId", "GIFT_WRAP").orderBy("defaultSequenceNum").queryList()
context.allgiftWraps = allgiftWraps

context.contentPathPrefix = CatalogWorker.getContentPathPrefix(request)

// retrieve the product store id from the cart
productStoreId = shoppingCart.getProductStoreId()
context.productStoreId = productStoreId

partyId = shoppingCart.getPartyId()
if ("_NA_".equals(partyId)) partyId = null
context.partyId = partyId

defaultDesiredDeliveryDate = shoppingCart.getDefaultItemDeliveryDate()
if (!defaultDesiredDeliveryDate) {
    defaultDesiredDeliveryDate = (new java.sql.Date(System.currentTimeMillis())).toString()
} else {
    context.useAsDefaultDesiredDeliveryDate =  "true"
}
context.defaultDesiredDeliveryDate = defaultDesiredDeliveryDate

defaultComment = shoppingCart.getDefaultItemComment()
if (defaultComment) {
    context.useAsDefaultComment = "true"
}
context.defaultComment = defaultComment

// get all party shopping lists
if (partyId) {
  shoppingLists = from("ShoppingList").where("partyId", partyId).queryList()
  context.shoppingLists = shoppingLists
}

// get product inventory summary for each shopping cart item
productStore = from("ProductStore").where("productStoreId", productStoreId).cache(true).queryOne()
context.productStore = productStore
productStoreFacilityId = null
if (productStore) {
    productStoreFacilityId = productStore.inventoryFacilityId
}
context.facilityId = productStoreFacilityId
inventorySummary = runService('getProductInventorySummaryForItems', [orderItems : shoppingCart.makeOrderItems(dispatcher), facilityId : productStoreFacilityId])
context.availableToPromiseMap = inventorySummary.availableToPromiseMap
context.quantityOnHandMap = inventorySummary.quantityOnHandMap
context.mktgPkgATPMap = inventorySummary.mktgPkgATPMap
context.mktgPkgQOHMap = inventorySummary.mktgPkgQOHMap

// get purchase order item types
purchaseOrderItemTypeList = from("OrderItemType").where("parentTypeId", "PURCHASE_SPECIFIC").cache(true).queryList()
context.purchaseOrderItemTypeList = purchaseOrderItemTypeList
