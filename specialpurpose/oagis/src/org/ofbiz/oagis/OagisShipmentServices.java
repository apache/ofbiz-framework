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
import java.sql.Timestamp;

import javolution.util.FastMap;
import javolution.util.FastList;

import org.w3c.dom.Document;

import org.ofbiz.service.DispatchContext;
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
            Map reserveOrderItemInventoryCtx = FastMap.newInstance();
            Map result = null;
            //GenericValue inventoryItem = null;
            try {                
                GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", documentId));                
                String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");
                
                List shipmentItems = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", documentId, "productId",invItemItem));
                GenericValue shipmentItem =  EntityUtil.getFirst(shipmentItems);
                String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");
                //Now we have enough keys to lookup the right OrderShipment
                List orderShipments = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", documentId, "shipmentItemSeqId",shipmentItemSeqId));
                GenericValue orderShipment =   EntityUtil.getFirst(orderShipments);
                String orderId = orderShipment.getString("orderId");
                String orderItemSeqId = orderShipment.getString("orderItemSeqId");
                
                // Look for reservations in some status.
                orderItemShipGrpInvReservations = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId,"orderItemSeqId",orderItemSeqId,"shipGroupSeqId",shipGroupSeqId));                
                GenericValue orderItemShipGrpInvReservation =   EntityUtil.getFirst(orderItemShipGrpInvReservations);
                GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId",orderItemShipGrpInvReservation.get("inventoryItemId")));
                
                // Check if the inventory Item we reserved is same as Item shipped
                // If not then reserve Inventory Item
                String serialNumber = inventoryItem.getString("serialNumber");
                if(invDetailSerialNum != null) {
                    //The if codition is for chacking serialized Inventory.
                    if(!serialNumber.equals(invDetailSerialNum)) {
                        // Check if the Inventory we want is available
                        inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("productId", invItemItem, "serialNumber", invDetailSerialNum)));
                        Debug.logInfo("======== InventoryItem In Else ========="+inventoryItem, module);
                        reserveOrderItemInventoryCtx.put("inventoryItemId", inventoryItem.getString("inventoryItemId"));
                        result = dispatcher.runSync("reserveOrderItemInventory", reserveOrderItemInventoryCtx);
                        
                        Debug.logInfo("========reserveOrderItemInventory ========="+result, module);
                    }                    
                }
                Map orderItemShipGrpInvResCtx = FastMap.newInstance(); // This Map is for the issueOrderItemShipGrpInvResToShipment service.
                orderItemShipGrpInvResCtx.put("shipmentId", documentId);
                orderItemShipGrpInvResCtx.putAll(reserveOrderItemInventoryCtx);                    
                result = dispatcher.runSync("issueOrderItemShipGrpInvResToShipment", orderItemShipGrpInvResCtx);
                Debug.logInfo("==============result for issueOrderItemShipGrpInvResToShipment=========="+result, module);
                

                /*
                 Here we have to put the code for inserting the Tracking number in ShipmentPackageRouteSegment.
                 The tracking number is coming from shipUnitTrackingId.
                */
                
                /*
                //find shipmentRouteSegmentId by the help of shipmentId                
                GenericValue shipmentRouteSegment = EntityUtil.getFirst(delegator.findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId",documentId)));               
                String shipmentRouteSegmentId = (String) shipmentRouteSegment.get("shipmentRouteSegmentId");
                Debug.logInfo("================shipmentRouteSegmentId============== "+shipmentRouteSegmentId, module);
               
                //find shipmentPackageSeqId by the help of shipmentId
                GenericValue shipmentPackage = EntityUtil.getFirst(delegator.findByAnd("ShipmentPackage", UtilMisc.toMap("shipmentId",documentId)));                
                String shipmentPackageSeqId = shipmentPackage.getString("shipmentPackageSeqId");
                Debug.logInfo("==============shipmetPackageSeqId========= "+shipmentPackageSeqId, module);                                     
                
                //Code for saving the tracking code..
                Map map = new FastMap();
                map.put("shipmentId", documentId);
                map.put("shipmentRouteSegmentId", shipmentRouteSegmentId);
                map.put("shipmentPackageSeqId", shipmentPackageSeqId);
                map.put("trackingCode", shipUnitTrackingId);
                map.put("userLogin", userLogin);
                Debug.logInfo("==============ShipmentPackageRouteSeg========= "+map, module);
                result = dispatcher.runSync("createShipmentPackageRouteSeg", map);
                Debug.logInfo("==============Here is result========= "+result, module);
                */
                
            } catch (Exception e) {
                return ServiceUtil.returnError("return error"+e);
              }
        }catch (Exception e){
            Debug.logError(e, module);
        }
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.println("Service not Implemented");
        writer.flush();
        Map result = ServiceUtil.returnError("Service not Implemented");
        return result;
        
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
        String shipmentId = (String) context.get("shipmentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map csResult = null;
        Map psmMap = new HashMap();
        GenericValue orderItemShipGroup = null;
        GenericValue productStore =null;
        String orderStatusId = null;
        if (orderHeader != null) {
            orderStatusId = orderHeader.getString("statusId");
            if (orderStatusId.equals("ORDER_APPROVED")) {
                try {
                    orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId), UtilMisc.toList("shipGroupSeqId")));
                    String productStoreId = orderHeader.getString("productStoreId"); 
                    productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                    String originFacilityId = productStore.getString("inventoryFacilityId");
                    String statusId = "SHIPMENT_INPUT";
                    
                    csResult= dispatcher.runSync("createShipment", UtilMisc.toMap("primaryOrderId", orderId,"primaryShipGroupSeqId",orderItemShipGroup.get("shipGroupSeqId") ,"statusId", statusId ,"originFacilityId", originFacilityId ,"userLogin", userLogin));
                    shipmentId = (String) csResult.get("shipmentId");

                    List orderItems = new ArrayList();
                    Map orderItemCtx = new HashMap();
                    orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        
                        orderItemCtx.put("orderId", orderItem.get("orderId"));
                        orderItemCtx.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                        orderItemCtx.put("shipmentId", shipmentId);
                        orderItemCtx.put("quantity", orderItem.get("quantity"));
                        orderItemCtx.put("userLogin", userLogin);
                         
                        dispatcher.runSync("addOrderShipmentToShipment", orderItemCtx);
                    }
                    String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                    String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                    String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                    Timestamp timestamp = null;
                    timestamp = UtilDateTime.nowTimestamp();

                    psmMap.put("logicalId", logicalId);
                    psmMap.put("authId", authId);
                    psmMap.put("referenceId", referenceId);
                    psmMap.put("sentDate", timestamp);
                    psmMap.put("shipmentId", shipmentId);
                    psmMap.put("userLogin", userLogin);
                    
                    // send the process shipment message
                    dispatcher.runSync("sendProcessShipmentMsg", psmMap);                    
                } catch (Exception e) {
                    Debug.logError("Error in processing" + e.getMessage(), module);
                }
            }
        }
        psmMap.put("component", "INVENTORY");
        psmMap.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUES" which is more than 10 char 
        psmMap.put("outgoingMessage", "Y");
        psmMap.put("confirmation", "1");
        psmMap.put("bsrVerb", "PROCESS");
        psmMap.put("bsrNoun", "SHIPMENT");
        psmMap.put("bsrRevision", "001");
        psmMap.put("processingStatusId", orderStatusId);        
        psmMap.put("orderId", orderId);        
        try {
            dispatcher.runSync("createOagisMessageInfo", psmMap);
        } catch (Exception e) {
            return ServiceUtil.returnError("error in creating message info" + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
