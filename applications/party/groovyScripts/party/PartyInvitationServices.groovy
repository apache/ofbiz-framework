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


import org.apache.ofbiz.party.party.PartyHelper

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue

// Party Invitation Services
def createPartyInvitation() {
    Map result = success()
    GenericValue newEntity = makeValue("PartyInvitation", parameters)
    newEntity.partyInvitationId = delegator.getNextSeqId("PartyInvitation")
    result.partyInvitationId = newEntity.partyInvitationId
    if (! parameters.toName && parameters.partyId) {
        newEntity.toName = PartyHelper.getPartyName(delegator, parameters.partyId, false)
    }
    if (!newEntity.lastInviteDate) {
        newEntity.lastInviteDate = UtilDateTime.nowTimestamp()
    }
    newEntity.create()
    return result
}

def updatePartyInvitation() {
    GenericValue lookedUpValue = makeValue("PartyInvitation", parameters)
    if (! parameters.toName && parameters.partyId) {
        newEntity.toName = PartyHelper.getPartyName(delegator, parameters.partyId, false)
    }
    lookedUpValue.store()
    return success()
}

def acceptPartyInvitation() {
    List partyInvitationGroupAssocs = from("PartyInvitationGroupAssoc")
            .where(partyInvitationId: parameters.partyInvitationId)
            .queryList()
    if (partyInvitationGroupAssocs) {
        Map createPartyRelationshipCtx = [partyIdTo: parameters.partyId,
                                          partyRelationshipTypeId: "GROUP_ROLLUP"]
        partyInvitationGroupAssocs.each {
            createPartyRelationshipCtx.partyIdFrom = it.partyIdTo
            run service: "createPartyRelationship", with: createPartyRelationshipCtx
        }
    }

    List partyInvitationRoleAssocs = from("PartyInvitationRoleAssoc")
            .where(partyInvitationId: parameters.partyInvitationId)
            .queryList()
    if (partyInvitationRoleAssocs) {
        Map ensurePartyRoleCtx = [partyId: parameters.partyId]
        partyInvitationRoleAssocs.each {
            ensurePartyRoleCtx.roleTypeId = it.roleTypeId
            run service: "ensurePartyRole", with: ensurePartyRoleCtx
        }
    }

    Map updatePartyInvitationCtx = [partyInvitationId: parameters.partyInvitationId,
                                    statusId: "PARTYINV_ACCEPTED"]
    Map result = run service: "updatePartyInvitation", with: updatePartyInvitationCtx
    return result
}

