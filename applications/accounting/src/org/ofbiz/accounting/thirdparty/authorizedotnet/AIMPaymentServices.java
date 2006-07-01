/*
 * $Id: $
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.accounting.thirdparty.authorizedotnet;

import java.util.*;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.service.*;

import org.ofbiz.accounting.payment.PaymentGatewayServices;

/**
 *
 * @author Fred Forrester (foresterf@fredforester.org)
 */
public class AIMPaymentServices {

    public static final String module = AIMPaymentServices.class.getName();

    private static Properties AIMProperties = null;

    private static void buildTestRequest(Map request, Map props) {
        Debug.logInfo("buildTestRequest.",module);

        String cardType = (String)props.get("cardtype");

        //request.put("x_Amount", "1");
        //request.put("x_Exp_Date", "0108");
        if (cardType == null || cardType.length() == 0)
            cardType = "MasterCard";
        if (cardType.equals("MasterCard"))
            request.put("x_Card_Num", "5424000000000015");
        if (cardType.equals("Visa"))
            request.put("x_Card_Num", "4007000000027");
        if (cardType.equals("American Express"))
            request.put("x_Card_Num", "370000000000002");
        if (cardType.equals("Discover"))
            request.put("x_Card_Num", "6011000000000012");
        //request.put("cardType", "VISA");
        request.put("x_Description", "Test Transaction");

    }

    public static Map ccAuth(DispatchContext ctx, Map context) {
        Debug.logInfo("--> Authorize.Net ccAuth Transaction Start <--",module);
        //Debug.logInfo("Enter ccAuth " + context,module);
        Map results = new HashMap();
        results = ServiceUtil.returnSuccess();
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
                Debug.logInfo("Missing transaction values. Aborting transaction.",module);
                Debug.logInfo("<-- Abnormal ccAuth Transaction End -->\r\n",module);
                return results;
            }
        }

        Map reply = processCard(request, props);

        //now we need to process the result
        processAuthTransResult(reply, results);
        Debug.logInfo("<-- Authorize.net ccAuth Transaction End -->",module);
        return results;
    }

    public static Map ccCapture(DispatchContext ctx, Map context) {
        Debug.logInfo("--> Authorize.Net ccCapture Transaction Start <--",module);
        GenericDelegator delegator = ctx.getDelegator();
        //Debug.logInfo("Enter ccCapture " + context,module);
        GenericValue creditCard = null;
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
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
        //Debug.logInfo("authTransaction " + authTransaction.toString(),module);
        //Debug.logInfo("creditCard " + creditCard.toString(),module);
        Map results = new HashMap();
        results = ServiceUtil.returnSuccess();
        Map request = new HashMap();

        Properties props = buildAIMProperties(context);
        buildMerchantInfo(context,props,request);
        buildGatewayResponeConfig(context,props,request);
        buildEmailSettings(context,props,request);
        //props.put("transType","PRIOR_AUTH_CAPTURE");
        props.put("transType","CAPTURE_ONLY");
        props.put("cardtype", (String)creditCard.get("cardType"));
        buildCaptureTransaction(context,props,request);

        Map validateResults = validateRequest(context,props,request);
        String respMsg = (String)validateResults.get(ModelService.RESPONSE_MESSAGE);
        if(respMsg != null) {
            if(respMsg.equals(ModelService.RESPOND_ERROR)) {
                results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
                Debug.logInfo("Missing transaction values. Aborting transaction.",module);
                Debug.logInfo("<-- Abnormal Transaction ccCapture End -->\r\n",module);
                return results;
            }
        }

        Map reply = processCard(request, props);

        processCaptureTransResult(reply,results);
        Debug.logInfo("<-- Authorize.net ccCapture Transaction End -->",module);
        return results;

    }
    public static Map ccRelease(DispatchContext ctx, Map context) {
        Map results = new HashMap();
        results.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        results.put(ModelService.ERROR_MESSAGE, "Authorize.net ccRelease unsupported with version 3.0");
        return results;
    }

    public static Map ccRefund(DispatchContext ctx, Map context) {
        Debug.logInfo("--> Authorize.Net Transaction Start <--",module);
        //Debug.logInfo("ccRefund context " + context,module);
        //Debug.logInfo("ccRefund ctx " + ctx,module);
        GenericDelegator delegator = ctx.getDelegator();
        //Debug.logInfo("Enter ccCapture " + context,module);
        GenericValue creditCard = null;
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
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
        //Debug.logInfo("authTransaction " + authTransaction.toString(),module);
        //Debug.logInfo("creditCard " + creditCard.toString(),module);
        Map results = new HashMap();
        results = ServiceUtil.returnSuccess();
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
                Debug.logInfo("Missing transaction values. Aborting transaction.",module);
                Debug.logInfo("<-- Abnormal Transaction ccCapture End -->\r\n",module);
                return results;
            }
        }

        Map reply = processCard(request, props);

        processRefundTransResult(reply,results);
        Debug.logInfo("<-- Authorize.net ccCapture Transaction End -->",module);
        return results;
    }

    public static Map ccCredit(DispatchContext ctx, Map context) {
        Debug.logInfo("--> Authorize.Net Transaction Start <--",module);
        Map results = new HashMap();
        results.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        results.put(ModelService.ERROR_MESSAGE, "Authorize.net ccCredit unsupported with version 3.0");
        return results;
    }

    public static Map ccAuthCapture(DispatchContext ctx, Map context) {
        Debug.logInfo("--> Authorize.Net ccAuthCapture Transaction Start <--",module);
        //Debug.logInfo("Enter ccAuth " + context,module);
        Map results = new HashMap();
        results = ServiceUtil.returnSuccess();
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
                Debug.logInfo("Missing transaction values. Aborting transaction.",module);
                Debug.logInfo("<-- Abnormal Transaction End -->\r\n",module);
                return results;
            }
        }

        Map reply = processCard(request, props);

        //now we need to process the result
        processAuthCaptureTransResult(reply, results);
        Debug.logInfo("<-- Authorize.net Transaction End -->",module);
        return results;
    }

    private static HashMap processCard(Map request, Properties props) {
        Debug.logInfo("processCard.",module);
        HashMap result = new HashMap();

        String url = props.getProperty("url");
        if(url == null || url.length() == 0) {
            url = "https://certification.authorize.net/gateway/transact.dll"; // test url
        }

        if(isTestMode()) {
            //buildTestRequest(request,props);
            Debug.logInfo("TEST Authorize.net",module);
            Debug.logInfo("TEST Authorize.net request string " + request.toString(),module);
            Debug.logInfo("TEST Authorize.net properties string " + props.toString(),module);
        }

        try {
            Debug.logInfo("contacting Authorize.Net",module);
            HttpClient httpClient = new HttpClient(url, request);

            httpClient.setClientCertificateAlias("AUTHORIZE_NET");
            String httpResponse = httpClient.post();

            Debug.logInfo("transaction response: " + httpResponse,module);

            AuthorizeResponse ar = new AuthorizeResponse(httpResponse);
            String resp = ar.getResponseCode();

            if (resp.equals(ar.APPROVED)) {
                //respCode = EcommerceServices.DECLINED;
                result.put("authResult", new Boolean(true));
                Debug.logInfo("--> TRANSACTION APPROVED <--",module);
            } else {
                result.put("authResult", new Boolean(false));
                Debug.logInfo("--> TRANSACTION DECLINED <--",module);
                Debug.logInfo("responseCode:   " + ar.getResponseField(AuthorizeResponse.RESPONSE_CODE),module);
                Debug.logInfo("responseReason: " + ar.getResponseField(AuthorizeResponse.RESPONSE_REASON_CODE),module);
                Debug.logInfo("reasonText:     " + ar.getResponseField(AuthorizeResponse.RESPONSE_REASON_TEXT),module);
            }

            result.put("httpResponse", httpResponse);
            result.put("authorizeResponse", ar);

        } catch (HttpClientException e) {
            Debug.logInfo("       Could not complete Authorize.Net transaction: " + e.toString(),module);
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
        //Debug.logInfo("buildAIMProperties.",module);
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

        if(testReq != null) {
            if(testReq.equals("TRUE")) {
                Debug.logInfo("This transaction is a test transaction.",module);
                url = "https://certification.authorize.net/gateway/transact.dll";
            } else {
                Debug.logInfo("This transaction is a live transaction.",module);
            }
        } else {
            Debug.logInfo("This transaction is a test transaction.",module);
            url = "https://certification.authorize.net/gateway/transact.dll";
            testReq = "TRUE";
        }
        if (ver.equals("3.1")) {
            if (tranKey == null && tranKey.length() <= 0) {
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
        //Debug.logInfo("buildMerchantInfo.",module);
        AIMRequest.put("x_Login", props.getProperty("login"));
        String trankey = props.getProperty("trankey");
        String version = getVersion();
        if (trankey != null && trankey.length() > 0)
            AIMRequest.put("x_Tran_Key",props.getProperty("trankey"));
        AIMRequest.put("x_Password",props.getProperty("password"));
        AIMRequest.put("x_Version", props.getProperty("ver"));
        return;
    }

    private static void buildGatewayResponeConfig(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildGatewayResponeConfig.",module);
        AIMRequest.put("x_Delim_Data", props.getProperty("delimited"));
        AIMRequest.put("x_Delim_Char", props.getProperty("delimiter"));
        return;
    }

    private static void buildCustomerBillingInfo(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildCustomerBillingInfo.",module);
        GenericValue cp = (GenericValue)params.get("billToParty");
        GenericValue ba = (GenericValue)params.get("billingAddress");

        // contact information
        AIMRequest.put("x_First_Name",UtilFormatOut.checkNull(cp.getString("firstName")));
        AIMRequest.put("x_Last_Name",UtilFormatOut.checkNull(cp.getString("lastName")));
        AIMRequest.put("x_Address",UtilFormatOut.checkNull(ba.getString("address1")));
        AIMRequest.put("x_City",UtilFormatOut.checkNull(ba.getString("city")));
        AIMRequest.put("x_State",UtilFormatOut.checkNull(ba.getString("stateProvinceGeoId")));
        AIMRequest.put("x_Zip",UtilFormatOut.checkNull(ba.getString("postalCode")));
        AIMRequest.put("x_Country",UtilFormatOut.checkNull(ba.getString("countryGeoId")));
        return;
    }

    private static void buildEmailSettings(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildEmailSettings.",module);
        GenericValue ea = (GenericValue)params.get("billToEmail");
        AIMRequest.put("x_Email_Customer", props.getProperty("emailCustomer"));
        AIMRequest.put("x_Email_Merchant", props.getProperty("emailMerchant"));

        if (ea != null)
            AIMRequest.put("x_Email",UtilFormatOut.checkNull(ea.getString("infoString")));
        return;
    }

    private static void buildInvoiceInfo(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildInvoiceInfo.",module);
        String description = (String) UtilFormatOut.checkNull(props.getProperty("transDescription"));
        String orderId = (String) UtilFormatOut.checkNull((String)params.get("orderId"));
        AIMRequest.put("x_Invoice_Num","scinc-" + orderId);
        AIMRequest.put("x_Description",description);
        return;
    }

    private static void buildAuthTransaction(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildAuthTransaction.",module);

        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("processAmount")).toString();
        String number = (String) UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = (String) UtilFormatOut.checkNull(cc.getString("expireDate"));

        AIMRequest.put("x_Amount",amount);
        AIMRequest.put("x_Currency_Code",currency);
        AIMRequest.put("x_Method", props.getProperty("method"));
        AIMRequest.put("x_Type", props.getProperty("transType"));
        AIMRequest.put("x_Card_Num",number);
        AIMRequest.put("x_Exp_Date",expDate);
    }

    private static void buildCaptureTransaction(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("buildCaptureTransaction.",module);

        GenericValue at = (GenericValue)params.get("authTransaction");
        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("captureAmount")).toString();
        String number = (String) UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = (String) UtilFormatOut.checkNull(cc.getString("expireDate"));
        String refNum = (String)at.get("referenceNum");
        String version = (String)props.getProperty("ver");

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
        //Debug.logInfo("buildCaptureTransaction.",module);

        GenericValue at = (GenericValue)params.get("authTransaction");
        GenericValue cc = (GenericValue)params.get("creditCard");
        String currency = (String) params.get("currency");
        String amount = ((Double)params.get("refundAmount")).toString();
        String number = (String) UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = (String) UtilFormatOut.checkNull(cc.getString("expireDate"));
        String refNum = (String)at.get("referenceNum");
        String version = (String)props.getProperty("ver");

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

    private static Map validateRequest(Map params, Properties props, Map AIMRequest) {
        //Debug.logInfo("validateRequest.",module);
        Map result = new HashMap();
        //result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        //result.put(ModelService.ERROR_MESSAGE, "Minimum required - invalid values");
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }


    private static void processAuthTransResult(Map reply, Map results) {
        //Debug.logInfo("processAuthTransResult.",module);
        //Debug.logInfo("Authorize net result." + results.toString(),module);
        //Debug.logInfo("Authorize net reply." + reply.toString(),module);
        String version = getVersion();
        //Map results = new HashMap();
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
            results.put("processAmount", new Double((String)ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", new Double("0.00"));
            results.put("authRefNum", AuthorizeResponse.ERROR);

        }


        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }

    private static void processCaptureTransResult(Map reply, Map results) {
        //Debug.logInfo("processCaptureTransResult.",module);
        //Debug.logInfo("Authorize net result." + results.toString(),module);
        //Debug.logInfo("Authorize net reply." + reply.toString(),module);
        String version = getVersion();
        //Map results = new HashMap();
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean captureResult = (Boolean)reply.get("authResult");
        results.put("captureResult", new Boolean(captureResult.booleanValue()));
        results.put("captureFlag",ar.getReasonCode());
        results.put("captureMessage",ar.getReasonText());

        if(captureResult.booleanValue()) { //passed
            results.put("captureCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("captureRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("captureAmount", new Double((String)ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("captureAmount", new Double("0.00"));

        }


        Debug.logInfo("processCaptureTransResult: " + results.toString(),module);
    }

    private static void processRefundTransResult(Map reply, Map results) {
        //Debug.logInfo("processCaptureTransResult.",module);
        //Debug.logInfo("Authorize net result." + results.toString(),module);
        //Debug.logInfo("Authorize net reply." + reply.toString(),module);
        String version = getVersion();
        //Map results = new HashMap();
        AuthorizeResponse ar = (AuthorizeResponse)reply.get("authorizeResponse");
        Boolean captureResult = (Boolean)reply.get("authResult");
        results.put("refundResult", new Boolean(captureResult.booleanValue()));
        results.put("refundFlag",ar.getReasonCode());
        results.put("refundMessage",ar.getReasonText());

        if(captureResult.booleanValue()) { //passed
            results.put("refundCode", ar.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
            results.put("refundRefNum", ar.getResponseField(AuthorizeResponse.TRANSACTION_ID));
            results.put("refundAmount", new Double((String)ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("refundAmount", new Double("0.00"));

        }

        Debug.logInfo("processRefundTransResult: " + results.toString(),module);
    }

    private static void processAuthCaptureTransResult(Map reply, Map results) {
        //Debug.logInfo("processAuthCaptureTransResult.",module);
        //Debug.logInfo("Authorize net result." + results.toString(),module);
        //Debug.logInfo("Authorize net reply." + reply.toString(),module);
        String version = getVersion();
        //Map results = new HashMap();
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
            results.put("processAmount", new Double((String)ar.getResponseField(AuthorizeResponse.AMOUNT)));
        } else {
            results.put("authCode", ar.getResponseCode());
            results.put("processAmount", new Double("0.00"));
            results.put("authRefNum", AuthorizeResponse.ERROR);
        }

        Debug.logInfo("processAuthTransResult: " + results.toString(),module);
    }
}
