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
package org.apache.ofbiz.accounting.thirdparty.ideal;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
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

import com.ing.ideal.connector.IdealConnector;
import com.ing.ideal.connector.IdealException;
import com.ing.ideal.connector.Issuer;
import com.ing.ideal.connector.Issuers;
import com.ing.ideal.connector.Transaction;


public class IdealEvents {

    public static final String resource = "AccountingUiLabels";
    public static final String resourceErr = "AccountingErrorUiLabels";
    public static final String module = IdealEvents.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    /** Initiate iDEAL Request */
    public static String callIdeal(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // get the orderId
        String orderId = (String) request.getAttribute("orderId");
        String issuerId = (String) request.getAttribute("issuerId");

        // get the order header
        GenericValue orderHeader = null;
        List<GenericValue> orderItemList = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            orderItemList = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingOrderHeader", locale));
            return "error";
        }

        // get the order total
        BigDecimal orderTotal = orderHeader.getBigDecimal("grandTotal");

        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(request);

        if (productStore == null) {
            Debug.logError("ProductStore is null", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }

        // get the payment properties file
        GenericValue paymentConfig = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStore.getString("productStoreId"), "EXT_IDEAL", null, true);
        String configString = null;
        String paymentGatewayConfigId = null;
        if (paymentConfig != null) {
            paymentGatewayConfigId = paymentConfig.getString("paymentGatewayConfigId");
            configString = paymentConfig.getString("paymentPropertiesPath");
        }

        if (configString == null) {
            configString = "payment.properties";
        }

        String merchantId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantId", configString, "merchantId");
        String merchantSubId = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantSubId", configString, "merchantSubId");
        String merchantReturnURL = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantReturnURL", configString, "merchantReturnURL");
        String acquirerURL = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "acquirerURL", configString, "acquirerURL");
        String acquirerTimeout = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "acquirerTimeout", configString, "acquirerTimeout");
        String privateCert = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "privateCert", configString, "privateCert");
        String acquirerKeyStoreFilename = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "acquirerKeyStoreFilename", configString, "acquirerKeyStoreFilename");
        String acquirerKeyStorePassword = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "acquirerKeyStorePassword", configString, "acquirerKeyStorePassword");
        String merchantKeyStoreFilename = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantKeyStoreFilename", configString, "merchantKeyStoreFilename");
        String merchantKeyStorePassword = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantKeyStorePassword", configString, "merchantKeyStorePassword");
        String expirationPeriod = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "expirationPeriod", configString, "expirationPeriod");

        if (UtilValidate.isEmpty(merchantId)
            || UtilValidate.isEmpty(merchantReturnURL)
            || UtilValidate.isEmpty(privateCert)
            || UtilValidate.isEmpty(merchantKeyStoreFilename)
            || UtilValidate.isEmpty(merchantKeyStoreFilename)) {
            Debug.logError("Payment properties is not configured properly, some notify URL from iDEAL is not correctly defined!", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingMerchantConfiguration", locale));
            return "error";
        }
        
        List<String> descriptionList = new LinkedList<String>();
        for (GenericValue orderItem : orderItemList) {
            if (UtilValidate.isNotEmpty(orderItem.get("itemDescription"))){
                descriptionList.add((String) orderItem.get("itemDescription"));
            }
        }
        
        String orderDescription = StringUtil.join(descriptionList, ",");
        String amount = orderTotal.setScale(decimals, rounding).movePointRight(2).toPlainString();
        
        String redirectString = null;
        
        try {
            IdealConnector connector = new IdealConnector("payment");
            Transaction transaction = new Transaction();
            transaction.setIssuerID(issuerId);
            transaction.setAmount(amount);
            transaction.setPurchaseID(orderId);
            transaction.setDescription(orderDescription);

            String returnURL = merchantReturnURL + "?orderId=" + orderId;
            Random random = new SecureRandom();
            String EntranceCode = Long.toString(Math.abs(random.nextLong()), 36);
            transaction.setEntranceCode(EntranceCode);
            transaction.setMerchantReturnURL(returnURL);
            Transaction trx = connector.requestTransaction(transaction);
            redirectString = trx.getIssuerAuthenticationURL();
            request.getSession().setAttribute("purchaseID", orderId);
            request.getSession().setAttribute("payAmount", orderTotal.toPlainString());
        } catch (IdealException ex) {
            Debug.logError(ex.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", ex.getConsumerMessage());
            return "error";
        }
        
        // redirect to iDEAL
        try {
            response.sendRedirect(redirectString);
        } catch (IOException e) {
            Debug.logError(e, "Problems redirecting to iDEAL", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsConnectingWithIdeal", locale));
            return "error";
        }

        return "success";
    }

    /** iDEAL notification */
    public static String idealNotify(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Map <String, Object> parametersMap = UtilHttp.getParameterMap(request);
        String transactionId = request.getParameter("trxid");
        for (String name : parametersMap.keySet()) {
            String value = request.getParameter(name);
            Debug.logError("### Param: " + name + " => " + value, module);
        }
        
        String orderId = null;
        String paymentStatus = null;
        try {
            IdealConnector connector = new IdealConnector("payment");
            Transaction transaction = connector.requestTransactionStatus(transactionId);
            orderId = transaction.getPurchaseID();
            if (orderId == null) {
                orderId = (String) request.getSession().getAttribute("purchaseID");
            }
            String payAmount = transaction.getAmount();
            if (payAmount == null) {
                payAmount = (String) request.getSession().getAttribute("payAmount");
            }
            paymentStatus = transaction.getStatus();
            request.setAttribute("transactionId", transactionId);
            request.setAttribute("paymentStatus", paymentStatus);
            request.setAttribute("paymentAmount", payAmount);
        } catch (IdealException ex) {
            Debug.logError(ex.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", ex.getConsumerMessage());
            return "error";
        }

        // get the user
        if (userLogin == null) {
            String userLoginId = "system";
            try {
                userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get UserLogin for: " + userLoginId + "; cannot continue", module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingAuthenticationUser", locale));
                return "error";
            }
        }
        // get the order header
        GenericValue orderHeader = null;
        if (UtilValidate.isNotEmpty(orderId)) {
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get the order header for order: " + orderId, module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingOrderHeader", locale));
                return "error";
            }
        } else {
            Debug.logError("iDEAL did not callback with a valid orderId!", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.noValidOrderIdReturned", locale));
            return "error";
        }
        if (orderHeader == null) {
            Debug.logError("Cannot get the order header for order: " + orderId, module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.problemsGettingOrderHeader", locale));
            return "error";
        }

        // attempt to start a transaction
        boolean okay = true;
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
            // authorized
            if ("Success".equals(paymentStatus)) {
                okay = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            // cancelled
            } else if ("Cancelled".equals(paymentStatus)) {
                okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
            }
            if (okay) {
                // set the payment preference
                okay = setPaymentPreferences(delegator, dispatcher, userLogin, orderId, request);
            }
        } catch (Exception e) {
            String errMsg = "Error handling iDEAL notification";
            Debug.logError(e, errMsg, module);
            try {
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericTransactionException gte2) {
                Debug.logError(gte2, "Unable to rollback transaction", module);
            }
        } finally {
            if (!okay) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Failure in processing iDEAL callback", null);
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
            request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resource, "IdealSuccessful", locale));
            // attempt to release the offline hold on the order (workflow)
            OrderChangeHelper.releaseInitialOrderHold(dispatcher, orderId);
            // call the email confirm service
            Map<String, String> emailContext = UtilMisc.toMap("orderId", orderId, "userLogin", userLogin);
            try {
                dispatcher.runSync("sendOrderConfirmation", emailContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problems sending email confirmation", module);
            }
        }
        return "success";
    }

    private static boolean setPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, String orderId, HttpServletRequest request) {
        Debug.logVerbose("Setting payment prefrences..", module);
        List <GenericValue> paymentPrefs = null;
        try {
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED").queryList();
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
        String paymentAmount = (String) request.getAttribute("paymentAmount");
        String paymentStatus = (String) request.getAttribute("paymentStatus");
        String transactionId = (String) request.getAttribute("transactionId");

        List <GenericValue> toStore = new LinkedList <GenericValue> ();
        java.sql.Timestamp authDate = UtilDateTime.nowTimestamp();

        paymentPreference.set("maxAmount", new BigDecimal(paymentAmount));
        if ("Success".equals(paymentStatus)) {
            paymentPreference.set("statusId", "PAYMENT_RECEIVED");
        } else if ("Cancelled".equals(paymentStatus)) {
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
        response.set("currencyUomId", "EUR");
        response.set("referenceNum", transactionId);
        response.set("gatewayCode", paymentStatus);
        response.set("gatewayFlag", paymentStatus.substring(0,1));
        response.set("transactionDate", authDate);
        toStore.add(response);

        try {
            delegator.storeAll(toStore);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot set payment preference/payment info", module);
            return false;
        }

        GenericValue userLoginId = null;
        try {
            userLoginId = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            return false;
        }

        // create a payment record too
        Map <String, Object> results = null;
        try {
            String comment = UtilProperties.getMessage(resource, "AccountingPaymentReceiveViaiDEAL", locale);
            results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLoginId,
                    "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "comments", comment));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resourceErr, "idealEvents.failedToExecuteServiceCreatePaymentFromPreference", locale));
            return false;
        }

        if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
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
                GenericValue ideal = EntityQuery.use(delegator).from("PaymentGatewayiDEAL").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
                if (ideal != null) {
                    Object idealField = ideal.get(paymentGatewayConfigParameterName);
                    if (idealField != null) {
                        returnValue = idealField.toString().trim();
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

    public static List<Issuer> getIssuerList() {
        List<Issuer> issuerList = new LinkedList<Issuer>();
        try {
            IdealConnector connector = new IdealConnector("payment");
            Issuers issuers = connector.getIssuerList();
            List<Issuer> shortList = issuers.getShortList();
            List<Issuer> longList = issuers.getLongList();
            for (Iterator<Issuer> iter = shortList.iterator(); iter.hasNext();) {
                Issuer issuer = (Issuer) iter.next();
                issuerList.add(issuer);
            }
            for (Iterator<Issuer> iter = longList.iterator(); iter.hasNext();) {
                Issuer issuer = (Issuer) iter.next();
                issuerList.add(issuer);
            }
        } catch (IdealException ex){
            Debug.logError(ex.getMessage(), module);
        }

        return issuerList;
    }
}
