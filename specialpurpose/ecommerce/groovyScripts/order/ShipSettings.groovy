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
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.catalog.*;

cart = session.getAttribute("shoppingCart");
partyId = cart.getPartyId();
context.cart = cart;

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

if (partyId && !partyId.equals("_NA_")) {
    party = from("Party").where("partyId", partyId).queryOne();
    person = party.getRelatedOne("Person", false);
    context.party = party;
    context.person = person;
}

if (cart?.getShippingContactMechId()) {
    shippingContactMechId = cart.getShippingContactMechId();
    shippingPartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechId", shippingContactMechId).filterByDate().queryFirst();
    parameters.shippingContactMechId = shippingPartyContactDetail.contactMechId;
    context.callSubmitForm = true;

    fullAddressBuf = new StringBuffer();
    fullAddressBuf.append(shippingPartyContactDetail.address1);
    if (shippingPartyContactDetail.address2) {
        fullAddressBuf.append(", ");
        fullAddressBuf.append(shippingPartyContactDetail.address2);
    }
    fullAddressBuf.append(", ");
    fullAddressBuf.append(shippingPartyContactDetail.city);
    fullAddressBuf.append(", ");
    fullAddressBuf.append(shippingPartyContactDetail.postalCode);
    parameters.fullAddress = fullAddressBuf.toString();

    // NOTE: these parameters are a special case because they might be filled in by the address lookup service, so if they are there we won't fill in over them...
    if (!parameters.postalCode) {
        parameters.attnName = shippingPartyContactDetail.attnName;
        parameters.address1 = shippingPartyContactDetail.address1;
        parameters.address2 = shippingPartyContactDetail.address2;
        parameters.city = shippingPartyContactDetail.city;
        parameters.postalCode = shippingPartyContactDetail.postalCode;
        parameters.stateProvinceGeoId = shippingPartyContactDetail.stateProvinceGeoId;
        parameters.countryGeoId = shippingPartyContactDetail.countryGeoId;
        parameters.allowSolicitation = shippingPartyContactDetail.allowSolicitation;
    }

    parameters.yearsAtAddress = shippingPartyContactDetail.yearsWithContactMech;
    parameters.monthsAtAddress = shippingPartyContactDetail.monthsWithContactMech;
} else {
    context.postalFields = UtilHttp.getParameterMap(request);
}
