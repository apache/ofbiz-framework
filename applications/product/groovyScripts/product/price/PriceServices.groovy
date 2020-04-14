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
import org.apache.ofbiz.product.product.ProductServices
import org.apache.ofbiz.service.ServiceUtil




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
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition("taxAuthorityRateTypeId", "SALES_TAX"),
                    EntityCondition.makeCondition("taxAuthorityRateTypeId", "VAT_TAX")
                ], EntityOperator.OR)
            ])
            GenericValue taxAuthorityRateProduct = from("TaxAuthorityRateProduct").where(condition).filterByDate().queryFirst()
            parameters.taxPercentage = taxAuthorityRateProduct?.taxPercentage
        }
        if (!parameters.taxPercentage) {
            String errorMessage = UtilProperties.getMessage("ProductUiLabels", "ProductPriceTaxPercentageNotFound", locale)
            logError(errorMessage)
            return error(errorMessage)
        }
        // in short the formula is: taxAmount = priceWithTax - (priceWithTax/(1+taxPercentage/100))
        BigDecimal taxAmount = parameters.priceWithTax - (parameters.priceWithTax/(1 + parameters.taxPercentage/100))
        parameters.taxAmount = taxAmount.setScale(3, RoundingMode.HALF_UP)

        BigDecimal priceWithoutTax = parameters.priceWithTax - parameters.taxAmount
        parameters.priceWithoutTax = priceWithoutTax.setScale(3, RoundingMode.HALF_UP)

        if (parameters.taxInPrice == "Y") {
            // the price passed in has tax included, and we want to store it with tax included
            parameters.price = parameters.priceWithTax
        } else {
            // the price passed in has tax included, but we want to store it without tax included
            parameters.price = parameters.priceWithoutTax
        }

    }
    return success()
}

// TODO NMA convert to entity auto when changed fileds are managed

/**
 * Save History of ProductPrice Change
 * @return
 */
def saveProductPriceChange() {
    // Note that this is kept pretty simple: if a price is specific but no oldPrice, then it is generally a create,
    // if both are specified it is generally an update, if only the oldPrice is specified it is generally a delete
    GenericValue newEntity = makeValue("ProductPriceChange")
    newEntity.setNonPKFields(parameters)
    newEntity.productPriceChangeId = delegator.getNextSeqId("ProductPriceChange")
    newEntity.changedDate = UtilDateTime.nowTimestamp()
    newEntity.changedByUserLogin = userLogin.userLoginId
    newEntity.create()
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
    if (parameters.inputParamEnumId == "PRIP_QUANTITY") {
        parameters.condValue = parameters.condValueInput
    }
    if (parameters.inputParamEnumId == "PRIP_LIST_PRICE") {
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
    if ((parameters.inputParamEnumId == "PRIP_QUANTITY") || (parameters.inputParamEnumId == "PRIP_LIST_PRICE")) {
        return success()
    }
    if (parameters.inputParamEnumId == "PRIP_PRODUCT_ID") {
        List condValues = from("Product").queryList()
        // May prove more useful rather than an entity-and in custom cases
        for (GenericValue condValue : condValues) {
            String option = (condValue.internalName ? "${condValue.internalName}: " : ": ") + (condValue.productId ? "${condValue.productId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_PROD_CAT_ID") {
        List condValues = from("ProductCategory").queryList()
        // May prove more useful rather than an entity-and in custom cases
        for (GenericValue condValue : condValues) {
            String option = (condValue.categoryName ? "${condValue.categoryName} " : " ") + (condValue.description ? "${condValue.description} " : " ") +
                    (condValue.longDescription ? condValue.longDescription.substring(0,10) : "") + (condValue.productCategoryId ? " [${condValue.productCategoryId}]: " : " []: ") +
                    (condValue.productCategoryId ? "${condValue.productCategoryId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_PROD_FEAT_ID") {
        List condValues = from("ProductFeatureType").queryList()
        // May prove more useful rather than an entity-and in custom cases
        for (GenericValue condValue : condValues) {
            String option = (condValue.description ? "${condValue.description} " : " ") + (condValue.productFeatureTypeId ? " ${condValue.productFeatureTypeId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if ((parameters.inputParamEnumId == "PRIP_PARTY_ID") || (parameters.inputParamEnumId == "PRIP_PARTY_GRP_MEM")) {
        List condValues = from("PartyNameView").queryList()
        for (GenericValue condValue : condValues) {
            String option = (condValue.firstName ? "${condValue.firstName} " : " ") + (condValue.lastName ? "${condValue.lastName}" : "") +
                    (condValue.groupName ? "${condValue.groupName}: " : ": ") + (condValue.partyId ? "${condValue.partyId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_PARTY_CLASS") {
        List condValues = from("PartyClassificationGroup").queryList()
        // May prove more useful rather than an entity-and in custom cases
        for (GenericValue condValue : condValues) {
            String option = (condValue.description ? "${condValue.description}: " : ": ") + (condValue.partyClassificationGroupId ? "${condValue.partyClassificationGroupId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_ROLE_TYPE") {
        List condValues = from("RoleType").queryList()
        // May prove more useful rather than an entity-and in custom cases
        for (GenericValue condValue : condValues) {
            String option = (condValue.description ? "${condValue.description}: " : ": ") + (condValue.roleTypeId ? "${condValue.roleTypeId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_WEBSITE_ID") {
        List condValues = from("WebSite").queryList()
        for (GenericValue condValue : condValues) {
            String option = (condValue.siteName ? "${condValue.siteName}: " : ": ") + (condValue.webSiteId ? "${condValue.webSiteId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_PROD_SGRP_ID") {
        List condValues = from("ProductStoreGroup").queryList()
        for (GenericValue condValue : condValues) {
            String option = (condValue.productStoreGroupName ? "${condValue.productStoreGroupName} " : " ") + (condValue.description ? "(${condValue.description}): " : "(): ") + (condValue.productStoreGroupId ? "${condValue.productStoreGroupId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_PROD_CLG_ID") {
        List condValues = from("ProdCatalog").queryList()
        for (GenericValue condValue : condValues) {
            String option = (condValue.catalogName ? "${condValue.catalogName}: " : ": ") + (condValue.prodCatalogId ? "${condValue.prodCatalogId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (parameters.inputParamEnumId == "PRIP_CURRENCY_UOMID") {
        List condValues = from("Uom").where(uomTypeId: "CURRENCY_MEASURE").queryList()
        for (GenericValue condValue : condValues) {
            String option = (condValue.description ? "${condValue.description}: " : ": ") + (condValue.uomId ? "${condValue.uomId}" : "")
            productPriceRulesCondValues << option
        }
    }
    if (!productPriceRulesCondValues) {
        String noOptions = UtilProperties.getMessage("CommonUiLabels", "CommonNoOptions", locale)
        productPriceRulesCondValues << noOptions
    }
    result.productPriceRulesCondValues = productPriceRulesCondValues
    return result
}
