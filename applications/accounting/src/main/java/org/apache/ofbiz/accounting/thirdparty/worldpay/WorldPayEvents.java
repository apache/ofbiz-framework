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
package org.apache.ofbiz.accounting.thirdparty.worldpay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
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

/**
 * WorldPay Select Junior Integration Events/Services
 */
public class WorldPayEvents {

    private static final String RESOURCE = "AccountingUiLabels";
    private static final String RES_ERROR = "AccountingErrorUiLabels";
    public static final String COMMON_RES = "CommonUiLabels";
    private static final String MODULE = WorldPayEvents.class.getName();

    public static String worldPayRequest(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        // get the orderId from the request, stored by previous event(s)
        String orderId = (String) request.getAttribute("orderId");
        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for order: " + orderId, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingOrderHeader", locale));
            return "error";
        }
        // get the order total
        String orderTotal = orderHeader.getBigDecimal("grandTotal").toPlainString();
        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(request);
        if (productStore == null) {
            Debug.logError("ProductStore is null", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingMerchantConfiguration",
                    locale));
            return "error";
        }
        // get the payment properties file
        GenericValue paymentConfig = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStore.getString("productStoreId"),
                "EXT_WORLDPAY", null, true);
        String configString = null;
        String paymentGatewayConfigId = null;
        if (paymentConfig != null) {
            paymentGatewayConfigId = paymentConfig.getString("paymentGatewayConfigId");
            configString = paymentConfig.getString("paymentPropertiesPath");
        }
        if (configString == null) {
            configString = "payment.properties";
        }
        String redirectURL = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "redirectUrl",
                configString, "payment.worldpay.redirectUrl", "");
        String instId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "instId",
                configString, "payment.worldpay.instId", "NONE");
        String authMode = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "authMode",
                configString, "payment.worldpay.authMode", "A");
        String fixContact = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "fixContact",
                configString, "payment.worldpay.fixContact", "N");
        String hideContact = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hideContact",
                configString, "payment.worldpay.hideContact", "N");
        String hideCurrency = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hideCurrency",
                configString, "payment.worldpay.hideCurrency", "N");
        String langId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "langId",
                configString, "payment.worldpay.langId", "");
        String noLanguageMenu = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "noLanguageMenu",
                configString, "payment.worldpay.noLanguageMenu", "N");
        String withDelivery = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "withDelivery",
                configString, "payment.worldpay.withDelivery", "N");
        String testMode = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "testMode",
                configString, "payment.worldpay.testMode", "100");
        // get the contact address to pass over
        GenericValue contactAddress = null;
        GenericValue contactAddressShip = null;
        GenericValue addressOcm = null;
        GenericValue shippingAddress = null;
        try {
            addressOcm = EntityQuery.use(delegator).from("OrderContactMech").where("orderId", orderId,
                    "contactMechPurposeTypeId", "BILLING_LOCATION").queryFirst();
            shippingAddress = EntityQuery.use(delegator).from("OrderContactMech").where("orderId", orderId, "contactMechPurposeTypeId",
                    "SHIPPING_LOCATION").queryFirst();
            if (addressOcm == null) {
                addressOcm = shippingAddress;
            }
            contactAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId",
                    addressOcm.getString("contactMechId")).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order contact information", MODULE);
        }
        // get the country geoID
        GenericValue countryGeo = null;
        String country = "";
        if (contactAddress != null) {
            try {
                countryGeo = contactAddress.getRelatedOne("CountryGeo", false);
                if (countryGeo != null) {
                    country = countryGeo.getString("geoCode");
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Problems getting country geo entity", MODULE);
            }
        }
        // string of customer's name
        String name = "";
        if (contactAddress != null) {
            if (UtilValidate.isNotEmpty(contactAddress.getString("attnName"))) {
                name = contactAddress.getString("attnName");
            } else if (UtilValidate.isNotEmpty(contactAddress.getString("toName"))) {
                name = contactAddress.getString("toName");
            }
        }
        // build an address string
        StringBuilder address = new StringBuilder();
        String postalCode = "";
        if (contactAddress != null) {
            if (contactAddress.get("address1") != null) {
                address.append(contactAddress.getString("address1").trim());
            }
            if (contactAddress.get("address2") != null) {
                if (address.length() > 0) {
                    address.append("&#10;");
                }
                address.append(contactAddress.getString("address2").trim());
            }
            if (contactAddress.get("city") != null) {
                if (address.length() > 0) {
                    address.append("&#10;");
                }
                address.append(contactAddress.getString("city").trim());
            }
            if (contactAddress.get("stateProvinceGeoId") != null) {
                if (contactAddress.get("city") != null) {
                    address.append(", ");
                }
                address.append(contactAddress.getString("stateProvinceGeoId").trim());
            }
            if (contactAddress.get("postalCode") != null) {
                postalCode = contactAddress.getString("postalCode");
            }
        }
        // get the email address to pass over
        String emailAddress = null;
        GenericValue emailContact = null;
        try {
            GenericValue emailOcm = EntityQuery.use(delegator).from("OrderContactMech").where("orderId", orderId,
                    "contactMechPurposeTypeId", "ORDER_EMAIL").queryFirst();
            emailContact = emailOcm.getRelatedOne("ContactMech", false);
            emailAddress = emailContact.getString("infoString");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order email address", MODULE);
        }
        // build an shipping address string
        StringBuilder shipAddress = new StringBuilder();
        String shipPostalCode = "";
        String shipName = "";
        if (shippingAddress != null) {
            try {
                contactAddressShip = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingAddress.get("contactMechId"))
                        .queryOne();
                if (UtilValidate.isNotEmpty(contactAddressShip)) {
                    if (UtilValidate.isNotEmpty(contactAddressShip.getString("attnName"))) {
                        shipName = contactAddressShip.getString("attnName");
                    } else if (UtilValidate.isNotEmpty(contactAddressShip.getString("toName"))) {
                        shipName = contactAddressShip.getString("toName");
                    }
                    if (contactAddressShip.get("address1") != null) {
                        shipAddress.append(contactAddressShip.getString("address1").trim());
                    }
                    if (contactAddressShip.get("address2") != null) {
                        if (shipAddress.length() > 0) {
                            shipAddress.append("&#10;");
                        }
                        shipAddress.append(contactAddressShip.getString("address2").trim());
                    }
                    if (contactAddressShip.get("city") != null) {
                        if (shipAddress.length() > 0) {
                            shipAddress.append("&#10;");
                        }
                        shipAddress.append(contactAddressShip.getString("city").trim());
                    }
                    if (contactAddressShip.get("stateProvinceGeoId") != null) {
                        if (contactAddressShip.get("city") != null) {
                            shipAddress.append(", ");
                        }
                        shipAddress.append(contactAddressShip.getString("stateProvinceGeoId").trim());
                    }
                    if (contactAddressShip.get("postalCode") != null) {
                        shipPostalCode = contactAddressShip.getString("postalCode");
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Problems getting shipping address", MODULE);
            }
        }
        // get the company name
        String company = UtilFormatOut.checkEmpty(productStore.getString("companyName"), "");
        // get the currency
        String defCur = UtilFormatOut.checkEmpty(productStore.getString("defaultCurrencyUomId"), "USD");
        // order description
        String description = UtilProperties.getMessage(RESOURCE, "AccountingOrderNr", locale) + orderId + " "
                                 + (company != null ? UtilProperties.getMessage(COMMON_RES, "CommonFrom", locale) + " " + company : "");
        // check the instId - very important
        if (instId == null || "NONE".equals(instId)) {
            Debug.logError("Worldpay InstId not found, cannot continue", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingInstId", locale));
            return "error";
        }
        try {
            Integer.parseInt(instId);
        } catch (NumberFormatException nfe) {
            Debug.logError(nfe, "Problem converting instId string to integer", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingInstIdToInteger", locale));
            return "error";
        }
        // check the testMode
        if (testMode != null) {
            try {
                Integer.parseInt(testMode);
            } catch (NumberFormatException nfe) {
                Debug.logWarning(nfe, "Problems getting the testMode value, setting to 0", MODULE);
            }
        }
        // create the redirect string
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("instId", instId);
        parameters.put("cartId", orderId);
        parameters.put("currency", defCur);
        parameters.put("amount", orderTotal);
        parameters.put("desc", description);
        parameters.put("testMode", testMode);
        parameters.put("authMode", authMode);
        parameters.put("name", name);
        parameters.put("address", address.toString());
        parameters.put("country", country);
        parameters.put("postcode", postalCode);
        parameters.put("email", emailAddress);
        if (UtilValidate.isNotEmpty(shipName)) {
            parameters.put("M_shipping_name", shipName);
            if (UtilValidate.isNotEmpty(shipAddress.toString())) {
                parameters.put("M_shipping_address", shipAddress.toString());
            }
            if (UtilValidate.isNotEmpty(shipPostalCode)) {
                parameters.put("M_shipping_postcode", shipPostalCode);
            }
        }
        if ("Y".equals(fixContact)) {
            parameters.put("fixContact", "");
        }
        if ("Y".equals(hideContact)) {
            parameters.put("hideContact", "");
        }
        if ("Y".equals(hideCurrency)) {
            parameters.put("hideCurrency", "");
        }
        if ("Y".equals(noLanguageMenu)) {
            parameters.put("noLanguageMenu", "");
        }
        if ("Y".equals(withDelivery)) {
            parameters.put("withDelivery", "");
        }
        if (UtilValidate.isNotEmpty(langId)) {
            parameters.put("langId", langId);
        }
        // create the redirect URL
        String encodedParameters = UtilHttp.urlEncodeArgs(parameters, false);
        String redirectString = redirectURL + "?" + encodedParameters;
        // redirect to WorldPay
        try {
            response.sendRedirect(redirectString);
        } catch (IOException e) {
            Debug.logError(e, "Problems redirecting to WorldPay", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsConnectingWithWorldPay", locale));
            return "error";
        }
        return "success";
    }

    /** WorldPay notification */
    public static String worldPayNotify(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Map<String, Object> parametersMap = UtilHttp.getParameterMap(request);
        String orderId = request.getParameter("cartId");
        for (String name : parametersMap.keySet()) {
            String value = request.getParameter(name);
            Debug.logError("### Param: " + name + " => " + value, MODULE);
        }
        // get the user
        if (userLogin == null) {
            String userLoginId = "system";
            try {
                userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get UserLogin for: " + userLoginId + "; cannot continue", MODULE);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingAuthenticationUser",
                        locale));
                return "error";
            }
        }
        // get the order header
        GenericValue orderHeader = null;
        if (UtilValidate.isNotEmpty(orderId)) {
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get the order header for order: " + orderId, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingOrderHeader", locale));
                return "error";
            }
        } else {
            Debug.logError("WorldPay did not callback with a valid orderId!", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.noValidOrderIdReturned", locale));
            return "error";
        }
        if (orderHeader == null) {
            Debug.logError("Cannot get the order header for order: " + orderId, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "worldPayEvents.problemsGettingOrderHeader", locale));
            return "error";
        }
        // get the transaction status
        String paymentStatus = request.getParameter("transStatus");
        // attempt to start a transaction
        boolean okay = true;
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            // authorized
            if ("Y".equals(paymentStatus)) {
                okay = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            // cancelled
            } else if ("C".equals(paymentStatus)) {
                okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
            }
            if (okay) {
                // set the payment preference
                okay = setPaymentPreferences(delegator, dispatcher, userLogin, orderId, request);
            }
        } catch (Exception e) {
            String errMsg = "Error handling WorldPay notification";
            Debug.logError(e, errMsg, MODULE);
            try {
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericTransactionException gte2) {
                Debug.logError(gte2, "Unable to rollback transaction", MODULE);
            }
        } finally {
            if (!okay) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Failure in processing WorldPay callback", null);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback transaction", MODULE);
                }
            } else {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to commit transaction", MODULE);
                }
            }
        }
        if (okay) {
            // call the email confirm service
            Map<String, Object> emailContext = UtilMisc.toMap("orderId", orderId, "userLogin", userLogin);
            try {
                dispatcher.runSync("sendOrderConfirmation", emailContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problems sending email confirmation", MODULE);
            }
        }
        return "success";
    }

    private static boolean setPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String orderId,
                                                 HttpServletRequest request) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Setting payment preferences..", MODULE);
        }
        List<GenericValue> paymentPrefs = null;
        try {
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get payment preferences for order #" + orderId, MODULE);
            return false;
        }
        if (!paymentPrefs.isEmpty()) {
            for (GenericValue pref : paymentPrefs) {
                boolean okay = setPaymentPreference(dispatcher, userLogin, pref, request);
                if (!okay) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean setPaymentPreference(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPreference,
                                                HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        String paymentStatus = request.getParameter("transStatus");
        String paymentAmount = request.getParameter("authAmount");
        Long paymentDate = Long.valueOf(request.getParameter("transTime"));
        String transactionId = request.getParameter("transId");
        String gatewayFlag = request.getParameter("rawAuthCode");
        String avs = request.getParameter("AVS");
        List<GenericValue> toStore = new LinkedList<>();
        java.sql.Timestamp authDate = null;
        try {
            authDate = new java.sql.Timestamp(paymentDate);
        } catch (Exception e) {
            Debug.logError(e, "Cannot create date from long: " + paymentDate, MODULE);
            authDate = UtilDateTime.nowTimestamp();
        }
        paymentPreference.set("maxAmount", new BigDecimal(paymentAmount));
        if ("Y".equals(paymentStatus)) {
            paymentPreference.set("statusId", "PAYMENT_RECEIVED");
        } else if ("C".equals(paymentStatus)) {
            paymentPreference.set("statusId", "PAYMENT_CANCELLED");
        } else {
            paymentPreference.set("statusId", "PAYMENT_NOT_RECEIVED");
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
        response.set("gatewayFlag", gatewayFlag);
        response.set("transactionDate", authDate);
        response.set("gatewayAvsResult", avs);
        response.set("gatewayCvResult", avs.substring(0, 1));

        toStore.add(response);
        try {
            delegator.storeAll(toStore);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot set payment preference/payment info", MODULE);
            return false;
        }
        // create a payment record too
        Map<String, Object> results = null;
        try {
            String comment = UtilProperties.getMessage(RESOURCE, "AccountingPaymentReceiveViaWorldPay", locale);
            results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin,
                    "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "comments", comment));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to execute service createPaymentFromPreference", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR,
                    "worldPayEvents.failedToExecuteServiceCreatePaymentFromPreference", locale));
            return false;
        }

        if (ServiceUtil.isError(results)) {
            Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
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
                GenericValue worldPay = EntityQuery.use(delegator).from("PaymentGatewayWorldPay").where("paymentGatewayConfigId",
                        paymentGatewayConfigId).queryOne();
                if (UtilValidate.isNotEmpty(worldPay)) {
                    Object worldPayField = worldPay.get(paymentGatewayConfigParameterName);
                    if (worldPayField != null) {
                        returnValue = worldPayField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
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
        String returnValue = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, paymentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}
