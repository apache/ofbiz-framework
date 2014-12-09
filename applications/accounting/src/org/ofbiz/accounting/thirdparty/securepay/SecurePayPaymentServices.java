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
package org.ofbiz.accounting.thirdparty.securepay;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import securepay.jxa.api.Payment;
import securepay.jxa.api.Txn;

public class SecurePayPaymentServices {

    public static final String module = SecurePayPaymentServices.class.getName();
    public final static String resource = "AccountingUiLabels";

    public static Map<String, Object> doAuth(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        // generate the request/properties
        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayNotProperlyConfigurated", locale));
        }

        String merchantId = props.getProperty("merchantID");
        String serverURL = props.getProperty("serverurl");
        String processtimeout = props.getProperty("processtimeout");
        String pwd = props.getProperty("pwd");
        String enableamountround = props.getProperty("enableamountround");
        String currency = (String) context.get("currency");
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (enableamountround.equals("Y")) {
            newAmount = new BigDecimal(processAmount.setScale(0, BigDecimal.ROUND_HALF_UP)+".00");
        } else {
            newAmount = processAmount;
        }

        if (currency.equals("JPY")) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        GenericValue creditCard = (GenericValue) context.get("creditCard");
        String expiryDate = (String) creditCard.get("expireDate");
        String cardSecurityCode = (String) context.get("cardSecurityCode");
        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);

        Txn txn = payment.addTxn(10, orderId);
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        if (UtilValidate.isNotEmpty(currency)) {
            txn.setCurrencyCode(currency);
        } else {
            txn.setCurrencyCode("AUD");
        }

        txn.setCardNumber((String) creditCard.get("cardNumber"));
        txn.setExpiryDate(expiryDate.substring(0, 3) + expiryDate.substring(5));
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            txn.setCVV(cardSecurityCode);
        }
        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayPaymentWasNotSent", locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false){
                    result.put("authResult", new Boolean(false));
                    result.put("authRefNum", "N/A");
                    result.put("processAmount", BigDecimal.ZERO);
                } else {
                    result.put("authRefNum", resp.getTxnId());
                    result.put("authResult", new Boolean(true));
                    result.put("processAmount", processAmount);
                }
                result.put("authCode", resp.getResponseCode());
                result.put("authMessage", resp.getResponseText());
            } 
        }
        return result;
    }

    public static Map<String, Object> ccReAuth(DispatchContext dctx, Map<String, ? extends Object> context) {
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> doCapture(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayNotProperlyConfigurated", locale));
        }

        String merchantId = props.getProperty("merchantID");
        String serverURL = props.getProperty("serverurl");
        String processtimeout = props.getProperty("processtimeout");
        String pwd = props.getProperty("pwd");
        String enableamountround = props.getProperty("enableamountround");
        String currency = authTransaction.getString("currencyUomId");
        BigDecimal captureAmount = (BigDecimal) context.get("captureAmount");
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (enableamountround.equals("Y")) {
            newAmount = new BigDecimal(captureAmount.setScale(0, BigDecimal.ROUND_HALF_UP)+".00");
        } else {
            newAmount = captureAmount;
        }

        if (currency.equals("JPY")) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(11, (String) orderPaymentPreference.get("orderId"));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setPreauthId(authTransaction.getString("referenceNum"));

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayPaymentWasNotSent", locale));
        } else {
            if (payment.getCount() == 1){
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false){
                    result.put("captureResult", false);
                    result.put("captureRefNum", authTransaction.getString("referenceNum"));
                    result.put("captureAmount", BigDecimal.ZERO);
                } else {
                    result.put("captureResult", true);
                    result.put("captureAmount", captureAmount);
                    result.put("captureRefNum", resp.getTxnId());
                }
                result.put("captureFlag", "C");
                result.put("captureCode", resp.getResponseCode());
                result.put("captureMessage", resp.getResponseText());
            }
        }
        return result;
    }

    public static Map<String, Object> doVoid(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayNotProperlyConfigurated", locale));
        }

        String merchantId = props.getProperty("merchantID");
        String serverURL = props.getProperty("serverurl");
        String processtimeout = props.getProperty("processtimeout");
        String pwd = props.getProperty("pwd");
        String enableamountround = props.getProperty("enableamountround");
        String currency = authTransaction.getString("currencyUomId");
        BigDecimal releaseAmount = (BigDecimal) context.get("releaseAmount");
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (enableamountround.equals("Y")) {
            newAmount = new BigDecimal(releaseAmount.setScale(0, BigDecimal.ROUND_HALF_UP)+".00");
        } else {
            newAmount = releaseAmount;
        }

        if (currency.equals("JPY")) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(6, (String) orderPaymentPreference.get("orderId"));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setTxnId(authTransaction.getString("referenceNum"));

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayPaymentWasNotSent", locale));
        } else {
            if (payment.getCount() == 1){
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false){
                    result.put("releaseResult", false);
                    result.put("releaseRefNum", authTransaction.getString("referenceNum"));
                    result.put("releaseAmount", BigDecimal.ZERO);
                } else {
                    result.put("releaseResult", true);
                    result.put("releaseAmount", releaseAmount);
                    result.put("releaseRefNum", resp.getTxnId());
                }
                result.put("releaseFlag", "U");
                result.put("releaseCode", resp.getResponseCode());
                result.put("releaseMessage", resp.getResponseText());
            }
        }
        return result;
    }

    public static Map<String, Object> doRefund(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }

        String referenceNum = null;
        try {
            GenericValue paymentGatewayResponse = EntityQuery.use(delegator).from("PaymentGatewayResponse")
                    .where("orderPaymentPreferenceId", authTransaction.get("orderPaymentPreferenceId"),
                            "paymentServiceTypeEnumId", "PRDS_PAY_CAPTURE")
                    .queryFirst();
            referenceNum = paymentGatewayResponse != null ? paymentGatewayResponse.get("referenceNum") : authTransaction.getString("referenceNum");
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }

        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayNotProperlyConfigurated", locale));
        }

        String merchantId = props.getProperty("merchantID");
        String serverURL = props.getProperty("serverurl");
        String processtimeout = props.getProperty("processtimeout");
        String pwd = props.getProperty("pwd");
        String enableamountround = props.getProperty("enableamountround");
        String currency = authTransaction.getString("currencyUomId");
        BigDecimal refundAmount = (BigDecimal) context.get("refundAmount");
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;

        if (enableamountround.equals("Y")) {
            newAmount = new BigDecimal(refundAmount.setScale(0, BigDecimal.ROUND_HALF_UP)+".00");
        } else {
            newAmount = refundAmount;
        }

        int amont;
        if (currency.equals("JPY")) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);
        Txn txn = payment.addTxn(4, (String) orderPaymentPreference.get("orderId"));
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setTxnId(referenceNum);

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayPaymentWasNotSent", locale));
        } else {
            if (payment.getCount() == 1){
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false){
                    result.put("refundResult", false);
                    result.put("refundRefNum", authTransaction.getString("referenceNum"));
                    result.put("refundAmount", BigDecimal.ZERO);
                } else {
                    result.put("refundResult", true);
                    result.put("refundAmount", refundAmount);
                    result.put("refundRefNum", resp.getTxnId());
                }
                result.put("refundCode", resp.getResponseCode());
                result.put("refundMessage", resp.getResponseText());
                result.put("refundFlag", "R");
            }
        }
        return result;
    }

    public static Map<String, Object> doCredit(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        // generate the request/properties
        Properties props = buildScProperties(context, delegator);
        if (props == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayNotProperlyConfigurated", locale));
        }

        String merchantId = props.getProperty("merchantID");
        String serverURL = props.getProperty("serverurl");
        String processtimeout = props.getProperty("processtimeout");
        String pwd = props.getProperty("pwd");
        String enableamountround = props.getProperty("enableamountround");
        String referenceCode = (String) context.get("referenceCode");
        String currency = (String) context.get("currency");
        String cardSecurityCode = (String) context.get("cardSecurityCode");
        BigDecimal creditAmount = (BigDecimal) context.get("creditAmount");
        BigDecimal multiplyAmount = new BigDecimal(100);
        BigDecimal newAmount = null;
        int amont;

        if (enableamountround.equals("Y")) {
            newAmount = new BigDecimal(creditAmount.setScale(0, BigDecimal.ROUND_HALF_UP)+".00");
        } else {
            newAmount = creditAmount;
        }

        if (currency.equals("JPY")) {
            amont = newAmount.intValue();
        } else {
            amont = newAmount.multiply(multiplyAmount).intValue();
        }

        GenericValue creditCard = (GenericValue) context.get("creditCard");
        Payment payment = new Payment();
        payment.setServerURL(serverURL);
        payment.setProcessTimeout(Integer.valueOf(processtimeout));
        payment.setMerchantId(merchantId);

        Txn txn = payment.addTxn(0, referenceCode);
        txn.setTxnSource(8);
        txn.setAmount(Integer.toString(amont));
        txn.setCardNumber((String) creditCard.get("cardNumber"));
        String expiryDate = (String) creditCard.get("expireDate");
        txn.setExpiryDate(expiryDate.substring(0, 3) + expiryDate.substring(5));
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            txn.setCVV(cardSecurityCode);
        }

        // Send payment to SecurePay for processing
        boolean processed = payment.process(pwd);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(processed)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingSecurityPayPaymentWasNotSent", locale));
        } else {
            if (payment.getCount() == 1) {
                Txn resp = payment.getTxn(0);
                boolean approved = resp.getApproved();
                if (approved == false){
                    result.put("creditResult", false);
                    result.put("creditRefNum", "N/A");
                    result.put("creditAmount", BigDecimal.ZERO);
                } else {
                    result.put("creditResult", true);
                    result.put("creditAmount", creditAmount);
                    result.put("creditRefNum", resp.getTxnId());
                }
                result.put("creditCode", resp.getResponseCode());
                result.put("creditMessage", resp.getResponseText());
            }
        }
        return result;
    }

    private static Properties buildScProperties(Map<String, ? extends Object> context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        String merchantId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantId", configString, "payment.securepay.merchantID", null);
        String pwd = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "pwd", configString, "payment.securepay.pwd", null);
        String serverURL = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "serverURL", configString, "payment.securepay.serverurl", null);
        String processTimeout = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "processTimeout", configString, "payment.securepay.processtimeout", null);
        String enableAmountRound = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableAmountRound", configString, "payment.securepay.enableamountround", null);

        Properties props = new Properties();
        props.put("merchantID", merchantId);
        props.put("pwd", pwd);
        props.put("serverurl", serverURL);
        props.put("processtimeout", processTimeout);
        props.put("enableamountround", enableAmountRound);
        return props;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue securePay = EntityQuery.use(delegator).from("PaymentGatewaySecurePay").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
                if (UtilValidate.isNotEmpty(securePay)) {
                    Object securePayField = securePay.get(paymentGatewayConfigParameterName);
                    if (securePayField != null) {
                        returnValue = securePayField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, parameterName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,String resource, String parameterName, String defaultValue) {
        String returnValue = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName, resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}