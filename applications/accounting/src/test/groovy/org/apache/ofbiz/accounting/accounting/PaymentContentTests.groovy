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
package org.apache.ofbiz.accounting.accounting

import java.sql.Timestamp
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

// updatePaymentContent() has no other caller anywhere in the codebase (only reachable by direct
// service invocation), so unlike the other update() DSL conversions it had no existing test file
// to add coverage to - this class is its dedicated home.
@JunitJupiterTest
class PaymentContentTests implements JupiterTestHelper {

    // Regression coverage for the update() DSL / ServiceErrorException catch site added in
    // updatePaymentContent(): a PK with no matching PaymentContent record must come back as a
    // service error carrying the original plain-string "Error getting Payment Content" message,
    // not the generic EntityUpdateBuilder message and not a silently-successful result.
    @Test
    @Order(1)
    void testUpdatePaymentContentNotFound() {
        String paymentId = testParams.paymentId ?: 'TEST_NONEXISTENT_PAYMENT'
        String paymentContentTypeId = testParams.paymentContentTypeId ?: 'COMMENTS'
        String contentId = testParams.contentId ?: 'TEST_NONEXISTENT_CONTENT'
        Timestamp fromDate = UtilDateTime.toTimestamp('01/01/2099 00:00:00')
        Map serviceCtx = [
                paymentId: paymentId,
                paymentContentTypeId: paymentContentTypeId,
                contentId: contentId,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePaymentContent', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
        assert ServiceUtil.getErrorMessage(serviceResult) == 'Error getting Payment Content'
    }

    // updatePaymentContent() had no coverage at all before this - not even a happy-path test.
    // Builds its own Payment/PaymentContentType/Content/PaymentContent fixture chain directly
    // (rather than depending on another testdef suite's data-load - each <test-suite> in
    // accounting's ofbiz-component.xml runs in its own isolated, rolled-back transaction, so a
    // fixture loaded by one suite's entity-xml is not visible to a different suite), then calls
    // the service and verifies both the PaymentContent update and the follow-on updateContent
    // call persisted.
    @Test
    @Order(2)
    void testUpdatePaymentContent() {
        String paymentContentTypeId = testParams.paymentContentTypeId ?: 'TEST_PMT_CNT_TYPE'
        delegator.createOrStore(delegator.makeValue('PaymentContentType', [paymentContentTypeId: paymentContentTypeId]))

        String paymentId = testParams.paymentId ?: 'TEST_PMT_CONTENT'
        GenericValue payment = delegator.makeValue('Payment', [
                paymentId: paymentId,
                paymentTypeId: 'CUSTOMER_PAYMENT',
                partyIdFrom: 'DemoCustomer',
                partyIdTo: 'Company',
                statusId: 'PMNT_NOT_PAID',
                effectiveDate: UtilDateTime.nowTimestamp(),
                amount: BigDecimal.valueOf(20),
                currencyUomId: 'USD'
        ])
        delegator.createOrStore(payment)

        Map createContentResult = dispatcher.runSync('createContent',
                [contentName: 'Original Payment Content', userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId
        assert contentId

        Timestamp fromDate = UtilDateTime.toTimestamp('01/01/2020 00:00:00')
        GenericValue paymentContent = delegator.makeValue('PaymentContent', [
                paymentId: paymentId,
                paymentContentTypeId: paymentContentTypeId,
                contentId: contentId,
                fromDate: fromDate
        ])
        paymentContent.create()

        Timestamp thruDate = UtilDateTime.toTimestamp('01/01/2030 00:00:00')
        String newContentName = 'Updated Payment Content'
        Map serviceCtx = [
                paymentId: paymentId,
                paymentContentTypeId: paymentContentTypeId,
                contentId: contentId,
                fromDate: fromDate,
                thruDate: thruDate,
                contentName: newContentName,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePaymentContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue updatedPaymentContent = from('PaymentContent')
                .where('paymentId', paymentId, 'paymentContentTypeId', paymentContentTypeId,
                        'contentId', contentId, 'fromDate', fromDate).queryOne()
        assert updatedPaymentContent
        assert updatedPaymentContent.thruDate == thruDate

        GenericValue updatedContent = from('Content').where('contentId', contentId).queryOne()
        assert updatedContent
        assert updatedContent.contentName == newContentName
    }

}
