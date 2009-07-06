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

import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.order.OrderReadHelper;

productId = request.getParameter("productId") ?: "";
supplier = null;
supplierPartyId = null;

orderId = request.getParameter("orderId");
if (orderId) {
    orderItemShipGroup = EntityUtil.getFirst(delegator.findList("OrderItemShipGroup", null, null, ["orderId" , "orderId"], null, false));
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    EntityCondition cond = EntityCondition.makeCondition([EntityCondition.makeCondition("orderId", orderId),
            EntityCondition.makeCondition("roleTypeId", "BILL_FROM_VENDOR")], EntityOperator.AND);
    supplier = EntityUtil.getFirst(delegator.findList("OrderHeaderAndRoles", cond, null, null, null, false));
    context.shipGroupSeqId =  orderItemShipGroup.shipGroupSeqId ;
    context.orderHeader = orderHeader;
}

ShoppingCart shoppingCart = ShoppingCartEvents.getCartObject(request);

conditionList = [];

// make sure the look up is case insensitive
conditionList.add(EntityCondition.makeCondition(EntityFunction.UPPER(EntityFieldValue.makeFieldValue("productId")),
                                 EntityOperator.LIKE, productId.toUpperCase() + "%"));
if (!supplier) {
    supplierPartyId = shoppingCart.getOrderPartyId();
} else {
    supplierPartyId = supplier.getString("partyId");
}
conditionList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId));

conditionList.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, shoppingCart.getCurrency()));
conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

selectedFields = ["productId", "supplierProductId", "supplierProductName", "lastPrice", "minimumOrderQuantity"] as Set;
selectedFields.add("availableFromDate");
selectedFields.add("availableThruDate");
productList = delegator.findList("SupplierProduct", conditions, selectedFields, ["productId"], null, false);

productList = EntityUtil.filterByDate(productList, nowTimestamp, "availableFromDate", "availableThruDate", true);
newProductList = [];

productList.each { supplierProduct ->
    quantityOnOrder = 0.0;
    productId = supplierProduct.productId;
    condition = [];  // find approved purchase orders
    condition = EntityCondition.makeCondition(EntityCondition.makeCondition("orderTypeId", "PURCHASE_ORDER"), EntityOperator.AND,
            EntityCondition.makeCondition("statusId", "ORDER_APPROVED"));

    orderHeaders = delegator.findList("OrderHeader", condition, null, ["orderId DESC"], null, false);
    orderHeaders.each { orderHeader ->
        orderReadHelper = new OrderReadHelper(orderHeader);
        orderItems = orderReadHelper.getOrderItems();
        orderItems.each { orderItem ->
            if (productId.equals(orderItem.productId) && "ITEM_APPROVED".equals(orderItem.statusId)) {
                if (!orderItem.cancelQuantity) {
                    cancelQuantity = 0.0;
                }
                shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem);
                quantityOnOrder += orderItem.quantity - cancelQuantity - shippedQuantity;
            }
        }
    }
    String facilityId = request.getParameter("facilityId");
    if (facilityId) {
        productFacilityList = delegator.findByAnd("ProductFacility", ["productId": productId, "facilityId" : facilityId]);
    } else {
        productFacilityList = delegator.findByAnd("ProductFacility", ["productId": productId]);
    }
    productFacilityList.each { productFacility ->
        result = dispatcher.runSync("getInventoryAvailableByFacility", ["productId" : productId, "facilityId" : productFacility.facilityId]);
        qohAtp = result.quantityOnHandTotal.toString() + "/" + result.availableToPromiseTotal.toString();
        productInfoMap = [:];
        
        product = delegator.findOne("Product", ["productId" : productId], false);
        productInfoMap.internalName = product.internalName;

        productInfoMap.productId = productId;
        productInfoMap.qohAtp = qohAtp;
        productInfoMap.quantityOnOrder = quantityOnOrder;

        productInfoMap.supplierProductId = supplierProduct.supplierProductId;
        productInfoMap.lastPrice = supplierProduct.lastPrice;
        productInfoMap.orderQtyIncrements = supplierProduct.orderQtyIncrements;

        productInfoMap.minimumStock = productFacility.minimumStock;

        newProductList.add(productInfoMap);
    }
}
context.productList = newProductList;