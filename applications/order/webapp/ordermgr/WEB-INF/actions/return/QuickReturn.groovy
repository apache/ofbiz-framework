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
import org.ofbiz.order.order.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.store.*;

orderId = parameters.orderId;
context.orderId = orderId;

returnHeaderTypeId = parameters.returnHeaderTypeId;
context.returnHeaderTypeId = returnHeaderTypeId;

partyId = parameters.party_id;

if (partyId) {
    if (("VENDOR_RETURN").equals(returnHeaderTypeId)) {
        context.toPartyId = partyId;
    }
    party = delegator.findByPrimaryKey("Party", [partyId : partyId]);
    context.party = party;
}

returnHeaders = delegator.findByAnd("ReturnHeader", [statusId : "RETURN_REQUESTED"], ["entryDate"]);
context.returnHeaders = returnHeaders;

// put in the return to party information from the order header
if (orderId) {
    order = delegator.findByPrimaryKey("OrderHeader", [orderId : orderId]);
    productStore = order.getRelatedOne("ProductStore");
    if (productStore) {
        if (("VENDOR_RETURN").equals(returnHeaderTypeId)) {
            context.partyId = productStore.payToPartyId;
        } else {
            context.destinationFacilityId = ProductStoreWorker.determineSingleFacilityForStore(delegator, productStore.productStoreId);
            context.toPartyId = productStore.payToPartyId;
            context.partyId = partyId;
        }
    }

    orh = new OrderReadHelper(order);
    context.orh = orh;
    context.orderHeaderAdjustments = orh.getAvailableOrderHeaderAdjustments();
}

// payment method info
if (partyId) {
    creditCardList = EntityUtil.filterByDate(delegator.findByAnd("PaymentMethodAndCreditCard", [partyId : partyId]));
    if (creditCardList) {
        context.creditCardList = creditCardList;
    }
    eftAccountList = EntityUtil.filterByDate(delegator.findByAnd("PaymentMethodAndEftAccount", [partyId : partyId]));
    if (eftAccountList) {
        context.eftAccountList = eftAccountList;
    }
}


returnTypes = delegator.findList("ReturnType", null, null, ["sequenceId"], null, false);
context.returnTypes = returnTypes;

returnReasons = delegator.findList("ReturnReason", null, null, ["sequenceId"], null, false);
context.returnReasons = returnReasons;

itemStts = delegator.findByAnd("StatusItem", [statusTypeId : "INV_SERIALIZED_STTS"], ["sequenceId"]);
context.itemStts = itemStts;

typeMap = [:];
returnItemTypeMap = delegator.findByAnd("ReturnItemTypeMap", [returnHeaderTypeId : returnHeaderTypeId]);
returnItemTypeMap.each { value ->
    typeMap[value.returnItemMapKey] = value.returnItemTypeId;
}
context.returnItemTypeMap = typeMap;

if (orderId) {
    returnRes = dispatcher.runSync("getReturnableItems", [orderId : orderId]);
    context.returnableItems = returnRes.returnableItems;
    orderHeader = delegator.findByPrimaryKeyCache("OrderHeader", [orderId : orderId]);
    context.orderHeader = orderHeader;
}

context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
