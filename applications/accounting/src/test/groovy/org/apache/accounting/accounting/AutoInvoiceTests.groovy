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
package org.apache.accounting.accounting

import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoInvoiceTests extends OFBizTestCase {

    AutoInvoiceTests(String name) {
        super(name)
    }

    void testInvoiceWorkerGetInvoiceTotal() {
        String invoiceId = 'demo10000'
        BigDecimal amount = 323.54
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = 'demo10001'
        amount = 36.43
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = 'demo10002'
        amount = 56.99
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = 'demo11000'
        amount = 20.00
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = 'demo11001'
        amount = 543.23
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = 'demo1200'
        amount = 511.23
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8000'
        amount = 60.00
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8001'
        amount = 10.00
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8002'
        amount = 36.43
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8003'
        amount = 46.43
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8004'
        amount = 33.99
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8100'
        amount = 1320.00
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8005'
        amount = 33.99
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8006'
        amount = 46.43
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8007'
        amount = 36.43
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8008'
        amount = 48.00
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8009'
        amount = 127.09
        assertInvoiceTotal(invoiceId, amount)

        invoiceId = '8010'
        amount = 179.97
        assertInvoiceTotal(invoiceId, amount)
    }

    // Test case for Commission Run
    void testCommissionRun() {
        /*
            Precondition : For Creating Commission invoice following data should be there :
                1 ) Sales invoices with paid status.(invoiceId = "8100")
                2 ) Sales Representative with agreement to Company on product (DemoCustAgent and DemoRepAll). -->
            Postcondition :
                1 ) Two commission will be creating for the parties DemoCustAgent and DemoRepAll (like 10000 and 10001 invoiceId).
                2 ) Its amountTotal will be same as commission cost of associated products.
        */
        BigDecimal invoiceTotal = 0
        BigDecimal amountTotal = 0

        List<GenericValue> invoiceItems = from('InvoiceItem')
                .where('invoiceId', '8100')
                .queryList()
        assert invoiceItems

        for (GenericValue invoiceItem : invoiceItems) {
            if (invoiceItem.productId && invoiceItem.productId == 'WG-9943-B3') {
                invoiceTotal = invoiceTotal.add(invoiceItem.quantity * (invoiceItem.amount * 0.03 + 1))
            }
        }

        Map serviceCtx = [
                invoiceIds: ['8100'],
                partyIds: ['DemoRepAll', 'DemoCustAgent', 'DemoRepStore'],
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommissionInvoices', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.invoicesCreated

        for (Map invoice : serviceResult.invoicesCreated) {
            amountTotal = amountTotal.add(InvoiceWorker.getInvoiceTotal(delegator, invoice.commissionInvoiceId))
        }
        assert invoiceTotal == amountTotal
    }

    // Test case to verify GL postings for Cancel Invoice process
    void testGlPostingOnCancelInvoice() {
        /*
            Precondition :
              * Invoice is in ready status so accounting transaction is already posted to the GL
              * GL Accounts associated with Invoice :8008 are ACCOUNTS PAYABLE and UNINVOICED ITEM RECEIPTS
              * Credit in account 210000 - ACCOUNTS PAYABLE ;debitTotal $303.41 ; creditTotal:$1651.7 ; debitCreditDifference : $ -1348.42
              * Debit in account 214000 - UNINVOICED ITEM RECEIPTS;debitTotal :$408 ; creditTotal:$48 ; debitCreditDifference : $360

            Post condition : After Cancel Invoice process reverse accounting transactions are automatically posted to the GL.
              * ACCOUNTS PAYABLE 210000  - debitTotal $351.41 ; creditTotal:$1651.7 ; debitCreditDifference : $ -1300
              * UNINVOICED ITEM RECEIPTS 214000 - debitTotal :$408 ; creditTotal:$96 ; debitCreditDifference : $312
        */
        Map serviceCtx = [
                organizationPartyId: 'Company',
                findDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findCustomTimePeriods', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.customTimePeriodList
        GenericValue customTimePeriod = (serviceResult.customTimePeriodList).get(0)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                organizationPartyId: 'Company',
                customTimePeriodStartDate: customTimePeriod.fromDate,
                customTimePeriodEndDate: customTimePeriod.thruDate,
                glAccountId: '210000',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal payableDebitTotal = serviceResult.debitTotal
        BigDecimal payableDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        serviceCtx.glAccountId = '214000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal uninvoicedCreditTotal = serviceResult.creditTotal
        BigDecimal uninvoicedDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        Map cancelInvoiceCtx = [
                invoiceId: '8008',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('cancelInvoice', cancelInvoiceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        BigDecimal totalPayableDebitAmount = payableDebitTotal.add(48)
        BigDecimal totalPayableDebitCreditDifference = payableDebitCreditDifference.add(48)
        serviceResult.clear()
        serviceCtx.glAccountId = '210000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert totalPayableDebitAmount == serviceResult.debitTotal
        assert totalPayableDebitCreditDifference == serviceResult.debitCreditDifference

        BigDecimal totalUnInvoicedCreditAmount = uninvoicedCreditTotal.add(48)
        BigDecimal totalUnInvoicedDebitCreditDifference = uninvoicedDebitCreditDifference.subtract(48)
        serviceResult.clear()
        serviceCtx.glAccountId = '214000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert totalUnInvoicedCreditAmount == serviceResult.creditTotal
        assert totalUnInvoicedDebitCreditDifference == serviceResult.debitCreditDifference
    }

    // Test case to verify GL postings for Cancel Check Run process
    void testGlPostingOnCancelCheckRun() {
        /*
            Precondition :
              * Two invoices are associated with PaymentGroupId 9000 which are 8001 and 8002
              * Invoices are in ready status so accounting transactions are already posted to the GL
              * GL Accounts associated with Invoices are ACCOUNTS PAYABLE (210000) and GENERAL CHECKING ACCOUNT (111100)

            Post condition : After Cancel Check Run process accounting transactions are automatically posted to the GL.
              * ACCOUNTS PAYABLE 210000  - debitTotal increased of $82.86 ; creditTotal increased of $165.72
                                                                          ; debitCreditDifference decreased of $82.86
              * GENERAL CHECKING ACCOUNT 111100 - debitTotal increased of $82.86 ; debitCreditDifference increased of $82.86
        */
        Map serviceCtx = [
                organizationPartyId: 'Company',
                findDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findCustomTimePeriods', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.customTimePeriodList
        GenericValue customTimePeriod = (serviceResult.customTimePeriodList).get(0)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                organizationPartyId: 'Company',
                customTimePeriodStartDate: customTimePeriod.fromDate,
                customTimePeriodEndDate: customTimePeriod.thruDate,
                glAccountId: '210000',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal payableDebitTotal = serviceResult.debitTotal
        BigDecimal payableCreditTotal = serviceResult.creditTotal
        BigDecimal payableDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        serviceCtx.glAccountId = '111100'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal undepositedDebitTotal = serviceResult.debitTotal
        BigDecimal undepositedDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        Map cancelCheckRunPaymentsCtx = [
                paymentGroupId: '9000',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('cancelCheckRunPayments', cancelCheckRunPaymentsCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentGroupMemberAndTrans = from('PmtGrpMembrPaymentAndFinAcctTrans')
                .where('paymentGroupId', '9000')
                .queryFirst()
        if (paymentGroupMemberAndTrans && 'FINACT_TRNS_APPROVED' != paymentGroupMemberAndTrans.finAccountTransStatusId) {
            BigDecimal tempBig = 82.86

            BigDecimal totalPayableDebitAmount = tempBig.add(payableDebitTotal)
            BigDecimal totalPayableCreditAmount = 165.72G.add(payableCreditTotal)
            BigDecimal totalPayableDebitCreditDifference = payableDebitCreditDifference.subtract(tempBig)
            serviceResult.clear()
            serviceCtx.glAccountId = '210000'
            serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
            assert ServiceUtil.isSuccess(serviceResult)
            assert totalPayableDebitAmount == serviceResult.debitTotal
            assert totalPayableCreditAmount == serviceResult.creditTotal
            assert totalPayableDebitCreditDifference == serviceResult.debitCreditDifference

            BigDecimal totalUndepositedDebitAmount = tempBig.add(undepositedDebitTotal)
            BigDecimal totalUndepositedDebitCreditDifference = tempBig.add(undepositedDebitCreditDifference)
            serviceResult.clear()
            serviceCtx.glAccountId = '111100'
            serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
            assert ServiceUtil.isSuccess(serviceResult)
            assert totalUndepositedDebitAmount == serviceResult.debitTotal
            assert totalUndepositedDebitCreditDifference == serviceResult.debitCreditDifference
        }
    }

    private void assertInvoiceTotal(String invoiceId, BigDecimal amount) {
        GenericValue invoice =  EntityQuery.use(delegator).from('Invoice').where('invoiceId', invoiceId).queryOne()
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice)
        assert  invoiceTotal == amount
    }

}
