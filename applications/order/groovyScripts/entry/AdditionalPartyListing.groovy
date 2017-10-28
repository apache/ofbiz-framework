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

import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents

cart = ShoppingCartEvents.getCartObject(request)
additionalPartyRole = cart.getAdditionalPartyRoleMap()

roleData = [:]
partyData = [:]

additionalPartyRole.each { roleTypeId, partyList ->
    roleData[roleTypeId] = from("RoleType").where("roleTypeId", roleTypeId).queryOne()

    partyList.each { partyId ->
        partyMap = [:]
        partyMap.partyId = partyId
        party = from("Party").where("partyId", partyId).cache(true).queryOne()
        if ("PERSON".equals(party.partyTypeId)) {
            party = party.getRelatedOne("Person", true)
            partyMap.type = "person"
            partyMap.firstName = party.firstName
            partyMap.lastName = party.lastName
        } else {
            party = party.getRelatedOne("PartyGroup", true)
            partyMap.type = "group"
            partyMap.groupName = party.groupName
        }
        partyData[partyId] = partyMap
    }
}

context.additionalPartyRoleMap = additionalPartyRole
context.roleList = additionalPartyRole.keySet()
context.roleData = roleData
context.partyData = partyData
