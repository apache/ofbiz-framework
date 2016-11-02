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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime

// get all timesheets of all user, including the planned hours
timesheets = []
inputFields = [:]

if (!parameters.noConditionFind) {
    parameters.noConditionFind = "N"
}
inputFields.putAll(parameters)
performFindResults = runService('performFind', ["entityName": "Timesheet", "inputFields": inputFields, "orderBy": "fromDate DESC"])
if (performFindResults.listSize > 0) {
    timesheetsDb = performFindResults.listIt.getCompleteList()
    performFindResults.listIt.close()
    
    timesheetsDb.each { timesheetDb ->
        //get hours from EmplLeave
        findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true)
        leaveExprsList = []
        leaveExprsList.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, timesheetDb.fromDate))
        leaveExprsList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, timesheetDb.thruDate))
        leaveExprsList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, timesheetDb.partyId))
        emplLeaveList = from("EmplLeave").where(leaveExprsList).cursorScrollInsensitive().distinct().queryIterator()
        leaveHours = 0.00
        
        while ((emplLeaveMap = emplLeaveList.next())) {
            emplLeaveEntry = emplLeaveMap
            resultHour = runService('getPartyLeaveHoursForDate', 
                ["userLogin": parameters.userLogin, "partyId": emplLeaveEntry.partyId, "leaveTypeId": emplLeaveEntry.leaveTypeId, "fromDate": emplLeaveEntry.fromDate])
            if (resultHour) {
                leaveActualHours = resultHour.hours.doubleValue()
                leaveHours += leaveActualHours
            }
        }
        //get hours from TimeEntry
        timesheet = [:]
        timesheet.putAll(timesheetDb)
        entries = timesheetDb.getRelated("TimeEntry", null, null, false)
        hours = 0.00
        entries.each { timeEntry ->
            if (timeEntry.hours) {
                hours += timeEntry.hours.doubleValue()
            }
        }
        timesheet.weekNumber = UtilDateTime.weekNumber(timesheetDb.fromDate)
        timesheet.hours = hours + leaveHours
        timesheets.add(timesheet)
        emplLeaveList.close()
    }
    context.timesheets = timesheets
}
