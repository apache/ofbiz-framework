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


import org.apache.ofbiz.accounting.invoice.*
import org.apache.ofbiz.accounting.payment.*
import org.apache.ofbiz.base.util.UtilNumber
import org.apache.ofbiz.base.util.collections.*
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

paymentId = parameters.paymentId
payment = from("Payment").where("paymentId", paymentId).queryOne()

decimals = UtilNumber.getBigDecimalScale("invoice.decimals")
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding")

exprList = [EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, payment.partyIdFrom),
            EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, payment.partyIdTo)]
partyCond = EntityCondition.makeCondition(exprList, EntityOperator.AND)

exprList1 = [EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_APPROVED"),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_SENT"),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_READY"),
             EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "INVOICE_RECEIVED")]
statusCond = EntityCondition.makeCondition(exprList1, EntityOperator.OR)

currCond = EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, payment.currencyUomId)
actualCurrCond = EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, payment.actualCurrencyUomId)

topCond = EntityCondition.makeCondition([partyCond, statusCond, currCond], EntityOperator.AND)
topCondActual = EntityCondition.makeCondition([partyCond, statusCond, actualCurrCond], EntityOperator.AND)

//retrieve invoices for the related parties which have not been (fully) applied yet and which have the same currency as the payment
invoices = select("invoiceId", "invoiceTypeId", "currencyUomId", "description", "invoiceDate")
                .from("Invoice")
                .where(topCond)
                .orderBy("invoiceDate")
                .queryList()
context.invoices = getInvoices(invoices, false)
//retrieve invoices for the related parties which have not been (fully) applied yet and which have the same originalCurrency as the payment
invoices = select("invoiceId", "invoiceTypeId", "currencyUomId", "description", "invoiceDate")
                .from("Invoice")
                .where(topCondActual)
                .orderBy("invoiceDate")
                .queryList()
context.invoicesOtherCurrency = getInvoices(invoices, true)

List getInvoices(List invoices, boolean actual) {
    if (invoices) {
        invoicesList = [] // to pass back to the screeen list of unapplied invoices
        paymentApplied = PaymentWorker.getPaymentApplied(payment)
        paymentToApply = payment.getBigDecimal("amount").setScale(decimals,rounding).subtract(paymentApplied)
        if (actual && payment.actualCurrencyAmount) {
            paymentToApply = payment.getBigDecimal("actualCurrencyAmount").setScale(decimals,rounding).subtract(paymentApplied)
        }
        invoices.each { invoice ->
            invoiceAmount = InvoiceWorker.getInvoiceTotal(invoice).setScale(decimals,rounding)
            invoiceApplied = InvoiceWorker.getInvoiceApplied(invoice).setScale(decimals,rounding)
            invoiceToApply = invoiceAmount.subtract(invoiceApplied)
            if (invoiceToApply.signum() == 1) {
                invoiceMap = [:]
                invoiceMap.putAll(invoice)
                invoiceMap.amount = invoiceAmount
                invoiceMap.description = invoice.description
                invoiceMap.amountApplied = invoiceApplied
                if (paymentToApply.compareTo(invoiceToApply) < 0 ) {
                    invoiceMap.amountToApply = paymentToApply
                } else {
                    invoiceMap.amountToApply = invoiceToApply
                }
                invoicesList.add(invoiceMap)
            }
        }
        return invoicesList
    }
}
