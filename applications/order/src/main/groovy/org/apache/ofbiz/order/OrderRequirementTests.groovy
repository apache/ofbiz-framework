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
package org.apache.ofbiz.order

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class OrderRequirementTests extends OFBizTestCase {
    public OrderRequirementTests(String name) {
        super(name)
    }
    // Requirement related test services
    void testCheckCreateProductRequirementForFacility() {
        Map serviceCtx = [
            facilityId: 'WebStoreWarehouse',
            orderItemSeqId: '00001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateProductRequirementForFacility', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCheckCreateStockRequirementQoh() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            orderItemSeqId: '00001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateStockRequirementQoh', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCheckCreateStockRequirementAtp() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            orderItemSeqId: '00001',
            shipGroupSeqId: '00001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateStockRequirementAtp', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCheckCreateOrderRequirement() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            orderItemSeqId: '00001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('checkCreateOrderRequirement', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testAutoAssignRequirementToSupplier() {
        Map serviceCtx = [
            requirementId: '1000',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('autoAssignRequirementToSupplier', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testCreateRequirementCustRequest() {
        Map serviceCtx = [
            requirementId: '1000',
            custRequestId: '9000',
            custRequestItemSeqId: '00001',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createRequirementCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
    void testAddRequirementTask() {
        Map serviceCtx = [
            requirementId: '1000',
            workEffortId: '9000',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync("addRequirementTask", serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }
}
