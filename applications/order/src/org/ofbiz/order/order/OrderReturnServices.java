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

package org.ofbiz.order.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.thirdparty.paypal.ExpressCheckoutEvents;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * OrderReturnServices
 */
public class OrderReturnServices {

    public static final String module = OrderReturnServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    //  set some BigDecimal properties
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO = ZERO.setScale(decimals);
    }

    // locate the return item's initial inventory item cost
    public static Map getReturnItemInitialCost(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        String returnItemSeqId = (String) context.get("returnItemSeqId");

        Map result = ServiceUtil.returnSuccess();
        result.put("initialItemCost", getReturnItemInitialCost(delegator, returnId, returnItemSeqId));
        return result;
    }

    // obtain order/return total information
    public static Map getOrderAvailableReturnedTotal(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        OrderReadHelper orh = null;
        try {
            orh = new OrderReadHelper(delegator, orderId);
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // an adjustment value to test
        BigDecimal adj = (BigDecimal) context.get("adjustment");
        if (adj == null) {
            adj = ZERO;
        }

        Boolean countNewReturnItems = (Boolean) context.get("countNewReturnItems");
        if (countNewReturnItems == null) {
            countNewReturnItems = Boolean.FALSE;
        }
        BigDecimal returnTotal = orh.getOrderReturnedTotal(countNewReturnItems.booleanValue());
        BigDecimal orderTotal = orh.getOrderGrandTotal();
        BigDecimal available = orderTotal.subtract(returnTotal).subtract(adj);


        Map result = ServiceUtil.returnSuccess();
        result.put("availableReturnTotal", available);
        result.put("orderTotal", orderTotal);
        result.put("returnTotal", returnTotal);
        return result;
    }

    // worker method which can be used in screen iterations
    public static BigDecimal getReturnItemInitialCost(Delegator delegator, String returnId, String returnItemSeqId) {
        if (delegator == null || returnId == null || returnItemSeqId == null) {
            throw new IllegalArgumentException("Method parameters cannot contain nulls");
        }
        Debug.log("Finding the initial item cost for return item : " + returnId + " / " + returnItemSeqId, module);

        // the cost holder
        BigDecimal itemCost = BigDecimal.ZERO;

        // get the return item information
        GenericValue returnItem = null;
        try {
            returnItem = delegator.findByPrimaryKey("ReturnItem", UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItemSeqId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }
        Debug.log("Return item value object - " + returnItem, module);

        // check for an orderItem association
        if (returnItem != null) {
            String orderId = returnItem.getString("orderId");
            String orderItemSeqId = returnItem.getString("orderItemSeqId");
            if (orderItemSeqId != null && orderId != null) {
                Debug.log("Found order item reference", module);
                // locate the item issuance(s) for this order item
                List itemIssue = null;
                try {
                    itemIssue = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    throw new GeneralRuntimeException(e.getMessage());
                }
                if (UtilValidate.isNotEmpty(itemIssue)) {
                    Debug.log("Found item issuance reference", module);
                    // just use the first one for now; maybe later we can find a better way to determine which was the
                    // actual item being returned; maybe by serial number
                    GenericValue issue = EntityUtil.getFirst(itemIssue);
                    GenericValue inventoryItem = null;
                    try {
                        inventoryItem = issue.getRelatedOne("InventoryItem");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        throw new GeneralRuntimeException(e.getMessage());
                    }
                    if (inventoryItem != null) {
                        Debug.log("Located inventory item - " + inventoryItem.getString("inventoryItemId"), module);
                        if (inventoryItem.get("unitCost") != null) {
                            itemCost = inventoryItem.getBigDecimal("unitCost");
                        } else {
                            Debug.logInfo("Found item cost; but cost was null. Returning default amount (0.00)", module);
                        }
                    }
                }
            }
        }

        Debug.log("Initial item cost - " + itemCost, module);
        return itemCost;
    }

    // helper method for sending return notifications
    private static Map sendReturnNotificationScreen(DispatchContext dctx, Map context, String emailType) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");

        // get the return header
        GenericValue returnHeader = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorUnableToGetReturnHeaderForID", UtilMisc.toMap("returnId",returnId), locale));
        }

        // get the return items
        List returnItems = null;
        try {
            returnItems = returnHeader.getRelated("ReturnItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorUnableToGetReturnItemRecordsFromReturnHeader", locale));
        }

        // get the order header -- the first item will determine which product store to use from the order
        String productStoreId = null;
        String emailAddress = null;
        if (UtilValidate.isNotEmpty(returnItems)) {
            GenericValue firstItem = EntityUtil.getFirst(returnItems);
            GenericValue orderHeader = null;
            try {
                orderHeader = firstItem.getRelatedOne("OrderHeader");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorUnableToGetOrderHeaderFromReturnItem", locale));
            }

            if (orderHeader != null && UtilValidate.isNotEmpty(orderHeader.getString("productStoreId"))) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                productStoreId = orh.getProductStoreId();
                emailAddress = orh.getOrderEmailString();
            }
        }

        // get the email setting and send the mail
        if (UtilValidate.isNotEmpty(productStoreId)) {
            Map sendMap = FastMap.newInstance();

            GenericValue productStoreEmail = null;
            try {
                productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", productStoreId, "emailType", emailType));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (productStoreEmail != null && emailAddress != null) {
                String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
                if (UtilValidate.isEmpty(bodyScreenLocation)) {
                    bodyScreenLocation = ProductStoreWorker.getDefaultProductStoreEmailScreenLocation(emailType);
                }
                sendMap.put("bodyScreenUri", bodyScreenLocation);
                String xslfoAttachScreenLocation = productStoreEmail.getString("xslfoAttachScreenLocation");
                sendMap.put("xslfoAttachScreenLocation", xslfoAttachScreenLocation);

                ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("EcommerceUiLabels", locale);
                uiLabelMap.addBottomResourceBundle("OrderUiLabels");
                uiLabelMap.addBottomResourceBundle("CommonUiLabels");

                Map bodyParameters = UtilMisc.toMap("returnHeader", returnHeader, "returnItems", returnItems, "uiLabelMap", uiLabelMap, "locale", locale);
                sendMap.put("bodyParameters", bodyParameters);

                sendMap.put("subject", productStoreEmail.getString("subject"));
                sendMap.put("contentType", productStoreEmail.get("contentType"));
                sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                sendMap.put("sendTo", emailAddress);

                sendMap.put("userLogin", userLogin);

                Map sendResp = null;
                try {
                    sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem sending mail", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderProblemSendingEmail", locale));
                }

                // check for errors
                if (sendResp != null && ServiceUtil.isError(sendResp)) {
                    sendResp.put("emailType", emailType);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderProblemSendingEmail", locale), null, null, sendResp);
                }
                return ServiceUtil.returnSuccess();
            }
        }

        return ServiceUtil.returnFailure("No valid email setting for store");
    }

    // return request notification
    public static Map sendReturnAcceptNotification(DispatchContext dctx, Map context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_ACCEPT");
    }

    // return complete notification
    public static Map sendReturnCompleteNotification(DispatchContext dctx, Map context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_COMPLETE");
    }

    // return cancel notification
    public static Map sendReturnCancelNotification(DispatchContext dctx, Map context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_CANCEL");
    }
    
    // cancel replacement order if return not received within 30 days and send notification
    public static Map<String,Object> autoCancelReplacementOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<GenericValue> returnHeaders = null;
        try {
            returnHeaders = delegator.findList("ReturnHeader", EntityCondition.makeCondition(
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "RETURN_ACCEPTED"), EntityOperator.AND, 
                    EntityCondition.makeCondition("returnHeaderTypeId", EntityOperator.EQUALS, "CUSTOMER_RETURN")), null, UtilMisc.toList("entryDate"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Return headers", module);
        }
        for (GenericValue returnHeader : returnHeaders) {
            String returnId = returnHeader.getString("returnId");
            Timestamp entryDate = returnHeader.getTimestamp("entryDate");
            String daysTillCancelStr = UtilProperties.getPropertyValue("order.properties", "daysTillCancelReplacementOrder", "30");
            int daysTillCancel = 0;
            try {
                daysTillCancel = Integer.parseInt(daysTillCancelStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, "Unable to get daysTillCancel", module);
            }
            if (daysTillCancel > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(entryDate.getTime());
                cal.add(Calendar.DAY_OF_YEAR, daysTillCancel);
                Date cancelDate = cal.getTime();
                Date nowDate = new Date();
                if (cancelDate.equals(nowDate) || nowDate.after(cancelDate)) {
                    try {
                        List<GenericValue> returnItems = delegator.findList("ReturnItem", EntityCondition.makeCondition(
                                EntityCondition.makeCondition("returnId", EntityOperator.EQUALS, returnId), EntityOperator.AND, 
                                EntityCondition.makeCondition("returnTypeId", EntityOperator.EQUALS, "RTN_WAIT_REPLACE_RES")), null, UtilMisc.toList("createdStamp"), null, false);
                        for (GenericValue returnItem : returnItems) {
                            GenericValue returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse");
                            if (returnItemResponse != null) {
                                String replacementOrderId = returnItemResponse.getString("replacementOrderId");
                                Map svcCtx = UtilMisc.toMap("orderId", replacementOrderId, "userLogin", userLogin);
                                GenericValue orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", replacementOrderId), false);
                                if ("ORDER_HOLD".equals(orderHeader.getString("statusId"))) {
                                    try {
                                        dispatcher.runSync("cancelOrderItem", svcCtx);
                                    } catch (GenericServiceException e) {
                                        Debug.logError(e, "Problem calling service cancelOrderItem: " + svcCtx, module);
                                    }
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);;
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }
    // get the returnable quantiy for an order item
    public static Map getReturnableQuantity(DispatchContext dctx, Map context) {
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        GenericValue product = null;
        Locale locale = (Locale) context.get("locale");
        if (orderItem.get("productId") != null) {
            try {
                product = orderItem.getRelatedOne("Product");
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Unable to get Product from OrderItem", module);
            }
        }

        // check returnable status
        boolean returnable = true;

        // first check returnable flag
        if (product != null && product.get("returnable") != null &&
                "N".equalsIgnoreCase(product.getString("returnable"))) {
            // the product is not returnable at all
            returnable = false;
        }

        // next check support discontinuation
        if (product != null && product.get("supportDiscontinuationDate") != null &&
                !UtilDateTime.nowTimestamp().before(product.getTimestamp("supportDiscontinuationDate"))) {
            // support discontinued either now or in the past
            returnable = false;
        }

        String itemStatus = orderItem.getString("statusId");
        BigDecimal orderQty = orderItem.getBigDecimal("quantity");
        if (orderItem.getBigDecimal("cancelQuantity") != null) {
            orderQty = orderQty.subtract(orderItem.getBigDecimal("cancelQuantity"));
        }

        // get the returnable quantity
        BigDecimal returnableQuantity = BigDecimal.ZERO;
        if (returnable && (itemStatus.equals("ITEM_APPROVED") || itemStatus.equals("ITEM_COMPLETED"))) {
            List returnedItems = null;
            try {
                returnedItems = orderItem.getRelated("ReturnItem");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetReturnItemInformation", locale));
            }
            if (UtilValidate.isEmpty(returnedItems)) {
                returnableQuantity = orderQty;
            } else {
                BigDecimal returnedQty = BigDecimal.ZERO;
                Iterator ri = returnedItems.iterator();
                while (ri.hasNext()) {
                    GenericValue returnItem = (GenericValue) ri.next();
                    GenericValue returnHeader = null;
                    try {
                        returnHeader = returnItem.getRelatedOne("ReturnHeader");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetReturnHeaderFromItem", locale));
                    }
                    String returnStatus = returnHeader.getString("statusId");
                    if (!returnStatus.equals("RETURN_CANCELLED")) {
                        returnedQty = returnedQty.add(returnItem.getBigDecimal("returnQuantity"));
                    }
                }
                if (returnedQty.compareTo(orderQty) < 0) {
                    returnableQuantity = orderQty.subtract(returnedQty);
                }
            }
        }

        // get the returnable price now equals to orderItem.unitPrice, since adjustments are booked separately

        Map result = ServiceUtil.returnSuccess();
        result.put("returnableQuantity", returnableQuantity);
        result.put("returnablePrice", orderItem.getBigDecimal("unitPrice"));
        return result;
    }

    // get a map of returnable items (items not already returned) and quantities
    public static Map getReturnableItems(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetReturnItemInformation", locale));
        }

        Map returnable = new LinkedHashMap();
        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            // OrderItems which have been issued may be returned.
            EntityConditionList whereConditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderHeader.getString("orderId")),
                    EntityCondition.makeCondition("orderItemStatusId", EntityOperator.IN, UtilMisc.toList("ITEM_APPROVED", "ITEM_COMPLETED"))
               ), EntityOperator.AND);
            /*
            EntityConditionList havingConditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("quantityIssued", EntityOperator.GREATER_THAN, Double.valueOf(0))
               ), EntityOperator.AND);
             */
            List orderItemQuantitiesIssued = null;
            try {
                orderItemQuantitiesIssued = delegator.findList("OrderItemQuantityReportGroupByItem", whereConditions, UtilMisc.toSet("orderId", "orderItemSeqId", "quantityIssued"), UtilMisc.toList("orderItemSeqId"), null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetReturnHeaderFromItem", locale));
            }

            if (orderItemQuantitiesIssued != null) {
                Iterator i = orderItemQuantitiesIssued.iterator();
                while (i.hasNext()) {
                    GenericValue orderItemQuantityIssued = (GenericValue) i.next();
                    GenericValue item = null;
                    try {
                        item = orderItemQuantityIssued.getRelatedOne("OrderItem");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetOrderItemInformation", locale));
                    }
                    // items not issued/shipped are considered as returnable only if they are
                    // not physical items
                    BigDecimal quantityIssued = orderItemQuantityIssued.getBigDecimal("quantityIssued");
                    if (UtilValidate.isEmpty(quantityIssued) || quantityIssued.compareTo(BigDecimal.ZERO) == 0) {
                        try {
                            GenericValue itemProduct = item.getRelatedOne("Product");
                            if (ProductWorker.isPhysical(itemProduct)) {
                                continue;
                            }
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Problems looking up returnable product type information", module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetTheItemReturnableProduct", locale));
                        }

                    }
                    Map serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("getReturnableQuantity", UtilMisc.toMap("orderItem", item));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToGetTheItemReturnableQuantity", locale));
                    }
                    if (serviceResult.containsKey(ModelService.ERROR_MESSAGE)) {
                        return ServiceUtil.returnError((String) serviceResult.get(ModelService.ERROR_MESSAGE));
                    } else {

                        // Don't add the OrderItem to the map of returnable OrderItems if there isn't any returnable quantity.
                        if (((BigDecimal) serviceResult.get("returnableQuantity")).compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                        Map returnInfo = new HashMap();
                        // first the return info (quantity/price)
                        returnInfo.put("returnableQuantity", serviceResult.get("returnableQuantity"));
                        returnInfo.put("returnablePrice", serviceResult.get("returnablePrice"));

                        // now the product type information
                        String itemTypeKey = "FINISHED_GOOD"; // default item type (same as invoice)
                        GenericValue product = null;
                        if (item.get("productId") != null) {
                            try {
                                product = item.getRelatedOne("Product");
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError("Unable to obtain order item information!");
                            }
                        }
                        if (product != null) {
                            itemTypeKey = product.getString("productTypeId");
                        } else if (item != null && item.getString("orderItemTypeId") != null) {
                            itemTypeKey = item.getString("orderItemTypeId");
                        }
                        returnInfo.put("itemTypeKey", itemTypeKey);

                        returnable.put(item, returnInfo);

                        // Order item adjustments
                        List itemAdjustments = null;
                        try {
                            itemAdjustments = item.getRelated("OrderAdjustment");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError("Unable to obtain order item adjustments");
                        }
                        if (UtilValidate.isNotEmpty(itemAdjustments)) {
                            Iterator itemAdjustmentsIt = itemAdjustments.iterator();
                            while (itemAdjustmentsIt.hasNext()) {
                                GenericValue itemAdjustment = (GenericValue)itemAdjustmentsIt.next();
                                returnInfo = new HashMap();
                                returnInfo.put("returnableQuantity", BigDecimal.ONE);
                                 // TODO: the returnablePrice should be set to the amount minus the already returned amount
                                returnInfo.put("returnablePrice", itemAdjustment.get("amount"));
                                returnInfo.put("itemTypeKey", itemTypeKey);
                                returnable.put(itemAdjustment, returnInfo);
                            }
                        }
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorNoOrderItemsFound", locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToFindOrderHeader", locale));
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("returnableItems", returnable);
        return result;
    }

    // check return items status and update return header status
    public static Map checkReturnComplete(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List returnItems = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        // if already completed just return
        String currentStatus = null;
        if (returnHeader != null && returnHeader.get("statusId") != null) {
            currentStatus = returnHeader.getString("statusId");
            if ("RETURN_COMPLETED".equals(currentStatus) || "RETURN_CANCELLED".equals(currentStatus)) {
                return ServiceUtil.returnSuccess();
            }
        }

        // now; to be used for all timestamps
        Timestamp now = UtilDateTime.nowTimestamp();

        List completedItems = new ArrayList();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            Iterator itemsIter = returnItems.iterator();
            while (itemsIter.hasNext()) {
                GenericValue item = (GenericValue) itemsIter.next();
                String itemStatus = item != null ? item.getString("statusId") : null;
                if (itemStatus != null) {
                    // both completed and cancelled items qualify for completed status change
                    if ("RETURN_COMPLETED".equals(itemStatus) || "RETURN_CANCELLED".equals(itemStatus)) {
                        completedItems.add(item);
                    } else {
                        // Non-physical items don't need an inventory receive and so are
                        // considered completed after the return is accepted
                        if ("RETURN_ACCEPTED".equals(returnHeader.getString("statusId"))) {
                            try {
                                GenericValue itemProduct = item.getRelatedOne("Product");
                                if (!ProductWorker.isPhysical(itemProduct)) {
                                    completedItems.add(item);
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Problems looking up returned product type information", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingReturnHeaderItemInformation", locale));
                            }
                        }
                    }
                }
            }

            // if all items are completed/cancelled these should match
            if (completedItems.size() == returnItems.size()) {
                // The return is just moved to its next status by calling the
                // updateReturnHeader service; this will trigger all the appropriate ecas
                // including this service again, so that the return is moved
                // to the final status
                if (currentStatus != null && "RETURN_ACCEPTED".equals(currentStatus)) {
                    try {
                        dispatcher.runSync("updateReturnHeader", UtilMisc.<String, Object>toMap("returnId", returnId,
                                                                                "statusId", "RETURN_RECEIVED",
                                                                                "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToCreateReturnStatusHistory", locale));
                    }
                } else if (currentStatus != null && "RETURN_RECEIVED".equals(currentStatus)) {
                    try {
                        dispatcher.runSync("updateReturnHeader", UtilMisc.<String, Object>toMap("returnId", returnId,
                                                                                "statusId", "RETURN_COMPLETED",
                                                                                "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorUnableToCreateReturnStatusHistory", locale));
                    }
                }
            }

        }

        Map result = ServiceUtil.returnSuccess();
        result.put("statusId", returnHeader.get("statusId"));
        return result;
    }

    // credit (billingAccount) return
    public static Map processCreditReturn(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader != null) {
                returnItems = returnHeader.getRelatedByAnd("ReturnItem", UtilMisc.toMap("returnTypeId", "RTN_CREDIT"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap("returnId", returnId, "returnTypeId", "RTN_CREDIT"));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            String finAccountId = returnHeader.getString("finAccountId");
            String billingAccountId = returnHeader.getString("billingAccountId");
            String fromPartyId = returnHeader.getString("fromPartyId");
            String toPartyId = returnHeader.getString("toPartyId");

            // make sure total refunds on a return don't exceed amount of returned orders
            Map serviceResult = null;
            try {
                serviceResult = dispatcher.runSync("checkPaymentAmountForRefund", UtilMisc.toMap("returnId", returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem running the checkPaymentAmountForRefund service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithCheckPaymentAmountForRefund", locale));
            }
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }

            // Fetch the ProductStore
            GenericValue productStore = null;
            GenericValue orderHeader = null;
            GenericValue returnItem = null;
            if (UtilValidate.isNotEmpty(returnItems)) {
                returnItem = EntityUtil.getFirst(returnItems);
            }
            if (UtilValidate.isNotEmpty(returnItem)) {
                try {
                    orderHeader = returnItem.getRelatedOne("OrderHeader");
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            if (orderHeader != null) {
                OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
                productStore = orderReadHelper.getProductStore();
            }

            // if both billingAccountId and finAccountId are supplied, look for productStore.storeCreditAccountEnumId preference
            if (finAccountId != null && billingAccountId != null) {
                Debug.logWarning("FinAccount and BillingAccount both are supplied for storing credit, priority will be given to ProductStore preference. Default is FinAccount.", module);
                if (productStore != null && productStore.getString("storeCreditAccountEnumId") != null && "BILLING_ACCOUNT".equals(productStore.getString("storeCreditAccountEnumId"))) {
                    finAccountId = null;
                } else {
                    billingAccountId = null;
                }
            }

            if (finAccountId == null && billingAccountId == null) {
                // First find a Billing Account with negative balance, and if found store credit to that
                List<GenericValue> billingAccounts = FastList.newInstance();
                try {
                    billingAccounts = delegator.findByAnd("BillingAccountRoleAndAddress", UtilMisc.toMap("partyId", fromPartyId, "roleTypeId", "BILL_TO_CUSTOMER"));
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                billingAccounts = EntityUtil.filterByDate(billingAccounts);
                billingAccounts = EntityUtil.orderBy(billingAccounts, UtilMisc.toList("-fromDate"));
                if (UtilValidate.isNotEmpty(billingAccounts)) {
                    ListIterator<GenericValue> billingAccountItr = billingAccounts.listIterator();
                    while (billingAccountItr.hasNext() && billingAccountId == null) {
                        String thisBillingAccountId = billingAccountItr.next().getString("billingAccountId");
                        BigDecimal billingAccountBalance = ZERO;
                        try {
                            billingAccountBalance = getBillingAccountBalance(thisBillingAccountId, dctx);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (billingAccountBalance.signum() == -1) {
                            billingAccountId = thisBillingAccountId;
                        }
                    }
                }

                // if no billing account with negative balance is found, look for productStore.storeCreditAccountEnumId settings
                if (billingAccountId == null) {
                    if (productStore != null && productStore.getString("storeCreditAccountEnumId") != null && "BILLING_ACCOUNT".equals(productStore.getString("storeCreditAccountEnumId"))) {
                        if (UtilValidate.isNotEmpty(billingAccounts)) {
                            billingAccountId = EntityUtil.getFirst(billingAccounts).getString("billingAccountId");
                        } else {
                            // create new BillingAccount w/ 0 balance
                            Map results = createBillingAccountFromReturn(returnHeader, returnItems, dctx, context);
                            if (ServiceUtil.isError(results)) {
                                Debug.logError("Error creating BillingAccount: " + results.get(ModelService.ERROR_MESSAGE), module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorWithCreateBillingAccount", locale) + results.get(ModelService.ERROR_MESSAGE));
                            }
                            billingAccountId = (String) results.get("billingAccountId");

                            // double check; make sure we have a billingAccount
                            if (billingAccountId == null) {
                                Debug.logError("No available billing account, none was created", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoAvailableBillingAccount", locale));
                            }
                        }
                    } else {
                        List<GenericValue> finAccounts = null;
                        try {
                            finAccounts = delegator.findByAnd("FinAccountAndRole", UtilMisc.toMap("partyId", fromPartyId, "finAccountTypeId", "STORE_CREDIT_ACCT", "roleTypeId", "OWNER", "statusId", "FNACT_ACTIVE"));
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        finAccounts = EntityUtil.filterByDate(finAccounts);
                        finAccounts = EntityUtil.orderBy(finAccounts, UtilMisc.toList("-fromDate"));
                        if (UtilValidate.isNotEmpty(finAccounts)) {
                            finAccountId = EntityUtil.getFirst(finAccounts).getString("finAccountId");
                        }

                        if (finAccountId == null) {
                            Map createAccountCtx = FastMap.newInstance();
                            createAccountCtx.put("ownerPartyId", fromPartyId);
                            createAccountCtx.put("finAccountTypeId", "STORE_CREDIT_ACCT");
                            createAccountCtx.put("productStoreId", productStore.getString("productStoreId"));
                            createAccountCtx.put("currencyUomId", returnHeader.getString("currencyUomId"));
                            createAccountCtx.put("finAccountName", "Store Credit Account for party ["+fromPartyId+"]");
                            createAccountCtx.put("userLogin", userLogin);
                            Map createAccountResult = null;
                            try {
                                createAccountResult = dispatcher.runSync("createFinAccountForStore", createAccountCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problems running the createFinAccountForStore service", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsCreatingFinAccountForStore", locale));
                            }
                            if (ServiceUtil.isError(createAccountResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountResult));
                            }
                            finAccountId = (String) createAccountResult.get("finAccountId");

                            // double check; make sure we have a FinAccount
                            if (finAccountId == null) {
                                Debug.logError("No available fin account, none was created", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoAvailableFinAccount", locale));
                            }

                            Map finAccountRoleResult = null;
                            try {
                                finAccountRoleResult = dispatcher.runSync("createFinAccountRole", UtilMisc.toMap("finAccountId", finAccountId, "partyId", fromPartyId, "roleTypeId", "OWNER", "userLogin", userLogin));
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problem running the createFinAccountRole service", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingFinAccountRoleRecord", locale));
                            }
                            if (ServiceUtil.isError(finAccountRoleResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(finAccountRoleResult));
                            }
                        }
                    }
                }
            }

            // now; to be used for all timestamps
            Timestamp now = UtilDateTime.nowTimestamp();

            // first, compute the total credit from the return items
            BigDecimal creditTotal = ZERO;
            for (Iterator itemsIter = returnItems.iterator(); itemsIter.hasNext();) {
                GenericValue item = (GenericValue) itemsIter.next();
                BigDecimal quantity = item.getBigDecimal("returnQuantity");
                BigDecimal price = item.getBigDecimal("returnPrice");
                if (quantity == null) quantity = ZERO;
                if (price == null) price = ZERO;
                creditTotal = creditTotal.add(price.multiply(quantity).setScale(decimals, rounding));
            }

            // add the adjustments to the total
            creditTotal = creditTotal.add(adjustments.setScale(decimals, rounding));

            // create finAccountRole and finAccountTrans
            String finAccountTransId = null;
            if (finAccountId != null) {
                Map finAccountTransResult = null;
                try {
                    finAccountTransResult = dispatcher.runSync("createFinAccountTrans", UtilMisc.toMap("finAccountId", finAccountId, "finAccountTransTypeId", "DEPOSIT", "partyId", toPartyId, "amount", creditTotal, "reasonEnumId", "FATR_REFUND", "userLogin", userLogin));
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem creating FinAccountTrans record", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingFinAccountTransRecord", locale));
                }
                if (ServiceUtil.isError(finAccountTransResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(finAccountTransResult));
                }
                finAccountTransId = (String) finAccountTransResult.get("finAccountTransId");
            }

            // create a Payment record for this credit; will look just like a normal payment
            // However, since this payment is not a DISBURSEMENT or RECEIPT but really a matter of internal record
            // it is of type "Other (Non-posting)"
            String paymentId = delegator.getNextSeqId("Payment");
            GenericValue payment = delegator.makeValue("Payment", UtilMisc.toMap("paymentId", paymentId));
            payment.set("paymentTypeId", "CUSTOMER_REFUND");
            payment.set("partyIdFrom", toPartyId);  // if you receive a return FROM someone, then you'd have to give a return TO that person
            payment.set("partyIdTo", fromPartyId);
            payment.set("effectiveDate", now);
            payment.set("amount", creditTotal);
            payment.set("comments", "Return Credit");
            payment.set("statusId", "PMNT_CONFIRMED");  // set the status to confirmed so nothing else can happen to the payment
            if (billingAccountId != null) {
                payment.set("paymentMethodTypeId", "EXT_BILLACT");
            } else {
                payment.set("paymentMethodTypeId", "FIN_ACCOUNT");
            }
            try {
                delegator.create(payment);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem creating Payment record", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingPaymentRecord", locale));
            }

            // create a return item response
            Map itemResponse = UtilMisc.toMap("paymentId", paymentId);
            itemResponse.put("responseAmount", creditTotal);
            itemResponse.put("responseDate", now);
            itemResponse.put("userLogin", userLogin);
            if (billingAccountId != null) {
                itemResponse.put("billingAccountId", billingAccountId);
            } else {
                itemResponse.put("finAccountTransId", finAccountTransId);
            }
            Map serviceResults = null;
            try {
                serviceResults = dispatcher.runSync("createReturnItemResponse", itemResponse);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError("Could not create ReturnItemResponse record", null, null, serviceResults);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem creating ReturnItemResponse record", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingReturnItemResponseRecord", locale));
            }

            // the resulting response ID will be associated with the return items
            String itemResponseId = (String) serviceResults.get("returnItemResponseId");

            // loop through the items again to update them and store a status change history
            for (Iterator itemsIter = returnItems.iterator(); itemsIter.hasNext();) {
                GenericValue item = (GenericValue) itemsIter.next();
                Map returnItemMap = UtilMisc.toMap("returnItemResponseId", itemResponseId, "returnId", item.get("returnId"), "returnItemSeqId", item.get("returnItemSeqId"), "statusId", "RETURN_COMPLETED", "userLogin", userLogin);
                // store the item changes (attached responseId)
                try {
                    serviceResults = dispatcher.runSync("updateReturnItem", returnItemMap);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemStoringReturnItemUpdates", locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem storing ReturnItem updates", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemStoringReturnItemUpdates", locale));
                }
            }

            if (billingAccountId != null) {
                // create the PaymentApplication for the billing account
                String paId = delegator.getNextSeqId("PaymentApplication");
                GenericValue pa = delegator.makeValue("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paId));
                pa.set("paymentId", paymentId);
                pa.set("billingAccountId", billingAccountId);
                pa.set("amountApplied", creditTotal);
                try {
                    delegator.create(pa);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem creating PaymentApplication record for billing account", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingPaymentApplicationRecord", locale));
                }

                // create the payment applications for the return invoice in case of billing account
                try {
                    serviceResults = dispatcher.runSync("createPaymentApplicationsFromReturnItemResponse",
                            UtilMisc.<String, Object>toMap("returnItemResponseId", itemResponseId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingPaymentApplicationRecord", locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem creating PaymentApplication records for return invoice", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingPaymentApplicationRecord", locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }
    
    /**
     * Helper method to get billing account balance, cannot use BillingAccountWorker.getBillingAccountBalance()
     * due to circular build dependency.
     * @param billingAccountId
     * @param dctx
     * @return
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountBalance(String billingAccountId, DispatchContext dctx) throws GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));

        BigDecimal balance = ZERO;
        BigDecimal accountLimit = ZERO;
        if (billingAccount.getBigDecimal("accountLimit") != null) {
            accountLimit = billingAccount.getBigDecimal("accountLimit");
        }
        balance = balance.add(accountLimit);
        // pending (not cancelled, rejected, or received) order payments
        EntityConditionList whereConditions = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("billingAccountId", EntityOperator.EQUALS, billingAccountId),
                EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_BILLACT"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("ORDER_CANCELLED", "ORDER_REJECTED")),
                EntityCondition.makeCondition("preferenceStatusId", EntityOperator.NOT_IN, UtilMisc.toList("PAYMENT_SETTLED", "PAYMENT_RECEIVED", "PAYMENT_DECLINED", "PAYMENT_CANCELLED")) // PAYMENT_NOT_AUTH
           ), EntityOperator.AND);

        List orderPaymentPreferenceSums = delegator.findList("OrderPurchasePaymentSummary", whereConditions, UtilMisc.toSet("maxAmount"), null, null, false);
        for (Iterator oppsi = orderPaymentPreferenceSums.iterator(); oppsi.hasNext();) {
            GenericValue orderPaymentPreferenceSum = (GenericValue) oppsi.next();
            BigDecimal maxAmount = orderPaymentPreferenceSum.getBigDecimal("maxAmount");
            balance = maxAmount != null ? balance.subtract(maxAmount) : balance;
        }

        List paymentAppls = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("billingAccountId", billingAccountId));
        // TODO: cancelled payments?
        for (Iterator pAi = paymentAppls.iterator(); pAi.hasNext();) {
            GenericValue paymentAppl = (GenericValue) pAi.next();
            if (paymentAppl.getString("invoiceId") == null) {
                BigDecimal amountApplied = paymentAppl.getBigDecimal("amountApplied");
                balance = balance.add(amountApplied);
            }
        }

        balance = balance.setScale(decimals, rounding);
        return balance;
    }

    /**
     * Helper method to generate a BillingAccount (store credit) from a return
     * header.  This method takes care of all business logic relating to
     * the initialization of a Billing Account from the Return data.
     *
     * The BillingAccount.thruDate will be set to (now +
     * ProductStore.storeCreditValidDays + end of day).  The product stores
     * are obtained via the return orders, and the minimum storeCreditValidDays
     * will be used.  The default is to set thruDate to null, which implies no
     * expiration.
     *
     * Note that we set BillingAccount.accountLimit to 0.0 for store credits.
     * This is because the available balance of BillingAccounts is
     * calculated as accountLimit + sum of Payments - sum of Invoices.
     */
    private static Map createBillingAccountFromReturn(GenericValue returnHeader, List returnItems, DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {
            // get the related product stores via the orders related to this return
            List orders = EntityUtil.getRelated("OrderHeader", returnItems);
            List productStores = EntityUtil.getRelated("ProductStore", orders);

            // find the minimum storeCreditValidDays of all the ProductStores associated with all the Orders on the Return, skipping null ones
            Long storeCreditValidDays = null;
            for (Iterator iter = productStores.iterator(); iter.hasNext();) {
                GenericValue productStore = (GenericValue) iter.next();
                Long thisStoreValidDays = productStore.getLong("storeCreditValidDays");
                if (thisStoreValidDays == null) continue;

                if (storeCreditValidDays == null) {
                    storeCreditValidDays = thisStoreValidDays;
                } else if (thisStoreValidDays.compareTo(storeCreditValidDays) < 0) {
                    // if this store's days < store credit valid days, use this store's days
                    storeCreditValidDays = thisStoreValidDays;
                }
            }

            // if there is a storeCreditValidDays, set the thruDate to (nowTimestamp + storeCreditValidDays + end of day)
            Timestamp thruDate = null;
            if (storeCreditValidDays != null) thruDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), storeCreditValidDays);

            // create the billing account
            Map input = UtilMisc.toMap("accountLimit", BigDecimal.ZERO, "description", "Credit Account for Return #" + returnHeader.get("returnId"), "userLogin", userLogin);
            input.put("accountCurrencyUomId", returnHeader.get("currencyUomId"));
            input.put("thruDate", thruDate);
            Map results = dispatcher.runSync("createBillingAccount", input);
            if (ServiceUtil.isError(results)) return results;
            String billingAccountId = (String) results.get("billingAccountId");

            // set the role on the account
            input = UtilMisc.toMap("billingAccountId", billingAccountId, "partyId", returnHeader.get("fromPartyId"), "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin);
            Map roleResults = dispatcher.runSync("createBillingAccountRole", input);
            if (ServiceUtil.isError(roleResults)) {
                Debug.logError("Error with createBillingAccountRole: " + roleResults.get(ModelService.ERROR_MESSAGE), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorWithCreateBillingAccountRole", locale) + roleResults.get(ModelService.ERROR_MESSAGE));
            }

            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity error when creating BillingAccount: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsCreatingBillingAccount", locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service error when creating BillingAccount: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsCreatingBillingAccount", locale));
        }
    }

    public static Map processRefundReturnForReplacement(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Map serviceResult = FastMap.newInstance();
        
        GenericValue orderHeader = null;
        List<GenericValue> orderPayPrefs = FastList.newInstance();
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", UtilMisc.toList("-maxAmount"));
        } catch (GenericEntityException e) {
            Debug.logError("Problem looking up order information for orderId #" + orderId, module);
            ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderCannotGetOrderHeader", locale));
        }
         
        // Check for replacement order
        if (UtilValidate.isEmpty(orderPayPrefs)) {
            List<GenericValue> returnItemResponses = FastList.newInstance();
            try {
                returnItemResponses = orderHeader.getRelated("ReplacementReturnItemResponse");
            } catch (GenericEntityException e) {
                Debug.logError("Problem getting ReturnItemResponses", module);
                ServiceUtil.returnError(e.getMessage());
            }
            
            for (GenericValue returnItemResponse : returnItemResponses) {
                GenericValue returnItem = null;
                GenericValue returnHeader = null;
                try {
                    returnItem = EntityUtil.getFirst(returnItemResponse.getRelated("ReturnItem"));
                    returnHeader = returnItem.getRelatedOne("ReturnHeader");
                } catch (GenericEntityException e) {
                    Debug.logError("Problem getting ReturnItem", module);
                    ServiceUtil.returnError(e.getMessage());
                }
                
                if ("RETURN_RECEIVED".equals(returnHeader.getString("statusId"))) {
                    String returnId = returnItem.getString("returnId");
                    String returnTypeId = returnItem.getString("returnTypeId");
                    try {
                        serviceResult = dispatcher.runSync("processRefundReturn", UtilMisc.toMap("returnId", returnId, "returnTypeId", returnTypeId, "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem running the processRefundReturn service", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithTheRefundSeeLogs", locale));
                    }
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        }
        return serviceResult;
    }

    // refund (cash/charge) return
    public static Map processRefundReturn(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String returnId = (String) context.get("returnId");
        String returnTypeId = (String) context.get("returnTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List returnItems = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader != null) {
                returnItems = returnHeader.getRelatedByAnd("ReturnItem", UtilMisc.toMap("returnTypeId", returnTypeId));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap("returnId", returnId, "returnTypeId", returnTypeId));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            Map itemsByOrder = new HashMap();
            Map totalByOrder = new HashMap();

            // make sure total refunds on a return don't exceed amount of returned orders
            Map serviceResult = null;
            try {
                serviceResult = dispatcher.runSync("checkPaymentAmountForRefund", UtilMisc.toMap("returnId", returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem running the checkPaymentAmountForRefund service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithCheckPaymentAmountForRefund", locale));
            }
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }

            groupReturnItemsByOrder(returnItems, itemsByOrder, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            Set itemSet = itemsByOrder.entrySet();
            Iterator itemByOrderIt = itemSet.iterator();
            while (itemByOrderIt.hasNext()) {
                Map.Entry entry = (Map.Entry) itemByOrderIt.next();
                String orderId = (String) entry.getKey();
                List items = (List) entry.getValue();
                BigDecimal orderTotal = (BigDecimal) totalByOrder.get(orderId);

                // get order header & payment prefs
                GenericValue orderHeader = null;
                List orderPayPrefs = null;
                try {
                    orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                    // sort these desending by maxAmount
                    orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", UtilMisc.toList("-maxAmount"));

                    List exprs = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_SETTLED"), EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_RECEIVED"));
                    orderPayPrefs = EntityUtil.filterByOr(orderPayPrefs, exprs);
                    
                    // Check for replacement order
                    if (UtilValidate.isEmpty(orderPayPrefs)) {
                        List<GenericValue> orderItemAssocs = delegator.findByAnd("OrderItemAssoc", UtilMisc.toMap("toOrderId", orderId, "orderItemAssocTypeId", "REPLACEMENT"));
                        if (UtilValidate.isNotEmpty(orderItemAssocs)) {
                            String originalOrderId = EntityUtil.getFirst(orderItemAssocs).getString("orderId");
                            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", originalOrderId));
                            orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", UtilMisc.toList("-maxAmount"));
                            orderPayPrefs = EntityUtil.filterByOr(orderPayPrefs, exprs);
                            orderId = originalOrderId;
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot get Order details for #" + orderId, module);
                    continue;
                }
                OrderReadHelper orderReadHelper = new OrderReadHelper(delegator, orderId);

                // Determine the fall-through refund paymentMethodId from the PartyAcctgPreference of the owner of the productStore for the order
                GenericValue refundPaymentMethod = null;
                GenericValue productStore = orderReadHelper.getProductStore();
                if (UtilValidate.isEmpty(productStore) || UtilValidate.isEmpty(productStore.get("payToPartyId"))) {
                    Debug.logError("No payToPartyId found for orderId " + orderId, module);
                } else {
                    GenericValue orgAcctgPref = null;
                    Map acctgPreferencesResult = null;
                    try {
                        acctgPreferencesResult = dispatcher.runSync("getPartyAccountingPreferences", UtilMisc.toMap("organizationPartyId", productStore.get("payToPartyId"), "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Error retrieving PartyAcctgPreference for partyId " + productStore.get("payToPartyId"), module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithGetPartyAcctgPreferences", locale));
                    }
                    orgAcctgPref = (GenericValue) acctgPreferencesResult.get("partyAccountingPreference");

                    if (UtilValidate.isNotEmpty(orgAcctgPref)) {
                        try {
                            refundPaymentMethod = orgAcctgPref.getRelatedOne("PaymentMethod");
                        } catch (GenericEntityException e) {
                            Debug.logError("Error retrieving related refundPaymentMethod from PartyAcctgPreference for partyId " + productStore.get("payToPartyId"), module);
                        }
                    }
                }

                // now; for all timestamps
                Timestamp now = UtilDateTime.nowTimestamp();

                // Assemble a map of orderPaymentPreferenceId -> list of maps of (OPP and availableAmountForRefunding)
                //     where availableAmountForRefunding = receivedAmount - alreadyRefundedAmount
                // We break the OPPs down this way because we need to process the refunds to payment methods in a particular order
                Map receivedPaymentTotalsByPaymentMethod = orderReadHelper.getReceivedPaymentTotalsByPaymentMethod() ;
                Map refundedTotalsByPaymentMethod = orderReadHelper.getReturnedTotalsByPaymentMethod() ;
                
                // getOrderPaymentPreferenceTotalByType has been called because getReceivedPaymentTotalsByPaymentMethod does not 
                // return payments captured from Billing Account.This is because when payment is captured from Billing Account
                // then no entry is maintained in Payment entity. 
                BigDecimal receivedPaymentTotalsByBillingAccount = orderReadHelper.getOrderPaymentPreferenceTotalByType("EXT_BILLACT");

                /*
                 * Go through the OrderPaymentPreferences and determine how much remains to be refunded for each.
                 * Then group these refund amounts and orderPaymentPreferences by paymentMethodTypeId.  That is,
                 * the intent is to get the refundable amounts per orderPaymentPreference, grouped by payment method type.
                 */
                Map prefSplitMap = new HashMap();
                Iterator oppit = orderPayPrefs.iterator();
                while (oppit.hasNext()) {
                    GenericValue orderPayPref = (GenericValue) oppit.next();
                    String paymentMethodTypeId = orderPayPref.getString("paymentMethodTypeId");
                    String orderPayPrefKey = orderPayPref.getString("paymentMethodId") != null ? orderPayPref.getString("paymentMethodId") : orderPayPref.getString("paymentMethodTypeId");

                    // See how much we can refund to the payment method
                    BigDecimal orderPayPrefReceivedTotal = ZERO;
                    if (receivedPaymentTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add((BigDecimal)receivedPaymentTotalsByPaymentMethod.get(orderPayPrefKey)).setScale(decimals, rounding);
                    }
                    
                    if (receivedPaymentTotalsByBillingAccount != null) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add(receivedPaymentTotalsByBillingAccount);
                    }
                    BigDecimal orderPayPrefRefundedTotal = ZERO;
                    if (refundedTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefRefundedTotal = orderPayPrefRefundedTotal.add((BigDecimal)refundedTotalsByPaymentMethod.get(orderPayPrefKey)).setScale(decimals, rounding);
                    }
                    BigDecimal orderPayPrefAvailableTotal = orderPayPrefReceivedTotal.subtract(orderPayPrefRefundedTotal);

                    // add the refundable amount and orderPaymentPreference to the paymentMethodTypeId map
                    if (orderPayPrefAvailableTotal.compareTo(ZERO) > 0) {
                        Map orderPayPrefDetails = new HashMap();
                        orderPayPrefDetails.put("orderPaymentPreference", orderPayPref);
                        orderPayPrefDetails.put("availableTotal", orderPayPrefAvailableTotal);
                        if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                            ((List) prefSplitMap.get(paymentMethodTypeId)).add(orderPayPrefDetails);
                        } else {
                            prefSplitMap.put(paymentMethodTypeId, UtilMisc.toList(orderPayPrefDetails));
                        }
                    }
                }

                // Keep a decreasing total of the amount remaining to refund
                BigDecimal amountLeftToRefund = orderTotal.setScale(decimals, rounding);

                // This can be extended to support additional electronic types
                List electronicTypes = UtilMisc.toList("CREDIT_CARD", "EFT_ACCOUNT", "FIN_ACCOUNT", "GIFT_CARD");
                
                // Figure out if EXT_PAYPAL should be considered as an electronic type
                if (productStore != null) {
                    ExpressCheckoutEvents.CheckoutType payPalType = ExpressCheckoutEvents.determineCheckoutType(delegator, productStore.getString("productStoreId"));
                    if (!payPalType.equals(ExpressCheckoutEvents.CheckoutType.NONE)) {
                        electronicTypes.add("EXT_PAYPAL");
                    }
                }
                // This defines the ordered part of the sequence of refund processing
                List orderedRefundPaymentMethodTypes = new ArrayList();
                orderedRefundPaymentMethodTypes.add("EXT_BILLACT");
                orderedRefundPaymentMethodTypes.add("FIN_ACCOUNT");
                orderedRefundPaymentMethodTypes.add("GIFT_CARD");
                orderedRefundPaymentMethodTypes.add("CREDIT_CARD");
                orderedRefundPaymentMethodTypes.add("EFT_ACCOUNT");

                // Add all the other paymentMethodTypes, in no particular order
                EntityConditionList pmtConditionList = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.NOT_IN, orderedRefundPaymentMethodTypes)), EntityOperator.AND);
                List otherPaymentMethodTypes = new ArrayList();
                try {
                    otherPaymentMethodTypes = delegator.findList("PaymentMethodType", pmtConditionList, null, null, null, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot get PaymentMethodTypes", module);
                    return ServiceUtil.returnError("Problems getting PaymentMethodTypes: " + e.toString());
                }
                orderedRefundPaymentMethodTypes.addAll(EntityUtil.getFieldListFromEntityList(otherPaymentMethodTypes, "paymentMethodTypeId", true));

                // Iterate through the specified sequence of paymentMethodTypes, refunding to the correct OrderPaymentPreferences
                //    as long as there's a positive amount remaining to refund
                Iterator orpmtit = orderedRefundPaymentMethodTypes.iterator();
                while (orpmtit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                    String paymentMethodTypeId = (String) orpmtit.next();
                    if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                        List paymentMethodDetails = (List) prefSplitMap.get(paymentMethodTypeId);

                        // Iterate through the OrderPaymentPreferences of this type
                        Iterator pmtppit = paymentMethodDetails.iterator();
                        while (pmtppit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                            Map orderPaymentPrefDetails = (Map) pmtppit.next();
                            GenericValue orderPaymentPreference = (GenericValue) orderPaymentPrefDetails.get("orderPaymentPreference");
                            BigDecimal orderPaymentPreferenceAvailable = (BigDecimal) orderPaymentPrefDetails.get("availableTotal");

                            // Refund up to the maxAmount for the paymentPref, or whatever is left to refund if that's less than the maxAmount
                            BigDecimal amountToRefund = orderPaymentPreferenceAvailable.min(amountLeftToRefund);
                            // The amount actually refunded for the paymentPref, default to requested amount
                            BigDecimal amountRefunded = amountToRefund;

                            String paymentId = null;
                            String returnItemStatusId = "RETURN_COMPLETED";  // generally, the return item will be considered complete after this
                            // Call the refund service to refund the payment
                            if (electronicTypes.contains(paymentMethodTypeId)) {
                                try {
                                    // for electronic types such as CREDIT_CARD and EFT_ACCOUNT, use refundPayment service
                                    serviceResult = dispatcher.runSync("refundPayment", UtilMisc.<String, Object>toMap("orderPaymentPreference", orderPaymentPreference, "refundAmount", amountToRefund.setScale(decimals, rounding), "userLogin", userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError("Error in refund payment: " + ServiceUtil.getErrorMessage(serviceResult), module);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get("paymentId");
                                    amountRefunded = (BigDecimal) serviceResult.get("refundAmount");
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, "Problem running the refundPayment service", module);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithTheRefundSeeLogs", locale));
                                }
                            } else if (paymentMethodTypeId.equals("EXT_BILLACT")) {
                                try {
                                    // for Billing Account refunds
                                    serviceResult = dispatcher.runSync("refundBillingAccountPayment", UtilMisc.<String, Object>toMap("orderPaymentPreference", orderPaymentPreference, "refundAmount", amountToRefund.setScale(decimals, rounding), "userLogin", userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError("Error in refund payment: " + ServiceUtil.getErrorMessage(serviceResult), module);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get("paymentId");
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, "Problem running the refundPayment service", module);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithTheRefundSeeLogs", locale));
                                }
                            } else {
                                // handle manual refunds
                                try {
                                    Map input = UtilMisc.toMap("userLogin", userLogin, "amount", amountLeftToRefund, "statusId", "PMNT_NOT_PAID");
                                    input.put("partyIdTo", returnHeader.get("fromPartyId"));
                                    input.put("partyIdFrom", returnHeader.get("toPartyId"));
                                    input.put("paymentTypeId", "CUSTOMER_REFUND");
                                    input.put("paymentMethodId", orderPaymentPreference.get("paymentMethodId"));
                                    input.put("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
                                    input.put("paymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));

                                    serviceResult = dispatcher.runSync("createPayment", input);

                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError("Error in refund payment: " + ServiceUtil.getErrorMessage(serviceResult), module);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get("paymentId");
                                    returnItemStatusId = "RETURN_MAN_REFUND";    // however, in this case we should flag it as a manual refund
                                } catch (GenericServiceException e) {
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                            }

                            // Fill out the data for the new ReturnItemResponse
                            Map response = FastMap.newInstance();
                            response.put("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                            response.put("responseAmount", amountRefunded.setScale(decimals, rounding));
                            response.put("responseDate", now);
                            response.put("userLogin", userLogin);
                            response.put("paymentId", paymentId);
                            if (paymentMethodTypeId.equals("EXT_BILLACT")) {
                                response.put("billingAccountId", orderReadHelper.getBillingAccount().getString("billingAccountId"));
                            }
                            Map serviceResults = null;
                            try {
                                serviceResults = dispatcher.runSync("createReturnItemResponse", response);
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsCreatingReturnItemResponseEntity", locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problems creating new ReturnItemResponse entity", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsCreatingReturnItemResponseEntity", locale));
                            }
                            String responseId = (String) serviceResults.get("returnItemResponseId");

                            // Set the response on each item
                            Iterator itemsIter = items.iterator();
                            while (itemsIter.hasNext()) {
                                GenericValue item = (GenericValue) itemsIter.next();
                                
                                Map returnItemMap = UtilMisc.toMap("returnItemResponseId", responseId, "returnId", item.get("returnId"), "returnItemSeqId", item.get("returnItemSeqId"), "statusId", returnItemStatusId, "userLogin", userLogin);
                                //Debug.log("Updating item status", module);
                                try {
                                    serviceResults = dispatcher.runSync("updateReturnItem", returnItemMap);
                                    if (ServiceUtil.isError(serviceResults)) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemUpdatingReturnItemReturnItemResponseId", locale), null, null, serviceResults);
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError("Problem updating the ReturnItem entity", module);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemUpdatingReturnItemReturnItemResponseId", locale));
                                }

                                //Debug.log("Item status and return status history created", module);
                            }

                            // Create the payment applications for the return invoice
                            try {
                                serviceResults = dispatcher.runSync("createPaymentApplicationsFromReturnItemResponse",
                                        UtilMisc.<String, Object>toMap("returnItemResponseId", responseId, "userLogin", userLogin));
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemUpdatingReturnItemReturnItemResponseId", locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problem creating PaymentApplication records for return invoice", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemUpdatingReturnItemReturnItemResponseId", locale));
                            }

                            // Update the amount necessary to refund
                            amountLeftToRefund = amountLeftToRefund.subtract(amountRefunded);
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map refundBillingAccountPayment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal refundAmount = (BigDecimal) context.get("refundAmount");

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne("OrderHeader");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader from OrderPaymentPreference", module);
            return ServiceUtil.returnError("Problems getting OrderHeader from OrderPaymentPreference: " + e.toString());
        }

        OrderReadHelper orh = new OrderReadHelper(orderHeader);

        String payFromPartyId = orh.getBillFromParty().getString("partyId");
        String payToPartyId = orh.getBillToParty().getString("partyId");

        // Create the PaymentGatewayResponse record
        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
        GenericValue response = delegator.makeValue("PaymentGatewayResponse");
        response.set("paymentGatewayResponseId", responseId);
        response.set("paymentServiceTypeEnumId", "PRDS_PAY_REFUND");
        response.set("orderPaymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        response.set("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        response.set("transCodeEnumId", "PGT_REFUND");
        response.set("amount", refundAmount);
        response.set("transactionDate", UtilDateTime.nowTimestamp());
        response.set("currencyUomId", orh.getCurrency());
        try {
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to create PaymentGatewayResponse record");
        }

        // Create the Payment record (parties reversed)
        Map paymentCtx = UtilMisc.toMap("paymentTypeId", "CUSTOMER_REFUND");
        paymentCtx.put("paymentMethodTypeId", paymentPref.get("paymentMethodTypeId"));
        paymentCtx.put("paymentGatewayResponseId", responseId);
        paymentCtx.put("partyIdTo", payToPartyId);
        paymentCtx.put("partyIdFrom", payFromPartyId);
        paymentCtx.put("statusId", "PMNT_CONFIRMED");
        paymentCtx.put("paymentPreferenceId", paymentPref.get("orderPaymentPreferenceId"));
        paymentCtx.put("currencyUomId", orh.getCurrency());
        paymentCtx.put("amount", refundAmount);
        paymentCtx.put("userLogin", userLogin);
        paymentCtx.put("comments", "Refund");

        String paymentId = null;
        try {
            Map paymentCreationResult = dispatcher.runSync("createPayment", paymentCtx);
            if (ServiceUtil.isError(paymentCreationResult)) {
                return paymentCreationResult;
            } else {
                paymentId = (String) paymentCreationResult.get("paymentId");
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("Problem creating Payment " + e.getMessage());
        }

        if (paymentId == null) {
            return ServiceUtil.returnError("Create payment failed");
        }

        // if the original order was paid with a billing account, then go find the billing account from the order and associate this refund with that billing account
        // thus returning value to the billing account
        if ("EXT_BILLACT".equals(paymentPref.getString("paymentMethodTypeId"))) {
            GenericValue billingAccount = orh.getBillingAccount();
            if (UtilValidate.isNotEmpty(billingAccount.getString("billingAccountId"))) {
                try {
                    Map paymentApplResult = dispatcher.runSync("createPaymentApplication", UtilMisc.<String, Object>toMap("paymentId", paymentId, "billingAccountId", billingAccount.getString("billingAccountId"),
                                "amountApplied", refundAmount, "userLogin", userLogin));
                    if (ServiceUtil.isError(paymentApplResult)) {
                        return paymentApplResult;
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError("Problem creating PaymentApplication: " + e.getMessage());
                }
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("paymentId", paymentId);
        return result;
    }

    public static Map createPaymentApplicationsFromReturnItemResponse(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // the strategy for this service is to get a list of return invoices via the return items -> return item billing relationships
        // then split up the responseAmount among the invoices evenly
        String responseId = (String) context.get("returnItemResponseId");
        String errorMsg = "Failed to create payment applications for return item response [" + responseId + "]. ";
        try {
            GenericValue response = delegator.findByPrimaryKey("ReturnItemResponse", UtilMisc.toMap("returnItemResponseId", responseId));
            if (response == null) {
                return ServiceUtil.returnError(errorMsg + "Return Item Response not found with ID [" + responseId + "].");
            }
            BigDecimal responseAmount = response.getBigDecimal("responseAmount").setScale(decimals, rounding);
            String paymentId = response.getString("paymentId");

            // for each return item in the response, get the list of return item billings and then a list of invoices
            Map returnInvoices = FastMap.newInstance(); // key is invoiceId, value is Invoice GenericValue
            List items = response.getRelated("ReturnItem");
            for (Iterator itemIter = items.iterator(); itemIter.hasNext();) {
                GenericValue item = (GenericValue) itemIter.next();
                List billings = item.getRelated("ReturnItemBilling");
                for (Iterator billIter = billings.iterator(); billIter.hasNext();) {
                    GenericValue billing = (GenericValue) billIter.next();
                    GenericValue invoice = billing.getRelatedOne("Invoice");

                    // put the invoice in the map if it doesn't already exist (a very loopy way of doing group by invoiceId without creating a view)
                    if (returnInvoices.get(invoice.getString("invoiceId")) == null) {
                        returnInvoices.put(invoice.getString("invoiceId"), invoice);
                    }
                }
            }

            // for each return invoice found, sum up the related billings
            Map invoiceTotals = FastMap.newInstance(); // key is invoiceId, value is the sum of all billings for that invoice
            BigDecimal grandTotal = ZERO; // The sum of all return invoice totals
            for (Iterator iter = returnInvoices.values().iterator(); iter.hasNext();) {
                GenericValue invoice = (GenericValue) iter.next();

                List billings = invoice.getRelated("ReturnItemBilling");
                BigDecimal runningTotal = ZERO;
                for (Iterator billIter = billings.iterator(); billIter.hasNext();) {
                    GenericValue billing = (GenericValue) billIter.next();
                    runningTotal = runningTotal.add(billing.getBigDecimal("amount").multiply(billing.getBigDecimal("quantity")).setScale(decimals, rounding));
                }

                invoiceTotals.put(invoice.getString("invoiceId"), runningTotal);
                grandTotal = grandTotal.add(runningTotal);
            }

            // now allocate responseAmount * invoiceTotal / grandTotal to each invoice
            for (Iterator iter = returnInvoices.values().iterator(); iter.hasNext();) {
                GenericValue invoice = (GenericValue) iter.next();
                String invoiceId = invoice.getString("invoiceId");
                BigDecimal invoiceTotal = (BigDecimal) invoiceTotals.get(invoiceId);

                BigDecimal amountApplied = responseAmount.multiply(invoiceTotal).divide(grandTotal, decimals, rounding).setScale(decimals, rounding);

                if (paymentId != null) {
                    // create a payment application for the invoice
                    Map input = UtilMisc.toMap("paymentId", paymentId, "invoiceId", invoice.getString("invoiceId"));
                    input.put("amountApplied", amountApplied);
                    input.put("userLogin", userLogin);
                    if (response.get("billingAccountId") != null) {
                        GenericValue billingAccount = response.getRelatedOne("BillingAccount");
                        if (billingAccount != null) {
                            input.put("billingAccountId", response.get("billingAccountId"));
                        }
                    }
                    Map serviceResults = dispatcher.runSync("createPaymentApplication", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                    }
                    if (Debug.verboseOn()) { Debug.logInfo("Created PaymentApplication for response with amountApplied " + amountApplied.toString(), module); }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    // replacement return (create new order adjusted to be at no charge)
    public static Map processReplacementReturn(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        String returnTypeId = (String) context.get("returnTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        GenericValue returnHeader = null;
        List returnItems = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader != null) {
                returnItems = returnHeader.getRelatedByAnd("ReturnItem", UtilMisc.toMap("returnTypeId", returnTypeId));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorGettingReturnHeaderItemInformation", locale));
        }
        String returnHeaderTypeId = returnHeader.getString("returnHeaderTypeId");
        List createdOrderIds = new ArrayList();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            Map returnItemsByOrderId = new HashMap();
            Map totalByOrder = new HashMap();
            groupReturnItemsByOrder(returnItems, returnItemsByOrderId, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            Set returnItemsByOrderIdEntrySet = returnItemsByOrderId.entrySet();
            Iterator returnItemsByOrderIdEntryIter = returnItemsByOrderIdEntrySet.iterator();
            while (returnItemsByOrderIdEntryIter.hasNext()) {
                Map.Entry entry = (Map.Entry) returnItemsByOrderIdEntryIter.next();
                String orderId = (String) entry.getKey();
                List returnItemList = (List) entry.getValue();

                // get order header & payment prefs
                GenericValue orderHeader = null;
                try {
                    orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot get Order details for #" + orderId, module);
                    continue;
                }

                OrderReadHelper orh = new OrderReadHelper(orderHeader);

                // create the replacement order
                Map orderMap = UtilMisc.toMap("userLogin", userLogin);

                String placingPartyId = null;
                GenericValue placingParty = null;
                if ("CUSTOMER_RETURN".equals(returnHeaderTypeId)) {
                    placingParty = orh.getPlacingParty();
                    if (placingParty != null) {
                        placingPartyId = placingParty.getString("partyId");
                    }
                    orderMap.put("orderTypeId", "SALES_ORDER");
                } else {
                    placingParty = orh.getSupplierAgent();
                    if (placingParty != null) {
                        placingPartyId = placingParty.getString("partyId");
                    }
                    orderMap.put("orderTypeId", "PURCHASE_ORDER");
                }
                orderMap.put("partyId", placingPartyId);
                orderMap.put("productStoreId", orderHeader.get("productStoreId"));
                orderMap.put("webSiteId", orderHeader.get("webSiteId"));
                orderMap.put("visitId", orderHeader.get("visitId"));
                orderMap.put("currencyUom", orderHeader.get("currencyUom"));
                orderMap.put("grandTotal",  BigDecimal.ZERO);

                // make the contact mechs
                List contactMechs = new ArrayList();
                List orderCm = null;
                try {
                    orderCm = orderHeader.getRelated("OrderContactMech");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (orderCm != null) {
                    Iterator orderCmi = orderCm.iterator();
                    while (orderCmi.hasNext()) {
                        GenericValue v = (GenericValue) orderCmi.next();
                        contactMechs.add(GenericValue.create(v));
                    }
                    orderMap.put("orderContactMechs", contactMechs);
                }

                // make the shipment prefs
                /*
                 * OrderShipmentPreference is a deprecated entity
                List shipmentPrefs = new ArrayList();
                List orderSp = null;
                try {
                    orderSp = orderHeader.getRelated("OrderShipmentPreference");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (orderSp != null) {
                    Iterator orderSpi = orderSp.iterator();
                    while (orderSpi.hasNext()) {
                        GenericValue v = (GenericValue) orderSpi.next();
                        shipmentPrefs.add(GenericValue.create(v));
                    }
                    orderMap.put("orderShipmentPreferences", shipmentPrefs);
                }
                 */

                // make the order items
                BigDecimal orderPriceTotal = BigDecimal.ZERO;
                BigDecimal additionalItemTotal = BigDecimal.ZERO;
                List orderItems = new ArrayList();
                List orderItemShipGroupInfo = new ArrayList();
                List orderItemShipGroupIds = new ArrayList(); // this is used to store the ship group ids of the groups already added to the orderItemShipGroupInfo list
                List orderItemAssocs = new ArrayList();
                if (returnItemList != null) {
                    Iterator returnItemIter = returnItemList.iterator();
                    int itemCount = 1;
                    while (returnItemIter.hasNext()) {
                        GenericValue returnItem = (GenericValue) returnItemIter.next();
                        GenericValue orderItem = null;
                        GenericValue product = null;
                        try {
                            orderItem = returnItem.getRelatedOne("OrderItem");
                            product = orderItem.getRelatedOne("Product");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            continue;
                        }
                        if (orderItem != null) {
                            BigDecimal quantity = returnItem.getBigDecimal("returnQuantity");
                            BigDecimal unitPrice = returnItem.getBigDecimal("returnPrice");
                            if (quantity != null && unitPrice != null) {
                                orderPriceTotal = orderPriceTotal.add(quantity.multiply(unitPrice));
                                // Check if the product being returned has a Refurbished Equivalent and if so
                                // (and there is inventory for the assoc product) use that product instead
                                GenericValue refurbItem = null;
                                if ("CUSTOMER_RETURN".equals(returnHeaderTypeId)) {
                                    try {
                                        if (UtilValidate.isNotEmpty(product)) {
                                            GenericValue refurbItemAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(product.getRelated("MainProductAssoc",
                                                                                                       UtilMisc.toMap("productAssocTypeId", "PRODUCT_REFURB"),
                                                                                                       UtilMisc.toList("sequenceNum"))));
                                            if (UtilValidate.isNotEmpty(refurbItemAssoc)) {
                                                refurbItem = refurbItemAssoc.getRelatedOne("AssocProduct");
                                            }
                                        }
                                    } catch (GenericEntityException e) {
                                        Debug.logError(e, module);
                                    }
                                    if (UtilValidate.isNotEmpty(refurbItem)) {
                                        boolean inventoryAvailable = false;
                                        try {
                                            Map invReqResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.toMap("productStoreId", orderHeader.get("productStoreId"),
                                                                                                                                           "productId", refurbItem.getString("productId"),
                                                                                                                                           "product", refurbItem, "quantity", quantity));
                                            if (ServiceUtil.isError(invReqResult)) {
                                                Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invReqResult, module);
                                            } else {
                                                inventoryAvailable = "Y".equals((String) invReqResult.get("available"));
                                            }
                                        } catch (GenericServiceException e) {
                                            Debug.logError(e, "Fatal error calling inventory checking services: " + e.toString(), module);
                                        }
                                        if (!inventoryAvailable) {
                                            // If the Refurbished Equivalent is not available,
                                            // then use the original product.
                                            refurbItem = null;
                                        }
                                    }
                                }

                                GenericValue newItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", UtilFormatOut.formatPaddedNumber(itemCount++, 5)));
                                if (UtilValidate.isEmpty(refurbItem)) {
                                    newItem.set("productId", orderItem.get("productId"));
                                    newItem.set("itemDescription", orderItem.get("itemDescription"));
                                } else {
                                    newItem.set("productId", refurbItem.get("productId"));
                                    newItem.set("itemDescription", ProductContentWrapper.getProductContentAsText(refurbItem, "PRODUCT_NAME", locale, null));
                                }
                                newItem.set("orderItemTypeId", orderItem.get("orderItemTypeId"));
                                newItem.set("productFeatureId", orderItem.get("productFeatureId"));
                                newItem.set("prodCatalogId", orderItem.get("prodCatalogId"));
                                newItem.set("productCategoryId", orderItem.get("productCategoryId"));
                                newItem.set("quantity", quantity);
                                newItem.set("unitPrice", unitPrice);
                                newItem.set("unitListPrice", orderItem.get("unitListPrice"));
                                newItem.set("comments", orderItem.get("comments"));
                                newItem.set("correspondingPoId", orderItem.get("correspondingPoId"));
                                newItem.set("statusId", "ITEM_CREATED");
                                orderItems.add(newItem);

                                // Set the order item ship group information
                                // TODO: only the first ship group associated to the item
                                //       of the original order is considered and cloned,
                                //       anIs there a better way to handle this?d the returned units are assigned to it.
                                //
                                GenericValue orderItemShipGroupAssoc = null;
                                try {
                                    orderItemShipGroupAssoc = EntityUtil.getFirst(orderItem.getRelated("OrderItemShipGroupAssoc"));
                                    if (orderItemShipGroupAssoc != null) {
                                        if (!orderItemShipGroupIds.contains(orderItemShipGroupAssoc.getString("shipGroupSeqId"))) {
                                            GenericValue orderItemShipGroup = orderItemShipGroupAssoc.getRelatedOne("OrderItemShipGroup");
                                            GenericValue newOrderItemShipGroup = (GenericValue)orderItemShipGroup.clone();
                                            newOrderItemShipGroup.set("orderId", null);
                                            orderItemShipGroupInfo.add(newOrderItemShipGroup);
                                            orderItemShipGroupIds.add(orderItemShipGroupAssoc.getString("shipGroupSeqId"));
                                        }
                                        GenericValue newOrderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderItemSeqId", newItem.getString("orderItemSeqId"), "shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"), "quantity", quantity));
                                        orderItemShipGroupInfo.add(newOrderItemShipGroupAssoc);
                                    }
                                } catch (GenericEntityException gee) {
                                    Debug.logError(gee, module);
                                }
                                // Create an association between the replacement order item and the order item of the original order
                                GenericValue newOrderItemAssoc = delegator.makeValue("OrderItemAssoc", UtilMisc.toMap("orderId", orderHeader.getString("orderId"),
                                        "orderItemSeqId", orderItem.getString("orderItemSeqId"), "shipGroupSeqId", "_NA_",
                                        "toOrderItemSeqId", newItem.getString("orderItemSeqId"), "toShipGroupSeqId", "_NA_", "orderItemAssocTypeId", "REPLACEMENT"));
                                orderItemAssocs.add(newOrderItemAssoc);

                                // For repair replacement orders, add to the order also the repair items
                                if ("RTN_REPAIR_REPLACE".equals(returnTypeId)) {
                                    List repairItems = null;
                                    try {
                                        if (UtilValidate.isNotEmpty(product)) {
                                            repairItems = EntityUtil.filterByDate(product.getRelated("MainProductAssoc",
                                                    UtilMisc.toMap("productAssocTypeId", "PRODUCT_REPAIR_SRV"), UtilMisc.toList("sequenceNum")));
                                        }
                                    } catch (GenericEntityException e) {
                                        Debug.logError(e, module);
                                        continue;
                                    }
                                    if (UtilValidate.isNotEmpty(repairItems)) {
                                        Iterator repairItemIt = repairItems.iterator();
                                        while (repairItemIt.hasNext()) {
                                            GenericValue repairItem = (GenericValue)repairItemIt.next();
                                            GenericValue repairItemProduct = null;
                                            try {
                                                repairItemProduct = repairItem.getRelatedOne("AssocProduct");
                                            } catch (GenericEntityException e) {
                                                Debug.logError(e, module);
                                                continue;
                                            }
                                            if (UtilValidate.isNotEmpty(repairItemProduct)) {
                                                BigDecimal repairUnitQuantity = repairItem.getBigDecimal("quantity");
                                                if (UtilValidate.isEmpty(repairUnitQuantity)) {
                                                    repairUnitQuantity = BigDecimal.ONE;
                                                }
                                                BigDecimal repairQuantity = quantity.multiply(repairUnitQuantity);
                                                newItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", UtilFormatOut.formatPaddedNumber(itemCount++, 5)));

                                                // price
                                                Map priceContext = FastMap.newInstance();
                                                priceContext.put("currencyUomId", orderHeader.get("currencyUom"));
                                                if (placingPartyId != null) {
                                                    priceContext.put("partyId", placingPartyId);
                                                }
                                                priceContext.put("quantity", repairUnitQuantity);
                                                priceContext.put("product", repairItemProduct);
                                                priceContext.put("webSiteId", orderHeader.get("webSiteId"));
                                                priceContext.put("productStoreId", orderHeader.get("productStoreId"));
                                                // TODO: prodCatalogId, agreementId
                                                priceContext.put("productPricePurposeId", "PURCHASE");
                                                priceContext.put("checkIncludeVat", "Y");
                                                Map priceResult = null;
                                                try {
                                                    priceResult = dispatcher.runSync("calculateProductPrice", priceContext);
                                                } catch (GenericServiceException gse) {
                                                    Debug.logError(gse, module);
                                                    continue;
                                                }
                                                if (ServiceUtil.isError(priceResult)) {
                                                    Debug.logError(ServiceUtil.getErrorMessage(priceResult), module);
                                                    continue;
                                                }
                                                Boolean validPriceFound = (Boolean) priceResult.get("validPriceFound");
                                                if (Boolean.FALSE.equals(validPriceFound)) {
                                                    Debug.logError("Could not find a valid price for the product with ID [" + repairItemProduct.get("productId") + "].", module);
                                                    continue;
                                                }

                                                if (priceResult.get("listPrice") != null) {
                                                    newItem.set("unitListPrice", (BigDecimal)priceResult.get("listPrice"));
                                                }

                                                BigDecimal repairUnitPrice = null;
                                                if (priceResult.get("basePrice") != null) {
                                                    repairUnitPrice = (BigDecimal)priceResult.get("basePrice");
                                                } else {
                                                    repairUnitPrice = BigDecimal.ZERO;
                                                }
                                                newItem.set("unitPrice", repairUnitPrice);

                                                newItem.set("productId", repairItemProduct.get("productId"));
                                                // TODO: orderItemTypeId, prodCatalogId, productCategoryId
                                                newItem.set("quantity", repairQuantity);
                                                newItem.set("itemDescription", ProductContentWrapper.getProductContentAsText(repairItemProduct, "PRODUCT_NAME", locale, null));
                                                newItem.set("statusId", "ITEM_CREATED");
                                                orderItems.add(newItem);
                                                additionalItemTotal = additionalItemTotal.add(repairQuantity.multiply(repairUnitPrice));
                                                if (UtilValidate.isNotEmpty(orderItemShipGroupAssoc)) {
                                                    GenericValue newOrderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderItemSeqId", newItem.getString("orderItemSeqId"), "shipGroupSeqId", orderItemShipGroupAssoc.getString("shipGroupSeqId"), "quantity", repairQuantity));
                                                    orderItemShipGroupInfo.add(newOrderItemShipGroupAssoc);
                                                }
                                                // Create an association between the repair order item and the order item of the original order
                                                newOrderItemAssoc = delegator.makeValue("OrderItemAssoc", UtilMisc.toMap("orderId", orderHeader.getString("orderId"),
                                                        "orderItemSeqId", orderItem.getString("orderItemSeqId"), "shipGroupSeqId", "_NA_",
                                                        "toOrderItemSeqId", newItem.getString("orderItemSeqId"), "toShipGroupSeqId", "_NA_", "orderItemAssocTypeId", "REPLACEMENT"));
                                                orderItemAssocs.add(newOrderItemAssoc);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    orderMap.put("orderItems", orderItems);
                    if (orderItemShipGroupInfo.size() > 0) {
                        orderMap.put("orderItemShipGroupInfo", orderItemShipGroupInfo);
                    }
                    if (orderItemAssocs.size() > 0) {
                        orderMap.put("orderItemAssociations", orderItemAssocs);
                    }
                } else {
                    Debug.logError("No return items found??", module);
                    continue;
                }

                // create the replacement adjustment
                GenericValue adj = delegator.makeValue("OrderAdjustment");
                adj.set("orderAdjustmentTypeId", "REPLACE_ADJUSTMENT");
                adj.set("amount", orderPriceTotal.negate());
                adj.set("comments", "Replacement Item Return #" + returnId);
                adj.set("createdDate", nowTimestamp);
                adj.set("createdByUserLogin", userLogin.getString("userLoginId"));
                orderMap.put("orderAdjustments", UtilMisc.toList(adj));

                // Payment preference
                if ((additionalItemTotal.compareTo(BigDecimal.ZERO) > 0) || ("RTN_CSREPLACE".equals(returnTypeId) && orderPriceTotal.compareTo(ZERO) > 0)) {
                    GenericValue paymentMethod = null;
                    try {
                        paymentMethod = returnHeader.getRelatedOne("PaymentMethod");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                    if (UtilValidate.isNotEmpty(paymentMethod)) {
                        String paymentMethodId = paymentMethod.getString("paymentMethodId");
                        String paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
                        GenericValue opp = delegator.makeValue("OrderPaymentPreference", new HashMap());
                        opp.set("paymentMethodTypeId", paymentMethodTypeId);
                        opp.set("paymentMethodId", paymentMethodId);
                        // TODO: manualRefNum, manualAuthCode, securityCode, presentFlag, overflowFlag
                        if (paymentMethodId != null || "FIN_ACCOUNT".equals(paymentMethodTypeId)) {
                            opp.set("statusId", "PAYMENT_NOT_AUTH");
                        } else if (paymentMethodTypeId != null) {
                            // external payment method types require notification when received
                            // internal payment method types are assumed to be in-hand
                            if (paymentMethodTypeId.startsWith("EXT_")) {
                                opp.set("statusId", "PAYMENT_NOT_RECEIVED");
                            } else {
                                opp.set("statusId", "PAYMENT_RECEIVED");
                            }
                        }
                        if ("RTN_CSREPLACE".equals(returnTypeId)) {
                            opp.set("maxAmount", orderPriceTotal);
                        }
                        orderMap.put("orderPaymentInfo", UtilMisc.toList(opp));
                    }
                }

                // we'll assume new order is under same terms as original.  note orderTerms is a required parameter of storeOrder
                try {
                    orderMap.put("orderTerms", orderHeader.getRelated("OrderTerm"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot create replacement order because order terms for original order are not available", module);
                }
                // we'll assume the new order has the same order roles of the original one
                try {
                    List orderRoles = orderHeader.getRelated("OrderRole");
                    Map orderRolesMap = FastMap.newInstance();
                    if (orderRoles != null) {
                        Iterator orderRolesIt = orderRoles.iterator();
                        while (orderRolesIt.hasNext()) {
                            GenericValue orderRole = (GenericValue) orderRolesIt.next();
                            List parties = (List) orderRolesMap.get(orderRole.getString("roleTypeId"));
                            if (parties == null) {
                                parties = FastList.newInstance();
                                orderRolesMap.put(orderRole.getString("roleTypeId"), parties);
                            }
                            parties.add(orderRole.getString("partyId"));
                        }
                    }
                    if (orderRolesMap.size() > 0) {
                        orderMap.put("orderAdditionalPartyRoleMap", orderRolesMap);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot create replacement order because order roles for original order are not available", module);
                }

                // create the order
                String createdOrderId = null;
                Map orderResult = null;
                try {
                    orderResult = dispatcher.runSync("storeOrder", orderMap);
                } catch (GenericServiceException e) {
                    Debug.logInfo(e, "Problem creating the order!", module);
                }
                if (orderResult != null) {
                    createdOrderId = (String) orderResult.get("orderId");
                    createdOrderIds.add(createdOrderId);
                }

                // since there is no payments required; order is ready for processing/shipment
                if (createdOrderId != null) {
                    if ("RETURN_ACCEPTED".equals(returnHeader.get("statusId")) && "RTN_WAIT_REPLACE_RES".equals(returnTypeId)) {
                        Map serviceResult = null;
                        try {
                            serviceResult = dispatcher.runSync("changeOrderStatus", UtilMisc.toMap("orderId", createdOrderId, "statusId", "ORDER_HOLD", "userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Service invocation error, status changes were not updated for order #" + createdOrderId, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (ServiceUtil.isError(serviceResult)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                        }                        
                    } else {
                        OrderChangeHelper.approveOrder(dispatcher, userLogin, createdOrderId);
                    }

                    // create a ReturnItemResponse and attach to each ReturnItem
                    Map itemResponse = FastMap.newInstance();
                    itemResponse.put("replacementOrderId", createdOrderId);
                    itemResponse.put("responseAmount", orderPriceTotal);
                    itemResponse.put("responseDate", nowTimestamp);
                    itemResponse.put("userLogin", userLogin);
                    String returnItemResponseId = null;
                    try {
                        Map createReturnItemResponseResult = dispatcher.runSync("createReturnItemResponse", itemResponse);
                        if (ServiceUtil.isError(createReturnItemResponseResult)) {
                            return ServiceUtil.returnError("Could not create ReturnItemResponse record", null, null, createReturnItemResponseResult);
                        }
                        returnItemResponseId = (String) createReturnItemResponseResult.get("returnItemResponseId");
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem creating ReturnItemResponse record", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemCreatingReturnItemResponseRecord", locale));
                    }

                    Iterator updateReturnItemIter = returnItemList.iterator();
                    while (updateReturnItemIter.hasNext()) {
                        GenericValue returnItem = (GenericValue) updateReturnItemIter.next();
                        Map updateReturnItemCtx = FastMap.newInstance();
                        updateReturnItemCtx.put("returnId", returnId);
                        updateReturnItemCtx.put("returnItemSeqId", returnItem.get("returnItemSeqId"));
                        updateReturnItemCtx.put("returnItemResponseId", returnItemResponseId);
                        updateReturnItemCtx.put("userLogin", userLogin);
                        try {
                            Map updateReturnItemResult = dispatcher.runSync("updateReturnItem", updateReturnItemCtx);
                            if (ServiceUtil.isError(updateReturnItemResult)) {
                                return ServiceUtil.returnError("Could not update ReturnItem record", null, null, updateReturnItemResult);
                            }
                        } catch (GenericServiceException e) {
                            String errMsg = "Could not update ReturnItem record";
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
                    }
                }
            }
        }

        // create a return message AND create ReturnItemResponse record(s)
        StringBuilder successMessage = new StringBuilder();
        if (createdOrderIds.size() > 0) {
            successMessage.append("The following new orders have been created : ");
            Iterator i = createdOrderIds.iterator();
            while (i.hasNext()) {
                successMessage.append(i.next());
                if (i.hasNext()) {
                    successMessage.append(", ");
                }
            }
        } else {
            successMessage.append("No orders were created.");
        }

        return ServiceUtil.returnSuccess(successMessage.toString());
    }

    public static Map processSubscriptionReturn(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Timestamp now = UtilDateTime.nowTimestamp();

        GenericValue returnHeader;
        List returnItems = null;
        try {
            returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
            if (returnHeader != null) {
                returnItems = returnHeader.getRelatedByAnd("ReturnItem", UtilMisc.toMap("returnTypeId", "RTN_REFUND"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (returnItems != null) {
            Iterator ri = returnItems.iterator();
            while (ri.hasNext()) {
                GenericValue returnItem = (GenericValue) ri.next();
                String orderItemSeqId = returnItem.getString("orderItemSeqId");
                String orderId = returnItem.getString("orderId");

                // lookup subscriptions
                List subscriptions;
                try {
                    subscriptions = delegator.findByAnd("Subscription", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // cancel all current subscriptions
                if (subscriptions != null) {
                    Iterator si = subscriptions.iterator();
                    while (si.hasNext()) {
                        GenericValue subscription = (GenericValue) si.next();
                        Timestamp thruDate = subscription.getTimestamp("thruDate");
                        if (thruDate == null || thruDate.after(now)) {
                            subscription.set("thruDate", now);
                            try {
                                delegator.store(subscription);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Takes a List of returnItems and returns a Map of orderId -> items and a Map of orderId -> orderTotal
     * @param returnItems
     * @param itemsByOrder
     * @param totalByOrder
     * @param delegator
     * @param returnId
     */
    public static void groupReturnItemsByOrder(List returnItems, Map returnItemsByOrderId, Map totalByOrder, Delegator delegator, String returnId, String returnTypeId) {
        Iterator itemIt = returnItems.iterator();
        while (itemIt.hasNext()) {
            GenericValue returnItem = (GenericValue) itemIt.next();
            String orderId = returnItem.getString("orderId");
            if (orderId != null) {
                if (returnItemsByOrderId != null) {
                    BigDecimal totalForOrder = null;
                    if (totalByOrder != null) {
                        totalForOrder = (BigDecimal) totalByOrder.get(orderId);
                    }

                    List returnItemList = (List) returnItemsByOrderId.get(orderId);
                    if (returnItemList == null) {
                        returnItemList = new ArrayList();
                    }
                    if (totalForOrder == null) {
                        totalForOrder = BigDecimal.ZERO;
                    }

                    // add to the items list
                    returnItemList.add(returnItem);
                    returnItemsByOrderId.put(orderId, returnItemList);

                    if (totalByOrder != null) {
                        // add on the total for this line
                        BigDecimal quantity = returnItem.getBigDecimal("returnQuantity");
                        BigDecimal amount = returnItem.getBigDecimal("returnPrice");
                        if (quantity == null) {
                            quantity = BigDecimal.ZERO;
                        }
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }
                        BigDecimal thisTotal = amount.multiply(quantity);
                        BigDecimal existingTotal = totalForOrder;
                        Map condition = UtilMisc.toMap("returnId", returnItem.get("returnId"), "returnItemSeqId", returnItem.get("returnItemSeqId"));
                        BigDecimal newTotal = existingTotal.add(thisTotal).add(getReturnAdjustmentTotal(delegator, condition));
                        totalByOrder.put(orderId, newTotal);
                    }
                }
            }
        }

        // We may also have some order-level adjustments, so we need to go through each order again and add those as well
        if ((totalByOrder != null) && (totalByOrder.keySet() != null)) {
            Iterator orderIterator = totalByOrder.keySet().iterator();
            while (orderIterator.hasNext()) {
                String orderId = (String) orderIterator.next();
                // find returnAdjustment for returnHeader
                Map condition = UtilMisc.toMap("returnId", returnId,
                                               "returnItemSeqId", org.ofbiz.common.DataModelConstants.SEQ_ID_NA,
                                               "returnTypeId", returnTypeId);
                BigDecimal existingTotal = ((BigDecimal)totalByOrder.get(orderId)).add(getReturnAdjustmentTotal(delegator, condition));
                totalByOrder.put(orderId, existingTotal);
            }
        }
    }


    public static Map getReturnAmountByOrder(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");
        List returnItems = null;
        Map returnAmountByOrder = new HashMap();
        try {
            returnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("returnId", returnId));

        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorGettingReturnHeaderItemInformation", locale));
        }
        if ((returnItems != null) && (returnItems.size() > 0)) {
            Iterator returnItemIterator = returnItems.iterator();
            GenericValue returnItem = null;
            GenericValue returnItemResponse = null;
            GenericValue payment = null;
            String orderId;
            List paymentList = new ArrayList();
            while (returnItemIterator.hasNext()) {
                returnItem = (GenericValue) returnItemIterator.next();
                orderId = returnItem.getString("orderId");
                try {
                    returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse");
                    if ((returnItemResponse != null) && (orderId != null)) {
                        // TODO should we filter on payment's status (PMNT_SENT,PMNT_RECEIVED)
                        payment = returnItemResponse.getRelatedOne("Payment");
                        if ((payment != null) && (payment.getBigDecimal("amount") != null) &&
                                !paymentList.contains(payment.get("paymentId"))) {
                            UtilMisc.addToBigDecimalInMap(returnAmountByOrder, orderId, payment.getBigDecimal("amount"));
                            paymentList.add(payment.get("paymentId"));  // make sure we don't add duplicated payment amount
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up return item related information", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderErrorGettingReturnHeaderItemInformation", locale));
                }
            }
        }
        return UtilMisc.toMap("orderReturnAmountMap", returnAmountByOrder);
    }

    public static Map checkPaymentAmountForRefund(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");
        Map returnAmountByOrder = null;
        Map serviceResult = null;
        //GenericValue orderHeader = null;
        try {
            serviceResult = dispatcher.runSync("getReturnAmountByOrder", org.ofbiz.base.util.UtilMisc.toMap("returnId", returnId));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem running the getReturnAmountByOrder service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderProblemsWithGetReturnAmountByOrder", locale));
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError((String) serviceResult.get(ModelService.ERROR_MESSAGE));
        } else {
            returnAmountByOrder = (Map) serviceResult.get("orderReturnAmountMap");
        }

        if ((returnAmountByOrder != null) && (returnAmountByOrder.keySet() != null)) {
            Iterator orderIterator = returnAmountByOrder.keySet().iterator();
            while (orderIterator.hasNext()) {
                String orderId = (String) orderIterator.next();
                BigDecimal returnAmount = (BigDecimal) returnAmountByOrder.get(orderId);
                if (returnAmount.abs().compareTo(new BigDecimal("0.000001")) < 0) {
                    Debug.logError("Order [" + orderId + "] refund amount[ " + returnAmount + "] less than zero", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderReturnTotalCannotLessThanZero", locale));
                }
                OrderReadHelper helper = new OrderReadHelper(delegator, orderId);
                BigDecimal grandTotal = helper.getOrderGrandTotal();
                if (returnAmount == null) {
                    Debug.logInfo("No returnAmount found for order:" + orderId, module);
                } else {
                    if (returnAmount.subtract(grandTotal).compareTo(new BigDecimal("0.01")) > 0) {
                        Debug.logError("Order [" + orderId + "] refund amount[ " + returnAmount + "] exceeds order total [" + grandTotal + "]", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderRefundAmountExceedsOrderTotal", locale));
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map createReturnAdjustment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String orderAdjustmentId = (String) context.get("orderAdjustmentId");
        String returnAdjustmentTypeId = (String) context.get("returnAdjustmentTypeId");
        String returnId = (String) context.get("returnId");
        String returnItemSeqId = (String) context.get("returnItemSeqId");
        String description = (String) context.get("description");

        GenericValue returnItemTypeMap = null;
        GenericValue orderAdjustment = null;
        GenericValue returnAdjustmentType = null;
        GenericValue orderItem = null;
        GenericValue returnItem = null;
        GenericValue returnHeader = null;

        BigDecimal amount;

        // if orderAdjustment is not empty, then copy most return adjustment information from orderAdjustment's
        if (orderAdjustmentId != null) {
            try {
                orderAdjustment = delegator.findByPrimaryKey("OrderAdjustment", UtilMisc.toMap("orderAdjustmentId", orderAdjustmentId));

                // get returnHeaderTypeId from ReturnHeader and then use it to figure out return item type mapping
                returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));
                String returnHeaderTypeId = ((returnHeader != null) && (returnHeader.getString("returnHeaderTypeId") != null)) ? returnHeader.getString("returnHeaderTypeId") : "CUSTOMER_RETURN";
                returnItemTypeMap = delegator.findByPrimaryKey("ReturnItemTypeMap",
                        UtilMisc.toMap("returnHeaderTypeId", returnHeaderTypeId, "returnItemMapKey", orderAdjustment.get("orderAdjustmentTypeId")));
                returnAdjustmentType = returnItemTypeMap.getRelatedOne("ReturnAdjustmentType");
                if (returnAdjustmentType != null && UtilValidate.isEmpty(description)) {
                    description = returnAdjustmentType.getString("description");
                }
                if ((returnItemSeqId != null) && !("_NA_".equals(returnItemSeqId))) {
                    returnItem = delegator.findByPrimaryKey("ReturnItem",
                            UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItemSeqId));
                    Debug.log("returnId:" + returnId + ",returnItemSeqId:" + returnItemSeqId);
                    orderItem = returnItem.getRelatedOne("OrderItem");
                } else {
                    // we don't have the returnItemSeqId but before we consider this
                    // an header adjustment we try to get a return item in this return
                    // associated to the same order item to which the adjustments refers (if any)
                    if (UtilValidate.isNotEmpty(orderAdjustment.getString("orderItemSeqId")) &&
                            !"_NA_".equals(orderAdjustment.getString("orderItemSeqId"))) {
                        returnItem = EntityUtil.getFirst(delegator.findByAnd("ReturnItem",
                                                                             UtilMisc.toMap("returnId", returnId,
                                                                                            "orderId", orderAdjustment.getString("orderId"),
                                                                                            "orderItemSeqId", orderAdjustment.getString("orderItemSeqId"))));
                        if (UtilValidate.isNotEmpty(returnItem)) {
                            orderItem = returnItem.getRelatedOne("OrderItem");
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new GeneralRuntimeException(e.getMessage());
            }
            context.putAll(orderAdjustment.getAllFields());
        }

        // if orderAdjustmentTypeId is empty, ie not found from orderAdjustmentId, then try to get returnAdjustmentTypeId from returnItemTypeMap,
        // if still empty, use default RET_MAN_ADJ
        if (returnAdjustmentTypeId == null) {
            String mappingTypeId = returnItemTypeMap != null ? returnItemTypeMap.get("returnItemTypeId").toString() : null;
            returnAdjustmentTypeId = mappingTypeId != null ? mappingTypeId : "RET_MAN_ADJ";
        }
        // calculate the returnAdjustment amount
        if (returnItem != null) {  // returnAdjustment for returnItem
            if (needRecalculate(returnAdjustmentTypeId)) {
                Debug.logInfo("returnPrice:" + returnItem.getBigDecimal("returnPrice") + ",returnQuantity:" + returnItem.getBigDecimal("returnQuantity") + ",sourcePercentage:" + orderAdjustment.getBigDecimal("sourcePercentage"), module);
                if (orderAdjustment == null) {
                    Debug.logError("orderAdjustment [" + orderAdjustmentId + "] not found", module);
                    return ServiceUtil.returnError("orderAdjustment [" + orderAdjustmentId + "] not found");
                }
                BigDecimal returnTotal = returnItem.getBigDecimal("returnPrice").multiply(returnItem.getBigDecimal("returnQuantity"));
                BigDecimal orderTotal = orderItem.getBigDecimal("quantity").multiply(orderItem.getBigDecimal("unitPrice"));
                amount = getAdjustmentAmount("RET_SALES_TAX_ADJ".equals(returnAdjustmentTypeId), returnTotal, orderTotal, orderAdjustment.getBigDecimal("amount"));
            } else {
                amount = (BigDecimal) context.get("amount");
            }
        } else { // returnAdjustment for returnHeader
            amount = (BigDecimal) context.get("amount");
        }

        // store the return adjustment
        String seqId = delegator.getNextSeqId("ReturnAdjustment");
        GenericValue newReturnAdjustment = delegator.makeValue("ReturnAdjustment",
                UtilMisc.toMap("returnAdjustmentId", seqId));

        try {
            newReturnAdjustment.setNonPKFields(context);
            if (orderAdjustment != null && orderAdjustment.get("taxAuthorityRateSeqId") != null) {
                newReturnAdjustment.set("taxAuthorityRateSeqId", orderAdjustment.getString("taxAuthorityRateSeqId"));
            }
            newReturnAdjustment.set("amount", amount == null ? BigDecimal.ZERO : amount);
            newReturnAdjustment.set("returnAdjustmentTypeId", returnAdjustmentTypeId);
            newReturnAdjustment.set("description", description);
            newReturnAdjustment.set("returnItemSeqId", UtilValidate.isEmpty(returnItemSeqId) ? "_NA_" : returnItemSeqId);

            delegator.create(newReturnAdjustment);
            Map result = ServiceUtil.returnSuccess("Create ReturnAdjustment with Id:" + seqId + " successfully.");
            result.put("returnAdjustmentId", seqId);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to store returnAdjustment", module);
            return ServiceUtil.returnError("Failed to store returnAdjustment");
        }
    }

    public static Map updateReturnAdjustment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        GenericValue returnItem = null;
        GenericValue returnAdjustment = null;
        String returnAdjustmentTypeId = null;
        BigDecimal amount;


        try {
            returnAdjustment = delegator.findByPrimaryKey("ReturnAdjustment", UtilMisc.toMap("returnAdjustmentId", context.get("returnAdjustmentId")));
            if (returnAdjustment != null) {
                returnItem = delegator.findByPrimaryKey("ReturnItem",
                        UtilMisc.toMap("returnId", returnAdjustment.get("returnId"), "returnItemSeqId", returnAdjustment.get("returnItemSeqId")));
                returnAdjustmentTypeId = returnAdjustment.getString("returnAdjustmentTypeId");
            }

            // calculate the returnAdjustment amount
            if (returnItem != null) {  // returnAdjustment for returnItem
                BigDecimal originalReturnPrice = (context.get("originalReturnPrice") != null) ? ((BigDecimal) context.get("originalReturnPrice")) : returnItem.getBigDecimal("returnPrice");
                BigDecimal originalReturnQuantity = (context.get("originalReturnQuantity") != null) ? ((BigDecimal) context.get("originalReturnQuantity")) : returnItem.getBigDecimal("returnQuantity");

                if (needRecalculate(returnAdjustmentTypeId)) {
                    BigDecimal returnTotal = returnItem.getBigDecimal("returnPrice").multiply(returnItem.getBigDecimal("returnQuantity"));
                    BigDecimal originalReturnTotal = originalReturnPrice.multiply(originalReturnQuantity);
                    amount = getAdjustmentAmount("RET_SALES_TAX_ADJ".equals(returnAdjustmentTypeId), returnTotal, originalReturnTotal, returnAdjustment.getBigDecimal("amount"));
                } else {
                    amount = (BigDecimal) context.get("amount");
                }
            } else { // returnAdjustment for returnHeader
                amount = (BigDecimal) context.get("amount");
            }

            Map result = null;
            if (UtilValidate.isNotEmpty(amount)) {
                returnAdjustment.setNonPKFields(context);
                returnAdjustment.set("amount", amount);
                delegator.store(returnAdjustment);
                Debug.logInfo("Update ReturnAdjustment with Id:" + context.get("returnAdjustmentId") + " to amount " + amount +" successfully.", module);
                result = ServiceUtil.returnSuccess("Update ReturnAdjustment with Id:" + context.get("returnAdjustmentId") + " to amount " + amount +" successfully.");
            } else {
                result = ServiceUtil.returnSuccess();
            }
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to store returnAdjustment", module);
            return ServiceUtil.returnError("Failed to store returnAdjustment");
        }
    }

    //  used as a dispatch service, invoke different service based on the parameters passed in
    public static Map createReturnItemOrAdjustment(DispatchContext dctx, Map context) {
        Debug.logInfo("createReturnItemOrAdjustment's context:" + context, module);
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        Debug.logInfo("orderItemSeqId:" + orderItemSeqId +"#", module);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isNotEmpty(orderItemSeqId) ? "createReturnItem" : "createReturnAdjustment";
        Debug.logInfo("serviceName:" + serviceName, module);
        try {
            Map inMap = filterServiceContext(dctx, serviceName, context);
            if ("createReturnItem".equals(serviceName)) {
                // we don't want to automatically include the adjustments
                // when the return item is created because they are selectable by the user
                inMap.put("includeAdjustments", "N");
            }
            return dispatcher.runSync(serviceName, inMap);
        } catch (org.ofbiz.service.GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    //  used as a dispatch service, invoke different service based on the parameters passed in
    public static Map updateReturnItemOrAdjustment(DispatchContext dctx, Map context) {
        Debug.logInfo("updateReturnItemOrAdjustment's context:" + context, module);
        String returnAdjustmentId = (String) context.get("returnAdjustmentId");
        Debug.logInfo("returnAdjustmentId:" + returnAdjustmentId +"#", module);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isEmpty(returnAdjustmentId) ? "updateReturnItem" : "updateReturnAdjustment";
        Debug.logInfo("serviceName:" + serviceName, module);
        try {
            return dispatcher.runSync(serviceName, filterServiceContext(dctx, serviceName, context));
        } catch (org.ofbiz.service.GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * These return adjustment types need to be recalculated when the return item is updated
     * @param returnAdjustmentTypeId
     * @return
     */
    public static boolean needRecalculate(String returnAdjustmentTypeId) {
        return "RET_PROMOTION_ADJ".equals(returnAdjustmentTypeId) ||
                "RET_DISCOUNT_ADJ".equals(returnAdjustmentTypeId) ||
                "RET_SALES_TAX_ADJ".equals(returnAdjustmentTypeId);

    }

    /**
     * Get the total return adjustments for a set of key -> value condition pairs.  Done for code efficiency.
     * @param delegator
     * @param condition
     * @return
     */
    public static BigDecimal getReturnAdjustmentTotal(Delegator delegator, Map condition) {
        BigDecimal total = BigDecimal.ZERO;
        List adjustments;
        try {
            // TODO: find on a view-entity with a sum is probably more efficient
            adjustments = delegator.findByAnd("ReturnAdjustment", condition);
            if (adjustments != null) {
                Iterator adjustmentIterator = adjustments.iterator();
                while (adjustmentIterator.hasNext()) {
                    GenericValue returnAdjustment = (GenericValue) adjustmentIterator.next();
                    if ((returnAdjustment != null) && (returnAdjustment.get("amount") != null)) {
                       total = total.add(returnAdjustment.getBigDecimal("amount"));
                    }
                }
            }
        } catch (org.ofbiz.entity.GenericEntityException e) {
            Debug.logError(e, module);
        }
        return total;
    }

    /**
     *  Get rid of unnecessary parameters based on the given service name
     * @param dctx Service DispatchContext
     * @param serviceName
     * @param context   context before clean up
     * @return filtered context
     * @throws GenericServiceException
     */
    public static Map filterServiceContext(DispatchContext dctx, String serviceName, Map context) throws GenericServiceException {
        ModelService modelService = dctx.getModelService(serviceName);

        if (modelService == null) {
            throw new GenericServiceException("Problems getting the service model");
        }
        Map serviceContext = FastMap.newInstance();
        List modelParmInList = modelService.getInModelParamList();
        Iterator modelParmInIter = modelParmInList.iterator();
        while (modelParmInIter.hasNext()) {
            ModelParam modelParam = (ModelParam) modelParmInIter.next();
            String paramName =  modelParam.name;

            Object value = context.get(paramName);
            if (value != null) {
                serviceContext.put(paramName, value);
            }
        }
        return serviceContext;
    }

    /**
     * Calculate new returnAdjustment amount and set scale and rounding mode based on returnAdjustmentType: RET_SALES_TAX_ADJ use sales.tax._ and others use order._
     * @param isSalesTax  if returnAdjustmentType is SaleTax
     * @param returnTotal
     * @param originalTotal
     * @param amount
     * @return  new returnAdjustment amount
     */
    public static BigDecimal getAdjustmentAmount(boolean isSalesTax, BigDecimal returnTotal, BigDecimal originalTotal, BigDecimal amount) {
        String settingPrefix = isSalesTax ? "salestax" : "order";
        String decimalsPrefix = isSalesTax ? ".calc" : "";
        int decimals = UtilNumber.getBigDecimalScale(settingPrefix + decimalsPrefix + ".decimals");
        int rounding = UtilNumber.getBigDecimalRoundingMode(settingPrefix + ".rounding");
        returnTotal = returnTotal.setScale(decimals, rounding);
        originalTotal = originalTotal.setScale(decimals, rounding);
        BigDecimal newAmount = null;
        if (ZERO.compareTo(originalTotal) != 0) {
            newAmount = returnTotal.divide(originalTotal, decimals, rounding).multiply(amount).setScale(decimals, rounding);
        } else {
            newAmount = ZERO;
        }
        return newAmount;
    }
}
