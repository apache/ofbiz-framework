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
package org.apache.ofbiz.order.order.test

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class OrderTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateOrderDeliverySchedule() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        Map serviceCtx = [
                orderId: orderId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderDeliverySchedule', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(2)
    void testCreateOrderItemChange() {
        String changeTypeEnumId = testParams.changeTypeEnumId ?: 'ODR_ITM_APPEND'
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        Map serviceCtx = [
                changeTypeEnumId: changeTypeEnumId,
                orderId: orderId,
                orderItemSeqId: orderItemSeqId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderItemChange', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.orderItemChangeId
    }

    @Test
    @Order(3)
    void testCreateOrderPaymentApplication() {
        String paymentId = testParams.paymentId ?: '1014'
        Map serviceCtx = [
                paymentId: paymentId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderPaymentApplication', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(4)
    void testCreateRequirement() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String requirementTypeId = testParams.requirementTypeId ?: 'CUSTOMER_REQUIREMENT'
        Map serviceCtx = [
                custRequestId: custRequestId,
                requirementTypeId: requirementTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(5)
    void testGetRequirementsForSupplier() {
        String partyId = testParams.partyId ?: 'Company'
        Map serviceCtx = [
                partyId: partyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRequirementsForSupplier', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(6)
    void testCreateRequirementRole() {
        String requirementId = testParams.requirementId ?: '1000'
        String partyId = testParams.partyId ?: 'Company'
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [
                requirementId: requirementId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirementRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(7)
    void testCreateAutoRequirementsForOrder() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        Map serviceCtx = [
                orderId: orderId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAutoRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(8)
    void testCreateATPRequirementsForOrder() {
        String orderId = testParams.orderId ?: 'TEST_DEMO10090'
        Map serviceCtx = [
                orderId: orderId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createATPRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
