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

cond = EntityCondition.makeCondition([
        EntityCondition.makeCondition ("projectTypeId", EntityOperator.EQUALS, "SCRUM_PROJECT"),
        EntityCondition.makeCondition ("projectStatusId", EntityOperator.NOT_EQUAL, "SPJ_CLOSED")
        ], EntityJoinOperator.AND);
securityGroupCond = EntityCondition.makeCondition([
    EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, userLogin.partyId),
    EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
    EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
    ], EntityJoinOperator.AND);
fields = new HashSet(["partyId", "groupId"]);

allSprints = from("ProjectSprint").where(cond).orderBy("projectName", "-sprintActualStartDate").queryList();
partyAndSecurityGroupList = select("partyId", "groupId").from("ScrumMemberUserLoginAndSecurityGroup").where(securityGroupCond).orderBy("partyId").queryList();
oldProjectId = null;
newProjectId = null;
countSprint = 0;
sprints = [];
allSprints.each { sprint ->
    newProjectId = sprint.projectId;
    productAndRole = from("ProductAndRole").where("roleTypeId", "PRODUCT_OWNER_COMP", "productId", sprint.productId).queryList();
    companyId = "";
    companyName = "";
    if (productAndRole.size() > 0) {
    	companyName = productAndRole.get(0).groupName;
    	companyId = productAndRole.get(0).partyId;
    }
    sprint = sprint.getAllFields();
    sprint.put("companyId", companyId)
    sprint.put("companyName", companyName)
    product = from("Product").where("productId", sprint.productId).queryOne();
    productName = "";
    if (product != null) productName = product.internalName;
    sprint.put("productName", productName);
    //sprint.add("companyName", companyName, "String")
    if (oldProjectId != newProjectId) {
    	oldProjectId = newProjectId;
    	countSprint = 0;
        ismember = false;
       if (partyAndSecurityGroupList) {
           groupId = partyAndSecurityGroupList[0].groupId;
           if ("SCRUM_PRODUCT_OWNER".equals(groupId)) {
               productAndRoleList = from("ProductRole").where("productId", sprint.productId, "partyId", partyAndSecurityGroupList.getAt(0).partyId, "thruDate", null).queryList();
               if (productAndRoleList) {
                   ismember = true;
                   }
           }else if("SCRUM_STAKEHOLDER".equals(groupId)) {
               // check in company relationship.
               scrumRolesCond = EntityCondition.makeCondition([
                   EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, userLogin.partyId),
                   EntityCondition.makeCondition ("roleTypeId", EntityOperator.EQUALS, "STAKEHOLDER"),
                   EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
                   EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
                   ], EntityJoinOperator.AND);
               scrumRolesPersonAndCompanyList = from("ScrumRolesPersonAndCompany").where(scrumRolesCond).queryList();
               productRoleList = from("ProductRole").where("partyId", scrumRolesPersonAndCompanyList[0].partyIdFrom, "roleTypeId", "PRODUCT_OWNER_COMP", "thruDate", null).queryList();
               if (productRoleList) {
                   productRoleList.each { productRoleMap ->
                       stakeholderProduct = productRoleMap.productId;
                       if (stakeholderProduct.equals(sprint.productId)) {
                           ismember = true;
                       }
                   }
               }
               //check in product.
               if (ismember == false) {
                   productAndRoleList = from("ProductAndRole")
                                           .where("productId", sprint.productId, "partyId", userLogin.partyId, "roleTypeId", "STAKEHOLDER", 
                                               "supportDiscontinuationDate", null, "thruDate", null)
                                           .queryList();
                   if (productAndRoleList) {
                       ismember = true;
                   }
               }
           } else if("SCRUM_MASTER".equals(groupId)) {
               //check in product
               productRoleList = [];
               productRoleList = from("ProductAndRole")
                   .where("productId" : sprint.productId, "partyId" : userLogin.partyId, 
                       "roleTypeId" : "SCRUM_MASTER", "supportDiscontinuationDate" : null, "thruDate" : null)
                   .queryList();
               
               if (productRoleList) {
                   ismember = true;
               }
               //check in project.
               if (ismember == false) {
                   projectPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprint.projectId, "partyId", userLogin.partyId).queryList();
                   if (projectPartyAssignment) {
                       ismember = true;
                   }
               }
               //check in sprint.
               if (ismember == false) {
                   allSprintList = [];
                   allSprintList = from("WorkEffort").where("workEffortParentId", sprint.projectId).queryList();
                   allSprintList.each { SprintListMap ->
                       sprintId = SprintListMap.workEffortId;
                       workEffortPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprintId, "partyId", userLogin.partyId).queryList();
                       if (workEffortPartyAssignment) {
                           ismember = true;
                       }
                   }
               }
           } else {
               allSprintList = [];
               allSprintList = from("WorkEffort").where("workEffortParentId", sprint.projectId).queryList();
               allSprintList.each { SprintListMap ->
                   sprintId = SprintListMap.workEffortId;
                   workEffortPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprintId, "partyId", userLogin.partyId).queryList();
                   if (workEffortPartyAssignment) {
                       ismember = true;
                   }
               }
           }
       }
        if (security.hasEntityPermission("SCRUM", "_ADMIN", session) 
        || ((security.hasEntityPermission("SCRUM", "_ROLE_ADMIN", session) || security.hasEntityPermission("SCRUM", "_ROLE_VIEW", session) 
        || security.hasEntityPermission("SCRUM_PROJECT", "_ROLE_ADMIN", session)  || security.hasEntityPermission("SCRUM_PROJECT", "_ROLE_VIEW", session)
        || security.hasEntityPermission("SCRUM_PROJECT", "_VIEW", session)) && ismember)) {
               sprints.add(sprint);
               countSprint++;
        }
    } else { 
    	if (countSprint < 4) {
           ismember = false;
           if (partyAndSecurityGroupList) {
               groupId = partyAndSecurityGroupList[0].groupId;
               if ("SCRUM_PRODUCT_OWNER".equals(groupId)) {
                   productAndRoleList = from("ProductRole").where("productId", sprint.productId, "partyId", partyAndSecurityGroupList.getAt(0).partyId, "thruDate", null).queryList();
                   if (productAndRoleList) {
                       ismember = true;
                   }
               }else if("SCRUM_STAKEHOLDER".equals(groupId)) {
                       // check in company relationship.
                       scrumRolesCond = EntityCondition.makeCondition([
                           EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, userLogin.partyId),
                           EntityCondition.makeCondition ("roleTypeId", EntityOperator.EQUALS, "STAKEHOLDER"),
                           EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
                           EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
                           ], EntityJoinOperator.AND);
                       scrumRolesPersonAndCompanyList = from("ScrumRolesPersonAndCompany").where(scrumRolesCond).queryList();
                       productRoleList = from("ProductRole").where("partyId", scrumRolesPersonAndCompanyList[0].partyIdFrom, "roleTypeId", "PRODUCT_OWNER_COMP", "thruDate", null).queryList();
                       if (productRoleList) {
                           productRoleList.each { productRoleMap ->
                               stakeholderProduct = productRoleMap.productId;
                               if (stakeholderProduct.equals(sprint.productId)) {
                                   ismember = true;
                               }
                           }
                      }
                       //check in product.
                       if (ismember == false) {
                           productAndRoleList = from("ProductAndRole").where("productId" : sprint.productId, "partyId" : userLogin.partyId, "roleTypeId" : "STAKEHOLDER", "supportDiscontinuationDate" : null, "thruDate" : null).queryList();
                           if (productAndRoleList) {
                               ismember = true;
                           }
                       }
               } else if("SCRUM_MASTER".equals(groupId)) {
                       //check in product
                       productRoleList = [];
                       productRoleList = from("ProductAndRole").where("productId" : sprint.productId, "partyId" : userLogin.partyId, "roleTypeId" : "SCRUM_MASTER", "supportDiscontinuationDate" : null, "thruDate" : null).queryList();
                       if (productRoleList) {
                           ismember = true;
                       }
                       //check in project.
                       if (ismember == false) {
                           projectPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprint.projectId, "partyId", userLogin.partyId).queryList();
                           if (projectPartyAssignment) {
                               ismember = true;
                           }
                       }
                       //check in sprint.
                       if (ismember == false) {
                           allSprintList = [];
                           allSprintList = from("WorkEffort").where("workEffortParentId", sprint.projectId).queryList();
                           allSprintList.each { SprintListMap ->
                               sprintId = SprintListMap.workEffortId;
                               workEffortPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprintId, "partyId", userLogin.partyId).queryList();
                               if (workEffortPartyAssignment) {
                                   ismember = true;
                               }
                           }
                       }
               } else {
                   allSprintList = [];
                   allSprintList = from("WorkEffort").where("workEffortParentId", sprint.projectId).queryList();
                   allSprintList.each { SprintListMap ->
                       sprintId = SprintListMap.workEffortId;
                       workEffortPartyAssignment = from("WorkEffortPartyAssignment").where("workEffortId", sprintId, "partyId", userLogin.partyId).queryList();
                       if (workEffortPartyAssignment) {
                           ismember = true;
                       }
                   }
               }
           }
            if (security.hasEntityPermission("SCRUM", "_ADMIN", session) 
            || ((security.hasEntityPermission("SCRUM", "_ROLE_ADMIN", session) || security.hasEntityPermission("SCRUM", "_ROLE_VIEW", session) 
            || security.hasEntityPermission("SCRUM_PROJECT", "_ROLE_ADMIN", session)  || security.hasEntityPermission("SCRUM_PROJECT", "_ROLE_VIEW", session)
            || security.hasEntityPermission("SCRUM_PROJECT", "_VIEW", session)) && ismember)) {
            	sprints.add(sprint);
            	countSprint++;
            }
        }
    }
}
if (sprints) {
	sprints = UtilMisc.sortMaps(sprints, ["companyName", "projectName", "productName"])
    context.sprints = sprints;
}
