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
package org.apache.ofbiz.accounting.thirdparty.verisign;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import paypal.payflow.PayflowAPI;
import paypal.payflow.SDKProperties;
/**
 * PayflowPro - Verisign PayFlow Pro <=> OFBiz Service Module
 */
public class PayflowPro {

    public static final String module = PayflowPro.class.getName();
    public final static String resource = "AccountingUiLabels";

    /**
     * Authorize credit card payment service. Service wrapper around PayFlow Pro API.
     * @param dctx Service Engine DispatchContext.
     * @param context Map context of parameters.
     * @return Response map, including RESPMSG, and RESULT keys.
     */
    public static Map<String, Object> ccProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        String orderId = (String) context.get("orderId");
        String cvv2 = (String) context.get("cardSecurityCode");
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        GenericValue party = (GenericValue) context.get("billToParty");
        GenericValue cc = (GenericValue) context.get("creditCard");
        GenericValue payPalPaymentMethod = (GenericValue) context.get("payPalPaymentMethod");
        GenericValue ps = (GenericValue) context.get("billingAddress");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configString = (String) context.get("paymentConfig");

        if (configString == null) {
            configString = "payment.properties";
        }

        boolean isPayPal = false;
        // Are we doing a cc or a paypal payment?
        if ("EXT_PAYPAL".equals(paymentPref.getString("paymentMethodTypeId"))) {
            isPayPal = true;
        }

        Map<String, String> data = new HashMap<String, String>();

        boolean isReAuth = false;
        if (isPayPal) {
            data.put("TRXTYPE", "A");
            data.put("TENDER", "P");
            data.put("ORIGID", payPalPaymentMethod.getString("transactionId"));
        } else {
            if (authTrans == null) {
                authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
            }

            // set the orderId as comment1 so we can query in PF Manager
            data.put("COMMENT1", orderId);
            data.put("PONUM", orderId);
            data.put("CUSTCODE", party.getString("partyId"));

            // transaction type
            if (comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "preAuth", configString, "payment.verisign.preAuth",  "Y")) {
                data.put("TRXTYPE", "A");
                // only support re-auth for auth types; sale types don't do it
                if (authTrans != null) {
                    String refNum = authTrans.getString("referenceNum");
                    data.put("ORIGID", refNum);
                    isReAuth = true;
                }
            } else {
                data.put("TRXTYPE", "S");
            }

            // credit card tender
            data.put("TENDER", "C");

            // card security code
            if (UtilValidate.isNotEmpty(cvv2)) {
                data.put("CVV2", cvv2);
            }

            // get the payment information
            data.put("ACCT", cc.getString("cardNumber"));

            // name on card
            String name = cc.getString("firstNameOnCard") + " " + cc.getString("lastNameOnCard");
            data.put("FIRSTNAME", cc.getString("firstNameOnCard"));
            data.put("LASTNAME", cc.getString("lastNameOnCard"));
            data.put("COMMENT2", name);
            if (cc.get("expireDate") != null) {
                String exp = cc.getString("expireDate");
                String expDate = exp.substring(0, 2);

                expDate = expDate + exp.substring(exp.length() - 2);
                data.put("EXPDATE", expDate);
            }

            // gather the address info
            if (ps != null) {
                String street = ps.getString("address1") + ((UtilValidate.isNotEmpty(ps.getString("address2"))) ? " " + ps.getString("address2") : "");
                data.put("STREET"+"["+street.length()+"]", street);
                data.put("ZIP", ps.getString("postalCode"));
            }
        }

        // set the amount
        data.put("AMT", processAmount.toString());

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, configString, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, configString);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        //if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&AUTHCODE=T&PNREF=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Response from Verisign: " + resp, module);
        }
        if (isPayPal) {
            // Attach the avs info returned in doExpressCheckout and stored in PayPalPaymentMethod
            resp += "&AVSADDR=" + payPalPaymentMethod.getString("avsAddr") + "&AVSZIP=" + payPalPaymentMethod.getString("avsZip");
        }

        // check the response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        parseAuthResponse(delegator, paymentGatewayConfigId, resp, result, configString, isReAuth, isPayPal);
        result.put("processAmount", processAmount);
        return result;
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        BigDecimal amount = (BigDecimal) context.get("captureAmount");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configString = (String) context.get("paymentConfig");
        Locale locale = (Locale) context.get("locale");
        if (configString == null) {
            configString = "payment.properties";
        }

        boolean isPayPal = false;
        // Are we doing a cc or a paypal payment?
        if ("EXT_PAYPAL".equals(paymentPref.getString("paymentMethodTypeId"))) {
            isPayPal = true;
        }

        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        if (authTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }

        // auth ref number
        String refNum = authTrans.getString("referenceNum");
        Map<String, String> data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Delayed Capture)
        data.put("TRXTYPE", "D");

        if (isPayPal) {
            // paypal tender
            data.put("TENDER", "P");
            data.put("CAPTURECOMPLETE", "N");
        } else {
            // credit card tender
            data.put("TENDER", "C");

            // get the orderID
            String orderId = paymentPref.getString("orderId");
            data.put("COMMENT1", orderId);
        }


        // amount to capture
        data.put("AMT", amount.toString());

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, configString, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, configString);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&AUTHCODE=T&PNREF=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // check the response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        parseCaptureResponse(resp, result);
        result.put("captureAmount", amount);
        return result;
    }

    public static Map<String, Object> ccVoid(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        BigDecimal amount = (BigDecimal) context.get("releaseAmount");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configString = (String) context.get("paymentConfig");
        Locale locale = (Locale) context.get("locale");
        if (configString == null) {
            configString = "payment.properties";
        }

        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        if (authTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }

        boolean isPayPal = false;
        // Are we doing a cc or a paypal payment?
        if ("EXT_PAYPAL".equals(paymentPref.getString("paymentMethodTypeId"))) {
            isPayPal = true;
        }

        // auth ref number
        String refNum = authTrans.getString("referenceNum");
        Map<String, String> data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Void)
        data.put("TRXTYPE", "V");

        // get the orderID
        String orderId = paymentPref.getString("orderId");

        if (isPayPal) {
            data.put("TENDER", "P");

            data.put("NOTE", orderId);
        } else {
            // credit card tender
            data.put("TENDER", "C");

            data.put("COMMENT1", orderId);

            // amount to void
            data.put("AMT", amount.toString());
        }


        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, configString, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, configString);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&AUTHCODE=T&PNREF=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // check the response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        parseVoidResponse(resp, result);
        result.put("releaseAmount", amount);
        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal amount = (BigDecimal) context.get("refundAmount");
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configString = (String) context.get("paymentConfig");
        Locale locale = (Locale) context.get("locale");
        if (configString == null) {
            configString = "payment.properties";
        }

        GenericValue captureTrans = PaymentGatewayServices.getCaptureTransaction(paymentPref);

        if (captureTrans == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }

        boolean isPayPal = false;
        // Are we doing a cc or a paypal payment?
        if ("EXT_PAYPAL".equals(paymentPref.getString("paymentMethodTypeId"))) {
            isPayPal = true;
        }

        // auth ref number
        String refNum = captureTrans.getString("referenceNum");
        Map<String, String> data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Credit)
        data.put("TRXTYPE", "C");

        // get the orderID
        String orderId = paymentPref.getString("orderId");

        if (isPayPal) {
            data.put("TENDER", "P");

            data.put("MEMO", orderId);
            // PayPal won't allow us to refund more than the capture amount
            BigDecimal captureAmount = captureTrans.getBigDecimal("amount");
            amount = amount.min(captureAmount);
        } else {
            // credit card tender
            data.put("TENDER", "C");

            data.put("COMMENT1", orderId);
        }

        // amount to capture
        data.put("AMT", amount.toString());

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, configString, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, configString);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&AUTHCODE=T&PNREF=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // check the response
        Map<String, Object> result = ServiceUtil.returnSuccess();
        parseRefundResponse(resp, result);
        result.put("refundAmount", amount);
        return result;
    }


    public static Map<String, Object> setExpressCheckout(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        ShoppingCart cart = (ShoppingCart) context.get("cart");
        Locale locale = cart.getLocale();
        GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, cart.getProductStoreId(), "EXT_PAYPAL", null, true);
        String paymentGatewayConfigId = payPalPaymentSetting.getString("paymentGatewayConfigId");
        String configString = "payment.properties";

        if (cart == null || cart.items().size() <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingErrorUiLabels", 
                    "AccountingPayPalShoppingCartIsEmpty", locale));
        }

        Map<String, String> data = new HashMap<String, String>();

        data.put("TRXTYPE", "O");
        data.put("TENDER", "P");
        data.put("ACTION", "S");
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        if (UtilValidate.isNotEmpty(token)) {
            data.put("TOKEN", token);
        }
        data.put("RETURNURL", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "returnUrl", configString, "payment.verisign.returnUrl"));
        data.put("CANCELURL", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "cancelReturnUrl", configString, "payment.verisign.cancelReturnUrl"));

        try {
            addCartDetails(data, cart);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayflowErrorRetreivingCartDetails", locale));
        }

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, null, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, null);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&TOKEN=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        Map<String, String> responseMap = parseResponse(resp);
        String result = responseMap.get("RESULT");
        if (!"0".equals(result)) {
            String respMsg = responseMap.get("RESPMSG");
            Debug.logError("A problem occurred while requesting an express checkout token from paypal: Result = " + result + ", Message = " + respMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingErrorUiLabels", 
                    "AccountingPayPalCommunicationError", locale));
        }
        token = responseMap.get("TOKEN");
        cart.setAttribute("payPalCheckoutToken", token);
        return ServiceUtil.returnSuccess();
    }

    private static void addCartDetails(Map<String, String> parameterMap, ShoppingCart cart) throws GenericEntityException {
        parameterMap.put("CURRENCY", cart.getCurrency());
        int line = 0;
        for (ShoppingCartItem item : cart.items()) {
            //paramMap.put("L_NUMBER" + line, item.getProductId());
            parameterMap.put("L_NAME" + line, item.getName());
            parameterMap.put("L_DESC" + line, item.getDescription());
            parameterMap.put("L_AMT" + line, item.getBasePrice().setScale(2).toPlainString());
            parameterMap.put("L_QTY" + line, item.getQuantity().toBigInteger().toString());
            line++;
            BigDecimal otherAdjustments = item.getOtherAdjustments();
            if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
                parameterMap.put("L_NAME" + line, item.getName() + " Adjustments");
                parameterMap.put("L_DESC" + line, "Adjustments for item: " + item.getName());
                parameterMap.put("L_AMT" + line, otherAdjustments.setScale(2).toPlainString());
                parameterMap.put("L_QTY" + line, "1");
                line++;
            }
        }
        BigDecimal otherAdjustments = cart.getOrderOtherAdjustmentTotal();
        if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
            parameterMap.put("L_NAME" + line, "Order Adjustments");
            parameterMap.put("L_AMT" + line, otherAdjustments.setScale(2).toPlainString());
            parameterMap.put("L_QTY" + line, "1");
            line++;
        }
        parameterMap.put("ITEMAMT", cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
        parameterMap.put("TAXAMT", cart.getTotalSalesTax().setScale(2).toPlainString());
        parameterMap.put("FREIGHTAMT", cart.getTotalShipping().setScale(2).toPlainString());
        parameterMap.put("AMT", cart.getGrandTotal().setScale(2).toPlainString());

        if (!cart.shippingApplies()) {
            parameterMap.put("NOSHIPPING", "1");
        } else {
            GenericValue shippingAddress = cart.getShippingAddress();
            parameterMap.put("ADDROVERRIDE", "1");
            parameterMap.put("SHIPTOSTREET", StringUtils.left(shippingAddress.getString("address1"), 30));
            parameterMap.put("SHIPTOSTREET2", StringUtils.left(shippingAddress.getString("address2"), 30));
            parameterMap.put("SHIPTOCITY", StringUtils.left(shippingAddress.getString("city"), 40));
            if (shippingAddress.getString("stateProvinceGeoId") != null && !"_NA_".equals(shippingAddress.getString("stateProvinceGeoId"))) {
                GenericValue stateProvinceGeo = shippingAddress.getRelatedOne("StateProvinceGeo", false);
                parameterMap.put("SHIPTOSTATE", StringUtils.left(stateProvinceGeo.getString("geoCode"), 40));
            }
            parameterMap.put("SHIPTOZIP", StringUtils.left(shippingAddress.getString("postalCode"), 16));
            GenericValue countryGeo = shippingAddress.getRelatedOne("CountryGeo", false);
            parameterMap.put("SHIPTOCOUNTRY", StringUtils.left(countryGeo.getString("geoCode"), 2));
        }
    }

    public static Map<String, Object> getExpressCheckout(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        ShoppingCart cart = (ShoppingCart) context.get("cart");
        Locale locale = cart.getLocale();
        GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, cart.getProductStoreId(), "EXT_PAYPAL", null, true);
        String paymentGatewayConfigId = payPalPaymentSetting.getString("paymentGatewayConfigId");
        String configString = "payment.properties";

        Map<String, String> data = new HashMap<String, String>();
        data.put("TRXTYPE", "O");
        data.put("TENDER", "P");
        data.put("ACTION", "G");
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        if (UtilValidate.isNotEmpty(token)) {
            data.put("TOKEN", token);
        }

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, null, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, null);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&PAYERID=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        Map<String, String> responseMap = parseResponse(resp);
        if (!"0".equals(responseMap.get("RESULT"))) {
            Debug.logError("A problem occurred while requesting the checkout details from paypal: Result = " + responseMap.get("RESULT") + ", Message = " + responseMap.get("RESPMSG"), module);
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingErrorUiLabels", 
                    "AccountingPayPalCommunicationError", locale));
        }

        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put("userLogin", cart.getUserLogin());
        inMap.put("partyId", cart.getOrderPartyId());
        inMap.put("contactMechId", cart.getShippingContactMechId());
        inMap.put("fromDate", UtilDateTime.nowTimestamp());
        inMap.put("payerId", responseMap.get("PAYERID"));
        inMap.put("expressCheckoutToken", token);
        inMap.put("payerStatus", responseMap.get("PAYERSTATUS"));
        inMap.put("avsAddr", responseMap.get("AVSADDR"));
        inMap.put("avsZip", responseMap.get("AVSZIP"));
        inMap.put("correlationId", responseMap.get("CORRELATIONID"));
        Map<String, Object> outMap = null;
        try {
            outMap = dispatcher.runSync("createPayPalPaymentMethod", inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        String paymentMethodId = (String) outMap.get("paymentMethodId");

        cart.clearPayments();
        cart.addPaymentAmount(paymentMethodId, cart.getGrandTotal(), true);

        return ServiceUtil.returnSuccess();

    }

    public static Map<String, Object> doExpressCheckout(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        OrderReadHelper orh = new OrderReadHelper(delegator, paymentPref.getString("orderId"));
        GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, orh.getProductStoreId(), "EXT_PAYPAL", null, true);
        String paymentGatewayConfigId = payPalPaymentSetting.getString("paymentGatewayConfigId");
        String configString = "payment.properties";
        GenericValue payPalPaymentMethod = null;
        try {
            payPalPaymentMethod = paymentPref.getRelatedOne("PaymentMethod", false);
            payPalPaymentMethod = payPalPaymentMethod.getRelatedOne("PayPalPaymentMethod", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        BigDecimal processAmount = paymentPref.getBigDecimal("maxAmount");

        Map<String, String> data = new HashMap<String, String>();
        data.put("TRXTYPE", "O");
        data.put("TENDER", "P");
        data.put("PAYERID", payPalPaymentMethod.getString("payerId"));
        data.put("TOKEN", payPalPaymentMethod.getString("expressCheckoutToken"));
        data.put("ACTION", "D");
        // set the amount
        data.put("AMT", processAmount.setScale(2).toPlainString());

        PayflowAPI pfp = init(delegator, paymentGatewayConfigId, null, context);

        // get the base params
        StringBuilder params = makeBaseParams(delegator, paymentGatewayConfigId, null);

        // parse the context parameters
        params.append("&").append(parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp;
        if (!comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "enableTransmit", configString, "payment.verisign.enable_transmit",  "false")) {
            resp = pfp.submitTransaction(params.toString(), pfp.generateRequestId());
        } else {
            resp = "RESULT=0&PAYERID=" + (new Date()).getTime() + "&RESPMSG=Testing";
        }

        Map<String, String> responseMap = parseResponse(resp);

        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put("userLogin", userLogin);
        inMap.put("paymentMethodId", payPalPaymentMethod.get("paymentMethodId"));
        inMap.put("transactionId", responseMap.get("PNREF"));
        Map<String, Object> outMap = null;
        try {
            outMap = dispatcher.runSync("updatePayPalPaymentMethod", inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(outMap)) {
            Debug.logError(ServiceUtil.getErrorMessage(outMap), module);
            return outMap;
        }
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, String> parseResponse(String resp) {
        Debug.logInfo("Verisign response string: " + resp, module);
        Map<String, String> parameters = new HashMap<String, String>();
        List<String> params = StringUtil.split(resp, "&");
        for (String str : params) {
            if (str.length() > 0) {
                List<String> kv = StringUtil.split(str, "=");
                String k = kv.get(0);
                String v = kv.get(1);

                if (k != null && v != null)
                    parameters.put(k, v);
            }
        }
        return parameters;
    }
    private static void parseAuthResponse(Delegator delegator, String paymentGatewayConfigId, String resp, Map<String, Object> result, String resource, boolean isReAuth, boolean isPayPal) {
        Map<String, String> parameters = parseResponse(resp);

        // txType
        boolean isSale = !comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "preAuth", resource, "payment.verisign.preAuth", "Y");

        // avs checking - ignore on re-auth
        boolean avsCheckOkay = true;
        String avsCode = null;
        if (!isReAuth) {
            boolean checkAvs = comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "checkAvs", resource, "payment.verisign.checkAvs", "Y");
            if (checkAvs && !isSale) {
                String addAvs = (String) parameters.get("AVSADDR");
                String zipAvs = (String) parameters.get("AVSZIP");
                avsCode = addAvs + zipAvs;
                if (addAvs == null || "N".equals(addAvs) || zipAvs == null || "N".equals(zipAvs)) {
                    avsCheckOkay = false;
                }
            }
        }

        // cvv2 checking - ignore on re-auth or paypal
        boolean cvv2CheckOkay = true;
        String cvvCode = null;
        if (!isReAuth && !isPayPal) {
            boolean checkCvv2 = comparePaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "checkCvv2", resource, "payment.verisign.checkCvv2", "Y");
            if (checkCvv2 && !isSale) {
                cvvCode = (String) parameters.get("CVV2MATCH");
                if (cvvCode == null || "N".equals(cvvCode)) {
                    cvv2CheckOkay = false;
                }
            }
        }

        String respCode = (String) parameters.get("RESULT");
        int codeInt = -999; // custom response code -- not from payflow docs
        try {
            codeInt = Integer.parseInt(respCode);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse response code; not a number!", module);
        }


        result.put("authRefNum", parameters.get("PNREF"));

        if (codeInt == 0 && avsCheckOkay && cvv2CheckOkay) {
            result.put("authResult", Boolean.TRUE);
            result.put("authCode", parameters.get("AUTHCODE"));
        } else if (codeInt < 0) {
            // communications error
            Debug.logWarning("In PayflowPro failing authorization; respCode/RESULT=" + respCode + ", avsCheckOkay=" + avsCheckOkay + ", cvv2CheckOkay=" + cvv2CheckOkay + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("authResult", Boolean.FALSE);
            result.put("authRefNum", respCode);
        } else if (codeInt == 0) {
            Debug.logWarning("In PayflowPro approved, but invalid flags; respCode/RESULT=" + respCode + ", avsCheckOkay=" + avsCheckOkay + ", cvv2CheckOkay=" + cvv2CheckOkay + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("authResult", Boolean.TRUE);
            result.put("authCode", parameters.get("AUTHCODE"));
        } else {
            // other error
            Debug.logWarning("In PayflowPro failing authorization; respCode/RESULT=" + respCode + ", avsCheckOkay=" + avsCheckOkay + ", cvv2CheckOkay=" + cvv2CheckOkay + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("authResult", Boolean.FALSE);

            // now check certain special conditions and report back through the generic params
            if ("12".equals(respCode)) {
                result.put("resultDeclined", Boolean.TRUE);
            } else if ("50".equals(respCode)) {
                result.put("resultNsf", Boolean.TRUE);
            } else if ("23".equals(respCode)) {
                result.put("resultBadCardNumber", Boolean.TRUE);
            } else if ("24".equals(respCode)) {
                result.put("resultBadExpire", Boolean.TRUE);
            }
        }
        result.put("cvCode", cvvCode);
        result.put("avsCode", avsCode);
        result.put("authFlag", parameters.get("RESULT"));
        result.put("authMessage", parameters.get("RESPMSG"));
    }

    private static void parseCaptureResponse(String resp, Map<String, Object> result) {
        Map<String, String> parameters = parseResponse(resp);

        String respCode = (String) parameters.get("RESULT");
        int codeInt = -999; // custom response code -- not from payflow docs
        try {
            codeInt = Integer.parseInt(respCode);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse response code; not a number!", module);
        }

        if (codeInt == 0) {
            result.put("captureResult", Boolean.TRUE);
            result.put("captureCode", parameters.get("AUTHCODE"));
        } else if (codeInt < 0) {
            // communications error
            Debug.logWarning("In PayflowPro failing capture; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("captureResult", Boolean.FALSE);
            result.put("captureRefNum", respCode);
        } else {
            Debug.logWarning("In PayflowPro failing capture; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("captureResult", Boolean.FALSE);
        }
        result.put("captureRefNum", parameters.get("PNREF"));
        result.put("captureFlag", parameters.get("RESULT"));
        result.put("captureMessage", parameters.get("RESPMSG"));
    }

    private static void parseVoidResponse(String resp, Map<String, Object> result) {
        Map<String, String> parameters = parseResponse(resp);

        String respCode = (String) parameters.get("RESULT");
        int codeInt = -999; // custom response code -- not from payflow docs
        try {
            codeInt = Integer.parseInt(respCode);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse response code; not a number!", module);
        }

        if (codeInt == 0) {
            result.put("releaseResult", Boolean.TRUE);
            result.put("releaseCode", parameters.get("AUTHCODE"));
        } else if (codeInt < 0) {
            // communications error
            Debug.logWarning("In PayflowPro failing void; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("releaseResult", Boolean.FALSE);
            result.put("releaseRefNum", respCode);
        } else {
            Debug.logWarning("In PayflowPro failing void; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("releaseResult", Boolean.FALSE);
        }
        result.put("releaseRefNum", parameters.get("PNREF"));
        result.put("releaseFlag", parameters.get("RESULT"));
        result.put("releaseMessage", parameters.get("RESPMSG"));
    }

    private static void parseRefundResponse(String resp, Map<String, Object> result) {
        Map<String, String> parameters = parseResponse(resp);

        String respCode = (String) parameters.get("RESULT");
        int codeInt = -999; // custom response code -- not from payflow docs
        try {
            codeInt = Integer.parseInt(respCode);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse response code; not a number!", module);
        }

        if (codeInt == 0) {
            result.put("refundResult", Boolean.TRUE);
            result.put("refundCode", parameters.get("AUTHCODE"));
        } else if (codeInt < 0) {
            // communications error
            Debug.logWarning("In PayflowPro failing refund; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("refundResult", Boolean.FALSE);
            result.put("refundRefNum", respCode);
        } else {
            Debug.logWarning("In PayflowPro failing refund; respCode/RESULT=" + respCode + "; PNREF=" + parameters.get("PNREF") + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("refundResult", Boolean.FALSE);
        }
        result.put("refundRefNum", parameters.get("PNREF"));
        result.put("refundFlag", parameters.get("RESULT"));
        result.put("refundMessage", parameters.get("RESPMSG"));
    }

    private static String parseContext(Map<String, ? extends Object> context) {
        StringBuilder buf = new StringBuilder();
        Set<String> keySet = context.keySet();
        Iterator<String> i = keySet.iterator();

        while (i.hasNext()) {
            String name = (String) i.next();
            Object valueObj = context.get(name);

            if (valueObj == null || (valueObj instanceof String) && ((String) valueObj).length() == 0) {
                // not valid; do nothing
            } else {
                String value = valueObj.toString();

                // Payflow expects an unencoded name/value pair string
                buf.append(name).append("=");
                buf.append(value);
                if (i.hasNext())
                    buf.append("&");
            }
        }
        return buf.toString();
    }

    private static StringBuilder makeBaseParams(Delegator delegator, String paymentGatewayConfigId, String resource) {
        StringBuilder buf = new StringBuilder();

        try {
            buf.append("PARTNER=");
            buf.append(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "partner", resource, "payment.verisign.partner", "VeriSign"));
            buf.append("&");
            buf.append("VENDOR=");
            buf.append(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "vendor", resource, "payment.verisign.vendor", "nobody"));
            buf.append("&");
            buf.append("USER=");
            buf.append(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "userId", resource, "payment.verisign.user", "nobody"));
            buf.append("&");
            buf.append("PWD=");
            buf.append(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "pwd", resource, "payment.verisign.pwd", "password"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return null;
        }
        return buf;
    }

    private static PayflowAPI init(Delegator delegator, String paymentGatewayConfigId, String resource, Map<String, ? extends Object> context) {
        // No more used
        // String certsPath = FlexibleStringExpander.expandString(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "certsPath", resource, "payment.verisign.certsPath", "pfcerts"), context);
        String hostAddress = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hostAddress", resource, "payment.verisign.hostAddress", "pilot-payflowpro.paypal.com");
        Integer hostPort = Integer.decode(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hostPort", resource, "payment.verisign.hostPort", "443"));
        Integer timeout = Integer.decode(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "timeout", resource, "payment.verisign.timeout", "80"));
        String proxyAddress = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "proxyAddress", resource, "payment.verisign.proxyAddress", "");
        Integer proxyPort = Integer.decode(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "proxyPort", resource, "payment.verisign.proxyPort", "80"));
        String proxyLogon = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "proxyLogon", resource, "payment.verisign.proxyLogon", "");
        String proxyPassword = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "proxyPassword", resource, "payment.verisign.proxyPassword", "");
        String logFileName = FlexibleStringExpander.expandString(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "logFileName", resource, "payment.verisign.logFileName", ""), context);
        Integer loggingLevel = Integer.decode(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "loggingLevel", resource, "payment.verisign.loggingLevel", "6"));
        Integer maxLogFileSize = Integer.decode(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "maxLogFileSize", resource, "payment.verisign.maxLogFileSize", "1000000"));
        boolean stackTraceOn = "Y".equalsIgnoreCase(getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "stackTraceOn", resource, "payment.verisign.stackTraceOn", "N"));

        PayflowAPI pfp = new PayflowAPI(hostAddress, hostPort.intValue(), timeout.intValue(), proxyAddress,
                proxyPort.intValue(), proxyLogon, proxyPassword);
        SDKProperties.setLogFileName(logFileName);
        SDKProperties.setLoggingLevel(loggingLevel);
        SDKProperties.setMaxLogFileSize(maxLogFileSize);
        SDKProperties.setStackTraceOn(stackTraceOn);
        return pfp;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue payflowPro = EntityQuery.use(delegator).from("PaymentGatewayPayflowPro").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
                if (UtilValidate.isNotEmpty(payflowPro)) {
                    Object payflowProField = payflowPro.get(paymentGatewayConfigParameterName);
                    if (payflowProField != null) {
                        returnValue = payflowProField.toString().trim();
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

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName, String defaultValue) {
        String returnValue = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName, resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }

    private static boolean comparePaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                        String resource, String parameterName, String compareValue) {
        boolean returnValue = false;

        String value = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName, resource, parameterName, compareValue);
        if (UtilValidate.isNotEmpty(value)) {
            returnValue = value.trim().equalsIgnoreCase(compareValue);
        }
        return returnValue;
    }

/*
 * RESULT values (and RESPMSG text)
 *
0 Approved
1 User authentication failed. Error is caused by one or more of the following:
Login information is incorrect. Verify that USER, VENDOR, PARTNER, and
PASSWORD have been entered correctly. See Table 3.2 on page 26 for additional
information and field descriptions.
Invalid Processor information entered. Contact merchant bank to verify.
"Allowed IP Address" security feature implemented. The transaction is coming
from an unknown IP address. See VeriSign Manager online help for details on how
to use Manager to update the allowed IP addresses.
You are using a test (not active) account to submit a transaction to the live VeriSign
servers. Change the URL from pilot-payflowpro.paypal.com to payflowpro.paypal.com.
2 Invalid tender type. Your merchant bank account does not support the following
credit card type that was submitted.
3 Invalid transaction type. Transaction type is not appropriate for this transaction. For
example, you cannot credit an authorization-only transaction.
4 Invalid amount format. Use the format: #####.##  Do not include currency
symbols or commas.
5 Invalid merchant information. Processor does not recognize your merchant account
information. Contact your bank account acquirer to resolve this problem.
6 Invalid or unsupported currency code
7 Field format error. Invalid information entered. See RESPMSG.
8 Not a transaction server
9 Too many parameters or invalid stream
10 Too many line items
11 Client time-out waiting for response
12 Declined. Check the credit card number, expiration date, and transaction information to
make sure they were entered correctly. If this does not resolve the problem, have the
customer call their card issuing bank to resolve.
13 Referral. Transaction cannot be approved electronically but can be approved with a
verbal authorization. Contact your merchant bank to obtain an authorization and submit
a manual Voice Authorization transaction.
14 Invalid Client Certification ID. Check the HTTP header. If the tag, X-VPS-VIT-
CLIENT-CERTIFICATION-ID, is missing, RESULT code 14 is returned.
19 Original transaction ID not found. The transaction ID you entered for this
transaction is not valid. See RESPMSG.
20 Cannot find the customer reference number
22 Invalid ABA number

23 Invalid account number. Check credit card number and re-submit.
24 Invalid expiration date. Check and re-submit.
25 Invalid Host Mapping. You are trying to process a tender type such as Discover Card,
but you are not set up with your merchant bank to accept this card type.
26 Invalid vendor account
27 Insufficient partner permissions
28 Insufficient user permissions
29 Invalid XML document. This could be caused by an unrecognized XML tag or a bad
XML format that cannot be parsed by the system.
30 Duplicate transaction
31 Error in adding the recurring profile
32 Error in modifying the recurring profile
33 Error in canceling the recurring profile
34 Error in forcing the recurring profile
35 Error in reactivating the recurring profile
36 OLTP Transaction failed
37 Invalid recurring profile ID
50 Insufficient funds available in account
99 General error. See RESPMSG.
100 Transaction type not supported by host
101 Time-out value too small
102 Processor not available
103 Error reading response from host
104 Timeout waiting for processor response. Try your transaction again.
105 Credit error. Make sure you have not already credited this transaction, or that this
transaction ID is for a creditable transaction. (For example, you cannot credit an
authorization.)
106 Host not available
107 Duplicate suppression time-out

108 Void error. See RESPMSG. Make sure the transaction ID entered has not already been
voided. If not, then look at the Transaction Detail screen for this transaction to see if it
has settled. (The Batch field is set to a number greater than zero if the transaction has
been settled). If the transaction has already settled, your only recourse is a reversal
(credit a payment or submit a payment for a credit).
109 Time-out waiting for host response
111 Capture error. Either an attempt to capture a transaction that is not an authorization
transaction type, or an attempt to capture an authorization transaction that has already
been captured.
112 Failed AVS check. Address and ZIP code do not match. An authorization may still
exist on the cardholder account.
113 Merchant sale total will exceed the sales cap with current transaction. ACH
transactions only.
114 Card Security Code (CSC) Mismatch. An authorization may still exist on the
cardholder account.
115 System busy, try again later
116 VPS Internal error. Failed to lock terminal number
117 Failed merchant rule check. One or more of the following three failures occurred:
An attempt was made to submit a transaction that failed to meet the security settings
specified on the Payflow Pro Java SDK Security Settings page. If the transaction
exceeded the Maximum Amount security setting, then no values are returned for AVS
or CSC.
AVS validation failed. The AVS return value should appear in the RESPMSG.
CSC validation failed. The CSC return value should appear in the RESPMSG.
118 Invalid keywords found in string fields
122 Merchant sale total will exceed the credit cap with current transaction. ACH
transactions only.
125 Fraud Protection Services Filter _ Declined by filters

126 Fraud Protection Services Filter _ Flagged for review by filters
Important Note: Result code 126 indicates that a transaction triggered a fraud filter.
This is not an error, but a notice that the transaction is in a review status. The
transaction has been authorized but requires you to review and to manually accept the
transaction before it will be allowed to settle.
Result code 126 is intended to give you an idea of the kind of transaction that is
considered suspicious to enable you to evaluate whether you can benefit from using the
Fraud Protection Services.
To eliminate result 126, turn the filters off.
For more information, see the User Guide for Payflow Pro With Fraud Protection
Services or User Guide for Payflow Link Guide With Fraud Protection Services.
127 Fraud Protection Services Filter _ Not processed by filters
128 Fraud Protection Services Filter _ Declined by merchant after being flagged for
review by filters
131 Version 1 Payflow Pro SDK client no longer supported. Upgrade to the most recent
version of the Payflow Pro client.
150 Issuing bank timed out
151 Issuing bank unavailable
1000 Generic host error. This is a generic message returned by your credit card processor.
The RESPMSG will contain more information describing the error.
1001 Buyer Authentication Service unavailable
1002 Buyer Authentication Service _ Transaction timeout
1003 Buyer Authentication Service _ Invalid client version
1004 Buyer Authentication Service _ Invalid timeout value
1011 Buyer Authentication Service unavailable
1012 Buyer Authentication Service unavailable
1013 Buyer Authentication Service unavailable
1014 Buyer Authentication Service _ Merchant is not enrolled for Buyer
Authentication Service (3-D Secure).
1016 Buyer Authentication Service _ 3-D Secure error response received. Instead of
receiving a PARes response to a Validate Authentication transaction, an error response
was received.
1017 Buyer Authentication Service _ 3-D Secure error response is invalid. An error
response is received and the response is not well formed for a Validate Authentication
transaction.

1021 Buyer Authentication Service _ Invalid card type
1022 Buyer Authentication Service _ Invalid or missing currency code
1023 Buyer Authentication Service _ merchant status for 3D secure is invalid
1041 Buyer Authentication Service _ Validate Authentication failed: missing or
invalid PARES
1042 Buyer Authentication Service _ Validate Authentication failed: PARES format is
invalid
1043 Buyer Authentication Service _ Validate Authentication failed: Cannot find
successful Verify Enrollment
1044 Buyer Authentication Service _ Validate Authentication failed: Signature
validation failed for PARES
1045 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid amount in PARES
1046 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid acquirer in PARES
1047 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid Merchant ID in PARES
1048 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid card number in PARES
1049 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid currency code in PARES
1050 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid XID in PARES
1051 Buyer Authentication Service _ Validate Authentication failed: Mismatched or
invalid order date in PARES
1052 Buyer Authentication Service _ Validate Authentication failed: This PARES was
already validated for a previous Validate Authentication transaction

 */

/* RESULT for communication errors (less than 0)
 *
-1 Failed to connect to host
-2 Failed to resolve hostname
-5 Failed to initialize SSL context
-6 Parameter list format error: & in name
-7 Parameter list format error: invalid [ ] name length clause
-8 SSL failed to connect to host
-9 SSL read failed
-10 SSL write failed
-11 Proxy authorization failed
-12 Timeout waiting for response
-13 Select failure
-14 Too many connections
-15 Failed to set socket options
-20 Proxy read failed
-21 Proxy write failed
-22 Failed to initialize SSL certificate
-23 Host address not specified
-24 Invalid transaction type
-25 Failed to create a socket
-26 Failed to initialize socket layer
-27 Parameter list format error: invalid [ ] name length clause
-28 Parameter list format error: name
-29 Failed to initialize SSL connection
-30 Invalid timeout value

-31 The certificate chain did not validate, no local certificate found
-32 The certificate chain did not validate, common name did not match URL
- 40 Unexpected Request ID found in request.
- 41 Required Request ID not found in request
- 42 Required Response ID not found in request
- 43 Unexpected Response ID found in request
- 44 Response ID not found in the response received from the server
-99 Out of memory
-100 Parameter list cannot be empty
-103 Context initialization failed
-104 Unexpected transaction state
-105 Invalid name value pair request
-106 Invalid response format
-107 This XMLPay version is not supported
-108 The server certificate chain did not validate
-109 Unable to do logging
-111 The following error occurred while initializing from message file: <Details of
the error message>
-113 Unable to round and truncate the currency value simultaneously
 */
}
