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
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
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

public class ProductsExportToGoogle {
    
    private static final String resource = "ProductUiLabels";
    private static final String module = ProductsExportToGoogle.class.getName();

    public static Map exportToGoogle(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");
        try {
            String configString = "productsExport.properties";
                            
            // get the Developer Key
            String developerKey = UtilProperties.getPropertyValue(configString, "productsExport.google.developerKey");
            
            // get the Authentication Url
            String authenticationUrl = UtilProperties.getPropertyValue(configString, "productsExport.google.authenticationUrl");
            
            // get the Google Account Email
            String accountEmail = UtilProperties.getPropertyValue(configString, "productsExport.google.accountEmail");
            
            // get the Google Account Password
            String accountPassword = UtilProperties.getPropertyValue(configString, "productsExport.google.accountPassword");
            
            // get the Url to Post Items
            String postItemsUrl = UtilProperties.getPropertyValue(configString, "productsExport.google.postItemsUrl");
        
            StringBuffer dataItemsXml = new StringBuffer();
            
            Map result = buildDataItemsXml(dctx, context, dataItemsXml);
            if (!ServiceUtil.isFailure(result)) { 
                String token = authenticate(authenticationUrl, accountEmail, accountPassword);
                if (token != null) {    
                    result = postItem(token, postItemsUrl, developerKey, dataItemsXml, locale, (String)context.get("testMode"));
                    String msg = ServiceUtil.getErrorMessage(result);
                    if (msg != null && msg.length() > 0) {
                        return ServiceUtil.returnFailure(msg);
                    }
                } else {
                    Debug.logError("Error during authentication to Google Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.errorDuringAuthenticationToGoogle", locale));
                }
            } else {
                return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
            }
        } catch (Exception e) {        
            Debug.logError("Exception in exportToGoogle", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionInExportToGoogle", locale));
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "productsExportToGoogle.productItemsSentCorrecltyToGoogle", locale));
    }
   
    private static String authenticate(String authenticationUrl, String accountEmail, String accountPassword) {
        String postOutput = null;
        String token = null;
        try {
            postOutput = makeLoginRequest(authenticationUrl, accountEmail, accountPassword);
        } catch (IOException e) {
            Debug.logError("Could not connect to authentication server: " + e.toString(), module);
            return token;
        }

        // Parse the result of the login request. If everything went fine, the 
        // response will look like
        //      HTTP/1.0 200 OK
        //      Server: GFE/1.3
        //      Content-Type: text/plain 
        //      SID=DQAAAGgA...7Zg8CTN
        //      LSID=DQAAAGsA...lk8BBbG
        //      Auth=DQAAAGgA...dk3fA5N
        // so all we need to do is look for "Auth" and get the token that comes after it

        StringTokenizer tokenizer = new StringTokenizer(postOutput, "=\n ");
      
        while (tokenizer.hasMoreElements()) {
            if ("Auth".equals(tokenizer.nextToken())) {
                if (tokenizer.hasMoreElements()) {
                    token = tokenizer.nextToken(); 
                }
                break;
            }
        }
        if (token == null) {
            Debug.logError("Authentication error. Response from server:\n" + postOutput, module);
        }
        return token;
    }

    private static String makeLoginRequest(String authenticationUrl, String accountEmail, String accountPassword) throws IOException {
        // Open connection
        URL url = new URL(authenticationUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      
        // Set properties of the connection
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    
        // Form the POST parameters
        StringBuffer content = new StringBuffer();
        content.append("Email=").append(URLEncoder.encode(accountEmail, "UTF-8"));
        content.append("&Passwd=").append(URLEncoder.encode(accountPassword, "UTF-8"));
        content.append("&source=").append(URLEncoder.encode("Google Base data API for OFBiz", "UTF-8"));
        content.append("&service=").append(URLEncoder.encode("gbase", "UTF-8"));

        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(content.toString().getBytes("UTF-8"));
        outputStream.close();
    
        // Retrieve the output
        int responseCode = urlConnection.getResponseCode();
        InputStream inputStream;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = urlConnection.getInputStream();
        } else {
            inputStream = urlConnection.getErrorStream();
        }
    
        return toString(inputStream);
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
    
    private static Map postItem(String token, String postItemsUrl, String developerKey, StringBuffer dataItems, 
                                Locale locale, String testMode) throws IOException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Request To Google Base :\n" + dataItems.toString(), module);
        }
        
        HttpURLConnection connection = (HttpURLConnection)(new URL(postItemsUrl)).openConnection();
        
        // Test Mode Yes (add the dry-run=true)
        if (UtilValidate.isNotEmpty(testMode) && "Y".equals(testMode)) {
            connection.setRequestProperty("dry-run", "true");
        } 
        
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/atom+xml");
        connection.setRequestProperty("Authorization", "GoogleLogin auth=" + token);
        connection.setRequestProperty("X-Google-Key", "key=" + developerKey);
    
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(dataItems.toString().getBytes());
        outputStream.close();
    
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        Map result = FastMap.newInstance();
        String response = "";
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = toString(inputStream);
            if (response != null && response.length() > 0) {
                result = readResponseFromGoogle(response, locale);
                String msg = ServiceUtil.getErrorMessage(result);
                if (msg != null && msg.length() > 0) {
                    result = ServiceUtil.returnFailure(msg);
                } else {
                    result = ServiceUtil.returnSuccess();
                }
            } 
        } else {
            inputStream = connection.getErrorStream();
            response = toString(inputStream);
            result = ServiceUtil.returnFailure(response);
        }
        
        if (Debug.verboseOn()) {
            Debug.logVerbose("Response From Google Base :\n" + response, module);
        }
        return result;
    }
    
    private static Map buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml) {
        Locale locale = (Locale)context.get("locale");
        try {
             GenericDelegator delegator = dctx.getDelegator();
             LocalDispatcher dispatcher = dctx.getDispatcher();
             List selectResult = (List)context.get("selectResult");
             String webSiteUrl = (String)context.get("webSiteUrl");
             String imageUrl = (String)context.get("imageUrl");
             String actionType = (String)context.get("actionType");
             String statusId = (String)context.get("statusId");
             String trackingCodeId = (String)context.get("trackingCodeId");
             
             // Get the list of products to be exported to Google Base
             List productsList  = delegator.findByCondition("Product", new EntityExpr("productId", EntityOperator.IN, selectResult), null, null);
             
             // Get the tracking code
             if (UtilValidate.isEmpty(trackingCodeId) || "_NA_".equals(trackingCodeId)) {
                 trackingCodeId = "";
             } else {
                 trackingCodeId = "?atc=" + trackingCodeId;
             }
             
             try {
                 Document feedDocument = UtilXml.makeEmptyXmlDocument("feed");
                 Element feedElem = feedDocument.getDocumentElement();
                 feedElem.setAttribute("xmlns", "http://www.w3.org/2005/Atom");
                 feedElem.setAttribute("xmlns:openSearch", "http://a9.com/-/spec/opensearchrss/1.0/");
                 feedElem.setAttribute("xmlns:g", "http://base.google.com/ns/1.0");
                 feedElem.setAttribute("xmlns:batch", "http://schemas.google.com/gdata/batch");
                 
                 // Iterate the product list getting all the relevant data
                 Iterator productsListItr = productsList.iterator();
                 while(productsListItr.hasNext()) {
                     GenericValue prod = (GenericValue)productsListItr.next();
                     String price = getProductPrice(dispatcher, prod);
                     if (price == null) {
                         Debug.logInfo("Price not found for product [" + prod.getString("productId")+ "]; product will not be exported.", module);
                         continue;
                     }
                     String link = webSiteUrl + "/control/product/~product_id=" + prod.getString("productId") + trackingCodeId;
                     String title = UtilFormatOut.encodeXmlValue(prod.getString("productName"));
                     String description = UtilFormatOut.encodeXmlValue(prod.getString("description"));
                     String image_link = "";
                     if (UtilValidate.isNotEmpty(prod.getString("largeImageUrl"))) {
                         image_link = imageUrl + prod.getString("largeImageUrl");
                     }
                     
                     Element entryElem = UtilXml.addChildElement(feedElem, "entry", feedDocument);
                     Element batchElem = UtilXml.addChildElement(entryElem, "batch:operation", feedDocument);
                     batchElem.setAttribute("type", actionType);
                     
                     // status is draft or deactivate
                     if (statusId != null && ("draft".equals(statusId) || "deactivate".equals(statusId))) {
                         Element appControlElem = UtilXml.addChildElement(entryElem, "app:control", feedDocument);
                         appControlElem.setAttribute("xmlns:app", "http://purl.org/atom/app&#35;");
                         UtilXml.addChildElementValue(appControlElem, "app:draft", "yes", feedDocument);
                         
                         // status is deactivate
                         if ("deactivate".equals(statusId)) {
                             UtilXml.addChildElement(appControlElem, "gm:disapproved", feedDocument);
                         }
                     }
                     
                     UtilXml.addChildElementValue(entryElem, "title", title, feedDocument);
                     
                     Element contentElem = UtilXml.addChildElementValue(entryElem, "content", description, feedDocument);
                     contentElem.setAttribute("type", "xhtml");
                     
                     UtilXml.addChildElementValue(entryElem, "id", link, feedDocument);
                     
                     Element linkElem = UtilXml.addChildElement(entryElem, "link", feedDocument);
                     linkElem.setAttribute("rel", "alternate");
                     linkElem.setAttribute("type", "text/html");
                     linkElem.setAttribute("href", link);
                     
                     UtilXml.addChildElementValue(entryElem, "g:item_type", "products", feedDocument);
                     UtilXml.addChildElementValue(entryElem, "g:price", price, feedDocument);
                     
                     // if the product has an image it will be published on Google Product Search
                     if (UtilValidate.isNotEmpty(image_link)) {
                         UtilXml.addChildElementValue(entryElem, "g:image_link", image_link, feedDocument);
                     }
                 }
                 
                 dataItemsXml.append(UtilXml.writeXmlDocument(feedDocument));
             } catch (Exception e) {
                 Debug.logError("Exception during building data items to Google", module);
                 return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionDuringBuildingDataItemsToGoogle", locale));
             }
         } catch (Exception e) {
            Debug.logError("Exception during building data items to Google", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionDuringBuildingDataItemsToGoogle", locale));
         } 
         return ServiceUtil.returnSuccess();
    }

    private static String getProductPrice(LocalDispatcher dispatcher, GenericValue product) {
        String priceString = null;
        try {
            Map map = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product));
            boolean validPriceFound = ((Boolean)map.get("validPriceFound")).booleanValue();
            boolean isSale = ((Boolean)map.get("isSale")).booleanValue();
            if (validPriceFound) {
                priceString = UtilFormatOut.formatPrice((Double)map.get("price"));
            }
        } catch(Exception e){
            Debug.logError("Exception calculating price for product [" + product.getString("productId") + "]", module);
        }
        return priceString;
    }
    
    private static Map readResponseFromGoogle(String msg, Locale locale) {
        StringBuffer message = new StringBuffer();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            List atomEntryList = UtilXml.childElementList(elemResponse, "atom:entry");
            Iterator atomEntryElemIter = atomEntryList.iterator();
            while (atomEntryElemIter.hasNext()) {
                Element atomEntryElement = (Element)atomEntryElemIter.next();
                List batchInterruptedEntryList = UtilXml.childElementList(atomEntryElement, "batch:interrupted");
                Iterator batchInterruptedEntryElemIter = batchInterruptedEntryList.iterator();
                while (batchInterruptedEntryElemIter.hasNext()) {
                    Element batchInterruptedEntryElement = (Element)batchInterruptedEntryElemIter.next();
                    String reason = batchInterruptedEntryElement.getAttribute("reason");
                    message.append(reason);
                }
            }
        } catch (Exception e) {
            Debug.logError("Exception reading response from Google", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionReadingResponseFromGoogle", locale));
        }
        
        if (message.length() > 0) {
            Debug.logError("Error in the response from Google " + message.toString(), module);
            message.insert(0, UtilProperties.getMessage(resource, "productsExportToGoogle.errorInTheResponseFromGoogle", locale));
            return ServiceUtil.returnFailure(message.toString());
        }
        return ServiceUtil.returnSuccess();
    }
}
