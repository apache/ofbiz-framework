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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class OagisShipmentServices {

    public static final String module = OagisShipmentServices.class.getName();

    public static final Set<String> invalidShipmentStatusSet = UtilMisc.toSet("SHIPMENT_CANCELLED", "SHIPMENT_PICKED", "SHIPMENT_PACKED",
            "SHIPMENT_SHIPPED", "SHIPMENT_DELIVERED");

    public static final String resource = "OagisUiLabels";

    public static final String oagisMainNamespacePrefix = "n";
    public static final String oagisSegmentsNamespacePrefix = "os";
    public static final String oagisFieldsNamespacePrefix = "of";

    public static Map<String, Object> oagisReceiveShowShipment(DispatchContext ctx, Map<String, Object> context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
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
        Map<String, Object> oagisMsgInfoCtx = UtilMisc.toMap("bsrVerb", (Object) bsrVerb, "bsrNoun", bsrNoun, "bsrRevision", bsrRevision);

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

        Map<String, String> omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

        // always log this to make messages easier to find
        Debug.logInfo("Processing oagisReceiveShowShipment for shipmentId [" + shipmentId + "] message ID [" + omiPkMap + "]", module);

        // before getting into this check to see if we've tried once and had an error, if so set isErrorRetry even if it wasn't passed in
        GenericValue previousOagisMessageInfo = null;
        try {
            previousOagisMessageInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(omiPkMap).queryOne();
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
                Debug.logError("Message received for shipmentId [" + shipmentId + "] message ID [" + omiPkMap + "] was already partially processed but is not in a system error state, needs manual review; message ID: " + omiPkMap, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorMessageAlreadyProcessed", UtilMisc.toMap("shipmentId", shipmentId, "omiPkMap", omiPkMap), locale));
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
                // errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "CreateOagisMessageInfoServiceError"));
                Debug.logError(errMsg, module);
            }
            */
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
            Debug.logError(e, errMsg, module);
        }

        GenericValue shipment = null;
        try {
            shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error getting Shipment from database for ID [" + shipmentId + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericEntityException"));
        }

        if (shipment == null) {
            String errMsg = "Could not find Shipment ID [" + shipmentId + "]";
            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "ShipmentIdNotValid"));
        } else {
            if (invalidShipmentStatusSet.contains(shipment.get("statusId"))) {
                String errMsg = "Shipment with ID [" + shipmentId + "] is in a status [" + shipment.get("statusId") + "] that means it has been or is being shipped, so this Show Shipment message may be a duplicate.";
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "ShipmentInBadStatus"));
            }
        }

        List<? extends Element> shipUnitElementList = UtilXml.childElementList(daShowShipmentElement, "ns:SHIPUNIT"); // n
        if (errorMapList.size() == 0 && UtilValidate.isNotEmpty(shipUnitElementList)) {
            try {
                String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");
                String originFacilityId = shipment.getString("originFacilityId");

                Element shipUnitFirstElement = shipUnitElementList.get(0);
                String trackingNum = UtilXml.childElementValue(shipUnitFirstElement, "of:TRACKINGID"); // of
                String carrierCode = UtilXml.childElementValue(shipUnitFirstElement, "of:CARRIER"); // of
                if (UtilValidate.isNotEmpty(carrierCode)) {
                    String carrierPartyId = null;
                    if (carrierCode.startsWith("F") || carrierCode.startsWith("f")) {
                        carrierPartyId = "FEDEX";
                    } else if (carrierCode.startsWith("U")|| carrierCode.startsWith("u")) {
                        carrierPartyId = "UPS";
                    }
                    Map<String, Object> resultMap = dispatcher.runSync("updateShipmentRouteSegment", UtilMisc.<String, Object>toMap("shipmentId", shipmentId,
                            "shipmentRouteSegmentId", "00001", "carrierPartyId", carrierPartyId, "trackingIdNumber", trackingNum, "userLogin", userLogin));
                    if (ServiceUtil.isError(resultMap)) {
                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "updateShipmentRouteSegmentError"));
                        Debug.logError(errMsg, module);
                    }
                }

                // TODO: looks like we may have multiple shipunits for a single productId, AND with things split into
                //multiple package we may need to sort by the packages by productId to avoid deadlocks, just like we
                //do with INVITEMs... note sure exactly how that will work

                for (Element shipUnitElement : shipUnitElementList) {
                    String shipmentPackageSeqId = UtilXml.childElementValue(shipUnitElement, "of:SHPUNITSEQ"); // of
                    List<? extends Element> invItemElementList = UtilXml.childElementList(shipUnitElement, "ns:INVITEM"); //n
                    if (UtilValidate.isNotEmpty(invItemElementList)) {

                        // sort the INVITEM elements by ITEM so that all shipments are processed in the same order, avoids deadlocking problems we've seen with concurrently processed orders
                        List<Map<String, Object>> invitemMapList = new LinkedList<Map<String,Object>>();
                        boolean foundBadProductId = false;
                        for (Element invItemElement : invItemElementList) {
                            String productId = UtilXml.childElementValue(invItemElement, "of:ITEM"); // of

                            // make sure productId is valid
                            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                            if (product == null) {
                                String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                                errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                                Debug.logError(errMsg, module);
                                foundBadProductId = true;
                                continue;
                            }

                            Map<String, Object> invitemMap = new HashMap<String, Object>();
                            invitemMap.put("productId", productId);
                            // support multiple INVITEM elements for a given productId
                            UtilMisc.addToListInMap(invItemElement, invitemMap, "invItemElementList");
                            invitemMapList.add(invitemMap);
                        }

                        if (foundBadProductId) {
                            continue;
                        }

                        invitemMapList = UtilGenerics.cast(
                                UtilMisc.sortMaps(UtilGenerics.<List<Map<Object, Object>>>cast(invitemMapList), UtilMisc.toList("productId")));

                        for (Map<String, Object> invitemMap : invitemMapList) {
                            List<Element> localInvItemElementList = UtilGenerics.checkList(invitemMap.get("invItemElementList"), Element.class);

                            for (Element invItemElement : localInvItemElementList) {
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
                                List<GenericValue> shipmentItemList = null;

                                // try getting it by the unit number, which is bogus but can be what some try IFF there is only one INVITEM in the SHPUNIT
                                if (invitemMapList.size() == 1 && localInvItemElementList.size() == 1 && UtilValidate.isNotEmpty(possibleShipmentItemSeqId)) {
                                    GenericValue shipmentItem = EntityQuery.use(delegator).from("ShipmentItem").where("shipmentId", shipmentId, "shipmentItemSeqId", possibleShipmentItemSeqId).queryOne();
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
                                    shipmentItemList = EntityQuery.use(delegator).from("ShipmentItem").where("shipmentId", shipmentId, "productId",productId).queryList();
                                    if (UtilValidate.isEmpty(shipmentItemList)) {
                                        String errMsg = "Could not find Shipment Item for Shipment with ID [" + shipmentId + "] and Product with ID [" + productId + "].";
                                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ShipmentItemForProductNotFound", "description", errMsg));
                                        Debug.logError(errMsg, module);
                                        continue;
                                    }

                                    // try to isolate it to one item, ie find the first in the list that matches the quantity
                                    //AND that has not already been used/issued
                                    for (GenericValue shipmentItem : shipmentItemList) {
                                        if (messageQuantity.intValue() == shipmentItem.getDouble("quantity").intValue()) {
                                            // see if there is an ItemIssuance for this ShipmentItem, ie has already had inventory issued to it
                                            //if so then move on, this isn't the ShipmentItem you want
                                            List<GenericValue> itemIssuanceList = EntityQuery.use(delegator).from("ItemIssuance").where("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItem.get("shipmentItemSeqId")).queryList();
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
                                    errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "SingleShipmentItemForProductNotFound", "description", errMsg));
                                    Debug.logError(errMsg, module);
                                    continue;
                                }
                                GenericValue shipmentItem = shipmentItemList.get(0);

                                String shipmentItemSeqId = shipmentItem.getString("shipmentItemSeqId");
                                GenericValue orderShipment = EntityQuery.use(delegator).from("OrderShipment").where("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId).queryFirst();
                                if (orderShipment == null) {
                                    String errMsg = "Could not find Order-Shipment record for ShipmentItem with ID [" + shipmentId + "] and Item Seq-ID [" + shipmentItemSeqId + "].";
                                    errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "OrderShipmentNotFound", "description", errMsg));
                                    Debug.logError(errMsg, module);
                                    continue;
                                }

                                String orderId = orderShipment.getString("orderId");
                                String orderItemSeqId = orderShipment.getString("orderItemSeqId");
                                String requireInventory = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne().getString("requireInventory");
                                if (requireInventory == null) {
                                    requireInventory = "N";
                                }

                                // NOTE: there could be more than one reservation record for a given shipment item? for example if there wasn't enough quantity in one inventory item and reservations on two were needed
                                List<GenericValue> orderItemShipGrpInvReservationList = EntityQuery.use(delegator).from("OrderItemShipGrpInvRes").where("orderId", orderId, "orderItemSeqId", orderItemSeqId,"shipGroupSeqId",shipGroupSeqId).queryList();

                                // find the total quantity for all reservations
                                int totalReserved = 0;
                                for (GenericValue orderItemShipGrpInvReservation : orderItemShipGrpInvReservationList) {
                                    if (orderItemShipGrpInvReservation.getDouble("quantity") != null) {
                                        totalReserved += orderItemShipGrpInvReservation.getDouble("quantity").doubleValue();
                                    }
                                }

                                List<String> serialNumberList = new LinkedList<String>();
                                List<? extends Element> invDetailElementList = UtilXml.childElementList(invItemElement, "ns:INVDETAIL"); //n
                                for (Element invDetailElement : invDetailElementList) {
                                    String serialNumber = UtilXml.childElementValue(invDetailElement, "of:SERIALNUM"); // os
                                    if (UtilValidate.isNotEmpty(serialNumber)) {
                                        serialNumberList.add(serialNumber);
                                    }
                                }

                                // do some validations
                                if (UtilValidate.isNotEmpty(serialNumberList)) {
                                    if (messageQuantity.intValue() != serialNumberList.size()) {
                                        String errMsg = "Error: the quantity in the message [" + messageQuantity.intValue() + "] did not match the number of serial numbers passed [" + serialNumberList.size() + "] for ShipmentItem with ID [" + shipmentId + "] and Item Seq-ID [" + shipmentItemSeqId + "].";
                                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "QuantitySerialMismatch", "description", errMsg));
                                        Debug.logInfo(errMsg, module);
                                        continue;
                                    }
                                }

                                // because there may be more than one ShipmentItem for an OrderItem allow there to be more inventory reservations for the
                                //OrderItem than there is quantity on the current ShipmentItem
                                if (totalReserved < messageQuantity.intValue()) {
                                    String errMsg = "Inventory reservation quantity [" + totalReserved + "] was less than the message quantity [" + messageQuantity.intValue() + "] so cannot receive against reservations for ShipmentItem with ID [" + shipmentId + ":" + shipmentItemSeqId + "], and OrderItem [" + orderShipment.getString("orderId") + ":" + orderShipment.getString("orderItemSeqId") + "]";
                                    errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumbersMissing"));
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


                                for (GenericValue orderItemShipGrpInvReservation : orderItemShipGrpInvReservationList) {
                                    if (quantityLeft <= 0) {
                                        break;
                                    }
                                    int currentInvResQuantity = orderItemShipGrpInvReservation.getDouble("quantity").intValue();

                                    int quantityToUse;
                                    if (quantityLeft > currentInvResQuantity) {
                                        quantityToUse = currentInvResQuantity;
                                        quantityLeft -= currentInvResQuantity;
                                    } else {
                                        quantityToUse = quantityLeft;
                                        quantityLeft = 0;
                                    }

                                    Map<String, Object> isitspastCtx = UtilMisc.toMap("orderId", (Object) orderId, "shipGroupSeqId", shipGroupSeqId,
                                            "orderItemSeqId", orderItemSeqId);
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
                                            String serialNumber = serialNumberList.get(i);

                                            if (OagisServices.requireSerialNumberExist != null) {
                                                // according to requireSerialNumberExist make sure serialNumber does or does not exist in database, add an error message as needed
                                                Set<String> productIdSet = ProductWorker.getRefurbishedProductIdSet(productId, delegator);
                                                productIdSet.add(productId);

                                                List<GenericValue> inventoryItemsBySerialNumber = EntityQuery.use(delegator).from("InventoryItem").where(EntityCondition.makeCondition(EntityCondition.makeCondition("serialNumber", EntityOperator.EQUALS, serialNumber),
                                                        EntityOperator.AND, EntityCondition.makeCondition("productId", EntityOperator.IN, productIdSet))).queryList();
                                                if (OagisServices.requireSerialNumberExist.booleanValue()) {
                                                    if (inventoryItemsBySerialNumber.size() == 0) {
                                                        String errMsg = "Referenced serial numbers must already exist, but serial number [" + serialNumber + "] was not found. Product ID(s) considered are: " + productIdSet;
                                                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                                        continue;
                                                    }
                                                } else {
                                                    if (inventoryItemsBySerialNumber.size() > 0) {
                                                        String errMsg = "Referenced serial numbers must NOT already exist, but serial number [" + serialNumber + "] already exists. Product ID(s) considered are: " + productIdSet;
                                                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredNotExistButFound"));
                                                        continue;
                                                    }
                                                }
                                            }

                                            isitspastCtx.put("serialNumber", serialNumber);
                                            isitspastCtx.put("quantity", new Double (1));
                                            isitspastCtx.put("inventoryItemId", orderItemShipGrpInvReservation.get("inventoryItemId"));
                                            isitspastCtx.remove("itemIssuanceId");
                                            Map<String, Object> resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                            if (ServiceUtil.isError(resultMap)) {
                                                String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
                                                Debug.logError(errMsg, module);
                                            }
                                        }
                                    } else {
                                        isitspastCtx.put("quantity", new Double(quantityToUse));
                                        // NOTE: this same service is called for non-serialized inventory in spite of the name it is made to handle it
                                        Map<String, Object> resultMap = dispatcher.runSync("issueSerializedInvToShipmentPackageAndSetTracking", isitspastCtx);
                                        if (ServiceUtil.isError(resultMap)) {
                                            String errMsg = ServiceUtil.getErrorMessage(resultMap);
                                            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "IssueSerializedInvServiceError"));
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

                    List<GenericValue> shipmentItemList = EntityQuery.use(delegator).from("ShipmentItem").where("shipmentId", shipmentId).queryList();
                    for (GenericValue shipmentItem : shipmentItemList) {
                        int shipmentItemQuantity = shipmentItem.getDouble("quantity").intValue();

                        int totalItemIssuanceQuantity = 0;
                        List<GenericValue> itemIssuanceList = EntityQuery.use(delegator).from("ItemIssuance").where("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItem.get("shipmentItemSeqId")).queryList();
                        for (GenericValue itemIssuance : itemIssuanceList) {
                            totalItemIssuanceQuantity += itemIssuance.getDouble("quantity").intValue();
                        }

                        if (shipmentItemQuantity > totalItemIssuanceQuantity) {
                            String errMsg = "ShipmentItem [" + shipmentId + ":" + shipmentItem.get("shipmentItemSeqId") + "] was not completely fulfilled; shipment item quantity was [" + shipmentItemQuantity + "], but total fulfilled is only [" + totalItemIssuanceQuantity + "]";
                            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "ShipmentItemNotCompletelyFulfilled"));
                            Debug.logError(errMsg, module);
                        }
                    }
                }

                if (errorMapList.size() == 0) {
                    Map<String, Object> resultMap = dispatcher.runSync("setShipmentStatusPackedAndShipped",
                            UtilMisc.toMap("shipmentId", shipmentId, "userLogin", userLogin));
                    if (ServiceUtil.isError(resultMap)) {
                        String errMsg = ServiceUtil.getErrorMessage(resultMap);
                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SetShipmentStatusPackedAndShippedError"));
                        Debug.logError(errMsg, module);
                    }
                }
            } catch (Throwable t) {
                String errMsg = UtilProperties.getMessage(resource, "OagisErrorMessageShowShipment", UtilMisc.toMap("shipmentId", shipmentId, "omiPkMap", omiPkMap), locale);
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));

                try {
                    oagisMsgInfoCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    dispatcher.runSync("updateOagisMessageInfo", oagisMsgInfoCtx, 60, true);

                    Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
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
                return ServiceUtil.returnError(errMsg + t.toString());
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
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
                Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
                saveErrorMapListCtx.putAll(omiPkMap);
                saveErrorMapListCtx.put("errorMapList", errorMapList);
                saveErrorMapListCtx.put("userLogin", userLogin);
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
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
            // return success here so that the message won't be retried and the Confirm BOD, etc won't be sent multiple times
            String errMsg = UtilProperties.getMessage(resource, "OagisErrorBusinessLevel", UtilMisc.toMap("errorString", ""), locale) + errorMapList.get(0).toString();
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

        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        return result;
    }

    public static Map<String, Object> oagisSendProcessShipmentsFromBackOrderSet(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Set<String> noLongerOnBackOrderIdSet = UtilGenerics.checkSet(context.get("noLongerOnBackOrderIdSet"), String.class);
        Debug.logInfo("Running oagisSendProcessShipmentsFromBackOrderSet with noLongerOnBackOrderIdSet=" + noLongerOnBackOrderIdSet, module);
        if (UtilValidate.isEmpty(noLongerOnBackOrderIdSet)) {
            return ServiceUtil.returnSuccess();
        }

        try {
            for (String orderId : noLongerOnBackOrderIdSet) {
                dispatcher.runAsync("oagisSendProcessShipment", UtilMisc.toMap("orderId", orderId), true);
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorOagisSendProcessShipment", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> oagisSendProcessShipment(DispatchContext ctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        // Check if order is not on back order before processing shipment
        try {
            Map<String, Object> checkOrderResp = dispatcher.runSync("checkOrderIsOnBackOrder", UtilMisc.toMap("orderId", orderId));
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
            sendToUrl = EntityUtilProperties.getPropertyValue("oagis", "url.send.processShipment", delegator);
        }

        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.filename.base", "", delegator);
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ProcessShipment" + orderId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.directory", delegator);
        }

        OutputStream out = (OutputStream) context.get("outputStream");

        if (Debug.infoOn()) Debug.logInfo("Call to oagisSendProcessShipment for orderId [" + orderId + "], sendToUrl=[" + sendToUrl + "], saveToDirectory=[" + saveToDirectory + "], saveToFilename=[" + saveToFilename + "]", module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        MapStack<String> bodyParameters =  MapStack.create();
        bodyParameters.put("orderId", orderId);

        // the userLogin passed in will usually be the customer, so don't use it; use the system user instead
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting userLogin", module);
        }

        GenericValue orderHeader = null;
        GenericValue orderItemShipGroup = null;

        String logicalId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.LOGICALID", delegator);
        String referenceId = null;
        String task = "SHIPREQUEST"; // Actual value of task is "SHIPREQUEST" which is more than 10 char, need this in the db so it will match Confirm BODs, etc
        String component = "INVENTORY";
        Map<String, String> omiPkMap = null;

        String shipmentId = null;

        try {
            // see if there are any OagisMessageInfo for this order that are in the OAGMP_OGEN_SUCCESS or OAGMP_SENT statuses, if so don't send again; these need to be manually reviewed before resending to avoid accidental duplicate messages
            List<GenericValue> previousOagisMessageInfoList = EntityQuery.use(delegator).from("OagisMessageInfo").where("orderId", orderId, "task", task, "component", component).queryList();
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisFoundExistingMessage", UtilMisc.toMap("orderId", orderId), locale) + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")));
            }
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisFoundExistingMessageSent", UtilMisc.toMap("orderId", orderId), locale) + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")));
            }

            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdNotFound", UtilMisc.toMap("orderId", orderId), locale));
            }

            List<String> validStores = StringUtil.split(EntityUtilProperties.getPropertyValue("oagis", "Oagis.Order.ValidProductStores", delegator), ",");
            if (UtilValidate.isNotEmpty(validStores)) {
                if (!validStores.contains(orderHeader.getString("productStoreId"))) {
                    return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisOrderIdNotValidStore", UtilMisc.toMap("orderId", orderId), locale));
                }
            }
            String orderStatusId = orderHeader.getString("statusId");
            if (!"ORDER_APPROVED".equals(orderStatusId)) {
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisOrderIdNotInApprovedStatus", UtilMisc.toMap("orderId", orderId, "orderStatusId", orderStatusId), locale));
            }
            if (!"SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdNotASalesOrder", UtilMisc.toMap("orderId", orderId), locale));
            }

            // first check some things...
            OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
            // before doing or saving anything see if any OrderItems are Products with isPhysical=Y
            if (!orderReadHelper.hasPhysicalProductItems()) {
                // no need to process shipment, return success
                return ServiceUtil.returnSuccess();
            }
            if (!orderReadHelper.hasShippingAddress()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdWithoutShippingAddress", UtilMisc.toMap("orderId", orderId), locale));
            }

            // check payment authorization
            Map<String, Object> authServiceContext = new HashMap<String, Object>();
            authServiceContext.put("orderId", orderId);
            authServiceContext.put("userLogin", userLogin);
            authServiceContext.put("reAuth", true);
            Map<String, Object> authResult = dispatcher.runSync("authOrderPayments", authServiceContext);
            if (!authResult.get("processResult").equals("APPROVED")) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdPaymentNotAuthorized", locale));
            }

            referenceId = delegator.getNextSeqId("OagisMessageInfo");
            omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

            String authId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.AUTHID", delegator);
            Timestamp timestamp = UtilDateTime.nowTimestamp();
            String sentDate = OagisServices.isoDateFormat.format(timestamp);

            bodyParameters.putAll(omiPkMap);
            bodyParameters.put("authId", authId);
            bodyParameters.put("sentDate", sentDate);

            // prepare map to Create Oagis Message Info
            try {
                Map<String, Object> comiCtx = new HashMap<String, Object>();
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
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where(findShipmentCondition).queryFirst();

            if (shipment != null) {
                // if picked, packed, shipped, delivered then complain, no reason to process the shipment!
                String statusId = shipment.getString("statusId");
                if ("SHIPMENT_PICKED".equals(statusId) || "SHIPMENT_PACKED".equals(statusId) || "SHIPMENT_SHIPPED".equals(statusId) || "SHIPMENT_DELIVERED".equals(statusId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdWithShipment", UtilMisc.toMap("statusId", statusId), locale));
                }
                shipmentId = shipment.getString("shipmentId");
            } else {
                Map<String, Object> cospResult= dispatcher.runSync("createOrderShipmentPlan", UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin));
                shipmentId = (String) cospResult.get("shipmentId");
                shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            }

            bodyParameters.put("shipment", shipment);
            List<GenericValue> shipmentItems = EntityQuery.use(delegator).from("ShipmentItem").where("shipmentId", shipmentId).queryList();
            bodyParameters.put("shipmentItems", shipmentItems);

            GenericValue  address = EntityUtil.getFirst(orderReadHelper.getShippingLocations());
            bodyParameters.put("address", address);
            String emailString = orderReadHelper.getOrderEmailString();
            bodyParameters.put("emailString", emailString);
            String contactMechId = shipment.getString("destinationTelecomNumberId");

            GenericValue telecomNumber = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", contactMechId).queryOne();
            if (telecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisOrderIdNotTelecomNumberFound", UtilMisc.toMap("orderId", orderId), locale));
            }
            bodyParameters.put("telecomNumber", telecomNumber);

            orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId).queryFirst();
            bodyParameters.put("orderItemShipGroup", orderItemShipGroup);
            Set<String> correspondingPoIdSet = new HashSet<String>();

            List<GenericValue> orderItems = orderReadHelper.getOrderItems();
            for (GenericValue orderItem : orderItems) {
                String correspondingPoId = orderItem.getString("correspondingPoId");
                if (correspondingPoId != null) {
                    correspondingPoIdSet.add(correspondingPoId);
                }
            }
            bodyParameters.put("correspondingPoIdSet", correspondingPoIdSet);
            if (orderHeader.get("externalId") != null) {
                Set<String> externalIdSet = new HashSet<String>();
                externalIdSet.add(orderHeader.getString("externalId"));
                bodyParameters.put("externalIdSet", externalIdSet);
            }
            // Check if order was a return replacement order (associated with return)
            GenericValue returnItemResponse = EntityQuery.use(delegator).from("ReturnItemResponse").where("replacementOrderId", orderId).queryFirst();
            if (returnItemResponse != null) {
                boolean includeReturnLabel = false;

                // Get the associated return Id (replaceReturnId)
                String returnItemResponseId = returnItemResponse.getString("returnItemResponseId");
                List<GenericValue> returnItemList = EntityQuery.use(delegator).from("ReturnItem").where("returnItemResponseId", returnItemResponseId).queryList();
                GenericValue firstReturnItem = EntityUtil.getFirst(returnItemList);
                if (firstReturnItem != null) {
                    bodyParameters.put("replacementReturnId", firstReturnItem.getString("returnId"));
                } else {
                    Debug.logWarning("Could not find a ReturnItem for returnItemResponseId [" + returnItemResponseId + "]; this really shouldn't happen but isn't a real error either. It means a ReturnItemResponse was created but not attached to any item!", module);
                }

                // return label should only be sent when we want a return label to be included; this would be for a cross-ship replacement type ReturnItem

                // go through the returnItemList and if any are cross-ship replacement, then include a label (not for wait replacement in other words)
                for (GenericValue returnItem : returnItemList) {
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
            bodyParameters.put("partyNameView", EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyId).queryOne());
            List<GenericValue> partyCarrierAccounts = EntityQuery.use(delegator).from("PartyCarrierAccount").where("partyId", partyId).filterByDate().queryList();
            if (partyCarrierAccounts != null) {
                for (GenericValue partyCarrierAccount : partyCarrierAccounts) {
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

            String bodyScreenUri = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Template.ProcessShipment", delegator);
            String outText = null;
            Writer writer = new StringWriter();
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", "screen.name", delegator),
                    EntityUtilProperties.getPropertyValue("widget", "screen.screenrenderer", delegator));
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, screenStringRenderer);
            screens.render(bodyScreenUri);
            writer.close();
            outText = writer.toString();
            if (Debug.infoOn()) Debug.logInfo("Finished rendering oagisSendProcessShipment message for orderId [" + orderId + "]", module);

            try {
                Map<String, Object> uomiCtx = new HashMap<String, Object>();
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

            Map<String, Object> sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename, locale, delegator);
            if (sendMessageReturn != null && ServiceUtil.isError(sendMessageReturn)) {
                try {
                    Map<String, Object> uomiCtx = new HashMap<String, Object>();
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
                Map<String, Object> uomiCtx = new HashMap<String, Object>();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_SENT");
                uomiCtx.put("userLogin", userLogin);
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
        } catch (Throwable t) {
            String errMsg = UtilProperties.getMessage(resource, "OagisErrorProcessShipment", UtilMisc.toMap("orderId", orderId, "shipmentId", shipmentId, "omiPkMap", omiPkMap), locale) + t.toString();
            Debug.logError(t, errMsg, module);

            // if we have a referenceId and the omiPkMap not null, save the error status
            if (omiPkMap != null) {
                try {
                    // only do this if there is a record already in place
                    if (EntityQuery.use(delegator).from("OagisMessageInfo").where(omiPkMap).queryOne() == null) {
                        return ServiceUtil.returnError(errMsg);
                    }

                    Map<String, Object> uomiCtx = new HashMap<String, Object>();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    uomiCtx.put("bsrVerb", "PROCESS");
                    uomiCtx.put("bsrNoun", "SHIPMENT");
                    uomiCtx.put("orderId", orderId);
                    uomiCtx.put("shipmentId", shipmentId);
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);

                    List<Map<String, String>> errorMapList = UtilMisc.toList(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));
                    Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
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

    public static Map<String, Object> oagisSendReceiveDelivery(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");
        String sendToUrl = (String) context.get("sendToUrl");
        if (UtilValidate.isEmpty(sendToUrl)) {
            sendToUrl = EntityUtilProperties.getPropertyValue("oagis", "url.send.receiveDelivery", delegator);
        }

        String saveToFilename = (String) context.get("saveToFilename");
        if (UtilValidate.isEmpty(saveToFilename)) {
            String saveToFilenameBase = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.filename.base", "", delegator);
            if (UtilValidate.isNotEmpty(saveToFilenameBase)) {
                saveToFilename = saveToFilenameBase + "ReceiveDelivery" + returnId + ".xml";
            }
        }
        String saveToDirectory = (String) context.get("saveToDirectory");
        if (UtilValidate.isEmpty(saveToDirectory)) {
            saveToDirectory = EntityUtilProperties.getPropertyValue("oagis", "test.save.outgoing.directory", delegator);
        }

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting system userLogin", module);
        }

        OutputStream out = (OutputStream) context.get("outputStream");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        MapStack<String> bodyParameters =  MapStack.create();

        String orderId = null;

        String referenceId = null;
        String task = "RMA"; // Actual value of task is "SHIPREQUEST" which is more than 10 char, need this in the db so it will match Confirm BODs, etc
        String component = "INVENTORY";
        Map<String, String> omiPkMap = null;

        try {
            // see if there are any OagisMessageInfo for this order that are in the OAGMP_OGEN_SUCCESS or OAGMP_SENT statuses, if so don't send again; these need to be manually reviewed before resending to avoid accidental duplicate messages
            List<GenericValue> previousOagisMessageInfoList = EntityQuery.use(delegator).from("OagisMessageInfo").where("returnId", returnId, "task", task, "component", component).queryList();
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisFoundExistingMessageForReturn", UtilMisc.toMap("returnId", returnId), locale) + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_OGEN_SUCCESS")));
            }
            if (EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")).size() > 0) {
                // this isn't really an error, just a failed constraint so return success
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisFoundExistingMessageForReturnSent", UtilMisc.toMap("returnId", returnId), locale) + EntityUtil.filterByAnd(previousOagisMessageInfoList, UtilMisc.toMap("processingStatusId", "OAGMP_SENT")));
            }

            GenericValue returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisReturnIdNotFound", UtilMisc.toMap("returnId", returnId), locale));
            }
            String statusId = returnHeader.getString("statusId");
            if (!"RETURN_ACCEPTED".equals(statusId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisReturnIdNotInAcceptedStatus", UtilMisc.toMap("returnId", returnId), locale));
            }

            List<GenericValue> returnItems = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId).queryList();
            bodyParameters.put("returnItems", returnItems);

            orderId = EntityUtil.getFirst(returnItems).getString("orderId");

            String logicalId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.LOGICALID", delegator);
            String authId = EntityUtilProperties.getPropertyValue("oagis", "CNTROLAREA.SENDER.AUTHID", delegator);

            referenceId = delegator.getNextSeqId("OagisMessageInfo");
            omiPkMap = UtilMisc.toMap("logicalId", logicalId, "component", component, "task", task, "referenceId", referenceId);

            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            String sentDate = OagisServices.isoDateFormat.format(nowTimestamp);

            bodyParameters.putAll(omiPkMap);
            bodyParameters.put("authId", authId);
            bodyParameters.put("sentDate", sentDate);

            // prepare map to Create Oagis Message Info
            try {
                Map<String, Object> comiCtx = new HashMap<String, Object>();
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

            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisReturnIdNotValid", UtilMisc.toMap("orderId", orderId), locale));
            }

            String partyId = returnHeader.getString("fromPartyId");
            GenericValue postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", returnHeader.getString("originContactMechId")).queryOne();
            bodyParameters.put("postalAddress", postalAddress);
            bodyParameters.put("partyNameView", EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyId).queryOne());

            // calculate total qty of return items in a shipping unit received, order associated with return
            double totalQty = 0.0;
            Map<String, List<String>> serialNumberListByReturnItemSeqIdMap = new HashMap<String, List<String>>();
            bodyParameters.put("serialNumberListByReturnItemSeqIdMap", serialNumberListByReturnItemSeqIdMap);
            for (GenericValue returnItem : returnItems) {
                double itemQty = returnItem.getDouble("returnQuantity").doubleValue();
                totalQty += itemQty;

                // for each ReturnItem also get serial numbers using ItemIssuanceAndInventoryItem
                // NOTE: technically if the ReturnItem.quantity != OrderItem.quantity then we don't know which serial number is being returned, so rather than guessing we will send it only in that case
                GenericValue orderItem = returnItem.getRelatedOne("OrderItem", false);
                if (orderItem != null) {
                    if (orderItem.getDouble("quantity").doubleValue() == itemQty) {
                        List<GenericValue> itemIssuanceAndInventoryItemList = EntityQuery.use(delegator).from("ItemIssuanceAndInventoryItem")
                                .where("orderId", orderItem.get("orderId"), "orderItemSeqId", orderItem.get("orderItemSeqId"), "inventoryItemTypeId", "SERIALIZED_INV_ITEM")
                                .queryList();
                        if (itemIssuanceAndInventoryItemList.size() == itemQty) {
                            List<String> serialNumberList = new LinkedList<String>();
                            serialNumberListByReturnItemSeqIdMap.put(returnItem.getString("returnItemSeqId"), serialNumberList);
                            for (GenericValue itemIssuanceAndInventoryItem : itemIssuanceAndInventoryItemList) {
                                serialNumberList.add(itemIssuanceAndInventoryItem.getString("serialNumber"));
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

            String bodyScreenUri = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Template.ReceiveDelivery", delegator);
            Writer writer = new StringWriter();
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", "screen.name", delegator),
                    EntityUtilProperties.getPropertyValue("widget", "screen.screenrenderer", delegator));
            ScreenRenderer screens = new ScreenRenderer(writer, bodyParameters, screenStringRenderer);
            screens.render(bodyScreenUri);
            writer.close();
            String outText = writer.toString();

            try {
                Map<String, Object> uomiCtx = new HashMap<String, Object>();
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

            Map<String, Object> sendMessageReturn = OagisServices.sendMessageText(outText, out, sendToUrl, saveToDirectory, saveToFilename, locale, delegator);
            if (sendMessageReturn != null && ServiceUtil.isError(sendMessageReturn)) {
                try {
                    Map<String, Object> uomiCtx = new HashMap<String, Object>();
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
                Map<String, Object> uomiCtx = new HashMap<String, Object>();
                uomiCtx.putAll(omiPkMap);
                uomiCtx.put("processingStatusId", "OAGMP_SENT");
                uomiCtx.put("userLogin", userLogin);
                dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "OagisErrorInCreatingDataForOagisMessageInfoEntity", (Locale) context.get("locale"));
                Debug.logError(e, errMsg, module);
            }
        } catch (Throwable t) {
            String errMsg = UtilProperties.getMessage(resource, "OagisErrorReceivingDeliveryMessageReturn", UtilMisc.toMap("returnId", returnId, "orderId", orderId, "omiPkMap", omiPkMap), locale);
            Debug.logError(t, errMsg, module);

            // if we have a referenceId and the omiPkMap not null, save the error status
            if (omiPkMap != null) {
                try {
                    // only do this if there is a record already in place
                    if (EntityQuery.use(delegator).from("OagisMessageInfo").where(omiPkMap).queryOne() == null) {
                        return ServiceUtil.returnError(errMsg);
                    }

                    Map<String, Object> uomiCtx = new HashMap<String, Object>();
                    uomiCtx.putAll(omiPkMap);
                    uomiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    uomiCtx.put("bsrVerb", "RECEIVE");
                    uomiCtx.put("bsrNoun", "DELIVERY");
                    uomiCtx.put("returnId", returnId);
                    uomiCtx.put("orderId", orderId);
                    uomiCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateOagisMessageInfo", uomiCtx, 60, true);

                    List<Map<String, String>> errorMapList = UtilMisc.toList(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));
                    Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
                    saveErrorMapListCtx.putAll(omiPkMap);
                    saveErrorMapListCtx.put("errorMapList", errorMapList);
                    saveErrorMapListCtx.put("userLogin", userLogin);
                    dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
                } catch (GeneralException e) {
                    String errMsg2 = "Error saving message error info: " + e.toString();
                    Debug.logError(e, errMsg2, module);
                }
            }
            return ServiceUtil.returnError(errMsg + t.toString());
        }
        return result;
    }
}
