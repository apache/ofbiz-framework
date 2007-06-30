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
//import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import javolution.util.FastList;
import javolution.util.FastMap;

import java.sql.Timestamp;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;




public class OagisInventoryServices {
    
    public static final String module = OagisInventoryServices.class.getName();
    
    public static Map syncInventory(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        //create a List for Storing  error Information 
        List errorList = FastList.newInstance();
        // create Map for service sendConfirmBod
        Map sendConfirmBodCtx = FastMap.newInstance();

        GenericValue contactMech = null;
        GenericValue facilityContactMech=null;
        GenericValue  userLogin = null ;

        String contactMechId = null;
        String emailString = null;
        String contactMechTypeId = null;
        String errMsg = null;

        try {
            userLogin = delegator.findByPrimaryKey("UserLogin",UtilMisc.toMap("userLoginId","admin"));
            Document doc = UtilXml.readXmlDocument(in, true, "SyncInventory");
            Element receiveInventoryElement = doc.getDocumentElement();
            receiveInventoryElement.normalize();
                        
            Element docCtrlAreaElement = UtilXml.firstChildElement(receiveInventoryElement, "N1:CNTROLAREA");
            Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:SENDER");
            Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:BSR");
            String bsrVerb = UtilXml.childElementValue(docBsrElement, "N2:VERB");
            String bsrNoun = UtilXml.childElementValue(docBsrElement, "N2:NOUN");
            String bsrRevision = UtilXml.childElementValue(docBsrElement, "N2:REVISION");
            String logicalId = UtilXml.childElementValue(docSenderElement, "N2:LOGICALID");
            
            String component = UtilXml.childElementValue(docSenderElement, "N2:COMPONENT");
            String task = UtilXml.childElementValue(docSenderElement, "N2:TASK");
            String referenceId = UtilXml.childElementValue(docSenderElement, "N2:REFERENCEID");
            String confirmation = UtilXml.childElementValue(docSenderElement, "N2:CONFIRMATION");
//            String language = UtilXml.childElementValue(docSenderElement, "N2:LANGUAGE");
//            String codePage = UtilXml.childElementValue(docSenderElement, "N2:CODEPAGE");
            String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
            Element dataAreaElement = UtilXml.firstChildElement(receiveInventoryElement, "n:DATAAREA");
            Element dataAreaSyncInventoryElement = UtilXml.firstChildElement(dataAreaElement, "n:SYNC_INVENTORY");
            Element dataAreaInventoryElement = UtilXml.firstChildElement(dataAreaSyncInventoryElement, "n:INVENTORY");
            String receivedDate = UtilXml.childElementValue(dataAreaInventoryElement, "N1:DATETIMEANY");
            Element dataAreaQuantityElement = UtilXml.firstChildElement(dataAreaInventoryElement, "N1:QUANTITY");
            String value = UtilXml.childElementValue(dataAreaQuantityElement, "N2:VALUE");
//            String numOfDec = UtilXml.childElementValue(dataAreaQuantityElement, "N2:NUMOFDEC");
            String sign = UtilXml.childElementValue(dataAreaQuantityElement, "N2:SIGN");
            String uom = UtilXml.childElementValue(dataAreaQuantityElement, "N2:UOM");
            String item = UtilXml.childElementValue(dataAreaQuantityElement, "N2:ITEM");
            String itemStatus = UtilXml.childElementValue(dataAreaQuantityElement, "N2:ITEMSTATUS");
            
            double quantityAccepted ;
            double quantityRejected ;
            
            if ( sign.equals("+")) {
                quantityAccepted = Double.parseDouble(value);
                quantityRejected =0.0 ;
            } else {
                quantityRejected = Double.parseDouble(value);
                quantityAccepted = 0.0;
            }
            //create Map for service receiveInventoryProduct                        
            Map receiveInventoryCtx = FastMap.newInstance();
            receiveInventoryCtx.put("userLogin",userLogin);
            receiveInventoryCtx.put("statusId",itemStatus);
            receiveInventoryCtx.put("productId",item);
            receiveInventoryCtx.put("inventoryItemTypeId","NON_SERIAL_INV_ITEM");
            receiveInventoryCtx.put("facilityId","WebStoreWarehouse");
            receiveInventoryCtx.put("quantityAccepted",new Double(quantityAccepted));
            receiveInventoryCtx.put("quantityRejected",new Double(quantityRejected));
            receiveInventoryCtx.put("userLogin",userLogin);
            //receiveInventoryCtx.put("uomId",uom);
           
            Timestamp timestamp = null;
            timestamp = UtilDateTime.nowTimestamp();
            
            //create Map for service createOagisMessageInfo
            
            Map oagisMessageInfoCtx= FastMap.newInstance();
            oagisMessageInfoCtx.put("logicalId",logicalId);
            oagisMessageInfoCtx.put("component",component);
            oagisMessageInfoCtx.put("task",task);
            oagisMessageInfoCtx.put("referenceId",referenceId);
            oagisMessageInfoCtx.put("confirmation",confirmation);
            oagisMessageInfoCtx.put("userLogin",userLogin);
            oagisMessageInfoCtx.put("authId",authId);
            oagisMessageInfoCtx.put("bsrVerb",bsrVerb);
            oagisMessageInfoCtx.put("bsrNoun",bsrNoun);
            oagisMessageInfoCtx.put("bsrRevision",bsrRevision);
            oagisMessageInfoCtx.put("receivedDate",timestamp);
            oagisMessageInfoCtx.put("outgoingMessage","N");
            
           //create Map for service sendConfirmBod
            
            sendConfirmBodCtx.put("logicalId",logicalId);
            sendConfirmBodCtx.put("component",component);
            sendConfirmBodCtx.put("task",task);
            sendConfirmBodCtx.put("referenceId",referenceId);
            sendConfirmBodCtx.put("userLogin",userLogin);
            
            
            //create a Map for getting result of service getProductInventoryAvailable 
            Map gpiaResult = FastMap.newInstance();

            try {
                gpiaResult = dispatcher.runSync("getProductInventoryAvailable",UtilMisc.toMap("productId",item) );
                Debug.logInfo("==============gpiaResult===== "+gpiaResult, module);             
            } catch(GenericServiceException gse) {
                errorList.add("Error Running Service createOagisMessageInfo");
                String errMessageForgetProductInventoryAvailable = gse.getMessage();
                Debug.logError(gse, errMessageForgetProductInventoryAvailable, module);
            }
            
            String availableToPromiseTotal = gpiaResult.get("availableToPromiseTotal").toString();
            // create a Map for getting result of service receiveInventoryProduct
            Map receiveInventoryProductResult = FastMap.newInstance();
            try {
                if (value.equals(availableToPromiseTotal) ) {   
                    Debug.logInfo("==========Both Values are same  =====",module);
                } else {
                    //sevice for receiveInventoryProduct in InventoryItem
                    receiveInventoryProductResult = dispatcher.runSync("receiveInventoryProduct",receiveInventoryCtx );
                    Debug.logInfo("==============receiveInventoryProductResult===== "+receiveInventoryProductResult, module);
                }
            } catch(GenericServiceException gse) {
                errorList.add("Error Running Service receiveInventoryProduct");
                String errMessageForreceiveInventoryProduct = gse.getMessage();
                Debug.logError(gse, errMessageForreceiveInventoryProduct, module);
            }
            // create a Map for getting result of service createOagisMessageInfo
            Map oagisMessageInfoResult = FastMap.newInstance();
            try {
                //service for creating OagisMessageInfo  
                oagisMessageInfoResult = dispatcher.runSync("createOagisMessageInfo", oagisMessageInfoCtx);
                Debug.logInfo("==============oagisMessageInfoResult===== "+oagisMessageInfoResult, module);
            } catch(GenericServiceException gse) {
                errorList.add("Error Running Service createOagisMessageInfo");
                String errMessageForcreateOagisMessageInfo = gse.getMessage();
                Debug.logError(gse, errMessageForcreateOagisMessageInfo, module);
            }
            //create List for Getting FacilityContactMech
            List facilityContactMechs = FastList.newInstance();
            
            facilityContactMechs = delegator.findByAnd("FacilityContactMech", UtilMisc.toMap("facilityId", "WebStoreWarehouse"));
            Iterator fcmIter  = facilityContactMechs.iterator();
            while(fcmIter.hasNext()) {
                facilityContactMech = (GenericValue) fcmIter.next();
                contactMechId = facilityContactMech.getString("contactMechId");
                contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
                contactMechTypeId = contactMech.getString("contactMechTypeId");
                if (contactMechTypeId.equals("EMAIL_ADDRESS")) {
                    emailString = contactMech.getString("infoString");
                }
            }
            //create Map for availableToPromiseTotal
            Map atptMap = FastMap.newInstance();
            atptMap.put("qoh", availableToPromiseTotal);
            //create Map for getting value of ProductStoreEmailSetting 
            GenericValue psesMap = null;
            psesMap  =   delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", "9001", "emailType", "PRDS_OAGIS_CONFIRM"));
            
            if(psesMap.get("bodyScreenLocation") != null) {
                //create a Map for services sendMailFromScreen
                Map notifyCtx = FastMap.newInstance();
                String fromAddress = psesMap.getString("fromAddress");
                notifyCtx.put("sendFrom", fromAddress);
                String ccAddress = psesMap.getString("ccAddress");
                notifyCtx.put("sendCc", ccAddress);
                String bccAddress = psesMap.getString("bccAddress");
                notifyCtx.put("sendBcc", bccAddress);
                String contentType = psesMap.getString("contentType");
                notifyCtx.put("contentType", contentType);
                String subject = psesMap.getString("subject");
                notifyCtx.put("subject", subject);
                String bodyScreenUri = psesMap.getString("bodyScreenLocation");
                notifyCtx.put("bodyScreenUri", bodyScreenUri);
                Map bodyParameters = FastMap.newInstance();
                bodyParameters.put("atptMap", atptMap);
                notifyCtx.put("bodyParameters", bodyParameters);
                notifyCtx.put("sendTo", emailString);
                //service for sending emailNotification
                dispatcher.runSync("sendMailFromScreen", notifyCtx);
            }
        } catch (Exception e) {
            String errMessageReceiveInventoryProduct = "Error During Entity Interaction   ";
            Debug.logError(e, errMessageReceiveInventoryProduct, module);
            errorList.add("Error During Entity Interaction");
        }
        StringBuffer successString = new StringBuffer();
        if (errorList.size() > 0) {
            Iterator errorListIter = errorList.iterator();
            while (errorListIter.hasNext()) {
                String errorMsg = (String) errorListIter.next();
                successString.append(errorMsg);
                if (errorListIter.hasNext()) {
                    successString.append(", ");
                }
            }
            try {
                 if(successString.length() > 0){
                    //send confirm bod
                    Map scbCtx = FastMap.newInstance();
                    scbCtx = dispatcher.runSync("sendConfirmBod",sendConfirmBodCtx );
                    Debug.logInfo("==========scbCtx======"+ scbCtx,module);
                 }
            } catch(GenericServiceException gse) {    
                String errMessageForsendConfirmBod = gse.getMessage();
                Debug.logError(gse, errMessageForsendConfirmBod, module);
            }    
        }
        Map result = ServiceUtil.returnSuccess("Action Performed Successfully");
        result.put("contentType", "text/plain");
        return result;
    }
    public static Map receivePoAcknowledge(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
       // GenericValue userLogin = (GenericValue) context.get("userLogin");
        List errorList = FastList.newInstance();
        Map sendConfirmBodCtx = FastMap.newInstance();
        
        try {
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));

            // parse the message 
            Document doc = UtilXml.readXmlDocument(in, true, "ReceivePoAcknowledge");
            Element receivePoElement = doc.getDocumentElement();
            receivePoElement.normalize();
            Element docCtrlAreaElement = UtilXml.firstChildElement(receivePoElement, "N1:CNTROLAREA");
            
            Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:SENDER");
            Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:BSR");

            String bsrVerb = UtilXml.childElementValue(docBsrElement, "N2:VERB");
            String bsrNoun = UtilXml.childElementValue(docBsrElement, "N2:NOUN");
            String bsrRevision = UtilXml.childElementValue(docBsrElement, "N2:REVISION");
            String logicalId = UtilXml.childElementValue(docSenderElement, "N2:LOGICALID");
            String component = UtilXml.childElementValue(docSenderElement, "N2:COMPONENT");
            String task = UtilXml.childElementValue(docSenderElement, "N2:TASK");
            String referenceId = UtilXml.childElementValue(docSenderElement, "N2:REFERENCEID");
            String confirmation = UtilXml.childElementValue(docSenderElement, "N2:CONFIRMATION");
//            String language = UtilXml.childElementValue(docSenderElement, "N2:LANGUAGE");
//            String codePage = UtilXml.childElementValue(docSenderElement, "N2:CODEPAGE");
            String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
            Element dataAreaElement = UtilXml.firstChildElement(receivePoElement, "n:DATAAREA");
            Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "n:ACKNOWLEDGE_DELIVERY");
            Element receiptHdrElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "n:RECEIPTHDR");
            Element qtyElement = UtilXml.firstChildElement(receiptHdrElement, "N1:QUANTITY");
            
            String itemQty = UtilXml.childElementValue(qtyElement, "N2:VALUE");
            String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
            String productId = UtilXml.childElementValue(receiptHdrElement, "N2:ITEM");
//            String receivedDate = UtilXml.childElementValue(receiptHdrElement, "N1:DATETIME");
            
            Element invDetailElement = UtilXml.firstChildElement(receiptHdrElement, "n:INVDETAIL");
            
            String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
            
            Element documentRefElement = UtilXml.firstChildElement(receiptHdrElement, "N1:DOCUMNTREF");
            String orderTypeId = UtilXml.childElementValue(documentRefElement, "N2:DOCTYPE");
            String orderId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");
//            String lineNum = UtilXml.childElementValue(documentRefElement, "N2:LINENUM");
            // prepare map to create inventory against PO
            Map cipCtx = new HashMap();
            String inventoryItemTypeId = null;
            if (serialNumber == null) {
                inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
                Debug.logInfo("=========inventoryItemTypeId======="+ inventoryItemTypeId ,module);
            }
            else {
                inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                Debug.logInfo("=========inventoryItemTypeId======="+ inventoryItemTypeId ,module);
                cipCtx.put("serialNumber", serialNumber);
            }
            // sign handling for items
            double quantityAccepted = 0.0;
            double quantityRejected = 0.0;
            if (sign.equals("+")) {
                quantityAccepted = Double.parseDouble(itemQty);
                Debug.logInfo("=========quantityAccepted======="+ quantityAccepted ,module);
                quantityRejected= 0.0;
            } else {
                  quantityRejected = Double.parseDouble(itemQty);
                  quantityAccepted = 0.0;
            }
            
            //prepare Map for receiveInventoryProduct
            cipCtx.put("facilityId", "WebStoreWarehouse");
            cipCtx.put("productId", productId);
            cipCtx.put("inventoryItemTypeId", inventoryItemTypeId);
            cipCtx.put("quantityAccepted", new Double(quantityAccepted));
            cipCtx.put("quantityRejected", new Double(quantityRejected));
            cipCtx.put("userLogin", userLogin);
            cipCtx.put("orderId", orderId);
            Debug.logInfo("=========cipCtx======="+ cipCtx ,module);
            Map riResult = null;
            Map comiResult = null;
            try {
                riResult = dispatcher.runSync("receiveInventoryProduct", cipCtx);
                Debug.logInfo("==========riResult======"+ riResult ,module);
            } catch (GenericServiceException gse) {
                Debug.logInfo("==========riResult===2==="+ riResult ,module);
                Debug.logInfo("==========riResult====3=="+ riResult ,module);
                errorList.add("Error running method receiveInventoryProduct");
                String errMessageForreceiveInventoryProduct = gse.getMessage();
                Debug.logError(gse, errMessageForreceiveInventoryProduct, module);
            }
            //prepare Map for ConfirmBod Service
            sendConfirmBodCtx.put("logicalId",logicalId);
            sendConfirmBodCtx.put("component",component);
            sendConfirmBodCtx.put("task",task);
            sendConfirmBodCtx.put("referenceId",referenceId);
            sendConfirmBodCtx.put("userLogin",userLogin);
            sendConfirmBodCtx.put("confirmation", confirmation);
            sendConfirmBodCtx.put("orderId", orderId);
            // prepare map to store BOD information
            Map comiCtx = new HashMap();
            
            Timestamp timestamp = null;
            timestamp = UtilDateTime.nowTimestamp();

            comiCtx.put("logicalId", logicalId);
            comiCtx.put("authId", authId);
            comiCtx.put("referenceId", referenceId);
            comiCtx.put("receivedDate", timestamp);
            comiCtx.put("component", component);
            comiCtx.put("task", task);  
            comiCtx.put("outgoingMessage", "N");
            comiCtx.put("confirmation", confirmation);
            comiCtx.put("bsrVerb", bsrVerb);
            comiCtx.put("bsrNoun", bsrNoun);
            comiCtx.put("bsrRevision", bsrRevision);
            comiCtx.put("orderId", orderId);
            comiCtx.put("userLogin", userLogin);
            try {
                comiResult = dispatcher.runSync("createOagisMessageInfo", comiCtx);
                Debug.logInfo("==========comiResult======"+ comiResult ,module);
            } catch (GenericServiceException gse) {
                Debug.logInfo("==========comiResult===2==="+ comiResult ,module);
                errorList.add("Error running method createOagisMessageInfo");
                String errMessageForcreateOagisMessageInfo = gse.getMessage();
                Debug.logError(gse, errMessageForcreateOagisMessageInfo, module);
            }
        } catch (Exception e) {
            String errMessageReceiveInventoryProduct = e.getMessage();
            Debug.logError(e, errMessageReceiveInventoryProduct, module);
            errorList.add("Error During Entity Interaction");
        }
        StringBuffer successString = new StringBuffer();
        if (errorList.size() > 0) {
            Iterator errorListIter = errorList.iterator();
            while (errorListIter.hasNext()) {
                String errorMsg = (String) errorListIter.next();
                successString.append(errorMsg);
                if (errorListIter.hasNext()) {
                    successString.append(", ");
                }
            }
            try {
                 if(successString.length() > 0){
                    //send confirm bod
                    Map scbCtx = FastMap.newInstance();
                    Debug.logInfo("==========scbCtx======",module);
                    scbCtx = dispatcher.runSync("sendConfirmBod",sendConfirmBodCtx );
                 }
            } catch(GenericServiceException gse) {    
                String errMessageForsendConfirmBod = gse.getMessage();
                Debug.logError(gse, errMessageForsendConfirmBod, module);
            }    
        }
        Map result = ServiceUtil.returnSuccess("Action Performed Successfully");
        result.put("contentType", "text/plain");
        return result;
    }
    
    public static Map receiveRmaAcknowledge(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
       // GenericValue userLogin = (GenericValue) context.get("userLogin");
        List errorList = FastList.newInstance();
        //Map for confirmBod service
        Map sendConfirmBodCtx = FastMap.newInstance();
        
        try {
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
            // parse the message 
            Document doc = UtilXml.readXmlDocument(in, true, "receiveRmaAcknowledge");
            Element receivePoElement = doc.getDocumentElement();
            receivePoElement.normalize();
            Element docCtrlAreaElement = UtilXml.firstChildElement(receivePoElement, "N1:CNTROLAREA");
            Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:SENDER");
            Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "N1:BSR");
            
            String bsrVerb = UtilXml.childElementValue(docBsrElement, "N2:VERB");
            String bsrNoun = UtilXml.childElementValue(docBsrElement, "N2:NOUN");
            String bsrRevision = UtilXml.childElementValue(docBsrElement, "N2:REVISION");
            
            String logicalId = UtilXml.childElementValue(docSenderElement, "N2:LOGICALID");
            String component = UtilXml.childElementValue(docSenderElement, "N2:COMPONENT");
            String task = UtilXml.childElementValue(docSenderElement, "N2:TASK");
            String referenceId = UtilXml.childElementValue(docSenderElement, "N2:REFERENCEID");
            String confirmation = UtilXml.childElementValue(docSenderElement, "N2:CONFIRMATION");
//            String language = UtilXml.childElementValue(docSenderElement, "N2:LANGUAGE");
//            String codePage = UtilXml.childElementValue(docSenderElement, "N2:CODEPAGE");
            String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
            Element dataAreaElement = UtilXml.firstChildElement(receivePoElement, "n:DATAAREA");
            Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "n:ACKNOWLEDGE_DELIVERY");
            Element receiptHdrElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "n:RECEIPTHDR");
            Element qtyElement = UtilXml.firstChildElement(receiptHdrElement, "N1:QUANTITY");
            
            String itemQty = UtilXml.childElementValue(qtyElement, "N2:VALUE");
            String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
            String sku = UtilXml.childElementValue(receiptHdrElement, "N2:ITEM");
            String receivedDate = UtilXml.childElementValue(receiptHdrElement, "N1:DATETIME");
            
            Element invDetailElement = UtilXml.firstChildElement(receiptHdrElement, "n:INVDETAIL");
            
            String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
            String invItemStatus = UtilXml.childElementValue(receiptHdrElement, "N2:DISPOSITN");
            
            Element documentRefElement = UtilXml.firstChildElement(receiptHdrElement, "N1:DOCUMNTREF");
            
            String orderTypeId = UtilXml.childElementValue(documentRefElement, "N2:DOCTYPE");
            String returnId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");
            // String lineNum = UtilXml.childElementValue(documentRefElement, "N2:LINENUM");

            GenericValue returnHeader = null;
            
            //Map Declaration
            Map urhCtx = new HashMap();
            Map comiCtx = new HashMap();
            Map cipCtx = new HashMap();
            Map urhResult = null;
            
            String orderId = null;
            
            if (returnId != null) {
                returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                if (returnHeader.getString("statusId").equals("RETURN_REQUESTED")) {
                    urhCtx.put("returnId", returnId);
                    urhCtx.put("statusId", "RETURN_COMPLETED");
                    urhCtx.put("userLogin", userLogin);
                    try {
                      urhResult = dispatcher.runSync("updateReturnHeader", urhCtx);
                      Debug.logInfo("==============urhResult===== " + urhResult, module);
                     
                    } catch (GenericServiceException gse) {
                        errorList.add("Error running method receiveInventoryProduct");
                        Debug.logInfo("==========urhResult======"+ urhResult, module);
                        String errMessageForcreateOagisMessageInfo = "Error Running Service sendConfirmBod";
                        Debug.logError(gse, errMessageForcreateOagisMessageInfo, module);  
                    }
                    GenericValue returnItem = EntityUtil.getFirst(delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId)));
                    orderId = returnItem.getString("orderId");
                }
            }
            Debug.logInfo("==========orderId======"+ orderId, module);
            String inventoryItemTypeId = null;
            if (serialNumber == null) {
                inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
            }
            else {
                
                inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                cipCtx.put("serialNumber", serialNumber);
            }
            // sign handling for items
            double quantityAccepted = 0.0;
            double quantityRejected = 0.0;
            if (sign.equals("+")) {
                quantityAccepted = Double.parseDouble(itemQty);
                quantityRejected= 0.0;
            } else {
                  quantityRejected = Double.parseDouble(itemQty);
                  quantityAccepted = 0.0;
            }
            GenericValue inventoryItem = null;
            inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNumber)));
            Debug.logInfo("==============inventoryItem===== " + inventoryItem , module);
            String productId = inventoryItem.getString("productId");
            if (productId.compareTo(sku) != 0) {
                productId = sku;
            }
            if ( invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
                cipCtx.put("statusId", "INV_AVAILABLE");    
            } else if ( invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable") ) {
                cipCtx.put("statusId", "INV_ON_HOLD");
            }
            Debug.logInfo("==========cipCtx======"+ cipCtx, module);
           
            // prepare Map for ConfirmBod Service
            sendConfirmBodCtx.put("logicalId",logicalId);
            sendConfirmBodCtx.put("component",component);
            sendConfirmBodCtx.put("task",task);
            sendConfirmBodCtx.put("referenceId",referenceId);
            sendConfirmBodCtx.put("userLogin",userLogin);
            sendConfirmBodCtx.put("returnId",returnId);
            Debug.logInfo("==========sendConfirmBodCtx======"+ sendConfirmBodCtx, module);
            
            //prepare MAp for receiveInventoryProduct service
            String facilityId = "WebStoreWarehouse";
            String locationSeqId = "FAC_AVNET_AZ";
            cipCtx.put("facilityId", facilityId);
            cipCtx.put("locationSeqId", locationSeqId);
            cipCtx.put("productId", productId);
            cipCtx.put("inventoryItemTypeId", inventoryItemTypeId);
            cipCtx.put("quantityAccepted", new Double(quantityAccepted));
            cipCtx.put("quantityRejected", new Double(quantityRejected));
            cipCtx.put("userLogin", userLogin);
            cipCtx.put("ownerPartyId", "DemoCustomer");
            cipCtx.put("orderId", orderId);
            
            Map riResult = null;
            Map comiResult = null;
            
            try {
                Debug.logInfo("==========cipCtx======"+ cipCtx, module);
                riResult = dispatcher.runSync("receiveInventoryProduct", cipCtx);
                Debug.logInfo("==========riResult======"+ riResult, module);
            } catch (GenericServiceException gse) {
                errorList.add("Error running method receiveInventoryProduct");
                String errMessageForreceiveInventoryProduct = gse.getMessage();
                Debug.logInfo("==========errorList======"+ errorList, module);
                Debug.logError(gse, errMessageForreceiveInventoryProduct, module);
            }

            // prepare map to store BOD informatio  
            Timestamp timestamp = null;
            timestamp = UtilDateTime.nowTimestamp();
            
            comiCtx.put("logicalId", logicalId);
            comiCtx.put("authId", authId);
            comiCtx.put("referenceId", referenceId);
            comiCtx.put("receivedDate", timestamp);
            comiCtx.put("component", component);
            comiCtx.put("task", task);  
            comiCtx.put("outgoingMessage", "N");
            comiCtx.put("confirmation", confirmation);
            comiCtx.put("bsrVerb", bsrVerb);
            comiCtx.put("bsrNoun", bsrNoun);
            comiCtx.put("bsrRevision", bsrRevision);
            comiCtx.put("processingStatusId", "RETURN_COMPLETED");        
            comiCtx.put("returnId", returnId);
            comiCtx.put("orderId", orderId);
            comiCtx.put("userLogin", userLogin);
            try {
                comiResult = dispatcher.runSync("createOagisMessageInfo", comiCtx);
                    Debug.logInfo("==========result======"+ comiResult, module);
            } catch (GenericServiceException gse) {
                errorList.add("Error running method createOagisMessageInfo");
                String errMessageForcreateOagisMessageInfo = gse.getMessage();
                Debug.logError(gse, errMessageForcreateOagisMessageInfo, module);
            }
        } catch (Exception e) {
            String errMessage = e.getMessage();
            Debug.logError(e, errMessage, module);
            errorList.add("Error During Entity Interaction");
        }
        StringBuffer successString = new StringBuffer();
        if (errorList.size() > 0) {
            Iterator errorListIter = errorList.iterator();
            while (errorListIter.hasNext()) {
                String errorMsg = (String) errorListIter.next();
                successString.append(errorMsg);
                if (errorListIter.hasNext()) {
                    successString.append(", ");
                }
            }
            try {
                 if(successString.length() > 0){
                    //send confirm bod
                    Map scbCtx = FastMap.newInstance();
                    Debug.logInfo("==========scbCtx======"+ scbCtx,module);
                    scbCtx = dispatcher.runSync("sendConfirmBod",sendConfirmBodCtx );
                 }
            } catch(GenericServiceException gse) {    
                String errMessageForsendConfirmBod = gse.getMessage();
                Debug.logError(gse, errMessageForsendConfirmBod, module);
            }    
        }
        Map result = ServiceUtil.returnSuccess("Action Performed Successfully");
        result.put("contentType", "text/plain");
        return result;
    }
}
