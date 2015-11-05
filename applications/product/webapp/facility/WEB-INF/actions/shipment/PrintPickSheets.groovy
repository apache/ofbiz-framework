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

import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilHttp;

toPrintOrders = [];
maxNumberOfOrders = parameters.maxNumberOfOrdersToPrint;
int maxNumberOfOrders = maxNumberOfOrders.toInteger();
int printListCounter = 0;
printGroupName = parameters.printGroupName;
if (printGroupName != null) {
    pickMoveInfoList.each { pickMoveInfo ->
        groupName = pickMoveInfo.groupName ;
        if (groupName == printGroupName) {
            toPrintOrders.add(pickMoveInfo.orderReadyToPickInfoList);
        }
    }
}
else {
    pickMoveInfoList.each { pickMoveInfo ->
        toPrintOrders.add(pickMoveInfo.orderReadyToPickInfoList);
    }
}
if (toPrintOrders) {
    orderList = [];
    orderInfoList = [];
    itemInfoList = [];
    orderHeaderList = [];
    orderChargeList =[];
    toPrintOrders.each { toPrintOrder ->
        if(toPrintOrder) {
            orderHeaders = toPrintOrder.orderHeader;
            orderItemShipGroups = toPrintOrder.orderItemShipGroup;
            orderItemShipGrpInvResList = toPrintOrder.orderItemShipGrpInvResList;
            orderItemShipGrpInvResInfoList = toPrintOrder.orderItemShipGrpInvResInfoList;
            orderHeaders.each { orderHeader ->
                printListCounter = ++printListCounter;
                if (printListCounter <= maxNumberOfOrders) {
                    orderMap = [:];
                    orderId = orderHeader.orderId;
                    orderMap.orderId = orderId;
                    orderMap.orderDate = orderHeader.orderDate;
                    billingOrderContactMechs = [];
                    billingOrderContactMechs = from("OrderContactMech").where("orderId", orderId, "contactMechPurposeTypeId", "BILLING_LOCATION").queryList();
                    if (billingOrderContactMechs.size() > 0) {
                        billingContactMechId = EntityUtil.getFirst(billingOrderContactMechs).contactMechId;
                        billingAddress = from("PostalAddress").where("contactMechId", billingContactMechId).queryOne();
                    }
                    shippingContactMechId = from("OrderContactMech").where("orderId", orderId, "contactMechPurposeTypeId", "SHIPPING_LOCATION").queryFirst().contactMechId;
                    shippingAddress = from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
                    orderItemShipGroups.each { orderItemShipGroup ->
                        if (orderItemShipGroup.orderId == orderId) {
                            orderMap.shipmentMethodType = EntityUtil.getFirst(orderItemShipGroup.getRelated("ShipmentMethodType", null, null, false)).description;
                            orderMap.carrierPartyId = orderItemShipGroup.carrierPartyId;
                            orderMap.shipGroupSeqId = orderItemShipGroup.shipGroupSeqId;
                            orderMap.carrierPartyId = orderItemShipGroup.carrierPartyId;
                            orderMap.isGift = orderItemShipGroup.isGift;
                            orderMap.giftMessage = orderItemShipGroup.giftMessage;
                        }
                        orderMap.shippingAddress = shippingAddress;
                        if (billingOrderContactMechs.size() > 0) {
                            orderMap.billingAddress = billingAddress;
                        }
                        orderInfoMap = [:];
                        orderInfoMap.(orderHeader.orderId) = orderMap;
                    }
                    addInMap = "true";
                    orderItemMap = [:];
                    orderItemShipGrpInvResInfoList.each { orderItemShipGrpInvResInfos ->
                        orderItemShipGrpInvResInfos.each { orderItemShipGrpInvResInfo ->
                            if (orderItemShipGrpInvResInfo.orderItemShipGrpInvRes.orderId == orderId && addInMap == "true") {
                                orderItemMap.(orderHeader.orderId) = orderItemShipGrpInvResInfos;
                                addInMap = "false";
                            }
                        }
                    }
                    orderChargeMap = [:];
                    orderReadHelper = new OrderReadHelper(orderHeader);
                    orderItems = orderReadHelper.getOrderItems();
                    orderAdjustments = orderReadHelper.getAdjustments();
                    orderHeaderAdjustments = orderReadHelper.getOrderHeaderAdjustments();
                    context.orderHeaderAdjustments = orderHeaderAdjustments;
                    orderSubTotal = orderReadHelper.getOrderItemsSubTotal();
                    context.orderSubTotal = orderSubTotal;
                    otherAdjAmount = orderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, true, false, false);
                    shippingAmount = orderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true);
                    shippingAmount = shippingAmount.add(orderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true));
                    taxAmount = orderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, true, false);
                    taxAmount = taxAmount.add(orderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, true, false));
                    grandTotal = orderReadHelper.getOrderGrandTotal(orderItems, orderAdjustments);
                    orderChargeMap.orderSubTotal = orderSubTotal;
                    orderChargeMap.taxAmount = taxAmount;
                    orderChargeMap.shippingAmount = shippingAmount;
                    orderChargeMap.otherAdjAmount = otherAdjAmount;
                    orderChargeMap.grandTotal = grandTotal;
                    orderChargeMap.totalItem = orderItems.size();
                    orderCharges = [:];
                    orderCharges.(orderHeader.orderId) = orderChargeMap;
                    orderChargeList.add(orderCharges);
                    itemInfoList.add(orderItemMap);
                    orderInfoList.add(orderInfoMap);
                    orderList.add(orderHeader);
                    context.orderHeaderList = orderList;
                    context.orderInfoList = orderInfoList;
                    context.itemInfoList = itemInfoList;
                    context.orderChargeList = orderChargeList;
                }
            }
        }
    }
    UtilHttp.setContentDisposition(response, "orderPickSheet.pdf");
}
