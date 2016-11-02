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


import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

exprBldr = new org.apache.ofbiz.entity.condition.EntityConditionBuilder()

if (invoiceTypeId) {
    expr = exprBldr.AND() {
        EQUALS(invoiceTypeId: invoiceTypeId)
        LESS_THAN(dueDate: UtilDateTime.nowTimestamp())
    }
    if ("PURCHASE_INVOICE".equals(invoiceTypeId)) {
        invoiceStatusesCondition = exprBldr.IN(statusId: ["INVOICE_RECEIVED", "INVOICE_IN_PROCESS", "INVOICE_READY"])
    } else if ("SALES_INVOICE".equals(invoiceTypeId)) {
        invoiceStatusesCondition = exprBldr.IN(statusId: ["INVOICE_SENT", "INVOICE_APPROVED", "INVOICE_READY"])
    }
    expr = exprBldr.AND([expr, invoiceStatusesCondition])

    PastDueInvoices = from("Invoice").where(expr).orderBy("dueDate DESC").queryList()
    if (PastDueInvoices) {
        invoiceIds = PastDueInvoices.invoiceId
        totalAmount = runService('getInvoiceRunningTotal', [invoiceIds: invoiceIds, organizationPartyId: organizationPartyId])
        if (totalAmount) {
            context.PastDueInvoicestotalAmount = totalAmount.invoiceRunningTotal
        }
        context.PastDueInvoices = PastDueInvoices
    }

    invoicesCond = exprBldr.AND(invoiceStatusesCondition) {
        EQUALS(invoiceTypeId: invoiceTypeId)
        GREATER_THAN_EQUAL_TO(dueDate: UtilDateTime.nowTimestamp())
    }
    InvoicesDueSoon = from("Invoice").where(invoicesCond).orderBy("dueDate ASC").maxRows(10).queryList()
    if (InvoicesDueSoon) {
        invoiceIds = InvoicesDueSoon.invoiceId
        totalAmount = runService('getInvoiceRunningTotal', [invoiceIds: invoiceIds, organizationPartyId: organizationPartyId])
        if (totalAmount) {
            context.InvoicesDueSoonTotalAmount = totalAmount.invoiceRunningTotal
        }
        context.InvoicesDueSoon = InvoicesDueSoon
    }
}
