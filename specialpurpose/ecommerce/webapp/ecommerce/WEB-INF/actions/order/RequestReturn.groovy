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

party = userLogin.getRelatedOne("Party", false);
context.party = party;

returnTypes = from("ReturnType").orderBy("sequenceId").queryList();
context.returnTypes = returnTypes;

returnReasons = from("ReturnReason").orderBy("sequenceId").queryList();
context.returnReasons = returnReasons;

if (orderId) {
    returnRes = runService('getReturnableItems', [orderId : orderId]);
    context.returnableItems = returnRes.returnableItems;
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
    context.orderHeader = orderHeader;
}

returnItemTypeMap = from("ReturnItemTypeMap").where("returnHeaderTypeId", "CUSTOMER_RETURN").queryList();
typeMap = new HashMap();
returnItemTypeMap.each { value -> typeMap[value.returnItemMapKey] = value.returnItemTypeId }
context.returnItemTypeMap = typeMap;

//put in the return to party information from the order header
if (orderId) {
    order = from("OrderHeader").where("orderId", orderId).queryOne();
    productStore = order.getRelatedOne("ProductStore", false);
    context.toPartyId = productStore.payToPartyId;
}

context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
