/**
 * Offers data clean up routines to programmatically clean up data in production via services.
 */


import com.simbaquartz.xcommon.services.contact.EmailTypesEnum
import com.simbaquartz.xcommon.services.contact.PhoneTypesEnum
import com.simbaquartz.xcrm.services.leads.LeadRoleTypesEnum
import com.simbaquartz.xparty.ContactMethodTypesEnum
import com.simbaquartz.xparty.helpers.ExtPartyRelationshipHelper
import com.simbaquartz.xparty.hierarchy.role.AccountRoles
import com.simbaquartz.xparty.services.PartyThreadsHelper
import com.simbaquartz.xparty.services.location.PostalAddressTypesEnum
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.party.party.PartyWorker
import org.apache.ofbiz.service.GenericServiceException
import org.apache.ofbiz.service.ServiceUtil

import java.sql.Timestamp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static java.util.stream.Collectors.toList

String module = "DataCleanUp.groovy"

/**
 * Cleans up email/phone/address for the profile, adds primary purpose if one doesn't exist.
 * Deletes empty records of phone numbers.
 * @return
 */
def syncPartyData() {
    String inputPartyId = parameters.partyId
    // get all parties
    Map<String, Object> inputMap = ["userLogin": userLogin]

    if (UtilValidate.isNotEmpty(inputPartyId)) {
        inputMap.put("partyId", inputPartyId)
    } else {
        inputMap.put("showAll", "Y")
        inputMap.put("lookupFlag", "Y")
        inputMap.put("VIEW_INDEX", "0")
        inputMap.put("VIEW_SIZE", "10000")
    }

    Map<String, Object> searchPartiesResp
    try {
        searchPartiesResp = dispatcher.runSync("findParty", inputMap)
    } catch (GenericServiceException e) {
        Debug.logError(e, module)
        return ServiceUtil.returnError(e.getMessage())
    }

    List<GenericValue> partyList = (List<GenericValue>) searchPartiesResp.get("partyList")

    Debug.logInfo("New method Found number of parties: " + partyList.size(), module)

    List<String> partyRecords = partyList.stream().map { it.getString("partyId") }.collect toList()

    partyRecords.each { partyId ->
        Debug.log("Sanitizing party id: " + partyId, module)
        // email check
        // check if party has a email
        GenericValue partyLatestContactMech =
                PartyWorker.findPartyLatestContactMech(
                        partyId, ContactMethodTypesEnum.EMAIL.getTypeId(), delegator)

        if (UtilValidate.isNotEmpty(partyLatestContactMech)) {
            Debug.log("Has email address: " + partyLatestContactMech, module)
            String emailContactMechId = partyLatestContactMech.getString("contactMechId")
            // check if this has a primary purpose set, if not, set it
            GenericValue partyPrimaryEmail = from("PartyContactMechPurpose").where(
                    "contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getLabel(),
                    "contactMechId", emailContactMechId
            ).queryFirst()

            if (UtilValidate.isEmpty(partyPrimaryEmail)) {
                Debug.log("Does not have primary email set", module)
                // email exists but primary purpose not set, set primary purpose
                Map makePrimaryContext = [
                        "userLogin"    : userLogin,
                        "partyId"      : partyId,
                        "contactMechId": emailContactMechId
                ]

                try {
                    dispatcher.runSync("makePrimaryEmail", makePrimaryContext)
                } catch (GenericServiceException e) {
                    Debug.logError(e, "DataCleanUp.groovy")
                }
            } else {
                Debug.log("Has primary email set, doing nothing", module)
            }
        } else {
            Debug.log("Does not have an email doing nothing", module)
        }
        // phone check
        // check if party has a phone
        GenericValue partyLatestPhoneContactMech =
                PartyWorker.findPartyLatestContactMech(
                        partyId, ContactMethodTypesEnum.PHONE.getTypeId(), delegator)

        if (UtilValidate.isNotEmpty(partyLatestPhoneContactMech)) {
            String phoneContactMechId = partyLatestPhoneContactMech.getString("contactMechId")

            GenericValue partyContactMechToDelete = from("PartyContactMech").where("partyId", partyId,
                    "contactMechId", phoneContactMechId).queryFirst()
            GenericValue phoneToDelete = from("TelecomNumber").where("contactMechId", phoneContactMechId).queryFirst()
            if (UtilValidate.isEmpty(phoneToDelete)) {
                // dirty data, clean up
                delegator.removeValue(partyContactMechToDelete)
            } else {

                // special check to delete phone numbers where areaCode and contactNumber is null
                if (UtilValidate.isEmpty(phoneToDelete.getString("areaCode")) &&
                        UtilValidate.isEmpty(phoneToDelete.getString("contactNumber"))
                ) {
                    Debug.log("Found empty phone number, deleting.")
                    delegator.removeValue(phoneToDelete)
                    delegator.removeValue(partyContactMechToDelete)
                }
            }
            // check if this has a primary purpose set, if not, set it
            GenericValue partyPrimaryPhone = from("PartyContactMechPurpose").where(
                    "contactMechPurposeTypeId", PhoneTypesEnum.PRIMARY.getLabel(),
                    "contactMechId", phoneContactMechId
            ).queryFirst()

            if (UtilValidate.isEmpty(partyPrimaryPhone)) {
                // phone exists but primary purpose not set, set primary purpose
                Map makePrimaryContext = [
                        "userLogin"    : userLogin,
                        "partyId"      : partyId,
                        "contactMechId": phoneContactMechId
                ]

                try {
                    dispatcher.runSync("makePrimaryPhone", makePrimaryContext)
                } catch (GenericServiceException e) {
                    Debug.logError(e, "DataCleanUp.groovy")
                }
            }
        }

        // address check
        // check if party has a address
        GenericValue partyLatestAddressContactMech =
                PartyWorker.findPartyLatestContactMech(
                        partyId, ContactMethodTypesEnum.ADDRESS.getTypeId(), delegator)

        if (UtilValidate.isNotEmpty(partyLatestAddressContactMech)) {
            // check if this has a primary purpose set, if not, set it
            String addressContactMechId = partyLatestAddressContactMech.getString("contactMechId")

            GenericValue partyPrimaryEmail = from("PartyContactMechPurpose").where(
                    "contactMechPurposeTypeId", PostalAddressTypesEnum.PRIMARY.getLabel(),
                    "contactMechId", addressContactMechId
            ).queryFirst()

            if (UtilValidate.isEmpty(partyPrimaryEmail)) {
                // address exists but primary purpose not set, set primary purpose
                Map makePrimaryContext = [
                        "userLogin"    : userLogin,
                        "partyId"      : partyId,
                        "contactMechId": addressContactMechId,
                ]

                try {
                    dispatcher.runSync("makePrimaryAddress", makePrimaryContext)
                } catch (GenericServiceException e) {
                    Debug.logError(e, "DataCleanUp.groovy")
                }
            }
        }
    }

    Debug.logInfo("Finished all threads for parties " + partyRecords.size(), module)

    return success()
}

/**
 * Runs and invokes populateBasicUserInformation for all parties.
 * @return
 */
def syncPersonData() {
    String inputPartyId = parameters.partyId
    // get all parties
    Map<String, Object> inputMap = ["userLogin": userLogin]

    if (UtilValidate.isNotEmpty(inputPartyId)) {
        inputMap.put("partyId", inputPartyId)
    } else {
        inputMap.put("showAll", "Y")
        inputMap.put("lookupFlag", "Y")
        inputMap.put("VIEW_INDEX", "0")
        inputMap.put("VIEW_SIZE", "10000")
    }

    Map<String, Object> searchPartiesResp
    try {
        searchPartiesResp = dispatcher.runSync("findParty", inputMap)
    } catch (GenericServiceException e) {
        Debug.logError(e, module)
        return ServiceUtil.returnError(e.getMessage())
    }

    List<GenericValue> partyList = (List<GenericValue>) searchPartiesResp.get("partyList")

    Debug.logInfo("Found number of parties: " + partyList.size(), module)

    ExecutorService executor = Executors.newFixedThreadPool(30)

    List<String> partyRecords = partyList.stream().map { it.getString("partyId") }.collect toList()

    partyRecords.each { partyIdRecord ->
        Runnable worker = new PartyThreadsHelper(partyIdRecord, delegator, userLogin, dispatcher)
        executor.execute(worker)
    }
    executor.shutdown()
    // Wait until all threads are finish
    while (!executor.isTerminated()) {

    }

    Debug.logInfo("Finished all threads for parties " + partyRecords.size(), module)

    return success()
}

/**
 * Changes the lead owner relationship for the account form LEAD_ACCOUNT_OWNER  to CONTACT_OWNER
 * @return
 */
def syncLeadAccountRelationship() {
    String partyId = parameters.partyId
    // fetch all parties (partyIdFrom) with partyRelationShipTypeId as LEAD_OWNER
    String leadOwnerRelationshipTypeId = "LEAD_OWNER_ACCNT"
    String newLeadOwnerRelationshipTypeId = AccountRoles.CONTACT_OWNER.getPartyRelationshipTypeId()
    String oldRoleTypeIdTo = LeadRoleTypesEnum.LEAD.getPartyRelationshipTypeId()
    String oldRoleTypeIdFrom = AccountRoles.OWNER.getPartyRelationshipTypeId()
    List<Map> leadRecords
    if (UtilValidate.isNotEmpty(partyId)) {
        leadRecords = ExtPartyRelationshipHelper.getActivePartyRelationshipsFromParty(dispatcher, null, partyId, leadOwnerRelationshipTypeId, null)
    } else {
        leadRecords = ExtPartyRelationshipHelper.getActivePartyRelationshipsOfType(dispatcher, leadOwnerRelationshipTypeId)
    }

    Debug.log("leadRecords found: " + leadRecords.size(), module)
    List<GenericValue> updatedRelationships = []
    List<GenericValue> oldRelationshipsToDelete= []
    // for each lead record find owner for
    leadRecords.each { leadRecord ->
        String leadPartyId = leadRecord.get("partyIdTo")
        String leadOwnedByAccountPartyId = leadRecord.get("partyIdFrom")
        Debug.log("Cleaning up lead id : " + leadPartyId, module)

        // fetch the PartyRelationship record by primary key
        Map criteriaMap = ["partyIdTo": leadPartyId,
        "partyIdFrom": leadOwnedByAccountPartyId,
        "roleTypeIdTo": oldRoleTypeIdTo,
        "roleTypeIdFrom": oldRoleTypeIdFrom,
        "fromDate": leadRecord.get("fromDate"),
        "partyRelationshipTypeId": leadOwnerRelationshipTypeId]

        Debug.log("criteriaMap : " + criteriaMap, module)

        GenericValue relationShipToUpdate = from("PartyRelationship").where(criteriaMap).queryOne()

        if(relationShipToUpdate){
            GenericValue newRelationship = relationShipToUpdate.clone()
            newRelationship.set("partyRelationshipTypeId", newLeadOwnerRelationshipTypeId)
            updatedRelationships.add(newRelationship)
            oldRelationshipsToDelete.add(relationShipToUpdate)
        }else{
            Debug.log("Relationship not found...")

        }
        Debug.log("Lead owner account id : " + leadOwnedByAccountPartyId, module)

    }

    // update records
    if(UtilValidate.isNotEmpty(updatedRelationships)){
        Debug.log("Relationships to store :" +  updatedRelationships, module)
        delegator.storeAll(updatedRelationships)
        // Debug.log("Relationships to remove :" +  oldRelationshipsToDelete, module)
//        delegator.removeAll(oldRelationshipsToDelete)
    }


    return success("Lead relationships # " + leadRecords.size() + " have been set up successfully!")
}

/**
 * Updates the TaskContent entries and populates the attachedByPartyId, lastModifiedByPartyId and  lastModifiedByUserLogin based on attachedByUserLoginId.
 * @return
 */
def sanitizeTaskContent() {
    String taskId = parameters.taskId
    // fetch all task contents.
    List<GenericValue> taskContents
    if (UtilValidate.isNotEmpty(taskId)) {
        taskContents = from("TaskContent").where("taskId", taskId).queryList()
    } else {
        taskContents = from("TaskContent").queryList()
    }

    Debug.log("task content found: " + taskContents.size(), module)
    // for each lead record find owner for
    taskContents.each { taskContent ->
        String userLoginId = taskContent.get("attachedByUserLogin")
        Debug.log("sanitizing taskId id : " + taskContent.get("taskId"), module)
        // Fetch party id from user login id.
        GenericValue userLogin = from("UserLogin").where("userLoginId", userLoginId).queryOne()
        attachedByPartyId = (String) userLogin.get("partyId")
        Debug.log("party id for the userLogin is: " + attachedByPartyId, module)

        // set the attachedByPartyId field in TaskContent entity.
        taskContent.set("attachedByPartyId", attachedByPartyId)
        taskContent.set("lastModifiedByPartyId", attachedByPartyId)
        taskContent.set("lastModifiedByUserLogin", userLoginId)

    }

    if (UtilValidate.isNotEmpty(taskContents))
        delegator.storeAll(taskContents)

    return success("Task contents have been updated successfully!")
}

/**
 * Sanitizes timesheets.
 * - Checks if linked task has a project id, also associates it with the TimeSheet.projectId
 * @return
 */
def sanitizeTimesheet() {
    String timesheetId = parameters.timesheetId
    // fetch all timesheet records.
    List<GenericValue> timeSheetRecords
    if (UtilValidate.isNotEmpty(timesheetId)) {
        timesheetRecords = from("Timesheet").where("timesheetId", timesheetId).queryList()
    } else {
        timesheetRecords = from("Timesheet").queryList()
    }

    Debug.log("timesheet records found: " + timesheetRecords.size(), module)
    // Iterate each timesheet record
    timesheetRecords.each { timesheet ->
        Debug.log("sanitizing timesheet id : " + timesheet.get("timesheetId"), module)

        Timestamp workLogDate = timesheet.getTimestamp("workLogDate")
        Long workInMillSec = timesheet.getLong("workInMilliSec")

        if (UtilValidate.isNotEmpty(workLogDate) && UtilValidate.isNotEmpty(workInMillSec)) {
            Long startDate = workLogDate.getTime() - workInMillSec
            Timestamp startDateTs = new Timestamp(startDate)

            timesheet.set("startDate", startDateTs)
        }

        //check if timesheet linked task has a project id associated, if so populate the client id and project id.
        String linkedTaskId = timesheet.getString("taskId")
        String linkedProjectId = timesheet.getString("projectId")

        if (linkedTaskId && !linkedProjectId) {
            GenericValue taskDetails = from("TaskHeader").where("taskId", linkedTaskId).queryOne()
            if (taskDetails) {
                // check if task has a project, if so add the project to timesheet
                String taskProjectId = taskDetails.getString("projectId")
                if (UtilValidate.isNotEmpty(taskProjectId)) {
                    Debug.log("associating project id : " + taskProjectId + " for task id: " + linkedTaskId, module)
                    timesheet.set("projectId", taskProjectId)
                }
            }

        }
    }

    if (UtilValidate.isNotEmpty(timesheetRecords))
        delegator.storeAll(timesheetRecords)

    return success("Timesheet have been updated successfully!")
}