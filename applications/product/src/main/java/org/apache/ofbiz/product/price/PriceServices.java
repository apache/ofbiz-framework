/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.product.price;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * PriceServices - Workers and Services class for product price related functionality
 */
public class PriceServices {

    public static final String module = PriceServices.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final BigDecimal ONE_BASE = BigDecimal.ONE;
    public static final BigDecimal PERCENT_SCALE = new BigDecimal("100.000");

    public static final int taxCalcScale = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final int taxFinalScale = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    public static final int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");

    /**
     * <p>Calculates the price of a product from pricing rules given the following input, and of course access to the database:</p>
     * <ul>
     *   <li>productId
     *   <li>partyId
     *   <li>prodCatalogId
     *   <li>webSiteId
     *   <li>productStoreId
     *   <li>productStoreGroupId
     *   <li>agreementId
     *   <li>quantity
     *   <li>currencyUomId
     *   <li>checkIncludeVat
     * </ul>
     */
    public static Map<String, Object> calculateProductPrice(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        GenericValue product = (GenericValue) context.get("product");
        String productId = product.getString("productId");
        String prodCatalogId = (String) context.get("prodCatalogId");
        String webSiteId = (String) context.get("webSiteId");
        String checkIncludeVat = (String) context.get("checkIncludeVat");
        String surveyResponseId = (String) context.get("surveyResponseId");
        Map<String, Object> customAttributes = UtilGenerics.checkMap(context.get("customAttributes"));

        String findAllQuantityPricesStr = (String) context.get("findAllQuantityPrices");
        boolean findAllQuantityPrices = "Y".equals(findAllQuantityPricesStr);
        boolean optimizeForLargeRuleSet = "Y".equals(context.get("optimizeForLargeRuleSet"));

        String agreementId = (String) context.get("agreementId");

        String productStoreId = (String) context.get("productStoreId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        Locale locale = (Locale) context.get("locale");
        
        GenericValue productStore = null;
        try {
            // we have a productStoreId, if the corresponding ProductStore.primaryStoreGroupId is not empty, use that
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting product store info from the database while calculating price" + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductPriceCannotRetrieveProductStore", UtilMisc.toMap("errorString", e.toString()) , locale));
        }
        if (UtilValidate.isEmpty(productStoreGroupId)) {
            if (productStore != null) {
                try {
                    if (UtilValidate.isNotEmpty(productStore.getString("primaryStoreGroupId"))) {
                        productStoreGroupId = productStore.getString("primaryStoreGroupId");
                    } else {
                        // no ProductStore.primaryStoreGroupId, try ProductStoreGroupMember
                        List<GenericValue> productStoreGroupMemberList = EntityQuery.use(delegator).from("ProductStoreGroupMember").where("productStoreId", productStoreId).orderBy("sequenceNum", "-fromDate").cache(true).queryList();
                        productStoreGroupMemberList = EntityUtil.filterByDate(productStoreGroupMemberList, true);
                        if (productStoreGroupMemberList.size() > 0) {
                            GenericValue productStoreGroupMember = EntityUtil.getFirst(productStoreGroupMemberList);
                            productStoreGroupId = productStoreGroupMember.getString("productStoreGroupId");
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error getting product store info from the database while calculating price" + e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "ProductPriceCannotRetrieveProductStore", UtilMisc.toMap("errorString", e.toString()) , locale));
                }
            }

            // still empty, default to _NA_
            if (UtilValidate.isEmpty(productStoreGroupId)) {
                productStoreGroupId = "_NA_";
            }
        }

        // if currencyUomId is null get from properties file, if nothing there assume USD (USD: American Dollar) for now
        String currencyDefaultUomId = (String) context.get("currencyUomId");
        String currencyUomIdTo = (String) context.get("currencyUomIdTo"); 
        if (UtilValidate.isEmpty(currencyDefaultUomId)) {
            if (productStore != null && UtilValidate.isNotEmpty(productStore.getString("defaultCurrencyUomId"))) {
                currencyDefaultUomId = productStore.getString("defaultCurrencyUomId");
            } else {
                currencyDefaultUomId = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
            }
        }

        // productPricePurposeId is null assume "PURCHASE", which is equivalent to what prices were before the purpose concept
        String productPricePurposeId = (String) context.get("productPricePurposeId");
        if (UtilValidate.isEmpty(productPricePurposeId)) {
            productPricePurposeId = "PURCHASE";
        }

        // termUomId, for things like recurring prices specifies the term (time/frequency measure for example) of the recurrence
        // if this is empty it will simply not be used to constrain the selection
        String termUomId = (String) context.get("termUomId");

        // if this product is variant, find the virtual product and apply checks to it as well
        String virtualProductId = null;
        if ("Y".equals(product.getString("isVariant"))) {
            try {
                virtualProductId = ProductWorker.getVariantVirtualId(product);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting virtual product id from the database while calculating price" + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductPriceCannotRetrieveVirtualProductId", UtilMisc.toMap("errorString", e.toString()) , locale));
            }
        }

        // get prices for virtual product if one is found; get all ProductPrice entities for this productId and currencyUomId
        List<GenericValue> virtualProductPrices = null;
        if (virtualProductId != null) {
            try {
                virtualProductPrices = EntityQuery.use(delegator).from("ProductPrice").where("productId", virtualProductId, "currencyUomId", currencyDefaultUomId, "productStoreGroupId", productStoreGroupId).orderBy("-fromDate").cache(true).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "An error occurred while getting the product prices", module);
            }
            virtualProductPrices = EntityUtil.filterByDate(virtualProductPrices, true);
        }

        // NOTE: partyId CAN be null
        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId) && context.get("userLogin") != null) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            partyId = userLogin.getString("partyId");
        }

        // check for auto-userlogin for price rules
        if (UtilValidate.isEmpty(partyId) && context.get("autoUserLogin") != null) {
            GenericValue userLogin = (GenericValue) context.get("autoUserLogin");
            partyId = userLogin.getString("partyId");
        }

        BigDecimal quantity = (BigDecimal) context.get("quantity");
        if (quantity == null) quantity = BigDecimal.ONE;

        BigDecimal amount = (BigDecimal) context.get("amount");

        List<EntityCondition> productPriceEcList = new LinkedList<EntityCondition>();
        productPriceEcList.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        // this funny statement is for backward compatibility purposes; the productPricePurposeId is a new pk field on the ProductPrice entity and in order databases may not be populated, until the pk is updated and such; this will ease the transition somewhat
        if ("PURCHASE".equals(productPricePurposeId)) {
            productPriceEcList.add(EntityCondition.makeCondition(
                    EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, productPricePurposeId),
                    EntityOperator.OR,
                    EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, null)));
        } else {
            productPriceEcList.add(EntityCondition.makeCondition("productPricePurposeId", EntityOperator.EQUALS, productPricePurposeId));
        }
        productPriceEcList.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, currencyDefaultUomId));
        productPriceEcList.add(EntityCondition.makeCondition("productStoreGroupId", EntityOperator.EQUALS, productStoreGroupId));
        if (UtilValidate.isNotEmpty(termUomId)) {
            productPriceEcList.add(EntityCondition.makeCondition("termUomId", EntityOperator.EQUALS, termUomId));
        }
        EntityCondition productPriceEc = EntityCondition.makeCondition(productPriceEcList, EntityOperator.AND);

        // for prices, get all ProductPrice entities for this productId and currencyUomId
        List<GenericValue> productPrices = null;
        try {
            productPrices = EntityQuery.use(delegator).from("ProductPrice").where(productPriceEc).orderBy("-fromDate").cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "An error occurred while getting the product prices", module);
        }
        productPrices = EntityUtil.filterByDate(productPrices, true);

        // ===== get the prices we need: list, default, average cost, promo, min, max =====
        // if any of these prices is missing and this product is a variant, default to the corresponding price on the virtual product
        GenericValue listPriceValue = getPriceValueForType("LIST_PRICE", productPrices, virtualProductPrices);
        GenericValue defaultPriceValue = getPriceValueForType("DEFAULT_PRICE", productPrices, virtualProductPrices);

        // If there is an agreement between the company and the client, and there is
        // a price for the product in it, it will override the default price of the
        // ProductPrice entity.
        if (UtilValidate.isNotEmpty(agreementId)) {
            try {
                GenericValue agreementPriceValue = EntityQuery.use(delegator).from("AgreementItemAndProductAppl").where("agreementId", agreementId, "productId", productId, "currencyUomId", currencyDefaultUomId).queryFirst();
                if (agreementPriceValue != null && agreementPriceValue.get("price") != null) {
                    defaultPriceValue = agreementPriceValue;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting agreement info from the database while calculating price" + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductPriceCannotRetrieveAgreementInfo", UtilMisc.toMap("errorString", e.toString()) , locale));
            }
        }

        GenericValue competitivePriceValue = getPriceValueForType("COMPETITIVE_PRICE", productPrices, virtualProductPrices);
        GenericValue averageCostValue = getPriceValueForType("AVERAGE_COST", productPrices, virtualProductPrices);
        GenericValue promoPriceValue = getPriceValueForType("PROMO_PRICE", productPrices, virtualProductPrices);
        GenericValue minimumPriceValue = getPriceValueForType("MINIMUM_PRICE", productPrices, virtualProductPrices);
        GenericValue maximumPriceValue = getPriceValueForType("MAXIMUM_PRICE", productPrices, virtualProductPrices);
        GenericValue wholesalePriceValue = getPriceValueForType("WHOLESALE_PRICE", productPrices, virtualProductPrices);
        GenericValue specialPromoPriceValue = getPriceValueForType("SPECIAL_PROMO_PRICE", productPrices, virtualProductPrices);

        // now if this is a virtual product check each price type, if doesn't exist get from variant with lowest DEFAULT_PRICE
        if ("Y".equals(product.getString("isVirtual"))) {
            // only do this if there is no default price, consider the others optional for performance reasons
            if (defaultPriceValue == null) {
                //use the cache to find the variant with the lowest default price
                try {
                    List<GenericValue> variantAssocList = EntityQuery.use(delegator).from("ProductAssoc").where("productId", product.get("productId"), "productAssocTypeId", "PRODUCT_VARIANT").orderBy("-fromDate").cache(true).filterByDate().queryList();
                    BigDecimal minDefaultPrice = null;
                    List<GenericValue> variantProductPrices = null;
                    for (GenericValue variantAssoc: variantAssocList) {
                        String curVariantProductId = variantAssoc.getString("productIdTo");
                        List<GenericValue> curVariantPriceList = EntityQuery.use(delegator).from("ProductPrice").where("productId", curVariantProductId).orderBy("-fromDate").cache(true).filterByDate(nowTimestamp).queryList();
                        List<GenericValue> tempDefaultPriceList = EntityUtil.filterByAnd(curVariantPriceList, UtilMisc.toMap("productPriceTypeId", "DEFAULT_PRICE"));
                        GenericValue curDefaultPriceValue = EntityUtil.getFirst(tempDefaultPriceList);
                        if (curDefaultPriceValue != null) {
                            BigDecimal curDefaultPrice = curDefaultPriceValue.getBigDecimal("price");
                            if (minDefaultPrice == null || curDefaultPrice.compareTo(minDefaultPrice) < 0) {
                                // check to see if the product is discontinued for sale before considering it the lowest price
                                GenericValue curVariantProduct = EntityQuery.use(delegator).from("Product").where("productId", curVariantProductId).cache().queryOne();
                                if (curVariantProduct != null) {
                                    Timestamp salesDiscontinuationDate = curVariantProduct.getTimestamp("salesDiscontinuationDate");
                                    if (salesDiscontinuationDate == null || salesDiscontinuationDate.after(nowTimestamp)) {
                                        minDefaultPrice = curDefaultPrice;
                                        variantProductPrices = curVariantPriceList;
                                    }
                                }
                            }
                        }
                    }

                    if (variantProductPrices != null) {
                        // we have some other options, give 'em a go...
                        if (listPriceValue == null) {
                            listPriceValue = getPriceValueForType("LIST_PRICE", variantProductPrices, null);
                        }
                        if (competitivePriceValue == null) {
                            competitivePriceValue = getPriceValueForType("COMPETITIVE_PRICE", variantProductPrices, null);
                        }
                        if (averageCostValue == null) {
                            averageCostValue = getPriceValueForType("AVERAGE_COST", variantProductPrices, null);
                        }
                        if (promoPriceValue == null) {
                            promoPriceValue = getPriceValueForType("PROMO_PRICE", variantProductPrices, null);
                        }
                        if (minimumPriceValue == null) {
                            minimumPriceValue = getPriceValueForType("MINIMUM_PRICE", variantProductPrices, null);
                        }
                        if (maximumPriceValue == null) {
                            maximumPriceValue = getPriceValueForType("MAXIMUM_PRICE", variantProductPrices, null);
                        }
                        if (wholesalePriceValue == null) {
                            wholesalePriceValue = getPriceValueForType("WHOLESALE_PRICE", variantProductPrices, null);
                        }
                        if (specialPromoPriceValue == null) {
                            specialPromoPriceValue = getPriceValueForType("SPECIAL_PROMO_PRICE", variantProductPrices, null);
                        }
                        defaultPriceValue = getPriceValueForType("DEFAULT_PRICE", variantProductPrices, null);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "An error occurred while getting the product prices", module);
                }
            }
        }

        BigDecimal promoPrice = BigDecimal.ZERO;
        if (promoPriceValue != null && promoPriceValue.get("price") != null) {
            promoPrice = promoPriceValue.getBigDecimal("price");
        }

        BigDecimal wholesalePrice = BigDecimal.ZERO;
        if (wholesalePriceValue != null && wholesalePriceValue.get("price") != null) {
            wholesalePrice = wholesalePriceValue.getBigDecimal("price");
        }

        boolean validPriceFound = false;
        BigDecimal defaultPrice = BigDecimal.ZERO;
        List<GenericValue> orderItemPriceInfos = new LinkedList<GenericValue>();
        if (defaultPriceValue != null) {
            // If a price calc formula (service) is specified, then use it to get the unit price
            if ("ProductPrice".equals(defaultPriceValue.getEntityName()) && UtilValidate.isNotEmpty(defaultPriceValue.getString("customPriceCalcService"))) {
                GenericValue customMethod = null;
                try {
                    customMethod = defaultPriceValue.getRelatedOne("CustomMethod", false);
                } catch (GenericEntityException gee) {
                    Debug.logError(gee, "An error occurred while getting the customPriceCalcService", module);
                }
                if (customMethod != null && UtilValidate.isNotEmpty(customMethod.getString("customMethodName"))) {
                    Map<String, Object> inMap = UtilMisc.toMap("userLogin", context.get("userLogin"), "product", product);
                    inMap.put("initialPrice", defaultPriceValue.getBigDecimal("price"));
                    inMap.put("currencyUomId", currencyDefaultUomId);
                    inMap.put("quantity", quantity);
                    inMap.put("amount", amount);
                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                        inMap.put("surveyResponseId", surveyResponseId);
                    }
                    if (UtilValidate.isNotEmpty(customAttributes)) {
                        inMap.put("customAttributes", customAttributes);
                    }
                    try {
                        Map<String, Object> outMap = dispatcher.runSync(customMethod.getString("customMethodName"), inMap);
                        if (ServiceUtil.isSuccess(outMap)) {
                            BigDecimal calculatedDefaultPrice = (BigDecimal)outMap.get("price");
                            orderItemPriceInfos = UtilGenerics.checkList(outMap.get("orderItemPriceInfos"));
                            if (UtilValidate.isNotEmpty(calculatedDefaultPrice)) {
                                defaultPrice = calculatedDefaultPrice;
                                validPriceFound = true;
                            }
                        }
                    } catch (GenericServiceException gse) {
                        Debug.logError(gse, "An error occurred while running the customPriceCalcService [" + customMethod.getString("customMethodName") + "]", module);
                    }
                }
            }
            if (!validPriceFound && defaultPriceValue.get("price") != null) {
                defaultPrice = defaultPriceValue.getBigDecimal("price");
                validPriceFound = true;
            }
        }

        BigDecimal listPrice = listPriceValue != null ? listPriceValue.getBigDecimal("price") : null;

        if (listPrice == null) {
            // no list price, use defaultPrice for the final price

            // ========= ensure calculated price is not below minSalePrice or above maxSalePrice =========
            BigDecimal maxSellPrice = maximumPriceValue != null ? maximumPriceValue.getBigDecimal("price") : null;
            if (maxSellPrice != null && defaultPrice.compareTo(maxSellPrice) > 0) {
                defaultPrice = maxSellPrice;
            }
            // min price second to override max price, safety net
            BigDecimal minSellPrice = minimumPriceValue != null ? minimumPriceValue.getBigDecimal("price") : null;
            if (minSellPrice != null && defaultPrice.compareTo(minSellPrice) < 0) {
                defaultPrice = minSellPrice;
                // since we have found a minimum price that has overriden a the defaultPrice, even if no valid one was found, we will consider it as if one had been...
                validPriceFound = true;
            }

            result.put("basePrice", defaultPrice);
            result.put("price", defaultPrice);
            result.put("defaultPrice", defaultPrice);
            result.put("competitivePrice", competitivePriceValue != null ? competitivePriceValue.getBigDecimal("price") : null);
            result.put("averageCost", averageCostValue != null ? averageCostValue.getBigDecimal("price") : null);
            result.put("promoPrice", promoPriceValue != null ? promoPriceValue.getBigDecimal("price") : null);
            result.put("specialPromoPrice", specialPromoPriceValue != null ? specialPromoPriceValue.getBigDecimal("price") : null);
            result.put("validPriceFound", Boolean.valueOf(validPriceFound));
            result.put("isSale", Boolean.FALSE);
            result.put("orderItemPriceInfos", orderItemPriceInfos);

            Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                    checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
            if (errorResult != null) return errorResult;
        } else {
            try {
                List<GenericValue> allProductPriceRules = makeProducePriceRuleList(delegator, optimizeForLargeRuleSet, productId, virtualProductId, prodCatalogId, productStoreGroupId, webSiteId, partyId, currencyDefaultUomId);
                allProductPriceRules = EntityUtil.filterByDate(allProductPriceRules, true);

                List<GenericValue> quantityProductPriceRules = null;
                List<GenericValue> nonQuantityProductPriceRules = null;
                if (findAllQuantityPrices) {
                    // split into list with quantity conditions and list without, then iterate through each quantity cond one
                    quantityProductPriceRules = new LinkedList<GenericValue>();
                    nonQuantityProductPriceRules = new LinkedList<GenericValue>();
                    for (GenericValue productPriceRule: allProductPriceRules) {
                        List<GenericValue> productPriceCondList = EntityQuery.use(delegator).from("ProductPriceCond").where("productPriceRuleId", productPriceRule.get("productPriceRuleId")).cache(true).queryList();

                        boolean foundQuantityInputParam = false;
                        // only consider a rule if all conditions except the quantity condition are true
                        boolean allExceptQuantTrue = true;
                        for (GenericValue productPriceCond: productPriceCondList) {
                            if ("PRIP_QUANTITY".equals(productPriceCond.getString("inputParamEnumId"))) {
                                foundQuantityInputParam = true;
                            } else {
                                if (!checkPriceCondition(productPriceCond, productId, virtualProductId, prodCatalogId, productStoreGroupId, webSiteId, partyId, quantity, listPrice, currencyDefaultUomId, delegator, nowTimestamp)) {
                                    allExceptQuantTrue = false;
                                }
                            }
                        }

                        if (foundQuantityInputParam && allExceptQuantTrue) {
                            quantityProductPriceRules.add(productPriceRule);
                        } else {
                            nonQuantityProductPriceRules.add(productPriceRule);
                        }
                    }
                }

                if (findAllQuantityPrices) {
                    List<Map<String, Object>> allQuantityPrices = new LinkedList<Map<String,Object>>();

                    // if findAllQuantityPrices then iterate through quantityProductPriceRules
                    // foreach create an entry in the out list and eval that rule and all nonQuantityProductPriceRules rather than a single rule
                    for (GenericValue quantityProductPriceRule: quantityProductPriceRules) {
                        List<GenericValue> ruleListToUse = new LinkedList<GenericValue>();
                        ruleListToUse.add(quantityProductPriceRule);
                        ruleListToUse.addAll(nonQuantityProductPriceRules);

                        Map<String, Object> quantCalcResults = calcPriceResultFromRules(ruleListToUse, listPrice, defaultPrice, promoPrice,
                            wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                            averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                            webSiteId, partyId, null, currencyDefaultUomId, delegator, nowTimestamp, locale);
                        Map<String, Object> quantErrorResult = addGeneralResults(quantCalcResults, competitivePriceValue, specialPromoPriceValue, productStore,
                            checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                        if (quantErrorResult != null) return quantErrorResult;

                        // also add the quantityProductPriceRule to the Map so it can be used for quantity break information
                        quantCalcResults.put("quantityProductPriceRule", quantityProductPriceRule);

                        allQuantityPrices.add(quantCalcResults);
                    }
                    result.put("allQuantityPrices", allQuantityPrices);

                    // use a quantity 1 to get the main price, then fill in the quantity break prices
                    Map<String, Object> calcResults = calcPriceResultFromRules(allProductPriceRules, listPrice, defaultPrice, promoPrice,
                        wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                        averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                        webSiteId, partyId, BigDecimal.ONE, currencyDefaultUomId, delegator, nowTimestamp, locale);
                    result.putAll(calcResults);
                    // The orderItemPriceInfos out parameter requires a special treatment:
                    // the list of OrderItemPriceInfos generated by the price rule is appended to
                    // the existing orderItemPriceInfos list and the aggregated list is returned.
                    List<GenericValue> orderItemPriceInfosFromRule = UtilGenerics.checkList(calcResults.get("orderItemPriceInfos"));
                    if (UtilValidate.isNotEmpty(orderItemPriceInfosFromRule)) {
                        orderItemPriceInfos.addAll(orderItemPriceInfosFromRule);
                    }
                    result.put("orderItemPriceInfos", orderItemPriceInfos);

                    Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                            checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                    if (errorResult != null) return errorResult;
                } else {
                    Map<String, Object> calcResults = calcPriceResultFromRules(allProductPriceRules, listPrice, defaultPrice, promoPrice,
                        wholesalePrice, maximumPriceValue, minimumPriceValue, validPriceFound,
                        averageCostValue, productId, virtualProductId, prodCatalogId, productStoreGroupId,
                        webSiteId, partyId, quantity, currencyDefaultUomId, delegator, nowTimestamp, locale);
                    result.putAll(calcResults);
                    // The orderItemPriceInfos out parameter requires a special treatment:
                    // the list of OrderItemPriceInfos generated by the price rule is appended to
                    // the existing orderItemPriceInfos list and the aggregated list is returned.
                    List<GenericValue> orderItemPriceInfosFromRule = UtilGenerics.checkList(calcResults.get("orderItemPriceInfos"));
                    if (UtilValidate.isNotEmpty(orderItemPriceInfosFromRule)) {
                        orderItemPriceInfos.addAll(orderItemPriceInfosFromRule);
                    }
                    result.put("orderItemPriceInfos", orderItemPriceInfos);

                    Map<String, Object> errorResult = addGeneralResults(result, competitivePriceValue, specialPromoPriceValue, productStore,
                        checkIncludeVat, currencyDefaultUomId, productId, quantity, partyId, dispatcher, locale);
                    if (errorResult != null) return errorResult;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting rules from the database while calculating price", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductPriceCannotRetrievePriceRules", UtilMisc.toMap("errorString", e.toString()) , locale));
            }
        }

        // Convert the value to the price currency, if required
        if ("true".equals(EntityUtilProperties.getPropertyValue("catalog", "convertProductPriceCurrency", delegator))) {
            if (UtilValidate.isNotEmpty(currencyDefaultUomId) && UtilValidate.isNotEmpty(currencyUomIdTo) && !currencyDefaultUomId.equals(currencyUomIdTo)) {
                if (UtilValidate.isNotEmpty(result)) {
                    Map<String, Object> convertPriceMap = new HashMap<String, Object>();
                    for (Map.Entry<String, Object> entry : result.entrySet()) {
                        BigDecimal tempPrice = BigDecimal.ZERO;
                        switch (entry.getKey()) {
                        case "basePrice":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "price":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "defaultPrice":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "competitivePrice":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "averageCost":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "promoPrice":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "specialPromoPrice":
                            tempPrice = (BigDecimal) entry.getValue();
                        case "listPrice":
                            tempPrice = (BigDecimal) entry.getValue();

                        }
                        
                        if (tempPrice != null && tempPrice != BigDecimal.ZERO) {
                            Map<String, Object> priceResults = new HashMap<String, Object>();
                            try {
                                priceResults = dispatcher.runSync("convertUom", UtilMisc.<String, Object> toMap("uomId", currencyDefaultUomId, "uomIdTo", currencyUomIdTo,
                                        "originalValue", tempPrice, "defaultDecimalScale", Long.valueOf(2), "defaultRoundingMode", "HalfUp"));
                                if (ServiceUtil.isError(priceResults) || (priceResults.get("convertedValue") == null)) {
                                    Debug.logWarning("Unable to convert " + entry.getKey() + " for product  " + productId, module);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                            }
                            convertPriceMap.put(entry.getKey(), priceResults.get("convertedValue"));
                        } else {
                            convertPriceMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    if (UtilValidate.isNotEmpty(convertPriceMap)) {
                        convertPriceMap.put("currencyUsed", currencyUomIdTo);
                        result = convertPriceMap;
                    }
                }
            }
        }
        
        return result;
    }

    private static GenericValue getPriceValueForType(String productPriceTypeId, List<GenericValue> productPriceList, List<GenericValue> secondaryPriceList) {
        List<GenericValue> filteredPrices = EntityUtil.filterByAnd(productPriceList, UtilMisc.toMap("productPriceTypeId", productPriceTypeId));
        GenericValue priceValue = EntityUtil.getFirst(filteredPrices);
        if (filteredPrices != null && filteredPrices.size() > 1) {
            if (Debug.infoOn()) Debug.logInfo("There is more than one " + productPriceTypeId + " with the currencyUomId " + priceValue.getString("currencyUomId") + " and productId " + priceValue.getString("productId") + ", using the latest found with price: " + priceValue.getBigDecimal("price"), module);
        }
        if (priceValue == null && secondaryPriceList != null) {
            return getPriceValueForType(productPriceTypeId, secondaryPriceList, null);
        }
        return priceValue;
    }

    public static Map<String, Object> addGeneralResults(Map<String, Object> result, GenericValue competitivePriceValue, GenericValue specialPromoPriceValue, GenericValue productStore,
        String checkIncludeVat, String currencyUomId, String productId, BigDecimal quantity, String partyId, LocalDispatcher dispatcher, Locale locale) {
        result.put("competitivePrice", competitivePriceValue != null ? competitivePriceValue.getBigDecimal("price") : null);
        result.put("specialPromoPrice", specialPromoPriceValue != null ? specialPromoPriceValue.getBigDecimal("price") : null);
        result.put("currencyUsed", currencyUomId);

        // okay, now we have the calculated price, see if we should add in tax and if so do it
        if ("Y".equals(checkIncludeVat) && productStore != null && "Y".equals(productStore.getString("showPricesWithVatTax"))) {
            Map<String, Object> calcTaxForDisplayContext = UtilMisc.toMap("productStoreId", productStore.get("productStoreId"),
                    "productId", productId, "quantity", quantity,
                    "basePrice", (BigDecimal) result.get("price"));
            if (UtilValidate.isNotEmpty(partyId)) {
                calcTaxForDisplayContext.put("billToPartyId", partyId);
            }

            try {
                Map<String, Object> calcTaxForDisplayResult = dispatcher.runSync("calcTaxForDisplay", calcTaxForDisplayContext);
                if (ServiceUtil.isError(calcTaxForDisplayResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "ProductPriceCannotCalculateVatTax", locale), null, null, calcTaxForDisplayResult);
                }
                // taxTotal, taxPercentage, priceWithTax
                result.put("price", calcTaxForDisplayResult.get("priceWithTax"));

                // based on the taxPercentage calculate the other amounts, including: listPrice, defaultPrice, averageCost, promoPrice, competitivePrice
                BigDecimal taxPercentage = (BigDecimal) calcTaxForDisplayResult.get("taxPercentage");
                BigDecimal taxMultiplier = ONE_BASE.add(taxPercentage.divide(PERCENT_SCALE, taxCalcScale));
                if (result.get("listPrice") != null) {
                    result.put("listPrice", ((BigDecimal) result.get("listPrice")).multiply(taxMultiplier).setScale(taxFinalScale, taxRounding));
                }
                if (result.get("defaultPrice") != null) {
                    result.put("defaultPrice", ((BigDecimal) result.get("defaultPrice")).multiply(taxMultiplier).setScale(taxFinalScale, taxRounding));
                }
                if (result.get("averageCost") != null) {
                    result.put("averageCost", ((BigDecimal) result.get("averageCost")).multiply(taxMultiplier).setScale(taxFinalScale, taxRounding));
                }
                if (result.get("promoPrice") != null) {
                    result.put("promoPrice", ((BigDecimal) result.get("promoPrice")).multiply(taxMultiplier).setScale(taxFinalScale, taxRounding));
                }
                if (result.get("competitivePrice") != null) {
                    result.put("competitivePrice", ((BigDecimal) result.get("competitivePrice")).multiply(taxMultiplier).setScale(taxFinalScale, taxRounding));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Error calculating VAT tax (with calcTaxForDisplay service): " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductPriceCannotCalculateVatTax", locale));
            }
        }

        return null;
    }

    public static List<GenericValue> makeProducePriceRuleList(Delegator delegator, boolean optimizeForLargeRuleSet, String productId, String virtualProductId, String prodCatalogId, String productStoreGroupId, String webSiteId, String partyId, String currencyUomId) throws GenericEntityException {
        List<GenericValue> productPriceRules = null;

        // At this point we have two options: optimize for large ruleset, or optimize for small ruleset
        // NOTE: This only effects the way that the rules to be evaluated are selected.
        // For large rule sets we can do a cached pre-filter to limit the rules that need to be evaled for a specific product.
        // Genercally I don't think that rule sets will get that big though, so the default is optimize for smaller rule set.
        if (optimizeForLargeRuleSet) {
            // ========= find all rules that must be run for each input type; this is kind of like a pre-filter to slim down the rules to run =========
            TreeSet<String> productPriceRuleIds = new TreeSet<String>();

            // ------- These are all of the conditions that DON'T depend on the current inputs -------

            // by productCategoryId
            // for we will always include any rules that go by category, shouldn't be too many to iterate through each time and will save on cache entries
            // note that we always want to put the category, quantity, etc ones that find all rules with these conditions in separate cache lists so that they can be easily cleared
            Collection<GenericValue> productCategoryIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PROD_CAT_ID").cache(true).queryList();
            if (UtilValidate.isNotEmpty(productCategoryIdConds)) {
                for (GenericValue productCategoryIdCond: productCategoryIdConds) {
                    productPriceRuleIds.add(productCategoryIdCond.getString("productPriceRuleId"));
                }
            }

            // by productFeatureId
            Collection<GenericValue> productFeatureIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PROD_FEAT_ID").cache(true).queryList();
            if (UtilValidate.isNotEmpty(productFeatureIdConds)) {
                for (GenericValue productFeatureIdCond: productFeatureIdConds) {
                    productPriceRuleIds.add(productFeatureIdCond.getString("productPriceRuleId"));
                }
            }

            // by quantity -- should we really do this one, ie is it necessary?
            // we could say that all rules with quantity on them must have one of these other values
            // but, no we'll do it the other way, any that have a quantity will always get compared
            Collection<GenericValue> quantityConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_QUANTITY").cache(true).queryList();
            if (UtilValidate.isNotEmpty(quantityConds)) {
                for (GenericValue quantityCond: quantityConds) {
                    productPriceRuleIds.add(quantityCond.getString("productPriceRuleId"));
                }
            }

            // by roleTypeId
            Collection<GenericValue> roleTypeIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_ROLE_TYPE").cache(true).queryList();
            if (UtilValidate.isNotEmpty(roleTypeIdConds)) {
                for (GenericValue roleTypeIdCond: roleTypeIdConds) {
                    productPriceRuleIds.add(roleTypeIdCond.getString("productPriceRuleId"));
                }
            }

            // TODO, not supported yet: by groupPartyId
            // TODO, not supported yet: by partyClassificationGroupId
            // later: (by partyClassificationTypeId)

            // by listPrice
            Collection<GenericValue> listPriceConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_LIST_PRICE").cache(true).queryList();
            if (UtilValidate.isNotEmpty(listPriceConds)) {
                for (GenericValue listPriceCond: listPriceConds) {
                    productPriceRuleIds.add(listPriceCond.getString("productPriceRuleId"));
                }
            }

            // ------- These are all of them that DO depend on the current inputs -------

            // by productId
            Collection<GenericValue> productIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PRODUCT_ID", "condValue", productId).cache(true).queryList();
            if (UtilValidate.isNotEmpty(productIdConds)) {
                for (GenericValue productIdCond: productIdConds) {
                    productPriceRuleIds.add(productIdCond.getString("productPriceRuleId"));
                }
            }

            // by virtualProductId, if not null
            if (virtualProductId != null) {
                Collection<GenericValue> virtualProductIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PRODUCT_ID", "condValue", virtualProductId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(virtualProductIdConds)) {
                    for (GenericValue virtualProductIdCond: virtualProductIdConds) {
                        productPriceRuleIds.add(virtualProductIdCond.getString("productPriceRuleId"));
                    }
                }
            }

            // by prodCatalogId - which is optional in certain cases
            if (UtilValidate.isNotEmpty(prodCatalogId)) {
                Collection<GenericValue> prodCatalogIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PROD_CLG_ID", "condValue", prodCatalogId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(prodCatalogIdConds)) {
                    for (GenericValue prodCatalogIdCond: prodCatalogIdConds) {
                        productPriceRuleIds.add(prodCatalogIdCond.getString("productPriceRuleId"));
                    }
                }
            }

            // by productStoreGroupId
            if (UtilValidate.isNotEmpty(productStoreGroupId)) {
                Collection<GenericValue> storeGroupConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PROD_SGRP_ID", "condValue", productStoreGroupId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(storeGroupConds)) {
                    for (GenericValue storeGroupCond: storeGroupConds) {
                        productPriceRuleIds.add(storeGroupCond.getString("productPriceRuleId"));
                    }
                }
            }

            // by webSiteId
            if (UtilValidate.isNotEmpty(webSiteId)) {
                Collection<GenericValue> webSiteIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_WEBSITE_ID", "condValue", webSiteId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(webSiteIdConds)) {
                    for (GenericValue webSiteIdCond: webSiteIdConds) {
                        productPriceRuleIds.add(webSiteIdCond.getString("productPriceRuleId"));
                    }
                }
            }

            // by partyId
            if (UtilValidate.isNotEmpty(partyId)) {
                Collection<GenericValue> partyIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_PARTY_ID", "condValue", partyId).cache(true).queryList();
                if (UtilValidate.isNotEmpty(partyIdConds)) {
                    for (GenericValue partyIdCond: partyIdConds) {
                        productPriceRuleIds.add(partyIdCond.getString("productPriceRuleId"));
                    }
                }
            }

            // by currencyUomId
            Collection<GenericValue> currencyUomIdConds = EntityQuery.use(delegator).from("ProductPriceCond").where("inputParamEnumId", "PRIP_CURRENCY_UOMID", "condValue", currencyUomId).cache(true).queryList();
            if (UtilValidate.isNotEmpty(currencyUomIdConds)) {
                for (GenericValue currencyUomIdCond: currencyUomIdConds) {
                    productPriceRuleIds.add(currencyUomIdCond.getString("productPriceRuleId"));
                }
            }

            productPriceRules = new LinkedList<GenericValue>();
            for (String productPriceRuleId: productPriceRuleIds) {
                GenericValue productPriceRule = EntityQuery.use(delegator).from("ProductPriceRule").where("productPriceRuleId", productPriceRuleId).cache().queryOne();
                if (productPriceRule == null) continue;
                productPriceRules.add(productPriceRule);
            }
        } else {
            productPriceRules = EntityQuery.use(delegator).from("ProductPriceRule").cache(true).queryList();
            if (productPriceRules == null) productPriceRules = new LinkedList<GenericValue>();
        }

        return productPriceRules;
    }

    public static Map<String, Object> calcPriceResultFromRules(List<GenericValue> productPriceRules, BigDecimal listPrice, BigDecimal defaultPrice, BigDecimal promoPrice,
        BigDecimal wholesalePrice, GenericValue maximumPriceValue, GenericValue minimumPriceValue, boolean validPriceFound,
        GenericValue averageCostValue, String productId, String virtualProductId, String prodCatalogId, String productStoreGroupId,
        String webSiteId, String partyId, BigDecimal quantity, String currencyUomId, Delegator delegator, Timestamp nowTimestamp,
        Locale locale) throws GenericEntityException {

        Map<String, Object> calcResults = new HashMap<String, Object>();

        List<GenericValue> orderItemPriceInfos = new LinkedList<GenericValue>();
        boolean isSale = false;

        // ========= go through each price rule by id and eval all conditions =========
        int totalConds = 0;
        int totalActions = 0;
        int totalRules = 0;

        // get some of the base values to calculate with
        BigDecimal averageCost = (averageCostValue != null && averageCostValue.get("price") != null) ? averageCostValue.getBigDecimal("price") : listPrice;
        BigDecimal margin = listPrice.subtract(averageCost);

        // calculate running sum based on listPrice and rules found
        BigDecimal price = listPrice;

        for (GenericValue productPriceRule: productPriceRules) {
            String productPriceRuleId = productPriceRule.getString("productPriceRuleId");

            // check from/thru dates
            java.sql.Timestamp fromDate = productPriceRule.getTimestamp("fromDate");
            java.sql.Timestamp thruDate = productPriceRule.getTimestamp("thruDate");

            if (fromDate != null && fromDate.after(nowTimestamp)) {
                // hasn't started yet
                continue;
            }
            if (thruDate != null && thruDate.before(nowTimestamp)) {
                // already expired
                continue;
            }

            // check all conditions
            boolean allTrue = true;
            StringBuilder condsDescription = new StringBuilder();
            List<GenericValue> productPriceConds = EntityQuery.use(delegator).from("ProductPriceCond").where("productPriceRuleId", productPriceRuleId).cache(true).queryList();
            for (GenericValue productPriceCond: productPriceConds) {

                totalConds++;

                if (!checkPriceCondition(productPriceCond, productId, virtualProductId, prodCatalogId, productStoreGroupId, webSiteId, partyId, quantity, listPrice, currencyUomId, delegator, nowTimestamp)) {
                    allTrue = false;
                    break;
                }

                // add condsDescription string entry
                condsDescription.append("[");
                GenericValue inputParamEnum = productPriceCond.getRelatedOne("InputParamEnumeration", true);

                condsDescription.append(inputParamEnum.getString("enumCode"));
                // condsDescription.append(":");
                GenericValue operatorEnum = productPriceCond.getRelatedOne("OperatorEnumeration", true);

                condsDescription.append(operatorEnum.getString("description"));
                // condsDescription.append(":");
                condsDescription.append(productPriceCond.getString("condValue"));
                condsDescription.append("] ");
            }

            // add some info about the prices we are calculating from
            condsDescription.append("[list:");
            condsDescription.append(listPrice);
            condsDescription.append(";avgCost:");
            condsDescription.append(averageCost);
            condsDescription.append(";margin:");
            condsDescription.append(margin);
            condsDescription.append("] ");

            boolean foundFlatOverride = false;

            // if all true, perform all actions
            if (allTrue) {
                // check isSale
                if ("Y".equals(productPriceRule.getString("isSale"))) {
                    isSale = true;
                }

                List<GenericValue> productPriceActions = EntityQuery.use(delegator).from("ProductPriceAction").where("productPriceRuleId", productPriceRuleId).cache(true).queryList();
                for (GenericValue productPriceAction: productPriceActions) {

                    totalActions++;

                    // yeah, finally here, perform the action, ie, modify the price
                    BigDecimal modifyAmount = BigDecimal.ZERO;

                    if ("PRICE_POD".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null) {
                            modifyAmount = defaultPrice.multiply(productPriceAction.getBigDecimal("amount").movePointLeft(2));
                            price = defaultPrice;
                        }
                    } else if ("PRICE_POL".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null) {
                            modifyAmount = listPrice.multiply(productPriceAction.getBigDecimal("amount").movePointLeft(2));
                        }
                    } else if ("PRICE_POAC".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null) {
                            modifyAmount = averageCost.multiply(productPriceAction.getBigDecimal("amount").movePointLeft(2));
                        }
                    } else if ("PRICE_POM".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null) {
                            modifyAmount = margin.multiply(productPriceAction.getBigDecimal("amount").movePointLeft(2));
                        }
                    } else if ("PRICE_POWHS".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null && wholesalePrice != null) {
                            modifyAmount = wholesalePrice.multiply(productPriceAction.getBigDecimal("amount").movePointLeft(2));
                        }
                    } else if ("PRICE_FOL".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        if (productPriceAction.get("amount") != null) {
                            modifyAmount = productPriceAction.getBigDecimal("amount");
                        }
                    } else if ("PRICE_FLAT".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        // this one is a bit different, break out of the loop because we now have our final price
                        foundFlatOverride = true;
                        if (productPriceAction.get("amount") != null) {
                            price = productPriceAction.getBigDecimal("amount");
                        } else {
                            Debug.logInfo("ProductPriceAction had null amount, using default price: " + defaultPrice + " for product with id " + productId, module);
                            price = defaultPrice;
                            isSale = false;                // reverse isSale flag, as this sale rule was actually not applied
                        }
                    } else if ("PRICE_PFLAT".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        // this one is a bit different too, break out of the loop because we now have our final price
                        foundFlatOverride = true;
                        price = promoPrice;
                        if (productPriceAction.get("amount") != null) {
                            price = price.add(productPriceAction.getBigDecimal("amount"));
                        }
                        if (price.compareTo(BigDecimal.ZERO) == 0) {
                            if (defaultPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo("PromoPrice and ProductPriceAction had null amount, using default price: " + defaultPrice + " for product with id " + productId, module);
                                price = defaultPrice;
                            } else if (listPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo("PromoPrice and ProductPriceAction had null amount and no default price was available, using list price: " + listPrice + " for product with id " + productId, module);
                                price = listPrice;
                            } else {
                                Debug.logError("PromoPrice and ProductPriceAction had null amount and no default or list price was available, so price is set to zero for product with id " + productId, module);
                                price = BigDecimal.ZERO;
                            }
                            isSale = false;                // reverse isSale flag, as this sale rule was actually not applied
                        }
                    } else if ("PRICE_WFLAT".equals(productPriceAction.getString("productPriceActionTypeId"))) {
                        // same as promo price but using the wholesale price instead
                        foundFlatOverride = true;
                        price = wholesalePrice;
                        if (productPriceAction.get("amount") != null) {
                            price = price.add(productPriceAction.getBigDecimal("amount"));
                        }
                        if (price.compareTo(BigDecimal.ZERO) == 0) {
                            if (defaultPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo("WholesalePrice and ProductPriceAction had null amount, using default price: " + defaultPrice + " for product with id " + productId, module);
                                price = defaultPrice;
                            } else if (listPrice.compareTo(BigDecimal.ZERO) != 0) {
                                Debug.logInfo("WholesalePrice and ProductPriceAction had null amount and no default price was available, using list price: " + listPrice + " for product with id " + productId, module);
                                price = listPrice;
                            } else {
                                Debug.logError("WholesalePrice and ProductPriceAction had null amount and no default or list price was available, so price is set to zero for product with id " + productId, module);
                                price = BigDecimal.ZERO;
                            }
                            isSale = false; // reverse isSale flag, as this sale rule was actually not applied
                        }
                    }

                    // add a orderItemPriceInfo element too, without orderId or orderItemId
                    StringBuilder priceInfoDescription = new StringBuilder();

                    
                    priceInfoDescription.append(condsDescription.toString());
                    priceInfoDescription.append("[");
                    priceInfoDescription.append(UtilProperties.getMessage(resource, "ProductPriceConditionType", locale));
                    priceInfoDescription.append(productPriceAction.getString("productPriceActionTypeId"));
                    priceInfoDescription.append("]");

                    GenericValue orderItemPriceInfo = delegator.makeValue("OrderItemPriceInfo");

                    orderItemPriceInfo.set("productPriceRuleId", productPriceAction.get("productPriceRuleId"));
                    orderItemPriceInfo.set("productPriceActionSeqId", productPriceAction.get("productPriceActionSeqId"));
                    orderItemPriceInfo.set("modifyAmount", modifyAmount);
                    orderItemPriceInfo.set("rateCode", productPriceAction.get("rateCode"));
                    // make sure description is <= than 250 chars
                    String priceInfoDescriptionString = priceInfoDescription.toString();

                    if (priceInfoDescriptionString.length() > 250) {
                        priceInfoDescriptionString = priceInfoDescriptionString.substring(0, 250);
                    }
                    orderItemPriceInfo.set("description", priceInfoDescriptionString);
                    orderItemPriceInfos.add(orderItemPriceInfo);

                    if (foundFlatOverride) {
                        break;
                    } else {
                        price = price.add(modifyAmount);
                    }
                }
            }

            totalRules++;

            if (foundFlatOverride) {
                break;
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Unchecked Calculated price: " + price, module);
            Debug.logVerbose("PriceInfo:", module);
            for (GenericValue orderItemPriceInfo: orderItemPriceInfos) {
                if (Debug.verboseOn()) Debug.logVerbose(" --- " + orderItemPriceInfo, module);
            }
        }

        // if no actions were run on the list price, then use the default price
        if (totalActions == 0) {
            price = defaultPrice;
            // here we will leave validPriceFound as it was originally set for the defaultPrice since that is what we are setting the price to...
        } else {
            // at least one price rule action was found, so we will consider it valid
            validPriceFound = true;
        }

        // ========= ensure calculated price is not below minSalePrice or above maxSalePrice =========
        BigDecimal maxSellPrice = maximumPriceValue != null ? maximumPriceValue.getBigDecimal("price") : null;
        if (maxSellPrice != null && price.compareTo(maxSellPrice) > 0) {
            price = maxSellPrice;
        }
        // min price second to override max price, safety net
        BigDecimal minSellPrice = minimumPriceValue != null ? minimumPriceValue.getBigDecimal("price") : null;
        if (minSellPrice != null && price.compareTo(minSellPrice) < 0) {
            price = minSellPrice;
            // since we have found a minimum price that has overriden a the defaultPrice, even if no valid one was found, we will consider it as if one had been...
            validPriceFound = true;
        }

        if (Debug.verboseOn()) Debug.logVerbose("Final Calculated price: " + price + ", rules: " + totalRules + ", conds: " + totalConds + ", actions: " + totalActions, module);

        calcResults.put("basePrice", price);
        calcResults.put("price", price);
        calcResults.put("listPrice", listPrice);
        calcResults.put("defaultPrice", defaultPrice);
        calcResults.put("averageCost", averageCost);
        calcResults.put("orderItemPriceInfos", orderItemPriceInfos);
        calcResults.put("isSale", Boolean.valueOf(isSale));
        calcResults.put("validPriceFound", Boolean.valueOf(validPriceFound));

        return calcResults;
    }

    public static boolean checkPriceCondition(GenericValue productPriceCond, String productId, String virtualProductId, String prodCatalogId,
            String productStoreGroupId, String webSiteId, String partyId, BigDecimal quantity, BigDecimal listPrice,
            String currencyUomId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        if (Debug.verboseOn()) Debug.logVerbose("Checking price condition: " + productPriceCond, module);
        int compare = 0;

        if ("PRIP_PRODUCT_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            compare = UtilMisc.toList(productId, virtualProductId).contains(productPriceCond.getString("condValue"))? 0: 1;
        } else if ("PRIP_PROD_CAT_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            // if a ProductCategoryMember exists for this productId and the specified productCategoryId
            String productCategoryId = productPriceCond.getString("condValue");
            // and from/thru date within range
            List<GenericValue> productCategoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember")
                    .where("productId", productId, "productCategoryId", productCategoryId)
                    .cache(true)
                    .filterByDate(nowTimestamp)
                    .queryList();
            // then 0 (equals), otherwise 1 (not equals)
            if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                compare = 0;
            } else {
                compare = 1;
            }

            // if there is a virtualProductId, try that given that this one has failed
            // NOTE: this is important becuase of the common scenario where a virtual product is a member of a category but the variants will typically NOT be
            // NOTE: we may want to parameterize this in the future, ie with an indicator on the ProductPriceCond entity
            if (compare == 1 && UtilValidate.isNotEmpty(virtualProductId)) {
                // and from/thru date within range
                List<GenericValue> virtualProductCategoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId", virtualProductId, "productCategoryId", productCategoryId).cache(true).filterByDate(nowTimestamp).queryList();
                if (UtilValidate.isNotEmpty(virtualProductCategoryMembers)) {
                    // we found a member record? great, then this condition is satisfied
                    compare = 0;
                }
            }
        } else if ("PRIP_PROD_FEAT_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            // NOTE: DEJ20070130 don't retry this condition with the virtualProductId as well; this breaks various things you might want to do with price rules, like have different pricing for a variant products with a certain distinguishing feature

            // if a ProductFeatureAppl exists for this productId and the specified productFeatureId
            String productFeatureId = productPriceCond.getString("condValue");
            // and from/thru date within range
            List<GenericValue> productFeatureAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productId", productId, "productFeatureId", productFeatureId).cache(true).filterByDate(nowTimestamp).queryList();
            // then 0 (equals), otherwise 1 (not equals)
            if (UtilValidate.isNotEmpty(productFeatureAppls)) {
                compare = 0;
            } else {
                compare = 1;
            }
        } else if ("PRIP_PROD_CLG_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isNotEmpty(prodCatalogId)) {
                compare = prodCatalogId.compareTo(productPriceCond.getString("condValue"));
            } else {
                // this shouldn't happen because if prodCatalogId is null no PRIP_PROD_CLG_ID prices will be in the list
                compare = 1;
            }
        } else if ("PRIP_PROD_SGRP_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isNotEmpty(productStoreGroupId)) {
                compare = productStoreGroupId.compareTo(productPriceCond.getString("condValue"));
            } else {
                compare = 1;
            }
        } else if ("PRIP_WEBSITE_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isNotEmpty(webSiteId)) {
                compare = webSiteId.compareTo(productPriceCond.getString("condValue"));
            } else {
                compare = 1;
            }
        } else if ("PRIP_QUANTITY".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (quantity == null) {
                // if no quantity is passed in, assume all quantity conditions pass
                // NOTE: setting compare = 0 won't do the trick here because the condition won't always be or include and equal
                return true;
            } else {
                compare = quantity.compareTo(new BigDecimal(productPriceCond.getString("condValue")));
            }
        } else if ("PRIP_PARTY_ID".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isNotEmpty(partyId)) {
                compare = partyId.compareTo(productPriceCond.getString("condValue"));
            } else {
                compare = 1;
            }
        } else if ("PRIP_PARTY_GRP_MEM".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isEmpty(partyId)) {
                compare = 1;
            } else {
                String groupPartyId = productPriceCond.getString("condValue");
                if (partyId.equals(groupPartyId)) {
                    compare = 0;
                } else {
                    // look for PartyRelationship with
                    // partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is
                    // the group member, so the partyIdFrom is the groupPartyId
                    // and from/thru date within range
                    List<GenericValue> partyRelationshipList = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", groupPartyId, "partyIdTo", partyId, "partyRelationshipTypeId", "GROUP_ROLLUP").cache(true).filterByDate(nowTimestamp).queryList();
                    // then 0 (equals), otherwise 1 (not equals)
                    if (UtilValidate.isNotEmpty(partyRelationshipList)) {
                        compare = 0;
                    } else {
                        compare = checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyId);
                    }
                }
            }
        } else if ("PRIP_PARTY_CLASS".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (UtilValidate.isEmpty(partyId)) {
                compare = 1;
            } else {
                String partyClassificationGroupId = productPriceCond.getString("condValue");
                // find any PartyClassification
                // and from/thru date within range
                List<GenericValue> partyClassificationList = EntityQuery.use(delegator).from("PartyClassification").where("partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId).cache(true).filterByDate(nowTimestamp).queryList();
                // then 0 (equals), otherwise 1 (not equals)
                if (UtilValidate.isNotEmpty(partyClassificationList)) {
                    compare = 0;
                } else {
                    compare = 1;
                }
            }
        } else if ("PRIP_ROLE_TYPE".equals(productPriceCond.getString("inputParamEnumId"))) {
            if (partyId != null) {
                // if a PartyRole exists for this partyId and the specified roleTypeId
                GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId, "roleTypeId", productPriceCond.getString("condValue")).cache(true).queryOne();

                // then 0 (equals), otherwise 1 (not equals)
                if (partyRole != null) {
                    compare = 0;
                } else {
                    compare = 1;
                }
            } else {
                compare = 1;
            }
        } else if ("PRIP_LIST_PRICE".equals(productPriceCond.getString("inputParamEnumId"))) {
            BigDecimal listPriceValue = listPrice;

            compare = listPriceValue.compareTo(new BigDecimal(productPriceCond.getString("condValue")));
        } else if ("PRIP_CURRENCY_UOMID".equals(productPriceCond.getString("inputParamEnumId"))) {
            compare = currencyUomId.compareTo(productPriceCond.getString("condValue"));
        } else {
            Debug.logWarning("An un-supported productPriceCond input parameter (lhs) was used: " + productPriceCond.getString("inputParamEnumId") + ", returning false, ie check failed", module);
            return false;
        }

        if (Debug.verboseOn()) Debug.logVerbose("Price Condition compare done, compare=" + compare, module);

        if ("PRC_EQ".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare == 0) return true;
        } else if ("PRC_NEQ".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare != 0) return true;
        } else if ("PRC_LT".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare < 0) return true;
        } else if ("PRC_LTE".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare <= 0) return true;
        } else if ("PRC_GT".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare > 0) return true;
        } else if ("PRC_GTE".equals(productPriceCond.getString("operatorEnumId"))) {
            if (compare >= 0) return true;
        } else {
            Debug.logWarning("An un-supported productPriceCond condition was used: " + productPriceCond.getString("operatorEnumId") + ", returning false, ie check failed", module);
            return false;
        }
        return false;
    }

    private static int checkConditionPartyHierarchy(Delegator delegator, Timestamp nowTimestamp, String groupPartyId, String partyId) throws GenericEntityException{
        List<GenericValue> partyRelationshipList = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdTo", partyId, "partyRelationshipTypeId", "GROUP_ROLLUP").cache(true).filterByDate(nowTimestamp).queryList();
        for (GenericValue genericValue : partyRelationshipList) {
            String partyIdFrom = (String)genericValue.get("partyIdFrom");
            if (partyIdFrom.equals(groupPartyId)) {
                return 0;
            }
            if (0 == checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyIdFrom)) {
                return 0;
            }
        }
        
        return 1;
    }

    /**
     * Calculates the purchase price of a product
     */
    public static Map<String, Object> calculatePurchasePrice(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();

        List<GenericValue> orderItemPriceInfos = new LinkedList<GenericValue>();
        boolean validPriceFound = false;
        BigDecimal price = BigDecimal.ZERO;

        GenericValue product = (GenericValue)context.get("product");
        String productId = product.getString("productId");
        String agreementId = (String)context.get("agreementId");
        String currencyUomId = (String)context.get("currencyUomId");
        String partyId = (String)context.get("partyId");
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        Locale locale = (Locale)context.get("locale");

        // a) Get the Price from the Agreement* data model
        // TODO: Implement this

        // b) If no price can be found, get the lastPrice from the SupplierProduct entity
        if (!validPriceFound) {
            Map<String, Object> priceContext = UtilMisc.toMap("currencyUomId", currencyUomId, "partyId", partyId, "productId", productId, "quantity", quantity, "agreementId", agreementId);
            List<GenericValue> productSuppliers = null;
            try {
                Map<String, Object> priceResult = dispatcher.runSync("getSuppliersForProduct", priceContext);
                if (ServiceUtil.isError(priceResult)) {
                    String errMsg = ServiceUtil.getErrorMessage(priceResult);
                    Debug.logError(errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                productSuppliers = UtilGenerics.checkList(priceResult.get("supplierProducts"));
            } catch (GenericServiceException gse) {
                Debug.logError(gse, module);
                return ServiceUtil.returnError(gse.getMessage());
            }
            if (productSuppliers != null) {
                for (GenericValue productSupplier: productSuppliers) {
                    if (!validPriceFound) {
                        price = ((BigDecimal)productSupplier.get("lastPrice"));
                        validPriceFound = true;
                    }
                    // add a orderItemPriceInfo element too, without orderId or orderItemId
                    StringBuilder priceInfoDescription = new StringBuilder();
                    priceInfoDescription.append(UtilProperties.getMessage(resource, "ProductSupplier", locale));
                    priceInfoDescription.append(" [");
                    priceInfoDescription.append(UtilProperties.getMessage(resource, "ProductSupplierMinimumOrderQuantity", locale));
                    priceInfoDescription.append(productSupplier.getBigDecimal("minimumOrderQuantity"));
                    priceInfoDescription.append(UtilProperties.getMessage(resource, "ProductSupplierLastPrice", locale));
                    priceInfoDescription.append(productSupplier.getBigDecimal("lastPrice"));
                    priceInfoDescription.append("]");
                    GenericValue orderItemPriceInfo = delegator.makeValue("OrderItemPriceInfo");
                    // make sure description is <= than 250 chars
                    String priceInfoDescriptionString = priceInfoDescription.toString();
                    if (priceInfoDescriptionString.length() > 250) {
                        priceInfoDescriptionString = priceInfoDescriptionString.substring(0, 250);
                    }
                    orderItemPriceInfo.set("description", priceInfoDescriptionString);
                    orderItemPriceInfos.add(orderItemPriceInfo);
                }
            }
        }

        // c) If no price can be found, get the averageCost from the ProductPrice entity
        if (!validPriceFound) {
            List<GenericValue> prices = null;
            try {
                prices = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPricePurposeId", "PURCHASE").orderBy("-fromDate").queryList();

                // if no prices are found; find the prices of the parent product
                if (UtilValidate.isEmpty(prices)) {
                    GenericValue parentProduct = ProductWorker.getParentProduct(productId, delegator);
                    if (parentProduct != null) {
                        String parentProductId = parentProduct.getString("productId");
                        prices = EntityQuery.use(delegator).from("ProductPrice").where("productId", parentProductId, "productPricePurposeId", "PURCHASE").orderBy("-fromDate").queryList();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            // filter out the old prices
            prices = EntityUtil.filterByDate(prices);

            // first check for the AVERAGE_COST price type
            List<GenericValue> pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap("productPriceTypeId", "AVERAGE_COST"));
            if (UtilValidate.isEmpty(pricesToUse)) {
                // next go with default price
                pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap("productPriceTypeId", "DEFAULT_PRICE"));
                if (UtilValidate.isEmpty(pricesToUse)) {
                    // finally use list price
                    pricesToUse = EntityUtil.filterByAnd(prices, UtilMisc.toMap("productPriceTypeId", "LIST_PRICE"));
                }
            }

            // use the most current price
            GenericValue thisPrice = EntityUtil.getFirst(pricesToUse);
            if (thisPrice != null) {
                price = thisPrice.getBigDecimal("price");
                validPriceFound = true;
            }
        }

        result.put("price", price);
        result.put("validPriceFound", Boolean.valueOf(validPriceFound));
        result.put("orderItemPriceInfos", orderItemPriceInfos);
        return result;
    }
}
