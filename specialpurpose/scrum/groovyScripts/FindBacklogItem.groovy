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
import java.lang.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.entity.condition.*
import java.sql.Timestamp

productId =parameters.productId
custRequestList=[]
backlogList=[]
custRequestList = from("CustRequestItem").where("productId", productId).queryList()
custRequestList.each { custRequestListMap ->
    custRequestId=custRequestListMap.custRequestId
    exprBldr = []
    exprBldr.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REOPENED"))
    exprBldr.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REVIEWED"))
    andExprs = []
    andExprs.add(EntityCondition.makeCondition("custRequestId", EntityOperator.EQUALS, custRequestId))
    andExprs.add(EntityCondition.makeCondition(exprBldr, EntityOperator.OR))
    custRequestTypeCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND)
    orderBy = ["custRequestTypeId"]
    productBacklogList = from("CustRequest").where(andExprs).orderBy("custRequestTypeId").queryList()
    productBacklogList.each { productBacklogMap ->
        productBackId = productBacklogMap.custRequestId
        taskBacklogList = from("CustRequestWorkEffort").where("custRequestId", productBackId).queryList()
        int countImplTask=0, countImplTaskComplete=0, countInstallTask=0, countInstallTaskComplete=0, countErrTask=0, countErrTaskComplete=0, countTestTask=0
        taskBacklogList.each { taskBacklogMap ->
            taskId = taskBacklogMap.workEffortId
            
            task = from("WorkEffort").where("workEffortId", taskId).queryOne()
            if (task.workEffortTypeId == "SCRUM_TASK_IMPL") {
                countImplTask+=1
                if ( task.currentStatusId == "STS_COMPLETED" || task.currentStatusId == "STS_CANCELLED") {
                    countImplTaskComplete+=1
                }
            }
            else if (task.workEffortTypeId == "SCRUM_TASK_INST") {
                countInstallTask+=1
                if ( task.currentStatusId == "STS_COMPLETED" || task.currentStatusId == "STS_CANCELLED") {
                    countInstallTaskComplete+=1
                }
            }
            else if (task.workEffortTypeId == "SCRUM_TASK_ERROR") {
                countErrTask+=1
                if ( task.currentStatusId == "STS_COMPLETED" || task.currentStatusId == "STS_CANCELLED") {
                    countErrTaskComplete+=1
                }
            }
            else if (task.workEffortTypeId == "SCRUM_TASK_TEST" || task.currentStatusId == "STS_CANCELLED") {
                countTestTask+=1
            }
        }
        if ((countImplTask > 0 || countErrTask > 0 || countInstallTask > 0) && countImplTask == countImplTaskComplete 
            && countInstallTask == countInstallTaskComplete && countErrTask == countErrTaskComplete && countTestTask > 0) {
            productBacklogMap = productBacklogMap.getAllFields()
            backlogList.add(productBacklogMap)
        }
    }
}
if (backlogList) {
    backlogList = UtilMisc.sortMaps(backlogList, ["-custRequestName"])
    context.backlogList = backlogList
}

