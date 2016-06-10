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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;

custRequestList = [];
custAndWorkEffortList = [];
revisionList = [];
listIt = [];
//for productId and custRequestId
if ((parameters.productId != null)||(parameters.custRequestId != null)||(parameters.workEffortId != null)||(viewIndex > 0)) {
    orList =  [];
    orList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_PROD_BACKLOG"));
    orList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_UNPLAN_BACKLOG"));
    andList = [];
    if (parameters.productId) {
        andList.add(EntityCondition.makeCondition("productId", EntityOperator.LIKE, parameters.productId + "%"));
    }
    if (parameters.custRequestId) {
        andList.add(EntityCondition.makeCondition("custRequestId", EntityOperator.LIKE, parameters.custRequestId + "%"));
    }
    andList.add(EntityCondition.makeCondition(orList, EntityOperator.OR));
    custRequestCond = EntityCondition.makeCondition(andList, EntityOperator.AND);
    custRequestList = from("CustRequestAndCustRequestItem").where(custRequestCond).queryList();
    
    custRequestIds = EntityUtil.getFieldListFromEntityList(custRequestList, "custRequestId", true);
    taskOrList =  [];
    taskOrList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_ERROR"));
    taskOrList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_TEST"));
    taskOrList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_IMPL"));
    taskOrList.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_INST"));
    taskAndList = [];
    taskAndList.add(EntityCondition.makeCondition("custRequestId", EntityOperator.IN, custRequestIds));
    taskAndList.add(EntityCondition.makeCondition(taskOrList, EntityOperator.OR));
    custAndWorkEffortCond = EntityCondition.makeCondition(taskAndList, EntityOperator.AND);
    custAndWorkEffortList = from("CustRequestAndWorkEffort").where(custAndWorkEffortCond).queryList();
    
    //for workEffortId
    workEffortIds = EntityUtil.getFieldListFromEntityList(custAndWorkEffortList, "workEffortId", true);
    revisionAndList = [];
    if (parameters.workEffortId) {
        revisionAndList.add(EntityCondition.makeCondition("workEffortId", EntityOperator.LIKE, parameters.workEffortId + "%"));
    } else {
        revisionAndList.add(EntityCondition.makeCondition("workEffortId", EntityOperator.IN, workEffortIds));
    }
    revisionAndList.add(EntityCondition.makeCondition("workEffortContentTypeId", EntityOperator.EQUALS, "TASK_SUB_INFO"));
    revisionCond = EntityCondition.makeCondition(revisionAndList, EntityOperator.AND);
    revisionList = from("WorkEffortAndContentDataResource").where(revisionCond).orderBy("-fromDate").queryList();
    
    if (revisionList) {
        revisionList.each { revisionMap ->
            inputMap = [:];
            inputMap.workEffortId = revisionMap.workEffortId;
            inputMap.contentId = revisionMap.contentId;
            inputMap.fromDate = revisionMap.fromDate;
            inputMap.contentName = revisionMap.contentName;
            inputMap.description = revisionMap.description;
            inputMap.drObjectInfo = revisionMap.drObjectInfo;
            custAndWorkEfffList = from("CustRequestAndWorkEffort").where("workEffortId", revisionMap.workEffortId).queryList();
            if (custAndWorkEfffList) {
                custAndWorkEfffMap = custAndWorkEfffList[0];
                custAndCustItemList = from("CustRequestAndCustRequestItem").where("custRequestId", custAndWorkEfffMap.custRequestId).queryList();
                if (custAndCustItemList) {
                    custAndCustItemMap = custAndCustItemList[0];
                    inputMap.productId = custAndCustItemMap.productId;
                    inputMap.custRequestId = custAndCustItemMap.custRequestId;
                    }
                }
            listIt.add(inputMap);
        }
        context.listIt = listIt;
    }
}
