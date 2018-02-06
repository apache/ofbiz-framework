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


def genericBasePermissionCheck() {
    Map result = success()
    // allow mainAction to be set from outside methods or direct to the service
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("CommonUiLabels","CommonPermissionMainActionAttributeMissing", parameters.locale))
    }

    // allow primary permission to be set from outside methods or direct to the service
    String primaryPermission = parameters.primaryPermission
    if (!primaryPermission) {
        return error(UtilProperties.getMessage("CommonUiLabels","CommonPermissionPrimaryPermissionMissing", parameters.locale))
    }
    logVerbose("Checking for primary permission ${primaryPermission}_${mainAction}")

    // allow alt permission to be set from outside methods or direct to the service
    String altPermission = parameters.altPermission
    String altPermissionList = ""
    if (altPermission) {
        logInfo("Checking for alternate permission ${altPermission}_${mainAction}")
        altPermissionList = ", ${altPermission}_${mainAction}, ${altPermission}_ADMIN"
    }
    // altPermission is not a required field; no need to add Error

    // set up called service name
    String resourceDescription = parameters.resourceDescription
    if (!resourceDescription) {
        resourceDescription = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }

    // check permission, permission checks include _ADMIN
    if (security.hasEntityPermission(primaryPermission, "_${mainAction}", parameters.userLogin)
        || security.hasEntityPermission(altPermission, "_${mainAction}", parameters.userLogin)) {
        result.hasPermission = true
    } else {
        result.hasPermission = false
        messageContext = [resourceDescription: resourceDescription,
                          primaryPermission: primaryPermission,
                          mainAction: mainAction,
                          altPermissionList: altPermissionList]
        result.failMessage = UtilProperties.getMessage("CommonUiLabels", "CommonGenericPermissionError", messageContext, parameters.locale)
    }
    return result
}

/**
 * Get all CRUD and View Permissions
 */
def getAllCrudPermissions() {
    Map result = success()
    result.hasCreatePermission = false
    result.hasUpdatePermission = false
    result.hasDeletePermission = false
    result.hasViewPermission = false
    def primaryPermission = parameters.primaryPermission
    if (!primaryPermission) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonPermissionPrimaryPermissionMissing", parameters.locale))
    }
    logInfo("Getting all CRUD permissions for ${primaryPermission}")
    result = hasCrudPermission(primaryPermission, result)

    def altPermission = parameters.altPermission
    if (altPermission) {
        logInfo("Getting all CRUD permissions for ${altPermission}")
        result = hasCrudPermission(altPermission, result)
    }
    return result
}

def hasCrudPermission(String perm, Map resultMap) {
    if (security.hasEntityPermission(perm, "_CREATE", parameters.userLogin)) {
        resultMap.hasCreatePermission = true
    }
    if (security.hasEntityPermission(perm, "_UPDATE", parameters.userLogin)) {
        resultMap.hasUpdatePermission = true
    }
    if (security.hasEntityPermission(perm, "_DELETE", parameters.userLogin)) {
        resultMap.hasDeletePermission = true
    }
    if (security.hasEntityPermission(perm, "_VIEW", parameters.userLogin)) {
        resultMap.hasViewPermission = true
    }
    return resultMap
}

/**
 * Visual Theme permission logic
 */
def visualThemePermissionCheck() {
    parameters.primaryPermission = "VISUAL_THEME"
    Map result = run service: "genericBasePermissionCheck", with: parameters
    return result
}
