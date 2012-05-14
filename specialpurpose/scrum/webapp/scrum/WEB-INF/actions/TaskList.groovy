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
import javolution.util.FastList;
import org.apache.tools.ant.taskdefs.Parallel.TaskList;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;
import java.sql.Timestamp;

partyId = userLogin.partyId;
taskUnplanList = [];
taskPlanList = [];
taskPartyList = [];
taskListDropdown = [];

//${projectId} - ${projectName} - ${sprintName} - ${groovy:description.substring(0,Math.min(description.length(),30))}[${custRequestId}] - ${groovy:taskName.substring(0,Math.min(taskName.length(),20))}[${taskId}]"/>

taskUnplanList = delegator.findByAnd("ProjectSprintBacklogTaskAndParty", ["partyId" : partyId,"taskCurrentStatusId": "STS_CREATED","custRequestTypeId":"RF_UNPLAN_BACKLOG"],["taskTypeId"], false);
taskUnplanList.each { taskUnplanMap ->
	unplanMap=[:];
	custRequestId = taskUnplanMap.custRequestId;
	productlist = delegator.findByAnd("CustRequestItem", ["custRequestId" : custRequestId],["productId"], false);
	productlist.each { productMap ->
		productId = productMap.productId;
		product = delegator.findOne("Product",["productId":productId], false);
			productName = product.internalName;
			unplanMap.taskId = taskUnplanMap.taskId;
			unplanMap.taskName = taskUnplanMap.taskName;
			unplanMap.projectId = taskUnplanMap.projectId;
			unplanMap.projectName = taskUnplanMap.projectName;
			unplanMap.sprintId = taskUnplanMap.sprintId;
			unplanMap.sprintName = taskUnplanMap.sprintName;
			unplanMap.custRequestId = custRequestId;
			unplanMap.description = taskUnplanMap.description;
			unplanMap.productId = productId;
			unplanMap.productName = productName;
			
	}
	taskPartyList.add(taskUnplanMap);
	taskListDropdown.add(unplanMap);
}

exprBldr =  FastList.newInstance();
exprBldr.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_PROD_BACKLOG"));
exprBldr.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_SCRUM_MEETINGS"));
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("taskCurrentStatusId", EntityOperator.EQUALS, "STS_CREATED"));
andExprs.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
andExprs.add(EntityCondition.makeCondition(exprBldr, EntityOperator.OR));
custRequestTypeCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);

taskPlanList = delegator.findList("ProjectSprintBacklogTaskAndParty", custRequestTypeCond, null,["taskTypeId","projectId","sprintId"] ,null, false);
taskPlanList.each { taskPlanMap ->
    planMap=[:];
    if ("RF_SCRUM_MEETINGS".equals(taskPlanMap.custRequestTypeId)) {
        workEffPartyAssignedList = delegator.findByAnd("WorkEffortPartyAssignment",["partyId" : partyId, "workEffortId" : taskPlanMap.taskId], null, false);
        workEffPartyAssignedMap = workEffPartyAssignedList[0];
        if (!"SCAS_COMPLETED".equals(workEffPartyAssignedMap.statusId)) {
            taskPartyList.add(taskPlanMap);
            taskListDropdown.add(taskPlanMap);
        }
    } else {
        if (taskPlanMap.projectId) {
            taskPartyList.add(taskPlanMap);
            taskListDropdown.add(taskPlanMap);
        } else {
            custRequestId = taskPlanMap.custRequestId;
            productlist = delegator.findByAnd("CustRequestItem", ["custRequestId" : custRequestId],["productId"], false);
            product = delegator.findOne("Product",["productId":productlist[0].productId], false);
            productName = product.internalName;
            planMap.taskId = taskPlanMap.taskId;
            planMap.taskTypeId = taskPlanMap.taskTypeId;
            planMap.taskName = taskPlanMap.taskName;
            planMap.projectId = taskPlanMap.projectId;
            planMap.projectName = taskPlanMap.projectName;
            planMap.sprintId = taskPlanMap.sprintId;
            planMap.sprintName = taskPlanMap.sprintName;
            planMap.custRequestId = custRequestId;
            planMap.description = taskPlanMap.description;
            planMap.productId = productlist[0].productId;
            planMap.productName = productName;
            taskPartyList.add(planMap);
            taskListDropdown.add(planMap);
        }

    }
}
if (taskPartyList){
	context.taskPartyList = taskPartyList;
}
if (taskListDropdown){
	context.taskListDropdown = taskListDropdown;
}


