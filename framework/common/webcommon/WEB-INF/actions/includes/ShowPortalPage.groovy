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

portalPage = null;
portalPageId = parameters.portalPageId;
if(!portalPageId){
    portalPageId = parameters.parentPortalPageId;
}

if (portalPageId) {
    portalPage = delegator.findByPrimaryKey("PortalPage", [portalPageId : portalPageId]);
}

if (!portalPage) {
    // find the user private main page
    condPrivat = EntityCondition.makeCondition([
                 EntityCondition.makeCondition("originalPortalPageId", EntityOperator.LIKE, portalPageId + "%"),
                 EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, parameters.userLogin.userLoginId)
                 ],EntityOperator.AND);
    privatPortalPages = delegator.findList("PortalPage", condPrivat, null, null, null, false);

    if (privatPortalPages) {
        portalPage = privatPortalPages.get(0);
    } else {
        // look for system page according the current securitygroup
        //get the security group
        userLoginSecurityGroupId = null;
        condSec = EntityCondition.makeCondition([
                  EntityCondition.makeCondition("groupId", EntityOperator.LIKE, portalPageId + "%"),
                  EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, parameters.userLogin.userLoginId)
                  ],EntityOperator.AND);
        userLoginSecurityGroups = delegator.findList("UserLoginSecurityGroup", condSec, null, null, null, false);
        if (UtilValidate.isNotEmpty(userLoginSecurityGroups)) {
            userLoginSecurityGroupId = userLoginSecurityGroups.get(0).get("groupId");
        }
        //get the portal page
        cond1 = EntityCondition.makeCondition([
                EntityCondition.makeCondition("portalPageId", EntityOperator.LIKE, portalPageId + "%"),
                EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, userLoginSecurityGroupId),
                EntityCondition.makeCondition("ownerUserLoginId", EntityOperator.EQUALS, "_NA_"),
                EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, null)
                ],EntityOperator.AND);
        portalMainPages = delegator.findList("PortalPage", cond1, null, null, null, false);
        if (portalMainPages) {
            portalPage = portalMainPages.get(0);
        }
    }
}

if (portalPage) {
    parameters.portalPageId = portalPage.portalPageId;
    context.portalPage = portalPage;
    context.portalPageColumns = portalPage.getRelated("PortalPageColumn");
    context.portalPagePortlets = delegator.findByAnd("PortalPagePortletView", [portalPageId : portalPage.portalPageId]);
} 