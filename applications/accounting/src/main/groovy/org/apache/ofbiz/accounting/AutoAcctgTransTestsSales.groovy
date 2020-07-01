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
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgTransTestsSales extends OFBizTestCase {
    public AutoAcctgTransTestsSales(String name) {
        super(name)
    }

    // Test case for Accounting Transaction on Sales
    void testAcctgTransForSalesOrderShipments() {
        /*
            Precondition :
              1. create a sales order
              2. from the order view screen, approve the order
              3. from the order view screen, create a shipment to the customer (click on "New Shipment For Ship Group" and then click on the "Update" button in the next screen)

            Following process is tested by test case:
              1. issue (assign) the order items to the shipment: select the "Order Items" tab and then click on "Issue All"; this action will generate and post to the GL the accounting transaction for the items taken from the warehouse and ready to be shipped

            Post condition: all order items will be issued and it will generate and post to the GL the accounting transaction for the items taken from the warehouse and ready to be shipped
              * Credit; in account:140000 - Account Type:"INVENTORY_ACCOUNT"
              * Debit; in account:500000 - Account Type:"COGS_ACCOUNT"
        */
        String shipmentId = '9998'

        Map serviceCtx = [
                shipmentId: shipmentId,
                shipGroupSeqId: '00001',
                orderId: 'DEMO10090',
                orderItemSeqId: '00001',
                inventoryItemId: '9001',
                productId: 'GZ-2644',
                quantity: new BigDecimal('2'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('issueOrderItemShipGrpInvResToShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans')
                .where('shipmentId', shipmentId)
                .orderBy('-postedDate').queryFirst()
        assert acctgTrans
        assert acctgTrans.glJournalId != 'ERROR_JOURNAL'

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', acctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        checkEntriesBalance(acctgTransEntryList)

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                assert acctgTransEntry.glAccountTypeId == 'INVENTORY_ACCOUNT'
                assert acctgTransEntry.glAccountId == '140000'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountTypeId == 'COGS_ACCOUNT'
                assert acctgTransEntry.glAccountId == '500000'
            }
        }
    }

    void testAcctgTransOnSalesInvoice() {
        /*
            Precondition:
              1. Create a sales order
              2. From the order view screen, approve the order
              3. From the order view screen, create a shipment to the customer (click on "New Shipment For Ship Group" and then click on the "Update" button in the next screen)
              4. Issue the order items to the shipment: select the "Order Items" tab and then click on "Issue All."
              5. From the shipment detail screen of the shipment created in the previous step (there is a link to it from the order detail screen), set the status of the shipment to "pack"(Click on "Edit" and then from statusId drop down select statusId = "Pack" and then click update); this action will generate a sales invoice

            Following process is tested by test case:
              1. Go to the invoice detail screen (there is a link to the invoice from the order detail screen) and click on the "set status to ready"; this action will generate and post to the GL the accounting transaction for the sales invoice

            Post condition: "Set status to ready"; This action will generate and post to the GL the accounting transaction for the sales invoice
              * Credit; in account=400000 - Account Type="SALES_ACCOUNT"
              * Debit; in  account=120000 - Account Type="ACCOUNTS_RECEIVABLE"

            Note: The above notes seem to assume that you are going to manually follow the preconditions above before running this test.
                  Instead the test will now use order DEMO10090 which currently preconditions 1-4 fulfilled, and we'll then update the
                  shipment to the packed status (precondition 4).  Additionally it doesn't seem to be necessary to set the invoice status
                  to ready because the invoice is created in that state.
        */
        String shipmentId = '9998'

        Map serviceCtx = [
                shipmentId: shipmentId,
                statusId: 'SHIPMENT_PACKED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue shipmentItemBilling = from('ShipmentItemBilling')
                .where('shipmentId', shipmentId)
                .orderBy('-createdStamp').queryFirst()
        assert shipmentItemBilling

        List<GenericValue> acctgTransList = from('AcctgTrans')
                .where('invoiceId', shipmentItemBilling.invoiceId)
                .queryList()
        assert acctgTransList

        // Check the invoice transaction
        GenericValue salesAcctgTrans = EntityUtil.getFirst(EntityUtil.filterByAnd(acctgTransList, [acctgTransTypeId: 'SALES_INVOICE']))
        assert salesAcctgTrans
        assert salesAcctgTrans.glJournalId != 'ERROR_JOURNAL'

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', salesAcctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        checkEntriesBalance(acctgTransEntryList)

        List<GenericValue> accountsReceivableEntries = EntityUtil.filterByAnd(acctgTransEntryList, [glAccountTypeId: 'ACCOUNTS_RECEIVABLE', glAccountId: '120000'])
        assert accountsReceivableEntries

        List<GenericValue> salesAccountEntries = EntityUtil.filterByAnd(acctgTransEntryList, [glAccountId: '401000'])
        assert salesAccountEntries

        // Check the payment transaction
        GenericValue paymentAcctgTrans = EntityUtil.getFirst(EntityUtil.filterByAnd(acctgTransList, [acctgTransTypeId: 'PAYMENT_APPL']))
        assert paymentAcctgTrans
        assert paymentAcctgTrans.glJournalId != 'ERROR_JOURNAL'

        acctgTransEntryList.clear();
        acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', paymentAcctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        checkEntriesBalance(acctgTransEntryList)

        accountsReceivableEntries.clear()
        accountsReceivableEntries = EntityUtil.filterByAnd(acctgTransEntryList, [glAccountTypeId: 'ACCOUNTS_RECEIVABLE', glAccountId: '120000'])
        assert accountsReceivableEntries
    }

    void testAcctgTransOnPaymentReceivedFromCustomer() {
        /*
            Precondition :-
              1. Click on the Payment top menu in the Accounting application, then click on the "Create New Payment" link.
              2. In the "New incoming payment" box, set the customer id in the "From Party ID" field; then set "Payment Type ID" = "Customer Payment" and a proper "Payment Method Type" (e.g. "Electronic Funds Transfer"); then set the "amount" and submit the form

            Following process is tested by test case:
              1. From the payment detail screen, when you are ready to post the payment to the GL, click on the "Status to Received" link

            Post condition: "Status to Received", Received Payments. When you are ready to post the payment to the GL this action will generate and post to the GL the accounting transaction for the items taken from the warehouse and ready to be shipped:
              * Credit; in glAccountId=126000 - glAccountTypeId="ACCOUNTS_RECEIVABLE - UNAPPLIED PAYMENTS"
              * Debit; in glAccountId=112000 - glAccountTypeId="UNDEPOSITED_RECEIPTS"
         */
        GenericValue customerRole = from('PartyRole')
                .where('roleTypeId', 'CUSTOMER')
                .queryFirst()
        assert customerRole

        // Creating a payment from scratch rather than using the demo data
        Map serviceCtx = [
                partyIdFrom: customerRole.partyId,
                amount: new BigDecimal('100'),
                partyIdTo: 'Company',
                paymentMethodTypeId: 'EFT_ACCOUNT',
                paymentTypeId: 'CUSTOMER_PAYMENT',
                statusId: 'PMNT_RECEIVED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPayment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans')
                .where('paymentId', serviceResult.paymentId, 'acctgTransTypeId', 'INCOMING_PAYMENT')
                .queryFirst()
        assert acctgTrans
        assert acctgTrans.glJournalId != 'ERROR_JOURNAL'

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', acctgTrans.acctgTransId)
                .queryList()
        assert acctgTransEntryList

        checkEntriesBalance(acctgTransEntryList)

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                assert acctgTransEntry.glAccountTypeId == 'ACCREC_UNAPPLIED'
                assert acctgTransEntry.glAccountId == '126000'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountId == '111100'
            }
        }
    }

    void checkEntriesBalance(List<GenericValue> acctgTransEntryList) {
        BigDecimal debitTotal = new BigDecimal('0')
        BigDecimal creditTotal = new BigDecimal('0')

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                creditTotal = creditTotal.add(acctgTransEntry.origAmount)
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                debitTotal = debitTotal.add(acctgTransEntry.origAmount)
            }
        }

        assert debitTotal.compareTo(creditTotal) == 0
    }
}