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
package org.apache.ofbiz.product.product.inventory

import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

/**
 * Check Facility Related Permission
 *
 * A method to centralize facility security code, meant to be called in-line with
 * call-simple-method, and the checkAction and callingMethodName attributes should be in the method context
 *
 * @param callingMethodName Name of the calling method.
 * @param checkAction The permission action to test for.
 * @return Success response if permission is granted, error response otherwise with the error message describing
 * the missing permission.
 */
Map checkFacilityRelatedPermission(String callingMethodName, String checkAction, String alternatePermissionRoot) {
    callingMethodName = callingMethodName ?: UtilProperties.getMessage('CommonUiLabels', 'CommonPermissionThisOperation', parameters.locale)
    checkAction = checkAction ?: 'UPDATE'

    if (!security.hasEntityPermission('CATALOG', "_${checkAction}", parameters.userLogin)
            && (!security.hasEntityPermission('FACILITY', "_${checkAction}", parameters.userLogin))
            && ((!alternatePermissionRoot) || !security.hasEntityPermission("${alternatePermissionRoot}", "_${checkAction}", parameters.userLogin))) {
        return error(UtilProperties.getMessage('ProductUiLabels', 'ProductCatalogCreatePermissionError', parameters.locale))
    }
    return success()
}

/**
 * Main permission logic
 * @return Map with hasPermission boolean and potential failure message.
 */
Map facilityGenericPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage('ProductUiLabels', 'ProductMissingMainActionInPermissionService', parameters.locale))
    }
    String callingMethodName = parameters.resourceDescription
    Map permissionResult = checkFacilityRelatedPermission(callingMethodName, mainAction, null)
    if (ServiceUtil.isSuccess(permissionResult)) {
        Map result = success()
        result.hasPermission = true
        return result
    }
    Map result = failure()
    result.hasPermission = false
    result.failMessage = UtilProperties.getMessage('ProductUiLabels', 'ProductFacilityPermissionError', binding.variables, parameters.locale)
    return result
}

/**
 * ProductFacility Permission Checking Logic
 * @return Map with hasPermission boolean and potential failure message.
 */
Map checkProductFacilityRelatedPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage('CommonUiLabels', 'CommonPermissionMainActionAttributeMissing', parameters.locale))
    }
    parameters.altPermission = 'FACILITY'
    Map serviceResult = run service: 'checkProductRelatedPermission', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        Map result = failure()
        result.hasPermission = false
        result.failMessage = UtilProperties.getMessage('ProductUiLabels', 'ProductFacilityPermissionError', binding.variables, parameters.locale)
        return result
    }
    Map result = success()
    result.hasPermission = true
    return result
}

/**
 * Create an InventoryItem
 */
Map createInventoryItem() {
    GenericValue product = from('Product').where(productId: parameters.productId).queryOne()

    // Check if this product can or not have a lotId
    if (product.lotIdFilledIn == 'Mandatory' && !parameters.lotId) {
        return error(label('ProductErrorUiLabels', 'ProductLotIdMandatory', [parameters: parameters]))
    } else if (product.lotIdFilledIn == 'Forbidden' && parameters.lotId) {
        return error(label('ProductErrorUiLabels', 'ProductLotIdForbidden', [parameters: parameters]))
    }

    // If this InventoryItem is returned by a manufacturing task, don't create a lot
    if (parameters.isReturned == 'N' && parameters.lotId) {
        // Create the lot if if doesn't already exist.
        List<GenericValue> lotList = from('Lot').where(lotId: parameters.lotId).queryList()
        if (!lotList) {
            GenericValue lot = makeValue('Lot')
            lot.lotId = parameters.lotId
            lot.create()
        }
    }

    GenericValue inventoryItem = makeValue('InventoryItem')
    // TODO: make sure availableToPromiseTotal and quantityOnHandTotal are not changed
    inventoryItem.setNonPKFields(parameters)

    if (!inventoryItem.facilityId) {
        return error(label('ProductUiLabels', 'FacilityInventoryItemsMissingFacilityId'))
    }

    // if inventoryItem's ownerPartyId is empty, get the ownerPartyId from the facility
    if (!inventoryItem.ownerPartyId) {
        GenericValue facility = delegator.getRelatedOne('Facility', inventoryItem, false)
        inventoryItem.ownerPartyId = facility.ownerPartyId
        // if inventoryItem's ownerPartyId is still empty, return an error message
        if (!inventoryItem.ownerPartyId) {
            return error(label('ProductUiLabels', 'FacilityInventoryItemsMissingOwnerPartyId'))
        }
    }

    // if inventoryItem's currencyUomId is empty, get the currencyUomId
    // from the party accounting preferences of the owner of the inventory item
    if (!inventoryItem.currencyUomId) {
        Map partyAccountingPreferencesCallMap = [organizationPartyId: inventoryItem.ownerPartyId]
        Map serviceResult = run service: 'getPartyAccountingPreferences', with: partyAccountingPreferencesCallMap
        Map accPref = serviceResult.partyAccountingPreference
        if (accPref) {
            inventoryItem.currencyUomId = accPref.baseCurrencyUomId
        }
        inventoryItem.currencyUomId = inventoryItem.currencyUomId ?: UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default')

        // if inventoryItem's currencyUomId is still empty, return an error message
        if (!inventoryItem.currencyUomId) {
            return error(label('ProductUiLabels', 'FacilityInventoryItemsMissingCurrencyId'))
        }
    }

    // if inventoryItem's unitCost is empty, get the product's standard
    // cost by calling the getProductCost service
    if (!inventoryItem.unitCost) {
        // TODO: create a new service getProductStdCost that calls getProductCost
        Map inputMap = [productId: inventoryItem.productId, currencyUomId: inventoryItem.currencyUomId, costComponentTypePrefix: 'EST_STD']
        Map productCostResult = run service: 'getProductCost', with: inputMap
        if (!ServiceUtil.isSuccess(productCostResult)) {
            return productCostResult
        }
        inventoryItem.unitCost = productCostResult.productCost
    }

    // if inventoryItem's unitCost is still empty, or negative return an error message
    // TODO/WARNING: getProductCost returns 0 even if no std costs are found
    if (!inventoryItem.unitCost && inventoryItem.unitCost != (BigDecimal) 0) {
        return error(label('ProductUiLabels', 'FacilityInventoryItemsMissingUnitCost'))
    }

    // if you don't want inventory item with unitCost = 0, change the operator
    // attribute from "less" to "less-equals".
    if (inventoryItem.unitCost < (BigDecimal) 0) {
        return error(label('ProductUiLabels', 'FacilityInventoryItemsNegativeUnitCost'))
    }

    inventoryItem.inventoryItemId = delegator.getNextSeqId('InventoryItem')
    inventoryItem.create()

    return success([inventoryItemId: inventoryItem.inventoryItemId])
}
