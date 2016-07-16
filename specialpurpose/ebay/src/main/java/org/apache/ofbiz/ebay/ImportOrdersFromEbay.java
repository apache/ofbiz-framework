/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.ebay;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImportOrdersFromEbay {

    private static final String resource = "EbayUiLabels";
    private static final String module = ImportOrdersFromEbay.class.getName();

    public static Map<String, Object> importOrdersSearchFromEbay(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            StringBuffer sellerTransactionsItemsXml = new StringBuffer();

            if (!ServiceUtil.isFailure(buildGetSellerTransactionsRequest(context, sellerTransactionsItemsXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), sellerTransactionsItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetSellerTransactions", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
                String success = (String)result.get(ModelService.SUCCESS_MESSAGE);
                if (success != null) {
                    result = checkOrders(delegator, dispatcher, locale, context, success);
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in importOrdersSearchFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrdersSearchFromEbay", locale));
        }

        return result;
    }

    public static Map<String, Object> importOrderFromEbay(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> order = new HashMap<String, Object>();
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            order.put("productStoreId", context.get("productStoreId"));
            order.put("userLogin", context.get("userLogin"));
            order.put("externalId", context.get("externalId"));
            order.put("createdDate", context.get("createdDate"));
            order.put("productId", context.get("productId"));
            order.put("quantityPurchased", context.get("quantityPurchased"));
            order.put("transactionPrice", context.get("transactionPrice"));
            order.put("shippingService", context.get("shippingService"));
            order.put("shippingServiceCost", context.get("shippingServiceCost"));
            order.put("shippingTotalAdditionalCost", context.get("shippingTotalAdditionalCost"));
            order.put("salesTaxAmount", context.get("salesTaxAmount"));
            order.put("salesTaxPercent", context.get("salesTaxPercent"));
            order.put("amountPaid", context.get("amountPaid"));
            order.put("paidTime", context.get("paidTime"));
            order.put("shippedTime", context.get("shippedTime"));

            order.put("buyerName", context.get("buyerName"));
            order.put("emailBuyer", context.get("emailBuyer"));
            order.put("shippingAddressPhone", context.get("shippingAddressPhone"));
            order.put("shippingAddressStreet", context.get("shippingAddressStreet"));
            order.put("shippingAddressStreet1", context.get("shippingAddressStreet1"));
            order.put("shippingAddressStreet2", context.get("shippingAddressStreet2"));
            order.put("shippingAddressPostalCode", context.get("shippingAddressPostalCode"));
            order.put("shippingAddressCountry", context.get("shippingAddressCountry"));
            order.put("shippingAddressStateOrProvince", context.get("shippingAddressStateOrProvince"));
            order.put("shippingAddressCityName", context.get("shippingAddressCityName"));

            result = createShoppingCart(delegator, dispatcher, locale, order, true);
        } catch (Exception e) {
            Debug.logError("Exception in importOrderFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrderFromEbay", locale));
        }

        return result;
    }

    public static Map<String, Object> setEbayOrderToComplete(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");
        String externalId = (String) context.get("externalId");
        String transactionId = "";
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            if (orderId == null && externalId == null) {
                Debug.logError("orderId or externalId must be filled", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.orderIdOrExternalIdAreMandatory", locale));
            }

            GenericValue orderHeader = null;
            if (UtilValidate.isNotEmpty(orderId)) {
                // Get the order header and verify if this order has been imported
                // from eBay (i.e. sales channel = EBAY_CHANNEL and externalId is set)
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                if (orderHeader == null) {
                    Debug.logError("Cannot find order with orderId [" + orderId + "]", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.errorRetrievingOrderFromOrderId", locale));
                }

                if (!"EBAY_SALES_CHANNEL".equals(orderHeader.getString("salesChannelEnumId")) || orderHeader.getString("externalId") == null) {
                    // This order was not imported from eBay: there is nothing to do.
                    return ServiceUtil.returnSuccess();
                }

                // get externalId from OrderHeader
                externalId = (String)orderHeader.get("externalId");
                transactionId = orderHeader.getString("transactionId");
                String productStoreId = (String) orderHeader.get("productStoreId");
                if (UtilValidate.isNotEmpty(productStoreId)) {
                    context.put("productStoreId", productStoreId);
                }
            }

            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);

            StringBuffer completeSaleXml = new StringBuffer();

            if (!ServiceUtil.isFailure(buildCompleteSaleRequest(delegator, locale, externalId, transactionId, context, completeSaleXml, eBayConfigResult.get("token").toString()))) {
                result = EbayHelper.postItem(eBayConfigResult.get("xmlGatewayUri").toString(), completeSaleXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "CompleteSale", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());

                String successMessage = (String)result.get("successMessage");
                if (successMessage != null) {
                    return readCompleteSaleResponse(successMessage, locale);
                } else{
                    ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.errorDuringPostCompleteSaleRequest", locale));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in setEbayOrderToComplete " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInSetEbayOrderToComplete", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> checkOrders(Delegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> context, String response) {
        StringBuffer errorMessage = new StringBuffer();
        List<Map<String, Object>> orders = readResponseFromEbay(response, locale, (String)context.get("productStoreId"), delegator, errorMessage);
        if (orders == null) {
            Debug.logError("Error :" + errorMessage.toString(), module);
            return ServiceUtil.returnFailure(errorMessage.toString());
        } else if (orders.size() == 0) {
            Debug.logError("No orders found", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
        } else {
            Iterator<Map<String, Object>> orderIter = orders.iterator();
            while (orderIter.hasNext()) {
                Map<String, Object> order = orderIter.next();
                order.put("productStoreId", context.get("productStoreId"));
                order.put("userLogin", context.get("userLogin"));
                Map<String, Object> error = createShoppingCart(delegator, dispatcher, locale, order, false);
                String errorMsg = ServiceUtil.getErrorMessage(error);
                if (UtilValidate.isNotEmpty(errorMsg)) {
                    order.put("errorMessage", errorMsg);
                } else {
                    order.put("errorMessage", "");
                }
            }
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("responseMessage", ModelService.RESPOND_SUCCESS);
            result.put("orderList", orders);
            return result;
        }
    }

    private static Map<String, Object> buildGetSellerTransactionsRequest(Map<String, Object> context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        String fromDate = (String)context.get("fromDate");
        String thruDate = (String)context.get("thruDate");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetSellerTransactionsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

             EbayHelper.appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);
             UtilXml.addChildElementValue(transElem, "IncludeContainingOrder", "true", transDoc);

             String fromDateOut = EbayHelper.convertDate(fromDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeFrom", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert from date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertFromDate", locale));
             }


             fromDateOut = EbayHelper.convertDate(thruDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeTo", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert thru date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertThruDate", locale));
             }
             //Debug.logInfo("The value of generated string is ======= " + UtilXml.writeXmlDocument(transDoc), module);
             dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get seller transactions request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetSellerTransactionRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> buildCompleteSaleRequest(Delegator delegator, Locale locale, String externalId, String transactionId, Map<String, Object> context, StringBuffer dataItemsXml, String token) {
        String paid = (String)context.get("paid");
        String shipped = (String)context.get("shipped");

        try {
            if (externalId == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.errorDuringBuildItemAndTransactionIdFromExternalId", locale));
            }

            Document transDoc = UtilXml.makeEmptyXmlDocument("CompleteSaleRequest");
            Element transElem = transDoc.getDocumentElement();
            transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

            EbayHelper.appendRequesterCredentials(transElem, transDoc, token);
            if (externalId.startsWith("EBT_")) {
                UtilXml.addChildElementValue(transElem, "TransactionID", externalId.substring(4), transDoc);
                UtilXml.addChildElementValue(transElem, "ItemID", transactionId.substring(4), transDoc);
            } else if (externalId.startsWith("EBO_")) {
                UtilXml.addChildElementValue(transElem, "OrderID", externalId.substring(4), transDoc);
                UtilXml.addChildElementValue(transElem, "TransactionID", "0", transDoc);
                UtilXml.addChildElementValue(transElem, "ItemID", "0", transDoc);
            } else if (externalId.startsWith("EBI_")) {
                UtilXml.addChildElementValue(transElem, "ItemID", externalId.substring(4), transDoc);
                UtilXml.addChildElementValue(transElem, "TransactionID", "0", transDoc);
            } else if (externalId.startsWith("EBS_")) {
                //UtilXml.addChildElementValue(transElem, "OrderID", externalId.substring(4), transDoc);
                UtilXml.addChildElementValue(transElem, "TransactionID", "0", transDoc);
                UtilXml.addChildElementValue(transElem, "ItemID", externalId.substring(4), transDoc);
            }

            // default shipped = Y (call from eca during order completed)
            if (paid == null && shipped == null) {
                shipped = "Y";
                paid = "Y";
            }

            // Set item id to paid or not paid
            if (UtilValidate.isNotEmpty(paid)) {
                if ("Y".equals(paid)) {
                    paid = "true";
                } else {
                    paid = "false";
                }
                UtilXml.addChildElementValue(transElem, "Paid", paid, transDoc);
            }

            // Set item id to shipped or not shipped
            if (UtilValidate.isNotEmpty(shipped)) {
                if ("Y".equals(shipped)) {
                    shipped = "true";
                } else {
                    shipped = "false";
                }
                UtilXml.addChildElementValue(transElem, "Shipped", shipped, transDoc);
            }

            dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
        } catch (Exception e) {
            Debug.logError("Exception during building complete sale request", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingCompleteSaleRequest", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> readCompleteSaleResponse(String msg, Locale locale) {
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List<? extends Element> errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator<? extends Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "ShortMessage", "");
                }
                return ServiceUtil.returnFailure(errorMessage);
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure();
        }
        return ServiceUtil.returnSuccess();
    }

    private static List<Map<String, Object>> readResponseFromEbay(String msg, Locale locale, String productStoreId, Delegator delegator, StringBuffer errorMessage) {
        List<Map<String, Object>> orders = null;
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            //Debug.logInfo("The generated string is ======= " + UtilXml.writeXmlDocument(docResponse), module);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            List<? extends Element> paginationList = UtilXml.childElementList(elemResponse, "PaginationResult");

            int totalOrders = 0;
            Iterator<? extends Element> paginationElemIter = paginationList.iterator();
            while (paginationElemIter.hasNext()) {
                Element paginationElement = paginationElemIter.next();
                String totalNumberOfEntries = UtilXml.childElementValue(paginationElement, "TotalNumberOfEntries", "0");
                totalOrders = Integer.valueOf(totalNumberOfEntries);
            }

            if (ack != null && "Success".equals(ack)) {
                orders = new LinkedList<Map<String,Object>>();
                if (totalOrders > 0) {
                    // retrieve transaction array
                    List<? extends Element> transactions = UtilXml.childElementList(elemResponse, "TransactionArray");
                    Iterator<? extends Element> transactionsElemIter = transactions.iterator();
                    while (transactionsElemIter.hasNext()) {
                        Element transactionsElement = transactionsElemIter.next();

                        // retrieve transaction
                        List<? extends Element> transaction = UtilXml.childElementList(transactionsElement, "Transaction");
                        Iterator<? extends Element> transactionElemIter = transaction.iterator();
                        while (transactionElemIter.hasNext()) {
                            Map<String, Object> order = new HashMap<String, Object>();
                            String itemId = "";

                            Element transactionElement = transactionElemIter.next();
                            List<? extends Element> containingOrders = UtilXml.childElementList(transactionElement, "ContainingOrder");
                            if (UtilValidate.isNotEmpty(containingOrders)) {
                                continue;
                            }
                            order.put("amountPaid", UtilXml.childElementValue(transactionElement, "AmountPaid", "0"));

                            // retrieve buyer
                            List<? extends Element> buyer = UtilXml.childElementList(transactionElement, "Buyer");
                            Iterator<? extends Element> buyerElemIter = buyer.iterator();
                            while (buyerElemIter.hasNext()) {
                                Element buyerElement = buyerElemIter.next();
                                order.put("emailBuyer", UtilXml.childElementValue(buyerElement, "Email", ""));
                                order.put("eiasTokenBuyer", UtilXml.childElementValue(buyerElement, "EIASToken", ""));
                                order.put("ebayUserIdBuyer", UtilXml.childElementValue(buyerElement, "UserID", ""));

                                // retrieve buyer information
                                List<? extends Element> buyerInfo = UtilXml.childElementList(buyerElement, "BuyerInfo");
                                Iterator<? extends Element> buyerInfoElemIter = buyerInfo.iterator();
                                while (buyerInfoElemIter.hasNext()) {
                                    Element buyerInfoElement = buyerInfoElemIter.next();

                                    // retrieve shipping address
                                    List<? extends Element> shippingAddressInfo = UtilXml.childElementList(buyerInfoElement, "ShippingAddress");
                                    Iterator<? extends Element> shippingAddressElemIter = shippingAddressInfo.iterator();
                                    while (shippingAddressElemIter.hasNext()) {
                                        Element shippingAddressElement = shippingAddressElemIter.next();
                                        order.put("buyerName", UtilXml.childElementValue(shippingAddressElement, "Name", ""));
                                        order.put("shippingAddressStreet", UtilXml.childElementValue(shippingAddressElement, "Street", ""));
                                        order.put("shippingAddressStreet1", UtilXml.childElementValue(shippingAddressElement, "Street1", ""));
                                        order.put("shippingAddressStreet2", UtilXml.childElementValue(shippingAddressElement, "Street2", ""));
                                        order.put("shippingAddressCityName", UtilXml.childElementValue(shippingAddressElement, "CityName", ""));
                                        order.put("shippingAddressStateOrProvince", UtilXml.childElementValue(shippingAddressElement, "StateOrProvince", ""));
                                        order.put("shippingAddressCountry", UtilXml.childElementValue(shippingAddressElement, "Country", ""));
                                        order.put("shippingAddressCountryName", UtilXml.childElementValue(shippingAddressElement, "CountryName", ""));
                                        order.put("shippingAddressPhone", UtilXml.childElementValue(shippingAddressElement, "Phone", ""));
                                        order.put("shippingAddressPostalCode", UtilXml.childElementValue(shippingAddressElement, "PostalCode", ""));
                                    }
                                }
                            }

                            // retrieve shipping details
                            List<? extends Element> shippingDetails = UtilXml.childElementList(transactionElement, "ShippingDetails");
                            Iterator<? extends Element> shippingDetailsElemIter = shippingDetails.iterator();
                            while (shippingDetailsElemIter.hasNext()) {
                                Element shippingDetailsElement = shippingDetailsElemIter.next();
                                order.put("insuranceFee", UtilXml.childElementValue(shippingDetailsElement, "InsuranceFee", "0"));
                                order.put("insuranceOption", UtilXml.childElementValue(shippingDetailsElement, "InsuranceOption", ""));
                                order.put("insuranceWanted", UtilXml.childElementValue(shippingDetailsElement, "InsuranceWanted", "false"));

                                // retrieve sales Tax
                                List<? extends Element> salesTax = UtilXml.childElementList(shippingDetailsElement, "SalesTax");
                                Iterator<? extends Element> salesTaxElemIter = salesTax.iterator();
                                while (salesTaxElemIter.hasNext()) {
                                    Element salesTaxElement = salesTaxElemIter.next();
                                    order.put("salesTaxAmount", UtilXml.childElementValue(salesTaxElement, "SalesTaxAmount", "0"));
                                    order.put("salesTaxPercent", UtilXml.childElementValue(salesTaxElement, "SalesTaxPercent", "0"));
                                    order.put("salesTaxState", UtilXml.childElementValue(salesTaxElement, "SalesTaxState", "0"));
                                    order.put("shippingIncludedInTax", UtilXml.childElementValue(salesTaxElement, "ShippingIncludedInTax", "false"));
                                }

                                // retrieve tax table
                                List<? extends Element> taxTable = UtilXml.childElementList(shippingDetailsElement, "TaxTable");
                                Iterator<? extends Element> taxTableElemIter = taxTable.iterator();
                                while (taxTableElemIter.hasNext()) {
                                    Element taxTableElement = taxTableElemIter.next();

                                    List<? extends Element> taxJurisdiction = UtilXml.childElementList(taxTableElement, "TaxJurisdiction");
                                    Iterator<? extends Element> taxJurisdictionElemIter = taxJurisdiction.iterator();
                                    while (taxJurisdictionElemIter.hasNext()) {
                                        Element taxJurisdictionElement = taxJurisdictionElemIter.next();

                                        order.put("jurisdictionID", UtilXml.childElementValue(taxJurisdictionElement, "JurisdictionID", ""));
                                        order.put("jurisdictionSalesTaxPercent", UtilXml.childElementValue(taxJurisdictionElement, "SalesTaxPercent", "0"));
                                        order.put("jurisdictionShippingIncludedInTax", UtilXml.childElementValue(taxJurisdictionElement, "ShippingIncludedInTax", "0"));
                                    }
                                }
                            }

                            // retrieve created date
                            order.put("createdDate", UtilXml.childElementValue(transactionElement, "CreatedDate", ""));

                            // retrieve item
                            List<? extends Element> item = UtilXml.childElementList(transactionElement, "Item");
                            Iterator<? extends Element> itemElemIter = item.iterator();
                            while (itemElemIter.hasNext()) {
                                Element itemElement = itemElemIter.next();
                                itemId = UtilXml.childElementValue(itemElement, "ItemID", "");
                                order.put("paymentMethods", UtilXml.childElementValue(itemElement, "PaymentMethods", ""));
                                order.put("quantity", UtilXml.childElementValue(itemElement, "Quantity", "0"));
                                order.put("startPrice", UtilXml.childElementValue(itemElement, "StartPrice", "0"));
                                order.put("title", UtilXml.childElementValue(itemElement, "Title", ""));

                                String productId = UtilXml.childElementValue(itemElement, "SKU", "");
                                if (UtilValidate.isEmpty(productId)) {
                                    productId = UtilXml.childElementValue(itemElement, "ApplicationData", "");
                                    if (UtilValidate.isEmpty(productId)) {
                                         productId = EbayHelper.retrieveProductIdFromTitle(delegator, (String)order.get("title"));
                                    }
                                }
                                order.put("productId", productId);

                                // retrieve selling status
                                List<? extends Element> sellingStatus = UtilXml.childElementList(itemElement, "SellingStatus");
                                Iterator<? extends Element> sellingStatusitemElemIter = sellingStatus.iterator();
                                while (sellingStatusitemElemIter.hasNext()) {
                                    Element sellingStatusElement = sellingStatusitemElemIter.next();
                                    order.put("amount", UtilXml.childElementValue(sellingStatusElement, "CurrentPrice", "0"));
                                    order.put("quantitySold", UtilXml.childElementValue(sellingStatusElement, "QuantitySold", "0"));
                                    order.put("listingStatus", UtilXml.childElementValue(sellingStatusElement, "ListingStatus", ""));
                                }
                            }

                            // retrieve quantity purchased
                            order.put("quantityPurchased", UtilXml.childElementValue(transactionElement, "QuantityPurchased", "0"));

                            // retrieve status
                            List<? extends Element> status = UtilXml.childElementList(transactionElement, "Status");
                            Iterator<? extends Element> statusElemIter = status.iterator();
                            while (statusElemIter.hasNext()) {
                                Element statusElement = statusElemIter.next();
                                order.put("eBayPaymentStatus", UtilXml.childElementValue(statusElement, "eBayPaymentStatus", ""));
                                order.put("checkoutStatus", UtilXml.childElementValue(statusElement, "CheckoutStatus", ""));
                                order.put("paymentMethodUsed", UtilXml.childElementValue(statusElement, "PaymentMethodUsed", ""));
                                order.put("completeStatus", UtilXml.childElementValue(statusElement, "CompleteStatus", ""));
                                order.put("buyerSelectedShipping", UtilXml.childElementValue(statusElement, "BuyerSelectedShipping", ""));
                            }

                            // retrieve transactionId
                            String transactionId = UtilXml.childElementValue(transactionElement, "TransactionID", "");

                            // set the externalId
                            if ("0".equals(transactionId)) {
                                // this is a Chinese Auction: ItemID is used to uniquely identify the transaction
                                order.put("externalId", "EBI_" + itemId);
                            } else {
                                order.put("externalId", "EBT_" + transactionId);
                            }
                            order.put("transactionId", "EBI_" + itemId);

                            GenericValue orderExist = externalOrderExists(delegator, (String)order.get("externalId"));
                            if (orderExist != null) {
                                order.put("orderId", orderExist.get("orderId"));
                            } else {
                                order.put("orderId", "");
                            }

                            // retrieve transaction price
                            order.put("transactionPrice", UtilXml.childElementValue(transactionElement, "TransactionPrice", "0"));

                            // retrieve external transaction
                            List<? extends Element> externalTransaction = UtilXml.childElementList(transactionElement, "ExternalTransaction");
                            Iterator<? extends Element> externalTransactionElemIter = externalTransaction.iterator();
                            while (externalTransactionElemIter.hasNext()) {
                                Element externalTransactionElement = externalTransactionElemIter.next();
                                order.put("externalTransactionID", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionID", ""));
                                order.put("externalTransactionTime", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionTime", ""));
                                order.put("feeOrCreditAmount", UtilXml.childElementValue(externalTransactionElement, "FeeOrCreditAmount", "0"));
                                order.put("paymentOrRefundAmount", UtilXml.childElementValue(externalTransactionElement, "PaymentOrRefundAmount", "0"));
                            }

                            // retrieve shipping service selected
                            List<? extends Element> shippingServiceSelected = UtilXml.childElementList(transactionElement, "ShippingServiceSelected");
                            Iterator<? extends Element> shippingServiceSelectedElemIter = shippingServiceSelected.iterator();
                            while (shippingServiceSelectedElemIter.hasNext()) {
                                Element shippingServiceSelectedElement = shippingServiceSelectedElemIter.next();
                                order.put("shippingService", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingService", ""));
                                order.put("shippingServiceCost", UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceCost", "0"));

                                String incuranceCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingInsuranceCost", "0");
                                String additionalCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingServiceAdditionalCost", "0");
                                String surchargeCost = UtilXml.childElementValue(shippingServiceSelectedElement, "ShippingSurcharge", "0");

                                double shippingInsuranceCost = 0;
                                double shippingServiceAdditionalCost = 0;
                                double shippingSurcharge = 0;

                                if (UtilValidate.isNotEmpty(incuranceCost)) {
                                    shippingInsuranceCost = new Double(incuranceCost).doubleValue();
                                }

                                if (UtilValidate.isNotEmpty(additionalCost)) {
                                    shippingServiceAdditionalCost = new Double(additionalCost).doubleValue();
                                }

                                if (UtilValidate.isNotEmpty(surchargeCost)) {
                                    shippingSurcharge = new Double(surchargeCost).doubleValue();
                                }

                                double shippingTotalAdditionalCost = shippingInsuranceCost + shippingServiceAdditionalCost + shippingSurcharge;
                                String totalAdditionalCost = new Double(shippingTotalAdditionalCost).toString();
                                order.put("shippingTotalAdditionalCost", totalAdditionalCost);
                            }

                            // retrieve paid time
                            order.put("paidTime", UtilXml.childElementValue(transactionElement, "PaidTime", ""));

                            // retrieve shipped time
                            order.put("shippedTime", UtilXml.childElementValue(transactionElement, "ShippedTime", ""));

                            order.put("productStoreId", productStoreId);

                            orders.add(order);
                        }
                    }
                }
            } else {
                List<? extends Element> errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator<? extends Element> errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return orders;
    }

    private static Map<String, Object> createShoppingCart(Delegator delegator, LocalDispatcher dispatcher, Locale locale, Map<String, Object> parameters, boolean create) {
        try {
            String productStoreId = (String) parameters.get("productStoreId");
            GenericValue userLogin = (GenericValue) parameters.get("userLogin");
            String defaultCurrencyUomId = "";
            String payToPartyId = "";
            String facilityId = "";

            // Product Store is mandatory
            if (productStoreId == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
            } else {
                GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
                if (productStore != null) {
                    defaultCurrencyUomId = productStore.getString("defaultCurrencyUomId");
                    payToPartyId = productStore.getString("payToPartyId");
                    facilityId = productStore.getString("inventoryFacilityId");
                } else {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
                }
            }

            // create a new shopping cart
            ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, defaultCurrencyUomId);

            // set the external id with the eBay Item Id
            String externalId = (String) parameters.get("externalId");

            if (UtilValidate.isNotEmpty(externalId)) {
                if (externalOrderExists(delegator, externalId) != null && create) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdAlreadyExist", locale));
                }
                cart.setExternalId(externalId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdNotAvailable", locale));
            }

            cart.setOrderType("SALES_ORDER");
            cart.setChannelType("EBAY_SALES_CHANNEL");
            cart.setUserLogin(userLogin, dispatcher);
            cart.setProductStoreId(productStoreId);

            if (UtilValidate.isNotEmpty(facilityId)) {
                cart.setFacilityId(facilityId);
            }

            String amountStr = (String) parameters.get("amountPaid");
            BigDecimal amountPaid = BigDecimal.ZERO;

            if (UtilValidate.isNotEmpty(amountStr)) {
                amountPaid = new BigDecimal(amountStr);
            }

            // add the payment EXT_BAY for the paid amount
            cart.addPaymentAmount("EXT_EBAY", amountPaid, externalId, null, true, false, false);

            // set the order date with the eBay created date
            Timestamp orderDate = UtilDateTime.nowTimestamp();
            if (UtilValidate.isNotEmpty(parameters.get("createdDate"))) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date createdDate = sdf.parse((String) parameters.get("createdDate"));
                orderDate = new Timestamp(createdDate.getTime());
            }
            cart.setOrderDate(orderDate);

            // check if the producId exists and it is valid
            String productId = (String) parameters.get("productId");
            if (UtilValidate.isEmpty(productId)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productIdNotAvailable", locale));
            } else {
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                if (UtilValidate.isEmpty(product)) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productIdDoesNotExist", locale));
                }
            }

            // Before import the order from eBay to OFBiz is mandatory that the payment has be received
            String paidTime = (String) parameters.get("paidTime");
            if (UtilValidate.isEmpty(paidTime)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            }

            BigDecimal unitPrice = new BigDecimal((String) parameters.get("transactionPrice"));
            BigDecimal quantity = new BigDecimal((String) parameters.get("quantityPurchased"));
            cart.addItemToEnd(productId, null, quantity, unitPrice, null, null, null, "PRODUCT_ORDER_ITEM", dispatcher, Boolean.FALSE, Boolean.FALSE);

            // set partyId from
            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }

            // Apply shipping costs as order adjustment
            String shippingCost = (String) parameters.get("shippingServiceCost");
            if (UtilValidate.isNotEmpty(shippingCost)) {
                double shippingAmount = new Double(shippingCost).doubleValue();
                if (shippingAmount > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount, 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            // Apply additional shipping costs as order adjustment
            String shippingTotalAdditionalCost = (String) parameters.get("shippingTotalAdditionalCost");
            if (UtilValidate.isNotEmpty(shippingTotalAdditionalCost)) {
                double shippingAdditionalCost = new Double(shippingTotalAdditionalCost).doubleValue();
                if (shippingAdditionalCost > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost, 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            // Apply sales tax as order adjustment
            String salesTaxAmount = (String) parameters.get("salesTaxAmount");
            String salesTaxPercent = (String) parameters.get("salesTaxPercent");
            if (UtilValidate.isNotEmpty(salesTaxAmount)) {
                double salesTaxAmountTotal = new Double(salesTaxAmount).doubleValue();
                if (salesTaxAmountTotal > 0) {
                    double salesPercent = 0.0;
                    if (UtilValidate.isNotEmpty(salesTaxPercent)) {
                        salesPercent = new Double(salesTaxPercent).doubleValue();
                    }
                    GenericValue salesTaxAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SALES_TAX", cart.getOrderId(), null, null, salesTaxAmountTotal, salesPercent);
                    if (salesTaxAdjustment != null) {
                        cart.addAdjustment(salesTaxAdjustment);
                    }
                }
            }

            // order has to be created ?
            if (create) {
                Debug.logInfo("Importing new order from eBay", module);
                // set partyId to
                String partyId = null;
                String contactMechId = "";
                GenericValue partyAttribute = null;
                if (UtilValidate.isNotEmpty(parameters.get("eiasTokenBuyer"))) {
                    partyAttribute = EntityQuery.use(delegator).from("PartyAttribute").where("attrValue", (String)parameters.get("eiasTokenBuyer")).queryFirst();
                }

                // if we get a party, check its contact information.
                if (UtilValidate.isNotEmpty(partyAttribute)) {
                    partyId = (String) partyAttribute.get("partyId");
                    Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                    GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

                    contactMechId = EbayHelper.setShippingAddressContactMech(dispatcher, delegator, party, userLogin, parameters);
                    String emailBuyer = (String) parameters.get("emailBuyer");
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        EbayHelper.setEmailContactMech(dispatcher, delegator, party, userLogin, parameters);
                    }
                    EbayHelper.setPhoneContactMech(dispatcher, delegator, party, userLogin, parameters);
                }

                // create party if none exists already
                if (UtilValidate.isEmpty(partyId)) {
                    Debug.logInfo("Creating new party for the eBay buyer.", module);
                    partyId = EbayHelper.createCustomerParty(dispatcher, (String) parameters.get("buyerName"), userLogin);
                    if (UtilValidate.isEmpty(partyId)) {
                        Debug.logWarning("Using admin party for the eBay buyer.", module);
                        partyId = "admin";
                    }
                }

                // create new party's contact information
                if (UtilValidate.isEmpty(contactMechId)) {
                    Debug.logInfo("Creating new postal address for party: " + partyId, module);
                    contactMechId = EbayHelper.createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", parameters);
                    if (UtilValidate.isEmpty(contactMechId)) {
                        return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayUnableToCreatePostalAddress", locale) + parameters);
                    }
                    Debug.logInfo("Created postal address: " + contactMechId, module);
                    Debug.logInfo("Creating new phone number for party: " + partyId, module);
                    EbayHelper.createPartyPhone(dispatcher, partyId, (String) parameters.get("shippingAddressPhone"), userLogin);
                    Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                    EbayHelper.createEbayCustomer(dispatcher, partyId, (String) parameters.get("ebayUserIdBuyer"), (String) parameters.get("eiasTokenBuyer"), userLogin);
                    String emailBuyer = (String) parameters.get("emailBuyer");
                    if (UtilValidate.isNotEmpty(emailBuyer) && !emailBuyer.equalsIgnoreCase("Invalid Request")) {
                        Debug.logInfo("Creating new email for party: " + partyId, module);
                        EbayHelper.createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                    }
                }

                Debug.logInfo("Setting cart roles for party: " + partyId, module);
                cart.setBillToCustomerPartyId(partyId);
                cart.setPlacingCustomerPartyId(partyId);
                cart.setShipToCustomerPartyId(partyId);
                cart.setEndUserCustomerPartyId(partyId);

                Debug.logInfo("Setting contact mech in cart: " + contactMechId, module);
                cart.setAllShippingContactMechId(contactMechId);
                cart.setAllMaySplit(Boolean.FALSE);

                Debug.logInfo("Setting shipment method: " + (String) parameters.get("shippingService"), module);
                EbayHelper.setShipmentMethodType(cart, (String) parameters.get("shippingService"), productStoreId, delegator);

                cart.makeAllShipGroupInfos();

                // create the order
                Debug.logInfo("Creating CheckOutHelper.", module);
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Debug.logInfo("Creating order.", module);
                Map<String, Object> orderCreate = checkout.createOrder(userLogin);

                String orderId = (String)orderCreate.get("orderId");
                Debug.logInfo("Created order with id: " + orderId, module);

                // approve the order
                if (UtilValidate.isNotEmpty(orderId)) {
                    Debug.logInfo("Approving order with id: " + orderId, module);
                    boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                    Debug.logInfo("Order approved with result: " + approved, module);

                    // create the payment from the preference
                    if (approved) {
                        Debug.logInfo("Creating payment for approved order.", module);
                        EbayHelper.createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), amountPaid, partyId);
                        Debug.logInfo("Payment created.", module);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in createShoppingCart: " + e.getMessage(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInCreateShoppingCart", locale) + ": " + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }


    private static GenericValue externalOrderExists(Delegator delegator, String externalId) throws GenericEntityException {
        Debug.logInfo("Checking for existing externalId: " + externalId, module);
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("externalId", EntityComparisonOperator.EQUALS, externalId), EntityCondition.makeCondition("statusId", EntityComparisonOperator.NOT_EQUAL, "ORDER_CANCELLED")), EntityComparisonOperator.AND);
        GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader")
                .where(condition)
                .cache(true)
                .queryFirst();
        return orderHeader;
    }
}
