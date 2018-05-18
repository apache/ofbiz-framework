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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

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
