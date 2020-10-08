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
package org.apache.ofbiz.party

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class PartyStatusChangeTests extends OFBizTestCase {
    public PartyStatusChangeTests(String name) {
        super(name)
    }

    // Test case for changing party status to PARTY_DISABLED
    void testSetPartyStatusToDisabled() {
        String partyId = 'PARTY_ENABLED'
        String statusId = 'PARTY_DISABLED'

        Map serviceCtx = [
                partyId: partyId,
                statusId: statusId,
                statusDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setPartyStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from("Party")
                .where('partyId', partyId)
                .queryOne()
        assert party
        assert statusId.equals(party.statusId)
        assert 'PARTY_ENABLED'.equals(serviceResult.oldStatusId)
    }

    // Test case for changing party status to PARTY_ENABLED
    void testSetPartyStatusToEnabled() {
        String partyId = 'PARTY_DISABLED'
        String statusId = 'PARTY_ENABLED'

        Map serviceCtx = [
                partyId: partyId,
                statusId: statusId,
                statusDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setPartyStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from("Party")
                .where('partyId', partyId)
                .queryOne()
        assert party
        assert statusId.equals(party.statusId)
        assert 'PARTY_DISABLED'.equals(serviceResult.oldStatusId)
    }
}