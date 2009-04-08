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
import org.ofbiz.webpos.WebPosEvents;
import org.ofbiz.webpos.session.WebPosSession;
import org.ofbiz.webpos.transaction.WebPosTransaction;
import java.text.SimpleDateFormat;

webPosSession = WebPosEvents.getWebPosSession(request, null);
if (webPosSession) {
    shoppingCart = webPosSession.getCart();
    context.transactionId = webPosSession.getCurrentTransaction().getTransactionId();
    context.userLoginId = webPosSession.getUserLoginId();
    context.drawerNumber = webPosSession.getCurrentTransaction().getDrawerNumber();
    sdf = new SimpleDateFormat(UtilProperties.getMessage(WebPosTransaction.resource, "WebPosTransactionDateFormat", Locale.getDefault()));
    context.transactionDate = sdf.format(new Date());
    context.totalDue = webPosSession.getCurrentTransaction().getTotalDue();
} else {
    shoppingCart = null;
}

context.cashAmount = 0;
context.checkAmount = 0;
context.giftAmount = 0;
context.creditAmount = 0;

// Get the Cart and Prepare Size
if (shoppingCart) {
    context.shoppingCartSize = shoppingCart.size();
    payments = shoppingCart.selectedPayments();
    for (i = 0; i < payments; i++) {
        paymentInfo = shoppingCart.getPaymentInfo(i);
        if (paymentInfo.amount != null) {
            amount = paymentInfo.amount.doubleValue();
            if (paymentInfo.paymentMethodTypeId != null) {
                if ("CASH".equals(paymentInfo.paymentMethodTypeId)) {
                    context.cashAmount =  (context.cashAmount) ? context.cashAmount + amount : amount;
                }
                else if ("PERSONAL_CHECK".equals(paymentInfo.paymentMethodTypeId)) {
                    context.checkAmount = (context.checkAmount) ? context.checkAmount + amount : amount;
                    requestParameters.refNumCheck = paymentInfo.refNum[0];
                }
                else if ("GIFT_CARD".equals(paymentInfo.paymentMethodTypeId)) {
                    context.giftAmount = (context.giftAmount) ? context.giftAmount + amount : amount;
                    requestParameters.refNumGift = paymentInfo.refNum[0];
                }
                else if ("CREDIT_CARD".equals(paymentInfo.paymentMethodTypeId)) {
                    context.creditAmount = (context.creditAmount) ? context.creditAmount + amount : amount;
                    requestParameters.refNumCredit = paymentInfo.refNum[0];
                    print("paymentInfo "+paymentInfo);
                }
            }
        }
    }
    context.shoppingCart = shoppingCart;
} else {
    context.shoppingCartSize = 0;
}

context.paymentCash   = delegator.findOne("PaymentMethodType", ["paymentMethodTypeId" : "CASH"], true);
context.paymentCheck  = delegator.findOne("PaymentMethodType", ["paymentMethodTypeId" : "PERSONAL_CHECK"], true);
context.paymentGift   = delegator.findOne("PaymentMethodType", ["paymentMethodTypeId" : "GIFT_CARD"], true);
context.paymentCredit = delegator.findOne("PaymentMethodType", ["paymentMethodTypeId" : "CREDIT_CARD"], true);