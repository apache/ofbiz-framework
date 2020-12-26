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

import java.sql.Timestamp

import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.minilang.SimpleMapProcessor
import org.apache.ofbiz.service.GenericServiceException
import org.apache.ofbiz.service.ServiceUtil

def createLead() {
    String leadContactPartyId
    String partyGroupPartyId
    // Check if Person or PartyGroup name is supplied
    if ((!parameters.firstName || !parameters.lastName) && !parameters.groupName) {
        return error(UtilProperties.getMessage("MarketingUiLabels", "SfaFirstNameLastNameAndCompanyNameMissingError", locale))
    }
    run service: "ensurePartyRole", with: [partyId: userLogin.partyId, roleTypeId: "OWNER"]
    // PartyRole check end
    parameters.roleTypeId = "LEAD"

    Map serviceResult = run service: "createPersonRoleAndContactMechs", with: parameters
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    leadContactPartyId = serviceResult.partyId
    serviceResult = run service: "createPartyRelationship", with: [partyIdFrom: userLogin.partyId,
                                                                   partyIdTo: leadContactPartyId,
                                                                   roleTypeIdFrom: "OWNER",
                                                                   roleTypeIdTo: "LEAD",
                                                                   partyRelationshipTypeId: "LEAD_OWNER"]
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    run service: "setPartyStatus", with: [partyId: leadContactPartyId,
                                          statusId: "LEAD_ASSIGNED"]

    // Now create PartyGroup corresponding to the companyName, if its not null and then set up
    // relationship of Person and PartyGroup as Employee and title
    if (parameters.groupName) {
    parameters.partyTypeId = "PARTY_GROUP"
    if (!leadContactPartyId) {
        parameters.roleTypeId = "ACCOUNT_LEAD"
        // In case we have any contact mech data then associate with party group
        serviceResult = run service: "createPartyGroupRoleAndContactMechs", with: parameters
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        partyGroupPartyId = serviceResult.partyId
        run service: "setPartyStatus", with: [partyId: partyGroupPartyId,
                                              statusId: "LEAD_ASSIGNED"]
    } else {
        serviceResult = run service: "createPartyGroup", with: resolvePartyProcessMap()
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        partyGroupPartyId = serviceResult.partyId
        serviceResult = run service: "createPartyRole", with: [partyId: partyGroupPartyId,
                                                               roleTypeId: "ACCOUNT_LEAD"]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
    }
    }
    if (leadContactPartyId && partyGroupPartyId) {
        run service: "createPartyRelationship", with: [partyIdFrom: partyGroupPartyId,
                                                       partyIdTo: leadContactPartyId,
                                                       roleTypeIdFrom: "ACCOUNT_LEAD",
                                                       roleTypeIdTo: "LEAD",
                                                       positionTitle: parameters.title,
                                                       partyRelationshipTypeId: "EMPLOYMENT"]
    }
    if (partyGroupPartyId) {
    run service: "createPartyRelationship", with: [partyIdFrom: userLogin.partyId,
                                                   partyIdTo: partyGroupPartyId,
                                                   roleTypeIdFrom: "OWNER",
                                                   roleTypeIdTo: "ACCOUNT_LEAD",
                                                   partyRelationshipTypeId: "LEAD_OWNER"]
    }

    if (parameters.dataSourceId) {
        serviceResult = run service: "createPartyDataSource", with: [partyId: leadContactPartyId,
                                                                     dataSourceId: parameters.dataSourceId]
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
    }
    Map result = success()
    result.partyId = leadContactPartyId
    result.partyGroupPartyId = partyGroupPartyId
    result.roleTypeId = parameters.roleTypeId
    result.successMessage = UtilProperties.getMessage("MarketingUiLabels", "SfaLeadCreatedSuccessfully", locale)
    return result
}

/**
 * Convert a lead person into a contact and associated lead group to an account
 * @return
 */
def convertLeadToContact() {
    String partyId = parameters.partyId
    String partyGroupId = parameters.partyGroupId
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    GenericValue partyRelationship = from("PartyRelationship")
        .where(partyIdTo: partyId,
                roleTypeIdFrom: "OWNER",
                roleTypeIdTo: "LEAD",
                partyRelationshipTypeId: "LEAD_OWNER")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        partyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: partyRelationship.getAllFields()
        logInfo("Expiring relationship ${partyRelationship}")
    }

    // Expire relation between lead company and lead person
    partyRelationship = from("PartyRelationship")
        .where(partyIdFrom: partyGroupId, roleTypeIdTo: "LEAD", roleTypeIdFrom: "ACCOUNT_LEAD", partyRelationshipTypeId: "EMPLOYMENT")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        partyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: partyRelationship.getAllFields()
    }

    // Expire relation between lead company and its owner
    partyRelationship = from("PartyRelationship")
        .where(partyIdFrom: userLogin.partyId, partyIdTo: partyGroupId, roleTypeIdTo: "ACCOUNT_LEAD", roleTypeIdFrom: "OWNER")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        partyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: partyRelationship.getAllFields()
    }

    run service: "ensurePartyRole", with: [partyId: partyGroupId,
                                           roleTypeId: "ACCOUNT"]

    run service: "createPartyRelationship", with: [partyIdFrom: userLogin.partyId,
                                                   partyIdTo: partyGroupId,
                                                   roleTypeIdFrom: "OWNER",
                                                   roleTypeIdTo: "ACCOUNT",
                                                   partyRelationshipTypeId: "ACCOUNT"]
    
    run service: "setPartyStatus", with: [partyId: partyGroupId,
                                          statusId: "LEAD_CONVERTED"]
    
    run service: "createPartyRole", with: [partyId: partyId,
                                           roleTypeId: "CONTACT"]
    
    // create new relationship between new account and contact person there
    run service: "createPartyRelationship", with: [partyIdFrom: partyGroupId,
                                                   roleTypeIdFrom: "ACCOUNT",
                                                   partyIdTo: partyId,
                                                   roleTypeIdTo: "CONTACT",
                                                   partyRelationshipTypeId: "EMPLOYMENT"]
    
    run service: "setPartyStatus", with: [partyId: partyId,
                                          statusId: "LEAD_CONVERTED"]

    Map result = success()
    result.partyId = partyId
    result.partyGroupId = partyGroupId
    result.successMessage = "Lead ${partyGroupId} ${partyId} succesfully converted to Account/Contact"
    return result
}

def resolvePartyProcessMap() {
    List messages = []
    Map resultMap = [:]
    //TODO convert map processor
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/party/PartyMapProcs.xml',
            'partyGroup', parameters, resultMap, messages, context.locale)
    // Check errors
    if (messages) {
        throw new GenericServiceException(messages.join(','))
    }
    return resultMap
}
