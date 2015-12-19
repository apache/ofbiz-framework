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
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProductsExportToGoogle {

    private static final String resource = "GoogleBaseUiLabels";
    private static final String module = ProductsExportToGoogle.class.getName();
    private static final String googleBaseNSUrl = "http://base.google.com/ns/1.0";
    private static final String googleBaseBatchUrl = "http://schemas.google.com/gdata/batch";
    private static final String googleBaseMetadataUrl = "http://base.google.com/ns-metadata/1.0";
    private static final String googleBaseAppUrl = "http://purl.org/atom/app#";
    private static final String configString = "googleBaseExport.properties";

    public static Map<String, Object> exportToGoogle(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> result = null;
        try {
            Map<String, Object> googleBaseConfigResult = buildGoogleBaseConfig(context, delegator);
            StringBuffer dataItemsXml = new StringBuffer();

            result = buildDataItemsXml(dctx, context, dataItemsXml);
            if (!ServiceUtil.isFailure(result) && UtilValidate.isNotEmpty(googleBaseConfigResult)) {
                String token = authenticate(googleBaseConfigResult.get("authenticationUrl").toString(), googleBaseConfigResult.get("accountEmail").toString(), googleBaseConfigResult.get("accountPassword").toString());
                if (token != null) {
                    List<String> newProductsInGoogle = UtilGenerics.checkList(result.get("newProductsInGoogle"), String.class);
                    List<String> productsRemovedFromGoogle = UtilGenerics.checkList(result.get("productsRemovedFromGoogle"), String.class);
                    String localeString = result.get("localeString").toString();
                    result = postItem(token, googleBaseConfigResult.get("postItemsUrl").toString(), googleBaseConfigResult.get("developerKey").toString(), dataItemsXml,
                            locale, (String)context.get("testMode"), newProductsInGoogle, productsRemovedFromGoogle, dispatcher, delegator, localeString);
                } else {
                    Debug.logError("Error during authentication to Google Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.errorDuringAuthenticationToGoogle", locale));
                }
            } else {
                return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
            }
        } catch (IOException e) {
            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result) + UtilProperties.getMessage(resource, "GoogleBaseExportErrorLoadingResource", locale) + e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> exportProductCategoryToGoogle(DispatchContext dctx, Map<String, Object> context) {
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
                List<String> productsList = new LinkedList<String>();
                Map<String, Object> result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));

                if (result.get("categoryMembers") != null) {
                    List<GenericValue> productCategoryMembers = UtilGenerics.checkList(result.get("categoryMembers"), GenericValue.class);
                    if (productCategoryMembers != null) {
                        for (GenericValue prodCatMemb : productCategoryMembers) {
                            if (prodCatMemb != null) {
                                String productId = prodCatMemb.getString("productId");
                                if (productId != null) {
                                    GenericValue prod = prodCatMemb.getRelatedOne("Product", false);
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
                    Map<String, Object> paramIn = new HashMap<String, Object>();
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

    private static Map<String, Object> postItem(String token, String postItemsUrl, String developerKey, StringBuffer dataItems,
                                Locale locale, String testMode, List<String> newProductsInGoogle, List<String> productsRemovedFromGoogle,
                                LocalDispatcher dispatcher, Delegator delegator, String localeString) throws IOException {
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
        Map<String, Object> result = new HashMap<String, Object>();
        String response = "";
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = toString(inputStream);
            if (UtilValidate.isNotEmpty(response)) {
                result = readResponseFromGoogle(response, newProductsInGoogle, productsRemovedFromGoogle, dispatcher, delegator, locale, localeString);
                //String msg = ServiceUtil.getErrorMessage(result);
                if (ServiceUtil.isError(result)) {
                    result = ServiceUtil.returnFailure(UtilGenerics.checkList(result.get(ModelService.ERROR_MESSAGE_LIST)));
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

    private static Map<String, Object> buildDataItemsXml(DispatchContext dctx, Map<String, Object> context, StringBuffer dataItemsXml) {
        Locale locale = (Locale)context.get("locale");
        List<String> newProductsInGoogle = new LinkedList<String>();
        List<String> productsRemovedFromGoogle = new LinkedList<String>();
        String localeString = null;
        String productStoreId = (String) context.get("productStoreId");
        
        try {
            Delegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            List<String> selectResult = UtilGenerics.checkList(context.get("selectResult"), String.class);
            String webSiteUrl = (String)context.get("webSiteUrl");
            String actionType = (String)context.get("actionType");
            String statusId = (String)context.get("statusId");
            String trackingCodeId = (String)context.get("trackingCodeId");
            String countryCode = (String)context.get("countryCode");
            String webSiteMountPoint = (String)context.get("webSiteMountPoint");
            //Tagging Links
            String utmSource = null;
            String utmMedium = null;
            String utmTerm = null;
            String utmContent = null;
            String utmCampaign = null;
            
            if (UtilValidate.isNotEmpty(productStoreId)) {
                GenericValue googleBaseConfig = null;
                try {
                    googleBaseConfig = EntityQuery.use(delegator).from("GoogleBaseConfig").where(UtilMisc.toMap("productStoreId", productStoreId)).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError("Unable to find value for GoogleBaseConfig", module);
                    e.printStackTrace();
                }
                if (UtilValidate.isNotEmpty(googleBaseConfig)) {
                    String source = googleBaseConfig.getString("utmSource");
                    String medium = googleBaseConfig.getString("utmMedium");
                    String term = googleBaseConfig.getString("utmTerm");
                    String content = googleBaseConfig.getString("utmContent");
                    String campaign = googleBaseConfig.getString("utmCampaign");
                    
                    if (UtilValidate.isNotEmpty(source)) {
                        utmSource = "?utm_source=" + source;
                    } else {
                        utmSource = "";
                    }
                    if (UtilValidate.isNotEmpty(medium)) {
                        utmMedium = "&utm_medium=" + medium;
                    } else {
                        utmMedium = "";
                    }
                    if (UtilValidate.isNotEmpty(term)) {
                        utmTerm = "&utm_term=" + term;
                    } else {
                        utmTerm = "";
                    }
                    if (UtilValidate.isNotEmpty(content)) {
                        utmContent = "&utm_content=" + content;
                    } else {
                        utmContent = "";
                    }
                    if (UtilValidate.isNotEmpty(campaign)) {
                        utmCampaign = "&utm_campaign=" + campaign;
                    } else {
                        utmCampaign = "";
                    }
                }
            }
            
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
                localeString = "en_US";
            } else if ("GB".equals(countryCode)) {
                productCurrency = "GBP";
                localeString = "en_GB";
            } else if ("DE".equals(countryCode)) {
                productCurrency = "EUR";
                localeString = "de";
            } else {
                Debug.logError("Exception during building data items to Google, Country Code must be either US, UK or DE: "+countryCode, module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.invalidCountryCode", locale));
            }
            // Get the list of products to be exported to Google Base
            List<GenericValue> productsList  = EntityQuery.use(delegator).from("Product").where(EntityCondition.makeCondition("productId", EntityOperator.IN, selectResult)).queryList();

            // Get the tracking code
            if (UtilValidate.isEmpty(trackingCodeId) || "_NA_".equals(trackingCodeId)) {
                trackingCodeId = "";
            } else {
                trackingCodeId = "?atc=" + trackingCodeId;
            }

            Document feedDocument = UtilXml.makeEmptyXmlDocument("feed");
            Element feedElem = feedDocument.getDocumentElement();
            feedElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.w3.org/2005/Atom");
            feedElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:gm", googleBaseMetadataUrl);
            feedElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:g", googleBaseNSUrl);
            feedElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:batch", googleBaseBatchUrl);
            feedElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:app", googleBaseAppUrl);

            // Iterate the product list getting all the relevant data
            Iterator<GenericValue> productsListItr = productsList.iterator();
            int index = 0;
            String itemActionType = null;
            GenericValue googleProduct;
            while (productsListItr.hasNext()) {
                itemActionType = actionType;
                GenericValue prod = productsListItr.next();
                String price = getProductPrice(dispatcher, prod);
                if (price == null) {
                    Debug.logInfo("Price not found for product [" + prod.getString("productId")+ "]; product will not be exported.", module);
                    continue;
                }
                // TODO: improve this (i.e. get the relative path from the properties file)
                String link = webSiteUrl + "/" + webSiteMountPoint + "/control/product/~product_id=" + prod.getString("productId") + trackingCodeId + utmSource + utmMedium + utmTerm + utmContent + utmCampaign;
                String productName = null;
                String productDescription = null;
                //String productURL = null;
                List<GenericValue> productAndInfos = EntityQuery.use(delegator).from("ProductContentAndInfo").where("productId", prod.getString("productId"), "localeString", localeString, "thruDate", null).queryList();
                if (productAndInfos.size() > 0) {
                    for (GenericValue productContentAndInfo : productAndInfos ) {
                        String dataReSourceId = productContentAndInfo.getString("dataResourceId");
                        GenericValue electronicText = EntityQuery.use(delegator).from("ElectronicText").where("dataResourceId", dataReSourceId).queryOne();
                        if ("PRODUCT_NAME".equals(productContentAndInfo.getString("productContentTypeId")))
                            productName = electronicText.getString("textData");
                        if ("LONG_DESCRIPTION".equals(productContentAndInfo.getString("productContentTypeId")))
                            productDescription = electronicText.getString("textData");
                        //if ("PRODUCT_URL".equals(productContentAndInfo.getString("productContentTypeId")))
                            //productURL = electronicText.getString("textData") + utmSource + utmMedium + utmTerm + utmContent + utmCampaign;
                    }
                } else {
                    productName = prod.getString("internalName");
                    productDescription = prod.getString("longDescription");
                    if (UtilValidate.isEmpty(productDescription)) {
                        productDescription = prod.getString("description");
                    }
                }
                String title = UtilFormatOut.encodeXmlValue(productName);
                if (UtilValidate.isEmpty(title)) 
                    title = UtilFormatOut.encodeXmlValue(prod.getString("productName"));
                if (UtilValidate.isEmpty(title)) 
                    title = UtilFormatOut.encodeXmlValue(prod.getString("internalName"));
                String description = UtilFormatOut.encodeXmlValue(productDescription);
                if (UtilValidate.isEmpty(description))
                    description = UtilFormatOut.encodeXmlValue(prod.getString("description"));
                if (UtilValidate.isEmpty(description))
                    description = UtilFormatOut.encodeXmlValue(prod.getString("internalName"));
                
                String imageLink = "";
                if (UtilValidate.isNotEmpty(prod.getString("largeImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("largeImageUrl");
                } else if (UtilValidate.isNotEmpty(prod.getString("mediumImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("mediumImageUrl");
                } else if (UtilValidate.isNotEmpty(prod.getString("smallImageUrl"))) {
                    imageLink = webSiteUrl + prod.getString("smallImageUrl");
                }
                
                // should use image management script task.
                /*try {
                    List<GenericValue> productContentAndInfos = delegator.findByAnd("ProductContentAndInfo", 
                            UtilMisc.toMap("productId", prod.getString("productId"), "productContentTypeId","DEFAULT_IMAGE"));
                    if (UtilValidate.isNotEmpty(productContentAndInfos)) {
                        imageLink = webSiteUrl + productContentAndInfos.get(0).getString("drObjectInfo");
                    } else {
                        productContentAndInfos = delegator.findByAnd("ProductContentAndInfo", 
                                UtilMisc.toMap("productId", prod.getString("productId"),
                                        "productContentTypeId","IMAGE", 
                                        "statusId","IM_APPROVED",
                                        "drIsPublic", "Y"),
                                        UtilMisc.toList("contentId"));
                        if (UtilValidate.isNotEmpty(productContentAndInfos)) {
                            imageLink = webSiteUrl + productContentAndInfos.get(0).getString("drObjectInfo");
                        }
                    }
                } catch (Exception e) {
                    Debug.logError(e, module);
                }*/
                
                String googleProductId = null;
                if (!"insert".equals(actionType)) {
                    try {
                        googleProduct = EntityQuery.use(delegator).from("GoodIdentification").where("productId", prod.getString("productId"), "goodIdentificationTypeId", "GOOGLE_ID_" + localeString).queryOne();
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
                Element batchElem = UtilXml.addChildElementNSElement(entryElem, "batch:operation", feedDocument, googleBaseBatchUrl);

                Element batchOperationElem = (Element) batchElem.getFirstChild();
                batchOperationElem.setAttribute("type", itemActionType);
                
                if("delete".equals(actionType) || "update".equals(actionType)){
                    UtilXml.addChildElementNSValue(entryElem, "id", googleProductId, feedDocument, "");
                }

                Element appControlElem = UtilXml.addChildElementNSElement(entryElem, "app:control", feedDocument, googleBaseAppUrl);
                Element appControlChildElem = (Element) appControlElem.getLastChild();
                // Add the publishing priority for the product. By default it takes about 24 hours to publish your product if you submit data from Data Feed. By adding publishing priority your data
                // can be published in 15 - 30 minutes.
                UtilXml.addChildElementNSValue(appControlChildElem, "gm:publishing_priority", "high", feedDocument, googleBaseMetadataUrl);

                // status is draft or deactivate
                if (statusId != null && ("draft".equals(statusId) || "deactivate".equals(statusId))) {
                    UtilXml.addChildElementNSValue(appControlElem, "app:draft", "yes", feedDocument, googleBaseAppUrl);

                    // status is deactivate
                    if ("deactivate".equals(statusId)) {
                        UtilXml.addChildElementNSElement(appControlElem, "gm:disapproved", feedDocument, googleBaseMetadataUrl);
                    }
                }

                //Add product title
                UtilXml.addChildElementValue(entryElem, "title", title, feedDocument);

                Element contentElem = UtilXml.addChildElementValue(entryElem, "content", description, feedDocument);
                contentElem.setAttribute("type", "xhtml");

                UtilXml.addChildElementNSValue(entryElem, "g:id", prod.getString("productId"), feedDocument, googleBaseNSUrl);
                Element linkElem = UtilXml.addChildElement(entryElem, "link", feedDocument);
                linkElem.setAttribute("href", link);
                linkElem.setAttribute("rel", "self");
                linkElem.setAttribute("type", "application/atom+xml");
                
                // item_type is the categories in which your product should belong.
                UtilXml.addChildElementNSValue(entryElem, "g:item_type", "products", feedDocument, googleBaseNSUrl);

                List<GenericValue> productCategoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId", prod.getString("productId")).orderBy("productCategoryId").queryList();

                Iterator<GenericValue> productCategoryMembersIter = productCategoryMembers.iterator();
                while (productCategoryMembersIter.hasNext()) {
                    GenericValue productCategoryMember = productCategoryMembersIter.next();
                    GenericValue productCategory = productCategoryMember.getRelatedOne("ProductCategory", false);
                    String productCategoryId = productCategory.getString("productCategoryId");
                    String checkCategory = null;
                    if ("GB".equals(countryCode)) {
                        checkCategory = "UK";
                    } else {
                        checkCategory = countryCode;
                    }
                    if (UtilValidate.isNotEmpty(productCategoryId) && (checkCategory.equals(productCategoryId.substring(0, 2)))) {
                        String categoryDescription = "";
                        if (UtilValidate.isNotEmpty(productCategory.getString("categoryName"))) {
                            categoryDescription = productCategory.getString("categoryName");
                        } else if (UtilValidate.isNotEmpty(productCategory.getString("description"))) {
                            categoryDescription = productCategory.getString("description");
                        } else if (UtilValidate.isNotEmpty(productCategory.getString("longDescription"))) {
                            categoryDescription = productCategory.getString("longDescription");
                        }
                        if (UtilValidate.isNotEmpty(categoryDescription)) {
                            UtilXml.addChildElementNSValue(entryElem, "g:product_type", StringUtil.wrapString(categoryDescription).toString() , feedDocument, googleBaseNSUrl);
                        }
                    }
                }
                //Add product price
                UtilXml.addChildElementNSValue(entryElem, "g:price", price, feedDocument, googleBaseNSUrl);
                
                // Ensure the load goes to the correct country location either US dollar, GB sterling or DE euro
                UtilXml.addChildElementNSValue(entryElem, "g:target_country", countryCode, feedDocument, googleBaseNSUrl);
                if (UtilValidate.isNotEmpty(prod.getString("brandName"))) {
                    UtilXml.addChildElementNSValue(entryElem, "g:brand", prod.getString("brandName"), feedDocument, googleBaseNSUrl);
                }
                try {
                    googleProduct = EntityQuery.use(delegator).from("GoodIdentification").where("productId", prod.getString("productId"), "goodIdentificationTypeId", "SKU").queryOne();
                    if (UtilValidate.isNotEmpty(googleProduct)) {
                        UtilXml.addChildElementNSValue(entryElem, "g:ean", googleProduct.getString("idValue"), feedDocument, googleBaseNSUrl);
                    }
                } catch (GenericEntityException gee) {
                    Debug.logInfo("Unable to get the SKU for product [" + prod.getString("productId") + "]: " + gee.getMessage(), module);
                }
                
                String condition = null;
                if ("US".equals(countryCode) || "GB".equals(countryCode)) {
                    condition = "new";
                } else if ("DE".equals(countryCode)) {
                    condition = "new";
                }
                UtilXml.addChildElementNSValue(entryElem, "g:condition", condition, feedDocument, googleBaseNSUrl);
                // This is a US specific requirement for product feeds
                //                     UtilXml.addChildElementValue(entryElem, "g:mpn", "", feedDocument);

                // if the product has an image it will be published on Google Product Search
                if (UtilValidate.isNotEmpty(imageLink)) {
                    UtilXml.addChildElementNSValue(entryElem, "g:image_link", imageLink, feedDocument, googleBaseNSUrl);
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
                //Add product condition
                if ("Y".equals(context.get("allowRecommended"))) {
                    setRecommendedAttributes(prod, feedDocument, entryElem, delegator, countryCode, productCurrency);
                }
                index++;
            }
            //Debug.logInfo("The value of generated String is ========\n" + UtilXml.writeXmlDocument(feedDocument), module);
            dataItemsXml.append(UtilXml.writeXmlDocument(feedDocument));
        } catch (IOException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "GoogleBaseExportErrorCreatingXmlDocument", locale) + e.getMessage());
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "GoogleBaseExportUnableToReadFromProduct", locale) + e.toString());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("newProductsInGoogle", newProductsInGoogle);
        result.put("productsRemovedFromGoogle", productsRemovedFromGoogle);
        result.put("localeString", localeString);
        //Debug.logInfo("======returning with result: " + result, module);
        return result;
    }

    private static String getProductPrice(LocalDispatcher dispatcher, GenericValue product) {
        String priceString = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            map = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product));
            boolean validPriceFound = ((Boolean)map.get("validPriceFound")).booleanValue();
            if (validPriceFound) {
                priceString = map.get("price").toString();
            }
        } catch (GenericServiceException e1) {
            Debug.logError("calculateProductPrice Service exception getting the product price:" + e1.toString(), module);
        }
        return priceString;
    }

    private static Map<String, Object> readResponseFromGoogle(String msg, List<String> newProductsInGoogle, List<String> productsRemovedFromGoogle,
            LocalDispatcher dispatcher, Delegator delegator, Locale locale, String localeString) {
        List<String> message = new LinkedList<String>();
        // Debug.logInfo("====get xml response from google: " + msg, module);
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            List<? extends Element> atomEntryList = UtilXml.childElementList(elemResponse, "atom:entry");
            Iterator<? extends Element> atomEntryElemIter = atomEntryList.iterator();
            int index = 0;
            while (atomEntryElemIter.hasNext()) {
                String id = null;
                Element atomEntryElement = atomEntryElemIter.next();
                Node atomId = atomEntryElement.getElementsByTagName("atom:id").item(0);
                if(UtilValidate.isNotEmpty(atomId)) {
                    id = atomId.getTextContent();
                }
                String productId = "";
                if (UtilValidate.isNotEmpty(id) && newProductsInGoogle.get(index) != null) {
                    productId = newProductsInGoogle.get(index);
                    try {
                        GenericValue googleProductId = delegator.makeValue("GoodIdentification");
                        googleProductId.set("goodIdentificationTypeId", "GOOGLE_ID_" + localeString);
                        googleProductId.set("productId", productId);
                        googleProductId.set("idValue", id);
                        delegator.createOrStore(googleProductId);
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to create or update Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                if (UtilValidate.isNotEmpty(id) && productsRemovedFromGoogle.get(index) != null) {
                    productId = productsRemovedFromGoogle.get(index);
                    try {
                        delegator.removeByAnd("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "GOOGLE_ID_" + localeString, "productId", productId));
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to remove Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                if (UtilValidate.isEmpty(productId)){
                    productId = atomEntryElement.getElementsByTagName("g:id").item(0).getTextContent();
                }
                String title = "Google response:" + UtilXml.childElementValue(atomEntryElement, "atom:title", "");
                List<? extends Element> batchStatusList = UtilXml.childElementList(atomEntryElement, "batch:status");
                Iterator<? extends Element> batchStatusEntryElemIter = batchStatusList.iterator();
                while (batchStatusEntryElemIter.hasNext()) {
                    Element batchStatusEntryElement = batchStatusEntryElemIter.next();
                    if (UtilValidate.isNotEmpty(batchStatusEntryElement.getAttribute("reason"))) {
                        message.add(title + " " + productId + ":" + batchStatusEntryElement.getAttribute("reason"));
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
        //Debug.logInfo("===== batch:status - message: " + message, module);
        //Debug.logInfo("===== Exports: " + message.size() + " products" , null);
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, Object> buildGoogleBaseConfig(Map<String, Object> context, Delegator delegator) {
        String productStoreId = (String) context.get("productStoreId");
        Map<String, Object> buildGoogleBaseConfigContext = new HashMap<String, Object>();

        if (UtilValidate.isNotEmpty(productStoreId)) {
            GenericValue googleBaseConfig = null;
            try {
                googleBaseConfig = EntityQuery.use(delegator).from("GoogleBaseConfig").where(UtilMisc.toMap("productStoreId", productStoreId)).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError("Unable to find value for GoogleBaseConfig", module);
                e.printStackTrace();
            }
            if (UtilValidate.isNotEmpty(googleBaseConfig)) {
               buildGoogleBaseConfigContext.put("developerKey", googleBaseConfig.getString("developerKey"));
               buildGoogleBaseConfigContext.put("authenticationUrl", googleBaseConfig.getString("authenticationUrl"));
               buildGoogleBaseConfigContext.put("accountEmail", googleBaseConfig.getString("accountEmail"));
               buildGoogleBaseConfigContext.put("accountPassword", googleBaseConfig.getString("accountPassword"));
               buildGoogleBaseConfigContext.put("postItemsUrl", googleBaseConfig.getString("postItemsUrl"));
            }
        } else {
            buildGoogleBaseConfigContext.put("developerKey", EntityUtilProperties.getPropertyValue(configString, "googleBaseExport.developerKey", delegator));
            buildGoogleBaseConfigContext.put("authenticationUrl", EntityUtilProperties.getPropertyValue(configString, "googleBaseExport.authenticationUrl", delegator));
            buildGoogleBaseConfigContext.put("accountEmail", EntityUtilProperties.getPropertyValue(configString, "googleBaseExport.accountEmail", delegator));
            buildGoogleBaseConfigContext.put("accountPassword", EntityUtilProperties.getPropertyValue(configString, "googleBaseExport.accountPassword", delegator));
            buildGoogleBaseConfigContext.put("postItemsUrl", EntityUtilProperties.getPropertyValue(configString, "googleBaseExport.postItemsUrl", delegator));
        }
        return buildGoogleBaseConfigContext;
    }
    private static void setRecommendedAttributes(GenericValue product, Document feedDocument, Element entryElem, Delegator delegator, String countryCode, String productCurrency) {
        if (UtilValidate.isEmpty(product) && UtilValidate.isEmpty(feedDocument) && UtilValidate.isEmpty(countryCode) && UtilValidate.isNotEmpty(entryElem)) {
            Debug.logError("Required parameters in setRecommended method", module);
        }
        try {
            //Add brand name
            if (UtilValidate.isNotEmpty(product.getString("brandName"))) {
                UtilXml.addChildElementNSValue(entryElem, "g:brand", product.getString("brandName"), feedDocument, googleBaseNSUrl);
            }
            
            //Add quantity item
            List<GenericValue> inventoryItems = EntityQuery.use(delegator).from("InventoryItem").where("productId", product.getString("productId")).queryList();
            if (UtilValidate.isNotEmpty(inventoryItems)) {
                BigDecimal totalquantity = new BigDecimal(0);
                for (GenericValue inventoryItem : inventoryItems) {
                    BigDecimal availableToPromiseTotal = new BigDecimal(inventoryItem.getString("availableToPromiseTotal"));
                    totalquantity = totalquantity.add(availableToPromiseTotal);
                }
                if ("DE".equals(countryCode)) {
                    UtilXml.addChildElementNSValue(entryElem, "g:menge", totalquantity.toString(), feedDocument, googleBaseNSUrl);
                } else {
                    Integer intValue = totalquantity.intValue();
                    UtilXml.addChildElementNSValue(entryElem, "g:quantity", intValue.toString(), feedDocument, googleBaseNSUrl);
                }
            }
            //Add Online Only
            UtilXml.addChildElementNSValue(entryElem, "g:online_only", "y", feedDocument, googleBaseNSUrl);
            //Add shipping weight
            if (UtilValidate.isNotEmpty(product.getBigDecimal("shippingWeight")) && UtilValidate.isNotEmpty(product.getString("weightUomId"))) {
                GenericValue uom = EntityQuery.use(delegator).from("Uom").where("uomId", product.getString("weightUomId")).queryOne();
                String shippingWeight = product.getBigDecimal("shippingWeight") + " " + uom.getString("description");
                UtilXml.addChildElementNSValue(entryElem, "g:shipping_weight", shippingWeight, feedDocument, googleBaseNSUrl);
            }
            List<GenericValue> productFeatureAndAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", product.getString("productId")).queryList();
            if (productFeatureAndAppls.size() > 0) {
                for (GenericValue productFeatureAndAppl : productFeatureAndAppls) {
                    //Add Genre
                    if ("GENRE".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:genre", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                    //Add color
                    if ("COLOR".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:color", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                    //Add size
                    if ("SIZE".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:size", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                    //Add year
                    if ("YEAR_MADE".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:year", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                    //Add author
                    if ("ARTIST".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:author", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                    //Add edition
                    if ("EDITION".equals(productFeatureAndAppl.getString("productFeatureTypeId"))) {
                        UtilXml.addChildElementNSValue(entryElem, "g:edition", productFeatureAndAppl.getString("description"), feedDocument, googleBaseNSUrl);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logError(e.toString(), module);
        }
    }
}
