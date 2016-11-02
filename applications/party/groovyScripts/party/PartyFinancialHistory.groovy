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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityTypeUtil

Boolean actualCurrency = new Boolean(context.actualCurrency)
if (actualCurrency == null) {
    actualCurrency = true
}
actualCurrencyUomId = context.actualCurrencyUomId
if (!actualCurrencyUomId) {
    actualCurrencyUomId = context.defaultOrganizationPartyCurrencyUomId
}
//get total/unapplied/applied invoices separated by sales/purch amount:
totalInvSaApplied = BigDecimal.ZERO
totalInvSaNotApplied = BigDecimal.ZERO
totalInvPuApplied = BigDecimal.ZERO
totalInvPuNotApplied = BigDecimal.ZERO

invExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_IN_PROCESS"),
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_WRITEOFF"),
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_CANCELLED"),
        EntityCondition.makeCondition([
            EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, parameters.partyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, context.defaultOrganizationPartyId)
                ],EntityOperator.AND),
            EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, context.defaultOrganizationPartyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, parameters.partyId)
                ],EntityOperator.AND)
            ],EntityOperator.OR)
        ],EntityOperator.AND)

invIterator = from("InvoiceAndType").where(invExprs).cursorScrollInsensitive().distinct().queryIterator()

while (invoice = invIterator.next()) {
    Boolean isPurchaseInvoice = EntityTypeUtil.hasParentType(delegator, "InvoiceType", "invoiceTypeId", invoice.getString("invoiceTypeId"), "parentTypeId", "PURCHASE_INVOICE")
    Boolean isSalesInvoice = EntityTypeUtil.hasParentType(delegator, "InvoiceType", "invoiceTypeId", (String) invoice.getString("invoiceTypeId"), "parentTypeId", "SALES_INVOICE")
    if (isPurchaseInvoice) {
        totalInvPuApplied += InvoiceWorker.getInvoiceApplied(invoice, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
        totalInvPuNotApplied += InvoiceWorker.getInvoiceNotApplied(invoice, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
    }
    else if (isSalesInvoice) {
        totalInvSaApplied += InvoiceWorker.getInvoiceApplied(invoice, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
        totalInvSaNotApplied += InvoiceWorker.getInvoiceNotApplied(invoice, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
    }
    else {
        Debug.logError("InvoiceType: " + invoice.invoiceTypeId + " without a valid parentTypeId: " + invoice.parentTypeId + " !!!! Should be either PURCHASE_INVOICE or SALES_INVOICE", "")
    }
}

invIterator.close()

//get total/unapplied/applied payment in/out total amount:
totalPayInApplied = BigDecimal.ZERO
totalPayInNotApplied = BigDecimal.ZERO
totalPayOutApplied = BigDecimal.ZERO
totalPayOutNotApplied = BigDecimal.ZERO

payExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PMNT_NOTPAID"),
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PMNT_CANCELLED"),
        EntityCondition.makeCondition([
               EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, parameters.partyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, context.defaultOrganizationPartyId)
                ], EntityOperator.AND),
            EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, context.defaultOrganizationPartyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, parameters.partyId)
                ], EntityOperator.AND)
            ], EntityOperator.OR)
        ], EntityOperator.AND)

payIterator = from("PaymentAndType").where(payExprs).cursorScrollInsensitive().distinct().queryIterator()

while (payment = payIterator.next()) {
    if ("DISBURSEMENT".equals(payment.parentTypeId) || "TAX_PAYMENT".equals(payment.parentTypeId)) {
        totalPayOutApplied += PaymentWorker.getPaymentApplied(payment, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
        totalPayOutNotApplied += PaymentWorker.getPaymentNotApplied(payment, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
    }
    else if ("RECEIPT".equals(payment.parentTypeId)) {
        totalPayInApplied += PaymentWorker.getPaymentApplied(payment, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
        totalPayInNotApplied += PaymentWorker.getPaymentNotApplied(payment, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP)
    }
    else {
        Debug.logError("PaymentTypeId: " + payment.paymentTypeId + " without a valid parentTypeId: " + payment.parentTypeId + " !!!! Should be either DISBURSEMENT, TAX_PAYMENT or RECEIPT", "")
    }
}
payIterator.close()

context.finanSummary = [:]
context.finanSummary.totalSalesInvoice = totalSalesInvoice = totalInvSaApplied.add(totalInvSaNotApplied)
context.finanSummary.totalPurchaseInvoice = totalPurchaseInvoice = totalInvPuApplied.add(totalInvPuNotApplied)
context.finanSummary.totalPaymentsIn = totalPaymentsIn = totalPayInApplied.add(totalPayInNotApplied)
context.finanSummary.totalPaymentsOut = totalPaymentsOut = totalPayOutApplied.add(totalPayOutNotApplied)
context.finanSummary.totalInvoiceNotApplied = totalInvSaNotApplied.subtract(totalInvPuNotApplied)
context.finanSummary.totalPaymentNotApplied = totalPayInNotApplied.subtract(totalPayOutNotApplied)

transferAmount = totalSalesInvoice.subtract(totalPurchaseInvoice).subtract(totalPaymentsIn).subtract(totalPaymentsOut)

if (transferAmount.signum() == -1) { // negative?
    context.finanSummary.totalToBeReceived = transferAmount.negate()
} else {
    context.finanSummary.totalToBePaid = transferAmount
}

