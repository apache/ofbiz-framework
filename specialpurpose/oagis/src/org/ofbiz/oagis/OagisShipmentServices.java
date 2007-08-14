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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
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
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
        
    /** if TRUE then must exist, if FALSE must not exist, if null don't care */
    public static final Boolean requireSerialNumberExist;
    static {
        String requireSerialNumberExistStr = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Warehouse.RequireSerialNumberExist");
        if ("true".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.TRUE;
        } else if ("false".equals(requireSerialNumberExistStr)) {
            requireSerialNumberExist = Boolean.FALSE;
        } else {
            requireSerialNumberExist = null;
        }
    }

    public static Map showShipment(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List errorMapList = FastList.newInstance();
            
        GenericValue userLogin = null; 
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

        String sentDate = UtilXml.childElementValue(controlAreaElement, "os:DATETIMEISO");
        Timestamp sentTimestamp = OagisServices.parseIsoDateString(sentDate, errorMapList);
        
        Element dataAreaElement = UtilXml.firstChildElement(showShipmentElement, "ns:DATAAREA"); // n
        Element daShowShipmentElement = UtilXml.firstChildElement(dataAreaElement, "ns:SHOW_SHIPMENT"); // n
        Element shipmentElement = UtilXml.firstChildElement(daShowShipmentElement, "ns:SHIPMENT"); // n                               
        String shipmentId = UtilXml.childElementValue(shipmentElement, "of:DOCUMENTID"); // of           

        oagisMsgInfoCtx.put("logicalId", logicalId);
        oagisMsgInfoCtx.put("component", component);
        oagisMsgInfoCtx.put("task", task);
        oagisMsgInfoCtx.put("referenceId", referenceId);
        oagisMsgInfoCtx.put("confirmation", confirmation);
        oagisMsgInfoCtx.put("authId", authId);
        oagisMsgInfoCtx.put("outgoingMessage", "N");
        oagisMsgInfoCtx.put("receivedDate", nowTimestamp);
        oagisMsgInfoCtx.put("sentDate", sentTimestamp);
        oagisMsgInfoCtx.put("shipmentId", shipmentId);
        oagisMsgInfoCtx.put("userLogin", userLogin);
        oagisMsgInfoCtx.put("processingStatusId", "OAGMP_RECEIVED");
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
            if (ServiceUtil.isError(oagisMsgInfoResult)){
                String errMsg = ServiceUtil.getErrorMessage(oagisMsgInfoResult);
                // errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */
        } catch (GenericServiceException e){
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: "+e.toString();
            // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            Debug.logError(e, errMsg, module);
        }
           
        GenericValue shipment = null;
        try {
            shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
        } catch (GenericEntityException e) {
            String errMsg = "Error getting Shipment from database: "+ e.toString();
            Debug.logInfo(e, errMsg, module);
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
        }
        
        if (shipment == null) {
            String errMsg = "Could not find Shipment id ID [" + shipmentId + "]";
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "ShipmentIdNotValid"));
        }
        
        String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");                
        String originFacilityId = shipment.getString("originFacilityId");                              
        
        List shipUnitElementList = UtilXml.childElementList(daShowShipmentElement, "ns:SHIPUNIT"); // n
        if (errorMapList.size() == 0 && UtilValidate.isNotEmpty(shipUnitElementList)) {
            Element shipUnitElement = (Element)shipUnitElementList.get(0);
            String trackingNum = UtilXml.childElementValue(shipUnitElement, "of:TRACKINGID"); // of
            String carrierCode = UtilXml.childElementValue(shipUnitElement, "of:CARRIER"); // of
            if (UtilValidate.isNotEmpty(carrierCode)){
                String carrierPartyId = null;
                if ( carrierCode.startsWith("F") || carrierCode.startsWith("f")) {                
                    carrierPartyId = "FEDEX";                                           
                } else if (carrierCode.startsWith("U")|| carrierCode.startsWith("u")) {
                    carrierPartyId = "UPS";                                            
                }
                try {
                    Map resultMap = dispatcher.runSync("updateShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", "00001", "carrierPartyId", carrierPartyId, "trackingIdNumber", trackingNum, "userLogin", userLogin));                        
                    if (ServiceUtil.isError(resultMap)){
                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "updateShipmentRouteSegmentError"));
                        Debug.logError(errMsg, module);
                    }
                } catch (GenericServiceException e) {
                    Debug.logInfo(e, module);
                    String errMsg = "Error executing updateShipmentRouteSegment Service: "+e.toString();
                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                }
            }
            
            Iterator shipUnitElementItr = shipUnitElementList.iterator();
            while (shipUnitElementItr.hasNext()) {                 
                shipUnitElement = (Element) shipUnitElementItr.next();
                String shipmentPackageSeqId = UtilXml.childElementValue(shipUnitElement, "of:SHPUNITSEQ"); // of
                List invItemElementList = UtilXml.childElementList(shipUnitElement, "ns:INVITEM"); //n
                if (UtilValidate.isNotEmpty(invItemElementList)) {
                    // sort the INVITEM elements by ITEM so that all shipments are processed in the same order, avoids deadlocking problems we've seen with concurrently processed orders
                    List invitemMapList = FastList.newInstance();
                    Iterator invItemElementIter = invItemElementList.iterator();
                    while (invItemElementIter.hasNext()) {                 
                        Element invItemElement = (Element) invItemElementIter.next();
                        String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of
                        Map invitemMap = FastMap.newInstance();
                        invitemMap.put("productId", productId);
                        invitemMap.put("invItemElement", invItemElement);
                        invitemMapList.add(invitemMap);
                    }
                    UtilMisc.sortMaps(invitemMapList, UtilMisc.toList("productId"));
                    
                    Iterator invitemMapIter = invitemMapList.iterator();
                    while (invitemMapIter.hasNext()) {
                        Map invitemMap = (Map) invitemMapIter.next();
                        Element invItemElement = (Element) invitemMap.get("invItemElement");
                        String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of
                        
                        try {
                            Element quantityElement = UtilXml.firstChildElement(invItemElement, "os:QUANTITY"); // os
                            String quantityValueStr = UtilXml.childElementValue(quantityElement, "of:VALUE"); // os
                            // TODO: <of:NUMOFDEC>0</of:NUMOFDEC> should always be 0, but might want to add code to check
                            Integer messageQuantity = Integer.valueOf(quantityValueStr);

                            GenericValue shipmentItem = EntityUtil.getFirst(delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId, "productId",productId)));                    
                            String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");                      
                            GenericValue orderShipment = EntityUtil.getFirst(delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId)));                    
                            String orderId = orderShipment.getString("orderId");                
                            String orderItemSeqId = orderShipment.getString("orderItemSeqId");                
                            GenericValue product = delegator.findByPrimaryKey("Product",UtilMisc.toMap("productId", productId));                    
                            String requireInventory = product.getString("requireInventory");                    
                            if (requireInventory == null) {
                                requireInventory = "N";
                            }
                            
                            // NOTE: there could be more than one reservation record for a given shipment item? for example if there wasn't enough quantity in one inventory item and reservations on two were needed
                            List orderItemShipGrpInvReservationList = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId,"shipGroupSeqId",shipGroupSeqId));
                            
                            // find the total quantity for all reservations
                            int totalReserved = 0;
                            Iterator orderItemShipGrpInvReservationCountIter = orderItemShipGrpInvReservationList.iterator();
                            while (orderItemShipGrpInvReservationCountIter.hasNext()) {
                                GenericValue orderItemShipGrpInvReservation = (GenericValue) orderItemShipGrpInvReservationCountIter.next();
                                if (orderItemShipGrpInvReservation.getDouble("quantity") != null) {
                                    totalReserved += orderItemShipGrpInvReservation.getDouble("quantity").doubleValue();
                                }
                            }

                            List serialNumberList = FastList.newInstance();
                            List invDetailElementList = UtilXml.childElementList(invItemElement, "ns:INVDETAIL"); //n
                            Iterator invDetailElementItr = invDetailElementList.iterator();
                            while (invDetailElementItr.hasNext()) {
                                Element invDetailElement = (Element) invDetailElementItr.next();
                                String serialNumber = UtilXml.childElementValue(invDetailElement, "of:SERIALNUM"); // os
                                if (UtilValidate.isNotEmpty(serialNumber)) {
                                    serialNumberList.add(serialNumber);
                                }
                            }

                            // do some validations
                            boolean continueLoop = false;
                            if (UtilValidate.isNotEmpty(serialNumberList)) {
                                if (messageQuantity.intValue() != serialNumberList.size()) {
                                    String errMsg = "Error: the quantity in the message [" + messageQuantity.intValue() + "] did not match the number of serial numbers passed [" + serialNumberList.size() + "].";
                                    errorMapList.add(UtilMisc.toMap("reasonCode", "QuantitySerialMismatch", "description", errMsg));
                                    Debug.logInfo(errMsg, module);
                                    continueLoop = true;
                                }
                            } 
                            if ((int) totalReserved != messageQuantity.intValue()) {
                                String errMsg = "Not enough serial numbers [" + serialNumberList.size() + "] for the quantity [" + messageQuantity.intValue() + "].";
                                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumbersMissing"));
                                Debug.logInfo(errMsg, module);
                                continueLoop = true;
                            }
                            
                            if (continueLoop) {
                                continue;
                            }
                            
                            Iterator serialNumberIter = serialNumberList.iterator();
                            Iterator orderItemShipGrpInvReservationIter = orderItemShipGrpInvReservationList.iterator();
                            while (orderItemShipGrpInvReservationIter.hasNext()) {
                                GenericValue orderItemShipGrpInvReservation = (GenericValue) orderItemShipGrpInvReservationIter.next();
                                int currentResQuantity = orderItemShipGrpInvReservation.getDouble("quantity").intValue();
                                
                                Map isitspastCtx = UtilMisc.toMap("orderId", orderId, "shipGroupSeqId", shipGroupSeqId, "orderItemSeqId", orderItemSeqId);                
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
                                
                                if (UtilValidate.isNotEmpty(serialNumberList)) {
                                    for (int i = 0; i < currentResQuantity; i++) {
                                        String serialNumber = (String) serialNumberIter.next();

                                        // according to requireSerialNumberExist make sure serialNumber does or does not exist in database, add an error message as needed
                                        if (requireSerialNumberExist != null) {
                                            Set productIdSet = FastSet.newInstance();
                                            productIdSet.add(productId);
                                            // find associated refurb items, we want serial number for main item or any refurb items too
                                            List refubProductAssocs = EntityUtil.filterByDate(delegator.findByAnd("ProductAssoc", 
                                                    UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_REFURB")), true);
                                            Iterator refubProductAssocIter = refubProductAssocs.iterator();
                                            while (refubProductAssocIter.hasNext()) {
                                                GenericValue refubProductAssoc = (GenericValue) refubProductAssocIter.next();
                                                productIdSet.add(refubProductAssoc.get("productIdTo"));
                                            }
                                            EntityCondition bySerialNumberCondition = new EntityExpr(new EntityExpr("serialNumber", EntityOperator.EQUALS, serialNumber), 
                                                    EntityOperator.AND, new EntityExpr("productId", EntityOperator.IN, productIdSet));
                                            List inventoryItemsBySerialNumber = delegator.findByCondition("InventoryItem", bySerialNumberCondition, null, null);
                                            if (requireSerialNumberExist.booleanValue()) {
                                                if (inventoryItemsBySerialNumber.size() > 0) {
                                                    String errMsg = "Referenced serial numbers must already exist, but serial number [" + serialNumber + "] was not found.";
                                                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                                    continue;
                                                }
                                            } else {
                                                if (inventoryItemsBySerialNumber.size() == 0) {
                                                    String errMsg = "Referenced serial numbers must NOT already exist, but serial number [" + serialNumber + "] already exists.";
                                                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumberRequiredNotExistButFound"));
                                                    continue;
                                                }
                                            }
                                        }
                                        
                                        isitspastCtx.put("serialNumber", serialNumber);
                                        isitspastCtx.put("quantity", new Double (1));
                                        isitspastCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));
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
                                    try {
                                        //TODO: I think this else part is for NON Serialized Inv item. So it will be different service that we need to call here.
                                        isitspastCtx.put("quantity", new Double(currentResQuantity));
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
                            }
                        } catch (NumberFormatException e) {
                            String errMsg = "Error in format for number: " + e.toString();
                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "NumberFormatException"));
                            Debug.logInfo(e, errMsg, module);
                        } catch (GenericEntityException e) {
                            String errMsg = "Error executing issueSerializedInvToShipmentPackageAndSetTracking Service: " + e.toString();
                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
                            Debug.logInfo(e, errMsg, module);
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
                String errMsg = "Error executing setShipmentStatusPackedAndShipped Service: " + e.toString();
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            }   
        }  
        
        Map result = FastMap.newInstance();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map saveErrorMapListCtx = FastMap.newInstance();
            saveErrorMapListCtx.put("logicalId", logicalId);
            saveErrorMapListCtx.put("component", component);
            saveErrorMapListCtx.put("task", task);
            saveErrorMapListCtx.put("referenceId", referenceId);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e){
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map sendConfirmBodCtx = FastMap.newInstance();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                sendConfirmBodCtx.put("origRefId", shipmentId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e){
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
            
            // DEJ20070807 what was this next line commented out? if there are errors we want to return an error so this will roll back 
            result.putAll(ServiceUtil.returnError("Errors found processing message; information saved and return error sent back"));
            return result;
        } else {
            oagisMsgInfoCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } catch (GenericServiceException e){
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }
        
        result.putAll(ServiceUtil.returnSuccess("Service Completed Successfully"));
        return result;
    }

    public static Map oagisProcessShipment(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        
        // Check if order is not on back order before processing shipment
        try {
            Map checkOrderResp = dispatcher.runSync("checkOrderIsOnBackOrder", UtilMisc.toMap("orderId", orderId));
            if (((Boolean) checkOrderResp.get("isBackOrder")).booleanValue()) {
                Debug.logWarning("Order [" + orderId + "] is on back order, cannot Process Shipment", module);
                return ServiceUtil.returnSuccess();
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        
        String sendToUrl = (String) context.get("sendToUrl");
        if (UtilValidate.isEmpty(sendToUrl)) {
            sendToUrl = UtilProperties.getPropertyValue("oagis.properties", "url.send.processShipment");
        }
        
        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.filename.base", "");
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ProcessShipment" + orderId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.directory");
        }
        
        OutputStream out = (OutputStream) context.get("outputStream");
        
        if (Debug.infoOn()) Debug.logInfo("Call to oagisProcessShipment for orderId [" + orderId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);
        
        Map result = ServiceUtil.returnSuccess();
        MapStack bodyParameters =  MapStack.create();

        // the userLogin passed in will usually be the customer, so don't use it; use the system user instead
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting userLogin", module);
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
                // first check some things...
                OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
                try {
                    // before doing or saving anything see if any OrderItems are Products with isPhysical=Y
                    if (!orderReadHelper.hasPhysicalProductItems()) {
                        // no need to process shipment, return success
                        return ServiceUtil.returnSuccess();
                    }
                } catch (GenericEntityException e) {
                    String errMsg = "Error checking order: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                if (!orderReadHelper.hasShippingAddress()) {
                    return ServiceUtil.returnError("Cannot send Process Shipment for order [" + orderId + "], it has no shipping address.");
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
                    
                Timestamp timestamp = UtilDateTime.nowTimestamp();
                String sentDate = OagisServices.isoDateFormat.format(timestamp);
                bodyParameters.put("sentDate", sentDate);
                comiCtx.put("sentDate", timestamp);
                
                // prepare map to Create Oagis Message Info
                comiCtx.put("processingStatusId", "OAGMP_TRIGGERED");
                comiCtx.put("component", "INVENTORY");
                comiCtx.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUEST" which is more than 10 char
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "PROCESS");
                comiCtx.put("bsrNoun", "SHIPMENT");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("orderId", orderId);
                comiCtx.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e){
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
                if (Debug.infoOn()) Debug.logInfo("Saved OagisMessageInfo for oagisProcessShipment message for orderId [" + orderId + "]", module);

                String shipmentId = null;
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
                    List shipmentItems = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId));
                    bodyParameters.put("shipmentItems", shipmentItems);

                    GenericValue  address = EntityUtil.getFirst(orderReadHelper.getShippingLocations());
                    bodyParameters.put("address", address);
                    String emailString = orderReadHelper.getOrderEmailString();
                    bodyParameters.put("emailString", emailString);
                    String contactMechId = shipment.getString("destinationTelecomNumberId");
                    GenericValue telecomNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));
                    bodyParameters.put("telecomNumber", telecomNumber);
                    
                    orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId)));
                    bodyParameters.put("orderItemShipGroup", orderItemShipGroup);
                    Set correspondingPoIdSet = FastSet.newInstance();

                    List orderItems = orderReadHelper.getOrderItems();
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
                
                bodyParameters.put("shipmentId", shipmentId);
                bodyParameters.put("orderId", orderId);
                bodyParameters.put("userLogin", userLogin);

                
                String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ProcessShipment");

                String outText = null;
                try {
                    Writer writer = new StringWriter();
                    ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, htmlScreenRenderer);
                    screens.render(bodyScreenUri);
                    writer.close();
                    outText = writer.toString();
                } catch (Exception e) {
                    String errMsg = "Error rendering message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                if (Debug.infoOn()) Debug.logInfo("Finished rendering oagisProcessShipment message for orderId [" + orderId + "]", module);

                try {
                    comiCtx.put("processingStatusId", "OAGMP_OGEN_SUCCESS");
                    comiCtx.put("shipmentId", shipmentId);
                    if (OagisServices.debugSaveXmlOut) {
                        comiCtx.put("fullMessageXml", outText);
                    }
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e){
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
                
                Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);

                if (Debug.infoOn()) Debug.logInfo("Message send done for oagisProcessShipment for orderId [" + orderId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);
                try {
                    comiCtx.put("processingStatusId", "OAGMP_SENT");
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e){
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
                
                if (sendMessageReturn != null) {
                    return sendMessageReturn;
                }
            }
        }
        return result;
    }
    
    public static Map oagisReceiveDelivery(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        
        String sendToUrl = (String) context.get("sendToUrl");
        if (UtilValidate.isEmpty(sendToUrl)) {
            sendToUrl = UtilProperties.getPropertyValue("oagis.properties", "url.send.receiveDelivery");
        }

        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.filename.base", "");
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ReceiveDelivery" + returnId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = UtilProperties.getPropertyValue("oagis.properties", "test.save.outgoing.directory");
        }

        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting system userLogin", module);
        }

        OutputStream out = (OutputStream) context.get("outputStream");
        
        Map result = ServiceUtil.returnSuccess();
        MapStack bodyParameters =  MapStack.create();

        GenericValue returnHeader = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (returnHeader != null) {
            String statusId = returnHeader.getString("statusId");
            if ("RETURN_ACCEPTED".equals(statusId)) {
                Map comiCtx = FastMap.newInstance();

                String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                bodyParameters.put("logicalId", logicalId);
                comiCtx.put("logicalId", logicalId);
                
                String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                bodyParameters.put("authId", authId);
                comiCtx.put("authId", authId);
                
                String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                bodyParameters.put("referenceId", referenceId);
                comiCtx.put("referenceId", referenceId);
                
                Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
                String sentDate = OagisServices.isoDateFormat.format(nowTimestamp);
                bodyParameters.put("sentDate", sentDate);

                // prepare map to Create Oagis Message Info
                comiCtx.put("component", "INVENTORY");
                comiCtx.put("task", "RMA"); 
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "RECEIVE");
                comiCtx.put("bsrNoun", "DELIVERY");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("returnId", returnId);
                comiCtx.put("sentDate", nowTimestamp);
                comiCtx.put("userLogin", userLogin);
                comiCtx.put("processingStatusId", "OAGMP_TRIGGERED");
                try {
                    dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }

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

                String partyId = returnHeader.getString("fromPartyId");

                try {    
                    GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", returnHeader.getString("originContactMechId")));
                    bodyParameters.put("postalAddress", postalAddress);
                    bodyParameters.put("partyNameView", delegator.findByPrimaryKey("PartyNameView", UtilMisc.toMap("partyId", partyId)));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
       
                // calculate total qty of return items in a shipping unit received, order associated with return
                double totalQty = 0.0;
                Map serialNumberListByReturnItemSeqIdMap = FastMap.newInstance();
                bodyParameters.put("serialNumberListByReturnItemSeqIdMap", serialNumberListByReturnItemSeqIdMap);
                Iterator riIter = returnItems.iterator();
                while (riIter.hasNext()) {
                    GenericValue returnItem = (GenericValue) riIter.next();
                    double itemQty = returnItem.getDouble("returnQuantity").doubleValue();
                    totalQty += itemQty;
                    
                    // for each ReturnItem also get serial numbers using ItemIssuanceAndInventoryItem
                    // NOTE: technically if the ReturnItem.quantity != OrderItem.quantity then we don't know which serial number is being returned, so rather than guessing we will send it only in that case
                    try {
                        GenericValue orderItem = returnItem.getRelatedOne("OrderItem");
                        if (orderItem != null) {
                            if (orderItem.getDouble("quantity").doubleValue() == itemQty) {
                                List itemIssuanceAndInventoryItemList = delegator.findByAnd("ItemIssuanceAndInventoryItem", 
                                        UtilMisc.toMap("orderId", orderItem.get("orderId"), "orderItemSeqId", orderItem.get("orderItemSeqId"), 
                                                "inventoryItemTypeId", "SERIALIZED_INV_ITEM"));
                                if (itemIssuanceAndInventoryItemList.size() == itemQty) {
                                    List serialNumberList = FastList.newInstance();
                                    serialNumberListByReturnItemSeqIdMap.put(returnItem.get("returnItemSeqId"), serialNumberList);
                                    Iterator itemIssuanceAndInventoryItemIter = itemIssuanceAndInventoryItemList.iterator();
                                    while (itemIssuanceAndInventoryItemIter.hasNext()) {
                                        GenericValue itemIssuanceAndInventoryItem = (GenericValue) itemIssuanceAndInventoryItemIter.next();
                                        serialNumberList.add(itemIssuanceAndInventoryItem.get("serialNumber"));
                                    }
                                } else {
                                    // TODO: again a quantity mismatch, whatever to do?
                                    Debug.logWarning("Number of serial numbers [" + itemIssuanceAndInventoryItemList.size() + "] did not match quantity [" + itemQty + "] for return item: " + returnItem.getPrimaryKey(), module);
                                }
                            } else {
                                // TODO: we don't know which serial numbers are returned, should we throw an error? probably not, just do what we can
                                Debug.logWarning("Could not get matching serial numbers because order item quantity [" + orderItem.getDouble("quantity") + "] did not match quantity [" + itemQty + "] for return item: " + returnItem.getPrimaryKey(), module);
                            }
                        }
                    } catch (GenericEntityException e) {
                        String errMsg = "Error getting data for processing return message: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }
                }
                bodyParameters.put("totalQty", new Double(totalQty));
                
                String emailString = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator).getString("infoString");
                bodyParameters.put("emailString", emailString);
    
                GenericValue telecomNumber = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
                bodyParameters.put("telecomNumber", telecomNumber);

                String entryDate = OagisServices.isoDateFormat.format(returnHeader.getTimestamp("entryDate"));
                bodyParameters.put("entryDate", entryDate);
                
                bodyParameters.put("returnId", returnId);

                String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ReceiveDelivery");
                String outText = null;
                try {
                    Writer writer = new StringWriter();
                    ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, htmlScreenRenderer);
                    screens.render(bodyScreenUri);
                    writer.close();
                    outText = writer.toString();
                } catch (Exception e) {
                    String errMsg = "Error rendering message: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }

                try {
                    comiCtx.put("orderId", orderId);
                    comiCtx.put("processingStatusId", "OAGMP_OGEN_SUCCESS");
                    if (OagisServices.debugSaveXmlOut) {
                        comiCtx.put("fullMessageXml", outText);
                    }
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }

                Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);

                try {
                    comiCtx.put("processingStatusId", "OAGMP_SENT");
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }

                if (sendMessageReturn != null) {
                    return sendMessageReturn;
                }
            }
        }    
        return result;
    }
}
