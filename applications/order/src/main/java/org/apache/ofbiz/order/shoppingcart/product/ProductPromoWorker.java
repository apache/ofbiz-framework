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
package org.apache.ofbiz.order.shoppingcart.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
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
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.ProductPromoUseInfo;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.product.product.ProductSearch;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ProductPromoWorker - Worker class for catalog/product promotion related functionality
 */
public final class ProductPromoWorker {

    public static final String module = ProductPromoWorker.class.getName();
    private static final String resource = "OrderUiLabels";
    private static final String resource_error = "OrderErrorUiLabels";

    private static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode rounding = UtilNumber.getRoundingMode("order.rounding");

    private static final MathContext generalRounding = new MathContext(10);

    private ProductPromoWorker() {}

    public static List<GenericValue> getStoreProductPromos(Delegator delegator, LocalDispatcher dispatcher, ServletRequest request) {
        List<GenericValue> productPromos = new LinkedList<>();
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

            Iterator<GenericValue> productStorePromoAppls = UtilMisc.toIterator(EntityUtil.filterByDate(productStore.getRelated("ProductStorePromoAppl", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("sequenceNum"), true), true));
            while (productStorePromoAppls != null && productStorePromoAppls.hasNext()) {
                GenericValue productStorePromoAppl = productStorePromoAppls.next();
                if (UtilValidate.isNotEmpty(productStorePromoAppl.getString("manualOnly")) && "Y".equals(productStorePromoAppl.getString("manualOnly"))) {
                    // manual only promotions are not automatically evaluated (they must be explicitly selected by the user)
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Skipping promotion with id [" + productStorePromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                    }
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
                    if (!condResult) {
                        productPromo = null;
                    }
                }
                if (productPromo != null) {
                    productPromos.add(productPromo);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromos;
    }

    public static Set<String> getStoreProductPromoCodes(ShoppingCart cart) {
        Set<String> promoCodes = new HashSet<>();
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
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Skipping promotion with id [" + productStorePromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                    }
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
        List<GenericValue> productPromoList = new LinkedList<>();

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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Not doing promotions, none applied to store with ID " + productStoreId, module);
                }
            }

            for (GenericValue prodCatalogPromoAppl : productStorePromoApplsList) {
                if ("Y".equals(prodCatalogPromoAppl.getString("manualOnly"))) {
                    // manual only promotions are not automatically evaluated (they must be explicitly selected by the user)
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Skipping promotion with id [" + prodCatalogPromoAppl.getString("productPromoId") + "] because it is applied to the store with ID " + productStoreId + " as a manual only promotion.", module);
                    }
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
        List<GenericValue> productPromoList = new LinkedList<>();

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

            if (Debug.verboseOn() && UtilValidate.isEmpty(agreementPromoApplsList)) {
                    Debug.logVerbose("Not doing promotions, none applied to agreement with ID " + agreementId, module);
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
                if ("SALES_ORDER".equals(cart.getOrderType())) {
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
            List<ProductPromoUseInfo> sortedPromoUses = new ArrayList<>();
            while (promoUses.hasNext()) {
                ProductPromoUseInfo promoUse = promoUses.next();
                sortedPromoUses.add(promoUse);
            }
            Collections.sort(sortedPromoUses);
            List<GenericValue> sortedExplodedProductPromoList = new ArrayList<>(sortedPromoUses.size());
            Map<String, Long> usesPerPromo = new HashMap<>();
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
        } catch (GeneralException e) {
            Debug.logError(e, "Error running promotions, will ignore: " + e.toString(), module);
        }
    }

    private static boolean hasOrderTotalCondition(GenericValue productPromo, Delegator delegator) throws GenericEntityException {
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

    private static void runProductPromos(List<GenericValue> productPromoList, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp, boolean isolatedTestRun) throws GeneralException {
        String partyId = cart.getPartyId();

        // this is our safety net; we should never need to loop through the rules more than a certain number of times, this is that number and may have to be changed for insanely large promo sets...
        long maxIterations = 1000;
        // part of the safety net to avoid infinite iteration
        long numberOfIterations = 0;

        // set a max limit on how many times each promo can be run, for cases where there is no use limit this will be the use limit
        //default to 2 times the number of items in the cart
        long maxUseLimit = cart.getTotalQuantity().multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.CEILING).longValue();
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
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Running promotion [" + productPromoId + "], useLimit=" + useLimit + ", # of rules=" + productPromoRules.size(), module);
                        }

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
                                while ((useLimit == null || useLimit > cart.getProductPromoUseCount(productPromoId)) && productPromoCodeIter.hasNext()) {
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
            candidateUseLimit = useLimitPerOrder;
        }

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
            long perCustomerThisOrder = useLimitPerCustomer - productPromoCustomerUseSize;
            if (candidateUseLimit == null || candidateUseLimit > perCustomerThisOrder) {
                candidateUseLimit = perCustomerThisOrder;
            }
        }

        Long useLimitPerPromotion = productPromo.getLong("useLimitPerPromotion");
        if (useLimitPerPromotion != null) {
            // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
            long productPromoUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            long perPromotionThisOrder = useLimitPerPromotion - productPromoUseSize;
            if (candidateUseLimit == null || candidateUseLimit > perPromotionThisOrder) {
                candidateUseLimit = perPromotionThisOrder;
            }
        }

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
            long perCustomerThisOrder = codeUseLimitPerCustomer - productPromoCustomerUseSize;
            codeUseLimit = perCustomerThisOrder;
        }

        Long codeUseLimitPerCode = productPromoCode.getLong("useLimitPerCode");
        if (codeUseLimitPerCode != null) {
            // check to see how many times this has been used for other orders for this customer, the remainder is the limit for this order
            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productPromoCodeId", EntityOperator.EQUALS, productPromoCodeId),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityOperator.AND);
            long productPromoCodeUseSize = EntityQuery.use(delegator).from("ProductPromoUseCheck").where(checkCondition).queryCount();
            long perCodeThisOrder = codeUseLimitPerCode - productPromoCodeUseSize;
            if (codeUseLimit == null || codeUseLimit > perCodeThisOrder) {
                codeUseLimit = perCodeThisOrder;
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
                    List<EntityCondition> validEmailCondList = new LinkedList<>();
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
            if (useLimit != null && useLimit <= 0) {
                return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_maximum_limit", UtilMisc.toMap("productPromoCodeId", productPromoCodeId), locale);
            }

            return null;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up ProductPromoCode", module);
            return UtilProperties.getMessage(resource_error, "productpromoworker.promotion_code_error_lookup", UtilMisc.toMap("productPromoCodeId", productPromoCodeId, "errorMsg", e.toString()), locale);
        }
    }

    public static String makeAutoDescription(GenericValue productPromo, Delegator delegator, Locale locale, LocalDispatcher dispatcher) throws GenericEntityException {
        if (productPromo == null) {
            return "";
        }
        List<String> partyClassificationsIncluded = new ArrayList<>();
        List<String> partyClassificationsExcluded = new ArrayList<>();
        StringBuilder promoDescBuf = new StringBuilder();
        List<GenericValue> productPromoRules = productPromo.getRelated("ProductPromoRule", null, null, true);
        Iterator<GenericValue> promoRulesIter = productPromoRules.iterator();
        while (promoRulesIter != null && promoRulesIter.hasNext()) {
            GenericValue productPromoRule = promoRulesIter.next();

            List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromo.get("productPromoId")).orderBy("productPromoCondSeqId").cache(true).queryList();
            productPromoConds = EntityUtil.filterByAnd(productPromoConds, UtilMisc.toMap("productPromoRuleId", productPromoRule.get("productPromoRuleId")));
            // using the other method to consolidate cache entries because the same cache is used elsewhere: List productPromoConds = productPromoRule.getRelated("ProductPromoCond", null, UtilMisc.toList("productPromoCondSeqId"), true);
            Iterator<GenericValue> productPromoCondIter = UtilMisc.toIterator(productPromoConds);
            while (productPromoCondIter != null && productPromoCondIter.hasNext()) {
                GenericValue productPromoCond = productPromoCondIter.next();

                String equalityOperator = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoOperatorEquality." + productPromoCond.getString("operatorEnumId"), locale);
                String quantityOperator = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoOperatorQuantity." + productPromoCond.getString("operatorEnumId"), locale);

                String condValue = "invalid";
                if (UtilValidate.isNotEmpty(productPromoCond.getString("condValue"))) {
                    condValue = productPromoCond.getString("condValue");
                }

                Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("condValue", condValue, "equalityOperator", equalityOperator, "quantityOperator", quantityOperator);

                if ("PPIP_PARTY_CLASS".equalsIgnoreCase(productPromoCond.getString("inputParamEnumId")) || "PPC_PARTY_CLASS".equalsIgnoreCase(productPromoCond.getString("customMethodId"))) {
                    GenericValue partyClassificationGroup = EntityQuery.use(delegator).from("PartyClassificationGroup").where("partyClassificationGroupId", condValue).cache(true).queryOne();
                    if (partyClassificationGroup != null && UtilValidate.isNotEmpty(partyClassificationGroup.getString("description"))) {
                        condValue = partyClassificationGroup.getString("description");
                    }

                    if ("PPC_EQ".equalsIgnoreCase(productPromoCond.getString("operatorEnumId"))) {
                        partyClassificationsIncluded.add(condValue);
                    }
                    if ("PPC_NEQ".equalsIgnoreCase(productPromoCond.getString("operatorEnumId"))) {
                        partyClassificationsExcluded.add(condValue);
                    }
                } else {
                    String enumId = null;
                    if (UtilValidate.isNotEmpty(productPromoCond.getString("customMethodId"))) {
                        GenericValue enumeration = EntityQuery.use(delegator).from("Enumeration").where("enumCode", productPromoCond.getString("customMethodId")).cache().queryFirst();
                        if (enumeration != null) {
                            enumId = enumeration.getString("enumId");
                        }
                    } else {
                        enumId = productPromoCond.getString("inputParamEnumId");
                    }

                    if (UtilValidate.isNotEmpty(productPromoCond.getString("otherValue"))) {
                        messageContext.put("otherValue", productPromoCond.getString("otherValue"));
                    }
                    String msgProp = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoCondition." + enumId, messageContext, locale);
                    promoDescBuf.append(msgProp);
                    promoDescBuf.append(" ");

                    if (promoRulesIter.hasNext()) {
                        promoDescBuf.append(" and ");
                    }
                }
            }

            List<GenericValue> productPromoActions = productPromoRule.getRelated("ProductPromoAction", null, UtilMisc.toList("productPromoActionSeqId"), true);
            Iterator<GenericValue> productPromoActionIter = UtilMisc.toIterator(productPromoActions);
            while (productPromoActionIter != null && productPromoActionIter.hasNext()) {
                GenericValue productPromoAction = productPromoActionIter.next();

                String productId = productPromoAction.getString("productId");

                Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("quantity", productPromoAction.get("quantity"), "amount", productPromoAction.get("amount"), "productId", productId, "partyId", productPromoAction.get("partyId"));

                if (UtilValidate.isEmpty(messageContext.get("productId"))) {
                    messageContext.put("productId", "any");
                }
                if (UtilValidate.isEmpty(messageContext.get("partyId"))) {
                    messageContext.put("partyId", "any");
                }
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                if (product != null) {
                    messageContext.put("productName", ProductContentWrapper.getProductContentAsText(product, "PRODUCT_NAME", locale, dispatcher, "html"));
                }

                String enumId = null;
                if (UtilValidate.isNotEmpty(productPromoAction.getString("customMethodId"))) {
                    GenericValue enumeration = EntityQuery.use(delegator).from("Enumeration").where("enumCode", productPromoAction.getString("customMethodId")).cache().queryFirst();
                    if (enumeration != null) {
                        enumId = enumeration.getString("enumId");
                    }
                } else {
                    enumId = productPromoAction.getString("productPromoActionEnumId");
                }

                String msgProp = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoAction." + enumId, messageContext, locale);
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
            if (promoDescBuf.charAt(promoDescBuf.length() - 1) == ' ') {
                promoDescBuf.deleteCharAt(promoDescBuf.length() - 1);
            }
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

        if (UtilValidate.isNotEmpty(partyClassificationsIncluded)) {
            Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("partyClassificationsIncluded", partyClassificationsIncluded);
            String msgProp = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoCondition.PPIP_PARTY_CLASS.APPLIED", messageContext, locale);
            promoDescBuf.append("\n" + msgProp);
        }

        if (UtilValidate.isNotEmpty(partyClassificationsExcluded)) {
            Map<String, Object> messageContext = UtilMisc.<String, Object>toMap("partyClassificationsExcluded", partyClassificationsExcluded);
            String msgProp = UtilProperties.getMessage("ProductPromoUiLabels", "ProductPromoCondition.PPIP_PARTY_CLASS.NOT_APPLIED", messageContext, locale);
            promoDescBuf.append("\n" + msgProp);
        }

        return promoDescBuf.toString();
    }

    private static boolean runProductPromoRules(ShoppingCart cart, Long useLimit, boolean requireCode, String productPromoCodeId, Long codeUseLimit, long maxUseLimit,
        GenericValue productPromo, List<GenericValue> productPromoRules, LocalDispatcher dispatcher, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException, UseLimitException {
        boolean cartChanged = false;
        Map<ShoppingCartItem,BigDecimal> usageInfoMap = prepareProductUsageInfoMap(cart);
        String productPromoId = productPromo.getString("productPromoId");
        while ((useLimit == null || useLimit > cart.getProductPromoUseCount(productPromoId)) &&
                (!requireCode || UtilValidate.isNotEmpty(productPromoCodeId)) &&
                (codeUseLimit == null || codeUseLimit > cart.getProductPromoCodeUse(productPromoCodeId))) {
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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Checking " + productPromoConds.size() + " conditions for rule " + productPromoRule, module);
                }

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
        Map<ShoppingCartItem,BigDecimal> usageInfoMap = new HashMap<>();
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
        Map<ShoppingCartItem,BigDecimal> deltaUsageInfoMap = new HashMap<>(newMap);

        for (Entry<ShoppingCartItem, BigDecimal> entry : oldMap.entrySet()) {
            ShoppingCartItem key = entry.getKey();
            BigDecimal oldUsed = entry.getValue();
            BigDecimal newUsed = entry.getValue();
            if (newUsed.compareTo(oldUsed) > 0) {
                deltaUsageInfoMap.put(key, newUsed.add(oldUsed.negate()));
            } else {
                deltaUsageInfoMap.remove(key);
            }
        }
        return deltaUsageInfoMap;
    }

    private static boolean checkCondition(GenericValue productPromoCond, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        String condValue = productPromoCond.getString("condValue");
        String otherValue = productPromoCond.getString("otherValue");
        String inputParamEnumId = productPromoCond.getString("inputParamEnumId");
        String operatorEnumId = productPromoCond.getString("operatorEnumId");
        if (otherValue != null && otherValue.contains("@")) {
            otherValue = "";
        }
        GenericValue userLogin = cart.getUserLogin();
        if (userLogin == null) {
            userLogin = cart.getAutoUserLogin();
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Checking promotion condition: " + productPromoCond, module);
        }
        Integer compareBase = null;

        //resolve the service name to use
        String serviceName = null;
        GenericValue customMethod = productPromoCond.getRelatedOne("CustomMethod", true);
        if (customMethod != null) {
            serviceName = customMethod.getString("customMethodName");
        } else {
            if ("PPIP_SERVICE".equals(inputParamEnumId)) {
                serviceName = condValue;
            } else {
                //for backward compatibility resolve customMethodId from enumCode
                GenericValue condEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", inputParamEnumId).cache().queryOne();
                if (condEnum != null) {
                    customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", condEnum.get("enumCode")).cache().queryOne();
                    if (customMethod == null) {
                        Debug.logWarning("The oldest enumeration " + inputParamEnumId + " for promo " + productPromoCond.getPkShortValueString()
                                + " haven't the new customMethod to use, please check your data or load seed data", module);
                        return false;
                    }
                    serviceName = customMethod.getString("customMethodName");
                }
            }
        }
        
        if (serviceName != null) {
            Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("productPromoCond", productPromoCond, "shoppingCart", cart, "nowTimestamp", nowTimestamp);
            Map<String, Object> condResult = null;
            try {
                condResult = dispatcher.runSync(serviceName, serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logWarning("Failed to execute productPromoCond service " + serviceName + " for promo " + productPromoCond.getPkShortValueString() + " throw " + e.toString(), module);
                return false;
            }
            compareBase = (Integer) condResult.get("compareBase");
            if (condResult.containsKey("operatorEnumId")) {
                operatorEnumId = (String) condResult.get("operatorEnumId");
            }
            
            if (Debug.verboseOn()) Debug.logVerbose("Condition compare done, compareBase=" + compareBase, module);
            if (compareBase != null) {
                int compare = compareBase;
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
        }
        // default to not meeting the condition
        return false;
    }

    public static boolean checkConditionsForItem(GenericValue productPromoActionOrCond, ShoppingCart cart, ShoppingCartItem cartItem, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        GenericValue productPromoRule = productPromoActionOrCond.getRelatedOne("ProductPromoRule", true);

        List<GenericValue> productPromoConds = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromoRule.get("productPromoId")).orderBy("productPromoCondSeqId").cache(true).queryList();
        productPromoConds = EntityUtil.filterByAnd(productPromoConds, UtilMisc.toMap("productPromoRuleId", productPromoRule.get("productPromoRuleId")));
        for (GenericValue productPromoCond: productPromoConds) {
            boolean passed = checkConditionForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp);
            if (!passed) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkConditionForItem(GenericValue productPromoCond, ShoppingCart cart, ShoppingCartItem cartItem, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException {
        String condValue = productPromoCond.getString("condValue");
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
        BigDecimal percentOff = amountOff.divide(listPrice, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100L));

        if (!"PPIP_LPMUP_AMT".equals(inputParamEnumId) && !"PPIP_LPMUP_PER".equals(inputParamEnumId)) {
            // condition doesn't apply to individual item, always passes
            return true;
        }

        // NOTE: only check this after we know it's this type of cond, otherwise condValue may not be a number
        int compare = percentOff.compareTo(new BigDecimal(condValue));

        Debug.logInfo("Checking condition for item productId=" + cartItem.getProductId() + ","
                + " listPrice=" + listPrice + ", basePrice=" + basePrice + ", amountOff=" + amountOff + ","
                + " percentOff=" + percentOff + ", condValue=" + condValue + ", compareBase=" + compare + ", "
                + "productPromoCond=" + productPromoCond, module);

        boolean res = ("PPC_EQ".equals(operatorEnumId) && compare == 0)
                || ("PPC_NEQ".equals(operatorEnumId) && compare != 0)
                || ("PPC_LT".equals(operatorEnumId) && compare < 0)
                || ("PPC_LTE".equals(operatorEnumId) && compare <= 0)
                || ("PPC_GT".equals(operatorEnumId) && compare > 0)
                || ("PPC_GTE".equals(operatorEnumId) && compare >= 0);
        if (!res) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderAnUnSupportedProductPromoCondCondition",
                    UtilMisc.toMap("operatorEnumId", operatorEnumId) , cart.getLocale()), module);
        }
        return res;
    }

    public static int checkConditionPartyHierarchy(Delegator delegator, Timestamp nowTimestamp, String groupPartyId, String partyId) throws GenericEntityException{
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
    private static ActionResultInfo performAction(GenericValue productPromoAction, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException, CartItemModifyException {
        ActionResultInfo actionResultInfo = new ActionResultInfo();
        performAction(actionResultInfo, productPromoAction, cart, delegator, dispatcher, nowTimestamp);
        return actionResultInfo;
    }

    public static void performAction(ActionResultInfo actionResultInfo, GenericValue productPromoAction, ShoppingCart cart, Delegator delegator, LocalDispatcher dispatcher, Timestamp nowTimestamp) throws GenericEntityException, CartItemModifyException {
        String productPromoActionEnumId = productPromoAction.getString("productPromoActionEnumId");
        String serviceName = null;
        GenericValue customMethod = productPromoAction.getRelatedOne("CustomMethod", true);
        
        if (customMethod != null) {
            serviceName = customMethod.getString("customMethodName");
        } else {
            if ("PROMO_SERVICE".equals(productPromoActionEnumId)) {
                serviceName = productPromoAction.getString("serviceName");
            } else {
                //for backware compatibility resolve customMethodId from enumCode
                GenericValue condEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", productPromoActionEnumId).cache().queryOne();
                if (condEnum != null) {
                    customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", condEnum.get("enumCode")).cache().queryOne();
                    if (customMethod != null) {
                        serviceName = customMethod.getString("customMethodName");
                    }
                }
            }
        } 

        if (serviceName != null) {
            Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("productPromoAction", productPromoAction, "shoppingCart", cart, "nowTimestamp", nowTimestamp, "actionResultInfo", actionResultInfo);
            Map<String, Object> actionResult;
            try {
                actionResult = dispatcher.runSync(serviceName, serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logError("Error calling promo action service [" + serviceName + "]", module);
                throw new CartItemModifyException("Error calling promo action service [" + serviceName + "]", e);
            }
            if (ServiceUtil.isError(actionResult)) {
                Debug.logError("Error calling promo action service [" + serviceName + "], result is: " + actionResult, module);
                throw new CartItemModifyException(ServiceUtil.getErrorMessage(actionResult));
            }
            actionResultInfo = (ActionResultInfo) actionResult.get("actionResultInfo");
            CartItemModifyException cartItemModifyException = (CartItemModifyException) actionResult.get("cartItemModifyException");
            if (cartItemModifyException != null) {
                throw cartItemModifyException;
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

    public static List<ShoppingCartItem> getCartItemsUsed(ShoppingCart cart, GenericValue productPromoAction) {
        List<ShoppingCartItem> cartItemsUsed = new LinkedList<>();
        for (ShoppingCartItem cartItem : cart) {
            BigDecimal quantityUsed = cartItem.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction);
            if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                cartItemsUsed.add(cartItem);
            }
        }
        return cartItemsUsed;
    }

    public static BigDecimal getCartItemsUsedTotalAmount(ShoppingCart cart, GenericValue productPromoAction) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : cart) {
            BigDecimal quantityUsed = cartItem.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction);
            if (quantityUsed.compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = totalAmount.add(quantityUsed.multiply(cartItem.getBasePrice()));
            }
        }
        return totalAmount;
    }

    public static void distributeDiscountAmount(BigDecimal discountAmountTotal, BigDecimal totalAmount, List<ShoppingCartItem> cartItemsUsed, GenericValue productPromoAction, Delegator delegator) {
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
                weightedAmount = weightedAmount.setScale(3, RoundingMode.HALF_UP);
                discountAmount = discountAmount.subtract(weightedAmount);
                doOrderItemPromoAction(productPromoAction, cartItem, weightedAmount, "amount", delegator);
            } else {
                // last one, just use discountAmount
                doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator);
            }
        }
        // this is the old way that causes problems: doOrderPromoAction(productPromoAction, cart, discountAmount, "amount", delegator);
    }

    public static Integer findPromoItem(GenericValue productPromoAction, ShoppingCart cart) {
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
                        return i;
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
        boolean addNewAdjustment = true;
        List<GenericValue> adjustments = cartItem.getAdjustments();
        if (UtilValidate.isNotEmpty(adjustments)) {
            for(GenericValue adjustment : adjustments) {
                if("PROMOTION_ADJUSTMENT".equals(adjustment.getString("orderAdjustmentTypeId")) &&
                        productPromoAction.get("productPromoId").equals(adjustment.getString("productPromoId")) &&
                        productPromoAction.get("productPromoRuleId").equals(adjustment.getString("productPromoRuleId")) &&
                        productPromoAction.get("productPromoActionSeqId").equals(adjustment.getString("productPromoActionSeqId"))) {
                    BigDecimal newAmount = amount.add(adjustment.getBigDecimal(amountField));
                    adjustment.set(amountField, newAmount);
                    addNewAdjustment = false;
                }
            }
        }
        if (addNewAdjustment) {
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
            if (UtilValidate.isNotEmpty(prodPromo.get("promoName"))) {
                return (String) prodPromo.get("promoName");
            }
            if (UtilValidate.isNotEmpty(prodPromo.get("promoText"))) {
                return (String) prodPromo.get("promoText");
            }
            return "No promotion name nor text";

        } catch (GenericEntityException e) {
            Debug.logWarning("Error getting ProductPromo for Id " + prodPromoId, module);
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

        Set<String> productIds = new HashSet<>();
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

        Set<String> productIds = new HashSet<>();
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

        List<GenericValue> productPromoProductsCond = new LinkedList<>();
        List<GenericValue> productPromoCategoriesCond = new LinkedList<>();
        List<GenericValue> productPromoProductsAction = new LinkedList<>();
        List<GenericValue> productPromoCategoriesAction = new LinkedList<>();

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

    private static boolean isProductOld(String productId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        if (product != null) {
            Timestamp salesDiscontinuationDate = product.getTimestamp("salesDiscontinuationDate");
            if (salesDiscontinuationDate != null && salesDiscontinuationDate.before(nowTimestamp)) {
                return true;
            }
        }
        return false;
    }

    private static void handleProductPromoCategories(Set<String> productIds, List<GenericValue> productPromoCategories, String productPromoApplEnumId, Delegator delegator, Timestamp nowTimestamp) throws GenericEntityException {
        boolean include = !"PPPA_EXCLUDE".equals(productPromoApplEnumId);
        Set<String> productCategoryIds = new HashSet<>();
        Map<String, List<Set<String>>> productCategoryGroupSetListMap = new HashMap<>();

        for (GenericValue productPromoCategory : productPromoCategories) {
            if (productPromoApplEnumId.equals(productPromoCategory.getString("productPromoApplEnumId"))) {
                Set<String> tempCatIdSet = new HashSet<>();
                if ("Y".equals(productPromoCategory.getString("includeSubCategories"))) {
                    ProductSearch.getAllSubCategoryIds(productPromoCategory.getString("productCategoryId"), tempCatIdSet, delegator, nowTimestamp);
                } else {
                    tempCatIdSet.add(productPromoCategory.getString("productCategoryId"));
                }

                String andGroupId = productPromoCategory.getString("andGroupId");
                if ("_NA_".equals(andGroupId)) {
                    productCategoryIds.addAll(tempCatIdSet);
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
            List<Set<String>> productIdSetList = new LinkedList<>();

            for (Set<String> catIdSet : catIdSetList) {
                // make a Set of productIds including all ids from all categories
                Set<String> groupProductIdSet = new HashSet<>();
                getAllProductIds(catIdSet, groupProductIdSet, delegator, nowTimestamp, true);
                productIdSetList.add(groupProductIdSet);
            }

            // now go through all productId sets and only include IDs that are in all sets
            // by definition if each id must be in all categories, then it must be in the first, so go through the first and drop each one that is not in all others
            Set<String> firstProductIdSet = productIdSetList.remove(0);
            for (Set<String> productIdSet : productIdSetList) {
                firstProductIdSet.retainAll(productIdSet);
            }

            if (!firstProductIdSet.isEmpty()) {
                if (include) {
                    productIds.addAll(firstProductIdSet);
                } else {
                    productIds.removeAll(firstProductIdSet);
                }
            }
        }
    }

    private static void getAllProductIds(Set<String> productCategoryIdSet, Set<String> productIdSet, Delegator delegator, Timestamp nowTimestamp, boolean include) throws GenericEntityException {
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

    private static void handleProductPromoProducts(Set<String> productIds, List<GenericValue> productPromoProducts, String productPromoApplEnumId) throws GenericEntityException {
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
    private static class UseLimitException extends Exception {
        public UseLimitException(String str) {
            super(str);
        }
    }
}
