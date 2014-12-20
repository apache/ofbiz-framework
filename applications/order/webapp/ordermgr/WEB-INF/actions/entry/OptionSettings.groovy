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

import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.store.*;

cart = session.getAttribute("shoppingCart");
context.cart = cart;

productStore = ProductStoreWorker.getProductStore(request);
if (productStore) {
    context.productStore = productStore;
    context.carrierShipmentMethodList = from("ProductStoreShipmentMethView").where("productStoreId", productStore.productStoreId).orderBy("sequenceNumber").cache(true).queryList();
}

// nuke the event messages
request.removeAttribute("_EVENT_MESSAGE_");

party = null;
orderPartyIdId = cart.getPartyId();
if (orderPartyIdId) {
    orderPartyId = from("Party").where("partyId", orderPartyIdId).queryOne();
    context.orderPartyId = orderPartyId;
}

context.emailList = ContactHelper.getContactMechByType(orderPartyId, "EMAIL_ADDRESS", false);

// create the beforeDate for calendar
fromCal = Calendar.getInstance();
fromCal.setTime(new java.util.Date());
fromCal.set(Calendar.HOUR_OF_DAY, fromCal.getActualMinimum(Calendar.HOUR_OF_DAY));
fromCal.set(Calendar.MINUTE, fromCal.getActualMinimum(Calendar.MINUTE));
fromCal.set(Calendar.SECOND, fromCal.getActualMinimum(Calendar.SECOND));
fromCal.set(Calendar.MILLISECOND, fromCal.getActualMinimum(Calendar.MILLISECOND));
fromTs = new Timestamp(fromCal.getTimeInMillis());
fromStr = fromTs.toString();
fromStr = fromStr.substring(0, fromStr.indexOf('.'));
context.beforeDateStr = fromStr;

// create the afterDate for calendar
toCal = Calendar.getInstance();
toCal.setTime(new java.util.Date());
//toCal.set(Calendar.DAY_OF_WEEK, toCal.getActualMaximum(Calendar.DAY_OF_WEEK));
toCal.set(Calendar.HOUR_OF_DAY, toCal.getActualMaximum(Calendar.HOUR_OF_DAY));
toCal.set(Calendar.MINUTE, toCal.getActualMaximum(Calendar.MINUTE));
toCal.set(Calendar.SECOND, toCal.getActualMaximum(Calendar.SECOND));
toCal.set(Calendar.MILLISECOND, toCal.getActualMaximum(Calendar.MILLISECOND));
toTs = new Timestamp(toCal.getTimeInMillis());
toStr = toTs.toString();
context.afterDateStr = toStr;

