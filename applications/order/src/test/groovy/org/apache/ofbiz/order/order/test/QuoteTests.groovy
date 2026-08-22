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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class QuoteTests implements JupiterTestHelper {

    @Test
    @Order(1)
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
    @Test
    @Order(2)
    void testCreateQuoteWorkEffortFail() {
        GenericValue userLogin = getUserLogin('DemoRepStore')

        String quoteId = '9001'
        String workEffortId = '9007'

        // Capture the record as it stands before this test's own call, so the comparison below
        // doesn't depend on wall-clock timestamps (racy when tests run within the same millisecond)
        GenericValue quoteWorkEffortBefore = from('QuoteWorkEffort')
                .where(quoteId: quoteId, workEffortId: workEffortId).queryOne()

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
        GenericValue quoteWorkEffort = from('QuoteWorkEffort')
                .where(quoteId: quoteId, workEffortId: workEffortId).queryOne()

        assert quoteWorkEffort == quoteWorkEffortBefore
    }

    @Test
    @Order(3)
    void testCheckUpdateQuotestatus() {
        String quoteId = testParams.quoteId ?: '9001'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
        ]

        Map serviceResult = dispatcher.runSync('checkUpdateQuoteStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = from('Quote').where(quoteId: quoteId).queryOne()
        assert quote.statusId == 'QUO_ORDERED'
    }

    // Test case for calling createQuoteWorkEffort without a workEffortId which
    // triggers an ECA to create the WorkEffort first.
    @Test
    @Order(4)
    void testCreateWorkEffortAndQuoteWorkEffort() {
        String currentStatusId = testParams.currentStatusId ?: 'ROU_ACTIVE'
        String workEffortName = testParams.workEffortName ?: 'Test WorkEffort'
        String workEffortTypeId = testParams.workEffortTypeId ?: 'ROUTING'
        String quoteId = testParams.quoteId ?: '9000'
        GenericValue userLogin = getUserLogin('system')

        // Use the bare minimum inputs necessary to create the work effort as we
        // aren't testing that service, only that it plays well as an ECA.
        Map serviceCtx = [
            currentStatusId: currentStatusId,
            workEffortName: workEffortName,
            workEffortTypeId: workEffortTypeId,
            quoteId: quoteId,
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

    @Test
    @Order(5)
    void testCreateQuote() {
        String partyId = testParams.partyId ?: 'Company'
        Map serviceCtx = [
                userLogin: userLogin,
                partyId: partyId
        ]
        Map serviceResult = dispatcher.runSync('createQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
        GenericValue quote = from('Quote').where(quoteId: serviceResult.quoteId).queryOne()
        assert quote
    }

    @Test
    @Order(6)
    void testUpdateQuote() {
        String quoteId = testParams.quoteId ?: '9000'
        String statusId = testParams.statusId ?: 'QUO_APPROVED'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                statusId: statusId
        ]
        Map serviceResult = dispatcher.runSync('updateQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = from('Quote').where(quoteId: quoteId).queryOne()
        assert quote.statusId == statusId

        serviceCtx.statusId = 'QUO_CREATED'
        serviceResult = dispatcher.runSync('updateQuote', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    @Test
    @Order(7)
    void testCopyQuote() {
        String quoteId = testParams.quoteId ?: '9000'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId
        ]
        Map serviceResult = dispatcher.runSync('copyQuote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    @Test
    @Order(8)
    void testCreateQuoteItem() {
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00004'
        String productId = testParams.productId ?: 'GZ-1001'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId,
                productId: productId
        ]
        Map serviceResult = dispatcher.runSync('createQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: quoteId, quoteItemSeqId: quoteItemSeqId).queryOne()
        assert quoteItem.quoteUnitPrice
    }

    @Test
    @Order(9)
    void testUpdateQuoteItem() {
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00002'
        String productId = testParams.productId ?: 'GZ-1001'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId,
                productId: productId
        ]
        Map serviceResult = dispatcher.runSync('updateQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: quoteId, quoteItemSeqId: quoteItemSeqId).queryOne()
        assert quoteItem.productId == productId
    }

    @Test
    // Must run after testUpdateQuoteTerm: this removes QuoteItem 9000/00002, which cascades to
    // delete its QuoteTerm - testUpdateQuoteTerm updates that same QuoteTerm and fails
    // ("Value not found, cannot update") if it no longer exists.
    @Order(12)
    void testRemoveQuoteItem() {
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00002'
        String termTypeId = testParams.termTypeId ?: 'FIN_PAYMENT_DISC'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId
        ]
        Map serviceResult = dispatcher.runSync('removeQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where(quoteId: quoteId, quoteItemSeqId: quoteItemSeqId).queryOne()
        assert !quoteItem
        GenericValue quoteTerm = from('QuoteTerm').where(quoteId: quoteId, quoteItemSeqId: quoteItemSeqId, termTypeId: termTypeId).queryOne()
        assert !quoteTerm
    }

    @Test
    @Order(11)
    void testCreateQuoteTerm() {
        String termTypeId = testParams.termTypeId ?: 'FIN_PAYMENT_DISC'
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00001'
        String uomId = testParams.uomId ?: 'CNY'
        String description = testParams.description ?: 'create quoteTerm'
        Map serviceCtx = [
                userLogin: userLogin,
                termTypeId: termTypeId,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId,
                termValue: 40L,
                termDays: 4L,
                uomId: uomId,
                description: description
        ]

        Map serviceResult = dispatcher.runSync('createQuoteTerm', serviceCtx)
        List<GenericValue> terms = from('QuoteTerm')
                .where(termTypeId: termTypeId, quoteId: quoteId, quoteItemSeqId: quoteItemSeqId).queryList()

        assert ServiceUtil.isSuccess(serviceResult)
        assert terms
        GenericValue term = terms[0]
        assert serviceCtx.termTypeId == term.termTypeId
        assert serviceCtx.termValue == term.termValue
        assert serviceCtx.termDays == term.termDays
        assert serviceCtx.uomId == term.uomId
        assert serviceCtx.description == term.description
    }

    @Test
    @Order(10)
    void testUpdateQuoteTerm() {
        String termTypeId = testParams.termTypeId ?: 'FIN_PAYMENT_DISC'
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00002'
        String uomId = testParams.uomId ?: 'CNY'
        String description = testParams.description ?: 'update quoteterm'
        Map serviceCtx = [
            termTypeId: termTypeId,
            quoteId: quoteId,
            quoteItemSeqId: quoteItemSeqId,
            termValue: 30L,
            termDays: 3L,
            uomId: uomId,
            description: description,
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

    @Test
    @Order(13)
    void testDeleteQuoteTerm() {
        String termTypeId = testParams.termTypeId ?: 'FIN_PAYMENT_DISC'
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00003'
        Map serviceCtx = [
                userLogin: userLogin,
                termTypeId: termTypeId,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId
        ]

        Map serviceResult = dispatcher.runSync('deleteQuoteTerm', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteTerm = from('QuoteTerm')
                .where(termTypeId: serviceCtx.termTypeId, quoteId: serviceCtx.quoteId, quoteItemSeqId: serviceCtx.quoteItemSeqId).queryOne()
        assert !quoteTerm
    }

    @Test
    @Order(14)
    void testCreateQuoteAttribute() {
        String quoteId = testParams.quoteId ?: '9001'
        String attrName = testParams.attrName ?: 'Test'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                attrName: attrName
        ]

        Map serviceResult = dispatcher.runSync('createQuoteAttribute', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(15)
    void testCreateQuoteCoefficient() {
        String quoteId = testParams.quoteId ?: '9001'
        String coeffName = testParams.coeffName ?: 'Test'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                coeffName: coeffName
        ]

        Map serviceResult = dispatcher.runSync('createQuoteCoefficient', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(16)
    void testGetNextQuoteId() {
        String partyId = testParams.partyId ?: 'DemoCustomer-1'
        Map serviceCtx = [
                userLogin: userLogin,
                partyId: partyId
        ]

        Map serviceResult = dispatcher.runSync('getNextQuoteId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    @Test
    @Order(17)
    void testQuoteSequenceEnforced() {
        String partyId = testParams.partyId ?: 'DemoCustomer'
        GenericValue partyAcctgPreference = from('PartyAcctgPreference').where('partyId', partyId).queryOne()
        Long lastQuoteNumber = partyAcctgPreference.lastQuoteNumber ?: 0

        Map serviceCtx = [
                userLogin: userLogin,
                partyId: partyId,
                partyAcctgPreference: partyAcctgPreference
        ]

        Map serviceResult = dispatcher.runSync('quoteSequenceEnforced', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId == lastQuoteNumber + 1L
    }

    @Test
    @Order(18)
    void testCopyQuoteItem() {
        String quoteId = testParams.quoteId ?: '9001'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00001'
        String quoteIdTo = testParams.quoteIdTo ?: '9001'
        String quoteItemSeqIdTo = testParams.quoteItemSeqIdTo ?: '00002'
        String copyQuoteAdjustments = testParams.copyQuoteAdjustments ?: 'Y'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId,
                quoteIdTo: quoteIdTo,
                quoteItemSeqIdTo: quoteItemSeqIdTo,
                copyQuoteAdjustments: copyQuoteAdjustments
        ]

        Map serviceResult = dispatcher.runSync('copyQuoteItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteAdjustment = from('QuoteAdjustment')
                .where('quoteId', quoteId, 'quoteItemSeqId', quoteItemSeqIdTo, 'quoteAdjustmentTypeId', 'SALES_TAX').queryFirst()
        assert quoteAdjustment
    }

    @Test
    @Order(19)
    void testCreateQuoteAndQuoteItemForRequest() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestItemSeqId = testParams.custRequestItemSeqId ?: '00001'
        Map serviceCtx = [
                userLogin: userLogin,
                custRequestId: custRequestId,
                custRequestItemSeqId: custRequestItemSeqId
        ]
        Map serviceResult = dispatcher.runSync('createQuoteAndQuoteItemForRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestItemSeqId', custRequestItemSeqId).queryFirst()
        assert quoteItem
    }

    @SuppressWarnings('UnnecessaryObjectReferences')
    @Test
    @Order(20)
    void testCreateQuoteFromCart() {
        String applyStorePromotions = testParams.applyStorePromotions ?: 'Y'
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
            applyStorePromotions: applyStorePromotions
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCart', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', productId).queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    @Test
    @Order(21)
    void testCreateQuoteFromShoppingList() {
        String shoppingListId = testParams.shoppingListId ?: '9000'
        String applyStorePromotions = testParams.applyStorePromotions ?: 'Y'
        Map serviceCtx = [
            userLogin: userLogin,
            shoppingListId: shoppingListId,
            applyStorePromotions: applyStorePromotions
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromShoppingList', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', 'SV-1001').queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    @Test
    @Order(22)
    void testAutoUpdateQuotePrice() {
        String quoteId = testParams.quoteId ?: '9000'
        String quoteItemSeqId = testParams.quoteItemSeqId ?: '00001'
        Map serviceCtx = [
            userLogin: userLogin,
            quoteId: quoteId,
            quoteItemSeqId: quoteItemSeqId,
            defaultQuoteUnitPrice: BigDecimal.valueOf(12)
        ]
        Map serviceResult = dispatcher.runSync('autoUpdateQuotePrice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', quoteId, 'quoteItemSeqId', quoteItemSeqId).queryOne()
        assert quoteItem.quoteUnitPrice == 12
    }

    @Test
    @Order(23)
    void testCreateQuoteFromCustRequest() {
        String custRequestId = testParams.custRequestId ?: '9000'
        Map serviceCtx = [
                userLogin: userLogin,
                custRequestId: custRequestId
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestId', custRequestId).queryFirst()
        assert quoteItem
    }

    @Test
    @Order(24)
    void testAutoCreateQuoteAdjustments() {
        String quoteId = testParams.quoteId ?: '9001'
        Map serviceCtx = [
            userLogin: userLogin,
            quoteId: quoteId
        ]
        Map serviceResult = dispatcher.runSync('autoCreateQuoteAdjustments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue promoQuoteAdjustment = from('QuoteAdjustment')
                .where('quoteId', quoteId, 'quoteAdjustmentTypeId', 'PROMOTION_ADJUSTMENT').queryFirst()
        assert promoQuoteAdjustment
    }

    @Test
    @Order(25)
    void testCreateQuoteNote() {
        String quoteId = testParams.quoteId ?: '9001'
        String noteName = testParams.noteName ?: 'Test Note'
        String noteInfo = testParams.noteInfo ?: 'This is a test'
        Map serviceCtx = [
                userLogin: userLogin,
                quoteId: quoteId,
                noteName: noteName,
                noteInfo: noteInfo
        ]

        Map serviceResult = dispatcher.runSync('createQuoteNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
