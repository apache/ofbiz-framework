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
class OrderRequirementTests implements JupiterTestHelper {

    // Requirement related test services
    @Test
    @Order(1)
    void testCheckCreateProductRequirementForFacility() {
        String facilityId = testParams.facilityId ?: 'WebStoreWarehouse'
        String defaultRequirementMethodId = testParams.defaultRequirementMethodId ?: 'PRODRQM_STOCK'
        Map serviceCtx = [
            facilityId: facilityId,
            defaultRequirementMethodId: defaultRequirementMethodId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateProductRequirementForFacility', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(2)
    void testCheckCreateStockRequirementQoh() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String itemIssuanceId = testParams.itemIssuanceId ?: '9006'
        String quantity = testParams.quantity ?: '300'
        Map serviceCtx = [
            orderId: orderId,
            orderItemSeqId: orderItemSeqId,
            shipGroupSeqId: shipGroupSeqId,
            itemIssuanceId: itemIssuanceId,
            quantity: quantity,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateStockRequirementQoh', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(3)
    void testCheckCreateStockRequirementAtp() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10091'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String inventoryItemId = testParams.inventoryItemId ?: '9028'
        String quantity = testParams.quantity ?: '20'
        Map serviceCtx = [
            orderId: orderId,
            orderItemSeqId: orderItemSeqId,
            shipGroupSeqId: shipGroupSeqId,
            inventoryItemId: inventoryItemId,
            quantity: quantity,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateStockRequirementAtp', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(4)
    void testCheckCreateOrderRequirement() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        Map serviceCtx = [
            orderId: orderId,
            orderItemSeqId: orderItemSeqId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateOrderRequirement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(5)
    void testAutoAssignRequirementToSupplier() {
        String requirementId = testParams.requirementId ?: '1000'
        Map serviceCtx = [
            requirementId: requirementId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('autoAssignRequirementToSupplier', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(6)
    void testCreateRequirementCustRequest() {
        String requirementId = testParams.requirementId ?: '1000'
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestItemSeqId = testParams.custRequestItemSeqId ?: '00001'
        Map serviceCtx = [
            requirementId: requirementId,
            custRequestId: custRequestId,
            custRequestItemSeqId: custRequestItemSeqId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirementCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    @Test
    @Order(7)
    void testAddRequirementTask() {
        String requirementId = testParams.requirementId ?: '1000'
        String workEffortId = testParams.workEffortId ?: '9000'
        Map serviceCtx = [
            requirementId: requirementId,
            workEffortId: workEffortId,
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createWorkRequirementFulfillment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
