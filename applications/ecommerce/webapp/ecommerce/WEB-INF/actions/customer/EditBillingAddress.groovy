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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.entity.condition.EntityCondition;

if (userLogin) {
    party = userLogin.getRelatedOne("Party");
    contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "BILLING_LOCATION", "POSTAL_ADDRESS", false));
    if (contactMech) {
        postalAddress = contactMech.getRelatedOne("PostalAddress");    
        parameters.billToContactMechId = postalAddress.contactMechId;
        parameters.billToAddress1 = postalAddress.address1;
        parameters.billToAddress2 = postalAddress.address2;
        parameters.billToCity = postalAddress.city;
        parameters.billToPostalCode = postalAddress.postalCode;
        parameters.billToStateProvinceGeoId = postalAddress.stateProvinceGeoId;
        parameters.billToCountryGeoId = postalAddress.countryGeoId;
        billToStateProvinceGeo = delegator.findByPrimaryKey("Geo", [geoId : postalAddress.stateProvinceGeoId]);
        if (billToStateProvinceGeo) {
            parameters.billToStateProvinceGeo = billToStateProvinceGeo.geoName;
        }
        billToCountryProvinceGeo = delegator.findByPrimaryKey("Geo", [geoId : postalAddress.countryGeoId]);
        if (billToCountryProvinceGeo) {
            parameters.billToCountryProvinceGeo = billToCountryProvinceGeo.geoName;
        }
    }
    
    creditCards = []; 
    paymentMethod = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findList("PaymentMethod", EntityCondition.makeCondition([partyId : party.partyId]), null, ["fromDate"], null, false)));
    if (paymentMethod) {
        creditCard = paymentMethod.getRelatedOne("CreditCard");
        if (creditCard) {
            parameters.paymentMethodTypeId = "CREDIT_CARD";
            parameters.cardNumber = creditCard.cardNumber;
            parameters.paymentMethodId = creditCard.paymentMethodId;
            parameters.firstNameOnCard = creditCard.firstNameOnCard;
            parameters.lastNameOnCard = creditCard.lastNameOnCard;
            parameters.expMonth = (creditCard.expireDate).substring(0, 2);
            parameters.expYear = (creditCard.expireDate).substring(3);
       } 
    }    
} 