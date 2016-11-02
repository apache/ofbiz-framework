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
import org.apache.ofbiz.entity.condition.*

ppCond = EntityCondition.makeCondition("portletCategoryId", EntityOperator.EQUALS, parameters.portletCategoryId)
categories = delegator.findList("PortletPortletCategory", ppCond, null, null, null, false)

portalPortlets = []
    categories.each { category ->
    pCond = EntityCondition.makeCondition("portalPortletId", EntityOperator.EQUALS, category.get("portalPortletId"))
    listPortalPortlets = delegator.findList("PortalPortlet", pCond, null, null, null, false)

    inMap = [:]
    listPortalPortlets.each { listPortalPortlet ->
        if (listPortalPortlet.securityServiceName && listPortalPortlet.securityMainAction) {
            inMap.mainAction = listPortalPortlet.securityMainAction
            inMap.userLogin = context.userLogin
            result = runService(listPortalPortlet.securityServiceName, inMap)
            hasPermission = result.hasPermission
        } else {
            hasPermission = true
        }

        if (hasPermission) {
            portalPortlets.add(listPortalPortlet)
        }
    }
}

context.portletCat = delegator.findList("PortletCategory", null, null, null, null, false)
context.portalPortlets = portalPortlets