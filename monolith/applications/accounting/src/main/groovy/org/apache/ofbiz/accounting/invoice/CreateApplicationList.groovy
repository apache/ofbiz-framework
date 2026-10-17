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
package org.apache.ofbiz.accounting.invoice

import java.text.NumberFormat

// @param GenericValue invoice - The Invoice entity to find payment applications for
if (!invoice) {
    return
}

invoiceApplications = [] // to pass back to the screen with payment applications added
// retrieve related applications with null itemnumber
invoiceAppls = invoice.getRelated('PaymentApplication', [invoiceItemSeqId: null], null, false)
invoiceAppls.each { invoiceAppl ->
    itemmap = [
            invoiceId: invoiceAppl.invoiceId,
            invoiceItemSeqId: invoiceAppl.invoiceItemSeqId,
            total: InvoiceWorker.getInvoiceTotal(invoice),
            paymentApplicationId: invoiceAppl.paymentApplicationId,
            paymentId: invoiceAppl.paymentId,
            billingAccountId: invoiceAppl.billingAccountId,
            taxAuthGeoId: invoiceAppl.taxAuthGeoId,
            amountToApply: invoiceAppl.amountApplied,
            amountApplied: invoiceAppl.amountApplied
    ]
    invoiceApplications.add(itemmap)
}

// retrieve related applications with an existing itemnumber
invoice.getRelated('InvoiceItem', null, null, false).each { item ->
    BigDecimal itemTotal = null
    if (item.amount != null) {
        if (item.quantity) {
            itemTotal = item.getBigDecimal('amount') * item.getBigDecimal('quantity')
        } else {
            itemTotal = item.getBigDecimal('amount')
        }
    }
    // get relation payment applications for every item(can be more than 1 per item number)
    item.getRelated('PaymentApplication', null, null, false).each { paymentApplication ->
        itemmap = [
                *: item,
                total: NumberFormat.getInstance(locale).format(itemTotal),
                paymentApplicationId: paymentApplication.paymentApplicationId,
                paymentId: paymentApplication.paymentId,
                toPaymentId: paymentApplication.toPaymentId,
                amountApplied: paymentApplication.getBigDecimal('amountApplied'),
                amountToApply: paymentApplication.getBigDecimal('amountApplied'),
                billingAccountId: paymentApplication.billingAccountId,
                taxAuthGeoId: paymentApplication.taxAuthGeoId
        ]
        invoiceApplications.add(itemmap)
    }
}
if (invoiceApplications) {
    context.invoiceApplications = invoiceApplications
}
