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
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

orderId = context.orderId;
orderPaymentPreferenceId = context.orderPaymentPreferenceId;

if ((!orderId) || (!orderPaymentPreferenceId)) return;

if (orderId) {
   orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
   context.orderHeader = orderHeader;
}

if (orderPaymentPreferenceId) {
   orderPaymentPreference = from("OrderPaymentPreference").where("orderPaymentPreferenceId", orderPaymentPreferenceId).queryOne();
   context.orderPaymentPreference = orderPaymentPreference;
}

if (orderPaymentPreference) {
   paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType", true);
   context.paymentMethodType = paymentMethodType;
}

if (orderPaymentPreference) {
    context.paymentTypeId = "CUSTOMER_PAYMENT";
}

if (orderPaymentPreference) {
    // we retrieve the captureAmount by looking at the latest authorized gateway response for this orderPaymentPreference
    gatewayResponses = orderPaymentPreference.getRelated("PaymentGatewayResponse", null, ["transactionDate DESC"], false);
    EntityUtil.filterByCondition(gatewayResponses, EntityCondition.makeCondition("transCodeEnumId", EntityOperator.EQUALS, "PGT_AUTHORIZE"));

    if (gatewayResponses) {
        latestAuth = gatewayResponses[0];
        context.captureAmount = latestAuth.getBigDecimal("amount");
    } else {
        // todo: some kind of error telling user to re-authorize
    }
}
