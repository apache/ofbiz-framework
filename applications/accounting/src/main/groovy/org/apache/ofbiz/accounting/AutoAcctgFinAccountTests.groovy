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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

class AutoAcctgFinAccountTests extends OFBizTestCase {
    public AutoAcctgFinAccountTests(String name) {
        super(name)
    }

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

    void testUpdateFinAccountRole() {
        Map serviceCtx = [
                finAccountId: '1004',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'SUPPLIER',
                fromDate: UtilDateTime.toTimestamp("11/03/2016 00:00:00"),
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

    void testDeleteFinAccountRole() {
        Map serviceCtx = [
                finAccountId: '1004',
                partyId: 'DEMO_COMPANY',
                roleTypeId: 'SUPPLIER',
                fromDate: UtilDateTime.toTimestamp("11/03/2016 00:00:00"),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteFinAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountRole = from('FinAccountRole')
                .where('finAccountId', '1004', 'partyId', 'DEMO_COMPANY', 'roleTypeId', 'SUPPLIER')
                .queryFirst()
        assert finAccountRole == null
    }

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

    void setFinAccountTransStatus() {
        Map serviceCtx = [
                finAccountTransId: '1010',
                statusId: 'FINACT_TRNS_APPROVED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setFinAccountTransStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue finAccountTrans = from('FinAccountTrans')
                .where('finAccountTransId', '1010')
                .queryOne()
        assert finAccountTrans
        assert finAccountTrans.statusId == 'FINACT_TRNS_APPROVED'
        assert finAccountTrans.oldStatusId == 'FINACT_TRNS_CREATED'
    }
}
