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
import org.apache.ofbiz.service.ServiceUtil

def createLead() {
    Map result = success()
    String leadContactPartyId
    String partyGroupPartyId
    // Check if Person or PartyGroup name is supplied
    if (((!parameters.firstName) || (!parameters.lastName)) && (!parameters.groupName)) {
        String errorMessage = UtilProperties.getMessage("MarketingUiLabels", "SfaFirstNameLastNameAndCompanyNameMissingError", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map ensurePartyRoleCtx = [partyId: userLogin.partyId, roleTypeId: "OWNER"]
    run service: "ensurePartyRole", with: ensurePartyRoleCtx
    // PartyRole check end
    if (parameters.firstName && parameters.lastName) {
        parameters.roleTypeId = "LEAD"

        Map serviceResult = run service: "createPersonRoleAndContactMechs", with: parameters
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        leadContactPartyId = serviceResult.partyId
        Map partyRelationshipCtx = [partyIdFrom: userLogin.partyId, partyIdTo: leadContactPartyId, roleTypeIdFrom: "OWNER", roleTypeIdTo: "LEAD", partyRelationshipTypeId: "LEAD_OWNER"]
        Map serviceResultCPR = run service: "createPartyRelationship", with: partyRelationshipCtx
        if (!ServiceUtil.isSuccess(serviceResultCPR)) {
            return serviceResultCPR
        }
        Map updatePartyStatusCtx = [partyId: leadContactPartyId, statusId: "LEAD_ASSIGNED"]
        run service: "setPartyStatus", with: updatePartyStatusCtx
    }
    // Now create PartyGroup corresponding to the companyName, if its not null and then set up relationship of Person and PartyGroup as Employee and title
    if (parameters.groupName) {
        parameters.partyTypeId = "PARTY_GROUP"
        if (!leadContactPartyId) {
            parameters.roleTypeId = "ACCOUNT_LEAD"
            // In case we have any contact mech data then associate with party group
            Map serviceResultCPGRACM = run service: "createPartyGroupRoleAndContactMechs", with: parameters
            if (!ServiceUtil.isSuccess(serviceResultCPGRACM)) {
                return serviceResultCPGRACM
            }
            partyGroupPartyId = serviceResultCPGRACM.partyId
            Map updatePartyStatusCtxGroup = [partyId: partyGroupPartyId, statusId: "LEAD_ASSIGNED"]
            run service: "setPartyStatus", with: updatePartyStatusCtxGroup
        } else {
            Map partyGroupCtx = [:]
            List<String> messages = []
            // TODO need to convert from MapProcessor
            SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/party/PartyMapProcs.xml', 'partyGroup', parameters, partyGroupCtx, messages, context.locale)
            if (messages) return error(StringUtil.join(messages, ","))
            Map serviceResultCPG = run service: "createPartyGroup", with: partyGroupCtx
            if (!ServiceUtil.isSuccess(serviceResultCPG)) {
                return serviceResultCPG
            }
            partyGroupPartyId = serviceResultCPG.partyId
            Map createPartyRoleCtx = [partyId: partyGroupPartyId, roleTypeId: "ACCOUNT_LEAD"]
            Map serviceResultCPR = run service: "createPartyRole", with: createPartyRoleCtx
            if (!ServiceUtil.isSuccess(serviceResultCPR)) {
                return serviceResultCPR
            }
        }
        Map partyRelationshipCtx = [:]
        if (leadContactPartyId) {
            partyRelationshipCtx = [partyIdFrom: partyGroupPartyId, partyIdTo: leadContactPartyId, roleTypeIdFrom: "ACCOUNT_LEAD", roleTypeIdTo: "LEAD", positionTitle: parameters.title, partyRelationshipTypeId: "EMPLOYMENT"]
            run service: "createPartyRelationship", with: partyRelationshipCtx
        }
        partyRelationshipCtx.partyIdFrom = userLogin.partyId
        partyRelationshipCtx.partyIdTo = partyGroupPartyId
        partyRelationshipCtx.roleTypeIdFrom = "OWNER"
        partyRelationshipCtx.roleTypeIdTo = "ACCOUNT_LEAD"
        partyRelationshipCtx.partyRelationshipTypeId = "LEAD_OWNER"
        run service: "createPartyRelationship", with: partyRelationshipCtx
    }
    if (parameters.dataSourceId) {
        Map partyDataSourceCtx = [partyId: leadContactPartyId, dataSourceId: parameters.dataSourceId]
        Map serviceResultCPDS = run service: "createPartyDataSource", with: partyDataSourceCtx
        if (!ServiceUtil.isSuccess(serviceResultCPDS)) {
            return serviceResultCPDS
        }
    }
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
    Map result = success()
    Map deletePartyRelationship = [:]
    String partyId = parameters.partyId
    String partyGroupId = parameters.partyGroupId
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    GenericValue partyRelationship = from("PartyRelationship")
        .where(partyIdTo: partyId, roleTypeIdFrom: "OWNER", roleTypeIdTo: "LEAD", partyRelationshipTypeId: "LEAD_OWNER")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        deletePartyRelationship << partyRelationship
        deletePartyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: deletePartyRelationship
        logInfo("Expiring relationship  ${deletePartyRelationship}")
        partyRelationship = null
        deletePartyRelationship = [:]
    }
    // Expire relation between lead company and lead person
    partyRelationship = from("PartyRelationship")
        .where(partyIdFrom: partyGroupId, roleTypeIdTo: "LEAD", roleTypeIdFrom: "ACCOUNT_LEAD", partyRelationshipTypeId: "EMPLOYMENT")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        deletePartyRelationship << partyRelationship
        deletePartyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: deletePartyRelationship
        partyRelationship = null
        deletePartyRelationship = [:]
    }
    // Expire relation between lead company and its owner
    partyRelationship = from("PartyRelationship")
        .where(partyIdFrom: userLogin.partyId, partyIdTo: partyGroupId, roleTypeIdTo: "ACCOUNT_LEAD", roleTypeIdFrom: "OWNER")
        .filterByDate()
        .orderBy("-fromDate")
        .queryFirst()
    if (partyRelationship) {
        deletePartyRelationship << partyRelationship
        deletePartyRelationship.thruDate = nowTimestamp
        run service: "updatePartyRelationship", with: deletePartyRelationship
        partyRelationship = null
        deletePartyRelationship = [:]
    }
    Map partyRoleCtx = [partyId: partyGroupId, roleTypeId: "ACCOUNT"]
    run service: "ensurePartyRole", with: partyRoleCtx
    
    Map partyRelationshipCtx = [partyIdFrom: userLogin.partyId, partyIdTo: partyGroupId, roleTypeIdFrom: "OWNER", roleTypeIdTo: "ACCOUNT", partyRelationshipTypeId: "ACCOUNT"]
    run service: "createPartyRelationship", with: partyRelationshipCtx
    
    Map updatePartyCtx = [partyId: partyGroupId, statusId: "LEAD_CONVERTED"]
    run service: "setPartyStatus", with: updatePartyCtx
    
    Map createPartyRoleCtx = [partyId: partyId, roleTypeId: "CONTACT"]
    run service: "createPartyRole", with: createPartyRoleCtx
    
    // create new relationship between new account and contact person there
    partyRelationshipCtx = [partyIdFrom: partyGroupId, roleTypeIdFrom: "ACCOUNT", partyIdTo: partyId, roleTypeIdTo: "CONTACT", partyRelationshipTypeId: "EMPLOYMENT"]
    run service: "createPartyRelationship", with: partyRelationshipCtx
    
    updatePartyCtx = [partyId: partyId, statusId: "LEAD_CONVERTED"]
    run service: "setPartyStatus", with: updatePartyCtx
    
    result.partyId = partyId
    result.partyGroupId = partyGroupId
    result.successMessage = "Lead ${partyGroupId} ${partyId}  succesfully converted to Account/Contact"
    return result
}
