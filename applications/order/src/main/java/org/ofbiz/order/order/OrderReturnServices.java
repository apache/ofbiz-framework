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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.thirdparty.paypal.ExpressCheckoutEvents;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * OrderReturnServices
 */
public class OrderReturnServices {

    public static final String module = OrderReturnServices.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";
    public static final String resourceProduct = "ProductUiLabels";

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
    public static Map<String, Object> getReturnItemInitialCost(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        String returnItemSeqId = (String) context.get("returnItemSeqId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("initialItemCost", getReturnItemInitialCost(delegator, returnId, returnItemSeqId));
        return result;
    }

    // obtain order/return total information
    public static Map<String, Object> getOrderAvailableReturnedTotal(DispatchContext dctx, Map<String, ? extends Object> context) {
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


        Map<String, Object> result = ServiceUtil.returnSuccess();
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
        Debug.logInfo("Finding the initial item cost for return item : " + returnId + " / " + returnItemSeqId, module);

        // the cost holder
        BigDecimal itemCost = BigDecimal.ZERO;

        // get the return item information
        GenericValue returnItem = null;
        try {
            returnItem = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId, "returnItemSeqId", returnItemSeqId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }
        Debug.logInfo("Return item value object - " + returnItem, module);

        // check for an orderItem association
        if (returnItem != null) {
            String orderId = returnItem.getString("orderId");
            String orderItemSeqId = returnItem.getString("orderItemSeqId");
            if (orderItemSeqId != null && orderId != null) {
                Debug.logInfo("Found order item reference", module);
                // locate the item issuance(s) for this order item
                GenericValue issue = null;
                try {
                    issue = EntityQuery.use(delegator).from("ItemIssuance").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    throw new GeneralRuntimeException(e.getMessage());
                }
                if (UtilValidate.isNotEmpty(issue)) {
                    Debug.logInfo("Found item issuance reference", module);
                    // just use the first one for now; maybe later we can find a better way to determine which was the
                    // actual item being returned; maybe by serial number
                    GenericValue inventoryItem = null;
                    try {
                        inventoryItem = issue.getRelatedOne("InventoryItem", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        throw new GeneralRuntimeException(e.getMessage());
                    }
                    if (inventoryItem != null) {
                        Debug.logInfo("Located inventory item - " + inventoryItem.getString("inventoryItemId"), module);
                        if (inventoryItem.get("unitCost") != null) {
                            itemCost = inventoryItem.getBigDecimal("unitCost");
                        } else {
                            Debug.logInfo("Found item cost; but cost was null. Returning default amount (0.00)", module);
                        }
                    }
                }
            }
        }

        Debug.logInfo("Initial item cost - " + itemCost, module);
        return itemCost;
    }

    // helper method for sending return notifications
    private static Map<String, Object> sendReturnNotificationScreen(DispatchContext dctx, Map<String, ? extends Object> context, String emailType) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");

        // get the return header
        GenericValue returnHeader = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorUnableToGetReturnHeaderForID", UtilMisc.toMap("returnId",returnId), locale));
        }

        // get the return items
        List<GenericValue> returnItems = null;
        List<GenericValue> returnAdjustments = new LinkedList<GenericValue>();
        try {
            returnItems = returnHeader.getRelated("ReturnItem", null, null, false);
            returnAdjustments = EntityQuery.use(delegator).from("ReturnAdjustment")
                    .where("returnId", returnId, "returnItemSeqId", "_NA_")
                    .orderBy("returnAdjustmentTypeId")
                    .cache(true)
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, 
                    "OrderErrorUnableToGetReturnItemRecordsFromReturnHeader", locale));
        }

        // get the order header -- the first item will determine which product store to use from the order
        String productStoreId = null;
        String emailAddress = null;
        if (UtilValidate.isNotEmpty(returnItems)) {
            GenericValue firstItem = EntityUtil.getFirst(returnItems);
            GenericValue orderHeader = null;
            try {
                orderHeader = firstItem.getRelatedOne("OrderHeader", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorUnableToGetOrderHeaderFromReturnItem", locale));
            }

            if (orderHeader != null && UtilValidate.isNotEmpty(orderHeader.getString("productStoreId"))) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                productStoreId = orh.getProductStoreId();
                emailAddress = orh.getOrderEmailString();
            }
        }

        // get the email setting and send the mail
        if (UtilValidate.isNotEmpty(productStoreId)) {
            Map<String, Object> sendMap = new HashMap<String, Object>();

            GenericValue productStoreEmail = null;
            try {
                productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId", productStoreId, "emailType", emailType).queryOne();
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

                Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("returnHeader", returnHeader, "returnItems", returnItems, "returnAdjustments", returnAdjustments, "locale", locale, "userLogin", userLogin);
                sendMap.put("bodyParameters", bodyParameters);

                sendMap.put("subject", productStoreEmail.getString("subject"));
                sendMap.put("contentType", productStoreEmail.get("contentType"));
                sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
                sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
                sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));
                sendMap.put("sendTo", emailAddress);
                sendMap.put("partyId", returnHeader.getString("fromPartyId"));

                sendMap.put("userLogin", userLogin);

                Map<String, Object> sendResp = null;
                try {
                    sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem sending mail", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemSendingEmail", locale));
                }

                // check for errors
                if (sendResp != null && ServiceUtil.isError(sendResp)) {
                    sendResp.put("emailType", emailType);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemSendingEmail", locale), null, null, sendResp);
                }
                return ServiceUtil.returnSuccess();
            }
        }

        return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceProduct, 
                "ProductProductStoreEmailSettingsNotValid", 
                    UtilMisc.toMap("productStoreId", productStoreId, 
                            "emailType", emailType), locale));
    }

    // return request notification
    public static Map<String, Object> sendReturnAcceptNotification(DispatchContext dctx, Map<String, ? extends Object> context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_ACCEPT");
    }

    // return complete notification
    public static Map<String, Object> sendReturnCompleteNotification(DispatchContext dctx, Map<String, ? extends Object> context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_COMPLETE");
    }

    // return cancel notification
    public static Map<String, Object> sendReturnCancelNotification(DispatchContext dctx, Map<String, ? extends Object> context) {
        return sendReturnNotificationScreen(dctx, context, "PRDS_RTN_CANCEL");
    }

    // cancel replacement order if return not received within 30 days and send notification
    public static Map<String,Object> autoCancelReplacementOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<GenericValue> returnHeaders = null;
        try {
            returnHeaders = EntityQuery.use(delegator).from("ReturnHeader")
                    .where("statusId", "RETURN_ACCEPTED", "returnHeaderTypeId", "CUSTOMER_RETURN")
                    .orderBy("entryDate")
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Return headers", module);
        }
        for (GenericValue returnHeader : returnHeaders) {
            String returnId = returnHeader.getString("returnId");
            Timestamp entryDate = returnHeader.getTimestamp("entryDate");
            String daysTillCancelStr = EntityUtilProperties.getPropertyValue("order", "daysTillCancelReplacementOrder", "30", delegator);
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
                        List<GenericValue> returnItems = EntityQuery.use(delegator).from("ReturnItem")
                                .where("returnId", returnId, "returnTypeId", "RTN_WAIT_REPLACE_RES")
                                .orderBy("createdStamp")
                                .queryList();
                        for (GenericValue returnItem : returnItems) {
                            GenericValue returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse", false);
                            if (returnItemResponse != null) {
                                String replacementOrderId = returnItemResponse.getString("replacementOrderId");
                                Map<String, Object> svcCtx = UtilMisc.<String, Object>toMap("orderId", replacementOrderId, "userLogin", userLogin);
                                GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", replacementOrderId).queryOne();
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
                        Debug.logError(e, module);
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }
    // get the returnable quantiy for an order item
    public static Map<String, Object> getReturnableQuantity(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue orderItem = (GenericValue) context.get("orderItem");
        GenericValue product = null;
        Locale locale = (Locale) context.get("locale");
        if (orderItem.get("productId") != null) {
            try {
                product = orderItem.getRelatedOne("Product", false);
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
            List<GenericValue> returnedItems = null;
            try {
                returnedItems = orderItem.getRelated("ReturnItem", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorUnableToGetReturnItemInformation", locale));
            }
            if (UtilValidate.isEmpty(returnedItems)) {
                returnableQuantity = orderQty;
            } else {
                BigDecimal returnedQty = BigDecimal.ZERO;
                for (GenericValue returnItem : returnedItems) {
                    GenericValue returnHeader = null;
                    try {
                        returnHeader = returnItem.getRelatedOne("ReturnHeader", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorUnableToGetReturnHeaderFromItem", locale));
                    }
                    String returnStatus = returnHeader.getString("statusId");
                    if (!returnStatus.equals("RETURN_CANCELLED")) {
                        if(!UtilValidate.isEmpty(returnItem.getBigDecimal("returnQuantity"))){
                            returnedQty = returnedQty.add(returnItem.getBigDecimal("returnQuantity"));   
                        } 
                    }
                }
                if (returnedQty.compareTo(orderQty) < 0) {
                    returnableQuantity = orderQty.subtract(returnedQty);
                }
            }
        }

        // get the returnable price now equals to orderItem.unitPrice, since adjustments are booked separately

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("returnableQuantity", returnableQuantity);
        result.put("returnablePrice", orderItem.getBigDecimal("unitPrice"));
        return result;
    }

    // get a map of returnable items (items not already returned) and quantities
    public static Map<String, Object> getReturnableItems(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorUnableToGetReturnItemInformation", locale));
        }

        Map<GenericValue, Map<String, Object>> returnable = new LinkedHashMap<GenericValue, Map<String, Object>>();
        if (orderHeader != null) {
            // OrderReadHelper orh = new OrderReadHelper(orderHeader);
            // OrderItems which have been issued may be returned.
            EntityConditionList<EntityExpr> whereConditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderHeader.getString("orderId")),
                    EntityCondition.makeCondition("orderItemStatusId", EntityOperator.IN, UtilMisc.toList("ITEM_APPROVED", "ITEM_COMPLETED"))
               ), EntityOperator.AND);
            /*
            EntityConditionList havingConditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("quantityIssued", EntityOperator.GREATER_THAN, Double.valueOf(0))
               ), EntityOperator.AND);
             */
            List<GenericValue> orderItemQuantitiesIssued = null;
            try {
                orderItemQuantitiesIssued = EntityQuery.use(delegator).select("orderId", "orderItemSeqId", "quantityIssued").from("OrderItemQuantityReportGroupByItem").where(whereConditions).orderBy("orderItemSeqId").queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorUnableToGetReturnHeaderFromItem", locale));
            }

            if (orderItemQuantitiesIssued != null) {
                for (GenericValue orderItemQuantityIssued : orderItemQuantitiesIssued) {
                    GenericValue item = null;
                    try {
                        item = orderItemQuantityIssued.getRelatedOne("OrderItem", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorUnableToGetOrderItemInformation", locale));
                    }
                    // items not issued/shipped are considered as returnable only if they are
                    // not physical items
                    if ("SALES_ORDER".equals(orderHeader.getString("orderTypeId"))) {
                        BigDecimal quantityIssued = orderItemQuantityIssued.getBigDecimal("quantityIssued");
                        if (UtilValidate.isEmpty(quantityIssued) || quantityIssued.compareTo(BigDecimal.ZERO) == 0) {
                            try {
                                GenericValue itemProduct = item.getRelatedOne("Product", false);
                                if (ProductWorker.isPhysical(itemProduct)) {
                                    continue;
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Problems looking up returnable product type information", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderErrorUnableToGetTheItemReturnableProduct", locale));
                            }
                        }
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("getReturnableQuantity", UtilMisc.toMap("orderItem", item));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorUnableToGetTheItemReturnableQuantity", locale));
                    }
                    if (serviceResult.containsKey(ModelService.ERROR_MESSAGE)) {
                        return ServiceUtil.returnError((String) serviceResult.get(ModelService.ERROR_MESSAGE));
                    } else {

                        // Don't add the OrderItem to the map of returnable OrderItems if there isn't any returnable quantity.
                        if (((BigDecimal) serviceResult.get("returnableQuantity")).compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                        Map<String, Object> returnInfo = new HashMap<String, Object>();
                        // first the return info (quantity/price)
                        returnInfo.put("returnableQuantity", serviceResult.get("returnableQuantity"));
                        returnInfo.put("returnablePrice", serviceResult.get("returnablePrice"));

                        // now the product type information
                        String itemTypeKey = "FINISHED_GOOD"; // default item type (same as invoice)
                        GenericValue product = null;
                        if (item.get("productId") != null) {
                            try {
                                product = item.getRelatedOne("Product", false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderErrorUnableToGetOrderItemInformation", locale));
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
                        List<GenericValue> itemAdjustments = null;
                        try {
                            itemAdjustments = item.getRelated("OrderAdjustment", null, null, false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderErrorUnableToGetOrderAdjustmentsFromItem", locale));
                        }
                        if (UtilValidate.isNotEmpty(itemAdjustments)) {
                            for (GenericValue itemAdjustment : itemAdjustments) {
                                returnInfo = new HashMap<String, Object>();
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
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorNoOrderItemsFound", locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorUnableToFindOrderHeader", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("returnableItems", returnable);
        return result;
    }

    // check return items status and update return header status
    public static Map<String, Object> checkReturnComplete(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem", null, null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        // if already completed just return
        String currentStatus = null;
        if (returnHeader != null && returnHeader.get("statusId") != null) {
            currentStatus = returnHeader.getString("statusId");
            if ("RETURN_COMPLETED".equals(currentStatus) || "RETURN_CANCELLED".equals(currentStatus)) {
                return ServiceUtil.returnSuccess();
            }
        }

        List<GenericValue> completedItems = new LinkedList<GenericValue>();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            for (GenericValue item : returnItems) {
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
                                GenericValue itemProduct = item.getRelatedOne("Product", false);
                                if (!ProductWorker.isPhysical(itemProduct)) {
                                    completedItems.add(item);
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Problems looking up returned product type information", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderErrorGettingReturnHeaderItemInformation", locale));
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
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorUnableToCreateReturnStatusHistory", locale));
                    }
                } else if (currentStatus != null && "RETURN_RECEIVED".equals(currentStatus)) {
                    try {
                        dispatcher.runSync("updateReturnHeader", UtilMisc.<String, Object>toMap("returnId", returnId,
                                                                                "statusId", "RETURN_COMPLETED",
                                                                                "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderErrorUnableToCreateReturnStatusHistory", locale));
                    }
                }
            }

        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("statusId", returnHeader.get("statusId"));
        return result;
    }

    // credit (billingAccount) return
    public static Map<String, Object> processCreditReturn(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem", UtilMisc.toMap("returnTypeId", "RTN_CREDIT"), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap("returnId", returnId, "returnTypeId", "RTN_CREDIT"));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            String finAccountId = returnHeader.getString("finAccountId");
            String billingAccountId = returnHeader.getString("billingAccountId");
            String fromPartyId = returnHeader.getString("fromPartyId");
            String toPartyId = returnHeader.getString("toPartyId");

            // make sure total refunds on a return don't exceed amount of returned orders
            Map<String, Object> serviceResult = null;
            try {
                serviceResult = dispatcher.runSync("checkPaymentAmountForRefund", UtilMisc.toMap("returnId", returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem running the checkPaymentAmountForRefund service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemsWithCheckPaymentAmountForRefund", locale));
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
                    orderHeader = returnItem.getRelatedOne("OrderHeader", false);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            if (orderHeader != null) {
                OrderReadHelper orderReadHelper = new OrderReadHelper(orderHeader);
                productStore = orderReadHelper.getProductStore();
            }

            // if both billingAccountId and finAccountId are supplied, look for productStore.storeCreditAccountEnumId preference
            if (finAccountId != null && billingAccountId != null && productStore != null && productStore.getString("storeCreditAccountEnumId") != null) {
                Debug.logWarning("You have entered both financial account and billing account for store credit. Based on the configuration on product store, only one of them will be selected.", module);
                if ("BILLING_ACCOUNT".equals(productStore.getString("storeCreditAccountEnumId"))) {
                    finAccountId = null;
                    Debug.logWarning("Default setting on product store is billing account. Store credit will goes to billing account [" + billingAccountId + "]", module);
                } else {
                    billingAccountId = null;
                    Debug.logWarning("Default setting on product store is financial account. Store credit will goes to financial account [" + finAccountId + "]", module);
               }
            }

            if (finAccountId == null && billingAccountId == null) {
                // First find a Billing Account with negative balance, and if found store credit to that
                List<GenericValue> billingAccounts = new LinkedList<GenericValue>();
                try {
                    billingAccounts = EntityQuery.use(delegator).from("BillingAccountRoleAndAddress")
                            .where("partyId", fromPartyId, "roleTypeId", "BILL_TO_CUSTOMER")
                            .filterByDate()
                            .orderBy("-fromDate")
                            .queryList();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (UtilValidate.isNotEmpty(billingAccounts)) {
                    ListIterator<GenericValue> billingAccountItr = billingAccounts.listIterator();
                    while (billingAccountItr.hasNext() && billingAccountId == null) {
                        String thisBillingAccountId = billingAccountItr.next().getString("billingAccountId");
                        BigDecimal billingAccountBalance = ZERO;
                        try {
                            GenericValue billingAccount = EntityQuery.use(delegator).from("BillingAccount").where("billingAccountId", thisBillingAccountId).queryOne();
                            billingAccountBalance = OrderReadHelper.getBillingAccountBalance(billingAccount);
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
                            Map<String, Object> results = createBillingAccountFromReturn(returnHeader, returnItems, dctx, context);
                            if (ServiceUtil.isError(results)) {
                                Debug.logError("Error creating BillingAccount: " + results.get(ModelService.ERROR_MESSAGE), module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderErrorWithCreateBillingAccount", locale) + results.get(ModelService.ERROR_MESSAGE));
                            }
                            billingAccountId = (String) results.get("billingAccountId");

                            // double check; make sure we have a billingAccount
                            if (billingAccountId == null) {
                                Debug.logError("No available billing account, none was created", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderNoAvailableBillingAccount", locale));
                            }
                        }
                    } else {
                        GenericValue finAccount = null;
                        try {
                            finAccount = EntityQuery.use(delegator).from("FinAccountAndRole")
                                    .where("partyId", fromPartyId, "finAccountTypeId", "STORE_CREDIT_ACCT", "roleTypeId", "OWNER", "statusId", "FNACT_ACTIVE")
                                    .filterByDate()
                                    .orderBy("-fromDate")
                                    .queryFirst();
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (UtilValidate.isNotEmpty(finAccount)) {
                            finAccountId = finAccount.getString("finAccountId");
                        }

                        if (finAccountId == null) {
                            Map<String, Object> createAccountCtx = new HashMap<String, Object>();
                            createAccountCtx.put("ownerPartyId", fromPartyId);
                            createAccountCtx.put("finAccountTypeId", "STORE_CREDIT_ACCT");
                            createAccountCtx.put("productStoreId", productStore.getString("productStoreId"));
                            createAccountCtx.put("currencyUomId", returnHeader.getString("currencyUomId"));
                            createAccountCtx.put("finAccountName", "Store Credit Account for party ["+fromPartyId+"]");
                            createAccountCtx.put("userLogin", userLogin);
                            Map<String, Object> createAccountResult = null;
                            try {
                                createAccountResult = dispatcher.runSync("createFinAccountForStore", createAccountCtx);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problems running the createFinAccountForStore service", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderProblemsCreatingFinAccountForStore", locale));
                            }
                            if (ServiceUtil.isError(createAccountResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountResult));
                            }
                            finAccountId = (String) createAccountResult.get("finAccountId");

                            // double check; make sure we have a FinAccount
                            if (finAccountId == null) {
                                Debug.logError("No available fin account, none was created", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderNoAvailableFinAccount", locale));
                            }

                            Map<String, Object> finAccountRoleResult = null;
                            try {
                                finAccountRoleResult = dispatcher.runSync("createFinAccountRole", UtilMisc.toMap("finAccountId", finAccountId, "partyId", fromPartyId, "roleTypeId", "OWNER", "userLogin", userLogin));
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problem running the createFinAccountRole service", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderProblemCreatingFinAccountRoleRecord", locale));
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
            for (GenericValue item : returnItems) {
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
                Map<String, Object> finAccountTransResult = null;
                try {
                    finAccountTransResult = dispatcher.runSync("createFinAccountTrans", UtilMisc.toMap("finAccountId", finAccountId, "finAccountTransTypeId", "DEPOSIT", "partyId", toPartyId, "amount", creditTotal, "reasonEnumId", "FATR_REFUND", "userLogin", userLogin));
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem creating FinAccountTrans record", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemCreatingFinAccountTransRecord", locale));
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
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemCreatingPaymentRecord", locale));
            }

            // create a return item response
            Map<String, Object> itemResponse = UtilMisc.<String, Object>toMap("paymentId", paymentId);
            itemResponse.put("responseAmount", creditTotal);
            itemResponse.put("responseDate", now);
            itemResponse.put("userLogin", userLogin);
            if (billingAccountId != null) {
                itemResponse.put("billingAccountId", billingAccountId);
            } else {
                itemResponse.put("finAccountTransId", finAccountTransId);
            }
            Map<String, Object> serviceResults = null;
            try {
                serviceResults = dispatcher.runSync("createReturnItemResponse", itemResponse);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemCreatingReturnItemResponseRecord", locale), null, null, serviceResults);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem creating ReturnItemResponse record", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemCreatingReturnItemResponseRecord", locale));
            }

            // the resulting response ID will be associated with the return items
            String itemResponseId = (String) serviceResults.get("returnItemResponseId");

            // loop through the items again to update them and store a status change history
            for (GenericValue item : returnItems) {
                Map<String, Object> returnItemMap = UtilMisc.<String, Object>toMap("returnItemResponseId", itemResponseId, "returnId", item.get("returnId"), "returnItemSeqId", item.get("returnItemSeqId"), "statusId", "RETURN_COMPLETED", "userLogin", userLogin);
                // store the item changes (attached responseId)
                try {
                    serviceResults = dispatcher.runSync("updateReturnItem", returnItemMap);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemStoringReturnItemUpdates", locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem storing ReturnItem updates", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemStoringReturnItemUpdates", locale));
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
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemCreatingPaymentApplicationRecord", locale));
                }

                // create the payment applications for the return invoice in case of billing account
                try {
                    serviceResults = dispatcher.runSync("createPaymentApplicationsFromReturnItemResponse",
                            UtilMisc.<String, Object>toMap("returnItemResponseId", itemResponseId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemCreatingPaymentApplicationRecord", locale), null, null, serviceResults);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem creating PaymentApplication records for return invoice", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderProblemCreatingPaymentApplicationRecord", locale));
                }
            }
        }

        return ServiceUtil.returnSuccess();
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
    private static Map<String, Object> createBillingAccountFromReturn(GenericValue returnHeader, List<GenericValue> returnItems, DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {
            // get the related product stores via the orders related to this return
            List<GenericValue> orders = EntityUtil.getRelated("OrderHeader", null, returnItems, false);
            List<GenericValue> productStores = EntityUtil.getRelated("ProductStore", null, orders, false);

            // find the minimum storeCreditValidDays of all the ProductStores associated with all the Orders on the Return, skipping null ones
            Long storeCreditValidDays = null;
            for (GenericValue productStore : productStores) {
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
            Map<String, Object> input = UtilMisc.<String, Object>toMap("accountLimit", BigDecimal.ZERO, "description", "Credit Account for Return #" + returnHeader.get("returnId"), "userLogin", userLogin);
            input.put("accountCurrencyUomId", returnHeader.get("currencyUomId"));
            input.put("thruDate", thruDate);
            Map<String, Object> results = dispatcher.runSync("createBillingAccount", input);
            if (ServiceUtil.isError(results)) return results;
            String billingAccountId = (String) results.get("billingAccountId");

            // set the role on the account
            input = UtilMisc.toMap("billingAccountId", billingAccountId, "partyId", returnHeader.get("fromPartyId"), "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin);
            Map<String, Object> roleResults = dispatcher.runSync("createBillingAccountRole", input);
            if (ServiceUtil.isError(roleResults)) {
                Debug.logError("Error with createBillingAccountRole: " + roleResults.get(ModelService.ERROR_MESSAGE), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderErrorWithCreateBillingAccountRole", locale) + roleResults.get(ModelService.ERROR_MESSAGE));
            }

            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity error when creating BillingAccount: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderProblemsCreatingBillingAccount", locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service error when creating BillingAccount: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderProblemsCreatingBillingAccount", locale));
        }
    }

    public static Map<String, Object> processRefundReturnForReplacement(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        Map<String, Object> serviceResult = new HashMap<String, Object>();

        GenericValue orderHeader = null;
        List<GenericValue> orderPayPrefs = new LinkedList<GenericValue>();
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", null, UtilMisc.toList("-maxAmount"), false);
        } catch (GenericEntityException e) {
            Debug.logError("Problem looking up order information for orderId #" + orderId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderCannotGetOrderHeader", locale));
        }

        // Check for replacement order
        if (UtilValidate.isEmpty(orderPayPrefs)) {
            List<GenericValue> returnItemResponses = new LinkedList<GenericValue>();
            try {
                returnItemResponses = orderHeader.getRelated("ReplacementReturnItemResponse", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError("Problem getting ReturnItemResponses", module);
                return ServiceUtil.returnError(e.getMessage());
            }

            for (GenericValue returnItemResponse : returnItemResponses) {
                GenericValue returnItem = null;
                GenericValue returnHeader = null;
                try {
                    returnItem = EntityUtil.getFirst(returnItemResponse.getRelated("ReturnItem", null, null, false));
                    returnHeader = returnItem.getRelatedOne("ReturnHeader", false);
                } catch (GenericEntityException e) {
                    Debug.logError("Problem getting ReturnItem", module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if ("RETURN_RECEIVED".equals(returnHeader.getString("statusId"))) {
                    String returnId = returnItem.getString("returnId");
                    String returnTypeId = returnItem.getString("returnTypeId");
                    try {
                        serviceResult = dispatcher.runSync("processRefundReturn", UtilMisc.toMap("returnId", returnId, "returnTypeId", returnTypeId, "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem running the processRefundReturn service", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemsWithTheRefundSeeLogs", locale));
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
    public static Map<String, Object> processRefundReturn(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String returnId = (String) context.get("returnId");
        String returnTypeId = (String) context.get("returnTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem", UtilMisc.toMap("returnTypeId", returnTypeId), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorGettingReturnHeaderItemInformation", locale));
        }

        BigDecimal adjustments = getReturnAdjustmentTotal(delegator, UtilMisc.toMap("returnId", returnId, "returnTypeId", returnTypeId));

        if (returnHeader != null && (UtilValidate.isNotEmpty(returnItems) || adjustments.compareTo(ZERO) > 0)) {
            Map<String, List<GenericValue>> itemsByOrder = new HashMap<String, List<GenericValue>>();
            Map<String, BigDecimal> totalByOrder = new HashMap<String, BigDecimal>();

            // make sure total refunds on a return don't exceed amount of returned orders
            Map<String, Object> serviceResult = null;
            try {
                serviceResult = dispatcher.runSync("checkPaymentAmountForRefund", UtilMisc.toMap("returnId", returnId));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem running the checkPaymentAmountForRefund service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                        "OrderProblemsWithCheckPaymentAmountForRefund", locale));
            }
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }

            groupReturnItemsByOrder(returnItems, itemsByOrder, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            for (Map.Entry<String, List<GenericValue>> entry : itemsByOrder.entrySet()) {
                String orderId = entry.getKey();
                List<GenericValue> items = entry.getValue();
                BigDecimal orderTotal = totalByOrder.get(orderId);

                // get order header & payment prefs
                GenericValue orderHeader = null;
                List<GenericValue> orderPayPrefs = null;
                try {
                    orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                    // sort these desending by maxAmount
                    orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", null, UtilMisc.toList("-maxAmount"), false);

                    List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_SETTLED"), EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAYMENT_RECEIVED"));
                    orderPayPrefs = EntityUtil.filterByOr(orderPayPrefs, exprs);

                    // Check for replacement order
                    if (UtilValidate.isEmpty(orderPayPrefs)) {
                        GenericValue orderItemAssoc = EntityQuery.use(delegator).from("OrderItemAssoc")
                                                          .where("toOrderId", orderId, "orderItemAssocTypeId", "REPLACEMENT")
                                                          .queryFirst();
                        if (UtilValidate.isNotEmpty(orderItemAssoc)) {
                            String originalOrderId = orderItemAssoc.getString("orderId");
                            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", originalOrderId).queryOne();
                            orderPayPrefs = orderHeader.getRelated("OrderPaymentPreference", null, UtilMisc.toList("-maxAmount"), false);
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
                GenericValue productStore = orderReadHelper.getProductStore();
                if (UtilValidate.isEmpty(productStore) || UtilValidate.isEmpty(productStore.get("payToPartyId"))) {
                    Debug.logError("No payToPartyId found for orderId " + orderId, module);
                } else {
                    GenericValue orgAcctgPref = null;
                    Map<String, Object> acctgPreferencesResult = null;
                    try {
                        acctgPreferencesResult = dispatcher.runSync("getPartyAccountingPreferences", UtilMisc.toMap("organizationPartyId", productStore.get("payToPartyId"), "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Error retrieving PartyAcctgPreference for partyId " + productStore.get("payToPartyId"), module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemsWithGetPartyAcctgPreferences", locale));
                    }
                    orgAcctgPref = (GenericValue) acctgPreferencesResult.get("partyAccountingPreference");

                    if (UtilValidate.isNotEmpty(orgAcctgPref)) {
                        try {
                            orgAcctgPref.getRelatedOne("PaymentMethod", false);
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
                Map<String, BigDecimal> receivedPaymentTotalsByPaymentMethod = orderReadHelper.getReceivedPaymentTotalsByPaymentMethod() ;
                Map<String, BigDecimal> refundedTotalsByPaymentMethod = orderReadHelper.getReturnedTotalsByPaymentMethod() ;

                // getOrderPaymentPreferenceTotalByType has been called because getReceivedPaymentTotalsByPaymentMethod does not
                // return payments captured from Billing Account.This is because when payment is captured from Billing Account
                // then no entry is maintained in Payment entity.
                BigDecimal receivedPaymentTotalsByBillingAccount = orderReadHelper.getOrderPaymentPreferenceTotalByType("EXT_BILLACT");

                /*
                 * Go through the OrderPaymentPreferences and determine how much remains to be refunded for each.
                 * Then group these refund amounts and orderPaymentPreferences by paymentMethodTypeId.  That is,
                 * the intent is to get the refundable amounts per orderPaymentPreference, grouped by payment method type.
                 */
                Map<String, List<Map<String, Object>>> prefSplitMap = new HashMap<String, List<Map<String,Object>>>();
                for (GenericValue orderPayPref : orderPayPrefs) {
                    String paymentMethodTypeId = orderPayPref.getString("paymentMethodTypeId");
                    String orderPayPrefKey = orderPayPref.getString("paymentMethodId") != null ? orderPayPref.getString("paymentMethodId") : orderPayPref.getString("paymentMethodTypeId");

                    // See how much we can refund to the payment method
                    BigDecimal orderPayPrefReceivedTotal = ZERO;
                    if (receivedPaymentTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add(receivedPaymentTotalsByPaymentMethod.get(orderPayPrefKey)).setScale(decimals, rounding);
                    }

                    if (receivedPaymentTotalsByBillingAccount != null) {
                        orderPayPrefReceivedTotal = orderPayPrefReceivedTotal.add(receivedPaymentTotalsByBillingAccount);
                    }
                    BigDecimal orderPayPrefRefundedTotal = ZERO;
                    if (refundedTotalsByPaymentMethod.containsKey(orderPayPrefKey)) {
                        orderPayPrefRefundedTotal = orderPayPrefRefundedTotal.add(refundedTotalsByPaymentMethod.get(orderPayPrefKey)).setScale(decimals, rounding);
                    }
                    BigDecimal orderPayPrefAvailableTotal = orderPayPrefReceivedTotal.subtract(orderPayPrefRefundedTotal);

                    // add the refundable amount and orderPaymentPreference to the paymentMethodTypeId map
                    if (orderPayPrefAvailableTotal.compareTo(ZERO) > 0) {
                        Map<String, Object> orderPayPrefDetails = new HashMap<String, Object>();
                        orderPayPrefDetails.put("orderPaymentPreference", orderPayPref);
                        orderPayPrefDetails.put("availableTotal", orderPayPrefAvailableTotal);
                        if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                            (prefSplitMap.get(paymentMethodTypeId)).add(orderPayPrefDetails);
                        } else {
                            prefSplitMap.put(paymentMethodTypeId, UtilMisc.toList(orderPayPrefDetails));
                        }
                    }
                }

                // Keep a decreasing total of the amount remaining to refund
                BigDecimal amountLeftToRefund = orderTotal.setScale(decimals, rounding);

                // This can be extended to support additional electronic types
                List<String> electronicTypes = UtilMisc.<String>toList("CREDIT_CARD", "EFT_ACCOUNT", "FIN_ACCOUNT", "GIFT_CARD");

                // Figure out if EXT_PAYPAL should be considered as an electronic type
                if (productStore != null) {
                    ExpressCheckoutEvents.CheckoutType payPalType = ExpressCheckoutEvents.determineCheckoutType(delegator, productStore.getString("productStoreId"));
                    if (!payPalType.equals(ExpressCheckoutEvents.CheckoutType.NONE)) {
                        electronicTypes.add("EXT_PAYPAL");
                    }
                }
                // This defines the ordered part of the sequence of refund processing
                List<String> orderedRefundPaymentMethodTypes = new LinkedList<String>();
                orderedRefundPaymentMethodTypes.add("EXT_BILLACT");
                orderedRefundPaymentMethodTypes.add("FIN_ACCOUNT");
                orderedRefundPaymentMethodTypes.add("GIFT_CARD");
                orderedRefundPaymentMethodTypes.add("CREDIT_CARD");
                orderedRefundPaymentMethodTypes.add("EFT_ACCOUNT");

                // Add all the other paymentMethodTypes, in no particular order
                List<GenericValue> otherPaymentMethodTypes = new LinkedList<GenericValue>();
                try {
                    otherPaymentMethodTypes = EntityQuery.use(delegator).from("PaymentMethodType")
                            .where(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.NOT_IN, orderedRefundPaymentMethodTypes))
                            .cache(true).queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot get PaymentMethodTypes", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "OrderOrderPaymentPreferencesCannotGetPaymentMethodTypes",
                            UtilMisc.toMap("errorString", e.toString()), locale));
                }
                List<String> fieldList = EntityUtil.getFieldListFromEntityList(otherPaymentMethodTypes, "paymentMethodTypeId", true);
                orderedRefundPaymentMethodTypes.addAll(fieldList);

                // Iterate through the specified sequence of paymentMethodTypes, refunding to the correct OrderPaymentPreferences
                //    as long as there's a positive amount remaining to refund
                Iterator<String> orpmtit = orderedRefundPaymentMethodTypes.iterator();
                while (orpmtit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                    String paymentMethodTypeId = orpmtit.next();
                    if (prefSplitMap.containsKey(paymentMethodTypeId)) {
                        List<Map<String, Object>> paymentMethodDetails = prefSplitMap.get(paymentMethodTypeId);

                        // Iterate through the OrderPaymentPreferences of this type
                        Iterator<Map<String, Object>> pmtppit = paymentMethodDetails.iterator();
                        while (pmtppit.hasNext() && amountLeftToRefund.compareTo(ZERO) == 1) {
                            Map<String, Object> orderPaymentPrefDetails = pmtppit.next();
                            GenericValue orderPaymentPreference = (GenericValue) orderPaymentPrefDetails.get("orderPaymentPreference");
                            BigDecimal orderPaymentPreferenceAvailable = (BigDecimal) orderPaymentPrefDetails.get("availableTotal");
                            GenericValue refundOrderPaymentPreference=null;

                            // Refund up to the maxAmount for the paymentPref, or whatever is left to refund if that's less than the maxAmount
                            BigDecimal amountToRefund = orderPaymentPreferenceAvailable.min(amountLeftToRefund);
                            // The amount actually refunded for the paymentPref, default to requested amount
                            BigDecimal amountRefunded = amountToRefund;

                            String paymentId = null;
                            String returnItemStatusId = "RETURN_COMPLETED";  // generally, the return item will be considered complete after this
                            // Call the refund service to refund the payment
                            if (electronicTypes.contains(paymentMethodTypeId)) {
                                try {
                                    Map<String, Object> serviceContext = UtilMisc.toMap("orderId", orderId,"userLogin", context.get("userLogin"));
                                    serviceContext.put("paymentMethodId", orderPaymentPreference.getString("paymentMethodId"));
                                    serviceContext.put("paymentMethodTypeId", orderPaymentPreference.getString("paymentMethodTypeId"));
                                    serviceContext.put("statusId", orderPaymentPreference.getString("statusId"));
                                    serviceContext.put("maxAmount", amountToRefund.setScale(decimals, rounding));
                                    String orderPaymentPreferenceNewId = null;
                                    Map<String, Object> result = dispatcher.runSync("createOrderPaymentPreference", serviceContext);
                                    orderPaymentPreferenceNewId = (String) result.get("orderPaymentPreferenceId");
                                    try {
                                        refundOrderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceNewId).queryOne();
                                    } catch (GenericEntityException e) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithTheRefundSeeLogs", locale));
                                    }
                                    serviceResult = dispatcher.runSync("refundPayment", UtilMisc.<String, Object>toMap("orderPaymentPreference", refundOrderPaymentPreference, "refundAmount", amountToRefund.setScale(decimals, rounding), "userLogin", userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError("Error in refund payment: " + ServiceUtil.getErrorMessage(serviceResult), module);
                                        continue;
                                    }
                                    // for electronic types such as CREDIT_CARD and EFT_ACCOUNT, use refundPayment service
                                    paymentId = (String) serviceResult.get("paymentId");
                                    amountRefunded = (BigDecimal) serviceResult.get("refundAmount");
                                } catch (GenericServiceException e) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemsWithTheRefundSeeLogs", locale));
                                }
                            } else if (paymentMethodTypeId.equals("EXT_BILLACT")) {
                                try {
                                    // for Billing Account refunds
                                    serviceResult = dispatcher.runSync("refundBillingAccountPayment",
                                            UtilMisc.<String, Object> toMap("orderPaymentPreference", orderPaymentPreference, "refundAmount",
                                                    amountToRefund.setScale(decimals, rounding), "userLogin", userLogin));
                                    if (ServiceUtil.isError(serviceResult) || ServiceUtil.isFailure(serviceResult)) {
                                        Debug.logError("Error in refund payment: " + ServiceUtil.getErrorMessage(serviceResult), module);
                                        continue;
                                    }
                                    paymentId = (String) serviceResult.get("paymentId");
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, "Problem running the refundPayment service", module);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                            "OrderProblemsWithTheRefundSeeLogs", locale));
                                }
                            } else {
                                // handle manual refunds
                                try {
                                    Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin, "amount", amountLeftToRefund, "statusId", "PMNT_NOT_PAID");
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
                            Map<String, Object> response = new HashMap<String, Object>();
                            if (UtilValidate.isNotEmpty(refundOrderPaymentPreference)) {
                                response.put("orderPaymentPreferenceId", refundOrderPaymentPreference.getString("orderPaymentPreferenceId"));
                            } else {
                                response.put("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                            }
                            response.put("responseAmount", amountRefunded.setScale(decimals, rounding));
                            response.put("responseDate", now);
                            response.put("userLogin", userLogin);
                            response.put("paymentId", paymentId);
                            if (paymentMethodTypeId.equals("EXT_BILLACT")) {
                                response.put("billingAccountId", orderReadHelper.getBillingAccount().getString("billingAccountId"));
                            }
                            Map<String, Object> serviceResults = null;
                            try {
                                serviceResults = dispatcher.runSync("createReturnItemResponse", response);
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                            "OrderProblemsCreatingReturnItemResponseEntity", locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problems creating new ReturnItemResponse entity", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderProblemsCreatingReturnItemResponseEntity", locale));
                            }
                            String responseId = (String) serviceResults.get("returnItemResponseId");

                            // Set the response on each item
                            for (GenericValue item : items) {
                                Map<String, Object> returnItemMap = UtilMisc.<String, Object>toMap("returnItemResponseId", responseId, "returnId", item.get("returnId"), "returnItemSeqId", item.get("returnItemSeqId"), "statusId", returnItemStatusId, "userLogin", userLogin);
                                //Debug.logInfo("Updating item status", module);
                                try {
                                    serviceResults = dispatcher.runSync("updateReturnItem", returnItemMap);
                                    if (ServiceUtil.isError(serviceResults)) {
                                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                                "OrderProblemUpdatingReturnItemReturnItemResponseId", locale), null, null, serviceResults);
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError("Problem updating the ReturnItem entity", module);
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                            "OrderProblemUpdatingReturnItemReturnItemResponseId", locale));
                                }

                                //Debug.logInfo("Item status and return status history created", module);
                            }

                            // Create the payment applications for the return invoice
                            try {
                                serviceResults = dispatcher.runSync("createPaymentApplicationsFromReturnItemResponse",
                                        UtilMisc.<String, Object>toMap("returnItemResponseId", responseId, "userLogin", userLogin));
                                if (ServiceUtil.isError(serviceResults)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                            "OrderProblemUpdatingReturnItemReturnItemResponseId", locale), null, null, serviceResults);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Problem creating PaymentApplication records for return invoice", module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderProblemUpdatingReturnItemReturnItemResponseId", locale));
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

    public static Map<String, Object> refundBillingAccountPayment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal refundAmount = (BigDecimal) context.get("refundAmount");
        Locale locale = (Locale) context.get("locale");

        GenericValue orderHeader = null;
        try {
            orderHeader = paymentPref.getRelatedOne("OrderHeader", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get OrderHeader from OrderPaymentPreference", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "OrderOrderPaymentCannotBeCreatedWithRelatedOrderHeader", locale) + e.toString());
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderOrderPaymentGatewayResponseCannotBeCreated", locale));
        }

        // Create the Payment record (parties reversed)
        Map<String, Object> paymentCtx = UtilMisc.<String, Object>toMap("paymentTypeId", "CUSTOMER_REFUND");
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
            Map<String, Object> paymentCreationResult = dispatcher.runSync("createPayment", paymentCtx);
            if (ServiceUtil.isError(paymentCreationResult)) {
                return paymentCreationResult;
            } else {
                paymentId = (String) paymentCreationResult.get("paymentId");
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "OrderOrderPaymentFailed", 
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (paymentId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "OrderOrderPaymentFailed", UtilMisc.toMap("errorString", ""), locale));
        }

        // if the original order was paid with a billing account, then go find the billing account from the order and associate this refund with that billing account
        // thus returning value to the billing account
        if ("EXT_BILLACT".equals(paymentPref.getString("paymentMethodTypeId"))) {
            GenericValue billingAccount = orh.getBillingAccount();
            if (UtilValidate.isNotEmpty(billingAccount.getString("billingAccountId"))) {
                try {
                    Map<String, Object> paymentApplResult = dispatcher.runSync("createPaymentApplication", UtilMisc.<String, Object>toMap("paymentId", paymentId, "billingAccountId", billingAccount.getString("billingAccountId"),
                                "amountApplied", refundAmount, "userLogin", userLogin));
                    if (ServiceUtil.isError(paymentApplResult)) {
                        return paymentApplResult;
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "OrderOrderPaymentApplicationFailed", 
                            UtilMisc.toMap("errorString", e.getMessage()), locale));
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("paymentId", paymentId);
        return result;
    }

    public static Map<String, Object> createPaymentApplicationsFromReturnItemResponse(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // the strategy for this service is to get a list of return invoices via the return items -> return item billing relationships
        // then split up the responseAmount among the invoices evenly
        String responseId = (String) context.get("returnItemResponseId");
        String errorMsg = "Failed to create payment applications for return item response [" + responseId + "]. ";
        try {
            GenericValue response = EntityQuery.use(delegator).from("ReturnItemResponse").where("returnItemResponseId", responseId).queryOne();
            if (response == null) {
                return ServiceUtil.returnError(errorMsg + "Return Item Response not found with ID [" + responseId + "].");
            }
            BigDecimal responseAmount = response.getBigDecimal("responseAmount").setScale(decimals, rounding);
            String paymentId = response.getString("paymentId");

            // for each return item in the response, get the list of return item billings and then a list of invoices
            Map<String, GenericValue> returnInvoices = new HashMap<String, GenericValue>(); // key is invoiceId, value is Invoice GenericValue
            List<GenericValue> items = response.getRelated("ReturnItem", null, null, false);
            for (GenericValue item : items) {
                List<GenericValue> billings = item.getRelated("ReturnItemBilling", null, null, false);
                for (GenericValue billing : billings) {
                    GenericValue invoice = billing.getRelatedOne("Invoice", false);

                    // put the invoice in the map if it doesn't already exist (a very loopy way of doing group by invoiceId without creating a view)
                    if (returnInvoices.get(invoice.getString("invoiceId")) == null) {
                        returnInvoices.put(invoice.getString("invoiceId"), invoice);
                    }
                }
            }

            // for each return invoice found, sum up the related billings
            Map<String, BigDecimal> invoiceTotals = new HashMap<String, BigDecimal>(); // key is invoiceId, value is the sum of all billings for that invoice
            BigDecimal grandTotal = ZERO; // The sum of all return invoice totals
            for (GenericValue invoice : returnInvoices.values()) {
                List<GenericValue> billings = invoice.getRelated("ReturnItemBilling", null, null, false);
                BigDecimal runningTotal = ZERO;
                for (GenericValue billing : billings) {
                    runningTotal = runningTotal.add(billing.getBigDecimal("amount").multiply(billing.getBigDecimal("quantity")).setScale(decimals, rounding));
                }

                invoiceTotals.put(invoice.getString("invoiceId"), runningTotal);
                grandTotal = grandTotal.add(runningTotal);
            }

            // now allocate responseAmount * invoiceTotal / grandTotal to each invoice
            for (GenericValue invoice : returnInvoices.values()) {
                String invoiceId = invoice.getString("invoiceId");
                BigDecimal invoiceTotal = invoiceTotals.get(invoiceId);

                BigDecimal amountApplied = responseAmount.multiply(invoiceTotal).divide(grandTotal, decimals, rounding).setScale(decimals, rounding);

                if (paymentId != null) {
                    // create a payment application for the invoice
                    Map<String, Object> input = UtilMisc.<String, Object>toMap("paymentId", paymentId, "invoiceId", invoice.getString("invoiceId"));
                    input.put("amountApplied", amountApplied);
                    input.put("userLogin", userLogin);
                    if (response.get("billingAccountId") != null) {
                        GenericValue billingAccount = response.getRelatedOne("BillingAccount", false);
                        if (billingAccount != null) {
                            input.put("billingAccountId", response.get("billingAccountId"));
                        }
                    }
                    Map<String, Object> serviceResults = dispatcher.runSync("createPaymentApplication", input);
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
    public static Map<String, Object> processReplacementReturn(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        String returnTypeId = (String) context.get("returnTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        GenericValue returnHeader = null;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem", UtilMisc.toMap("returnTypeId", returnTypeId), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorGettingReturnHeaderItemInformation", locale));
        }
        String returnHeaderTypeId = returnHeader.getString("returnHeaderTypeId");
        List<String> createdOrderIds = new LinkedList<String>();
        if (returnHeader != null && UtilValidate.isNotEmpty(returnItems)) {
            Map<String, List<GenericValue>> returnItemsByOrderId = new HashMap<String, List<GenericValue>>();
            Map<String, BigDecimal> totalByOrder = new HashMap<String, BigDecimal>();
            groupReturnItemsByOrder(returnItems, returnItemsByOrderId, totalByOrder, delegator, returnId, returnTypeId);

            // process each one by order
            for (Map.Entry<String, List<GenericValue>> entry : returnItemsByOrderId.entrySet()) {
                String orderId = entry.getKey();
                List<GenericValue> returnItemList = entry.getValue();

                // get order header & payment prefs
                GenericValue orderHeader = null;
                try {
                    orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot get Order details for #" + orderId, module);
                    continue;
                }

                OrderReadHelper orh = new OrderReadHelper(orderHeader);

                // create the replacement order
                Map<String, Object> orderMap = UtilMisc.<String, Object>toMap("userLogin", userLogin);

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
                List<GenericValue> contactMechs = new LinkedList<GenericValue>();
                List<GenericValue> orderCm = null;
                try {
                    orderCm = orderHeader.getRelated("OrderContactMech", null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (orderCm != null) {
                    for (GenericValue v : orderCm) {
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
                    orderSp = orderHeader.getRelated("OrderShipmentPreference", null, null, false);
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
                List<GenericValue> orderItems = new LinkedList<GenericValue>();
                List<GenericValue> orderItemShipGroupInfo = new LinkedList<GenericValue>();
                List<String> orderItemShipGroupIds = new LinkedList<String>(); // this is used to store the ship group ids of the groups already added to the orderItemShipGroupInfo list
                List<GenericValue> orderItemAssocs = new LinkedList<GenericValue>();
                if (returnItemList != null) {
                    int itemCount = 1;
                    for (GenericValue returnItem : returnItemList) {
                        GenericValue orderItem = null;
                        GenericValue product = null;
                        try {
                            orderItem = returnItem.getRelatedOne("OrderItem", false);
                            product = orderItem.getRelatedOne("Product", false);
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
                                            GenericValue refurbItemAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_REFURB"), UtilMisc.toList("sequenceNum"), false)));
                                            if (UtilValidate.isNotEmpty(refurbItemAssoc)) {
                                                refurbItem = refurbItemAssoc.getRelatedOne("AssocProduct", false);
                                            }
                                        }
                                    } catch (GenericEntityException e) {
                                        Debug.logError(e, module);
                                    }
                                    if (UtilValidate.isNotEmpty(refurbItem)) {
                                        boolean inventoryAvailable = false;
                                        try {
                                            Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.toMap("productStoreId", orderHeader.get("productStoreId"),
                                                                                                                                           "productId", refurbItem.getString("productId"),
                                                                                                                                           "product", refurbItem, "quantity", quantity));
                                            if (ServiceUtil.isError(invReqResult)) {
                                                Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invReqResult, module);
                                            } else {
                                                inventoryAvailable = "Y".equals(invReqResult.get("available"));
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
                                    newItem.set("itemDescription", ProductContentWrapper.getProductContentAsText(refurbItem, "PRODUCT_NAME", locale, null, "html"));
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
                                    orderItemShipGroupAssoc = EntityUtil.getFirst(orderItem.getRelated("OrderItemShipGroupAssoc", null, null, false));
                                    if (orderItemShipGroupAssoc != null) {
                                        if (!orderItemShipGroupIds.contains(orderItemShipGroupAssoc.getString("shipGroupSeqId"))) {
                                            GenericValue orderItemShipGroup = orderItemShipGroupAssoc.getRelatedOne("OrderItemShipGroup", false);
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
                                    List<GenericValue> repairItems = null;
                                    try {
                                        if (UtilValidate.isNotEmpty(product)) {
                                            repairItems = EntityUtil.filterByDate(product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_REPAIR_SRV"), UtilMisc.toList("sequenceNum"), false));
                                        }
                                    } catch (GenericEntityException e) {
                                        Debug.logError(e, module);
                                        continue;
                                    }
                                    if (UtilValidate.isNotEmpty(repairItems)) {
                                        for (GenericValue repairItem : repairItems) {
                                            GenericValue repairItemProduct = null;
                                            try {
                                                repairItemProduct = repairItem.getRelatedOne("AssocProduct", false);
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
                                                Map<String, Object> priceContext = new HashMap<String, Object>();
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
                                                Map<String, Object> priceResult = null;
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
                                                    newItem.set("unitListPrice", priceResult.get("listPrice"));
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
                                                newItem.set("itemDescription", ProductContentWrapper.getProductContentAsText(repairItemProduct, "PRODUCT_NAME", locale, null, "html"));
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
                        paymentMethod = returnHeader.getRelatedOne("PaymentMethod", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                    if (UtilValidate.isNotEmpty(paymentMethod)) {
                        String paymentMethodId = paymentMethod.getString("paymentMethodId");
                        String paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
                        GenericValue opp = delegator.makeValue("OrderPaymentPreference");
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
                    orderMap.put("orderTerms", orderHeader.getRelated("OrderTerm", null, null, false));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot create replacement order because order terms for original order are not available", module);
                }
                // we'll assume the new order has the same order roles of the original one
                try {
                    List<GenericValue> orderRoles = orderHeader.getRelated("OrderRole", null, null, false);
                    Map<String, List<String>> orderRolesMap = new HashMap<String, List<String>>();
                    if (orderRoles != null) {
                        for (GenericValue orderRole : orderRoles) {
                            List<String> parties = orderRolesMap.get(orderRole.getString("roleTypeId"));
                            if (parties == null) {
                                parties = new LinkedList<String>();
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
                Map<String, Object> orderResult = null;
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
                        Map<String, Object> serviceResult = null;
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
                        if ("CUSTOMER_RETURN".equals(returnHeaderTypeId)) {
                            OrderChangeHelper.approveOrder(dispatcher, userLogin, createdOrderId);
                        } else {
                            try {
                                OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, createdOrderId, "ORDER_APPROVED", null, "ITEM_APPROVED", null);
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Service invocation error, status changes were not updated for order #" + createdOrderId, module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderErrorCannotStoreStatusChanges", locale) + createdOrderId);
                            }
                        }
                    }

                    // create a ReturnItemResponse and attach to each ReturnItem
                    Map<String, Object> itemResponse = new HashMap<String, Object>();
                    itemResponse.put("replacementOrderId", createdOrderId);
                    itemResponse.put("responseAmount", orderPriceTotal);
                    itemResponse.put("responseDate", nowTimestamp);
                    itemResponse.put("userLogin", userLogin);
                    String returnItemResponseId = null;
                    try {
                        Map<String, Object> createReturnItemResponseResult = dispatcher.runSync("createReturnItemResponse", itemResponse);
                        if (ServiceUtil.isError(createReturnItemResponseResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderProblemCreatingReturnItemResponseRecord", locale), 
                                    null, null, createReturnItemResponseResult);
                        }
                        returnItemResponseId = (String) createReturnItemResponseResult.get("returnItemResponseId");
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem creating ReturnItemResponse record", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                "OrderProblemCreatingReturnItemResponseRecord", locale));
                    }

                    for (GenericValue returnItem : returnItemList) {
                        Map<String, Object> updateReturnItemCtx = new HashMap<String, Object>();
                        updateReturnItemCtx.put("returnId", returnId);
                        updateReturnItemCtx.put("returnItemSeqId", returnItem.get("returnItemSeqId"));
                        updateReturnItemCtx.put("returnItemResponseId", returnItemResponseId);
                        updateReturnItemCtx.put("userLogin", userLogin);
                        try {
                            Map<String, Object> updateReturnItemResult = dispatcher.runSync("updateReturnItem", updateReturnItemCtx);
                            if (ServiceUtil.isError(updateReturnItemResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                        "OrderProblemStoringReturnItemUpdates", locale), null, null, updateReturnItemResult);
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Could not update ReturnItem record", module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                                    "OrderProblemStoringReturnItemUpdates", locale));
                        }
                    }
                }
            }
        }

        // create a return message AND create ReturnItemResponse record(s)
        StringBuilder successMessage = new StringBuilder();
        if (createdOrderIds.size() > 0) {
            successMessage.append("The following new orders have been created : ");
            Iterator<String> i = createdOrderIds.iterator();
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

    public static Map<String, Object> processSubscriptionReturn(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Timestamp now = UtilDateTime.nowTimestamp();

        GenericValue returnHeader;
        List<GenericValue> returnItems = null;
        try {
            returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader != null) {
                returnItems = returnHeader.getRelated("ReturnItem", UtilMisc.toMap("returnTypeId", "RTN_REFUND"), null, false);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (returnItems != null) {
            for (GenericValue returnItem : returnItems) {
                String orderItemSeqId = returnItem.getString("orderItemSeqId");
                String orderId = returnItem.getString("orderId");

                // lookup subscriptions
                List<GenericValue> subscriptions;
                try {
                    subscriptions = EntityQuery.use(delegator).from("Subscription").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // cancel all current subscriptions
                if (subscriptions != null) {
                    for (GenericValue subscription : subscriptions) {
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
     * @param returnItems a List of return items
     * @param returnItemsByOrderId the return items by order id
     * @param totalByOrder the total by order id
     * @param delegator the delegator
     * @param returnId the return id
     * @param returnTypeId the return type id
     */
    public static void groupReturnItemsByOrder(List<GenericValue> returnItems, Map<String, List<GenericValue>> returnItemsByOrderId, Map<String, BigDecimal> totalByOrder, Delegator delegator, String returnId, String returnTypeId) {
        for (GenericValue returnItem : returnItems) {
            String orderId = returnItem.getString("orderId");
            if (orderId != null) {
                if (returnItemsByOrderId != null) {
                    BigDecimal totalForOrder = null;
                    if (totalByOrder != null) {
                        totalForOrder = totalByOrder.get(orderId);
                    }

                    List<GenericValue> returnItemList = returnItemsByOrderId.get(orderId);
                    if (returnItemList == null) {
                        returnItemList = new LinkedList<GenericValue>();
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
                        Map<String, Object> condition = UtilMisc.toMap("returnId", returnItem.get("returnId"), "returnItemSeqId", returnItem.get("returnItemSeqId"));
                        BigDecimal newTotal = existingTotal.add(thisTotal).add(getReturnAdjustmentTotal(delegator, condition));
                        totalByOrder.put(orderId, newTotal);
                    }
                }
            }
        }

        // We may also have some order-level adjustments, so we need to go through each order again and add those as well
        if ((totalByOrder != null) && (totalByOrder.keySet() != null)) {
            for (String orderId : totalByOrder.keySet()) {
                // find returnAdjustment for returnHeader
                Map<String, Object> condition = UtilMisc.<String, Object>toMap("returnId", returnId,
                                               "returnItemSeqId", org.ofbiz.common.DataModelConstants.SEQ_ID_NA,
                                               "returnTypeId", returnTypeId);
                BigDecimal existingTotal = (totalByOrder.get(orderId)).add(getReturnAdjustmentTotal(delegator, condition));
                totalByOrder.put(orderId, existingTotal);
            }
        }
    }


    public static Map<String, Object> getReturnAmountByOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");
        List<GenericValue> returnItems = null;
        Map<String, Object> returnAmountByOrder = new HashMap<String, Object>();
        try {
            returnItems = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId).queryList();

        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up return information", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderErrorGettingReturnHeaderItemInformation", locale));
        }
        if ((returnItems != null) && (returnItems.size() > 0)) {
            List<String> paymentList = new LinkedList<String>();
            for (GenericValue returnItem : returnItems) {
                String orderId = returnItem.getString("orderId");
                try {
                    GenericValue returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse", false);
                    if ((returnItemResponse != null) && (orderId != null)) {
                        // TODO should we filter on payment's status (PMNT_SENT,PMNT_RECEIVED)
                        GenericValue payment = returnItemResponse.getRelatedOne("Payment", false);
                        if ((payment != null) && (payment.getBigDecimal("amount") != null) &&
                                !paymentList.contains(payment.get("paymentId"))) {
                            UtilMisc.addToBigDecimalInMap(returnAmountByOrder, orderId, payment.getBigDecimal("amount"));
                            paymentList.add(payment.getString("paymentId"));  // make sure we don't add duplicated payment amount
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up return item related information", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderErrorGettingReturnHeaderItemInformation", locale));
                }
            }
        }
        return UtilMisc.<String, Object>toMap("orderReturnAmountMap", returnAmountByOrder);
    }

    public static Map<String, Object> checkPaymentAmountForRefund(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");
        Locale locale = (Locale) context.get("locale");
        Map<String, BigDecimal> returnAmountByOrder = null;
        Map<String, Object> serviceResult = null;
        //GenericValue orderHeader = null;
        try {
            serviceResult = dispatcher.runSync("getReturnAmountByOrder", org.ofbiz.base.util.UtilMisc.toMap("returnId", returnId));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem running the getReturnAmountByOrder service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, 
                    "OrderProblemsWithGetReturnAmountByOrder", locale));
        }
        if (ServiceUtil.isError(serviceResult)) {
            return ServiceUtil.returnError((String) serviceResult.get(ModelService.ERROR_MESSAGE));
        } else {
            returnAmountByOrder = UtilGenerics.checkMap(serviceResult.get("orderReturnAmountMap"));
        }

        if ((returnAmountByOrder != null) && (returnAmountByOrder.keySet() != null)) {
            for (String orderId : returnAmountByOrder.keySet()) {
                BigDecimal returnAmount = returnAmountByOrder.get(orderId);
                if (returnAmount == null) {
                    return ServiceUtil.returnError("No returnAmount found for order:" + orderId);
                }
                if (returnAmount.abs().compareTo(new BigDecimal("0.000001")) < 0) {
                    Debug.logError("Order [" + orderId + "] refund amount[ " + returnAmount + "] less than zero", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderReturnTotalCannotLessThanZero", locale));
                }
                OrderReadHelper helper = new OrderReadHelper(delegator, orderId);
                BigDecimal grandTotal = helper.getOrderGrandTotal();
                if (returnAmount.subtract(grandTotal).compareTo(new BigDecimal("0.01")) > 0) {
                    Debug.logError("Order [" + orderId + "] refund amount[ " + returnAmount + "] exceeds order total [" + grandTotal + "]", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                            "OrderRefundAmountExceedsOrderTotal", locale));
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createReturnAdjustment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderAdjustmentId = (String) context.get("orderAdjustmentId");
        String returnAdjustmentTypeId = (String) context.get("returnAdjustmentTypeId");
        String returnId = (String) context.get("returnId");
        String returnItemSeqId = (String) context.get("returnItemSeqId");
        String description = (String) context.get("description");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        GenericValue returnItemTypeMap = null;
        GenericValue orderAdjustment = null;
        GenericValue returnAdjustmentType = null;
        GenericValue orderItem = null;
        GenericValue returnItem = null;
        GenericValue returnHeader = null;

        // if orderAdjustment is not empty, then copy most return adjustment information from orderAdjustment's
        if (orderAdjustmentId != null) {
            try {
                orderAdjustment = EntityQuery.use(delegator).from("OrderAdjustment").where("orderAdjustmentId", orderAdjustmentId).queryOne();
                if (orderAdjustment == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "OrderCreateReturnAdjustmentNotFoundOrderAdjustment", 
                            UtilMisc.toMap("orderAdjustmentId", orderAdjustmentId), locale));
                }
                // get returnHeaderTypeId from ReturnHeader and then use it to figure out return item type mapping
                returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
                String returnHeaderTypeId = ((returnHeader != null) && (returnHeader.getString("returnHeaderTypeId") != null)) ? returnHeader.getString("returnHeaderTypeId") : "CUSTOMER_RETURN";
                returnItemTypeMap = EntityQuery.use(delegator).from("ReturnItemTypeMap").where("returnHeaderTypeId", returnHeaderTypeId, "returnItemMapKey", orderAdjustment.get("orderAdjustmentTypeId")).queryOne();
                returnAdjustmentType = returnItemTypeMap.getRelatedOne("ReturnAdjustmentType", false);
                if (returnAdjustmentType != null && UtilValidate.isEmpty(description)) {
                    description = returnAdjustmentType.getString("description");
                }
                if ((returnItemSeqId != null) && !("_NA_".equals(returnItemSeqId))) {
                    returnItem = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId, "returnItemSeqId", returnItemSeqId).queryOne();
                    Debug.logInfo("returnId:" + returnId + ",returnItemSeqId:" + returnItemSeqId, module);
                    orderItem = returnItem.getRelatedOne("OrderItem", false);
                } else {
                    // we don't have the returnItemSeqId but before we consider this
                    // an header adjustment we try to get a return item in this return
                    // associated to the same order item to which the adjustments refers (if any)
                    if (UtilValidate.isNotEmpty(orderAdjustment.getString("orderItemSeqId")) &&
                            !"_NA_".equals(orderAdjustment.getString("orderItemSeqId"))) {
                        returnItem = EntityQuery.use(delegator).from("ReturnItem")
                                .where("returnId", returnId, "orderId", orderAdjustment.getString("orderId"), "orderItemSeqId", orderAdjustment.getString("orderItemSeqId"))
                                .queryFirst();
                        if (UtilValidate.isNotEmpty(returnItem)) {
                            orderItem = returnItem.getRelatedOne("OrderItem", false);
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new GeneralRuntimeException(e.getMessage());
            }
            context.putAll(orderAdjustment.getAllFields());
            if (UtilValidate.isNotEmpty(amount)) {
                context.put("amount", amount);
            }
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
            Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,
                    "OrderCreateReturnAdjustment", UtilMisc.toMap("seqId", seqId), locale));
            result.put("returnAdjustmentId", seqId);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to store returnAdjustment", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderCreateReturnAdjustmentFailed", locale));
        }
    }

    public static Map<String, Object> updateReturnAdjustment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue returnItem = null;
        GenericValue returnAdjustment = null;
        String returnAdjustmentTypeId = null;
        BigDecimal amount;


        try {
            returnAdjustment = EntityQuery.use(delegator).from("ReturnAdjustment").where("returnAdjustmentId", context.get("returnAdjustmentId")).queryOne();
            if (returnAdjustment != null) {
                returnItem = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnAdjustment.get("returnId"), "returnItemSeqId", returnAdjustment.get("returnItemSeqId")).queryOne();
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

            Map<String, Object> result = null;
            if (UtilValidate.isNotEmpty(amount)) {
                returnAdjustment.setNonPKFields(context);
                returnAdjustment.set("amount", amount);
                delegator.store(returnAdjustment);
                Debug.logInfo("Update ReturnAdjustment with Id:" + context.get("returnAdjustmentId") + " to amount " + amount +" successfully.", module);
                result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,
                        "OrderUpdateReturnAdjustment", 
                        UtilMisc.toMap("returnAdjustmentId", context.get("returnAdjustmentId"), "amount", amount), locale));
            } else {
                result = ServiceUtil.returnSuccess();
            }
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to store returnAdjustment", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "OrderCreateReturnAdjustmentFailed", locale));
        }
    }

    //  used as a dispatch service, invoke different service based on the parameters passed in
    public static Map<String, Object> createReturnItemOrAdjustment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logInfo("createReturnItemOrAdjustment's context:" + context, module);
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        Debug.logInfo("orderItemSeqId:" + orderItemSeqId +"#", module);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isNotEmpty(orderItemSeqId) ? "createReturnItem" : "createReturnAdjustment";
        Debug.logInfo("serviceName:" + serviceName, module);
        try {
            Map<String, Object> inMap = dctx.makeValidContext(serviceName, "IN", context);
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
    public static Map<String, Object> updateReturnItemOrAdjustment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logInfo("updateReturnItemOrAdjustment's context:" + context, module);
        String returnAdjustmentId = (String) context.get("returnAdjustmentId");
        Debug.logInfo("returnAdjustmentId:" + returnAdjustmentId +"#", module);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        //if the request is to create returnItem, orderItemSeqId should not be empty
        String serviceName = UtilValidate.isEmpty(returnAdjustmentId) ? "updateReturnItem" : "updateReturnAdjustment";
        Debug.logInfo("serviceName:" + serviceName, module);
        try {
            return dispatcher.runSync(serviceName, dctx.makeValidContext(serviceName, "IN", context));
        } catch (org.ofbiz.service.GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * These return adjustment types need to be recalculated when the return item is updated
     * @param returnAdjustmentTypeId the return adjustment type id
     * @return returns if the returnn adjustment need to be recalculated
     */
    public static boolean needRecalculate(String returnAdjustmentTypeId) {
        return "RET_PROMOTION_ADJ".equals(returnAdjustmentTypeId) ||
                "RET_DISCOUNT_ADJ".equals(returnAdjustmentTypeId) ||
                "RET_SALES_TAX_ADJ".equals(returnAdjustmentTypeId);

    }

    /**
     * Get the total return adjustments for a set of key -> value condition pairs.  Done for code efficiency.
     * @param delegator the delegator
     * @param condition the conditions to use
     * @return return the total return adjustments
     */
    public static BigDecimal getReturnAdjustmentTotal(Delegator delegator, Map<String, ? extends Object> condition) {
        BigDecimal total = BigDecimal.ZERO;
        List<GenericValue> adjustments;
        try {
            // TODO: find on a view-entity with a sum is probably more efficient
            adjustments = EntityQuery.use(delegator).from("ReturnAdjustment").where(condition).queryList();
            if (adjustments != null) {
                for (GenericValue returnAdjustment : adjustments) {
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
     * @param serviceName the service name
     * @param context   context before clean up
     * @return filtered context
     * @throws GenericServiceException
     * 
     * @deprecated - Use DispatchContext.makeValidContext(String, String, Map) instead
     */
    @Deprecated
    public static Map<String, Object> filterServiceContext(DispatchContext dctx, String serviceName, Map<String, ? extends Object> context) throws GenericServiceException {
        return dctx.makeValidContext(serviceName, "IN", context);
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
            newAmount = returnTotal.multiply(amount).divide(originalTotal, decimals, rounding);
        } else {
            newAmount = ZERO;
        }
        return newAmount;
    }
}
