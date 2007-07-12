/*
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
 */
package org.ofbiz.oagis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class OagisInventoryServices {
    
    public static final String module = OagisInventoryServices.class.getName();
    
    public static final Double doubleZero = new Double(0.0);
    public static final Double doubleOne = new Double(1.0);
    
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
        List errorMapList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        
        Document doc = null;
        try {
            doc = UtilXml.readXmlDocument(in, true, "ReceivePoAcknowledge");
        } catch (SAXException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledge message: " + e.toString();
            errorMapList.add(UtilMisc.toMap("SAXException", errMsg));
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledge message: " + e.toString();
            errorMapList.add(UtilMisc.toMap("ParserConfigurationException", errMsg));
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "Error parsing the ReceivePoAcknowledge message: " + e.toString();
            errorMapList.add(UtilMisc.toMap("IOException", errMsg));
            Debug.logError(e, errMsg, module);
        }
        
        // TODO: if errorList is not empty, return here
        
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

        String inventoryItemTypeId = null;
        String orderId = null;
        String facilityId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.PoReceiptFacilityId");
        
        // get RECEIPTLN elements from message
        List acknowledgeElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "n:RECEIPTLN");
        if (UtilValidate.isNotEmpty(acknowledgeElementList)) {
        	Iterator acknowledgeElementIter = acknowledgeElementList.iterator();
        	while (acknowledgeElementIter.hasNext()) {
                Map ripCtx = FastMap.newInstance();
                Element receiptLnElement = (Element) acknowledgeElementIter.next();
                Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "N1:QUANTITY");
                
                String itemQtyStr = UtilXml.childElementValue(qtyElement, "N2:VALUE");
                double itemQty = Double.parseDouble(itemQtyStr);
                String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
                
                String productId = UtilXml.childElementValue(receiptLnElement, "N2:ITEM");
                
                Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "N1:DOCUMNTREF");
                orderId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");
                ripCtx.put("orderId", orderId);
                
                String orderItemSeqId = UtilXml.childElementValue(documentRefElement, "N2:LINENUM");
                ripCtx.put("orderItemSeqId", orderItemSeqId);
                
                // get inventory item status
                String invItemStatus = UtilXml.childElementValue(receiptLnElement, "N2:DISPOSITN");
                if ( invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
                    ripCtx.put("statusId", "INV_AVAILABLE");    
                } else if ( invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable") ) {
                    ripCtx.put("statusId", "INV_ON_HOLD");
                }
                // get the serial number(s) 
                List serialNumsList = FastList.newInstance();
                List invDetailList = UtilXml.childElementList(receiptLnElement, "n:INVDETAIL");
                if (UtilValidate.isNotEmpty(invDetailList)) {
                    inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                    ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);
                    for (Iterator j = invDetailList.iterator(); j.hasNext();) {
                        Element invDetailElement = (Element) j.next();
                        String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
                        serialNumsList.add(serialNumber);
                    }

                    /* DEJ20070711 Commenting this out because it shouldn't happen, ie more likely the ITEM element will be filled 
                     * than INVDETAIL->SERIALNUM, and this isn't a reliable way to look it up (may be more than 1 record for a given 
                     * serialNumber for different products 
                    // this is a Serialized Inventory Item. If the productId from the message is not valid then lets read it from InventoryItem in Ofbiz database.
                    if (productId == null || "".equals(productId)) {
                        try {
                            GenericValue inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNumber)));
                            if (inventoryItem !=null) {
                                productId = inventoryItem.getString("productId");
                            }
                        } catch (GenericEntityException e){
                            String errMsg = "Error Getting Entity InventoryItem";
                            Debug.logError(e, errMsg, module);
                        }
                    } */
                } else {
                    inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
                    ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);
                }
                ripCtx.put("productId", productId);
                ripCtx.put("facilityId",facilityId);
                ripCtx.put("userLogin", userLogin);

                // sign handling for items
                double quantityAccepted = 0.0;
                double quantityRejected = 0.0;
                if (sign.equals("+")) {
                    quantityAccepted = itemQty;
                    quantityRejected= 0.0;
                } else {
                    quantityRejected = itemQty;
                    quantityAccepted = 0.0;
                }
                if (quantityAccepted > 0) {
                	if (serialNumsList.size() > 0) {
                		if (serialNumsList.size() != quantityAccepted) {
                			// this is an error, do something about it, like add to the list to send back a Confirm BOD with error messages
                			String errMsg = "Error: the quantity [" + quantityAccepted + "] did not match the number of serial numbers passed [" + serialNumsList.size() + "].";
                			errorMapList.add(UtilMisc.toMap("QuantitySerialMismatch", errMsg));
                		}
                		
                    	Iterator serialNumIter = serialNumsList.iterator();
                    	while (serialNumIter.hasNext()) {
                    		String serialNum = (String) serialNumIter.next();
                    		
                            // clone the context as it may be changted in the call
                    		Map localRipCtx = FastMap.newInstance();
                            localRipCtx.putAll(ripCtx);
                            
                            localRipCtx.put("quantityAccepted", new Double(1.0));
                            // always set this to 0, if needed we'll handle the rejected quantity separately
                            localRipCtx.put("quantityRejected", new Double(0.0));

                            localRipCtx.put("serialNumber", serialNum);
                            
                            try {
                                Map ripResult = dispatcher.runSync("receiveInventoryProduct", localRipCtx);
                                if (ServiceUtil.isError(ripResult)) {
                                	String errMsg = ServiceUtil.getErrorMessage(ripResult);
                        			errorMapList.add(UtilMisc.toMap("ReceiveInventoryServiceError", errMsg));
                                }
                            } catch (GenericServiceException e) {
                                String errMsg = "Error running service receiveInventoryProduct: " + e.toString();
                    			errorMapList.add(UtilMisc.toMap("GenericServiceException", errMsg));
                                Debug.logError(e, errMsg, module);
                            }    
                    	}
                	} else {
                		// no serial numbers, just receive the quantity
                        
                		// clone the context as it may be changted in the call
                		Map localRipCtx = FastMap.newInstance();
                        localRipCtx.putAll(ripCtx);
                        
                        localRipCtx.put("quantityAccepted", new Double(quantityAccepted));
                        // always set this to 0, if needed we'll handle the rejected quantity separately
                        localRipCtx.put("quantityRejected", new Double(0.0));
                        
                        try {
                            Map ripResult = dispatcher.runSync("receiveInventoryProduct", localRipCtx);
                            if (ServiceUtil.isError(ripResult)) {
                            	String errMsg = ServiceUtil.getErrorMessage(ripResult);
                    			errorMapList.add(UtilMisc.toMap("ReceiveInventoryServiceError", errMsg));
                            }
                        } catch (GenericServiceException e) {
                            String errMsg = "Error running service receiveInventoryProduct: " + e.toString();
                			errorMapList.add(UtilMisc.toMap("GenericServiceException", errMsg));
                            Debug.logError(e, errMsg, module);
                        }    
                	}
                } else {
                    // TODOLATER: need to run service receiveInventoryProduct and updateInventoryItem when quantityRejected > 0
                	// NOTE DEJ20070711 this shouldn't happen for current needs, so save for later
                }
            }
        }         
        //prepare result Map for createOagisMessageinfo
        
        Timestamp timestamp = null;
        timestamp = UtilDateTime.nowTimestamp();
        Map comiCtx = FastMap.newInstance();
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
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            // TODO: reconsider sending this error back to other server, not much they can do about it, and it may not be a critical error causing the message to be rejected...
			errorMapList.add(UtilMisc.toMap("CreateOagisMessageInfoError", errMsg));
            Debug.logError(e, errMsg, module);
        }

        if (errorMapList.size() > 0) {
        	Map result = ServiceUtil.returnError("Errors found processing message");
            result.put("contentType", "text/plain");
            result.putAll(comiCtx);
            result.put("errorMapList", errorMapList);
            //result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        return ServiceUtil.returnSuccess();
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

        // TODO: need to support multiple RECEIPTLN elements in a single message
        Element receiptLnElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "n:RECEIPTLN");
        Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "N1:QUANTITY");
            
        String itemQty = UtilXml.childElementValue(qtyElement, "N2:VALUE");
        String sign = UtilXml.childElementValue(qtyElement, "N2:SIGN");
        String sku = UtilXml.childElementValue(receiptLnElement, "N2:ITEM");
            
        // TODO: need to support multiple INVDETAIL elements under a RECEIPTLN element
        Element invDetailElement = UtilXml.firstChildElement(receiptLnElement, "n:INVDETAIL");
        String serialNumber = UtilXml.childElementValue(invDetailElement, "N2:SERIALNUM");
        
        String invItemStatus = UtilXml.childElementValue(receiptLnElement, "N2:DISPOSITN");
            
        Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "N1:DOCUMNTREF");
        //String orderTypeId = UtilXml.childElementValue(documentRefElement, "N2:DOCTYPE");
        String returnId = UtilXml.childElementValue(documentRefElement, "N2:DOCUMENTID");
        // TODO: need to get and if possible use the DOCUMNTREF -> LINENUM element, would be orderItemSeqId
            
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
        if (serialNumber == null || serialNumber.equals("")) {
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }
        else {
        	// this is a Serialized Inventory Item. If the productId from the message is not valid then lets read it from InventoryItem in Ofbiz database.
            inventoryItemTypeId = "SERIALIZED_INV_ITEM";
            cipCtx.put("serialNumber", serialNumber);
            if (sku == null || sku.equals("")) {
                try {
                	GenericValue inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNumber)));
                    if (inventoryItem !=null){
                    	sku = inventoryItem.getString("productId");
                    }
                } catch (GenericEntityException e){
                    String errMsg = "Error Getting Entity InventoryItem";
                    Debug.logError(e, errMsg, module);
                }
            }
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
        cipCtx.put("productId", sku);
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
