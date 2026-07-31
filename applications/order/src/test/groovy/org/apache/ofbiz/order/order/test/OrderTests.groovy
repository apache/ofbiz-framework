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
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderDeliverySchedule', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(2)
    void testCreateOrderItemChange() {
        Map serviceCtx = [
                changeTypeEnumId: 'ODR_ITM_APPEND',
                orderId: 'TEST_DEMO10090',
                orderItemSeqId: '00001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderItemChange', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.orderItemChangeId
    }

    @Test
    @Order(3)
    void testCreateOrderPaymentApplication() {
        Map serviceCtx = [
                paymentId: '1014',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderPaymentApplication', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(4)
    void testCreateRequirement() {
        Map serviceCtx = [
                custRequestId: '9000',
                requirementTypeId: 'CUSTOMER_REQUIREMENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(5)
    void testGetRequirementsForSupplier() {
        Map serviceCtx = [
                partyId: 'Company',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRequirementsForSupplier', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(6)
    void testCreateRequirementRole() {
        Map serviceCtx = [
                requirementId: '1000',
                partyId: 'Company',
                roleTypeId: 'OWNER',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirementRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(7)
    void testCreateAutoRequirementsForOrder() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAutoRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    @Test
    @Order(8)
    void testCreateATPRequirementsForOrder() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createATPRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
