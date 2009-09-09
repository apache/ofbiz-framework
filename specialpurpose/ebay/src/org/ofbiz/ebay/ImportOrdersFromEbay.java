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
package org.ofbiz.ebay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collection;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImportOrdersFromEbay {

    private static final String resource = "EbayUiLabels";
    private static final String module = ImportOrdersFromEbay.class.getName();

    public static Map importOrdersSearchFromEbay(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map result = FastMap.newInstance();
        try {
            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            StringBuffer sellerTransactionsItemsXml = new StringBuffer();

            if (!ServiceUtil.isFailure(buildGetSellerTransactionsRequest(context, sellerTransactionsItemsXml, eBayConfigResult.get("token").toString()))) {
                result = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), sellerTransactionsItemsXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "GetSellerTransactions", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
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

    public static Map importOrderFromEbay(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map order = FastMap.newInstance();
        Map result = FastMap.newInstance();
        try {
            order.put("productStoreId", (String) context.get("productStoreId"));
            order.put("userLogin", (GenericValue) context.get("userLogin"));
            order.put("externalId", (String) context.get("externalId"));
            order.put("transactionId", (String) context.get("transactionId"));
            order.put("createdDate", (String) context.get("createdDate"));
            order.put("productId", (String) context.get("productId"));
            order.put("quantityPurchased", (String) context.get("quantityPurchased"));
            order.put("transactionPrice", (String) context.get("transactionPrice"));
            order.put("shippingService", (String) context.get("shippingService"));
            order.put("shippingServiceCost", (String) context.get("shippingServiceCost"));
            order.put("shippingTotalAdditionalCost", (String) context.get("shippingTotalAdditionalCost"));
            order.put("salesTaxAmount", (String) context.get("salesTaxAmount"));
            order.put("salesTaxPercent", (String) context.get("salesTaxPercent"));
            order.put("amountPaid", (String) context.get("amountPaid"));
            order.put("paidTime", (String) context.get("paidTime"));
            order.put("shippedTime", (String) context.get("shippedTime"));

            order.put("buyerName", (String) context.get("buyerName"));
            order.put("emailBuyer", (String) context.get("emailBuyer"));
            order.put("shippingAddressPhone", (String) context.get("shippingAddressPhone"));
            order.put("shippingAddressStreet", (String) context.get("shippingAddressStreet"));
            order.put("shippingAddressStreet1", (String) context.get("shippingAddressStreet1"));
            order.put("shippingAddressStreet2", (String) context.get("shippingAddressStreet2"));
            order.put("shippingAddressPostalCode", (String) context.get("shippingAddressPostalCode"));
            order.put("shippingAddressCountry", (String) context.get("shippingAddressCountry"));
            order.put("shippingAddressStateOrProvince", (String) context.get("shippingAddressStateOrProvince"));
            order.put("shippingAddressCityName", (String) context.get("shippingAddressCityName"));

            result = createShoppingCart(delegator, dispatcher, locale, order, true);
        } catch (Exception e) {
            Debug.logError("Exception in importOrderFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrderFromEbay", locale));
        }

        return result;
    }

    public static Map setEbayOrderToComplete(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");
        String externalId = (String) context.get("externalId");
        String transactionId = (String) context.get("transactionId");
        Map result = FastMap.newInstance();
        try {
            if (orderId == null && externalId == null) {
                Debug.logError("orderId or externalId must be filled", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.orderIdOrExternalIdAreMandatory", locale));
            }

            GenericValue orderHeader = null;
            if (UtilValidate.isNotEmpty(orderId)) {
                // Get the order header and verify if this order has been imported
                // from eBay (i.e. sales channel = EBAY_CHANNEL and externalId is set)
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                if (orderHeader == null) {
                    Debug.logError("Cannot find order with orderId [" + orderId + "]", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.errorRetrievingOrderFromOrderId", locale));
                }

                if (!"EBAY_SALES_CHANNEL".equals(orderHeader.getString("salesChannelEnumId")) || orderHeader.getString("externalId") == null) {
                    // This order was not imported from eBay: there is nothing to do.
                    return ServiceUtil.returnSuccess();
                }

                // get externalId and transactionId from OrderHeader
                externalId = (String)orderHeader.get("externalId");
                transactionId = (String)orderHeader.get("transactionId");
            }

            Map<String, Object> eBayConfigResult = EbayHelper.buildEbayConfig(context, delegator);
            StringBuffer completeSaleXml = new StringBuffer();

            if (!ServiceUtil.isFailure(buildCompleteSaleRequest(delegator, locale, externalId, transactionId, context, completeSaleXml, eBayConfigResult.get("token").toString()))) {
                result = postItem(eBayConfigResult.get("xmlGatewayUri").toString(), completeSaleXml, eBayConfigResult.get("devID").toString(), eBayConfigResult.get("appID").toString(), eBayConfigResult.get("certID").toString(), "CompleteSale", eBayConfigResult.get("compatibilityLevel").toString(), eBayConfigResult.get("siteID").toString());
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

    private static String toString(InputStream inputStream) throws IOException {
        String string;
        StringBuilder outputBuilder = new StringBuilder();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }

    private static Map postItem(String postItemsUrl, StringBuffer dataItems, String devID, String appID, String certID,
                                String callName, String compatibilityLevel, String siteID) throws IOException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Request of " + callName + " To eBay:\n" + dataItems.toString(), module);
        }

        HttpURLConnection connection = (HttpURLConnection)(new URL(postItemsUrl)).openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-EBAY-API-COMPATIBILITY-LEVEL", compatibilityLevel);
        connection.setRequestProperty("X-EBAY-API-DEV-NAME", devID);
        connection.setRequestProperty("X-EBAY-API-APP-NAME", appID);
        connection.setRequestProperty("X-EBAY-API-CERT-NAME", certID);
        connection.setRequestProperty("X-EBAY-API-CALL-NAME", callName);
        connection.setRequestProperty("X-EBAY-API-SITEID", siteID);
        connection.setRequestProperty("Content-Type", "text/xml");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(dataItems.toString().getBytes());
        outputStream.close();

        int responseCode = connection.getResponseCode();
        InputStream inputStream = null;
        Map result = FastMap.newInstance();
        String response = null;

        if (responseCode == HttpURLConnection.HTTP_CREATED ||
            responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = toString(inputStream);
            result = ServiceUtil.returnSuccess(response);
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Response of " + callName + " From eBay:\n" + response, module);
        }

        return result;
    }

    private static Map checkOrders(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map context, String response) {
        StringBuffer errorMessage = new StringBuffer();
        List orders = readResponseFromEbay(response, locale, (String)context.get("productStoreId"), delegator, errorMessage);
        if (orders == null) {
            Debug.logError("Error :" + errorMessage.toString(), module);
            return ServiceUtil.returnFailure(errorMessage.toString());
        } else if (orders.size() == 0) {
            Debug.logError("No orders found", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
        } else {
            Iterator orderIter = orders.iterator();
            while (orderIter.hasNext()) {
                Map order = (Map)orderIter.next();
                order.put("productStoreId", (String) context.get("productStoreId"));
                order.put("userLogin", (GenericValue) context.get("userLogin"));
                Map error = createShoppingCart(delegator, dispatcher, locale, order, false);
                String errorMsg = ServiceUtil.getErrorMessage(error);
                if (UtilValidate.isNotEmpty(errorMsg)) {
                    order.put("errorMessage", errorMsg);
                } else {
                    order.put("errorMessage", "");
                }
            }
            Map result = FastMap.newInstance();
            result.put("responseMessage", ModelService.RESPOND_SUCCESS);
            result.put("orderList", orders);
            return result;
        }
    }

    private static Map buildGetSellerTransactionsRequest(Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        String fromDate = (String)context.get("fromDate");
        String thruDate = (String)context.get("thruDate");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetSellerTransactionsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

             appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);

             String fromDateOut = convertDate(fromDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeFrom", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert from date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertFromDate", locale));
             }


             fromDateOut = convertDate(thruDate, "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
             if (fromDateOut != null) {
                 UtilXml.addChildElementValue(transElem, "ModTimeTo", fromDateOut, transDoc);
             } else {
                 Debug.logError("Cannot convert thru date from yyyy-MM-dd HH:mm:ss.SSS date format to yyyy-MM-dd'T'HH:mm:ss.SSS'Z' date format", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.cannotConvertThruDate", locale));
             }

             dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get seller transactions request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetSellerTransactionRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    private static Map buildGetEbayDetailsRequest(Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GeteBayDetailsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

             appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailName", "ShippingServiceDetails", transDoc);
             UtilXml.addChildElementValue(transElem, "DetailName", "TaxJurisdiction", transDoc);
             dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
         } catch (Exception e) {
             Debug.logError("Exception during building get eBay details request", module);
             return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingGetEbayDetailsRequest", locale));
         }
         return ServiceUtil.returnSuccess();
    }

    public static Map buildCompleteSaleRequest(GenericDelegator delegator, Locale locale, String itemId, String transactionId, Map context, StringBuffer dataItemsXml, String token) {
        String paid = (String)context.get("paid");
        String shipped = (String)context.get("shipped");

        try {
            if (itemId == null || transactionId == null) {
                Debug.logError("Cannot be retrieve itemId and transactionId from externalId", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.errorDuringBuildItemAndTransactionIdFromExternalId", locale));
            }

            Document transDoc = UtilXml.makeEmptyXmlDocument("CompleteSaleRequest");
            Element transElem = transDoc.getDocumentElement();
            transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");

            appendRequesterCredentials(transElem, transDoc, token);

            UtilXml.addChildElementValue(transElem, "ItemID", itemId, transDoc);

            // default shipped = Y (call from eca during order completed)
            if (paid == null && shipped == null) {
                shipped = "Y";
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

            UtilXml.addChildElementValue(transElem, "TransactionID", transactionId, transDoc);

            dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
        } catch (Exception e) {
            Debug.logError("Exception during building complete sale request", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionDuringBuildingCompleteSaleRequest", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
    }

    private static Map readCompleteSaleResponse(String msg, Locale locale) {
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            if (ack != null && "Failure".equals(ack)) {
                String errorMessage = "";
                List errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = (Element) errorElemIter.next();
                    errorMessage = UtilXml.childElementValue(errorElement, "ShortMessage", "");
                }
                return ServiceUtil.returnFailure(errorMessage);
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure();
        }
        return ServiceUtil.returnSuccess();
    }

    private static List readResponseFromEbay(String msg, Locale locale, String productStoreId, GenericDelegator delegator, StringBuffer errorMessage) {
        List orders = null;
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            String ack = UtilXml.childElementValue(elemResponse, "Ack", "Failure");
            List paginationList = UtilXml.childElementList(elemResponse, "PaginationResult");

            int totalOrders = 0;
            Iterator paginationElemIter = paginationList.iterator();
            while (paginationElemIter.hasNext()) {
                Element paginationElement = (Element) paginationElemIter.next();
                String totalNumberOfEntries = UtilXml.childElementValue(paginationElement, "TotalNumberOfEntries", "0");
                totalOrders = new Integer(totalNumberOfEntries).intValue();
            }

            if (ack != null && "Success".equals(ack)) {
                orders = new ArrayList();
                if (totalOrders > 0) {
                    // retrieve transaction array
                    List transactions = UtilXml.childElementList(elemResponse, "TransactionArray");
                    Iterator transactionsElemIter = transactions.iterator();
                    while (transactionsElemIter.hasNext()) {
                        Element transactionsElement = (Element) transactionsElemIter.next();

                        // retrieve transaction
                        List transaction = UtilXml.childElementList(transactionsElement, "Transaction");
                        Iterator transactionElemIter = transaction.iterator();
                        while (transactionElemIter.hasNext()) {
                            Map order = FastMap.newInstance();
                            String itemId = "";

                            Element transactionElement = (Element) transactionElemIter.next();
                            order.put("amountPaid", UtilXml.childElementValue(transactionElement, "AmountPaid", "0"));

                            // retrieve buyer
                            List buyer = UtilXml.childElementList(transactionElement, "Buyer");
                            Iterator buyerElemIter = buyer.iterator();
                            while (buyerElemIter.hasNext()) {
                                Element buyerElement = (Element)buyerElemIter.next();
                                order.put("emailBuyer", UtilXml.childElementValue(buyerElement, "Email", ""));
                                order.put("eiasTokenBuyer", UtilXml.childElementValue(buyerElement, "EIASToken", ""));
                                order.put("ebayUserIdBuyer", UtilXml.childElementValue(buyerElement, "UserID", ""));

                                // retrieve buyer information
                                List buyerInfo = UtilXml.childElementList(buyerElement, "BuyerInfo");
                                Iterator buyerInfoElemIter = buyerInfo.iterator();
                                while (buyerInfoElemIter.hasNext()) {
                                    Element buyerInfoElement = (Element)buyerInfoElemIter.next();

                                    // retrieve shipping address
                                    List shippingAddressInfo = UtilXml.childElementList(buyerInfoElement, "ShippingAddress");
                                    Iterator shippingAddressElemIter = shippingAddressInfo.iterator();
                                    while (shippingAddressElemIter.hasNext()) {
                                        Element shippingAddressElement = (Element)shippingAddressElemIter.next();
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
                            List shippingDetails = UtilXml.childElementList(transactionElement, "ShippingDetails");
                            Iterator shippingDetailsElemIter = shippingDetails.iterator();
                            while (shippingDetailsElemIter.hasNext()) {
                                Element shippingDetailsElement = (Element)shippingDetailsElemIter.next();
                                order.put("insuranceFee", UtilXml.childElementValue(shippingDetailsElement, "InsuranceFee", "0"));
                                order.put("insuranceOption", UtilXml.childElementValue(shippingDetailsElement, "InsuranceOption", ""));
                                order.put("insuranceWanted", UtilXml.childElementValue(shippingDetailsElement, "InsuranceWanted", "false"));

                                // retrieve sales Tax
                                List salesTax = UtilXml.childElementList(shippingDetailsElement, "SalesTax");
                                Iterator salesTaxElemIter = salesTax.iterator();
                                while (salesTaxElemIter.hasNext()) {
                                    Element salesTaxElement = (Element)salesTaxElemIter.next();
                                    order.put("salesTaxAmount", UtilXml.childElementValue(salesTaxElement, "SalesTaxAmount", "0"));
                                    order.put("salesTaxPercent", UtilXml.childElementValue(salesTaxElement, "SalesTaxPercent", "0"));
                                    order.put("salesTaxState", UtilXml.childElementValue(salesTaxElement, "SalesTaxState", "0"));
                                    order.put("shippingIncludedInTax", UtilXml.childElementValue(salesTaxElement, "ShippingIncludedInTax", "false"));
                                }

                                // retrieve tax table
                                List taxTable = UtilXml.childElementList(shippingDetailsElement, "TaxTable");
                                Iterator taxTableElemIter = taxTable.iterator();
                                while (taxTableElemIter.hasNext()) {
                                    Element taxTableElement = (Element)taxTableElemIter.next();

                                    List taxJurisdiction = UtilXml.childElementList(taxTableElement, "TaxJurisdiction");
                                    Iterator taxJurisdictionElemIter = taxJurisdiction.iterator();
                                    while (taxJurisdictionElemIter.hasNext()) {
                                        Element taxJurisdictionElement = (Element)taxJurisdictionElemIter.next();

                                        order.put("jurisdictionID", UtilXml.childElementValue(taxJurisdictionElement, "JurisdictionID", ""));
                                        order.put("jurisdictionSalesTaxPercent", UtilXml.childElementValue(taxJurisdictionElement, "SalesTaxPercent", "0"));
                                        order.put("jurisdictionShippingIncludedInTax", UtilXml.childElementValue(taxJurisdictionElement, "ShippingIncludedInTax", "0"));
                                    }
                                }
                            }

                            // retrieve created date
                            order.put("createdDate", UtilXml.childElementValue(transactionElement, "CreatedDate", ""));

                            // retrieve item
                            List item = UtilXml.childElementList(transactionElement, "Item");
                            Iterator itemElemIter = item.iterator();
                            while (itemElemIter.hasNext()) {
                                Element itemElement = (Element)itemElemIter.next();
                                itemId = UtilXml.childElementValue(itemElement, "ItemID", "");
                                order.put("paymentMethods", UtilXml.childElementValue(itemElement, "PaymentMethods", ""));
                                order.put("quantity", UtilXml.childElementValue(itemElement, "Quantity", "0"));
                                order.put("startPrice", UtilXml.childElementValue(itemElement, "StartPrice", "0"));
                                order.put("title", UtilXml.childElementValue(itemElement, "Title", ""));

                                String productId = UtilXml.childElementValue(itemElement, "SKU", "");
                                if (UtilValidate.isEmpty(productId)) {
                                    productId = UtilXml.childElementValue(itemElement, "ApplicationData", "");
                                    if (UtilValidate.isEmpty(productId)) {
                                         productId = retrieveProductIdFromTitle(delegator, (String)order.get("title"));
                                    }
                                }
                                order.put("productId", productId);

                                // retrieve selling status
                                List sellingStatus = UtilXml.childElementList(itemElement, "SellingStatus");
                                Iterator sellingStatusitemElemIter = sellingStatus.iterator();
                                while (sellingStatusitemElemIter.hasNext()) {
                                    Element sellingStatusElement = (Element)sellingStatusitemElemIter.next();
                                    order.put("amount", UtilXml.childElementValue(sellingStatusElement, "CurrentPrice", "0"));
                                    order.put("quantitySold", UtilXml.childElementValue(sellingStatusElement, "QuantitySold", "0"));
                                    order.put("listingStatus", UtilXml.childElementValue(sellingStatusElement, "ListingStatus", ""));
                                }
                            }

                            // retrieve quantity purchased
                            order.put("quantityPurchased", UtilXml.childElementValue(transactionElement, "QuantityPurchased", "0"));

                            // retrieve status
                            List status = UtilXml.childElementList(transactionElement, "Status");
                            Iterator statusElemIter = status.iterator();
                            while (statusElemIter.hasNext()) {
                                Element statusElement = (Element)statusElemIter.next();
                                order.put("eBayPaymentStatus", UtilXml.childElementValue(statusElement, "eBayPaymentStatus", ""));
                                order.put("checkoutStatus", UtilXml.childElementValue(statusElement, "CheckoutStatus", ""));
                                order.put("paymentMethodUsed", UtilXml.childElementValue(statusElement, "PaymentMethodUsed", ""));
                                order.put("completeStatus", UtilXml.childElementValue(statusElement, "CompleteStatus", ""));
                                order.put("buyerSelectedShipping", UtilXml.childElementValue(statusElement, "BuyerSelectedShipping", ""));
                            }

                            // retrieve transactionId
                            String transactionId = UtilXml.childElementValue(transactionElement, "TransactionID", "");

                            // set the externalId and transactionId
                            order.put("externalId", itemId);
                            order.put("transactionId", transactionId);

                            GenericValue orderExist = externalOrderExists(delegator, itemId, transactionId);
                            if (orderExist != null) {
                                order.put("orderId", (String)orderExist.get("orderId"));
                            } else {
                                order.put("orderId", "");
                            }

                            // retrieve transaction price
                            order.put("transactionPrice", UtilXml.childElementValue(transactionElement, "TransactionPrice", "0"));

                            // retrieve external transaction
                            List externalTransaction = UtilXml.childElementList(transactionElement, "ExternalTransaction");
                            Iterator externalTransactionElemIter = externalTransaction.iterator();
                            while (externalTransactionElemIter.hasNext()) {
                                Element externalTransactionElement = (Element)externalTransactionElemIter.next();
                                order.put("externalTransactionID", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionID", ""));
                                order.put("externalTransactionTime", UtilXml.childElementValue(externalTransactionElement, "ExternalTransactionTime", ""));
                                order.put("feeOrCreditAmount", UtilXml.childElementValue(externalTransactionElement, "FeeOrCreditAmount", "0"));
                                order.put("paymentOrRefundAmount", UtilXml.childElementValue(externalTransactionElement, "PaymentOrRefundAmount", "0"));
                            }

                            // retrieve shipping service selected
                            List shippingServiceSelected = UtilXml.childElementList(transactionElement, "ShippingServiceSelected");
                            Iterator shippingServiceSelectedElemIter = shippingServiceSelected.iterator();
                            while (shippingServiceSelectedElemIter.hasNext()) {
                                Element shippingServiceSelectedElement = (Element)shippingServiceSelectedElemIter.next();
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
                List errorList = UtilXml.childElementList(elemResponse, "Errors");
                Iterator errorElemIter = errorList.iterator();
                while (errorElemIter.hasNext()) {
                    Element errorElement = (Element) errorElemIter.next();
                    errorMessage.append(UtilXml.childElementValue(errorElement, "ShortMessage", ""));
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception during read response from Ebay", module);
        }
        return orders;
    }

    private static Map createShoppingCart(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map parameters, boolean create) {
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
                GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
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

            // set the transaction id with the eBay Transacation Id
            String transactionId = (String) parameters.get("transactionId");

            if (UtilValidate.isNotEmpty(externalId)) {
                if (externalOrderExists(delegator, externalId, transactionId) != null && create) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.externalIdAlreadyExist", locale));
                }
                cart.setExternalId(externalId);
                cart.setTransactionId(transactionId);
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
            if (UtilValidate.isNotEmpty((String) parameters.get("createdDate"))) {
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
                GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
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
                    GenericValue shippingAdjustment = makeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount, 0.0);
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
                    GenericValue shippingAdjustment = makeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost, 0.0);
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
                    GenericValue salesTaxAdjustment = makeOrderAdjustment(delegator, "SALES_TAX", cart.getOrderId(), null, null, salesTaxAmountTotal, salesPercent);
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
                String emailContactMechId = null;
                String phoneContactMechId = null;
                GenericValue partyAttribute = null;
                if (UtilValidate.isNotEmpty((String)parameters.get("eiasTokenBuyer"))) {
                    partyAttribute = EntityUtil.getFirst(delegator.findByAnd("PartyAttribute", UtilMisc.toMap("attrValue", (String)parameters.get("eiasTokenBuyer"))));
                }

                // if we get a party, check its contact information.
                if (UtilValidate.isNotEmpty(partyAttribute)) {
                    partyId = (String) partyAttribute.get("partyId");
                    Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                    GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

                    contactMechId = setShippingAddressContactMech(dispatcher, delegator, party, userLogin, parameters);
                    String emailBuyer = (String) parameters.get("emailBuyer");
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        String emailContactMech = setEmailContactMech(dispatcher, delegator, party, userLogin, parameters);
                    }
                    String phoneContactMech = setPhoneContactMech(dispatcher, delegator, party, userLogin, parameters);
                }

                // create party if none exists already
                if (UtilValidate.isEmpty(partyId)) {
                    Debug.logInfo("Creating new party for the eBay buyer.", module);
                    partyId = createCustomerParty(dispatcher, (String) parameters.get("buyerName"), userLogin);
                    if (UtilValidate.isEmpty(partyId)) {
                        Debug.logWarning("Using admin party for the eBay buyer.", module);
                        partyId = "admin";
                    }
                }

                // create new party's contact information
                if (UtilValidate.isEmpty(contactMechId)) {
                    Debug.logInfo("Creating new postal address for party: " + partyId, module);
                    contactMechId = createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", parameters);
                    if (UtilValidate.isEmpty(contactMechId)) {
                        return ServiceUtil.returnFailure("Unable to create postalAddress with input map: " + parameters);
                    }
                    Debug.logInfo("Created postal address: " + contactMechId, module);
                    Debug.logInfo("Creating new phone number for party: " + partyId, module);
                    createPartyPhone(dispatcher, partyId, (String) parameters.get("shippingAddressPhone"), userLogin);
                    Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                    createEbayCustomer(dispatcher, partyId, (String) parameters.get("ebayUserIdBuyer"), (String) parameters.get("eiasTokenBuyer"), userLogin);
                    String emailBuyer = (String) parameters.get("emailBuyer");
                    if (UtilValidate.isNotEmpty(emailBuyer) && !emailBuyer.equalsIgnoreCase("Invalid Request")) {
                        Debug.logInfo("Creating new email for party: " + partyId, module);
                        createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                    }
                }

                Debug.logInfo("Setting cart roles for party: " + partyId, module);
                cart.setBillToCustomerPartyId(partyId);
                cart.setPlacingCustomerPartyId(partyId);
                cart.setShipToCustomerPartyId(partyId);
                cart.setEndUserCustomerPartyId(partyId);

                Debug.logInfo("Setting contact mech in cart: " + contactMechId, module);
                cart.setShippingContactMechId(contactMechId);
                cart.setMaySplit(Boolean.FALSE);

                Debug.logInfo("Setting shipment method: " + (String) parameters.get("shippingService"), module);
                setShipmentMethodType(cart, (String) parameters.get("shippingService"));

                cart.makeAllShipGroupInfos();

                // create the order
                Debug.logInfo("Creating CheckOutHelper.", module);
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Debug.logInfo("Creating order.", module);
                Map orderCreate = checkout.createOrder(userLogin);

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
                        createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), partyId);
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

    private static boolean createPaymentFromPaymentPreferences(GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
                                                               String orderId, String externalId, Timestamp orderDate, String partyIdFrom) {
        List paymentPreferences = null;
        try {
            Map paymentFields = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_RECEIVED", "paymentMethodTypeId", "EXT_EBAY");
            paymentPreferences = delegator.findByAnd("OrderPaymentPreference", paymentFields);

            if (UtilValidate.isNotEmpty(paymentPreferences)) {
                Iterator i = paymentPreferences.iterator();
                while (i.hasNext()) {
                    GenericValue pref = (GenericValue) i.next();
                    boolean okay = createPayment(dispatcher, userLogin, pref, orderId, externalId, orderDate, partyIdFrom);
                    if (!okay)
                        return false;
                }
            }
        } catch (Exception e) {
            Debug.logError(e, "Cannot get payment preferences for order #" + orderId, module);
            return false;
        }
        return true;
    }

    private static boolean createPayment(LocalDispatcher dispatcher, GenericValue userLogin, GenericValue paymentPreference,
                                         String orderId, String externalId, Timestamp orderDate, String partyIdFrom) {
        try {
            GenericDelegator delegator = paymentPreference.getDelegator();

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse");
            response.set("paymentGatewayResponseId", responseId);
            response.set("paymentServiceTypeEnumId", "PRDS_PAY_EXTERNAL");
            response.set("orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"));
            response.set("paymentMethodTypeId", paymentPreference.get("paymentMethodTypeId"));
            response.set("paymentMethodId", paymentPreference.get("paymentMethodId"));
            response.set("amount", paymentPreference.get("maxAmount"));
            response.set("referenceNum", externalId);
            response.set("transactionDate", orderDate);
            delegator.createOrStore(response);

            // create the payment
            Map results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin,
                                             "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"),
                                             "paymentFromId", partyIdFrom,
                                             "paymentRefNum", externalId,
                                             "comments", "Payment receive via eBay"));

            if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
                return false;
            }
            return true;
        } catch (Exception e) {
            Debug.logError(e, "Failed to create the payment for order " + orderId, module);
            return false;
        }
    }

    private static GenericValue makeOrderAdjustment(GenericDelegator delegator, String orderAdjustmentTypeId, String orderId,
                                                    String orderItemSeqId, String shipGroupSeqId, double amount, double sourcePercentage) {
        GenericValue orderAdjustment  = null;

        try {
            if (UtilValidate.isNotEmpty(orderItemSeqId)) {
                orderItemSeqId = "_NA_";
            }
            if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
                shipGroupSeqId = "_NA_";
            }

            Map inputMap = UtilMisc.toMap("orderAdjustmentTypeId", orderAdjustmentTypeId,  "orderId", orderId, "orderItemSeqId", orderItemSeqId,
                                          "shipGroupSeqId", shipGroupSeqId, "amount", new Double(amount));
            if (sourcePercentage != 0) {
                inputMap.put("sourcePercentage", new Double(sourcePercentage));
            }
            orderAdjustment = delegator.makeValue("OrderAdjustment", inputMap);
        } catch (Exception e) {
            Debug.logError(e, "Failed to made order adjustment for order " + orderId, module);
        }
        return orderAdjustment;
    }

    private static String createCustomerParty(LocalDispatcher dispatcher, String name, GenericValue userLogin) {
        String partyId = null;

        try {
            if (UtilValidate.isNotEmpty(name) && UtilValidate.isNotEmpty(userLogin)) {
                Debug.logVerbose("Creating Customer Party: "+name, module);

                // Try to split the lastname from the firstname
                String firstName = "";
                String lastName = "";
                int pos = name.indexOf(" ");

                if (pos >= 0) {
                    firstName = name.substring(0, pos);
                    lastName = name.substring(pos+1);
                } else {
                    lastName = name;
                }

                Map summaryResult = dispatcher.runSync("createPerson", UtilMisc.<String, Object>toMap("description", name, "firstName", firstName, "lastName", lastName,
                                                                                      "userLogin", userLogin, "comments", "Created via eBay"));
                partyId = (String) summaryResult.get("partyId");
                Debug.logVerbose("Created Customer Party: "+partyId, module);
            }
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPerson", module);
        }
        return partyId;
    }

    private static String createAddress(LocalDispatcher dispatcher, String partyId, GenericValue userLogin,
                                       String contactMechPurposeTypeId, Map address) {
        Debug.logInfo("Creating postal address with input map: " + address, module);
        String contactMechId = null;
        try {
            Map context = FastMap.newInstance();
            context.put("partyId", partyId);
            context.put("toName", (String)address.get("buyerName"));
            context.put("address1", (String)address.get("shippingAddressStreet1"));
            context.put("address2", (String)address.get("shippingAddressStreet2"));
            context.put("postalCode", (String)address.get("shippingAddressPostalCode"));
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", contactMechPurposeTypeId);

            String country = (String)address.get("shippingAddressCountry");
            String state = (String)address.get("shippingAddressStateOrProvince");
            String city = (String)address.get("shippingAddressCityName");
            correctCityStateCountry(dispatcher, context, city, state, country);

            Map summaryResult = dispatcher.runSync("createPartyPostalAddress",context);
            contactMechId = (String)summaryResult.get("contactMechId");
            // Set also as a billing address
            context = FastMap.newInstance();
            context.put("partyId", partyId);
            context.put("contactMechId", contactMechId);
            context.put("contactMechPurposeTypeId", "BILLING_LOCATION");
            context.put("userLogin", userLogin);
            dispatcher.runSync("createPartyContactMechPurpose", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createAddress", module);
        }
        return contactMechId;
    }

    private static void correctCityStateCountry(LocalDispatcher dispatcher, Map map, String city, String state, String country) {
        try {
            Debug.logInfo("correctCityStateCountry params: " + city + ", " + state + ", " + country, module);
            if (UtilValidate.isEmpty(country)) {
                country = "US";
            }
            country = country.toUpperCase();
            if (country.indexOf("UNITED STATES") > -1 || country.indexOf("USA") > -1) {
                country = "US";
            }
            Debug.logInfo("GeoCode: " + country, module);
            Map outMap = getCountryGeoId(dispatcher.getDelegator(), country);
            String geoId = (String)outMap.get("geoId");
            if (UtilValidate.isEmpty(geoId)) {
                geoId = "USA";
            }
            map.put("countryGeoId", geoId);
            Debug.logInfo("Country geoid: " + geoId, module);
            if (geoId.equals("USA") || geoId.equals("CAN")) {
                if (UtilValidate.isNotEmpty(state)) {
                    map.put("stateProvinceGeoId", state.toUpperCase());
                }
                map.put("city", city);
            } else {
                map.put("city", city + ", " + state);
            }
            Debug.logInfo("State geoid: " + state, module);
        } catch (Exception e) {
            Debug.logError(e, "Failed to correctCityStateCountry", module);
        }
    }

    private static String createPartyPhone(LocalDispatcher dispatcher, String partyId, String phoneNumber, GenericValue userLogin) {
        Map summaryResult = FastMap.newInstance();
        Map context = FastMap.newInstance();
        String phoneContactMechId = null;

        try {
            context.put("contactNumber", phoneNumber);
            context.put("partyId", partyId);
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", "PHONE_SHIPPING");
            summaryResult = dispatcher.runSync("createPartyTelecomNumber", context);
            phoneContactMechId = (String)summaryResult.get("contactMechId");
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPartyPhone", module);
        }
        return phoneContactMechId;
    }

    private static String createPartyEmail(LocalDispatcher dispatcher, String partyId, String email, GenericValue userLogin) {
        Map context = FastMap.newInstance();
        Map summaryResult = FastMap.newInstance();
        String emailContactMechId = null;

        try {
            if (UtilValidate.isNotEmpty(email)) {
                context.clear();
                context.put("emailAddress", email);
                context.put("userLogin", userLogin);
                context.put("contactMechTypeId", "EMAIL_ADDRESS");
                summaryResult = dispatcher.runSync("createEmailAddress", context);
                emailContactMechId = (String) summaryResult.get("contactMechId");

                context.clear();
                context.put("partyId", partyId);
                context.put("contactMechId", emailContactMechId);
                context.put("contactMechPurposeTypeId", "OTHER_EMAIL");
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyContactMech", context);
            }
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPartyEmail", module);
        }
        return emailContactMechId;
    }

    public static void createEbayCustomer(LocalDispatcher dispatcher, String partyId, String ebayUserIdBuyer, String eias, GenericValue userLogin) {
        Map context = FastMap.newInstance();
        Map summaryResult = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(eias)) {
            try {
                context.put("partyId", partyId);
                context.put("attrName", "EBAY_BUYER_EIAS");
                context.put("attrValue", eias);
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyAttribute", context);
            } catch (Exception e) {
                Debug.logError(e, "Failed to create eBay EIAS party attribute");
            }
            context.clear();
            summaryResult.clear();
        }
        if (UtilValidate.isNotEmpty(ebayUserIdBuyer)) {
            try {
                context.put("partyId", partyId);
                context.put("attrName", "EBAY_BUYER_USER_ID");
                context.put("attrValue", ebayUserIdBuyer);
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyAttribute", context);
            } catch (Exception e) {
                Debug.logError(e, "Failed to create eBay userId party attribute");
            }
        }
    }

    private static Map getCountryGeoId(GenericDelegator delegator, String geoCode) {
        GenericValue geo = null;
        try {
            Debug.logInfo("geocode: " + geoCode, module);

            geo = EntityUtil.getFirst(delegator.findByAnd("Geo", UtilMisc.toMap("geoCode", geoCode.toUpperCase(), "geoTypeId", "COUNTRY")));
            Debug.logInfo("Found a geo entity " + geo, module);
            if (UtilValidate.isEmpty(geo)) {
                geo = delegator.makeValue("Geo");
                geo.set("geoId", geoCode + "_IMPORTED");
                geo.set("geoTypeId", "COUNTRY");
                geo.set("geoName", geoCode + "_IMPORTED");
                geo.set("geoCode", geoCode + "_IMPORTED");
                geo.set("abbreviation", geoCode + "_IMPORTED");
                delegator.create(geo);
                Debug.logInfo("Creating new geo entity: " + geo, module);
            }
        } catch (Exception e) {
            String errMsg = "Failed to find/setup geo id";
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("geoId", (String)geo.get("geoId"));
        return result;
    }

    private static GenericValue externalOrderExists(GenericDelegator delegator, String externalId, String transactionId) throws GenericEntityException {
        Debug.logInfo("Checking for existing externalId: " + externalId +" and transactionId: " + transactionId, module);
        GenericValue orderHeader = null;
        List entities = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "transactionId", transactionId));
        if (UtilValidate.isNotEmpty(entities)) {
            orderHeader = EntityUtil.getFirst(entities);
        }
        return orderHeader;
    }

    private static String convertDate(String dateIn, String fromDateFormat, String toDateFormat) {
        String dateOut;
        try {
            SimpleDateFormat formatIn = new SimpleDateFormat(fromDateFormat);
            SimpleDateFormat formatOut= new SimpleDateFormat(toDateFormat);
            Date data = formatIn.parse(dateIn, new ParsePosition(0));
            dateOut = formatOut.format(data);
        } catch (Exception e) {
            dateOut = null;
        }
        return dateOut;
    }

    private static void setShipmentMethodType(ShoppingCart cart, String shippingService) {
        String partyId = "_NA_";
        String shipmentMethodTypeId = "NO_SHIPPING";

        if (shippingService != null) {
            if ("USPSPriority".equals(shippingService)) {
                partyId = "USPS";
                shipmentMethodTypeId = "STANDARD";
            } else if ("UPSGround".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "GROUND";
            } else if ("UPS3rdDay".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "THIRD_DAY";
            } else if ("UPS2ndDay".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "SECOND_DAY";
            } else if ("USPSExpressMailInternational".equals(shippingService)) {
                partyId = "USPS";
                shipmentMethodTypeId = "INT_EXPRESS";
            } else if ("UPSNextDay".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "NEXT_DAY";
            } else if ("UPSNextDayAir".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "AIR";
            } else if ("ShippingMethodStandard".equals(shippingService)) {
                partyId = "UPS";
                shipmentMethodTypeId = "GROUND";
            } else if ("StandardInternational".equals(shippingService)) {
                partyId = "USPS";
                shipmentMethodTypeId = "INT_EXPRESS";
            } else if ("LocalDelivery".equals(shippingService)) {
                partyId = "_NA_";
                shipmentMethodTypeId = "STANDARD";
            }
        }
        cart.setCarrierPartyId(partyId);
        cart.setShipmentMethodTypeId(shipmentMethodTypeId);
    }

    private static String setShippingAddressContactMech (LocalDispatcher dispatcher, GenericDelegator delegator, GenericValue party, GenericValue userLogin, Map parameters) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a shipping location purpose.
        Collection shippingLocations = ContactHelper.getContactMechByPurpose(party, "SHIPPING_LOCATION", false);

        // check them to see if one matches
        Iterator shippingLocationsIterator = shippingLocations.iterator();
        while (shippingLocationsIterator.hasNext()) {
            GenericValue shippingLocation = (GenericValue) shippingLocationsIterator.next();
            contactMechId = shippingLocation.getString("contactMechId");
            GenericValue postalAddress;
            try {
                // get the postal address for this contact mech
                postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", contactMechId));

                //  match values to compare by modifying them the same way they were when they were created
                String country = ((String)parameters.get("shippingAddressCountry")).toUpperCase();
                String state = ((String)parameters.get("shippingAddressStateOrProvince")).toUpperCase();
                String city = (String)parameters.get("shippingAddressCityName");
                correctCityStateCountry(dispatcher, parameters, city, state, country);

                // TODO:  The following comparison does not consider the To Name or Attn: lines of the address.
                //
                // now compare values.  If all fields match, that's our shipping address.  Return the related contact mech id.
                if (   parameters.get("shippingAddressStreet1").toString().equals((postalAddress.get("address1").toString())) &&
                        parameters.get("shippingAddressStreet2").toString().equals((postalAddress.get("address2").toString())) &&
                        parameters.get("city").toString().equals((postalAddress.get("city").toString())) &&
                        parameters.get("stateProvinceGeoId").toString().equals((postalAddress.get("stateProvinceGeoId").toString())) &&
                        parameters.get("countryGeoId").toString().equals((postalAddress.get("countryGeoId").toString())) &&
                        parameters.get("shippingAddressPostalCode").toString().equals((postalAddress.get("postalCode").toString()))
                       ) { // this is an exact address match!!
                    return contactMechId;
                }
            } catch (Exception e) {
                Debug.logError(e, "Problem with verifying postal addresses for contactMechId " + contactMechId + ".", module);
            }
        }
        // none of the existing contact mechs/postal addresses match (or none were found).  Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", parameters);
    }

    private static String setEmailContactMech (LocalDispatcher dispatcher, GenericDelegator delegator, GenericValue party, GenericValue userLogin, Map parameters) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a email address purpose.
        Collection emailAddressContactMechs = ContactHelper.getContactMechByPurpose(party, "OTHER_EMAIL", false);

        // check them to see if one matches
        Iterator emailAddressesContactMechsIterator = emailAddressContactMechs.iterator();
        while (emailAddressesContactMechsIterator.hasNext()) {
            GenericValue emailAddressContactMech = (GenericValue) emailAddressesContactMechsIterator.next();
            contactMechId = emailAddressContactMech.getString("contactMechId");
            // now compare values.  If one matches, that's our email address.  Return the related contact mech id.
            if (parameters.get("emailBuyer").toString().equals((emailAddressContactMech.get("infoString").toString()))) {
                 return contactMechId;
            }
        }
        // none of the existing contact mechs/email addresses match (or none were found).  Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createPartyEmail(dispatcher, partyId, (String) parameters.get("emailBuyer"), userLogin);
    }

    private static String setPhoneContactMech (LocalDispatcher dispatcher, GenericDelegator delegator, GenericValue party, GenericValue userLogin, Map parameters) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a telecom number purpose.
        Collection phoneNumbers = ContactHelper.getContactMechByPurpose(party, "PHONE_SHIPPING", false);

        // check them to see if one matches
        Iterator phoneNumbersIterator = phoneNumbers.iterator();
        while (phoneNumbersIterator.hasNext()) {
            GenericValue phoneNumberContactMech = (GenericValue) phoneNumbersIterator.next();
            contactMechId = phoneNumberContactMech.getString("contactMechId");
            GenericValue phoneNumber;
            try {
                // get the phone number for this contact mech
                phoneNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));

                // now compare values.  If one matches, that's our phone number.  Return the related contact mech id.
                if (parameters.get("shippingAddressPhone").toString().equals((phoneNumber.get("contactNumber").toString()))) {
                    return contactMechId;
                }
            } catch (Exception e) {
                Debug.logError("Problem with verifying phone number for contactMechId " + contactMechId + ".", module);
            }
        }
        // none of the existing contact mechs/email addresses match (or none were found).  Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createPartyPhone(dispatcher, partyId, (String) parameters.get("shippingAddressPhone"), userLogin);
    }

    private static String retrieveProductIdFromTitle(GenericDelegator delegator, String title) {
        String productId = "";
        try {
            // First try to get an exact match: title == internalName
            List products = delegator.findByAnd("Product", UtilMisc.toMap("internalName", title));
            if (UtilValidate.isNotEmpty(products) && products.size() == 1) {
                productId = (String) ((GenericValue)products.get(0)).get("productId");
            }
            // If it fails, attempt to get the product id from the first word of the title
            if (UtilValidate.isEmpty(productId)) {
                String titleFirstWord = null;
                if (title != null && title.indexOf(' ') != -1) {
                    titleFirstWord = title.substring(0, title.indexOf(' '));
                }
                if (UtilValidate.isNotEmpty(titleFirstWord)) {
                    GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", titleFirstWord));
                    if (UtilValidate.isNotEmpty(product)) {
                        productId = product.getString("productId");
                    }
                }
            }
        } catch (GenericEntityException e) {
            productId = "";
        }
        return productId;
    }
}