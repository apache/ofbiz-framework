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

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

import javolution.util.FastList;

partyId = parameters.party_id;
if (!partyId) partyId = parameters.partyId;
if (!partyId) partyId = (String) request.getAttribute("partyId");
context.partyId = partyId;

List roleTypeAndPartyExprs = FastList.newInstance();
expr = EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId);
roleTypeAndPartyExprs.add(expr); 
expr = EntityCondition.makeCondition("roleTypeId", EntityOperator.NOT_EQUAL, "_NA_");
roleTypeAndPartyExprs.add(expr);
ecl = EntityCondition.makeCondition(roleTypeAndPartyExprs, EntityOperator.AND);

partyRoles = delegator.findList("RoleTypeAndParty", ecl, null, ["description"], null, false);
context.partyRoles = partyRoles;

roles = delegator.findList("RoleType", null, null, ["description", "roleTypeId"], null, false);
context.roles = roles;

party = delegator.findByPrimaryKey("Party", [partyId : partyId]);
context.party = party;
if (party) {
    context.lookupPerson = party.getRelatedOne("Person");
    context.lookupGroup = party.getRelatedOne("PartyGroup");
}