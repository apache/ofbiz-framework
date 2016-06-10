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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.catalog.*;


partyId = null;

userLogin = context.userLogin;
if (userLogin) {
    partyId = userLogin.partyId;
}

if (!partyId && parameters.partyId) {
    partyId = parameters.partyId;
}

if (partyId) {
    parameters.partyId = partyId;

    // NOTE: if there was an error, then don't look up and fill in all of this data, just use the values from the previous request (which will be in the parameters Map automagically)
    if (!request.getAttribute("_ERROR_MESSAGE_") && !request.getAttribute("_ERROR_MESSAGE_LIST_")) {
        person = from("Person").where("partyId", partyId).queryOne();
        if (person) {
            context.callSubmitForm = true;
            // should never be null for the anonymous checkout, but just in case
            parameters.firstName = person.firstName;
            parameters.middleName = person.middleName;
            parameters.lastName = person.lastName;
        }

        // get the Email Address
        emailPartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_EMAIL").filterByDate().queryFirst();
        if (emailPartyContactDetail) {
            parameters.emailContactMechId = emailPartyContactDetail.contactMechId;
            parameters.emailAddress = emailPartyContactDetail.infoString;
            parameters.emailSol = emailPartyContactDetail.allowSolicitation;
        }

        // get the Phone Numbers
        homePhonePartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "PHONE_HOME").filterByDate().queryFirst();
        if (homePhonePartyContactDetail) {
            parameters.homePhoneContactMechId = homePhonePartyContactDetail.contactMechId;
            parameters.homeCountryCode = homePhonePartyContactDetail.countryCode;
            parameters.homeAreaCode = homePhonePartyContactDetail.areaCode;
            parameters.homeContactNumber = homePhonePartyContactDetail.contactNumber;
            parameters.homeExt = homePhonePartyContactDetail.extension;
            parameters.homeSol = homePhonePartyContactDetail.allowSolicitation;
        }

        workPhonePartyContactDetail = from("PartyContactDetailByPurpose").where(partyId : partyId, contactMechPurposeTypeId : "PHONE_WORK").filterByDate().queryFirst();
        if (workPhonePartyContactDetail) {
            parameters.workPhoneContactMechId = workPhonePartyContactDetail.contactMechId;
            parameters.workCountryCode = workPhonePartyContactDetail.countryCode;
            parameters.workAreaCode = workPhonePartyContactDetail.areaCode;
            parameters.workContactNumber = workPhonePartyContactDetail.contactNumber;
            parameters.workExt = workPhonePartyContactDetail.extension;
            parameters.workSol = workPhonePartyContactDetail.allowSolicitation;
        }
    }
}

cart = session.getAttribute("shoppingCart");
cartPartyId = cart.getPartyId();
context.cart = cart;

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

if (cartPartyId && !cartPartyId.equals("_NA_")) {
    cartParty = from("Party").where("partyId", cartPartyId).queryOne();
    if (cartParty) {
        cartPerson = cartParty.getRelatedOne("Person", false);
        context.party = cartParty;
        context.person = cartPerson;
    }
}

if (cart && cart.getShippingContactMechId()) {
    shippingContactMechId = cart.getShippingContactMechId();
    shippingPartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", cartPartyId, "contactMechId", shippingContactMechId).filterByDate().queryFirst();
    parameters.shippingContactMechId = shippingPartyContactDetail.contactMechId;
    context.callSubmitForm = true;
    parameters.shipToName = shippingPartyContactDetail.toName;
    parameters.shipToAttnName = shippingPartyContactDetail.attnName;
    parameters.shipToAddress1 = shippingPartyContactDetail.address1;
    parameters.shipToAddress2 = shippingPartyContactDetail.address2;
    parameters.shipToCity = shippingPartyContactDetail.city;
    parameters.shipToPostalCode = shippingPartyContactDetail.postalCode;
    parameters.shipToStateProvinceGeoId = shippingPartyContactDetail.stateProvinceGeoId;
    parameters.shipToCountryGeoId = shippingPartyContactDetail.countryGeoId;
} else {
    context.postalFields = UtilHttp.getParameterMap(request);
}

billingContactMechId = session.getAttribute("billingContactMechId");
if (billingContactMechId) {
    billPostalAddress = from("PostalAddress").where("contactMechId", billingContactMechId).queryOne();
    parameters.billingContactMechId = billPostalAddress.contactMechId;
    parameters.billToName = billPostalAddress.toName;
    parameters.billToAttnName = billPostalAddress.attnName;
    parameters.billToAddress1 = billPostalAddress.address1;
    parameters.billToAddress2 = billPostalAddress.address2;
    parameters.billToCity = billPostalAddress.city;
    parameters.billToPostalCode = billPostalAddress.postalCode;
    parameters.billToStateProvinceGeoId = billPostalAddress.stateProvinceGeoId;
    parameters.billToCountryGeoId = billPostalAddress.countryGeoId;
}

if (cart?.getShippingContactMechId() && shippingPartyContactDetail) {
    shippingContactMechId = shippingPartyContactDetail.contactMechId;
    if (billingContactMechId?.equals(shippingContactMechId)) {
        context.useShippingPostalAddressForBilling = "Y";
    }
}
parameters.shippingContactMechPurposeTypeId = "SHIPPING_LOCATION";
parameters.billingContactMechPurposeTypeId = "BILLING_LOCATION";
