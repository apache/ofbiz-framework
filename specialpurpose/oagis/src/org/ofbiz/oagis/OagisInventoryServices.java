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
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OagisInventoryServices {
    
    public static final String module = OagisInventoryServices.class.getName();
    
    public static final Double doubleZero = new Double(0.0);
    public static final Double doubleOne = new Double(1.0);
    
    public static Map syncInventory(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        List errorMapList = FastList.newInstance();
        List inventoryMapList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e){
                String errMsg = "Error Getting UserLogin: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
        }

        String facilityId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.SyncInventoryFacilityId");
        
        Element syncInventoryRootElement = doc.getDocumentElement();
        syncInventoryRootElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(syncInventoryRootElement, "os:CNTROLAREA");
        Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:BSR");
        Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:SENDER");
        
        String bsrVerb = UtilXml.childElementValue(docBsrElement, "of:VERB");
        String bsrNoun = UtilXml.childElementValue(docBsrElement, "of:NOUN");
        String bsrRevision = UtilXml.childElementValue(docBsrElement, "of:REVISION");

        
        String logicalId = UtilXml.childElementValue(docSenderElement, "of:LOGICALID");
        String component = UtilXml.childElementValue(docSenderElement, "of:COMPONENT");
        String task = UtilXml.childElementValue(docSenderElement, "of:TASK");
        String referenceId = UtilXml.childElementValue(docSenderElement, "of:REFERENCEID");
        String confirmation = UtilXml.childElementValue(docSenderElement, "of:CONFIRMATION");
        String authId = UtilXml.childElementValue(docSenderElement, "of:AUTHID");
        
        // data area elements
        List dataAreaList = UtilXml.childElementList(syncInventoryRootElement, "ns:DATAAREA");
        if (UtilValidate.isNotEmpty(dataAreaList)) {
            Iterator dataAreaIter = dataAreaList.iterator();
            while (dataAreaIter.hasNext()) {
                Element dataAreaElement = (Element) dataAreaIter.next();
                Element syncInventoryElement = UtilXml.firstChildElement(dataAreaElement, "ns:SYNC_INVENTORY");
                Element inventoryElement = UtilXml.firstChildElement(syncInventoryElement, "ns:INVENTORY");

                Element quantityElement = UtilXml.firstChildElement(inventoryElement, "os:QUANTITY");
                
                String itemQtyStr = UtilXml.childElementValue(quantityElement, "of:VALUE");
                double itemQty = Double.parseDouble(itemQtyStr);
                /* TODO sign denoted whether quantity is accepted(+) or rejected(-), which plays role in receiving inventory
                 * In this message will it serve any purpose, since it is not handled.
                 */
                String sign = UtilXml.childElementValue(quantityElement, "of:SIGN");
                // TODO: Not used now, Later we may need it
                //String uom = UtilXml.childElementValue(quantityElement, "of:UOM");
                String productId = UtilXml.childElementValue(inventoryElement, "of:ITEM");
                String itemStatus = UtilXml.childElementValue(inventoryElement, "of:ITEMSTATUS");
                
                // if anything but "NOTAVAILABLE" set to available
                boolean isAvailable = !"NOTAVAILABLE".equals(itemStatus);
                String statusId = "INV_AVAILABLE";
                if (!isAvailable) {
                    statusId = "INV_ON_HOLD"; 
                }
                
                String snapshotDateStr = UtilXml.childElementValue(inventoryElement, "os:DATETIMEISO");  
                //Parse this into a valid Timestamp Object
                Timestamp snapshotDate = OagisServices.parseIsoDateString(snapshotDateStr, errorMapList);
                
                // get quantity on hand diff   
                double quantityOnHandTotal = 0.0;
                
                // only if looking for available inventory find the non-serialized QOH total
                if (isAvailable) {
                    EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                            new EntityExpr("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, snapshotDate), 
                            new EntityExpr("productId", EntityOperator.EQUALS, productId),
                            new EntityExpr("inventoryItemTypeId", EntityOperator.EQUALS, "NON_SERIAL_INV_ITEM"),
                            new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId)), EntityOperator.AND);
                    try {
                        List invItemAndDetails = delegator.findByCondition("InventoryItemDetailForSum", condition, UtilMisc.toList("quantityOnHandSum"), null);
                        Iterator invItemAndDetailIter = invItemAndDetails.iterator();
                        while (invItemAndDetailIter.hasNext()) {
                            GenericValue inventoryItemDetailForSum = (GenericValue) invItemAndDetailIter.next();
                            quantityOnHandTotal += inventoryItemDetailForSum.getDouble("quantityOnHandSum").doubleValue();
                        }
                    } catch (GenericEntityException e) {
                        String errMsg = "Error Getting Inventory Item And Detail: " + e.toString();
                        errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                        Debug.logError(e, errMsg, module);
                    }
                }

                // now regardless of AVAILABLE or NOTAVAILABLE check serialized inventory, just use the corresponding statusId as set above
                EntityCondition serInvCondition = new EntityConditionList(UtilMisc.toList(
                        new EntityExpr("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, snapshotDate),
                        new EntityExpr(new EntityExpr("statusEndDatetime", EntityOperator.GREATER_THAN, snapshotDate), EntityOperator.OR, new EntityExpr("statusEndDatetime", EntityOperator.EQUALS, null)),
                        new EntityExpr("productId", EntityOperator.EQUALS, productId),
                        new EntityExpr("statusId", EntityOperator.EQUALS, statusId),
                        new EntityExpr("inventoryItemTypeId", EntityOperator.EQUALS, "SERIALIZED_INV_ITEM"),
                        new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId)), EntityOperator.AND);
                try {
                    long invItemQuantCount = delegator.findCountByCondition("InventoryItemStatusForCount", serInvCondition, null);
                    quantityOnHandTotal += invItemQuantCount;
                } catch (GenericEntityException e) {
                    String errMsg = "Error Getting Inventory Item by Status Count: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }
                
                // check for mismatch in quantity
                if (itemQty != quantityOnHandTotal) {
                    double quantityDiff = Math.abs((itemQty - quantityOnHandTotal));
                    inventoryMapList.add(UtilMisc.toMap("productId", productId, "statusId", statusId, "quantityOnHandTotal", String.valueOf(quantityOnHandTotal), "quantityFromMessage", itemQtyStr, "quantityDiff", String.valueOf(quantityDiff), "timestamp", snapshotDate));
                }
            }
        }
        // send mail if mismatch(s) found
        if (inventoryMapList.size() > 0) {
            // prepare information to send mail
            Map sendMap = FastMap.newInstance();

            String sendToEmail = UtilProperties.getPropertyValue("oagis.properties", "oagis.notification.email.sendTo");
    
            /* DEJ20070802 changed to get email address from properties file, should be way easier to manage
            // get facility email address
            List facilityContactMechs = null;
            GenericValue contactMech = null;
            try {
                facilityContactMechs = delegator.findByAnd("FacilityContactMech", UtilMisc.toMap("facilityId", facilityId));    
            } catch (GenericEntityException e) {
                String errMsg = "Error Getting FacilityContactMech: " + e.toString();
                errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                Debug.logError(e, errMsg, module);
            }

            Iterator fcmIter  = facilityContactMechs.iterator();
            while(fcmIter.hasNext()) {
                GenericValue facilityContactMech = (GenericValue) fcmIter.next();
                String contactMechId = facilityContactMech.getString("contactMechId");
                try {
                    contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
                } catch (GenericEntityException e) {
                    String errMsg = "Error Getting ContactMech: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }
                String contactMechTypeId = contactMech.getString("contactMechTypeId");
                if (contactMechTypeId.equals("EMAIL_ADDRESS")) {
                    String emailString = contactMech.getString("infoString");
                    sendMap.put("sendTo", emailString);
                }
            }
            */
            
            if (UtilValidate.isNotEmpty(sendToEmail)) {
                String productStoreId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.SyncInventoryProductStoreId");
                GenericValue productStoreEmail = null;
                try {
                    productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", productStoreId, "emailType", "PRDS_OAGIS_CONFIRM"));
                } catch (GenericEntityException e) {
                    String errMsg = "Error Getting Entity ProductStoreEmailSetting: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }
                if (productStoreEmail != null) {
                    String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                    sendMap.put("bodyScreenUri", bodyScreenLocation);
                } else {
                    sendMap.put("bodyScreenUri", "component://oagis/widget/EmailOagisMessageScreens.xml#InventoryMismatchNotice");
                }
                if (locale == null) {
                    locale = Locale.getDefault();
                }

                sendMap.put("sendTo", sendToEmail);
                
                sendMap.put("subject", productStoreEmail.getString("subject"));
                sendMap.put("sendFrom", productStoreEmail.getString("fromAddress"));
                sendMap.put("sendCc", productStoreEmail.getString("ccAddress"));
                sendMap.put("sendBcc", productStoreEmail.getString("bccAddress"));
                sendMap.put("contentType", productStoreEmail.getString("contentType"));
                
                Map bodyParameters = UtilMisc.toMap("inventoryMapList", inventoryMapList, "locale", locale);
                sendMap.put("bodyParameters", bodyParameters);
                sendMap.put("userLogin", userLogin);
                
                // send the notification
                try {
                    // run async so it will happen in the background AND so errors in sending won't mess this up
                    dispatcher.runAsync("sendMailFromScreen", sendMap, true);
                } catch(Exception e) {
                    String errMsg = "Error Running Service sendMailFromScreen: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericServiceException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }
            } else {
                // no send to email address, just log to file
                Debug.logImportant("No sendTo email address found in process syncInventory service: inventoryMapList: " + inventoryMapList, module);
            }
        }
       
        // create oagis message info
        Map comiCtx= FastMap.newInstance();
        comiCtx.put("logicalId", logicalId);
        comiCtx.put("component", component);
        comiCtx.put("task", task);
        comiCtx.put("referenceId", referenceId);
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("authId", authId);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("receivedDate", UtilDateTime.nowTimestamp());
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("userLogin", userLogin);
        if (OagisServices.debugSaveXmlIn) {
            try {
                comiCtx.put("fullMessageXml", UtilXml.writeXmlDocument(doc));
            } catch (IOException e) {
                // this is just for debug info, so just log and otherwise ignore error
                String errMsg = "Warning: error creating text from XML Document for saving to database: " + e.toString();
                Debug.logWarning(errMsg, module);
            }
        }
        try {
            dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            /* now calling async for better error handling
            if (ServiceUtil.isError(comiResult)) {
                String errMsg = ServiceUtil.getErrorMessage(comiResult);
                errorMapList.add(UtilMisc.toMap("reasonCode", "CreateOagisMessageServiceError", "description", errMsg));
            }
            */
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            //errorMapList.add(UtilMisc.toMap("reasonCode", "CreateOagisMessageInfoError", "description", errMsg));
            Debug.logError(e, errMsg, module);
        }
        
        Map result = FastMap.newInstance();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        // check error list if there is any 
        if (errorMapList.size() > 0) {
            result.put("errorMapList", errorMapList);
            String errMsg = "Error Processing Received Messages";
            result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        result.putAll(ServiceUtil.returnSuccess("Action Performed Successfully"));
        return result;
    }
    
    public static Map receivePoAcknowledge(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        List errorMapList = FastList.newInstance();
        Map comiCtx = FastMap.newInstance();
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                String errMsg = "Error Getting UserLogin: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
        }
        
        // parse the message 
        Element receivePoElement = doc.getDocumentElement();
        receivePoElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(receivePoElement, "os:CNTROLAREA");
            
        Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:SENDER");
        Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:BSR");

        String bsrVerb = UtilXml.childElementValue(docBsrElement, "of:VERB");
        String bsrNoun = UtilXml.childElementValue(docBsrElement, "of:NOUN");
        String bsrRevision = UtilXml.childElementValue(docBsrElement, "of:REVISION");
        
        String logicalId = UtilXml.childElementValue(docSenderElement, "of:LOGICALID");
        String component = UtilXml.childElementValue(docSenderElement, "of:COMPONENT");
        String task = UtilXml.childElementValue(docSenderElement, "of:TASK"); // This field should be Not more then 10 char long
        String referenceId = UtilXml.childElementValue(docSenderElement, "of:REFERENCEID");
        String confirmation = UtilXml.childElementValue(docSenderElement, "of:CONFIRMATION");
        String authId = UtilXml.childElementValue(docSenderElement, "of:AUTHID");

        String sentDate = UtilXml.childElementValue(docCtrlAreaElement, "os:DATETIMEISO");
        Timestamp sentTimestamp = OagisServices.parseIsoDateString(sentDate, errorMapList);
        
        Element dataAreaElement = UtilXml.firstChildElement(receivePoElement, "ns:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");

        String facilityId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.PoReceiptFacilityId");
        String productId = null;
        // get RECEIPTLN elements from message
        List acknowledgeElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        if (UtilValidate.isNotEmpty(acknowledgeElementList)) {
            Iterator acknowledgeElementIter = acknowledgeElementList.iterator();
            while (acknowledgeElementIter.hasNext()) {
                Map ripCtx = FastMap.newInstance();
                Element receiptLnElement = (Element) acknowledgeElementIter.next();
                Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "os:QUANTITY");
                
                String itemQtyStr = UtilXml.childElementValue(qtyElement, "of:VALUE");
                double itemQty = Double.parseDouble(itemQtyStr);
                String sign = UtilXml.childElementValue(qtyElement, "of:SIGN");
                
                productId = UtilXml.childElementValue(receiptLnElement, "of:ITEM");
                
                Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "os:DOCUMNTREF");
                String orderId = UtilXml.childElementValue(documentRefElement, "of:DOCUMENTID");
                String orderTypeId = UtilXml.childElementValue(documentRefElement, "of:DOCTYPE");
                if(orderTypeId.equals("PO")) {
                    orderTypeId = "PURCHASE_ORDER";
                }
                
                String datetimeReceived = UtilXml.childElementValue(receiptLnElement, "os:DATETIMEISO");
                Timestamp timestampItemReceived = OagisServices.parseIsoDateString(datetimeReceived, errorMapList);
                ripCtx.put("datetimeReceived", timestampItemReceived);
                // Check reference to PO number, if exists
                GenericValue orderHeader = null;
                if(orderId != null) {
                    try {
                        List toStore = FastList.newInstance();
                        orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                        if (orderHeader != null) {
                            // Case : update the record 
                            ripCtx.put("orderId", orderId);
                            comiCtx.put("orderId", orderId);
                            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", orderId, "productId",productId,"quantity",new Double(itemQtyStr)));
                            delegator.setNextSubSeqId(orderItem,"orderItemSeqId", 5, 1);
                            delegator.create(orderItem);
                            ripCtx.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                        } else { 
                            // Case : New record entry when PO not exists in the Database
                            orderHeader =  delegator.makeValue("OrderHeader", UtilMisc.toMap("orderId", orderId, "orderTypeId",orderTypeId , 
                                    "orderDate", timestampItemReceived, "statusId", "ORDER_CREATED", "entryDate", UtilDateTime.nowTimestamp(),
                                    "productStoreId", UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.SyncInventoryProductStoreId","9001")));
                            toStore.add(orderHeader);
                            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", orderId , 
                                    "orderItemSeqId", UtilFormatOut.formatPaddedNumber(1L, 5) ,
                                    "productId",productId ,"quantity",new Double(itemQtyStr) ));
                            toStore.add(orderItem);
                            delegator.storeAll(toStore);
                        }
                    } catch (GenericEntityException e) {
                        String errMsg = "Error Getting OrderHeader: " + e.toString();
                        errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                        Debug.logError(e, errMsg, module);
                    }
                }
                // get inventory item status
                String invItemStatus = UtilXml.childElementValue(receiptLnElement, "of:DISPOSITN");
                if (invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
                    ripCtx.put("statusId","INV_AVAILABLE");    
                } else if (invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable") ) {
                    ripCtx.put("statusId","INV_ON_HOLD");
                }
                ripCtx.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
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
                ripCtx.put("quantityAccepted", new Double(quantityAccepted));
                ripCtx.put("quantityRejected", new Double(quantityRejected));
                try {
                    Map ripResult = dispatcher.runSync("receiveInventoryProduct", ripCtx);
                    if (ServiceUtil.isError(ripResult)) {
                        String errMsg = ServiceUtil.getErrorMessage(ripResult);
                        errorMapList.add(UtilMisc.toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg));
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error running service receiveInventoryProduct: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericServiceException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }    
            }
        }
        //prepare result Map for createOagisMessageinfo
        
        Timestamp timestamp = null;
        timestamp = UtilDateTime.nowTimestamp();
        comiCtx.put("logicalId", logicalId);
        comiCtx.put("authId", authId);
        comiCtx.put("referenceId", referenceId);
        comiCtx.put("receivedDate", timestamp);
        comiCtx.put("sentDate", sentTimestamp);
        comiCtx.put("component", component);
        comiCtx.put("task", task);  
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("userLogin", userLogin);
        if (OagisServices.debugSaveXmlIn) {
            try {
                comiCtx.put("fullMessageXml", UtilXml.writeXmlDocument(doc));
            } catch (IOException e) {
                // this is just for debug info, so just log and otherwise ignore error
                String errMsg = "Warning: error creating text from XML Document for saving to database: " + e.toString();
                Debug.logWarning(errMsg, module);
            }
        }
        try {
            dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            /* running async for better error handling
            if (ServiceUtil.isError(comiResult)) {
                String errMsg = ServiceUtil.getErrorMessage(comiResult);
                errorMapList.add(UtilMisc.toMap("reasonCode", "CreateOagisMessageServiceError", "description", errMsg));
            }
            */
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            // reconsider sending this error back to other server, not much they can do about it, and it may not be a critical error causing the message to be rejected...
            //errorMapList.add(UtilMisc.toMap("reasonCode", "CreateOagisMessageInfoError", "description", errMsg));
            Debug.logError(e, errMsg, module);
        }
        Map result = FastMap.newInstance();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);
        
        if (errorMapList.size() > 0) {
            result.put("errorMapList", errorMapList);
            String errMsg = "Error Processing Received Messages";
            result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        result.putAll(ServiceUtil.returnSuccess("Action Performed Successfully"));
        return result;
    }
    
    public static Map receiveRmaAcknowledge(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        List errorMapList = FastList.newInstance();
        
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                String errMsg = "Error Getting UserLogin: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
        }
        // parse the message
        Element receiveRmaElement = doc.getDocumentElement();
        receiveRmaElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(receiveRmaElement, "os:CNTROLAREA");
        Element docSenderElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:SENDER");
        Element docBsrElement = UtilXml.firstChildElement(docCtrlAreaElement, "os:BSR");
            
        String bsrVerb = UtilXml.childElementValue(docBsrElement, "of:VERB");
        String bsrNoun = UtilXml.childElementValue(docBsrElement, "of:NOUN");
        String bsrRevision = UtilXml.childElementValue(docBsrElement, "of:REVISION");
            
        String logicalId = UtilXml.childElementValue(docSenderElement, "of:LOGICALID");
        String component = UtilXml.childElementValue(docSenderElement, "of:COMPONENT");
        String task = UtilXml.childElementValue(docSenderElement, "of:TASK");
        String referenceId = UtilXml.childElementValue(docSenderElement, "of:REFERENCEID");
        String confirmation = UtilXml.childElementValue(docSenderElement, "of:CONFIRMATION");
        String authId = UtilXml.childElementValue(docSenderElement, "of:AUTHID");
            
        String sentDate = UtilXml.childElementValue(docCtrlAreaElement, "os:DATETIMEISO");
        Timestamp sentTimestamp = OagisServices.parseIsoDateString(sentDate, errorMapList);

        Element dataAreaElement = UtilXml.firstChildElement(receiveRmaElement, "ns:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");
        
        String inventoryItemTypeId = null;
        String returnId = null;
        String facilityId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.PoReceiptFacilityId");
        String locationSeqId = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.ReturnReceiptLocationSeqId");
        
        Timestamp timestamp = UtilDateTime.nowTimestamp();
        Map comiCtx = FastMap.newInstance();
        comiCtx.put("logicalId", logicalId);
        comiCtx.put("authId", authId);
        comiCtx.put("referenceId", referenceId);
        comiCtx.put("receivedDate", timestamp);
        comiCtx.put("sentDate", sentTimestamp);
        comiCtx.put("component", component);
        comiCtx.put("task", task);  
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("processingStatusId", "OAGMP_RECEIVED");
        comiCtx.put("userLogin", userLogin);
//        if (OagisServices.debugSaveXmlIn) {
//            try {
//                comiCtx.put("fullMessageXml", UtilXml.writeXmlDocument(doc));
//            } catch (IOException e) {
//                // this is just for debug info, so just log and otherwise ignore error
//                String errMsg = "Warning: error creating text from XML Document for saving to database: " + e.toString();
//                Debug.logWarning(errMsg, module);
//            }
//        }
//        try {
//            dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
//            /* running async for better error handling
//            if (ServiceUtil.isError(comiResult)) { 
//                String errMsg = ServiceUtil.getErrorMessage(comiResult);
//                errorMapList.add(UtilMisc.toMap("reasonCode", "CreateOagisMessageServiceError", "description", errMsg)); 
//            }
//            */
//        } catch (GenericServiceException e) {
//            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
//            Debug.logError(e, errMsg, module);
//        }
        
        String statusId = null;
        // get RECEIPTLN elements from message
        List acknowledgeElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        if (UtilValidate.isNotEmpty(acknowledgeElementList)) {
            Iterator acknowledgeElementIter = acknowledgeElementList.iterator();
            while (acknowledgeElementIter.hasNext()) {
                Map ripCtx = FastMap.newInstance();
                Element receiptLnElement = (Element) acknowledgeElementIter.next();
                Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "os:QUANTITY");

                String itemQtyStr = UtilXml.childElementValue(qtyElement, "of:VALUE");
                double itemQty = Double.parseDouble(itemQtyStr);
                String sign = UtilXml.childElementValue(qtyElement, "of:SIGN");

                String productId = UtilXml.childElementValue(receiptLnElement, "of:ITEM");
                if ( productId == null ) {
                    String errMsg = "productId not available in Message" ;
                    errorMapList.add(UtilMisc.toMap("reasonCode", "ParseException", "description", errMsg));
                    Debug.logError(errMsg, module);
                }
                Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "os:DOCUMNTREF");
                returnId = UtilXml.childElementValue(documentRefElement, "of:DOCUMENTID");
                ripCtx.put("returnId", returnId);

                String returnHeaderTypeId = UtilXml.childElementValue(documentRefElement, "of:DOCTYPE");
                if(returnHeaderTypeId.equals("RMA")) {
                    returnHeaderTypeId = "CUSTOMER_RETURN";
                }
                String returnItemSeqId = UtilXml.childElementValue(documentRefElement, "of:LINENUM");
                String datetimeReceived = UtilXml.childElementValue(receiptLnElement, "os:DATETIMEISO");
                Timestamp timestampItemReceived = OagisServices.parseIsoDateString(datetimeReceived, errorMapList);
                ripCtx.put("datetimeReceived", timestampItemReceived);

                GenericValue returnHeader = null;
                try {
                    returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                } catch  (GenericEntityException e) {
                    String errMsg = "Error Getting ReturnHeader: " + e.toString();
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }

                if (UtilValidate.isNotEmpty(returnHeader)) {
                    statusId = returnHeader.get("statusId").toString();
                    if (statusId.equals("RETURN_ACCEPTED")) {
                        // getting inventory item status
                        String invItemStatus = UtilXml.childElementValue(receiptLnElement, "of:DISPOSITN");
                        if ( invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
                            ripCtx.put("statusId", "INV_AVAILABLE");
                        } else if ( invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable") ) {
                            ripCtx.put("statusId", "INV_ON_HOLD");
                        }
                        // geting the serial number(s)
                        String serialNumber = null;
                        List serialNumsList = FastList.newInstance();
                        List invDetailList = UtilXml.childElementList(receiptLnElement, "ns:INVDETAIL");
                        if (UtilValidate.isNotEmpty(invDetailList)) {
                            inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                            ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);
                            for (Iterator j = invDetailList.iterator(); j.hasNext();) {
                                Element invDetailElement = (Element) j.next();
                                serialNumber = UtilXml.childElementValue(invDetailElement, "of:SERIALNUM");
                                if (UtilValidate.isNotEmpty(serialNumber)) {
                                    serialNumsList.add(serialNumber);
                                }
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
                            } */
                        } else {
                            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
                            ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);
                        }
                        ripCtx.put("facilityId",facilityId);
                        ripCtx.put("locationSeqId", locationSeqId);
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
                                    String errMsg = "Error: the quantity [" + quantityAccepted + "] did not match the number of serial numbers passed [" + serialNumsList.size() + "].";
                                    errorMapList.add(UtilMisc.toMap("reasonCode", "QuantitySerialMismatch", "description", errMsg));
                                }

                                Iterator serialNumIter = serialNumsList.iterator();
                                while (serialNumIter.hasNext()) {
                                    String serialNum = (String) serialNumIter.next();
                                    try {
                                        GenericValue inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNum)));
                                        if (inventoryItem != null) {
                                            String inventoryItemProductId = inventoryItem.getString("productId");
                                            if(inventoryItemProductId.equals(productId)) {
                                                Map updateInvItmMap = FastMap.newInstance();
                                                updateInvItmMap.put( "inventoryItemId" , inventoryItem.get("inventoryItemId").toString());
                                                updateInvItmMap.put( "userLogin" , userLogin);
                                                updateInvItmMap.put( "productId",productId);
                                                updateInvItmMap.put( "statusId","INV_ON_HOLD");
                                                try {
                                                    Map test = dispatcher.runSync("updateInventoryItem", updateInvItmMap);
                                                } catch (GenericServiceException e) {
                                                    String errMsg = "Error running service updateInventoryItem: " + e.toString();
                                                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericServiceException", "description", errMsg));
                                                    Debug.logError(e, errMsg, module);
                                                }
                                            }
                                        }
                                    } catch (GenericEntityException e) {
                                        String errMsg = "Error Getting Entity InventoryItem";
                                        Debug.logError(e, errMsg, module);
                                    }

                                    // clone the context as it may be changed in the call
                                    Map localRipCtx = FastMap.newInstance();
                                    localRipCtx.putAll(ripCtx);
                                    localRipCtx.put("quantityAccepted", new Double(1.0));
                                    // always set this to 0, if needed we'll handle the rejected quantity separately
                                    localRipCtx.put("quantityRejected", new Double(0.0));

                                    localRipCtx.put("serialNumber", serialNum);
                                    localRipCtx.put("productId", productId);
                                    localRipCtx.put("returnItemSeqId", returnItemSeqId);
                                    runReceiveInventoryProduct( localRipCtx , errorMapList , dispatcher);
                                }
                            } else {
                                // no serial numbers, just receive the quantity
                                // clone the context as it may be changted in the call
                                Map localRipCtx = FastMap.newInstance();
                                localRipCtx.putAll(ripCtx);
                                localRipCtx.put("quantityAccepted", new Double(quantityAccepted));
                                // always set this to 0, if needed we'll handle the rejected quantity separately
                                localRipCtx.put("quantityRejected", new Double(0.0));
                                localRipCtx.put("productId", productId);
                                localRipCtx.put("returnItemSeqId", returnItemSeqId);
                                runReceiveInventoryProduct( localRipCtx , errorMapList , dispatcher);
                            }
                        } else {
                            // TODOLATER: need to run service receiveInventoryProduct and updateInventoryItem when quantityRejected > 0
                            // NOTE DEJ20070711 this shouldn't happen for current needs, so save for later
                        }
                    } else { 
                        String errMsg = "Return status is not RETURN_ACCEPTED";
                        Debug.logError(errMsg, module);
                        errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    }
                } else {
                    String errMsg = "ReturnId Not Valid: Id not present in Database";
                    Debug.logError(errMsg, module);
                    errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                }
            }
            statusId = "RETURN_COMPLETED";
            try {
                dispatcher.runSync("updateReturnHeader", UtilMisc.toMap("statusId", statusId, "returnId", returnId, "userLogin", userLogin));
            } catch (GenericServiceException e) {
                String errMsg = "Error Storing the value: " + e.toString();
                errorMapList.add(UtilMisc.toMap("reasonCode", "GenericEntityException", "description", errMsg));
                Debug.logError(e, errMsg, module);
            }
        }

        // prepare result Map for createOagisMessageinfo
        
        
        Map result = FastMap.newInstance();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);
        if (errorMapList.size() > 0) {
            result.put("errorMapList", errorMapList);
            String errMsg = "Error Processing Received Messages";
            result.putAll(ServiceUtil.returnError(errMsg));
            return result;
        }
        
        comiCtx.put("returnId", returnId);
        comiCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
        try {
            dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
        } catch (GenericServiceException e){
            String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        result.putAll(ServiceUtil.returnSuccess("Action Performed Successfully"));
        return result;
    }

    public static void runReceiveInventoryProduct(Map localRipCtx, List errorMapList, LocalDispatcher dispatcher) {
        try {
            Map ripResult = dispatcher.runSync("receiveInventoryProduct",
                    localRipCtx);
            if (ServiceUtil.isError(ripResult)) {
                String errMsg = ServiceUtil.getErrorMessage(ripResult);
                errorMapList.add(UtilMisc.toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg));
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error running service receiveInventoryProduct: "
                    + e.toString();
            errorMapList.add(UtilMisc.toMap("reasonCode", "GenericServiceException", "description", errMsg));
            Debug.logError(e, errMsg, module);
        }
    }
    
}
