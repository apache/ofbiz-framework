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

import org.ofbiz.base.util.*;
import org.ofbiz.order.order.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;


facilityId = parameters.facilityId;
if (facilityId) {
    facility = delegator.findOne("Facility", [facilityId : facilityId], false);
    context.facilityId = facilityId;
    context.facility = facility;
}

// order based packing
orderId = parameters.orderId;
shipGroupSeqId = parameters.shipGroupSeqId;
shipmentId = parameters.shipmentId;
if (!shipmentId) {
    shipmentId = request.getAttribute("shipmentId");
}
context.shipmentId = shipmentId;

// If a shipment exists, provide the IDs of any related invoices
invoiceIds = null;
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
}

// validate order information
if (orderId && !shipGroupSeqId && orderId.indexOf("/") > -1) {
    // split the orderID/shipGroupSeqID
    idSplit = orderId.split("\\/");
    orderId = idSplit[0];
    shipGroupSeqId = idSplit[1];
} else if (orderId && !shipGroupSeqId) {
    shipGroupSeqId = "00001";
}

// setup the packing session
packSession = session.getAttribute("packingSession");
clear = parameters.clear;
if (!packSession) {
    packSession = new org.ofbiz.shipment.packing.PackingSession(dispatcher, userLogin);
    session.setAttribute("packingSession", packSession);
    Debug.log("Created NEW packing session!!");
} else {
    if (packSession.getStatus() == 0) {
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        shipGrp = orh.getOrderItemShipGroup(shipGroupSeqId);
        context.shippedShipGroupSeqId = shipGroupSeqId;
        context.shippedOrderId = orderId;
        context.shippedCarrier = shipGrp.carrierPartyId;

        packSession.clear();
        shipGroupSeqId = null;
        orderId = null;
    } else if (clear) {
        packSession.clear();
    }
}
packSession.clearItemInfos();

// picklist based packing information
picklistBinId = parameters.picklistBinId;
// see if the bin ID is already set
if (!picklistBinId) {
    picklistBinId = packSession.getPicklistBinId();
}
if (picklistBinId) {
    bin = delegator.findOne("PicklistBin", [picklistBinId : picklistBinId], false);
    if (bin) {
        orderId = bin.primaryOrderId;
        shipGroupSeqId = bin.primaryShipGroupSeqId;
        packSession.addItemInfo(bin.getRelated("PicklistItem", [itemStatusId : 'PICKITEM_PENDING'], null, false));
        //context.put("picklistItemInfos", bin.getRelated("PicklistItem", UtilMisc.toMap("itemStatusId", "PICKITEM_PENDING"), null, false));
    }
} else {
    picklistBinId = null;
}

// make sure we always re-set the infos
packSession.setPrimaryShipGroupSeqId(shipGroupSeqId);
packSession.setPrimaryOrderId(orderId);
packSession.setPicklistBinId(picklistBinId);
packSession.setFacilityId(facilityId);

if (invoiceIds) {
    orderId = null;
}
shipment = EntityUtil.getFirst(delegator.findByAnd("Shipment", [primaryOrderId : orderId, statusId : "SHIPMENT_PICKED"], null, false));
context.shipment = shipment;

context.packingSession = packSession;
context.orderId = orderId;
context.shipGroupSeqId = shipGroupSeqId;
context.picklistBinId = picklistBinId;

// grab the order information
if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        context.orderId = orderId;
        context.orderHeader = orderHeader;
        context.orderReadHelper = orh;
        orderItemShipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
        context.orderItemShipGroup = orderItemShipGroup;
        carrierPartyId = orderItemShipGroup.carrierPartyId;
            carrierShipmentBoxTypes = delegator.findList("CarrierShipmentBoxType", EntityCondition.makeCondition([partyId : carrierPartyId]), null, null, null, false);
            if (carrierShipmentBoxTypes) {
            context.carrierShipmentBoxTypes = carrierShipmentBoxTypes;
            }

        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            if (shipGroupSeqId) {
                if (!shipment) {
    
                    // Generate the shipment cost estimate for the ship group
                    productStoreId = orh.getProductStoreId();
                    shippableItemInfo = orh.getOrderItemAndShipGroupAssoc(shipGroupSeqId);
                    shippableItems = delegator.findList("OrderItemAndShipGrpInvResAndItemSum", EntityCondition.makeCondition([orderId : orderId, shipGroupSeqId : shipGroupSeqId]), null, null, null, false);
                    shippableTotal = new Double(orh.getShippableTotal(shipGroupSeqId).doubleValue());
                    shippableWeight = new Double(orh.getShippableWeight(shipGroupSeqId).doubleValue());
                    shippableQuantity = new Double(orh.getShippableQuantity(shipGroupSeqId).doubleValue());
                    if (orderItemShipGroup.contactMechId && orderItemShipGroup.shipmentMethodTypeId && orderItemShipGroup.carrierPartyId && orderItemShipGroup.carrierRoleTypeId) {
                        shipmentCostEstimate = packSession.getShipmentCostEstimate(orderItemShipGroup, productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
                        context.shipmentCostEstimateForShipGroup = shipmentCostEstimate;
                    }
                    context.productStoreId = productStoreId;
    
                    if (!picklistBinId) {
                        packSession.addItemInfo(shippableItems);
                        //context.put("itemInfos", shippableItemInfo);
                    }
                } else {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderHasBeenAlreadyVerified", [orderId : orderId], locale));
                }
            } else {
                request.setAttribute("errorMessageList", ['No ship group sequence ID. Cannot process.']);
            }
        } else {
            request.setAttribute("errorMessageList", ["Order #" + orderId + " is not approved for packing."]);
        }
    } else {
        request.setAttribute("errorMessageList", ["Order #" + orderId + " cannot be found."]);
    }
}

// Try to get the defaultWeightUomId first from the facility, then from the shipment properties, and finally defaulting to kilos
defaultWeightUomId = null;
if (facility) {
    defaultWeightUomId = facility.defaultWeightUomId;
}
if (!defaultWeightUomId) {
    defaultWeightUomId = UtilProperties.getPropertyValue("shipment.properties", "shipment.default.weight.uom", "WT_kg");
}
context.defaultWeightUomId = defaultWeightUomId;
