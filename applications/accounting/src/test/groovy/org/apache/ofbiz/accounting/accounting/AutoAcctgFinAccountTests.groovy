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
        Map serviceCtx = [
                finAccountId: '1000',
                finAccountTypeId: 'BANK_ACCOUNT',
                finAccountName: 'Bank Account',
                finAccountCode: '1000',
                currencyUomId: 'USD',
                organizationPartyId: 'DEMO_COMPANY',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                                    .where('finAccountId', '1000', 'finAccountTypeId', 'BANK_ACCOUNT')
                                    .queryOne()
        assert finAccount
        assert finAccount.finAccountCode == '1000'
    }

    @Test
    @Order(2)
    void testUpdateFinAccount() {
        Map serviceCtx = [
                finAccountId: '1001',
                organizationPartyId: 'DEMO_COMPANY2',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                .where('finAccountId', '1001')
                .queryOne()
        assert finAccount
        assert finAccount.organizationPartyId == 'DEMO_COMPANY2'
    }

    @Test
    @Order(3)
    void testDeleteFinAccount() {
        Map serviceCtx = [
                finAccountId: '1002',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFinAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccount = from('FinAccount')
                .where('finAccountId', '1002')
                .queryOne()
        assert finAccount.thruDate != null
    }

    @Test
    @Order(4)
    void testCreateFinAccountRole() {
        Map serviceCtx = [
                finAccountId: '1003',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'INTERNAL_ORGANIZATIO',
                fromDate: UtilDateTime.nowTimestamp(),
                currencyUomId: 'USD',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', '1003', 'partyId', 'DEMO_COMPANY', 'roleTypeId', 'INTERNAL_ORGANIZATIO')
                .queryFirst()
        assert finAccountRole
    }

    @Test
    @Order(5)
    void testUpdateFinAccountRole() {
        Map serviceCtx = [
                finAccountId: '1004',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'SUPPLIER',
                fromDate: UtilDateTime.toTimestamp('11/03/2016 00:00:00'),
                thruDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', '1004', 'partyId', 'DEMO_COMPANY', 'roleTypeId', 'SUPPLIER')
                .queryFirst()
        assert finAccountRole
        assert finAccountRole.thruDate != null
    }

    @Test
    @Order(6)
    void testDeleteFinAccountRole() {
        Map serviceCtx = [
                finAccountId: '1004',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'SUPPLIER',
                fromDate: UtilDateTime.toTimestamp('11/03/2016 00:00:00'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', '1004', 'partyId', 'DEMO_COMPANY', 'roleTypeId', 'SUPPLIER')
                .queryFirst()
        assert finAccountRole == null
    }

    @Test
    @Order(7)
    void testCreateFinAccountTrans() {
        Map serviceCtx = [
                finAccountId: '1003',
                finAccountTransTypeId: 'ADJUSTMENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountTrans', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountTran = from('FinAccountTrans')
                .where('finAccountId', '1003', 'finAccountTransTypeId', 'ADJUSTMENT')
                .queryFirst()
        assert finAccountTran
    }

    @Test
    @Order(8)
    void testCreateFinAccountStatus() {
        Map serviceCtx = [
                finAccountId: '1003',
                statusId: 'FNACT_ACTIVE',
                statusDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFinAccountStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountStatus = from('FinAccountStatus')
                .where('finAccountId', '1003', 'statusId', 'FNACT_ACTIVE')
                .queryFirst()
        assert finAccountStatus
    }

    @Test
    @Order(9)
    void testCreateFinAccountAuth() {
        Map serviceCtx = [
                finAccountId: '1004',
                amount: new BigDecimal('100'),
                currencyUomId: 'USD',
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
        Map serviceCtx = [
                finAccountTransId: '1010',
                statusId: 'FINACT_TRNS_APPROVED',
                userLogin: userLogin
        ]
        GenericValue finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', '1010')
                .queryOne()
        String oldStatusId = finAccountTrans.statusId

        Map serviceResult = dispatcher.runSync('setFinAccountTransStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', '1010')
                .queryOne()
        assert finAccountTrans
        assert finAccountTrans.statusId == 'FINACT_TRNS_APPROVED'
        assert oldStatusId == 'FINACT_TRNS_CREATED'
    }

}
