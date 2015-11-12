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
package org.ofbiz.accounting.thirdparty.valuelink;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * ValueLinkServices - Integration with ValueLink Gift Cards
 */
public class ValueLinkServices {

    public static final String module = ValueLinkServices.class.getName();
    public final static String resource = "AccountingUiLabels";
    public final static String resourceError = "AccountingErrorUiLabels";
    public final static String resourceOrder = "OrderUiLabels";

    // generate/display new public/private/kek keys
    public static Map<String, Object> createKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        Boolean kekOnly = context.get("kekOnly") != null ? (Boolean) context.get("kekOnly") : Boolean.FALSE;
        String kekTest = (String) context.get("kekTest");
        Debug.logInfo("KEK Only : " + kekOnly.booleanValue(), module);

        StringBuffer buf = vl.outputKeyCreation(kekOnly.booleanValue(), kekTest);
        String output = buf.toString();
        Debug.logInfo(":: Key Generation Output ::\n\n" + output, module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("output", output);
        return result;
    }

    // test the KEK encryption
    public static Map<String, Object> testKekEncryption(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        //GenericValue userLogin = (GenericValue) context.get("userLogin");
        Properties props = getProperties(context);

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        String testString = (String) context.get("kekTest");
        Integer mode = (Integer) context.get("mode");
        byte[] testBytes = StringUtil.fromHexString(testString);

        // place holder
        byte[] testEncryption = null;
        String desc = "";

        if (mode.intValue() == 1) {
            // encrypt the test bytes
            testEncryption = vl.encryptViaKek(testBytes);
            desc = "Encrypted";
        } else {
            // decrypt the test bytes
            testEncryption = vl.decryptViaKek(testBytes);
            desc = "Decrypted";
        }

        // setup the output
        StringBuilder buf = new StringBuilder();
        buf.append("======== Begin Test String (").append(testString.length()).append(") ========\n");
        buf.append(testString).append("\n");
        buf.append("======== End Test String ========\n\n");

        buf.append("======== Begin Test Bytes (").append(testBytes.length).append(") ========\n");
        buf.append(StringUtil.toHexString(testBytes)).append("\n");
        buf.append("======== End Test Bytes ========\n\n");

        buf.append("======== Begin Test Bytes ").append(desc).append(" (").append(testEncryption.length).append(") ========\n");
        buf.append(StringUtil.toHexString(testEncryption)).append("\n");
        buf.append("======== End Test Bytes ").append(desc).append(" ========\n\n");

        String output = buf.toString();
        Debug.logInfo(":: KEK Test Output ::\n\n" + output, module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("output", output);
        return result;
    }

    // change working key service
    public static Map<String, Object> assignWorkingKey(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Properties props = getProperties(context);
        Locale locale = (Locale) context.get("locale");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        vl.reload();

        // place holder
        byte[] mwk = null;

        // see if we passed in the DES hex string
        String desHexString = (String) context.get("desHexString");
        if (UtilValidate.isEmpty(desHexString)) {
            mwk = vl.generateMwk();
        } else {
            mwk = vl.generateMwk(StringUtil.fromHexString(desHexString));
        }

        // encrypt the mwk
        String mwkHex = StringUtil.toHexString(vl.encryptViaKek(mwk));

        // build the request
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Encrypt");
        request.put("EncryptKey", mwkHex);
        request.put("EncryptID", Long.valueOf(vl.getWorkingKeyIndex().longValue() + 1));

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkCannotUpdateWorkingKey", locale));
        }
        Debug.logInfo("Response : " + response, module);

        // on success update the database / reload the cached api
        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            if (responseCode.equals("00")) {
                GenericValue vlKeys = GenericValue.create(vl.getGenericValue());
                vlKeys.set("lastWorkingKey", vlKeys.get("workingKey"));
                vlKeys.set("workingKey", StringUtil.toHexString(mwk));
                vlKeys.set("workingKeyIndex", request.get("EncryptID"));
                vlKeys.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                vlKeys.set("lastModifiedByUserLogin", userLogin != null ? userLogin.get("userLoginId") : null);
                try {
                    vlKeys.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to store updated keys; the keys were changed with ValueLink : " + vlKeys, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingValueLinkCannotStoreWorkingKey", locale));
                }
                vl.reload();
                return ServiceUtil.returnSuccess();
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingValueLinkTransactionFailed", 
                        UtilMisc.toMap("responseCode", responseCode), locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> activate(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String vlPromoCode = (String) context.get("vlPromoCode");
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        // override interface for void/rollback
        String iFace = (String) context.get("Interface");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Activate");
        if (UtilValidate.isNotEmpty(vlPromoCode)) {
            request.put("PromoCode", vlPromoCode);
        }
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        if (UtilValidate.isNotEmpty(cardNumber)) {
            request.put("CardNo", cardNumber);
        }
        if (UtilValidate.isNotEmpty(pin)) {
            request.put("PIN", vl.encryptPin(pin));
        }

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToActivateGiftCard", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
                result.put("pin", vl.decryptPin((String) response.get("pin")));
            } else {
                result.put("processResult", Boolean.FALSE);
                result.put("pin", response.get("PIN"));
            }
            result.put("responseCode", responseCode);
            result.put("authCode", response.get("authcode"));
            result.put("cardNumber", response.get("cardno"));
            result.put("amount", vl.getAmount((String) response.get("currbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Activate Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> linkPhysicalCard(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String virtualCard = (String) context.get("virtualCard");
        String virtualPin = (String) context.get("virtualPin");
        String physicalCard = (String) context.get("physicalCard");
        String physicalPin = (String) context.get("physicalPin");
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Link");
        request.put("VCardNo", virtualCard);
        request.put("VPIN", vl.encryptPin(virtualPin));
        request.put("PCardNo", physicalCard);
        request.put("PPIN", vl.encryptPin(physicalPin));

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToLinkGiftCard", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkGiftCardActivated", locale));
            
            if (responseCode.equals("00")) {

                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("authCode", response.get("authcode"));
            result.put("amount", vl.getAmount((String) response.get("newbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Link Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> disablePin(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Disable");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToDisablePin", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkPinDisabled", locale));
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("balance", vl.getAmount((String) response.get("currbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Disable Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> redeem(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        // override interface for void/rollback
        String iFace = (String) context.get("Interface");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Redeem");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToRedeemGiftCard", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("authCode", response.get("authcode"));
            result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
            result.put("amount", vl.getAmount((String) response.get("newbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("cashBack", vl.getAmount((String) response.get("cashback")));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Redeem Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> reload(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        // override interface for void/rollback
        String iFace = (String) context.get("Interface");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Reload");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToReloadGiftCard", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("authCode", response.get("authcode"));
            result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
            result.put("amount", vl.getAmount((String) response.get("newbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Reload Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> balanceInquire(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "Balance");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToCallBalanceInquiry", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("balance", vl.getAmount((String) response.get("currbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Balance Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> transactionHistory(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", "History");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToCallHistoryInquiry", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("balance", vl.getAmount((String) response.get("currbal")));
            result.put("history", response.get("history"));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("History Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> refund(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Properties props = getProperties(context);
        String cardNumber = (String) context.get("cardNumber");
        String pin = (String) context.get("pin");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        // override interface for void/rollback
        String iFace = (String) context.get("Interface");

        // get an api instance
        ValueLinkApi vl = ValueLinkApi.getInstance(delegator, props);
        Map<String, Object> request = vl.getInitialRequestMap(context);
        request.put("Interface", iFace != null ? iFace : "Refund");
        request.put("CardNo", cardNumber);
        request.put("PIN", vl.encryptPin(pin));
        request.put("Amount", vl.getAmount(amount));
        request.put("LocalCurr", vl.getCurrency(currency));

        // user defined field #1
        if (UtilValidate.isNotEmpty(orderId)) {
            request.put("User1", orderId);
        }

        // user defined field #2
        if (UtilValidate.isNotEmpty(partyId)) {
            request.put("User2", partyId);
        }

        // set the timeout reversal
        setTimeoutReversal(dctx, context, request);

        // send the request
        Map<String, Object> response = null;
        try {
            response = vl.send(request);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem communicating with VL");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToRefundGiftCard", locale));
        }

        if (response != null) {
            String responseCode = (String) response.get("responsecode");
            Map<String, Object> result = ServiceUtil.returnSuccess();
            if (responseCode.equals("00")) {
                result.put("processResult", Boolean.TRUE);
            } else {
                result.put("processResult", Boolean.FALSE);
            }
            result.put("responseCode", responseCode);
            result.put("authCode", response.get("authcode"));
            result.put("previousAmount", vl.getAmount((String) response.get("prevbal")));
            result.put("amount", vl.getAmount((String) response.get("newbal")));
            result.put("expireDate", response.get("expiredate"));
            result.put("cardClass", response.get("cardclass"));
            result.put("referenceNum", response.get("traceno"));
            Debug.logInfo("Refund Result : " + result, module);
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkReceivedEmptyResponse", locale));
        }
    }

    public static Map<String, Object> voidRedeem(DispatchContext dctx, Map<String, Object> context) {
        context.put("Interface", "Redeem/Void");
        return redeem(dctx, context);
    }

    public static Map<String, Object> voidRefund(DispatchContext dctx, Map<String, Object> context) {
        context.put("Interface", "Refund/Void");
        return refund(dctx, context);
    }

    public static Map<String, Object> voidReload(DispatchContext dctx, Map<String, Object> context) {
        context.put("Interface", "Reload/Void");
        return reload(dctx, context);
    }

    public static Map<String, Object> voidActivate(DispatchContext dctx, Map<String, Object> context) {
        context.put("Interface", "Activate/Void");
        return activate(dctx, context);
    }

    public static Map<String, Object> timeOutReversal(DispatchContext dctx, Map<String, Object> context) {
        String vlInterface = (String) context.get("Interface");
        Locale locale = (Locale) context.get("locale");
        Debug.logInfo("704 Interface : " + vlInterface, module);
        if (vlInterface != null) {
            if (vlInterface.startsWith("Activate")) {
                if (vlInterface.equals("Activate/Rollback")) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingValueLinkThisTransactionIsNotSupported", locale));
                }
                return activate(dctx, context);
            } else if (vlInterface.startsWith("Redeem")) {
                return redeem(dctx, context);
            } else if (vlInterface.startsWith("Reload")) {
                return reload(dctx, context);
            } else if (vlInterface.startsWith("Refund")) {
                return refund(dctx, context);
            }
        }

        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                "AccountingValueLinkTransactionNotValid", locale));
    }

    // 0704 Timeout Reversal (Supports - Activate/Void, Redeem, Redeem/Void, Reload, Reload/Void, Refund, Refund/Void)
    private static void setTimeoutReversal(DispatchContext dctx, Map<String, Object> ctx, Map<String, Object> request) {
        String vlInterface = (String) request.get("Interface");
        // clone the context
        Map<String, Object> context = new HashMap<String, Object>();
        context.putAll(ctx);

        // append the rollback interface
        if (!vlInterface.endsWith("Rollback")) {
            context.put("Interface", vlInterface + "/Rollback");
        } else {
            // no need to re-run ourself we are persisted
            return;
        }

        // set the old tx time and number
        context.put("MerchTime", request.get("MerchTime"));
        context.put("TermTxnNo", request.get("TermTxnNo"));

        // Activate/Rollback is not supported by valuelink
        if (!vlInterface.equals("Activate")) {
            // create the listener
            Debug.logInfo("Set 704 context : " + context, module);
            try {
                dctx.getDispatcher().addRollbackService("vlTimeOutReversal", context, false);
                //dctx.getDispatcher().addCommitService("vlTimeOutReversal", context, false);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to setup 0704 Timeout Reversal", module);
            }
        }
    }

    private static Properties getProperties(Map<String, Object> context) {
        String paymentProperties = (String) context.get("paymentConfig");
        if (paymentProperties == null) {
            paymentProperties = "payment.properties";
        }
        return UtilProperties.getProperties(paymentProperties);
    }


    // payment processing wrappers (process/release/refund)

    public static Map<String, Object> giftCardProcessor(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        GenericValue giftCard = (GenericValue) context.get("giftCard");
        GenericValue party = (GenericValue) context.get("billToParty");
        String paymentConfig = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        String orderId = (String) context.get("orderId");
        BigDecimal amount = (BigDecimal) context.get("processAmount");        

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<String, Object>();
        redeemCtx.put("userLogin", userLogin);
        redeemCtx.put("paymentConfig", paymentConfig);
        redeemCtx.put("cardNumber", giftCard.get("cardNumber"));
        redeemCtx.put("pin", giftCard.get("pinNumber"));
        redeemCtx.put("currency", currency);
        redeemCtx.put("orderId", orderId);
        redeemCtx.put("partyId", party.get("partyId"));
        redeemCtx.put("amount", amount);

        // invoke the redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("redeemGiftCard", redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the redeem service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToRedeemGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            // confirm the amount redeemed; since VL does not error in insufficient funds
            if (processResult.booleanValue()) {
                BigDecimal previous = (BigDecimal) redeemResult.get("previousAmount");
                if (previous == null) previous = BigDecimal.ZERO;
                BigDecimal current = (BigDecimal) redeemResult.get("amount");
                if (current == null) current = BigDecimal.ZERO;
                BigDecimal redeemed = previous.subtract(current);
                Debug.logInfo("Redeemed (" + amount + "): " + redeemed + " / " + previous + " : " + current, module);
                if (redeemed.compareTo(amount) < 0) {
                    // we didn't redeem enough void the transaction and return false
                    Map<String, Object> voidResult = null;
                    try {
                        voidResult = dispatcher.runSync("voidRedeemGiftCard", redeemCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }
                    if (ServiceUtil.isError(voidResult)) {
                        return voidResult;
                    }
                    processResult = Boolean.FALSE;
                    amount = redeemed;
                    result.put("authMessage", "Gift card did not contain enough funds");
                }
            }
            result.put("processAmount", amount);
            result.put("authFlag", redeemResult.get("responseCode"));
            result.put("authResult", processResult);
            result.put("captureResult", processResult);
            result.put("authCode", redeemResult.get("authCode"));
            result.put("captureCode", redeemResult.get("authCode"));
            result.put("authRefNum", redeemResult.get("referenceNum"));
            result.put("captureRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    public static Map<String, Object> giftCardRelease(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String paymentConfig = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        BigDecimal amount = (BigDecimal) context.get("releaseAmount");

        // get the orderId for tracking
        String orderId = paymentPref.getString("orderId");

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne("GiftCard", false);
        } catch (GenericEntityException e) {
            Debug.logError("Unable to get GiftCard from OrderPaymentPreference", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotLocateItFromOrderPaymentPreference", locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToReleaseGiftCard", locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> redeemCtx = new HashMap<String, Object>();
        redeemCtx.put("userLogin", userLogin);
        redeemCtx.put("paymentConfig", paymentConfig);
        redeemCtx.put("cardNumber", giftCard.get("cardNumber"));
        redeemCtx.put("pin", giftCard.get("pinNumber"));
        redeemCtx.put("currency", currency);
        redeemCtx.put("orderId", orderId);
        redeemCtx.put("amount", amount);

        // invoke the void redeem service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("voidRedeemGiftCard", redeemCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the redeem service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToRedeemGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            result.put("releaseAmount", redeemResult.get("amount"));
            result.put("releaseFlag", redeemResult.get("responseCode"));
            result.put("releaseResult", processResult);
            result.put("releaseCode", redeemResult.get("authCode"));
            result.put("releaseRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    public static Map<String, Object> giftCardRefund(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String paymentConfig = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");

        // get the orderId for tracking
        String orderId = paymentPref.getString("orderId");

        // get the GiftCard VO
        GenericValue giftCard = null;
        try {
            giftCard = paymentPref.getRelatedOne("GiftCard", false);
        } catch (GenericEntityException e) {
            Debug.logError("Unable to get GiftCard from OrderPaymentPreference", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotLocateItFromOrderPaymentPreference", locale));
        }

        if (giftCard == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToReleaseGiftCard", locale));
        }

        // make sure we have a currency
        if (currency == null) {
            currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
        }

        Map<String, Object> refundCtx = new HashMap<String, Object>();
        refundCtx.put("userLogin", userLogin);
        refundCtx.put("paymentConfig", paymentConfig);
        refundCtx.put("cardNumber", giftCard.get("cardNumber"));
        refundCtx.put("pin", giftCard.get("pinNumber"));
        refundCtx.put("currency", currency);
        refundCtx.put("orderId", orderId);
        refundCtx.put("amount", amount);

        // invoke the refund service
        Map<String, Object> redeemResult = null;
        try {
            redeemResult = dispatcher.runSync("refundGiftCard", refundCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the refund service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToRefundGiftCardFailure", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        if (redeemResult != null) {
            Boolean processResult = (Boolean) redeemResult.get("processResult");
            result.put("refundAmount", redeemResult.get("amount"));
            result.put("refundFlag", redeemResult.get("responseCode"));
            result.put("refundResult", processResult);
            result.put("refundCode", redeemResult.get("authCode"));
            result.put("refundRefNum", redeemResult.get("referenceNum"));
        }

        return result;
    }

    // item fulfillment wrappers (purchase/reload)

    public static Map<String, Object> giftCardPurchase(DispatchContext dctx, Map<String, Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
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
                    "AccountingGiftCerticateNumberCannotProcess", locale));
        }

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "GIFT_CARD", null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString("paymentPropertiesPath");
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingFinAccountSetting", 
                    UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId", "GIFT_CARD"), locale));
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
            Debug.logError("Unable to get Product from OrderItem", module);
        }
        if (product == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingGiftCerticateNumberCannotFulfill", locale));
        }

        // get the productFeature type TYPE (VL promo code)
        GenericValue typeFeature = null;
        try {
            typeFeature = EntityQuery.use(delegator)
                    .from("ProductFeatureAndAppl")
                    .where("productId", product.get("productId"), 
                            "productFeatureTypeId", "TYPE")
                    .orderBy("-fromDate").filterByDate().queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkUnableToGetFeatureType", locale));
        }
        if (typeFeature == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkFeatureTypeRequested", 
                    UtilMisc.toMap("productId", product.get("productId")), locale));
        }

        // get the VL promo code
        String promoCode = typeFeature.getString("idCode");
        if (UtilValidate.isEmpty(promoCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingValueLinkPromoCodeInvalid", locale));
        }

        // survey information
        String surveyId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.surveyId", delegator);

        // get the survey response
        GenericValue surveyResponse = null;
        try {
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId,
                            "orderItemSeqId", orderItem.get("orderItemSeqId"),
                            "surveyId", surveyId)
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
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

        // get the send to email address - key defined in properties file
        String sendToKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.survey.sendToEmail", delegator);
        String sendToEmail = (String) answerMap.get(sendToKey);
        // get the copyMe flag and set the order email address
        String orderEmails = orh.getOrderEmailString();
        String copyMeField = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.purchase.survey.copyMe", delegator);
        String copyMeResp = copyMeField != null ? (String) answerMap.get(copyMeField) : null;
        boolean copyMe = (UtilValidate.isNotEmpty(copyMeField)
                && UtilValidate.isNotEmpty(copyMeResp) && "true".equalsIgnoreCase(copyMeResp)) ? true : false;

        int qtyLoop = quantity.intValue();
        for (int i = 0; i < qtyLoop; i++) {
            // activate a gift card
            Map<String, Object> activateCtx = new HashMap<String, Object>();
            activateCtx.put("paymentConfig", paymentConfig);
            activateCtx.put("vlPromoCode", promoCode);
            activateCtx.put("currency", currency);
            activateCtx.put("partyId", partyId);
            activateCtx.put("orderId", orderId);
            activateCtx.put("amount", amount);
            activateCtx.put("userLogin", userLogin);

            boolean failure = false;
            Map<String, Object> activateResult = null;
            try {
                activateResult = dispatcher.runSync("activateGiftCard", activateCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to activate gift card(s)", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "AccountingValueLinkUnableToActivateGiftCard", locale));
            }

            Boolean processResult = (Boolean) activateResult.get("processResult");
            if (activateResult == null || activateResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult.booleanValue()) {
                failure = true;
            }

            if (!failure) {
                // set the void on rollback
                try {
                    dispatcher.addRollbackService("voidActivateGiftCard", activateCtx, false);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Unable to setup Activate/Void on error", module);
                }
            }

            // create the fulfillment record
            Map<String, Object> vlFulFill = new HashMap<String, Object>();
            vlFulFill.put("typeEnumId", "GC_ACTIVATE");
            vlFulFill.put("merchantId", EntityUtilProperties.getPropertyValue(paymentConfig, "payment.valuelink.merchantId", delegator));
            vlFulFill.put("partyId", partyId);
            vlFulFill.put("orderId", orderId);
            vlFulFill.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
            vlFulFill.put("surveyResponseId", surveyResponse.get("surveyResponseId"));
            vlFulFill.put("cardNumber", activateResult.get("cardNumber"));
            vlFulFill.put("pinNumber", activateResult.get("pin"));
            vlFulFill.put("amount", activateResult.get("amount"));
            vlFulFill.put("responseCode", activateResult.get("responseCode"));
            vlFulFill.put("referenceNum", activateResult.get("referenceNum"));
            vlFulFill.put("authCode", activateResult.get("authCode"));
            vlFulFill.put("userLogin", userLogin);
            try {
                dispatcher.runAsync("createGcFulFillmentRecord", vlFulFill, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "AccountingGiftCerticateNumberCannotStoreFulfillmentInfo",
                        UtilMisc.toMap("errorString", e.toString() ), locale));
            }

            if (failure) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "AccountingValueLinkUnableToActivateGiftCard", locale));
            }

            // add some information to the answerMap for the email
            answerMap.put("cardNumber", activateResult.get("cardNumber"));
            answerMap.put("pinNumber", activateResult.get("pin"));
            answerMap.put("amount", activateResult.get("amount"));

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
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> giftCardReload(DispatchContext dctx, Map<String, Object> context) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
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

        // payment config
        GenericValue paymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "GIFT_CARD", null, true);
        String paymentConfig = null;
        if (paymentSetting != null) {
            paymentConfig = paymentSetting.getString("paymentPropertiesPath");
        }
        if (paymentConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
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
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderId, 
                            "orderItemSeqId", orderItem.get("orderItemSeqId"), 
                            "surveyId", surveyId).orderBy("-responseDate")
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
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

        String cardNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.cardNumber", delegator);
        String pinNumberKey = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.giftcert.reload.survey.pinNumber", delegator);
        String cardNumber = (String) answerMap.get(cardNumberKey);
        String pinNumber = (String) answerMap.get(pinNumberKey);

        // reload the gift card
        Map<String, Object> reloadCtx = new HashMap<String, Object>();
        reloadCtx.put("paymentConfig", paymentConfig);
        reloadCtx.put("currency", currency);
        reloadCtx.put("partyId", partyId);
        reloadCtx.put("orderId", orderId);
        reloadCtx.put("cardNumber", cardNumber);
        reloadCtx.put("pin", pinNumber);
        reloadCtx.put("amount", amount);
        reloadCtx.put("userLogin", userLogin);

        Map<String, Object> reloadResult = null;
        try {
            reloadResult = dispatcher.runSync("reloadGiftCard", reloadCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Unable to reload gift card", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingValueLinkUnableToReloadGiftCard", locale));
        }

        // create the fulfillment record
        Map<String, Object> vlFulFill = new HashMap<String, Object>();
        vlFulFill.put("typeEnumId", "GC_RELOAD");
        vlFulFill.put("merchantId", EntityUtilProperties.getPropertyValue(paymentConfig, "payment.valuelink.merchantId", delegator));
        vlFulFill.put("partyId", partyId);
        vlFulFill.put("orderId", orderId);
        vlFulFill.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
        vlFulFill.put("surveyResponseId", surveyResponse.get("surveyResponseId"));
        vlFulFill.put("cardNumber", cardNumber);
        vlFulFill.put("pinNumber", pinNumber);
        vlFulFill.put("amount", amount);
        vlFulFill.put("responseCode", reloadResult.get("responseCode"));
        vlFulFill.put("referenceNum", reloadResult.get("referenceNum"));
        vlFulFill.put("authCode", reloadResult.get("authCode"));
        vlFulFill.put("userLogin", userLogin);
        try {
            dispatcher.runAsync("createGcFulFillmentRecord", vlFulFill, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "AccountingGiftCerticateNumberCannotStoreFulfillmentInfo", locale));
        }

        Boolean processResult = (Boolean) reloadResult.get("processResult");
        if (reloadResult == null || reloadResult.containsKey(ModelService.ERROR_MESSAGE) || !processResult.booleanValue()) {
            Debug.logError("Reload Failed Need to Refund : " + reloadResult, module);

            // process the return
            try {
                Map<String, Object> refundCtx = UtilMisc.<String, Object>toMap("orderItem", orderItem, 
                        "partyId", partyId, "userLogin", userLogin);
                dispatcher.runAsync("refundGcPurchase", refundCtx, null, true, 300, true);
            } catch (GenericServiceException e) {
                Debug.logError(e, "ERROR! Unable to call create refund service; this failed reload will NOT be refunded", module);
            }

            String responseCode = "-1";
            if (processResult != null) {
                responseCode = (String) reloadResult.get("responseCode");
            }
            if ("17".equals(responseCode)) {
                Debug.logError("Error code : " + responseCode + " : Max Balance Exceeded", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "AccountingValueLinkUnableToRefundGiftCardMaxBalanceExceeded", locale));
            } else {
                Debug.logError("Error code : " + responseCode + " : Processing Error", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "AccountingValueLinkUnableToReloadGiftCardFailed", locale));
            }
        }

        // add some information to the answerMap for the email
        answerMap.put("processResult", reloadResult.get("processResult"));
        answerMap.put("responseCode", reloadResult.get("responseCode"));
        answerMap.put("previousAmount", reloadResult.get("previousAmount"));
        answerMap.put("amount", reloadResult.get("amount"));

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
            Map<String, Object> emailCtx = new HashMap<String, Object>();
            answerMap.put("locale", locale);

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
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "AccountingGiftCerticateNumberCannotSendEmailNotice",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
