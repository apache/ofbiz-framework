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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.common.geo.GeoWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * Tax Authority tax calculation and other misc services
 */

public class TaxAuthorityServices {

    public static final String module = TaxAuthorityServices.class.getName();
    public static final BigDecimal ZERO_BASE = new BigDecimal("0.000"); 
    public static final BigDecimal ONE_BASE = new BigDecimal("1.000"); 
    public static final BigDecimal PERCENT_SCALE = new BigDecimal("100.000"); 

    public static Map rateProductTaxCalcForDisplay(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String billToPartyId = (String) context.get("billToPartyId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal basePrice = (BigDecimal) context.get("basePrice");
        BigDecimal shippingPrice = (BigDecimal) context.get("shippingPrice");

        if (quantity == null) quantity = ONE_BASE;
        BigDecimal amount = basePrice.multiply(quantity);
        int salestaxFinalDecimals=UtilNumber.getBigDecimalScale("salestax.final.decimals");
        int salestaxCalcDecimals=UtilNumber.getBigDecimalScale("salestax.calc.decimals");
        int salestaxRounding=UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
        
        BigDecimal taxTotal = ZERO_BASE;
        BigDecimal taxPercentage = ZERO_BASE;
        BigDecimal priceWithTax = basePrice;
        if (shippingPrice != null) priceWithTax = priceWithTax.add(shippingPrice); 

        try {
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            GenericValue productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            if (productStore == null) {
                throw new IllegalArgumentException("Could not find ProductStore with ID [" + productStoreId + "] for tax calculation");
            }
            
            if ("Y".equals(productStore.getString("showPricesWithVatTax"))) {
                Set taxAuthoritySet = FastSet.newInstance();
                if (productStore.get("vatTaxAuthPartyId") == null) {
                    List taxAuthorityRawList = delegator.findByConditionCache("TaxAuthority", new EntityExpr("taxAuthGeoId", EntityOperator.EQUALS, productStore.get("vatTaxAuthGeoId")), null, null);
                    taxAuthoritySet.addAll(taxAuthorityRawList);
                } else {
                    GenericValue taxAuthority = delegator.findByPrimaryKeyCache("TaxAuthority", UtilMisc.toMap("taxAuthGeoId", productStore.get("vatTaxAuthGeoId"), "taxAuthPartyId", productStore.get("vatTaxAuthPartyId")));
                    taxAuthoritySet.add(taxAuthority);
                }
                
                if (taxAuthoritySet.size() == 0) {
                    throw new IllegalArgumentException("Could not find any Tax Authories for store with ID [" + productStoreId + "] for tax calculation; the store settings may need to be corrected.");
                }
                
                List taxAdustmentList = getTaxAdjustments(delegator, product, productStore, null, billToPartyId, taxAuthoritySet, basePrice, amount, shippingPrice);
                if (taxAdustmentList.size() == 0) {
                    // this is something that happens every so often for different products and such, so don't blow up on it...
                    Debug.logWarning("Could not find any Tax Authories Rate Rules for store with ID [" + productStoreId + "], productId [" + productId + "], basePrice [" + basePrice + "], amount [" + amount + "], for tax calculation; the store settings may need to be corrected.", module);
                }

                // add up amounts from adjustments (amount OR exemptAmount, sourcePercentage)
                Iterator taxAdustmentIter = taxAdustmentList.iterator();
                while (taxAdustmentIter.hasNext()) {
                    GenericValue taxAdjustment = (GenericValue) taxAdustmentIter.next();
                    taxPercentage = taxPercentage.add(taxAdjustment.getBigDecimal("sourcePercentage"));
                    BigDecimal adjAmount = taxAdjustment.getBigDecimal("amount");
                    taxTotal = taxTotal.add(adjAmount);
                    priceWithTax = priceWithTax.add(adjAmount.divide(quantity,salestaxCalcDecimals,salestaxRounding));
                    Debug.logInfo("For productId [" + productId + "] added [" + adjAmount.divide(quantity,salestaxCalcDecimals,salestaxRounding) + "] of tax to price for geoId [" + taxAdjustment.getString("taxAuthGeoId") + "], new price is [" + priceWithTax + "]", module);
                }
            }
        } catch (GenericEntityException e) {
            String errMsg = "Data error getting tax settings: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        // round to 2 decimal places for display/etc
        taxTotal = taxTotal.setScale(salestaxFinalDecimals, salestaxRounding);
        priceWithTax = priceWithTax.setScale(salestaxFinalDecimals, salestaxRounding);
        
        Map result = ServiceUtil.returnSuccess();
        result.put("taxTotal", taxTotal);
        result.put("taxPercentage", taxPercentage);
        result.put("priceWithTax", priceWithTax);
        return result;
    }

    public static Map rateProductTaxCalc(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String payToPartyId = (String) context.get("payToPartyId");
        String billToPartyId = (String) context.get("billToPartyId");
        List itemProductList = (List) context.get("itemProductList");
        List itemAmountList = (List) context.get("itemAmountList");
        List itemPriceList = (List) context.get("itemPriceList");
        List itemShippingList = (List) context.get("itemShippingList");
        BigDecimal orderShippingAmount = (BigDecimal) context.get("orderShippingAmount");
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");

        if (shippingAddress == null || (shippingAddress.get("countryGeoId") == null && shippingAddress.get("stateProvinceGeoId") == null)) {
            return ServiceUtil.returnError("The address(es) used for tax calculation did not have State/Province or Country values set, so we cannot determine the taxes to charge.");
        }
        // without knowing the TaxAuthority parties, just find all TaxAuthories for the set of IDs...
        Set taxAuthoritySet = FastSet.newInstance();
        GenericValue productStore = null;
        try {
            getTaxAuthorities(delegator, shippingAddress, taxAuthoritySet);
            if (productStoreId != null) {
                productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            }
        } catch (GenericEntityException e) {
            String errMsg = "Data error getting tax settings: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        if (productStore == null && payToPartyId == null) {
            throw new IllegalArgumentException("Could not find payToPartyId [" + payToPartyId + "] or ProductStore [" + productStoreId + "] for tax calculation");
        }
        
        // Setup the return lists.
        List orderAdjustments = FastList.newInstance();
        List itemAdjustments = FastList.newInstance();

        // Loop through the products; get the taxCategory; and lookup each in the cache.
        for (int i = 0; i < itemProductList.size(); i++) {
            GenericValue product = (GenericValue) itemProductList.get(i);
            BigDecimal itemAmount = (BigDecimal) itemAmountList.get(i);
            BigDecimal itemPrice = (BigDecimal) itemPriceList.get(i);
            BigDecimal shippingAmount = (BigDecimal) itemShippingList.get(i);
            List taxList = null;
            if (shippingAddress != null) {
                taxList = getTaxAdjustments(delegator, product, productStore, payToPartyId, billToPartyId, taxAuthoritySet, itemPrice, itemAmount, shippingAmount);
            }
            // this is an add and not an addAll because we want a List of Lists of GenericValues, one List of Adjustments per item
            itemAdjustments.add(taxList);
        }
        if (orderShippingAmount.doubleValue() > 0) {
            List taxList = getTaxAdjustments(delegator, null, productStore, payToPartyId, billToPartyId, taxAuthoritySet, ZERO_BASE, ZERO_BASE, orderShippingAmount);
            orderAdjustments.addAll(taxList);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("orderAdjustments", orderAdjustments);
        result.put("itemAdjustments", itemAdjustments);

        return result;
    }

    private static void getTaxAuthorities(GenericDelegator delegator, GenericValue shippingAddress, Set taxAuthoritySet) throws GenericEntityException {
        Set geoIdSet = FastSet.newInstance();
        if (shippingAddress != null) {
            if (shippingAddress.getString("countryGeoId") != null) {
                geoIdSet.add(shippingAddress.getString("countryGeoId"));
            }
            if (shippingAddress.getString("stateProvinceGeoId") != null) {
                geoIdSet.add(shippingAddress.getString("stateProvinceGeoId"));
            }
            if (shippingAddress.getString("countyGeoId") != null) {
                geoIdSet.add(shippingAddress.getString("countyGeoId"));
            }
        }
        // get the most granular, or all available, geoIds and then find parents by GeoAssoc with geoAssocTypeId="REGIONS" and geoIdTo=<granular geoId> and find the GeoAssoc.geoId
        geoIdSet = GeoWorker.expandGeoRegionDeep(geoIdSet, delegator);

        List taxAuthorityRawList = delegator.findByConditionCache("TaxAuthority", new EntityExpr("taxAuthGeoId", EntityOperator.IN, geoIdSet), null, null);
        taxAuthoritySet.addAll(taxAuthorityRawList);
    }

    private static List getTaxAdjustments(GenericDelegator delegator, GenericValue product, GenericValue productStore, String payToPartyId, String billToPartyId, Set taxAuthoritySet, BigDecimal itemPrice, BigDecimal itemAmount, BigDecimal shippingAmount) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List adjustments = FastList.newInstance();

        if (payToPartyId == null) {
            if (productStore != null) {
                payToPartyId = productStore.getString("payToPartyId");
            }
        }

        // store expr
        EntityCondition storeCond = null;
        if (productStore != null) {
            storeCond = new EntityExpr(
                    new EntityExpr("productStoreId", EntityOperator.EQUALS, productStore.get("productStoreId")),
                    EntityOperator.OR,
                    new EntityExpr("productStoreId", EntityOperator.EQUALS, null));
        } else {
            storeCond = new EntityExpr("productStoreId", EntityOperator.EQUALS, null);
        }

        // build the TaxAuthority expressions (taxAuthGeoId, taxAuthPartyId)
        List taxAuthCondOrList = FastList.newInstance();
        // start with the _NA_ TaxAuthority...
        taxAuthCondOrList.add(new EntityExpr(
                new EntityExpr("taxAuthPartyId", EntityOperator.EQUALS, "_NA_"), 
                EntityOperator.AND, 
                new EntityExpr("taxAuthGeoId", EntityOperator.EQUALS, "_NA_")));
        
        Iterator taxAuthorityIter = taxAuthoritySet.iterator();
        while (taxAuthorityIter.hasNext()) {
            GenericValue taxAuthority = (GenericValue) taxAuthorityIter.next();
            EntityCondition taxAuthCond = new EntityExpr(
                    new EntityExpr("taxAuthPartyId", EntityOperator.EQUALS, taxAuthority.getString("taxAuthPartyId")), 
                    EntityOperator.AND, 
                    new EntityExpr("taxAuthGeoId", EntityOperator.EQUALS, taxAuthority.getString("taxAuthGeoId")));
            taxAuthCondOrList.add(taxAuthCond);
        }
        EntityCondition taxAuthoritiesCond = new EntityConditionList(taxAuthCondOrList, EntityOperator.OR);

        try {
            EntityCondition productCategoryCond = null;
            if (product != null) {
                // find the tax categories associated with the product and filter by those, with an IN clause or some such
                // question: get all categories, or just a special type? for now let's do all categories...
                Set productCategoryIdSet = FastSet.newInstance();
                List pcmList = delegator.findByAndCache("ProductCategoryMember", UtilMisc.toMap("productId", product.get("productId")));
                pcmList = EntityUtil.filterByDate(pcmList, true);
                Iterator pcmIter = pcmList.iterator();
                while (pcmIter.hasNext()) {
                    GenericValue pcm = (GenericValue) pcmIter.next();
                    productCategoryIdSet.add(pcm.get("productCategoryId"));
                }
                
                if (productCategoryIdSet.size() == 0) {
                    productCategoryCond = new EntityExpr("productCategoryId", EntityOperator.EQUALS, null);
                } else {
                    productCategoryCond = new EntityExpr(
                            new EntityExpr("productCategoryId", EntityOperator.EQUALS, null),
                            EntityOperator.OR,
                            new EntityExpr("productCategoryId", EntityOperator.IN, productCategoryIdSet));
                }
            } else {
                productCategoryCond = new EntityExpr("productCategoryId", EntityOperator.EQUALS, null);
            }

            // build the main condition clause
            List mainExprs = UtilMisc.toList(storeCond, taxAuthoritiesCond, productCategoryCond);
            mainExprs.add(new EntityExpr(new EntityExpr("minItemPrice", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("minItemPrice", EntityOperator.LESS_THAN_EQUAL_TO, itemPrice)));
            mainExprs.add(new EntityExpr(new EntityExpr("minPurchase", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("minPurchase", EntityOperator.LESS_THAN_EQUAL_TO, itemAmount)));
            EntityCondition mainCondition = new EntityConditionList(mainExprs, EntityOperator.AND);

            // create the orderby clause
            List orderList = UtilMisc.toList("minItemPrice", "minPurchase", "fromDate");

            // finally ready... do the rate query
            List lookupList = delegator.findByCondition("TaxAuthorityRateProduct", mainCondition, null, orderList);
            List filteredList = EntityUtil.filterByDate(lookupList, true);

            if (filteredList.size() == 0) {
                Debug.logWarning("In TaxAuthority Product Rate no records were found for condition:" + mainCondition.toString(), module);
                return adjustments;
            }

            // find the right entry(s) based on purchase amount
            Iterator flIt = filteredList.iterator();
            while (flIt.hasNext()) {
                GenericValue taxAuthorityRateProduct = (GenericValue) flIt.next();

                BigDecimal taxRate = taxAuthorityRateProduct.get("taxPercentage") != null ? taxAuthorityRateProduct.getBigDecimal("taxPercentage") : ZERO_BASE;
                BigDecimal taxable = ZERO_BASE;

                if (product != null && (product.get("taxable") == null || (product.get("taxable") != null && product.getBoolean("taxable").booleanValue()))) {
                    taxable = taxable.add(itemAmount);
                }
                if (shippingAmount != null && taxAuthorityRateProduct != null && (taxAuthorityRateProduct.get("taxShipping") == null || (taxAuthorityRateProduct.get("taxShipping") != null && taxAuthorityRateProduct.getBoolean("taxShipping").booleanValue()))) {
                    taxable = taxable.add(shippingAmount);
                }
                
                if (taxable.doubleValue() == 0) {
                    // this should make it less confusing if the taxable flag on the product is not Y/true, and there is no shipping and such
                    continue;
                }

                // taxRate is in percentage, so needs to be divided by 100
                BigDecimal taxAmount = (taxable.multiply(taxRate)).divide(PERCENT_SCALE, 3, BigDecimal.ROUND_CEILING);

                String taxAuthGeoId = taxAuthorityRateProduct.getString("taxAuthGeoId");
                String taxAuthPartyId = taxAuthorityRateProduct.getString("taxAuthPartyId");

                // get glAccountId from TaxAuthorityGlAccount entity using the payToPartyId as the organizationPartyId
                GenericValue taxAuthorityGlAccount = delegator.findByPrimaryKey("TaxAuthorityGlAccount", UtilMisc.toMap("taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, "organizationPartyId", payToPartyId));
                String taxAuthGlAccountId = null;
                if (taxAuthorityGlAccount != null) {
                    taxAuthGlAccountId = taxAuthorityGlAccount.getString("glAccountId");
                } else {
                    // TODO: what to do if no TaxAuthorityGlAccount found? Use some default, or is that done elsewhere later on?
                }

                GenericValue adjValue = delegator.makeValue("OrderAdjustment", null);
                adjValue.set("taxAuthorityRateSeqId", taxAuthorityRateProduct.getString("taxAuthorityRateSeqId"));
                adjValue.set("amount", taxAmount);
                adjValue.set("sourcePercentage", taxRate);
                adjValue.set("orderAdjustmentTypeId", "SALES_TAX");
                // the primary Geo should be the main jurisdiction that the tax is for, and the secondary would just be to define a parent or wrapping jurisdiction of the primary
                adjValue.set("primaryGeoId", taxAuthGeoId);
                adjValue.set("comments", taxAuthorityRateProduct.getString("description"));
                if (taxAuthPartyId != null) adjValue.set("taxAuthPartyId", taxAuthPartyId);
                if (taxAuthGlAccountId != null) adjValue.set("overrideGlAccountId", taxAuthGlAccountId);
                if (taxAuthGeoId != null) adjValue.set("taxAuthGeoId", taxAuthGeoId);

                // check to see if this party has a tax ID for this, and if the party is tax exempt in the primary (most-local) jurisdiction
                if (UtilValidate.isNotEmpty(billToPartyId) && taxAuthGeoId != null) {
                    // see if partyId is a member of any groups , if so honor their tax exemptions
                    // look for PartyRelationship with partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is the group member, so the partyIdFrom is the groupPartyId
                    Set billToPartyIdSet = FastSet.newInstance();
                    billToPartyIdSet.add(billToPartyId);
                    List partyRelationshipList = EntityUtil.filterByDate(delegator.findByAndCache("PartyRelationship", UtilMisc.toMap("partyIdTo", billToPartyId, "partyRelationshipTypeId", "GROUP_ROLLUP")), true);
                    Iterator partyRelationshipIter = partyRelationshipList.iterator();
                    while (partyRelationshipIter.hasNext()) {
                        GenericValue partyRelationship = (GenericValue) partyRelationshipIter.next();
                        billToPartyIdSet.add(partyRelationship.get("partyIdFrom"));
                    }

                    handlePartyTaxExempt(adjValue, billToPartyIdSet, taxAuthGeoId, taxAuthPartyId, taxAmount, nowTimestamp, delegator);
                } else {
                    Debug.logInfo("NOTE: A tax calculation was done without a billToPartyId or taxAuthGeoId, so no tax exemptions or tax IDs considered; billToPartyId=[" + billToPartyId + "] taxAuthGeoId=[" + taxAuthGeoId + "]", module);
                }

                adjustments.add(adjValue);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up tax rates", module);
            return new ArrayList();
        }

        return adjustments;
    }
    
    private static void handlePartyTaxExempt(GenericValue adjValue, Set billToPartyIdSet, String taxAuthGeoId, String taxAuthPartyId, BigDecimal taxAmount, Timestamp nowTimestamp, GenericDelegator delegator) throws GenericEntityException {
        List ptiConditionList = UtilMisc.toList(
                new EntityExpr("partyId", EntityOperator.IN, billToPartyIdSet),
                new EntityExpr("taxAuthGeoId", EntityOperator.EQUALS, taxAuthGeoId),
                new EntityExpr("taxAuthPartyId", EntityOperator.EQUALS, taxAuthPartyId));
        ptiConditionList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
        ptiConditionList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
        EntityCondition ptiCondition = new EntityConditionList(ptiConditionList, EntityOperator.AND);
        // sort by -fromDate to get the newest (largest) first, just in case there is more than one, we only want the most recent valid one, should only be one per jurisdiction...
        List partyTaxInfos = delegator.findByCondition("PartyTaxAuthInfo", ptiCondition, null, UtilMisc.toList("-fromDate"));
        if (partyTaxInfos.size() > 0) {
            GenericValue partyTaxInfo = (GenericValue) partyTaxInfos.get(0);
            adjValue.set("customerReferenceId", partyTaxInfo.get("partyTaxId"));
            if ("Y".equals(partyTaxInfo.getString("isExempt"))) {
                adjValue.set("amount", new Double(0));
                adjValue.set("exemptAmount", taxAmount);
            } else {
                // try the "parent" TaxAuthority
                List taxAuthorityAssocList = delegator.findByAndCache("TaxAuthorityAssoc", 
                        UtilMisc.toMap("toTaxAuthGeoId", taxAuthGeoId, "toTaxAuthPartyId", taxAuthPartyId, "taxAuthorityAssocTypeId", "EXEMPT_INHER"), 
                        UtilMisc.toList("-fromDate"));
                taxAuthorityAssocList = EntityUtil.filterByDate(taxAuthorityAssocList, true);
                GenericValue taxAuthorityAssoc = EntityUtil.getFirst(taxAuthorityAssocList);
                if (taxAuthorityAssoc != null) {
                    handlePartyTaxExempt(adjValue, billToPartyIdSet, taxAuthorityAssoc.getString("taxAuthGeoId"), taxAuthorityAssoc.getString("taxAuthPartyId"), taxAmount, nowTimestamp, delegator);
                }
            }
        }
    }
}
