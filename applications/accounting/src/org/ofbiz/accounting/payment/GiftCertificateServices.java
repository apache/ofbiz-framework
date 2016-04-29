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
package org.ofbiz.accounting.payment;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.finaccount.FinAccountHelper;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;


public class GiftCertificateServices {

    public static final String module = GiftCertificateServices.class.getName();
    public static final String resourceError = "AccountingErrorUiLabels";
    public static final String resourceOrderError = "OrderErrorUiLabels";
    // These are default settings, in case ProductStoreFinActSetting does not have them
    public static final int CARD_NUMBER_LENGTH = 14;
    public static final int PIN_NUMBER_LENGTH = 6;

    public static BigDecimal ZERO = BigDecimal.ZERO;

    // Base Gift Certificate Services
    public static Map<String, Object> createGiftCertificate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        BigDecimal initialAmount = (BigDecimal) context.get("initialAmount");

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        String cardNumber = null;
        String pinNumber = null;
        String refNum = null;
        String finAccountId = null;
        try {
            final String accountName = "Gift Certificate Account";
            final String deposit = "DEPOSIT";

            GenericValue giftCertSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId)
                    .cache().queryOne();
            Map<String, Object> acctResult = null;

            if ("Y".equals(giftCertSettings.getString("requirePinCode"))) {
                // TODO: move this code to createFinAccountForStore as well
                int cardNumberLength = CARD_NUMBER_LENGTH;
                int pinNumberLength = PIN_NUMBER_LENGTH;
                if (giftCertSettings.getLong("accountCodeLength") != null) {
                    cardNumberLength = giftCertSettings.getLong("accountCodeLength").intValue();
                }
                if (giftCertSettings.getLong("pinCodeLength") != null) {
                    pinNumberLength = giftCertSettings.getLong("pinCodeLength").intValue();
                }
                cardNumber = generateNumber(delegator, cardNumberLength, true);
                pinNumber = generateNumber(delegator, pinNumberLength, false);

                // in this case, the card number is the finAccountId
                finAccountId = cardNumber;

                // create the FinAccount
                Map<String, Object> acctCtx = UtilMisc.<String, Object>toMap("finAccountId", finAccountId);
                acctCtx.put("finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId);
                acctCtx.put("finAccountName", accountName);
                acctCtx.put("finAccountCode", pinNumber);
                acctCtx.put("userLogin", userLogin);
                acctResult = dispatcher.runSync("createFinAccount", acctCtx);
            } else {
                acctResult = dispatcher.runSync("createFinAccountForStore", UtilMisc.<String, Object>toMap("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId, "userLogin", userLogin));
                if (acctResult.get("finAccountId") != null) {
                    finAccountId = cardNumber = (String) acctResult.get("finAccountId");
                }
                if (acctResult.get("finAccountCode") != null) {
                    cardNumber = (String) acctResult.get("finAccountCode");
                }
            }

            if (ServiceUtil.isError(acctResult)) {
                String error = ServiceUtil.getErrorMessage(acctResult);
                return ServiceUtil.returnError(error);
            }

            // create the initial (deposit) transaction
            // do something tricky here: run as the "system" user
            // that can actually create a financial account transaction
            GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
            refNum = createTransaction(delegator, dispatcher, permUserLogin, initialAmount, productStoreId, 
                    partyId, currencyUom, deposit, finAccountId, locale);

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCreationError", locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCreationError", locale));
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("cardNumber", cardNumber);
        result.put("pinNumber", pinNumber);
        result.put("initialAmount", initialAmount);
        result.put("processResult", Boolean.TRUE);
        result.put("responseCode", "1");
        result.put("referenceNum", refNum);
        Debug.logInfo("Create GC Result - " + result, module);
        return result;
    }

    public static Map<String, Object> addFundsToGiftCertificate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        final String deposit = "DEPOSIT";

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String cardNumber = (String) context.get("cardNumber");
        String pinNumber = (String) context.get("pinNumber");
        BigDecimal amount = (BigDecimal) context.get("amount");

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        String finAccountId = null;
        GenericValue finAccount = null;
         // validate the pin if the store requires it and figure out the finAccountId from card number
        try {
            GenericValue giftCertSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId)
                    .cache().queryOne();
            if ("Y".equals(giftCertSettings.getString("requirePinCode"))) {
                if (!validatePin(delegator, cardNumber, pinNumber)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "AccountingGiftCerticateNumberPinNotValid", locale));
                }
                finAccountId = cardNumber;
            } else {
                finAccount = FinAccountHelper.getFinAccountFromCode(cardNumber, delegator);
                if (finAccount != null) {
                    finAccountId = finAccount.getString("finAccountId");
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountSetting", 
                    UtilMisc.toMap("productStoreId", productStoreId, 
                            "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId), locale));
        }

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", ""), locale));
        }

        if (finAccount == null) {
            try {
                finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", finAccountId), locale));
            }
        }

        // get the previous balance
        BigDecimal previousBalance = ZERO;
        if (finAccount.get("availableBalance") != null) {
            previousBalance = finAccount.getBigDecimal("availableBalance");
        }

        // create the transaction
        BigDecimal balance = ZERO;
        String refNum = null;
        try {
            refNum = GiftCertificateServices.createTransaction(delegator, dispatcher, userLogin, amount, productStoreId, partyId,
                    currencyUom, deposit, finAccountId, locale);
            finAccount.refresh();
            balance = finAccount.getBigDecimal("availableBalance");
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("previousBalance", previousBalance);
        result.put("balance", balance);
        result.put("amount", amount);
        result.put("processResult", Boolean.TRUE);
        result.put("responseCode", "1");
        result.put("referenceNum", refNum);
        Debug.logInfo("Add Funds GC Result - " + result, module);
        return result;
    }

    public static Map<String, Object> redeemGiftCertificate(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        final String withdrawl = "WITHDRAWAL";
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String cardNumber = (String) context.get("cardNumber");
        String pinNumber = (String) context.get("pinNumber");
        BigDecimal amount = (BigDecimal) context.get("amount");

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // validate the amount
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountMustBePositive", locale));
        }

        // validate the pin if the store requires it
        try {
            GenericValue giftCertSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId)
                    .cache().queryOne();
            if ("Y".equals(giftCertSettings.getString("requirePinCode")) && !validatePin(delegator, cardNumber, pinNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberPinNotValid", locale));
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountSetting", 
                    UtilMisc.toMap("productStoreId", productStoreId, 
                            "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId), locale));
        }
        Debug.logInfo("Attempting to redeem GC for " + amount, module);

        GenericValue finAccount = null;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", cardNumber).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", cardNumber), locale));
        }

        // check the actual balance (excluding authorized amounts) and create the transaction if it is sufficient
        BigDecimal previousBalance = finAccount.get("actualBalance") == null ? BigDecimal.ZERO : finAccount.getBigDecimal("actualBalance");

        BigDecimal balance = BigDecimal.ZERO;
        String refNum = null;
        Boolean procResult;
        if (previousBalance.compareTo(amount) >= 0) {
            try {
                refNum = GiftCertificateServices.createTransaction(delegator, dispatcher, userLogin, amount, productStoreId,
                        partyId, currencyUom, withdrawl, cardNumber, locale);
                finAccount.refresh();
                balance = finAccount.get("availableBalance") == null ? BigDecimal.ZERO : finAccount.getBigDecimal("availableBalance");
                procResult = Boolean.TRUE;
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            procResult = Boolean.FALSE;
            balance = previousBalance;
            refNum = "N/A";
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("previousBalance", previousBalance);
        result.put("balance", balance);
        result.put("amount", amount);
        result.put("processResult", procResult);
        result.put("responseCode", "2");
        result.put("referenceNum", refNum);
        Debug.logInfo("Redeem GC Result - " + result, module);
        return result;
    }

    public static Map<String, Object> checkGiftCertificateBalance(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String cardNumber = (String) context.get("cardNumber");
        String pinNumber = (String) context.get("pinNumber");
        Locale locale = (Locale) context.get("locale");

        // validate the pin
        if (!validatePin(delegator, cardNumber, pinNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberPinNotValid", locale));
        }

        GenericValue finAccount = null;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", cardNumber).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", cardNumber), locale));
        }

        // TODO: get the real currency from context
        //String currencyUom = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        // get the balance
        BigDecimal balance = finAccount.get("availableBalance") == null ? BigDecimal.ZERO : finAccount.getBigDecimal("availableBalance");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("balance", balance);
        Debug.logInfo("GC Balance Result - " + result, module);
        return result;
    }

    // Fullfilment Services
    public static Map<String, Object> giftCertificateProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        BigDecimal amount = (BigDecimal) context.get("processAmount");
        String currency = (String) context.get("currency");
        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // get the authorizations
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountCannotCapture", locale));
        }

        // get the gift certificate and its authorization from the authorization
        String finAccountAuthId = authTransaction.getString("referenceNum");
        try {
            GenericValue finAccountAuth = EntityQuery.use(delegator).from("FinAccountAuth").where("finAccountAuthId", finAccountAuthId).queryOne();
            GenericValue giftCard = finAccountAuth.getRelatedOne("FinAccount", false);
            // make sure authorization has not expired
            Timestamp authExpiration = finAccountAuth.getTimestamp("thruDate");
            if ((authExpiration != null) && (authExpiration.before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingFinAccountAuthorizationExpired", 
                        UtilMisc.toMap("paymentGatewayResponseId", authTransaction.getString("paymentGatewayResponseId"),
                                "authExpiration", authExpiration), locale));
            }
            // make sure the fin account itself has not expired
            if ((giftCard.getTimestamp("thruDate") != null) && (giftCard.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberExpired", 
                        UtilMisc.toMap("thruDate", giftCard.getTimestamp("thruDate")), locale));
            }

            // obtain the order information
            OrderReadHelper orh = new OrderReadHelper(delegator, orderPaymentPreference.getString("orderId"));

            Map<String, Object> redeemCtx = new HashMap<String, Object>();
            redeemCtx.put("userLogin", userLogin);
            redeemCtx.put("productStoreId", orh.getProductStoreId());
            redeemCtx.put("cardNumber", giftCard.get("finAccountId"));
            redeemCtx.put("pinNumber", giftCard.get("finAccountCode"));
            redeemCtx.put("currency", currency);
            if (orh.getBillToParty() != null) {
                redeemCtx.put("partyId", orh.getBillToParty().get("partyId"));
            }
            redeemCtx.put("amount", amount);

            // invoke the redeem service
            Map<String, Object> redeemResult = null;
            redeemResult = dispatcher.runSync("redeemGiftCertificate", redeemCtx);
            if (ServiceUtil.isError(redeemResult)) {
                return redeemResult;
            }

            // now release the authorization should this use the gift card release service?
            Map<String, Object> releaseResult = dispatcher.runSync("expireFinAccountAuth", 
                    UtilMisc.<String, Object>toMap("userLogin", userLogin, "finAccountAuthId", finAccountAuthId));
            if (ServiceUtil.isError(releaseResult)) {
                return releaseResult;
            }

            String authRefNum = authTransaction.getString("referenceNum");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (redeemResult != null) {
                Boolean processResult = (Boolean) redeemResult.get("processResult");
                result.put("processAmount", amount);
                result.put("captureResult", processResult);
                result.put("captureCode", "C");
                result.put("captureRefNum", redeemResult.get("referenceNum"));
                result.put("authRefNum", authRefNum);
            }

            return result;

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotProcess", 
                    UtilMisc.toMap("errorString", ex.getMessage()), locale));
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotProcess", 
                    UtilMisc.toMap("errorString", ex.getMessage()), locale));
        }
}


    public static Map<String, Object> giftCertificateAuthorize(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        GenericValue giftCard = (GenericValue) context.get("giftCard");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        BigDecimal amount = (BigDecimal) context.get("processAmount");

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        // obtain the order information
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();
        try {
            // if the store requires pin codes, then validate pin code against card number, and the gift certificate's finAccountId is the gift card's card number
            // otherwise, the gift card's card number is an ecrypted string, which must be decoded to find the FinAccount
            GenericValue giftCertSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId)
                    .cache().queryOne();
            GenericValue finAccount = null;
            String finAccountId = null;
            if (UtilValidate.isNotEmpty(giftCertSettings)) {
                if ("Y".equals(giftCertSettings.getString("requirePinCode"))) {
                    if (validatePin(delegator, giftCard.getString("cardNumber"), giftCard.getString("pinNumber"))) {
                        finAccountId = giftCard.getString("cardNumber");
                        finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
                    }
                } else {
                        finAccount = FinAccountHelper.getFinAccountFromCode(giftCard.getString("cardNumber"), delegator);
                        if (finAccount == null) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                                    "AccountingGiftCerticateNumberNotFound", 
                                    UtilMisc.toMap("finAccountId", ""), locale));
                        }
                        finAccountId = finAccount.getString("finAccountId");
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingFinAccountSetting", 
                        UtilMisc.toMap("productStoreId", productStoreId, 
                                "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId), locale));
            }

            if (finAccountId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberPinNotValid", locale));
            }

            // check for expiration date
            if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberExpired", 
                        UtilMisc.toMap("thruDate", finAccount.getTimestamp("thruDate")), locale));
            }

            // check the amount to authorize against the available balance of fin account, which includes active authorizations as well as transactions
            BigDecimal availableBalance = finAccount.getBigDecimal("availableBalance");
            Boolean processResult = null;
            String refNum = null;
            Map<String, Object> result = ServiceUtil.returnSuccess();

            // make sure to round and scale it to the same as availableBalance
            amount = amount.setScale(FinAccountHelper.decimals, FinAccountHelper.rounding);

            // if availableBalance equal to or greater than amount, then auth
            if (UtilValidate.isNotEmpty(availableBalance) && availableBalance.compareTo(amount) >= 0) {
                Timestamp thruDate = null;
                if (giftCertSettings.getLong("authValidDays") != null) {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), giftCertSettings.getLong("authValidDays"));
                }
                Map<String, Object> tmpResult = dispatcher.runSync("createFinAccountAuth", 
                        UtilMisc.<String, Object>toMap("finAccountId", finAccountId, 
                                "amount", amount, "currencyUomId", currency,
                                "thruDate", thruDate, "userLogin", userLogin));
                if (ServiceUtil.isError(tmpResult)) {
                    return tmpResult;
                } else {
                    refNum = (String) tmpResult.get("finAccountAuthId");
                    processResult = Boolean.TRUE;
                }
            } else {
                Debug.logError("Attempted to authorize [" + amount + "] against a balance of only [" + availableBalance + "]", module);
                refNum = "N/A";      // a refNum is always required from authorization
                processResult = Boolean.FALSE;
            }

            result.put("processAmount", amount);
            result.put("authResult", processResult);
            result.put("processAmount", amount);
            result.put("authFlag", "2");
            result.put("authCode", "A");
            result.put("captureCode", "C");
            result.put("authRefNum", refNum);

            return result;
        } catch (GenericEntityException ex) {
            Debug.logError(ex, "Cannot authorize gift certificate", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotAuthorize", 
                    UtilMisc.toMap("errorString", ex.getMessage()), locale));
        } catch (GenericServiceException ex) {
            Debug.logError(ex, "Cannot authorize gift certificate", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotAuthorize", 
                    UtilMisc.toMap("errorString", ex.getMessage()), locale));
        }
    }

    public static Map<String, Object> giftCertificateRefund(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String currency = (String) context.get("currency");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");
        Locale locale = (Locale) context.get("locale");
        return giftCertificateRestore(dctx, userLogin, paymentPref, amount, currency, "refund", locale);
    }

    public static Map<String, Object> giftCertificateRelease(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Locale locale = (Locale) context.get("locale");

        String err = UtilProperties.getMessage(resourceError, 
                "AccountingGiftCerticateNumberCannotBeExpired", locale);
        try {
            // expire the related financial authorization transaction
            GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTransaction == null) {
                return ServiceUtil.returnError(err + UtilProperties.getMessage(resourceError, 
                        "AccountingFinAccountCannotFindAuthorization", locale));
            }
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin, 
                    "finAccountAuthId", authTransaction.get("referenceNum"));
            Map<String, Object> serviceResults = dispatcher.runSync("expireFinAccountAuth", input);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("releaseRefNum", authTransaction.getString("referenceNum"));
            result.put("releaseAmount", authTransaction.getBigDecimal("amount"));
            result.put("releaseResult", Boolean.TRUE);

            // if there's an error, don't release
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(err + ServiceUtil.getErrorMessage(serviceResults));
            }

            return result;
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(err + e.getMessage());
        }
    }

    private static Map<String, Object> giftCertificateRestore(DispatchContext dctx, GenericValue userLogin, GenericValue paymentPref, 
            BigDecimal amount, String currency, String resultPrefix, Locale locale) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        // get the orderId for tracking
        String orderId = paymentPref.getString("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString("partyId");
        }

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne("GiftCard", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get GiftCard from OrderPaymentPreference", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotLocateItFromOrderPaymentPreference", locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotRelease", locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> refundCtx = new HashMap<String, Object>();
        refundCtx.put("productStoreId", productStoreId);
        refundCtx.put("currency", currency);
        refundCtx.put("partyId", partyId);
        //reloadCtx.put("orderId", orderId);
        refundCtx.put("cardNumber", giftCard.get("cardNumber"));
        refundCtx.put("pinNumber", giftCard.get("pinNumber"));
        refundCtx.put("amount", amount);
        refundCtx.put("userLogin", userLogin);

        Map<String, Object> restoreGcResult = null;
        try {
            restoreGcResult = dispatcher.runSync("addFundsToGiftCertificate", refundCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberRefundCallError", locale));
        }
        if (ServiceUtil.isError(restoreGcResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(restoreGcResult));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (restoreGcResult != null) {
            Boolean processResult = (Boolean) restoreGcResult.get("processResult");
            result.put(resultPrefix + "Amount", amount);
            result.put(resultPrefix + "Result", processResult);
            result.put(resultPrefix + "Code", "R");
            result.put(resultPrefix + "Flag", restoreGcResult.get("responseCode"));
            result.put(resultPrefix + "RefNum", restoreGcResult.get("referenceNum"));
        }

        return result;
    }

    public static Map<String, Object> giftCertificatePurchase(DispatchContext dctx, Map<String, ? extends Object> context) {
        // this service should always be called via FULFILLMENT_EXTASYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        Locale locale = (Locale) context.get("locale");

        // order ID for tracking
        String orderId = orderItem.getString("orderId");

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderHeader from OrderItem",module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "OrderCannotGetOrderHeader", UtilMisc.toMap("orderId", orderId), locale));
        }

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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotProcess",
                    UtilMisc.toMap("orderId", orderId), locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString("partyId");
        }

        // amount/quantity of the gift card(s)
        BigDecimal amount = orderItem.getBigDecimal("unitPrice");
        BigDecimal quantity = orderItem.getBigDecimal("quantity");

        // the product entity needed for information
        GenericValue product = null;
        try {
            product = orderItem.getRelatedOne("Product", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get Product from OrderItem", module);
        }
        if (product == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotFulfill", locale));
        }

        // Gift certificate settings are per store in this entity
        GenericValue giftCertSettings = null;
        try {
            giftCertSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId)
                    .cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get Product Store FinAccount settings for " + FinAccountHelper.giftCertFinAccountTypeId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountSetting", 
                    UtilMisc.toMap("productStoreId", productStoreId, 
                            "finAccountTypeId", FinAccountHelper.giftCertFinAccountTypeId), locale) + ": " + e.getMessage());
        }

        // survey information
        String surveyId = giftCertSettings.getString("purchaseSurveyId");

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            // there should be only one
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "surveyId", surveyId)
                    .orderBy("-responseDate").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotFulfill", locale));
        }
        if (surveyResponse == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotFulfill", locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated("SurveyResponseAnswer", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotFulfillFromSurveyAnswers", locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<String, Object>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne("SurveyQuestion", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "AccountingGiftCerticateNumberCannotFulfillFromSurveyAnswers", locale));
                }
                if (question != null) {
                    String desc = question.getString("description");
                    String ans = answer.getString("textResponse");  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        // get the send to email address - key defined in product store settings entity
        String sendToKey = giftCertSettings.getString("purchSurveySendTo");
        String sendToEmail = (String) answerMap.get(sendToKey);

        // get the copyMe flag and set the order email address
        String orderEmails = orh.getOrderEmailString();
        String copyMeField = giftCertSettings.getString("purchSurveyCopyMe");
        String copyMeResp = copyMeField != null ? (String) answerMap.get(copyMeField) : null;
        boolean copyMe = (UtilValidate.isNotEmpty(copyMeField)
                && UtilValidate.isNotEmpty(copyMeResp) && "true".equalsIgnoreCase(copyMeResp)) ? true : false;

        int qtyLoop = quantity.intValue();
        for (int i = 0; i < qtyLoop; i++) {
            // create a gift certificate
            Map<String, Object> createGcCtx = new HashMap<String, Object>();
            //createGcCtx.put("paymentConfig", paymentConfig);
            createGcCtx.put("productStoreId", productStoreId);
            createGcCtx.put("currency", currency);
            createGcCtx.put("partyId", partyId);
            //createGcCtx.put("orderId", orderId);
            createGcCtx.put("initialAmount", amount);
            createGcCtx.put("userLogin", userLogin);

            Map<String, Object> createGcResult = null;
            try {
                createGcResult = dispatcher.runSync("createGiftCertificate", createGcCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberCreationError", locale) + e.getMessage());
            }
            if (ServiceUtil.isError(createGcResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberCreationError", locale) 
                        + ServiceUtil.getErrorMessage(createGcResult));
            }

            // create the fulfillment record
            Map<String, Object> gcFulFill = new HashMap<String, Object>();
            gcFulFill.put("typeEnumId", "GC_ACTIVATE");
            gcFulFill.put("partyId", partyId);
            gcFulFill.put("orderId", orderId);
            gcFulFill.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
            gcFulFill.put("surveyResponseId", surveyResponse.get("surveyResponseId"));
            gcFulFill.put("cardNumber", createGcResult.get("cardNumber"));
            gcFulFill.put("pinNumber", createGcResult.get("pinNumber"));
            gcFulFill.put("amount", createGcResult.get("initialAmount"));
            gcFulFill.put("responseCode", createGcResult.get("responseCode"));
            gcFulFill.put("referenceNum", createGcResult.get("referenceNum"));
            gcFulFill.put("userLogin", userLogin);
            try {
                dispatcher.runAsync("createGcFulFillmentRecord", gcFulFill, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberCannotStoreFulfillmentInfo",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }

            // add some information to the answerMap for the email
            answerMap.put("cardNumber", createGcResult.get("cardNumber"));
            answerMap.put("pinNumber", createGcResult.get("pinNumber"));
            answerMap.put("amount", createGcResult.get("initialAmount"));

            // get the email setting for this email type
            GenericValue productStoreEmail = null;
            String emailType = "PRDS_GC_PURCHASE";
            try {
                productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", emailType).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get product store email setting for gift card purchase", module);
            }
            if (productStoreEmail == null) {
                Debug.logError("No gift card purchase email setting found for this store; cannot send gift card information", module);
            } else {
                answerMap.put("locale", locale);

                // set the bcc address(s)
                String bcc = productStoreEmail.getString("bccAddress");
                if (copyMe) {
                    if (UtilValidate.isNotEmpty(bcc)) {
                        bcc = bcc + "," + orderEmails;
                    } else {
                        bcc = orderEmails;
                    }
                }
                Map<String, Object> emailCtx = new HashMap<String, Object>();
                String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                if (UtilValidate.isEmpty(bodyScreenLocation)) {
                    bodyScreenLocation = ProductStoreWorker.getDefaultProductStoreEmailScreenLocation(emailType);
                }
                emailCtx.put("bodyScreenUri", bodyScreenLocation);
                emailCtx.put("bodyParameters", answerMap);
                emailCtx.put("sendTo", sendToEmail);
                emailCtx.put("contentType", productStoreEmail.get("contentType"));
                emailCtx.put("sendFrom", productStoreEmail.get("fromAddress"));
                emailCtx.put("sendCc", productStoreEmail.get("ccAddress"));
                emailCtx.put("sendBcc", bcc);
                emailCtx.put("subject", productStoreEmail.getString("subject"));
                emailCtx.put("userLogin", userLogin);
                
                // send off the email async so we will retry on failed attempts
                // SC 20060405: Changed to runSync because runAsync kept getting an error:
                // Problem serializing service attributes (Cannot serialize object of class java.util.PropertyResourceBundle)
                try {
                    dispatcher.runSync("sendMailFromScreen", emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem sending mail", module);
                    // this is fatal; we will rollback and try again later
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "AccountingGiftCerticateNumberCannotSendEmailNotice",
                            UtilMisc.toMap("errorString", e.toString()), locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> giftCertificateReload(DispatchContext dctx, Map<String, ? extends Object> context) {
        // this service should always be called via FULFILLMENT_EXTSYNC
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        Locale locale = (Locale) context.get("locale");

        // order ID for tracking
        String orderId = orderItem.getString("orderId");

        // the order header for store info
        GenericValue orderHeader = null;
        try {
            orderHeader = orderItem.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderHeader from OrderItem",module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "OrderCannotGetOrderHeader", UtilMisc.toMap("orderId", orderId), locale));
        }

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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "AccountingGiftCerticateNumberCannotReload", UtilMisc.toMap("orderId", orderId), locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "GIFT_CARD", null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString("paymentPropertiesPath");
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "AccountingGiftCerticateNumberCannotGetPaymentConfiguration", locale));
        }

        // party ID for tracking
        GenericValue placingParty = orh.getPlacingParty();
        String partyId = null;
        if (placingParty != null) {
            partyId = placingParty.getString("partyId");
        }

        // amount of the gift card reload
        BigDecimal amount = orderItem.getBigDecimal("unitPrice");

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.surveyId", delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            // there should be only one
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId, "orderItemSeqId", orderItem.get("orderItemSeqId"), "surveyId", surveyId)
                    .orderBy("-responseDate").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "AccountingGiftCerticateNumberCannotReload", locale));
        }

        // get the response answers
        List<GenericValue> responseAnswers = null;
        try {
            responseAnswers = surveyResponse.getRelated("SurveyResponseAnswer", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "AccountingGiftCerticateNumberCannotReloadFromSurveyAnswers", locale));
        }

        // make a map of answer info
        Map<String, Object> answerMap = new HashMap<String, Object>();
        if (responseAnswers != null) {
            for (GenericValue answer : responseAnswers) {
                GenericValue question = null;
                try {
                    question = answer.getRelatedOne("SurveyQuestion", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                            "AccountingGiftCerticateNumberCannotReloadFromSurveyAnswers", locale));
                }
                if (question != null) {
                    String desc = question.getString("description");
                    String ans = answer.getString("textResponse");  // only support text response types for now
                    answerMap.put(desc, ans);
                }
            }
        }

        String cardNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.cardNumber", delegator);
        String pinNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.pinNumber", delegator);
        String cardNumber = (String) answerMap.get(cardNumberKey);
        String pinNumber = (String) answerMap.get(pinNumberKey);

        // reload the gift card
        Map<String, Object> reloadCtx = new HashMap<String, Object>();
        reloadCtx.put("productStoreId", productStoreId);
        reloadCtx.put("currency", currency);
        reloadCtx.put("partyId", partyId);
        //reloadCtx.put("orderId", orderId);
        reloadCtx.put("cardNumber", cardNumber);
        reloadCtx.put("pinNumber", pinNumber);
        reloadCtx.put("amount", amount);
        reloadCtx.put("userLogin", userLogin);

        String errorMessage = null;
        Map<String, Object> reloadGcResult = null;
        try {
            reloadGcResult = dispatcher.runSync("addFundsToGiftCertificate", reloadCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            errorMessage = "Unable to call reload service!";
        }
        if (ServiceUtil.isError(reloadGcResult)) {
            errorMessage = ServiceUtil.getErrorMessage(reloadGcResult);
        }

        // create the fulfillment record
        Map<String, Object> gcFulFill = new HashMap<String, Object>();
        gcFulFill.put("typeEnumId", "GC_RELOAD");
        gcFulFill.put("userLogin", userLogin);
        gcFulFill.put("partyId", partyId);
        gcFulFill.put("orderId", orderId);
        gcFulFill.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
        gcFulFill.put("surveyResponseId", surveyResponse.get("surveyResponseId"));
        gcFulFill.put("cardNumber", cardNumber);
        gcFulFill.put("pinNumber", pinNumber);
        gcFulFill.put("amount", amount);
        if (reloadGcResult != null) {
            gcFulFill.put("responseCode", reloadGcResult.get("responseCode"));
            gcFulFill.put("referenceNum", reloadGcResult.get("referenceNum"));
        }
        try {
            dispatcher.runAsync("createGcFulFillmentRecord", gcFulFill, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotStoreFulfillmentInfo",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (errorMessage != null) {
            // there was a problem
            Debug.logError("Reload Failed Need to Refund : " + reloadGcResult, module);

            // process the return
            try {
                Map<String, Object> refundCtx = UtilMisc.toMap("orderItem", orderItem, 
                        "partyId", partyId, "userLogin", userLogin);
                dispatcher.runAsync("refundGcPurchase", refundCtx, null, true, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR! Unable to call create refund service; this failed reload will NOT be refunded", module);
            }

            return ServiceUtil.returnError(errorMessage);
        }

        // add some information to the answerMap for the email
        answerMap.put("processResult", reloadGcResult.get("processResult"));
        answerMap.put("responseCode", reloadGcResult.get("responseCode"));
        answerMap.put("previousAmount", reloadGcResult.get("previousBalance"));
        answerMap.put("amount", reloadGcResult.get("amount"));

        // get the email setting for this email type
        GenericValue productStoreEmail = null;
        String emailType = "PRDS_GC_RELOAD";
        try {
            productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", emailType).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get product store email setting for gift card purchase", module);
        }
        if (productStoreEmail == null) {
            Debug.logError("No gift card purchase email setting found for this store; cannot send gift card information", module);
        } else {
            answerMap.put("locale", locale);

            Map<String, Object> emailCtx = new HashMap<String, Object>();
            String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
            if (UtilValidate.isEmpty(bodyScreenLocation)) {
                bodyScreenLocation = ProductStoreWorker.getDefaultProductStoreEmailScreenLocation(emailType);
            }
            emailCtx.put("bodyScreenUri", bodyScreenLocation);
            emailCtx.put("bodyParameters", answerMap);
            emailCtx.put("sendTo", orh.getOrderEmailString());
            emailCtx.put("contentType", productStoreEmail.get("contentType"));
            emailCtx.put("sendFrom", productStoreEmail.get("fromAddress"));
            emailCtx.put("sendCc", productStoreEmail.get("ccAddress"));
            emailCtx.put("sendBcc", productStoreEmail.get("bccAddress"));
            emailCtx.put("subject", productStoreEmail.getString("subject"));
            emailCtx.put("userLogin", userLogin);

            // send off the email async so we will retry on failed attempts
            try {
                dispatcher.runAsync("sendMailFromScreen", emailCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem sending mail", module);
                // this is fatal; we will rollback and try again later
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingGiftCerticateNumberCannotSendEmailNotice",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // Tracking Service
    public static Map<String, Object> createFulfillmentRecord(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        // create the fulfillment record
        GenericValue gcFulFill = delegator.makeValue("GiftCardFulfillment");
        gcFulFill.set("fulfillmentId", delegator.getNextSeqId("GiftCardFulfillment"));
        gcFulFill.set("typeEnumId", context.get("typeEnumId"));
        gcFulFill.set("merchantId", context.get("merchantId"));
        gcFulFill.set("partyId", context.get("partyId"));
        gcFulFill.set("orderId", context.get("orderId"));
        gcFulFill.set("orderItemSeqId", context.get("orderItemSeqId"));
        gcFulFill.set("surveyResponseId", context.get("surveyResponseId"));
        gcFulFill.set("cardNumber", context.get("cardNumber"));
        gcFulFill.set("pinNumber", context.get("pinNumber"));
        gcFulFill.set("amount", context.get("amount"));
        gcFulFill.set("responseCode", context.get("responseCode"));
        gcFulFill.set("referenceNum", context.get("referenceNum"));
        gcFulFill.set("authCode", context.get("authCode"));
        gcFulFill.set("fulfillmentDate", UtilDateTime.nowTimestamp());
        try {
            delegator.create(gcFulFill);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotStoreFulfillmentInfo",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    // Refund Service
    public static Map<String, Object> refundGcPurchase(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");

        // refresh the item object for status changes
        try {
            orderItem.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        Map<String, Object> returnableInfo = null;
        try {
            returnableInfo = dispatcher.runSync("getReturnableQuantity", UtilMisc.toMap("orderItem", orderItem, 
                    "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                    "OrderErrorUnableToGetReturnItemInformation", locale));
        }

        if (returnableInfo != null) {
            BigDecimal returnableQuantity = (BigDecimal) returnableInfo.get("returnableQuantity");
            BigDecimal returnablePrice = (BigDecimal) returnableInfo.get("returnablePrice");
            Debug.logInfo("Returnable INFO : " + returnableQuantity + " @ " + returnablePrice + " :: " + orderItem, module);

            // create the return header
            Map<String, Object> returnHeaderInfo = new HashMap<String, Object>();
            returnHeaderInfo.put("fromPartyId", partyId);
            returnHeaderInfo.put("userLogin", userLogin);
            Map<String, Object> returnHeaderResp = null;
            try {
                returnHeaderResp = dispatcher.runSync("createReturnHeader", returnHeaderInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorUnableToCreateReturnHeader", locale));
            }

            if (returnHeaderResp != null) {
                String errorMessage = ServiceUtil.getErrorMessage(returnHeaderResp);
                if (errorMessage != null) {
                    return ServiceUtil.returnError(errorMessage);
                }
            }

            String returnId = null;
            if (returnHeaderResp != null) {
                returnId = (String) returnHeaderResp.get("returnId");
            }

            if (returnId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorCreateReturnHeaderWithoutId", locale));
            }

            // create the return item
            Map<String, Object> returnItemInfo = new HashMap<String, Object>();
            returnItemInfo.put("returnId", returnId);
            returnItemInfo.put("returnReasonId", "RTN_DIG_FILL_FAIL");
            returnItemInfo.put("returnTypeId", "RTN_REFUND");
            returnItemInfo.put("returnItemType", "ITEM");
            returnItemInfo.put("description", orderItem.get("itemDescription"));
            returnItemInfo.put("orderId", orderItem.get("orderId"));
            returnItemInfo.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
            returnItemInfo.put("returnQuantity", returnableQuantity);
            returnItemInfo.put("returnPrice", returnablePrice);
            returnItemInfo.put("userLogin", userLogin);
            Map<String, Object> returnItemResp = null;
            try {
                returnItemResp = dispatcher.runSync("createReturnItem", returnItemInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorUnableToCreateReturnItem", locale));
            }

            if (returnItemResp != null) {
                String errorMessage = ServiceUtil.getErrorMessage(returnItemResp);
                if (errorMessage != null) {
                    return ServiceUtil.returnError(errorMessage);
                }
            }

            String returnItemSeqId = null;
            if (returnItemResp != null) {
                returnItemSeqId = (String) returnItemResp.get("returnItemSeqId");
            }

            if (returnItemSeqId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorCreateReturnItemWithoutId", locale));
            } else {
                Debug.logVerbose("Created return item : " + returnId + " / " + returnItemSeqId, module);
            }

            // need the system userLogin to "fake" out the update service
            GenericValue admin = null;
            try {
                admin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorUnableToUpdateReturnHeaderStatusWithoutUserLogin", locale));
            }

            // update the status to received so it can process
            Map<String, Object> updateReturnInfo = new HashMap<String, Object>();
            updateReturnInfo.put("returnId", returnId);
            updateReturnInfo.put("statusId", "RETURN_RECEIVED");
            updateReturnInfo.put("currentStatusId", "RETURN_REQUESTED");
            updateReturnInfo.put("userLogin", admin);
            Map<String, Object> updateReturnResp = null;
            try {
                updateReturnResp = dispatcher.runSync("updateReturnHeader", updateReturnInfo);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrderError, 
                        "OrderErrorUnableToUpdateReturnHeaderStatus", locale));
            }

            if (updateReturnResp != null) {
                String errorMessage = ServiceUtil.getErrorMessage(updateReturnResp);
                if (errorMessage != null) {
                    return ServiceUtil.returnError(errorMessage);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // Private worker methods
    private static boolean validatePin(Delegator delegator, String cardNumber, String pinNumber) {
        GenericValue finAccount = null;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", cardNumber).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (finAccount != null) {
            String dbPin = finAccount.getString("finAccountCode");
            Debug.logInfo("GC Pin Validation: [Sent: " + pinNumber + "] [Actual: " + dbPin + "]", module);
            if (dbPin != null && dbPin.equals(pinNumber)) {
                return true;
            }
        } else {
            Debug.logInfo("GC FinAccount record not found (" + cardNumber + ")", module);
        }
        return false;
    }

    private static String createTransaction(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, 
            BigDecimal amount, String productStoreId, String partyId, String currencyUom, String txType, 
            String finAccountId, Locale locale) throws GeneralException {
        final String coParty = getPayToPartyId(delegator, productStoreId);
        final String paymentMethodType = "GIFT_CERTIFICATE";

        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }

        String paymentType = null;
        String partyIdFrom = null;
        String partyIdTo = null;
        if ("DEPOSIT".equals(txType)) {
            paymentType = "GC_DEPOSIT";
            partyIdFrom = partyId;
            partyIdTo = coParty;
        } else if ("WITHDRAWAL".equals(txType)) {
            paymentType = "GC_WITHDRAWAL";
            partyIdFrom = coParty;
            partyIdTo = partyId;
        } else {
            throw new GeneralException(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountCannotCreateTransaction", locale));
        }

        // create the payment for the transaction
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap("paymentTypeId", paymentType);
        paymentCtx.put("paymentMethodTypeId", paymentMethodType);
        //paymentCtx.put("paymentMethodId", "");
        //paymentCtx.put("paymentGatewayResponseId", "");
        paymentCtx.put("partyIdTo", partyIdTo);
        paymentCtx.put("partyIdFrom", partyIdFrom);
        paymentCtx.put("statusId", "PMNT_RECEIVED");
        //paymentCtx.put("paymentPreferenceId", "");
        paymentCtx.put("currencyUomId", currencyUom);
        paymentCtx.put("amount", amount);
        paymentCtx.put("userLogin", userLogin);
        paymentCtx.put("paymentRefNum", "N/A");

        String paymentId = null;
        Map<String, Object> payResult = null;
        try {
            payResult = dispatcher.runSync("createPayment", paymentCtx);
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (payResult == null) {
            throw new GeneralException("Unknow error in creating financial account transaction!");
        }
        if (ServiceUtil.isError(payResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(payResult));
        } else {
            paymentId = (String) payResult.get("paymentId");
        }

        // create the initial transaction
        Map<String, Object> transCtx = UtilMisc.<String, Object>toMap("finAccountTransTypeId", txType);
        transCtx.put("finAccountId", finAccountId);
        transCtx.put("partyId", userLogin.getString("partyId"));
        transCtx.put("userLogin", userLogin);
        transCtx.put("paymentId", paymentId);
        transCtx.put("amount", amount);

        Map<String, Object> transResult = null;
        String txId = null;
        try {
            transResult = dispatcher.runSync("createFinAccountTrans", transCtx);
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (transResult == null) {
            throw new GeneralException("Unknown error in creating financial account transaction!");
        }
        if (ServiceUtil.isError(transResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
        } else {
            txId = (String) transResult.get("finAccountTransId");
        }

        return txId;
    }

    private static String generateNumber(Delegator delegator, int length, boolean isId) throws GenericEntityException {
        if (length > 19) {
            length = 19;
        }

        Random rand = new SecureRandom();
        boolean isValid = false;
        StringBuilder number = null;
        while (!isValid) {
            number = new StringBuilder("");
            for (int i = 0; i < length; i++) {
                int randInt = rand.nextInt(9);
                number.append(randInt);
            }

            if (isId) {
                number.append(UtilValidate.getLuhnCheckDigit(number.toString()));

                // validate the number
                if (checkCardNumber(number.toString())) {
                    // make sure this number doens't already exist
                    isValid = checkNumberInDatabase(delegator, number.toString());
                }
            } else {
                isValid = true;
            }
        }
        return number.toString();
    }

    private static boolean checkNumberInDatabase(Delegator delegator, String number) throws GenericEntityException {
        GenericValue finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", number).queryOne();
        if (finAccount == null) {
            return true;
        }
        return false;
    }

    private static boolean checkCardNumber(String number) {
        number = number.replaceAll("\\D", "");
        return UtilValidate.sumIsMod10(UtilValidate.getLuhnSum(number));
    }

    private static String getPayToPartyId(Delegator delegator, String productStoreId) {
        String payToPartyId = "Company"; // default value
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to locate ProductStore (" + productStoreId + ")", module);
            return null;
        }
        if (productStore != null && productStore.get("payToPartyId") != null) {
            payToPartyId = productStore.getString("payToPartyId");
        }
        return payToPartyId;
    }
}
