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
package org.ofbiz.accounting.tax;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.geo.GeoWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * Tax Authority tax calculation and other misc services
 */

public class TaxAuthorityServices {

    public static final String module = TaxAuthorityServices.class.getName();
    public static final BigDecimal ZERO_BASE = BigDecimal.ZERO;
    public static final BigDecimal ONE_BASE = BigDecimal.ONE;
    public static final BigDecimal PERCENT_SCALE = new BigDecimal("100.000");
    public static int salestaxFinalDecimals = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    public static int salestaxCalcDecimals = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static int salestaxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
    public static final String resource = "AccountingUiLabels";

    public static Map<String, Object> rateProductTaxCalcForDisplay(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String billToPartyId = (String) context.get("billToPartyId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal basePrice = (BigDecimal) context.get("basePrice");
        BigDecimal shippingPrice = (BigDecimal) context.get("shippingPrice");
        Locale locale = (Locale) context.get("locale");

        if (quantity == null) quantity = ONE_BASE;
        BigDecimal amount = basePrice.multiply(quantity);

        BigDecimal taxTotal = ZERO_BASE;
        BigDecimal taxPercentage = ZERO_BASE;
        BigDecimal priceWithTax = basePrice;
        if (shippingPrice != null) priceWithTax = priceWithTax.add(shippingPrice);

        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
            if (productStore == null) {
                throw new IllegalArgumentException("Could not find ProductStore with ID [" + productStoreId + "] for tax calculation");
            }

            if ("Y".equals(productStore.getString("showPricesWithVatTax"))) {
                Set<GenericValue> taxAuthoritySet = new HashSet<GenericValue>();
                if (productStore.get("vatTaxAuthPartyId") == null) {
                    List<GenericValue> taxAuthorityRawList = EntityQuery.use(delegator).from("TaxAuthority")
                            .where("taxAuthGeoId", productStore.get("vatTaxAuthGeoId")).cache().queryList();
                    taxAuthoritySet.addAll(taxAuthorityRawList);
                } else {
                    GenericValue taxAuthority = EntityQuery.use(delegator).from("TaxAuthority").where("taxAuthGeoId", productStore.get("vatTaxAuthGeoId"), "taxAuthPartyId", productStore.get("vatTaxAuthPartyId")).cache().queryOne();
                    taxAuthoritySet.add(taxAuthority);
                }

                if (taxAuthoritySet.size() == 0) {
                    throw new IllegalArgumentException("Could not find any Tax Authories for store with ID [" + productStoreId + "] for tax calculation; the store settings may need to be corrected.");
                }

                List<GenericValue> taxAdustmentList = getTaxAdjustments(delegator, product, productStore, null, billToPartyId, taxAuthoritySet, basePrice, quantity, amount, shippingPrice, ZERO_BASE);
                if (taxAdustmentList.size() == 0) {
                    // this is something that happens every so often for different products and such, so don't blow up on it...
                    Debug.logWarning("Could not find any Tax Authories Rate Rules for store with ID [" + productStoreId + "], productId [" + productId + "], basePrice [" + basePrice + "], amount [" + amount + "], for tax calculation; the store settings may need to be corrected.", module);
                }

                // add up amounts from adjustments (amount OR exemptAmount, sourcePercentage)
                for (GenericValue taxAdjustment : taxAdustmentList) {
                    if ("SALES_TAX".equals(taxAdjustment.getString("orderAdjustmentTypeId"))) {
                        taxPercentage = taxPercentage.add(taxAdjustment.getBigDecimal("sourcePercentage"));
                        BigDecimal adjAmount = taxAdjustment.getBigDecimal("amount");
                        taxTotal = taxTotal.add(adjAmount);
                        priceWithTax = priceWithTax.add(adjAmount.divide(quantity,salestaxCalcDecimals,salestaxRounding));
                        Debug.logInfo("For productId [" + productId + "] added [" + adjAmount.divide(quantity,salestaxCalcDecimals,salestaxRounding) + "] of tax to price for geoId [" + taxAdjustment.getString("taxAuthGeoId") + "], new price is [" + priceWithTax + "]", module);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Data error getting tax settings: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingTaxSettingError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // round to 2 decimal places for display/etc
        taxTotal = taxTotal.setScale(salestaxFinalDecimals, salestaxRounding);
        priceWithTax = priceWithTax.setScale(salestaxFinalDecimals, salestaxRounding);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("taxTotal", taxTotal);
        result.put("taxPercentage", taxPercentage);
        result.put("priceWithTax", priceWithTax);
        return result;
    }

    public static Map<String, Object> rateProductTaxCalc(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String facilityId = (String) context.get("facilityId");
        String payToPartyId = (String) context.get("payToPartyId");
        String billToPartyId = (String) context.get("billToPartyId");
        List<GenericValue> itemProductList = UtilGenerics.checkList(context.get("itemProductList"));
        List<BigDecimal> itemAmountList = UtilGenerics.checkList(context.get("itemAmountList"));
        List<BigDecimal> itemPriceList = UtilGenerics.checkList(context.get("itemPriceList"));
        List<BigDecimal> itemQuantityList = UtilGenerics.checkList(context.get("itemQuantityList"));
        List<BigDecimal> itemShippingList = UtilGenerics.checkList(context.get("itemShippingList"));
        BigDecimal orderShippingAmount = (BigDecimal) context.get("orderShippingAmount");
        BigDecimal orderPromotionsAmount = (BigDecimal) context.get("orderPromotionsAmount");
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");
        Locale locale = (Locale) context.get("locale");
        GenericValue productStore = null;
        GenericValue facility = null;
        try {
            if (productStoreId != null) {
                productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
            }
            if (facilityId != null) {
                facility = EntityQuery.use(delegator).from("Facility").where("facilityId", facilityId).queryOne();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Data error getting tax settings: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingTaxSettingError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        if (productStore == null && payToPartyId == null) {
            throw new IllegalArgumentException("Could not find payToPartyId [" + payToPartyId + "] or ProductStore [" + productStoreId + "] for tax calculation");
        }
        
        if (shippingAddress == null && facility != null) {
            // if there is no shippingAddress and there is a facility it means it is a face-to-face sale so get facility's address
            try {
                GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
                if (facilityContactMech != null) {
                    shippingAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", facilityContactMech.get("contactMechId")).queryOne();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Data error getting tax settings: " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingTaxSettingError", UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }
        if (shippingAddress == null || (shippingAddress.get("countryGeoId") == null && shippingAddress.get("stateProvinceGeoId") == null && shippingAddress.get("postalCodeGeoId") == null)) {
            String errMsg = UtilProperties.getMessage(resource, "AccountingTaxNoAddressSpecified", locale);
            if (shippingAddress != null) {
                errMsg += UtilProperties.getMessage(resource, "AccountingTaxNoAddressSpecifiedDetails", UtilMisc.toMap("contactMechId", shippingAddress.getString("contactMechId"), "address1", shippingAddress.get("address1"), "postalCodeGeoId", shippingAddress.get("postalCodeGeoId"), "stateProvinceGeoId", shippingAddress.get("stateProvinceGeoId"), "countryGeoId", shippingAddress.get("countryGeoId")), locale);
                Debug.logError(errMsg, module);
            }
            return ServiceUtil.returnError(errMsg);
        }

        // without knowing the TaxAuthority parties, just find all TaxAuthories for the set of IDs...
        Set<GenericValue> taxAuthoritySet = new HashSet<GenericValue>();
        try {
            getTaxAuthorities(delegator, shippingAddress, taxAuthoritySet);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Data error getting tax settings: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "AccountingTaxSettingError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // Setup the return lists.
        List<GenericValue> orderAdjustments = new LinkedList<GenericValue>();
        List<List<GenericValue>> itemAdjustments = new LinkedList<List<GenericValue>>();

        // Loop through the products; get the taxCategory; and lookup each in the cache.
        for (int i = 0; i < itemProductList.size(); i++) {
            GenericValue product = itemProductList.get(i);
            BigDecimal itemAmount = itemAmountList.get(i);
            BigDecimal itemPrice = itemPriceList.get(i);
            BigDecimal itemQuantity = itemQuantityList != null ? itemQuantityList.get(i) : null;
            BigDecimal shippingAmount = itemShippingList != null ? itemShippingList.get(i) : null;
            List<GenericValue> taxList = null;
            if (shippingAddress != null) {
                taxList = getTaxAdjustments(delegator, product, productStore, payToPartyId, billToPartyId, taxAuthoritySet, itemPrice, itemQuantity, itemAmount, shippingAmount, ZERO_BASE);
            }
            // this is an add and not an addAll because we want a List of Lists of GenericValues, one List of Adjustments per item
            itemAdjustments.add(taxList);
        }
        if (orderShippingAmount != null && orderShippingAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<GenericValue> taxList = getTaxAdjustments(delegator, null, productStore, payToPartyId, billToPartyId, taxAuthoritySet, ZERO_BASE, ZERO_BASE, ZERO_BASE, orderShippingAmount, ZERO_BASE);
            orderAdjustments.addAll(taxList);
        }
        if (orderPromotionsAmount != null && orderPromotionsAmount.compareTo(BigDecimal.ZERO) != 0) {
            List<GenericValue> taxList = getTaxAdjustments(delegator, null, productStore, payToPartyId, billToPartyId, taxAuthoritySet, ZERO_BASE, ZERO_BASE, ZERO_BASE, ZERO_BASE, orderPromotionsAmount);
            orderAdjustments.addAll(taxList);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderAdjustments", orderAdjustments);
        result.put("itemAdjustments", itemAdjustments);

        return result;
    }

    private static void getTaxAuthorities(Delegator delegator, GenericValue shippingAddress, Set<GenericValue> taxAuthoritySet) throws GenericEntityException {
        Map<String, String> geoIdByTypeMap = new HashMap<String, String>();
        if (shippingAddress != null) {
            if (UtilValidate.isNotEmpty(shippingAddress.getString("countryGeoId"))) {
                geoIdByTypeMap.put("COUNTRY", shippingAddress.getString("countryGeoId"));
            }
            if (UtilValidate.isNotEmpty(shippingAddress.getString("stateProvinceGeoId"))) {
                geoIdByTypeMap.put("STATE", shippingAddress.getString("stateProvinceGeoId"));
            }
            if (UtilValidate.isNotEmpty(shippingAddress.getString("countyGeoId"))) {
                geoIdByTypeMap.put("COUNTY", shippingAddress.getString("countyGeoId"));
            }
            String postalCodeGeoId = ContactMechWorker.getPostalAddressPostalCodeGeoId(shippingAddress, delegator);
            if (UtilValidate.isNotEmpty(postalCodeGeoId)) {
                geoIdByTypeMap.put("POSTAL_CODE", postalCodeGeoId);
            }
        } else {
            Debug.logWarning("shippingAddress was null, adding nothing to taxAuthoritySet", module);
        }

        //Debug.logInfo("Tax calc geoIdByTypeMap before expand:" + geoIdByTypeMap + "; this is for shippingAddress=" + shippingAddress, module);
        // get the most granular, or all available, geoIds and then find parents by GeoAssoc with geoAssocTypeId="REGIONS" and geoIdTo=<granular geoId> and find the GeoAssoc.geoId
        geoIdByTypeMap = GeoWorker.expandGeoRegionDeep(geoIdByTypeMap, delegator);
        //Debug.logInfo("Tax calc geoIdByTypeMap after expand:" + geoIdByTypeMap, module);

        List<GenericValue> taxAuthorityRawList = EntityQuery.use(delegator)
                .from("TaxAuthority").where(EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.IN, geoIdByTypeMap.values())).cache().queryList();
        taxAuthoritySet.addAll(taxAuthorityRawList);
        //Debug.logInfo("Tax calc taxAuthoritySet after expand:" + taxAuthoritySet, module);
    }

    private static List<GenericValue> getTaxAdjustments(Delegator delegator, GenericValue product, GenericValue productStore, 
            String payToPartyId, String billToPartyId, Set<GenericValue> taxAuthoritySet, 
            BigDecimal itemPrice, BigDecimal itemQuantity, BigDecimal itemAmount, 
            BigDecimal shippingAmount, BigDecimal orderPromotionsAmount) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List<GenericValue> adjustments = new LinkedList<GenericValue>();

        if (payToPartyId == null) {
            if (productStore != null) {
                payToPartyId = productStore.getString("payToPartyId");
            }
        }

        // store expr
        EntityCondition storeCond = null;
        if (productStore != null) {
            storeCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStore.get("productStoreId")),
                    EntityOperator.OR,
                    EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, null));
        } else {
            storeCond = EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, null);
        }

        // build the TaxAuthority expressions (taxAuthGeoId, taxAuthPartyId)
        List<EntityCondition> taxAuthCondOrList = new LinkedList<EntityCondition>();
        // start with the _NA_ TaxAuthority...
        taxAuthCondOrList.add(EntityCondition.makeCondition(
                EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, "_NA_"),
                EntityOperator.AND,
                EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, "_NA_")));

        for (GenericValue taxAuthority : taxAuthoritySet) {
            EntityCondition taxAuthCond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, taxAuthority.getString("taxAuthPartyId")),
                    EntityOperator.AND,
                    EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, taxAuthority.getString("taxAuthGeoId")));
            taxAuthCondOrList.add(taxAuthCond);
        }
        EntityCondition taxAuthoritiesCond = EntityCondition.makeCondition(taxAuthCondOrList, EntityOperator.OR);

        try {
            EntityCondition productCategoryCond = null;
            if (product != null) {
                // find the tax categories associated with the product and filter by those, with an IN clause or some such
                // if this product is variant, find the virtual product id and consider also the categories of the virtual
                // question: get all categories, or just a special type? for now let's do all categories...
                String virtualProductId = null;
                if ("Y".equals(product.getString("isVariant"))) {
                    virtualProductId = ProductWorker.getVariantVirtualId(product);
                }
                Set<String> productCategoryIdSet = new HashSet<String>();
                EntityCondition productIdCond = null;
                if (virtualProductId != null) {
                    productIdCond = EntityCondition.makeCondition(
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.getString("productId")),
                            EntityOperator.OR,
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, virtualProductId));

                } else {
                    productIdCond = EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.getString("productId"));
                }
                List<GenericValue> pcmList = EntityQuery.use(delegator).select("productCategoryId", "fromDate", "thruDate")
                        .from("ProductCategoryMember")
                        .where(productIdCond).cache().filterByDate().queryList();
                for (GenericValue pcm : pcmList) {
                    productCategoryIdSet.add(pcm.getString("productCategoryId"));
                }

                if (productCategoryIdSet.size() == 0) {
                    productCategoryCond = EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, null);
                } else {
                    productCategoryCond = EntityCondition.makeCondition(
                            EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, null),
                            EntityOperator.OR,
                            EntityCondition.makeCondition("productCategoryId", EntityOperator.IN, productCategoryIdSet));
                }
            } else {
                productCategoryCond = EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, null);
            }

            // FIXME handles shipping and promo tax. Simple solution, see https://issues.apache.org/jira/browse/OFBIZ-4160 for a better one 
            if (product == null && shippingAmount != null) {
                EntityCondition taxShippingCond = EntityCondition.makeCondition(
                            EntityCondition.makeCondition("taxShipping", EntityOperator.EQUALS, null),
                            EntityOperator.OR,
                            EntityCondition.makeCondition("taxShipping", EntityOperator.EQUALS, "Y"));

                if (productCategoryCond != null) {
                    productCategoryCond = EntityCondition.makeCondition(productCategoryCond,
                            EntityOperator.OR,
                            taxShippingCond);
                }
            }

            if (product == null && orderPromotionsAmount != null) {
                EntityCondition taxOrderPromotionsCond = EntityCondition.makeCondition(
                            EntityCondition.makeCondition("taxPromotions", EntityOperator.EQUALS, null),
                            EntityOperator.OR,
                            EntityCondition.makeCondition("taxPromotions", EntityOperator.EQUALS, "Y"));

                if (productCategoryCond != null) {
                    productCategoryCond = EntityCondition.makeCondition(productCategoryCond,
                            EntityOperator.OR,
                            taxOrderPromotionsCond);
                }
            }

            // build the main condition clause
            List<EntityCondition> mainExprs = UtilMisc.toList(storeCond, taxAuthoritiesCond, productCategoryCond);
            mainExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("minItemPrice", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("minItemPrice", EntityOperator.LESS_THAN_EQUAL_TO, itemPrice)));
            mainExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("minPurchase", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("minPurchase", EntityOperator.LESS_THAN_EQUAL_TO, itemAmount)));
            EntityCondition mainCondition = EntityCondition.makeCondition(mainExprs, EntityOperator.AND);

            // finally ready... do the rate query
            List<GenericValue> lookupList = EntityQuery.use(delegator).from("TaxAuthorityRateProduct")
                    .where(mainCondition).orderBy("minItemPrice", "minPurchase", "fromDate").filterByDate().queryList();

            if (lookupList.size() == 0) {
                Debug.logWarning("In TaxAuthority Product Rate no records were found for condition:" + mainCondition.toString(), module);
                return adjustments;
            }

            // find the right entry(s) based on purchase amount
            for (GenericValue taxAuthorityRateProduct : lookupList) {
                BigDecimal taxRate = taxAuthorityRateProduct.get("taxPercentage") != null ? taxAuthorityRateProduct.getBigDecimal("taxPercentage") : ZERO_BASE;
                BigDecimal taxable = ZERO_BASE;

                if (product != null && (product.get("taxable") == null || (product.get("taxable") != null && product.getBoolean("taxable").booleanValue()))) {
                    taxable = taxable.add(itemAmount);
                }
                if (shippingAmount != null && taxAuthorityRateProduct != null && (taxAuthorityRateProduct.get("taxShipping") == null || (taxAuthorityRateProduct.get("taxShipping") != null && taxAuthorityRateProduct.getBoolean("taxShipping").booleanValue()))) {
                    taxable = taxable.add(shippingAmount);
                }
                if (orderPromotionsAmount != null && taxAuthorityRateProduct != null && (taxAuthorityRateProduct.get("taxPromotions") == null || (taxAuthorityRateProduct.get("taxPromotions") != null && taxAuthorityRateProduct.getBoolean("taxPromotions").booleanValue()))) {
                    taxable = taxable.add(orderPromotionsAmount);
                }

                if (taxable.compareTo(BigDecimal.ZERO) == 0) {
                    // this should make it less confusing if the taxable flag on the product is not Y/true, and there is no shipping and such
                    continue;
                }

                // taxRate is in percentage, so needs to be divided by 100
                BigDecimal taxAmount = (taxable.multiply(taxRate)).divide(PERCENT_SCALE, salestaxCalcDecimals, salestaxRounding);

                String taxAuthGeoId = taxAuthorityRateProduct.getString("taxAuthGeoId");
                String taxAuthPartyId = taxAuthorityRateProduct.getString("taxAuthPartyId");

                // get glAccountId from TaxAuthorityGlAccount entity using the payToPartyId as the organizationPartyId
                GenericValue taxAuthorityGlAccount = EntityQuery.use(delegator).from("TaxAuthorityGlAccount")
                        .where("taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, "organizationPartyId", payToPartyId).queryOne();
                String taxAuthGlAccountId = null;
                if (taxAuthorityGlAccount != null) {
                    taxAuthGlAccountId = taxAuthorityGlAccount.getString("glAccountId");
                } else {
                    // TODO: what to do if no TaxAuthorityGlAccount found? Use some default, or is that done elsewhere later on?
                }

                GenericValue productPrice = null;
                if (product != null && taxAuthPartyId != null && taxAuthGeoId != null) {
                    // find a ProductPrice for the productId and taxAuth* values, and see if it has a priceWithTax value
                    productPrice = EntityQuery.use(delegator).from("ProductPrice")
                            .where("productId", product.get("productId"), 
                                    "taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, 
                                    "productPricePurposeId", "PURCHASE")
                            .orderBy("-fromDate").filterByDate().queryFirst();
                    
                    
                    if (productPrice == null) {
                    	GenericValue virtualProduct = ProductWorker.getParentProduct(product.getString("productId"), delegator); 
                    	if (virtualProduct != null) {
                    		productPrice = EntityQuery.use(delegator).from("ProductPrice")
                                    .where("productId", virtualProduct.get("productId"), 
                                            "taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, 
                                            "productPricePurposeId", "PURCHASE")
                                    .orderBy("-fromDate").filterByDate().queryFirst();
                    	}
                    }
                    //Debug.logInfo("=================== productId=" + product.getString("productId"), module);
                    //Debug.logInfo("=================== productPrice=" + productPrice, module);
                }
                GenericValue taxAdjValue = delegator.makeValue("OrderAdjustment");

                if (productPrice != null && "Y".equals(productPrice.getString("taxInPrice"))) {
                    // tax is in the price already, so we want the adjustment to be a VAT_TAX adjustment to be subtracted instead of a SALES_TAX adjustment to be added
                    taxAdjValue.set("orderAdjustmentTypeId", "VAT_TAX");

                    // the amount will be different because we want to figure out how much of the price was tax, and not how much tax needs to be added
                    // the formula is: taxAmount = priceWithTax - (priceWithTax/(1+taxPercentage/100))
                    BigDecimal taxAmountIncluded = itemAmount.subtract(itemAmount.divide(BigDecimal.ONE.add(taxRate.divide(PERCENT_SCALE, 4, BigDecimal.ROUND_HALF_UP)), 3, BigDecimal.ROUND_HALF_UP));
                    taxAdjValue.set("amountAlreadyIncluded", taxAmountIncluded);
                    taxAdjValue.set("amount", BigDecimal.ZERO);
                } else {
                    taxAdjValue.set("orderAdjustmentTypeId", "SALES_TAX");
                    taxAdjValue.set("amount", taxAmount);
                }
                
                taxAdjValue.set("sourcePercentage", taxRate);
                taxAdjValue.set("taxAuthorityRateSeqId", taxAuthorityRateProduct.getString("taxAuthorityRateSeqId"));
                // the primary Geo should be the main jurisdiction that the tax is for, and the secondary would just be to define a parent or wrapping jurisdiction of the primary
                taxAdjValue.set("primaryGeoId", taxAuthGeoId);
                taxAdjValue.set("comments", taxAuthorityRateProduct.getString("description"));
                if (taxAuthPartyId != null) taxAdjValue.set("taxAuthPartyId", taxAuthPartyId);
                if (taxAuthGlAccountId != null) taxAdjValue.set("overrideGlAccountId", taxAuthGlAccountId);
                if (taxAuthGeoId != null) taxAdjValue.set("taxAuthGeoId", taxAuthGeoId);

                // check to see if this party has a tax ID for this, and if the party is tax exempt in the primary (most-local) jurisdiction
                if (UtilValidate.isNotEmpty(billToPartyId) && UtilValidate.isNotEmpty(taxAuthGeoId)) {
                    // see if partyId is a member of any groups, if so honor their tax exemptions
                    // look for PartyRelationship with partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is the group member, so the partyIdFrom is the groupPartyId
                    Set<String> billToPartyIdSet = new HashSet<String>();
                    billToPartyIdSet.add(billToPartyId);
                    List<GenericValue> partyRelationshipList = EntityQuery.use(delegator).from("PartyRelationship")
                            .where("partyIdTo", billToPartyId, "partyRelationshipTypeId", "GROUP_ROLLUP")
                            .cache().filterByDate().queryList();

                    for (GenericValue partyRelationship : partyRelationshipList) {
                        billToPartyIdSet.add(partyRelationship.getString("partyIdFrom"));
                    }
                    handlePartyTaxExempt(taxAdjValue, billToPartyIdSet, taxAuthGeoId, taxAuthPartyId, taxAmount, nowTimestamp, delegator);
                } else {
                    Debug.logInfo("NOTE: A tax calculation was done without a billToPartyId or taxAuthGeoId, so no tax exemptions or tax IDs considered; billToPartyId=[" + billToPartyId + "] taxAuthGeoId=[" + taxAuthGeoId + "]", module);
                }

                adjustments.add(taxAdjValue);

                if (productPrice != null && itemQuantity != null && 
                        productPrice.getBigDecimal("priceWithTax") != null && 
                        !"Y".equals(productPrice.getString("taxInPrice"))) {
                    BigDecimal priceWithTax = productPrice.getBigDecimal("priceWithTax");
                    BigDecimal price = productPrice.getBigDecimal("price");
                    BigDecimal baseSubtotal = price.multiply(itemQuantity);
                    BigDecimal baseTaxAmount = (baseSubtotal.multiply(taxRate)).divide(PERCENT_SCALE, salestaxCalcDecimals, salestaxRounding);
                    //Debug.logInfo("=================== priceWithTax=" + priceWithTax, module);
                    //Debug.logInfo("=================== enteredTotalPriceWithTax=" + enteredTotalPriceWithTax, module);
                    //Debug.logInfo("=================== calcedTotalPriceWithTax=" + calcedTotalPriceWithTax, module);
                    
                    // tax is not already in price so we want to add it in, but this is a VAT situation so adjust to make it as accurate as possible

                    // for VAT taxes if the calculated total item price plus calculated taxes is different from what would be 
                    // expected based on the original entered price with taxes (if the price was entered this way), then create
                    // an adjustment that corrects for the difference, and this correction will be effectively subtracted from the 
                    // price and not from the tax (the tax is meant to be calculated based on Tax Authority rules and so should
                    // not be shorted)
                    
                    // TODO (don't think this is needed, but just to keep it in mind): get this to work with multiple VAT tax authorities instead of just one (right now will get incorrect totals if there are multiple taxes included in the price)
                    // TODO add constraint to ProductPrice lookup by any productStoreGroupId associated with the current productStore
                    
                    BigDecimal enteredTotalPriceWithTax = priceWithTax.multiply(itemQuantity);
                    BigDecimal calcedTotalPriceWithTax = (baseSubtotal).add(baseTaxAmount);
                    if (!enteredTotalPriceWithTax.equals(calcedTotalPriceWithTax)) {
                        // if the calculated amount is higher than the entered amount we want the value to be negative 
                        //     to get it down to match the entered amount
                        // so, subtract the calculated amount from the entered amount (ie: correction = entered - calculated)
                        BigDecimal correctionAmount = enteredTotalPriceWithTax.subtract(calcedTotalPriceWithTax);
                        //Debug.logInfo("=================== correctionAmount=" + correctionAmount, module);
                        
                        GenericValue correctionAdjValue = delegator.makeValue("OrderAdjustment");
                        correctionAdjValue.set("taxAuthorityRateSeqId", taxAuthorityRateProduct.getString("taxAuthorityRateSeqId"));
                        correctionAdjValue.set("amount", correctionAmount);
                        // don't set this, causes a doubling of the tax rate because calling code adds up all tax rates: correctionAdjValue.set("sourcePercentage", taxRate);
                        correctionAdjValue.set("orderAdjustmentTypeId", "VAT_PRICE_CORRECT");
                        // the primary Geo should be the main jurisdiction that the tax is for, and the secondary would just be to define a parent or wrapping jurisdiction of the primary
                        correctionAdjValue.set("primaryGeoId", taxAuthGeoId);
                        correctionAdjValue.set("comments", taxAuthorityRateProduct.getString("description"));
                        if (taxAuthPartyId != null) correctionAdjValue.set("taxAuthPartyId", taxAuthPartyId);
                        if (taxAuthGlAccountId != null) correctionAdjValue.set("overrideGlAccountId", taxAuthGlAccountId);
                        if (taxAuthGeoId != null) correctionAdjValue.set("taxAuthGeoId", taxAuthGeoId);
                        adjustments.add(correctionAdjValue);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up tax rates", module);
            return new LinkedList<GenericValue>();
        }

        return adjustments;
    }

    private static void handlePartyTaxExempt(GenericValue adjValue, Set<String> billToPartyIdSet, String taxAuthGeoId, String taxAuthPartyId, BigDecimal taxAmount, Timestamp nowTimestamp, Delegator delegator) throws GenericEntityException {
        Debug.logInfo("Checking for tax exemption : " + taxAuthGeoId + " / " + taxAuthPartyId, module);
        List<EntityCondition> ptiConditionList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("partyId", EntityOperator.IN, billToPartyIdSet),
                EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, taxAuthGeoId),
                EntityCondition.makeCondition("taxAuthPartyId", EntityOperator.EQUALS, taxAuthPartyId));
        ptiConditionList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
        ptiConditionList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
        EntityCondition ptiCondition = EntityCondition.makeCondition(ptiConditionList, EntityOperator.AND);
        // sort by -fromDate to get the newest (largest) first, just in case there is more than one, we only want the most recent valid one, should only be one per jurisdiction...
        GenericValue partyTaxInfo = EntityQuery.use(delegator).from("PartyTaxAuthInfo").where(ptiCondition).orderBy("-fromDate").queryFirst();

        boolean foundExemption = false;
        if (partyTaxInfo != null) {
            adjValue.set("customerReferenceId", partyTaxInfo.get("partyTaxId"));
            if ("Y".equals(partyTaxInfo.getString("isExempt"))) {
                adjValue.set("amount", BigDecimal.ZERO);
                adjValue.set("exemptAmount", taxAmount);
                foundExemption = true;
            }
        }

        // if no exceptions were found for the current; try the parent
        if (!foundExemption) {
            // try the "parent" TaxAuthority
            GenericValue taxAuthorityAssoc = EntityQuery.use(delegator).from("TaxAuthorityAssoc")
                    .where("toTaxAuthGeoId", taxAuthGeoId, "toTaxAuthPartyId", taxAuthPartyId, "taxAuthorityAssocTypeId", "EXEMPT_INHER")
                    .orderBy("-fromDate").filterByDate().queryFirst();
            // Debug.logInfo("Parent assoc to " + taxAuthGeoId + " : " + taxAuthorityAssoc, module);
            if (taxAuthorityAssoc != null) {
                handlePartyTaxExempt(adjValue, billToPartyIdSet, taxAuthorityAssoc.getString("taxAuthGeoId"), taxAuthorityAssoc.getString("taxAuthPartyId"), taxAmount, nowTimestamp, delegator);
            }
        }
    }
}
