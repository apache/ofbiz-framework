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
