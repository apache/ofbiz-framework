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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
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
    
    public static final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
    public static final SimpleDateFormat isoDateFormatNoTzValue = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'");
    
    public static final String resource = "OagisUiLabels";

    public static final String certAlias = UtilProperties.getPropertyValue("oagis.properties", "auth.client.certificate.alias");
    public static final String basicAuthUsername = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.username");
    public static final String basicAuthPassword = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.password");

    public static final boolean debugSaveXmlOut = "true".equals(UtilProperties.getPropertyValue("oagis.properties", "Oagis.Debug.Save.Xml.Out"));
    public static final boolean debugSaveXmlIn = "true".equals(UtilProperties.getPropertyValue("oagis.properties", "Oagis.Debug.Save.Xml.In"));

    /** if TRUE then must exist, if FALSE must not exist, if null don't care */
    public static final Boolean requireSerialNumberExist;
    static {
        String requireSerialNumberExistStr = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.RequireSerialNumberExist");
        if ("true".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.TRUE;
        } else if ("false".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.FALSE;
        } else {
            requireSerialNumberExist = null;
        }
    }

    public static Map oagisSendConfirmBod(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        
        String errorReferenceId = (String) context.get("referenceId");
        List errorMapList = (List) context.get("errorMapList");

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
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
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

        Timestamp timestamp = UtilDateTime.nowTimestamp();
        String sentDate = isoDateFormat.format(timestamp);
        bodyParameters.put("sentDate", sentDate);
        
        Map omiPkMap = FastMap.newInstance();
        omiPkMap.put("logicalId", logicalId);
        omiPkMap.put("component", "EXCEPTION");
        omiPkMap.put("task", "RECIEPT");
        omiPkMap.put("referenceId", referenceId);
        
        Map oagisMsgInfoContext = FastMap.newInstance();
        oagisMsgInfoContext.putAll(omiPkMap);
        oagisMsgInfoContext.put("authId", authId);
        oagisMsgInfoContext.put("sentDate", timestamp);
        oagisMsgInfoContext.put("confirmation", "0");
        oagisMsgInfoContext.put("bsrVerb", "CONFIRM");
        oagisMsgInfoContext.put("bsrNoun", "BOD");
        oagisMsgInfoContext.put("bsrRevision", "004");
        oagisMsgInfoContext.put("outgoingMessage", "Y");
        oagisMsgInfoContext.put("processingStatusId", "OAGMP_TRIGGERED");
        oagisMsgInfoContext.put("userLogin", userLogin);
        try {
            dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoContext, 60, true);
            /* 
            if (ServiceUtil.isError(oagisMsgInfoResult)) return ServiceUtil.returnError("Error creating OagisMessageInfo");
            */
        } catch (GenericServiceException e) {
            Debug.logError(e, "Saving message to database failed", module);
        }
        
        try {
            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map saveErrorMapListCtx = FastMap.newInstance();
            saveErrorMapListCtx.putAll(omiPkMap);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
        } catch (GenericServiceException e) {
            String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        bodyParameters.put("errorLogicalId", context.get("logicalId"));
        bodyParameters.put("errorComponent", context.get("component"));
        bodyParameters.put("errorTask", context.get("task"));
        bodyParameters.put("errorReferenceId", errorReferenceId);
        bodyParameters.put("errorMapList", errorMapList);
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
        
        if (Debug.infoOn()) Debug.logInfo("Finished rendering oagisSendConfirmBod message for errorReferenceId [" + errorReferenceId + "]", module);

        try {
            oagisMsgInfoContext.put("processingStatusId", "OAGMP_OGEN_SUCCESS");
            if (OagisServices.debugSaveXmlOut) {
                oagisMsgInfoContext.put("fullMessageXml", outText);
            }
            dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoContext, 60, true);
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
            Debug.logError(e, errMsg, module);
        }
        
        Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);
        if (sendMessageReturn != null) {
            return sendMessageReturn;
        }
        if (Debug.infoOn()) Debug.logInfo("Message send done for oagisSendConfirmBod for errorReferenceId [" + errorReferenceId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);

        try {
            oagisMsgInfoContext.put("processingStatusId", "OAGMP_SENT");
            dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoContext, 60, true);
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
            Debug.logError(e, errMsg, module);
        }
        
        return ServiceUtil.returnSuccess("Service Completed Successfully");
    }

    public static Map oagisReceiveConfirmBod(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Document doc = (Document) context.get("document");
        List errorMapList = FastList.newInstance();
        
        GenericValue userLogin = null; 
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin",UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting UserLogin with userLoginId 'system':" + e.toString();
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

        String sentDate = UtilXml.childElementValue(docCtrlAreaElement, "os:DATETIMEISO");
        Timestamp sentTimestamp = OagisServices.parseIsoDateString(sentDate, errorMapList);
        
        Element dataAreaElement = UtilXml.firstChildElement(confirmBodElement, "ns:DATAAREA");
        Element dataAreaConfirmBodElement = UtilXml.firstChildElement(dataAreaElement, "ns:CONFIRM_BOD");
        Element dataAreaConfirmElement = UtilXml.firstChildElement(dataAreaConfirmBodElement, "ns:CONFIRM");
        Element dataAreaCtrlElement = UtilXml.firstChildElement(dataAreaConfirmElement, "os:CNTROLAREA");
        Element dataAreaSenderElement = UtilXml.firstChildElement(dataAreaCtrlElement, "os:SENDER");
        String dataAreaLogicalId = UtilXml.childElementValue(dataAreaSenderElement, "of:LOGICALID");
        String dataAreaComponent = UtilXml.childElementValue(dataAreaSenderElement, "of:COMPONENT");
        String dataAreaTask = UtilXml.childElementValue(dataAreaSenderElement, "of:TASK");
        String dataAreaReferenceId = UtilXml.childElementValue(dataAreaSenderElement, "of:REFERENCEID");
        String dataAreaDate = UtilXml.childElementValue(dataAreaCtrlElement, "os:DATETIMEISO");
        String origRef = UtilXml.childElementValue(dataAreaConfirmElement, "of:ORIGREF");
          
        Timestamp receivedTimestamp = UtilDateTime.nowTimestamp();
        
        Map omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

        Map oagisMsgInfoCtx = FastMap.newInstance();
        oagisMsgInfoCtx.putAll(omiPkMap);
        oagisMsgInfoCtx.put("authId", authId);
        oagisMsgInfoCtx.put("receivedDate", receivedTimestamp);
        oagisMsgInfoCtx.put("sentDate", sentTimestamp);
        oagisMsgInfoCtx.put("confirmation", confirmation);
        oagisMsgInfoCtx.put("bsrVerb", bsrVerb);
        oagisMsgInfoCtx.put("bsrNoun", bsrNoun);
        oagisMsgInfoCtx.put("bsrRevision", bsrRevision);
        oagisMsgInfoCtx.put("outgoingMessage", "N");
        oagisMsgInfoCtx.put("origRef", origRef);
        oagisMsgInfoCtx.put("processingStatusId", "OAGMP_RECEIVED");
        oagisMsgInfoCtx.put("userLogin", userLogin);
        if (OagisServices.debugSaveXmlIn) {
            try {
                oagisMsgInfoCtx.put("fullMessageXml", UtilXml.writeXmlDocument(doc));
            } catch (IOException e) {
                // this is just for debug info, so just log and otherwise ignore error
                String errMsg = "Warning: error creating text from XML Document for saving to database: " + e.toString();
                Debug.logWarning(errMsg, module);
            }
        }
        
        try {
            dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            /* running async for better error handling
            if (ServiceUtil.isError(oagisMsgInfoResult)) {
                String errMsg = "Error creating OagisMessageInfo for the Incoming Message: "+ServiceUtil.getErrorMessage(oagisMsgInfoResult);
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */

            List dataAreaConfirmMsgList = UtilXml.childElementList(dataAreaConfirmElement, "ns:CONFIRMMSG");

            Map originalOmiPkMap = UtilMisc.toMap("logicalId", dataAreaLogicalId, "component", dataAreaComponent, "task", dataAreaTask, "referenceId", dataAreaReferenceId);
            GenericValue originalOagisMsgInfo = delegator.findByPrimaryKey("OagisMessageInfo", originalOmiPkMap);
            if (originalOagisMsgInfo != null) {
                Iterator dataAreaConfirmMsgListItr = dataAreaConfirmMsgList.iterator();
                while (dataAreaConfirmMsgListItr.hasNext()) {
                    Element dataAreaConfirmMsgElement = (Element) dataAreaConfirmMsgListItr.next();
                    String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:DESCRIPTN");
                    String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:REASONCODE");
                    
                    Map createOagisMessageErrorInfoForOriginal = FastMap.newInstance();
                    createOagisMessageErrorInfoForOriginal.putAll(originalOmiPkMap);
                    createOagisMessageErrorInfoForOriginal.put("reasonCode", reasonCode);
                    createOagisMessageErrorInfoForOriginal.put("description", description);
                    createOagisMessageErrorInfoForOriginal.put("userLogin", userLogin);
                
                    // this will run in the same transaction
                    Map oagisMsgErrorInfoResult = dispatcher.runSync("createOagisMessageErrorInfo", createOagisMessageErrorInfoForOriginal);
                    if (ServiceUtil.isError(oagisMsgErrorInfoResult)) {
                        String errMsg = "Error creating OagisMessageErrorInfo: " + ServiceUtil.getErrorMessage(oagisMsgErrorInfoResult);
                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageErrorInfoServiceError"));
                        Debug.logError(errMsg, module);
                    }
                }
            } else {
                String errMsg = "No such message with an error was found; Not creating OagisMessageErrorInfo record(s) for original message, but saving info for this message anyway; ID info: " + omiPkMap;
                Debug.logWarning(errMsg, module);
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "OriginalOagisMessageInfoNotFoundError"));
            }

            // now attach all of the messages to the CBOD OagisMessageInfo record
            Iterator dataAreaConfirmMsgListItr = dataAreaConfirmMsgList.iterator();
            while (dataAreaConfirmMsgListItr.hasNext()) {
                Element dataAreaConfirmMsgElement = (Element) dataAreaConfirmMsgListItr.next();
                String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:DESCRIPTN");
                String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:REASONCODE");
                
                Map createOagisMessageErrorInfoForCbod = FastMap.newInstance();
                createOagisMessageErrorInfoForCbod.putAll(omiPkMap);
                createOagisMessageErrorInfoForCbod.put("reasonCode", reasonCode);
                createOagisMessageErrorInfoForCbod.put("description", description);
                createOagisMessageErrorInfoForCbod.put("userLogin", userLogin);

                // this one will also go in another transaction as the create service for the base record did too
                Map oagisMsgErrorInfoResult = dispatcher.runSync("createOagisMessageErrorInfo", createOagisMessageErrorInfoForCbod, 60, true);
                if (ServiceUtil.isError(oagisMsgErrorInfoResult)) {
                    String errMsg = "Error creating OagisMessageErrorInfo: " + ServiceUtil.getErrorMessage(oagisMsgErrorInfoResult);
                    Debug.logError(errMsg, module);
                }
            }
        } catch (Throwable t) {
            String errMsg = "System Error processing Confirm BOD message: " + t.toString();
            Debug.logError(t, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        Map result = FastMap.newInstance();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map saveErrorMapListCtx = FastMap.newInstance();
            saveErrorMapListCtx.put("logicalId", logicalId);
            saveErrorMapListCtx.put("component", component);
            saveErrorMapListCtx.put("task", task);
            saveErrorMapListCtx.put("referenceId", referenceId);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
            
            // TODO and NOTE DEJ20070813: should we really send back a Confirm BOD if there is an error with the Confirm BOD they send us? probably so... will do for now...
            try {
                Map sendConfirmBodCtx = FastMap.newInstance();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                // no such thing for confirm bod: sendConfirmBodCtx.put("origRefId", shipmentId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
            
            // return success here so that the message won't be retried and the Confirm BOD, etc won't be sent multiple times 
            result.putAll(ServiceUtil.returnSuccess("Errors found processing message; information saved and return error sent back"));
            return result;
        } else {
            oagisMsgInfoCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }
        
        result.putAll(ServiceUtil.returnSuccess("Service Completed Successfully"));
        return result;
    }
    
    public static Map oagisReReceiveMessage(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        String logicalId = (String) context.get("logicalId");
        String component = (String) context.get("component");
        String task = (String) context.get("task");
        String referenceId = (String) context.get("referenceId");
        Map oagisMessageInfoKey = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);
        
        try {
            GenericValue oagisMessageInfo = null;
            
            if (UtilValidate.isNotEmpty(referenceId) && (UtilValidate.isEmpty(component) || UtilValidate.isEmpty(task) || UtilValidate.isEmpty(referenceId))) {
                // try looking up by just the referenceId, those alone are often unique, return error if there is more than one result
                List oagisMessageInfoList = delegator.findByAnd("OagisMessageInfo", UtilMisc.toMap("referenceId", referenceId));
                if (oagisMessageInfoList.size() == 1) {
                    oagisMessageInfo = (GenericValue) oagisMessageInfoList.get(0);
                } else if (oagisMessageInfoList.size() > 1) {
                    return ServiceUtil.returnError("Looked up by referenceId because logicalId, component, or task were not passed in but found more than one [" + oagisMessageInfoList.size() + "] record with referenceId [" + referenceId + "]");
                }
            } else {
                oagisMessageInfo = delegator.findByPrimaryKey("OagisMessageInfo", oagisMessageInfoKey);
            }
            
            if (oagisMessageInfo == null) {
                return ServiceUtil.returnError("Could not find OagisMessageInfo record with key [" + oagisMessageInfoKey + "], not rerunning message.");
            }
            
            String fullMessageXml = oagisMessageInfo.getString("fullMessageXml");
            if (UtilValidate.isEmpty(fullMessageXml)) {
                return ServiceUtil.returnError("There was no fullMessageXml text in OagisMessageInfo record with key [" + oagisMessageInfoKey + "], not rerunning message.");
            }
            
            // we know we have text now, run it!
            ByteArrayInputStream bis = new ByteArrayInputStream(fullMessageXml.getBytes("UTF-8"));
            Map result = dispatcher.runSync("oagisMessageHandler", UtilMisc.toMap("inputStream", bis, "isErrorRetry", Boolean.TRUE));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError("Error trying to re-receive message with ID [" + oagisMessageInfoKey + "]", null, null, result);
            }
            return ServiceUtil.returnSuccess();
        } catch (Exception e) {
            String errMsg = "Error re-receiving message with ID [" + oagisMessageInfoKey + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }
    
    public static Map oagisMessageHandler(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        InputStream in = (InputStream) context.get("inputStream");
        List errorList = FastList.newInstance();
        Boolean isErrorRetry = (Boolean) context.get("isErrorRetry");

        GenericValue userLogin = null; 
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));    
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting UserLogin with userLoginId system: "+e.toString();
            Debug.logError(e, errMsg, module);
        }
        
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
            Debug.logWarning("Received OAGIS XML message, here is the text: \n" + xmlText, module);

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
        
        Element senderElement = UtilXml.firstChildElement(controlAreaElement, "os:SENDER");
        String logicalId = UtilXml.childElementValue(senderElement, "of:LOGICALID");
        String component = UtilXml.childElementValue(senderElement, "of:COMPONENT");
        String task = UtilXml.childElementValue(senderElement, "of:TASK");
        String referenceId = UtilXml.childElementValue(senderElement, "of:REFERENCEID");
        
        if (UtilValidate.isEmpty(bsrVerb) || UtilValidate.isEmpty(bsrNoun)) {
            return ServiceUtil.returnError("Was able to receive and parse the XML message, but BSR->NOUN [" + bsrNoun + "] and/or BSR->VERB [" + bsrVerb + "] are empty");
        }
        
        GenericValue oagisMessageInfo = null;
        Map oagisMessageInfoKey = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);
        try {
            oagisMessageInfo = delegator.findByPrimaryKey("OagisMessageInfo", oagisMessageInfoKey);
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting Entity OagisMessageInfo: " + e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        Map messageProcessContext = UtilMisc.toMap("document", doc, "userLogin", userLogin);
        
        // call async, no additional results to return: Map subServiceResult = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(oagisMessageInfo)) {
            if (Boolean.TRUE.equals(isErrorRetry) || "OAGMP_SYS_ERROR".equals(oagisMessageInfo.getString("processingStatusId"))) {
                // there was an error last time, tell the service this is a retry
                messageProcessContext.put("isErrorRetry", Boolean.TRUE);
            } else {
                String responseMsg = "Message already received with ID: " + oagisMessageInfoKey;
                Debug.logError(responseMsg, module);

                List errorMapList = UtilMisc.toList(UtilMisc.toMap("reasonCode", "MessageAlreadyReceived", "description", responseMsg));

                Map sendConfirmBodCtx = FastMap.newInstance();
                sendConfirmBodCtx.put("logicalId", logicalId);
                sendConfirmBodCtx.put("component", component);
                sendConfirmBodCtx.put("task", task);
                sendConfirmBodCtx.put("referenceId", referenceId);
                sendConfirmBodCtx.put("errorMapList", errorMapList);
                sendConfirmBodCtx.put("userLogin", userLogin);

                try {
                    // run async because this will send a message back to the other server and may take some time, and/or fail
                    dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error sending Confirm BOD: " + e.toString();
                    Debug.logError(e, errMsg, module);
                }
                Map result = ServiceUtil.returnSuccess(responseMsg);
                result.put("contentType", "text/plain");
                return result;
            }
        }
        
        Debug.logInfo("Processing OAGIS message with verb [" + bsrVerb + "] and noun [" + bsrNoun + "] with context: " + messageProcessContext, module);
        
        if (bsrVerb.equalsIgnoreCase("CONFIRM") && bsrNoun.equalsIgnoreCase("BOD")) {
            try {
                // subServiceResult = dispatcher.runSync("oagisReceiveConfirmBod", messageProcessContext);
                dispatcher.runAsync("oagisReceiveConfirmBod", messageProcessContext, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error running service oagisReceiveConfirmBod: " + e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SHOW") && bsrNoun.equalsIgnoreCase("SHIPMENT")) {
            try {
                //subServiceResult = dispatcher.runSync("oagisReceiveShowShipment", messageProcessContext);
                // DEJ20070808 changed to run asynchronously and persisted so that if it fails it will retry; for transaction deadlock and other reasons
                dispatcher.runAsync("oagisReceiveShowShipment", messageProcessContext, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error running service oagisReceiveShowShipment: " + e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SYNC") && bsrNoun.equalsIgnoreCase("INVENTORY")) {
            try {
                //subServiceResult = dispatcher.runSync("oagisReceiveSyncInventory", messageProcessContext);
                // DEJ20070808 changed to run asynchronously and persisted so that if it fails it will retry; for transaction deadlock and other reasons
                dispatcher.runAsync("oagisReceiveSyncInventory", messageProcessContext, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error running service oagisReceiveSyncInventory: " + e.toString();
                errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("ACKNOWLEDGE") && bsrNoun.equalsIgnoreCase("DELIVERY")) {
            Element dataAreaElement = UtilXml.firstChildElement(rootElement, "ns:DATAAREA");
            Element ackDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");
            Element receiptlnElement = UtilXml.firstChildElement(ackDeliveryElement, "ns:RECEIPTLN");
            Element docRefElement = UtilXml.firstChildElement(receiptlnElement, "os:DOCUMNTREF");
            String docType = docRefElement != null ? UtilXml.childElementValue(docRefElement, "of:DOCTYPE") : null;
            String disposition = UtilXml.childElementValue(receiptlnElement, "of:DISPOSITN");
            
            if ("PO".equals(docType)) {
                try {
                    //subServiceResult = dispatcher.runSync("oagisReceiveAcknowledgeDeliveryPo", messageProcessContext);
                    dispatcher.runAsync("oagisReceiveAcknowledgeDeliveryPo", messageProcessContext, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service oagisReceiveAcknowledgeDeliveryPo: " + e.toString();
                    errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else if ("RMA".equals(docType)) {
                try {
                    //subServiceResult = dispatcher.runSync("oagisReceiveAcknowledgeDeliveryRma", messageProcessContext);
                    dispatcher.runAsync("oagisReceiveAcknowledgeDeliveryRma", messageProcessContext, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service oagisReceiveAcknowledgeDeliveryRma: " + e.toString();
                    errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else if (UtilValidate.isEmpty(docType) && ("NotAvailableTOAvailable".equals(disposition) || "AvailableTONotAvailable".equals(disposition))) {
                try {
                    dispatcher.runAsync("oagisReceiveAcknowledgeDeliveryStatus", messageProcessContext, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service oagisReceiveAcknowledgeDeliveryStatus: " + e.toString();
                    errorList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else {
                return ServiceUtil.returnError("For Acknowledge Delivery message could not determine if it is for a PO or RMA or Status Change. DOCTYPE from message is [" + docType + "], DISPOSITN is [" + disposition + "]");
            }
        } else {
            String errMsg = "Unknown Message Type Received, verb/noun combination not supported: verb=[" + bsrVerb + "], noun=[" + bsrNoun + "]";
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        Map result = ServiceUtil.returnSuccess();
        result.put("contentType", "text/plain");

        /* no sub-service error processing to be done here, all handled in the sub-services:
        result.putAll(subServiceResult);
        List errorMapList = (List) subServiceResult.get("errorMapList");
        if (UtilValidate.isNotEmpty(errorList)) {
            Iterator errListItr = errorList.iterator();
            while (errListItr.hasNext()) {
                Map errorMap = (Map) errListItr.next();
                errorMapList.add(UtilMisc.toMap("description", errorMap.get("description"), "reasonCode", errorMap.get("reasonCode")));
            }
            result.put("errorMapList", errorMapList);
        }
        */
        
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
    
    public static Timestamp parseIsoDateString(String dateString, List errorMapList) {
        if (UtilValidate.isEmpty(dateString)) return null;
        
        Date dateTimeInvReceived = null;
        try {
            dateTimeInvReceived = isoDateFormat.parse(dateString);
        } catch (ParseException e) {
            Debug.logInfo("Message does not have timezone information in date field", module);
            try {
                dateTimeInvReceived = isoDateFormatNoTzValue.parse(dateString);
            } catch (ParseException e1) {
                String errMsg = "Error parsing Date: " + e1.toString();
                if (errorMapList != null) errorMapList.add(UtilMisc.toMap("reasonCode", "ParseException", "description", errMsg));
                Debug.logError(e, errMsg, module);
            }
        }

        Timestamp snapshotDate = null;      
        if (dateTimeInvReceived != null) {
            snapshotDate = new Timestamp(dateTimeInvReceived.getTime());
        }
        return snapshotDate;
    }
}
