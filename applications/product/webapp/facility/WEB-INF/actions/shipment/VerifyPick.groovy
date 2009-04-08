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
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.shipment.verify.VerifyPickSession;

verifyPickSession = session.getAttribute("verifyPickSession");
if (!verifyPickSession) {
    verifyPickSession = new VerifyPickSession(dispatcher, userLogin);
    session.setAttribute("verifyPickSession", verifyPickSession);
}

shipmentId = parameters.shipmentId;
if (!shipmentId) {
    shipmentId = request.getAttribute("shipmentId");
}
context.shipmentId = shipmentId;

if (shipmentId) {
    context.orderId = null;
    shipment = delegator.findOne("Shipment",  [shipmentId : shipmentId], false);
    if (shipment) {
        shipmentItemBillingList = shipment.getRelated("ShipmentItemBilling");
        invoiceIds = EntityUtil.getFieldListFromEntityList(shipmentItemBillingList, "invoiceId", true);
        if (invoiceIds) {
            context.invoiceIds = invoiceIds;
        }
    }
}

facilityId = parameters.facilityId;
if (facilityId) {
    facility = delegator.findOne("Facility", [facilityId : facilityId], false);
    context.facility = facility;
}
orderId = parameters.orderId;
shipGroupSeqId = parameters.shipGroupSeqId;

if (orderId && !shipGroupSeqId && orderId.indexOf("/") > -1) {
    idArray = orderId.split("\\/");
    orderId = idArray[0];
    shipGroupSeqId = idArray[1];
} else if (orderId && !shipGroupSeqId) {
    shipGroupSeqId = "00001";
}

picklistBinId = parameters.picklistBinId;
if (picklistBinId) {
    picklistBin = delegator.findOne("PicklistBin", [picklistBinId : picklistBinId], false);
    if (picklistBin) {
        orderId = picklistBin.primaryOrderId;
        shipGroupSeqId = picklistBin.primaryShipGroupSeqId;
    }
}

context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
context.picklistBinId = picklistBinId;

if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        context.orderId = orderId;
        context.orderHeader = orderHeader;
        context.orderReadHelper = orh;
        orderItemShipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
        context.orderItemShipGroup = orderItemShipGroup;
        orderItems = orh.getOrderItems();
        context.orderItems = orderItems;
        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            if (shipGroupSeqId) {
                productStoreId = orh.getProductStoreId();
                context.productStoreId = productStoreId;
            } else {
                request.setAttribute("errorMessageList", ['No ship group sequence ID. Cannot process.']);
            }
        } else {
            request.setAttribute("errorMessageList", ["Order #" + orderId + " is not approved for picking."]);
        }
    } else {
        request.setAttribute("errorMessageList", ["Order #" + orderId + " cannot be found."]);
    }
}
context.verifyPickSession = verifyPickSession;