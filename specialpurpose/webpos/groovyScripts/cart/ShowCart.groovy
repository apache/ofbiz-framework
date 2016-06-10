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
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webpos.WebPosEvents;
import org.ofbiz.webpos.session.WebPosSession;
import org.ofbiz.webpos.transaction.WebPosTransaction;

webPosSession = WebPosEvents.getWebPosSession(request, null);
if (webPosSession) {
    shoppingCart = webPosSession.getCart();
    context.isManager = webPosSession.isManagerLoggedIn();
    context.transactionId = webPosSession.getCurrentTransaction().getTransactionId();
    context.userLoginId = webPosSession.getUserLoginId();
    context.drawerNumber = webPosSession.getCurrentTransaction().getDrawerNumber();
    context.totalDue = webPosSession.getCurrentTransaction().getTotalDue();
    context.totalQuantity = webPosSession.getCurrentTransaction().getTotalQuantity();
    context.isOpen = webPosSession.getCurrentTransaction().isOpen();
    
    context.person = null;
    if (UtilValidate.isNotEmpty(shoppingCart)) {
        placingCustomerParty = from("PartyAndPerson").where("partyId", shoppingCart.getPlacingCustomerPartyId()).queryOne();
        if (UtilValidate.isNotEmpty(placingCustomerParty)) {
            context.person = placingCustomerParty.lastName + " " + placingCustomerParty.firstName;
        }
    }
} else {
    shoppingCart = null;
}

context.cashAmount = BigDecimal.ZERO;
context.checkAmount = BigDecimal.ZERO;
context.giftAmount = BigDecimal.ZERO;
context.creditAmount = BigDecimal.ZERO;
context.totalPay = BigDecimal.ZERO;

if (shoppingCart) {
    context.shoppingCartSize = shoppingCart.size();
    payments = shoppingCart.selectedPayments();
    for (i = 0; i < payments; i++) {
        paymentInfo = shoppingCart.getPaymentInfo(i);
        if (paymentInfo.amount != null) {
            amount = paymentInfo.amount;
            if (paymentInfo.paymentMethodTypeId != null) {
                if ("CASH".equals(paymentInfo.paymentMethodTypeId)) {
                    context.cashAmount = new BigDecimal((context.cashAmount).add(amount));
                }
                else if ("PERSONAL_CHECK".equals(paymentInfo.paymentMethodTypeId)) {
                    context.checkAmount = new BigDecimal((context.checkAmount).add(amount));
                }
                else if ("GIFT_CARD".equals(paymentInfo.paymentMethodTypeId)) {
                    context.giftAmount = new BigDecimal((context.giftAmount).add(amount));
                }
                else if ("CREDIT_CARD".equals(paymentInfo.paymentMethodTypeId)) {
                    context.creditAmount = new BigDecimal((context.creditAmount).add(amount));
                }
                context.totalPay = new BigDecimal((context.totalPay).add(amount));
            }
        }
    }
    context.shoppingCart = shoppingCart;
} else {
    context.shoppingCartSize = 0;
}

context.paymentCash   = from("PaymentMethodType").where("paymentMethodTypeId" : "CASH").cache(true).queryOne();
context.paymentCheck  = from("PaymentMethodType").where("paymentMethodTypeId" : "PERSONAL_CHECK").cache(true).queryOne();
context.paymentGift   = from("PaymentMethodType").where("paymentMethodTypeId" : "GIFT_CARD").cache(true).queryOne();
context.paymentCredit = from("PaymentMethodType").where("paymentMethodTypeId" : "CREDIT_CARD").cache(true).queryOne();