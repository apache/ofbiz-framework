/*
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
 */
package org.ofbiz.googlebase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductsExportToGoogle {
 
    private static final String resource = "GoogleBaseUiLabels";
    private static final String module = ProductsExportToGoogle.class.getName();

    public static Map exportToGoogle(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
 
        Map result = null;
        try {
            String configString = "googleBaseExport.properties";
 
            // get the Developer Key
            String developerKey = UtilProperties.getPropertyValue(configString, "googleBaseExport.developerKey");
 
            // get the Authentication Url
            String authenticationUrl = UtilProperties.getPropertyValue(configString, "googleBaseExport.authenticationUrl");
 
            // get the Google Account Email
            String accountEmail = UtilProperties.getPropertyValue(configString, "googleBaseExport.accountEmail");
 
            // get the Google Account Password
            String accountPassword = UtilProperties.getPropertyValue(configString, "googleBaseExport.accountPassword");
 
            // get the Url to Post Items
            String postItemsUrl = UtilProperties.getPropertyValue(configString, "googleBaseExport.postItemsUrl");
 
            StringBuffer dataItemsXml = new StringBuffer();
 
            result = buildDataItemsXml(dctx, context, dataItemsXml);
            if (!ServiceUtil.isFailure(result)) {
                String token = authenticate(authenticationUrl, accountEmail, accountPassword);
                if (token != null) {
                    result = postItem(token, postItemsUrl, developerKey, dataItemsXml, locale, (String)context.get("testMode"), (List)result.get("newProductsInGoogle"), (List)result.get("productsRemovedFromGoogle"), dispatcher, delegator);
                } else {
                    Debug.logError("Error during authentication to Google Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.errorDuringAuthenticationToGoogle", locale));
                }
            } else {
                return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
            }
        } catch (IOException e) {
            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result) + "IO Error loading resource :" + e.getMessage());
        }
        return result;
    }
 
    public static Map exportProductCategoryToGoogle(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String productCategoryId = (String) context.get("productCategoryId");
        String actionType = (String) context.get("actionType");
        String webSiteUrl = (String) context.get("webSiteUrl");
        String imageUrl = (String) context.get("imageUrl");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        if (userLogin == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.cannotRetrieveUserLogin", locale));
        }
 
        try {
            if (UtilValidate.isNotEmpty(productCategoryId)) {
                List productsList = FastList.newInstance();
                Map result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));
 
                if (result.get("categoryMembers") != null) {
                    List productCategoryMembers = (List)result.get("categoryMembers");
                    if (productCategoryMembers != null) {
                        Iterator i = productCategoryMembers.iterator();
                        while (i.hasNext()) {
                            GenericValue prodCatMemb = (GenericValue) i.next();
 
                            if (prodCatMemb != null) {
                                String productId = prodCatMemb.getString("productId");
 
                                if (productId != null) {
                                    GenericValue prod = prodCatMemb.getRelatedOne("Product");
                                    Timestamp salesDiscontinuationDate = prod.getTimestamp("salesDiscontinuationDate");
                                    // do not consider discontinued product
                                    if (salesDiscontinuationDate == null) {
                                        productsList.add(productId);
                                    }
                                }
                            }
                        }
                    }
                }
 
                if (productsList.size() == 0) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.noProductsAvailableInProductCategory", locale));
                } else {
                    Map paramIn = FastMap.newInstance();
                    paramIn.put("selectResult", productsList);
                    paramIn.put("webSiteUrl", webSiteUrl);
                    paramIn.put("imageUrl", imageUrl);
                    paramIn.put("actionType", actionType);
                    paramIn.put("statusId", "publish");
                    paramIn.put("testMode", "N");
                    paramIn.put("userLogin", userLogin);
                    result = dispatcher.runSync("exportToGoogle", paramIn);
 
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
                    }
                }
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.missingParameterProductCategoryId", locale));
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionInExportProductCategoryToGoogle", locale));
        }
 
        return ServiceUtil.returnSuccess();
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
        StringBuffer outputBuilder = new StringBuffer();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }
 
    private static Map postItem(String token, String postItemsUrl, String developerKey, StringBuffer dataItems,
                                Locale locale, String testMode, List newProductsInGoogle, List productsRemovedFromGoogle, LocalDispatcher dispatcher, GenericDelegator delegator) throws IOException {
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
                result = readResponseFromGoogle(response, newProductsInGoogle, productsRemovedFromGoogle, dispatcher, delegator, locale);
                //String msg = ServiceUtil.getErrorMessage(result);
                if (ServiceUtil.isError(result)) {
                    result = ServiceUtil.returnFailure((List)result.get(ModelService.ERROR_MESSAGE_LIST));
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
        List newProductsInGoogle = FastList.newInstance();
        List productsRemovedFromGoogle = FastList.newInstance();
        try {
            GenericDelegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            List selectResult = (List)context.get("selectResult");
            String webSiteUrl = (String)context.get("webSiteUrl");
            String imageUrl = (String)context.get("imageUrl");
            String actionType = (String)context.get("actionType");
            String statusId = (String)context.get("statusId");
            String trackingCodeId = (String)context.get("trackingCodeId");
            String countryCode = (String)context.get("countryCode");
            String webSiteMountPoint = (String)context.get("webSiteMountPoint");

            if (!webSiteUrl.startsWith("http://") && !webSiteUrl.startsWith("https://")) {
                webSiteUrl = "http://" + webSiteUrl;
            }
            if (webSiteUrl.endsWith("/")) {
                webSiteUrl = webSiteUrl.substring(0, webSiteUrl.length() - 1);
            }

            if (webSiteMountPoint.endsWith("/")) {
                webSiteMountPoint = webSiteMountPoint.substring(0, webSiteMountPoint.length() - 1);
            }
            if (webSiteMountPoint.startsWith("/")) {
                webSiteMountPoint = webSiteMountPoint.substring(1, webSiteMountPoint.length());
            }

            String productCurrency = null;
            if ("US".equals(countryCode)) {
                productCurrency = "USD";
            } else if ("GB".equals(countryCode)) {
                productCurrency = "GBP";
            } else if ("DE".equals(countryCode)) {
                productCurrency = "EUR";
            } else {
                Debug.logError("Exception during building data items to Google, Country Code must be either US, UK or DE: "+countryCode, module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.invalidCountryCode", locale));
            }
            // Get the list of products to be exported to Google Base
            List productsList  = delegator.findList("Product", EntityCondition.makeCondition("productId", EntityOperator.IN, selectResult), null, null, null, false);

            // Get the tracking code
            if (UtilValidate.isEmpty(trackingCodeId) || "_NA_".equals(trackingCodeId)) {
                trackingCodeId = "";
            } else {
                trackingCodeId = "?atc=" + trackingCodeId;
            }

            Document feedDocument = UtilXml.makeEmptyXmlDocument("feed");
            Element feedElem = feedDocument.getDocumentElement();
            feedElem.setAttribute("xmlns", "http://www.w3.org/2005/Atom");
            feedElem.setAttribute("xmlns:openSearch", "http://a9.com/-/spec/opensearchrss/1.0/");
            feedElem.setAttribute("xmlns:g", "http://base.google.com/ns/1.0");
            feedElem.setAttribute("xmlns:batch", "http://schemas.google.com/gdata/batch");
            feedElem.setAttribute("xmlns:app", "http://purl.org/atom/app#");

            // Iterate the product list getting all the relevant data
            Iterator productsListItr = productsList.iterator();
            int index = 0;
            String itemActionType = null;
            GenericValue googleProduct;
            while (productsListItr.hasNext()) {
                itemActionType = actionType;
                GenericValue prod = (GenericValue)productsListItr.next();
                String price = getProductPrice(dispatcher, prod);
                if (price == null) {
                    Debug.logInfo("Price not found for product [" + prod.getString("productId")+ "]; product will not be exported.", module);
                    continue;
                }
                // TODO: improve this (i.e. get the relative path from the properies file)
                String link = webSiteUrl + "/"+webSiteMountPoint+"/control/product/~product_id=" + prod.getString("productId") + trackingCodeId;
                String title = UtilFormatOut.encodeXmlValue(prod.getString("productName"));
                String description = UtilFormatOut.encodeXmlValue(prod.getString("description"));
                String imageLink = "";
                if (UtilValidate.isNotEmpty(prod.getString("largeImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("largeImageUrl");
                } else if (UtilValidate.isNotEmpty(prod.getString("mediumImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("mediumImageUrl");
                } else if (UtilValidate.isNotEmpty(prod.getString("smallImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("smallImageUrl");
                }

                String googleProductId = null;
                if (!"insert".equals(actionType)) {
                    try {
                        googleProduct = delegator.findByPrimaryKey("GoodIdentification", UtilMisc.toMap("productId", prod.getString("productId"), "goodIdentificationTypeId", "GOOGLE_ID"));
                        if (UtilValidate.isNotEmpty(googleProduct)) {
                            googleProductId = googleProduct.getString("idValue");
                        }
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to obtain GoodIdentification entity value of the Google id for product [" + prod.getString("productId") + "]: " + gee.getMessage(), module);
                    }
                }
                if ("update".equals(actionType) && UtilValidate.isEmpty(googleProductId)) {
                    itemActionType = "insert";
                }
                Element entryElem = UtilXml.addChildElement(feedElem, "entry", feedDocument);
                Element batchElem = UtilXml.addChildElement(entryElem, "batch:operation", feedDocument);
                batchElem.setAttribute("type", itemActionType);

                // status is draft or deactivate
                if (statusId != null && ("draft".equals(statusId) || "deactivate".equals(statusId))) {
                    Element appControlElem = UtilXml.addChildElement(entryElem, "app:control", feedDocument);
                    UtilXml.addChildElementValue(appControlElem, "app:draft", "yes", feedDocument);

                    // status is deactivate
                    if ("deactivate".equals(statusId)) {
                        UtilXml.addChildElement(appControlElem, "gm:disapproved", feedDocument);
                    }
                }

                UtilXml.addChildElementValue(entryElem, "title", title, feedDocument);

                Element contentElem = UtilXml.addChildElementValue(entryElem, "content", description, feedDocument);
                contentElem.setAttribute("type", "xhtml");

                if (UtilValidate.isNotEmpty(googleProductId)) {
                    UtilXml.addChildElementValue(entryElem, "id", googleProductId, feedDocument);
                } else {
                    UtilXml.addChildElementValue(entryElem, "id", link, feedDocument);
                }

                Element linkElem = UtilXml.addChildElement(entryElem, "link", feedDocument);
                linkElem.setAttribute("rel", "alternate");
                linkElem.setAttribute("type", "text/html");
                linkElem.setAttribute("href", link);

                UtilXml.addChildElementValue(entryElem, "g:item_type", "products", feedDocument);
                UtilXml.addChildElementValue(entryElem, "g:price", price, feedDocument);

                // Might be nicer to load this from the product but for now we'll set it based on the country destination
                UtilXml.addChildElementValue(entryElem, "g:currency", productCurrency, feedDocument);

                // Ensure the load goes to the correct country location either US dollar, GB sterling or DE euro
                UtilXml.addChildElementValue(entryElem, "g:target_country", countryCode, feedDocument);

                UtilXml.addChildElementValue(entryElem, "g:brand", prod.getString("brandName"), feedDocument);

                try {
                    googleProduct = delegator.findByPrimaryKey("GoodIdentification", UtilMisc.toMap("productId", prod.getString("productId"), "goodIdentificationTypeId", "SKU"));
                    if (UtilValidate.isNotEmpty(googleProduct)) {
                        UtilXml.addChildElementValue(entryElem, "g:ean", googleProduct.getString("idValue"), feedDocument);
                    }
                } catch (GenericEntityException gee) {
                    Debug.logInfo("Unable to get the SKU for product [" + prod.getString("productId") + "]: " + gee.getMessage(), module);
                }

                UtilXml.addChildElementValue(entryElem, "g:condition", "new", feedDocument);
                // This is a US specific requirement for product feeds
                //                     UtilXml.addChildElementValue(entryElem, "g:mpn", "", feedDocument);

                // if the product has an image it will be published on Google Product Search
                if (UtilValidate.isNotEmpty(imageLink)) {
                    UtilXml.addChildElementValue(entryElem, "g:image_link", imageLink, feedDocument);
                }
                // if the product is exported to google for the first time, we add it to the list
                if ("insert".equals(itemActionType)) {
                    newProductsInGoogle.add(prod.getString("productId"));
                    productsRemovedFromGoogle.add(null);
                } else if ("delete".equals(itemActionType)) {
                    newProductsInGoogle.add(null);
                    productsRemovedFromGoogle.add(prod.getString("productId"));
                } else {
                    newProductsInGoogle.add(null);
                    productsRemovedFromGoogle.add(null);
                }
                index++;
            }

            dataItemsXml.append(UtilXml.writeXmlDocument(feedDocument));
        } catch (IOException e) {
            return ServiceUtil.returnError("IO Error creating XML document for Google :" + e.getMessage());
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to read from product entity: "  + e.toString());
        }


        Map result = ServiceUtil.returnSuccess();
        result.put("newProductsInGoogle", newProductsInGoogle);
        result.put("productsRemovedFromGoogle", productsRemovedFromGoogle);
        Debug.log("======returning with result: " + result);
        return result;
    }

    private static String getProductPrice(LocalDispatcher dispatcher, GenericValue product) {
        String priceString = null;
        Map<String, Object> map = FastMap.newInstance();
        try {
            map = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product));
            boolean validPriceFound = ((Boolean)map.get("validPriceFound")).booleanValue();
            boolean isSale = ((Boolean)map.get("isSale")).booleanValue();
            if (validPriceFound) {
                priceString = map.get("price").toString();
            }
        } catch (GenericServiceException e1) {
            Debug.logError("calculateProductPrice Service exception getting the product price:" + e1.toString(), module);
        }
        return priceString;
    }
 
    private static Map readResponseFromGoogle(String msg, List newProductsInGoogle, List productsRemovedFromGoogle, LocalDispatcher dispatcher, GenericDelegator delegator, Locale locale) {
        List message = FastList.newInstance();
        // Debug.log("====get xml response from google: " + msg);
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            List atomEntryList = UtilXml.childElementList(elemResponse, "atom:entry");
            Iterator atomEntryElemIter = atomEntryList.iterator();
            int index = 0;
            while (atomEntryElemIter.hasNext()) {
                Element atomEntryElement = (Element)atomEntryElemIter.next();
                String id = UtilXml.childElementValue(atomEntryElement, "atom:id", "");
                if (UtilValidate.isNotEmpty(id) && newProductsInGoogle.get(index) != null) {
                    String productId = (String)newProductsInGoogle.get(index);
                    try {
                        GenericValue googleProductId = delegator.makeValue("GoodIdentification");
                        googleProductId.set("goodIdentificationTypeId", "GOOGLE_ID");
                        googleProductId.set("productId", productId);
                        googleProductId.set("idValue", id);
                        delegator.createOrStore(googleProductId);
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to create or update Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                if (UtilValidate.isNotEmpty(id) && productsRemovedFromGoogle.get(index) != null) {
                    String productId = (String)productsRemovedFromGoogle.get(index);
                    try {
                        int count = delegator.removeByAnd("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "GOOGLE_ID", "productId", productId));
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to remove Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                String title = "Google response: " + UtilXml.childElementValue(atomEntryElement, "atom:title", "");
                List batchStatusList = UtilXml.childElementList(atomEntryElement, "batch:status");
                Iterator batchStatusEntryElemIter = batchStatusList.iterator();
                while (batchStatusEntryElemIter.hasNext()) {
                    Element batchStatusEntryElement = (Element)batchStatusEntryElemIter.next();
                    if (UtilValidate.isNotEmpty(batchStatusEntryElement.getAttribute("reason"))) {
                        message.add(title + " " + batchStatusEntryElement.getAttribute("reason"));
                    }
                }
                String errors = UtilXml.childElementValue(atomEntryElement, "batch:status", "");
                if (UtilValidate.isNotEmpty(errors)) {
                    message.add(title + " " + errors);
                }
                index++;
            }
        } catch (Exception e) {
            Debug.logError("Exception reading response from Google: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionReadingResponseFromGoogle", locale));
        }
 
        if (message.size() > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "productsExportToGoogle.errorInTheResponseFromGoogle", locale), message);
        }
        return ServiceUtil.returnSuccess();
    }
}