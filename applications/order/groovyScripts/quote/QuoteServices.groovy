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

import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem
import org.apache.ofbiz.product.config.ProductConfigWorker
import org.apache.ofbiz.product.config.ProductConfigWrapper
import org.apache.ofbiz.service.ExecutionServiceException
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

/**
 * Set the Quote status to ordered.
 */
def checkUpdateQuoteStatus() {
    GenericValue quote = from('Quote').where(parameters).queryOne()
    if (!quote) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteDoesNotExists', locale))
    }
    quote.statusId = 'QUO_ORDERED'
    quote.store()
    return success()
}

/**
 * Get new Quote sequence Id.
 */
def getNextQuoteId() {
    // Try to find PartyAcctgPreference for parameters.partyId, see if we need any special quote number sequencing
    GenericValue partyAcctgPreference = from('PartyAcctgPreference').where('partyId', parameters.partyId).queryOne()
    logInfo("In getNextQuoteId partyId is [${parameters.partyId}], partyAcctgPreference: ${partyAcctgPreference}")

    Map customMethod = null
    if (partyAcctgPreference) {
        customMethod = partyAcctgPreference.getRelatedOne('QuoteCustomMethod', false)
    } else {
        logWarning("Acctg preference not defined for partyId [${parameters.partyId}]")
    }

    String customMethodName
    if (customMethod?.customMethodName) {
        customMethodName = customMethod.customMethodName
    } else if (partyAcctgPreference?.oldQuoteSequenceEnumId == 'QTESQ_ENF_SEQ') {
        // Retrieve service from deprecated enumeration
        customMethodName = 'quoteSequenceEnforced'
    }

    String quoteId
    if (customMethodName) {
        Map serviceResult = run service: customMethodName, with: [
            partyId: parameters.partyId,
            partyAcctgPreference: partyAcctgPreference
        ]
        quoteId = serviceResult.quoteId
    } else {
        // Default to the default sequencing: QTESQ_STANDARD
        quoteId = parameters.quoteId
        if (quoteId) {
            GenericValue quote = from('Quote').where('quoteId', quoteId).queryOne()
            if (quote) {
                // Return alert if ID already exists
                return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteIdAlreadyExists', [quoteId: quoteId], locale))
            } else {
                // Check the provided ID
                String errorMessage = UtilValidate.checkValidDatabaseId(quoteId)
                if (errorMessage) {
                    return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteGetNextIdError', locale) + errorMessage)
                }
            }
        } else {
            quoteId = delegator.getNextSeqId("Quote")
        }
    }

    if (partyAcctgPreference) {
        quoteId = "${partyAcctgPreference.quoteIdPrefix?:''}${quoteId}"
    }
    return [successMessage: null, quoteId: quoteId]
}

/**
 * Enforced Sequence (no gaps, per organization).
 */
def quoteSequenceEnforced() {
    logInfo('In getNextQuoteId sequence enum Enforced')
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    // This is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    partyAcctgPreference.lastQuoteNumber = partyAcctgPreference.lastQuoteNumber ? partyAcctgPreference.lastQuoteNumber + 1: new Long('1')

    partyAcctgPreference.store()
    return [successMessage: null, quoteId: partyAcctgPreference.lastQuoteNumber]
}

/**
 * Create a new Quote.
 */
def createQuote() {
    if (parameters.partyId
            && parameters.partyId != userLogin.partyId
            && !security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateQuote', locale))
    }

    // Create new entity and create all the fields.
    GenericValue newEntity = makeValue('Quote', parameters)
    newEntity.statusId = parameters.statusId ?: 'QUO_CREATED'

    // Create a non existing ID; if we have a productStoreId do it for the payToPartyId of that ProductStore
    // according to PartyAcctgPreferences, otherwise get from standard sequence.
    GenericValue productStore
    if (parameters.productStoreId) {
        productStore = from('ProductStore').where('productStoreId', parameters.productStoreId).queryOne()
    }
    if (productStore?.payToPartyId) {
        Map serviceResult = run service: 'getNextQuoteId', with: [partyId: productStore.payToPartyId]
        newEntity.quoteId = serviceResult.quoteId
    } else {
        newEntity.quoteId = delegator.getNextSeqId("Quote")
    }

    // Finally create the record (should not exist already).
    newEntity.create()

    // If the logged in partyId that is creating the quote is not equal to the partyId
    // then we associate it to the quote as the request taker.
    // This is not done if they are the same e.g. a logged in customer that is
    // creating a quote for its own sake.
    if (parameters.partyId != userLogin.partyId) {
        Map serviceResult = run service: 'createQuoteRole', with: [
            quoteId: newEntity.quoteId,
            partyId: userLogin.partyId,
            roleTypeId: 'REQ_TAKER'
        ]
    }

    // Set ProductStore's payToPartyId as internal organisation for quote.
    if (productStore?.payToPartyId) {
        Map serviceResult = run service: 'createQuoteRole', with: [
            quoteId: newEntity.quoteId,
            partyId: productStore.payToPartyId,
            roleTypeId: 'INTERNAL_ORGANIZATIO'
        ]
    }
    def msg = UtilProperties.getMessage('OrderUiLabels', 'OrderOrderQuoteCreatedSuccessfully', locale)
    return [successMessage: msg, quoteId: newEntity.quoteId]
}

/**
 * Update an existing quote.
 * @return quoteId
 */
def updateQuote() {
    if (!security.hasEntityPermission('ORDERMGR', '_UPDATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunUpdateQuote', locale))
    }
    quoteId = parameters.quoteId
    GenericValue quote = from('Quote').where('quoteId', quoteId).queryOne()

    if (!parameters.statusId) {
        parameters.statusId = quote.statusId
    }

    if (parameters.statusId != quote.statusId) {
        // Check if the status change is a valid change.
        GenericValue validChange = from("StatusValidChange").where('statusId', quote.statusId, 'statusIdTo', parameters.statusId).queryOne()

        if (!validChange) {
            logError("The status change from ${quote.statusId} to ${parameters.statusId} is not a valid change")
            // FIXME : LABEL :D
            return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteStatusChangeIsNotValid', locale))
        }
    }

    quote.setNonPKFields(parameters)
    quote.store()

    return success(UtilProperties.getMessage('OrderUiLabels', 'OrderOrderQuoteUpdatedSuccessfully', locale))
}

/**
 * Copy an existing Quote.
 */
def copyQuote() {
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCopyQuote', locale))
    }
    GenericValue quote = from('Quote').where(parameters).queryOne()
    if (!quote) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteDoesNotExists', locale))
    }
    Map serviceResult = run service: 'createQuote', with: [*:quote, statusId: null]
    String quoteIdTo = serviceResult.quoteId

    // Copy quoteItems.
    if ('Y' == parameters.copyQuoteItems) {
        List quoteItems = quote.getRelated('QuoteItem', null, null, false)
        for (GenericValue quoteItem : quoteItems) {
            Map serviceContext = dctx.makeValidContext('createQuoteItem', ModelService.IN_PARAM, [*: quoteItem, quoteId: quoteIdTo, userLogin: userLogin])
            serviceResult = dispatcher.runSync('createQuoteItem', serviceContext)
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }

    // Copy quoteAdjustments.
    if ('Y' == parameters.copyQuoteAdjustments) {
        List quoteAdjustments = quote.getRelated('QuoteAdjustment', null, null, false)
        for (GenericValue quoteAdjustement : quoteAdjustments) {
            if (!quoteAdjustment.quoteItemSeqId) {
                Map serviceContext = dctx.makeValidContext('createQuoteAdjustment', ModelService.IN_PARAM, [*: quoteAdjustement, quoteId: quoteIdTo, userLogin: userLogin])
                serviceResult = dispatcher.runSync('createQuoteAdjustment', serviceContext)
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult
                }
            }
        }
    }

    // Copy quoteRoles.
    if ('Y' == parameters.copyQuoteRoles) {
        List quoteRoles = quote.getRelated('QuoteRole', null, null, false)
        for (GenericValue quoteRole : quoteRoles) {
            if (quoteRole.roleTypeId != 'REQ_TAKER') {
                Map serviceContext = dctx.makeValidContext('createQuoteRole', ModelService.IN_PARAM, [*: quoteRole, quoteId: quoteIdTo, userLogin: userLogin])
                serviceResult = dispatcher.runSync('createQuoteRole', serviceContext)
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult
                }
            }
        }
    }

    // Copy quoteAttributes.
    if ('Y' == parameters.copyQuoteAttributes) {
        List quoteAttributes = quote.getRelated('QuoteAttribute', null, null, false)
        for (GenericValue quoteAttribute : quoteAttributes) {
            Map serviceContext = dctx.makeValidContext('createQuoteAttribute', ModelService.IN_PARAM, [*: quoteAttribute, quoteId: quoteIdTo, userLogin: userLogin])
            serviceResult = dispatcher.runSync('createQuoteAttribute', serviceContext)
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }

    // Copy quoteCoefficients.
    if ('Y' == parameters.copyQuoteCoefficients) {
        List quoteCoefficients = quote.getRelated('QuoteCoefficient', null, null, false)
        for (GenericValue quoteCoefficient : quoteCoefficients) {
            Map serviceContext = dctx.makeValidContext('createQuoteCoefficient', ModelService.IN_PARAM, [*: quoteCoefficient, quoteId: quoteIdTo, userLogin: userLogin])
            serviceResult = dispatcher.runSync('createQuoteCoefficient', serviceContext)
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }

    // Copy quoteTerms.
    if ('Y' == parameters.copyQuoteTerms) {
        List quoteTerms = quote.getRelated('QuoteTerm', null, null, false)
        for (GenericValue quoteTerm : quoteTerms) {
            Map serviceContext = dctx.makeValidContext('createQuoteTerm', ModelService.IN_PARAM, [*: quoteTerm, quoteId: quoteIdTo, userLogin: userLogin])
            serviceResult = dispatcher.runSync('createQuoteTerm', serviceContext)
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }
    def msg = UtilProperties.getMessage('OrderUiLabels', 'OrderOrderQuoteCreatedSuccessfully', locale);
    return [successMessage: msg, quoteId: quoteIdTo]
}

def ensureWorkEffortAndCreateQuoteWorkEffort() {
    String workEffortId = parameters.workEffortId
    if (!workEffortId) {
        Map serviceResult = run service: 'createWorkEffort', with: parameters
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        workEffortId = serviceResult.workEffortId
    }
    Map createQuoteWorkEffortInMap = [quoteId: parameters.quoteId, workEffortId: workEffortId]
    Map serviceResult
    try {
        serviceResult = run service: 'createQuoteWorkEffort', with: createQuoteWorkEffortInMap
    } catch (ExecutionServiceException e) {
        serviceResult = ServiceUtil.returnError(e.toString())
    }
    serviceResult.workEffortId = workEffortId
    return serviceResult
}
/**
 * Create a new QuoteItem, calculate the quoteUnitPrice from config or productPrice if not given.
 */
def createQuoteItem() {
    GenericValue quote = from('Quote').where(parameters).queryOne()
    if (!quote) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteDoesNotExists', locale))
    }
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateQuoteItem', locale))
    }
    GenericValue quoteItem = delegator.makeValidValue('QuoteItem', parameters)
    if (!quoteItem.quoteItemSeqId) {
        delegator.setNextSubSeqId(quoteItem, 'quoteItemSeqId', 5, 1)
    }

    if (!parameters.quoteUnitPrice && parameters.productId) {
        GenericValue product = from('Product').where(parameters).cache().queryOne()
        if (product?.isVirtual == 'Y') {
            return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderCannotAddVirtualProductToQuote', locale))
        }
        if (product?.productTypeId?.startsWith('AGGREGATED')
                && parameters.configId) {
            ProductConfigWrapper configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, parameters.configId, product.productId, null, null, null, null, locale, userLogin)
            quoteItem.quoteUnitPrice = configWrapper.getTotalPrice()
        } else {
            Map serviceResult = run service: 'calculateProductPrice', with: [
                product: product,
                quantity: quoteItem.quantity,
                amount: parameters.selectedAmount
            ]
            quoteItem.quoteUnitPrice = serviceResult.price
        }
    }
    quoteItem.create()
    return [successMessage: null, quoteId: quoteItem.quoteId, quoteItemSeqId: quoteItem.quoteItemSeqId]
}

/**
 * Update an existing QuoteItem.
 */
def updateQuoteItem() {
    if (!security.hasEntityPermission('ORDERMGR', '_UPDATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunUpdateQuoteItem', locale))
    }

    Map pksQuoteItem = [quoteId: parameters.quoteId, quoteItemSeqId: parameters.quoteItemSeqId]
    GenericValue quoteItem = from('QuoteItem').where(pksQuoteItem).queryOne()
    if (!quoteItem) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteItemDoesNotExists', locale))
    }
    quoteItem.setNonPKFields(parameters)
    quoteItem.store()
    return success()
}

/**
 * Remove a QuoteItem.
 */
def removeQuoteItem() {
    Map pksQuoteItem = [quoteId: parameters.quoteId, quoteItemSeqId: parameters.quoteItemSeqId]
    GenericValue quoteItem = from('QuoteItem').where(pksQuoteItem).queryOne()
    if (!quoteItem) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteItemDoesNotExists', locale))
    }
    delegator.removeByAnd('QuoteTerm', pksQuoteItem)
    delegator.removeByAnd('QuoteAdjustment', pksQuoteItem)
    quoteItem.remove()
    return success()
}

/**
 * Copy an existing QuoteItem.
 */
def copyQuoteItem() {
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCopyQuoteItem', locale))
    }
    GenericValue quoteItem = from('QuoteItem').where(parameters).queryOne()
    if (!quoteItem) {
        return error(UtilProperties.getMessage('OrderUiLabels', 'OrderQuoteItemDoesNotExists', locale))
    }
    Map input = [
        userLogin: userLogin,
        *: quoteItem,
        quoteId: parameters.quoteIdTo,
        quoteItemSeqId: parameters.quoteItemSeqIdTo
    ]
    if (!parameters.quoteIdTo && !parameters.quoteItemSeqIdTo) {
        input.quoteItemSeqId = null
    }
    Map serviceResult = run service: 'createQuoteItem', with: input
    if ('Y' == parameters.copyQuoteAdjustments) {
        List quoteAdjustments = quoteItem.getRelated('QuoteAdjustment', null, null, false)
        for (GenericValue quoteAdjustment : quoteAdjustments) {
            Map serviceContext = dctx.makeValidContext('createQuoteAdjustment', ModelService.IN_PARAM, [*: quoteAdjustment, quoteId: parameters.quoteIdTo, quoteItemSeqId: parameters.quoteItemSeqIdTo, userLogin: userLogin])
            serviceResult = dispatcher.runSync('createQuoteAdjustment', serviceContext)
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult
            }
        }
    }

    return success()
}

/**
 * Create a new Quote and QuoteItem for a given CustRequest.
 */
def createQuoteAndQuoteItemForRequest() {
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateQuoteAndQuoteItemForRequest', locale))
    }
    GenericValue custRequest = from('CustRequest').where(parameters).queryOne()
    GenericValue custRequestItem = from('CustRequestItem').where(parameters).queryOne()
    if (!custRequest) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderErrorCustRequestWithIdDoesntExist', locale))
    }

    Map input = [
        userLogin: userLogin,
        *: parameters,
        *: custRequest,
        quoteTypeId: 'PROPOSAL',
        partyId: custRequest.fromPartyId,
        quoteName: custRequest.custRequestName,
        currencyUomId: custRequest.maximumAmountUomId
    ]
    if (!input.statusId) {
        input.statusId = 'QUO_CREATED'
    }
    Map serviceResult = run service: 'createQuote', with: input
    String quoteId = serviceResult.quoteId

    serviceResult = run service: 'createQuoteItem', with: [
        *: custRequestItem,
        comments: custRequestItem.story,
        quoteId: quoteId
    ]
    String quoteItemSeqId = serviceResult.quoteItemSeqId

    // copy the roles from the request to the quote
    List custRequestParties = from('CustRequestParty').where(custRequestId: custRequest.custRequestId).queryList()
    custRequestParties?.each { GenericValue custPartyRole ->
        serviceResult = run service: 'createQuoteRole', with: [*: custPartyRole, quoteId: quoteId]
    }

    return [successMessage: null, quoteId: quoteId, quoteItemSeqId: quoteItemSeqId]
}

/**
 * Create a Quote from a ShoppingCart.
 */
def createQuoteFromCart() {
    ShoppingCart cart = (ShoppingCart) parameters.cart

    Map createQuoteInMap = parameters
    createQuoteInMap.partyId = cart.getPartyId()

    if (createQuoteInMap.partyId
            && createQuoteInMap.partyId != userLogin.partyId
            && !security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateQuoteFromCart', locale))
    }

    createQuoteInMap.currencyUomId = cart.getCurrency()
    createQuoteInMap.salesChannelEnumId = cart.getChannelType()

    String orderType = cart.getOrderType()
    if (orderType && orderType == 'SALES_ORDER') {
        createQuoteInMap.productStoreId = cart.getProductStoreId()
        createQuoteInMap.quoteTypeId = 'PRODUCT_QUOTE'
    }
    if (orderType && orderType == 'PURCHASE_ORDER') {
        createQuoteInMap.quoteTypeId = 'PURCHASE_QUOTE'
    }

    createQuoteInMap.statusId = 'QUO_CREATED'

    Map serviceResult = run service: 'createQuote', with: createQuoteInMap
    GenericValue quote = from('Quote').where('quoteId', serviceResult.quoteId).queryOne()

    cart.items()?.each { ShoppingCartItem item ->
        Map createQuoteItemInMap = [userLogin: userLogin, locale: locale]
        if (item.getIsPromo()) {
            createQuoteItemInMap.isPromo = 'Y'
        }
        if (item.getConfigWrapper()) {
            createQuoteItemInMap.configId = item.getConfigWrapper().getConfigId()
        }

        if (parameters.applyStorePromotions != 'N' || createQuoteItemInMap.isPromo != 'Y') {
            createQuoteItemInMap.quoteId = quote.quoteId
            createQuoteItemInMap.productId = item.getProductId()
            createQuoteItemInMap.quantity = item.getQuantity()
            createQuoteItemInMap.selectedAmount = item.getSelectedAmount()
            createQuoteItemInMap.quoteUnitPrice = item.getBasePrice()
            createQuoteItemInMap.comments = item.getItemComment()
            createQuoteItemInMap.reservStart = item.getReservStart()
            createQuoteItemInMap.reservLength = item.getReservLength()
            createQuoteItemInMap.reservPersons = item.getReservPersons()

            Map serviceQuoteItemResult = run service: 'createQuoteItem', with: createQuoteItemInMap
            //and the quoteItemSeqId is assigned to the shopping cart item (as orderItemSeqId)
            item.setOrderItemSeqId(serviceQuoteItemResult.quoteItemSeqId)
        }
        if (parameters.applyStorePromotions != 'N') {
            cart.makeAllQuoteAdjustments()?.each { GenericValue adjustment ->
                adjustment.quoteId = quote.quoteId
                adjustment.quoteAdjustmentId = delegator.getNextSeqId('QuoteAdjustment')
                adjustment.create()
            }
        }
    }
    return [successMessage: null, quoteId: quote.quoteId]
}

/**
 * Create a Quote from a Shopping List.
 */
def createQuoteFromShoppingList() {
    Map serviceResult = run service: 'loadCartFromShoppingList', with: parameters
    serviceResult = run service: 'createQuoteFromCart', with: [
        cart: serviceResult.shoppingCart,
        applyStorePromotions: parameters.applyStorePromotions
    ]
    return [successMessage: null, quoteId: serviceResult.quoteId]
}

/**
 * Auto update a QuoteItem price.
 */
def autoUpdateQuotePrice() {
    if (!security.hasEntityPermission('ORDERMGR', '_UPDATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunAutoUpdateQuotePrice', locale))
    }
    GenericValue quoteItem = from('QuoteItem').where(parameters).queryOne()
    if (!quoteItem) {
        return error(UtilProperties.getMessage('OrderUiLabels', 'OrderQuoteItemDoesNotExists', locale))
    }
    if (parameters.manualQuoteUnitPrice) {
        quoteItem.quoteUnitPrice = parameters.manualQuoteUnitPrice
    } else if (parameters.defaultQuoteUnitPrice) {
        quoteItem.quoteUnitPrice = parameters.defaultQuoteUnitPrice
    }
    quoteItem.store()
    return success()
}

/**
 * Create a Quote from a CustRequest.
 */
def createQuoteFromCustRequest() {
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunCreateQuoteFromCustRequest', locale))
    }

    GenericValue custRequest = from('CustRequest').where('custRequestId', parameters.custRequestId).queryOne()

    // Error if request type not equals to RF_QUOTE or RF_PUR_QUOTE
    if (custRequest.custRequestTypeId != 'RF_QUOTE' && custRequest.custRequestTypeId != 'RF_PUR_QUOTE') {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteNotARequest', locale))
    }

    Map createQuoteInMap = [
        partyId: custRequest.fromPartyId,
        productStoreId: custRequest.productStoreId,
        salesChannelEnumId: custRequest.salesChannelEnumId,
        quoteName: custRequest.custRequestName,
        description: custRequest.description,
        currencyUomId: custRequest.maximumAmountUomId,
        statusId: 'QUO_CREATED'
    ]

    // Set the quoteType (product or purchase)
    if (parameters.quoteTypeId) {
        createQuoteInMap.quoteTypeId = parameters.quoteTypeId
    } else if (custRequest.custRequestTypeId == 'RF_QUOTE') {
        createQuoteInMap.quoteTypeId = 'PRODUCT_QUOTE'
    } else {
        createQuoteInMap.quoteTypeId = 'PURCHASE_QUOTE'
    }

    Map serviceResult = run service: 'createQuote', with: createQuoteInMap
    String quoteId = serviceResult.quoteId

    exprdCond = [
            EntityCondition.makeCondition('custRequestId', custRequest.custRequestId),
            EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'CRQ_CANCELLED'),
            EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'CRQ_REJECTED')
    ]
    List custRequestItems = from('CustRequestItem').where(exprdCond).queryList()

    custRequestItems.each { GenericValue custRequestItem ->
        Map serviceCQIResult = run service: 'createQuoteItem', with: [*:custRequestItem, quoteId: quoteId]
    }

    // Roles
    custRequest.getRelated('CustRequestParty', null, null, false)?.each { GenericValue custRequestParty ->
        run service: 'createQuoteRole', with: [
                quoteId: quoteId,
                partyId: custRequestParty.partyId,
                roleTypeId: custRequestParty.roleTypeId
        ]
    }

    return [successMessage: null, quoteId: quoteId]
}

/**
 * Auto create QuoteAdjustments.
 */
def autoCreateQuoteAdjustments() {
    if (!security.hasEntityPermission('ORDERMGR', '_CREATE', userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunAutoCreateQuoteAdjustments', locale))
    }
    String quoteId = parameters.quoteId
    GenericValue quote = from('Quote').where('quoteId', quoteId).queryOne()
    if (!quote) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderQuoteDoesNotExists', locale))
    }

    // All existing promo quote items are removed.
    quote.getRelated('QuoteItem', [isPromo: 'Y'], null, false)?.each { GenericValue quoteItem ->
        run service: 'removeQuoteItem', with: [*: quoteItem]
    }

    // All existing auto quote adjustments are removed.
    quote.getRelated('QuoteAdjustment', null, null, false)?.each { GenericValue quoteAdjustment ->
        // Make sure this is not a manual adjustments
        if (quoteAdjustment.productPromoId) {
            run service: 'removeQuoteAdjustment', with: [*: quoteAdjustment]
        }
    }

    Map serviceResult = run service: 'loadCartFromQuote', with: [*: parameters, applyQuoteAdjustments: false]
    ShoppingCart shoppingCart = (ShoppingCart) serviceResult.shoppingCart

    shoppingCart.items()?.each { ShoppingCartItem item ->
        String orderItemSeqId = item.getOrderItemSeqId()
        if (!orderItemSeqId) {
            // This is a new (promo) item, a new quote item is created
            serviceResult = run service: 'createQuoteItem', with: [
                quoteId: quoteId,
                quantity: item.getQuantity(),
                productId: item.getProductId(),
                isPromo: 'Y'
            ]
            // and the quoteItemSeqId is assigned to the shopping cart item (as orderItemSeqId).
            item.setOrderItemSeqId(serviceResult.quoteItemSeqId)
        }
    }

    // Set the quoteUnitPrice from the item basePrice.
    quote.getRelated('QuoteItem', null, null, false)?.each { GenericValue quoteItem ->
        if (!quoteItem.quoteUnitPrice || quoteItem.quoteUnitPrice == 0) {
            ShoppingCartItem item = shoppingCart.findCartItem(quoteItem.quoteItemSeqId)
            if (item) {
                quoteItem.quoteUnitPrice = item.getBasePrice()
                run service: 'updateQuoteItem', with: [*: quoteItem]
            }
        }
    }
    shoppingCart.makeAllQuoteAdjustments()?.each { GenericValue adjustment ->
        adjustment.quoteId = quoteId
        run service: 'createQuoteAdjustment', with: [*: adjustment]
    }
    return success()
}

/**
 * Create a new Note associated with a Quote
 */
def createQuoteNote() {
    // Passed in field will be noteInfo, which matches entity, but service expects field called note.
    Map serviceContext = dctx.makeValidContext('createNote', ModelService.IN_PARAM, [*: parameters, note: parameters.noteInfo])
    Map serviceResult = dispatcher.runSync('createNote', serviceContext)
    if (ServiceUtil.isError(serviceResult)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderProblemCreatingTheNoteNoNoteIdReturned', locale))
    }
    GenericValue quoteNote = makeValue('QuoteNote')
    quoteNote.quoteId = parameters.quoteId
    quoteNote.noteId = serviceResult.noteId
    quoteNote.create()
    return success()
}

/**
 * Create a Quote adjustment
 */
def createQuoteAdjustment() {
    GenericValue quoteAdjustment = makeValue('QuoteAdjustment', parameters)
    quoteAdjustment.quoteAdjustmentId = delegator.getNextSeqId("QuoteAdjustment")
    quoteAdjustment.createdByUserLogin = userLogin.userLoginId
    quoteAdjustment.createdDate = UtilDateTime.nowTimestamp()
    quoteAdjustment.create()
    return [successMessage: null, quoteAdjustmentId: quoteAdjustment.quoteAdjustmentId]
}

