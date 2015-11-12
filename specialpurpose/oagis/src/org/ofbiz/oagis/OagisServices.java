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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.fo.FoFormRenderer;
import org.ofbiz.widget.renderer.html.HtmlScreenRenderer;
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

    public static final boolean debugSaveXmlOut = "true".equals(UtilProperties.getPropertyValue("oagis", "Oagis.Debug.Save.Xml.Out"));
    public static final boolean debugSaveXmlIn = "true".equals(UtilProperties.getPropertyValue("oagis", "Oagis.Debug.Save.Xml.In"));

    /** if TRUE then must exist, if FALSE must not exist, if null don't care */
    public static final Boolean requireSerialNumberExist;
    static {
        String requireSerialNumberExistStr = UtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.RequireSerialNumberExist");
        if ("true".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.TRUE;
        } else if ("false".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.FALSE;
        } else {
            requireSerialNumberExist = null;
        }
    }

    public static Map<String, Object> oagisSendConfirmBod(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String errorReferenceId = (String) context.get("referenceId");
        List<Map<String, String>> errorMapList = UtilGenerics.checkList(context.get("errorMapList"));
        
        String sendToUrl = (String) context.get("sendToUrl");
        if (UtilValidate.isEmpty(sendToUrl)) {
            sendToUrl = EntityUtilProperties.getPropertyValue("oagis", "url.send.confirmBod", delegator);
        }

        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.filename.base", "", delegator);
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ConfirmBod" + errorReferenceId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.directory", delegator);
        }

        OutputStream out = (OutputStream) context.get("outputStream");

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting userLogin", module);
        }

        String logicalId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.LOGICALID", delegator);
        String authId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.AUTHID", delegator);

        MapStack<String> bodyParameters =  MapStack.create();
        bodyParameters.put("logicalId", logicalId);
        bodyParameters.put("authId", authId);

        String referenceId = delegator.getNextSeqId("OagisMessageInfo");
        bodyParameters.put("referenceId", referenceId);

        Timestamp timestamp = UtilDateTime.nowTimestamp();
        String sentDate = isoDateFormat.format(timestamp);
        bodyParameters.put("sentDate", sentDate);

        Map<String, Object> omiPkMap = new HashMap<String, Object>();
        omiPkMap.put("logicalId", logicalId);
        omiPkMap.put("component", "EXCEPTION");
        omiPkMap.put("task", "RECIEPT");
        omiPkMap.put("referenceId", referenceId);

        Map<String, Object> oagisMsgInfoContext = new HashMap<String, Object>();
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
        } catch (GenericServiceException e) {
            Debug.logError(e, "Saving message to database failed", module);
        }

        try {
            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
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
        String bodyScreenUri = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Template.ConfirmBod", delegator);

        String outText = null;
        try {
            Writer writer = new StringWriter();
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, new HtmlScreenRenderer());
            screens.render(bodyScreenUri);
            writer.close();
            outText = writer.toString();
        } catch (Exception e) {
            Debug.logError(e, "Error rendering message: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorMessage", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        if (Debug.infoOn()) {
            Debug.logInfo("Finished rendering oagisSendConfirmBod message for errorReferenceId [" + errorReferenceId + "]", module);
        }

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

        Map<String, Object> sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename, locale, delegator);
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
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale));
    }

    public static Map<String, Object> oagisReceiveConfirmBod(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Document doc = (Document) context.get("document");
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
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
        String origRef = UtilXml.childElementValue(dataAreaConfirmElement, "of:ORIGREF");

        Timestamp receivedTimestamp = UtilDateTime.nowTimestamp();

        Map<String, Object> omiPkMap = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);

        Map<String, Object> oagisMsgInfoCtx = new HashMap<String, Object>();
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
                errorMapList.add(UtilMisc.<String, String>toMap("description", (Object) errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */

            List<? extends Element> dataAreaConfirmMsgList = UtilXml.childElementList(dataAreaConfirmElement, "ns:CONFIRMMSG");
            if (UtilValidate.isEmpty(dataAreaConfirmMsgList)) {
                String errMsg = "No CONFIRMMSG elements found in Confirm BOD message: " + omiPkMap;
                Debug.logWarning(errMsg, module);
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "NoCONFIRMMSGElements"));
            } else {
                Map<String, Object> originalOmiPkMap = UtilMisc.toMap("logicalId", (Object) dataAreaLogicalId, "component", dataAreaComponent,
                        "task", dataAreaTask, "referenceId", dataAreaReferenceId);
                GenericValue originalOagisMsgInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(originalOmiPkMap).queryOne();
                if (originalOagisMsgInfo != null) {
                    for (Element dataAreaConfirmMsgElement : dataAreaConfirmMsgList) {
                        String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:DESCRIPTN");
                        String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:REASONCODE");

                        Map<String, Object> createOagisMessageErrorInfoForOriginal = new HashMap<String, Object>();
                        createOagisMessageErrorInfoForOriginal.putAll(originalOmiPkMap);
                        createOagisMessageErrorInfoForOriginal.put("reasonCode", reasonCode);
                        createOagisMessageErrorInfoForOriginal.put("description", description);
                        createOagisMessageErrorInfoForOriginal.put("userLogin", userLogin);

                        // this will run in the same transaction
                        Map<String, Object> oagisMsgErrorInfoResult = dispatcher.runSync("createOagisMessageErrorInfo", createOagisMessageErrorInfoForOriginal);
                        if (ServiceUtil.isError(oagisMsgErrorInfoResult)) {
                            String errMsg = "Error creating OagisMessageErrorInfo: " + ServiceUtil.getErrorMessage(oagisMsgErrorInfoResult);
                            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "CreateOagisMessageErrorInfoServiceError"));
                            Debug.logError(errMsg, module);
                        }
                    }
                } else {
                    String errMsg = "No such message with an error was found; Not creating OagisMessageErrorInfo record(s) for original message, but saving info for this message anyway; ID info: " + omiPkMap;
                    Debug.logWarning(errMsg, module);
                    errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "OriginalOagisMessageInfoNotFoundError"));
                }

                // now attach all of the messages to the CBOD OagisMessageInfo record
                for (Element dataAreaConfirmMsgElement : dataAreaConfirmMsgList) {
                    String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:DESCRIPTN");
                    String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "of:REASONCODE");

                    Map<String, Object> createOagisMessageErrorInfoForCbod = new HashMap<String, Object>();
                    createOagisMessageErrorInfoForCbod.putAll(omiPkMap);
                    createOagisMessageErrorInfoForCbod.put("reasonCode", reasonCode);
                    createOagisMessageErrorInfoForCbod.put("description", description);
                    createOagisMessageErrorInfoForCbod.put("userLogin", userLogin);

                    // this one will also go in another transaction as the create service for the base record did too
                    Map<String, Object> oagisMsgErrorInfoResult = dispatcher.runSync("createOagisMessageErrorInfo", createOagisMessageErrorInfoForCbod, 60, true);
                    if (ServiceUtil.isError(oagisMsgErrorInfoResult)) {
                        String errMsg = "Error creating OagisMessageErrorInfo: " + ServiceUtil.getErrorMessage(oagisMsgErrorInfoResult);
                        Debug.logError(errMsg, module);
                    }
                }
            }
        } catch (Throwable t) {
            Debug.logError(t, "System Error processing Confirm BOD message: " + t.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorProcessingConfirmBOD", UtilMisc.toMap("errorString", t.toString()), locale));
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
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
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
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
            result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisErrorProcessingMessage", locale)));
            return result;
        } else {
            oagisMsgInfoCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }
        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        
        return result;
    }

    public static Map<String, Object> oagisReReceiveMessage(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String logicalId = (String) context.get("logicalId");
        String component = (String) context.get("component");
        String task = (String) context.get("task");
        String referenceId = (String) context.get("referenceId");
        Map<String, Object> oagisMessageInfoKey = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);

        try {
            GenericValue oagisMessageInfo = null;

            if (UtilValidate.isNotEmpty(referenceId) && (UtilValidate.isEmpty(component) || UtilValidate.isEmpty(task) || UtilValidate.isEmpty(referenceId))) {
                // try looking up by just the referenceId, those alone are often unique, return error if there is more than one result
                List<GenericValue> oagisMessageInfoList = EntityQuery.use(delegator).from("OagisMessageInfo").where("referenceId", referenceId).queryList();
                if (oagisMessageInfoList.size() == 1) {
                    oagisMessageInfo = oagisMessageInfoList.get(0);
                } else if (oagisMessageInfoList.size() > 1) {
                    Integer messageSize = new Integer(oagisMessageInfoList.size());
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorLookupByReferenceError", UtilMisc.toMap("messageSize", messageSize.toString(), "referenceId", referenceId), locale));
                }
            } else {
                oagisMessageInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(oagisMessageInfoKey).queryOne();
            }

            if (oagisMessageInfo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorFindOagisMessageInfo", UtilMisc.toMap("oagisMessageInfoKey", oagisMessageInfoKey), locale));
            }

            String fullMessageXml = oagisMessageInfo.getString("fullMessageXml");
            if (UtilValidate.isEmpty(fullMessageXml)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorNotFoundFullMessageXml", UtilMisc.toMap("oagisMessageInfoKey", oagisMessageInfoKey), locale));
            }

            // we know we have text now, run it!
            ByteArrayInputStream bis = new ByteArrayInputStream(fullMessageXml.getBytes("UTF-8"));
            Map<String, Object> result = dispatcher.runSync("oagisMessageHandler", UtilMisc.toMap("inputStream", bis, "isErrorRetry", Boolean.TRUE));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorReceivingAgainMessage", UtilMisc.toMap("oagisMessageInfoKey", oagisMessageInfoKey), locale), null, null, result);
            }
            return ServiceUtil.returnSuccess();
        } catch (Exception e) {
            Debug.logError(e, "Error re-receiving message with ID [" + oagisMessageInfoKey + "]: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorReceivingAgainMessage", UtilMisc.toMap("oagisMessageInfoKey", oagisMessageInfoKey), locale) + e.toString());
        }
    }

    public static Map<String, Object> oagisMessageHandler(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        InputStream in = (InputStream) context.get("inputStream");
        List<Map<String, String>> errorList = new LinkedList<Map<String,String>>();
        Boolean isErrorRetry = (Boolean) context.get("isErrorRetry");
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
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
            errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SAXException"));
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Parser Configuration Error parsing the Received Message: " + e.toString();
            errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "ParserConfigurationException"));
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "IO Error parsing the Received Message: " + e.toString();
            errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "IOException"));
            Debug.logError(e, errMsg, module);
        }

        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorParsingMessage", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorParsingXMLMessage", UtilMisc.toMap("bsrNoun", bsrNoun, "bsrVerb", bsrVerb), locale));
        }

        GenericValue oagisMessageInfo = null;
        Map<String, Object> oagisMessageInfoKey = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);
        try {
            oagisMessageInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(oagisMessageInfoKey).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error Getting Entity OagisMessageInfo: " + e.toString(), module);
        }

        Map<String, Object> messageProcessContext = UtilMisc.toMap("document", doc, "userLogin", userLogin);

        // call async, no additional results to return: Map subServiceResult = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(oagisMessageInfo)) {
            if (Boolean.TRUE.equals(isErrorRetry) || "OAGMP_SYS_ERROR".equals(oagisMessageInfo.getString("processingStatusId"))) {
                // there was an error last time, tell the service this is a retry
                messageProcessContext.put("isErrorRetry", Boolean.TRUE);
            } else {
                String responseMsg = "Message already received with ID: " + oagisMessageInfoKey;
                Debug.logError(responseMsg, module);

                List<Map<String, String>> errorMapList = UtilMisc.toList(UtilMisc.<String, String>toMap("reasonCode", "MessageAlreadyReceived", "description", responseMsg));

                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
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
                    Debug.logError(e, "Error sending Confirm BOD: " + e.toString(), module);
                }
                Map<String, Object> result = ServiceUtil.returnSuccess(responseMsg);
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
                errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SHOW") && bsrNoun.equalsIgnoreCase("SHIPMENT")) {
            try {
                //subServiceResult = dispatcher.runSync("oagisReceiveShowShipment", messageProcessContext);
                // DEJ20070808 changed to run asynchronously and persisted so that if it fails it will retry; for transaction deadlock and other reasons
                dispatcher.runAsync("oagisReceiveShowShipment", messageProcessContext, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error running service oagisReceiveShowShipment: " + e.toString();
                errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        } else if (bsrVerb.equalsIgnoreCase("SYNC") && bsrNoun.equalsIgnoreCase("INVENTORY")) {
            try {
                //subServiceResult = dispatcher.runSync("oagisReceiveSyncInventory", messageProcessContext);
                // DEJ20070808 changed to run asynchronously and persisted so that if it fails it will retry; for transaction deadlock and other reasons
                dispatcher.runAsync("oagisReceiveSyncInventory", messageProcessContext, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error running service oagisReceiveSyncInventory: " + e.toString();
                errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
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
                    errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else if ("RMA".equals(docType)) {
                try {
                    //subServiceResult = dispatcher.runSync("oagisReceiveAcknowledgeDeliveryRma", messageProcessContext);
                    dispatcher.runAsync("oagisReceiveAcknowledgeDeliveryRma", messageProcessContext, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service oagisReceiveAcknowledgeDeliveryRma: " + e.toString();
                    errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else if (UtilValidate.isEmpty(docType) && ("NotAvailableTOAvailable".equals(disposition) || "AvailableTONotAvailable".equals(disposition))) {
                try {
                    dispatcher.runAsync("oagisReceiveAcknowledgeDeliveryStatus", messageProcessContext, true);
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service oagisReceiveAcknowledgeDeliveryStatus: " + e.toString();
                    errorList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                    Debug.logError(e, errMsg, module);
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorDeliveryMessage", UtilMisc.toMap("docType", docType, "disposition", disposition), locale));
            }
        } else {
            Debug.logError("Unknown Message Type Received, verb/noun combination not supported: verb=[" + bsrVerb + "], noun=[" + bsrNoun + "]", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorUnknownMessageType", UtilMisc.toMap("bsrVerb", bsrVerb, "bsrNoun", bsrNoun), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("contentType", "text/plain");

        /* no sub-service error processing to be done here, all handled in the sub-services:
        result.putAll(subServiceResult);
        List<Map<String, String>> errorMapList = (List) subServiceResult.get("errorMapList");
        if (UtilValidate.isNotEmpty(errorList)) {
            Iterator errListItr = errorList.iterator();
            while (errListItr.hasNext()) {
                Map errorMap = (Map) errListItr.next();
                errorMapList.add(UtilMisc.<String, String>toMap("description", errorMap.get("description"), "reasonCode", errorMap.get("reasonCode")));
            }
            result.put("errorMapList", errorMapList);
        }
        */

        return result;
    }

    public static Map<String, Object> sendMessageText(String outText, OutputStream out, String sendToUrl, String saveToDirectory, String saveToFilename, Locale locale, Delegator delegator) {
        final String certAlias = EntityUtilProperties.getPropertyValue("oagis", "auth.client.certificate.alias", delegator);
        final String basicAuthUsername = EntityUtilProperties.getPropertyValue("oagis", "auth.basic.username", delegator);
        final String basicAuthPassword = EntityUtilProperties.getPropertyValue("oagis", "auth.basic.password", delegator);
    	if (out != null) {
            Writer outWriter = new OutputStreamWriter(out);
            try {
                outWriter.write(outText);
                outWriter.close();
            } catch (IOException e) {
                Debug.logError(e, "Error writing message to output stream: " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorWritingMessage", UtilMisc.toMap("errorString", e.toString()), locale));
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
                Debug.logError(e, "Error saving message to file [" + saveToFilename + "]: " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorSavingMessage", UtilMisc.toMap("saveToFilename", saveToFilename, "errorString", e.toString()), locale));
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
                Debug.logError(e, "Error posting message to server with URL [" + sendToUrl + "]: " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorPostingMessage", UtilMisc.toMap("sendToUrl", sendToUrl, "errorString", e.toString()), locale));
            }
        } else {
            if (Debug.infoOn()) Debug.logInfo("No send to information, so here is the message: " + outText, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorSendingInformations", locale));
        }

        return null;
    }

    public static Timestamp parseIsoDateString(String dateString, List<Map<String, String>> errorMapList) {
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
                if (errorMapList != null) errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ParseException", "description", errMsg));
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
