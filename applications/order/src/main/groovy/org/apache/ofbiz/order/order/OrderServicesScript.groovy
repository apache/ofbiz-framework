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

import org.apache.ofbiz.base.util.GeneralException
import org.apache.ofbiz.base.util.ObjectType
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.order.customer.CheckoutMapProcs
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem

import java.sql.Timestamp

/**
 * Service to create OrderHeader
 */
Map createOrderHeader() {
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (!(security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin))) {
        return error(label('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateOrderShipment'))
    }

    GenericValue orderHeader = makeValue('OrderHeader',
            [orderId: parameters.orderId ?: delegator.getNextSeqId('OrderHeader')])
    orderHeader.with {
        setNonPKFields(parameters)
        statusId = orderHeader.statusId ?: 'ORDER_CREATED'
        orderDate = orderHeader.orderDate ?: nowTimestamp
        entryDate = orderHeader.entryDate ?: nowTimestamp
        create()
    }
    return success([orderId: orderHeader.orderId])
}

/**
 * Service to get the next OrderId
 */
Map getNextOrderId() {
    GenericValue partyAcctgPreference
    GenericValue customMethod
    String customMethodName

    partyAcctgPreference = from('PartyAcctgPreference').where(context).queryOne()
    logInfo "In getNextOrderId partyId is [$parameters.partyId], partyAcctgPreference: $partyAcctgPreference"

    if (partyAcctgPreference) {
        customMethod = partyAcctgPreference.getRelatedOne('OrderCustomMethod', true)
    } else {
        logWarning "Acctg preference not defined for partyId [$parameters.partyId]"
    }

    if (customMethod) {
        customMethodName = customMethod.customMethodName
    }

    String orderIdTemp
    if (customMethodName) {
        parameters.partyAcctgPreference = partyAcctgPreference
        Map serviceResult = run service: customMethodName, with: parameters
        orderIdTemp = serviceResult.orderId
    } else {
        logInfo 'In getNextOrderId sequence by Standard'
        // default to the default sequencing: ODRSQ_STANDARD
        orderIdTemp = parameters.orderId ?: delegator.getNextSeqId('OrderHeader')
    }

    GenericValue productStore = null
    if (parameters.productStoreId) {
        productStore = from('ProductStore').where(context).queryOne()
    }

    // use orderIdTemp along with the orderIdPrefix to create the real ID
    String orderId = "${productStore?.orderNumberPrefix ?: ''}${partyAcctgPreference?.orderIdPrefix ?: ''}${orderIdTemp as String}"

    return success([orderId: orderId])
}

/**
 * Service to get Summary Information About Orders for a Customer
 */
Map getOrderedSummaryInformation() {
    /*
    // The permission checking is commented out to make this service work also when triggered from ecommerce
    if (!security.hasEntityPermission('ORDERMGR', '_VIEW', session && !parameters.partyId.equals(userLogin.partyId))) {
        Map result = error('To get order summary information you must have the ORDERMGR_VIEW permission, or
        be logged in as the party to get the summary information for.')
        return result
    }
    */
    Timestamp fromDate = null, thruDate = null
    Timestamp now = UtilDateTime.nowTimestamp()
    Integer monthsToInclude = parameters.monthsToInclude
    if (monthsToInclude) {
        thruDate = now
        fromDate = UtilDateTime.adjustTimestamp(now, Calendar.MONTH, -monthsToInclude)
    }

    String roleTypeId = parameters.roleTypeId ?: 'PLACING_CUSTOMER'
    String orderTypeId = parameters.orderTypeId ?: 'SALES_ORDER'
    String statusId = parameters.statusId ?: 'ORDER_COMPLETED'

    //find the existing exchange rates
    EntityConditionBuilder exprBldr = new EntityConditionBuilder()

    EntityCondition condition = exprBldr.AND {
        EQUALS(partyId: partyId)
        EQUALS(roleTypeId: roleTypeId)
        EQUALS(orderTypeId: orderTypeId)
        EQUALS(statusId: statusId)
    }

    if (fromDate) {
        condition = exprBldr.AND(condition) {
            condition
            exprBldr.OR {
                GREATER_THAN_EQUAL_TO(orderDate: fromDate)
                EQUALS(orderDate: null)
            }
        }
    }

    if (thruDate) {
        condition = exprBldr.AND(condition) {
            condition
            exprBldr.OR {
                LESS_THAN_EQUAL_TO(orderDate: thruDate)
                EQUALS(orderDate: null)
            }
        }
    }

    GenericValue orderInfo = select('partyId', 'roleTypeId', 'totalGrandAmount', 'totalSubRemainingAmount', 'totalOrders')
            .from('OrderHeaderAndRoleSummary').where(condition).queryFirst()

    // first set the required OUT fields to zero
    return success([
            totalGrandAmount: orderInfo ? orderInfo.totalGrandAmount : BigDecimal.ZERO,
            totalSubRemainingAmount: orderInfo ? orderInfo.totalSubRemainingAmount : BigDecimal.ZERO,
            totalOrders: orderInfo ? orderInfo.totalOrders : 0L])
}

/**
 * Service to get enforced Sequence (no gaps, per organization)
 */
Map orderSequence_enforced() {
    logInfo 'In getNextOrderId sequence enum Enforced'
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    // this is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    partyAcctgPreference.lastOrderNumber = partyAcctgPreference.lastOrderNumber
            ? partyAcctgPreference.lastOrderNumber + 1
            : 1

    partyAcctgPreference.store()
    return success([orderId: partyAcctgPreference.lastOrderNumber])
}

/**
 * Service to automatically create OrderAdjustments
 */
Map recreateOrderAdjustments() {
    GenericValue order = from('OrderHeader').where(context).queryOne()

    // All existing promo order items are cancelled
    List<GenericValue> orderItems = order.getRelated('OrderItem', null, null, false)
    for (GenericValue orderItem : orderItems) {
        if (orderItem.isPromo == 'Y' && orderItem.statusId != 'ITEM_CANCELLED') {
            run service: 'cancelOrderItemNoActions', with: [*: parameters,
                                                            orderItemSeqId: orderItem.orderItemSeqId]
        }
    }

    List<GenericValue> orderAdjustments = order.getRelated('OrderAdjustment', null, null, false)
    // Accumulate the total existing promotional adjustment
    BigDecimal existingOrderAdjustmentTotal = BigDecimal.ZERO
    for (GenericValue orderAdjustment : orderAdjustments) {
        if (orderAdjustment.orderAdjustmentTypeId == 'PROMOTION_ADJUSTMENT' ) {
            existingOrderAdjustmentTotal = existingOrderAdjustmentTotal.add(orderAdjustment.getBigDecimal('amount').setScale(3))
        }
    }

    // Recalculate the promotions for the order
    Map<String, Object> serviceCtx = [*: parameters,
        skipInventoryChecks: true,
        skipProductChecks: true]
    Map<String, Object> loadCartFromOrderInMap = dispatcher.runSync('loadCartFromOrder', serviceCtx)
    ShoppingCart cart = loadCartFromOrderInMap.shoppingCart
    List<ShoppingCartItem> items = cart.items()
    for (ShoppingCartItem item : items) {
        String orderItemSeqId = item.getOrderItemSeqId()
        if (!orderItemSeqId) {
            // this is a new (promo) item
            // a new order item is created
            GenericValue newOrderItem = makeValue('OrderItem')
            newOrderItem.with {
                orderId = parameters.orderId
                orderItemTypeId = item.getItemType()
                selectedAmount = item.getSelectedAmount()
                unitPrice = item.getBasePrice()
                unitListPrice = item.getListPrice()
                itemDescription = item.getName(dispatcher)
                statusId = item.getStatusId()
                productId = item.getProductId()
                quantity = item.getQuantity()
                isModifiedPrice = 'N'
                isPromo = 'Y'
                statusId = newOrderItem.statusId ?: 'ITEM_CREATED'
            }
            newOrderItem.orderItemSeqId = delegator.getNextSeqId('OrderItem')
            newOrderItem.create()
            // And the orderItemSeqId is assigned to the shopping cart item
            item.setOrderItemSeqId(newOrderItem.orderItemSeqId)
        }
    }
    List<GenericValue> adjustments = cart.makeAllAdjustments()

    // Accumulate the new promotion total from the recalculated promotion adjustments
    BigDecimal newOrderAdjustmentTotal = BigDecimal.ZERO
    for (GenericValue adjustment : adjustments) {
        if (adjustment.productPromoId && !adjustment.orderAdjustmentId) {
            newOrderAdjustmentTotal = newOrderAdjustmentTotal.add(adjustment.getBigDecimal('amount').setScale(3))
        }
    }

    // Determine the difference between existing and new promotion adjustment totals, if any
    BigDecimal orderAdjustmentTotalDifference = newOrderAdjustmentTotal.subtract(existingOrderAdjustmentTotal)

    // If the total has changed, create an OrderAdjustment to reflect the fact
    if (orderAdjustmentTotalDifference != 0) {
        run service: 'createOrderAdjustment',
            with: [orderAdjustmentTypeId: 'PROMOTION_ADJUSTMENT',
                   orderId: parameters.orderId,
                   orderItemSeqId: '_NA_',
                   shipGroupSeqId: '_NA_',
                   description: 'Adjustment due to order change',
                   amount: orderAdjustmentTotalDifference
            ]
    }
    return success()
}

/*
 * Update OrderContactMech
 */
Map updateOrderContactMech() {
    if (!(security.hasEntityPermission('ORDERMGR', '_UPDATE', parameters.userLogin))) {
        return error(label('OrderErrorUiLabels', 'OrderSecurityErrorToRunUpdateOrderContactMech'))
    }

    if (parameters.contactMechPurposeTypeId == 'SHIPPING_LOCATION' &&
            parameters.contactMechId != parameters.oldContactMechId) {
        Map orderItemShipGroupMap = [orderId: parameters.orderId]
        if (parameters.oldContactMechId) {
            orderItemShipGroupMap.contactMechId = parameters.oldContactMechId
        }
        List<GenericValue> shipGroupList = from('OrderItemShipGroup')
            .where(orderItemShipGroupMap)
            .queryList()
        if (shipGroupList) {
            for (GenericValue shipGroup: shipGroupList) {
                run service: 'updateOrderItemShipGroup', with: [
                    orderId: parameters.orderId,
                    contactMechId: parameters.contactMechId,
                    contactMechPurposeTypeId: parameters.contactMechPurposeTypeId,
                    shipGroupSeqId: shipGroup.shipGroupSeqId,
                    shipmentMethod: "${shipGroup.shipmentMethodTypeId}@${shipGroup.carrierPartyId}@${shipGroup.carrierRoleTypeId}",
                    oldContactMechId: parameters.oldContactMechId]
            }
        }
    } else {
        List<GenericValue> orderContactMechList = from('OrderContactMech')
            .where(orderId: parameters.orderId,
                   contactMechPurposeTypeId: parameters.contactMechPurposeTypeId,
                   contactMechId: parameters.contactMechId)
            .queryList()
        // If orderContactMechList value is null then create new entry in OrderContactMech entity
        if (!orderContactMechList) {
            run service: 'createOrderContactMech', with: parameters
            if (parameters.oldContactMechId) {
                run service: 'removeOrderContactMech', with: [
                    orderId: parameters.orderId,
                    contactMechId: parameters.oldContactMechId,
                    contactMechPurposeTypeId: parameters.contactMechPurposeTypeId]
            }
        }
    }
    return success()
}

/*
 * Update OrderItemShipGroup
 */
Map updateOrderItemShipGroup() {
    if (!(security.hasEntityPermission('ORDERMGR', '_UPDATE', parameters.userLogin))) {
        return error(label('OrderErrorUiLabels', 'OrderSecurityErrorToRunUpdateOrderItemShipGroup'))
    }
    GenericValue lookedUpValue =  from('OrderItemShipGroup')
            .where(parameters)
            .queryOne()

    // splitting shipmentMethod request parameter value that contains '@' symbol into
    // 'shipmentMethodTypeId', 'carrierPartyId' and 'carrierRoleTypeId'.
    String shipmentMethod = parameters.shipmentMethod
    if (shipmentMethod != null) {
        String[] arr = shipmentMethod.split( '@' )
        parameters.put('shipmentMethodTypeId', arr[0])
        parameters.put('carrierPartyId', arr[1])
        parameters.put('carrierRoleTypeId', arr[2])
    }
    lookedUpValue.setNonPKFields(parameters)

    Map inputMap = [orderId: parameters.orderId,
                    contactMechPurposeTypeId: parameters.contactMechPurposeTypeId]
    if (parameters.contactMechId) {
        inputMap.contactMechId = parameters.contactMechId
    }
    List orderContactMechList = from('OrderContactMech')
        .where(inputMap)
        .queryList()
    // If orderContactMechList value is null then create new entry in OrderContactMech entity
    if (!orderContactMechList && parameters.contactMechId) {
        run service: 'createOrderContactMech', with: [*: inputMap]
    }
    lookedUpValue.store()

    // Remove the old values from OrderContactMech entity with the help of oldContactMechId
    Map shipGroupLookupMap = [orderId: parameters.orderId]
    if (parameters.oldContactMechId) {
        shipGroupLookupMap.contactMechId =  parameters.oldContactMechId
    }
    List<GenericValue> orderItemShipGroupList = from('OrderItemShipGroup')
        .where(shipGroupLookupMap)
        .queryList()
    if (!orderItemShipGroupList) {
        inputMap.contactMechId = parameters.oldContactMechId
        run service: 'removeOrderContactMech', with: [*: inputMap]
    }

    // Update promisedDateTime & currentPromisedDate in OrderItemShipGrpInvRes entity
    List itemShipGrpInvResList = from('OrderItemShipGrpInvRes')
        .where('orderId', parameters.orderId,
               'shipGroupSeqId', parameters.shipGroupSeqId)
        .queryList()
    if (itemShipGrpInvResList) {
        for (GenericValue orderItemShipGrpInvRes: itemShipGrpInvResList) {
            orderItemShipGrpInvRes.promisedDatetime = parameters.shipByDate
            orderItemShipGrpInvRes.currentPromisedDate = parameters.shipByDate
            orderItemShipGrpInvRes.store()
        }
    }
    return success()
}

/*
 * Compute and return the OrderItemShipGroup estimated ship date based on the associated items.
 */
Map getOrderItemShipGroupEstimatedShipDate() {
    GenericValue orderItemShipGroup = from('OrderItemShipGroup')
        .where('orderId', parameters.orderId,
               'contactMechId', parameters.oldContactMechId)
        .queryFirst()
    GenericValue orderItemShipGroupInvRes =  from('OrderItemShipGrpInvRes')
        .where('orderId', parameters.orderId,
               'shipGroupSeqId', parameters.shipGroupSeqId)
        .orderBy((orderItemShipGroup.maySplit == 'Y' ? '+' : '-') + 'promisedDatetime')
        .queryFirst()
    return success(estimatedShipDate: orderItemShipGroupInvRes.promisedDatetime)
}

/*
 * Create a PaymentMethodToOrder
 */
Map addPaymentMethodToOrder() {
    if (!(security.hasEntityPermission('ORDERMGR', '_CREATE', parameters.userLogin))) {
        return error(label('OrderErrorUiLabels', 'OrderSecurityErrorToRunAddPaymentMethodToOrder'))
    }
    GenericValue paymentMethod = from('PaymentMethod')
        .where('paymentMethodId', parameters.paymentMethodId)
        .queryOne()

    // In this method we calls createOrderPaymentPreference and returns orderPaymentPreferenceId field to authOrderPaymentPreference
    return (run service: 'createOrderPaymentPreference', with: [
            orderId: parameters.orderId,
            maxAmount: parameters.maxAmount,
            paymentMethodId: parameters.paymentMethodId,
            paymentMethodTypeId: paymentMethod.paymentMethodTypeId])
}

/*
 * Gets an order status
 */
Map getOrderStatus() {
    GenericValue order = from('OrderHeader').where(parameters).queryOne()
    return order
            ? success(statusId: order.statusId)
            : error(label('OrderErrorUiLabels', 'OrderOrderIdDoesNotExists'))
}

/*
 * Check if an Order is on Back Order
 */
Map checkOrderIsOnBackOrder() {
    EntityCondition condition = new EntityConditionBuilder().AND {
            EQUALS(orderId: parameters.orderId)
            NOT_EQUAL(quantityNotAvailable: null)
            GREATER_THAN_EQUAL_TO(quantityNotAvailable: BigDecimal.ZERO)
        }
    return success([isBackOrder: from('OrderItemShipGrpInvRes')
            .where(condition)
            .queryCount() > 0])
}

/*
 * Creates a new Order Item Change record
 */
Map createOrderItemChange() {
    GenericValue newEntity = makeValue('OrderItemChange',
            [*: parameters,
             orderItemChangeId: delegator.getNextSeqId('OrderItemChange'),
             changeDatetime: parameters.changeDatetime ?: UtilDateTime.nowTimestamp(),
             changeUserLogin: parameters.changeUserLogin ?: userLogin.userLoginId])
    newEntity.create()
    return success([orderItemChangeId: newEntity.orderItemChangeId])
}

/*
 * Create and update a Shipping Address
 */
Map createUpdateShippingAddress() {
    String contactMechId = parameters.shipToContactMechId
    String keepAddressBook = parameters.keepAddressBook ?: 'Y'
    // Call map Processor
    Map shipToAddressCtx = CheckoutMapProcs.shipToAddress(parameters)
    String partyId = parameters.partyId
    shipToAddressCtx.partyId = partyId

    if (!contactMechId) {
        shipToAddressCtx.contactMechPurposeTypeId = 'SHIPPING_LOCATION'
        Map serviceResult = run service: 'createPartyPostalAddress', with: shipToAddressCtx
        parameters.shipToContactMechId = serviceResult.contactMechId
        logInfo("Shipping address created with contactMechId ${parameters.shipToContactMechId}")
    } else if (keepAddressBook == 'Y') {
        GenericValue newValue = makeValue('PostalAddress', shipToAddressCtx)
        GenericValue oldValue = from('PostalAddress').where(parameters).queryOne()
        if (newValue != oldValue) {
            shipToAddressCtx.contactMechId = null
            Map serviceResult = run service: 'createPartyPostalAddress', with: shipToAddressCtx
            parameters.shipToContactMechId = serviceResult.contactMechId
        }

        List<GenericValue> pcmpShipList = from('PartyContactMechPurpose')
            .where(partyId: partyId,
                   contactMechId: parameters.shipToContactMechId,
                   contactMechPurposeTypeId: 'SHIPPING_LOCATION')
            .filterByDate()
            .queryList()
        // If purpose does not exists then create
        if (!pcmpShipList) {
            List<GenericValue> pcmpList = from('PartyContactMechPurpose')
                .where(partyId: partyId,
                       contactMechPurposeTypeId: 'SHIPPING_LOCATION')
                .filterByDate()
                .queryList()
            for (GenericValue pcmp: pcmpList) {
                run service: 'expirePartyContactMechPurpose', with: pcmp.getAllFields()
            }
            run service: 'createPartyContactMechPurpose', with: [*: parameters,
                                                                 partyId: partyId,
                                                                 contactMechId: parameters.shipToContactMechId,
                                                                 contactMechPurposeTypeId: 'SHIPPING_LOCATION']
        }
        if (parameters.setDefaultShipping == 'Y') {
            run service: 'setPartyProfileDefaults', with: [partyId: partyId,
                                                           productStoreId: parameters.productStoreId,
                                                           defaultShipAddr: parameters.shipToContactMechId]
        }
    } else {
        shipToAddressCtx.shipToContactMechId = shipToAddressCtx.contactMechId
        if (shipToAddressCtx.shipToContactMechId == parameters.billToContactMechId) {
            GenericValue newValue = makeValue('PostalAddress', shipToAddressCtx)
            GenericValue oldValue = from('PostalAddress').where(parameters).queryOne()
            if (newValue != oldValue) {
                List<GenericValue> pcmpShipList = from('PartyContactMechPurpose')
                    .where(partyId: partyId,
                           contactMechId: shipToAddressCtx.shipToContactMechId,
                           contactMechPurposeTypeId: 'SHIPPING_LOCATION')
                    .filterByDate()
                    .queryList()
                for (GenericValue pcmp: pcmpShipList) {
                    run service: 'expirePartyContactMechPurpose', with: pcmp.getAllFields()
                }
                Map serviceResult = run service: 'createPartyPostalAddress', with: [*: shipToAddressCtx,
                                                                                    partyId: partyId,
                                                                                    contactMechId: null,
                                                                                    contactMechPurposeTypeId: 'SHIPPING_LOCATION']
                parameters.shipToContactMechId = serviceResult.contactMechId
                logInfo("Shipping address updated with contactMechId ${shipToAddressCtx.shipToContactMechId}")
            }
        } else {
            shipToAddressCtx.userLogin = parameters.userLogin
            Map serviceResult = run service: 'updatePartyPostalAddress', with: shipToAddressCtx
            parameters.shipToContactMechId = serviceResult.contactMechId
            logInfo("Shipping address updated with contactMechId ${shipToAddressCtx.shipToContactMechId}")
        }
    }
    return success([contactMechId: parameters.shipToContactMechId])
}

/*
 * Create and update Billing Address
 */
Map createUpdateBillingAddress() {
    String keepAddressBook = parameters.keepAddressBook ?: 'Y'
    Map billToAddressCtx = [:]
    if (parameters.useShippingAddressForBilling != 'Y') {
        // Call map Processor
        billToAddressCtx = CheckoutMapProcs.billToAddress(parameters)
    }
    String partyId = parameters.partyId
    billToAddressCtx.partyId = partyId

    if (parameters.useShippingAddressForBilling == 'Y') {
        if (parameters.billToContactMechId) {
            if (parameters.shipToContactMechId != parameters.billToContactMechId) {
                List<GenericValue> pcmpList = from('PartyContactMechPurpose')
                    .where(partyId: partyId,
                           contactMechId: parameters.billToContactMechId,
                           contactMechPurposeTypeId: 'BILLING_LOCATION')
                    .filterByDate()
                    .queryList()
                for (GenericValue pcmp: pcmpList) {
                    run service: 'deletePartyContactMech', with: pcmp.getAllFields()
                }
                if (keepAddressBook == 'N') {
                    run service: 'createPartyContactMechPurpose', with: [contactMechId: parameters.billToContactMechId]
                }
                // Check that the ship-to address doesn't already have a bill-to purpose
                pcmpList = from('PartyContactMechPurpose')
                    .where(partyId: partyId,
                           contactMechId: parameters.shipToContactMechId,
                           contactMechPurposeTypeId: 'BILLING_LOCATION')
                    .filterByDate()
                    .queryList()
                if (!pcmpList) {
                    Map serviceContext = [*: parameters,
                                          partyId: partyId,
                                          contactMechId: parameters.shipToContactMechId,
                                          contactMechPurposeTypeId: 'BILLING_LOCATION']
                    run service: 'createPartyContactMechPurpose', with: serviceContext
                }
                logInfo("Billing address updated with contactMechId ${parameters.billToContactMechId}")
            }
        } else {
            Map serviceContext = [*: parameters,
                                  partyId: partyId,
                                  contactMechId: parameters.shipToContactMechId,
                                  contactMechPurposeTypeId: 'BILLING_LOCATION']
            run service: 'createPartyContactMechPurpose', with: serviceContext
        }
        parameters.billToContactMechId = parameters.shipToContactMechId
    } else {
        if (parameters.billToContactMechId) {
            if (parameters.shipToContactMechId == parameters.billToContactMechId) {
                Map serviceResult = run service: 'createPartyPostalAddress', with: [*: billToAddressCtx,
                                                                                    contactMechId: null,
                                                                                    contactMechPurposeTypeId: 'BILLING_LOCATION']
                parameters.billToContactMechId = serviceResult.contactMechId

                List<GenericValue> pcmpList = from('PartyContactMechPurpose')
                    .where(partyId: partyId,
                           contactMechPurposeTypeId: 'BILLING_LOCATION')
                    .filterByDate()
                    .queryList()
                for (GenericValue pcmp: pcmpList) {
                    run service: 'expirePartyContactMechPurpose', with: pcmp.getAllFields()
                }
                serviceContext = [*: parameters,
                                  partyId: partyId,
                                  contactMechId: parameters.billToContactMechId,
                                  contactMechPurposeTypeId: 'BILLING_LOCATION']
                run service: 'createPartyContactMechPurpose', with: serviceContext
                logInfo("Billing address updated with contactMechId ${parameters.billToContactMechId}")
            } else {
                if (keepAddressBook == 'N') {
                    billToAddressCtx.userLogin = parameters.userLogin
                    Map serviceResult = run service: 'updatePartyPostalAddress', with: billToAddressCtx
                    parameters.billToContactMechId = serviceResult.contactMechId
                } else if (keepAddressBook == 'Y') {
                    GenericValue newValue = makeValue('PostalAddress', billToAddressCtx)
                    GenericValue oldValue = from('PostalAddress').where(parameters).queryOne()
                    if (newValue != oldValue) {
                        billToAddressCtx.contactMechId = null
                        Map serviceResult = run service: 'createPartyPostalAddress', with: billToAddressCtx
                        parameters.billToContactMechId = serviceResult.contactMechId
                    }
                }
                logInfo("Billing Postal Address created billToContactMechId is ${parameters.billToContactMechId}")
            }
            List<GenericValue> pcmpBillList = from('PartyContactMechPurpose')
                .where(partyId: partyId,
                       contactMechId: parameters.billToContactMechId,
                       contactMechPurposeTypeId: 'BILLING_LOCATION')
                .filterByDate()
                .queryList()
            // If purpose does not exists then create
            if (!pcmpBillList) {
                List<GenericValue> pcmpList = from('PartyContactMechPurpose')
                    .where(partyId: partyId,
                           contactMechPurposeTypeId: 'BILLING_LOCATION')
                    .filterByDate()
                    .queryList()
                for (GenericValue pcmp: pcmpList) {
                    run service: 'expirePartyContactMechPurpose', with: pcmp.getAllFields()
                }
                run service: 'createPartyContactMechPurpose', with: [*: parameters,
                                                                     partyId: partyId,
                                                                     contactMechId: parameters.billToContactMechId,
                                                                     contactMechPurposeTypeId: 'BILLING_LOCATION']
            }
            if (parameters.setDefaultBilling == 'Y') {
                run service: 'setPartyProfileDefaults', with: [partyId: partyId,
                                                               productStoreId: parameters.productStoreId,
                                                               defaultBillAddr: parameters.billToContactMechId]
            }
        } else {
            Map serviceResult = run service: 'createPartyPostalAddress', with: [*: billToAddressCtx,
                                                                                contactMechPurposeTypeId: 'BILLING_LOCATION']
            parameters.billToContactMechId = serviceResult.contactMechId
            logInfo("Billing address created with contactmechId ${parameters.billToContactMechId}")
        }
    }
    return success([contactMechId: parameters.billToContactMechId])
}

/*
 * Create and update credit card
 */
Map createUpdateCreditCard() {
    Map serviceResult
    String paymentMethodId = parameters.paymentMethodId
    if (paymentMethodId) {
        // call update Credit Card
        GenericValue paymentMethod = from('PaymentMethod')
            .where(partyId: parameters.partyId,
                   paymentMethodTypeId: 'CREDIT_CARD')
            .orderBy('-fromDate')
            .queryFirst()
        paymentMethodId = paymentMethod ? paymentMethod.paymentMethodId : ''
        serviceResult = run service: 'updateCreditCard', with: [*: parameters,
                                                              paymentMethodId: paymentMethodId]
    } else {
        // call create Credit Card
        serviceResult = run service: 'createCreditCard', with: parameters
    }
    return success(paymentMethodId: serviceResult.paymentMethodId)
}

/*
 * Set unitPrice as lastPrice on create purchase order, edit purchase order items and on receive inventory against a purchase order,
 * but only if the order price didn't come from an agreement
 */
Map setUnitPriceAsLastPrice() {
    GenericValue order = from('OrderHeader').where(parameters).queryOne()
    if (!order || order.agreementId) {
        return success()
        // Do not update lastPrice if an agreement has been used on the order
        // TODO replace by orderPItemPriceInfo analyse when it will support agreement
    }
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (parameters.facilityId) {
        GenericValue orderSupplier = from('OrderHeaderItemAndRoles')
            .where(orderId: order.orderId,
                   roleTypeId: 'BILL_FROM_VENDOR',
                   orderTypeId: 'PURCHASE_ORDER')
            .queryFirst()
        List<GenericValue> supplierProducts = from('SupplierProduct')
            .where(productId: parameters.productId,
                   partyId: orderSupplier.partyId,
                   availableThruDate: null)
            .queryList()
        for (GenericValue supplierProduct: supplierProducts) {
            if (parameters.orderCurrencyUnitPrice && parameters.orderCurrencyUnitPrice != supplierProduct.lastPrice) {
                GenericValue newSupplierProduct = supplierProduct.clone()
                newSupplierProduct.availableFromDate = nowTimestamp
                newSupplierProduct.lastPrice = parameters.orderCurrencyUnitPrice
                newSupplierProduct.create()
                supplierProduct.availableThruDate = nowTimestamp
                supplierProduct.store()
            } else if (parameters.unitCost != supplierProduct.lastPrice) {
                GenericValue newSupplierProduct = supplierProduct.clone()
                newSupplierProduct.availableFromDate = nowTimestamp
                newSupplierProduct.lastPrice = parameters.unitCost
                newSupplierProduct.create()
                supplierProduct.availableThruDate = nowTimestamp
                supplierProduct.store()
            }
        }
    } else if (!parameters.orderItems) {
        List<GenericValue> orderItems = from('OrderItem')
            .where(orderId: order.orderId)
            .queryList()
        Map<String, Object> itemPriceMap = parameters.itemPriceMap as Map
        Map<String, Object> overridePriceMap = parameters.overridePriceMap as Map
        List<Map<String, BigDecimal>> productIdPrices = []
        Set<String> productIds = []
        for (Map.Entry<String, String> itemPrice : itemPriceMap.entrySet()) {
            String orderItemSeqId = itemPrice.getKey()
            BigDecimal unitPrice = itemPrice.getValue()
            GenericValue orderItem = orderItems.find { it.orderItemSeqId == orderItemSeqId }
            Map.Entry<String, String> overridePrice = overridePriceMap.find { it.key == orderItemSeqId }
            if (orderItem && overridePrice) {
                productIdPrices << [(orderItem.productId): unitPrice]
                productIds << orderItem.productId
            }
        }
        EntityCondition condition = new EntityConditionBuilder().AND {
            EQUALS(partyId: parameters.supplierPartyId)
            EQUALS(availableThruDate: null)
            IN(productId: productIds)
        }
        List<GenericValue> supplierProducts = from('SupplierProduct')
            .where(condition)
            .queryList()
        for (GenericValue supplierProduct : supplierProducts) {
            BigDecimal unitPrice = productIdPrices.find { it.keySet().contains(supplierProduct.productId) }?.(supplierProduct.productId)
            if (unitPrice != supplierProduct.lastPrice) {
                GenericValue newSupplierProduct = supplierProduct.clone()
                newSupplierProduct.availableFromDate = nowTimestamp
                newSupplierProduct.lastPrice = unitPrice
                newSupplierProduct.create()
                supplierProduct.availableThruDate = nowTimestamp
                supplierProduct.store()
            }
        }
    } else {
        List<GenericValue> orderItems = parameters.orderItems
        Set<String> productIds = orderItems*.productId
        EntityCondition condition = new EntityConditionBuilder().AND {
            EQUALS(partyId: parameters.supplierPartyId)
            EQUALS(availableThruDate: null)
            IN(productId: productIds)
        }
        List<GenericValue> supplierProducts = from('SupplierProduct')
            .where(condition)
            .queryList()
        for (GenericValue supplierProduct : supplierProducts) {
            GenericValue orderItem = orderItems.find { it.productId == supplierProduct.productId }
            if (orderItem.unitPrice != supplierProduct.lastPrice) {
                GenericValue newSupplierProduct = supplierProduct.clone()
                newSupplierProduct.availableFromDate = nowTimestamp
                newSupplierProduct.lastPrice = orderItem.unitPrice
                newSupplierProduct.create()
                supplierProduct.availableThruDate = nowTimestamp
                supplierProduct.store()
            }
        }
    }
    return success()
}

/*
 * Cancels those back orders from suppliers whose cancel back order date (cancelBackOrderDate) has passed the current date
 */
Map cancelAllBackOrders() {
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    EntityCondition condition = EntityCondition.makeCondition([
        EntityCondition.makeCondition('orderTypeId', 'PURCHASE_ORDER'),
        EntityCondition.makeCondition('statusId', EntityOperator.NOT_IN, ['ORDER_CANCELLED', 'ORDER_COMPLETED'])])
    List<String> currentOrderIds = from('OrderHeader')
        .where(condition)
        .getFieldList('orderId')
    if (currentOrderIds) {
        List<GenericValue> orderItems = from('OrderItem')
            .where(EntityCondition.makeCondition('orderId', EntityOperator.IN, currentOrderIds))
            .queryList()
        for (GenericValue currentOrderItem: orderItems) {
            Timestamp backOrderDate = currentOrderItem.cancelBackOrderDate
            if (backOrderDate && backOrderDate.before(nowTimestamp) ) {
                run service: 'cancelOrderItem', with: [ orderId: currentOrderItem.orderId,
                                                        orderItemSeqId: currentOrderItem.orderItemSeqId]
            }
        }
    }
    return success()
}

/*
 * Updates shipping method and shipping charges from Order View page when Shipment is in picked status and items of Order are packed
 */
Map updateShippingMethodAndCharges() {
    // splitting shipmentMethodAndAmount request parameter value that contains "*" symbol into "shipmentMethod" and "newAmount".
    // shipmentMethod request parameter value contains "@" symbol between "shipmentMethodTypeId" and "carrierPartyId".
    // This will be splitted in updateOrderItemShipGroup method
    String shipmentMethodAndAmount = parameters.shipmentMethodAndAmount
    if (shipmentMethodAndAmount != null) {
        parameters.shipmentMethod = shipmentMethodAndAmount.substring(0, shipmentMethodAndAmount.indexOf('*'))
        parameters.amount = shipmentMethodAndAmount.substring(shipmentMethodAndAmount.indexOf('*') + 1)
        parameters.shipmentMethodTypeId = shipmentMethodAndAmount.substring(0, shipmentMethodAndAmount.indexOf('@'))
    }
    BigDecimal newAmount
    BigDecimal shippingAmount
    BigDecimal percentAllowedBd
    String percentAllowed = UtilProperties.getPropertyValue('shipment.properties', 'shipment.default.cost_actual_over_estimated_percent_allowed')
    try {
        newAmount = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(parameters.amount, 'BigDecimal', null, locale)
        shippingAmount = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(parameters.shippingAmount, 'BigDecimal', null, locale)
        percentAllowedBd = (BigDecimal) ObjectType.simpleTypeOrObjectConvert(percentAllowed, 'BigDecimal', null, locale)
    } catch (GeneralException e) {
        return error(e.getMessage())
    }
    BigDecimal diffPercentage = (newAmount > shippingAmount
            ? (newAmount - shippingAmount / shippingAmount)
            : (shippingAmount - newAmount / newAmount)) * 100

    if (diffPercentage > percentAllowedBd) {
        run service: 'updateOrderAdjustment', with: parameters
    }
    run service: 'updateOrderItemShipGroup', with: parameters
    run service: 'updateShipmentRouteSegment', with: [*: parameters,
                                                       trackingIdNumber: null,
                                                       trackingDigest: null,
                                                       carrierServiceStatusId: null]
    run service: 'upsShipmentConfirm', with: parameters
    return success()
}

/*
 * Calculate ATP and Qoh According For each facility
 */
Map productAvailabilityByFacility() {
    List availabilityList = []
    List<GenericValue> facilityList = from('Facility')
        .where(ownerPartyId: parameters.ownerPartyId)
        .queryList()
    for (GenericValue facility: facilityList) {
        Map serviceResult = run service: 'getInventoryAvailableByFacility', with: [
            facilityId: facility.facilityId,
            productId: parameters.productId]
        availabilityList << [facilityId: facility.facilityId,
                             quantityOnHandTotal: serviceResult.quantityOnHandTotal,
                             availableToPromiseTotal: serviceResult.availableToPromiseTotal]
    }
    return success([availabalityList: availabilityList])
}

/*
 * Create Order Payment Application
 */
Map createOrderPaymentApplication() {
    GenericValue payment = from('Payment').where(parameters).queryOne()
    if (!payment) {
        return error(label('AccountingUiLabels', 'AccountingNoPaymentsfound'))
    }
    GenericValue orderPaymentPref = from('OrderPaymentPreference')
        .where(orderPaymentPreferenceId: payment.paymentPreferenceId)
        .queryOne()
    GenericValue orderItemBilling = from('OrderItemBilling')
        .where(orderId: orderPaymentPref.orderId)
        .queryFirst()
    if (orderItemBilling) {
        run service: 'createPaymentApplication', with: [amountApplied: payment.amount,
                                                        paymentId: payment.paymentId,
                                                        invoiceId: orderItemBilling.invoiceId]
    }
    return success()
}

/*
 * Move order items between ship groups
 */
Map moveItemBetweenShipGroups() {
    GenericValue toOisga = from('OrderItemShipGroupAssoc')
        .where(orderId: parameters.orderId,
               orderItemSeqId: parameters.orderItemSeqId,
               shipGroupSeqId: parameters.toGroupIndex)
        .queryOne()
    if (!toOisga) {
        run service: 'addOrderItemShipGroupAssoc', with: [*: parameters,
                                                          quantity: BigDecimal.ZERO,
                                                          shipGroupSeqId: parameters.toGroupIndex]
        toOisga = from('OrderItemShipGroupAssoc')
            .where(orderId: parameters.orderId,
                   orderItemSeqId: parameters.orderItemSeqId,
                   shipGroupSeqId: parameters.toGroupIndex)
            .queryOne()
    }

    run service: 'updateOrderItemShipGroupAssoc', with: [orderId: parameters.orderId,
                                                         orderItemSeqId: parameters.orderItemSeqId,
                                                         shipGroupSeqId: parameters.toGroupIndex,
                                                         quantity: toOisga.quantity + parameters.quantity]

    GenericValue fromOisga = from('OrderItemShipGroupAssoc')
        .where(orderId: parameters.orderId,
               orderItemSeqId: parameters.orderItemSeqId,
               shipGroupSeqId: parameters.fromGroupIndex)
        .queryOne()
    if (!fromOisga) {
        return error(label('OrderErrorUiLabels', 'OrderServiceOrderItemShipGroupAssocNotExist'))
    }
    run service: 'updateOrderItemShipGroupAssoc', with: [orderId: parameters.orderId,
                                                         orderItemSeqId: parameters.orderItemSeqId,
                                                         shipGroupSeqId: parameters.fromGroupIndex,
                                                         quantity: fromOisga.quantity - parameters.quantity]
    return success()
}
