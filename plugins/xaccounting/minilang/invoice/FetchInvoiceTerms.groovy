package com.fidelissd.invoice

import com.fidelissd.fsdOrderManager.orderentity.role.OrderEntityRole
import com.fidelissd.fsdOrderManager.orderentity.role.OrderEntityRoleHelper
import com.fidelissd.hierarchy.orderentity.OrderEntityType
import com.fidelissd.product.terms.TermEntry
import org.ofbiz.base.util.UtilMisc
import org.ofbiz.base.util.UtilValidate
import org.ofbiz.entity.GenericValue
import org.ofbiz.service.ServiceUtil

String invoiceId = parameters.invoiceId;
Set<TermEntry> invoiceReportAllTerms  = new TreeSet<>();
context.invoiceReportAllTerms = invoiceReportAllTerms ;

// 1. Fetch Invoice level attributes
List<GenericValue> invoiceTerms = delegator.findByAnd("InvoiceTerm", UtilMisc.toMap("invoiceId", invoiceId), null, false);
Set<TermEntry> invoiceLevelTerms = new TreeSet<>();
if(invoiceTerms!=null && invoiceTerms.size()>0) {
    invoiceTerms.each { GenericValue invoiceTermGv ->
        GenericValue termTypeGv = invoiceTermGv.getRelatedOne("TermType", true);
        TermEntry quoteTermEntry = new TermEntry(termTypeGv.getString("description"), "");
        switch(invoiceTermGv.getString("termTypeId")) {
            case "FIN_PAYMENT_TERM":
                quoteTermEntry.value = invoiceTermGv.getLong("termDays").toString() ; break;
            case "FAR_FOB":
            default:
                quoteTermEntry.value = invoiceTermGv.getString("textValue");
        }
        invoiceLevelTerms.add(quoteTermEntry);
    }
}
context.invoiceReportAllTerms.addAll(invoiceLevelTerms);

// 2. get supplier terms & conditions
String partyId;
List<OrderEntityRole> invoiceSuppliers = OrderEntityRoleHelper.getOrderEntityRoles(delegator, invoiceId, OrderEntityType.INVOICE, "SUPPLIER");
if(UtilValidate.isEmpty(invoiceSuppliers))
{
    // try BILL_FROM_VENDOR
    invoiceSuppliers = OrderEntityRoleHelper.getOrderEntityRoles(delegator, invoiceId, OrderEntityType.INVOICE, "BILL_FROM_VENDOR");
}

if(UtilValidate.isNotEmpty(invoiceSuppliers))
{
    OrderEntityRole supplierRole = invoiceSuppliers.first();
    partyId = supplierRole.getPartyId();

    Map partyTermsResp = dispatcher.runSync("fetchPartyTerms", UtilMisc.toMap("partyId", partyId, "userLogin", userLogin));
    if(ServiceUtil.isSuccess(partyTermsResp)) {
        if(partyTermsResp.get("partyWarranties")!=null) context.invoiceReportAllTerms.addAll(partyTermsResp.get("partyWarranties"));
        if(partyTermsResp.get("partySpecialConditions")!=null) context.invoiceReportAllTerms.addAll(partyTermsResp.get("partySpecialConditions"))
    }
}

// 3. fetch terms & conditions for products in invoice
TreeSet uniqueProductIds = new TreeSet();
invoiceItems.each{invoiceItem ->
    if(UtilValidate.isNotEmpty(invoiceItem.getString("productId")))
        uniqueProductIds.add(invoiceItem.getString("productId"));
}
List<String> productsIdsList = new ArrayList();
productsIdsList.addAll(uniqueProductIds);
Map productsTermsResp = dispatcher.runSync("fetchTermsForProducts", UtilMisc.toMap("productIds", productsIdsList, "userLogin", userLogin));
if(ServiceUtil.isSuccess(productsTermsResp)) {
    if(productsTermsResp.get("productWarranties")!=null) context.invoiceReportAllTerms.addAll(productsTermsResp.get("productWarranties"));
    if(productsTermsResp.get("productSpecialConditions")!=null) context.invoiceReportAllTerms.addAll(productsTermsResp.get("productSpecialConditions"))
}

