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
import org.ofbiz.shipment.weightPackage.WeightPackageSession;

weightPackageSession = session.getAttribute("weightPackageSession");
if (!weightPackageSession) {
    weightPackageSession = new WeightPackageSession(dispatcher, userLogin);
    session.setAttribute("weightPackageSession", weightPackageSession);
}
context.weightPackageSession = weightPackageSession;

orderId = parameters.orderId;
shipGroupSeqId = parameters.shipGroupSeqId;

shipment = EntityUtil.getFirst(delegator.findByAnd("Shipment", [primaryOrderId : orderId, statusId : "SHIPMENT_PICKED"]));
context.shipment = shipment;
if (shipment) {
    invoice = EntityUtil.getFirst(delegator.findByAnd("ShipmentItemBilling", [shipmentId : shipment.shipmentId]));
    context.invoice = invoice;
} else {
    context.invoice = null;
}

facilityId = parameters.facilityId;
if (facilityId) {
    facility = delegator.findOne("Facility", [facilityId : facilityId], false);
    context.facility = facility;
}

if (orderId && !shipGroupSeqId && orderId.indexOf("/") > -1) {
    idSplit = orderId.split("\\/");
    orderId = idSplit[0];
    shipGroupSeqId = idSplit[1];
} else if (orderId && !shipGroupSeqId) {
    shipGroupSeqId = "00001";
}

picklistBinId = parameters.picklistBinId;
if (picklistBinId) {
    picklistBin = delegator.findOne("PicklistBin", [picklistBinId : picklistBinId], false);
    if (picklistBin) {
        orderId = picklistBin.primaryOrderId;
        shipGroupSeqId = picklistBin.primaryShipGroupSeqId;
    } else {
        picklistBinId = null;
    }
}

weightPackageSession.setPrimaryShipGroupSeqId(shipGroupSeqId);
weightPackageSession.setPrimaryOrderId(orderId);
weightPackageSession.setPicklistBinId(picklistBinId);
weightPackageSession.setFacilityId(facilityId);

if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
        GenericValue orderItemShipGroup = orderReadHelper.getOrderItemShipGroup(shipGroupSeqId);
        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            if (shipGroupSeqId) {
                productStoreId = orderReadHelper.getProductStoreId();
                shippableItemInfo = orderReadHelper.getOrderItemAndShipGroupAssoc(shipGroupSeqId);
                shippableItems = delegator.findList("OrderItemAndShipGrpInvResAndItemSum", EntityCondition.makeCondition([orderId : orderId, shipGroupSeqId : shipGroupSeqId]), null, null, null, false);
                shippableTotal = orderReadHelper.getShippableTotal(shipGroupSeqId);
                shippableWeight = orderReadHelper.getShippableWeight(shipGroupSeqId);
                shippableQuantity = orderReadHelper.getShippableQuantity(shipGroupSeqId);
                estimatedShippingCost = weightPackageSession.getShipmentCostEstimate(orderItemShipGroup, orderId, productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
                if (weightPackageSession.getPackedLines(orderId)) {
                    shipWeight = weightPackageSession.getShippableWeight(orderId);
                    newEstimatedShippingCost = weightPackageSession.getShipmentCostEstimate(orderItemShipGroup, orderId, productStoreId, shippableItemInfo, shippableTotal, shipWeight, shippableQuantity);
                    context.newEstimatedShippingCost = newEstimatedShippingCost;
                }
                context.productStoreId = productStoreId;
                context.estimatedShippingCost = estimatedShippingCost;
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoShipGroupSequenceIdFoundCannotProcess", locale));
            }
        } else {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotApprovedForPacking", [orderId : orderId], locale));
        }
    } else {
        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderIdNotFound", [orderId : orderId], locale));
    }
}

context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
context.picklistBinId = picklistBinId;

shipmentBoxTypes = delegator.findList("ShipmentBoxType", null, null, ["description"], null, true);
context.shipmentBoxTypes = shipmentBoxTypes;

defaultDimensionUomId = null;
if (facility) {
    defaultDimensionUomId = facility.defaultDimensionUomId;
}
if (!defaultDimensionUomId) {
    defaultDimensionUomId = UtilProperties.getPropertyValue("shipment.properties", "shipment.default.dimension.uom", "LEN_in");
}
context.defaultDimensionUomId = defaultDimensionUomId;

defaultWeightUomId = null;
if (facility) {
    defaultWeightUomId = facility.defaultWeightUomId;
}
if (!defaultWeightUomId) {
    defaultWeightUomId = UtilProperties.getPropertyValue("shipment.properties", "shipment.default.weight.uom", "WT_kg");
}
context.defaultWeightUomId = defaultWeightUomId;