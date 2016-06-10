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
    returnHeader = from("ReturnHeader").where("returnId", returnId).queryOne();
    if (returnHeader) {
        partyId = returnHeader.fromPartyId;
        toPartyId = parameters.toPartyId;

        context.currentStatus = returnHeader.getRelatedOne("StatusItem", true);
    }
}
context.returnHeader = returnHeader;
context.returnId = returnId;

//fin account info
finAccounts = null;
if (partyId) {
    finAccounts = from("FinAccountAndRole").where("partyId", partyId, "finAccountTypeId", "STORE_CREDIT_ACCT", "roleTypeId", "OWNER", "statusId", "FNACT_ACTIVE").filterByDate().queryList();
}
context.finAccounts = finAccounts;

// billing account info
billingAccountList = null;
if (partyId) {
    billingAccountList = from("BillingAccountAndRole").where("partyId", partyId).filterByDate().queryList();
}
context.billingAccountList = billingAccountList;

// payment method info
List creditCardList = null;
List eftAccountList = null;
if (partyId) {
    creditCardList = from("PaymentMethodAndCreditCard").where("partyId", partyId).filterByDate().queryList();
    eftAccountList = from("PaymentMethodAndEftAccount").where("partyId", partyId).filterByDate().queryList();
}
context.creditCardList = creditCardList;
context.eftAccountList = eftAccountList;

orderRole = null;
orderHeader = null;
if (orderId) {
    orderRole = from("OrderRole").where("orderId", orderId, "roleTypeId", "BILL_TO_CUSTOMER").queryFirst();
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
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
        postalAddressTo = from("PostalAddress").where("contactMechId", contactMechTo.contactMechId).cache(true).queryOne();
        context.postalAddressTo = postalAddressTo;
    }
    
    party = from("Party").where("partyId", partyId).cache(true).queryOne();
    if (party) {
        shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
        if (shippingContactMechList) {
            context.postalAddressFrom = from("PostalAddress").where("contactMechId", EntityUtil.getFirst(shippingContactMechList).contactMechId).cache(true).queryOne();
        }
    }
}
