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
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.serialize.XmlSerializer
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.product.product.KeywordIndex
import org.apache.ofbiz.product.product.ProductWorker
import org.apache.ofbiz.service.ServiceUtil



 module = "ProductServices.groovy" // this is used for logging

 /**
  * Create a Product
  */
 def createProduct() {
    Map result = success()
    if (!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin)
        || security.hasEntityPermission("CATALOG_ROLE", "_CREATE", parameters.userLogin))) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", parameters.locale))
    }

    GenericValue newEntity = makeValue("Product")
    newEntity.setNonPKFields(parameters)
    
    newEntity.productId = parameters.productId

    if (UtilValidate.isEmpty(newEntity.productId)) {
        newEntity.productId = delegator.getNextSeqId("Product")
    } else {
        String errorMessage = UtilValidate.checkValidDatabaseId(newEntity.productId)
        if(errorMessage != null) {
            logError(errorMessage)
            return error(errorMessage)
        }
        GenericValue dummyProduct = findOne("Product", ["productId": parameters.productId], false)
        if (UtilValidate.isNotEmpty(dummyProduct)) {
             errorMessage = UtilProperties.getMessage("CommonErrorUiLabels", CommonErrorDuplicateKey, parameters.locale)
            logError(errorMessage)
            return error(errorMessage)
        }
    }
    result.productId = newEntity.productId
    
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    
    newEntity.createdDate = nowTimestamp
    newEntity.lastModifiedDate = nowTimestamp
    newEntity.lastModifiedByUserLogin = userLogin.userLoginId
    newEntity.createdByUserLogin = userLogin.userLoginId
    
    if (UtilValidate.isEmpty(newEntity.isVariant)) {
        newEntity.isVariant = "N"
    }
    if (UtilValidate.isEmpty(newEntity.isVirtual)) {
        newEntity.isVirtual = "N"
    }
    if (UtilValidate.isEmpty(newEntity.billOfMaterialLevel)) {
        newEntity.billOfMaterialLevel = (Long) 0
    }
    
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


    if (security.hasEntityPermission("CATALOG_ROLE","_CREATE", parameters.userLogin)) {
        List productCategoryRoles = from("ProductCategoryRole").where("partyId": userLogin.partyId, "roleTypeId": "LTD_ADMIN").queryList()
        
        for (GenericValue productCategoryRole : productCategoryRoles) {
            // add this new product to the category
            GenericValue newLimitMember = makeValue("ProductCategoryMember")
            newLimitMember.productId = newEntity.productId
            newLimitMember.productCateogryId = productCategoryRole.productCategoryId
            newLimitMember.fromDate = nowTimestamp
            newLimitMember.create()
        }
    }

    return result
} 

/**
 * Update a product
 */
def updateProduct() {
    Map res = checkProductRelatedPermission("updateProduct", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = findOne("Product", ["productId": parameters.productId], false)
    // save this value before overwriting it so we can compare it later
    Map saveIdMap = ["primaryProductCategoryId": lookedUpValue.primaryProductCategoryId]
    
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
    
    GenericValue lookedUpValue = findOne("Product", ["productId": parameters.productId], false)
    lookedUpValue.productName = parameters.productName
    if ("Y".equals(lookedUpValue.isVirtual)) {
        lookedUpValue.internalName = lookedUpValue.productName
    }
    
    lookedUpValue.lastModifiedDate = UtilDateTime.nowTimestamp();
    lookedUpValue.lastModifiedByUserLogin = userLogin.userLoginId
    
    lookedUpValue.store()
    
    if ("Y".equals(lookedUpValue.isVirtual)) {
        // get all variant products, to update their productNames
        Map variantProductAssocMap = ["productId": parameters.productId, "productAssocTypeId": "PRODUCT_VARIANT"]
        
        // get all productAssocs, then get the actual product to update
        List variantProductAssocs = from("ProductAssoc").where(variantProductAssocMap).queryList()
        variantProductAssocs = EntityUtil.filterByDate(variantProductAssocs)
        for(GenericValue variantProductAssoc : variantProductAssocs) {
            GenericValue variantProduct = null
            variantProduct = findOne("Product", ["productId": variantProductAssoc.productIdTo], false)
            
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
    String checkAction = "CREATE"
    Map res = checkProductRelatedPermission(callingMethodName, checkAction)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    checkAction = "DELETE"
    res = checkProductRelatedPermission(callingMethodName, checkAction)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue dummyProduct = findOne("Product", ["productId": parameters.productId], false)
    if (UtilValidate.isNotEmpty(dummyProduct)) {
        String errorMessage = UtilProperties.getMessage("CommonErrorUiLabels", CommonErrorDuplicateKey, parameters.locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    
    // look up the old product and clone it
    GenericValue oldProduct = findOne("Product", ["productId": parameters.oldProductId], false)
    GenericValue newProduct = oldProduct.clone()
    
    // set the productId, and write it to the datasource
    newProduct.productId = parameters.productId
    
    // if requested, set the new internalName field
    if (UtilValidate.isNotEmpty(parameters.newInternalName)) {
        newProduct.internalName = parameters.newInternalName
    }
    
    // if requested, set the new productName field
    if (UtilValidate.isNotEmpty(parameters.newProductName)) {
        newProduct.productName = parameters.newProductName
    }
    
    // if requested, set the new description field
    if (UtilValidate.isNotEmpty(parameters.newDescription)) {
        newProduct.description = parameters.newDescription
    }
    
    // if requested, set the new longDescription field
    if (UtilValidate.isNotEmpty(parameters.newLongDescription)) {
        newProduct.longDescription = parameters.newLongDescription
    }
    
    newProduct.create()
    
    // set up entity filter
    Map productFindContext = ["productId": parameters.oldProductId]
    Map reverseProductFindContext = ["productIdTo": parameters.oldProductId]
    
    // if requested, duplicate related data as well
    if (UtilValidate.isNotEmpty(parameters.duplicatePrices)) {
        List foundValues = from("ProductPrice").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateIDs)) {
        List foundValues = from("GoodIdentification").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateContent)) {
        List foundValues = from("ProductContent").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateCategoryMembers)) {
        List foundValues = from("ProductCategoryMember").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateAssocs)) {
        List foundValues = from("ProductAssoc").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
        
        // small difference here, also do the reverse assocs...
        foundValues = from("ProductAssoc").where("productIdTo": parameters.oldProductId).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productIdTo = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateAttributes)) {
        List foundValues = from("ProductAttribute").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateFeatureAppls)) {
        List foundValues = from("ProductFeatureAppl").where(productFindContext).queryList()
        for (GenericValue foundValue : foundValues) {
            GenericValue newTempValue = foundValue.clone()
            newTempValue.productId = parameters.productId
            newTempValue.create()
        }
    }
    if (UtilValidate.isNotEmpty(parameters.duplicateInventoryItems)) {
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
    if (UtilValidate.isNotEmpty(parameters.removePrices)) {
        delegator.removeByAnd("ProductPrice", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeIDs)) {
        delegator.removeByAnd("GoodIdentification", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeContent)) {
        delegator.removeByAnd("ProductContent", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeCategoryMembers)) {
        delegator.removeByAnd("ProductCategoryMember", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeAssocs)) {
        delegator.removeByAnd("ProductAssoc", productFindContext)
        // small difference here, also do the reverse assocs...
        delegator.removeByAnd("ProductAssoc", reverseProductFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeAttributes)) {
        delegator.removeByAnd("ProductAttribute", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeFeatureAppls)) {
        delegator.removeByAnd("ProductFeatureAppl", productFindContext)
    }
    if (UtilValidate.isNotEmpty(parameters.removeInventoryItems)) {
        delegator.removeByAnd("InventoryItem", productFindContext)
    }
    return success()
}

// Product Keyword Services

/**
 * induce all the keywords of a product
 */
def forceIndexProductKeywords() {
    GenericValue product = findOne("Product", [productId: parameters.productId], false)
    KeywordIndex.forceIndexKeywords(product)
    return success()
}

/**
 * delete all the keywords of a produc
 */
def deleteProductKeywords() {
    GenericValue product = findOne("Product", [productId: parameters.productId], false)
    delegator.removeRelated("ProductKeyword", product)
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
    if (productInstance == null) {
        Map findProductMap = [productId: parameters.productId]
        productInstance = findOne("Product", findProductMap, false)
    }
    //induce keywords if autoCreateKeywords is empty or Y
    if (UtilValidate.isEmpty(productInstance.autoCreateKeywords) || "Y".equals(productInstance.autoCreateKeywords)) {
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
    GenericValue product = findOne("Product", parameters, false)
    product.salesDiscontinuationDate = nowTimestamp
    product.store()
    
    // expire product from all categories
    List productCategoryMembers = delegator.getRelated("ProductCategoryMember", null, null, product, false)
    for (GenericValue productCategoryMember : productCategoryMembers) {
        if (UtilValidate.isEmpty(productCategoryMember.thruDate)) {
            productCategoryMember.thruDate = UtilDateTime.nowTimestamp()
            productCategoryMember.store()
        }
    }
    // expire product from all associations going to it
    List assocProductAssocs = delegator.getRelated("AssocProductAssoc", null, null, product, false)
    for (GenericValue assocProductAssoc : assocProductAssocs) {
        if (UtilValidate.isEmpty(assocProductAssoc.thruDate)) {
            assocProductAssoc.thruDate = UtilDateTime.nowTimestamp()
            assocProductAssoc.store()
        }
    }
    return success()
}


def countProductView() {
    if (UtilValidate.isEmpty(parameters.weight)) {
        parameters.weight = (Long) 1
    }
    GenericValue productCalculatedInfo = findOne("ProductCalculatedInfo", ["productId": parameters.productId], false)
    if (UtilValidate.isEmpty(productCalculatedInfo)) {
        // go ahead and create it
        productCalculatedInfo = makeValue("ProductCalculatedInfo")
        productCalculatedInfo.productId = parameters.productId
        productCalculatedInfo.totalTimesViewed = parameters.weight
        productCalculatedInfo.create()
    } else {
        productCalculatedInfo.totalTimesViewed = productCalculatedInfo.totalTimesViewed + parameters.weight
        productCalculatedInfo.store()
    }
    
    // do the same for the virtual product...
    GenericValue product = findOne("Product", ["productId": parameters.productId], true)
    ProductWorker productWorker = new ProductWorker()
    String virtualProductId = productWorker.getVariantVirtualId(product)
    if (UtilValidate.isNotEmpty(virtualProductId)) {
        Map callSubMap = ["productId": virtualProductId, "weight": parameters.weight]
        run service: "countProductView", with: callSubMap
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
    GenericValue productStore = findOne("ProductStore", ["productStoreId": parameters.productStoreId], false)
    
    if (!UtilValidate.isEmpty(productStore)) {
        if ("Y".equals(productStore.autoApproveReviews)) {
            newEntity.statusId = "PRR_APPROVED"
        }
    }
    
    // create the new ProductReview
    newEntity.productReviewId = delegator.getNextSeqId("ProductReview")
    Map result = success()
    result.productReviewId = newEntity.productReviewId
    
    if (UtilValidate.isEmpty(newEntity.postedDateTime)) {
        newEntity.postedDateTime = UtilDateTime.nowTimestamp()
    }
    
    newEntity.create()
    
    String productId = newEntity.productId
    String successMessage = UtilProperties.getMessage("ProductUiLabels", "ProductCreateProductReviewSuccess", parameters.locale)
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
    
    GenericValue lookupPKMap = makeValue("ProductReview")
    lookupPKMap.setPKFields(parameters)
    GenericValue lookedUpValue = findOne("ProductReview", lookupPKMap, false)
    lookupPKMap.setNonPKFields(parameters)
    lookupPKMap.store()
    
    String productId = lookedUpValue.productId
    updateProductWithReviewRatingAvg(productId)
    
    return success()
}

/**
 * change the product review Status
 */
def setProductReviewStatus(){
    Map res = checkProductRelatedPermission("setProductReviewStatus", "UPDATE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    
    GenericValue productReview = findOne("ProductReview", parameters, false)
    if (UtilValidate.isNotEmpty(productReview)) {
        if (!productReview.statusId.equals(parameters.statusId)) {
            GenericValue statusChange = from("StatusValidChange")
                .where("statusId", productReview.statusId, "statusIdTo", parameters.statusId)
                .queryOne()
            if (UtilValidate.isEmpty(statusChange)) {
                String msg = "Status is not a valid change: from " + productReview.statusId + " to " + parameters.statusId
                logError(msg)
                String errorMessage = UtilProperties.getMessage("ProductErrorUiLabels", ProductReviewErrorCouldNotChangeOrderStatusFromTo, parameters.locale)
                logError(errorMessage)
                return error(errorMessage)
            }
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
    logInfo("Got new average customer rating "+ averageCustomerRating)
    
    if (averageCustomerRating == 0) {
        return success()
    }
    
    // update the review average on the ProductCalculatedInfo entity
    GenericValue productCalculatedInfo = findOne("ProductCalculatedInfo", parameters, false)
    if (UtilValidate.isEmpty(productCalculatedInfo)) {
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
    String checkAction = "CREATE"
    Map res = checkProductRelatedPermission(callingMethodName, checkAction)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    checkAction = "DELETE"
    res = checkProductRelatedPermission(callingMethodName, checkAction)
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    
    Map productFindContext = ["productId": parameters.virtualProductId]
    GenericValue oldProduct = findOne("Product", productFindContext, false)
    
    Map variantsFindContext = ["productId": parameters.virtualProductId, "productAssocTypeId": "PRODUCT_VARIANT"]

    List variants = from("ProductAssoc").where(variantsFindContext).filterByDate().queryList()
    List foundVariantValues = []
    List foundValues = []
    for (GenericValue newProduct : variants) {
        Map productVariantContext = ["productId": newProduct.productIdTo]
        // if requested, duplicate related data
        if (UtilValidate.isNotEmpty(parameters.duplicatePrices)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductPrice").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductPrice").where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateIDs)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("GoodIdentification").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("GoodIdentification").where(productFindContext).queryList() 
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
                }
            
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateContent)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductContent").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductContent").where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateCategoryMembers)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductCategoryMember").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductCategoryMember").where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateAttributes)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductAttribute").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductAttribute").where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateFacilities)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductFacility").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductFacility").where(productFindContext).queryList()
            for (GenericValue foundValue : foundValues) {
                GenericValue newTempValue = foundValue.clone()
                newTempValue.productId = newProduct.productIdTo
                newTempValue.create()
            }
        }
        if (UtilValidate.isNotEmpty(parameters.duplicateLocations)) {
            if (UtilValidate.isNotEmpty(parameters.removeBefore)) {
                foundVariantValues = from("ProductFacilityLocation").where(productVariantContext).queryList()
                for (GenericValue foundVariantValue : foundVariantValues) {
                    foundVariantValue.remove()
                }
            }
            foundValues = from("ProductFacilityLocation").where(productFindContext).queryList()
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
 * call-simple-method, and the checkAction and callingMethodName attributes should be in the method context
 */
def checkProductRelatedPermission (String callingMethodName, String checkAction){
    if (UtilValidate.isEmpty(callingMethodName)) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    if (UtilValidate.isEmpty(checkAction)) {
        checkAction = "UPDATE"
    }
    List roleCategories = []
    // find all role-categories that this product is a member of
    if (!security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)) {
        Map lookupRoleCategoriesMap = ["productId": parameters.productId, "partyId": userLogin.partyId, "roleTypeId": "LTD_ADMIN"]
        roleCategories = from("ProductCategoryMemberAndRole").where(lookupRoleCategoriesMap).filterByDate("roleFromDate", "roleThruDate").queryList()
    }
    
    if (! ((security.hasEntityPermission("CATALOG", "_${checkAction}", parameters.userLogin)) 
        || (security.hasEntityPermission("CATALOG_ROLE", "_${checkAction}", parameters.userLogin) && !UtilValidate.isEmpty(roleCategories)) 
        || (!UtilValidate.isEmpty(parameters.alternatePermissionRoot) && security.hasEntityPermission(parameters.alternatePermissionRoot, checkAction, parameters.userLogin)))) {
            String checkActionLabel = "ProductCatalog" + checkAction.charAt(0) + checkAction.substring(1).toLowerCase() + "PermissionError"
            String resourceDescription = callingMethodName
            
            String errorMessage = UtilProperties.getMessage("ProductUiLabels", checkActionLabel, parameters.locale)
            logError(errorMessage)
            return error(errorMessage)
        }
    return success()
}

/**
 * Main permission logic
 */
def productGenericPermission(){
    String mainAction = parameters.mainAction
    Map result = success()
    if (UtilValidate.isEmpty(mainAction)) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map res = checkProductRelatedPermission(parameters.resourceDescription, parameters.mainAction)
    if (!ServiceUtil.isSuccess(res)) {
        String failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductPermissionError", parameters.locale)
        Boolean hasPermission = false
        result = fail(failMessage)
        result.hasPermission = hasPermission
    } else {
        Boolean hasPermission = true
        result.hasPermission = hasPermission
    }
    return result
}

/**
 * product price permission logic
 */
def productPriceGenericPermission(){
    String mainAction = parameters.mainAction
    if (UtilValidate.isEmpty(mainAction)) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "ProductMissingMainActionInPermissionService", parameters.locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    Map result = success()
    if (!security.hasEntityPermission("CATALOG_PRICE_MAINT", null, parameters.userLogin)) {
        String errorMessage = UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", parameters.locale)
        logError(errorMessage)
        result = error(errorMessage)
    }
    Map res = checkProductRelatedPermission(null, null)
    if (ServiceUtil.isSuccess(result) && ServiceUtil.isSuccess(res)) {
        result.hasPermission = true
    } else {
        String failMessage = UtilProperties.getMessage("ProductUiLabels", "ProductPermissionError", parameters.locale)
        result = fail(failMessage)
        result.hasPermission = false
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
def addPartyToProduct(){
    Map result = checkProductRelatedPermission("addPartyToProduct", "CREATE")
    if (!ServiceUtil.isSuccess(result)) {
        return result
    }
    GenericValue newEntity = makeValue("ProductRole", parameters)
    
    if (UtilValidate.isEmpty(newEntity.fromDate)) {
        newEntity.fromDate = UtilDateTime.nowTimestamp()
    }
    newEntity.create()
    return success()
}

/**
 * Update Party to Product
 */
def updatePartyToProduct(){
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
def removePartyFromProduct(){
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
def createProductCategoryGlAccount(){
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
def updateProductCategoryGlAccount(){
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
def deleteProductCategoryGlAccount(){
    Map res = checkProductRelatedPermission("deleteProductCategorGLAccount", "DELETE")
    if (!ServiceUtil.isSuccess(res)) {
        return res
    }
    GenericValue lookedUpValue = findOne("ProductCategoryGlAccount", parameters, false)
    lookedUpValue.remove()
    
    return success()
}

// Product GroupOrder Services -->

/**
 * Create ProductGroupOrder
 */
def createProductGroupOrder(){
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
def updateProductGroupOrder(){
    GenericValue productGroupOrder = findOne("ProductGroupOrder", ["groupOrderId": parameters.groupOrderId], false)
    productGroupOrder.setNonPKFields(parameters)
    productGroupOrder.store()
    
    if ("GO_CREATED".equals(productGroupOrder.statusId)) {
        GenericValue jobSandbox = findOne("JobSandbox", ["jobId": productGroupOrder.jobId], false)
        if (UtilValidate.isNotEmpty(jobSandbox)) {
            jobSandbox.runTime = parameters.thruDate
            jobSandbox.store()
        }
    }
    return success()
}

/**
 * Delete ProductGroupOrder
 */
def deleteProductGroupOrder(){
    List orderItemGroupOrders = from("OrderItemGroupOrder").where("groupOrderId": parameters.groupOrderId).queryList()
    for (GenericValue orderItemGroupOrder : orderItemGroupOrders) {
        orderItemGroupOrder.remove()
    }
    GenericValue productGroupOrder = findOne("ProductGroupOrder", ["groupOrderId": parameters.groupOrderId], false)
    if (UtilValidate.isEmpty(productGroupOrder)) {
        return error("Entity value not found with name: " + productGroupOrder)
    }
    productGroupOrder.remove()
    
    GenericValue jobSandbox = findOne("JobSandbox", ["jobId": productGroupOrder.jobId], false)
    if (UtilValidate.isEmpty(jobSandbox)) {
        return error("Entity value not found with name: " + jobSandbox)
    }
    jobSandbox.remove()
    
    List jobSandboxList = from("JobSandbox").where("runtimeDataId": jobSandbox.runtimeDataId).queryList()
    for (GenericValue jobSandboxRelatedRuntimeData : jobSandboxList) {
        jobSandboxRelatedRuntimeData.remove()
    }
    
    GenericValue runtimeData = findOne("RuntimeData", ["runtimeDataId": jobSandbox.runtimeDataId], false)
    if (UtilValidate.isEmpty(runtimeData)) {
        return error("Entity value not found with name: " + runtimeData)
    }
    runtimeData.remove()
    
    return success()
}

/**
 * Create ProductGroupOrder
 */
def createJobForProductGroupOrder(){
    GenericValue productGroupOrder = findOne("ProductGroupOrder", ["groupOrderId": parameters.groupOrderId], false)
    if (UtilValidate.isEmpty(productGroupOrder.jobId)) {
        // Create RuntimeData For ProductGroupOrder
        Map runtimeDataMap = ["groupOrderId": parameters.groupOrderId]
        XmlSerializer xmlSerializer = new XmlSerializer()
        String runtimeInfo = xmlSerializer.serialize(runtimeDataMap)
        
        GenericValue runtimeData = makeValue("RuntimeData")
        runtimeData.runtimeDataId = delegator.getNextSeqId("RuntimeData")
        String runtimeDataId = runtimeData.runtimeDataId
        runtimeData.runtimeInfo = runtimeInfo
        runtimeData.create()
        
        // Create Job For ProductGroupOrder
        // FIXME: Jobs should not be manually created
        GenericValue jobSandbox = makeValue("JobSandbox")
        jobSandbox.jobId = delegator.getNextSeqId("JobSandbox")
        String jobId = jobSandbox.jobId
        jobSandbox.jobName = "Check ProductGroupOrder Expired"
        jobSandbox.runTime = parameters.thruDate
        jobSandbox.poolId = "pool"
        jobSandbox.statusId = "SERVICE_PENDING"
        jobSandbox.serviceName = "checkProductGroupOrderExpired"
        jobSandbox.runAsUser = "system"
        jobSandbox.runtimeDataId = runtimeDataId
        jobSandbox.maxRecurrenceCount = (Long) 1
        jobSandbox.priority = (Long) 50
        jobSandbox.create()
        
        productGroupOrder.jobId = jobId
        productGroupOrder.store()
    }
    return success()
}

/**
 * Check OrderItem For ProductGroupOrder
 */
def checkOrderItemForProductGroupOrder(){
    List orderItems = from("OrderItem").where("orderId": parameters.orderId).queryList()
    for (GenericValue orderItem : orderItems) {
        String productId = orderItem.productId
        GenericValue product = findOne("Product", ["productId": orderItem.productId], false)
        if ("Y".equals(product.isVariant)) {
            List variantProductAssocs = from("ProductAssoc").where("productIdTo": orderItem.productId, "productAssocTypeId": "PRODUCT_VARIANT").queryList()
            variantProductAssocs = EntityUtil.filterByDate(variantProductAssocs)
            GenericValue variantProductAssoc = variantProductAssocs.get(0)
            productId = variantProductAssoc.productId
        }
        List productGroupOrders = from("ProductGroupOrder").where("productId": productId).queryList()
        if (UtilValidate.isNotEmpty(productGroupOrders)) {
            productGroupOrders = EntityUtil.filterByDate(productGroupOrders)
            GenericValue productGroupOrder = productGroupOrders.get(0)
            if (UtilValidate.isEmpty(productGroupOrder.soldOrderQty)) {
                productGroupOrder.soldOrderQty = orderItem.quantity
            } else {
            productGroupOrder.soldOrderQty = productGroupOrder.soldOrderQty + orderItem.quantity
            }
            productGroupOrder.store()
            
            Map createOrderItemGroupOrderMap = ["orderId": orderItem.orderId, "orderItemSeqId": orderItem.orderItemSeqId, "groupOrderId": productGroupOrder.groupOrderId]
            
            run service: "createOrderItemGroupOrder", with: createOrderItemGroupOrderMap
        }
    }
    return success()
}

/**
 * Cancle OrderItemGroupOrder
 */
def cancleOrderItemGroupOrder(){
    List orderItems = []
    if (UtilValidate.isNotEmpty(parameters.orderItemSeqId)) {
        orderItems = from("OrderItem")
            .where("orderId", parameters.orderId, "orderItemSeqId", parameters.orderItemSeqId)
            .queryList()
    } else {
        orderItems = from("OrderItem")
            .where("orderId", parameters.orderId)
            .queryList()
    }
    for(GenericValue orderItem : orderItems) {
        List orderItemGroupOrders = from("OrderItemGroupOrder")
            .where("orderId", orderItem.orderId, "orderItemSeqId", orderItem.orderItemSeqId)
            .queryList()
        if (UtilValidate.isNotEmpty(orderItemGroupOrders)) {
            GenericValue orderItemGroupOrder = orderItemGroupOrders.get(0)
            GenericValue productGroupOrder = findOne("ProductGroupOrder", [groupOrderId: orderItemGroupOrder.groupOrderId], false)
            
            if (UtilValidate.isNotEmpty(productGroupOrder)) {
                if ("GO_CREATED".equals(productGroupOrder.statusId)) {
                    if ("ITEM_CANCELLED".equals(orderItem.statusId)) {
                        BigDecimal cancelQuantity
                        if (UtilValidate.isNotEmpty(orderItem.cancelQuantity)) {
                            cancelQuantity = orderItem.cancelQuantity
                        } else {
                            cancelQuantity = orderItem.quantity
                        }
                        productGroupOrder.soldOrderQty = productGroupOrder.soldOrderQty - cancelQuantity
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
def checkProductGroupOrderExpired(){
    GenericValue productGroupOrder = findOne("ProductGroupOrder", parameters, false)
    if (UtilValidate.isNotEmpty(productGroupOrder)) {
        String groupOrderStatusId
        String newItemStatusId
        if (productGroupOrder.soldOrderQty >= productGroupOrder.reqOrderQty) {
            newItemStatusId = "ITEM_APPROVED"
            groupOrderStatusId = "GO_SUCCESS"
        } else {
            newItemStatusId = "ITEM_CANCELLED"
            groupOrderStatusId = "GO_CANCELLED"
        }
    Map updateProductGroupOrderMap = [:]
    updateProductGroupOrderMap.groupOrderId = productGroupOrder.groupOrderId
    updateProductGroupOrderMap.statusId = groupOrderStatusId
    run service: "updateProductGroupOrder", with: updateProductGroupOrderMap
    
    List orderItemGroupOrders = from("OrderItemGroupOrder")
        .where("groupOrderId", productGroupOrder.groupOrderId)
        .queryList()
    for(GenericValue orderItemGroupOrder : orderItemGroupOrders) {
        Map changeOrderItemStatusMap = ["orderId": orderItemGroupOrder.orderId, "orderItemSeqId": orderItemGroupOrder.orderItemSeqId, "statusId": newItemStatusId]
        run service: "changeOrderItemStatus", with: changeOrderItemStatusMap
    }
    return success()
    }
}

