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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil


/**
 * Create a ShoppingList
 * @return
 */
def createShoppingList() {
    Map result = success()
    GenericValue newEntity = makeValue("ShoppingList")
    newEntity.setNonPKFields(parameters)

    if (!newEntity.partyId) {
        newEntity.partyId = userLogin.partyId
    }

    if(!newEntity.shoppingListTypeId) {
        newEntity.shoppingListTypeId = "SLT_WISH_LIST"
    }

    if (!newEntity.listName) {
        newEntity.listName = UtilProperties.getMessage("OrderUiLabels", "OrderNewShoppingList", parameters.locale) ?: "New Shopping List"
    }

    if (!newEntity.isPublic) {
        newEntity.isPublic = "N"
    }

    if (!newEntity.isActive) {
        if (newEntity.shoppingListTypeId == "SLT_AUTO_REODR") {
            newEntity.isActive = "N"
        } else {
            newEntity.isActive = "Y"
        }
    }

    newEntity.shoppingListId = delegator.getNextSeqId("ShoppingList")
    result.shoppingListId = newEntity.shoppingListId
    newEntity.create()
    String successMessage = UtilProperties.getMessage("OrderUiLabels", "OrderShoppingListCreatedSuccessfully", parameters.locale)
    result.successMessage = successMessage
    return result
}

/**
 * Update a ShoppingList
 * @return
 */
def updateShoppingList() {
    Map result = success()
    GenericValue shoppingList = from("ShoppingList").where(parameters).queryOne()
    shoppingList.setNonPKFields(parameters)

    // don't let auto-reorders be active unless there is some recurrence info

    if (shoppingList.shoppingListTypeId == "SLT_AUTO_REODR" &&
    (!shoppingList.recurrenceInfoId || !shoppingList.paymentMethodId || !shoppingList.contactMechId|| !shoppingList.shipmentMethodTypeId)) {
        shoppingList.isActive =  "N"
    }
    shoppingList.store()
    String successMessage = UtilProperties.getMessage("OrderUiLabels", "OrderShoppingListUpdatedSuccessfully", parameters.locale)
    result.successMessage = successMessage
    return result
}

/**
 * Create a ShoppingList Item
 * @return
 */
def createShoppingListItem() {
    Map result = success()
    List shoppingListItems = from("ShoppingListItem").where(productId: parameters.productId, shoppingListId: parameters.shoppingListId).queryList()
    if (!shoppingListItems) {
        String parentMethodName = "createShoppingListItem"
        String permissionAction = "CREATE"
        GenericValue shoppingList = from("ShoppingList").where(parameters).queryOne()
        GenericValue product = from("Product").where(parameters).queryOne()
        if (!product) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductErrorProductNotFound", parameters.locale))
        }
        GenericValue newEntity = makeValue("ShoppingListItem")
        newEntity.shoppingListId = parameters.shoppingListId
        delegator.setNextSubSeqId(newEntity, "shoppingListItemSeqId", 5, 1)

        newEntity.setNonPKFields(parameters)
        result.shoppingListItemSeqId = newEntity.shoppingListItemSeqId
        newEntity.create()
        if (shoppingList.partyId != userLogin.partyId) {
            shoppingList.lastAdminModified = UtilDateTime.nowTimestamp()
            shoppingList.store()
        }
    } else {
        GenericValue shoppingListItem = shoppingListItems.get(0)

        shoppingListItem.quantity = shoppingListItem.quantity ?: (BigDecimal) 0.0
        parameters.quantity = parameters.quantity ?: (BigDecimal) 0.0

        BigDecimal totalquantity = shoppingListItem.quantity + parameters.quantity
        result.shoppingListItemSeqId = shoppingListItem.shoppingListItemSeqId
        Map shoppingListItemParameters = [:]
        shoppingListItemParameters << shoppingListItem
        shoppingListItemParameters.quantity = totalquantity
        Map serviceResult = run service:"updateShoppingListItem", with: shoppingListItemParameters
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return error(serviceResult.errorMessage)
        }
    }
    return result
}

/**
 * Update a ShoppingListItem
 * @return
 */
def updateShoppingListItem() {
    GenericValue shoppingList = from("ShoppingList").where(parameters).queryOne()
    GenericValue shoppingListItem = from("ShoppingListItem").where(parameters).queryOne()
    shoppingListItem.setNonPKFields(parameters)
    shoppingListItem.store()

    if (shoppingList.partyId != userLogin.partyId) {
        shoppingList.lastAdminModified = UtilDateTime.nowTimestamp()
        shoppingList.store()
    }
    return success()
}

/**
 * Remove a ShoppingListItem
 * @return
 */
def removeShoppingListItem() {
    GenericValue shoppingList = from("ShoppingList").where(parameters).queryOne()
    GenericValue shoppingListItem = from("ShoppingListItem").where(parameters).queryOne()
    shoppingListItem.remove()

    if (shoppingList.partyId != userLogin.partyId) {
        shoppingList.lastAdminModified = UtilDateTime.nowTimestamp()
        shoppingList.store()
    }
    return success()
}

/**
 * Adds a shopping list item if one with the same productId does not exist
 * @return
 */
def addDistinctShoppingListItem() {
    Map result = success()
    List shoppingListItemList = from("ShoppingListItem").where(shoppingListId: parameters.shoppingListId).queryList()

    for (GenericValue shoppingListItem : shoppingListItemList) {
        if (parameters.productId == shoppingListItem.productId) {
            result.shoppingListItemSeqId = shoppingListItem.shoppingListItemSeqId
            return result
        }
    }
    Map serviceResult = run service:"createShoppingListItem", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return error(serviceResult.errorMessage)
    }

    result.shoppingListItemSeqId = serviceResult.shoppingListItemSeqId
    return result
}

/**
 * Calculate Deep Total Price for a ShoppingList
 * @return
 */
def calculateShoppingListDeepTotalPrice() {
    Map result = success()
    Map calcPriceOutMap = [:]
    Map serviceResult =  run service:"checkShoppingListItemSecurity", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return error(serviceResult.errorMessage)
    }
    Boolean hasPermission = serviceResult.hasPermission
    if (!hasPermission) {
        return error(UtilProperties.getMessage("OrderErrorUiLabels", "OrderSecurityErrorToRunForAnotherParty", parameters.locale))
    }
    Map calcPriceInBaseMap = [prodCatalogId: parameters.prodCatalogId, webSiteId: parameters.webSiteId]
    if (parameters.partyId) {
        calcPriceInBaseMap.partyId = parameters.partyId
    }
    if (parameters.productStoreId) {
        calcPriceInBaseMap.productStoreId = parameters.productStoreId
    }
    if (parameters.productStoreGroupId) {
        calcPriceInBaseMap.productStoreGroupId = parameters.productStoreGroupId
    }
    if (parameters.currencyUomId) {
        calcPriceInBaseMap.currencyUomId = parameters.currencyUomId
    }
    if (parameters.autoUserLogin) {
        calcPriceInBaseMap.autoUserLogin = parameters.autoUserLogin
    }

    List shoppingListItems = from("ShoppingListItem").where(shoppingListId: parameters.shoppingListId).cache().queryList()
    BigDecimal totalPrice = (BigDecimal) 0.0
    for (GenericValue shoppingListItem : shoppingListItems) {
        GenericValue product = from("Product").where(productId: shoppingListItem.productId).cache().queryOne()
        Map calcPriceInMap = [:]
        calcPriceInMap << calcPriceInBaseMap

        calcPriceInMap.product = product
        calcPriceInMap.quantity = shoppingListItem.quantity
        if (shoppingListItem.modifiedPrice) {
            Map serviceResultCPP = run service:"calculateProductPrice", with: calcPriceInMap
            if (!ServiceUtil.isSuccess(serviceResultCPP)) {
                return error(serviceResultCPP.errorMessage)
            }
            calcPriceOutMap.price = serviceResultCPP.price
        }
        calcPriceOutMap.price = calcPriceOutMap.price ?: (BigDecimal) 0.0
        BigDecimal itemPrice = shoppingListItem.modifiedPrice ?: calcPriceOutMap.price
        BigDecimal shoppingListItemQuantity = shoppingListItem.quantity ?: (BigDecimal) 0.0
        totalPrice += (itemPrice * shoppingListItemQuantity)
    }
    List childshoppingLists = from("ShoppingList")
    .where(parentShoppingListId: parameters.shoppingListId, partyId: userLogin.partyId)
    .cache()
    .queryList()
    for (GenericValue childshoppingList : childshoppingLists) {
        Map calcChildPriceInMap = [:]
        calcChildPriceInMap << calcPriceInBaseMap
        calcChildPriceInMap.shoppingListId = childshoppingList.shoppingListId
        Map serviceResultCSLDTP = run service:"calculateShoppingListDeepTotalPrice", with: calcChildPriceInMap
        if (!ServiceUtil.isSuccess(serviceResultCSLDTP)) {
            return error(serviceResultCSLDTP.errorMessage)
        }
        calcPriceOutMap.totalPrice = serviceResultCSLDTP.totalPrice
        totalPrice += calcPriceOutMap.totalPrice
    }

    result.totalPrice = totalPrice

    return result
}

/**
 * Checks security on a ShoppingList
 * @return
 */
def checkShoppingListSecurity() {
    Map result = success()
    Boolean hasPermission = false
    if (userLogin && (userLogin.userLoginId != "anonymous") && parameters.partyId && (userLogin.partyId != parameters.partyId)
    && !security.hasEntityPermission("PARTYMGR", "_${parameters.permissionAction}", parameters.userLogin)) {
        return error(UtilProperties.getMessage("OrderErrorUiLabels", "OrderSecurityErrorToRunForAnotherParty", parameters.locale))
    } else {
        hasPermission = true
    }
    result.hasPermission = hasPermission
    return result
}

/**
 * Checks security on a ShoppingListIte
 * @return
 */
def checkShoppingListItemSecurity() {
    Map result = success()
    Boolean hasPermission = false
    GenericValue shoppingList = from("ShoppingList").where(parameters).queryOne()
    if (shoppingList?.partyId && !(userLogin.partyId.equals(shoppingList.partyId)) 
        && !security.hasEntityPermission("PARTYMGR", "_${parameters.permissionAction}", parameters.userLogin)) {
        Map errorLog = [:]
        errorLog = [parentMethodName: parameters.parentMethodName]
        errorLog.permissionAction = parameters.permissionAction
        return error(UtilProperties.getMessage("OrderErrorUiLabels", "OrderSecurityErrorToRunForAnotherParty", errorLog, parameters.locale))
    } else {
        hasPermission = true
    }
    result.hasPermission = hasPermission
    return result
}

/**
 * Add suggestions to a shopping list
 * @return
 */
def addSuggestionsToShoppingList() {
    Map result = success()
    String shoppingListId
    // first check the ProductStore.enableAutoSuggestionList indicator
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()
    if (!(orderHeader?.productStoreId)) {
        return result
    }
    GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
    if (productStore.enableAutoSuggestionList != "Y") {
        return result
    }

    GenericValue orderRole = from ("OrderRole").where(orderId: parameters.orderId, roleTypeId: "PLACING_CUSTOMER").queryFirst()

    GenericValue shoppingList = from("ShoppingList").where(partyId: orderRole.partyId, listName: "Auto Suggestions").queryFirst()
    if (!shoppingList) {
        Map createShoppingListInMap = [partyId: orderRole.partyId, listName: "Auto Suggestions", shoppingListTypeId: "SLT_WISH_LIST",
            productStoreId: parameters.productStoreId]
        Map serviceResultCSL = dispatcher.runSync("createShoppingList", createShoppingListInMap, 7200, true)
        if (!ServiceUtil.isSuccess(serviceResultCSL)) {
            return error(serviceResultCSL.errorMessage)
        }
        shoppingListId = serviceResultCSL.serviceResult
    } else {
        shoppingListId = shoppingList.shoppingListId
    }
    List orderItemList = from ("OrderItem").where(orderId: parameters.orderId).orderBy("orderItemSeqId").queryList()
    for (GenericValue orderItem : orderItemList) {
        if (orderItem.productId) {
            List compProductAssocList = from("ProductAssoc").where(productId: orderItem.productId, productAssocTypeId: "PRODUCT_COMPLEMENT")
                .filterByDate().queryList()
            for (GenericValue compProductAssoc : compProductAssocList) {
                Map shoppingListParameters = [productId: compProductAssoc.productIdTo, shoppingListId: shoppingListId, quantity: (BigDecimal) 1]
                Map serviceResultADSLI = run service:"addDistinctShoppingListItem", with: shoppingListParameters
                if (!ServiceUtil.isSuccess(serviceResultADSLI)) {
                    return error(serviceResultADSLI.errorMessage)
                }
            }
            GenericValue product = from("Product").where(productId: orderItem.productId).queryOne()
            if (product.isVariant == "Y") {
                GenericValue virtualProductAssoc = from("ProductAssoc").where(productIdTo: orderItem.productId,
                    productAssocTypeId: "PRODUCT_VARIANT").filterByDate().queryFirst()
                if (virtualProductAssoc) {
                    compProductAssocList = from("ProductAssoc").where(productId: virtualProductAssoc.productId,
                        productAssocTypeId: "PRODUCT_COMPLEMENT").filterByDate().queryList()
                    for (GenericValue compProductAssoc : compProductAssocList) {
                        Map shoppingListParameters = [productId: compProductAssoc.productIdTo, shoppingListId: shoppingListId]
                        shoppingListParameters.quantity = (BigDecimal) 1
                        Map serviceResult = run service:"addDistinctShoppingListItem", with: shoppingListParameters
                        if (!ServiceUtil.isSuccess(serviceResult)) {
                            return error(serviceResult.errorMessage)
                        }
                    }
                }
            }
        }
    }
    return result
}
