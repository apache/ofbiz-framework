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
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.serialize.XmlSerializer
import org.apache.ofbiz.product.product.KeywordIndex
import org.apache.ofbiz.product.product.ProductWorker
import org.apache.ofbiz.service.ServiceUtil

import java.sql.Timestamp

/**
 * Create a Product
 */
def createProduct() {
    Map result = success()
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)
            || security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", parameters.locale))
    }

    GenericValue newEntity = makeValue("Product", parameters)
    if (!newEntity.productId) {
        newEntity.productId = delegator.getNextSeqId("Product")
    } else {
        String errorMessage = UtilValidate.checkValidDatabaseId(newEntity.productId)
        if (errorMessage) {
            return error(errorMessage)
        }
        GenericValue dummyProduct = from("Product").where(parameters).queryOne()
        if (dummyProduct) {
            return error(UtilProperties.getMessage("CommonErrorUiLabels", "CommonErrorDuplicateKey", parameters.locale))
        }
    }
    result.productId = newEntity.productId

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    newEntity.createdDate = nowTimestamp
    newEntity.lastModifiedDate = nowTimestamp
    newEntity.lastModifiedByUserLogin = userLogin.userLoginId
    newEntity.createdByUserLogin = userLogin.userLoginId
    newEntity.isVariant = newEntity.isVariant ?: "N"
    newEntity.isVirtual = newEntity.isVirtual ?: "N"
    newEntity.billOfMaterialLevel = newEntity.billOfMaterialLevel ?: 0l
    newEntity.create()

/*
 *  if setting the primaryProductCategoryId create a member entity too 
 *  THIS IS REMOVED BECAUSE IT CAUSES PROBLEMS FOR WORKING ON PRODUCTION SITES
 *  <if-not-empty field="newEntity.primaryProductCategoryId">
 *  <make-value entity-name="ProductCategoryMember" value-field="newMember"/>
 *  <set from-field="productId" map-name="newEntity" to-field-name="productId" to-map-name="newMember"/>
 *  <set from-field="primaryProductCategoryId" map-name="newEntity" to-field-name="productCategoryId" to-map-name="newMember"/>
 *  <now-timestamp field="nowStamp"/>
 *  <set from-field="nowStamp" field="newMember.fromDate"/>
 *  <create-value value-field="newMember"/>
 *   </if-not-empty>
 */

    // if the user has the role limited position, add this product to the limit category/ies

    if (security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin)) {
        List productCategoryRoles = from("ProductCategoryRole")
                .where(partyId: userLogin.partyId, roleTypeId: "LTD_ADMIN")
                .queryList()

        for (GenericValue productCategoryRole : productCategoryRoles) {
            // add this new product to the category
            GenericValue newLimitMember = makeValue("ProductCategoryMember")
            newLimitMember.productId = newEntity.productId
            newLimitMember.productCategoryId = productCategoryRole.productCategoryId
            newLimitMember.fromDate = nowTimestamp
            newLimitMember.create()
        }
    }

    return result
}

    //FIXME maybe convert to entity-auto
/**
 * Update a product
 */
def updateProduct() {
    Map res = checkProductRelatedPermission("updateProduct", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = from("Product").where(parameters).queryOne()

    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.lastModifiedDate = UtilDateTime.nowTimestamp()
    lookedUpValue.lastModifiedByUserLogin = userLogin.userLoginId
    lookedUpValue.store()

    return success()
}

/**
 * Update a Product Name from quick admin
 */
def updateProductQuickAdminName() {
    Map res = checkProductRelatedPermission("updateQuickAdminName", "UPDATE")

    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue lookedUpValue = from("Product").where(parameters).queryOne()
    lookedUpValue.productName = parameters.productName
    if ("Y" == lookedUpValue.isVirtual) {
        lookedUpValue.internalName = lookedUpValue.productName
    }

    lookedUpValue.lastModifiedDate = UtilDateTime.nowTimestamp()
    lookedUpValue.lastModifiedByUserLogin = userLogin.userLoginId

    lookedUpValue.store()

    if ("Y" == lookedUpValue.isVirtual) {
        // get all variant products, to update their productNames
        Map variantProductAssocMap = [productId: parameters.productId, productAssocTypeId: "PRODUCT_VARIANT"]

        // get all productAssocs, then get the actual product to update
        List variantProductAssocs = from("ProductAssoc")
                .where(variantProductAssocMap)
                .filterByDate()
                .queryList()
        for (GenericValue variantProductAssoc : variantProductAssocs) {
            GenericValue variantProduct = from("Product").where(productId: variantProductAssoc.productIdTo).queryOne()

            variantProduct.productName = parameters.productName
            variantProduct.lastModifiedDate = UtilDateTime.nowTimestamp()
            variantProduct.lastModifiedByUserLogin = userLogin.userLoginId
            variantProduct.store()
        }
    }
    return success()
}

/**
 * Duplicate a Product
 */
def duplicateProduct() {
    String callingMethodName = "duplicateProduct"
    Map res = checkProductRelatedPermission(callingMethodName, "CREATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    res = checkProductRelatedPermission(callingMethodName, "DELETE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue dummyProduct = from("Product").where(parameters).queryOne()
    if (dummyProduct) {
        return error(UtilProperties.getMessage("CommonErrorUiLabels", "CommonErrorDuplicateKey", parameters.locale))
    }

    // look up the old product and clone it
    GenericValue oldProduct = from("Product").where(productId: parameters.oldProductId).queryOne()
    GenericValue newProduct = oldProduct.clone()

    // set the productId, and write it to the datasource
    newProduct.productId = parameters.productId

    // if requested, set the new internalName field
    if (parameters.newInternalName) {
        newProduct.internalName = parameters.newInternalName
    }

    // if requested, set the new productName field
    if (parameters.newProductName) {
        newProduct.productName = parameters.newProductName
    }

    // if requested, set the new description field
    if (parameters.newDescription) {
        newProduct.description = parameters.newDescription
    }

    // if requested, set the new longDescription field
    if (parameters.newLongDescription) {
        newProduct.longDescription = parameters.newLongDescription
    }
    newProduct.create()

    // set up entity filter
    Map productFindContext = [productId: parameters.oldProductId]
    Map reverseProductFindContext = [productIdTo: parameters.oldProductId]

    // if requested, duplicate related data as well
    List relationToDuplicate = []
    if (parameters.duplicatePrices) relationToDuplicate << "ProductPrice"
    if (parameters.duplicateIDs) relationToDuplicate << "GoodIdentification"
    if (parameters.duplicateContent) relationToDuplicate << "ProductContent"
    if (parameters.duplicateCategoryMembers) relationToDuplicate << "ProductCategoryMember"
    if (parameters.duplicateAttributes) relationToDuplicate << "ProductAttribute"
    if (parameters.duplicateFeatureAppls) relationToDuplicate << "ProductFeatureAppl"
    if (parameters.duplicateAssocs) {
        relationToDuplicate << "ProductAssoc"

        // small difference here, also do the reverse assocs...
        List foundValues = from("ProductAssoc").where(reverseProductFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productIdTo = parameters.productId
            newTempValue.create()
        }
    }

    // duplicate by generic process
    relationToDuplicate.each {
        from(it).where(productFindContext).queryList().each {
            GenericValue newTempValue = it.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }

    if (parameters.duplicateInventoryItems) {
        List foundValues = from("InventoryItem").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            /*
             *      NOTE: new inventory items should always be created calling the
             *            createInventoryItem service because in this way we are sure
             *            that all the relevant fields are filled with default values.
             *            However, the code here should work fine because all the values
             *            for the new inventory item are inerited from the existing item.
             *      TODO: is this code correct? What is the meaning of duplicating inventory items?
             *            What about the InventoryItemDetail entries?
             */
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            // this one is slightly different because it needs a new sequenced inventoryItemId
            newTempValue.inventoryItemId = delegator.getNextSeqId("InventoryItem")
            newTempValue.create()
        }
    }

    // if requested, remove related data as well
    List relationToRemove = []
    if (parameters.removePrices) relationToRemove << "ProductPrice"
    if (parameters.removeIDs) relationToRemove << "GoodIdentification"
    if (parameters.removeContent) relationToRemove << "ProductContent"
    if (parameters.removeCategoryMembers) relationToRemove << "ProductCategoryMember"
    if (parameters.removeAttributes) relationToRemove << "ProductAttribute"
    if (parameters.removeFeatureAppls) relationToRemove << "ProductFeatureAppl"
    if (parameters.removeInventoryItems) relationToRemove << "InventoryItem"
    if (parameters.removeAssocs) {
        relationToRemove << "ProductAssoc"
        // small difference here, also do the reverse assocs...
        delegator.removeByAnd("ProductAssoc", reverseProductFindContext)
    }
    relationToRemove.each {
        delegator.removeByAnd(it, productFindContext)
    }
    return success()
}

    // Product Keyword Services

/**
 * induce all the keywords of a product
 */
def forceIndexProductKeywords() {
    GenericValue product = from("Product").where(parameters).cache().queryOne()
    KeywordIndex.forceIndexKeywords(product)
    return success()
}

/**
 * delete all the keywords of a produc
 */
def deleteProductKeywords() {
    GenericValue product = from("Product").where(parameters).cache().queryOne()
    product.removeRelated("ProductKeyword")
    return success()
}

/**
 * Index the Keywords for a Product
 */
def indexProductKeywords() {
    //this service is meant to be called from an entity ECA for entities that include a productId
    //if it is the Product entity itself triggering this action, then a [productInstance] parameter
    //will be passed and we can save a few cycles looking that up
    GenericValue productInstance = parameters.productInstance
    if (!productInstance) {
        productInstance = from("Product").where(parameters).queryOne()
    }
    //induce keywords if autoCreateKeywords is empty or Y
    if (!productInstance.autoCreateKeywords || "Y" == productInstance.autoCreateKeywords) {
        KeywordIndex.indexKeywords(productInstance)
    }
    return success()
}

/**
 *  Discontinue Product Sales
 *  set sales discontinuation date to now
 */
def discontinueProductSales() {
    // set sales discontinuation date to now 
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    GenericValue product = from("Product").where(parameters).queryOne()
    product.salesDiscontinuationDate = nowTimestamp
    product.store()


    // expire product from all categories
    exprBldr = new EntityConditionBuilder()
    condition = exprBldr.AND() {
        EQUALS(productId: product.productId)
        EQUALS(thruDate: null)
    }
    delegator.storeByCondition("ProductCategoryMember",
            [thruDate: nowTimestamp], condition)
    // expire product from all associations going to it
    delegator.storeByCondition("ProductAssoc",
            [thruDate: nowTimestamp], condition)
    return success()
}


def countProductView() {
    long weight = parameters.weight ?: 1l

    GenericValue productCalculatedInfo = from("ProductCalculatedInfo").where(parameters).queryOne()
    if (!productCalculatedInfo) {
        // go ahead and create it
        productCalculatedInfo = makeValue("ProductCalculatedInfo")
        productCalculatedInfo.productId = parameters.productId
        productCalculatedInfo.totalTimesViewed = weight
        productCalculatedInfo.create()
    } else {
        productCalculatedInfo.totalTimesViewed += weight
        productCalculatedInfo.store()
    }

    // do the same for the virtual product...
    GenericValue product = from("Product").where(parameters).cache().queryOne()
    ProductWorker productWorker = new ProductWorker()
    String virtualProductId = productWorker.getVariantVirtualId(product)
    if (virtualProductId) {
        run service: "countProductView", with: [productId: virtualProductId, weight: weight]
    }
    return success()
}

/**
 * Create a ProductReview
 */
def createProductReview() {
    GenericValue newEntity = makeValue("ProductReview", parameters)
    newEntity.userLoginId = userLogin.userLoginId
    newEntity.statusId = "PRR_PENDING"

    // code to check for auto-approved reviews (store setting)
    GenericValue productStore = from("ProductStore").where(parameters).cache().queryOne()
    if (productStore && "Y" == productStore.autoApproveReviews) {
        newEntity.statusId = "PRR_APPROVED"
    }

    // create the new ProductReview
    newEntity.productReviewId = delegator.getNextSeqId("ProductReview")
    Map result = success()
    result.productReviewId = newEntity.productReviewId

    if (!newEntity.postedDateTime) {
        newEntity.postedDateTime = UtilDateTime.nowTimestamp()
    }
    newEntity.create()

    String productId = newEntity.productId
    String successMessage = UtilProperties.getMessage("ProductUiLabels",
            "ProductCreateProductReviewSuccess", parameters.locale)
    updateProductWithReviewRatingAvg(productId)

    return result
}

/**
 *  Update ProductReview
 */
def updateProductReview() {
    Map res = checkProductRelatedPermission("updateProductReview", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = from("ProductReview").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()

    String productId = lookedUpValue.productId
    updateProductWithReviewRatingAvg(productId)

    return success()
}

/**
 * change the product review Status
 */
def setProductReviewStatus() {
    Map res = checkProductRelatedPermission("setProductReviewStatus", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue productReview = from("ProductReview").where(parameters).queryOne()
    if (productReview && productReview.statusId != parameters.statusId) {
        if (from("StatusValidChange")
                .where(statusId: productReview.statusId, statusIdTo: parameters.statusId)
                .queryCount() == 0) {
            String errorMessage = UtilProperties.getMessage("ProductErrorUiLabels",
                    ProductReviewErrorCouldNotChangeOrderStatusFromTo, parameters.locale)
            logError(errorMessage)
            return error(errorMessage)
        }
    }

    productReview.statusId = parameters.statusId
    productReview.store()
    Map result = success()
    result.productReviewId = productReview.productReviewId

    return result
}

/**
 * Update Product with new Review Rating Avg
 * this method is meant to be called in-line and depends in a productId parameter
 */
def updateProductWithReviewRatingAvg(String productId) {
    ProductWorker productWorker = new ProductWorker()
    BigDecimal averageCustomerRating = productWorker.getAverageProductRating(delegator, productId)
    logInfo("Got new average customer rating " + averageCustomerRating)

    if (averageCustomerRating == 0) {
        return success()
    }

    // update the review average on the ProductCalculatedInfo entity
    GenericValue productCalculatedInfo = from("ProductCalculatedInfo").where(parameters).queryOne()
    if (!productCalculatedInfo) {
        // go ahead and create it
        productCalculatedInfo = makeValue("ProductCalculatedInfo")
        productCalculatedInfo.productId = productId
        productCalculatedInfo.averageCustomerRating = averageCustomerRating
        productCalculatedInfo.create()
    } else {
        productCalculatedInfo.averageCustomerRating = averageCustomerRating
        productCalculatedInfo.store()
    }

    return success()
}

/**
 * Updates the Product's Variants
 */
def copyToProductVariants() {
    String callingMethodName = "copyToProductVariants"
    Map res = checkProductRelatedPermission(callingMethodName, "CREATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    res = checkProductRelatedPermission(callingMethodName, "DELETE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    Map productFindContext = [productId: parameters.virtualProductId]
    GenericValue oldProduct = findOne("Product", productFindContext, false)

    Map variantsFindContext = [productId: parameters.virtualProductId, productAssocTypeId: "PRODUCT_VARIANT"]
    List variants = from("ProductAssoc")
            .where(variantsFindContext)
            .filterByDate()
            .queryList()
    for (GenericValue newProduct : variants) {
        Map productVariantContext = [productId: newProduct.productIdTo]

        // if requested, duplicate related data
        List relationToDuplicate = []
        if (parameters.duplicatePrices) relationToDuplicate << "ProductPrice"
        if (parameters.duplicateIDs) relationToDuplicate << "GoodIdentification"
        if (parameters.duplicateContent) relationToDuplicate << "ProductContent"
        if (parameters.duplicateCategoryMembers) relationToDuplicate << "ProductCategoryMember"
        if (parameters.duplicateAttributes) relationToDuplicate << "ProductAttribute"
        if (parameters.duplicateFacilities) relationToDuplicate << "ProductFacility"
        if (parameters.duplicateLocations) relationToDuplicate << "ProductFacilityLocation"
        relationToDuplicate.each {
            if (parameters.removeBefore) {
                delegator.removeByCondition(it, productVariantContext)
            }
            List foundValues = from(it).where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
    }
    return success()
}

/**
 * Check Product Related Permission
 * a method to centralize product security code, meant to be called in-line with
 */
def checkProductRelatedPermission(String callingMethodName, String checkAction) {
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    if (UtilValidate.isEmpty(checkAction)) {
        checkAction = "UPDATE"
    }
    List roleCategories = []
    // find all role-categories that this product is a member of
    if (parameters.productId && !security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)) {
        Map lookupRoleCategoriesMap = [productId : parameters.productId,
                                       partyId   : userLogin.partyId,
                                       roleTypeId: "LTD_ADMIN"]
        roleCategories = from("ProductCategoryMemberAndRole")
                .where(lookupRoleCategoriesMap)
                .filterByDate("roleFromDate", "roleThruDate")
                .queryList()
    }

    if (!(security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)
            || (roleCategories && security.hasEntityPermission("CATALOG_ROLE", "_${checkAction}", parameters.userLogin))
            || (parameters.alternatePermissionRoot &&
            security.hasEntityPermission(parameters.alternatePermissionRoot, checkAction, parameters.userLogin)))) {
        String checkActionLabel = "ProductCatalog${checkAction.charAt(0)}${checkAction.substring(1).toLowerCase()}PermissionError"
        return error(UtilProperties.getMessage("ProductUiLabels", checkActionLabel,
                [resourceDescription: callingMethodName, mainAction: checkAction], parameters.locale))
    }
    return success()
}

/**
 * call checkProductRelatedPermission function with support permission service interface
 */
def checkProductRelatedPermissionService() {
    parameters.alternatePermissionRoot = parameters.altPermission
    Map result = checkProductRelatedPermission(parameters.resourceDescription, parameters.mainAction)
    result.hasPermission = ServiceUtil.isSuccess(result)
    return result
}

/**
 * Main permission logic
 */
def productGenericPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("ProductUiLabels",
                "ProductMissingMainActionInPermissionService", parameters.locale))
    }

    Map result = success()
    result.hasPermission = ServiceUtil.isSuccess(
            checkProductRelatedPermission(parameters.resourceDescription, parameters.mainAction))
    if (!result.hasPermission) {
        result = fail(UtilProperties.getMessage("ProductUiLabels", "ProductPermissionError", parameters.locale))
    }
    return result
}

/**
 * product price permission logic
 */
def productPriceGenericPermission() {
    String mainAction = parameters.mainAction
    if (!mainAction) {
        return error(UtilProperties.getMessage("ProductUiLabels",
                "ProductMissingMainActionInPermissionService", parameters.locale))
    }

    Map result = success()
    if (!security.hasPermission("CATALOG_PRICE_MAINT", parameters.userLogin)) {
        result = error(UtilProperties.getMessage("ProductUiLabels",
                "ProductPriceMaintPermissionError", parameters.locale))
    }
    result.hasPermission = ServiceUtil.isSuccess(result) && checkProductRelatedPermission(parameters.resourceDescription, mainAction)
    if (!result.hasPermission) {
        result = fail(UtilProperties.getMessage("ProductUiLabels", "ProductPermissionError", parameters.locale))
    }
    return result
}

/**
 * ================================================================
 * ProductRole Services
 * ================================================================
 */

/**
 * Add Party to Product
 */
def addPartyToProduct() {
    //TODO convert to entity-auto
    Map result = checkProductRelatedPermission("addPartyToProduct", "CREATE")
    if (!ServiceUtil.isSuccess(result)) {
        return result
    }
    GenericValue newEntity = makeValue("ProductRole", parameters)

    if (!newEntity.fromDate) {
        newEntity.fromDate = UtilDateTime.nowTimestamp()
    }
    newEntity.create()
    return success()
}

/**
 * Update Party to Product
 */
def updatePartyToProduct() {
    //TODO convert to entity-auto
    Map result = checkProductRelatedPermission("updatePartyToProduct", "UPDATE")
    if (!ServiceUtil.isSuccess(result)) {
        return result
    }
    GenericValue lookupPKMap = makeValue("ProductRole")
    lookupPKMap.setPKFields(parameters)
    GenericValue lookedUpValue = findOne("ProductRole", lookupPKMap, false)
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    return success()
}

/**
 * Remove Party From Product
 */
def removePartyFromProduct() {
    //TODO convert to entity-auto
    Map res = checkProductRelatedPermission("removePartyFromProduct", "DELETE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    Map lookupPKMap = makeValue("ProductRole")
    lookupPKMap.setPKFields(parameters)
    GenericValue lookedUpValue = findOne("ProductRole", lookupPKMap, false)
    lookedUpValue.remove()

    return success()
}

    // ProductCategoryGlAccount methods
/**
 * Create a ProductCategoryGlAccount
 */
def createProductCategoryGlAccount() {
    //TODO convert to entity-auto
    Map res = checkProductRelatedPermission("createProductCategoryGlAccount", "CREATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue newEntity = makeValue("ProductCategoryGlAccount", parameters)
    newEntity.create()

    return success()
}

/**
 * Update a ProductCategoryGlAccount
 */
def updateProductCategoryGlAccount() {
    //TODO convert to entity-auto
    Map res = checkProductRelatedPermission("updateProductCategoryGlAccount", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }

    GenericValue lookedUpValue = findOne("ProductCategoryGlAccount", parameters, false)
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()

    return success()
}

/**
 * Delete a ProductCategoryGlAccount
 */
def deleteProductCategoryGlAccount() {
    //TODO convert to entity-auto
    Map res = checkProductRelatedPermission("deleteProductCategorGLAccount", "DELETE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = findOne("ProductCategoryGlAccount", parameters, false)
    lookedUpValue.remove()

    return success()
}

// Product GroupOrder Services

/**
 * Create ProductGroupOrder
 */
    //TODO convert to entity-auto
def createProductGroupOrder() {
    GenericValue newEntity = makeValue("ProductGroupOrder")
    delegator.setNextSubSeqId(newEntity, "groupOrderId", 5, 1)
    Map result = success()
    result.groupOrderId = newEntity.groupOrderId
    newEntity.setNonPKFields(parameters)
    newEntity.create()

    return result
}

/**
 * Update ProductGroupOrder
 */
def updateProductGroupOrder() {
    GenericValue productGroupOrder = from("ProductGroupOrder").where(parameters).queryOne()
    productGroupOrder.setNonPKFields(parameters)
    productGroupOrder.store()

    if ("GO_CREATED" == productGroupOrder.statusId) {
        GenericValue jobSandbox = from("JobSandbox").where(jobId: productGroupOrder.jobId).queryOne()
        if (jobSandbox) {
            jobSandbox.runTime = parameters.thruDate
            jobSandbox.store()
        }
    }
    return success()
}

/**
 * Delete ProductGroupOrder
 */
def deleteProductGroupOrder() {
    GenericValue productGroupOrder = from("ProductGroupOrder").where(parameters).queryOne()
    productGroupOrder.remove()
    productGroupOrder.removeRelated("OrderItemGroupOrder")

    GenericValue jobSandbox = from("JobSandbox").where(jobId: productGroupOrder.jobId).queryOne()
    if (jobSandbox) {
        jobSandbox.remove()
        jobSandbox.removeRelated("RuntimeData")
    }
    return success()
}

/**
 * Create ProductGroupOrder
 */
def createJobForProductGroupOrder() {
    GenericValue productGroupOrder = from("ProductGroupOrder").where(parameters).queryOne()
    if (productGroupOrder.jobId) {
        // Create RuntimeData For ProductGroupOrder
        Map runtimeDataMap = [groupOrderId: parameters.groupOrderId]
        XmlSerializer xmlSerializer = new XmlSerializer()
        String runtimeInfo = xmlSerializer.serialize(runtimeDataMap)

        GenericValue runtimeData = makeValue("RuntimeData")
        runtimeData.runtimeDataId = delegator.getNextSeqId("RuntimeData")
        String runtimeDataId = runtimeData.runtimeDataId
        runtimeData.runtimeInfo = runtimeInfo
        runtimeData.create()

        // Create Job For ProductGroupOrder
        // FIXME: Jobs should not be manually created
        Map jobFields = [jobId             : delegator.getNextSeqId("JobSandbox"),
                         jobName           : "Check ProductGroupOrder Expired",
                         runTime           : parameters.thruDate,
                         poolId            : "pool",
                         statusId          : "SERVICE_PENDING",
                         serviceName       : "checkProductGroupOrderExpired",
                         runAsUser         : "system",
                         runtimeDataId     : runtimeDataId,
                         maxRecurrenceCount: 1l,
                         priority          : 50l]
        delegator.create("JobSandbox", jobFields)

        productGroupOrder.jobId = jobFields.jobId
        productGroupOrder.store()
    }
    return success()
}

/**
 * Check OrderItem For ProductGroupOrder
 */
def checkOrderItemForProductGroupOrder() {
    List orderItems = from("OrderItem").where(orderId: parameters.orderId).queryList()
    for (GenericValue orderItem : orderItems) {
        String productId = orderItem.productId
        GenericValue product = from("Product").where(productId: orderItem.productId).queryOne()
        if ("Y" == product.isVariant) {
            GenericValue variantProductAssoc = from("ProductAssoc")
                    .where(productIdTo: orderItem.productId, productAssocTypeId: "PRODUCT_VARIANT")
                    .filterByDate()
                    .queryFirst()
            productId = variantProductAssoc.productId
        }
        GenericValue productGroupOrder = from("ProductGroupOrder")
                .where(productId: productId)
                .filterByDate()
                .queryFirst()
        if (productGroupOrder) {
            productGroupOrder.soldOrderQty = productGroupOrder.soldOrderQty ?: 0l
            productGroupOrder.soldOrderQty += orderItem.quantity
            productGroupOrder.store()

            run service: "createOrderItemGroupOrder", with: [orderId       : orderItem.orderId,
                                                             orderItemSeqId: orderItem.orderItemSeqId,
                                                             groupOrderId  : productGroupOrder.groupOrderId]
        }
    }
    return success()
}

/**
 * Cancle OrderItemGroupOrder
 */
def cancleOrderItemGroupOrder() {
    Map orderItemCond = [orderId: parameters.orderId]
    if (parameters.orderItemSeqId) orderItemCond.orderItemSeqId = parameters.orderItemSeqId
    List orderItems = from("OrderItem")
            .where(orderItemCond)
            .queryList()
    for (GenericValue orderItem : orderItems) {
        GenericValue orderItemGroupOrder = from("OrderItemGroupOrder")
                .where(orderId: orderItem.orderId, orderItemSeqId: orderItem.orderItemSeqId)
                .queryFirst()
        if (orderItemGroupOrder) {
            GenericValue productGroupOrder = from("ProductGroupOrder")
                    .where(groupOrderId: orderItemGroupOrder.groupOrderId).queryOne()
            if (productGroupOrder) {
                if ("GO_CREATED" == productGroupOrder.statusId) {
                    if ("ITEM_CANCELLED" == orderItem.statusId) {
                        BigDecimal cancelQuantity = orderItem.cancelQuantity ?: orderItem.quantity
                        productGroupOrder.soldOrderQty -= cancelQuantity
                    }
                    productGroupOrder.store()
                    orderItemGroupOrder.remove()
                }
            }
        }
    }
    return success()
}

/**
 * Check ProductGroupOrder Expired
 */
def checkProductGroupOrderExpired() {
    GenericValue productGroupOrder = from("ProductGroupOrder").where(parameters).queryOne()
    if (productGroupOrder) {
        String groupOrderStatusId
        String newItemStatusId
        if (productGroupOrder.soldOrderQty >= productGroupOrder.reqOrderQty) {
            newItemStatusId = "ITEM_APPROVED"
            groupOrderStatusId = "GO_SUCCESS"
        } else {
            newItemStatusId = "ITEM_CANCELLED"
            groupOrderStatusId = "GO_CANCELLED"
        }
        run service: "updateProductGroupOrder", with: [groupOrderId: productGroupOrder.groupOrderId,
                                                       statusId    : groupOrderStatusId]

        List orderItemGroupOrders = from("OrderItemGroupOrder")
                .where(groupOrderId: productGroupOrder.groupOrderId)
                .queryList()
        for (GenericValue orderItemGroupOrder : orderItemGroupOrders) {
            run service: "changeOrderItemStatus", with: [orderId       : orderItemGroupOrder.orderId,
                                                         orderItemSeqId: orderItemGroupOrder.orderItemSeqId,
                                                         statusId      : newItemStatusId]
        }
    }
    return success()
}

