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

import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.party.contact.ContactMechWorker;

partyId = parameters.partyId;
if (!partyId) {
    partyId = parameters.party_id;
}
context.partyId = partyId;

// payment info
paymentResults = PaymentWorker.getPaymentMethodAndRelated(request, partyId);
//returns the following: "paymentMethod", "creditCard", "giftCard", "eftAccount", "paymentMethodId", "curContactMechId", "donePage", "tryEntity"
context.putAll(paymentResults);

curPostalAddressResults = ContactMechWorker.getCurrentPostalAddress(request, partyId, paymentResults.curContactMechId); 
//returns the following: "curPartyContactMech", "curContactMech", "curPostalAddress", "curPartyContactMechPurposes"
context.putAll(curPostalAddressResults);

context.postalAddressInfos = ContactMechWorker.getPartyPostalAddresses(request, partyId, paymentResults.curContactMechId);

//prepare "Data" maps for filling form input boxes
tryEntity = paymentResults.tryEntity;

creditCardData = paymentResults.creditCard;
if (!tryEntity.booleanValue()) creditCardData = parameters;
if (!creditCardData) creditCardData = new HashMap();
if (creditCardData) context.creditCardData = creditCardData;

giftCardData = paymentResults.giftCard;
if (!tryEntity.booleanValue()) giftCardData = parameters;
if (!giftCardData) giftCardData = new HashMap();
if (giftCardData) context.giftCardData = giftCardData;

eftAccountData = paymentResults.eftAccount;
if (!tryEntity.booleanValue()) eftAccountData = parameters;
if (!eftAccountData) eftAccountData = new HashMap();
if (eftAccountData) context.eftAccountData = eftAccountData;

donePage = parameters.DONE_PAGE;
if (!donePage || donePage.length() <= 0) donePage = "viewprofile";
context.donePage = donePage;

paymentMethodData = paymentResults.paymentMethod;
if (!tryEntity.booleanValue()) paymentMethodData = parameters;
if (!paymentMethodData) paymentMethodData = new HashMap();
if (paymentMethodData) context.paymentMethodData = paymentMethodData;