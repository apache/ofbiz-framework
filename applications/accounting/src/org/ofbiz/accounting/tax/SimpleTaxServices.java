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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;

/**
 * Order Processing Services
 */

public class SimpleTaxServices {

    public static final String module = SimpleTaxServices.class.getName();

    /** Null tax calc service. */
    public static Map nullTaxCalc(DispatchContext dctx, Map context) {
        return UtilMisc.toMap("orderAdjustments", UtilMisc.toList(null), "itemAdjustments", UtilMisc.toList(null));
    }

    /** Simple tax calc service. */
    public static Map simpleTaxCalc(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String billToPartyId = (String) context.get("billToPartyId");
        List itemProductList = (List) context.get("itemProductList");
        List itemAmountList = (List) context.get("itemAmountList");
        List itemPriceList = (List) context.get("itemPriceList");
        List itemShippingList = (List) context.get("itemShippingList");
        Double orderShippingAmount = (Double) context.get("orderShippingAmount");
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");
        //GenericValue userLogin = (GenericValue) context.get("userLogin");
        //Locale locale = (Locale) context.get("locale");

        // Simple Tax Calc only uses the state from the address and the SalesTaxLookup entity.

        String countryCode = null;
        String stateCode = null;

        if (shippingAddress != null) {
            countryCode = shippingAddress.getString("countryGeoId");
            stateCode = shippingAddress.getString("stateProvinceGeoId");
        }

        // Setup the return lists.
        List orderAdjustments = new ArrayList();
        List itemAdjustments = new ArrayList();

        // Loop through the products; get the taxCategory; and lookup each in the cache.
        for (int i = 0; i < itemProductList.size(); i++) {
            GenericValue product = (GenericValue) itemProductList.get(i);
            Double itemAmount = (Double) itemAmountList.get(i);
            Double itemPrice = (Double) itemPriceList.get(i);
            Double shippingAmount = (Double) itemShippingList.get(i);
            List taxList = null;
            if (shippingAddress != null) {
                taxList = getTaxAmount(delegator, product, productStoreId, billToPartyId, countryCode, stateCode, itemPrice.doubleValue(), itemAmount.doubleValue(), shippingAmount.doubleValue());
            }
            itemAdjustments.add(taxList);
        }
        if (orderShippingAmount.doubleValue() > 0) {
            List taxList = getTaxAmount(delegator, null, productStoreId, billToPartyId, countryCode, stateCode, 0.00, 0.00, orderShippingAmount.doubleValue());
            orderAdjustments.addAll(taxList);
        }

        Map result = UtilMisc.toMap("orderAdjustments", orderAdjustments, "itemAdjustments", itemAdjustments);

        return result;
    }

    private static List getTaxAmount(GenericDelegator delegator, GenericValue item, String productStoreId, String billToPartyId, String countryCode, String stateCode, double itemPrice, double itemAmount, double shippingAmount) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List adjustments = new ArrayList();

        // store expr
        EntityCondition storeCond = new EntityExpr("productStoreId", EntityOperator.EQUALS, productStoreId);

        // build the country expressions
        List countryExprs = UtilMisc.toList(new EntityExpr("countryGeoId", EntityOperator.EQUALS, countryCode), new EntityExpr("countryGeoId", EntityOperator.EQUALS, "_NA_"));
        EntityCondition countryCond = new EntityConditionList(countryExprs, EntityOperator.OR);

        // build the state expression
        List stateExprs = UtilMisc.toList(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, stateCode), new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, "_NA_"));
        EntityCondition stateCond = new EntityConditionList(stateExprs, EntityOperator.OR);

        // build the tax cat expression
        List taxCatExprs = UtilMisc.toList(new EntityExpr("taxCategory", EntityOperator.EQUALS, "_NA_"));
        if (item != null && item.get("taxCategory") != null) {
            taxCatExprs.add(new EntityExpr("taxCategory", EntityOperator.EQUALS, item.getString("taxCategory")));
        }
        EntityCondition taxCatCond = new EntityConditionList(taxCatExprs, EntityOperator.OR);

        // build the main condition clause
        List mainExprs = UtilMisc.toList(storeCond, countryCond, stateCond);
        if (taxCatExprs.size() > 1) {
            mainExprs.add(taxCatCond);
        } else {
            mainExprs.add(taxCatExprs.get(0));
        }
        EntityCondition mainCondition = new EntityConditionList(mainExprs, EntityOperator.AND);

        // create the orderby clause
        List orderList = UtilMisc.toList("minItemPrice", "minPurchase", "fromDate");

        try {
            List lookupList = delegator.findByCondition("SimpleSalesTaxLookup", mainCondition, null, orderList);
            List filteredList = EntityUtil.filterByDate(lookupList);

            if (filteredList.size() == 0) {
                Debug.logWarning("SimpleTaxCalc: No State/TaxCategory pair found (with or without taxCat).", module);
                return adjustments;
            }

            // find the right entry(s) based on purchase amount
            Iterator flIt = filteredList.iterator();
            while (flIt.hasNext()) {
                GenericValue taxLookup = (GenericValue) flIt.next();
                double minPrice = taxLookup.get("minItemPrice") != null ? taxLookup.getDouble("minItemPrice").doubleValue() : 0.00;
                double minAmount = taxLookup.get("minPurchase") != null ? taxLookup.getDouble("minPurchase").doubleValue() : 0.00;

                // DEJ20050528 not sure why this is done like this, could put this condition in the query sent to the database, though perhaps it is this way because of issues with that of some sort?
                if (itemPrice >= minPrice && itemAmount >= minAmount) {
                    double taxRate = taxLookup.get("salesTaxPercentage") != null ? taxLookup.getDouble("salesTaxPercentage").doubleValue() : 0;
                    double taxable = 0.00;

                    if (item != null && (item.get("taxable") == null || (item.get("taxable") != null && item.getBoolean("taxable").booleanValue()))) {
                        taxable += itemAmount;
                    }
                    if (taxLookup != null && (taxLookup.get("taxShipping") == null || (taxLookup.get("taxShipping") != null && taxLookup.getBoolean("taxShipping").booleanValue()))) {
                        taxable += shippingAmount;
                    }

                    // TODO: DEJ20050528 this is an interesting way to round the number, according to the JavaDoc
                    //this uses the "ROUND_HALF_EVEN" method (as defined in the BigDecimal class, see JavaDoc
                    //of that for details); it seems we might want to use the ROUND_HALF_UP method...
                    String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
                    DecimalFormat formatter = new DecimalFormat(currencyFormat);
                    double taxTotal = taxable * taxRate;
                    String amountStr = formatter.format(taxTotal);
                    Double taxAmount = null;
                    try {
                        taxAmount = new Double(formatter.parse(amountStr).doubleValue());
                    } catch (ParseException e) {
                        throw new GeneralException("Problem getting parsed amount from string", e);
                    }

                    String primaryGeoId = taxLookup.getString("stateProvinceGeoId");
                    String secondaryGeoId = taxLookup.getString("countryGeoId");
                    String taxAuthPartyId = taxLookup.getString("taxAuthPartyId");
                    String taxAuthGlAccountId = taxLookup.getString("taxAuthGlAccountId");

                    // if no state/province, the country is the primary
                    if (primaryGeoId == null || "_NA_".equals(primaryGeoId)) {
                        primaryGeoId = secondaryGeoId;
                        secondaryGeoId = null;
                    }

                    Map adjMap = new HashMap();
                    adjMap.put("amount", taxAmount);
                    adjMap.put("sourcePercentage", new Double(taxRate));
                    adjMap.put("orderAdjustmentTypeId", "SALES_TAX");
                    // the primary Geo should be the main jurisdiction that the tax is for, and the secondary would just be to define a parent or wrapping jurisdiction of the primary
                    adjMap.put("primaryGeoId", primaryGeoId);
                    if (secondaryGeoId != null) adjMap.put("secondaryGeoId", secondaryGeoId);
                    adjMap.put("comments", taxLookup.getString("description"));
                    if (taxAuthPartyId != null) adjMap.put("taxAuthPartyId", taxAuthPartyId);
                    if (taxAuthGlAccountId != null) adjMap.put("overrideGlAccountId", taxAuthGlAccountId);
                    if (primaryGeoId != null) adjMap.put("taxAuthGeoId", primaryGeoId);

                    // check to see if this party has a tax ID for this, and if the party is tax exempt in the primary (most-local) jurisdiction
                    if (UtilValidate.isNotEmpty(billToPartyId) && primaryGeoId != null) {
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

                        List ptiConditionList = UtilMisc.toList(
                                new EntityExpr("partyId", EntityOperator.IN, billToPartyIdSet),
                                new EntityExpr("taxAuthGeoId", EntityOperator.EQUALS, primaryGeoId));
                        ptiConditionList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
                        ptiConditionList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
                        EntityCondition ptiCondition = new EntityConditionList(ptiConditionList, EntityOperator.AND);
                        // sort by -fromDate to get the newest (largest) first, just in case there is more than one, we only want the most recent valid one, should only be one per jurisdiction...
                        List partyTaxAuthInfos = delegator.findByCondition("PartyTaxAuthInfo", ptiCondition, null, UtilMisc.toList("-fromDate"));
                        if (partyTaxAuthInfos.size() > 0) {
                            GenericValue partyTaxAuthInfo = (GenericValue) partyTaxAuthInfos.get(0);
                            adjMap.put("customerReferenceId", partyTaxAuthInfo.get("partyTaxId"));
                            if ("Y".equals(partyTaxAuthInfo.getString("isExempt"))) {
                                adjMap.put("amount", new Double(0));
                                adjMap.put("exemptAmount", taxAmount);
                            }
                        }
                    } else {
                        Debug.logInfo("NOTE: A tax calculation was done without a billToPartyId or primaryGeoId, so no tax exemptions or tax IDs considered; billToPartyId=[" + billToPartyId + "] primaryGeoId=[" + primaryGeoId + "]", module);
                    }

                    adjustments.add(delegator.makeValue("OrderAdjustment", adjMap));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up tax rates", module);
            return new ArrayList();
        } catch (GeneralException e) {
            Debug.logError(e, "Problems looking up tax rates", module);
            return new ArrayList();
        }

        return adjustments;
    }
}
