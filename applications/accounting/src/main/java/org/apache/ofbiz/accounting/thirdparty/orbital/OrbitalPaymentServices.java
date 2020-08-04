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
package org.apache.ofbiz.accounting.thirdparty.orbital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.paymentech.orbital.sdk.configurator.Configurator;
import com.paymentech.orbital.sdk.interfaces.RequestIF;
import com.paymentech.orbital.sdk.interfaces.ResponseIF;
import com.paymentech.orbital.sdk.interfaces.TransactionProcessorIF;
import com.paymentech.orbital.sdk.request.FieldNotFoundException;
import com.paymentech.orbital.sdk.request.Request;
import com.paymentech.orbital.sdk.transactionProcessor.TransactionException;
import com.paymentech.orbital.sdk.transactionProcessor.TransactionProcessor;
import com.paymentech.orbital.sdk.util.exceptions.InitializationException;

public class OrbitalPaymentServices {

    private static final String MODULE = OrbitalPaymentServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";
    private static final String ERROR = "Error";

    private static final int DECIMALS = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("invoice.rounding");


    public static final String BIN_VALUE = "000002";
    public static TransactionProcessorIF tp = null;
    public static ResponseIF response = null;
    public static RequestIF request = null;

    public static Map<String, Object> ccAuth(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        props.put("transType", "AUTH_ONLY");
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, "Error in request initialization", MODULE);
        }
        buildAuthOrAuthCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get("processCardResponse"));
        processAuthTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccAuthCapture(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        props.put("transType", "AUTH_CAPTURE");
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, "Error in request initialization", MODULE);
        }
        buildAuthOrAuthCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get("processCardResponse"));
        processAuthCaptureTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccCapture(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne("CreditCard", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }
        context.put("creditCard", creditCard);
        context.put("authTransaction", authTransaction);
        context.put("orderId", orderPaymentPreference.getString("orderId"));

        props.put("transType", "PRIOR_AUTH_CAPTURE");
        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.MARK_FOR_CAPTURE_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, "Error in request initialization", MODULE);
        }
        buildCaptureTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get("processCardResponse"));
        processCaptureTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccRefund(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue creditCard = null;
        try {
            creditCard = orderPaymentPreference.getRelatedOne("CreditCard", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }
        context.put("creditCard", creditCard);
        context.put("authTransaction", authTransaction);
        context.put("orderId", orderPaymentPreference.getString("orderId"));

        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.NEW_ORDER_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, "Error in request initialization", MODULE);
        }
        buildRefundTransaction(context, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get("processCardResponse"));
        processRefundTransResult(processCardResponseContext, results);
        return results;
    }

    public static Map<String, Object> ccRelease(DispatchContext ctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> props = buildOrbitalProperties(context, delegator);

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        try {
            orderPaymentPreference.getRelatedOne("CreditCard", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentUnableToGetCCInfo", locale));
        }
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }
        context.put("authTransaction", authTransaction);
        context.put("orderId", orderPaymentPreference.getString("orderId"));

        //Tell the request object which template to use (see RequestIF.java)
        try {
            request = new Request(RequestIF.REVERSE_TRANSACTION);
        } catch (InitializationException e) {
            Debug.logError(e, "Error in request initialization", MODULE);
        }
        buildReleaseTransaction(context, delegator, props, request, results);
        Map<String, Object> validateResults = validateRequest(context, props, request);
        String respMsg = (String) validateResults.get(ModelService.RESPONSE_MESSAGE);
        if (ModelService.RESPOND_ERROR.equals(respMsg)) {
            results.put(ModelService.ERROR_MESSAGE, "Validation Failed - invalid values");
            return results;
        }
        initializeTransactionProcessor();
        Map<String, Object> processCardResponseContext = processCard(request);
        // For Debugging Purpose
        printTransResult((ResponseIF) processCardResponseContext.get("processCardResponse"));
        processReleaseTransResult(processCardResponseContext, results);
        return results;
    }


    private static Map<String, Object> buildOrbitalProperties(Map<String, Object> context, Delegator delegator) {
        //TODO: Will move this to property file and then will read it from there.
        String configFile = "/applications/accounting/config/linehandler.properties";
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        Map<String, Object> buildConfiguratorContext = new HashMap<>();
        try {
            buildConfiguratorContext.put("OrbitalConnectionUsername", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "username"));
            buildConfiguratorContext.put("OrbitalConnectionPassword", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "connectionPassword"));
            buildConfiguratorContext.put("merchantId", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "merchantId"));
            buildConfiguratorContext.put("engine.class", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "engineClass"));
            buildConfiguratorContext.put("engine.hostname", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hostName"));
            buildConfiguratorContext.put("engine.port", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "port"));
            buildConfiguratorContext.put("engine.hostname.failover", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "hostNameFailover"));
            buildConfiguratorContext.put("engine.port.failover", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "portFailover"));
            buildConfiguratorContext.put("engine.connection_timeout_seconds", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "connectionTimeoutSeconds"));
            buildConfiguratorContext.put("engine.read_timeout_seconds", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "readTimeoutSeconds"));
            buildConfiguratorContext.put("engine.authorizationURI", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "authorizationURI"));
            buildConfiguratorContext.put("engine.sdk_version", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "sdkVersion"));
            buildConfiguratorContext.put("engine.ssl.socketfactory", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "sslSocketFactory"));
            buildConfiguratorContext.put("Response.response_type", getPaymentGatewayConfigValue(delegator, paymentGatewayConfigId, "responseType"));
            String configFileLocation = System.getProperty("ofbiz.home") + configFile;
            Configurator config = Configurator.getInstance(configFileLocation);
            buildConfiguratorContext.putAll(config.getConfigurations());
            config.setConfigurations(buildConfiguratorContext);
        } catch (InitializationException e) {
            Debug.logError(e, "Orbital Configurator Initialization Error: " + e.getMessage(), MODULE);
        }
        return buildConfiguratorContext;
    }

    private static String getPaymentGatewayConfigValue(Delegator delegator, String paymentGatewayConfigId,
            String paymentGatewayConfigParameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue paymentGatewayOrbital = EntityQuery.use(delegator).from("PaymentGatewayOrbital").where("paymentGatewayConfigId", paymentGatewayConfigId).queryOne();
                if (paymentGatewayOrbital != null) {
                    Object paymentGatewayOrbitalField = paymentGatewayOrbital.get(paymentGatewayConfigParameterName);
                    if (paymentGatewayOrbitalField != null) {
                        return returnValue = paymentGatewayOrbitalField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return returnValue;
    }

    private static void buildAuthOrAuthCaptureTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue cc = (GenericValue) params.get("creditCard");
        BigDecimal amount = (BigDecimal) params.get("processAmount");
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));
        expDate = formatExpDateForOrbital(expDate);
        String cardSecurityCode = (String) params.get("cardSecurityCode");
        String orderId = UtilFormatOut.checkNull((String) params.get("orderId"));
        String transType = props.get("transType").toString();
        String messageType = null;
        if ("AUTH_ONLY".equals(transType)) {
            messageType = "A";
        } else if ("AUTH_CAPTURE".equals(transType)) {
            messageType = "AC";
        }
        try {
            request.setFieldValue("IndustryType", "EC");
            request.setFieldValue("MessageType", UtilFormatOut.checkNull(messageType));
            request.setFieldValue("MerchantID", UtilFormatOut.checkNull(props.get("merchantId").toString()));
            request.setFieldValue("BIN", BIN_VALUE);
            request.setFieldValue("OrderID", UtilFormatOut.checkNull(orderId));
            request.setFieldValue("AccountNum", UtilFormatOut.checkNull(number));

            request.setFieldValue("Amount", UtilFormatOut.checkNull(amountValue));
            request.setFieldValue("Exp", UtilFormatOut.checkNull(expDate));
            // AVS Information
            GenericValue creditCard = null;
            if (params.get("orderPaymentPreference") != null) {
                GenericValue opp = (GenericValue) params.get("orderPaymentPreference");
                if ("CREDIT_CARD".equals(opp.getString("paymentMethodTypeId"))) {
                    // sometimes the ccAuthCapture interface is used, in which case the creditCard is passed directly
                     creditCard = (GenericValue) params.get("creditCard");
                    if (creditCard == null || !(opp.get("paymentMethodId").equals(creditCard.get("paymentMethodId")))) {
                        creditCard = opp.getRelatedOne("CreditCard", false);
                    }
                }

                request.setFieldValue("AVSname", "Demo Customer");
                if (UtilValidate.isNotEmpty(creditCard.getString("contactMechId"))) {
                    GenericValue address = creditCard.getRelatedOne("PostalAddress", false);
                    if (address != null) {
                        request.setFieldValue("AVSaddress1", UtilFormatOut.checkNull(address.getString("address1")));
                        request.setFieldValue("AVScity", UtilFormatOut.checkNull(address.getString("city")));
                        request.setFieldValue("AVSstate", UtilFormatOut.checkNull(address.getString("stateProvinceGeoId")));
                        request.setFieldValue("AVSzip", UtilFormatOut.checkNull(address.getString("postalCode")));
                    }
                }
            } else {
                // this would be the case for an authorization
                GenericValue cp = (GenericValue) params.get("billToParty");
                GenericValue ba = (GenericValue) params.get("billingAddress");
                request.setFieldValue("AVSname", UtilFormatOut.checkNull(cp.getString("firstName")) + UtilFormatOut.checkNull(cp.getString("lastName")));
                request.setFieldValue("AVSaddress1", UtilFormatOut.checkNull(ba.getString("address1")));
                request.setFieldValue("AVScity", UtilFormatOut.checkNull(ba.getString("city")));
                request.setFieldValue("AVSstate", UtilFormatOut.checkNull(ba.getString("stateProvinceGeoId")));
                request.setFieldValue("AVSzip", UtilFormatOut.checkNull(ba.getString("postalCode")));
                request.setFieldValue("AVSCountryCode", UtilFormatOut.checkNull(ba.getString("countryGeoId")));
            }
            // Additional Information
            request.setFieldValue("Comments", "This is building of request object");
            String shippingRef = getShippingRefForOrder(orderId, delegator);
            request.setFieldValue("ShippingRef", shippingRef);
            request.setFieldValue("CardSecVal", UtilFormatOut.checkNull(cardSecurityCode));

            //Display the request
            if ("AUTH_ONLY".equals(transType)) {
                Debug.logInfo("\nAuth Request:\n ======== " + request.getXML());
            } else if ("AUTH_CAPTURE".equals(transType)) {
                Debug.logInfo("\nAuth Capture Request:\n ======== " + request.getXML());
            }
            results.put("processAmount", amount);
        } catch (InitializationException ie) {
            Debug.logInfo("Unable to initialize request object", MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError("Unable to find XML field in template", MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildCaptureTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue authTransaction = (GenericValue) params.get("authTransaction");
        GenericValue creditCard = (GenericValue) params.get("creditCard");
        BigDecimal amount = (BigDecimal) params.get("captureAmount");
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String orderId = UtilFormatOut.checkNull((String) params.get("orderId"));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue("MerchantID", UtilFormatOut.checkNull(props.get("merchantId").toString()));
            request.setFieldValue("BIN", BIN_VALUE);
            request.setFieldValue("TxRefNum", UtilFormatOut.checkNull(authTransaction.get("referenceNum").toString()));
            request.setFieldValue("OrderID", UtilFormatOut.checkNull(orderId));
            request.setFieldValue("Amount", UtilFormatOut.checkNull(amountValue));

            request.setFieldValue("PCDestName", UtilFormatOut.checkNull(creditCard.getString("firstNameOnCard") + creditCard.getString("lastNameOnCard")));
            if (UtilValidate.isNotEmpty(creditCard.getString("contactMechId"))) {
                GenericValue address = creditCard.getRelatedOne("PostalAddress", false);
                if (address != null) {
                    request.setFieldValue("PCOrderNum", UtilFormatOut.checkNull(orderId));
                    request.setFieldValue("PCDestAddress1", UtilFormatOut.checkNull(address.getString("address1")));
                    request.setFieldValue("PCDestAddress2", UtilFormatOut.checkNull(address.getString("address2")));
                    request.setFieldValue("PCDestCity", UtilFormatOut.checkNull(address.getString("city")));
                    request.setFieldValue("PCDestState", UtilFormatOut.checkNull(address.getString("stateProvinceGeoId")));
                    request.setFieldValue("PCDestZip", UtilFormatOut.checkNull(address.getString("postalCode")));
                }
            }
            //Display the request
            Debug.logInfo("\nCapture Request:\n ======== " + request.getXML());
            results.put("captureAmount", amount);
        } catch (InitializationException ie) {
            Debug.logInfo("Unable to initialize request object", MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError("Unable to find XML field in template" + fnfe.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildRefundTransaction(Map<String, Object> params, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        GenericValue cc = (GenericValue) params.get("creditCard");
        BigDecimal amount = (BigDecimal) params.get("refundAmount");
        String amountValue = amount.setScale(DECIMALS, ROUNDING).movePointRight(2).toPlainString();
        String number = UtilFormatOut.checkNull(cc.getString("cardNumber"));
        String expDate = UtilFormatOut.checkNull(cc.getString("expireDate"));
        expDate = formatExpDateForOrbital(expDate);
        String orderId = UtilFormatOut.checkNull((String) params.get("orderId"));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue("IndustryType", "EC");
            request.setFieldValue("MessageType", "R");
            request.setFieldValue("MerchantID", UtilFormatOut.checkNull(props.get("merchantId").toString()));
            request.setFieldValue("BIN", BIN_VALUE);
            request.setFieldValue("OrderID", UtilFormatOut.checkNull(orderId));
            request.setFieldValue("AccountNum", UtilFormatOut.checkNull(number));
            request.setFieldValue("Amount", UtilFormatOut.checkNull(amountValue));
            request.setFieldValue("Exp", UtilFormatOut.checkNull(expDate));
            request.setFieldValue("Comments", "This is a credit card refund");

            Debug.logInfo("\nRefund Request:\n ======== " + request.getXML());
            results.put("refundAmount", amount);
        } catch (InitializationException ie) {
            Debug.logInfo("Unable to initialize request object", MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError("Unable to find XML field in template", MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void buildReleaseTransaction(Map<String, Object> params, Delegator delegator, Map<String, Object> props, RequestIF request, Map<String, Object> results) {
        BigDecimal amount = (BigDecimal) params.get("releaseAmount");
        GenericValue authTransaction = (GenericValue) params.get("authTransaction");
        String orderId = UtilFormatOut.checkNull((String) params.get("orderId"));
        try {
            //If there were no errors preparing the template, we can now specify the data
            //Basic Auth Fields
            request.setFieldValue("MerchantID", UtilFormatOut.checkNull(props.get("merchantId").toString()));
            request.setFieldValue("BIN", BIN_VALUE);
            request.setFieldValue("TxRefNum", UtilFormatOut.checkNull(authTransaction.get("referenceNum").toString()));
            request.setFieldValue("OrderID", UtilFormatOut.checkNull(orderId));

            //Display the request
            Debug.logInfo("\nRelease Request:\n ======== " + request.getXML());
            results.put("releaseAmount", amount);
        } catch (InitializationException ie) {
            Debug.logInfo("Unable to initialize request object", MODULE);
        } catch (FieldNotFoundException fnfe) {
            Debug.logError("Unable to find XML field in template" + fnfe.getMessage(), MODULE);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }


    private static void initializeTransactionProcessor() {
        //Create a Transaction Processor
        //The Transaction Processor acquires and releases resources and executes transactions.
        //It configures a pool of protocol engines, then uses the pool to execute transactions.
        try {
            tp = new TransactionProcessor();
        } catch (InitializationException iex) {
            Debug.logError("TransactionProcessor failed to initialize" + iex.getMessage(), MODULE);
            iex.printStackTrace();
        }
    }

    private static Map<String, Object> processCard(RequestIF request) {
        Map<String, Object> processCardResult = new HashMap<>();
        try {
            response = tp.process(request);
            if (response.isApproved()) {
                processCardResult.put("authResult", Boolean.TRUE);
            } else {
                processCardResult.put("authResult", Boolean.FALSE);
            }
            processCardResult.put("processCardResponse", response);
        } catch (TransactionException tex) {
            Debug.logError("TransactionProcessor failed to initialize" + tex.getMessage(), MODULE);
            tex.printStackTrace();
        }
        processCardResult.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return processCardResult;
    }

    private static void processAuthTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get("processCardResponse");
        Boolean authResult = (Boolean) processCardResponseContext.get("authResult");
        results.put("authResult", authResult);
        results.put("authFlag", response.getResponseCode());
        results.put("authMessage", response.getMessage());
        if (authResult) { //passed
            results.put("authCode", response.getAuthCode());
            results.put("authRefNum", response.getTxRefNum());
            results.put("cvCode", UtilFormatOut.checkNull(response.getCVV2RespCode()));
            results.put("avsCode", response.getAVSResponseCode());
            results.put("processAmount", new BigDecimal(results.get("processAmount").toString()));
        } else {
            results.put("authCode", response.getAuthCode());
            results.put("processAmount", BigDecimal.ZERO);
            results.put("authRefNum", OrbitalPaymentServices.ERROR);
        }
        Debug.logInfo("processAuthTransResult: " + results.toString(), MODULE);
    }

    private static void processAuthCaptureTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get("processCardResponse");
        Boolean authResult = (Boolean) processCardResponseContext.get("authResult");
        results.put("authResult", authResult);
        results.put("authFlag", response.getResponseCode());
        results.put("authMessage", response.getMessage());
        results.put("captureResult", authResult);
        results.put("captureFlag", response.getResponseCode());
        results.put("captureMessage", response.getMessage());
        results.put("captureRefNum", response.getTxRefNum());
        if (authResult) { //passed
            results.put("authCode", response.getAuthCode());
            results.put("authRefNum", response.getTxRefNum());
            results.put("cvCode", UtilFormatOut.checkNull(response.getCVV2RespCode()));
            results.put("avsCode", response.getAVSResponseCode());
            results.put("processAmount", new BigDecimal(results.get("processAmount").toString()));
        } else {
            results.put("authCode", response.getAuthCode());
            results.put("processAmount", BigDecimal.ZERO);
            results.put("authRefNum", OrbitalPaymentServices.ERROR);
        }
        Debug.logInfo("processAuthCaptureTransResult: " + results.toString(), MODULE);
    }

    private static void processCaptureTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get("processCardResponse");
        Boolean captureResult = (Boolean) processCardResponseContext.get("authResult");
        results.put("captureResult", captureResult);
        results.put("captureFlag", response.getResponseCode());
        results.put("captureMessage", response.getMessage());
        results.put("captureRefNum", response.getTxRefNum());
        if (captureResult) { //passed
            results.put("captureCode", response.getAuthCode());
            results.put("captureAmount", new BigDecimal(results.get("captureAmount").toString()));
        } else {
            results.put("captureAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("processCaptureTransResult: " + results.toString(), MODULE);
    }

    private static void processRefundTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get("processCardResponse");
        Boolean refundResult = (Boolean) processCardResponseContext.get("authResult");
        results.put("refundResult", refundResult);
        results.put("refundFlag", response.getResponseCode());
        results.put("refundMessage", response.getMessage());
        results.put("refundRefNum", response.getTxRefNum());
        if (refundResult) { //passed
            results.put("refundCode", response.getAuthCode());
            results.put("refundAmount", new BigDecimal(results.get("refundAmount").toString()));
        } else {
            results.put("refundAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("processRefundTransResult: " + results.toString(), MODULE);
    }

    private static void processReleaseTransResult(Map<String, Object> processCardResponseContext, Map<String, Object> results) {
        ResponseIF response = (ResponseIF) processCardResponseContext.get("processCardResponse");
        Boolean releaseResult = (Boolean) processCardResponseContext.get("authResult");
        results.put("releaseResult", releaseResult);
        results.put("releaseFlag", response.getResponseCode());
        results.put("releaseMessage", response.getMessage());
        results.put("releaseRefNum", response.getTxRefNum());
        if (releaseResult) { //passed
            results.put("releaseCode", response.getAuthCode());
            results.put("releaseAmount", new BigDecimal(results.get("releaseAmount").toString()));
        } else {
            results.put("releaseAmount", BigDecimal.ZERO);
        }
        Debug.logInfo("processReleaseTransResult: " + results.toString(), MODULE);
    }

    private static void printTransResult(ResponseIF response) {
        Map<String, Object> generatedResponse = new HashMap<>();
        generatedResponse.put("isGood", response.isGood());
        generatedResponse.put("isError", response.isError());
        generatedResponse.put("isQuickResponse", response.isQuickResponse());
        generatedResponse.put("isApproved", response.isApproved());
        generatedResponse.put("isDeclined", response.isDeclined());
        generatedResponse.put("AuthCode", response.getAuthCode());
        generatedResponse.put("TxRefNum", response.getTxRefNum());
        generatedResponse.put("ResponseCode", response.getResponseCode());
        generatedResponse.put("Status", response.getStatus());
        generatedResponse.put("Message", response.getMessage());
        generatedResponse.put("AVSCode", response.getAVSResponseCode());
        generatedResponse.put("CVV2ResponseCode", response.getCVV2RespCode());

        Debug.logInfo("printTransResult === " + generatedResponse.toString(), MODULE);
    }

    private static String formatExpDateForOrbital(String expDate) {
        String formatedDate = expDate.substring(0, 2) + expDate.substring(5);
        return formatedDate;
    }

    private static String getShippingRefForOrder(String orderId, Delegator delegator) {
        String shippingRef = "";
        try {
            GenericValue trackingCodeOrder = EntityQuery.use(delegator).from("TrackingCodeOrder").where("orderId", orderId).queryFirst();
            GenericValue trackingCode = null;
            if (trackingCodeOrder != null) {
                trackingCode = trackingCodeOrder.getRelatedOne("TrackingCode", false);
            }
            if (trackingCode != null && UtilValidate.isNotEmpty(trackingCode.getString("description"))) {
                // get tracking code description and provide it into shipping reference.
                shippingRef = trackingCode.getString("trackingCodeId") + "====" + trackingCode.getString("description");
            } else {
                shippingRef = "No Tracking Info processed in order";
            }
        } catch (GenericEntityException e) {
            Debug.logError("Shipping Ref not found returning empty string", MODULE);
            Debug.logError(e, MODULE);
        }
        return shippingRef;
    }

    private static Map<String, Object> validateRequest(Map<String, Object> params, Map props, RequestIF request) {
        Map<String, Object> result = new HashMap<>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
