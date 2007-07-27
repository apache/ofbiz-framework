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
package org.ofbiz.product.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductsExportToEbay {
    
    private static final String resource = "ProductUiLabels";
    private static final String module = ProductsExportToEbay.class.getName();
    
    public static Map exportToEbay(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");
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
             
            StringBuffer dataItemsXml = new StringBuffer();
            
            /*
            String itemId = "";
            if (!ServiceUtil.isFailure(buildAddTransactionConfirmationItemRequest(context, dataItemsXml, token,  itemId))) { 
                Map result = postItem(xmlGatewayUri, dataItemsXml, devID, appID, certID, "AddTransactionConfirmationItem");
                Debug.logInfo(result.toString(), module);
            }
            */
            
            if (!ServiceUtil.isFailure(buildDataItemsXml(dctx, context, dataItemsXml, token))) { 
                Map result = postItem(xmlGatewayUri, dataItemsXml, devID, appID, certID, "AddItem", compatibilityLevel, siteID);
                if (ServiceUtil.isFailure(result)) { 
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
                } else {
                    Debug.logError("Error during authentication to eBay Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.errorDuringAuthenticationToEbay", locale));
                }            
            }
        } catch (Exception e) {        
            Debug.logError("Exception in exportToEbay " + e, module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionInExportToEbay", locale));
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "productsExportToEbay.productItemsSentCorrecltyToEbay", locale));
    }
   
    private static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
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
        InputStream inputStream;
        Map result = FastMap.newInstance();
        String response = null;
        
        if (responseCode == HttpURLConnection.HTTP_CREATED ||
            responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = toString(inputStream);
            result = ServiceUtil.returnSuccess(response);
        } else {
            inputStream = connection.getErrorStream();
            response = toString(inputStream);
            result = ServiceUtil.returnFailure(response);
        }
        
        if (Debug.verboseOn()) {
            Debug.logVerbose("Response of " + callName + " From eBay:\n" + response, module);
        }
        
        return result;
    }
    
    private static Map buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
            GenericDelegator delegator = dctx.getDelegator();
            List selectResult = (List)context.get("selectResult");
             
            // Get the list of products to be exported to eBay
            List productsList  = delegator.findByCondition("Product", new EntityExpr("productId", EntityOperator.IN, selectResult), null, null);
             
            try {
                Document itemDocument = UtilXml.makeEmptyXmlDocument("AddItemRequest");
                Element itemRequestElem = itemDocument.getDocumentElement();
                itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
                 
                appendRequesterCredentials(itemRequestElem, itemDocument, token);
                 
                // Iterate the product list getting all the relevant data
                Iterator productsListItr = productsList.iterator();
                while(productsListItr.hasNext()) {
                    GenericValue prod = (GenericValue)productsListItr.next();
                    String title = parseText(prod.getString("productName"));
                    String description = parseText(prod.getString("description"));
                     
                    Element itemElem = UtilXml.addChildElement(itemRequestElem, "Item", itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Country", (String)context.get("country"), itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Location", (String)context.get("location"), itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Currency", "USD", itemDocument);
                    UtilXml.addChildElementValue(itemElem, "SKU", prod.getString("productId"), itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Title", title, itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Description", description, itemDocument);
                    UtilXml.addChildElementValue(itemElem, "ListingDuration", (String)context.get("listingDuration"), itemDocument);
                    UtilXml.addChildElementValue(itemElem, "Quantity", (String)context.get("quantity"), itemDocument);
                     
                    setPaymentMethodAccepted(itemDocument, itemElem, context);
                     
                    Element primaryCatElem = UtilXml.addChildElement(itemElem, "PrimaryCategory", itemDocument);
                    UtilXml.addChildElementValue(primaryCatElem, "CategoryID", (String)context.get("ebayCategory"), itemDocument);
                     
                    Element startPriceElem = UtilXml.addChildElementValue(itemElem, "StartPrice", (String)context.get("startPrice"), itemDocument);
                    startPriceElem.setAttribute("currencyID", "USD");
                }
                 
                dataItemsXml.append(UtilXml.writeXmlDocument(itemDocument));
            } catch (Exception e) {
                Debug.logError("Exception during building data items to eBay", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
            }
        } catch (Exception e) {
            Debug.logError("Exception during building data items to eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        } 
        return ServiceUtil.returnSuccess();
    }
    
    private static Map buildCategoriesXml(Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
            Document itemRequest = UtilXml.makeEmptyXmlDocument("GetCategoriesRequest");
            Element itemRequestElem = itemRequest.getDocumentElement();
            itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
             
            appendRequesterCredentials(itemRequestElem, itemRequest, token);
             
            UtilXml.addChildElementValue(itemRequestElem, "DetailLevel", "ReturnAll", itemRequest);
            UtilXml.addChildElementValue(itemRequestElem, "CategorySiteID", "0", itemRequest);
            UtilXml.addChildElementValue(itemRequestElem, "LevelLimit", "2", itemRequest);
            UtilXml.addChildElementValue(itemRequestElem, "ViewAllNodes", "false", itemRequest);
             
            dataItemsXml.append(UtilXml.writeXmlDocument(itemRequest));
        } catch (Exception e) {
            Debug.logError("Exception during building data items to eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    
    private static Map buildAddTransactionConfirmationItemRequest(Map context, StringBuffer dataItemsXml, String token, String itemId) {
        Locale locale = (Locale)context.get("locale");
        try {
            Document transDoc = UtilXml.makeEmptyXmlDocument("AddTransactionConfirmationItemRequest");
            Element transElem = transDoc.getDocumentElement();
            transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
             
            appendRequesterCredentials(transElem, transDoc, token);
             
            UtilXml.addChildElementValue(transElem, "ItemID", itemId, transDoc);
            UtilXml.addChildElementValue(transElem, "ListingDuration", "Days_1", transDoc);
            Element negotiatePriceElem = UtilXml.addChildElementValue(transElem, "NegotiatedPrice", "50.00", transDoc);
            negotiatePriceElem.setAttribute("currencyID", "USD");
            UtilXml.addChildElementValue(transElem, "RecipientRelationType", "1", transDoc);
            UtilXml.addChildElementValue(transElem, "RecipientUserID", "buyer_anytime", transDoc);
             
            dataItemsXml.append(UtilXml.writeXmlDocument(transDoc));
            Debug.logInfo(dataItemsXml.toString(), module);
        } catch (Exception e) {
            Debug.logError("Exception during building AddTransactionConfirmationItemRequest eBay", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingAddTransactionConfirmationItemRequestToEbay", locale));
        }
        return ServiceUtil.returnSuccess();
    }
    
    private static void setPaymentMethodAccepted(Document itemDocument, Element itemElem, Map context) {
        String payPal = (String)context.get("paymentPayPal");
        String visaMC = (String)context.get("paymentVisaMC");
        String amEx = (String)context.get("paymentAmEx");
        String discover = (String)context.get("paymentDiscover");
        String ccAccepted = (String)context.get("paymentCCAccepted");
        String cashInPerson = (String)context.get("paymentCashInPerson");
        String cashOnPickup = (String)context.get("paymentCashOnPickup");
        String cod = (String)context.get("paymentCOD");
        String codPrePayDelivery = (String)context.get("paymentCODPrePayDelivery");
        String mocc = (String)context.get("paymentMOCC");
        String moneyXferAccepted = (String)context.get("paymentMoneyXferAccepted");
        String personalCheck = (String)context.get("paymentPersonalCheck");
        
        // PayPal
        if (UtilValidate.isNotEmpty(payPal) && "Y".equals(payPal)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "PayPal", itemDocument);
            UtilXml.addChildElementValue(itemElem, "PayPalEmailAddress", (String)context.get("payPalEmail"), itemDocument);
        }
        // Visa/Master Card
        if (UtilValidate.isNotEmpty(visaMC) && "Y".equals(visaMC)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "VisaMC", itemDocument);
        }
        // American Express
        if (UtilValidate.isNotEmpty(amEx) && "Y".equals(amEx)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "AmEx", itemDocument);
        }
        // Discover
        if (UtilValidate.isNotEmpty(discover) && "Y".equals(discover)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "Discover", itemDocument);
        }
        // Credit Card Accepted
        if (UtilValidate.isNotEmpty(ccAccepted) && "Y".equals(ccAccepted)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CCAccepted", itemDocument);
        }
        // Cash In Person
        if (UtilValidate.isNotEmpty(cashInPerson) && "Y".equals(cashInPerson)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CashInPerson", itemDocument);
        }
        // Cash on Pickup
        if (UtilValidate.isNotEmpty(cashOnPickup) && "Y".equals(cashOnPickup)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CashOnPickup", itemDocument);
        }
        // Cash on Delivery
        if (UtilValidate.isNotEmpty(cod) && "Y".equals(cod)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "COD", itemDocument);
        }
        // Cash On Delivery After Paid
        if (UtilValidate.isNotEmpty(codPrePayDelivery) && "Y".equals(codPrePayDelivery)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "CODPrePayDelivery", itemDocument);
        }
        // Money order/cashiers check
        if (UtilValidate.isNotEmpty(mocc) && "Y".equals(mocc)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "MOCC", itemDocument);
        }
        // Direct transfer of money
        if (UtilValidate.isNotEmpty(moneyXferAccepted) && "Y".equals(moneyXferAccepted)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "MoneyXferAccepted", itemDocument);
        }
        // Personal Check
        if (UtilValidate.isNotEmpty(personalCheck) && "Y".equals(personalCheck)) {
            UtilXml.addChildElementValue(itemElem, "PaymentMethods", "PersonalCheck", itemDocument);
        }
    }
    
    private static String parseText(String text) {
        Pattern htmlPattern = Pattern.compile("[<](.+?)[>]");
        Pattern tabPattern = Pattern.compile("\\s");
        if (null != text && text.length() > 0){
            Matcher matcher = htmlPattern.matcher(text);
            text = matcher.replaceAll("");
            matcher = tabPattern.matcher(text);
            text = matcher.replaceAll(" ");
        } else {
            text = "";
        }
        return text;
    }
}