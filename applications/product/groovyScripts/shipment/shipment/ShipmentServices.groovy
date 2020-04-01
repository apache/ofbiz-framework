/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.product.product.ProductWorker
import org.apache.ofbiz.service.ServiceUtil


/**
 * Create Shipment
 * @return
 */
def createShipment() {
    Map result = success()
    GenericValue newEntity = makeValue("Shipment")
    newEntity.setNonPKFields(parameters)

    if (parameters.shipmentId) {
        newEntity.setPKFields(parameters)
    } else {
        newEntity.shipmentId = delegator.getNextSeqId("Shipment")
    }
    result.shipmentId = newEntity.shipmentId
    String shipmentTypeId = parameters.shipmentTypeId
    // set the created and lastModified info
    newEntity.createdDate = UtilDateTime.nowTimestamp()
    newEntity.createdByUserLogin = userLogin.userLoginId
    newEntity.lastModifiedDate = UtilDateTime.nowTimestamp()
    newEntity.lastModifiedByUserLogin = userLogin.userLoginId
    /*
     * if needed create some WorkEfforts and remember their IDs:
     * estimatedShipDate: estimatedShipWorkEffId
     * estimatedArrivalDate: estimatedArrivalWorkEffId
     */
    if (parameters.estimatedShipDate) {
        Map shipWorkEffortMap = [workEffortName: "Shipment #${newEntity.shipmentId} ${newEntity.primaryOrderId} Ship"]
        if ((shipmentTypeId == "OUTGOING_SHIPMENT") || (shipmentTypeId == "SALES_SHIPMENT") || (shipmentTypeId == "PURCHASE_RETURN")) {
            shipWorkEffortMap.workEffortTypeId = "SHIPMENT_OUTBOUND"
        }
        shipWorkEffortMap.currentStatusId = "CAL_TENTATIVE"
        shipWorkEffortMap.workEffortPurposeTypeId = "WEPT_WAREHOUSING"
        shipWorkEffortMap.estimatedStartDate = parameters.estimatedShipDate
        shipWorkEffortMap.estimatedCompletionDate = parameters.estimatedShipDate
        shipWorkEffortMap.facilityId = parameters.originFacilityId
        shipWorkEffortMap.quickAssignPartyId = userLogin.partyId
        Map serviceResultSD = run service: "createWorkEffort", with: shipWorkEffortMap
        newEntity.estimatedShipWorkEffId = serviceResultSD.workEffortId
        if (newEntity.partyIdFrom) {
            Map assignPartyToWorkEffortShip = [workEffortId: newEntity.estimatedShipWorkEffId, partyId: newEntity.partyIdFrom, roleTypeId: "CAL_ATTENDEE", statusId: "CAL_SENT"]
            run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortShip
        }
    }
    if (parameters.estimatedArrivalDate) {
        Map arrivalWorkEffortMap = [workEffortName: "Shipment #${newEntity.shipmentId} ${newEntity.primaryOrderId} Arrival"]
        if ((shipmentTypeId == "INCOMING_SHIPMENT") || (shipmentTypeId == "PURCHASE_SHIPMENT") || (shipmentTypeId == "SALES_RETURN")) {
            arrivalWorkEffortMap.workEffortTypeId = "SHIPMENT_INBOUND"
        }
        arrivalWorkEffortMap.currentStatusId = "CAL_TENTATIVE"
        arrivalWorkEffortMap.workEffortPurposeTypeId = "WEPT_WAREHOUSING"
        arrivalWorkEffortMap.estimatedStartDate = parameters.estimatedArrivalDate
        arrivalWorkEffortMap.estimatedCompletionDate = parameters.estimatedArrivalDate
        arrivalWorkEffortMap.facilityId = parameters.destinationFacilityId
        arrivalWorkEffortMap.quickAssignPartyId = userLogin.partyId
        Map serviceResultAD = run service: "createWorkEffort", with: arrivalWorkEffortMap
        newEntity.estimatedArrivalWorkEffId = serviceResultAD.workEffortId
        if (newEntity.partyIdTo) {
            Map assignPartyToWorkEffortArrival = [workEffortId: newEntity.estimatedArrivalWorkEffId, partyId: newEntity.partyIdTo, roleTypeId: "CAL_ATTENDEE", statusId: "CAL_SENT"]
            run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortArrival
        }
    }
    newEntity.create()

    // get the ShipmentStatus history started
    if (newEntity.statusId) {
        Map createShipmentStatusMap = [shipmentId: newEntity.shipmentId, statusId: newEntity.statusId]
        run service: "createShipmentStatus", with: createShipmentStatusMap
    }
    return result
}

/**
 * Update Shipment
 * @return
 */
def updateShipment() {
    Map result = success()
    List errorList = []
    GenericValue lookedUpValue = from("Shipment").where(parameters).queryOne()
    // put the type in return map so that service consumer knows what type of shipment was updated
    result.shipmentTypeId = lookedUpValue.shipmentTypeId

    if (parameters.statusId) {
        if (parameters.statusId != lookedUpValue.statusId) {
            // make sure a StatusValidChange record exists, if not return error
            GenericValue checkStatusValidChange = from("StatusValidChange").where(statusId: lookedUpValue.statusId, statusIdTo: parameters.statusId).queryOne()
            if (!checkStatusValidChange) {
                errorList.add("ERROR: Changing the status from ${lookedUpValue.statusId} to ${parameters.statusId} is not allowed.")
            }
            Map createShipmentStatusMap = [shipmentId: parameters.shipmentId, statusId: parameters.statusId]
            if (parameters.eventDate) {
                createShipmentStatusMap.statusDate = parameters.eventDate
            }
            run service: "createShipmentStatus", with: createShipmentStatusMap
        }
    }
    // now finally check for errors
    if (errorList) {
        logError(errorList)
        return error(errorList)
    }
    // Check the pickup and delivery dates for changes and update the corresponding WorkEfforts
    if (((parameters.estimatedShipDate) && (parameters.estimatedShipDate != lookedUpValue.estimatedShipDate))
    || ((parameters.originFacilityId) && (parameters.originFacilityId != lookedUpValue.originFacilityId))
    || ((parameters.statusId) && (parameters.statusId != lookedUpValue.statusId)
    && ((parameters.statusId == "SHIPMENT_CANCELLED") || (parameters.statusId == "SHIPMENT_PACKED") || (parameters.statusId == "SHIPMENT_SHIPPED")))) {
        GenericValue estShipWe = from("WorkEffort").where(workEffortId: lookedUpValue.estimatedShipWorkEffId).queryOne()
        if (estShipWe) {
            estShipWe.estimatedStartDate = parameters.estimatedShipDate
            estShipWe.estimatedCompletionDate = parameters.estimatedShipDate
            estShipWe.facilityId = parameters.originFacilityId
            if ((parameters.statusId) && (parameters.statusId != lookedUpValue.statusId)) {
                if (parameters.statusId == "SHIPMENT_CANCELLED") {
                    estShipWe.currentStatusId = "CAL_CANCELLED"
                }
                if (parameters.statusId == "SHIPMENT_PACKED") {
                    estShipWe.currentStatusId = "CAL_CONFIRMED"
                }
                if (parameters.statusId == "SHIPMENT_SHIPPED") {
                    estShipWe.currentStatusId = "CAL_COMPLETED"
                }
            }
            Map estShipWeUpdMap = [:]
            estShipWeUpdMap << estShipWe
            run service: "updateWorkEffort", with: estShipWeUpdMap
        }
    }
    if (((parameters.estimatedArrivalDate) && (parameters.estimatedArrivalDate != lookedUpValue.estimatedArrivalDate))
    || ((parameters.destinationFacilityId) && (parameters.destinationFacilityId != lookedUpValue.destinationFacilityId))) {
        GenericValue estimatedArrivalWorkEffort = from("WorkEffort").where(workEffortId: lookedUpValue.estimatedArrivalWorkEffId).queryOne()
        if (estimatedArrivalWorkEffort) {
            estimatedArrivalWorkEffort.estimatedStartDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.estimatedCompletionDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.facilityId = parameters.destinationFacilityId
            Map estimatedArrivalWorkEffortUpdMap = [:]
            estimatedArrivalWorkEffortUpdMap << estimatedArrivalWorkEffort
            run service: "updateWorkEffort", with: estimatedArrivalWorkEffortUpdMap
        }
    }
    // if the partyIdTo or partyIdFrom has changed, add WEPAs
    if ((parameters.partyIdFrom) && (parameters.partyIdFrom != lookedUpValue.partyIdFrom) && (lookedUpValue.estimatedShipWorkEffId)) {
        Map assignPartyToWorkEffortShip = [workEffortId: lookedUpValue.estimatedShipWorkEffId, partyId: parameters.partyIdFrom]
        List existingShipWepas = from("WorkEffortPartyAssignment").where(assignPartyToWorkEffortShip).filterByDate().queryList()
        if (!existingShipWepas) {
            assignPartyToWorkEffortShip.roleTypeId = "CAL_ATTENDEE"
            assignPartyToWorkEffortShip.statusId = "CAL_SENT"
            run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortShip
        }
    }
    if ((parameters.partyIdTo) && (parameters.partyIdTo != lookedUpValue.partyIdTo) && (lookedUpValue.estimatedArrivalWorkEffId)) {
        Map assignPartyToWorkEffortArrival = [workEffortId: lookedUpValue.estimatedArrivalWorkEffId, partyId: parameters.partyIdTo]
        List existingArrivalWepas = from("WorkEffortPartyAssignment").where(assignPartyToWorkEffortArrival).filterByDate().queryList()
        if (!existingArrivalWepas) {
            assignPartyToWorkEffortArrival.roleTypeId = "CAL_ATTENDEE"
            assignPartyToWorkEffortArrival.statusId = "CAL_SENT"
            run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortArrival
        }
    }
    // finally before setting nonpk fields, set the oldStatusId, oldPrimaryOrderId, oldOriginFacilityId, oldDestinationFacilityId
    result.oldStatusId = lookedUpValue.statusId
    result.oldPrimaryOrderId = lookedUpValue.primaryOrderId
    result.oldOriginFacilityId = lookedUpValue.originFacilityId
    result.oldDestinationFacilityId = lookedUpValue.destinationFacilityId

    // now that all changes have been checked, set the nonpks
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.lastModifiedDate = UtilDateTime.nowTimestamp()
    lookedUpValue.lastModifiedByUserLogin = userLogin.userLoginId
    result.shipmentId = lookedUpValue.shipmentId
    lookedUpValue.store()
    return result
}

/**
 * Create Shipment based on ReturnHeader
 * @return
 */
def createShipmentForReturn() {
    Map result = success()
    GenericValue returnHeader = from("ReturnHeader").where(returnId: parameters.returnId).queryOne()
    Map shipmentCtx = [partyIdFrom: returnHeader.fromPartyId, partyIdTo: returnHeader.toPartyId, originContactMechId: returnHeader.originContactMechId, destinationFacilityId: returnHeader.destinationFacilityId, primaryReturnId: returnHeader.returnId]
    // later different behavior for customer vs. returns would happen here
    if (returnHeader.returnHeaderTypeId.contains("CUSTOMER_")) {
        shipmentCtx.shipmentTypeId = "SALES_RETURN"
        shipmentCtx.statusId = "PURCH_SHIP_CREATED" // we may later need different status codes for return shipments
    } else if (returnHeader.returnHeaderTypeId == "VENDOR_RETURN") {
        shipmentCtx.shipmentTypeId = "PURCHASE_RETURN"
        shipmentCtx.statusId = "SHIPMENT_INPUT"
    } else {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityReturnHeaderTypeNotSupported", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map serviceResult = run service: "createShipment", with: shipmentCtx
    result.shipmentId = serviceResult.shipmentId
    return result
}

/**
 * Create Shipment and ShipmentItems based on ReturnHeader and ReturnItems
 * @return
 */
def createShipmentAndItemsForReturn() {
    Map result = success()
    List returnItems = from("ReturnItem").where(returnId: parameters.returnId).queryList()

    // The return shipment is created if the return contains one or more physical products
    Boolean isPhysicalProductAvailable = false
    for (GenericValue returnItem : returnItems) {
        GenericValue product = delegator.getRelatedOne("Product", returnItem, false)
        if (product) {
            ProductWorker productWorker = new ProductWorker()
            Boolean isPhysicalProduct = productWorker.isPhysical(product)
            if (isPhysicalProduct) {
                isPhysicalProductAvailable = true
            }
        }
    }
    if (isPhysicalProductAvailable) {
        Map serviceResult = run service: "createShipmentForReturn", with: parameters
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        String shipmentId = serviceResult.shipmentId
        logInfo("Created new shipment " + shipmentId)

        for (GenericValue returnItem : returnItems) {
            // Shipment items are created only for physical products
            Boolean isPhysicalProduct = false
            GenericValue product = delegator.getRelatedOne("Product", returnItem, false)
            ProductWorker productWorker = new ProductWorker()
            isPhysicalProduct = productWorker.isPhysical(product)

            if (isPhysicalProduct) {
                Map shipItemCtx = [shipmentId: shipmentId, productId: returnItem.productId, quantity: returnItem.returnQuantity]
                logInfo("calling create shipment item with ${shipItemCtx}")
                Map serviceResultCSI = run service: "createShipmentItem", with: shipItemCtx
                String shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
                shipItemCtx = [:]
                shipItemCtx.shipmentId = shipmentId
                shipItemCtx.shipmentItemSeqId = shipmentItemSeqId
                shipItemCtx.returnId = returnItem.returnId
                shipItemCtx.returnItemSeqId = returnItem.returnItemSeqId
                shipItemCtx.quantity = returnItem.returnQuantity
                run service: "createReturnItemShipment", with: shipItemCtx
            }
        }
        result.shipmentId = shipmentId
    }
    return result
}

/**
 * Create Shipment and ShipmentItems based on primaryReturnId for Vendor return
 * @return
 */
def createShipmentAndItemsForVendorReturn() {
    Map result = success()
    Map serviceResult = run service: "createShipment", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    String shipmentId = serviceResult.shipmentId
    logInfo("Created new shipment ${shipmentId}")
    List returnItems = from("ReturnItem").where(returnId: parameters.primaryReturnId).queryList()
    for (GenericValue returnItem : returnItems) {
        Map shipItemCtx = [shipmentId: shipmentId, productId: returnItem.productId, quantity: returnItem.returnQuantity]
        logInfo("calling create shipment item with ${shipItemCtx}")
        Map serviceResultCSI = run service: "createShipmentItem", with: shipItemCtx
        String shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
        shipItemCtx = [:]
        shipItemCtx.shipmentId = shipmentId
        shipItemCtx.shipmentItemSeqId = shipmentItemSeqId
        shipItemCtx.returnId =returnItem.returnId
        shipItemCtx.returnItemSeqId = returnItem.returnItemSeqId
        shipItemCtx.quantity = returnItem.returnQuantity
        run service: "createReturnItemShipment", with: shipItemCtx
    }
    result.shipmentId = shipmentId
    return result
}

/**
 * Set Shipment Settings From Primary Order
 * @return
 */
def setShipmentSettingsFromPrimaryOrder() {
    GenericValue orderItemShipGroup
    // on Shipment set partyIdFrom, partyIdTo (vendorPartyId), originContactMechId, destinationContactMechId, estimatedShipCost
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    if (!shipment?.primaryOrderId) {
        // No primaryOrderId specified, don't do anything
        logInfo("Not running setShipmentSettingsFromPrimaryOrder, primaryOrderId is empty for shipmentId [${shipment.shipmentId}]")
        return success()
    }
    // TODO: we may not want to check this if, for example, Purchase Orders don't have any OrderItemShipGroups
    if (!shipment.primaryShipGroupSeqId) {
        // No primaryShipGroupSeqId specified, don't do anything
        logInfo("Not running setShipmentSettingsFromPrimaryOrder, primaryShipGroupSeqId is empty for shipmentId [${parameters.shipmentId}]")
        return success()
    }
    GenericValue orderHeader = from("OrderHeader").where(orderId: shipment.primaryOrderId).queryOne()
    if (shipment.primaryShipGroupSeqId) {
        orderItemShipGroup = from("OrderItemShipGroup").where(orderId: shipment.primaryOrderId, shipGroupSeqId: shipment.primaryShipGroupSeqId).queryOne()
    }
    if (orderHeader.orderTypeId == "SALES_ORDER") {
        shipment.shipmentTypeId = "SALES_SHIPMENT"
    }
    if (orderHeader.orderTypeId == "PURCHASE_ORDER") {
        if (shipment.shipmentTypeId != "DROP_SHIPMENT") {
            shipment.shipmentTypeId = "PURCHASE_SHIPMENT"
        }
    }
    // set the facility if we are from a store with a single facility
    if ((!shipment.originFacilityId) && (shipment.shipmentTypeId == "SALES_SHIPMENT") && (orderHeader.productStoreId)) {
        GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
        if (productStore.oneInventoryFacility == "Y") {
            shipment.originFacilityId = productStore.inventoryFacilityId
        }
    }
    // partyIdFrom, partyIdTo (vendorPartyId) - NOTE: these work the same for Purchase and Sales Orders...
    List orderRoles = from("OrderRole").where(orderId: shipment.primaryOrderId).queryList()
    Map limitRoleMap = [:]
    List limitOrderRoles = []
    GenericValue limitOrderRole
    // From: SHIP_FROM_VENDOR
    if (!shipment.partyIdFrom) {
        limitRoleMap = [roleTypeId: "SHIP_FROM_VENDOR"]
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, limitRoleMap)
        if (limitOrderRoles) {
            limitOrderRole = limitOrderRoles.get(0)
            shipment.partyIdFrom = limitOrderRole.partyId
        }
        limitRoleMap = [:]
        limitOrderRoles = []
        limitOrderRole = null
    }
    // From: VENDOR
    if (!shipment.partyIdFrom) {
        limitRoleMap = [roleTypeId: "VENDOR"]
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, limitRoleMap)
        if (limitOrderRoles) {
            limitOrderRole = limitOrderRoles.get(0)
            shipment.partyIdFrom = limitOrderRole.partyId
        }
        limitRoleMap = [:]
        limitOrderRoles = []
        limitOrderRole = null
    }
    // To: SHIP_TO_CUSTOMER
    if (!shipment.partyIdTo) {
        limitRoleMap.roleTypeId = "SHIP_TO_CUSTOMER"
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, limitRoleMap)
        if (limitOrderRoles) {
            limitOrderRole = limitOrderRoles.get(0)
            shipment.partyIdTo = limitOrderRole.partyId
        }
        limitRoleMap = [:]
        limitOrderRoles = []
        limitOrderRole = null
    }
    // To: CUSTOMER
    if (!shipment.partyIdTo) {
        limitRoleMap.roleTypeId = "CUSTOMER"
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, limitRoleMap)
        if (limitOrderRoles) {
            limitOrderRole = limitOrderRoles.get(0)
            shipment.partyIdTo = limitOrderRole.partyId
        }
        limitRoleMap = [:]
        limitOrderRoles = []
        limitOrderRole = null
    }
    List orderContactMechs = from("OrderContactMech").where(orderId: shipment.primaryOrderId).queryList()
    // destinationContactMechId
    if (!shipment.destinationContactMechId) {
        // first try from orderContactMechs
        Map destinationContactMap = [contactMechPurposeTypeId: "SHIPPING_LOCATION"]
        List destinationOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs, destinationContactMap)
        if (destinationOrderContactMechs) {
            GenericValue destinationOrderContactMech = destinationOrderContactMechs.get(0)
            shipment.destinationContactMechId = destinationOrderContactMech.contactMechId
        } else {
            logWarning("Cannot find a shipping destination address for ${shipment.primaryOrderId}")
        }
    }
    // originContactMechId.  Only do this if it is not a purchase shipment
    if (shipment.shipmentTypeId != "PURCHASE_SHIPMENT") {
        if (!shipment.originContactMechId) {
            Map originContactMap = [contactMechPurposeTypeId: "SHIP_ORIG_LOCATION"]
            List originOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs, originContactMap)
            if (originOrderContactMechs) {
                GenericValue originOrderContactMech = originOrderContactMechs.get(0)
                shipment.originContactMechId = originOrderContactMech.contactMechId
            } else {
                logWarning("Cannot find a shipping origin address for ${shipment.primaryOrderId}")
            }
        }
    }
    // destinationTelecomNumberId
    if (!shipment.destinationTelecomNumberId) {
        Map destTelecomOrderContactMechMap = [contactMechPurposeTypeId: "PHONE_SHIPPING"]
        List destTelecomOrdercontactMechs = EntityUtil.filterByAnd(orderContactMechs, destTelecomOrderContactMechMap)
        if (destTelecomOrdercontactMechs) {
            GenericValue destTelecomOrderContactMech = destTelecomOrdercontactMechs.get(0)
            shipment.destinationTeelcomNumberId = destTelecomOrderContactMech.contactMechId
        } else {
            // use the first unexpired phone number of the shipment partyIdTo
            GenericValue phoneNumber = from("PartyAndTelecomNumber").where(partyId: shipment.partyIdTo).filterByDate().queryFirst()
            if (phoneNumber) {
                shipment.destinationTelecomNumberId = phoneNumber.contactMechId
            } else {
                logWarning("Cannot find a shipping destination phone number for ${shipment.primaryOrderId}")
            }
        }
    }
    // originTelecomNumberId
    if (!shipment.originTelecomNumberId) {
        Map originTelecomOrderContactMechMap = [contactMechPurposeTypeId: "PHONE_SHIP_ORIG"]
        List originTelecomOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs, originTelecomOrderContactMechMap)
        if (originTelecomOrderContactMechs) {
            GenericValue originTelecomOrderContactMech = originTelecomOrderContactMechs.get(0)
            shipment.originTelecomNumberId = originTelecomOrderContactMech.contactMechId
        } else {
            logWarning("Cannot find a shipping origin phone number for ${shipment.primaryOrderId}")
        }
    }
    // set the destination facility if it is a purchase order
    if (!shipment.destinationFacilityId) {
        if (shipment.shipmentTypeId == "PURCHASE_SHIPMENT") {
            Map facilityLookup = [contactMechId: shipment.destinationContactMechId]
            GenericValue destinationFacility = from("FacilityContactMech").where(facilityLookup).queryFirst()
            shipment.destinationFacilityId = destinationFacility.facilityId
        }
    }
    /*
     * NOTE: use new place to find source/destination location/addresses for new OrderItemShipGroup.contactMechId (destination address for sales orders, source address for purchase orders)
     * do this second so it will override the orderContactMech
     * TODO: maybe we should add a new entity for OrderItemShipGroup ContactMechs?
     */
    if (orderItemShipGroup) {
        if (orderHeader.orderTypeId == "SALES_ORDER") {
            shipment.destinationContactMechId = orderItemShipGroup.contactMechId
            shipment.destinationTelecomNumberId = orderItemShipGroup.telecomContactMechId
        }
    }
    if (!shipment.estimatedShipCost) {
        OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader)
        List orderItems = orderReadHelper.getValidOrderItems()
        List orderAdjustments = orderReadHelper.getAdjustments()
        List orderHeaderAdjustments = orderReadHelper.getOrderHeaderAdjustments()
        BigDecimal orderSubTotal = orderReadHelper.getOrderItemsSubTotal()

        BigDecimal shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true)
        shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true))
        //org.apache.ofbiz.base.util.Debug.log("shippingAmmount=" + shippingAmount)
        shipment.put("estimatedShipCost", shippingAmount)
    }
    // create a ShipmentRouteSegment with originFacilityId (if set on Shipment), destContactMechId,
    // and from OrderItemShipGroup shipmentMethodTypeId, carrierPartyId, etc
    Map shipmentRouteSegmentMap = [shipmentId: shipment.shipmentId]
    List shipmentRouteSegments = from("ShipmentRouteSegment").where(shipmentRouteSegmentMap).queryList()
    if (!shipmentRouteSegments) {
        // estimatedShipDate, estimatedArrivalDate
        shipmentRouteSegmentMap.estimatedstartDate = shipment.estimatedShipDate
        shipmentRouteSegmentMap.estimatedarrivalDate = shipment.estimatedArrivalDate
        shipmentRouteSegmentMap.originFacilityId = shipment.originFacilityId
        shipmentRouteSegmentMap.originContactMechId = shipment.originContactMechId
        shipmentRouteSegmentMap.originTelecomNumberId = shipment.originTelecomNumberId
        shipmentRouteSegmentMap.destFacilityId = shipment.destinationFacilityId
        shipmentRouteSegmentMap.destContactMechId = shipment.destinationContactMechId
        shipmentRouteSegmentMap.destTelecomNumberId = shipment.destinationTelecomNumberId

        orderItemShipGroup = from("OrderItemShipGroup").where(orderId: shipment.primaryOrderId, shipGroupSeqId: shipment.primaryShipGroupSeqId).queryOne()
        if (orderItemShipGroup) {
            shipmentRouteSegmentMap.carrierPartyId = orderItemShipGroup.carrierPartyId
            shipmentRouteSegmentMap.shipmentMethodTypeId = orderItemShipGroup.shipmentMethodTypeId
        }
        run service: "createShipmentRouteSegment", with: shipmentRouteSegmentMap
    }
    Map shipmentUpdateMap = [:]
    shipmentUpdateMap << shipment
    run service: "updateShipment", with: shipmentUpdateMap
    return success()
}

/**
 * Set Shipment Settings From Facilities
 * @return
 */
def setShipmentSettingsFromFacilities() {
    GenericValue facilityContactMech
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    GenericValue shipmentCopy = shipment.clone()
    List descendingFromDateOrder = ["-fromDate"]
    if (shipment?.originFacilityId) {
        if (!shipment.originContactMechId) {
            facilityContactMech = org.apache.ofbiz.party.contact.ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.get("originFacilityId"),
                    org.apache.ofbiz.base.util.UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"))
            if (facilityContactMech != null) {
                shipment.put("originContactMechId", facilityContactMech.get("contactMechId"));
            }
        }
        if (!shipment.originTelecomNumberId) {
            facilityContactMech = org.apache.ofbiz.party.contact.ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.get("originFacilityId"),
                    org.apache.ofbiz.base.util.UtilMisc.toList("PHONE_SHIP_ORIG", "PRIMARY_PHONE"))
            if (facilityContactMech != null) {
                shipment.put("originTelecomNumberId", facilityContactMech.get("contactMechId"))
            }
        }
    }
    if (shipment.destinationFacilityId) {
        if (!shipment.destinationContactMechId) {
            facilityContactMech = org.apache.ofbiz.party.contact.ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.get("destinationFacilityId"),
                    org.apache.ofbiz.base.util.UtilMisc.toList("SHIPPING_LOCATION", "PRIMARY_LOCATION"))
            if (facilityContactMech != null) {
                shipment.put("destinationContactMechId", facilityContactMech.get("contactMechId"))
            }
        }
        if (!shipment.destinationTelecomNumberId) {
            facilityContactMech = org.apache.ofbiz.party.contact.ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.get("destinationFacilityId"),
                    org.apache.ofbiz.base.util.UtilMisc.toList("PHONE_SHIPPING", "PRIMARY_PHONE"))
            if (facilityContactMech != null) {
                shipment.put("destinationTelecomNumberId", facilityContactMech.get("contactMechId"))
            }
        }
    }
    if (shipment != shipmentCopy) {
        Map shipmentUpdateMap = [:]
        shipmentUpdateMap << shipment
        run service: "updateShipment", with: shipmentUpdateMap
    }
    return success()
}

/**
 * Send Shipment Scheduled Notification
 * @return
 */
def sendShipmentScheduledNotification() {
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    // find email address for currently logged in user, set as sendFrom
    GenericValue curUserPartyAndContactMech = from("PartyAndContactMech").where(partyId: userLogin.partyId, contactMechTypeId: "EMAIL_ADDRESS").queryFirst()
    Map sendEmailMap = [sendFrom: ("," + curUserPartyAndContactMech.infoString)]

    // find email addresses of partyIdFrom, set as sendTo
    Map sendToPartyIdMap = [:]
    sendToPartyIdMap."${shipment.partyIdFrom}" = shipment.partyIdFrom
    // find email addresses of all parties not equal to partyIdFrom in SUPPLIER_AGENT roleTypeId associated with primary order, set as sendTo
    List supplierAgentOrderRoles = from("OrderRole").where(orderId: shipment.primaryOrderId, roleTypeId: "SUPPLIER_AGENT").queryList()
    for (GenericValue supplierAgentOrderRole : supplierAgentOrderRoles) {
        sendToPartyIdMap."${supplierAgentOrderRole.partyId}" = supplierAgentOrderRole.partyId
    }
    // go through all send to parties and get email addresses
    for (Map.Entry entry : sendToPartyIdMap) {
        List sendToPartyPartyAndContactMechs = from("PartyAndContactMech").where(partyId: entry.getKey(), contactMechTypeId: "EMAIL_ADDRESS").queryList()
        for (GenericValue sendToPartyPartyAndContactMech : sendToPartyPartyAndContactMechs) {
            StringBuilder newContact = new StringBuilder();
            if (sendEmailMap.sendTo) {
                newContact.append(sendEmailMap.sendTo)
            }
            newContact.append(",").append(sendToPartyPartyAndContactMech.infoString)
            sendEmailMap.sendTo = newContact.toString()
        }
    }
    // set subject, contentType, templateName, templateData
    sendEmailMap.subject = "Scheduled Notification for Shipment " + shipment.shipmentId
    if (shipment.primaryOrderId) {
        sendEmailMap.subject = sendEmailMap.subject + " for Primary Order " + shipment.primaryOrderId
    }
    sendEmailMap.contentType = "text/html"
    sendEmailMap.templateName = "component://product/template/shipment/ShipmentScheduledNotice.ftl"
    Map templateData = [shipment: shipment]
    sendEmailMap.templateData = templateData

    // call sendGenericNotificationEmail service, if enough information was found
    logInfo("Sending generic notification email (if all info is in place): ${sendEmailMap}")
    if ((sendEmailMap.sendTo) && (sendEmailMap.sendFrom)) {
        run service: "sendGenericNotificationEmail", with: sendEmailMap
    } else {
        logError("Insufficient data to send notice email: ${sendEmailMap}")
    }
    return success()
}

/**
 * Release the purchase order's items assigned to the shipment but not actually received
 * @return
 */
def balanceItemIssuancesForShipment() {
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    List issuances = delegator.getRelated("ItemIssuance", null, null, shipment, false)
    for (GenericValue issuance : issuances) {
        List receipts = from("ShipmentReceipt").where(shipmentId: shipment.shipmentId, orderId: issuance.orderId, orderItemSeqId: issuance.orderItemSeqId).queryList()
        BigDecimal issuanceQuantity = (BigDecimal) 0
        for (GenericValue receipt : receipts) {
            issuanceQuantity = issuanceQuantity + receipt.quantityAccepted + receipt.quantityRejected
        }
        issuance.quantity = issuanceQuantity
        issuance.store()
    }
    return success()
}

// ShipmentItem services

/**
 * splitShipmentItemByQuantity
 * @return
 */
def splitShipmentItemByQuantity() {
    Map result = success()
    GenericValue originalShipmentItem = from("ShipmentItem").where(parameters).queryOne()
    // create new ShipmentItem
    Map inputMap = [shipmentId: originalShipmentItem.shipmentId, productId: originalShipmentItem.productId, quantity: parameters.newItemQuantity]
    Map serviceResult = run service: "createShipmentItem", with: inputMap
    String newShipmentItemSeqId = serviceResult.shipmentItemSeqId
    // reduce the originalShipmentItem.quantity
    originalShipmentItem.quantity -= parameters.newItemQuantity
    // update the original ShipmentItem
    Map updateOriginalShipmentItemMap = [:]
    updateOriginalShipmentItemMap << originalShipmentItem
    run service: "updateShipmentItem", with: updateOriginalShipmentItemMap

    // split the OrderShipment record(s) as well for the new quantities,
    // from originalShipmentItem.shipmentItemSeqId to newShipmentItemSeqId
    List itemOrderShipmentList = from("OrderShipment").where(shipmentId: originalShipmentItem.shipmentId, shipmentItemSeqId: originalShipmentItem.shipmentItemSeqId).queryList()
    BigDecimal orderShipmentQuantityLeft = parameters.newItemQuantity
    for (GenericValue itemOrderShipment : itemOrderShipmentList) {
        if (orderShipmentQuantityLeft > (BigDecimal) 0) {
            if (itemOrderShipment.quantity > orderShipmentQuantityLeft) {
                // there is enough in this OrderShipment record, so just adjust it and move on
                Map updateOrderShipmentMap = [:]
                updateOrderShipmentMap << itemOrderShipment
                updateOrderShipmentMap.quantity = itemOrderShipment.quantity - orderShipmentQuantityLeft
                run service: "updateOrderShipment", with: updateOrderShipmentMap

                Map createOrderShipmentMap = [orderId: itemOrderShipment.orderId, orderItemSeqId: itemOrderShipment.orderItemSeqId, shipmentId: itemOrderShipment.shipmentId, shipmentItemSeqId: newShipmentItemSeqId, quantity: orderShipmentQuantityLeft]
                run service: "createOrderShipment", with: createOrderShipmentMap
                orderShipmentQuantityLeft = (BigDecimal) 0
            } else {
                // not enough on this one, create a new one for the new item and delete this one
                Map deleteOrderShipmentMap = [:]
                deleteOrderShipmentMap << itemOrderShipment
                run service: "deleteOrderShipment", with: deleteOrderShipmentMap

                Map createOrderShipmentMap = [orderId: itemOrderShipment.orderId, orderItemSeqId: itemOrderShipment.orderItemSeqId, shipmentId: itemOrderShipment.shipmentId, shipmentItemSeqId: newShipmentItemSeqId, quantity: itemOrderShipment.quantity]
                run service: "createOrderShipment", with: createOrderShipmentMap

                orderShipmentQuantityLeft -= itemOrderShipment.quantity
            }
        }
    }
    result.newShipmentItemSeqId = newShipmentItemSeqId
    return result
}

// ShipmentPackage services

/**
 * Create ShipmentPackage
 * @return
 */
def createShipmentPackage() {
    Map result = success()
    GenericValue newEntity = makeValue("ShipmentPackage", parameters)
    if ("New" == newEntity.shipmentPackageSeqId) {
        newEntity.shipmentPackageSeqId = null
    }
    // if no shipmentPackageSeqId, generate one based on existing items, ie one greater than the current higher number
    delegator.setNextSubSeqId(newEntity, "shipmentPackageSeqId", 5, 1)
    result.shipmentPackageSeqId = newEntity.shipmentPackageSeqId
    newEntity.dateCreated = UtilDateTime.nowTimestamp()
    newEntity.create()
    ensurePackageRouteSeg(newEntity.shipmentId, newEntity.shipmentPackageSeqId)
    return result
}

/**
 * Update ShipmentPackage
 * @return
 */
def updateShipmentPackage() {
    GenericValue lookedUpValue = from("ShipmentPackage").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    ensurePackageRouteSeg(lookedUpValue.shipmentId, lookedUpValue.shipmentPackageSeqId)
    return success()
}

/**
 * Delete ShipmentPackage
 * @return
 */
def deleteShipmentPackage() {
    // If there is any Shipment Package Content available for this shipment than Shipment Package cannot
    // be deleted as it require Shipment Package Content to be deleted first
    List shipmentPackageContents = from("ShipmentPackageContent").where(shipmentId: parameters.shipmentId, shipmentPackageSeqId: parameters.shipmentPackageSeqId).queryList()
    if (shipmentPackageContents) {
        String errorMessage = UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorShipmentPackageCannotBeDeleted", locale)
        logError(errorMessage)
        return error(errorMessage)
    } else {
        GenericValue lookedUpValue = from("ShipmentPackage").where(parameters).queryOne()
        lookedUpValue.remove()
    }
    return success()
}

/**
 * Ensure ShipmentPackageRouteSeg exists for all RouteSegments for this Package
 * @return
 */
def ensurePackageRouteSeg(String shipmentId, String shipmentPackageSeqId) {
    List shipmentRouteSegments = from("ShipmentRouteSegment").where(shipmentId: shipmentId).queryList()
    for (GenericValue shipmentRouteSegment : shipmentRouteSegments) {
        GenericValue checkShipmentPackageRouteSeg = from("ShipmentPackageRouteSeg").where(shipmentId: shipmentId, shipmentPackageSeqId : shipmentPackageSeqId, shipmentRouteSegmentId: shipmentRouteSegment.shipmentRouteSegmentId).queryOne()
        if (!checkShipmentPackageRouteSeg) {
            Map checkShipmentPackageRouteSegMap = [shipmentRouteSegmentId: shipmentRouteSegment.shipmentRouteSegmentId, shipmentPackageSeqId: shipmentPackageSeqId, shipmentId: shipmentId]
            run service: "createShipmentPackageRouteSeg", with: checkShipmentPackageRouteSegMap
        }
    }
    return success()
}

//  ShipmentPackageContent services

/**
 * Add Shipment Content To Package
 * @return
 */
def addShipmentContentToPackage() {
    Map result = success()
    GenericValue newEntity = makeValue("ShipmentPackageContent")
    newEntity.setPKFields(parameters)
    GenericValue shipmentPackageContent = from("ShipmentPackageContent").where(newEntity).queryOne()
    logVerbose("In addShipmentContentToPackage trying values: ${newEntity}")
    if (!shipmentPackageContent) {
        Map serviceResult = run service: "createShipmentPackageContent", with: parameters
        newEntity.shipmentPackageSeqId = serviceResult.shipmentPackageSeqId
    } else {
        // add the quantities and store it
        shipmentPackageContent.quantity += parameters.quantity
        Map updateSPCMap = [:]
        updateSPCMap << shipmentPackageContent
        run service: "updateShipmentPackageContent", with: updateSPCMap
    }
    logInfo("Shipment package: ${newEntity}")
    result.shipmentPackageSeqId = newEntity.shipmentPackageSeqId
    return result
}

// ShipmentRouteSegment services

/**
 * Ensure ShipmentPackageRouteSeg exists for all Packages for this RouteSegment
 * @return
 */
def ensureRouteSegPackage() {
    GenericValue shipmentRouteSegment = from("ShipmentRouteSegment").where(parameters).cache().queryOne()
    List shipmentPackages = from("ShipmentPackage").where(shipmentId: shipmentRouteSegment.shipmentId).queryList()
    for (GenericValue shipmentPackage : shipmentPackages) {
        GenericValue checkShipmentPackageRouteSeg = from("ShipmentPackageRouteSeg").where(shipmentId: shipmentRouteSegment.shipmentId, shipmentRouteSegmentId: shipmentRouteSegment.shipmentRouteSegmentId, shipmentPackageSeqId: shipmentPackage.shipmentPackageSeqId).queryOne()
        if (!checkShipmentPackageRouteSeg) {
            Map createShipmentPackageRouteSegMap = [shipmentId: parameters.shipmentId, shipmentRouteSegmentId: parameters.shipmentRouteSegmentId, shipmentPackageSeqId: shipmentPackage.shipmentPackageSeqId]
            run service: "createShipmentPackageRouteSeg", with: createShipmentPackageRouteSegMap
        }
    }
    return success()
}

// Check the Status of a Shipment to see if it can be changed - meant to be called in-line

/**
 * Check the Status of a Shipment to see if it can be changed - meant to be called in-line
 * @return
 */
def checkCanChangeShipmentStatusPacked() {
    parameters.fromStatusId = "SHIPMENT_PACKED"
    return checkCanChangeShipmentStatusGeneral(parameters)
}

/**
 * Check the Status of a Shipment to see if it can be changed - meant to be called in-line
 * @return
 */
def checkCanChangeShipmentStatusShipped() {
    parameters.fromStatusId = "SHIPMENT_SHIPPED"
    return checkCanChangeShipmentStatusGeneral(parameters)
}

/**
 * Check the Status of a Shipment to see if it can be changed - meant to be called in-line
 * @return
 */
def checkCanChangeShipmentStatusDelivered() {
    parameters.fromStatusId = "SHIPMENT_DEIVERED"
    return checkCanChangeShipmentStatusGeneral(parameters)
}

/**
 * Check the Status of a Shipment to see if it can be changed - meant to be called in-line
 * @return
 */
def checkCanChangeShipmentStatusGeneral(Map inputParameters) {
    Map result = success()
    String fromStatusId = inputParameters.fromStatusId
    if (!inputParameters.mainAction) {
        inputParameters.mainAction = "UPDATE"
    }
    Map serviceResult = run service: "facilityGenericPermission", with: inputParameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    Boolean hasPermission = serviceResult.hasPermission
    GenericValue testShipment = from("Shipment").where(inputParameters).cache().queryOne()
    if ((((!fromStatusId) || (fromStatusId == "SHIPMENT_PACKED")) && (testShipment.statusId == "SHIPMENT_PACKED"))
    || (((fromStatusId == "SHIPMENT_PACKED") || (fromStatusId == "SHIPMENT_SHIPPED")) && (testShipment.statusId == "SHIPMENT_SHIPPED"))
    || (((fromStatusId == "SHIPMENT_PACKED") || (fromStatusId == "SHIPMENT_SHIPPED") || (fromStatusId == "SHIPMENT_DELIVERED")) && (testShipment.statusId == "SHIPMENT_DELIVERD"))
    || (testShipment.statusId == "SHIPMENT_CANCELLED")) {
        GenericValue testShipmentStatus = delegator.getRelatedOne("StatusItem", testShipment, false)
        Map testShipmentMap = [:]
        testShipmentMap.testShipment = testShipment
        testShipmentMap.testShipmentStatus = testShipmentStatus
        String failMessage = UtilProperties.getMessage("ProductErrorUiLabels", "ShipmentCanChangeStatusPermissionError", testShipmentMap, locale)
        hasPermission = false
        result.failMessage = failMessage
    }
    result.hasPermission = hasPermission
    return result
}

// quick ship entire order in one package per facility & ship group

/**
 * Quick ships an entire order from multiple facilities
 * @return
 */
def quickShipEntireOrder() {
    Map result = success()
    List successMessageList
    List shipmentIds
    List shipmentShipGroupFacilityList
    // first get the order header; make sure we have a product store
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    if (!orderHeader?.productStoreId) {
        // no store cannot use quick ship; throw error
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentMissingProductStore", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    // get the product store entity
    GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
    if ("Y" != productStore?.reserveInventory) {
        // no reservations; no shipment; cannot use quick ship
        Map errorLog = [productStore: productStore]
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentNotCreatedForNotReserveInventory", errorLog, locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    if ("Y" == productStore.explodeOrderItems) {
        // can't insert duplicate rows in shipmentPackageContent
        Map errorLog = [productStore: productStore]
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentNotCreatedForExplodesOrderItems", errorLog, locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    // locate shipping facilities associated with order item rez's
    List orderItemShipGrpInvResFacilityIds = []
    List orderItemAndShipGrpInvResAndItemList = from("OrderItemAndShipGrpInvResAndItem").where(orderId:orderHeader.orderId, statusId: "ITEM_APPROVED").queryList()
    for (GenericValue orderItemAndShipGrpInvResAndItem : orderItemAndShipGrpInvResAndItemList) {
        if (!orderItemShipGrpInvResFacilityIds.contains(orderItemAndShipGrpInvResAndItem.facilityId)) {
            orderItemShipGrpInvResFacilityIds << orderItemAndShipGrpInvResAndItem.facilityId
        }
    }
    Map serviceResult = getOrderItemShipGroupLists(orderHeader)

    // traverse facilities, instantiate shipment for each
    for (String orderItemShipGrpInvResFacilityId : orderItemShipGrpInvResFacilityIds) {
        // sanity check for valid facility
        GenericValue facility = from("Facility").where(facilityId: orderItemShipGrpInvResFacilityId).queryOne()
        // should never be empty - referential integrity enforced
        Map serviceResultCSFFASG = createShipmentForFacilityAndShipGroup(orderHeader, serviceResult.orderItemListByShGrpMap, serviceResult.orderItemShipGroupList, serviceResult.orderItemAndShipGroupAssocList, facility, orderItemShipGrpInvResFacilityId, null, parameters.eventDate, parameters.setPackedOnly)
        successMessageList = serviceResultCSFFASG.successMessageList
        shipmentIds = serviceResultCSFFASG.shipmentIds
        shipmentShipGroupFacilityList = serviceResultCSFFASG.shipmentShipGroupFacilityList
    }
    logInfo("Finished quickShipEntireOrder:\nshipmentShipGroupFacilityList=${shipmentShipGroupFacilityList}\nsuccessMessageList=${successMessageList}")
    result.shipmentShipGroupFacilityList = shipmentShipGroupFacilityList
    result.successMessageList = successMessageList
    if (!shipmentShipGroupFacilityList) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentNotCreated", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    return result
}

/**
 * Create and complete a drop shipment for a ship group
 * @return
 */
def quickDropShipOrder() {
    Map result = success()
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    if (orderHeader?.statusId == "ORDER_CREATED") {
        String errorMessage = UtilProperties.getMessage("OrderErrorUiLabels", "OrderApproveOrderBeforeQuickDropShip", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map shipmentContext = [primaryOrderId: parameters.orderId, primaryShipGroupSeqId: parameters.shipGroupSeqId, statusId: "PURCH_SHIP_CREATED", shipmentTypeId: "DROP_SHIPMENT"]
    Map serviceResultCS = run service: "createShipment", with: shipmentContext
    if (!ServiceUtil.isSuccess(serviceResultCS)) {
        return serviceResultCS
    }
    String shipmentId = serviceResultCS.shipmentId
    Map updateShipmentContext = [shipmentId: shipmentId, statusId: "PURCH_SHIP_SHIPPED"]
    Map serviceResult = run service: "updateShipment", with: updateShipmentContext
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    updateShipmentContext.statusId = "PURCH_SHIP_RECEIVED"
    serviceResult = run service: "updateShipment", with: updateShipmentContext
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    result.shipmentId = shipmentId
    // Iterate through the order items in the ship group
    List orderItemShipGroupAssocs = from("OrderItemShipGroupAssoc").where(orderId: parameters.orderId, shipGroupSeqId: parameters.shipGroupSeqId).queryList()
    for (GenericValue orderItemShipGroupAssoc : orderItemShipGroupAssocs) {
        GenericValue orderItem = delegator.getRelatedOne("OrderItem", orderItemShipGroupAssoc, false)
        // Set the item status to completed
        Map itemStatusContext = [orderId: parameters.orderId, orderItemSeqId: orderItem.orderItemSeqId, statusId: "ITEM_COMPLETED"]
        Map resultCOIS = run service: "changeOrderItemStatus", with: itemStatusContext
        if (!ServiceUtil.isSuccess(resultCOIS)) {
            return resultCOIS
        }
        // Set the status of any linked sales order items to completed as well
        List orderItemAssocs = from("OrderItemAssoc").where(toOrderId: parameters.orderId, toOrderItemSeqId: orderItem.orderItemSeqId, orderItemAssocTypeId: "DROP_SHIPMENT").queryList()
        if (orderItemAssocs) {
            for (GenericValue orderItemAssoc : orderItemAssocs) {
                itemStatusContext.orderId = orderItemAssoc.orderId
                itemStatusContext.orderItemSeqId = orderItemAssoc.orderItemSeqId
                Map serviceResultCOIS = run service: "changeOrderItemStatus", with: itemStatusContext
                if (!ServiceUtil.isSuccess(serviceResultCOIS)) {
                    return serviceResultCOIS
                }
            }
        }
    }
    return result
}

/**
 * Quick receives an entire purchase order in a facility
 * @return
 */
def quickReceivePurchaseOrder() {
    Map result = success()
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    GenericValue facility = from("Facility").where(parameters).queryOne()
    Map serviceResult = getOrderItemShipGroupLists(orderHeader)
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    Map serviceResultCSFFASG = createShipmentForFacilityAndShipGroup(orderHeader, serviceResult.orderItemListByShGrpMap, serviceResult.orderItemShipGroupList, serviceResult.orderItemAndShipGroupAssocList, null, null, parameters.facilityId, null, null)
    logInfo("Finished quickReceivePurchaseOrder for orderId ${parameters.orderId} and destination facilityId ${parameters.facilityId} shipment created ${serviceResultCSFFASG.shipmentIds}")
    result.shipmentIds = serviceResultCSFFASG.shipmentIds
    return result
}

/**
 * Sub-method used by quickShip methods to get a list of OrderItemAndShipGroupAssoc and a Map of shipGroupId -> OrderItemAndShipGroupAssoc
 * @return
 */
def getOrderItemShipGroupLists(GenericValue orderHeader) {
    Map result = success()
    // lookup all the approved items, doing by item because the item must be approved before shipping
    List orderItemAndShipGroupAssocList = from("OrderItemAndShipGroupAssoc").where(orderId: orderHeader.orderId, statusId: "ITEM_APPROVED").queryList()
    // make sure we have something to ship
    if (!orderItemAndShipGroupAssocList) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoItemsAvailableToShip", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    List orderItemShipGroupList = delegator.getRelated("OrderItemShipGroup", null, null, orderHeader, false)
    // group orderItems (actually OrderItemAndShipGroupAssocs) by shipGroupSeqId in a Map with List values
    // This Map is actually used only for sales orders' shipments right now.
    List orderItemListByShGrpMap = []
    for (GenericValue orderItemAndShipGroupAssoc : orderItemAndShipGroupAssocList) {
        orderItemListByShGrpMap << orderItemAndShipGroupAssoc
    }
    result.orderItemListByShGrpMap = orderItemListByShGrpMap
    result.orderItemAndShipGroupAssocList = orderItemAndShipGroupAssocList
    result.orderItemShipGroupList = orderItemShipGroupList
    return result
}

/**
 * Sub-method used by quickShip methods to create a shipment
 * @return
 */
def createShipmentForFacilityAndShipGroup(GenericValue orderHeader, List orderItemListByShGrpMap, List orderItemShipGroupList, List orderItemAndShipGroupAssocList, GenericValue facility, String orderItemShipGrpInvResFacilityId, String facilityId, Timestamp eventDate, Boolean setPackedOnly) {
    Map result = success()
    List shipmentIds = []
    List shipmentShipGroupFacilityList = []
    List successMessageList = []
    String partyIdFrom
    Map packedContext
    // for OrderItemShipGroup need to split all OISGIRs into their ship groups and create a shipment for each
    for (GenericValue orderItemShipGroup : orderItemShipGroupList) {
        // lookup all the approved items
        List orderItems = from("OrderItemAndShipGroupAssoc").where(orderId: orderHeader.orderId, shipGroupSeqId: orderItemShipGroup.shipGroupSeqId, statusId: "ITEM_APPROVED").queryList()
        List perShipGroupItemList = orderItemListByShGrpMap
        // make sure we have something to ship
        if (!perShipGroupItemList) {
            List argListNames = [orderItemShipGroup.shipGroupSeqId]
            String successMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentNoItemsAvailableToShip", argListNames, locale)
        } else {
            // create the shipment for this facility and ship group combination
            Map shipmentContext = [primaryOrderId: orderHeader.orderId, primaryShipGroupSeqId: orderItemShipGroup.shipGroupSeqId]
            // for Sales Shipment, order items' reservation facilityId is the originFacilityId, and the initial status is "INPUT"
            // for Purchase Shipment, the facilityId parameter is the destinationFacilityId, and the initial status is "CREATED"
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                if (orderItemShipGroup.vendorPartyId) {
                    partyIdFrom = orderItemShipGroup.vendorPartyId
                } else {
                    facility = from("Facility").where(facilityId: orderItemShipGrpInvResFacilityId).queryOne()
                    if (facility?.ownerPartyId) {
                        partyIdFrom = facility.ownerPartyId
                    }
                    if (!partyIdFrom) {
                        List orderRoles = from("OrderRole").where(orderId: orderHeader.orderId, roleTypeId: "SHIP_FROM_VENDOR").queryList()
                        if (orderRoles) {
                            GenericValue orderRole = orderRoles.get(0)
                            partyIdFrom = orderRole.partyId
                        } else {
                            GenericValue orderRole = from("OrderRole").where(orderId: orderHeader.orderId, roleTypeId: "BILL_FROM_VENDOR").queryFirst()
                            partyIdFrom = orderRole.partyId
                        }
                    }
                }
                shipmentContext.partyIdFrom = partyIdFrom
                shipmentContext.originFacilityId = orderItemShipGrpInvResFacilityId
                shipmentContext.statusId = "SHIPMENT_INPUT"
            } else {
                shipmentContext.destinationFacilityId = facility.facilityId
                shipmentContext.statusId = "PRUCH_SHIP_CREATED"
            }
            Map serviceResult = run service: "createShipment", with: shipmentContext
            Map shipmentLookupMap = [shipmentId: serviceResult.shipmentId]
            GenericValue shipment = from("Shipment").where(shipmentLookupMap).queryOne()
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                for (GenericValue orderItemAndShipGroupAssoc : perShipGroupItemList) {
                    // just get the OrderItemShipGrpInvResAndItem records for this facility and this ship group, since that is what this shipment is for
                    Map itemResFindMap = [facilityId: orderItemShipGrpInvResFacilityId]
                    List itemResList = delegator.getRelated("OrderItemShipGrpInvResAndItem", null, null, orderItemAndShipGroupAssoc, false)
                    for (GenericValue itemRes : itemResList) {
                        Map issueContext = [shipmentId: shipment.shipmentId, orderId: itemRes.orderId, orderItemSeqId: itemRes.orderItemSeqId, shipGroupSeqId: itemRes.shipGroupSeqId, inventoryItemId: itemRes.inventoryItemId, quantity: itemRes.quantity, eventDate: eventDate]
                        run service: "issueOrderItemShipGrpInvResToShipment", with: issueContext
                    }
                }
            } else { // Issue all purchase order items
                Map itemResFindMap = [facilityId: facilityId]
                for (GenericValue item : orderItemAndShipGroupAssocList) {
                    Map issueContext = [shipmentId: shipment.shipmentId, orderId: item.orderId, orderItemSeqId: item.orderItemSeqId, shipGroupSeqId: item.shipgroupSeqId, quantity: item.quantity]
                    run service: "issueOrderItemToShipment", with: issueContext
                }
            }
            // place all issued items into a single package
            List itemIssuances = from("ItemIssuance").where(orderId: orderHeader.orderId, shipGroupSeqId: orderItemShipGroup.shipGroupSeqId, shipmentId: shipment.shipmentId).queryList()
            String shipmentPackageSeqId = "New"
            for (GenericValue itemIssuance: itemIssuances) {
                logVerbose("In quick ship adding item to package: ${shipmentPackageSeqId}")
                Map shipItemContext = [shipmentId: itemIssuance.shipmentId, shipmentItemSeqId: itemIssuance.shipmentItemSeqId, quantity: itemIssuance.quantity, shipmentPackageSeqId: shipmentPackageSeqId]
                Map serviceResultASCTP = run service: "addShipmentContentToPackage", with: shipItemContext
                shipmentPackageSeqId = serviceResultASCTP.shipmentPackageSeqId
            }
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                // update the shipment status to packed
                packedContext = [shipmentId: shipment.shipmentId, eventDate: eventDate, statusId: "SHIPMENT_PACKED"]
                run service: "updateShipment", with: packedContext
                // update the shipment status to shipped (if setPackedOnly has NOT been set)
                if (!setPackedOnly) {
                    packedContext.shipmentId = shipment.shipmentId
                    packedContext.statusId = "SHIPMENT_SHIPPED"
                    run service: "updateShipment", with: packedContext
                }
            } else { // PURCHASE_ORDER
                // update the shipment status to shipped
                packedContext.shipmentId = shipment.shipmentId
                packedContext.statusId = "PURCH_SHIP_SHIPPED"
                run service: "updateShipment", with: packedContext
            }
            Map shipmentShipGroupFacility = [shipmentId: shipment.shipmentId, facilityId: facility.facilityId, shipGroupSeqId: orderItemShipGroup.shipGroupSeqId]
            shipmentShipGroupFacilityList << shipmentShipGroupFacility
            List argListNames = []
            argListNames << shipmentShipGroupFacility.shipmentId
            argListNames << shipmentShipGroupFacility.shipGroupSeqId
            argListNames << shipmentShipGroupFacility.facilityId
            successMessageList.add(UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentIdCreated", argListNames, locale))
            shipmentIds << shipment.shipmentId
        }
    }
    result.shipmentIds = shipmentIds
    result.successMessageList = successMessageList
    result.shipmentShipGroupFacilityList = shipmentShipGroupFacilityList
    return result
}

/**
 * Create Shipment, ShipmentItems and OrderShipment
 * @return
 */
def createOrderShipmentPlan () {
    Map result = success()
    // first get the order header; make sure we have a product store
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    if (!orderHeader?.productStoreId) {
        // no store cannot use quick ship; throw error
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoQuickShip", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    // get the product store entity
    GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
    List orderItemShipGroupList = delegator.getRelated("OrderItemShipGroup", null, null, orderHeader, false)
    for (GenericValue orderItemShipGroup : orderItemShipGroupList) {
        Map createShipmentContext = [primaryOrderId: orderHeader.orderId, primaryShipGroupSeqId: orderItemShipGroup.shipGroupSeqId, statusId: "SHIPMENT_INPUT", originFacilityId: productStore.inventoryFacilityId, userLogin: parameters.userLogin]
        Map serviceResult = run service: "createShipment", with: createShipmentContext
        parameters.shipmentId = serviceResult.shipmentId
        GenericValue shipment = from("Shipment").where(parameters).queryOne()
        List orderItems = delegator.getRelated("OrderItem", null, null, orderHeader, false)
        for (GenericValue orderItem : orderItems) {
            GenericValue itemProduct = from("Product").where(productId: orderItem.productId).cache().queryOne()
            // make sure the OrderItem is for a Product that has a ProductType with isPhysical=Y
            if (itemProduct) {
                GenericValue itemProductType = delegator.getRelatedOne("ProductType", itemProduct, true)
                if (itemProductType.isPhysical == "Y") {
                    // Create shipment item
                    Map addOrderShipemtToShipmentCtx = [orderId: orderHeader.orderId, orderItemSeqId: orderItem.orderItemSeqId, shipmentId: parameters.shipmentId, shipGroupSeqId: orderItemShipGroup.shipGroupSeqId, quantity: orderItem.quantity, userLogin: parameters.userLogin]
                    run service: "addOrderShipmentToShipment", with: addOrderShipemtToShipmentCtx
                }
            }
        }
        result.shipmentId = parameters.shipmentId
    }
    return result
}

/**
 * 
 * @return
 */
def issueSerializedInvToShipmentPackageAndSetTracking() {
    /*
     *  If serialNumber is provided, Then compare it with the serialNumber of inventoryItem on reservation. If they don't match,
     *  We'll have to reReserve specific inventory that is shiped.
     *  If serialNumber exist then run reserveAnInventoryItem for serialisedInventory.There is no need to check
     *  this condition for the non-serialized inventory (Non serialized Inventory will directly issued).
     */
    if (parameters.serialNumber) {
        GenericValue orderItemShipGrpInvRes = from("OrderItemShipGrpInvRes").where(parameters).queryOne()
        GenericValue inventoryItem = delegator.getRelatedOne("InventoryItem", orderItemShipGrpInvRes, false)
        if (inventoryItem.serialNumber != parameters.serialNumber) {
            // The inventory that we have reserved is not what we shipped. Lets reReserve, this time we'll get what we want
            Map serviceResult = run service: "reserveAnInventoryItem", with: parameters
            parameters.inventoryItemId = serviceResult.inventoryItemId
        }
    }
    // get InventoryItem issued to shipment
    Map issueContext = [shipmentId: parameters.shipmentId, inventoryItemId: parameters.inventoryItemId, orderId: parameters.orderId, shipGroupSeqId: parameters.shipGroupSeqId, orderItemSeqId: parameters.orderItemSeqId, inventoryItemId: parameters.inventoryItemId, quantity: parameters.quantity]
    Map serviceResultIO = run service: "issueOrderItemShipGrpInvResToShipment", with: issueContext
    parameters.itemIssuanceId = serviceResultIO.itemIssuanceId
    // place all issued items into a package for tracking num
    logInfo("QuickShipOrderByItem grouping by tracking number : ${parameters.trackingNum}")
    GenericValue itemIssuance = from("ItemIssuance").where(parameters).queryOne()
    Map shipItemContext = [shipmentPackageSeqId: parameters.shipmentPackageSeqId]
    if (!shipItemContext.shipmentPackageSeqId) {
        shipItemContext.shipmentPackageSeqId = "New"
    }
    logInfo("Package SeqID : ${shipItemContext.shipmentPackageSeqId}")
    GenericValue shipmentPackage = from("ShipmentPackge").where(parameters).queryOne()
    if (!shipmentPackage) {
        Map shipPackageContext = [shipmentId: itemIssuance.shipmentId, shipmentPackageSeqId: shipItemContext.shipmentPackageSeqId]
        run service: "createShipmentPackage", with: shipPackageContext
    }
    shipItemContext.shipmentId = itemIssuance.shipmentId
    shipItemContext.shipmentItemSeqId = itemIssuance.shipmentItemSeqId
    shipItemContext.quantity = itemIssuance.quantity
    Map serviceResultASCTP = run service: "addShipmentContentToPackage", with: shipItemContext
    Map packageMap = ["${parameters.trackingNum}": serviceResultASCTP.shipmentPackageSeqId]
    Map routeSegLookup = [shipmentPackageSeqId: serviceResultASCTP.shipmentPackageSeqId]
    if (routeSegLookup.shipmentPackageSeqId) {
        routeSegLookup.shipmentId = itemIssuance.shipmentId
        // quick ship orders should only have one route segment
        routeSegLookup.shipmentRouteSegmentId = "0001"
        GenericValue packageRouteSegment = from("ShipmentPackageRouteSeg").where(routeSegLookup).queryOne()
        if (packageRouteSegment) {
            packageRouteSegment.trackingCode = parameters.trackingNum
            packageRouteSegment.store()
        } else {
            logWarning("No route segment found : ${routeSegLookup}")
        }
    }
    if (!routeSegLookup.shipmentPackageSeqId) {
        logWarning("No shipment package ID found; cannot update RouteSegment")
    }
    return success()
}

/**
 * Move a shipment into Packed status and then to Shipped status
 * @return
 */
def setShipmentStatusPackedAndShipped() {
    // update the shipment status to packed
    Map packedContext = [shipmentId: parameters.shipmentId, statusId: "SHIPMENT_PACKED"]
    run service: "updateShipment", with: packedContext
    // update the shipment status to shipped
    if (!parameters.setPackedOnly) {
        packedContext.shipmentId = parameters.shipmentId
        packedContext.statusId = "SHIPMENT_SHIPPED"
        run service: "updateShipment", with: packedContext
    }
    return success()
}

/**
 * Quick ships order based on item list
 * @return
 */
def quickShipOrderByItem() {
    /*
     *  quick ship order using multiple packages per tracking number
     *  Parameters coming in: orderId, shipGroupSeqId,itemShipList, originFacilityId, setPackedOnly
     *  The input list contains Maps with four keys: orderItemSeqId, inventoryItemId, qtyShipped, trackingNum
     *  Parameters going out: shipmentId
     */
    Map result = success()
    // first get the order header; make sure we have a product store
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    if (!parameters.originFacilityId) {
        if (!orderHeader?.productStoreId) {
            // no store cannot use quick ship; throw error
            String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoQuickShip", locale)
            logError(errorMessage)
            return error(errorMessage)
        } else {
            // get the product store entity
            GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
            if (productStore.reserveInventory != "Y") {
                // no reservations; no shipment; cannot use quick ship
                String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoQuickShipForNotReserveInventory", locale)
                logError(errorMessage)
                return error(errorMessage)
            }
            if (productStore.oneInventoryFacility != "Y") {
                // if we allow multiple facilities we cannot use quick ship; throw error
                String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoQuickShipForMultipleFacilities", locale)
                logError(errorMessage)
                return error(errorMessage)
            }
            if (!productStore.inventoryFacilityId) {
                String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoQuickShipForNotInventoryFacility", locale)
                logError(errorMessage)
                return error(errorMessage)
            }
        }
    }
    // make sure we have items to issue
    if (!parameters.itemShipList) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoItemsAvailableToShip", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    // move the itemMap to the envrironment
    List itemMapList = parameters.itemShipList
    // we are all good to go; create the shipment
    Map shipmentContext = [:]
    if (parameters.originFacilityId) {
        shipmentContext.originFacilityId = parameters.originFacilityId
    }
    shipmentContext.primaryOrderId = parameters.orderId
    shipmentContext.primaryShipGroupSeqId = parameters.shipGroupSeqId
    shipmentContext.statusId = "SHIPMENT_INPUT"
    Map serviceResult = run service: "createShipment", with: shipmentContext
    Map shipmentLookupMap = [shipmentId: serviceResult.shipmentId]
    GenericValue shipment = from("Shipment").where(shipmentLookupMap).queryOne()
    // issue the passed in order items
    logVerbose("ShipMap List : ${itemMapList}  /  ${parameters.itemShipList}")
    for (Map itemMap : itemMapList) {
        logVerbose("Item Map : ${itemMap}")
        Map issueContext = [shipmentId: shipment.shipmentId, orderId: parameters.orderId, shipGroupSeqId: parameters.shipGroupSeqId, orderItemSeqId: itemMap.orderItemSeqId, inventoryItemId: itemMap.inventoryItemId, quantity: itemMap.qtyShipped]
        Map serviceResultIO = run service: "issueOrderItemShipGrpInvResToShipment", with: issueContext
        itemMap.itemIssuanceId = serviceResultIO.itemIssuanceId
    }
    // place all issued items into a unique package per tracking num
    Map packageMap = [:]
    for (Map itemMap : itemMapList) {
        logInfo("QuickShipOrderByItem grouping by tracking number : ${itemMap.trackingNum}")
        GenericValue itemIssuance = from("ItemIssuance").where(itemIssuanceId: itemMap.itemIssuanceId).queryOne()
        Map shipItemContext = [shipmentPackageSeqId: packageMap.'${itemMap.trackingNum}']
        if (!shipItemContext.shipmentPackageSeqId) {
            shipItemContext.shipmentPackageSeqId = "New"
        }
        logInfo("Package SeqID : ${shipItemContext.shipmentPackageSeqId}")
        shipItemContext.shipmentId = itemIssuance.shipmentId
        shipItemContext.shipmentItemSeqId = itemIssuance.shipmentItemSeqId
        shipItemContext.quantity = itemIssuance.quantity
        Map serviceResultASCTP = run service: "addShipmentContentToPackage", with: shipItemContext
        packageMap.'${itemMap.trackingNum}' = serviceResultASCTP.shipmentPackageSeqId
        Map routeSegLookup = [shipmentPackageSeqId: serviceResultASCTP.shipmentPackageSeqId]

        if (routeSegLookup.shipmentPackageSeqId) {
            routeSegLookup.shipmentId = itemIssuance.shipmentId
            // quick ship orders should only have one route segment
            routeSegLookup.shipmentRouteSegmentId = "00001"
            GenericValue packageRouteSegment = from("ShipmentPackageRouteSeg").where(routeSegLookup).queryOne()
            if (packageRouteSegment) {
                packageRouteSegment.trackingCode = itemMap.trackingNum
                packageRouteSegment.store()
            } else {
                logWarning("No route segment found : ${routeSegLookup}")
            }
        }
        if (!routeSegLookup.shipmentPackageSeqId) {
            logWarning("No shipment package ID found; cannot update RouteSegment")
        }
    }
    // update the shipment status to packed
    Map packedContext = [shipmentId: shipment.shipmentId, statusId: "SHIPMENT_PACKED"]
    run service: "updateShipment", with: packedContext
    // update the shipment status to shipped
    if (!parameters.setPackedOnly) {
        packedContext.shipmentId = shipment.shipmentId
        packedContext.statusId = "SHIPMENT_SHIPPED"
        run service: "updateShipment", with: packedContext
    }
    result.shipmentId = shipment.shipmentId
    return result
}

/**
 * Delete an OrderShipment and updates the ShipmentItem
 * @return
 */
def removeOrderShipmentFromShipment() {
    GenericValue orderShipment = from("OrderShipment").where(parameters).queryOne()
    GenericValue shipmentItem = from("ShipmentItem").where(parameters).queryOne()
    Map inMap = [userLogin: parameters.userLogin, shipmentId: parameters.shipmentId, shipmentItemSeqId: parameters.shipmentItemSeqId, orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, shipGroupSeqId: parameters.shipGroupSeqId]
    run service: "deleteOrderShipment", with: inMap
    shipmentItem.quantity = orderShipment.quantity - shipmentItem.quantity
    inMap = [:]
    if (shipmentItem.quantity > (BigDecimal) 0) {
        inMap.userLogin = parameters.userLogin
        inMap.shipmentId = parameters.shipmentId
        inMap.shipmentItemSeqId = parameters.shipmentItemSeqId
        inMap.quantity = shipmentItem.quantity
        run service: "updateShipmentItem", with: inMap
    } else {
        inMap.userLogin = parameters.userLogin
        inMap.shipmentId = parameters.shipmentId
        inMap.shipmentItemSeqId = parameters.shipmentItemSeqId
        run service: "deleteShipmentItem", with: inMap
    }
    return success()
}

// for a given order item and quantity it creates (or updates if already exists) an entry in the ShipmentPlan.

/**
 * Add or update a ShipmentPlan entry
 * @return
 */
def addOrderShipmentToShipment() {
    Map result = success()
    // if quantity is greater than 0 we add or update the ShipmentPlan
    if (parameters.quantity > (BigDecimal) 0) {
        // get orderHeader
        GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
        // get orderItem
        GenericValue orderItem = from("OrderItem").where(parameters).queryOne()
        // make sure the orderItem is not already present in this shipment
        Map lookupMap = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, shipGroupSeqId: parameters.shipGroupSeqId, shipmentId: parameters.shipmentId]
        List existingOrderShipments = from("OrderShipment").where(lookupMap).queryList()
        if (existingOrderShipments) {
            return error("Not adding Order Item to plan for shipment [${parameters.shipmentId}] because the order item is already in the shipment (order [${parameters.orderId}], order item [${parameters.orderItemSeqId}])")
        }
        Map inputMap = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId]
        Map serviceResult = run service: "getQuantityForShipment", with: inputMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        BigDecimal remainingQuantity = serviceResult.remainingQuantity

        if (parameters.quantity > remainingQuantity) {
            return error("Not adding Order Item to plan for shipment [${parameters.shipmentId}] because the quantity is greater than the remaining quantity (order [${parameters.orderId}], order item [${parameters.orderItemSeqId}])")
        }
        inputMap = [:]
        inputMap.userLogin = parameters.userLogin
        inputMap.shipmentId = parameters.shipmentId
        inputMap.productId = orderItem.productId
        inputMap.quantity = parameters.quantity
        Map serviceResultCSI = run service: "createShipmentItem", with: inputMap
        parameters.shipmentItemSeqId = serviceResultCSI.shipemntItemSeqId
        result.shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
        inputMap = [:]
        inputMap.userLogin = parameters.userLogin
        inputMap.shipmentId = parameters.shipmentId
        inputMap.shipmentItemSeqId = parameters.shipmentItemSeqId
        inputMap.orderId = parameters.orderId
        inputMap.orderItemSeqId = parameters.orderItemSeqId
        inputMap.quantity = parameters.quantity
        run service: "createOrderShipment", with: parameters
    }
    return result
}

/**
 * get the order item quantity still not put in shipments
 * @return
 */
def getQuantityForShipment() {
    Map result = success()
    BigDecimal plannedQuantity = (BigDecimal) 0
    BigDecimal issuedQuantity = (BigDecimal) 0
    // get orderItem
    GenericValue orderItem = from("OrderItem").where(parameters).queryOne()
    Map orderShipmentLookup = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId]
    List existingOrderShipments = from("OrderShipment").where(orderShipmentLookup).queryList()
    for (GenericValue orderShipment : existingOrderShipments) {
        if (!orderShipment.quantity) {
            orderShipment.quantity = (BigDecimal) 0
        }
        plannedQuantity += orderShipment.quantity
    }
    existingOrderShipments = from("ItemIssuance").where(orderShipmentLookup).queryList()
    for (GenericValue itemIssuance : existingOrderShipments) {
        if (!itemIssuance.quantity) {
            itemIssuance.quantity = (BigDecimal) 0
        }
        if (!itemIssuance.cancelQuantity) {
            itemIssuance.cancelQuantity = (BigDecimal) 0
        }
        issuedQuantity = issuedQuantity + itemIssuance.quantity - itemIssuance.cancelQuantity
    }
    BigDecimal totPlannedOrIssuedQuantity = issuedQuantity + plannedQuantity
    if (!orderItem?.cancelQuantity) {
        orderItem.cancelQuantity = (BigDecimal) 0
    }
    BigDecimal remainingQuantity = orderItem.cancelQuantity + totPlannedOrIssuedQuantity - orderItem.quantity
    result.remainingQuantity = remainingQuantity
    return result
}

/**
 * Check Shipment Items and Cancel Item Issuance and Order Shipment
 * @return
 */
def checkCancelItemIssuanceAndOrderShipmentFromShipment() {
    List orderShipmentList = from("OrderShipment").where(shipmentId: parameters.shipmentId).queryList()
    for (GenericValue orderShipment : orderShipmentList) {
        Map deleteOrderShipmentMap = [:]
        deleteOrderShipmentMap << orderShipment
        run service: "deleteOrderShipment", with: deleteOrderShipmentMap
    }
    logInfo("Cancelling Item Issuances for shimpentId: ${parameters.shipmentId}")
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    List issuances = delegator.getRelated("ItemIssuance", null, null, shipment, false)
    for (GenericValue issuance : issuances) {
        Map inputMap = [itemIssuanceId: issuance.itemIssuanceId]
        run service: "cancelOrderItemIssuanceFromSalesShipment", with: inputMap
    }
    return success()
}
