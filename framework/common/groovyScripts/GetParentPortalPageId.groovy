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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.entity.util.EntityUtil

// executes only on startup when only the basic parameters.portalPageId (from commonscreens.xml) is available
if (userLogin && parameters.parentPortalPageId && !parameters.portalPageId) {
    // look for system page according the current securitygroup
    //get the security group
    condSec = EntityCondition.makeCondition([
            EntityCondition.makeCondition("portalPageId", EntityOperator.LIKE, parameters.parentPortalPageId + "%"),
            EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, null),
            EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLogin.userLoginId)
            ],EntityOperator.AND)
    portalMainPages = EntityUtil.filterByDate(delegator.findList("PortalPageAndUserLogin", condSec, null, null, null, false))
    if (!portalMainPages) { // look for a null securityGroup if not found
        condSec = EntityCondition.makeCondition([
            EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, null),
            EntityCondition.makeCondition("parentPortalPageId", EntityOperator.EQUALS, null),
            EntityCondition.makeCondition("portalPageId", EntityOperator.LIKE, parameters.parentPortalPageId + "%")
            ],EntityOperator.AND)
        portalMainPages = delegator.findList("PortalPage", condSec, null, null, null, false)
    }
    if (portalMainPages) {
        portalPageId = portalMainPages.get(0).portalPageId
        // check if overridden with a privat page
        privatMainPages = from("PortalPage").where("originalPortalPageId", portalPageId, "ownerUserLoginId", userLogin.userLoginId).queryList();
        if (privatMainPages) {
            context.parameters.portalPageId = privatMainPages.get(0).portalPageId
        } else {
            context.parameters.portalPageId = portalPageId
        }
    }
}
// Debug.log('======portalPageId: ' + parameters.portalPageId)
if (userLogin && parameters.portalPageId) {
    portalPage = from("PortalPage").where("portalPageId", parameters.portalPageId).queryOne();
    if (portalPage) {
        if (portalPage.parentPortalPageId) {
            context.parameters.parentPortalPageId = portalPage.parentPortalPageId
        } else {
            if ("_NA_".equals(portalPage.ownerUserLoginId)) {
                context.parameters.parentPortalPageId = portalPage.portalPageId
            } else {
                context.parameters.parentPortalPageId = portalPage.originalPortalPageId
            }
        }
    }
}
// Debug.log('======parent portalPageId: ' + parameters.parentPortalPageId)
if (!context.headerItem && parameters.portalPageId) {
    context.headerItem = parameters.portalPageId // and the menu item is highlighted
}
