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

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.party.contact.ContactHelper
import org.apache.ofbiz.product.store.ProductStoreWorker

productStore = ProductStoreWorker.getProductStore(request)
if (productStore) {
    context.defaultProductStore = productStore
    if (productStore.defaultSalesChannelEnumId)
        context.defaultSalesChannel = from("Enumeration").where("enumId", productStore.defaultSalesChannelEnumId).cache(true).queryOne()
}
// Get the Cart
shoppingCart = session.getAttribute("shoppingCart")
context.shoppingCart = shoppingCart

salesChannels = from("Enumeration").where("enumTypeId", "ORDER_SALES_CHANNEL").orderBy("sequenceId").cache(true).queryList()
context.salesChannels = salesChannels

productStores = from("ProductStore").orderBy("productStoreId", "storeName").cache(true).queryList()
context.productStores = productStores

suppliers = from("PartyRoleAndPartyDetail").where("roleTypeId", "SUPPLIER").orderBy("groupName", "partyId").queryList()
context.suppliers = suppliers

organizations = from("PartyAcctgPrefAndGroup").queryList()
context.organizations = organizations

// Set Shipping From the Party 
partyId = null
partyId = parameters.partyId
if (partyId) {
    party = from("Person").where("partyId", partyId).queryOne()
    if (party) {
        contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false))
        if (contactMech) {
            ShoppingCart shoppingCart = ShoppingCartEvents.getCartObject(request)
            shoppingCart.setAllShippingContactMechId(contactMech.contactMechId)
        }
    }
}
