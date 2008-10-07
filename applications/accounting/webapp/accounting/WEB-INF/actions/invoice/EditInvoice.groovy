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
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.*;
import org.ofbiz.accounting.invoice.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.math.MathContext;
import org.ofbiz.base.util.UtilNumber;
import javolution.util.FastList;

 
invoiceId = parameters.get("invoiceId");

invoice = delegator.findByPrimaryKey("Invoice", [invoiceId : invoiceId]);
context.invoice = invoice;

other = parameters.other;        // allow the display of the invoice in the currency of the other party. sales: partyId, purch: partyIdFrom using the convertUom service.
conversionRate = null;
ZERO = BigDecimal.ZERO;
decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
if ("Y".equalsIgnoreCase(other)) {
    if (invoice.currencyUomId.equals(invoice.getRelatedOne("Party").preferredCurrencyUomId)) {
        otherCurrency = invoice.getRelatedOne("FromParty").preferredCurrencyUomId;
    } else {
        otherCurrency = invoice.getRelatedOne("Party").preferredCurrencyUomId;
    }
    result = null;
    if (otherCurrency && invoice.currencyUomId && !otherCurrency.equals(invoice.currencyUomId)) {
        invoice.currencyUomId = otherCurrency;
        // check if the transaction is created, take the conversion from there
        acctgTransEntries = invoice.getRelated("AcctgTrans");
        if (acctgTransEntries) {
            acctgTransEntry = acctgTransEntries[0].getRelated("AcctgTransEntry")[0];
            conversionRate = acctgTransEntry.getBigDecimal("amount").divided(acctgTransEntry.getBigDecimal("origAmount"), new MathContext(100));
        }
        // check if a payment is applied and use the currency conversion from there
        if (!conversionRate) {
            paymentAppls = invoice.getRelated("PaymentApplication");
            paymentAppls.each { paymentAppl ->
                payment = paymentAppl.getRelatedOne("Payment"); 
                if (!conversionRate) {
                    conversionRate = payment.getBigDecimal("amount").divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100));
                } else {
                    conversionRate = conversionRate.add(payment.getBigDecimal("amount").divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100))).divide(new BigDecimal("2"),new MathContext(100));
                }
            }
        }
        if (!conversionRate) {
            result = dispatcher.runSync("convertUom", [uomId : invoice.currencyUomId, 
                                               uomIdTo : otherCurrency, 
                                               originalValue : new Double("1.00"), 
                                               asOfDate : invoice.invoiceDate]);
    
            if (result.convertedValue != null) {
                conversionRate = new BigDecimal(result.convertedValue.doubleValue());
                invoice.invoiceMessage = invoice.get("invoiceMessage") ? 
                          invoice.invoiceMessage.concat(" Converted from " + invoice.currencyUomId + " Rate: " + conversionRate.setScale(6, rounding).toString()) :
                          "Converted from " + invoice.currencyUomId + " Rate: " + conversionRate.setScale(6, rounding).toString();
            }
        }
    } 
}

if (!conversionRate) {
    conversionRate = 1;
}

if (invoice) {
    invoiceItems = invoice.getRelatedOrderBy("InvoiceItem", ["invoiceItemSeqId"]);
    invoiceItemsConv = FastList.newInstance();
    invoiceItems.each { invoiceItem ->
      invoiceItem.amount = new Double((invoiceItem.getBigDecimal("amount").multiply(conversionRate).setScale(decimals, rounding)).doubleValue());
      invoiceItemsConv.add(invoiceItem);
    }
    

    context.invoiceItems = invoiceItemsConv;
    
    invoiceTotal = InvoiceWorker.getInvoiceTotalBd(invoice).multiply(conversionRate).setScale(decimals, rounding).doubleValue();
    invoiceNoTaxTotal = InvoiceWorker.getInvoiceNoTaxTotalBd(invoice).multiply(conversionRate).setScale(decimals, rounding).doubleValue();
    context.invoiceTotal = new Double(invoiceTotal);    
    context.invoiceNoTaxTotal = new Double(invoiceNoTaxTotal);
    
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
               if (billingPartyTaxId) {
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
