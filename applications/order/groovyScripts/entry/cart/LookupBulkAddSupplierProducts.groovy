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

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityFunction
import org.apache.ofbiz.entity.condition.EntityFieldValue
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.order.order.OrderReadHelper

// This script can take quite a while to run with a decent amount of data
// so we'll take a best effort approach to limit the size of the results
maxRows = null
// TODO: Find a way to get the pagination parameters for a given form
if (!parameters.containsKey("VIEW_INDEX_2")) {
    // There's only one set of pagination parameters so it must be for us
    if (parameters.VIEW_SIZE_1) {
        if (parameters.VIEW_INDEX_1) {
            viewSize = Integer.valueOf(parameters.VIEW_SIZE_1)
            viewIndex = Integer.valueOf(parameters.VIEW_INDEX_1)
            maxRows = viewSize * (viewIndex + 1)
        }
    }
}
if (!maxRows) {
    maxRows = 50
}

productId = parameters.productId
supplier = null
supplierPartyId = null

orderId = parameters.orderId
if (orderId) {
    orderItemShipGroup = from("OrderItemShipGroup").orderBy("orderId").queryFirst()
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
    supplier = from("OrderHeaderAndRoles").where("orderId", orderId, "roleTypeId", "BILL_FROM_VENDOR").queryFirst()
    context.shipGroupSeqId =  orderItemShipGroup.shipGroupSeqId 
    context.orderHeader = orderHeader
}

ShoppingCart shoppingCart = ShoppingCartEvents.getCartObject(request)

conditionList = []

if (productId) {
    // make sure the look up is case insensitive
    conditionList.add(EntityCondition.makeCondition(EntityFunction.upper(EntityFieldValue.makeFieldValue("productId")),
                                     EntityOperator.LIKE, productId.toUpperCase() + "%"))
}
if (!supplier) {
    supplierPartyId = shoppingCart.getOrderPartyId()
} else {
    supplierPartyId = supplier.getString("partyId")
}
conditionList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId))

conditionList.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, shoppingCart.getCurrency()))
conditionList.add(EntityCondition.makeConditionDate("availableFromDate", "availableThruDate"))

supplierProducts = select("productId", "supplierProductId", "supplierProductName", "lastPrice", "minimumOrderQuantity", "orderQtyIncrements").from("SupplierProduct")
                    .where(conditionList)
                    .orderBy("productId")
                    .queryList()

newProductList = []
for (supplierProduct in supplierProducts) {
    productId = supplierProduct.productId

    String facilityId = parameters.facilityId
    if (facilityId) {
        productFacilityList = from("ProductFacility").where("productId", productId, "facilityId", facilityId).cache(true).queryList()
    } else {
        productFacilityList = from("ProductFacility").where("productId", productId).cache(true).queryList()
    }
    if (newProductList.size() >= maxRows) {
        // We've got enough results to display, keep going to get the result size but skip the heavy stuff
        newProductList.add(null)
    } else {
        quantityOnOrder = 0.0
        // find approved purchase orders
        orderHeaders = from("OrderHeader").where("orderTypeId", "PURCHASE_ORDER", "statusId", "ORDER_APPROVED").orderBy("orderId DESC").queryList()
        orderHeaders.each { orderHeader ->
            orderReadHelper = new OrderReadHelper(orderHeader)
            orderItems = orderReadHelper.getOrderItems()
            orderItems.each { orderItem ->
                if (productId.equals(orderItem.productId) && "ITEM_APPROVED".equals(orderItem.statusId)) {
                    if (!orderItem.cancelQuantity) {
                        cancelQuantity = 0.0
                    }
                    shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)
                    quantityOnOrder += orderItem.quantity - cancelQuantity - shippedQuantity
                }
            }
        }
        product = from("Product").where("productId", productId).cache(true).queryOne()
        productFacilityList.each { productFacility ->
            result = runService('getInventoryAvailableByFacility', ["productId" : productId, "facilityId" : productFacility.facilityId])
            qohAtp = result.quantityOnHandTotal.toPlainString() + "/" + result.availableToPromiseTotal.toPlainString()
            productInfoMap = [:]
            
            productInfoMap.internalName = product.internalName
    
            productInfoMap.productId = productId
            productInfoMap.qohAtp = qohAtp
            productInfoMap.quantityOnOrder = quantityOnOrder
    
            productInfoMap.supplierProductId = supplierProduct.supplierProductId
            productInfoMap.lastPrice = supplierProduct.lastPrice
            productInfoMap.orderQtyIncrements = supplierProduct.orderQtyIncrements
            productInfoMap.minimumOrderQuantity = supplierProduct.minimumOrderQuantity

            productInfoMap.minimumStock = productFacility.minimumStock
    
            newProductList.add(productInfoMap)
        }
    }
}
context.productListSize = newProductList.size()
context.productList = newProductList
