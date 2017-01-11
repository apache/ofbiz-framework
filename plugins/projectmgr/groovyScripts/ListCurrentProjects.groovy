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


import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityJoinOperator
import org.apache.ofbiz.entity.condition.EntityOperator

cond = EntityCondition.makeCondition([
        EntityCondition.makeCondition ("workEffortTypeId", EntityOperator.EQUALS, "PROJECT"),
        EntityCondition.makeCondition ("currentStatusId", EntityOperator.NOT_EQUAL, "PRJ_CLOSED")
        ], EntityJoinOperator.AND)
allProjects = select("workEffortId").from("WorkEffort").where(cond).orderBy("workEffortName").queryList()

projects = []
allProjects.each { project ->
    result = runService('getProject', ["userLogin" : parameters.userLogin, "projectId" : project.workEffortId])
    if (result.projectInfo) {
        resultAssign = from("WorkEffortPartyAssignment").where("partyId", parameters.userLogin.partyId, "workEffortId", project.workEffortId).queryList()
        if (security.hasEntityPermission("PROJECTMGR", "_ADMIN", session)
        || ((security.hasEntityPermission("PROJECTMGR", "_ROLE_ADMIN", session) || security.hasEntityPermission("PROJECTMGR", "_ROLE_VIEW", session)) && resultAssign)) {
            projects.add(result.projectInfo)
        }
    }
}
if (projects) {
    context.projects = projects
}
