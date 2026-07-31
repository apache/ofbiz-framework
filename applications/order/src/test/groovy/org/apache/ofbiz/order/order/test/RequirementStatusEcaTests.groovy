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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.transaction.TransactionUtil
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

@JunitJupiterTest
class RequirementStatusEcaTests implements JupiterTestHelper {

    @Test
    void testCreateRequirementStatusAfterOuterTransactionCommit() {
        ensureRequirementReferenceData()
        GenericValue userLogin = ensureUserLogin('mrpRequirementStatusTest')
        assert userLogin

        boolean beganTransaction = false
        String requirementId = null

        try {
            beganTransaction = TransactionUtil.begin()
            Map serviceCtx = [
                requirementTypeId: 'CUSTOMER_REQUIREMENT',
                statusId: 'REQ_PROPOSED',
                userLogin: userLogin
            ]
            Map serviceResult = dispatcher.runSync('createRequirement', serviceCtx)
            assert ServiceUtil.isSuccess(serviceResult)

            requirementId = serviceResult.requirementId
            assert requirementId
            assert from('Requirement').where('requirementId', requirementId).queryCount() == 1

            sleep(500L)
            assert from('RequirementStatus').where('requirementId', requirementId, 'statusId', 'REQ_PROPOSED').queryCount() == 0

            TransactionUtil.commit(beganTransaction)
            beganTransaction = false
        } finally {
            if (beganTransaction) {
                TransactionUtil.rollback(beganTransaction, 'Rolling back unfinished requirement status ECA test transaction', null)
            }
        }

        assert waitForRequirementStatus(requirementId, 'REQ_PROPOSED')
    }

    private boolean waitForRequirementStatus(String requirementId, String statusId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (from('RequirementStatus').where('requirementId', requirementId, 'statusId', statusId).queryCount() == 1) {
                return true
            }
            sleep(100L)
        }
        return false
    }

    private GenericValue ensureUserLogin(String userLoginId) {
        GenericValue userLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
        if (userLogin == null) {
            userLogin = delegator.makeValue('UserLogin', [
                userLoginId: userLoginId,
                userFullName: 'MRP Requirement Status Test User',
                enabled: 'Y'
            ])
            delegator.create(userLogin)
        }
        return userLogin
    }

    private void ensureRequirementReferenceData() {
        if (from('RequirementType').where('requirementTypeId', 'CUSTOMER_REQUIREMENT').queryOne() == null) {
            delegator.create(delegator.makeValue('RequirementType', [
                requirementTypeId: 'CUSTOMER_REQUIREMENT',
                hasTable: 'N',
                description: 'Customer Requirement'
            ]))
        }
        if (from('StatusType').where('statusTypeId', 'REQUIREMENT_STATUS').queryOne() == null) {
            delegator.create(delegator.makeValue('StatusType', [
                statusTypeId: 'REQUIREMENT_STATUS',
                description: 'Requirement Status'
            ]))
        }
        if (from('StatusItem').where('statusId', 'REQ_PROPOSED').queryOne() == null) {
            delegator.create(delegator.makeValue('StatusItem', [
                statusId: 'REQ_PROPOSED',
                statusTypeId: 'REQUIREMENT_STATUS',
                statusCode: 'PROPOSED',
                sequenceId: '01',
                description: 'Proposed'
            ]))
        }
    }

}
