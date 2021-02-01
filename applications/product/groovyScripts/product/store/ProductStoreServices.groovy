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


import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityTypeUtil
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil


/**
 * Create a Product Store
 * @return
 */
def createProductStore() {
    Map result = success()
    if (!security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", parameters.locale))
    }
    if ("Y" == parameters.oneInventoryFacility
            && !parameters.inventoryFacilityId) {
        return error(UtilProperties.getMessage("ProductUiLabels", "InventoryFacilityIdRequired", parameters.locale))
    }
    if ("Y" == parameters.showPriceWithVatTax) {
        if (!parameters.vatTaxAuthGeoId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductVatTaxAuthGeoNotSet", parameters.locale))
        }
        if (!parameters.vatTaxAuthPartyId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductVatTaxAuthPartyNotSet", parameters.locale))
        }
    }
    GenericValue newEntity = makeValue("ProductStore")
    newEntity.setNonPKFields(parameters)
    String productStoreId = delegator.getNextSeqId("ProductStore")
    newEntity.productStoreId = productStoreId
    newEntity.create()

    // create the ProductStoreFacility record
    if (newEntity.inventoryFacilityId) {
        makeValue("ProductStoreFacility", [
                facilityId: newEntity.inventoryFacilityId,
                productStoreId: newEntity.productStoreId,
                fromDate: UtilDateTime.nowTimestamp()])
                .create()
    }
    result.productStoreId = productStoreId
    return result
}

/**
 * Update a Product Store
 * @return
 */
def updateProductStore() {
    if (!security.hasEntityPermission("CATALOG", "_UPDATE", parameters.userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogUpdatePermissionError", parameters.locale))
    }
    if ("Y" == parameters.oneInventoryFacility
        && !parameters.inventoryFacilityId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "InventoryFacilityIdRequired", parameters.locale))
    }
    GenericValue store = from("ProductStore").where(productStoreId: parameters.productStoreId).queryOne()
    String oldFacilityId = store.inventoryFacilityId
    store.setNonPKFields(parameters)

    // visualThemeId must be replaced by ecomThemeId because of Entity.field names conflict. See OFBIZ-10567
    store.visualThemeId = parameters.ecomThemeId
    if ("Y" == store.showPricesWithVatTax) {
        if (!store.vatTaxAuthGeoId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductVatTaxAuthGeoNotSet", parameters.locale))
        }
        if (!store.vatTaxAuthPartyId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductVatTaxAuthPartyNotSet", parameters.locale))
        }
    }
    store.store()

    // update the ProductStoreFacility record
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (oldFacilityId != store.inventoryFacilityId) {
        if ("Y" == store.oneInventoryFacility) {
            // expire all the facilities
            EntityConditionBuilder exprBldr = new EntityConditionBuilder()
            EntityCondition thruDateCondition = exprBldr.OR() {
                EQUALS(thruDate: null)
                GREATER_THAN_EQUAL_TO(thruDate: nowTimestamp)
            }
            EntityCondition condition = exprBldr.AND(thruDateCondition) {
                EQUALS(productStoreId: store.productStoreId)
                LESS_THAN_EQUAL_TO(fromDate: nowTimestamp)
            }
            delegator.storeByCondition("ProductStoreFacility", condition, [thruDate: nowTimestamp])
        }
        // create the new entry
        makeValue("ProductStoreFacility", [
                facilityId: store.inventoryFacilityId,
                productStoreId: store.productStoreId,
                fromDate: nowTimestamp])
                .create()
    }
    return success()
}

// Store Inventory Services

/**
 * Reserve Store Inventory
 * @return
 */
def reserveStoreInventory() {
    Map result = success()
    BigDecimal quantityNotReserved

    GenericValue productStore = from("ProductStore").where(parameters).cache().queryOne()
    if (!productStore) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductProductStoreNotFound", parameters.locale))
    }

    GenericValue product = from("Product").where(parameters).cache().queryOne()
    GenericValue orderHeader = from("OrderHeader").where(parameters).cache().queryOne()
    parameters.priority = orderHeader.priority

    // if prodCatalog is set to not reserve inventory, break here
    if ("N" == productStore.reserveInventory) {
        // note: if not set, defaults to yes, reserve inventory
        logVerbose("ProductStore with id [" + productStore.productStoreId + "], is set to NOT reserve inventory, not reserving inventory")
        result.quantityNotReserved = parameters.quantity
        return result
    }
    String requireInventory = isStoreInventoryRequiredInline(product, productStore)
    String facilityId = parameters.facilityId
    if (!facilityId) {
        if ("Y" == productStore.oneInventoryFacility) {
            if (!productStore.inventoryFacilityId) {
                return error(UtilProperties.getMessage("ProductUiLabels", "ProductProductStoreNoSpecifiedInventoryFacility", parameters.locale))
            }
            Map serviceResult = run service: "reserveProductInventoryByFacility", with: [*: parameters,
                                                                                         facilityId: productStore.inventoryFacilityId,
                                                                                         requireInventory: requireInventory,
                                                                                         reserveOrderEnumId: productStore.reserveOrderEnumId]
            quantityNotReserved = serviceResult.quantityNotReserved

            if (Debug.infoOn()) {
                if (quantityNotReserved == (BigDecimal) 0) {
                    logInfo("Inventory IS reserved in facility with id [${productStore.inventoryFacilityId}] for product id [${parameters.productId}]; desired quantity was ${parameters.quantity}")
                } else {
                    logInfo("There is insufficient inventory available in facility with id [${productStore.inventoryFacilityId}] for product id [${parameters.productId}]; desired quantity is ${parameters.quantity}, amount could not reserve is ${quantityNotReserved}")
                }
            }
        } else {
            GenericValue storeFound
            List productStoreFacilities = from("ProductStoreFacility")
                    .where(productStoreId: productStore.productStoreId)
                    .orderBy("sequenceNum")
                    .cache()
                    .queryList()
            for (GenericValue productStoreFacility : productStoreFacilities) {
                // in this case quantityNotReserved will always be empty until it finds a facility it can totally reserve from, then it will be 0.0 and we are done
                if (!storeFound) {
                    // TODO: must entire quantity be available in one location?
                    // Right now the answer is yes, it only succeeds if one facility has sufficient inventory for the order.
                    Map callServiceMapIABF = [productId: parameters.productId, facilityId: productStoreFacility.facilityId]
                    logInfo("ProductStoreService:In productStoreFacilities loop: [" + parameters.facilityId + "]")
                    Map serviceResultIABF = run service: "getInventoryAvailableByFacility", with: callServiceMapIABF
                    BigDecimal availableToPromiseTotal = serviceResultIABF.availableToPromiseTotal

                    if (availableToPromiseTotal >= parameters.quantity) {
                        storeFound = productStoreFacility
                    }
                }
            }
            // didn't find anything? Take the first facility from list
            if (!storeFound) {
                storeFound = productStoreFacilities.get(0)
            }
            facilityId = storeFound.facilityId ?: ""
            Map serviceResult = run service: "reserveProductInventoryByFacility", with: [*: parameters,
                                                                                         facilityId: facilityId,
                                                                                         requireInventory: requireInventory,
                                                                                         reserveOrderEnumId: productStore.reserveOrderEnumId]
            quantityNotReserved = serviceResult.quantityNotReserved
            logInfo("Inventory IS reserved in facility with id [${storeFound.facilityId}] for product id [${parameters.productId}]; desired quantity was ${parameters.quantity}")
        }
    } else {
        List productStoreFacilities = from("ProductStoreFacility").where(productStoreId: productStore.productStoreId, facilityId: facilityId).cache().orderBy("sequenceNum").queryList()
        GenericValue facilityFound
        for (GenericValue productStoreFacility : productStoreFacilities) {
            // Search Product Store Facilities to insure the facility passed in is associated to the Product Store passed in
            facilityFound = productStoreFacility
            logInfo("ProductStoreService:Facility Found : [" + facilityFound + "]")
        }
        if (!facilityFound) {
            return  error(UtilProperties.getMessage("ProductUiLabels", "FacilityNoAssociatedWithProcuctStore", parameters.locale))
        }
        Map serviceResult = run service: "reserveProductInventoryByFacility", with: [*: parameters,
                                                                                     facilityId: facilityId,
                                                                                     requireInventory: requireInventory,
                                                                                     reserveOrderEnumId: productStore.reserveOrderEnumId]
        quantityNotReserved = serviceResult.quantityNotReserved
        if (Debug.infoOn()) {
            if (quantityNotReserved == (BigDecimal) 0) {
                logInfo("Inventory IS reserved in facility with id [${facilityId}] for product id [${parameters.productId}]; desired quantity was ${parameters.quantity}")
            } else {
                logInfo("There is insufficient inventory available in facility with id [${facilityId}] for product id [${parameters.productId}]; desired quantity is ${parameters.quantity}, amount could not reserve is ${quantityNotReserved}")
            }
        }
    }
    result.quantityNotReserved = quantityNotReserved
    return result
}

/**
 * Is Store Inventory Required
 * @return
 */
def isStoreInventoryRequired() {
    GenericValue productStore = parameters.productStore ?: from("ProductStore").where(parameters).cache().queryOne()
    GenericValue product = parameters.product ?: from("Product").where(parameters).cache().queryOne()

    Map result = success()
    result.requireInventory = isStoreInventoryRequiredInline(product, productStore)
    return result
}

/**
 * Is Store Inventory Required
 * @param product
 * @param productStore
 * @return
 */
def isStoreInventoryRequiredInline(GenericValue product, GenericValue productStore) {
    String requireInventory = product.requireInventory
    requireInventory = requireInventory ?: productStore.requireInventory
    requireInventory = requireInventory ?: "Y"
    return requireInventory
}

/**
 * Is Store Inventory Available
 * @return
 */
def isStoreInventoryAvailable() {
    Map result = success()
    GenericValue productStore = parameters.productStore ?: from("ProductStore").where(parameters).cache().queryOne()
    GenericValue product = parameters.product ?: from("Product").where(parameters).cache().queryOne()

    BigDecimal availableToPromiseTotal
    String available

    // If the given product is a SERVICE or DIGITAL_GOOD
    if (product.productTypeId == "SERVICE" || product.productTypeId == "DIGITAL_GOOD") {
        logVerbose("Product with id ${product.productId}, is of type ${product.productTypeId}, returning true for inventory available check")
        result.available = "Y"
        return result
    }

    // TODO: what to do with ASSET_USAGE? Only done elsewhere? Would need date/time range info to check availability

    // if prodCatalog is set to not check inventory break here
    if ("N" == productStore.checkInventory) {
        logVerbose("ProductStore with id ${productStore.productStoreId}, is set to NOT check inventory," +
                " returning true for inventory available check")
        result.available = "Y"
        return result
    }
    if ("Y" == productStore.oneInventoryFacility) {
        if (!productStore.inventoryFacilityId) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductProductStoreNotCheckAvailability", parameters.locale))
        }
        boolean isMarketingPkg = EntityTypeUtil.hasParentType(delegator, 'ProductType', 'productTypeId',
                product.productTypeId, 'parentTypeId', 'MARKETING_PKG')
        String serviceName = isMarketingPkg ? "getMktgPackagesAvailable" : "getInventoryAvailableByFacility"
        Map serviceResult = run service: serviceName, with: [productId: parameters.productId,
                                                             facilityId: productStore.inventoryFacilityId]
        availableToPromiseTotal = serviceResult.availableToPromiseTotal

        // check to see if we got enough back...
        if (availableToPromiseTotal >= parameters.quantity) {
            available = "Y"
            logInfo("Inventory IS available in facility with id ${productStore.inventoryFacilityId} for " +
                    "product id ${parameters.productId}; desired quantity is ${parameters.quantity}," +
                    "available quantity is ${availableToPromiseTotal}")
        } else {
            available = "N"
            logInfo("Returning false because there is insufficient inventory available in facility with id " +
                    "${productStore.inventoryFacilityId} for product id ${parameters.productId}; desired quantity" +
                    " is ${parameters.quantity}, available quantity is ${availableToPromiseTotal}")
        }
    } else {
        List productStoreFacilities = from("ProductStoreFacility")
                .where(productStoreId: productStore.productStoreId)
                .orderBy("sequenceNum")
                .cache()
                .queryList()
        available = "N"
        for (GenericValue productStoreFacility : productStoreFacilities) {
            // TODO: must entire quantity be available in one location?
            // Right now the answer is yes, it only succeeds if one facility has sufficient inventory for the order.
            boolean isMarketingPkg = EntityTypeUtil.hasParentType(delegator, 'ProductType', 'productTypeId'
                    , product.productTypeId, 'parentTypeId', 'MARKETING_PKG')
            String serviceName = isMarketingPkg ? "getMktgPackagesAvailable" : "getInventoryAvailableByFacility"
            Map serviceResult = run service: serviceName, with: [productId: parameters.productId,
                                                                 facilityId: productStoreFacility.facilityId]
            availableToPromiseTotal = serviceResult.availableToPromiseTotal

            if (availableToPromiseTotal >= parameters.quantity) {
                available = "Y"
                logInfo("Inventory IS available in facility with id ${productStoreFacility.facilityId}" +
                        " for product id ${parameters.productId}; desired quantity is ${parameters.quantity}," +
                        " available quantity is ${availableToPromiseTotal}")
            }
        }
    }
    result.available = available

    /* TODO: must entire quantity be available in one location?
     *  Right now the answer is yes, it only succeeds if one facility has sufficient inventory for the order.
     *  When we get into splitting options it is much more complicated. There are various options like:
     *  - allow split between facilities
     *  - in split order facilities by highest quantities
     *  - in split order facilities by lowest quantities
     *  - in split order facilities by order in database, ie sequence numbers on facility-store join table
     *  - in split order facilities by nearest locations to customer (not an easy one there...)
     */
    // loop through all facilities attached to this catalog and check for individual or cumulative sufficient inventory
    return result
}

/**
 * Is Store Inventory Available or Not Required
 * @return
 */
def isStoreInventoryAvailableOrNotRequired() {
    Map result = success()
    GenericValue productStore = parameters.productStore ?: from("ProductStore").where(parameters).cache().queryOne()
    GenericValue product = parameters.product ?: from("Product").where(parameters).cache().queryOne()
    if ("Y" != isStoreInventoryRequiredInline(product, productStore)) {
        result.availableOrNotRequired = "Y"
    } else {
        Map serviceResult = run service: "isStoreInventoryAvailable", with: parameters
        result.availableOrNotRequired = serviceResult.available
    }
    return result
}

/*
 * =============================
 * Permission Methods
 * =============================
 */

// a methods to centralize product security code, meant to be called in-line with
// call-simple-method, and the checkAction and callingMethodName attributes should be in the method context

/**
 * Check ProductStore Related Permission
 * @return
 */
def checkProductStoreRelatedPermission(Map inputParameter) {
    List roleStores
    String callingMethodName = inputParameter.resourceDescription
    String checkAction = inputParameter.mainAction
    String productStoreIdName = inputParameter.productStoreIdName
    String productStoreIdToCheck = inputParameter.productStoreIdToCheck
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", locale)
    }
    if (!checkAction) {
        checkAction = "UPDATE"
    }
    if (!productStoreIdName) {
        productStoreIdName = inputParameter.productStoreId
    }
    if (!productStoreIdToCheck) {
        productStoreIdToCheck = inputParameter.productstoreIdName
    }

    // find all role-store that this productStore is a member of
    if (!security.hasEntityPermission("CATALOG", ("_" + checkAction), userLogin)) {
        roleStores = from("ProductStoreRole").where(productStoreId: productStoreIdToCheck, partyId: userLogin.partyId, roleTypeId: "LTD_ADMIN").filterByDate().queryList()
        roleStores = EntityUtil.filterByDate(roleStores, UtilDateTime.nowTimestamp(), "roleFromDate", "roleThruDate", true)
    }
    logInfo("Checking store permission, roleStores=${roleStores}")
    if (!(security.hasEntityPermission("CATALOG", ("_" + checkAction), userLogin) ||
    (security.hasEntityPermission("CATALOG_ROLE", ("_" + checkAction), userLogin) && roleStores))) {
        logVerbose("Permission check failed, user does not have permission")
        String checkActionLabel = 'ProductCatalog' + checkAction.charAt(0) + checkAction.substring(1).toLowerCase() + 'PermissionError'
        return error(UtilProperties.getMessage("ProductUiLabels", checkActionLabel, locale))
    }
    return success()
}

/**
 * Main permission logic
 * @return
 */
def productStoreGenericPermission() {
    Map result = success()
    if (!parameters.mainAction) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map serviceInMap = parameters
    Map serviceResult = checkProductStoreRelatedPermission(serviceInMap)
    if (ServiceUtil.isSuccess(serviceResult)) {
        result.hasPermission = true
    } else {
        String failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductPermissionError", parameters.locale)
        result.failMessage = failMessage
        result.hasPermission = false
    }
    return result
}

/**
 * When product store group hierarchy has been operate, synchronize primaryParentGroupId with ProductStoreGroupRollup
 * @return
 */
def checkProductStoreGroupRollup() {
    GenericValue productStoreGroup = from("ProductStoreGroup").where(parameters).queryOne()
    if (!parameters.primaryParentGroupId) {
        GenericValue productStoreGroupRollup = from("ProductStoreGroupRollup").where(parameters).queryOne()
        if (productStoreGroupRollup) {
            productStoreGroup.primaryParentGroupId = productStoreGroupRollup.parentGroupId
            run service: "updateProductStoreGroup", with: productStoreGroup.getAllFields()
        }
    } else {
        if (from("ProductStoreGroupRollup")
                .where(productStoreGroupId: productStoreGroup.productStoreGroupId,
                        parentGroupId: parameters.primaryParentGroupId)
                .filterByDate()
                .queryCount() == 0) {
            run service: "createProductStoreGroupRollup", with: [productStoreGroupId: productStoreGroup.productStoreGroupId,
                                                                parentGroupId: parameters.primaryParentGroupId,
                                                                fromDate: UtilDateTime.nowTimestamp()]
        }
    }
    return success()
}