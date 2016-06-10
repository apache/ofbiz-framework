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

import java.sql.*;
import java.sql.Timestamp;
import java.util.Calendar;
import net.fortuna.ical4j.model.DateTime;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.condition.*;
import sun.util.calendar.LocalGregorianCalendar.Date;

def module = "AddProductBacklogItem.groovy";

// find cust request and items
def inputFields = [:];

if(parameters.statusId == null){
    parameters.statusId = "";
}else if("Any".equals(parameters.statusId)){
    parameters.statusId = "";
}
inputFields.putAll(parameters);
inputFields.custRequestTypeId = "RF_PROD_BACKLOG";
def performFindResults = runService('performFind', ["entityName": "CustRequestAndCustRequestItem", "inputFields": inputFields, "orderBy": "custSequenceNum"]);
def custRequestAndItems = performFindResults.listIt.getCompleteList();
performFindResults.listIt.close();

// prepare cust request item list [cust request and item Map]
def countSequence = 1;
def custRequestAndCustRequestItems = [];
custRequestAndItems.each() { custRequestAndItem ->
    def tempCustRequestAndItem = [:];
    tempCustRequestAndItem.putAll(custRequestAndItem);
    tempCustRequestAndItem.custSequenceNum = countSequence;
    tempCustRequestAndItem.realSequenceNum = custRequestAndItem.custSequenceNum;
    // if custRequest has task then get Actual Hours
    custWorkEffortList = from("CustRequestWorkEffort").where("custRequestId", custRequestAndItem.custRequestId).queryList();
    if (custWorkEffortList) {
        actualHours = 0.00;
        custWorkEffortList.each() { custWorkEffortMap ->
            result = runService('getScrumActualHour', ["taskId" : custWorkEffortMap.workEffortId,"partyId" : null, "userLogin" : userLogin]);
            actualHours += result.actualHours;
        }
        if(actualHours) {
            tempCustRequestAndItem.actualHours = actualHours;
        } else {
            tempCustRequestAndItem.actualHours = null;
        }
    } else {
        tempCustRequestAndItem.actualHours = null;
    }
    custRequestAndCustRequestItems.add(tempCustRequestAndItem);
    countSequence ++;
}

if ("N".equals(parameters.sequence)) { // re-order category list item
    custRequestAndCustRequestItems = UtilMisc.sortMaps(custRequestAndCustRequestItems, ["parentCustRequestId"]);
}
//set status back for display in Find screen
if("".equals(parameters.statusId)){
    parameters.statusId = "Any";
}
context.custRequestAndCustRequestItems = custRequestAndCustRequestItems;

// unplanned backlog item list

productId = parameters.productId;

conditionList = [];
orConditionList = [];
mainConditionList = [];

conditionList.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_UNPLAN_BACKLOG"));
conditionList.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, parameters.productId));

orConditionList.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_ACCEPTED"));
orConditionList.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REOPENED"));

orConditions = EntityCondition.makeCondition(orConditionList, EntityOperator.OR);
conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

mainConditionList.add(orConditions);
mainConditionList.add(conditions);

unplannedList = select("custRequestId", "custSequenceNum", "statusId", "description", "custEstimatedMilliSeconds", "custRequestName", "parentCustRequestId").from("CustRequestAndCustRequestItem").where(mainConditionList).orderBy("custSequenceNum").queryList();

def countSequenceUnplanned = 1;
def unplanBacklogItems = [];
unplannedList.each() { unplannedItem ->
    def tempUnplanned = [:];
    tempUnplanned.putAll(unplannedItem);
    tempUnplanned.custSequenceNum = countSequenceUnplanned;
    tempUnplanned.realSequenceNum = unplannedItem.custSequenceNum;
    // if custRequest has task then get Actual Hours
    unplanCustWorkEffortList = from("CustRequestWorkEffort").where("custRequestId", unplannedItem.custRequestId).queryList();
    if (unplanCustWorkEffortList) {
        actualHours = 0.00;
        unplanCustWorkEffortList.each() { custWorkEffortMap ->
            result = runService('getScrumActualHour', ["taskId" : custWorkEffortMap.workEffortId,"partyId" : null, "userLogin" : userLogin]);
            actualHours += result.actualHours;
        }
        if(actualHours) {
            tempUnplanned.actualHours = actualHours;
        } else {
            tempUnplanned.actualHours = null;
        }
    } else {
        tempUnplanned.actualHours = null;
    }
    unplanBacklogItems.add(tempUnplanned);
    countSequenceUnplanned ++;
}
if ("N".equals(parameters.UnplannedSequence)) { // re-order category list item
    unplanBacklogItems = UtilMisc.sortMaps(unplanBacklogItems, ["parentCustRequestId"]);
}
context.unplanBacklogItems = unplanBacklogItems;
