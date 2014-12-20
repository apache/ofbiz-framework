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

import org.ofbiz.entity.util.EntityUtil
import org.ofbiz.party.contact.ContactHelper

contactInfoList = [];
partyIdFrom = parameters.partyIdFrom;
partyIdTo = parameters.partyIdTo;

if (partyIdFrom && partyIdTo) {
    partyList = [partyIdTo, partyIdFrom];
    partyList.each { partyId ->
        party = from("Party").where("partyId", partyId).queryOne();
        person =  party.getRelatedOne("Person", false);
        contactDetailMap = [partyId : partyId, firstName : person.firstName, lastName : person.lastName];

        generalContactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "GENERAL_LOCATION", "POSTAL_ADDRESS", false));
        if (generalContactMech) {
            contactDetailMap.addrContactMechId = generalContactMech.contactMechId;
            postalAddress = generalContactMech.getRelatedOne("PostalAddress", false);
            if (postalAddress) {
                contactDetailMap.address1 = postalAddress.address1;
                contactDetailMap.city = postalAddress.city;
                address2 = postalAddress.address2;
                if (address2) {
                    contactDetailMap.address2 = address2;
                }
                geo = sfrom("Geo").where("geoId", postalAddress.stateProvinceGeoId).queryOne();
                contactDetailMap.state = geo.geoName;

                geo = from("Geo").where("geoId", postalAddress.countryGeoId).queryOne();
                contactDetailMap.country = geo.geoName;
            }
        }
        emailContactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "PRIMARY_EMAIL", "EMAIL_ADDRESS", false));
        if (emailContactMech) {
            contactDetailMap.primaryEmail = emailContactMech.infoString;
            contactDetailMap.emailContactMechId = emailContactMech.contactMechId;
        }
        phoneContactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "PRIMARY_PHONE", "TELECOM_NUMBER", false));
        if (phoneContactMech) {
            contactDetailMap.phoneContactMechId = phoneContactMech.contactMechId;
            telecomNumber = phoneContactMech.getRelatedOne("TelecomNumber", false);
            if (telecomNumber) {
                countryCode = telecomNumber.countryCode;
                if (countryCode) {
                    contactDetailMap.countryCode = countryCode;
                }
                areaCode = telecomNumber.areaCode;
                if (areaCode) {
                    contactDetailMap.areaCode = areaCode;
                }
                contactNumber = telecomNumber.contactNumber;
                if (contactNumber) {
                    contactDetailMap.contactNumber = contactNumber;
                }
            }
        }
        contactInfoList.add(contactDetailMap);
    }
}
context.contactInfoList = contactInfoList;