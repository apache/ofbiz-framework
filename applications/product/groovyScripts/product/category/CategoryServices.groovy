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


import org.apache.ofbiz.entity.util.EntityUtilProperties

import java.sql.Timestamp
import java.util.Objects

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil


/*
 * ================================================================
 * ProductCategory Services
 * ================================================================
 */

/**
 * Create an ProductCategory
 */
def createProductCategory() {
    def resourceDescription = parameters.rescourceDescription ?: "createProductCategory"
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)
        || security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError",
            [resourceDescription: resourceDescription], parameters.locale))
    }

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    GenericValue newEntity = makeValue("ProductCategory", parameters)

    if (!parameters.productCategoryId) {
        newEntity.productCategoryId = delegator.getNextSeqId("ProductCategory")
    } else {
        newEntity.productCategoryId = parameters.productCategoryId
        def errorMessage = UtilValidate.checkValidDatabaseId(newEntity.productCategoryId)
        if (errorMessage != null) {
            return error(errorMessage)
        }
    }
    Map result = success()
    result.productCategoryId = newEntity.productCategoryId
    newEntity.create()

    if (security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin)) {
        List productCategoryRoles = from("ProductCategoryRole")
            .where("partyId", parameters.userLogin.partyId, "roleTypeId", "LTD_ADMIN")
            .filterByDate()
            .queryList()
        for (def productCategoryRole : productCategoryRoles) {
            // add this new product to the category
            GenericValue newLimitRollup = makeValue("ProductCategoryRollup", [
                productCategoryId: newEntity.productCategoryId,
                parentProductCategoryId: productCategoryRole.productCategoryId,
                fromDate: nowTimestamp
            ])
            newLimitRollup.create()
        }
    }
    return result
}

/**
 * Update an ProductCategory
 */
def updateProductCategory() {
    Map res = checkCategoryRelatedPermission("updateProductCategory", "UPDATE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = from("ProductCategory").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    return success()
}

/*
 * ================================================================
 * ProductCategoryMember Services
 * ================================================================
 */

/**
 * Add Product to Multiple Categories
 */
def addProductToCategories() {
    Map addProductToCategoryMap = dispatcher.dispatchContext.makeValidContext("addProductToCategory", ModelService.IN_PARAM, parameters)
    if (parameters.categories instanceof java.util.List) {
        for (def category : parameters.categories) {
            addProductToCategoryMap.productCategoryId = category
            run service: "addProductToCategory", with: addProductToCategoryMap
        }
    } else {
        /* Note that the security semantics require the user to have the general admin permission,
           or the role limited permission and association with the category, not the product */
        Map res = checkCategoryRelatedPermission("addProductToCategories", "CREATE", parameters.categories, null)
        if (!ServiceUtil.isSuccess(res)) {
            return res
        }

        addProductToCategoryMap.productCategoryId = parameters.categories
        run service: "addProductToCategory", with: addProductToCategoryMap
    }
    return success()
}

/**
 * Remove Product From Category
 */
def removeProductFromCategory() {
    // If the associated category was the primary category for the product, clear that field
    GenericValue product = from("Product").where(parameters).queryOne()
    if (Objects.equals(product?.primaryProductCategoryId, parameters.productCategoryId)) {
        product.primaryProductCategoryId = null
        product.store()
    }
    GenericValue lookedUpValue = from('ProductCategoryMember').where(parameters).queryOne()
    lookedUpValue.remove()
    return success()
}

/*
 * ================================================================
 * ProductCategoryRole Services
 * ================================================================
 */

/**
 * Add Party to Category
 */
def addPartyToCategory() {
    Map res = checkCategoryRelatedPermission("addPartyToCategory", "CREATE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue newEntity = makeValue("ProductCategoryRole", parameters)
    if (!newEntity.fromDate) {
        newEntity.fromDate = UtilDateTime.nowTimestamp()
    }
    newEntity.create()
    return success()
}

/**
 * Update Party to Category Application
 */
def updatePartyToCategory() {
    Map res = checkCategoryRelatedPermission("updatePartyToCategory", "UPDATE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = from("ProductCategoryRole").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    return success()
}

/**
 * Remove Party From Category
 */
def removePartyFromCategory() {
    Map res = checkCategoryRelatedPermission("removePartyFromCategory", "DELETE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = from("ProductCategoryRole").where(parameters).queryOne()
    lookedUpValue.remove()
    return success()
}

/*
 * ================================================================
 * ProductCategoryRollup Services
 * ================================================================
 */

/**
 * Add ProductCategory to Category 
 */
def addProductCategoryToCategory() {
    Map res = checkCategoryRelatedPermission("addProductCategoryToCategory", "CREATE", null, "parentProductCategoryId")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue newEntity = makeValue("ProductCategoryRollup", parameters)

    if (!newEntity.fromDate) {
        newEntity.fromDate = UtilDateTime.nowTimestamp()
    }

    newEntity.create()
    return success()
}

/**
 * Add ProductCategory to Categories
 */
def addProductCategoryToCategories() {
    if (parameters.categories instanceof java.util.List) {
        for (def category : parameters.categories) {
            // note the the user must be associated with the parent category with the role limited permission
            Map res = checkCategoryRelatedPermission("addProductCategoryToCategories", "CREATE", category, null)
            if (!ServiceUtil.isSuccess(res)) {
                return res
            }
            GenericValue newEntity = makeValue("ProductCategoryRollup", parameters)
            newEntity.parentProductCategoryId = category

            if (!newEntity.fromDate) {
                newEntity.fromDate = UtilDateTime.nowTimestamp()
            }
            newEntity.create()
        }
    } else {
        // note the the user must be associated with the parent category with the role limited permission
        Map res = checkCategoryRelatedPermission("addProductCategoryToCategories", "CREATE", "parameters.categories", null)
        if (!ServiceUtil.isSuccess(res)) {
            return res
        }
        GenericValue newEntity = makeValue("ProductCategoryRollup", parameters)
        newEntity.parentProductCategoryId = parameters.categories

        if (!newEntity.fromDate) {
            newEntity.fromDate = UtilDateTime.nowTimestamp()
        }

        newEntity.create()
    }
    return success()
}

/**
 * Update ProductCategory to Category Application
 */
def updateProductCategoryToCategory() {
    // note the the user must be associated with the parent category with the role limited permission
    Map res = checkCategoryRelatedPermission("updateProductCategoryToCategory", "UPDATE", null, "parentProductCategoryId")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue lookedUpValue = from("ProductCategoryRollup").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    Map result = success()
    result.productCategoryId = parameters.productCategoryId
    return result
}

/**
 * Remove ProductCategory From Category
 */
def removeProductCategoryFromCategory() {
    // note the the user must be associated with the parent category with the role limited permission
    Map res = checkCategoryRelatedPermission("removeProductCategoryFromCategory", "DELETE", null, "parentProductCategoryId")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue lookedUpValue = from("ProductCategoryRollup").where(parameters).queryOne()
    lookedUpValue.remove()
    return success()
}

/*
 * ================================================================
 * Special Category Function Services
 * ================================================================
 */

/**
 * copy CategoryProduct Members to a CategoryProductTo
 */
def copyCategoryProductMembers() {
    Map res = checkCategoryRelatedPermission("copyCategoryProductMembers", "CREATE", null, "productCategoryIdTo")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    def query = from("ProductCategoryMember").where("productCategoryId", parameters.productCategoryId)
    if (parameters.validDate) {
        query.filterByDate()
    }
    List productCategoryMembers = query.queryList()

    // add each to a list to store and then store all and let the entity engine do inserts or updates as needed; much more reliable/useful
    List pcmsToStore = []
    for (def productCategoryMember : productCategoryMembers) {
        def newProductCategoryMember = productCategoryMember.clone()
        parameters.productCategoryIdTo = newProductCategoryMember.productCategoryId
        pcmsToStore.add(newProductCategoryMember)
    }
    delegator.storeAll(pcmsToStore)

    if (parameters.recurse == "Y") {
        // call this service for each sub-category in the rollup with the same productCategoryIdTo
        Map lookupChildrenMap = [parentProductCategoryId: parameters.productCategoryId]
        query = from("ProductCategoryRollup").where(lookupChildrenMap)

        if (parameters.validDate) {
            query.filterByDate()
        }
        List productCategoryRollups = query.queryList()

        Map callServiceMap = [:]
        for (def productCategoryRollup : productCategoryRollups) {
            callServiceMap = [
                productCategoryId: productCategoryRollup.productCategoryId,
                productCategoryIdTo: parameters.productCategoryIdTo,
                validDate: parameters.validDate,
                recurse: parameters.recurse
            ]
            run service: "copyCategoryProductMembers", with: callServiceMap
        }
    }
    return success()
}

/**
 * a service wrapper for copyCategoryEntities
 */
def duplicateCategoryEntities() {
    def resourceDescription = parameters.resourceDescription ?: "duplicateCategoryEntities"
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)
        || security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", [resourceDescription: resourceDescription], parameters.locale))
    }

    copyCategoryEntities(parameters.entityName, parameters.productCategoryId, parameters.productCategoryIdTo, parameters.validDate)
    return success()
}

/**
 * copies all entities of entityName with a productCategoryId to a new entity with a productCategoryIdTo,
 * filtering them by a timestamp passed in to validDate if necessary
 * @param entityName
 * @param productCategoryId
 * @param productCategoryIdTo
 * @param validDate
 */
def copyCategoryEntities(entityName, productCategoryId, productCategoryIdTo, validDate) {
    def query = from(entityName).where("productCategoryId", productCategoryId)
    if (validDate) {
        query.filterByDate()
    }
    List categoryEntities = query.queryList()

    // add each to a list to store and then store all and let the entity engine do inserts or updates as needed; much more reliable/useful
    List entitiesToStore = []
    for (def categoryEntity : categoryEntities) {
        def newCategoryEntity = categoryEntity.clone()
        newCategoryEntity.productCategoryId = productCategoryIdTo
        entitiesToStore.add(newCategoryEntity)
    }
    delegator.storeAll(entitiesToStore)
}

/**
 * Remove ProductCategory From Category
 */
def expireAllCategoryProductMembers() {
    Map res = checkCategoryRelatedPermission("expireAllCategoryProductMembers", "UPDATE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    Timestamp expireTimestamp = null
    if (parameters.thruDate) {
        expireTimestamp = parameters.thruDate
    } else {
        expireTimestamp = UtilDateTime.nowTimestamp()
    }

    List productCategoryMembers = from("ProductCategoryMember").where("productCategoryId", parameters.productCategoryId).queryList()

    for (GenericValue productCategoryMember : productCategoryMembers) {
        productCategoryMember.thruDate = expireTimestamp
        productCategoryMember.store()
    }
    return success()
}

/**
 * Remove ProductCategory From Category
 */
def removeExpiredCategoryProductMembers() {
    Map res = checkCategoryRelatedPermission("removeExpiredCategoryProductMembers", "DELETE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    Timestamp expireTimestamp = null
    if (parameters.validDate) {
        expireTimestamp = parameters.validDate
    } else {
        expireTimestamp = UtilDateTime.nowTimestamp()
    }

    List productCategoryMembers = from("ProductCategoryMember").where("productCategoryId", parameters.productCategoryId).queryList()

    for (GenericValue productCategoryMember : productCategoryMembers) {
        Timestamp thruDate = productCategoryMember.thruDate
        if (thruDate && thruDate.before(expireTimestamp)) {
            productCategoryMember.remove()
        }
    }
    return success()
}

/*
 * ================================================================
 * Special Category Related Create Services
 * ================================================================
 */

/**
 * Create a Product in a Category along with special information such as features
 *
 */
def createProductInCategory() {
    Map res = checkCategoryRelatedPermission("createProductInCategory", "CREATE", null, null)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    if (!parameters.currencyUomId) {
        parameters.currencyUomId = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator)
    }

    // create product
    if (!parameters.productTypeId) {
        parameters.productTypeId = "FINISHED_GOOD"
    }
    Map cPRes = run service: "createProduct", with: parameters
    def productId = cPRes.productId
    Map result = success()
    result.productId = productId

    // create ProductCategoryMember
    Map callCreateProductCategoryMemberMap = [
        productId: productId,
        productCategoryId: parameters.productCategoryId
    ]
    run service: "addProductToCategory", with: callCreateProductCategoryMemberMap

    // create defaultPrice and averageCost ProductPrice
    if (parameters.defaultPrice) {
        Map createDefaultPriceMap = [
            productId: productId,
            currencyUomId: parameters.currencyUomId,
            price: parameters.defaultPrice,
            productStoreGroupId: "_NA_",
            productPriceTypeId: "DEFAULT_PRICE",
            productPricePurposeId: "PURCHASE",
        ]
        run service: "createProductPrice", with: createDefaultPriceMap
    }

    if (parameters.averageCost) {
        Map createAverageCostMap = [
            productId: productId,
            currencyUomId: parameters.currencyUomId,
            price: parameters.averageCost,
            productStoreGroupId: "_NA_",
            productPriceTypeId: "AVERAGE_COST",
            productPricePurposeId: "PURCHASE",
        ]
        run service: "createProductPrice", with: createAverageCostMap
    }

    // create ProductFeatureAppl(s)
    String hasSelectableFeatures = "N"
    for (Map entry : parameters.productFeatureIdByType.entrySet()) {
        def productFeatureTypeId = entry.getKey()
        def productFeatureId = entry.getValue()
        logInfo("Applying feature [${productFeatureId}] of type [${productFeatureTypeId}] to product [${productId}]")
        Map createPfaMap = [productId: productId, productFeatureId: productFeatureId]
        if (parameters.productFeatureSelectableByType[productFeatureTypeId].equals("Y")) {
            createPfaMap.productFeatureApplTypeId = "SELECTABLE_FEATURE"
            hasSelectableFeatures = "Y"
        } else {
            createPfaMap.productFeatureApplTypeId = "STANDARD_FEATURE"
        }
        run service: "applyFeatureToProduct", with: createPfaMap
        createPfaMap = null
    }

    // set isVirtual based on hasSelectableFeatures
    if (hasSelectableFeatures.equals("Y")) {
        GenericValue newProduct = from("Product").where(parameters).queryOne()
        newProduct.isVirtual = "Y"
        newProduct.store()
    }
    return result
}

/**
 * Duplicate a ProductCategory
 */
def duplicateProductCategory() {
    def resourceDescription = parameters.resourceDescription ?: "duplicateProductCategory"
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)
        || security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError",
                [resourceDescription: resourceDescription], parameters.locale))
    }
    
    if (findOne("ProductCategory", [productCategoryId: parameters.productCategoryId], false)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCategoryAlreadyExists",
            [resourceDescription: resourceDescription], parameters.locale))
    }

    // look up the old product category and clone it
    GenericValue oldCategory = from('ProductCategory').where([productCategoryId: parameters.oldProductCategoryId]).queryOne()
    GenericValue newCategory = oldCategory.clone()

    // set the new product category id, and write it to the datasource
    newCategory.productCategoryId = parameters.productCategoryId
    newCategory.create()
    def productCategoryId = parameters.oldProductCategoryId
    def productCategoryIdTo = parameters.productCategoryId

    // if requested, duplicate related data as well
    if (parameters.duplicateMembers) {
        copyCategoryEntities("ProductCategoryMember", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateContent) {
        copyCategoryEntities("ProductCategoryContent", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateRoles) {
        copyCategoryEntities("ProductCategoryRole", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateAttributes) {
        copyCategoryEntities("ProductCategoryAttribute", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateFeatureCategories) {
        copyCategoryEntities("ProductFeatureCategoryAppl", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateFeatureGroups) {
        copyCategoryEntities("ProductFeatureCatGrpAppl", productCategoryId, productCategoryIdTo, null)
    }
    if (parameters.duplicateCatalogs) {
        copyCategoryEntities("ProdCatalogCategory", productCategoryId, productCategoryIdTo, null)
    }

    /* Parent rollups are where oldProductCategoryId = ProductCategoryRollup.productCategoryId,
     * but the child roll up is where oldProductCategoryId = ProductCategoryRollup.parentProductCategoryId
     * and hence requires a new find-by map */
    def foundValues = []
    if (parameters.duplicateParentRollup) {
        foundValues = from("ProductCategoryRollup").where("productCategoryId", parameters.oldProductCategoryId).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productCategoryId = parameters.productCategoryId
            newTempValue.create()
        }
    }
    if (parameters.duplicateChildRollup) {
        foundValues = from("ProductCategoryRollup").where("parentProductCategoryId", parameters.oldProductCategoryId).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.parentProductCategoryId = parameters.productCategoryId
            newTempValue.create()
        }
    }
    return success()
}

/*
 * ================================================================
 * Product Category Attribute Services
 * ================================================================
 */

/**
 * Create an attribute for a product category
 */
def createProductCategoryAttribute() {
 
    def resourceDescription = parameters.resourceDescription ?: "createProductCategoryAttribute"
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError",
            [resourceDescription: resourceDescription], parameters.locale))
    }
    
    // check if the new attribute-name is unique to the product-category-id
    exprBldr = new EntityConditionBuilder()
    condition = exprBldr.AND() {
        EQUALS(productCategoryId: parameters.productCategoryId)
        EQUALS(attrName: parameters.attrName)
    }
    List existingData = from('ProductCategoryAttribute').where(condition).queryList()
    if (existingData) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCategoryAttrAlreadyExists",
            [resourceDescription: resourceDescription], parameters.locale))
    }
    GenericValue newEntity = makeValue("ProductCategoryAttribute", parameters)
    newEntity.create()
    return success()
}

/**
 * Update an association between two product categories
 */
def updateProductCategoryAttribute() {
    def resourceDescription = parameters.resourceDescription ?: "updateProductCategoryAttribute"
    if (!(security.hasEntityPermission("CATALOG", "_UPDATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogUpdatePermissionError",
            [resourceDescription: resourceDescription], parameters.locale))
    }

    GenericValue productCategoryAttributeInstance = from("ProductCategoryAttribute").where(parameters).queryOne()
    productCategoryAttributeInstance.setNonPKFields(parameters)
    productCategoryAttributeInstance.store()
    return success()
}

/**
 * Delete an association between two product categories
 */
def deleteProductCategoryAttribute() {
    def resourceDescription = parameters.resourceDescription ?: "deleteProductCategoryAttribute"
    if (!(security.hasEntityPermission("CATALOG", "_DELETE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogDeletePermissionError",
            [resourceDescription: resourceDescription], parameters.locale))
    }

    GenericValue productCategoryAttributeInstance = from("ProductCategoryAttribute").where(parameters).queryOne()
    productCategoryAttributeInstance.remove()
    return success()
}

// ProductCategoryLink Create/Update/Delete
/**
 * create a ProductCategoryLink
 */
def createProductCategoryLink() {
    GenericValue newEntity = makeValue("ProductCategoryLink")
    newEntity.productCategoryId = parameters.productCategoryId

    // don't set the fromDate yet; let's get the seq ID first
    if(!parameters.linkSeqId) {
        delegator.setNextSubSeqId(newEntity, "linkSeqId", 5, 1)
    }

    // now set the rest of the PK fields (should just be fromDate now; unless linkSeqId is not empty)
    if (!newEntity.fromDate) {
        newEntity.fromDate = UtilDateTime.nowTimestamp()
    }
    newEntity.create()
    return success()
}

/*
 * ================================================================
 * Permission Methods
 * ================================================================
 */

/**
 * Check Product Category Related Permission
 */
def checkCategoryRelatedPermission(callingMethodName, checkAction, productCategoryIdToCheck, productCategoryIdName) {
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    if (!checkAction) {
        checkAction = "UPDATE"
    }
    if (!productCategoryIdName) {
        productCategoryIdName = "productCategoryId"
    }
    if (!productCategoryIdToCheck) {
        productCategoryIdToCheck = parameters."${productCategoryIdName}"
    }

    // find all role-categories that this category is a member of
    List roleCategories = []
    if (!(security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin))) {
        roleCategories = from("ProductCategoryRollupAndRole")
            .where("productCategoryId", productCategoryIdToCheck, "partyId", parameters.userLogin.partyId, "roleTypeId", "LTD_ADMIN")
            .filterByDate()
            .queryList()
        roleCategories = EntityUtil.filterByDate(roleCategories, UtilDateTime.nowTimestamp(), "roleFromDate", "roleThruDate", true)
    }
    logInfo("Checking category permission, roleCategories=${roleCategories}")
    Map result = success()
    result.hasPermission = true
    if (!(security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)
        || (security.hasEntityPermission("CATALOG_ROLE", "_${checkAction}", parameters.userLogin)
        && roleCategories))) {
        logVerbose("Permission check failed, user does not have the correct permission.")
        result = error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError",
            [resourceDescription: callingMethodName], parameters.locale))
    }
    return result
}

/**
 * Main permission logic
 */
def productCategoryGenericPermission() {
    if (!parameters.mainAction) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale))
    }

    Map result = success()
    Map res = checkCategoryRelatedPermission(parameters.resourceDescription, parameters.mainAction, null, null)
    if (ServiceUtil.isSuccess(res)) {
        result.hasPermission = true
    } else {
        result.failMessage = UtilProperties
            .getMessage("ProductUiLabels", "ProductPermissionError",
                [resourceDescription: parameters.resourceDescription, mainAction: parameters.mainAction],
                parameters.locale)
        result.hasPermission = false
    }
    return result
}

// A service version of checkCategoryRelatedPermission, only with purchase/viewAllowPermReqd taken into account
/**
 * Check Product Category Permission With View and Purchase Allow
 */
def checkCategoryPermissionWithViewPurchaseAllow() {
    Map genericResult = run service: "productCategoryGenericPermission", with: parameters
    if (genericResult.hasPermission.equals(false)) {
        Map result = [
            hasPermission: genericResult.hasPermission,
            failMessage: genericResult.failMessage
        ]
        return result
    }

    // If the generic permission test passed, carry on
    boolean hasPermission = true

    // Set up for a call to ckeckCategoryRelatedPermission below, but callingMethodName is needed sooner
    String resourceDescription = parameters.resourceDescription
    if (!resourceDescription) {
        resourceDescription = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    String callingMethodName = resourceDescription
    String checkAction = parameters.mainAction ?: "UPDATE"

    EntityCondition condition = EntityCondition.makeCondition([
        EntityCondition.makeCondition([
            EntityCondition.makeCondition("prodCatalogCategoryTypeId", "PCCT_VIEW_ALLW"),
            EntityCondition.makeCondition("prodCatalogCategoryTypeId", "PCCT_PURCH_ALLW")
        ], EntityOperator.OR),
        EntityCondition.makeCondition("productCategoryId", parameters.productCategoryId)
    ])
    List prodCatalogCategoryList = from("ProdCatalogCategory").where(condition).filterByDate().queryList()
    String failMessage = ""
    for (Map prodCatalogCategory : prodCatalogCategoryList) {
        // Do not do a permission check, unless the ProdCatalog requires it
        def prodCatalog = from('ProdCatalog').where([prodCatalogId: prodCatalogCategory.prodCatalogId]).queryOne()
        if (prodCatalog.viewAllowPermReqd.equals("Y")
            && !security.hasEntityPermission("CATALOG_VIEW", "_ALLOW", parameters.userLogin)) {
            logVerbose("Permission check failed, user does not have permission")
            failMessage = UtilProperties.getMessage("CommonUiLabels", "CommmonCallingMethodPermissionError", [callingMethodName, "CATALOG_VIEW_ALLOW"], parameters.locale)
            hasPermission = false
        }
        if (prodCatalog.purchaseAllowPermReqd.equals("Y")
            && !security.hasEntityPermission("CATALOG_PURCHASE", "_ALLOW", parameters.userLogin)) {
            logVerbose("Permission check failed, user does not have permission")
            failMessage = UtilProperties.getMessage("CommonUiLabels", "CommonCallingMethodPermissionError", [callingMethodName, "CATALOG_PURCHASE_ALLOW"], parameters.locale)
            hasPermission = false
        }
    }

    Map result = success()
    result.failMessage = failMessage
    result.hasPermission = hasPermission
    return result
}

// To help dynamically populate a products dropdown given a product category id from a change in another dropdown, possibly sorted on sequenceNum
/**
 * Set the product options for selected product category, mostly used by getDependentDropdownValues
 */
def getAssociatedProductsList() {
    parameters.categoryId = parameters.productCategoryId
    Map getProductCategoryMembersMap = dispatcher.dispatchContext.makeValidContext("getProductCategoryMembers", ModelService.IN_PARAM, parameters)
    Map res = run service: "getProductCategoryMembers", with: getProductCategoryMembersMap
    List productsList = res.categoryMembers
    productsList = EntityUtil.orderBy(productsList, ["sequenceNum"])
    List products = []
    for (Map productMember : productsList) {
        GenericValue product = from('Product').where([productId: productMember.productId]).queryOne()
        String productName = "${product.internalName}: ${product.productId}"
        products.add(productName)
    }
    if (!products) {
        products.add(UtilProperties.getMessage("ProductUiLabels", "ProductNoProducts", parameters.locale))
    }
    Map result = success()
    result.products = products
    return result
}
