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

package org.ofbiz.ebay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbayHelper { 
    private static final String configFileName = "ebayExport.properties";
    private static final String module = EbayHelper.class.getName();
    public static final String resource = "EbayUiLabels";

    public static Map<String, Object> buildEbayConfig(Map<String, Object> context, GenericDelegator delegator) {
        Map<String, Object> buildEbayConfigContext = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        if (UtilValidate.isNotEmpty(productStoreId)) {
            GenericValue eBayConfig = null;
            try {
                eBayConfig = delegator.findOne("EbayConfig", false, UtilMisc.toMap("productStoreId", productStoreId));
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource, "buildEbayConfig.unableToFindEbayConfig" + e.getMessage(), locale);
                return ServiceUtil.returnError(errMsg);
            }
            if (UtilValidate.isNotEmpty(eBayConfig)) {
                buildEbayConfigContext.put("devID", eBayConfig.getString("devId"));
                buildEbayConfigContext.put("appID", eBayConfig.getString("appId"));
                buildEbayConfigContext.put("certID", eBayConfig.getString("certId"));
                buildEbayConfigContext.put("token", eBayConfig.getString("token"));
                buildEbayConfigContext.put("compatibilityLevel", eBayConfig.getString("compatibilityLevel"));
                buildEbayConfigContext.put("siteID", eBayConfig.getString("siteId"));
                buildEbayConfigContext.put("xmlGatewayUri", eBayConfig.getString("xmlGatewayUri"));
            }
        } else {
            buildEbayConfigContext.put("devID", UtilProperties.getPropertyValue(configFileName, "eBayExport.devID"));
            buildEbayConfigContext.put("appID", UtilProperties.getPropertyValue(configFileName, "eBayExport.appID"));
            buildEbayConfigContext.put("certID", UtilProperties.getPropertyValue(configFileName, "eBayExport.certID"));
            buildEbayConfigContext.put("token", UtilProperties.getPropertyValue(configFileName, "eBayExport.token"));
            buildEbayConfigContext.put("compatibilityLevel", UtilProperties.getPropertyValue(configFileName, "eBayExport.compatibilityLevel"));
            buildEbayConfigContext.put("siteID", UtilProperties.getPropertyValue(configFileName, "eBayExport.siteID"));
            buildEbayConfigContext.put("xmlGatewayUri", UtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri"));
        }    
        return buildEbayConfigContext;
    }    

    public static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
    }
    
    public static String convertDate(String dateIn, String fromDateFormat, String toDateFormat) {
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

    public static String toString(InputStream inputStream) throws IOException {
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

    public static Map<String, Object> postItem(String postItemsUrl, StringBuffer generatedXmlData, String devID, String appID, String certID,
            String callName, String compatibilityLevel, String siteID) throws IOException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Request of " + callName + " To eBay:\n" + generatedXmlData.toString(), module);
        }
        HttpURLConnection connection = (HttpURLConnection) (new URL(postItemsUrl)).openConnection();
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
        outputStream.write(generatedXmlData.toString().getBytes());
        outputStream.close();

        int responseCode = connection.getResponseCode();
        InputStream inputStream = null;
        Map<String, Object> result = FastMap.newInstance();
        String response = null;

        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            response = EbayHelper.toString(inputStream);
            result = ServiceUtil.returnSuccess(response);
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(EbayHelper.toString(inputStream));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Response of " + callName + " From eBay:\n" + response, module);
        }
        return result;
    }
}