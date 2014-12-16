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

 // this script is used to get the company's logo header information for orders, invoices, and returns.  It can either take order, invoice, returnHeader from
 // parameters or use orderId, invoiceId, or returnId to look them up.
 // if none of these parameters are available then fromPartyId is used or "ORGANIZATION_PARTY" from general.properties as fallback

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.order.order.OrderReadHelper;
import java.sql.Timestamp;
import org.ofbiz.party.content.PartyContentWrapper;

orderHeader = parameters.orderHeader;
orderId = parameters.orderId;
invoice = parameters.invoice;
invoiceId = parameters.invoiceId;
shipmentId = parameters.shipmentId;
returnHeader = parameters.returnHeader;
returnId = parameters.returnId;
quote = null;
quoteId = parameters.quoteId;
fromPartyId = parameters.fromPartyId;

if (!orderHeader && orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
} else if (shipmentId) {
    shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
    orderHeader = shipment.getRelatedOne("PrimaryOrderHeader", false);
}

if (!invoice && invoiceId)    {
    invoice = delegator.findOne("Invoice", [invoiceId : invoiceId], false);
}

if (!returnHeader && returnId) {
    returnHeader = delegator.findOne("ReturnHeader", [returnId : returnId], false);
}

if (quoteId) {
    quote = delegator.findOne("Quote", [quoteId : quoteId], false);
}

// defaults:
def logoImageUrl = null; // the default value, "/images/ofbiz_powered.gif", is set in the screen decorators
def partyId = null;

// get the logo partyId from order or invoice - note that it is better to do comparisons this way in case the there are null values
if (orderHeader) {
    orh = new OrderReadHelper(orderHeader);
    // for sales order, the logo party is the "BILL_FROM_VENDOR" of the order.  If that's not available, we'll use the OrderHeader's ProductStore's payToPartyId
    if ("SALES_ORDER".equals(orderHeader.orderTypeId)) {
        if (orh.getBillFromParty()) {
            partyId = orh.getBillFromParty().partyId;
        } else {
            productStore = orderHeader.getRelatedOne("ProductStore", false);
            if (orderHeader.orderTypeId.equals("SALES_ORDER") && productStore?.payToPartyId) {
                partyId = productStore.payToPartyId;
            }
        }
    // purchase orders - use the BILL_TO_CUSTOMER of the order
    } else if ("PURCHASE_ORDER".equals(orderHeader.orderTypeId)) {
        def billToParty = orh.getBillToParty();
        if (billToParty) {
            partyId = billToParty.partyId;
        } else {
            def billToCustomer = EntityUtil.getFirst(orderHeader.getRelated("OrderRole", [roleTypeId : "BILL_TO_CUSTOMER"], null, false));
            if (billToCustomer) {
                partyId = billToCustomer.partyId;
            }
        }
    }
} else if (invoice) {
    if ("SALES_INVOICE".equals(invoice.invoiceTypeId) && invoice.partyIdFrom) {
        partyId = invoice.partyIdFrom;
    }
    if ("PURCHASE_INVOICE".equals(invoice.invoiceTypeId) || "CUST_RTN_INVOICE".equals(invoice.invoiceTypeId) && invoice.partyId) {
        partyId = invoice.partyId;
    }
} else if (returnHeader) {
    if ("CUSTOMER_RETURN".equals(returnHeader.returnHeaderTypeId) && returnHeader.toPartyId) {
        partyId = returnHeader.toPartyId;
    } else if ("VENDOR_RETURN".equals(returnHeader.returnHeaderTypeId) && returnHeader.fromPartyId) {
        partyId = returnHeader.fromPartyId;
    }
} else if (quote) {
    productStore = quote.getRelatedOne("ProductStore", false);
    if (productStore?.payToPartyId) {
        partyId = productStore.payToPartyId;
    }
}

// if partyId wasn't found use fromPartyId-parameter
if (!partyId) {
    if (fromPartyId) {
        partyId = fromPartyId;
    } else {
        partyId = EntityUtilProperties.getPropertyValue("general.properties", "ORGANIZATION_PARTY", delegator);
    }
}

// the logo
partyGroup = delegator.findOne("PartyGroup", [partyId : partyId], false);
if (partyGroup) {
    partyContentWrapper = new PartyContentWrapper(dispatcher, partyGroup, locale, "text/html");
    partyContent = partyContentWrapper.getFirstPartyContentByType(partyGroup.partyId , partyGroup, "LGOIMGURL", delegator);
    if (partyContent) {
        logoImageUrl = "/content/control/stream?contentId="+partyContent.contentId;
    } else {
        if (partyGroup?.logoImageUrl) {
            logoImageUrl = partyGroup.logoImageUrl;
        }
    }
}
//If logoImageUrl not null then only set it to context else it will override the default value "/images/ofbiz_powered.gif"
if (logoImageUrl) {
    context.logoImageUrl = logoImageUrl;
}

// the company name
companyName = "Default Company";
if (partyGroup?.groupName) {
    companyName = partyGroup.groupName;
}
context.companyName = companyName;

// the address
addresses = delegator.findByAnd("PartyContactWithPurpose", [partyId : partyId, contactMechPurposeTypeId : "GENERAL_LOCATION"], null, false);
addresses = EntityUtil.filterByDate(addresses, nowTimestamp, "contactFromDate", "contactThruDate", true);
addresses = EntityUtil.filterByDate(addresses, nowTimestamp, "purposeFromDate", "purposeThruDate", true);
address = null;
if (addresses) {
    address = delegator.findOne("PostalAddress", [contactMechId : addresses[0].contactMechId], false);
}
if (address)    {
   // get the country name and state/province abbreviation
   country = address.getRelatedOne("CountryGeo", true);
   if (country) {
      context.countryName = country.get("geoName", locale);
   }
   stateProvince = address.getRelatedOne("StateProvinceGeo", true);
   if (stateProvince) {
       context.stateProvinceAbbr = stateProvince.abbreviation;
   }
}
context.postalAddress = address;

//telephone
phones = delegator.findByAnd("PartyContactWithPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_PHONE"], null, false);
phones = EntityUtil.filterByDate(phones, nowTimestamp, "contactFromDate", "contactThruDate", true);
phones = EntityUtil.filterByDate(phones, nowTimestamp, "purposeFromDate", "purposeThruDate", true);
if (phones) {
    context.phone = delegator.findOne("TelecomNumber", [contactMechId : phones[0].contactMechId], false);
}

// Fax
faxNumbers = delegator.findByAnd("PartyContactWithPurpose", [partyId : partyId, contactMechPurposeTypeId : "FAX_NUMBER"], null, false);
faxNumbers = EntityUtil.filterByDate(faxNumbers, nowTimestamp, "contactFromDate", "contactThruDate", true);
faxNumbers = EntityUtil.filterByDate(faxNumbers, nowTimestamp, "purposeFromDate", "purposeThruDate", true);
if (faxNumbers) {
    context.fax = delegator.findOne("TelecomNumber", [contactMechId : faxNumbers[0].contactMechId], false);
}

//Email
emails = delegator.findByAnd("PartyContactWithPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_EMAIL"], null, false);
emails = EntityUtil.filterByDate(emails, nowTimestamp, "contactFromDate", "contactThruDate", true);
emails = EntityUtil.filterByDate(emails, nowTimestamp, "purposeFromDate", "purposeThruDate", true);
if (emails) {
    context.email = delegator.findOne("ContactMech", [contactMechId : emails[0].contactMechId], false);
} else {    //get email address from party contact mech
    contacts = delegator.findByAnd("PartyContactMech", [partyId : partyId], null, false);
    selContacts = EntityUtil.filterByDate(contacts, nowTimestamp, "fromDate", "thruDate", true);
    if (selContacts) {
        i = selContacts.iterator();
        while (i.hasNext())    {
            email = i.next().getRelatedOne("ContactMech", false);
            if ("ELECTRONIC_ADDRESS".equals(email.contactMechTypeId))    {
                context.email = email;
                break;
            }
        }
    }
}

// website
websiteUrls = delegator.findByAnd("PartyContactWithPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_WEB_URLs"], null, false);
websiteUrls = EntityUtil.filterByDate(websiteUrls, nowTimestamp, "contactFromDate", "contactThruDate", true);
websiteUrls = EntityUtil.filterByDate(websiteUrls, nowTimestamp, "purposeFromDate", "purposeThruDate", true);
if (websiteUrls) {
    websiteUrl = EntityUtil.getFirst(websiteUrls);
    context.website = delegator.findOne("ContactMech", [contactMechId : websiteUrl.contactMechId], false);
} else { //get web address from party contact mech
contacts = delegator.findByAnd("PartyContactMech", [partyId : partyId], null, false);
selContacts = EntityUtil.filterByDate(contacts, nowTimestamp, "fromDate", "thruDate", true);
if (selContacts) {
    Iterator i = selContacts.iterator();
    while (i.hasNext())    {
        website = i.next().getRelatedOne("ContactMech", false);
        if ("WEB_ADDRESS".equals(website.contactMechTypeId)) {
            context.website = website;
            break;
        }
    }
}
}

//Bank account
paymentMethods = delegator.findByAnd("PaymentMethod", [partyId : partyId, paymentMethodTypeId : "EFT_ACCOUNT"], null, false);
selPayments = EntityUtil.filterByDate(paymentMethods, nowTimestamp, "fromDate", "thruDate", true);
if (selPayments) {
    context.eftAccount = delegator.findOne("EftAccount", [paymentMethodId : selPayments[0].paymentMethodId], false);
}

// Tax ID Info
partyTaxAuthInfoList = delegator.findByAnd("PartyTaxAuthInfo", [partyId : partyId], null, false);
if (partyTaxAuthInfoList) {
    if (address.countryGeoId) {
        // if we have an address with country filter by that
        partyTaxAuthInfoList.eachWithIndex { partyTaxAuthInfo, i ->
            if (partyTaxAuthInfo.taxAuthGeoId.equals(address.countryGeoId)) {
                context.sendingPartyTaxId = partyTaxAuthInfo.partyTaxId;
            }
        }
    } else {
        // otherwise just grab the first one
        context.sendingPartyTaxId = partyTaxAuthInfoList[0].partyTaxId;
    }
}
