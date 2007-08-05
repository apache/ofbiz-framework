/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.ofbiz.oagis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class OagisServices {
    
    public static final String module = OagisServices.class.getName();
    
    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static final String resource = "OagisUiLabels";

    public static final String certAlias = UtilProperties.getPropertyValue("oagis.properties", "auth.client.certificate.alias");
    public static final String basicAuthUsername = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.username");
    public static final String basicAuthPassword = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.password");

    public static final boolean debugSaveXmlOut = "true".equals(UtilProperties.getPropertyValue("oagis.properties", "Oagis.Debug.Save.Xml.Out"));
    public static final boolean debugSaveXmlIn = "true".equals(UtilProperties.getPropertyValue("oagis.properties", "Oagis.Debug.Save.Xml.In"));

    public static Map oagisSendConfirmBod(DispatchContext ctx, Map context) {
        
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        
        String errorReferenceId = (String) context.get("referenceId");

        String sendToUrl = (String) context.get("sendToUrl");
        if (UtilValidate.isEmpty(sendToUrl)) {
            sendToUrl = UtilProperties.getPropertyValue("oagis.properties", "url.send.confirmBod");
        }

        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.filename.base", "");
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ConfirmBod" + errorReferenceId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.directory");
        }

        OutputStream out = (OutputStream) context.get("outputStream");
        
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting userLogin", module);
        }
        
        String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
        String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
        
        MapStack bodyParameters =  MapStack.create();
        bodyParameters.put("logicalId", logicalId);
        bodyParameters.put("authId", authId);

        String referenceId = delegator.getNextSeqId("OagisMessageInfo");
        bodyParameters.put("referenceId", referenceId);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
        Timestamp timestamp = UtilDateTime.nowTimestamp();
        String sentDate = dateFormat.format(timestamp);
        bodyParameters.put("sentDate", sentDate);
        
        bodyParameters.put("errorLogicalId", context.get("logicalId"));
        bodyParameters.put("errorComponent", context.get("component"));
        bodyParameters.put("errorTask", context.get("task"));
        bodyParameters.put("errorReferenceId", errorReferenceId);
        bodyParameters.put("errorMapList",(List) context.get("errorMapList"));
        bodyParameters.put("origRef", context.get("origRefId"));
        String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ConfirmBod");
        
        String outText = null;
        try {
            Writer writer = new StringWriter();
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, new HtmlScreenRenderer());
            screens.render(bodyScreenUri);
            writer.close();
            outText = writer.toString();
        } catch (Exception e) {
            String errMsg = "Error rendering message: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        // TODO: call service with require-new-transaction=true to save the OagisMessageInfo data (to make sure it saves before)
        Map oagisMsgInfoContext = new HashMap();
        oagisMsgInfoContext.put("logicalId", logicalId);
        oagisMsgInfoContext.put("component", "EXCEPTION");
        oagisMsgInfoContext.put("task", "RECIEPT");
        oagisMsgInfoContext.put("referenceId", referenceId);
        oagisMsgInfoContext.put("authId", authId);
        oagisMsgInfoContext.put("sentDate", timestamp);
        oagisMsgInfoContext.put("confirmation", "0");
        oagisMsgInfoContext.put("bsrVerb", "CONFIRM");
        oagisMsgInfoContext.put("bsrNoun", "BOD");
        oagisMsgInfoContext.put("bsrRevision", "004");
        oagisMsgInfoContext.put("userLogin", userLogin);
        if (OagisServices.debugSaveXmlOut) {
            oagisMsgInfoContext.put("fullMessageXml", outText);
        }
        try {
            dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoContext, 60, true);
            /* running async for better error handling
            if (ServiceUtil.isError(oagisMsgInfoResult)) return ServiceUtil.returnError("Error creating OagisMessageInfo");
            */
            
        } catch (GenericServiceException e) {
            Debug.logError(e, "Saving message to database failed", module);
        }

        Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);
        if (sendMessageReturn != null) {
            return sendMessageReturn;
        }
        
        return ServiceUtil.returnSuccess("Service Completed Successfully");
    }

    public static Map receiveConfirmBod(DispatchContext ctx, Map context) {
        
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Document doc = (Document) context.get("document");
        List errorMapList = FastList.newInstance();
        
        GenericValue userLogin = null; 
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin",UtilMisc.toMap("userLoginId","admin"));
        } catch (GenericEntityException e){
            String errMsg = "Error Getting UserLogin with userLoginId 'admin':"+e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        Element confirmBodElement = doc.getDocumentElement();
        confirmBodElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(confirmBodElement, "os:CNTROLAREA");
        Element bsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:BSR");
        String bsrVerb = UtilXml.childElementValue(bsrElement, "of:VERB");
        String bsrNoun = UtilXml.childElementValue(bsrElement, "of:NOUN");
        String bsrRevision = UtilXml.childElementValue(bsrElement, "of:REVISION");
            
        Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:SENDER");
        String logicalId = UtilXml.childElementValue(docSenderElement, "of:LOGICALID");
        String component = UtilXml.childElementValue(docSenderElement, "of:COMPONENT");
        String task = UtilXml.childElementValue(docSenderElement, "of:TASK");
        String referenceId = UtilXml.childElementValue(docSenderElement, "of:REFERENCEID");
        String confirmation = UtilXml.childElementValue(docSenderElement, "of:CONFIRMATION");
        //String language = UtilXml.childElementValue(docSenderElement, "of:LANGUAGE");
        //String codepage = UtilXml.childElementValue(docSenderElement, "of:CODEPAGE");
        String authId = UtilXml.childElementValue(docSenderElement, "of:AUTHID");
        String sentDate = UtilXml.childElementValue(docCtrlAreaElement, "os:DATETIMEANY");
          
        Element dataAreaElement = UtilXml.firstChildElement(confirmBodElement, "n:DATAAREA");
        Element dataAreaConfirmBodElement = UtilXml.firstChildElement(dataAreaElement, "n:CONFIRM_BOD");
        Element dataAreaConfirmElement = UtilXml.firstChildElement(dataAreaConfirmBodElement, "n:CONFIRM");
        Element dataAreaCtrlElement = UtilXml.firstChildElement(dataAreaConfirmElement, "os:CNTROLAREA");
        Element dataAreaSenderElement = UtilXml.firstChildElement(dataAreaCtrlElement, "os:SENDER");
        String dataAreaLogicalId = UtilXml.childElementValue(dataAreaSenderElement, "of:LOGICALID");
        String dataAreaComponent = UtilXml.childElementValue(dataAreaSenderElement, "of:COMPONENT");
        String dataAreaTask = UtilXml.childElementValue(dataAreaSenderElement, "of:TASK");
        String dataAreaReferenceId = UtilXml.childElementValue(dataAreaSenderElement, "of:REFERENCEID");
        String dataAreaDate = UtilXml.childElementValue(dataAreaCtrlElement, "os:DATETIMEANY");
        String origRef = UtilXml.childElementValue(dataAreaConfirmElement, "of:ORIGREF");
          
        Timestamp timestamp = UtilDateTime.nowTimestamp();
        Map oagisMsgInfoCtx = new HashMap();
        oagisMsgInfoCtx.put("logicalId", logicalId);
        oagisMsgInfoCtx.put("component", component);
        oagisMsgInfoCtx.put("task", task);
        oagisMsgInfoCtx.put("referenceId", referenceId);
        oagisMsgInfoCtx.put("authId", authId);
        oagisMsgInfoCtx.put("receivedDate", timestamp);
        oagisMsgInfoCtx.put("confirmation", confirmation);
        oagisMsgInfoCtx.put("bsrVerb", bsrVerb);
        oagisMsgInfoCtx.put("bsrNoun", bsrNoun);
        oagisMsgInfoCtx.put("bsrRevision", bsrRevision);
        oagisMsgInfoCtx.put("outgoingMessage", "N");
        oagisMsgInfoCtx.put("userLogin", userLogin);
        try {
            dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            /* running async for better error handling
            if (ServiceUtil.isError(oagisMsgInfoResult)){
                String errMsg = "Error creating OagisMessageInfo for the Incoming Message: "+ServiceUtil.getErrorMessage(oagisMsgInfoResult);
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */
        } catch (GenericServiceException e){
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: "+e.toString();
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            Debug.logError(e, errMsg, module);
        }

        Map oagisMsgErrorCtx = new HashMap();
        oagisMsgErrorCtx.put("logicalId", dataAreaLogicalId);
        oagisMsgErrorCtx.put("component", dataAreaComponent);
        oagisMsgErrorCtx.put("task", dataAreaTask);
        oagisMsgErrorCtx.put("referenceId", dataAreaReferenceId);
          
        GenericValue oagisMsgInfo = null;
        try {
            oagisMsgInfo = delegator.findByPrimaryKey("OagisMessageInfo", oagisMsgErrorCtx);
        } catch (GenericEntityException e){
            String errMsg = "Error Getting Entity OagisMessageInfo: "+e.toString();
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
            Debug.logError(e, errMsg, module);
        }
        
        oagisMsgErrorCtx.put("userLogin", userLogin);
        
        List dataAreaConfirmMsgList = UtilXml.childElementList(dataAreaConfirmElement, "n:CONFIRMMSG");
        Iterator dataAreaConfirmMsgListItr = dataAreaConfirmMsgList.iterator();
        
        if (oagisMsgInfo != null){
            while (dataAreaConfirmMsgListItr.hasNext()){
                Element dataAreaConfirmMsgElement = (Element) dataAreaConfirmMsgListItr.next();
                String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:DESCRIPTN");
                String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:REASONCODE");
                oagisMsgErrorCtx.put("reasonCode", reasonCode);
                oagisMsgErrorCtx.put("description", description);
            
                try {
                    Map oagisMsgErrorInfoResult = dispatcher.runSync("createOagisMessageErrorInfo", oagisMsgErrorCtx);
                    if (ServiceUtil.isError(oagisMsgErrorInfoResult)){
                        String errMsg = "Error creating OagisMessageErrorInfo: "+ServiceUtil.getErrorMessage(oagisMsgErrorInfoResult);
                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageErrorInfoServiceError"));
                        Debug.logError(errMsg, module);
                    }
                } catch (GenericServiceException e){
                    String errMsg = "Error creating OagisMessageErrorInfo: "+e.toString();
                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            }
        } else{
            String errMsg = "No such message with an error was found in OagisMessageInfo Entity ; Not creating OagisMessageErrorInfo";
            Debug.logWarning(errMsg, module);
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "OagisMessageInfoNotFoundError"));
        }
        
        Map result = new HashMap();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);
        
        if (errorMapList.size()>0){
            String errMsg = "Error Processing Received Message";
            result.put("errorMapList", errorMapList);
            //result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        
        result.putAll(ServiceUtil.returnSuccess("Service Completed Successfully"));
        return result;
    }
    
    public static Map oagisMessageHandler(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        InputStream in = (InputStream) context.get("inputStream");
        List errorList = FastList.newInstance();
        
        Document doc = null;
        String xmlText = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuffer xmlTextBuf = new StringBuffer();
            String currentLine = null;
            while ((currentLine = br.readLine()) != null) {
                xmlTextBuf.append(currentLine);
                xmlTextBuf.append('\n');
            }
            xmlText = xmlTextBuf.toString();
            
            // DEJ20070804 adding this temporarily for debugging, should be changed to verbose at some point in the future
            Debug.logInfo("Received OAGIS XML message, here is the text: \n" + xmlText, module);

            ByteArrayInputStream bis = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
            doc = UtilXml.readXmlDocument(bis, true, "OagisMessage");
        } catch (SAXException e) {
            String errMsg = "XML Error parsing the Received Message [" + e.toString() + "]; The text received we could not parse is: [" + xmlText + "]";
            errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SAXException"));
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Parser Configuration Error parsing the Received Message: " + e.toString();
            errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "ParserConfigurationException"));
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "IO Error parsing the Received Message: " + e.toString();
            errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "IOException"));
            Debug.logError(e, errMsg, module);
        }

        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError("Unable to parse received message");
        }

        Element rootElement = doc.getDocumentElement();
        rootElement.normalize();
        Element controlAreaElement = UtilXml.firstChildElement(rootElement, "os:CNTROLAREA");
        Element bsrElement = UtilXml.firstChildElement(controlAreaElement, "os:BSR");
        String bsrVerb = UtilXml.childElementValue(bsrElement, "of:VERB");
        String bsrNoun = UtilXml.childElementValue(bsrElement, "of:NOUN");
        
        if (UtilValidate.isEmpty(bsrVerb) || UtilValidate.isEmpty(bsrNoun)) {
            return ServiceUtil.returnError("Was able to receive and parse the XML message, but BSR->NOUN [" + bsrNoun + "] and/or BSR->VERB [" + bsrVerb + "] are empty");
        }
        
        // TODO: before dispatching the message, make sure the combined ID (primary of OagisMessageInfo entity) is not in the database, ie hasn't been received already
        
        Map subServiceResult = FastMap.newInstance();
        if (bsrVerb.equalsIgnoreCase("CONFIRM") && bsrNoun.equalsIgnoreCase("BOD")) {
            try {
                subServiceResult = dispatcher.runSync("receiveConfirmBod", UtilMisc.toMap("document",doc));
            } catch (GenericServiceException e) {
                String errMsg = "Error running service receiveConfirmBod: "+e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SHOW") && bsrNoun.equalsIgnoreCase("SHIPMENT")) {
            try {
                subServiceResult = dispatcher.runSync("showShipment", UtilMisc.toMap("document",doc));
            } catch (GenericServiceException e) {
                String errMsg = "Error running service showShipment: "+e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SYNC") && bsrNoun.equalsIgnoreCase("INVENTORY")) {
            try {
                subServiceResult = dispatcher.runSync("syncInventory", UtilMisc.toMap("document",doc));
            } catch (GenericServiceException e) {
                String errMsg = "Error running service syncInventory: "+e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("ACKNOWLEDGE") && bsrNoun.equalsIgnoreCase("DELIVERY")) {
            Element dataAreaElement = UtilXml.firstChildElement(rootElement, "ns:DATAAREA");
            Element ackDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");
            Element receiptlnElement = UtilXml.firstChildElement(ackDeliveryElement, "ns:RECEIPTLN");
            Element docRefElement = UtilXml.firstChildElement(receiptlnElement, "os:DOCUMNTREF");
            String docType = UtilXml.childElementValue(docRefElement, "of:DOCTYPE");
            if ("PO".equals(docType)){
                try {
                    subServiceResult = dispatcher.runSync("receivePoAcknowledge", UtilMisc.toMap("document",doc));
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service receivePoAcknowledge: "+e.toString();
                    errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else if ("RMA".equals(docType)) {
                try {
                    subServiceResult = dispatcher.runSync("receiveRmaAcknowledge", UtilMisc.toMap("document",doc));
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service receiveRmaAcknowledge: "+e.toString();
                    errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else {
                return ServiceUtil.returnError("For Acknowledge Delivery message could not determine if it is for a PO or RMA. DOCTYPE from message is " + docType);
            }
        } else {
            String errMsg = "Unknown Message Received";
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        Map result = ServiceUtil.returnSuccess();
        result.putAll(subServiceResult);
        result.put("contentType", "text/plain");

        List errorMapList = (List) subServiceResult.get("errorMapList");
        if (UtilValidate.isNotEmpty(errorList)) {
            Iterator errListItr = errorList.iterator();
            while (errListItr.hasNext()) {
                Map errorMap = (Map) errListItr.next();
                errorMapList.add(UtilMisc.toMap("description", errorMap.get("description"), "reasonCode", errorMap.get("reasonCode")));
            }
            result.put("errorMapList", errorMapList);
        }
        
        return result;
    }

    public static Map sendMessageText(String outText, OutputStream out, String sendToUrl, String saveToDirectory, String saveToFilename) {
        if (out != null) {
            Writer outWriter = new OutputStreamWriter(out);
            try {
                outWriter.write(outText);
                outWriter.close();
            } catch (IOException e) {
                String errMsg = "Error writing message to output stream: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else if (UtilValidate.isNotEmpty(saveToFilename) && UtilValidate.isNotEmpty(saveToDirectory)) {
            try {
                File outdir = new File(saveToDirectory);
                if (!outdir.exists()) {
                    outdir.mkdir();
                }
                Writer outWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, saveToFilename)), "UTF-8")));
                outWriter.write(outText);
                outWriter.close();
            } catch (Exception e) {
                String errMsg = "Error saving message to file [" + saveToFilename + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else if (UtilValidate.isNotEmpty(sendToUrl)) {
            HttpClient http = new HttpClient(sendToUrl);

            // test parameters
            http.setHostVerificationLevel(SSLUtil.HOSTCERT_NO_CHECK);
            http.setAllowUntrusted(true);
            http.setDebug(true);
              
            // needed XML post parameters
            if (UtilValidate.isNotEmpty(certAlias)) {
                http.setClientCertificateAlias(certAlias);
            }
            if (UtilValidate.isNotEmpty(basicAuthUsername)) {
                http.setBasicAuthInfo(basicAuthUsername, basicAuthPassword);
            }
            http.setContentType("text/xml");
            http.setKeepAlive(true);

            try {
                http.post(outText);
            } catch (Exception e) {
                String errMsg = "Error posting message to server with URL [" + sendToUrl + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            if (Debug.infoOn()) Debug.logInfo("No send to information, so here is the message: " + outText, module);
            return ServiceUtil.returnError("No send to information pass (url, file, or out stream)");
        }
        
        return null;
    }
}
