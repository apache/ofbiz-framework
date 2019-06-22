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

import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.GroovyScriptTestCase

class OrderTests extends GroovyScriptTestCase {
    void testAddRequirementTask() {
        Map serviceCtx = [
            requirementId: '1000',
            workEffortId: '9000',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync("addRequirementTask", serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnAdjustment() {
        Map serviceCtx = [
            amount: '2.0000',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testQuickReturnOrder() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            returnHeaderTypeId: 'CUSTOMER_RETURN',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('quickReturnOrder', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
    void testCreateReturnAndItemOrAdjustment() {
        Map serviceCtx = [
            orderId: 'DEMO10090',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnAndItemOrAdjustment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnAdjustmentId != null
    }
    void testCheckReturnComplete() {
        Map serviceCtx = [
            amount: '2.0000',
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkReturnComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.statusId != null
    }
    void testCheckPaymentAmountForRefund() {
        Map serviceCtx = [
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkPaymentAmountForRefund', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    //TODO: This can be moved to a different file
    void testCheckCreateProductRequirementForFacility() {
        Map serviceCtx = [
            facilityId: 'WebStoreWarehouse',
            orderItemSeqId: '00001',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('checkCreateProductRequirementForFacility', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnItemShipment() {
        Map serviceCtx = [
            shipmentId: '1014',
            shipmentItemSeqId: '00001',
            returnId: '1009',
            returnItemSeqId: '00001',
            quantity: new BigDecimal('2.0000'),
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnItemShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnStatus() {
        Map serviceCtx = [
            returnId: '1009',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateReturnHeader() {
        Map serviceCtx = [
            toPartyId: 'Company',
            returnHeaderTypeId: 'CUSTOMER_RETURN',
            userLogin: EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        ]
        Map serviceResult = dispatcher.runSync('createReturnHeader', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.returnId != null
    }
}