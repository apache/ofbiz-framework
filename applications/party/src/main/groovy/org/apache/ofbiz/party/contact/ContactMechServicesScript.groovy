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
package org.apache.ofbiz.party.contact

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

import org.apache.ofbiz.base.crypto.HashCrypt
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.party.party.PartyHelper
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.webapp.WebAppUtil
import org.xml.sax.SAXException

/**
 * Update a Contact Mechanism
 */
Map updateContactMech() {
    Map successMessageMap = [
            EMAIL_ADDRESS: 'EmailAddress',
            WEB_ADDRESS: 'WebAddress',
            IP_ADDRESS: 'IpAddress',
            ELECTRONIC_ADDRESS: 'ElectronicAddress',
            DOMAIN_NAME: 'DomainName',
            default: 'ContactMechanism'
    ]
    GenericValue lookedValue = from('ContactMech').where('contactMechId', parameters.contactMechId).queryOne()
    require(lookedValue as boolean, 'ServiceErrorUiLabels', 'ServiceValueNotFound')
    String contactMechTypeId = parameters.contactMechTypeId ?: lookedValue.contactMechTypeId
    String successMessage = 'Party' +
            (successMessageMap."$contactMechTypeId" ?: successMessageMap.default) +
            'SuccessfullyUpdated'
    if (lookedValue.infoString != parameters.infoString) {
        lookedValue.setNonPKFields(parameters)
        lookedValue.contactMechId = null
        Map serviceResult = run service: 'createContactMech', with: lookedValue.getAllFields()
        Map serviceReturn = success('PartyUiLabels', successMessage)
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    Map serviceReturn = success('PartyUiLabels', 'PartyNothingToDoHere')
    serviceReturn.contactMechId = parameters.contactMechId
    return serviceReturn
}

/**
 * locale function to control if the state province is mandatoring
 */
String hasValidStateProvince(String countryGeoId, String stateProvinceGeoId) {
    String errorMessage
    if (!stateProvinceGeoId) {
        if (countryGeoId == 'USA') {
            errorMessage = 'PartyStateInUsMissing'
        }
        if (countryGeoId == 'CAN') {
            errorMessage = 'PartyProvinceInCanadaMissing'
        }
    }
    return errorMessage
}

/**
 * Create Contact Mechanism with PostalAddress
 */
Map createPostalAddress() {
    String errorMessage = hasValidStateProvince(parameters.countryGeoId, parameters.stateProvinceGeoId)
    boolean isStateProvinceValid = !errorMessage
    require(isStateProvinceValid, 'PartyUiLabels', errorMessage)
    GenericValue newValue = makeValue('PostalAddress', parameters)
    Map createContactMechMap = [contactMechTypeId: 'POSTAL_ADDRESS', contactMechId: parameters.contactMechId]
    Map serviceResult = run service: 'createContactMech', with: createContactMechMap
    newValue.contactMechId = serviceResult.contactMechId
    newValue.create()
    Map serviceReturn = success('PartyUiLabels', 'PartyPostalAddressSuccessfullyCreated')
    serviceReturn.contactMechId = newValue.contactMechId
    return serviceReturn
}

/**
 * Update Contact Mechanism with PostalAddress
 */
Map updatePostalAddress() {
    String errorMessage = hasValidStateProvince(parameters.countryGeoId, parameters.stateProvinceGeoId)
    boolean isStateProvinceValid = !errorMessage
    require(isStateProvinceValid, 'PartyUiLabels', errorMessage)
    GenericValue lookedValue = from('PostalAddress').where('contactMechId', parameters.contactMechId).queryOne()
    require(lookedValue as boolean, 'ServiceErrorUiLabels', 'ServiceValueNotFound')
    GenericValue newValue = (GenericValue) lookedValue.clone()
    newValue.setNonPKFields(parameters)
    String contactMechId
    String oldContactMechId = lookedValue.contactMechId
    String successMessage = 'PartyPostalAddressSuccessfullyUpdated'
    if (newValue != lookedValue) {
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

    Map serviceReturn = success('PartyUiLabels', successMessage)
    serviceReturn.contactMechId = contactMechId
    serviceReturn.oldContactMechId = oldContactMechId
    return serviceReturn
}

/**
 * Create Contact Mechanism with Telecom Number
 */
Map createTelecomNumber() {
    GenericValue newValue = makeValue('TelecomNumber', parameters)
    Map createContactMechMap = [contactMechTypeId: 'TELECOM_NUMBER', contactMechId: parameters.contactMechId]
    Map serviceResult = run service: 'createContactMech', with: createContactMechMap
    newValue.contactMechId = serviceResult.contactMechId
    newValue.create()
    Map serviceReturn = success('PartyUiLabels', 'PartyTelecomNumberSuccessfullyCreated')
    serviceReturn.contactMechId = newValue.contactMechId
    return serviceReturn
}

/**
 * Update Contact Mechanism with Telecom Number
 */
Map updateTelecomNumber() {
    GenericValue lookedValue = from('TelecomNumber').where('contactMechId', parameters.contactMechId).queryOne()
    require(lookedValue as boolean, 'ServiceErrorUiLabels', 'ServiceValueNotFound')
    GenericValue newValue = (GenericValue) lookedValue.clone()
    newValue.setNonPKFields(parameters)
    String contactMechId
    String oldContactMechId = lookedValue.contactMechId
    String successMessage = 'PartyTelecomNumberSuccessfullyUpdated'
    if (newValue != lookedValue) {
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

    Map serviceReturn = success('PartyUiLabels', successMessage)
    serviceReturn.contactMechId = contactMechId
    serviceReturn.oldContactMechId = oldContactMechId
    return serviceReturn
}

/**
 * Create an email address contact mechanism
 */
Map createEmailAddress() {
    require(parameters.emailAddress as boolean, 'PartyUiLabels', 'PartyEmailAddressMissing')
    if (UtilValidate.isEmail(parameters.emailAddress)) {
        Map createContactMechMap = [contactMechTypeId: 'EMAIL_ADDRESS',
                                    contactMechId: parameters.contactMechId,
                                    infoString: parameters.emailAddress]
        Map serviceResult = run service: 'createContactMech', with: createContactMechMap
        Map serviceReturn = success('PartyUiLabels', 'PartyEmailAddressSuccessfullyCreated')
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    return error('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly')
}

/**
 * Update an email address contact mechanism
 */
Map updateEmailAddress() {
    require(parameters.emailAddress as boolean, 'PartyUiLabels', 'PartyEmailAddressMissing')
    if (UtilValidate.isEmail(parameters.emailAddress)) {
        Map updateContactMechMap = [contactMechTypeId: 'EMAIL_ADDRESS',
                                    contactMechId: parameters.contactMechId,
                                    infoString: parameters.emailAddress]
        Map serviceResult = run service: 'updateContactMech', with: updateContactMechMap
        Map serviceReturn = success('PartyUiLabels', 'PartyEmailAddressSuccessfullyUpdated')
        serviceReturn.contactMechId = serviceResult.contactMechId
        return serviceReturn
    }
    return error('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly')
}

/**
 * Create FtpAddress contact Mech
 */
Map createFtpAddress() {
    Map contactMech = run service: 'createContactMech', with: [contactMechTypeId: 'FTP_ADDRESS']
    String contactMechId = contactMech.contactMechId
    if (contactMechId) {
        GenericValue ftpAddress = makeValue('FtpAddress', parameters)
        ftpAddress.contactMechId = contactMechId
        ftpAddress.create()
    } else {
        return error('Error creating contactMech')
    }

    Map resultMap = success()
    resultMap.contactMechId = contactMechId
    return resultMap
}

/**
 * Update FtpAddress contact Mech
 */
Map updateFtpAddressWithHistory() {
    Map resultMap = success()
    resultMap.oldContactMechId = parameters.contactMechId
    resultMap.contactMechId = parameters.contactMechId
    Map newContactMechResult
    if (resultMap.oldContactMechId) {
        GenericValue lookedValue = from('FtpAddress').where('contactMechId', parameters.contactMechId).queryOne()
        require(lookedValue as boolean, 'ServiceErrorUiLabels', 'ServiceValueNotFound')
        GenericValue newValue = (GenericValue) lookedValue.clone()
        newValue.setNonPKFields(parameters)
        if (newValue != lookedValue) {  // if there is some modifications in FtpAddress data
            newContactMechResult = run service: 'createFtpAddress', with: parameters
        } else { //update only contactMech
            Map updateContactMechMap = dispatcher.getDispatchContext().makeValidContext('updateContactMech', ModelService.IN_PARAM, parameters)
            updateContactMechMap.contactMechTypeId = 'FTP_ADDRESS'
            newContactMechResult = run service: 'updateContactMech', with: updateContactMechMap
        }

        if (resultMap.oldContactMechId != newContactMechResult.contactMechId) {
            resultMap.put('contactMechId', newContactMechResult.contactMechId)
        }
    }
    return resultMap
}

/**
 * Create FtpAddress contact Mech and link it with given partyId
 */
Map createPartyFtpAddress() {
    Map contactMech = run service: 'createFtpAddress', with: parameters
    if (ServiceUtil.isError(contactMech)) {
        return contactMech
    }
    String contactMechId = contactMech.contactMechId

    Map createPartyContactMechMap = [*:parameters, contactMechId: contactMechId]
    Map serviceResult = run service: 'createPartyContactMech', with: createPartyContactMechMap
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }

    //TODO: manage purpose

    Map resultMap = success()
    resultMap.contactMechId = contactMechId
    return resultMap
}

Map updatePartyFtpAddress() {
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
 * Hash an email address verification token the way it is stored
 */
String hashVerificationToken(String token) {
    return HashCrypt.digestHash('SHA-256', token.getBytes(StandardCharsets.UTF_8))
}

/**
 * Create a verification for an email address of a party and return its token
 */
Map createEmailAddressVerification() {
    GenericValue contactMech = from('ContactMech').where(contactMechId: parameters.contactMechId).queryOne()
    require(contactMech?.contactMechTypeId == 'EMAIL_ADDRESS' && contactMech.infoString as boolean,
            'PartyUiLabels', 'PartyEmailAddressNotExist')
    GenericValue partyContactMech = from('PartyContactMech')
            .where(partyId: parameters.partyId, contactMechId: parameters.contactMechId)
            .filterByDate()
            .queryFirst()
    require(partyContactMech as boolean, 'PartyUiLabels', 'PartyEmailAddressNotExist')

    byte[] randomBytes = new byte[32]
    new SecureRandom().nextBytes(randomBytes)
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    int expireHours = EntityUtilProperties.getPropertyValue('security',
            'email_verification.expire.hours', '48', delegator) as int

    GenericValue verification = makeValue('EmailAddressVerification', [
            emailAddress: contactMech.infoString,
            verifyHash: hashVerificationToken(token),
            expireDate: UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.HOUR, expireHours),
            partyId: parameters.partyId,
            contactMechId: parameters.contactMechId])
    delegator.createOrStore(verification)
    return success([token: token])
}

/**
 * Whether a verification for this email address was issued less than email_verification.resend.minutes ago
 */
boolean emailAddressVerificationRecentlySent(String emailAddress) {
    int resendMinutes = EntityUtilProperties.getPropertyValue('security',
            'email_verification.resend.minutes', '5', delegator) as int
    if (resendMinutes <= 0) {
        return false
    }
    GenericValue pending = from('EmailAddressVerification').where(emailAddress: emailAddress).queryOne()
    return pending?.lastUpdatedStamp?.after(
            UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.MINUTE, -resendMinutes)) as boolean
}

/**
 * Email a verification link for an email address of a party, when the product store asks for it
 */
Map sendEmailAddressVerification() {
    GenericValue userLogin = parameters.userLogin
    // partyContactMechPermissionCheck passes for the caller's own party or a party contact mech manager
    String partyId = parameters.partyId ?: userLogin.partyId
    require(partyId as boolean, 'PartyUiLabels', 'PartyEmailAddressNotExist')
    GenericValue storeEmail = from('ProductStoreEmailSetting')
            .where(productStoreId: parameters.productStoreId, emailType: 'PRDS_EMAIL_VERIFY')
            .cache()
            .queryOne()
    if (!storeEmail?.bodyScreenLocation) {
        return success()
    }
    GenericValue contactMech = from('ContactMech').where(contactMechId: parameters.contactMechId).queryOne()
    if (contactMech?.infoString && emailAddressVerificationRecentlySent(contactMech.infoString)) {
        logInfo("A verification was sent to contact mech ${parameters.contactMechId} less than "
                + 'email_verification.resend.minutes ago; not sending another one')
        return success()
    }
    Map created = run service: 'createEmailAddressVerification',
            with: [partyId: partyId, contactMechId: parameters.contactMechId]
    // Name the store and the account, so that someone who did not ask for this email can tell and ignore it
    GenericValue productStore = from('ProductStore').where(productStoreId: parameters.productStoreId).cache().queryOne()
    Map bodyParameters = [token: created.token,
            storeName: productStore?.storeName ?: productStore?.companyName ?: parameters.productStoreId,
            partyName: PartyHelper.getPartyName(delegator, partyId, false)]
    // The given web site is used only for this store; otherwise the first of the store's web sites with a webapp
    GenericValue webSite = parameters.webSiteId
            ? from('WebSite').where(webSiteId: parameters.webSiteId).cache().queryOne()
            : null
    if (webSite?.productStoreId != parameters.productStoreId) {
        webSite = null
    }
    List<GenericValue> storeWebSites = webSite ? [webSite]
            : from('WebSite').where(productStoreId: parameters.productStoreId).orderBy('webSiteId').cache().queryList()
    GenericValue chosenWebSite = null
    for (GenericValue candidate : storeWebSites) {
        try {
            bodyParameters.contextRoot = WebAppUtil.getWebappInfoFromWebsiteId(candidate.webSiteId).contextRoot
            chosenWebSite = candidate
            break
        } catch (IllegalArgumentException | IOException | SAXException e) {
            logWarning("No webapp found for web site ${candidate.webSiteId}: ${e.message}")
        }
    }
    webSite = chosenWebSite ?: storeWebSites[0]
    // Sent asynchronously and not persisted: a mail failure must not roll back the caller's transaction (registration),
    // and the token in bodyParameters must not be stored in JobSandbox/RuntimeData.
    dispatcher.runAsync('sendMailHiddenInLogFromScreen', [
            userLogin: userLogin,
            sendTo: contactMech.infoString,
            subject: storeEmail.subject,
            sendFrom: storeEmail.fromAddress,
            // no sendCc/sendBcc: the store's copy addresses must not receive working verification links
            contentType: storeEmail.contentType,
            bodyParameters: bodyParameters,
            bodyScreenUri: storeEmail.bodyScreenLocation,
            webSiteId: webSite?.webSiteId,
            emailType: 'PRDS_EMAIL_VERIFY'], false)
    return success()
}

/**
 * Mark the email address a verification token was issued for as verified, and consume the token
 */
Map verifyEmailAddress() {
    GenericValue verification = from('EmailAddressVerification')
            .where(verifyHash: hashVerificationToken(parameters.token))
            .queryFirst()
    boolean valid = verification && verification.partyId && verification.contactMechId
            && verification.expireDate && !UtilValidate.isDateBeforeNow(verification.expireDate)
    List<GenericValue> partyContactMechs = valid
            ? from('PartyContactMech')
                .where(partyId: verification.partyId, contactMechId: verification.contactMechId)
                .filterByDate()
                .queryList()
            : []
    GenericValue contactMech = partyContactMechs
            ? from('ContactMech').where(contactMechId: verification.contactMechId).queryOne()
            : null
    if (!contactMech || contactMech.infoString != verification.emailAddress) {
        return error(label('PartyUiLabels', 'PartyEmailAddressVerificationInvalid'))
    }
    partyContactMechs.each {
        it.verified = 'Y'
        it.store()
    }
    verification.remove()
    return success(label('PartyUiLabels', 'PartyEmailAddressVerified'))
}
