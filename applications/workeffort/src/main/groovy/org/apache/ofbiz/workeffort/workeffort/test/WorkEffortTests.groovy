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
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil

class WorkEffortTests extends OFBizTestCase {

    WorkEffortTests(String name) {
        super(name)
    }

    void testCreateWorkEffortAndPartyAssign() {
        Map serviceCtx = [
                partyId: 'TestParty-1',
                roleTypeId: 'CAL_OWNER',
                statusId: 'PRTYASGN_ASSIGNED',
                workEffortId: 'TestWorkEffort-99',
                partyTypeId: 'PARTY_GROUP',
                workEffortName: 'Test WorkEffort Event',
                workEffortTypeId: 'TASK',
                currentStatusId: 'CAL_ACCEPTED',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortAndPartyAssign', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   serviceResult.workEffortId,
                                                                                                   'partyId',
                                                                                                   'TestParty-1',
                                                                                                   'roleTypeId',
                                                                                                   'CAL_OWNER').queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffortPartyAssignment
        assert workEffort.workEffortTypeId == 'TASK'
        assert workEffort.currentStatusId == 'CAL_ACCEPTED'
        assert workEffortPartyAssignment.statusId == 'PRTYASGN_ASSIGNED'
    }

    void testDeleteWorkEffort() {
        Map createCtx = [
                workEffortId: 'TestWorkEffort-98',
                workEffortName: 'Delete Me',
                workEffortTypeId: 'TASK',
                currentStatusId: 'CAL_TENTATIVE',
                userLogin: userLogin,
        ]
        dispatcher.runSync('createWorkEffort', createCtx)

        Map serviceCtx = [
                workEffortId: 'TestWorkEffort-98',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', 'TestWorkEffort-98').queryOne()
        assert !workEffort
    }

    void testCopyWorkEffort() {
        Map serviceCtx = [
                sourceWorkEffortId: 'TestWorkeffort-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortName == 'New Test Workeffort'
    }

    void testDuplicateWorkEffort() {
        Map serviceCtx = [
                oldWorkEffortId: 'TestWorkeffort-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('duplicateWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffortId

        GenericValue workEffort = from('WorkEffort').where('workEffortId', serviceResult.workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortName == 'New Test Workeffort'
    }

    void testMakeCommunicationEventWorkEffort() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-1',
                workEffortId: 'TestWorkeffort-3',
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

    void testAssignPartyToWorkEffort() {
        Map serviceCtx = [
                partyId: 'TestParty',
                roleTypeId: 'CONTENT_AUTHOR',
                statusId: 'PRTYASGN_ASSIGNED',
                workEffortId: 'TestWorkeffort-3',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('assignPartyToWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.fromDate

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         'TestParty',
                                                                                         'roleTypeId',
                                                                                         'CONTENT_AUTHOR',
                                                                                         'workEffortId',
                                                                                         'TestWorkeffort-3',
                                                                                         'fromDate',
                                                                                         serviceResult.fromDate).queryOne()
        assert workEffortPartyAssignment
    }

    void testUpdatePartyToWorkEffortAssignment() {
        Map serviceCtx = [
                partyId: 'TestParty',
                roleTypeId: 'CUSTOMER',
                statusId: 'PRTYASGN_ASSIGNED',
                workEffortId: 'TestWorkeffort-3',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updatePartyToWorkEffortAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         'TestParty',
                                                                                         'roleTypeId',
                                                                                         'CUSTOMER',
                                                                                         'workEffortId',
                                                                                         'TestWorkeffort-3',
                                                                                         'fromDate',
                                                                                         java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortPartyAssignment
        assert workEffortPartyAssignment.statusId == 'PRTYASGN_ASSIGNED'
    }

    void testDeletePartyToWorkEffortAssignment() {
        Map serviceCtx = [
                partyId: 'TestParty',
                roleTypeId: 'ACCOUNTANT',
                workEffortId: 'TestWorkeffort-3',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('expireWorkEffortPartyAssignment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortPartyAssignment = from('WorkEffortPartyAssignment').where('partyId',
                                                                                         'TestParty',
                                                                                         'roleTypeId',
                                                                                         'ACCOUNTANT',
                                                                                         'workEffortId',
                                                                                         'TestWorkeffort-3',
                                                                                         'fromDate',
                                                                                         java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortPartyAssignment
        assert workEffortPartyAssignment.thruDate
    }

    void testQuickAssignPartyToWorkEffort() {
        Map serviceCtx = [
                quickAssignPartyId: 'TestCompany',
                workEffortId: 'TestWorkeffort-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('quickAssignPartyToWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   'TestWorkeffort-3',
                                                                                                   'partyId',
                                                                                                   'TestCompany').queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        assert workEffortPartyAssignment
    }

    void testQuickAssignPartyToWorkEffortWithRole() {
        Map serviceCtx = [
                quickAssignPartyId: 'TestParty-1',
                roleTypeId: 'BILL_FROM_VENDOR',
                workEffortId: 'TestWorkeffort-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('quickAssignPartyToWorkEffortWithRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortPartyAssignmentList = from('WorkEffortPartyAssignment').where('workEffortId',
                                                                                                   'TestWorkeffort-3',
                                                                                                   'partyId',
                                                                                                   'TestParty-1',
                                                                                                   'roleTypeId',
                                                                                                   'BILL_FROM_VENDOR').queryList()
        GenericValue workEffortPartyAssignment = workEffortPartyAssignmentList ? workEffortPartyAssignmentList[0] : null
        assert workEffortPartyAssignment
    }

    void testCreateWorkEffortNote() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-3',
                noteInfo: 'This is test note.',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.noteId

        GenericValue workEffortNote = from('WorkEffortNote').where('workEffortId', 'TestWorkeffort-3', 'noteId', serviceResult.noteId).queryOne()
        GenericValue noteData = from('NoteData').where('noteId', serviceResult.noteId).queryOne()
        assert workEffortNote
        assert noteData
        assert noteData.noteInfo == 'This is test note.'
    }

    void testUpdateWorkEffortNote() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-3',
                noteId: 'TestNote-1',
                internalNote: 'Y',
                noteInfo: 'This is updated test note.',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateWorkEffortNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortNote = from('WorkEffortNote').where('workEffortId', 'TestWorkeffort-3', 'noteId', 'TestNote-1').queryOne()
        GenericValue noteData = from('NoteData').where('noteId', 'TestNote-1').queryOne()
        assert workEffortNote
        assert noteData
        assert noteData.noteInfo == 'This is updated test note.'
    }

    void testGetWorkEffort() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('getWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.workEffort
    }

    void testCreateWorkEffortAssoc() {
        Map serviceCtx = [
                workEffortIdFrom: 'TestWorkeffort-2',
                workEffortIdTo: 'TestWorkeffort-3',
                workEffortAssocTypeId: 'ROUTING_COMPONENT',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 02:02:02'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortAssoc = from('WorkEffortAssoc').where('workEffortIdFrom',
                                                                     'TestWorkeffort-2',
                                                                     'workEffortIdTo',
                                                                     'TestWorkeffort-3',
                                                                     'workEffortAssocTypeId',
                                                                     'ROUTING_COMPONENT',
                                                                     'fromDate',
                                                                     java.sql.Timestamp.valueOf('2009-09-09 02:02:02')).queryOne()
        assert workEffortAssoc
    }

    void testCopyWorkEffortAssocs() {
        Map serviceCtx = [
                sourceWorkEffortId: 'TestWorkeffort-2',
                targetWorkEffortId: 'TestWorkeffort-4',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyWorkEffortAssocs', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortAssocList = from('WorkEffortAssoc').where('workEffortIdFrom', 'TestWorkeffort-4').queryList()
        assert workEffortAssocList
    }

    void testCreateWorkEffortKeyword() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-2',
                keyword: 'new test keyword for workeffort',
                relevancyWeight: 1L,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createWorkEffortKeyword', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortKeyword = from('WorkEffortKeyword').where('workEffortId',
                                                                         'TestWorkeffort-2',
                                                                         'keyword',
                                                                         'new test keyword for workeffort').queryOne()
        assert workEffortKeyword
    }

    void testDeleteWorkEffortKeyword() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-3',
                keyword: 'test keyword',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffortKeyword', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue workEffortKeyword = from('WorkEffortKeyword').where('workEffortId', 'TestWorkeffort-3', 'keyword', 'test keyword').queryOne()
        assert !workEffortKeyword
    }

    void testDeleteWorkEffortKeywords() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-2',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteWorkEffortKeywords', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> workEffortKeywordList = from('WorkEffortKeyword').where('workEffortId', 'TestWorkeffort-2').queryList()
        assert !workEffortKeywordList
    }

    void testCreateTimesheet() {
        Map serviceCtx = [
                partyId: 'TestParty',
                comments: 'Test timesheet',
                statusId: 'TIMESHEET_IN_PROCESS',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timesheetId

        GenericValue timesheet = from('Timesheet').where('timesheetId', serviceResult.timesheetId).queryOne()
        assert timesheet
        assert timesheet.partyId == 'TestParty'
        assert timesheet.statusId == 'TIMESHEET_IN_PROCESS'
        assert timesheet.comments == 'Test timesheet'
    }

    void testUpdateTimesheet() {
        Map serviceCtx = [
                timesheetId: 'TestTimesheet-2',
                clientPartyId: 'TestParty',
                statusId: 'TIMESHEET_COMPLETED',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timesheet = from('Timesheet').where('timesheetId', 'TestTimesheet-2').queryOne()
        assert timesheet
        assert timesheet.clientPartyId == 'TestParty'
        assert timesheet.statusId == 'TIMESHEET_COMPLETED'
    }

    void testDeleteTimesheet() {
        Map serviceCtx = [
                timesheetId: 'TestTimesheet-3',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteTimesheet', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timesheet = from('Timesheet').where('timesheetId', 'TestTimesheet-3').queryOne()
        assert !timesheet
    }

    void testCreateTimesheets() {
        List partyIdList = ['TestParty', 'TestParty-1']
        Map serviceCtx = [
                partyIdList: partyIdList,
                comments: 'Test timesheet for test parties',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheets', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        for (String partyId : partyIdList) {
            List<GenericValue> timesheetList = from('Timesheet')
                .where('partyId', partyId, 'comments', 'Test timesheet for test parties')
                .queryList()
            assert timesheetList
        }
    }

    void testCreateTimesheetForThisWeek() {
        Map serviceCtx = [
                partyId: 'TestParty-1',
                comments: 'Test timesheet',
                requiredDate: java.sql.Timestamp.valueOf('2009-09-06 00:00:00.0'),
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimesheetForThisWeek', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timesheetId

        GenericValue timesheet = from('Timesheet').where('timesheetId', serviceResult.timesheetId).queryOne()
        assert timesheet
        assert timesheet.partyId == 'TestParty-1'
        assert timesheet.fromDate == java.sql.Timestamp.valueOf('2009-09-06 00:00:00.0')
    }

    void testAddTimesheetToNewInvoice() {
        Map serviceCtx = [
                partyId: 'TestParty-1',
                partyIdFrom: 'TestCompany',
                timesheetId: 'TestTimesheet-2',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('addTimesheetToNewInvoice', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.invoiceId

        GenericValue invoice = from('Invoice').where('invoiceId', serviceResult.invoiceId).queryOne()
        assert invoice
        assert invoice.partyId == 'TestParty-1'
    }

    void testCreateTimeEntry() {
        Map serviceCtx = [
                workEffortId: 'TestWorkeffort-2',
                comments: 'Test Time Entry',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.timeEntryId

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', serviceResult.timeEntryId).queryOne()
        assert timeEntry
        assert timeEntry.workEffortId == 'TestWorkeffort-2'
        assert timeEntry.comments == 'Test Time Entry'
    }

    void testUpdateTimeEntry() {
        Map serviceCtx = [
                timeEntryId: 'TestTimeEntry-1',
                timesheetId: 'TestTimesheet-4',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', 'TestTimeEntry-1').queryOne()
        assert timeEntry
        assert timeEntry.timesheetId == 'TestTimesheet-4'
    }

    void testDeleteTimeEntry() {
        Map serviceCtx = [
                timeEntryId: 'TestTimeEntry-2',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('deleteTimeEntry', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue timeEntry = from('TimeEntry').where('timeEntryId', 'TestTimeEntry-2').queryOne()
        assert !timeEntry
    }

    void testCreateEventService() {
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Map createCtx = [
                workEffortTypeId: 'EVENT',
                quickAssignPartyId: 'DemoCustomer',
                workEffortName: 'Create Work Effort',
                currentStatusId: 'CAL_TENTATIVE',
                userLogin: systemLogin,
        ]
        Map createResult = dispatcher.runSync('createWorkEffort', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String workEffortId = createResult.workEffortId
        assert workEffortId

        Map updateCtx = [
                workEffortId: workEffortId,
                workEffortTypeId: 'EVENT',
                workEffortName: 'Update an event',
                currentStatusId: 'CAL_ACCEPTED',
                userLogin: systemLogin,
        ]
        Map updateResult = dispatcher.runSync('updateWorkEffort', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortTypeId == 'EVENT'
        assert workEffort.workEffortName == 'Update an event'
        assert workEffort.currentStatusId == 'CAL_ACCEPTED'
    }

    void testCreateProjectService() {
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Map createCtx = [
                workEffortTypeId: 'PROJECT',
                quickAssignPartyId: 'DemoCustomer',
                workEffortName: 'Create a project',
                currentStatusId: 'CAL_TENTATIVE',
                userLogin: systemLogin,
        ]
        Map createResult = dispatcher.runSync('createWorkEffort', createCtx)
        assert ServiceUtil.isSuccess(createResult)
        String workEffortId = createResult.workEffortId
        assert workEffortId

        Map updateCtx = [
                workEffortId: workEffortId,
                workEffortTypeId: 'PROJECT',
                workEffortName: 'Update a project',
                currentStatusId: 'CAL_ACCEPTED',
                userLogin: systemLogin,
        ]
        Map updateResult = dispatcher.runSync('updateWorkEffort', updateCtx)
        assert ServiceUtil.isSuccess(updateResult)

        Map noteCtx = [
                workEffortId: workEffortId,
                noteParty: 'DemoCustomer',
                noteInfo: "This is a note for party 'DemoCustomer'",
                userLogin: systemLogin,
        ]
        Map noteResult = dispatcher.runSync('createWorkEffortNote', noteCtx)
        assert ServiceUtil.isSuccess(noteResult)

        GenericValue workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        assert workEffort
        assert workEffort.workEffortTypeId == 'PROJECT'
        assert workEffort.workEffortName == 'Update a project'
        assert workEffort.currentStatusId == 'CAL_ACCEPTED'

        GenericValue noteData = from('NoteData').where('noteId', noteResult.noteId).queryOne()
        assert noteData
        assert noteData.noteParty == 'DemoCustomer'
        assert noteData.noteInfo == "This is a note for party 'DemoCustomer'"
    }

    void testGetTimeEntryRate() {
        Map serviceCtx = [
                timeEntryId: 'TestTimeEntry-3',
                currencyUomId: 'USD',
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
                                                                     'USD').queryList()
        GenericValue rateAmount = rateAmountList ? rateAmountList[0] : null
        assert rateAmount
    }

}
