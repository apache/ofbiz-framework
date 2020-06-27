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

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityJoinOperator
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.minilang.SimpleMapProcessor

import java.sql.Timestamp
import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.party.party.PartyHelper

/**
  * Save Party Name Change
  */
def savePartyNameChange() {
    // for special case in ecommerce, if no partyId is passed in use userLogin.partyId
    if (!parameters.partyId) parameters.partyId = userLogin.partyId

    GenericValue partyNameHistory = makeValue('PartyNameHistory', parameters)
    partyNameHistory.changeDate = UtilDateTime.nowTimestamp()

    if (parameters.groupName) {
        GenericValue partyGroup = from('PartyGroup').where(parameters).queryOne()
        if (partyGroup.groupName != parameters.groupName) {
            partyNameHistory.setNonPKFields(partyGroup)
            partyNameHistory.create()
        }
    } else if (parameters.firstName ||
               parameters.middleName || 
               parameters.lastName ||
               parameters.personalTitle ||
               parameters.suffix) {
        GenericValue person = from('Person').where(parameters).queryOne()
        if (person.firstName != parameters.firstName ||
                person.middleName != parameters.middleName ||
                person.lastName != parameters.lastName ||
                person.personalTitle != parameters.personalTitle ||
                person.suffix != parameters.suffix) {
            partyNameHistory.setNonPKFields(person)
            partyNameHistory.create()
        }
    }
    return success()
}

/**
 * Get Party Name For Date
 */
def getPartyNameForDate() {
    Map resultMap = success()

    List<GenericValue> partyNameHistoryList = from("PartyNameHistory")
        .where("partyId", parameters.partyId)
        .orderBy("-changeDate")
        .queryList()

    GenericValue person = from('Person').where(parameters).queryOne()
    GenericValue partyGroup = from('PartyGroup').where(parameters).queryOne()

    if (!parameters.compareDate) parameters.compareDate = UtilDateTime.nowTimestamp()

    // go through the list which is sorted by most recent first and find the oldest (last) one with the changeDate greater than the compareDate
    GenericValue partyNameHistoryCurrent = null
    for (GenericValue partyNameHistory : partyNameHistoryList) {
        Timestamp changeDate = partyNameHistory.changeDate
        if (changeDate.after(parameters.compareDate)) {
            partyNameHistoryCurrent = partyNameHistory
        }
    }

    if (partyNameHistoryCurrent) {
        // partyNameHistoryCurrent has a value
        if (person) {
            person = partyNameHistoryCurrent
        } else if (partyGroup) {
            partyGroup = partyNameHistoryCurrent
        }
    }

    if (person) {
        resultMap.firstName = person.firstName
        resultMap.lastName = person.lastName
        if (person.middleName) resultMap.middleName = person.middleName
        if (person.personalTitle) resultMap.personalTitle = person.personalTitle
        if (person.suffix) resultMap.suffix = person.suffix
        if (!partyNameHistoryCurrent && person.gender) resultMap.gender = person.gender

        resultMap.fullName = PartyHelper.getPartyName(person, parameters.lastNameFirst == "Y")
    } else if (partyGroup) {
        resultMap.groupName = partyGroup.groupName
        resultMap.fullName = partyGroup.groupName
    }

    return resultMap
}

/**
 * Get Postal Address Boundary
 */
def getPostalAddressBoundary() {
    Map resultMap = success()

    List<String> geoIds = from('PostalAddressBoundary')
        .where(makeValue('PostalAddressBoundary', parameters))
        .getFieldList('geoId')

    List<GenericValue> geos = from('Geo')
            .where(EntityCondition.makeCondition('geoId', EntityOperator.IN, geoIds))
            .queryList()

    resultMap.geos = geos
    return resultMap
}

/**
 * Create mass party identification with association between value and type
 */
def createPartyIdentifications() {
    Map resultMap = success()

    for (Map.Entry<String, String> entry : parameters.identifications.entrySet()) {
        GenericValue identificationType = from('PartyIdentificationType')
            .where("partyIdentificationTypeId", entry.getValue())
            .queryOne()
        if (identificationType) {
            String idValue = parameters.identifications[identificationType.partyIdentificationTypeId]
            if (idValue) {
                run service: 'createPartyIdentification', with: [partyId: parameters.partyId,
                                                                 idValue: idValue,
                                                                 partyIdentificationTypeId: identificationType.partyIdentificationTypeId]
            }
        }
    }
 
    return resultMap
}

/**
 * Sets Party Profile Defaults
 */
def setPartyProfileDefaults() {
    if (!parameters.partyId) parameters.partyId = userLogin.partyId

    // lookup existing value
    GenericValue partyProfileDefault = from('PartyProfileDefault')
        .where(parameters)
        .queryOne()
    if (!partyProfileDefault) {

        // create the profile defaut because is missing
        partyProfileDefault = makeValue('PartyProfileDefault', parameters)
        partyProfileDefault.create()
    } else {

        // update the fields
        partyProfileDefault.setNonPKFields(parameters)
        partyProfileDefault.store()
    }

    return success()
}

/**
 * Gets all parties related to partyIdFrom using the PartyRelationship entity
 */
def getPartiesByRelationship() {
    Map resultMap = success()

    GenericValue lookupMap = makeValue('PartyRelationship')
    lookupMap.setAllFields(parameters, false, null, null)
    List<String> partyIdTos = from('PartyRelationship')
        .where(lookupMap)
        .getFieldList('partyIdTo')

    List<GenericValue> parties = from('Party')
            .where(EntityCondition.makeCondition('partyId', EntityOperator.IN, partyIdTos))
            .queryList()
    if (parties) resultMap.parties = parties
    return resultMap
}

/**
 * Gets Parent Organizations for an Organization Party
 */
def getParentOrganizations() {
    Map resultMap = success()

    List relatedPartyIdList = [parameters.organizationPartyId]
    String recurse = "Y"
    if (parameters.getParentsOfParents) recurse = parameters.getParentsOfParents

    Map res = followPartyRelationshipsInline(relatedPartyIdList,'GROUP_ROLLUP',
            'ORGANIZATION_UNIT', 'Y', 'PARENT_ORGANIZATION',
            null, 'Y', recurse, 'Y')
    resultMap.parentOrganizationPartyIdList = res.relatedPartyIdList
    return resultMap
}

/**
 * Get Parties Related to a Party
 */
def getRelatedParties() {
    Map resultMap = success()

    List relatedPartyIdList = [parameters.partyIdFrom]
    resultMap = followPartyRelationshipsInline(relatedPartyIdList, parameters.partyRelationshipTypeId,
            parameters.roleTypeIdFrom, parameters.roleTypeIdFromInclueAllChildTypes,
            parameters.roleTypeIdTo, parameters.roleTypeIdToIncludeAllChildTypes,
            parameters.includeFromToSwitched, parameters.recurse, parameters.useCache)
    return resultMap
}

/**
 * Get Child RoleTypes
 */
def getChildRoleTypes () {
    Map resultMap = success()

    Map res = getChildRoleTypesInline([parameters.roleTypeId])

    resultMap.childRoleTypeIdList = res.childRoleTypeIdList
    return resultMap
}

/**
 * Get the email of the party
 */
def getPartyEmail () {
    Map resultMap = success()

    // First try to find primary email Address when not found get other email
    Timestamp searchTimestamp = UtilDateTime.nowTimestamp()
    GenericValue emailAddress = from("PartyContactWithPurpose")
        .where(partyId: parameters.partyId, contactMechPurposeTypeId: parameters.contactMechPurposeTypeId)
        .filterByDate(searchTimestamp, 'purposeFromDate', 'purposeThruDate', 'contactFromDate', 'contactThruDate')
        .queryFirst()
    // Any other email
    if (!emailAddress) {
        emailAddress = from("PartyAndContactMech")
        .where(partyId: parameters.partyId, contactMechTypeId: "EMAIL_ADDRESS")
        .filterByDate(searchTimestamp)
        .queryFirst()
    }
    // Any other electronic address
    if (!emailAddress) {
        emailAddress = from("PartyAndContactMech")
        .where(partyId: parameters.partyId, contactMechTypeId: "ELECTRONIC_ADDRESS")
        .filterByDate(searchTimestamp)
        .queryFirst()
    }
    if (emailAddress) {
        resultMap.emailAddress = emailAddress.infoString
        resultMap.contactMechId = emailAddress.contactMechId
    }
    return resultMap
}

/**
 * Get the telephone number of the party
 */
def getPartyTelephone () {
    Map resultMap = success()
    Timestamp searchTimestamp = UtilDateTime.nowTimestamp()
    GenericValue telephone = null

    List<GenericValue> telephoneList = from("PartyContactDetailByPurpose")
        .where(partyId: parameters.partyId, contactMechTypeId: 'TELECOM_NUMBER')
        .filterByDate(searchTimestamp, 'purposeFromDate', 'purposeThruDate', 'fromDate', 'thruDate')
        .queryList()
    if (telephoneList) {
        List<String> types = []
        if (!parameters.contactMechPurposeTypeId) {
            // search in this order if not provided
            types = ['PRIMARY_PHONE', 'PHONE_MOBILE', 'PHONE_WORK',
                     'PHONE_QUICK', 'PHONE_HOME', 'PHONE_BILLING',
                     'PHONE_SHIPPING', 'PHONE_SHIP_ORIG']
        } else {
            types << parameters.contactMechPurposeTypeId
        }

        telephone = EntityUtil.getFirst(EntityUtil.filterByCondition(telephoneList,
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityJoinOperator.IN, types)))
        if (telephone) {
            resultMap.contactMechPurposeTypeId = telephone.contactMechPurposeTypeId
        }
    } else {
        telephone = from("PartyAndContactMech")
            .where(partyId: parameters.partyId, contactMechTypeId: 'TELECOM_NUMBER')
            .filterByDate(searchTimestamp)
            .queryFirst()
    }

    if (telephone) {
        resultMap.contactMechId = telephone.contactMechId
        if (telephone.containsKey('countryCode')) {
            resultMap.countryCode = telephone.countryCode
        } else if (telephone.containsKey('tnCountryCode')) {
            resultMap.countryCode = telephone.tnCountryCode
        }
        if (telephone.containsKey('areaCode')) {
            resultMap.areaCode = telephone.areaCode 
        } else if (telephone.containsKey('tnAreaCode')) {
            resultMap.areaCode = telephone.tnAreaCode
        }
        if (telephone.containsKey('contactNumber')) {
            resultMap.contactNumber = telephone.contactNumber
        } else if (telephone.containsKey('tnContactNumber')) {
            resultMap.contactNumber = telephone.tnContactNumber
        }
        if (telephone.containsKey('extension')) resultMap.extension = telephone.extension
    }

    return resultMap
}

/**
 * Get the postal address of the party
 */
def getPartyPostalAddress () {
    Map resultMap = success()
    GenericValue address = null
    Timestamp searchTimestamp = UtilDateTime.nowTimestamp()

    List<GenericValue> addressList = from("PartyContactDetailByPurpose")
        .where(partyId: parameters.partyId, contactMechTypeId: 'POSTAL_ADDRESS')
        .filterByDate(searchTimestamp, 'purposeFromDate', 'purposeThruDate', 'fromDate', 'thruDate')
        .queryList()
    if (addressList) {
        List<String> types = []
        if (!parameters.contactMechPurposeTypeId) {
            // search in this order if not provided
            types = ['GENERAL_LOCATION', 'BILLING_LOCATION', 'PAYMENT_LOCATION', 'SHIPPING_LOCATION']
        } else {
            types << parameters.contactMechPurposeTypeId
        }
        addressList = EntityUtil.filterByCondition(addressList,
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityJoinOperator.IN, types))
        if (addressList) {
            address = addressList[0]
            resultMap.contactMechPurposeTypeId = address.contactMechPurposeTypeId
        }
    } else {
        address = from("PartyAndContactMech")
            .where(partyId: parameters.partyId, contactMechTypeId: 'POSTAL_ADDRESS')
            .filterByDate(searchTimestamp)
            .queryFirst()
    }

    if (address) {
        resultMap.contactMechId = address.contactMechId
        if (address.containsKey("address1")) {
            if (address.address1)           resultMap.address1 = address.address1
            if (address.address2)           resultMap.address2 = address.address2
            if (address.directions)         resultMap.directions = address.directions
            if (address.city)               resultMap.city = address.city
            if (address.postalCode)         resultMap.postalCode = address.postalCode
            if (address.stateProvinceGeoId) resultMap.stateProvinceGeoId = address.stateProvinceGeoId
            if (address.countyGeoId)        resultMap.countyGeoId = address.countyGeoId
            if (address.countryGeoId)       resultMap.countryGeoId = address.countryGeoId
        } else if (address.containsKey("paAddress1")) {
            if (address.paAddress1)           resultMap.address1 = address.paAddress1
            if (address.paAddress2)           resultMap.address2 = address.paAddress2
            if (address.paDirections)         resultMap.directions = address.paDirections
            if (address.paCity)               resultMap.city = address.paCity
            if (address.paPostalCode)         resultMap.postalCode = address.paPostalCode
            if (address.paStateProvinceGeoId) resultMap.stateProvinceGeoId = address.paStateProvinceGeoId
            if (address.paCountyGeoId)        resultMap.countyGeoId = address.paCountyGeoId
            if (address.paCountryGeoId)       resultMap.countryGeoId = address.paCountryGeoId
        }
    }

    return resultMap
}

/**
  * Create an AddressMatchMap
  */
def createAddressMatchMap() {
    GenericValue newAddressMatchMap = makeValue('AddressMatchMap', parameters)
    if (parameters.mapKey)   newAddressMatchMap.mapKey = ((String) parameters.mapKey).toUpperCase(context.locale)
    if (parameters.mapValue) newAddressMatchMap.mapValue = ((String) parameters.mapValue).toUpperCase(context.locale)
    newAddressMatchMap.create()
    return success()
}

/**
 * Remove all AddressMatchMap
 */
def clearAddressMatchMap() {
    delegator.removeAll('AddressMatchMap')
    return success()
}

/**
 * Create a PartyRelationship
 */
def createPartyRelationship() {
    if (!parameters.fromDate)       parameters.fromDate = UtilDateTime.nowTimestamp()
    if (!parameters.roleTypeIdFrom) parameters.roleTypeIdFrom = '_NA_'
    if (!parameters.roleTypeIdTo)   parameters.roleTypeIdTo = '_NA_'
    if (!parameters.partyIdFrom)    parameters.partyIdFrom = userLogin.partyId

    // check if not already exist
    List<GenericValue> partyRels = from("PartyRelationship")
        .where(partyIdFrom: parameters.partyIdFrom,
               partyIdTo: parameters.partyIdTo,
               roleTypeIdFrom: parameters.roleTypeIdFrom,
               roleTypeIdTo: parameters.roleTypeIdTo)
        .filterByDate()
        .queryList()
    if (!partyRels) {
        GenericValue partyRelationship = makeValue('PartyRelationship', parameters)
        partyRelationship.create()
    }
    return success()
}

/**
 * Update a PartyRelationship
 */
def updatePartyRelationship() {
    if (!parameters.roleTypeIdFrom) parameters.roleTypeIdFrom = '_NA_'
    if (!parameters.roleTypeIdTo)   parameters.roleTypeIdTo = '_NA_'

    // lookup existing value
    GenericValue partyRelationship = from('PartyRelationship')
        .where(parameters)
        .queryOne()
    partyRelationship.setNonPKFields(parameters)
    partyRelationship.store()

    return success()
}

/**
 * Delete a PartyRelationship
 */
def deletePartyRelationship() {
    if (!parameters.roleTypeIdFrom) parameters.roleTypeIdFrom = '_NA_'
    if (!parameters.roleTypeIdTo)   parameters.roleTypeIdTo = '_NA_'

    // lookup existing value
    GenericValue partyRelationship = from('PartyRelationship')
        .where(parameters)
        .queryOne()
    partyRelationship.remove()

    return success()
}

/**
 * Create a company/contact relationship and add the related roles
 */
def createPartyRelationshipContactAccount() {
    Map resultMap = success()

    Map roleMap = [partyId: parameters.accountPartyId, roleTypeId: 'ACCOUNT']
    GenericValue partyRole = from('PartyRole')
        .where(roleMap)
        .queryOne()
    if (!partyRole) {
        run service: 'createPartyRole', with: roleMap
    }

    roleMap = [partyId: parameters.contactPartyId, roleTypeId: 'CONTACT']
    partyRole = from('PartyRole')
        .where(roleMap)
        .queryOne()
    if (!partyRole) {
        run service: 'createPartyRole', with: roleMap
    }

    run service: 'createPartyRelationship',
        with: [partyIdFrom: parameters.accountPartyId,
               roleTypeIdFrom: 'ACCOUNT',
               partyIdTo: parameters.contactPartyId,
               roleTypeIdTo: 'CONTACT',
               partyRelationshipTypeId: 'EMPLOYMENT',
               comments: parameters.comments
              ]
 
    return resultMap
}

/**
 * Notification email on party creation
 */
def sendCreatePartyEmailNotification() {
    Map resultMap = success()

    Map lookupMap = [emailType: 'PARTY_REGIS_CONFIRM']
    String productStoreId = parameters.productStoreId
    if (!productStoreId) {
        logWarning('No productStoreId specified.')
    } else {
        lookupMap.productStoreId = productStoreId
    }

    GenericValue storeEmail = from('ProductStoreEmailSetting')
        .where(lookupMap)
        .queryOne()
    if (storeEmail && storeEmail.bodyScreenLocation) {
        GenericValue webSite = from('WebSite')
            .where(productStoreId: storeEmail.productStoreId)
            .queryFirst()

        Map bodyParameters = parameters
        GenericValue person = from('Person')
            .where(parameters)
            .queryOne()
        bodyParameters.person = person

        run service: 'sendMailFromScreen',
            with: [bodyParameters: bodyParameters,
                   sendTo: parameters.emailAddress,
                   subject: storeEmail.subject,
                   sendFrom: storeEmail.fromAddress,
                   sendCc: storeEmail.ccAddress,
                   sendBcc: storeEmail.bccAddress,
                   contentType: storeEmail.contentType,
                   bodyScreenUri: storeEmail.bodyScreenLocation,
                   webSiteId: webSite.webSiteId,
                   emailType: lookupMap.emailType]
    }
    return resultMap
}

/**
 * Send the Notification email on personal information update
 */
def sendUpdatePersonalInfoEmailNotification() {
    Map resultMap = success()

    Map lookupMap = [emailType: 'UPD_PRSNL_INF_CNFRM']
    String productStoreId = parameters.productStoreId
    if (!productStoreId) {
        logWarning('No productStoreId specified.')
    } else {
        lookupMap.productStoreId = productStoreId
    }

    GenericValue storeEmail = from('ProductStoreEmailSetting')
        .where(lookupMap)
        .queryOne()
    if (storeEmail && storeEmail.bodyScreenLocation) {
        String partyId = parameters.partyId
        if (parameters.updatedUserLogin) partyId = parameters.updatedUserLogin.partyId

        GenericValue webSite = from('WebSite')
            .where(productStoreId: storeEmail.productStoreId)
            .queryFirst()

        Map bodyParameters = parameters
        GenericValue partyAndPerson = from('PartyAndPerson')
            .where(parameters)
            .queryOne()
        bodyParameters.partyAndPerson = partyAndPerson

        GenericValue partyContactDetailByPurpose = from('PartyContactDetailByPurpose')
            .where(partyId: partyId, contactMechPurposeTypeId: 'PRIMARY_EMAIL')
            .filterByDate()
           .queryFirst()
        if (!partyContactDetailByPurpose) logWarning('No email found.')
        else {
            GenericValue contactMech = from('ContactMech')
                .where(contactMechId: partyContactDetailByPurpose.contactMechId)
                .queryOne()
            run service: 'sendMailFromScreen',
                with: [bodyParameters: bodyParameters,
                       sendTo: contactMech.infoString,
                       subject: storeEmail.subject,
                       sendFrom: storeEmail.fromAddress,
                       sendCc: storeEmail.ccAddress,
                       sendBcc: storeEmail.bccAddress,
                       contentType: storeEmail.contentType,
                       bodyScreenUri: storeEmail.bodyScreenLocation,
                       webSiteId: webSite.webSiteId,
                       emailType: lookupMap.emailType]
        }
    }
    return resultMap
}

/**
 * Send the Notification email on account activated
 */
def sendAccountActivatedEmailNotification() {
    Map resultMap = success()

    Map lookupMap = [emailType: 'PRDS_CUST_ACTIVATED']
    String productStoreId = parameters.productStoreId
    if (!productStoreId) {
        logWarning('No productStoreId specified.')
    } else {
        lookupMap.productStoreId = productStoreId
    }

    GenericValue storeEmail = from('ProductStoreEmailSetting')
            .where(lookupMap)
            .queryOne()
    if (storeEmail && storeEmail.bodyScreenLocation) {
        String partyId = parameters.partyId
        if (!partyId) partyId = userLogin.partyId

        GenericValue webSite = from('WebSite')
                .where(productStoreId: storeEmail.productStoreId)
                .queryFirst()

        Map bodyParameters = parameters
        GenericValue person = from('Person')
                .where(partyId: partyId)
                .queryOne()
        bodyParameters.person = person

        GenericValue partyContactDetailByPurpose = from('PartyContactDetailByPurpose')
                .where(partyId: partyId, contactMechPurposeTypeId: 'PRIMARY_EMAIL')
                .filterByDate()
                .queryFirst()
        if (!partyContactDetailByPurpose) logWarning('No email found.')
        else {
            GenericValue contactMech = from('ContactMech')
                .where('contactMechId': partyContactDetailByPurpose.contactMechId)
                .queryOne()
            run service: 'sendMailFromScreen',
                with: [bodyParameters: bodyParameters,
                       sendTo: contactMech.infoString,
                       subject: storeEmail.subject,
                       sendFrom: storeEmail.fromAddress,
                       sendCc: storeEmail.ccAddress,
                       sendBcc: storeEmail.bccAddress,
                       contentType: storeEmail.contentType,
                       bodyScreenUri: storeEmail.bodyScreenLocation,
                       webSiteId: webSite.webSiteId,
                       emailType: lookupMap.emailType]
        }
    }
    return resultMap
}

/**
 * Create and update a person
 */
def createUpdatePerson() {
    Map resultMap = success()
    String partyId = parameters.partyId

    Map personContext = [partyId: partyId]
    List<String> messages = []
    //TODO need to convert from MapProcessor
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/party/PartyMapProcs.xml',
            'person', parameters, personContext, messages, context.locale)

    // Check errors
    if (messages) return error(StringUtil.join(messages, ','))

    GenericValue party = from('Party')
       .where(partyId: partyId)
       .queryOne()
    String serviceName = (party ? 'update' : 'create') + 'Person'
    run service: serviceName, with: personContext
    resultMap.partyId = partyId
    return resultMap
}

/**
 * Create customer profile on basis of First Name ,Last Name and Email Address
 */
def quickCreateCustomer() {
    Map resultMap = success()

    Map personContext = [:]
    Map emailContext = [:]
    List<String> messages = []
    //TODO need to convert from MapProcessor
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/contact/PartyContactMechMapProcs.xml',
            'person', parameters, personContext, messages, context.locale)
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/contact/PartyContactMechMapProcs.xml',
            'emailAddress', parameters, emailContext, messages, context.locale)

    // Check errors
    if (messages) return error(StringUtil.join(messages, ','))

    // Create person
    Map serviceResult = run service: 'createPerson', with: personContext
    if (ServiceUtil.isError(serviceResult)) return serviceResult
    String partyId = serviceResult.partyId

    GenericValue userLogin = from('UserLogin')
            .where(userLoginId: 'system')
            .cache()
            .queryOne()
    emailContext.partyId = partyId
    emailContext.userLogin = userLogin
    emailContext.contactMechPurposeTypeId = 'PRIMARY_EMAIL'
    serviceResult = run service: 'createPartyEmailAddress', with: emailContext
    if (ServiceUtil.isError(serviceResult)) return serviceResult

    // Sign up for Contact List
    if (parameters.subscribeContactList == 'Y') {
        serviceResult = run service: 'signUpForContactList', with: [partyId: partyId,
                                                                    contactListId: parameters.contactListId,
                                                                    email: parameters.emailAddress]
        if (ServiceUtil.isError(serviceResult)) return serviceResult
    }

    // Create the PartyRole
    serviceResult = run service: 'createPartyRole', with: [partyId: partyId,
                                                           roleTypeId: 'CUSTOMER']
    if (ServiceUtil.isError(serviceResult)) return serviceResult

    resultMap.partyId = partyId
    return resultMap
}

/**
 * Get the main role of this party which is a child of the MAIN_ROLE roletypeId
 */
def getPartyMainRole() {
    Map resultMap = success()

    List<GenericValue> partyRoles = from("PartyRole")
        .where(partyId: parameters.partyId)
        .queryList()
    // find the role in the list
    String mainRoleTypeId = null
    for (GenericValue partyRole : partyRoles) {
        if (!mainRoleTypeId) {
            List<GenericValue> roleTypeIn3Levels = from("RoleTypeIn3Levels")
                    .where(topRoleTypeId: 'MAIN_ROLE', lowRoleTypeId: partyRole.roleTypeId)
                    .queryList()
            if (roleTypeIn3Levels) {
                mainRoleTypeId = partyRole.roleTypeId
            }
        }
    }
    if (mainRoleTypeId) {
        resultMap.roleTypeId = mainRoleTypeId
        GenericValue roleType = from('RoleType')
                .where(roleTypeId: mainRoleTypeId)
                .cache()
                .queryOne()
        resultMap.description = roleType.description
    }

    return resultMap
}

// Specific methods

/**
 * Follow PartyRelationships
 * Uses the following fields in the env (with * are required):
 * relatedPartyIdList* (initial partyIdFrom should be in this list; accumulator of new partyIds, ie all partyIdTo found will be added to this, thus can support recursion)
 * partyRelationshipTypeId
 * roleTypeIdFrom
 * roleTypeIdFromIncludeAllChildTypes
 * roleTypeIdTo
 * roleTypeIdToInclueAllChildTypes
 * includeFromToSwitched
 * recurse
 * useCache (should be "true" or "false")
 */
def followPartyRelationshipsInline(List relatedPartyIdList,String partyRelationshipTypeId, String roleTypeIdFrom, String roleTypeIdFromIncludeAllChildTypes,
    String roleTypeIdTo, String roleTypeIdToInclueAllChildTypes, String includeFromToSwitched, String recurse, String useCache ) {
    Map resultMap = success()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    List roleTypeIdFromList = null
    if (roleTypeIdFrom) roleTypeIdFromList = [roleTypeIdFrom]
    if ("Y" == roleTypeIdFromIncludeAllChildTypes) {
        List roleTypeIdListName = roleTypeIdFromList
        Map res = getChildRoleTypesInline(roleTypeIdListName)
        roleTypeIdFromList = res.childRoleTypeIdList
    }

    List roleTypeIdToList = null
    if (roleTypeIdTo) roleTypeIdToList = [roleTypeIdTo]
    if ("Y" == roleTypeIdToInclueAllChildTypes) {
        List roleTypeIdListName = roleTypeIdToList
        Map res = getChildRoleTypesInline(roleTypeIdListName)
        roleTypeIdToList = res.childRoleTypeIdList
    }

    Map res = followPartyRelationshipsInlineRecurse(relatedPartyIdList, roleTypeIdFromList, roleTypeIdToList,
        partyRelationshipTypeId, includeFromToSwitched, recurse, nowTimestamp, useCache)
    for (String newPartyId : res.NewRelatedPartyIdList) {
        if (!relatedPartyIdList.contains(newPartyId)) {
            relatedPartyIdList << newPartyId
        }
    }
    resultMap.relatedPartyIdList = relatedPartyIdList
    return resultMap
}

/**
 * Follow PartyRelationships Recurse
 */
def followPartyRelationshipsInlineRecurse (List relatedPartyIdList, List roleTypeIdFromList, List roleTypeIdToList, String partyRelationshipTypeId,
     String includeFromToSwitched, String recurse, Timestamp searchTimestamp, String useCache) {

    Map resultMap = success()
    List newRelatedPartyIdList = []
    List relatedPartyIdAlreadySearchedList = []
    for (String relatedPartyId : relatedPartyIdList) {
         if (!relatedPartyIdAlreadySearchedList.contains(relatedPartyId)) {
             relatedPartyIdAlreadySearchedList.add(relatedPartyId)

             List<EntityCondition> entityConditionList = [EntityCondition.makeCondition("partyIdFrom", relatedPartyId)]
             if (roleTypeIdFromList) entityConditionList << EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, roleTypeIdFromList)
             if (roleTypeIdToList) entityConditionList << EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.IN, roleTypeIdToList)
             if (partyRelationshipTypeId) entityConditionList << EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)
             EntityCondition condition = EntityCondition.makeCondition(entityConditionList)

             // get the newest (highest date) first
             List PartyRelationshipList = from("PartyRelationship")
                     .where(condition)
                     .orderBy('-fromDate')
                     .filterByDate(searchTimestamp)
                     .cache('Y' == useCache)
                     .queryList()
             for (GenericValue PartyRelationship : PartyRelationshipList) {
                 if ( !relatedPartyIdList.contains(PartyRelationship.partyIdTo) &&
                      !newRelatedPartyIdList.contains(PartyRelationship.partyIdTo)) {
                      newRelatedPartyIdList << PartyRelationship.partyIdTo
                 }
             }

             if ('Y' == includeFromToSwitched) {
                 entityConditionList = [EntityCondition.makeCondition("partyIdTo", relatedPartyId)]
                 // The roles are reversed
                 if (roleTypeIdFromList) entityConditionList << EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, roleTypeIdToList)
                 if (roleTypeIdToList) entityConditionList << EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.IN, roleTypeIdFromList)
                 if (partyRelationshipTypeId) entityConditionList << EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)
                 condition = EntityCondition.makeCondition(entityConditionList)

                PartyRelationshipList = from("PartyRelationship")
                        .where(condition)
                        .orderBy('-fromDate')
                        .filterByDate(searchTimestamp)
                        .cache('Y' == useCache)
                        .queryList()
                for (GenericValue PartyRelationship : PartyRelationshipList) {
                    if ( !relatedPartyIdList.contains(PartyRelationship.partyIdFrom) &&
                         !newRelatedPartyIdList.contains(PartyRelationship.partyIdFrom)) {
                         newRelatedPartyIdList << PartyRelationship.partyIdFrom
                    }
                }
             }
         }
    }

    // if we found new ones, add them to the master list and if recurse=Y then recurse
    if (newRelatedPartyIdList) {
        relatedPartyIdList = newRelatedPartyIdList
        if ('Y' == recurse) {
            logVerbose("Recursively calling followPartyRelationshipsInlineRecurse NewRelatedPartyIdList=${newRelatedPartyIdList}")
            Map res = followPartyRelationshipsInlineRecurse(relatedPartyIdList, roleTypeIdFromList, roleTypeIdToList,
                partyRelationshipTypeId, includeFromToSwitched, recurse, searchTimestamp, useCache)
            for (String newPartyId : res.NewRelatedPartyIdList) {
                if ( !newRelatedPartyIdList.contains(newPartyId)) {
                     newRelatedPartyIdList << newPartyId
                }
            }
        }
    }
    resultMap.NewRelatedPartyIdList = newRelatedPartyIdList
    return resultMap
}

/**
 * Get Child RoleTypes Inline
 */
def getChildRoleTypesInline (List roleTypeIdListName) {
    Map resultMap = success()
    List newRoleTypeIdList = []
    List roleTypeIdAlreadySearchedList = []

    for (String roleTypeId : roleTypeIdListName) {
        if (!roleTypeIdAlreadySearchedList.contains(roleTypeId)) {
            roleTypeIdAlreadySearchedList << roleTypeId

            List RoleTypeList = from("RoleType").where(parentTypeId: roleTypeId).cache().queryList()
            for (GenericValue newRoleType : RoleTypeList) {
                if ( !roleTypeIdListName.contains(newRoleType.roleTypeId) &&
                     !newRoleTypeIdList.contains(newRoleType.roleTypeId)) {
                     newRoleTypeIdList << newRoleType.roleTypeId
                }
            }
        }
    }

    // if we found new ones, add them to the master list and if recurse=Y then recurse
    if (newRoleTypeIdList) {
        roleTypeIdListName = newRoleTypeIdList
        logVerbose("Recursively calling getChildRoleTypesInline roleTypeIdListName=${roleTypeIdListName}, newRoleTypeIdList=${newRoleTypeIdList}")
        Map res = getChildRoleTypesInline(roleTypeIdListName)
        for (String childRoleTypeId : res.childRoleTypeIdList) {
            if ( !newRoleTypeIdList.contains(childRoleTypeId)) {
                 newRoleTypeIdList << childRoleTypeId
            }
        }
    }

    resultMap.childRoleTypeIdList = newRoleTypeIdList
    return resultMap
}
