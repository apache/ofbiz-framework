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
package org.apache.ofbiz.accounting.thirdparty.clearcommerce;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ClearCommerce Payment Services (CCE 5.4)
 */
public class CCPaymentServices {

    public final static String module = CCPaymentServices.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static RoundingMode rounding = UtilNumber.getRoundingMode("invoice.rounding");
    public final static String resource = "AccountingUiLabels";
    private final static int maxSevComp = 4;

    public static Map<String, Object> ccAuth(DispatchContext dctx, Map<String, Object> context) {
        String ccAction = (String) context.get("ccAction");
        Delegator delegator = dctx.getDelegator();
        if (ccAction == null) {
            ccAction = "PreAuth";
        }
        Document authRequestDoc = buildPrimaryTxRequest(context, ccAction, (BigDecimal) context.get("processAmount"),
                (String) context.get("orderId"));

        Document authResponseDoc = null;
        try {
            authResponseDoc = sendRequest(authRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(authResponseDoc) > maxSevComp) { // 5 and higher, process error from HSBC
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("authResult", Boolean.FALSE);
            result.put("processAmount", BigDecimal.ZERO);
            result.put("authRefNum", getReferenceNum(authResponseDoc));
            List<String> messages = getMessageList(authResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processAuthResponse(authResponseDoc);
    }

    public static Map<String, Object> ccCredit(DispatchContext dctx, Map<String, Object> context) {
        String action = "Credit";
        Delegator delegator = dctx.getDelegator();
        if (context.get("pbOrder") != null) {
            action = "Auth"; // required for periodic billing....
        }

        Document creditRequestDoc = buildPrimaryTxRequest(context, action, (BigDecimal) context.get("creditAmount"),
                (String) context.get("referenceCode"));
        Document creditResponseDoc = null;
        try {
            creditResponseDoc = sendRequest(creditRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(creditResponseDoc) > maxSevComp) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("creditResult", Boolean.FALSE);
            result.put("creditAmount", BigDecimal.ZERO);
            result.put("creditRefNum", getReferenceNum(creditResponseDoc));
            List<String> messages = getMessageList(creditResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processCreditResponse(creditResponseDoc);
    }

    public static Map<String, Object> ccCapture(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotCapture", locale));
        }

        Document captureRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "PostAuth", (BigDecimal) context.get("captureAmount"), delegator);

        Document captureResponseDoc = null;
        try {
            captureResponseDoc = sendRequest(captureRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(captureResponseDoc) > maxSevComp) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("captureResult", Boolean.FALSE);
            result.put("captureAmount", BigDecimal.ZERO);
            result.put("captureRefNum", getReferenceNum(captureResponseDoc));
            List<String> messages = getMessageList(captureResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processCaptureResponse(captureResponseDoc);
    }

    public static Map<String, Object> ccRelease(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }

        Document releaseRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"), "Void",
                null, delegator);

        Document releaseResponseDoc = null;
        try {
            releaseResponseDoc = sendRequest(releaseRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(releaseResponseDoc) > maxSevComp) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("releaseResult", Boolean.FALSE);
            result.put("releaseAmount", BigDecimal.ZERO);
            result.put("releaseRefNum", getReferenceNum(releaseResponseDoc));
            List<String> messages = getMessageList(releaseResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processReleaseResponse(releaseResponseDoc);
    }

    public static Map<String, Object> ccReleaseNoop(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRelease", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("releaseResult", Boolean.TRUE);
        result.put("releaseCode", authTransaction.getString("gatewayCode"));
        result.put("releaseAmount", authTransaction.getBigDecimal("amount"));
        result.put("releaseRefNum", authTransaction.getString("referenceNum"));
        result.put("releaseFlag", authTransaction.getString("gatewayFlag"));
        result.put("releaseMessage", "Approved.");

        return result;
    }

    public static Map<String, Object> ccRefund(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotRefund", locale));
        }

        // Although refunds are applied to captured transactions, using the auth reference number is ok here
        // Related auth and capture transactions will always have the same reference number
        Document refundRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "Credit", (BigDecimal) context.get("refundAmount"), delegator);

        Document refundResponseDoc = null;
        try {
            refundResponseDoc = sendRequest(refundRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(refundResponseDoc) > maxSevComp) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("refundResult", Boolean.FALSE);
            result.put("refundAmount", BigDecimal.ZERO);
            result.put("refundRefNum", getReferenceNum(refundResponseDoc));
            List<String> messages = getMessageList(refundResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processRefundResponse(refundResponseDoc);
    }

    public static Map<String, Object> ccReAuth(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingPaymentTransactionAuthorizationNotFoundCannotReauth", locale));
        }

        Document reauthRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "RePreAuth", (BigDecimal) context.get("reauthAmount"), delegator);

        Document reauthResponseDoc = null;
        try {
            reauthResponseDoc = sendRequest(reauthRequestDoc, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(reauthResponseDoc) > maxSevComp) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("reauthResult", Boolean.FALSE);
            result.put("reauthAmount", BigDecimal.ZERO);
            result.put("reauthRefNum", getReferenceNum(reauthResponseDoc));
            List<String> messages = getMessageList(reauthResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processReAuthResponse(reauthResponseDoc);

    }

    public static Map<String, Object> ccReport(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        // configuration file
        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }

        // orderId
        String orderId = (String) context.get("orderId");
        if (UtilValidate.isEmpty(orderId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "AccountingClearCommerceCannotExecuteReport", locale));
        }

        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument("EngineDocList");
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, "DocVersion", "1.0", requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, "EngineDoc", requestDocument);
        UtilXml.addChildElementValue(engineDocElement, "ContentType", "ReportDoc", requestDocument);

        String sourceId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.sourceId", delegator);
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, "SourceId", sourceId, requestDocument);
        }

        String groupId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.groupId", delegator);
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, "GroupId", groupId, requestDocument);
        } else {
            UtilXml.addChildElementValue(engineDocElement, "GroupId", orderId, requestDocument);
        }

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, "User", requestDocument);
        UtilXml.addChildElementValue(userElement, "Name",
                EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.username", "", delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, "Password",
                EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.password", "", delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, "Alias",
                EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.alias", "", delegator), requestDocument);

        String effectiveAlias = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.effectiveAlias", delegator);
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, "EffectiveAlias", effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, "Instructions", requestDocument);
        Element routingListDocElement = UtilXml.addChildElement(instructionsElement, "RoutingList", requestDocument);
        Element routingDocElement = UtilXml.addChildElement(routingListDocElement, "Routing", requestDocument);
        UtilXml.addChildElementValue(routingDocElement, "name", "CcxReports", requestDocument);

        // EngineDocList.EngineDoc.ReportDoc
        Element reportDocElement = UtilXml.addChildElement(engineDocElement, "ReportDoc", requestDocument);
        Element compList = UtilXml.addChildElement(reportDocElement, "CompList", requestDocument);
        Element comp = UtilXml.addChildElement(compList, "Comp", requestDocument);
        UtilXml.addChildElementValue(comp, "Name", "CcxReports", requestDocument);
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList
        Element actionList = UtilXml.addChildElement(comp, "ReportActionList", requestDocument);
        Element action = UtilXml.addChildElement(actionList, "ReportAction", requestDocument);
        UtilXml.addChildElementValue(action, "ReportName", "CCE_OrderDetail", requestDocument);
        Element start = UtilXml.addChildElementValue(action, "Start", "1", requestDocument);
        start.setAttribute("DataType", "S32");
        Element count = UtilXml.addChildElementValue(action, "Count", "10", requestDocument);
        count.setAttribute("DataType", "S32");
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList.ReportAction.ValueList
        Element valueList = UtilXml.addChildElement(action, "ValueList", requestDocument);
        Element value = UtilXml.addChildElement(valueList, "Value", requestDocument);
        String clientIdConfig = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.clientId", delegator);
        if (UtilValidate.isNotEmpty(clientIdConfig)) {
            Element clientId = UtilXml.addChildElementValue(value, "ClientId", clientIdConfig, requestDocument);
            clientId.setAttribute("DataType", "S32");
        }
        UtilXml.addChildElementValue(value, "OrderId", orderId, requestDocument);

        Debug.set(Debug.VERBOSE, true);
        // Document reportResponseDoc = null;
        try {
            sendRequest(requestDocument, (String) context.get("paymentConfig"), delegator);
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }
        Debug.set(Debug.VERBOSE, true);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        return result;
    }

    private static Map<String, Object> processAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("authResult", Boolean.TRUE);
            result.put("authCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String authAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("processAmount", new BigDecimal(authAmountStr).movePointLeft(2));
        } else {
            result.put("authResult", Boolean.FALSE);
            result.put("processAmount", BigDecimal.ZERO);
        }

        result.put("authRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("authFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("authMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        // AVS
        String avsCode = UtilXml.childElementValue(procResponseElement, "AvsDisplay");
        if (UtilValidate.isNotEmpty(avsCode)) {
            result.put("avsCode", avsCode);
        }

        // Fraud score
        Element fraudInfoElement = UtilXml.firstChildElement(orderFormElement, "FraudInfo");
        if (fraudInfoElement != null) {
            result.put("scoreCode", UtilXml.childElementValue(fraudInfoElement, "TotalScore"));
        }

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map<String, Object> processCreditResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("creditResult", Boolean.TRUE);
            result.put("creditCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String creditAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("creditAmount", new BigDecimal(creditAmountStr).movePointLeft(2));
        } else {
            result.put("creditResult", Boolean.FALSE);
            result.put("creditAmount", BigDecimal.ZERO);
        }

        result.put("creditRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("creditFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("creditMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map<String, Object> processCaptureResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("captureResult", Boolean.TRUE);
            result.put("captureCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String captureAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("captureAmount", new BigDecimal(captureAmountStr).movePointLeft(2));
        } else {
            result.put("captureResult", Boolean.FALSE);
            result.put("captureAmount", BigDecimal.ZERO);
        }

        result.put("captureRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("captureFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("captureMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map<String, Object> processReleaseResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("releaseResult", Boolean.TRUE);
            result.put("releaseCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String releaseAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("releaseAmount", new BigDecimal(releaseAmountStr).movePointLeft(2));
        } else {
            result.put("releaseResult", Boolean.FALSE);
            result.put("releaseAmount", BigDecimal.ZERO);
        }

        result.put("releaseRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("releaseFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("releaseMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map<String, Object> processRefundResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("refundResult", Boolean.TRUE);
            result.put("refundCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String refundAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("refundAmount", new BigDecimal(refundAmountStr).movePointLeft(2));
        } else {
            result.put("refundResult", Boolean.FALSE);
            result.put("refundAmount", BigDecimal.ZERO);
        }

        result.put("refundRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("refundFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("refundMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map<String, Object> processReAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("reauthResult", Boolean.TRUE);
            result.put("reauthCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String reauthAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("reauthAmount", new BigDecimal(reauthAmountStr).movePointLeft(2));
        } else {
            result.put("reauthResult", Boolean.FALSE);
            result.put("reauthAmount", BigDecimal.ZERO);
        }

        result.put("reauthRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("reauthFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("reauthMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List<String> messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static List<String> getMessageList(Document responseDocument) {

        List<String> messageList = new ArrayList<>();

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element messageListElement = UtilXml.firstChildElement(engineDocElement, "MessageList");
        List<? extends Element> messageElementList = UtilXml.childElementList(messageListElement, "Message");
        if (UtilValidate.isNotEmpty(messageElementList)) {
            for (Element messageElement : messageElementList) {
                int severity = 0;
                try {
                    severity = Integer.parseInt(UtilXml.childElementValue(messageElement, "Sev"));
                } catch (NumberFormatException nfe) {
                    Debug.logError("Error parsing message severity: " + nfe.getMessage(), module);
                    severity = 9;
                }
                String message = "[" + UtilXml.childElementValue(messageElement, "Audience") + "] " + UtilXml
                        .childElementValue(messageElement, "Text") + " (" + severity + ")";
                messageList.add(message);
            }
        }

        return messageList;
    }

    private static int getMessageListMaxSev(Document responseDocument) {

        int maxSev = 0;

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element messageListElement = UtilXml.firstChildElement(engineDocElement, "MessageList");
        String maxSevStr = UtilXml.childElementValue(messageListElement, "MaxSev");
        if (UtilValidate.isNotEmpty(maxSevStr)) {
            try {
                maxSev = Integer.parseInt(maxSevStr);
            } catch (NumberFormatException nfe) {
                Debug.logError("Error parsing MaxSev: " + nfe.getMessage(), module);
                maxSev = 9;
            }
        }
        return maxSev;
    }

    private static String getReferenceNum(Document responseDocument) {
        String referenceNum = null;
        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        if (engineDocElement != null) {
            Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
            if (orderFormElement != null) {
                referenceNum = UtilXml.childElementValue(orderFormElement, "Id");
            }
        }
        return referenceNum;
    }

    private static Document buildPrimaryTxRequest(Map<String, Object> context, String type, BigDecimal amount, String refNum) {

        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }
        // payment mech
        GenericValue creditCard = (GenericValue) context.get("creditCard");
        Delegator delegator = creditCard.getDelegator();
        Document requestDocument = createRequestDocument(paymentConfig, delegator);

        Element engineDocElement = UtilXml.firstChildElement(requestDocument.getDocumentElement(), "EngineDoc");
        Element orderFormDocElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");

        // add the reference number as a comment
        UtilXml.addChildElementValue(orderFormDocElement, "Comments", refNum, requestDocument);

        Element consumerElement = UtilXml.addChildElement(orderFormDocElement, "Consumer", requestDocument);

        // email address
        GenericValue billToEmail = (GenericValue) context.get("billToEmail");
        if (billToEmail != null) {
            UtilXml.addChildElementValue(consumerElement, "Email", billToEmail.getString("infoString"), requestDocument);
        }

        boolean enableCVM = EntityUtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, "payment.clearcommerce.enableCVM", "Y", delegator);
        String cardSecurityCode = enableCVM ? (String) context.get("cardSecurityCode") : null;

        // Default to locale code 840 (United States)
        String localCode = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.localeCode", "840", delegator);

        appendPaymentMechNode(consumerElement, creditCard, cardSecurityCode, localCode);

        // billing address
        GenericValue billingAddress = (GenericValue) context.get("billingAddress");
        if (billingAddress != null) {
            Element billToElement = UtilXml.addChildElement(consumerElement, "BillTo", requestDocument);
            Element billToLocationElement = UtilXml.addChildElement(billToElement, "Location", requestDocument);
            appendAddressNode(billToLocationElement, billingAddress);
        }

        // shipping address
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");
        if (shippingAddress != null) {
            Element shipToElement = UtilXml.addChildElement(consumerElement, "ShipTo", requestDocument);
            Element shipToLocationElement = UtilXml.addChildElement(shipToElement, "Location", requestDocument);
            appendAddressNode(shipToLocationElement, shippingAddress);
        }

        // Default to currency code 840 (USD)
        String currencyCode = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.currencyCode", "840", delegator);

        // transaction
        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        // TODO: determine if adding OrderItemList is worthwhile - JFE 2004.02.14

        Map<String, Object> pbOrder = UtilGenerics.cast(context.get("pbOrder"));
        if (pbOrder != null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("pbOrder Map not empty:" + pbOrder.toString(), module);
            }
            Element pbOrderElement = UtilXml.addChildElement(orderFormDocElement, "PbOrder", requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyCycle", (String) pbOrder.get(
                    "OrderFrequencyCycle"), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyInterval", (String) pbOrder
                    .get("OrderFrequencyInterval"), requestDocument);
            interval.setAttribute("DataType", "S32");
            Element total = UtilXml.addChildElementValue(pbOrderElement, "TotalNumberPayments", (String) pbOrder.get(
                    "TotalNumberPayments"), requestDocument);
            total.setAttribute("DataType", "S32");
        } else if (context.get("OrderFrequencyCycle") != null && context.get("OrderFrequencyInterval") != null
                && context.get("TotalNumberPayments") != null) {
            Element pbOrderElement = UtilXml.addChildElement(orderFormDocElement, "PbOrder", requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyCycle", (String) context.get(
                    "OrderFrequencyCycle"), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyInterval", (String) context
                    .get("OrderFrequencyInterval"), requestDocument);
            interval.setAttribute("DataType", "S32");
            Element total = UtilXml.addChildElementValue(pbOrderElement, "TotalNumberPayments", (String) context.get(
                    "TotalNumberPayments"), requestDocument);
            total.setAttribute("DataType", "S32");
        }

        return requestDocument;
    }

    private static Document buildSecondaryTxRequest(Map<String, Object> context, String id, String type, BigDecimal amount, Delegator delegator) {

        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }

        Document requestDocument = createRequestDocument(paymentConfig, delegator);

        Element engineDocElement = UtilXml.firstChildElement(requestDocument.getDocumentElement(), "EngineDoc");
        Element orderFormDocElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        UtilXml.addChildElementValue(orderFormDocElement, "Id", id, requestDocument);

        // Default to currency code 840 (USD)
        String currencyCode = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.currencyCode", "840", delegator);

        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        return requestDocument;
    }

    private static void appendPaymentMechNode(Element element, GenericValue creditCard, String cardSecurityCode, String localeCode) {

        final int securityCodeLength = 4;
        Document document = element.getOwnerDocument();

        Element paymentMechElement = UtilXml.addChildElement(element, "PaymentMech", document);
        Element creditCardElement = UtilXml.addChildElement(paymentMechElement, "CreditCard", document);

        UtilXml.addChildElementValue(creditCardElement, "Number", creditCard.getString("cardNumber"), document);

        String expDate = creditCard.getString("expireDate");
        Element expiresElement = UtilXml.addChildElementValue(creditCardElement, "Expires", expDate.substring(0, 3)
                + expDate.substring(5), document);
        expiresElement.setAttribute("DataType", "ExpirationDate");
        expiresElement.setAttribute("Locale", localeCode);

        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            // Cvv2Val must be exactly securityCodeLength characters
            if (cardSecurityCode.length() < securityCodeLength) {
                // space padding on right side of cardSecurityCode
                cardSecurityCode = String.format("%-" + securityCodeLength + "s", cardSecurityCode);

            } else if (cardSecurityCode.length() > securityCodeLength) {
                cardSecurityCode = cardSecurityCode.substring(0, securityCodeLength);
            }
            UtilXml.addChildElementValue(creditCardElement, "Cvv2Val", cardSecurityCode, document);
            UtilXml.addChildElementValue(creditCardElement, "Cvv2Indicator", "1", document);
        }
    }

    private static void appendAddressNode(Element element, GenericValue address) {

        Document document = element.getOwnerDocument();

        Element addressElement = UtilXml.addChildElement(element, "Address", document);

        UtilXml.addChildElementValue(addressElement, "Name", address.getString("toName"), document);
        UtilXml.addChildElementValue(addressElement, "Street1", address.getString("address1"), document);
        UtilXml.addChildElementValue(addressElement, "Street2", address.getString("address2"), document);
        UtilXml.addChildElementValue(addressElement, "City", address.getString("city"), document);
        UtilXml.addChildElementValue(addressElement, "StateProv", address.getString("stateProvinceGeoId"), document);
        UtilXml.addChildElementValue(addressElement, "PostalCode", address.getString("postalCode"), document);

        String countryGeoId = address.getString("countryGeoId");
        if (UtilValidate.isNotEmpty(countryGeoId)) {
            try {
                GenericValue countryGeo = address.getRelatedOne("CountryGeo", true);
                UtilXml.addChildElementValue(addressElement, "Country", countryGeo.getString("geoSecCode"), document);
            } catch (GenericEntityException gee) {
                Debug.logInfo(gee, "Error finding related Geo for countryGeoId: " + countryGeoId, module);
            }
        }
    }

    private static void appendTransactionNode(Element element, String type, BigDecimal amount, String currencyCode) {

        Document document = element.getOwnerDocument();

        Element transactionElement = UtilXml.addChildElement(element, "Transaction", document);
        UtilXml.addChildElementValue(transactionElement, "Type", type, document);

        // Some transactions will not have an amount (release, reAuth)
        if (amount != null) {
            Element currentTotalsElement = UtilXml.addChildElement(transactionElement, "CurrentTotals", document);
            Element totalsElement = UtilXml.addChildElement(currentTotalsElement, "Totals", document);

            // DecimalFormat("#") is used here in case the total is something like 9.9999999...
            // in that case, we want to send 999, not 999.9999999...
            String totalString = amount.setScale(decimals, rounding).movePointRight(2).toPlainString();

            Element totalElement = UtilXml.addChildElementValue(totalsElement, "Total", totalString, document);
            totalElement.setAttribute("DataType", "Money");
            totalElement.setAttribute("Currency", currencyCode);
        }
    }

    private static Document createRequestDocument(String paymentConfig, Delegator delegator) {

        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument("EngineDocList");
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, "DocVersion", "1.0", requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, "EngineDoc", requestDocument);
        UtilXml.addChildElementValue(engineDocElement, "ContentType", "OrderFormDoc", requestDocument);

        String sourceId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.sourceId", delegator);
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, "SourceId", sourceId, requestDocument);
        }

        String groupId = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.groupId", delegator);
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, "GroupId", groupId, requestDocument);
        }

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, "User", requestDocument);
        UtilXml.addChildElementValue(userElement, "Name", EntityUtilProperties.getPropertyValue(paymentConfig,
                "payment.clearcommerce.username", "", delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, "Password", EntityUtilProperties.getPropertyValue(paymentConfig,
                "payment.clearcommerce.password", "", delegator), requestDocument);
        UtilXml.addChildElementValue(userElement, "Alias", EntityUtilProperties.getPropertyValue(paymentConfig,
                "payment.clearcommerce.alias", "", delegator), requestDocument);

        String effectiveAlias = EntityUtilProperties.getPropertyValue(paymentConfig,
                "payment.clearcommerce.effectiveAlias", delegator);
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, "EffectiveAlias", effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, "Instructions", requestDocument);

        String pipeline = "PaymentNoFraud";
        if (EntityUtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, "payment.clearcommerce.enableFraudShield", "Y", delegator)) {
            pipeline = "Payment";
        }
        UtilXml.addChildElementValue(instructionsElement, "Pipeline", pipeline, requestDocument);

        // EngineDocList.EngineDoc.OrderFormDoc
        Element orderFormDocElement = UtilXml.addChildElement(engineDocElement, "OrderFormDoc", requestDocument);

        // default to "P" for Production Mode
        String mode = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.processMode", "P", delegator);
        UtilXml.addChildElementValue(orderFormDocElement, "Mode", mode, requestDocument);

        return requestDocument;
    }

    private static Document sendRequest(Document requestDocument, String paymentConfig, Delegator delegator) throws ClearCommerceException {
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }
        String serverURL = EntityUtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.serverURL", delegator);
        if (UtilValidate.isEmpty(serverURL)) {
            throw new ClearCommerceException("Missing server URL; check your ClearCommerce configuration");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("ClearCommerce server URL: " + serverURL, module);
        }

        OutputStream os = new ByteArrayOutputStream();

        try {
            UtilXml.writeXmlDocument(requestDocument, os, "UTF-8", true, false, 0);
        } catch (TransformerException e) {
            throw new ClearCommerceException("Error serializing requestDocument: " + e.getMessage());
        }

        String xmlString = os.toString();

        if (Debug.verboseOn()) {
            Debug.logVerbose("ClearCommerce XML request string: " + xmlString, module);
        }

        HttpClient http = new HttpClient(serverURL);
        http.setParameter("CLRCMRC_XML", xmlString);

        String response = null;
        try {
            response = http.post();
        } catch (HttpClientException hce) {
            Debug.logInfo(hce, module);
            throw new ClearCommerceException("ClearCommerce connection problem", hce);
        }

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(response, false);
        } catch (Exception e) {
            throw new ClearCommerceException("Error reading response Document from a String: " + e.getMessage());
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Result severity from clearCommerce:" + getMessageListMaxSev(responseDocument), module);
        }
        if (Debug.verboseOn() && getMessageListMaxSev(responseDocument) > maxSevComp) {
            Debug.logVerbose("Returned messages:" + getMessageList(responseDocument), module);
        }
        return responseDocument;
    }

}

@SuppressWarnings("serial")
class ClearCommerceException extends GeneralException {

    ClearCommerceException() {
        super();
    }

    ClearCommerceException(String msg) {
        super(msg);
    }

    ClearCommerceException(Throwable t) {
        super(t);
    }

    ClearCommerceException(String msg, Throwable t) {
        super(msg, t);
    }
}
