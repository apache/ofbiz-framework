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

import java.util.HashMap;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.securityext.login.*;
import org.ofbiz.webapp.control.*;

partyId = parameters.get("partyId");
if (partyId == null) {
    partyId = parameters.get("party_id");
}
context.put("partyId", partyId);

// payment info
paymentResults = PaymentWorker.getPaymentMethodAndRelated(request, partyId);
//returns the following: "paymentMethod", "creditCard", "giftCard", "eftAccount", "paymentMethodId", "curContactMechId", "donePage", "tryEntity"
context.putAll(paymentResults);

curPostalAddressResults = ContactMechWorker.getCurrentPostalAddress(request, partyId, paymentResults.get("curContactMechId")); 
//returns the following: "curPartyContactMech", "curContactMech", "curPostalAddress", "curPartyContactMechPurposes"
context.putAll(curPostalAddressResults);

postalAddressInfos = ContactMechWorker.getPartyPostalAddresses(request, partyId, paymentResults.get("curContactMechId"));
context.put("postalAddressInfos", postalAddressInfos);

//prepare "Data" maps for filling form input boxes
tryEntity = paymentResults.get("tryEntity");

creditCardData = paymentResults.get("creditCard");
if (!tryEntity.booleanValue()) creditCardData = parameters;
if (creditCardData == null) creditCardData = new HashMap();
if (creditCardData != null) context.put("creditCardData", creditCardData);

giftCardData = paymentResults.get("giftCard");
if (!tryEntity.booleanValue()) giftCardData = parameters;
if (giftCardData == null) giftCardData = new HashMap();
if (giftCardData != null) context.put("giftCardData", giftCardData);

eftAccountData = paymentResults.get("eftAccount");
if (!tryEntity.booleanValue()) eftAccountData = parameters;
if (eftAccountData == null) eftAccountData = new HashMap();
if (eftAccountData != null) context.put("eftAccountData", eftAccountData);

donePage = parameters.get("DONE_PAGE");
if (donePage == null || donePage.length() <= 0) donePage = "viewprofile";
context.put("donePage", donePage);

paymentMethodData = paymentResults.get("paymentMethod");
if (!tryEntity.booleanValue()) paymentMethodData = parameters;
if (paymentMethodData == null) paymentMethodData = new HashMap();
if (paymentMethodData != null) context.put("paymentMethodData", paymentMethodData);
