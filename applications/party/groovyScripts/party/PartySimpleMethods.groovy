
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
import org.apache.ofbiz.minilang.SimpleMapProcessor
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.StringUtil

// Simple method to create a party group, its role and basic contact mechs

/**
 * Creates a party group, role and contactMechs
 * @return
 */
def createPartyGroupRoleAndContactMechs() {
    Map result = success()
    String successMessage
    List<String> messages = []
    Map partyGroupContext = [:]
    // TODO need to convert from MapProcessor
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/party/PartyMapProcs.xml', 'partyGroup', parameters, partyGroupContext, messages, context.locale)
    // Check errors
    if (messages) return error(StringUtil.join(messages, ','))

    if (parameters.address1) {
        Map postalAddressContext = [:]
        // TODO need to convert from MapProcessor
        SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/contact/PartyContactMechMapProcs.xml', 'postalAddress', parameters, postalAddressContext, messages, context.locale)
        // Check errors
        if (messages) return error(StringUtil.join(messages, ','))
    }

    if (parameters.contactNumber) {
        Map telecomNumberContext = [:]
        // TODO need to convert from MapProcessor
        SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/contact/PartyContactMechMapProcs.xml', 'telecomNumber', parameters, telecomNumberContext, messages, context.locale)
        // Check errors
        if (messages) return error(StringUtil.join(messages, ','))
    }

    if (parameters.emailAddress) {
        Map emailAddressContext = [:]
        if  (!UtilValidate.isEmail(parameters.emailAddress)) {
            return error(UtilProperties.getMessage('PartyUiLabels', 'PartyEmailAddressNotFormattedCorrectly', parameters.locale))
        } else {
            emailAddressContext.emailAddress = parameters.emailAddress
        }
    }

    partyGroupContext.partyTypeId = "PARTY_GROUP"
    Map serviceResult = run service:"createPartyGroup", with: partyGroupContext
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    result.partyId = serviceResult.partyId

    if(parameters.roleTypeId) {
        Map createPartyRoleCtx = [partyId: serviceResult.partyId, roleTypeId: parameters.roleTypeId]
        Map serviceResultCPR = run service:"createPartyRole", with: createPartyRoleCtx
        if (!ServiceUtil.isSuccess(serviceResultCPR)) {
            return serviceResultCPR
        }
        successMessage = serviceResultCPR.successMessage
    }
    Map inputMap = [
        postalAddContactMechPurpTypeId: parameters.postalAddContactMechPurpTypeId,
        contactNumber: parameters.contactNumber,
        phoneContactMechPurpTypeId: parameters.phoneContactMechPurpTypeId,
        emailAddress: parameters.emailAddress,
        emailContactMechPurpTypeId: parameters.emailContactMechPurpTypeId]

    run service:"createPartyContactMechs", with: inputMap

    result.successMessage = successMessage
    return result
}
