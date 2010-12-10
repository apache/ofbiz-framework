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
    orderHeader = delegator.findByPrimaryKey("OrderHeader", [orderId : orderId]);
} else if (shipmentId) {
    shipment = delegator.findByPrimaryKey("Shipment", [shipmentId : shipmentId]);
    orderHeader = shipment.getRelatedOne("PrimaryOrderHeader");
}

if (!invoice && invoiceId)    {
    invoice = delegator.findByPrimaryKey("Invoice", [invoiceId : invoiceId]);
}

if (!returnHeader && returnId) {
    returnHeader = delegator.findByPrimaryKey("ReturnHeader", [returnId : returnId]);
}

if (quoteId) {
    quote = delegator.findByPrimaryKey("Quote", [quoteId : quoteId]);
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
            productStore = orderHeader.getRelatedOne("ProductStore");
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
            def billToCustomer = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderRole", [roleTypeId : "BILL_TO_CUSTOMER"]));
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
    productStore = quote.getRelatedOne("ProductStore");
    if (productStore?.payToPartyId) {
        partyId = productStore.payToPartyId;
    }
}

// if partyId wasn't found use fromPartyId-parameter
if (!partyId) {
    if (fromPartyId) {
        partyId = fromPartyId;
    } else {
        partyId = UtilProperties.getPropertyValue("general.properties", "ORGANIZATION_PARTY");
    }
}

// the logo
partyGroup = delegator.findByPrimaryKey("PartyGroup", [partyId : partyId]);
if (partyGroup?.logoImageUrl) {
    logoImageUrl = partyGroup.logoImageUrl;
}
context.logoImageUrl = logoImageUrl;

// the company name
companyName = "Default Company";
if (partyGroup?.groupName) {
    companyName = partyGroup.groupName;
}
context.companyName = companyName;

// the address
addresses = delegator.findByAnd("PartyContactMechPurpose", [partyId : partyId, contactMechPurposeTypeId : "GENERAL_LOCATION"]);
selAddresses = EntityUtil.filterByDate(addresses, nowTimestamp, "fromDate", "thruDate", true);
address = null;
if (selAddresses) {
    address = delegator.findByPrimaryKey("PostalAddress", [contactMechId : selAddresses[0].contactMechId]);
}
if (address)    {
   // get the country name and state/province abbreviation
   country = address.getRelatedOneCache("CountryGeo");
   if (country) {
      context.countryName = country.get("geoName", locale);
   }
   stateProvince = address.getRelatedOneCache("StateProvinceGeo");
   if (stateProvince) {
       context.stateProvinceAbbr = stateProvince.abbreviation;
   }
}
context.postalAddress = address;

//telephone
phones = delegator.findByAnd("PartyContactMechPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_PHONE"]);
selPhones = EntityUtil.filterByDate(phones, nowTimestamp, "fromDate", "thruDate", true);
if (selPhones) {
    context.phone = delegator.findByPrimaryKey("TelecomNumber", [contactMechId : selPhones[0].contactMechId]);
}

// Fax
faxNumbers = delegator.findByAnd("PartyContactMechPurpose", [partyId : partyId, contactMechPurposeTypeId : "FAX_NUMBER"]);
faxNumbers = EntityUtil.filterByDate(faxNumbers, nowTimestamp, null, null, true);
if (faxNumbers) {
    context.fax = delegator.findOne("TelecomNumber", [contactMechId : faxNumbers[0].contactMechId], false);
}

//Email
emails = delegator.findByAnd("PartyContactMechPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_EMAIL"]);
selEmails = EntityUtil.filterByDate(emails, nowTimestamp, "fromDate", "thruDate", true);
if (selEmails) {
    context.email = delegator.findByPrimaryKey("ContactMech", [contactMechId : selEmails[0].contactMechId]);
} else {    //get email address from party contact mech
    contacts = delegator.findByAnd("PartyContactMech", [partyId : partyId]);
    selContacts = EntityUtil.filterByDate(contacts, nowTimestamp, "fromDate", "thruDate", true);
    if (selContacts) {
        i = selContacts.iterator();
        while (i.hasNext())    {
            email = i.next().getRelatedOne("ContactMech");
            if ("ELECTRONIC_ADDRESS".equals(email.contactMechTypeId))    {
                context.email = email;
                break;
            }
        }
    }
}

// website
websiteUrls = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose", [partyId : partyId, contactMechPurposeTypeId : "PRIMARY_WEB_URL"]));
if (websiteUrls) {
    websiteUrl = EntityUtil.getFirst(websiteUrls);
    context.website = delegator.findOne("ContactMech", [contactMechId : websiteUrl.contactMechId], false);
} else { //get web address from party contact mech
contacts = delegator.findByAnd("PartyContactMech", [partyId : partyId]);
selContacts = EntityUtil.filterByDate(contacts, nowTimestamp, "fromDate", "thruDate", true);
if (selContacts) {
    Iterator i = selContacts.iterator();
    while (i.hasNext())    {
        website = i.next().getRelatedOne("ContactMech");
        if ("WEB_ADDRESS".equals(website.contactMechTypeId)) {
            context.website = website;
            break;
        }
    }
}
}

//Bank account
paymentMethods = delegator.findByAnd("PaymentMethod", [partyId : partyId, paymentMethodTypeId : "EFT_ACCOUNT"]);
selPayments = EntityUtil.filterByDate(paymentMethods, nowTimestamp, "fromDate", "thruDate", true);
if (selPayments) {
    context.eftAccount = delegator.findByPrimaryKey("EftAccount", [paymentMethodId : selPayments[0].paymentMethodId]);
}

// Tax ID Info
partyTaxAuthInfoList = delegator.findByAnd("PartyTaxAuthInfo", [partyId : partyId]);
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
