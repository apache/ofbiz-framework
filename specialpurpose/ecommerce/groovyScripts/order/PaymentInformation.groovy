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

cart = ShoppingCartEvents.getCartObject(request);
context.cart = cart;

partyId = cart.getPartyId();
currencyUomId = cart.getCurrency();

if (!partyId) {
    partyId = userLogin.partyId;
}
context.partyId = partyId;

if (partyId && !partyId.equals("_NA_")) {
    party = from("Party").where("partyId", partyId).queryOne();
    person = party.getRelatedOne("Person", false);
    context.party = party;
    context.person = person;
}

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

if (parameters.useShipAddr && cart.getShippingContactMechId()) {
    shippingContactMech = cart.getShippingContactMechId();
    postalAddress = from("PostalAddress").where("contactMechId", shippingContactMech).queryOne();
    context.useEntityFields = "Y";
    context.postalAddress = postalAddress;

    if (postalAddress && partyId) {
        partyContactMech = from("PartyContactMech").where("partyId", partyId, "contactMechId", postalAddress.contactMechId).orderBy("-fromDate").filterByDate().queryFirst();
        context.partyContactMech = partyContactMech;
    }
} else {
    context.postalAddress = UtilHttp.getParameterMap(request);
}

if (cart) {
    if (cart.getPaymentMethodIds()) {
        paymentMethods = cart.getPaymentMethods();
        paymentMethods.each { paymentMethod ->
            account = null;
            if ("CREDIT_CARD".equals(paymentMethod?.paymentMethodTypeId)) {
                account = paymentMethod.getRelatedOne("CreditCard", false);
                context.creditCard = account;
                context.paymentMethodTypeId = "CREDIT_CARD";
            } else if ("EFT_ACCOUNT".equals(paymentMethod?.paymentMethodTypeId)) {
                account = paymentMethod.getRelatedOne("EftAccount", false);
                context.eftAccount = account;
                context.paymentMethodTypeId = "EFT_ACCOUNT";
            } else if ("GIFT_CARD".equals(paymentMethod?.paymentMethodTypeId)) {
                account = paymentMethod.getRelatedOne("GiftCard", false);
                context.giftCard = account;
                context.paymentMethodTypeId = "GIFT_CARD";
                context.addGiftCard = "Y";
            } else {
                context.paymentMethodTypeId = "EXT_OFFLINE";
            }
            if (account && !parameters.useShipAddr) {
                address = account.getRelatedOne("PostalAddress", false);
                context.postalAddress = address;
            }
        }
    }
}

if (!parameters.useShipAddr) {
    if (cart && context.postalAddress) {
        postalAddress = context.postalAddress;
        shippingContactMechId = cart.getShippingContactMechId();
        contactMechId = postalAddress.contactMechId;
        if (shippingContactMechId?.equals(contactMechId)) {
            context.useShipAddr = "Y";
        }
    }
} else {
    context.useShipAddr = parameters.useShipAddr;
}

// Added here to satisfy GenericAddress.ftl
if (context.postalAddress) {
    postalAddress = context.postalAddress;
    parameters.address1 = postalAddress.address1;
    parameters.address2 = postalAddress.address2;
    parameters.city = postalAddress.city;
    parameters.stateProvinceGeoId = postalAddress.stateProvinceGeoId;
    parameters.postalCode = postalAddress.postalCode;
    parameters.countryGeoId = postalAddress.countryGeoId;
    parameters.contactMechId = postalAddress.contactMechId;
    if (context.creditCard) {
       context.callSubmitForm = true;
    }
}
