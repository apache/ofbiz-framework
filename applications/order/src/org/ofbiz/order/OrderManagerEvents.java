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
package org.ofbiz.order;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;

/**
 * Order Manager Events
 */
public class OrderManagerEvents {

    public static final String module = OrderManagerEvents.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static String processOfflinePayments(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        if (session.getAttribute("OFFLINE_PAYMENTS") != null) {
            String orderId = (String) request.getAttribute("orderId");
            List toBeStored = new LinkedList();
            List paymentPrefs = null;
            GenericValue placingCustomer = null;
            try {
                paymentPrefs = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId));
                List pRoles = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "PLACING_CUSTOMER"));
                if (pRoles != null && pRoles.size() > 0)
                    placingCustomer = EntityUtil.getFirst(pRoles);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems looking up order payment preferences", module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderErrorProcessingOfflinePayments", locale));
                return "error";
            }
            if (paymentPrefs != null) {
                Iterator i = paymentPrefs.iterator();
                while (i.hasNext()) {
                    // update the preference to received
                    // TODO: updating payment preferences should be done as a service 
                    GenericValue ppref = (GenericValue) i.next();
                    ppref.set("statusId", "PAYMENT_RECEIVED");
                    ppref.set("authDate", UtilDateTime.nowTimestamp());
                    toBeStored.add(ppref);

                    // create a payment record
                    Map results = null;
                    try {
                        results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("orderPaymentPreferenceId", ppref.get("orderPaymentPreferenceId"), 
                                "paymentFromId", placingCustomer.getString("partyId"), "comments", "Payment received offline and manually entered."));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
                        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                        return "error";
                    }
 
                    if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                        Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module); 
                        request.setAttribute("_ERROR_MESSAGE_", (String) results.get(ModelService.ERROR_MESSAGE));
                        return "error";
                    }
                }

                // store the updated preferences
                try {
                    delegator.storeAll(toBeStored);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems storing payment information", module);
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemStoringReceivedPaymentInformation", locale));
                    return "error";
                }

                // set the status of the order to approved
                OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            }
        }
        return "success";
    }

    public static String receiveOfflinePayment(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        String orderId = request.getParameter("orderId");

        // get the order header & payment preferences
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems reading order header from datasource.", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemsReadingOrderHeaderInformation", locale));
            return "error";
        }

        Double grandTotal = new Double(0.00);
        if (orderHeader != null) {
            grandTotal = orderHeader.getDouble("grandTotal");
        }

        // get the payment types to receive
        List paymentMethodTypes = null;

        try {
            List pmtFields = UtilMisc.toList(new EntityExpr("paymentMethodTypeId", EntityOperator.NOT_EQUAL, "EXT_OFFLINE"));
            paymentMethodTypes = delegator.findByAnd("PaymentMethodType", pmtFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting payment types", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemsWithPaymentTypeLookup", locale));
            return "error";
        }

        if (paymentMethodTypes == null) {
        	request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemsWithPaymentTypeLookup", locale));
            return "error";
        }

        List toBeStored = new LinkedList();
        GenericValue placingCustomer = null;
        try {
            List pRoles = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "PLACING_CUSTOMER"));
            if (pRoles != null && pRoles.size() > 0)
                placingCustomer = EntityUtil.getFirst(pRoles);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up order payment preferences", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderErrorProcessingOfflinePayments", locale));
            return "error";
        }

        Iterator pmti = paymentMethodTypes.iterator();
        while (pmti.hasNext()) {
            GenericValue paymentMethodType = (GenericValue) pmti.next();
            String paymentMethodTypeId = paymentMethodType.getString("paymentMethodTypeId");
            String amountStr = request.getParameter(paymentMethodTypeId + "_amount");
            String paymentReference = request.getParameter(paymentMethodTypeId + "_reference");
            if (!UtilValidate.isEmpty(amountStr)) {
                double paymentTypeAmount = 0.00;
                try {
                    paymentTypeAmount = NumberFormat.getNumberInstance(locale).parse(amountStr).doubleValue();
                } catch (java.text.ParseException pe) {
                	request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemsPaymentParsingAmount", locale));
                    return "error";
                }
                if (paymentTypeAmount > 0.00) {

                    // create the OrderPaymentPreference
                    // TODO: this should be done with a service
                    Map prefFields = UtilMisc.toMap("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
                    GenericValue paymentPreference = delegator.makeValue("OrderPaymentPreference", prefFields);
                    paymentPreference.set("paymentMethodTypeId", paymentMethodType.getString("paymentMethodTypeId"));
                    paymentPreference.set("maxAmount", new Double(paymentTypeAmount));
                    paymentPreference.set("statusId", "PAYMENT_RECEIVED");
                    paymentPreference.set("orderId", orderId);
                    paymentPreference.set("createdDate", UtilDateTime.nowTimestamp());
                    if (userLogin != null) {
                        paymentPreference.set("createdByUserLogin", userLogin.getString("userLoginId"));
                    }
                    
                    try {
                        delegator.create(paymentPreference);
                    } catch (GenericEntityException ex) {
                        Debug.logError(ex, "Cannot create a new OrderPaymentPreference", module);
                        request.setAttribute("_ERROR_MESSAGE_", ex.getMessage());
                        return "error";
                    }

                    // create a payment record
                    Map results = null;
                    try {
                        results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin, 
                                "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "paymentRefNum", paymentReference, 
                                "paymentFromId", placingCustomer.getString("partyId"), "comments", "Payment received offline and manually entered."));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
                        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                        return "error";
                    }
                     
                    if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                        Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
                        request.setAttribute("_ERROR_MESSAGE_", (String) results.get(ModelService.ERROR_MESSAGE));
                        return "error";
                    }
                }
            }
        }

        // get the current payment prefs
        GenericValue offlineValue = null;
        List currentPrefs = null;
        double paymentTally = 0.00;
        try {
            List oppFields = UtilMisc.toList(new EntityExpr("orderId", EntityOperator.EQUALS, orderId),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
            currentPrefs = delegator.findByAnd("OrderPaymentPreference", oppFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Unable to get existing payment preferences from order", module);
        }
        if (currentPrefs != null && currentPrefs.size() > 0) {
            Iterator cpi = currentPrefs.iterator();
            while (cpi.hasNext()) {
                GenericValue cp = (GenericValue) cpi.next();
                String paymentMethodType = cp.getString("paymentMethodTypeId");
                if ("EXT_OFFLINE".equals(paymentMethodType)) {
                    offlineValue = cp;
                } else {
                    Double cpAmt = cp.getDouble("maxAmount");
                    if (cpAmt != null) {
                        paymentTally += cpAmt.doubleValue();
                    }
                }
            }
        }

        // now finish up
        boolean okayToApprove = false;
        if (paymentTally >= grandTotal.doubleValue()) {
            // cancel the offline preference
            okayToApprove = true;
            if (offlineValue != null) {
                offlineValue.set("statusId", "PAYMENT_CANCELLED");
                toBeStored.add(offlineValue);
            }
        }

        // store the status changes and the newly created payment preferences and payments
        // TODO: updating order payment preference should be done with a service
        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems storing payment information", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemStoringReceivedPaymentInformation", locale));
            return "error";
        }

        if (okayToApprove) {
            // update the status of the order and items
            OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
        }

        return "success";
    }

}
