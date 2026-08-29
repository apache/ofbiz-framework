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
package org.apache.ofbiz.workeffort.workeffort.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.service.ServiceUtil
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class WorkEffortTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateWorkEffortAndPartyAssign() {
        String partyId = testParams.partyId ?: 'TestParty-1'
        String roleTypeId = testParams.roleTypeId ?: 'CAL_OWNER'
        String statusId = testParams.statusId ?: 'PRTYASGN_ASSIGNED'
        String workEffortId = testParams.workEffortId ?: 'TestWorkEffort-99'
        String partyTypeId = testParams.partyTypeId ?: 'PARTY_GROUP'
        String workEffortName = testParams.workEffortName ?: 'Test WorkEffort Event'
        String workEffortTypeId = testParams.workEffortTypeId ?: 'TASK'
        String currentStatusId = testParams.currentStatusId ?: 'CAL_ACCEPTED'
        Map serviceCtx = [
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                workEffortId: workEffortId,
                partyTypeId: partyTypeId,
                workEffortName: workEffortName,
                workEffortTypeId: workEffortTypeId,
                currentStatusId: currentStatusId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortAndPartyAssign', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   serviceResult.workEffortId,
                                                                                                   'partyId',
                                                                                                   partyId,
                                                                                                   'roleTypeId',
                                                                                                   roleTypeId).queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffortPartyAssignment
        assert workEffort.workEffortTypeId == workEffortTypeId
        assert workEffort.currentStatusId == currentStatusId
        assert workEffortPartyAssignment.statusId == statusId
    }

    @Test
    @Order(2)
    void testDeleteWorkEffort() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkEffort-98'
        String workEffortName = testParams.workEffortName ?: 'Delete Me'
        String workEffortTypeId = testParams.workEffortTypeId ?: 'TASK'
        String currentStatusId = testParams.currentStatusId ?: 'CAL_TENTATIVE'
        Map createCtx = [
                workEffortId: workEffortId,
                workEffortName: workEffortName,
                workEffortTypeId: workEffortTypeId,
                currentStatusId: currentStatusId,
                userLogin: userLogin,
        ]
        dispatcher.runSync('createWorkEffort', createCtx)

        Map serviceCtx = [
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        assert !workEffort
    }

    @Test
    @Order(3)
    void testCopyWorkEffort() {
        String sourceWorkEffortId = testParams.sourceWorkEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                sourceWorkEffortId: sourceWorkEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortName == 'New Test Workeffort'
    }

    @Test
    @Order(4)
    void testDuplicateWorkEffort() {
        String oldWorkEffortId = testParams.oldWorkEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                oldWorkEffortId: oldWorkEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('duplicateWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortName == 'New Test Workeffort'
    }

    @Test
    @Order(5)
    void testMakeCommunicationEventWorkEffort() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-1'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('makeCommunicationEventWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId
        assert serviceResult.communicationEventId

        GenericValue communicationEventWorkEff = from('CommunicationEventWorkEff').where('workEffortId',
                                                                                         serviceResult.workEffortId,
                                                                                         'communicationEventId',
                                                                                         serviceResult.communicationEventId).queryOne()
        assert communicationEventWorkEff
    }

    @Test
    @Order(6)
    void testAssignPartyToWorkEffort() {
        String partyId = testParams.partyId ?: 'TestParty'
        String roleTypeId = testParams.roleTypeId ?: 'CONTENT_AUTHOR'
        String statusId = testParams.statusId ?: 'PRTYASGN_ASSIGNED'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                workEffortId: workEffortId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('assignPartyToWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.fromDate

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         partyId,
                                                                                         'roleTypeId',
                                                                                         roleTypeId,
                                                                                         'workEffortId',
                                                                                         workEffortId,
                                                                                         'fromDate',
                                                                                         serviceResult.fromDate).queryOne()
        assert workEffortPartyAssignment
    }

    @Test
    @Order(7)
    void testUpdatePartyToWorkEffortAssignment() {
        String partyId = testParams.partyId ?: 'TestParty'
        String roleTypeId = testParams.roleTypeId ?: 'CUSTOMER'
        String statusId = testParams.statusId ?: 'PRTYASGN_ASSIGNED'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                workEffortId: workEffortId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updatePartyToWorkEffortAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         partyId,
                                                                                         'roleTypeId',
                                                                                         roleTypeId,
                                                                                         'workEffortId',
                                                                                         workEffortId,
                                                                                         'fromDate',
                                                                                         java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortPartyAssignment
        assert workEffortPartyAssignment.statusId == statusId
    }

    @Test
    @Order(8)
    void testDeletePartyToWorkEffortAssignment() {
        String partyId = testParams.partyId ?: 'TestParty'
        String roleTypeId = testParams.roleTypeId ?: 'ACCOUNTANT'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                partyId: partyId,
                roleTypeId: roleTypeId,
                workEffortId: workEffortId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('expireWorkEffortPartyAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         partyId,
                                                                                         'roleTypeId',
                                                                                         roleTypeId,
                                                                                         'workEffortId',
                                                                                         workEffortId,
                                                                                         'fromDate',
                                                                                         java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortPartyAssignment
        assert workEffortPartyAssignment.thruDate
    }

    @Test
    @Order(9)
    void testQuickAssignPartyToWorkEffort() {
        String quickAssignPartyId = testParams.quickAssignPartyId ?: 'TestCompany'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                quickAssignPartyId: quickAssignPartyId,
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('quickAssignPartyToWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   workEffortId,
                                                                                                   'partyId',
                                                                                                   quickAssignPartyId).queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        assert workEffortPartyAssignment
    }

    @Test
    @Order(10)
    void testQuickAssignPartyToWorkEffortWithRole() {
        String quickAssignPartyId = testParams.quickAssignPartyId ?: 'TestParty-1'
        String roleTypeId = testParams.roleTypeId ?: 'BILL_FROM_VENDOR'
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                quickAssignPartyId: quickAssignPartyId,
                roleTypeId: roleTypeId,
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('quickAssignPartyToWorkEffortWithRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   workEffortId,
                                                                                                   'partyId',
                                                                                                   quickAssignPartyId,
                                                                                                   'roleTypeId',
                                                                                                   roleTypeId).queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        assert workEffortPartyAssignment
    }

    @Test
    @Order(11)
    void testCreateWorkEffortNote() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        String noteInfo = testParams.noteInfo ?: 'This is test note.'
        Map serviceCtx = [
                workEffortId: workEffortId,
                noteInfo: noteInfo,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.noteId

        GenericValue workEffortNote = from('WorkEffortNote').where('workEffortId', workEffortId, 'noteId', serviceResult.noteId).queryOne()
        GenericValue noteData = from('NoteData').where('noteId', serviceResult.noteId).queryOne()
        assert workEffortNote
        assert noteData
        assert noteData.noteInfo == noteInfo
    }

    @Test
    @Order(12)
    void testUpdateWorkEffortNote() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        String noteId = testParams.noteId ?: 'TestNote-1'
        String internalNote = testParams.internalNote ?: 'Y'
        String noteInfo = testParams.noteInfo ?: 'This is updated test note.'
        Map serviceCtx = [
                workEffortId: workEffortId,
                noteId: noteId,
                internalNote: internalNote,
                noteInfo: noteInfo,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateWorkEffortNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortNote = from('WorkEffortNote').where('workEffortId', workEffortId, 'noteId', noteId).queryOne()
        GenericValue noteData = from('NoteData').where('noteId', noteId).queryOne()
        assert workEffortNote
        assert noteData
        assert noteData.noteInfo == noteInfo
    }

    @Test
    @Order(13)
    void testGetWorkEffort() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        Map serviceCtx = [
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('getWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffort
    }

    @Test
    @Order(14)
    void testCreateWorkEffortAssoc() {
        String workEffortIdFrom = testParams.workEffortIdFrom ?: 'TestWorkeffort-2'
        String workEffortIdTo = testParams.workEffortIdTo ?: 'TestWorkeffort-3'
        String workEffortAssocTypeId = testParams.workEffortAssocTypeId ?: 'ROUTING_COMPONENT'
        Map serviceCtx = [
                workEffortIdFrom: workEffortIdFrom,
                workEffortIdTo: workEffortIdTo,
                workEffortAssocTypeId: workEffortAssocTypeId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortAssoc = from('WorkEffortAssoc').where('workEffortIdFrom',
                                                                     workEffortIdFrom,
                                                                     'workEffortIdTo',
                                                                     workEffortIdTo,
                                                                     'workEffortAssocTypeId',
                                                                     workEffortAssocTypeId,
                                                                     'fromDate',
                                                                     java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortAssoc
    }

    @Test
    @Order(15)
    void testCopyWorkEffortAssocs() {
        String sourceWorkEffortId = testParams.sourceWorkEffortId ?: 'TestWorkeffort-2'
        String targetWorkEffortId = testParams.targetWorkEffortId ?: 'TestWorkeffort-4'
        Map serviceCtx = [
                sourceWorkEffortId: sourceWorkEffortId,
                targetWorkEffortId: targetWorkEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyWorkEffortAssocs', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortAssocList = from('WorkEffortAssoc').where('workEffortIdFrom', targetWorkEffortId).queryList()
        assert workEffortAssocList
    }

    @Test
    @Order(16)
    void testCreateWorkEffortKeyword() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-2'
        String keyword = testParams.keyword ?: 'new test keyword for workeffort'
        Map serviceCtx = [
                workEffortId: workEffortId,
                keyword: keyword,
                relevancyWeight: 1L,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortKeyword', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortKeyword = from('WorkEffortKeyword').where('workEffortId',
                                                                         workEffortId,
                                                                         'keyword',
                                                                         keyword).queryOne()
        assert workEffortKeyword
    }

    @Test
    @Order(17)
    void testDeleteWorkEffortKeyword() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-3'
        String keyword = testParams.keyword ?: 'test keyword'
        Map serviceCtx = [
                workEffortId: workEffortId,
                keyword: keyword,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffortKeyword', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortKeyword = from('WorkEffortKeyword').where('workEffortId', workEffortId, 'keyword', keyword).queryOne()
        assert !workEffortKeyword
    }

    @Test
    @Order(18)
    void testDeleteWorkEffortKeywords() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-2'
        Map serviceCtx = [
                workEffortId: workEffortId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffortKeywords', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortKeywordList = from('WorkEffortKeyword').where('workEffortId', workEffortId).queryList()
        assert !workEffortKeywordList
    }

    @Test
    @Order(19)
    void testCreateTimesheet() {
        String partyId = testParams.partyId ?: 'TestParty'
        String comments = testParams.comments ?: 'Test timesheet'
        String statusId = testParams.statusId ?: 'TIMESHEET_IN_PROCESS'
        Map serviceCtx = [
                partyId: partyId,
                comments: comments,
                statusId: statusId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timesheetId

        GenericValue timesheet = from('Timesheet').where('timesheetId', serviceResult.timesheetId).queryOne()
        assert timesheet
        assert timesheet.partyId == partyId
        assert timesheet.statusId == statusId
        assert timesheet.comments == comments
    }

    @Test
    @Order(20)
    void testUpdateTimesheet() {
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-2'
        String clientPartyId = testParams.clientPartyId ?: 'TestParty'
        String statusId = testParams.statusId ?: 'TIMESHEET_COMPLETED'
        Map serviceCtx = [
                timesheetId: timesheetId,
                clientPartyId: clientPartyId,
                statusId: statusId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timesheet = from('Timesheet').where('timesheetId', timesheetId).queryOne()
        assert timesheet
        assert timesheet.clientPartyId == clientPartyId
        assert timesheet.statusId == statusId
    }

    @Test
    @Order(21)
    void testDeleteTimesheet() {
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-3'
        Map serviceCtx = [
                timesheetId: timesheetId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timesheet = from('Timesheet').where('timesheetId', timesheetId).queryOne()
        assert !timesheet
    }

    @Test
    @Order(22)
    void testCreateTimesheets() {
        String comments = testParams.comments ?: 'Test timesheet for test parties'
        List partyIdList = ['TestParty', 'TestParty-1']
        Map serviceCtx = [
                partyIdList: partyIdList,
                comments: comments,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheets', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        for (String partyId : partyIdList) {
            List<GenericValue> timesheetList = from('Timesheet')
                .where('partyId', partyId, 'comments', comments)
                .queryList()
            assert timesheetList
        }
    }

    @Test
    @Order(23)
    void testCreateTimesheetForThisWeek() {
        String partyId = testParams.partyId ?: 'TestParty-1'
        String comments = testParams.comments ?: 'Test timesheet'
        Map serviceCtx = [
                partyId: partyId,
                comments: comments,
                requiredDate: java.sql.Timestamp.valueOf('2009-09-06 00:00:00.0'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheetForThisWeek', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timesheetId

        GenericValue timesheet = from('Timesheet').where('timesheetId', serviceResult.timesheetId).queryOne()
        assert timesheet
        assert timesheet.partyId == partyId
        assert timesheet.fromDate == java.sql.Timestamp.valueOf('2009-09-06 00:00:00.0')
    }

    @Test
    @Order(24)
    void testAddTimesheetToNewInvoice() {
        String partyId = testParams.partyId ?: 'TestParty-1'
        String partyIdFrom = testParams.partyIdFrom ?: 'TestCompany'
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-2'
        Map serviceCtx = [
                partyId: partyId,
                partyIdFrom: partyIdFrom,
                timesheetId: timesheetId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('addTimesheetToNewInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.invoiceId

        GenericValue invoice = from('Invoice').where('invoiceId', serviceResult.invoiceId).queryOne()
        assert invoice
        assert invoice.partyId == partyId
    }

    @Test
    @Order(25)
    void testCreateTimeEntry() {
        String workEffortId = testParams.workEffortId ?: 'TestWorkeffort-2'
        String comments = testParams.comments ?: 'Test Time Entry'
        Map serviceCtx = [
                workEffortId: workEffortId,
                comments: comments,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timeEntryId

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', serviceResult.timeEntryId).queryOne()
        assert timeEntry
        assert timeEntry.workEffortId == workEffortId
        assert timeEntry.comments == comments
    }

    @Test
    @Order(26)
    void testUpdateTimeEntry() {
        String timeEntryId = testParams.timeEntryId ?: 'TestTimeEntry-1'
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-4'
        Map serviceCtx = [
                timeEntryId: timeEntryId,
                timesheetId: timesheetId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', timeEntryId).queryOne()
        assert timeEntry
        assert timeEntry.timesheetId == timesheetId
    }

    @Test
    @Order(27)
    void testDeleteTimeEntry() {
        String timeEntryId = testParams.timeEntryId ?: 'TestTimeEntry-2'
        Map serviceCtx = [
                timeEntryId: timeEntryId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', timeEntryId).queryOne()
        assert !timeEntry
    }

    @Test
    @Order(28)
    void testCreateEventService() {
        String workEffortTypeId = testParams.workEffortTypeId ?: 'EVENT'
        String quickAssignPartyId = testParams.quickAssignPartyId ?: 'DemoCustomer'
        String workEffortName = testParams.workEffortName ?: 'Create Work Effort'
        String currentStatusId = testParams.currentStatusId ?: 'CAL_TENTATIVE'
        String workEffortName1 = testParams.workEffortName1 ?: 'Update an event'
        String currentStatusId1 = testParams.currentStatusId1 ?: 'CAL_ACCEPTED'
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Map createCtx = [
                workEffortTypeId: workEffortTypeId,
                quickAssignPartyId: quickAssignPartyId,
                workEffortName: workEffortName,
                currentStatusId: currentStatusId,
                userLogin: systemLogin,
        ]
        Map createResult = dispatcher.runSync('createWorkEffort', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String workEffortId = createResult.workEffortId
        assert workEffortId

        Map updateCtx = [
                workEffortId: workEffortId,
                workEffortTypeId: workEffortTypeId,
                workEffortName: workEffortName1,
                currentStatusId: currentStatusId1,
                userLogin: systemLogin,
        ]
        Map updateResult = dispatcher.runSync('updateWorkEffort', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortTypeId == workEffortTypeId
        assert workEffort.workEffortName == workEffortName1
        assert workEffort.currentStatusId == currentStatusId1
    }

    @Test
    @Order(29)
    void testCreateProjectService() {
        String workEffortTypeId = testParams.workEffortTypeId ?: 'PROJECT'
        String quickAssignPartyId = testParams.quickAssignPartyId ?: 'DemoCustomer'
        String workEffortName = testParams.workEffortName ?: 'Create a project'
        String currentStatusId = testParams.currentStatusId ?: 'CAL_TENTATIVE'
        String workEffortName1 = testParams.workEffortName1 ?: 'Update a project'
        String currentStatusId1 = testParams.currentStatusId1 ?: 'CAL_ACCEPTED'
        String noteParty = testParams.noteParty ?: 'DemoCustomer'
        String noteInfo = testParams.noteInfo ?: "This is a note for party '${noteParty}'"
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Map createCtx = [
                workEffortTypeId: workEffortTypeId,
                quickAssignPartyId: quickAssignPartyId,
                workEffortName: workEffortName,
                currentStatusId: currentStatusId,
                userLogin: systemLogin,
        ]
        Map createResult = dispatcher.runSync('createWorkEffort', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String workEffortId = createResult.workEffortId
        assert workEffortId

        Map updateCtx = [
                workEffortId: workEffortId,
                workEffortTypeId: workEffortTypeId,
                workEffortName: workEffortName1,
                currentStatusId: currentStatusId1,
                userLogin: systemLogin,
        ]
        Map updateResult = dispatcher.runSync('updateWorkEffort', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        Map noteCtx = [
                workEffortId: workEffortId,
                noteParty: noteParty,
                noteInfo: noteInfo,
                userLogin: systemLogin,
        ]
        Map noteResult = dispatcher.runSync('createWorkEffortNote', noteCtx)
        assert ServiceUtil.isSuccess(noteResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortTypeId == workEffortTypeId
        assert workEffort.workEffortName == workEffortName1
        assert workEffort.currentStatusId == currentStatusId1

        GenericValue noteData = from('NoteData').where('noteId', noteResult.noteId).queryOne()
        assert noteData
        assert noteData.noteParty == noteParty
        assert noteData.noteInfo == noteInfo
    }

    @Test
    @Order(30)
    void testGetTimeEntryRate() {
        String timeEntryId = testParams.timeEntryId ?: 'TestTimeEntry-3'
        String currencyUomId = testParams.currencyUomId ?: 'USD'
        Map serviceCtx = [
                timeEntryId: timeEntryId,
                currencyUomId: currencyUomId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('getTimeEntryRate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.rateAmount

        List<GenericValue> rateAmountList = from('RateAmount').where('partyId',
                                                                     'TestParty',
                                                                     'rateTypeId',
                                                                     'STANDARD',
                                                                     'rateCurrencyUomId',
                                                                     currencyUomId).queryList()
        GenericValue rateAmount = rateAmountList ? rateAmountList[0] : null
        assert rateAmount
    }

}
