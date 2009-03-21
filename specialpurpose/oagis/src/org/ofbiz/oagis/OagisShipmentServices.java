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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceAuthException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.ServiceValidationException;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class OagisShipmentServices {
 
    public static final String module = OagisShipmentServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
 
    public static final Set invalidShipmentStatusSet = UtilMisc.toSet("SHIPMENT_CANCELLED", "SHIPMENT_PICKED", "SHIPMENT_PACKED", "SHIPMENT_SHIPPED", "SHIPMENT_DELIVERED");
 
    public static final String resource = "OagisUiLabels";

    public static final String certAlias = UtilProperties.getPropertyValue("oagis.properties", "auth.client.certificate.alias");
    public static final String basicAuthUsername = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.username");
    public static final String basicAuthPassword = UtilProperties.getPropertyValue("oagis.properties", "auth.basic.password");

    public static final String oagisMainNamespacePrefix = "n";
    public static final String oagisSegmentsNamespacePrefix = "os";
    public static final String oagisFieldsNamespacePrefix = "of";
 
    public static Map oagisReceiveShowShipment(DispatchContext ctx, Map context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
 
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List errorMapList = FastList.newInstance();
 
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
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

        Map omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

        // always log this to make messages easier to find
        Debug.log("Processing oagisReceiveShowShipment for shipmentId [" + shipmentId + "] message ID [" + omiPkMap + "]", module);
 
        // before getting into this check to see if we've tried once and had an error, if so set isErrorRetry even if it wasn't passed in
        GenericValue previousOagisMessageInfo = null;
        try {
            previousOagisMessageInfo = delegator.findByPrimaryKey("OagisMessageInfo", omiPkMap);
        } catch (GenericEntityException e) {
            String errMsg = "Error getting OagisMessageInfo from database for shipment ID [" + shipmentId + "] message ID [" + omiPkMap + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            // anything else to do about this? we don't really want to send the error back or anything...
        }
 
        if (previousOagisMessageInfo != null && !isErrorRetry) {
            if ("OAGMP_SYS_ERROR".equals(previousOagisMessageInfo.getString("processingStatusId"))) {
                isErrorRetry = true;
            } else {
                // message already in the db, but is not in a system error state...
                String errMsg = "Message received for shipmentId [" + shipmentId + "] message ID [" + omiPkMap + "] was already partially processed but is not in a system error state, needs manual review; message ID: " + omiPkMap;
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }

        oagisMsgInfoCtx.putAll(omiPkMap);
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
            if (isErrorRetry) {
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } else {
                dispatcher.runSync("createOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            }
            /* running async for better error handling
            if (ServiceUtil.isError(oagisMsgInfoResult)) {
                String errMsg = ServiceUtil.getErrorMessage(oagisMsgInfoResult);
                // errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            Debug.logError(e, errMsg, module);
        }
 
        GenericValue shipment = null;
        try {
            shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
        } catch (GenericEntityException e) {
            String errMsg = "Error getting Shipment from database for ID [" + shipmentId + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "GenericEntityException"));
        }
 
        if (shipment == null) {
            String errMsg = "Could not find Shipment ID [" + shipmentId + "]";
            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "ShipmentIdNotValid"));
        } else {
            if (invalidShipmentStatusSet.contains(shipment.get("statusId"))) {
                String errMsg = "Shipment with ID [" + shipmentId + "] is in a status [" + shipment.get("statusId") + "] that means it has been or is being shipped, so this Show Shipment message may be a duplicate.";
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "ShipmentInBadStatus"));
            }
        }
 
        List shipUnitElementList = UtilXml.childElementList(daShowShipmentElement, "ns:SHIPUNIT"); // n
        if (errorMapList.size() == 0 && UtilValidate.isNotEmpty(shipUnitElementList)) {
            try {
                String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");
                String originFacilityId = shipment.getString("originFacilityId");
 
                Element shipUnitElement = (Element)shipUnitElementList.get(0);
                String trackingNum = UtilXml.childElementValue(shipUnitElement, "of:TRACKINGID"); // of
                String carrierCode = UtilXml.childElementValue(shipUnitElement, "of:CARRIER"); // of
                if (UtilValidate.isNotEmpty(carrierCode)) {
                    String carrierPartyId = null;
                    if ( carrierCode.startsWith("F") || carrierCode.startsWith("f")) {
                        carrierPartyId = "FEDEX";
                    } else if (carrierCode.startsWith("U")|| carrierCode.startsWith("u")) {
                        carrierPartyId = "UPS";
                    }
                    Map resultMap = dispatcher.runSync("updateShipmentRouteSegment", UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", "00001", "carrierPartyId", carrierPartyId, "trackingIdNumber", trackingNum, "userLogin", userLogin));
                    if (ServiceUtil.isError(resultMap)) {
                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "updateShipmentRouteSegmentError"));
                        Debug.logError(errMsg, module);
                    }
                }
 
                // TODO: looks like we may have multiple shipunits for a single productId, AND with things split into
                //multiple package we may need to sort by the packages by productId to avoid deadlocks, just like we
                //do with INVITEMs... note sure exactly how that will work
 
                Iterator shipUnitElementItr = shipUnitElementList.iterator();
                while (shipUnitElementItr.hasNext()) {
                    shipUnitElement = (Element) shipUnitElementItr.next();
                    String shipmentPackageSeqId = UtilXml.childElementValue(shipUnitElement, "of:SHPUNITSEQ"); // of
                    List invItemElementList = UtilXml.childElementList(shipUnitElement, "ns:INVITEM"); //n
                    if (UtilValidate.isNotEmpty(invItemElementList)) {

                        // sort the INVITEM elements by ITEM so that all shipments are processed in the same order, avoids deadlocking problems we've seen with concurrently processed orders
                        List invitemMapList = FastList.newInstance();
                        Iterator invItemElementIter = invItemElementList.iterator();
                        boolean foundBadProductId = false;
                        while (invItemElementIter.hasNext()) {
                            Element invItemElement = (Element) invItemElementIter.next();
                            String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of

                            // make sure productId is valid
                            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
                            if (product == null) {
                                String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                                errorMapList.add(UtilMisc.toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                                Debug.logError(errMsg, module);
                                foundBadProductId = true;
                                continue;
                            }

                            Map invitemMap = FastMap.newInstance();
                            invitemMap.put("productId", productId);
                            // support multiple INVITEM elements for a given productId
                            UtilMisc.addToListInMap(invItemElement, invitemMap, "invItemElementList");
                            invitemMapList.add(invitemMap);
                        }
 
                        if (foundBadProductId) {
                            continue;
                        }
 
                        UtilMisc.sortMaps(invitemMapList, UtilMisc.toList("productId"));
 
                        Iterator invitemMapIter = invitemMapList.iterator();
                        while (invitemMapIter.hasNext()) {
                            Map invitemMap = (Map) invitemMapIter.next();
                            List localInvItemElementList = (List) invitemMap.get("invItemElementList");
 
                            Iterator localInvItemElementIter = localInvItemElementList.iterator();
                            while (localInvItemElementIter.hasNext()) {
                                Element invItemElement = (Element) localInvItemElementIter.next();
                                String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of
 
                                // this is based on the SHPUNIT which is basically a box/package, but we'll try to find the item with it if applicable
                                String possibleShipmentItemSeqId = null;
                                if (UtilValidate.isNotEmpty(shipmentPackageSeqId)) {
                                    possibleShipmentItemSeqId = UtilFormatOut.formatPaddedNumber(Long.parseLong(shipmentPackageSeqId), 5);
                                }
 
                                Element quantityElement = UtilXml.firstChildElement(invItemElement, "os:QUANTITY"); // os
                                String quantityValueStr = UtilXml.childElementValue(quantityElement, "of:VALUE"); // os
                                // TODO: <of:NUMOFDEC>0</of:NUMOFDEC> should always be 0, but might want to add code to check
                                Integer messageQuantity = Integer.valueOf(quantityValueStr);

                                // do a few things to try to find the ShipmentItem corresponding to the INVITEM
                                List shipmentItemList = null;
 
                                // try getting it by the unit number, which is bogus but can be what some try IFF there is only one INVITEM in the SHPUNIT
                                if (invitemMapList.size() == 1 && localInvItemElementList.size() == 1 && UtilValidate.isNotEmpty(possibleShipmentItemSeqId)) {
                                    GenericValue shipmentItem = delegator.findByPrimaryKey("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", possibleShipmentItemSeqId));
                                    if (shipmentItem != null && !productId.equals(shipmentItem.getString("productId"))) {
                                        // found an item, but it was for the wrong Product!
                                        shipmentItem = null;
                                    }
                                    if (shipmentItem != null) {
                                        Debug.logInfo("For Shipment [" + shipmentId + "] found ShipmentItem based on Package/Unit ID, possibleShipmentItemSeqId is [" + possibleShipmentItemSeqId + "]", module);
                                        shipmentItemList = UtilMisc.toList(shipmentItem);
                                    }
                                }
 
                                if (UtilValidate.isEmpty(shipmentItemList)) {
                                    shipmentItemList = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId, "productId",productId));
                                    if (UtilValidate.isEmpty(shipmentItemList)) {
                                        String errMsg = "Could not find Shipment Item for Shipment with ID [" + shipmentId + "] and Product with ID [" + productId + "].";
                                        errorMapList.add(UtilMisc.toMap("reasonCode", "ShipmentItemForProductNotFound", "description", errMsg));
                                        Debug.logError(errMsg, module);
                                        continue;
                                    }
 
                                    // try to isolate it to one item, ie find the first in the list that matches the quantity
                                    //AND that has not already been used/issued
                                    Iterator shipmentItemIter = shipmentItemList.iterator();
                                    while (shipmentItemIter.hasNext()) {
                                        GenericValue shipmentItem = (GenericValue) shipmentItemIter.next();
                                        if (messageQuantity.intValue() == shipmentItem.getDouble("quantity").intValue()) {
                                            // see if there is an ItemIssuance for this ShipmentItem, ie has already had inventory issued to it
                                            //if so then move on, this isn't the ShipmentItem you want
                                            List itemIssuanceList = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItem.get("shipmentItemSeqId")));
                                            if (itemIssuanceList.size() == 0) {
                                                // found a match, set the list to be a new list with just this item and then break
                                                shipmentItemList = UtilMisc.toList(shipmentItem);
                                                break;
                                            }
                                        }
                                    }
                                }
 
                                // TODO: if there is more than one shipmentItem, what to do? split quantity somehow?
                                // for now just get the first item, the other scenario is not yet supported
                                if (shipmentItemList.size() > 1) {
                                    String errMsg = "Could not find single Shipment Item for Shipment with ID [" + shipmentId + "] and Product with ID [" + productId + "], found [" + shipmentItemList.size() + "] and could not narrow down to one.";
                                    errorMapList.add(UtilMisc.toMap("reasonCode", "SingleShipmentItemForProductNotFound", "description", errMsg));
                                    Debug.logError(errMsg, module);
                                    continue;
                                }
                                GenericValue shipmentItem = (GenericValue) shipmentItemList.get(0);
 
                                String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");
                                GenericValue orderShipment = EntityUtil.getFirst(delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId)));
                                if (orderShipment == null) {
                                    String errMsg = "Could not find Order-Shipment record for ShipmentItem with ID [" + shipmentId + "] and Item Seq-ID [" + shipmentItemSeqId + "].";
                                    errorMapList.add(UtilMisc.toMap("reasonCode", "OrderShipmentNotFound", "description", errMsg));
                                    Debug.logError(errMsg, module);
                                    continue;
                                }
 
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
                                if (UtilValidate.isNotEmpty(serialNumberList)) {
                                    if (messageQuantity.intValue() != serialNumberList.size()) {
                                        String errMsg = "Error: the quantity in the message [" + messageQuantity.intValue() + "] did not match the number of serial numbers passed [" + serialNumberList.size() + "] for ShipmentItem with ID [" + shipmentId + "] and Item Seq-ID [" + shipmentItemSeqId + "].";
                                        errorMapList.add(UtilMisc.toMap("reasonCode", "QuantitySerialMismatch", "description", errMsg));
                                        Debug.logInfo(errMsg, module);
                                        continue;
                                    }
                                }
 
                                // because there may be more than one ShipmentItem for an OrderItem allow there to be more inventory reservations for the
                                //OrderItem than there is quantity on the current ShipmentItem
                                if ((int) totalReserved < messageQuantity.intValue()) {
                                    String errMsg = "Inventory reservation quantity [" + totalReserved + "] was less than the message quantity [" + messageQuantity.intValue() + "] so cannot receive against reservations for ShipmentItem with ID [" + shipmentId + ":" + shipmentItemSeqId + "], and OrderItem [" + orderShipment.getString("orderId") + ":" + orderShipment.getString("orderItemSeqId") + "]";
                                    errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumbersMissing"));
                                    Debug.logInfo(errMsg, module);
                                    continue;
                                }
 
                                // just receive quantity for this ShipmentItem
                                int quantityLeft;
                                int shipmentItemQuantity = shipmentItem.getDouble("quantity").intValue();
                                if (shipmentItemQuantity <= messageQuantity.intValue()) {
                                    quantityLeft = shipmentItemQuantity;
                                } else {
                                    quantityLeft = messageQuantity.intValue();
                                }
 
 
                                Iterator serialNumberIter = serialNumberList.iterator();
                                Iterator orderItemShipGrpInvReservationIter = orderItemShipGrpInvReservationList.iterator();
                                while (orderItemShipGrpInvReservationIter.hasNext() && quantityLeft > 0) {
                                    GenericValue orderItemShipGrpInvReservation = (GenericValue) orderItemShipGrpInvReservationIter.next();
                                    int currentInvResQuantity = orderItemShipGrpInvReservation.getDouble("quantity").intValue();
 
                                    int quantityToUse;
                                    if (quantityLeft > currentInvResQuantity) {
                                        quantityToUse = currentInvResQuantity;
                                        quantityLeft -= currentInvResQuantity;
                                    } else {
                                        quantityToUse = quantityLeft;
                                        quantityLeft = 0;
                                    }
 
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
                                        for (int i = 0; i < quantityToUse; i++) {
                                            String serialNumber = (String) serialNumberIter.next();

                                            if (OagisServices.requireSerialNumberExist != null) {
                                                // according to requireSerialNumberExist make sure serialNumber does or does not exist in database, add an error message as needed
                                                Set productIdSet = ProductWorker.getRefurbishedProductIdSet(productId, delegator);
                                                productIdSet.add(productId);
 
                                                EntityCondition bySerialNumberCondition = EntityCondition.makeCondition(EntityCondition.makeCondition("serialNumber", EntityOperator.EQUALS, serialNumber),
                                                        EntityOperator.AND, EntityCondition.makeCondition("productId", EntityOperator.IN, productIdSet));
                                                List inventoryItemsBySerialNumber = delegator.findList("InventoryItem", bySerialNumberCondition, null, null, null, false);
                                                if (OagisServices.requireSerialNumberExist.booleanValue()) {
                                                    if (inventoryItemsBySerialNumber.size() == 0) {
                                                        String errMsg = "Referenced serial numbers must already exist, but serial number [" + serialNumber + "] was not found. Product ID(s) considered are: " + productIdSet;
                                                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                                        continue;
                                                    }
                                                } else {
                                                    if (inventoryItemsBySerialNumber.size() > 0) {
                                                        String errMsg = "Referenced serial numbers must NOT already exist, but serial number [" + serialNumber + "] already exists. Product ID(s) considered are: " + productIdSet;
                                                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SerialNumberRequiredNotExistButFound"));
                                                        continue;
                                                    }
                                                }
                                            }
 
                                            isitspastCtx.put("serialNumber", serialNumber);
                                            isitspastCtx.put("quantity", new Double (1));
                                            isitspastCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));
                                            isitspastCtx.remove("itemIssuanceId");
                                            Map resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                            if (ServiceUtil.isError(resultMap)) {
                                                String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
                                                Debug.logError(errMsg, module);
                                            }
                                        }
                                    } else {
                                        isitspastCtx.put("quantity", new Double(quantityToUse));
                                        // NOTE: this same service is called for non-serialized inventory in spite of the name it is made to handle it
                                        Map resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                        if (ServiceUtil.isError(resultMap)) {
                                            String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
                                            Debug.logError(errMsg, module);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
 
                if (errorMapList.size() == 0) {
                    // NOTTODOLATER: to support mulitple and partial Show Shipment messages per shipment:
                    //check here if the entire shipment has been issues, ie there should be sufficient
                    //ItemIssuance quantities for the ShipmentItem quantities
                    // NOTE ON THIS DEJ20070906: this is actually really bad because it implies the shipment
                    //has been split and that isn't really allowed; maybe better to return an error!
 
                    List shipmentItemList = delegator.findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId));
                    Iterator shipmentItemIter = shipmentItemList.iterator();
                    while (shipmentItemIter.hasNext()) {
                        GenericValue shipmentItem = (GenericValue) shipmentItemIter.next();
                        int shipmentItemQuantity = shipmentItem.getDouble("quantity").intValue();
 
                        int totalItemIssuanceQuantity = 0;
                        List itemIssuanceList = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItem.get("shipmentItemSeqId")));
                        Iterator itemIssuanceIter = itemIssuanceList.iterator();
                        while (itemIssuanceIter.hasNext()) {
                            GenericValue itemIssuance = (GenericValue) itemIssuanceIter.next();
                            totalItemIssuanceQuantity += itemIssuance.getDouble("quantity").intValue();
                        }
 
                        if (shipmentItemQuantity > totalItemIssuanceQuantity) {
                            String errMsg = "ShipmentItem [" + shipmentId + ":" + shipmentItem.get("shipmentItemSeqId") + "] was not completely fulfilled; shipment item quantity was [" + shipmentItemQuantity + "], but total fulfilled is only [" + totalItemIssuanceQuantity + "]";
                            errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "ShipmentItemNotCompletelyFulfilled"));
                            Debug.logError(errMsg, module);
                        }
                    }
                }

                if (errorMapList.size() == 0) {
                    Map resultMap = dispatcher.runSync("setShipmentStatusPackedAndShipped", UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "userLogin", userLogin));
                    if (ServiceUtil.isError(resultMap)) {
                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                        errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SetShipmentStatusPackedAndShippedError"));
                        Debug.logError(errMsg, module);
                    }
                }
            } catch (Throwable t) {
                String errMsg = "System Error processing Show Shipment message for shipmentId [" + shipmentId + "] message [" + omiPkMap + "]: " + t.toString();
                errorMapList.add(UtilMisc.toMap("description", errMsg, "reasonCode", "SystemError"));

                try {
                    oagisMsgInfoCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);

                    Map saveErrorMapListCtx = FastMap.newInstance();
                    saveErrorMapListCtx.putAll(omiPkMap);
                    saveErrorMapListCtx.put("errorMapList", errorMapList);
                    saveErrorMapListCtx.put("userLogin", userLogin);
                    dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg2 = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                    Debug.logError(e, errMsg2, module);
                }
 
                Debug.logInfo(t, errMsg, module);
                // in this case we don't want to return a Confirm BOD, so return an error now
                return ServiceUtil.returnError(errMsg);
            }
        }
 
        Map result = FastMap.newInstance();
        result.putAll(omiPkMap);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            try {
                oagisMsgInfoCtx.put("processingStatusId", "OAGMP_PROC_ERROR");
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
                Map saveErrorMapListCtx = FastMap.newInstance();
                saveErrorMapListCtx.putAll(omiPkMap);
                saveErrorMapListCtx.put("errorMapList", errorMapList);
                saveErrorMapListCtx.put("userLogin", userLogin);
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map sendConfirmBodCtx = FastMap.newInstance();
                sendConfirmBodCtx.putAll(omiPkMap);
                sendConfirmBodCtx.put("errorMapList", errorMapList);
                sendConfirmBodCtx.put("userLogin", userLogin);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                sendConfirmBodCtx.put("origRefId", shipmentId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error sending Confirm BOD: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
 
            String errMsg = "Found business level errors in message processing, not saving those changes but saving error messages; first error is: " + errorMapList.get(0);
 
            // return success here so that the message won't be retried and the Confirm BOD, etc won't be sent multiple times 
            result.putAll(ServiceUtil.returnSuccess(errMsg));
 
            // however, we still don't want to save the partial results, so set rollbackOnly
            try {
                TransactionUtil.setRollbackOnly(errMsg, null);
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error setting rollback only ", module);
            }

            return result;
        } else {
            try {
                oagisMsgInfoCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
                dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
        }
 
        result.putAll(ServiceUtil.returnSuccess("Service Completed Successfully"));
        return result;
    }
 
    public static Map oagisSendProcessShipmentsFromBackOrderSet(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
 
        Set noLongerOnBackOrderIdSet = (Set) context.get("noLongerOnBackOrderIdSet");
        Debug.logInfo("Running oagisSendProcessShipmentsFromBackOrderSet with noLongerOnBackOrderIdSet=" + noLongerOnBackOrderIdSet, module);
        if (UtilValidate.isEmpty(noLongerOnBackOrderIdSet)) {
            return ServiceUtil.returnSuccess();
        }
 
        try {
            Iterator noLongerOnBackOrderIdIter = noLongerOnBackOrderIdSet.iterator();
            while (noLongerOnBackOrderIdIter.hasNext()) {
                String orderId = (String) noLongerOnBackOrderIdIter.next();
                dispatcher.runAsync("oagisSendProcessShipment", UtilMisc.toMap("orderId", orderId), true);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error calling oagisSendProcessShipment service for orders with items no longer on backorder: " + e.toString();
            return ServiceUtil.returnError(errMsg);
        }
 
        return ServiceUtil.returnSuccess();
    }

    public static Map oagisSendProcessShipment(DispatchContext ctx, Map context) {
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
 
        if (Debug.infoOn()) Debug.logInfo("Call to oagisSendProcessShipment for orderId [" + orderId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);
 
        Map result = ServiceUtil.returnSuccess();
        MapStack bodyParameters =  MapStack.create();
        bodyParameters.put("orderId", orderId);

        // the userLogin passed in will usually be the customer, so don't use it; use the system user instead
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting userLogin", module);
        }
 
        GenericValue orderHeader = null;
        GenericValue orderItemShipGroup = null;

        String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
        String referenceId = null;
        String task = "SHIPREQUEST"; // Actual value of task is "SHIPREQUEST" which is more than 10 char, need this in the db so it will match Confirm BODs, etc
        String component = "INVENTORY";
        Map omiPkMap = null;
 
        String shipmentId = null;

        try {
            // see if there are any OagisMessageInfo for this order that are in the OAGMP_OGEN_SUCCESS or OAGMP_SENT statuses, if so don't send again; these need to be manually reviewed before resending to avoid accidental duplicate messages
            List previousOagisMessageInfoList = delegator.findByAnd("OagisMessageInfo", UtilMisc.toMap("orderId", orderId, "task", task, "component", component));
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                String successMsg = "Found existing message info(s) in OAGMP_OGEN_SUCCESS, so not sending Process Shipment message for order [" + orderId + "] existing message(s) are: " + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS"));
                return ServiceUtil.returnSuccess(successMsg);
            }
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                String successMsg = "Found existing message info(s) in OAGMP_SENT status, so not sending Process Shipment message for order [" + orderId + "] existing message(s) are: " + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT"));
                return ServiceUtil.returnSuccess(successMsg);
            }
 
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                return ServiceUtil.returnError("Could not find OrderHeader with ID [" + orderId + "]");
            }
 
            List validStores = StringUtil.split(UtilProperties.getPropertyValue("oagis.properties", "Oagis.Order.ValidProductStores"), ",");
            if (UtilValidate.isNotEmpty(validStores)) {
                if (!validStores.contains(orderHeader.getString("productStoreId"))) {
                    return ServiceUtil.returnSuccess("Order [" + orderId + "] placed is not for valid Store(s)");
                }
            }
            String orderStatusId = orderHeader.getString("statusId");
            if (!"ORDER_APPROVED".equals(orderStatusId)) {
                return ServiceUtil.returnSuccess("OrderHeader not in the approved status (ORDER_APPROVED) with ID [" + orderId + "], is in the [" + orderStatusId + "] status");
            }
            if (!"SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                return ServiceUtil.returnError("OrderHeader not a sales order (SALES_ORDER) with ID [" + orderId + "]");
            }
 
            // first check some things...
            OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
            // before doing or saving anything see if any OrderItems are Products with isPhysical=Y
            if (!orderReadHelper.hasPhysicalProductItems()) {
                // no need to process shipment, return success
                return ServiceUtil.returnSuccess();
            }
            if (!orderReadHelper.hasShippingAddress()) {
                return ServiceUtil.returnError("Cannot send Process Shipment for order [" + orderId + "], it has no shipping address.");
            }

            // check payment authorization
            Map authServiceContext = FastMap.newInstance();
            authServiceContext.put("orderId", orderId);
            authServiceContext.put("userLogin", userLogin);
            authServiceContext.put("reAuth", new Boolean("true"));
            Map authResult = dispatcher.runSync("authOrderPayments", authServiceContext);
            if (!authResult.get("processResult").equals("APPROVED")) {
                return ServiceUtil.returnError("No authorized payment available, not sending Process Shipment");
            }
 
            referenceId = delegator.getNextSeqId("OagisMessageInfo");
            omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

            String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
            Timestamp timestamp = UtilDateTime.nowTimestamp();
            String sentDate = OagisServices.isoDateFormat.format(timestamp);
 
            bodyParameters.putAll(omiPkMap);
            bodyParameters.put("authId", authId);
            bodyParameters.put("sentDate", sentDate);

            // prepare map to Create Oagis Message Info
            try {
                Map comiCtx = FastMap.newInstance();
                comiCtx.putAll(omiPkMap);
                comiCtx.put("processingStatusId", "OAGMP_TRIGGERED");
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "PROCESS");
                comiCtx.put("bsrNoun", "SHIPMENT");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("orderId", orderId);
                comiCtx.put("sentDate", timestamp);
                comiCtx.put("authId", authId);
                comiCtx.put("userLogin", userLogin);
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
            if (Debug.infoOn()) Debug.logInfo("Saved OagisMessageInfo for oagisSendProcessShipment message for orderId [" + orderId + "]", module);

            // check to see if there is already a Shipment for this order
            EntityCondition findShipmentCondition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("primaryOrderId", EntityOperator.EQUALS, orderId),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_CANCELLED")
                    ), EntityOperator.AND);
            List shipmentList = delegator.findList("Shipment", findShipmentCondition, null, null, null, false);
            GenericValue shipment = EntityUtil.getFirst(shipmentList);
 
            if (shipment != null) {
                // if picked, packed, shipped, delivered then complain, no reason to process the shipment!
                String statusId = shipment.getString("statusId");
                if ("SHIPMENT_PICKED".equals(statusId) || "SHIPMENT_PACKED".equals(statusId) || "SHIPMENT_SHIPPED".equals(statusId) || "SHIPMENT_DELIVERED".equals(statusId)) {
                    return ServiceUtil.returnError("Not sending Process Shipment message because found Shipment that is already being processed, is in status [" + statusId + "]");
                }
                shipmentId = shipment.getString("shipmentId");
            } else {
                Map cospResult= dispatcher.runSync("createOrderShipmentPlan", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
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
            if (telecomNumber == null) {
                return ServiceUtil.returnError("In Send ProcessShipment Telecom number not found for orderId [" + orderId + "]");
            }
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
                boolean includeReturnLabel = false;
 
                // Get the associated return Id (replaceReturnId)
                String returnItemResponseId = returnItemResponse.getString("returnItemResponseId");
                List returnItemList = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnItemResponseId", returnItemResponseId));
                GenericValue firstReturnItem = EntityUtil.getFirst(returnItemList);
                if (firstReturnItem != null) {
                    bodyParameters.put("replacementReturnId", firstReturnItem.getString("returnId"));
                } else {
                    Debug.logWarning("Could not find a ReturnItem for returnItemResponseId [" + returnItemResponseId + "]; this really shouldn't happen but isn't a real error either. It means a ReturnItemResponse was created but not attached to any item!", module);
                }

                // return label should only be sent when we want a return label to be included; this would be for a cross-ship replacement type ReturnItem
 
                // go through the returnItemList and if any are cross-ship replacement, then include a label (not for wait replacement in other words)
                Iterator returnItemIter = returnItemList.iterator();
                while (returnItemIter.hasNext()) {
                    GenericValue returnItem = (GenericValue) returnItemIter.next();
                    if ("RTN_CSREPLACE".equals(returnItem.getString("returnTypeId"))) {
                        includeReturnLabel = true;
                    }
                }
 
                if (includeReturnLabel) {
                    bodyParameters.put("shipnotes", "RETURNLABEL");
                }
 
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
 
            bodyParameters.put("shipmentId", shipmentId);
            bodyParameters.put("orderId", orderId);
            bodyParameters.put("userLogin", userLogin);

            String bodyScreenUri = UtilProperties.getPropertyValue("oagis.properties", "Oagis.Template.ProcessShipment");
            String outText = null;
            Writer writer = new StringWriter();
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, htmlScreenRenderer);
            screens.render(bodyScreenUri);
            writer.close();
            outText = writer.toString();
            if (Debug.infoOn()) Debug.logInfo("Finished rendering oagisSendProcessShipment message for orderId [" + orderId + "]", module);

            try {
                Map uomiCtx = FastMap.newInstance();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_OGEN_SUCCESS");
                uomiCtx.put("shipmentId", shipmentId);
                uomiCtx.put("userLogin", userLogin);
                if (OagisServices.debugSaveXmlOut) {
                    uomiCtx.put("fullMessageXml", outText);
                }
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
 
            Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);
            if (sendMessageReturn != null && ServiceUtil.isError(sendMessageReturn)) {
                try {
                    Map uomiCtx = FastMap.newInstance();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SEND_ERROR");
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
                return sendMessageReturn;
            }

            if (Debug.infoOn()) Debug.logInfo("Message send done for oagisSendProcessShipment for orderId [" + orderId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);
            try {
                Map uomiCtx = FastMap.newInstance();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_SENT");
                uomiCtx.put("userLogin", userLogin);
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
        } catch (Throwable t) {
            String errMsg = "System Error doing Process Shipment message for orderId [" + orderId + "] shipmentId [" + shipmentId + "] message [" + omiPkMap + "]: " + t.toString();
            Debug.logError(t, errMsg, module);
 
            // if we have a referenceId and the omiPkMap not null, save the error status
            if (omiPkMap != null) {
                try {
                    // only do this if there is a record already in place
                    if (delegator.findByPrimaryKey("OagisMessageInfo", omiPkMap) == null) {
                        return ServiceUtil.returnError(errMsg);
                    }
 
                    Map uomiCtx = FastMap.newInstance();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    uomiCtx.put("bsrVerb", "PROCESS");
                    uomiCtx.put("bsrNoun", "SHIPMENT");
                    uomiCtx.put("orderId", orderId);
                    uomiCtx.put("shipmentId", shipmentId);
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);

                    List errorMapList = UtilMisc.toList(UtilMisc.toMap("description", errMsg, "reasonCode", "SystemError"));
                    Map saveErrorMapListCtx = FastMap.newInstance();
                    saveErrorMapListCtx.putAll(omiPkMap);
                    saveErrorMapListCtx.put("errorMapList", errorMapList);
                    saveErrorMapListCtx.put("userLogin", userLogin);
                    dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
                } catch (GeneralException e) {
                    String errMsg2 = "Error saving message error info: " + e.toString();
                    Debug.logError(e, errMsg2, module);
                }
            }
 
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }
 
    public static Map oagisSendReceiveDelivery(DispatchContext dctx, Map context) {
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

        String orderId = null;
 
        String referenceId = null;
        String task = "RMA"; // Actual value of task is "SHIPREQUEST" which is more than 10 char, need this in the db so it will match Confirm BODs, etc
        String component = "INVENTORY";
        Map omiPkMap = null;
 
        try {
            // see if there are any OagisMessageInfo for this order that are in the OAGMP_OGEN_SUCCESS or OAGMP_SENT statuses, if so don't send again; these need to be manually reviewed before resending to avoid accidental duplicate messages
            List previousOagisMessageInfoList = delegator.findByAnd("OagisMessageInfo", UtilMisc.toMap("returnId", returnId, "task", task, "component", component));
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                String successMsg = "Found existing message info(s) in OAGMP_OGEN_SUCCESS, so not sending Receive Delivery message for return [" + returnId + "] existing message(s) are: " + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS"));
                return ServiceUtil.returnSuccess(successMsg);
            }
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                String successMsg = "Found existing message info(s) in OAGMP_SENT status, so not sending Receive Delivery message for return [" + returnId + "] existing message(s) are: " + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT"));
                return ServiceUtil.returnSuccess(successMsg);
            }
 
            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader == null) {
                return ServiceUtil.returnError("Could not find Return with ID [" + returnId + "]");
            }
            String statusId = returnHeader.getString("statusId");
            if (!"RETURN_ACCEPTED".equals(statusId)) {
                return ServiceUtil.returnError("Return with ID [" + returnId + "] no in accepted status (RETURN_ACCEPTED)");
            }
 
            List returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));
            bodyParameters.put("returnItems", returnItems);
 
            orderId = EntityUtil.getFirst(returnItems).getString("orderId");
 
            String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
            String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");

            referenceId = delegator.getNextSeqId("OagisMessageInfo");
            omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            String sentDate = OagisServices.isoDateFormat.format(nowTimestamp);

            bodyParameters.putAll(omiPkMap);
            bodyParameters.put("authId", authId);
            bodyParameters.put("sentDate", sentDate);

            // prepare map to Create Oagis Message Info
            try {
                Map comiCtx = FastMap.newInstance();
                comiCtx.putAll(omiPkMap);
                comiCtx.put("outgoingMessage", "Y");
                comiCtx.put("confirmation", "1");
                comiCtx.put("bsrVerb", "RECEIVE");
                comiCtx.put("bsrNoun", "DELIVERY");
                comiCtx.put("bsrRevision", "001");
                comiCtx.put("returnId", returnId);
                comiCtx.put("orderId", orderId);
                comiCtx.put("authId", authId);
                comiCtx.put("sentDate", nowTimestamp);
                comiCtx.put("processingStatusId", "OAGMP_TRIGGERED");
                comiCtx.put("userLogin", userLogin);
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                return ServiceUtil.returnError("No valid Order with [" + orderId + "] found, cannot process Return");
            }

            String partyId = returnHeader.getString("fromPartyId");
            GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", returnHeader.getString("originContactMechId")));
            bodyParameters.put("postalAddress", postalAddress);
            bodyParameters.put("partyNameView", delegator.findByPrimaryKey("PartyNameView", UtilMisc.toMap("partyId", partyId)));
 
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
                            // just logging this as info because the product may not be serialized or have serialized inventory
                            Debug.logInfo("Number of serial numbers [" + itemIssuanceAndInventoryItemList.size() + "] did not match quantity [" + itemQty + "] for return item: " + returnItem.getPrimaryKey() + "; may not be a serialized inventory product", module);
                        }
                    } else {
                        // TODO: we don't know which serial numbers are returned, should we throw an error? probably not, just do what we can
                        Debug.logWarning("Could not get matching serial numbers because order item quantity [" + orderItem.getDouble("quantity") + "] did not match quantity [" + itemQty + "] for return item: " + returnItem.getPrimaryKey(), module);
                    }
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
            Writer writer = new StringWriter();
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, htmlScreenRenderer);
            screens.render(bodyScreenUri);
            writer.close();
            String outText = writer.toString();

            try {
                Map uomiCtx = FastMap.newInstance();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_OGEN_SUCCESS");
                uomiCtx.put("userLogin", userLogin);
                if (OagisServices.debugSaveXmlOut) {
                    uomiCtx.put("fullMessageXml", outText);
                }
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }

            Map sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename);
            if (sendMessageReturn != null && ServiceUtil.isError(sendMessageReturn)) {
                try {
                    Map uomiCtx = FastMap.newInstance();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SEND_ERROR");
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                    Debug.logError(e, errMsg, module);
                }
                return sendMessageReturn;
            }

            try {
                Map uomiCtx = FastMap.newInstance();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_SENT");
                uomiCtx.put("userLogin", userLogin);
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
        } catch (Throwable t) {
            String errMsg = "System Error doing Receive Delivery message for returnId [" + returnId + "] orderId [" + orderId + "] message [" + omiPkMap + "]: " + t.toString();
            Debug.logError(t, errMsg, module);
 
            // if we have a referenceId and the omiPkMap not null, save the error status
            if (omiPkMap != null) {
                try {
                    // only do this if there is a record already in place
                    if (delegator.findByPrimaryKey("OagisMessageInfo", omiPkMap) == null) {
                        return ServiceUtil.returnError(errMsg);
                    }
 
                    Map uomiCtx = FastMap.newInstance();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    uomiCtx.put("bsrVerb", "RECEIVE");
                    uomiCtx.put("bsrNoun", "DELIVERY");
                    uomiCtx.put("returnId", returnId);
                    uomiCtx.put("orderId", orderId);
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);

                    List errorMapList = UtilMisc.toList(UtilMisc.toMap("description", errMsg, "reasonCode", "SystemError"));
                    Map saveErrorMapListCtx = FastMap.newInstance();
                    saveErrorMapListCtx.putAll(omiPkMap);
                    saveErrorMapListCtx.put("errorMapList", errorMapList);
                    saveErrorMapListCtx.put("userLogin", userLogin);
                    dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
                } catch (GeneralException e) {
                    String errMsg2 = "Error saving message error info: " + e.toString();
                    Debug.logError(e, errMsg2, module);
                }
            }
 
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }
}
