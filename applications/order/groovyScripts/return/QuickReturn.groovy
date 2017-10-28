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

import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.party.contact.ContactHelper
import org.apache.ofbiz.product.store.ProductStoreWorker

orderId = parameters.orderId
context.orderId = orderId

returnHeaderTypeId = parameters.returnHeaderTypeId
context.returnHeaderTypeId = returnHeaderTypeId

partyId = parameters.party_id

if (partyId) {
    if (("VENDOR_RETURN").equals(returnHeaderTypeId)) {
        context.toPartyId = partyId
    }
    party = from("Party").where("partyId", partyId).queryOne()
    context.party = party
}

returnHeaders = from("ReturnHeader").where("statusId", "RETURN_REQUESTED").queryList()
context.returnHeaders = returnHeaders

// put in the return to party information from the order header
if (orderId) {
    order = from("OrderHeader").where("orderId", orderId).queryOne()
    productStore = order.getRelatedOne("ProductStore", false)
    if (productStore) {
        if (("VENDOR_RETURN").equals(returnHeaderTypeId)) {
            context.partyId = productStore.payToPartyId
        } else {
            context.destinationFacilityId = ProductStoreWorker.determineSingleFacilityForStore(delegator, productStore.productStoreId)
            context.toPartyId = productStore.payToPartyId
            context.partyId = partyId
        }
    }

    orh = new OrderReadHelper(order)
    context.orh = orh
    context.orderHeaderAdjustments = orh.getAvailableOrderHeaderAdjustments()
}

// payment method info
if (partyId) {
    creditCardList = from("PaymentMethodAndCreditCard").where("partyId", partyId).filterByDate().queryList()
    if (creditCardList) {
        context.creditCardList = creditCardList
    }
    eftAccountList = from("PaymentMethodAndEftAccount").where("partyId", partyId).filterByDate().queryList()
    if (eftAccountList) {
        context.eftAccountList = eftAccountList
    }
}


returnTypes = from("ReturnType").orderBy("sequenceId").queryList()
context.returnTypes = returnTypes

returnReasons = from("ReturnReason").orderBy("sequenceId").queryList()
context.returnReasons = returnReasons

itemStts = from("StatusItem").where("statusTypeId", "INV_SERIALIZED_STTS").orderBy("sequenceId").queryList()
context.itemStts = itemStts

typeMap = [:]
returnItemTypeMap = from("ReturnItemTypeMap").where("returnHeaderTypeId", returnHeaderTypeId).queryList()
returnItemTypeMap.each { value ->
    typeMap[value.returnItemMapKey] = value.returnItemTypeId
}
context.returnItemTypeMap = typeMap

if (orderId) {
    returnRes = runService('getReturnableItems', [orderId : orderId])
    context.returnableItems = returnRes.returnableItems
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
    context.orderHeader = orderHeader
}

context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false)
