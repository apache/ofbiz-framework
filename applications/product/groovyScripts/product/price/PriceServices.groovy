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

import java.math.RoundingMode
import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.party.party.PartyHelper

/**
 * Create a Product Price
 * @return
 */
def createProductPrice() {
    Map result = success()
    if (!security.hasPermission("CATALOG_PRICE_MAINT", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", locale))
    }
    inlineHandlePriceWithTaxIncluded()

    GenericValue newEntity = makeValue("ProductPrice", parameters)

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (!newEntity.fromDate) {
        newEntity.fromDate = nowTimestamp
    }
    result.fromDate = newEntity.fromDate
    newEntity.lastModifiedDate = nowTimestamp
    newEntity.createdDate = nowTimestamp
    newEntity.lastModifiedByUserLogin = userLogin.userLoginId
    newEntity.createdByUserLogin = userLogin.userLoginId
    newEntity.create()

    return result
}

/**
 * Update an ProductPrice
 * @return
 */
def updateProductPrice() {
    Map result = success()
    if (!security.hasPermission("CATALOG_PRICE_MAINT", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", locale))
    }
    inlineHandlePriceWithTaxIncluded()

    GenericValue lookedUpValue = from("ProductPrice").where(parameters).queryOne()
    // grab the old price value before setting nonpk parameter fields
    result.oldPrice = lookedUpValue.price
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.lastModifiedDate = UtilDateTime.nowTimestamp()
    lookedUpValue.lastModifiedByUserLogin = userLogin.userLoginId
    lookedUpValue.store()

    return result
}

/**
 * Delete an ProductPrice
 * @return
 */
def deleteProductPrice() {
    Map result = success()
    if (!security.hasPermission("CATALOG_PRICE_MAINT", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", locale))
    }
    GenericValue lookedUpValue = from("ProductPrice").where(parameters).queryOne()
    // grab the old price value before setting nonpk parameter fields
    result.oldPrice = lookedUpValue.price
    lookedUpValue.remove()
    return result
}

/**
 * Inline Handle Price with Tax Included
 * @return
 */
def inlineHandlePriceWithTaxIncluded() {
    // handle price with tax included related fields (priceWithTax, taxAmount, taxPercentage, taxAuthPartyId, taxAuthGeoId)
    if (parameters.taxAuthPartyId && parameters.taxAuthGeoId) {
        parameters.priceWithTax = parameters.price

        // if taxPercentage not passed in look it up based on taxAuthGeoId and taxAuthPartyId
        if (!parameters.taxPercentage) {
            // we only have basic data to constrain by here, so assume that if it is a VAT tax setup it should be pretty simple
            EntityCondition condition = EntityCondition.makeCondition([
                EntityCondition.makeCondition("taxAuthGeoId", parameters.taxAuthGeoId),
                EntityCondition.makeCondition("taxAuthPartyId", parameters.taxAuthPartyId),
                EntityCondition.makeCondition("taxAuthorityRateTypeId", EntityOperator.IN, ["SALES_TAX", "VAT_TAX"])
            ])
            GenericValue taxAuthorityRateProduct = from("TaxAuthorityRateProduct").where(condition).filterByDate().queryFirst()
            parameters.taxPercentage = taxAuthorityRateProduct?.taxPercentage
        }
        if (!parameters.taxPercentage) {
            return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceTaxPercentageNotFound", locale))
        }
        // in short the formula is: taxAmount = priceWithTax - (priceWithTax/(1+taxPercentage/100))
        BigDecimal taxAmount = parameters.priceWithTax - (parameters.priceWithTax / (1 + parameters.taxPercentage/100))
        parameters.taxAmount = taxAmount.setScale(3, RoundingMode.HALF_UP)

        BigDecimal priceWithoutTax = parameters.priceWithTax - parameters.taxAmount
        parameters.priceWithoutTax = priceWithoutTax.setScale(3, RoundingMode.HALF_UP)

        parameters.price = parameters.taxInPrice == "Y" ?
                parameters.priceWithTax: // the price passed in has tax included, and we want to store it with tax included
                parameters.priceWithoutTax // the price passed in has tax included, but we want to store it without tax included
    }
    return success()
}

// ProductPriceCond methods

/**
 * Create an ProductPriceCond
 * @return
 */
def createProductPriceCond() {
    Map result = success()
    if (!security.hasEntityPermission("CATALOG", "_CREATE", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogCreatePermissionError", locale))
    }
    if (!security.hasPermission("CATALOG_PRICE_MAINT", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", locale))
    }
    if (parameters.condValueInput) {
        parameters.condValue = parameters.condValueInput
    }
    GenericValue newEntity = makeValue("ProductPriceCond", parameters)
    delegator.setNextSubSeqId(newEntity, "productPriceCondSeqId", 2, 1)
    result.productPriceCondSeqId = newEntity.productPriceCondSeqId
    newEntity.create()
    return result
}

/**
 * Update an ProductPriceCond
 * @return
 */
def updateProductPriceCond() {
    if (!security.hasEntityPermission("CATALOG", "_UPDATE", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductCatalogUpdatePermissionError", locale))
    }
    if (!security.hasPermission("CATALOG_PRICE_MAINT", userLogin)) {
        return error(UtilProperties.getMessage("ProductUiLabels", "ProductPriceMaintPermissionError", locale))
    }
    if (["PRIP_QUANTITY", "PRIP_LIST_PRICE"].contains(parameters.inputParamEnumId)) {
        parameters.condValue = parameters.condValueInput
    }
    GenericValue lookedUpValue = from("ProductPriceCond").where(parameters).queryOne()
    lookedUpValue.setNonPKFields(parameters)
    lookedUpValue.store()
    return success()
}

/**
 * Set the Value options for selected Price Rule Condition Input
 * @return
 */
def getAssociatedPriceRulesConds() {
    Map result = success()
    List productPriceRulesCondValues = []

    // May prove more useful rather than an entity-and in custom cases, set limit to not return too huge element
    int sizeLimit = 200
    switch (parameters.inputParamEnumId) {
        case "PRIP_PRODUCT_ID":
            from("Product").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.productId,
                                                description: it.internalName ?: '[' + it.productId + ']']
            }
            break

        case "PRIP_PROD_CAT_ID":
            from("ProductCategory").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.productCategoryId,
                                                description: it.categoryName ?: '[' + it.productCategoryId + ']']
            }
            break

        case "PRIP_PROD_FEAT_ID":
            from("ProductFeatureType").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.productFeatureTypeId,
                                                description: it.description ?: '[' + it.productFeatureTypeId + ']']
            }
            break

        case "PRIP_PARTY_ID":
        case "PRIP_PARTY_GRP_MEM":
            from("PartyNameView").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.partyId,
                                                description: PartyHelper.getPartyName(it)]
            }
            break

        case "PRIP_PARTY_CLASS":
            from("PartyClassificationGroup").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.partyClassificationGroupId,
                                                description: it.description ?: '[' + it.partyClassificationGroupId + ']']
            }
            break

        case "PRIP_ROLE_TYPE":
            from("RoleType").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.roleTypeId,
                                                description: it.description ?: '[' + it.roleTypeId + ']']
            }
            break

        case "PRIP_WEBSITE_ID":
            from("WebSite").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.webSiteId,
                                                description: it.siteName ?: '[' + it.webSiteId + ']']
            }
            break

        case "PRIP_PROD_SGRP_ID":
            from("ProductStoreGroup").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.productStoreGroupId,
                                                description: it.productStoreGroupName ?: '[' + it.productStoreGroupId + ']']
            }
            break

        case "PRIP_PROD_CLG_ID":
            from("ProdCatalog").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.prodCatalogId,
                                                description: it.catalogName ?: '[' + it.prodCatalogId + ']']
            }
            break

        case "PRIP_CURRENCY_UOMID":
            from("Uom").limit(sizeLimit).queryList()?.each {
                productPriceRulesCondValues << [key: it.uomId,
                                                description: it.abbreviation ?: '[' + it.uomId + ']']
            }
            break

        default:
            return success()
            break
    }

    result.productPriceRulesCondValues = []
    if (!productPriceRulesCondValues) {
        result.productPriceRulesCondValues << UtilProperties.getMessage("CommonUiLabels", "CommonNoOptions", locale)
    } else {
        productPriceRulesCondValues.each {
            result.productPriceRulesCondValues << it.description + ": " + it.key
        }
    }
    return result
}
