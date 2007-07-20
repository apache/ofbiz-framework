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
package org.ofbiz.order.order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImportOrdersFromEbay {
    
    private static final String resource = "OrderUiLabels";
    private static final String module = ImportOrdersFromEbay.class.getName();
    
    public static Map importOrdersSearchFromEbay(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map result = FastMap.newInstance();
        try {
            String configString = "productsExport.properties";
                            
            // get the Developer Key
            String devID = UtilProperties.getPropertyValue(configString, "productsExport.eBay.devID");
            
            // get the Application Key
            String appID = UtilProperties.getPropertyValue(configString, "productsExport.eBay.appID");
            
            // get the Certifcate Key
            String certID = UtilProperties.getPropertyValue(configString, "productsExport.eBay.certID");
            
            // get the Token
            String token = UtilProperties.getPropertyValue(configString, "productsExport.eBay.token");
            
            // get the Compatibility Level
            String compatibilityLevel = UtilProperties.getPropertyValue(configString, "productsExport.eBay.compatibilityLevel");
            
            // get the Site ID
            String siteID = UtilProperties.getPropertyValue(configString, "productsExport.eBay.siteID");
            
            // get the xmlGatewayUri
            String xmlGatewayUri = UtilProperties.getPropertyValue(configString, "productsExport.eBay.xmlGatewayUri");
            
            StringBuffer ebayDetailsItemsXml = new StringBuffer();
            StringBuffer sellerTransactionsItemsXml = new StringBuffer();
            
            if (!ServiceUtil.isFailure(buildGetEbayDetailsRequest(context, ebayDetailsItemsXml, token))) { 
                postItem(xmlGatewayUri, ebayDetailsItemsXml, devID, appID, certID, "GeteBayDetails", compatibilityLevel, siteID);
                
                if (!ServiceUtil.isFailure(buildGetSellerTransactionsRequest(context, sellerTransactionsItemsXml, token))) { 
                    result = postItem(xmlGatewayUri, sellerTransactionsItemsXml, devID, appID, certID, "GetSellerTransactions", compatibilityLevel, siteID);
                    String success = (String)result.get(ModelService.SUCCESS_MESSAGE);
                    if (success != null) { 
                        result = checkOrders(delegator, dispatcher, locale, context, success);
                    }
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
            String orderSelected = (String) context.get("orderSelected");
            if (UtilValidate.isNotEmpty(orderSelected) && "Y".equals(orderSelected)) {
                order.put("productStoreId", (String) context.get("productStoreId"));
                order.put("userLogin", (GenericValue) context.get("userLogin"));
                order.put("externalId", (String) context.get("externalId"));
                order.put("createdDate", (String) context.get("createdDate"));
                order.put("productId", (String) context.get("productId"));
                order.put("quantityPurchased", (String) context.get("quantityPurchased"));
                order.put("transactionPrice", (String) context.get("transactionPrice"));
                order.put("shippingServiceCost", (String) context.get("shippingServiceCost"));
                order.put("shippingTotalAdditionalCost", (String) context.get("shippingTotalAdditionalCost"));
                order.put("amountPaid", (String) context.get("amountPaid"));
                order.put("paidTime", (String) context.get("paidTime"));
                order.put("shippedTime", (String) context.get("shippedTime"));
                
                order.put("buyerName", (String) context.get("buyerName"));
                order.put("emailBuyer", (String) context.get("emailBuyer"));
                order.put("shippingAddressPhone", (String) context.get("shippingAddressPhone"));
                order.put("shippingAddressStreet1", (String) context.get("shippingAddressStreet1"));
                order.put("shippingAddressPostalCode", (String) context.get("shippingAddressPostalCode"));
                order.put("shippingAddressCountry", (String) context.get("shippingAddressCountry"));
                order.put("shippingAddressStateOrProvince", (String) context.get("shippingAddressStateOrProvince"));
                order.put("shippingAddressCityName", (String) context.get("shippingAddressCityName"));
                
                result = createShoppingCart(delegator, dispatcher, locale, order, Boolean.TRUE);
            }
        } catch (Exception e) {        
            Debug.logError("Exception in importOrderFromEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInImportOrderFromEbay", locale));
        }
        
        return result;
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
        if (responseCode == HttpURLConnection.HTTP_CREATED ||
            responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            String response = toString(inputStream);
            result = ServiceUtil.returnSuccess(response);
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        }
        return result;
    }
    
    private static Map checkOrders(GenericDelegator delegator, LocalDispatcher dispatcher, Locale locale, Map context, String response) {
        if (response != null && response.length() > 0) {
            List orders = readResponseFromEbay(response, locale, (String)context.get("productStoreId"), delegator);
            if (orders != null && orders.size() > 0) {
                Iterator orderIter = orders.iterator();
                while (orderIter.hasNext()) {
                    Map order = (Map)orderIter.next();
                    order.put("productStoreId", (String) context.get("productStoreId"));
                    order.put("userLogin", (GenericValue) context.get("userLogin"));
                    Map error = createShoppingCart(delegator, dispatcher, locale, order, Boolean.FALSE);
                    String errorMsg = ServiceUtil.getErrorMessage(error);
                    if (errorMsg != null) {
                        order.put("errorMessage", errorMsg);
                    }
                }
                Map result = FastMap.newInstance();
                result.put("responseMessage", ModelService.RESPOND_SUCCESS);
                result.put("orderList", orders);
                return result;
            } else {
                Debug.logError("No orders found", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
            }
        } else {
            Debug.logError("No orders found", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.noOrdersFound", locale));
        }
    }
        
    private static Map buildGetSellerTransactionsRequest(Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        Timestamp thruDate = (Timestamp)context.get("thruDate");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetSellerTransactionsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
              
             appendRequesterCredentials(transElem, transDoc, token);
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);
             UtilXml.addChildElementValue(transElem, "ModTimeFrom", fromDate.toString(), transDoc);
             UtilXml.addChildElementValue(transElem, "ModTimeTo", thruDate.toString(), transDoc);
             
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
    
    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
    }
    
    private static List readResponseFromEbay(String msg, Locale locale, String productStoreId, GenericDelegator delegator) {
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
            
            if (ack != null && ack.equals("Success") && totalOrders    > 0) {
                orders = new ArrayList();
                
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
                                    order.put("shippingAddressStreet1", UtilXml.childElementValue(shippingAddressElement, "Street1", ""));
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
                            order.put("productId", UtilXml.childElementValue(itemElement, "SKU", ""));
                            order.put("startPrice", UtilXml.childElementValue(itemElement, "StartPrice", "0"));
                            order.put("title", UtilXml.childElementValue(itemElement, "Title", ""));
                            
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
                        
                        // build the externalId                        
                        String externalId = buildExternalId(itemId, transactionId); 
                        order.put("externalId", externalId);
                        
                        GenericValue orderExist = externalOrderExists(delegator, externalId);
                        if (orderExist != null) {
                            order.put("orderId", (String)orderExist.get("orderId"));   
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
            
            // set the external id with the eBay Item Id + eBay Transacation Id
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
            Double amountPaid = new Double(0);
            
            if (UtilValidate.isNotEmpty(amountStr)) {
                amountPaid = new Double(amountStr);
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
            
            // create the shipment group item
            cart.addItemGroup("00001", null);
            
            // create the order item
            String productId = (String) parameters.get("productId");
            if (UtilValidate.isEmpty(productId)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productIdNotAvailable", locale));
            }                
            
            // Before import the order from eBay to OFBiz is mandatory that the payment has be received
            String paidTime = (String) parameters.get("paidTime");
            if (UtilValidate.isEmpty(paidTime)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            } 
            
            Double unitPrice = new Double((String) parameters.get("transactionPrice"));
            double quantity = new Double((String) parameters.get("quantityPurchased")).doubleValue();
            Double amount = new Double(quantity * unitPrice.doubleValue());
            cart.addItemToEnd(productId, amount, quantity, unitPrice, null, null, null, "PRODUCT_ORDER_ITEM", dispatcher, Boolean.FALSE, Boolean.FALSE);
           
            // set partyId from
            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }
            
            // Apply shipping costs as order adjustment
            String shippingCost = (String) parameters.get("shippingServiceCost");
            if (UtilValidate.isNotEmpty(shippingCost)) {
                double shippingAmount = new Double(shippingCost).doubleValue();
                if (shippingAmount > 0) {
                    GenericValue shippingAdjustment = madeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount);       
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
                    GenericValue shippingAdjustment = madeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost);       
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }
            
            // order has to be created ?
            if (create) {
                // set partyId to
                String partyId = createCustomerParty(dispatcher, (String) parameters.get("buyerName"), userLogin);
                String contactMechId = "";            
                if (UtilValidate.isNotEmpty(partyId)) {
                    contactMechId = createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", parameters);
                    createPartyPhone(dispatcher, partyId, (String) parameters.get("shippingAddressPhone"), userLogin);
                    String emailBuyer = (String) parameters.get("emailBuyer");
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                    }
                } else {
                    partyId = "admin";
                }
                
                cart.setBillToCustomerPartyId(partyId);
                cart.setPlacingCustomerPartyId(partyId);
                cart.setShipToCustomerPartyId(partyId);
                cart.setEndUserCustomerPartyId(partyId);
                
                cart.setCarrierPartyId("_NA_");
                cart.setShippingContactMechId(contactMechId);
                
                //TODO handle shipment method type
                cart.setShipmentMethodTypeId("NO_SHIPPING");
                cart.setMaySplit(false);
                cart.makeAllShipGroupInfos();
                  
                // create the order
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Map orderCreate = checkout.createOrder(userLogin);
                
                String orderId = (String)orderCreate.get("orderId");
                
                // approve the order
                if (UtilValidate.isNotEmpty(orderId)) {
                    boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                    
                    // create the payment from the preference
                    if (approved) {
                        createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), partyId);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception in createShoppingCart", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.exceptionInCreateShoppingCart", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    
    private static boolean createPaymentFromPaymentPreferences(GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, 
                                                               String orderId, String externalId, Timestamp orderDate, String partyIdFrom) {
        List paymentPreferences = null;
        try {
            Map paymentFields = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_RECEIVED", "paymentMethodTypeId", "EXT_EBAY");
            paymentPreferences = delegator.findByAnd("OrderPaymentPreference", paymentFields);
            
            if (paymentPreferences != null && paymentPreferences.size() > 0) {
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
            GenericValue response = delegator.makeValue("PaymentGatewayResponse", null);
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
    
    public static GenericValue madeOrderAdjustment(GenericDelegator delegator, String orderAdjustmentTypeId, String orderId, String orderItemSeqId, String shipGroupSeqId, double amount) {
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
            orderAdjustment = delegator.makeValue("OrderAdjustment", inputMap);
        } catch (Exception e) {
            Debug.logError(e, "Failed to made order adjustment for order " + orderId, module);
        }
        return orderAdjustment;
    }

    public static String createCustomerParty(LocalDispatcher dispatcher, String name, GenericValue userLogin)
    {
        String partyId = null;
        
        try {
            if (UtilValidate.isNotEmpty(name) && UtilValidate.isNotEmpty(userLogin) ) {
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
                    
                Map summaryResult = dispatcher.runSync("createPerson", UtilMisc.toMap("description", name, "firstName", firstName, "lastName", lastName, 
                                                                                      "userLogin", userLogin, "comments", "Created via eBay"));                                         
                partyId = (String) summaryResult.get("partyId");   
                Debug.logVerbose("Created Customer Party: "+partyId, module);
            }
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPerson", module);
        }
        return partyId;
    }
    
    public static String createAddress(LocalDispatcher dispatcher, String partyId, GenericValue userLogin, 
                                       String contactMechPurposeTypeId, Map address)
    {
        String contactMechId = null;
        try {
            Map context = FastMap.newInstance();
            context.put("partyId", partyId);
            context.put("address1", (String)address.get("shippingAddressStreet1"));
            context.put("postalCode", (String)address.get("shippingAddressPostalCode"));
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", contactMechPurposeTypeId);       
            
            String country = ((String)address.get("shippingAddressCountry")).toUpperCase();
            String state = ((String)address.get("shippingAddressStateOrProvince")).toUpperCase();
            String city = (String)address.get("shippingAddressCityName");
            correctCityStateCountry(dispatcher, context, city, state, country);

            Map summaryResult = dispatcher.runSync("createPartyPostalAddress",context);
            contactMechId = (String)summaryResult.get("contactMechId");
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createAddress", module);
        }
        return contactMechId;
    }
    
    private static void correctCityStateCountry(LocalDispatcher dispatcher, Map map, String city, String state, String country)
    {
        try { 
            Debug.logInfo("correctCityStateCountry params: " + city + ", " + state + ", " + country, module);
            String geoId = "USA";
            if (country.indexOf("UNITED STATES") > -1 || country.indexOf("USA") > -1)
                country = "US";
            Debug.logInfo("GeoCode: " + country, module);
            Map outMap = getCountryGeoId(dispatcher.getDelegator(), country);
            geoId = (String)outMap.get("geoId");
            map.put("countryGeoId", geoId);
            Debug.logInfo("Country geoid: " + geoId, module);
            if (geoId.equals("USA") || geoId.equals("CAN")) {
                if (state.length() > 0) {
                    map.put("stateProvinceGeoId", state);
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
        
    public static String createPartyPhone(LocalDispatcher dispatcher, String partyId, String phoneNumber, GenericValue userLogin)
    {
        Map summaryResult = FastMap.newInstance();
        Map context = FastMap.newInstance();
        String phoneContactMechId = null;
        
        try { 
            context.put("contactNumber", phoneNumber);                   
            context.put("partyId", partyId);
            context.put("userLogin", userLogin);
            summaryResult = dispatcher.runSync("createPartyTelecomNumber", context);
            phoneContactMechId = (String)summaryResult.get("contactMechId");
        } catch (Exception e) { 
            Debug.logError(e, "Failed to createPartyPhone", module);
        }
        return phoneContactMechId;
    }
    
    public static String createPartyEmail(LocalDispatcher dispatcher, String partyId, String email, GenericValue userLogin)
    {
        Map context = FastMap.newInstance();
        Map summaryResult = FastMap.newInstance();
        String emailContactMechId = null;
        
        try {
            if (UtilValidate.isNotEmpty(email))
            {
                context.clear();
                context.put("emailAddress", email);
                context.put("userLogin", userLogin);
                context.put("contactMechTypeId", "EMAIL_ADDRESS");                                      
                summaryResult = dispatcher.runSync("createEmailAddress", context);
                emailContactMechId = (String) summaryResult.get("contactMechId");
                
                context.clear();
                context.put("partyId", partyId);
                context.put("contactMechId", emailContactMechId);
                context.put("userLogin", userLogin);                 
                summaryResult = dispatcher.runSync("createPartyContactMech", context);
            }
        } catch (Exception e) { 
            Debug.logError(e, "Failed to createPartyEmail", module);
        }
        return emailContactMechId;
    }
    
    public static Map getCountryGeoId(GenericDelegator delegator, String geoCode)
    {
        GenericValue geo = null;
        try {
            Debug.logInfo("geocode: " + geoCode, module);
            
            List geoEntities = delegator.findByAnd("Geo", UtilMisc.toMap("geoCode", geoCode.toUpperCase(), "geoTypeId", "COUNTRY"));
            if (geoEntities != null && geoEntities.size() > 0)
            {
                geo = (GenericValue)geoEntities.get(0);
                Debug.logInfo("Found a geo entity " + geo, module);
            }
            else
            {
                geo = delegator.makeValue("Geo", null);
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
    
    public static GenericValue externalOrderExists(GenericDelegator delegator, String externalId) throws GenericEntityException
    {
        Debug.logInfo("Checking for existing externalOrderId: " + externalId, module);
        GenericValue orderHeader = null;
        List entities = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId));
        if (entities != null && entities.size() > 0) {
            orderHeader = EntityUtil.getFirst(entities);
        }
        return orderHeader;
    } 
    
    public static String buildExternalId(String itemId, String transactionId)
    {
        StringBuffer str = new StringBuffer();
        if (itemId != null) {
            str.append(itemId);
        }
        
        if (transactionId != null) {
            if (!("0".equals(transactionId))) {
                str.append("_");
            }           
            str.append(transactionId);
        }
        return str.toString();
    }
}