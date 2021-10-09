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

/**
 * Check user has Content Manager permission
 * @return
 */
def contentManagerPermission() {
    Map result = success()
    parameters.primaryPermission = "CONTENTMGR"
    Map resultService = run service: "genericBasePermissionCheck", with: parameters
    result.hasPermission = resultService.hasPermission
    return result
}

/**
 * Check user has Content Manager permission
 * @return
 */
def contentManagerRolePermission() {
    Map result = success()
    parameters.primaryPermission = "CONTENTMGR"
    parameters.altPermission = "CONTENTMGR_ROLE"
    Map resultService = run service: "genericBasePermissionCheck", with: parameters
    result.hasPermission = resultService.hasPermission
    return result
}

/**
 * Generic service for Content Permissions
 * @return
 */
def genericContentPermission() {
    String statusId = parameters.statusId
    String contentPurposeTypeId = parameters.contentPurposeTypeId
    String contentId = parameters.contentId
    String ownerContentId = parameters.ownerContentId
    String contentOperationId = parameters.contentOperationId
    String mainAction = parameters.mainAction
    parameters.primaryPermission = "CONTENTMGR"
    Map resultService = run service: "genericBasePermissionCheck", with: parameters
    Boolean hasPermission = resultService.hasPermission

    // setting the roleEntity or this service
    String roleEntityField = "contentId"
    String roleEntity = "ContentRole"
    // here we can use contentIdTo to check parent(s) ownership
    if (!parameters.ownerContentId && parameters.contentIdFrom) {
        ownerContentId = parameters.contentIdFrom
    }

    //  mainAction based call outs
    if (!hasPermission) {
        switch (parameters.mainAction) {
            case "VIEW":
                Map serviceVCP = viewContentPermission(hasPermission, contentId, contentOperationId,
                        contentPurposeTypeId, roleEntity, roleEntityField)
                hasPermission = serviceVCP.hasPermission ?: false
                break
            case "CREATE":
                contentOperationId = parameters.contentOperationId ?: "CONTENT_CREATE"
                Map serviceCCP = createContentPermission(hasPermission, ownerContentId, contentOperationId,
                        statusId, contentPurposeTypeId, roleEntity, roleEntityField)
                hasPermission = serviceCCP.hasPermission ?: false
                break
            case "UPDATE":
                contentOperationId = parameters.contentOperationId ?: "CONTENT_UPDATE"
                Map serviceUCP = updateContentPermission(hasPermission, contentId, ownerContentId,
                        contentOperationId, contentPurposeTypeId, roleEntity, roleEntityField)
                hasPermission = serviceUCP.hasPermission ?: false
        } // all other actions use main base check
    } else {
        logInfo("Admin permission found: ${parameters.primaryPermission}_${mainAction}")
    }
    logInfo("Permission service [${mainAction} / ${parameters.contentId}] completed; returning hasPermission = ${hasPermission}")

    return success(hasPermission: hasPermission)
}


/**
 * Check user can view content
 * @param hasPermission
 * @param contentId
 * @param contentOperationId
 * @param contentPurposeTypeId
 * @param roleEntity
 * @param roleEntityField
 * @return
 */
def viewContentPermission(Boolean hasPermission, String contentId, String contentOperationId,
                          String contentPurposeTypeId, String roleEntity,
                          String roleEntityField) {

    // if called directly check the main permission
    if (!hasPermission) {
        parameters.primaryPermission = "CONTENTMGR"
        parameters.mainAction = "VIEW"
        Map serviceResult = run service: "genericBasePermissionCheck", with: parameters
        hasPermission = serviceResult.hasPermission
    }

    // check content role permission
    parameters.primaryPermission = "CONTENTMGR_ROLE"
    Map serviceGBPC = run service: "genericBasePermissionCheck", with: parameters
    hasPermission = serviceGBPC.hasPermission

    // must have the security permission to continue
    if (hasPermission) {
        // if no operation is passed; we use the CONTENT_VIEW operation
        parameters.contentOperationId = parameters.contentOperationId ?: "CONTENT_VIEW"

        // contentId is required for update checking
        contentId = contentId ?: parameters.contentId
        if (!contentId) {
            return error(UtilProperties.getMessage('ContentUiLabels', 'ContentViewPermissionError'))
        }

        //check the operation security
        contentOperationId = parameters.contentOperationId
        String checkId = contentId
        Map serviceCCOS = checkContentOperationSecurity(contentOperationId, contentPurposeTypeId, checkId, roleEntity, roleEntityField)
        hasPermission = serviceCCOS.hasPermission
    }
    return success(hasPermission: hasPermission)
}

/**
 * Check user can create new content
 * @param hasPermission
 * @param ownerContentId
 * @param contentOperationId
 * @param statusId
 * @param contentPurposeTypeId
 * @param roleEntity
 * @param roleEntityField
 * @return
 */
def createContentPermission(Boolean hasPermission, String ownerContentId, String contentOperationId,
                            String statusId, String contentPurposeTypeId,
                            String roleEntity, String roleEntityField) {

    parameters.roleEntity = roleEntity
    parameters.roleEntityField = roleEntityField
    String checkId

    // if called directly check the main permission
    if (!hasPermission) {
        parameters.primaryPermission = "CONTENTMGR"
        parameters.mainAction = "CREATE"
        Map serviceResult = run service: "genericBasePermissionCheck", with: parameters
        hasPermission = serviceResult.hasPermission
    }

    // ownerContentId can be set from a calling method
    ownerContentId = ownerContentId ?: parameters.ownerContentId

    // operation ID can be set from the calling method
    contentOperationId = contentOperationId ?: parameters.contentOperationId

    // statusId can be set from the calling method
    statusId = statusId ?: parameters.statusId

    // check role permission?
    parameters.primaryPermission = "CONTENTMGR_ROLE"
    Map serviceResultGBPC = run service: "genericBasePermissionCheck", with: parameters
    hasPermission = serviceResultGBPC.hasPermission

    // must have the security permission to continue
    if (hasPermission) {
        logVerbose("Found necessary ROLE permission: ${parameters.primaryPermission}_${mainAction} :: ${contentOperationId}")

        // if an operation is passed, check the operation security
        if (contentOperationId) {
            checkId = ownerContentId
            Map serviceResultCCOS = checkContentOperationSecurity(contentOperationId, contentPurposeTypeId, checkId, roleEntity, roleEntityField)
            hasPermission = serviceResultCCOS.hasPermission
        }

        // check if there was no operation; or if the operation check failed, we are okay to create unless we are creating against a parent;
        // check parent ownership
        if (!contentOperationId || hasPermission == false) {
            if (ownerContentId) {
                logVerbose("No operation found; but ownerContentId [${ownerContentId}] was; checking ownership")
                checkId = ownerContentId
                logVerbose("Checking Parent Ownership [${checkId}]")
                parameters.checkId = checkId
                Map serviceResultCO = run service: "checkOwnership", with: parameters
                hasPermission = serviceResultCO.hasPermission ?: false

                if (!hasPermission) {
                    // no permission on this parent; check the parent's parent(s)
                    while (!hasPermission && checkId) {
                        // iterate until either we have permission or there are no more parents
                        GenericValue currentContent = from("Content").where(contentId: checkId).queryOne()
                        if (currentContent?.ownerContentId) {
                            checkId = currentContent.ownerContentId
                            logVerbose("Checking Parent(s) Ownership [${checkId}]")
                            parameters.checkId = checkId
                            Map serviceCO = run service: "checkOwnership", with: parameters
                            hasPermission = serviceCO.hasPermission ?: false
                        } else {
                            // no parent record found; time to stop recursion
                            checkId = null
                        }
                    }
                } else {
                    logVerbose("Permission set to TRUE; granting access")
                }
            }
        }
    }
    return success(hasPermission: hasPermission)
}

/**
 * Check user can update existing content
 * @param hasPermission
 * @param contentId
 * @param ownerContentId
 * @param contentOperationId
 * @param contentPurposeTypeId
 * @param roleEntity
 * @param roleEntityField
 * @return
 */
def updateContentPermission(Boolean hasPermission, String contentId, String ownerContentId,
                            String contentOperationId, String contentPurposeTypeId,
                            String roleEntity, String roleEntityField) {
    String checkId

    // if called directly check the main permission
    if (!hasPermission) {
        parameters.primaryPermission = "CONTENTMGR"
        parameters.mainAction = "UPDATE"
        parameters.roleEntity = roleEntity
        parameters.roleEntityField = roleEntityField
        Map serviceResult = run service: "genericBasePermissionCheck", with: parameters
        hasPermission = serviceResult.hasPermission
    }
    // contentId is required for update checking
    contentId = contentId ?: parameters.contentId
    if (!contentId) {
        return error(label('ContentUiLabels', 'ContentSecurityUpdatePermission'))
    }

    // ownerContentId can be set from a calling method
    ownerContentId = ownerContentId ?: parameters.ownerContentId

    // operation ID can be set from the calling method
    contentOperationId = contentOperationId ?: parameters.contentOperationId

    // check role permission
    parameters.primaryPermission = "CONTENTMGR_ROLE"
    Map resultService = run service: "genericBasePermissionCheck", with: parameters
    hasPermission = resultService.hasPermission ?: false


    // must have permission to continue
    if (hasPermission) {
        logVerbose("Found necessary ROLE permission: ${parameters.primaryPermission}_${mainAction}")

        // obtain the current content record
        GenericValue thisContent = from("Content").where(contentId: contentId).cache().queryOne()
        if (!thisContent) {
            return error(label("ContentUiLabels", "ContentNoContentFound"))
        }

        // check the operation
        if (contentOperationId) {
            logVerbose("Checking content operation for UPDATE: ${contentOperationId}")
            checkId = contentId
            Map serviceCCOS = checkContentOperationSecurity(contentOperationId, contentPurposeTypeId, checkId, roleEntity, roleEntityField)
            hasPermission = serviceCCOS.hasPermission ?: false
        }

        // check if there was no operation; or if the operation check failed
        if (!contentOperationId || !hasPermission) {
            // if no valid operation is passed; check ownership for permission
            logVerbose("No valid operation for UPDATE; checking ownership instead!")
            checkId = contentId
            parameters.checkId = checkId
            Map serviceCO = run service: "checkOwnership", with: parameters
            hasPermission = serviceCO.hasPermission ?: false

            // we are okay to update; unless we are updating the owner content; verify ownership there
            if (ownerContentId && thisContent.ownerContentId != ownerContentId) {
                logVerbose("Updating content ownership; need to verify permision on parent(s)")
                checkId = ownerContentId
                parameters.checkId = checkId
                Map serviceResultCO = run service: "checkOwnership", with: parameters
                    hasPermission = serviceResultCO.hasPermission ?: false
                }
                if (!hasPermission) {
                    // no permission on this parent; check the parent's parent(s)
                    while (!hasPermission && checkId) {
                        // iterate until either we have permission or there are no more parents
                        GenericValue currentContent = from("Content").where(contentId: checkId).cache().queryOne()
                        if (currentContent?.ownerContentId) {
                            checkId = currentContent.ownerContentId
                            parameters.checkId = checkId
                            Map serviceResCO = run service: "checkOwnership", with: parameters
                            hasPermission = serviceResCO.hasPermission ?: false
                        } else {
                            // no parent record found; time to stop recursion
                            checkId = null
                        }
                    }
                }
            }
        }
    return success(hasPermission: hasPermission)
}

/**
 * method to check operation security
 * @param contentOperationId
 * @param contentPurposeTypeId
 * @param checkId
 * @param roleEntity
 * @param roleEntityField
 * @return
 */
def checkContentOperationSecurity(String contentOperationId, String contentPurposeTypeId, String checkId,
                                  String roleEntity, String roleEntityField) {

    roleEntityField = parameters.roleEntityField
    roleEntity = parameters.roleEntity
    List operations = []
    // resetting the permission flag
    Boolean hasPermission = false
    if (!contentOperationId) {
        return error(label('ContentUiLabels', 'ContentRequiredField', [requiredField: contentOperationId]))
    }

    contentPurposeTypeId = contentPurposeTypeId ?: parameters.contentPurposeTypeId
    contentPurposeTypeId = contentPurposeTypeId ?: "_NA_"

    GenericValue checkContent = from("Content").where(contentId: checkId).cache().queryOne()
    String statusId = checkContent?.statusId

    // If operation is CONTENT_CREATE and contentPurposeTypeId exists in parameters than obtain operations
    // for that contentPurposeTypeId, else get the operations for checkContent
    if (contentOperationId == "CONTENT_CREATE" && contentPurposeTypeId) {
        // find defined purpose/operation mappings
        operations = from("ContentPurposeOperation")
                .where(contentPurposeTypeId: contentPurposeTypeId, contentOperationId: contentOperationId)
                .cache()
                .queryList()
    } else {
        // get all purposes for checkContent
        List contentPurposes = findAllContentPurposes(checkId)

        // check the _NA_ purpose but only if no other purposes were found
        if (contentPurposes) {
            // find defined purpose/operation mappings
            for (GenericValue currentPurpose : contentPurposes) {
                List currentOperations = from("ContentPurposeOperation")
                        .where(contentPurposeTypeId: currentPurpose.contentPurposeTypeId, contentOperationId: contentOperationId)
                        .orderBy("contentPurposeTypeId")
                        .cache()
                        .queryList()
                operations << currentOperations
            }
        } else {
            operations = from("ContentPurposeOperation")
                    .where(contentPurposeTypeId: "_NA_", contentOperationId: contentOperationId)
                    .orderBy("contentPurposeTypeId")
                    .cache()
                    .queryList()
        }
    }

    // place holder for the content ID
    String toCheckContentId = checkId
    logVerbose("[${checkId}] Found Operations [${contentPurposeTypeId}/${contentOperationId}] :: ${operations}")

    if (!operations) {
        // there are no ContentPurposeOperation entries for this operation/purpose; default is approve permission
        logVerbose("No operations found; permission granted!")
        hasPermission = true
    } else {
        // there are requirements to test
        //  get all possible partyIds for this user (including group memberships)
        List partyIdList = findAllAssociatedPartyIds()

        // check each operation security
        for (GenericValue operation : operations) {
            if (!hasPermission) {
                // reset the checkId if needed
                if (!checkId && toCheckContentId) {
                    checkId = toCheckContentId
                }
                logVerbose("Testing [${checkId}] [${statusId}] OPERATION: ${operation}")

                // check statusId
                if (operation.statusId == "_NA_" || (statusId && (operation.statusId == statusId))) {
                    logVerbose("Passed status check; now checking role(s)")

                    // first check passed; now we test for the role membership(s)
                    for (String thisPartyId : partyIdList) {
                        if (!hasPermission) {
                            String checkRoleTypeId = operation.roleTypeId
                            String checkPartyId = thisPartyId
                            // reset the checkId if needed
                            if (!checkId && toCheckContentId) {
                                checkId = toCheckContentId
                            }
                            Map serviceCRS = checkRoleSecurity(roleEntity, roleEntityField, checkId, checkPartyId, checkRoleTypeId)
                            hasPermission = serviceCRS.hasPermission ?: false

                            // check the parent(s) for permission
                            if (!hasPermission && checkId) {
                                logVerbose("Starting loop; checking operation: ${operation.contentOperationId}")
                                // iterate until either we have permission or there are no more parents
                                while (!hasPermission && checkId) {
                                    GenericValue currentContent = from("Content").where(contentId: checkId).queryOne()
                                    if (currentContent?.ownerContentId) {
                                        checkId = currentContent.ownerContentId
                                        Map serviceResultCRS = checkRoleSecurity(roleEntity, roleEntityField, checkId, checkPartyId, checkRoleTypeId)
                                        hasPermission = serviceResultCRS.hasPermission ?: false
                                    } else {
                                        // no parent record found; time to stop recursion
                                        checkId = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return success(hasPermission: hasPermission)
}

// method to check content ownership
/**
 * Checks the (role) ownership of a record
 * @return
 */
def checkOwnership() {
    Map result = success()
    String roleEntity = parameters.roleEntity
    String roleEntityField = parameters.roleEntityField
    String checkId = parameters.checkId
    String checkPartyId
    // resetting the permission flag
    Boolean hasPermission = false

    if (!checkId) {
        return error(label("ContentUiLabels", "ContentRequiredField", [requiredField: "checkId"]))
    }

    // get all the associated parties (this user + all group memberships)
    List partyIdList = findAllAssociatedPartyIds()

    // check to see if any of the parties are owner of the content
    for (String thisPartyId : partyIdList) {
        if (!hasPermission) {
            logVerbose("Checking to see if party [${thisPartyId}] has ownership of ${checkId} :: ${hasPermission}")
            checkPartyId = thisPartyId
            Map serviceResult = checkRoleSecurity(roleEntity, roleEntityField, checkId, checkPartyId, "OWNER")
            if (serviceResult.errorMessage != null) {
                logError(serviceResult.errorMessage)
            } else {
                hasPermission = serviceResult.hasPermission
            }
        } else {
            logVerbose("Field hasPermission is TRUE [${hasPermission}] did not test!")
        }
    }
    return success(hasPermission: hasPermission)
}

// method the check Content Role associations
/**
 * Check users role associations with Content
 * @param roleEntity
 * @param roleEntityField
 * @param checkId
 * @param checkPartyId
 * @param checkRoleTypeId
 * @return
 */
def checkRoleSecurity(String roleEntity, String roleEntityField, String checkId, String checkPartyId, String checkRoleTypeId) {
    Boolean hasPermission
    List foundRoles
    logVerbose("checkRoleSecurity: just reset hasPermission value to false!")

    List missingFields = []

    if (!roleEntity)      missingFields << "roleEntity"      // roleEntity is required to determine which content role table to look: ContentRole, DataResourceRole, etc
    if (!roleEntityField) missingFields << "roleEntityField" // roleEntityField is required to determine the pk field to check; contentId, dataResourceId, etc
    if (!checkId)         missingFields << "checkId"         // setting the env field contentId is required for this simple method
    if (!checkPartyId)    missingFields << "checkPartyId"
    if (missingFields) {
        return error(label('ContentUiLabels', 'ContentRequiredField', [requiredField: missingFields]))
    }
    logVerbose("About to test of checkRoleTypeId is empty... ${checkRoleTypeId}")

    if (checkRoleTypeId && checkRoleTypeId == "_NA_") {
        // _NA_ role means anyone (logged in) has permission
        hasPermission = true
    } else {
        // not _NA_ so do the actual role check
        if (checkRoleTypeId) {
            logVerbose("Doing lookup [${roleEntity}] with roleTypeId : ${checkRoleTypeId}")
            // looking up a specific role
            hasPermission = from("${roleEntity}")
                    .where(["${roleEntityField}": checkId,
                            roleTypeId          : checkRoleTypeId,
                            partyId             : checkPartyId])
                    .queryCount() > 0
        } else {
            logVerbose("Doing lookup without roleTypeId")
            // looking up any role
            hasPermission = from("${roleEntity}")
                    .where(["${roleEntityField}": checkId,
                            partyId             : checkPartyId])
                    .queryCount() > 0

        }
        logVerbose("Checking for ContentRole: [party] - ${checkPartyId} [role] - ${checkRoleTypeId} [content] - ${checkId} :: ${foundRoles}")
    }
    return success(hasPermission: hasPermission)
}

/**
 * Find all content purposes for the specified content
 * @param checkId
 * @return
 */
def findAllContentPurposes(String checkId) {
    if (!checkId) {
        return error(label('ContentUiLabels', 'ContentRequiredField', [requiredField: "checkId"]))
    }
    return from("ContentPurpose")
            .where(contentId: checkId)
            .cache()
            .queryList()
}

/**
 * Finds all associated party Ids for a use
 * @return
 */
def findAllAssociatedPartyIds () {
    Map serviceResult = run service: "getRelatedParties", with: [partyIdFrom            : userLogin.partyId,
                                                                 partyRelationshipTypeId: "GROUP_ROLLUP",
                                                                 includeFromToSwitched  : "Y"]
    List partyIdList = serviceResult.relatedPartyIdList

    logVerbose("Got list of associated parties: ${partyIdList}")
    return partyIdList
}

/**
 * Finds all associated parent content
 * @return
 */
def findAllParentContent(String contentId) {
    if (!contentId) {
        return error(label('ContentUiLabels', 'ContentRequiredField', [requiredField:  "contentId"]))
    }
    return success(contentAssocList: from("ContentAssoc")
            .where(contentIdTo: contentId)
            .filterByDate()
            .cache()
            .queryList())
}
