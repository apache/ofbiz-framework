/*
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

import java.sql.Timestamp;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

productId = parameters.productId;
entryExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
        EntityCondition.makeCondition("invoiceId", EntityOperator.NOT_EQUAL, null),
        ], EntityOperator.AND);
// check if latest invoice generated is still in process so allow re-generation to correct errors
entryIterator = from("ProjectSprintBacklogTaskAndTimeEntryTimeSheet").where(entryExprs).orderBy("-fromDate").queryIterator();
while (entryItem = entryIterator.next()) {
    invoice = entryItem.getRelatedOne("Invoice", false);
    if (invoice.getString("statusId").equals("INVOICE_IN_PROCESS")) {
        context.partyIdFrom = invoice.partyIdFrom;
        context.partyId = invoice.partyId;
        context.invoiceId = invoice.invoiceId;
        context.invoiceDate = invoice.invoiceDate;
        break;
        }
    }
entryIterator.close();
//start of this month
context.thruDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp());

// build find task conditions
def taskConds = UtilMisc.toList(EntityCondition.makeCondition("productId", parameters.productId));
taskConds.add(EntityCondition.makeCondition("invoiceId", null));
taskConds.add(EntityCondition.makeCondition("timesheetStatusId", "TIMESHEET_COMPLETED"));
if (parameters.fromDate) {
    fromDate = parameters.fromDate;
    if (fromDate.length() < 14) {
        fromDate = fromDate + " " + "00:00:00.000";
    }
    taskConds.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(fromDate)));
}
if (parameters.thruDate) {
    thruDate = parameters.thruDate;
    if (thruDate.length() < 14) {
        thruDate = thruDate + " " + "00:00:00.000";
    }
    taskConds.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, Timestamp.valueOf(thruDate)));
} else {
    taskConds.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, context.thruDate));
}
// include meeting ?
if ("N".equals(includeMeeting)) {
    taskConds.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.NOT_EQUAL, "RF_SCRUM_MEETINGS"));
}
// get sprint task list
def sprintTasks = from("ProjectSprintBacklogTaskAndTimeEntryTimeSheet").where(taskConds).queryList();

// get cancelled backlog task list
def cancelledBacklogTasks = from("CancelledBacklogsTaskAndTimeEntryTimeSheet").where(taskConds).queryList();

// get unplanned task list
def unplannedTasks = from("UnPlannedBacklogsTaskAndTimeEntryTimeSheet").where(taskConds).queryList();

def hoursNotYetBilledTasks = [];
hoursNotYetBilledTasks.addAll(sprintTasks);
hoursNotYetBilledTasks.addAll(cancelledBacklogTasks);
hoursNotYetBilledTasks.addAll(unplannedTasks);
context.hoursNotYetBilledTasks = UtilMisc.sortMaps(hoursNotYetBilledTasks, ["productId","custRequestId","taskId","fromDate"])

// get time entry date
timeEntryList = UtilMisc.sortMaps(hoursNotYetBilledTasks, ["fromDate"])
if (!parameters.fromDate && timeEntryList) {
    context.resultDate = timeEntryList[0].fromDate;
}
