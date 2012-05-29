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

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.shipment.verify.VerifyPickSession;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityOperator;

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
        shipmentItemBillingList = shipment.getRelated("ShipmentItemBilling", null, null, false);
        invoiceIds = EntityUtil.getFieldListFromEntityList(shipmentItemBillingList, "invoiceId", true);
        if (invoiceIds) {
            context.invoiceIds = invoiceIds;
            parameters.orderId = null;
        }
    }
}

facilityId = parameters.facilityId;
if (facilityId) {
    facility = delegator.findOne("Facility", [facilityId : facilityId], false);
    context.facility = facility;
}
verifyPickSession.setFacilityId(facilityId);
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
        verifyPickSession.setPicklistBinId(picklistBinId);
    }
}

if (orderId && !picklistBinId) {
    picklistBin = EntityUtil.getFirst(delegator.findByAnd("PicklistBin", [primaryOrderId : orderId], null, false));
    if (picklistBin) {
        picklistBinId = picklistBin.picklistBinId;
        verifyPickSession.setPicklistBinId(picklistBinId);
    }
}

context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
context.picklistBinId = picklistBinId;
context.isOrderStatusApproved = false;

if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        context.orderId = orderId;
        context.orderHeader = orderHeader;
        context.orderReadHelper = orh;
        
        orderItemShipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
        context.orderItemShipGroup = orderItemShipGroup;
        List exprs = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        orderItems = orh.getOrderItemsByCondition(exprs);
        context.orderItems = orderItems;
        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            context.isOrderStatusApproved = true;
            if (shipGroupSeqId) {
                productStoreId = orh.getProductStoreId();
                context.productStoreId = productStoreId;
                shipments = delegator.findByAnd("Shipment", [primaryOrderId : orderId, statusId : "SHIPMENT_PICKED"], null, false);
                if (shipments) {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorAllItemsOfOrderAreAlreadyVerified", [orderId : orderId], locale));
                }
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoShipGroupSequenceIdFoundCannotProcess", locale));
            }
        } else {
            context.isOrderStatusApproved = false;
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotApprovedForPicking", [orderId : orderId], locale));
        }
    } else {
        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderIdNotFound", [orderId : orderId], locale));
    }
}
context.verifyPickSession = verifyPickSession;
