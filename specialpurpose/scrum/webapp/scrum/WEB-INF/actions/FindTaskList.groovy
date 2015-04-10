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

import java.util.*;
import java.lang.*;
import org.apache.tools.ant.taskdefs.Parallel.TaskList;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;
import java.sql.Timestamp;

taskId = parameters.taskId;;
taskName = parameters.taskName;
sprintId = parameters.sprintId;
sprintName = parameters.sprintName;
taskTypeId = parameters.taskTypeId;
projectId = parameters.projectId;
projectName = parameters.projectName;
backlogTypeId = parameters.unplannedFlag;
statusId = parameters.statusId;
partyId = parameters.partyId;

unplannedTaskList = [];
plannedTaskList = [];
resultList=[];
taskList=[];
implementTaskList=[];
testTaskList=[];
errorTaskList = [];
installTaskList = [];

// get Unplaned task list
if ((taskId != null)||(taskName != null)||(taskTypeId != null)||(sprintId != null)||(sprintName != null)
    ||(projectId != null)||(projectName != null)||(backlogTypeId != null)||(statusId != null)
    ||(partyId != null)||(viewIndex_1 > 0)||(viewIndex_2 > 0)||(viewIndex_3 > 0)||(viewIndex_4 > 0)
    ||(viewIndexNo_1 > 0)||(viewIndexNo_2 > 0)||(viewIndexNo_3 > 0)||(viewIndexNo_4 > 0)){
    if ((taskId != null)||(taskName != null)||(taskTypeId != null)){
        exprBldr =  [];
        if (taskId) {
            exprBldr.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, taskId));
        }
        if (taskName){
            exprBldr.add(EntityCondition.makeCondition("workEffortName", EntityOperator.LIKE, "%"+taskName+"%"));
        }
        if (taskTypeId){
            exprBldr.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, taskTypeId));
        }
        if (statusId){
            exprBldr.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, statusId));
        }
        unplannedTaskList = from("UnPlannedBacklogsAndTasks").where(exprBldr).orderBy("-createdDate").queryList();
    }
    else{
        unplannedTaskList = from("UnPlannedBacklogsAndTasks").orderBy("-createdDate").queryList();
    }
    
    exprBldr2 =  [];
    if (taskId) {
        exprBldr2.add(EntityCondition.makeCondition("taskId", EntityOperator.EQUALS, taskId));
    }
    if (taskName){
        exprBldr2.add(EntityCondition.makeCondition("taskName", EntityOperator.LIKE, "%"+taskName+"%"));
    }
    if (taskTypeId){
        exprBldr2.add(EntityCondition.makeCondition("taskTypeId", EntityOperator.EQUALS, taskTypeId));
    }
    if (statusId){
        exprBldr2.add(EntityCondition.makeCondition("taskCurrentStatusId", EntityOperator.EQUALS, statusId));
    }
    if (sprintId){
        exprBldr2.add(EntityCondition.makeCondition("sprintId", EntityOperator.EQUALS, sprintId));
    }
    if (sprintName){
        exprBldr2.add(EntityCondition.makeCondition("sprintName", EntityOperator.LIKE, "%"+sprintName+"%"));
    }
    if (projectId){
        exprBldr2.add(EntityCondition.makeCondition("projectId", EntityOperator.EQUALS, projectId));
    }
    if (projectName){
        exprBldr2.add(EntityCondition.makeCondition("projectName", EntityOperator.LIKE, "%"+projectName+"%"));
    }
    exprBldr2.add(EntityCondition.makeCondition("sprintTypeId", EntityOperator.EQUALS, "SCRUM_SPRINT"));
    plannedTaskList = from("ProjectSprintBacklogAndTask").where(exprBldr2).orderBy("-taskCreatedDate").queryList();
    
    unplannedTaskList.each{ unplannedTaskMap ->
        unplannedMap = [:];
        unplannedMap.taskId = unplannedTaskMap.workEffortId;
        unplannedMap.sprintId = null;
        unplannedMap.projectId = null;
        unplannedMap.productId = unplannedTaskMap.productId;
        unplannedMap.taskName = unplannedTaskMap.workEffortName;
        unplannedMap.taskTypeId = unplannedTaskMap.workEffortTypeId;
        unplannedMap.taskCurrentStatusId = unplannedTaskMap.currentStatusId;
        unplannedMap.taskEstimatedMilliSeconds = unplannedTaskMap.estimatedMilliSeconds;
        unplannedMap.taskCreatedDate = unplannedTaskMap.createdDate;
        unplannedMap.custRequestId = unplannedTaskMap.custRequestId;
        unplannedMap.description = unplannedTaskMap.description;
        unplannedMap.custRequestTypeId = unplannedTaskMap.custRequestTypeId;
        unplannedMap.taskActualMilliSeconds = unplannedTaskMap.actualMilliSeconds;
        unplannedMap.taskEstimatedStartDate = unplannedTaskMap.estimatedStartDate;
        taskList.add(unplannedMap);
    }
    
    plannedTaskList.each{ plannedTaskMap ->
        plannedMap = [:];
        plannedMap.taskId = plannedTaskMap.taskId;
        plannedMap.taskName = plannedTaskMap.taskName;
        plannedMap.taskTypeId = plannedTaskMap.taskTypeId;
        plannedMap.taskCurrentStatusId = plannedTaskMap.taskCurrentStatusId;
        plannedMap.taskEstimatedMilliSeconds = plannedTaskMap.taskEstimatedMilliSeconds;
        plannedMap.taskCreatedDate = plannedTaskMap.taskCreatedDate;
        plannedMap.sprintId = plannedTaskMap.sprintId;
        plannedMap.sprintName = plannedTaskMap.sprintName;
        plannedMap.projectId = plannedTaskMap.projectId;
        plannedMap.projectName = plannedTaskMap.projectName;
        plannedMap.custRequestId = plannedTaskMap.custRequestId;
        plannedMap.description = plannedTaskMap.description;
        plannedMap.custRequestTypeId = plannedTaskMap.custRequestTypeId;
        plannedMap.taskActualMilliSeconds = plannedTaskMap.taskActualMilliSeconds;
        plannedMap.taskEstimatedStartDate = plannedTaskMap.taskEstimatedStartDate;
        taskList.add(plannedMap);
    }
    //Check the backlog
    if (backlogTypeId){
        if (backlogTypeId=="Y"){
            taskList.each{taskMap ->
                if(taskMap.custRequestTypeId=="RF_UNPLAN_BACKLOG"){
                    resultList.add(taskMap);
                }
            }
        }
        if (backlogTypeId=="N"){
            taskList.each{taskMap ->
                if(taskMap.custRequestTypeId=="RF_PROD_BACKLOG"){
                    resultList.add(taskMap);
                }
            }
        }
    } else {
        taskList.each { taskMap ->
            resultList.add(taskMap);
            }
    }
    // Check party assigned
    if (partyId){
        assignedList = resultList;
        resultList = [];
        assignedList.each { assignedMap ->
            workEffortId = assignedMap.taskId;
            assignToList = from("WorkEffortPartyAssignment").where("workEffortId", workEffortId, "partyId", partyId).queryList();
            if (assignToList) {
                assignedMap.partyId = assignToList[0].partyId;
                resultList.add(assignedMap);
            }
       }
    } else {
        assignedList = resultList;
        resultList = [];
        assignedList.each { assignedMap ->
            workEffortId = assignedMap.taskId;
            assignToList = from("WorkEffortPartyAssignment").where("workEffortId", workEffortId).queryList();
            if (assignToList) {
                assignedMap.partyId = assignToList[0].partyId;
                resultList.add(assignedMap);
            } else {
                resultList.add(assignedMap);
            }
       }
    }
    
    resultList.each{resultMap ->
        if (resultMap.taskTypeId=="SCRUM_TASK_IMPL"){
            implementTaskList.add(resultMap);
        }
        if (resultMap.taskTypeId=="SCRUM_TASK_INST"){
            installTaskList.add(resultMap);
        }
        if (resultMap.taskTypeId=="SCRUM_TASK_TEST"){
            testTaskList.add(resultMap);
        }
        if (resultMap.taskTypeId=="SCRUM_TASK_ERROR"){
            errorTaskList.add(resultMap);
        }
    }
    
    if (implementTaskList){
        context.implementTaskList = implementTaskList;
    }
    if (installTaskList){
        context.installTaskList = installTaskList;
    }
    if (testTaskList){
        context.testTaskList = testTaskList;
    }
    if (errorTaskList){
        context.errorTaskList = errorTaskList;
    }
}
