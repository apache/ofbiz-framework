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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.entity.condition.EntityCondition;

if (userLogin) {
    party = userLogin.getRelatedOne("Party", false);
    contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "BILLING_LOCATION", "POSTAL_ADDRESS", false));
    if (contactMech) {
        postalAddress = contactMech.getRelatedOne("PostalAddress", false);
        billToContactMechId = postalAddress.contactMechId;
        context.billToContactMechId = billToContactMechId;
        context.billToName = postalAddress.toName;
        context.billToAttnName = postalAddress.attnName; 
        context.billToAddress1 = postalAddress.address1;
        context.billToAddress2 = postalAddress.address2;
        context.billToCity = postalAddress.city;
        context.billToPostalCode = postalAddress.postalCode;
        context.billToStateProvinceGeoId = postalAddress.stateProvinceGeoId;
        context.billToCountryGeoId = postalAddress.countryGeoId;
        billToStateProvinceGeo = from("Geo").where("geoId", postalAddress.stateProvinceGeoId).queryOne();
        if (billToStateProvinceGeo) {
            context.billToStateProvinceGeo = billToStateProvinceGeo.geoName;
        }
        billToCountryProvinceGeo = from("Geo").where("geoId", postalAddress.countryGeoId).queryOne();
        if (billToCountryProvinceGeo) {
            context.billToCountryProvinceGeo = billToCountryProvinceGeo.geoName;
        }

        creditCards = [];
        paymentMethod = from("PaymentMethod").where("partyId", party.partyId, "paymentMethodTypeId", "CREDIT_CARD").orderBy("fromDate").filterByDate().queryFirst();
        if (paymentMethod) {
            creditCard = paymentMethod.getRelatedOne("CreditCard", false);
            context.paymentMethodTypeId = "CREDIT_CARD";
            context.cardNumber = creditCard.cardNumber;
            context.cardType = creditCard.cardType;
            context.paymentMethodId = creditCard.paymentMethodId;
            context.firstNameOnCard = creditCard.firstNameOnCard;
            context.lastNameOnCard = creditCard.lastNameOnCard;
            context.expMonth = (creditCard.expireDate).substring(0, 2);
            context.expYear = (creditCard.expireDate).substring(3);
        }
        if (shipToContactMechId) {
            if (billToContactMechId && billToContactMechId.equals(shipToContactMechId)) {
                context.useShippingAddressForBilling = "Y";
            }
        }
    }

    billToContactMechList = ContactHelper.getContactMech(party, "PHONE_BILLING", "TELECOM_NUMBER", false)
    if (billToContactMechList) {
        billToTelecomNumber = (EntityUtil.getFirst(billToContactMechList)).getRelatedOne("TelecomNumber", false);
        pcm = EntityUtil.getFirst(billToTelecomNumber.getRelated("PartyContactMech", null, null, false));
        context.billToTelecomNumber = billToTelecomNumber;
        context.billToExtension = pcm.extension;
    }

    billToFaxNumberList = ContactHelper.getContactMech(party, "FAX_BILLING", "TELECOM_NUMBER", false)
    if (billToFaxNumberList) {
        billToFaxNumber = (EntityUtil.getFirst(billToFaxNumberList)).getRelatedOne("TelecomNumber", false);
        faxPartyContactMech = EntityUtil.getFirst(billToFaxNumber.getRelated("PartyContactMech", null, null, false));
        context.billToFaxNumber = billToFaxNumber;
        context.billToFaxExtension = faxPartyContactMech.extension;
    }
}
