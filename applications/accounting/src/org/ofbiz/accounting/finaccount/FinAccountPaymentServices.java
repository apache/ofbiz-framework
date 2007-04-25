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

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.finaccount.FinAccountHelper;
import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.product.store.ProductStoreWorker;

import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javolution.util.FastMap;

/**
 * FinAccountPaymentServices - Financial account used as payment method
 */
public class FinAccountPaymentServices {

    public static final String module = FinAccountPaymentServices.class.getName();

    // base payment intergration services
    public static Map finAccountPreAuth(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String finAccountCode = (String) context.get("finAccountCode");
        String finAccountPin = (String) context.get("finAccountPin");
        String finAccountId = (String) context.get("finAccountId");
        String orderId = (String) context.get("orderId");
        Double amount = (Double) context.get("processAmount");

        // check for an existing auth trans and cancel it
        GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        if (authTrans != null) {
            Map input = UtilMisc.toMap("userLogin", userLogin, "finAccountAuthId", authTrans.get("referenceNum"));
            try {
                dispatcher.runSync("expireFinAccountAuth", input);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // obtain the order information
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        String productStoreId = orh.getProductStoreId();

        // get the financial account
        GenericValue finAccount;
        if (finAccountId != null) {
            try {
                finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            if (finAccountCode != null) {
                try {
                    finAccount = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to locate financial account from account code");
                }
            } else {
                return ServiceUtil.returnError("Both finAccountId and finAccountCode cannot be null; at least one is required");
            }
        }
        if (finAccount == null) {
            return ServiceUtil.returnError("Invalid financial account; cannot locate account");
        }

        String finAccountTypeId = finAccount.getString("finAccountTypeId");
        finAccountId = finAccount.getString("finAccountId");

        try {
            // fin the store requires a pin number; validate the PIN with the code
            GenericValue finAccountSettings = delegator.findByPrimaryKeyCache("ProductStoreFinActSetting",
                    UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId", finAccountTypeId));
            String allowAuthToNegative = "N";

            if (finAccountSettings != null) {
                allowAuthToNegative = finAccountSettings.getString("allowAuthToNegative");

                // validate the PIN if the store requires it
                if ("Y".equals(finAccountSettings.getString("requirePinCode"))) {
                    if (!FinAccountHelper.validatePin(delegator, finAccountCode, finAccountPin)) {
                        Map result = ServiceUtil.returnSuccess();
                        result.put("authMessage", "Financial account PIN/CODE combination not found");
                        result.put("authResult", Boolean.FALSE);
                        result.put("processAmount", amount);
                        result.put("authFlag", "0");
                        result.put("authCode", "A");
                        result.put("authRefNum", "0");
                        Debug.logError("Unable to auth FinAccount: " + result, module);
                        return result;
                    }
                }
            }
           
            // check for account being frozen
            String isFrozen = finAccount.getString("isFrozen");
            if (isFrozen != null && "Y".equals(isFrozen)) {
                // try to call replenish
                try {
                    dispatcher.runSync("finAccountReplenish", UtilMisc.toMap("finAccountId",
                            finAccountId, "productStoreId", productStoreId, "userLogin", userLogin));
                } catch (GenericServiceException e) {
                    Debug.logWarning(e.getMessage(), module);
                }

                // refresh the finaccount
                finAccount.refresh();
                isFrozen = finAccount.getString("isFrozen");

                if (isFrozen != null && "Y".equals(isFrozen)) {
                    Map result = ServiceUtil.returnSuccess();
                    result.put("authMessage", "Account is currently frozen");
                    result.put("authResult", Boolean.FALSE);
                    result.put("processAmount", amount);
                    result.put("authFlag", "0");
                    result.put("authCode", "A");
                    result.put("authRefNum", "0");
                    Debug.logError("Unable to auth FinAccount: " + result, module);
                    return result;
                }
            }

            // check for expiration date
            if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
                Map result = ServiceUtil.returnSuccess();
                result.put("authMessage", "Account has expired as of " + finAccount.getTimestamp("thruDate"));
                result.put("authResult", Boolean.FALSE);
                result.put("processAmount", amount);
                result.put("authFlag", "0");
                result.put("authCode", "A");
                result.put("authRefNum", "0");
                Debug.logError("Unable to auth FinAccount: " + result, module);
                return result;
            }

            // check the amount to authorize against the available balance of fin account, which includes active authorizations as well as transactions
            BigDecimal availableBalance = finAccount.getBigDecimal("availableBalance");
            if (availableBalance == null) {
                availableBalance = FinAccountHelper.ZERO;
            }
            
            Map result = ServiceUtil.returnSuccess();
            String authMessage = null;
            Boolean processResult;
            String refNum;

            // turn amount into a big decimal, making sure to round and scale it to the same as availableBalance
            BigDecimal amountBd = (new BigDecimal(amount.doubleValue())).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding);

            Debug.log("Allow auth to negative: " + allowAuthToNegative + " :: available: " + availableBalance + " comp: " + FinAccountHelper.ZERO + " = " + availableBalance.compareTo(FinAccountHelper.ZERO) + " :: req: " + amountBd, module);
            // check the available balance to see if we can auth this tx
            if (("Y".equals(allowAuthToNegative) && availableBalance.compareTo(FinAccountHelper.ZERO) > -1)
                    || (availableBalance.compareTo(amountBd) > -1)) {
                Timestamp thruDate;
                
                if (finAccountSettings != null && finAccountSettings.getLong("authValidDays") != null) {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), finAccountSettings.getLong("authValidDays").intValue());
                } else {
                    thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), 30); // default 30 days for an auth
                }

                Map tmpResult = dispatcher.runSync("createFinAccountAuth", UtilMisc.toMap("finAccountId", finAccountId,
                        "amount", amount, "thruDate", thruDate, "userLogin", userLogin));

                if (ServiceUtil.isError(tmpResult)) {
                    return tmpResult;
                } else {
                    refNum = (String) tmpResult.get("finAccountAuthId");
                    processResult = Boolean.TRUE;
                }

                // refresh the account
                finAccount.refresh();
            } else {
                Debug.logError("Attempted to authorize [" + amount + "] against a balance of only [" + availableBalance + "]", module);
                refNum = "0"; // a refNum is always required from authorization
                authMessage = "Insufficient funds";
                processResult = Boolean.FALSE;
            }

            result.put("processAmount", amount);
            result.put("authMessage", authMessage);
            result.put("authResult", processResult);
            result.put("processAmount", amount);
            result.put("authFlag", "1");
            result.put("authCode", "A");            
            result.put("authRefNum", refNum);
            Debug.logInfo("FinAccont Auth: " + result, module);

            return result;
        } catch (GenericEntityException ex) {
            Debug.logError(ex, "Cannot authorize financial account", module);
            return ServiceUtil.returnError("Cannot authorize financial account due to " + ex.getMessage());
        } catch (GenericServiceException ex) {
            Debug.logError(ex, "Cannot authorize gift certificate", module);
            return ServiceUtil.returnError("Cannot authorize financial account due to " + ex.getMessage());
        }
    }

    public static Map finAccountReleaseAuth(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");

        String err = "Unable to expire financial account authorization: ";
        try {

            // expire the related financial authorization transaction
            GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTransaction == null) {
                return ServiceUtil.returnError(err + " Could not find authorization transaction.");
            }
            
            Map input = UtilMisc.toMap("userLogin", userLogin, "finAccountAuthId", authTransaction.get("referenceNum"));
            Map serviceResults = dispatcher.runSync("expireFinAccountAuth", input);

            Map result = ServiceUtil.returnSuccess();
            result.put("releaseRefNum", authTransaction.getString("referenceNum"));
            result.put("releaseAmount", authTransaction.getDouble("amount"));
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

    public static Map finAccountCapture(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        Double amount = (Double) context.get("captureAmount");
        String currency = (String) context.get("currency");

        // get the authorization transaction
        if (authTrans == null){
            authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTrans == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }

        // get the auth record
        String finAccountAuthId = authTrans.getString("referenceNum");
        GenericValue finAccountAuth;
        try {
            finAccountAuth = delegator.findByPrimaryKey("FinAccountAuth", UtilMisc.toMap("finAccountAuthId", finAccountAuthId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Debug.logInfo("Financial account capture [" + finAccountAuth.get("finAccountId") + "] for the amount of $" +
                amount + " Tx #" + finAccountAuth.get("finAccountAuthId"), module);

        // get the financial account
        GenericValue finAccount;
        try {
            finAccount = finAccountAuth.getRelatedOne("FinAccount");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure authorization has not expired
        Timestamp authExpiration = finAccountAuth.getTimestamp("thruDate");
        if ((authExpiration != null) && (authExpiration.before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError("Authorization transaction [" + authTrans.getString("paymentGatewayResponseId") + "] has expired as of " + authExpiration);
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError("Financial account has expired as of " + finAccount.getTimestamp("thruDate"));
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

        // build the withdraw context
        Map withdrawCtx = FastMap.newInstance();
        withdrawCtx.put("finAccountId", finAccountId);
        withdrawCtx.put("productStoreId", productStoreId);
        withdrawCtx.put("currency", currency);
        withdrawCtx.put("partyId", partyId);
        withdrawCtx.put("orderId", orderId);
        withdrawCtx.put("amount", amount);
        withdrawCtx.put("requireBalance", Boolean.FALSE); // for captures; if auth passed, allow
        withdrawCtx.put("userLogin", userLogin);

        // call the withdraw service
        Map withdrawResp;
        try {
            withdrawResp = dispatcher.runSync("finAccountWithdraw", withdrawCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(withdrawResp)) {
            return withdrawResp;
        }

        // cancel the authorization
        Map releaseResult;
        try {
            releaseResult = dispatcher.runSync("expireFinAccountAuth", UtilMisc.toMap("userLogin", userLogin, "finAccountAuthId", finAccountAuthId));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(releaseResult)) {
            return releaseResult;
        }

        // create the capture response
        Map result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) withdrawResp.get("processResult");
        Double withdrawAmount = (Double) withdrawResp.get("amount");
        String referenceNum = (String) withdrawResp.get("referenceNum");
        result.put("captureResult", processResult);
        result.put("captureRefNum", referenceNum);
        result.put("captureCode", "C");
        result.put("captureFlag", "1");
        result.put("captureAmount", withdrawAmount);

        return result;
    }

    public static Map finAccountRefund(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Double amount = (Double) context.get("refundAmount");
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
        }

        // call the deposit service
        Map depositCtx = FastMap.newInstance();
        depositCtx.put("finAccountId", finAccountId);
        depositCtx.put("productStoreId", productStoreId);
        depositCtx.put("isRefund", Boolean.TRUE);
        depositCtx.put("currency", currency);
        depositCtx.put("partyId", partyId);
        depositCtx.put("orderId", orderId);
        depositCtx.put("amount", amount);
        depositCtx.put("userLogin", userLogin);

        Map depositResp;
        try {
            depositResp = dispatcher.runSync("finAccountDeposit", depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            return depositResp;
        }

        // create the refund response
        Map result = ServiceUtil.returnSuccess();
        Boolean processResult = (Boolean) depositResp.get("processResult");
        Double depositAmount = (Double) depositResp.get("amount");
        String referenceNum = (String) depositResp.get("referenceNum");
        result.put("refundResult", processResult);
        result.put("refundRefNum", referenceNum);
        result.put("refundCode", "R");
        result.put("refundFlag", "1");
        result.put("refundAmount", depositAmount);

        return result;
    }

    // base account transaction services
    public static Map finAccountWithdraw(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String orderId = (String) context.get("orderId");
        Boolean requireBalance = (Boolean) context.get("requireBalance");
        Double amount = (Double) context.get("amount");
        if (requireBalance == null) requireBalance = Boolean.TRUE;

        final String WITHDRAWAL = "WITHDRAWAL";

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
        }

        // validate the amount
        if (amount.doubleValue() < 0.00) {
            return ServiceUtil.returnError("Amount should be a positive number.");
        }

        GenericValue finAccount;
        try {
            finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError("Unable to find Financial account for this transaction");
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError("Financial account has expired as of " + finAccount.getTimestamp("thruDate"));
        }

        // check the actual balance (excluding authorized amounts) and create the transaction if it is sufficient
        BigDecimal previousBalance = finAccount.getBigDecimal("actualBalance");
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.ZERO;
        }

        BigDecimal balance;
        String refNum;
        Boolean procResult;
        if (requireBalance.booleanValue() && previousBalance.doubleValue() < amount.doubleValue()) {
            procResult = Boolean.FALSE;
            balance = previousBalance;
            refNum = "N/A";
        } else {
            try {
                refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin, amount,
                        productStoreId, partyId, orderId, orderItemSeqId, currencyUom, WITHDRAWAL, finAccountId);
                finAccount.refresh();
                balance = finAccount.getBigDecimal("actualBalance");
                procResult = Boolean.TRUE;
            } catch (GeneralException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // make sure balance is not null
        if (balance == null) {
            balance = FinAccountHelper.ZERO;
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("previousBalance", new Double(previousBalance.doubleValue()));
        result.put("balance", new Double(balance.doubleValue()));
        result.put("amount", amount);
        result.put("processResult", procResult);
        result.put("referenceNum", refNum);
        return result;
    }

    // base deposit service
    public static Map finAccountDeposit(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String orderId = (String) context.get("orderId");
        Boolean isRefund = (Boolean) context.get("isRefund");
        Double amount = (Double) context.get("amount");

        final String DEPOSIT = isRefund == null || !isRefund.booleanValue() ? "DEPOSIT" : "ADJUSTMENT";

        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }
        String currencyUom = (String) context.get("currency");
        if (UtilValidate.isEmpty(currencyUom)) {
            currencyUom = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
        }

        GenericValue finAccount;
        try {
            finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // verify we have a financial account
        if (finAccount == null) {
            return ServiceUtil.returnError("Unable to find Financial account for this transaction");
        }

        // make sure the fin account itself has not expired
        if ((finAccount.getTimestamp("thruDate") != null) && (finAccount.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp()))) {
            return ServiceUtil.returnError("Financial account has expired as of " + finAccount.getTimestamp("thruDate"));
        }
        Debug.log("Deposit into financial account #" + finAccountId + " [" + amount + "]", module);
        
        // get the previous balance
        BigDecimal previousBalance = finAccount.getBigDecimal("actualBalance");
        if (previousBalance == null) {
            previousBalance = FinAccountHelper.ZERO;    
        }

        // create the transaction
        BigDecimal balance;
        String refNum;
        try {
            refNum = FinAccountPaymentServices.createFinAcctPaymentTransaction(delegator, dispatcher, userLogin, amount,
                    productStoreId, partyId, orderId, orderItemSeqId, currencyUom, DEPOSIT, finAccountId);
            finAccount.refresh();
            balance = finAccount.getBigDecimal("actualBalance");
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make sure balance is not null
        if (balance == null) {
            balance = FinAccountHelper.ZERO;
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("previousBalance", new Double(previousBalance.doubleValue()));
        result.put("balance", new Double(balance.doubleValue()));
        result.put("amount", amount);
        result.put("processResult", Boolean.TRUE);
        result.put("referenceNum", refNum);        
        return result;
    }

    // auto-replenish service (deposit)
    public static Map finAccountReplenish(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountId = (String) context.get("finAccountId");

        // lookup the FinAccount
        GenericValue finAccount;
        try {
            finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccount == null) {
            return ServiceUtil.returnError("Invalid financial account [" + finAccountId + "]");
        }
        String currency = finAccount.getString("currencyUomId");

        // look up the type -- determine auto-replenish is active
        GenericValue finAccountType;
        try {
            finAccountType = finAccount.getRelatedOne("FinAccountType");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        String replenishEnumId = finAccountType.getString("replenishEnumId");
        if (!"FARP_AUTOMATIC".equals(replenishEnumId)) {
            // type does not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // get the product store settings
        GenericValue finAccountSettings;
        try {
            finAccountSettings = delegator.findByPrimaryKeyCache("ProductStoreFinActSetting",
                    UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId",
                            finAccount.getString("finAccountTypeId")));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (finAccountSettings == null) {
            // no settings; don't replenish
            return ServiceUtil.returnSuccess();
        }

        Double replThres = finAccountSettings.getDouble("replenishThreshold");
        if (replThres == null) {
            return ServiceUtil.returnSuccess();
        }
        BigDecimal replenishThreshold = new BigDecimal(replThres.doubleValue());

        BigDecimal replenishLevel = finAccount.getBigDecimal("replenishLevel");
        if (replenishLevel == null || replenishLevel.compareTo(FinAccountHelper.ZERO) == 0) {
            // no replenish level set; this account goes not support auto-replenish
            return ServiceUtil.returnSuccess();
        }

        // get the current balance
        BigDecimal balance = finAccount.getBigDecimal("actualBalance");

        // see if we are within the threshold for replenishment
        if (balance.compareTo(replenishThreshold) > -1) {
            // not ready
            return ServiceUtil.returnSuccess();        
        }

        // the deposit is level - balance (500 - (-10) = 510 || 500 - (10) = 490)
        BigDecimal depositAmount = replenishLevel.subtract(balance);

        // get the owner party
        String ownerPartyId = finAccount.getString("ownerPartyId");
        if (ownerPartyId == null) {
            // no owner cannot replenish; (not fatal, just not supported by this account)
            Debug.logWarning("No owner attached to financial account [" + finAccountId + "] cannot auto-replenish", module);
            return ServiceUtil.returnSuccess();
        }

        // get the payment method to use to replenish
        String paymentMethodId = finAccount.getString("replenishPaymentId");
        if (paymentMethodId == null) {
            Debug.logWarning("No payment method attached to financial account [" + finAccountId + "] cannot auto-replenish", module);
            return ServiceUtil.returnSuccess();
        }

        GenericValue paymentMethod;
        try {
            paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (paymentMethod == null) {
            // no payment methods on file; cannot replenish
            Debug.logWarning("No payment method found for ID [" + paymentMethodId + "] for party [" + ownerPartyId + "] cannot auto-replenish", module);
            return ServiceUtil.returnSuccess();
        }

        // clear out the frozen flag
        finAccount.set("isFrozen", "N");
        try {
            finAccount.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // hit the payment method for the amount to replenish
        Map orderItemMap = UtilMisc.toMap("Auto-Replenishment FA #" + finAccountId, new Double(depositAmount.doubleValue()));
        Map replOrderCtx = FastMap.newInstance();
        replOrderCtx.put("productStoreId", productStoreId);
        replOrderCtx.put("paymentMethodId", paymentMethod.getString("paymentMethodId"));
        replOrderCtx.put("currency", currency);
        replOrderCtx.put("partyId", ownerPartyId);
        replOrderCtx.put("itemMap", orderItemMap);
        replOrderCtx.put("userLogin", userLogin);
        Map replResp;
        try {
            replResp = dispatcher.runSync("createSimpleNonProductSalesOrder", replOrderCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(replResp)) {
            return replResp;
        }
        String orderId = (String) replResp.get("orderId");

        // create the deposit
        Map depositCtx = FastMap.newInstance();
        depositCtx.put("productStoreId", productStoreId);
        depositCtx.put("finAccountId", finAccountId);
        depositCtx.put("currency", currency);
        depositCtx.put("partyId", ownerPartyId);
        depositCtx.put("orderId", orderId);
        depositCtx.put("orderItemSeqId", "00001"); // always one item on a replish order
        depositCtx.put("amount",  new Double(depositAmount.doubleValue()));
        depositCtx.put("userLogin", userLogin);
        Map depositResp;
        try {
            depositResp = dispatcher.runSync("finAccountDeposit", depositCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(depositResp)) {
            return depositResp;
        }

        return ServiceUtil.returnSuccess();
    }
    
    private static String createFinAcctPaymentTransaction(GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, Double amount,
            String productStoreId, String partyId, String orderId, String orderItemSeqId, String currencyUom, String txType, String finAccountId) throws GeneralException {

        final String coParty = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
        final String paymentMethodType = "FIN_ACCOUNT";

        if (UtilValidate.isEmpty(partyId)) {
            partyId = "_NA_";
        }

        String paymentType;
        String partyIdFrom;
        String partyIdTo;
        Double paymentAmount;

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
            if (amount.doubleValue() < 0) {
                paymentType = "DISBURSEMENT";
                partyIdFrom = coParty;
                partyIdTo = partyId;
                paymentAmount = new Double(amount.doubleValue() * -1); // must be positive
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
        Map paymentCtx = UtilMisc.toMap("paymentTypeId", paymentType);
        paymentCtx.put("paymentMethodTypeId", paymentMethodType);
        paymentCtx.put("partyIdTo", partyIdTo);
        paymentCtx.put("partyIdFrom", partyIdFrom);
        paymentCtx.put("statusId", "PMNT_RECEIVED");
        paymentCtx.put("currencyUomId", currencyUom);
        paymentCtx.put("amount", paymentAmount);
        paymentCtx.put("userLogin", userLogin);
        paymentCtx.put("paymentRefNum", Long.toString(UtilDateTime.nowTimestamp().getTime()));

        String paymentId;
        Map payResult;
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
        Map transCtx = UtilMisc.toMap("finAccountTransTypeId", txType);
        transCtx.put("finAccountId", finAccountId);
        transCtx.put("partyId", partyId);
        transCtx.put("orderId", orderId);
        transCtx.put("orderItemSeqId", orderItemSeqId);
        transCtx.put("amount", amount);
        transCtx.put("userLogin", userLogin);
        transCtx.put("paymentId", paymentId);

        Map transResult;
        String txId;
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
}
