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

import org.apache.ofbiz.base.util.UtilDateTime;

import static org.apache.ofbiz.entity.condition.EntityComparisonOperator.IN
import static org.apache.ofbiz.entity.condition.EntityCondition.makeCondition
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoPaymentTests extends OFBizTestCase {
    public AutoPaymentTests(String name) {
        super(name)
    }

    // Test case for Batching Payments process
    void testCreatePaymentGroupAndMember() {
        String paymentGroupTypeId = 'BATCH_PAYMENT'
        String paymentGroupName = 'Payment Batch'
        List paymentIds = ['demo10000', 'demo10001']

        Map serviceCtx = [
                paymentGroupTypeId: paymentGroupTypeId,
                paymentGroupName: paymentGroupName,
                paymentIds: paymentIds,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPaymentGroupAndMember', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String paymentGroupId = serviceResult.paymentGroupId

        GenericValue paymentGroup = from('PaymentGroup')
                .where('paymentGroupId', paymentGroupId)
                .queryOne()
        assert paymentGroup
        assert paymentGroup.paymentGroupTypeId == paymentGroupTypeId
        assert paymentGroup.paymentGroupName == paymentGroupName

        List<GenericValue> paymentGroupMemberList = from('PaymentGroupMember')
                .where('paymentGroupId', paymentGroupId)
                .queryList()
        assert paymentGroupMemberList

        for (GenericValue paymentGroupMember : paymentGroupMemberList) {
            assert paymentIds.contains(paymentGroupMember.paymentId)
        }
    }

    // Test case for voiding payments
    void testVoidPayment() {
        /*
            Precondition : payment is in sent status and invoice is in ready for posting status
                           Credit in account 213000 - CUSTOMER CREDIT
                           Debit in account 210000 - ACCOUNTS PAYABLE

            Post condition : payment status changes to void.
                             removes PaymentApplication if any associated.
                             Credit in account 210000- ACCOUNTS PAYABLE
                             Debit in account 213000 - CUSTOMER CREDIT
        */
        String paymentId = '8000'

        Map serviceCtx = [
                paymentId: paymentId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('voidPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue payment = from('Payment')
                .where('paymentId', paymentId)
                .queryOne()
        assert payment
        assert payment.statusId == 'PMNT_VOID'

        GenericValue acctgTrans = from('AcctgTrans')
                .where('paymentId', paymentId)
                .orderBy('-transactionDate').queryFirst()
        assert acctgTrans

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', acctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                assert acctgTransEntry.glAccountTypeId == 'ACCOUNTS_PAYABLE'
                assert acctgTransEntry.glAccountId == '210000'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountId == '213000'
            }
        }
    }

    // Test case for canceling invoices
    void testCancelInvoice() {
        /*
            Precondition : invoice is in ready status
                           Credit in account 210000 - ACCOUNTS PAYABLE
                           Debit in account 516100 -->

            Post condition : invoice status changes to cancelled.
                             removes PaymentApplication if any associated.
                             Credit in account 516100
                             Debit in account 210000 - ACCOUNTS PAYABLE
        */
        String invoiceId = '8001'

        Map serviceCtx = [
                invoiceId: invoiceId,
                statusId: 'INVOICE_CANCELLED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoice = from('Invoice')
                .where('invoiceId', invoiceId)
                .queryOne()
        assert invoice
        assert invoice.statusId == 'INVOICE_CANCELLED'

        GenericValue acctgTrans = from('AcctgTrans')
                .where('invoiceId', invoiceId)
                .orderBy('-transactionDate').queryFirst()
        assert acctgTrans

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', acctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                assert acctgTransEntry.glAccountId == '516100'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountId == '210000'
                assert acctgTransEntry.glAccountTypeId == 'ACCOUNTS_PAYABLE'
            }
        }
    }

    // Test case for process mass check run
    void testCreatePaymentAndPaymentGroupForInvoices() {
        /*
            Precondition : Invoice is in ready status.
                           Invoice outstanding amount should be greater than zero -->

            Following process is tested by test case:
                This will call createPaymentAndPaymentGroupForInvoices service and return a paymentGroupId;
                1. Checked for paymentGroupId for not empty
                2. Checked for associated paymentGroupMembers for not empty -->

            Post condition : Invoice status should be changed to paid.
                             Payment should be created with PaymentApplications.
                             PaymentGroup and PaymentGroupMembers should be created.
        */
        Map serviceCtx = [
                organizationPartyId: 'Company',
                checkStartNumber: new Long('100101'),
                invoiceIds: ['8000', '8008'],
                paymentMethodTypeId: 'COMPANY_CHECK',
                paymentMethodId: 'SC_CHECKING',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPaymentAndPaymentGroupForInvoices', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String paymentGroupId = serviceResult.paymentGroupId
        assert paymentGroupId

        List<GenericValue> paymentGroupMemberList = from('PaymentGroupMember')
                .where('paymentGroupId', paymentGroupId)
                .queryList()
        assert paymentGroupMemberList
    }

    // Test case for cancel check run
    void testCancelCheckRunPayments() {
        /*
            Pre condition : Invoice is in paid status.
                            Payment should be present.
                            thruDate for PaymentGroupMember should be Null -->

            Following process is tested by test case:
                This will call cancelCheckRunPayments service;
                1. Checked for thruDate for not empty
                2. Checked for associated payment status as PMNT_VOID -->

            Post condition : thruDate for PaymentGroupMember should be Not Null
                             payment status should be changed to PMNT_VOID.
        */
        String paymentGroupId = '9001'

        Map serviceCtx = [
                paymentGroupId: paymentGroupId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('cancelCheckRunPayments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> paymentGroupMemberAndTransList = from('PmtGrpMembrPaymentAndFinAcctTrans')
                .where('paymentGroupId', paymentGroupId)
                .queryList()
        GenericValue firstPaymentGroupMemberAndTrans = EntityUtil.getFirst(paymentGroupMemberAndTransList)
        if (firstPaymentGroupMemberAndTrans && !'FINACT_TRNS_APPROVED'.equals(firstPaymentGroupMemberAndTrans.finAccountTransStatusId)) {
            for (GenericValue aymentGroupMemberAndTrans : paymentGroupMemberAndTransList) {
                assert aymentGroupMemberAndTrans.thruDate
                assert aymentGroupMemberAndTrans.statusId == 'PMNT_VOID'
            }
        }
    }

    // Test case for deposit or withdraw payments
    void testDepositWithdrawPayments() {
        //List paymentIds = ['demo10001', 'demo10010']
        List paymentIds = ['demo10010']

        Map serviceCtx = [
                paymentIds: paymentIds,
                finAccountId: 'SC_CHECKING',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('depositWithdrawPayments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String

        List<GenericValue> payments = from('Payment')
                .where(makeCondition('paymentId', IN, paymentIds))
                .queryList()

        for (GenericValue payment : payments) {
            GenericValue finAccountTrans = from('FinAccountTrans')
                    .where('finAccountTransId', payment.finAccountTransId)
                    .queryOne()
            assert finAccountTrans
            assert ['DEPOSIT', 'WITHDRAWAL'].contains(finAccountTrans.finAccountTransTypeId)
            assert finAccountTrans.amount.compareTo(payment.amount) == 0
        }
    }

    void testDepositWithdrawPaymentsInSingleTrans() {
        List paymentIds = ['8004']
        BigDecimal paymentRunningTotal = new BigDecimal('0')

        Map serviceCtx = [
                paymentIds: paymentIds,
                finAccountId: 'SC_CHECKING',
                groupInOneTransaction: 'Y',
                paymentGroupTypeId: 'BATCH_PAYMENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('depositWithdrawPayments', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String finAccountTransId = serviceResult.finAccountTransId

        List<GenericValue> payments = from('Payment')
                .where(makeCondition('paymentId', IN, paymentIds))
                .queryList()
        for (GenericValue payment : payments) {
            assert finAccountTransId == payment.finAccountTransId
            paymentRunningTotal = paymentRunningTotal.add(payment.amount)
        }

        GenericValue finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', finAccountTransId)
                .queryOne()
        assert paymentRunningTotal.compareTo(finAccountTrans.amount) == 0
    }

    // Test case for fin account trans
    void testSetFinAccountTransStatus() {
        /*
            Precondition : FinAccountTrans should be in CREATED status

            Post condition : FinAccountTrans status changes to CANCELED
                             Clear finAccountTransId field and update associated Payment record
        */
        String finAccountTransId = '9102'

        Map serviceCtx = [
                finAccountTransId: finAccountTransId,
                statusId: 'FINACT_TRNS_CANCELED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setFinAccountTransStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', finAccountTransId)
                .queryOne()
        assert finAccountTrans
        assert finAccountTrans.statusId == 'FINACT_TRNS_CANCELED'

        GenericValue payment = from('Payment')
                .where('paymentId', finAccountTrans.paymentId)
                .queryOne()
        assert payment
    }

    // Test case to verify GL postings for Void Payment process
    void testGlPostingsOnVoidPayment() {
        /*
            Precondition :
              * Payment is in sent status so accounting transaction is already posted to the GL
              * GL Account associated with Payment :8003 are ACCOUNTS RECEVABLE and UNDEPOSITED RECEIPTS
              * Credit in account 120000 - ACCOUNTS RECEVABLE ;debitTotal :$754.17 ; creditTotal:$274.18 ; debitCreditDifference : $479.99
              * Debit in account 112000 UNDEPOSITED RECEIPTS ;debitTotal :$136.85 ; creditTotal:$116.85 ; debitCreditDifference : $20

            Post condition : When status is set to void, an reverse accounting transaction is automatically posted to the GL.
              * Payment status changes to void.
              * Credit in account 112000- UNDEPOSITED RECEIPTS  ;debitTotal :$136.85 ; creditTotal: $136.85 ; debitCreditDifference : $0
              * Debit in account 120000 - ACCOUNTS RECEVABLE debitTotal :$774.17 ; creditTotal: $274.18 ; debitCreditDifference : $ 499.99
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
                glAccountId: '120000',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal receivableDebitTotal = serviceResult.debitTotal
        BigDecimal receivableCreditTotal = serviceResult.creditTotal
        BigDecimal receivableDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        serviceCtx.glAccountId = '112000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        BigDecimal undepositedDebitTotal = serviceResult.debitTotal
        BigDecimal undepositedCreditTotal = serviceResult.creditTotal
        BigDecimal undepositedDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        Map voidPaymentCtx = [
                paymentId: '8003',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('voidPayment', voidPaymentCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        BigDecimal totalReceivableDebitAmount = receivableDebitTotal.add(new BigDecimal('20'))
        BigDecimal totalReceivableDebitCreditDifference = receivableDebitCreditDifference.add(new BigDecimal('20'))
        serviceResult.clear()
        serviceCtx.glAccountId = '120000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert serviceResult
        assert totalReceivableDebitAmount.compareTo(serviceResult.debitTotal) == 0
        assert totalReceivableDebitCreditDifference.compareTo(serviceResult.debitCreditDifference) == 0

        BigDecimal totalUndepositedCreditAmount = undepositedCreditTotal.add(new BigDecimal('20'))
        BigDecimal totalUndepositedDebitCreditDifference = undepositedDebitCreditDifference.subtract(new BigDecimal('20'))
        serviceResult.clear()
        serviceCtx.glAccountId = '112000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert serviceResult
        assert totalUndepositedCreditAmount.compareTo(serviceResult.creditTotal) == 0
        assert totalUndepositedDebitCreditDifference.compareTo(serviceResult.debitCreditDifference) == 0
    }

    // Test case to verify GL postings for Check Run process
    void testGlPostingOnCheckRun() {
        /*
            Precondition :
              * Invoice is in ready status so accounting transaction is already posted to the GL
              * GL Accounts associated with Invoice :8007 are ACCOUNTS PAYABLE and UNINVOICED ITEM RECEIPTS
              * Credit in account 210000 - ACCOUNTS PAYABLE ;debitTotal $430 ; creditTotal:$1955.4 ; debitCreditDifference : $ -1524.85
              * Debit in account 214000 - UNINVOICED ITEM RECEIPTS;debitTotal :$408 ; creditTotal:$48 ; debitCreditDifference : $360
              * UNDEPOSITED RECEIPTS 112000 - debitTotal :$136.85 ; creditTotal:$136.85 ; debitCreditDifference : $0

            Post condition : After Check Run process accounting transactions are automatically posted to the GL.
              * Payment get associated with invoice.
              * GL Accounts associated with Payment are ACCOUNTS PAYABLE and UNDEPOSITED RECEIPTS.
              * ACCOUNTS PAYABLE 210000(for Invoice and Payment) - debitTotal $503.41 ; creditTotal:$1991.83 ; debitCreditDifference : $ -1488.42
              * UNINVOICED ITEM RECEIPTS 214000 - debitTotal :$408 ; creditTotal:$48 ; debitCreditDifference : $360
              * GENERAL CHECKING ACCOUNT 111100 (for payment)- debitTotal :$136.85 ; creditTotal:$173.28 ; debitCreditDifference : $ -36.43
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
        BigDecimal undepositedCreditTotal = serviceResult.creditTotal
        BigDecimal undepositedDebitCreditDifference = serviceResult.debitCreditDifference

        serviceResult.clear()
        Map invoiceServiceCtx = [
                organizationPartyId: 'Company',
                checkStartNumber: new Long('100100'),
                invoiceIds: ['8007'],
                paymentMethodTypeId: 'COMPANY_CHECK',
                paymentMethodId: 'SC_CHECKING',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('createPaymentAndPaymentGroupForInvoices', invoiceServiceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String paymentGroupId = serviceResult.paymentGroupId
        assert paymentGroupId

        BigDecimal tempBig = new BigDecimal('36.43')

        BigDecimal totalPayableDebitAmount = tempBig.add(payableDebitTotal)
        BigDecimal totalPayableDebitCreditDifference = tempBig.add(payableDebitCreditDifference)
        serviceResult.clear()
        serviceCtx.glAccountId = '210000'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert totalPayableDebitAmount.compareTo(serviceResult.debitTotal) == 0
        assert payableCreditTotal.compareTo(serviceResult.creditTotal) == 0
        assert totalPayableDebitCreditDifference.compareTo(serviceResult.debitCreditDifference) == 0

        BigDecimal totalUndepositedCreditAmount = tempBig.add(undepositedCreditTotal)
        BigDecimal totalUndepositedDebitCreditDifference = undepositedDebitCreditDifference.subtract(tempBig)
        serviceResult.clear()
        serviceCtx.glAccountId = '111100'
        serviceResult = dispatcher.runSync('getAcctgTransEntriesAndTransTotal', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert undepositedDebitTotal.compareTo(serviceResult.debitTotal) == 0
        assert totalUndepositedCreditAmount.compareTo(serviceResult.creditTotal) == 0
        assert totalUndepositedDebitCreditDifference.compareTo(serviceResult.debitCreditDifference) == 0
    }

    void disabledTestUpdatePaymentMethodAddress() {
        // Create a new Postal Address, set the bare minimum necessary, this test isn't about the postal address
        Map serviceCtx = [
                address1: '2003 Open Blvd',
                city: 'Open City',
                postalCode: '999999',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId

        // Count the number of EftAccounts and CreditCards associated to the oldContactMechId, use to verify at the end
        long noEftAccounts9000Before = from('EftAccount')
                .where(makeCondition('contactMechId', '9000'))
                .queryCount()
        long noCreditCards9000Before = from('CreditCard')
                .where(makeCondition('contactMechId', '9000'))
                .queryCount()

        // Run the actual service to be tested
        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                oldContactMechId: '9000',
                contactMechId: contactMechId,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('updatePaymentMethodAddress', serviceCtx)
        assert serviceResult

        // Count the number EftAccounts and CreditCards now associated with the oldContactMechId (should be zero for both)
        long noEftAccounts9000After = from('EftAccount')
                .where(makeCondition('contactMechId', '9000'))
                .queryCount()
        long noCreditCards9000After = from('CreditCard')
                .where(makeCondition('contactMechId', '9000'))
                .queryCount()

        // Old contactMech should no longer have any payment methods associated to it
        assert noEftAccounts9000After == 0
        assert noCreditCards9000After == 0

        // Count the number of EftAccounts and CreditCards associated to the oldContactMechId, use to verify at the end
        long noEftAccountsNewContactMech = from('EftAccount')
                .where(makeCondition('contactMechId', contactMechId))
                .queryCount()
        long noCreditCardsNewContactMech = from('CreditCard')
                .where(makeCondition('contactMechId', contactMechId))
                .queryCount()

        // New contactMech should have the same number of payment methods as the old did
        assert noEftAccountsNewContactMech == noEftAccounts9000Before
        assert noCreditCardsNewContactMech == noCreditCards9000Before
    }
}