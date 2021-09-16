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
import org.apache.ofbiz.party.contact.ContactMechWorker
import org.apache.ofbiz.product.product.ProductWorker
import org.apache.ofbiz.service.ServiceUtil

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
            GenericValue checkStatusValidChange = from("StatusValidChange")
                    .where(statusId: lookedUpValue.statusId,
                            statusIdTo: parameters.statusId)
                    .cache()
                    .queryOne()
            if (!checkStatusValidChange) {
                errorList << "ERROR: Changing the status from ${lookedUpValue.statusId}" +
                        " to ${parameters.statusId} is not allowed."
            }
            Map createShipmentStatusMap = [shipmentId: parameters.shipmentId,
                                           statusId: parameters.statusId]
            if (parameters.eventDate) {
                createShipmentStatusMap.statusDate = parameters.eventDate
            }
            run service: "createShipmentStatus", with: createShipmentStatusMap
        }
    }
    // now finally check for errors
    if (errorList) {
        return error(errorList.toString())
    }
    Map serviceResult = run service: "checkAndUpdateWorkEffort", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)

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
    GenericValue returnHeader = from("ReturnHeader").where(returnId: parameters.returnId).queryOne()
    Map shipmentCtx = [partyIdFrom: returnHeader.fromPartyId,
                       partyIdTo: returnHeader.toPartyId,
                       originContactMechId: returnHeader.originContactMechId,
                       destinationFacilityId: returnHeader.destinationFacilityId,
                       primaryReturnId: returnHeader.returnId]
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
    Map result = success()
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
        GenericValue product = returnItem.getRelatedOne("Product", false)
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
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        String shipmentId = serviceResult.shipmentId
        logInfo("Created new shipment ${shipmentId}")

        for (GenericValue returnItem : returnItems) {
            // Shipment items are created only for physical products
            GenericValue product = returnItem.getRelatedOne("Product", false)
            ProductWorker productWorker = new ProductWorker()

            if (productWorker.isPhysical(product)) {
                Map serviceResultCSI = run service: "createShipmentItem", with: [shipmentId: shipmentId,
                                                                                 productId: returnItem.productId,
                                                                                 quantity: returnItem.returnQuantity]
                String shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
                shipItemCtx = [shipmentId: shipmentId,
                               shipmentItemSeqId: shipmentItemSeqId,
                               returnId: returnItem.returnId,
                               returnItemSeqId: returnItem.returnItemSeqId,
                               quantity: returnItem.returnQuantity]
                run service: "createReturnItemShipment", with: [shipmentId: shipmentId,
                                                                shipmentItemSeqId: shipmentItemSeqId,
                                                                returnId: returnItem.returnId,
                                                                returnItemSeqId: returnItem.returnItemSeqId,
                                                                quantity: returnItem.returnQuantity]
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
    Map serviceResult = run service: "createShipment", with: parameters
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    String shipmentId = serviceResult.shipmentId
    logInfo("Created new shipment ${shipmentId}")
    List returnItems = from("ReturnItem").where(returnId: parameters.primaryReturnId).queryList()
    for (GenericValue returnItem : returnItems) {
        Map serviceResultCSI = run service: "createShipmentItem", with: [shipmentId: shipmentId,
                                                                         productId: returnItem.productId,
                                                                         quantity: returnItem.returnQuantity]
        String shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
        run service: "createReturnItemShipment", with: [shipmentId: shipmentId,
                                                        shipmentItemSeqId: shipmentItemSeqId,
                                                        returnId: returnItem.returnId,
                                                        returnItemSeqId: returnItem.returnItemSeqId,
                                                        quantity: returnItem.returnQuantity]
    }
    Map result = success()
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
        logInfo("Not running setShipmentSettingsFromPrimaryOrder," +
                " primaryOrderId is empty for shipmentId [${shipment.shipmentId}]")
        return success()
    }
    // TODO: we may not want to check this if, for example, Purchase Orders don't have any OrderItemShipGroups
    if (!shipment.primaryShipGroupSeqId) {
        // No primaryShipGroupSeqId specified, don't do anything
        logInfo("Not running setShipmentSettingsFromPrimaryOrder," +
                " primaryShipGroupSeqId is empty for shipmentId [${parameters.shipmentId}]")
        return success()
    }
    GenericValue orderHeader = from("OrderHeader").where(orderId: shipment.primaryOrderId).queryOne()
    if (shipment.primaryShipGroupSeqId) {
        orderItemShipGroup = from("OrderItemShipGroup")
                .where(orderId: shipment.primaryOrderId,
                        shipGroupSeqId: shipment.primaryShipGroupSeqId)
                .queryOne()
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
    if (!shipment.originFacilityId
            && shipment.shipmentTypeId == "SALES_SHIPMENT"
            && orderHeader.productStoreId) {
        GenericValue productStore = from("ProductStore")
                .where(productStoreId: orderHeader.productStoreId)
                .cache()
                .queryOne()
        if (productStore.oneInventoryFacility == "Y") {
            shipment.originFacilityId = productStore.inventoryFacilityId
        }
    }
    // partyIdFrom, partyIdTo (vendorPartyId) - NOTE: these work the same for Purchase and Sales Orders...
    List orderRoles = from("OrderRole").where(orderId: shipment.primaryOrderId).queryList()
    List limitOrderRoles = []
    // From: SHIP_FROM_VENDOR
    if (!shipment.partyIdFrom) {
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, [roleTypeId: "SHIP_FROM_VENDOR"])
        if (limitOrderRoles) {
            shipment.partyIdFrom = limitOrderRoles[0].partyId
        }
    }
    // From: VENDOR
    if (!shipment.partyIdFrom) {
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, [roleTypeId: "VENDOR"])
        if (limitOrderRoles) {
            shipment.partyIdFrom = limitOrderRoles[0].partyId
        }
    }
    // To: SHIP_TO_CUSTOMER
    if (!shipment.partyIdTo) {
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, [roleTypeId: "SHIP_TO_CUSTOMER"])
        if (limitOrderRoles) {
            shipment.partyIdTo = limitOrderRoles[0].partyId
        }
    }
    // To: CUSTOMER
    if (!shipment.partyIdTo) {
        limitOrderRoles = EntityUtil.filterByAnd(orderRoles, [roleTypeId: "CUSTOMER"])
        if (limitOrderRoles) {
            shipment.partyIdTo = limitOrderRoles[0].partyId
        }
    }
    List orderContactMechs = from("OrderContactMech").where(orderId: shipment.primaryOrderId).queryList()

    // destinationContactMechId
    if (!shipment.destinationContactMechId) {
        // first try from orderContactMechs
        List destinationOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs,
                [contactMechPurposeTypeId: "SHIPPING_LOCATION"])
        if (destinationOrderContactMechs) {
            shipment.destinationContactMechId = destinationOrderContactMechs[0].contactMechId
        } else {
            logWarning("Cannot find a shipping destination address for ${shipment.primaryOrderId}")
        }
    }

    // originContactMechId.  Only do this if it is not a purchase shipment
    if (shipment.shipmentTypeId != "PURCHASE_SHIPMENT") {
        if (!shipment.originContactMechId) {
            List originOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs,
                    [contactMechPurposeTypeId: "SHIP_ORIG_LOCATION"])
            if (originOrderContactMechs) {
                shipment.originContactMechId = originOrderContactMechs[0].contactMechId
            } else {
                logWarning("Cannot find a shipping origin address for ${shipment.primaryOrderId}")
            }
        }
    }
    // destinationTelecomNumberId
    if (!shipment.destinationTelecomNumberId) {
        List destTelecomOrdercontactMechs = EntityUtil.filterByAnd(orderContactMechs,
                [contactMechPurposeTypeId: "PHONE_SHIPPING"])
        if (destTelecomOrdercontactMechs) {
            shipment.destinationTelecomNumberId = destTelecomOrdercontactMechs[0].contactMechId
        } else {
            // use the first unexpired phone number of the shipment partyIdTo
            GenericValue phoneNumber = from("PartyAndTelecomNumber")
                    .where(partyId: shipment.partyIdTo)
                    .filterByDate()
                    .queryFirst()
            if (phoneNumber) {
                shipment.destinationTelecomNumberId = phoneNumber.contactMechId
            } else {
                logWarning("Cannot find a shipping destination phone number for ${shipment.primaryOrderId}")
            }
        }
    }
    // originTelecomNumberId
    if (!shipment.originTelecomNumberId) {
        List originTelecomOrderContactMechs = EntityUtil.filterByAnd(orderContactMechs,
                [contactMechPurposeTypeId: "PHONE_SHIP_ORIG"])
        if (originTelecomOrderContactMechs) {
            shipment.originTelecomNumberId = originTelecomOrderContactMechs[0].contactMechId
        } else {
            logWarning("Cannot find a shipping origin phone number for ${shipment.primaryOrderId}")
        }
    }
    // set the destination facility if it is a purchase order
    if (!shipment.destinationFacilityId) {
        if (shipment.shipmentTypeId == "PURCHASE_SHIPMENT") {
            GenericValue destinationFacility = from("FacilityContactMech")
                    .where(contactMechId: shipment.destinationContactMechId)
                    .queryFirst()
            shipment.destinationFacilityId = destinationFacility.facilityId
        }
    }
    /*
     * NOTE: use new place to find source/destination location/addresses for new OrderItemShipGroup.contactMechId
     * (destination address for sales orders, source address for purchase orders)
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

        BigDecimal shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems,
                orderAdjustments, false, false, true)
        shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments,
                orderSubTotal, false, false, true))
        shipment.estimatedShipCost = shippingAmount
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

        orderItemShipGroup = from("OrderItemShipGroup")
                .where(orderId: shipment.primaryOrderId,
                        shipGroupSeqId: shipment.primaryShipGroupSeqId)
                .queryOne()
        if (orderItemShipGroup) {
            shipmentRouteSegmentMap.carrierPartyId = orderItemShipGroup.carrierPartyId
            shipmentRouteSegmentMap.shipmentMethodTypeId = orderItemShipGroup.shipmentMethodTypeId
        }
        run service: "createShipmentRouteSegment", with: shipmentRouteSegmentMap
    }
    run service: "updateShipment", with: shipment.getAllFields()
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
            facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.originFacilityId, ["SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"])
            if (facilityContactMech != null) {
                shipment.originContactMechId = facilityContactMech.contactMechId
            }
        }
        if (!shipment.originTelecomNumberId) {
            facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.originFacilityId, ["PHONE_SHIP_ORIG", "PRIMARY_PHONE"])
            if (facilityContactMech != null) {
                shipment.originTelecomNumberId = facilityContactMech.contactMechId
            }
        }
    }
    if (shipment.destinationFacilityId) {
        if (!shipment.destinationContactMechId) {
            facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.destinationFacilityId, ["SHIPPING_LOCATION", "PRIMARY_LOCATION"])
            if (facilityContactMech != null) {
                shipment.destinationContactMechId = facilityContactMech.contactMechId
            }
        }
        if (!shipment.destinationTelecomNumberId) {
            facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(
                    delegator, shipment.destinationFacilityId, ["PHONE_SHIPPING", "PRIMARY_PHONE"])
            if (facilityContactMech != null) {
                shipment.destinationTelecomNumberId = facilityContactMech.contactMechId
            }
        }
    }
    if (shipment != shipmentCopy) {
        run service: "updateShipment", with: shipment.getAllFields()
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
    GenericValue curUserPartyAndContactMech = from("PartyAndContactMech")
            .where(partyId: userLogin.partyId,
                    contactMechTypeId: "EMAIL_ADDRESS")
            .queryFirst()
    Map sendEmailMap = [sendFrom: curUserPartyAndContactMech.infoString]

    // find email addresses of partyIdFrom, set as sendTo
    Map sendToPartyIdMap = [:]
    sendToPartyIdMap."${shipment.partyIdFrom}" = shipment.partyIdFrom

    // find email addresses of all parties not equal to partyIdFrom in SUPPLIER_AGENT roleTypeId
    // associated with primary order, set as sendTo
    List supplierAgentOrderRoles = from("OrderRole")
            .where(orderId: shipment.primaryOrderId,
                    roleTypeId: "SUPPLIER_AGENT")
            .queryList()
    for (GenericValue supplierAgentOrderRole : supplierAgentOrderRoles) {
        sendToPartyIdMap."${supplierAgentOrderRole.partyId}" = supplierAgentOrderRole.partyId
    }

    // go through all send to parties and get email addresses
    List sendTos = []
    for (Map.Entry entry : sendToPartyIdMap) {
        List sendToPartyAndContactMechs = from("PartyAndContactMech")
                .where(partyId: entry.getKey(),
                        contactMechTypeId: "EMAIL_ADDRESS")
                .getFieldList("infoString")
        sendToPartyAndContactMechs.each {
            sendTos << it
        }
    }
    sendEmailMap.sendTo = sendTos.join(",")

    // set subject, contentType, templateName, templateData
    sendEmailMap.subject = "Scheduled Notification for Shipment " + shipment.shipmentId
    if (shipment.primaryOrderId) {
        sendEmailMap.subject = "${sendEmailMap.subject} for Primary Order ${shipment.primaryOrderId}"
    }
    sendEmailMap.contentType = "text/html"
    sendEmailMap.templateName = "component://product/template/shipment/ShipmentScheduledNotice.ftl"
    sendEmailMap.templateData = [shipment: shipment]

    // call sendGenericNotificationEmail service, if enough information was found
    logInfo("Sending generic notification email (if all info is in place): ${sendEmailMap}")
    if (sendEmailMap.sendTo && sendEmailMap.sendFrom) {
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
    List issuances = shipment.getRelated("ItemIssuance", null, null, false)
    for (GenericValue issuance : issuances) {
        List receipts = from("ShipmentReceipt")
                .where( shipmentId: shipment.shipmentId,
                        orderId: issuance.orderId,
                        orderItemSeqId: issuance.orderItemSeqId)
                .queryList()
        BigDecimal issuanceQuantity = BigDecimal.ZERO
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
    GenericValue originalShipmentItem = from("ShipmentItem").where(parameters).queryOne()

    // create new ShipmentItem
    Map serviceResult = run service: "createShipmentItem", with: [shipmentId: originalShipmentItem.shipmentId,
                                                                  productId: originalShipmentItem.productId,
                                                                  quantity: parameters.newItemQuantity]
    String newShipmentItemSeqId = serviceResult.shipmentItemSeqId

    // reduce the originalShipmentItem.quantity
    originalShipmentItem.quantity -= parameters.newItemQuantity

    // update the original ShipmentItem
    run service: "updateShipmentItem", with: originalShipmentItem.getAllFields()

    // split the OrderShipment record(s) as well for the new quantities,
    // from originalShipmentItem.shipmentItemSeqId to newShipmentItemSeqId
    List itemOrderShipmentList = from("OrderShipment")
            .where(shipmentId: originalShipmentItem.shipmentId,
                    shipmentItemSeqId: originalShipmentItem.shipmentItemSeqId)
            .queryList()
    BigDecimal orderShipmentQuantityLeft = parameters.newItemQuantity
    for (GenericValue itemOrderShipment : itemOrderShipmentList) {
        if (orderShipmentQuantityLeft > (BigDecimal.ZERO)) {
            if (itemOrderShipment.quantity > orderShipmentQuantityLeft) {
                // there is enough in this OrderShipment record, so just adjust it and move on
                Map updateOrderShipmentMap = itemOrderShipment.getAllFields()
                updateOrderShipmentMap.quantity = itemOrderShipment.quantity - orderShipmentQuantityLeft
                run service: "updateOrderShipment", with: updateOrderShipmentMap
                run service: "createOrderShipment", with: [orderId: itemOrderShipment.orderId,
                                                           orderItemSeqId: itemOrderShipment.orderItemSeqId,
                                                           shipmentId: itemOrderShipment.shipmentId,
                                                           shipmentItemSeqId: newShipmentItemSeqId,
                                                           quantity: orderShipmentQuantityLeft]
                orderShipmentQuantityLeft = BigDecimal.ZERO
            } else {
                // not enough on this one, create a new one for the new item and delete this one
                run service: "deleteOrderShipment", with: itemOrderShipment.getAllFields()
                run service: "createOrderShipment", with: [orderId: itemOrderShipment.orderId,
                                                           orderItemSeqId: itemOrderShipment.orderItemSeqId,
                                                           shipmentId: itemOrderShipment.shipmentId,
                                                           shipmentItemSeqId: newShipmentItemSeqId,
                                                           quantity: itemOrderShipment.quantity]
                orderShipmentQuantityLeft -= itemOrderShipment.quantity
            }
        }
    }
    Map result = success()
    result.newShipmentItemSeqId = newShipmentItemSeqId
    return result
}

// ShipmentPackage services

/**
 * Create ShipmentPackage
 * @return
 */
def createShipmentPackage() {
    GenericValue newEntity = makeValue("ShipmentPackage", parameters)
    if ("New" == newEntity.shipmentPackageSeqId) {
        newEntity.shipmentPackageSeqId = null
    }

    // if no shipmentPackageSeqId, generate one based on existing items, ie one greater than the current higher number
    if (!newEntity.shipmentPackageSeqId) {
        delegator.setNextSubSeqId(newEntity, "shipmentPackageSeqId", 5, 1)
    }

    newEntity.dateCreated = UtilDateTime.nowTimestamp()
    newEntity.create()
    ensurePackageRouteSeg(newEntity.shipmentId, newEntity.shipmentPackageSeqId)

    Map result = success()
    result.shipmentPackageSeqId = newEntity.shipmentPackageSeqId
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
    List shipmentPackageContents = from("ShipmentPackageContent")
            .where(shipmentId: parameters.shipmentId,
                    shipmentPackageSeqId: parameters.shipmentPackageSeqId)
            .queryList()
    if (shipmentPackageContents) {
        String errorMessage = UtilProperties.getMessage("ProductErrorUiLabels",
                "ProductErrorShipmentPackageCannotBeDeleted", locale)
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
        GenericValue checkShipmentPackageRouteSeg = from("ShipmentPackageRouteSeg")
                .where(shipmentId: shipmentId,
                        shipmentPackageSeqId : shipmentPackageSeqId,
                        shipmentRouteSegmentId: shipmentRouteSegment.shipmentRouteSegmentId)
                .queryOne()
        if (!checkShipmentPackageRouteSeg) {
            run service: "createShipmentPackageRouteSeg", with: [shipmentId: shipmentId,
                                                                 shipmentRouteSegmentId: shipmentRouteSegment.shipmentRouteSegmentId,
                                                                 shipmentPackageSeqId: shipmentPackageSeqId]
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
    if (!shipmentPackageContent) {
        Map serviceResult = run service: "createShipmentPackageContent", with: parameters
        newEntity.shipmentPackageSeqId = serviceResult.shipmentPackageSeqId
    } else {
        // add the quantities and store it
        shipmentPackageContent.quantity += parameters.quantity
        run service: "updateShipmentPackageContent", with: shipmentPackageContent.getAllFields()
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
        GenericValue checkShipmentPackageRouteSeg = from("ShipmentPackageRouteSeg")
                .where(shipmentRouteSegment as Map)
                .queryOne()
        if (!checkShipmentPackageRouteSeg) {
            run service: "createShipmentPackageRouteSeg", with: [shipmentId: parameters.shipmentId,
                                                                 shipmentRouteSegmentId: parameters.shipmentRouteSegmentId,
                                                                 shipmentPackageSeqId: shipmentPackage.shipmentPackageSeqId]
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
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    Boolean hasPermission = serviceResult.hasPermission
    GenericValue testShipment = from("Shipment").where(inputParameters).cache().queryOne()
    if (testShipment) {

        boolean badMoveToPacked = testShipment.statusId == "SHIPMENT_PACKED" && fromStatusId == "SHIPMENT_PACKED"
        boolean badMoveToShipped = testShipment.statusId == "SHIPMENT_SHIPPED" &&
                ["SHIPMENT_PACKED", "SHIPMENT_SHIPPED"].contains(fromStatusId)
        boolean badMoveToDelivered = testShipment.statusId == "SHIPMENT_DELIVERED" &&
                ["SHIPMENT_PACKED", "SHIPMENT_SHIPPED", "SHIPMENT_DELIVERED"].contains(fromStatusId)

        if (badMoveToPacked || badMoveToShipped || badMoveToDelivered
                || testShipment.statusId == "SHIPMENT_CANCELLED") {
            GenericValue testShipmentStatus = testShipment.getRelatedOne("StatusItem", true)
            Map testShipmentMap = [testShipment      : testShipment,
                                   testShipmentStatus: testShipmentStatus]
            String failMessage = UtilProperties.getMessage("ProductErrorUiLabels",
                    "ShipmentCanChangeStatusPermissionError", testShipmentMap, locale)
            hasPermission = false
            result.failMessage = failMessage
        }
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
    if (!orderHeader || !orderHeader.productStoreId) {
        // no store cannot use quick ship; throw error
        return error(UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentMissingProductStore", locale))
    }
    // get the product store entity
    GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
    if ("Y" != productStore?.reserveInventory) {
        // no reservations; no shipment; cannot use quick ship
        return error(UtilProperties.getMessage("ProductUiLabels",
                "FacilityShipmentNotCreatedForNotReserveInventory", [productStore: productStore], locale))
    }
    if ("Y" == productStore.explodeOrderItems) {
        // can't insert duplicate rows in shipmentPackageContent
        return errorMessage(UtilProperties.getMessage("ProductUiLabels",
                "FacilityShipmentNotCreatedForExplodesOrderItems", [productStore: productStore], locale))
    }
    // locate shipping facilities associated with order item rez's
    List orderItemShipGrpInvResFacilityIds = from("OrderItemAndShipGrpInvResAndItem")
            .where(orderId:orderHeader.orderId,
                    statusId: "ITEM_APPROVED")
            .distinct()
            .getFieldList('facilityId')
    Map serviceResult = getOrderItemShipGroupLists(orderHeader)

    // traverse facilities, instantiate shipment for each
    for (String orderItemShipGrpInvResFacilityId : orderItemShipGrpInvResFacilityIds) {
        // sanity check for valid facility
        GenericValue facility = from("Facility").where(facilityId: orderItemShipGrpInvResFacilityId).queryOne()
        // should never be empty - referential integrity enforced
        Map serviceResultCSFFASG = createShipmentForFacilityAndShipGroup(orderHeader,
                serviceResult.orderItemListByShGrpMap, serviceResult.orderItemShipGroupList,
                serviceResult.orderItemAndShipGroupAssocList, orderItemShipGrpInvResFacilityId,
                parameters.eventDate, parameters.setPackedOnly)
        successMessageList = serviceResultCSFFASG.successMessageList
        shipmentIds = serviceResultCSFFASG.shipmentIds
        shipmentShipGroupFacilityList = serviceResultCSFFASG.shipmentShipGroupFacilityList
    }
    logInfo("Finished quickShipEntireOrder:\n" +
            "shipmentShipGroupFacilityList=${shipmentShipGroupFacilityList}\n" +
            "successMessageList=${successMessageList}")
    result.shipmentShipGroupFacilityList = shipmentShipGroupFacilityList
    result.successMessageList = successMessageList
    if (!shipmentShipGroupFacilityList) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentNotCreated", locale)
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
        String errorMessage = UtilProperties.getMessage("OrderErrorUiLabels",
                "OrderApproveOrderBeforeQuickDropShip", locale)
        return error(errorMessage)
    }
    Map serviceResultCS = run service: "createShipment", with: [primaryOrderId: parameters.orderId,
                                                                primaryShipGroupSeqId: parameters.shipGroupSeqId,
                                                                statusId: "PURCH_SHIP_CREATED",
                                                                shipmentTypeId: "DROP_SHIPMENT"]
    if (ServiceUtil.isError(serviceResultCS)) {
        return serviceResultCS
    }
    String shipmentId = serviceResultCS.shipmentId
    Map updateShipmentContext = [shipmentId: shipmentId,
                                 statusId: "PURCH_SHIP_SHIPPED"]
    Map serviceResult = run service: "updateShipment", with: updateShipmentContext
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    updateShipmentContext.statusId = "PURCH_SHIP_RECEIVED"
    serviceResult = run service: "updateShipment", with: updateShipmentContext
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    result.shipmentId = shipmentId
    // Iterate through the order items in the ship group
    List orderItemShipGroupAssocs = from("OrderItemShipGroupAssoc")
            .where(orderId: parameters.orderId,
                    shipGroupSeqId: parameters.shipGroupSeqId)
            .queryList()
    for (GenericValue orderItemShipGroupAssoc : orderItemShipGroupAssocs) {
        GenericValue orderItem = orderItemShipGroupAssoc.getRelatedOne("OrderItem", false)
        // Set the item status to completed
        Map resultCOIS = run service: "changeOrderItemStatus", with: [orderId: parameters.orderId,
                                                                      orderItemSeqId: orderItem.orderItemSeqId,
                                                                      statusId: "ITEM_COMPLETED"]
        if (ServiceUtil.isError(resultCOIS)) {
            return resultCOIS
        }
        // Set the status of any linked sales order items to completed as well
        List orderItemAssocs = from("OrderItemAssoc")
                .where(toOrderId: parameters.orderId,
                        toOrderItemSeqId: orderItem.orderItemSeqId,
                        orderItemAssocTypeId: "DROP_SHIPMENT")
                .queryList()
        if (orderItemAssocs) {
            for (GenericValue orderItemAssoc : orderItemAssocs) {
                Map serviceResultCOIS = run service: "changeOrderItemStatus", with: [orderId: orderItemAssoc.orderId,
                                                                      orderItemSeqId: orderItemAssoc.orderItemSeqId,
                                                                      statusId: "ITEM_COMPLETED"]
                if (ServiceUtil.isError(serviceResultCOIS)) {
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
    Map serviceResult = getOrderItemShipGroupLists(orderHeader)
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    Map serviceResultCSFFASG = createShipmentForFacilityAndShipGroup(orderHeader, serviceResult.orderItemListByShGrpMap,
            serviceResult.orderItemShipGroupList, serviceResult.orderItemAndShipGroupAssocList,
            parameters.facilityId, null, null)
    logInfo("Finished quickReceivePurchaseOrder for orderId ${parameters.orderId}" +
            " and destination facilityId ${parameters.facilityId}" +
            " shipment created ${serviceResultCSFFASG.shipmentIds}")
    result.shipmentIds = serviceResultCSFFASG.shipmentIds
    return result
}

/**
 * Sub-method used by quickShip methods to get a list of OrderItemAndShipGroupAssoc and a Map of shipGroupId -> OrderItemAndShipGroupAssoc
 * @return
 */
def getOrderItemShipGroupLists(GenericValue orderHeader) {
    // lookup all the approved items, doing by item because the item must be approved before shipping
    List orderItemAndShipGroupAssocList = from("OrderItemAndShipGroupAssoc")
            .where(orderId: orderHeader.orderId,
                    statusId: "ITEM_APPROVED")
            .queryList()
    // make sure we have something to ship
    if (!orderItemAndShipGroupAssocList) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "FacilityNoItemsAvailableToShip", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    List orderItemShipGroupList = orderHeader.getRelated("OrderItemShipGroup", null, null, false)
    // group orderItems (actually OrderItemAndShipGroupAssocs) by shipGroupSeqId in a Map with List values
    // This Map is actually used only for sales orders' shipments right now.
    List orderItemListByShGrpMap = []
    for (GenericValue orderItemAndShipGroupAssoc : orderItemAndShipGroupAssocList) {
        orderItemListByShGrpMap << orderItemAndShipGroupAssoc
    }
    Map result = success()
    result.orderItemListByShGrpMap = orderItemListByShGrpMap
    result.orderItemAndShipGroupAssocList = orderItemAndShipGroupAssocList
    result.orderItemShipGroupList = orderItemShipGroupList
    return result
}

/**
 * Sub-method used by quickShip methods to create a shipment
 * @return
 */
def createShipmentForFacilityAndShipGroup(GenericValue orderHeader, List orderItemListByShGrpMap,
                                          List orderItemShipGroupList, List orderItemAndShipGroupAssocList,
                                          String orderItemShipGrpInvResFacilityId,
                                          Timestamp eventDate, Boolean setPackedOnly) {
    Map result = success()
    List shipmentIds = []
    List shipmentShipGroupFacilityList = []
    List successMessageList = []
    String partyIdFrom
    // for OrderItemShipGroup need to split all OISGIRs into their ship groups and create a shipment for each
    GenericValue facility = from("Facility").where(facilityId: orderItemShipGrpInvResFacilityId).cache().queryOne()
    for (GenericValue orderItemShipGroup : orderItemShipGroupList) {

        List perShipGroupItemList = orderItemListByShGrpMap
        // make sure we have something to ship
        if (!perShipGroupItemList) {
            List argListNames = [orderItemShipGroup.shipGroupSeqId]
            return success(UtilProperties.getMessage("ProductUiLabels",
                    "FacilityShipmentNoItemsAvailableToShip", argListNames, locale))
        } else {
            // create the shipment for this facility and ship group combination
            Map shipmentContext = [primaryOrderId: orderHeader.orderId,
                                   primaryShipGroupSeqId: orderItemShipGroup.shipGroupSeqId]
            // for Sales Shipment, order items' reservation facilityId is the originFacilityId, and the initial status is "INPUT"
            // for Purchase Shipment, the facilityId parameter is the destinationFacilityId, and the initial status is "CREATED"
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                if (orderItemShipGroup.vendorPartyId) {
                    partyIdFrom = orderItemShipGroup.vendorPartyId
                } else {
                    if (facility?.ownerPartyId) {
                        partyIdFrom = facility.ownerPartyId
                    }
                    if (!partyIdFrom) {
                        GenericValue orderRole = from("OrderRole")
                                .where(orderId: orderHeader.orderId,
                                        roleTypeId: "SHIP_FROM_VENDOR")
                                .queryFirst()
                        if (!orderRole) {
                            orderRole = from("OrderRole")
                                    .where(orderId: orderHeader.orderId,
                                            roleTypeId: "BILL_FROM_VENDOR")
                                    .queryFirst()
                        }
                        if (orderRole)
                            partyIdFrom = orderRole.partyId
                    }
                }
                shipmentContext.partyIdFrom = partyIdFrom
                shipmentContext.originFacilityId = orderItemShipGrpInvResFacilityId
                shipmentContext.statusId = "SHIPMENT_INPUT"
            } else {
                shipmentContext.destinationFacilityId = facility.facilityId
                shipmentContext.statusId = "PURCH_SHIP_CREATED"
            }
            Map serviceResult = run service: "createShipment", with: shipmentContext
            GenericValue shipment = from("Shipment").where(shipmentId: serviceResult.shipmentId).queryOne()
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                for (GenericValue orderItemAndShipGroupAssoc : perShipGroupItemList) {
                    // just get the OrderItemShipGrpInvResAndItem records for this facility and this ship group,
                    // since that is what this shipment is for
                    List itemResList = orderItemAndShipGroupAssoc.getRelated("OrderItemShipGrpInvResAndItem",
                            [facilityId: orderItemShipGrpInvResFacilityId], null, false)
                    for (GenericValue itemRes : itemResList) {
                        run service: "issueOrderItemShipGrpInvResToShipment", with: [shipmentId: shipment.shipmentId,
                                                                                     orderId: itemRes.orderId,
                                                                                     orderItemSeqId: itemRes.orderItemSeqId,
                                                                                     shipGroupSeqId: itemRes.shipGroupSeqId,
                                                                                     inventoryItemId: itemRes.inventoryItemId,
                                                                                     quantity: itemRes.quantity,
                                                                                     eventDate: eventDate]
                    }
                }
            } else { // Issue all purchase order items
                for (GenericValue item : orderItemAndShipGroupAssocList) {
                    run service: "issueOrderItemToShipment", with: [shipmentId: shipment.shipmentId,
                                                                    orderId: item.orderId,
                                                                    orderItemSeqId: item.orderItemSeqId,
                                                                    shipGroupSeqId: item.shipGroupSeqId,
                                                                    quantity: item.quantity]
                }
            }
            // place all issued items into a single package
            List itemIssuances = from("ItemIssuance")
                    .where(orderId: orderHeader.orderId,
                            shipGroupSeqId: orderItemShipGroup.shipGroupSeqId,
                            shipmentId: shipment.shipmentId)
                    .queryList()
            String shipmentPackageSeqId = "New"
            for (GenericValue itemIssuance: itemIssuances) {
                Map serviceResultASCTP = run service: "addShipmentContentToPackage", with: [shipmentId: itemIssuance.shipmentId,
                                                                                            shipmentItemSeqId: itemIssuance.shipmentItemSeqId,
                                                                                            quantity: itemIssuance.quantity,
                                                                                            shipmentPackageSeqId: shipmentPackageSeqId]
                shipmentPackageSeqId = serviceResultASCTP.shipmentPackageSeqId
            }
            if (orderHeader.orderTypeId == "SALES_ORDER") {
                // update the shipment status to packed
                run service: "updateShipment", with: [shipmentId: shipment.shipmentId,
                                                      eventDate: eventDate,
                                                      statusId: "SHIPMENT_PACKED"]

                // update the shipment status to shipped (if setPackedOnly has NOT been set)
                if (!setPackedOnly) {
                    run service: "updateShipment", with: [shipmentId: shipment.shipmentId,
                                                          eventDate: eventDate,
                                                          statusId: "SHIPMENT_SHIPPED"]
                }
            } else { // PURCHASE_ORDER
                // update the shipment status to shipped
                run service: "updateShipment", with: [shipmentId: shipment.shipmentId,
                                                      eventDate: eventDate,
                                                      statusId: "PURCH_SHIP_SHIPPED"]
            }
            Map shipmentShipGroupFacility = [shipmentId: shipment.shipmentId,
                                             facilityId: facility.facilityId,
                                             shipGroupSeqId: orderItemShipGroup.shipGroupSeqId]
            shipmentShipGroupFacilityList << shipmentShipGroupFacility
            List argListNames = []
            argListNames << shipmentShipGroupFacility.shipmentId
            argListNames << shipmentShipGroupFacility.shipGroupSeqId
            argListNames << shipmentShipGroupFacility.facilityId
            successMessageList << UtilProperties.getMessage("ProductUiLabels",
                    "FacilityShipmentIdCreated", argListNames, locale)
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
    GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).cache().queryOne()
    List orderItemShipGroupList = orderHeader.getRelated("OrderItemShipGroup", null, null, false)
    for (GenericValue orderItemShipGroup : orderItemShipGroupList) {
        Map serviceResult = run service: "createShipment", with: [primaryOrderId: orderHeader.orderId,
                                                                  primaryShipGroupSeqId: orderItemShipGroup.shipGroupSeqId,
                                                                  statusId: "SHIPMENT_INPUT",
                                                                  originFacilityId: productStore.inventoryFacilityId]
        parameters.shipmentId = serviceResult.shipmentId
        GenericValue shipment = from("Shipment").where(parameters).queryOne()
        List orderItems = orderHeader.getRelated("OrderItem", null, null, false)
        for (GenericValue orderItem : orderItems) {
            GenericValue itemProduct = from("Product").where(productId: orderItem.productId).cache().queryOne()

            // make sure the OrderItem is for a Product that has a ProductType with isPhysical=Y
            if (itemProduct) {
                GenericValue itemProductType = itemProduct.getRelatedOne("ProductType", true)
                if (itemProductType.isPhysical == "Y") {

                    // Create shipment item
                    run service: "addOrderShipmentToShipment", with: [orderId: orderHeader.orderId,
                                                                      orderItemSeqId: orderItem.orderItemSeqId,
                                                                      shipmentId: shipment.shipmentId,
                                                                      shipGroupSeqId: orderItemShipGroup.shipGroupSeqId,
                                                                      quantity: orderItem.quantity]
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
        GenericValue inventoryItem = orderItemShipGrpInvRes.getRelatedOne("InventoryItem", false)
        if (inventoryItem.serialNumber != parameters.serialNumber) {
            // The inventory that we have reserved is not what we shipped. Lets reReserve, this time we'll get what we want
            Map serviceResult = run service: "reserveAnInventoryItem", with: parameters
            parameters.inventoryItemId = serviceResult.inventoryItemId
        }
    }

    // get InventoryItem issued to shipment
    Map serviceResultIO = run service: "issueOrderItemShipGrpInvResToShipment", with: parameters
    parameters.itemIssuanceId = serviceResultIO.itemIssuanceId

    // place all issued items into a package for tracking num
    logInfo("QuickShipOrderByItem grouping by tracking number : ${parameters.trackingNum}")
    GenericValue itemIssuance = from("ItemIssuance").where(parameters).queryOne()
    Map shipItemContext = [:]
    shipItemContext.shipmentPackageSeqId = parameters.shipmentPackageSeqId ?: "New"
    logInfo("Package SeqID : ${shipItemContext.shipmentPackageSeqId}")
    GenericValue shipmentPackage = from("ShipmentPackage").where(parameters).queryOne()
    if (!shipmentPackage) {
        run service: "createShipmentPackage", with: [shipmentId: itemIssuance.shipmentId,
                                                     shipmentPackageSeqId: shipItemContext.shipmentPackageSeqId]
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
    run service: "updateShipment", with: [shipmentId: parameters.shipmentId,
                                          statusId: "SHIPMENT_PACKED"]
    // update the shipment status to shipped
    if (!parameters.setPackedOnly) {
        run service: "updateShipment", with: [shipmentId: parameters.shipmentId,
                                          statusId: "SHIPMENT_SHIPPED"]
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
            GenericValue productStore = from("ProductStore")
                    .where(productStoreId: orderHeader.productStoreId)
                    .cache()
                    .queryOne()
            if (productStore.reserveInventory != "Y") {
                // no reservations; no shipment; cannot use quick ship
                String errorMessage = UtilProperties.getMessage("ProductUiLabels",
                        "FacilityNoQuickShipForNotReserveInventory", locale)
                logError(errorMessage)
                return error(errorMessage)
            }
            if (productStore.oneInventoryFacility != "Y") {
                // if we allow multiple facilities we cannot use quick ship; throw error
                String errorMessage = UtilProperties.getMessage("ProductUiLabels",
                        "FacilityNoQuickShipForMultipleFacilities", locale)
                logError(errorMessage)
                return error(errorMessage)
            }
            if (!productStore.inventoryFacilityId) {
                String errorMessage = UtilProperties.getMessage("ProductUiLabels",
                        "FacilityNoQuickShipForNotInventoryFacility", locale)
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
    Map shipmentContext = [primaryOrderId: parameters.orderId,
                           primaryShipGroupSeqId: parameters.shipGroupSeqId,
                           statusId: "SHIPMENT_INPUT"]
    if (parameters.originFacilityId) {
        shipmentContext.originFacilityId = parameters.originFacilityId
    }
    Map serviceResult = run service: "createShipment", with: shipmentContext
    GenericValue shipment = from("Shipment").where(serviceResult).queryOne()

    // issue the passed in order items
    for (Map itemMap : itemMapList) {
        Map serviceResultIO = run service: "issueOrderItemShipGrpInvResToShipment", with: [shipmentId: shipment.shipmentId,
                                                                                           orderId: parameters.orderId,
                                                                                           shipGroupSeqId: parameters.shipGroupSeqId,
                                                                                           orderItemSeqId: itemMap.orderItemSeqId,
                                                                                           inventoryItemId: itemMap.inventoryItemId,
                                                                                           quantity: itemMap.qtyShipped]
        itemMap.itemIssuanceId = serviceResultIO.itemIssuanceId
    }
    // place all issued items into a unique package per tracking num
    Map packageMap = [:]
    for (Map itemMap : itemMapList) {
        logInfo("QuickShipOrderByItem grouping by tracking number : ${itemMap.trackingNum}")
        GenericValue itemIssuance = from("ItemIssuance").where(itemIssuanceId: itemMap.itemIssuanceId).queryOne()
        Map shipItemContext = [shipmentId: itemIssuance.shipmentId,
                               shipmentItemSeqId: itemIssuance.shipmentItemSeqId,
                               quantity: itemIssuance.quantity]
        shipItemContext.shipmentPackageSeqId = packageMap."${itemMap.trackingNum}" ?: "New"
        logInfo("Package SeqID : ${shipItemContext.shipmentPackageSeqId}")
        Map serviceResultASCTP = run service: "addShipmentContentToPackage", with: shipItemContext
        packageMap."${itemMap.trackingNum}" = serviceResultASCTP.shipmentPackageSeqId
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
    run service: "updateShipment", with: [shipmentId: shipment.shipmentId,
                                          statusId: "SHIPMENT_PACKED"]

    // update the shipment status to shipped
    if (!parameters.setPackedOnly) {
        run service: "updateShipment", with: [shipmentId: shipment.shipmentId,
                                          statusId: "SHIPMENT_SHIPPED"]
    }
    Map result = success()
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
    run service: "deleteOrderShipment", with: parameters
    shipmentItem.quantity = orderShipment.quantity - shipmentItem.quantity
    if (shipmentItem.quantity > (BigDecimal.ZERO)) {
        run service: "updateShipmentItem", with: shipmentItem.getAllFields()
    } else {
        run service: "deleteShipmentItem", with: parameters
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
    if (parameters.quantity > (BigDecimal.ZERO)) {
        // get orderHeader
        GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
        // get orderItem
        GenericValue orderItem = from("OrderItem").where(parameters).queryOne()
        // make sure the orderItem is not already present in this shipment
        if (from("OrderShipment")
                .where(orderId: parameters.orderId,
                        orderItemSeqId: parameters.orderItemSeqId,
                        shipGroupSeqId: parameters.shipGroupSeqId,
                        shipmentId: parameters.shipmentId)
                .queryCount() != 0) {
            return error("Not adding Order Item to plan for shipment [${parameters.shipmentId}] because" +
                    " the order item is already in the shipment (order [${parameters.orderId}]," +
                    " order item [${parameters.orderItemSeqId}])")
        }
        Map serviceResult = run service: "getQuantityForShipment", with: [orderId: parameters.orderId,
                                                                          orderItemSeqId: parameters.orderItemSeqId]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        BigDecimal remainingQuantity = serviceResult.remainingQuantity

        if (parameters.quantity > remainingQuantity) {
            return error("Not adding Order Item to plan for shipment [${parameters.shipmentId}] because" +
                    " the quantity is greater than the remaining quantity" +
                    " (order [${parameters.orderId}], order item [${parameters.orderItemSeqId}])")
        }
        Map serviceResultCSI = run service: "createShipmentItem", with: [shipmentId: parameters.shipmentId,
                                                                         productId: orderItem.productId,
                                                                         quantity: parameters.quantity]
        parameters.shipmentItemSeqId = serviceResultCSI.shipemntItemSeqId
        result.shipmentItemSeqId = serviceResultCSI.shipmentItemSeqId
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
    BigDecimal plannedQuantity = BigDecimal.ZERO
    BigDecimal issuedQuantity = BigDecimal.ZERO
    // get orderItem
    GenericValue orderItem = from("OrderItem").where(parameters).queryOne()
    Map orderShipmentLookup = [orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId]
    List existingOrderShipments = from("OrderShipment").where(orderShipmentLookup).queryList()
    for (GenericValue orderShipment : existingOrderShipments) {
        plannedQuantity += orderShipment.quantity ?: BigDecimal.ZERO
    }
    existingOrderShipments = from("ItemIssuance").where(orderShipmentLookup).queryList()
    for (GenericValue itemIssuance : existingOrderShipments) {
        BigDecimal quantity = itemIssuance.quantity ?: BigDecimal.ZERO
        BigDecimal cancelQuantity = itemIssuance.cancelQuantity ?: BigDecimal.ZERO
        issuedQuantity += quantity - cancelQuantity
    }

    BigDecimal totPlannedOrIssuedQuantity = issuedQuantity + plannedQuantity
    BigDecimal orderCancelQuantity = orderItem.cancelQuantity ?: BigDecimal.ZERO

    result.remainingQuantity = orderCancelQuantity + totPlannedOrIssuedQuantity - orderItem.quantity
    return result
}

/**
 * Check Shipment Items and Cancel Item Issuance and Order Shipment
 * @return
 */
def checkCancelItemIssuanceAndOrderShipmentFromShipment() {
    List orderShipmentList = from("OrderShipment").where(shipmentId: parameters.shipmentId).queryList()
    for (GenericValue orderShipment : orderShipmentList) {
        run service: "deleteOrderShipment", with: orderShipment.getAllFields()
    }
    logInfo("Cancelling Item Issuances for shimpentId: ${parameters.shipmentId}")
    GenericValue shipment = from("Shipment").where(parameters).queryOne()
    List issuances = shipment.getRelated("ItemIssuance", null, null, false)
    for (GenericValue issuance : issuances) {
        run service: "cancelOrderItemIssuanceFromSalesShipment", with: [itemIssuanceId: issuance.itemIssuanceId]
    }
    return success()
}
