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
package org.apache.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.marketing.tracking.TrackingCodeEvents;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * Events used for processing checkout and orders.
 */
public class CheckOutEvents {

    private static final String MODULE = CheckOutEvents.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";
    private static final String DEFAULT_INIT_CHECKOUT_PAGE = "shippingaddress";

    public static String cartNotEmpty(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);

        if (UtilValidate.isNotEmpty(cart.items())) {
            return "success";
        }
        String errMsg = UtilProperties.getMessage(RES_ERROR, "checkevents.cart_empty", cart.getLocale());
        request.setAttribute("_ERROR_MESSAGE_", errMsg);
        return "error";
    }

    public static String setCheckOutPages(HttpServletRequest request, HttpServletResponse response) {
        if ("error".equals(CheckOutEvents.cartNotEmpty(request, response))) {
            return "error";
        }

        HttpSession session = request.getSession();

        String curPage = request.getParameter("checkoutpage");
        Debug.logInfo("CheckoutPage: " + curPage, MODULE);

        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        GenericValue userLogin = cart.getUserLogin();
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("userLogin");
        }
        if (curPage == null) {
            try {
                Map<String, Object> createDropShipGroupResult = cart.createDropShipGroups(dispatcher);
                if ("error".equals(createDropShipGroupResult.get("responseMessage"))) {
                    Debug.logError((String) createDropShipGroupResult.get("errorMessage"), MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", (String) createDropShipGroupResult.get("errorMessage"));
                    return "error";
                }
            } catch (CartItemModifyException e) {
                Debug.logError(e, MODULE);
            }
        } else if ("shippingoptions".equals(curPage)) {
            //remove empty ship group
            cart.cleanUpShipGroups();
        }
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);

        if ("shippingaddress".equals(curPage)) {
            // Set the shipping address options
            String shippingContactMechId = request.getParameter("shipping_contact_mech_id");

            String taxAuthPartyGeoIds = request.getParameter("taxAuthPartyGeoIds");
            String partyTaxId = request.getParameter("partyTaxId");
            String isExempt = request.getParameter("isExempt");

            List<String> errorMessages = new ArrayList<>();
            Map<String, Object> errorMaps = new HashMap<>();
            for (int shipGroupIndex = 0; shipGroupIndex < cart.getShipGroupSize(); shipGroupIndex++) {
                // set the shipping method
                if (shippingContactMechId == null) {
                    shippingContactMechId = (String) request.getAttribute("contactMechId"); // FIXME
                }
                String supplierPartyId = (String) request.getAttribute(shipGroupIndex + "_supplierPartyId");
                String supplierAgreementId = (String) request.getAttribute(shipGroupIndex + "_supplierAgreementId");
                Map<String, ? extends Object> callResult = checkOutHelper.finalizeOrderEntryShip(shipGroupIndex, shippingContactMechId,
                        supplierPartyId, supplierAgreementId);
                ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
            }

            // if taxAuthPartyGeoIds is not empty drop that into the database
            if (UtilValidate.isNotEmpty(taxAuthPartyGeoIds)) {
                try {
                    Map<String, ? extends Object> createCustomerTaxAuthInfoResult = dispatcher.runSync("createCustomerTaxAuthInfo",
                            UtilMisc.<String, Object>toMap("partyId", cart.getPartyId(), "taxAuthPartyGeoIds", taxAuthPartyGeoIds, "partyTaxId",
                                    partyTaxId, "isExempt", isExempt, "userLogin", userLogin));
                    ServiceUtil.getMessages(request, createCustomerTaxAuthInfoResult, null);
                    if (ServiceUtil.isError(createCustomerTaxAuthInfoResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(createCustomerTaxAuthInfoResult);
                        request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                        Debug.logError(errorMessage, MODULE);
                        return "error";
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error setting customer tax info: " + e.toString();
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
            }

            Map<String, ? extends Object> callResult = checkOutHelper.setCheckOutShippingAddress(shippingContactMechId);
            ServiceUtil.getMessages(request, callResult, null);

            if (!(ServiceUtil.isError(callResult))) {
                // No errors so push the user onto the next page
                curPage = "shippingoptions";
            }
        } else if ("shippingoptions".equals(curPage)) {
            // Set the general shipping options
            String shippingMethod = request.getParameter("shipping_method");
            String shippingInstructions = request.getParameter("shipping_instructions");
            String orderAdditionalEmails = request.getParameter("order_additional_emails");
            String maySplit = request.getParameter("may_split");
            String giftMessage = request.getParameter("gift_message");
            String isGift = request.getParameter("is_gift");
            String internalCode = request.getParameter("internalCode");
            String shipBeforeDate = request.getParameter("shipBeforeDate");
            String shipAfterDate = request.getParameter("shipAfterDate");
            Map<String, ? extends Object> callResult = ServiceUtil.returnSuccess();

            for (int shipGroupIndex = 0; shipGroupIndex < cart.getShipGroupSize(); shipGroupIndex++) {
                callResult = checkOutHelper.finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit, giftMessage,
                        isGift, internalCode, shipBeforeDate, shipAfterDate, orderAdditionalEmails);
                ServiceUtil.getMessages(request, callResult, null);
            }
            if (!(callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                // No errors so push the user onto the next page
                curPage = "payment";
            }
        } else if ("payment".equals(curPage)) {
            // Set the payment options
            Map<String, Map<String, Object>> selectedPaymentMethods = getSelectedPaymentMethods(request);

            String billingAccountId = request.getParameter("billingAccountId");
            if (UtilValidate.isNotEmpty(billingAccountId)) {
                BigDecimal billingAccountAmt = null;
                billingAccountAmt = determineBillingAccountAmount(billingAccountId, request.getParameter("billingAccountAmount"), dispatcher);
                if ((billingAccountId != null) && !"_NA_".equals(billingAccountId) && (billingAccountAmt == null)) {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "OrderInvalidAmountSetForBillingAccount",
                            UtilMisc.toMap("billingAccountId", billingAccountId), cart.getLocale()));
                    return "error";
                }
                selectedPaymentMethods.put("EXT_BILLACT", UtilMisc.<String, Object>toMap("amount", billingAccountAmt, "securityCode", null));
            }

            if (UtilValidate.isEmpty(selectedPaymentMethods)) {
                return "error";
            }

            List<String> singleUsePayments = new ArrayList<>();

            // check for gift card not on file
            Map<String, Object> params = UtilHttp.getParameterMap(request);
            Map<String, Object> gcResult = checkOutHelper.checkGiftCard(params, selectedPaymentMethods);
            ServiceUtil.getMessages(request, gcResult, null);
            if (gcResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return "error";
            }
            String gcPaymentMethodId = (String) gcResult.get("paymentMethodId");
            BigDecimal gcAmount = (BigDecimal) gcResult.get("amount");
            if (gcPaymentMethodId != null) {
                selectedPaymentMethods.put(gcPaymentMethodId, UtilMisc.<String, Object>toMap("amount", gcAmount,
                        "securityCode", null));
                if ("Y".equalsIgnoreCase(request.getParameter("singleUseGiftCard"))) {
                    singleUsePayments.add(gcPaymentMethodId);
                }
            }

            Map<String, Object> callResult = checkOutHelper.setCheckOutPayment(selectedPaymentMethods, singleUsePayments, billingAccountId);
            ServiceUtil.getMessages(request, callResult, null);

            if (!(callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                // No errors so push the user onto the next page
                curPage = "confirm";
            }
        } else {
            curPage = determineInitialCheckOutPage(cart);
        }

        return curPage;
    }

    /**
     * Method to determine the initial checkout page based on requirements. This will also set
     * any cart variables necessary to satisfy the requirements, such as setting the
     * shipment method according to the type of items in the cart.
     */
    public static String determineInitialCheckOutPage(ShoppingCart cart) {
        String page = DEFAULT_INIT_CHECKOUT_PAGE;
        if (cart == null) {
            return page;
        }

        // if no shipping applies, set the no shipment method and skip to payment
        if (!cart.shippingApplies()) {
            cart.setAllShipmentMethodTypeId("NO_SHIPPING");
            cart.setAllCarrierPartyId("_NA_");
            page = "payment";
        }

        return page;
    }

    public static String setCheckOutError(HttpServletRequest request, HttpServletResponse response) {
        String currentPage = request.getParameter("checkoutpage");
        if (UtilValidate.isEmpty(currentPage)) {
            return "error";
        }
        return currentPage;
    }

    /**
     * Use for quickcheckout submit.  It calculates the tax before setting the payment options.
     * Shipment option should already be set by the quickcheckout form.
     */
    public static String setQuickCheckOutOptions(HttpServletRequest request, HttpServletResponse response) {
        String result = calcTax(request, response);
        if ("error".equals(result)) {
            return "error";
        }
        return setCheckOutOptions(request, response);
    }

    public static String setPartialCheckOutOptions(HttpServletRequest request, HttpServletResponse response) {
        // FIXME response need to be checked ?
        setCheckOutOptions(request, response);
        request.setAttribute("_ERROR_MESSAGE_", null);
        return "success";
    }

    public static String setCartShipToCustomerParty(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        String shipToCustomerPartyId = request.getParameter("shipToCustomerPartyId");
        cart.setShipToCustomerPartyId(shipToCustomerPartyId);
        cart.setAllShippingContactMechId(null);
        return "success";
    }

    public static String checkPaymentMethods(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> resp = checkOutHelper.validatePaymentMethods();
        if (ServiceUtil.isError(resp)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(resp));
            return "error";
        }
        return "success";
    }

    public static Map<String, Map<String, Object>> getSelectedPaymentMethods(HttpServletRequest request) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        Map<String, Map<String, Object>> selectedPaymentMethods = new HashMap<>();
        String[] paymentMethods = request.getParameterValues("checkOutPaymentId");

        String checkOutPaymentId = (String) request.getAttribute("checkOutPaymentId");
        if ((paymentMethods == null || paymentMethods.length <= 0) && UtilValidate.isNotEmpty(checkOutPaymentId)) {
            paymentMethods = new String[]{checkOutPaymentId};
        }

        if (UtilValidate.isNotEmpty(request.getParameter("issuerId"))) {
            request.setAttribute("issuerId", request.getParameter("issuerId"));
        }

        String errMsg = null;

        if (paymentMethods != null) {
            for (String paymentMethod : paymentMethods) {
                Map<String, Object> paymentMethodInfo = new HashMap<>();

                String securityCode = request.getParameter("securityCode_" + paymentMethod);
                if (UtilValidate.isNotEmpty(securityCode)) {
                    paymentMethodInfo.put("securityCode", securityCode);
                }
                String paymentRefNumber = request.getParameter("paymentRefNumber");
                if (UtilValidate.isNotEmpty(paymentRefNumber)) {
                    paymentMethodInfo.put("refNum", paymentRefNumber);
                }
                String amountStr = request.getParameter("amount_" + paymentMethod);
                BigDecimal amount = null;
                if (UtilValidate.isNotEmpty(amountStr) && !"REMAINING".equals(amountStr)) {
                    try {
                        amount = new BigDecimal(amountStr);
                    } catch (NumberFormatException e) {
                        Debug.logError(e, MODULE);
                        errMsg = UtilProperties.getMessage(RES_ERROR, "checkevents.invalid_amount_set_for_payment_method", (cart != null
                                ? cart.getLocale() : Locale.getDefault()));
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        return null;
                    }
                }
                paymentMethodInfo.put("amount", amount);
                selectedPaymentMethods.put(paymentMethod, paymentMethodInfo);
            }
        }
        Debug.logInfo("Selected Payment Methods : " + selectedPaymentMethods, MODULE);
        return selectedPaymentMethods;
    }

    // this servlet is used by quick checkout
    public static String setCheckOutOptions(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // Set the payment options
        Map<String, Map<String, Object>> selectedPaymentMethods = getSelectedPaymentMethods(request);

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);

        // get the billing account and amount
        String billingAccountId = request.getParameter("billingAccountId");
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            BigDecimal billingAccountAmt = null;
            billingAccountAmt = determineBillingAccountAmount(billingAccountId, request.getParameter("billingAccountAmount"), dispatcher);
            if (billingAccountAmt == null) {
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "OrderInvalidAmountSetForBillingAccount",
                        UtilMisc.toMap("billingAccountId", billingAccountId), (cart != null ? cart.getLocale() : Locale.getDefault())));
                return "error";
            }
            selectedPaymentMethods.put("EXT_BILLACT", UtilMisc.<String, Object>toMap("amount", billingAccountAmt, "securityCode", null));
        }

        if (selectedPaymentMethods == null) {
            return "error";
        }

        String shippingMethod = request.getParameter("shipping_method");
        String shippingContactMechId = request.getParameter("shipping_contact_mech_id");

        String taxAuthPartyGeoIds = request.getParameter("taxAuthPartyGeoIds");
        String partyTaxId = request.getParameter("partyTaxId");
        String isExempt = request.getParameter("isExempt");

        String shippingInstructions = request.getParameter("shipping_instructions");
        String orderAdditionalEmails = request.getParameter("order_additional_emails");
        String maySplit = request.getParameter("may_split");
        String giftMessage = request.getParameter("gift_message");
        String isGift = request.getParameter("is_gift");
        String internalCode = request.getParameter("internalCode");
        String shipBeforeDate = request.getParameter("shipBeforeDate");
        String shipAfterDate = request.getParameter("shipAfterDate");

        List<String> singleUsePayments = new ArrayList<>();

        // get a request map of parameters
        Map<String, Object> params = UtilHttp.getParameterMap(request);

        // if taxAuthPartyGeoIds is not empty drop that into the database
        if (UtilValidate.isNotEmpty(taxAuthPartyGeoIds)) {
            try {
                Map<String, Object> createCustomerTaxAuthInfoResult = dispatcher.runSync("createCustomerTaxAuthInfo",
                        UtilMisc.toMap("partyId", cart.getPartyId(), "taxAuthPartyGeoIds", taxAuthPartyGeoIds, "partyTaxId",
                                partyTaxId, "isExempt", isExempt));
                ServiceUtil.getMessages(request, createCustomerTaxAuthInfoResult, null);
                if (ServiceUtil.isError(createCustomerTaxAuthInfoResult)) {
                    String errorMessage = ServiceUtil.getErrorMessage(createCustomerTaxAuthInfoResult);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error setting customer tax info: " + e.toString();
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }

        // check for gift card not on file
        Map<String, Object> gcResult = checkOutHelper.checkGiftCard(params, selectedPaymentMethods);
        ServiceUtil.getMessages(request, gcResult, null);
        if (ServiceUtil.isError(gcResult)) {
            return "error";
        }

        String gcPaymentMethodId = (String) gcResult.get("paymentMethodId");
        BigDecimal gcAmount = (BigDecimal) gcResult.get("amount");
        if (gcPaymentMethodId != null) {
            selectedPaymentMethods.put(gcPaymentMethodId, UtilMisc.<String, Object>toMap("amount", gcAmount, "securityCode", null));
            if ("Y".equalsIgnoreCase(request.getParameter("singleUseGiftCard"))) {
                singleUsePayments.add(gcPaymentMethodId);
            }
        }

        Map<String, Object> optResult = checkOutHelper.setCheckOutOptions(shippingMethod, shippingContactMechId, selectedPaymentMethods,
                singleUsePayments, billingAccountId, shippingInstructions,
                orderAdditionalEmails, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate);

        ServiceUtil.getMessages(request, optResult, null);
        if (ServiceUtil.isError(optResult)) {
            return "error";
        }

        return "success";
    }

    // Check for payment method and shipping method exist for checkout process of anonymous user
    public static String checkoutValidation(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        if (cart.isSalesOrder()) {
            List<GenericValue> paymentMethodTypes = cart.getPaymentMethodTypes();
            if (UtilValidate.isEmpty(paymentMethodTypes)) {
                String errMsg = UtilProperties.getMessage(RES_ERROR, "OrderNoPaymentMethodTypeSelected",
                        cart.getLocale());
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            String shipmentMethod = cart.getShipmentMethodTypeId();
            if (UtilValidate.isEmpty(shipmentMethod)) {
                String errMsg = UtilProperties.getMessage(RES_ERROR, "OrderNoShipmentMethodSelected",
                        cart.getLocale());
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }
        return "success";
    }

    // Create order event - uses createOrder service for processing
    public static String createOrder(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> callResult;
        String result = checkoutValidation(request, response);
        if ("error".equals(result)) {
            return "error";
        }

        if (UtilValidate.isEmpty(userLogin)) {
            userLogin = cart.getUserLogin();
            session.setAttribute("userLogin", userLogin);
        }
        // remove this whenever creating an order so quick reorder cache will refresh/recalc
        session.removeAttribute("_QUICK_REORDER_PRODUCTS_");

        boolean areOrderItemsExploded = explodeOrderItems(delegator, cart);

        //get the TrackingCodeOrder List
        List<GenericValue> trackingCodeOrders = TrackingCodeEvents.makeTrackingCodeOrders(request);
        String distributorId = (String) session.getAttribute("_DISTRIBUTOR_ID_");
        String affiliateId = (String) session.getAttribute("_AFFILIATE_ID_");
        String visitId = VisitHandler.getVisitId(session);
        String webSiteId = WebSiteWorker.getWebSiteId(request);

        callResult = checkOutHelper.createOrder(userLogin, distributorId, affiliateId, trackingCodeOrders, areOrderItemsExploded, visitId, webSiteId);
        if (callResult != null) {
            ServiceUtil.getMessages(request, callResult, null);
            if (ServiceUtil.isError(callResult)) {
                // messages already setup with the getMessages call, just return the error response code
                return "error";
            }
            if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                // set the orderId for use by chained events
                String orderId = cart.getOrderId();
                request.setAttribute("orderId", orderId);
                request.setAttribute("orderAdditionalEmails", cart.getOrderAdditionalEmails());
            }
        }

        String issuerId = request.getParameter("issuerId");
        if (UtilValidate.isNotEmpty(issuerId)) {
            request.setAttribute("issuerId", issuerId);
        }


        return cart.getOrderType().toLowerCase(Locale.getDefault());
    }

    // Event wrapper for the tax calc.
    public static String calcTax(HttpServletRequest request, HttpServletResponse response) {
        try {
            calcTax(request);
        } catch (GeneralException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    // Invoke the taxCalc
    private static void calcTax(HttpServletRequest request) throws GeneralException {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);

        //Calculate and add the tax adjustments
        checkOutHelper.calcAndAddTax();
    }

    public static boolean explodeOrderItems(Delegator delegator, ShoppingCart cart) {
        if (cart == null) {
            return false;
        }
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        return !(productStore == null || productStore.get("explodeOrderItems") == null)
                && productStore.getBoolean("explodeOrderItems");
    }

    public static String checkShipmentNeeded(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", cart.getProductStoreId()).cache().queryOne();
            Debug.logInfo("checkShipmentNeeded: reqShipAddrForDigItems=" + productStore.getString("reqShipAddrForDigItems"), MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting ProductStore: " + e.toString(), MODULE);
        }

        if (productStore != null && "N".equals(productStore.getString("reqShipAddrForDigItems"))) {
            Debug.logInfo("checkShipmentNeeded: cart.containOnlyDigitalGoods()=" + cart.containOnlyDigitalGoods(), MODULE);
            // don't require shipping for all digital items
            if (cart.containOnlyDigitalGoods()) {
                return "shipmentNotNeeded";
            }
        }

        return "shipmentNeeded";
    }

    // Event wrapper for processPayment.
    public static String processPayment(HttpServletRequest request, HttpServletResponse response) {
        // run the process payment process + approve order when complete; may also run sync fulfillments
        int failureCode = 0;
        try {
            if (!processPayment(request)) {
                failureCode = 1;
            }
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            ServiceUtil.setMessages(request, e.getMessage(), null, null);
            failureCode = 2;
        } catch (GeneralRuntimeException e) {
            Debug.logError(e, MODULE);
            ServiceUtil.setMessages(request, e.getMessage(), null, null);
        }

        // event return based on failureCode
        switch (failureCode) {
        case 0:
            return "success";
        case 1:
            return "fail";
        default:
            return "error";
        }
    }

    private static boolean processPayment(HttpServletRequest request) throws GeneralException {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);

        // check if the order is to be held (processing)
        boolean holdOrder = cart.getHoldOrder();

        // load the ProductStore settings
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        Map<String, Object> callResult = checkOutHelper.processPayment(productStore, userLogin, false, holdOrder);

        if (ServiceUtil.isError(callResult)) {
            // clear out the rejected payment methods (if any) from the cart, so they don't get re-authorized
            cart.clearDeclinedPaymentMethods(delegator);
            // null out the orderId for next pass
            cart.setOrderId(null);
        }

        // generate any messages required
        ServiceUtil.getMessages(request, callResult, null);

        // check for customer message(s)
        List<String> messages = UtilGenerics.cast(callResult.get("authResultMsgs"));
        if (UtilValidate.isNotEmpty(messages)) {
            request.setAttribute("_EVENT_MESSAGE_LIST_", messages);
        }

        // determine whether it was a success or failure
        return (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS));
    }

    public static String checkOrderDenylist(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        CheckOutHelper checkOutHelper = new CheckOutHelper(null, delegator, cart);
        String result;

        Map<String, Object> callResult = checkOutHelper.checkOrderDenyList();
        if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
            request.setAttribute("_ERROR_MESSAGE_", callResult.get(ModelService.ERROR_MESSAGE));
            result = "error";
        } else if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_FAIL)) {
            request.setAttribute("_ERROR_MESSAGE_", callResult.get(ModelService.ERROR_MESSAGE));
            result = "failed";
        } else {
            result = (String) callResult.get(ModelService.SUCCESS_MESSAGE);
        }

        return result;
    }

    public static String failedDenylistCheck(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String orderPartyId = cart.getOrderPartyId();
        GenericValue userLogin = PartyWorker.findPartyLatestUserLogin(orderPartyId, delegator);
        GenericValue currentUser = (GenericValue) session.getAttribute("userLogin");
        String result;

        // Load the properties store
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> callResult = checkOutHelper.failedDenylistCheck(userLogin, productStore);

        //Generate any messages required
        ServiceUtil.getMessages(request, callResult, null);

        // wipe the session
        if (("anonymous".equals(currentUser.getString("userLoginId"))) || (currentUser.getString("userLoginId"))
                .equals(userLogin.getString("userLoginId"))) {
            session.invalidate();
        }
        //Determine whether it was a success or not
        if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
            result = (String) callResult.get(ModelService.ERROR_MESSAGE);
            request.setAttribute("_ERROR_MESSAGE_", result);
            result = "error";
        } else {
            result = (String) callResult.get(ModelService.ERROR_MESSAGE);
            request.setAttribute("_ERROR_MESSAGE_", result);
            result = "success";
        }
        return result;
    }

    public static String checkExternalCheckout(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        String paymentMethodTypeId = request.getParameter("paymentMethodTypeId");
        if ("EXT_PAYPAL".equals(paymentMethodTypeId) || cart.getPaymentMethodTypeIds().contains("EXT_PAYPAL")) {
            try {
                GenericValue payPalProdStorePaySetting = EntityQuery.use(delegator).from("ProductStorePaymentSetting").where("productStoreId",
                        productStore.getString("productStoreId"), "paymentMethodTypeId", "EXT_PAYPAL").queryFirst();
                if (payPalProdStorePaySetting != null) {
                    GenericValue gatewayConfig = payPalProdStorePaySetting.getRelatedOne("PaymentGatewayConfig", false);
                    if (gatewayConfig != null && "PAY_GATWY_PAYFLOWPRO".equals(gatewayConfig.getString("paymentGatewayConfigTypeId"))) {
                        return "paypal";
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return "success";
    }

    public static String checkExternalPayment(HttpServletRequest request, HttpServletResponse response) {
        // warning there can only be ONE payment preference for this to work
        // you cannot accept multiple payment type when using an external gateway
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String result;

        String orderId = (String) request.getAttribute("orderId");
        CheckOutHelper checkOutHelper = new CheckOutHelper(null, delegator, null);
        Map<String, Object> callResult = checkOutHelper.checkExternalPayment(orderId);

        //Generate any messages required
        ServiceUtil.getMessages(request, callResult, null);

        // any error messages have prepared for display, return the type ('error' if failed)
        result = (String) callResult.get("type");
        return result;
    }

    public static String finalizeOrderEntry(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String shippingContactMechId = null;
        String shippingMethod = null;
        BigDecimal shipEstimate = null;
        String shippingInstructions = null;
        String maySplit = null;
        String giftMessage = null;
        String isGift = null;
        String internalCode = null;
        String methodType = null;
        String shipBeforeDate = null;
        String shipAfterDate = null;
        String internalOrderNotes = null;
        String shippingNotes = null;
        String shipToPartyId = null;

        String mode = request.getParameter("finalizeMode");
        Debug.logInfo("FinalizeMode: " + mode, MODULE);
        // necessary to avoid infinite looping when in a funny state, and will go right back to beginning
        if (mode == null) {
            return "customer";
        }

        // check the userLogin object
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        // if null then we must be an anonymous shopper
        if (userLogin == null) {
            // remove auto-login fields
            request.getSession().removeAttribute("autoUserLogin");
            request.getSession().removeAttribute("autoName");
            // clear out the login fields from the cart
            try {
                cart.setAutoUserLogin(null, dispatcher);
            } catch (CartItemModifyException e) {
                Debug.logError(e, MODULE);
            }
        }

        // Reassign items requiring drop-shipping to new or existing drop-ship groups
        if ("init".equals(mode) || "default".equals(mode)) {
            try {
                Map<String, Object> createDropShipGroupResult = cart.createDropShipGroups(dispatcher);
                if ("error".equals(createDropShipGroupResult.get("responseMessage"))) {
                    Debug.logError((String) createDropShipGroupResult.get("errorMessage"), MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", (String) createDropShipGroupResult.get("errorMessage"));
                    return "error";
                }
            } catch (CartItemModifyException e) {
                Debug.logError(e, MODULE);
            }
        }

        // set the customer info
        if ("default".equals(mode)) {
            cart.setDefaultCheckoutOptions(dispatcher);
        }

        // remove the empty ship groups
        if ("removeEmptyShipGroups".equals(mode)) {
            cart.cleanUpShipGroups();
        }

        // set the customer info
        if ("cust".equals(mode)) {
            String partyId = (String) request.getAttribute("partyId");
            if (partyId != null) {
                cart.setOrderPartyId(partyId);
                // no userLogin means we are an anonymous shopper; fake the UL for service calls
                if (userLogin == null) {
                    try {
                        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "anonymous").queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (userLogin != null) {
                        userLogin.set("partyId", partyId);
                    }
                    request.getSession().setAttribute("userLogin", userLogin);
                    try {
                        cart.setUserLogin(userLogin, dispatcher);
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                    }
                    Debug.logInfo("Anonymous user-login has been activated", MODULE);
                }
            }
        }

        if ("addpty".equals(mode)) {
            cart.setAttribute("addpty", "Y");
        }

        if ("term".equals(mode)) {
            cart.setOrderTermSet(true);
        }

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);

        // ====================================================================================
        if ("ship".equals(mode) || "options".equals(mode)) {
            Map<String, Object> callResult = ServiceUtil.returnSuccess();
            List<String> errorMessages = new ArrayList<>();
            Map<String, Object> errorMaps = new HashMap<>();
            for (int shipGroupIndex = 0; shipGroupIndex < cart.getShipGroupSize(); shipGroupIndex++) {
                // set the shipping method
                if ("ship".equals(mode)) {
                    shippingContactMechId = request.getParameter(shipGroupIndex + "_shipping_contact_mech_id");
                    String facilityId = request.getParameter(shipGroupIndex + "_shipGroupFacilityId");
                    if (shippingContactMechId == null) {
                        shippingContactMechId = (String) request.getAttribute("contactMechId");
                    } else if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
                        String[] shipInfo = shippingContactMechId.split("_@_");
                        if (shipInfo.length > 1) {
                            shippingContactMechId = shipInfo[0];
                            facilityId = shipInfo[1];
                        }
                    }
                    String supplierPartyId = request.getParameter(shipGroupIndex + "_supplierPartyId");
                    String supplierAgreementId = request.getParameter(shipGroupIndex + "_supplierAgreementId");
                    if (UtilValidate.isNotEmpty(facilityId)) {
                        cart.setShipGroupFacilityId(shipGroupIndex, facilityId);
                    }
                    // If shipTo party is different than order party
                    shipToPartyId = request.getParameter("shipToPartyId");
                    if (UtilValidate.isNotEmpty(shipToPartyId)) {
                        cart.setShipToCustomerPartyId(shipToPartyId);
                    } else {
                        cart.setShipToCustomerPartyId(request.getParameter("orderPartyId"));
                    }
                    callResult = checkOutHelper.finalizeOrderEntryShip(shipGroupIndex, shippingContactMechId, supplierPartyId, supplierAgreementId);
                    ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
                }
                // set the options
                if ("options".equals(mode)) {
                    shippingMethod = request.getParameter(shipGroupIndex + "_shipping_method");
                    if (UtilValidate.isEmpty(shippingMethod)) {
                        shippingMethod = request.getParameter("shipping_method");
                    }
                    shippingInstructions = request.getParameter(shipGroupIndex + "_shipping_instructions");
                    if (UtilValidate.isEmpty(shippingInstructions)) {
                        shippingInstructions = request.getParameter("shipping_instructions");
                    }
                    maySplit = request.getParameter(shipGroupIndex + "_may_split");
                    if (UtilValidate.isEmpty(maySplit)) {
                        maySplit = request.getParameter("may_split");
                    }
                    giftMessage = request.getParameter(shipGroupIndex + "_gift_message");
                    isGift = request.getParameter(shipGroupIndex + "_is_gift");
                    internalCode = request.getParameter("internalCode"); // FIXME
                    shipBeforeDate = request.getParameter("sgi" + shipGroupIndex + "_shipBeforeDate");
                    shipAfterDate = request.getParameter("sgi" + shipGroupIndex + "_shipAfterDate");
                    internalOrderNotes = request.getParameter("internal_order_notes");
                    shippingNotes = request.getParameter("shippingNotes");
                    if (UtilValidate.isNotEmpty(request.getParameter(shipGroupIndex + "_ship_estimate"))) {
                        shipEstimate = new BigDecimal(request.getParameter(shipGroupIndex + "_ship_estimate"));
                    }
                    cart.clearOrderNotes();
                    cart.clearInternalOrderNotes();
                    if (shipEstimate == null) {  // allow ship estimate to be set manually if a purchase order
                        callResult = checkOutHelper.finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit,
                                giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate, internalOrderNotes, shippingNotes);
                    } else {
                        callResult = checkOutHelper.finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit,
                                giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate, internalOrderNotes, shippingNotes, shipEstimate);
                    }
                    ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
                }
            }
            //See whether we need to return an error or not
            callResult = ServiceUtil.returnSuccess();
            if (!errorMessages.isEmpty()) {
                callResult.put(ModelService.ERROR_MESSAGE_LIST, errorMessages);
                callResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            }
            if (!errorMaps.isEmpty()) {
                callResult.put(ModelService.ERROR_MESSAGE_MAP, errorMaps);
                callResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            }
            // generate any messages required
            ServiceUtil.getMessages(request, callResult, null);
            // determine whether it was a success or not
            if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                if ("ship".equals(mode)) {
                    return "shipping";
                }
                if ("options".equals(mode)) {
                    return "options";
                }
                return "error";
            }
        }
        // ###############################################################################

        // check for offline payment type
        // payment option; if offline we skip the payment screen
        methodType = request.getParameter("paymentMethodType");
        if ("offline".equals(methodType)) {
            Debug.logInfo("Changing mode from->to: " + mode + "->payment", MODULE);
            mode = "payment";
        }

        if ("payment".equals(mode)) {
            Map<String, Object> callResult = ServiceUtil.returnSuccess();
            List<String> errorMessages = new ArrayList<>();
            Map<String, Object> errorMaps = new HashMap<>();

            // Set the payment options
            Map<String, Map<String, Object>> selectedPaymentMethods = getSelectedPaymentMethods(request);

            // Set the billing account (if any)
            String billingAccountId = request.getParameter("billingAccountId");
            if (UtilValidate.isNotEmpty(billingAccountId)) {
                BigDecimal billingAccountAmt = null;
                billingAccountAmt = determineBillingAccountAmount(billingAccountId, request.getParameter("billingAccountAmount"), dispatcher);
                if (billingAccountAmt == null) {
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "OrderInvalidAmountSetForBillingAccount",
                            UtilMisc.toMap("billingAccountId", billingAccountId), (cart != null ? cart.getLocale() : Locale.getDefault())));
                    return "error";
                }
                selectedPaymentMethods.put("EXT_BILLACT", UtilMisc.<String, Object>toMap("amount", billingAccountAmt, "securityCode", null));
            }

            // If the user has just created a new payment method, add it to the map with a null amount, so that
            //  it becomes the sole payment method for the order.
            String newPaymentMethodId = (String) request.getAttribute("paymentMethodId");
            if (!UtilValidate.isEmpty(newPaymentMethodId)) {
                selectedPaymentMethods.put(newPaymentMethodId, null);
                if (!selectedPaymentMethods.containsKey(newPaymentMethodId)) {
                    selectedPaymentMethods.put(newPaymentMethodId, UtilMisc.toMap("amount", null, "securityCode", null));
                }
            }

            // The selected payment methods are set
            errorMessages.addAll(checkOutHelper.setCheckOutPaymentInternal(selectedPaymentMethods, null, billingAccountId));
            // Verify if a gift card has been selected during order entry
            callResult = checkOutHelper.checkGiftCard(paramMap, selectedPaymentMethods);
            ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
            if (errorMessages.isEmpty() && errorMaps.isEmpty()) {
                String gcPaymentMethodId = (String) callResult.get("paymentMethodId");
                BigDecimal giftCardAmount = (BigDecimal) callResult.get("amount");
                // WARNING: if gcPaymentMethodId is not empty, all the previously set payment methods will be removed
                Map<String, Object> gcCallRes = checkOutHelper.finalizeOrderEntryPayment(gcPaymentMethodId, giftCardAmount, true, true);
                ServiceUtil.addErrors(errorMessages, errorMaps, gcCallRes);
            }
            //See whether we need to return an error or not
            callResult = ServiceUtil.returnSuccess();
            if (!errorMessages.isEmpty()) {
                callResult.put(ModelService.ERROR_MESSAGE_LIST, errorMessages);
                callResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            }
            if (!errorMaps.isEmpty()) {
                callResult.put(ModelService.ERROR_MESSAGE_MAP, errorMaps);
                callResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            }
            // generate any messages required
            ServiceUtil.getMessages(request, callResult, null);
            // determine whether it was a success or not
            if (callResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return "paymentError";
            }
        }
        // determine where to direct the browser
        return determineNextFinalizeStep(request, response);
    }

    public static String determineNextFinalizeStep(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        // flag anoymous checkout to bypass additional party settings
        boolean isAnonymousCheckout = false;
        if (userLogin != null && "anonymous".equals(userLogin.getString("userLoginId"))) {
            isAnonymousCheckout = true;
        }

        // determine where to direct the browser
        // these are the default values
        boolean requireCustomer = true;
        boolean requireNewShippingAddress = false;
        boolean requireShipping = true;
        boolean requireOptions = true;
        boolean requireShipGroups = false;
        boolean requirePayment = !"PURCHASE_ORDER".equals(cart.getOrderType());
        boolean requireTerm = true;
        boolean requireAdditionalParty = isAnonymousCheckout;
        boolean isSingleUsePayment = true;
        // these options are not available to anonymous shoppers (security)
        if (userLogin != null && !"anonymous".equals(userLogin.getString("userLoginId"))) {
            String requireCustomerStr = request.getParameter("finalizeReqCustInfo");
            String requireNewShippingAddressStr = request.getParameter("finalizeReqNewShipAddress");
            String requireShippingStr = request.getParameter("finalizeReqShipInfo");
            String requireOptionsStr = request.getParameter("finalizeReqOptions");
            String requirePaymentStr = request.getParameter("finalizeReqPayInfo");
            String requireTermStr = request.getParameter("finalizeReqTermInfo");
            String requireAdditionalPartyStr = request.getParameter("finalizeReqAdditionalParty");
            String requireShipGroupsStr = request.getParameter("finalizeReqShipGroups");
            String singleUsePaymentStr = request.getParameter("singleUsePayment");
            requireCustomer = requireCustomerStr == null || "true".equalsIgnoreCase(requireCustomerStr);
            requireNewShippingAddress = requireNewShippingAddressStr != null && "true".equalsIgnoreCase(requireNewShippingAddressStr);
            requireShipping = requireShippingStr == null || "true".equalsIgnoreCase(requireShippingStr);
            requireOptions = requireOptionsStr == null || "true".equalsIgnoreCase(requireOptionsStr);
            requireShipGroups = requireShipGroupsStr != null && "true".equalsIgnoreCase(requireShipGroupsStr);
            if (requirePayment) {
                requirePayment = requirePaymentStr == null || "true".equalsIgnoreCase(requirePaymentStr);
            }
            if (requireTerm) {
                requireTerm = requireTermStr == null || "true".equalsIgnoreCase(requireTermStr);
            }
            requireAdditionalParty = requireAdditionalPartyStr == null || "true".equalsIgnoreCase(requireAdditionalPartyStr);
            isSingleUsePayment = singleUsePaymentStr != null && "Y".equalsIgnoreCase(singleUsePaymentStr) ? true : false;
        }

        boolean shippingAddressSet = true;
        boolean shippingOptionsSet = true;
        for (int shipGroupIndex = 0; shipGroupIndex < cart.getShipGroupSize(); shipGroupIndex++) {
            String shipContactMechId = cart.getShippingContactMechId(shipGroupIndex);
            if (shipContactMechId == null) {
                shippingAddressSet = false;
            }
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(shipGroupIndex);
            if (shipmentMethodTypeId == null) {
                shippingOptionsSet = false;
            }
        }

        String customerPartyId = cart.getPartyId();

        String[] processOrder = {"customer", "shipping", "shipGroups", "options", "term", "payment",
                "addparty", "paysplit"};

        if ("PURCHASE_ORDER".equals(cart.getOrderType())) {
            // Force checks for the following
            requireCustomer = true;
            requireShipping = true;
            requireOptions = true;
            processOrder = new String[]{"customer", "term", "shipping", "shipGroups", "options", "payment",
                    "addparty", "paysplit"};
        }

        for (String currProcess : processOrder) {
            if ("customer".equals(currProcess)) {
                if (requireCustomer && (customerPartyId == null || "_NA_".equals(customerPartyId))) {
                    return "customer";
                }
            } else if ("shipping".equals(currProcess)) {
                if (requireShipping) {
                    if (requireNewShippingAddress) {
                        return "shippingAddress";
                    } else if (!shippingAddressSet) {
                        return "shipping";
                    }
                }
            } else if ("shipGroups".equals(currProcess)) {
                if (requireShipGroups) {
                    return "shipGroups";
                }
            } else if ("options".equals(currProcess)) {
                if (requireOptions && !shippingOptionsSet) {
                    return "options";
                }
            } else if ("term".equals(currProcess)) {
                if (requireTerm && !cart.isOrderTermSet()) {
                    return "term";
                }
            } else if ("payment".equals(currProcess)) {
                List<String> paymentMethodIds = cart.getPaymentMethodIds();
                List<String> paymentMethodTypeIds = cart.getPaymentMethodTypeIds();
                if (requirePayment && UtilValidate.isEmpty(paymentMethodIds) && UtilValidate.isEmpty(paymentMethodTypeIds)) {
                    return "payment";
                }
            } else if ("addparty".equals(currProcess)) {
                if (requireAdditionalParty && cart.getAttribute("addpty") == null) {
                    return "addparty";
                }
            } else if ("paysplit".equals(currProcess)) {
                if (isSingleUsePayment) {
                    return "paysplit";
                }
            }
        }

        // Finally, if all checks go through, finalize the order.

        // this is used to go back to a previous page in checkout after processing all of the changes, just to make sure we get everything...
        String checkoutGoTo = request.getParameter("checkoutGoTo");
        if (UtilValidate.isNotEmpty(checkoutGoTo)) {
            return checkoutGoTo;
        }

        if ("SALES_ORDER".equals(cart.getOrderType())) {
            return "sales";
        }
        return "po";
    }

    public static String finalizeOrderEntryError(HttpServletRequest request, HttpServletResponse response) {
        String finalizePage = request.getParameter("finalizeMode");
        if (UtilValidate.isEmpty(finalizePage)) {
            return "error";
        }
        return finalizePage;
    }

    /**
     * Determine what billing account amount to use based on the form input.
     * This method returns the amount that will be charged to the billing account.
     * <p>
     * An amount can be associated with the billingAccountId with a
     * parameter billingAccountAmount.  If no amount is specified, then
     * the entire available balance of the given billing account will be used.
     * If there is an error, a null will be returned.
     * @return Amount to charge billing account or null if there was an error
     */
    private static BigDecimal determineBillingAccountAmount(String billingAccountId, String billingAccountAmount, LocalDispatcher dispatcher) {
        BigDecimal billingAccountAmt = null;

        // set the billing account amount to the minimum of billing account available balance or amount input if less than balance
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            // parse the amount to a decimal
            if (UtilValidate.isNotEmpty(billingAccountAmount)) {
                try {
                    billingAccountAmt = new BigDecimal(billingAccountAmount);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (billingAccountAmt == null) {
                billingAccountAmt = BigDecimal.ZERO;
            }
            BigDecimal availableBalance = CheckOutHelper.availableAccountBalance(billingAccountId, dispatcher);

            // set amount to be charged to entered amount unless it exceeds the available balance
            BigDecimal chargeAmount = BigDecimal.ZERO;
            if (billingAccountAmt.compareTo(availableBalance) < 0) {
                chargeAmount = billingAccountAmt;
            } else {
                chargeAmount = availableBalance;
            }
            if (chargeAmount.compareTo(BigDecimal.ZERO) < 0.0) {
                chargeAmount = BigDecimal.ZERO;
            }

            return chargeAmount;
        }
        return null;
    }

    /**
     * Create a replacement order from an existing order against a lost shipment etc.
     **/
    public static String createReplacementOrder(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");

        Map<String, Object> context = cart.makeCartMap(dispatcher, false);
        String originalOrderId = request.getParameter("orderId");

        // create the replacement order adjustment
        List<GenericValue> orderAdjustments = UtilGenerics.cast(context.get("orderAdjustments"));
        List<GenericValue> orderItems = UtilGenerics.cast(context.get("orderItems"));
        OrderReadHelper orderReadHelper = new OrderReadHelper(orderAdjustments, orderItems);
        BigDecimal grandTotal = orderReadHelper.getOrderGrandTotal();
        if (grandTotal.compareTo(new BigDecimal(0)) != 0) {
            GenericValue adjustment = delegator.makeValue("OrderAdjustment");
            adjustment.set("orderAdjustmentTypeId", "REPLACE_ADJUSTMENT");
            adjustment.set("amount", grandTotal.negate());
            adjustment.set("comments", "ReShip Order for Order #" + originalOrderId);
            adjustment.set("createdDate", UtilDateTime.nowTimestamp());
            adjustment.set("createdByUserLogin", userLogin.getString("userLoginId"));
            cart.addAdjustment(adjustment);
        }
        // create the order association
        List<ShoppingCartItem> cartLines = cart.items();
        for (ShoppingCartItem sci : cartLines) {
            int index = cart.getItemIndex(sci);
            try {
                GenericValue orderItem = EntityQuery.use(delegator).from("OrderItem")
                        .where("orderId", originalOrderId, "isPromo", sci.getIsPromo() ? "Y" : "N",
                                "productId", sci.getProductId(), "orderItemTypeId", sci.getItemType())
                        .queryFirst();
                if (orderItem != null) {
                    sci.setAssociatedOrderId(orderItem.getString("orderId"));
                    sci.setAssociatedOrderItemSeqId(orderItem.getString("orderItemSeqId"));
                    sci.setOrderItemAssocTypeId("REPLACEMENT");
                    cart.addItem(index, sci);
                }
            } catch (CartItemModifyException | GenericEntityException e) {
                Debug.logError(e.getMessage(), MODULE);
            }
        }

        String result = createOrder(request, response);
        if ("error".equals(result)) {
            return "error";
        }
        return "success";
    }
}
