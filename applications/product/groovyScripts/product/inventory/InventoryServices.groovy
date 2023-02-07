import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.service.ServiceUtil

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

/**
 * Check Facility Related Permission
 *
 * A method to centralize facility security code, meant to be called in-line with
 * call-simple-method, and the checkAction and callingMethodName attributes should be in the method context
 *
 * @param callingMethodName
 * @param checkAction The permission action to test for.
 * @return Success response if permission is granted, error response otherwise with the error message describing
 * the missing permission.
 */
def checkFacilityRelatedPermission(String callingMethodName, String checkAction, String alternatePermissionRoot) {
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    if (!checkAction) {
        checkAction = "UPDATE"
    }
    if (!security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)
            && (!security.hasEntityPermission("FACILITY", "_${checkAction}", parameters.userLogin))
            && ((!alternatePermissionRoot) || !security.hasEntityPermission("${alternatePermissionRoot}", "_${checkAction}", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", parameters.locale))
    }
    return success();
}


/**
 * Main permission logic
 * @return
 */
def facilityGenericPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale))
    }
    String callingMethodName = parameters.resourceDescription
    Map permissionResult = checkFacilityRelatedPermission(callingMethodName, mainAction, null)
    if (ServiceUtil.isSuccess(permissionResult)) {
        Map result = success()
        result.hasPermission = true
        return result
    } else {
        Map result = failure()
        result.hasPermission = false
        result.failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductFacilityPermissionError", binding.variables, parameters.locale)
        return result
    }
}

/**
 * ProductFacility Permission Checking Logic
 * @return
 */
def checkProductFacilityRelatedPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonPermissionMainActionAttributeMissing", parameters.locale))
    }
    String resourceDescription = parameters.resourceDescription
    if (!resourceDescription) {
        resourceDescription = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    parameters.altPermission = "FACILITY"
    Map serviceResult = run service: "checkProductRelatedPermission", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        Map result = failure()
        result.hasPermission = false
        result.failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductFacilityPermissionError", binding.variables, parameters.locale)
        return result
    } else {
        Map result = success()
        result.hasPermission = true
        return result
    }
}

