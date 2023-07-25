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
package org.apache.ofbiz.order.order

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil

/**
 * Create a ReturnHeader
 */
Map createReturnHeader() {
    Map result = success()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin)
            && userLogin.partyId != parameters.fromPartyId) {
        return informError('OrderSecurityErrorToRunCreateReturnHeader')
    }
    String returnHeaderTypeId = parameters.returnHeaderTypeId
    if (parameters.toPartyId) {
        // make sure that the party to return to is an INTERNAL_ORGANIZATIO for customer return
        // and SUPPLIER for vendor return else stop
        if (returnHeaderTypeId.contains('CUSTOMER_')) {
            GenericValue partyRole = from('PartyRole')
                    .where(partyId: parameters.toPartyId, roleTypeId: 'INTERNAL_ORGANIZATIO')
                    .cache()
                    .queryOne()
            if (!partyRole) {
                return informError('OrderReturnRequestPartyRoleInternalOrg')
            }
        } else {
            GenericValue partyRole = from('PartyRole')
                    .where(partyId: parameters.toPartyId, roleTypeId: 'SUPPLIER')
                    .cache()
                    .queryOne()
            if (!partyRole) {
                return informError('OrderReturnRequestPartyRoleSupplier')
            }
        }
    } else {
        // no toPartyId was specified. use destination facility to determine the party of the return
        if (returnHeaderTypeId == 'CUSTOMER_RETURN' && parameters.destinationFacilityId) {
            GenericValue destinationFacility = from('Facility')
                    .where(facilityId: parameters.destinationFacilityId)
                    .cache()
                    .queryOne()
            parameters.toPartyId = destinationFacility.ownerPartyId
        }
    }
    if (parameters.paymentMethodId) {
        GenericValue paymentMethod = from('PaymentMethod')
                .where(paymentMethodId: parameters.paymentMethodId)
                .cache()
                .queryOne()
        if (!paymentMethod) {
            return informError('OrderReturnRequestPaymentMethodId')
        }
    }
    // check the needs (auto) inventory receive flag
    // (default to N, meaning that return won't automatically be considered Received when Accepted)
    parameters.needsInventoryReceive = parameters.needsInventoryReceive ?: 'N'
    GenericValue newEntity = makeValue('ReturnHeader')
    newEntity.setNonPKFields(parameters)

    // If PartyAcctgPreference.useInvoiceIdForReturns is Y, get the ID from the getNextInvoiceId service
    GenericValue systemLogin = from('UserLogin')
            .where(userLoginId: 'system')
            .cache()
            .queryOne()
    Map serviceResult = run service: 'getPartyAccountingPreferences', with: [organizationPartyId: parameters.toPartyId,
                                                                             userLogin: systemLogin]
    GenericValue partyAcctgPreference = serviceResult.partyAccountingPreference

    if (partyAcctgPreference && partyAcctgPreference.useInvoiceIdForReturns == 'Y') {
        Map serviceResultInvoice = run service: 'getNextInvoiceId', with: [partyId: parameters.toPartyId]
        newEntity.returnId = serviceResultInvoice.invoiceId
    } else {
        newEntity.returnId = delegator.getNextSeqId('ReturnHeader')
    }

    result.returnId = newEntity.returnId

    boolean hasCreatePermission = security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin)
    if (!newEntity.entryDate || !hasCreatePermission) {
        newEntity.entryDate = nowTimestamp
    }
    if (!newEntity.statusId || !hasCreatePermission) {
        newEntity.statusId = returnHeaderTypeId == 'CUSTOMER_RETURN' ? 'RETURN_REQUESTED' : 'SUP_RETURN_REQUESTED'
    }
    newEntity.createdBy = userLogin.userLoginId
    newEntity.create()
    result.successMessage = "Return Request #[${newEntity.returnId}] was created successfully."
    return result
}

/**
 * Update a ReturnHeader
 */
Map updateReturnHeader() {
    Map result = success()
    GenericValue returnHeader = from('ReturnHeader').where(parameters).queryOne()
    // test the total return amount vs the total order amount
    if (parameters.statusId == 'RETURN_ACCEPTED') {
        // get the list of ReturnItems.  Note: return may be associated with many different orders
        List returnItems = from('ReturnItem').where(returnId: returnHeader.returnId).distinct().queryList()

        // this is used to make sure we don't return a negative amount
        BigDecimal returnTotalAmount = BigDecimal.ZERO

        // check them all to make sure that the return total does not exceed order total.
        for (GenericValue returnItem : returnItems) {
            if (!returnHeader.paymentMethodId && !parameters.paymentMethodId &&
                    (returnItem.returnTypeId == 'RTN_CSREPLACE' || returnItem.returnTypeId == 'RTN_REPAIR_REPLACE')) {
                return informError('OrderReturnPaymentMethodNeededForThisTypeOfReturn')
            }
            // tally up the return total amount
            returnTotalAmount += returnItem.returnPrice * returnItem.returnQuantity
            // compare return vs order total
            if (returnItem.orderId) {
                // no adjustment needed: adjustment is passed in to calculate
                // the effect of an additional item on return total.
                Map serviceResult = run service: 'getOrderAvailableReturnedTotal', with: [orderId: returnItem.orderId,
                                                                                          adjustment: BigDecimal.ZERO]
                BigDecimal availableReturnTotal = serviceResult.availableReturnTotal
                BigDecimal returnTotal = serviceResult.returnTotal
                BigDecimal orderTotal = serviceResult.returnTotal
                logInfo("Available amount for return on order # ${returnItem.orderId} is " +
                        "[${availableReturnTotal}] (orderTotal = [${orderTotal}] - returnTotal = [${returnTotal}]")

                if (availableReturnTotal < -0.01) {
                    return informError('OrderReturnPriceCannotExceedTheOrderTotal')
                }
            } else {
                logInfo('Not an order based returnItem; uable to check valid amounts!')
            }
        }
        // Checking that the Status change is Valid or Not
        if (parameters.statusId && parameters.statusId != returnHeader.statusId) {
            GenericValue statusValidChange = from('StatusValidChange')
                    .where(statusId: returnHeader.statusId, statusIdTo: parameters.statusId)
                    .cache()
                    .queryOne()
            if (!statusValidChange) {
                return informError('OrderErrorReturnHeaderItemStatusNotChangedIsNotAValidChange')
            }
        }
        List returnAdjustments = from('ReturnAdjustment').where(returnId: returnHeader.returnId).queryList()
        for (GenericValue returnAdjustment : returnAdjustments) {
            returnTotalAmount += returnAdjustment.amount
        }
        if (returnTotalAmount < 0) {
            return informError('OrderReturnTotalCannotLessThanZero')
        }
    }
    result.oldStatusId = returnHeader.statusId
    returnHeader.setNonPKFields(parameters)
    returnHeader.store()
    return result
}

/**
 * Create Return Item
 */
Map createReturnItem() {
    Map result = success()
    GenericValue orderItem
    GenericValue returnHeader = from('ReturnHeader')
            .where(returnId: parameters.returnId)
            .queryOne()

    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin)
            && returnHeader.fromPartyId != userLogin.partyId) {
        return informError('OrderSecurityErrorToRunCreateReturnItem')
    }
    if (!parameters.returnItemTypeId) {
        return informError('OrderReturnItemTypeIsNotDefined')
    }

    if (!returnHeader?.paymentMethodId && returnHeader.statusId == 'RETURN_ACCEPTED'
            && (parameters.returnTypeId == 'RTN_CSREPLACE' || parameters.returnTypeId == 'RTN_REPAIR_REPLACE')) {
        return informError('OrderReturnPaymentMethodNeededForThisTypeOfReturn')
    }
    if (parameters.returnQuantity == (BigDecimal.ZERO)) {
        return informError('OrderNoReturnQuantityAvailablePreviousReturnsMayExist')
    }

    // setup some default values for protection
    BigDecimal returnableQuantity = BigDecimal.ZERO
    BigDecimal returnablePrice = BigDecimal.ZERO

    // if an orderItemSeqId is provided, then find the corresponding orderItem
    if (parameters.orderItemSeqId) {
        Map itemLookup = makeValue('OrderItem')
        itemLookup.setPKFields(parameters)
        if (parameters.orderItemSeqId) {
            orderItem = from('OrderItem').where(itemLookup).queryOne()
            logInfo('Return item is an Order Item - ' + orderItem.orderItemSeqId)
        }
    }
    /*
     * get the returnableQuantity and returnablePrice:
     * for orderItems, it's found by getReturnableQuantity;
     * for adjustments, either order adjustments or manual adjustments, it's always 1 and based on input parameter
     */
    if (orderItem) {
        Map serviceResult = run service: 'getReturnableQuantity', with: [orderItem: orderItem]
        returnableQuantity = serviceResult.returnableQuantity ?: returnableQuantity
        returnablePrice = serviceResult.returnablePrice ?: returnablePrice
    }
    if (returnableQuantity > (BigDecimal.ZERO)) {
        // the user is only allowed to set a returnPrice if he has ORDERMGR_CREATE privilege,
        // otherwise only the returnablePrice calculated by service is used
        if (!security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin)) {
            returnablePrice = parameters.returnPrice
        }
        // make sure the returnQuantity is not greater than the returnableQuantity
        // from service or the quantity on the original orderItem
        if (parameters.returnQuantity > returnableQuantity) {
            return informError('OrderRequestedReturnQuantityNotAvailablePreviousReturnsMayExist')
        }
        if (orderItem && parameters.returnQuantity > orderItem.quantity) {
            return informError('OrderReturnQuantityCannotExceedTheOrderedQuantity')
        }
        if (parameters.returnPrice > returnablePrice) {
            return informError('OrderReturnPriceCannotExceedThePurchasePrice')
        }
    } else {
        logError("Order ${parameters.orderId} item ${parameters.orderItemSeqId} has been returned in full")
        return informError('OrderIllegalReturnItemTypePassed')
    }
    GenericValue newEntity = makeValue('ReturnItem')
    newEntity.returnId = parameters.returnId
    delegator.setNextSubSeqId(newEntity, 'returnItemSeqId', 5, 1)
    newEntity.setNonPKFields(parameters)
    newEntity.statusId = 'RETURN_REQUESTED' // default status for new return items
    result.returnItemSeqId = newEntity.returnItemSeqId
    newEntity.create()
    newEntity.refresh()

    if (orderItem && !parameters.includeAdjustments || parameters.includeAdjustments == 'Y') {
        // create return adjustments for all adjustments associated with the order item
        List orderAdjustments = delegator.getRelated('OrderAdjustment', null, null, orderItem, false)
        for (GenericValue orderAdjustment : orderAdjustments) {
            Map returnAdjCtx = [returnId: parameters.returnId,
                                returnItemSeqId: newEntity.returnItemSeqId,
                                returnTypeId: newEntity.returnTypeId,
                                orderAdjustmentId: orderAdjustment.orderAdjustmentId]
            run service: 'createReturnAdjustment', with: returnAdjCtx
        }
    }
    return result
}

/**
 * Update Return Item
 */
Map updateReturnItem() {
    Map result = success()
    Map lookupPKMap = [returnId: parameters.returnId, returnItemSeqId: parameters.returnItemSeqId]
    GenericValue returnItem = from('ReturnItem').where(lookupPKMap).queryOne()
    BigDecimal originalReturnPrice = returnItem.returnPrice
    BigDecimal originalReturnQuantity = returnItem.returnQuantity
    result.oldStatusId = returnItem.statusId

    returnItem.setNonPKFields(parameters)
    returnItem.store()
    returnItem.refresh()

    // now update all return adjustments associated with this return item
    List returnAdjustments = from('ReturnAdjustment')
            .where(returnId: returnItem.returnId,
            returnItemSeqId: returnItem.returnItemSeqId)
            .queryList()
    for (GenericValue returnAdjustment : returnAdjustments) {
        logInfo("updating returnAdjustment with Id:[${returnAdjustment.returnAdjustmentId}]")
        Map ctx = [:]
        ctx << returnAdjustment
        ctx.originalReturnPrice = originalReturnPrice
        ctx.originalReturnQuantity = originalReturnQuantity
        ctx.ReturnTypeId = returnItem.returnTypeId
        run service: 'updateReturnAdjustment', with: ctx
    }
    return result
}

/**
 * Update Return Items Status
 */
Map updateReturnItemsStatus() {
    List returnItems = from('ReturnItem').where(returnId: parameters.returnId).queryList()
    for (GenericValue item : returnItems) {
        item.statusId = parameters.statusId
        Map serviceInMap = [:]
        serviceInMap << item
        run service: 'updateReturnItem', with: serviceInMap
    }
    return success()
}

/**
 * Remove Return Item
 */
Map removeReturnItem() {
    GenericValue returnHeader = from('ReturnHeader').where(parameters).queryOne()
    if (returnHeader.returnHeaderTypeId == 'CUSTOMER_RETURN') {
        if ('RETURN_REQUESTED' != returnHeader.statusId) {
            return informError('OrderCannotRemoveItemsOnceReturnIsApproved')
        }
    } else {
        if ('SUP_RETURN_REQUESTED' != returnHeader.statusId) {
            return informError('OrderCannotRemoveItemsOnceReturnIsApproved')
        }
    }
    GenericValue returnItem = from('ReturnItem')
            .where(returnId: parameters.returnId,
            returnItemSeqId: parameters.returnItemSeqId)
            .queryOne()
    // remove related  adjustments
    List returnAdjustments = from('ReturnAdjustment')
            .where(returnItemSeqId: returnItem.returnItemSeqId,
            returnId: returnItem.returnId)
            .queryList()
    for (GenericValue returnAdjustment : returnAdjustments) {
        run service: 'removeReturnAdjustment', with: [returnAdjustmentId: returnAdjustment.returnAdjustmentId]
    }
    returnItem.remove()
    return success()
}

// note that this service is designed to be called once for each shipment receipt that is created

/**
 * Update Return Status From ShipmentReceipt
 */
Map updateReturnStatusFromReceipt() {
    Map result = success()
    Map lookupPKMap = [returnId: parameters.returnId]
    GenericValue returnHeader = from('ReturnHeader').where(lookupPKMap).queryOne()
    List shipmentReceipts = from('ShipmentReceipt').where(lookupPKMap).queryList()
    Map totalsMap = [:]
    for (GenericValue receipt : shipmentReceipts) {
        if (!totalsMap[receipt?.returnItemSeqId]) {
            totalsMap[receipt.returnItemSeqId] = BigDecimal.ZERO
        }
        totalsMap[receipt.returnItemSeqId] += receipt.quantityAccepted + receipt.quantityRejected
    }
    List returnItems = delegator.getRelated('ReturnItem', null, null, returnHeader, false)

    for (Map.Entry entry : totalsMap.entrySet()) {
        Map filterMap = [returnItemSeqId: entry.getKey()]
        Map item = EntityUtil.getFirst(EntityUtil.filterByAnd(returnItems, filterMap))
        item.receivedQuantity = entry.getValue()
        if (entry.getValue() >= item.returnQuantity) {
            // update the status for the item
            item.statusId = 'RETURN_RECEIVED'
        }
        Map serviceInMap = [:]
        serviceInMap << item
        // update the returnItem with at least receivedQuantity, and also statusId if applicable
        run service: 'updateReturnItem', with: serviceInMap
    }
    // check to see if all items have been received
    boolean allReceived = true
    List allReturnItems = from('ReturnItem').where(lookupPKMap).queryList()
    for (GenericValue item : allReturnItems) {
        if ('RETURN_RECEIVED' != item.statusId) {
            if (item.orderItemSeqId) {
                // non-order items (i.e. adjustments) are not received
                allReceived = false
            }
        }
    }

    // if the items are all received, then update the return header, store the status history change,
    // and set the shipment to received
    if (allReceived) {
        /*
         * Go through all the items yet again and set their shipment status to PURCH_SHIP_RECEIVED (if it isn't already)
         * This activates SECAS such as creating return invoices. This MUST be done before updating the return header so
         * that the ReturnItemBillings are created and then whatever SECA binds to the return header update will have them.
         */
        for (GenericValue receipt : shipmentReceipts) {
            GenericValue shipment = delegator.getRelatedOne('Shipment', receipt, false)
            if (shipment.shipmentId) {
                if ('RETURN_RECEIVED' != shipment.statusId) {
                    run service: 'updateShipment', with: [shipmentId: shipment.shipmentId,
                                                          statusId: 'PURCH_SHIP_RECEIVED']
                }
            }
        }
        // update the return header
        run service: 'updateReturnHeader', with: [statusId: 'RETURN_RECEIVED',
                                                  returnId: returnHeader.returnId]
    }
    result.returnHeaderStatus = returnHeader.statusId
    return result
}

/**
 * Create Quick Return From Order
 */
Map quickReturnFromOrder() {
    Map result = success()
    GenericValue returnItemTypeMapping

    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin)
            && !parameters.fromPartyId == userLogin.partyId) {
        return informError('OrderSecurityErrorToRunQuickReturnFromOrder')
    }

    // get primary information from the order header
    GenericValue orderHeader = from('OrderHeader').where(orderId: parameters.orderId).queryOne()
    String returnHeaderTypeId = parameters.returnHeaderTypeId
    String roleTypeId = returnHeaderTypeId == 'CUSTOMER_RETURN' ? 'BILL_TO_CUSTOMER' : 'BILL_FROM_VENDOR'

    // find the bill to customer; for return's fromPartyId
    GenericValue orderRole = from('OrderRole').where(orderId: orderHeader.orderId, roleTypeId: roleTypeId).queryFirst()
    // create the return header
    // changed from minilang: used createHeaderCtx instead of updateHeader on neesInventoryReceive
    Map createHeaderCtx = [destinationFacilityId: orderHeader.originFacilityId,
        needsInventoryReceive: 'Y',
        returnHeaderTypeId: returnHeaderTypeId]
    // get the return to party for customer return and return from party for vendor return from the product store
    GenericValue productStore = delegator.getRelatedOne('ProductStore', orderHeader, false)
    if (returnHeaderTypeId == 'CUSTOMER_RETURN') {
        createHeaderCtx.fromPartyId = orderRole.partyId
        createHeaderCtx.toPartyId = productStore.payToPartyId
        createHeaderCtx.destinationFacilityId = createHeaderCtx.destinationFacilityId ?: productStore.inventoryFacilityId
    } else {
        createHeaderCtx.fromPartyId = productStore.paytoPartyId
        createHeaderCtx.toPartyId = orderRole.partyId
    }
    // copy over the currency of the order to the currency of the return
    createHeaderCtx.currencyUomId = orderHeader.currencyUom
    Map serviceResult = run service: 'createReturnHeader', with: createHeaderCtx
    String returnId = serviceResult.returnId
    // get the available to return order items
    List orderItems = from('OrderItem').where(orderId: orderHeader.orderId, statusId: 'ITEM_COMPLETED').queryList()

    parameters.returnReasonId = parameters.returnReasonId ?: 'RTN_NOT_WANT'
    parameters.returnTypeId = parameters.returnTypeId ?: 'RTN_REFUND'
    // create the return items
    for (GenericValue orderItem : orderItems) {
        Map newItemCtx = [returnId: returnId,
            returnReasonId: parameters.returnReasonId,
            returnTypeId: parameters.returnTypeId]
        if (orderItem.productId) {
            newItemCtx.productId = orderItem.productId
        }
        newItemCtx.orderId = orderItem.orderId
        newItemCtx.orderItemSeqId = orderItem.orderItemSeqId
        newItemCtx.description = orderItem.itemDescription

        // get the returnable price and quantity
        Map serviceResultQuantity = run service: 'getReturnableQuantity', with: [orderItem: orderItem]
        newItemCtx.returnQuantity = serviceResultQuantity.returnableQuantity
        newItemCtx.returnPrice = serviceResultQuantity.returnablePrice

        // get the matching return item type from the order item type
        String orderItemTypeId = orderItem.orderItemTypeId
        if (orderItemTypeId == 'PRODUCT_ORDER_ITEM') {
            // Check if orderItemTypeId equals PRODUCT_ORDER_ITEM,
            // if so, use ProductType and ReturnItemTypeMap to get ReturnItemType
            String productTypeId = from('Product')
                    .where(productId: orderItem.productId)
                    .cache()
                    .getFieldList('productTypeId')
                    .first()
            returnItemTypeMapping = from('ReturnItemTypeMap')
                    .where([returnItemMapKey: productTypeId, returnHeaderTypeId: returnHeaderTypeId])
                    .queryOne()
        } else {
            // if not, try the ReturnItemTypeMap, but this may not actually work, so log a warning
            logWarning('Trying to find returnItemtype from ReturnItemTypeMap with orderItemtypeId' +
                    " [${orderItem.orderItemTypeId} for order item [${orderItem}]")
            returnItemTypeMapping = from('ReturnItemTypeMap')
                    .where([returnItemMapKey: orderItemTypeId, returnHeaderTypeId: returnHeaderTypeId])
                    .queryOne()
        }
        if (!returnItemTypeMapping.returnItemTypeId) {
            return informError('OrderReturnItemTypeOrderItemNoMatching')
        }
        newItemCtx.returnItemTypeId = returnItemTypeMapping.returnItemTypeId
        // create the return item
        if (newItemCtx.orderAdjustmentId) {
            logInfo("Found unexpected orderAdjustment: ${newItemCtx.orderAdjustmentId}")
            newItemCtx.orderAdjustmentId = null
        }
        if (newItemCtx.returnQuantity > (BigDecimal.ZERO)) {
            // otherwise, items which have been fully returned would still get passed in and then come back with an error
            run service: 'createReturnItem', with: newItemCtx
        } else {
            logInfo("This return item is not going to be created because its returnQuantity is zero: ${newItemCtx}")
        }
    }

    // create a return adjustment for all order adjustments not attached to a particular orderItem (orderItemSeqId = "_NA_")
    List orderAdjustments = from('OrderAdjustment')
            .where(orderId: orderHeader.orderId, orderItemSeqId: '_NA_')
            .queryList()
    for (GenericValue orderAdjustment : orderAdjustments) {
        Map returnAdjCtx = [:]
        returnAdjCtx.returnId = returnId
        // filter out orderAdjustment that have been returned
        if (from('ReturnAdjustment').where(orderAdjustmentId: orderAdjustment.orderAdjustmentId).queryCount() == 0) {
            logInfo('Create new return adjustment: ' + returnAdjCtx)
            run service: 'createReturnAdjustment', with: returnAdjCtx
        }
    }
    // very important: if countNewReturnItemx is not set,
    // getOrderAvailableReturnedTotal would not count the return items we just created
    Map orderAvailableCtx = [orderId: orderHeader.orderId, countNewReturnItemx: true]
    Map serviceResultART = run service: 'getOrderAvailableReturnedTotal', with: orderAvailableCtx
    BigDecimal availableReturnTotal = serviceResult.availableReturnTotal
    BigDecimal returnTotal = serviceResultART.returnTotal
    BigDecimal orderTotal = serviceResultART.orderTotal
    logInfo("OrderTotal [${orderTotal}] - ReturnTotal [${returnTotal}] = available Return Total [${}]")

    // create a manual balance adjustment based on the difference between order total and return total
    if (availableReturnTotal != (BigDecimal.ZERO)) {
        logWarning('Creating a balance adjustment of [' + availableReturnTotal + '] for return [' + returnId + ']')

        // create the balance adjustment return item
        run service: 'createReturnAdjustment', with: [returnId: returnId,
                                                      returnAdjustmentTypeId: 'RET_MAN_ADJ',
                                                      returnItemSeqId: '_NA_',
                                                      description: 'Balance Adjustment',
                                                      amount: availableReturnTotal]
    }
    // update the header status
    Map updateHeaderCtx = [returnId: returnId]
    updateHeaderCtx.statusId = returnHeaderTypeId == 'CUSTOMER_RETURN' ? 'RETURN_ACCEPTED' : 'SUP_RETURN_ACCEPTED'
    run service: 'updateReturnHeader', with: updateHeaderCtx

    if (returnHeaderTypeId == 'CUSTOMER_RETURN') {
        // auto-receive this return if we passed in the flag
        if (parameters.receiveReturn) {
            run service: 'quickReceiveReturn', with: [returnId: returnId]
        } else {
            // update the header status
            logInfo('Receive flag not set; will handle receiving on entity-sync')
        }
    }
    result.returnId = returnId
    return result
}

/**
 * If returnId is null, create a return; then create Return Item or Adjustment based on the parameters passed in
 */
Map createReturnAndItemOrAdjustment() {
    Map result = success()
    if (!parameters.returnId) {
        Map serviceResultcRH = run service: 'createReturnHeader', with: parameters
        if (!ServiceUtil.isSuccess(serviceResultcRH)) {
            return serviceResultcRH
        }
        parameters.returnId = serviceResultcRH.returnId
        result.returnId = serviceResultcRH.returnId
    }
    Map serviceResult = run service: 'createReturnItemOrAdjustment', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    result.returnAdjustmentId = serviceResult.returnAdjustmentId
    result.returnItemSeqId = serviceResult.returnItemSeqId
    return result
}

/**
 * Update a ReturnItems
 */
Map cancelReturnItems() {
    List returnItems = from('ReturnItem').where(returnId: parameters.returnId).distinct().queryList()
    for (GenericValue returnItem : returnItems) {
        run service: 'updateReturnItem', with: [returnId: parameters.returnId,
                                                returnItemSeqId: returnItem.returnItemSeqId,
                                                statusId: 'RETURN_CANCELLED']
    }
    return success()
}

/**
 * Cancel the associated OrderItems of the replacement order, if any.
 */
Map cancelReplacementOrderItems() {
    GenericValue returnItem = from('ReturnItems').where(parameters).queryOne()
    if (returnItem.returnTypeId == 'RTN_REPLACE'
            || returnItem.returnTypeId == 'RTN_CSREPLACE'
            || returnItem.returnTypeId == 'RTN_REPAIR_REPLACE') {
        // get the returned order item
        GenericValue orderItem = delegator.getRelatedOne('OrderItem', returnItem, false)
        // get the order items of the replacement order associated to the returned item
        Map oiaMap = [orderItemAssocTypeId: 'REPLACEMENT']
        List replacementOrderItems = delegator.getRelated('FromOrderItemAssoc', oiaMap, null, orderItem, false)
        for (GenericValue replacementOrderItem : replacementOrderItems) {
            run service: 'cancelOrderItem', with: [orderId: replacementOrderItem.toOrderId,
                                                   orderItemSeqId: replacementOrderItem.toOrderItemSeqId]
        }
    }
    return success()
}

/**
 * Process the replacements in a wait return
 */
Map processWaitReplacementReturn() {
    run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                    returnTypeId: 'RTN_REPLACE']
    return success()
}

/**
 * Process the replacements in a cross-ship return
 */
Map processCrossShipReplacementReturn() {
    run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                    returnTypeId: 'RTN_CSREPLACE']
    return success()
}

/**
 * Process the replacements in a repair return
 */
Map processRepairReplacementReturn() {
    run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                    returnTypeId: 'RTN_REPAIR_REPLACE']
    return success()
}

/**
 * Process the replacements in a wait reserved return when the return is accepted and then received
 */
Map processWaitReplacementReservedReturn() {
    GenericValue returnHeader = from('ReturnHeader').where(parameters).queryOne()
    if (returnHeader.statusId == 'RETURN_ACCEPTED') {
        run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                        returnTypeId: 'RTN_WAIT_REPLACE_RES']
    }
    if (returnHeader.statusId == 'RETURN_RECEIVED') {
        GenericValue returnItem = from('ReturnItem')
                .where(returnId: returnHeader.returnId,
                        returnTypeId: 'RTN_WAIT_REPLACE_RES')
                .queryFirst()
        if (returnItem) {
            // Get the replacement order and update its status to Approved
            GenericValue returnItemResponse = delegator.getRelatedOne('ReturnItemResponse', returnItem, false)
            GenericValue orderHeader = from('OrderHeader').where(orderId: returnItemResponse.replacementOrderId).queryOne()
            if (orderHeader) {
                if (orderHeader.statusId == 'ORDER_HOLD') {
                    run service: 'changeOrderStatus', with: [statusId: 'ORDER_APPROVED',
                                                             orderId: returnItemResponse.replacementOrderId,
                                                             setItemStatus: 'Y']
                }
                if (orderHeader.statusId == 'ORDER_CANCELLED') {
                    run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                                    returnTypeId: 'RTN_WAIT_REPLACE_RES']
                }
            }
        }
    }
    return success()
}

/**
 * Process the replacements in a immediate return
 */
Map processReplaceImmediatelyReturn() {
    run service: 'processReplacementReturn', with: [returnId: parameters.returnId,
                                                    returnTypeId: 'RTN_REPLACE_IMMEDIAT']
    return success()
}

/**
 * Process the refund in a return
 */
Map processRefundOnlyReturn() {
    run service: 'processRefundReturn', with: [returnId: parameters.returnId,
                                               returnTypeId: 'RTN_REFUND']
    return success()
}

/**
 * Process the Immediate refund in a return
 */
Map processRefundImmediatelyReturn() {
    run service: 'processRefundReturn', with: [returnId: parameters.returnId,
                                               returnTypeId: 'RTN_REFUND_IMMEDIATE']
    return success()
}

/**
 * Get the return status associated with customer vs. vendor return
 */
Map getStatusItemsForReturn() {
    Map result = success()
    if (parameters.returnHeaderTypeId == 'CUSTOMER_RETURN') {
        List statusItems = from('StatusItem').where(statusTypeId: 'ORDER_RETURN_STTS').queryList()
        result.statusItems = statusItems
    } else {
        List statusItems = from('StatusItem').where(statusTypeId: 'PORDER_RETURN_STTS').queryList()
        result.statusItems = statusItems
    }
    return result
}

/**
 * Associate exchange order with original order in OrderItemAssoc entity
 */
Map createExchangeOrderAssoc() {
    List returnItems = from('ReturnItem')
            .where(orderId: parameters.originOrderId,
            returnTypeId: 'RTN_REFUND')
            .queryList()
    Long returnItemSize = returnItems.size()
    List orderItems = from('OrderItem').where(orderId: parameters.orderId).queryList()
    Long orderItemSize = orderItems.size()
    if (returnItemSize > orderItemSize) {
        Long returnItemCounter = (Long) 1
        for (GenericValue returnItem : returnItems) {
            Map orderItemAssocMap = [orderId: parameters.originOrderId, orderItemSeqId: returnItem.orderItemSeqId]
            Long orderItemCounter = (Long) 1
            orderItems.each { GenericValue orderItem ->
                if (returnItemCounter == orderItemCounter) {
                    orderItemAssocMap.toOrderId = parameters.orderId
                    orderItemAssocMap.toOrderItemSeqId = orderItem.orderItemSeqId
                } else if (returnItemCounter > orderItemSize) {
                    orderItemAssocMap.toOrderId = parameters.orderId
                    orderItemAssocMap.toOrderItemSeqId = orderItem.orderItemSeqId
                }
                orderItemCounter++
            }
            orderItemAssocMap.shipGroupSeqId = '_NA_'
            orderItemAssocMap.toShipGroupSeqId = '_NA_'
            orderItemAssocMap.orderItemAssocTypeId = 'EXCHANGE'
            GenericValue orderItemAssoc = makeValue('OrderItemAssoc')
            orderItemAssoc.setPKFields(orderItemAssocMap)
            GenericValue orderItemAssocValue = from('OrderItemAssoc').where(orderItemAssoc).queryOne()
            if (!orderItemAssocValue) {
                orderItemAssoc.create()
                orderItemAssoc = null
            }
            returnItemCounter++
        }
    } else {
        Long orderItemCounter = (Long) 1
        for (GenericValue orderItem : orderItems) {
            Map orderItemAssocMap = [toOrderId: parameters.orderId, toOrderItemSeqId: orderItem.orderItemSeqId]
            Long returnItemCounter = (Long) 1
            returnItems.each { GenericValue returnItem ->
                if (orderItemCounter == returnItemCounter) {
                    orderItemAssocMap.orderId = parameters.originOrderId
                    orderItemAssocMap.orderItemSeqId = returnItem.orderItemSeqId
                } else if (orderItemCounter > returnItemSize) {
                    orderItemAssocMap.orderId = parameters.originOrderId
                    orderItemAssocMap.orderItemSeqId = returnItem.orderItemSeqId
                }
                returnItemCounter++
            }
            orderItemAssocMap.shipGroupSeqId = '_NA_'
            orderItemAssocMap.toShipGroupSeqId = '_NA_'
            orderItemAssocMap.orderItemAssocTypeId = 'EXCHANGE'
            GenericValue orderItemAssoc = makeValue('OrderItemAsscoc')
            orderItemAssoc.setPKFields(orderItemAssocMap)
            GenericValue orderItemAssocValue = from('OrderItemAssoc').where(orderItemAssoc).queryOne()
            if (!orderItemAssocValue) {
                orderItemAssoc.create()
                orderItemAssocMap = null
            }
            orderItemCounter++
        }
    }
    return success()
}

/**
 * When one or more product is received directly through receive inventory or
 * refund return then add these product(s) back to category,
 * if they does not have any active category
 */
Map addProductsBackToCategory() {
    if (parameters.inventoryItemId) {
        GenericValue inventoryItem = from('InventoryItem').where(parameters).queryOne()
        GenericValue product = delegator.getRelatedOne('Product', inventoryItem, false)
        List productCategoryMembers = delegator.getRelated('ProductCategoryMember',
                null, ['-thruDate'], product, false)
        // check whether this product is associated to any category, if not just skip
        if (productCategoryMembers) {
            List pcms = EntityUtil.filterByDate(productCategoryMembers)
            // check if this product is associated to any active category,
            // if not found then activate the most recent inactive category
            if (!pcms) {
                Map pcm = [:]
                pcm << productCategoryMembers.get(0)
                pcm.thruDate = null
                run service: 'updateProductToCategory', with: pcm
            }
        }
    } else {
        if (parameters.returnId) {
            List returnItems = from('ReturnItem')
                    .where(returnId: parameters.returnId,
                    returnTypeId: 'RTN_REFUND')
                    .queryList()
            if (returnItems) {
                for (GenericValue returnItem : returnItems) {
                    GenericValue product = delegator.getRelatedOne('Product', returnItem, false)
                    List productCategoryMembers = delegator.getRelated('ProductCategoryMember',
                            null, ['-thruDate'], product, false)
                    // check whether this product is associated to any category, if not just skip
                    if (productCategoryMembers) {
                        List pcms = EntityUtil.filterByDate(productCategoryMembers)
                        // check if this product is associated to any active category,
                        // if not found then activate the most recent inactive category
                        if (!pcms) {
                            Map pcm = [:]
                            pcm << productCategoryMembers.get(0)
                            pcm.thruDate = null
                            run service: 'updateProductToCategory', with: pcm
                        }
                    }
                }
            }
        }
    }
    return success()
}

/**
 * Create ReturnHeader and ReturnItem Status
 */
Map createReturnStatus() {
    GenericValue newEntity = makeValue('ReturnStatus')
    if (parameters.returnItemSeqId) {
        GenericValue returnItem = from('ReturnItem').where(parameters).queryOne()
        newEntity.returnItemSeqId = returnItem.returnItemSeqId
        newEntity.statusId = returnItem.statusId
    } else {
        GenericValue returnHeader = from('ReturnHeader').where(parameters).queryOne()
        newEntity.statusId = returnHeader.statusId
    }
    newEntity.returnStatusId = delegator.getNextSeqId('ReturnStatus')
    newEntity.returnId = parameters.returnId
    newEntity.changeByUserLoginId = userLogin.userLoginId
    newEntity.statusDatetime = UtilDateTime.nowTimestamp()
    newEntity.create()
    return success()
}

/**
 * Update ReturnContactMech
 */
Map updateReturnContactMech() {
    GenericValue returnContactMechMap = makeValue('ReturnContactMech')
    returnContactMechMap.setPKFields(parameters)
    GenericValue returnHeader = from('ReturnHeader').where(parameters).queryOne()
    Map createReturnContactMechMap = [returnId: parameters.returnId,
        contactMechPurposeTypeId: parameters.contactMechPurposeTypeId,
        contactMechId: parameters.contactMechId]
    List returnContactMechList = from('ReturnContactMech').where(createReturnContactMechMap).queryList()
    // If returnContactMechList value is null then create new entry in ReturnContactMech entity
    if (!returnContactMechList) {
        if (parameters.contactMechPurposeTypeId == 'SHIPPING_LOCATION') {
            returnHeader.originContactMechId = createReturnContactMechMap.contactMechId
            returnHeader.store()
        }
        run service: 'createReturnContactMech', with: createReturnContactMechMap
    }
    returnContactMechMap.store()
    return success()
}

/**
 * Create the return item for rental (which items has product type is ASSET_USAGE_OUT_IN)
 */
Map createReturnItemForRental() {
    Map result = success()
    GenericValue orderHeader = from('OrderHeader').where(orderId: parameters.orderId).queryOne()

    if (orderHeader.orderTypeId == 'SALES_ORDER') {
        GenericValue orderRole = from('OrderRole')
                .where(orderId: orderHeader.orderId,
                roleTypeId: 'BILL_TO_CUSTOMER')
                .queryFirst()
        GenericValue productStore = delegator.getRelatedOne('ProductStore', orderHeader, false)

        Map createReturnCtx = [:]
        if (productStore?.inventoryFacilityId) {
            createReturnCtx.destinationFacilityId = productStore.inventoryFacilityId
        }

        /*
         * changed from minilang since there seems to be no purpose
         *
         * if (productStore?.reqReturnInventoryReceive) {
         *     updateHeaderCtx.needsInventoryReceive = productStore.reqReturnInventoryReceive
         * } else {
         *     updateHeaderCtx.needsInventoryReceive = "N"
         * }
         */

        createReturnCtx << [
                orderId: orderHeader.orderId,
                currencyUomId: orderHeader.currencyUom,
                fromPartyId: orderRole.partyId,
                toPartyId: productStore.payToPartyId,
                returnHeaderTypeId: 'CUSTOMER_RETURN',
                returnReasonId: 'RTN_NORMAL_RETURN',
                returnTypeId: 'RTN_RENTAL',
                returnItemTypeId: 'RET_FDPROD_ITEM',
                expectedItemStatus: 'INV_RETURNED',
                returnPrice: BigDecimal.ZERO,
        ]

        List orderItems = from('OrderItemAndProduct')
                .where(orderId: orderHeader.orderId,
                statusId: 'ITEM_COMPLETED',
                productTypeId: 'ASSET_USAGE_OUT_IN')
                .queryList()
        for (GenericValue orderItem : orderItems) {
            createReturnCtx.productId = orderItem.productId
            createReturnCtx.orderItemSeqId = orderItem.orderItemSeqId
            createReturnCtx.description = orderItem.itemDescription
            createReturnCtx.returnQuantity = orderItem.quantity
            Map serviceResult = run service: 'createReturnAndItemOrAdjustment', with: createReturnCtx
            String returnId = serviceResult.returnId
            if (returnId) {
                createReturnCtx.returnId = returnId
                // changed from minilang: added returnId to result since it's a required outgoing parameter
                result.returnId = returnId
            }
        }
    }
    return result
}

private Map informError(String label) {
    String errorMessage = UtilProperties.getMessage('OrderErrorUiLabels', label, parameters.locale)
    logError(errorMessage)
    return error(errorMessage)
}
