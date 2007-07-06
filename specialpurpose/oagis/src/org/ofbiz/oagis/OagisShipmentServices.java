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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;

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


public class OagisShipmentServices {
    
    public static final String module = OagisShipmentServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static final String resource = "OagisUiLabels";

    public static final String certAlias = UtilProperties.getPropertyValue("oagis.properties", "auth.client.certificate.alias");
    public static final String basicAuthUsername = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.username");
    public static final String basicAuthPassword = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.password");
    
    public static Map showShipment(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        
        List errorList = new LinkedList();
        Document doc = null;
        try {
            doc = UtilXml.readXmlDocument(in, true, "ShowShipment");
        } catch (SAXException e) {
            String errMsg = "Error parsing the ShowShipmentResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing the ShowShipmentResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        } catch (IOException e) {
            String errMsg = "Error parsing the ShowShipmentResponse";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
            
        GenericValue userLogin =null; 
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));    
        } catch (GenericEntityException e){
            String errMsg = "Error Getting UserLogin with userLoginId 'admin'";
            Debug.logError(e, errMsg, module);
        }
                    
        Element showShipmentElement = doc.getDocumentElement();
        showShipmentElement.normalize();
          
        Element controlAreaElement = UtilXml.firstChildElement(showShipmentElement, "N1:CNTROLAREA");
        Element bsrElement = UtilXml.firstChildElement(controlAreaElement, "N1:BSR");
        String bsrVerb = UtilXml.childElementValue(bsrElement, "N2:VERB");
        String bsrNoun = UtilXml.childElementValue(bsrElement, "N2:NOUN");
        String bsrRevision = UtilXml.childElementValue(bsrElement, "N2:REVISION");
          
        Map oagisMsgInfoCtx = new HashMap();
        oagisMsgInfoCtx.put("bsrVerb", bsrVerb);
        oagisMsgInfoCtx.put("bsrNoun", bsrNoun);
        oagisMsgInfoCtx.put("bsrRevision", bsrRevision);
            
        Element senderElement = UtilXml.firstChildElement(controlAreaElement, "N1:SENDER");
        String logicalId = UtilXml.childElementValue(senderElement, "N2:LOGICALID");
        String component = UtilXml.childElementValue(senderElement, "N2:COMPONENT");
        String task = UtilXml.childElementValue(senderElement, "N2:TASK");
        String referenceId = UtilXml.childElementValue(senderElement, "N2:REFERENCEID");
        String confirmation = UtilXml.childElementValue(senderElement, "N2:CONFIRMATION");
        String authId = UtilXml.childElementValue(senderElement, "N2:AUTHID");
          
        oagisMsgInfoCtx.put("logicalId", logicalId);
        oagisMsgInfoCtx.put("component", component);
        oagisMsgInfoCtx.put("task", task);
        oagisMsgInfoCtx.put("referenceId", referenceId);
        oagisMsgInfoCtx.put("confirmation", confirmation);
        oagisMsgInfoCtx.put("authId", authId);
        oagisMsgInfoCtx.put("outgoingMessage", "N");
        oagisMsgInfoCtx.put("userLogin", userLogin);
        
        try {
            Map oagisMsgInfoResult = dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoCtx);
            if (ServiceUtil.isError(oagisMsgInfoResult)){
                String errMsg = "Error creating OagisMessageInfo for the Incoming Message";
                errorList.add(errMsg);
                Debug.logError(errMsg, module);
            }
        } catch (GenericServiceException e){
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message";
            errorList.add(errMsg);
            Debug.logError(e, errMsg, module);
        }
           
        Element dataAreaElement = UtilXml.firstChildElement(showShipmentElement, "n:DATAAREA");
        Element daShowShipmentElement = UtilXml.firstChildElement(dataAreaElement, "n:SHOW_SHIPMENT");
        Element shipmentElement = UtilXml.firstChildElement(daShowShipmentElement, "n:SHIPMENT");                                  
        String shipmentId = UtilXml.childElementValue(shipmentElement, "N2:DOCUMENTID");            
           
        Element shipUnitElement = UtilXml.firstChildElement(daShowShipmentElement, "n:SHIPUNIT");
        String trackingNum = UtilXml.childElementValue(shipUnitElement, "N2:TRACKINGID");            
            
        Element invItem = UtilXml.firstChildElement(shipUnitElement, "n:INVITEM");            
        String productId = UtilXml.childElementValue(invItem, "N2:ITEM");
            
        Element invDetail = UtilXml.firstChildElement(invItem, "n:INVDETAIL");
        String serialNumber = UtilXml.childElementValue(invDetail,"N1:SERIALNUM");
        try {                
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");                
            String originFacilityId = shipment.getString("originFacilityId");                              
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
            Map isitspastCtx = UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "orderItemSeqId", orderItemSeqId, "quantity", shipmentItem.get("quantity"), "quantityNotReserved", shipmentItem.get("quantity"));                
            isitspastCtx.put("productId", productId);
            isitspastCtx.put("reservedDatetime", orderItemShipGrpInvReservation.get("reservedDatetime"));
            isitspastCtx.put("requireInventory", requireInventory);
            isitspastCtx.put("reserveOrderEnumId", orderItemShipGrpInvReservation.get("reserveOrderEnumId"));
            isitspastCtx.put("sequenceId", orderItemShipGrpInvReservation.get("sequenceId"));
            isitspastCtx.put("originFacilityId", originFacilityId);
            isitspastCtx.put("userLogin", userLogin);
            isitspastCtx.put("serialNumber", serialNumber);
            isitspastCtx.put("trackingNum", trackingNum);
            isitspastCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));                
            isitspastCtx.put("shipmentId", shipmentId);      
            try {                    
                Map resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                if (ServiceUtil.isError(resultMap)){
                    String errMsg = "Error executing issueSerializedInvToShipmentPackageAndSetTracking Service";
                    errorList.add(errMsg);
                    Debug.logError(errMsg, module);
                }
            } catch(GenericServiceException e) {
                Debug.logInfo(e, module);
                errorList.add(e.getMessage());
            }
        } catch (GenericEntityException e) {
            Debug.logInfo(e, module);
            errorList.add(e.getMessage());
        }
        
        Map result = new HashMap();
        result.put("contentType","text/plain");
        if (errorList.size() > 0) {
            // error message generation
            result.putAll(oagisMsgInfoCtx);
            result.put(ModelService.RESPONSE_MESSAGE,ModelService.RESPOND_ERROR); 
            result.put(ModelService.ERROR_MESSAGE_LIST, errorList);
            result.put("reasonCode", "1000"); 
            result.put("description", "processing message failed");
            return result;
        }
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
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        GenericValue orderHeader = null;
        GenericValue orderItemShipGroup = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        GenericValue shipment =null;
        if (orderHeader != null) {
            String orderStatusId = orderHeader.getString("statusId");
            if (orderStatusId.equals("ORDER_APPROVED")) {
                try {
                    Map  cospResult= dispatcher.runSync("createOrderShipmentPlan", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
                    shipmentId = (String) cospResult.get("shipmentId");
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                try {
                    shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
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
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                Set correspondingPoIdSet = new TreeSet();
                try {
                    List orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", shipment.getString("primaryOrderId")));
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        String correspondingPoId = orderItem.getString("correspondingPoId");
                        if (correspondingPoId != null) {
	                        correspondingPoIdSet.add(correspondingPoId);
	                        bodyParameters.put("correspondingPoIdSet", correspondingPoIdSet);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                Set externalIdSet = new TreeSet();
                try {
                    GenericValue primaryOrderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", shipment.getString("primaryOrderId")));
                    externalIdSet.add(primaryOrderHeader.getString("externalId"));
                    bodyParameters.put("externalIdSet", externalIdSet);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                // if order was a return replacement order (associated with return)
                List returnItemResponses =  null;
                List returnItemRespExprs = UtilMisc.toList(new EntityExpr("replacementOrderId", EntityOperator.NOT_EQUAL, null));
                EntityCondition returnItemRespCond = new EntityConditionList(returnItemRespExprs, EntityOperator.AND);
                // list of fields to select (initial list)
                List fieldsToSelect = FastList.newInstance();
                fieldsToSelect.add("replacementOrderId");
                try {
                    returnItemResponses = delegator.findByCondition("ReturnItemResponse", returnItemRespCond, fieldsToSelect, null);
                    Iterator rirIter = returnItemResponses.iterator();
                    while (rirIter.hasNext()) {
                        if (rirIter.next().equals(shipment.getString("primaryOrderId"))) {
                            bodyParameters.put("shipnotes", "RETURNLABEL");
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                bodyParameters.put("logicalId", logicalId);
                result.put("logicalId", logicalId);
                
                String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                bodyParameters.put("authId", authId);
                result.put("authId", authId);

                String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                bodyParameters.put("referenceId", referenceId);
                result.put("referenceId", referenceId);
                    
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
                Timestamp timestamp = UtilDateTime.nowTimestamp();
                String sentDate = dateFormat.format(timestamp);
                bodyParameters.put("sentDate", sentDate);
                result.put("sentDate", timestamp);
               
                // tracking shipper account
                String partyId = shipment.getString("partyIdTo");
                List partyCarrierAccounts = new ArrayList();
                try {
                    partyCarrierAccounts = delegator.findByAnd("PartyCarrierAccount", UtilMisc.toMap("partyId", partyId));
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
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
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
                
                // TODO: call service with require-new-transaction=true to save the OagisMessageInfo data (to make sure it saves before)

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
                    	String resp = http.post(writer.toString());
                    } catch (Exception e) {
                    	String errMsg = "Error posting message to server with UTL [" + sendToUrl + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }

                // prepare map to Create Oagis Message Info
                result.put("component", "INVENTORY");
                result.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUEST" which is more than 10 char
                result.put("outgoingMessage", "Y");
                result.put("confirmation", "1");
                result.put("bsrVerb", "PROCESS");
                result.put("bsrNoun", "SHIPMENT");
                result.put("bsrRevision", "001");
                result.put("processingStatusId", orderStatusId);
                result.put("orderId", orderId);
                result.put("shipmentId", shipmentId);
                result.put("userLogin", userLogin);
            }
        }
        return result;
    }
    
    public static Map oagisReceiveDelivery(DispatchContext dctx, Map context) {
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
                userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting userLogin", module);
            }
        }
        if (returnId != null) {
            GenericValue returnHeader = null;
            String statusId =null;
            try {
                returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                statusId = returnHeader.getString("statusId");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (statusId.equals("RETURN_ACCEPTED")) {
                try {
                    List returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
                    bodyParameters.put("returnItems", returnItems);
                    
                    String orderId = EntityUtil.getFirst(returnItems).getString("orderId");
                    result.put("orderId", orderId);
                    
                    GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", returnHeader.getString("originContactMechId")));
                    bodyParameters.put("postalAddress", postalAddress);
       
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
                    List partyContactMechs = delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId));
                    Iterator pcmIter = partyContactMechs.iterator();
                    while (pcmIter.hasNext()) {
                        GenericValue partyContactMech = (GenericValue) pcmIter.next();
                        String contactMechId = partyContactMech.getString("contactMechId");
                        GenericValue contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
                        String contactMechTypeId = contactMech.getString("contactMechTypeId");
                        if(contactMechTypeId.equals("EMAIL_ADDRESS")) {
                           String emailString = contactMech.getString("infoString");
                           bodyParameters.put("emailString", emailString);
                        }
                        if(contactMechTypeId.equals("TELECOM_NUMBER")) {
                           GenericValue telecomNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));
                           bodyParameters.put("telecomNumber", telecomNumber);
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                bodyParameters.put("logicalId", logicalId);
                result.put("logicalId", logicalId);
                
                String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                bodyParameters.put("authId", authId);
                result.put("authId", authId);
                
                String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                bodyParameters.put("referenceId", referenceId);
                result.put("referenceId", referenceId);
                
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
                Timestamp timestamp = UtilDateTime.nowTimestamp();
                String sentDate = dateFormat.format(timestamp);
                bodyParameters.put("sentDate", sentDate);
                result.put("sentDate", timestamp);
                
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
                        String resp = http.post(writer.toString());
                    } catch (Exception e) {
                        String errMsg = "Error posting message to server with UTL [" + sendToUrl + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }
                // prepare map to store BOD information
                result.put("component", "INVENTORY");
                result.put("task", "RMA");  
                result.put("outgoingMessage", "Y");
                result.put("confirmation", "1");
                result.put("bsrVerb", "RECEIVE");
                result.put("bsrNoun", "DELIVERY");
                result.put("bsrRevision", "001");
                result.put("processingStatusId", statusId);        
                result.put("returnId", returnId);
                result.put("userLogin", userLogin);
            }
        }    
        return result;
    }
}
