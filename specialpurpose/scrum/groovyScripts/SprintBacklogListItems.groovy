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

taskStatusId = null
reopenedStatusId = null
backlogStatusId = parameters.backlogStatusId
paraBacklogStatusId = backlogStatusId
currentStatus = sprintStatus.currentStatusId
projectSprintList = []

if ("SPRINT_CLOSED".equals(currentStatus)) {
    backlogStatusId = null
} else {
    if (backlogStatusId == "Any") {
        backlogStatusId = null
    } else {
        backlogStatusId = "CRQ_REVIEWED"
        reopenedStatusId = "CRQ_REOPENED"
        taskStatusId = "STS_CREATED"
    }
}
orCurentExprs = []
    if (taskStatusId) {
        orCurentExprs.add(EntityCondition.makeCondition("taskCurrentStatusId", EntityOperator.EQUALS, taskStatusId))
        orCurentExprs.add(EntityCondition.makeCondition("taskCurrentStatusId", EntityOperator.EQUALS, "SPRINT_ACTIVE"))
    }
orBacklogExprs = []
    if (backlogStatusId) {
        orBacklogExprs.add(EntityCondition.makeCondition("backlogStatusId", EntityOperator.EQUALS, backlogStatusId))
    }
    if (reopenedStatusId) {
        orBacklogExprs.add(EntityCondition.makeCondition("backlogStatusId", EntityOperator.EQUALS, reopenedStatusId))
    }
andExprs =  []
    if (parameters.projectId) {
        andExprs.add(EntityCondition.makeCondition("projectId", EntityOperator.EQUALS, parameters.projectId))
    } else {
        andExprs.add(EntityCondition.makeCondition("projectId", EntityOperator.EQUALS, sprintStatus.workEffortParentId))
    }
    if (orBacklogExprs) {
        andExprs.add(EntityCondition.makeCondition(orBacklogExprs, EntityOperator.OR))
    }
    if (orCurentExprs) {
        andExprs.add(EntityCondition.makeCondition(orCurentExprs, EntityOperator.OR))
    }
    andExprs.add(EntityCondition.makeCondition("sprintId", EntityOperator.EQUALS, parameters.sprintId))
    andExprs.add(EntityCondition.makeCondition("sprintTypeId", EntityOperator.EQUALS, "SCRUM_SPRINT"))
    
projectSprintCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND)
projectSprintList = from("ProjectSprintBacklogAndTask").where(andExprs).orderBy("custSequenceNum","custRequestId","taskTypeId").queryList()

context.listIt = projectSprintList
context.paraBacklogStatusId = paraBacklogStatusId

//get backlog and task information
if (parameters.sprintId) {
    //get total backlog size
    completedBacklog = 0
    reviewedBacklog = 0
    totalbacklog = 0
    allTask = []
    sprintList = from("CustRequestWorkEffort").where("workEffortId", parameters.sprintId).queryList()
    sprintList.each { sprintMap ->
        custMap = sprintMap.getRelatedOne("CustRequest", false)
        //if ("RF_PROD_BACKLOG".equals(custMap.custRequestTypeId)) {
            totalbacklog += 1
            if ("CRQ_REVIEWED".equals(custMap.statusId)){
                reviewedBacklog += 1
            } else {
                completedBacklog += 1
            }
            //get task
            workEffortList = custMap.getRelated("CustRequestWorkEffort", null, null, false)
            if (workEffortList) {
                allTask.addAll(workEffortList)
            }
        //}
    }
    //get total task size
    completedTask = 0
    createdTask = 0
    totalTask = 0
    if (allTask) {
        allTask.each { taskMap ->
            workEffMap = taskMap.getRelatedOne("WorkEffort", false)
            if (!"SCRUM_SPRINT".equals(workEffMap.workEffortTypeId)) {
                totalTask += 1
                if ("STS_CREATED".equals(workEffMap.currentStatusId)){
                    createdTask += 1
                } else {
                    completedTask += 1
                }
            }
        }
    }
    context.completedBacklog = completedBacklog
    context.reviewedBacklog = reviewedBacklog
    context.totalbacklog = totalbacklog
    context.completedTask = completedTask
    context.createdTask = createdTask
    context.totalTask = totalTask
}
