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
  
import java.util.*;
import java.lang.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;
import java.sql.Timestamp;
import javolution.util.FastList;
import javolution.util.FastMap;

delegator = parameters.delegator;
locale = parameters.locale;
timeZone = parameters.timeZone;

partyId = parameters.partyId;
if (!partyId) {
    partyId = parameters.userLogin.partyId;
}

// show the requested timesheet, otherwise the current , if not exist create
timesheet = null;
timesheetId = parameters.timesheetId;
//Debug.logInfo("====editweek: " + partyId + " timesheetId: " + timesheetId +"==========");
if (timesheetId) {
        timesheet = delegator.findByPrimaryKey("Timesheet", ["timesheetId" : timesheetId]);
        partyId = timesheet.partyId; // use the party from this timesheet
    } else { 
        start = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp());
        timesheets = delegator.findByAnd("Timesheet", ["partyId" : partyId, "fromDate" : start]);
        if (!UtilValidate.isEmpty(timesheets)) {
            timesheet = timesheets.get(0);
        } else {
            result = dispatcher.runSync("createProjectTimesheet", ["userLogin" : parameters.userLogin, "partyId" : partyId]);
            if (result && result.timesheetId) {
                timesheet = delegator.findByPrimaryKey("Timesheet", ["timesheetId" : result.timesheetId]);
            }
        }
}
// get the user names
context.partyNameView = delegator.findByPrimaryKey("PartyNameView",["partyId" : partyId]);
// get the default rate for this person
rateTypes = EntityUtil.filterByDate(delegator.findByAnd("PartyRate", ["partyId" : partyId, "defaultRate" : "Y"]));
if (UtilValidate.isNotEmpty(rateTypes)) {
    rateType = rateTypes.get(0);
    context.defaultRateTypeId = rateType.rateTypeId;
} 

if (!timesheet) return;
context.timesheet = timesheet;
context.weekNumber = UtilDateTime.weekNumber(timesheet.fromDate);

entries = new LinkedList(); 
entry = ["timesheetId" : timesheet.timesheetId, "check" : "true"];
taskTotal = 0.00;
day0Total = 0.00; day1Total=0.00; day2Total=0.00; day3Total=0.00; day4Total=0.00; day5Total=0.00; day6Total=0.00;
pHours = 0.00;
timeEntry = null;
lastTimeEntry = null;

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
            entry.plannedHours = pHours;
            
            // get party assignment data to be able to set the task to complete
            workEffortPartyAssigns = EntityUtil.filterByDate(entryWorkEffort.getRelatedByAnd("WorkEffortPartyAssignment", ["partyId" : partyId]));
            if (UtilValidate.isNotEmpty(workEffortPartyAssigns)) {
                workEffortPartyAssign = workEffortPartyAssigns.get(0);
                entry.fromDate = workEffortPartyAssign.getTimestamp("fromDate");
                entry.roleTypeId = workEffortPartyAssign.roleTypeId;
                if ("PAS_COMPLETED".equals(workEffortPartyAssign.statusId)) {
                    entry.checkComplete = "Y";
                }
            }
            
            // get project/phase information
            entry.workEffortId = entryWorkEffort.workEffortId;    
            entry.workEffortName = entryWorkEffort.workEffortName; 
            result = dispatcher.runSync("getProjectIdAndNameFromTask", ["userLogin" : parameters.userLogin,"taskId" : entryWorkEffort.workEffortId]);
                entry.phaseId = result.phaseId;    
                entry.phaseName = result.phaseName;  
                entry.projectId = result.projectId;  
                entry.projectName = result.projectName;  
                
        }
        entry.total = taskTotal;
        //Drop Down Lists
        entries.add(entry);
        // start new entry
        taskTotal = 0.00;
        entry = ["timesheetId" : timesheet.timesheetId, "check" : "true"];
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
        dayNumber = (timeEntry.fromDate.getTime() - timesheet.fromDate.getTime()) / (24*60*60*1000);
        hours = timeEntry.hours.doubleValue();
        entry.put(String.valueOf(dayNumber), hours);
        if (dayNumber == 0) day0Total += hours;
        if (dayNumber == 1) day1Total += hours;
        if (dayNumber == 2) day2Total += hours;
        if (dayNumber == 3) day3Total += hours;
        if (dayNumber == 4) day4Total += hours;
        if (dayNumber == 5) day5Total += hours;
        if (dayNumber == 6) day6Total += hours;
        taskTotal += hours;
    }
    entry.rateTypeId = timeEntry.rateTypeId;
}

if (timeEntry) {
    lastTimeEntry = timeEntry;
    retrieveWorkEffortData();
    }
    
// add empty lines if timesheet not completed    
if (!timesheet.statusId.equals("TIMESHEET_COMPLETED")) {
    for (c=0; c < 3; c++) { // add empty lines 
        entries.add(["timesheetId" : timesheet.timesheetId,"check" : "false"]);
    }
}

// add the totals line if at least one entry
if (timeEntry) {
    entry = ["timesheetId" : timesheet.timesheetId, "check" : "true"];
    entry."0" = day0Total;
    entry."1" = day1Total;
    entry."2" = day2Total;
    entry."3" = day3Total;
    entry."4" = day4Total;
    entry."5" = day5Total;
    entry."6" = day6Total;
    entry."phaseName" = "Totals";
    entry."workEffortId" = "Totals";
    entry."total" = day0Total + day1Total + day2Total + day3Total + day4Total + day5Total + day6Total;
    entries.add(entry);
}
context.timeEntries = entries;
// get all timesheets of this user, including the planned hours
timesheetsDb = delegator.findByAnd("Timesheet", ["partyId" : partyId], ["fromDate DESC"]);
timesheets = new LinkedList(); 
timesheetsDb.each { timesheetDb ->
    timesheet = FastMap.newInstance();
    timesheet.putAll(timesheetDb);
    entries = timesheetDb.getRelated("TimeEntry");
    hours = 0.00;
    entries.each { timeEntry ->
        if (timeEntry.hours) {
            hours += timeEntry.hours.doubleValue();
        }
    }
    timesheet.weekNumber = UtilDateTime.weekNumber(timesheetDb.fromDate);
    timesheet.hours = hours;
    timesheets.add(timesheet);
}
context.timesheets = timesheets;

//add task to Dropdown Lists
tasks = [];
orderByList = ["projectName", "phaseName", "workEffortName"];
projectPhaseTasks = [];
dataAdd = [];

if (!"mytasks".equals(headerItem)) {
	//assigned task to party
    tasks.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
    tasks.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_COMPLETED"));
    tasks.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_CANCELLED"));
    tasks.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_ON_HOLD"));
    taskCond = EntityCondition.makeCondition(tasks, EntityOperator.AND);
    projectPhaseTaskChecks = delegator.findList("ProjectPartyAndPhaseAndTask", taskCond, null, orderByList, null, false);
    projectPhaseTaskChecks.each { projectPhaseTask ->
    	taskPaertys = [];
        taskPaertys.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        taskPaertys.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, projectPhaseTask.workEffortId));
        taskPaertys.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAS_COMPLETED"));
        taskPaertyCond = EntityCondition.makeCondition (taskPaertys, EntityOperator.AND);
        projectPhaseTaskPartys = delegator.findList ("ProjectAndPhaseAndTaskParty", taskPaertyCond, null, orderByList, null, false);
        projectPhaseTaskPartys.each { check ->
        	ass = [];
            ass.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            ass.add(EntityCondition.makeCondition("workEffortId", EntityOperator.NOT_EQUAL, check.workEffortId));
            ass.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAS_ASSIGNED"));
            assCond = EntityCondition.makeCondition (ass, EntityOperator.AND);
            workEffortAssignments = delegator.findList("WorkEffortPartyAssignment", assCond, null, null, null, false);
            if (workEffortAssignments) {
                found = false;
                timeEntries.each { timeEntry ->
	                if (timeEntry.workEffortId.equals(check.workEffortId)) {
	                    found = true;
	                }
                }
                if (!found) {
                    projectPhaseTasks.add(check);
                }
            }
        }
    }
}
else{//Don't assign tasks
	tasksAss = [];
    tasksAss.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_COMPLETED"));
    tasksAss.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_CANCELED"));
    tasksAss.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.NOT_EQUAL, "PTS_ON_HOLD"));
    taskAssCond = EntityCondition.makeCondition(tasks, EntityOperator.AND);
    projectPhaseTaskChecks = delegator.findList("ProjectAndPhaseAndTask", taskAssCond, null, orderByList, null, false);
    projectPhaseTaskChecks.each { projectPhaseTaskCheck ->
        taskNotAs = [];
        workEffortAssignments = delegator.findList("WorkEffortPartyAssignment", null, null, null, null, false);
        found = false;
        workEffortAssignments.each { workEffortAssignment ->
                if (workEffortAssignment.workEffortId.equals(projectPhaseTaskCheck.workEffortId)) {
                    found = true;
                }
        }
        if (!found) {
            dataAdd.add(projectPhaseTaskCheck);
        }
    }
    dataAdd.each { dataCheck ->
        found = false;
        //Don't Inprogress tasks
        timeEntries.each { timeEntryAdd ->
            if (dataCheck.workEffortId.equals(timeEntryAdd.workEffortId)) {
                found = true;
            }
        }
        if (!found) {
            projectPhaseTasks.add(dataCheck);
        }
    }
}
if (projectPhaseTasks) {//Add task to lists
    context.projectTaskLists = projectPhaseTasks;
}
