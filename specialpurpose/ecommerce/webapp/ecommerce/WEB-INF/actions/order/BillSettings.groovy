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

import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.accounting.payment.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;

cart = session.getAttribute("shoppingCart");
currencyUomId = cart.getCurrency();
payType = parameters.paymentMethodType;
if (!payType && parameters.useGc) {
    payType = "GC";
}
context.cart = cart;
context.paymentMethodType = payType;

partyId = cart.getPartyId() ?: userLogin.partyId;
context.partyId = partyId;

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

if (partyId && !partyId.equals("_NA_")) {
    party = from("Party").where("partyId", partyId).queryOne();
    person = party.getRelatedOne("Person", false);
    context.party = party;
    context.person = person;
    if (party) {
        context.paymentMethodList = EntityUtil.filterByDate(party.getRelated("PaymentMethod", null, null, false));

        billingAccountList = BillingAccountWorker.makePartyBillingAccountList(userLogin, currencyUomId, partyId, delegator, dispatcher);
        if (billingAccountList) {
            context.selectedBillingAccountId = cart.getBillingAccountId();
            context.billingAccountList = billingAccountList;
        }
    }
}

if (parameters.useShipAddr && cart.getShippingContactMechId()) {
    shippingContactMech = cart.getShippingContactMechId();
    postalAddress = from("PostalAddress").where("contactMechId", shippingContactMech).queryOne();
    context.useEntityFields = "Y";
    context.postalFields = postalAddress;

    if (postalAddress && partyId) {
        partyContactMech = from("PartyContactMech").where("partyId", partyId, "contactMechId", postalAddress.contactMechId).orderBy("-fromDate").filterByDate().queryFirst();
        context.partyContactMech = partyContactMech;
    }
} else {
    context.postalFields = UtilHttp.getParameterMap(request);
}

if (cart && !parameters.singleUsePayment) {
    if (cart.getPaymentMethodIds() ) {
        checkOutPaymentId = cart.getPaymentMethodIds()[0];
        context.checkOutPaymentId = checkOutPaymentId;
        paymentMethod = from("PaymentMethod").where("paymentMethodId", checkOutPaymentId).queryOne();
        account = null;

        if ("CREDIT_CARD".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("CreditCard", false);
            context.creditCard = account;
            context.paymentMethodType = "CC";
        } else if ("EFT_ACCOUNT".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("EftAccount", false);
            context.eftAccount = account;
            context.paymentMethodType = "EFT";
        } else if ("GIFT_CARD".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("GiftCard", false);
            context.giftCard = account;
            context.paymentMethodType = "GC";
        } else {
            context.paymentMethodType = "offline";
        }
        if (account && parameters.useShipAddr) {
            address = account.getRelatedOne("PostalAddress", false);
            context.postalAddress = address;
            context.postalFields = address;
        }
    } else if (cart.getPaymentMethodTypeIds()) {
        checkOutPaymentId = cart.getPaymentMethodTypeIds()[0];
        context.checkOutPaymentId = checkOutPaymentId;
    }
}

requestPaymentMethodType = parameters.paymentMethodType;
if (requestPaymentMethodType) {
    context.paymentMethodType = requestPaymentMethodType;
}
