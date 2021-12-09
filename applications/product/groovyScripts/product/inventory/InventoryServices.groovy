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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityTypeUtil
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil


/*
 * a method to centralize facility security code, meant to be called in-line with
 * call-simple-method, and the checkAction and callingMethodName attributes should be in the method context
 */

/**
 * Check Facility Related Permission
 * @param callingMethodName
 * @param checkAction
 * @return
 */
def checkFacilityRelatedPermission(String callingMethodName, String checkAction, String alternatePermissionRoot) {
    Map result = success()
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
    return result;
}

/**
 * Main permission logic
 * @return
 */
def facilityGenericPermission() {
    Map result = success()
    boolean hasPermission = false;
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale))
    }
    String callingMethodName = parameters.resourceDescription
    Map permissionResult = checkFacilityRelatedPermission(callingMethodName, mainAction, null)
    if (ServiceUtil.isSuccess(permissionResult)) {
        hasPermission = true
    } else {
        result.failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductFacilityPermissionError", parameters.locale)
    }
    result.hasPermission = hasPermission
    return result
}

/**
 * ProductFacility Permission Checking Logic
 * @return
 */
def checkProductFacilityRelatedPermission() {
    Map result = success()
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
        result.hasPermission = false
        result.failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductFacilityPermissionError", parameters.locale)
    } else {
        result.hasPermission = true
    }
    return result
}

// InventoryItem methods

/**
 * Create an InventoryItem
 * @return
 */
def createInventoryItem() {
    Map result = success()
    // Create a lot before
    GenericValue product = from("Product").where(productId: parameters.productId).queryOne()
    // Check if this product can or not have a lotId
    if ("Mandatory".equals(product.lotIdFilledIn) && (!parameters.lotId)) {
        return error(UtilProperties.getMessage("ProductErrorUiLabels", "ProductLotIdMandatory", parameters.locale))
    }
    if ("Forbidden".equals(product.lotIdFilledIn) && (!parameters.lotId)) {
        return error(UtilProperties.getMessage("ProductErrorUiLabels", "ProductLotIdForbidden", parameters.locale))
    }
    // If this InventoryItem is returned by a manufacturing task, don't create a lot
    if ("N".equals(parameters.isReturned) && (parameters.lotId)) {
        // Check if the lot already exists
        List<GenericValue> lotList = from("Lot").where(lotId: parameters.lotId).queryList()
        if (!lotList) {
            GenericValue lot = makeValue("Lot")
            lot.lotId = parameters.lotId
            lot.create()
        }
    }
    GenericValue inventoryItem = makeValue("InventoryItem")
    // TODO: make sure availableToPromiseTotal and quantityOnHandTotal are not changed
    inventoryItem.setNonPKFields(parameters)
    parameters.inventoryItem = inventoryItem
    Map resultCheckDefault = run service: "inventoryItemCheckSetDefaultValues", with: parameters
    if (!ServiceUtil.isSuccess(resultCheckDefault)) {
        return resultCheckDefault
    }
    inventoryItem.inventoryItemId = delegator.getNextSeqId("InventoryItem")
    inventoryItem.create()

    result.inventoryItemId = inventoryItem.inventoryItemId
    return result
}

/**
 * createInventoryItemCheckSetAtpQoh
 * @return
 */
def createInventoryItemCheckSetAtpQoh() {
    Map result = success()
    if (parameters.availableToPromiseTotal || parameters.quantityOnHandTotal) {
        logInfo("Got an InventoryItem with ATP/QOH Total with ID ${parameters.inventoryItemId}, creating InventoryItemDetail")
        Map createDetailMap = [inventoryItemId: parameters.inventoryItemId]
        if (parameters.availableToPromiseTotal) {
            createDetailMap.availableToPromiseDiff = parameters.availableToPromiseTotal
        }
        if (parameters.quantityOnHandTotal) {
            createDetailMap.quantityOnHandDiff = parameters.quantityOnHandTotal
        }
        result = run service: "createInventoryItemDetail", with: createDetailMap
    }
    return result
}

/**
 * Check and, if empty, fills with default values ownerPartyId, currencyUomId, unitCost
 * @return
 */
def inventoryItemCheckSetDefaultValues() {
    Map result = success()
    GenericValue inventoryItem = parameters.inventoryItem
    String updateInventoryItem
    if (!inventoryItem) {
        inventoryItem = from("InventoryItem").where(parameters).queryOne()
        updateInventoryItem = "Y"
    }
    // if all the inventoryItem's fields are already filled, return with success
    if (inventoryItem.facilityId && inventoryItem.ownerPartyId && inventoryItem.currencyUomId && inventoryItem.unitCost) {
        return result
    }
    if (!inventoryItem.facilityId) {
        return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsMissingFacilityId", parameters.locale))
    }
    // if inventoryItem's ownerPartyId is empty, get the ownerPartyId from the facility
    if (!inventoryItem.ownerPartyId) {
        GenericValue facility = delegator.getRelatedOne("Facility", inventoryItem, false)
        inventoryItem.ownerPartyId = facility.ownerPartyId
        // if inventoryItem's ownerPartyId is still empty, return an error message
        if (!inventoryItem.ownerPartyId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsMissingOwnerPartyId", parameters.locale))
        }
    }
    // if inventoryItem's currencyUomId is empty, get the currencyUomId
    // from the party accounting preferences of the owner of the inventory item
    if (!inventoryItem.currencyUomId) {
        Map partyAccountingPreferencesCallMap = [organizationPartyId: inventoryItem.ownerPartyId]
        Map serviceResult = run service: "getPartyAccountingPreferences", with: partyAccountingPreferencesCallMap
        Map accPref = serviceResult.partyAccountingPreference
        inventoryItem.currencyUomId = accPref.baseCurrencyUomId
        if (!inventoryItem.currencyUomId) {
            inventoryItem.currencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default')
        }
        // if inventoryItem's currencyUomId is still empty, return an error message
        if (!inventoryItem.currencyUomId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsMissingCurrencyId", parameters.locale))
        }
    }
    // if inventoryItem's unitCost is empty, get the product's standard
    // cost by calling the getProductCost service
    if (!inventoryItem.unitCost) {
        // TODO: create a new service getProductStdCost that calls getProductCost
        Map inputMap = [productId: inventoryItem.productId, currencyUomId: inventoryItem.currencyUomId, costComponentTypePrefix: "EST_STD"]
        Map productCostResult = run service: "getProductCost", with: inputMap
        if (!ServiceUtil.isSuccess(productCostResult)) {
            return productCostResult
        }
        inventoryItem.unitCost = productCostResult.productCost
    }
    // if inventoryItem's unitCost is still empty, or negative return an error message
    // TODO/WARNING: getProductCost returns 0 even if no std costs are found
    if (!inventoryItem.unitCost && inventoryItem.unitCost != (BigDecimal) 0) {
        return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsMissingUnitCost", parameters.locale))
    }
    // if you don't want inventory item with unitCost = 0, change the operator
    // attribute from "less" to "less-equals".
    if (inventoryItem.unitCost < (BigDecimal) 0) {
        return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsNegativeUnitCost", parameters.locale))
    }
    if (updateInventoryItem) {
        inventoryItem.store()
    }
    return result
}

/**
 * Update an InventoryItem
 * @return
 */
def updateInventoryItem() {
    Map result = success()
    GenericValue lookedUpValue = from("InventoryItem").where(parameters).queryOne()
    if (!lookedUpValue.ownerPartyId) {
        GenericValue oldFacility = delegator.getRelatedOne("Facility", lookedUpValue, false)
        lookedUpValue.ownerPartyId = oldFacility.ownerPartyId
    }
    result.oldOwnerPartyId = lookedUpValue.ownerPartyId
    result.oldStatusId = lookedUpValue.statusId
    result.oldProductId = lookedUpValue.productId
    // special handling for the unitCost
    if (parameters.unitCost) {
        if (parameters.unitCost < (BigDecimal) 0.0) {
            return error(UtilProperties.getMessage("ProductUiLabels", "FacilityInventoryItemsUnitCostCannotBeNegative", parameters.locale))
        }
    }
    String oldUnitCost = lookedUpValue.unitCost
    if (parameters.lotId) {
        // Check if the lot already exists
        List<GenericValue> lotList = from("Lot").where(lotId: parameters.lotId).queryList()
        if (!lotList) {
            GenericValue lot = makeValue("Lot")
            lot.lotId = parameters.lotId
            lot.create()
        }
    }
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    // if the unit cost is changed create an InventoryItemDetail to keep track of unit cost history
    if (parameters.unitCost) {
        if (!oldUnitCost.equals(parameters.unitCost)) {
            Map createInventoryItemDetailInMap = [inventoryItemId: lookedUpValue.inventoryItemId, unitCost: parameters.unitCost]
            Map serviceResult = run service: "createInventoryItemDetail", with: createInventoryItemDetailInMap
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return serviceResult
            }
        }
    }
    return result
}

/**
 * Create an inventory item status record
 * @return
 */
def createInventoryItemStatus() {
    Map result = success()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    // find the most recent InventoryItemStatus record and set the statusEndDatetime
    GenericValue oldInventoryItemStatus = from("InventoryItemStatus")
        .where(inventoryItemId: parameters.inventoryItemId).orderBy("-statusDatetime").queryFirst()
    if (oldInventoryItemStatus) {
        oldInventoryItemStatus.statusEndDatetime = nowTimestamp
        oldInventoryItemStatus.store()
    }
    GenericValue inventoryItemStatus = makeValue("InventoryItemStatus", parameters)
    inventoryItemStatus.statusDatetime = nowTimestamp
    inventoryItemStatus.changeByUserLoginId = userLogin.userLoginId

    // make sure the current productId is set, if not passed in look up the current value
    if (inventoryItemStatus.productId) {
        GenericValue inventoryItem = from("InventoryItem").where(parameters).queryOne()
        inventoryItemStatus.productId = inventoryItem.productId
    }
    inventoryItemStatus.create()
    return result
}

/**
 * Create an InventoryItemDetail
 * @return
 */
def createInventoryItemDetail() {
    Map result = success()
    GenericValue newEntity = makeValue("InventoryItemDetail")
    newEntity.inventoryItemId = parameters.inventoryItemId
    // NOTE DEJ20070927: not using make-next-seq-id because a single InventoryItem may see traffic from lots of threads at the same time,
    // and make-next-seq-id doesn't do well with that <make-next-seq-id seq-field-name="inventoryItemDetailSeqId" value-field="newEntity" 
    // increment-by="1" numeric-padding="4"/>
    newEntity.inventoryItemDetailSeqId = delegator.getNextSeqId("InventoryItemDetail")
    result.inventoryItemDetailSeqId = newEntity.inventoryItemDetailSeqId
    newEntity.setNonPKFields(parameters)

    // set the effectiveDate; if from an ItemIssuance lookup the issuedDateTime
    if (parameters.itemIssuanceId) {
        GenericValue itemIssuance = from("ItemIssuance").where(parameters).queryOne()
        newEntity.effectiveDate = itemIssuance.issuedDateTime
    } else {
        newEntity.effectiveDate = UtilDateTime.nowTimestamp()
    }
    // if availableToPromiseDiff or quantityOnHandDiff are empty set to 0
    if (!newEntity.availableToPromiseDiff) {
        newEntity.availableToPromiseDiff = (BigDecimal) 0
    }
    if (!newEntity.quantityOnHandDiff) {
        newEntity.quantityOnHandDiff = (BigDecimal) 0
    }
    if (!newEntity.accountingQuantityDiff) {
        newEntity.accountingQuantityDiff = (BigDecimal) 0
    }
    newEntity.create()
    return result
}

/**
 * Update an InventoryItem From the Associated Detail Records
 * @return
 */
def updateInventoryItemFromDetail() {
    Map result = success()
    GenericValue inventoryItem = from("InventoryItem").where(parameters).queryOne()
    GenericValue inventoryItemDetailSummary = from("InventoryItemDetailSummary").where(parameters).queryOne()

    inventoryItem.availableToPromiseTotal = inventoryItemDetailSummary.availableToPromiseTotal
    inventoryItem.quantityOnHandTotal = inventoryItemDetailSummary.quantityOnHandTotal
    inventoryItem.accountingQuantityTotal = inventoryItemDetailSummary.accountingQuantityTotal
    inventoryItem.store()
    return result
}

/**
 * Update the totals on serialized inventory
 * @return
 */
def updateSerializedInventoryTotals() {
    Map result = success()
    GenericValue inventoryItem = from("InventoryItem").where(parameters).queryOne()
    if ("SERIALIZED_INV_ITEM".equals(inventoryItem.inventoryItemTypeId)) {
        if ("INV_AVAILABLE".equals(inventoryItem.statusId)
            && ((inventoryItem.availableToPromiseTotal != (BigDecimal) 1) || (inventoryItem.quantityOnHandTotal != (BigDecimal) 1))) {
            // available
            inventoryItem.availableToPromiseTotal = (BigDecimal) 1
            inventoryItem.quantityOnHandTotal = (BigDecimal) 1
            logInfo("In updateSerializedInventoryTotals Storing totals for item [${inventoryItem.inventoryItemId}] INV_AVAIABLE [1/1]")
            delegator.store(inventoryItem)
        } else if ("INV_DELIVERED".equals(inventoryItem.statusId)
            && ((inventoryItem.availableToPromiseTotal != (BigDecimal) 0) || (inventoryItem.quantityOnHandTotal != (BigDecimal) 0))) {
            // delivered
            inventoryItem.availableToPromiseTotal = (BigDecimal) 0
            inventoryItem.quantityOnHandTotal = (BigDecimal) 0
            logInfo("In updateSerializedInventoryTotals Storing totals [${inventoryItem.inventoryItemId}] for INV_DELIVERED [0/0]")
            delegator.store(inventoryItem)
        } else if (!"INV_AVAILABLE".equals(inventoryItem.statusId) && !"INV_DELIVERED".equals(inventoryItem.statusId)
            && ((inventoryItem.availableToPromiseTotal != (BigDecimal) 0) || (inventoryItem.quantityOnHandTotal != (BigDecimal) 1))) {
            // any promised; or on-hand but not available status
            inventoryItem.availableToPromiseTotal = (BigDecimal) 0
            inventoryItem.quantityOnHandTotal = (BigDecimal) 1
            logInfo("In updateSerializedInventoryTotals Storing totals [${inventoryItem.inventoryItemId}] for other status [0/1]")
            delegator.store(inventoryItem)
        }
    }
    return result
}

/**
 * Update Old Inventory To Detail All
 * @return
 */
def updateOldInventoryToDetailAll() {
    Map result = success()
    // find all InventoryItem records where oldQuantityOnHand or oldAvailableToPromise are not null
    EntityCondition cond = EntityCondition.makeCondition([EntityCondition.makeCondition("oldQuantityOnHand", EntityOperator.NOT_EQUAL,  null), 
        EntityCondition.makeCondition("oldAvailableToPromise", EntityOperator.NOT_EQUAL, null)], EntityOperator.OR)
    List<GenericValue> inventoryItemList = from("InventoryItem").where(cond).queryList()
    for (GenericValue inventoryItem : inventoryItemList) {
        Map callServiceMap = [inventoryItem: inventoryItem]
        Map serviceResult = run service: "updateOldInventoryToDetailSingle", with: callServiceMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
    }
    return result
}

/**
 * Update Old Inventory To Detail Single
 * @return
 */
def updateOldInventoryToDetailSingle() {
    Map result = success()
    // for each create an InventoryItemDetail representing the old QOH or ATP value, then null those fields
    GenericValue inventoryItem = parameters.inventoryItem
    Map createDetailMap = [inventoryItemId: inventoryItem.inventoryItemId, availableToPromiseDiff: inventoryItem.oldAvailableToPromise,
        quantityOnHandDiff: inventoryItem.oldQuantityOnHand]
    Map serviceResult = run service: "createInventoryItemDetail", with: createDetailMap
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    inventoryItem.oldAvailableToPromise = null
    inventoryItem.oldQuantityOnHand = null
    inventoryItem.store()
    return result
}

/**
 * Check Product Inventory Discontinuation
 * @return
 */
def checkProductInventoryDiscontinuation() {
    Map result = success()
    Map productIdMap = [productId: parameters.productId]
    GenericValue product = from("Product").where(productIdMap).queryOne()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    GenericValue virtProduct = null

    // if discontinueProductSales field is empty and the product is a variant, get the fieldcontent from the virtual product
    if (product) {
        if ("Y".equals(product.isVariant)) {
            // retrieve related virtual product because also to be used later
            Map getAssoc = [productIdTo: product.productId, productAssocTypeId: "PRODUCT_VARIANT"]
            GenericValue assoc = from("ProductAssoc").where(getAssoc).filterByDate().queryFirst()
            if (assoc) {
                virtProduct = delegator.getRelatedOne("MainProduct", assoc, false)
                if (!product.salesDiscWhenNotAvail) {
                    product.salesDiscWhenNotAvail = virtProduct.salesDiscWhenNotAvail
                }
            }
        }
    }
    // before checking inventory availability see if the product is already discontinued, and discontinued in the past (if in the future, still check availability and discontinue now if necessary)
    if (product && "Y".equals(product.salesDiscWhenNotAvail)
        && (!product.salesDiscontinuationDate || nowTimestamp.before(product.salesDiscontinuationDate))) {
        // now for the real fun, get the inventory available if is less-equal to zero discontinue product
        Map serviceResultGPIA = run service: "getProductInventoryAvailable", with: productIdMap
        if (!ServiceUtil.isSuccess(serviceResultGPIA)) {
            return serviceResultGPIA
        }
        BigDecimal availableToPromiseTotal = serviceResultGPIA.availableToPromiseTotal
        Map discontinueProductSalesMap = [:]
        if (availableToPromiseTotal <= (BigDecimal) 0) {
            discontinueProductSalesMap = [productId: parameters.productId]
            Map serviceResultDPS = run service: "discontinueProductSales", with: discontinueProductSalesMap
            if (!ServiceUtil.isSuccess(serviceResultDPS)) {
                return serviceResultDPS
            }
        }
        // check if related virtual product has no variant left, if yes discontinue the virtual product too when salesDiscWhenNotAvail is 'Y'
        if (virtProduct) {
            if ("Y".equals(virtProduct.salesDiscWhenNotAvail)) {
                Map getFromAssoc = [productId: virtProduct.productId, productAssocTypeId: "PRODUCt_VARIANT"]
                List<GenericValue> assocsDate = from("ProductAssoc").where(getFromAssoc).filterByDate().queryList()
                if (!assocsDate) {
                    discontinueProductSalesMap = [productId: virtProduct.productId]
                    Map serviceResult = run service: "discontinueProductSales", with: discontinueProductSalesMap
                    if (!ServiceUtil.isSuccess(serviceResult)) {
                        return serviceResult
                    }
                }
            }
        }
    }
    return result
}

/**
 * Create an InventoryItemVariance
 * @return
 */
def createInventoryItemVariance() {
    Map result = success()
    // add changes to availableToPromise and quantityOnHand
    GenericValue inventoryItem = from("InventoryItem").where(parameters).queryOne()
    if (!"NON_SERIAL_INV_ITEM".equals(inventoryItem.inventoryItemTypeId)) {
        return error("Can only create an InventoryItemVariance for a Non-Serialized Inventory Item")
    }
    // instead of updating InventoryItem, add an InventoryItemDetail
    Map createDetailMap = [inventoryItemId: parameters.inventoryItemId]
    if(parameters.physicalInventoryId) {
        createDetailMap.physicalInventoryId = parameters.physicalInventoryId
    }
    if(parameters.availableToPromiseVar) {
        createDetailMap.availableToPromiseDiff = parameters.availableToPromiseVar
    }
    if(parameters.quantityOnHandVar) {
        createDetailMap.quantityOnHandDiff = parameters.quantityOnHandVar
    }
    if(parameters.quantityOnHandVar) {
        createDetailMap.accountingQuantityDiff = parameters.quantityOnHandVar
    }
    if(parameters.varianceReasonId) {
        createDetailMap.reasonEnumId = parameters.varianceReasonId
    }
    if(parameters.comments) {
        createDetailMap.description = parameters.comments
    }
    Map serviceResult = run service: "createInventoryItemDetail", with: createDetailMap
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    GenericValue newEntity = makeValue("InventoryItemVariance", parameters)
    newEntity.create()
    // TODO: (possibly a big deal?) check to see if any reserved inventory needs to be changed because of a change in availableToPromise
    // TODO: make sure availableToPromise is never greater than the quantityOnHand?
    return result
}

/**
 * Create a PhysicalInventory and an InventoryItemVariance
 * @return
 */
def createPhysicalInventoryAndVariance() {
    Map result = success()
    Map serviceResult = run service: "createPhysicalInventory", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    parameters.physicalInventoryId = serviceResult.physicalInventoryId
    result.physicalInventoryId = serviceResult.physicalInventoryId
    serviceResult = run service: "createInventoryItemVariance", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    return result
}

// Check/Reserve Inventory Services 

/**
 * Get Inventory Available for a Product
 * @return
 */
def getProductInventoryAvailable() {
    Map result = success()
//    this method can be called with some optional parameters:
//    -facilityId
//    -partyId
//    -locationSeqId
//    -containerId
// If the service definitions are used then only some of these will ever be specified, or none of them.
//
// Whatever it is called with, it will basically get a list of InventoryItems and total the available amount.

    // logInfo("Getting inventory available to promise count; parameters are: ${parameters}")
    Map lookupFieldMap = [productId: parameters.productId]
    if (parameters.locationSeqId) {
        lookupFieldMap.locationSeqId = parameters.locationSeqId
    }
    if (parameters.inventoryItemId) {
        lookupFieldMap.inventoryItemId = parameters.inventoryItemId
    }
    if (parameters.facilityId) {
        lookupFieldMap.facilityId = parameters.facilityId
    }
    if (parameters.partyId) {
        lookupFieldMap.partyId = parameters.partyId
    }
    if (parameters.containerId) {
        lookupFieldMap.containerId = parameters.containerId
    }
    if (parameters.lotId) {
        lookupFieldMap.lotId = parameters.lotId
    }
    // we might get away with a cache here since real serious errors will occur during the reservation service... but only if we need the speed
    List<GenericValue> inventoryItems = []
    if (parameters.useCache) {
        // if caching was requested, don't use an iterator
        inventoryItems = from("InventoryItem").where(lookupFieldMap).cache().queryList()
    } else {
        inventoryItems = from("InventoryItem").where(lookupFieldMap).queryList()
    }
    parameters.availableToPromiseTotal = (BigDecimal) 0
    parameters.quantityOnHandTotal = (BigDecimal) 0
    parameters.accountingQuantityTotal = (BigDecimal) 0
    for (GenericValue inventoryItem : inventoryItems) {
        // NOTE: this code no longer distinguishes between serialized and non-serialized because both now have availableToPromiseTotal and quantityOnHandTotal populated 
        // (for serialized are based on status, non-serialized are based on InventoryItemDetail)
        if ((parameters.statusId && parameters.statusId.equals(inventoryItem.statusId))
            || (!parameters.statusId
            && (!inventoryItem.statusId || "INV_AVIALABLE".equals(inventoryItem.statusId)
                || "INV_NS_RETURNED".equals(inventoryItem.statusId) || "SERIALIZED_INV_ITEM".equals(inventoryItem.inventoryItemTypeId)))) {
            if (inventoryItem.quantityOnHandTotal) {
                parameters.quantityOnHandTotal += inventoryItem.quantityOnHandTotal
            }
            if (inventoryItem.availableToPromiseTotal) {
                parameters.availableToPromiseTotal += inventoryItem.availableToPromiseTotal
            }
            if (inventoryItem.accountingQuantityTotal) {
                parameters.accountingQuantityTotal += inventoryItem.accountingQuantityTotal
            }
        }
    }
    result.availableToPromiseTotal = parameters.availableToPromiseTotal
    result.quantityOnHandTotal = parameters.quantityOnHandTotal
    result.accountingQuantityTotal = parameters.accountingQuantityTotal
    return result
}

/**
 * Count Inventory On Hand for a Product constrained by a facilityId at a given date.
 * @return
 */
def countProductInventoryOnHand() {
    Map result = success()
    EntityCondition cond = EntityCondition.makeCondition(
        EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, parameters.inventoryCountDate),
        EntityCondition.makeCondition("facilityId", parameters.facilityId),
        EntityCondition.makeCondition("productId", parameters.productId));
    GenericValue inventoryItemDetailTotal = from("InventoryItemDetailForSum").where(cond).select("quantityOnHandSum").queryFirst()
    BigDecimal quantityOnHandTotal = inventoryItemDetailTotal.quantityOnHandSum
    if (!quantityOnHandTotal) {
        quantityOnHandTotal = (BigDecimal) 0
    }
    result.quantityOnHandTotal = quantityOnHandTotal
    return result
}

/**
 * Count Inventory Shipped for Sales Orders for a Product constrained by a facilityId in a given date range.
 * @return
 */
def countProductInventoryShippedForSales() {
    Map result = success()
    BigDecimal quantityOnHandTotal = (BigDecimal) 0
    if (!parameters.thruDate) {
        parameters.thruDate = UtilDateTime.nowTimestamp()
    }
    EntityCondition cond = EntityCondition.makeCondition(
        EntityCondition.makeCondition("effectiveDate", EntityOperator.GREATER_THAN_EQUAL_TO, parameters.fromDate), 
        EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN, parameters.thruDate),
        EntityCondition.makeCondition("facilityId", parameters.facilityId),
        EntityCondition.makeCondition("productId", parameters.productId),
        EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null),
        EntityCondition.makeCondition("quantityOnHandDiff", EntityOperator.LESS_THAN, (BigDecimal) 0));
    GenericValue inventoryItemDetailTotal = from("InventoryItemDetailForSum").where(cond).select("quantityOnHandSum").queryFirst()
    if (inventoryItemDetailTotal.quantityOnHandSum) {
        quantityOnHandTotal = inventoryItemDetailTotal.quantityOnHandSum.multiply((BigDecimal) (-1))
    }
    result.quantityOnHandTotal = quantityOnHandTotal
    return result
}

/**
 * Get Marketing Packages Available From Components In Inventory
 * @return
 */
def getMktgPackagesAvailable() {
    Map result = success()
    BigDecimal availableToPromiseTotal = (BigDecimal) 0
    BigDecimal quantityOnHandTotal = (BigDecimal) 0
    GenericValue product = from("Product").where(productId: parameters.productId).queryOne()
    Boolean isMarketingPkgAuto = EntityTypeUtil.hasParentType(delegator, 'ProductType', 'productTypeId',
        product.productTypeId, 'parentTypeId', 'MARKETING_PKG_AUTO')
    // In case of MARKETING_PKG_AUTO, pass MANUF_COMPONENT else PRODUCT_COMPONENT for MARKETING_PKG_PICK
    Map lookupMktgPkgParams = [productId: parameters.productId]
    if (isMarketingPkgAuto) {
        lookupMktgPkgParams.type = "MANUF_COMPONENT"
    } else {
        lookupMktgPkgParams.type = "PRODUCT_COMPONENT"
    }
    Map serviceResult = run service: "getAssociatedProducts", with: lookupMktgPkgParams
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    List<GenericValue> assocProducts = serviceResult.assocProducts
    // if there are any components, then the ATP and QOH are based on the quantities of those component
    // products and found with another service
    if (assocProducts) {
        Map inventoryByAssocProductsParams = [assocProducts: assocProducts, facilityId: parameters.facilityId, statusId: parameters.statusId]
        serviceResult = run service: "getProductInventoryAvailableFromAssocProducts", with: inventoryByAssocProductsParams
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        availableToPromiseTotal = serviceResult.availableToPromiseTotal
        quantityOnHandTotal = serviceResult.quantityOnHandTotal
    }
    result.availableToPromiseTotal = availableToPromiseTotal
    result.quantityOnHandTotal = quantityOnHandTotal
    return result
}

// Code in balanceInventoryItems service was doing same job which reassignInventoryReservations service is doing. Purpose of both the services are same. In fact reassignInventoryReservations service does better job of 
// reserving items for an order. 1) It takes into account the order priority, currentPromisedDate, reservedDateTime and sequenceId. where as  balanceInventoryItems was prioritizing orders 
// based on reservedDatetime and sequenceId. 2) reassignInventoryReservations excludes items with sufficient inventory, where as balanceInventoryItems also pulls up order items which have sufficient inventory. 

// Calling reassignInventoryReservations from balanceInventoryItems. balanceInventoryItems can be deleted, but not deleting it because its used in many places in OFBiz. 
// To DO: We can delete balanceInventoryItems in future and replace it with reassignInventoryReservations every where. 

/**
 * Balances available-to-promise on inventory items
 * @return
 */
def balanceInventoryItems() {
    Map result = success()
    GenericValue inventoryItem = from("InventoryItem").where(parameters).queryOne()
    Map reassignInventoryReservationsCtx = [productId: inventoryItem.productId, facilityId: inventoryItem.facilityId,
        fromDate: UtilDateTime.nowTimestamp()]
    EntityCondition cond = EntityCondition.makeCondition(
        EntityCondition.makeCondition("productId", inventoryItem.productId),
        EntityCondition.makeCondition("planItemStatusId", "ALLOC_PLAN_ITEM_APRV"),
        EntityCondition.makeCondition("planMethodEnumId", "MANUAL"));
    List<GenericValue> allocationPlanAndItemList = from("AllocationPlanAndItem").where(cond).orderBy("prioritySeqId").queryList()
    if (!allocationPlanAndItemList) {
        result = run service: "reassignInventoryReservations", with: reassignInventoryReservationsCtx
    } else {
        reassignInventoryReservationsCtx.allocationPlanAndItemList = allocationPlanAndItemList
        result = run service: "reassignInventoryReservationsByAllocationPlan", with: reassignInventoryReservationsCtx
    }
    return result
}

/**
 * Balances available-to-promise on inventory items
 * @return
 */
def reassignInventoryReservations() {
    Map result = success()
    List<GenericValue> allReservations = []
    Map touchedOrderIdMap = [:]
    EntityCondition cond = EntityCondition.makeCondition(
        EntityCondition.makeCondition("productId", parameters.productId),
        EntityCondition.makeCondition("facilityId", parameters.facilityId),
        EntityCondition.makeCondition("inventoryItemTypeId", "NON_SERIAL_INV_ITEM"))
    EntityCondition orCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, (BigDecimal) 0),
            EntityCondition.makeCondition("availableToPromiseTotal", null),
            EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.LESS_THAN, (BigDecimal) 0)
            ], EntityOperator.OR);
    if (parameters.fromDate) {
        orCond = EntityCondition.makeCondition(orCond, EntityOperator.OR,
            EntityCondition.makeCondition("currentPromisedDate", EntityOperator.GREATER_THAN, parameters.fromDate))
    }
    cond = EntityCondition.makeCondition(cond, orCond)
    List orderByList = ["priority", "currentPromisedDate", "reservedDatetime", "sequenceId"]
    List<GenericValue> relatedRes = from("OrderItemShipGrpInvResAndItem").where(cond).orderBy(orderByList).queryList()
    for (GenericValue oneRelatedRes : relatedRes) {
        EntityCondition entityCondition = EntityCondition.makeCondition(
            EntityCondition.makeCondition("orderId", oneRelatedRes.orderId),
            EntityCondition.makeCondition("shipGroupSeqId", oneRelatedRes.shipGroupSeqId),
            EntityCondition.makeCondition("orderItemSeqId", oneRelatedRes.orderItemSeqId),
            EntityCondition.makeCondition("inventoryItemId", oneRelatedRes.inventoryItemId),
            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED"),
            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_PICKED"));

        List<GenericValue> picklistItemList = from("PicklistAndBinAndItem").where(entityCondition).queryList()
        // only cancel/re-reserve when there are no picklists associated; this will prevent
        // orders appearing on duplicate pick lists
        if (!picklistItemList) {
            logInfo("Order [${oneRelatedRes.orderId}] was not found on any picklist for InventoryItem [${oneRelatedRes.inventoryItemId}]");
            allReservations.add(oneRelatedRes)
        }
    }
    // FIRST, cancel all the reservations
    for (GenericValue oisgir : allReservations) {
        Map cancelOisgirMap = [orderId: oisgir.orderId,
            orderItemSeqId: oisgir.orderItemSeqId,
            inventoryItemId: oisgir.inventoryItemId,
            shipGroupSeqId: oisgir.shipGroupSeqId]
        Map serviceResult = run service: "cancelOrderItemShipGrpInvRes", with: cancelOisgirMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
    }
    // THEN, re-reserve the cancelled items
    for (GenericValue oisgir : allReservations) {
        // maintain a Set (in a Map) of orderIds that we have reallocated for, but only if they had some quantityNotReserved
        if (oisgir.quantityNotAvailable && oisgir.quantityNotAvailable > (BigDecimal) 0) {
            touchedOrderIdMap[oisgir.orderId] = "Y"
            logVerbose("Adding ${oisgir.orderId} to touchedOrderIdMap")
        }
        GenericValue orderHeader = from("OrderHeader").where(orderId: oisgir.orderId).queryOne()
        // require inventory is N because it had to be N to begin with to have a negative ATP
        Map resMap = [productId: parameters.productId,
            orderId: oisgir.orderId,
            orderItemSeqId: oisgir.orderItemSeqId,
            quantity: oisgir.quantity,
            reservedDatetime: oisgir.reservedDatetime,
            reserveOrderEnumId: oisgir.reserveOrderEnumId,
            requireInventory: "N",
            shipGroupSeqId: oisgir.shipGroupSeqId,
            sequenceId: oisgir.sequenceId,
            facilityId: parameters.facilityId,
            priority: orderHeader.priority]
        logInfo("Re-reserving product [${resMap.productId}] for order item [${resMap.orderId}:${resMap.orderItemSeqId}] quantity [${resMap.quantity}]; facility [${parameters.facilityId}]")
        Map serviceResultRPIBF = run service: "reserveProductInventoryByFacility", with: resMap
        if (!ServiceUtil.isSuccess(serviceResultRPIBF)) {
            return serviceResultRPIBF
        }
    }
    // now go through touchedOrderIdMap keys and make a Set/Map of orderIds that are no longer on back-order
    Map noLongerOnBackOrderIdMap = [:]
    for (Map.Entry<String, String> entry : touchedOrderIdMap.entrySet()) {
        String touchedOrderId = entry.getKey()
        def throwAwayValue = entry.getValue()
        Map checkOrderIsOnBackOrderMap = [orderId: touchedOrderId]
        Map serviceResultCOIOBO = run service: "checkOrderIsOnBackOrder", with: checkOrderIsOnBackOrderMap
        if (!ServiceUtil.isSuccess(serviceResultCOIOBO)) {
            return serviceResultCOIOBO
        }
        Boolean isBackOrder = serviceResultCOIOBO.isBackOrder
        if (!isBackOrder) {
            noLongerOnBackOrderIdMap[touchedOrderId] = "Y"
        }
        if (noLongerOnBackOrderIdMap) {
            Set noLongerOnBackOrderIdSet = noLongerOnBackOrderIdMap.keySet()
            result.noLongerOnBackOrderIdSet = noLongerOnBackOrderIdSet
        }
    }
    return result
}

/**
 * Balances available-to-promise on inventory items
 * @return
 */
def reassignInventoryReservationsByAllocationPlan() {
    Map result = success()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    Map serviceResult =[:]
    Map touchedOrderIdMap = [:]
    Map noLongerOnBackOrderIdMap = [:]
    List<GenericValue> allocationPlanAndItemList = parameters.allocationPlanAndItemList
    for (GenericValue allocationPlanAndItem : allocationPlanAndItemList) {
        GenericValue orderHeader = from("OrderHeader").where(orderId: allocationPlanAndItem.orderId).queryOne()
        GenericValue productStore = delegator.getRelatedOne("ProductStore", orderHeader, false)
        if (!"Y".equals(productStore.allocateInventory)) {
            continue
        }
        GenericValue orderItem = from("OrderItem")
            .where(orderId: allocationPlanAndItem.orderId, orderItemSeqId: allocationPlanAndItem.orderItemSeqId)
            .queryOne()
        if (orderItem.reserveAfterDate && nowTimestamp.before(orderItem.reserveAfterDate)) {
            continue
        }
        //  maintain a Set (in a Map) of orderIds that we have inventory allocated
        if (allocationPlanAndItem.allocatedQuantity && allocationPlanAndItem.allocatedQuantity > (BigDecimal) 0) {
            touchedOrderIdMap[allocationPlanAndItem.orderId] = "Y"
            logVerbose("Adding ${allocationPlanAndItem.orderId} to touchedOrderIdMap")
        }
        GenericValue orderItemShipGroup = from("OrderItemShipGroup").where(orderId: allocationPlanAndItem.orderId).queryFirst()
        // Check for existing reservations of the order item
        List<GenericValue> orderItemShipGrpInvResList = from("OrderItemShipGrpInvRes")
            .where(orderId: allocationPlanAndItem.orderId, orderItemSeqId: allocationPlanAndItem.orderItemSeqId,
                shipGroupSeqId: orderItemShipGroup.shipGroupSeqId)
            .queryList()
        // Calculated already reserved quantity for the order item
        BigDecimal totalReservedQuantity = (BigDecimal) 0
        for (GenericValue orderItemShipGrpInvRes : orderItemShipGrpInvResList) {
            if (orderItemShipGrpInvRes.quantity && orderItemShipGrpInvRes.quantityNotAvailable) {
                BigDecimal reservedQuantity = orderItemShipGrpInvRes.quantity - orderItemShipGrpInvRes.quantityNotAvailable
                if (reservedQuantity) {
                    totalReservedQuantity += reservedQuantity.setScale(6)
                }
            }
        }
        if (!allocationPlanAndItem.allocatedQuantity) {
            allocationPlanAndItem.allocatedQuantity = (BigDecimal) 0
        }
        BigDecimal toBeReservedQuantity = allocationPlanAndItem.allocatedQuantity - totalReservedQuantity

        // require inventory is N because it had to be N to begin with to have a negative ATP
        Map resMap = [productId: parameters.productId, orderId: allocationPlanAndItem.orderId, orderItemSeqId: allocationPlanAndItem.orderItemSeqId,
            quantity: toBeReservedQuantity, reservedDatetime: nowTimestamp, requireInventory: "Y",
            shipGroupSeqId: orderItemShipGroup.shipGroupSeqId, facilityId: parameters.facilityId, priority: allocationPlanAndItem.prioritySeqId]
        logInfo("Reserving product [${resMap.productId}] for order item [${resMap.orderId}:${resMap.orderItemSeqId}] quantity [${toBeReservedQuantity}]; facility [${parameters.facilityId}]")
        serviceResult = run service: "reserveProductInventoryByFacility", with: resMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
    }
    // now go through touchedOrderIdMap keys and make a Set/Map of orderIds that are no longer on back-order
    for (Map.Entry<String, String> entry : touchedOrderIdMap.entrySet()) {
        String touchedOrderId = entry.getKey()
        Map checkOrderIsOnBackOrderMap = [orderId: touchedOrderId]
        serviceResult = run service: "checkOrderIsOnBackOrder", with: checkOrderIsOnBackOrderMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        Boolean isBackOrder = serviceResult.isBackOrder
        if (!isBackOrder) {
            noLongerOnBackOrderIdMap[touchedOrderId] = "Y"
        }
    }
    if (noLongerOnBackOrderIdMap) {
        result.noLongerOnBackOrderIdSet = noLongerOnBackOrderIdMap.keySet()
    }

    return result
}

/**
 * To balance order items with negative reservations
 * @return
 */
def balanceOrderItemsWithNegativeReservations() {
    Map result = success()
    Map orderItems = [:]
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    GenericValue productStore = delegator.getRelatedOne("ProductStore", orderHeader, false)
    if ("Y".equals(productStore.balanceResOnOrderCreation)) {
        EntityCondition cond = EntityCondition.makeCondition(
            EntityCondition.makeCondition("orderId", parameters.orderId),
            EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, (BigDecimal) 0),
            EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null))
        List oisgirais = from("OrderItemAndShipGrpInvResAndItem").where(cond).queryList()
        for (GenericValue oisgir : oisgirais) {
            orderItems[oisgir.orderItemSeqId] = oisgir
        }
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        for (Map.Entry<String, String> entry : orderItems.entrySet()) {
            String orderItemSeqId = entry.getKey()
            GenericValue oisgir = entry.getValue()
            Map reassignInventoryReservationsCtx = [productId: oisgir.productId, facilityId: oisgir.facilityId]
            if (oisgir.shipBeforeDate) {
                reassignInventoryReservationsCtx.fromDate = oisgir.shipBeforeDate
            } else {
                reassignInventoryReservationsCtx.fromDate = nowTimestamp
            }
            Map serviceResult = run service: "reassignInventoryReservations", with: reassignInventoryReservationsCtx
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return serviceResult
            }
        }
    } else {
        logInfo("Not reassigning the reservations because productStore.balanceResOnOrderCreation is set to N or null.")
    }
    return result
}

//Inventory Transfer Services

/**
 * Update an Inventory Transfer
 * @return
 */
def updateInventoryTransfer() {
    Map result = success()
    GenericValue inventoryTransfer = from("InventoryTransfer").where(inventoryTransferId: parameters.inventoryTransferId).queryOne()
    if (parameters.statusId) {
        // make sure a StatusValidChange record exists, if not return error
        GenericValue checkStatusValidChange = from("StatusValidChange")
            .where(statusId: inventoryTransfer.statusId, statusIdTo: parameters.statusId)
            .queryOne()
        if (!checkStatusValidChange) {
            return error("ERROR: Changing the status from ${inventoryTransfer.statusId} to ${parameters.statusId} is not allowed.")
        }
    }
    inventoryTransfer.setNonPKFields(parameters)
    inventoryTransfer.store()
    return result
}

/**
 * Create inventory transfers for the given product and quantity. Return the units not available for transfers.
 * @return
 */
def createInventoryTransfersForProduct() {
    Map result = success()
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    BigDecimal quantityNotTransferred
    // check the product; make sure its a physical item
    GenericValue product = from("Product").where(parameters).queryOne()
    GenericValue facility = from("Facility").where(parameters).cache(true).queryOne()
    GenericValue productType = delegator.getRelatedOne("ProductType", product, false)
    if ("N".equals(productType.isPhysical)) {
        quantityNotTransferred = (BigDecimal) 0
    } else {
        // before we do the find, put together the orderBy list based on which reserveOrderEnumId is specified
        // FIFO=first in first out, so it should be order by ASCending receive or expire date
        // LIFO=last in first out, so it means order by DESCending receive or expire date
        String orderByString
        switch (parameters.reserveOrderEnumId) {
            case "INVRO_GUNIT_COST":
                orderByString = "unitCost DESC"
                break
            case "INVRO_LUNIT_COST":
                orderByString = "+unitCost"
                break
            case "INVRO_FIFO_EXP":
                orderByString = "+expireDate"
                break
            case "INVRO_LIFO_EXP":
                orderByString = "-expireDate"
                break
            case "INVRO_LIFO_REC":
                orderByString = "-datetimeReceived"
                break
            default:
                // the default reserveOrderEnumId is INVRO_FIFO_REC, ie FIFO based on date received
                orderByString = "+datetimeReceived"
                parameters.reserveOrderEnumId = "INVRO_FIFO_REC"
                break
        }
        quantityNotTransferred = parameters.quantity
        if (!quantityNotTransferred) {
            quantityNotTransferred = (BigDecimal) 0
        }
        Map locationTypeMap = [enumTypeId: "FACLOC_TYPE"]
        List<GenericValue> locationTypeEnums = from("Enumeration").where(locationTypeMap).queryList()
        List<String> locationTypeEnumIds = EntityUtil.getFieldListFromEntityList(locationTypeEnums, "enumId", true)
        Boolean nothing = locationTypeEnumIds.add(null)
        for (String locationTypeEnumId : locationTypeEnumIds) {
            EntityCondition cond = EntityCondition.makeCondition(
                EntityCondition.makeCondition("productId", parameters.productId),
                EntityCondition.makeCondition("facilityId", parameters.facilityId),
                EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.GREATER_THAN, (BigDecimal) 0))
            if(locationTypeEnumId) {
                cond = EntityCondition.makeCondition(cond, EntityCondition.makeCondition("locationTypeEnumId", locationTypeEnumId))
            }
            if (parameters.containerId) {
                cond = EntityCondition.makeCondition(cond, EntityCondition.makeCondition("containerId", parameters.containerId))
            }
            List<GenericValue> inventoryItemAndLocations = from("InventoryItemAndLocation").where(cond).orderBy(orderByString).queryList()
            // first transfer InventoryItems in FLT_PICKLOC type locations, then FLT_BULK locations, then InventoryItems with no locations
            for (GenericValue inventoryItemAndLocation : inventoryItemAndLocations) {
                Map inputMap = [inventoryItemId: inventoryItemAndLocation.inventoryItemId, locationSeqId: inventoryItemAndLocation.locationSeqId]
                if (!parameters.statusId) {
                    inputMap.statusId = "IXF_REQUESTED"
                } else {
                    inputMap.statusId = parameters.statusId
                }
                inputMap.facilityId = parameters.facilityId
                inputMap.facilityIdTo = parameters.facilityIdTo
                if (parameters.locationSeqIdTo) {
                    inputMap.locationSeqIdTo = parameters.locationSeqIdTo
                }
                if (parameters.sendDate) {
                    inputMap.sendDate = parameters.sendDate
                }
                // TODO: inventory transfers for serialized items are not yet implemented
                if ("NON_SERIAL_INV_ITEM".equals(inventoryItemAndLocation.inventoryItemTypeId)) {
                    if (quantityNotTransferred > inventoryItemAndLocation.availableToPromiseTotal) {
                        inputMap.xferQty = inventoryItemAndLocation.availableToPromiseTotal
                    } else {
                        inputMap.xferQty = quantityNotTransferred
                    }
                    Map serviceResult = run service: "createInventoryTransfer", with: inputMap
                    if (!ServiceUtil.isSuccess(serviceResult)) {
                        return serviceResult
                    }
                    if (inputMap.xferQty) {
                        quantityNotTransferred -= inputMap.xferQty
                    }
                }
                if (quantityNotTransferred == (BigDecimal) 0) {
                    break
                }
            }
            if (quantityNotTransferred == (BigDecimal) 0) {
                break
            }
        }
    }
    result.quantityNotTransferred = quantityNotTransferred
    if (quantityNotTransferred > (BigDecimal) 0) {
        return error(UtilProperties.getMessage('ProductUiLabels', 'ProductInventoryATPNotAvailable',
            ['unavailableQuantity' : quantityNotTransferred, 'xferQty': parameters.quantity], locale))
    }
    return result
}

/**
 * If product store setOwnerUponIssuance is Y or empty, set the inventory item owner upon issuance.
 * @return
 */
def changeOwnerUponIssuance() {
    Map result = success()
    GenericValue itemIssuance = from("ItemIssuance").where(parameters).queryOne()
    GenericValue inventoryItem = delegator.getRelatedOne("InventoryItem", itemIssuance, false)
    if (inventoryItem) {
        if ("SERIALIZED_INV_ITEM".equals(inventoryItem.inventoryItemTypeId)) {
            GenericValue orderHeader = delegator.getRelatedOne("OrderHeader", itemIssuance, false)
            Map updateContext = [:]
            if (orderHeader) {
                Map orderRoleAndMap = [orderId: orderHeader.orderId, roleTypeId: "END_USER_CUSTOMER"]
                GenericValue orderRole = from("OrderRole").where(orderRoleAndMap).queryFirst()
                GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
                if (orderRole && (!productStore || !productStore.setOwnerUponIssuance || "Y".equals(productStore.setOwnerUponIssuance))) {
                    updateContext.ownerPartyId = orderRole.partyId
                }
            }
            updateContext.inventoryItemId = inventoryItem.inventoryItemId
            Map serviceResult = run service: "updateInventoryItem", with: updateContext
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return serviceResult
            }
        }
    }
    return result
}

/**
 * Sets priority of an order for Inventory Reservation, orders with HIGH priority would be served first.
 * @return
 */
def setOrderReservationPriority() {
    Map result = success()
    String orderId = parameters.orderId
    GenericValue orderHeader = from("OrderHeader").where(orderId: orderId).queryOne()
    String priority = parameters.priority
    List<GenericValue> oisgirs = from("OrderItemShipGrpInvRes").where(orderId: orderId).queryList()
    if (!priority) {
        for (GenericValue oisgir : oisgirs) {
            if (!oisgir.priority) {
                oisgir.priority = 2
            }
            oisgir.store()
        }
        if (!orderHeader.priority) {
            orderHeader.priority = 2
        }
        orderHeader.store()
    } else {
        orderHeader.priority = priority
        orderHeader.store()
        for (GenericValue oisgir : oisgirs) {
            oisgir.priority = priority
            oisgir.store()
        }
        List<GenericValue> oisgirais = from("OrderItemAndShipGrpInvResAndItem").where(orderId: orderId).queryList()
        for (GenericValue oisgir : oisgirais) {
            Map reassignInventoryReservationsCtx = [productId: oisgir.productId, facilityId: oisgir.facilityId]
            Map serviceResult = run service: "reassignInventoryReservations", with: reassignInventoryReservationsCtx
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return serviceResult
            }
        }
    }
    return result
}

/**
 * Service that updates stock availability of products
 * @return
 */
def setLastInventoryCount() {
    Map result = success()
    GenericValue inventoryItem = from("InventoryItem").where(inventoryItemId: parameters.inventoryItemId).queryOne()
    GenericValue productFacility = from("ProductFacility").where(productId: inventoryItem.productId, facilityId: inventoryItem.facilityId).queryOne()
    if (productFacility) {
        Map serviceInMap = [productId: productFacility.productId, facilityId: productFacility.facilityId]
        Map serviceResult = run service: "getInventoryAvailableByFacility", with: serviceInMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        BigDecimal availableToPromiseTotal = serviceResult.availableToPromiseTotal
        productFacility.lastInventoryCount = availableToPromiseTotal
        serviceInMap = [:]
        serviceInMap << productFacility
        serviceResult = run service: "updateProductFacility", with: serviceInMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
    }
    return result
}

/**
 * Create or update GeoPoint assigned to facility
 * @return
 */
def createUpdateFacilityGeoPoint() {
    Map result = success()
    if (!parameters.geoPointId) {
        Map serviceResult = run service: "createGeoPoint", with: parameters
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        String geoPointId = result.geoPointId
        GenericValue facility = from("Facility").where(parameters).queryOne()
        facility.geoPointId = geoPointId
        facility.store()
    } else {
        Map serviceResult = run service: "updateGeoPoint", with: parameters
        return serviceResult
    }
    return result
}

/**
 * Create an InventoryItemLabelAppl
 * @return
 */
def createInventoryItemLabelAppl() {
    Map result = success()
    GenericValue newEntity = makeValue("InventoryItemLabelAppl", parameters)
    GenericValue inventoryItemLabel = from("InventoryItemLabel").where(parameters).queryOne()
    newEntity.inventoryItemLabelTypeId = inventoryItemLabel.inventoryItemLabelTypeId
    newEntity.create()
    return result
}
