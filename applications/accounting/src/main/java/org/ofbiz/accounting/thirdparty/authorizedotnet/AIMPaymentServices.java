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

package org.ofbiz.accounting.thirdparty.authorizedotnet;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

public class AIMPaymentServices {

    public static final String module = AIMPaymentServices.class.getName();
    public final static String resource = "AccountingUiLabels";

    // The list of refund failure response codes that would cause the ccRefund service
    // to attempt to void the refund's associated authorization transaction.  This list
    // contains the responses where the voiding does not need to be done within a certain
    // time limit
    private static final List<String> VOIDABLE_RESPONSES_NO_TIME_LIMIT = UtilMisc.toList("50");

    // A list of refund failure response codes that would cause the ccRefund service
    // to first check whether the refund's associated authorization transaction has occurred
    // within a certain time limit, and if so, cause it to void the transaction
    private static final List<String> VOIDABLE_RESPONSES_TIME_LIMIT = UtilMisc.toList("54");

    // The number of days in the time limit when one can safely consider an unsettled
    // transaction to be still valid
    private static final int TIME_LIMIT_VERIFICATION_DAYS = 120;

    private static Properties AIMProperties = null;

    // A routine to check whether a given refund failure response code will cause the
    // ccRefund service to attempt to void the refund's associated authorization transaction
    private static boolean isVoidableResponse(String responseCode) {
        return
            VOIDABLE_RESPONSES_NO_TIME_LIMIT.contains(responseCode) ||
            VOIDABLE_RESPONSES_TIME_LIMIT.contains(responseCode);
    }

    public static Map<String, Object> ccAuth(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<String, Object>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put("transType", "AUTH_ONLY");
        buildAuthTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        //now we need to process the result
        processAuthTransResult(request, reply, results);
        return results;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne("CreditCard",orderPaymentPreference, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }
        context.put("creditCard", creditCard);
        context.put("authTransaction", authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<String, Object>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        request.put("x_Invoice_Num","Order " + orderPaymentPreference.getString("orderId"));
        // PRIOR_AUTH_CAPTURE is the right one to use, since we already have an authorization from the authTransaction.
        // CAPTURE_ONLY is a "force" transaction to be used if there is no prior authorization
        props.put("transType", "PRIOR_AUTH_CAPTURE");
        //props.put("transType","CAPTURE_ONLY");
        props.put("cardtype", creditCard.get("cardType"));
        buildCaptureTransaction(context,props,request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        processCaptureTransResult(request, reply, results);
        // if there is no captureRefNum, then the capture failed
        if (results.get("captureRefNum") == null) {
             return ServiceUtil.returnError((String) results.get("captureMessage"));
        }
        return results;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne("CreditCard", orderPaymentPreference, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }
        context.put("creditCard",creditCard);
        context.put("authTransaction",authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<String, Object>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put("transType", "CREDIT");
        props.put("cardtype", creditCard.get("cardType"));
        buildRefundTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        results.putAll(processRefundTransResult(request, reply));
        boolean refundResult = ((Boolean)results.get("refundResult")).booleanValue();
        String refundFlag = (String)results.get("refundFlag");
        // Since the refund failed, we are going to void the previous authorization against
        // which ccRefunds attempted to issue the refund.  This happens because Authorize.NET requires
        // that settled transactions need to be voided the same day.  unfortunately they provide no method for
        // determining what transactions can be voided and what can be refunded, so we'll have to try it with timestamps
        if (!refundResult && isVoidableResponse(refundFlag)) {
            boolean canDoVoid = false;
            if (VOIDABLE_RESPONSES_TIME_LIMIT.contains(refundFlag)) {
                // We are calculating the timestamp that is at the beginning of a time limit,
                // since we can safely assume that, within this time limit, an unsettled transaction
                // can still be considered valid
                Calendar startCalendar = UtilDateTime.toCalendar(UtilDateTime.nowTimestamp());
                startCalendar.add(Calendar.DATE, -TIME_LIMIT_VERIFICATION_DAYS);
                Timestamp startTimestamp = new java.sql.Timestamp(startCalendar.getTime().getTime());
                Timestamp authTimestamp = authTransaction.getTimestamp("transactionDate");
                if (startTimestamp.before(authTimestamp)) {
                    canDoVoid = true;
                }
            } else {
                // Since there's no time limit to check, the voiding of the transaction will go
                // through as usual
                canDoVoid = true;
            }
            if (canDoVoid) {
                Debug.logWarning("Refund was unsuccessful; will now attempt a VOID transaction.", module);
                BigDecimal authAmountObj = authTransaction.getBigDecimal("amount");
                BigDecimal refundAmountObj = (BigDecimal)context.get("refundAmount");
                BigDecimal authAmount = authAmountObj != null ? authAmountObj : BigDecimal.ZERO;
                BigDecimal refundAmount = refundAmountObj != null ? refundAmountObj : BigDecimal.ZERO;
                if (authAmount.compareTo(refundAmount) == 0) {
                    reply = voidTransaction(authTransaction, context, delegator);
                    if (ServiceUtil.isError(reply)) {
                        return reply;
                    }
                    results = ServiceUtil.returnSuccess();
                    results.putAll(processRefundTransResult(request, reply));
                    return results;
                } else {
                    // TODO: Modify the code to (a) do a void of the whole transaction, and (b)
                    // create a new auth-capture of the difference.
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "AccountingAuthorizeNetCannotPerformVoidTransaction", 
                            UtilMisc.toMap("authAmount", authAmount, "refundAmount", refundAmount), locale));
                }
            }
        }
        return results;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }
        Map<String, Object> reply = voidTransaction(authTransaction, context, delegator);
        if (ServiceUtil.isError(reply)) {
            return reply;
        }
        Map<String, Object> results = ServiceUtil.returnSuccess();
        context.put("x_Amount", ((BigDecimal) context.get("releaseAmount")).toPlainString()); // hack for releaseAmount
        results.putAll(processReleaseTransResult(context, reply));
        return results;
    }

    private static Map<String, Object> voidTransaction(GenericValue authTransaction, Map<String, Object> context, Delegator delegator) {
        Locale locale = (Locale) context.get("locale");
        context.put("authTransaction", authTransaction);
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<String, Object>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildEmailSettings(context, props, request);
        props.put("transType", "VOID");
        buildVoidTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        return processCard(request, props, locale);
    }

    public static Map<String, Object> ccCredit(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        results.put(ModelService.ERROR_MESSAGE, "Authorize.net ccCredit unsupported with version 3.1");
        return results;
    }

    public static Map<String, Object> ccAuthCapture(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> request = new HashMap<String, Object>();
        Properties props = buildAIMProperties(context, delegator);
        buildMerchantInfo(context, props, request);
        buildGatewayResponeConfig(context, props, request);
        buildCustomerBillingInfo(context, props, request);
        buildEmailSettings(context, props, request);
        buildInvoiceInfo(context, props, request);
        props.put("transType", "AUTH_CAPTURE");
        buildAuthTransaction(context, props, request);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        Map<String, Object> reply = processCard(request, props, locale);
        //now we need to process the result
        processAuthCaptureTransResult(request, reply, results);
        // if there is no captureRefNum, then the capture failed
        if (results.get("captureRefNum") == null) {
             return ServiceUtil.returnError((String) results.get("captureMessage"));
        }
        return results;
    }

    private static Map<String, Object> processCard(Map<String, Object> request, Properties props, Locale locale) {
        Map<String, Object> result = new HashMap<String, Object>();
        String url = props.getProperty("url");
        if (UtilValidate.isEmpty(url)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingAuthorizeNetTransactionUrlNotFound", locale));
        }
        if (isTestMode()) {
            Debug.logInfo("TEST Authorize.net using url [" + url + "]", module);
            Debug.logInfo("TEST Authorize.net request string " + request.toString(),module);
            Debug.logInfo("TEST Authorize.net properties string " + props.toString(),module);
        }
        
        // card present has a different layout from standard AIM; this determines how to parse the response
        int apiType = UtilValidate.isEmpty(props.get("cpMarketType")) ? AuthorizeResponse.AIM_RESPONSE : AuthorizeResponse.CP_RESPONSE;
        
        try {
            HttpClient httpClient = new HttpClient(url, request);
            String certificateAlias = props.getProperty("certificateAlias");
            httpClient.setClientCertificateAlias(certificateAlias);
            String httpResponse = httpClient.post();
            Debug.logInfo("transaction response: " + httpResponse,module);
            AuthorizeResponse ar = new AuthorizeResponse(httpResponse, apiType);            
            if (ar.isApproved()) {            
                result.put("authResult", Boolean.TRUE);
            } else {
                result.put("authResult", Boolean.FALSE);
                if (Debug.infoOn()) {
                    Debug.logInfo("transactionId:  " + ar.getTransactionId(), module);
                    Debug.logInfo("responseCode:   " + ar.getResponseCode(), module);
                    Debug.logInfo("responseReason: " + ar.getReasonCode(), module);
                    Debug.logInfo("reasonText:     " + ar.getReasonText(), module);
                }
            }
            result.put("httpResponse", httpResponse);
            result.put("authorizeResponse", ar);
        } catch (HttpClientException e) {
            Debug.logInfo(e, "Could not complete Authorize.Net transaction: " + e.toString(),module);
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    private static boolean isTestMode() {
        return "true".equalsIgnoreCase((String)AIMProperties.get("testReq"));
    }

    private static Properties buildAIMProperties(Map<String, Object> context, Delegator delegator) {
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        String configStr = (String) context.get("paymentConfig");
        if (configStr == null) {
            configStr = "payment.properties";
        }
        GenericValue cc = (GenericValue)context.get("creditCard");
        String url = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "transactionUrl", configStr, "payment.authorizedotnet.url");
        String certificateAlias = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "certificateAlias", configStr, "payment.authorizedotnet.certificateAlias");
        String ver = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "apiVersion", configStr, "payment.authorizedotnet.version");
        String delimited = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "delimitedData", configStr, "payment.authorizedotnet.delimited");
        String delimiter = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "delimiterChar", configStr, "payment.authorizedotnet.delimiter");
        String cpVersion = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "cpVersion", configStr, "payment.authorizedotnet.cpVersion");
        String cpMarketType = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "cpMarketType", configStr, "payment.authorizedotnet.cpMarketType");
        String cpDeviceType = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "cpDeviceType", configStr, "payment.authorizedotnet.cpDeviceType");
        String method = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "method", configStr, "payment.authorizedotnet.method");
        String emailCustomer = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "emailCustomer", configStr, "payment.authorizedotnet.emailcustomer");
        String emailMerchant = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "emailMerchant", configStr, "payment.authorizedotnet.emailmerchant");
        String testReq = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "testMode", configStr, "payment.authorizedotnet.test");
        String relay = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "relayResponse", configStr, "payment.authorizedotnet.relay");
        String tranKey = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "tranKey", configStr, "payment.authorizedotnet.trankey");
        String login = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "userId", configStr, "payment.authorizedotnet.login");
        String password = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "pwd", configStr, "payment.authorizedotnet.password");
        String transDescription = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "transDescription", configStr, "payment.authorizedotnet.transdescription");
        String duplicateWindow = getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "duplicateWindow", configStr, "payment.authorizedotnet.duplicateWindow");        
        if (UtilValidate.isEmpty(ver)) {
            ver = "3.0";
        }
        if (UtilValidate.isEmpty(login)) {
            Debug.logInfo("the login property in " + configStr + " is not configured.", module);
        }
        if (UtilValidate.isEmpty(password) && !("3.1".equals(ver))) {
            Debug.logInfo("The password property in " + configStr + " is not configured.", module);
        }
        if ("3.1".equals(ver)) {
            if (UtilValidate.isEmpty(tranKey)) {
                Debug.logInfo("Trankey property required for version 3.1 reverting to 3.0",module);
                ver = "3.0";
            }
        }
        if (UtilValidate.isNotEmpty(cpMarketType) && UtilValidate.isEmpty(cpVersion)) {
            cpVersion = "1.0";
        }
        
        Properties props = new Properties();
        props.put("url", url);
        props.put("certificateAlias", certificateAlias);
        props.put("ver", ver);
        props.put("delimited", delimited);
        props.put("delimiter", delimiter);
        props.put("method", method);
        props.put("cpVersion", cpVersion);
        props.put("cpMarketType", cpMarketType);
        props.put("cpDeviceType", cpDeviceType);
        props.put("emailCustomer", emailCustomer);
        props.put("emailMerchant", emailMerchant);
        props.put("testReq", testReq);
        props.put("relay", relay);
        props.put("transDescription", transDescription);
        props.put("login", login);
        props.put("password", password);
        props.put("trankey", tranKey);
        props.put("duplicateWindow", duplicateWindow);
        if (cc != null) {
            props.put("cardtype", cc.get("cardType"));
        }
        if (AIMProperties == null) {
            AIMProperties = props;
        }
        if (isTestMode()) {
            Debug.logInfo("Created Authorize.Net properties file: " + props.toString(), module);
        }
        return props;
    }

    private static void buildMerchantInfo(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        AIMRequest.put("x_Login", props.getProperty("login"));
        String trankey = props.getProperty("trankey");
        if (UtilValidate.isNotEmpty(trankey)) {
            AIMRequest.put("x_Tran_Key", props.getProperty("trankey"));
        } else {
            // only send password if no tran key
            AIMRequest.put("x_Password",props.getProperty("password"));
        }
        
        // api version (non Card Present)
        String apiVersion = props.getProperty("ver");
        if (UtilValidate.isNotEmpty(apiVersion)) {
            AIMRequest.put("x_Version", props.getProperty("ver"));
        }
        
        // CP version
        String cpVersion = props.getProperty("cpver");
        if (UtilValidate.isNotEmpty(cpVersion)) {
            AIMRequest.put("x_cpversion", cpVersion);
        }

        // Check duplicateWindow time frame. If same transaction happens in the predefined time frame then return error. 
        String duplicateWindow = props.getProperty("duplicateWindow");
        if (UtilValidate.isNotEmpty(duplicateWindow)) {
            AIMRequest.put("x_duplicate_window", props.getProperty("duplicateWindow"));
        }
        
        // CP market type
        String cpMarketType = props.getProperty("cpMarketType");
        if (UtilValidate.isNotEmpty(cpMarketType)) {
            AIMRequest.put("x_market_type", cpMarketType);
            
            // CP test mode
            if ("true".equalsIgnoreCase(props.getProperty("testReq"))) {
                AIMRequest.put("x_test_request", props.getProperty("testReq"));                
            }
        }
        
        // CP device typ
        String cpDeviceType = props.getProperty("cpDeviceType");
        if (UtilValidate.isNotEmpty(cpDeviceType)) {
            AIMRequest.put("x_device_type", cpDeviceType);
        }                      
    }

    private static void buildGatewayResponeConfig(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        if (AIMRequest.get("x_market_type") != null) {
            // card present transaction
            AIMRequest.put("x_response_format", "true".equalsIgnoreCase(props.getProperty("delimited")) ? "1" : "0");            
        } else {
            AIMRequest.put("x_Delim_Data", props.getProperty("delimited"));            
        }
        AIMRequest.put("x_Delim_Char", props.getProperty("delimiter"));
    }

    private static void buildCustomerBillingInfo(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        try {
            // this would be used in the case of a capture, where one of the parameters is an OrderPaymentPreference
            if (params.get("orderPaymentPreference") != null) {
                GenericValue opp = (GenericValue) params.get("orderPaymentPreference");
                if ("CREDIT_CARD".equals(opp.getString("paymentMethodTypeId"))) {
                    // sometimes the ccAuthCapture interface is used, in which case the creditCard is passed directly
                    GenericValue creditCard = (GenericValue) params.get("creditCard");
                    if (creditCard == null || ! (opp.get("paymentMethodId").equals(creditCard.get("paymentMethodId")))) {
                        creditCard = opp.getRelatedOne("CreditCard", false);
                    }
                    AIMRequest.put("x_First_Name", UtilFormatOut.checkNull(creditCard.getString("firstNameOnCard")));
                    AIMRequest.put("x_Last_Name", UtilFormatOut.checkNull(creditCard.getString("lastNameOnCard")));
                    AIMRequest.put("x_Company", UtilFormatOut.checkNull(creditCard.getString("companyNameOnCard")));
                    if (UtilValidate.isNotEmpty(creditCard.getString("contactMechId"))) {
                        GenericValue address = creditCard.getRelatedOne("PostalAddress", false);
                        if (address != null) {
                            AIMRequest.put("x_Address", UtilFormatOut.checkNull(address.getString("address1")));
                            AIMRequest.put("x_City", UtilFormatOut.checkNull(address.getString("city")));
                            AIMRequest.put("x_State", UtilFormatOut.checkNull(address.getString("stateProvinceGeoId")));
                            AIMRequest.put("x_Zip", UtilFormatOut.checkNull(address.getString("postalCode")));
                            AIMRequest.put("x_Country", UtilFormatOut.checkNull(address.getString("countryGeoId")));
                        }
                    }                    
                } else {
                    Debug.logWarning("Payment preference " + opp + " is not a credit card", module);
                }
            } else {
                // this would be the case for an authorization
                GenericValue cp = (GenericValue)params.get("billToParty");
                GenericValue ba = (GenericValue)params.get("billingAddress");
                AIMRequest.put("x_First_Name", UtilFormatOut.checkNull(cp.getString("firstName")));
                AIMRequest.put("x_Last_Name", UtilFormatOut.checkNull(cp.getString("lastName")));
                AIMRequest.put("x_Address", UtilFormatOut.checkNull(ba.getString("address1")));
                AIMRequest.put("x_City", UtilFormatOut.checkNull(ba.getString("city")));
                AIMRequest.put("x_State", UtilFormatOut.checkNull(ba.getString("stateProvinceGeoId")));
                AIMRequest.put("x_Zip", UtilFormatOut.checkNull(ba.getString("postalCode")));
                AIMRequest.put("x_Country", UtilFormatOut.checkNull(ba.getString("countryGeoId")));
            }
            return;
        } catch (GenericEntityException ex) {
            Debug.logError("Cannot build customer information for " + params + " due to error: " + ex.getMessage(), module);
            return;
        }
    }

    private static void buildEmailSettings(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        GenericValue ea = (GenericValue)params.get("billToEmail");
        AIMRequest.put("x_Email_Customer", props.getProperty("emailCustomer"));
        AIMRequest.put("x_Email_Merchant", props.getProperty("emailMerchant"));
        if (ea != null) {
            AIMRequest.put("x_Email", UtilFormatOut.checkNull(ea.getString("infoString")));
        }
    }

    private static void buildInvoiceInfo(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        String description = UtilFormatOut.checkNull(props.getProperty("transDescription"));
        String orderId = UtilFormatOut.checkNull((String)params.get("orderId"));
        if (UtilValidate.isEmpty(orderId)) {
            GenericValue orderPaymentPreference = (GenericValue) params.get("orderPaymentPreference");
            if (UtilValidate.isNotEmpty(orderPaymentPreference)) {
                orderId = (String) orderPaymentPreference.get("orderId");
            }
        }
        AIMRequest.put("x_Invoice_Num", "Order " + orderId);
        AIMRequest.put("x_Description", description);
    }

    private static void buildAuthTransaction(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        GenericValue cc = (GenericValue) params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((BigDecimal)params.get("processAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));
        String cardSecurityCode = (String) params.get("cardSecurityCode");
        AIMRequest.put("x_Amount", amount);
        AIMRequest.put("x_Currency_Code", currency);        
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num", number);
        AIMRequest.put("x_Exp_Date", expDate);
        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            AIMRequest.put("x_card_code", cardSecurityCode);
        }
        if (AIMRequest.get("x_market_type") != null) {
            AIMRequest.put("x_card_type", getCardType(UtilFormatOut.checkNull(cc.getString("cardType"))));
        }
    }

    private static void buildCaptureTransaction(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        GenericValue at = (GenericValue) params.get("authTransaction");
        GenericValue cc = (GenericValue) params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((BigDecimal) params.get("captureAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));
        AIMRequest.put("x_Amount", amount);
        AIMRequest.put("x_Currency_Code", currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num", number);
        AIMRequest.put("x_Exp_Date", expDate);
        AIMRequest.put("x_Trans_ID", at.get("referenceNum"));
        AIMRequest.put("x_ref_trans_id", at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code", at.get("gatewayCode"));
        if (AIMRequest.get("x_market_type") != null) {
            AIMRequest.put("x_card_type", getCardType(UtilFormatOut.checkNull(cc.getString("cardType"))));
        }
    }

    private static void buildRefundTransaction(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        GenericValue at = (GenericValue) params.get("authTransaction");
        GenericValue cc = (GenericValue) params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((BigDecimal) params.get("refundAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));
        AIMRequest.put("x_Amount", amount);
        AIMRequest.put("x_Currency_Code", currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num", number);
        AIMRequest.put("x_Exp_Date", expDate);
        AIMRequest.put("x_Trans_ID", at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code", at.get("gatewayCode"));
        AIMRequest.put("x_ref_trans_id", at.get("referenceNum"));
        if (AIMRequest.get("x_market_type") != null) {
            AIMRequest.put("x_card_type", getCardType(UtilFormatOut.checkNull(cc.getString("cardType"))));
        }
        Debug.logInfo("buildCaptureTransaction. " + at.toString(), module);
    }

    private static void buildVoidTransaction(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        GenericValue at = (GenericValue) params.get("authTransaction");
        String currency = (String) params.get("currency");
        AIMRequest.put("x_Currency_Code", currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_ref_trans_id", at.get("referenceNum"));
        AIMRequest.put("x_Trans_ID", at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code", at.get("gatewayCode"));
        Debug.logInfo("buildVoidTransaction. " + at.toString(), module);
    }

    private static Map<String, Object> validateRequest(Map<String, Object> params, Properties props, Map<String, Object> AIMRequest) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    private static void processAuthTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get("authorizeResponse");
        try {
            Boolean authResult = (Boolean) reply.get("authResult");
            results.put("authResult", new Boolean(authResult.booleanValue()));
            results.put("authFlag", ar.getReasonCode());
            results.put("authMessage", ar.getReasonText());
            if (authResult.booleanValue()) { //passed
                results.put("authCode", ar.getAuthorizationCode());
                results.put("authRefNum", ar.getTransactionId());
                results.put("cvCode", ar.getCvResult());
                results.put("avsCode", ar.getAvsResult());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put("processAmount", getXAmount(request));
                } else {
                    results.put("processAmount", ar.getAmount());
                }
            } else {
                results.put("authCode", ar.getResponseCode());
                results.put("processAmount", BigDecimal.ZERO);
                results.put("authRefNum", AuthorizeResponse.ERROR);
            }
        } catch (Exception ex) {
            Debug.logError(ex, module);
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", BigDecimal.ZERO);
            results.put("authRefNum", AuthorizeResponse.ERROR);
        }
        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }

    private static void processCaptureTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get("authorizeResponse");
        try {
            Boolean captureResult = (Boolean) reply.get("authResult");
            results.put("captureResult", new Boolean(captureResult.booleanValue()));
            results.put("captureFlag", ar.getReasonCode());
            results.put("captureMessage", ar.getReasonText());
            results.put("captureRefNum", ar.getTransactionId());
            if (captureResult.booleanValue()) { //passed
                results.put("captureCode", ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put("captureAmount", getXAmount(request));
                } else {
                    results.put("captureAmount", ar.getAmount());
                }
            } else {
                results.put("captureAmount", BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, module);
            results.put("captureAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("captureRefNum: " + results.toString(),module);
    }

    private static Map<String, Object> processRefundTransResult(Map<String, Object> request, Map<String, Object> reply) {
        Map<String, Object> results = new HashMap<String, Object>();
        AuthorizeResponse ar = (AuthorizeResponse) reply.get("authorizeResponse");
        try {
            Boolean captureResult = (Boolean) reply.get("authResult");
            results.put("refundResult", new Boolean(captureResult.booleanValue()));
            results.put("refundFlag", ar.getReasonCode());
            results.put("refundMessage", ar.getReasonText());
            results.put("refundRefNum", ar.getTransactionId());
            if (captureResult.booleanValue()) { //passed
                results.put("refundCode", ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put("refundAmount", getXAmount(request));
                } else {
                    results.put("refundAmount", ar.getAmount());
                }
            } else {
                results.put("refundAmount", BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, module);
            results.put("refundAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("processRefundTransResult: " + results.toString(),module);
        return results;
    }

    private static Map<String, Object> processReleaseTransResult(Map<String, Object> request, Map<String, Object> reply) {
        Map<String, Object> results = new HashMap<String, Object>();
        AuthorizeResponse ar = (AuthorizeResponse) reply.get("authorizeResponse");
        try {
            Boolean captureResult = (Boolean) reply.get("authResult");
            results.put("releaseResult", new Boolean(captureResult.booleanValue()));
            results.put("releaseFlag", ar.getReasonCode());
            results.put("releaseMessage", ar.getReasonText());
            results.put("releaseRefNum", ar.getTransactionId());
            if (captureResult.booleanValue()) { //passed
                results.put("releaseCode", ar.getAuthorizationCode());
                if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                    results.put("releaseAmount", getXAmount(request));
                } else {
                    results.put("releaseAmount", ar.getAmount());
                }
            } else {
                results.put("releaseAmount", BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            Debug.logError(ex, module);
            results.put("releaseAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("processReleaseTransResult: " + results.toString(),module);
        return results;
    }

    private static void processAuthCaptureTransResult(Map<String, Object> request, Map<String, Object> reply, Map<String, Object> results) {
        AuthorizeResponse ar = (AuthorizeResponse) reply.get("authorizeResponse");
        try {
        Boolean authResult = (Boolean) reply.get("authResult");
        results.put("authResult", new Boolean(authResult.booleanValue()));
        results.put("authFlag", ar.getReasonCode());
        results.put("authMessage", ar.getReasonText());
        results.put("captureResult", new Boolean(authResult.booleanValue()));
        results.put("captureFlag", ar.getReasonCode());
        results.put("captureMessage", ar.getReasonText());
        results.put("captureRefNum", ar.getTransactionId());
        if (authResult.booleanValue()) { //passed
            results.put("authCode", ar.getAuthorizationCode());
            results.put("authRefNum", ar.getTransactionId());
            results.put("cvCode", ar.getCvResult());
            results.put("avsCode", ar.getAvsResult());
            if (BigDecimal.ZERO.compareTo(ar.getAmount()) == 0) {
                results.put("processAmount", getXAmount(request));
            } else {
                results.put("processAmount", ar.getAmount());
            }
        } else {
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", BigDecimal.ZERO);
            results.put("authRefNum", AuthorizeResponse.ERROR);
        }
        } catch (Exception ex) {
            Debug.logError(ex, module);
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", BigDecimal.ZERO);
            results.put("authRefNum", AuthorizeResponse.ERROR);
        }
        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId, String paymentGatewayConfigParameterName,
                                                       String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue payflowPro = EntityQuery.use(delegator).from("PaymentGatewayAuthorizeNet").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
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
    
    private static String getCardType(String cardType) {
        if (("CCT_VISA".equalsIgnoreCase(cardType))) return "V";
        if (("CCT_MASTERCARD".equalsIgnoreCase(cardType))) return "M";
        if ((("CCT_AMERICANEXPRESS".equalsIgnoreCase(cardType)) || ("CCT_AMEX".equalsIgnoreCase(cardType)))) return "A";
        if (("CCT_DISCOVER".equalsIgnoreCase(cardType))) return "D";
        if (("CCT_JCB".equalsIgnoreCase(cardType))) return "J";
        if ((("CCT_DINERSCLUB".equalsIgnoreCase(cardType)))) return "C";        
        return "";
    }
    
    private static BigDecimal getXAmount(Map<String, Object> request) {
        BigDecimal amt = BigDecimal.ZERO;
        if (request.get("x_Amount") != null) {
            try {
                BigDecimal amount = new BigDecimal((String) request.get("x_Amount"));
                amt = amount;
            } catch (NumberFormatException e) {
                Debug.logWarning(e, e.getMessage(), module);
            }
        }
        return amt;
    }
}
