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
package org.ofbiz.accounting.thirdparty.cybersource;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.cybersource.ws.client.Client;
import com.cybersource.ws.client.ClientException;
import com.cybersource.ws.client.FaultException;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * CyberSource WS Integration Services
 */
public class IcsPaymentServices {

    public static final String module = IcsPaymentServices.class.getName();

    // load the JSSE properties
    static {
        SSLUtil.loadJsseProperties();
    }

    public static Map ccAuth(DispatchContext dctx, Map context) {
        // generate the request/properties
        Properties props = buildCsProperties(context);
        if (props == null) {
            return ServiceUtil.returnError("ERROR: Getting Cybersource property configuration");
        }

        Map request = buildAuthRequest(context);
        request.put("merchantID", props.get("merchantID"));

        // transmit the request
        Map reply = null;
        try {
            reply = Client.runTransaction(request, props);
        } catch (FaultException e) {
            Debug.logError(e, "ERROR: Fault from CyberSource", module);
            Debug.logError(e, "Fault : " + e.getFaultString(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        } catch (ClientException e) {
            Debug.logError(e, "ERROR: CyberSource Client exception : " + e.getMessage(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        }

        // process the reply
        Map result = ServiceUtil.returnSuccess();
        processAuthResult(reply, result);
        return result;
    }

    public static Map ccReAuth(DispatchContext dctx, Map context) {
        return ServiceUtil.returnSuccess();
    }

    public static Map ccCapture(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");

        //lets see if there is a auth transaction already in context
        GenericValue authTransaction = (GenericValue) context.get("authTrans");

        if (authTransaction == null){
            authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        }

        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot capture");
        }

        // generate the request/properties
        Properties props = buildCsProperties(context);
        if (props == null) {
            return ServiceUtil.returnError("ERROR: Getting Cybersource property configuration");
        }

        Map request = buildCaptureRequest(context, authTransaction);
        request.put("merchantID", props.get("merchantID"));

        // transmit the request
        Map reply = null;
        try {
            reply = Client.runTransaction(request, props);
        } catch (FaultException e) {
            Debug.logError(e, "ERROR: Fault from CyberSource", module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        } catch (ClientException e) {
            Debug.logError(e, "ERROR: CyberSource Client exception : " + e.getMessage(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        }

        // process the reply
        Map result = ServiceUtil.returnSuccess();
        processCaptureResult(reply, result);
        return result;
    }

    public static Map ccRelease(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot release");
        }

        // generate the request/properties
        Properties props = buildCsProperties(context);
        if (props == null) {
            return ServiceUtil.returnError("ERROR: Getting Cybersource property configuration");
        }

        Map request = buildReleaseRequest(context, authTransaction);
        request.put("merchantID", props.get("merchantID"));

        // transmit the request
        Map reply = null;
        try {
            reply = Client.runTransaction(request, props);
        } catch (FaultException e) {
            Debug.logError(e, "ERROR: Fault from CyberSource", module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        } catch (ClientException e) {
            Debug.logError(e, "ERROR: CyberSource Client exception : " + e.getMessage(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        }

        // process the reply
        Map result = ServiceUtil.returnSuccess();
        processReleaseResult(reply, result);
        return result;
    }

    public static Map ccRefund(DispatchContext dctx, Map context) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found for the OrderPaymentPreference; cannot refund");
        }

        // generate the request/properties
        Properties props = buildCsProperties(context);
        if (props == null) {
            return ServiceUtil.returnError("ERROR: Getting Cybersource property configuration");
        }

        Map request = buildRefundRequest(context, authTransaction);
        request.put("merchantID", props.get("merchantID"));

        // transmit the request
        Map reply = null;
        try {
            reply = Client.runTransaction(request, props);
        } catch (FaultException e) {
            Debug.logError(e, "ERROR: Fault from CyberSource", module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        } catch (ClientException e) {
            Debug.logError(e, "ERROR: CyberSource Client exception : " + e.getMessage(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        }

        // process the reply
        Map result = ServiceUtil.returnSuccess();
        processRefundResult(reply, result);
        return result;
    }

    public static Map ccCredit(DispatchContext dctx, Map context) {
        // generate the request/properties
        Properties props = buildCsProperties(context);
        if (props == null) {
            return ServiceUtil.returnError("ERROR: Getting Cybersource property configuration");
        }

        Map request = buildCreditRequest(context);
        request.put("merchantID", props.get("merchantID"));

        // transmit the request
        Map reply = null;
        try {
            reply = Client.runTransaction(request, props);
        } catch (FaultException e) {
            Debug.logError(e, "ERROR: Fault from CyberSource", module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        } catch (ClientException e) {
            Debug.logError(e, "ERROR: CyberSource Client exception : " + e.getMessage(), module);
            return ServiceUtil.returnError("Unable to communicate with CyberSource");
        }

        // process the reply
        Map result = ServiceUtil.returnSuccess();
        processCreditResult(reply, result);
        return result;
    }

    private static Properties buildCsProperties(Map context) {
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }

        String merchantId = UtilProperties.getPropertyValue(configString, "payment.cybersource.merchantID");
        String targetApi = UtilProperties.getPropertyValue(configString, "payment.cybersource.api.version");
        String production = UtilProperties.getPropertyValue(configString, "payment.cybersource.production");
        String enableLog = UtilProperties.getPropertyValue(configString, "payment.cybersource.log");
        String logSize = UtilProperties.getPropertyValue(configString, "payment.cybersource.log.size");
        String logFile = UtilProperties.getPropertyValue(configString, "payment.cybersource.log.file");
        String logDir = UtilProperties.getPropertyValue(configString, "payment.cybersource.log.dir");

        String keysPath = UtilProperties.getPropertyValue(configString, "payment.cybersource.keysDir");
        String keysFile = UtilProperties.getPropertyValue(configString, "payment.cybersource.keysFile");

        // some property checking
        if (UtilValidate.isEmpty(merchantId)) {
            Debug.logWarning("The merchantId property in [" + configString + "] is not configured", module);
            return null;
        }
        if (UtilValidate.isEmpty(keysPath)) {
            Debug.logWarning("The keysDir property in [" + configString + "] is not configured", module);
            return null;
        }

        // create some properties for CS Client
        Properties props = new Properties();
        props.put("merchantID", merchantId);
        props.put("keysDirectory", keysPath);
        props.put("targetAPIVersion", targetApi);
        props.put("sendToProduction", production);
        props.put("enableLog", enableLog);
        props.put("logDirectory", logDir);
        props.put("logFilename", logFile);
        props.put("logMaximumSize", logSize);

        if (keysFile != null && keysFile.length() > 0) {
            props.put("alternateKeyFilename", keysFile);
        }
        //Debug.logInfo("Created CyberSource Properties : " + props, module);

        return props;
    }

    private static Map buildAuthRequest(Map context) {
        String configString = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        if (configString == null) {
            configString = "payment.properties";
        }

        // make the request map
        String capture = UtilProperties.getPropertyValue(configString, "payment.cybersource.autoBill", "false");
        String orderId = (String) context.get("orderId");

        Map request = new HashMap();
        request.put("ccAuthService_run", "true");              // run auth service
        request.put("ccCaptureService_run", capture);          // run capture service (i.e. sale)
        request.put("merchantReferenceCode", orderId);         // set the order ref number
        request.put("purchaseTotals_currency", currency);      // set the order currency
        appendFullBillingInfo(request, context);               // add in all address info
        appendItemLineInfo(request, context, "processAmount"); // add in the item info
        appendAvsRules(request, context);                      // add in the AVS flags and decline codes

        return request;
    }

    private static Map buildCaptureRequest(Map context, GenericValue authTransaction) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        String configString = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        if (configString == null) {
            configString = "payment.properties";
        }
        String merchantDesc = UtilProperties.getPropertyValue(configString, "payment.cybersource.merchantDescr", null);
        String merchantCont = UtilProperties.getPropertyValue(configString, "payment.cybersource.merchantContact", null);

        Map request = new HashMap();
        request.put("ccCaptureService_run", "true");
        request.put("ccCaptureService_authRequestID", authTransaction.getString("referenceNum"));
        request.put("item_0_unitPrice", getAmountString(context, "captureAmount"));
        request.put("merchantReferenceCode", orderPaymentPreference.getString("orderId"));
        request.put("purchaseTotals_currency", currency);

        // TODO: add support for verbal authorizations
        //request.put("ccCaptureService_authType", null);   -- should be 'verbal'
        //request.put("ccCaptureService_verbalAuthCode", null); -- code from verbal auth

        if (merchantDesc != null) {
            request.put("invoiceHeader_merchantDescriptor", merchantDesc);        // merchant description
        }
        if (merchantCont != null) {
            request.put("invoiceHeader_merchantDescriptorContact", merchantCont); // merchant contact info
        }

        return request;
    }

    private static Map buildReleaseRequest(Map context, GenericValue authTransaction) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        String currency = (String) context.get("currency");
        Map request = new HashMap();
        request.put("ccAuthReversalService_run", "true");
        request.put("ccAuthReversalService_authRequestID", authTransaction.getString("referenceNum"));
        request.put("item_0_unitPrice", getAmountString(context, "releaseAmount"));
        request.put("merchantReferenceCode", orderPaymentPreference.getString("orderId"));
        request.put("purchaseTotals_currency", currency);
        return request;
    }

    private static Map buildRefundRequest(Map context, GenericValue authTransaction) {
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        String configString = (String) context.get("paymentConfig");
        String currency = (String) context.get("currency");
        if (configString == null) {
            configString = "payment.properties";
        }
        String merchantDesc = UtilProperties.getPropertyValue(configString, "payment.cybersource.merchantDescr", null);
        String merchantCont = UtilProperties.getPropertyValue(configString, "payment.cybersource.merchantContact", null);

        Map request = new HashMap();
        request.put("ccCreditService_run", "true");
        request.put("ccCreditService_captureRequestID", authTransaction.getString("referenceNum"));
        request.put("item_0_unitPrice", getAmountString(context, "refundAmount"));
        request.put("merchantReferenceCode", orderPaymentPreference.getString("orderId"));
        request.put("purchaseTotals_currency", currency);

        if (merchantDesc != null) {
            request.put("invoiceHeader_merchantDescriptor", merchantDesc);        // merchant description
        }
        if (merchantCont != null) {
            request.put("invoiceHeader_merchantDescriptorContact", merchantCont); // merchant contact info
        }

        return request;
    }

    private static Map buildCreditRequest(Map context) {
        String refCode = (String) context.get("referenceCode");
        Map request = new HashMap();
        request.put("ccCreditService_run", "true");            // run credit service
        request.put("merchantReferenceCode", refCode);         // set the ref number could be order id
        appendFullBillingInfo(request, context);               // add in all address info
        appendItemLineInfo(request, context, "creditAmount");  // add in the item info
        return request;
    }

    private static void appendAvsRules(Map request, Map context) {
        String configString = (String) context.get("paymentConfig");
        if (configString == null) {
            configString = "payment.properties";
        }
        String avsCodes = UtilProperties.getPropertyValue(configString, "payment.cybersource.avsDeclineCodes", null);

        GenericValue party = (GenericValue) context.get("billToParty");
        if (party != null) {
            GenericValue avsOverride = null;

            try {
                avsOverride = party.getDelegator().findByPrimaryKey("PartyIcsAvsOverride",
                        UtilMisc.toMap("partyId", party.getString("partyId")));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (avsOverride != null && avsOverride.get("avsDeclineString") != null) {
                String overrideString = avsOverride.getString("avsDeclineString");
                if (overrideString != null && overrideString.length() > 0) {
                    avsCodes = overrideString;
                }
            }
        }

        if (avsCodes != null && avsCodes.length() > 0) {
            request.put("businessRules_declineAVSFlags", avsCodes);
        }

        String avsIgnore = UtilProperties.getPropertyValue(configString, "payment.cybersource.avsDeclineCodes", "false");
        request.put("businessRules_ignoreAVS", avsIgnore);
    }

    private static void appendFullBillingInfo(Map request, Map context) {
        // person info
        GenericValue party = (GenericValue) context.get("billToParty");

        // contact info
        GenericValue email = (GenericValue) context.get("billToEmail");
        if (email != null) {
            request.put("billTo_email", email.getString("infoString"));
        } else {
            Debug.logWarning("Email not defined; Cybersource will fail.", module);
        }

        // phone number seems to not be used; possibly only for reporting.

        // CC payment info
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        if (creditCard != null) {
            List expDateList = StringUtil.split(creditCard.getString("expireDate"), "/");

            request.put("billTo_firstName", creditCard.getString("firstNameOnCard"));
            request.put("billTo_lastName", creditCard.getString("lastNameOnCard"));
            request.put("card_accountNumber", creditCard.getString("cardNumber"));
            request.put("card_expirationMonth", expDateList.get(0));
            request.put("card_expirationYear", expDateList.get(1));
        } else {
            Debug.logWarning("CreditCard not defined; Cybersource will fail.", module);
        }

        // CCV info
        String cvNum = (String) context.get("cardSecurityCode");
        String cvSet = UtilValidate.isEmpty(cvNum) ? "1" : "0";
        request.put("card_cvIndicator", cvSet);
        if ("1".equals(cvNum)) {
            request.put("card_cvNumber", cvNum);
        }

        // payment contact info
        GenericValue billingAddress = (GenericValue) context.get("billingAddress");

        if (billingAddress != null) {
            request.put("billTo_street1", billingAddress.getString("address1"));
            if (billingAddress.get("address2") != null) {
                request.put("billTo_street2", billingAddress.getString("address2"));
            }
            request.put("billTo_city", billingAddress.getString("city"));
            String bCountry = billingAddress.get("countryGeoId") != null ? billingAddress.getString("countryGeoId") : "USA";

            request.put("billTo_country", bCountry);
            request.put("billTo_postalCode", billingAddress.getString("postalCode"));
            if (billingAddress.get("stateProvinceGeoId") != null) {
                request.put("billTo_state", billingAddress.getString("stateProvinceGeoId"));
            }
        } else {
            Debug.logWarning("BillingAddress not defined; Cybersource will fail.", module);
        }

        // order shipping information
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");
        if (shippingAddress != null) {
            if (creditCard != null) {
                // TODO: this is just a kludge since we don't have a firstName and lastName on the PostalAddress entity, that needs to be done
                request.put("shipTo_firstName", creditCard.getString("firstNameOnCard"));
                request.put("shipTo_lastName", creditCard.getString("lastNameOnCard"));
            }

            request.put("shipTo_street1", shippingAddress.getString("address1"));
            if (shippingAddress.get("address2") != null) {
                request.put("shipTo_street2", shippingAddress.getString("address2"));
            }
            request.put("shipTo_city", shippingAddress.getString("city"));
            String sCountry = shippingAddress.get("countryGeoId") != null ? shippingAddress.getString("countryGeoId") : "USA";

            request.put("shipTo_country", sCountry);
            request.put("shipTo_postalCode", shippingAddress.getString("postalCode"));
            if (shippingAddress.get("stateProvinceGeoId") != null) {
                request.put("shipTo_state", shippingAddress.getString("stateProvinceGeoId"));
            }
        }
    }

    private static void appendItemLineInfo(Map request, Map context, String amountField) {
        // send over a line item total offer w/ the total for billing; don't trust CyberSource for calc
        String currency = (String) context.get("currency");

        int lineNumber = 0;
        request.put("item_" + lineNumber + "_unitPrice", getAmountString(context, amountField));

        // the currency
        request.put("purchaseTotals_currency", currency);

        // create the offers (one for each line item)
        List orderItems = (List) context.get("orderItems");
        if (orderItems != null) {
            Iterator itemIterator = orderItems.iterator();

            while (itemIterator.hasNext()) {
                lineNumber++;
                GenericValue item = (GenericValue) itemIterator.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to get Product from OrderItem, not passing info to CyberSource");
                }

                if (product != null) {
                    request.put("item_" + lineNumber + "_productName", product.getString("productName"));
                    request.put("item_" + lineNumber + "_productSKU", product.getString("productId"));
                } else {
                    // no product; just send the item description -- non product items
                    request.put("item_" + lineNumber + "_productName", item.getString("description"));
                }

                // get the quantity..
                Double quantity = item.getDouble("quantity");

                // test quantity if INT pass as is; if not pass as 1
                long roundQ = Math.round(quantity.doubleValue());
                Double rounded = new Double(new Long(roundQ).toString());

                if (rounded.doubleValue() != quantity.doubleValue()) {
                    request.put("item_" + lineNumber + "_quantity", "1");
                } else {
                    request.put("", new Integer(quantity.intValue()).toString());
                }

                // set the amount to 0.0000 -- we will send a total too.
                request.put("item_" + lineNumber + "_unitPrice", "0.0000");
            }
        }
    }

    private static String getAmountString(Map context, String amountField) {
        String currencyFormat = UtilProperties.getPropertyValue("general.properties", "currency.decimal.format", "##0.00");
        DecimalFormat formatter = new DecimalFormat(currencyFormat);
        Double processAmount = (Double) context.get(amountField);
        return formatter.format(processAmount);
    }

    private static void processAuthResult(Map reply, Map result) {
        String decision = getDecision(reply);
        if ("ACCEPT".equalsIgnoreCase(decision)) {
            result.put("authCode", reply.get("ccAuthReply_authorizationCode"));
            result.put("authResult", Boolean.TRUE);
        } else {
            result.put("authCode", decision);
            result.put("authResult", Boolean.FALSE);
            // TODO: based on reasonCode populate the following flags as applicable: resultDeclined, resultNsf, resultBadExpire, resultBadCardNumber
        }

        if (reply.get("ccAuthReply_amount") != null) {
            result.put("processAmount", new Double((String) reply.get("ccAuthReply_amount")));
        } else {
            result.put("processAmount", new Double(0.00));
        }

        result.put("authRefNum", reply.get("requestID"));
        result.put("authFlag", reply.get("ccAuthReply_reasonCode"));
        result.put("authMessage", reply.get("ccAuthReply_processorResponse"));
        result.put("cvCode", reply.get("ccAuthReply_cvCode"));
        result.put("avsCode", reply.get("ccAuthReply_avsCode"));
        result.put("scoreCode", reply.get("ccAuthReply_authFactorCode"));
        result.put("captureRefNum", reply.get("requestID")); // maybe use something else here?
        result.put("captureCode", reply.get("ccCaptureReply_reconciliationID"));
    }

    private static void processCaptureResult(Map reply, Map result) {
        String decision = getDecision(reply);
        if ("ACCEPT".equalsIgnoreCase(decision)) {
            result.put("captureResult", Boolean.TRUE);
        } else {
            result.put("captureResult", Boolean.FALSE);
        }

        if (reply.get("ccCaptureReply_amount") != null) {
            result.put("captureAmount", new Double((String) reply.get("ccCaptureReply_amount")));
        } else {
            result.put("captureAmount", new Double(0.00));
        }

        result.put("captureRefNum", reply.get("requestID"));
        result.put("captureCode", reply.get("ccCaptureReply_reconciliationID"));
        result.put("captureFlag", reply.get("ccCaptureReply_reasonCode"));
        result.put("captureMessage", reply.get("decision"));
    }

    private static void processReleaseResult(Map reply, Map result) {
        String decision = getDecision(reply);
        if ("ACCEPT".equalsIgnoreCase(decision)) {
            result.put("releaseResult", Boolean.TRUE);
        } else {
            result.put("releaseResult", Boolean.FALSE);
        }

        if (reply.get("ccAuthReversalReply_amount") != null) {
            result.put("releaseAmount", new Double((String) reply.get("ccAuthReversalReply_amount")));
        } else {
            result.put("releaseAmount", new Double(0.00));
        }

        result.put("releaseRefNum", reply.get("requestID"));
        result.put("releaseCode", reply.get("ccAuthReversalReply_authorizationCode"));
        result.put("releaseFlag", reply.get("ccAuthReversalReply_reasonCode"));
        result.put("releaseMessage", reply.get("decision"));
    }

    private static void processRefundResult(Map reply, Map result) {
        String decision = getDecision(reply);
        if ("ACCEPT".equalsIgnoreCase(decision)) {
            result.put("refundResult", Boolean.TRUE);
        } else {
            result.put("refundResult", Boolean.FALSE);
        }

        if (reply.get("ccCreditReply_amount") != null) {
            result.put("refundAmount", new Double((String) reply.get("ccCreditReply_amount")));
        } else {
            result.put("refundAmount", new Double(0.00));
        }

        result.put("refundRefNum", reply.get("requestID"));
        result.put("refundCode", reply.get("ccCreditReply_reconciliationID"));
        result.put("refundFlag", reply.get("ccCreditReply_reasonCode"));
        result.put("refundMessage", reply.get("decision"));
    }

    private static void processCreditResult(Map reply, Map result) {
        String decision = (String) reply.get("decision");
        if ("ACCEPT".equalsIgnoreCase(decision)) {
            result.put("creditResult", Boolean.TRUE);
        } else {
            result.put("creditResult", Boolean.FALSE);
        }

        if (reply.get("ccCreditReply_amount") != null) {
            result.put("creditAmount", new Double((String) reply.get("ccCreditReply_amount")));
        } else {
            result.put("creditAmount", new Double(0.00));
        }

        result.put("creditRefNum", reply.get("requestID"));
        result.put("creditCode", reply.get("ccCreditReply_reconciliationID"));
        result.put("creditFlag", reply.get("ccCreditReply_reasonCode"));
        result.put("creditMessage", reply.get("decision"));
    }

    private static String getDecision(Map reply) {
        String decision = (String) reply.get("decision");
        String reasonCode = (String) reply.get("reasonCode");
        if (!"ACCEPT".equalsIgnoreCase(decision)) {
            Debug.logInfo("CyberSource : " + decision + " (" + reasonCode + ")", module);
            Debug.logInfo("Reply Dump : " + reply, module);
        }
        return decision;
    }
}
