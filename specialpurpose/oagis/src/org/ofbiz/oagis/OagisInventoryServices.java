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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
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
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OagisInventoryServices {

    public static final String module = OagisInventoryServices.class.getName();
    public static final String resource = "OagisUiLabels";
    public static final Double doubleZero = new Double(0.0);
    public static final Double doubleOne = new Double(1.0);
    

    public static Map<String, Object> oagisReceiveSyncInventory(DispatchContext ctx, Map<String, Object> context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();
        List<Map<String, Object>> inventoryMapList = new LinkedList<Map<String,Object>>();
        final String syncInventoryFacilityId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.SyncInventoryFacilityId", delegator);
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting UserLogin: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

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

        // create oagis message info
        Map<String, Object> comiCtx = new HashMap<String, Object>();
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
        comiCtx.put("processingStatusId", "OAGMP_RECEIVED");
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
            if (isErrorRetry) {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } else {
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        // data area elements
        List<? extends Element> dataAreaList = UtilXml.childElementList(syncInventoryRootElement, "ns:DATAAREA");
        if (UtilValidate.isNotEmpty(dataAreaList)) {
            try {
                for (Element dataAreaElement : dataAreaList) {
                    Element syncInventoryElement = UtilXml.firstChildElement(dataAreaElement, "ns:SYNC_INVENTORY");
                    Element inventoryElement = UtilXml.firstChildElement(syncInventoryElement, "ns:INVENTORY");

                    Element quantityElement = UtilXml.firstChildElement(inventoryElement, "os:QUANTITY");

                    String itemQtyStr = UtilXml.childElementValue(quantityElement, "of:VALUE");
                    double itemQty = Double.parseDouble(itemQtyStr);
                    /* TODO sign denoted whether quantity is accepted(+) or rejected(-), which plays role in receiving inventory
                     * In this message will it serve any purpose, since it is not handled.
                     */
                    
                    // TODOLATER: Not used now, Later we may need it
                    //String sign = UtilXml.childElementValue(quantityElement, "of:SIGN");
                    //String uom = UtilXml.childElementValue(quantityElement, "of:UOM");
                    String productId = UtilXml.childElementValue(inventoryElement, "of:ITEM");
                    String itemStatus = UtilXml.childElementValue(inventoryElement, "of:ITEMSTATUS");

                    // make sure productId is valid
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                        Debug.logError(errMsg, module);
                        continue;
                    }

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
                        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, snapshotDate),
                                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                EntityCondition.makeCondition("inventoryItemTypeId", EntityOperator.EQUALS, "NON_SERIAL_INV_ITEM"),
                                EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, syncInventoryFacilityId)), EntityOperator.AND);
                        List<GenericValue> invItemAndDetails = EntityQuery.use(delegator).select("quantityOnHandSum").from("InventoryItemDetailForSum").where(condition).queryList();
                        for (GenericValue inventoryItemDetailForSum : invItemAndDetails) {
                            quantityOnHandTotal += inventoryItemDetailForSum.getDouble("quantityOnHandSum").doubleValue();
                        }
                    }

                    // now regardless of AVAILABLE or NOTAVAILABLE check serialized inventory, just use the corresponding statusId as set above
                    EntityCondition serInvCondition = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, snapshotDate),
                            EntityCondition.makeCondition(EntityCondition.makeCondition("statusEndDatetime", EntityOperator.GREATER_THAN, snapshotDate), EntityOperator.OR, EntityCondition.makeCondition("statusEndDatetime", EntityOperator.EQUALS, null)),
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId),
                            EntityCondition.makeCondition("inventoryItemTypeId", EntityOperator.EQUALS, "SERIALIZED_INV_ITEM"),
                            EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, syncInventoryFacilityId)), EntityOperator.AND);
                    long invItemQuantCount = EntityQuery.use(delegator).from("InventoryItemStatusForCount").where(serInvCondition).queryCount();
                    quantityOnHandTotal += invItemQuantCount;

                    // check for mismatch in quantity
                    if (itemQty != quantityOnHandTotal) {
                        double quantityDiff = Math.abs((itemQty - quantityOnHandTotal));
                        inventoryMapList.add(UtilMisc.toMap("productId", (Object) productId, "statusId", statusId,
                                "quantityOnHandTotal", String.valueOf(quantityOnHandTotal), "quantityFromMessage", itemQtyStr,
                                "quantityDiff", String.valueOf(quantityDiff), "timestamp", snapshotDate));
                    }
                }
            } catch (Throwable t) {
                String errMsg = "Error processing Sync Inventory message: " + t.toString();
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "Exception"));
                Debug.logInfo(t, errMsg, module);
            }
        }
        // send mail if mismatch(s) found
        if (errorMapList.size() == 0 && inventoryMapList.size() > 0) {
            try {
                // prepare information to send mail
                Map<String, Object> sendMap = new HashMap<String, Object>();

                String sendToEmail = EntityUtilProperties.getPropertyValue("oagis", "oagis.notification.email.sendTo", delegator);

                /* DEJ20070802 changed to get email address from properties file, should be way easier to manage
                // get facility email address
                List facilityContactMechs = null;
                GenericValue contactMech = null;
                try {
                    facilityContactMechs = delegator.findByAnd("FacilityContactMech", UtilMisc.toMap("facilityId", facilityId), null, false);
                } catch (GenericEntityException e) {
                    String errMsg = "Error Getting FacilityContactMech: " + e.toString();
                    errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "GenericEntityException", "description", errMsg));
                    Debug.logError(e, errMsg, module);
                }

                Iterator fcmIter  = facilityContactMechs.iterator();
                while (fcmIter.hasNext()) {
                    GenericValue facilityContactMech = (GenericValue) fcmIter.next();
                    String contactMechId = facilityContactMech.getString("contactMechId");
                    try {
                        contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
                    } catch (GenericEntityException e) {
                        String errMsg = "Error Getting ContactMech: " + e.toString();
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "GenericEntityException", "description", errMsg));
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
                    String productStoreId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.SyncInventoryProductStoreId", delegator);
                    GenericValue productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", "PRDS_OAGIS_CONFIRM").queryOne();
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

                    Map<String, Object> bodyParameters = UtilMisc.toMap("inventoryMapList", inventoryMapList, "locale", locale);
                    sendMap.put("bodyParameters", bodyParameters);
                    sendMap.put("userLogin", userLogin);

                    // send the notification
                    // run async so it will happen in the background AND so errors in sending won't mess this up
                    dispatcher.runAsync("sendMailFromScreen", sendMap, true);
                } else {
                    // no send to email address, just log to file
                    Debug.logImportant("No sendTo email address found in process oagisReceiveSyncInventory service: inventoryMapList: " + inventoryMapList, module);
                }
            } catch (Throwable t) {
                Debug.logInfo(t, "System Error processing Sync Inventory message: " + t.toString(), module);
                // in this case we don't want to return a Confirm BOD, so return an error now
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorProcessingSyncInventory", UtilMisc.toMap("errorString", t.toString()), locale));
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            try {
                comiCtx.put("processingStatusId", "OAGMP_PROC_ERROR");
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
            saveErrorMapListCtx.put("logicalId", logicalId);
            saveErrorMapListCtx.put("component", component);
            saveErrorMapListCtx.put("task", task);
            saveErrorMapListCtx.put("referenceId", referenceId);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                // for sync inventory no such ID: sendConfirmBodCtx.put("origRefId", shipmentId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error sending Confirm BOD: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            // return success here so that the message won't be retried and the Confirm BOD, etc won't be sent multiple times
            result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisErrorProcessingMessage", locale)));
            return result;
        } else {
            try {
                comiCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }

        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        return result;
    }

    public static Map<String, Object> oagisReceiveAcknowledgeDeliveryPo(DispatchContext ctx, Map<String, Object> context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();
        Map<String, Object> comiCtx = new HashMap<String, Object>();

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting UserLogin: " + e.toString();
            Debug.logError(e, errMsg, module);
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
        Timestamp timestamp = UtilDateTime.nowTimestamp();

        Map<String, Object> omiPkMap = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);

        // always log this to make messages easier to find
        Debug.logInfo("Processing oagisReceiveAcknowledgeDeliveryPo for message ID [" + omiPkMap + "]", module);

        // before getting into this check to see if we've tried once and had an error, if so set isErrorRetry even if it wasn't passed in
        GenericValue previousOagisMessageInfo = null;
        try {
            previousOagisMessageInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(omiPkMap).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error getting OagisMessageInfo from database for message ID [" + omiPkMap + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            // anything else to do about this? we don't really want to send the error back or anything...
        }

        if (previousOagisMessageInfo != null && !isErrorRetry) {
            if ("OAGMP_SYS_ERROR".equals(previousOagisMessageInfo.getString("processingStatusId"))) {
                isErrorRetry = true;
            } else {
                // message already in the db, but is not in a system error state...
                Debug.logError("Message received for message ID [" + omiPkMap + "] was already partially processed but is not in a system error state, needs manual review; message ID: " + omiPkMap, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorMessageAlreadyProcessed", UtilMisc.toMap("shipmentId", "", "omiPkMap", omiPkMap), locale));
            }
        }

        comiCtx.putAll(omiPkMap);
        comiCtx.put("authId", authId);
        comiCtx.put("receivedDate", timestamp);
        comiCtx.put("sentDate", sentTimestamp);
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("processingStatusId", "OAGMP_RECEIVED");
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
            if (isErrorRetry) {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } else {
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        Element dataAreaElement = UtilXml.firstChildElement(receivePoElement, "ns:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");

        String facilityId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.PoReceiptFacilityId", delegator);
        String orderId = null;
        // get RECEIPTLN elements from message
        List<? extends Element> acknowledgeElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        if (UtilValidate.isNotEmpty(acknowledgeElementList)) {
            try {
                for (Element receiptLnElement : acknowledgeElementList) {
                    Map<String, Object> ripCtx = new HashMap<String, Object>();
                    Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "os:QUANTITY");

                    String itemQtyStr = UtilXml.childElementValue(qtyElement, "of:VALUE");
                    double itemQty = Double.parseDouble(itemQtyStr);
                    String sign = UtilXml.childElementValue(qtyElement, "of:SIGN");

                    String productId = UtilXml.childElementValue(receiptLnElement, "of:ITEM");

                    // make sure productId is valid
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                        Debug.logError(errMsg, module);
                        continue;
                    }

                    Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "os:DOCUMNTREF");
                    orderId = UtilXml.childElementValue(documentRefElement, "of:DOCUMENTID");
                    String orderTypeId = UtilXml.childElementValue(documentRefElement, "of:DOCTYPE");
                    if (orderTypeId.equals("PO")) {
                        orderTypeId = "PURCHASE_ORDER";
                    }

                    String datetimeReceived = UtilXml.childElementValue(receiptLnElement, "os:DATETIMEISO");
                    Timestamp timestampItemReceived = OagisServices.parseIsoDateString(datetimeReceived, errorMapList);
                    ripCtx.put("datetimeReceived", timestampItemReceived);
                    // Check reference to PO number, if exists
                    GenericValue orderHeader = null;
                    if (orderId != null) {
                        List<GenericValue> toStore = new LinkedList<GenericValue>();
                        orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                        if (orderHeader != null) {
                            // Case : update the record
                            ripCtx.put("orderId", orderId);
                            comiCtx.put("orderId", orderId);
                            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", orderId, "productId",productId,"quantity",new Double(itemQtyStr)));
                            delegator.setNextSubSeqId(orderItem, "orderItemSeqId", 5, 1);
                            delegator.create(orderItem);
                            ripCtx.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                        } else {
                            // Case : New record entry when PO not exists in the Database
                            orderHeader =  delegator.makeValue("OrderHeader", UtilMisc.toMap("orderId", orderId, "orderTypeId",orderTypeId ,
                                    "orderDate", timestampItemReceived, "statusId", "ORDER_CREATED", "entryDate", UtilDateTime.nowTimestamp(),
                                    "productStoreId", EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.SyncInventoryProductStoreId","9001", delegator)));
                            toStore.add(orderHeader);
                            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", orderId,
                                    "orderItemSeqId", UtilFormatOut.formatPaddedNumber(1L, 5),
                                    "productId", productId, "quantity", new Double(itemQtyStr)));
                            toStore.add(orderItem);
                            delegator.storeAll(toStore);
                        }
                    }

                    /* NOTE DEJ20070813 this is only meant to be used in the Ack Delivery RMA message, so ignoring here and always settings status to AVAILABLE
                    // get inventory item status
                    String invItemStatus = UtilXml.childElementValue(receiptLnElement, "of:DISPOSITN");
                    if (invItemStatus.equals("ReceivedTOAvailable") || invItemStatus.equals("NotAvailableTOAvailable")) {
                        ripCtx.put("statusId","INV_AVAILABLE");
                    } else if (invItemStatus.equals("ReceivedTONotAvailable") || invItemStatus.equals("AvailableTONotAvailable")) {
                        ripCtx.put("statusId","INV_ON_HOLD");
                    }
                    */

                    ripCtx.put("statusId","INV_AVAILABLE");
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
                    Map<String, Object> ripResult = dispatcher.runSync("receiveInventoryProduct", ripCtx);
                    if (ServiceUtil.isError(ripResult)) {
                        String errMsg = ServiceUtil.getErrorMessage(ripResult);
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg));
                    }
                }
            } catch (Throwable t) {
                String errMsg = UtilProperties.getMessage(resource, "OagisErrorDeliveryMessagePO", UtilMisc.toMap("omiPkMap", omiPkMap), locale) + t.toString();
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));

                try {
                    comiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);

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
                return ServiceUtil.returnError(errMsg);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            try {
                comiCtx.put("processingStatusId", "OAGMP_PROC_ERROR");
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
            saveErrorMapListCtx.put("logicalId", logicalId);
            saveErrorMapListCtx.put("component", component);
            saveErrorMapListCtx.put("task", task);
            saveErrorMapListCtx.put("referenceId", referenceId);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                sendConfirmBodCtx.put("origRefId", orderId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error sending Confirm BOD: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
            
            // return success here so that the message won't be retried and the Confirm BOD, etc won't be sent multiple times
            String errMsg = UtilProperties.getMessage(resource, "OagisErrorBusinessLevel", UtilMisc.toMap("errorString", ""), locale) + errorMapList.get(0);
            result.putAll(ServiceUtil.returnSuccess(errMsg));

            // however, we still don't want to save the partial results, so set rollbackOnly
            try {
                TransactionUtil.setRollbackOnly(errMsg, null);
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error setting rollback only ", module);
            }

            return result;
        } else {
            comiCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }

        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        return result;
    }

    public static Map<String, Object> oagisReceiveAcknowledgeDeliveryRma(DispatchContext ctx, Map<String, Object> context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error Getting UserLogin: " + e.toString(), module);
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

        // get the first returnId from the list so we at least have something in the info record
        Element firstReceiptlnElement = UtilXml.firstChildElement(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        Element firstDocRefElement = UtilXml.firstChildElement(firstReceiptlnElement, "os:DOCUMNTREF");
        String firstReturnId = UtilXml.childElementValue(firstDocRefElement, "of:DOCUMENTID");

        String facilityId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.PoReceiptFacilityId", delegator);
        String locationSeqId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.ReturnReceiptLocationSeqId", delegator);

        Timestamp timestamp = UtilDateTime.nowTimestamp();
        Map<String, Object> comiCtx = new HashMap<String, Object>();

        Map<String, Object> omiPkMap = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);

        // always log this to make messages easier to find
        Debug.logInfo("Processing oagisReceiveAcknowledgeDeliveryRma for message ID [" + omiPkMap + "]", module);

        // before getting into this check to see if we've tried once and had an error, if so set isErrorRetry even if it wasn't passed in
        GenericValue previousOagisMessageInfo = null;
        try {
            previousOagisMessageInfo = EntityQuery.use(delegator).from("OagisMessageInfo").where(omiPkMap).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error getting OagisMessageInfo from database for message ID [" + omiPkMap + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            // anything else to do about this? we don't really want to send the error back or anything...
        }

        if (previousOagisMessageInfo != null && !isErrorRetry) {
            if ("OAGMP_SYS_ERROR".equals(previousOagisMessageInfo.getString("processingStatusId"))) {
                isErrorRetry = true;
            } else {
                // message already in the db, but is not in a system error state...
                Debug.logError("Message received for message ID [" + omiPkMap + "] was already partially processed but is not in a system error state, needs manual review; message ID: " + omiPkMap, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorMessageAlreadyProcessed", UtilMisc.toMap("shipmentId", "", "omiPkMap", omiPkMap), locale));
            }
        }

        comiCtx.putAll(omiPkMap);
        comiCtx.put("authId", authId);
        comiCtx.put("receivedDate", timestamp);
        comiCtx.put("sentDate", sentTimestamp);
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("processingStatusId", "OAGMP_RECEIVED");
        comiCtx.put("returnId", firstReturnId);
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
            if (isErrorRetry) {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } else {
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        String lastReturnId = null;
        //String inventoryItemId = null;
        List<String> invItemIds = new LinkedList<String>();
        // get RECEIPTLN elements from message
        List<? extends Element> receiptLineElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        if (UtilValidate.isNotEmpty(receiptLineElementList)) {
            try {
                Map<String, String> processedStatusIdByReturnIdMap = new HashMap<String, String>();

                for (Element receiptLnElement : receiptLineElementList) {
                    Map<String, Object> ripCtx = new HashMap<String, Object>();
                    Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "os:QUANTITY");

                    String itemQtyStr = UtilXml.childElementValue(qtyElement, "of:VALUE");
                    double itemQty = Double.parseDouble(itemQtyStr);
                    String sign = UtilXml.childElementValue(qtyElement, "of:SIGN");

                    String productId = UtilXml.childElementValue(receiptLnElement, "of:ITEM");
                    if (UtilValidate.isEmpty(productId)) {
                        String errMsg = "Product ID Missing";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdMissing", "description", errMsg));
                        Debug.logError(errMsg, module);
                    }
                    // make sure productId is valid
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                        Debug.logError(errMsg, module);
                        continue;
                    }

                    Element documentRefElement = UtilXml.firstChildElement(receiptLnElement, "os:DOCUMNTREF");
                    String returnId = UtilXml.childElementValue(documentRefElement, "of:DOCUMENTID");
                    lastReturnId = returnId;
                    ripCtx.put("returnId", returnId);

                    String returnHeaderTypeId = UtilXml.childElementValue(documentRefElement, "of:DOCTYPE");
                    if (returnHeaderTypeId.equals("RMA")) {
                        returnHeaderTypeId = "CUSTOMER_RETURN";
                    }

                    String returnItemSeqId = UtilXml.childElementValue(documentRefElement, "of:LINENUM");
                    if (UtilValidate.isNotEmpty(returnItemSeqId)) {
                        // if there is a LINENUM/returnItemSeqId make sure it is valid
                        GenericValue returnItem = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId, "returnItemSeqId", returnItemSeqId).cache().queryOne();
                        if (returnItem == null) {
                            String errMsg = "Return Item with ID [" + returnId + ":" + returnItemSeqId + "] not found (invalid Return/Item ID Combination).";
                            errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReturnAndItemIdNotValid", "description", errMsg));
                            Debug.logError(errMsg, module);
                            continue;
                        }
                    } else {
                        String errMsg = "No Return Item ID (LINENUM) found in DOCUMNTREF for Return [" + returnId + "]; this is a required field.";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReturnItemIdLinenumMissing", "description", errMsg));
                        Debug.logError(errMsg, module);
                        continue;
                    }

                    // getting inventory item status
                    String invItemStatusId = null;
                    String disposition = UtilXml.childElementValue(receiptLnElement, "of:DISPOSITN");
                    if ("ReceivedTOAvailable".equals(disposition)) {
                        invItemStatusId = "INV_AVAILABLE";
                    } else if ("ReceivedTONotAvailable".equals(disposition)) {
                        invItemStatusId = "INV_ON_HOLD";
                    } else if ("NotAvailableTOAvailable".equals(disposition) || "AvailableTONotAvailable".equals(disposition)) {
                        // for RMA we should only get the ReceivedTO* DISPOSITN values; if we get something else we should return an error
                        String errMsg = "Got DISPOSITN value [" + disposition + "] that is not valid for RMA, only for status change.";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "DispositnNotValidForRMA", "description", errMsg));
                        continue;
                    }
                    ripCtx.put("statusId", invItemStatusId);

                    // TODOLATER: get the returnItem associated with the product received and update the receivedQuantity

                    String datetimeReceived = UtilXml.childElementValue(receiptLnElement, "os:DATETIMEISO");
                    Timestamp timestampItemReceived = OagisServices.parseIsoDateString(datetimeReceived, errorMapList);
                    ripCtx.put("datetimeReceived", timestampItemReceived);

                    GenericValue returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();

                    if (returnHeader != null) {
                        //getting ReturnHeader status
                        String statusId = returnHeader.get("statusId").toString();

                        // save this here so the status will be updated after all processed
                        processedStatusIdByReturnIdMap.put(returnId, statusId);

                        // getting the serial number(s)
                        List<String> serialNumsList = new LinkedList<String>();
                        List<? extends Element> invDetailList = UtilXml.childElementList(receiptLnElement, "ns:INVDETAIL");
                        if (UtilValidate.isNotEmpty(invDetailList)) {
                            for (Element invDetailElement : invDetailList) {
                                String serialNumber = UtilXml.childElementValue(invDetailElement, "of:SERIALNUM");
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
                                 GenericValue inventoryItem = EntityUtil.getFirst(delegator.findByAnd("InventoryItem", UtilMisc.toMap("serialNumber", serialNumber), null, false));
                                 if (inventoryItem !=null) {
                                     productId = inventoryItem.getString("productId");
                                 }
                             } catch (GenericEntityException e) {
                                 String errMsg = "Error Getting Entity InventoryItem";
                                 Debug.logError(e, errMsg, module);
                            } */
                        }

                        //do some validations
                        Integer messageQuantity = Integer.valueOf(itemQtyStr);
                        if (UtilValidate.isNotEmpty(serialNumsList)) {
                            if (messageQuantity.intValue() != serialNumsList.size()) {
                                String errMsg = "Not enough serial numbers [" + serialNumsList.size() + "] for the quantity [" + messageQuantity.intValue() + "].";
                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumbersMissing"));
                                Debug.logInfo(errMsg, module);
                                continue;
                            }
                        }

                        ripCtx.put("facilityId",facilityId);
                        ripCtx.put("locationSeqId", locationSeqId);
                        ripCtx.put("userLogin", userLogin);

                        // sign handling for items
                        double quantityAccepted = 0.0;
                        if (sign.equals("+")) {
                            quantityAccepted = itemQty;
                        } else {
                            quantityAccepted = 0.0;
                        }
                        if (quantityAccepted > 0) {
                            if (serialNumsList.size() > 0) {
                                String inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                                ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);

                                for (String serialNum : serialNumsList) {
                                    // also look at the productId, and associated refurb productId(s) (or other way around, we might get a refurb sku
                                    //and need to look up by the non-refurb sku); serialNumbers may not be unique globally, but should be per product
                                    Set<String> productIdSet = ProductWorker.getRefurbishedProductIdSet(productId, delegator);
                                    productIdSet.add(productId);

                                    List<GenericValue> inventoryItemsBySerialNumber = EntityQuery.use(delegator).from("InventoryItem").where(EntityCondition.makeCondition(EntityCondition.makeCondition("serialNumber", EntityOperator.EQUALS, serialNum),
                                            EntityOperator.AND, EntityCondition.makeCondition("productId", EntityOperator.IN, productIdSet))).queryList();

                                    if (OagisServices.requireSerialNumberExist != null) {
                                        // according to requireSerialNumberExist make sure serialNumber does or does not exist in database, add an error message as needed
                                        if (OagisServices.requireSerialNumberExist.booleanValue()) {
                                            if (inventoryItemsBySerialNumber.size() == 0) {
                                                String errMsg = "Referenced serial numbers must already exist, but serial number [" + serialNum + "] was not found.";
                                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                                continue;
                                            }
                                        } else {
                                            if (inventoryItemsBySerialNumber.size() > 0) {
                                                String errMsg = "Referenced serial numbers must NOT already exist, but serial number [" + serialNum + "] already exists.";
                                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredNotExistButFound"));
                                                continue;
                                            }
                                        }
                                    }

                                    // TODOLATER: another fun thing to check: see if the serial number matches a serial number attached to the original return (if possible!)

                                    //clone the context as it may be changed in the call
                                    Map<String, Object> localRipCtx = new HashMap<String, Object>();
                                    localRipCtx.putAll(ripCtx);
                                    localRipCtx.put("quantityAccepted", new Double(1.0));
                                    // always set this to 0, if needed we'll handle the rejected quantity separately
                                    localRipCtx.put("quantityRejected", new Double(0.0));
                                    localRipCtx.put("serialNumber", serialNum);
                                    localRipCtx.put("productId", productId);
                                    localRipCtx.put("returnItemSeqId", returnItemSeqId);

                                    GenericValue inventoryItem = EntityUtil.getFirst(inventoryItemsBySerialNumber);
                                    if (inventoryItem != null) {
                                        localRipCtx.put("currentInventoryItemId", inventoryItem.getString("inventoryItemId"));
                                    }

                                    Map<String, Object> ripResult = dispatcher.runSync("receiveInventoryProduct", localRipCtx);
                                    if (ServiceUtil.isError(ripResult)) {
                                        String errMsg = ServiceUtil.getErrorMessage(ripResult);
                                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg));
                                    } else {
                                        invItemIds.add((String) ripResult.get("inventoryItemId"));
                                    }
                                }
                            } else {
                                String inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
                                ripCtx.put("inventoryItemTypeId", inventoryItemTypeId);

                                // no serial numbers, just receive the quantity
                                // clone the context as it may be changted in the call
                                Map<String, Object> localRipCtx = new HashMap<String, Object>();
                                localRipCtx.putAll(ripCtx);
                                localRipCtx.put("quantityAccepted", new Double(quantityAccepted));
                                // always set this to 0, if needed we'll handle the rejected quantity separately
                                localRipCtx.put("quantityRejected", new Double(0.0));
                                localRipCtx.put("productId", productId);
                                localRipCtx.put("returnItemSeqId", returnItemSeqId);
                                String inventoryItemId = null;

                                Map<String, Object> ripResult = dispatcher.runSync("receiveInventoryProduct", localRipCtx);
                                if (ServiceUtil.isError(ripResult)) {
                                    String errMsg = ServiceUtil.getErrorMessage(ripResult);
                                    errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReceiveInventoryServiceError", "description", errMsg));
                                }
                                inventoryItemId = (String) ripResult.get("inventoryItemId");

                                invItemIds.add(inventoryItemId);

                                if (("INV_ON_HOLD").equals(invItemStatusId)) {
                                    Map<String, Object> createPhysicalInvAndVarCtx = new HashMap<String, Object>();
                                    createPhysicalInvAndVarCtx.put("inventoryItemId", inventoryItemId);
                                    createPhysicalInvAndVarCtx.put("physicalInventoryDate", UtilDateTime.nowTimestamp());
                                    // NOTE DEJ20070815: calling damaged for now as the only option so that all will feed into a check/repair process and go into the ON_HOLD status; we should at some point change OFBiz so these can go into the ON_HOLD status without having to call them damaged
                                    createPhysicalInvAndVarCtx.put("generalComments", "Damaged, in repair");
                                    createPhysicalInvAndVarCtx.put("varianceReasonId", "VAR_DAMAGED");
                                    createPhysicalInvAndVarCtx.put("availableToPromiseVar", new Double(-quantityAccepted));
                                    createPhysicalInvAndVarCtx.put("quantityOnHandVar", new Double(0.0));
                                    createPhysicalInvAndVarCtx.put("userLogin", userLogin);
                                    Map<String, Object> cpivResult = dispatcher.runSync("createPhysicalInventoryAndVariance", createPhysicalInvAndVarCtx);
                                    if (ServiceUtil.isError(cpivResult)) {
                                        String errMsg = ServiceUtil.getErrorMessage(cpivResult);
                                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "CreatePhysicalInventoryAndVarianceServiceError", "description", errMsg));
                                    }
                                }
                            }
                        } else {
                            // TODOLATER: need to run service receiveInventoryProduct and updateInventoryItem when quantityRejected > 0
                            // NOTE DEJ20070711 this shouldn't happen for current needs, so save for later
                        }
                    } else {
                        String errMsg = "Return ID [" + returnId + "] Not Found";
                        Debug.logError(errMsg, module);
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ReturnIdNotFound", "description", errMsg));
                    }
                }

                for (Map.Entry<String, String> processedStatusIdByReturnIdEntry : processedStatusIdByReturnIdMap.entrySet()) {
                    String returnId = processedStatusIdByReturnIdEntry.getKey();
                    String statusId = processedStatusIdByReturnIdEntry.getValue();

                    if (UtilValidate.isNotEmpty(statusId) && statusId.equals("RETURN_ACCEPTED")) {
                        // check to see if all return items have been received, if so then set to received then completed

                        // NOTE: an alternative method would be to see if the total has been received for each
                        //ReturnItem (receivedQuantity vs returnQuantity), but we may have a hard time matching
                        //those up so that information is probably not as reliable; another note: as of 20070821
                        //the receipt processing code properly updates ReturnItem with receivedQuantity and status
                        //so we _could_ possibly move to that method if needed

                        // loop through ReturnItem records, get totals for each productId
                        Map<String, Double> returnQuantityByProductIdMap = new HashMap<String, Double>();
                        List<GenericValue> returnItemList = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId), null, false);
                        for (GenericValue returnItem : returnItemList) {
                            String productId = returnItem.getString("productId");
                            Double returnQuantityDbl = returnItem.getDouble("returnQuantity");
                            if (UtilValidate.isNotEmpty(productId) && returnQuantityDbl != null) {
                                double newTotal = returnQuantityDbl.doubleValue();
                                Double existingTotal = returnQuantityByProductIdMap.get(productId);
                                if (existingTotal != null) newTotal += existingTotal.doubleValue();
                                returnQuantityByProductIdMap.put(productId, new Double(newTotal));
                            }
                        }

                        // set to true, if we find any that aren't fully received, will set to false
                        boolean fullReturnReceived = true;

                        // for each productId see if total received is equal to the total for that ID
                        for (Map.Entry<String, Double> returnQuantityByProductIdEntry : returnQuantityByProductIdMap.entrySet()) {
                            String productId = returnQuantityByProductIdEntry.getKey();
                            double returnQuantity = returnQuantityByProductIdEntry.getValue();

                            double receivedQuantity = 0;
                            // note no facilityId because we don't really care where the return items were received
                            List<GenericValue> shipmentReceiptList = delegator.findByAnd("ShipmentReceipt", UtilMisc.toMap("productId", productId, "returnId", returnId), null, false);
                            // NOTE only consider those with a quantityOnHandDiff > 0 so we just look at how many have been received, not what was actually done with them
                            for (GenericValue shipmentReceipt : shipmentReceiptList) {
                                Double quantityAccepted = shipmentReceipt.getDouble("quantityAccepted");
                                if (quantityAccepted != null && quantityAccepted.doubleValue() > 0) {
                                    receivedQuantity += quantityAccepted.doubleValue();
                                }
                            }

                            if (receivedQuantity < returnQuantity) {
                                fullReturnReceived = false;
                                break;
                            } else if (receivedQuantity > returnQuantity) {
                                // TODOLATER: we received MORE than expected... what to do about that?!?
                                String warnMsg = "Received more [" + receivedQuantity + "] than were expected on return [" + returnQuantity + "] for Return ID [" + returnId + "] and Product ID [" + productId + "]";
                                warnMsg = warnMsg + "; still completing return, but something should be done with these extras!";
                                Debug.logWarning(warnMsg, module);
                                // even with that, allow it to go through and complete the return
                            }
                        }

                        if (fullReturnReceived) {
                            dispatcher.runSync("updateReturnHeader", UtilMisc.<String, Object>toMap("statusId", "RETURN_RECEIVED", "returnId", returnId, "userLogin", userLogin));
                            dispatcher.runSync("updateReturnHeader", UtilMisc.<String, Object>toMap("statusId", "RETURN_COMPLETED", "returnId", returnId, "userLogin", userLogin));
                        }
                    }
                }
            } catch (Throwable t) {
                String errMsg = "System Error processing Acknowledge Delivery RMA message for message [" + omiPkMap + "]: " + t.toString();
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));

                try {
                    comiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);

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
                return ServiceUtil.returnError(errMsg);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("logicalId", logicalId);
        result.put("component", component);
        result.put("task", task);
        result.put("referenceId", referenceId);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            try {
                comiCtx.put("processingStatusId", "OAGMP_PROC_ERROR");
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
            saveErrorMapListCtx.put("logicalId", logicalId);
            saveErrorMapListCtx.put("component", component);
            saveErrorMapListCtx.put("task", task);
            saveErrorMapListCtx.put("referenceId", referenceId);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);
                // NOTE: this is different for each service, should be shipmentId or returnId or PO orderId or etc
                // TODO: unfortunately there could be multiple returnIds for the message, so what to do...?
                sendConfirmBodCtx.put("origRefId", lastReturnId);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error sending Confirm BOD: " + e.toString();
                Debug.logError(e, errMsg, module);
            }
            String errMsg = "Found business level errors in message processing, not saving results; first error is: " + errorMapList.get(0);

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
            comiCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }

        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        result.put("inventoryItemIdList", invItemIds);
        return result;
    }

    public static Map<String, Object> oagisReceiveAcknowledgeDeliveryStatus(DispatchContext ctx, Map<String, Object> context) {
        Document doc = (Document) context.get("document");
        boolean isErrorRetry = Boolean.TRUE.equals(context.get("isErrorRetry"));
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Delegator delegator = ctx.getDelegator();
        List<Map<String, String>> errorMapList = new LinkedList<Map<String,String>>();

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error Getting UserLogin: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        // parse the message
        Element receiveStatusElement = doc.getDocumentElement();
        receiveStatusElement.normalize();
        Element docCtrlAreaElement = UtilXml.firstChildElement(receiveStatusElement, "os:CNTROLAREA");
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

        Element dataAreaElement = UtilXml.firstChildElement(receiveStatusElement, "ns:DATAAREA");
        Element acknowledgeDeliveryElement = UtilXml.firstChildElement(dataAreaElement, "ns:ACKNOWLEDGE_DELIVERY");

        String facilityId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.PoReceiptFacilityId", delegator);
        String locationSeqId = EntityUtilProperties.getPropertyValue("oagis", "Oagis.Warehouse.ReturnReceiptLocationSeqId", delegator);

        Timestamp timestamp = UtilDateTime.nowTimestamp();
        Map<String, Object> comiCtx = new HashMap<String, Object>();

        Map<String, Object> omiPkMap = UtilMisc.toMap("logicalId", (Object) logicalId, "component", component, "task", task, "referenceId", referenceId);

        // always log this to make messages easier to find
        Debug.logInfo("Processing oagisReceiveAcknowledgeDeliveryStatus for message ID [" + omiPkMap + "]", module);

        // before getting into this check to see if we've tried once and had an error, if so set isErrorRetry even if it wasn't passed in
        GenericValue previousOagisMessageInfo = null;
        try {
            previousOagisMessageInfo = delegator.findOne("OagisMessageInfo", omiPkMap, false);
        } catch (GenericEntityException e) {
            String errMsg = "Error getting OagisMessageInfo from database for message ID [" + omiPkMap + "]: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            // anything else to do about this? we don't really want to send the error back or anything...
        }

        if (previousOagisMessageInfo != null && !isErrorRetry) {
            if ("OAGMP_SYS_ERROR".equals(previousOagisMessageInfo.getString("processingStatusId"))) {
                isErrorRetry = true;
            } else {
                // message already in the db, but is not in a system error state...
                Debug.logError("Message received for message ID [" + omiPkMap + "] was already partially processed but is not in a system error state, needs manual review; message ID: " + omiPkMap, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "OagisErrorMessageAlreadyProcessed", UtilMisc.toMap("shipmentId", "", "omiPkMap", omiPkMap), locale));
            }
        }

        comiCtx.putAll(omiPkMap);
        comiCtx.put("authId", authId);
        comiCtx.put("receivedDate", timestamp);
        comiCtx.put("sentDate", sentTimestamp);
        comiCtx.put("outgoingMessage", "N");
        comiCtx.put("confirmation", confirmation);
        comiCtx.put("bsrVerb", bsrVerb);
        comiCtx.put("bsrNoun", bsrNoun);
        comiCtx.put("bsrRevision", bsrRevision);
        comiCtx.put("processingStatusId", "OAGMP_RECEIVED");
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
            if (isErrorRetry) {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } else {
                dispatcher.runSync("createOagisMessageInfo", comiCtx, 60, true);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error creating OagisMessageInfo for the Incoming Message: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        //String inventoryItemId = null;
        List<String> invItemIds = new LinkedList<String>();
        // get RECEIPTLN elements from message
        List<? extends Element> receiptLineElementList = UtilXml.childElementList(acknowledgeDeliveryElement, "ns:RECEIPTLN");
        if (UtilValidate.isNotEmpty(receiptLineElementList)) {
            try {
                for (Element receiptLnElement : receiptLineElementList) {
                    Map<String, Object> uiiCtx = new HashMap<String, Object>();
                    Element qtyElement = UtilXml.firstChildElement(receiptLnElement, "os:QUANTITY");

                    String itemQtyStr = UtilXml.childElementValue(qtyElement, "of:VALUE");
                    String sign = UtilXml.childElementValue(qtyElement, "of:SIGN");

                    String productId = UtilXml.childElementValue(receiptLnElement, "of:ITEM");
                    if (UtilValidate.isEmpty(productId)) {
                        String errMsg = "Product ID Missing";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdMissing", "description", errMsg));
                        Debug.logError(errMsg, module);
                    }
                    // make sure productId is valid
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if (product == null) {
                        String errMsg = "Product with ID [" + productId + "] not found (invalid Product ID).";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "ProductIdNotValid", "description", errMsg));
                        Debug.logError(errMsg, module);
                        continue;
                    }

                    /* no place to put this in the InventoryItem update
                    String datetimeReceived = UtilXml.childElementValue(receiptLnElement, "os:DATETIMEISO");
                    Timestamp timestampItemReceived = OagisServices.parseIsoDateString(datetimeReceived, errorMapList);
                    uiiCtx.put("datetimeReceived", timestampItemReceived);
                    */

                    String invItemStatusId = null;
                    String reqFromItemStatusId = null;
                    String disposition = UtilXml.childElementValue(receiptLnElement, "of:DISPOSITN");
                    if ("NotAvailableTOAvailable".equals(disposition)) {
                        invItemStatusId = "INV_AVAILABLE";
                        reqFromItemStatusId = "INV_ON_HOLD";
                    } else if ("AvailableTONotAvailable".equals(disposition)) {
                        invItemStatusId = "INV_ON_HOLD";
                        reqFromItemStatusId = "INV_AVAILABLE";
                    } else if ("ReceivedTOAvailable".equals(disposition) || "ReceivedTONotAvailable".equals(disposition)) {
                        String errMsg = "Got DISPOSITN value [" + disposition + "] that is not valid for Status Change, only for RMA/return.";
                        errorMapList.add(UtilMisc.<String, String>toMap("reasonCode", "DispositnNotValidForStatusChange", "description", errMsg));
                        continue;
                    }

                    uiiCtx.put("statusId", invItemStatusId);

                    // geting the serial number(s)
                    List<String> serialNumsList = new LinkedList<String>();
                    List<? extends Element> invDetailList = UtilXml.childElementList(receiptLnElement, "ns:INVDETAIL");
                    if (UtilValidate.isNotEmpty(invDetailList)) {
                        for (Element invDetailElement : invDetailList) {
                            String serialNumber = UtilXml.childElementValue(invDetailElement, "of:SERIALNUM");
                            if (UtilValidate.isNotEmpty(serialNumber)) {
                                serialNumsList.add(serialNumber);
                            }
                        }
                    }

                    //do some validations
                    Integer messageQuantity = Integer.valueOf(itemQtyStr);
                    if (UtilValidate.isNotEmpty(serialNumsList)) {
                        if (messageQuantity.intValue() != serialNumsList.size()) {
                            String errMsg = "Not enough serial numbers [" + serialNumsList.size() + "] for the quantity [" + messageQuantity.intValue() + "].";
                            errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumbersMissing"));
                            Debug.logInfo(errMsg, module);
                            continue;
                        }
                    }

                    uiiCtx.put("facilityId",facilityId);
                    uiiCtx.put("locationSeqId", locationSeqId);
                    uiiCtx.put("userLogin", userLogin);

                    // sign handling for items
                    if (!"+".equals(sign)) {
                        String errMsg = "Got a sign [" + sign + "] that was not plus (+), this is not valid for a Status Change operation.";
                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SignNotPlusForStatusChange"));
                        continue;
                    }

                    if (serialNumsList.size() > 0) {
                        String inventoryItemTypeId = "SERIALIZED_INV_ITEM";
                        uiiCtx.put("inventoryItemTypeId", inventoryItemTypeId);

                        for (String serialNum : serialNumsList) {
                            // also look at the productId, and associated refurb productId(s) (or other way around, we might get a refurb sku
                            //and need to look up by the non-refurb sku); serialNumbers may not be unique globally, but should be per product
                            Set<String> productIdSet = ProductWorker.getRefurbishedProductIdSet(productId, delegator);
                            productIdSet.add(productId);

                            List<GenericValue> inventoryItemsBySerialNumber = EntityQuery.use(delegator).from("InventoryItem").where(EntityCondition.makeCondition(EntityCondition.makeCondition("serialNumber", EntityOperator.EQUALS, serialNum),
                                    EntityOperator.AND, EntityCondition.makeCondition("productId", EntityOperator.IN, productIdSet))).queryList();

                            // this is a status update, so referenced serial number MUST already exist
                            if (inventoryItemsBySerialNumber.size() == 0) {
                                String errMsg = "Referenced serial numbers must already exist, but serial number [" + serialNum + "] was not found.";
                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                continue;
                            }

                            GenericValue inventoryItem = EntityUtil.getFirst(inventoryItemsBySerialNumber);
                            if (UtilValidate.isNotEmpty(reqFromItemStatusId) && !reqFromItemStatusId.equals(inventoryItem.getString("statusId"))) {
                                String errMsg = "Referenced serial number [" + serialNum + "] has status [" + inventoryItem.getString("statusId") + "] but we were expecting [" + reqFromItemStatusId + "]; this may mean the Acknowledge Delivery RMA message has not yet come in for this item.";
                                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SerialNumberRequiredButNotFound"));
                                continue;
                            }

                            Map<String, Object> updateInvItmMap = new HashMap<String, Object>();
                            updateInvItmMap.put("inventoryItemId", inventoryItem.getString("inventoryItemId"));
                            updateInvItmMap.put("userLogin", userLogin);
                            updateInvItmMap.put("statusId", invItemStatusId);
                            String inventoryItemProductId = inventoryItem.getString("productId");
                            if (!inventoryItemProductId.equals(productId)) {
                                // got a new productId for the serial number; this may happen for refurbishment, etc
                                updateInvItmMap.put("productId",productId);
                            }
                            dispatcher.runSync("updateInventoryItem", updateInvItmMap);
                            invItemIds.add(inventoryItem.getString("inventoryItemId"));
                        }
                    } else {
                        String inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
                        uiiCtx.put("inventoryItemTypeId", inventoryItemTypeId);

                        // TODO: later somehow do status changes for non-serialized inventory

                        // for now just return an error message
                        String errMsg = "No serial numbers were included in the message and right now this is not supported";
                        errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "NoSerialNumbersInMessage"));
                    }
                }
            } catch (Throwable t) {
                String errMsg = "System Error processing Acknowledge Delivery Status message for message [" + omiPkMap + "]: " + t.toString();
                errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "SystemError"));

                try {
                    comiCtx.put("processingStatusId", "OAGMP_SYS_ERROR");
                    dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);

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
                return ServiceUtil.returnError(errMsg);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.putAll(omiPkMap);
        result.put("userLogin", userLogin);

        if (errorMapList.size() > 0) {
            try {
                comiCtx.put("processingStatusId", "OAGMP_PROC_ERROR");
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            // call services createOagisMsgErrInfosFromErrMapList and for incoming messages oagisSendConfirmBod
            Map<String, Object> saveErrorMapListCtx = new HashMap<String, Object>();
            saveErrorMapListCtx.putAll(omiPkMap);
            saveErrorMapListCtx.put("errorMapList", errorMapList);
            saveErrorMapListCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createOagisMsgErrInfosFromErrMapList", saveErrorMapListCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            try {
                Map<String, Object> sendConfirmBodCtx = new HashMap<String, Object>();
                sendConfirmBodCtx.putAll(saveErrorMapListCtx);

                // run async because this will send a message back to the other server and may take some time, and/or fail
                dispatcher.runAsync("oagisSendConfirmBod", sendConfirmBodCtx, null, true, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error sending Confirm BOD: " + e.toString();
                Debug.logError(e, errMsg, module);
            }

            String errMsg = "Found business level errors in message processing, not saving results; first error is: " + errorMapList.get(0);

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
            comiCtx.put("processingStatusId", "OAGMP_PROC_SUCCESS");
            try {
                dispatcher.runSync("updateOagisMessageInfo", comiCtx, 60, true);
            } catch (GenericServiceException e) {
                String errMsg = "Error updating OagisMessageInfo for the Incoming Message: " + e.toString();
                // don't pass this back, nothing they can do about it: errorMapList.add(UtilMisc.<String, String>toMap("description", errMsg, "reasonCode", "GenericServiceException"));
                Debug.logError(e, errMsg, module);
            }
        }

        result.putAll(ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "OagisServiceCompletedSuccessfully", locale)));
        result.put("inventoryItemIdList", invItemIds);
        return result;
    }
}
