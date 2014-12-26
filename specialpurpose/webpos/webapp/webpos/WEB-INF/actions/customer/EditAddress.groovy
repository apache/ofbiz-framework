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

person = from("Person").where("partyId", parameters.partyId).queryOne();
if (person) {
    request.setAttribute("lastName", person.lastName);
    request.setAttribute("firstName", person.firstName);
    request.setAttribute("partyId", parameters.partyId);
}

contactMech = from("ContactMech").where("contactMechId", parameters.contactMechId).queryOne();
if (contactMech) {
    postalAddress = contactMech.getRelatedOne("PostalAddress", false);
    if (postalAddress) {
        request.setAttribute("contactMechId", postalAddress.contactMechId);
        request.setAttribute("toName", postalAddress.toName);
        request.setAttribute("attnName", postalAddress.attnName);
        request.setAttribute("address1", postalAddress.address1);
        request.setAttribute("address2", postalAddress.address2);
        request.setAttribute("city", postalAddress.city);
        request.setAttribute("postalCode", postalAddress.postalCode);
        request.setAttribute("stateProvinceGeoId", postalAddress.stateProvinceGeoId);
        request.setAttribute("countryGeoId", postalAddress.countryGeoId);
        stateProvinceGeo = from("Geo").where("geoId", postalAddress.stateProvinceGeoId).queryOne();
        if (stateProvinceGeo) {
            request.setAttribute("stateProvinceGeo", stateProvinceGeo.get("geoName", locale));
        }
        countryProvinceGeo = from("Geo").where("geoId", postalAddress.countryGeoId).queryOne();
        if (countryProvinceGeo) {
            request.setAttribute("countryProvinceGeo", countryProvinceGeo.get("geoName", locale));
        }
    }
}
request.setAttribute("contactMechPurposeTypeId", parameters.contactMechPurposeTypeId);
