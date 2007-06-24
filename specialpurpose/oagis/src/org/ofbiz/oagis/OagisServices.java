package org.ofbiz.oagis;

/**
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
**/
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.*;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OagisServices {
    
    public static final String module = OagisServices.class.getName();
    
    public static Map sendConfirmBod(DispatchContext ctx, Map context) {
        return ServiceUtil.returnError("Service not Implemented");
    }

    public static Map receiveConfirmBod(DispatchContext ctx, Map context) {
        
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        Map oagisMsgInfoContext = new HashMap();
        Map oagisMsgErrorContext = new HashMap();
        GenericValue userLogin = null;
        String errMsg = null;
        try{
            userLogin = delegator.findByPrimaryKey("UserLogin",UtilMisc.toMap("userLoginId","admin"));
            Document doc = UtilXml.readXmlDocument(in, true, "RecieveConfirmBod");
            Element confirmBodElement = doc.getDocumentElement();
            confirmBodElement.normalize();
            Element docCtrlAreaElement = UtilXml.firstChildElement(confirmBodElement, "N1:CNTROLAREA");
            Element bsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:BSR");
            String bsrVerb = UtilXml.childElementValue(bsrElement, "N2:VERB");
            String bsrNoun = UtilXml.childElementValue(bsrElement, "N2:NOUN");
            String bsrRevision = UtilXml.childElementValue(bsrElement, "N2:REVISION");
            
            Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:SENDER");
            String logicalId = UtilXml.childElementValue(docSenderElement, "N2:LOGICALID");
            String component = UtilXml.childElementValue(docSenderElement, "N2:COMPONENT");
            String task = UtilXml.childElementValue(docSenderElement, "N2:TASK");
            String referenceId = UtilXml.childElementValue(docSenderElement, "N2:REFERENCEID");
            String confirmation = UtilXml.childElementValue(docSenderElement, "N2:CONFIRMATION");
            String language = UtilXml.childElementValue(docSenderElement, "N2:LANGUAGE");
            String codepage = UtilXml.childElementValue(docSenderElement, "N2:CODEPAGE");
            String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
            String sentDate = UtilXml.childElementValue(docCtrlAreaElement, "N1:DATETIMEANY");
            
            oagisMsgInfoContext.put("logicalId", logicalId);
            oagisMsgInfoContext.put("component", component);
            oagisMsgInfoContext.put("task", task);
            oagisMsgInfoContext.put("referenceId", referenceId);
            oagisMsgInfoContext.put("authId", authId);
            oagisMsgInfoContext.put("confirmation", confirmation);
            oagisMsgInfoContext.put("bsrVerb", bsrVerb);
            oagisMsgInfoContext.put("bsrNoun", bsrNoun);
            oagisMsgInfoContext.put("bsrRevision", bsrRevision);
            oagisMsgInfoContext.put("userLogin", userLogin);
            Debug.logInfo("==============oagisMsgInfoContext===== "+oagisMsgInfoContext, module);
            
            Element dataAreaElement = UtilXml.firstChildElement(confirmBodElement, "n:DATAAREA");
            Element dataAreaConfirmBodElement = UtilXml.firstChildElement(dataAreaElement, "n:CONFIRM_BOD");
            Element dataAreaConfirmElement = UtilXml.firstChildElement(dataAreaConfirmBodElement, "n:CONFIRM");
            Element dataAreaCtrlElement = UtilXml.firstChildElement(dataAreaConfirmElement, "N1:CNTROLAREA");
            Element dataAreaSenderElement = UtilXml.firstChildElement(dataAreaCtrlElement, "N1:SENDER");
            String dataAreaLogicalId = UtilXml.childElementValue(dataAreaSenderElement, "N2:LOGICALID");
            String dataAreaComponent = UtilXml.childElementValue(dataAreaSenderElement, "N2:COMPONENT");
            String dataAreaTask = UtilXml.childElementValue(dataAreaSenderElement, "N2:TASK");
            String dataAreaReferenceId = UtilXml.childElementValue(dataAreaSenderElement, "N2:REFERENCEID");
            
            String dataAreaDate = UtilXml.childElementValue(dataAreaCtrlElement, "N1:DATETIMEANY");
            
            String origRef = UtilXml.childElementValue(dataAreaConfirmElement, "N2:ORIGREF");
            
            Element dataAreaConfirmMsgElement = UtilXml.firstChildElement(dataAreaConfirmElement, "n:CONFIRMMSG");
            String description = UtilXml.childElementValue(dataAreaConfirmMsgElement, "N2:DESCRIPTN");
            String reasonCode = UtilXml.childElementValue(dataAreaConfirmMsgElement, "N2:REASONCODE");
            
            oagisMsgErrorContext.put("logicalId", dataAreaLogicalId);
            oagisMsgErrorContext.put("component", dataAreaComponent);
            oagisMsgErrorContext.put("task", dataAreaTask);
            oagisMsgErrorContext.put("referenceId", dataAreaReferenceId);
            oagisMsgErrorContext.put("reasonCode", reasonCode);
            oagisMsgErrorContext.put("description", description);
            oagisMsgErrorContext.put("userLogin", userLogin);
            Debug.logInfo("==============oagisErrorMsgContext===== "+oagisMsgErrorContext, module);
            
            Map resultMap = dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoContext);
            Debug.logInfo("==========resultMap-1======" +resultMap, module);
            resultMap = dispatcher.runSync("createOagisMessageErrorInfo", oagisMsgErrorContext);
            Debug.logInfo("==========resultMap-2======" +resultMap, module);
            
        }catch (Exception e){
            errMsg = "Error running method receiveConfirmBod";
            Debug.logError(e, errMsg, module);
        }
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        if (errMsg!= null){
            writer.println("Service failed");
            writer.flush();
            return ServiceUtil.returnError("Service failed");
        } else {
            writer.println("Service Completed Successfully");
            writer.flush();
            Map result = ServiceUtil.returnSuccess("Service Completed Successfully");
            return result;    
        }
        
    }
}
