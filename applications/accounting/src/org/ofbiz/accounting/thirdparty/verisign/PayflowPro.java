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
package org.ofbiz.accounting.thirdparty.verisign;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.Verisign.payment.PFProAPI;

import org.apache.commons.collections.map.LinkedMap;
import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * PayflowPro - Verisign PayFlow Pro <=> OFBiz Service Module
 */
public class PayflowPro {

    public static final String module = PayflowPro.class.getName();

    /**
     * Authorize credit card payment service. Service wrapper around PayFlow Pro API.
     * @param dctx Service Engine DispatchContext.
     * @param context Map context of parameters.
     * @return Response map, including RESPMSG, and RESULT keys.
     */
    public static Map ccProcessor(DispatchContext dctx, Map context) {
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        String orderId = (String) context.get("orderId");
        String cvv2 = (String) context.get("cardSecurityCode");
        Double processAmount = (Double) context.get("processAmount");
        GenericValue party = (GenericValue) context.get("billToParty");
        GenericValue cc = (GenericValue) context.get("creditCard");
        GenericValue ps = (GenericValue) context.get("billingAddress");
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        if (authTrans == null){
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        // set the orderId as comment1 so we can query in PF Manager
        boolean isReAuth = false;
        Map data = UtilMisc.toMap("COMMENT1", orderId);
        data.put("PONUM", orderId);
        data.put("CUSTCODE", party.getString("partyId"));

        // transaction type
        if (UtilProperties.propertyValueEqualsIgnoreCase(configString, "payment.verisign.preAuth", "Y")) {
            data.put("TRXTYPE", "A");
            // only support re-auth for auth types; sale types don't do it
            if (authTrans != null) {
                String refNum = authTrans.getString("referenceNum");
                data.put("ORIGID", refNum);
                isReAuth = true;
            }
        } else {
            data.put("TRXTYPE", "S");
        }

        // credit card tender
        data.put("TENDER", "C");

        // card security code
        if (UtilValidate.isNotEmpty(cvv2)) {
            data.put("CVV2", cvv2);
        }

        // set the amount
        data.put("AMT", processAmount.toString());

        // get the payment information
        data.put("ACCT", cc.getString("cardNumber"));

        // name on card
        String name = cc.getString("firstNameOnCard") + " " + cc.getString("lastNameOnCard");
        data.put("FIRSTNAME", cc.getString("firstNameOnCard"));
        data.put("LASTNAME", cc.getString("lastNameOnCard"));
        data.put("COMMENT2", name);
        if (cc.get("expireDate") != null) {
            String exp = cc.getString("expireDate");
            String expDate = exp.substring(0, 2);

            expDate = expDate + exp.substring(exp.length() - 2);
            data.put("EXPDATE", expDate);
        }

        // gather the address info
        if (ps != null) {
            String street = ps.getString("address1") +
                (ps.get("address2") != null && ps.getString("address2").length() > 0 ? " " +
                    ps.getString("address2") : "");

            data.put("STREET", street);
            data.put("ZIP", ps.getString("postalCode"));
        }

        PFProAPI pn = init(configString);

        // get the base params
        StringBuffer params = makeBaseParams(configString);

        // parse the context parameters
        params.append("&" + parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp = pn.SubmitTransaction(params.toString());

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // reset for next use
        pn.DestroyContext();

        // check the response
        Map result = ServiceUtil.returnSuccess();
        parseAuthResponse(resp, result, configString, isReAuth);
        result.put("processAmount", processAmount);
        return result;
    }

    public static Map ccCapture(DispatchContext dctx, Map context) {
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        Double amount = (Double) context.get("captureAmount");
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        if (authTrans == null){
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        if (authTrans == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }

        // auth ref number
        String refNum = authTrans.getString("referenceNum");
        Map data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Delayed Capture)
        data.put("TRXTYPE", "D");

        // credit card tender
        data.put("TENDER", "C");

        // get the orderID
        String orderId = paymentPref.getString("orderId");
        data.put("COMMENT1", orderId);

        // amount to capture
        data.put("AMT", amount.toString());

        PFProAPI pn = init(configString);

        // get the base params
        StringBuffer params = makeBaseParams(configString);

        // parse the context parameters
        params.append("&" + parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp = pn.SubmitTransaction(params.toString());

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // reset for next use
        pn.DestroyContext();

        // check the response
        Map result = ServiceUtil.returnSuccess();
        parseCaptureResponse(resp, result, configString);
        result.put("captureAmount", amount);
        return result;
    }

    public static Map ccVoid(DispatchContext dctx, Map context) {
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        Double amount = (Double) context.get("releaseAmount");
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        if (authTrans == null){
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        if (authTrans == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }

        // auth ref number
        String refNum = authTrans.getString("referenceNum");
        Map data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Void)
        data.put("TRXTYPE", "V");

        // credit card tender
        data.put("TENDER", "C");

        // get the orderID
        String orderId = paymentPref.getString("orderId");
        data.put("COMMENT1", orderId);

        // amount to capture
        data.put("AMT", amount.toString());

        PFProAPI pn = init(configString);

        // get the base params
        StringBuffer params = makeBaseParams(configString);

        // parse the context parameters
        params.append("&" + parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp = pn.SubmitTransaction(params.toString());

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // reset for next use
        pn.DestroyContext();

        // check the response
        Map result = ServiceUtil.returnSuccess();
        parseVoidResponse(resp, result, configString);
        result.put("releaseAmount", amount);
        return result;
    }

    public static Map ccRefund(DispatchContext dctx, Map context) {
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        Double amount = (Double) context.get("refundAmount");
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        GenericValue captureTrans = PaymentGatewayServices.getCaptureTransaction(paymentPref);


        if (captureTrans == null) {
            return ServiceUtil.returnError("No capture transaction found for the OrderPaymentPreference; cannot refund");
        }

        // auth ref number
        String refNum = captureTrans.getString("referenceNum");
        Map data = UtilMisc.toMap("ORIGID", refNum);

        // tx type (Credit)
        data.put("TRXTYPE", "C");

        // credit card tender
        data.put("TENDER", "C");

        // get the orderID
        String orderId = paymentPref.getString("orderId");
        data.put("COMMENT1", orderId);

        // amount to capture
        data.put("AMT", amount.toString());

        PFProAPI pn = init(configString);

        // get the base params
        StringBuffer params = makeBaseParams(configString);

        // parse the context parameters
        params.append("&" + parseContext(data));

        // transmit the request
        if (Debug.verboseOn()) Debug.logVerbose("Sending to Verisign: " + params.toString(), module);
        String resp = pn.SubmitTransaction(params.toString());

        if (Debug.verboseOn()) Debug.logVerbose("Response from Verisign: " + resp, module);

        // reset for next use
        pn.DestroyContext();

        // check the response
        Map result = ServiceUtil.returnSuccess();
        parseRefundResponse(resp, result, configString);
        result.put("refundAmount", amount);
        return result;
    }

    private static void parseAuthResponse(String resp, Map result, String resource, boolean isReAuth) {
        Debug.logInfo("Verisign response string: " + resp, module);
        Map parameters = new LinkedMap();
        List params = StringUtil.split(resp, "&");
        Iterator i = params.iterator();

        while (i.hasNext()) {
            String str = (String) i.next();

            if (str.length() > 0) {
                List kv = StringUtil.split(str, "=");
                Object k = kv.get(0);
                Object v = kv.get(1);

                if (k != null && v != null)
                    parameters.put(k, v);
            }
        }

        // txType
        boolean isSale = !UtilProperties.propertyValueEqualsIgnoreCase(resource, "payment.verisign.preAuth", "Y");

        // avs checking - ignore on re-auth
        boolean avsCheckOkay = true;
        String avsCode = null;
        if (!isReAuth) {
            boolean checkAvs = UtilProperties.propertyValueEqualsIgnoreCase(resource, "payment.verisign.checkAvs", "Y");
            if (checkAvs && !isSale) {
                String addAvs = (String) parameters.get("AVSADDR");
                String zipAvs = (String) parameters.get("AVSZIP");
                avsCode = addAvs + zipAvs;
                if ("N".equals(addAvs) || "N".equals(zipAvs)) {
                    avsCheckOkay = false;
                }
            }
        }

        // cvv2 checking - ignore on re-auth
        boolean cvv2CheckOkay = true;
        String cvvCode = null;
        if (!isReAuth) {
            boolean checkCvv2 = UtilProperties.propertyValueEqualsIgnoreCase(resource, "payment.verisign.checkAvs", "Y");
            if (checkCvv2 && !isSale) {
                cvvCode = (String) parameters.get("CVV2MATCH");
                if ("N".equals(cvvCode)) {
                    cvv2CheckOkay = false;
                }
            }
        }

        String respCode = (String) parameters.get("RESULT");
        if ("0".equals(respCode) && avsCheckOkay && cvv2CheckOkay) {
            result.put("authResult", Boolean.TRUE);
            result.put("authCode", parameters.get("AUTHCODE"));
        } else {
            Debug.logWarning("In PayflowPro failing authorization; respCode/RESULT=" + respCode + ", avsCheckOkay=" + avsCheckOkay + ", cvv2CheckOkay=" + cvv2CheckOkay + "; AUTHCODE=" + parameters.get("AUTHCODE"), module);
            result.put("authResult", Boolean.FALSE);
            
            // now check certain special conditions and report back through the generic params
            if ("12".equals(respCode)) {
                result.put("resultDeclined", Boolean.TRUE);
            } else if ("50".equals(respCode)) {
                result.put("resultNsf", Boolean.TRUE);
            } else if ("23".equals(respCode)) {
                result.put("resultBadCardNumber", Boolean.TRUE);
            } else if ("24".equals(respCode)) {
                result.put("resultBadExpire", Boolean.TRUE);
            }
        }
        result.put("cvCode", cvvCode);
        result.put("avsCode", avsCode);
        result.put("authRefNum", parameters.get("PNREF"));
        result.put("authFlag", parameters.get("RESULT"));
        result.put("authMessage", parameters.get("RESPMSG"));
    }

    private static void parseCaptureResponse(String resp, Map result, String resource) {
        Map parameters = new LinkedMap();
        List params = StringUtil.split(resp, "&");
        Iterator i = params.iterator();

        while (i.hasNext()) {
            String str = (String) i.next();

            if (str.length() > 0) {
                List kv = StringUtil.split(str, "=");
                Object k = kv.get(0);
                Object v = kv.get(1);

                if (k != null && v != null)
                    parameters.put(k, v);
            }
        }
        String respCode = (String) parameters.get("RESULT");

        if (respCode.equals("0")) {
            result.put("captureResult", Boolean.TRUE);
            result.put("captureCode", parameters.get("AUTHCODE"));
        } else {
            result.put("captureResult", Boolean.FALSE);
        }
        result.put("captureRefNum", parameters.get("PNREF"));
        result.put("captureFlag", parameters.get("RESULT"));
        result.put("captureMessage", parameters.get("RESPMSG"));
    }

    private static void parseVoidResponse(String resp, Map result, String resource) {
        Map parameters = new LinkedMap();
        List params = StringUtil.split(resp, "&");
        Iterator i = params.iterator();

        while (i.hasNext()) {
            String str = (String) i.next();

            if (str.length() > 0) {
                List kv = StringUtil.split(str, "=");
                Object k = kv.get(0);
                Object v = kv.get(1);

                if (k != null && v != null)
                    parameters.put(k, v);
            }
        }
        String respCode = (String) parameters.get("RESULT");

        if (respCode.equals("0")) {
            result.put("releaseResult", Boolean.TRUE);
            result.put("releaseCode", parameters.get("AUTHCODE"));
        } else {
            result.put("releaseResult", Boolean.FALSE);
        }
        result.put("releaseRefNum", parameters.get("PNREF"));
        result.put("releaseFlag", parameters.get("RESULT"));
        result.put("releaseMessage", parameters.get("RESPMSG"));
    }

    private static void parseRefundResponse(String resp, Map result, String resource) {
        Map parameters = new LinkedMap();
        List params = StringUtil.split(resp, "&");
        Iterator i = params.iterator();

        while (i.hasNext()) {
            String str = (String) i.next();

            if (str.length() > 0) {
                List kv = StringUtil.split(str, "=");
                Object k = kv.get(0);
                Object v = kv.get(1);

                if (k != null && v != null)
                    parameters.put(k, v);
            }
        }
        String respCode = (String) parameters.get("RESULT");

        if (respCode.equals("0")) {
            result.put("refundResult", Boolean.TRUE);
            result.put("refundCode", parameters.get("AUTHCODE"));
        } else {
            result.put("refundResult", Boolean.FALSE);
        }
        result.put("refundRefNum", parameters.get("PNREF"));
        result.put("refundFlag", parameters.get("RESULT"));
        result.put("refundMessage", parameters.get("RESPMSG"));
    }

    private static String parseContext(Map context) {
        StringBuffer buf = new StringBuffer();
        Set keySet = context.keySet();
        Iterator i = keySet.iterator();

        while (i.hasNext()) {
            Object name = i.next();
            Object value = context.get(name);

            if (value != null && (value instanceof String) && ((String) value).length() == 0) continue;
            buf.append(name + "=");
            buf.append(value);
            if (i.hasNext())
                buf.append("&");
        }
        return buf.toString();
    }

    private static StringBuffer makeBaseParams(String resource) {
        StringBuffer buf = new StringBuffer();

        try {
            buf.append("PARTNER=");
            buf.append(UtilProperties.getPropertyValue(resource, "payment.verisign.partner", "VeriSign"));
            buf.append("&");
            buf.append("VENDOR=");
            buf.append(UtilProperties.getPropertyValue(resource, "payment.verisign.vendor", "nobody"));
            buf.append("&");
            buf.append("USER=");
            buf.append(UtilProperties.getPropertyValue(resource, "payment.verisign.user", "nobody"));
            buf.append("&");
            buf.append("PWD=");
            buf.append(UtilProperties.getPropertyValue(resource, "payment.verisign.pwd", "password"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return null;
        }
        return buf;
    }

    private static PFProAPI init(String resource) {
        String certsPath = UtilProperties.getPropertyValue(resource, "payment.verisign.certsPath", "pfcerts");
        String hostAddress = UtilProperties.getPropertyValue(resource, "payment.verisign.hostAddress", "test-payflow.verisign.com");
        Integer hostPort = Integer.decode(UtilProperties.getPropertyValue(resource, "payment.verisign.hostPort", "443"));
        Integer timeout = Integer.decode(UtilProperties.getPropertyValue(resource, "payment.verisign.timeout", "80"));
        String proxyAddress = UtilProperties.getPropertyValue(resource, "payment.verisign.proxyAddress", "");
        Integer proxyPort = Integer.decode(UtilProperties.getPropertyValue(resource, "payment.verisign.proxyPort", "80"));
        String proxyLogon = UtilProperties.getPropertyValue(resource, "payment.verisign.proxyLogon", "");
        String proxyPassword = UtilProperties.getPropertyValue(resource, "payment.verisign.proxyPassword", "");

        PFProAPI pn = new PFProAPI();

        // Set the certificate path
        pn.SetCertPath(certsPath);
        // Call the client.
        pn.CreateContext(hostAddress, hostPort.intValue(), timeout.intValue(), proxyAddress, proxyPort.intValue(), proxyLogon, proxyPassword);
        return pn;
    }

/*
 * RESULT values (and RESPMSG text)
 * 
0 Approved
1 User authentication failed. Error is caused by one or more of the following: 
Login information is incorrect. Verify that USER, VENDOR, PARTNER, and 
PASSWORD have been entered correctly. See Table 3.2 on page 26 for additional 
information and field descriptions. 
Invalid Processor information entered. Contact merchant bank to verify. 
"Allowed IP Address" security feature implemented. The transaction is coming 
from an unknown IP address. See VeriSign Manager online help for details on how 
to use Manager to update the allowed IP addresses. 
You are using a test (not active) account to submit a transaction to the live VeriSign 
servers. Change the URL from test-payflow.verisign.com to payflow.verisign.com. 
2 Invalid tender type. Your merchant bank account does not support the following 
credit card type that was submitted. 
3 Invalid transaction type. Transaction type is not appropriate for this transaction. For 
example, you cannot credit an authorization-only transaction. 
4 Invalid amount format. Use the format: “#####.##”  Do not include currency 
symbols or commas. 
5 Invalid merchant information. Processor does not recognize your merchant account 
information. Contact your bank account acquirer to resolve this problem. 
6 Invalid or unsupported currency code 
7 Field format error. Invalid information entered. See RESPMSG. 
8 Not a transaction server 
9 Too many parameters or invalid stream 
10 Too many line items 
11 Client time-out waiting for response 
12 Declined. Check the credit card number, expiration date, and transaction information to 
make sure they were entered correctly. If this does not resolve the problem, have the 
customer call their card issuing bank to resolve. 
13 Referral. Transaction cannot be approved electronically but can be approved with a 
verbal authorization. Contact your merchant bank to obtain an authorization and submit 
a manual Voice Authorization transaction.  
14 Invalid Client Certification ID. Check the HTTP header. If the tag, X-VPS-VIT- 
CLIENT-CERTIFICATION-ID, is missing, RESULT code 14 is returned. 
19 Original transaction ID not found. The transaction ID you entered for this 
transaction is not valid. See RESPMSG. 
20 Cannot find the customer reference number 
22 Invalid ABA number 

23 Invalid account number. Check credit card number and re-submit. 
24 Invalid expiration date. Check and re-submit. 
25 Invalid Host Mapping. You are trying to process a tender type such as Discover Card, 
but you are not set up with your merchant bank to accept this card type. 
26 Invalid vendor account 
27 Insufficient partner permissions 
28 Insufficient user permissions 
29 Invalid XML document. This could be caused by an unrecognized XML tag or a bad 
XML format that cannot be parsed by the system. 
30 Duplicate transaction 
31 Error in adding the recurring profile 
32 Error in modifying the recurring profile 
33 Error in canceling the recurring profile 
34 Error in forcing the recurring profile 
35 Error in reactivating the recurring profile 
36 OLTP Transaction failed 
37 Invalid recurring profile ID 
50 Insufficient funds available in account 
99 General error. See RESPMSG. 
100 Transaction type not supported by host 
101 Time-out value too small 
102 Processor not available 
103 Error reading response from host 
104 Timeout waiting for processor response. Try your transaction again. 
105 Credit error. Make sure you have not already credited this transaction, or that this 
transaction ID is for a creditable transaction. (For example, you cannot credit an 
authorization.) 
106 Host not available 
107 Duplicate suppression time-out 

108 Void error. See RESPMSG. Make sure the transaction ID entered has not already been 
voided. If not, then look at the Transaction Detail screen for this transaction to see if it 
has settled. (The Batch field is set to a number greater than zero if the transaction has 
been settled). If the transaction has already settled, your only recourse is a reversal 
(credit a payment or submit a payment for a credit). 
109 Time-out waiting for host response 
111 Capture error. Either an attempt to capture a transaction that is not an authorization 
transaction type, or an attempt to capture an authorization transaction that has already 
been captured. 
112 Failed AVS check. Address and ZIP code do not match. An authorization may still 
exist on the cardholder’s account. 
113 Merchant sale total will exceed the sales cap with current transaction. ACH 
transactions only. 
114 Card Security Code (CSC) Mismatch. An authorization may still exist on the 
cardholder’s account. 
115 System busy, try again later 
116 VPS Internal error. Failed to lock terminal number 
117 Failed merchant rule check. One or more of the following three failures occurred: 
An attempt was made to submit a transaction that failed to meet the security settings 
specified on the Payflow Pro Java SDK Security Settings page. If the transaction 
exceeded the Maximum Amount security setting, then no values are returned for AVS 
or CSC. 
AVS validation failed. The AVS return value should appear in the RESPMSG. 
CSC validation failed. The CSC return value should appear in the RESPMSG. 
118 Invalid keywords found in string fields 
122 Merchant sale total will exceed the credit cap with current transaction. ACH 
transactions only. 
125 Fraud Protection Services Filter — Declined by filters 

126 Fraud Protection Services Filter — Flagged for review by filters 
Important Note: Result code 126 indicates that a transaction triggered a fraud filter. 
This is not an error, but a notice that the transaction is in a review status. The 
transaction has been authorized but requires you to review and to manually accept the 
transaction before it will be allowed to settle. 
Result code 126 is intended to give you an idea of the kind of transaction that is 
considered suspicious to enable you to evaluate whether you can benefit from using the 
Fraud Protection Services. 
To eliminate result 126, turn the filters off. 
For more information, see the User’s Guide for Payflow Pro With Fraud Protection 
Services or User’s Guide for Payflow Link Guide With Fraud Protection Services. 
127 Fraud Protection Services Filter — Not processed by filters 
128 Fraud Protection Services Filter — Declined by merchant after being flagged for 
review by filters 
131 Version 1 Payflow Pro SDK client no longer supported. Upgrade to the most recent 
version of the Payflow Pro client. 
150 Issuing bank timed out 
151 Issuing bank unavailable 
1000 Generic host error. This is a generic message returned by your credit card processor. 
The RESPMSG will contain more information describing the error. 
1001 Buyer Authentication Service unavailable 
1002 Buyer Authentication Service — Transaction timeout 
1003 Buyer Authentication Service — Invalid client version 
1004 Buyer Authentication Service — Invalid timeout value 
1011 Buyer Authentication Service unavailable 
1012 Buyer Authentication Service unavailable 
1013 Buyer Authentication Service unavailable 
1014 Buyer Authentication Service — Merchant is not enrolled for Buyer 
Authentication Service (3-D Secure). 
1016 Buyer Authentication Service — 3-D Secure error response received. Instead of 
receiving a PARes response to a Validate Authentication transaction, an error response 
was received. 
1017 Buyer Authentication Service — 3-D Secure error response is invalid. An error 
response is received and the response is not well formed for a Validate Authentication 
transaction. 

1021 Buyer Authentication Service — Invalid card type 
1022 Buyer Authentication Service — Invalid or missing currency code 
1023 Buyer Authentication Service — merchant status for 3D secure is invalid 
1041 Buyer Authentication Service — Validate Authentication failed: missing or 
invalid PARES 
1042 Buyer Authentication Service — Validate Authentication failed: PARES format is 
invalid 
1043 Buyer Authentication Service — Validate Authentication failed: Cannot find 
successful Verify Enrollment 
1044 Buyer Authentication Service — Validate Authentication failed: Signature 
validation failed for PARES 
1045 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid amount in PARES 
1046 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid acquirer in PARES 
1047 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid Merchant ID in PARES 
1048 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid card number in PARES 
1049 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid currency code in PARES 
1050 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid XID in PARES 
1051 Buyer Authentication Service — Validate Authentication failed: Mismatched or 
invalid order date in PARES 
1052 Buyer Authentication Service — Validate Authentication failed: This PARES was 
already validated for a previous Validate Authentication transaction 

 */
    
/* RESULT for communication errors (less than 0)
 * 
-1 Failed to connect to host 
-2 Failed to resolve hostname 
-5 Failed to initialize SSL context 
-6 Parameter list format error: & in name 
-7 Parameter list format error: invalid [ ] name length clause 
-8 SSL failed to connect to host 
-9 SSL read failed 
-10 SSL write failed 
-11 Proxy authorization failed 
-12 Timeout waiting for response 
-13 Select failure 
-14 Too many connections 
-15 Failed to set socket options 
-20 Proxy read failed 
-21 Proxy write failed 
-22 Failed to initialize SSL certificate 
-23 Host address not specified 
-24 Invalid transaction type 
-25 Failed to create a socket 
-26 Failed to initialize socket layer 
-27 Parameter list format error: invalid [ ] name length clause 
-28 Parameter list format error: name 
-29 Failed to initialize SSL connection 
-30 Invalid timeout value

-31 The certificate chain did not validate, no local certificate found 
-32 The certificate chain did not validate, common name did not match URL 
- 40 Unexpected Request ID found in request. 
- 41 Required Request ID not found in request 
- 42 Required Response ID not found in request 
- 43 Unexpected Response ID found in request 
- 44 Response ID not found in the response received from the server 
-99 Out of memory 
-100 Parameter list cannot be empty 
-103 Context initialization failed 
-104 Unexpected transaction state 
-105 Invalid name value pair request 
-106 Invalid response format 
-107 This XMLPay version is not supported 
-108 The server certificate chain did not validate 
-109 Unable to do logging 
-111 The following error occurred while initializing from message file: <Details of 
the error message> 
-113 Unable to round and truncate the currency value simultaneously 
 */
}
