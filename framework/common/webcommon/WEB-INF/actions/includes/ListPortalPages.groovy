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

import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.condition.*;
// only execute when a user is logged in
if (parameters.userLogin) {
    if (!parameters.parentPortalPageId) {
        portalPage = delegator.findByPrimaryKey("PortalPage", [portalPageId : parameters.portalPageId]);
        if (portalPage) {
            parameters.parentPortalPageId = portalPage.parentPortalPageId;
            if (!parameters.parentPortalPageId) {
                if (portalPage.originalPortalPageId) {
                    parameters.parentPortalPageId = portalPage.originalPortalPageId;
                } else {
                    parameters.parentPortalPageId = portalPage.portalPageId;
                }
            }
        }
    }
    userLoginSecurityGroupId = null;
    condSec = EntityCondition.makeCondition([
                  EntityCondition.makeCondition("groupId", EntityOperator.LIKE, parameters.parentPortalPageId + "%"),
                  EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, parameters.userLogin.userLoginId)
                  ],EntityOperator.AND);
    userLoginSecurityGroups = delegator.findList("UserLoginSecurityGroup", condSec, null, null, null, false);

    if (UtilValidate.isNotEmpty(userLoginSecurityGroups)) {
        userLoginSecurityGroupId = userLoginSecurityGroups.get(0).get("groupId");
    }

    //get the portal mainpage
    cond1 = EntityCondition.makeCondition([
            EntityCondition.makeCondition("portalPageId", EntityOperator.LIKE, parameters.parentPortalPageId + "%"),
            EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, userLoginSecurityGroupId),
            EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, "_NA_"),
            EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, null)
            ],EntityOperator.AND);
    portalMainPages = delegator.findList("PortalPage", cond1, null, null, null, false);
    parentPortalPageId = null;
    if (portalMainPages) {
        parentPortalPageId = portalMainPages.get(0).portalPageId;
    } else {
        parentPortalPageId = parameters.parentPortalPageId
    }
    // get user and system pages
    ppCond =
            EntityCondition.makeCondition([
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, parentPortalPageId),
                    EntityCondition.makeCondition("portalPageId", EntityOperator.EQUALS, parentPortalPageId),
                    EntityCondition.makeCondition("originalPortalPageId", EntityOperator.EQUALS, parentPortalPageId)
                ],EntityOperator.OR),
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, parameters.userLogin.userLoginId),
                    EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, "_NA_")
                ],EntityOperator.OR),
            ],EntityOperator.AND);
    portalPages = delegator.findList("PortalPage", ppCond, null, ["sequenceNum"], null, false);

    // remove overridden system pages
    portalPages.each { portalPage ->
        if (portalPage.ownerUserLoginId.equals("_NA_")) {
            userPortalPages = delegator.findByAnd("PortalPage", [originalPortalPageId : portalPage.portalPageId, ownerUserLoginId : parameters.userLogin.userLoginId]);
            if (userPortalPages) {
                portalPages.remove(portalPage);
            }
        }
    }

    // get sequenceNumMin and sequenceNumMax
    sequenceNumCond =
            EntityCondition.makeCondition([
                EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, parameters.userLogin.userLoginId),
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, parentPortalPageId),
                    EntityCondition.makeCondition("originalPortalPageId", EntityOperator.EQUALS, parentPortalPageId)
                ],EntityOperator.OR),
            ],EntityOperator.AND);
    sequenceNums = delegator.findList("PortalPage", sequenceNumCond, null, ["sequenceNum"], null, false);
    if (sequenceNums) {
       context.parameters.sequenceNumMin = sequenceNums.get(0).sequenceNum;
       context.parameters.sequenceNumMax = sequenceNums.get(sequenceNums.size()-1).sequenceNum;
    }
    else{
       context.parameters.sequenceNumMin = "null";
       context.parameters.sequenceNumMax = "null";
    }

    context.portalPages = portalPages;
    context.userLoginSecurityGroupId = userLoginSecurityGroupId;
    parameters.portalPagesSize = portalPages.get(portalPages.size()-1).sequenceNum;
}

