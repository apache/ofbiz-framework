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
package org.apache.ofbiz.order

import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.Debug

import java.sql.Timestamp

import static org.apache.ofbiz.base.util.UtilDateTime.nowTimestamp
import static org.apache.ofbiz.entity.condition.EntityCondition.makeCondition
import static org.apache.ofbiz.entity.condition.EntityComparisonOperator.GREATER_THAN_EQUAL_TO

class QuoteTests extends OFBizTestCase {
    public QuoteTests(String name) {
        super(name)
    }

    // Retrieves a particular login record.
    private GenericValue getUserLogin(String userLoginId) {
        GenericValue userLogin = EntityQuery.use(delegator)
                .from('UserLogin').where(userLoginId: userLoginId).queryOne()
        assert userLogin
        return userLogin
    }

    // Test case for successfully creating a QuoteWorkEffort record.
    void testCreateQuoteWorkEffort() {
        GenericValue userLogin = getUserLogin('DemoRepStore')

        def quoteId = '9001'
        def workEffortId = '9007'

        def input = [userLogin: userLogin, quoteId: quoteId, workEffortId: workEffortId]
        Map serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', input)

        // Confirm the service output parameters.
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId == input.workEffortId

        // Confirm the database changes.
        GenericValue quoteWorkEffort = EntityQuery.use(delegator)
                .from('QuoteWorkEffort').where(quoteId: quoteId, workEffortId: workEffortId).queryOne()
        assert quoteWorkEffort
    }

    // Test case for unsuccessfully creating a QuoteWorkEffort record by attempting
    // to use a quoteId and workEffortId that has already been used in an existing
    // QuoteWorkEffortRecord.
    void testCreateQuoteWorkEffortFail() {
        // Use to confirm nothing has changed at the end of the test
        Timestamp startTime = nowTimestamp()
        GenericValue userLogin = getUserLogin('DemoRepStore')

        def quoteId = '9001'
        def workEffortId = '9007'

        // Execute the service, note break-on-error is false so that the test
        // itself doesn't fail and we also need a separate transaction so our
        // lookup below doesn't fail due to the rollback
        def input = [userLogin: userLogin, quoteId: quoteId, workEffortId: workEffortId]
        Map serviceResult
        try {
            serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', input)
        } catch (Exception e) {
            serviceResult = ServiceUtil.returnError(e.toString())
        }
        assert ServiceUtil.isError(serviceResult)

        // Confirm the database changes, in this case nothing should have changed
        GenericValue quoteWorkEffort = EntityQuery.use(delegator)
                .from('QuoteWorkEffort').where(
                    makeCondition(quoteId: quoteId, workEffortId: workEffortId),
                    makeCondition('lastUpdatedStamp', GREATER_THAN_EQUAL_TO, startTime)
                ).queryOne()

        assert !quoteWorkEffort
    }

    // Test case for CheckUpdateQuotestatus
    void testCheckUpdateQuotestatus() {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                quoteId: '9001',
        ]

        Map serviceResult = dispatcher.runSync('checkUpdateQuoteStatus', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = EntityQuery.use(delegator).from('Quote').where(quoteId: '9001').queryOne()
        assert quote.statusId == 'QUO_ORDERED'
    }

    // Test case for calling createQuoteWorkEffort without a workEffortId which
    // triggers an ECA to create the WorkEffort first.
    void testCreateWorkEffortAndQuoteWorkEffort() {
        GenericValue userLogin = getUserLogin('flexadmin')

        // Use the bare minimum inputs necessary to create the work effort as we
        // aren't testing that service, only that it plays well as an ECA.
        def input = [
            currentStatusId: 'ROU_ACTIVE',
            workEffortName: 'Test WorkEffort',
            workEffortTypeId: 'ROUTING',
            quoteId: '9000',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('ensureWorkEffortAndCreateQuoteWorkEffort', input)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        // Confirm that a matching WorkEffort was created.
        GenericValue workEfforts = EntityQuery.use(delegator)
                .from('WorkEffort').where(
                    workEffortId: serviceResult.workEffortId,
                    currentStatusId: input.currentStatusId,
                    workEffortName: input.workEffortName,
                    workEffortTypeId: input.workEffortTypeId
                ).queryOne()
        assert workEfforts

        GenericValue quoteWorkEffort = EntityQuery.use(delegator)
                .from('WorkEffort').where(
                    quoteId: input.quoteId,
                    workEffortId: serviceResult.workEffortId
                ).queryOne()
        assert quoteWorkEffort
    }

    // Test createQuote service
    void testCreateQuote () {
        GenericValue userLogin = getUserLogin('system')
        Map input = [
                userLogin: userLogin,
                partyId: 'Company'
        ]
        Map serviceResult = dispatcher.runSync('createQuote', input)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
        GenericValue quote = EntityQuery.use(delegator).from('Quote').where(quoteId: serviceResult.quoteId).queryOne()
        assert quote
    }

    // Test updateQuote service
    void testUpdateQuote() {
        GenericValue userLogin = getUserLogin('system')
        Map input = [
                userLogin: userLogin,
                quoteId: '9000',
                statusId: 'QUO_APPROVED'
        ]
        Map serviceResult = dispatcher.runSync('updateQuote', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quote = EntityQuery.use(delegator).from('Quote').where(quoteId: '9000').queryOne()
        assert quote.statusId == 'QUO_APPROVED'

        input.statusId = 'QUO_CREATED'
        serviceResult = dispatcher.runSync('updateQuote', input)
        assert ServiceUtil.isError(serviceResult)
    }

    // Test copyQuote service
    void testCopyQuote() {
        GenericValue userLogin = getUserLogin('system')
        Map input = [
                userLogin: userLogin,
                quoteId: '9000'
        ]
        Map serviceResult = dispatcher.runSync('copyQuote', input)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    // Test createQuoteItem service
    void testCreateQuoteItem() {
        GenericValue userLogin = getUserLogin('system')
        Map input = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00004',
                productId: 'GZ-1001'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteItem', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00004').queryOne()
        assert quoteItem.quoteUnitPrice
    }

    // Test updateQuoteItem service
    void testUpdateQuoteItem() {
        GenericValue userLogin = getUserLogin('system')

        Map input = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00002',
                productId: 'GZ-1001'
        ]
        Map serviceResult = dispatcher.runSync('updateQuoteItem', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00002').queryOne()
        assert quoteItem.productId == 'GZ-1001'
    }

    // Test removeQuoteItem service
    void testRemoveQuoteItem() {
        GenericValue userLogin = getUserLogin('system')

        Map input = [
                userLogin: userLogin,
                quoteId: '9000',
                quoteItemSeqId: '00002'
        ]
        Map serviceResult = dispatcher.runSync('removeQuoteItem', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where(quoteId: '9000', quoteItemSeqId: '00002').queryOne()
        assert !quoteItem
        GenericValue quoteTerm = EntityQuery.use(delegator).from('QuoteTerm').where(quoteId: '9000', quoteItemSeqId: '00002', termTypeId: 'FIN_PAYMENT_DISC').queryOne()
        assert !quoteTerm
    }

    // test create a Term
    void testCreateQuoteTerm () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                termTypeId: 'FIN_PAYMENT_DISC',
                quoteId: '9000',
                quoteItemSeqId: '00001',
                termValue: 40L,
                termDays: 4L,
                uomId: 'CNY',
                description: 'create quoteTerm'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteTerm', input)
        List<GenericValue> terms = EntityQuery.use(delegator).from('QuoteTerm')
                .where(termTypeId: 'FIN_PAYMENT_DISC', quoteId: '9000', quoteItemSeqId: '00001').queryList()

        assert ServiceUtil.isSuccess(serviceResult)
        assert terms
        GenericValue term = terms[0]
        assert input.termTypeId == term.termTypeId
        assert input.termValue == term.termValue
        assert input.termDays == term.termDays
        assert input.uomId == term.uomId
        assert input.description == term.description
    }

    // Update a term.
    void testUpdateQuoteTerm() {
        GenericValue userLogin = getUserLogin('system')
        def input = [
            termTypeId: 'FIN_PAYMENT_DISC',
            quoteId: '9000',
            quoteItemSeqId: '00002',
            termValue: 30L,
            termDays: 3L,
            uomId: 'CNY',
            description: 'update quoteterm',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateQuoteTerm', input)
        assert ServiceUtil.isSuccess(serviceResult)

        // Confirm that a matching Quoteterm was updated
        GenericValue quoteTerm = EntityQuery.use(delegator)
                .from('QuoteTerm').where(
                    termTypeId: input.termTypeId,
                    quoteId: input.quoteId,
                    quoteItemSeqId: input.quoteItemSeqId
                ).queryOne()
        assert quoteTerm
        assert quoteTerm.termTypeId == input.termTypeId
        assert quoteTerm.quoteId == input.quoteId
        assert quoteTerm.quoteItemSeqId == input.quoteItemSeqId
        assert quoteTerm.termValue == input.termValue
        assert quoteTerm.termDays == input.termDays
        assert quoteTerm.uomId == input.uomId
        assert quoteTerm.description == input.description
    }

    // delete a term
    void testDeleteQuoteTerm () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                termTypeId: 'FIN_PAYMENT_DISC',
                quoteId: '9000',
                quoteItemSeqId: '00003'
        ]

        Map serviceResult = dispatcher.runSync('deleteQuoteTerm', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteTerm = EntityQuery.use(delegator).from('QuoteTerm').where(termTypeId: serviceResult.termTypeId, quoteId: serviceResult.quoteId, quoteItemSeqId: serviceResult.quoteItemSeqId).queryOne()
        assert !quoteTerm
    }

    // Create Quote Attribute
    void testCreateQuoteAttribute () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                quoteId: '9001',
                attrName: 'Test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteAttribute', input)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    // Create Quote Coefficient
    void testCreateQuoteCoefficient () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                quoteId: '9001',
                coeffName: 'Test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteCoefficient', input)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    // Get Next Quote Id
    void testGetNextQuoteId () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                partyId: 'DemoCustomer-1'
        ]

        Map serviceResult = dispatcher.runSync('getNextQuoteId', input)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId
    }

    // Test Quote Sequence Enforced
    void testQuoteSequenceEnforced() {
        GenericValue userLogin = getUserLogin('system')
        GenericValue partyAcctgPreference = EntityQuery.use(delegator)
                .from('PartyAcctgPreference').where('partyId', 'DemoCustomer').queryOne()
        Long lastQuoteNumber = partyAcctgPreference.lastQuoteNumber
        if (!lastQuoteNumber) {
            lastQuoteNumber = 0
        }

        def input = [
                userLogin: userLogin,
                partyId: 'DemoCustomer',
                partyAcctgPreference: partyAcctgPreference
        ]

        Map serviceResult = dispatcher.runSync('quoteSequenceEnforced', input)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quoteId == lastQuoteNumber +1L
    }

    // Copy Quote Item
    void testCopyQuoteItem () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                quoteId: '9001',
                quoteItemSeqId: '00001',
                quoteIdTo: '9001',
                quoteItemSeqIdTo: '00002',
                copyQuoteAdjustments: 'Y'
        ]

        Map serviceResult = dispatcher.runSync('copyQuoteItem', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteAdjustment = EntityQuery.use(delegator).from('QuoteAdjustment').where('quoteId', '9001', 'quoteItemSeqId', '00002', 'quoteAdjustmentTypeId', 'SALES_TAX').queryFirst()
        assert quoteAdjustment
    }

    // Test createQuoteAndQuoteItemForRequest
    void testCreateQuoteAndQuoteItemForRequest () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                custRequestId: '9000',
                custRequestItemSeqId: '00001'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteAndQuoteItemForRequest', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestItemSeqId', '00001').queryFirst()
        assert quoteItem
    }

    // Test createQuoteFromCart
    void testCreateQuoteFromCart() {
        GenericValue userLogin = getUserLogin('system')
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

        def input = [
            userLogin: userLogin,
            cart: cart,
            applyStorePromotions: 'Y'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCart', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', productId).queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = EntityQuery.use(delegator).from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    // Test createQuoteFromShoppingList
    void testCreateQuoteFromShoppingList() {
        GenericValue userLogin = getUserLogin('system')
        def input = [
            userLogin: userLogin,
            shoppingListId: '9000',
            applyStorePromotions: 'Y'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromShoppingList', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where('quoteId', serviceResult.quoteId, 'productId', 'SV-1001').queryFirst()
        assert quoteItem
        GenericValue quoteAdjustment = EntityQuery.use(delegator).from('QuoteAdjustment').where('quoteId', serviceResult.quoteId).queryFirst()
        assert quoteAdjustment
    }

    // Test autoUpdateQuotePrice
    void testAutoUpdateQuotePrice() {
        GenericValue userLogin = getUserLogin('system')
        def input = [
            userLogin: userLogin,
            quoteId: '9000',
            quoteItemSeqId: '00001',
            defaultQuoteUnitPrice: BigDecimal.valueOf(12)
        ]
        Map serviceResult = dispatcher.runSync('autoUpdateQuotePrice', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where('quoteId', '9000', 'quoteItemSeqId', '00001').queryOne()
        assert quoteItem.quoteUnitPrice == 12
    }

    // Test createQuoteFromCustRequest
    void testCreateQuoteFromCustRequest () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                custRequestId: '9000'
        ]
        Map serviceResult = dispatcher.runSync('createQuoteFromCustRequest', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue quoteItem = EntityQuery.use(delegator).from('QuoteItem').where('quoteId', serviceResult.quoteId, 'custRequestId', '9000').queryFirst()
        assert quoteItem
    }

    // Test autoCreateQuoteAdjustments
    void testAutoCreateQuoteAdjustments () {
        GenericValue userLogin = EntityQuery.use(delegator)
        .from('UserLogin').where(userLoginId: 'system').queryOne()
        assert userLogin
        GenericValue quote = EntityQuery.use(delegator)
        .from('Quote').where(quoteId: '9001').queryOne()

        def input = [
            userLogin: userLogin,
            quoteId: '9001'
        ]
        Map serviceResult = dispatcher.runSync('autoCreateQuoteAdjustments', input)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue promoQuoteAdjustment = EntityQuery.use(delegator).from('QuoteAdjustment').where('quoteId', '9001', 'quoteAdjustmentTypeId', 'PROMOTION_ADJUSTMENT').queryFirst()
        assert promoQuoteAdjustment
    }

    // Create Quote Note
    void testCreateQuoteNote () {
        GenericValue userLogin = getUserLogin('system')
        def input = [
                userLogin: userLogin,
                quoteId: '9001',
                noteName: 'Test Note',
                noteInfo: 'This is a test'
        ]

        Map serviceResult = dispatcher.runSync('createQuoteNote', input)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
