package com.simbaquartz.xaccounting.services;


import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.accounting.invoice.InvoiceWorker;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;


public class InvoiceReportingServices {

    public static final String module = InvoiceReportingServices.class.getName();



    /**
     * Decide whether to not consider a record because it belongs to the category
     *
     * productsOfCategory:
     * In case of all categories except Others the products of the category are listed.
     * In case of Others, all products from all categories are listed.
     *
     * TODO: Try to use some filter approach instead of post-processing.
     *
     * @param productsOfCategory
     * @param productCategoryId
     * @param record
     * @return
     */
    public static  boolean considerRecord(List<String> productsOfCategory, String productCategoryId, Map record) {

        if (UtilValidate.isEmpty(productCategoryId)) {
            return true; // no categorization
        }
        if (UtilValidate.isEmpty(productsOfCategory)) {
            return true; // no categorization
        }

        /*List<String> quoteProducts = (List) record.get("quoteProducts");
        if (!productCategoryId.equals("Others")) {
            for (String quoteProduct : quoteProducts) {
                Map<String, String> productDetails = EmailNotificationServices.splitMergedRecords(quoteProduct);
                for (String productId : productsOfCategory) {
                    if (productId.equals((String) productDetails.get("productId"))) {
                        return true; // we have a match
                    }
                }
            }
        } else {
            for (String quoteProduct : quoteProducts) {
                boolean match = false;
                Map<String, String> productDetails = EmailNotificationServices.splitMergedRecords(quoteProduct);
                for (String productId : productsOfCategory) {
                    if (productId.equals((String) productDetails.get("productId"))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    // we have a quoteProduct not in any category
                    return true;
                }
            }
        }*/

        return false;
    }



    /**
     * Fetch Invoice Amount Payable or Receivable Summary.
     * Payables when InvoieType is PURCHASE_INVOICE, and Receivables when InvoiceType passed is SALES_INVOICE
     *
     * */
    public static Map<String, Object> getInvoicesAmountSummary(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceType = (String) context.get("invoiceType");
        List<String> statusesList = new ArrayList<>();
        if("SALES_INVOICE".equals(invoiceType)) {
            statusesList.add("INVOICE_SENT");
            statusesList.add("INVOICE_APPROVED");
            statusesList.add("INVOICE_READY");
        } else {
            statusesList.add("INVOICE_RECEIVED");
            statusesList.add("INVOICE_IN_PROCESS");
            statusesList.add("INVOICE_READY");
        }
        Map<String,BigDecimal> invoiceAmountsMap = new HashMap<>();

        try {
            EntityConditionList<EntityExpr> mainCond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, invoiceType),
                    EntityCondition.makeCondition("statusId", EntityOperator.IN, statusesList)),
                    EntityOperator.AND);
            List<GenericValue> invoices = EntityQuery.use(delegator).from("Invoice").where(mainCond).orderBy("dueDate DESC").queryList();
            for(GenericValue invoiceGv: invoices) {
                GenericValue invoiceBillFromParty = InvoiceWorker.getBillFromParty(invoiceGv);
                String partyIdFrom = invoiceBillFromParty.getString("partyId");

                GenericValue invoiceBillToParty = InvoiceWorker.getBillToParty(invoiceGv);
                String partyIdTo = invoiceBillToParty.getString("partyId");
                BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceGv);

                String partyIdToUse = partyIdTo;
                if(invoiceType.equals("PURCHASE_INVOICE")) {
                    partyIdToUse = partyIdFrom;
                }
                if(invoiceAmountsMap.containsKey(partyIdToUse)) {
                    BigDecimal existingTotal = invoiceAmountsMap.get(partyIdToUse);
                    existingTotal = existingTotal.add(invoiceTotal);
                    invoiceAmountsMap.put(partyIdToUse, existingTotal);
                } else {
                    invoiceAmountsMap.put(partyIdToUse, invoiceTotal);
                }
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        serviceResult.put("invoicesAmountSummary", invoiceAmountsMap);
        return serviceResult;
    }

    /**
     * Fetch Invoice Amount Receivable Summary for SALES_INVOICE.
     *
     * */
    public static Map<String, Object> getAccountsReceivableSummary(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }

        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");
        String quoteOrderedDateRangeFrom="";
        String quoteOrderedDateRangeTo="";
        String purchaseInvoiceDueDateRangeFrom = "";
        String purchaseInvoiceDueDateRangeTo = "";
        List<String> invoiceIdsList = new ArrayList<>();
        List<String> statusesList = new ArrayList<>();
        statusesList.add("INVOICE_SENT");
        statusesList.add("INVOICE_APPROVED");
        statusesList.add("INVOICE_READY");
        statusesList.add("INVOICE_PAID");
        Map<String, Object> accountReceivableSummaryMap = FastMap.newInstance();
        List<Map<String, Object>> billingPendingRecords = FastList.newInstance();
        List<Map<String, Object>> paymentPendingRecords = FastList.newInstance();
        String FSD_US_DATE_FORMAT = "MM/dd/yyyy";

        int pendingUnBilledAwards = 0;
        int pendingUnpaidAwards = 0;
        int paidAwards = 0;
        BigDecimal pendingUnBilledAwardTotalAmount = BigDecimal.ZERO;
        BigDecimal pendingUnpaidAwardTotalAmount = BigDecimal.ZERO;
        BigDecimal paidAwardTotalAmount = BigDecimal.ZERO;

        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteOrderedDate");
            searchFields.add("quoteSalesInvoiceId");
            searchFields.add("quoteTotal");
            searchFields.add("quoteSalesInvoiceOutstandingAmount");
            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);

            if (UtilValidate.isNotEmpty(keyword)) {
                if (keyword.contains("-")) keyword = keyword.replaceAll("-", "");
                filterQueryFields.add("_text_:" + keyword);
            } else {
                if(UtilValidate.isEmpty(keyword) && !(showPaidPurchaseInvoice) ) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceStatusId:PMNT_NOT_PAID";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeFrom"))) {
                    Timestamp dateRangeFrom = (Timestamp) context.get("dateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeFrom = isoDateFormat.format(new Date(dateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeTo"))) {
                    Timestamp dateRangeTo = (Timestamp) context.get("dateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeTo = isoDateFormat.format(new Date(dateRangeTo.getTime()));
                }

                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeFrom"))) {
                    Timestamp dueDateRangeFrom = (Timestamp) context.get("purchaseInvoiceDueDateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeFrom = isoDateFormat.format(new Date(dueDateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeTo"))) {
                    Timestamp dueDateRangeTo = (Timestamp) context.get("purchaseInvoiceDueDateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeTo = isoDateFormat.format(new Date(dueDateRangeTo.getTime()));
                }

                //agency filter
                String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }
                //customer payment filter
                String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //customer filter
                String customerPartyId = (String) context.get("customerPartyId");
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                String solicitationNumber = (String) context.get("solicitationNumber");
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //supplier filter
                List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteTagConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                //quote ordered date range filter
                if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom) && UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                    String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO " + quoteOrderedDateRangeTo + "]";
                    filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom)) {
                        String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO *]";
                        filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                        String quoteOrderedDateRangeToConstraint = "quoteOrderedDate:[* TO " + quoteOrderedDateRangeTo + "]";
                        filterQueryFields.add(quoteOrderedDateRangeToConstraint);
                    }
                }

                //purchase invoice due date range filter
                if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom) && UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                    String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO " + purchaseInvoiceDueDateRangeTo + "]";
                    filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom)) {
                        String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO *]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                        String purchaseInvoiceDueDateRangeToConstraint = "quotePurchaseInvoiceDueDate:[* TO " + purchaseInvoiceDueDateRangeTo + "]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeToConstraint);
                    }
                }

                //Quote Id filter
                String quoteId = (String) context.get("quoteId");
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                String quoteOrderNumber = (String) context.get("quoteOrderNumber");
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:"  +"\""+ quoteContractReferenceNumber +"\"";
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                String shipmentStatusId = (String) context.get("shipmentStatusId");
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
                if(UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)){
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                String quoteInvoiceId = (String) context.get("quoteInvoiceId");
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                String shippingCity = (String) context.get("shippingCity");
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                String shippingState = (String) context.get("shippingState");
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                List<String> tagIds = (List<String>) context.get("tagIds");
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
                String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
                String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
                String quoteInvoiceStatusTypeConstraint = "";
                if (UtilValidate.isNotEmpty(quoteInvoiceType) && UtilValidate.isNotEmpty(quoteInvoiceStatus)) {
                    if (quoteInvoiceType.equals("SALES_INVOICE")) {
                        quoteInvoiceStatusTypeConstraint = "quotePaymentStatusId:\"" + quoteInvoiceStatus + "\"";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    } else if (quoteInvoiceType.equals("PURCHASE_INVOICE")) {
                        if (quoteInvoiceStatus.equals("PMNT_NOT_PAID"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_IN_PROCESS\" OR  \"purchaseInvoiceStatusId:INVOICE_READY\")";
                        else if (quoteInvoiceStatus.equals("PMNT_RECEIVED"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_PAID\" )";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    }
                }

                //quoteFlag filter
                String quoteFlagStatus = (String) context.get("quoteFlagStatus");
                String quoteFlagStatusConstraint="";
                if(UtilValidate.isNotEmpty(quoteFlagStatus)){
                    if(quoteFlagStatus.equals("PAY_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "payImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                    if(quoteFlagStatus.equals("BILL_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "billImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                }

                //Cm Invoice Number filter
                String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId\" " + purchaseCMInvoiceId + " )";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }
            }

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            int totalDays=0;
            int totalCount=0;
            float averageNonBilledInvoiceDays;
            BigDecimal totalReceivableAwardsAmount = BigDecimal.ZERO;
            for (Map orderEntry : orderEntries) {
                if(UtilValidate.isNotEmpty(orderEntry.get("quoteSalesInvoiceId")))
                {
                    Map<String, String> invoiceInfo = AxAccountingHelper.splitMergedRecords(orderEntry.get("quoteSalesInvoiceId").toString());
                    String invoiceId = invoiceInfo.get("salesInvoiceId");
                    if (UtilValidate.isNotEmpty(invoiceId)) {
                        invoiceIdsList.add(invoiceId);
                    }
                }
                if(UtilValidate.isNotEmpty(orderEntry.get("quoteOrderedDate")))
                {
                    Date date=((Date) orderEntry.get("quoteOrderedDate"));
                    Timestamp awardDate=new Timestamp(date.getTime());
                    int interval=UtilDateTime.getIntervalInDays(awardDate,UtilDateTime.nowTimestamp());
                    totalDays=totalDays+interval;
                    totalCount++;
                }

                Float quoteSubTotal = (Float) orderEntry.get("quoteTotal");
                if (UtilValidate.isNotEmpty(quoteSubTotal)) {
                    BigDecimal subTotal = new BigDecimal(quoteSubTotal.toString());
                    totalReceivableAwardsAmount = totalReceivableAwardsAmount.add(subTotal);
                }

                Float quoteSalesInvoiceOutstandingAmount = (Float) orderEntry.get("quoteSalesInvoiceOutstandingAmount");
                if (UtilValidate.isNotEmpty(quoteSalesInvoiceOutstandingAmount)) {
                    BigDecimal saleInvoiceOutstandingTotal = new BigDecimal(quoteSalesInvoiceOutstandingAmount.toString());
                    pendingUnpaidAwardTotalAmount = pendingUnpaidAwardTotalAmount.add(saleInvoiceOutstandingTotal);
                }
            }

            averageNonBilledInvoiceDays=(float)totalDays/totalCount;

            EntityConditionList<EntityExpr> mainCond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"),
                    EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIdsList)),
                    EntityOperator.AND);
            List<GenericValue> invoices = EntityQuery.use(delegator).from("Invoice").where(mainCond).orderBy("dueDate DESC").queryList();
            if(UtilValidate.isNotEmpty(invoices)){
                for(GenericValue invoiceGv: invoices) {
                    Map<String, Object> pendingUnBilledInvoiceAppendix = FastMap.newInstance();
                    Map<String, Object> pendingUnpaidInvoiceAppendix = FastMap.newInstance();
                    BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceGv);
                    List<GenericValue> invoiceItemAdjustMent = EntityQuery.use(delegator).from("InvoiceAdjustment").where("invoiceId", invoiceGv.getString("invoiceId")).queryList();
                    if(UtilValidate.isNotEmpty(invoiceItemAdjustMent)) {
                        for(GenericValue invoiceAdj : invoiceItemAdjustMent) {
                            BigDecimal amount = invoiceAdj.getBigDecimal("amount");
                            invoiceTotal = invoiceTotal.add(amount);
                        }
                    }
                    //Payment Pending (Orders that have been invoiced but payment has not been received yet)
                    String statusId = invoiceGv.getString("statusId");
                    if(statusId.equals("INVOICE_PAID")) {
                        paidAwards++;
                    }else{
                        pendingUnpaidAwards++;
                        pendingUnpaidInvoiceAppendix.put("invoiceId", invoiceGv.getString("invoiceId"));
                        Timestamp invoiceDueDate = (Timestamp) invoiceGv.get("dueDate");
                        if(UtilValidate.isNotEmpty(invoiceDueDate)){
                            pendingUnpaidInvoiceAppendix.put("dueDate", UtilDateTime.toDateString(new java.util.Date(invoiceDueDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        Timestamp invoicedDate = (Timestamp) invoiceGv.get("invoiceDate");
                        if(UtilValidate.isNotEmpty(invoicedDate)){
                            pendingUnpaidInvoiceAppendix.put("invoiceDate", UtilDateTime.toDateString(new java.util.Date(invoicedDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        BigDecimal totalInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(invoiceGv);
                        pendingUnpaidInvoiceAppendix.put("totalInvoiceAmount", totalInvoiceAmount);
                        paymentPendingRecords.add(pendingUnpaidInvoiceAppendix);
                    }
                    List<GenericValue> invoiceBillingRecords = EntityQuery.use(delegator).from("InvoiceBilling").where("invoiceId", invoiceGv.getString("invoiceId")).queryList();
                    if(UtilValidate.isEmpty(invoiceBillingRecords)) {
                        //Billing Pending (Orders that have not been invoiced yet).
                        pendingUnBilledAwards++;
                        pendingUnBilledAwardTotalAmount = pendingUnBilledAwardTotalAmount.add(invoiceTotal);
                        pendingUnBilledInvoiceAppendix.put("invoiceId", invoiceGv.getString("invoiceId"));
                        Timestamp invoiceDueDate = (Timestamp) invoiceGv.get("dueDate");
                        if(UtilValidate.isNotEmpty(invoiceDueDate)){
                            pendingUnBilledInvoiceAppendix.put("dueDate", UtilDateTime.toDateString(new java.util.Date(invoiceDueDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        Timestamp invoicedDate = (Timestamp) invoiceGv.get("invoiceDate");
                        if(UtilValidate.isNotEmpty(invoicedDate)){
                            pendingUnBilledInvoiceAppendix.put("invoiceDate", UtilDateTime.toDateString(new java.util.Date(invoicedDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        BigDecimal totalInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(invoiceGv);
                        pendingUnBilledInvoiceAppendix.put("totalInvoiceAmount", totalInvoiceAmount);
                        billingPendingRecords.add(pendingUnBilledInvoiceAppendix);
                    }
                }
            }

            List<Map<String,Object>> pendingBillingListPastDue = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueIn7Days = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueIn15Days = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueIn30Days = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueIn60Days = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueIn90Days = FastList.newInstance();
            List<Map<String,Object>> pendingBillingListDueBeyond90Days = FastList.newInstance();


            for(Map<String, Object> payment: billingPendingRecords){
                List<String> filterQueryFieldsForPendingBilling = UtilMisc.toList("docType:quote", awardedQuoteConstraint);
                searchFields.add("quotePurchaseOrderNumber");
                searchFields.add("quoteCustomerName");
                searchFields.add("quoteSupplierPartyName");
                searchFields.add("quoteShippingDestinations");
                searchFields.add("quoteSalesInvoiceDueDate");

                String salesInvoiceId = (String) payment.get("invoiceId");
                if (UtilValidate.isNotEmpty(salesInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN(\"salesInvoiceId:" + salesInvoiceId + "\")";
                    filterQueryFieldsForPendingBilling.add(quoteSalesInvoiceIdConstraint);
                }

                Map performSolrSearchForPendingPurchaseContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFieldsForPendingBilling);
                Map performSolrSearchForPendingPurchaseResponse = dispatcher.runSync("performSolrSearch", performSolrSearchForPendingPurchaseContext);

                if (ServiceUtil.isError(performSolrSearchForPendingPurchaseResponse)) {
                    Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchForPendingPurchaseResponse), module);
                }

                List<Map> pendingBillingEntries = (List) performSolrSearchForPendingPurchaseResponse.get("records");

                for (Map record : pendingBillingEntries) {
                    payment.put("quotePurchaseOrderNumber", record.get("quotePurchaseOrderNumber"));
                    payment.put("quoteCustomerName", record.get("quoteCustomerName"));
                    payment.put("quoteSupplierPartyName", record.get("quoteSupplierPartyName"));
                    List<Map<String, String>> shippingDestinations = FastList.newInstance();
                    List<String> shippingDestinationRecords = (List<String>) record.get("quoteShippingDestinations");
                    if (UtilValidate.isNotEmpty(shippingDestinationRecords)) {
                        for (String shippingDestinationRecord : shippingDestinationRecords) {
                            Map shippingDestinationPostalAddress = AxAccountingHelper.splitMergedRecords(shippingDestinationRecord);

                            if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                                shippingDestinations.add(shippingDestinationPostalAddress);
                            }
                        }
                    }
                    payment.put("shippingDestinations", shippingDestinations);
                    
                    Date quoteSalesInvoiceDueDate = (Date) record.get("quoteSalesInvoiceDueDate");
                    if(UtilValidate.isNotEmpty(quoteSalesInvoiceDueDate)) {
                        Timestamp quoteSalesInvoiceDueDateTimestamp = new Timestamp(quoteSalesInvoiceDueDate.getTime());
                        Timestamp now = UtilDateTime.nowTimestamp();

                        if (quoteSalesInvoiceDueDateTimestamp.before(now)) {
                            pendingBillingListPastDue.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 7))) {
                            pendingBillingListDueIn7Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 15))) {
                            pendingBillingListDueIn15Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 30))) {
                            pendingBillingListDueIn30Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 60))) {
                            pendingBillingListDueIn60Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 90))) {
                            pendingBillingListDueIn90Days.add(payment);
                        } else {
                            pendingBillingListDueBeyond90Days.add(payment);
                        }
                    }
                }
            }

            accountReceivableSummaryMap.put("pendingBillingListPastDue", pendingBillingListPastDue);
            accountReceivableSummaryMap.put("pendingBillingListDueIn7Days", pendingBillingListDueIn7Days);
            accountReceivableSummaryMap.put("pendingBillingListDueIn15Days", pendingBillingListDueIn15Days);
            accountReceivableSummaryMap.put("pendingBillingListDueIn30Days", pendingBillingListDueIn30Days);
            accountReceivableSummaryMap.put("pendingBillingListDueIn60Days", pendingBillingListDueIn60Days);
            accountReceivableSummaryMap.put("pendingBillingListDueIn90Days", pendingBillingListDueIn90Days);
            accountReceivableSummaryMap.put("pendingBillingListDueBeyond90Days", pendingBillingListDueBeyond90Days);

            accountReceivableSummaryMap.put("pendingUnBilledAwards", pendingUnBilledAwards);
            accountReceivableSummaryMap.put("pendingUnBilledAwardTotalAmount", pendingUnBilledAwardTotalAmount);

            List<Map<String,Object>> pendingPaymentListPastDue = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn7Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn15Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn30Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn60Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn90Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueBeyond90Days = FastList.newInstance();
            
            for(Map<String, Object> payment: paymentPendingRecords){
                List<String> filterQueryFieldsForPendingSales = UtilMisc.toList("docType:quote", awardedQuoteConstraint);
                searchFields.add("quotePurchaseOrderNumber");
                searchFields.add("quoteCustomerName");
                searchFields.add("quoteSupplierPartyName");
                searchFields.add("quoteShippingDestinations");

                String salesInvoiceId = (String) payment.get("invoiceId");
                if (UtilValidate.isNotEmpty(salesInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN(\"salesInvoiceId:" + salesInvoiceId + "\")";
                    filterQueryFieldsForPendingSales.add(quoteSalesInvoiceIdConstraint);
                }

                Map performSolrSearchForPendingPurchaseContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFieldsForPendingSales);
                Map performSolrSearchForPendingPurchaseResponse = dispatcher.runSync("performSolrSearch", performSolrSearchForPendingPurchaseContext);

                if (ServiceUtil.isError(performSolrSearchForPendingPurchaseResponse)) {
                    Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchForPendingPurchaseResponse), module);
                }

                List<Map> pendingSalesEntries = (List) performSolrSearchForPendingPurchaseResponse.get("records");

                for (Map record : pendingSalesEntries) {
                    payment.put("quotePurchaseOrderNumber", record.get("quotePurchaseOrderNumber"));
                    payment.put("quoteCustomerName", record.get("quoteCustomerName"));
                    payment.put("quoteSupplierPartyName", record.get("quoteSupplierPartyName"));
                    List<Map<String, String>> shippingDestinations = FastList.newInstance();
                    List<String> shippingDestinationRecords = (List<String>) record.get("quoteShippingDestinations");
                    if (UtilValidate.isNotEmpty(shippingDestinationRecords)) {
                        for (String shippingDestinationRecord : shippingDestinationRecords) {
                            Map shippingDestinationPostalAddress = AxAccountingHelper.splitMergedRecords(shippingDestinationRecord);

                            if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                                shippingDestinations.add(shippingDestinationPostalAddress);
                            }
                        }
                    }
                    payment.put("shippingDestinations", shippingDestinations);
                    
                    Date quoteSalesInvoiceDueDate = (Date) record.get("quoteSalesInvoiceDueDate");
                    if(UtilValidate.isNotEmpty(quoteSalesInvoiceDueDate)) {
                        Timestamp quoteSalesInvoiceDueDateTimestamp = new Timestamp(quoteSalesInvoiceDueDate.getTime());
                        Timestamp now = UtilDateTime.nowTimestamp();

                        if (quoteSalesInvoiceDueDateTimestamp.before(now)) {
                            pendingPaymentListPastDue.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 7))) {
                            pendingPaymentListDueIn7Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 15))) {
                            pendingPaymentListDueIn15Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 30))) {
                            pendingPaymentListDueIn30Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 60))) {
                            pendingPaymentListDueIn60Days.add(payment);
                        } else if (quoteSalesInvoiceDueDateTimestamp.before(UtilDateTime.addDaysToTimestamp(now, 90))) {
                            pendingPaymentListDueIn90Days.add(payment);
                        } else {
                            pendingPaymentListDueBeyond90Days.add(payment);
                        }
                    }
                }
            }

            accountReceivableSummaryMap.put("pendingPaymentListPastDue", pendingPaymentListPastDue);
            accountReceivableSummaryMap.put("pendingPaymentListDueIn7Days", pendingPaymentListDueIn7Days);
            accountReceivableSummaryMap.put("pendingPaymentListDueIn15Days", pendingPaymentListDueIn15Days);
            accountReceivableSummaryMap.put("pendingPaymentListDueIn30Days", pendingPaymentListDueIn30Days);
            accountReceivableSummaryMap.put("pendingPaymentListDueIn60Days", pendingPaymentListDueIn60Days);
            accountReceivableSummaryMap.put("pendingPaymentListDueIn90Days", pendingPaymentListDueIn90Days);
            accountReceivableSummaryMap.put("pendingPaymentListDueBeyond90Days", pendingPaymentListDueBeyond90Days);
            accountReceivableSummaryMap.put("averageNonBilledInvoiceDays", averageNonBilledInvoiceDays);

            paidAwardTotalAmount = totalReceivableAwardsAmount.subtract(pendingUnpaidAwardTotalAmount);
            accountReceivableSummaryMap.put("pendingUnpaidAwardTotalAmount", pendingUnpaidAwardTotalAmount);
            accountReceivableSummaryMap.put("paidAwardTotalAmount", paidAwardTotalAmount);
            accountReceivableSummaryMap.put("totalReceivableAwardsAmount", totalReceivableAwardsAmount);
            accountReceivableSummaryMap.put("pendingUnpaidAwards", pendingUnpaidAwards);
            accountReceivableSummaryMap.put("paidAwards", paidAwards);


        } catch (GenericEntityException |GenericServiceException e) {
            e.printStackTrace();
        }
        serviceResult.put("accountReceivableSummaryMap", accountReceivableSummaryMap);
        return serviceResult;
    }

    /**
     * Invoice Amount Receivable Summary Stats for SALES_INVOICE which will give count and dollar value.
     *
     * */
    public static Map<String, Object> getAccountsReceivableSummaryStats(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }

        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");
        String quoteOrderedDateRangeFrom="";
        String quoteOrderedDateRangeTo="";
        String purchaseInvoiceDueDateRangeFrom = "";
        String purchaseInvoiceDueDateRangeTo = "";
        Map<String, Object> accountReceivableSummaryMap = FastMap.newInstance();

        int pendingUnpaidAwards = 0;
        int paidAwards = 0;
        BigDecimal pendingUnpaidAwardTotalAmount = BigDecimal.ZERO;
        BigDecimal paidAwardTotalAmount = BigDecimal.ZERO;

        // WAD-2998: Filter by category of product
        List<String> productsOfCategory = FastList.newInstance();
        String productCategoryId = (String) context.get("productCategoryId");
        String onHold = (String) context.get("onHold");
        List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
        try {
            if (UtilValidate.isNotEmpty(productCategoryId) && (supplierPartyIds.size() == 1)) {
                // filter only if there is one supplier
                if (productCategoryId.equals("Others")) {
                    // collect from ALL categories
                    List<GenericValue> categoriesForProducts = EntityQuery.use(delegator)
                            .from("ProductCategoryMember")
                            .filterByDate()
                            .queryList();
                    for (GenericValue categoryForProduct : categoriesForProducts) {
                        String productId = (String) categoryForProduct.get("productId");
                        productsOfCategory.add(productId);
                    }
                } else {
                    List<GenericValue> categoriesForProducts = EntityQuery.use(delegator)
                            .from("ProductCategoryMember")
                            .where("productCategoryId", productCategoryId)
                            .filterByDate()
                            .queryList();
                    for (GenericValue categoryForProduct : categoriesForProducts) {
                        String productId = (String) categoryForProduct.get("productId");
                        productsOfCategory.add(productId);
                    }
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }


        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quotePaymentStatusId");
            searchFields.add("quoteTotal");
            searchFields.add("quoteSalesInvoiceOutstandingAmount");
            searchFields.add("quoteProducts");
            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);

            if (UtilValidate.isNotEmpty(keyword)) {
                if (keyword.contains("-")) keyword = keyword.replaceAll("-", "");
                filterQueryFields.add("_text_:" + keyword);
            } else {
                if(UtilValidate.isEmpty(keyword) && !(showPaidPurchaseInvoice) ) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceStatusId:PMNT_NOT_PAID";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeFrom"))) {
                    Timestamp dateRangeFrom = (Timestamp) context.get("dateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeFrom = isoDateFormat.format(new Date(dateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeTo"))) {
                    Timestamp dateRangeTo = (Timestamp) context.get("dateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeTo = isoDateFormat.format(new Date(dateRangeTo.getTime()));
                }

                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeFrom"))) {
                    Timestamp dueDateRangeFrom = (Timestamp) context.get("purchaseInvoiceDueDateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeFrom = isoDateFormat.format(new Date(dueDateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeTo"))) {
                    Timestamp dueDateRangeTo = (Timestamp) context.get("purchaseInvoiceDueDateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeTo = isoDateFormat.format(new Date(dueDateRangeTo.getTime()));
                }

                //customer payment filter
                String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //agency filter
                String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }

                //customer filter
                String customerPartyId = (String) context.get("customerPartyId");
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                String solicitationNumber = (String) context.get("solicitationNumber");
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //supplier filter
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteTagConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                //quote ordered date range filter
                if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom) && UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                    String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO " + quoteOrderedDateRangeTo + "]";
                    filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom)) {
                        String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO *]";
                        filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                        String quoteOrderedDateRangeToConstraint = "quoteOrderedDate:[* TO " + quoteOrderedDateRangeTo + "]";
                        filterQueryFields.add(quoteOrderedDateRangeToConstraint);
                    }
                }

                //purchase invoice due date range filter
                if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom) && UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                    String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO " + purchaseInvoiceDueDateRangeTo + "]";
                    filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom)) {
                        String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO *]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                        String purchaseInvoiceDueDateRangeToConstraint = "quotePurchaseInvoiceDueDate:[* TO " + purchaseInvoiceDueDateRangeTo + "]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeToConstraint);
                    }
                }

                //Quote Id filter
                String quoteId = (String) context.get("quoteId");
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                String quoteOrderNumber = (String) context.get("quoteOrderNumber");
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:"  +"\""+ quoteContractReferenceNumber +"\"";
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                String shipmentStatusId = (String) context.get("shipmentStatusId");
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
                if(UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)){
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                String quoteInvoiceId = (String) context.get("quoteInvoiceId");
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                String shippingCity = (String) context.get("shippingCity");
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                String shippingState = (String) context.get("shippingState");
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                List<String> tagIds = (List<String>) context.get("tagIds");
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
                String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
                String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
                String quoteInvoiceStatusTypeConstraint = "";
                if (UtilValidate.isNotEmpty(quoteInvoiceType) && UtilValidate.isNotEmpty(quoteInvoiceStatus)) {
                    if (quoteInvoiceType.equals("SALES_INVOICE")) {
                        quoteInvoiceStatusTypeConstraint = "quotePaymentStatusId:\"" + quoteInvoiceStatus + "\"";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    } else if (quoteInvoiceType.equals("PURCHASE_INVOICE")) {
                        if (quoteInvoiceStatus.equals("PMNT_NOT_PAID"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_IN_PROCESS\" OR  \"purchaseInvoiceStatusId:INVOICE_READY\")";
                        else if (quoteInvoiceStatus.equals("PMNT_RECEIVED"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_PAID\" )";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    }
                }

                //Cm Invoice Number filter
                String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId\" " + purchaseCMInvoiceId + " )";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }

                // onHold filter
                if (UtilValidate.isNotEmpty(onHold)) {
                    if (onHold.equals("ON_HOLD")) {
                        String onHoldFilter = "quoteOnHold:Y";
                        filterQueryFields.add(onHoldFilter);
                    } else if (onHold.equals("NOT_ON_HOLD")) {
                        String onHoldFilter = "quoteOnHold:N";
                        filterQueryFields.add(onHoldFilter);
                    }
                }

                //quoteFlag filter
                String quoteFlagStatus = (String) context.get("quoteFlagStatus");
                String quoteFlagStatusConstraint="";
                if(UtilValidate.isNotEmpty(quoteFlagStatus)){
                    if(quoteFlagStatus.equals("PAY_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "payImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                    if(quoteFlagStatus.equals("BILL_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "billImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                }
            }

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");
            BigDecimal totalReceivableAwardsAmount = BigDecimal.ZERO;
            for (Map orderEntry : orderEntries) {
                if (!considerRecord(productsOfCategory, productCategoryId, orderEntry)) {
                    continue;
                }

                String quotePaymentStatus = (String) orderEntry.get("quotePaymentStatusId");
                if(UtilValidate.isNotEmpty(quotePaymentStatus)) {
                    if(quotePaymentStatus.equals("PMNT_RECEIVED")) {
                        paidAwards++;
                    } else {
                        pendingUnpaidAwards++;
                    }
                }
                Float quoteSubTotal = (Float) orderEntry.get("quoteTotal");
                if (UtilValidate.isNotEmpty(quoteSubTotal)) {
                    BigDecimal subTotal = new BigDecimal(quoteSubTotal.toString());
                    totalReceivableAwardsAmount = totalReceivableAwardsAmount.add(subTotal);
                }

                Float quoteSalesInvoiceOutstandingAmount = (Float) orderEntry.get("quoteSalesInvoiceOutstandingAmount");
                if (UtilValidate.isNotEmpty(quoteSalesInvoiceOutstandingAmount)) {
                    BigDecimal saleInvoiceOutstandingTotal = new BigDecimal(quoteSalesInvoiceOutstandingAmount.toString());
                    pendingUnpaidAwardTotalAmount = pendingUnpaidAwardTotalAmount.add(saleInvoiceOutstandingTotal);
                }
            }
            paidAwardTotalAmount = totalReceivableAwardsAmount.subtract(pendingUnpaidAwardTotalAmount);
            accountReceivableSummaryMap.put("pendingUnpaidAwards", pendingUnpaidAwards);
            accountReceivableSummaryMap.put("pendingUnpaidAwardTotalAmount", pendingUnpaidAwardTotalAmount);
            accountReceivableSummaryMap.put("paidAwardTotalAmount", paidAwardTotalAmount);
            accountReceivableSummaryMap.put("totalReceivableAwardsAmount", totalReceivableAwardsAmount);
            accountReceivableSummaryMap.put("paidAwards", paidAwards);

        } catch (GenericServiceException e) {
            e.printStackTrace();
        }
        serviceResult.put("accountReceivableSummaryMap", accountReceivableSummaryMap);
        return serviceResult;
    }

    /**
     * Fetch Invoice Amount Payable Summary for PURCHASE_INVOICE.
     *
     * */
    public static Map<String, Object> getAccountsPayableSummaryForFinanceReportPdf(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }
        
        String quoteOrderedDateRangeFrom="";
        String quoteOrderedDateRangeTo="";
        String purchaseInvoiceDueDateRangeFrom="";
        String purchaseInvoiceDueDateRangeTo="";
        List<String> invoiceIdsList = new ArrayList<>();
        List<String> statusesList = new ArrayList<>();
        statusesList.add("INVOICE_RECEIVED");
        statusesList.add("INVOICE_IN_PROCESS");
        statusesList.add("INVOICE_READY");
        statusesList.add("INVOICE_PAID");
        Map<String, Object> accountPayableSummaryMap = FastMap.newInstance();
        List<Map<String, Object>> paymentPendingRecords = FastList.newInstance();
        String FSD_US_DATE_FORMAT = "MM/dd/yyyy";

        int pendingUnpaidAwards = 0;
        int paidAwards = 0;
        BigDecimal pendingUnpaidAwardTotalAmount = BigDecimal.ZERO;
        BigDecimal paidAwardTotalAmount = BigDecimal.ZERO;

        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteOrderedDate");
            searchFields.add("quoteSalesInvoiceId");
            searchFields.add("quotePurchaseInvoiceIds");
            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);

            if (UtilValidate.isNotEmpty(keyword)) {
                if (keyword.contains("-")) keyword = keyword.replaceAll("-", "");
                filterQueryFields.add("_text_:" + keyword);
            } else {
                if (UtilValidate.isNotEmpty(context.get("dateRangeFrom"))) {
                    Timestamp dateRangeFrom = (Timestamp) context.get("dateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeFrom = isoDateFormat.format(new Date(dateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeTo"))) {
                    Timestamp dateRangeTo = (Timestamp) context.get("dateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeTo = isoDateFormat.format(new Date(dateRangeTo.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeFrom"))) {
                    Timestamp dueDateRangeFrom = (Timestamp) context.get("purchaseInvoiceDueDateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeFrom = isoDateFormat.format(new Date(dueDateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeTo"))) {
                    Timestamp dueDateRangeTo = (Timestamp) context.get("purchaseInvoiceDueDateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeTo = isoDateFormat.format(new Date(dueDateRangeTo.getTime()));
                }

                //agency filter
                String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }

                //customer payment filter
                String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //customer filter
                String customerPartyId = (String) context.get("customerPartyId");
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                String solicitationNumber = (String) context.get("solicitationNumber");
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //supplier filter
                List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteTagConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                //quote ordered date range filter
                if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom) && UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                    String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO " + quoteOrderedDateRangeTo + "]";
                    filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom)) {
                        String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO *]";
                        filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                        String quoteOrderedDateRangeToConstraint = "quoteOrderedDate:[* TO " + quoteOrderedDateRangeTo + "]";
                        filterQueryFields.add(quoteOrderedDateRangeToConstraint);
                    }
                }

                //purchase invoice due date range filter
                if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom) && UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                    String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO " + purchaseInvoiceDueDateRangeTo + "]";
                    filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom)) {
                        String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO *]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                        String purchaseInvoiceDueDateRangeToConstraint = "quotePurchaseInvoiceDueDate:[* TO " + purchaseInvoiceDueDateRangeTo + "]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeToConstraint);
                    }
                }

                //Quote Id filter
                String quoteId = (String) context.get("quoteId");
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                String quoteOrderNumber = (String) context.get("quoteOrderNumber");
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:" +"\""+ quoteContractReferenceNumber +"\"";
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                String shipmentStatusId = (String) context.get("shipmentStatusId");
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
                if(UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)){
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                String quoteInvoiceId = (String) context.get("quoteInvoiceId");
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                String shippingCity = (String) context.get("shippingCity");
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                String shippingState = (String) context.get("shippingState");
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                List<String> tagIds = (List<String>) context.get("tagIds");
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
                String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
                String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
                String quoteInvoiceStatusTypeConstraint = "";
                if (UtilValidate.isNotEmpty(quoteInvoiceType) && UtilValidate.isNotEmpty(quoteInvoiceStatus)) {
                    if (quoteInvoiceType.equals("SALES_INVOICE")) {
                        quoteInvoiceStatusTypeConstraint = "quotePaymentStatusId:\"" + quoteInvoiceStatus + "\"";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    } else if (quoteInvoiceType.equals("PURCHASE_INVOICE")) {
                        if (quoteInvoiceStatus.equals("PMNT_NOT_PAID"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_IN_PROCESS\" OR  \"purchaseInvoiceStatusId:INVOICE_READY\")";
                        else if (quoteInvoiceStatus.equals("PMNT_RECEIVED"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_PAID\" )";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    }
                }

                //quoteFlag filter
                String quoteFlagStatus = (String) context.get("quoteFlagStatus");
                String quoteFlagStatusConstraint="";
                if(UtilValidate.isNotEmpty(quoteFlagStatus)){
                    if(quoteFlagStatus.equals("PAY_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "payImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                    if(quoteFlagStatus.equals("BILL_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "billImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                }

                //Cm Invoice Number filter
                String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId\" " + purchaseCMInvoiceId + " )";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }
            }

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            int totalDays=0;
            int totalCount=0;
            float averageNonBilledInvoiceDays;
            for (Map orderEntry : orderEntries) {
                if(UtilValidate.isNotEmpty(orderEntry.get("quotePurchaseInvoiceIds")))
                {

                    List<String> quotePurchaseInvoiceIds = (List<String>) orderEntry.get("quotePurchaseInvoiceIds");
                    String invoiceId = "";
                    if (UtilValidate.isNotEmpty(quotePurchaseInvoiceIds)) {
                        for(String quotePurchaseInvoiceId : quotePurchaseInvoiceIds) {
                            Map<String, String> invoiceInfo = AxAccountingHelper.splitMergedRecords(quotePurchaseInvoiceId);
                            invoiceId = invoiceInfo.get("purchaseInvoiceId");
                            invoiceIdsList.add(invoiceId);
                        }
                    }
                }
                if(UtilValidate.isNotEmpty(orderEntry.get("quoteOrderedDate")))
                {
                    Date date=((Date) orderEntry.get("quoteOrderedDate"));
                    Timestamp awardDate=new Timestamp(date.getTime());
                    int interval=UtilDateTime.getIntervalInDays(awardDate,UtilDateTime.nowTimestamp());
                    totalDays=totalDays+interval;
                    totalCount++;
                }
            }
            averageNonBilledInvoiceDays=(float)totalDays/totalCount;
            EntityConditionList<EntityExpr> mainCond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "PURCHASE_INVOICE"),
                    EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIdsList)),
                    EntityOperator.AND);
            List<GenericValue> invoices = EntityQuery.use(delegator).from("Invoice").where(mainCond).orderBy("dueDate DESC").queryList();
            if(UtilValidate.isNotEmpty(invoices)){
                for(GenericValue invoiceGv: invoices) {
                    Map<String, Object> pendingUnpaidInvoiceAppendix = FastMap.newInstance();
                    BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceGv);
                    //Payment Pending (Orders that have not been invoiced yet).
                    String statusId = invoiceGv.getString("statusId");
                    if(statusId.equals("INVOICE_PAID")) {
                        paidAwards++;
                        paidAwardTotalAmount = paidAwardTotalAmount.add(invoiceTotal);
                    }else{
                        pendingUnpaidAwards++;
                        pendingUnpaidAwardTotalAmount = pendingUnpaidAwardTotalAmount.add(invoiceTotal);
                        pendingUnpaidInvoiceAppendix.put("invoiceId", invoiceGv.getString("invoiceId"));
                        if(UtilValidate.isNotEmpty(invoiceGv.get("dueDate"))) {
                            Timestamp dueDate=(Timestamp)invoiceGv.get("dueDate");
                            int interval = UtilDateTime.getIntervalInDays(dueDate, UtilDateTime.nowTimestamp());
                            if(interval>0) {
                                pendingUnpaidInvoiceAppendix.put("pastDueSinceDays", interval);
                            }
                        }
                        Timestamp invoiceDueDate = (Timestamp) invoiceGv.get("dueDate");
                        if(UtilValidate.isNotEmpty(invoiceDueDate)){
                            pendingUnpaidInvoiceAppendix.put("dueDateTimestamp", invoiceDueDate);
                            pendingUnpaidInvoiceAppendix.put("dueDate", UtilDateTime.toDateString(new java.util.Date(invoiceDueDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        Timestamp invoicedDate = (Timestamp) invoiceGv.get("invoiceDate");
                        if(UtilValidate.isNotEmpty(invoicedDate)){
                            pendingUnpaidInvoiceAppendix.put("invoiceDate", UtilDateTime.toDateString(new java.util.Date(invoicedDate.getTime()), FSD_US_DATE_FORMAT));
                        }
                        BigDecimal totalInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(invoiceGv);
                        pendingUnpaidInvoiceAppendix.put("totalInvoiceAmount", totalInvoiceAmount);
                        paymentPendingRecords.add(pendingUnpaidInvoiceAppendix);
                    }

                }
            }

            List<Map<String,Object>> pendingPaymentListPastDue = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn7Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn15Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn30Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn60Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueIn90Days = FastList.newInstance();
            List<Map<String,Object>> pendingPaymentListDueBeyond90Days = FastList.newInstance();

            for(Map<String, Object> payment: paymentPendingRecords){
                List<String> filterQueryFieldsForPendingPurchase = UtilMisc.toList("docType:quote", awardedQuoteConstraint);
                searchFields.add("quotePurchaseOrderNumber");
                searchFields.add("quoteCustomerName");
                searchFields.add("quoteSupplierPartyName");
                searchFields.add("quoteShippingDestinations");

                String purchaseInvoiceId = (String) payment.get("invoiceId");
                if (UtilValidate.isNotEmpty(purchaseInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + purchaseInvoiceId + "\")";
                    filterQueryFieldsForPendingPurchase.add(quoteSalesInvoiceIdConstraint);
                }

                Map performSolrSearchForPendingPurchaseContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFieldsForPendingPurchase);
                Map performSolrSearchForPendingPurchaseResponse = dispatcher.runSync("performSolrSearch", performSolrSearchForPendingPurchaseContext);

                if (ServiceUtil.isError(performSolrSearchForPendingPurchaseResponse)) {
                    Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchForPendingPurchaseResponse), module);
                }

                List<Map> pendingPurchaseEntries = (List) performSolrSearchForPendingPurchaseResponse.get("records");

                for (Map record : pendingPurchaseEntries) {
                    payment.put("quotePurchaseOrderNumber", record.get("quotePurchaseOrderNumber"));
                    payment.put("quoteCustomerName", record.get("quoteCustomerName"));
                    payment.put("quoteSupplierPartyName", record.get("quoteSupplierPartyName"));
                    List<Map<String, String>> shippingDestinations = FastList.newInstance();
                    List<String> shippingDestinationRecords = AxAccountingHelper.getShipToLocationsForPuchaseInvoice(delegator, purchaseInvoiceId);
                    if (UtilValidate.isNotEmpty(shippingDestinationRecords)) {
                        for (String shippingDestinationRecord : shippingDestinationRecords) {
                            Map shippingDestinationPostalAddress = AxAccountingHelper.splitMergedRecords(shippingDestinationRecord);
                            if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                                shippingDestinations.add(shippingDestinationPostalAddress);
                            }
                        }
                    }
                    payment.put("shippingDestinations", shippingDestinations);

                    Timestamp dueDate = (Timestamp) payment.get("dueDateTimestamp");
                    Timestamp now = UtilDateTime.nowTimestamp();
                    if (UtilValidate.isEmpty(dueDate)) {
                        dueDate = UtilDateTime.addDaysToTimestamp(now, 100); // beyond 90 days temporarily
                    }

                    if (dueDate.before(now)) {
                        pendingPaymentListPastDue.add(payment);
                    } else if (dueDate.before(UtilDateTime.addDaysToTimestamp(now, 7))) {
                        pendingPaymentListDueIn7Days.add(payment);
                    } else if (dueDate.before(UtilDateTime.addDaysToTimestamp(now, 15))) {
                        pendingPaymentListDueIn15Days.add(payment);
                    } else if (dueDate.before(UtilDateTime.addDaysToTimestamp(now, 30))) {
                        pendingPaymentListDueIn30Days.add(payment);
                    } else if (dueDate.before(UtilDateTime.addDaysToTimestamp(now, 60))) {
                        pendingPaymentListDueIn60Days.add(payment);
                    } else if (dueDate.before(UtilDateTime.addDaysToTimestamp(now, 90))) {
                        pendingPaymentListDueIn90Days.add(payment);
                    } else {
                        pendingPaymentListDueBeyond90Days.add(payment);
                    }
                }
            }

            accountPayableSummaryMap.put("pendingPaymentListPastDue", pendingPaymentListPastDue);
            accountPayableSummaryMap.put("pendingPaymentListDueIn7Days", pendingPaymentListDueIn7Days);
            accountPayableSummaryMap.put("pendingPaymentListDueIn15Days", pendingPaymentListDueIn15Days);
            accountPayableSummaryMap.put("pendingPaymentListDueIn30Days", pendingPaymentListDueIn30Days);
            accountPayableSummaryMap.put("pendingPaymentListDueIn60Days", pendingPaymentListDueIn60Days);
            accountPayableSummaryMap.put("pendingPaymentListDueIn90Days", pendingPaymentListDueIn90Days);
            accountPayableSummaryMap.put("pendingPaymentListDueBeyond90Days", pendingPaymentListDueBeyond90Days);

            accountPayableSummaryMap.put("pendingUnpaidAwards", pendingUnpaidAwards);
            accountPayableSummaryMap.put("pendingUnpaidAwardTotalAmount", pendingUnpaidAwardTotalAmount);

            accountPayableSummaryMap.put("paidAwards", paidAwards);
            accountPayableSummaryMap.put("paidAwardTotalAmount", paidAwardTotalAmount);
            accountPayableSummaryMap.put("averageNonBilledInvoiceDays", averageNonBilledInvoiceDays);


        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
        }
        serviceResult.put("accountPayableSummaryMap", accountPayableSummaryMap);
        return serviceResult;
    }

    /**
     * Fetch Invoice Amount Payable Summary details for PURCHASE_INVOICE.
     *
     * */
    public static Map<String, Object> getAccountsPayableSummary(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }

        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");
        String quoteOrderedDateRangeFrom="";
        String quoteOrderedDateRangeTo="";
        String purchaseInvoiceDueDateRangeFrom="";
        String purchaseInvoiceDueDateRangeTo="";
        Map<String, Object> accountPayableSummaryMap = FastMap.newInstance();

        int pendingUnpaidAwards = 0;
        int paidAwards = 0;
        BigDecimal pendingUnpaidAwardTotalAmount = BigDecimal.ZERO;
        BigDecimal paidAwardTotalAmount = BigDecimal.ZERO;


        // WAD-2998: Filter by category of product
        List<String> productsOfCategory = FastList.newInstance();
        String productCategoryId = (String) context.get("productCategoryId");
        String onHold = (String) context.get("onHold");
        List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
        try {
            if (UtilValidate.isNotEmpty(productCategoryId) && (supplierPartyIds.size() == 1)) {
                // filter only if there is one supplier
                if (productCategoryId.equals("Others")) {
                    // collect from ALL categories
                    List<GenericValue> categoriesForProducts = EntityQuery.use(delegator)
                            .from("ProductCategoryMember")
                            .filterByDate()
                            .queryList();
                    for (GenericValue categoryForProduct : categoriesForProducts) {
                        String productId = (String) categoryForProduct.get("productId");
                        productsOfCategory.add(productId);
                    }
                } else {
                    List<GenericValue> categoriesForProducts = EntityQuery.use(delegator)
                            .from("ProductCategoryMember")
                            .where("productCategoryId", productCategoryId)
                            .filterByDate()
                            .queryList();
                    for (GenericValue categoryForProduct : categoriesForProducts) {
                        String productId = (String) categoryForProduct.get("productId");
                        productsOfCategory.add(productId);
                    }
                }
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }


        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteOrderedDate");
            searchFields.add("quotePurchaseInvoiceStatusId");
            searchFields.add("quoteCostTotal");
            searchFields.add("quoteOutStandingTotalAmount");
            searchFields.add("quoteProducts");

            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);


            if (UtilValidate.isNotEmpty(keyword)) {
                if (keyword.contains("-")) keyword = keyword.replaceAll("-", "");
                filterQueryFields.add("_text_:" + keyword);
            } else {
                if(UtilValidate.isEmpty(keyword) && !(showPaidPurchaseInvoice) ) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceStatusId:PMNT_NOT_PAID";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeFrom"))) {
                    Timestamp dateRangeFrom = (Timestamp) context.get("dateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeFrom = isoDateFormat.format(new Date(dateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("dateRangeTo"))) {
                    Timestamp dateRangeTo = (Timestamp) context.get("dateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    quoteOrderedDateRangeTo = isoDateFormat.format(new Date(dateRangeTo.getTime()));
                }

                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeFrom"))) {
                    Timestamp dueDateRangeFrom = (Timestamp) context.get("purchaseInvoiceDueDateRangeFrom");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeFrom = isoDateFormat.format(new Date(dueDateRangeFrom.getTime()));
                }
                if (UtilValidate.isNotEmpty(context.get("purchaseInvoiceDueDateRangeTo"))) {
                    Timestamp dueDateRangeTo = (Timestamp) context.get("purchaseInvoiceDueDateRangeTo");
                    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    purchaseInvoiceDueDateRangeTo = isoDateFormat.format(new Date(dueDateRangeTo.getTime()));
                }

                //customer payment filter
                String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //agency filter
                String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }

                //customer filter
                String customerPartyId = (String) context.get("customerPartyId");
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                String solicitationNumber = (String) context.get("solicitationNumber");
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //supplier filter
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteTagConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                //quote ordered date range filter
                if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom) && UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                    String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO " + quoteOrderedDateRangeTo + "]";
                    filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom)) {
                        String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO *]";
                        filterQueryFields.add(quoteOrderedDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                        String quoteOrderedDateRangeToConstraint = "quoteOrderedDate:[* TO " + quoteOrderedDateRangeTo + "]";
                        filterQueryFields.add(quoteOrderedDateRangeToConstraint);
                    }
                }

                if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom) && UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                    String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO " + purchaseInvoiceDueDateRangeTo + "]";
                    filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                } else {
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom)) {
                        String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO *]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                    }
                    if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                        String purchaseInvoiceDueDateRangeToConstraint = "quotePurchaseInvoiceDueDate:[* TO " + purchaseInvoiceDueDateRangeTo + "]";
                        filterQueryFields.add(purchaseInvoiceDueDateRangeToConstraint);
                    }
                }

                //Quote Id filter
                String quoteId = (String) context.get("quoteId");
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                String quoteOrderNumber = (String) context.get("quoteOrderNumber");
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:" +"\""+ quoteContractReferenceNumber +"\"";
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                String shipmentStatusId = (String) context.get("shipmentStatusId");
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
                if(UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)){
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                String quoteInvoiceId = (String) context.get("quoteInvoiceId");
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                String shippingCity = (String) context.get("shippingCity");
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                String shippingState = (String) context.get("shippingState");
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                List<String> tagIds = (List<String>) context.get("tagIds");
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
                String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
                String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
                String quoteInvoiceStatusTypeConstraint = "";
                if (UtilValidate.isNotEmpty(quoteInvoiceType) && UtilValidate.isNotEmpty(quoteInvoiceStatus)) {
                    if (quoteInvoiceType.equals("SALES_INVOICE")) {
                        quoteInvoiceStatusTypeConstraint = "quotePaymentStatusId:\"" + quoteInvoiceStatus + "\"";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    } else if (quoteInvoiceType.equals("PURCHASE_INVOICE")) {
                        if (quoteInvoiceStatus.equals("PMNT_NOT_PAID"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_IN_PROCESS\" OR  \"purchaseInvoiceStatusId:INVOICE_READY\")";
                        else if (quoteInvoiceStatus.equals("PMNT_RECEIVED"))
                            quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_PAID\" )";
                        filterQueryFields.add(quoteInvoiceStatusTypeConstraint);
                    }
                }

                //Cm Invoice Number filter
                String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String quoteBillingStatusConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId\" " + purchaseCMInvoiceId + " )";
                    filterQueryFields.add(quoteBillingStatusConstraint);
                }

                // onHold filter
                if (UtilValidate.isNotEmpty(onHold)) {
                    if (onHold.equals("ON_HOLD")) {
                        String onHoldFilter = "quoteOnHold:Y";
                        filterQueryFields.add(onHoldFilter);
                    } else if (onHold.equals("NOT_ON_HOLD")) {
                        String onHoldFilter = "quoteOnHold:N";
                        filterQueryFields.add(onHoldFilter);
                    }
                }

                //quoteFlag filter
                String quoteFlagStatus = (String) context.get("quoteFlagStatus");
                String quoteFlagStatusConstraint="";
                if(UtilValidate.isNotEmpty(quoteFlagStatus)){
                    if(quoteFlagStatus.equals("PAY_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "payImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                    if(quoteFlagStatus.equals("BILL_IMMEDIATELY")) {
                        quoteFlagStatusConstraint = "billImmediatelyFlag:" + "Y";
                        filterQueryFields.add(quoteFlagStatusConstraint);
                    }
                }
            }
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            BigDecimal totalPayableAwardsAmount = BigDecimal.ZERO;
            for (Map orderEntry : orderEntries) {
                if (!considerRecord(productsOfCategory, productCategoryId, orderEntry)) {
                    continue;
                }

                if(UtilValidate.isNotEmpty(orderEntry.get("quotePurchaseInvoiceStatusId"))) {
                    Float quoteOutStandingTotalAmount = (Float) orderEntry.get("quoteOutStandingTotalAmount");
                    BigDecimal quoteOutStandingTotalAmountBD = BigDecimal.ZERO;
                    if(null!=quoteOutStandingTotalAmount) {
                        quoteOutStandingTotalAmountBD = new BigDecimal(quoteOutStandingTotalAmount.toString());
                        pendingUnpaidAwardTotalAmount = pendingUnpaidAwardTotalAmount.add(quoteOutStandingTotalAmountBD);
                    }
                    Float quoteCostTotalFloat = (Float) orderEntry.get("quoteCostTotal");
                    BigDecimal quoteCostTotal=BigDecimal.ZERO;
                    if(null!=quoteCostTotalFloat) {
                        quoteCostTotal = new BigDecimal(quoteCostTotalFloat.toString());
                        totalPayableAwardsAmount = totalPayableAwardsAmount.add(quoteCostTotal);
                    }
                    if(orderEntry.get("quotePurchaseInvoiceStatusId").equals("PMNT_PAID")) {
                        paidAwards++;
                    }else if (orderEntry.get("quotePurchaseInvoiceStatusId").equals("PMNT_NOT_PAID")){
                        pendingUnpaidAwards++;
                    }
                }
            }

            paidAwardTotalAmount = totalPayableAwardsAmount.subtract(pendingUnpaidAwardTotalAmount);
            accountPayableSummaryMap.put("pendingUnpaidAwards", pendingUnpaidAwards);
            accountPayableSummaryMap.put("pendingUnpaidAwardTotalAmount", pendingUnpaidAwardTotalAmount);
            accountPayableSummaryMap.put("paidAwards", paidAwards);
            accountPayableSummaryMap.put("paidAwardTotalAmount", paidAwardTotalAmount);

        } catch (GenericServiceException e) {
            e.printStackTrace();
        }
        serviceResult.put("accountPayableSummaryMap", accountPayableSummaryMap);
        return serviceResult;
    }
}
