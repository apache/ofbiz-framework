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
package org.apache.ofbiz.order.order.test

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class OrderReturnTests extends OFBizTestCase {

    OrderReturnTests(String name) {
        super(name)
    }
    // Return related test services
    void testQuickReturnOrder() {
        String orderId = 'TEST_RTN12432'
        Map serviceCtx = [
            orderId: orderId,
            returnHeaderTypeId: 'CUSTOMER_RETURN',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('quickReturnOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null

        List returnItems = from('ReturnItem').where(returnId: serviceResult.returnId).queryList()
        assert returnItems.size() == 1
        assert returnItems[0].returnQuantity == 2.0G
        assert returnItems[0].returnPrice == 10.00G

        List returnAdjustments = from('ReturnAdjustment').where(returnId: serviceResult.returnId).queryList()
        assert returnAdjustments*.orderAdjustmentId.toSet() == [
                'TEST_RTN12432_ITEM', 'TEST_RTN12432_PROMO', 'TEST_RTN12432_SHIP', 'TEST_RTN12432_TAX'
        ].toSet()
        assert !returnAdjustments.any { it.returnAdjustmentTypeId == 'RET_MAN_ADJ' }

        BigDecimal returnTotal = returnItems.sum(BigDecimal.ZERO) {
            it.returnQuantity * it.returnPrice
        } + returnAdjustments.sum(BigDecimal.ZERO) { it.amount }
        assert returnTotal == 17.00G

        Map invoiceResult = dispatcher.runSync('createInvoiceFromReturn', [
                returnId: serviceResult.returnId,
                billItems: returnItems,
                userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(invoiceResult)
        assert invoiceResult.invoiceId != null

        List invoiceItems = from('InvoiceItem').where(invoiceId: invoiceResult.invoiceId).queryList()
        BigDecimal invoiceTotal = invoiceItems.sum(BigDecimal.ZERO) {
            (it.quantity ?: BigDecimal.ONE) * it.amount
        }
        assert invoiceTotal == returnTotal
    }
    void testProcessCreditReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processCreditReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessCrossShipReplacementReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processCrossShipReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessRefundImmediatelyReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundImmediatelyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testGetReturnItemInitialCost() {
        Map serviceCtx = [
                returnId: '1009',
                returnItemSeqId: '00001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getReturnItemInitialCost', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.initialItemCost != null
    }
    void testProcessRefundReturn() {
        Map serviceCtx = [
                returnId: '1009',
                returnTypeId: 'RTN_REFUND',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessReplacementReturn() {
        Map serviceCtx = [
                returnId: '1009',
                returnTypeId: 'RTN_REFUND',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessReplaceImmediatelyReturn() {
        Map serviceCtx = [
                returnId: '1009',
                orderItemSeqId: '00001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processReplaceImmediatelyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessRefundOnlyReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundOnlyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessWaitReplacementReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processWaitReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessWaitReplacementReservedReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processWaitReplacementReservedReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult != null
    }
    void testProcessSubscriptionReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processSubscriptionReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnAndItemOrAdjustment() {
        Map serviceCtx = [
                orderId: 'DEMO10090',
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnAndItemOrAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testCreateReturnAdjustment() {
        Map serviceCtx = [
                amount: '2.0000',
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testCheckReturnComplete() {
        Map serviceCtx = [
                amount: '2.0000',
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkReturnComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.statusId != null
    }
    void testCheckPaymentAmountForRefund() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkPaymentAmountForRefund', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnItemShipment() {
        Map serviceCtx = [
                shipmentId: '1014',
                shipmentItemSeqId: '00001',
                returnId: '1009',
                returnItemSeqId: '00001',
                quantity: 2.0000,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnItemShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnStatus() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testGetReturnAmountByOrder() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getReturnAmountByOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnHeader() {
        Map serviceCtx = [
                toPartyId: 'Company',
                returnHeaderTypeId: 'CUSTOMER_RETURN',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnHeader', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
    void testProcessRefundReturnForReplacement() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundReturnForReplacement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testProcessRepairReplacementReturn() {
        Map serviceCtx = [
                returnId: '1009',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRepairReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
