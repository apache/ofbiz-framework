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
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.webapp.website.WebSiteWorker;
import java.sql.Timestamp;

uiLabelMap = UtilProperties.getResourceBundleMap("scrumUiLabels", locale);
partyId = parameters.partyId;
if (!partyId) {
    partyId = parameters.userLogin.partyId;
}

// show the requested timesheet, otherwise the current , if not exist create
timesheet = null;
timesheetId = parameters.timesheetId;
if (timesheetId) {
    timesheet = delegator.findOne("Timesheet", ["timesheetId" : timesheetId], false);
    partyId = timesheet.partyId; // use the party from this timesheet
} else {
    // make sure because of timezone changes, not a duplicate timesheet is created
    midweek = UtilDateTime.addDaysToTimestamp(UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp()),3);
    entryExprs = EntityCondition.makeCondition([
        EntityCondition.makeCondition("fromDate", EntityComparisonOperator.LESS_THAN, midweek),
        EntityCondition.makeCondition("thruDate", EntityComparisonOperator.GREATER_THAN, midweek),
        EntityCondition.makeCondition("partyId", EntityComparisonOperator.EQUALS, partyId)
        ], EntityOperator.AND);
    entryIterator = delegator.find("Timesheet", entryExprs, null, null, null, null);
    timesheet = entryIterator.next();
    entryIterator.close();
    if (timesheet == null) {
        result = dispatcher.runSync("createProjectTimesheet", ["userLogin" : parameters.userLogin, "partyId" : partyId]);
        if (result && result.timesheetId) {
            timesheet = delegator.findOne("Timesheet", ["timesheetId" : result.timesheetId], false);
        }
    }
}
if (!timesheet) return;
context.timesheet = timesheet;
context.weekNumber = UtilDateTime.weekNumber(timesheet.fromDate);

// get the user names
context.partyNameView = delegator.findOne("PartyNameView",["partyId" : partyId], false);
// get the default rate for this person
rateTypes = EntityUtil.filterByDate(delegator.findByAnd("PartyRate", ["partyId" : partyId, "defaultRate" : "Y"]));
if (rateTypes) {
    context.defaultRateTypeId = rateTypes[0].rateTypeId;
}

entries = [];
entry = ["timesheetId" : timesheet.timesheetId];
leaveEntry = ["timesheetId" : timesheet.timesheetId];
taskTotal = 0.00;
planTotal = 0.00;
leaveTaskTotal = 0.00;
leavePlanTotal = 0.00;
day0Total = 0.00; day1Total=0.00; day2Total=0.00; day3Total=0.00; day4Total=0.00; day5Total=0.00; day6Total=0.00;
pDay0Total = 0.00; pDay1Total=0.00; pDay2Total=0.00; pDay3Total=0.00; pDay4Total=0.00; pDay5Total=0.00; pDay6Total=0.00;
pHours = 0.00;
timeEntry = null;
lastTimeEntry = null;
emplLeaveEntry = null;
lastEmplLeaveEntry = null;

// retrieve work effort data when the workeffortId has changed.
void retrieveWorkEffortData() {
        // get the planned number of hours
        entryWorkEffort = lastTimeEntry.getRelatedOne("WorkEffort");
        if (entryWorkEffort) {
            plannedHours = entryWorkEffort.getRelated("WorkEffortSkillStandard");
            pHours = 0.00;
            plannedHours.each { plannedHour ->
                if (plannedHour.estimatedDuration) {
                    pHours += plannedHour.estimatedDuration;
                }
            }
            estimatedHour =  0.00;
            
            estimatedMilliSeconds = entryWorkEffort.estimatedMilliSeconds
            if (estimatedMilliSeconds > 0) 
                estimatedHour = estimatedMilliSeconds/3600000;
            entry.plannedHours = estimatedHour;
            //entry.plannedHours = pHours;
            planHours = 0.0;
            planHours = lastTimeEntry.planHours;
            lastTimeEntryOfTasks = delegator.findByAnd("TimeEntry", ["workEffortId" : lastTimeEntry.workEffortId, "partyId" : partyId], ["-fromDate"]);
            if (lastTimeEntryOfTasks.size() != 0) lastTimeEntry = lastTimeEntryOfTasks[0];
            if (planHours < 1) {
                planHours = estimatedHour;
            }
            entry.planHours = lastTimeEntry.planHours;
            actualHours = entryWorkEffort.getRelated("TimeEntry");
            aHours = 0.00;
            actualHours.each { actualHour ->
                if (actualHour.hours) {
                    aHours += actualHour.hours;
                }
            }
            entry.actualHours = aHours;
            // get party assignment data to be able to set the task to complete
            workEffortPartyAssigns = EntityUtil.filterByDate(entryWorkEffort.getRelatedByAnd("WorkEffortPartyAssignment", ["partyId" : partyId]));
            if (workEffortPartyAssigns) {
                workEffortPartyAssign = workEffortPartyAssigns[0];
                entry.fromDate = workEffortPartyAssign.getTimestamp("fromDate");
                entry.roleTypeId = workEffortPartyAssign.roleTypeId;
                if ("SCAS_COMPLETED".equals(workEffortPartyAssign.statusId)) {
                    entry.checkComplete = "Y";
                    
                }
            } else {
                if ("STS_COMPLETED".equals(entryWorkEffort.currentStatusId)) {
                    entry.checkComplete = "Y";
                }
            }

            // get project/phase information
            entry.workEffortId = entryWorkEffort.workEffortId;
            entry.workEffortName = entryWorkEffort.workEffortName;
            result = dispatcher.runSync("getProjectInfoFromTask", ["userLogin" : parameters.userLogin,"taskId" : entryWorkEffort.workEffortId]);
                entry.phaseId = result.phaseId;
                entry.phaseName = result.phaseName;
                entry.projectId = result.projectId;
                entry.projectName = result.projectName;
                entry.taskWbsId = result.taskWbsId;

        }
        entry.acualTotal = taskTotal;
        entry.planTotal = planTotal;
        //Drop Down Lists
        if (entry.checkComplete != "Y") {
            if (aHours > 0.00)
                entries.add(entry);
        } else {
                entries.add(entry);
        }
        // start new entry
        taskTotal = 0.00;
        planTotal = 0.00;
        entry = ["timesheetId" : timesheet.timesheetId];
}

timeEntries = timesheet.getRelated("TimeEntry", ["workEffortId", "rateTypeId", "fromDate"]);
te = timeEntries.iterator();
while (te.hasNext()) {
    // only fill lastTimeEntry when not the first time
    if (timeEntry!=void) {
        lastTimeEntry = timeEntry;
    }
    timeEntry = te.next();

    if (lastTimeEntry &&
            (!lastTimeEntry.workEffortId.equals(timeEntry.workEffortId) ||
            !lastTimeEntry.rateTypeId.equals(timeEntry.rateTypeId))) {
            retrieveWorkEffortData();
        }
    if (timeEntry.hours) {
        dayNumber = "d" + (timeEntry.fromDate.getTime() - timesheet.fromDate.getTime()) / (24*60*60*1000);
        hours = timeEntry.hours.doubleValue();
        entry.put(String.valueOf(dayNumber), hours);
        if (dayNumber.equals("d0")) day0Total += hours;
        if (dayNumber.equals("d1")) day1Total += hours;
        if (dayNumber.equals("d2")) day2Total += hours;
        if (dayNumber.equals("d3")) day3Total += hours;
        if (dayNumber.equals("d4")) day4Total += hours;
        if (dayNumber.equals("d5")) day5Total += hours;
        if (dayNumber.equals("d6")) day6Total += hours;
        taskTotal += hours;
    }
    if (timeEntry.planHours) {
        dayNumber = "pd" + (timeEntry.fromDate.getTime() - timesheet.fromDate.getTime()) / (24*60*60*1000);
        planHours = timeEntry.planHours.doubleValue();
        entry.put(String.valueOf(dayNumber), planHours);
        if (dayNumber.equals("pd0")) pDay0Total += planHours;
        if (dayNumber.equals("pd1")) pDay1Total += planHours;
        if (dayNumber.equals("pd2")) pDay2Total += planHours;
        if (dayNumber.equals("pd3")) pDay3Total += planHours;
        if (dayNumber.equals("pd4")) pDay4Total += planHours;
        if (dayNumber.equals("pd5")) pDay5Total += planHours;
        if (dayNumber.equals("pd6")) pDay6Total += planHours;
        planTotal += planHours;

    }
    entry.rateTypeId = timeEntry.rateTypeId;
}
//retrieve Empl Leave data.
void retrieveEmplLeaveData() {
        if (lastEmplLeaveEntry) {
            //service get Hours
            inputMap = [:];
            inputMap.userLogin = parameters.userLogin;
            inputMap.partyId = lastEmplLeaveEntry.partyId;
            inputMap.leaveTypeId = lastEmplLeaveEntry.leaveTypeId;
            inputMap.fromDate = lastEmplLeaveEntry.fromDate;
            result = dispatcher.runSync("getPartyLeaveHoursForDate", inputMap);
            if (result.hours) {
                leaveEntry.plannedHours = result.hours;
                leaveEntry.planHours =  result.hours;
            }
            if (lastEmplLeaveEntry.leaveStatus == "LEAVE_APPROVED") {
                leaveEntry.checkComplete = "Y";
            }
            leaveEntry.partyId = lastEmplLeaveEntry.partyId;
            leaveEntry.leaveTypeId = lastEmplLeaveEntry.leaveTypeId;
            leaveEntry.leavefromDate = lastEmplLeaveEntry.fromDate;
            leaveEntry.leavethruDate = lastEmplLeaveEntry.thruDate;
            leaveEntry.description = lastEmplLeaveEntry.description;
        }
        leaveEntry.acualTotal = leaveTaskTotal;
        leaveEntry.planHours = leavePlanTotal;
        leaveEntry.actualHours = leaveTaskTotal;
        //Drop Down Lists
        entries.add(leaveEntry);
        // start new leaveEntry
        leaveTaskTotal = 0.00;
        leavePlanTotal = 0.00;
        leaveEntry = ["timesheetId" : timesheet.timesheetId];
   }

// define condition
findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
leaveExprs = [];
leaveExprs.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, timesheet.fromDate));
leaveExprs.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, timesheet.thruDate));
leaveExprs.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
emplLeave = delegator.find("EmplLeave", EntityCondition.makeCondition(leaveExprs, EntityOperator.AND), null, null, null, findOpts);

while ((emplLeaveMap = emplLeave.next())) {
    if (emplLeaveEntry!=void) {
        lastEmplLeaveEntry = emplLeaveEntry;
    }
    
    emplLeaveEntry = emplLeaveMap;
    
    if (lastEmplLeaveEntry && (
            !lastEmplLeaveEntry.leaveTypeId.equals(emplLeaveEntry.leaveTypeId) ||
            !lastEmplLeaveEntry.partyId.equals(emplLeaveEntry.partyId))) {
            retrieveEmplLeaveData();
        }
    input = [:];
    input.userLogin = parameters.userLogin;
    input.partyId = emplLeaveEntry.partyId;
    input.leaveTypeId = emplLeaveEntry.leaveTypeId;
    input.fromDate = emplLeaveEntry.fromDate;
    resultHours = dispatcher.runSync("getPartyLeaveHoursForDate", input);
    
    if (resultHours.hours) {
        leaveDayNumber = "d" + (emplLeaveEntry.fromDate.getTime() - timesheet.fromDate.getTime()) / (24*60*60*1000);
        inputMap = [:];
        inputMap.userLogin = parameters.userLogin;
        inputMap.partyId = emplLeaveEntry.partyId;
        inputMap.leaveTypeId = emplLeaveEntry.leaveTypeId;
        inputMap.fromDate = emplLeaveEntry.fromDate;
        resultHours = dispatcher.runSync("getPartyLeaveHoursForDate", inputMap);
        leaveHours = resultHours.hours.doubleValue();
        leaveEntry.put(String.valueOf(leaveDayNumber), leaveHours);
        if (leaveDayNumber.equals("d0")) day0Total += leaveHours;
        if (leaveDayNumber.equals("d1")) day1Total += leaveHours;
        if (leaveDayNumber.equals("d2")) day2Total += leaveHours;
        if (leaveDayNumber.equals("d3")) day3Total += leaveHours;
        if (leaveDayNumber.equals("d4")) day4Total += leaveHours;
        if (leaveDayNumber.equals("d5")) day5Total += leaveHours;
        if (leaveDayNumber.equals("d6")) day6Total += leaveHours;
        leaveTaskTotal += leaveHours;
    }
    if (resultHours.hours) {
        leavePlanDay = "pd" + (emplLeaveEntry.fromDate.getTime() - timesheet.fromDate.getTime()) / (24*60*60*1000);
        inputMap = [:];
        inputMap.userLogin = parameters.userLogin;
        inputMap.partyId = emplLeaveEntry.partyId;
        inputMap.leaveTypeId = emplLeaveEntry.leaveTypeId;
        inputMap.fromDate = emplLeaveEntry.fromDate;
        resultPlanHours = dispatcher.runSync("getPartyLeaveHoursForDate", inputMap);
        leavePlanHours = resultPlanHours.hours.doubleValue();
        leaveEntry.put(String.valueOf(leavePlanDay), leavePlanHours);
        if (leavePlanDay.equals("pd0")) pDay0Total += leavePlanHours;
        if (leavePlanDay.equals("pd1")) pDay1Total += leavePlanHours;
        if (leavePlanDay.equals("pd2")) pDay2Total += leavePlanHours;
        if (leavePlanDay.equals("pd3")) pDay3Total += leavePlanHours;
        if (leavePlanDay.equals("pd4")) pDay4Total += leavePlanHours;
        if (leavePlanDay.equals("pd5")) pDay5Total += leavePlanHours;
        if (leavePlanDay.equals("pd6")) pDay6Total += leavePlanHours;
        leavePlanTotal += leavePlanHours;
    }
    leaveEntry.rateTypeId = "STANDARD";
}
emplLeave.close();

if (timeEntry) {
    lastTimeEntry = timeEntry;
    retrieveWorkEffortData();
    }
if (emplLeaveEntry) {
    lastEmplLeaveEntry = emplLeaveEntry;
    retrieveEmplLeaveData();
    }

// add empty lines if timesheet not completed
if (!timesheet.statusId.equals("TIMESHEET_COMPLETED")) {
    for (c=0; c < 3; c++) { // add empty lines
        entries.add(["timesheetId" : timesheet.timesheetId]);
    }
}

// add the totals line if at least one entry
if (timeEntry || emplLeaveEntry) {
    entry = ["timesheetId" : timesheet.timesheetId];
    entry.d0 = day0Total;
    entry.d1 = day1Total;
    entry.d2 = day2Total;
    entry.d3 = day3Total;
    entry.d4 = day4Total;
    entry.d5 = day5Total;
    entry.d6 = day6Total;
    entry.pd0 = pDay0Total;
    entry.pd1 = pDay1Total;
    entry.pd2 = pDay2Total;
    entry.pd3 = pDay3Total;
    entry.pd4 = pDay4Total;
    entry.pd5 = pDay5Total;
    entry.pd6 = pDay6Total;
    entry.phaseName = uiLabelMap.ScrumTotals;
    entry.workEffortId = "Totals";
    entry.total = day0Total + day1Total + day2Total + day3Total + day4Total + day5Total + day6Total;
    entries.add(entry);
}
context.timeEntries = entries;
// get all timesheets of this user, including the planned hours
timesheetsDb = delegator.findByAnd("Timesheet", ["partyId" : partyId], ["fromDate DESC"]);
timesheets = new LinkedList();
timesheetsDb.each { timesheetDb ->
    //get hours from EmplLeave;
    findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
    leaveExprsList = [];
    leaveExprsList.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, timesheetDb.fromDate));
    leaveExprsList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, timesheetDb.thruDate));
    leaveExprsList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
    emplLeaveList = delegator.find("EmplLeave", EntityCondition.makeCondition(leaveExprsList, EntityOperator.AND), null, null, null, findOpts);
    leaveHours = 0.00;
    
    while ((emplLeaveMap = emplLeaveList.next())) {
        emplLeaveEntry = emplLeaveMap;
        inputData = [:];
        inputData.userLogin = parameters.userLogin;
        inputData.partyId = emplLeaveEntry.partyId;
        inputData.leaveTypeId = emplLeaveEntry.leaveTypeId;
        inputData.fromDate = emplLeaveEntry.fromDate;
        resultHour = dispatcher.runSync("getPartyLeaveHoursForDate", inputData);
        if (resultHour) {
            leaveActualHours = resultHour.hours.doubleValue();
            leaveHours += leaveActualHours;
        }
    }
    //get hours from TimeEntry;
    timesheet = [:];
    timesheet.putAll(timesheetDb);
    entries = timesheetDb.getRelated("TimeEntry");
    hours = 0.00;
    entries.each { timeEntry ->
        if (timeEntry.hours) {
            hours += timeEntry.hours.doubleValue();
        }
    }
    timesheet.weekNumber = UtilDateTime.weekNumber(timesheetDb.fromDate);
    timesheet.hours = hours + leaveHours;
    timesheets.add(timesheet);
    emplLeaveList.close();
}
context.timesheets = timesheets;

// get existing task that no assign
taskList=[];
projectSprintBacklogAndTaskList = [];
backlogIndexList = [];
projectAndTaskList = delegator.findByAnd("ProjectSprintBacklogAndTask", ["sprintTypeId" : "SCRUM_SPRINT","taskCurrentStatusId" : "STS_CREATED"], ["projectName ASC","taskActualStartDate DESC"]);
projectAndTaskList.each { projectAndTaskMap ->
userLoginId = userLogin.partyId;
    sprintId = projectAndTaskMap.sprintId;
    workEffortList = delegator.findByAnd("WorkEffortAndProduct", ["workEffortId" : projectAndTaskMap.projectId]);
    backlogIndexList.add(workEffortList[0].productId);
	
    partyAssignmentSprintList = delegator.findByAnd("WorkEffortPartyAssignment", ["workEffortId" : sprintId, "partyId" : userLoginId]);
    partyAssignmentSprintMap = partyAssignmentSprintList[0];
    // if this userLoginId is a member of sprint
    if (partyAssignmentSprintMap) {
        workEffortId = projectAndTaskMap.taskId;
        partyAssignmentTaskList = delegator.findByAnd("WorkEffortPartyAssignment", ["workEffortId" : workEffortId]);
        partyAssignmentTaskMap = partyAssignmentTaskList[0];
        // if the task do not assigned
        if (partyAssignmentTaskMap) {
            custRequestTypeId = projectAndTaskMap.custRequestTypeId;
			backlogStatusId = projectAndTaskMap.backlogStatusId;
			if (custRequestTypeId.equals("RF_SCRUM_MEETINGS") && backlogStatusId.equals("CRQ_REVIEWED")) {
				projectSprintBacklogAndTaskList.add(projectAndTaskMap);
			   }
            } else {
					projectSprintBacklogAndTaskList.add(0,projectAndTaskMap);
             }
        }
    }

// for unplanned taks.
unplanList=[];
if (backlogIndexList) {
    backlogIndex = new HashSet(backlogIndexList);
    custRequestList = delegator.findByAnd("CustRequest", ["custRequestTypeId" : "RF_UNPLAN_BACKLOG","statusId" : "CRQ_REVIEWED"],["custRequestDate DESC"]);
    if (custRequestList) {
        custRequestList.each { custRequestMap ->
            custRequestItemList = custRequestMap.getRelated("CustRequestItem");
			custRequestItem =  
			productOut = custRequestItemList[0].productId;
			product = delegator.findOne("Product", ["productId" : productOut], false);
            backlogIndex.each { backlogProduct ->
                productId = backlogProduct
                if (productId.equals(productOut)) {
                    custRequestWorkEffortList = delegator.findByAnd("CustRequestWorkEffort", ["custRequestId" : custRequestItemList[0].custRequestId]);
                    custRequestWorkEffortList.each { custRequestWorkEffortMap ->
                        partyAssignmentTaskList = delegator.findByAnd("WorkEffortPartyAssignment", ["workEffortId" : custRequestWorkEffortMap.workEffortId]);
                        partyAssignmentTaskMap = partyAssignmentTaskList[0];
                        // if the task do not assigned
                        if (!partyAssignmentTaskMap) {
                            result = [:];
                            workEffortMap = delegator.findOne("WorkEffort", ["workEffortId" : custRequestWorkEffortMap.workEffortId], false);
                            result.description = custRequestMap.description;
                            result.productName = product.internalName;
                            result.taskId = workEffortMap.workEffortId;
                            result.taskName = workEffortMap.workEffortName;
							result.custRequestTypeId = custRequestMap.custRequestTypeId;
							result.taskTypeId = workEffortMap.workEffortTypeId;
                            unplanList.add(result);
                        }
                    }
                }
            }
        }
    }
}
projectSprintBacklogAndTaskList = UtilMisc.sortMaps(projectSprintBacklogAndTaskList, ["projectName","sprintName","-taskTypeId","custRequestId"]);
projectSprintBacklogAndTaskList.each { projectSprintBacklogAndTaskMap ->
	blTypeId = projectSprintBacklogAndTaskMap.custRequestTypeId;
	if (blTypeId == "RF_SCRUM_MEETINGS"){
		taskList.add(projectSprintBacklogAndTaskMap);
	}
}
projectSprintBacklogAndTaskList = UtilMisc.sortMaps(projectSprintBacklogAndTaskList, ["-projectName","sprintName","-taskTypeId","custRequestId"]);
projectSprintBacklogAndTaskList.each { projectSprintBacklogAndTaskMap ->
	blTypeId = projectSprintBacklogAndTaskMap.custRequestTypeId;
	if (blTypeId == "RF_PROD_BACKLOG"){
		taskList.add(0,projectSprintBacklogAndTaskMap);
	}
}
unplanList = UtilMisc.sortMaps(unplanList,["-productName","-taskTypeId","custRequestId"]);
unplanList.each { unplanMap->
		taskList.add(0,unplanMap);
}
context.taskList = taskList;

// notification context
webSiteId = WebSiteWorker.getWebSiteId(request);
context.webSiteId = webSiteId;
