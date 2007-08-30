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

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.*;


public class AIMPaymentServices {

    public static final String module = AIMPaymentServices.class.getName();

    // TODO: Reformat the comments below to fit JavaDocs specs

    // The list of refund failure response codes that would cause the ccRefund service
    // to attempt to void the refund's associated authorization transaction.  This list
    // contains the responses where the voiding does not need to be done within a certain
    // time limit
    private static final List VOIDABLE_RESPONSES_NO_TIME_LIMIT = UtilMisc.toList("50");

    // A list of refund failure response codes that would cause the ccRefund service
    // to first check whether the refund's associated authorization transaction has occurred
    // within a certain time limit, and if so, cause it to void the transaction
    private static final List VOIDABLE_RESPONSES_TIME_LIMIT = UtilMisc.toList("54");

    // The number of days in the time limit when one can safely consider an unsettled
    // transaction to be still valid
    private static int TIME_LIMIT_VERIFICATION_DAYS = 120;

    private static Properties AIMProperties = null;

    // A routine to check whether a given refund failure response code will cause the
    // ccRefund service to attempt to void the refund's associated authorization transaction
    private static boolean isVoidableResponse(String responseCode) {
        return
            VOIDABLE_RESPONSES_NO_TIME_LIMIT.contains(responseCode) ||
            VOIDABLE_RESPONSES_TIME_LIMIT.contains(responseCode);
    }

    public static Map ccAuth(DispatchContext ctx, Map context) {
        Map results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildCustomerBillingInfo(context,props,request);
        buildEmailSettings(context,props,request);
        buildInvoiceInfo(context,props,request);
        props.put("transType","AUTH_ONLY");
        buildAuthTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                return results;
            }
        }

        Map reply = processCard(request, props);

        //now we need to process the result
        processAuthTransResult(reply, results);
        return results;
    }

    public static Map ccCapture(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne("CreditCard",orderPaymentPreference);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to obtain cc information from payment preference");
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot Capture");
        }
        context.put("creditCard",creditCard);
        context.put("authTransaction",authTransaction);

        Map results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildCustomerBillingInfo(context,props,request);
        buildEmailSettings(context,props,request);
        request.put("x_Invoice_Num","Order " + orderPaymentPreference.getString("orderId"));
        // PRIOR_AUTH_CAPTURE is the right one to use, since we already have an authorization from the authTransaction.
        // CAPTURE_ONLY is a "force" transaction to be used if there is no prior authorization 
        props.put("transType","PRIOR_AUTH_CAPTURE");
        //props.put("transType","CAPTURE_ONLY");
        props.put("cardtype", (String)creditCard.get("cardType"));
        buildCaptureTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                return results;
            }
        }

        Map reply = processCard(request, props);

        processCaptureTransResult(reply,results);
        return results;
    }

    public static Map ccRefund(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        
        GenericValue creditCard = null;
        try {
            creditCard = delegator.getRelatedOne("CreditCard",orderPaymentPreference);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to obtain cc information from payment preference");
        }
        
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot Capture");
        }
        
        context.put("creditCard",creditCard);
        context.put("authTransaction",authTransaction);
        Map results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildEmailSettings(context,props,request);
        //props.put("transType","PRIOR_AUTH_CAPTURE");
        props.put("transType","CREDIT");
        props.put("cardtype", (String)creditCard.get("cardType"));
        buildRefundTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                return results;
            }
        }

        Map reply = processCard(request, props);
        results.putAll( processRefundTransResult(reply) );

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
                Double authAmountObj = authTransaction.getDouble("amount");
                Double refundAmountObj = (Double)context.get("refundAmount");

                double authAmount = authAmountObj != null? authAmountObj.doubleValue() : 0.0;
                double refundAmount = refundAmountObj != null? refundAmountObj.doubleValue() : 0.0;

                if (authAmount == refundAmount) {
                    reply = voidTransaction(authTransaction, context);
                    if (ServiceUtil.isError(reply)) return reply;
                    
                    results = ServiceUtil.returnSuccess();
                    results.putAll( processRefundTransResult(reply) );
                    return results;
                } else {
                    // TODO: Modify the code to (a) do a void of the whole transaction, and (b)
                    // create a new auth-capture of the difference.
                    return ServiceUtil.returnError("Cannot perform a VOID transaction: authAmount [" + authAmount + "] is different than refundAmount [" + refundAmount + "]");
                }
            }
        }

        return results;
    }

    public static Map ccRelease(DispatchContext ctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne("CreditCard");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to obtain cc information from payment preference [ID = " + orderPaymentPreference.getString("orderPaymentPreferenceId") + "]");
        }

        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference [ID = " + orderPaymentPreference.getString("orderPaymentPreferenceId") + "]; cannot void");
        }

        Map reply = voidTransaction(authTransaction, context);
        if (ServiceUtil.isError(reply)) return reply;

        Map results = ServiceUtil.returnSuccess();
        results.putAll( processReleaseTransResult(reply) );
        return results;
    }

    private static Map voidTransaction(GenericValue authTransaction, Map context) {
        context.put("authTransaction",authTransaction);
        Map results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildEmailSettings(context,props,request);
        props.put("transType","VOID");
        buildVoidTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                return results;
            }
        }

        return processCard(request, props);
    }

    public static Map ccCredit(DispatchContext ctx, Map context) {
        Map results = new HashMap();
        results.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        results.put(ModelService.ERROR_MESSAGE, "Authorize.net ccCredit unsupported with version 3.0");
        return results;
    }

    public static Map ccAuthCapture(DispatchContext ctx, Map context) {
        Map results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildCustomerBillingInfo(context,props,request);
        buildEmailSettings(context,props,request);
        buildInvoiceInfo(context,props,request);
        props.put("transType","AUTH_CAPTURE");
        buildAuthTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                return results;
            }
        }

        Map reply = processCard(request, props);

        //now we need to process the result
        processAuthCaptureTransResult(reply, results);
        return results;
    }

    private static HashMap processCard(Map request, Properties props) {
        HashMap result = new HashMap();

        String url = props.getProperty("url");
        if (url == null || url.length() == 0) {
            url = "https://certification.authorize.net/gateway/transact.dll"; // test url
            Debug.logWarning("No payment.authorizedotnet.url found.  Using a default of [" + url + "]", module);
        }
        if(isTestMode()) {
            Debug.logInfo("TEST Authorize.net using url [" + url + "]", module);
            Debug.logInfo("TEST Authorize.net request string " + request.toString(),module);
            Debug.logInfo("TEST Authorize.net properties string " + props.toString(),module);
        }

        try {
            HttpClient httpClient = new HttpClient(url, request);

            httpClient.setClientCertificateAlias("AUTHORIZE_NET");
            String httpResponse = httpClient.post();

            Debug.logInfo("transaction response: " + httpResponse,module);

            AuthorizeResponse ar = new AuthorizeResponse(httpResponse);
            String resp = ar.getResponseCode();

            if (resp.equals(AuthorizeResponse.APPROVED)) {
                result.put("authResult", Boolean.TRUE);
            } else {
                result.put("authResult", Boolean.FALSE);
                Debug.logInfo("responseCode:   " + ar.getResponseField(AuthorizeResponse.RESPONSE_CODE),module);
                Debug.logInfo("responseReason: " + ar.getResponseField(AuthorizeResponse.RESPONSE_REASON_CODE),module);
                Debug.logInfo("reasonText:     " + ar.getResponseField(AuthorizeResponse.RESPONSE_REASON_TEXT),module);
            }

            result.put("httpResponse", httpResponse);
            result.put("authorizeResponse", ar);

        } catch (HttpClientException e) {
            Debug.logInfo("Could not complete Authorize.Net transaction: " + e.toString(),module);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    private static boolean isTestMode() {
        boolean ret = true;
        String testReq = (String)AIMProperties.get("testReq");
        if(testReq != null) {
            if(testReq.equals("TRUE"))
                ret = true;
            else
                ret = false;
        }
        return ret;
    }

    private static String getVersion() {
        String ver = (String)AIMProperties.get("ver");
        return ver;

    }

    private static Properties buildAIMProperties(Map context) {
        String configStr = (String)context.get("paymentConfig");
        if(configStr == null) {
            configStr = "payment.properties";
        }

        GenericValue cc = (GenericValue)context.get("creditCard");

        String url = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.url");
        String ver = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.version");
        String delimited = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.delimited");
        String delimiter = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.delimiter");
        String method = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.method");
        //String transType = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.type");
        String emailCustomer = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.emailcustomer");
        String emailMerchant = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.emailmerchant");
        String testReq = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.test");
        String relay = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.relay");
        String login = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.login");
        String transDescription = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.transdescription");
        String tranKey = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.trankey");
        String password = UtilProperties.getPropertyValue(configStr, "payment.authorizedotnet.password");

        if (ver == null || ver.length() == 0) {
            ver = "3.0";
        }

        if(login == null || login.length() == 0) {
            Debug.logInfo("the login property in " + configStr + " is not configured.",module);
        }

        if(password == null || password.length() == 0) {
            Debug.logInfo("The password property in " + configStr + " is not configured.",module);
        }

        if (ver.equals("3.1")) {
            if (tranKey == null || tranKey.length() <= 0) {
                Debug.logInfo("Trankey property required for version 3.1 reverting to 3.0",module);
                ver = "3.0";
            }
        }

        Properties props = new Properties();
        props.put("url", url);
        props.put("ver", ver);
        props.put("delimited", delimited);
        props.put("delimiter", delimiter);
        props.put("method", method);
        //props.put("transType", transType);
        props.put("emailCustomer", emailCustomer);
        props.put("emailMerchant", emailMerchant);
        props.put("testReq", testReq);
        props.put("relay", relay);
        props.put("transDescription", transDescription);
        props.put("login", login);
        props.put("password", password);
        props.put("trankey", tranKey);

        if (cc != null)
            props.put("cardtype", (String)cc.get("cardType"));

        if (AIMProperties == null)
            AIMProperties = props;

        Debug.logInfo("Created Authorize.Net properties file: " + props.toString(),module);

        return props;

    }

    private static void buildMerchantInfo(Map params, Properties props, Map AIMRequest) {
        AIMRequest.put("x_Login", props.getProperty("login"));
        String trankey = props.getProperty("trankey");
        if (trankey != null && trankey.length() > 0)
            AIMRequest.put("x_Tran_Key",props.getProperty("trankey"));
        AIMRequest.put("x_Password",props.getProperty("password"));
        AIMRequest.put("x_Version", props.getProperty("ver"));
    }

    private static void buildGatewayResponeConfig(Map params, Properties props, Map AIMRequest) {
        AIMRequest.put("x_Delim_Data", props.getProperty("delimited"));
        AIMRequest.put("x_Delim_Char", props.getProperty("delimiter"));
    }

    private static void buildCustomerBillingInfo(Map params, Properties props, Map AIMRequest) {
        try {
            // this would be used in the case of a capture, where one of the parameters is an OrderPaymentPreference
            if (params.get("orderPaymentPreference") != null) {
                GenericValue opp = (GenericValue) params.get("orderPaymentPreference");
                if ("CREDIT_CARD".equals(opp.getString("paymentMethodTypeId"))) {
                    GenericValue creditCard = opp.getRelatedOne("CreditCard");
                    AIMRequest.put("x_First_Name",UtilFormatOut.checkNull(creditCard.getString("firstNameOnCard")));
                    AIMRequest.put("x_Last_Name",UtilFormatOut.checkNull(creditCard.getString("lastNameOnCard")));
                    AIMRequest.put("x_Company",UtilFormatOut.checkNull(creditCard.getString("companyNameOnCard")));
                    if (UtilValidate.isNotEmpty(creditCard.getString("contactMechId"))) {
                        GenericValue address = creditCard.getRelatedOne("PostalAddress");
                        AIMRequest.put("x_Address",UtilFormatOut.checkNull(address.getString("address1")));
                        AIMRequest.put("x_City",UtilFormatOut.checkNull(address.getString("city")));
                        AIMRequest.put("x_State",UtilFormatOut.checkNull(address.getString("stateProvinceGeoId")));
                        AIMRequest.put("x_Zip",UtilFormatOut.checkNull(address.getString("postalCode")));
                        AIMRequest.put("x_Country",UtilFormatOut.checkNull(address.getString("countryGeoId")));
                    }
                } else {
                    Debug.logWarning("Payment preference " + opp + " is not a credit card", module);
                }
            } else {
                // this would be the case for an authorization
                GenericValue cp = (GenericValue)params.get("billToParty");
                GenericValue ba = (GenericValue)params.get("billingAddress");

                AIMRequest.put("x_First_Name",UtilFormatOut.checkNull(cp.getString("firstName")));
                AIMRequest.put("x_Last_Name",UtilFormatOut.checkNull(cp.getString("lastName")));
                AIMRequest.put("x_Address",UtilFormatOut.checkNull(ba.getString("address1")));
                AIMRequest.put("x_City",UtilFormatOut.checkNull(ba.getString("city")));
                AIMRequest.put("x_State",UtilFormatOut.checkNull(ba.getString("stateProvinceGeoId")));
                AIMRequest.put("x_Zip",UtilFormatOut.checkNull(ba.getString("postalCode")));
                AIMRequest.put("x_Country",UtilFormatOut.checkNull(ba.getString("countryGeoId")));
            }
            return;
    
        } catch (GenericEntityException ex) {
            Debug.logError("Cannot build customer information for " + params + " due to error: " + ex.getMessage(), module);
            return;
        } 
    }

    private static void buildEmailSettings(Map params, Properties props, Map AIMRequest) {
        GenericValue ea = (GenericValue)params.get("billToEmail");
        AIMRequest.put("x_Email_Customer", props.getProperty("emailCustomer"));
        AIMRequest.put("x_Email_Merchant", props.getProperty("emailMerchant"));

        if (ea != null)
            AIMRequest.put("x_Email",UtilFormatOut.checkNull(ea.getString("infoString")));
    }

    private static void buildInvoiceInfo(Map params, Properties props, Map AIMRequest) {
        String description = UtilFormatOut.checkNull(props.getProperty("transDescription"));
        String orderId = UtilFormatOut.checkNull((String)params.get("orderId"));
        AIMRequest.put("x_Invoice_Num","Order " + orderId);
        AIMRequest.put("x_Description", description);
    }

    private static void buildAuthTransaction(Map params, Properties props, Map AIMRequest) {
        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("processAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));

        AIMRequest.put("x_Amount",amount);
        AIMRequest.put("x_Currency_Code",currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num",number);
        AIMRequest.put("x_Exp_Date",expDate);
    }

    private static void buildCaptureTransaction(Map params, Properties props, Map AIMRequest) {

        GenericValue at = (GenericValue)params.get("authTransaction");
        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("captureAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));

        AIMRequest.put("x_Amount",amount);
        AIMRequest.put("x_Currency_Code",currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num",number);
        AIMRequest.put("x_Exp_Date",expDate);
        AIMRequest.put("x_Trans_ID",at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code",at.get("gatewayCode"));
    }

    private static void buildRefundTransaction(Map params, Properties props, Map AIMRequest) {
        GenericValue at = (GenericValue)params.get("authTransaction");
        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("refundAmount")).toString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));

        AIMRequest.put("x_Amount",amount);
        AIMRequest.put("x_Currency_Code",currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num",number);
        AIMRequest.put("x_Exp_Date",expDate);
        AIMRequest.put("x_Trans_ID",at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code",at.get("gatewayCode"));

        Debug.logInfo("buildCaptureTransaction. " + at.toString(),module);
    }

    private static void buildVoidTransaction(Map params, Properties props, Map AIMRequest) {
        GenericValue at = (GenericValue)params.get("authTransaction");
        String currency = (String) params.get("currency");

        AIMRequest.put("x_Currency_Code",currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Trans_ID",at.get("referenceNum"));
        AIMRequest.put("x_Auth_Code",at.get("gatewayCode"));

        Debug.logInfo("buildVoidTransaction. " + at.toString(),module);
    }

    private static Map validateRequest(Map params, Properties props, Map AIMRequest) {
        Map result = new HashMap();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }


    private static void processAuthTransResult(Map reply, Map results) {
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean authResult = (Boolean)reply.get("authResult");
        results.put("authResult", new Boolean(authResult.booleanValue()));
        results.put("authFlag",ar.getReasonCode());
        results.put("authMessage",ar.getReasonText());


        if(authResult.booleanValue()) { //passed
            results.put("authCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("authRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("cvCode", ar.getResponseField(AuthorizeResponse.CID_RESPONSE_CODE));
            results.put("avsCode", ar.getResponseField(AuthorizeResponse.AVS_RESULT_CODE));
            results.put("processAmount", new Double(ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", new Double("0.00"));
            results.put("authRefNum", AuthorizeResponse.ERROR);

        }

        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }

    private static void processCaptureTransResult(Map reply, Map results) {
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean captureResult = (Boolean)reply.get("authResult");
        results.put("captureResult", new Boolean(captureResult.booleanValue()));
        results.put("captureFlag",ar.getReasonCode());
        results.put("captureMessage",ar.getReasonText());

        if(captureResult.booleanValue()) { //passed
            results.put("captureCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("captureRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("captureAmount", new Double(ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("captureAmount", new Double("0.00"));

        }

        Debug.logInfo("processCaptureTransResult: " + results.toString(),module);
    }

    private static Map processRefundTransResult(Map reply) {
        Map results = new HashMap();
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean captureResult = (Boolean)reply.get("authResult");
        results.put("refundResult", new Boolean(captureResult.booleanValue()));
        results.put("refundFlag",ar.getReasonCode());
        results.put("refundMessage",ar.getReasonText());
        results.put("refundRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));

        if(captureResult.booleanValue()) { //passed
            results.put("refundCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("refundAmount", new Double(ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("refundAmount", new Double("0.00"));
        }

        Debug.logInfo("processRefundTransResult: " + results.toString(),module);
        return results;
    }

    private static Map processReleaseTransResult(Map reply) {
        Map results = new HashMap();
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean captureResult = (Boolean)reply.get("authResult");
        results.put("releaseResult", new Boolean(captureResult.booleanValue()));
        results.put("releaseFlag",ar.getReasonCode());
        results.put("releaseMessage",ar.getReasonText());
        results.put("releaseRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));

        if(captureResult.booleanValue()) { //passed
            results.put("releaseCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("releaseAmount", new Double(ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("releaseAmount", new Double("0.00"));

        }

        Debug.logInfo("processReleaseTransResult: " + results.toString(),module);
        return results;
    }

    private static void processAuthCaptureTransResult(Map reply, Map results) {
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean authResult = (Boolean)reply.get("authResult");
        results.put("authResult", new Boolean(authResult.booleanValue()));
        results.put("authFlag",ar.getReasonCode());
        results.put("authMessage",ar.getReasonText());
        results.put("captureResult", new Boolean(authResult.booleanValue()));
        results.put("captureFlag",ar.getReasonCode());
        results.put("captureMessage",ar.getReasonText());

        if(authResult.booleanValue()) { //passed
            results.put("authCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("authRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("captureRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("cvCode", ar.getResponseField(AuthorizeResponse.CID_RESPONSE_CODE));
            results.put("avsCode", ar.getResponseField(AuthorizeResponse.AVS_RESULT_CODE));
            results.put("processAmount", new Double(ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", new Double("0.00"));
            results.put("authRefNum", AuthorizeResponse.ERROR);
        }

        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }
}
