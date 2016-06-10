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

partyId = parameters.partyId ?: parameters.party_id;
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
if (!tryEntity) creditCardData = parameters;
context.creditCardData = creditCardData ?:[:];

giftCardData = paymentResults.giftCard;
if (!tryEntity) giftCardData = parameters;
context.giftCardData = giftCardData ?: [:];

eftAccountData = paymentResults.eftAccount;
if (!tryEntity) eftAccountData = parameters;
context.eftAccountData = eftAccountData ?: [:];

context.donePage = parameters.DONE_PAGE ?:"viewprofile";

paymentMethodData = paymentResults.paymentMethod;
if (!tryEntity.booleanValue()) paymentMethodData = parameters;
if (!paymentMethodData) paymentMethodData = new HashMap();
if (paymentMethodData) context.paymentMethodData = paymentMethodData;
