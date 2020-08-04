/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.order.order;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Order Helper - Helper Methods For Non-Read Actions
 */
public final class OrderChangeHelper {

    private static final String MODULE = OrderChangeHelper.class.getName();

    private OrderChangeHelper() { }

    public static boolean approveOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) {
        return approveOrder(dispatcher, userLogin, orderId, false);
    }

    public static boolean approveOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId, boolean holdOrder) {
        GenericValue productStore = OrderReadHelper.getProductStoreFromOrder(dispatcher.getDelegator(), orderId);
        if (productStore == null) {
            throw new IllegalArgumentException("Could not find ProductStore for orderId [" + orderId + "], cannot approve order.");
        }

        // interal status for held orders
        String headerStatus = "ORDER_PROCESSING";
        String itemStatus = "ITEM_CREATED";
        String digitalItemStatus = "ITEM_APPROVED";

        if (!holdOrder) {
            if (productStore.get("headerApprovedStatus") != null) {
                headerStatus = productStore.getString("headerApprovedStatus");
            }
            if (productStore.get("itemApprovedStatus") != null) {
                itemStatus = productStore.getString("itemApprovedStatus");
            }
            if (productStore.get("digitalItemApprovedStatus") != null) {
                digitalItemStatus = productStore.getString("digitalItemApprovedStatus");
            }
        }

        try {
            OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, orderId, headerStatus, "ITEM_CREATED", itemStatus, digitalItemStatus);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service invocation error, status changes were not updated for order #" + orderId, MODULE);
            return false;
        }

        return true;
    }

    public static boolean rejectOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) {
        GenericValue productStore = OrderReadHelper.getProductStoreFromOrder(dispatcher.getDelegator(), orderId);
        String headerStatus = "ORDER_REJECTED";
        String itemStatus = "ITEM_REJECTED";
        if (productStore.get("headerDeclinedStatus") != null) {
            headerStatus = productStore.getString("headerDeclinedStatus");
        }
        if (productStore.get("itemDeclinedStatus") != null) {
            itemStatus = productStore.getString("itemDeclinedStatus");
        }

        try {
            OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, orderId, headerStatus, null, itemStatus, null);
            OrderChangeHelper.cancelInventoryReservations(dispatcher, userLogin, orderId);
            OrderChangeHelper.releasePaymentAuthorizations(dispatcher, userLogin, orderId);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service invocation error, status changes were not updated for order #" + orderId, MODULE);
            return false;
        }
        return true;
    }

    public static boolean completeOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) {
        try {
            OrderChangeHelper.createReceivedPayments(dispatcher, userLogin, orderId);
            OrderChangeHelper.createOrderInvoice(dispatcher, userLogin, orderId);
            OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, orderId, "ORDER_COMPLETED", "ITEM_APPROVED", "ITEM_COMPLETED", null);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return false;
        }
        return true;
    }

    public static boolean cancelOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) {
        GenericValue productStore = OrderReadHelper.getProductStoreFromOrder(dispatcher.getDelegator(), orderId);
        String headerStatus = "ORDER_CANCELLED";
        String itemStatus = "ITEM_CANCELLED";
        if (productStore.get("headerCancelStatus") != null) {
            headerStatus = productStore.getString("headerCancelStatus");
        }
        if (productStore.get("itemCancelStatus") != null) {
            itemStatus = productStore.getString("itemCancelStatus");
        }

        try {
            OrderChangeHelper.orderStatusChanges(dispatcher, userLogin, orderId, headerStatus, null, itemStatus, null);
            OrderChangeHelper.cancelInventoryReservations(dispatcher, userLogin, orderId);
            OrderChangeHelper.releasePaymentAuthorizations(dispatcher, userLogin, orderId);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service invocation error, status changes were not updated for order #" + orderId, MODULE);
            return false;
        }
        return true;
    }

    public static void orderStatusChanges(LocalDispatcher dispatcher, GenericValue userLogin, String orderId, String orderStatus, String fromItemStatus, String toItemStatus, String digitalItemStatus) throws GenericServiceException {
        // set the status on the order header
        Map<String, Object> statusFields = UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", orderStatus, "userLogin", userLogin);
        Map<String, Object> statusResult = dispatcher.runSync("changeOrderStatus", statusFields);
        if (ServiceUtil.isError(statusResult)) {
            String errorMessage = ServiceUtil.getErrorMessage(statusResult);
            Debug.logError(errorMessage, MODULE);
            throw new GenericServiceException(errorMessage);
        }

        // set the status on the order item(s)
        Map<String, Object> itemStatusFields = UtilMisc.<String, Object>toMap("orderId", orderId, "statusId", toItemStatus, "userLogin", userLogin);
        if (fromItemStatus != null) {
            itemStatusFields.put("fromStatusId", fromItemStatus);
        }
        Map<String, Object> itemStatusResult = dispatcher.runSync("changeOrderItemStatus", itemStatusFields);
        if (ServiceUtil.isError(itemStatusResult)) {
            String errorMessage = ServiceUtil.getErrorMessage(itemStatusResult);
            Debug.logError(errorMessage, MODULE);
            throw new GenericServiceException(errorMessage);
        }

        // now set the status for digital items
        if (digitalItemStatus != null && !digitalItemStatus.equals(toItemStatus)) {
            Delegator delegator = dispatcher.getDelegator();
            GenericValue orderHeader = null;
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Unable to get OrderHeader for OrderID : " + orderId, MODULE);
            }
            if (orderHeader != null) {
                List<GenericValue> orderItems = null;
                try {
                    orderItems = orderHeader.getRelated("OrderItem", null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "ERROR: Unable to get OrderItem records for OrderHeader : " + orderId, MODULE);
                }
                if (UtilValidate.isNotEmpty(orderItems)) {
                    for (GenericValue orderItem : orderItems) {
                        String orderItemSeqId = orderItem.getString("orderItemSeqId");
                        GenericValue product = null;

                        try {
                            product = orderItem.getRelatedOne("Product", false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "ERROR: Unable to get Product record for OrderItem : " + orderId + "/" + orderItemSeqId, MODULE);
                        }
                        if (product != null) {
                            GenericValue productType = null;
                            try {
                                productType = product.getRelatedOne("ProductType", false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "ERROR: Unable to get ProductType from Product : " + product, MODULE);
                            }
                            if (productType != null) {
                                String isDigital = productType.getString("isDigital");
                                if (isDigital != null && "Y".equalsIgnoreCase(isDigital)) {
                                    // update the status
                                    Map<String, Object> digitalStatusFields = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "statusId", digitalItemStatus, "userLogin", userLogin);
                                    Map<String, Object> digitalStatusChange = dispatcher.runSync("changeOrderItemStatus", digitalStatusFields);
                                    if (ServiceUtil.isError(digitalStatusChange)) {
                                        String errorMessage = ServiceUtil.getErrorMessage(digitalStatusChange);
                                        Debug.logError(errorMessage, MODULE);
                                        throw new GenericServiceException(errorMessage);
                                    }
                                }
                            }
                        } else {
                            String orderItemType = orderItem.getString("orderItemTypeId");
                            if (!"PRODUCT_ORDER_ITEM".equals(orderItemType)) {
                                // non-product items don't ship; treat as a digital item
                                Map<String, Object> digitalStatusFields = UtilMisc.<String, Object>toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "statusId", digitalItemStatus, "userLogin", userLogin);
                                Map<String, Object> digitalStatusChange = dispatcher.runSync("changeOrderItemStatus", digitalStatusFields);
                                if (ServiceUtil.isError(digitalStatusChange)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(digitalStatusChange);
                                    Debug.logError(errorMessage, MODULE);
                                    throw new GenericServiceException(errorMessage);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void cancelInventoryReservations(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) throws GenericServiceException {
        // cancel the inventory reservations
        Map<String, Object> cancelInvFields = UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin);
        Map<String, Object> cancelInvResult = dispatcher.runSync("cancelOrderInventoryReservation", cancelInvFields);
        if (ServiceUtil.isError(cancelInvResult)) {
            String errorMessage = ServiceUtil.getErrorMessage(cancelInvResult);
            Debug.logError(errorMessage, MODULE);
            throw new GenericServiceException(errorMessage);
        }
    }

    public static void releasePaymentAuthorizations(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) throws GenericServiceException {
        Map<String, Object> releaseFields = UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin);
        Map<String, Object> releaseResult = dispatcher.runSync("releaseOrderPayments", releaseFields);
        if (ServiceUtil.isError(releaseResult)) {
            String errorMessage = ServiceUtil.getErrorMessage(releaseResult);
            Debug.logError(errorMessage, MODULE);
            throw new GenericServiceException(errorMessage);
        }
    }

    public static void createReceivedPayments(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) throws GenericServiceException {
        GenericValue orderHeader = null;
        try {
            orderHeader = dispatcher.getDelegator().findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            GenericValue btparty = orh.getBillToParty();
            String partyId = "_NA_";
            if (btparty != null) {
                partyId = btparty.getString("partyId");
            }

            List<GenericValue> opps = orh.getPaymentPreferences();
            for (GenericValue opp : opps) {
                if ("PAYMENT_RECEIVED".equals(opp.getString("statusId"))) {
                    List<GenericValue> payments = orh.getOrderPayments(opp);
                    if (UtilValidate.isEmpty(payments)) {
                        // only do this one time; if we have payment already for this pref ignore.
                        Map<String, Object> results = dispatcher.runSync("createPaymentFromPreference",
                                UtilMisc.<String, Object>toMap("userLogin", userLogin, "orderPaymentPreferenceId", opp.getString("orderPaymentPreferenceId"),
                                "paymentRefNum", UtilDateTime.nowTimestamp().toString(), "paymentFromId", partyId));
                        if (ServiceUtil.isError(results)) {
                            String errorMessage = ServiceUtil.getErrorMessage(results);
                            Debug.logError(errorMessage, MODULE);
                            throw new GenericServiceException(errorMessage);
                        }
                    }
                }
            }
        }
    }

    public static void createOrderInvoice(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) throws GenericServiceException {
        GenericValue orderHeader = null;
        try {
            orderHeader = dispatcher.getDelegator().findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (orderHeader != null) {
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            List<GenericValue> items = orh.getOrderItems();

            Map<String, Object> serviceParam = UtilMisc.<String, Object>toMap("orderId", orderId, "billItems", items, "userLogin", userLogin);
            Map<String, Object> serviceRes = dispatcher.runSync("createInvoiceForOrder", serviceParam);
            if (ServiceUtil.isError(serviceRes)) {
                String errorMessage = ServiceUtil.getErrorMessage(serviceRes);
                Debug.logError(errorMessage, MODULE);
                throw new GenericServiceException(errorMessage);
            }
        }
    }
}
