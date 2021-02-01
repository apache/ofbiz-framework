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

package org.apache.ofbiz.order

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class OrderTests extends OFBizTestCase {
    public OrderTests(String name) {
        super(name)
    }

    void testCreateOrderDeliverySchedule() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderDeliverySchedule', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

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

    void testCreateOrderPaymentApplication() {
        Map serviceCtx = [
                paymentId: '1014',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createOrderPaymentApplication', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    void testCreateRequirement() {
        Map serviceCtx = [
                custRequestId: '9000',
                requirementTypeId: 'CUSTOMER_REQUIREMENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    void testGetRequirementsForSupplier() {
        Map serviceCtx = [
                partyId: 'Company',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRequirementsForSupplier', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

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

    void testCreateAutoRequirementsForOrder() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAutoRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

    void testCreateATPRequirementsForOrder() {
        Map serviceCtx = [
                orderId: 'TEST_DEMO10090',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createATPRequirementsForOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
}