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

import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.product.store.ProductStoreWorker;

shoppingCart = session.getAttribute("shoppingCart");
partyId = shoppingCart.getPartyId();
party = from("Party").where("partyId", partyId).cache(true).queryOne();
productStoreId = ProductStoreWorker.getProductStoreId(request);

context.cart = shoppingCart;
context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);

profiledefs = from("PartyProfileDefault").where("partyId", partyId, "productStoreId", productStoreId).cache(true).queryOne();

context.profileDefs = profiledefs;
