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

import java.util.*
import org.apache.ofbiz.base.util.Debug

partyId = parameters.partyId
// get existing task that no assign
projectSprintBacklogAndTaskList = []
projectAndTaskList = from("ProjectSprintBacklogAndTask").where("sprintTypeId", "SCRUM_SPRINT","taskCurrentStatusId", "STS_CREATED").orderBy("taskId DESC").queryList()
projectAndTaskList.each { projectAndTaskMap ->
userLoginId = userLogin.partyId
    projectId = projectAndTaskMap.projectId
    partyAssignmentProjectList = from("WorkEffortPartyAssignment").where("workEffortId", projectId, "partyId", partyId).queryList()
    partyAssignmentProjectMap = partyAssignmentProjectList[0]
        // if this userLoginId is a member of project
        if (partyAssignmentProjectMap) {
            sprintId = projectAndTaskMap.sprintId
            partyAssignmentSprintList = from("WorkEffortPartyAssignment").where("workEffortId", sprintId, "partyId", partyId).queryList()
            partyAssignmentSprintMap = partyAssignmentSprintList[0]
            // if this userLoginId is a member of sprint
            if (partyAssignmentSprintMap) {
                workEffortId = projectAndTaskMap.taskId
                partyAssignmentTaskList = from("WorkEffortPartyAssignment").where("workEffortId", workEffortId).queryList()
                partyAssignmentTaskMap = partyAssignmentTaskList[0]
                // if the task do not assigned
                if (!partyAssignmentTaskMap) {
                    projectSprintBacklogAndTaskList.add(projectAndTaskMap)
                    // if the task do not assigned and assigned with custRequestTypeId = RF_SCRUM_MEETINGS
                    } else {
                        custRequestTypeId = projectAndTaskMap.custRequestTypeId
                        backlogStatusId = projectAndTaskMap.backlogStatusId
                        if (custRequestTypeId.equals("RF_SCRUM_MEETINGS") && backlogStatusId.equals("CRQ_REVIEWED")) {
                            projectSprintBacklogAndTaskList.add(projectAndTaskMap)
                           }
                     }
                }
            }
    }
context.projectSprintBacklogAndTaskList = projectSprintBacklogAndTaskList
