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
package org.apache.ofbiz.workeffort.timesheet

import org.apache.ofbiz.entity.GenericValue

/**
 * Check permission to create, update or delete a Timesheet or a TimeEntry.
 * A user with WORKEFFORTMGR_<mainAction> (or WORKEFFORTMGR_ADMIN) may act on any record.
 * Any other user may only change or delete records of their own party, and may only add time
 * entries to their own timesheets. Such a user may also not approve a timesheet, link time to an
 * invoice, change invoiced time, or move a record to another party or another party's timesheet.
 * A time entry that is not on a timesheet may still be created for any party, as a manufacturing
 * production run declaration does for the worker.
 */
Map timesheetPermissionCheck() {
    boolean hasPermission = security.hasEntityPermission('WORKEFFORTMGR', "_${parameters.mainAction}", userLogin)
    if (!hasPermission && !parameters.invoiceId && !parameters.invoiceItemSeqId && !parameters.approvedByUserLoginId
            && parameters.statusId != 'TIMESHEET_APPROVED') {
        if (parameters.mainAction == 'CREATE' && !parameters.timesheetId) {
            hasPermission = true
        } else {
            GenericValue timeEntry = parameters.timeEntryId
                    ? from('TimeEntry').where(timeEntryId: parameters.timeEntryId).queryOne() : null
            GenericValue timesheet = parameters.timesheetId
                    ? from('Timesheet').where(timesheetId: parameters.timesheetId).queryOne() : null
            List<String> ownerPartyIds = [timeEntry?.partyId, timeEntry?.getRelatedOne('Timesheet', false)?.partyId,
                                          timesheet?.partyId, parameters.partyId].findAll { it }
            hasPermission = ownerPartyIds && ownerPartyIds.every { it == userLogin.partyId }
                    && !(parameters.timeEntryId && (!timeEntry || timeEntry.invoiceId))
                    && !(parameters.timesheetId && !timesheet)
        }
    }
    Map result = success([hasPermission: hasPermission])
    if (!hasPermission) {
        result.failMessage = label('WorkEffortUiLabels', 'WorkEffortTimesheetPermissionError', parameters)
    }
    return result
}
