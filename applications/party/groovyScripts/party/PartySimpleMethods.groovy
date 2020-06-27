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
import org.apache.ofbiz.service.GenericServiceException
import org.apache.ofbiz.service.ServiceUtil

// Simple method to create a party group, its role and basic contact mechs

/**
 * Creates a party group, role and contactMechs
 * @return
 */
def createPartyGroupRoleAndContactMechs() {
    try {
        parameters.partyGroupContext = resolvePartyGroupMap(parameters)
        if (parameters.address1) {
            parameters.postalAddressContext = resolvePostalAddressMap()
        }
        if (parameters.contactNumber) {
            parameters.telecomNumberContext = resolveTelecomNumberMap()
        }
    } catch (GenericServiceException e) {
        return error(e.toString())
    }

    if (parameters.emailAddress) {
        Map emailAddressContext = [:]
        if  (!UtilValidate.isEmail(parameters.emailAddress)) {
            return error(UtilProperties.getMessage('PartyUiLabels',
                    'PartyEmailAddressNotFormattedCorrectly', parameters.locale))
        } else {
            emailAddressContext.emailAddress = parameters.emailAddress
        }
        parameters.emailAddressContext = emailAddressContext
    }

    parameters.partyGroupContext.partyTypeId = "PARTY_GROUP"
    Map serviceResult = run service: "createPartyGroup", with: parameters.partyGroupContext
    if (ServiceUtil.isError(serviceResult)) {
        return serviceResult
    }
    Map result = success()
    result.partyId = serviceResult.partyId

    if (parameters.roleTypeId) {
        Map serviceResultCPR = run service: "createPartyRole", with: [partyId: serviceResult.partyId,
                                                                     roleTypeId: parameters.roleTypeId]
        if (ServiceUtil.isError(serviceResultCPR)) {
            return serviceResultCPR
        }
    }

    run service:"createPartyContactMechs", with: parameters

    return result
}

// TODO need to convert from MapProcessor
def resolvePartyGroupMap() {
    return resolvePartyProcessMap('partyGroup')
}
def resolvePostalAddressMap() {
    return resolvePartyProcessMap('postalAddress')
}
def resolveTelecomNumberMap() {
    return resolvePartyProcessMap('telecomNumber')
}
def resolvePartyProcessMap(String processMapName) {
    List messages = []
    Map resultMap = [:]
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/party/PartyMapProcs.xml',
            processMapName, parameters, resultMap, messages, context.locale)
    // Check errors
    if (messages) {
        throw new GenericServiceException(messages.join(','))
    }
    return resultMap
}