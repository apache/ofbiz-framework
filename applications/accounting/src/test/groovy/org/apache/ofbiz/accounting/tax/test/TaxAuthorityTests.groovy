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
package org.apache.ofbiz.accounting.tax.test

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

import java.sql.Timestamp

@JunitJupiterTest
class TaxAuthorityTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testUpdatePartyTaxAuthInfoPreservesEmptyFieldsWhileApplyingRealChanges() {
        String partyId = userLogin.partyId
        String taxAuthGeoId = 'USA'
        String taxAuthPartyId = partyId
        Timestamp fromDate = UtilDateTime.nowTimestamp()

        Map createTaxAuthorityCtx = [taxAuthGeoId: taxAuthGeoId, taxAuthPartyId: taxAuthPartyId, userLogin: userLogin]
        Map createTaxAuthorityResult = dispatcher.runSync('createTaxAuthority', createTaxAuthorityCtx)
        assert ServiceUtil.isSuccess(createTaxAuthorityResult)

        Map createCtx = [partyId: partyId, taxAuthGeoId: taxAuthGeoId, taxAuthPartyId: taxAuthPartyId,
                          fromDate: fromDate, isExempt: 'Y', isNexus: 'N', userLogin: userLogin]
        Map createResult = dispatcher.runSync('createPartyTaxAuthInfo', createCtx)
        assert ServiceUtil.isSuccess(createResult)

        // isExempt is passed as an empty string here -- with setIfEmpty=false this must NOT null out
        // the 'Y' set above, while isNexus (a real, non-empty change) must still apply.
        Map updateCtx = [partyId: partyId, taxAuthGeoId: taxAuthGeoId, taxAuthPartyId: taxAuthPartyId,
                          fromDate: fromDate, isExempt: '', isNexus: 'Y', userLogin: userLogin]
        Map updateResult = dispatcher.runSync('updatePartyTaxAuthInfo', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        GenericValue partyTaxAuthInfo = from('PartyTaxAuthInfo')
                .where('partyId', partyId, 'taxAuthGeoId', taxAuthGeoId, 'taxAuthPartyId', taxAuthPartyId, 'fromDate', fromDate)
                .queryOne()
        assert partyTaxAuthInfo.isNexus == 'Y'
        assert partyTaxAuthInfo.isExempt == 'Y'
    }

    @Test
    @Order(2)
    void testUpdatePartyTaxAuthInfoNotFoundPreservesOriginalMessage() {
        Map updateCtx = [partyId: 'DoesNotExist12345', taxAuthGeoId: 'USA', taxAuthPartyId: 'DoesNotExist12345',
                          fromDate: UtilDateTime.nowTimestamp(), isNexus: 'Y', userLogin: userLogin]
        Map updateResult = dispatcher.runSync('updatePartyTaxAuthInfo', updateCtx)
        assert ServiceUtil.isError(updateResult)
        assert updateResult.errorMessage == 'PartyTaxAuthInfo not found for the given parameters'
    }

}
