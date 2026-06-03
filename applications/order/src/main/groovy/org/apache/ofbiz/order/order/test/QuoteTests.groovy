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
package org.apache.ofbiz.order.order.test

import static org.apache.ofbiz.entity.condition.EntityComparisonOperator.GREATER_THAN_EQUAL_TO

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class QuoteTests extends OFBizTestCase {

    QuoteTests(String name) {
        super(name)
    }

    void testCreateQuoteWorkEffort() {
        GenericValue userLogin = getUserLogin('DemoRepStore')

        String quoteId = '9001'
        String workEffortId = '9007'

        Map serviceCtx = [userLogin: userLogin, quoteId: quoteId, workEffortId: workEffortId]
        Map serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', serviceCtx)

        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId == serviceCtx.workEffortId

        GenericValue quoteWorkEffort = from('QuoteWorkEffort')
                .where(quoteId: quoteId, workEffortId: workEffortId).queryOne()
        assert quoteWorkEffort
    }

    // Test case for unsuccessfully creating a QuoteWorkEffort record by attempting
    // to use a quoteId and workEffortId that has already been used in an existing
    // QuoteWorkEffortRecord.
    void testCreateQuoteWorkEffortFail() {
        Timestamp startTime = UtilDateTime.nowTimestamp()
        GenericValue userLogin = getUserLogin('DemoRepStore')

        String quoteId = '9001'
        String workEffortId = '9007'

        // Execute the service, note break-on-error is false so that the test
        // itself doesn't fail and we also need a separate transaction so our
        // lookup below doesn't fail due to the rollback
        Map serviceCtx = [userLogin: userLogin, quoteId: quoteId, workEffortId: workEffortId]
        Map serviceResult
        try {
            serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', serviceCtx)
        } catch (Exception e) {
            serviceResult = ServiceUtil.returnError(e.toString())
        }
        assert ServiceUtil.isError(serviceResult)

        // Confirm the database changes, in this case nothing should have changed
        GenericValue quoteWorkEffort = from('QuoteWorkEffort').where(
                EntityCondition.makeCondition(quoteId: quoteId, workEffortId: workEffortId),
                EntityCondition.makeCondition('lastUpdatedStamp', GREATER_THAN_EQUAL_TO, startTime)
                ).queryOne()

        assert !quoteWorkEffort
    }

    void testCheckUpdateQuotestatus() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9001',
        ]

        Map serviceResult = dispatcher.runSync('checkUpdateQuoteStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = from('Quote').where(quoteId: '9001').queryOne()
        assert quote.statusId == 'QUO_ORDERED'
    }

    // Test case for calling createQuoteWorkEffort without a workEffortId which
    // triggers an ECA to create the WorkEffort first.
    void testCreateWorkEffortAndQuoteWorkEffort() {
        GenericValue userLogin = getUserLogin('system')

        // Use the bare minimum inputs necessary to create the work effort as we
        // aren't testing that service, only that it plays well as an ECA.
        Map serviceCtx = [
            currentStatusId: 'ROU_ACTIVE',
            workEffortName: 'Test WorkEffort',
            workEffortTypeId: 'ROUTING',
            quoteId: '9000',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        GenericValue workEffort = from('WorkEffort').where(
                    workEffortId: serviceResult.workEffortId,
                    currentStatusId: serviceCtx.currentStatusId,
                    workEffortName: serviceCtx.workEffortName,
                    workEffortTypeId: serviceCtx.workEffortTypeId
                ).queryOne()
        assert workEffort

        GenericValue quoteWorkEffort = from('QuoteWorkEffort').where(
                    quoteId: serviceCtx.quoteId,
                    workEffortId: serviceResult.workEffortId
                ).queryOne()
        assert quoteWorkEffort
    }

    void testCreateQuote () {
        Map serviceCtx = [
                userLogin: userLogin,
                partyId: 'Company'
        ]
        Map serviceResult = dispatcher.runSync('createQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
        GenericValue quote = from('Quote').where(quoteId: serviceResult.quoteId).queryOne()
        assert quote
    }

    void testUpdateQuote() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9000',
                statusId: 'QUO_APPROVED'
        ]
        Map serviceResult = dispatcher.runSync('updateQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = from('Quote').where(quoteId: '9000').queryOne()
        assert quote.statusId == 'QUO_APPROVED'

        serviceCtx.statusId = 'QUO_CREATED'
        serviceResult = dispatcher.runSync('updateQuote', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    void testCopyQuote() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9000'
        ]
        Map serviceResult = dispatcher.runSync('copyQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    void testCreateQuoteItem() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00004',
                productId: 'GZ-1001'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00004').queryOne()
        assert quoteItem.quoteUnitPrice
    }

    void testUpdateQuoteItem() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00002',
                productId: 'GZ-1001'
        ]
        Map serviceResult = dispatcher.runSync('updateQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00002').queryOne()
        assert quoteItem.productId == 'GZ-1001'
    }

    void testRemoveQuoteItem() {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00002'
        ]
        Map serviceResult = dispatcher.runSync('removeQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00002').queryOne()
        assert !quoteItem
        GenericValue quoteTerm = from('QuoteTerm').where(quoteId: '9000', quoteItemSeqId: '00002', termTypeId: 'FIN_PAYMENT_DISC').queryOne()
        assert !quoteTerm
    }

    void testCreateQuoteTerm () {
        Map serviceCtx = [
                userLogin: userLogin,
                termTypeId: 'FIN_PAYMENT_DISC',
                quoteId: '9000',
                quoteItemSeqId: '00001',
                termValue: 40L,
                termDays: 4L,
                uomId: 'CNY',
                description: 'create quoteTerm'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteTerm', serviceCtx)
        List<GenericValue> terms = from('QuoteTerm')
                .where(termTypeId: 'FIN_PAYMENT_DISC', quoteId: '9000', quoteItemSeqId: '00001').queryList()

        assert ServiceUtil.isSuccess(serviceResult)
        assert terms
        GenericValue term = terms[0]
        assert serviceCtx.termTypeId == term.termTypeId
        assert serviceCtx.termValue == term.termValue
        assert serviceCtx.termDays == term.termDays
        assert serviceCtx.uomId == term.uomId
        assert serviceCtx.description == term.description
    }

    void testUpdateQuoteTerm() {
        Map serviceCtx = [
            termTypeId: 'FIN_PAYMENT_DISC',
            quoteId: '9000',
            quoteItemSeqId: '00002',
            termValue: 30L,
            termDays: 3L,
            uomId: 'CNY',
            description: 'update quoteterm',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateQuoteTerm', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        // Confirm that a matching Quoteterm was updated
        GenericValue quoteTerm = from('QuoteTerm').where(
                    termTypeId: serviceCtx.termTypeId,
                    quoteId: serviceCtx.quoteId,
                    quoteItemSeqId: serviceCtx.quoteItemSeqId
                ).queryOne()
        assert quoteTerm
        assert quoteTerm.termTypeId == serviceCtx.termTypeId
        assert quoteTerm.quoteId == serviceCtx.quoteId
        assert quoteTerm.quoteItemSeqId == serviceCtx.quoteItemSeqId
        assert quoteTerm.termValue == serviceCtx.termValue
        assert quoteTerm.termDays == serviceCtx.termDays
        assert quoteTerm.uomId == serviceCtx.uomId
        assert quoteTerm.description == serviceCtx.description
    }

    void testDeleteQuoteTerm () {
        Map serviceCtx = [
                userLogin: userLogin,
                termTypeId: 'FIN_PAYMENT_DISC',
                quoteId: '9000',
                quoteItemSeqId: '00003'
        ]

        Map serviceResult = dispatcher.runSync('deleteQuoteTerm', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteTerm = from('QuoteTerm')
                .where(termTypeId: serviceCtx.termTypeId, quoteId: serviceCtx.quoteId, quoteItemSeqId: serviceCtx.quoteItemSeqId).queryOne()
        assert !quoteTerm
    }

    void testCreateQuoteAttribute () {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9001',
                attrName: 'Test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteAttribute', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    void testCreateQuoteCoefficient () {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9001',
                coeffName: 'Test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteCoefficient', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    void testGetNextQuoteId () {
        Map serviceCtx = [
                userLogin: userLogin,
                partyId: 'DemoCustomer-1'
        ]

        Map serviceResult = dispatcher.runSync('getNextQuoteId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    void testQuoteSequenceEnforced() {
        GenericValue partyAcctgPreference = from('PartyAcctgPreference').where('partyId', 'DemoCustomer').queryOne()
        Long lastQuoteNumber = partyAcctgPreference.lastQuoteNumber ?: 0

        Map serviceCtx = [
                userLogin: userLogin,
                partyId: 'DemoCustomer',
                partyAcctgPreference: partyAcctgPreference
        ]

        Map serviceResult = dispatcher.runSync('quoteSequenceEnforced', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId == lastQuoteNumber + 1L
    }

    void testCopyQuoteItem () {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9001',
                quoteItemSeqId: '00001',
                quoteIdTo: '9001',
                quoteItemSeqIdTo: '00002',
                copyQuoteAdjustments: 'Y'
        ]

        Map serviceResult = dispatcher.runSync('copyQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteAdjustment = from('QuoteAdjustment')
                .where('quoteId', '9001', 'quoteItemSeqId', '00002', 'quoteAdjustmentTypeId', 'SALES_TAX').queryFirst()
        assert quoteAdjustment
    }

    void testCreateQuoteAndQuoteItemForRequest () {
        Map serviceCtx = [
                userLogin: userLogin,
                custRequestId: '9000',
                custRequestItemSeqId: '00001'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteAndQuoteItemForRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestItemSeqId', '00001').queryFirst()
        assert quoteItem
    }

    @SuppressWarnings('UnnecessaryObjectReferences')
    void testCreateQuoteFromCart() {
        String productId = 'SV-1001'
        String partyId = 'DemoCustomer'

        ShoppingCart cart = new ShoppingCart(delegator, '9000', Locale.getDefault(), 'USD')
        cart.setOrderType('SALES_ORDER')
        cart.setChannelType('WEB_SALES_CHANNEL')
        cart.setBillToCustomerPartyId(partyId)
        cart.setPlacingCustomerPartyId(partyId)
        cart.setShipToCustomerPartyId(partyId)
        cart.setEndUserCustomerPartyId(partyId)
        cart.setUserLogin(userLogin, dispatcher)
        cart.addOrIncreaseItem(productId, null, BigDecimal.ONE, null, null, null,
                null, null, null, null, 'DemoCatalog', null, null,
                null, null, dispatcher)
        cart.setDefaultCheckoutOptions(dispatcher)

        Map serviceCtx = [
            userLogin: userLogin,
            cart: cart,
            applyStorePromotions: 'Y'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCart', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', productId).queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    void testCreateQuoteFromShoppingList() {
        Map serviceCtx = [
            userLogin: userLogin,
            shoppingListId: '9000',
            applyStorePromotions: 'Y'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromShoppingList', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', 'SV-1001').queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    void testAutoUpdateQuotePrice() {
        Map serviceCtx = [
            userLogin: userLogin,
            quoteId: '9000',
            quoteItemSeqId: '00001',
            defaultQuoteUnitPrice: BigDecimal.valueOf(12)
        ]
        Map serviceResult = dispatcher.runSync('autoUpdateQuotePrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', '9000', 'quoteItemSeqId', '00001').queryOne()
        assert quoteItem.quoteUnitPrice == 12
    }

    void testCreateQuoteFromCustRequest () {
        Map serviceCtx = [
                userLogin: userLogin,
                custRequestId: '9000'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestId', '9000').queryFirst()
        assert quoteItem
    }

    void testAutoCreateQuoteAdjustments () {
        Map serviceCtx = [
            userLogin: userLogin,
            quoteId: '9001'
        ]
        Map serviceResult = dispatcher.runSync('autoCreateQuoteAdjustments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue promoQuoteAdjustment = from('QuoteAdjustment')
                .where('quoteId', '9001', 'quoteAdjustmentTypeId', 'PROMOTION_ADJUSTMENT').queryFirst()
        assert promoQuoteAdjustment
    }

    void testCreateQuoteNote () {
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: '9001',
                noteName: 'Test Note',
                noteInfo: 'This is a test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
