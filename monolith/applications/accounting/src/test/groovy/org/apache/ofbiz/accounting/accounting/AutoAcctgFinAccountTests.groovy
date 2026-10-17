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
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgFinAccountTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateFinAccount() {
        String finAccountId = testParams.finAccountId ?: '1000'
        String finAccountTypeId = testParams.finAccountTypeId ?: 'BANK_ACCOUNT'
        String finAccountCode = testParams.finAccountCode ?: '1000'
        Map serviceCtx = [
                finAccountId: finAccountId,
                finAccountTypeId: finAccountTypeId,
                finAccountName: testParams.finAccountName ?: 'Bank Account',
                finAccountCode: finAccountCode,
                currencyUomId: testParams.currencyUomId ?: 'USD',
                organizationPartyId: testParams.organizationPartyId ?: 'DEMO_COMPANY',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                                    .where('finAccountId', finAccountId, 'finAccountTypeId', finAccountTypeId)
                                    .queryOne()
        assert finAccount
        assert finAccount.finAccountCode == finAccountCode
    }

    @Test
    @Order(2)
    void testUpdateFinAccount() {
        String finAccountId = testParams.finAccountId ?: '1001'
        String organizationPartyId = testParams.organizationPartyId ?: 'DEMO_COMPANY2'
        Map serviceCtx = [
                finAccountId: finAccountId,
                organizationPartyId: organizationPartyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                .where('finAccountId', finAccountId)
                .queryOne()
        assert finAccount
        assert finAccount.organizationPartyId == organizationPartyId
    }

    @Test
    @Order(3)
    void testDeleteFinAccount() {
        String finAccountId = testParams.finAccountId ?: '1002'
        Map serviceCtx = [
                finAccountId: finAccountId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                .where('finAccountId', finAccountId)
                .queryOne()
        assert finAccount.thruDate != null
    }

    @Test
    @Order(4)
    void testCreateFinAccountRole() {
        String finAccountId = testParams.finAccountId ?: '1003'
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String roleTypeId = testParams.roleTypeId ?: 'INTERNAL_ORGANIZATIO'
        Map serviceCtx = [
                finAccountId: finAccountId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                fromDate: UtilDateTime.nowTimestamp(),
                currencyUomId: testParams.currencyUomId ?: 'USD',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', finAccountId, 'partyId', partyId, 'roleTypeId', roleTypeId)
                .queryFirst()
        assert finAccountRole
    }

    @Test
    @Order(5)
    void testUpdateFinAccountRole() {
        String finAccountId = testParams.finAccountId ?: '1004'
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String roleTypeId = testParams.roleTypeId ?: 'SUPPLIER'
        Map serviceCtx = [
                finAccountId: finAccountId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                fromDate: UtilDateTime.toTimestamp('11/03/2016 00:00:00'),
                thruDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', finAccountId, 'partyId', partyId, 'roleTypeId', roleTypeId)
                .queryFirst()
        assert finAccountRole
        assert finAccountRole.thruDate != null
    }

    @Test
    @Order(6)
    void testDeleteFinAccountRole() {
        String finAccountId = testParams.finAccountId ?: '1004'
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String roleTypeId = testParams.roleTypeId ?: 'SUPPLIER'
        Map serviceCtx = [
                finAccountId: finAccountId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                fromDate: UtilDateTime.toTimestamp('11/03/2016 00:00:00'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', finAccountId, 'partyId', partyId, 'roleTypeId', roleTypeId)
                .queryFirst()
        assert finAccountRole == null
    }

    @Test
    @Order(7)
    void testCreateFinAccountTrans() {
        String finAccountId = testParams.finAccountId ?: '1003'
        String finAccountTransTypeId = testParams.finAccountTransTypeId ?: 'ADJUSTMENT'
        Map serviceCtx = [
                finAccountId: finAccountId,
                finAccountTransTypeId: finAccountTransTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountTrans', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountTran = from('FinAccountTrans')
                .where('finAccountId', finAccountId, 'finAccountTransTypeId', finAccountTransTypeId)
                .queryFirst()
        assert finAccountTran
    }

    @Test
    @Order(8)
    void testCreateFinAccountStatus() {
        String finAccountId = testParams.finAccountId ?: '1003'
        String statusId = testParams.statusId ?: 'FNACT_ACTIVE'
        Map serviceCtx = [
                finAccountId: finAccountId,
                statusId: statusId,
                statusDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountStatus = from('FinAccountStatus')
                .where('finAccountId', finAccountId, 'statusId', statusId)
                .queryFirst()
        assert finAccountStatus
    }

    @Test
    @Order(9)
    void testCreateFinAccountAuth() {
        String finAccountId = testParams.finAccountId ?: '1004'
        String currencyUomId = testParams.currencyUomId ?: 'USD'
        Map serviceCtx = [
                finAccountId: finAccountId,
                amount: new BigDecimal('100'),
                currencyUomId: currencyUomId,
                authorizationDate: UtilDateTime.nowTimestamp(),
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountAuth', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.finAccountAuthId != null
    }

    @Test
    @Order(10)
    void testSetFinAccountTransStatus() {
        String finAccountTransId = testParams.finAccountTransId ?: '1010'
        String statusId = testParams.statusId ?: 'FINACT_TRNS_APPROVED'
        Map serviceCtx = [
                finAccountTransId: finAccountTransId,
                statusId: statusId,
                userLogin: userLogin
        ]
        GenericValue finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', finAccountTransId)
                .queryOne()
        String oldStatusId = finAccountTrans.statusId

        Map serviceResult = dispatcher.runSync('setFinAccountTransStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', finAccountTransId)
                .queryOne()
        assert finAccountTrans
        assert finAccountTrans.statusId == statusId
        assert oldStatusId == 'FINACT_TRNS_CREATED'
    }

}
