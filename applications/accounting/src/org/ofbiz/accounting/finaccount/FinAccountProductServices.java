/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.finaccount.FinAccountHelper;

/**
 * FinAccountProductServices - Financial Accounts created from product purchases (i.e. gift certificates)
 */
public class FinAccountProductServices {

    public static final String module = FinAccountProductServices.class.getName();
    public static final String resourceOrderError = "OrderErrorUiLabels";
    public static final String resourceError = "AccountingErrorUiLabels";
    
    public static Map<String, Object> createPartyFinAccountFromPurchase(DispatchContext dctx, Map<String, Object> context) {
        // this service should always be called via FULFILLMENT_EXTASYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // order ID for tracking
        String orderId = orderItem.getString("orderId");
        String orderItemSeqId = orderItem.getString("orderItemSeqId");

        // the order header for store info
        GenericValue orderHeader;
        try {
            orderHeader = orderItem.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderHeader from OrderItem", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "OrderCannotGetOrderHeader", UtilMisc.toMap("orderId", orderId), locale));
        }

        String productId = orderItem.getString("productId");
        GenericValue featureAndAppl;
        try {
            List<GenericValue> featureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                    .where("productId", productId, "productFeatureTypeId", "TYPE", "productFeatureApplTypeId", "STANDARD_FEATURE")
                    .queryList();
            featureAndAppls = EntityUtil.filterByDate(featureAndAppls);
            featureAndAppl = EntityUtil.getFirst(featureAndAppls);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // financial account data; pulled from the TYPE feature of the product
        String finAccountTypeId = "BALANCE_ACCOUNT"; // default
        String finAccountName = "Customer Financial Account";
        if (featureAndAppl != null) {
            if (UtilValidate.isNotEmpty(featureAndAppl.getString("idCode"))) {
                finAccountTypeId = featureAndAppl.getString("idCode");
            }
            if (UtilValidate.isNotEmpty(featureAndAppl.getString("description"))) {
                finAccountName = featureAndAppl.getString("description");
            }
        }

        // locate the financial account type
        GenericValue finAccountType;
        try {
            finAccountType = EntityQuery.use(delegator).from("FinAccountType").where("finAccountTypeId", finAccountTypeId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        String replenishEnumId = finAccountType.getString("replenishEnumId");

        // get the order read helper
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the currency
        String currency = orh.getCurrency();

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // get the product store
        String productStoreId = null;
        if (orderHeader != null) {
            productStoreId = orh.getProductStoreId();
        }
        if (productStoreId == null) {
            Debug.logFatal("Unable to create financial accout; no productStoreId on OrderHeader : " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountCannotCreate", 
                    UtilMisc.toMap("orderId", orderId), locale));
        }

        // party ID (owner)
        GenericValue billToParty = orh.getBillToParty();
        String partyId = null;
        if (billToParty != null) {
            partyId = billToParty.getString("partyId");
        }

        // payment method info
        List<GenericValue> payPrefs = orh.getPaymentPreferences();
        String paymentMethodId = null;
        if (payPrefs != null) {
            for (GenericValue pref : payPrefs) {
                // needs to be a CC or EFT account
                String type = pref.getString("paymentMethodTypeId");
                if ("CREDIT_CARD".equals(type) || "EFT_ACCOUNT".equals(type)) {
                    paymentMethodId = pref.getString("paymentMethodId");
                }
            }
        }
        // some person data for expanding
        GenericValue partyGroup = null;
        GenericValue person = null;
        GenericValue party = null;

        if (billToParty != null) {
            try {
                party = billToParty.getRelatedOne("Party", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (party != null) {
                String partyTypeId = party.getString("partyTypeId");
                if ("PARTY_GROUP".equals(partyTypeId)) {
                    partyGroup = billToParty;
                } else if ("PERSON".equals(partyTypeId)) {
                    person = billToParty;
                }
            }
        }

        // create the context for FSE
        Map<String, Object> expContext = new HashMap<String, Object>();
        expContext.put("orderHeader", orderHeader);
        expContext.put("orderItem", orderItem);
        expContext.put("party", party);
        expContext.put("person", person);
        expContext.put("partyGroup", partyGroup);

        // expand the name field to dynamically add information
        FlexibleStringExpander exp = FlexibleStringExpander.getInstance(finAccountName);
        finAccountName = exp.expandString(expContext);

        // price/amount/quantity to create initial deposit amount
        BigDecimal quantity = orderItem.getBigDecimal("quantity");
        BigDecimal price = orderItem.getBigDecimal("unitPrice");
        BigDecimal deposit = price.multiply(quantity).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding);

        // create the financial account
        Map<String, Object> createCtx = new HashMap<String, Object>();
        String finAccountId;

        createCtx.put("finAccountTypeId", finAccountTypeId);
        createCtx.put("finAccountName", finAccountName);
        createCtx.put("productStoreId", productStoreId);
        createCtx.put("ownerPartyId", partyId);
        createCtx.put("currencyUomId", currency);
        createCtx.put("statusId", "FNACT_ACTIVE");
        createCtx.put("userLogin", userLogin);

        // if we auto-replenish this type; set the level to the initial deposit
        if (replenishEnumId != null && "FARP_AUTOMATIC".equals(replenishEnumId)) {
            createCtx.put("replenishLevel", deposit);
            createCtx.put("replenishPaymentId", paymentMethodId);
        }

        Map<String, Object> createResp;
        try {
            createResp = dispatcher.runSync("createFinAccountForStore", createCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(createResp), module);
            return createResp;
        }

        finAccountId = (String) createResp.get("finAccountId");

        // create the owner role
        Map<String, Object> roleCtx = new HashMap<String, Object>();
        roleCtx.put("partyId", partyId);
        roleCtx.put("roleTypeId", "OWNER");
        roleCtx.put("finAccountId", finAccountId);
        roleCtx.put("userLogin", userLogin);
        roleCtx.put("fromDate", UtilDateTime.nowTimestamp());
        Map<String, Object> roleResp;
        try {
            roleResp = dispatcher.runSync("createFinAccountRole", roleCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(roleResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(roleResp), module);
            return roleResp;
        }

        // create the initial deposit
        Map<String, Object> depositCtx = new HashMap<String, Object>();
        depositCtx.put("finAccountId", finAccountId);
        depositCtx.put("productStoreId", productStoreId);
        depositCtx.put("currency", currency);
        depositCtx.put("partyId", partyId);
        depositCtx.put("orderId", orderId);
        depositCtx.put("orderItemSeqId", orderItemSeqId);
        depositCtx.put("amount", deposit);
        depositCtx.put("reasonEnumId", "FATR_IDEPOSIT");
        depositCtx.put("userLogin", userLogin);

        Map<String, Object> depositResp;
        try {
            depositResp = dispatcher.runSync("finAccountDeposit", depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            Debug.logFatal(ServiceUtil.getErrorMessage(depositResp), module);
            return depositResp;
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("finAccountId", finAccountId);
        return result;
    }
}
