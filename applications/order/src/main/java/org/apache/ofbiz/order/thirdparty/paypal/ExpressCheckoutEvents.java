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
package org.apache.ofbiz.order.thirdparty.paypal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


public class ExpressCheckoutEvents {

    private static final String RES_ERROR = "AccountingErrorUiLabels";
    private static final String MODULE = ExpressCheckoutEvents.class.getName();
    public enum CheckoutType { PAYFLOW, STANDARD, NONE }

    public static String setExpressCheckout(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        CheckoutType checkoutType = determineCheckoutType(request);
        if (!checkoutType.equals(CheckoutType.NONE)) {
            String serviceName = null;
            if (checkoutType.equals(CheckoutType.PAYFLOW)) {
                serviceName = "payflowSetExpressCheckout";
            } else if (checkoutType.equals(CheckoutType.STANDARD)) {
                serviceName = "payPalSetExpressCheckout";
            }
            Map<String, ? extends Object> inMap = UtilMisc.toMap("userLogin", cart.getUserLogin(), "cart", cart);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync(serviceName, inMap);
            } catch (GenericServiceException e) {
                Debug.logInfo(e, MODULE);
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "AccountingPayPalCommunicationError", locale));
                return "error";
            }
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
        }
        return "success";
    }

    public static String expressCheckoutRedirect(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        String paymentGatewayConfigId = null;
        GenericValue payPalGatewayConfig = null;
        String productStoreId = null;
        if (UtilValidate.isEmpty(token)) {
            Debug.logError("No ExpressCheckout token found in cart, you must do a successful setExpressCheckout before redirecting.", MODULE);
            return "error";
        }
        productStoreId = cart.getProductStoreId();
        if (productStoreId != null) {
            GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "EXT_PAYPAL", null, true);
            if (payPalPaymentSetting != null) {
                paymentGatewayConfigId = payPalPaymentSetting.getString("paymentGatewayConfigId");
            }
        }
        if (paymentGatewayConfigId != null) {
            try {
                payPalGatewayConfig = EntityQuery.use(delegator).from("PaymentGatewayPayPal").where("paymentGatewayConfigId",
                        paymentGatewayConfigId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (payPalGatewayConfig == null) {
            request.setAttribute("_EVENT_MESSAGE_", "Couldn't retrieve a PaymentGatewayConfigPayPal record for Express Checkout, cannot continue.");
            return "error";
        }
        StringBuilder redirectUrl = new StringBuilder(payPalGatewayConfig.getString("redirectUrl"));
        redirectUrl.append("?cmd=_express-checkout&token=");
        redirectUrl.append(token);
        try {
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return "error";
        }
        return "success";
    }

    public static String expressCheckoutUpdate(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        CheckoutType checkoutType = determineCheckoutType(request);
        if (checkoutType.equals(CheckoutType.STANDARD)) {
            Map<String, Object> inMap = new HashMap<>();
            inMap.put("request", request);
            inMap.put("response", response);
            try {
                Map<String, Object> result = dispatcher.runSync("payPalCheckoutUpdate", inMap);
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
        }
        return "success";
    }

    public static String getExpressCheckoutDetails(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        CheckoutType checkoutType = determineCheckoutType(request);
        if (!checkoutType.equals(CheckoutType.NONE)) {
            String serviceName = null;
            if (checkoutType.equals(CheckoutType.PAYFLOW)) {
                serviceName = "payflowGetExpressCheckout";
            } else if (checkoutType.equals(CheckoutType.STANDARD)) {
                serviceName = "payPalGetExpressCheckout";
            }
            Map<String, ? extends Object> inMap = UtilMisc.toMap("userLogin", cart.getUserLogin(), "cart", cart);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync(serviceName, inMap);
            } catch (GenericServiceException e) {
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "AccountingPayPalCommunicationError", locale));
                return "error";
            }
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
        }

        return "success";
    }

    public static Map<String, Object> doExpressCheckout(String productStoreId, String orderId, GenericValue paymentPref,
                                                        GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher) {
        CheckoutType checkoutType = determineCheckoutType(delegator, productStoreId);
        if (!checkoutType.equals(CheckoutType.NONE)) {
            String serviceName = null;
            if (checkoutType.equals(CheckoutType.PAYFLOW)) {
                serviceName = "payflowDoExpressCheckout";
            } else if (checkoutType.equals(CheckoutType.STANDARD)) {
                serviceName = "payPalDoExpressCheckout";
            }
            Map<String, Object> inMap = new HashMap<>();
            inMap.put("userLogin", userLogin);
            inMap.put("orderPaymentPreference", paymentPref);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync(serviceName, inMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            return result;
        }

        return ServiceUtil.returnSuccess();
    }

    public static String expressCheckoutCancel(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        cart.removeAttribute("payPalCheckoutToken");
        return "success";
    }

    public static CheckoutType determineCheckoutType(HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        return determineCheckoutType(delegator, cart.getProductStoreId());
    }

    public static CheckoutType determineCheckoutType(Delegator delegator, String productStoreId) {
        GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId,
                "EXT_PAYPAL", null, true);
        if (payPalPaymentSetting != null && payPalPaymentSetting.getString("paymentGatewayConfigId") != null) {
            try {
                GenericValue paymentGatewayConfig = payPalPaymentSetting.getRelatedOne("PaymentGatewayConfig", false);
                if (paymentGatewayConfig != null) {
                    String paymentGatewayConfigTypeId = paymentGatewayConfig.getString("paymentGatewayConfigTypeId");
                    if ("PAY_GATWY_PAYFLOWPRO".equals(paymentGatewayConfigTypeId)) {
                        return CheckoutType.PAYFLOW;
                    } else if ("PAY_GATWY_PAYPAL".equals(paymentGatewayConfigTypeId)) {
                        GenericValue payPalConfig = paymentGatewayConfig.getRelatedOne("PaymentGatewayPayPal", false);
                        // TODO: Probably better off with an indicator field to indicate Express Checkout use
                        if (UtilValidate.isNotEmpty(payPalConfig.get("apiUserName"))) {
                            return CheckoutType.STANDARD;
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return CheckoutType.NONE;
    }

}
