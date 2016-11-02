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

import java.util.HashMap
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.party.contact.ContactMechWorker

paymentResults = PaymentWorker.getPaymentMethodAndRelated(request, userLogin.partyId)
//returns the following: "paymentMethod", "creditCard", "giftCard", "eftAccount", "paymentMethodId", "curContactMechId", "donePage", "tryEntity"
context.putAll(paymentResults)

curPostalAddressResults = ContactMechWorker.getCurrentPostalAddress(request, userLogin.partyId, paymentResults.curContactMechId)
//returns the following: "curPartyContactMech", "curContactMech", "curPostalAddress", "curPartyContactMechPurposes"
context.putAll(curPostalAddressResults)

postalAddressInfos = ContactMechWorker.getPartyPostalAddresses(request, userLogin.partyId, paymentResults.curContactMechId)
context.put("postalAddressInfos", postalAddressInfos)

//prepare "Data" maps for filling form input boxes
tryEntity = paymentResults.tryEntity

creditCardData = paymentResults.creditCard
if (!tryEntity) creditCardData = parameters
if (!creditCardData) creditCardData = [:]
if (creditCardData) context.creditCardData = creditCardData

giftCardData = paymentResults.giftCard
if (!tryEntity) giftCardData = parameters
if (!giftCardData) giftCardData = [:]
if (giftCardData) context.giftCardData = giftCardData

eftAccountData = paymentResults.eftAccount
if (!tryEntity) eftAccountData = parameters
if (!eftAccountData) eftAccountData = [:]
if (eftAccountData) context.eftAccountData = eftAccountData

paymentMethodData = paymentResults.paymentMethod
if (!tryEntity) paymentMethodData = parameters
if (!paymentMethodData) paymentMethodData = [:]
if (paymentMethodData) context.paymentMethodData = paymentMethodData

//prepare security flag
if (!security.hasEntityPermission("PARTYMGR", "_VIEW", session) && (context.creditCard || context.giftCard || context.eftAccount) && context.paymentMethod && !userLogin.partyId.equals(context.paymentMethod.partyId)) {
    context.canNotView = true
} else {
    context.canNotView = false
}
