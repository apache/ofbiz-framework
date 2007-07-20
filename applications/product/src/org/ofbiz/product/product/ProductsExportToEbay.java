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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastMap;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductsExportToEbay {
    
    private static final String resource = "ProductUiLabels";
    private static final String module = ProductsExportToEbay.class.getName();
    private static final String xmlHeader = "<?xml version=\'1.0\' encoding='UTF-8'?>\n";

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
            
            // get the Rest Token
            String restToken = UtilProperties.getPropertyValue(configString, "productsExport.eBay.restToken");
            
            // get the xmlGatewayUri
            String xmlGatewayUri = UtilProperties.getPropertyValue(configString, "productsExport.eBay.xmlGatewayUri");
            
            StringBuffer dataItemsXml = new StringBuffer();
            
            if (!ServiceUtil.isFailure(buildSellerTransactionsXml(context, dataItemsXml, token))) { 
                Map result = postItem(xmlGatewayUri, dataItemsXml, devID, appID, certID, "GetSellerTransactions");
                Debug.logInfo(result.toString(), module);
                /*
                if (ServiceUtil.isFailure(result)) { 
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
                } else {
                    Debug.logError("Error during authentication to eBay Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToeBay.errorDuringAuthenticationToeBay", locale));
                } 
                */   
            }
            
            dataItemsXml.replace(0, dataItemsXml.length(), "");
            if (!ServiceUtil.isFailure(buildDataItemsXml(dctx, context, dataItemsXml, token))) { 
                Map result = postItem(xmlGatewayUri, dataItemsXml, devID, appID, certID, "AddItem");
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
    
    private static Map postItem(String postItemsUrl, StringBuffer dataItems, String devID, String appID, String certID, String callName) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)(new URL(postItemsUrl)).openConnection();
      
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-EBAY-API-COMPATIBILITY-LEVEL", "517");
        connection.setRequestProperty("X-EBAY-API-DEV-NAME", devID);
        connection.setRequestProperty("X-EBAY-API-APP-NAME", appID);
        connection.setRequestProperty("X-EBAY-API-CERT-NAME", certID);
        connection.setRequestProperty("X-EBAY-API-CALL-NAME", callName);
        connection.setRequestProperty("X-EBAY-API-SITEID", "0");
        connection.setRequestProperty("Content-Type", "text/xml");
        
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(dataItems.toString().getBytes());
        outputStream.close();
    
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        Map result = FastMap.newInstance();
        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            inputStream = connection.getInputStream();
            result = ServiceUtil.returnSuccess(toString(inputStream));
            Debug.logInfo(toString(inputStream), module);
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        }
        return result;
    }
    
    private static Map buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
             GenericDelegator delegator = dctx.getDelegator();
             LocalDispatcher dispatcher = dctx.getDispatcher();
             List selectResult = (List)context.get("selectResult");
             String webSiteUrl = (String)context.get("webSiteUrl");
             String imageUrl = (String)context.get("imageUrl");
             String trackingCodeId = (String)context.get("trackingCodeId");
             
             // Get the list of products to be exported to eBay
             List productsList  = delegator.findByCondition("Product", new EntityExpr("productId", EntityOperator.IN, selectResult), null, null);
             
             // Get the tracking code
             if (UtilValidate.isEmpty(trackingCodeId) || "_NA_".equals(trackingCodeId)) {
                 trackingCodeId = "";
             } else {
                 trackingCodeId = "?atc=" + trackingCodeId;
             }
             
             try {
                 Document itemDocument = UtilXml.makeEmptyXmlDocument("AddItemRequest");
                 Element itemRequestElem = itemDocument.getDocumentElement();
                 itemRequestElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
                 
                 dataItemsXml.append(xmlHeader);
                 
                 // Iterate the product list getting all the relevant data
                 Iterator productsListItr = productsList.iterator();
                 while(productsListItr.hasNext()) {
                     GenericValue prod = (GenericValue)productsListItr.next();
                     String link = webSiteUrl + "/control/product/~product_id=" + prod.getString("productId") + trackingCodeId;
                     String title = parseText(prod.getString("productName"));
                     String description = parseText(prod.getString("description"));
                     String image_link = "";
                     if (UtilValidate.isNotEmpty(prod.getString("largeImageUrl"))) {
                         image_link = imageUrl + prod.getString("largeImageUrl");
                     }
                     
                     Element requesterCredentialsElem = UtilXml.addChildElement(itemRequestElem, "RequesterCredentials", itemDocument);
                     UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, itemDocument);
                     
                     Element itemElem = UtilXml.addChildElement(itemRequestElem, "Item", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Country", "US", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Location", "New York", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Currency", "USD", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "SKU", prod.getString("productId"), itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Title", title, itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Description", title, itemDocument);
                     UtilXml.addChildElementValue(itemElem, "ListingDuration", "Days_1", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "Quantity", "3", itemDocument);
                     UtilXml.addChildElementValue(itemElem, "PaymentMethods", "AmEx", itemDocument);
                     
                     Element primaryCatElem = UtilXml.addChildElement(itemElem, "PrimaryCategory", itemDocument);
                     UtilXml.addChildElementValue(primaryCatElem, "CategoryID", "20118", itemDocument);
                     
                     Element startPriceElem = UtilXml.addChildElementValue(itemElem, "StartPrice", "1.00", itemDocument);
                     startPriceElem.setAttribute("currencyID", "USD");
                 }
                 OutputStream os = new ByteArrayOutputStream();
                 OutputFormat format = new OutputFormat();
                 format.setOmitDocumentType(true);
                 format.setOmitXMLDeclaration(true);
                 format.setIndenting(false);
                 XMLSerializer serializer = new XMLSerializer(os, format);
                 serializer.asDOMSerializer();
                 serializer.serialize(itemDocument.getDocumentElement());
                 
                 dataItemsXml.append(os.toString());
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
             
             dataItemsXml.append(xmlHeader);
             Element requesterCredentialsElem = UtilXml.addChildElement(itemRequestElem, "RequesterCredentials", itemRequest);
             UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, itemRequest);
             
             UtilXml.addChildElementValue(itemRequestElem, "DetailLevel", "ReturnAll", itemRequest);
             UtilXml.addChildElementValue(itemRequestElem, "CategorySiteID", "0", itemRequest);
             UtilXml.addChildElementValue(itemRequestElem, "LevelLimit", "2", itemRequest);
             UtilXml.addChildElementValue(itemRequestElem, "ViewAllNodes", "false", itemRequest);
             
             OutputStream os = new ByteArrayOutputStream();
             OutputFormat format = new OutputFormat();
             format.setOmitDocumentType(true);
             format.setOmitXMLDeclaration(true);
             format.setIndenting(false);
             XMLSerializer serializer = new XMLSerializer(os, format);
             serializer.asDOMSerializer();
             serializer.serialize(itemRequest.getDocumentElement());
             
             dataItemsXml.append(os.toString());
         } catch (Exception e) {
             Debug.logError("Exception during building data items to eBay", module);
              return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
         }
         return ServiceUtil.returnSuccess();
    }
    
    private static Map buildSellerTransactionsXml(Map context, StringBuffer dataItemsXml, String token) {
        Locale locale = (Locale)context.get("locale");
        try {
             Document transDoc = UtilXml.makeEmptyXmlDocument("GetSellerTransactionsRequest");
             Element transElem = transDoc.getDocumentElement();
             transElem.setAttribute("xmlns", "urn:ebay:apis:eBLBaseComponents");
             
             dataItemsXml.append(xmlHeader);
             
             Element requesterCredentialsElem = UtilXml.addChildElement(transElem, "RequesterCredentials", transDoc);
             UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, transDoc);
             Timestamp end = new Timestamp(System.currentTimeMillis());
             Timestamp start = UtilDateTime.getDayEnd(end, -1);
             
             UtilXml.addChildElementValue(transElem, "DetailLevel", "ReturnAll", transDoc);
             UtilXml.addChildElementValue(transElem, "ModTimeFrom", start.toString(), transDoc);
             UtilXml.addChildElementValue(transElem, "ModTimeTo", end.toString(), transDoc);
             
             OutputStream os = new ByteArrayOutputStream();
             OutputFormat format = new OutputFormat();
             format.setOmitDocumentType(true);
             format.setOmitXMLDeclaration(true);
             format.setIndenting(false);
             XMLSerializer serializer = new XMLSerializer(os, format);
             serializer.asDOMSerializer();
             serializer.serialize(transDoc.getDocumentElement());
             
             dataItemsXml.append(os.toString());
         } catch (Exception e) {
             Debug.logError("Exception during building data items to eBay", module);
              return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToEbay.exceptionDuringBuildingDataItemsToEbay", locale));
         }
         return ServiceUtil.returnSuccess();
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