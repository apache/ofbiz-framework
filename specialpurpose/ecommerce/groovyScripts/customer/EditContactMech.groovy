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

import java.util.HashMap;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;

/* puts the following in the context: "contactMech", "contactMechId",
        "partyContactMech", "partyContactMechPurposes", "contactMechTypeId",
        "contactMechType", "purposeTypes", "postalAddress", "telecomNumber",
        "requestName", "donePage", "tryEntity", "contactMechTypes"
 */
target = [:];
ContactMechWorker.getContactMechAndRelated(request, userLogin.partyId, target);
context.putAll(target);


if (!security.hasEntityPermission("PARTYMGR", "_VIEW", session) && !context.partyContactMech && context.contactMech) {
    context.canNotView = true;
} else {
    context.canNotView = false;
}

preContactMechTypeId = parameters.preContactMechTypeId;
if (preContactMechTypeId) context.preContactMechTypeId = preContactMechTypeId;

paymentMethodId = parameters.paymentMethodId;
if (paymentMethodId) context.paymentMethodId = paymentMethodId;

cmNewPurposeTypeId = parameters.contactMechPurposeTypeId;
if (cmNewPurposeTypeId) {
    contactMechPurposeType = from("ContactMechPurposeType").where("contactMechPurposeTypeId", cmNewPurposeTypeId).queryOne();
    if (contactMechPurposeType) {
        context.contactMechPurposeType = contactMechPurposeType;
    } else {
        cmNewPurposeTypeId = null;
    }
    context.cmNewPurposeTypeId = cmNewPurposeTypeId;
}

tryEntity = context.tryEntity;

contactMechData = context.contactMech;
if (!tryEntity) contactMechData = parameters;
if (!contactMechData) contactMechData = [:];
if (contactMechData) context.contactMechData = contactMechData;

partyContactMechData = context.partyContactMech;
if (!tryEntity) partyContactMechData = parameters;
if (!partyContactMechData) partyContactMechData = [:];
if (partyContactMechData) context.partyContactMechData = partyContactMechData;

postalAddressData = context.postalAddress;
if (!tryEntity) postalAddressData = parameters;
if (!postalAddressData) postalAddressData = [:];
if (postalAddressData) context.postalAddressData = postalAddressData;

telecomNumberData = context.telecomNumber;
if (!tryEntity) telecomNumberData = parameters;
if (!telecomNumberData) telecomNumberData = [:];
if (telecomNumberData) context.telecomNumberData = telecomNumberData;

// load the geo names for selected countries and states/regions
if (parameters.countryGeoId) {
    geoValue = from("Geo").where("geoId", parameters.countryGeoId).cache(true).queryOne();
    if (geoValue) {
        context.selectedCountryName = geoValue.geoName;
    }
} else if (postalAddressData?.countryGeoId) {
    geoValue = from("Geo").where("geoId", postalAddressData.countryGeoId).cache(true).queryOne();
    if (geoValue) {
        context.selectedCountryName = geoValue.geoName;
    }
}

if (parameters.stateProvinceGeoId) {
    geoValue = from("Geo").where("geoId", parameters.stateProvinceGeoId).cache(true).queryOne();
    if (geoValue) {
        context.selectedStateName = geoValue.geoId;
    }
} else if (postalAddressData?.stateProvinceGeoId) {
    geoValue = from("Geo").where("geoId", postalAddressData.stateProvinceGeoId).cache(true).queryOne();
    if (geoValue) {
        context.selectedStateName = geoValue.geoId;
    }
}
