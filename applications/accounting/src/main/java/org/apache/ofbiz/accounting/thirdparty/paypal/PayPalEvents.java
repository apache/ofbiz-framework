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
package org.apache.ofbiz.accounting.thirdparty.paypal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;


public class PayPalEvents {

    public static final String resource = "AccountingUiLabels";
    public static final String resourceErr = "AccountingErrorUiLabels";
    public static final String commonResource = "CommonUiLabels";
    public static final String module = PayPalEvents.class.getName();

    /** Initiate PayPal Request */
    public static String callPayPal(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        // get the orderId
        String orderId = (String) request.getAttribute("orderId");

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingOrderHeader", locale));
            return "error";
        }

        // get the order total
        String orderTotal = orderHeader.getBigDecimal("grandTotal").toPlainString();
        String currencyUom = orderHeader.getString("currencyUom");

        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(request);

        if (productStore == null) {
            Debug.logError("ProductStore is null", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }

        // get the payment properties file
        GenericValue paymentConfig = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStore.getString("productStoreId"), "EXT_PAYPAL", null, true);
        String configString = null;
        String paymentGatewayConfigId = null;
        if (paymentConfig != null) {
            paymentGatewayConfigId = paymentConfig.getString("paymentGatewayConfigId");
            configString = paymentConfig.getString("paymentPropertiesPath");
        }

        if (configString == null) {
            configString = "payment.properties";
        }

        // get the company name
        String company = UtilFormatOut.checkEmpty(productStore.getString("companyName"), "");

        // create the item name
        String itemName = UtilProperties.getMessage(resource, "AccountingOrderNr", locale) + orderId + " " +
                                 (company != null ? UtilProperties.getMessage(commonResource, "CommonFrom", locale) + " "+ company : "");
        String itemNumber = "0";

        // get the redirect url
        String redirectUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "redirectUrl", configString, "payment.paypal.redirect");

        // get the notify url
        String notifyUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "notifyUrl", configString, "payment.paypal.notify");

        // get the return urls
        String returnUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "returnUrl", configString, "payment.paypal.return");

        // get the cancel return urls
        String cancelReturnUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "cancelReturnUrl", configString, "payment.paypal.cancelReturn");

        // get the image url
        String imageUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "imageUrl", configString, "payment.paypal.image");

        // get the paypal account
        String payPalAccount = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "businessEmail", configString, "payment.paypal.business");

        if (UtilValidate.isEmpty(redirectUrl)
            || UtilValidate.isEmpty(notifyUrl)
            || UtilValidate.isEmpty(returnUrl)
            || UtilValidate.isEmpty(imageUrl)
            || UtilValidate.isEmpty(payPalAccount)) {
            Debug.logError("Payment properties is not configured properly, some notify URL from PayPal is not correctly defined!", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }

        // create the redirect string
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("cmd", "_xclick");
        parameters.put("business", payPalAccount);
        parameters.put("item_name", itemName);
        parameters.put("item_number", itemNumber);
        parameters.put("invoice", orderId);
        parameters.put("custom", userLogin.getString("userLoginId"));
        parameters.put("amount", orderTotal);
        parameters.put("currency_code", currencyUom);
        parameters.put("return", returnUrl);
        if (UtilValidate.isNotEmpty(cancelReturnUrl)) parameters.put("cancel_return", cancelReturnUrl);
        parameters.put("notify_url", notifyUrl);
        parameters.put("image_url", imageUrl);
        parameters.put("no_note", "1");        // no notes allowed in paypal (not passed back)
        parameters.put("no_shipping", "1");    // no shipping address required (local shipping used)

        String encodedParameters = UtilHttp.urlEncodeArgs(parameters, false);
        String redirectString = redirectUrl + "?" + encodedParameters;

        // set the order in the session for cancelled orders
        request.getSession().setAttribute("PAYPAL_ORDER", orderId);

        // redirect to paypal
        try {
            response.sendRedirect(redirectString);
        } catch (IOException e) {
            Debug.logError(e, "Problems redirecting to PayPal", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsConnectingWithPayPal", locale));
            return "error";
        }

        return "success";
    }

    /** PayPal Call-Back Event 
     * @throws IOException */
    public static String payPalIPN(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore == null) {
            Debug.logError("ProductStore is null", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }

        // get the payment properties file
        GenericValue paymentConfig = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStore.getString("productStoreId"), "EXT_PAYPAL", null, true);

        String configString = null;
        String paymentGatewayConfigId = null;
        if (paymentConfig != null) {
            paymentGatewayConfigId = paymentConfig.getString("paymentGatewayConfigId");
            configString = paymentConfig.getString("paymentPropertiesPath");
        }

        if (configString == null) {
            configString = "payment.properties";
        }

        // get the confirm URL
        String confirmUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "confirmUrl", configString, "payment.paypal.confirm");

        // get the redirect URL
        String redirectUrl = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "redirectUrl", configString, "payment.paypal.redirect");

        if (UtilValidate.isEmpty(confirmUrl) || UtilValidate.isEmpty(redirectUrl)) {
            Debug.logError("Payment properties is not configured properly, no confirm URL defined!", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }

        // first verify this is valid from PayPal
        Map <String, Object> parametersMap = UtilHttp.getParameterMap(request);
        parametersMap.put("cmd", "_notify-validate");

        // send off the confirm request
        String confirmResp = null;
        String str = UtilHttp.urlEncodeArgs(parametersMap);
        URL u = new URL(redirectUrl);
        URLConnection uc = u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream(), "UTF-8"))) {

            pw.println(str);
            confirmResp = in.readLine();
            Debug.logError("PayPal Verification Response: " + confirmResp, module);
        } catch (IOException e) {
            Debug.logError(e, "Problems sending verification message.", module);
        }

        Debug.logInfo("Got verification from PayPal, processing..", module);
        boolean verified = false;
        for (String name : parametersMap.keySet()) {
            String value = request.getParameter(name);
            Debug.logError("### Param: " + name + " => " + value, module);
            if (UtilValidate.isNotEmpty(name) && "payer_status".equalsIgnoreCase(name) &&
                UtilValidate.isNotEmpty(value) && "verified".equalsIgnoreCase(value)) {
                verified = true;
            }
        }
        if (!verified) {
            Debug.logError("###### PayPal did not verify this request, need investigation!", module);
        }

        // get the system user
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get UserLogin for: system; cannot continue", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingAuthenticationUser", locale));
            return "error";
        }

        // get the orderId
        String orderId = request.getParameter("invoice");

        // get the order header
        GenericValue orderHeader = null;
        if (UtilValidate.isNotEmpty(orderId)) {
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingOrderHeader", locale));
                return "error";
            }
        } else {
            Debug.logError("PayPal did not callback with a valid orderId!", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.noValidOrderIdReturned", locale));
            return "error";
        }

        if (orderHeader == null) {
            Debug.logError("Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.problemsGettingOrderHeader", locale));
            return "error";
        }

        // get the transaction status
        String paymentStatus = request.getParameter("payment_status");

        // attempt to start a transaction
        boolean okay = true;
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            if ("Completed".equals(paymentStatus)) {
                okay = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            } else if ("Failed".equals(paymentStatus) || "Denied".equals(paymentStatus)) {
                okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
            }

            if (okay) {
                // set the payment preference
                okay = setPaymentPreferences(delegator, dispatcher, userLogin, orderId, request);
            }
        } catch (Exception e) {
            String errMsg = "Error handling PayPal notification";
            Debug.logError(e, errMsg, module);
            try {
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericTransactionException gte2) {
                Debug.logError(gte2, "Unable to rollback transaction", module);
            }
        } finally {
            if (!okay) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Failure in processing PayPal callback", null);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback transaction", module);
                }
            } else {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to commit transaction", module);
                }
            }
        }


        if (okay) {
            // call the email confirm service
            Map <String, String> emailContext = UtilMisc.toMap("orderId", orderId);
            try {
                dispatcher.runSync("sendOrderConfirmation", emailContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problems sending email confirmation", module);
            }
        }

        return "success";
    }

    /** Event called when customer cancels a paypal order */
    public static String cancelPayPalOrder(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        // get the stored order id from the session
        String orderId = (String) request.getSession().getAttribute("PAYPAL_ORDER");

        // attempt to start a transaction
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException gte) {
            Debug.logError(gte, "Unable to begin transaction", module);
        }

        // cancel the order
        boolean okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);

        if (okay) {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to commit transaction", module);
            }
        } else {
            try {
                TransactionUtil.rollback(beganTransaction, "Failure in processing PayPal cancel callback", null);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to rollback transaction", module);
            }
        }

        request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.previousPayPalOrderHasBeenCancelled", locale));
        return "success";
    }

    private static boolean setPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String orderId, HttpServletRequest request) {
        Debug.logVerbose("Setting payment prefrences..", module);
        List <GenericValue> paymentPrefs = null;
        try {
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get payment preferences for order #" + orderId, module);
            return false;
        }
        if (paymentPrefs.size() > 0) {
            for (GenericValue pref : paymentPrefs) {
                boolean okay = setPaymentPreference(dispatcher, userLogin, pref, request);
                if (!okay)
                    return false;
            }
        }
        return true;
    }

    private static boolean setPaymentPreference(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPreference, HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        String paymentDate = request.getParameter("payment_date");
        String paymentType = request.getParameter("payment_type");
        String paymentAmount = request.getParameter("mc_gross");
        String paymentStatus = request.getParameter("payment_status");
        String transactionId = request.getParameter("txn_id");

        List<GenericValue> toStore = new LinkedList<>();

        // PayPal returns the timestamp in the format 'hh:mm:ss Jan 1, 2000 PST'
        // Parse this into a valid Timestamp Object
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss MMM d, yyyy z");
        java.sql.Timestamp authDate = null;
        try {
            authDate = new java.sql.Timestamp(sdf.parse(paymentDate).getTime());
        } catch (ParseException e) {
            Debug.logError(e, "Cannot parse date string: " + paymentDate, module);
            authDate = UtilDateTime.nowTimestamp();
        } catch (NullPointerException e) {
            Debug.logError(e, "Cannot parse date string: " + paymentDate, module);
            authDate = UtilDateTime.nowTimestamp();
        }

        paymentPreference.set("maxAmount", new BigDecimal(paymentAmount));
        if ("Completed".equals(paymentStatus)) {
            paymentPreference.set("statusId", "PAYMENT_RECEIVED");
        } else if ("Pending".equals(paymentStatus)) {
            paymentPreference.set("statusId", "PAYMENT_NOT_RECEIVED");
        } else {
            paymentPreference.set("statusId", "PAYMENT_CANCELLED");
        }
        toStore.add(paymentPreference);


        Delegator delegator = paymentPreference.getDelegator();

        // create the PaymentGatewayResponse
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse");
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", "PRDS_PAY_EXTERNAL");
        response.set("orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPreference.get("paymentMethodTypeId"));
        response.set("paymentMethodId", paymentPreference.get("paymentMethodId"));

        // set the auth info
        response.set("amount", new BigDecimal(paymentAmount));
        response.set("referenceNum", transactionId);
        response.set("gatewayCode", paymentStatus);
        response.set("gatewayFlag", paymentStatus.substring(0,1));
        response.set("gatewayMessage", paymentType);
        response.set("transactionDate", authDate);
        toStore.add(response);

        try {
            delegator.storeAll(toStore);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot set payment preference/payment info", module);
            return false;
        }

        // create a payment record too
        Map <String, Object> results = null;
        try {
            String comment = UtilProperties.getMessage(resource, "AccountingPaymentReceiveViaPayPal", locale);
            results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin,
                    "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "comments", comment));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "payPalEvents.failedToExecuteServiceCreatePaymentFromPreference", locale));
            return false;
        }

        if (ServiceUtil.isError(results)) {
            Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
            request.setAttribute("_ERROR_MESSAGE_", results.get(ModelService.ERROR_MESSAGE));
            return false;
        }

        return true;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue payPal = EntityQuery.use(delegator).from("PaymentGatewayPayPal").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
                if (payPal != null) {
                    String payPalField = payPal.getString(paymentGatewayConfigParameterName);
                    if (payPalField != null) {
                        returnValue = payPalField.trim();
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
}
