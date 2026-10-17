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
package org.apache.ofbiz.party.party

roleTypeId = parameters.roleTypeId
roleTypeAndParty = from('RoleTypeAndParty').where('partyId', parameters.partyId, 'roleTypeId', roleTypeId).queryFirst()
if (roleTypeAndParty) {
    switch (roleTypeId) {
        case 'ACCOUNT':
            context.accountDescription = roleTypeAndParty.description
            break
        case 'CONTACT':
            context.contactDescription = roleTypeAndParty.description
            break
        case 'LEAD':
            context.leadDescription = roleTypeAndParty.description
            partyRelationships = from('PartyRelationship')
                    .where('partyIdTo', parameters.partyId, 'roleTypeIdFrom', 'ACCOUNT_LEAD', 'roleTypeIdTo', 'LEAD',
                            'partyRelationshipTypeId', 'EMPLOYMENT')
                    .filterByDate().queryFirst()
            if (partyRelationships) {
                context.partyGroupId = partyRelationships.partyIdFrom
                context.partyId = parameters.partyId
            }
            break
            break
        case 'ACCOUNT_LEAD':
            context.accountLeadDescription = roleTypeAndParty.description
            partyRelationships = from('PartyRelationship')
                    .where('partyIdFrom', parameters.partyId, 'roleTypeIdFrom', 'ACCOUNT_LEAD', 'roleTypeIdTo', 'LEAD',
                            'partyRelationshipTypeId', 'EMPLOYMENT')
                    .filterByDate().queryFirst()
            if (partyRelationships) {
                context.partyGroupId = parameters.partyId
                context.partyId = partyRelationships.partyIdTo
            }
            break
    }
}
