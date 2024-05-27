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
package org.apache.ofbiz.party.party

import java.math.RoundingMode

import org.apache.ofbiz.accounting.invoice.InvoiceWorker
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

Boolean actualCurrency = Boolean.valueOf(context.actualCurrency)
if (actualCurrency == null) {
    actualCurrency = true
}

invExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'INVOICE_IN_PROCESS'),
        EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'INVOICE_WRITEOFF'),
        EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'INVOICE_CANCELLED'),
        EntityCondition.makeCondition([
                EntityCondition.makeCondition([
                    EntityCondition.makeCondition('partyId', EntityOperator.EQUALS, parameters.partyId),
                    EntityCondition.makeCondition('partyIdFrom', EntityOperator.EQUALS, context.defaultOrganizationPartyId)
                ], EntityOperator.AND),
            EntityCondition.makeCondition([
                EntityCondition.makeCondition('partyId', EntityOperator.EQUALS, context.defaultOrganizationPartyId),
                EntityCondition.makeCondition('partyIdFrom', EntityOperator.EQUALS, parameters.partyId)
            ], EntityOperator.AND)
        ], EntityOperator.OR)
    ], EntityOperator.AND)

invIterator = from('InvoiceAndType').where(invExprs).cursorScrollInsensitive().distinct().queryIterator()
invoiceList = []

/* codenarc-disable */
while (invoice = invIterator.next()) {
/* codenarc-enable */
    unAppliedAmount = InvoiceWorker.getInvoiceNotApplied(invoice, actualCurrency).setScale(2, RoundingMode.HALF_UP)
    if (unAppliedAmount.signum() == 1) {
        if (actualCurrency == true) {
            invoiceCurrencyUomId = invoice.currencyUomId
        } else {
            invoiceCurrencyUomId = context.defaultOrganizationPartyCurrencyUomId
        }
        invoiceList.add([invoiceId: invoice.invoiceId,
                         invoiceDate: invoice.invoiceDate,
                         unAppliedAmount: unAppliedAmount,
                         invoiceCurrencyUomId: invoiceCurrencyUomId,
                         amount: InvoiceWorker.getInvoiceTotal(invoice, actualCurrency).setScale(2, RoundingMode.HALF_UP),
                         invoiceTypeId: invoice.invoiceTypeId,
                         invoiceParentTypeId: invoice.parentTypeId])
    }
}
invIterator.close()

context.ListUnAppliedInvoices = invoiceList
