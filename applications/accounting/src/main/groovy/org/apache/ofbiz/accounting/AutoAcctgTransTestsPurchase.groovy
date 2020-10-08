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

class AutoAcctgTransTestsPurchase extends OFBizTestCase {
    public AutoAcctgTransTestsPurchase(String name) {
        super(name)
    }

    // Test case for Accounting Transaction on Purchase
    void testAcctgTransOnPoReceipts() {
        /*
            Precondition : shipment is created from supplier and order items are issued
            create a purchase order using following:
              Supplier : DemoSupplier
              Item     : WG-1111
              Quantity : 10

            Post condition : Credit in account 214000 - UNINVOICED ITEM RECEIPT amount = grand total of order.
                             Debit in account 140000- INVENTORY amount = grand total of order.
        */
        String orderId = 'DEMO10091'
        String shipmentId = '9999'
        String productId = 'GZ-2644'

        Map serviceCtx = [
                inventoryItemTypeId: 'NON_SERIAL_INV_ITEM',
                productId: productId,
                facilityId: 'WebStoreWarehouse',
                quantityAccepted: new BigDecimal('5'),
                quantityRejected: new BigDecimal('0'),
                shipmentId: shipmentId,
                orderId: orderId,
                orderItemSeqId: '00001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('receiveInventoryProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans')
                .where('shipmentId', shipmentId)
                .queryFirst()
        assert acctgTrans
        assert acctgTrans.glJournalId != 'ERROR_JOURNAL'

        List<GenericValue> acctgTransEntryList = from('AcctgTransEntry')
                .where('acctgTransId', acctgTrans.acctgTransId, 'productId', productId)
                .queryList()
        assert acctgTransEntryList

        checkEntriesBalance(acctgTransEntryList)

        for (GenericValue acctgTransEntry : acctgTransEntryList) {
            if (acctgTransEntry.debitCreditFlag == 'C') {
                assert acctgTransEntry.glAccountTypeId == 'UNINVOICED_SHIP_RCPT'
                assert acctgTransEntry.glAccountId == '214000'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountTypeId == 'INVENTORY_ACCOUNT'
                assert acctgTransEntry.glAccountId == '140000'
            }
        }

        GenericValue orderItemBilling = from('OrderItemBilling')
                .where('orderId', orderId)
                .queryFirst()
        assert orderItemBilling
    }

    void testAcctgTransOnEditPoInvoice() {
        /*
            Precondition: To the Purchase Invoice created add taxes and two different shipping charges
              1. for taxes: set "Invoice Item Type" = "Invoice Sales Tax" and "Unit Price" = 10$
              2. for the first shipping charge: set "Invoice Item Type" = "Invoice Shipping And Handling" and "Unit Price" = 5$
              3. for the second shipping charge: set "Invoice Item Type" = "Invoice Shipping And Handling", set "Override Gl Account Id" = "516100" and "Unit Price" = 5$

            Post condition: When status is set to ready, an accounting transaction is automatically posted to the GL:
              * Credit; in account 210000 - "ACCOUNTS PAYABLE"; amount: 290$
              * Debit; in account 214000 - "UNINVOICED ITEM RECEIPTS"; amount: 270$
              * Debit; in account 516100 - "PURCHASE ORDER ADJUSTMENTS"; amount: 10$
              * Debit; in account 510000 - "FREIGHT IN"; amount: 5$
              * Debit; in account 516100 - "PURCHASE ORDER ADJUSTMENTS"; amount: 5$
         */
        GenericValue orderItemBilling = from('OrderItemBilling')
                .where('orderId', 'DEMO10091')
                .queryFirst()
        assert orderItemBilling

        Map serviceCtx = [
                statusId: 'INVOICE_READY',
                invoiceId: orderItemBilling.invoiceId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans')
                .where('invoiceId', orderItemBilling.invoiceId)
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
                assert acctgTransEntry.glAccountTypeId == 'ACCOUNTS_PAYABLE'
                assert acctgTransEntry.glAccountId == '210000'
            }
        }
    }

    void testAcctgTransOnPaymentSentToSupplier() {
        /*
            Precondition: New payment is created for: supplierId = "DemoSupplier", "Payment Type ID" = "Vendor Payment" and
                          a proper "Payment Method Type" (e.g. "Electronic Funds Transfer"), amount = $290

            Post condition: On payment's status is sent: a double-entry accounting transaction is automatically posted to the GL:
                * Credit; in account 111100 - "GENERAL CHECKING ACCOUNT"; amount: 290$; however this depends on the "Payment method type" selected;
                * Debit; in account 216000 - "ACCOUNTS PAYABLE - UNAPPLIED PAYMENTS"; amount: 290$
         */
        String paymentId = '9000'

        Map serviceCtx = [
                statusId: 'PMNT_SENT',
                paymentId: paymentId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setPaymentStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans')
                .where('paymentId', paymentId)
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
                assert acctgTransEntry.glAccountId == '111100'
            } else if (acctgTransEntry.debitCreditFlag == 'D') {
                assert acctgTransEntry.glAccountTypeId == 'ACCPAYABLE_UNAPPLIED'
                assert acctgTransEntry.glAccountId == '216000'
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