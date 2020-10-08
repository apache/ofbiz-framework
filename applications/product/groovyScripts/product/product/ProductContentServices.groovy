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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

/**
 * Create Content For Product
 * @return
 */
def createProductContent() {
    Map result = success()
    GenericValue newEntity = makeValue("ProductContent", parameters)
    newEntity.create()
    run service: "updateContent", with: parameters
    result.contentId = newEntity.contentId
    result.productId = newEntity.productId
    result.productContentTypeId = newEntity.productContentTypeId
    return result
}

/**
 * Update Content For Product
 * @return
 */
def updateProductContent() {
    GenericValue lookedUpValue = from("ProductContent").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    run service: "updateContent", with: parameters
    return success()
}

/**
 * Create Email Content For Product
 * @return
 */
def createEmailContentForProduct() {
    Map result = success()
    Map createProductContent = parameters
    Map serviceResult = run service: "createEmailContent", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    createProductContent.contentId = serviceResult.contentId
    run service: "createProductContent", with: createProductContent
    return result
}

/**
 * Create Download Content For Product
 * @return
 */
def createDownloadContentForProduct() {
    Map createProductContent = parameters
    Map persistContentAndAssoc = parameters
    persistContentAndAssoc.contentTypeId = "DOCUMENT"
    persistContentAndAssoc.dataResourceTypeId = "IMAGE_OBJECT"
    persistContentAndAssoc.contentName = parameters._imageData_fileName
    persistContentAndAssoc.mimeTypeId = parameters._imageData_contentType
    
    Map serviceResult = run service: "persistContentAndAssoc", with: persistContentAndAssoc
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    createProductContent.contentId = serviceResult.contentId
    run service: "createProductContent", with: createProductContent
    return success()
}


/**
 * Create Simple Text Content For Product
 * @return
 */
def createSimpleTextContentForProduct() {
    Map serviceResult = run service: "createSimpleTextContent", with: parameters
    Map createProductContentMap = parameters
    createProductContentMap.contentId = serviceResult.contentId
    run service: "createProductContent", with: createProductContentMap
    return success()
}

/**
 * Create Simple Text Content For Alternate Locale
 * @return
 */
def createSimpleTextContentForAlternateLocale() {
    Map serviceResult = run service: "createSimpleTextContent", with: parameters
    Map createContentAssocMap = [contentIdTo: serviceResult.contentId,
                                 contentId: parameters.mainContentId,
                                 contentAssocTypeId: "ALTERNATE_LOCALE"]
    run service: "createContentAssoc", with: createContentAssocMap
    return success()
}

/**
 * Method to upload multiple Additional View images for product
 * @return
 */
def uploadProductAdditionalViewImages() {
    Map result = success()
    Map addAdditionalViewForProductMap = [productId: parameters.productId]
    if (parameters.additionalImageOne) {
        addAdditionalViewForProductMap.uploadedFile = parameters.additionalImageOne
        addAdditionalViewForProductMap.productContentTypeId = "ADDITIONAL_IMAGE_1"
        addAdditionalViewForProductMap._uploadedFile_fileName = parameters._additionalImageOne_fileName
        addAdditionalViewForProductMap._uploadedFile_contentType = parameters._additionalImageOne_contentType
        run service: "addAdditionalViewForProduct", with: addAdditionalViewForProductMap
    }
    if (parameters.additionalImageTwo) {
        addAdditionalViewForProductMap.uploadedFile = parameters.additionalImageTwo
        addAdditionalViewForProductMap.productContentTypeId = "ADDITIONAL_IMAGE_2"
        addAdditionalViewForProductMap._uploadedFile_fileName = parameters._additionalImageTwo_fileName
        addAdditionalViewForProductMap._uploadedFile_contentType = parameters._additionalImageTwo_contentType
        run service: "addAdditionalViewForProduct", with: addAdditionalViewForProductMap
    }
    if (parameters.additionalImageThree) {
        addAdditionalViewForProductMap.uploadedFile = parameters.additionalImageThree
        addAdditionalViewForProductMap.productContentTypeId = "ADDITIONAL_IMAGE_3"
        addAdditionalViewForProductMap._uploadedFile_fileName = parameters._additionalImageThree_fileName
        addAdditionalViewForProductMap._uploadedFile_contentType = parameters._additionalImageThree_contentType
        run service: "addAdditionalViewForProduct", with: addAdditionalViewForProductMap
    }
    if (parameters.additionalImageFour) {
        addAdditionalViewForProductMap.uploadedFile = parameters.additionalImageFour
        addAdditionalViewForProductMap.productContentTypeId = "ADDITIONAL_IMAGE_4"
        addAdditionalViewForProductMap._uploadedFile_fileName = parameters._additionalImageFour_fileName
        addAdditionalViewForProductMap._uploadedFile_contentType = parameters._additionalImageFour_contentType
        run service: "addAdditionalViewForProduct", with: addAdditionalViewForProductMap
    }
    result.productId = parameters.productId
    return result
}

/**
 * Update Product SEO
 * @return
 */
def updateContentSEOForProduct() {
    GenericValue productContent
    if (parameters.title) {
        productContent = from("ProductContentAndInfo")
                .where(productId: parameters.productId,
                        productContentTypeId: "PAGE_TITLE")
                .queryFirst()
        if (productContent) {
            GenericValue electronicText = from("ElectronicText")
                    .where(dataResourceId: productContent.dataResourceId)
                    .queryOne()
            if (electronicText) {
                electronicText.textData = parameters.title
                electronicText.store()
            }
        } else {
            Map createTextContentMap = [productId: parameters.productId,
                                        productContentTypeId: "PAGE_TITLE",
                                        text: parameters.title]
            run service: "createSimpleTextContentForProduct", with: createTextContentMap
        }
    }
    if (parameters.metaKeyword) {
        productContent = from("ProductContentAndInfo")
                .where(productId: parameters.productId,
                        productContentTypeId: "META_KEYWORD")
                .queryFirst()
        if (productContent) {
            GenericValue electronicText = from("ElectronicText")
                    .where(dataResourceId: productContent.dataResourceId)
                    .queryOne()
            if (electronicText) {
                electronicText.textData = parameters.metaKeyword
                electronicText.store()
            }
        } else {
            Map createTextContentMap = [productId: parameters.productId,
                                        productContentTypeId: "META_KEYWORD",
                                        text: parameters.metaKeyword]
            run service: "createSimpleTextContentForProduct", with: createTextContentMap
        }
    }
    if (parameters.metaDescription) {
        productContent = from("ProductContentAndInfo")
                .where(productId: parameters.productId,
                        productContentTypeId: "META_DESCRIPTION")
                .queryFirst()
        if (productContent) {
            GenericValue electronicText = from("ElectronicText")
                    .where(dataResourceId: productContent.dataResourceId)
                    .queryOne()
            if (electronicText) {
                electronicText.textData = parameters.metaDescription
                electronicText.store()
            }
        } else {
            Map createTextContentMap = [productId: parameters.productId,
                                        productContentTypeId: "META_DESCRIPTION",
                                        text: parameters.metaDescription]
            run service: "createSimpleTextContentForProduct", with: createTextContentMap
        }
    }
    return success()
}
