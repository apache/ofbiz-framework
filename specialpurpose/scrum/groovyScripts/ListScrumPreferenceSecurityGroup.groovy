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

import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

partyId = parameters.partyId

andCond = []
orCond = []
combinedCondList = []

andCond.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId))
andCond.add(EntityCondition.makeCondition("partyStatusId", EntityOperator.EQUALS, "PARTY_ENABLED"))

orCond.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "SCRUM_PRODUCT_OWNER"))
orCond.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "SCRUM_MASTER"))
orCond.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "SCRUM_TEAM"))
orCond.add(EntityCondition.makeCondition("enabled", EntityOperator.EQUALS, "Y"))
orCond.add(EntityCondition.makeCondition("enabled", EntityOperator.EQUALS, null))

orCondList = EntityCondition.makeCondition(orCond, EntityOperator.OR)
andCondList = EntityCondition.makeCondition(andCond, EntityOperator.AND)

combinedCondList.add(orCondList)
combinedCondList.add(andCondList)

combinedConds = EntityCondition.makeCondition(combinedCondList, EntityOperator.AND)

scrumUserLoginSecurityGroupList = from("ScrumMemberUserLoginAndSecurityGroup").where(combinedConds).queryList()
userPreferenceList = []
userPreferenceOutList = []
if (scrumUserLoginSecurityGroupList) {
    scrumUserLoginSecurityGroupList.each { scrumUserLoginSecurityGroupMap ->
        if (scrumUserLoginSecurityGroupMap.groupId == "SCRUM_PRODUCT_OWNER") {
            ownerCond = []
            ownerCond.add(EntityCondition.makeCondition("enumTypeId", EntityOperator.EQUALS, "SCRUM_PREFERENCE"))
            ownerCond.add(EntityCondition.makeCondition("enumId", EntityOperator.NOT_EQUAL, "MASTER_NOTIFY"))
            ownerConds = EntityCondition.makeCondition(ownerCond, EntityOperator.AND)
            userPreferenceList = from("Enumeration").where(ownerConds).queryList()
        } else if (scrumUserLoginSecurityGroupMap.groupId == "SCRUM_MASTER") {
            masterCond = []
            masterCond.add(EntityCondition.makeCondition("enumTypeId", EntityOperator.EQUALS, "SCRUM_PREFERENCE"))
            masterCond.add(EntityCondition.makeCondition("enumId", EntityOperator.EQUALS, "MASTER_NOTIFY"))
            masterConds = EntityCondition.makeCondition(masterCond, EntityOperator.AND)
            userPreferenceList = from("Enumeration").where(masterConds).queryList()
        } /*else if (scrumUserLoginSecurityGroupMap.groupId == "SCRUM_TEAM") {
            teamCond = []
            teamCond.add(EntityCondition.makeCondition("enumTypeId", EntityOperator.EQUALS, "SCRUM_PREFERENCE"))
            teamCond.add(EntityCondition.makeCondition("enumId", EntityOperator.EQUALS, "INVITE_NOTIFI"))
            teamConds = EntityCondition.makeCondition(teamCond, EntityOperator.AND)
            userPreferenceList = delegator.findList("Enumeration" , teamConds, null, null, null, false)
        }*/
        if (userPreferenceList) {
            userPreferenceList.each { userPreferenceMap ->
                userPreferenceOutList.add(userPreferenceMap)
            }
        }
    }
    context.userPreferenceList = userPreferenceOutList
} else {
    if (security.hasEntityPermission("SCRUM", "_ADMIN", session)) {
        userPreferenceList = from("Enumeration").where("enumTypeId", "SCRUM_PREFERENCE").queryList()
        context.userPreferenceList = userPreferenceList
    }
}

