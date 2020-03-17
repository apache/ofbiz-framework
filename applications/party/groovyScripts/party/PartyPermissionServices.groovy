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

// ============== Basic Permission Checking =============

//Returns hasPermission=true if user has one of the base PARTYMGR CRUD+ADMIN permissions
/**
 * Party Manager base permission logic
 */
def basePermissionCheck() {
    parameters.primaryPermission = "PARTYMGR"
    Map serviceResult = run service: "genericBasePermissionCheck", with: parameters
    return serviceResult
}

//Returns hasPermission=true if userLogin partyId equals partyId parameter
/**
 * Party ID Permission Check
 */
def partyIdPermissionCheck(Map parameters) {
    Map result = success()
    Boolean hasPermission
    String partyId = parameters.partyId

    if (partyId && userLogin.partyId && partyId == userLogin.partyId) {
        hasPermission = true
    } else {
        String resourceDescription = parameters.resourceDescription
        if (!resourceDescription) {
            resourceDescription = UtilProperties.getPropertyValue("CommonUiLabels", "CommonPermissionThisOperation")
        }
        String failMessage = UtilProperties.getMessage("PartyUiLabels",
                "PartyPermissionErrorPartyId", [resourceDescription: resourceDescription], parameters.locale)
        hasPermission = false
        result.failMessage = failMessage
    }
    result.hasPermission = hasPermission
    return result
}

//Returns hasPermission=true if userLogin party equals partyId parameter OR
//      user has one of the base PARTYMGR CRUD+ADMIN permissions
/**
 * Base Permission Plus Party ID Permission Check
 */
def basePlusPartyIdPermissionCheck() {
    Map result = run service: "basePermissionCheck", with: parameters
    if (!result.hasPermission) {
        result = partyIdPermissionCheck(parameters)
    }
    return result
}

// ============== Additional Permission Checking =============

//Returns hasPermission=true if userLogin partyId equals partyId parameter OR
//       user has one of the base PARTYMGR or PARTYMGR_STS CRUD+ADMIN permissions
/**
 * Party status permission logic
 */
def partyStatusPermissionCheck() {
    Map result = success()
    Boolean hasPermission = false
    if (parameters.partyId && parameters.partyId == userLogin.partyId) {
        hasPermission = true
        result.hasPermission = hasPermission
    }
    if (!hasPermission) {
        parameters.altPermission = "PARTYMGR_STS"
        result = run service: "basePermissionCheck", with: parameters
    }
    return result
}

//Returns hasPermission=true if userLogin partyId equals partyId parameter OR
//       user has one of the base PARTYMGR or PARTYMGR_GRP CRUD+ADMIN permissions
/**
 * Party group permission logic
 */
def partyGroupPermissionCheck() {
    parameters.altPermission = "PARTYMGR_GRP"
    Map result = run service: "partyStatusPermissionCheck", with: parameters
    return result
}

//Returns hasPermission=true if user has one of the base PARTYMGR or PARTYMGR_SRC CRUD+ADMIN permissions
/**
 * Party datasource permission logic
 */
def partyDatasourcePermissionCheck() {
    parameters.altPermission = "PARTYMGR_SRC"
    Map result = run service: "basePermissionCheck", with: parameters
    return result
}

//Returns hasPermission=true if user has one of the base PARTYMGR or PARTYMGR_ROLE CRUD+ADMIN permissions
/**
 * Party role permission logic
 */
def partyRolePermissionCheck() {
    parameters.altPermission = "PARTYMGR_ROLE"
    Map result = run service: "partyStatusPermissionCheck", with: parameters
    return result
}

//Returns hasPermission=true if user has one of the base PARTYMGR or PARTYMGR_REL CRUD+ADMIN permissions
/**
 * Party relationship permission logic
 */
def partyRelationshipPermissionCheck() {
    Map result = success()
    if (!parameters.partyIdFrom) {
        parameters.partyIdFrom = userLogin.partyId
        result.hasPermission = true
    } else {
        parameters.altPermission = "PARTYMGR_REL"
        result = run service: "basePermissionCheck", with: parameters
    }
    return result
}

//Returns hasPermission=true if userLogin partyId equals partyId parameter OR
//       user has one of the base PARTYMGR or PARTYMGR_PCM CRUD+ADMIN permissions
/**
 * Party contact mech permission logic
 */
def partyContactMechPermissionCheck() {
    Map result = success()
    if (!parameters.partyId || userLogin.partyId == parameters.partyId) {
        Boolean hasPermission = true
        result.hasPermission = hasPermission
    } else {
        parameters.altPermission = "PARTYMGR_PCM"
        result = run service: "basePermissionCheck", with: parameters
    }
    return result
}

//Accept/Decline PartyInvitation Permission Checks
/**
 * Accept and Decline PartyInvitation Permission Logic
 */
def accAndDecPartyInvitationPermissionCheck() {
    Map result = success()
    Boolean hasPermission = false
    if (security.hasEntityPermission("PARTYMGR_UPDATE", "_UPDATE", parameters.userLogin)) {
        hasPermission = true
        result.hasPermission = hasPermission
    }
    if (!hasPermission) {
        GenericValue partyInvitation = from("PartyInvitation").where(parameters).queryOne()
        if (!partyInvitation?.partyId) {
            if (!partyInvitation?.emailAddress) {
                return error(UtilProperties.getMessage("PartyUiLabels",
                        "PartyInvitationNotValidError", parameters.locale))
            } else {
                Map serviceResult = run service: "findPartyFromEmailAddress", with: [address: partyInvitation.emailAddress]
                String partyId = serviceResult.partyId
                if (partyId && partyId == userLogin.partyId) {
                    hasPermission = true
                    result.hasPermission = hasPermission
                } else {
                    return error(UtilProperties.getMessage("PartyUiLabels",
                            "PartyInvitationNotValidError", parameters.locale))
                }
            }
        } else {
            if (partyInvitation.partyId == userLogin.partyId) {
                hasPermission = true
                result.hasPermission = hasPermission
            }
        }
    }
    if (!hasPermission) {
        String failMessage = UtilProperties.getMessage("PartyUiLabels", "PartyInvitationAccAndDecPermissionError", parameters.locale)
        logWarning(failMessage)
        result.failMessage = failMessage
        result.hasPermission = hasPermission
    }
    return result
}

//Cancel PartyInvitation Permission Checks
/**
 * Cancel PartyInvitation Permission Logic
 */
def cancelPartyInvitationPermissionCheck() {
    Map result = success()
    Boolean hasPermission = false
    if (security.hasEntityPermission("PARTYMGR_UPDATE", "_UPDATE", parameters.userLogin)) {
        hasPermission = true
        result.hasPermission = hasPermission
    }
    if (!hasPermission) {
        GenericValue partyInvitation = from("PartyInvitation").where(parameters).queryOne()
        if (partyInvitation?.partyIdFrom
                && partyInvitation.partyIdFrom == userLogin.partyId) {
            hasPermission = true
            result.hasPermission = hasPermission
        }
        if (!hasPermission) {
            if (!partyInvitation?.partyId) {
                if (!partyInvitation?.emailAddress) {
                    String errorMessage = UtilProperties.getMessage("PartyUiLabels", "PartyInvitationNotValidError", parameters.locale)
                    logError(errorMessage)
                    return error(errorMessage)
                } else {
                    Map findPartyCtx = [address: partyInvitation.emailAddress]
                    Map serviceResult = run service: "findPartyFromEmailAddress", with: findPartyCtx
                    String partyId = serviceResult.partyId
                    if (partyId) {
                        if (partyId == userLogin.partyId) {
                            hasPermission = true
                            result.hasPermission = hasPermission
                        }
                    } else {
                        String errorMessage = UtilProperties.getMessage("PartyUiLabels", "PartyInvitationNotValidError", parameters.locale)
                        logError(errorMessage)
                        return error(errorMessage)
                    }
                }
            } else {
                if (partyInvitation?.partyId == userLogin.partyId) {
                    hasPermission = true
                    result.hasPermission = hasPermission
                }
            }
        }
    }
    if (!hasPermission) {
        String failMessage = UtilProperties.getMessage("PartyUiLabels", "PartyInvitationCancelPermissionError", parameters.locale)
        logWarning(failMessage)
        result.failMessage = failMessage
        result.hasPermission = hasPermission
    }
    return result
}

//Returns hasPermission=true if userLogin partyId equals partyIdFrom parameter OR
//       partyIdTo parameter OR user has one of the base PARTYMGR or PARTYMGR_CME CRUD+ADMIN permissions
/**
 * Communication Event permission logic
 */
def partyCommunicationEventPermissionCheck() {
    Map result = success()
    if (parameters.communicationEventTypeId == "EMAIL_COMMUNICATION" && parameters.mainAction == "CREATE") {
        parameters.altPermission = "PARTYMGR_CME-EMAIL"
    } else if (parameters.communicationEventTypeId == "COMMENT_NOTE" && parameters.mainAction == "CREATE") {
        parameters.altPermission = "PARTYMGR_CME-NOTE"
    } else if (parameters.partyIdFrom != userLogin.partyId
            && parameters.partyIdTo != userLogin.partyId
            && parameters.partyId != userLogin.partyId) { // <- update role
        parameters.altPermission = "PARTYMGR_CME"
    } else {
        result.hasPermission = true
    }
    if (!result.hasPermission) {
        result = run service: "basePermissionCheck", with: parameters
    }
    return result
}