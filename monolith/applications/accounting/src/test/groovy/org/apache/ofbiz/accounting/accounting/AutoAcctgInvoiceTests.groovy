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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper

import java.sql.Timestamp
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgInvoiceTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateInvoiceContent() {
        Map serviceCtx = [
            invoiceId: testParams.invoiceId ?: '1008',
            contentId: testParams.contentId ?: '1000',
            invoiceContentTypeId: testParams.invoiceContentTypeId ?: 'COMMENTS',
            fromDate: UtilDateTime.nowTimestamp(),
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceContentAndUpdateContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceContent = from('InvoiceContent')
            .where('invoiceId', serviceResult.invoiceId,
                   'contentId', serviceResult.contentId,
                   'invoiceContentTypeId', serviceResult.invoiceContentTypeId)
            .queryFirst()

        assert invoiceContent.contentId == serviceResult.contentId
    }
    @Test
    @Order(2)
    void testCreateSimpleTextContentForInvoice() {
        String invoiceId = testParams.invoiceId ?: '1009'
        String invoiceContentTypeId = testParams.invoiceContentTypeId ?: 'COMMENTS'
        Map serviceCtx = [
                invoiceId: invoiceId,
                contentTypeId: testParams.contentTypeId ?: 'DOCUMENT',
                invoiceContentTypeId: invoiceContentTypeId,
                text: testParams.text ?: 'Content for invoice # 1009',
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createSimpleTextContentForInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceContent = from('InvoiceContent')
                .where('invoiceId', invoiceId,
                'invoiceContentTypeId', invoiceContentTypeId)
                .queryFirst()

        assert invoiceContent
    }

    @Test
    @Order(3)
    void testCopyInvoice() {
        Map serviceCtx = [
                invoiceIdToCopyFrom: testParams.invoiceIdToCopyFrom ?: '1000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId
    }

    @Test
    @Order(4)
    void testCreateInvoice() {
        Map serviceCtx = [
                invoiceTypeId: testParams.invoiceTypeId ?: 'PURCHASE_INVOICE',
                partyIdFrom: testParams.partyIdFrom ?: 'DEMO_COMPANY',
                partyId: testParams.partyId ?: 'DEMO_COMPANY1',
                invoiceDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId
    }

    @Test
    @Order(5)
    void testGetInvoice() {
        Map serviceCtx = [
                invoiceId: testParams.invoiceId ?: '1001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoice
        assert serviceResult.invoiceItems
    }

    @Test
    @Order(6)
    void testSetInvoiceStatus() {
        String invoiceId = testParams.invoiceId ?: '1002'
        String statusId = testParams.statusId ?: 'INVOICE_APPROVED'
        Map serviceCtx = [
                invoiceId: invoiceId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoice = from('Invoice')
                .where('invoiceId', invoiceId)
                .queryOne()

        assert invoice
        assert invoice.statusId == statusId
    }

    @Test
    @Order(7)
    void testCopyInvoiceToTemplate() {
        Map serviceCtx = [
                invoiceId: testParams.invoiceId ?: '1002',
                invoiceTypeId: testParams.invoiceTypeId ?: 'PURCHASE_INVOICE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyInvoiceToTemplate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceId
    }

    @Test
    @Order(8)
    void testCreateInvoiceItem() {
        Map serviceCtx = [
                invoiceId: testParams.invoiceId ?: '1003',
                invoiceItemTypeId: testParams.invoiceItemTypeId ?: 'PINV_FXASTPRD_ITEM',
                amount: 1,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.invoiceItemSeqId
    }

    @Test
    @Order(9)
    void testCreateInvoiceStatus() {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        String invoiceId = testParams.invoiceId ?: '1004'
        String statusId = testParams.statusId ?: 'INVOICE_IN_PROCESS'
        Map serviceCtx = [
                invoiceId: invoiceId,
                statusId: statusId,
                statusDate: nowTimestamp,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceStatus = from('InvoiceStatus')
                .where('invoiceId', invoiceId,
                        'statusId', statusId,
                        'statusDate', nowTimestamp)
                .queryOne()

        assert invoiceStatus
    }

    @Test
    @Order(10)
    void testCreateInvoiceRole() {
        String invoiceId = testParams.invoiceId ?: '1006'
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String roleTypeId = testParams.roleTypeId ?: 'INTERNAL_ORGANIZATIO'
        Map serviceCtx = [
                invoiceId: invoiceId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceRole = from('InvoiceRole')
                .where('invoiceId', invoiceId,
                        'partyId', partyId,
                        'roleTypeId', roleTypeId)
                .queryOne()

        assert invoiceRole
    }

    @Test
    @Order(11)
    void testCreateInvoiceTerm() {
        Map serviceCtx = [
                invoiceId: testParams.invoiceId ?: '1006',
                invoiceItemSeqId: testParams.invoiceItemSeqId ?: '00001',
                termTypeId: testParams.termTypeId ?: 'FINANCIAL_TERM',
                termValue: 50.00,
                termDays: 10,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceTerm', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceTerm = from('InvoiceTerm')
                .where('invoiceTermId', serviceResult.invoiceTermId)
                .queryOne()

        assert invoiceTerm
    }

    @Test
    @Order(12)
    void testCancelInvoice() {
        Map serviceCtx = [
                invoiceId: testParams.invoiceId ?: '1007',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('cancelInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.invoiceTypeId
    }

}
