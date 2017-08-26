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

import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.content.ContentManagementWorker
import org.apache.ofbiz.entity.model.ModelUtil

paramMap = UtilHttp.getParameterMap(request)
forumId = ContentManagementWorker.getFromSomewhere("permRoleSiteId", paramMap, request, context)
blogRoles = from("RoleType").where("parentTypeId", "BLOG").cache(true).queryList()

if (forumId) {
    siteRoleMap = [:]
    for (int i=0; i < blogRoles.size(); i++) {
        roleType = blogRoles.get(i)
        roleTypeId = roleType.roleTypeId
        filteredRoleList = from("ContentRole").where("contentId", forumId, "roleTypeId", roleTypeId).filterByDate().queryList()
        cappedBlogRoleName = ModelUtil.dbNameToVarName(roleTypeId)

        filteredRoleList.each { contentRole ->
            partyId = contentRole.partyId
            fromDate = contentRole.fromDate
            map = siteRoleMap.get(partyId)
            if (!map) {
                map = [:]
                map.partyId = partyId
                siteRoleMap.put(partyId, map)
            }
            map.put(cappedBlogRoleName, "Y")
            map.put(cappedBlogRoleName + "FromDate", fromDate)
        }
    }
    siteList = new ArrayList(siteRoleMap.values())
    context.siteList = siteList
    context.rowCount = siteList.size()
    blogRoleList = [] as ArrayList
    blogRoles.each { roleType ->
        blogRoleList.add(roleType.roleTypeId)
    }
    context.blogRoleIdList = blogRoleList
}
