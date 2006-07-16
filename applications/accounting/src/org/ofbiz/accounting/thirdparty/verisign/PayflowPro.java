/*
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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
        if (respCode.equals("0") && avsCheckOkay && cvv2CheckOkay) {
            result.put("authResult", Boolean.TRUE);
            result.put("authCode", parameters.get("AUTHCODE"));
        } else {
            result.put("authResult", Boolean.FALSE);
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
}

