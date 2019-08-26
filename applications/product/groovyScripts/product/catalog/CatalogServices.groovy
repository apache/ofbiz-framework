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
import org.apache.ofbiz.base.util.UtilURL
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.service.ServiceUtil

/**
 * get All categories
 */
def getAllCategories() {
    String defaultTopCategoryId = parameters.topCategory ?: EntityUtilProperties.getPropertyValue("catalog", "top.category.default", delegator)
    Map serviceRes = run service: "getRelatedCategories", with: [parentProductCategoryId: defaultTopCategoryId]
    return serviceRes
}

def getRelatedCategories() {
    Map result = success()
    List categories = []
    List rollups = from("ProductCategoryRollup")
            .where("parentProductCategoryId", parameters.parentProductCategoryId)
            .orderBy("sequenceNum")
            .queryList()
    if (parameters.categories) {
        categories.addAll(parameters.categories)
    }
    if (rollups) {
        List subCategories = []
        for (GenericValue parent: rollups) {
            subCategories << parent.getRelatedOne("CurrentProductCategory", true)
        }
        if (subCategories) {
            Set distinctCategories = categories? [*categories] as Set: [] as Set
            Map relatedCategoryContext = [categories: subCategories]
            for (Map subCategory: subCategories) {
                relatedCategoryContext.parentProductCategoryId = subCategory.productCategoryId
                Map res = run service: "getRelatedCategories", with: relatedCategoryContext
                if (res.categories) {
                    distinctCategories.addAll(res.categories)
                }
            }
            categories = distinctCategories as List
        }
    } else {
        GenericValue productCategory = from("ProductCategory")
                .where("productCategoryId", parameters.parentProductCategoryId)
                .queryOne()
        categories << productCategory
    }
    result.categories = categories
    return result
}


/**
 * Check if image url exists or not for all categories
 */
def checkImageUrlForAllCategories() {
    Map result = success()
    Map gacRes = run service: "getAllCategories", with: parameters
    List categories = gacRes.categories
    if (categories) {
        Map categoriesMap = [:]
        // get the category from categories
        for (Map category : categories) {
            Map ciufcapRes = run service: "checkImageUrlForCategoryAndProduct", with: [categoryId: category.productCategoryId]
            Map fileStatusMap = [fileExists: ciufcapRes.fileExists, fileNotExists: ciufcapRes.fileNotExists]
            String categoryId = category.productCategoryId
            categoriesMap[categoryId] = fileStatusMap
        }
        result.categoriesMap = categoriesMap
    }
    return result
}


/**
 * Check if image url exists or not for category and product
 */
def checkImageUrlForCategoryAndProduct() {
    Map result = success()
    Map gpcmRes = run service: "getProductCategoryMembers", with: parameters
    List categoryMembers = gpcmRes.categoryMembers
    Map category = gpcmRes.category

    List fileExists = []
    List fileNotExists = []
    Map filesImageMap = [:]
    // Get category images and check it exists or not
    if (category) {
        Map ciufcRes = run service: "checkImageUrlForCategory", with: [categoryId: category.productCategoryId]
        filesImageMap = ciufcRes.filesImageMap

        if (filesImageMap) {
            fileImageExists("categoryImageUrlMap", "categoryImageUrl", filesImageMap, fileExists, fileNotExists)
            fileImageExists("linkOneImageUrlMap", "linkOneImageUrl", filesImageMap, fileExists, fileNotExists)
            fileImageExists("linkTwoImageUrlMap", "linkTwoImageUrl", filesImageMap, fileExists, fileNotExists)
        }
    }

    if (categoryMembers) {
        for (GenericValue productCategoryMember : categoryMembers) {
            Map ciufpRes = run service: "checkImageUrlForProduct", with: [productId: productCategoryMember.productId]
            filesImageMap = ciufpRes.filesImageMap

            if (filesImageMap) {
                fileImageExists("smallImageUrlMap", "smallImageUrl", filesImageMap, fileExists, fileNotExists)
                fileImageExists("mediumImageUrlMap", "mediumImageUrl", filesImageMap, fileExists, fileNotExists)
                fileImageExists("largeImageUrlMap", "largeImageUrl", filesImageMap, fileExists, fileNotExists)
                fileImageExists("detailImageUrlMap", "detailImageUrl", filesImageMap, fileExists, fileNotExists)
            }

            /* Case for virtual product
               get related assoc product */
            Map product = productCategoryMember.getRelatedOne("Product", false)
            if ("Y" == product.isVirtual) {
                Map virtualProductContext = [
                    productId: product.productId,
                    productAssocTypeId: "PRODUCT_VARIANT"
                ]
                List variantProducts = from("ProductAssoc")
                        .where(virtualProductContext)
                        .filterByDate()
                        .queryList()
                if (variantProducts) {
                    for (Map variantProduct : variantProducts) {
                        Map res = run service: "checkImageUrlForProduct", with: [productId: variantProduct.productIdTo]
                        filesImageMap = res.filesImageMap
                        if (filesImageMap) {
                            fileImageExists("smallImageUrlMap", "smallImageUrl", filesImageMap, fileExists, fileNotExists)
                            fileImageExists("mediumImageUrlMap", "mediumImageUrl", filesImageMap, fileExists, fileNotExists)
                            fileImageExists("largeImageUrlMap", "largeImageUrl", filesImageMap, fileExists, fileNotExists)
                            fileImageExists("detailImageUrlMap", "detailImageUrl", filesImageMap, fileExists, fileNotExists)
                        }
                    }
                }
            }
        }
        result.fileExists = fileExists
        result.fileNotExists = fileNotExists
    }
    return result
}

/**
 * This Method checks if the images exist and puts them into
 * the lists filesExist and filesNotExist.
 * <p>
 * The lists filesExist and filesNotExist are filled with the new values.
 * @param map
 * @param key
 * @param filesImageMap
 * @param fileExists
 * @param fileNotExists
 */
def fileImageExists(map, key, filesImageMap, fileExists, fileNotExists) {
    if (filesImageMap."${map}"?."${key}") {
        if ("Y" == filesImageMap."${map}".isExists) {
            fileExists.add(filesImageMap."${map}"."${key}")
        } else {
            fileNotExists.add(filesImageMap."${map}"."${key}")
        }
    }
}

/**
 * This service gets the category id and checks if
 * all the images of category exists or not
 */
def checkImageUrlForCategory() {
    Map result = success()
    Map filesImageMap = [:]
    GenericValue category = from("ProductCategory")
            .where("productCategoryId", parameters.categoryId)
            .queryOne()
    if (category) {
        // check for category image url
        imageUrlCheck(category, "categoryImageUrl", filesImageMap)
        // check for link image url
        imageUrlCheck(category, "linkOneImageUrl", filesImageMap)
        // check for link two image url
        imageUrlCheck(category, "linkTwoImageUrl", filesImageMap)
        category.store()
        if (filesImageMap) {
            result.filesImageMap = filesImageMap
        }
    }
    return result
}

/**
 * Check if image url exists or not for product
 */
def checkImageUrlForProduct() {
    Map result = success()
    Map filesImageMap = [:]
    if (parameters.productId) {
        Map product = from("Product").where(parameters).queryOne()
        // check for small image url
        imageUrlCheck(product, "smallImageUrl", filesImageMap)
        // check for medium image url
        imageUrlCheck(product, "mediumImageUrl", filesImageMap)
        // check for large image url
        imageUrlCheck(product, "largeImageUrl", filesImageMap)
        // check for detail image url
        imageUrlCheck(product, "detailImageUrl", filesImageMap)
        if (filesImageMap) {
            result.filesImageMap = filesImageMap
        }
        product.store()
    }
    return result
}

/**
 * This method fills filesImageMap with the needed content and sets the relevant type in
 * the prodOrCat (product or category) map to null, if there is no image existent for this type
 * @param prodOrCat
 * @param filesImageMap
 * @param imageType
 * @return
 */
def imageUrlCheck(prodOrCat, imageType, filesImageMap) {
    if (prodOrCat."${imageType}") {
        Map res = run service: "checkImageUrl", with: [imageUrl: prodOrCat."${imageType}"]
        String isExists = res.isExists
        filesImageMap."${imageType}Map" = [:]
        filesImageMap."${imageType}Map"."${imageType}" = prodOrCat."${imageType}"
        filesImageMap."${imageType}Map".isExists = isExists
        if (isExists == "N") {
            prodOrCat."${imageType}" = null
        }
    }
}

/**
 * Check if image url exists or not
 */
def checkImageUrl() {
    def imageUrl = parameters.imageUrl
    boolean httpFlag = imageUrl.startsWith("http")
    boolean ftpFlag = imageUrl.startsWith("ftp")
    String url = httpFlag || ftpFlag
        ? UtilURL.fromUrlString(imageUrl)
        : UtilURL.fromOfbizHomePath("/themes/common-theme/webapp" + imageUrl)
    Map result = success()
    result.isExists = url ? "Y" : "N"
    return result
}

/**
 * Catalog permission logic
 */
def catalogPermissionCheck() {
    parameters.primaryPermission = "CATALOG"
    Map result = run service: "genericBasePermissionCheck", with: parameters
    return result
}

/**
 * ProdCatalogToParty permission logic
 */
def prodCatalogToPartyPermissionCheck() {
    parameters.altPermission = "PARTYMGR"
    return catalogPermissionCheck()
}

/**
 * create missing category and product alternative urls
 */
def createMissingCategoryAndProductAltUrls() {
    Timestamp now = UtilDateTime.nowTimestamp()
    Map result = success()
    result.prodCatalogId = parameters.prodCatalogId
    int categoriesNotUpdated = 0
    int categoriesUpdated = 0
    int productsNotUpdated = 0
    int productsUpdated = 0
    String checkProduct = parameters.product
    String checkCategory = parameters.category
    List prodCatalogCategoryList = from("ProdCatalogCategory").where("prodCatalogId", parameters.prodCatalogId).queryList()

    // get all categories
    List productCategories = []
    for (GenericValue prodCatalog: prodCatalogCategoryList) {
        productCategories.addAll(createMissingCategoryAltUrlInline(prodCatalog.productCategoryId))
    }

    for (Map productCategoryList : productCategories) {
        // create product category alternative URLs
        if (checkCategory) {
            List productCategoryContentAndInfoList = from("ProductCategoryContentAndInfo")
                    .where("productCategoryId", productCategoryList.productCategoryId,
                            "prodCatContentTypeId", "ALTERNATIVE_URL")
                    .filterByDate()
                    .orderBy("-fromDate")
                    .cache()
                    .queryList()
            if (!productCategoryContentAndInfoList) {
                Map createSimpleTextContentForCategoryCtx = [
                    fromDate: now,
                    prodCatContentTypeId: "ALTERNATIVE_URL",
                    localeString: "en",
                    productCategoryId: productCategoryList.productCategoryId
                ]
                if (!productCategoryList.categoryName) {
                    GenericValue productCategoryContent = from("ProductCategoryContentAndInfo")
                            .where("productCategoryId", productCategoryList.productCategoryId,
                                    "prodCatContentTypeId", "CATEGORY_NAME")
                            .filterByDate()
                            .orderBy("-fromDate")
                            .cache()
                            .queryFirst()
                    if (productCategoryContent) {
                        Map gcadrRes = run service: "getContentAndDataResource", with: [contentId: productCategoryContent.contentId]
                        Map resultMap = gcadrRes.resultData
                        createSimpleTextContentForCategoryCtx.text = resultMap.electronicText.textData
                    }
                } else {
                    createSimpleTextContentForCategoryCtx.text = productCategoryList.categoryName
                }
                if (createSimpleTextContentForCategoryCtx.text) {
                    Map res = run service: "createSimpleTextContentForCategory", with: createSimpleTextContentForCategoryCtx
                    if (ServiceUtil.isError(res)) {
                        return res
                    }
                    categoriesUpdated += 1
                }
            } else {
                categoriesNotUpdated += 1
            }
        }

        // create product alternative URLs
        if (checkProduct) {
            List productCategoryMemberList = from("ProductCategoryMember")
                    .where("productCategoryId", productCategoryList.productCategoryId)
                    .filterByDate()
                    .orderBy("-fromDate")
                    .cache()
                    .queryList()
            for (Map productCategoryMember: productCategoryMemberList) {
                String memberProductId = productCategoryMember.productId
                List productContentAndInfoList = from("ProductContentAndInfo")
                        .where("productId", memberProductId, "productContentTypeId", "ALTERNATIVE_URL")
                        .cache().orderBy("-fromDate")
                        .filterByDate()
                        .queryList()
                if (!productContentAndInfoList) {
                    GenericValue product = from("Product").where("productId", memberProductId).queryOne()
                    Map createSimpleTextContentForProductCtx = [
                        fromDate: now,
                        productContentTypeId: "ALTERNATIVE_URL",
                        localeString: "en",
                        productId: memberProductId
                    ]
                    createSimpleTextContentForProductCtx.text = product.internalName ?: product.productName
                    if (createSimpleTextContentForProductCtx.text) {
                        Map res = run service: "createSimpleTextContentForProduct", with: createSimpleTextContentForProductCtx
                        if (ServiceUtil.isError(res)) {
                            return res
                        }
                        productsUpdated += 1
                    }
                } else {
                    productsNotUpdated += 1
                }
            }
        }
    }
    result.successMessageList = [
        "Categories updated: ${categoriesUpdated}",
        "Products updated: ${productsUpdated}"
    ]
    result.categoriesNotUpdated = categoriesNotUpdated
    result.productsNotUpdated = productsNotUpdated
    result.categoriesUpdated = categoriesUpdated
    result.productsUpdated = productsUpdated
    return result
}

def createMissingCategoryAltUrlInline(parentProductCategoryId) {
    List productCategoryRollupIds = from("ProductCategoryRollup")
            .where("parentProductCategoryId", parentProductCategoryId)
            .filterByDate()
            .cache()
            .getFieldList("productCategoryId")
    List productCategories = []
    for (String productCategoryId : productCategoryRollupIds) {

        // append product category to list
        productCategories << from("ProductCategory")
                .where("productCategoryId", productCategoryId)
                .cache()
                .queryOne()

        // find rollup product categories
        productCategories.addAll(createMissingCategoryAltUrlInline(productCategoryId))
    }
    return productCategories
}