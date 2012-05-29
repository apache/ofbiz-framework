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

// stores
productStores = delegator.findList("ProductStore", null, null, ["storeName"], null, true);
context.productStores = productStores;

// current store
productStoreId = parameters.productStoreId;
if (productStoreId) {
    productStore = delegator.findOne("ProductStore", [productStoreId : productStoreId], false);
    context.currentStore = productStore;
}

// payment settings
paymentSettings = delegator.findByAnd("Enumeration", [enumTypeId : "PRDS_PAYSVC"], ["sequenceId"], false);
context.paymentSettings = paymentSettings;

// payment method (for auto-fill)
paymentMethodId = parameters.paymentMethodId;
context.paymentMethodId = paymentMethodId;

// payment method type
paymentMethodTypeId = parameters.paymentMethodTypeId;
context.paymentMethodTypeId = paymentMethodTypeId;

// service type (transaction type)
txType = parameters.transactionType;
context.txType = txType;
if (txType) {
    currentTx = delegator.findOne("Enumeration", [enumId : txType], false);
    context.currentTx = currentTx;
}

if (paymentMethodId) {
    paymentMethod = delegator.findOne("PaymentMethod", [paymentMethodId : paymentMethodId], false);
    if (paymentMethod) {
        // payment method type
        paymentMethodTypeId = paymentMethod.paymentMethodTypeId;

        // party information
        party = paymentMethod.getRelatedOne("Party", false);
        if (party && "PERSON".equals(party.partyTypeId)) {
            person = party.getRelatedOne("Person", false);
            context.person = person;
        } else if (party && "PARTY_GROUP".equals(party.partyTypeId)) {
            partyGroup = party.getRelatedOne("PartyGroup", false);
            context.partyGroup = partyGroup;
        }

        // method info + address
        creditCard = paymentMethod.getRelatedOne("CreditCard", false);
        context.put("creditCard", creditCard);
        if (creditCard) {
            postalAddress = creditCard.getRelatedOne("PostalAddress", false);
            context.postalFields = postalAddress;
        }

        giftCard = paymentMethod.getRelatedOne("GiftCard", false);
        context.giftCard = giftCard;

        // todo add support for eft account
    }
}

if (paymentMethodTypeId) {
    paymentMethodType = delegator.findOne("PaymentMethodType", [paymentMethodTypeId : paymentMethodTypeId], false);
    context.paymentMethodType = paymentMethodType;
    context.paymentMethodTypeId = paymentMethodTypeId;
}

context.showToolTip = "true";
