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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue

def createSystemInfoNote() {
    parameters.noteParty = parameters.noteParty ?: userLogin.partyId
    GenericValue noteData = makeValue("NoteData", parameters)
    noteData.noteDateTime = UtilDateTime.nowTimestamp()
    noteData.noteName = "SYSTEMNOTE"
    noteData.setNextSeqId()
    noteData.create()
    return success()
}

def deleteSystemInfoNote() {
    GenericValue noteData = from("NoteData").where(noteId: parameters.noteId).queryOne()
    noteData.removeRelated('CustRequestItemNote')
    noteData.removeRelated('CustRequestNote')
    noteData.removeRelated('MarketingCampaignNote')
    noteData.removeRelated('OrderHeaderNote')
    noteData.removeRelated('PartyNote')
    noteData.removeRelated('QuoteNote')
    noteData.removeRelated('WorkEffortNote')
    noteData.remove()
    return success()
}

def deleteAllSystemNotes() {
    delegator.removeByAnd("NoteData", [noteParty: userLogin.partyId, noteName: "SYSTEMNOTE"])
    return success()
}

def getSystemInfoNotes() {
    List systemInfoNotes = from("NoteData")
            .where(noteParty: userLogin.partyId,
                    noteName: "SYSTEMNOTE")
            .orderBy("-noteDateTime")
            .queryList()
    if (systemInfoNotes) {
        return success(systemInfoNotes: systemInfoNotes)
    }
    return success()
}

def getLastSystemInfoNote() {
    Map result = success()
    List systemInfoNotes = from("NoteData")
            .where(noteParty: userLogin ? userLogin.partyId : "_NA_",
                    noteName: "SYSTEMNOTE")
            .orderBy("-noteDateTime")
            .queryList()
    if (systemInfoNotes) {
        result.lastSystemInfoNote1 = systemInfoNotes[0]
        if (systemInfoNotes.size() == 2) {
            result.lastSystemInfoNote2 = systemInfoNotes[1]
        } else if (systemInfoNotes.size() > 2) {
            result.lastSystemInfoNote2 = systemInfoNotes[1]
            result.lastSystemInfoNote3 = systemInfoNotes[2]
        }
    }
    return result
}

def getSystemInfoStatus() {
    List systemInfoStatus = []
    List comms = from("CommunicationEventAndRole")
            .where(partyId: userLogin.partyId,
                    statusId: "COM_ROLE_COMPLETED")
            .orderBy("-entryDate")
            .queryList()
    if (comms) {
        systemInfoStatus << [noteInfo: "Open communication events: " + comms.size(), noteDateTime: comms[0].entryDate]
    }

    List assigns = from("WorkEffortAndPartyAssign")
            .where(partyId: userLogin.partyId,
                    statusId: "PAS_ASSIGNED",
                    workEffortTypeId: "TASK")
            .orderBy("-fromDate")
            .filterByDate()
            .queryList()
    if (assigns) {
        systemInfoStatus << [noteInfo: "Assigned and not completed tasks: " + assigns.size(), noteDateTime: assigns[0].fromDate]
    }
    return success(systemInfoStatus: systemInfoStatus)
}