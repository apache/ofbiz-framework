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

package org.apache.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * FinAccountPaymentServices - Financial account used as payment method
 */
public class FinAccountPaymentServices {

    private static final String MODULE = FinAccountPaymentServices.class.getName();
    private static final String RES_ERROR = "AccountingErrorUiLabels";

    // base payment integration services
    public static Map<String, Object> finAccountPreAuth(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String finAccountCode = (String) context.get("finAccountCode");
        String finAccountPin = (String) context.get("finAccountPin");
        String finAccountId = (String) context.get("finAccountId");
        String orderId = (String) context.get("orderId");
        BigDecimal amount = (BigDecimal) context.get("processAmount");

        if (paymentPref != null) {
            // check for an existing auth trans and cancel it
            GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTrans != null) {
                Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "finAccountAuthId",
                        authTrans.get("referenceNum"));
                try {
                    Map<String, Object> result = dispatcher.runSync("expireFinAccountAuth", input);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            if (finAccountId == null) {
                finAccountId = paymentPref.getString("finAccountId");
            }
        }

        // obtain the order information
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);

        // NOTE DEJ20070808: this means that we want store related settings for where
        // the item is being purchased,
        // NOT where the account was setup; should this be changed to use settings from
        // the store where the account was setup?
        String productStoreId = orh.getProductStoreId();

        // TODO, NOTE DEJ20070808: why is this setup this way anyway? for the
        // allowAuthToNegative wouldn't that be better setup
        // on the FinAccount and not on the ProductStoreFinActSetting? maybe an override
        // on the FinAccount would be good...

        // get the financial account
        GenericValue finAccount;
        if (finAccountId != null) {
            try {
                finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId)
                        .queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            if (finAccountCode != null) {
                try {
                    finAccount = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "AccountingFinAccountCannotLocateItFromAccountCode", locale));
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingFinAccountIdAndFinAccountCodeAreNull", locale));
            }
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountIdInvalid", locale));
        }

        String finAccountTypeId = finAccount.getString("finAccountTypeId");
        finAccountId = finAccount.getString("finAccountId");
        String statusId = finAccount.getString("statusId");

        try {
            // fin the store requires a pin number; validate the PIN with the code
            Map<String, Object> findProductStoreFinActSettingMap = UtilMisc.<String, Object>toMap("productStoreId",
                    productStoreId, "finAccountTypeId", finAccountTypeId);
            GenericValue finAccountSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting").where(
                    findProductStoreFinActSettingMap).cache().queryOne();

            if (finAccountSettings == null) {
                Debug.logWarning(
                        "In finAccountPreAuth could not find ProductStoreFinActSetting record, values searched by: "
                                + findProductStoreFinActSettingMap, MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("In finAccountPreAuth finAccountSettings=" + finAccountSettings, MODULE);
            }

            BigDecimal minBalance = FinAccountHelper.getZero();
            String allowAuthToNegative = "N";

            if (finAccountSettings != null) {
                allowAuthToNegative = finAccountSettings.getString("allowAuthToNegative");
                minBalance = finAccountSettings.getBigDecimal("minBalance");
                if (minBalance == null) {
                    minBalance = FinAccountHelper.getZero();
                }

                // validate the PIN if the store requires it
                if ("Y".equals(finAccountSettings.getString("requirePinCode"))) {
                    if (!FinAccountHelper.validatePin(delegator, finAccountCode, finAccountPin)) {
                        Map<String, Object> result = ServiceUtil.returnSuccess();
                        result.put("authMessage", UtilProperties.getMessage(RES_ERROR,
                                "AccountingFinAccountPinCodeCombinatorNotFound", locale));
                        result.put("authResult", Boolean.FALSE);
                        result.put("processAmount", amount);
                        result.put("authFlag", "0");
                        result.put("authCode", "A");
                        result.put("authRefNum", "0");
                        Debug.logWarning("Unable to auth FinAccount: " + result, MODULE);
                        return result;
                    }
                }
            }

            // check for expiration date
            if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(
                    UtilDateTime.nowTimestamp()))) {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("authMessage", UtilProperties.getMessage(RES_ERROR,
                        "AccountingFinAccountExpired",
                        UtilMisc.toMap("thruDate", finAccount.getTimestamp("thruDate")), locale));
                result.put("authResult", Boolean.FALSE);
                result.put("processAmount", amount);
                result.put("authFlag", "0");
                result.put("authCode", "A");
                result.put("authRefNum", "0");
                Debug.logWarning("Unable to auth FinAccount: " + result, MODULE);
                return result;
            }

            // check for account being in bad standing somehow
            if ("FNACT_NEGPENDREPL".equals(statusId) || "FNACT_MANFROZEN".equals(statusId) || "FNACT_CANCELLED".equals(
                    statusId)) {
                // refresh the finaccount
                finAccount.refresh();
                statusId = finAccount.getString("statusId");

                if ("FNACT_NEGPENDREPL".equals(statusId) || "FNACT_MANFROZEN".equals(statusId) || "FNACT_CANCELLED"
                        .equals(statusId)) {
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    if ("FNACT_NEGPENDREPL".equals(statusId)) {
                        result.put("authMessage", UtilProperties.getMessage(RES_ERROR,
                                "AccountingFinAccountNegative", locale));
                    } else if ("FNACT_MANFROZEN".equals(statusId)) {
                        result.put("authMessage", UtilProperties.getMessage(RES_ERROR,
                                "AccountingFinAccountFrozen", locale));
                    } else if ("FNACT_CANCELLED".equals(statusId)) {
                        result.put("authMessage", UtilProperties.getMessage(RES_ERROR,
                                "AccountingFinAccountCancelled", locale));
                    }
                    result.put("authResult", Boolean.FALSE);
                    result.put("processAmount", amount);
                    result.put("authFlag", "0");
                    result.put("authCode", "A");
                    result.put("authRefNum", "0");
                    Debug.logWarning("Unable to auth FinAccount: " + result, MODULE);
                    return result;
                }
            }

            // check the amount to authorize against the available balance of fin account,
            // which includes active authorizations as well as transactions
            BigDecimal availableBalance = finAccount.getBigDecimal("availableBalance");
            if (availableBalance == null) {
                availableBalance = FinAccountHelper.getZero();
            } else {
                BigDecimal availableBalanceOriginal = availableBalance;
                availableBalance = availableBalance.setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());
                if (availableBalance.compareTo(availableBalanceOriginal) != 0) {
                    Debug.logWarning("In finAccountPreAuth for finAccountId [" + finAccountId + "] availableBalance ["
                            + availableBalanceOriginal + "] was different after rounding [" + availableBalance
                            + "]; it should never have made it into the database this way, so check whatever put it there.",
                            MODULE);
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            String authMessage = null;
            Boolean processResult;
            String refNum;

            // make sure to round and scale it to the same as availableBalance
            amount = amount.setScale(FinAccountHelper.getDecimals(), FinAccountHelper.getRounding());

            Debug.logInfo("Allow auth to negative: " + allowAuthToNegative + " :: available: " + availableBalance + " comp: "
                    + minBalance + " = " + availableBalance.compareTo(minBalance) + " :: req: " + amount, MODULE);
            // check the available balance to see if we can auth this tx
            if (("Y".equals(allowAuthToNegative) && availableBalance.compareTo(minBalance) > -1)
                    || (availableBalance.compareTo(amount) > -1)) {
                Timestamp thruDate;

                if (finAccountSettings != null && finAccountSettings.getLong("authValidDays") != null) {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), finAccountSettings.getLong("authValidDays"));
                } else {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), 30L); // default 30 days for an auth
                }

                Map<String, Object> tmpResult = dispatcher.runSync("createFinAccountAuth", UtilMisc.<String, Object>toMap("finAccountId",
                        finAccountId, "amount", amount, "thruDate", thruDate, "userLogin", userLogin));

                if (ServiceUtil.isError(tmpResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                }
                refNum = (String) tmpResult.get("finAccountAuthId");
                processResult = Boolean.TRUE;

                // refresh the account
                finAccount.refresh();
            } else {
                Debug.logWarning("Attempted to authorize [" + amount + "] against a balance of only ["
                        + availableBalance + "] for finAccountId [" + finAccountId + "]", MODULE);
                refNum = "0"; // a refNum is always required from authorization
                authMessage = "Insufficient funds";
                processResult = Boolean.FALSE;
            }

            result.put("processAmount", amount);
            result.put("authMessage", authMessage);
            result.put("authResult", processResult);
            result.put("authFlag", "1");
            result.put("authCode", "A");
            result.put("authRefNum", refNum);
            Debug.logInfo("FinAccont Auth: " + result, MODULE);

            return result;
        } catch (GenericEntityException | GenericServiceException ex) {
            Debug.logError(ex, "Cannot authorize financial account", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountCannotBeAuthorized",
                    UtilMisc.toMap("errorString", ex.getMessage()), locale));
        }
    }

    public static Map<String, Object> finAccountReleaseAuth(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Locale locale = (Locale) context.get("locale");

        String err = UtilProperties.getMessage(RES_ERROR, "AccountingFinAccountCannotBeExpired", locale);
        try {

            // expire the related financial authorization transaction
            GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTransaction == null) {
                return ServiceUtil.returnError(err + UtilProperties.getMessage(RES_ERROR,
                        "AccountingFinAccountCannotFindAuthorization", locale));
            }

            Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "finAccountAuthId", authTransaction.get("referenceNum"));
            Map<String, Object> serviceResults = dispatcher.runSync("expireFinAccountAuth", input);
            // if there's an error, don't release
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(err + ServiceUtil.getErrorMessage(serviceResults));
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("releaseRefNum", authTransaction.getString("referenceNum"));
            result.put("releaseAmount", authTransaction.getBigDecimal("amount"));
            result.put("releaseResult", Boolean.TRUE);

            return result;
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(err + e.getMessage());
        }
    }

    public static Map<String, Object> finAccountCapture(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        BigDecimal amount = (BigDecimal) context.get("captureAmount");
        String currency = (String) context.get("currency");

        // get the authorization transaction
        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountCannotCapture", locale));
        }

        // get the auth record
        String finAccountAuthId = authTrans.getString("referenceNum");
        GenericValue finAccountAuth;
        try {
            finAccountAuth = EntityQuery.use(delegator).from("FinAccountAuth").where("finAccountAuthId",
                    finAccountAuthId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Debug.logInfo("Financial account capture [" + finAccountAuth.get("finAccountId") + "] for the amount of $"
                + amount + " Tx #" + finAccountAuth.get("finAccountAuthId"), MODULE);

        // get the financial account
        GenericValue finAccount;
        try {
            finAccount = finAccountAuth.getRelatedOne("FinAccount", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure authorization has not expired
        Timestamp authExpiration = finAccountAuth.getTimestamp("thruDate");
        if ((authExpiration != null) && (authExpiration.before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountAuthorizationExpired",
                    UtilMisc.toMap("paymentGatewayResponseId", authTrans.getString("paymentGatewayResponseId"),
                            "authExpiration", authExpiration), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountExpired",
                    UtilMisc.toMap("thruDate", finAccount.getTimestamp("thruDate")), locale));
        }
        String finAccountId = finAccount.getString("finAccountId");

        // need the product store ID & party ID
        String orderId = orderPaymentPreference.getString("orderId");
        String productStoreId = null;
        String partyId = null;
        if (orderId != null) {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            productStoreId = orh.getProductStoreId();

            GenericValue billToParty = orh.getBillToParty();
            if (billToParty != null) {
                partyId = billToParty.getString("partyId");
            }
        }

        // BIG NOTE: make sure the expireFinAccountAuth and finAccountWithdraw services are done in the SAME TRANSACTION
        //(i.e. no require-new-transaction in either of them AND no running async)

        // cancel the authorization before doing the withdraw to avoid problems with way negative available amount on account;
        // should happen in same transaction to avoid conflict problems
        Map<String, Object> releaseResult;
        try {
            releaseResult = dispatcher.runSync("expireFinAccountAuth", UtilMisc.<String, Object>toMap("userLogin", userLogin, "finAccountAuthId",
                    finAccountAuthId));
            if (ServiceUtil.isError(releaseResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // build the withdraw context
        Map<String, Object> withdrawCtx = new HashMap<>();
        withdrawCtx.put("finAccountId", finAccountId);
        withdrawCtx.put("productStoreId", productStoreId);
        withdrawCtx.put("currency", currency);
        withdrawCtx.put("partyId", partyId);
        withdrawCtx.put("orderId", orderId);
        withdrawCtx.put("amount", amount);
        withdrawCtx.put("reasonEnumId", "FATR_PURCHASE");
        withdrawCtx.put("requireBalance", Boolean.FALSE); // for captures; if auth passed, allow
        withdrawCtx.put("userLogin", userLogin);

        // call the withdraw service
        Map<String, Object> withdrawResp;
        try {
            withdrawResp = dispatcher.runSync("finAccountWithdraw", withdrawCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(withdrawResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(withdrawResp));
        }

        // create the capture response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) withdrawResp.get("processResult");
        BigDecimal withdrawAmount = (BigDecimal) withdrawResp.get("amount");
        String referenceNum = (String) withdrawResp.get("referenceNum");
        result.put("captureResult", processResult);
        result.put("captureRefNum", referenceNum);
        result.put("captureCode", "C");
        result.put("captureFlag", "1");
        result.put("captureAmount", withdrawAmount);

        return result;
    }

    public static Map<String, Object> finAccountRefund(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");
        String currency = (String) context.get("currency");
        String finAccountId = (String) context.get("finAccountId");

        String productStoreId = null;
        String partyId = null;

        String orderId = null;
        if (orderPaymentPreference != null) {
            orderId = orderPaymentPreference.getString("orderId");
            if (orderId != null) {
                OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
                productStoreId = orh.getProductStoreId();

                GenericValue billToParty = orh.getBillToParty();
                if (billToParty != null) {
                    partyId = billToParty.getString("partyId");
                }
            }
            if (finAccountId == null) {
                finAccountId = orderPaymentPreference.getString("finAccountId");
            }
        }

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", ""), locale));
        }

        // call the deposit service
        Map<String, Object> depositCtx = new HashMap<>();
        depositCtx.put("finAccountId", finAccountId);
        depositCtx.put("productStoreId", productStoreId);
        depositCtx.put("isRefund", Boolean.TRUE);
        depositCtx.put("currency", currency);
        depositCtx.put("partyId", partyId);
        depositCtx.put("orderId", orderId);
        depositCtx.put("amount", amount);
        depositCtx.put("reasonEnumId", "FATR_REFUND");
        depositCtx.put("userLogin", userLogin);

        Map<String, Object> depositResp;
        try {
            depositResp = dispatcher.runSync("finAccountDeposit", depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depositResp));
        }

        // create the refund response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) depositResp.get("processResult");
        BigDecimal depositAmount = (BigDecimal) depositResp.get("amount");
        String referenceNum = (String) depositResp.get("referenceNum");
        result.put("refundResult", processResult);
        result.put("refundRefNum", referenceNum);
        result.put("refundCode", "R");
        result.put("refundFlag", "1");
        result.put("refundAmount", depositAmount);

        return result;
    }

    // base account transaction services
    public static Map<String, Object> finAccountWithdraw(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String reasonEnumId = (String) context.get("reasonEnumId");
        String orderId = (String) context.get("orderId");
        Boolean requireBalance = (Boolean) context.get("requireBalance");
        BigDecimal amount = (BigDecimal) context.get("amount");
        if (requireBalance == null) {
            requireBalance = Boolean.TRUE;
        }

        final String withdrawal = "WITHDRAWAL";

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
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountMustBePositive", locale));
        }

        GenericValue finAccount;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", ""), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountExpired",
                    UtilMisc.toMap("thruDate", finAccount.getTimestamp("thruDate")), locale));
        }

        // check the actual balance (excluding authorized amounts) and create the
        // transaction if it is sufficient
        BigDecimal previousBalance = finAccount.getBigDecimal("actualBalance");
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.getZero();
        }

        BigDecimal balance;
        String refNum;
        Boolean procResult;
        if (requireBalance && previousBalance.compareTo(amount) < 0) {
            procResult = Boolean.FALSE;
            balance = previousBalance;
            refNum = "N/A";
        } else {
            try {
                refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin,
                        amount,
                        productStoreId, partyId, orderId, orderItemSeqId, currencyUom, withdrawal, finAccountId,
                        reasonEnumId);
                finAccount.refresh();
                balance = finAccount.getBigDecimal("actualBalance");
                procResult = Boolean.TRUE;
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // make sure balance is not null
        if (balance == null) {
            balance = FinAccountHelper.getZero();
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("previousBalance", previousBalance);
        result.put("balance", balance);
        result.put("amount", amount);
        result.put("processResult", procResult);
        result.put("referenceNum", refNum);
        return result;
    }

    // base deposit service
    public static Map<String, Object> finAccountDeposit(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String reasonEnumId = (String) context.get("reasonEnumId");
        String orderId = (String) context.get("orderId");
        Boolean isRefund = (Boolean) context.get("isRefund");
        BigDecimal amount = (BigDecimal) context.get("amount");

        final String deposit = isRefund == null || !isRefund ? "DEPOSIT" : "ADJUSTMENT";

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        GenericValue finAccount;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", finAccountId), locale));
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", ""), locale));
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime
                .nowTimestamp()))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountExpired",
                    UtilMisc.toMap("thruDate", finAccount.getTimestamp("thruDate")), locale));
        }
        Debug.logInfo("Deposit into financial account #" + finAccountId + " [" + amount + "]", MODULE);

        // get the previous balance
        BigDecimal previousBalance = finAccount.getBigDecimal("actualBalance");
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.getZero();
        }

        // create the transaction
        BigDecimal actualBalance;
        String refNum;
        try {
            refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin, amount,
                    productStoreId, partyId, orderId, orderItemSeqId, currencyUom, deposit, finAccountId, reasonEnumId);
            finAccount.refresh();
            actualBalance = finAccount.getBigDecimal("actualBalance");
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure balance is not null
        if (actualBalance == null) {
            actualBalance = FinAccountHelper.getZero();
        } else {
            if (actualBalance.compareTo(BigDecimal.ZERO) < 0) {
                // balance went below zero, set negative pending replenishment status so that no
                // more auths or captures will go through until it is replenished
                try {
                    Map<String, Object> rollbackCtx = UtilMisc.toMap("userLogin", userLogin, "finAccountId",
                            finAccountId, "statusId", "FNACT_NEGPENDREPL");
                    dispatcher.addRollbackService("updateFinAccount", rollbackCtx, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("previousBalance", previousBalance);
        result.put("balance", actualBalance);
        result.put("amount", amount);
        result.put("processResult", Boolean.TRUE);
        result.put("referenceNum", refNum);
        return result;
    }

    // auto-replenish service (deposit)
    public static Map<String, Object> finAccountReplenish(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");

        // lookup the FinAccount
        GenericValue finAccount;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", finAccountId), locale));
        }
        String currency = finAccount.getString("currencyUomId");
        String statusId = finAccount.getString("statusId");

        // look up the type -- determine auto-replenish is active
        GenericValue finAccountType;
        try {
            finAccountType = finAccount.getRelatedOne("FinAccountType", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        String replenishEnumId = finAccountType.getString("replenishEnumId");
        if (!"FARP_AUTOMATIC".equals(replenishEnumId)) {
            // type does not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // attempt to lookup the product store from a previous deposit
        if (productStoreId == null) {
            productStoreId = getLastProductStoreId(delegator, finAccountId);
            if (productStoreId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "AccountingFinAccountCannotBeReplenish", locale));
            }
        }

        // get the product store settings
        GenericValue finAccountSettings;
        Map<String, Object> psfasFindMap = UtilMisc.<String, Object>toMap("productStoreId", productStoreId,
                "finAccountTypeId", finAccount.getString("finAccountTypeId"));
        try {
            finAccountSettings = EntityQuery.use(delegator).from("ProductStoreFinActSetting").where(psfasFindMap)
                    .cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccountSettings == null) {
            Debug.logWarning("finAccountReplenish Warning: not replenishing FinAccount [" + finAccountId
                    + "] because no ProductStoreFinActSetting record found for: " + psfasFindMap, MODULE);
            // no settings; don't replenish
            return ServiceUtil.returnSuccess();
        }

        BigDecimal replenishThreshold = finAccountSettings.getBigDecimal("replenishThreshold");
        if (replenishThreshold == null) {
            Debug.logWarning("finAccountReplenish Warning: not replenishing FinAccount [" + finAccountId
                    + "] because ProductStoreFinActSetting.replenishThreshold field was null for: " + psfasFindMap,
                    MODULE);
            return ServiceUtil.returnSuccess();
        }

        BigDecimal replenishLevel = finAccount.getBigDecimal("replenishLevel");
        if (replenishLevel == null || replenishLevel.compareTo(BigDecimal.ZERO) == 0) {
            Debug.logWarning("finAccountReplenish Warning: not replenishing FinAccount [" + finAccountId
                    + "] because FinAccount.replenishLevel field was null or 0", MODULE);
            // no replenish level set; this account goes not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // get the current balance
        BigDecimal balance = finAccount.getBigDecimal("actualBalance");

        // see if we are within the threshold for replenishment
        if (balance.compareTo(replenishThreshold) > -1) {
            Debug.logInfo("finAccountReplenish Info: Not replenishing FinAccount [" + finAccountId
                    + "] because balance [" + balance + "] is greater than the replenishThreshold ["
                    + replenishThreshold + "]", MODULE);
            // not ready
            return ServiceUtil.returnSuccess();
        }

        // configure rollback service to set status to Negative Pending Replenishment
        if ("FNACT_NEGPENDREPL".equals(statusId)) {
            try {
                Map<String, Object> rollbackCtx = UtilMisc.toMap("userLogin", userLogin, "finAccountId", finAccountId,
                        "statusId", "FNACT_NEGPENDREPL");
                dispatcher.addRollbackService("updateFinAccount", rollbackCtx, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        String replenishMethod = finAccountSettings.getString("replenishMethodEnumId");
        BigDecimal depositAmount;
        if (replenishMethod == null || "FARP_TOP_OFF".equals(replenishMethod)) {
            // the deposit is level - balance (500 - (-10) = 510 || 500 - (10) = 490)
            depositAmount = replenishLevel.subtract(balance);
        } else if ("FARP_REPLENISH_LEVEL".equals(replenishMethod)) {
            // the deposit is replenish-level itself
            depositAmount = replenishLevel;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountUnknownReplenishMethod", locale));
        }

        // get the owner party
        String ownerPartyId = finAccount.getString("ownerPartyId");
        if (ownerPartyId == null) {
            // no owner cannot replenish; (not fatal, just not supported by this account)
            Debug.logWarning("finAccountReplenish Warning: No owner attached to financial account [" + finAccountId
                    + "] cannot auto-replenish", MODULE);
            return ServiceUtil.returnSuccess();
        }

        // get the payment method to use to replenish
        String paymentMethodId = finAccount.getString("replenishPaymentId");
        if (paymentMethodId == null) {
            Debug.logWarning(
                    "finAccountReplenish Warning: No payment method (replenishPaymentId) attached to financial account ["
                            + finAccountId + "] cannot auto-replenish", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNoPaymentMethodAssociatedWithReplenishAccount", locale));
        }

        GenericValue paymentMethod;
        try {
            paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId)
                    .queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (paymentMethod == null) {
            // no payment methods on file; cannot replenish
            Debug.logWarning("finAccountReplenish Warning: No payment method found for ID [" + paymentMethodId
                    + "] for party [" + ownerPartyId + "] cannot auto-replenish", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "AccountingFinAccountNoPaymentMethodAssociatedWithReplenishAccount", locale));
        }

        // hit the payment method for the amount to replenish
        Map<String, BigDecimal> orderItemMap = UtilMisc.toMap("Auto-Replenishment FA #" + finAccountId, depositAmount);
        Map<String, Object> replOrderCtx = new HashMap<>();
        replOrderCtx.put("productStoreId", productStoreId);
        replOrderCtx.put("paymentMethodId", paymentMethod.getString("paymentMethodId"));
        replOrderCtx.put("currency", currency);
        replOrderCtx.put("partyId", ownerPartyId);
        replOrderCtx.put("itemMap", orderItemMap);
        replOrderCtx.put("userLogin", userLogin);
        Map<String, Object> replResp;
        try {
            replResp = dispatcher.runSync("createSimpleNonProductSalesOrder", replOrderCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(replResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(replResp));
        }
        String orderId = (String) replResp.get("orderId");

        // create the deposit
        Map<String, Object> depositCtx = new HashMap<>();
        depositCtx.put("productStoreId", productStoreId);
        depositCtx.put("finAccountId", finAccountId);
        depositCtx.put("currency", currency);
        depositCtx.put("partyId", ownerPartyId);
        depositCtx.put("orderId", orderId);
        depositCtx.put("orderItemSeqId", "00001"); // always one item on a replish order
        depositCtx.put("amount", depositAmount);
        depositCtx.put("reasonEnumId", "FATR_REPLENISH");
        depositCtx.put("userLogin", userLogin);
        try {
            Map<String, Object> depositResp = dispatcher.runSync("finAccountDeposit", depositCtx);
            if (ServiceUtil.isError(depositResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depositResp));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // say we are in good standing again
        if ("FNACT_NEGPENDREPL".equals(statusId)) {
            try {
                Map<String, Object> ufaResp = dispatcher.runSync("updateFinAccount",
                        UtilMisc.<String, Object>toMap("finAccountId", finAccountId, "statusId", "FNACT_ACTIVE", "userLogin", userLogin));
                if (ServiceUtil.isError(ufaResp)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(ufaResp));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    private static String getLastProductStoreId(Delegator delegator, String finAccountId) {
        GenericValue trans = null;
        try {
            trans = EntityQuery.use(delegator).from("FinAccountTrans")
                    .where(
                            EntityCondition.makeCondition("finAccountTransTypeId", EntityOperator.EQUALS, "DEPOSIT"),
                            EntityCondition.makeCondition("finAccountId", EntityOperator.EQUALS, finAccountId),
                            EntityCondition.makeCondition("orderId", EntityOperator.NOT_EQUAL, null)).orderBy(
                                    "-transactionDate").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (trans != null) {
            String orderId = trans.getString("orderId");
            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            return orh.getProductStoreId();
        }

        // none found; pick one from our set stores
        try {
            GenericValue store = EntityQuery.use(delegator).from("ProductStore").orderBy("productStoreId").queryFirst();
            if (store != null) {
                return store.getString("productStoreId");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return null;
    }

    private static String createFinAcctPaymentTransaction(Delegator delegator, LocalDispatcher dispatcher,
            GenericValue userLogin, BigDecimal amount,
            String productStoreId, String partyId, String orderId, String orderItemSeqId, String currencyUom,
            String txType, String finAccountId, String reasonEnumId) throws GeneralException {

        final String coParty = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
        final String paymentMethodType = "FIN_ACCOUNT";

        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }

        String paymentType;
        String partyIdFrom;
        String partyIdTo;
        BigDecimal paymentAmount;

        // determine the payment type and which direction the parties should go
        if ("DEPOSIT".equals(txType)) {
            paymentType = "RECEIPT";
            partyIdFrom = partyId;
            partyIdTo = coParty;
            paymentAmount = amount;
        } else if ("WITHDRAWAL".equals(txType)) {
            paymentType = "DISBURSEMENT";
            partyIdFrom = coParty;
            partyIdTo = partyId;
            paymentAmount = amount;
        } else if ("ADJUSTMENT".equals(txType)) {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                paymentType = "DISBURSEMENT";
                partyIdFrom = coParty;
                partyIdTo = partyId;
                paymentAmount = amount.negate(); // must be positive
            } else {
                paymentType = "RECEIPT";
                partyIdFrom = partyId;
                partyIdTo = coParty;
                paymentAmount = amount;
            }
        } else {
            throw new GeneralException("Unable to create financial account transaction!");
        }

        // payment amount should always be positive; adjustments may
        // create the payment for the transaction
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap("paymentTypeId", paymentType);
        paymentCtx.put("paymentMethodTypeId", paymentMethodType);
        paymentCtx.put("partyIdTo", partyIdTo);
        paymentCtx.put("partyIdFrom", partyIdFrom);
        paymentCtx.put("statusId", "PMNT_RECEIVED");
        paymentCtx.put("currencyUomId", currencyUom);
        paymentCtx.put("amount", paymentAmount);
        paymentCtx.put("userLogin", userLogin);
        paymentCtx.put("paymentRefNum", Long.toString(UtilDateTime.nowTimestamp().getTime()));

        String paymentId;
        Map<String, Object> payResult;
        try {
            payResult = dispatcher.runSync("createPayment", paymentCtx);
            if (ServiceUtil.isError(payResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(payResult));
            }
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (payResult == null) {
            throw new GeneralException("Unknown error in creating financial account transaction!");
        }
        if (ServiceUtil.isError(payResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(payResult));
        }
        paymentId = (String) payResult.get("paymentId");

        // create the initial transaction
        Map<String, Object> transCtx = UtilMisc.<String, Object>toMap("finAccountTransTypeId", txType);
        transCtx.put("finAccountId", finAccountId);
        transCtx.put("partyId", partyId);
        transCtx.put("orderId", orderId);
        transCtx.put("orderItemSeqId", orderItemSeqId);
        transCtx.put("reasonEnumId", reasonEnumId);
        transCtx.put("amount", amount);
        transCtx.put("userLogin", userLogin);
        transCtx.put("paymentId", paymentId);

        Map<String, Object> transResult;
        try {
            transResult = dispatcher.runSync("createFinAccountTrans", transCtx);
            if (ServiceUtil.isError(transResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
            }
        } catch (GenericServiceException e) {
            throw new GeneralException(e);
        }
        if (transResult == null) {
            throw new GeneralException("Unknown error in creating financial account transaction!");
        }
        if (ServiceUtil.isError(transResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(transResult));
        }

        return (String) transResult.get("finAccountTransId");
    }
}
