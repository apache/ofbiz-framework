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
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgBillingAccountTests implements JupiterTestHelper {

    private String billingAccountId

    @Test
    @Order(1)
    void testCreateBillingAccount() {
        Map serviceCtx = [
                accountLimit: 1000,
                description: 'AutoAcctgBillingAccountTests billing account',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createBillingAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        billingAccountId = serviceResult.billingAccountId
        assert billingAccountId

        GenericValue billingAccount = from('BillingAccount').where('billingAccountId', billingAccountId).queryOne()
        assert billingAccount
    }

    // DEMO_COMPANY already holds the INTERNAL_ORGANIZATIO role (see AccountingTestsData.xml), so this
    // combination is legitimate and must still succeed after OFBIZ-12372's validation is in place.
    @Test
    @Order(2)
    void testCreateBillingAccountRole() {
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        String roleTypeId = 'INTERNAL_ORGANIZATIO'
        Map serviceCtx = [
                billingAccountId: billingAccountId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createBillingAccountRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue billingAccountRole = from('BillingAccountRole')
                .where('billingAccountId', billingAccountId, 'partyId', partyId, 'roleTypeId', roleTypeId)
                .queryOne()
        assert billingAccountRole
    }

    // OFBIZ-12372: DEMO_COMPANY does not hold the CARRIER role (only INTERNAL_ORGANIZATIO/SUPPLIER,
    // see AccountingTestsData.xml), so this combination must be rejected instead of the
    // createBillingAccountRole -> ensurePartyRole eca silently fabricating a PartyRole record for a
    // role the party was never given.
    @Test
    @Order(3)
    void testCreateBillingAccountRoleRejectsPartyWithoutRole() {
        String partyId = testParams.partyId ?: 'DEMO_COMPANY'
        Map serviceCtx = [
                billingAccountId: billingAccountId,
                partyId: partyId,
                roleTypeId: 'CARRIER',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createBillingAccountRole', serviceCtx)
        assert ServiceUtil.isError(serviceResult)

        GenericValue billingAccountRole = from('BillingAccountRole')
                .where('billingAccountId', billingAccountId, 'partyId', partyId, 'roleTypeId', 'CARRIER')
                .queryOne()
        assert !billingAccountRole

        GenericValue spuriousPartyRole = from('PartyRole')
                .where('partyId', partyId, 'roleTypeId', 'CARRIER')
                .queryOne()
        assert !spuriousPartyRole
    }

}
