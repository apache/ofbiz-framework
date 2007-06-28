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
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import javolution.util.FastMap;
import javolution.util.FastList;

import org.w3c.dom.Document;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

import org.w3c.dom.Element;

import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;


public class OagisShipmentServices {
    
    public static final String module = OagisShipmentServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static final String resource = "OagisUiLabels";
    
    public static Map showShipment(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = null;
        try{
            Document doc = UtilXml.readXmlDocument(in, true, "ShowShipment");
            userLogin = delegator.findByPrimaryKey("UserLogin",UtilMisc.toMap("userLoginId","admin"));            
            Element shipmentElement = doc.getDocumentElement();
            shipmentElement.normalize();
            Map BODMap = new FastMap();
            
            Element controllAreaElement = UtilXml.firstChildElement(shipmentElement, "N1:CNTROLAREA");            
            Element BSRElement = UtilXml.firstChildElement(controllAreaElement, "N1:BSR");                       
            String bsrVerb = UtilXml.childElementValue(BSRElement, "N2:VERB");
            String bsrNoun = UtilXml.childElementValue(BSRElement, "N2:NOUN");
            String bsrRevision = UtilXml.childElementValue(BSRElement, "/N2:REVISION");
            
            BODMap.put("bsrVerb",bsrVerb);
            BODMap.put("bsrNoun",bsrNoun);
            BODMap.put("bsrRevision",bsrRevision);
            
            Element senderElement = UtilXml.firstChildElement(controllAreaElement, "N1:SENDER");
            String logicalId = UtilXml.childElementValue(senderElement, "N2:LOGICALID");
            String component = UtilXml.childElementValue(senderElement, "N2:COMPONENT");
            String task = UtilXml.childElementValue(senderElement, "N2:TASK");
            String reference = UtilXml.childElementValue(senderElement, "N2:REFERENCEID");
            String confirmation = UtilXml.childElementValue(senderElement, "N2:CONFIRMATION");
            String language = UtilXml.childElementValue(senderElement, "N2:LANGUAGE");
            String codePage = UtilXml.childElementValue(senderElement, "N2:CODEPAGE");
            String authId = UtilXml.childElementValue(senderElement, "N2:AUTHID");
            
            BODMap.put("logicalId",logicalId);
            BODMap.put("component",component);
            BODMap.put("task",task);
            BODMap.put("referenceId", reference);
            BODMap.put("confirmation", confirmation);
            BODMap.put("authId", authId);
            
            String controllAreaDateTime = UtilXml.childElementValue(controllAreaElement, "N1:DATETIMEANY");            
            
            Element dataAreaElement = UtilXml.firstChildElement(shipmentElement, "n:DATAAREA");
            Element showShipmentElement = UtilXml.firstChildElement(dataAreaElement, "n:SHOW_SHIPMENT");
            Element shipment_N_Element = UtilXml.firstChildElement(showShipmentElement, "n:SHIPMENT");
            String dataAreaDateTime = UtilXml.childElementValue(shipment_N_Element, "N1:DATETIMEANY");
            
            Element operAMT = UtilXml.firstChildElement(shipment_N_Element, "N1:OPERAMT");
            String operAMTValue = UtilXml.childElementValue(operAMT, "N2:VALUE");
            String operAMTNum =UtilXml.childElementValue(operAMT,"N2:NUMOFDEC");
            String operAMTSign =UtilXml.childElementValue(operAMT,"N2:SIGN");
            String operAMTCurrency =UtilXml.childElementValue(operAMT,"N2:CURRENCY");
                        
            String documentId =UtilXml.childElementValue(shipment_N_Element,"N2:DOCUMENTID");
            
            String description =UtilXml.childElementValue(shipment_N_Element,"N2:DESCRIPTN");
            
            Element partner = UtilXml.firstChildElement(shipment_N_Element, "N1:PARTNER");
            String partnerName =UtilXml.childElementValue(partner,"N2:NAME");
            String partnerType =UtilXml.childElementValue(partner,"N2:PARTNRTYPE");
            String partnerCurrency =UtilXml.childElementValue(partner,"N2:CURRENCY");
            
            Element partnerAddress = UtilXml.firstChildElement(partner, "N1:ADDRESS");
            String partnerAddLine =UtilXml.childElementValue(partnerAddress,"N2:ADDRLINE");
            String partnerCity =UtilXml.childElementValue(partnerAddress,"N2:CITY");
            String partnerCountry =UtilXml.childElementValue(partnerAddress,"N2:COUNTRY");
            String partnerFax =UtilXml.childElementValue(partnerAddress,"N2:FAX");
            String partnerPostalCode =UtilXml.childElementValue(partnerAddress,"N2:POSTALCODE");
            String partnerState =UtilXml.childElementValue(partnerAddress,"N2:STATEPROVN");
            String partnerTelePhn =UtilXml.childElementValue(partnerAddress,"N2:TELEPHONE");
            
            Element partnerContact = UtilXml.firstChildElement(partner, "N1:CONTACT");
            String partnerContactName =UtilXml.childElementValue(partnerContact,"N2:NAME");
            String partnerContactEmail =UtilXml.childElementValue(partnerContact,"N2:FAX");
            String partnerContactTelePhn =UtilXml.childElementValue(partnerContact,"N2:TELEPHONE");
            
            Element shipUnitElement = UtilXml.firstChildElement(showShipmentElement, "n:SHIPUNIT");
            Element shipUnitQuantity = UtilXml.firstChildElement(shipUnitElement, "N1:QUANTITY");
            String shipUnitQuantityValue =UtilXml.childElementValue(shipUnitQuantity,"N2:VALUE");
            String shipUnitQuantityNum =UtilXml.childElementValue(shipUnitQuantity,"N2:NUMOFDEC");
            String shipUnitQuantitySign =UtilXml.childElementValue(shipUnitQuantity,"N2:SIGN");
            String shipUnitQuantityUOM =UtilXml.childElementValue(shipUnitQuantity,"N2:UOM");
            
            String shipUnitCarrier =UtilXml.childElementValue(shipUnitElement,"N2:CARRIER");
            String shipUnitTrackingId =UtilXml.childElementValue(shipUnitElement,"N2:TRACKINGID");
            String shipUnitSeqId =UtilXml.childElementValue(shipUnitElement,"N2:SHPUNITSEQ");
            String shipUnitTOT =UtilXml.childElementValue(shipUnitElement,"N2:SHPUNITTOT");
            
            Element invItem = UtilXml.firstChildElement(shipUnitElement, "n:INVITEM");
            Element invItemQuantity = UtilXml.firstChildElement(invItem, "N1:QUANTITY");
            String invItemQuantityValue =UtilXml.childElementValue(invItemQuantity,"N2:VALUE");
            String invItemQuantityNum =UtilXml.childElementValue(invItemQuantity,"N2:NUMOFDEC");
            String invItemQuantitySign =UtilXml.childElementValue(invItemQuantity,"N2:SIGN");
            String invItemQuantityUOM =UtilXml.childElementValue(invItemQuantity,"N2:UOM");
            String invItemItem =UtilXml.childElementValue(invItem,"N2:ITEM");
            
            Element invDetail = UtilXml.firstChildElement(invItem, "n:INVDETAIL");
            String invDetailSerialNum =UtilXml.childElementValue(invDetail,"N1:SERIALNUM");
            
            /*Code for Issuing the Items*/
            List orderItemShipGrpInvReservations = FastList.newInstance();            
            Map iSITSPASTCtx = FastMap.newInstance();
            Map result = null;
            //GenericValue inventoryItem = null;
            try {                
                GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", documentId));                
                String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");                
                String originFacilityId = shipment.getString("originFacilityId");                              
                List shipmentItems = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", documentId, "productId",invItemItem));                
                GenericValue shipmentItem =  EntityUtil.getFirst(shipmentItems);                
                String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");                
                //Now we have enough keys to lookup the right OrderShipment
                List orderShipments = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", documentId, "shipmentItemSeqId",shipmentItemSeqId));                
                GenericValue orderShipment =   EntityUtil.getFirst(orderShipments);                
                String orderId = orderShipment.getString("orderId");                
                String orderItemSeqId = orderShipment.getString("orderItemSeqId");                
                GenericValue product = delegator.findByPrimaryKey("Product",UtilMisc.toMap("productId",invItemItem));                
                String requireInventory = product.getString("requireInventory");
                if(requireInventory == null) {
                    requireInventory = "N";
                }                
                // Look for reservations in some status.
                orderItemShipGrpInvReservations = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId,"orderItemSeqId",orderItemSeqId,"shipGroupSeqId",shipGroupSeqId));               
                GenericValue orderItemShipGrpInvReservation =   EntityUtil.getFirst(orderItemShipGrpInvReservations);                
                GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId",orderItemShipGrpInvReservation.get("inventoryItemId")));                
                String serialNumber = inventoryItem.getString("serialNumber");                
                
                iSITSPASTCtx.put("orderId", orderId);
                iSITSPASTCtx.put("shipGroupSeqId", shipGroupSeqId);
                iSITSPASTCtx.put("orderItemSeqId", orderItemSeqId);                
                iSITSPASTCtx.put("quantity", shipmentItem.get("quantity"));
                iSITSPASTCtx.put("quantityNotReserved", shipmentItem.get("quantity"));
                iSITSPASTCtx.put("productId", invItemItem);
                iSITSPASTCtx.put("reservedDatetime", orderItemShipGrpInvReservation.get("reservedDatetime"));
                iSITSPASTCtx.put("requireInventory", requireInventory);
                iSITSPASTCtx.put("reserveOrderEnumId", orderItemShipGrpInvReservation.get("reserveOrderEnumId"));
                iSITSPASTCtx.put("sequenceId", orderItemShipGrpInvReservation.get("sequenceId"));
                iSITSPASTCtx.put("originFacilityId", originFacilityId);
                iSITSPASTCtx.put("userLogin", userLogin);
                iSITSPASTCtx.put("serialNumber", invDetailSerialNum);
                iSITSPASTCtx.put("trackingNum", shipUnitTrackingId);
                iSITSPASTCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));                
                iSITSPASTCtx.put("shipmentId", documentId);                                
                // Check if the inventory Item we reserved is same as Item shipped
                // If not then reserve Inventory Item                               
                try {                    
                    result = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", iSITSPASTCtx);                                      
                } catch(Exception e) {
                    Debug.logInfo("========In catch =========", module);
                    return ServiceUtil.returnError("return error"+e);
                }
            } catch (Exception e) {
                return ServiceUtil.returnError("return error"+e);
            }
        }catch (Exception e){
            Debug.logError(e, module);
        }
        return ServiceUtil.returnError("Service not Implemented");
        
    }
    
    public static void writeScreenToOutputStream(OutputStream out, String bodyScreenUri, MapStack parameters){

        Writer writer = new OutputStreamWriter(out);
        ScreenRenderer screens = new ScreenRenderer(writer, parameters, new HtmlScreenRenderer());
        try {
            screens.render(bodyScreenUri);
        } catch (Exception e) {
            Debug.logError(e, "Error rendering [text/xml]: ", module);
        }

    }
    
    
    public static Map exportMsgFromScreen(DispatchContext dctx, Map serviceContext) {

        String bodyScreenUri = (String) serviceContext.remove("bodyScreenUri");
        Map bodyParameters = (Map) serviceContext.remove("bodyParameters");

        MapStack screenContext = MapStack.create();
        Writer bodyWriter = new StringWriter();
        ScreenRenderer screens = new ScreenRenderer(bodyWriter, screenContext, htmlScreenRenderer);
        if (bodyParameters != null) {
            screens.populateContextForService(dctx, bodyParameters);
            screenContext.putAll(bodyParameters);
        }
        //screenContext.putAll(serviceContext);
        //screens.getContext().put("formStringRenderer", foFormRenderer);
        try {
            screens.render(bodyScreenUri);
        } catch (Exception e) {
            String errMsg = "Error rendering [text/xml]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        Map result = ServiceUtil.returnSuccess();
        Debug.logInfo(bodyWriter.toString(), module);
        result.put("body", bodyWriter.toString());
        return result;
    }

    public static Map processShipment(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        //String shipmentId = (String) context.get("shipmentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        GenericValue orderItemShipGroup = null;
        GenericValue productStore =null;
        GenericValue shipment =null;
        if (orderHeader != null) {
            String orderStatusId = orderHeader.getString("statusId");
            if (orderStatusId.equals("ORDER_APPROVED")) {
                try {
                    // There can be more then one ship Groups
                    orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId), UtilMisc.toList("shipGroupSeqId")));
                    String productStoreId = orderHeader.getString("productStoreId"); 
                    productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                    String originFacilityId = productStore.getString("inventoryFacilityId");
                    String statusId = "SHIPMENT_INPUT";
                    
                    Map  result= dispatcher.runSync("createShipment", UtilMisc.toMap("primaryOrderId", orderId,"primaryShipGroupSeqId",orderItemShipGroup.get("shipGroupSeqId") ,"statusId", statusId ,"originFacilityId", originFacilityId ,"userLogin", userLogin));
                    shipmentId = (String) result.get("shipmentId");
                    shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));

                    List orderItems = new ArrayList();
                    Map orderItemCtx = new HashMap();
                    orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        orderItemCtx.put("orderId", orderId);
                        orderItemCtx.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                        orderItemCtx.put("shipmentId", shipmentId);
                        orderItemCtx.put("quantity", orderItem.get("quantity"));
                        orderItemCtx.put("userLogin", userLogin);
                        dispatcher.runSync("addOrderShipmentToShipment", orderItemCtx);
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Set correspondingPoIdSet = new TreeSet();
                try {
                    List orderItems = orderHeader.getRelated("OrderItem");
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        String correspondingPoId = orderItem.getString("correspondingPoId");
                        correspondingPoIdSet.add(correspondingPoId);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                Set externalIdSet = new TreeSet();
                try {
                    GenericValue shipmentOrderHeader = shipment.getRelatedOne("PrimaryOrderHeader");
                    externalIdSet.add(shipmentOrderHeader.getString("externalId"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
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
                bodyParameters.put("correspondingPoIdSet", correspondingPoIdSet);
                bodyParameters.put("externalIdSet", externalIdSet);
                bodyParameters.put("userLogin", userLogin);
                String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ProcessShipment");
                OutputStream out = (OutputStream) context.get("outputStream");
                Writer writer = new OutputStreamWriter(out);
                ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, new HtmlScreenRenderer());
                try {
                    screens.render(bodyScreenUri);
                } catch (Exception e) {
                      Debug.logError(e, "Error rendering [text/xml]: ", module);
                }
                // prepare map to Create Oagis Message Info
                Map comiCtx = new HashMap();
                comiCtx.put("logicalId", logicalId);
                comiCtx.put("component", "INVENTORY");
                comiCtx.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUEST" which is more than 10 char
                comiCtx.put("referenceId", referenceId);
                comiCtx.put("authId", authId);
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("sentDate", timestamp);
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "PROCESS");
                comiCtx.put("bsrNoun", "SHIPMENT");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("processingStatusId", orderStatusId);
                comiCtx.put("orderId", orderId);
                comiCtx.put("shipmentId", shipmentId);
                comiCtx.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("createOagisMessageInfo", comiCtx);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }
        return ServiceUtil.returnSuccess("Service Completed Successfully");
    }
    
    public static Map receiveDelivery(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue returnHeader = null;
        GenericValue postalAddress =null;
        List returnItems = new ArrayList();
        String partyId = null;
        String orderId = null;
        if (returnId != null) {
            try {
                returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                String statusId = returnHeader.getString("statusId");
                if (statusId.equals("RETURN_ACCEPTED")) {
                    returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
                    postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", returnHeader.getString("originContactMechId")));
                            
                    // calculate total qty of return items in a shipping unit received
                    double itemQty = 0.0;
                    double totalQty = 0.0;
                    Iterator riIter = returnItems.iterator();
                    while (riIter.hasNext()) {
                        GenericValue returnItem = (GenericValue) riIter.next();
                        itemQty = returnItem.getDouble("returnQuantity").doubleValue();
                        totalQty = totalQty + itemQty;
                        orderId = returnItem.getString("orderId");
                    }
                    partyId = returnHeader.getString("fromPartyId");
                    List partyContactMechs = new ArrayList();
                    GenericValue contactMech = null;
                    GenericValue telecomNumber =null;
                    String emailString = null;
                    partyContactMechs = delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId));
                    Iterator pcmIter = partyContactMechs.iterator();
                    while (pcmIter.hasNext()) {
                        GenericValue partyContactMech = (GenericValue) pcmIter.next();
                        String contactMechId = partyContactMech.getString("contactMechId");
                        contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
                        String contactMechTypeId = contactMech.getString("contactMechTypeId");
                        if(contactMechTypeId.equals("EMAIL_ADDRESS")) {
                            emailString = contactMech.getString("infoString");
                        }
                        if(contactMechTypeId.equals("TELECOM_NUMBER")) {
                            telecomNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));
                        }
                    }
                    String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                    String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                    String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'Z");
                    Timestamp timestamp = UtilDateTime.nowTimestamp();
                    String sentDate = dateFormat.format(timestamp);
                    Map bodyParameters = new HashMap();
                    bodyParameters.put("returnId", returnId);
                    bodyParameters.put("returnItems", returnItems);
                    bodyParameters.put("totalQty", new Double(totalQty));
                    bodyParameters.put("postalAddress", postalAddress);
                    bodyParameters.put("telecomNumber", telecomNumber);
                    bodyParameters.put("emailString", emailString);
                    bodyParameters.put("logicalId", logicalId);
                    bodyParameters.put("authId", authId);
                    bodyParameters.put("referenceId", referenceId);
                    bodyParameters.put("sentDate", sentDate);
                    bodyParameters.put("returnId", returnId);
                    String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ReceiveDelivery");
                    Map emfsCtx = new HashMap();
                    emfsCtx.put("bodyParameters", bodyParameters);
                    emfsCtx.put("bodyScreenUri", bodyScreenUri);
                            
                    // export the message
                    try {
                        dispatcher.runSync("exportMsgFromScreen", emfsCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError("Error in exporting message" + e.getMessage(), module);
                        return ServiceUtil.returnError("Error in exporting message");
                    }
                            
                    // prepare map to store BOD information
                    Map comiCtx = new HashMap();
                    comiCtx.put("logicalId", logicalId);
                    comiCtx.put("authId", authId);
                    comiCtx.put("referenceId", referenceId);
                    comiCtx.put("sentDate", timestamp);
                    comiCtx.put("component", "INVENTORY");
                    comiCtx.put("task", "RMA");  
                    comiCtx.put("outgoingMessage", "Y");
                    comiCtx.put("confirmation", "1");
                    comiCtx.put("bsrVerb", "RECEIVE");
                    comiCtx.put("bsrNoun", "DELIVERY");
                    comiCtx.put("bsrRevision", "001");
                    comiCtx.put("processingStatusId", statusId);        
                    comiCtx.put("returnId", returnId);
                    comiCtx.put("orderId", orderId);
                    comiCtx.put("userLogin", userLogin);
                    try {
                        dispatcher.runSync("createOagisMessageInfo", comiCtx);
                    } catch (GenericServiceException e) {
                          return ServiceUtil.returnError("Error in creating message info" + e.getMessage());
                    }
                }
            } catch (Exception e) {
                  Debug.logError("Error in Processing" + e.getMessage(), module);
            }
        }
        return ServiceUtil.returnSuccess("Service Completed Successfully");
    }
}
