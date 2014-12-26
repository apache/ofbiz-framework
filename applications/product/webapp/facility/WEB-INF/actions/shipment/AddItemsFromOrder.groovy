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

import org.ofbiz.entity.*
import org.ofbiz.entity.util.*
import org.ofbiz.entity.condition.EntityCondition;

shipmentId = request.getParameter("shipmentId");
orderId = request.getParameter("orderId");
shipGroupSeqId = request.getParameter("shipGroupSeqId");
selectFromShipmentPlan = request.getParameter("selectFromShipmentPlan");

shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();

if (shipment) {
    context.originFacility = shipment.getRelatedOne("OriginFacility", false);
    context.destinationFacility = shipment.getRelatedOne("DestinationFacility", false);
}

if (!orderId && shipment && !selectFromShipmentPlan) {
    orderId = shipment.primaryOrderId;
}
if (!shipGroupSeqId && shipment) {
    shipGroupSeqId = shipment.primaryShipGroupSeqId;
}

if (orderId && shipment) {
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
    context.orderHeader = orderHeader;

    if (orderHeader) {
        context.orderHeaderStatus = orderHeader.getRelatedOne("StatusItem", false);
        context.orderType = orderHeader.getRelatedOne("OrderType", false);

        isSalesOrder = "SALES_ORDER".equals(orderHeader.orderTypeId);
        context.isSalesOrder = isSalesOrder;

        orderItemShipGroup = null;
        if (shipGroupSeqId) {
            orderItemShipGroup = from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
            context.orderItemShipGroup = orderItemShipGroup;
        }

        oiasgaLimitMap = null;
        if (orderItemShipGroup) {
            oiasgaLimitMap = [shipGroupSeqId : shipGroupSeqId];
        }
        orderItems = orderHeader.getRelated("OrderItemAndShipGroupAssoc", oiasgaLimitMap, ['shipGroupSeqId', 'orderItemSeqId'], false);
        orderItemDatas = [] as LinkedList;
        orderItems.each { orderItemAndShipGroupAssoc ->
            orderItemData = [:];
            product = orderItemAndShipGroupAssoc.getRelatedOne("Product", false);

            itemIssuances = orderItemAndShipGroupAssoc.getRelated("ItemIssuance", null, null, false);
            totalQuantityIssued = 0;
            itemIssuances.each { itemIssuance ->
                if (itemIssuance.quantity) {
                    totalQuantityIssued += itemIssuance.getDouble("quantity");
                }
                if (itemIssuance.cancelQuantity) {
                    totalQuantityIssued -= itemIssuance.getDouble("cancelQuantity");
                }
            }

            if (isSalesOrder) {
                oisgirLimitMap = null;
                if (orderItemShipGroup) {
                    oisgirLimitMap = [shipGroupSeqId : shipGroupSeqId];
                }
                orderItemShipGrpInvResList = orderItemAndShipGroupAssoc.getRelated("OrderItemShipGrpInvRes", oisgirLimitMap, ['reservedDatetime'], false);
                orderItemShipGrpInvResDatas = [] as LinkedList;
                totalQuantityReserved = 0;
                orderItemShipGrpInvResList.each { orderItemShipGrpInvRes ->
                    inventoryItem = orderItemShipGrpInvRes.getRelatedOne("InventoryItem", false);
                    orderItemShipGrpInvResData = [:];
                    orderItemShipGrpInvResData.orderItemShipGrpInvRes = orderItemShipGrpInvRes;
                    orderItemShipGrpInvResData.inventoryItem = inventoryItem;
                    orderItemShipGrpInvResData.inventoryItemFacility = inventoryItem.getRelatedOne("Facility", false);
                    orderItemShipGrpInvResDatas.add(orderItemShipGrpInvResData);

                    if (orderItemShipGrpInvRes.quantity) {
                        totalQuantityReserved += orderItemShipGrpInvRes.getDouble("quantity");
                    }
                }

                orderItemData.orderItemShipGrpInvResDatas = orderItemShipGrpInvResDatas;
                orderItemData.totalQuantityReserved = totalQuantityReserved;
                orderItemData.totalQuantityIssuedAndReserved = totalQuantityReserved + totalQuantityIssued;
            }

            orderItemData.orderItemAndShipGroupAssoc = orderItemAndShipGroupAssoc;
            orderItemData.product = product;
            orderItemData.itemIssuances = itemIssuances;
            orderItemData.totalQuantityIssued = totalQuantityIssued;
            orderItemDatas.add(orderItemData);
        }
        context.orderItemDatas = orderItemDatas;
    }
}
if (shipment && selectFromShipmentPlan) {
    shipmentPlans = from("OrderShipment").where("shipmentId", shipment.shipmentId).orderBy("orderId", "orderItemSeqId").queryList();
    orderItemDatas = [] as LinkedList;

    context.isSalesOrder = true;
    shipmentPlans.each { shipmentPlan ->
        orderItemData = [:];
        orderItem = shipmentPlan.getRelatedOne("OrderItem", false);

        orderItemShipGroup = null;
        if (shipGroupSeqId) {
            orderItemShipGroup = from("OrderItemShipGroup").where("orderId", orderItem.orderId, "shipGroupSeqId", shipGroupSeqId).queryOne();
            context.orderItemShipGroup = orderItemShipGroup;
        }

        oiasgaLimitMap = null;
        if (orderItemShipGroup) {
            oiasgaLimitMap = [shipGroupSeqId : shipGroupSeqId];
        }

        orderItemShipGroupAssoc = null;
        orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc", oiasgaLimitMap, null, false);
        if (orderItemShipGroupAssocs) {
            orderItemShipGroupAssoc = EntityUtil.getFirst(orderItemShipGroupAssocs);
        }
        plannedQuantity = shipmentPlan.getDouble("quantity");
        totalProposedQuantity = 0.0;

        product = orderItem.getRelatedOne("Product", false);

        itemIssuances = orderItem.getRelated("ItemIssuance", null, null, false);
        totalQuantityIssued = 0;
        totalQuantityIssuedInShipment = 0;
        itemIssuances.each { itemIssuance ->
            if (itemIssuance.quantity) {
                totalQuantityIssued += itemIssuance.getDouble("quantity");
            }
            if (itemIssuance.cancelQuantity) {
                totalQuantityIssued -= itemIssuance.getDouble("cancelQuantity");
            }
            if (itemIssuance.shipmentId && itemIssuance.shipmentId.equals(shipmentId)) {
                totalQuantityIssuedInShipment += itemIssuance.getDouble("quantity");
                if (itemIssuance.cancelQuantity) {
                    totalQuantityIssuedInShipment -= itemIssuance.getDouble("cancelQuantity");
                }
            }
        }

        orderItemShipGrpInvResList = orderItem.getRelated("OrderItemShipGrpInvRes", null, ['reservedDatetime'], false);
        orderItemShipGrpInvResDatas = [] as LinkedList;
        totalQuantityReserved = 0;
        orderItemShipGrpInvResList.each { orderItemShipGrpInvRes ->
            inventoryItem = orderItemShipGrpInvRes.getRelatedOne("InventoryItem", false);
            orderItemShipGrpInvResData = [:];
            orderItemShipGrpInvResData.orderItemShipGrpInvRes = orderItemShipGrpInvRes;
            orderItemShipGrpInvResData.inventoryItem = inventoryItem;
            orderItemShipGrpInvResData.inventoryItemFacility = inventoryItem.getRelatedOne("Facility", false);
            orderItemShipGrpInvResDatas.add(orderItemShipGrpInvResData);

            reservedQuantity = 0.0;
            quantityNotAvailable = 0.0;
            proposedQuantity = 0.0;
            if (orderItemShipGrpInvRes.quantity) {
                reservedQuantity = orderItemShipGrpInvRes.getDouble("quantity");
                totalQuantityReserved += reservedQuantity;
            }
            if (orderItemShipGrpInvRes.quantityNotAvailable) {
                quantityNotAvailable = orderItemShipGrpInvRes.getDouble("quantityNotAvailable");
            }
            proposedQuantity = reservedQuantity - quantityNotAvailable;
            if (plannedQuantity - totalProposedQuantity < proposedQuantity) {
                proposedQuantity = plannedQuantity - totalProposedQuantity;
            }
            if (proposedQuantity < 0) {
                proposedQuantity = 0.0;
            }
            totalProposedQuantity += proposedQuantity;
            orderItemShipGrpInvResData.shipmentPlanQuantity = proposedQuantity;
        }

        orderItemShipGroupAssocMap = new HashMap(orderItemShipGroupAssoc);
        orderItemShipGroupAssocMap.quantity = orderItemShipGroupAssoc.getDouble("quantity");
        orderItemData.orderItemAndShipGroupAssoc = orderItemShipGroupAssocMap;
        orderItemData.orderItemShipGrpInvResDatas = orderItemShipGrpInvResDatas;
        orderItemData.totalQuantityReserved = totalQuantityReserved;
        orderItemData.totalQuantityIssuedAndReserved = totalQuantityReserved + totalQuantityIssued;
        orderItemData.orderItem = orderItem;
        orderItemData.product = product;
        orderItemData.itemIssuances = itemIssuances;
        orderItemData.totalQuantityIssued = totalQuantityIssued;
        orderItemDatas.add(orderItemData);
    }
    context.orderItemDatas = orderItemDatas;
}
context.shipmentId = shipmentId;
context.shipment = shipment;
context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
