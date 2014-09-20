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
import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.catalog.*;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

import javolution.util.FastMap;
import javolution.util.FastList;

cart = session.getAttribute("shoppingCart");

if (cart) {
createNewShipGroup = request.getParameter("createNewShipGroup");
if ("Y".equals(createNewShipGroup)) {
    cart.addShipInfo();
}

orderPartyId = cart.getPartyId();
shipToPartyId = parameters.shipToPartyId;
context.cart = cart;

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

if ("SALES_ORDER".equals(cart.getOrderType())) {
    if (!"_NA_".equals(orderPartyId)) {
        orderParty = delegator.findOne("Party", [partyId : orderPartyId], false);
        if (orderParty) {
            shippingContactMechList = ContactHelper.getContactMech(orderParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
            orderPerson = orderParty.getRelatedOne("Person", false);
            context.orderParty = orderParty;
            context.orderPerson = orderPerson;
            context.shippingContactMechList = shippingContactMechList;
        }
    }
    // Ship to another party
    if (shipToPartyId) {
        shipToParty = delegator.findOne("Party", [partyId : shipToPartyId], false);
        if (shipToParty) {
            context.shipToParty = shipToParty;
            shipToPartyShippingContactMechList = ContactHelper.getContactMech(shipToParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
            context.shipToPartyShippingContactMechList = shipToPartyShippingContactMechList;
        }
    }
    // suppliers for the drop-ship select box
    suppliers = delegator.findByAnd("PartyRole", [roleTypeId : "SUPPLIER"], null, false);
    context.suppliers = suppliers;

    // facilities used to reserve the items per ship group
    productStoreFacilities = delegator.findByAnd("ProductStoreFacility", [productStoreId : cart.getProductStoreId()], null, false);
    context.productStoreFacilities = productStoreFacilities;
} else {
    // Purchase order
    if (!"_NA_".equals(orderPartyId)) {
        orderParty = delegator.findOne("Party", [partyId : orderPartyId], false);
        if (orderParty) {
           orderPerson = orderParty.getRelatedOne("Person", false);
           context.orderParty = orderParty;
           context.orderPerson = orderPerson;
         }
    }

    companyId = cart.getBillToCustomerPartyId();
    if (companyId) {
        facilityMaps = FastList.newInstance();
        facilities = delegator.findByAnd("Facility", [ownerPartyId : companyId], null, true);

        // if facilites is null then check the PartyRelationship where there is a relationship set for Parent & Child organization. Then also fetch the value of companyId from there.
        if (UtilValidate.isEmpty(facilities)) {
            partyRelationship = EntityUtil.getFirst(delegator.findList("PartyRelationship", EntityCondition.makeCondition(["roleTypeIdFrom": "PARENT_ORGANIZATION", "partyIdTo": companyId]), null, null, null, false));
            if (UtilValidate.isNotEmpty(partyRelationship)) {
                companyId = partyRelationship.partyIdFrom;
                facilities = delegator.findByAnd("Facility", [ownerPartyId : companyId], null, true);
            }
        }
        facilities.each { facility ->
            facilityMap = FastMap.newInstance();
            facilityContactMechValueMaps = ContactMechWorker.getFacilityContactMechValueMaps(delegator, facility.facilityId, false, null);
            facilityMap.facilityContactMechList = facilityContactMechValueMaps;
            facilityMap.facility = facility;
            facilityMaps.add(facilityMap);
        }
        context.facilityMaps = facilityMaps;
    }
    // Ship to another party
    if (shipToPartyId) {
        shipToParty = delegator.findOne("Party", [partyId : shipToPartyId], false);
        if (shipToParty)
        {
            context.shipToParty = shipToParty;
            shipToPartyShippingContactMechList = ContactHelper.getContactMech(shipToParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
            context.shipToPartyShippingContactMechList = shipToPartyShippingContactMechList;
        }
    }
}
}