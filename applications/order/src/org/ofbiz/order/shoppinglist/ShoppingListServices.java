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
package org.ofbiz.order.shoppinglist;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.HashMap;
import java.sql.Timestamp;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.product.store.ProductStoreWorker;

/**
 * Shopping List Services
 */
public class ShoppingListServices {

    public static final String module = ShoppingListServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map setShoppingListRecurrence(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");
        Locale locale = (Locale) context.get("locale");

        if (frequency == null || interval == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderFrequencyOrIntervalWasNotSpecified", locale), module);
            return ServiceUtil.returnSuccess();
        }

        if (startDate == null) {
            switch (frequency.intValue()) {
                case 5:
                    startDate = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                case 6:
                    startDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                case 7:
                    startDate = UtilDateTime.getYearStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                default:
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderInvalidFrequencyForShoppingListRecurrence",locale));
            }
        }

        long startTime = startDate.getTime();
        long endTime = 0;
        if (endDate != null) {
            endTime = endDate.getTime();
        }

        RecurrenceInfo recInfo = null;
        try {
            recInfo = RecurrenceInfo.makeInfo(delegator, startTime, frequency.intValue(), interval.intValue(), -1, endTime);
        } catch (RecurrenceInfoException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToCreateShoppingListRecurrenceInformation",locale));
        }

        Debug.log("Next Recurrence - " + UtilDateTime.getTimestamp(recInfo.next()), module);
        Map result = ServiceUtil.returnSuccess();
        result.put("recurrenceInfoId", recInfo.getID());

        return result;
    }

    public static Map createListReorders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
        
            List exprs = UtilMisc.toList(new EntityExpr("shoppingListTypeId", EntityOperator.EQUALS, "SLT_AUTO_REODR"),
                    new EntityExpr("isActive", EntityOperator.EQUALS, "Y"));
            EntityCondition cond = new EntityConditionList(exprs, EntityOperator.AND);
            List order = UtilMisc.toList("-lastOrderedDate");

            EntityListIterator eli = null;
            eli = delegator.findListIteratorByCondition("ShoppingList", cond, null, order);
    
            if (eli != null) {
                GenericValue shoppingList;
                while (((shoppingList = (GenericValue) eli.next()) != null)) {
                    Timestamp lastOrder = shoppingList.getTimestamp("lastOrderedDate");
                    GenericValue recurrenceInfo = null;
                    recurrenceInfo = shoppingList.getRelatedOne("RecurrenceInfo");
    
                    Timestamp startDateTime = recurrenceInfo.getTimestamp("startDateTime");
                    RecurrenceInfo recurrence = null;
                    if (recurrenceInfo != null) {
                        try {
                            recurrence = new RecurrenceInfo(recurrenceInfo);
                        } catch (RecurrenceInfoException e) {
                            Debug.logError(e, module);
                        }
                    }
    
                    // check the next recurrence
                    if (recurrence != null) {
                        long next = lastOrder == null ? recurrence.next(startDateTime.getTime()) : recurrence.next(lastOrder.getTime());
                        Timestamp now = UtilDateTime.nowTimestamp();
                        Timestamp nextOrder = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(next));
    
                        if (nextOrder.after(now)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
    
                    ShoppingCart listCart = makeShoppingListCart(dispatcher, shoppingList, locale);
                    CheckOutHelper helper = new CheckOutHelper(dispatcher, delegator, listCart);
    
                    // store the order
                    Map createResp = helper.createOrder(userLogin);
                    if (createResp != null && ServiceUtil.isError(createResp)) {
                        Debug.logError("Cannot create order for shopping list - " + shoppingList, module);
                    } else {
                        String orderId = (String) createResp.get("orderId");
    
                        // authorize the payments
                        Map payRes = null;
                        try {
                            payRes = helper.processPayment(ProductStoreWorker.getProductStore(listCart.getProductStoreId(), delegator), userLogin);
                        } catch (GeneralException e) {
                            Debug.logError(e, module);
                        }
    
                        if (payRes != null && ServiceUtil.isError(payRes)) {
                            Debug.logError("Payment processing problems with shopping list - " + shoppingList, module);
                        }
    
                        shoppingList.set("lastOrderedDate", UtilDateTime.nowTimestamp());
                        shoppingList.store();
    
                        // send notification
                        try {
                            dispatcher.runAsync("sendOrderPayRetryNotification", UtilMisc.toMap("orderId", orderId));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, module);
                        }
    
                        // increment the recurrence
                        recurrence.incrementCurrentCount();
                    }
                }
    
                eli.close();
            }
            
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error creating shopping list auto-reorders", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }

            String errMsg = "Error while creating new shopping list based automatic reorder" + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based automatic reorder", module);
            }
        }
    }

    public static Map splitShipmentMethodString(DispatchContext dctx, Map context) {
        String shipmentMethodString = (String) context.get("shippingMethodString");
        Map result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(shipmentMethodString)) {
            int delimiterPos = shipmentMethodString.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                result.put("shipmentMethodTypeId", shipmentMethodTypeId);
                result.put("carrierPartyId", carrierPartyId);
            }
        }
        return result;
    }

    public static Map makeListFromOrder(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        String shoppingListTypeId = (String) context.get("shoppingListTypeId");
        String shoppingListId = (String) context.get("shoppingListId");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");

        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
        
            GenericValue orderHeader = null;
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
    
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLocateOrder", UtilMisc.toMap("orderId",orderId), locale));
            }
            String productStoreId = orderHeader.getString("productStoreId");
    
            if (UtilValidate.isEmpty(shoppingListId)) {
                // create a new shopping list
                if (partyId == null) {
                    partyId = userLogin.getString("partyId");
                }
    
                Map serviceCtx = UtilMisc.toMap("userLogin", userLogin, "partyId", partyId,
                        "productStoreId", productStoreId, "listName", "List Created From Order #" + orderId);
    
                if (UtilValidate.isNotEmpty(shoppingListTypeId)) {
                    serviceCtx.put("shoppingListTypeId", shoppingListTypeId);
                }
    
                Map newListResult = null;
                try {
    
                    newListResult = dispatcher.runSync("createShoppingList", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problems creating new ShoppingList", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToCreateNewShoppingList",locale));
                }
    
                // check for errors
                if (ServiceUtil.isError(newListResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(newListResult));
                }
    
                // get the new list id
                if (newListResult != null) {
                    shoppingListId = (String) newListResult.get("shoppingListId");
                }
            }
    
            GenericValue shoppingList = null;
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));
    
            if (shoppingList == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoShoppingListAvailable",locale));
            }
            shoppingListTypeId = shoppingList.getString("shoppingListTypeId");
    
            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            if (orh == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLoadOrderReadHelper", UtilMisc.toMap("orderId",orderId), locale));
            }
    
            List orderItems = orh.getOrderItems();
            Iterator i = orderItems.iterator();
            while (i.hasNext()) {
                GenericValue orderItem = (GenericValue) i.next();
                if (orderItem.get("productId") != null) {
                    Map ctx = UtilMisc.toMap("userLogin", userLogin, "shoppingListId", shoppingListId, "productId",
                            orderItem.get("productId"), "quantity", orderItem.get("quantity"));
                    Map serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("createShoppingListItem", ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }
                    if (serviceResult == null || ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToAddItemToShoppingList",UtilMisc.toMap("shoppingListId",shoppingListId), locale));
                    }
                }
            }
    
            if ("SLT_AUTO_REODR".equals(shoppingListTypeId)) {
                GenericValue paymentPref = EntityUtil.getFirst(orh.getPaymentPreferences());
                GenericValue shipGroup = EntityUtil.getFirst(orh.getOrderItemShipGroups());
    
                Map slCtx = new HashMap();
                slCtx.put("shipmentMethodTypeId", shipGroup.get("shipmentMethodTypeId"));
                slCtx.put("carrierRoleTypeId", shipGroup.get("carrierRoleTypeId"));
                slCtx.put("carrierPartyId", shipGroup.get("carrierPartyId"));
                slCtx.put("contactMechId", shipGroup.get("contactMechId"));
                slCtx.put("paymentMethodId", paymentPref.get("paymentMethodId"));
                slCtx.put("currencyUom", orh.getCurrency());
                slCtx.put("startDateTime", startDate);
                slCtx.put("endDateTime", endDate);
                slCtx.put("frequency", frequency);
                slCtx.put("intervalNumber", interval);
                slCtx.put("isActive", "Y");
                slCtx.put("shoppingListId", shoppingListId);
                slCtx.put("userLogin", userLogin);
    
                Map slUpResp = null;
                try {
                    slUpResp = dispatcher.runSync("updateShoppingList", slCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
    
                if (slUpResp == null || ServiceUtil.isError(slUpResp)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToUpdateShoppingListInformation",UtilMisc.toMap("shoppingListId",shoppingListId), locale));
                }
            }
    
            Map result = ServiceUtil.returnSuccess();
            result.put("shoppingListId", shoppingListId);
            return result;
            
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error making shopping list from order", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }

            String errMsg = "Error while creating new shopping list based on order" + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based on order", module);
            }
        }
    }

    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        GenericDelegator delegator = dispatcher.getDelegator();
        ShoppingCart listCart = null;
        if (shoppingList != null && shoppingList.get("productStoreId") != null) {
            String productStoreId = shoppingList.getString("productStoreId");
            String currencyUom = shoppingList.getString("currencyUom");
            if (currencyUom == null) {
                GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                if (productStore == null) {
                    return null;
                }
                currencyUom = productStore.getString("defaultCurrencyUomId");
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }

            List items = null;
            try {
                items = shoppingList.getRelated("ShoppingListItem", UtilMisc.toList("shoppingListItemSeqId"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (UtilValidate.isNotEmpty(items)) {
                listCart = new ShoppingCart(delegator, productStoreId, locale, currencyUom);
                listCart.setOrderPartyId(shoppingList.getString("partyId"));
                listCart.setAutoOrderShoppingListId(shoppingList.getString("shoppingListId"));

                Iterator i = items.iterator();
                while (i.hasNext()) {
                    GenericValue shoppingListItem = (GenericValue) i.next();
                    String productId = shoppingListItem.getString("productId");
                    Double quantity = shoppingListItem.getDouble("quantity");
                    Timestamp reservStart = shoppingListItem.getTimestamp("reservStart");
                    Double reservLength = null;
                    if (shoppingListItem.get("reservLength") != null) {
                        reservLength = shoppingListItem.getDouble("reservLength");
                    }
                    Double reservPersons = null;;
                    if (shoppingListItem.get("reservPersons") != null) {
                        reservPersons = shoppingListItem.getDouble("reservPersons");
                    }
                    if (UtilValidate.isNotEmpty(productId) && quantity != null) {
                        // list items are noted in the shopping cart
                        String listId = shoppingListItem.getString("shoppingListId");
                        String itemId = shoppingListItem.getString("shoppingListItemSeqId");
                        Map attributes = UtilMisc.toMap("shoppingListId", listId, "shoppingListItemSeqId", itemId);

                        try { 
                            listCart.addOrIncreaseItem(productId, null, quantity.doubleValue(), reservStart, reservLength, reservPersons, null, null, null, attributes, null, null, null, null, null, dispatcher);
                        } catch (CartItemModifyException e) {
                            Debug.logError(e, "Unable to add product to List Cart - " + productId, module);
                        } catch (ItemNotFoundException e) {
                            Debug.logError(e, "Product not found - " + productId, module);
                        }
                    }
                }

                if (listCart.size() > 0) {
                    if (UtilValidate.isNotEmpty(shoppingList.get("paymentMethodId"))) {
                        listCart.addPayment(shoppingList.getString("paymentMethodId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("contactMechId"))) {
                        listCart.setShippingContactMechId(0, shoppingList.getString("contactMechId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("shipmentMethodTypeId"))) {
                        listCart.setShipmentMethodTypeId(0, shoppingList.getString("shipmentMethodTypeId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("carrierPartyId"))) {
                        listCart.setCarrierPartyId(0, shoppingList.getString("carrierPartyId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.getString("productPromoCodeId"))) {
                        listCart.addProductPromoCode(shoppingList.getString("productPromoCodeId"), dispatcher);
                    }
                }
            }
        }
        return listCart;
    }

    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, String shoppingListId, Locale locale) {
        GenericDelegator delegator = dispatcher.getDelegator();
        GenericValue shoppingList = null;
        try {
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return makeShoppingListCart(dispatcher, shoppingList, locale);
    }

    /**
     * 
     * Given an orderId, this service will look through all its OrderItems and for each shoppingListItemId
     * and shoppingListItemSeqId, update the quantity purchased in the ShoppingListItem entity.  Used for
     * tracking how many of shopping list items are purchased.  This service is mounted as a seca on storeOrder.
     *
     * @param ctx - The DispatchContext that this service is operating in
     * @param context - Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map updateShoppingListQuantitiesFromOrder(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        try {
            List orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
            Iterator iter = orderItems.iterator();
            while (iter.hasNext()) {
                GenericValue orderItem = (GenericValue) iter.next();
                String shoppingListId = orderItem.getString("shoppingListId");
                String shoppingListItemSeqId = orderItem.getString("shoppingListItemSeqId");
                if ((shoppingListId != null) && (shoppingListId.length() > 0)) {
                    GenericValue shoppingListItem=delegator.findByPrimaryKey("ShoppingListItem", UtilMisc.toMap("shoppingListId",
                                shoppingListId, "shoppingListItemSeqId", shoppingListItemSeqId));
                    if (shoppingListItem != null) {
                        Double quantityPurchased = shoppingListItem.getDouble("quantityPurchased");
                        Double orderQuantity = orderItem.getDouble("quantity");
                        if (quantityPurchased != null) {
                            shoppingListItem.set("quantityPurchased", new Double(orderQuantity.doubleValue() + quantityPurchased.doubleValue()));
                        }else{
                            shoppingListItem.set("quantityPurchased", orderQuantity);
                        }
                        shoppingListItem.store();
                    }
                }
            }
        } catch (Exception e) {
            Debug.log("updateShoppingListQuantitiesFromOrder error:"+e.getMessage());
        }
        return result;
    }
}
