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

if (userLogin) {
    party = userLogin.getRelatedOne("Party", false);
    context.partyId = party.partyId
    if ("PERSON".equals(party.partyTypeId)) {
        person = from("Person").where("partyId", party.partyId).queryOne();
        context.firstName = person.firstName;
        context.lastName = person.lastName;
    } else {
        group = from("PartyGroup").where("partyId", party.partyId).queryOne();
        context.firstName = group.groupName;
        context.lastName = "";    
    }

    contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false));
    if (contactMech) {
        postalAddress = contactMech.getRelatedOne("PostalAddress", false);
        context.shipToContactMechId = postalAddress.contactMechId;

        context.shipToName = postalAddress.toName;
        context.shipToAttnName = postalAddress.attnName;
        context.shipToAddress1 = postalAddress.address1;
        context.shipToAddress2 = postalAddress.address2;
        context.shipToCity = postalAddress.city;
        context.shipToPostalCode = postalAddress.postalCode;
        context.shipToStateProvinceGeoId = postalAddress.stateProvinceGeoId;
        context.shipToCountryGeoId = postalAddress.countryGeoId;
        shipToStateProvinceGeo = from("Geo").where("geoId", postalAddress.stateProvinceGeoId).queryOne();
        if (shipToStateProvinceGeo) {
            context.shipToStateProvinceGeo =  shipToStateProvinceGeo.geoName;
        }
        shipToCountryProvinceGeo = from("Geo").where("geoId", postalAddress.countryGeoId).queryOne();
        if (shipToCountryProvinceGeo) {
            context.shipToCountryProvinceGeo =  shipToCountryProvinceGeo.geoName;
        }
    } else {
        context.shipToContactMechId = null;
    }

    shipToContactMechList = ContactHelper.getContactMech(party, "PHONE_SHIPPING", "TELECOM_NUMBER", false)
    if (shipToContactMechList) {
        shipToTelecomNumber = (EntityUtil.getFirst(shipToContactMechList)).getRelatedOne("TelecomNumber", false);
        pcm = EntityUtil.getFirst(shipToTelecomNumber.getRelated("PartyContactMech", null, null, false));
        context.shipToTelecomNumber = shipToTelecomNumber;
        context.shipToExtension = pcm.extension;
    }

    shipToFaxNumberList = ContactHelper.getContactMech(party, "FAX_SHIPPING", "TELECOM_NUMBER", false)
    if (shipToFaxNumberList) {
        shipToFaxNumber = (EntityUtil.getFirst(shipToFaxNumberList)).getRelatedOne("TelecomNumber", false);
        faxPartyContactMech = EntityUtil.getFirst(shipToFaxNumber.getRelated("PartyContactMech", null, null, false));
        context.shipToFaxNumber = shipToFaxNumber;
        context.shipToFaxExtension = faxPartyContactMech.extension;
    }
}
