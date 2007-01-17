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
package org.ofbiz.accounting.thirdparty.clearcommerce;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;


/**
 * ClearCommerce Payment Services (CCE 5.4)
 */
public class CCPaymentServices {

    public final static String module = CCPaymentServices.class.getName();

    public static Map ccAuth(DispatchContext dctx, Map context) {
        String ccAction = (String) context.get("ccAction");
        if (ccAction == null) ccAction = new String("PreAuth");
        Document authRequestDoc = buildPrimaryTxRequest(context, ccAction, (Double) context.get("processAmount"),
                (String) context.get("orderId"));

        Document authResponseDoc = null;
        try {
            authResponseDoc = sendRequest(authRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(authResponseDoc) > 4) {  // 5 and higher, process error from HSBC
            Map result = ServiceUtil.returnSuccess();
            result.put("authResult", new Boolean(false));
            result.put("processAmount", new Double(0.00));
            result.put("authRefNum", getReferenceNum(authResponseDoc));
            List messages = getMessageList(authResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processAuthResponse(authResponseDoc);
    }

    public static Map ccCredit(DispatchContext dctx, Map context) {
        String action = new String("Credit");
        if (context.get("pbOrder") != null) {
            action = new String("Auth");  // required for periodic billing....
        }

        Document creditRequestDoc = buildPrimaryTxRequest(context, action, (Double) context.get("creditAmount"),
                (String) context.get("referenceCode"));
        Document creditResponseDoc = null;
        try {
            creditResponseDoc = sendRequest(creditRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(creditResponseDoc) > 4) {
            Map result = ServiceUtil.returnSuccess();
            result.put("creditResult", new Boolean(false));
            result.put("creditAmount", new Double(0.00));
            result.put("creditRefNum", getReferenceNum(creditResponseDoc));
            List messages = getMessageList(creditResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processCreditResponse(creditResponseDoc);
    }

    public static Map ccCapture(DispatchContext dctx, Map context) {

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found; cannot capture");
        }

        Document captureRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "PostAuth", (Double) context.get("captureAmount"));

        Document captureResponseDoc = null;
        try {
            captureResponseDoc = sendRequest(captureRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(captureResponseDoc) > 4) {
            Map result = ServiceUtil.returnSuccess();
            result.put("captureResult", new Boolean(false));
            result.put("captureAmount", new Double(0.00));
            result.put("captureRefNum", getReferenceNum(captureResponseDoc));
            List messages = getMessageList(captureResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processCaptureResponse(captureResponseDoc);
    }

    public static Map ccRelease(DispatchContext dctx, Map context) {

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found; cannot release");
        }

        Document releaseRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"), "Void", null);

        Document releaseResponseDoc = null;
        try {
            releaseResponseDoc = sendRequest(releaseRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(releaseResponseDoc) > 4) {
            Map result = ServiceUtil.returnSuccess();
            result.put("releaseResult", new Boolean(false));
            result.put("releaseAmount", new Double(0.00));
            result.put("releaseRefNum", getReferenceNum(releaseResponseDoc));
            List messages = getMessageList(releaseResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processReleaseResponse(releaseResponseDoc);
    }

    public static Map ccReleaseNoop(DispatchContext dctx, Map context) {

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found; cannot release");
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("releaseResult", Boolean.valueOf(true));
        result.put("releaseCode", authTransaction.getString("gatewayCode"));
        result.put("releaseAmount", authTransaction.getDouble("amount"));
        result.put("releaseRefNum", authTransaction.getString("referenceNum"));
        result.put("releaseFlag", authTransaction.getString("gatewayFlag"));
        result.put("releaseMessage", "Approved.");

        return result;
    }

    public static Map ccRefund(DispatchContext dctx, Map context) {

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found; cannot refund");
        }

        // Although refunds are applied to captured transactions, using the auth reference number is ok here
        // Related auth and capture transactions will always have the same reference number
        Document refundRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "Credit", (Double) context.get("refundAmount"));

        Document refundResponseDoc = null;
        try {
            refundResponseDoc = sendRequest(refundRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(refundResponseDoc) > 4) {
            Map result = ServiceUtil.returnSuccess();
            result.put("refundResult", new Boolean(false));
            result.put("refundAmount", new Double(0.00));
            result.put("refundRefNum", getReferenceNum(refundResponseDoc));
            List messages = getMessageList(refundResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processRefundResponse(refundResponseDoc);
    }

    public static Map ccReAuth(DispatchContext dctx, Map context) {

        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        if (authTransaction == null) {
            return ServiceUtil.returnError("No authorization transaction found; cannot re-auth.");
        }

        Document reauthRequestDoc = buildSecondaryTxRequest(context, authTransaction.getString("referenceNum"),
                "RePreAuth", (Double) context.get("reauthAmount"));

        Document reauthResponseDoc = null;
        try {
            reauthResponseDoc = sendRequest(reauthRequestDoc, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }

        if (getMessageListMaxSev(reauthResponseDoc) > 4) {
            Map result = ServiceUtil.returnSuccess();
            result.put("reauthResult", new Boolean(false));
            result.put("reauthAmount", new Double(0.00));
            result.put("reauthRefNum", getReferenceNum(reauthResponseDoc));
            List messages = getMessageList(reauthResponseDoc);
            if (UtilValidate.isNotEmpty(messages)) {
                result.put("internalRespMsgs", messages);
            }
            return result;
        }

        return processReAuthResponse(reauthResponseDoc);

    }
    
    public static Map ccReport(DispatchContext dctx, Map context) {
        
        // configuration file
        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }
        
        // orderId
        String orderId = (String) context.get("orderId");
        if (UtilValidate.isEmpty(orderId)) {
            return ServiceUtil.returnError("orderId is required......");
        }
        
        
        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument("EngineDocList");
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, "DocVersion", "1.0", requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, "EngineDoc", requestDocument);
        UtilXml.addChildElementValue(engineDocElement, "ContentType", "ReportDoc", requestDocument);

        String sourceId = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.sourceId");
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, "SourceId", sourceId, requestDocument);
        }

        String groupId = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.groupId");
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, "GroupId", groupId, requestDocument);
        }
        else
            UtilXml.addChildElementValue(engineDocElement, "GroupId", orderId, requestDocument);
            

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, "User", requestDocument);
        UtilXml.addChildElementValue(userElement, "Name",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.username", ""), requestDocument);
        UtilXml.addChildElementValue(userElement, "Password",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.password", ""), requestDocument);
        UtilXml.addChildElementValue(userElement, "Alias",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.alias", ""), requestDocument);

        String effectiveAlias = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.effectiveAlias");
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, "EffectiveAlias", effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, "Instructions", requestDocument);
        Element routingListDocElement = UtilXml.addChildElement(instructionsElement, "RoutingList", requestDocument);
        Element routingDocElement = UtilXml.addChildElement(routingListDocElement, "Routing", requestDocument);
        UtilXml.addChildElementValue(routingDocElement,"name","CcxReports", requestDocument);

        // EngineDocList.EngineDoc.ReportDoc
        Element reportDocElement = UtilXml.addChildElement(engineDocElement, "ReportDoc",requestDocument);
        Element compList = UtilXml.addChildElement(reportDocElement, "CompList",requestDocument);
        Element comp = UtilXml.addChildElement(compList, "Comp",requestDocument);
        UtilXml.addChildElementValue(comp,"Name","CcxReports",requestDocument);
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList
        Element actionList = UtilXml.addChildElement(comp, "ReportActionList",requestDocument);
        Element action = UtilXml.addChildElement(actionList, "ReportAction",requestDocument);
        UtilXml.addChildElementValue(action,"ReportName","CCE_OrderDetail",requestDocument);  
        Element start = UtilXml.addChildElementValue(action,"Start","1",requestDocument);
        start.setAttribute("DataType", "S32");
        Element count = UtilXml.addChildElementValue(action,"Count","10",requestDocument);
        count.setAttribute("DataType", "S32");
        // EngineDocList.EngineDoc.ReportDoc.ReportActionList.ReportAction.ValueList
        Element valueList = UtilXml.addChildElement(action, "ValueList",requestDocument);
        Element value = UtilXml.addChildElement(valueList, "Value",requestDocument);
        String clientIdConfig = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.clientId");
        if (UtilValidate.isNotEmpty(clientIdConfig)) {
            Element clientId = UtilXml.addChildElementValue(value,"ClientId", clientIdConfig, requestDocument);
            clientId.setAttribute("DataType", "S32");
        }
        UtilXml.addChildElementValue(value,"OrderId", orderId, requestDocument);
        
        Debug.set(Debug.VERBOSE, true);
        Document reportResponseDoc = null;
        try {
            reportResponseDoc = sendRequest(requestDocument, (String) context.get("paymentConfig"));
        } catch (ClearCommerceException cce) {
            return ServiceUtil.returnError(cce.getMessage());
        }
        Debug.set(Debug.VERBOSE, true);
    
        Map result = ServiceUtil.returnSuccess();
        
        return result;
    }


    private static Map processAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("authResult", Boolean.valueOf(true));
            result.put("authCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String authAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("processAmount", new Double(Double.parseDouble(authAmountStr) / 100));
        } else {
            result.put("authResult", Boolean.valueOf(false));
            result.put("processAmount", Double.valueOf("0.00"));
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

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map processCreditResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("creditResult", Boolean.valueOf(true));
            result.put("creditCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String creditAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("creditAmount", new Double(Double.parseDouble(creditAmountStr) / 100));
        } else {
            result.put("creditResult", Boolean.valueOf(false));
            result.put("creditAmount", Double.valueOf("0.00"));
        }

        result.put("creditRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("creditFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("creditMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map processCaptureResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("captureResult", Boolean.valueOf(true));
            result.put("captureCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String captureAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("captureAmount", new Double(Double.parseDouble(captureAmountStr) / 100));
        } else {
            result.put("captureResult", Boolean.valueOf(false));
            result.put("captureAmount", Double.valueOf("0.00"));
        }

        result.put("captureRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("captureFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("captureMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map processReleaseResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("releaseResult", Boolean.valueOf(true));
            result.put("releaseCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String releaseAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("releaseAmount", new Double(Double.parseDouble(releaseAmountStr) / 100));
        } else {
            result.put("releaseResult", Boolean.valueOf(false));
            result.put("releaseAmount", Double.valueOf("0.00"));
        }

        result.put("releaseRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("releaseFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("releaseMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map processRefundResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("refundResult", Boolean.valueOf(true));
            result.put("refundCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String refundAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("refundAmount", new Double(Double.parseDouble(refundAmountStr) / 100));
        } else {
            result.put("refundResult", Boolean.valueOf(false));
            result.put("refundAmount", Double.valueOf("0.00"));
        }

        result.put("refundRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("refundFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("refundMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static Map processReAuthResponse(Document responseDocument) {

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element orderFormElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        Element transactionElement = UtilXml.firstChildElement(orderFormElement, "Transaction");
        Element procResponseElement = UtilXml.firstChildElement(transactionElement, "CardProcResp");

        Map result = ServiceUtil.returnSuccess();

        String errorCode = UtilXml.childElementValue(procResponseElement, "CcErrCode");
        if ("1".equals(errorCode)) {
            result.put("reauthResult", Boolean.valueOf(true));
            result.put("reauthCode", UtilXml.childElementValue(transactionElement, "AuthCode"));

            Element currentTotalsElement = UtilXml.firstChildElement(transactionElement, "CurrentTotals");
            Element totalsElement = UtilXml.firstChildElement(currentTotalsElement, "Totals");
            String reauthAmountStr = UtilXml.childElementValue(totalsElement, "Total");
            result.put("reauthAmount", new Double(Double.parseDouble(reauthAmountStr) / 100));
        } else {
            result.put("reauthResult", Boolean.valueOf(false));
            result.put("reauthAmount", Double.valueOf("0.00"));
        }

        result.put("reauthRefNum", UtilXml.childElementValue(orderFormElement, "Id"));
        result.put("reauthFlag", UtilXml.childElementValue(procResponseElement, "Status"));
        result.put("reauthMessage", UtilXml.childElementValue(procResponseElement, "CcReturnMsg"));

        List messages = getMessageList(responseDocument);
        if (UtilValidate.isNotEmpty(messages)) {
            result.put("internalRespMsgs", messages);
        }
        return result;
    }

    private static List getMessageList(Document responseDocument) {

        List messageList = new ArrayList();

        Element engineDocElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "EngineDoc");
        Element messageListElement = UtilXml.firstChildElement(engineDocElement, "MessageList");
        List messageElementList = UtilXml.childElementList(messageListElement, "Message");
        if (UtilValidate.isNotEmpty(messageElementList)) {
            for (Iterator i = messageElementList.iterator(); i.hasNext();) {
                Element messageElement = (Element) i.next();
                int severity = 0;
                try {
                    severity = Integer.parseInt(UtilXml.childElementValue(messageElement, "Sev"));
                } catch (NumberFormatException nfe) {
                    Debug.logError("Error parsing message severity: " + nfe.getMessage(), module);
                    severity = 9;
                }
                String message = "[" + UtilXml.childElementValue(messageElement, "Audience") + "] " +
                        UtilXml.childElementValue(messageElement, "Text") + " (" + severity + ")";
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

    private static Document buildPrimaryTxRequest(Map context, String type, Double amount, String refNum) {

        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }

        Document requestDocument = createRequestDocument(paymentConfig);

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

        // payment mech
        GenericValue creditCard = (GenericValue) context.get("creditCard");

        boolean enableCVM = UtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, "payment.clearcommerce.enableCVM", "Y");
        String cardSecurityCode = enableCVM ? (String) context.get("cardSecurityCode") : null;

        // Default to locale code 840 (United States)
        String localCode = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.localeCode", "840");

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
        String currencyCode = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.currencyCode", "840");

        // transaction
        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        // TODO: determine if adding OrderItemList is worthwhile - JFE 2004.02.14

        Map pbOrder = (Map) context.get("pbOrder");
        if (pbOrder != null) {
            if (Debug.verboseOn()) Debug.logInfo("pbOrder Map not empty:" + pbOrder.toString(),module);
            Element pbOrderElement =  UtilXml.addChildElement(orderFormDocElement, "PbOrder", requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyCycle", (String) pbOrder.get("OrderFrequencyCycle"), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyInterval", (String) pbOrder.get("OrderFrequencyInterval"), requestDocument);
            interval.setAttribute("DataType", "S32");
            Element total = UtilXml.addChildElementValue(pbOrderElement, "TotalNumberPayments", (String) pbOrder.get("TotalNumberPayments"), requestDocument);
            total.setAttribute("DataType", "S32");
        }
        else if  (context.get("OrderFrequencyCycle") != null && context.get("OrderFrequencyInterval") != null && context.get("TotalNumberPayments") != null) {
            Element pbOrderElement =  UtilXml.addChildElement(orderFormDocElement, "PbOrder", requestDocument); // periodic billing order
            UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyCycle", (String) context.get("OrderFrequencyCycle"), requestDocument);
            Element interval = UtilXml.addChildElementValue(pbOrderElement, "OrderFrequencyInterval", (String) pbOrder.get("OrderFrequencyInterval"), requestDocument);
            interval.setAttribute("DataType", "S32");
            Element total = UtilXml.addChildElementValue(pbOrderElement, "TotalNumberPayments", (String) pbOrder.get("TotalNumberPayments"), requestDocument);
            total.setAttribute("DataType", "S32");
        }
        
        return requestDocument;
    }

    private static Document buildSecondaryTxRequest(Map context, String id, String type, Double amount) {

        String paymentConfig = (String) context.get("paymentConfig");
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }

        Document requestDocument = createRequestDocument(paymentConfig);

        Element engineDocElement = UtilXml.firstChildElement(requestDocument.getDocumentElement(), "EngineDoc");
        Element orderFormDocElement = UtilXml.firstChildElement(engineDocElement, "OrderFormDoc");
        UtilXml.addChildElementValue(orderFormDocElement, "Id", id, requestDocument);

        // Default to currency code 840 (USD)
        String currencyCode = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.currencyCode", "840");

        appendTransactionNode(orderFormDocElement, type, amount, currencyCode);

        return requestDocument;
    }

    private static void appendPaymentMechNode(Element element, GenericValue creditCard, String cardSecurityCode, String localeCode) {

        Document document = element.getOwnerDocument();

        Element paymentMechElement = UtilXml.addChildElement(element, "PaymentMech", document);
        Element creditCardElement = UtilXml.addChildElement(paymentMechElement, "CreditCard", document);

        UtilXml.addChildElementValue(creditCardElement, "Number", creditCard.getString("cardNumber"), document);

        String expDate = creditCard.getString("expireDate");
        Element expiresElement = UtilXml.addChildElementValue(creditCardElement, "Expires",
                expDate.substring(0, 3) + expDate.substring(5), document);
        expiresElement.setAttribute("DataType", "ExpirationDate");
        expiresElement.setAttribute("Locale", localeCode);

        if (UtilValidate.isNotEmpty(cardSecurityCode)) {
            // Cvv2Val must be exactly 4 characters
            if (cardSecurityCode.length() < 4) {
                while (cardSecurityCode.length() < 4) {
                    cardSecurityCode = cardSecurityCode + " ";
                }
            } else if (cardSecurityCode.length() > 4) {
                cardSecurityCode = cardSecurityCode.substring(0, 4);
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
                GenericValue countryGeo = address.getRelatedOneCache("CountryGeo");
                UtilXml.addChildElementValue(addressElement, "Country", countryGeo.getString("geoSecCode"), document);
            } catch (GenericEntityException gee) {
                Debug.log(gee, "Error finding related Geo for countryGeoId: " + countryGeoId, module);
            }
        }
    }

    private static void appendTransactionNode(Element element, String type, Double amount, String currencyCode) {

        Document document = element.getOwnerDocument();

        Element transactionElement = UtilXml.addChildElement(element, "Transaction", document);
        UtilXml.addChildElementValue(transactionElement, "Type", type, document);

        // Some transactions will not have an amount (release, reAuth)
        if (amount != null) {
            Element currentTotalsElement = UtilXml.addChildElement(transactionElement, "CurrentTotals", document);
            Element totalsElement = UtilXml.addChildElement(currentTotalsElement, "Totals", document);

            // DecimalFormat("#") is used here in case the total is something like 9.9999999...
            // in that case, we want to send 999, not 999.9999999...
            String totalString = new DecimalFormat("#").format(amount.doubleValue() * 100);

            Element totalElement = UtilXml.addChildElementValue(totalsElement, "Total", totalString, document);
            totalElement.setAttribute("DataType", "Money");
            totalElement.setAttribute("Currency", currencyCode);
        }
    }

    private static Document createRequestDocument(String paymentConfig) {

        // EngineDocList
        Document requestDocument = UtilXml.makeEmptyXmlDocument("EngineDocList");
        Element engineDocListElement = requestDocument.getDocumentElement();
        UtilXml.addChildElementValue(engineDocListElement, "DocVersion", "1.0", requestDocument);

        // EngineDocList.EngineDoc
        Element engineDocElement = UtilXml.addChildElement(engineDocListElement, "EngineDoc", requestDocument);
        UtilXml.addChildElementValue(engineDocElement, "ContentType", "OrderFormDoc", requestDocument);

        String sourceId = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.sourceId");
        if (UtilValidate.isNotEmpty(sourceId)) {
            UtilXml.addChildElementValue(engineDocElement, "SourceId", sourceId, requestDocument);
        }

        String groupId = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.groupId");
        if (UtilValidate.isNotEmpty(groupId)) {
            UtilXml.addChildElementValue(engineDocElement, "GroupId", groupId, requestDocument);
        }

        // EngineDocList.EngineDoc.User
        Element userElement = UtilXml.addChildElement(engineDocElement, "User", requestDocument);
        UtilXml.addChildElementValue(userElement, "Name",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.username", ""), requestDocument);
        UtilXml.addChildElementValue(userElement, "Password",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.password", ""), requestDocument);
        UtilXml.addChildElementValue(userElement, "Alias",
                UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.alias", ""), requestDocument);

        String effectiveAlias = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.effectiveAlias");
        if (UtilValidate.isNotEmpty(effectiveAlias)) {
            UtilXml.addChildElementValue(userElement, "EffectiveAlias", effectiveAlias, requestDocument);
        }

        // EngineDocList.EngineDoc.Instructions
        Element instructionsElement = UtilXml.addChildElement(engineDocElement, "Instructions", requestDocument);

        String pipeline = "PaymentNoFraud";
        if (UtilProperties.propertyValueEqualsIgnoreCase(paymentConfig, "payment.clearcommerce.enableFraudShield", "Y")) {
            pipeline = "Payment";
        }
        UtilXml.addChildElementValue(instructionsElement, "Pipeline", pipeline, requestDocument);

        // EngineDocList.EngineDoc.OrderFormDoc
        Element orderFormDocElement = UtilXml.addChildElement(engineDocElement, "OrderFormDoc", requestDocument);

        // default to "P" for Production Mode
        String mode = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.processMode", "P");
        UtilXml.addChildElementValue(orderFormDocElement, "Mode", mode, requestDocument);

        return requestDocument;
    }

    private static Document sendRequest(Document requestDocument, String paymentConfig) throws ClearCommerceException {
        if (UtilValidate.isEmpty(paymentConfig)) {
            paymentConfig = "payment.properties";
        }
        String serverURL = UtilProperties.getPropertyValue(paymentConfig, "payment.clearcommerce.serverURL");
        if (UtilValidate.isEmpty(serverURL)) {
            throw new ClearCommerceException("Missing server URL; check your ClearCommerce configuration");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("ClearCommerce server URL: " + serverURL, module);
        }

        OutputStream os = new ByteArrayOutputStream();

        OutputFormat format = new OutputFormat();
        format.setOmitDocumentType(true);
        format.setOmitXMLDeclaration(true);
        format.setIndenting(false);

        XMLSerializer serializer = new XMLSerializer(os, format);
        try {
            serializer.asDOMSerializer();
            serializer.serialize(requestDocument.getDocumentElement());
        } catch (IOException ioe) {
            throw new ClearCommerceException("Error serializing requestDocument: " + ioe.getMessage());
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
            Debug.log(hce, module);
            throw new ClearCommerceException("ClearCommerce connection problem", hce);
        }

        // Note: if Debug.verboseOn(), HttpClient will log this...set on with:         Debug.set(Debug.VERBOSE, true);
       // if (Debug.verboseOn()) {
       //    Debug.logVerbose("ClearCommerce response: " + response, module);
       // }

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(response, false);
        } catch (SAXException se) {
            throw new ClearCommerceException("Error reading response Document from a String: " + se.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new ClearCommerceException("Error reading response Document from a String: " + pce.getMessage());
        } catch (IOException ioe) {
            throw new ClearCommerceException("Error reading response Document from a String: " + ioe.getMessage());
        }
        if (Debug.verboseOn()) Debug.logVerbose("Result severity from clearCommerce:" + getMessageListMaxSev(responseDocument), module);
        if (Debug.verboseOn() && getMessageListMaxSev(responseDocument) > 4)
                Debug.logVerbose("Returned messages:" + getMessageList(responseDocument),module);
        return responseDocument;
    }

}

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

