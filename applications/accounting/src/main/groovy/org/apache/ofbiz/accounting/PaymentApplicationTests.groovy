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

package org.apache.ofbiz.accounting

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.order.order.OrderReadHelper

class PaymentApplicationTests extends OFBizTestCase {
    
    public PaymentApplicationTests(String name) {
        super(name)
    }

    void testInvoiceAppl() {
        Map serviceInMap = [:]
        //from the test data
        serviceInMap.invoiceId = "appltest10000"
        serviceInMap.paymentId = "appltest10000"
        serviceInMap.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPaymentApplication', serviceInMap)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentApplication = from('PaymentApplication')
                .where('paymentApplicationId', serviceResult.paymentApplicationId).queryOne()
        assert paymentApplication

        GenericValue payment = from('Payment').where('paymentId', serviceInMap.paymentId).queryOne()
        assert payment

        assert paymentApplication != null
        assert paymentApplication.invoiceId == serviceInMap.invoiceId
        assert paymentApplication.paymentId == serviceInMap.paymentId
        assert paymentApplication.amountApplied == payment.amount
        // both payment and invoice should be completely applied
        BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)
        BigDecimal notAppliedInvoice = InvoiceWorker.getInvoiceNotApplied(delegator, serviceInMap.invoiceId)

        assert notAppliedPayment == BigDecimal.ZERO
        assert notAppliedInvoice == BigDecimal.ZERO
        delegator.removeAll('PaymentApplication')
    }

    void testToPayment() {
        Map serviceInMap = [:]
        serviceInMap.paymentId = "appltest10000"
        serviceInMap.toPaymentId = "appltest10001"
        serviceInMap.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPaymentApplication', serviceInMap)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentApplication = from('PaymentApplication')
                                            .where('paymentApplicationId', serviceResult.paymentApplicationId).queryOne()
        assert paymentApplication

        BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)

        GenericValue payment = from('Payment').where('paymentId', serviceInMap.paymentId).queryOne()
        assert payment

        assert paymentApplication != null
        assert paymentApplication.toPaymentId == serviceInMap.toPaymentId
        assert paymentApplication.paymentId == serviceInMap.paymentId
        assert paymentApplication.amountApplied == payment.amount

        notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)
        BigDecimal notAppliedToPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.toPaymentId)

        assert notAppliedPayment == BigDecimal.ZERO
        assert notAppliedToPayment == BigDecimal.ZERO
        delegator.removeAll('PaymentApplication')
    }

    void testBillingAppl() {
        Map serviceInMap = [:]
        //from the test data
        serviceInMap.paymentId = "appltest10000"
        serviceInMap.billingAccountId = "appltest10000"
        serviceInMap.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPaymentApplication', serviceInMap)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentApplication = from('PaymentApplication')
                                                .where('paymentApplicationId', serviceResult.paymentApplicationId).queryOne()
        assert paymentApplication

        BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)

        GenericValue payment = from('Payment').where('paymentId', serviceInMap.paymentId).queryOne()
        assert payment

        assert paymentApplication !=null
        assert paymentApplication.billingAccountId == serviceInMap.billingAccountId
        assert paymentApplication.paymentId == serviceInMap.paymentId
        assert paymentApplication.amountApplied == payment.amount
        // both payment and invoice should be completely applied
        notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)

        GenericValue billingAccount = from('BillingAccount').where('billingAccountId', serviceInMap.billingAccountId).queryOne()
        BigDecimal appliedBillling= OrderReadHelper.getBillingAccountBalance(billingAccount)
        assert appliedBillling

        assert notAppliedPayment == BigDecimal.ZERO
        delegator.removeAll('PaymentApplication')
    }

    void testTaxGeoId () {
        Map serviceInMap = [:]
        //from the test data
        serviceInMap.paymentId = "appltest10000"
        serviceInMap.taxAuthGeoId = "UT"
        serviceInMap.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createPaymentApplication', serviceInMap)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentApplication = from('PaymentApplication')
                .where('paymentApplicationId', serviceResult.paymentApplicationId).queryOne()
        assert paymentApplication

        GenericValue payment = from('Payment').where('paymentId', serviceInMap.paymentId).queryOne()
        assert payment
        assert paymentApplication != null
        assert paymentApplication.taxAuthGeoId == serviceInMap.taxAuthGeoId
        assert paymentApplication.paymentId == serviceInMap.paymentId
        assert paymentApplication.amountApplied == payment.amount
        // payment should be completely applied
        BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(delegator, serviceInMap.paymentId)
        assert notAppliedPayment == BigDecimal.ZERO
        delegator.removeAll('PaymentApplication')
    }

}
