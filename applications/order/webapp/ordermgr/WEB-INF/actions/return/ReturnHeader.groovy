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
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.contact.*;

if (parameters.userLogin) {
    userLogin = parameters.userLogin;
    context.userLogin = userLogin;
} 
returnHeader = null;
orderId = parameters.orderId;

if (parameters.returnHeader) {
    returnHeader = parameters.returnHeader;
    returnId = returnHeader.returnId;
    partyId = returnHeader.fromPartyId;
} else {
    partyId = parameters.fromPartyId;
    returnId = parameters.returnId;
}
if (returnId) {
    returnHeader = delegator.findByPrimaryKey("ReturnHeader", [returnId : returnId]);
    if (returnHeader) {
        partyId = returnHeader.fromPartyId;
        toPartyId = parameters.toPartyId;

        context.currentStatus = returnHeader.getRelatedOneCache("StatusItem");
    }
} else {
    partyId = parameters.partyId;
    returnHeaders = delegator.findList("ReturnHeader", EntityCondition.makeCondition("fromPartyId", EntityOperator.EQUALS, partyId), null, null, null, false);
    returnList = [];
    returnHeaders.each { returnHeader ->
        returnMap = [:];
        returnMap.returnId = returnHeader.returnId;
        statusItem = returnHeader.getRelatedOne("StatusItem");
        returnMap.statusId = statusItem.description;
        returnMap.fromPartyId = returnHeader.fromPartyId;
        returnMap.toPartyId = returnHeader.toPartyId;
        
        returnList.add(returnMap);
    }
    context.returnList = returnList;
}
context.returnHeader = returnHeader;
context.returnId = returnId;

//fin account info
finAccounts = null;
if (partyId) {
    finAccounts = delegator.findByAnd("FinAccountAndRole", [partyId: partyId, finAccountTypeId: "STORE_CREDIT_ACCT", roleTypeId: "OWNER", statusId: "FNACT_ACTIVE"]);
    finAccounts = EntityUtil.filterByDate(finAccounts);
}
context.finAccounts = finAccounts;

// billing account info
billingAccountList = null;
if (partyId) {
    billingAccountList = delegator.findByAnd("BillingAccountAndRole", [partyId : partyId]);
    billingAccountList = EntityUtil.filterByDate(billingAccountList);
}
context.billingAccountList = billingAccountList;

// payment method info
List creditCardList = null;
List eftAccountList = null;
if (partyId) {
    creditCardList = EntityUtil.filterByDate(delegator.findByAnd("PaymentMethodAndCreditCard", [partyId : partyId]));
    eftAccountList = EntityUtil.filterByDate(delegator.findByAnd("PaymentMethodAndEftAccount", [partyId : partyId]));
}
context.creditCardList = creditCardList;
context.eftAccountList = eftAccountList;

orderRole = null;
orderHeader = null;
if (orderId) {
    orderRoles = delegator.findByAnd("OrderRole", [orderId : orderId, roleTypeId : "BILL_TO_CUSTOMER"]);
    orderRole = EntityUtil.getFirst(orderRoles);
    orderHeader = delegator.findByPrimaryKey("OrderHeader", [orderId : orderId]);
}
context.orderRole = orderRole;
context.orderHeader = orderHeader;


// from address
addresses = null;
if (context.request) {
    addresses = ContactMechWorker.getPartyPostalAddresses(request, partyId, "_NA_");
}
context.addresses = addresses;

if (returnHeader) {
    contactMechTo = ContactMechWorker.getFacilityContactMechByPurpose(delegator, returnHeader.destinationFacilityId, ["PUR_RET_LOCATION", "SHIPPING_LOCATION", "PRIMARY_LOCATION"]);
    if (contactMechTo) {
        postalAddressTo = delegator.findOne("PostalAddress", [contactMechId : contactMechTo.contactMechId], true);
        context.postalAddressTo = postalAddressTo;
    }
    
    party = delegator.findOne("Party", [partyId : partyId], true);
    if (party) {
        shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
        if (shippingContactMechList) {
            context.postalAddressFrom = delegator.findOne("PostalAddress", [contactMechId : EntityUtil.getFirst(shippingContactMechList).contactMechId], true);
        }
    }
}
