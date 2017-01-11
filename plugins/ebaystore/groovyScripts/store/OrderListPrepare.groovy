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

import org.apache.ofbiz.entity.util.EntityUtil

orderLists = []
if (orderList) {
    for (orderCount = 0; orderCount < orderList.size(); orderCount++) {
        orderItem = orderList[orderCount]
        orderId = null
        orderHeaders = from("OrderHeader").where("externalId", orderItem.("externalId")).queryList()
        if (orderHeaders.size() > 0) {
            orderHeader = EntityUtil.getFirst(orderHeaders)
            orderId = orderHeader.get("orderId").toString()
        }
        checkoutStatus = orderItem.get("checkoutStatusCtx")
        paymentMethodUsed = null
        if (checkoutStatus) {
            paymentMethodUsed = checkoutStatus.get("paymentMethodUsed")
        }
        //orderLists.add(orderMap)
        items = orderItem.get("orderItemList")
        for (itemCount = 0; itemCount < items.size(); itemCount++) {
            item = items[itemCount]
            title = null
            if (!(item.get("title"))) {
                product = from("Product").where("productId", item.get("productId")).cache(true).queryOne()
                title = product.get("internalName")
            }
            orderMap = [:]
            orderMap.put("orderId", orderId)
            orderMap.put("externalId", orderItem.get("externalId"))
            orderMap.put("amountPaid", orderItem.get("amountPaid"))
            orderMap.put("createdDate", orderItem.get("createdDate"))
            orderMap.put("ebayUserIdBuyer", orderItem.get("ebayUserIdBuyer"))
            orderMap.put("paymentMethodUsed", paymentMethodUsed)
            orderMap.put("itemId", item.get("itemId"))
            orderMap.put("quantity", item.get("quantity"))
            orderMap.put("productId", item.get("productId"))
            orderMap.put("transactionPrice", item.get("transactionPrice"))
            orderMap.put("title", title)
            orderMap.put("closedDate", item.get("closedDate"))

            orderMap.put("shippingAddressCtx", orderItem.get("shippingAddressCtx"))
            orderMap.put("shippingServiceSelectedCtx", orderItem.get("shippingServiceSelectedCtx"))
            orderMap.put("shippingDetailsCtx", orderItem.get("shippingDetailsCtx"))
            orderMap.put("checkoutStatusCtx", orderItem.get("checkoutStatusCtx"))
            orderMap.put("externalTransactionCtx", orderItem.get("externalTransactionCtx"))
            orderMap.put("orderItemList", orderItem.get("orderItemList"))
            orderMap.put("paidTime", orderItem.get("paidTime"))
            orderMap.put("shippedTime", orderItem.get("shippedTime"))
            orderMap.put("paidTime", orderItem.get("paidTime"))
            orderMap.put("emailBuyer", orderItem.get("emailBuyer"))

            orderLists.add(orderMap)
        }
    }
}
context.orderLists = orderLists
