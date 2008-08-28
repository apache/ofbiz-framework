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
    party = userLogin.getRelatedOne("Party");
    person = delegator.findByPrimaryKey("Person", [partyId : party.partyId]);
    parameters.firstName = person.firstName;
    parameters.lastName = person.lastName;
    
    contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false));
    if (contactMech) {
        postalAddress = contactMech.getRelatedOne("PostalAddress");
        parameters.shipToContactMechId = postalAddress.contactMechId;

        parameters.shipToAddress1 = postalAddress.address1;
        parameters.shipToAddress2 = postalAddress.address2;
        parameters.shipToCity = postalAddress.city;
        parameters.shipToPostalCode = postalAddress.postalCode;
        parameters.shipToStateProvinceGeoId = postalAddress.stateProvinceGeoId;
        parameters.shipToCountryGeoId = postalAddress.countryGeoId;
        shipToStateProvinceGeo = delegator.findOne("Geo", [geoId : postalAddress.stateProvinceGeoId], false);
        if (shipToStateProvinceGeo) {
            parameters.shipToStateProvinceGeo =  shipToStateProvinceGeo.geoName;
        }
        shipToCountryProvinceGeo = delegator.findOne("Geo", [geoId : postalAddress.countryGeoId], false);
        if (shipToCountryProvinceGeo) {
            parameters.shipToCountryProvinceGeo =  shipToCountryProvinceGeo.geoName;
        }
    }
    
	shipToContactMechList = ContactHelper.getContactMech(party, "PHONE_SHIPPING", "TELECOM_NUMBER", false)
    if (shipToContactMechList) {
        shipToTelecomNumber = (EntityUtil.getFirst(shipToContactMechList)).getRelatedOne("TelecomNumber");
        pcm = EntityUtil.getFirst(shipToTelecomNumber.getRelated("PartyContactMech"));
        context.shipToTelecomNumber = shipToTelecomNumber;
        context.shipToExtension = pcm.extension;
    }

}