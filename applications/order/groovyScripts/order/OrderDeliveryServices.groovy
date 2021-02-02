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


import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil


/**
 * Creates a new Purchase Order Schedule
 * @return
 */
def createOrderDeliverySchedule() {
    String checkAction = "CREATE"
    Map serviceResult = checkSupplierRelatedPermission("createOrderDeliverySchedule", checkAction, parameters.orderId)
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }

    GenericValue schedule = makeValue("OrderDeliverySchedule")
    schedule.setPKFields(parameters)
    if(!schedule.orderItemSeqId) {
        schedule.orderItemSeqId = "_NA_"
    }
    // only set statusId if hasScheduleAdminRelatedPermission
    schedule.setNonPKFields(parameters)
    if (!security.hasEntityPermission("ORDERMGR", ("_" + checkAction), parameters.userLogin)) {
        // no permission, set to initial
        schedule.statusId = "ODS_SUBMITTED"
    }
    schedule.create()
    return success()
}

/**
 * Updates an existing Purchase Order Schedule
 * @return
 */
def updateOrderDeliverySchedule() {
    // Verify the user is allowed to edit the fields
    String checkAction = "UPDATE"
    Map serviceResult = checkSupplierRelatedPermission("updateOrderDeliverySchedule", checkAction, parameters.orderId)
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }

    // Lookup the existing schedule to modify
    GenericValue schedule = from("OrderDeliverySchedule").where(parameters).queryOne()

    // only set statusId if hasScheduleAdminRelatedPermission
    String saveStatusId = schedule.statusId
    schedule.setNonPKFields(parameters)
    if (!security.hasEntityPermission("ORDERMGR", ("_" + checkAction), parameters.userLogin)) {
        schedule.statusId = saveStatusId
    }
    // Update the actual schedule
    schedule.store()
    return success()
}

def sendOrderDeliveryScheduleNotification() {
    String checkAction = "UPDATE"
    Map serviceResult = checkSupplierRelatedPermission("sendOrderDeliveryScheduleNotification", checkAction, parameters.orderId)
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    if (!parameters.orderItemSeqId) {
        parameters.orderItemSeqId = "_NA_"
    }
    GenericValue orderDeliverySchedule = from("OrderDeliverySchedule").where(parameters).queryOne()
    // find email address for currently logged in user, set as sendFrom
    Map curUserPcmFindMap = [partyId: userLogin.partyId, contactMechTypeId: "EMAIL_ADDRESS"]
    GenericValue curUserPartyAndContactMech = from("PartyAndContactMech").where(curUserPcmFindMap).queryFirst()
    Map sendEmailMap = [sendFrom: ("," + curUserPartyAndContactMech.infoString)]

    // find email addresses of all parties in SHIPMENT_CLERK roleTypeId, set as sendTo
    Map shipmentClerkFindMap = [roleTypeId: "SHIPMENT_CLERK"]
    List shipmentClerkRoles = from("PartyRole").where(shipmentClerkFindMap).queryList()
    Map sendToPartyIdMap = [:]
    for (GenericValue shipmentClerkRole : shipmentClerkRoles) {
        sendToPartyIdMap[shipmentClerkRole.partyId] = shipmentClerkRole.partyId
    }
    // go through all send to parties and get email addresses
    for (Map.Entry entry : sendToPartyIdMap) {
        Map sendToPartyPcmFindMap = [partyId: entry.getKey(), contactMechTypeId: "EMAIL_ADDRESS"]
        List sendToPartyPartyAndContactMechs = from("PartyAndContactMech").where(sendToPartyPcmFindMap).queryList()
        for (GenericValue sendToPartyPartyAndContactMech : sendToPartyPartyAndContactMechs) {
            StringBuilder newContact = new StringBuilder();
            if (sendEmailMap.sendTo) {
                newContact.append(sendEmailMap.sendTo)
            }
            newContact.append(",").append(sendToPartyPartyAndContactMech.infoString)
            sendEmailMap.sendTo = newContact.toString()
        }
    }
    // set subject, contentType, templateName, templateData
    sendEmailMap.subject = "Delivery Information Updated for Order #" + orderDeliverySchedule.orderId
    if (orderDeliverySchedule.orderItemSeqId != "_NA_") {
        StringBuilder newSubject = new StringBuilder()
        newSubject.append(sendEmailMap.subject)
        newSubject.append(" Item #" + orderDeliverySchedule.orderItemSeqId)
        sendEmailMap.subject = newSubject.toString()
    }
    sendEmailMap.contentType = "text/html"
    sendEmailMap.templateName = "component://order/template/email/OrderDeliveryUpdatedNotice.ftl"
    Map templateData = [orderDeliverySchedule: orderDeliverySchedule]
    sendEmailMap.templateData = templateData

    // call sendGenericNotificationEmail service, if enough information was found
    logInfo("Sending generic notification email (if all info is in place): ${sendEmailMap}")
    if (sendEmailMap.sendTo && sendEmailMap.sendFrom) {
        run service:"sendGenericNotificationEmail", with: sendEmailMap
    } else {
        logError("Insufficient data to send notice email: ${sendEmailMap}")
    }
    return success()
}

/**
 * Check Supplier Related Permission Service
 * @return
 */
def checkSupplierRelatedOrderPermissionService() {
    Map result = success()
    Map serviceResult = checkSupplierRelatedPermission(parameters.callingMethodName, parameters.checkAction, parameters.orderId)
    result.hasSupplierRelatedPermission = serviceResult.hasSupplierRelatedPermission
    return result
}

// Should be called in-line to use its out parameter indicating whether the user has permission or not.

/**
 * Check Supplier Related Permission
 * @return
 */
def checkSupplierRelatedPermission(String callingMethodName, String checkAction, String orderId) {
    Map result = success()
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", locale)
    }
    if (!checkAction) {
        checkAction = "UPDATE"
    }
    result.hasSupplierRelatedPermission = false
    if (security.hasEntityPermission("ORDERMGR", ("_" + checkAction), userLogin)) {
        result.hasSupplierRelatedPermission = true
    } else {
        Map lookupOrderRoleMap = [orderId: orderId, partyId: userLogin.partyId, roleTypeId: "SUPPLIER_AGENT"]
        GenericValue permOrderRole = from("OrderRole").where(lookupOrderRoleMap).queryOne()
        if (!permOrderRole) {
            result = error("ERROR: You do not have permission to ${checkAction} Delivery Schedule Information; you must be associated with this order as a Supplier Agent or have the ORDERMGR_${checkAction} permission.")
            result.hasSupplierRelatedPermission = false
        } else {
            result.hasSupplierRelatedPermission = true
        }
    }
    logInfo("hasSupplierRelatedPermission is: " + result.hasSupplierRelatedPermission)
    return result
}