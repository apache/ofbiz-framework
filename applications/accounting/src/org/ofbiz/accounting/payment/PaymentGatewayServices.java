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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
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
    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("order.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO = ZERO.setScale(decimals);
    }
    
    /**
     * Authorizes a single order preference with an option to specify an amount. The result map has the Booleans
     * "errors" and "finished" which notify the user if there were any errors and if the authorizatoin was finished.
     * There is also a List "messages" for the authorization response messages and a Double, "processAmount" as the 
     * amount processed. 
     * 
     * TODO: it might be nice to return the paymentGatewayResponseId
     */
    public static Map authOrderPaymentPreference(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");
        Double overrideAmount = (Double) context.get("overrideAmount");

        // validate overrideAmount if its available
        if (overrideAmount != null) {
            if (overrideAmount.doubleValue() < 0) return ServiceUtil.returnError("Amount entered (" + overrideAmount + ") is negative.");
            if (overrideAmount.doubleValue() == 0) return ServiceUtil.returnError("Amount entered (" + overrideAmount + ") is zero.");
        }

        GenericValue orderHeader = null;
        GenericValue orderPaymentPreference = null;
        try {
            orderPaymentPreference = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
            orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Problems getting required information: orderPaymentPreference [" + orderPaymentPreferenceId + "]");
        }
        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        // get the total remaining
        BigDecimal orderGrandTotal = orh.getOrderGrandTotalBd();
        orderGrandTotal = orderGrandTotal.setScale(2, BigDecimal.ROUND_HALF_UP);
        double totalRemaining = orderGrandTotal.doubleValue();

        // get the process attempts so far
        Long procAttempt = orderPaymentPreference.getLong("processAttempt");
        if (procAttempt == null) {
            procAttempt = new Long(0);
        }

        // update the process attempt count
        orderPaymentPreference.set("processAttempt", new Long(procAttempt.longValue() + 1));
        try {
            orderPaymentPreference.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to update OrderPaymentPreference record!");
        }

        // if we are already authorized, then this is a re-auth request
        boolean reAuth = false;
        if (orderPaymentPreference.get("statusId") != null && "PAYMENT_AUTHORIZED".equals(orderPaymentPreference.getString("statusId"))) {
            reAuth = true;
        }

        // use overrideAmount or maxAmount
        Double transAmount = null;
        if (overrideAmount != null) {
            transAmount = overrideAmount;
        } else {
            transAmount = orderPaymentPreference.getDouble("maxAmount");
        }


        // if our transaction amount exists and is zero, there's nothing to process, so return
        if ((transAmount != null) && (transAmount.doubleValue() <= 0)) {
            // prepare the return map (always return success, default finished=false, default errors=false
            Map results = ServiceUtil.returnSuccess();
            results.put("finished", Boolean.FALSE);
            results.put("errors", Boolean.FALSE);
            return results;
        }

        try {
            // call the authPayment method
            Map authPaymentResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference, totalRemaining, reAuth, overrideAmount);

            // handle the response
            if (authPaymentResult != null) {
                // get the customer messages
                if (authPaymentResult.get("customerRespMsgs") != null) {
                    // NOTE DEJ20060911: hmmm... was something supposed to be done here?
                }

                // not null result means either an approval or decline; null would mean error
                Double thisAmount = (Double) authPaymentResult.get("processAmount");

                // process the auth results
                try {
                    boolean processResult = processResult(dctx, authPaymentResult, userLogin, orderPaymentPreference);
                    if (processResult) {
                        Map results = ServiceUtil.returnSuccess();
                        results.put("messages", authPaymentResult.get("customerRespMsgs"));
                        results.put("processAmount", thisAmount);
                        results.put("finished", Boolean.TRUE);
                        results.put("errors", Boolean.FALSE);
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
                                GenericValue productStore = orderHeader.getRelatedOne("ProductStore");
                                // according to the store should we try other cards?
                                if ("Y".equals(productStore.getString("autoOrderCcTryOtherCards"))) {
                                    // get other credit cards for the bill to party
                                    List otherPaymentMethodAndCreditCardList = null;
                                    String billToPartyId = null; 
                                    GenericValue billToParty = orh.getBillToParty();
                                    if (billToParty != null) {
                                        billToPartyId = billToParty.getString("partyId");
                                    } else {
                                        // TODO optional: any other ways to find the bill to party? perhaps look at info from OrderPaymentPreference, ie search back from other PaymentMethod...
                                    }
                                    
                                    if (UtilValidate.isNotEmpty(billToPartyId)) {
                                        otherPaymentMethodAndCreditCardList = delegator.findByAnd("PaymentMethodAndCreditCard", 
                                                UtilMisc.toMap("partyId", billToPartyId, "paymentMethodTypeId", "CREDIT_CARD"));
                                        otherPaymentMethodAndCreditCardList = EntityUtil.filterByDate(otherPaymentMethodAndCreditCardList, true);
                                    }

                                    if (otherPaymentMethodAndCreditCardList != null && otherPaymentMethodAndCreditCardList.size() > 0) {
                                        Iterator otherPaymentMethodAndCreditCardIter = otherPaymentMethodAndCreditCardList.iterator();
                                        while (otherPaymentMethodAndCreditCardIter.hasNext()) {
                                            GenericValue otherPaymentMethodAndCreditCard = (GenericValue) otherPaymentMethodAndCreditCardIter.next();
                                            
                                            // change OrderPaymentPreference in memory only and call auth service
                                            orderPaymentPreference.set("paymentMethodId", otherPaymentMethodAndCreditCard.getString("paymentMethodId"));
                                            Map authRetryResult = authPayment(dispatcher, userLogin, orh, orderPaymentPreference, totalRemaining, reAuth, overrideAmount);
                                            try {
                                                boolean processRetryResult = processResult(dctx, authPaymentResult, userLogin, orderPaymentPreference);
                                                
                                                if (processRetryResult) {
                                                    // wow, we got here that means the other card was successful...
                                                    // on success save the OrderPaymentPreference, and then return finished (which will break from loop)
                                                    orderPaymentPreference.store();
                                                    
                                                    Map results = ServiceUtil.returnSuccess();
                                                    results.put("messages", authRetryResult.get("customerRespMsgs"));
                                                    results.put("processAmount", thisAmount);
                                                    results.put("finished", Boolean.TRUE);
                                                    results.put("errors", Boolean.FALSE);
                                                    return results;
                                                }
                                            } catch (GeneralException e) {
                                                String errMsg = "Error saving and processing payment authorization results: " + e.toString();
                                                Debug.logError(e, errMsg + "; authRetryResult: " + authRetryResult, module);
                                                Map results = ServiceUtil.returnSuccess();
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

                        Map results = ServiceUtil.returnSuccess();
                        results.put("messages", authPaymentResult.get("customerRespMsgs"));
                        results.put("finished", Boolean.FALSE);
                        results.put("errors", Boolean.FALSE);
                        return results;
                    }
                } catch (GeneralException e) {
                    String errMsg = "Error saving and processing payment authorization results: " + e.toString();
                    Debug.logError(e, errMsg + "; authPaymentResult: " + authPaymentResult, module);
                    Map results = ServiceUtil.returnSuccess();
                    results.put(ModelService.ERROR_MESSAGE, errMsg);
                    results.put("finished", Boolean.FALSE);
                    results.put("errors", Boolean.TRUE);
                    return results;
                }
            } else {
                // error with payment processor; will try later
                String errMsg = "Invalid Order Payment Preference: maxAmount is 0";
                Debug.logInfo(errMsg, module);
                Map results = ServiceUtil.returnSuccess();
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
            String errMsg = "Error processing payment authorization: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }

    /**
     * Processes payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return APPROVED|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map authOrderPayments(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        Map result = new HashMap();

        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List paymentPrefs = null;

        try {
            // get the OrderHeader
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            // get the payments to auth
            Map lookupMap = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_AUTH");
            List orderList = UtilMisc.toList("maxAmount");
            paymentPrefs = delegator.findByAnd("OrderPaymentPreference", lookupMap, orderList);
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting the order information", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not get order information (" + gee.toString() + ").");
            return result;
        }

        // make sure we have a OrderHeader
        if (orderHeader == null) {
            return ServiceUtil.returnError("Could not find OrderHeader with orderId: " + orderId + "; not processing payments.");
        }

        // get the order amounts
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        BigDecimal orderGrandTotal = orh.getOrderGrandTotalBd();
        orderGrandTotal = orderGrandTotal.setScale(2, BigDecimal.ROUND_HALF_UP);
        double totalRemaining = orderGrandTotal.doubleValue();

        // loop through and auth each order payment preference
        int finished = 0;
        int hadError = 0;
        List messages = new ArrayList();
        Iterator payments = paymentPrefs.iterator();
        while (payments.hasNext()) {
            GenericValue paymentPref = (GenericValue) payments.next();

            Map authContext = new HashMap();
            authContext.put("orderPaymentPreferenceId", paymentPref.getString("orderPaymentPreferenceId"));
            authContext.put("userLogin", context.get("userLogin"));

            Map results = null;
            try {
                results = dispatcher.runSync("authOrderPaymentPreference", authContext);
            } catch (GenericServiceException se) {
                Debug.logError(se, "Error in calling authOrderPaymentPreference from authOrderPayments: " + se.toString(), module);
                hadError += 1;
                messages.add("Could not authorize OrderPaymentPreference [" + paymentPref.getString("orderPaymentPreferenceId") + "] for order [" + orderId + "]: " + se.toString());
                continue;
            }

            if (ServiceUtil.isError(results)) {
                hadError += 1;
                messages.add("Could not authorize OrderPaymentPreference [" + paymentPref.getString("orderPaymentPreferenceId") + "] for order [" + orderId + "]: " + results.get(ModelService.ERROR_MESSAGE)); 
                continue;
            }

            if (((Boolean) results.get("finished")).booleanValue()) finished += 1;
            if (((Boolean) results.get("errors")).booleanValue()) hadError += 1;
            if (results.get("messages") != null) messages.addAll((List) results.get("messages"));
            if (results.get("processAmount") != null) totalRemaining -= ((Double) results.get("processAmount")).doubleValue();
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
            Debug.logInfo("Only (" + finished + ") passed auth; returning FAILED", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put("processResult", "FAILED");
            return result;
        }
    }


    private static Map authPayment(LocalDispatcher dispatcher, GenericValue userLogin, OrderReadHelper orh, GenericValue paymentPreference, double totalRemaining, boolean reauth, Double overrideAmount) throws GeneralException {
        String paymentConfig = null;
        String serviceName = null;

        // get the payment settings i.e. serviceName and config properties file name
        String serviceType = AUTH_SERVICE_TYPE;
        if (reauth) {
            serviceType = REAUTH_SERVICE_TYPE;
        }

        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPreference, serviceType, false);
        if (paymentSettings != null) {
            serviceName = paymentSettings.getString("paymentService");
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
        } else {
            throw new GeneralException("Could not find any valid payment settings for order with ID [" + orh.getOrderId() + "], and payment operation (serviceType) [" + serviceType + "]");
        }

        // make sure the service name is not null
        if (serviceName == null) {
            throw new GeneralException("Invalid payment processor, serviceName is null: " + paymentSettings);
        }

        // make the process context
        Map processContext = new HashMap();

        // get the visit record to obtain the client's IP address
        GenericValue orderHeader = orh.getOrderHeader();
        //if (orderHeader == null) {}

        String visitId = orderHeader.getString("visitId");
        GenericValue visit = null;
        if (visitId != null) {
            try {
                visit = orderHeader.getDelegator().findByPrimaryKey("Visit", UtilMisc.toMap("visitId", visitId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        if (visit != null && visit.get("clientIpAddress") != null) {
            processContext.put("customerIpAddress", visit.getString("clientIpAddress"));
        }

        GenericValue productStore = orderHeader.getRelatedOne("ProductStore");

        processContext.put("userLogin", userLogin);
        processContext.put("orderId", orh.getOrderId());
        processContext.put("orderItems", orh.getOrderItems());
        processContext.put("shippingAddress", EntityUtil.getFirst(orh.getShippingLocations())); // TODO refactor the payment API to handle support all addresses
        processContext.put("paymentConfig", paymentConfig);
        processContext.put("currency", orh.getCurrency());
        processContext.put("orderPaymentPreference", paymentPreference);
        if (paymentPreference.get("securityCode") != null) {
            processContext.put("cardSecurityCode", paymentPreference.get("securityCode"));
        }

        // get the billing information
        getBillingInformation(orh, paymentPreference, processContext);

        // default charge is totalRemaining
        double thisAmount = totalRemaining;

        // use override or max amount available
        if (overrideAmount != null) {
            thisAmount = overrideAmount.doubleValue();
        } else if (paymentPreference.get("maxAmount") != null) {
            thisAmount = paymentPreference.getDouble("maxAmount").doubleValue();
        }

        // don't authorized more then what is required
        if (thisAmount > totalRemaining) {
            thisAmount = totalRemaining;
        }

        // format the decimal
        String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
        DecimalFormat formatter = new DecimalFormat(currencyFormat);
        String amountString = formatter.format(thisAmount);
        Double processAmount = null;
        try {
            processAmount = new Double(formatter.parse(amountString).doubleValue());
        } catch (ParseException e) {
            Debug.logError(e, "Problems parsing string formatted double to Double", module);
            throw new GeneralException("ParseException in number format", e);
        }

        if (Debug.verboseOn()) Debug.logVerbose("Charging amount: " + processAmount, module);
        processContext.put("processAmount", processAmount);

        // invoke the processor
        Map processorResult = null;
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
            if (tryOtherExpDates && (!UtilValidate.isDateAfterToday(creditCard.getString("expireDate")) || (processorResult != null && Boolean.TRUE.equals((Boolean) processorResult.get("resultBadExpire"))))) {
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
                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals((Boolean) processorResult.get("resultBadExpire"))) {
                    // okay, try one more year...
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set("expireDate", month + "/" + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                }
                
                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals((Boolean) processorResult.get("resultBadExpire"))) {
                    // okay, try one more year... and this is the last try
                    year = StringUtil.addToNumberString(year, 1);
                    creditCard.set("expireDate", month + "/" + year);
                    processorResult = dispatcher.runSync(serviceName, processContext, TX_TIME, true);
                }
                
                // at this point if we have a successful result, let's save the new creditCard expireDate
                if (!ServiceUtil.isError(processorResult) && Boolean.TRUE.equals((Boolean) processorResult.get("authResult"))) {
                    creditCard.store();
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error occurred on: " + serviceName + " => " + processContext, module);
            throw new GeneralException("Problems invoking payment processor! Will retry later. Order ID is: [" + orh.getOrderId() + "", e);
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
        GenericDelegator delegator = orderHeader.getDelegator();
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
            productStore = orderHeader.getRelatedOne("ProductStore");
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



    private static String getBillingInformation(OrderReadHelper orh, GenericValue paymentPreference, Map toContext) throws GenericEntityException {
        // gather the payment related objects.
        String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");
        GenericValue paymentMethod = paymentPreference.getRelatedOne("PaymentMethod");
        if (paymentMethod != null && "CREDIT_CARD".equals(paymentMethodTypeId)) {
            // type credit card
            GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard");
            GenericValue billingAddress = creditCard.getRelatedOne("PostalAddress");
            toContext.put("creditCard", creditCard);
            toContext.put("billingAddress", billingAddress);
        } else if (paymentMethod != null && "EFT_ACCOUNT".equals(paymentMethodTypeId)) {
            // type eft
            GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount");
            GenericValue billingAddress = eftAccount.getRelatedOne("PostalAddress");
            toContext.put("eftAccount", eftAccount);
            toContext.put("billingAddress", billingAddress);
        } else if (paymentMethod != null && "GIFT_CARD".equals(paymentMethodTypeId)) {
            // type gift card
            GenericValue giftCard = paymentMethod.getRelatedOne("GiftCard");
            toContext.put("giftCard", giftCard);
        } else if ("FIN_ACCOUNT".equals(paymentMethodTypeId)) {
            toContext.put("finAccountId", paymentPreference.getString("finAccountId"));
        } else {
            // add other payment types here; i.e. gift cards, etc.
            // unknown payment type; ignoring.
            Debug.logError("ERROR: Unsupported PaymentMethodType passed for authorization", module);
            return null;
        }

        // get some contact info.
        GenericValue billToPersonOrGroup = orh.getBillToParty();
        GenericValue billToEmail = null;

        Collection emails = ContactHelper.getContactMech(billToPersonOrGroup.getRelatedOne("Party"), "PRIMARY_EMAIL", "EMAIL_ADDRESS", false);

        if (UtilValidate.isNotEmpty(emails)) {
            billToEmail = (GenericValue) emails.iterator().next();
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
    public static Map releaseOrderPayments(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");

        Map result = new HashMap();

        // get the payment preferences
        List paymentPrefs = null;

        try {
            // get the valid payment prefs
            List othExpr = UtilMisc.toList(new EntityExpr("paymentMethodTypeId", EntityOperator.EQUALS, "EFT_ACCOUNT"));
            othExpr.add(new EntityExpr("paymentMethodTypeId", EntityOperator.EQUALS, "CREDIT_CARD"));
            othExpr.add(new EntityExpr("paymentMethodTypeId", EntityOperator.EQUALS, "GIFT_CARD"));
            othExpr.add(new EntityExpr("paymentMethodTypeId", EntityOperator.EQUALS, "FIN_ACCOUNT"));
            EntityCondition con1 = new EntityConditionList(othExpr, EntityJoinOperator.OR);

            EntityCondition statExpr = new EntityExpr("statusId", EntityOperator.EQUALS, "PAYMENT_SETTLED");
            EntityCondition con2 = new EntityConditionList(UtilMisc.toList(con1, statExpr), EntityOperator.AND);

            EntityCondition authExpr = new EntityExpr("statusId", EntityOperator.EQUALS, "PAYMENT_AUTHORIZED");
            EntityCondition con3 = new EntityConditionList(UtilMisc.toList(con2, authExpr), EntityOperator.OR);

            EntityExpr orderExpr = new EntityExpr("orderId", EntityOperator.EQUALS, orderId);
            EntityCondition con4 = new EntityConditionList(UtilMisc.toList(con3, orderExpr), EntityOperator.AND);

            paymentPrefs = delegator.findByCondition("OrderPaymentPreference", con4, null, null);
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting entity record(s), see stack trace", module);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Could not get order information (" + gee.toString() + ").");
            return result;
        }

        // return complete if no payment prefs were found
        if (paymentPrefs == null || paymentPrefs.size() == 0) {
            Debug.logWarning("No OrderPaymentPreference records available for release", module);
            result.put("processResult", "COMPLETE");
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            return result;
        }

        // iterate over the prefs and release each one
        List finished = new ArrayList();
        Iterator payments = paymentPrefs.iterator();
        while (payments.hasNext()) {
            GenericValue paymentPref = (GenericValue) payments.next();
            Map releaseContext = UtilMisc.toMap("userLogin", userLogin, "orderPaymentPreferenceId", paymentPref.getString("orderPaymentPreferenceId"));
            Map releaseResult = null;
            try {
                releaseResult = dispatcher.runSync("releaseOrderPaymentPreference", releaseContext);
            } catch( GenericServiceException e ) {
                String errMsg = "Problem calling releaseOrderPaymentPreference service for orderPaymentPreferenceId" + paymentPref.getString("orderPaymentPreferenceId");
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
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

    /**
     * 
     * Releases authorization for a single OrderPaymentPreference through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return SUCCESS|FAILED|ERROR for complete processing of payment.
     */
    public static Map releaseOrderPaymentPreference(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderPaymentPreferenceId = (String) context.get("orderPaymentPreferenceId");

        Map result = ServiceUtil.returnSuccess();

        // Get the OrderPaymentPreference
        GenericValue paymentPref = null;
        try {
            paymentPref = delegator.findByPrimaryKey("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId));
        } catch( GenericEntityException e ) {
            String errMsg = "Problem getting OrderPaymentPreference for orderPaymentPreferenceId " + orderPaymentPreferenceId; 
            Debug.logWarning(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // Error if no OrderPaymentPreference was found
        if (paymentPref == null) {
            String errMsg = "Could not find OrderPaymentPreference with orderPaymentPreferenceId: " + orderPaymentPreferenceId; 
            Debug.logWarning(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // Get the OrderHeader
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", paymentPref.getString("orderId")));
        } catch( GenericEntityException e ) {
            String errMsg = "Problem getting OrderHeader for orderId " + paymentPref.getString("orderId"); 
            Debug.logWarning(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // Error if no OrderHeader was found
        if (orderHeader == null) {
            String errMsg = "Could not find OrderHeader with orderId: " + paymentPref.getString("orderId") + "; not processing payments."; 
            Debug.logWarning(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        String currency = orh.getCurrency();

        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;

        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orderHeader, paymentPref, RELEASE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            serviceName = paymentSettings.getString("paymentService");
            if (serviceName == null) {
                String errMsg = "No payment release service for - " + paymentPref.getString("paymentMethodTypeId"); 
                Debug.logWarning(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            String errMsg = "No payment release settings found for - " + paymentPref.getString("paymentMethodTypeId"); 
            Debug.logWarning(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        if (paymentConfig == null || paymentConfig.length() == 0) {
            paymentConfig = "payment.properties";
        }

        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(paymentPref);
        Map releaseContext = new HashMap();
        releaseContext.put("orderPaymentPreference", paymentPref);
        releaseContext.put("releaseAmount", authTransaction.getDouble("amount"));
        releaseContext.put("currency", currency);
        releaseContext.put("paymentConfig", paymentConfig);
        releaseContext.put("userLogin", userLogin);

        // run the defined service
        Map releaseResult = null;
        try {
            releaseResult = dispatcher.runSync(serviceName, releaseContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            String errMsg = "Problem releasing payment";
            Debug.logError(e,errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // get the release result code
        if (releaseResult != null && !ServiceUtil.isError(releaseResult)) {
            Boolean releaseResponse = (Boolean) releaseResult.get("releaseResult");

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue pgResponse = delegator.makeValue("PaymentGatewayResponse", null);
            pgResponse.set("paymentGatewayResponseId", responseId);
            pgResponse.set("paymentServiceTypeEnumId", RELEASE_SERVICE_TYPE);
            pgResponse.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
            pgResponse.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
            pgResponse.set("paymentMethodId", paymentPref.get("paymentMethodId"));
            pgResponse.set("transCodeEnumId", "PGT_RELEASE");

            // set the release info
            pgResponse.set("referenceNum", releaseResult.get("releaseRefNum"));
            pgResponse.set("altReference", releaseResult.get("releaseAltRefNum"));
            pgResponse.set("gatewayCode", releaseResult.get("releaseCode"));
            pgResponse.set("gatewayFlag", releaseResult.get("releaseFlag"));
            pgResponse.set("gatewayMessage", releaseResult.get("releaseMessage"));
            pgResponse.set("transactionDate", UtilDateTime.nowTimestamp());

            // store the gateway response
            try {
                pgResponse.create();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem storing PaymentGatewayResponse entity; authorization was released! : " + pgResponse, module);
            }

            // create the internal messages
            List messages = (List) releaseResult.get("internalRespMsgs");
            if (messages != null && messages.size() > 0) {
                Iterator i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg", null);
                    String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                    String message = (String) i.next();
                    respMsg.set("paymentGatewayRespMsgId", respMsgId);
                    respMsg.set("paymentGatewayResponseId", responseId);
                    respMsg.set("pgrMessage", message);
                    try {
                        delegator.create(respMsg);
                    } catch (GenericEntityException e) {
                        String errMsg = "Unable to create PaymentGatewayRespMsg record"; 
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
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
                List paymentList = null;
                try {
                    paymentList = paymentPref.getRelated("Payment");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get Payment records from OrderPaymentPreference : " + paymentPref, module);
                }

                if (paymentList != null) {
                    Iterator pi = paymentList.iterator();
                    while (pi.hasNext()) {
                        GenericValue pay = (GenericValue) pi.next();
                        pay.set("statusId", "PMNT_CANCELLED");
                        try {
                            pay.store();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Unable to store Payment : " + pay, module);
                        }
                    }
                }
            } else {
                String errMsg = "Release failed for pref : " + paymentPref; 
                Debug.logError(errMsg, module);
                result = ServiceUtil.returnFailure(errMsg);
            }
        } else if (ServiceUtil.isError(releaseResult)) {
            saveError(dispatcher, userLogin, paymentPref, releaseResult, RELEASE_SERVICE_TYPE, "PGT_RELEASE");
            result = ServiceUtil.returnError(ServiceUtil.getErrorMessage(releaseResult));
        }

        return result;
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map capturePaymentsByInvoice(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");

        // lookup the invoice
        GenericValue invoice = null;
        try {
            invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble looking up Invoice #" + invoiceId, module);
            return ServiceUtil.returnError("Trouble looking up Invoice #" + invoiceId);
        }

        if (invoice == null) {
            Debug.logError("Could not locate invoice #" + invoiceId, module);
            return ServiceUtil.returnError("Could not locate invoice #" + invoiceId);
        }

        // get the OrderItemBilling records for this invoice
        List orderItemBillings = null;
        try {
            orderItemBillings = invoice.getRelated("OrderItemBilling");
        } catch (GenericEntityException e) {
            Debug.logError("Trouble getting OrderItemBilling(s) from Invoice #" + invoiceId, module);
            return ServiceUtil.returnError("Trouble getting OrderItemBilling(s) from Invoice #" + invoiceId);
        }

        // check for an associated billing account
        String billingAccountId = invoice.getString("billingAccountId");

        // make sure they are all for the same order
        String testOrderId = null;
        boolean allSameOrder = true;
        if (orderItemBillings != null) {
            Iterator oii = orderItemBillings.iterator();
            while (oii.hasNext()) {
                GenericValue oib = (GenericValue) oii.next();
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
            return ServiceUtil.returnSuccess();
        }

        // get the invoice amount (amount to bill)
        double invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
        if (Debug.infoOn()) Debug.logInfo("(Capture) Invoice [#" + invoiceId + "] total: " + invoiceTotal, module);

        // now capture the order
        Map serviceContext = UtilMisc.toMap("userLogin", userLogin, "orderId", testOrderId, "invoiceId", invoiceId, "captureAmount", new Double(invoiceTotal));
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            serviceContext.put("billingAccountId", billingAccountId);
        }
        try {
            return dispatcher.runSync("captureOrderPayments", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Trouble running captureOrderPayments service", module);
            return ServiceUtil.returnError("Trouble running captureOrderPayments service");
        }
    }

    /**
     * Captures payments through service calls to the defined processing service for the ProductStore/PaymentMethodType
     * @return COMPLETE|FAILED|ERROR for complete processing of ALL payment methods.
     */
    public static Map captureOrderPayments(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        Double captureAmount = (Double) context.get("captureAmount");
        BigDecimal captureAmountBd = new BigDecimal(captureAmount.doubleValue());

        // get the order header and payment preferences
        GenericValue orderHeader = null;
        List paymentPrefs = null;

        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            // get the payment prefs
            Map lookupMap = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_AUTHORIZED");
            List orderList = UtilMisc.toList("-maxAmount");
            paymentPrefs = delegator.findByAnd("OrderPaymentPreference", lookupMap, orderList);
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Problems getting entity record(s), see stack trace", module);
            return ServiceUtil.returnError("ERROR: Could not get order information (" + gee.toString() + ").");
        }

        // error if no order was found
        if (orderHeader == null) {
            return ServiceUtil.returnError("Could not find OrderHeader with orderId: " + orderId + "; not processing payments.");
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        
        // See if there is a billing account first.  If so, just charge the captureAmount to the billing account via PaymentApplication
        GenericValue billingAccount = null;
        BigDecimal billingAccountAvail = null;
        BigDecimal billingAccountCaptureAmount = ZERO;
        Map billingAccountInfo = null;
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            try {
                billingAccountInfo = dispatcher.runSync("calcBillingAccountBalance", UtilMisc.toMap("billingAccountId", billingAccountId));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Unable to get billing account information for #" + billingAccountId, module);
            }
        }
        if (billingAccountInfo != null) {
            billingAccount = (GenericValue) billingAccountInfo.get("billingAccount");
            // use available to capture because we want to know how much we can charge to a billing account
            Double availableToCapture = (Double) billingAccountInfo.get("availableToCapture");  
            billingAccountAvail = new BigDecimal(availableToCapture.doubleValue());
        }
        
        // if a billing account is used to pay for an order, then charge as much as we can to it before proceeding to other payment methods.
        if (billingAccount != null && billingAccountAvail != null) {
            try {
                // the amount to be "charged" to the billing account, which is the minimum of the amount the order wants to charge to the billing account 
                // or the amount still available for capturing from the billing account or the total amount to be captured on the order 
                BigDecimal billingAccountMaxAmount = new BigDecimal(orh.getBillingAccountMaxAmount());
                billingAccountCaptureAmount = billingAccountMaxAmount.min(billingAccountAvail).min(captureAmountBd);
                Debug.logInfo("billing account avail = [" + billingAccountAvail + "] capture amount = [" + billingAccountCaptureAmount + "] maxAmount = ["+billingAccountMaxAmount+"]", module);
                // capturing to a billing account if amount is greater than zero
                if (billingAccountCaptureAmount.compareTo(ZERO) == 1) {
                    Map tmpResult = dispatcher.runSync("captureBillingAccountPayment", UtilMisc.toMap("invoiceId", invoiceId, "billingAccountId", billingAccountId,
                            "captureAmount", new Double(billingAccountCaptureAmount.doubleValue()), "orderId", orderId, "userLogin", userLogin));
                    if (ServiceUtil.isError(tmpResult)) {
                        return tmpResult;
                    }

                    // now, if the full amount had not been captured, then capture
                    // it from other methods, otherwise return
                    if (billingAccountCaptureAmount.compareTo(captureAmountBd) == -1) {
                        BigDecimal outstandingAmount = captureAmountBd.subtract(billingAccountCaptureAmount).setScale(decimals, rounding);
                        captureAmountBd = outstandingAmount;
                    } else {
                        Debug.logInfo("Amount to capture [" + captureAmountBd + "] was fully captured in Payment [" + tmpResult.get("paymentId") + "].", module);
                        Map result = ServiceUtil.returnSuccess();
                        result.put("processResult", "COMPLETE");
                        return result;
                    }
               }
            } catch (GenericServiceException ex) {
                return ServiceUtil.returnError(ex.getMessage());
            }
        }            
        
        // return complete if no payment prefs were found
        if (paymentPrefs == null || paymentPrefs.size() == 0) {
            Debug.logWarning("No orderPaymentPreferences available to capture", module);
            Map result = ServiceUtil.returnSuccess();
            result.put("processResult", "COMPLETE");
            return result;
        }

        BigDecimal orderGrandTotal = orh.getOrderGrandTotalBd();
        orderGrandTotal = orderGrandTotal.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal totalPayments = PaymentWorker.getPaymentsTotal(orh.getOrderPayments());
        totalPayments = totalPayments.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal remainingTotalBd = orderGrandTotal.subtract(totalPayments);
        if (Debug.infoOn()) Debug.logInfo("Capture Remaining Total: " + remainingTotalBd, module);

        double amountToCapture = 0.0;
        if (captureAmountBd == null) {
            amountToCapture = remainingTotalBd.doubleValue();
        } else {
            amountToCapture = captureAmountBd.doubleValue();
        }
        if (Debug.infoOn()) Debug.logInfo("Actual Expected Capture Amount : " + amountToCapture, module);

        // iterate over the prefs and capture each one until we meet our total
        List finished = new ArrayList();
        Iterator payments = paymentPrefs.iterator();
        while (payments.hasNext()) {
            // DEJ20060708: Do we really want to just log and ignore the errors like this? I've improved a few of these in a review today, but it is being done all over...
            GenericValue paymentPref = (GenericValue) payments.next();
            GenericValue authTrans = getAuthTransaction(paymentPref);
            if (authTrans == null) {
                continue;
            }

            Double authAmount = authTrans.getDouble("amount");
            if (authAmount == null) authAmount = new Double(0.00);
            if (authAmount.doubleValue() == 0.00) {
                // nothing to capture
                Debug.logInfo("Nothing to capture; authAmount = 0", module);
                continue;
            }
            //Debug.log("Actual Auth amount : " + authAmount, module);
 
            // if the authAmount is more then the remaining total; just use remaining total
            if (authAmount.doubleValue() > remainingTotalBd.doubleValue()) {
                authAmount = new Double(remainingTotalBd.doubleValue());
            }

            // if we have a billing account; total up auth + account available
            double amountToBillAccount = 0.00;
            if (billingAccountAvail != null) {
                amountToBillAccount = authAmount.doubleValue() + billingAccountAvail.doubleValue();
            }

            // the amount for *this* capture
            double amountThisCapture = 0.00;

            // determine how much for *this* capture
            if (authAmount.doubleValue() >= amountToCapture) {
                // if the auth amount is more then expected capture just capture what is expected
                amountThisCapture = amountToCapture;
            } else if (payments.hasNext()) {
                // if we have more payments to capture; just capture what was authorized
                amountThisCapture = authAmount.doubleValue();
            } else if (billingAccountAvail != null && amountToBillAccount >= amountToCapture) {
                // the provided billing account will cover the remaining; just capture what was autorized
                amountThisCapture = authAmount.doubleValue();
            } else {
                // we need to capture more then what was authorized; re-auth for the new amount
                // TODO: add what the billing account cannot support to the re-auth amount
                // TODO: add support for re-auth for additional funds
                // just in case; we will capture the authorized amount here; until this is implemented
                Debug.logError("The amount to capture was more then what was authorized; we only captured the authorized amount : " + paymentPref, module);
                amountThisCapture = authAmount.doubleValue();
            }
           
            Debug.logInfo("Payment preference = [" + paymentPref + "] amount to capture = [" + amountToCapture +"] amount of this capture = [" + amountThisCapture +"] actual auth amount =[" + authAmount + "] amountToBillAccount = [" + amountToBillAccount + "]", module); 
            Map captureResult = capturePayment(dctx, userLogin, orh, paymentPref, amountThisCapture);
            if (captureResult != null) {
                // credit card processors return captureAmount, but gift certificate processors return processAmount
                Double amountCaptured = (Double) captureResult.get("captureAmount");
                if (amountCaptured == null) {
                    amountCaptured = (Double) captureResult.get("processAmount");
                }
                // decrease amount of next payment preference to capture
                if (amountCaptured != null) amountToCapture -= amountCaptured.doubleValue();
                finished.add(captureResult);

                // add the invoiceId to the result for processing
                captureResult.put("invoiceId", invoiceId);

               // process the capture's results
                try {
                    processResult(dctx, captureResult, userLogin, paymentPref);
                } catch (GeneralException e) {
                    Debug.logError(e, "Trouble processing the result; captureResult: " + captureResult, module);
                    return ServiceUtil.returnError("Trouble processing the capture results");
                }

                // create any splits which are needed
                BigDecimal totalAmountCaptured = new BigDecimal(amountThisCapture);
                if (authAmount.doubleValue() > totalAmountCaptured.doubleValue()) {
                    // create a new payment preference and authorize it
                    double newAmount = authAmount.doubleValue() - totalAmountCaptured.doubleValue(); // TODO: use BigDecimal arithmetic here (and everywhere else for that matter)
                    Debug.logInfo("Creating payment preference split", module);
                    String newPrefId = delegator.getNextSeqId("OrderPaymentPreference");
                    GenericValue newPref = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", newPrefId));
                    newPref.set("orderId", paymentPref.get("orderId"));
                    newPref.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
                    newPref.set("paymentMethodId", paymentPref.get("paymentMethodId"));
                    newPref.set("maxAmount", paymentPref.get("maxAmount"));
                    newPref.set("statusId", "PAYMENT_NOT_AUTH");
                    newPref.set("createdDate", UtilDateTime.nowTimestamp());
                    if (userLogin != null) {
                        newPref.set("createdByUserLogin", userLogin.getString("userLoginId"));
                    }
                    Debug.logInfo("New preference : " + newPref, module);
                    Map processorResult = null;
                    try {
                        // create the new payment preference
                        delegator.create(newPref);

                        // authorize the new preference
                        processorResult = authPayment(dispatcher, userLogin, orh, newPref, newAmount, false, null);
                        if (processorResult != null) {
                            // process the auth results
                            boolean authResult = processResult(dctx, processorResult, userLogin, newPref);
                            if (!authResult) {
                                Debug.logError("Authorization failed : " + newPref + " : " + processorResult, module);
                            }
                        } else {
                            Debug.logError("Payment not authorized : " + newPref + " : " + processorResult, module);
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
                }
            } else {
                Debug.logError("Payment not captured", module);
                continue;
            }
        }

        if (amountToCapture > 0.00) {
            GenericValue productStore = orh.getProductStore();
            if (! UtilValidate.isEmpty(productStore)) {
                boolean shipIfCaptureFails = UtilValidate.isEmpty(productStore.get("shipIfCaptureFails")) || "Y".equalsIgnoreCase(productStore.getString("shipIfCaptureFails"));
                if(! shipIfCaptureFails) {
                    return ServiceUtil.returnError("Cannot ship order because credit card captures were unsuccessful");
                }
            }
            Map result = ServiceUtil.returnSuccess();
            result.put("processResult", "FAILED");
            return result;
        } else {
            Map result = ServiceUtil.returnSuccess();
            result.put("processResult", "COMPLETE");
            return result;
        }
    }

    public static Map captureBillingAccountPayment(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        Double captureAmount = (Double) context.get("captureAmount");
        String orderId = (String) context.get("orderId");
        Map results = ServiceUtil.returnSuccess();
        
        try {
            // Note that the partyIdFrom of the Payment should be the partyIdTo of the invoice, since you're receiving a payment from the party you billed
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            Map paymentParams = UtilMisc.toMap("paymentTypeId", "CUSTOMER_PAYMENT", "paymentMethodTypeId", "EXT_BILLACT", 
                    "partyIdFrom", invoice.getString("partyId"), "partyIdTo", invoice.getString("partyIdFrom"), 
                    "statusId", "PMNT_RECEIVED", "effectiveDate", UtilDateTime.nowTimestamp());
            paymentParams.put("amount", captureAmount);
            paymentParams.put("currencyUomId", invoice.getString("currencyUomId"));
            paymentParams.put("userLogin", userLogin);
            Map tmpResult = dispatcher.runSync("createPayment", paymentParams);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            } 
            
            String paymentId = (String) tmpResult.get("paymentId");
            tmpResult = dispatcher.runSync("createPaymentApplication", UtilMisc.toMap("paymentId", paymentId, "invoiceId", invoiceId, "billingAccountId", billingAccountId, 
                    "amountApplied", captureAmount, "userLogin", userLogin));
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }
            if (paymentId == null) {
                return ServiceUtil.returnError("No payment created for invoice [" + invoiceId + "] and billing account [" + billingAccountId + "]");
            }
            results.put("paymentId", paymentId);
            
            if (orderId != null && captureAmount.doubleValue() > 0) {
                // Create a paymentGatewayResponse, if necessary
                GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                if (order == null) {
                    return ServiceUtil.returnError("No paymentGatewayResponse created for invoice [" + invoiceId + "] and billing account [" + billingAccountId + "]: Order with ID [" + orderId + "] not found!");
                }
                // See if there's an orderPaymentPreference - there should be only one OPP for EXT_BILLACT per order
                List orderPaymentPreferences = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "paymentMethodTypeId", "EXT_BILLACT"));
                if (orderPaymentPreferences != null && orderPaymentPreferences.size() > 0) {
                    GenericValue orderPaymentPreference = EntityUtil.getFirst(orderPaymentPreferences);
                    
                    // Check the productStore setting to see if we need to do this explicitly
                    GenericValue productStore = order.getRelatedOne("ProductStore");
                    if (productStore.getString("manualAuthIsCapture") == null || (! productStore.getString("manualAuthIsCapture").equalsIgnoreCase("Y"))) {        
                        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
                        GenericValue pgResponse = delegator.makeValue("PaymentGatewayResponse", null);
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
                        pgResponse.create();

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
    
    private static Map capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh, GenericValue paymentPref, double amount) {
        return capturePayment(dctx, userLogin, orh, paymentPref, amount, null);
    }

    private static Map capturePayment(DispatchContext dctx, GenericValue userLogin, OrderReadHelper orh, GenericValue paymentPref, double amount, GenericValue authTrans) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // look up the payment configuration settings
        String serviceName = null;
        String paymentConfig = null;

        // get the payment settings i.e. serviceName and config properties file name
        GenericValue paymentSettings = getPaymentSettings(orh.getOrderHeader(), paymentPref, CAPTURE_SERVICE_TYPE, false);
        if (paymentSettings != null) {
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            serviceName = paymentSettings.getString("paymentService");
            if (serviceName == null) {
                Debug.logError("Service name is null for payment setting; cannot process", module);
                return null;
            }
        } else {
            Debug.logError("Invalid payment settings entity, no payment settings found", module);
            return null;
        }

        if (paymentConfig == null || paymentConfig.length() == 0) {
            paymentConfig = "payment.properties";
        }

        // check the validity of the authorization; re-auth if necessary
        if (!PaymentGatewayServices.checkAuthValidity(paymentPref, paymentConfig)) {
            try {
                // re-auth required before capture
                Map processorResult = PaymentGatewayServices.authPayment(dispatcher, userLogin, orh, paymentPref, amount, true, null);

                boolean authResult = false;
                if (processorResult != null) {
                    // process the auth results
                    try {
                        authResult = processResult(dctx, processorResult, userLogin, paymentPref);
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
                String errMsg = "Error re-authorizing payment: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }

        // prepare the context for the capture service (must follow the ccCaptureInterface
        Map captureContext = new HashMap();
        captureContext.put("userLogin", userLogin);
        captureContext.put("orderPaymentPreference", paymentPref);
        captureContext.put("paymentConfig", paymentConfig);
        captureContext.put("currency", orh.getCurrency());

        // this is necessary because the ccCaptureInterface uses "captureAmount" but the paymentProcessInterface uses "processAmount"
        try {
            ModelService captureService = dctx.getModelService(serviceName);
            Set inParams = captureService.getInParamNames();
            if (inParams.contains("captureAmount")) {
                captureContext.put("captureAmount", new Double(amount));    
            } else if (inParams.contains("processAmount")) {
                captureContext.put("processAmount", new Double(amount));    
            } else {
                return ServiceUtil.returnError("Service [" + serviceName + "] does not have a captureAmount or processAmount.  Its parameters are: " + inParams);
            }
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError("Cannot get model service for " + serviceName);
        }
        
        
        if (authTrans != null) {
            captureContext.put("authTrans", authTrans);
        }

        Debug.logInfo("Capture [" + serviceName + "] : " + captureContext, module);

        // now invoke the capture service
        Map captureResult = null;
        try {
            captureResult = dispatcher.runSync(serviceName, captureContext, TX_TIME, true);
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

    private static void saveError(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPref, Map result, String serviceType, String transactionCode) {
        Map serviceContext = new HashMap();
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

    public static Map storePaymentErrorMessage(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        String serviceType = (String) context.get("paymentServiceTypeEnumId");
        String transactionCode = (String) context.get("transCodeEnumId");
        Map result = (Map) context.get("serviceResultMap");

        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse", null);
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", serviceType);
        response.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        response.set("paymentMethodId", paymentPref.get("paymentMethodId"));
        response.set("transCodeEnumId", transactionCode);
        response.set("referenceNum", "ERROR");
        response.set("gatewayMessage", ServiceUtil.getErrorMessage(result));
        response.set("transactionDate", UtilDateTime.nowTimestamp());

        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to create PaymentGatewayResponse for failed service call!");
        }

        Debug.logInfo("Created PaymentGatewayResponse record for returned error", module);
        return ServiceUtil.returnSuccess();
    }

    private static boolean processResult(DispatchContext dctx, Map result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        Boolean authResult = (Boolean) result.get("authResult");
        Boolean captureResult = (Boolean) result.get("captureResult");
        boolean resultPassed = false;
        String initialStatus = paymentPreference.getString("statusId");
        String authServiceType = null;

        if (authResult != null) {
            processAuthResult(dctx, result, userLogin, paymentPreference);
            resultPassed = authResult.booleanValue();
            authServiceType = ("PAYMENT_NOT_AUTH".equals(initialStatus)) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;;
        }
        if (captureResult != null) {
            processCaptureResult(dctx, result, userLogin, paymentPreference, authServiceType);
            if (!resultPassed)
                resultPassed = captureResult.booleanValue();
        }
        return resultPassed;
    }

    private static void processAuthResult(DispatchContext dctx, Map result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        result.put("userLogin", userLogin);
        result.put("orderPaymentPreference", paymentPreference);
        ModelService model = dctx.getModelService("processAuthResult");
        Map context = model.makeValid(result, ModelService.IN_PARAM);
        Map svcResp = null;
        try {
            svcResp = dispatcher.runSync("processAuthResult", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw e;
        }
        if (svcResp != null && ServiceUtil.isError(svcResp)) {
           Debug.logError(ServiceUtil.getErrorMessage(svcResp), module);
           throw new GeneralException(ServiceUtil.getErrorMessage(svcResp));
        }
    }

    public static Map processAuthResult(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        Boolean authResult = (Boolean) context.get("authResult");
        String authType = (String) context.get("serviceTypeEnum");
        String currencyUomId = (String) context.get("currencyUomId");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // type of auth this was can be determined by the previous status
        if (UtilValidate.isEmpty(authType)) {
            authType = ("PAYMENT_NOT_AUTH".equals(orderPaymentPreference.getString("statusId"))) ? AUTH_SERVICE_TYPE : REAUTH_SERVICE_TYPE;
        }

        try {
            String paymentMethodId = orderPaymentPreference.getString("paymentMethodId");
            GenericValue paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
            GenericValue creditCard = null;
            if (paymentMethod != null && "CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                creditCard = paymentMethod.getRelatedOne("CreditCard");
            }

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse", null);
            response.set("paymentGatewayResponseId", responseId);
            response.set("paymentServiceTypeEnumId", authType);
            response.set("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));
            response.set("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
            response.set("paymentMethodId", orderPaymentPreference.get("paymentMethodId"));
            response.set("transCodeEnumId", "PGT_AUTHORIZE");
            response.set("currencyUomId", currencyUomId);
    
            // set the avs/fraud result
            response.set("gatewayAvsResult", context.get("avsCode"));
            response.set("gatewayScoreResult", context.get("scoreCode"));
    
            // set the auth info
            response.set("amount", context.get("processAmount"));
            response.set("referenceNum", context.get("authRefNum"));
            response.set("altReference", context.get("authAltRefNum"));
            response.set("gatewayCode", context.get("authCode"));
            response.set("gatewayFlag", context.get("authFlag"));
            response.set("gatewayMessage", context.get("authMessage"));
            response.set("transactionDate", UtilDateTime.nowTimestamp());
            
            if (Boolean.TRUE.equals((Boolean) context.get("resultDeclined"))) response.set("resultDeclined", "Y");
            if (Boolean.TRUE.equals((Boolean) context.get("resultNsf"))) response.set("resultNsf", "Y");
            if (Boolean.TRUE.equals((Boolean) context.get("resultBadExpire"))) response.set("resultBadExpire", "Y");
            if (Boolean.TRUE.equals((Boolean) context.get("resultBadCardNumber"))) response.set("resultBadCardNumber", "Y");
            
            response.create();
    
            // create the internal messages
            List messages = (List) context.get("internalRespMsgs");
            if (messages != null && messages.size() > 0) {
                Iterator i = messages.iterator();
                while (i.hasNext()) {
                    GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg", null);
                    String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                    String message = (String) i.next();
                    respMsg.set("paymentGatewayRespMsgId", respMsgId);
                    respMsg.set("paymentGatewayResponseId", responseId);
                    respMsg.set("pgrMessage", message);
                    delegator.create(respMsg);
                }
            }
    
            if (response.getDouble("amount").doubleValue() != ((Double) context.get("processAmount")).doubleValue()) {
                Debug.logWarning("The authorized amount does not match the max amount : Response - " + response + " : result - " + context, module);
            }
    
            // set the status of the OrderPaymentPreference
            if (context != null && authResult.booleanValue()) {
                orderPaymentPreference.set("statusId", "PAYMENT_AUTHORIZED");
                orderPaymentPreference.set("securityCode", null);
            } else if (context != null && !authResult.booleanValue()) {
                orderPaymentPreference.set("statusId", "PAYMENT_DECLINED");
            } else {
                orderPaymentPreference.set("statusId", "PAYMENT_ERROR");
            }
            
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
                        creditCard.set("consecutiveFailedAuths", new Long(1));
                    } else {
                        creditCard.set("consecutiveFailedAuths", new Long(consecutiveFailedAuths.longValue() + 1));
                    }
                    creditCard.set("lastFailedAuthDate", nowTimestamp);
                    
                    if (Boolean.TRUE.equals((Boolean) context.get("resultNsf"))) {
                        Long consecutiveFailedNsf = creditCard.getLong("consecutiveFailedNsf");
                        if (consecutiveFailedNsf == null) {
                            creditCard.set("consecutiveFailedNsf", new Long(1));
                        } else {
                            creditCard.set("consecutiveFailedNsf", new Long(consecutiveFailedNsf.longValue() + 1));
                        }
                        creditCard.set("lastFailedNsfDate", nowTimestamp);
                    }
                    
                    creditCard.store();
                }
            }
            
            // auth was successful, to clear out any failed auth or nsf info
            if (authResult.booleanValue()) {
                if ((creditCard != null) && (creditCard.get("lastFailedAuthDate") != null)) {
                    creditCard.set("consecutiveFailedAuths", new Long(0));
                    creditCard.set("lastFailedAuthDate", null);
                    creditCard.set("consecutiveFailedNsf", new Long(0));
                    creditCard.set("lastFailedNsfDate", null);
                    
                    creditCard.store();
                }
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error updating payment status information: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }
    
    private static boolean needsNsfRetry(GenericValue orderPaymentPreference, Map processContext, GenericDelegator delegator) throws GenericEntityException {
        boolean needsNsfRetry = false;
        if (Boolean.TRUE.equals((Boolean) processContext.get("resultNsf"))) {
            // only track this for auto-orders, since we will only not fail and re-try on those
            GenericValue orderHeader = orderPaymentPreference.getRelatedOne("OrderHeader");
            if (UtilValidate.isNotEmpty(orderHeader.getString("autoOrderShoppingListId"))) {
                GenericValue productStore = orderHeader.getRelatedOne("ProductStore");
                if ("Y".equals(productStore.getString("autoOrderCcTryLaterNsf"))) {
                    // one last condition: make sure there have been less than ProductStore.autoOrderCcTryLaterMax 
                    //   PaymentGatewayResponse records with the same orderPaymentPreferenceId and paymentMethodId (just in case it has changed)
                    //   and that have resultNsf = Y, ie only consider other NSF responses
                    Long autoOrderCcTryLaterMax = productStore.getLong("autoOrderCcTryLaterMax");
                    if (autoOrderCcTryLaterMax != null) {
                        long failedTries = delegator.findCountByAnd("PaymentGatewayResponse", 
                                UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"), 
                                "paymentMethodId", orderPaymentPreference.get("paymentMethodId"),
                                "resultNsf", "Y"));
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

    private static GenericValue processAuthRetryResult(DispatchContext dctx, Map result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        processAuthResult(dctx, result, userLogin, paymentPreference);
        return getAuthTransaction(paymentPreference);
    }

    private static void processCaptureResult(DispatchContext dctx, Map result, GenericValue userLogin, GenericValue paymentPreference) throws GeneralException {
        processCaptureResult(dctx, result, userLogin, paymentPreference, null);
    }

    private static void processCaptureResult(DispatchContext dctx, Map result, GenericValue userLogin, GenericValue paymentPreference, String authServiceType) throws GeneralException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Boolean captureResult = (Boolean) result.get("captureResult");
        Double amount = null;
        if (result.get("captureAmount") != null) {
            amount = (Double) result.get("captureAmount");
        } else if (result.get("processAmount") != null) {
            amount = (Double) result.get("processAmount");
            result.put("captureAmount", amount);
        }

        if (amount == null) {
            throw new GeneralException("Unable to process null capture amount");
        }

        if (result != null && captureResult.booleanValue()) {
            result.put("orderPaymentPreference", paymentPreference);
            result.put("userLogin", userLogin);
            result.put("serviceTypeEnum", authServiceType);

            ModelService model = dctx.getModelService("processCaptureResult");
            Map context = model.makeValid(result, ModelService.IN_PARAM);
            Map capRes = null;
            try {
                capRes = dispatcher.runSync("processCaptureResult", context);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                throw e;
            }
            if (capRes != null && ServiceUtil.isError(capRes)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(capRes));
            }
        } else if (result != null && !captureResult.booleanValue()) {
            // problem with the capture lets get some needed info
            OrderReadHelper orh = null;
            try {
                GenericValue orderHeader = paymentPreference.getRelatedOne("OrderHeader");
                if (orderHeader != null)
                    orh = new OrderReadHelper(orderHeader);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems getting OrderHeader; cannot re-auth the payment", module);
            }

            if (amount != null && amount.doubleValue() == new Double(0.00).doubleValue()) {
                amount = paymentPreference.getDouble("maxAmount");
                Debug.log("resetting payment amount from 0.00 to correctMax amount", module);
            }
            Debug.log("reauth with amount: " + amount, module);
            if (orh != null) {
                // first lets re-auth the card
                Map authPayRes = authPayment(dispatcher, userLogin, orh, paymentPreference, amount.doubleValue(), true, null);
                Debug.log("authPayRes: " + authPayRes, module);
                if (authPayRes != null) {
                    Boolean authResp = (Boolean) authPayRes.get("authResult");
                    Boolean capResp = (Boolean) authPayRes.get("captureResult");
                    if (authResp != null) {
                        GenericValue authTrans = processAuthRetryResult(dctx, authPayRes, userLogin, paymentPreference);

                        if (authResp.booleanValue()) {
                            // first make sure we didn't already capture - probably not
                            if (capResp != null && capResp.booleanValue()) {
                                processCaptureResult(dctx, result, userLogin, paymentPreference);
                            } else {
                                // lets try to capture the funds now
                                Map capPayRes = capturePayment(dctx, userLogin, orh, paymentPreference, amount.doubleValue(), authTrans);
                                if (capPayRes != null) {
                                    Boolean capPayResp = (Boolean) capPayRes.get("captureResult");
                                    if (capPayResp != null && capPayResp.booleanValue()) {
                                        // it was successful
                                        processCaptureResult(dctx, capPayRes, userLogin, paymentPreference);
                                    } else {
                                        // not successful; log it
                                        Debug.logError("Capture of authorized payment failed: " + paymentPreference, module);
                                    }
                                } else {
                                    Debug.logError("Problems trying to capture payment (null result): " + paymentPreference, module);
                                }
                            }
                        } else {
                            Debug.logError("Payment authorization failed:  " + paymentPreference, module);
                        }
                    } else {
                        Debug.logError("Payment authorization failed (null result):  " + paymentPreference, module);
                    }
                } else {
                    Debug.logError("Problems trying to re-authorize the payment (null result): " + paymentPreference, module);
                }
            } else {
                Debug.logError("Null OrderReadHelper cannot process", module);
            }
        } else {
            Debug.logError("Result pass is null, no capture available", module);
        }
    }

    public static Map processCaptureResult(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue paymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        String payTo = (String) context.get("payToPartyId");
        Double amount = (Double) context.get("captureAmount");
        String serviceType = (String) context.get("serviceTypeEnum");
        String currencyUomId = (String) context.get("currencyUomId");
        
        if (UtilValidate.isEmpty(serviceType)) {
            serviceType = CAPTURE_SERVICE_TYPE;
        }

        // create the PaymentGatewayResponse record
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse", null);
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", serviceType);
        response.set("orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPreference.get("paymentMethodTypeId"));
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
        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error creating response information");
        }

        // create the internal messages
        List messages = (List) context.get("internalRespMsgs");
        if (messages != null && messages.size() > 0) {
            Iterator i = messages.iterator();
            while (i.hasNext()) {
                GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg", null);
                String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                String message = (String) i.next();
                respMsg.set("paymentGatewayRespMsgId", respMsgId);
                respMsg.set("paymentGatewayResponseId", responseId);
                respMsg.set("pgrMessage", message);
                try {
                    delegator.create(respMsg);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Error creating response message information");
                }
            }
        }

        // get the invoice
        GenericValue invoice = null;
        if (invoiceId != null) {
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                String message = "Failed to process capture result:  Could not find invoice ["+invoiceId+"] due to entity error: " + e.getMessage();
                Debug.logError(e, message, module);
                return ServiceUtil.returnError(message );
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
            List orl = null;
            try {
                orl = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "BILL_TO_CUSTOMER"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (orl.size() > 0) {
                GenericValue orderRole = EntityUtil.getFirst(orl);
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


        Map paymentCtx = UtilMisc.toMap("paymentTypeId", "CUSTOMER_PAYMENT");
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

        Map payRes = null;
        try {
            payRes = dispatcher.runSync("createPayment", paymentCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error creating payment record");
        }
        if (payRes != null && ServiceUtil.isError(payRes)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(payRes));
        }

        String paymentId = (String) payRes.get("paymentId");
        paymentPreference.set("statusId", "PAYMENT_SETTLED");
        try {
            paymentPreference.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        // create the PaymentApplication if invoiceId is available
        if (invoiceId != null) {
            Debug.logInfo("Processing Invoice #" + invoiceId, module);
            Map paCtx = UtilMisc.toMap("paymentId", paymentId, "invoiceId", invoiceId);
            paCtx.put("amountApplied", context.get("captureAmount"));
            paCtx.put("userLogin", userLogin);
            Map paRes = null;
            try {
                paRes = dispatcher.runSync("createPaymentApplication", paCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Error creating invoice application");
            }
            if (paRes != null && ServiceUtil.isError(paRes)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(paRes));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map refundPayment(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Double refundAmount = (Double) context.get("refundAmount");

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne("OrderHeader");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader from OrderPaymentPreference", module);
            return ServiceUtil.returnError("Problems getting OrderHeader from OrderPaymentPreference: " + e.toString());
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        GenericValue paymentSettings = null;
        if (orderHeader != null) {
            paymentSettings = getPaymentSettings(orderHeader, paymentPref, REFUND_SERVICE_TYPE, false);
        }

        if (paymentSettings != null) {
            String paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            String serviceName = paymentSettings.getString("paymentService");
            if (serviceName != null) {
                Map serviceContext = new HashMap();
                serviceContext.put("orderPaymentPreference", paymentPref);
                serviceContext.put("paymentConfig", paymentConfig);
                serviceContext.put("currency", orh.getCurrency());

                // get the creditCard/address/email
                String payToPartyId = null;
                try {
                    payToPartyId = getBillingInformation(orh, paymentPref, new HashMap());
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems getting billing information", module);
                    return ServiceUtil.returnError("Problems getting billing information");
                }

                // format the price
                String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
                DecimalFormat formatter = new DecimalFormat(currencyFormat);
                String amountString = formatter.format(refundAmount);
                Double processAmount = null;
                try {
                    processAmount = new Double(formatter.parse(amountString).doubleValue());
                } catch (ParseException e) {
                    Debug.logError(e, "Problem parsing amount using DecimalFormat", module);
                    return ServiceUtil.returnError("Refund processor problems; see logs");
                }
                serviceContext.put("refundAmount", processAmount);
                serviceContext.put("userLogin", userLogin);

                // call the service
                Map refundResponse = null;
                try {
                    refundResponse = dispatcher.runSync(serviceName, serviceContext, TX_TIME, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem refunding payment through processor", module);
                    return ServiceUtil.returnError("Refund processor problems; see logs");
                }
                if (ServiceUtil.isError(refundResponse)) {
                    saveError(dispatcher, userLogin, paymentPref, refundResponse, REFUND_SERVICE_TYPE, "PGT_REFUND");
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(refundResponse));
                }

                //Debug.log("Called Electronic Refund Service : " + refundResponse, module);

                // get the pay-from party
                if (paymentConfig == null || paymentConfig.length() == 0) {
                    paymentConfig = "payment.properties";
                }
                String payFromPartyId = getPayToPartyId(orderHeader);

                // create the PaymentGatewayResponse record
                String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
                GenericValue response = delegator.makeValue("PaymentGatewayResponse", null);
                response.set("paymentGatewayResponseId", responseId);
                response.set("paymentServiceTypeEnumId", REFUND_SERVICE_TYPE);
                response.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
                response.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
                response.set("paymentMethodId", paymentPref.get("paymentMethodId"));
                response.set("transCodeEnumId", "PGT_REFUND");

                // set the capture info
                response.set("amount", refundResponse.get("refundAmount"));
                response.set("referenceNum", refundResponse.get("refundRefNum"));
                response.set("altReference", refundResponse.get("refundAltRefNum"));
                response.set("gatewayCode", refundResponse.get("refundCode"));
                response.set("gatewayFlag", refundResponse.get("refundFlag"));
                response.set("gatewayMessage", refundResponse.get("refundMessage"));
                response.set("transactionDate", UtilDateTime.nowTimestamp());
                try {
                    delegator.create(response);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to create PaymentGatewayResponse record");
                }

                // create the internal messages
                List messages = (List) refundResponse.get("internalRespMsgs");
                if (messages != null && messages.size() > 0) {
                    Iterator i = messages.iterator();
                    while (i.hasNext()) {
                        GenericValue respMsg = delegator.makeValue("PaymentGatewayRespMsg", null);
                        String respMsgId = delegator.getNextSeqId("PaymentGatewayRespMsg");
                        String message = (String) i.next();
                        respMsg.set("paymentGatewayRespMsgId", respMsgId);
                        respMsg.set("paymentGatewayResponseId", responseId);
                        respMsg.set("pgrMessage", message);
                        try {
                            delegator.create(respMsg);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError("Unable to create PaymentGatewayRespMsg record");
                        }
                    }
                }

                // handle the (reverse) payment
                Boolean refundResult = (Boolean) refundResponse.get("refundResult");
                if (refundResult != null && refundResult.booleanValue()) {
                    // create a payment record
                    Map paymentCtx = UtilMisc.toMap("paymentTypeId", "CUSTOMER_REFUND");
                    paymentCtx.put("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
                    paymentCtx.put("paymentMethodId", paymentPref.get("paymentMethodId"));
                    paymentCtx.put("paymentGatewayResponseId", responseId);
                    paymentCtx.put("partyIdTo", payToPartyId);
                    paymentCtx.put("partyIdFrom", payFromPartyId);
                    paymentCtx.put("statusId", "PMNT_SENT");
                    paymentCtx.put("paymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
                    paymentCtx.put("currencyUomId", orh.getCurrency());   
                    paymentCtx.put("amount", refundResponse.get("refundAmount"));
                    paymentCtx.put("userLogin", userLogin);
                    paymentCtx.put("paymentRefNum", refundResponse.get("refundRefNum"));
                    paymentCtx.put("comments", "Refund");

                    String paymentId = null;
                    try {
                        Map payRes = dispatcher.runSync("createPayment", paymentCtx);
                        if (ModelService.RESPOND_ERROR.equals(payRes.get(ModelService.RESPONSE_MESSAGE))) {
                            return ServiceUtil.returnError((String) payRes.get(ModelService.ERROR_MESSAGE));
                        } else {
                            paymentId = (String) payRes.get("paymentId");
                        }
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem creating Payment", module);
                        return ServiceUtil.returnError("Problem creating Payment");
                    }
                    //Debug.log("Payment created : " + paymentId, module);

                    if (paymentId == null) {
                        return ServiceUtil.returnError("Create payment failed");
                    }

                    Map result = ServiceUtil.returnSuccess();
                    result.put("paymentId", paymentId);
                    return result;
                } else {
                    return ServiceUtil.returnFailure("The refund failed");
                }
            } else {
                return ServiceUtil.returnError("No refund service defined");
            }
        } else {
            return ServiceUtil.returnError("No payment settings found");
        }
    }


    public static Map retryFailedOrderAuth(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }

        // make sure we have a valid order record
        if (orderHeader == null || orderHeader.get("statusId") == null) {
            return ServiceUtil.returnError("Invalid OrderHeader record for ID: " + orderId);
        }

        // check the current order status
        if (!"ORDER_CREATED".equals(orderHeader.getString("statusId"))) {
            // if we are out of the created status; then we were either cancelled, rejected or approved
            Debug.logWarning("Was re-trying a failed auth for orderId [" + orderId + "] but it is not in the ORDER_CREATED status, so skipping.", module);
            return ServiceUtil.returnSuccess();
        }

        // run the auth service and check for failure(s)
        Map serviceResult = null;
        try {
            serviceResult = dispatcher.runSync("authOrderPayments", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
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

        Map result = ServiceUtil.returnSuccess();
        result.put("processResult", authResp);

        return result;
    }


    public static Map retryFailedAuths(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get a list of all payment prefs still pending
        List exprs = UtilMisc.toList(new EntityExpr("statusId", EntityOperator.EQUALS, "PAYMENT_NOT_AUTH"),
                new EntityExpr("processAttempt", EntityOperator.GREATER_THAN, new Long(0)));

        EntityListIterator eli = null;
        try {
            eli = delegator.findListIteratorByCondition("OrderPaymentPreference",
                    new EntityConditionList(exprs, EntityOperator.AND), null, UtilMisc.toList("orderId"));
            List processList = new ArrayList();
            if (eli != null) {
                Debug.logInfo("Processing failed order re-auth(s)", module);
                GenericValue value = null;
                while (((value = (GenericValue) eli.next()) != null)) {
                    String orderId = value.getString("orderId");
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync("retryFailedOrderAuth", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
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
    
    public static Map retryFailedAuthNsfs(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        
        // get the date/time for one week before now since we'll only retry once a week for NSFs
        Calendar calcCal = Calendar.getInstance();
        calcCal.setTimeInMillis(System.currentTimeMillis());
        calcCal.add(Calendar.WEEK_OF_YEAR, -1);
        Timestamp oneWeekAgo = new Timestamp(calcCal.getTimeInMillis());

        EntityListIterator eli = null;
        try {
            eli = delegator.findListIteratorByCondition("OrderPaymentPreference",
                    new EntityExpr(new EntityExpr("needsNsfRetry", EntityOperator.EQUALS, "Y"), EntityOperator.AND, new EntityExpr(ModelEntity.STAMP_FIELD, EntityOperator.LESS_THAN_EQUAL_TO, oneWeekAgo)), 
                    null, UtilMisc.toList("orderId"));

            List processList = new ArrayList();
            if (eli != null) {
                Debug.logInfo("Processing failed order re-auth(s)", module);
                GenericValue value = null;
                while (((value = (GenericValue) eli.next()) != null)) {
                    String orderId = value.getString("orderId");
                    if (!processList.contains(orderId)) { // just try each order once
                        try {
                            // each re-try is independent of each other; if one fails it should not effect the others
                            dispatcher.runAsync("retryFailedOrderAuthNsf", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
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
            List order = UtilMisc.toList("-transactionDate");
            List transactions = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, order);

            List exprs = UtilMisc.toList(new EntityExpr("paymentServiceTypeEnumId", EntityOperator.EQUALS, CAPTURE_SERVICE_TYPE));

            List capTransactions = EntityUtil.filterByAnd(transactions, exprs);

            capTrans = EntityUtil.getFirst(capTransactions);
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Problem getting capture information from PaymentGatewayResponse", module);
        }
        return capTrans;
    }

    /**
     * Gets the chronologically latest PaymentGatewayResponse from an OrderPaymentPreference which is either a PRDS_PAY_AUTH
     * or PRDS_PAY_REAUTH.  Used for capturing.  
     * @param orderPaymentPreference
     * @return
     */
    public static GenericValue getAuthTransaction(GenericValue orderPaymentPreference) {
        GenericValue authTrans = null;
        try {
            List order = UtilMisc.toList("-transactionDate");
            List transactions = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, order);

            List exprs = UtilMisc.toList(new EntityExpr("paymentServiceTypeEnumId", EntityOperator.EQUALS, AUTH_SERVICE_TYPE),
                    new EntityExpr("paymentServiceTypeEnumId", EntityOperator.EQUALS, REAUTH_SERVICE_TYPE));

            List authTransactions = EntityUtil.filterByOr(transactions, exprs);

            authTrans = EntityUtil.getFirst(authTransactions);
        } catch (GenericEntityException e) {
            Debug.logError(e, "ERROR: Problem getting authorization information from PaymentGatewayResponse", module);
        }
        return authTrans;
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
        Timestamp authTime = PaymentGatewayServices.getAuthTime(orderPaymentPreference);
        if (authTime == null) {
            return false;
        }

        GenericValue paymentMethod = null;
        try {
            paymentMethod = orderPaymentPreference.getRelatedOne("PaymentMethod");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (paymentMethod != null && paymentMethod.getString("paymentMethodTypeId").equals("CREDIT_CARD")) {
            GenericValue creditCard = null;
            try {
                creditCard = paymentMethod.getRelatedOne("CreditCard");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (creditCard != null) {
                String cardType = creditCard.getString("cardType");
                String reauthDays = null;
                // add more types as necessary -- maybe we should create seed data for credit card types??
                if ("Discover".equals(cardType)) {
                    reauthDays = UtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.disc.days", "90");
                } else if ("AmericanExpress".equals(cardType)) {
                    reauthDays = UtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.amex.days", "30");
                } else if ("MasterCard".equals(cardType)) {
                    reauthDays = UtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.mc.days", "30");
                } else if ("Visa".equals(cardType)) {
                    reauthDays = UtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.visa.days", "7");
                } else {
                    reauthDays = UtilProperties.getPropertyValue(paymentConfig, "payment.general.reauth.other.days", "7");
                }

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
        }

        return true;
    }

    // manual processing service

    public static Map processManualCcTx(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        // security check
        if (!security.hasEntityPermission("MANUAL", "_PAYMENT", userLogin)) {
            Debug.logWarning("**** Security [" + (new Date()).toString() + "]: " + userLogin.get("userLoginId") + " attempt to run manual payment transaction!", module);
            return ServiceUtil.returnError("You do not have permission for this transaction.");
        }

        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        String productStoreId = (String) context.get("productStoreId");
        String transactionType = (String) context.get("transactionType");
        String referenceCode = (String) context.get("referenceCode");
        if (referenceCode == null) {
            referenceCode = new Long(System.currentTimeMillis()).toString();
        }

        // check valid implemented types
        if (!transactionType.equals(CREDIT_SERVICE_TYPE)) {
            return ServiceUtil.returnError("This transaction type is not yet supported.");
        }

        // transaction request context
        Map requestContext = new HashMap();
        String paymentService = null;
        String paymentConfig = null;

        // get the transaction settings
        GenericValue paymentSettings = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId, transactionType, false);
        if (paymentSettings == null) {
            return ServiceUtil.returnError("No valid payment settings found for : " + productStoreId + "/" + transactionType);
        } else {
            paymentConfig = paymentSettings.getString("paymentPropertiesPath");
            paymentService = paymentSettings.getString("paymentService");
            requestContext.put("paymentConfig", paymentConfig);
        }

        // check the service name
        if (paymentService == null || paymentConfig == null) {
            return ServiceUtil.returnError("Invalid product store payment settings");
        }

        if (paymentMethodTypeId.equals("CREDIT_CARD")) {
            GenericValue creditCard = delegator.makeValue("CreditCard", null);
            creditCard.setAllFields(context, true, null, null);
            if (creditCard.get("firstNameOnCard") == null || creditCard.get("lastNameOnCard") == null || creditCard.get("cardType") == null || creditCard.get("cardNumber") == null) {
                return ServiceUtil.returnError("Credit card is missing required fields.");
            }
            String expMonth = (String) context.get("expMonth");
            String expYear = (String) context.get("expYear");
            String expDate = expMonth + "/" + expYear;
            creditCard.set("expireDate", expDate);
            requestContext.put("creditCard", creditCard);
            requestContext.put("cardSecurityCode", context.get("cardSecurityCode"));

            GenericValue billingAddress = delegator.makeValue("PostalAddress", null);
            billingAddress.setAllFields(context, true, null, null);
            if (billingAddress.get("address1") == null || billingAddress.get("city") == null || billingAddress.get("postalCode") == null) {
                return ServiceUtil.returnError("Credit card billing address is missing required fields.");
            }
            requestContext.put("billingAddress", billingAddress);

            /* This is not needed any more, using names on CC as a kludge instead of these kludge names until we get a firstName/lastName on the shipping PostalAddress
            GenericValue contactPerson = delegator.makeValue("Person", null);
            contactPerson.setAllFields(context, true, null, null);
            if (contactPerson.get("firstName") == null || contactPerson.get("lastName") == null) {
                return ServiceUtil.returnError("Contact person is missing required fields.");
            }
            requestContext.put("contactPerson", contactPerson);
            */

            GenericValue billToEmail = delegator.makeValue("ContactMech", null);
            billToEmail.set("infoString", context.get("infoString"));
            if (billToEmail.get("infoString") == null) {
                return ServiceUtil.returnError("Email address field cannot be empty.");
            }
            requestContext.put("billToEmail", billToEmail);
            requestContext.put("referenceCode", referenceCode);
            String currency = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
            requestContext.put("currency", currency);
            requestContext.put("creditAmount", context.get("amount")); // TODO fix me to work w/ other services
        } else {
            return ServiceUtil.returnError("Payment method type : " + paymentMethodTypeId + " is not yet implemented for manual transactions");
        }

        // process the transaction
        Map response = null;
        try {
            response = dispatcher.runSync(paymentService, requestContext, TX_TIME, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error calling service : " + paymentService + " / " + requestContext);
        }

        // check for errors
        if (ServiceUtil.isError(response)) {
            return ServiceUtil.returnError(ServiceUtil.makeErrorMessage(response, null, null, null, null));
        }

        // get the reference number // TODO add support for other tx types
        String refNum = (String) response.get("creditRefNum");
        String code = (String) response.get("creditCode");
        String msg = (String) response.get("creditMessage");
        Map returnResults = ServiceUtil.returnSuccess("Transaction result [" + msg + "/" + code +"] Ref#: " + refNum);
        returnResults.put("referenceNum", refNum);
        return returnResults;
    }

    // ****************************************************
    // Test Services
    // ****************************************************


    /**
     * Simple test processor; declines all orders < 100.00; approves all orders > 100.00
     */
    public static Map testProcessor(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Double processAmount = (Double) context.get("processAmount");

        if (processAmount != null && processAmount.doubleValue() >= 100.00)
            result.put("authResult", Boolean.TRUE);
        if (processAmount != null && processAmount.doubleValue() < 100.00)
            result.put("authResult", Boolean.FALSE);
            result.put("customerRespMsgs", UtilMisc.toList("Sorry this processor requires at least a $100.00 purchase."));
        if (processAmount == null)
            result.put("authResult", null);

        String refNum = UtilDateTime.nowAsString();

        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "X");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized.");
        result.put("internalRespMsgs", UtilMisc.toList("This is a test processor; no payments were captured or authorized."));
        return result;
    }


    /**
     * Simple test processor; declines all orders < 100.00; approves all orders > 100.00
     */
    public static Map testProcessorWithCapture(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Double processAmount = (Double) context.get("processAmount");

        if (processAmount != null && processAmount.doubleValue() >= 100.00)
            result.put("authResult", Boolean.TRUE);
            result.put("captureResult", Boolean.TRUE);
        if (processAmount != null && processAmount.doubleValue() < 100.00)
            result.put("authResult", Boolean.FALSE);
            result.put("captureResult", Boolean.FALSE);
            result.put("customerRespMsgs", UtilMisc.toList("Sorry this processor requires at least a $100.00 purchase."));
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
        result.put("authMessage", "This is a test processor; no payments were captured or authorized.");
        result.put("internalRespMsgs", UtilMisc.toList("This is a test processor; no payments were captured or authorized."));
        return result;
    }

    /**
     *  Test authorize - does random declines
     */
    public static Map testRandomAuthorize(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
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
        result.put("authMessage", "This is a test processor; no payments were captured or authorized.");

        return result;
    }

    /**
     * Always approve processor.
     */
    public static Map alwaysApproveProcessor(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Debug.logInfo("Test Processor Approving Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.TRUE);
        result.put("processAmount", context.get("processAmount"));
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authCode", "100");
        result.put("authFlag", "A");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized.");
        return result;
    }

    public static Map alwaysApproveWithCapture(DispatchContext dctx, Map context) {
        Map result = new HashMap();
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
        result.put("authMessage", "This is a test processor; no payments were captured or authorized.");
        return result;
    }


    /**
     * Always decline processor
     */
    public static Map alwaysDeclineProcessor(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Double processAmount = (Double) context.get("processAmount");
        Debug.logInfo("Test Processor Declining Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "D");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized");
        return result;
    }

    /**
     * Always NSF (not sufficient funds) processor
     */
    public static Map alwaysNsfProcessor(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Double processAmount = (Double) context.get("processAmount");
        Debug.logInfo("Test Processor NSF Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultNsf", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "N");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized");
        return result;
    }

    /**
     * Always fail/bad expire date processor
     */
    public static Map alwaysBadExpireProcessor(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Double processAmount = (Double) context.get("processAmount");
        Debug.logInfo("Test Processor Bad Expire Date Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultBadExpire", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "E");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized");
        return result;
    }

    /**
     * Fail/bad expire date when year is even processor
     */
    public static Map badExpireEvenProcessor(DispatchContext dctx, Map context) {
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        String expireDate = creditCard.getString("expireDate");
        String lastNumberStr = expireDate.substring(expireDate.length() - 1);
        int lastNumber = Integer.parseInt(lastNumberStr);
        
        if ((float) lastNumber / 2.0 == 0.0) {
            return alwaysBadExpireProcessor(dctx, context);
        } else {
            return alwaysApproveProcessor(dctx, context);
        }
    }

    /**
     * Always bad card number processor
     */
    public static Map alwaysBadCardNumberProcessor(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Double processAmount = (Double) context.get("processAmount");
        Debug.logInfo("Test Processor Bad Card Number Credit Card", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("authResult", Boolean.FALSE);
        result.put("resultBadCardNumber", Boolean.TRUE);
        result.put("processAmount", processAmount);
        result.put("authRefNum", refNum);
        result.put("authAltRefNum", refNum);
        result.put("authFlag", "N");
        result.put("authMessage", "This is a test processor; no payments were captured or authorized");
        return result;
    }

    /**
     * Always fail (error) processor
     */
    public static Map alwaysFailProcessor(DispatchContext dctx, Map context) {
        return ServiceUtil.returnError("Unable to communicate with bla");
    }

    public static Map testRelease(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();

        String refNum = UtilDateTime.nowAsString();

        result.put("releaseResult", Boolean.TRUE);
        result.put("releaseAmount", context.get("releaseAmount"));
        result.put("releaseRefNum", refNum);
        result.put("releaseAltRefNum", refNum);
        result.put("releaseFlag", "U");
        result.put("releaseMessage", "This is a test release; no authorizations exist");
        return result;
    }

    /**
     * Test capture service (returns true)
     */
    public static Map testCapture(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Capture Process", module);

        String refNum = UtilDateTime.nowAsString();

        result.put("captureResult", Boolean.TRUE);
        result.put("captureAmount", context.get("captureAmount"));
        result.put("captureRefNum", refNum);
        result.put("captureAltRefNum", refNum);
        result.put("captureFlag", "C");
        result.put("captureMessage", "This is a test capture; no money was transferred");
        return result;
    }

    public static Map testCaptureWithReAuth(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = (GenericValue) context.get("authTrans");
        Debug.logInfo("Test Capture with 2 minute delay failure/re-auth process", module);

        if (authTransaction == null){
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }
        Timestamp txStamp = authTransaction.getTimestamp("transactionDate");
        Timestamp nowStamp = UtilDateTime.nowTimestamp();

        Map result = ServiceUtil.returnSuccess();
        result.put("captureAmount", context.get("captureAmount"));
        result.put("captureRefNum", UtilDateTime.nowAsString());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(txStamp.getTime());
        cal.add(Calendar.MINUTE, 2);
        Timestamp twoMinAfter = new Timestamp(cal.getTimeInMillis());
        Debug.log("Re-Auth Capture Test : Tx Date - " + txStamp + " : 2 Min - " + twoMinAfter + " : Now - " + nowStamp, module);

        if (nowStamp.after(twoMinAfter)) {
            result.put("captureResult", Boolean.FALSE);
        } else {
            result.put("captureResult", Boolean.TRUE);
            result.put("captureFlag", "C");
            result.put("captureMessage", "This is a test capture; no money was transferred");
        }

        return result;
    }

    /**
     * Test refund service (returns true)
     */
    public static Map testRefund(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Refund Process", module);

        result.put("refundResult", Boolean.TRUE);
        result.put("refundAmount", context.get("refundAmount"));
        result.put("refundRefNum", UtilDateTime.nowAsString());
        result.put("refundFlag", "R");
        result.put("refundMessage", "This is a test refund; no money was transferred");
        return result;
    }

    public static Map testRefundFailure(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Debug.logInfo("Test Refund Process", module);

        result.put("refundResult", Boolean.FALSE);
        result.put("refundAmount", context.get("refundAmount"));
        result.put("refundRefNum", UtilDateTime.nowAsString());
        result.put("refundFlag", "R");
        result.put("refundMessage", "This is a test refund failure; no money was transferred");
        return result;
    }
}
