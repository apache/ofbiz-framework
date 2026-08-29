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
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class OrderReturnTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testQuickReturnOrder() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        String returnHeaderTypeId = testParams.returnHeaderTypeId ?: 'CUSTOMER_RETURN'
        Map serviceCtx = [
            orderId: orderId,
            returnHeaderTypeId: returnHeaderTypeId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('quickReturnOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
    @Test
    @Order(2)
    void testProcessCreditReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processCreditReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(3)
    void testProcessCrossShipReplacementReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processCrossShipReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(4)
    void testProcessRefundImmediatelyReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundImmediatelyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(5)
    void testGetReturnItemInitialCost() {
        String returnId = testParams.returnId ?: '1009'
        String returnItemSeqId = testParams.returnItemSeqId ?: '00001'
        Map serviceCtx = [
                returnId: returnId,
                returnItemSeqId: returnItemSeqId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getReturnItemInitialCost', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.initialItemCost != null
    }
    @Test
    @Order(6)
    void testProcessRefundReturn() {
        String returnId = testParams.returnId ?: '1009'
        String returnTypeId = testParams.returnTypeId ?: 'RTN_REFUND'
        Map serviceCtx = [
                returnId: returnId,
                returnTypeId: returnTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(7)
    void testProcessReplacementReturn() {
        String returnId = testParams.returnId ?: '1009'
        String returnTypeId = testParams.returnTypeId ?: 'RTN_REFUND'
        Map serviceCtx = [
                returnId: returnId,
                returnTypeId: returnTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(8)
    void testProcessReplaceImmediatelyReturn() {
        String returnId = testParams.returnId ?: '1009'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        Map serviceCtx = [
                returnId: returnId,
                orderItemSeqId: orderItemSeqId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processReplaceImmediatelyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(9)
    void testProcessRefundOnlyReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundOnlyReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(10)
    void testProcessWaitReplacementReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processWaitReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(11)
    void testProcessWaitReplacementReservedReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processWaitReplacementReservedReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult != null
    }
    @Test
    @Order(12)
    void testProcessSubscriptionReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processSubscriptionReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(13)
    void testCreateReturnAndItemOrAdjustment() {
        String orderId = testParams.orderId ?: 'DEMO10090'
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                orderId: orderId,
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnAndItemOrAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    @Test
    @Order(14)
    void testCreateReturnAdjustment() {
        String amount = testParams.amount ?: '2.0000'
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                amount: amount,
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    @Test
    @Order(15)
    void testCheckReturnComplete() {
        String amount = testParams.amount ?: '2.0000'
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                amount: amount,
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkReturnComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.statusId != null
    }
    @Test
    @Order(16)
    void testCheckPaymentAmountForRefund() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkPaymentAmountForRefund', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(17)
    void testCreateReturnItemShipment() {
        String shipmentId = testParams.shipmentId ?: '1014'
        String shipmentItemSeqId = testParams.shipmentItemSeqId ?: '00001'
        String returnId = testParams.returnId ?: '1009'
        String returnItemSeqId = testParams.returnItemSeqId ?: '00001'
        Map serviceCtx = [
                shipmentId: shipmentId,
                shipmentItemSeqId: shipmentItemSeqId,
                returnId: returnId,
                returnItemSeqId: returnItemSeqId,
                quantity: 2.0000,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnItemShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(18)
    void testCreateReturnStatus() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(19)
    void testGetReturnAmountByOrder() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getReturnAmountByOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(20)
    void testCreateReturnHeader() {
        String toPartyId = testParams.toPartyId ?: 'Company'
        String returnHeaderTypeId = testParams.returnHeaderTypeId ?: 'CUSTOMER_RETURN'
        Map serviceCtx = [
                toPartyId: toPartyId,
                returnHeaderTypeId: returnHeaderTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createReturnHeader', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
    @Test
    @Order(21)
    void testProcessRefundReturnForReplacement() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        Map serviceCtx = [
                orderId: orderId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRefundReturnForReplacement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(22)
    void testProcessRepairReplacementReturn() {
        String returnId = testParams.returnId ?: '1009'
        Map serviceCtx = [
                returnId: returnId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('processRepairReplacementReturn', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
