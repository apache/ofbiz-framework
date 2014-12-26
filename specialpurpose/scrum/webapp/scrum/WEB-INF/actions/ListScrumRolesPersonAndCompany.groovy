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

import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.EntityUtil;

productId = parameters.productId
personAndCompanyList = [];

if (productId) {
    productRoleList = from("ProductRole").where("productId" : productId, "roleTypeId" : "PRODUCT_OWNER_COMP").queryList();
    if (productRoleList) {
        personAndComCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition ("roleTypeId", EntityOperator.EQUALS, "PRODUCT_OWNER"),
            EntityCondition.makeCondition ("partyIdFrom", EntityOperator.EQUALS, productRoleList[0].partyId),
            EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
            EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
            ], EntityJoinOperator.AND);
        personAndCompanyList = from("ScrumRolesPersonAndCompany").where(personAndComCond).orderBy("groupName").queryList();
    }
}
if (personAndCompanyList) {
    context.companyCurrentList = personAndCompanyList;
    context.companyCurrent = personAndCompanyList[0].partyId;
    } else {
    context.companyCurrent = null;
}

// Get owner with security group
scrumRolesPersonAndCompanyList = [];
personAndComConds = EntityCondition.makeCondition([
    EntityCondition.makeCondition ("roleTypeId", EntityOperator.EQUALS, "PRODUCT_OWNER"),
    EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
    EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
    ], EntityJoinOperator.AND);
personAndCompanyList = from("ScrumRolesPersonAndCompany").where(personAndComConds).orderBy("groupName").queryList();
if (personAndCompanyList) {
    personAndCompanyList.each { personAndCompanyMap ->
        partyId = personAndCompanyMap.partyId;
        securityGroupCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, partyId),
            EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")
            ], EntityJoinOperator.AND);
        securityGroupList = from("ScrumMemberUserLoginAndSecurityGroup").where(securityGroupCond).queryList();
        if (securityGroupList) {
            scrumRolesPersonAndCompanyList.add(personAndCompanyMap);
            }
        }
    }
context.scrumRolesPersonAndCompanyList = scrumRolesPersonAndCompanyList;
