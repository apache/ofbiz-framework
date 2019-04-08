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

import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.shipment.weightPackage.WeightPackageSession

weightPackageSession = session.getAttribute("weightPackageSession")
if (!weightPackageSession) {
    weightPackageSession = new WeightPackageSession(dispatcher, userLogin)
    session.setAttribute("weightPackageSession", weightPackageSession)
}
context.weightPackageSession = weightPackageSession

showWarningForm = parameters.showWarningForm
if (!showWarningForm) {
    showWarningForm = request.getAttribute("showWarningForm")
    if (!showWarningForm) {
        showWarningForm = false
    }
}
context.showWarningForm = showWarningForm

orderId = parameters.orderId
shipGroupSeqId = parameters.shipGroupSeqId

shipment = from("Shipment").where("primaryOrderId", orderId, "statusId", "SHIPMENT_PICKED").queryFirst()
context.shipment = shipment
if (shipment) {
    invoice = from("ShipmentItemBilling").where("shipmentId", shipment.shipmentId).queryFirst()
    context.invoice = invoice
} else {
    context.invoice = null
}
actualCost = null
if (shipment) {
    shipmentRouteSegment = from("ShipmentRouteSegment").where("shipmentId", shipment.shipmentId).queryFirst()
    actualCost = shipmentRouteSegment.actualCost
    if (actualCost) {
        context.shipmentPackages = from("ShipmentPackage").where("shipmentId", shipment.shipmentId).queryList()
    }
}

facilityId = parameters.facilityId
if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne()
    context.facility = facility
}

if (orderId && !shipGroupSeqId && orderId.indexOf("/") > -1) {
    idSplit = orderId.split("\\/")
    orderId = idSplit[0]
    shipGroupSeqId = idSplit[1]
} else if (orderId && !shipGroupSeqId) {
    shipGroupSeqId = "00001"
}

picklistBinId = parameters.picklistBinId
if (picklistBinId) {
    picklistBin = from("PicklistBin").where("picklistBinId", picklistBinId).queryOne()
    if (picklistBin) {
        orderId = picklistBin.primaryOrderId
        shipGroupSeqId = picklistBin.primaryShipGroupSeqId
    } else {
        picklistBinId = null
    }
}

shipmentId = parameters.shipmentId
if (!shipmentId) {
    shipmentId = request.getAttribute("shipmentId")
}
if (!shipmentId && shipment) {
    shipmentId = shipment.shipmentId
}
context.shipmentId = shipmentId
if (shipmentId) {
    // Get the primaryOrderId from the shipment
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne()
    if (shipment && shipment.primaryOrderId) {
        orderItemBillingList = from("OrderItemBilling").where("orderId", shipment.primaryOrderId).orderBy("invoiceId").queryList()
        invoiceIds = EntityUtil.getFieldListFromEntityList(orderItemBillingList, "invoiceId", true)
        if (invoiceIds) {
            context.invoiceIds = invoiceIds
            
        }
    }
    if (shipment.statusId && "SHIPMENT_PACKED" == shipment.statusId) {
        orderId = null
    }
    shipmentPackageRouteSegs = from("ShipmentPackageRouteSeg").where("shipmentId", shipmentId).queryList()
    shipmentPackageRouteSegList = []
    shipmentPackageRouteSegs.each { shipmentPackageRouteSeg ->
        if (shipmentPackageRouteSeg.labelImage) {
            shipmentPackageRouteSegList.add(shipmentPackageRouteSeg)
        }
    }
    context.shipmentPackageRouteSegList = shipmentPackageRouteSegList
}

weightPackageSession.setShipmentId(shipmentId)
weightPackageSession.setPrimaryShipGroupSeqId(shipGroupSeqId)
weightPackageSession.setPrimaryOrderId(orderId)
weightPackageSession.setPicklistBinId(picklistBinId)
weightPackageSession.setFacilityId(facilityId)
context.primaryOrderId = orderId

carrierPartyId = null
if (orderId) {
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
    if (orderHeader) {
        OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader)
        GenericValue orderItemShipGroup = orderReadHelper.getOrderItemShipGroup(shipGroupSeqId)
        carrierPartyId = orderItemShipGroup.carrierPartyId
        if ("ORDER_APPROVED".equals(orderHeader.statusId)) {
            if (shipGroupSeqId) {
            if (shipment) {
                productStoreId = orderReadHelper.getProductStoreId()
                shippableItemInfo = orderReadHelper.getOrderItemAndShipGroupAssoc(shipGroupSeqId)
                shippableItems = from("OrderItemAndShipGrpInvResAndItemSum").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryList()
                shippableTotal = orderReadHelper.getShippableTotal(shipGroupSeqId)
                shippableWeight = orderReadHelper.getShippableWeight(shipGroupSeqId)
                shippableQuantity = orderReadHelper.getShippableQuantity(shipGroupSeqId)
                estimatedShippingCost = weightPackageSession.getShipmentCostEstimate(orderItemShipGroup, orderId, productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity)
                if (weightPackageSession.getPackedLines(orderId)) {
                    shipWeight = weightPackageSession.getShippableWeight(orderId)
                    newEstimatedShippingCost = weightPackageSession.getShipmentCostEstimate(orderItemShipGroup, orderId, productStoreId, shippableItemInfo, shippableTotal, shipWeight, shippableQuantity)
                    context.newEstimatedShippingCost = newEstimatedShippingCost
                }
                context.productStoreId = productStoreId
                context.estimatedShippingCost = estimatedShippingCost
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotVerified", ["orderId" : orderId], locale))
                orderId = null
            }
            } else {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoShipGroupSequenceIdFoundCannotProcess", locale))
                orderId = null
            }
        } else {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderNotApprovedForPacking", [orderId : orderId], locale))
            orderId = null
        }
    } else {
        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorOrderIdNotFound", [orderId : orderId], locale))
        orderId = null
    }
    context.orderedQuantity = weightPackageSession.getOrderedQuantity(orderId)
}
weightPackageSession.setCarrierPartyId(carrierPartyId)
context.orderId = orderId
context.shipGroupSeqId = shipGroupSeqId
context.picklistBinId = picklistBinId

if (carrierPartyId) {
    carrierShipmentBoxTypes = from("CarrierShipmentBoxType").where("partyId", carrierPartyId).queryList()
    shipmentBoxTypes = []
    carrierShipmentBoxTypes.each { carrierShipmentBoxType ->
        shipmentBoxTypes.add(from("ShipmentBoxType").where("shipmentBoxTypeId", carrierShipmentBoxType.shipmentBoxTypeId).queryOne())
        context.shipmentBoxTypes = shipmentBoxTypes
    }
}

if (actualCost) {
    context.newEstimatedShippingCost = actualCost
}

defaultDimensionUomId = null
if (facility) {
    defaultDimensionUomId = facility.defaultDimensionUomId
}
if (!defaultDimensionUomId) {
    defaultDimensionUomId = EntityUtilProperties.getPropertyValue("shipment", "shipment.default.dimension.uom", "LEN_in", delegator)
}
context.defaultDimensionUomId = defaultDimensionUomId

defaultWeightUomId = null
if (facility) {
    defaultWeightUomId = facility.defaultWeightUomId
}
if (!defaultWeightUomId) {
    defaultWeightUomId = EntityUtilProperties.getPropertyValue("shipment", "shipment.default.weight.uom", "WT_kg", delegator)
}
context.defaultWeightUomId = defaultWeightUomId
