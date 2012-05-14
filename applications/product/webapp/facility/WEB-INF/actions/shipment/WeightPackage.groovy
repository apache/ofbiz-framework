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
import org.ofbiz.entity.GenericValue;
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

showWarningForm = parameters.showWarningForm;
if (!showWarningForm) {
    showWarningForm = request.getAttribute("showWarningForm");
    if (!showWarningForm) {
        showWarningForm = false;
    }
}
context.showWarningForm = showWarningForm;

orderId = parameters.orderId;
shipGroupSeqId = parameters.shipGroupSeqId;

shipment = EntityUtil.getFirst(delegator.findByAnd("Shipment", [primaryOrderId : orderId, statusId : "SHIPMENT_PICKED"], null, false));
context.shipment = shipment;
if (shipment) {
    invoice = EntityUtil.getFirst(delegator.findByAnd("ShipmentItemBilling", [shipmentId : shipment.shipmentId], null, false));
    context.invoice = invoice;
} else {
    context.invoice = null;
}
actualCost = null;
if (shipment) {
    shipmentRouteSegment = EntityUtil.getFirst(delegator.findByAnd("ShipmentRouteSegment", [shipmentId : shipment.shipmentId], null, false));
    actualCost = shipmentRouteSegment.actualCost;
    if (actualCost) {
        context.shipmentPackages = delegator.findByAnd("ShipmentPackage", [shipmentId : shipment.shipmentId], null, false);
    }
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

shipmentId = parameters.shipmentId;
if (!shipmentId) {
    shipmentId = request.getAttribute("shipmentId");
}
if (!shipmentId && shipment) {
    shipmentId = shipment.shipmentId;
}
context.shipmentId = shipmentId;
if (shipmentId) {
    // Get the primaryOrderId from the shipment
    shipment = delegator.findOne("Shipment",  [shipmentId : shipmentId], false);
    if (shipment && shipment.primaryOrderId) {
        orderItemBillingList = delegator.findList("OrderItemBilling", EntityCondition.makeCondition([orderId : shipment.primaryOrderId]), null, ['invoiceId'], null, false);
        invoiceIds = EntityUtil.getFieldListFromEntityList(orderItemBillingList, "invoiceId", true);
        if (invoiceIds) {
            context.invoiceIds = invoiceIds;
            
        }
    }
    if (shipment.statusId && "SHIPMENT_PACKED" == shipment.statusId) {
        orderId = null;
    }
    shipmentPackageRouteSegs = delegator.findByAnd("ShipmentPackageRouteSeg",  [shipmentId : shipmentId], null, false);
    shipmentPackageRouteSegList = [];
    shipmentPackageRouteSegs.each { shipmentPackageRouteSeg ->
        if (shipmentPackageRouteSeg.labelImage) {
            shipmentPackageRouteSegList.add(shipmentPackageRouteSeg);
        }
    }
    context.shipmentPackageRouteSegList = shipmentPackageRouteSegList;
}

weightPackageSession.setShipmentId(shipmentId)
weightPackageSession.setPrimaryShipGroupSeqId(shipGroupSeqId);
weightPackageSession.setPrimaryOrderId(orderId);
weightPackageSession.setPicklistBinId(picklistBinId);
weightPackageSession.setFacilityId(facilityId);
context.primaryOrderId = orderId;

carrierPartyId = null;
if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
        GenericValue orderItemShipGroup = orderReadHelper.getOrderItemShipGroup(shipGroupSeqId);
        carrierPartyId = orderItemShipGroup.carrierPartyId;
        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            if (shipGroupSeqId) {
            if (shipment) {
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
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotVerified", ["orderId" : orderId], locale));
                orderId = null;
            }
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoShipGroupSequenceIdFoundCannotProcess", locale));
                orderId = null;
            }
        } else {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotApprovedForPacking", [orderId : orderId], locale));
            orderId = null;
        }
    } else {
        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderIdNotFound", [orderId : orderId], locale));
        orderId = null;
    }
    context.orderedQuantity = weightPackageSession.getOrderedQuantity(orderId);
}
weightPackageSession.setCarrierPartyId(carrierPartyId);
context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
context.picklistBinId = picklistBinId;

if (carrierPartyId) {
    carrierShipmentBoxTypes =  delegator.findByAnd("CarrierShipmentBoxType", [partyId : carrierPartyId], null, false);
    shipmentBoxTypes = [];
    carrierShipmentBoxTypes.each { carrierShipmentBoxType ->
        shipmentBoxTypes.add(delegator.findOne("ShipmentBoxType", [shipmentBoxTypeId : carrierShipmentBoxType.shipmentBoxTypeId], false));
        context.shipmentBoxTypes = shipmentBoxTypes;
    }
}

if (actualCost) {
    context.newEstimatedShippingCost = actualCost;
}

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
