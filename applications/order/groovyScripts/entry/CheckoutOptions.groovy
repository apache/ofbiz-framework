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
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper;

shoppingCart = session.getAttribute("shoppingCart")
currencyUomId = shoppingCart.getCurrency()
partyId = shoppingCart.getPartyId()
party = from("Party").where("partyId", partyId).cache(true).queryOne()
productStore = ProductStoreWorker.getProductStore(request)

shippingEstWpr = null
if (shoppingCart) {
    shippingEstWpr = new ShippingEstimateWrapper(dispatcher, shoppingCart, 0)
    context.shippingEstWpr = shippingEstWpr
    context.carrierShipmentMethodList = shippingEstWpr.getShippingMethods()
    // Reassign items requiring drop-shipping to new or existing drop-ship groups
    Map<String, Object> createDropShipGroupResult = shoppingCart.createDropShipGroups(dispatcher)
    if ("error".equals(createDropShipGroupResult.get("responseMessage"))) {
        Debug.logError((String)createDropShipGroupResult.get("errorMessage"), module)
        request.setAttribute("_ERROR_MESSAGE_", (String)createDropShipGroupResult.get("errorMessage"))
        return "error"
    }
}

profiledefs = from("PartyProfileDefault").where("partyId", userLogin.partyId, "productStoreId", productStoreId).queryOne()
context.profiledefs = profiledefs

context.shoppingCart = shoppingCart
context.userLogin = userLogin
context.productStoreId = productStore.get("productStoreId")
context.productStore = productStore
shipToParty = from("Party").where("partyId", shoppingCart.getShipToCustomerPartyId()).cache(true).queryOne()
context.shippingContactMechList = ContactHelper.getContactMech(shipToParty, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false)
context.emailList = ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false)

if (shoppingCart.getShipmentMethodTypeId() && shoppingCart.getCarrierPartyId()) {
    context.chosenShippingMethod = shoppingCart.getShipmentMethodTypeId() + '@' + shoppingCart.getCarrierPartyId()
} else if (profiledefs?.defaultShipMeth) {
    context.chosenShippingMethod = profiledefs.defaultShipMeth
}

// other profile defaults
if (!shoppingCart.getShippingAddress() && profiledefs?.defaultShipAddr) {
    shoppingCart.setAllShippingContactMechId(profiledefs.defaultShipAddr)
}
if (shoppingCart.selectedPayments() == 0 && profiledefs?.defaultPayMeth) {
    shoppingCart.addPayment(profiledefs.defaultPayMeth)
}

// create a list containing all the parties associated to the current cart, useful to change
// the ship to party id
cartParties = [shoppingCart.getShipToCustomerPartyId()]
if (!cartParties.contains(partyId)) {
    cartParties.add(partyId)
}
if (!cartParties.contains(shoppingCart.getOrderPartyId())) {
    cartParties.add(shoppingCart.getOrderPartyId())
}
if (!cartParties.contains(shoppingCart.getPlacingCustomerPartyId())) {
    cartParties.add(shoppingCart.getPlacingCustomerPartyId())
}
if (!cartParties.contains(shoppingCart.getBillToCustomerPartyId())) {
    cartParties.add(shoppingCart.getBillToCustomerPartyId())
}
if (!cartParties.contains(shoppingCart.getEndUserCustomerPartyId())) {
    cartParties.add(shoppingCart.getEndUserCustomerPartyId())
}
if (!cartParties.contains(shoppingCart.getSupplierAgentPartyId())) {
    cartParties.add(shoppingCart.getSupplierAgentPartyId())
}
salesReps = shoppingCart.getAdditionalPartyRoleMap().SALES_REP
if (salesReps) {
    salesReps.each { salesRep ->
        if (!cartParties.contains(salesRep)) {
            cartParties.add(salesRep)
        }
    }
}
context.cartParties = cartParties
