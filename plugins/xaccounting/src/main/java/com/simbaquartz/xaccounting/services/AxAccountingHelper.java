package com.simbaquartz.xaccounting.services;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.StringUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyHelper;

import org.apache.ofbiz.service.DispatchContext;


/**
 * Accounting related helper utility methods.
 */
public class AxAccountingHelper {
    private static final String module = AxAccountingHelper.class.getName();

    /**
     * Returns the awarded quote id for the given invoice, uses the Quote document indexed in SOLR core to find the
     * linked awarded quote id using quoteSalesInvoiceId field. Tries searching in SOLR first if not falls back to DB calls.
     *
     * @param invoiceId
     * @param isPurchaseInvoice if the input invoice is a purchase invoice.
     * @return Map containing key value pairs of awarded quote related information (e.g. quoteId, purchaseObligationNumber etc.
     */

    public static Map<String, Object> getAwardedQuoteDetailsForInvoice(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String invoiceId = (String) context.get("invoiceId");
        Boolean isPurchaseInvoice = (Boolean) context.get("isPurchaseInvoice");
        Map awardedQuoteDetails = FastMap.newInstance();

        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteId");
            searchFields.add("quoteSalesInvoiceId");
            searchFields.add("quotePurchaseInvoiceIds");
            searchFields.add("quoteOrderNumber");
            searchFields.add("quoteContractReferenceNumber");
            searchFields.add("quotePurchaseOrderNumber");
            searchFields.add("quoteRequisitionPurchaseRequestNumber");
            searchFields.add("quoteSolicitationNumber");
            searchFields.add("quoteOrderNumber");
            searchFields.add("quoteOrderedDate");
            searchFields.add("otherReferenceNumber");
            searchFields.add("quoteShippingDestinations");

            String invoiceIdConstraint = "";

            if (isPurchaseInvoice) {
                invoiceIdConstraint = "quotePurchaseInvoiceIds:*" + invoiceId + "*";
            } else {
                invoiceIdConstraint = "quoteSalesInvoiceId:*" + invoiceId + "*";
            }

            List<String> filterQueryFields = UtilMisc.toList("docType:quote", invoiceIdConstraint);
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> foundRecords = (List) performSolrSearchResponse.get("records");
            if (UtilValidate.isNotEmpty(foundRecords)) {
                awardedQuoteDetails = foundRecords.get(0);

                //fix any date fields to convert them as Timestamp
                Timestamp awardDate = null;
                Date quoteOrderedDate = (Date) awardedQuoteDetails.get("quoteOrderedDate");
                if (UtilValidate.isNotEmpty(quoteOrderedDate)) {
                    awardDate = UtilDateTime.toTimestamp(quoteOrderedDate);
                    awardedQuoteDetails.put("quoteOrderedDate", awardDate);
                }

                if (!isPurchaseInvoice) {
                    //prepare shipping Destinations for sales Invoice
                    List<Map<String, String>> shippingDestinations = FastList.newInstance();

                    List<String> shippingDestinationRecords = (List<String>) awardedQuoteDetails.get("quoteShippingDestinations");
                    if (UtilValidate.isNotEmpty(shippingDestinationRecords)) {
                        for (String shippingDestinationRecord : shippingDestinationRecords) {
                            Map shippingDestinationPostalAddress = splitMergedRecords(shippingDestinationRecord);

                            if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                                shippingDestinations.add(shippingDestinationPostalAddress);
                            }
                        }
                    }
                    awardedQuoteDetails.put("shippingDestinations", shippingDestinations);
                } else {
                    //prepare shipping Destinations for purchase Invoice
                    List<Map<String, String>> shippingDestinations = FastList.newInstance();
                    List<String> shippingDestinationRecords = getShipToLocationsForPuchaseInvoice(delegator, invoiceId);
                    if (UtilValidate.isNotEmpty(shippingDestinationRecords)) {
                        for (String shippingDestinationRecord : shippingDestinationRecords) {
                            Map shippingDestinationPostalAddress = splitMergedRecords(shippingDestinationRecord);

                            if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                                shippingDestinations.add(shippingDestinationPostalAddress);
                            }
                        }
                    }
                    awardedQuoteDetails.put("shippingDestinations", shippingDestinations);
                }

                //prepare invoice ids
                String salesInvoiceIdInfoText = (String) awardedQuoteDetails.get("quoteSalesInvoiceId");
                Map<String, String> salesInvoiceInfo = splitMergedRecords(salesInvoiceIdInfoText);
                if (UtilValidate.isNotEmpty(salesInvoiceInfo)) {
                    awardedQuoteDetails.put("salesInvoiceId", salesInvoiceInfo.get("salesInvoiceId"));
                }

                List<String> purchaseInvoicesInfo = (List<String>) awardedQuoteDetails.get("quotePurchaseInvoiceIds");
                if (UtilValidate.isNotEmpty(purchaseInvoicesInfo)) {
                    awardedQuoteDetails.put("purchaseInvoiceIds", extractPurchaseInvoiceIds(purchaseInvoicesInfo));
                }
            }

            if (UtilValidate.isEmpty(awardedQuoteDetails) && !isPurchaseInvoice) {
                //fallback to DB calls, would work only for Sales orders now as only sales invoice items have order > quote association.
                Debug.logWarning("Awarded quote not found in SOLR for invoice id : " + invoiceId + ", falling back to database calls, solr indexing is recommended for the quote.", module);
                GenericValue orderItem = null;
                try {
                    orderItem = EntityQuery.use(delegator).from("OrderItemBilling").where("invoiceId", invoiceId).queryFirst();

                    String quoteId = "";
                    if (UtilValidate.isNotEmpty(orderItem)) {
                        String orderId = orderItem.getString("orderId");

                        // for sales invoice and order id is sales order id
                        GenericValue orderedQuote = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryFirst();
                        awardedQuoteDetails.put("awardedQuote", orderedQuote.getString("quoteId"));

                        quoteId = orderedQuote.getString("quoteId");

                        if (UtilValidate.isEmpty(quoteId)) {
                            //it is for purchase invoice and order id is purchase order id
                            GenericValue salesOrderId = EntityQuery.use(delegator).from("OrderItemAssoc").where("toOrderId", orderId).queryFirst();

                            if (UtilValidate.isNotEmpty(salesOrderId)) {
                                GenericValue orderedQuoteForPurchaseInvoice = EntityQuery.use(delegator).from("OrderItem").where("orderId", salesOrderId.getString("orderId")).queryFirst();
                                quoteId = orderedQuoteForPurchaseInvoice.getString("quoteId");
                                awardedQuoteDetails.put("quoteId", quoteId);
                            }

                            //fetch meta data
                            GenericValue linkedOrderItem = EntityQuery.use(delegator).from("OrderItem").where("quoteId", quoteId).queryFirst();
                            if (UtilValidate.isNotEmpty(linkedOrderItem)) {
                                GenericValue linkedOrder = linkedOrderItem.getRelatedOne("OrderHeader", false);
                                Timestamp quoteOrderedDate = linkedOrder.getTimestamp("orderDate");
                                if (UtilValidate.isNotEmpty(quoteOrderedDate)) {
                                    awardedQuoteDetails.put("quoteOrderedDate", quoteOrderedDate);
                                }
                                List<GenericValue> orderAttrs = linkedOrder.getRelated("OrderAttribute", null, null, false);
                                for (GenericValue orderAttribute : orderAttrs) {
                                    String attrName = orderAttribute.getString("attrName");
                                    String attrValue = orderAttribute.getString("attrValue");

                                    switch (attrName) {
                                        case "purchaseOrderNumber":
                                            awardedQuoteDetails.put("quotePurchaseOrderNumber", attrValue);
                                            break;
                                        case "requisitionPurchaseRequestNumber":
                                            awardedQuoteDetails.put("quoteRequisitionPurchaseRequestNumber", attrValue);
                                            break;
                                        case "orderNumber":
                                            awardedQuoteDetails.put("quoteOrderNumber", attrValue);
                                            break;
                                        case "contractReferenceNumber":
                                            awardedQuoteDetails.put("quoteContractReferenceNumber", attrValue);
                                            break;
                                    }
                                }
                            }
                        }
                        if (UtilValidate.isNotEmpty(quoteId)) {
                            //get other reference number
                            GenericValue quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).queryOne();
                            if (UtilValidate.isNotEmpty(quote.getString("otherReferenceNumber"))) {
                                awardedQuoteDetails.put("otherReferenceNumber", quote.getString("otherReferenceNumber"));
                            }
                        }

                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "An error occurred while trying to fetch quote meta data using invoice.", module);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "An error occurred while trying to retrieve awarded quote id for an invoice using solr search.", module);
        }

        serviceResult.put("awardedQuoteDetails", awardedQuoteDetails);
        return serviceResult;
    }

    /**
     * Returns a list of map of sales and purchase orders and invoices for given quote.
     *
     * @param quoteId
     * @return Map of awardOrdersAndInvoices.
     */
    public static Map<String, Object> getAwardedQuoteOrdersAndInvoices(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String quoteId = (String) context.get("quoteId");

        try {
            Map awardOrdersAndInvoices = FastMap.newInstance();
            String salesOrderId = "";
            List<String> purchaseOrderIds = FastList.newInstance();

            List<GenericValue> orderItems = EntityQuery.use(delegator)
                    .from("OrderItem")
                    .where(UtilMisc.toMap("quoteId", quoteId))
                    .queryList();
            for (GenericValue orderItem : orderItems) {
                salesOrderId = (String) orderItem.get("orderId");
                break; // looking for 1 order
            }

            // get the corresponding purchase order(s)
            List<GenericValue> orderItemAssocs = EntityQuery.use(delegator)
                    .from("OrderItemAssoc")
                    .where("orderId", salesOrderId)
                    .queryList();
            for (GenericValue orderItemAssoc : orderItemAssocs) {
                String purchaseOrderId = (String) orderItemAssoc.get("toOrderId");
                if (UtilValidate.isNotEmpty(purchaseOrderId)) {
                    if (!purchaseOrderIds.contains(purchaseOrderId)) {
                        purchaseOrderIds.add(purchaseOrderId);
                    }
                }
            }

            awardOrdersAndInvoices.put("salesOrderId", salesOrderId);
            awardOrdersAndInvoices.put("purchaseOrderIds", purchaseOrderIds);

            //invoices
            String salesInvoiceId = "";
            List<String> purchaseInvoiceIds = FastList.newInstance();

            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteId");
            searchFields.add("quoteSalesInvoiceId");
            searchFields.add("quotePurchaseInvoiceIds");

            String quoteIdConstraint = "quoteId:" + quoteId;
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", quoteIdConstraint);
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = null;
            try {
                performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> foundRecords = (List) performSolrSearchResponse.get("records");
            if (UtilValidate.isNotEmpty(foundRecords)) {
                Map awardedQuoteDetails = foundRecords.get(0);

                String salesInvoiceIdInfoText = (String) awardedQuoteDetails.get("quoteSalesInvoiceId");
                if(UtilValidate.isNotEmpty(salesInvoiceIdInfoText)){
                    Map<String, String> salesInvoiceInfo = splitMergedRecords(salesInvoiceIdInfoText);
                    if (UtilValidate.isNotEmpty(salesInvoiceInfo)) {
                        salesInvoiceId = salesInvoiceInfo.get("salesInvoiceId");
                    }

                    List<String> purchaseInvoicesInfo = (List<String>) awardedQuoteDetails.get("quotePurchaseInvoiceIds");
                    purchaseInvoiceIds = extractPurchaseInvoiceIds(purchaseInvoicesInfo);
                }
            }

            awardOrdersAndInvoices.put("salesInvoiceId", salesInvoiceId);
            awardOrdersAndInvoices.put("purchaseInvoiceIds", purchaseInvoiceIds);

            serviceResult.put("awardOrdersAndInvoices", awardOrdersAndInvoices);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    /**
     * Returns a list of map of sales and purchase orders for given quote.
     *
     * @param quoteId
     * @return Map of awardOrders.
     */
    public static Map<String, Object> getAwardedQuoteOrders(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String quoteId = (String) context.get("quoteId");

        try {
            Map awardOrders = FastMap.newInstance();
            String salesOrderId = "";
            List<String> purchaseOrderIds = FastList.newInstance();

            List<GenericValue> orderItems = EntityQuery.use(delegator)
                    .from("OrderItem")
                    .where(UtilMisc.toMap("quoteId", quoteId))
                    .queryList();
            for (GenericValue orderItem : orderItems) {
                salesOrderId = (String) orderItem.get("orderId");
                break; // looking for 1 order
            }

            // get the corresponding purchase order(s)
            List<GenericValue> orderItemAssocs = EntityQuery.use(delegator)
                    .from("OrderItemAssoc")
                    .where("orderId", salesOrderId)
                    .queryList();
            for (GenericValue orderItemAssoc : orderItemAssocs) {
                String purchaseOrderId = (String) orderItemAssoc.get("toOrderId");
                if (UtilValidate.isNotEmpty(purchaseOrderId)) {
                    if (!purchaseOrderIds.contains(purchaseOrderId)) {
                        purchaseOrderIds.add(purchaseOrderId);
                    }
                }
            }

            awardOrders.put("salesOrderId", salesOrderId);
            awardOrders.put("purchaseOrderIds", purchaseOrderIds);
            serviceResult.put("awardOrders", awardOrders);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }


    /**
     * Returns a list of map of sales and purchase invoices for given quote.
     *
     * @param quoteId
     * @return Map of awardInvoices.
     */
    public static Map<String, Object> getAwardedQuoteInvoices(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String quoteId = (String) context.get("quoteId");
        Map awardInvoices = FastMap.newInstance();
        String salesInvoiceId = "";
        List<String> purchaseInvoiceIds = FastList.newInstance();

        List<String> searchFields = FastList.newInstance();
        searchFields.add("quoteId");
        searchFields.add("quoteSalesInvoiceId");
        searchFields.add("quotePurchaseInvoiceIds");

        String quoteIdConstraint = "quoteId:" + quoteId;
        List<String> filterQueryFields = UtilMisc.toList("docType:quote", quoteIdConstraint);
        Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
        Map performSolrSearchResponse = null;
        try {
            performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (ServiceUtil.isError(performSolrSearchResponse)) {
            Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
        }

        List<Map> foundRecords = (List) performSolrSearchResponse.get("records");
        if (UtilValidate.isNotEmpty(foundRecords)) {
            Map awardedQuoteDetails = foundRecords.get(0);

            String salesInvoiceIdInfoText = (String) awardedQuoteDetails.get("quoteSalesInvoiceId");
            if(UtilValidate.isNotEmpty(salesInvoiceIdInfoText)){
                Map<String, String> salesInvoiceInfo = splitMergedRecords(salesInvoiceIdInfoText);
                if (UtilValidate.isNotEmpty(salesInvoiceInfo)) {
                    salesInvoiceId = salesInvoiceInfo.get("salesInvoiceId");
                }

                List<String> purchaseInvoicesInfo = (List<String>) awardedQuoteDetails.get("quotePurchaseInvoiceIds");
                purchaseInvoiceIds = extractPurchaseInvoiceIds(purchaseInvoicesInfo);
            }
        }

        awardInvoices.put("salesInvoiceId", salesInvoiceId);
        awardInvoices.put("purchaseInvoiceIds", purchaseInvoiceIds);

        serviceResult.put("awardInvoices", awardInvoices);
        return serviceResult;
    }

    private static List<String> extractPurchaseInvoiceIds(List<String> purchaseInvoiceInfoTextFromSolr) {
        List<String> purchaseInvoiceIds = FastList.newInstance();

        if (UtilValidate.isNotEmpty(purchaseInvoiceInfoTextFromSolr)) {
            purchaseInvoiceInfoTextFromSolr.forEach(purchaseInvoiceInfo -> {
                Map<String, String> invoiceInfo = splitMergedRecords(purchaseInvoiceInfo);

                String invoiceId = invoiceInfo.get("purchaseInvoiceId");

                if (UtilValidate.isNotEmpty(invoiceId)) {
                    purchaseInvoiceIds.add(invoiceId);
                }
            });
        }

        return purchaseInvoiceIds;
    }

    //TODO: NOTE: duplicating the method here so that we dont have circular dependency between fsdAccounting and fsdSolrConnector
    public static final String SOLR_RECORD_ITEM_DELIMITER = "\\^";
    public static final String SOLR_RECORD_VALUE_DELIMITER = ":";

    public static Map<String, String> splitMergedRecords(String record, String itemDelimiter, String valueDelimiter) {
        Map<String, String> parsedMap = FastMap.newInstance();

        String[] items = record.split(itemDelimiter);

        for (String item : items) {
            String[] itemKeyValue = item.split(valueDelimiter);

            try {
                parsedMap.put(itemKeyValue[0].trim(), itemKeyValue[1].trim());
            } catch (ArrayIndexOutOfBoundsException aiobe) {
                Debug.logWarning("Unable to parse key value pairs for: " + item, module);
            }
        }

        return parsedMap;
    }

    //TODO: NOTE: duplicating the method here so that we dont have circular dependency between fsdOrderManager and fsdSolrConnector

    /**
     * Returns a list of maps parsed from solr indexed multivalued fields.
     *
     * @param record String value of the record, e.g. productId:FRESENIUS-190895^productName:Fresenius 2008T w/o CDX w/bibag
     * @return Map of parsed key value.
     */
    public static Map<String, String> splitMergedRecords(String record) {
        return splitMergedRecords(record, SOLR_RECORD_ITEM_DELIMITER, SOLR_RECORD_VALUE_DELIMITER);
    }


    /**
     * Returns a list of map of shipping destination for given quote.
     *
     * @param quoteId
     * @return Map of shippingDestinations.
     */
    public static Map<String, Object> getAwardedQuoteShippingDestinationsForQuote(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String quoteId = (String) context.get("quoteId");
        List<Map<String, String>> shippingDestinationList = FastList.newInstance();
        StringBuilder sb = new StringBuilder();
        List<String> searchFields = FastList.newInstance();
        searchFields.add("quoteId");
        searchFields.add("quoteShippingDestinations");

        String quoteIdConstraint = "quoteId:" + quoteId;
        List<String> filterQueryFields = UtilMisc.toList("docType:quote", quoteIdConstraint);
        Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
        Map performSolrSearchResponse = null;
        try {
            performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        if (ServiceUtil.isError(performSolrSearchResponse)) {
            Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
        }

        List<Map> foundRecords = (List) performSolrSearchResponse.get("records");
        if (UtilValidate.isNotEmpty(foundRecords)) {
            Map awardedQuoteDetails = foundRecords.get(0);

            List<String> quoteShippingDestinations = (List<String>) awardedQuoteDetails.get("quoteShippingDestinations");
            if (UtilValidate.isNotEmpty(quoteShippingDestinations)) {
                quoteShippingDestinations.forEach(shippingDestination -> {
                    Map<String, String> shippingDestinationInfo = splitMergedRecords(shippingDestination);
                    shippingDestinationList.add(shippingDestinationInfo);
                });
            }
        }
        serviceResult.put("shippingDestinations", shippingDestinationList);
        return serviceResult;
    }

    /**
     * Returns the complete list of shipment destinations for all awarded quotes.
     *
     * @return List of Map containing postal address values.
     */
    public static List<Map<String, String>> getAwardedQuoteShippingDestinations(LocalDispatcher dispatcher) {
        List<Map<String, String>> shippingDestinations = FastList.newInstance();

        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteId");
            searchFields.add("quoteShippingDestinations");

            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";

            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);
            List<String> facetFields = UtilMisc.toList("quoteShippingDestinations");
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields, "facetFields", facetFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");
            if(UtilValidate.isNotEmpty(orderEntries)){
                for (Map orderEntry : orderEntries) {
                    if (UtilValidate.isNotEmpty(orderEntry.get("quoteShippingDestinations"))) {
                        String shippingDestinationRecord = orderEntry.get("quoteShippingDestinations").toString();
                        Map<String, String> shippingDestinationPostalAddress = splitMergedRecords(shippingDestinationRecord);

                        if (UtilValidate.isNotEmpty(shippingDestinationPostalAddress)) {
                            shippingDestinations.add(shippingDestinationPostalAddress);
                        }
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "An error occurred while trying to retrieve awarded quote shipping destinations using solr search.", module);
        }

        return shippingDestinations;
    }


    /**
     * Returns the complete list of shipment destinations for given purchase invoice id.
     *
     * @return List of String containing postal address values.
     */
    public static List<String> getShipToLocationsForPuchaseInvoice(Delegator delegator, String invoiceId) {
        List<String> shippingDestinations = FastList.newInstance();
        try {
            String toName = "";
            String attnName = "";
            String address1 = "";
            String address2 = "";
            String city = "";
            String stateCode = "";
            String countryCode = "";
            String postalCode = "";
            GenericValue purchaseOrderItemBillings = EntityQuery.use(delegator).from("OrderItemBilling").where(UtilMisc.toMap("invoiceId", invoiceId)).queryFirst();
            // get purchase order id
            if (UtilValidate.isNotEmpty(purchaseOrderItemBillings)) {
                String purchaseOrderId = purchaseOrderItemBillings.getString("orderId");
                GenericValue purchaseShipment = EntityQuery.use(delegator).from("Shipment").where("primaryOrderId", purchaseOrderId).queryOne();
                if (UtilValidate.isNotEmpty(purchaseShipment)) {
                    GenericValue shippingContactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", purchaseShipment.getString("destinationContactMechId")).queryOne();//make sure it's contactMechTypeId is POSTAL_ADDRESS
                    if (shippingContactMech.getString("contactMechTypeId").equals("POSTAL_ADDRESS")) {
                        GenericValue shippingPostalAddress = shippingContactMech.getRelatedOne("PostalAddress", false);
                        if (UtilValidate.isNotEmpty(shippingPostalAddress)) {
                            if (UtilValidate.isNotEmpty(shippingPostalAddress.getString("toName"))) {
                                toName = "toName:" + shippingPostalAddress.getString("toName");
                            }
                            if (UtilValidate.isNotEmpty(shippingPostalAddress.getString("attnName"))) {
                                attnName = "attnName:" + shippingPostalAddress.getString("attnName");
                            }
                            if (UtilValidate.isNotEmpty(shippingPostalAddress.getString("city"))) {
                                city = "city:" + shippingPostalAddress.getString("city");
                            }
                            if (UtilValidate.isNotEmpty(shippingPostalAddress.getString("address1"))) {
                                address1 = "address1:" + shippingPostalAddress.getString("address1");
                            }
                            if (UtilValidate.isNotEmpty(shippingPostalAddress.getString("address2"))) {
                                address2 = "address2:" + shippingPostalAddress.getString("address2");
                            }
                            stateCode = "stateProvinceGeoId:" + shippingPostalAddress.getString("stateProvinceGeoId");
                            countryCode = "countryGeoId:" + shippingPostalAddress.getString("countryGeoId");
                            postalCode = "postalCode:" + shippingPostalAddress.getString("postalCode");
                        }
                    }
                }
            }
            List<String> shipmentInfoList = new LinkedList<String>();
            shipmentInfoList.add(toName);
            shipmentInfoList.add(attnName);
            shipmentInfoList.add(address1);
            shipmentInfoList.add(address2);
            shipmentInfoList.add(city);
            shipmentInfoList.add(stateCode);
            shipmentInfoList.add(countryCode);
            shipmentInfoList.add(postalCode);
            String shippingDestinationDetails = StringUtil.join(shipmentInfoList, "^");
            shippingDestinations.add(shippingDestinationDetails);
        } catch (GenericEntityException e) {
            Debug.logError(e, "An error occurred while trying to retrieve awarded quote shipping destinations using solr search.", module);
        }
        return shippingDestinations;
    }

    public static Map<String, Object> getShipmentDetailsForPurchaseInvoice(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String invoiceId = (String) context.get("invoiceId");
        Map shipmentStatusMap = FastMap.newInstance();
        try {
            GenericValue purchaseOrderItemBillings = EntityQuery.use(delegator).from("OrderItemBilling").where(UtilMisc.toMap("invoiceId", invoiceId)).queryFirst();
            // get purchase order id
            if (UtilValidate.isNotEmpty(purchaseOrderItemBillings)) {
                String purchaseOrderId = purchaseOrderItemBillings.getString("orderId");

                GenericValue purchaseShipment = EntityQuery.use(delegator).from("Shipment").where("primaryOrderId", purchaseOrderId).queryOne();
                if (UtilValidate.isNotEmpty(purchaseShipment)) {
                    GenericValue shipmentStatus = EntityQuery.use(delegator).from("StatusItem").where("statusId", purchaseShipment.getString("statusId")).queryOne();
                    shipmentStatusMap.put("shipmentId", purchaseShipment.getString("shipmentId"));
                    shipmentStatusMap.put("shipmentDescription", shipmentStatus.getString("description"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "An error occurred while trying to retrieve shipping details.", module);
        }
        serviceResult.put("shipmentDetails", shipmentStatusMap);
        return serviceResult;
    }

    public static Boolean isExistingPartyBillingAddress(Delegator delegator, String partyId, String address1, String city, String postalCode) {
        Boolean isBillingAddressExists = false;
        try {
            List<GenericValue> partyContactMechs = EntityQuery.use(delegator).from("PartyContactMech").where("partyId", partyId).filterByDate().queryList();
            if(UtilValidate.isNotEmpty(partyContactMechs)){
                for(GenericValue partyContactMech:partyContactMechs){
                    String contactMechId = partyContactMech.getString("contactMechId");
                    GenericValue contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId",contactMechId,"contactMechTypeId", "POSTAL_ADDRESS").queryOne();
                    if(UtilValidate.isNotEmpty(contactMech)){
                        GenericValue postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
                        if(UtilValidate.isNotEmpty(postalAddress)){
                            Boolean isAddress1Same = postalAddress.getString("address1").equalsIgnoreCase(address1);
                            Boolean isCitySame = postalAddress.getString("city").equalsIgnoreCase(city);
                            Boolean isZipCodeSame = false;
                            if(UtilValidate.isNotEmpty(postalAddress.getString("postalCode"))) {
                                isZipCodeSame = postalAddress.getString("postalCode").equals(postalCode);
                            }
                            if(isAddress1Same && isCitySame && isZipCodeSame) {
                                isBillingAddressExists = true;
                            }
                        }
                    }
                }

            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "An error occurred while trying to retrieve contact mechs for party.", module);
        }
        return isBillingAddressExists;
    }

    public static Map <String, Object> getTotalDollarAmountForSupplierAccounts(DispatchContext dctx, Map <String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String quoteOrderedDateRangeFrom = "";
        String quoteOrderedDateRangeTo = "";
        String purchaseInvoiceDueDateRangeFrom = "";
        String purchaseInvoiceDueDateRangeTo = "";
        String sortBy = (String) context.get("sortBy");
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }
        String customerPartyId = (String) context.get("customerPartyId");
        String solicitationNumber = (String) context.get("solicitationNumber");
        String quoteId = (String) context.get("quoteId");
        String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
        String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
        String quoteOrderNumber = (String) context.get("quoteOrderNumber");
        String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
        String shipmentStatusId = (String) context.get("shipmentStatusId");
        List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
        String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
        String quoteInvoiceId = (String) context.get("quoteInvoiceId");
        String shippingState = (String) context.get("shippingState");
        String shippingCity = (String) context.get("shippingCity");
        String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
        List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
        List<String> tagIds = (List<String>) context.get("tagIds");
        String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
        String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
        List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
        String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
        String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
        String quoteInvoiceStatusTypeConstraint = "";
        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");

        List<String> quoteSupplierPartyIdsList = new ArrayList<>();

        String productCategoryId = (String) context.get("productCategoryId");
        String quoteFlagStatus = (String) context.get("quoteFlagStatus");
        String onHold = (String) context.get("onHold");

        // WAD-2998: Filter by category of product
        List<String> productsOfCategory = FastList.newInstance();
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
            searchFields.add("quoteCustomerPartyId");
            searchFields.add("quoteSupplierPartyId");
            searchFields.add("quoteId");
            searchFields.add("quotePurchaseOrderNumber");
            searchFields.add("quotePurchaseInvoiceIds");
            searchFields.add("quoteShippingDestinations");
            searchFields.add("quoteSalesInvoiceId");
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

                //agency filter
                String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }

                //customer payment filter
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //customer filter
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //Quote Id filter
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:" + quoteContractReferenceNumber;
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                if (UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)) {
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
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
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String cmInvoiceIdConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId:" + purchaseCMInvoiceId + "\")";
                    filterQueryFields.add(cmInvoiceIdConstraint);
                }


                //supplier filter
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteSupplierConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteSupplierConstraint);
                }

                //quoteFlag filter
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
            }

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            if (UtilValidate.isNotEmpty(orderEntries)) {
                for (Map orderEntry : orderEntries) {
                    if (!InvoiceReportingServices.considerRecord(productsOfCategory, productCategoryId, orderEntry)) {
                        continue;
                    }
                    if (UtilValidate.isNotEmpty(orderEntry.get("quoteSupplierPartyId"))) {
                        quoteSupplierPartyIdsList.add(orderEntry.get("quoteSupplierPartyId").toString());
                    }
                }

                Set<String> setSupplierParties = new HashSet<String>(quoteSupplierPartyIdsList);
                quoteSupplierPartyIdsList.clear();
                quoteSupplierPartyIdsList.addAll(setSupplierParties);

                searchFields.clear();
                searchFields.add("quoteSupplierPartyId");
                searchFields.add("quoteCostTotal");
                searchFields.add("quoteSubTotal");
                searchFields.add("quoteTotal");
                searchFields.add("quoteOutStandingTotalAmount");
                searchFields.add("quoteSalesInvoiceOutstandingAmount");
                searchFields.add("quoteProducts");


                List<Map<Object, Object>> totalAmountRecieveable = FastList.newInstance();
                List<Map<Object, Object>> totalAmountPayable = FastList.newInstance();

                for (String quoteSupplierPartyId : quoteSupplierPartyIdsList) {
                    BigDecimal totalOutstandingAccountAmount = BigDecimal.ZERO;
                    BigDecimal salesInvoiceOutstandingAmount = BigDecimal.ZERO;
                    BigDecimal supplierTotalPaidAmount = BigDecimal.ZERO;
                    BigDecimal customerTotalPaidAmount = BigDecimal.ZERO;
                    BigDecimal totalCustomerAccountAmount = BigDecimal.ZERO;
                    BigDecimal totalSupplierAccountAmount = BigDecimal.ZERO;
                    List<String> filterSupplierAccountFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint);
                    Map<Object, Object> suppliertotalCost = FastMap.newInstance();
                    Map<Object, Object> customertotalCost = FastMap.newInstance();

                    if (UtilValidate.isNotEmpty(keyword)) {
                        if (keyword.contains("-")) keyword = keyword.replaceAll("-", "");
                        filterSupplierAccountFields.add("_text_:" + keyword);
                    } else {
                        if(UtilValidate.isEmpty(keyword) && !(showPaidPurchaseInvoice) ) {
                            String quoteBillingStatusConstraint = "quotePurchaseInvoiceStatusId:PMNT_NOT_PAID";
                            filterSupplierAccountFields.add(quoteBillingStatusConstraint);
                        }
                        if (UtilValidate.isNotEmpty(quoteSupplierPartyId)) {
                            String supplierPartyRoleConstraint = "quoteSupplierPartyId:" + quoteSupplierPartyId;
                            filterSupplierAccountFields.add(supplierPartyRoleConstraint);
                        }
                        if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom) && UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                            String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO " + quoteOrderedDateRangeTo + "]";
                            filterSupplierAccountFields.add(quoteOrderedDateRangeFromConstraint);
                        } else {
                            if (UtilValidate.isNotEmpty(quoteOrderedDateRangeFrom)) {
                                String quoteOrderedDateRangeFromConstraint = "quoteOrderedDate:[" + quoteOrderedDateRangeFrom + " TO *]";
                                filterSupplierAccountFields.add(quoteOrderedDateRangeFromConstraint);
                            }
                            if (UtilValidate.isNotEmpty(quoteOrderedDateRangeTo)) {
                                String quoteOrderedDateRangeToConstraint = "quoteOrderedDate:[* TO " + quoteOrderedDateRangeTo + "]";
                                filterSupplierAccountFields.add(quoteOrderedDateRangeToConstraint);
                            }
                        }

                        if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom) && UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                            String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO " + purchaseInvoiceDueDateRangeTo + "]";
                            filterSupplierAccountFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                        } else {
                            if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeFrom)) {
                                String purchaseInvoiceDueDateRangeFromConstraint = "quotePurchaseInvoiceDueDate:[" + purchaseInvoiceDueDateRangeFrom + " TO *]";
                                filterSupplierAccountFields.add(purchaseInvoiceDueDateRangeFromConstraint);
                            }
                            if (UtilValidate.isNotEmpty(purchaseInvoiceDueDateRangeTo)) {
                                String purchaseInvoiceDueDateRangeToConstraint = "quotePurchaseInvoiceDueDate:[* TO " + purchaseInvoiceDueDateRangeTo + "]";
                                filterSupplierAccountFields.add(purchaseInvoiceDueDateRangeToConstraint);
                            }
                        }

                        //agency filter
                        String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
                        if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                            String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                            filterSupplierAccountFields.add(quoteBillToCustomerPartyIdConstraint);
                        }

                        //customer payment filter
                        if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                            String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                            filterSupplierAccountFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                        }

                        //billing location filter
                        if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                            String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                            filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                        }

                        //customer filter
                        if (UtilValidate.isNotEmpty(customerPartyId)) {
                            String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                            filterSupplierAccountFields.add(customerPartyRoleConstraint);
                        }

                        //solicitation filter
                        if (UtilValidate.isNotEmpty(solicitationNumber)) {
                            String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                            filterSupplierAccountFields.add(solicitationNumberConstraint);
                        }

                        //Quote Id filter
                        if (UtilValidate.isNotEmpty(quoteId)) {
                            String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                            filterSupplierAccountFields.add(quoteIdConstraint);
                        }

                        //purchase order number filter
                        if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                            String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                            filterSupplierAccountFields.add(quotePurchaseOrderNumberConstraint);
                        }

                        //requisition number filter
                        if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                            String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                            filterSupplierAccountFields.add(requisitionNumberConstraint);
                        }

                        //order number filter
                        if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                            String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                            filterSupplierAccountFields.add(quoteOrderNumberConstraint);
                        }

                        //contract reference number filter
                        if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                            String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:" + quoteContractReferenceNumber;
                            filterSupplierAccountFields.add(quoteContractReferenceNumberConstraint);
                        }

                        //shipment status filter
                        if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                            String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                            String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                            filterSupplierAccountFields.add(shipmentStatusConstraint);
                            filterSupplierAccountFields.add(quoteOrderedShipmentStatusConstraint);
                        }

                        //customer invoice status filter
                        if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                            StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                            customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                                customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                            });
                            String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                            filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                        }

                        //government invoice status filter
                        if (UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)) {
                            String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                            filterSupplierAccountFields.add(govtInvoiceBillingStatusIdConstraint);
                        }

                        //supplier/vendor invoice status filter
                        if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                            StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                            supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                                supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                            });
                            String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                            filterSupplierAccountFields.add(quoteSupplierInvoiceStatusConstraint);
                        }

                        //invoice id filter
                        if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                            String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                            filterSupplierAccountFields.add(quoteSalesInvoiceIdConstraint);
                        }

                        // shipping city filter
                        if (UtilValidate.isNotEmpty(shippingCity)) {
                            String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                            filterSupplierAccountFields.add(shippingCityConstraint);
                        }

                        // shipping state filter
                        if (UtilValidate.isNotEmpty(shippingState)) {
                            String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                            filterSupplierAccountFields.add(shipmentStatusConstraint);
                        }

                        //tags filter
                        if (UtilValidate.isNotEmpty(tagIds)) {
                            StringBuffer tagIdsToFilterBy = new StringBuffer();
                            tagIds.forEach(tagId -> {
                                tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                            });
                            String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                            filterSupplierAccountFields.add(quoteTagConstraint);
                        }

                        // invoice type and status filter
                        if (UtilValidate.isNotEmpty(quoteInvoiceType) && UtilValidate.isNotEmpty(quoteInvoiceStatus)) {
                            if (quoteInvoiceType.equals("SALES_INVOICE")) {
                                quoteInvoiceStatusTypeConstraint = "quotePaymentStatusId:\"" + quoteInvoiceStatus + "\"";
                                filterSupplierAccountFields.add(quoteInvoiceStatusTypeConstraint);
                            } else if (quoteInvoiceType.equals("PURCHASE_INVOICE")) {
                                if (quoteInvoiceStatus.equals("PMNT_NOT_PAID"))
                                    quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_IN_PROCESS\" OR  \"purchaseInvoiceStatusId:INVOICE_READY\")";
                                else if (quoteInvoiceStatus.equals("PMNT_RECEIVED"))
                                    quoteInvoiceStatusTypeConstraint = "quotePurchaseInvoiceIds:IN (\"purchaseInvoiceStatusId:INVOICE_PAID\" )";
                                filterSupplierAccountFields.add(quoteInvoiceStatusTypeConstraint);
                            }
                        }

                        //Cm Invoice Number filter
                        if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                            String cmInvoiceIdConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId:" + purchaseCMInvoiceId + "\")";
                            filterSupplierAccountFields.add(cmInvoiceIdConstraint);
                        }


                        //supplier filter
                        if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                            StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                            supplierPartyIds.forEach(supplierPartyId -> {
                                supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                            });
                            String quoteSupplierConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                            filterSupplierAccountFields.add(quoteSupplierConstraint);
                        }

                        String quoteFlagStatusConstraint="";
                        if(UtilValidate.isNotEmpty(quoteFlagStatus)){
                            if(quoteFlagStatus.equals("PAY_IMMEDIATELY")) {
                                quoteFlagStatusConstraint = "payImmediatelyFlag:" + "Y";
                                filterSupplierAccountFields.add(quoteFlagStatusConstraint);
                            }
                            if(quoteFlagStatus.equals("BILL_IMMEDIATELY")) {
                                quoteFlagStatusConstraint = "billImmediatelyFlag:" + "Y";
                                filterSupplierAccountFields.add(quoteFlagStatusConstraint);
                            }
                        }

                        // onHold filter
                        if (UtilValidate.isNotEmpty(onHold)) {
                            if (onHold.equals("ON_HOLD")) {
                                String onHoldFilter = "quoteOnHold:Y";
                                filterSupplierAccountFields.add(onHoldFilter);
                            } else if (onHold.equals("NOT_ON_HOLD")) {
                                String onHoldFilter = "quoteOnHold:N";
                                filterSupplierAccountFields.add(onHoldFilter);
                            }
                        }
                    }
                    Map performSolrSearchSupplierAccountContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterSupplierAccountFields);
                    Map performSolrSearchSupplierAccountResponse = dispatcher.runSync("performSolrSearch", performSolrSearchSupplierAccountContext);

                    if (ServiceUtil.isError(performSolrSearchSupplierAccountResponse)) {
                        Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchSupplierAccountResponse), module);
                    }

                    List<Map> supplierAccountEntries = (List) performSolrSearchSupplierAccountResponse.get("records");
                    for (Map supplierAccountEntry : supplierAccountEntries) {
                        if (!InvoiceReportingServices.considerRecord(productsOfCategory, productCategoryId, supplierAccountEntry)) {
                            continue;
                        }

                        Float quoteCostTotal = (Float) supplierAccountEntry.get("quoteCostTotal");
                        if (UtilValidate.isNotEmpty(quoteCostTotal)) {
                            BigDecimal costTotal = new BigDecimal(quoteCostTotal.toString());
                            totalSupplierAccountAmount = totalSupplierAccountAmount.add(costTotal);
                        }
                        Float quoteOutStandingTotalAmount = (Float) supplierAccountEntry.get("quoteOutStandingTotalAmount");
                        if (UtilValidate.isNotEmpty(quoteOutStandingTotalAmount)) {
                            BigDecimal outstandingTotal = new BigDecimal(quoteOutStandingTotalAmount.toString());
                            totalOutstandingAccountAmount = totalOutstandingAccountAmount.add(outstandingTotal);
                        }
                        Float quoteSalesInvoiceOutstandingAmount = (Float) supplierAccountEntry.get("quoteSalesInvoiceOutstandingAmount");
                        if (UtilValidate.isNotEmpty(quoteSalesInvoiceOutstandingAmount)) {
                            BigDecimal saleInvoiceOutstandingTotal = new BigDecimal(quoteSalesInvoiceOutstandingAmount.toString());
                            salesInvoiceOutstandingAmount = salesInvoiceOutstandingAmount.add(saleInvoiceOutstandingTotal);
                        }
                        Float quoteSubTotal = (Float) supplierAccountEntry.get("quoteTotal");
                        if (UtilValidate.isNotEmpty(quoteSubTotal)) {
                            BigDecimal subTotal = new BigDecimal(quoteSubTotal.toString());
                            totalCustomerAccountAmount = totalCustomerAccountAmount.add(subTotal);
                        }
                    }
                    supplierTotalPaidAmount = totalSupplierAccountAmount.subtract(totalOutstandingAccountAmount);
                    customerTotalPaidAmount = totalCustomerAccountAmount.subtract(salesInvoiceOutstandingAmount);
                    suppliertotalCost.put("totalDollarAmount", totalSupplierAccountAmount);
                    suppliertotalCost.put("awardsCount", supplierAccountEntries.size());
                    suppliertotalCost.put("quoteSupplierPartyId", quoteSupplierPartyId);
                    suppliertotalCost.put("totalOutstandingSupplierAmount", totalOutstandingAccountAmount);
                    suppliertotalCost.put("supplierTotalPaidAmount", supplierTotalPaidAmount);

                    customertotalCost.put("totalDollarAmount", totalCustomerAccountAmount);
                    customertotalCost.put("awardsCount", supplierAccountEntries.size());
                    customertotalCost.put("quoteSupplierPartyId", quoteSupplierPartyId);
                    customertotalCost.put("salesInvoiceOutstandingAmount", salesInvoiceOutstandingAmount);
                    customertotalCost.put("salesInvoiceTotalPaidAmount", customerTotalPaidAmount);

                    totalAmountPayable.add(suppliertotalCost);
                    totalAmountRecieveable.add(customertotalCost);
                }

                serviceResult.put("amountPayable", totalAmountPayable);
                serviceResult.put("amountReceivable", totalAmountRecieveable);
            }

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }
    
    public static String getFinanceRecordStatus(Delegator delegator, LocalDispatcher dispatcher, String quoteId, String salesInvoiceId, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        String status = "";
        //get sales invoice status for quote id
        Boolean isSalesInvoiceReceived = false;
        Boolean isBillingPaid = false;
        Boolean isPurchaseInvoicePaid = false;
        GenericValue salesInvoiceRecord = EntityQuery.use(delegator).from("Invoice").where("invoiceId", salesInvoiceId).queryOne();
        if(UtilValidate.isNotEmpty(salesInvoiceRecord)){
            String invoiceStatusId = salesInvoiceRecord.getString("statusId");
            if (invoiceStatusId.equals("INVOICE_PAID")) {
                isSalesInvoiceReceived = true;
            }

            //get total invoice amount
            BigDecimal totalInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(salesInvoiceRecord);

            //get billing status for quote id
            BigDecimal totalBilledAmount = BigDecimal.ZERO;
            List<GenericValue> billingDetails = EntityQuery.use(delegator).from("InvoiceBilling").where("invoiceId", salesInvoiceId).queryList();
            if(UtilValidate.isNotEmpty(billingDetails)){
                for(GenericValue billingDetail:billingDetails){
                    totalBilledAmount = totalBilledAmount.add((BigDecimal)billingDetail.get("billedAmount"));
                }
                if(totalBilledAmount.compareTo(totalInvoiceAmount) == 0){
                    isBillingPaid = true;
                }
            }
        }

        //get purchase invoice status
        //get shipping destinations for quote
        Map<String, Object> getShippingAddressesResponse = dispatcher.runSync("getAwardedQuoteShippingDestinationsForQuote", UtilMisc.toMap("userLogin", userLogin, "quoteId", quoteId));
        if(ServiceUtil.isError(getShippingAddressesResponse)){
            Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(getShippingAddressesResponse), module);
        }
        List<Map<String, Object>> quoteShippingDestinations = (List<Map<String, Object>>) getShippingAddressesResponse.get("shippingDestinations");
        if(UtilValidate.isNotEmpty(quoteShippingDestinations)) {
            for (Map<String, Object> record : quoteShippingDestinations) {
                String purchaseInvoiceId = (String) record.get("purchaseInvoiceId");
                GenericValue purchaseInvoiceRecord = EntityQuery.use(delegator).from("Invoice").where("invoiceId", purchaseInvoiceId).queryOne();
                if (UtilValidate.isNotEmpty(purchaseInvoiceRecord)) {
                    String invoiceStatusId = purchaseInvoiceRecord.getString("statusId");
                    if (invoiceStatusId.equals("INVOICE_PAID")) {
                        isPurchaseInvoicePaid = true;
                    } else {
                        isPurchaseInvoicePaid = false;
                        break;
                    }
                }
            }
        }

        if(isSalesInvoiceReceived && isBillingPaid && isPurchaseInvoicePaid){
            status = "completed";
        }else if(isSalesInvoiceReceived || isBillingPaid || isPurchaseInvoicePaid){
            status = "progress";
        }else{
            status = "not started";
        }
        return status;
    }

    public static Map <String, Object> getSupplierInvoiceAmountForFinanceReport(DispatchContext dctx, Map <String, Object> context) {
        Map <String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp dateRangeFrom = (Timestamp) context.get("dateRangeFrom");
        Timestamp dateRangeTo = (Timestamp) context.get("dateRangeTo");
        Timestamp purchaseInvoiceDueDateRangeFrom = (Timestamp) context.get("purchaseInvoiceDueDateRangeFrom");
        Timestamp purchaseInvoiceDueDateRangeTo = (Timestamp) context.get("purchaseInvoiceDueDateRangeTo");
        String sortBy = (String) context.get("sortBy");
        String keyword = (String) context.get("keyword");
        String customerPartyId = (String) context.get("customerPartyId");
        String solicitationNumber = (String) context.get("solicitationNumber");
        String quoteId = (String) context.get("quoteId");
        String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
        String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
        String quoteOrderNumber = (String) context.get("quoteOrderNumber");
        String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
        String shipmentStatusId = (String) context.get("shipmentStatusId");
        List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
        String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
        String quoteInvoiceId = (String) context.get("quoteInvoiceId");
        String shippingState = (String) context.get("shippingState");
        String shippingCity = (String) context.get("shippingCity");
        String quoteInvoiceTypeId = (String) context.get("quoteInvoiceTypeId");
        String quoteInvoiceStatusId = (String) context.get("quoteInvoiceStatusId");
        String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
        List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
        String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
        String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
        String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
        String quoteFlagStatus = (String) context.get("quoteFlagStatus");
        List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
        List<String> tagIds = (List<String>) context.get("tagIds");
        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");

        String productCategoryId = (String) context.get("productCategoryId");
        String onHold = (String) context.get("onHold");


        List<Map<Object, Object>> invoiceAmount = FastList.newInstance();
        try {
            Map<String, Object> getTotalDollarAmountForSupplierAccountsResp = dispatcher.runSync("getTotalDollarAmountForSupplierAccounts",
                    UtilMisc.toMap("userLogin", userLogin, "dateRangeFrom", dateRangeFrom,
                            "dateRangeTo", dateRangeTo,
                            "purchaseInvoiceDueDateRangeFrom", purchaseInvoiceDueDateRangeFrom, "purchaseInvoiceDueDateRangeTo", purchaseInvoiceDueDateRangeTo,
                            "sortBy", sortBy,
                            "supplierPartyIds", supplierPartyIds,
                            "customerPartyId", customerPartyId,
                            "solicitationNumber", solicitationNumber,
                            "quoteId", quoteId,
                            "keyword", keyword,
                            "quotePurchaseOrderNumber", quotePurchaseOrderNumber,
                            "quoteRequisitionPurchaseRequestNumber", quoteRequisitionPurchaseRequestNumber,
                            "quoteOrderNumber", quoteOrderNumber,
                            "quoteContractReferenceNumber", quoteContractReferenceNumber,
                            "shipmentStatusId", shipmentStatusId,
                            "customerInvoiceStatusIds", customerInvoiceStatusIds,
                            "govtInvoiceBillingStatusId", govtInvoiceBillingStatusId,
                            "quoteInvoiceId", quoteInvoiceId,
                            "shippingState", shippingState,
                            "shippingCity", shippingCity,
                            "quoteInvoiceTypeId", quoteInvoiceTypeId,
                            "quoteInvoiceStatusId", quoteInvoiceStatusId,
                            "purchaseCMInvoiceId", purchaseCMInvoiceId,
                            "supplierInvoiceStatusIds", supplierInvoiceStatusIds,
                            "showPaidPurchaseInvoice", showPaidPurchaseInvoice,
                            "quoteBillToCustomerPartyId", quoteBillToCustomerPartyId,
                            "quoteSalesInvoicePaymentModeId", quoteSalesInvoicePaymentModeId,
                            "quoteBillingLocPartyId", quoteBillingLocPartyId,
                            "tagIds", tagIds,
                            "productCategoryId", productCategoryId,
                            "onHold", onHold,
                            "quoteFlagStatus", quoteFlagStatus
                    ));
            if(ServiceUtil.isError(getTotalDollarAmountForSupplierAccountsResp)){
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(getTotalDollarAmountForSupplierAccountsResp), module);
            }
            List <Map <Object, Object>> amountPayable = (List <Map <Object, Object>>) getTotalDollarAmountForSupplierAccountsResp.get("amountPayable");
            List <Map <Object, Object>> amountReceivable = (List <Map <Object, Object>>) getTotalDollarAmountForSupplierAccountsResp.get("amountReceivable");
            int count = 0;
            if(UtilValidate.isNotEmpty(amountPayable)) {
                for (Map<Object, Object> payable : amountPayable) {
                    if (null != amountReceivable.get(count)) {
                        payable.put("receivableAmount", amountReceivable.get(count).get("totalDollarAmount"));
                        payable.put("salesInvoiceOutstandingAmount", amountReceivable.get(count).get("salesInvoiceOutstandingAmount"));
                        payable.put("salesInvoiceTotalPaidAmount", amountReceivable.get(count).get("salesInvoiceTotalPaidAmount"));
                        invoiceAmount.add(payable);
                        count++;
                    }
                }
            }
            if ("amountPayable".equalsIgnoreCase(sortBy)) {
                invoiceAmount = UtilMisc.sortMaps(invoiceAmount, UtilMisc.toList("totalDollarAmount"));
            } else if ("-amountPayable".equalsIgnoreCase(sortBy)) {
                invoiceAmount = UtilMisc.sortMaps(invoiceAmount, UtilMisc.toList("-totalDollarAmount"));
            }
            serviceResult.put("invoiceAmount", invoiceAmount);
        } catch (Exception ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return serviceResult;
    }

    /**
     * Returns the Quote Id based on the Invoice Id.
     */
    public static Map<String, Object> getAwardDetailsFromInvoiceId(LocalDispatcher dispatcher,String invoiceId,String invoiceType) {

        Map <java.lang.String, java.lang.Object> serviceResult = ServiceUtil.returnSuccess();
        List<String> filterQueryFields=FastList.newInstance();
        try {
            List<String> searchFields = FastList.newInstance();
            searchFields.add("quoteId");
            searchFields.add("quotePurchaseInvoiceIds");
            searchFields.add("quoteSalesInvoiceId");
            searchFields.add("quoteCustomerName");
            searchFields.add("quoteSupplierPartyName");
            searchFields.add("quoteOrderedDate");
            searchFields.add("quoteSubTotal");
            searchFields.add("quotePurchaseOrderNumber");

            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            if("Purchase Invoice".equalsIgnoreCase(invoiceType)) {
                String purchaseInvoiceIdConstraint = "quotePurchaseInvoiceIds:IN (\"".concat("purchaseInvoiceId").concat(":").concat(invoiceId) + "\")";
                filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint, purchaseInvoiceIdConstraint);
            }
            if("Sales Invoice".equalsIgnoreCase(invoiceType)) {
                String salesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"".concat("salesInvoiceId").concat(":").concat(invoiceId) + "\")";
                filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint, salesInvoiceIdConstraint);
            }

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> foundRecords = (List) performSolrSearchResponse.get("records");
            if (UtilValidate.isNotEmpty(foundRecords)) {
                Map awardedQuoteDetails = foundRecords.get(0);
               if(null!=awardedQuoteDetails) {
                   serviceResult.put("quoteId",awardedQuoteDetails.get("quoteId"));
                   serviceResult.put("quoteCustomerName",awardedQuoteDetails.get("quoteCustomerName"));
                   serviceResult.put("quoteSupplierPartyName",awardedQuoteDetails.get("quoteSupplierPartyName"));
                   serviceResult.put("quoteOrderedDate",awardedQuoteDetails.get("quoteOrderedDate"));
                   serviceResult.put("quoteSubTotal",awardedQuoteDetails.get("quoteSubTotal"));
                   serviceResult.put("quotePurchaseOrderNumber",awardedQuoteDetails.get("quotePurchaseOrderNumber"));
               }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "An error occurred while trying to retrieve awarded quote from Invoice Id using solr search.", module);
        }

        return serviceResult;
    }

    public static Map<String, Object> getAgeingReport(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String keyword = (String) context.get("keyword");
        if (UtilValidate.isNotEmpty(keyword)) {
            keyword = keyword.trim(); // remove unnecessary spaces
            keyword = keyword.replaceAll("[-+.^:,()<>]","");
        }
        String customerPartyId = (String) context.get("customerPartyId");
        String solicitationNumber = (String) context.get("solicitationNumber");
        String quoteId = (String) context.get("quoteId");
        String quotePurchaseOrderNumber = (String) context.get("quotePurchaseOrderNumber");
        String quoteRequisitionPurchaseRequestNumber = (String) context.get("quoteRequisitionPurchaseRequestNumber");
        String quoteOrderNumber = (String) context.get("quoteOrderNumber");
        String quoteContractReferenceNumber = (String) context.get("quoteContractReferenceNumber");
        String shipmentStatusId = (String) context.get("shipmentStatusId");
        List<String> customerInvoiceStatusIds = (List<String>) context.get("customerInvoiceStatusIds");
        String govtInvoiceBillingStatusId = (String) context.get("govtInvoiceBillingStatusId");
        String quoteInvoiceId = (String) context.get("quoteInvoiceId");
        String shippingState = (String) context.get("shippingState");
        String shippingCity = (String) context.get("shippingCity");
        String purchaseCMInvoiceId = (String) context.get("purchaseCMInvoiceId");
        List<String> supplierPartyIds = (List<String>) context.get("supplierPartyIds");
        List<String> tagIds = (List<String>) context.get("tagIds");
        String quoteInvoiceType = (String) context.get("quoteInvoiceTypeId");
        String quoteInvoiceStatus = (String) context.get("quoteInvoiceStatusId");
        List<String> supplierInvoiceStatusIds = (List<String>) context.get("supplierInvoiceStatusIds");
        String quoteBillToCustomerPartyId = (String) context.get("quoteBillToCustomerPartyId");
        String quoteSalesInvoicePaymentModeId = (String) context.get("quoteSalesInvoicePaymentModeId");
        String quoteBillingLocPartyId = (String) context.get("quoteBillingLocPartyId");
        String quoteFlagStatus = (String) context.get("quoteFlagStatus");
        String quoteInvoiceStatusTypeConstraint = "";
        Boolean showPaidPurchaseInvoice = (Boolean) context.get("showPaidPurchaseInvoice");
        String quoteOrderedDateRangeFrom = "";
        String quoteOrderedDateRangeTo = "";
        String purchaseInvoiceDueDateRangeFrom = "";
        String purchaseInvoiceDueDateRangeTo = "";

        // WAD-2998
        String productCategoryId = (String) context.get("productCategoryId");
        String onHold = (String) context.get("onHold");

        List<String> quoteSupplierPartyIdsList = new ArrayList<>();

        Timestamp now = UtilDateTime.nowTimestamp();
        Timestamp now7 = UtilDateTime.addDaysToTimestamp(now, 7);
        Timestamp now15 = UtilDateTime.addDaysToTimestamp(now, 15);
        Timestamp now30 = UtilDateTime.addDaysToTimestamp(now, 30);
        Timestamp now60 = UtilDateTime.addDaysToTimestamp(now, 60);
        Timestamp now90 = UtilDateTime.addDaysToTimestamp(now, 90);


        // WAD-2998: Filter by category of product
        List<String> productsOfCategory = FastList.newInstance();
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
            searchFields.add("quotePurchaseInvoiceDueDate");
            searchFields.add("quoteSupplierPartyId");
            searchFields.add("quotePurchaseInvoiceIds");
            searchFields.add("quoteProducts");
            String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED";
            String paymentNotPaidConstraint = "quotePurchaseInvoiceStatusId:PMNT_NOT_PAID";
            List<String> filterQueryFields = UtilMisc.toList("docType:quote", awardedQuoteConstraint, paymentNotPaidConstraint);

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

                //agency filter
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    String quoteBillToCustomerPartyIdConstraint = "quoteBillToCustomerPartyId:" + quoteBillToCustomerPartyId;
                    filterQueryFields.add(quoteBillToCustomerPartyIdConstraint);
                }

                //customer payment filter
                if (UtilValidate.isNotEmpty(quoteSalesInvoicePaymentModeId)) {
                    String quoteSalesInvoicePaymentModeIdConstraint = "quoteSalesInvoicePaymentModeId:" + quoteSalesInvoicePaymentModeId;
                    filterQueryFields.add(quoteSalesInvoicePaymentModeIdConstraint);
                }

                //billing location filter
                if (UtilValidate.isNotEmpty(quoteBillingLocPartyId)) {
                    String quoteBillingLocPartyIdConstraint = "quoteBillingPartyIds:IN(\"billingPartyId:"+quoteBillingLocPartyId+"\")";
                    filterQueryFields.add(quoteBillingLocPartyIdConstraint);
                }

                //customer filter
                if (UtilValidate.isNotEmpty(customerPartyId)) {
                    String customerPartyRoleConstraint = "quoteCustomerPartyId:" + customerPartyId;
                    filterQueryFields.add(customerPartyRoleConstraint);
                }

                //solicitation filter
                if (UtilValidate.isNotEmpty(solicitationNumber)) {
                    String solicitationNumberConstraint = "quoteSolicitationNumber:" + solicitationNumber;
                    filterQueryFields.add(solicitationNumberConstraint);
                }

                //Quote Id filter
                if (UtilValidate.isNotEmpty(quoteId)) {
                    String quoteIdConstraint = "quoteId_ci:*" + quoteId + "*"; // Use Case-Insensitive field of quoteId
                    filterQueryFields.add(quoteIdConstraint);
                }

                //purchase order number filter
                if (UtilValidate.isNotEmpty(quotePurchaseOrderNumber)) {
                    String quotePurchaseOrderNumberConstraint = "quotePurchaseOrderNumber:" + quotePurchaseOrderNumber;
                    filterQueryFields.add(quotePurchaseOrderNumberConstraint);
                }

                //requisition number filter
                if (UtilValidate.isNotEmpty(quoteRequisitionPurchaseRequestNumber)) {
                    String requisitionNumberConstraint = "quoteRequisitionPurchaseRequestNumber:" + quoteRequisitionPurchaseRequestNumber;
                    filterQueryFields.add(requisitionNumberConstraint);
                }

                //order number filter
                if (UtilValidate.isNotEmpty(quoteOrderNumber)) {
                    String quoteOrderNumberConstraint = "quoteOrderNumber:" + quoteOrderNumber;
                    filterQueryFields.add(quoteOrderNumberConstraint);
                }

                //contract reference number filter
                if (UtilValidate.isNotEmpty(quoteContractReferenceNumber)) {
                    String quoteContractReferenceNumberConstraint = "quoteContractReferenceNumber:" + quoteContractReferenceNumber;
                    filterQueryFields.add(quoteContractReferenceNumberConstraint);
                }

                //shipment status filter
                if (UtilValidate.isNotEmpty(shipmentStatusId)) {
                    String quoteOrderedShipmentStatusConstraint = "quoteStatusId:(IN " + "QUO_ORDERED" + " )";
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("shipmentStatus").concat(":").concat(shipmentStatusId) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                    filterQueryFields.add(quoteOrderedShipmentStatusConstraint);
                }

                //customer invoice status filter
                if(UtilValidate.isNotEmpty(customerInvoiceStatusIds)){
                    StringBuffer customerInvoiceStatusIdsToFilterBy = new StringBuffer();
                    customerInvoiceStatusIds.forEach(customerInvoiceStatusId -> {
                        customerInvoiceStatusIdsToFilterBy.append("\"" + customerInvoiceStatusId + "\" ");
                    });
                    String quoteCustomerInvoiceStatusConstraint = "quoteBillingStatus:IN ( "+customerInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteCustomerInvoiceStatusConstraint);
                }

                //government invoice status filter
                if (UtilValidate.isNotEmpty(govtInvoiceBillingStatusId)) {
                    String govtInvoiceBillingStatusIdConstraint = "quoteGovtInvoiceBillingStatus:" + govtInvoiceBillingStatusId;
                    filterQueryFields.add(govtInvoiceBillingStatusIdConstraint);
                }

                //supplier/vendor invoice status filter
                if(UtilValidate.isNotEmpty(supplierInvoiceStatusIds)){
                    StringBuffer supplierInvoiceStatusIdsToFilterBy = new StringBuffer();
                    supplierInvoiceStatusIds.forEach(supplierInvoiceStatusId -> {
                        supplierInvoiceStatusIdsToFilterBy.append("\"" + supplierInvoiceStatusId + "\" ");
                    });
                    String quoteSupplierInvoiceStatusConstraint = "quoteShippingStatus:IN ( "+supplierInvoiceStatusIdsToFilterBy+")";
                    filterQueryFields.add(quoteSupplierInvoiceStatusConstraint);
                }

                //invoice id filter
                if (UtilValidate.isNotEmpty(quoteInvoiceId)) {
                    String quoteSalesInvoiceIdConstraint = "quoteSalesInvoiceId:IN (\"salesInvoiceId:" + quoteInvoiceId + "\") OR quotePurchaseInvoiceIds:IN(\"purchaseInvoiceId:" + quoteInvoiceId + "\")";
                    filterQueryFields.add(quoteSalesInvoiceIdConstraint);
                }

                // shipping city filter
                if (UtilValidate.isNotEmpty(shippingCity)) {
                    String shippingCityConstraint = "quoteCustomerCity:" + shippingCity;
                    filterQueryFields.add(shippingCityConstraint);
                }

                // shipping state filter
                if (UtilValidate.isNotEmpty(shippingState)) {
                    String shipmentStatusConstraint = "quoteShippingDestinations:IN (\"".concat("stateProvinceGeoId").concat(":").concat(shippingState) + "\")";
                    filterQueryFields.add(shipmentStatusConstraint);
                }

                //tags filter
                if (UtilValidate.isNotEmpty(tagIds)) {
                    StringBuffer tagIdsToFilterBy = new StringBuffer();
                    tagIds.forEach(tagId -> {
                        tagIdsToFilterBy.append("\"" + "tagId:" + tagId + "\" ");
                    });
                    String quoteTagConstraint = "quoteTags:IN ( " + tagIdsToFilterBy + ")";
                    filterQueryFields.add(quoteTagConstraint);
                }

                // invoice type and status filter
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
                if (UtilValidate.isNotEmpty(purchaseCMInvoiceId)) {
                    String cmInvoiceIdConstraint = "quotePurchaseInvoiceIds:IN(\"purchaseCMInvoiceId:" + purchaseCMInvoiceId + "\")";
                    filterQueryFields.add(cmInvoiceIdConstraint);
                }


                //supplier filter
                if (UtilValidate.isNotEmpty(supplierPartyIds)) {
                    StringBuffer supplierPartyIdsToFilterBy = new StringBuffer();
                    supplierPartyIds.forEach(supplierPartyId -> {
                        supplierPartyIdsToFilterBy.append("\"" + supplierPartyId + "\" ");
                    });
                    String quoteSupplierConstraint = "quoteSupplierPartyId:IN ( " + supplierPartyIdsToFilterBy + ")";
                    filterQueryFields.add(quoteSupplierConstraint);
                }

                //quoteFlag filter
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
            }


            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (ServiceUtil.isError(performSolrSearchResponse)) {
                Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(performSolrSearchResponse), module);
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            List resultList = FastList.newInstance();

            Map<String, BigDecimal> resultMapPastDue = FastMap.newInstance();
            Map<String, BigDecimal> resultMap7 = FastMap.newInstance();
            Map<String, BigDecimal> resultMap15 = FastMap.newInstance();
            Map<String, BigDecimal> resultMap30 = FastMap.newInstance();
            Map<String, BigDecimal> resultMap60 = FastMap.newInstance();
            Map<String, BigDecimal> resultMap90 = FastMap.newInstance();
            Map<String, BigDecimal> resultMap90Plus = FastMap.newInstance();

            String purchaseInvoiceOutstandingString;
            String purchaseInvoiceDueDateString;
            Timestamp purchaseInvoiceDueDate;
            BigDecimal purchaseInvoiceOutstanding;

            String quoteSupplierPartyId;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            if (UtilValidate.isNotEmpty(orderEntries)) {
                for (Map orderEntry : orderEntries) {
                    if (!InvoiceReportingServices.considerRecord(productsOfCategory, productCategoryId, orderEntry)) {
                        continue;
                    }

                    quoteSupplierPartyId = (String) orderEntry.get("quoteSupplierPartyId");
                    if (UtilValidate.isEmpty(quoteSupplierPartyId)) {
                        continue;
                    }

                    // process for each invoice associated with the quote.
                    List<String> quotePurchaseInvoiceIds = (List) orderEntry.get("quotePurchaseInvoiceIds");

                    for (Object quotePurchaseInvoiceId : quotePurchaseInvoiceIds) {
                        Map purchaseInvoiceDetails = splitMergedRecords((String) quotePurchaseInvoiceId);
                        purchaseInvoiceOutstandingString = (String) purchaseInvoiceDetails.get("purchaseInvoiceOutstanding");
                        purchaseInvoiceDueDateString = (String) purchaseInvoiceDetails.get("purchaseInvoiceDueDate");

                        // replace ; with : to get correct timestamp
                        purchaseInvoiceDueDateString = purchaseInvoiceDueDateString.replace("=", ":");

                        purchaseInvoiceDueDate = UtilDateTime.toTimestamp(sdf.parse(purchaseInvoiceDueDateString));

                        purchaseInvoiceOutstanding = new BigDecimal(purchaseInvoiceOutstandingString);


                        if (!resultMapPastDue.containsKey(quoteSupplierPartyId)) {
                            resultMapPastDue.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap7.containsKey(quoteSupplierPartyId)) {
                            resultMap7.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap15.containsKey(quoteSupplierPartyId)) {
                            resultMap15.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap30.containsKey(quoteSupplierPartyId)) {
                            resultMap30.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap60.containsKey(quoteSupplierPartyId)) {
                            resultMap60.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap90.containsKey(quoteSupplierPartyId)) {
                            resultMap90.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }
                        if (!resultMap90Plus.containsKey(quoteSupplierPartyId)) {
                            resultMap90Plus.put(quoteSupplierPartyId, BigDecimal.ZERO);
                        }

                        if (purchaseInvoiceDueDate.before(now)) {
                            resultMapPastDue.put(quoteSupplierPartyId, (BigDecimal) resultMapPastDue.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else if (purchaseInvoiceDueDate.before(now7)) {
                            resultMap7.put(quoteSupplierPartyId, (BigDecimal) resultMap7.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else if (purchaseInvoiceDueDate.before(now15)) {
                            resultMap15.put(quoteSupplierPartyId, (BigDecimal) resultMap15.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else if (purchaseInvoiceDueDate.before(now30)) {
                            resultMap30.put(quoteSupplierPartyId, (BigDecimal) resultMap30.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else if (purchaseInvoiceDueDate.before(now60)) {
                            resultMap60.put(quoteSupplierPartyId, (BigDecimal) resultMap60.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else if (purchaseInvoiceDueDate.before(now90)) {
                            resultMap90.put(quoteSupplierPartyId, (BigDecimal) resultMap90.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        } else {
                            resultMap90Plus.put(quoteSupplierPartyId, (BigDecimal) resultMap90Plus.get(quoteSupplierPartyId).add(purchaseInvoiceOutstanding));
                        }

                    }
                }

                BigDecimal totalPastDue = BigDecimal.ZERO;
                BigDecimal totalSevenDays = BigDecimal.ZERO;
                BigDecimal totalFifteenDays = BigDecimal.ZERO;
                BigDecimal totalThirtyDays = BigDecimal.ZERO;
                BigDecimal totalSixtyDays = BigDecimal.ZERO;
                BigDecimal totalNinetyDays = BigDecimal.ZERO;
                BigDecimal totalBeyondNinetyDays = BigDecimal.ZERO;
                BigDecimal totalSupplierDue = BigDecimal.ZERO;
                for (String supplierPartyId : resultMap7.keySet()) { // key are same for all the maps
                    Map resultMap = FastMap.newInstance();
                    BigDecimal totalDue = resultMapPastDue.get(supplierPartyId).add(resultMap7.get(supplierPartyId))
                           .add(resultMap15.get(supplierPartyId)).add(resultMap30.get(supplierPartyId))
                           .add(resultMap60.get(supplierPartyId)).add(resultMap90.get(supplierPartyId))
                            .add(resultMap90Plus.get(supplierPartyId));
                    totalPastDue = totalPastDue.add(resultMapPastDue.get(supplierPartyId));
                    totalSevenDays = totalSevenDays.add(resultMap7.get(supplierPartyId));
                    totalFifteenDays = totalFifteenDays.add(resultMap15.get(supplierPartyId));
                    totalThirtyDays = totalThirtyDays.add(resultMap30.get(supplierPartyId));
                    totalSixtyDays = totalSixtyDays.add(resultMap60.get(supplierPartyId));
                    totalNinetyDays = totalNinetyDays.add(resultMap90.get(supplierPartyId));
                    totalBeyondNinetyDays = totalBeyondNinetyDays.add(resultMap90Plus.get(supplierPartyId));
                    totalSupplierDue = totalSupplierDue.add(totalDue);

                    resultMap.put("supplierPartyId", supplierPartyId);
                    resultMap.put("pastDue", resultMapPastDue.get(supplierPartyId));
                    resultMap.put("sevenDays", resultMap7.get(supplierPartyId));
                    resultMap.put("fifteenDays", resultMap15.get(supplierPartyId));
                    resultMap.put("thirtyDays", resultMap30.get(supplierPartyId));
                    resultMap.put("sixtyDays", resultMap60.get(supplierPartyId));
                    resultMap.put("ninetyDays", resultMap90.get(supplierPartyId));
                    resultMap.put("beyondNinetyDays", resultMap90Plus.get(supplierPartyId));
                    resultMap.put("totalDue", totalDue);

                    resultList.add(resultMap);
                }
                serviceResult.put("totalSupplierDue", totalSupplierDue);
                serviceResult.put("totalPastDue", totalPastDue);
                serviceResult.put("totalSevenDays", totalSevenDays);
                serviceResult.put("totalFifteenDays", totalFifteenDays);
                serviceResult.put("totalThirtyDays", totalThirtyDays);
                serviceResult.put("totalSixtyDays", totalSixtyDays);
                serviceResult.put("totalNinetyDays", totalNinetyDays);
                serviceResult.put("totalBeyondNinetyDays", totalBeyondNinetyDays);
            }

            resultList = UtilMisc.sortMaps(resultList, UtilMisc.toList("-totalDue"));
            serviceResult.put("report", resultList);
        } catch(Exception e)
        {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    /**
     * Returns whether customer account is penetrated or not for given quote Id.
     */
    public static Boolean isCustomerPenetrated(LocalDispatcher dispatcher, Delegator delegator, String quoteId, String customerPartyId) throws GenericEntityException {
        Boolean isPenetrated = false; // default

        // get the quote products by searching in solr
        List<String> searchFields = FastList.newInstance();
        searchFields.add("quoteProducts");
        List<String> filterQueryFields = UtilMisc.toList("docType:quote");
        String quoteIdConstraint = "quoteId:" + quoteId;
        filterQueryFields.add(quoteIdConstraint);
        String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED"; // used later
        String customerConstraint = "quoteCustomerPartyId:" + customerPartyId; // used later

        try {
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (!ServiceUtil.isSuccess(performSolrSearchResponse)) {
                Debug.logError("performSolrSearch failed!", module);
                return isPenetrated;
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            if (UtilValidate.isNotEmpty(orderEntries)) {
                Map orderEntry = orderEntries.get(0); // there will be only one quote!
                List<String> quoteProducts = (List<String>) orderEntry.get("quoteProducts");
                List<Map> orderedQuotes;
                if (UtilValidate.isNotEmpty(quoteProducts)) {
                    for (String quoteProduct : quoteProducts) {
                        Map<String, String> quoteProductsInfo = AxAccountingHelper.splitMergedRecords(quoteProduct);
                        String productId = quoteProductsInfo.get("productId");
                        // check if this product has ever been ORDERED by this customer
                        searchFields = FastList.newInstance();
                        searchFields.add("quoteProducts");
                        filterQueryFields = UtilMisc.toList("docType:quote");
                        filterQueryFields.add(awardedQuoteConstraint);
                        filterQueryFields.add(customerConstraint);

                        performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
                        performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

                        if (!ServiceUtil.isSuccess(performSolrSearchResponse)) {
                            Debug.logError("performSolrSearch failed!", module);
                            return isPenetrated;
                        }

                        orderedQuotes = (List) performSolrSearchResponse.get("records");
                        if (UtilValidate.isNotEmpty(orderedQuotes)) {
                            for (Map orderedQuote : orderedQuotes) {
                                List<String> quoteProductsAlreadyOrdered = (List<String>) orderedQuote.get("quoteProducts");
                                if (UtilValidate.isNotEmpty(quoteProductsAlreadyOrdered)) {
                                    for (String quoteProductAlreadyOrdered : quoteProductsAlreadyOrdered) {
                                        Map<String, String> quoteProductsInfoAlreadyOrdered = AxAccountingHelper.splitMergedRecords(quoteProductAlreadyOrdered);
                                        String productIdAlreadyOrdered = quoteProductsInfoAlreadyOrdered.get("productId");
                                        if (UtilValidate.isNotEmpty(productIdAlreadyOrdered) && productIdAlreadyOrdered.equals(productId)) {
                                            // we found an ordered product by the given customer!
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
        }

        return isPenetrated;
    }

    /**
     * Returns whether customer account agency is same as agency the base quote have.
     */
    public static Boolean isPartySameAgencyAsQuote(LocalDispatcher dispatcher, GenericValue userLogin, String quoteId, String customerPartyId) throws GenericServiceException {
        Boolean isAgencySame = false; // default

        // get the quote products by searching in solr
        List<String> searchFields = FastList.newInstance();
        searchFields.add("quoteBillToCustomerPartyId");
        List<String> filterQueryFields = UtilMisc.toList("docType:quote");
        String quoteIdConstraint = "quoteId:" + quoteId;
        filterQueryFields.add(quoteIdConstraint);

            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (!ServiceUtil.isSuccess(performSolrSearchResponse)) {
                Debug.logError("performSolrSearch failed!", module);
                return isAgencySame;
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            if (UtilValidate.isNotEmpty(orderEntries)) {
                Map orderEntry = orderEntries.get(0); // there will be only one quote!
                String quoteBillToCustomerPartyId = (String) orderEntry.get("quoteBillToCustomerPartyId");
                if (UtilValidate.isNotEmpty(quoteBillToCustomerPartyId)) {
                    Map<String, Object> getPartyAgencyResponse = dispatcher.runSync("getPartyAgency", UtilMisc.toMap("userLogin", userLogin, "partyId", customerPartyId));
                    if(ServiceUtil.isError(getPartyAgencyResponse)){
                        Debug.logError("An error occurred while performing solr search, details: " + ServiceUtil.getErrorMessage(getPartyAgencyResponse), module);
                    }
                    String agencyPartyId = (String) getPartyAgencyResponse.get("agencyPartyId");
                    if(agencyPartyId.equals(quoteBillToCustomerPartyId)) {
                        isAgencySame = true;
                    }
                }
            }

        return isAgencySame;
    }

    /**
     * Returns whether quote is penetrated or not for given quote Id.
     */
    public static Boolean isQuotePenetrated(LocalDispatcher dispatcher, String baseQuoteId, String customerQuoteId) throws GenericEntityException {
        Boolean isPenetrated = false; // default

        // get the quote products by searching in solr
        List<String> searchFields = FastList.newInstance();
        searchFields.add("quoteProducts");
        List<String> filterQueryFields = UtilMisc.toList("docType:quote");
        String quoteIdConstraint = "quoteId:" + baseQuoteId;
        filterQueryFields.add(quoteIdConstraint);
        String awardedQuoteConstraint = "quoteStatusId:QUO_ORDERED"; // used later
        String selectedQuoteConstraint = "quoteId:" + customerQuoteId; // used later

        try {
            Map performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", filterQueryFields);
            Map performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

            if (!ServiceUtil.isSuccess(performSolrSearchResponse)) {
                Debug.logError("performSolrSearch failed!", module);
                return isPenetrated;
            }

            List<Map> orderEntries = (List) performSolrSearchResponse.get("records");

            if (UtilValidate.isNotEmpty(orderEntries)) {
                Map orderEntry = orderEntries.get(0); // there will be only one quote!
                List<String> quoteProducts = (List<String>) orderEntry.get("quoteProducts");
                List<Map> orderedQuotes;
                if (UtilValidate.isNotEmpty(quoteProducts)) {
                    for (String quoteProduct : quoteProducts) {
                        Map<String, String> quoteProductsInfo = AxAccountingHelper.splitMergedRecords(quoteProduct);
                        String productId = quoteProductsInfo.get("productId");
                        // check if this product has ever been ORDERED by this customer
                        searchFields = FastList.newInstance();
                        searchFields.add("quoteProducts");
                        List<String> secondFilterQueryFields = UtilMisc.toList("docType:quote");
                        secondFilterQueryFields.add(awardedQuoteConstraint);
                        secondFilterQueryFields.add(selectedQuoteConstraint);

                        performSolrSearchContext = UtilMisc.toMap("searchFields", searchFields, "filterQueryFields", secondFilterQueryFields);
                        performSolrSearchResponse = dispatcher.runSync("performSolrSearch", performSolrSearchContext);

                        if (!ServiceUtil.isSuccess(performSolrSearchResponse)) {
                            Debug.logError("performSolrSearch failed!", module);
                            return isPenetrated;
                        }

                        orderedQuotes = (List) performSolrSearchResponse.get("records");
                        if (UtilValidate.isNotEmpty(orderedQuotes)) {
                            for (Map orderedQuote : orderedQuotes) {
                                List<String> quoteProductsAlreadyOrdered = (List<String>) orderedQuote.get("quoteProducts");
                                if (UtilValidate.isNotEmpty(quoteProductsAlreadyOrdered)) {
                                    for (String quoteProductAlreadyOrdered : quoteProductsAlreadyOrdered) {
                                        Map<String, String> quoteProductsInfoAlreadyOrdered = AxAccountingHelper.splitMergedRecords(quoteProductAlreadyOrdered);
                                        String productIdAlreadyOrdered = quoteProductsInfoAlreadyOrdered.get("productId");
                                        if (UtilValidate.isNotEmpty(productIdAlreadyOrdered) && productIdAlreadyOrdered.equals(productId)) {
                                            // we found an ordered product by the given customer quote!
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
        }

        return isPenetrated;
    }

    /**
     * Returns quote bill To customer party Map.
     * @param quote
     */
    public static Map getQuoteBillToCustomer(GenericValue quote) throws GenericEntityException {
        List<GenericValue> quoteRoles = EntityUtil.filterByDate(quote.getRelated("QuoteRole", null, null, false));
        Map<String, String> agencyMap = FastMap.newInstance();
        for (GenericValue quoteRole : quoteRoles) {
            String partyName = PartyHelper.getPartyName(quote.getDelegator(), quoteRole.getString("partyId"), false);
            if ("BILL_TO_CUSTOMER".equals(quoteRole.getString("roleTypeId"))) {
                agencyMap.put("partyId", quoteRole.getString("partyId"));
                agencyMap.put("partyName", partyName);
            }
        }
        return agencyMap;
    }
}