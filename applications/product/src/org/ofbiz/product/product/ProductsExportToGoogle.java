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
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastMap;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
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
    private static final String xmlHeader = "<?xml version=\'1.0\' encoding='UTF-8'?>\n";

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
            
            if (!ServiceUtil.isFailure(buildDataItemsXml(dctx, context, dataItemsXml))) { 
                String token = authenticate(authenticationUrl, accountEmail, accountPassword);

                if (token != null) {    
                    Map result = postItem(token, postItemsUrl, developerKey, dataItemsXml);
                    if (ServiceUtil.isFailure(result))
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
                } else {
                    Debug.logError("Error during authentication to Google Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.errorDuringAuthenticationToGoogle", locale));
                }
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
            if (tokenizer.nextToken().equals("Auth")) {
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
    
    private static Map postItem(String token, String postItemsUrl, String developerKey, StringBuffer dataItems) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)(new URL(postItemsUrl)).openConnection();
      
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
        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            inputStream = connection.getInputStream();
            result = ServiceUtil.returnSuccess(toString(inputStream));
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        }
        return result;
    }
    
    private static Map buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml) {
        Locale locale = (Locale)context.get("locale");
        try {
             GenericDelegator delegator = dctx.getDelegator();
             LocalDispatcher dispatcher = dctx.getDispatcher();
             String products = (String)context.get("products");
             String webSiteUrl = (String)context.get("webSiteUrl");
             String imageUrl = (String)context.get("imageUrl");
             String trackingCodeId = (String)context.get("trackingCodeId");
             
             // Get the list of products to be exported to Google Base
             List productsList  = delegator.findByCondition("Product", new EntityExpr("productId", EntityOperator.IN, StringUtil.split(products, ",")), null, null);
             
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
                 
                 dataItemsXml.append(xmlHeader);
                 
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
                     batchElem.setAttribute("type", "insert");
                     
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
                     
                     if (UtilValidate.isNotEmpty(image_link)) {
                         UtilXml.addChildElementValue(entryElem, "g:image_link", image_link, feedDocument);
                     }
                     
                     Element appControlElem = UtilXml.addChildElement(entryElem, "app:control", feedDocument);
                     appControlElem.setAttribute("xmlns:app", "http://purl.org/atom/app#");
                     UtilXml.addChildElementValue(appControlElem, "app:draft", "yes", feedDocument);
                 }
                 OutputStream os = new ByteArrayOutputStream();
                 OutputFormat format = new OutputFormat();
                 format.setOmitDocumentType(true);
                 format.setOmitXMLDeclaration(true);
                 format.setIndenting(false);
                 XMLSerializer serializer = new XMLSerializer(os, format);
                 serializer.asDOMSerializer();
                 serializer.serialize(feedDocument.getDocumentElement());
                 
                 dataItemsXml.append(os.toString());
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
                // "price" is a mandatory output
                priceString = UtilFormatOut.formatPrice((Double)map.get("price"));
            }
            /*
            if (isSale && null != map.get("price")) {
                priceString = UtilFormatOut.formatPrice((Double)map.get("price"));
            } else if (null != map.get("defaultPrice")) {
                priceString = UtilFormatOut.formatPrice((Double)map.get("defaultPrice"));
            } else if (null != map.get("listPrice")) {
                priceString = UtilFormatOut.formatPrice((Double)map.get("listPrice"));
            }
             */
        } catch(Exception e){
            Debug.logError("Exception calculating price for product [" + product.getString("productId") + "]", module);
        }
        return priceString;
    }
}
