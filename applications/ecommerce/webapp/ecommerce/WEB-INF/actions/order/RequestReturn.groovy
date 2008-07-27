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
import org.ofbiz.party.contact.*;

orderId = parameters.orderId;
context.orderId = orderId;

party = userLogin.getRelatedOne("Party");
context.party = party;

returnTypes = delegator.findList("ReturnType", null, null, ["sequenceId"], null, false);
context.returnTypes = returnTypes;

returnReasons = delegator.findList("ReturnReason", null, null, ["sequenceId"], null, false);
context.returnReasons = returnReasons;

if (orderId) {
    returnRes = dispatcher.runSync("getReturnableItems", [orderId : orderId]);   
    context.returnableItems = returnRes.returnableItems;
    orderHeader = delegator.findByPrimaryKeyCache("OrderHeader", [orderId : orderId]);
    context.orderHeader = orderHeader;
}

returnItemTypeMap = delegator.findByAnd("ReturnItemTypeMap", [returnHeaderTypeId : "CUSTOMER_RETURN"]);
typeMap = new HashMap();
returnItemTypeMap.each { value -> typeMap[value.returnItemMapKey] = value.returnItemTypeId }
context.returnItemTypeMap = typeMap;

//put in the return to party information from the order header
if (orderId) {
    order = delegator.findByPrimaryKey("OrderHeader", [orderId : orderId]);
    productStore = order.getRelatedOne("ProductStore");
    context.toPartyId = productStore.payToPartyId;
}

context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
