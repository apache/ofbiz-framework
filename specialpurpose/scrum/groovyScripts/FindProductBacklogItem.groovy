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

import java.sql.*
import java.sql.Timestamp
import java.util.Calendar
import net.fortuna.ical4j.model.DateTime
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.condition.*
import sun.util.calendar.LocalGregorianCalendar.Date

def module = "FindProductBacklogItem.groovy"

// list planned and unplanned backlog
conditionBacklogList = []
orConditionBacklogList = []
mainConditionBacklogList = []
orConditionsBacklog =  null
orderBy = "custRequestDate"

// Prevents the query on all records when loading the screen for the first time
if ("Y".equals(parameters.noConditionFind)) {
    if(parameters.productId){
        conditionBacklogList.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, parameters.productId))
    }
    
    if(parameters.custRequestTypeId){
        conditionBacklogList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, parameters.custRequestTypeId))
    }else{
        // Adding both possibilities to the condition
        orConditionBacklogList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_UNPLAN_BACKLOG"))
        orConditionBacklogList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_PROD_BACKLOG"))
        orConditionsBacklog = EntityCondition.makeCondition(orConditionBacklogList, EntityOperator.OR)
    }
    
    if(parameters.billed){
        conditionBacklogList.add(EntityCondition.makeCondition("billed", EntityOperator.EQUALS, parameters.billed))
    }else{
        // Adding both choices to the condition
        orConditionBacklogList.add(EntityCondition.makeCondition("billed", EntityOperator.EQUALS, "Y"))
        orConditionBacklogList.add(EntityCondition.makeCondition("billed", EntityOperator.EQUALS, "N"))
        orConditionsBacklog = EntityCondition.makeCondition(orConditionBacklogList, EntityOperator.OR)
    }
    
    if(parameters.statusId){
        orderBy = "custSequenceNum"
        conditionBacklogList.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, parameters.statusId))
    }
    
    if(parameters.parentCustRequestId){
        conditionBacklogList.add(EntityCondition.makeCondition("parentCustRequestId", EntityOperator.EQUALS, parameters.parentCustRequestId))
    }
    
    if(parameters.description){
        conditionBacklogList.add(EntityCondition.makeCondition("description", EntityOperator.LIKE, "%" + parameters.description + "%"))
    }
    
    if(parameters.fromPartyId){
        conditionBacklogList.add(EntityCondition.makeCondition("fromPartyId", EntityOperator.LIKE, "%" + parameters.fromPartyId + "%"))
    }
    
    if (parameters.custRequestDate){
        fromDate = parameters.custRequestDate
        fromDate = fromDate + " " + "00:00:00.000"
        conditionBacklogList.add(EntityCondition.makeCondition("custRequestDate", EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(fromDate)))
        thruDate = parameters.custRequestDate
        thruDate = thruDate + " " + "23:59:59.999"
        conditionBacklogList.add(EntityCondition.makeCondition("custRequestDate", EntityOperator.LESS_THAN_EQUAL_TO, Timestamp.valueOf(thruDate)))
    }
    
    if(parameters.custRequestId){
        conditionBacklogList.add(EntityCondition.makeCondition("custRequestId", EntityOperator.LIKE, parameters.custRequestId + "%"))
    }
    
    conditionsBacklog = EntityCondition.makeCondition(conditionBacklogList, EntityOperator.AND)
    
    if(UtilValidate.isNotEmpty(orConditionsBacklog)){
        mainConditionBacklogList.add(orConditionsBacklog)
    }
    
    mainConditionBacklogList.add(conditionsBacklog)
    
    // Request
    backlogList = select("custRequestId","custRequestTypeId", "custSequenceNum", "statusId", "description", "custEstimatedMilliSeconds", "custRequestName", "parentCustRequestId","productId","billed","custRequestDate","fromPartyId")
                    .from("CustRequestAndCustRequestItem")
                    .where(mainConditionBacklogList)
                    .orderBy("-custRequestTypeId", orderBy)
                    .queryList()
                    
    def countSequenceBacklog = 1
    def backlogItems = []
    backlogList.each() { backlogItem ->
        def tempBacklog = [:]
        tempBacklog.putAll(backlogItem)
        tempBacklog.custSequenceNum = countSequenceBacklog
        tempBacklog.realSequenceNum = backlogItem.custSequenceNum
        // if custRequest has task then get Actual Hours
        backlogCustWorkEffortList = from("CustRequestWorkEffort").where("custRequestId", backlogItem.custRequestId).queryList()
        if (backlogCustWorkEffortList) {
            actualHours = 0.00
            backlogCustWorkEffortList.each() { custWorkEffortMap ->
                result = runService('getScrumActualHour', ["taskId" : custWorkEffortMap.workEffortId,"partyId" : null, "userLogin" : userLogin])
                actualHours += result.actualHours
            }
            if(actualHours) {
                tempBacklog.actualHours = actualHours
            } else {
                tempBacklog.actualHours = null
            }
        } else {
            tempBacklog.actualHours = null
        }
        backlogItems.add(tempBacklog)
        countSequenceBacklog ++
    }
    
    // re-order category list item
    if ("N".equals(parameters.sequence)) {
        backlogItems = UtilMisc.sortMaps(backlogItems, ["parentCustRequestId"])
    }
    context.backlogItems = backlogItems
}
