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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;

if (billingAccountId) {
    orderPaymentPreferencesList = [];
    orderList = from("OrderHeader").where('billingAccountId', billingAccountId).queryList();
    if (orderList) {
        orderList.each { orderHeader ->
            orderId = orderHeader.orderId;
            orderBillingAcc = from("OrderHeaderAndPaymentPref").where("orderId", orderId).queryFirst();
            orderBillingAccMap = [:];
            if (orderBillingAcc.paymentMethodTypeId.equals("EXT_BILLACT") && orderBillingAcc.paymentStatusId.equals("PAYMENT_NOT_RECEIVED")) {
                orderBillingAccMap.putAll(orderBillingAcc);
                orderId = orderBillingAcc.orderId;
                orderBillingAccMap.orderId = orderId;
            }
            orderPaymentPreferencesList.add(orderBillingAccMap);
        }
        context.orderPaymentPreferencesList = orderPaymentPreferencesList;
    }
}
