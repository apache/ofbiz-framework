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
package org.apache.ofbiz.accounting.invoice

import org.apache.ofbiz.accounting.util.UtilAccounting
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityTypeUtil
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.service.ServiceUtil

import java.sql.Timestamp

Map getNextInvoiceId() {
    Map result = success()

    // try to find PartyAcctgPreference for parameters.partyId, see if we need any special invoice number sequencing
    GenericValue partyAcctgPreference = from('PartyAcctgPreference').where(parameters).queryOne()
    if (Debug.infoOn()) {
        logInfo("In getNextInvoiceId partyId is [${parameters.partyId}], partyAcctgPreference: ${partyAcctgPreference}")
    }

    String customMethodName = null
    String invoiceIdPrefix = ''
    if (partyAcctgPreference) {
        invoiceIdPrefix = partyAcctgPreference.invoiceIdPrefix ?: ''
        //see OFBIZ-3765 beware of OFBIZ-3557
        GenericValue customMethod = partyAcctgPreference.getRelatedOne('InvoiceCustomMethod', true)
        if (customMethod) {
            customMethodName = customMethod.customMethodName
        } else {
            //retrieve service from deprecated enumeration see OFBIZ-3765 beware of OFBIZ-3557
            if (partyAcctgPreference.oldInvoiceSequenceEnumId == 'INVSQ_ENF_SEQ') {
                customMethodName = 'invoiceSequenceEnforced'
            }
            if (partyAcctgPreference.oldInvoiceSequenceEnumId == 'INVSQ_RESTARTYR') {
                customMethodName = 'invoiceSequenceRestart'
            }
        }
    } else {
        logWarning("Acctg preference not defined for partyId [${parameters.partyId}]")
    }

    String invoiceIdTemp = ''
    if (customMethodName) {
        parameters.partyAcctgPreference = partyAcctgPreference
        Map serviceResult = run service: customMethodName, with: parameters
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        invoiceIdTemp = serviceResult.invoiceId
    } else {
        logInfo('In createInvoice sequence enum Standard')
        //default to the default sequencing: INVSQ_STANDARD
        invoiceIdTemp = parameters.invoiceId
        if (invoiceIdTemp) {
            //check the provided ID
            errorMsg = UtilValidate.checkValidDatabaseId(invoiceIdTemp)
            if (errorMsg != null) {
                return error("In getNextInvoiceId ${errorMsg}")
            }
        } else {
            invoiceIdTemp = delegator.getNextSeqId('Invoice', 1)
        }
    }

    // use invoiceIdTemp along with the invoiceIdPrefix to create the real ID
    result.invoiceId = invoiceIdPrefix + invoiceIdTemp
    return result
}

Map invoiceSequenceEnforced() {
    Map result = success()

    logInfo('In createInvoice sequence enum Enforced')
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    //this is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    Long lastInvoiceNumber = 1
    if (partyAcctgPreference.lastInvoiceNumber) {
        lastInvoiceNumber = partyAcctgPreference.lastInvoiceNumber + 1
    }

    partyAcctgPreference.lastInvoiceNumber = lastInvoiceNumber
    delegator.store(partyAcctgPreference)
    result.invoiceId = lastInvoiceNumber
    return result
}

Map invoiceSequenceRestart() {
    logInfo('In createInvoice sequence enum Enforced')
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    //this is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (partyAcctgPreference.lastInvoiceRestartDate) {
        //first figure out if we need to reset the lastInvoiceNumber; is the lastInvoiceRestartDate after the fiscalYearStartMonth/Day for this year?
        curYearFiscalStartDate = UtilDateTime.getYearStart(nowTimestamp,
                partyAcctgPreference.fiscalYearStartDay, partyAcctgPreference.fiscalYearStartMonth, 0L)
        if (partyAcctgPreference.lastInvoiceRestartDate < curYearFiscalStartDate && nowTimestamp >= curYearFiscalStartDate) {
            //less than fiscal year start, we need to reset it
            partyAcctgPreference.lastInvoiceNumber = 1L
            partyAcctgPreference.lastInvoiceRestartDate = nowTimestamp
        } else {
            //greater than or equal to fiscal year start or nowTimestamp hasn't yet hit the current year fiscal start date, we're okay, just increment
            partyAcctgPreference.lastInvoiceNumber += 1L
        }
    } else {
        //if no lastInvoiceRestartDate then it's easy, just start now with 1
        partyAcctgPreference.lastInvoiceNumber = 1L
        partyAcctgPreference.lastInvoiceRestartDate = nowTimestamp
    }
    delegator.store(partyAcctgPreference)

    //get the current year string for prefix, etc; simple 4 digit year date string (using system defaults)
    Integer curYearString = UtilDateTime.getYear(partyAcctgPreference.lastInvoiceRestartDate, timeZone, locale)
    return success(invoiceId: "${curYearString}-${partyAcctgPreference.lastInvoiceNumber}")
}

/**
 * Create a new Invoice
 * @return Success response containing the invoiceId, error response otherwise.
 */
Map createInvoice() {
    if (!parameters.invoiceId) {
        Map serviceResult = run service: 'getNextInvoiceId', with: [*: parameters,
                                                                    partyId: parameters.partyIdFrom]
        parameters.invoiceId = serviceResult.invoiceId
    }
    GenericValue party = from('Party').where(parameters).queryOne()
    if (party?.preferredCurrencyUomId) {
        parameters.currencyUomId = party.preferredCurrencyUomId
    }
    GenericValue invoice = makeValue('Invoice', parameters)
    invoice.create()
    run service: 'createInvoiceStatus', with: parameters
    return success([invoiceId:  invoice.invoiceId])
}

/**
 * Retrieve an invoice and the items
 * @return Success response containing the invoice and items, failure response otherwise.
 */
Map getInvoice() {
    GenericValue invoice = from('Invoice').where(parameters).queryOne()
    if (!invoice) {
        return failure(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    return success([invoice: invoice,
                    invoiceItems: invoice.getRelated('InvoiceItem', null, null, false)])
}

/**
 * Update the header of an existing Invoice
 * @return Success response if invoice updated, error response otherwise.
 */
Map updateInvoice() {
    GenericValue invoice = from('Invoice').where(parameters).queryOne()
    if (!invoice) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    if (invoice.statusId != 'INVOICE_IN_PROCESS') {
        return error(label('AccountingUiLabels', 'AccountingInvoiceUpdateOnlyWithInProcessStatus', [statustId: invoice.statusId]))
    }

    // only save if something has changed, do not update status here
    // update all non status and key fields
    GenericValue lookedInvoice = invoice.clone()
    invoice.setNonPKFields([*: parameters, statustId: 'INVOICE_IN_PROCESS'], false)
    if (lookedInvoice != invoice) {
        invoice.store()
    }

    // check if there is a requested status change if yes call invoice status update service
    if (parameters.statusId && parameters.statusId != 'INVOICE_IN_PROCESS') {
        run service: 'setInvoiceStatus', with: [invoiceId: invoice.invoiceId,
                                                statustId: parameters.statustId]
    }
    return success()
}

/**
 * Create a new Invoice from an existing invoice
 * @return Success response containing the invoiceId, error response otherwise.
 */
Map copyInvoice() {
    Map serviceResult = run service: 'getInvoice', with: [invoiceId: parameters.invoiceIdToCopyFrom]
    GenericValue invoice = serviceResult.invoice
    List<GenericValue> invoiceItems = serviceResult.invoiceItems
    invoice.invoiceTypeId = parameters.invoiceTypeId ?: invoice.invoiceTypeId
    serviceResult = run service: 'createInvoice', with: [*: invoice.getAllFields(),
                                                         invoiceId: null]
    String newInvoiceId = serviceResult.invoiceId
    invoiceItems.each {
        run service: 'createInvoiceItem', with: [*: it.getAllFields(),
                                                 invoiceId: newInvoiceId]
    }
    return success([invoiceId: newInvoiceId])
}

/**
 * Copy a invoice to a InvoiceType starting with 'template'
 * @return Success response containing the invoiceId, error response otherwise.
 */
Map copyInvoiceToTemplate() {
    String invoiceTypeId = parameters.invoiceTypeId
    Map switchType = [SALES_INVOICE: 'SALES_INV_TEMPLATE',
            PURCHASE_INVOICE: 'PUR_INV_TEMPLATE']
    run service: 'copyInvoice', with: [*: parameters,
                                       invoiceIdToCopyFrom: parameters.invoiceId,
                                       invoiceTypeId: switchType.get(invoiceTypeId) ?: invoiceTypeId]
}

/**
 * Set The Invoice Status
 * @return Success response after status stored, error response otherwise.
 */
Map setInvoiceStatus() {
    GenericValue invoice = from('Invoice').where(parameters).queryOne()
    if (!invoice) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    String oldStatusId = invoice.statusId
    String invoiceTypeId = invoice.invoiceTypeId
    Map returnResult = [oldStatusId: oldStatusId, invoiceTypeId: invoiceTypeId]
    if (oldStatusId == parameters.statusId) {
        return success(returnResult)
    }

    if (from('StatusValidChange')
            .where(statusId: oldStatusId, statusIdTo: parameters.statusId)
            .queryCount() == 0) {
        return error(label('AccountingUiLabels', 'AccountingPSInvalidStatusChange'))
    }

    // if new status is paid check if the complete invoice is applied
    if (parameters.statusId == 'INVOICE_PAID') {
        BigDecimal notApplied = InvoiceWorker.getInvoiceNotApplied(invoice)
        if (notApplied != 0) {
            return error(label('AccountingUiLabels', 'AccountingInvoiceCannotChangeStatusToPaid'))
        }
    }

    // if it's OK to mark invoice paid, use parameters for paidDate
    invoice.paidDate = parameters.paidDate ?: UtilDateTime.nowTimestamp()

    if (parameters.statusId == 'INVOICE_READY' && invoice.paidDate) {
        invoice.paidDate = null
    }
    invoice.statusId = parameters.statusId
    invoice.store()

    run service: 'createInvoiceStatus', with: [invoiceId: invoice.invoiceId,
                                               statusId: invoice.statusId,
                                               statusDate: parameters.statusDate]

    // if the invoice is a payrol invoice, create the payment in the not-paid status
    // TODO the next part need to move on dedicate service
    if (invoiceTypeId == 'PAYROL_INVOICE' &&
            ['INVOICE_APPROVED', 'INVOICE_READY'].contains(invoice.statusId)) {
        // only generate payment if no application exist yet
        List paymentApplications = invoice.getRelated('PaymentApplication', null, null, false)
        if (!paymentApplications) {
            BigDecimal amount = InvoiceWorker.getInvoiceTotal(invoice)
            Map serviceResult = run service: 'createPayment', with: [partyIdFrom: invoice.partyId,
                                                                     partyIdTo: invoice.partyIdFrom,
                                                                     paymentMethodTypeId: 'COMPANY_CHECK',
                                                                     paymentTypeId: 'PAYROL_PAYMENT',
                                                                     statusId: 'PMNT_NOT_PAID',
                                                                     currencyUomId: invoice.currencyUomId,
                                                                     amount: amount]
            run service: 'createPaymentApplication', with: [invoiceId: invoice.invoiceId,
                                                            paymentId: serviceResult.paymentId,
                                                            amountApplied: amount]
        }
    }
    return success(returnResult)
}

/**
 * Check if the invoiceStatus is in progress
 * @return Success response containing hasPermission to edit, error response otherwise.
 */
Map checkInvoiceStatusInProgress() {
    GenericValue invoice = from('Invoice').where(parameters).cache().queryOne()
    boolean hasPermission = invoice && invoice.statusId == 'INVOICE_IN_PROCESS'
    return success([hasPermission: hasPermission])
}

/**
 * Service run after cancel an invoice
 * @return Success response containing the invoiceTypeId cancelled, error response otherwise.
 */
Map cancelInvoice() {
    GenericValue invoice = from('Invoice').where(parameters).cache().queryOne()
    if (!invoice) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    invoice.getRelated('PaymentApplication', null, null, false).each {
        GenericValue payment = it.getRelatedOne('Payment', false)
        if (payment.statusId == 'PMNT_CONFIRMED') {
            run service: 'setPaymentStatus', with: [paymentId: payment.paymentId,
                                                    statusId: UtilAccounting.isReceipt(payment) ? 'PMNT_RECEIVED' : 'PMNT_SENT']
        }
        run service: 'removePaymentApplication', with: [paymentApplicationId: it.paymentApplicationId]
    }
    return success([invoiceTypeId: invoice.invoiceTypeId])
}

/**
 * Send an invoice per Email
 * @return Success response
 */
Map sendInvoicePerEmail() {
    Map emailParams = dispatcher.getDispatchContext()
            .makeValidContext([*: parameters,
                               xslfoAttachScreenLocation: 'component://accounting/widget/AccountingPrintScreens.xml#InvoicePDF',
                               bodyParameters: [invoiceId: parameters.invoiceId,
                                                userLogin: parameters.userLogin,
                                                other: parameters.other] //to print in 'other currency'
    ])
    dispatcher.runAsync('sendMailFromScreen', emailParams)
    return success(label('AccountingUiLabels', 'AccountingEmailScheduledToSend'))
}

/**
 * Create a new Invoice Item
 * @return Success response containing the invoiceItemSeqId, error response otherwise.
 */
Map createInvoiceItem() {
    GenericValue invoiceItem = makeValue('InvoiceItem', parameters)
    if (!invoiceItem.invoiceItemSeqId) {
        delegator.setNextSubSeqId(invoiceItem, 'invoiceItemSeqId', 5, 1)
    }
    // if there is no amount and a productItem is supplied fill the amount(price) and description from the product record
    //     TODO: there are return adjustments now that make this code very broken. The check for price was added as a quick fix.
    if (invoiceItem.productId) {
        invoiceItem.quantity = invoiceItem.quantity ?: 1
        if (!invoiceItem.amount) {
            GenericValue product = from('Product').where(parameters).cache().queryOne()
            invoiceItem.description = product.description
            Map serviceResult = run service: 'calculateProductPrice', with: [product: product]
            invoiceItem.amount = serviceResult.price
        }
    }
    if (invoiceItem.amount == null) { // accept 0
        return error(label('AccountingUiLabels', 'AccountingInvoiceAmountIsMandatory'))
    }
    invoiceItem.create()
    return success([invoiceId: invoiceItem.invoiceId,
                    invoiceItemSeqId: invoiceItem.invoiceItemSeqId])
}

/**
 * Update an existing Invoice Item
 * @return Success response after updated, error response otherwise.
 */
Map updateInvoiceItem() {
    GenericValue invoiceItem = from('InvoiceItem').where(parameters).queryOne()
    if (!invoiceItem) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceItemNotFound', parameters))
    }
    GenericValue lookedInvoiceItem = invoiceItem.clone()
    invoiceItem.setNonPKFields(parameters, false)

    // check if the productNumber is updated, when yes retrieve product description and price
    if (lookedInvoiceItem.productId != invoiceItem.productId) {
        GenericValue product = from('Product').where(parameters).cache().queryOne()
        invoiceItem.description = product.description
        Map serviceResult = run service: 'calculateProductPrice', with: [product: product]
        invoiceItem.amount = serviceResult.price
        if (invoiceItem.amount == null) {
            return error(label('AccountingUiLabels', 'AccountingInvoiceAmountIsMandatory'))
        }
    }
    if (lookedInvoiceItem != invoiceItem) {
        invoiceItem.store()
    }
    return success([invoiceId: invoiceItem.invoiceId,
                    invoiceItemSeqId: invoiceItem.invoiceItemSeqId])
}

/**
 * Remove an existing Invoice Item
 * @return Success response after remove, error response otherwise.
 */
Map removeInvoiceItem() {
    GenericValue invoiceItem = from('InvoiceItem').where(parameters).queryOne()
    if (!invoiceItem) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceItemNotFound', parameters))
    }
    // check if there are specific item paymentApplications when yes remove those
    invoiceItem.removeRelated('PaymentApplication')
    invoiceItem.remove()
    return success()
}

/**
 * Scheduled service to generate Invoice from an existing Invoice
 */
Map autoGenerateInvoiceFromExistingInvoice() {
    Map switchType = [SALES_INV_TEMPLATE: 'SALES_INVOICE',
                      PUR_INV_TEMPLATE: 'PURCHASE_INVOICE']
    from('Invoice')
            .where(recurrenceInfoId: parameters.recurrenceInfoId)
            .queryList()
            .each {
                Map serviceResult = run service: 'copyInvoice', with: [*: it.getAllFields(),
                                                                       invoiceIdToCopyFrom: it.invoiceId]
                if (switchType.containsKey(it.invoiceTypeId)) {
                    String invoiceId = serviceResult.invoiceId
                    run service: 'updateInvoice', with: [invoiceId: invoiceId,
                                                         recurrenceInfoId: null,
                                                         invoiceTypeId: switchType(it.invoiceTypeId)]
                }
            }
    return success()
}

/**
 * Calculate running total for Invoices
 * @return Success response containing the invoiceRunningTotal, error response otherwise.
 */
Map getInvoiceRunningTotal() {
    BigDecimal runningTotal = 0
    parameters.invoiceIds.each {
        Map serviceResult = run service: 'getInvoicePaymentInfoList', with: [invoiceId: it]
        if (serviceResult.invoicePaymentInfoList) {
            runningTotal += serviceResult.invoicePaymentInfoList[0].outstandingAmount
        }
    }
    Map serviceResult = run service: 'getPartyAccountingPreferences', with: parameters
    Map partyAccountingPreference = serviceResult.partyAccountingPreference
    String currencyUomId = partyAccountingPreference.baseCurrencyUomId ?:
            EntityUtilProperties.getPropertyValue('general', 'currency.uom.id.default', 'USD', delegator)
    return success([invoiceRunningTotal: UtilFormatOut.formatCurrency(runningTotal, currencyUomId, parameters.locale)])
}

/**
 * Filter invoices by invoiceItemAssocTypeId
 * @return Success response containing filteredInvoiceList, error response otherwise.
 */
Map getInvoicesFilterByAssocType() {
    EntityCondition condition = new EntityConditionBuilder().AND {
        EQUALS(invoiceItemAssocTypeId: parameters.invoiceItemAssocTypeId)
        IN(invoiceIdFrom: parameters.invoiceList*.invoiceId)
    }
    List invoiceIds = from('InvoiceItemAssoc')
            .where(condition)
            .distinct()
            .filterByDate()
            .getFieldList('invoiceIdFrom')
    return success([filteredInvoiceList: parameters.invoiceList.findAll { invoiceIds.contains(it.invoiceId) }])
}

/**
 * Remove invoiceItemAssoc record on cancel invoice
 * @return Success response after remove, error response otherwise.
 */
Map removeInvoiceItemAssocOnCancelInvoice() {
    from('InvoiceItemAssoc')
            .where(invoiceIdTo: parameters.invoiceId)
            .queryList()
            .each {
                run service: 'deleteInvoiceItemAssoc', with: it.getAllFields()
            }
    return success()
}

/**
 * Reset OrderItemBilling and OrderAdjustmentBilling records on cancel invoice,
 * so it is isn't considered invoiced any more by createInvoiceForOrder service
 * @return Success response
 */
Map resetOrderItemBillingAndOrderAdjustmentBillingOnCancelInvoice() {
    from('OrderItemBilling')
            .where(invoiceId: parameters.invoiceId)
            .queryList()
            .each {
                it.quantity = 0
                it.store()
            }
    from('OrderAdjustmentBilling')
            .where(invoiceId: parameters.invoiceId)
            .queryList()
            .each {
                it.amount = 0
                it.store()
            }
    return success()
}

/**
 * Service set status of Invoices in bulk.
 * @return Success response
 */
Map massChangeInvoiceStatus() {
    parameters.invoiceIds.each {
        run service: 'setInvoiceStatus', with: [invoiceId: it,
                                                statusId: parameters.statusId]
    }
    return success()
}

/**
 * Set Parameter And Call Tax Calculate Service
 * @return Success response
 */
Map addTaxOnInvoice() {
    GenericValue invoice = from('Invoice').where(parameters).cache().queryOne()
    if (!invoice) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    GenericValue shippingContact = from('PartyContactMechPurpose')
            .where(partyId: invoice.partyId,
                    contactMechPurposeTypeId: 'SHIPPING_LOCATION')
            .queryFirst() ?:
            from('PartyContactMechPurpose')
                    .where(partyId: invoice.partyId,
                            contactMechPurposeTypeId: 'GENERAL_LOCATION')
                    .queryFirst()
    if (!shippingContact) {
        return error(label('AccountingUiLabels', 'AccountingTaxCannotCalculate'))
    }
    GenericValue postalAddress = from('PostalAddress').where(contactMechId: shippingContact.contactMechId).cache().queryOne()
    Map addTaxMap = [billToPartyId: invoice.invoiceTypeId == 'SALES_INVOICE' ? invoice.partyId : invoice.partyIdFrom,
                     payToPartyId: invoice.partyIdFrom,
                     orderPromotionsAmount: 0,
                     orderShippingAmount: 0,
                     shippingAddress: postalAddress,
                     itemProductList: [],
                     itemAmountList: [],
                     itemPriceList: [],
                     itemQuantityList: [],
                     itemShippingList: []]
    List invoiceItems = invoice.getRelated('InvoiceItem', null, null, false)
    invoiceItems.each {
        BigDecimal totalAmount = 0
        if (it.productId) {
            addTaxMap.itemProductList << from('Product').where(productId: it.productId).cache().queryOne()
            List promoAdjs = EntityUtil.filterByAnd(invoiceItems,
                    [productId: it.productId,
                    invoiceItemTypeId: 'ITM_PROMOTION_ADJ'])
            totalAmount = it.amount * it.quantity
            if (promoAdjs) {
                totalAmount -= it.amount
            }
        }
        addTaxMap.itemAmountList << totalAmount
        addTaxMap.itemPriceList << it.amount
        addTaxMap.itemQuantityList << it.quantity
        addTaxMap.itemShippingList << 0
    }
    if (!addTaxMap.itemProductList) {
        return error(label('AccountingUiLabels', 'AccountingTaxProductIdCannotCalculate'))
    }
    Map serviceResult = run service: 'calcTax', with: addTaxMap
    Map itemMap = [itemSeqIdList: [],
                   productList: []]
    invoiceItems.findAll { it.productId }.each {
        itemMap.itemSeqIdList << it.invoiceItemSeqId
        itemMap.productList << it.productId
    }
    Long countItemId = -1
    serviceResult.itemAdjustments.each {
        countItemId ++
        if (it) {
            it.each {
                run service: 'createInvoiceItem', with: [invoiceItemTypeId: invoice.invoiceTypeId == 'PURCHASE_INVOICE' ?
                        'PITM_SALES_TAX' : 'ITM_SALES_TAX',
                                                         invoiceId: invoice.invoiceId,
                                                         overrideGlAccountId: it.overrideGlAccountId,
                                                         productId: itemMap.productList[countItemId],
                                                         taxAuthPartyId: it.taxAuthPartyId,
                                                         taxAuthGeoId: it.taxAuthGeoId,
                                                         amount: it.amount,
                                                         quantity: 1,
                                                         parentInvoiceItemSeqId: itemMap.itemSeqIdList[countItemId],
                                                         taxAuthorityRateSeqId: it.taxAuthorityRateSeqId,
                                                         description: it.comments]
            }
        }
    }
    serviceResult.orderAdjustments.each {
        run service: 'createInvoiceItem', with: [invoiceItemTypeId: invoice.invoiceTypeId == 'PURCHASE_INVOICE' ?
                'PITM_SALES_TAX' : 'ITM_SALES_TAX',
                                                 invoiceId: invoice.invoiceId,
                                                 overrideGlAccountId: it.overrideGlAccountId,
                                                 taxAuthPartyId: it.taxAuthPartyId,
                                                 taxAuthGeoId: it.taxAuthGeoId,
                                                 amount: it.amount,
                                                 quantity: 1,
                                                 taxAuthorityRateSeqId: it.taxAuthorityRateSeqId,
                                                 description: it.comments]
    }
    return success()
}

/**
 * Create an invoice from existing order when invoicePerShipment is N
 * @return Success response
 */
Map createInvoiceFromOrder() {
    GenericValue order = from('OrderHeader').where(parameters).queryOne()
    String invoicePerShipment = order?.invoicePerShipment ?:
            EntityUtilProperties.getPropertyValue('accounting', 'create.invoice.per.shipment', 'N', delegator)
    if (invoicePerShipment == 'N') {
        List orderItemBillingItemsSeqIds = from('OrderItemBilling').where(orderId: order.orderId).getFieldList('orderItemSeqId')
        if (!orderItemBillingItemsSeqIds) {
            Map serviceResult = run service: 'createInvoiceForOrderAllItems', with: [orderId: order.orderId]
            return serviceResult
        }
        List orderItems = from('OrderItem').where(orderId: order.orderId).queryList()
        orderItems = orderItems.findAll { !orderItemBillingItemsSeqIds.contains(it.orderItemSeqId) }
        Map serviceResult = run service: 'createInvoiceForOrder', with: [orderId:  order.orderId,
                                                                         billItems: orderItems]
        return serviceResult
    }
    return success()
}

/**
 * check if a invoice is in a foreign currency related to the accounting company.
 * @return Success response
 */
Map isInvoiceInForeignCurrency() {
    GenericValue invoice = from('Invoice').where(parameters).cache().queryOne()
    if (!invoice) {
        return error(label('AccountingUiLabels', 'AccountingInvoiceNotFound', parameters))
    }
    String partyId = EntityTypeUtil.hasParentType(delegator, 'InvoiceType', 'invoiceTypeId',
            invoice.invoiceTypeId, 'parentTypeId', 'PURCHASE_INVOICE') ?
            invoice.partyId : invoice.partyIdFrom
    Map serviceResult = run service: 'getPartyAccountingPreferences', with: [organizationPartyId: partyId]
    return success([isForeign: invoice.currencyUomId == serviceResult.baseCurrencyUomId])
}
