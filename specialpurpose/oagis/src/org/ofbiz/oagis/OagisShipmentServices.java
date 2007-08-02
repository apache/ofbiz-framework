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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

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
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.ofbiz.party.party.PartyWorker;


public class OagisShipmentServices {
    
    public static final String module = OagisShipmentServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static final String resource = "OagisUiLabels";

    public static final String certAlias = UtilProperties.getPropertyValue("oagis.properties", "auth.client.certificate.alias");
    public static final String basicAuthUsername = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.username");
    public static final String basicAuthPassword = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.password");

    public static final String oagisMainNamespacePrefix = "n";
    public static final String oagisSegmentsNamespacePrefix = "os";
    public static final String oagisFieldsNamespacePrefix = "of";
        
    public static Map showShipment(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        
        List errorMapList = FastList.newInstance();
            
        GenericValue userLogin =null; 
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));    
        } catch (GenericEntityException e){
            String errMsg = "Error Getting UserLogin with userLoginId system: "+e.toString();
            Debug.logError(e, errMsg, module);
        }
                    
        Element showShipmentElement = doc.getDocumentElement();
        showShipmentElement.normalize();
          
        Element controlAreaElement = UtilXml.firstChildElement(showShipmentElement, "os:CNTROLAREA"); // os
        Element bsrElement = UtilXml.firstChildElement(controlAreaElement, "os:BSR"); // os
        String bsrVerb = UtilXml.childElementValue(bsrElement, "of:VERB"); // of
        String bsrNoun = UtilXml.childElementValue(bsrElement, "of:NOUN"); // of
        String bsrRevision = UtilXml.childElementValue(bsrElement, "of:REVISION"); // of
        Map oagisMsgInfoCtx = UtilMisc.toMap("bsrVerb", bsrVerb, "bsrNoun", bsrNoun, "bsrRevision", bsrRevision);
            
        Element senderElement = UtilXml.firstChildElement(controlAreaElement, "os:SENDER"); // os
        String logicalId = UtilXml.childElementValue(senderElement, "of:LOGICALID"); // of
        String component = UtilXml.childElementValue(senderElement, "of:COMPONENT"); // of
        String task = UtilXml.childElementValue(senderElement, "of:TASK"); // of
        String referenceId = UtilXml.childElementValue(senderElement, "of:REFERENCEID"); // of
        String confirmation = UtilXml.childElementValue(senderElement, "of:CONFIRMATION"); // of
        String authId = UtilXml.childElementValue(senderElement, "of:AUTHID"); // of

        Map result = new HashMap();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        oagisMsgInfoCtx.put("logicalId", logicalId);
        oagisMsgInfoCtx.put("component", component);
        oagisMsgInfoCtx.put("task", task);
        oagisMsgInfoCtx.put("referenceId", referenceId);
        oagisMsgInfoCtx.put("confirmation", confirmation);
        oagisMsgInfoCtx.put("authId", authId);
        oagisMsgInfoCtx.put("outgoingMessage", "N");
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
            dispatcher.runAsync("createOagisMessageInfo", oagisMsgInfoCtx, true);
            /* running async for better error handling
            if (ServiceUtil.isError(oagisMsgInfoResult)){
                String errMsg = ServiceUtil.getErrorMessage(oagisMsgInfoResult);
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */
        } catch (GenericServiceException e){
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: "+e.toString();
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            Debug.logError(e, errMsg, module);
        }
           
        Element dataAreaElement = UtilXml.firstChildElement(showShipmentElement, "ns:DATAAREA"); // n
        Element daShowShipmentElement = UtilXml.firstChildElement(dataAreaElement, "ns:SHOW_SHIPMENT"); // n
        Element shipmentElement = UtilXml.firstChildElement(daShowShipmentElement, "ns:SHIPMENT"); // n                               
        String shipmentId = UtilXml.childElementValue(shipmentElement, "of:DOCUMENTID"); // of           
        GenericValue shipment = null;
        try {
            shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
        } catch (GenericEntityException e) {
            String errMsg = "Error Shipment from database: "+ e.toString();
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
            Debug.logInfo(e, module);
            result.putAll(ServiceUtil.returnError(errMsg));
            result.put("errorMapList", errorMapList);
            return result;
        }                    
        String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");                
        String originFacilityId = shipment.getString("originFacilityId");                              
          
        List shipUnitElementList = UtilXml.childElementList(daShowShipmentElement, "ns:SHIPUNIT"); // n
        if(UtilValidate.isNotEmpty(shipUnitElementList)) {
            Iterator shipUnitElementItr = shipUnitElementList.iterator();
            while(shipUnitElementItr.hasNext()) {                 
                Element shipUnitElement = (Element) shipUnitElementItr.next();
                String trackingNum = UtilXml.childElementValue(shipUnitElement, "of:TRACKINGID"); // of
                String shipmentPackageSeqId = UtilXml.childElementValue(shipUnitElement, "of:SHPUNITSEQ"); // of
                List invItemElementList = UtilXml.childElementList(shipUnitElement, "ns:INVITEM"); //n
                if(UtilValidate.isNotEmpty(invItemElementList)) {
                    Iterator invItemElementItr = invItemElementList.iterator();
                    while(invItemElementItr.hasNext()) {                 
                        Element invItemElement = (Element) invItemElementItr.next();
                        String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of                
                        try {                                    
                            GenericValue shipmentItem = EntityUtil.getFirst(delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId, "productId",productId)));                    
                            String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");                      
                            GenericValue orderShipment = EntityUtil.getFirst(delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId)));                    
                            String orderId = orderShipment.getString("orderId");                
                            String orderItemSeqId = orderShipment.getString("orderItemSeqId");                
                            GenericValue product = delegator.findByPrimaryKey("Product",UtilMisc.toMap("productId",productId));                    
                            String requireInventory = product.getString("requireInventory");                    
                            if(requireInventory == null) {
                                requireInventory = "N";
                            }                    
                            GenericValue orderItemShipGrpInvReservation = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId,"shipGroupSeqId",shipGroupSeqId)));               
                            Map isitspastCtx = FastMap.newInstance();
                            isitspastCtx = UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "orderItemSeqId", orderItemSeqId, "quantity", orderItemShipGrpInvReservation.get("quantity"), "quantityNotReserved", orderItemShipGrpInvReservation.get("quantity"));                
                            isitspastCtx.put("productId", productId);
                            isitspastCtx.put("reservedDatetime", orderItemShipGrpInvReservation.get("reservedDatetime"));
                            isitspastCtx.put("requireInventory", requireInventory);
                            isitspastCtx.put("reserveOrderEnumId", orderItemShipGrpInvReservation.get("reserveOrderEnumId"));
                            isitspastCtx.put("sequenceId", orderItemShipGrpInvReservation.get("sequenceId"));
                            isitspastCtx.put("originFacilityId", originFacilityId);
                            isitspastCtx.put("userLogin", userLogin);            
                            isitspastCtx.put("trackingNum", trackingNum);
                            isitspastCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));                
                            isitspastCtx.put("shipmentId", shipmentId);      
                            isitspastCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
                            isitspastCtx.put("promisedDatetime", orderItemShipGrpInvReservation.get("promisedDatetime"));                    
                            List invDetailElementList = UtilXml.childElementList(invItemElement, "ns:INVDETAIL"); //n                            
                            if(UtilValidate.isNotEmpty(invDetailElementList)) {
                                Iterator invDetailElementItr = invDetailElementList.iterator();
                                while(invDetailElementItr.hasNext()) {
                                    Element invDetailElement = (Element) invDetailElementItr.next();
                                    String serialNumber = UtilXml.childElementValue(invDetailElement, "os:SERIALNUM"); // os                                                                                   
                                    isitspastCtx.put("serialNumber", serialNumber);                                        
                                    isitspastCtx.remove("itemIssuanceId");                            
                                    try {
                                        Map resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                        if (ServiceUtil.isError(resultMap)){
                                            String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
                                            Debug.logError(errMsg, module);
                                        }
                                    } catch(GenericServiceException e) {
                                        Debug.logInfo(e, module);
                                        String errMsg = "Error executing issueSerializedInvToShipmentPackageAndSetTracking Service: "+e.toString();
                                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                                    }
                                }
                            } else {
                                try {//TODO: I think this else part is for NON Serialized Inv item. So it will be different service that we need to call here.                    
                                    Map resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                    if (ServiceUtil.isError(resultMap)){
                                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
                                        Debug.logError(errMsg, module);
                                    }
                                } catch(GenericServiceException e) {
                                    Debug.logInfo(e, module);
                                    String errMsg = "Error executing issueSerializedInvToShipmentPackageAndSetTracking Service: "+e.toString();
                                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                                }            
                            }
                        } catch (GenericEntityException e) {
                            String errMsg = "Error executing issueSerializedInvToShipmentPackageAndSetTracking Service: "+e.toString();
                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
                            Debug.logInfo(e, module);
                        }
                    }
                }
            }
            try {
                Map resultMap = dispatcher.runSync("setShipmentStatusPackedAndShipped", UtilMisc.toMap("shipmentId", shipmentId, "userLogin", userLogin));               
                if (ServiceUtil.isError(resultMap)){
                    String errMsg = ServiceUtil.getErrorMessage(resultMap);
                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SetShipmentStatusPackedAndShippedError"));
                    Debug.logError(errMsg, module);
                }
            } catch(GenericServiceException e) {
                Debug.logInfo(e, module);
                String errMsg = "Error executing setShipmentStatusPackedAndShipped Service: "+e.toString();
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            }   
        }  
        
        
        if (errorMapList.size() > 0) {
            //result.putAll(ServiceUtil.returnError("Errors found processing message"));
            result.put("errorMapList", errorMapList);
            return result;
        }
        
        result.putAll(ServiceUtil.returnSuccess("Service Completed Successfully"));
        return result;
    }

    public static Map oagisProcessShipment(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipmentId = (String) context.get("shipmentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        
        String sendToUrl = (String) context.get("sendToUrl");
        String saveToFilename = (String) context.get("saveToFilename");
        String saveToDirectory = (String) context.get("saveToDirectory");
        OutputStream out = (OutputStream) context.get("outputStream");
        
        Map result = ServiceUtil.returnSuccess();
        MapStack bodyParameters =  MapStack.create();
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        
        // check payment authorization
        Map serviceContext = FastMap.newInstance();
        serviceContext.put("orderId", orderId);
        serviceContext.put("userLogin", userLogin);
        serviceContext.put("reAuth", new Boolean("true"));
        Map authResult = null;
        try {
            authResult = dispatcher.runSync("authOrderPayments", serviceContext);
            if (!authResult.get("processResult").equals("APPROVED")) {
                return ServiceUtil.returnError("No valid payment available, cannot process Shipment");            
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error authorizing payment: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        GenericValue orderHeader = null;
        GenericValue orderItemShipGroup = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (orderHeader != null) {
            String orderStatusId = orderHeader.getString("statusId");
            if (orderStatusId.equals("ORDER_APPROVED")) {
                try {
                    
                    // check to see if there is already a Shipment for this order
                    EntityCondition findShipmentCondition = new EntityConditionList(UtilMisc.toList(
                            new EntityExpr("primaryOrderId", EntityOperator.EQUALS, orderId),
                            new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_CANCELLED")
                            ), EntityOperator.AND);
                    List shipmentList = delegator.findByCondition("Shipment", findShipmentCondition, null, null);
                    GenericValue shipment = EntityUtil.getFirst(shipmentList);
                    
                    if (shipment != null) {
                        // if picked, packed, shipped, delivered then complain, no reason to process the shipment!
                        String statusId = shipment.getString("statusId");
                        if ("SHIPMENT_PICKED".equals(statusId) || "SHIPMENT_PACKED".equals(statusId) || "SHIPMENT_SHIPPED".equals(statusId) || "SHIPMENT_DELIVERED".equals(statusId)) {
                            return ServiceUtil.returnError("Not sending Process Shipment message because found Shipment that is already being processed, is in status [" + statusId + "]");
                        }
                        shipmentId = shipment.getString("shipmentId");
                    } else {
                        Map cospResult= dispatcher.runSync("createOrderShipmentPlan", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
                        shipmentId = (String) cospResult.get("shipmentId");
                        shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
                    }
                    
                    bodyParameters.put("shipment", shipment);
                    OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
                    if(orderReadHelper.hasShippingAddress()) {
                        GenericValue  address = EntityUtil.getFirst(orderReadHelper.getShippingLocations());
                        bodyParameters.put("address", address);
                    }
                    String emailString = orderReadHelper.getOrderEmailString();
                    bodyParameters.put("emailString", emailString);
                    String contactMechId = shipment.getString("destinationTelecomNumberId");
                    GenericValue telecomNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));
                    bodyParameters.put("telecomNumber", telecomNumber);
                    List shipmentItems = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId));
                    bodyParameters.put("shipmentItems", shipmentItems);
                    orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId)));
                    bodyParameters.put("orderItemShipGroup", orderItemShipGroup);
                    Set correspondingPoIdSet = FastSet.newInstance();
                    List orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        String correspondingPoId = orderItem.getString("correspondingPoId");
                        if (correspondingPoId != null) {
                            correspondingPoIdSet.add(correspondingPoId);
                        }
                    }
                    bodyParameters.put("correspondingPoIdSet", correspondingPoIdSet);
                    if (orderHeader.get("externalId") != null) {
                        Set externalIdSet = FastSet.newInstance();
                        externalIdSet.add(orderHeader.getString("externalId"));
                        bodyParameters.put("externalIdSet", externalIdSet);
                    }
                    // Check if order was a return replacement order (associated with return)
                    GenericValue returnItemResponse = EntityUtil.getFirst(delegator.findByAnd("ReturnItemResponse", UtilMisc.toMap("replacementOrderId", orderId)));
                    if (returnItemResponse != null) {
                        bodyParameters.put("shipnotes", "RETURNLABEL");
                        
                        // Get the associated return Id (replaceReturnId)
                        String returnItemResponseId = returnItemResponse.getString("returnItemResponseId");
                        GenericValue returnItem = EntityUtil.getFirst(delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnItemResponseId", returnItemResponseId)));
                        bodyParameters.put("replacementReturnId", returnItem.getString("returnId"));
                    }
                    // tracking shipper account, other Party info
                    String partyId = shipment.getString("partyIdTo");
                    bodyParameters.put("partyNameView", delegator.findByPrimaryKey("PartyNameView", UtilMisc.toMap("partyId", partyId)));
                    List partyCarrierAccounts = delegator.findByAnd("PartyCarrierAccount", UtilMisc.toMap("partyId", partyId));
                    partyCarrierAccounts = EntityUtil.filterByDate(partyCarrierAccounts);
                    if (partyCarrierAccounts != null) {
                        Iterator pcaIter = partyCarrierAccounts.iterator();
                        while (pcaIter.hasNext()) {
                            GenericValue partyCarrierAccount = (GenericValue) pcaIter.next();
                            String carrierPartyId = partyCarrierAccount.getString("carrierPartyId");
                            if (carrierPartyId.equals(orderItemShipGroup.getString("carrierPartyId"))) {
                                String accountNumber = partyCarrierAccount.getString("accountNumber");
                                bodyParameters.put("shipperId", accountNumber);
                            }
                        }
                    }
                } catch (GenericServiceException e) {
                    String errMsg = "Error preparing data for OAGIS Process Shipment message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                } catch (GenericEntityException e) {
                    String errMsg = "Error preparing data for OAGIS Process Shipment message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                
                String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                bodyParameters.put("logicalId", logicalId);
                Map comiCtx = UtilMisc.toMap("logicalId", logicalId);
                
                String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                bodyParameters.put("authId", authId);
                comiCtx.put("authId", authId);
    
                String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                bodyParameters.put("referenceId", referenceId);
                comiCtx.put("referenceId", referenceId);
                    
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
                Timestamp timestamp = UtilDateTime.nowTimestamp();
                String sentDate = dateFormat.format(timestamp);
                bodyParameters.put("sentDate", sentDate);
                comiCtx.put("sentDate", timestamp);
               
                bodyParameters.put("shipmentId", shipmentId);
                bodyParameters.put("orderId", orderId);
                bodyParameters.put("userLogin", userLogin);
                String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ProcessShipment");

                Writer writer = null;
                if (out != null) {
                    writer = new OutputStreamWriter(out);
                } else if (UtilValidate.isNotEmpty(saveToFilename)) {
                    try {
                        File outdir = new File(saveToDirectory);
                        if (!outdir.exists()) {
                            outdir.mkdir();
                        }
                        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, saveToFilename)), "UTF-8")));
                    } catch (Exception e) {
                        String errMsg = "Error opening file to save message to [" + saveToFilename + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                } else if (UtilValidate.isNotEmpty(sendToUrl)) {
                    writer = new StringWriter();
                }

                ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, new HtmlScreenRenderer());
                try {
                    screens.render(bodyScreenUri);
                    writer.close();
                } catch (Exception e) {
                    String errMsg = "Error rendering message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                
                // TODO: should we make sure this is saved in error conditions
                // prepare map to Create Oagis Message Info
                comiCtx.put("component", "INVENTORY");
                comiCtx.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUEST" which is more than 10 char
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "PROCESS");
                comiCtx.put("bsrNoun", "SHIPMENT");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("processingStatusId", orderStatusId);
                comiCtx.put("orderId", orderId);
                comiCtx.put("shipmentId", shipmentId);
                comiCtx.put("userLogin", userLogin);
                if (OagisServices.debugSaveXmlOut) {
                    comiCtx.put("fullMessageXml", writer.toString());
                }
                
                try {
                    dispatcher.runAsync("createOagisMessageInfo", comiCtx, true);
                } catch (GenericServiceException e){
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
    
                
                if (UtilValidate.isNotEmpty(sendToUrl)) {
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
                        http.post(writer.toString());
                    } catch (Exception e) {
                        String errMsg = "Error posting message to server with UTL [" + sendToUrl + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }
            }
        }
        return result;
    }
    
    public static Map oagisReceiveDelivery(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        
        String sendToUrl = (String) context.get("sendToUrl");
        String saveToFilename = (String) context.get("saveToFilename");
        String saveToDirectory = (String) context.get("saveToDirectory");
        OutputStream out = (OutputStream) context.get("outputStream");
        
        Map result = ServiceUtil.returnSuccess();
        MapStack bodyParameters =  MapStack.create();
        if (userLogin == null) {
            try {
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting system userLogin", module);
            }
        }
        GenericValue returnHeader = null;
        String statusId = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            statusId = returnHeader.getString("statusId");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (returnHeader != null) {
            if (statusId.equals("RETURN_ACCEPTED")) {
                List returnItems = null;
                try {
                    returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
                    bodyParameters.put("returnItems", returnItems);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                GenericValue orderHeader = null;
                String orderId = null;
                try {
                    orderId = EntityUtil.getFirst(returnItems).getString("orderId");
                    orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                    if (orderHeader == null) {
                        return ServiceUtil.returnError("No valid Order with [" + orderId + "] found, cannot process Return");
                    }
                } catch (GenericEntityException e) {
                    String errMsg = "Cannot process Return: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                Map comiCtx = UtilMisc.toMap("orderId", orderId);
                try {    
                    GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", returnHeader.getString("originContactMechId")));
                    bodyParameters.put("postalAddress", postalAddress);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
       
                // calculate total qty of return items in a shipping unit received, order associated with return
                double itemQty = 0.0;
                double totalQty = 0.0;
                Iterator riIter = returnItems.iterator();
                while (riIter.hasNext()) {
                    GenericValue returnItem = (GenericValue) riIter.next();
                    itemQty = returnItem.getDouble("returnQuantity").doubleValue();
                    totalQty = totalQty + itemQty;
                }
                bodyParameters.put("totalQty", new Double(totalQty));
                
                String partyId = returnHeader.getString("fromPartyId");
                String emailString = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator).getString("infoString");
                bodyParameters.put("emailString", emailString);
    
                GenericValue telecomNumber = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
                bodyParameters.put("telecomNumber", telecomNumber);
                
                String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                bodyParameters.put("logicalId", logicalId);
                comiCtx.put("logicalId", logicalId);
                
                String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                bodyParameters.put("authId", authId);
                comiCtx.put("authId", authId);
                
                String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                bodyParameters.put("referenceId", referenceId);
                comiCtx.put("referenceId", referenceId);
                
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
                Timestamp timestamp = UtilDateTime.nowTimestamp();
                String sentDate = dateFormat.format(timestamp);
                bodyParameters.put("sentDate", sentDate);
                comiCtx.put("sentDate", timestamp);
                
                String entryDate = dateFormat.format(returnHeader.getTimestamp("entryDate"));
                bodyParameters.put("entryDate", entryDate);
                
                bodyParameters.put("returnId", returnId);
                String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ReceiveDelivery");
                
                Writer writer = null;
                if (out != null) {
                    writer = new OutputStreamWriter(out);
                } else if (UtilValidate.isNotEmpty(saveToFilename)) {
                    try {
                        File outdir = new File(saveToDirectory);
                        if (!outdir.exists()) {
                            outdir.mkdir();
                        }
                        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, saveToFilename)), "UTF-8")));
                    } catch (Exception e) {
                        String errMsg = "Error opening file to save message to [" + saveToFilename + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                } else if (UtilValidate.isNotEmpty(sendToUrl)) {
                    writer = new StringWriter();
                }

                ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, new HtmlScreenRenderer());
                try {
                    screens.render(bodyScreenUri);
                    writer.close();
                } catch (Exception e) {
                    String errMsg = "Error rendering message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                
                // TODO: call service with require-new-transaction=true to save the OagisMessageInfo data (to make sure it saves before)
                // prepare map to Create Oagis Message Info
                comiCtx.put("component", "INVENTORY");
                comiCtx.put("task", "RMA"); 
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "RECEIVE");
                comiCtx.put("bsrNoun", "DELIVERY");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("processingStatusId", statusId);
                comiCtx.put("returnId", returnId);
                comiCtx.put("userLogin", userLogin);
                if (OagisServices.debugSaveXmlOut) {
                    comiCtx.put("fullMessageXml", writer.toString());
                }

                try {
                    dispatcher.runAsync("createOagisMessageInfo", comiCtx, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }

                if (UtilValidate.isNotEmpty(sendToUrl)) {
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
                        http.post(writer.toString());
                    } catch (Exception e) {
                        String errMsg = "Error posting message to server with UTL [" + sendToUrl + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }
            }
        }    
        return result;
    }
}
