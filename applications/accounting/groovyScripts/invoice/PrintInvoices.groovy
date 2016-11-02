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


import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.base.util.UtilNumber

import java.text.DateFormat

invoiceDetailList = []
invoiceIds.each { invoiceId ->
    invoicesMap = [:]
    invoice = from("Invoice").where('invoiceId', invoiceId).queryOne()
    invoicesMap.invoice = invoice
    
    currency = parameters.currency // allow the display of the invoice in the original currency, the default is to display the invoice in the default currency
    BigDecimal conversionRate = new BigDecimal("1")
    ZERO = BigDecimal.ZERO
    decimals = UtilNumber.getBigDecimalScale("invoice.decimals")
    rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding")
    
    if (invoice) {
        if (currency && !invoice.getString("currencyUomId").equals(currency)) {
            conversionRate = InvoiceWorker.getInvoiceCurrencyConversionRate(invoice)
            invoice.currencyUomId = currency
            invoice.invoiceMessage = " converted from original with a rate of: " + conversionRate.setScale(8, rounding)
        }
    
        invoiceItems = invoice.getRelated("InvoiceItem", null, ["invoiceItemSeqId"], false)
        invoiceItemsConv = []
        invoiceItems.each { invoiceItem ->
          if (invoiceItem.amount) {
              invoiceItem.amount = invoiceItem.getBigDecimal("amount").multiply(conversionRate).setScale(decimals, rounding)
              invoiceItemsConv.add(invoiceItem)
          }
        }
    
        invoicesMap.invoiceItems = invoiceItemsConv
    
        invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice).multiply(conversionRate).setScale(decimals, rounding)
        invoiceNoTaxTotal = InvoiceWorker.getInvoiceNoTaxTotal(invoice).multiply(conversionRate).setScale(decimals, rounding)
        invoicesMap.invoiceTotal = invoiceTotal
        invoicesMap.invoiceNoTaxTotal = invoiceNoTaxTotal
    
        if ("PURCHASE_INVOICE".equals(invoice.invoiceTypeId)) {
            billingAddress = InvoiceWorker.getSendFromAddress(invoice)
        } else {
            billingAddress = InvoiceWorker.getBillToAddress(invoice)
        }
        if (billingAddress) {
            invoicesMap.billingAddress = billingAddress
        }
        billToParty = InvoiceWorker.getBillToParty(invoice)
        invoicesMap.billToParty = billToParty
        sendingParty = InvoiceWorker.getSendFromParty(invoice)
        invoicesMap.sendingParty = sendingParty

        // This snippet was added for adding Tax ID in invoice header if needed 
        sendingTaxInfos = sendingParty.getRelated("PartyTaxAuthInfo", null, null, false)
        billingTaxInfos = billToParty.getRelated("PartyTaxAuthInfo", null, null, false)
        sendingPartyTaxId = null
        billToPartyTaxId = null

        if (billingAddress) {
            sendingTaxInfos.eachWithIndex { sendingTaxInfo, i ->
                if (sendingTaxInfo.taxAuthGeoId.equals(billingAddress.countryGeoId)) {
                     sendingPartyTaxId = sendingTaxInfos[i-1].partyTaxId
                }
            }
            billingTaxInfos.eachWithIndex { billingTaxInfo, i ->
                if (billingTaxInfo.taxAuthGeoId.equals(billingAddress.countryGeoId)) {
                     billToPartyTaxId = billingTaxInfos[i-1].partyTaxId
                }
            }
        }
        if (sendingPartyTaxId) {
            invoicesMap.sendingPartyTaxId = sendingPartyTaxId
        }
        if (billToPartyTaxId) {
            invoicesMap.billToPartyTaxId = billToPartyTaxId
        }
    
        terms = invoice.getRelated("InvoiceTerm", null, null, false)
        invoicesMap.terms = terms
    
        paymentAppls = from("PaymentApplication").where('invoiceId', invoiceId).queryList()
        invoicesMap.payments = paymentAppls
    
        orderItemBillings = from("OrderItemBilling").where('invoiceId', invoiceId).orderBy("orderId").queryList()
        orders = new LinkedHashSet()
        orderItemBillings.each { orderIb ->
            orders.add(orderIb.orderId)
        }
        invoicesMap.orders = orders
    
        invoiceStatus = invoice.getRelatedOne("StatusItem", false)
        invoicesMap.invoiceStatus = invoiceStatus
    
        edit = parameters.editInvoice
        if ("true".equalsIgnoreCase(edit)) {
            invoiceItemTypes = from("InvoiceItemType").queryList()
            invoicesMap.invoiceItemTypes = invoiceItemTypes
            invoicesMap.editInvoice = true
        }
    
        // format the date
        if (invoice.invoiceDate) {
            invoiceDate = DateFormat.getDateInstance(DateFormat.LONG).format(invoice.invoiceDate)
            invoicesMap.invoiceDate = invoiceDate
        } else {
            invoicesMap.invoiceDate = "N/A"
        }
    }
    invoiceDetailList.add(invoicesMap)
}

context.invoiceDetailList = invoiceDetailList
