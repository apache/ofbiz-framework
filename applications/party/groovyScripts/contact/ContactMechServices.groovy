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
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

/**
 * Update a Contact Mechanism
 */
def updateContactMech() {
    Map successMessageMap = [
            EMAIL_ADDRESS: 'EmailAddress',
            WEB_ADDRESS: 'WebAddress',
            IP_ADDRESS: 'IpAddress',
            ELECTRONIC_ADDRESS: 'ElectronicAddress',
            DOMAIN_NAME: 'DomainName',
            default: 'ContactMechanism'
    ]
    String successMessage = "Party" +
            (successMessageMap."${parameters.contactMechTypeId}" ?: successMessageMap.default) +
            "SuccessfullyUpdated"
    GenericValue lookedValue = from('ContactMech').where(parameters).queryOne()
    if (! lookedValue) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', locale))
    }
    if (lookedValue.infoString != parameters.infoString) {
        lookedValue.setNonPKFields(parameters)
        lookedValue.contactMechId = null
        Map serviceResult = run service: 'createContactMech', with: lookedValue.getAllFields()
        Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels', successMessage, locale))
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels', 'PartyNothingToDoHere', locale))
    serviceReturn.contactMechId = parameters.contactMechId
    return serviceReturn
}

/**
 * locale function to control if the state province is mandatoring
 * @param countryGeoId
 * @param stateProvinceGeoId
 * @return
 */
def hasValidStateProvince(String countryGeoId, String stateProvinceGeoId) {
    String errorMessage
    if (!stateProvinceGeoId) {
        if ('USA' == countryGeoId) errorMessage = 'PartyStateInUsMissing'
        if ('CAN' == countryGeoId) errorMessage = 'PartyProvinceInCanadaMissing'
    }
    return errorMessage
}

/**
 * Create Contact Mechanism with PostalAddress
 */
def createPostalAddress() {
    String errorMessage = hasValidStateProvince(parameters.countryGeoId, parameters.stateProvinceGeoId)
    if (errorMessage) {
        return error(UtilProperties.getMessage('PartyUiLabels', errorMessage, locale))
    }
    GenericValue newValue = makeValue('PostalAddress', parameters)
    Map createContactMechMap = [contactMechTypeId: 'POSTAL_ADDRESS', contactMechId: parameters.contactMechId]
    Map serviceResult = run service: 'createContactMech', with: createContactMechMap
    newValue.contactMechId = serviceResult.contactMechId
    newValue.create()
    Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels',
            'PartyPostalAddressSuccessfullyCreated', locale))
    serviceReturn.contactMechId = newValue.contactMechId
    return serviceReturn
}

/**
 * Update Contact Mechanism with PostalAddress
 */
def updatePostalAddress() {
    String errorMessage = hasValidStateProvince(parameters.countryGeoId, parameters.stateProvinceGeoId)
    if (errorMessage) {
        return error(UtilProperties.getMessage('PartyUiLabels', errorMessage, locale))
    }
    GenericValue lookedValue = from('PostalAddress').where(parameters).queryOne()
    if (! lookedValue) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', locale))
    }
    GenericValue newValue = makeValue('PostalAddress', parameters)
    String contactMechId
    String oldContactMechId = lookedValue.contactMechId
    String successMessage = 'PartyPostalAddressSuccessfullyUpdated'
    if (newValue.compareTo(lookedValue) != 0) {
        logInfo('Postal address need updating')
        Map createPostalAddressMap = [*:parameters]
        createPostalAddressMap.contactMechId = null
        Map serviceResult = run service: 'createPostalAddress', with: createPostalAddressMap
        contactMechId = serviceResult.contactMechId
    } else {
        Map serviceResult = run service: 'updateContactMech', with: parameters
        contactMechId = serviceResult.contactMechId
        if (contactMechId != oldContactMechId) {
            logInfo('Postal address need updating, contact mech changed')
            newValue.contactMechId = contactMechId
            newValue.create()
        } else {
            successMessage = 'PartyNothingToDoHere'
        }
    }

    Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels', successMessage, locale))
    serviceReturn.contactMechId = contactMechId
    serviceReturn.oldContactMechId = oldContactMechId
    return serviceReturn
}

/**
 * Create Contact Mechanism with Telecom Number
 */
def createTelecomNumber() {
    GenericValue newValue = makeValue('TelecomNumber', parameters)
    Map createContactMechMap = [contactMechTypeId: 'TELECOM_NUMBER', contactMechId: parameters.contactMechId]
    Map serviceResult = run service: 'createContactMech', with: createContactMechMap
    newValue.contactMechId = serviceResult.contactMechId
    newValue.create()
    Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels',
            'PartyTelecomNumberSuccessfullyCreated', locale))
    serviceReturn.contactMechId = newValue.contactMechId
    return serviceReturn
}

/**
 * Update Contact Mechanism with Telecom Number
 */
def updateTelecomNumber() {
    GenericValue lookedValue = from('TelecomNumber').where(parameters).queryOne()
    if (!lookedValue) {
        return error(UtilProperties.getMessage('ServiceErrorUiLabels', 'ServiceValueNotFound', locale))
    }
    GenericValue newValue = makeValue('TelecomNumber', parameters)
    String contactMechId
    String oldContactMechId = lookedValue.contactMechId
    String successMessage = 'PartyTelecomNumberSuccessfullyUpdated'
    if (newValue.compareTo(lookedValue) != 0) {
        logInfo('Telecom number need updating')
        Map createTelecomNumberMap = [*:parameters]
        createTelecomNumberMap.contactMechId = null
        Map serviceResult = run service: 'createTelecomNumber', with: createTelecomNumberMap
        contactMechId = serviceResult.contactMechId
    } else {
        Map serviceResult = run service: 'updateContactMech', with: parameters
        contactMechId = serviceResult.contactMechId
        if (contactMechId != oldContactMechId) {
            logInfo('Telecom number need updating, contact mech changed')
            newValue.contactMechId = contactMechId
            newValue.create()
        } else {
            successMessage = 'PartyNothingToDoHere'
        }
    }

    Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels', successMessage, locale))
    serviceReturn.contactMechId = contactMechId
    serviceReturn.oldContactMechId = oldContactMechId
    return serviceReturn
}

/**
 * Create an email address contact mechanism
 */
def createEmailAddress() {
    if (UtilValidate.isEmail(parameters.emailAddress)) {
        Map createContactMechMap = [contactMechTypeId: 'EMAIL_ADDRESS',
                                    contactMechId: parameters.contactMechId,
                                    infoString: parameters.emailAddress]
        Map serviceResult = run service: 'createContactMech', with: createContactMechMap
        Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels',
                'PartyEmailAddressSuccessfullyCreated', locale))
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', locale))
}

/**
 * Update an email address contact mechanism
 */
def updateEmailAddress() {
    if (UtilValidate.isEmail(parameters.emailAddress)) {
        Map updateContactMechMap = [contactMechTypeId: 'EMAIL_ADDRESS',
                                    contactMechId: parameters.contactMechId,
                                    infoString: parameters.emailAddress]
        Map serviceResult = run service: 'updateContactMech', with: updateContactMechMap
        Map serviceReturn = success(UtilProperties.getMessage('PartyUiLabels',
                'PartyEmailAddressSuccessfullyUpdated', locale))
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', locale))
}

/**
 * Create FtpAddress contact Mech
 */
def createFtpAddress() {
    Map contactMech = run service: 'createContactMech', with: [contactMechTypeId: 'FTP_ADDRESS']
    String contactMechId = contactMech.contactMechId
    if (contactMechId) {
        GenericValue ftpAddress = makeValue('FtpAddress', parameters)
        ftpAddress.contactMechId = contactMechId
        ftpAddress.create()
    } else return error('Error creating contactMech')

    Map resultMap = success()
    resultMap.contactMechId = contactMechId
    return resultMap
}

/**
 * Update FtpAddress contact Mech
 */
def updateFtpAddressWithHistory() {
    Map resultMap = success()
    resultMap.oldContactMechId = parameters.contactMechId
    resultMap.contactMechId = parameters.contactMechId
    Map newContactMechResult
    if (resultMap.oldContactMechId) {
        newValue = makeValue('FtpAddress', parameters)
        if (newValue != from('FtpAddress').where(parameters).queryOne()) {  // if there is some modifications in FtpAddress data
            newContactMechResult = run service: 'createFtpAddress', with: parameters
        } else { //update only contactMech
            Map updateContactMechMap = dispatcher.getDispatchContext().makeValidContext('updateContactMech', ModelService.IN_PARAM, parameters)
            updateContactMechMap.contactMechTypeId = 'FTP_ADDRESS'
            newContactMechResult = run service: 'updateContactMech', with: updateContactMechMap
        }

        if (!resultMap.oldContactMechId.equals(newContactMechResult.contactMechId)) {
            resultMap.put('contactMechId', newContactMechResult.contactMechId)
        }
    }
    return resultMap
}

/**
 * Create FtpAddress contact Mech and link it with given partyId
 * @return
 */
def createPartyFtpAddress() {
    Map contactMech = run service: 'createFtpAddress', with: parameters
    if (ServiceUtil.isError(contactMech)) return contactMech
    String contactMechId = contactMech.contactMechId

    Map createPartyContactMechMap = parameters
    createPartyContactMechMap.put('contactMechId', contactMechId)
    Map serviceResult = run service: 'createPartyContactMech', with: createPartyContactMechMap
    if (ServiceUtil.isError(serviceResult)) return serviceResult

    //TODO: manage purpose

    Map resultMap = success()
    resultMap.contactMechId = contactMechId
    return resultMap
}

def updatePartyFtpAddress() {
    Map updateFtpResult = run service: 'updateFtpAddressWithHistory', with: parameters
    Map result = success()
    result.contactMechId = parameters.contactMechId
    if (parameters.contactMechId != updateFtpResult.contactMechId) {
        Map updatePartyContactMechMap = dispatcher.getDispatchContext().makeValidContext('updatePartyContactMech', ModelService.IN_PARAM, parameters)
        updatePartyContactMechMap.newContactMechId = updateFtpResult.contactMechId
        updatePartyContactMechMap.contactMechTypeId = 'FTP_ADDRESS'
        run service: 'updatePartyContactMech', with: updatePartyContactMechMap
        result.contactMechId = updateFtpResult.contactMechId
    }
    return result
}

/**
 * Send an email to the person for Verification of his Email Address
 */
def sendVerifyEmailAddressNotification() {
    GenericValue storeEmail = from('ProductStoreEmailSetting')
            .where(emailType: 'PRDS_EMAIL_VERIFY')
            .cache()
            .queryFirst()
    GenericValue emailAddressVerification = from('EmailAddressVerification').where(parameters).queryOne()
    if (emailAddressVerification && storeEmail) {
        Map emailParams = [
            sendTo: parameters.emailAddress,
            subject: storeEmail.subject,
            sendFrom: storeEmail.fromAddress,
            sendCc: storeEmail.ccAddress,
            sendBcc: storeEmail.bccAddress,
            contentType: storeEmail.contentType,
            bodyParameters: [verifyHash: emailAddressVerification.verifyHash],
            bodyScreenUri: storeEmail.bodyScreenLocation]
        GenericValue webSite = from("WebSite")
                .where(productStoreId: storeEmail.productStoreId)
                .cache()
                .queryFirst()
        emailParams.webSiteId = webSite ? webSite.webSiteId : null
        run service: 'sendMailFromScreen', with: emailParams
    }
    return success()
}

/**
 * Verify an Email Address through verifyHash and expireDate
 */
def verifyEmailAddress() {
    GenericValue emailAddressVerification = from('EmailAddressVerification')
            .where(verifyHash: parameters.verifyHash)
            .queryFirst()
    if (! emailAddressVerification) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotExist', locale))
    }
    if (UtilValidate.isDateBeforeNow(emailAddressVerification.expireDate)) {
        return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressVerificationExpired', locale))
    }
    return success()
}