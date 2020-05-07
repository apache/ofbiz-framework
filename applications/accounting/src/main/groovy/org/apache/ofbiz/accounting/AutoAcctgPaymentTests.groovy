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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

class AutoAcctgPaymentTests extends OFBizTestCase {
    public AutoAcctgPaymentTests(String name) {
        super(name)
    }
    void testCreatePayment() {
        Map serviceCtx = [:]
        serviceCtx.paymentTypeId = 'CUSTOMER_PAYMENT'
        serviceCtx.partyIdFrom = 'Company'
        serviceCtx.partyIdTo = 'DemoCustCompany'
        serviceCtx.amount = new BigDecimal('100.00')
        serviceCtx.paymentMethodTypeId = 'COMPANY_CHECK'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', serviceResult.paymentId).queryOne()
        assert payment.paymentTypeId == 'CUSTOMER_PAYMENT'
        assert payment.paymentMethodTypeId == 'COMPANY_CHECK'
    }
    void testSetPaymentStatus() {
        Map serviceCtx = [:]
        serviceCtx.paymentId = '1000'
        serviceCtx.statusId = 'PAYMENT_AUTHORIZED'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('setPaymentStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', '1000').queryOne()
        assert payment
        assert serviceResult.oldStatusId == 'PAYMENT_NOT_AUTH'
    }
    void testQuickSendPayment() {
        Map serviceCtx = [:]
        serviceCtx.paymentId = '1001'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('quickSendPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment').where('paymentId', '1001').queryOne()
        assert payment
        assert payment.statusId == 'PMNT_SENT'
    }
    void testGetPayments() {
        Map serviceCtx = [
            finAccountTransId: '1001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPayments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.payments != null
    }
    void testCreatePaymentContent() {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Map serviceCtx = [
            paymentId: '1006',
            paymentContentTypeId: 'COMMENTS',
            contentId: '1006',
            fromDate: nowTimestamp,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPaymentContent', serviceCtx)
        GenericValue paymentContent = from('PaymentContent').where(paymentId: '1006', paymentContentTypeId: 'COMMENTS', contentId: '1006').filterByDate().queryFirst()
        assert paymentContent
    }
}
