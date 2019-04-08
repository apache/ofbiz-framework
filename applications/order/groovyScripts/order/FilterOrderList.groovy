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

import java.math.BigDecimal
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.order.order.OrderListState

orderHeaderList = context.orderHeaderList
productStore = ProductStoreWorker.getProductStore(request)

filterInventoryProblems = []

if (state.hasFilter("filterInventoryProblems") && orderHeaderList) {
    orderHeaderList.each { orderHeader ->
        orderReadHelper = OrderReadHelper.getHelper(orderHeader)
        backorderQty = orderReadHelper.getOrderBackorderQuantity()
        if (backorderQty.compareTo(BigDecimal.ZERO) > 0) {
            filterInventoryProblems.add(orderHeader.orderId)
        }
    }
}

filterPOsOpenPastTheirETA = []
filterPOsWithRejectedItems = []
filterPartiallyReceivedPOs = []

state = OrderListState.getInstance(request)

if ((state.hasFilter("filterPartiallyReceivedPOs") ||
        state.hasFilter("filterPOsOpenPastTheirETA") ||
        state.hasFilter("filterPOsWithRejectedItems")) &&
        orderHeaderList) {
    orderHeaderList.each { orderHeader ->
        orderReadHelper = OrderReadHelper.getHelper(orderHeader)
        if ("PURCHASE_ORDER".equals(orderHeader.orderTypeId)) {
            if (orderReadHelper.getRejectedOrderItems() &&
                    state.hasFilter("filterPOsWithRejectedItems")) {
                filterPOsWithRejectedItems.add(orderHeader.get("orderId"))
            } else if (orderReadHelper.getPastEtaOrderItems(orderHeader.get("orderId")) &&
                    state.hasFilter("filterPOsOpenPastTheirETA")) {
                filterPOsOpenPastTheirETA.add(orderHeader.orderId)
            } else if (orderReadHelper.getPartiallyReceivedItems() &&
                    state.hasFilter("filterPartiallyReceivedPOs")) {
                filterPartiallyReceivedPOs.add(orderHeader.orderId)
            }
        }
    }
}

filterAuthProblems = []

if (state.hasFilter("filterAuthProblems") && orderHeaderList) {
    orderHeaderList.each { orderHeader ->
        orderReadHelper = OrderReadHelper.getHelper(orderHeader)
        paymentPrefList = orderReadHelper.getPaymentPreferences()
        paymentPrefList.each { paymentPref ->
            if ("PAYMENT_NOT_AUTH".equals(paymentPref.statusId)) {
                filterAuthProblems.add(orderHeader.orderId)
            }
        }
    }
}
context.filterInventoryProblems = filterInventoryProblems
context.filterPOsWithRejectedItems = filterPOsWithRejectedItems
context.filterPOsOpenPastTheirETA = filterPOsOpenPastTheirETA
context.filterPartiallyReceivedPOs = filterPartiallyReceivedPOs
context.filterAuthProblems = filterAuthProblems
