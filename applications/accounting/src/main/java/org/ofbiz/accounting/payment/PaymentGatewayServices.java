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
package org.ofbiz.accounting.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * PaymentGatewayServices
 */
public class PaymentGatewayServices {

    public static final String module = PaymentGatewayServices.class.getName();
    public static final String AUTH_SERVICE_TYPE = "PRDS_PAY_AUTH";
    public static final String REAUTH_SERVICE_TYPE = "PRDS_PAY_REAUTH";
    public static final String RELEASE_SERVICE_TYPE = "PRDS_PAY_RELEASE";
    public static final String CAPTURE_SERVICE_TYPE = "PRDS_PAY_CAPTURE";
    public static final String REFUND_SERVICE_TYPE = "PRDS_PAY_REFUND";
    public static final String CREDIT_SERVICE_TYPE = "PRDS_PAY_CREDIT";
    private static final int TX_TIME = 300;
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals;
    private static int rounding;
    public final static String resource = "AccountingUiLabels";
    public static final String resourceError = "AccountingErrorUiLabels";
    public static final String resourceOrder = "OrderUiLabels";
    
    static {
        decimals = UtilNumber.getBigDecimalScale("order.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO = ZERO.setScale(decimals);
    }

    /**
     * Authorizes a single order preference with an option to specify an amount. The result map has the Booleans
     * "errors" and "finished" which notify the user if there were any errors and if the authorization was finished.
     * There is also a List "messages" for the authorization response messages and a BigDecimal, "processAmount" as the
     * amount processed.
     *
     * TODO: it might be nice to return the paymentGatewayResponseId
     */
    public static Map<String, Object> authOrderPaymentPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        BigDecimal overrideAmount = (BigDecimal) context.get("overrideAmount");

        // validate overrideAmount if its available
        if (overrideAmount != null) {
            if (overrideAmount.compareTo(BigDecimal.ZERO) < 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingPaymentAmountIsNegative",
                        UtilMisc.toMap("overrideAmount", overrideAmount), locale));
            }
            if (overrideAmount.compareTo(BigDecimal.ZERO) == 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingPaymentAmountIsZero",
                        UtilMisc.toMap("overrideAmount", overrideAmount), locale));
            }
        }

        GenericValue orderHeader = null;
        GenericValue orderPaymentPreference = null;
        try {
            orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
            orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the total remaining
        BigDecimal totalRemaining = orh.getOrderGrandTotal();

        // get the process attempts so far
        Long procAttempt = orderPaymentPreference.getLong("processAttempt");
        if (procAttempt == null) {
            procAttempt = Long.valueOf(0);
        }

        // update the process attempt count
        orderPaymentPreference.set("processAttempt", Long.valueOf(procAttempt.longValue() + 1));
        try {
            orderPaymentPreference.store();
            orderPaymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale));
        }

        // if we are already authorized, then this is a re-auth request
        boolean reAuth = false;
        if (orderPaymentPreference.get("statusId") != null && "PAYMENT_AUTHORIZED".equals(orderPaymentPreference.getString("statusId"))) {
            reAuth = true;
        }

        // use overrideAmount or maxAmount
        BigDecimal transAmount = null;
        if (overrideAmount != null) {
            transAmount = overrideAmount;
        } else {
            transAmount = orderPaymentPreference.getBigDecimal("maxAmount");
        }

        // round this before moving on just in case a funny number made it this far
        transAmount = transAmount.setScale(decimals, rounding);

        // if our transaction amount exists and is zero, there's nothing to process, so return
        if ((transAmount != null) && (transAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("finished", Boolean.TRUE); // finished is true since there is nothing to do
            results.put("errors", Boolean.FALSE); // errors is false since no error occurred
            return results;
        }

        try {
            // call the authPayment method
            Map<String, Object> authPaymentResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference, totalRemaining, reAuth, transAmount);

            // handle the response
            if (authPaymentResult != null) {
                // not null result means either an approval or decline; null would mean error
                BigDecimal thisAmount = (BigDecimal) authPaymentResult.get("processAmount");

                // process the auth results
                try {
                    boolean processResult = processResult(dctx, authPaymentResult, userLogin, orderPaymentPreference, locale);
                    if (processResult) {
                        Map<String, Object> results = ServiceUtil.returnSuccess();
                        results.put("messages", authPaymentResult.get("customerRespMsgs"));
                        results.put("processAmount", thisAmount);
                        results.put("finished", Boolean.TRUE);
                        results.put("errors", Boolean.FALSE);
                        results.put("authCode", authPaymentResult.get("authCode"));
                        return results;
                    } else {
                        boolean needsNsfRetry = needsNsfRetry(orderPaymentPreference, authPaymentResult, delegator);

                        // if we are doing an NSF retry then also...
                        if (needsNsfRetry) {
                            // TODO: what do we do with this? we need to fail the auth but still allow the order through so it can be fixed later
                            // NOTE: this is called through a different path for auto re-orders, so it should be good to go... will leave this comment here just in case...
                        }

                        // if we have a failure at this point and no NSF retry is needed, then try other credit cards on file, if the user has any
                        if (!needsNsfRetry) {
                            // is this an auto-order?
                            if (UtilValidate.isNotEmpty(orderHeader.getString("autoOrderShoppingListId"))) {
                                GenericValue productStore = orderHeader.getRelatedOne("ProductStore", false);
                                // according to the store should we try other cards?
                                if ("Y".equals(productStore.getString("autoOrderCcTryOtherCards"))) {
                                    // get other credit cards for the bill to party
                                    List<GenericValue> otherPaymentMethodAndCreditCardList = null;
                                    String billToPartyId = null;
                                    GenericValue billToParty = orh.getBillToParty();
                                    if (billToParty != null) {
                                        billToPartyId = billToParty.getString("partyId");
                                    } else {
                                        // TODO optional: any other ways to find the bill to party? perhaps look at info from OrderPaymentPreference, ie search back from other PaymentMethod...
                                    }

                                    if (UtilValidate.isNotEmpty(billToPartyId)) {
                                        otherPaymentMethodAndCreditCardList = EntityQuery.use(delegator).from("PaymentMethodAndCreditCard")
                                                .where("partyId", billToPartyId, "paymentMethodTypeId", "CREDIT_CARD").filterByDate().queryList();
                                    }

                                    if (UtilValidate.isNotEmpty(otherPaymentMethodAndCreditCardList)) {
                                        for (GenericValue otherPaymentMethodAndCreditCard : otherPaymentMethodAndCreditCardList) {
                                            // change OrderPaymentPreference in memory only and call auth service
                                            orderPaymentPreference.set("paymentMethodId", otherPaymentMethodAndCreditCard.getString("paymentMethodId"));
                                            Map<String, Object> authRetryResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference, totalRemaining, reAuth, transAmount);
                                            try {
                                                boolean processRetryResult = processResult(dctx, authPaymentResult, userLogin, 
                                                        orderPaymentPreference, locale);

                                                if (processRetryResult) {
                                                    // wow, we got here that means the other card was successful...
                                                    // on success save the OrderPaymentPreference, and then return finished (which will break from loop)
                                                    orderPaymentPreference.store();

                                                    Map<String, Object> results = ServiceUtil.returnSuccess();
                                                    results.put("messages", authRetryResult.get("customerRespMsgs"));
                                                    results.put("processAmount", thisAmount);
                                                    results.put("finished", Boolean.TRUE);
                                                    results.put("errors", Boolean.FALSE);
                                                    return results;
                                                }
                                            } catch (GeneralException e) {
                                                String errMsg = "Error saving and processing payment authorization results: " + e.toString();
                                                Debug.logError(e, errMsg + "; authRetryResult: " + authRetryResult, module);
                                                Map<String, Object> results = ServiceUtil.returnSuccess();
                                                results.put(ModelService.ERROR_MESSAGE, errMsg);
                                                results.put("finished", Boolean.FALSE);
                                                results.put("errors", Boolean.TRUE);
                                                return results;
                                            }

                                            // if no sucess, fall through to return not finished
                                        }
                                    }
                                }
                            }
                        }

                        Map<String, Object> results = ServiceUtil.returnSuccess();
                        results.put("messages", authPaymentResult.get("customerRespMsgs"));
                        results.put("finished", Boolean.FALSE);
                        results.put("errors", Boolean.FALSE);
                        return results;
                    }
                } catch (GeneralException e) {
                    String errMsg = "Error saving and processing payment authorization results: " + e.toString();
                    Debug.logError(e, errMsg + "; authPaymentResult: " + authPaymentResult, module);
                    Map<String, Object> results = ServiceUtil.returnSuccess();
                    results.put(ModelService.ERROR_MESSAGE, errMsg);
                    results.put("finished", Boolean.FALSE);
                    results.put("errors", Boolean.TRUE);
                    return results;
                }
            } else {
                // error with payment processor; will try later
                String errMsg = "Invalid Order Payment Preference: maxAmount is 0";
                Debug.logInfo(errMsg, module);
                Map<String, Object> results = ServiceUtil.returnSuccess();
                results.put("finished", Boolean.FALSE);
                results.put("errors", Boolean.TRUE);
                results.put(ModelService.ERROR_MESSAGE, errMsg);
                orderPaymentPreference.set("statusId", "PAYMENT_CANCELLED");
                try {
                    orderPaymentPreference.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Problem setting OrderPaymentPreference status to CANCELLED", module);
                }
                return results;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error processing payment authorization", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingPaymentCannotBeAuthorized", 
                    UtilMisc.toMap("errroString", e.toString()), locale));
        }
    }

    /**
     * Processes payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return APPROVED|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> authOrderPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        boolean reAuth = false;
        if (context.get("reAuth") != null) {
            reAuth = ((Boolean)context.get("reAuth")).booleanValue();
        }
        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List<GenericValue> paymentPrefs = null;

        try {
            // get the OrderHeader
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            // get the payments to auth
            Map<String, String> lookupMap = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_AUTH");
            List<String> orderList = UtilMisc.toList("maxAmount");
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where(lookupMap).orderBy(orderList).queryList();
            if (reAuth) {
                lookupMap.put("orderId", orderId);
                lookupMap.put("statusId", "PAYMENT_AUTHORIZED");
                paymentPrefs.addAll(EntityQuery.use(delegator).from("OrderPaymentPreference").where(lookupMap).orderBy(orderList).queryList());
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting the order information", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not get order information (" + gee.toString() + ").");
            return result;
        }

        // make sure we have a OrderHeader
        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }

        // get the order amounts
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        BigDecimal totalRemaining = orh.getOrderGrandTotal();

        // loop through and auth each order payment preference
        int finished = 0;
        int hadError = 0;
        List<String> messages = new LinkedList<String>();
        for (GenericValue paymentPref : paymentPrefs) {
            if (reAuth && "PAYMENT_AUTHORIZED".equals(paymentPref.getString("statusId"))) {
                String paymentConfig = null;
                // get the payment settings i.e. serviceName and config properties file name
                GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPref, AUTH_SERVICE_TYPE, false);
                if (paymentSettings != null) {
                    paymentConfig = paymentSettings.getString("paymentPropertiesPath");
                    if (UtilValidate.isEmpty(paymentConfig)) {
                        paymentConfig = "payment.properties";
                    }
                }
                // check the validity of the authorization; re-auth if necessary
                if (PaymentGatewayServices.checkAuthValidity(paymentPref, paymentConfig)) {
                    finished += 1;
                    continue;
                }
            }
            Map<String, Object> authContext = new HashMap<String, Object>();
            authContext.put("orderPaymentPreferenceId", paymentPref.getString("orderPaymentPreferenceId"));
            authContext.put("userLogin", context.get("userLogin"));

            Map<String, Object> results = null;
            try {
                results = dispatcher.runSync("authOrderPaymentPreference", authContext);
            } catch (GenericServiceException se) {
                Debug.logError(se, "Error in calling authOrderPaymentPreference from authOrderPayments", module);
                hadError += 1;
                messages.add("Could not authorize OrderPaymentPreference [" + paymentPref.getString("orderPaymentPreferenceId") + "] for order [" + orderId + "]: " + se.toString());
                continue;
            }

            // add authorization code to the result
            result.put("authCode", results.get("authCode"));

            if (ServiceUtil.isError(results)) {
                hadError += 1;
                messages.add("Could not authorize OrderPaymentPreference [" + paymentPref.getString("orderPaymentPreferenceId") + "] for order [" + orderId + "]: " + results.get(ModelService.ERROR_MESSAGE));
                continue;
            }
            if (((Boolean) results.get("finished")).booleanValue()) {
                finished += 1;
            }
            if (((Boolean) results.get("errors")).booleanValue()) {
                hadError += 1;
            }
            if (results.get("messages") != null) {
                List<String> message = UtilGenerics.checkList(results.get("messages"));
                messages.addAll(message);
            }
            if (results.get("processAmount") != null) {
                totalRemaining = totalRemaining.subtract(((BigDecimal) results.get("processAmount")));
            }
        }

        Debug.logInfo("Finished with auth(s) checking results", module);

        // add messages to the result
        result.put("authResultMsgs", messages);

        if (hadError > 0) {
            Debug.logError("Error(s) (" + hadError + ") during auth; returning ERROR", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put("processResult", "ERROR");
            return result;
        } else if (finished == paymentPrefs.size()) {
            Debug.logInfo("All auth(s) passed total remaining : " + totalRemaining, module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put("processResult", "APPROVED");
            return result;
        } else {
            Debug.logInfo("Only [" + finished + "/" + paymentPrefs.size() + "] OrderPaymentPreference authorizations passed; returning processResult=FAILED with no message so that message from ProductStore will be used", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put("processResult", "FAILED");
            return result;
        }
    }


    private static Map<String, Object> authPayment(LocalDispatcher dispatcher, GenericValue userLogin, OrderReadHelper orh, GenericValue paymentPreference, BigDecimal totalRemaining, boolean reauth, BigDecimal overrideAmount) throws GeneralException {
        String paymentConfig = null;
        String serviceName = null;
        String paymentGatewayConfigId = null;

        // get the payment settings i.e. serviceName and config properties file name
        String serviceType = AUTH_SERVICE_TYPE;
        if (reauth) {
            serviceType = REAUTH_SERVICE_TYPE;
        }

        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPreference, serviceType, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString("paymentService");
            }
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");
        } else {
            throw new GeneralException("Could not find any valid payment settings for order with ID [" + orh.getOrderId() + "], and payment operation (serviceType) [" + serviceType + "]");
        }

        // make sure the service name is not null
        if (serviceName == null) {
            throw new GeneralException("Invalid payment processor, serviceName is null: " + paymentSettings);
        }

        // make the process context
        Map<String, Object> processContext = new HashMap<String, Object>();

        // get the visit record to obtain the client's IP address
        GenericValue orderHeader = orh.getOrderHeader();
        //if (orderHeader == null) {}

        String visitId = orderHeader.getString("visitId");
        GenericValue visit = null;
        if (visitId != null) {
            try {
                visit = orderHeader.getDelegator().findOne("Visit", UtilMisc.toMap("visitId", visitId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        if (visit != null && visit.get("clientIpAddress") != null) {
            processContext.put("customerIpAddress", visit.getString("clientIpAddress"));
        }

        GenericValue productStore = orderHeader.getRelatedOne("ProductStore", false);

        processContext.put("userLogin", userLogin);
        processContext.put("orderId", orh.getOrderId());
        processContext.put("orderItems", orh.getOrderItems());
        processContext.put("shippingAddress", EntityUtil.getFirst(orh.getShippingLocations())); // TODO refactor the payment API to handle support all addresses
        processContext.put("paymentConfig", paymentConfig);
        processContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
        processContext.put("currency", orh.getCurrency());
        processContext.put("orderPaymentPreference", paymentPreference);
        if (paymentPreference.get("securityCode") != null) {
            processContext.put("cardSecurityCode", paymentPreference.get("securityCode"));
        }

        // get the billing information
        getBillingInformation(orh, paymentPreference, processContext);

        // default charge is totalRemaining
        BigDecimal processAmount = totalRemaining;

        // use override or max amount available
        if (overrideAmount != null) {
            processAmount = overrideAmount;
        } else if (paymentPreference.get("maxAmount") != null) {
            processAmount = paymentPreference.getBigDecimal("maxAmount");
        }

        // Check if the order is a replacement order
        boolean replacementOrderFlag = isReplacementOrder(orderHeader);

        // don't authorized more then what is required
        if (!replacementOrderFlag && processAmount.compareTo(totalRemaining) > 0) {
            processAmount = totalRemaining;
        }

        // format the decimal
        processAmount = processAmount.setScale(decimals, rounding);

        if (Debug.verboseOn()) Debug.logVerbose("Charging amount: " + processAmount, module);
        processContext.put("processAmount", processAmount);

        // invoke the processor
        Map<String, Object> processorResult = null;
        try {
            // invoke the payment processor; allow 5 minute transaction timeout and require a new tx; we'll capture the error and pass back nicely

            GenericValue creditCard = (GenericValue) processContext.get("creditCard");

            // only try other exp dates if orderHeader.autoOrderShoppingListId is not empty, productStore.autoOrderCcTryExp=Y and this payment is a creditCard
            boolean tryOtherExpDates = "Y".equals(productStore.getString("autoOrderCcTryExp")) && creditCard != null && UtilValidate.isNotEmpty(orderHeader.getString("autoOrderShoppingListId"));

            // if we are not trying other expire dates OR if we are and the date is after today, then run the service
            if (!tryOtherExpDates || UtilValidate.isDateAfterToday(creditCard.getString("expireDate"))) {
                processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
            }

            // try other expire dates if the expireDate is not after today, or if we called the auth service and resultBadExpire = true
            if (tryOtherExpDates && (!UtilValidate.isDateAfterToday(creditCard.getString("expireDate")) || (processorResult != null && Boolean.TRUE.equals(processorResult.get("resultBadExpire"))))) {
                // try adding 2, 3, 4 years later with the same month
                String expireDate = creditCard.getString("expireDate");
                int dateSlash1 = expireDate.indexOf("/");
                String month = expireDate.substring(0, dateSlash1);
                String year = expireDate.substring(dateSlash1 + 1);

                // start adding 2 years, if comes back with resultBadExpire try again up to twice incrementing one year
                year = StringUtil.addToNumberString(year, 2);
                // note that this is set in memory only for now, not saved to the database unless successful
                creditCard.set("expireDate", month + "/" + year);
                // don't need to set back in the processContext, it's already there: processContext.put("creditCard", creditCard);
                processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);

                // note that these additional tries will only be done if the service return is not an error, in that case we let it pass through to the normal error handling
                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals(processorResult.get("resultBadExpire"))) {
                    // okay, try one more year...
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set("expireDate", month + "/" + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                }

                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals(processorResult.get("resultBadExpire"))) {
                    // okay, try one more year... and this is the last try
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set("expireDate", month + "/" + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                }

                // at this point if we have a successful result, let's save the new creditCard expireDate
                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals(processorResult.get("authResult"))) {
                    // TODO: this is bad; we should be expiring the old card and creating a new one instead of editing it
                    creditCard.store();
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error occurred on: " + serviceName + ", Order ID is: [" + orh.getOrderId() + "]", module);
            throw new GeneralException("Problems invoking payment processor! Will retry later. Order ID is: [" + orh.getOrderId() + "]", e);
        }

        if (processorResult != null) {
            // check for errors from the processor implementation
            if (ServiceUtil.isError(processorResult)) {
                Debug.logError("Processor failed; will retry later: " + processorResult.get(ModelService.ERROR_MESSAGE), module);
                // log the error message as a gateway response when it fails
                saveError(dispatcher, userLogin, paymentPreference, processorResult, AUTH_SERVICE_TYPE, "PGT_AUTHORIZE");
                // this is the one place where we want to return null because the calling method will look for this
                return null;
            }

            // pass the payTo partyId to the result processor; we just add it to the result context.
            String payToPartyId = getPayToPartyId(orh.getOrderHeader());
            processorResult.put("payToPartyId", payToPartyId);

            // add paymentSettings to result; for use by later processors
            processorResult.put("paymentSettings", paymentSettings);

            // and pass on the currencyUomId
            processorResult.put("currencyUomId", orh.getCurrency());
        }

        return processorResult;
    }

    private static GenericValue getPaymentSettings(GenericValue orderHeader, GenericValue paymentPreference, String paymentServiceType, boolean anyServiceType) {
        Delegator delegator = orderHeader.getDelegator();
        GenericValue paymentSettings = null;
        String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");

        if (paymentMethodTypeId != null) {
            String productStoreId = orderHeader.getString("productStoreId");
            if (productStoreId != null) {
                paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId, paymentServiceType, anyServiceType);
            }
        }
        return paymentSettings;
    }

    private static String getPayToPartyId(GenericValue orderHeader) {
        String payToPartyId = "Company"; // default value
        GenericValue productStore = null;
        try {
            productStore = orderHeader.getRelatedOne("ProductStore", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStore from OrderHeader", module);
            return null;
        }
        if (productStore != null && productStore.get("payToPartyId") != null) {
            payToPartyId = productStore.getString("payToPartyId");
        } else {
            Debug.logWarning("Using default value of [Company] for payToPartyId on order [" + orderHeader.getString("orderId") + "]", module);
        }
        return payToPartyId;
    }

    private static String getBillingInformation(OrderReadHelper orh, GenericValue paymentPreference, Map<String, Object> toContext) throws GenericEntityException {
        // gather the payment related objects.
        String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");
        GenericValue paymentMethod = paymentPreference.getRelatedOne("PaymentMethod", false);
        if (paymentMethod != null && "CREDIT_CARD".equals(paymentMethodTypeId)) {
            // type credit card
            GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard", false);
            GenericValue billingAddress = creditCard.getRelatedOne("PostalAddress", false);
            toContext.put("creditCard", creditCard);
            toContext.put("billingAddress", billingAddress);
        } else if (paymentMethod != null && "EFT_ACCOUNT".equals(paymentMethodTypeId)) {
            // type eft
            GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount", false);
            GenericValue billingAddress = eftAccount.getRelatedOne("PostalAddress", false);
            toContext.put("eftAccount", eftAccount);
            toContext.put("billingAddress", billingAddress);
        } else if (paymentMethod != null && "GIFT_CARD".equals(paymentMethodTypeId)) {
            // type gift card
            GenericValue giftCard = paymentMethod.getRelatedOne("GiftCard", false);
            toContext.put("giftCard", giftCard);
            GenericValue orderHeader = paymentPreference.getRelatedOne("OrderHeader", false);
            List<GenericValue> orderItems = orderHeader.getRelated("OrderItem", null, null, false);
            toContext.put("orderId", orderHeader.getString("orderId"));
            toContext.put("orderItems", orderItems);
        } else if ("FIN_ACCOUNT".equals(paymentMethodTypeId)) {
            toContext.put("finAccountId", paymentPreference.getString("finAccountId"));
        } else if ("EXT_PAYPAL".equals(paymentMethodTypeId)) {
            GenericValue payPalPaymentMethod = paymentMethod.getRelatedOne("PayPalPaymentMethod", false);
            toContext.put("payPalPaymentMethod", payPalPaymentMethod);
        } else {
            // add other payment types here; i.e. gift cards, etc.
            // unknown payment type; ignoring.
            Debug.logError("ERROR: Unsupported PaymentMethodType passed for authorization", module);
            return null;
        }

        // get some contact info.
        GenericValue billToPersonOrGroup = orh.getBillToParty();
        GenericValue billToEmail = null;

        Collection<GenericValue> emails = ContactHelper.getContactMech(billToPersonOrGroup.getRelatedOne("Party", false), "PRIMARY_EMAIL", "EMAIL_ADDRESS", false);

        if (UtilValidate.isNotEmpty(emails)) {
            billToEmail = emails.iterator().next();
        }

        toContext.put("billToParty", billToPersonOrGroup);
        toContext.put("billToEmail", billToEmail);

        return billToPersonOrGroup.getString("partyId");
    }

    /**
     *
     * Releases authorizations through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payments.
     */
    public static Map<String, Object> releaseOrderPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String orderId = "";
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            if (orderPaymentPreferenceId != null) {
                paymentPref = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
                orderId = paymentPref.getString("orderId");
            }
            else {
                orderId =  (String) context.get("orderId");
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }

        // get the payment preferences
        List<GenericValue> paymentPrefs = null;
        try {
            // get the valid payment prefs
            List<EntityExpr> othExpr = UtilMisc.toList(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EFT_ACCOUNT"));
            othExpr.add(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "GIFT_CARD"));
            othExpr.add(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "FIN_ACCOUNT"));
            EntityCondition con1 = EntityCondition.makeCondition(othExpr, EntityJoinOperator.OR);
            EntityCondition statExpr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_SETTLED");
            EntityCondition con2 = EntityCondition.makeCondition(UtilMisc.toList(con1, statExpr), EntityOperator.AND);
            EntityCondition authExpr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_AUTHORIZED");
            EntityCondition con3 = EntityCondition.makeCondition(UtilMisc.toList(con2, authExpr), EntityOperator.OR);
            EntityExpr orderExpr = EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId);
            EntityCondition con4 = EntityCondition.makeCondition(UtilMisc.toList(con3, orderExpr), EntityOperator.AND);
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where(con4).queryList();
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting entity record(s), see stack trace", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not get order information (" + gee.toString() + ").");
            return result;
        }

        // return complete if no payment prefs were found
        if (paymentPrefs.size() == 0) {
            Debug.logWarning("No OrderPaymentPreference records available for release", module);
            result.put("processResult", "COMPLETE");
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result;
        }

        // iterate over the prefs and release each one
        List<GenericValue> finished = new LinkedList<GenericValue>();
        for (GenericValue pPref : paymentPrefs) {
            Map<String, Object> releaseContext = UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", pPref.getString("orderPaymentPreferenceId"));
            Map<String, Object> releaseResult = null;
            try {
                releaseResult = dispatcher.runSync("releaseOrderPaymentPreference", releaseContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling releaseOrderPaymentPreference service for orderPaymentPreferenceId" + 
                        paymentPref.getString("orderPaymentPreferenceId"), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale) + " " +
                        paymentPref.getString("orderPaymentPreferenceId"));
            }
            if (ServiceUtil.isError(releaseResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(releaseResult), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
            } else if (! ServiceUtil.isFailure(releaseResult)) {
                finished.add(paymentPref);
            }
        }
        result = ServiceUtil.returnSuccess();
        if (finished.size() == paymentPrefs.size()) {
            result.put("processResult", "COMPLETE");
        } else {
            result.put("processResult", "FAILED");
        }

        return result;
    }

    public static Map<String, Object> processCreditResult(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String currencyUomId = (String) context.get("currencyUomId");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Boolean creditResponse = (Boolean) context.get("creditResult");
        Locale locale = (Locale) context.get("locale");
        // create the PaymentGatewayResponse
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue pgCredit = delegator.makeValue("PaymentGatewayResponse");
        pgCredit.set("paymentGatewayResponseId", responseId);
        pgCredit.set("paymentServiceTypeEnumId", CREDIT_SERVICE_TYPE);
        pgCredit.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        pgCredit.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        pgCredit.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        pgCredit.set("transCodeEnumId", "PGT_CREDIT");
        // set the credit info
        pgCredit.set("amount", context.get("creditAmount"));
        pgCredit.set("referenceNum", context.get("creditRefNum"));
        pgCredit.set("altReference", context.get("creditAltRefNum"));
        pgCredit.set("gatewayCode", context.get("creditCode"));
        pgCredit.set("gatewayFlag", context.get("creditFlag"));
        pgCredit.set("gatewayMessage", context.get("creditMessage"));
        pgCredit.set("transactionDate", UtilDateTime.nowTimestamp());
        pgCredit.set("currencyUomId", currencyUomId);
        // create the internal messages
        List<GenericValue> messageEntities = new LinkedList<GenericValue>();
        List<String> messages = UtilGenerics.cast(context.get("internalRespMsgs"));
        if (UtilValidate.isNotEmpty(messages)) {
            for (String message : messages) {
                GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg");
                String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                respMsg.set("paymentGatewayRespMsgId", respMsgId);
                respMsg.set("paymentGatewayResponseId", responseId);
                respMsg.set("pgrMessage", message);
                // store the messages
                messageEntities.add(respMsg);
            }
        }
        // save the response and respective messages
        savePgrAndMsgs(dctx, pgCredit, messageEntities);

        if (creditResponse != null && creditResponse.booleanValue()) {
            paymentPref.set("statusId", "PAYMENT_CANCELLED");
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem storing updated payment preference; authorization was credit!", module);
            }
            // cancel any payment records
            List<GenericValue> paymentList = null;
            try {
                paymentList = paymentPref.getRelated("Payment", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get Payment records from OrderPaymentPreference : " + paymentPref, module);
            }
            if (paymentList != null) {
                Iterator<GenericValue> pi = paymentList.iterator();
                while (pi.hasNext()) {
                    GenericValue pay = pi.next();
                    try {
                        Map<String, Object> cancelResults = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("userLogin", userLogin, "paymentId", pay.get("paymentId"), "statusId", "PMNT_CANCELLED"));
                        if (ServiceUtil.isError(cancelResults)) {
                            throw new GenericServiceException(ServiceUtil.getErrorMessage(cancelResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Unable to cancel Payment : " + pay, module);
                    }
                }
            }
        } else {
            Debug.logError("Credit failed for pref : " + paymentPref, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, 
                    "AccountingTroubleCallingCreditOrderPaymentPreferenceService", 
                    UtilMisc.toMap("paymentPref", paymentPref), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     *
     * Releases authorization for a single OrderPaymentPreference through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return SUCCESS|FAILED|ERROR for complete processing of payment.
     */
    public static Map<String, Object> releaseOrderPaymentPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            paymentPref = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problem getting OrderPaymentPreference for orderPaymentPreferenceId " + 
                    orderPaymentPreferenceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }
        // Error if no OrderPaymentPreference was found
        if (paymentPref == null) {
            Debug.logWarning("Could not find OrderPaymentPreference with orderPaymentPreferenceId: " + 
                    orderPaymentPreferenceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }
        // Get the OrderHeader
        GenericValue orderHeader = null;
        String orderId = paymentPref.getString("orderId");
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problem getting OrderHeader for orderId " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }
        // Error if no OrderHeader was found
        if (orderHeader == null) {
            Debug.logWarning("Could not find OrderHeader with orderId: " + 
                    orderId + "; not processing payments.", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String currency = orh.getCurrency();
        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;
        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orderHeader, paymentPref, RELEASE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString("paymentService");
            }
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");
            if (serviceName == null) {
                Debug.logWarning("No payment release service for - " + paymentPref.getString("paymentMethodTypeId"), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                        "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale) + " " + 
                        paymentPref.getString("paymentMethodTypeId"));
            }
        } else {
            Debug.logWarning("No payment release settings found for - " + paymentPref.getString("paymentMethodTypeId"), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale) + " " + 
                    paymentPref.getString("paymentMethodTypeId"));
        }
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
        Map<String, Object> releaseContext = new HashMap<String, Object>();
        releaseContext.put("orderPaymentPreference", paymentPref);
        releaseContext.put("releaseAmount", authTransaction.getBigDecimal("amount"));
        releaseContext.put("currency", currency);
        releaseContext.put("paymentConfig", paymentConfig);
        releaseContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
        releaseContext.put("userLogin", userLogin);
        // run the defined service
        Map<String, Object> releaseResult = null;
        try {
            releaseResult = dispatcher.runSync(serviceName, releaseContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e,"Problem releasing payment", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale));
        }
        // get the release result code
        if (releaseResult != null && !ServiceUtil.isError(releaseResult)) {
            Map<String, Object> releaseResRes;
            try {
                ModelService model = dctx.getModelService("processReleaseResult");
                releaseResult.put("orderPaymentPreference", paymentPref);
                releaseResult.put("userLogin", userLogin);
                Map<String, Object> resCtx = model.makeValid(releaseResult, ModelService.IN_PARAM);
                releaseResRes = dispatcher.runSync(model.name,  resCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Trouble processing the release results", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                        "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale) + " " +
                        e.getMessage());
            }
            if (releaseResRes != null && ServiceUtil.isError(releaseResRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResRes));
            }
        } else if (ServiceUtil.isError(releaseResult)) {
            saveError(dispatcher, userLogin, paymentPref, releaseResult, RELEASE_SERVICE_TYPE, "PGT_RELEASE");
            result = ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
        }
        return result;
    }

    public static Map<String, Object> processReleaseResult(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String currencyUomId = (String) context.get("currencyUomId");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Boolean releaseResponse = (Boolean) context.get("releaseResult");
        Locale locale = (Locale) context.get("locale");
        // create the PaymentGatewayResponse
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue pgResponse = delegator.makeValue("PaymentGatewayResponse");
        pgResponse.set("paymentGatewayResponseId", responseId);
        pgResponse.set("paymentServiceTypeEnumId", RELEASE_SERVICE_TYPE);
        pgResponse.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        pgResponse.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        pgResponse.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        pgResponse.set("transCodeEnumId", "PGT_RELEASE");
        // set the release info
        pgResponse.set("amount", context.get("releaseAmount"));
        pgResponse.set("referenceNum", context.get("releaseRefNum"));
        pgResponse.set("altReference", context.get("releaseAltRefNum"));
        pgResponse.set("gatewayCode", context.get("releaseCode"));
        pgResponse.set("gatewayFlag", context.get("releaseFlag"));
        pgResponse.set("gatewayMessage", context.get("releaseMessage"));
        pgResponse.set("transactionDate", UtilDateTime.nowTimestamp());
        pgResponse.set("currencyUomId", currencyUomId);
        // store the gateway response
        savePgr(dctx, pgResponse);
        // create the internal messages
        List<String> messages = UtilGenerics.cast(context.get("internalRespMsgs"));
        if (UtilValidate.isNotEmpty(messages)) {
            Iterator<String> i = messages.iterator();
            while (i.hasNext()) {
                GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg");
                String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                String message = i.next();
                respMsg.set("paymentGatewayRespMsgId", respMsgId);
                respMsg.set("paymentGatewayResponseId", responseId);
                respMsg.set("pgrMessage", message);
                // store the messages
                savePgr(dctx, respMsg);
            }
        }

        if (releaseResponse != null && releaseResponse.booleanValue()) {
            paymentPref.set("statusId", "PAYMENT_CANCELLED");
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem storing updated payment preference; authorization was released!", module);
            }
            // cancel any payment records
            List<GenericValue> paymentList = null;
            try {
                paymentList = paymentPref.getRelated("Payment", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get Payment records from OrderPaymentPreference : " + paymentPref, module);
            }
            if (paymentList != null) {
                Iterator<GenericValue> pi = paymentList.iterator();
                while (pi.hasNext()) {
                    GenericValue pay = pi.next();
                    try {
                        Map<String, Object> cancelResults = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("userLogin", userLogin, "paymentId", pay.get("paymentId"), "statusId", "PMNT_CANCELLED"));
                        if (ServiceUtil.isError(cancelResults)) {
                            throw new GenericServiceException(ServiceUtil.getErrorMessage(cancelResults));
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Unable to cancel Payment : " + pay, module);
                    }
                }
            }
        } else {
            Debug.logError("Release failed for pref : " + paymentPref, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceOrder, 
                    "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale) + " " +
                    paymentPref);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> capturePaymentsByInvoice(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        Locale locale = (Locale) context.get("locale");

        // lookup the invoice
        GenericValue invoice = null;
        try {
            invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble looking up Invoice #" + invoiceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
        }

        if (invoice == null) {
            Debug.logError("Could not locate invoice #" + invoiceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
        }

        // get the OrderItemBilling records for this invoice
        List<GenericValue> orderItemBillings = null;
        try {
            orderItemBillings = invoice.getRelated("OrderItemBilling", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError("Trouble getting OrderItemBilling(s) from Invoice #" + invoiceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemLookingUpOrderItemBilling", 
                    UtilMisc.toMap("billFields", invoiceId), locale));
        }

        // check for an associated billing account
        String billingAccountId = invoice.getString("billingAccountId");

        // make sure they are all for the same order
        String testOrderId = null;
        boolean allSameOrder = true;
        if (orderItemBillings != null) {
            Iterator<GenericValue> oii = orderItemBillings.iterator();
            while (oii.hasNext()) {
                GenericValue oib = oii.next();
                String orderId = oib.getString("orderId");
                if (testOrderId == null) {
                    testOrderId = orderId;
                } else {
                    if (!orderId.equals(testOrderId)) {
                        allSameOrder = false;
                        break;
                    }
                }
            }
        }

        if (testOrderId == null || !allSameOrder) {
            Debug.logWarning("Attempt to settle Invoice #" + invoiceId + " which contained none/multiple orders", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, 
                    "AccountingInvoiceCannotBeSettle", 
                    UtilMisc.toMap("invoiceId", invoiceId), locale));
        }

        // get the invoice amount (amount to bill)
        BigDecimal invoiceTotal = InvoiceWorker.getInvoiceNotApplied(invoice);
        if (Debug.infoOn()) Debug.logInfo("(Capture) Invoice [#" + invoiceId + "] total: " + invoiceTotal, module);

        // now capture the order
        Map<String, Object> serviceContext = UtilMisc.toMap("userLogin", userLogin, "orderId", testOrderId, "invoiceId", invoiceId, "captureAmount", invoiceTotal);
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            serviceContext.put("billingAccountId", billingAccountId);
        }
        try {
            return dispatcher.runSync("captureOrderPayments", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Trouble running captureOrderPayments service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentCannotBeCaptured", locale));
        }
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map<String, Object> captureOrderPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        BigDecimal amountToCapture = (BigDecimal) context.get("captureAmount");
        Locale locale = (Locale) context.get("locale");
        amountToCapture = amountToCapture.setScale(decimals, rounding);

        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List<GenericValue> paymentPrefs = null;
        List<GenericValue> paymentPrefsBa = null;

        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            // get the payment prefs
            paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where("orderId", orderId, "statusId", "PAYMENT_AUTHORIZED").orderBy("-maxAmount").queryList();

            if (UtilValidate.isNotEmpty(billingAccountId)) {
                paymentPrefsBa = EntityQuery.use(delegator).from("OrderPaymentPreference")
                        .where("orderId", orderId, "paymentMethodTypeId", "EXT_BILLACT", "statusId", "PAYMENT_NOT_RECEIVED")
                        .orderBy("-maxAmount").queryList();
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting entity record(s), see stack trace", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale) + " " + gee.toString());
        }

        // error if no order was found
        if (orderHeader == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }

        // Check if the outstanding amount for the order is greater than the
        // amount that we are going to capture.
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        BigDecimal orderGrandTotal = orh.getOrderGrandTotal();
        orderGrandTotal = orderGrandTotal.setScale(decimals, rounding);
        BigDecimal totalPayments = PaymentWorker.getPaymentsTotal(orh.getOrderPayments());
        totalPayments = totalPayments.setScale(decimals, rounding);
        BigDecimal remainingTotal = orderGrandTotal.subtract(totalPayments);
        if (Debug.infoOn()) {
            Debug.logInfo("The Remaining Total for order: " + orderId + " is: " + remainingTotal, module);
        }
        // The amount to capture cannot be greater than the remaining total
        amountToCapture = amountToCapture.min(remainingTotal);
        if (Debug.infoOn()) {
            Debug.logInfo("Actual Expected Capture Amount : " + amountToCapture, module);
        }
        // Process billing accounts payments
        if (UtilValidate.isNotEmpty(paymentPrefsBa)) {
            Iterator<GenericValue> paymentsBa = paymentPrefsBa.iterator();
            while (paymentsBa.hasNext()) {
                GenericValue paymentPref = paymentsBa.next();

                BigDecimal authAmount = paymentPref.getBigDecimal("maxAmount");
                if (authAmount == null) authAmount = ZERO;
                authAmount = authAmount.setScale(decimals, rounding);

                if (authAmount.compareTo(ZERO) == 0) {
                    // nothing to capture
                    Debug.logInfo("Nothing to capture; authAmount = 0", module);
                    continue;
                }
                // the amount for *this* capture
                BigDecimal amountThisCapture = amountToCapture.min(authAmount);

                // decrease amount of next payment preference to capture
                amountToCapture = amountToCapture.subtract(amountThisCapture);

                // If we have an invoice, we find unapplied payments associated
                // to the billing account and we apply them to the invoice
                if (UtilValidate.isNotEmpty(invoiceId)) {
                    Map<String, Object> captureResult = null;
                    try {
                        captureResult = dispatcher.runSync("captureBillingAccountPayments", UtilMisc.<String, Object>toMap("invoiceId", invoiceId,
                                                                                                          "billingAccountId", billingAccountId,
                                                                                                          "captureAmount", amountThisCapture,
                                                                                                          "orderId", orderId,
                                                                                                          "userLogin", userLogin));
                        if (ServiceUtil.isError(captureResult)) {
                            return captureResult;
                        }
                    } catch (GenericServiceException ex) {
                        return ServiceUtil.returnError(ex.getMessage());
                    }
                    if (captureResult != null) {

                        BigDecimal amountCaptured = (BigDecimal) captureResult.get("captureAmount");
                        if (Debug.infoOn()) Debug.logInfo("Amount captured for order [" + orderId + "] from unapplied payments associated to billing account [" + billingAccountId + "] is: " + amountCaptured, module);

                        amountCaptured = amountCaptured.setScale(decimals, rounding);

                        if (amountCaptured.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                        // add the invoiceId to the result for processing
                        captureResult.put("invoiceId", invoiceId);
                        captureResult.put("captureResult", Boolean.TRUE);
                        captureResult.put("orderPaymentPreference", paymentPref);
                        if (context.get("captureRefNum") == null) {
                            captureResult.put("captureRefNum", ""); // FIXME: this is an hack to avoid a service validation error for processCaptureResult (captureRefNum is mandatory, but it is not used for billing accounts)
                        }                                                

                        // process the capture's results
                        try {
                            // the following method will set on the OrderPaymentPreference:
                            // maxAmount = amountCaptured and
                            // statusId = PAYMENT_RECEIVED
                            processResult(dctx, captureResult, userLogin, paymentPref, locale);
                        } catch (GeneralException e) {
                            Debug.logError(e, "Trouble processing the result; captureResult: " + captureResult, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                                    "AccountingPaymentCannotBeCaptured", locale) + " " + captureResult);
                        }

                        // create any splits which are needed
                        if (authAmount.compareTo(amountCaptured) > 0) {
                            BigDecimal splitAmount = authAmount.subtract(amountCaptured);
                            try {
                                Map<String, Object> splitCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "orderPaymentPreference", paymentPref, "splitAmount", splitAmount);
                                dispatcher.addCommitService("processCaptureSplitPayment", splitCtx, true);
                            } catch (GenericServiceException e) {
                                Debug.logWarning(e, "Problem processing the capture split payment", module);
                            }
                            if (Debug.infoOn()) Debug.logInfo("Captured: " + amountThisCapture + " Remaining (re-auth): " + splitAmount, module);
                        }
                    } else {
                        Debug.logError("Payment not captured for order [" + orderId + "] from billing account [" + billingAccountId + "]", module);
                    }
                }
            }
        }

        // iterate over the prefs and capture each one until we meet our total
        if (UtilValidate.isNotEmpty(paymentPrefs)) {
            Iterator<GenericValue> payments = paymentPrefs.iterator();
            while (payments.hasNext()) {
                // DEJ20060708: Do we really want to just log and ignore the errors like this? I've improved a few of these in a review today, but it is being done all over...
                GenericValue paymentPref = payments.next();
                GenericValue authTrans = getAuthTransaction(paymentPref);
                if (authTrans == null) {
                    Debug.logWarning("Authorized OrderPaymentPreference has no corresponding PaymentGatewayResponse, cannot capture payment: " + paymentPref, module);
                    continue;
                }

                // check for an existing capture
                GenericValue captureTrans = getCaptureTransaction(paymentPref);
                if (captureTrans != null) {
                    Debug.logWarning("Attempt to capture and already captured preference: " + captureTrans, module);
                    continue;
                }

                BigDecimal authAmount = authTrans.getBigDecimal("amount");
                if (authAmount == null) authAmount = ZERO;
                authAmount = authAmount.setScale(decimals, rounding);

                if (authAmount.compareTo(ZERO) == 0) {
                    // nothing to capture
                    Debug.logInfo("Nothing to capture; authAmount = 0", module);
                    continue;
                }

                // the amount for *this* capture
                BigDecimal amountThisCapture;

                // determine how much for *this* capture
                if (isReplacementOrder(orderHeader)) {
                    // if it is a replacement order then just capture the auth amount
                    amountThisCapture = authAmount;
                } else if (authAmount.compareTo(amountToCapture) >= 0) {
                    // if the auth amount is more then expected capture just capture what is expected
                    amountThisCapture = amountToCapture;
                } else if (payments.hasNext()) {
                    // if we have more payments to capture; just capture what was authorized
                    amountThisCapture = authAmount;
                } else {
                    // we need to capture more then what was authorized; re-auth for the new amount
                    // TODO: add what the billing account cannot support to the re-auth amount
                    // TODO: add support for re-auth for additional funds
                    // just in case; we will capture the authorized amount here; until this is implemented
                    Debug.logError("The amount to capture was more then what was authorized; we only captured the authorized amount : " + paymentPref, module);
                    amountThisCapture = authAmount;
                }

                Map<String, Object> captureResult = capturePayment(dctx, userLogin, orh, paymentPref, amountThisCapture, locale);
                if (captureResult != null && !ServiceUtil.isError(captureResult)) {
                    // credit card processors return captureAmount, but gift certificate processors return processAmount
                    BigDecimal amountCaptured = (BigDecimal) captureResult.get("captureAmount");
                    if (amountCaptured == null) {
                        amountCaptured = (BigDecimal) captureResult.get("processAmount");
                    }

                    amountCaptured = amountCaptured.setScale(decimals, rounding);

                    // decrease amount of next payment preference to capture
                    amountToCapture = amountToCapture.subtract(amountCaptured);

                    // add the invoiceId to the result for processing, not for a replacement order
                    if (!isReplacementOrder(orderHeader)) {
                        captureResult.put("invoiceId", invoiceId);
                    }

                    // process the capture's results
                    try {
                        processResult(dctx, captureResult, userLogin, paymentPref, locale);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Trouble processing the result; captureResult: " + captureResult, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                                "AccountingPaymentCannotBeCaptured", locale) + " " + captureResult);
                    }

                    // create any splits which are needed
                    if (authAmount.compareTo(amountCaptured) > 0) {
                        BigDecimal splitAmount = authAmount.subtract(amountCaptured);
                        try {
                            Map<String, Object> splitCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "orderPaymentPreference", paymentPref, "splitAmount", splitAmount);
                            dispatcher.addCommitService("processCaptureSplitPayment", splitCtx, true);
                        } catch (GenericServiceException e) {
                            Debug.logWarning(e, "Problem processing the capture split payment", module);
                        }
                        if (Debug.infoOn()) Debug.logInfo("Captured: " + amountThisCapture + " Remaining (re-auth): " + splitAmount, module);
                    }
                } else {
                    Debug.logError("Payment not captured", module);
                }
            }
        }

        if (amountToCapture.compareTo(ZERO) > 0) {
            GenericValue productStore = orh.getProductStore();
            if (!UtilValidate.isEmpty(productStore)) {
                boolean shipIfCaptureFails = UtilValidate.isEmpty(productStore.get("shipIfCaptureFails")) || "Y".equalsIgnoreCase(productStore.getString("shipIfCaptureFails"));
                if (! shipIfCaptureFails) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                            "AccountingPaymentCannotBeCaptured", locale));
                } else {
                    Debug.logWarning("Payment capture failed, shipping order anyway as per ProductStore setting (shipIfCaptureFails)", module);
                }
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("processResult", "FAILED");
            return result;
        } else {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("processResult", "COMPLETE");
            return result;
        }
    }

    public static Map<String, Object> processCaptureSplitPayment(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal splitAmount = (BigDecimal) context.get("splitAmount");

        String orderId = paymentPref.getString("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);

        String statusId = "PAYMENT_NOT_AUTH";
        if ("EXT_BILLACT".equals(paymentPref.getString("paymentMethodTypeId"))) {
            statusId = "PAYMENT_NOT_RECEIVED";
        } else if ("EXT_PAYPAL".equals(paymentPref.get("paymentMethodTypeId"))) {
            statusId = "PAYMENT_AUTHORIZED";
        }
        // create a new payment preference
        Debug.logInfo("Creating payment preference split", module);
        String newPrefId = delegator.getNextSeqId("OrderPaymentPreference");
        GenericValue newPref = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", newPrefId));
        newPref.set("orderId", paymentPref.get("orderId"));
        newPref.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        newPref.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        newPref.set("maxAmount", splitAmount);
        newPref.set("statusId", statusId);
        newPref.set("createdDate", UtilDateTime.nowTimestamp());
        if (userLogin != null) {
            newPref.set("createdByUserLogin", userLogin.getString("userLoginId"));
        }
        if (Debug.verboseOn()) Debug.logVerbose("New preference : " + newPref, module);

        Map<String, Object> processorResult = null;
        try {
            // create the new payment preference
            delegator.create(newPref);

            // PayPal requires us to reuse the existing authorization, so we'll
            // fake it and copy the existing auth with the remaining amount
            if ("EXT_PAYPAL".equals(paymentPref.get("paymentMethodTypeId"))) {
                String newAuthId = delegator.getNextSeqId("PaymentGatewayResponse");
                GenericValue authTrans = getAuthTransaction(paymentPref);
                GenericValue newAuthTrans = delegator.makeValue("PaymentGatewayResponse", authTrans);
                newAuthTrans.set("paymentGatewayResponseId", newAuthId);
                newAuthTrans.set("orderPaymentPreferenceId", newPref.get("orderPaymentPreferenceId"));
                newAuthTrans.set("amount", splitAmount);
                savePgr(dctx, newAuthTrans);
            } else if ("PAYMENT_NOT_AUTH".equals(statusId)) {
                // authorize the new preference
                processorResult = authPayment(dispatcher, userLogin, orh, newPref, splitAmount, false, null);
                if (processorResult != null) {
                    // process the auth results
                    boolean authResult = processResult(dctx, processorResult, userLogin, newPref, locale);
                    if (!authResult) {
                        Debug.logError("Authorization failed : " + newPref + " : " + processorResult, module);
                    }
                } else {
                    Debug.logError("Payment not authorized : " + newPref + " : " + processorResult, module);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: cannot create new payment preference : " + newPref, module);
        } catch (GeneralException e) {
            if (processorResult != null) {
                Debug.logError(e, "Trouble processing the auth result: " + newPref + " : " + processorResult, module);
            } else {
                Debug.logError(e, "Trouble authorizing the payment: " + newPref, module);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    // Deprecated: use captureBillingAccountPayments instead of this.
    public static Map<String, Object> captureBillingAccountPayment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        BigDecimal captureAmount = (BigDecimal) context.get("captureAmount");
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            // Note that the partyIdFrom of the Payment should be the partyIdTo of the invoice, since you're receiving a payment from the party you billed
            GenericValue invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
            Map<String, Object> paymentParams = UtilMisc.<String, Object>toMap("paymentTypeId", "CUSTOMER_PAYMENT", "paymentMethodTypeId", "EXT_BILLACT",
                    "partyIdFrom", invoice.getString("partyId"), "partyIdTo", invoice.getString("partyIdFrom"),
                    "statusId", "PMNT_RECEIVED", "effectiveDate", UtilDateTime.nowTimestamp());
            paymentParams.put("amount", captureAmount);
            paymentParams.put("currencyUomId", invoice.getString("currencyUomId"));
            paymentParams.put("userLogin", userLogin);
            Map<String, Object> tmpResult = dispatcher.runSync("createPayment", paymentParams);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            String paymentId = (String) tmpResult.get("paymentId");
            tmpResult = dispatcher.runSync("createPaymentApplication", UtilMisc.<String, Object>toMap("paymentId", paymentId, "invoiceId", invoiceId, "billingAccountId", billingAccountId,
                    "amountApplied", captureAmount, "userLogin", userLogin));
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }
            if (paymentId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingNoPaymentCreatedForInvoice", 
                        UtilMisc.toMap("invoiceId", invoiceId, "billingAccountId", billingAccountId), locale));
            }
            results.put("paymentId", paymentId);

            if (orderId != null && captureAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Create a paymentGatewayResponse, if necessary
                GenericValue order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                if (order == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingNoPaymentGatewayResponseCreatedForInvoice", 
                            UtilMisc.toMap("invoiceId", invoiceId, "billingAccountId", billingAccountId,
                                    "orderId", orderId), locale));
                }
                // See if there's an orderPaymentPreference - there should be only one OPP for EXT_BILLACT per order
                GenericValue orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId, "paymentMethodTypeId", "EXT_BILLACT").queryFirst();
                if (orderPaymentPreference != null) {

                    // Check the productStore setting to see if we need to do this explicitly
                    GenericValue productStore = order.getRelatedOne("ProductStore", false);
                    if (productStore.getString("manualAuthIsCapture") == null || (! productStore.getString("manualAuthIsCapture").equalsIgnoreCase("Y"))) {
                        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
                        GenericValue pgResponse = delegator.makeValue("PaymentGatewayResponse");
                        pgResponse.set("paymentGatewayResponseId", responseId);
                        pgResponse.set("paymentServiceTypeEnumId", CAPTURE_SERVICE_TYPE);
                        pgResponse.set("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                        pgResponse.set("paymentMethodTypeId", "EXT_BILLACT");
                        pgResponse.set("transCodeEnumId", "PGT_CAPTURE");
                        pgResponse.set("amount", captureAmount);
                        pgResponse.set("currencyUomId", invoice.getString("currencyUomId"));
                        pgResponse.set("transactionDate", UtilDateTime.nowTimestamp());
                        // referenceNum holds the relation to the order.
                        // todo: Extend PaymentGatewayResponse with a billingAccountId field?
                        pgResponse.set("referenceNum", billingAccountId);

                        // save the response
                        savePgr(dctx, pgResponse);

                        // Update the orderPaymentPreference
                        orderPaymentPreference.set("statusId", "PAYMENT_SETTLED");
                        orderPaymentPreference.store();

                        results.put("paymentGatewayResponseId", responseId);
                    }
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return results;
    }

    public static Map<String, Object> captureBillingAccountPayments(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        BigDecimal captureAmount = (BigDecimal) context.get("captureAmount");
        captureAmount = captureAmount.setScale(decimals, rounding);
        BigDecimal capturedAmount = BigDecimal.ZERO;

        try {
            // Select all the unapplied payment applications associated to the billing account
            List<GenericValue> paymentApplications = EntityQuery.use(delegator).from("PaymentApplication")
                    .where("billingAccountId", billingAccountId, "invoiceId", null)
                    .orderBy("-amountApplied").queryList();
            if (UtilValidate.isNotEmpty(paymentApplications)) {
                Iterator<GenericValue> paymentApplicationsIt = paymentApplications.iterator();
                while (paymentApplicationsIt.hasNext()) {
                    if (capturedAmount.compareTo(captureAmount) >= 0) {
                        // we have captured all the amount required
                        break;
                    }
                    GenericValue paymentApplication = paymentApplicationsIt.next();
                    GenericValue payment = paymentApplication.getRelatedOne("Payment", false);
                    if (payment.getString("paymentPreferenceId") != null) {
                        // if the payment is reserved for a specific OrderPaymentPreference,
                        // we don't use it.
                        continue;
                    }
                    // TODO: check the statusId of the payment
                    BigDecimal paymentApplicationAmount = paymentApplication.getBigDecimal("amountApplied");
                    BigDecimal amountToCapture = paymentApplicationAmount.min(captureAmount.subtract(capturedAmount));
                    amountToCapture = amountToCapture.setScale(decimals, rounding);
                    if (amountToCapture.compareTo(paymentApplicationAmount) == 0) {
                        // apply the whole payment application to the invoice
                        paymentApplication.set("invoiceId", invoiceId);
                        paymentApplication.store();
                    } else {
                        // the amount to capture is lower than the amount available in this payment application:
                        // split the payment application into two records and apply one to the invoice
                        GenericValue newPaymentApplication = delegator.makeValue("PaymentApplication", paymentApplication);
                        String paymentApplicationId = delegator.getNextSeqId("PaymentApplication");
                        paymentApplication.set("invoiceId", invoiceId);
                        paymentApplication.set("amountApplied", amountToCapture);
                        paymentApplication.store();
                        newPaymentApplication.set("paymentApplicationId", paymentApplicationId);
                        newPaymentApplication.set("amountApplied", paymentApplicationAmount.subtract(amountToCapture));
                        newPaymentApplication.create();
                    }
                    capturedAmount = capturedAmount.add(amountToCapture);
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        capturedAmount = capturedAmount.setScale(decimals, rounding);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("captureAmount", capturedAmount);
        return results;
    }

    private static Map<String, Object> capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh, 
            GenericValue paymentPref, BigDecimal amount, Locale locale) {
        return capturePayment(dctx, userLogin, orh, paymentPref, amount, null, locale);
    }

    private static Map<String, Object> capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh, 
            GenericValue paymentPref, BigDecimal amount, GenericValue authTrans, Locale locale) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;

        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPref, CAPTURE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString("paymentService");
            }
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");

            if (serviceName == null) {
                Debug.logError("Service name is null for payment setting; cannot process", module);
                return null;
            }
        } else {
            Debug.logError("Invalid payment settings entity, no payment settings found", module);
            return null;
        }

        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }

        // check the validity of the authorization; re-auth if necessary
        if (!PaymentGatewayServices.checkAuthValidity(paymentPref, paymentConfig)) {
            try {
                // re-auth required before capture
                Map<String, Object> processorResult = PaymentGatewayServices.authPayment(dispatcher, userLogin, orh, paymentPref, amount, true, null);

                boolean authResult = false;
                if (processorResult != null) {
                    // process the auth results
                    try {
                        authResult = processResult(dctx, processorResult, userLogin, paymentPref, locale);
                        if (!authResult) {
                            Debug.logError("Re-Authorization failed : " + paymentPref + " : " + processorResult, module);
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, "Trouble processing the re-auth result : " + paymentPref + " : " + processorResult, module);
                    }
                } else {
                    Debug.logError("Payment not re-authorized : " + paymentPref + " : " + processorResult, module);
                }

                if (!authResult) {
                    // returning null to cancel the capture process.
                    return null;
                }

                // get the new auth transaction
                authTrans = getAuthTransaction(paymentPref);
            } catch (GeneralException e) {
                Debug.logError(e, "Error re-authorizing payment", module);
                return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPaymentReauthorizingError", locale));
            }
        }

        // prepare the context for the capture service (must follow the ccCaptureInterface
        Map<String, Object> captureContext = new HashMap<String, Object>();
        captureContext.put("userLogin", userLogin);
        captureContext.put("orderPaymentPreference", paymentPref);
        captureContext.put("paymentConfig", paymentConfig);
        captureContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
        captureContext.put("currency", orh.getCurrency());

        // this is necessary because the ccCaptureInterface uses "captureAmount" but the paymentProcessInterface uses "processAmount"
        try {
            ModelService captureService = dctx.getModelService(serviceName);
            Set<String> inParams = captureService.getInParamNames();
            if (inParams.contains("captureAmount")) {
                captureContext.put("captureAmount", amount);
            } else if (inParams.contains("processAmount")) {
                captureContext.put("processAmount", amount);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentServiceMissingAmount", 
                        UtilMisc.toMap("serviceName", serviceName, "inParams", inParams), locale));
            }
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentServiceCannotGetModel", 
                    UtilMisc.toMap("serviceName", serviceName), locale));
        }


        if (authTrans != null) {
            captureContext.put("authTrans", authTrans);
        }

        if (Debug.infoOn()) Debug.logInfo("Capture [" + serviceName + "] : " + captureContext, module);
        try {
            String paymentMethodTypeId = paymentPref.getString("paymentMethodTypeId");
            if (paymentMethodTypeId != null && "GIFT_CARD".equals(paymentMethodTypeId)) {
                getBillingInformation(orh, paymentPref, captureContext);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        // now invoke the capture service
        Map<String, Object> captureResult = null;
        try {
            // NOTE DEJ20070819 calling this with a new transaction synchronously caused a deadlock because in this
            //transaction OrderHeader was updated and with this transaction paused and waiting for the new transaction
            //and the new transaction was waiting trying to read the same OrderHeader record; note that this only happens
            //for FinAccounts because they are processed internally whereas others are not
            // NOTE HOW TO FIX: don't call in separate transaction from here; individual services can have require-new-transaction
            //set to true if they want to behave that way (had: [, TX_TIME, true])
            captureResult = dispatcher.runSync(serviceName, captureContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Could not capture payment ... serviceName: " + serviceName + " ... context: " + captureContext, module);
            return null;
        }

        // pass the payTo partyId to the result processor; we just add it to the result context.
        String payToPartyId = getPayToPartyId(orh.getOrderHeader());
        captureResult.put("payToPartyId", payToPartyId);

        // add paymentSettings to result; for use by later processors
        captureResult.put("paymentSettings", paymentSettings);

        // pass the currencyUomId as well
        captureResult.put("currencyUomId", orh.getCurrency());

        // log the error message as a gateway response when it fails
        if (ServiceUtil.isError(captureResult)) {
            saveError(dispatcher, userLogin, paymentPref, captureResult, CAPTURE_SERVICE_TYPE, "PGT_CAPTURE");
        }

        return captureResult;
    }

    private static void saveError(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPref, Map<String, Object> result, String serviceType, String transactionCode) {
        Map<String, Object> serviceContext = new HashMap<String, Object>();
        serviceContext.put("paymentServiceTypeEnumId", serviceType);
        serviceContext.put("orderPaymentPreference", paymentPref);
        serviceContext.put("transCodeEnumId", transactionCode);
        serviceContext.put("serviceResultMap", result);
        serviceContext.put("userLogin", userLogin);

        try {
            dispatcher.runAsync("processPaymentServiceError", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
    }

    public static Map<String, Object> storePaymentErrorMessage(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String serviceType = (String) context.get("paymentServiceTypeEnumId");
        String transactionCode = (String) context.get("transCodeEnumId");
        Map<String, Object> result = UtilGenerics.cast(context.get("serviceResultMap"));
        Locale locale = (Locale) context.get("locale");
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse");
        String message = ServiceUtil.getErrorMessage(result);
        if (message.length() > 255) {
            message = message.substring(0, 255);
        }
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", serviceType);
        response.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        response.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        response.set("transCodeEnumId", transactionCode);
        response.set("referenceNum", "ERROR");
        response.set("gatewayMessage", message);
        response.set("transactionDate", UtilDateTime.nowTimestamp());

        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingNoPaymentGatewayResponseCreatedForFailedService", locale));
        }

        Debug.logInfo("Created PaymentGatewayResponse record for returned error", module);
        return ServiceUtil.returnSuccess();
    }

    private static boolean processResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, 
            GenericValue paymentPreference, Locale locale) throws GeneralException {
        Boolean authResult = (Boolean) result.get("authResult");
        Boolean captureResult = (Boolean) result.get("captureResult");
        boolean resultPassed = false;
        String initialStatus = paymentPreference.getString("statusId");
        String authServiceType = null;

        if (authResult != null) {
            processAuthResult(dctx, result, userLogin, paymentPreference);
            resultPassed = authResult.booleanValue();
            authServiceType = ("PAYMENT_NOT_AUTH".equals(initialStatus)) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;
        }
        if (captureResult != null) {
            processCaptureResult(dctx, result, userLogin, paymentPreference, authServiceType, locale);
            if (!resultPassed)
                resultPassed = captureResult.booleanValue();
        }
        return resultPassed;
    }

    private static void processAuthResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        result.put("userLogin", userLogin);
        result.put("orderPaymentPreference", paymentPreference);
        ModelService model = dctx.getModelService("processAuthResult");
        Map<String, Object> context = model.makeValid(result, ModelService.IN_PARAM);

        // in case we rollback make sure this service gets called
        dispatcher.addRollbackService(model.name, context, true);

        // invoke the service
        Map<String, Object> resResp;
        try {
            resResp = dispatcher.runSync(model.name, context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw e;
        }
        if (ServiceUtil.isError(resResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(resResp));
        }
    }

    public static Map<String, Object> processAuthResult(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        Boolean authResult = (Boolean) context.get("authResult");
        String authType = (String) context.get("serviceTypeEnum");
        String currencyUomId = (String) context.get("currencyUomId");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");

        // refresh the payment preference
        try {
            orderPaymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // type of auth this was can be determined by the previous status
        if (UtilValidate.isEmpty(authType)) {
            authType = ("PAYMENT_NOT_AUTH".equals(orderPaymentPreference.getString("statusId"))) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;
        }

        try {
            String paymentMethodId = orderPaymentPreference.getString("paymentMethodId");
            GenericValue paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
            GenericValue creditCard = null;
            if (paymentMethod != null && "CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                creditCard = paymentMethod.getRelatedOne("CreditCard", false);
            }

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse");
            response.set("paymentGatewayResponseId", responseId);
            response.set("paymentServiceTypeEnumId", authType);
            response.set("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));
            response.set("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
            response.set("paymentMethodId", orderPaymentPreference.get("paymentMethodId"));
            response.set("transCodeEnumId", "PGT_AUTHORIZE");
            response.set("currencyUomId", currencyUomId);

            // set the avs/fraud result
            response.set("gatewayAvsResult", context.get("avsCode"));
            response.set("gatewayCvResult", context.get("cvCode"));
            response.set("gatewayScoreResult", context.get("scoreCode"));

            // set the auth info
            BigDecimal processAmount = (BigDecimal) context.get("processAmount");
            response.set("amount", processAmount);
            response.set("referenceNum", context.get("authRefNum"));
            response.set("altReference", context.get("authAltRefNum"));
            response.set("gatewayCode", context.get("authCode"));
            response.set("gatewayFlag", context.get("authFlag"));
            response.set("gatewayMessage", context.get("authMessage"));
            response.set("transactionDate", UtilDateTime.nowTimestamp());

            if (Boolean.TRUE.equals(context.get("resultDeclined"))) response.set("resultDeclined", "Y");
            if (Boolean.TRUE.equals(context.get("resultNsf"))) response.set("resultNsf", "Y");
            if (Boolean.TRUE.equals(context.get("resultBadExpire"))) response.set("resultBadExpire", "Y");
            if (Boolean.TRUE.equals(context.get("resultBadCardNumber"))) response.set("resultBadCardNumber", "Y");

            // create the internal messages
            List<GenericValue> messageEntities = new LinkedList<GenericValue>();
            List<String> messages = UtilGenerics.cast(context.get("internalRespMsgs"));
            if (UtilValidate.isNotEmpty(messages)) {
                Iterator<String> i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg");
                    String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                    String message = i.next();
                    respMsg.set("paymentGatewayRespMsgId", respMsgId);
                    respMsg.set("paymentGatewayResponseId", responseId);
                    respMsg.set("pgrMessage", message);
                    messageEntities.add(respMsg);
                }
            }

            // save the response and respective messages
            savePgrAndMsgs(dctx, response, messageEntities);

            if (response.getBigDecimal("amount").compareTo((BigDecimal)context.get("processAmount")) != 0) {
                Debug.logWarning("The authorized amount does not match the max amount : Response - " + response + " : result - " + context, module);
            }

            // set the status of the OrderPaymentPreference
            if (authResult.booleanValue()) {
                orderPaymentPreference.set("statusId", "PAYMENT_AUTHORIZED");
            } else if (!authResult.booleanValue()) {
                orderPaymentPreference.set("statusId", "PAYMENT_DECLINED");
            } else {
                orderPaymentPreference.set("statusId", "PAYMENT_ERROR");
            }

            // remove sensitive credit card data regardless of outcome
            orderPaymentPreference.set("securityCode", null);
            orderPaymentPreference.set("track2", null);

            boolean needsNsfRetry = needsNsfRetry(orderPaymentPreference, context, delegator);
            if (needsNsfRetry) {
                orderPaymentPreference.set("needsNsfRetry", "Y");
            } else {
                orderPaymentPreference.set("needsNsfRetry", "N");
            }

            orderPaymentPreference.store();

            // if the payment was declined and this is a CreditCard, save that information on the CreditCard entity
            if (!authResult.booleanValue()) {
                if (creditCard != null) {
                    Long consecutiveFailedAuths = creditCard.getLong("consecutiveFailedAuths");
                    if (consecutiveFailedAuths == null) {
                        creditCard.set("consecutiveFailedAuths", Long.valueOf(1));
                    } else {
                        creditCard.set("consecutiveFailedAuths", Long.valueOf(consecutiveFailedAuths.longValue() + 1));
                    }
                    creditCard.set("lastFailedAuthDate", nowTimestamp);

                    if (Boolean.TRUE.equals(context.get("resultNsf"))) {
                        Long consecutiveFailedNsf = creditCard.getLong("consecutiveFailedNsf");
                        if (consecutiveFailedNsf == null) {
                            creditCard.set("consecutiveFailedNsf", Long.valueOf(1));
                        } else {
                            creditCard.set("consecutiveFailedNsf", Long.valueOf(consecutiveFailedNsf.longValue() + 1));
                        }
                        creditCard.set("lastFailedNsfDate", nowTimestamp);
                    }
                    creditCard.store();
                }
            }

            // auth was successful, to clear out any failed auth or nsf info
            if (authResult.booleanValue()) {
                if ((creditCard != null) && (creditCard.get("lastFailedAuthDate") != null)) {
                    creditCard.set("consecutiveFailedAuths", Long.valueOf(0));
                    creditCard.set("lastFailedAuthDate", null);
                    creditCard.set("consecutiveFailedNsf", Long.valueOf(0));
                    creditCard.set("lastFailedNsfDate", null);
                    creditCard.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error updating payment status information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingPaymentStatusUpdatingError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    private static boolean needsNsfRetry(GenericValue orderPaymentPreference, Map<String, ? extends Object> processContext, Delegator delegator) throws GenericEntityException {
        boolean needsNsfRetry = false;
        if (Boolean.TRUE.equals(processContext.get("resultNsf"))) {
            // only track this for auto-orders, since we will only not fail and re-try on those
            GenericValue orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader", false);
            if (UtilValidate.isNotEmpty(orderHeader.getString("autoOrderShoppingListId"))) {
                GenericValue productStore = orderHeader.getRelatedOne("ProductStore", false);
                if ("Y".equals(productStore.getString("autoOrderCcTryLaterNsf"))) {
                    // one last condition: make sure there have been less than ProductStore.autoOrderCcTryLaterMax
                    //   PaymentGatewayResponse records with the same orderPaymentPreferenceId and paymentMethodId (just in case it has changed)
                    //   and that have resultNsf = Y, ie only consider other NSF responses
                    Long autoOrderCcTryLaterMax = productStore.getLong("autoOrderCcTryLaterMax");
                    if (autoOrderCcTryLaterMax != null) {
                        long failedTries = EntityQuery.use(delegator).from("PaymentGatewayResponse")
                            .where("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"),
                                    "paymentMethodId", orderPaymentPreference.get("paymentMethodId"),
                                    "resultNsf", "Y")
                            .queryCount();
                        if (failedTries < autoOrderCcTryLaterMax.longValue()) {
                            needsNsfRetry = true;
                        }
                    } else {
                        needsNsfRetry = true;
                    }
                }
            }
        }
        return needsNsfRetry;
    }

    private static GenericValue processAuthRetryResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        processAuthResult(dctx, result, userLogin, paymentPreference);
        return getAuthTransaction(paymentPreference);
    }

    private static void processCaptureResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, 
            GenericValue paymentPreference, Locale locale) throws GeneralException {
        processCaptureResult(dctx, result, userLogin, paymentPreference, null, locale);
    }

    private static void processCaptureResult(DispatchContext dctx, Map<String, Object> result, GenericValue userLogin, 
            GenericValue paymentPreference, String authServiceType, Locale locale) throws GeneralException {
        if (result == null) {
            throw new GeneralException("Null capture result sent to processCaptureResult; fatal error");
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Boolean captureResult = (Boolean) result.get("captureResult");
        BigDecimal amount = null;
        if (result.get("captureAmount") != null) {
            amount = (BigDecimal) result.get("captureAmount");
        } else if (result.get("processAmount") != null) {
            amount = (BigDecimal) result.get("processAmount");
            result.put("captureAmount", amount);
        }

        if (amount == null) {
            throw new GeneralException("Unable to process null capture amount");
        }

        // setup the amount big decimal
        amount = amount.setScale(decimals, rounding);

        result.put("orderPaymentPreference", paymentPreference);
        result.put("userLogin", userLogin);
        result.put("serviceTypeEnum", authServiceType);

        ModelService model = dctx.getModelService("processCaptureResult");
        Map<String, Object> context = model.makeValid(result, ModelService.IN_PARAM);
        Map<String, Object> capRes;
        try {
            capRes = dispatcher.runSync("processCaptureResult", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw e;
        }
        if (capRes != null && ServiceUtil.isError(capRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(capRes));
        }
        if (!captureResult.booleanValue()) {
            // capture returned false (error)
            try {
                processReAuthFromCaptureFailure(dctx, result, amount, userLogin, paymentPreference, locale);
            } catch (GeneralException e) {
                // just log this for now (same as previous implementation)
                Debug.logError(e, module);
            }
        }
    }

    private static void processReAuthFromCaptureFailure(DispatchContext dctx, Map<String, Object> result, BigDecimal amount, 
            GenericValue userLogin, GenericValue paymentPreference, Locale locale) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        // lookup the order header
        OrderReadHelper orh = null;
        try {
            GenericValue orderHeader = paymentPreference.getRelatedOne("OrderHeader", false);
            if (orderHeader != null)
                orh = new OrderReadHelper(orderHeader);
        } catch (GenericEntityException e) {
            throw new GeneralException("Problems getting OrderHeader; cannot re-auth the payment", e);
        }

        // make sure the order exists
        if (orh == null) {
            throw new GeneralException("No order found for payment preference #" + paymentPreference.get("orderPaymentPreferenceId"));
        }

        // set the re-auth amount
        if (amount == null) {
            amount = ZERO;
        }
        if (amount.compareTo(ZERO) == 0) {
            amount = paymentPreference.getBigDecimal("maxAmount");
            Debug.logInfo("resetting payment amount from 0.00 to correctMax amount", module);
        }
        Debug.logInfo("reauth with amount: " + amount, module);

        // first re-auth the card
        Map<String, Object> authPayRes = authPayment(dispatcher, userLogin, orh, paymentPreference, amount, true, null);
        if (authPayRes == null) {
            throw new GeneralException("Null result returned from payment re-authorization");
        }

        // check the auth-response
        Boolean authResp = (Boolean) authPayRes.get("authResult");
        Boolean capResp = (Boolean) authPayRes.get("captureResult");
        if (authResp != null && Boolean.TRUE.equals(authResp)) {
            GenericValue authTrans = processAuthRetryResult(dctx, authPayRes, userLogin, paymentPreference);
            // check if auto-capture was enabled; process if so
            if (capResp != null && capResp.booleanValue()) {
                processCaptureResult(dctx, result, userLogin, paymentPreference, locale);
            } else {
                // no auto-capture; do manual capture now
                Map<String, Object> capPayRes = capturePayment(dctx, userLogin, orh, paymentPreference, amount, authTrans, locale);
                if (capPayRes == null) {
                    throw new GeneralException("Problems trying to capture payment (null result)");
                }

                // process the capture result
                Boolean capPayResp = (Boolean) capPayRes.get("captureResult");
                if (capPayResp != null && capPayResp.booleanValue()) {
                    // process the capture result
                    processCaptureResult(dctx, capPayRes, userLogin, paymentPreference, locale);
                } else {
                    throw new GeneralException("Capture of authorized payment failed");
                }
            }
        } else {
            throw new GeneralException("Payment re-authorization failed");
        }
    }

    public static Map<String, Object> processCaptureResult(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue paymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        String payTo = (String) context.get("payToPartyId");
        BigDecimal amount = (BigDecimal) context.get("captureAmount");
        String serviceType = (String) context.get("serviceTypeEnum");
        String currencyUomId = (String) context.get("currencyUomId");
        boolean captureSuccessful = ((Boolean) context.get("captureResult")).booleanValue();

        String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");

        if (UtilValidate.isEmpty(serviceType)) {
            serviceType = CAPTURE_SERVICE_TYPE;
        }

        // refresh the payment preference
        try {
            paymentPreference.refresh();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the status and maxAmount
        String prefStatusId;
        if (captureSuccessful) {
            prefStatusId = "EXT_BILLACT".equals(paymentMethodTypeId) ? "PAYMENT_RECEIVED": "PAYMENT_SETTLED";
        } else {
            prefStatusId = "PAYMENT_DECLINED";
        }
        paymentPreference.set("statusId", prefStatusId);
        paymentPreference.set("maxAmount", amount);
        try {
            paymentPreference.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (!"EXT_BILLACT".equals(paymentMethodTypeId)) {
            // create the PaymentGatewayResponse record
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse");
            response.set("paymentGatewayResponseId", responseId);
            response.set("paymentServiceTypeEnumId", serviceType);
            response.set("orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
            response.set("paymentMethodTypeId", paymentMethodTypeId);
            response.set("paymentMethodId", paymentPreference.get("paymentMethodId"));
            response.set("transCodeEnumId", "PGT_CAPTURE");
            response.set("currencyUomId", currencyUomId);
            if (context.get("authRefNum") != null) {
                response.set("subReference", context.get("authRefNum"));
                response.set("altReference", context.get("authAltRefNum"));
            } else {
                response.set("altReference", context.get("captureAltRefNum"));
            }

            // set the capture info
            response.set("amount", amount);
            response.set("referenceNum", context.get("captureRefNum"));
            response.set("gatewayCode", context.get("captureCode"));
            response.set("gatewayFlag", context.get("captureFlag"));
            response.set("gatewayMessage", context.get("captureMessage"));
            response.set("transactionDate", UtilDateTime.nowTimestamp());

            // save the response
            savePgr(dctx, response);

            // create the internal messages
            List<String> messages = UtilGenerics.cast(context.get("internalRespMsgs"));
            if (UtilValidate.isNotEmpty(messages)) {
                Iterator<String> i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg");
                    String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                    String message = i.next();
                    respMsg.set("paymentGatewayRespMsgId", respMsgId);
                    respMsg.set("paymentGatewayResponseId", responseId);
                    respMsg.set("pgrMessage", message);

                    // save the message
                    savePgr(dctx, respMsg);
                }
            }

            // get the invoice
            GenericValue invoice = null;
            if (invoiceId != null) {
                try {
                    invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
                } catch (GenericEntityException e) {
                    String message = "Failed to process capture result:  Could not find invoice ["+invoiceId+"] due to entity error: " + e.getMessage();
                    Debug.logError(e, message, module);
                    return ServiceUtil.returnError(message);
                }
            }

            // determine the partyIdFrom for the payment, which is who made the payment
            String partyIdFrom = null;
            if (invoice != null) {
                // get the party from the invoice, which is the bill-to party (partyId)
                partyIdFrom = invoice.getString("partyId");
            } else {
                // otherwise get the party from the order's OrderRole
                String orderId = paymentPreference.getString("orderId");
                GenericValue orderRole = null;
                try {
                    orderRole = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "roleTypeId", "BILL_TO_CUSTOMER").queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (orderRole != null) {
                    partyIdFrom = orderRole.getString("partyId");
                }
            }

            // get the partyIdTo for the payment, which is who is receiving it
            String partyIdTo = null;
            if (!UtilValidate.isEmpty(payTo)) {
                // use input pay to party
                partyIdTo = payTo;
            } else if (invoice != null) {
                // ues the invoice partyIdFrom as the pay to party (which is who supplied the invoice)
                partyIdTo = invoice.getString("partyIdFrom");
            } else {
                // otherwise default to Company and print a big warning about this
                partyIdTo = "Company";
                Debug.logWarning("Using default value of [" + partyIdTo + "] for payTo on invoice [" + invoiceId + "] and orderPaymentPreference [" +
                        paymentPreference.getString("orderPaymentPreferenceId") + "]", module);
            }


            Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap("paymentTypeId", "CUSTOMER_PAYMENT");
            paymentCtx.put("paymentMethodTypeId", paymentPreference.get("paymentMethodTypeId"));
            paymentCtx.put("paymentMethodId", paymentPreference.get("paymentMethodId"));
            paymentCtx.put("paymentGatewayResponseId", responseId);
            paymentCtx.put("partyIdTo", partyIdTo);
            paymentCtx.put("partyIdFrom", partyIdFrom);
            paymentCtx.put("statusId", "PMNT_RECEIVED");
            paymentCtx.put("paymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
            paymentCtx.put("amount", amount);
            paymentCtx.put("currencyUomId", currencyUomId);
            paymentCtx.put("userLogin", userLogin);
            paymentCtx.put("paymentRefNum", context.get("captureRefNum"));

            Map<String, Object> payRes;
            try {
                payRes = dispatcher.runSync("createPayment", paymentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreationError", locale));
            }
            if (ServiceUtil.isError(payRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(payRes));
            }

            String paymentId = (String) payRes.get("paymentId");

            // create the PaymentApplication if invoiceId is available
            if (invoiceId != null) {
                Debug.logInfo("Processing Invoice #" + invoiceId, module);
                Map<String, Object> paCtx = UtilMisc.<String, Object>toMap("paymentId", paymentId, "invoiceId", invoiceId);
                paCtx.put("amountApplied", context.get("captureAmount"));
                paCtx.put("userLogin", userLogin);
                Map<String, Object> paRes;
                try {
                    paRes = dispatcher.runSync("createPaymentApplication", paCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingInvoiceApplicationCreationError", locale));
                }
                if (paRes != null && ServiceUtil.isError(paRes)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(paRes));
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> refundOrderPaymentPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = null;
        try {
            orderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }
        // call the service refundPayment
        Map<String, Object> refundResponse = null;
        try {
            Map<String, Object> serviceContext = new HashMap<String, Object>();
            serviceContext.put("orderPaymentPreference", orderPaymentPreference);
            serviceContext.put("refundAmount", amount);
            serviceContext.put("userLogin", userLogin);
            refundResponse = dispatcher.runSync("refundPayment", serviceContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem refunding payment through processor", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentRefundError", locale));
        }
        refundResponse.putAll(ServiceUtil.returnSuccess("Payment #" + refundResponse.get("paymentId") +" is refunded successfully with amount " + refundResponse.get("refundAmount") +" for manual transaction."));
        return refundResponse;
    }

    public static Map<String, Object> refundPayment(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal refundAmount = (BigDecimal) context.get("refundAmount");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader from OrderPaymentPreference", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    e.toString());
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        GenericValue paymentSettings = null;
        if (orderHeader != null) {
            paymentSettings = getPaymentSettings(orderHeader, paymentPref, REFUND_SERVICE_TYPE, false);
        }

        String serviceName = null;
        String paymentGatewayConfigId = null;

        if (paymentSettings != null) {
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                serviceName = getPaymentCustomMethod(orh.getOrderHeader().getDelegator(), customMethodId);
            }
            if (UtilValidate.isEmpty(serviceName)) {
                serviceName = paymentSettings.getString("paymentService");
            }
            String paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");

            if (serviceName != null) {
                Map<String, Object> serviceContext = new HashMap<String, Object>();
                serviceContext.put("orderPaymentPreference", paymentPref);
                serviceContext.put("paymentConfig", paymentConfig);
                serviceContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
                serviceContext.put("currency", orh.getCurrency());

                // get the creditCard/address/email
                String payToPartyId = null;
                try {
                    payToPartyId = getBillingInformation(orh, paymentPref, new HashMap<String, Object>());
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems getting billing information", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingBillingAccountNotFound", UtilMisc.toMap("billingAccountId", ""), locale));
                }

                BigDecimal processAmount = refundAmount.setScale(decimals, rounding);
                serviceContext.put("refundAmount", processAmount);
                serviceContext.put("userLogin", userLogin);

                // call the service
                Map<String, Object> refundResponse = null;
                try {
                    refundResponse = dispatcher.runSync(serviceName, serviceContext, TX_TIME, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem refunding payment through processor", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingPaymentRefundError", locale));
                }
                if (ServiceUtil.isError(refundResponse)) {
                    saveError(dispatcher, userLogin, paymentPref, refundResponse, REFUND_SERVICE_TYPE, "PGT_REFUND");
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(refundResponse));
                }

                // get the pay to party ID for the order (will be the payFrom)
                String payFromPartyId = getPayToPartyId(orderHeader);

                // process the refund result
                Map<String, Object> refundResRes;
                try {
                    ModelService model = dctx.getModelService("processRefundResult");
                    Map<String, Object> refundResCtx = model.makeValid(context, ModelService.IN_PARAM);
                    refundResCtx.put("currencyUomId", orh.getCurrency());
                    refundResCtx.put("payToPartyId", payToPartyId);
                    refundResCtx.put("payFromPartyId", payFromPartyId);
                    refundResCtx.put("refundRefNum", refundResponse.get("refundRefNum"));
                    refundResCtx.put("refundAltRefNum", refundResponse.get("refundAltRefNum"));
                    refundResCtx.put("refundMessage", refundResponse.get("refundMessage"));
                    refundResCtx.put("refundResult", refundResponse.get("refundResult"));

                    // The refund amount could be different from what we tell the payment gateway due to issues
                    // such as having to void the entire original auth amount and re-authorize the new order total.
                    BigDecimal actualRefundAmount = (BigDecimal) refundResponse.get("refundAmount");
                    if (actualRefundAmount != null && actualRefundAmount.compareTo(processAmount) != 0) {
                        refundResCtx.put("refundAmount", refundResponse.get("refundAmount"));
                    }
                    refundResRes = dispatcher.runSync(model.name, refundResCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingPaymentRefundError", locale) + " " + e.getMessage());
                }

                return refundResRes;
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentRefundServiceNotDefined", locale));
            }
        } else {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, 
                    "AccountingPaymentSettingNotFound",
                    UtilMisc.toMap("productStoreId", orderHeader.getString("productStoreId"),
                            "transactionType", REFUND_SERVICE_TYPE), locale));
        }
    }

    public static Map<String, Object> processRefundResult(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String currencyUomId = (String) context.get("currencyUomId");
        String payToPartyId = (String) context.get("payToPartyId");
        String payFromPartyId = (String) context.get("payFromPartyId");

        // create the PaymentGatewayResponse record
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse");
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", REFUND_SERVICE_TYPE);
        response.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        response.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        response.set("transCodeEnumId", "PGT_REFUND");

        // set the capture info
        response.set("amount", context.get("refundAmount"));
        response.set("currencyUomId", currencyUomId);
        response.set("referenceNum", context.get("refundRefNum"));
        response.set("altReference", context.get("refundAltRefNum"));
        response.set("gatewayCode", context.get("refundCode"));
        response.set("gatewayFlag", context.get("refundFlag"));
        response.set("gatewayMessage", context.get("refundMessage"));
        response.set("transactionDate", UtilDateTime.nowTimestamp());

        // save the response
        savePgr(dctx, response);

        // create the internal messages
        List<String> messages = UtilGenerics.cast(context.get("internalRespMsgs"));
        if (UtilValidate.isNotEmpty(messages)) {
            Iterator<String> i = messages.iterator();
            while (i.hasNext()) {
                GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg");
                String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                String message = i.next();
                respMsg.set("paymentGatewayRespMsgId", respMsgId);
                respMsg.set("paymentGatewayResponseId", responseId);
                respMsg.set("pgrMessage", message);

                // save the message
                savePgr(dctx, respMsg);
            }
        }

        Boolean refundResult = (Boolean) context.get("refundResult");
        if (refundResult != null && refundResult.booleanValue()) {

            // mark the preference as refunded
            paymentPref.set("statusId", "PAYMENT_REFUNDED");
            try {
                paymentPref.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            // handle the (reverse) payment
            Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap("paymentTypeId", "CUSTOMER_REFUND");
            paymentCtx.put("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
            paymentCtx.put("paymentMethodId", paymentPref.get("paymentMethodId"));
            paymentCtx.put("paymentGatewayResponseId", responseId);
            paymentCtx.put("partyIdTo", payToPartyId);
            paymentCtx.put("partyIdFrom", payFromPartyId);
            paymentCtx.put("statusId", "PMNT_SENT");
            paymentCtx.put("paymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
            paymentCtx.put("currencyUomId", currencyUomId);
            paymentCtx.put("amount", context.get("refundAmount"));
            paymentCtx.put("userLogin", userLogin);
            paymentCtx.put("paymentRefNum", context.get("refundRefNum"));
            paymentCtx.put("comments", "Refund");

            String paymentId = null;
            try {
                Map<String, Object> payRes = dispatcher.runSync("createPayment", paymentCtx);
                if (ModelService.RESPOND_ERROR.equals(payRes.get(ModelService.RESPONSE_MESSAGE))) {
                    return ServiceUtil.returnError((String) payRes.get(ModelService.ERROR_MESSAGE));
                } else {
                    paymentId = (String) payRes.get("paymentId");
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem creating Payment", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreationError", locale));
            }
            //Debug.logInfo("Payment created : " + paymentId, module);

            if (paymentId == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreationError", locale));
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("paymentId", paymentId);
            result.put("refundAmount", context.get("refundAmount"));
            return result;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentRefundError", locale));            
        }
    }

    public static Map<String, Object>retryFailedOrderAuth(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }

        // make sure we have a valid order record
        if (orderHeader == null || orderHeader.get("statusId") == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }

        // check the current order status
        if (!"ORDER_CREATED".equals(orderHeader.getString("statusId"))) {
            // if we are out of the created status; then we were either cancelled, rejected or approved
            Debug.logWarning("Was re-trying a failed auth for orderId [" + orderId + "] but it is not in the ORDER_CREATED status, so skipping.", module);
            return ServiceUtil.returnSuccess();
        }

        // run the auth service and check for failure(s)
        Map<String, Object> serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("authOrderPayments", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
        }

        // check to see if there was a processor failure
        String authResp = (String) serviceResult.get("processResult");
        if (authResp == null) {
            authResp = "ERROR";
        }

        if ("ERROR".equals(authResp)) {
            Debug.logWarning("The payment processor had a failure in processing, will not modify any status", module);
        } else {
            if ("FAILED".equals(authResp)) {
                // declined; update the order status
                OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
            } else if ("APPROVED".equals(authResp)) {
                // approved; update the order status
                OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("processResult", authResp);

        return result;
    }


    public static Map<String, Object>retryFailedAuths(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get a list of all payment prefs still pending
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_NOT_AUTH"),
                EntityCondition.makeCondition("processAttempt", EntityOperator.GREATER_THAN, Long.valueOf(0)));

        EntityListIterator eli = null;
        try {
            eli = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_NOT_AUTH"),
                            EntityCondition.makeCondition("processAttempt", EntityOperator.GREATER_THAN, Long.valueOf(0)))
                    .orderBy("orderId").queryIterator();
            List<String> processList = new LinkedList<String>();
            if (eli != null) {
                Debug.logInfo("Processing failed order re-auth(s)", module);
                GenericValue value = null;
                while (((value = eli.next()) != null)) {
                    String orderId = value.getString("orderId");
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync("retryFailedOrderAuth", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
                            processList.add(orderId);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } finally {
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object>retryFailedAuthNsfs(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get the date/time for one week before now since we'll only retry once a week for NSFs
        Calendar calcCal = Calendar.getInstance();
        calcCal.setTimeInMillis(System.currentTimeMillis());
        calcCal.add(Calendar.WEEK_OF_YEAR, -1);
        Timestamp oneWeekAgo = new Timestamp(calcCal.getTimeInMillis());

        EntityListIterator eli = null;
        try {
            eli = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where(EntityCondition.makeCondition("needsNsfRetry", EntityOperator.EQUALS, "Y"), 
                            EntityCondition.makeCondition(ModelEntity.STAMP_FIELD, EntityOperator.LESS_THAN_EQUAL_TO, oneWeekAgo))
                    .orderBy("orderId").queryIterator();

            List<String> processList = new LinkedList<String>();
            if (eli != null) {
                Debug.logInfo("Processing failed order re-auth(s)", module);
                GenericValue value = null;
                while (((value = eli.next()) != null)) {
                    String orderId = value.getString("orderId");
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync("retryFailedOrderAuth", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
                            processList.add(orderId);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } finally {
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static GenericValue getCaptureTransaction(GenericValue orderPaymentPreference) {
        GenericValue capTrans = null;
        try {
            List<String> order = UtilMisc.toList("-transactionDate");
            List<GenericValue> transactions = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, order, false);
            List<EntityExpr> exprs = UtilMisc.toList(
                    EntityCondition.makeCondition("paymentServiceTypeEnumId", EntityOperator.EQUALS, CAPTURE_SERVICE_TYPE) ,
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("referenceNum"), EntityComparisonOperator.NOT_EQUAL, EntityFunction.UPPER("ERROR")));
            List<GenericValue> capTransactions = EntityUtil.filterByAnd(transactions, exprs);
            capTrans = EntityUtil.getFirst(capTransactions);
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Problem getting capture information from PaymentGatewayResponse", module);
        }
        return capTrans;
    }

    /**
     * Gets the chronologically latest PaymentGatewayResponse from an OrderPaymentPreference which is either a PRDS_PAY_AUTH
     * or PRDS_PAY_REAUTH.  Used for capturing.
     * @param orderPaymentPreference GenericValue object of the order payment preference
     * @return return the first authorization of the order payment preference
     */
    public static GenericValue getAuthTransaction(GenericValue orderPaymentPreference) {
        return EntityUtil.getFirst(getAuthTransactions(orderPaymentPreference));
    }

    /**
     * Gets a chronologically ordered list of PaymentGatewayResponses from an OrderPaymentPreference which is either a PRDS_PAY_AUTH
     * or PRDS_PAY_REAUTH.
     * @param orderPaymentPreference GenericValue object of the order payment preference
     * @return return the authorizations of the order payment preference
     */
    public static List<GenericValue> getAuthTransactions(GenericValue orderPaymentPreference) {
        List<GenericValue> authTransactions = null;
        try {
            List<String> order = UtilMisc.toList("-transactionDate");
            List<GenericValue> transactions = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, order, false);
            List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("paymentServiceTypeEnumId", EntityOperator.EQUALS, AUTH_SERVICE_TYPE),
                    EntityCondition.makeCondition("paymentServiceTypeEnumId", EntityOperator.EQUALS, REAUTH_SERVICE_TYPE));
            authTransactions = EntityUtil.filterByOr(transactions, exprs);
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Problem getting authorization information from PaymentGatewayResponse", module);
        }
        return authTransactions;
    }

    public static Timestamp getAuthTime(GenericValue orderPaymentPreference) {
        GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        Timestamp authTime = null;

        if (authTrans != null) {
            authTime = authTrans.getTimestamp("transactionDate");
        }

        return authTime;
    }

    public static boolean checkAuthValidity(GenericValue orderPaymentPreference, String paymentConfig) {
    	Delegator delegator = orderPaymentPreference.getDelegator();
        Timestamp authTime = PaymentGatewayServices.getAuthTime(orderPaymentPreference);
        if (authTime == null) {
            return false;
        }

        String reauthDays = null;

        GenericValue paymentMethod = null;
        try {
            paymentMethod = orderPaymentPreference.getRelatedOne("PaymentMethod", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (paymentMethod != null && paymentMethod.getString("paymentMethodTypeId").equals("CREDIT_CARD")) {
            GenericValue creditCard = null;
            try {
                creditCard = paymentMethod.getRelatedOne("CreditCard", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (creditCard != null) {
                String cardType = creditCard.getString("cardType");
                // add more types as necessary -- maybe we should create seed data for credit card types??
                if ("CCT_DISCOVER".equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.disc.days", "90", delegator);
                } else if ("CCT_AMERICANEXPRESS".equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.amex.days", "30", delegator);
                } else if ("CCT_MASTERCARD".equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.mc.days", "30", delegator);
                } else if ("CCT_VISA".equals(cardType)) {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.visa.days", "7", delegator);
                } else {
                    reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.other.days", "7", delegator);
                }

            }
        } else if (paymentMethod != null && "EXT_PAYPAL".equals(paymentMethod.get("paymentMethodTypeId"))) {
            reauthDays = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.paypal.days", "3", delegator);
        }

        if (reauthDays != null) {
            int days = 0;
            try {
                days = Integer.parseInt(reauthDays);
            } catch (Exception e) {
                Debug.logError(e, module);
            }

            if (days > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(authTime.getTime());
                cal.add(Calendar.DAY_OF_YEAR, days);
                Timestamp validTime = new Timestamp(cal.getTimeInMillis());
                Timestamp nowTime = UtilDateTime.nowTimestamp();
                if (nowTime.after(validTime)) {
                    return false;
                }
            }
        }

        return true;
    }

    // safe payment gateway response store

    /**
     * Saves either a PaymentGatewayResponse or PaymentGatewayRespMsg value and ensures that the value
     * is persisted even in the event of a rollback.
     * @param dctx
     * @param pgr Either a PaymentGatewayResponse or PaymentGatewayRespMsg GenericValue
     */
    private static void savePgr(DispatchContext dctx, GenericValue pgr) {
        Map<String, GenericValue> context = UtilMisc.<String, GenericValue>toMap("paymentGatewayResponse", pgr);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            dispatcher.addRollbackService("savePaymentGatewayResponse", context, true);
            delegator.create(pgr);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    private static void savePgrAndMsgs(DispatchContext dctx, GenericValue pgr, List<GenericValue> messages) {
        Map<String, GenericValue> context = UtilMisc.<String, GenericValue>toMap("paymentGatewayResponse", pgr, "messages", messages);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            dispatcher.addRollbackService("savePaymentGatewayResponseAndMessages", context, true);
            delegator.create(pgr);
            for (GenericValue message : messages) {
                delegator.create(message);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    public static Map<String, Object> savePaymentGatewayResponse(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue pgr = (GenericValue) context.get("paymentGatewayResponse");
        if ("PaymentGatewayResponse".equals(pgr.getEntityName())) {
            String message = pgr.getString("gatewayMessage");
            if (UtilValidate.isNotEmpty(message) && message.length() > 255) {
                pgr.set("gatewayMessage", message.substring(0, 255));
            }
        }

        try {
            delegator.create(pgr);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> savePaymentGatewayResponseAndMessages(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue pgr = (GenericValue) context.get("paymentGatewayResponse");
        String gatewayMessage = pgr.getString("gatewayMessage");
        if (UtilValidate.isNotEmpty(gatewayMessage) && gatewayMessage.length() > 255) {
            pgr.set("gatewayMessage", gatewayMessage.substring(0, 255));
        }
        @SuppressWarnings("unchecked")
        List<GenericValue> messages = (List<GenericValue>) context.get("messages");

        try {
            delegator.create(pgr);
            for (GenericValue message : messages) {
                delegator.create(message);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    // manual auth service
    public static Map<String, Object> processManualCcAuth(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        // security check
        if (!security.hasEntityPermission("MANUAL", "_PAYMENT", userLogin) && !security.hasEntityPermission("ACCOUNTING", "_CREATE", userLogin)) {
            Debug.logWarning("**** Security [" + (new Date()).toString() + "]: " + userLogin.get("userLoginId") + " attempt to run manual payment transaction!", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionNotAuthorized", locale));
        }

        String paymentMethodId = (String) context.get("paymentMethodId");
        String productStoreId = (String) context.get("productStoreId");
        String securityCode = (String) context.get("securityCode");
        BigDecimal amount = (BigDecimal) context.get("amount");

        // check the payment method; verify type
        GenericValue paymentMethod;
        try {
            paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (paymentMethod == null || !"CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentManualAuthOnlyForCreditCard", locale));
        }

        // get the billToParty object
        GenericValue billToParty;
        try {
            billToParty = paymentMethod.getRelatedOne("Party", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // get the credit card object
        GenericValue creditCard;
        try {
            creditCard = EntityQuery.use(delegator).from("CreditCard").where("paymentMethodId", paymentMethodId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(creditCard)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentCreditCardNotFound",
                    UtilMisc.toMap("paymentMethodId", paymentMethodId), locale));
        }

        // get the transaction settings
        String paymentService = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;

        GenericValue paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "CREDIT_CARD", "PRDS_PAY_AUTH", false);
        if (paymentSettings == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentSettingNotFound",
                    UtilMisc.toMap("productStoreId", productStoreId, "transactionType", ""), locale));
        } else {
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                paymentService = getPaymentCustomMethod(delegator, customMethodId);
            }
            if (UtilValidate.isEmpty(paymentService)) {
                paymentService = paymentSettings.getString("paymentService");
            }
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");
            if (UtilValidate.isEmpty(paymentConfig)) {
                paymentConfig = "payment.properties";
            }
        }

        // prepare the order payment preference (facade)
        GenericValue orderPaymentPref = delegator.makeValue("OrderPaymentPreference", new HashMap());
        orderPaymentPref.set("orderPaymentPreferenceId", "_NA_");
        orderPaymentPref.set("orderId", "_NA_");
        orderPaymentPref.set("presentFlag", "N");
        orderPaymentPref.set("overflowFlag", "Y");
        orderPaymentPref.set("paymentMethodTypeId", "CREDIT_CARD");
        orderPaymentPref.set("paymentMethodId", paymentMethodId);
        if (UtilValidate.isNotEmpty(securityCode)) {
            orderPaymentPref.set("securityCode", securityCode);
        }
        // this record is not to be stored, just passed to the service for use

        // get the default currency
        String currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);

        // prepare the auth context
        Map<String, Object> authContext = new HashMap<String, Object>();
        authContext.put("orderId", "_NA_");
        authContext.put("orderItems", new LinkedList());
        authContext.put("orderPaymentPreference", orderPaymentPref);
        authContext.put("creditCard", creditCard);
        authContext.put("billToParty", billToParty);
        authContext.put("currency", currency);
        authContext.put("paymentConfig", paymentConfig);
        authContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
        authContext.put("processAmount", amount);
        authContext.put("userLogin", userLogin);

        // call the auth service
        Map<String, Object> response;
        try {
            Debug.logInfo("Running authorization service: " + paymentService, module);
            response = dispatcher.runSync(paymentService, authContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentServiceError",
                    UtilMisc.toMap("paymentService", paymentService, "authContext", authContext),
                    locale));
        }
        if (ServiceUtil.isError(response)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(response));
        }

        Boolean authResult = (Boolean) response.get("authResult");
        Debug.logInfo("Authorization service returned: " + authResult, module);
        if (authResult != null && authResult) {
            return ServiceUtil.returnSuccess();
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentAuthorizationFailed",    locale));
        }
    }

    // manual processing service
    public static Map<String, Object> processManualCcTx(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        // security check
        if (!security.hasEntityPermission("MANUAL", "_PAYMENT", userLogin) && !security.hasEntityPermission("ACCOUNTING", "_CREATE", userLogin)) {
            Debug.logWarning("**** Security [" + (new Date()).toString() + "]: " + userLogin.get("userLoginId") + " attempt to run manual payment transaction!", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionNotAuthorized", locale));
        }
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String productStoreId = (String) context.get("productStoreId");
        String transactionType = (String) context.get("transactionType");
        String referenceCode = (String) context.get("referenceCode");
        if (referenceCode == null) {
            referenceCode = Long.valueOf(System.currentTimeMillis()).toString();
        }
        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            paymentPref = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problem getting OrderPaymentPreference for orderPaymentPreferenceId " + 
                    orderPaymentPreferenceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);
        }
        // Error if no OrderPaymentPreference was found
        if (paymentPref == null) {
            Debug.logWarning("Could not find OrderPaymentPreference with orderPaymentPreferenceId: " + 
                    orderPaymentPreferenceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingProblemGettingOrderPaymentPreferences", locale) + " " + 
                    orderPaymentPreferenceId);            
        }
        // Get the OrderHeader
        GenericValue orderHeader = null;
        String orderId = paymentPref.getString("orderId");
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problem getting OrderHeader for orderId " + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }
        // Error if no OrderHeader was found
        if (orderHeader == null) {
            Debug.logWarning("Could not find OrderHeader with orderId: " + orderId + "; not processing payments.", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceOrder, 
                    "OrderOrderNotFound", UtilMisc.toMap("orderId", orderId), locale));
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        // check valid implemented types
        if (!transactionType.equals(CREDIT_SERVICE_TYPE)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionNotYetSupported",    locale));
        }
        // transaction request context
        Map<String, Object> requestContext = new HashMap<String, Object>();
        String paymentService = null;
        String paymentConfig = null;
        String paymentGatewayConfigId = null;
        // get the transaction settings
        GenericValue paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId, transactionType, false);
        if (paymentSettings == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentSettingNotFound",
                    UtilMisc.toMap("productStoreId", productStoreId, "transactionType", transactionType), locale));
        } else {
            paymentGatewayConfigId = paymentSettings.getString("paymentGatewayConfigId");
            String customMethodId = paymentSettings.getString("paymentCustomMethodId");
            if (UtilValidate.isNotEmpty(customMethodId)) {
                paymentService = getPaymentCustomMethod(delegator, customMethodId);
            }
            if (UtilValidate.isEmpty(paymentService)) {
                paymentService = paymentSettings.getString("paymentService");
            }
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            if (paymentConfig == null) {
                paymentConfig = "payment.properties";
            }
            requestContext.put("paymentConfig", paymentConfig);
            requestContext.put("paymentGatewayConfigId", paymentGatewayConfigId);
        }
        // check the service name
        if (paymentService == null || paymentGatewayConfigId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentSettingNotValid", locale));
        }

        if (paymentMethodTypeId.equals("CREDIT_CARD")) {
            GenericValue creditCard = delegator.makeValue("CreditCard");
            creditCard.setAllFields(context, true, null, null);
            if (creditCard.get("firstNameOnCard") == null || creditCard.get("lastNameOnCard") == null || creditCard.get("cardType") == null || creditCard.get("cardNumber") == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreditCardMissingMandatoryFields", locale));
            }
            String expMonth = (String) context.get("expMonth");
            String expYear = (String) context.get("expYear");
            String expDate = expMonth + "/" + expYear;
            creditCard.set("expireDate", expDate);
            requestContext.put("creditCard", creditCard);
            requestContext.put("cardSecurityCode", context.get("cardSecurityCode"));
            GenericValue billingAddress = delegator.makeValue("PostalAddress");
            billingAddress.setAllFields(context, true, null, null);
            if (billingAddress.get("address1") == null || billingAddress.get("city") == null || billingAddress.get("postalCode") == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreditCardBillingAddressMssingMandatoryFields", locale));
            }
            requestContext.put("billingAddress", billingAddress);
            GenericValue billToEmail = delegator.makeValue("ContactMech");
            billToEmail.set("infoString", context.get("infoString"));
            if (billToEmail.get("infoString") == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreditCardEmailAddressCannotBeEmpty", locale));
            }
            requestContext.put("billToParty", orh.getBillToParty());
            requestContext.put("billToEmail", billToEmail);
            requestContext.put("referenceCode", referenceCode);
            String currency = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
            requestContext.put("currency", currency);
            requestContext.put("creditAmount", context.get("amount"));
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionNotYetSupported", locale) + " " + paymentMethodTypeId);
        }
        // process the transaction
        Map<String, Object> response = null;
        try {
            response = dispatcher.runSync(paymentService, requestContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentServiceError",
                    UtilMisc.toMap("paymentService", paymentService, "authContext", requestContext),
                    locale));
        }
        // get the response result code
        if (response != null && !ServiceUtil.isError(response)) {
            Map<String, Object> responseRes;
            try {
                ModelService model = dctx.getModelService("processCreditResult");
                response.put("orderPaymentPreference", paymentPref);
                response.put("userLogin", userLogin);
                Map<String, Object> resCtx = model.makeValid(response, ModelService.IN_PARAM);
                responseRes = dispatcher.runSync(model.name,  resCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "AccountingPaymentCreditError",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
            if (responseRes != null && ServiceUtil.isError(responseRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(responseRes));
            }
        } else if (ServiceUtil.isError(response)) {
            saveError(dispatcher, userLogin, paymentPref, response, CREDIT_SERVICE_TYPE, "PGT_CREDIT");
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(response));
        }
        // check for errors
        if (ServiceUtil.isError(response)) {
            return ServiceUtil.returnError(ServiceUtil.makeErrorMessage(response, null, null, null, null));
        }
        // get the reference number
        String refNum = (String) response.get("creditRefNum");
        String code = (String) response.get("creditCode");
        String msg = (String) response.get("creditMessage");
        Map<String, Object> returnResults = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, 
                "AccountingPaymentTransactionManualResult",
                UtilMisc.toMap("msg", msg, "code", code, "refNum", refNum), locale));
        returnResults.put("referenceNum", refNum);
        return returnResults;
    }

    // Verify Credit Card (Manually) Service
    public static Map<String, Object>verifyCreditCard(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String mode = (String) context.get("mode");
        String paymentMethodId = (String) context.get("paymentMethodId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        if (Debug.infoOn()) Debug.logInfo("Running verifyCreditCard [ " + paymentMethodId + "] for store: " + productStoreId, module);

        GenericValue productStore = null;
        productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);

        String productStorePaymentProperties = "payment.properties";
        if (productStore != null) {
            productStorePaymentProperties = ProductStoreWorker.getProductStorePaymentProperties(delegator, productStoreId, "CREDIT_CARD", "PRDS_PAY_AUTH", false);
        }

        String amount = null;
        if (mode.equalsIgnoreCase("CREATE")) {
            amount = EntityUtilProperties.getPropertyValue(productStorePaymentProperties, "payment.general.cc_create.auth", delegator);
        } else if (mode.equalsIgnoreCase("UPDATE")) {
            amount = EntityUtilProperties.getPropertyValue(productStorePaymentProperties, "payment.general.cc_update.auth", delegator);
        }
        if (Debug.infoOn()) Debug.logInfo("Running credit card verification [" + paymentMethodId + "] (" + amount + ") : " + productStorePaymentProperties + " : " + mode, module);

        if (UtilValidate.isNotEmpty(amount)) {
            BigDecimal authAmount = new BigDecimal(amount);
            if (authAmount.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> ccAuthContext = new HashMap<String, Object>();
                ccAuthContext.put("paymentMethodId", paymentMethodId);
                ccAuthContext.put("productStoreId", productStoreId);
                ccAuthContext.put("amount", authAmount);
                ccAuthContext.put("userLogin", userLogin);

                Map<String, Object> results;
                try {
                    results = dispatcher.runSync("manualForcedCcAuthTransaction", ccAuthContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage("AccountingUiLabels", "AccountingCreditCardManualAuthFailedError", locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // ****************************************************
    // Test Services
    // ****************************************************


    /**
     * Simple test processor; declines all orders < 100.00; approves all orders >= 100.00
     */
    public static Map<String, Object> testProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");

        if (processAmount != null && processAmount.compareTo(new BigDecimal("100.00")) >= 0)
            result.put("authResult", Boolean.TRUE);
        if (processAmount != null && processAmount.compareTo(new BigDecimal("100.00")) < 0)
            result.put("authResult", Boolean.FALSE);
            result.put("customerRespMsgs", UtilMisc.toList(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTestProcessorMinimumPurchase", locale)));
        if (processAmount == null)
            result.put("authResult", null);

        String refNum = UtilDateTime.nowAsString();

        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "X");
        result.put("authMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestProcessor", locale));
        result.put("internalRespMsgs", UtilMisc.toList(UtilProperties.getMessage(resource, 
                "AccountingPaymentTestProcessor", locale)));
        return result;
    }


    /**
     * Simple test processor; declines all orders < 100.00; approves all orders > 100.00
     */
    public static Map<String, Object> testProcessorWithCapture(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");

        if (processAmount != null && processAmount.compareTo(new BigDecimal("100.00")) >= 0)
            result.put("authResult", Boolean.TRUE);
            result.put("captureResult", Boolean.TRUE);
        if (processAmount != null && processAmount.compareTo(new BigDecimal("100.00")) < 0)
            result.put("authResult", Boolean.FALSE);
            result.put("captureResult", Boolean.FALSE);
            result.put("customerRespMsgs", UtilMisc.toList(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTestProcessorMinimumPurchase", locale)));
        if (processAmount == null)
            result.put("authResult", null);

        String refNum = UtilDateTime.nowAsString();

        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("captureRefNum", refNum);
        result.put("captureAltRefNum", refNum);
        result.put("authCode", "100");
        result.put("captureCode", "200");
        result.put("authFlag", "X");
        result.put("authMessage", UtilMisc.toList(UtilProperties.getMessage(resource, 
                "AccountingPaymentTestCapture", locale)));
        result.put("internalRespMsgs", UtilMisc.toList(UtilProperties.getMessage(resource, 
                "AccountingPaymentTestCapture", locale)));
        return result;
    }

    /**
     *  Test authorize - does random declines
     */
    public static Map<String, Object> testRandomAuthorize(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String refNum = UtilDateTime.nowAsString();
        Random r = new Random();
        int i = r.nextInt(9);
        if (i < 5 || i % 2 == 0) {
            result.put("authResult", Boolean.TRUE);
            result.put("authFlag", "A");
        } else {
            result.put("authResult", Boolean.FALSE);
            result.put("authFlag", "D");
        }

        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authCode", "100");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestCapture", locale));

        return result;
    }

    /**
     * Always approve processor.
     */
    public static Map<String, Object> alwaysApproveProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        Debug.logInfo("Test Processor Approving Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.TRUE);
        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authCode", "100");
        result.put("authFlag", "A");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestProcessor", locale));
        return result;
    }

    public static Map<String, Object> alwaysApproveWithCapture(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        String refNum = UtilDateTime.nowAsString();
        Debug.logInfo("Test Processor Approving Credit Card with Capture", module);

        result.put("authResult", Boolean.TRUE);
        result.put("captureResult", Boolean.TRUE);
        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("captureRefNum", refNum);
        result.put("captureAltRefNum", refNum);
        result.put("authCode", "100");
        result.put("captureCode", "200");
        result.put("authFlag", "A");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestCapture", locale));
        return result;
    }


    /**
     * Always decline processor
     */
    public static Map<String, Object> alwaysDeclineProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        Debug.logInfo("Test Processor Declining Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "D");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestProcessorDeclined", locale));
        return result;
    }

    /**
     * Always NSF (not sufficient funds) processor
     */
    public static Map<String, Object> alwaysNsfProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        Debug.logInfo("Test Processor NSF Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultNsf", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "N");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestProcessor", locale));
        return result;
    }

    /**
     * Always fail/bad expire date processor
     */
    public static Map<String, Object> alwaysBadExpireProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        Debug.logInfo("Test Processor Bad Expire Date Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultBadExpire", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "E");
        result.put("authMessage", UtilProperties.getMessage(resource, 
                "AccountingPaymentTestProcessor", locale));
        return result;
    }

    /**
     * Fail/bad expire date when year is even processor
     */
    public static Map<String, Object>badExpireEvenProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        String expireDate = creditCard.getString("expireDate");
        String lastNumberStr = expireDate.substring(expireDate.length() - 1);
        int lastNumber = Integer.parseInt(lastNumberStr);

        if (lastNumber % 2.0 == 0.0) {
            return alwaysBadExpireProcessor(dctx, context);
        } else {
            return alwaysApproveProcessor(dctx, context);
        }
    }

    /**
     * Always bad card number processor
     */
    public static Map<String, Object> alwaysBadCardNumberProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        Debug.logInfo("Test Processor Bad Card Number Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultBadCardNumber", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "N");
        result.put("authMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestBadCardNumber", locale));
        return result;
    }

    /**
     * Always fail (error) processor
     */
    public static Map<String, Object> alwaysFailProcessor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                "AccountingPaymentTestAuthorizationAlwaysFailed", locale));
    }

    public static Map<String, Object> testRelease(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String refNum = UtilDateTime.nowAsString();

        result.put("releaseResult", Boolean.TRUE);
        result.put("releaseAmount", context.get("releaseAmount"));
        result.put("releaseRefNum", refNum);
        result.put("releaseAltRefNum", refNum);
        result.put("releaseFlag", "U");
        result.put("releaseMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestRelease", locale));
        return result;
    }

    /**
     * Test capture service (returns true)
     */
    public static Map<String, Object> testCapture(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Capture Process", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("captureResult", Boolean.TRUE);
        result.put("captureAmount", context.get("captureAmount"));
        result.put("captureRefNum", refNum);
        result.put("captureAltRefNum", refNum);
        result.put("captureFlag", "C");
        result.put("captureMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestCapture", locale));
        return result;
    }

    /**
     * Always decline processor
     */
    public static Map<String, Object> testCCProcessorCaptureAlwaysDecline(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        BigDecimal processAmount = (BigDecimal) context.get("captureAmount");
        Debug.logInfo("Test Processor Declining Credit Card capture", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("captureResult", Boolean.FALSE);
        result.put("captureAmount", processAmount);
        result.put("captureRefNum", refNum);
        result.put("captureAltRefNum", refNum);
        result.put("captureFlag", "D");
        result.put("captureMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestCaptureDeclined", locale));
        return result;
    }

    public static Map<String, Object> testCaptureWithReAuth(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        Debug.logInfo("Test Capture with 2 minute delay failure/re-auth process", module);

        if (authTransaction == null) {
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentCannotBeCaptured", locale));
        }
        Timestamp txStamp = authTransaction.getTimestamp("transactionDate");
        Timestamp nowStamp = UtilDateTime.nowTimestamp();

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("captureAmount", context.get("captureAmount"));
        result.put("captureRefNum", UtilDateTime.nowAsString());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(txStamp.getTime());
        cal.add(Calendar.MINUTE, 2);
        Timestamp twoMinAfter = new Timestamp(cal.getTimeInMillis());
        if (Debug.infoOn()) Debug.logInfo("Re-Auth Capture Test : Tx Date - " + txStamp + " : 2 Min - " + twoMinAfter + " : Now - " + nowStamp, module);

        if (nowStamp.after(twoMinAfter)) {
            result.put("captureResult", Boolean.FALSE);
        } else {
            result.put("captureResult", Boolean.TRUE);
            result.put("captureFlag", "C");
            result.put("captureMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestCaptureWithReauth", locale));
        }

        return result;
    }

    /**
     * Test refund service (returns true)
     */
    public static Map<String, Object> testRefund(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Refund Process", module);

        result.put("refundResult", Boolean.TRUE);
        result.put("refundAmount", context.get("refundAmount"));
        result.put("refundRefNum", UtilDateTime.nowAsString());
        result.put("refundFlag", "R");
        result.put("refundMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestRefund", locale));
        
        return result;
    }

    public static Map<String, Object> testRefundFailure(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Refund Process", module);

        result.put("refundResult", Boolean.FALSE);
        result.put("refundAmount", context.get("refundAmount"));
        result.put("refundRefNum", UtilDateTime.nowAsString());
        result.put("refundFlag", "R");
        result.put("refundMessage", UtilProperties.getMessage(resource, "AccountingPaymentTestRefundFailure", locale));
        
        return result;
    }

    public static String getPaymentCustomMethod(Delegator delegator, String customMethodId) {
        String serviceName = null;
        GenericValue customMethod = null;
        try {
            customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", customMethodId).queryOne();
            if (UtilValidate.isNotEmpty(customMethod)) {
                serviceName = customMethod.getString("customMethodName");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return serviceName;
    }

    public static boolean isReplacementOrder(GenericValue orderHeader) {
        boolean replacementOrderFlag = false;

        List<GenericValue> returnItemResponses = new LinkedList<GenericValue>();
        try {
            returnItemResponses = orderHeader.getRelated("ReplacementReturnItemResponse", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return replacementOrderFlag;
        }
        if (UtilValidate.isNotEmpty(returnItemResponses)) {
            replacementOrderFlag = true;
        }

        return replacementOrderFlag;
    }
}
