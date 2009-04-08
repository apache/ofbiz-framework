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
    party = delegator.findByPrimaryKey("Party", [partyId : partyId]);
    person = party.getRelatedOne("Person");
    context.party = party;
    context.person = person;
    if (party) {
        context.paymentMethodList = EntityUtil.filterByDate(party.getRelated("PaymentMethod"));

        billingAccountList = BillingAccountWorker.makePartyBillingAccountList(userLogin, currencyUomId, partyId, delegator, dispatcher);
        if (billingAccountList) {
            context.selectedBillingAccountId = cart.getBillingAccountId();
            context.billingAccountList = billingAccountList;
        }
    }
}

if (parameters.useShipAddr && cart.getShippingContactMechId()) {
    shippingContactMech = cart.getShippingContactMechId();
    postalAddress = delegator.findByPrimaryKey("PostalAddress", [contactMechId : shippingContactMech]);
    context.useEntityFields = "Y";
    context.postalFields = postalAddress;

    if (postalAddress && partyId) {
        partyContactMechs = delegator.findByAnd("PartyContactMech", [partyId : partyId, contactMechId : postalAddress.contactMechId], ["-fromDate"]);
        partyContactMechs = EntityUtil.filterByDate(partyContactMechs);
        partyContactMech = EntityUtil.getFirst(partyContactMechs);
        context.partyContactMech = partyContactMech;
    }
} else {
    context.postalFields = UtilHttp.getParameterMap(request);
}

if (cart && !parameters.singleUsePayment) {
    if (cart.getPaymentMethodIds() ) {
        checkOutPaymentId = cart.getPaymentMethodIds()[0];
        context.checkOutPaymentId = checkOutPaymentId;
        paymentMethod = delegator.findByPrimaryKey("PaymentMethod", [paymentMethodId : checkOutPaymentId]);
        account = null;

        if ("CREDIT_CARD".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("CreditCard");
            context.creditCard = account;
            context.paymentMethodType = "CC";
        } else if ("EFT_ACCOUNT".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("EftAccount");
            context.eftAccount = account;
            context.paymentMethodType = "EFT";
        } else if ("GIFT_CARD".equals(paymentMethod.paymentMethodTypeId)) {
            account = paymentMethod.getRelatedOne("GiftCard");
            context.giftCard = account;
            context.paymentMethodType = "GC";
        } else {
            context.paymentMethodType = "offline";
        }
        if (account && parameters.useShipAddr) {
            address = account.getRelatedOne("PostalAddress");
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
