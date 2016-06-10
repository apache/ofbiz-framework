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

import java.math.BigDecimal;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.accounting.payment.*;
import org.ofbiz.order.order.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.store.*;


orderId = parameters.orderId;
orderHeader = null;
// we have a special case here where for an anonymous order the user will already be logged out, but the userLogin will be in the request so we can still do a security check here
if (!userLogin) {
    userLogin = parameters.temporaryAnonymousUserLogin;
    // This is another special case, when Order is placed by anonymous user from ecommerce and then Order is completed by admin(or any user) from Order Manager
    // then userLogin is not found when Order Complete Mail is send to user.
    if (!userLogin) {
        if (orderId) {
            orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
            orderStatuses = orderHeader.getRelated("OrderStatus", null, null, false);
            filteredOrderStatusList = [];
            extOfflineModeExists = false;
            
            // Handled the case of OFFLINE payment method. In case of OFFLINE payment "ORDER_CREATED" status must be checked.
            orderPaymentPreferences = orderHeader.getRelated("OrderPaymentPreference", null, UtilMisc.toList("orderPaymentPreferenceId"), false);
            filteredOrderPaymentPreferences = EntityUtil.filterByCondition(orderPaymentPreferences, EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.IN, ["EXT_OFFLINE"]));
            if (filteredOrderPaymentPreferences) {
                extOfflineModeExists = true;
            }
            if (extOfflineModeExists) {
                filteredOrderStatusList = EntityUtil.filterByCondition(orderStatuses, EntityCondition.makeCondition("statusId", EntityOperator.IN, ["ORDER_COMPLETED", "ORDER_APPROVED", "ORDER_CREATED"]));
            } else {
                filteredOrderStatusList = EntityUtil.filterByCondition(orderStatuses, EntityCondition.makeCondition("statusId", EntityOperator.IN, ["ORDER_COMPLETED", "ORDER_APPROVED"]));
            }            
            if (UtilValidate.isNotEmpty(filteredOrderStatusList)) {
                if (filteredOrderStatusList.size() < 2) {
                    statusUserLogin = EntityUtil.getFirst(filteredOrderStatusList).statusUserLogin;
                    userLogin = from("UserLogin").where("userLoginId", statusUserLogin).queryOne();
                } else {
                    filteredOrderStatusList.each { orderStatus ->
                        if ("ORDER_COMPLETED".equals(orderStatus.statusId)) {
                            statusUserLogin = orderStatus.statusUserLogin;
                            userLogin = from("UserLogin").where("userLoginId", statusUserLogin).queryOne();
                        }
                    }
                }
            }
        }
    }
    context.userLogin = userLogin;
}

/* partyId = null;
if (userLogin) partyId = userLogin.partyId; */

partyId = context.partyId;
if (userLogin) {
    if (!partyId) {
        partyId = userLogin.partyId;
    }
}


// can anybody view an anonymous order?  this is set in the screen widget and should only be turned on by an email confirmation screen
allowAnonymousView = context.allowAnonymousView;

isDemoStore = true;
if (orderId) {
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
    if ("PURCHASE_ORDER".equals(orderHeader?.orderTypeId)) {
        //drop shipper or supplier
        roleTypeId = "SUPPLIER_AGENT";
    } else {
        //customer
        roleTypeId = "PLACING_CUSTOMER";
    }
    context.roleTypeId = roleTypeId;
    // check OrderRole to make sure the user can view this order.  This check must be done for any order which is not anonymously placed and
    // any anonymous order when the allowAnonymousView security flag (see above) is not set to Y, to prevent peeking
    if (orderHeader && (!"anonymous".equals(orderHeader.createdBy) || ("anonymous".equals(orderHeader.createdBy) && !"Y".equals(allowAnonymousView)))) {
        orderRole = from("OrderRole").where("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId).queryFirst();

        if (!userLogin || !orderRole) {
            context.remove("orderHeader");
            orderHeader = null;
            Debug.logWarning("Warning: in OrderStatus.groovy before getting order detail info: role not found or user not logged in; partyId=[" + partyId + "], userLoginId=[" + (userLogin == null ? "null" : userLogin.get("userLoginId")) + "]", "orderstatus");
        }
    }
}

if (orderHeader) {
    productStore = orderHeader.getRelatedOne("ProductStore", true);
    if (productStore) isDemoStore = !"N".equals(productStore.isDemoStore);

    orderReadHelper = new OrderReadHelper(orderHeader);
    orderItems = orderReadHelper.getOrderItems();
    orderAdjustments = orderReadHelper.getAdjustments();
    orderHeaderAdjustments = orderReadHelper.getOrderHeaderAdjustments();
    orderSubTotal = orderReadHelper.getOrderItemsSubTotal();
    orderItemShipGroups = orderReadHelper.getOrderItemShipGroups();
    headerAdjustmentsToShow = orderReadHelper.getOrderHeaderAdjustmentsToShow();

    orderShippingTotal = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true);
    orderShippingTotal = orderShippingTotal.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true));

    orderTaxTotal = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, true, false);
    orderTaxTotal = orderTaxTotal.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, true, false));

    placingCustomerOrderRole = from("OrderRole").where("orderId", orderId, "roleTypeId", roleTypeId).queryFirst();
    placingCustomerPerson = placingCustomerOrderRole == null ? null : from("Person").where("partyId", placingCustomerOrderRole.partyId).queryOne();

    billingAccount = orderHeader.getRelatedOne("BillingAccount", false);

    orderPaymentPreferences = EntityUtil.filterByAnd(orderHeader.getRelated("OrderPaymentPreference", null, null, false), [EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED")]);
    paymentMethods = [];
    orderPaymentPreferences.each { opp ->
        paymentMethod = opp.getRelatedOne("PaymentMethod", false);
        if (paymentMethod) {
            paymentMethods.add(paymentMethod);
        } else {
            paymentMethodType = opp.getRelatedOne("PaymentMethodType", false);
            if (paymentMethodType) {
                context.paymentMethodType = paymentMethodType;
            }
        }
    }


    payToPartyId = productStore.payToPartyId;
    paymentAddress =  PaymentWorker.getPaymentAddress(delegator, payToPartyId);
    if (paymentAddress) context.paymentAddress = paymentAddress;

    // get Shipment tracking info
    orderShipmentInfoSummaryList = select("shipmentId", "shipmentRouteSegmentId", "carrierPartyId", "shipmentMethodTypeId","shipmentPackageSeqId","trackingCode","boxNumber")
                                    .from("OrderShipmentInfoSummary")
                                    .where("orderId", orderId)
                                    .orderBy("shipmentId", "shipmentRouteSegmentId", "shipmentPackageSeqId")
                                    .distinct(true)
                                    .queryList();

    customerPoNumberSet = new TreeSet();
    orderItems.each { orderItemPo ->
        correspondingPoId = orderItemPo.correspondingPoId;
        if (correspondingPoId && !"(none)".equals(correspondingPoId)) {
            customerPoNumberSet.add(correspondingPoId);
        }
    }

    // check if there are returnable items
    returned = 0.00;
    totalItems = 0.00;
    orderItems.each { oitem ->
        totalItems += oitem.quantity;
        ritems = oitem.getRelated("ReturnItem", null, null, false);
        ritems.each { ritem ->
            rh = ritem.getRelatedOne("ReturnHeader", false);
            if (!rh.statusId.equals("RETURN_CANCELLED")) {
                returned += ritem.returnQuantity;
            }
        }
    }

    if (totalItems > returned) {
        context.returnLink = "Y";
    }

    context.orderId = orderId;
    context.orderHeader = orderHeader;
    context.localOrderReadHelper = orderReadHelper;
    context.orderItems = orderItems;
    context.orderAdjustments = orderAdjustments;
    context.orderHeaderAdjustments = orderHeaderAdjustments;
    context.orderSubTotal = orderSubTotal;
    context.orderItemShipGroups = orderItemShipGroups;
    context.headerAdjustmentsToShow = headerAdjustmentsToShow;
    context.currencyUomId = orderReadHelper.getCurrency();

    context.orderShippingTotal = orderShippingTotal;
    context.orderTaxTotal = orderTaxTotal;
    context.orderGrandTotal = OrderReadHelper.getOrderGrandTotal(orderItems, orderAdjustments);
    context.placingCustomerPerson = placingCustomerPerson;

    context.billingAccount = billingAccount;
    context.paymentMethods = paymentMethods;

    context.productStore = productStore;
    context.isDemoStore = isDemoStore;

    context.orderShipmentInfoSummaryList = orderShipmentInfoSummaryList;
    context.customerPoNumberSet = customerPoNumberSet;

    orderItemChangeReasons = from("Enumeration").where("enumTypeId", "ODR_ITM_CH_REASON").queryList();
    context.orderItemChangeReasons = orderItemChangeReasons;
}
