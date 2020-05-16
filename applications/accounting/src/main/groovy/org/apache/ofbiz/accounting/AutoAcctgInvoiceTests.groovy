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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

class AutoAcctgInvoiceTests extends OFBizTestCase {
    public AutoAcctgInvoiceTests(String name) {
        super(name)
    }

    void testCreateInvoiceContent() {
        Map serviceCtx = [
            invoiceId: '1008',
            contentId: '1000',
            invoiceContentTypeId: 'COMMENTS',
            fromDate: UtilDateTime.nowTimestamp(),
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceContent = from('InvoiceContent')
            .where('invoiceId', serviceResult.invoiceId,
                   'contentId', serviceResult.contentId,
                   'invoiceContentTypeId', serviceResult.invoiceContentTypeId)
            .queryFirst()

        assert invoiceContent.contentId == serviceResult.contentId
    }
    void testCreateSimpleTextContentForInvoice() {
        Map serviceCtx = [
                invoiceId: '1009',
                contentId: '1001',
                contentTypeId: 'DOCUMENT',
                invoiceContentTypeId: 'COMMENTS',
                text: 'Content for invoice # 1009',
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createSimpleTextContentForInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceContent = from('InvoiceContent')
                .where('invoiceId', '1009',
                'contentId', '1001',
                'invoiceContentTypeId', 'COMMENTS')
                .queryFirst()

        assert invoiceContent != null
    }

    void testCopyInvoice() {
        Map serviceCtx = [
                invoiceIdToCopyFrom: '1000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId != null
    }

    void testCreateInvoice() {
        Map serviceCtx = [
                invoiceTypeId: 'PURCHASE_INVOICE',
                partyIdFrom: 'DEMO_COMPANY',
                partyId: 'DEMO_COMPANY1',
                invoiceDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId != null
    }

    void testGetInvoice() {
        Map serviceCtx = [
                invoiceId: '1001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoice != null
        assert serviceResult.invoiceItems != null
    }

    void testSetInvoiceStatus() {
        Map serviceCtx = [
                invoiceId: '1002',
                statusId: 'INVOICE_APPROVED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoice = from('Invoice')
                .where('invoiceId', '1002')
                .queryOne()

        assert invoice != null
        assert invoice.statusId == 'INVOICE_APPROVED'
    }

    void testCopyInvoiceToTemplate() {
        Map serviceCtx = [
                invoiceId: '1002',
                invoiceTypeId: 'PURCHASE_INVOICE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyInvoiceToTemplate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId != null
    }

    void testCreateInvoiceItem() {
        Map serviceCtx = [
                invoiceId: '1003',
                invoiceTypeId: 'PINV_FXASTPRD_ITEM',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceItemSeqId != null
    }

    void testCreateInvoiceStatus() {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Map serviceCtx = [
                invoiceId: '1004',
                statusId: 'INVOICE_IN_PROCESS',
                statusDate: nowTimestamp,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceStatus = from('InvoiceStatus')
                .where('invoiceId', '1004',
                        'statusId', 'INVOICE_IN_PROCESS',
                        'statusDate', nowTimestamp)
                .queryOne()

        assert invoiceStatus != null
    }

    void testCreateInvoiceRole() {
        Map serviceCtx = [
                invoiceId: '1006',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'INTERNAL_ORGANIZATIO',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceRole = from('InvoiceRole')
                .where('invoiceId', '1006',
                        'partyId', 'DEMO_COMPANY',
                        'roleTypeId', 'INTERNAL_ORGANIZATIO')
                .queryOne()

        assert invoiceRole != null
    }

    void testCreateInvoiceTerm() {
        Map serviceCtx = [
                invoiceId: '1006',
                invoiceItemSeqId: '00001',
                termTypeId: 'FINANCIAL_TERM',
                termValue: 50.00,
                termDays: 10,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceTerm', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceTerm = from('InvoiceTerm')
                .where('invoiceTermId', serviceResult.invoiceTermId)
                .queryOne()

        assert invoiceTerm != null
    }

    void testCancelInvoice() {
        Map serviceCtx = [
                invoiceId: '1007',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('cancelInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.invoiceTypeId != null
    }

}
