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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.EntityFindOptions;

import javolution.util.FastList;
if (invoiceTypeId) {
    List invoiceStatusesCondition = [];
    invoiceStatusesCondition.add(EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, invoiceTypeId));
    if ("PURCHASE_INVOICE".equals(invoiceTypeId)) {
        invoiceStatusesCondition.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["INVOICE_RECEIVED", "INVOICE_IN_PROCESS"]));
    } else if ("SALES_INVOICE".equals(invoiceTypeId)) {
        invoiceStatusesCondition.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["INVOICE_SENT", "INVOICE_APPROVED"]));
    }
    List pastDueInvoicesCondition = [];
    pastDueInvoicesCondition.addAll(invoiceStatusesCondition);
    pastDueInvoicesCondition.add(EntityCondition.makeCondition("dueDate", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp()));
    invoicesCond = EntityCondition.makeCondition(pastDueInvoicesCondition, EntityOperator.AND);
    PastDueInvoices = delegator.findList("Invoice", invoicesCond, null, ["dueDate DESC"], null, false);
    if (PastDueInvoices) {
        invoiceIds = PastDueInvoices.invoiceId;
        totalAmount = dispatcher.runSync("getInvoiceRunningTotal", [invoiceIds: invoiceIds, organizationPartyId: organizationPartyId, userLogin: userLogin]);
        if (totalAmount) {
            context.PastDueInvoicestotalAmount = totalAmount.invoiceRunningTotal;
        }
        context.PastDueInvoices = PastDueInvoices;
    }
    
    List invoicesDueSoonCondition = [];
    invoicesDueSoonCondition.addAll(invoiceStatusesCondition);
    invoicesDueSoonCondition.add(EntityCondition.makeCondition("dueDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
    invoicesCond = EntityCondition.makeCondition(invoicesDueSoonCondition, EntityOperator.AND);
    EntityFindOptions findOptions = new EntityFindOptions();
    findOptions.setMaxRows(10);
    InvoicesDueSoon = delegator.findList("Invoice", invoicesCond, null, ["dueDate ASC"], findOptions, false);
    if (InvoicesDueSoon) {
        invoiceIds = InvoicesDueSoon.invoiceId;
        totalAmount = dispatcher.runSync("getInvoiceRunningTotal", [invoiceIds: invoiceIds, organizationPartyId: organizationPartyId, userLogin: userLogin]);
        if (totalAmount) {
            context.InvoicesDueSoonTotalAmount = totalAmount.invoiceRunningTotal;
        }
        context.InvoicesDueSoon = InvoicesDueSoon;
    }
}
