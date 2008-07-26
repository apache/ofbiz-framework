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

import java.lang.*;
import java.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.accounting.payment.PaymentWorker;

productStoreId = ProductStoreWorker.getProductStoreId(request);
context.productStoreId = productStoreId;

if (userLogin) {
    profiledefs = delegator.findByPrimaryKey("PartyProfileDefault", [partyId : partyId, productStoreId : productStoreId]);

    showOld = "true".equals(parameters.SHOW_OLD);

    partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, userLogin.partyId, showOld);
    paymentMethodValueMaps = PaymentWorker.getPartyPaymentMethodValueMaps(delegator, userLogin.partyId, showOld);

    context.profiledefs = profiledefs;
    context.showOld = showOld;
    context.partyContactMechValueMaps = partyContactMechValueMaps;
    context.paymentMethodValueMaps = paymentMethodValueMaps;

    // shipping methods - for default selection
    if (profiledefs?.defaultShipAddr) {
        shipAddress = delegator.findByPrimaryKey("PostalAddress", [contactMechId : profiledefs.defaultShipAddr]);
        if (shipAddress) {
            carrierShipMeths = ProductStoreWorker.getAvailableStoreShippingMethods(delegator, productStoreId, shipAddress, [1], null, 0, 1);
            context.carrierShipMethods = carrierShipMeths;
        }
    }

    profileSurveys = ProductStoreWorker.getProductSurveys(delegator, productStoreId, null, "CUSTOMER_PROFILE");
    context.surveys = profileSurveys;

    orderBy = ["-entryDate"];
    findOpts = new EntityFindOptions();
    findOpts.setMaxRows(5);
    exprs = [EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId)];
    exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "COM_READ"));
    condition = EntityCondition.makeCondition(exprs, EntityOperator.AND);
    messages = delegator.findList("CommunicationEvent", condition, null, orderBy, findOpts, false);
    context.messages = messages;
    context.profileMessages = true;

    partyContent = delegator.findByAnd("ContentRole", [partyId : partyId, roleTypeId : "OWNER"]);
    partyContent = EntityUtil.filterByDate(partyContent);
    context.partyContent = partyContent;

    mimeTypes = delegator.findList("MimeType", null, null, ["description", "mimeTypeId"], null, false);
    context.mimeTypes = mimeTypes;

    partyContentTypes = delegator.findList("PartyContentType", null, null, ["description"], null, false);
    context.partyContentTypes = partyContentTypes;

    // call the getOrderedSummaryInformation service to get the sub-total of valid orders in last X months
    monthsToInclude = 12;
    serviceIn = [partyId : partyId, roleTypeId : "PLACING_CUSTOMER", orderTypeId : "SALES_ORDER", statusId : "ORDER_COMPLETED", monthsToInclude : monthsToInclude, userLogin : userLogin];
    result = dispatcher.runSync("getOrderedSummaryInformation", serviceIn);
    context.monthsToInclude = monthsToInclude;
    context.totalSubRemainingAmount = result.totalSubRemainingAmount;
    context.totalOrders = result.totalOrders;

    contactListPartyList = delegator.findByAnd("ContactListParty", [partyId : partyId], ["-fromDate"]);
    // show all, including history, ie don't filter: contactListPartyList = EntityUtil.filterByDate(contactListPartyList, true);
    context.contactListPartyList = contactListPartyList;

    publicContactLists = delegator.findByAnd("ContactList", [isPublic : "Y"], ["contactListName"]);
    context.publicContactLists = publicContactLists;

    partyAndContactMechList = delegator.findByAnd("PartyAndContactMech", [partyId : partyId], ["-fromDate"]);
    partyAndContactMechList = EntityUtil.filterByDate(partyAndContactMechList);
    context.partyAndContactMechList = partyAndContactMechList;
}
