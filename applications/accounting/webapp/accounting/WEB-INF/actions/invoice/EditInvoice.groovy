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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.*;
import org.ofbiz.accounting.invoice.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.math.MathContext;
import org.ofbiz.base.util.UtilNumber;
import javolution.util.FastList;
import javolution.util.FastMap;



invoiceId = parameters.get("invoiceId");

invoice = delegator.findByPrimaryKey("Invoice", [invoiceId : invoiceId]);
context.invoice = invoice;

currency = parameters.currency;        // allow the display of the invoice in the original currency, the default is to display the invoice in the default currency
BigDecimal conversionRate = new BigDecimal("1");
ZERO = BigDecimal.ZERO;
decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

if (invoice) {
    // each invoice of course has two billing addresses, but the one that is relevant for purchase invoices is the PAYMENT_LOCATION of the invoice
    // (ie Accounts Payable address for the supplier), while the right one for sales invoices is the BILLING_LOCATION (ie Accounts Receivable or
    // home of the customer.)
    if ("PURCHASE_INVOICE".equals(invoice.invoiceTypeId)) {
        billingAddress = InvoiceWorker.getSendFromAddress(invoice);
    } else {
        billingAddress = InvoiceWorker.getBillToAddress(invoice);
    }
    if (billingAddress) {
        context.billingAddress = billingAddress;
    }
    billingParty = InvoiceWorker.getBillToParty(invoice);
    context.billingParty = billingParty;
    sendingParty = InvoiceWorker.getSendFromParty(invoice);
    context.sendingParty = sendingParty;

    if (currency && !invoice.getString("currencyUomId").equals(currency)) {
        conversionRate = InvoiceWorker.getInvoiceCurrencyConversionRate(invoice);
        invoice.currencyUomId = currency;
        invoice.invoiceMessage = " converted from original with a rate of: " + conversionRate.setScale(8, rounding);
    }

    invoiceItems = invoice.getRelatedOrderBy("InvoiceItem", ["invoiceItemSeqId"]);
    invoiceItemsConv = FastList.newInstance();
    vatTaxesByType = FastMap.newInstance();
    invoiceItems.each { invoiceItem ->
        invoiceItem.amount = invoiceItem.getBigDecimal("amount").multiply(conversionRate).setScale(decimals, rounding);
        invoiceItemsConv.add(invoiceItem);
        // get party tax id for VAT taxes: they are required in invoices by EU
        // also create a map with tax grand total amount by VAT tax: it is also required in invoices by UE
        taxRate = invoiceItem.getRelatedOne("TaxAuthorityRateProduct");
        if (taxRate && "VAT_TAX".equals(taxRate.taxAuthorityRateTypeId)) {
            taxInfos = EntityUtil.filterByDate(delegator.findByAnd("PartyTaxAuthInfo", [partyId : billingParty.partyId, taxAuthGeoId : taxRate.taxAuthGeoId, taxAuthPartyId : taxRate.taxAuthPartyId]), invoice.invoiceDate);
            taxInfo = EntityUtil.getFirst(taxInfos);
            if (taxInfo) {
                context.billingPartyTaxId = taxInfo.partyTaxId;
            }
            vatTaxesByTypeAmount = vatTaxesByType[taxRate.taxAuthorityRateSeqId];
            if (!vatTaxesByTypeAmount) {
                vatTaxesByTypeAmount = 0.0;
            }
            vatTaxesByType.put(taxRate.taxAuthorityRateSeqId, vatTaxesByTypeAmount + invoiceItem.amount);
        }
    }
    context.vatTaxesByType = vatTaxesByType;
    context.vatTaxIds = vatTaxesByType.keySet().asList();

    context.invoiceItems = invoiceItemsConv;

    invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice).multiply(conversionRate).setScale(decimals, rounding);
    invoiceNoTaxTotal = InvoiceWorker.getInvoiceNoTaxTotal(invoice).multiply(conversionRate).setScale(decimals, rounding);
    context.invoiceTotal = invoiceTotal;
    context.invoiceNoTaxTotal = invoiceNoTaxTotal;

                //*________________this snippet was added for adding Tax ID in invoice header if needed _________________

               sendingTaxInfos = sendingParty.getRelated("PartyTaxAuthInfo");
               billingTaxInfos = billingParty.getRelated("PartyTaxAuthInfo");
               sendingPartyTaxId = null;
               billingPartyTaxId = null;

               if (billingAddress) {
                   sendingTaxInfos.eachWithIndex { sendingTaxInfo, i ->
                       if (sendingTaxInfo.taxAuthGeoId.equals(billingAddress.countryGeoId)) {
                            sendingPartyTaxId = sendingTaxInfos[i-1].partyTaxId;
                       }
                   }
                   billingTaxInfos.eachWithIndex { billingTaxInfo, i ->
                       if (billingTaxInfo.taxAuthGeoId.equals(billingAddress.countryGeoId)) {
                            billingPartyTaxId = billingTaxInfos[i-1].partyTaxId;
                       }
                   }
               }
               if (sendingPartyTaxId) {
                   context.sendingPartyTaxId = sendingPartyTaxId;
               }
               if (billingPartyTaxId && !context.billingPartyTaxId) {
                   context.billingPartyTaxId = billingPartyTaxId;
               }
               //________________this snippet was added for adding Tax ID in invoice header if needed _________________*/


    terms = invoice.getRelated("InvoiceTerm");
    context.terms = terms;

    paymentAppls = delegator.findByAnd("PaymentApplication", [invoiceId : invoiceId]);
    context.payments = paymentAppls;

    orderItemBillings = delegator.findByAnd("OrderItemBilling", [invoiceId : invoiceId], ['orderId']);
    orders = new LinkedHashSet();
    orderItemBillings.each { orderIb ->
        orders.add(orderIb.orderId);
    }
    context.orders = orders;

    invoiceStatus = invoice.getRelatedOne("StatusItem");
    context.invoiceStatus = invoiceStatus;

    edit = parameters.editInvoice;
    if ("true".equalsIgnoreCase(edit)) {
        invoiceItemTypes = delegator.findList("InvoiceItemType", null, null, null, null, false);
        context.invoiceItemTypes = invoiceItemTypes;
        context.editInvoice = true;
    }

    // format the date
    if (invoice.invoiceDate) {
        invoiceDate = DateFormat.getDateInstance(DateFormat.LONG).format(invoice.invoiceDate);
        context.invoiceDate = invoiceDate;
    } else {
        context.invoiceDate = "N/A";
    }
}
