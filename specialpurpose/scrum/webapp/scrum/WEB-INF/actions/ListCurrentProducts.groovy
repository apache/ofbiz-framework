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

roleTypeId = null;
paramCond = [];
products = [];
productId = parameters.productId;
internalName = parameters.internalName;
statusId = parameters.statusId;
if ("Any".equals(statusId)) {
    statusId = null;
}
partyId = parameters.partyId;

if(!security.hasEntityPermission("SCRUM", "_ADMIN", session)){
    if(security.hasEntityPermission("SCRUM_PRODUCT", "_ADMIN", session)){
        roleTypeId = "PRODUCT_OWNER";
    }
}

if (userLogin) {
    if(UtilValidate.isNotEmpty(partyId)){
        paramCond.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
    }
    if(UtilValidate.isNotEmpty(productId)){
        paramCond.add(EntityCondition.makeCondition("productId", EntityOperator.LIKE, productId + "%"));
    }
    if(UtilValidate.isNotEmpty(internalName)){
        paramCond.add(EntityCondition.makeCondition("internalName", EntityOperator.LIKE, "%" + internalName + "%"));
    }
    if(UtilValidate.isNotEmpty(statusId)){
        paramCond.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
    }
    
    paramCond.add(EntityCondition.makeCondition("productTypeId", EntityOperator.EQUALS, "SCRUM_ITEM"));
    paramCond.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "PRODUCT_OWNER_COMP"));
    paramCond.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
    
    cond = EntityCondition.makeCondition(paramCond, EntityOperator.AND);
    
    allProducts = delegator.findList("ProductAndRole", cond, null, ["groupName", "internalName"], null, false);
    
    securityGroupCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, userLogin.partyId),
        EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
        EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
        ], EntityJoinOperator.AND);
    fields = new HashSet(["partyId", "groupId"]);
    partyAndSecurityGroupList = delegator.findList("ScrumMemberUserLoginAndSecurityGroup", securityGroupCond, fields, ["partyId"], null, false);
    context.partyAndSecurityGroupList = partyAndSecurityGroupList;
    boolean addAllProducts = false;
    allProducts.each { product ->
        product = product.getAllFields();
        productMap = delegator.findByPrimaryKey("Product", ["productId" : product.productId]);    
        product.put("longDescription",productMap.longDescription)
        if(security.hasEntityPermission("SCRUM", "_ADMIN", session)){
            addAllProducts = true;
        }else{
            ismember = false;
            if (partyAndSecurityGroupList) {
                groupId = partyAndSecurityGroupList[0].groupId;
                if ("SCRUM_PRODUCT_OWNER".equals(groupId)) {
                    productAndRoleList = delegator.findByAnd("ProductRole", ["productId" : product.productId, "partyId" : userLogin.partyId, "thruDate" : null]);
                    if (productAndRoleList) {
                        productAndRoleList.each { productAndRoleMap ->
                            productIdInner = productAndRoleMap.productId;
                                if (productIdInner.equals(product.productId)) {
                                    ismember = true;
                                }
                            }
                        }
                } else if ("SCRUM_STAKEHOLDER".equals(groupId)) {
                    // check in company relationship.
                    scrumRolesCond = EntityCondition.makeCondition([
                        EntityCondition.makeCondition ("partyId", EntityOperator.EQUALS, userLogin.partyId),
                        EntityCondition.makeCondition ("roleTypeId", EntityOperator.EQUALS, "STAKEHOLDER"),
                        EntityCondition.makeCondition ("partyStatusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED"),
                        EntityCondition.makeCondition ("thruDate", EntityOperator.EQUALS, null)
                        ], EntityJoinOperator.AND);
                    scrumRolesPersonAndCompanyList = delegator.findList("ScrumRolesPersonAndCompany", scrumRolesCond, null, null, null, false);
                    productRoleList = delegator.findByAnd("ProductRole", ["partyId" : scrumRolesPersonAndCompanyList[0].partyIdFrom, "roleTypeId" : "PRODUCT_OWNER_COMP", "thruDate" : null]);
                    if (productRoleList) {
                        productRoleList.each { productRoleMap ->
                            stakeholderProduct = productRoleMap.productId;
                            if (stakeholderProduct.equals(product.productId)) {
                                ismember = true;
                            }
                        }
                   }
                   //check in product.
                    if (ismember == false) {
                        productAndRoleList = delegator.findByAnd("ProductAndRole", ["productId" : product.productId, "partyId" : userLogin.partyId
                            , "roleTypeId" : "STAKEHOLDER", "statusId" : "PRODUCT_ACTIVE", "thruDate" : null]);
                        if (productAndRoleList) {
                            ismember = true;
                        }
                    }
                } else if ("SCRUM_MASTER".equals(groupId)) {
                    //check in product.
                    productRoleList = [];
                    productRoleList = delegator.findByAnd("ProductAndRole", ["productId" : product.productId, "partyId" : userLogin.partyId
                        , "roleTypeId" : "SCRUM_MASTER", "statusId" : "PRODUCT_ACTIVE", "thruDate" : null]);
                    if (productRoleList) {
                        ismember = true;
                    }
                    //check in project.
                    if (ismember == false) {
                        projects = [];
                        projects = delegator.findByAnd("WorkEffortAndProduct", ["productId" : product.productId, "workEffortTypeId" : "SCRUM_PROJECT", "currentStatusId" : "SPJ_ACTIVE"]);
                        if (projects) {
                            projects.each { project ->
                                projectPartyAssignment = delegator.findByAnd("WorkEffortPartyAssignment", ["partyId" : userLogin.partyId, "workEffortId" : project.workEffortId]);
                                if (projectPartyAssignment) {
                                    ismember = true;
                                }
                            }
                        }
                    }
                    //check in sprint.
                    if (ismember == false) {
                        projects.each { project ->
                            allSprintList = [];
                            allSprintList = delegator.findByAnd("WorkEffort", ["workEffortParentId" : project.workEffortId, "currentStatusId" : "SPRINT_ACTIVE"]);
                            allSprintList.each { SprintListMap ->
                                sprintId = SprintListMap.workEffortId;
                                workEffortPartyAssignment = delegator.findByAnd("WorkEffortPartyAssignment", ["partyId" : userLogin.partyId, "workEffortId" : sprintId])
                                if (workEffortPartyAssignment) {
                                    ismember = true;
                                }
                            }
                        }
                    }
                } else {
                    projects = [];
                    projects = delegator.findByAnd("WorkEffortAndProduct", ["productId" : product.productId, "workEffortTypeId" : "SCRUM_PROJECT", "currentStatusId" : "SPJ_ACTIVE"]);
                    if (projects) {
                        projects.each { project ->
                            allSprintList = [];
                            allSprintList = delegator.findByAnd("WorkEffort", ["workEffortParentId" : project.workEffortId, "currentStatusId" : "SPRINT_ACTIVE"]);
                            allSprintList.each { SprintListMap ->
                                sprintId = SprintListMap.workEffortId;
                                workEffortPartyAssignment = delegator.findByAnd("WorkEffortPartyAssignment", ["partyId" : userLogin.partyId, "workEffortId" : sprintId])
                                if (workEffortPartyAssignment) {
                                    ismember = true;
                                }
                            }
                        }
                    }
                    if (ismember == false) {
                        exprBldr = [EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REVIEWED"),
                                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REOPENED")];
                        andExprs = [EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.productId),
                                    EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "STS_CREATED"),
                                    EntityCondition.makeCondition(exprBldr, EntityOperator.OR)];
                        unplannedBacklogCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
                        unplannedBacklogList = delegator.findList("UnPlannedBacklogsAndTasks", unplannedBacklogCond, null,null ,null, false);
                        if (unplannedBacklogList) {
                            unplannedBacklogList.each { unplannedMap ->
                                workEffortId = unplannedMap.workEffortId;
                                workEffortPartyAssignment = delegator.findByAnd("WorkEffortPartyAssignment", ["partyId" : userLogin.partyId, "workEffortId" : workEffortId])
                                if (workEffortPartyAssignment) {
                                    ismember = true;
                                }
                            }
                        }
                    }
                }
                
                if (ismember) {
                      products.add(product);
                }
              }
            }
            if(addAllProducts)
                products.add(product);
    }
} else {
    Debug.logError("Party ID missing =========>>> : null ", "");
}

if (products){
    context.listIt = products;
}