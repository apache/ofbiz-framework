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
import java.io.IOException;
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

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;




public class OagisInventoryServices {
    
    public static final String module = OagisInventoryServices.class.getName();
    
    public static Map syncInventory(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        //create a List for Storing  error Information 
        List errorList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
            } catch (GenericEntityException e){
                String errMsg = "Error Getting UserLogin with userLoginId 'admin'";
                Debug.logError(e, errMsg, module);
            }
        }

        Document doc = null;
        try {
            doc = UtilXml.readXmlDocument(in, true, "SyncInventory");
        } catch (SAXException e) {
            String errMsg = "Error parsing the SyncInventoryResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing the SyncInventoryResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "Error parsing the SyncInventoryResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
        
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
        String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
        Element dataAreaElement = UtilXml.firstChildElement(receiveInventoryElement, "n:DATAAREA");
        Element dataAreaSyncInventoryElement = UtilXml.firstChildElement(dataAreaElement, "n:SYNC_INVENTORY");
        Element dataAreaInventoryElement = UtilXml.firstChildElement(dataAreaSyncInventoryElement, "n:INVENTORY");
            
        Element dataAreaQuantityElement = UtilXml.firstChildElement(dataAreaInventoryElement, "N1:QUANTITY");
            
        String value = UtilXml.childElementValue(dataAreaQuantityElement, "N2:VALUE");
        String sign = UtilXml.childElementValue(dataAreaQuantityElement, "N2:SIGN");
        String uom = UtilXml.childElementValue(dataAreaQuantityElement, "N2:UOM");
        String item = UtilXml.childElementValue(dataAreaQuantityElement, "N2:ITEM");
        String itemStatus = UtilXml.childElementValue(dataAreaQuantityElement, "N2:ITEMSTATUS");
            
        double quantityAccepted ;
        double quantityRejected ;
            
        if ( sign.equals("+")) {
            quantityAccepted = Double.parseDouble(value);
            quantityRejected = 0.0 ;
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

        // create a Map for getting result of service createOagisMessageInfo
        try {
            //service for creating OagisMessageInfo  
            Map oagisMessageInfoResult = dispatcher.runSync("createOagisMessageInfo", oagisMessageInfoCtx);
            if (ServiceUtil.isError(oagisMessageInfoResult)){
                String errMsg = "Error creating OagisMessageInfo";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch(GenericServiceException gse) {
            errorList.add("Error Running Service createOagisMessageInfo");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }
            
        //create a Map for getting result of service getProductInventoryAvailable
        Map gpiaResult = FastMap.newInstance();
        try {
            gpiaResult = dispatcher.runSync("getProductInventoryAvailable", UtilMisc.toMap("productId", item) );
            if (ServiceUtil.isError(gpiaResult)){
                String errMsg = "Error running service getProductInventoryAvailable";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch(GenericServiceException gse) {
            errorList.add("Error Running Service getProductInventoryAvailable");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }
            
        String availableToPromiseTotal = gpiaResult.get("availableToPromiseTotal").toString();
        // create a Map for getting result of service receiveInventoryProduct
        try {
            if (value.equals(availableToPromiseTotal) ) {   
                Debug.logInfo("==========Both Values are same  =====",module);
            } else {
                //sevice for receiveInventoryProduct in InventoryItem
                Map receiveInventoryProductResult = dispatcher.runSync("receiveInventoryProduct",receiveInventoryCtx );
                if (ServiceUtil.isError(receiveInventoryProductResult)){
                    String errMsg = "Error running service receiveInventoryProduct";
                    errorList.add(errMsg);
                    Debug.logError(errMsg, module);
                }
            }
        } catch(GenericServiceException gse) {
            errorList.add("Error Running Service receiveInventoryProduct");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }
        
        //create List for Getting FacilityContactMech
        String contactMechId = null;
        String emailString = null;
        String contactMechTypeId = null;
        GenericValue contactMech = null;
        GenericValue facilityContactMech=null;
        List facilityContactMechs = FastList.newInstance();
        try {
            facilityContactMechs = delegator.findByAnd("FacilityContactMech", UtilMisc.toMap("facilityId", "WebStoreWarehouse"));    
        } catch (GenericEntityException e){
            String errMsg = "Error Getting FacilityContactMech ";
            Debug.logError(e, errMsg, module);
        }
        
        Iterator fcmIter  = facilityContactMechs.iterator();
        while(fcmIter.hasNext()) {
            facilityContactMech = (GenericValue) fcmIter.next();
            contactMechId = facilityContactMech.getString("contactMechId");
            try {
                contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
            } catch (GenericEntityException e){
                String errMsg = "Error Getting ContactMech ";
                Debug.logError(e, errMsg, module);
            }
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
        try {
            psesMap  =   delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", "9001", "emailType", "PRDS_OAGIS_CONFIRM"));
        } catch (GenericEntityException e){
            String errMsg = "Error Getting Entity ProductStoreEmailSetting";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
        
        
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
            notifyCtx.put("sendTo", "sahujeetendra@gmail.com" );
            try {
                //service for sending emailNotification
                Map smfsResult = dispatcher.runSync("sendMailFromScreen", notifyCtx);
                if (ServiceUtil.isError(smfsResult)){
                    String errMsg = "Error running service sendMailFromScreen";
                    errorList.add(errMsg);
                    Debug.logError(errMsg, module);
                }
            } catch(GenericServiceException gse) {
                errorList.add("Error Running Service sendMailFromScreen");
                String errMsg = gse.getMessage();
                Debug.logError(gse, errMsg, module);
            }
        }
        
        Map result = new HashMap();
        result.put("contentType", "text/plain");
        if (errorList.size() > 0) {
                result.putAll(oagisMessageInfoCtx);
                String errMsg = "Error Processing Received Messages";
                result.put("reasonCode", "1000");
                result.put("description", errMsg);
                //result.putAll(ServiceUtil.returnError(errMsg));
                return result;
        }
        result.putAll(ServiceUtil.returnSuccess("Action Performed Successfully"));
        return result;
    }
    
    public static Map receivePoAcknowledge(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        List errorList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        
        Document doc = null;
        try {
            doc = UtilXml.readXmlDocument(in, true, "ReceivePoAcknowledge");
        } catch (SAXException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
        
        // parse the message 
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
        String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
        Element dataAreaElement = UtilXml.firstChildElement(receivePoElement, "n:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "n:ACKNOWLEDGE_DELIVERY");
        Element receiptHdrElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "n:RECEIPTHDR");
        Element qtyElement = UtilXml.firstChildElement(receiptHdrElement, "N1:QUANTITY");
            
        String itemQty = UtilXml.childElementValue(qtyElement, "N2:VALUE");
        String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
        String productId = UtilXml.childElementValue(receiptHdrElement, "N2:ITEM");
            
        Element invDetailElement = UtilXml.firstChildElement(receiptHdrElement, "n:INVDETAIL");
            
        String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
            
        Element documentRefElement = UtilXml.firstChildElement(receiptHdrElement, "N1:DOCUMNTREF");
        String orderId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");

        // prepare map to create inventory against PO
        Map cipCtx = new HashMap();
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
            
        //prepare Map for receiveInventoryProduct
        cipCtx.put("facilityId", "WebStoreWarehouse");
        cipCtx.put("productId", productId);
        cipCtx.put("inventoryItemTypeId", inventoryItemTypeId);
        cipCtx.put("quantityAccepted", new Double(quantityAccepted));
        cipCtx.put("quantityRejected", new Double(quantityRejected));
        cipCtx.put("userLogin", userLogin);
        cipCtx.put("orderId", orderId);
        
        try {
            Map riResult = dispatcher.runSync("receiveInventoryProduct", cipCtx);
            if (ServiceUtil.isError(riResult)){
                String errMsg = "Error running service receiveInventoryProduct";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch (GenericServiceException gse) {
            errorList.add("Error running service receiveInventoryProduct");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }
            
        //prepare result Map for createOagisMessageinfo
        Timestamp timestamp = null;
        timestamp = UtilDateTime.nowTimestamp();
        Map comiCtx = new HashMap();
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
            Map comiResult = dispatcher.runSync("createOagisMessageInfo", comiCtx);
            if (ServiceUtil.isError(comiResult)){
                String errMsg = "Error creating OagisMessageInfo for the Incoming Message";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch (GenericServiceException gse) {
            errorList.add("Error running method createOagisMessageInfo");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }

        Map result = new HashMap();
        result.put("contentType", "text/plain");
        if (errorList.size() > 0) {
            result.putAll(comiCtx);
            String errMsg = "Error Processing Received Messages";
            result.put("reasonCode", "1000");
            result.put("description", errMsg);
            //result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        
        return result;
    }
    
    public static Map receiveRmaAcknowledge(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        List errorList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        
        Document doc = null;
        try {
            doc = UtilXml.readXmlDocument(in, true, "ReceiveRmaAcknowledge");
        } catch (SAXException e) {
            String errMsg = "Error parsing the ReceiveRmaAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing the ReceiveRmaAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "Error parsing the ReceiveRmaAcknowledgeResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
        
        // parse the message 
        Element receiveRmaElement = doc.getDocumentElement();
        receiveRmaElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(receiveRmaElement, "N1:CNTROLAREA");
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
        String authId = UtilXml.childElementValue(docSenderElement, "N2:AUTHID");
            
        Element dataAreaElement = UtilXml.firstChildElement(receiveRmaElement, "n:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "n:ACKNOWLEDGE_DELIVERY");
        Element receiptHdrElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "n:RECEIPTHDR");
        Element qtyElement = UtilXml.firstChildElement(receiptHdrElement, "N1:QUANTITY");
            
        String itemQty = UtilXml.childElementValue(qtyElement, "N2:VALUE");
        String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
        String sku = UtilXml.childElementValue(receiptHdrElement, "N2:ITEM");
            
        Element invDetailElement = UtilXml.firstChildElement(receiptHdrElement, "n:INVDETAIL");
            
        String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
        String invItemStatus = UtilXml.childElementValue(receiptHdrElement, "N2:DISPOSITN");
            
        Element documentRefElement = UtilXml.firstChildElement(receiptHdrElement, "N1:DOCUMNTREF");
            
        //String orderTypeId = UtilXml.childElementValue(documentRefElement, "N2:DOCTYPE");
        String returnId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");
            
        //Map Declaration
        Map urhCtx = new HashMap();
        String orderId = null;
        if (returnId != null) {
            GenericValue returnHeader = null;
            try {
                returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            } catch (GenericEntityException e){
                String errMsg = "Error Getting ReturnHeader ";
                Debug.logError(e, errMsg, module);
            }
            if (returnHeader.getString("statusId").equals("RETURN_ACCEPTED")) {
                urhCtx.put("returnId", returnId);
                urhCtx.put("statusId", "RETURN_COMPLETED");
                urhCtx.put("userLogin", userLogin);
                try {
                      Map urhResult = dispatcher.runSync("updateReturnHeader", urhCtx);
                      if (ServiceUtil.isError(urhResult)){
                          String errMsg = "Error running service updateReturnHeader";
                          errorList.add(errMsg);
                          Debug.logError(errMsg, module);
                      }
                } catch (GenericServiceException gse) {
                    String errMsg = "Error running service updateReturnHeader";
                    errorList.add(errMsg);
                    Debug.logError(gse, errMsg, module);  
                }
                try {
                    GenericValue returnItem = EntityUtil.getFirst(delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId)));
                    orderId = returnItem.getString("orderId");
                } catch (GenericEntityException e){
                    String errMsg = "Error Getting Entity ReturnItem";
                    Debug.logError(e, errMsg, module);
                }
            }
        }
        
        String inventoryItemTypeId = null;
        Map cipCtx = new HashMap();
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
        try {
            inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNumber)));
        } catch (GenericEntityException e){
            String errMsg = "Error Getting Entity InventoryItem";
            Debug.logError(e, errMsg, module);
        }
            
        String productId = inventoryItem.getString("productId");
        if (productId.compareTo(sku) != 0) {
            productId = sku;
        }
        if(serialNumber == null) { 
            productId = sku;
        }
        if ( invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
            cipCtx.put("statusId", "INV_AVAILABLE");    
        } else if ( invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable") ) {
            cipCtx.put("statusId", "INV_ON_HOLD");
        }

        String facilityId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.ReturnReceiptFacilityId");
        String locationSeqId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.ReturnReceiptLocationSeqId");
           
        //prepare MAp for receiveInventoryProduct service
        cipCtx.put("facilityId", facilityId);
        cipCtx.put("locationSeqId", locationSeqId);
        cipCtx.put("productId", productId);
        cipCtx.put("inventoryItemTypeId", inventoryItemTypeId);
        cipCtx.put("quantityAccepted", new Double(quantityAccepted));
        cipCtx.put("quantityRejected", new Double(quantityRejected));
        cipCtx.put("userLogin", userLogin);
            
        try {
            Map riResult = dispatcher.runSync("receiveInventoryProduct", cipCtx);
            if (ServiceUtil.isError(riResult)){
                String errMsg = "Error running service receiveInventoryProduct";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch (GenericServiceException gse) {
            errorList.add("Error running service receiveInventoryProduct");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }

            //prepare result Map for createOagisMessageinfo
            
        Timestamp timestamp = null;
        timestamp = UtilDateTime.nowTimestamp();
        Map comiCtx = new HashMap(); 
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
            Map comiResult = dispatcher.runSync("createOagisMessageInfo", comiCtx);
            if (ServiceUtil.isError(comiResult)){
                String errMsg = "Error creating OagisMessageInfo for the Incoming Message";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch (GenericServiceException gse) {
            errorList.add("Error running method createOagisMessageInfo");
            String errMsg = gse.getMessage();
            Debug.logError(gse, errMsg, module);
        }
        
        Map result = new HashMap();
        result.put("contentType", "text/plain");
        if (errorList.size() > 0) {
            result.putAll(comiCtx);
            String errMsg = "Error Processing Received Messages";
            result.put("reasonCode", "1000");
            result.put("description", errMsg);
            //result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        
        return result;
    }
}
