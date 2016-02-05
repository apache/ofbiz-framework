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

import java.sql.Timestamp;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.content.PartyContentWrapper;
import org.ofbiz.entity.util.EntityUtilProperties;

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
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
    try {
        if (parameters.facilityId) {
            UtilHttp.setContentDisposition(response, "PickSheet" + orderId + ".pdf");
        } else {
            UtilHttp.setContentDisposition(response, orderId + ".pdf");
        }
    } catch (MissingPropertyException e) {
        // This hack for OFBIZ-6792 to avoid "groovy.lang.MissingPropertyException: No such property: response for class: CompanyHeader" when response does not exist (in sendOrderConfirmation service)
    }    
} else if (shipmentId) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
    orderHeader = shipment.getRelatedOne("PrimaryOrderHeader", false);
}

if (!invoice && invoiceId)    {
    invoice = from("Invoice").where("invoiceId", invoiceId).queryOne();
    try {
        UtilHttp.setContentDisposition(response, invoiceId + ".pdf");
    } catch (MissingPropertyException e) {
        // This hack for OFBIZ-6792 to avoid "groovy.lang.MissingPropertyException: No such property: response for class: CompanyHeader" when response does not exist (in sendOrderConfirmation service)
    }    
}

if (!returnHeader && returnId) {
    returnHeader = from("ReturnHeader").where("returnId", returnId).queryOne();
}

if (quoteId) {
    quote = from("Quote").where("quoteId", quoteId).queryOne();
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
        partyId = parameters.get('ApplicationDecorator|organizationPartyId') ? parameters.get('ApplicationDecorator|organizationPartyId') : context.defaultOrganizationPartyId;
    }
}

// the logo
partyGroup = from("PartyGroup").where("partyId", partyId).queryOne();
if (partyGroup) {
    partyContentWrapper = new PartyContentWrapper(dispatcher, partyGroup, locale, EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator));
    partyContent = partyContentWrapper.getFirstPartyContentByType(partyGroup.partyId , partyGroup, "LGOIMGURL", delegator);
    if (partyContent) {
        logoImageUrl = "/content/control/stream?contentId=" + partyContent.contentId;
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
addresses = from("PartyContactWithPurpose")
                .where("partyId", partyId, "contactMechPurposeTypeId", "GENERAL_LOCATION")
                .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                .queryList();
address = null;
if (addresses) {
    address = from("PostalAddress").where("contactMechId", addresses[0].contactMechId).queryOne();
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
phones = from("PartyContactWithPurpose")
             .where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_PHONE")
             .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
             .queryList();
if (phones) {
    context.phone = from("TelecomNumber").where("contactMechId", phones[0].contactMechId).queryOne();
}

// Fax
faxNumbers = from("PartyContactWithPurpose")
                 .where("partyId", partyId, "contactMechPurposeTypeId", "FAX_NUMBER")
                 .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                 .queryList();
if (faxNumbers) {
    context.fax = from("TelecomNumber").where("contactMechId", faxNumbers[0].contactMechId).queryOne();
}

//Email
emails = from("PartyContactWithPurpose")
             .where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_EMAIL")
             .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
             .queryList();
if (emails) {
    context.email = from("ContactMech").where("contactMechId", emails[0].contactMechId).queryOne();
} else {    //get email address from party contact mech
    selContacts = from("PartyContactMech")
                      .where("partyId", partyId).filterByDate(nowTimestamp, "fromDate", "thruDate")
                      .queryList();
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
websiteUrls = from("PartyContactWithPurpose")
                  .where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_WEB_URL")
                  .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                  .queryList();
if (websiteUrls) {
    websiteUrl = EntityUtil.getFirst(websiteUrls);
    context.website = from("ContactMech").where("contactMechId", websiteUrl.contactMechId).queryOne();
} else { //get web address from party contact mech
    selContacts = from("PartyContactMech")
                      .where("partyId", partyId)
                      .filterByDate(nowTimestamp, "fromDate", "thruDate")
                      .queryList();
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
selPayments = from("PaymentMethod")
              .where("partyId", partyId, "paymentMethodTypeId", "EFT_ACCOUNT")
              .filterByDate(nowTimestamp, "fromDate", "thruDate")
              .queryList();
if (selPayments) {
    context.eftAccount = from("EftAccount").where("paymentMethodId", selPayments[0].paymentMethodId).queryOne();
}

// Tax ID Info
partyTaxAuthInfoList = from("PartyTaxAuthInfo").where("partyId", partyId)
                        .filterByDate(nowTimestamp, "fromDate", "thruDate")
                        .queryList();
if (partyTaxAuthInfoList) {
    if (address?.countryGeoId) {
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

