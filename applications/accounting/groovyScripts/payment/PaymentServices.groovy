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
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue

MODULE = "PaymentServices.groovy"
def createPayment() {
    if (!security.hasEntityPermission("ACCOUNTING", "_CREATE", parameters.userLogin) && (!security.hasEntityPermission("PAY_INFO", "_CREATE", parameters.userLogin) && userLogin.partyId != parameters.partyIdFrom && userLogin.partyId != parameters.partyIdTo)) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingCreatePaymentPermissionError)
    }

    GenericValue payment = delegator.makeValue("Payment")
    payment.paymentId = parameters.paymentId ?: delegator.getNextSeqId("Payment")
    paymentId = payment.paymentId
    parameters.statusId = parameters.statusId ?: "PMNT_NOT_PAID"

    if (parameters.paymentMethodId) {
        GenericValue paymentMethod = from("PaymentMethod").where("paymentMethodId", parameters.paymentMethodId).queryOne()
        if (parameters.paymentMethodTypeId != paymentMethod.paymentMethodTypeId) {
            Debug.logInfo("Replacing passed payment method type [" + parameters.paymentMethodTypeId + "] with payment method type [" + paymentMethod.paymentMethodTypeId + "] for payment method [" + parameters.paymentMethodId +"]", MODULE)
            parameters.paymentMethodTypeId = paymentMethod.paymentMethodTypeId
        }
    }

    if (parameters.paymentPreferenceId) {
        GenericValue orderPaymentPreference = from("OrderPaymentPreference").where("orderPaymentPreferenceId", parameters.paymentPreferenceId).queryOne()
        parameters.paymentId = parameters.paymentId ?: orderPaymentPreference.paymentMethodId
        parameters.paymentMethodTypeId = parameters.paymentMethodTypeId ?: orderPaymentPreference.paymentMethodTypeId
    }

    if (!parameters.paymentMethodTypeId) {
        return error(UtilProperties.getResourceBundleMap("AccountingUiLabels", locale)?.AccountingPaymentMethodIdPaymentMethodTypeIdNullError)
    }

    payment.setNonPKFields(parameters)
    payment.effectiveDate = payment.effectiveDate ?: UtilDateTime.nowTimestamp()
    delegator.create(payment)
    Map result = success()
    result.paymentId = paymentId
    return result
}

def getInvoicePaymentInfoList() {
    // Create a list with information on payment due dates and amounts for the invoice
    GenericValue invoice;
    List invoicePaymentInfoList = []
    if (!parameters.invoice) {
        invoice = from("Invoice").where("invoiceId", parameters.invoiceId).queryOne()
    } else {
        invoice = parameters.invoice
    }

    BigDecimal invoiceTotalAmount = InvoiceWorker.getInvoiceTotal(invoice)
    BigDecimal invoiceTotalAmountPaid = InvoiceWorker.getInvoiceApplied(invoice)

    List invoiceTerms = from("InvoiceTerm").where("invoiceId", invoice.invoiceId).queryList()

    BigDecimal remainingAppliedAmount = invoiceTotalAmountPaid
    BigDecimal computedTotalAmount = (BigDecimal) 0

    Map invoicePaymentInfo = [:]

    for (invoiceTerm in invoiceTerms) {
        termType = from("TermType").where("termTypeId", invoiceTerm.termTypeId).cache(true).queryOne()
        if ("FIN_PAYMENT_TERM" == termType.parentTypeId) {
            invoicePaymentInfo.clear()
            invoicePaymentInfo.invoiceId = invoice.invoiceId
            invoicePaymentInfo.invoiceTermId = invoiceTerm.invoiceTermId
            invoicePaymentInfo.termTypeId = invoiceTerm.termTypeId
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate, invoiceTerm.termDays)

            BigDecimal invoiceTermAmount = (invoiceTerm.termValue * invoiceTotalAmount ) / 100
            invoicePaymentInfo.amount = invoiceTermAmount
            computedTotalAmount = computedTotalAmount + (BigDecimal) invoicePaymentInfo.amount

            if (remainingAppliedAmount >= invoiceTermAmount) {
                invoicePaymentInfo.paidAmount = invoiceTermAmount
                remainingAppliedAmount = remainingAppliedAmount - invoiceTermAmount
            } else {
                invoicePaymentInfo.paidAmount = remainingAppliedAmount
                remainingAppliedAmount = (BigDecimal) 0
            }
            invoicePaymentInfo.outstandingAmount = invoicePaymentInfo.amount - invoicePaymentInfo.paidAmount
            invoicePaymentInfoList.add(invoicePaymentInfo)
        }
    }

    if (remainingAppliedAmount > 0.0 || invoiceTotalAmount <= 0.0 || computedTotalAmount < invoiceTotalAmount) {
        invoicePaymentInfo.clear()
        invoiceTerm = from("InvoiceTerm").where("invoiceId", invoice.invoiceId, "termTypeId", "FIN_PAYMENT_TERM").queryFirst()
        if (invoiceTerm) {
            invoicePaymentInfo.termTypeId = invoiceTerm.termTypeId
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate, invoiceTerm.termDays)
        } else {
            invoicePaymentInfo.dueDate = UtilDateTime.getDayEnd(invoice.invoiceDate)
        }
        invoicePaymentInfo.invoiceId = invoice.invoiceId
        invoicePaymentInfo.amount = invoiceTotalAmount - computedTotalAmount
        invoicePaymentInfo.paidAmount = remainingAppliedAmount
        invoicePaymentInfo.outstandingAmount = invoicePaymentInfo.amount - invoicePaymentInfo.paidAmount
        invoicePaymentInfoList.add(invoicePaymentInfo)
    }
    Map result = success()
    result.invoicePaymentInfoList = invoicePaymentInfoList
    return result

}
