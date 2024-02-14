package com.fidelissd.invoice

import com.fidelissd.fsdOrderManager.orderentity.builders.InvoiceBuilder
import com.simbaquartz.xcommon.collections.FastList
import org.ofbiz.base.util.UtilValidate
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.util.EntityQuery
import org.ofbiz.service.ServiceUtil

//Fetch Quote Info (for Solicitation Number) for Invoice PDF
def invoiceId = parameters.invoiceId;
GenericValue quote = InvoiceBuilder.invoice(delegator, invoiceId).order().quote().build()
context.quote = InvoiceBuilder.invoice(delegator, invoiceId).order().quote().build()

// get place of performance info
if(UtilValidate.isNotEmpty(quote)) {
    String quoteId = quote.quoteId
    List<GenericValue> quoteShipGroups = from("QuoteItemShipGroup").where("quoteId", quoteId).queryList()
    context.quoteShipGroups = quoteShipGroups

    String projectId = quote.projectId
    if(UtilValidate.isNotEmpty(projectId)) {
        GenericValue project = from("Project").where("projectId", projectId).queryOne();
        if(UtilValidate.isNotEmpty(project)) {
            context.projectName = project.getString("name");
        }
    }
}

// get task items for software type quote
if(quote.quoteTypeId == "SOFTWARE_QUOTE"){
    List<Map> taskItems = FastList.newInstance();
    BigDecimal totalTasksCost = BigDecimal.ZERO;
    String totalCostLabel = ""

    // check if invoice id relates to timesheet
    GenericValue timesheetInvoice = EntityQuery.use(delegator).from("TimesheetInvoice")
    .where("invoiceId", invoiceId, "quoteId", quote.quoteId).queryOne();

    if(UtilValidate.isEmpty(timesheetInvoice)) {
        Map<String, Object> getQuoteItemsCtx = [:]

        getQuoteItemsCtx.put("userLogin", userLogin)
        getQuoteItemsCtx.put("quoteId", quote.quoteId)

        Map<String, Object> getQuoteItemsCtxResp = dispatcher.runSync("getQuoteTaskItems", getQuoteItemsCtx)
        if (!ServiceUtil.isSuccess(getQuoteItemsCtxResp)) {
            return "error";
        }
        taskItems = (List<Map>) getQuoteItemsCtxResp.taskItems
        totalTasksCost = (BigDecimal) getQuoteItemsCtxResp.totalTasksCost
        totalCostLabel = "Total Estimated Cost"
    } else {
        Map<String, Object> getQuoteItemsCtx = [:]

        getQuoteItemsCtx.put("userLogin", userLogin)
        getQuoteItemsCtx.put("quoteId", quote.quoteId)

        Map<String, Object> getQuoteItemsCtxResp = dispatcher.runSync("getQuoteTimesheetItems", getQuoteItemsCtx)
        if (!ServiceUtil.isSuccess(getQuoteItemsCtxResp)) {
            return "error";
        }
        taskItems = (List<Map>) getQuoteItemsCtxResp.taskItems
        totalTasksCost = (BigDecimal) getQuoteItemsCtxResp.totalTasksCost
        totalCostLabel = "Total Amount Payable"
    }

    context.taskItems = taskItems
    context.totalTasksCost = totalTasksCost
    context.totalCostLabel = totalCostLabel
    context.currency = "USD"
}
