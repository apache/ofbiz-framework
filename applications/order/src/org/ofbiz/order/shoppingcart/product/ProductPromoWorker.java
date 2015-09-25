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
package org.ofbiz.order.shoppingcart.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCart.ProductPromoUseInfo;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductSearch;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;

import com.ibm.icu.util.Calendar;

/**
 * ProductPromoWorker - Worker class for catalog/product promotion related functionality
 */
public class ProductPromoWorker {

    public static final String module = ProductPromoWorker.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

    public static final MathContext generalRounding = new MathContext(10);

    public static List<GenericValue> getStoreProductPromos(Delegator delegator, LocalDispatcher dispatcher, ServletRequest request) {
        List<GenericValue> productPromos = new LinkedList<GenericValue>();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // get the ShoppingCart out of the session.
        HttpServletRequest req = null;
        ShoppingCart cart = null;
        try {
            req = (HttpServletRequest) request;
            cart = ShoppingCartEvents.getCartObject(req);
        } catch (ClassCastException cce) {
            Debug.logError("Not a HttpServletRequest, no shopping cart found.", module);
            return null;
        } catch (IllegalArgumentException e) {
            Debug.logError(e, module);
            return null;
        }

        boolean condResult = true;

        try {
            String productStoreId = cart.getProductStoreId();
            GenericValue productStore = null;
            try {
                productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up store with id " + productStoreId, module);
            }
            if (productStore == null) {
                Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNoStoreFoundWithIdNotDoingPromotions", UtilMisc.toMap("productStoreId",productStoreId), cart.getLocale()), module);
                return productPromos;
            }

            if (productStore != null) {
                Iterator<GenericValue> productStorePromoAppls = UtilMisc.toIterator(EntityUtil.filterByDate(productStore.getRelated("ProductStorePromoAppl", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("sequenceNum"), true), true));
                while (productStorePromoAppls != null && productStorePromoAppls.hasNext()) {
                    GenericValue productStorePromoAppl = productStorePromoAppls.next();
                    if (UtilValidate.isNotEmpty(productStorePromoAppl.getString("manualOnly")) && "Y".equals(productStorePromoAppl.getString("manualOnly"))) {
                        // manual only promotions are not automatically evaluated (they must be explicitly selected by the user)
                        if (Debug.verboseOn()) Debug.logVerbose("Skipping promotion with id [" + productStorePromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                        continue;
                    }
                    GenericValue productPromo = productStorePromoAppl.getRelatedOne("ProductPromo", true);
                    List<GenericValue> productPromoRules = productPromo.getRelated("ProductPromoRule", null, null, true);


                    if (productPromoRules != null) {
                        Iterator<GenericValue> promoRulesItr = productPromoRules.iterator();

                        while (condResult && promoRulesItr != null && promoRulesItr.hasNext()) {
                            GenericValue promoRule = promoRulesItr.next();
                            Iterator<GenericValue> productPromoConds = UtilMisc.toIterator(promoRule.getRelated("ProductPromoCond", null, UtilMisc.toList("productPromoCondSeqId"), true));

                            while (condResult && productPromoConds != null && productPromoConds.hasNext()) {
                                GenericValue productPromoCond = productPromoConds.next();

                                // evaluate the party related conditions; so we don't show the promo if it doesn't apply.
                                if ("PPIP_PARTY_ID".equals(productPromoCond.getString("inputParamEnumId"))) {
                                    condResult = checkCondition(productPromoCond, cart, delegator, dispatcher, nowTimestamp);
                                } else if ("PPIP_PARTY_GRP_MEM".equals(productPromoCond.getString("inputParamEnumId"))) {
                                    condResult = checkCondition(productPromoCond, cart, delegator, dispatcher, nowTimestamp);
                                } else if ("PPIP_PARTY_CLASS".equals(productPromoCond.getString("inputParamEnumId"))) {
                                    condResult = checkCondition(productPromoCond, cart, delegator, dispatcher, nowTimestamp);
                                } else if ("PPIP_ROLE_TYPE".equals(productPromoCond.getString("inputParamEnumId"))) {
                                    condResult = checkCondition(productPromoCond, cart, delegator, dispatcher, nowTimestamp);
                                }
                            }
                        }
                        if (!condResult) productPromo = null;
                    }
                    if (productPromo != null) productPromos.add(productPromo);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromos;
    }

    public static Set<String> getStoreProductPromoCodes(ShoppingCart cart) {
        Set<String> promoCodes = new HashSet<String>();
        Delegator delegator = cart.getDelegator();

        String productStoreId = cart.getProductStoreId();
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up store with id " + productStoreId, module);
        }
        if (productStore == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNoStoreFoundWithIdNotDoingPromotions", UtilMisc.toMap("productStoreId",productStoreId), cart.getLocale()), module);
            return promoCodes;
        }
        try {
            Iterator<GenericValue> productStorePromoAppls = UtilMisc.toIterator(EntityUtil.filterByDate(productStore.getRelated("ProductStorePromoAppl", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("sequenceNum"), true), true));
            while (productStorePromoAppls != null && productStorePromoAppls.hasNext()) {
                GenericValue productStorePromoAppl = productStorePromoAppls.next();
                if (UtilValidate.isNotEmpty(productStorePromoAppl.getString("manualOnly")) && "Y".equals(productStorePromoAppl.getString("manualOnly"))) {
                    // manual only promotions are not automatically evaluated (they must be explicitly selected by the user)
                    if (Debug.verboseOn()) Debug.logVerbose("Skipping promotion with id [" + productStorePromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                        continue;
                }
                GenericValue productPromo = productStorePromoAppl.getRelatedOne("ProductPromo", true);
                Iterator<GenericValue> productPromoCodesIter = UtilMisc.toIterator(productPromo.getRelated("ProductPromoCode", null, null, true));
                while (productPromoCodesIter != null && productPromoCodesIter.hasNext()) {
                    GenericValue productPromoCode = productPromoCodesIter.next();
                    promoCodes.add(productPromoCode.getString("productPromoCodeId"));
                }
            } 
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return promoCodes;
    }

    public static List<GenericValue> getProductStorePromotions(ShoppingCart cart, Timestamp nowTimestamp, LocalDispatcher dispatcher) {
        List<GenericValue> productPromoList = new LinkedList<GenericValue>();

        Delegator delegator = cart.getDelegator();

        String productStoreId = cart.getProductStoreId();
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up store with id " + productStoreId, module);
        }
        if (productStore == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNoStoreFoundWithIdNotDoingPromotions", UtilMisc.toMap("productStoreId",productStoreId), cart.getLocale()), module);
            return productPromoList;
        }

        try {
            // loop through promotions and get a list of all of the rules...
            List<GenericValue> productStorePromoApplsList = productStore.getRelated("ProductStorePromoAppl", null, UtilMisc.toList("sequenceNum"), true);
            productStorePromoApplsList = EntityUtil.filterByDate(productStorePromoApplsList, nowTimestamp);

            if (UtilValidate.isEmpty(productStorePromoApplsList)) {
                if (Debug.verboseOn()) Debug.logVerbose("Not doing promotions, none applied to store with ID " + productStoreId, module);
            }

            Iterator<GenericValue> prodCatalogPromoAppls = UtilMisc.toIterator(productStorePromoApplsList);
            while (prodCatalogPromoAppls != null && prodCatalogPromoAppls.hasNext()) {
                GenericValue prodCatalogPromoAppl = prodCatalogPromoAppls.next();
                if (UtilValidate.isNotEmpty(prodCatalogPromoAppl.getString("manualOnly")) && "Y".equals(prodCatalogPromoAppl.getString("manualOnly"))) {
                    // manual only promotions are not automatically evaluated (they must be explicitly selected by the user)
                    if (Debug.verboseOn()) Debug.logVerbose("Skipping promotion with id [" + prodCatalogPromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                    continue;
                }
                GenericValue productPromo = prodCatalogPromoAppl.getRelatedOne("ProductPromo", true);
                productPromoList.add(productPromo);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up promotion data while doing promotions", module);
        }
        return productPromoList;
    }

    public static List<GenericValue> getAgreementPromotions(ShoppingCart cart, Timestamp nowTimestamp, LocalDispatcher dispatcher) {
        List<GenericValue> productPromoList = new LinkedList<GenericValue>();

        Delegator delegator = cart.getDelegator();

        String agreementId = cart.getAgreementId();
        GenericValue agreement = null;
        try {
            agreement = EntityQuery.use(delegator).from("Agreement").where("agreementId", agreementId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up agreement with id " + agreementId, module);
        }
        if (agreement == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNoAgreementFoundWithIdNotDoingPromotions", UtilMisc.toMap("agreementId", agreementId), cart.getLocale()), module);
            return productPromoList;
        }
        GenericValue agreementItem = null;
        try {
            agreementItem = EntityQuery.use(delegator).from("AgreementItem").where("agreementId", agreementId, "agreementItemTypeId", "AGREEMENT_PRICING_PR", "currencyUomId", cart.getCurrency()).cache(true).queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up agreement items for agreement with id " + agreementId, module);
        }
        if (agreementItem == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNoAgreementItemFoundForAgreementWithIdNotDoingPromotions", UtilMisc.toMap("agreementId", agreementId), cart.getLocale()), module);
            return productPromoList;
        }

        try {
            // loop through promotions and get a list of all of the rules...
            List<GenericValue> agreementPromoApplsList = agreementItem.getRelated("AgreementPromoAppl", null, UtilMisc.toList("sequenceNum"), true);
            agreementPromoApplsList = EntityUtil.filterByDate(agreementPromoApplsList, nowTimestamp);

            if (UtilValidate.isEmpty(agreementPromoApplsList)) {
                if (Debug.verboseOn()) Debug.logVerbose("Not doing promotions, none applied to agreement with ID " + agreementId, module);
            }

            Iterator<GenericValue> agreementPromoAppls = UtilMisc.toIterator(agreementPromoApplsList);
            while (agreementPromoAppls != null && agreementPromoAppls.hasNext()) {
                GenericValue agreementPromoAppl = agreementPromoAppls.next();
                GenericValue productPromo = agreementPromoAppl.getRelatedOne("ProductPromo", true);
                productPromoList.add(productPromo);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up promotion data while doing promotions", module);
        }
        return productPromoList;
    }

    public static void doPromotions(ShoppingCart cart, LocalDispatcher dispatcher) {
        ProductPromoWorker.doPromotions(cart, null, dispatcher);
    }

    public static void doPromotions(ShoppingCart cart, List<GenericValue> productPromoList, LocalDispatcher dispatcher) {
        // this is called when a user logs in so that per customer limits are honored, called by cart when new userlogin is set
        // there is code to store ProductPromoUse information when an order is placed
        // ProductPromoUses are ignored if the corresponding order is cancelled
        // limits sub total for promos to not use gift cards (products with a don't use in promo indicator), also exclude gift cards from all other promotion considerations including subTotals for discounts, etc
        // TODO: (not done, delay, still considering...) add code to check ProductPromoUse limits per promo (customer, promo), and per code (customer, code) to avoid use of promos or codes getting through due to multiple carts getting promos applied at the same time, possibly on totally different servers

        if (!cart.getDoPromotions()) {
            return;
        }
        Delegator delegator = cart.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // start out by clearing all existing promotions, then we can just add all that apply
        cart.clearAllPromotionInformation();

        // there will be a ton of db access, so just do a big catch entity exception block
        try {
            if (productPromoList == null) {
                if (cart.getOrderType().equals("SALES_ORDER")) {
                    productPromoList = ProductPromoWorker.getProductStorePromotions(cart, nowTimestamp, dispatcher);
                } else {
                    productPromoList = ProductPromoWorker.getAgreementPromotions(cart, nowTimestamp, dispatcher);
                }
            }
            // do a calculate only run through the promotions, then order by descending totalDiscountAmount for each promotion
            // NOTE: on this run, with isolatedTestRun passed as false it should not apply any adjustments
            //  or track which cart items are used for which promotions, but it will track ProductPromoUseInfo and
            //  useLimits; we are basically just trying to run each promo "independently" to see how much each is worth
            runProductPromos(productPromoList, cart, delegator, dispatcher, nowTimestamp, true);

            // NOTE: we can easily recognize the promos for the order total: they are the ones with usage set to 0
            Iterator<ProductPromoUseInfo> promoUses = cart.getProductPromoUseInfoIter();
            List<ProductPromoUseInfo> sortedPromoUses = new ArrayList<ProductPromoUseInfo>();
            while (promoUses.hasNext()) {
                ProductPromoUseInfo promoUse = promoUses.next();
                sortedPromoUses.add(promoUse);
            }
            Collections.sort(sortedPromoUses);
            List<GenericValue> sortedExplodedProductPromoList = new ArrayList<GenericValue>(sortedPromoUses.size());
            Map<String, Long> usesPerPromo = new HashMap<String, Long>();
            int indexOfFirstOrderTotalPromo = -1;
            for (ProductPromoUseInfo promoUse: sortedPromoUses) {
                GenericValue productPromo = EntityQuery.use(delegator).from("ProductPromo").where("productPromoId", promoUse.getProductPromoId()).cache().queryOne();
                GenericValue newProductPromo = (GenericValue)productPromo.clone();
                if (!usesPerPromo.containsKey(promoUse.getProductPromoId())) {
                    usesPerPromo.put(promoUse.getProductPromoId(), 0l);
                }
                long uses = usesPerPromo.get(promoUse.getProductPromoId());
                uses = uses + 1;
                long useLimitPerOrder = (newProductPromo.get("useLimitPerOrder") != null? newProductPromo.getLong("useLimitPerOrder"): -1);
                if (useLimitPerOrder == -1 || uses < useLimitPerOrder) {
                    newProductPromo.set("useLimitPerOrder", uses);
                }
                usesPerPromo.put(promoUse.getProductPromoId(), uses);
                sortedExplodedProductPromoList.add(newProductPromo);
                if (indexOfFirstOrderTotalPromo == -1 && BigDecimal.ZERO.equals(promoUse.getUsageWeight())) {
                    indexOfFirstOrderTotalPromo = sortedExplodedProductPromoList.size() - 1;
                }
            }
            if (indexOfFirstOrderTotalPromo == -1) {
                indexOfFirstOrderTotalPromo = sortedExplodedProductPromoList.size() - 1;
            }

            for (GenericValue productPromo : productPromoList) {
                if (hasOrderTotalCondition(productPromo, delegator)) {
                    if (!usesPerPromo.containsKey(productPromo.getString("productPromoId"))) {
                        sortedExplodedProductPromoList.add(productPromo);
                    }
                } else {
                    if (!usesPerPromo.containsKey(productPromo.getString("productPromoId"))) {
                        if (indexOfFirstOrderTotalPromo != -1) {
                            sortedExplodedProductPromoList.add(indexOfFirstOrderTotalPromo, productPromo);
                        } else {
                            sortedExplodedProductPromoList.add(0, productPromo);
                        }
                    }
                }
            }

            // okay, all ready, do the real run, clearing the temporary result first...
            cart.clearAllPromotionInformation();
            runProductPromos(sortedExplodedProductPromoList, cart, delegator, dispatcher, nowTimestamp, false);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Number not formatted correctly in promotion rules, not completed...", module);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up promotion data while doing promotions", module);
        } catch (Exception e) {
            Debug.logError(e, "Error running promotions, will ignore: " + e.toString(), module);
        }
    }

    protected static boolean hasOrderTotalCondition(GenericValue productPromo, Delegator delegator) throws GenericEntityException {
        boolean hasOtCond = false;
        List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond")
                .where("productPromoId", productPromo.get("productPromoId"))
                .orderBy("productPromoCondSeqId")
                .cache(true).queryList();
        for (GenericValue productPromoCond : productPromoConds) {
            String inputParamEnumId = productPromoCond.getString("inputParamEnumId");
            if ("PPIP_ORDER_TOTAL".equals(inputParamEnumId)) {
                hasOtCond = true;
                break;
            }
        }
        return hasOtCond;
    }

    protected static void runProductPromos(List<GenericValue> productPromoList, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp, boolean isolatedTestRun) throws GeneralException {
        String partyId = cart.getPartyId();

        // this is our safety net; we should never need to loop through the rules more than a certain number of times, this is that number and may have to be changed for insanely large promo sets...
        long maxIterations = 1000;
        // part of the safety net to avoid infinite iteration
        long numberOfIterations = 0;

        // set a max limit on how many times each promo can be run, for cases where there is no use limit this will be the use limit
        //default to 2 times the number of items in the cart
        long maxUseLimit = cart.getTotalQuantity().multiply(BigDecimal.valueOf(2)).setScale(0, BigDecimal.ROUND_CEILING).longValue();
        maxUseLimit = Math.max(1, maxUseLimit);

        try {
            // repeat until no more rules to run: either all rules are run, or no changes to the cart in a loop
            boolean cartChanged = true;
            while (cartChanged) {
                cartChanged = false;
                numberOfIterations++;
                if (numberOfIterations > maxIterations) {
                    Debug.logError("ERROR: While calculating promotions the promotion rules where run more than " + maxIterations + " times, so the calculation has been ended. This should generally never happen unless you have bad rule definitions.", module);
                    break;
                }

                for (GenericValue productPromo : productPromoList) {
                    String productPromoId = productPromo.getString("productPromoId");

                    List<GenericValue> productPromoRules = productPromo.getRelated("ProductPromoRule", null, null, true);
                    if (UtilValidate.isNotEmpty(productPromoRules)) {
                        // always have a useLimit to avoid unlimited looping, default to 1 if no other is specified
                        Long candidateUseLimit = getProductPromoUseLimit(productPromo, partyId, delegator);
                        Long useLimit = candidateUseLimit;
                        if (Debug.verboseOn()) Debug.logVerbose("Running promotion [" + productPromoId + "], useLimit=" + useLimit + ", # of rules=" + productPromoRules.size(), module);

                        boolean requireCode = "Y".equals(productPromo.getString("requireCode"));
                        // check if promo code required
                        if (requireCode) {
                            Set<String> enteredCodes = cart.getProductPromoCodesEntered();
                            // Check whether any promotion code is applied on order.
                            if (cart.getOrderId() != null) {
                                List<GenericValue> orderproductPromoCodes =  EntityQuery.use(delegator).from("OrderProductPromoCode").where("orderId", cart.getOrderId()).queryList();
                                Iterator<GenericValue> orderproductPromoCodesItr = UtilMisc.toIterator(orderproductPromoCodes);
                                while (orderproductPromoCodesItr != null && orderproductPromoCodesItr.hasNext()) {
                                    GenericValue orderproductPromoCode = orderproductPromoCodesItr.next();
                                    enteredCodes.add(orderproductPromoCode.getString("productPromoCodeId"));
                                }
                            }
                            if (enteredCodes.size() > 0) {
                                // get all promo codes entered, do a query with an IN condition to see if any of those are related
                                EntityCondition codeCondition = EntityCondition.makeCondition(EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId), EntityOperator.AND, EntityCondition.makeCondition("productPromoCodeId", EntityOperator.IN, enteredCodes));
                                // may want to sort by something else to decide which code to use if there is more than one candidate
                                List<GenericValue> productPromoCodeList = EntityQuery.use(delegator).from("ProductPromoCode").where(codeCondition).orderBy("productPromoCodeId").queryList();
                                Iterator<GenericValue> productPromoCodeIter = productPromoCodeList.iterator();
                                // support multiple promo codes for a single promo, ie if we run into a use limit for one code see if we can find another for this promo
                                // check the use limit before each pass so if the promo use limit has been hit we don't keep on trying for the promo code use limit, if there is one of course
                                while ((useLimit == null || useLimit.longValue() > cart.getProductPromoUseCount(productPromoId)) && productPromoCodeIter.hasNext()) {
                                    GenericValue productPromoCode = productPromoCodeIter.next();
                                    String productPromoCodeId = productPromoCode.getString("productPromoCodeId");
                                    Long codeUseLimit = getProductPromoCodeUseLimit(productPromoCode, partyId, delegator);
                                    if (runProductPromoRules(cart, useLimit, true, productPromoCodeId, codeUseLimit, maxUseLimit, productPromo, productPromoRules, dispatcher, delegator, nowTimestamp)) {
                                        cartChanged = true;
                                    }

                                    if (cart.getProductPromoUseCount(productPromoId) > maxUseLimit) {
                                        Debug.logError("ERROR: While calculating promotions the promotion [" + productPromoId + "] action was applied more than " + maxUseLimit + " times, so the calculation has been ended. This should generally never happen unless you have bad rule definitions.", module);
                                        break;
                                    }
                                }
                            }
                        } else {
                            try {
                                if (runProductPromoRules(cart, useLimit, false, null, null, maxUseLimit, productPromo, productPromoRules, dispatcher, delegator, nowTimestamp)) {
                                    cartChanged = true;
                                }
                            } catch (RuntimeException e) {
                                throw new GeneralException("Error running promotion with ID [" + productPromoId + "]", e);
                            }
                        }
                    }

                    // if this is an isolatedTestRun clear out adjustments and cart item promo use info
                    if (isolatedTestRun) {
                        cart.clearAllPromotionAdjustments();
                        cart.clearCartItemUseInPromoInfo();
                    }
                }

                // if this is an isolatedTestRun, then only go through it once, never retry
                if (isolatedTestRun) {
                    cartChanged = false;
                }
            }
        } catch (UseLimitException e) {
            Debug.logError(e, e.toString(), module);
        }
    }

    /** calculate low use limit for this promo for the current "order", check per order, customer, promo */
    public static Long getProductPromoUseLimit(GenericValue productPromo, String partyId, Delegator delegator) throws GenericEntityException {
        String productPromoId = productPromo.getString("productPromoId");
        Long candidateUseLimit = null;

        Long useLimitPerOrder = productPromo.getLong("useLimitPerOrder");
        if (useLimitPerOrder != null) {
            if (candidateUseLimit == null || candidateUseLimit.longValue() > useLimitPerOrder.longValue()) {
                candidateUseLimit = useLimitPerOrder;
            }
        }

        // Debug.logInfo("Promo [" + productPromoId + "] use limit after per order check: " + candidateUseLimit, module);

        Long useLimitPerCustomer = productPromo.getLong("useLimitPerCustomer");
        // check this whether or not there is a party right now
        if (useLimitPerCustomer != null) {
            // if partyId is not empty check previous usage
            long productPromoCustomerUseSize = 0;
            if (UtilValidate.isNotEmpty(partyId)) {
                // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
                EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId),
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
                productPromoCustomerUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            }
            long perCustomerThisOrder = useLimitPerCustomer.longValue() - productPromoCustomerUseSize;
            if (candidateUseLimit == null || candidateUseLimit.longValue() > perCustomerThisOrder) {
                candidateUseLimit = Long.valueOf(perCustomerThisOrder);
            }
        }

        // Debug.logInfo("Promo [" + productPromoId + "] use limit after per customer check: " + candidateUseLimit, module);

        Long useLimitPerPromotion = productPromo.getLong("useLimitPerPromotion");
        if (useLimitPerPromotion != null) {
            // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
            long productPromoUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            long perPromotionThisOrder = useLimitPerPromotion.longValue() - productPromoUseSize;
            if (candidateUseLimit == null || candidateUseLimit.longValue() > perPromotionThisOrder) {
                candidateUseLimit = Long.valueOf(perPromotionThisOrder);
            }
        }

        // Debug.logInfo("Promo [" + productPromoId + "] use limit after per promotion check: " + candidateUseLimit, module);

        return candidateUseLimit;
    }

    public static Long getProductPromoCodeUseLimit(GenericValue productPromoCode, String partyId, Delegator delegator) throws GenericEntityException {
        String productPromoCodeId = productPromoCode.getString("productPromoCodeId");
        Long codeUseLimit = null;

        // check promo code use limits, per customer, code
        Long codeUseLimitPerCustomer = productPromoCode.getLong("useLimitPerCustomer");
        if (codeUseLimitPerCustomer != null) {
            long productPromoCustomerUseSize = 0;
            if (UtilValidate.isNotEmpty(partyId)) {
                // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
                EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("productPromoCodeId", EntityOperator.EQUALS, productPromoCodeId),
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
                productPromoCustomerUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            }
            long perCustomerThisOrder = codeUseLimitPerCustomer.longValue() - productPromoCustomerUseSize;
            if (codeUseLimit == null || codeUseLimit.longValue() > perCustomerThisOrder) {
                codeUseLimit = Long.valueOf(perCustomerThisOrder);
            }
        }

        Long codeUseLimitPerCode = productPromoCode.getLong("useLimitPerCode");
        if (codeUseLimitPerCode != null) {
            // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productPromoCodeId", EntityOperator.EQUALS, productPromoCodeId),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
            long productPromoCodeUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            long perCodeThisOrder = codeUseLimitPerCode.longValue() - productPromoCodeUseSize;
            if (codeUseLimit == null || codeUseLimit.longValue() > perCodeThisOrder) {
                codeUseLimit = Long.valueOf(perCodeThisOrder);
            }
        }

        return codeUseLimit;
    }

    public static String checkCanUsePromoCode(String productPromoCodeId, String partyId, Delegator delegator, Locale locale) {
        return checkCanUsePromoCode(productPromoCodeId, partyId, delegator, null, locale);
    }

    public static String checkCanUsePromoCode(String productPromoCodeId, String partyId, Delegator delegator, ShoppingCart cart, Locale locale) {
        try {
            GenericValue productPromoCode = EntityQuery.use(delegator).from("ProductPromoCode").where("productPromoCodeId", productPromoCodeId).queryOne();
            if (productPromoCode == null) {
                return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_not_valid", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
            }
            if (cart != null) {
                Set<String> promoCodes = ProductPromoWorker.getStoreProductPromoCodes(cart);
                if (UtilValidate.isEmpty(promoCodes) || !promoCodes.contains(productPromoCodeId)) {
                    return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_not_valid", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
                }
            }
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            Timestamp thruDate = productPromoCode.getTimestamp("thruDate");
            if (thruDate != null) {
                if (nowTimestamp.after(thruDate)) {
                    return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_is_expired_at", UtilMisc.toMap("productPromoCodeId", productPromoCodeId, "thruDate", thruDate), locale);
                }
            }
            Timestamp fromDate = productPromoCode.getTimestamp("fromDate");
            if (fromDate != null) {
                if (nowTimestamp.before(fromDate)) {
                    return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_will_be_activated_at", UtilMisc.toMap("productPromoCodeId", productPromoCodeId, "fromDate", fromDate), locale);
                }
            }

            if ("Y".equals(productPromoCode.getString("requireEmailOrParty"))) {
                boolean hasEmailOrParty = false;

                // check partyId
                if (UtilValidate.isNotEmpty(partyId)) {
                    if (EntityQuery.use(delegator).from("ProductPromoCodeParty").where("productPromoCodeId", productPromoCodeId, "partyId", partyId).queryOne() != null) {
                        // found party associated with the code, looks good...
                        return null;
                    }

                    // check email address in ProductPromoCodeEmail
                    List<EntityCondition> validEmailCondList = new LinkedList<EntityCondition>();
                    validEmailCondList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
                    validEmailCondList.add(EntityCondition.makeCondition("productPromoCodeId", EntityOperator.EQUALS, productPromoCodeId));
                    validEmailCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
                    validEmailCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp),
                            EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
                    long validEmailCount = EntityQuery.use(delegator).from("ProductPromoCodeEmailParty").where(validEmailCondList).queryCount();
                    if (validEmailCount > 0) {
                        // there was an email in the list, looks good...
                        return null;
                    }
                }

                if (!hasEmailOrParty) {
                    return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_no_account_or_email", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
                }
            }

            // check per customer and per promotion code use limits
            Long useLimit = getProductPromoCodeUseLimit(productPromoCode, partyId, delegator);
            if (useLimit != null && useLimit.longValue() <= 0) {
                return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_maximum_limit", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
            }

            return null;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up ProductPromoCode", module);
            return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_error_lookup", UtilMisc.toMap("productPromoCodeId", productPromoCodeId, "errorMsg", e.toString()), locale);
        }
    }

    public static String makeAutoDescription(GenericValue productPromo, Delegator delegator, Locale locale) throws GenericEntityException {
        if (productPromo == null) {
            return "";
        }
        StringBuilder promoDescBuf = new StringBuilder();
        List<GenericValue> productPromoRules = productPromo.getRelated("ProductPromoRule", null, null, true);
        Iterator<GenericValue> promoRulesIter = productPromoRules.iterator();
        while (promoRulesIter != null && promoRulesIter.hasNext()) {
            GenericValue productPromoRule = promoRulesIter.next();

            List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromo.get("productPromoId")).orderBy("productPromoCondSeqId").cache(true).queryList();
            productPromoConds = EntityUtil.filterByAnd(productPromoConds, UtilMisc.toMap("productPromoRuleId", productPromoRule.get("productPromoRuleId")));
            // using the other method to consolodate cache entries because the same cache is used elsewhere: List productPromoConds = productPromoRule.getRelated("ProductPromoCond", null, UtilMisc.toList("productPromoCondSeqId"), true);
            Iterator<GenericValue> productPromoCondIter = UtilMisc.toIterator(productPromoConds);
            while (productPromoCondIter != null && productPromoCondIter.hasNext()) {
                GenericValue productPromoCond = productPromoCondIter.next();

                String equalityOperator = UtilProperties.getMessage("promotext", "operator.equality." + productPromoCond.getString("operatorEnumId"), locale);
                String quantityOperator = UtilProperties.getMessage("promotext", "operator.quantity." + productPromoCond.getString("operatorEnumId"), locale);

                String condValue = "invalid";
                if (UtilValidate.isNotEmpty(productPromoCond.getString("condValue"))) {
                    condValue = productPromoCond.getString("condValue");
                }

                Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("condValue", condValue, "equalityOperator", equalityOperator, "quantityOperator", quantityOperator);
                String msgProp = UtilProperties.getMessage("promotext", "condition." + productPromoCond.getString("inputParamEnumId"), messageContext, locale);
                promoDescBuf.append(msgProp);
                promoDescBuf.append(" ");

                if (promoRulesIter.hasNext()) {
                    promoDescBuf.append(" and ");
                }
            }

            List<GenericValue> productPromoActions = productPromoRule.getRelated("ProductPromoAction", null, UtilMisc.toList("productPromoActionSeqId"), true);
            Iterator<GenericValue> productPromoActionIter = UtilMisc.toIterator(productPromoActions);
            while (productPromoActionIter != null && productPromoActionIter.hasNext()) {
                GenericValue productPromoAction = productPromoActionIter.next();

                String productId = productPromoAction.getString("productId");

                Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("quantity", productPromoAction.get("quantity"), "amount", productPromoAction.get("amount"), "productId", productId, "partyId", productPromoAction.get("partyId"));

                if (UtilValidate.isEmpty(messageContext.get("productId"))) messageContext.put("productId", "any");
                if (UtilValidate.isEmpty(messageContext.get("partyId"))) messageContext.put("partyId", "any");
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                if (product != null) {
                    messageContext.put("productName", ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", locale, null, "html"));
                }

                String msgProp = UtilProperties.getMessage("promotext", "action." + productPromoAction.getString("productPromoActionEnumId"), messageContext, locale);
                promoDescBuf.append(msgProp);
                promoDescBuf.append(" ");

                if (promoRulesIter.hasNext()) {
                    promoDescBuf.append(" and ");
                }
            }

            if (promoRulesIter.hasNext()) {
                promoDescBuf.append(" or ");
            }
        }

        if (promoDescBuf.length() > 0) {
            // remove any trailing space
            if (promoDescBuf.charAt(promoDescBuf.length() - 1) == ' ') promoDescBuf.deleteCharAt(promoDescBuf.length() - 1);
            // add a period
            promoDescBuf.append(". ");
            // capitalize the first letter
            promoDescBuf.setCharAt(0, Character.toUpperCase(promoDescBuf.charAt(0)));
        }

        if ("Y".equals(productPromo.getString("requireCode"))) {
            promoDescBuf.append(UtilProperties.getMessage(resource, "OrderRequiresCodeToUse", locale));
        }
        if (productPromo.getLong("useLimitPerOrder") != null) {
            promoDescBuf.append(UtilProperties.getMessage(resource, "OrderLimitPerOrder",
                    UtilMisc.toMap("limit", productPromo.getLong("useLimitPerOrder")), locale));
        }
        if (productPromo.getLong("useLimitPerCustomer") != null) {
            promoDescBuf.append(UtilProperties.getMessage(resource, "OrderLimitPerCustomer",
                    UtilMisc.toMap("limit", productPromo.getLong("useLimitPerCustomer")), locale));
        }
        if (productPromo.getLong("useLimitPerPromotion") != null) {
            promoDescBuf.append(UtilProperties.getMessage(resource, "OrderLimitPerPromotion",
                    UtilMisc.toMap("limit", productPromo.getLong("useLimitPerPromotion")), locale));
        }

        return promoDescBuf.toString();
    }

    protected static boolean runProductPromoRules(ShoppingCart cart, Long useLimit, boolean requireCode, String productPromoCodeId, Long codeUseLimit, long maxUseLimit,
        GenericValue productPromo, List<GenericValue> productPromoRules, LocalDispatcher dispatcher, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException, UseLimitException {
        boolean cartChanged = false;
        Map<ShoppingCartItem,BigDecimal> usageInfoMap = prepareProductUsageInfoMap(cart);
        String productPromoId = productPromo.getString("productPromoId");
        while ((useLimit == null || useLimit.longValue() > cart.getProductPromoUseCount(productPromoId)) &&
                (!requireCode || UtilValidate.isNotEmpty(productPromoCodeId)) &&
                (codeUseLimit == null || codeUseLimit.longValue() > cart.getProductPromoCodeUse(productPromoCodeId))) {
            boolean promoUsed = false;
            BigDecimal totalDiscountAmount = BigDecimal.ZERO;
            BigDecimal quantityLeftInActions = BigDecimal.ZERO;

            Iterator<GenericValue> promoRulesIter = productPromoRules.iterator();
            while (promoRulesIter != null && promoRulesIter.hasNext()) {
                GenericValue productPromoRule = promoRulesIter.next();

                // if apply then performActions when no conditions are false, so default to true
                boolean performActions = true;

                // loop through conditions for rule, if any false, set allConditionsTrue to false
                List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromo.get("productPromoId")).orderBy("productPromoCondSeqId").cache(true).queryList();
                productPromoConds = EntityUtil.filterByAnd(productPromoConds, UtilMisc.toMap("productPromoRuleId", productPromoRule.get("productPromoRuleId")));
                // using the other method to consolidate cache entries because the same cache is used elsewhere: List productPromoConds = productPromoRule.getRelated("ProductPromoCond", null, UtilMisc.toList("productPromoCondSeqId"), true);
                if (Debug.verboseOn()) Debug.logVerbose("Checking " + productPromoConds.size() + " conditions for rule " + productPromoRule, module);

                Iterator<GenericValue> productPromoCondIter = UtilMisc.toIterator(productPromoConds);
                while (productPromoCondIter != null && productPromoCondIter.hasNext()) {
                    GenericValue productPromoCond = productPromoCondIter.next();

                    boolean conditionSatisfied = checkCondition(productPromoCond, cart, delegator, dispatcher, nowTimestamp);

                    // any false condition will cause it to NOT perform the action
                    if (!conditionSatisfied) {
                        performActions = false;
                        break;
                    }
                }

                if (performActions) {
                    // perform all actions, either apply or unapply

                    List<GenericValue> productPromoActions = productPromoRule.getRelated("ProductPromoAction", null, UtilMisc.toList("productPromoActionSeqId"), true);
                    Iterator<GenericValue> productPromoActionIter = UtilMisc.toIterator(productPromoActions);
                    while (productPromoActionIter != null && productPromoActionIter.hasNext()) {
                        GenericValue productPromoAction = productPromoActionIter.next();
                        try {
                            ActionResultInfo actionResultInfo = performAction(productPromoAction, cart, delegator, dispatcher, nowTimestamp);
                            totalDiscountAmount = totalDiscountAmount.add(actionResultInfo.totalDiscountAmount);
                            quantityLeftInActions = quantityLeftInActions.add(actionResultInfo.quantityLeftInAction);

                            // only set if true, don't set back to false: implements OR logic (ie if ANY actions change content, redo loop)
                            boolean actionChangedCart = actionResultInfo.ranAction;
                            if (actionChangedCart) {
                                promoUsed = true;
                                cartChanged = true;
                            }
                        } catch (CartItemModifyException e) {
                            Debug.logError(e, "Error modifying the cart while performing promotion action [" + productPromoAction.getPrimaryKey() + "]", module);
                        }
                    }
                }
            }

            if (promoUsed) {
                // Get product use information from the cart
                Map<ShoppingCartItem,BigDecimal> newUsageInfoMap = prepareProductUsageInfoMap(cart);
                Map<ShoppingCartItem,BigDecimal> deltaUsageInfoMap = prepareDeltaProductUsageInfoMap(usageInfoMap, newUsageInfoMap);
                usageInfoMap = newUsageInfoMap;
                cart.addProductPromoUse(productPromo.getString("productPromoId"), productPromoCodeId, totalDiscountAmount, quantityLeftInActions, deltaUsageInfoMap);
            } else {
                // the promotion was not used, don't try again until we finish a full pass and come back to see the promo conditions are now satisfied based on changes to the cart
                break;
            }

            if (cart.getProductPromoUseCount(productPromoId) > maxUseLimit) {
                throw new UseLimitException("ERROR: While calculating promotions the promotion [" + productPromoId + "] action was applied more than " + maxUseLimit + " times, so the calculation has been ended. This should generally never happen unless you have bad rule definitions.");
            }
        }

        return cartChanged;
    }

    private static Map<ShoppingCartItem,BigDecimal> prepareProductUsageInfoMap(ShoppingCart cart) {
        Map<ShoppingCartItem,BigDecimal> usageInfoMap = new HashMap<ShoppingCartItem, BigDecimal>();
        List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
        for (ShoppingCartItem cartItem : lineOrderedByBasePriceList) {
            BigDecimal used = cartItem.getPromoQuantityUsed();
            if (used.compareTo(BigDecimal.ZERO) != 0) {
                usageInfoMap.put(cartItem, used);
            }
        }
        return usageInfoMap;
    }

    private static Map<ShoppingCartItem,BigDecimal> prepareDeltaProductUsageInfoMap(Map<ShoppingCartItem,BigDecimal> oldMap, Map<ShoppingCartItem,BigDecimal> newMap) {
        Map<ShoppingCartItem,BigDecimal> deltaUsageInfoMap = new HashMap<ShoppingCartItem, BigDecimal>(newMap);
        Iterator<ShoppingCartItem> cartLines = oldMap.keySet().iterator();
        while (cartLines.hasNext()) {
            ShoppingCartItem cartLine = cartLines.next();
            BigDecimal oldUsed = oldMap.get(cartLine);
            BigDecimal newUsed = newMap.get(cartLine);
            if (newUsed.compareTo(oldUsed) > 0) {
                deltaUsageInfoMap.put(cartLine, newUsed.add(oldUsed.negate()));
            } else {
                deltaUsageInfoMap.remove(cartLine);
            }
        }
        return deltaUsageInfoMap;
    }

    protected static boolean checkCondition(GenericValue productPromoCond, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        String condValue = productPromoCond.getString("condValue");
        String otherValue = productPromoCond.getString("otherValue");
        String inputParamEnumId = productPromoCond.getString("inputParamEnumId");
        String operatorEnumId = productPromoCond.getString("operatorEnumId");
        String shippingMethod = "";
        String carrierPartyId = "";
        if (otherValue != null && otherValue.contains("@")) {
            carrierPartyId = otherValue.substring(0, otherValue.indexOf("@"));
            shippingMethod = otherValue.substring(otherValue.indexOf("@")+1);
            otherValue = "";
        }
        String partyId = cart.getPartyId();
        GenericValue userLogin = cart.getUserLogin();
        if (userLogin == null) {
            userLogin = cart.getAutoUserLogin();
        }

        if (Debug.verboseOn()) Debug.logVerbose("Checking promotion condition: " + productPromoCond, module);
        Integer compareBase = null;

        if ("PPIP_SERVICE".equals(inputParamEnumId)) {
            Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("productPromoCond", productPromoCond, "shoppingCart", cart, "nowTimestamp", nowTimestamp);
            Map<String, Object> condResult;
            try {
                condResult = dispatcher.runSync(condValue, serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Fatal error calling promo condition check service [" + condValue + "]", module);
                return false;
            }
            if (ServiceUtil.isError(condResult)) {
                Debug.logError("Error calling calling promo condition check service [" + condValue + "]", module);
                return false;
            }
            Boolean directResult = (Boolean) condResult.get("directResult");
            if (directResult != null) {
                return directResult.booleanValue();
            }
            compareBase = (Integer) condResult.get("compareBase");
            if (condResult.containsKey("operatorEnumId")) {
                operatorEnumId = (String) condResult.get("operatorEnumId");
            }
        } else if ("PPIP_PRODUCT_AMOUNT".equals(inputParamEnumId)) {
            // for this type of promo force the operatorEnumId = PPC_EQ, effectively ignore that setting because the comparison is implied in the code
            operatorEnumId = "PPC_EQ";

            // this type of condition requires items involved to not be involved in any other quantity consuming cond/action, and does not pro-rate the price, just uses the base price
            BigDecimal amountNeeded = BigDecimal.ZERO;
            if (UtilValidate.isNotEmpty(condValue)) {
                amountNeeded = new BigDecimal(condValue);
            }

            // Debug.logInfo("Doing Amount Cond with Value: " + amountNeeded, module);

            Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp);

            List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
            Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator();
            while (amountNeeded.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
                ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next();
                // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                GenericValue product = cartItem.getProduct();
                String parentProductId = cartItem.getParentProductId();
                boolean passedItemConds = checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp);
                if (passedItemConds && !cartItem.getIsPromo() &&
                        (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                        (product == null || !"N".equals(product.getString("includeInPromotions")))) {

                    BigDecimal basePrice = cartItem.getBasePrice();
                    // get a rough price, round it up to an integer
                    BigDecimal quantityNeeded = amountNeeded.divide(basePrice, generalRounding).setScale(0, BigDecimal.ROUND_CEILING);

                    // reduce amount still needed to qualify for promo (amountNeeded)
                    BigDecimal quantity = cartItem.addPromoQuantityCandidateUse(quantityNeeded, productPromoCond, false);
                    // get pro-rated amount based on discount
                    amountNeeded = amountNeeded.subtract(quantity.multiply(basePrice));
                }
            }

            // Debug.logInfo("Doing Amount Cond with Value after finding applicable cart lines: " + amountNeeded, module);

            // if amountNeeded > 0 then the promo condition failed, so remove candidate promo uses and increment the promoQuantityUsed to restore it
            if (amountNeeded.compareTo(BigDecimal.ZERO) > 0) {
                // failed, reset the entire rule, ie including all other conditions that might have been done before
                cart.resetPromoRuleUse(productPromoCond.getString("productPromoId"), productPromoCond.getString("productPromoRuleId"));
                compareBase = Integer.valueOf(-1);
            } else {
                // we got it, the conditions are in place...
                compareBase = Integer.valueOf(0);
                // NOTE: don't confirm promo rule use here, wait until actions are complete for the rule to do that
            }
        } else if ("PPIP_PRODUCT_TOTAL".equals(inputParamEnumId)) {
            // this type of condition allows items involved to be involved in other quantity consuming cond/action, and does pro-rate the price
            if (UtilValidate.isNotEmpty(condValue)) {
                BigDecimal amountNeeded = new BigDecimal(condValue);
                BigDecimal amountAvailable = BigDecimal.ZERO;

                // Debug.logInfo("Doing Amount Not Counted Cond with Value: " + amountNeeded, module);

                Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp);

                List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
                for (ShoppingCartItem cartItem : lineOrderedByBasePriceList) {
                    // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                    GenericValue product = cartItem.getProduct();
                    String parentProductId = cartItem.getParentProductId();
                    boolean passedItemConds = checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp);
                    if (passedItemConds && !cartItem.getIsPromo() &&
                            (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                            (product == null || !"N".equals(product.getString("includeInPromotions")))) {

                        // just count the entire sub-total of the item
                        amountAvailable = amountAvailable.add(cartItem.getItemSubTotal());
                    }
                }

                // Debug.logInfo("Doing Amount Not Counted Cond with Value after finding applicable cart lines: " + amountNeeded, module);

                compareBase = Integer.valueOf(amountAvailable.compareTo(amountNeeded));
            }
        } else if ("PPIP_PRODUCT_QUANT".equals(inputParamEnumId)) {
            // for this type of promo force the operatorEnumId = PPC_EQ, effectively ignore that setting because the comparison is implied in the code
            operatorEnumId = "PPC_EQ";

            BigDecimal quantityNeeded = BigDecimal.ONE;
            if (UtilValidate.isNotEmpty(condValue)) {
                quantityNeeded = new BigDecimal(condValue);
            }

            Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp);

            List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
            Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator();
            while (quantityNeeded.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
                ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next();
                // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                GenericValue product = cartItem.getProduct();
                String parentProductId = cartItem.getParentProductId();
                boolean passedItemConds = checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp);
                if (passedItemConds && !cartItem.getIsPromo() &&
                        (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                        (product == null || !"N".equals(product.getString("includeInPromotions")))) {
                    // reduce quantity still needed to qualify for promo (quantityNeeded)
                    quantityNeeded = quantityNeeded.subtract(cartItem.addPromoQuantityCandidateUse(quantityNeeded, productPromoCond, false));
                }
            }

            // if quantityNeeded > 0 then the promo condition failed, so remove candidate promo uses and increment the promoQuantityUsed to restore it
            if (quantityNeeded.compareTo(BigDecimal.ZERO) > 0) {
                // failed, reset the entire rule, ie including all other conditions that might have been done before
                cart.resetPromoRuleUse(productPromoCond.getString("productPromoId"), productPromoCond.getString("productPromoRuleId"));
                compareBase = Integer.valueOf(-1);
            } else {
                // we got it, the conditions are in place...
                compareBase = Integer.valueOf(0);
                // NOTE: don't confirm rpomo rule use here, wait until actions are complete for the rule to do that
            }

        /* replaced by PPIP_PRODUCT_QUANT
        } else if ("PPIP_PRODUCT_ID_IC".equals(inputParamEnumId)) {
            String candidateProductId = condValue;

            if (candidateProductId == null) {
                // if null, then it's not in the cart
                compareBase = Integer.valueOf(1);
            } else {
                // Debug.logInfo("Testing to see if productId \"" + candidateProductId + "\" is in the cart", module);
                List productCartItems = cart.findAllCartItems(candidateProductId);

                // don't count promotion items in this count...
                Iterator pciIter = productCartItems.iterator();
                while (pciIter.hasNext()) {
                    ShoppingCartItem productCartItem = (ShoppingCartItem) pciIter.next();
                    if (productCartItem.getIsPromo()) pciIter.remove();
                }

                if (productCartItems.size() > 0) {
                    //Debug.logError("Item with productId \"" + candidateProductId + "\" IS in the cart", module);
                    compareBase = Integer.valueOf(0);
                } else {
                    //Debug.logError("Item with productId \"" + candidateProductId + "\" IS NOT in the cart", module);
                    compareBase = Integer.valueOf(1);
                }
            }
        } else if ("PPIP_CATEGORY_ID_IC".equals(inputParamEnumId)) {
            String productCategoryId = condValue;
            Set productIds = new HashSet();

            Iterator cartItemIter = cart.iterator();
            while (cartItemIter.hasNext()) {
                ShoppingCartItem cartItem = (ShoppingCartItem) cartItemIter.next();
                if (!cartItem.getIsPromo()) {
                    productIds.add(cartItem.getProductId());
                }
            }

            compareBase = Integer.valueOf(1);
            // NOTE: this technique is efficient for a smaller number of items in the cart, if there are a lot of lines
            //in the cart then a non-cached query with a set of productIds using the IN operator would be better
            Iterator productIdIter = productIds.iterator();
            while (productIdIter.hasNext()) {
                String productId = (String) productIdIter.next();

                // if a ProductCategoryMember exists for this productId and the specified productCategoryId
                List productCategoryMembers = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productId", productId, "productCategoryId", productCategoryId), null, true);
                // and from/thru date within range
                productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, nowTimestamp);
                if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                    // if any product is in category, set true and break
                    // then 0 (equals), otherwise 1 (not equals)
                    compareBase = Integer.valueOf(0);
                    break;
                }
            }
        */
        } else if ("PPIP_NEW_ACCT".equals(inputParamEnumId)) {
            if (UtilValidate.isNotEmpty(condValue)) {
                BigDecimal acctDays = cart.getPartyDaysSinceCreated(nowTimestamp);
                if (acctDays == null) {
                    // condition always fails if we don't know how many days since account created
                    return false;
                }
                compareBase = acctDays.compareTo(new BigDecimal(condValue));
            }
        } else if ("PPIP_PARTY_ID".equals(inputParamEnumId)) {
            if (partyId != null && UtilValidate.isNotEmpty(condValue)) {
                compareBase = Integer.valueOf(partyId.compareTo(condValue));
            } else {
                compareBase = Integer.valueOf(1);
            }
        } else if ("PPIP_PARTY_GRP_MEM".equals(inputParamEnumId)) {
            if (UtilValidate.isEmpty(partyId) || UtilValidate.isEmpty(condValue)) {
                compareBase = Integer.valueOf(1);
            } else {
                String groupPartyId = condValue;
                if (partyId.equals(groupPartyId)) {
                    compareBase = Integer.valueOf(0);
                } else {
                    // look for PartyRelationship with partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is the group member, so the partyIdFrom is the groupPartyId
                    // and from/thru date within range
                    List<GenericValue>  partyRelationshipList = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", groupPartyId, "partyIdTo", partyId, "partyRelationshipTypeId", "GROUP_ROLLUP").cache(true).filterByDate().queryList();

                    if (UtilValidate.isNotEmpty(partyRelationshipList)) {
                        compareBase = Integer.valueOf(0);
                    } else {
                        compareBase = Integer.valueOf(checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyId));
                    }
                }
            }
        } else if ("PPIP_PARTY_CLASS".equals(inputParamEnumId)) {
            if (UtilValidate.isEmpty(partyId) || UtilValidate.isEmpty(condValue)) {
                compareBase = Integer.valueOf(1);
            } else {
                String partyClassificationGroupId = condValue;
                // find any PartyClassification
                // and from/thru date within range
                List<GenericValue> partyClassificationList = EntityQuery.use(delegator).from("PartyClassification").where("partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId).cache(true).filterByDate().queryList();
                // then 0 (equals), otherwise 1 (not equals)
                if (UtilValidate.isNotEmpty(partyClassificationList)) {
                    compareBase = Integer.valueOf(0);
                } else {
                    compareBase = Integer.valueOf(1);
                }
            }
        } else if ("PPIP_ROLE_TYPE".equals(inputParamEnumId)) {
            if (partyId != null && UtilValidate.isNotEmpty(condValue)) {
                // if a PartyRole exists for this partyId and the specified roleTypeId
                GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId, "roleTypeId", condValue).cache(true).queryOne();

                // then 0 (equals), otherwise 1 (not equals)
                if (partyRole != null) {
                    compareBase = Integer.valueOf(0);
                } else {
                    compareBase = Integer.valueOf(1);
                }
            } else {
                compareBase = Integer.valueOf(1);
            }
        } else if ("PPIP_ORDER_TOTAL".equals(inputParamEnumId)) {
            if (UtilValidate.isNotEmpty(condValue)) {
                BigDecimal orderSubTotal = cart.getSubTotalForPromotions();
                if (Debug.verboseOn()) Debug.logVerbose("Doing order total compare: orderSubTotal=" + orderSubTotal, module);
                compareBase = Integer.valueOf(orderSubTotal.compareTo(new BigDecimal(condValue)));
            }
        } else if ("PPIP_ORST_HIST".equals(inputParamEnumId)) {
            // description="Order sub-total X in last Y Months"
            if (partyId != null && userLogin != null && UtilValidate.isNotEmpty(condValue)) {
                // call the getOrderedSummaryInformation service to get the sub-total
                int monthsToInclude = 12;
                if (otherValue != null) {
                    monthsToInclude = Integer.parseInt(otherValue);
                }
                Map<String, Object> serviceIn = UtilMisc.<String, Object>toMap("partyId", partyId, "roleTypeId", "PLACING_CUSTOMER", "orderTypeId", "SALES_ORDER", "statusId", "ORDER_COMPLETED", "monthsToInclude", Integer.valueOf(monthsToInclude), "userLogin", userLogin);
                try {
                    Map<String, Object> result = dispatcher.runSync("getOrderedSummaryInformation", serviceIn);
                    if (ServiceUtil.isError(result)) {
                        Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_HIST ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module);
                        return false;
                    } else {
                        BigDecimal orderSubTotal = (BigDecimal) result.get("totalSubRemainingAmount");
                        BigDecimal orderSubTotalAndCartSubTotal = orderSubTotal.add(cart.getSubTotal()); 
                        if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for the last " + monthsToInclude + " months.", module);
                        compareBase = Integer.valueOf(orderSubTotalAndCartSubTotal.compareTo(new BigDecimal(condValue)));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module);
                    return false;
                }
            } else {
                return false;
            }
        } else if ("PPIP_ORST_YEAR".equals(inputParamEnumId)) {
            // description="Order sub-total X since beginning of current year"
            if (partyId != null && userLogin != null && UtilValidate.isNotEmpty(condValue)) {
                // call the getOrderedSummaryInformation service to get the sub-total
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(nowTimestamp);
                int monthsToInclude = calendar.get(Calendar.MONTH) + 1;
                Map<String, Object> serviceIn = UtilMisc.<String, Object>toMap("partyId", partyId,
                        "roleTypeId", "PLACING_CUSTOMER",
                        "orderTypeId", "SALES_ORDER",
                        "statusId", "ORDER_COMPLETED",
                        "monthsToInclude", Integer.valueOf(monthsToInclude),
                        "userLogin", userLogin);
                try {
                    Map<String, Object> result = dispatcher.runSync("getOrderedSummaryInformation", serviceIn);
                    if (ServiceUtil.isError(result)) {
                        Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_YEAR ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module);
                        return false;
                    } else {
                        BigDecimal orderSubTotal = (BigDecimal) result.get("totalSubRemainingAmount");
                        if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for the last " + monthsToInclude + " months.", module);
                        compareBase = Integer.valueOf(orderSubTotal.compareTo(new BigDecimal((condValue))));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module);
                    return false;
                }
            }
        } else if ("PPIP_ORST_LAST_YEAR".equals(inputParamEnumId)) {
            // description="Order sub-total X since beginning of last year"
            if (partyId != null && userLogin != null && UtilValidate.isNotEmpty(condValue)) {
                // call the getOrderedSummaryInformation service to get the sub-total

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(nowTimestamp);
                int lastYear = calendar.get(Calendar.YEAR) - 1;
                Calendar fromDateCalendar = Calendar.getInstance();
                fromDateCalendar.set(lastYear, 0, 0, 0, 0);
                Timestamp fromDate = new Timestamp(fromDateCalendar.getTime().getTime());
                Calendar thruDateCalendar = Calendar.getInstance();
                thruDateCalendar.set(lastYear, 12, 0, 0, 0);
                Timestamp thruDate = new Timestamp(thruDateCalendar.getTime().getTime());
                Map<String, Object> serviceIn = UtilMisc.toMap("partyId", partyId,
                        "roleTypeId", "PLACING_CUSTOMER",
                        "orderTypeId", "SALES_ORDER",
                        "statusId", "ORDER_COMPLETED",
                        "fromDate", fromDate,
                        "thruDate", thruDate,
                        "userLogin", userLogin);
                try {
                    Map<String, Object> result = dispatcher.runSync("getOrderedSummaryInformation", serviceIn);
                    if (ServiceUtil.isError(result)) {
                        Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_LAST_YEAR ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module);
                        return false;
                    } else {
                        Double orderSubTotal = (Double) result.get("totalSubRemainingAmount");
                        if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for last year.", module);
                        compareBase = Integer.valueOf(orderSubTotal.compareTo(Double.valueOf(condValue)));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module);
                    return false;
                }
            } else {
                return false;
            }
        } else if ("PPIP_RECURRENCE".equals(inputParamEnumId)) {
            if (UtilValidate.isNotEmpty(condValue)) {
                compareBase = Integer.valueOf(1);
                GenericValue recurrenceInfo = EntityQuery.use(delegator).from("RecurrenceInfo").where("recurrenceInfoId", condValue).cache().queryOne();
                if (recurrenceInfo != null) {
                    RecurrenceInfo recurrence = null;
                    try {
                        recurrence = new RecurrenceInfo(recurrenceInfo);
                    } catch (RecurrenceInfoException e) {
                        Debug.logError(e, module);
                    }

                    // check the current recurrence
                    if (recurrence != null) {
                        if (recurrence.isValidCurrent()) {
                            compareBase = Integer.valueOf(0);
                        }
                    }
                }
            }
        } else if ("PPIP_ORDER_SHIPTOTAL".equals(inputParamEnumId) && shippingMethod.equals(cart.getShipmentMethodTypeId()) && carrierPartyId.equals(cart.getCarrierPartyId())) {
            if (UtilValidate.isNotEmpty(condValue)) {
                BigDecimal orderTotalShipping = cart.getTotalShipping();
                if (Debug.verboseOn()) { Debug.logVerbose("Doing order total Shipping compare: ordertotalShipping=" + orderTotalShipping, module); }
                compareBase = orderTotalShipping.compareTo(new BigDecimal(condValue));
            }
        } else if ("PPIP_LPMUP_AMT".equals(inputParamEnumId)) {
            // does nothing on order level, only checked on item level, so ignore by always considering passed
            return true;
        } else if ("PPIP_LPMUP_PER".equals(inputParamEnumId)) {
            // does nothing on order level, only checked on item level, so ignore by always considering passed
            return true;
        } else {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderAnUnSupportedProductPromoCondInputParameterLhs", UtilMisc.toMap("inputParamEnumId",productPromoCond.getString("inputParamEnumId")), cart.getLocale()), module);
            return false;
        }

        if (Debug.verboseOn()) Debug.logVerbose("Condition compare done, compareBase=" + compareBase, module);

        if (compareBase != null) {
            int compare = compareBase.intValue();
            if ("PPC_EQ".equals(operatorEnumId)) {
                if (compare == 0) return true;
            } else if ("PPC_NEQ".equals(operatorEnumId)) {
                if (compare != 0) return true;
            } else if ("PPC_LT".equals(operatorEnumId)) {
                if (compare < 0) return true;
            } else if ("PPC_LTE".equals(operatorEnumId)) {
                if (compare <= 0) return true;
            } else if ("PPC_GT".equals(operatorEnumId)) {
                if (compare > 0) return true;
            } else if ("PPC_GTE".equals(operatorEnumId)) {
                if (compare >= 0) return true;
            } else {
                Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderAnUnSupportedProductPromoCondCondition", UtilMisc.toMap("operatorEnumId",operatorEnumId) , cart.getLocale()), module);
                return false;
            }
        }
        // default to not meeting the condition
        return false;
    }

    protected static boolean checkConditionsForItem(GenericValue productPromoActionOrCond, ShoppingCart cart, ShoppingCartItem cartItem, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        GenericValue productPromoRule = productPromoActionOrCond.getRelatedOne("ProductPromoRule", true);

        List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromoRule.get("productPromoId")).orderBy("productPromoCondSeqId").cache(true).queryList();
        productPromoConds = EntityUtil.filterByAnd(productPromoConds, UtilMisc.toMap("productPromoRuleId", productPromoRule.get("productPromoRuleId")));
        for (GenericValue productPromoCond: productPromoConds) {
            boolean passed = checkConditionForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp);
            if (!passed) return false;
        }
        return true;
    }

    protected static boolean checkConditionForItem(GenericValue productPromoCond, ShoppingCart cart, ShoppingCartItem cartItem, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        String condValue = productPromoCond.getString("condValue");
        // String otherValue = productPromoCond.getString("otherValue");
        String inputParamEnumId = productPromoCond.getString("inputParamEnumId");
        String operatorEnumId = productPromoCond.getString("operatorEnumId");

        // don't get list price from cart because it may have tax included whereas the base price does not: BigDecimal listPrice = cartItem.getListPrice();
        List<GenericValue> listProductPriceList = EntityQuery.use(delegator).from("ProductPrice")
                .where("productId", cartItem.getProductId(), "productPriceTypeId", "LIST_PRICE", "productPricePurposeId", "PURCHASE")
                .orderBy("-fromDate")
                .filterByDate()
                .queryList();
        GenericValue listProductPrice = (listProductPriceList != null && listProductPriceList.size() > 0) ? listProductPriceList.get(0): null;
        BigDecimal listPrice = (listProductPrice != null) ? listProductPrice.getBigDecimal("price") : null;

        if (listPrice == null) {
            // can't find a list price so this condition is meaningless, consider it passed
            return true;
        }

        BigDecimal basePrice = cartItem.getBasePrice();
        BigDecimal amountOff = listPrice.subtract(basePrice);
        BigDecimal percentOff = amountOff.divide(listPrice, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100L));

        Integer compareBase = null;

        if ("PPIP_LPMUP_AMT".equals(inputParamEnumId)) {
            // NOTE: only check this after we know it's this type of cond, otherwise condValue may not be a number
            BigDecimal condValueBigDecimal = new BigDecimal(condValue);
            compareBase = Integer.valueOf(amountOff.compareTo(condValueBigDecimal));
        } else if ("PPIP_LPMUP_PER".equals(inputParamEnumId)) {
            // NOTE: only check this after we know it's this type of cond, otherwise condValue may not be a number
            BigDecimal condValueBigDecimal = new BigDecimal(condValue);
            compareBase = Integer.valueOf(percentOff.compareTo(condValueBigDecimal));
        } else {
            // condition doesn't apply to individual item, always passes
            return true;
        }

        Debug.logInfo("Checking condition for item productId=" + cartItem.getProductId() + ", listPrice=" + listPrice + ", basePrice=" + basePrice + ", amountOff=" + amountOff + ", percentOff=" + percentOff + ", condValue=" + condValue + ", compareBase=" + compareBase + ", productPromoCond=" + productPromoCond, module);

        if (compareBase != null) {
            int compare = compareBase.intValue();
            if ("PPC_EQ".equals(operatorEnumId)) {
                if (compare == 0) return true;
            } else if ("PPC_NEQ".equals(operatorEnumId)) {
                if (compare != 0) return true;
            } else if ("PPC_LT".equals(operatorEnumId)) {
                if (compare < 0) return true;
            } else if ("PPC_LTE".equals(operatorEnumId)) {
                if (compare <= 0) return true;
            } else if ("PPC_GT".equals(operatorEnumId)) {
                if (compare > 0) return true;
            } else if ("PPC_GTE".equals(operatorEnumId)) {
                if (compare >= 0) return true;
            } else {
                Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderAnUnSupportedProductPromoCondCondition", UtilMisc.toMap("operatorEnumId",operatorEnumId) , cart.getLocale()), module);
                return false;
            }
            // was a compareBase and nothing returned above, so condition didn't pass, return false
            return false;
        }

        // no compareBase, this condition doesn't apply
        return true;
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

    public static class ActionResultInfo {
        public boolean ranAction = false;
        public BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        public BigDecimal quantityLeftInAction = BigDecimal.ZERO;
    }

    /** returns true if the cart was changed and rules need to be re-evaluted */
    protected static ActionResultInfo performAction(GenericValue productPromoAction, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException, CartItemModifyException {
        ActionResultInfo actionResultInfo = new ActionResultInfo();
        performAction(actionResultInfo, productPromoAction, cart, delegator, dispatcher, nowTimestamp);
        return actionResultInfo;
    }

    public static void performAction(ActionResultInfo actionResultInfo, GenericValue productPromoAction, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException, CartItemModifyException {

        String productPromoActionEnumId = productPromoAction.getString("productPromoActionEnumId");

        if ("PROMO_SERVICE".equals(productPromoActionEnumId)) {
            Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("productPromoAction", productPromoAction, "shoppingCart", cart, "nowTimestamp", nowTimestamp, "actionResultInfo", actionResultInfo);
            String serviceName = productPromoAction.getString("serviceName");
            Map<String, Object> actionResult;
            try {
                actionResult = dispatcher.runSync(serviceName, serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logError("Error calling promo action service [" + serviceName + "]", module);
                throw new CartItemModifyException("Error calling promo action service [" + serviceName + "]", e);
            }
            if (ServiceUtil.isError(actionResult)) {
                Debug.logError("Error calling promo action service [" + serviceName + "], result is: " + actionResult, module);
                throw new CartItemModifyException((String) actionResult.get(ModelService.ERROR_MESSAGE));
            }
            CartItemModifyException cartItemModifyException = (CartItemModifyException) actionResult.get("cartItemModifyException");
            if (cartItemModifyException != null) {
                throw cartItemModifyException;
            }
        } else if ("PROMO_GWP".equals(productPromoActionEnumId)) {
            String productStoreId = cart.getProductStoreId();

            // the code was in there for this, so even though I don't think we want to restrict this, just adding this flag to make it easy to change; could make option dynamic, but now implied by the use limit
            boolean allowMultipleGwp = true;

            Integer itemLoc = findPromoItem(productPromoAction, cart);
            if (!allowMultipleGwp && itemLoc != null) {
                if (Debug.verboseOn()) Debug.logVerbose("Not adding promo item, already there; action: " + productPromoAction, module);
                actionResultInfo.ranAction = false;
            } else {
                BigDecimal quantity;
                if (productPromoAction.get("quantity") != null) {
                    quantity = productPromoAction.getBigDecimal("quantity");
                } else {
                    if ("Y".equals(productPromoAction.get("useCartQuantity"))) {
                        quantity = BigDecimal.ZERO;
                        List<ShoppingCartItem> used = getCartItemsUsed(cart, productPromoAction);
                        for (ShoppingCartItem item : used) {
                            BigDecimal available = item.getPromoQuantityAvailable();
                            quantity = quantity.add(available).add(item.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction));
                            item.addPromoQuantityCandidateUse(available, productPromoAction, false);
                        }
                    } else {
                        quantity = BigDecimal.ZERO;
                    }
                }

                List<String> optionProductIds = new LinkedList<String>();
                String productId = productPromoAction.getString("productId");

                GenericValue product = null;
                if (UtilValidate.isNotEmpty(productId)) {
                    // Debug.logInfo("======== Got GWP productId [" + productId + "]", module);
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        String errMsg = "GWP Product not found with ID [" + productId + "] for ProductPromoAction [" + productPromoAction.get("productPromoId") + ":" + productPromoAction.get("productPromoRuleId") + ":" + productPromoAction.get("productPromoActionSeqId") + "]";
                        Debug.logError(errMsg, module);
                        throw new CartItemModifyException(errMsg);
                    }
                    if ("Y".equals(product.getString("isVirtual"))) {
                        List<GenericValue> productAssocs = EntityUtil.filterByDate(product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), UtilMisc.toList("sequenceNum"), true));
                        for (GenericValue productAssoc : productAssocs) {
                            optionProductIds.add(productAssoc.getString("productIdTo"));
                        }
                        productId = null;
                        product = null;
                        // Debug.logInfo("======== GWP productId [" + productId + "] is a virtual with " + productAssocs.size() + " variants", module);
                    } else {
                        // check inventory on this product, make sure it is available before going on
                        //NOTE: even though the store may not require inventory for purchase, we will always require inventory for gifts
                        try {
                            // get the quantity in cart for inventory check
                            BigDecimal quantityAlreadyInCart = BigDecimal.ZERO;
                            if (cart != null) {
                                List<ShoppingCartItem> matchingItems = cart.findAllCartItems(productId);
                                for (ShoppingCartItem item : matchingItems) {
                                    quantityAlreadyInCart = quantityAlreadyInCart.add(item.getQuantity());
                                }
                            }
                            Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "productId", productId, "product", product, "quantity", quantity.add(quantityAlreadyInCart)));
                            if (ServiceUtil.isError(invReqResult)) {
                                Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invReqResult, module);
                                throw new CartItemModifyException((String) invReqResult.get(ModelService.ERROR_MESSAGE));
                            } else if (!"Y".equals(invReqResult.get("available"))) {
                                Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderNotApplyingGwpBecauseProductIdIsOutOfStockForProductPromoAction", UtilMisc.toMap("productId", productId, "productPromoAction", productPromoAction), cart.getLocale()), module);
                                productId = null;
                                product = null;
                            }
                        } catch (GenericServiceException e) {
                            String errMsg = "Fatal error calling inventory checking services: " + e.toString();
                            Debug.logError(e, errMsg, module);
                            throw new CartItemModifyException(errMsg);
                        }
                    }
                }

                // support multiple gift options if products are attached to the action, or if the productId on the action is a virtual product
                Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp);
                if (productIds != null) {
                    optionProductIds.addAll(productIds);
                }

                // make sure these optionProducts have inventory...
                Iterator<String> optionProductIdIter = optionProductIds.iterator();
                while (optionProductIdIter.hasNext()) {
                    String optionProductId = optionProductIdIter.next();

                    try {
                        // get the quantity in cart for inventory check
                        BigDecimal quantityAlreadyInCart = BigDecimal.ZERO;
                        if (cart != null) {
                            List<ShoppingCartItem> matchingItems = cart.findAllCartItems(optionProductId);
                            for (ShoppingCartItem item : matchingItems) {
                                quantityAlreadyInCart = quantityAlreadyInCart.add(item.getQuantity());
                            }
                        }
                        Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "productId", optionProductId, "product", product, "quantity", quantity.add(quantityAlreadyInCart)));
                        if (ServiceUtil.isError(invReqResult)) {
                            Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invReqResult, module);
                            throw new CartItemModifyException((String) invReqResult.get(ModelService.ERROR_MESSAGE));
                        } else if (!"Y".equals(invReqResult.get("available"))) {
                            optionProductIdIter.remove();
                        }
                    } catch (GenericServiceException e) {
                        String errMsg = "Fatal error calling inventory checking services: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        throw new CartItemModifyException(errMsg);
                    }
                }

                // check to see if any desired productIds have been selected for this promo action
                String alternateGwpProductId = cart.getDesiredAlternateGiftByAction(productPromoAction.getPrimaryKey());
                if (UtilValidate.isNotEmpty(alternateGwpProductId)) {
                    // also check to make sure this isn't a spoofed ID somehow, check to see if it is in the Set
                    if (optionProductIds.contains(alternateGwpProductId)) {
                        if (UtilValidate.isNotEmpty(productId)) {
                            optionProductIds.add(productId);
                        }
                        optionProductIds.remove(alternateGwpProductId);
                        productId = alternateGwpProductId;
                        product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    } else {
                        Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderAnAlternateGwpProductIdWasInPlaceButWasEitherNotValidOrIsNoLongerInStockForId", UtilMisc.toMap("alternateGwpProductId",alternateGwpProductId), cart.getLocale()), module);
                    }
                }

                // if product is null, get one from the productIds set
                if (product == null && optionProductIds.size() > 0) {
                    // get the first from an iterator and remove it since it will be the current one
                    Iterator<String> optionProductIdTempIter = optionProductIds.iterator();
                    productId = optionProductIdTempIter.next();
                    optionProductIdTempIter.remove();
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                }

                if (product == null) {
                    // no product found to add as GWP, just return
                    return;
                }

                // pass null for cartLocation to add to end of cart, pass false for doPromotions to avoid infinite recursion
                ShoppingCartItem gwpItem = null;
                try {
                    // just leave the prodCatalogId null, this line won't be associated with a catalog
                    String prodCatalogId = null;
                    gwpItem = ShoppingCartItem.makeItem(null, product, null, quantity, null, null, null, null, null, null, null, null, prodCatalogId, null, null, null, dispatcher, cart, Boolean.FALSE, Boolean.TRUE, null, Boolean.FALSE, Boolean.FALSE);
                    if (optionProductIds.size() > 0) {
                        gwpItem.setAlternativeOptionProductIds(optionProductIds);
                    } else {
                        gwpItem.setAlternativeOptionProductIds(null);
                    }
                } catch (CartItemModifyException e) {
                    int gwpItemIndex = cart.getItemIndex(gwpItem);
                    cart.removeCartItem(gwpItemIndex, dispatcher);
                    throw e;
                }

                BigDecimal discountAmount = quantity.multiply(gwpItem.getBasePrice()).negate();

                doOrderItemPromoAction(productPromoAction, gwpItem, discountAmount, "amount", delegator);

                // set promo after create; note that to setQuantity we must clear this flag, setQuantity, then re-set the flag
                gwpItem.setIsPromo(true);
                if (Debug.verboseOn()) Debug.logVerbose("gwpItem adjustments: " + gwpItem.getAdjustments(), module);

                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = discountAmount;
            }
        } else if ("PROMO_FREE_SHIPPING".equals(productPromoActionEnumId)) {
            // this may look a bit funny: on each pass all rules that do free shipping will set their own rule id for it,
            // and on unapply if the promo and rule ids are the same then it will clear it; essentially on any pass
            // through the promos and rules if any free shipping should be there, it will be there
            cart.addFreeShippingProductPromoAction(productPromoAction);
            // don't consider this as a cart change?
            actionResultInfo.ranAction = true;
            // should probably set the totalDiscountAmount to something, but we have no idea what it will be, so leave at 0, will still get run
        } else if ("PROMO_PROD_DISC".equals(productPromoActionEnumId)) {
            BigDecimal quantityDesired = productPromoAction.get("quantity") == null ? BigDecimal.ONE : productPromoAction.getBigDecimal("quantity");
            BigDecimal startingQuantity = quantityDesired;
            BigDecimal discountAmountTotal = BigDecimal.ZERO;

            Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp);

            List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
            Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator();
            while (quantityDesired.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
                ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next();
                // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                GenericValue product = cartItem.getProduct();
                String parentProductId = cartItem.getParentProductId();
                boolean passedItemConds = checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp);
                // Debug.logInfo("Running promo action for cartItem " + cartItem.getName() + ", passedItemConds=" + passedItemConds + ", productPromoAction=" + productPromoAction, module);
                if (passedItemConds && !cartItem.getIsPromo() &&
                        (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                        (product == null || !"N".equals(product.getString("includeInPromotions")))) {
                    // reduce quantity still needed to qualify for promo (quantityNeeded)
                    BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false);
                    if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                        quantityDesired = quantityDesired.subtract(quantityUsed);

                        // create an adjustment and add it to the cartItem that implements the promotion action
                        BigDecimal percentModifier = productPromoAction.get("amount") == null ? BigDecimal.ZERO : productPromoAction.getBigDecimal("amount").movePointLeft(2);
                        BigDecimal lineAmount = quantityUsed.multiply(cartItem.getBasePrice()).multiply(cartItem.getRentalAdjustment());
                        BigDecimal discountAmount = lineAmount.multiply(percentModifier).negate();
                        discountAmountTotal = discountAmountTotal.add(discountAmount);
                        // not doing this any more, now distributing among conditions and actions (see call below): doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator);
                    }
                }
            }

            if (quantityDesired.compareTo(startingQuantity) == 0 || quantityDesired.compareTo(BigDecimal.ZERO) > 0) {
                // couldn't find any (or enough) cart items to give a discount to, don't consider action run
                actionResultInfo.ranAction = false;
                // clear out any action uses for this so they don't become part of anything else
                cart.resetPromoRuleUse(productPromoAction.getString("productPromoId"), productPromoAction.getString("productPromoRuleId"));
            } else {
                BigDecimal totalAmount = getCartItemsUsedTotalAmount(cart, productPromoAction);
                if (Debug.verboseOn()) Debug.logVerbose("Applying promo [" + productPromoAction.getPrimaryKey() + "]\n totalAmount=" + totalAmount + ", discountAmountTotal=" + discountAmountTotal, module);
                distributeDiscountAmount(discountAmountTotal, totalAmount, getCartItemsUsed(cart, productPromoAction), productPromoAction, delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = discountAmountTotal;
                actionResultInfo.quantityLeftInAction = quantityDesired;
            }
        } else if ("PROMO_PROD_AMDISC".equals(productPromoActionEnumId)) {
            BigDecimal quantityDesired = productPromoAction.get("quantity") == null ? BigDecimal.ONE : productPromoAction.getBigDecimal("quantity");
            BigDecimal startingQuantity = quantityDesired;
            BigDecimal discountAmountTotal = BigDecimal.ZERO;

            Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp);

            List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
            Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator();
            while (quantityDesired.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
                ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next();
                // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                String parentProductId = cartItem.getParentProductId();
                GenericValue product = cartItem.getProduct();
                boolean passedItemConds = checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp);
                if (passedItemConds && !cartItem.getIsPromo() &&
                        (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                        (product == null || !"N".equals(product.getString("includeInPromotions")))) {
                    // reduce quantity still needed to qualify for promo (quantityNeeded)
                    BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false);
                    quantityDesired = quantityDesired.subtract(quantityUsed);

                    // create an adjustment and add it to the cartItem that implements the promotion action
                    BigDecimal discount = productPromoAction.get("amount") == null ? BigDecimal.ZERO : productPromoAction.getBigDecimal("amount");
                    // don't allow the discount to be greater than the price
                    if (discount.compareTo(cartItem.getBasePrice().multiply(cartItem.getRentalAdjustment())) > 0) {
                        discount = cartItem.getBasePrice().multiply(cartItem.getRentalAdjustment());
                    }
                    BigDecimal discountAmount = quantityUsed.multiply(discount).negate();
                    discountAmountTotal = discountAmountTotal.add(discountAmount);
                    // not doing this any more, now distributing among conditions and actions (see call below): doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator);
                }
            }

            if (quantityDesired.compareTo(startingQuantity) == 0) {
                // couldn't find any cart items to give a discount to, don't consider action run
                actionResultInfo.ranAction = false;
            } else {
                BigDecimal totalAmount = getCartItemsUsedTotalAmount(cart, productPromoAction);
                if (Debug.verboseOn()) Debug.logVerbose("Applying promo [" + productPromoAction.getPrimaryKey() + "]\n totalAmount=" + totalAmount + ", discountAmountTotal=" + discountAmountTotal, module);
                distributeDiscountAmount(discountAmountTotal, totalAmount, getCartItemsUsed(cart, productPromoAction), productPromoAction, delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = discountAmountTotal;
                actionResultInfo.quantityLeftInAction = quantityDesired;
            }
        } else if ("PROMO_PROD_PRICE".equals(productPromoActionEnumId)) {
            // with this we want the set of used items to be one price, so total the price for all used items, subtract the amount we want them to cost, and create an adjustment for what is left
            BigDecimal quantityDesired = productPromoAction.get("quantity") == null ? BigDecimal.ONE : productPromoAction.getBigDecimal("quantity");
            BigDecimal desiredAmount = productPromoAction.get("amount") == null ? BigDecimal.ZERO : productPromoAction.getBigDecimal("amount");
            BigDecimal totalAmount = BigDecimal.ZERO;

            Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp);

            List<ShoppingCartItem> cartItemsUsed = new LinkedList<ShoppingCartItem>();
            List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false);
            Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator();
            while (quantityDesired.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
                ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next();
                // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
                String parentProductId = cartItem.getParentProductId();
                GenericValue product = cartItem.getProduct();
                boolean passedItemConds = checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp);
                if (passedItemConds && !cartItem.getIsPromo() && (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                        (product == null || !"N".equals(product.getString("includeInPromotions")))) {
                    // reduce quantity still needed to qualify for promo (quantityNeeded)
                    BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false);
                    if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                        quantityDesired = quantityDesired.subtract(quantityUsed);
                        totalAmount = totalAmount.add(quantityUsed.multiply(cartItem.getBasePrice()).multiply(cartItem.getRentalAdjustment()));
                        cartItemsUsed.add(cartItem);
                    }
                }
            }

            if (totalAmount.compareTo(desiredAmount) > 0 && quantityDesired.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal discountAmountTotal = totalAmount.subtract(desiredAmount).negate();
                distributeDiscountAmount(discountAmountTotal, totalAmount, cartItemsUsed, productPromoAction, delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = discountAmountTotal;
                // no use setting the quantityLeftInAction because that does not apply for buy X for $Y type promotions, it is all or nothing
            } else {
                actionResultInfo.ranAction = false;
                // clear out any action uses for this so they don't become part of anything else
                cart.resetPromoRuleUse(productPromoAction.getString("productPromoId"), productPromoAction.getString("productPromoRuleId"));
            }
        } else if ("PROMO_ORDER_PERCENT".equals(productPromoActionEnumId)) {
            BigDecimal percentage = (productPromoAction.get("amount") == null ? BigDecimal.ZERO : (productPromoAction.getBigDecimal("amount").movePointLeft(2))).negate();
            BigDecimal amount = cart.getSubTotalForPromotions().multiply(percentage);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                doOrderPromoAction(productPromoAction, cart, amount, "amount", delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = amount;
            }
        } else if ("PROMO_ORDER_AMOUNT".equals(productPromoActionEnumId)) {
            BigDecimal amount = (productPromoAction.get("amount") == null ? BigDecimal.ZERO : productPromoAction.getBigDecimal("amount")).negate();
            // if amount is greater than the order sub total, set equal to order sub total, this normally wouldn't happen because there should be a condition that the order total be above a certain amount, but just in case...
            BigDecimal subTotal = cart.getSubTotalForPromotions();
            if (amount.negate().compareTo(subTotal) > 0) {
                amount = subTotal.negate();
            }
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                doOrderPromoAction(productPromoAction, cart, amount, "amount", delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = amount;
            }
        } else if ("PROMO_PROD_SPPRC".equals(productPromoActionEnumId)) {
            // if there are productIds associated with the action then restrict to those productIds, otherwise apply for all products
            Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp);

            // go through the cart items and for each product that has a specialPromoPrice use that price
            for (ShoppingCartItem cartItem : cart.items()) {
                String itemProductId = cartItem.getProductId();
                if (UtilValidate.isEmpty(itemProductId)) {
                    continue;
                }

                if (productIds.size() > 0 && !productIds.contains(itemProductId)) {
                    continue;
                }

                if (cartItem.getSpecialPromoPrice() == null) {
                    continue;
                }

                // get difference between basePrice and specialPromoPrice and adjust for that
                BigDecimal difference = cartItem.getBasePrice().multiply(cartItem.getRentalAdjustment()).subtract(cartItem.getSpecialPromoPrice()).negate();

                if (difference.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(cartItem.getQuantity(), productPromoAction, false);
                    if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amount = difference.multiply(quantityUsed);
                        doOrderItemPromoAction(productPromoAction, cartItem, amount, "amount", delegator);
                        actionResultInfo.ranAction = true;
                        actionResultInfo.totalDiscountAmount = amount;
                    }
                }
            }
        } else if ("PROMO_SHIP_CHARGE".equals(productPromoActionEnumId)) {
            BigDecimal percentage = (productPromoAction.get("amount") == null ? BigDecimal.ZERO : (productPromoAction.getBigDecimal("amount").movePointLeft(2))).negate();
            BigDecimal amount = cart.getTotalShipping().multiply(percentage);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                int existingOrderPromoIndex = cart.getAdjustmentPromoIndex(productPromoAction.getString("productPromoId"));
                if (existingOrderPromoIndex != -1 && cart.getAdjustment(existingOrderPromoIndex).getBigDecimal("amount").compareTo(amount) == 0) {
                        actionResultInfo.ranAction = false;  // already ran, no need to repeat
                } else {
                    if (existingOrderPromoIndex != -1 && cart.getAdjustment(existingOrderPromoIndex).getBigDecimal("amount").compareTo(amount) != 0) {
                        cart.removeAdjustment(existingOrderPromoIndex);
                    }
                    doOrderPromoAction(productPromoAction, cart, amount, "amount", delegator);
                    actionResultInfo.ranAction = true;
                    actionResultInfo.totalDiscountAmount = amount;
                }
            }
        } else if ("PROMO_TAX_PERCENT".equals(productPromoActionEnumId)) {
            BigDecimal percentage = (productPromoAction.get("amount") == null ? BigDecimal.ZERO : (productPromoAction.getBigDecimal("amount").movePointLeft(2))).negate();
            BigDecimal amount = cart.getTotalSalesTax().multiply(percentage);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                doOrderPromoAction(productPromoAction, cart, amount, "amount", delegator);
                actionResultInfo.ranAction = true;
                actionResultInfo.totalDiscountAmount = amount;
            }
        } else {
            Debug.logError("An un-supported productPromoActionType was used: " + productPromoActionEnumId + ", not performing any action", module);
            actionResultInfo.ranAction = false;
        }

        // in action, if doesn't have enough quantity to use the promo at all, remove candidate promo uses and increment promoQuantityUsed; this should go for all actions, if any action runs we confirm
        if (actionResultInfo.ranAction) {
            cart.confirmPromoRuleUse(productPromoAction.getString("productPromoId"), productPromoAction.getString("productPromoRuleId"));
        } else {
            cart.resetPromoRuleUse(productPromoAction.getString("productPromoId"), productPromoAction.getString("productPromoRuleId"));
        }
    }

    protected static List<ShoppingCartItem> getCartItemsUsed(ShoppingCart cart, GenericValue productPromoAction) {
        List<ShoppingCartItem> cartItemsUsed = new LinkedList<ShoppingCartItem>();
        for (ShoppingCartItem cartItem : cart) {
            BigDecimal quantityUsed = cartItem.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction);
            if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                cartItemsUsed.add(cartItem);
            }
        }
        return cartItemsUsed;
    }

    protected static BigDecimal getCartItemsUsedTotalAmount(ShoppingCart cart, GenericValue productPromoAction) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : cart) {
            BigDecimal quantityUsed = cartItem.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction);
            if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = totalAmount.add(quantityUsed.multiply(cartItem.getBasePrice()));
            }
        }
        return totalAmount;
    }

    protected static void distributeDiscountAmount(BigDecimal discountAmountTotal, BigDecimal totalAmount, List<ShoppingCartItem> cartItemsUsed, GenericValue productPromoAction, Delegator delegator) {
        BigDecimal discountAmount = discountAmountTotal;
        // distribute the discount evenly weighted according to price over the order items that the individual quantities came from; avoids a number of issues with tax/shipping calc, inclusion in the sub-total for other promotions, etc
        Iterator<ShoppingCartItem> cartItemsUsedIter = cartItemsUsed.iterator();
        while (cartItemsUsedIter.hasNext()) {
            ShoppingCartItem cartItem = cartItemsUsedIter.next();
            // to minimize rounding issues use the remaining total for the last one, otherwise use a calculated value
            if (cartItemsUsedIter.hasNext()) {
                BigDecimal quantityUsed = cartItem.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction);
                BigDecimal ratioOfTotal = quantityUsed.multiply(cartItem.getBasePrice()).divide(totalAmount, generalRounding);
                BigDecimal weightedAmount = ratioOfTotal.multiply(discountAmountTotal);
                // round the weightedAmount to 3 decimal places, we don't want an exact number cents/whatever because this will be added up as part of a subtotal which will be rounded to 2 decimal places
                weightedAmount = weightedAmount.setScale(3, BigDecimal.ROUND_HALF_UP);
                discountAmount = discountAmount.subtract(weightedAmount);
                doOrderItemPromoAction(productPromoAction, cartItem, weightedAmount, "amount", delegator);
            } else {
                // last one, just use discountAmount
                doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator);
            }
        }
        // this is the old way that causes problems: doOrderPromoAction(productPromoAction, cart, discountAmount, "amount", delegator);
    }

    protected static Integer findPromoItem(GenericValue productPromoAction, ShoppingCart cart) {
        List<ShoppingCartItem> cartItems = cart.items();

        for (int i = 0; i < cartItems.size(); i++) {
            ShoppingCartItem checkItem = cartItems.get(i);

            if (checkItem.getIsPromo()) {
                // found a promo item, see if it has a matching adjustment on it
                Iterator<GenericValue> checkOrderAdjustments = UtilMisc.toIterator(checkItem.getAdjustments());
                while (checkOrderAdjustments != null && checkOrderAdjustments.hasNext()) {
                    GenericValue checkOrderAdjustment = checkOrderAdjustments.next();
                    if (productPromoAction.getString("productPromoId").equals(checkOrderAdjustment.get("productPromoId")) &&
                        productPromoAction.getString("productPromoRuleId").equals(checkOrderAdjustment.get("productPromoRuleId")) &&
                        productPromoAction.getString("productPromoActionSeqId").equals(checkOrderAdjustment.get("productPromoActionSeqId"))) {
                        return Integer.valueOf(i);
                    }
                }
            }
        }
        return null;
    }

    public static void doOrderItemPromoAction(GenericValue productPromoAction, ShoppingCartItem cartItem, BigDecimal amount, String amountField, Delegator delegator) {
        // round the amount before setting to make sure we don't get funny numbers in there
        // only round to 3 places, we need more specific amounts in adjustments so that they add up cleaner as part of the item subtotal, which will then be rounded
        amount = amount.setScale(3, rounding);
        GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment",
                UtilMisc.toMap("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT", amountField, amount,
                        "productPromoId", productPromoAction.get("productPromoId"),
                        "productPromoRuleId", productPromoAction.get("productPromoRuleId"),
                        "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId"),
                        "description", getProductPromoDescription((String) productPromoAction.get("productPromoId"), delegator)));

        // if an orderAdjustmentTypeId was included, override the default
        if (UtilValidate.isNotEmpty(productPromoAction.getString("orderAdjustmentTypeId"))) {
            orderAdjustment.set("orderAdjustmentTypeId", productPromoAction.get("orderAdjustmentTypeId"));
        }

        cartItem.addAdjustment(orderAdjustment);
    }

    public static void doOrderPromoAction(GenericValue productPromoAction, ShoppingCart cart, BigDecimal amount, String amountField, Delegator delegator) {
        // round the amount before setting to make sure we don't get funny numbers in there
        amount = amount.setScale(decimals, rounding);
        GenericValue orderAdjustment = delegator.makeValue("OrderAdjustment",
                UtilMisc.toMap("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT", amountField, amount,
                        "productPromoId", productPromoAction.get("productPromoId"),
                        "productPromoRuleId", productPromoAction.get("productPromoRuleId"),
                        "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId"),
                        "description", getProductPromoDescription((String) productPromoAction.get("productPromoId"), delegator)));

        // if an orderAdjustmentTypeId was included, override the default
        if (UtilValidate.isNotEmpty(productPromoAction.getString("orderAdjustmentTypeId"))) {
            orderAdjustment.set("orderAdjustmentTypeId", productPromoAction.get("orderAdjustmentTypeId"));
        }

        cart.addAdjustment(orderAdjustment);
    }

    private static String getProductPromoDescription(String prodPromoId, Delegator delegator) {
        // get the promoText / promoName to set as a descr of the orderAdj
        GenericValue prodPromo;
        try {
            prodPromo = EntityQuery.use(delegator).from("ProductPromo").where("productPromoId", prodPromoId).cache().queryOne();
            if (UtilValidate.isNotEmpty(prodPromo.get("promoText"))) {
                return (String) prodPromo.get("promoText");
            }
            return (String) prodPromo.get("promoName");

        } catch (GenericEntityException e) {
            Debug.logWarning("Error getting ProductPromo for Id " + prodPromoId, module);
        }

        return null;
    }

    protected static Integer findAdjustment(GenericValue productPromoAction, List<GenericValue> adjustments) {
        for (int i = 0; i < adjustments.size(); i++) {
            GenericValue checkOrderAdjustment = adjustments.get(i);

            if (productPromoAction.getString("productPromoId").equals(checkOrderAdjustment.get("productPromoId")) &&
                productPromoAction.getString("productPromoRuleId").equals(checkOrderAdjustment.get("productPromoRuleId")) &&
                productPromoAction.getString("productPromoActionSeqId").equals(checkOrderAdjustment.get("productPromoActionSeqId"))) {
                return Integer.valueOf(i);
            }
        }
        return null;
    }

    public static Set<String> getPromoRuleCondProductIds(GenericValue productPromoCond, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        // get a cached list for the whole promo and filter it as needed, this for better efficiency in caching
        List<GenericValue> productPromoCategoriesAll = EntityQuery.use(delegator).from("ProductPromoCategory").where("productPromoId", productPromoCond.get("productPromoId")).cache(true).queryList();
        List<GenericValue> productPromoCategories = EntityUtil.filterByAnd(productPromoCategoriesAll, UtilMisc.toMap("productPromoRuleId", "_NA_", "productPromoCondSeqId", "_NA_"));
        productPromoCategories.addAll(EntityUtil.filterByAnd(productPromoCategoriesAll, UtilMisc.toMap("productPromoRuleId", productPromoCond.get("productPromoRuleId"), "productPromoCondSeqId", productPromoCond.get("productPromoCondSeqId"))));

        List<GenericValue> productPromoProductsAll = EntityQuery.use(delegator).from("ProductPromoProduct").where("productPromoId", productPromoCond.get("productPromoId")).cache(true).queryList();
        List<GenericValue> productPromoProducts = EntityUtil.filterByAnd(productPromoProductsAll, UtilMisc.toMap("productPromoRuleId", "_NA_", "productPromoCondSeqId", "_NA_"));
        productPromoProducts.addAll(EntityUtil.filterByAnd(productPromoProductsAll, UtilMisc.toMap("productPromoRuleId", productPromoCond.get("productPromoRuleId"), "productPromoCondSeqId", productPromoCond.get("productPromoCondSeqId"))));

        Set<String> productIds = new HashSet<String>();
        makeProductPromoIdSet(productIds, productPromoCategories, productPromoProducts, delegator, nowTimestamp, false);
        return productIds;
    }

    public static Set<String> getPromoRuleActionProductIds(GenericValue productPromoAction, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        // get a cached list for the whole promo and filter it as needed, this for better efficiency in caching
        List<GenericValue> productPromoCategoriesAll = EntityQuery.use(delegator).from("ProductPromoCategory").where("productPromoId", productPromoAction.get("productPromoId")).cache(true).queryList();
        List<GenericValue> productPromoCategories = EntityUtil.filterByAnd(productPromoCategoriesAll, UtilMisc.toMap("productPromoRuleId", "_NA_", "productPromoActionSeqId", "_NA_"));
        productPromoCategories.addAll(EntityUtil.filterByAnd(productPromoCategoriesAll, UtilMisc.toMap("productPromoRuleId", productPromoAction.get("productPromoRuleId"), "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId"))));

        List<GenericValue> productPromoProductsAll = EntityQuery.use(delegator).from("ProductPromoProduct").where("productPromoId", productPromoAction.get("productPromoId")).cache(true).queryList();
        List<GenericValue> productPromoProducts = EntityUtil.filterByAnd(productPromoProductsAll, UtilMisc.toMap("productPromoRuleId", "_NA_", "productPromoActionSeqId", "_NA_"));
        productPromoProducts.addAll(EntityUtil.filterByAnd(productPromoProductsAll, UtilMisc.toMap("productPromoRuleId", productPromoAction.get("productPromoRuleId"), "productPromoActionSeqId", productPromoAction.get("productPromoActionSeqId"))));

        Set<String> productIds = new HashSet<String>();
        makeProductPromoIdSet(productIds, productPromoCategories, productPromoProducts, delegator, nowTimestamp, false);
        return productIds;
    }

    public static void makeProductPromoIdSet(Set<String> productIds, List<GenericValue> productPromoCategories, List<GenericValue> productPromoProducts, Delegator delegator, Timestamp nowTimestamp, boolean filterOldProducts) throws GenericEntityException {
        // do the includes
        handleProductPromoCategories(productIds, productPromoCategories, "PPPA_INCLUDE", delegator, nowTimestamp);
        handleProductPromoProducts(productIds, productPromoProducts, "PPPA_INCLUDE");

        // do the excludes
        handleProductPromoCategories(productIds, productPromoCategories, "PPPA_EXCLUDE", delegator, nowTimestamp);
        handleProductPromoProducts(productIds, productPromoProducts, "PPPA_EXCLUDE");

        // do the always includes
        handleProductPromoCategories(productIds, productPromoCategories, "PPPA_ALWAYS", delegator, nowTimestamp);
        handleProductPromoProducts(productIds, productPromoProducts, "PPPA_ALWAYS");
    }

    public static void makeProductPromoCondActionIdSets(String productPromoId, Set<String> productIdsCond, Set<String> productIdsAction, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        makeProductPromoCondActionIdSets(productPromoId, productIdsCond, productIdsAction, delegator, nowTimestamp, false);
    }

    public static void makeProductPromoCondActionIdSets(String productPromoId, Set<String> productIdsCond, Set<String> productIdsAction, Delegator delegator, Timestamp nowTimestamp, boolean filterOldProducts) throws GenericEntityException {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        List<GenericValue> productPromoCategoriesAll = EntityQuery.use(delegator).from("ProductPromoCategory").where("productPromoId", productPromoId).cache(true).queryList();
        List<GenericValue> productPromoProductsAll = EntityQuery.use(delegator).from("ProductPromoProduct").where("productPromoId", productPromoId).cache(true).queryList();

        List<GenericValue> productPromoProductsCond = new LinkedList<GenericValue>();
        List<GenericValue> productPromoCategoriesCond = new LinkedList<GenericValue>();
        List<GenericValue> productPromoProductsAction = new LinkedList<GenericValue>();
        List<GenericValue> productPromoCategoriesAction = new LinkedList<GenericValue>();

        for (GenericValue productPromoProduct : productPromoProductsAll) {
            // if the rule id is null then this is a global promo one, so always include
            if (!"_NA_".equals(productPromoProduct.getString("productPromoCondSeqId")) || "_NA_".equals(productPromoProduct.getString("productPromoRuleId"))) {
                productPromoProductsCond.add(productPromoProduct);
            }
            if (!"_NA_".equals(productPromoProduct.getString("productPromoActionSeqId")) || "_NA_".equals(productPromoProduct.getString("productPromoRuleId"))) {
                productPromoProductsAction.add(productPromoProduct);
            }
        }
        for (GenericValue productPromoCategory : productPromoCategoriesAll) {
            if (!"_NA_".equals(productPromoCategory.getString("productPromoCondSeqId")) || "_NA_".equals(productPromoCategory.getString("productPromoRuleId"))) {
                productPromoCategoriesCond.add(productPromoCategory);
            }
            if (!"_NA_".equals(productPromoCategory.getString("productPromoActionSeqId")) || "_NA_".equals(productPromoCategory.getString("productPromoRuleId"))) {
                productPromoCategoriesAction.add(productPromoCategory);
            }
        }

        makeProductPromoIdSet(productIdsCond, productPromoCategoriesCond, productPromoProductsCond, delegator, nowTimestamp, filterOldProducts);
        makeProductPromoIdSet(productIdsAction, productPromoCategoriesAction, productPromoProductsAction, delegator, nowTimestamp, filterOldProducts);

        // last of all filterOldProducts, done here to make sure no product gets looked up twice
        if (filterOldProducts) {
            Iterator<String> productIdsCondIter = productIdsCond.iterator();
            while (productIdsCondIter.hasNext()) {
                String productId = productIdsCondIter.next();
                if (isProductOld(productId, delegator, nowTimestamp)) {
                    productIdsCondIter.remove();
                }
            }
            Iterator<String> productIdsActionIter = productIdsAction.iterator();
            while (productIdsActionIter.hasNext()) {
                String productId = productIdsActionIter.next();
                if (isProductOld(productId, delegator, nowTimestamp)) {
                    productIdsActionIter.remove();
                }
            }
        }
    }

    protected static boolean isProductOld(String productId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        if (product != null) {
            Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
            if (salesDiscontinuationDate != null && salesDiscontinuationDate.before(nowTimestamp)) {
                return true;
            }
        }
        return false;
    }

    protected static void handleProductPromoCategories(Set<String> productIds, List<GenericValue> productPromoCategories, String productPromoApplEnumId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        boolean include = !"PPPA_EXCLUDE".equals(productPromoApplEnumId);
        Set<String> productCategoryIds = new HashSet<String>();
        Map<String, List<Set<String>>> productCategoryGroupSetListMap = new HashMap<String, List<Set<String>>>();

        for (GenericValue productPromoCategory : productPromoCategories) {
            if (productPromoApplEnumId.equals(productPromoCategory.getString("productPromoApplEnumId"))) {
                Set<String> tempCatIdSet = new HashSet<String>();
                if ("Y".equals(productPromoCategory.getString("includeSubCategories"))) {
                    ProductSearch.getAllSubCategoryIds(productPromoCategory.getString("productCategoryId"), tempCatIdSet, delegator, nowTimestamp);
                } else {
                    tempCatIdSet.add(productPromoCategory.getString("productCategoryId"));
                }

                String andGroupId = productPromoCategory.getString("andGroupId");
                if ("_NA_".equals(andGroupId)) {
                    productCategoryIds.addAll(tempCatIdSet);
                } else {
                    List<Set<String>> catIdSetList = productCategoryGroupSetListMap.get(andGroupId);
                    if (catIdSetList == null) {
                        catIdSetList = new LinkedList<Set<String>>();
                    }
                    catIdSetList.add(tempCatIdSet);
                }
            }
        }

        // for the ones with andGroupIds, if there is only one category move it to the productCategoryIds Set
        // also remove all empty SetLists and Sets
        Iterator<Map.Entry<String, List<Set<String>>>> pcgslmeIter = productCategoryGroupSetListMap.entrySet().iterator();
        while (pcgslmeIter.hasNext()) {
            Map.Entry<String, List<Set<String>>> entry = pcgslmeIter.next();
            List<Set<String>> catIdSetList = entry.getValue();
            if (catIdSetList.size() == 0) {
                pcgslmeIter.remove();
            } else if (catIdSetList.size() == 1) {
                Set<String> catIdSet = catIdSetList.iterator().next();
                if (catIdSet.size() == 0) {
                    pcgslmeIter.remove();
                } else {
                    // if there is only one set in the list since the set will be or'ed anyway, just add them all to the productCategoryIds Set
                    productCategoryIds.addAll(catIdSet);
                    pcgslmeIter.remove();
                }
            }
        }

        // now that the category Set and Map are setup, take care of the productCategoryIds Set first
        getAllProductIds(productCategoryIds, productIds, delegator, nowTimestamp, include);

        // now handle the productCategoryGroupSetListMap
        // if a set has more than one category (because of an include sub-cats) then do an or
        // all lists will have more than category because of the pre-pass that was done, so and them together
        for (Map.Entry<String, List<Set<String>>> entry : productCategoryGroupSetListMap.entrySet()) {
            List<Set<String>> catIdSetList = entry.getValue();
            // get all productIds for this catIdSetList
            List<Set<String>> productIdSetList = new LinkedList<Set<String>>();

            for (Set<String> catIdSet : catIdSetList) {
                // make a Set of productIds including all ids from all categories
                Set<String> groupProductIdSet = new HashSet<String>();
                getAllProductIds(catIdSet, groupProductIdSet, delegator, nowTimestamp, true);
                productIdSetList.add(groupProductIdSet);
            }

            // now go through all productId sets and only include IDs that are in all sets
            // by definition if each id must be in all categories, then it must be in the first, so go through the first and drop each one that is not in all others
            Set<String> firstProductIdSet = productIdSetList.remove(0);
            for (Set<String> productIdSet : productIdSetList) {
                firstProductIdSet.retainAll(productIdSet);
            }

            /* the old way of doing it, not as efficient, recoded above using the retainAll operation, pretty handy
            Iterator firstProductIdIter = firstProductIdSet.iterator();
            while (firstProductIdIter.hasNext()) {
                String curProductId = (String) firstProductIdIter.next();

                boolean allContainProductId = true;
                Iterator productIdSetIter = productIdSetList.iterator();
                while (productIdSetIter.hasNext()) {
                    Set productIdSet = (Set) productIdSetIter.next();
                    if (!productIdSet.contains(curProductId)) {
                        allContainProductId = false;
                        break;
                    }
                }

                if (!allContainProductId) {
                    firstProductIdIter.remove();
                }
            }
             */

            if (firstProductIdSet.size() >= 0) {
                if (include) {
                    productIds.addAll(firstProductIdSet);
                } else {
                    productIds.removeAll(firstProductIdSet);
                }
            }
        }
    }

    protected static void getAllProductIds(Set<String> productCategoryIdSet, Set<String> productIdSet, Delegator delegator, Timestamp nowTimestamp, boolean include) throws GenericEntityException {
        for (String productCategoryId : productCategoryIdSet) {
            // get all product category memebers, filter by date
            List<GenericValue> productCategoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", productCategoryId).cache(true).filterByDate(nowTimestamp).queryList();
            for (GenericValue productCategoryMember : productCategoryMembers) {
                String productId = productCategoryMember.getString("productId");
                if (include) {
                    productIdSet.add(productId);
                } else {
                    productIdSet.remove(productId);
                }
            }
        }
    }

    protected static void handleProductPromoProducts(Set<String> productIds, List<GenericValue> productPromoProducts, String productPromoApplEnumId) throws GenericEntityException {
        boolean include = !"PPPA_EXCLUDE".equals(productPromoApplEnumId);
        for (GenericValue productPromoProduct : productPromoProducts) {
            if (productPromoApplEnumId.equals(productPromoProduct.getString("productPromoApplEnumId"))) {
                String productId = productPromoProduct.getString("productId");
                if (include) {
                    productIds.add(productId);
                } else {
                    productIds.remove(productId);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class UseLimitException extends Exception {
        public UseLimitException(String str) {
            super(str);
        }
    }
}
