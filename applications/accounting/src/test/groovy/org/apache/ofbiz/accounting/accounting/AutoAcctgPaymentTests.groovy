/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper

import java.sql.Timestamp
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgPaymentTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreatePayment() {
        String paymentTypeId = testParams.paymentTypeId ?: 'CUSTOMER_PAYMENT'
        String paymentMethodTypeId = testParams.paymentMethodTypeId ?: 'COMPANY_CHECK'
        Map serviceCtx = [:]
        serviceCtx.paymentTypeId = paymentTypeId
        serviceCtx.partyIdFrom = testParams.partyIdFrom ?: 'Company'
        serviceCtx.partyIdTo = testParams.partyIdTo ?: 'DemoCustCompany'
        serviceCtx.amount = 100.00
        serviceCtx.paymentMethodTypeId = paymentMethodTypeId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', serviceResult.paymentId).queryOne()
        assert payment.paymentTypeId == paymentTypeId
        assert payment.paymentMethodTypeId == paymentMethodTypeId
    }
    @Test
    @Order(2)
    void testSetPaymentStatus() {
        String paymentId = testParams.paymentId ?: '1000'
        Map serviceCtx = [:]
        serviceCtx.paymentId = paymentId
        serviceCtx.statusId = testParams.statusId ?: 'PAYMENT_AUTHORIZED'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('setPaymentStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', paymentId).queryOne()
        assert payment
        assert serviceResult.oldStatusId == 'PAYMENT_NOT_AUTH'
    }
    @Test
    @Order(3)
    void testQuickSendPayment() {
        String paymentId = testParams.paymentId ?: '1001'
        Map serviceCtx = [:]
        serviceCtx.paymentId = paymentId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('quickSendPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', paymentId).queryOne()
        assert payment
        assert payment.statusId == 'PMNT_SENT'
    }
    @Test
    @Order(4)
    void testGetPayments() {
        Map serviceCtx = [
            finAccountTransId: testParams.finAccountTransId ?: '1001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPayments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.payments != null
    }
    @Test
    @Order(5)
    void testCreatePaymentContent() {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        String paymentId = testParams.paymentId ?: '1006'
        String paymentContentTypeId = testParams.paymentContentTypeId ?: 'COMMENTS'
        String contentId = testParams.contentId ?: '1006'
        Map serviceCtx = [
            paymentId: paymentId,
            paymentContentTypeId: paymentContentTypeId,
            contentId: contentId,
            fromDate: nowTimestamp,
            userLogin: userLogin
        ]
        dispatcher.runSync('createPaymentContent', serviceCtx)
        GenericValue paymentContent = from('PaymentContent')
                .where(paymentId: paymentId, paymentContentTypeId: paymentContentTypeId, contentId: contentId).filterByDate().queryFirst()
        assert paymentContent
    }

}
