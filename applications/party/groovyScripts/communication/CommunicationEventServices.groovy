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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.service.ModelService

import java.sql.Timestamp

/**
 * Update a CommunicationEvent
 */
def updateCommunicationEvent() {

    GenericValue event = from("CommunicationEvent")
            .where(parameters)
            .queryOne()

    Map fieldsMap = [*:parameters]
    String newStatusId = null
    if (event.statusId != parameters.statusId) {
        newStatusId = parameters.statusId
        fieldsMap.statusId = event.statusId
    }

    // get partyId from email address if required
    if (!parameters.partyIdTo && parameters.contactMechIdTo) {

        GenericValue partyContactMech = from("PartyAndContactMech")
                .where(contactMechId: parameters.contactMechIdTo)
                .filterByDate()
                .queryFirst()

        fieldsMap.partyIdTo = partyContactMech ?
                partyContactMech.partyId : null
    }

    // if the from-party changed, change also the roles
    if (parameters.partyIdFrom
            && parameters.partyIdFrom != event.partyIdFrom) {

        // updating partyId from old:
        if (event.partyIdFrom) {
            GenericValue roleFrom = from("CommunicationEventRole")
                    .where([communicationEventId: event.communicationEventId,
                            partyId: event.partyIdFrom,
                            roleTypeId: "ORIGINATOR"])
                    .queryOne()
            roleFrom?.remove()
        }

        // add new role
        Map createCommEventRoleMap = [partyId: parameters.partyIdFrom]
        createCommEventRoleMap.contactMechPurposeTypeIdFrom = parameters.contactMechPurposeTypeIdFrom ?: ""

        Map getPartyEmailFrom = run service: "getPartyEmail", with: createCommEventRoleMap
        createCommEventRoleMap.contactMechId = getPartyEmailFrom.contactMechId
        createCommEventRoleMap.communicationEventId = event.communicationEventId
        createCommEventRoleMap.roleTypeId = "ORIGINATOR"

        run service: "createCommunicationEventRole", with: createCommEventRoleMap
        fieldsMap.contactMechIdFrom = createCommEventRoleMap.contactMechId
    }

    // if the to-party changed, change also the roles
    if (parameters.partyIdTo
            && parameters.partyIdTo != event.partyIdTo) {
        if (event.partyIdTo) {
            GenericValue roleTo = from("CommunicationEventRole")
                    .where([communicationEventId: event.communicationEventId,
                            partyId: event.partyIdto,
                            roleTypeId: "ADDRESSEE"])
                    .queryOne()
            roleTo?.remove()
        }
        // add new role
        Map createCommEventRoleMap = [partyId: parameters.partyIdTo]
        Map getPartyEmailTo = run service: "getPartyEmail", with: createCommEventRoleMap
        createCommEventRoleMap.contactMechId = getPartyEmailTo.contactMechId
        createCommEventRoleMap.communicationEventId = event.communicationEventId
        createCommEventRoleMap.roleTypeId = "ADDRESSEE"

        run service: "createCommunicationEventRole", with: createCommEventRoleMap
        fieldsMap.contactMechIdTo = createCommEventRoleMap.contactMechId
    }

    event.setNonPKFields(fieldsMap)
    event.store()

    if (newStatusId) {
        fieldsMap.statusId = newStatusId
        run service: "setCommunicationEventStatus", with: fieldsMap
    }

    return success()
}

/**
 * Delete a CommunicationEvent
 */
def deleteCommunicationEvent() {

    GenericValue event = from("CommunicationEvent")
            .where(parameters)
            .queryOne()

    // the service can be called multiple times because event can have several recipients
    // ignore if already deleted
    if (!event) {
        return success()
    }

    // remove related links to work effort and product
    event.removeRelated("CommunicationEventWorkEff")
    event.removeRelated("CommunicationEventProduct")

    List<GenericValue> contentAssocs = event.getRelated("CommEventContentAssoc", null, null, false)
    contentAssocs.each { contentAssoc ->
        contentAssoc.remove()
        //Delete content and dataresource too if requested
        if ("Y" == parameters.delContentDataResource) {
            List<GenericValue> contents = contentAssoc.getRelated("FromContent", null, null, false)
            contents.each { content ->
                content.removeRelated("ContentRole")
                content.removeRelated("ContentKeyword")

                List<GenericValue> relatedFromContentassocs = content.getRelated("FromContentAssoc", null, null, false)
                relatedFromContentassocs.each { relatedFromContentassoc ->
                    Map removeContentAndRelatedInmap = [contentId: relatedFromContentassoc.contentIdTo]
                    run service: "removeContentAndRelated", with: removeContentAndRelatedInmap
                }
                content.removeRelated("FromContentAssoc")

                List<GenericValue> relatedToContentassocs = content.getRelated("ToContentAssoc", null, null, false)
                relatedToContentassocs.each { relatedFromContentassoc ->
                    Map removeContentAndRelatedInmap = [contentId: relatedFromContentassoc.contentIdFrom]
                    run service: "removeContentAndRelated", with: removeContentAndRelatedInmap
                }
                content.removeRelated("ToContentAssoc")
                content.remove()

                // check first if the content is used on any other communication event if yes, only delete link
                List<GenericValue> commEvents = from("CommEventContentAssoc")
                        .where("contentId", content.contentId)
                        .queryList()

                if (commEvents && commEvents.size() == 1) {
                    Map removeContentAndRelatedInmap = [contentId: content.contentId]
                    run service: "removeContentAndRelated", with: removeContentAndRelatedInmap
                }
            }
        }
    }

    //delete the roles when exist and the event itself
    event.removeRelated("CommunicationEventRole")
    event.remove()

    return success()
}

/**
 * delete commEvent and workEffort
 */
def deleteCommunicationEventWorkEffort() {

    GenericValue event = from("CommunicationEvent")
            .where(parameters)
            .queryOne()

    // remove related workeffort when this is the only communicationevent connected to it
    List<GenericValue> workEffortComs = event.getRelated("CommunicationEventWorkEff", null, null, false)

    workEffortComs.each { workEffortCom ->
        workEffortCom.remove()
        GenericValue workEffort = workEffortCom.getRelatedOne("WorkEffort", false)

        List<GenericValue> otherComs = workEffort.getRelated("CommunicationEventWorkEff", null, null, false)

        if (!otherComs) {
            Debug.logInfo("remove workeffort ${workEffort.workEffortId} and related parties and status",
                    "CommunicationEventService.groovy")
            workEffort.removeRelated("WorkEffortPartyAssignment")
            workEffort.removeRelated("WorkEffortStatus")
            workEffort.removeRelated("WorkEffortKeyword")
            workEffort.remove()
        }
    }
    run service: "deleteCommunicationEvent", with: parameters
    return success()
}

/**
 * Create a CommunicationEventRole
 */
def createCommunicationEventRole() {

    // check if role already exist, then ignore
    GenericValue communicationEventRole =
            from("CommunicationEventRole")
                    .where(parameters)
                    .queryOne()

    if (!communicationEventRole) {
        GenericValue sysUserLogin = from("UserLogin").where(userLoginId: "system").queryOne()

        def partyRole = parameters
        partyRole.userLogin= sysUserLogin
        run service: "ensurePartyRole", with: partyRole

        GenericValue newEntity = makeValue("CommunicationEventRole")
        newEntity.setPKFields(parameters)
        newEntity.setNonPKFields(parameters)
        newEntity.statusId = parameters.statusId ?: 'COM_ROLE_CREATED'

        // if not provided get the latest contact mech id
        if (!newEntity.contactMechId) {
            GenericValue communicationEvent =
                    from("CommunicationEvent").where(communicationEventId: context.communicationEventId)
                            .queryOne()
            GenericValue communicationEventType = communicationEvent.getRelatedOne("CommunicationEventType")
            if (communicationEventType.contactMechTypeId) {
                GenericValue contactMech = from("PartyAndContactMech")
                        .where("partyId", newEntity.partyId,
                                "contactMechTypeId", communicationEventType.contactMechTypeId)
                        .orderBy("-fromDate")
                        .queryFirst()

                if (contactMech) {
                    newEntity.contactMechId = contactMechs[0]
                }
            }
        }
        newEntity.create()
        return success()
    }
    return success()
}

/**
 * Remove a CommunicationEventRole
 */
def removeCommunicationEventRole() {

    GenericValue eventRole = from("CommunicationEventRole")
            .where(parameters)
            .queryOne()

    if (eventRole) {
        eventRole.remove()

        if ("Y" == parameters.deleteCommEventIfLast
                && from("CommunicationEventRole")
                    .where("communicationEventId", eventRole.communicationEventId)
                    .queryCount() == 0) {
                run service: "deleteCommunicationEvent", with: parameters
        }
    }
    return success()
}

/**
 * Checks for email communication events with the status COM_IN_PROGRESS and a startdate which is expired,
 * then send the email
 */
def sendEmailDated() {
    Timestamp nowDate = UtilDateTime.nowTimestamp()
    EntityCondition conditions = EntityCondition.makeCondition([
            EntityCondition.makeCondition("statusId", "COM_IN_PROGRESS"),
            EntityCondition.makeCondition(EntityOperator.OR,
                    "communicationEventTypeId", "EMAIL_COMMUNICATION",
                    "communicationEventTypeId", "AUTO_EMAIL_COMM"),
            EntityCondition.makeCondition([
                    EntityCondition.makeCondition("datetimeStarted", EntityOperator.LESS_THAN, nowDate),
                    EntityCondition.makeCondition("datetimeStarted", EntityOperator.EQUALS, null),
            ], EntityOperator.OR)
    ])

    Map serviceContext = dispatcher.getDispatchContext().makeValidContext("sendCommEventAsEmail", ModelService.IN_PARAM, parameters)
    List<GenericValue> communicationEvents = from("CommunicationEvent").where(conditions).queryList()
    communicationEvents.each { communicationEvent ->
        // run service don't cover the new transaction need
        serviceContext.communicationEvent = communicationEvent
        dispatcher.runSync("sendCommEventAsEmail", serviceContext, 7200, true)
    }

    // sending of internal notes of a contactlist
    conditions = EntityCondition.makeCondition([
            EntityCondition.makeCondition("communicationEventTypeId", "COMMENT_NOTE"),
            EntityCondition.makeCondition("contactListId", EntityOperator.NOT_EQUAL, null),
            EntityCondition.makeCondition("statusId", "COM_IN_PROGRESS"),
            EntityCondition.makeCondition([
                    EntityCondition.makeCondition("datetimeStarted", EntityOperator.LESS_THAN, nowDate),
                    EntityCondition.makeCondition("datetimeStarted", EntityOperator.EQUALS, null),
            ], EntityOperator.OR)
    ])

    communicationEvents = from("CommunicationEvent").where(conditions).queryList()
    communicationEvents.each { communicationEvent ->

        List<GenericValue> contactListParties = from("ContactListParty")
                .where(contactListId: communicationEvent.contactListId)
                .queryList()

        contactListParties.each { contactListParty ->
            Map communicationEventRole = [communicationEventId: communicationEvent.communicationEventId,
                                          roleTypeId: "ADDRESSEE",
                                          partyId: contactListParty.partyId]
            run service: "createCommunicationEventRole", with: communicationEventRole
        }

        Map updCommEventStatusMap = [*:communicationEvent]
        updCommEventStatusMap.statusId = "COM_COMPLETE"
        run service: "setCommunicationEventStatus", with: updCommEventStatusMap
        return success()
    }
}

/**
 * Set The Communication Event Status
 */
def setCommunicationEventStatus() {

    GenericValue communicationEvent = from("CommunicationEvent")
            .where(parameters)
            .queryOne()
    oldStatusId = communicationEvent.statusId

    if (parameters.statusId != communicationEvent.statusId) {

        GenericValue statusChange = from("StatusValidChange")
                .where(statusId: communicationEvent.statusId,
                        statusIdTo: parameters.statusId)
                .queryOne()
        if (!statusChange) {
            Debug.logError("Cannot change from ${communicationEventRole.statusId} to ${parameters.statusId}",
                    "CommunicationEventServices.groovy")
            return error(UtilProperties.getMessage("ProductUiLabels",
                            "commeventservices.communication_event_status", parameters.locale as Locale))
        } else {
            communicationEvent.statusId = parameters.statusId
            communicationEvent.store()
            if ("COM_COMPLETE" == parameters.statusId) {
                if ("Y" == parameters.setRoleStatusToComplete) {
                    //if the status of the communicationevent is set to complete, all roles need to be set to complete,
                    //which means the commevent was dealt with and no further action is required by any
                    // of the other participants/addressees
                    List<GenericValue> roles = communicationEvent.getRelated("CommunicationEventRole", null, null, false)
                    roles.each { role ->
                        if ("COM_ROLE_COMPLETED" != role.statusId) {
                            role.statusId = "COM_ROLE_COMPLETED"
                            role.store()
                        }
                    }
                }
            } else { //make sure at least the senders role is set to complete

                GenericValue communicationEventRole =
                        from("CommunicationEventRole").where(
                                communicationEventId: communicationEvent.communicationEventId,
                                partyId: communicationEvent.partyIdFrom,
                                roleTypeId: "ORIGINATOR")
                                .queryOne()
                //found a mispelling in minilang so ...
                if (communicationEventRole
                        && !"COM_ROLE_COMPLETED" == communicationEventRole.statusId) {
                    Map updateRoleMap = [*:communicationEventRole]
                    updateRoleMap.statusId = "COM_ROLE_COMPLETED"
                    run service: "updateCommunicationEventRole", with: updateRoleMap
                }
            }
        }
    }
    return success()
}

//set the status for a particular party role to the status COM_ROLE_READ
def setCommEventRoleToRead() {
    if (!parameters.partyId) {
        parameters.partyId = parameters.userLogin.partyId
    }

    GenericValue eventRole
    if (!parameters.roleTypeId) {
        eventRole = from("CommunicationEventRole")
                .where(communicationEventId: parameters.communicationEventId,
                       partyId: parameters.partyId)
                .queryFirst()
            parameters.roleTypeId = eventRole.roleTypeId
    } else {
        eventRole = from("CommunicationEventRole")
                .where(parameters)
                .queryOne()
    }

    if (eventRole
            && "COM_ROLE_CREATED" == eventRole.statusId) {
        GenericValue userLogin = from("UserLogin").where(userLoginId: "system").queryOne()

        Map updStatMap = [*:parameters]
        updStatMap.statusId = "COM_ROLE_READ"
        updStatMap.userLogin = userLogin
        run service: "setCommunicationEventRoleStatus", with: updStatMap
    }

    return success()
}

//Set The Communication Event Status for a specific role
def setCommunicationEventRoleStatus() {

    GenericValue communicationEventRole = from("CommunicationEventRole")
            .where(parameters)
            .queryOne()

    oldStatusId = communicationEventRole.statusId
    if (parameters.statusId != communicationEventRole.statusId) {
        GenericValue statusChange = from("StatusValidChange")
                .where(statusId: communicationEventRole.statusId,
                        statusIdTo: parameters.statusId)
                .cache()
                .queryOne()
        if (!statusChange) {
            Debug.logError("Cannot change from ${communicationEventRole.statusId} to ${parameters.statusId}",
                    "CommunicationEventServices.groovy")
            return error(UtilProperties.getMessage("ProductUiLabels",
                            "commeventservices.communication_event_status", parameters.locale as Locale))
        } else {
            communicationEventRole.statusId = parameters.statusId
            communicationEventRole.store()
        }
    }
    return success()
}

//Create communication event and send mail to company
def sendContactUsEmailToCompany() {

    GenericValue systemUserLogin = from("UserLogin").where('userLoginId', 'system').cache().queryOne()
    Map contactUsMap = [*:parameters]
    contactUsMap.userLogin = systemUserLogin
    run service: "createCommunicationEventWithoutPermission", with: contactUsMap

    Map getPartyEmailMap = [partyId: parameters.partyIdTo, userLogin: systemUserLogin]
    Map getPartyEmailResult = run service: "getPartyEmail", with: getPartyEmailMap

    GenericValue productStoreEmailSetting = from("ProductStoreEmailSetting")
            .where(parameters)
            .queryOne()

    def bodyParameters = [partyId   : parameters.partyIdTo, email: parameters.emailAddress,
                          firstName : parameters.firstName, lastName: parameters.lastName,
                          postalCode: parameters.postalCode, countryCode: parameters.countryCode,
                          message   : parameters.content]

    if (productStoreEmailSetting.bodyScreenLocation) {
        Map emailParams = [bodyParameters: bodyParameters, userLogin: systemUserLogin]
        if (getPartyEmailResult.emailAddress) {
            emailParams.sendTo = getPartyEmailResult.emailAddress
        } else {
            emailParams.sendTo = productStoreEmailSetting.fromAddress
        }

        emailParams.subject = productStoreEmailSetting.subject
        emailParams.sendFrom = productStoreEmailSetting.fromAddress
        emailParams.contentType = productStoreEmailSetting.contentType
        emailParams.bodyScreenUri = productStoreEmailSetting.bodyScreenLocation

        run service: "sendMailFromScreen", with: emailParams
    }

    return success()
}
