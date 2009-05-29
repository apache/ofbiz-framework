/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
**/
package org.ofbiz.googlecheckout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Document;

import com.google.checkout.CheckoutException;
import com.google.checkout.notification.NewOrderNotification;
import com.google.checkout.util.Utils;

public class GoogleCheckoutResponseEvents {

    public static final String module = GoogleCheckoutResponseEvents.class.getName();

    public static String checkNotification(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
          
        GoogleCheckoutHelper helper = new GoogleCheckoutHelper(dispatcher, delegator);
        //String xmlString = helper.getTestResponseString(); // TODO: make this work with the info sent to the event
        String xmlString = processGoogleCheckoutResponse(request,response);
        // check and parse the document
        Document document = null;
        try {
            document = Utils.newDocumentFromString(xmlString);
        } catch (CheckoutException e) {
            Debug.logError(e, module);
        }
        
        // check the document type and process 
        if (document != null) {
            String nodeValue = document.getDocumentElement().getNodeName();
            if ("new-order-notification".equals(nodeValue)) {
                // handle create new order
                NewOrderNotification info = new NewOrderNotification(document);
                String serialNumber = info.getSerialNumber();
                try {
                    helper.createOrder(info, getProductStoreId(request), getWebsiteId(request), getCurrencyUom(request), getLocale(request));
                    sendResponse(response, serialNumber, false);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    sendResponse(response, serialNumber, true);
                    return null;
                }
            } else {
                Debug.logWarning("Unsupported document type submitted by Google; [" + nodeValue + "] has not yet been implemented.", module);
            }
        }
        
        return null;
    }
    public static String processGoogleCheckoutResponse(HttpServletRequest request, HttpServletResponse response) {
        StringBuffer xmlBuff = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                xmlBuff.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            Debug.logError(e, "Problems sending verification message", module);
        }
        
        if(UtilValidate.isNotEmpty(xmlBuff.toString())) {
            return xmlBuff.toString();
        } else {
            return "error";
        }
    }
    private static void sendResponse(HttpServletResponse response, String serialNumber, boolean error) {
        if (error) {
            try {
                response.sendError(500);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            PrintWriter out = null;
            try {
                out = response.getWriter();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
            if (out != null) {
                out.println("<notification-acknowledgment xmlns=\"http://checkout.google.com/schema/2\" serial-number=\"" + serialNumber + "\"/>");
                out.close();
            }
        }
    }
    
    private static String getWebsiteId(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("webSiteId");
    }
    
    private static String getProductStoreId(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String websiteId = getWebsiteId(request);
        GenericValue website = null;
        try {
            website = delegator.findOne("WebSite", true, "webSiteId", websiteId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (website != null) {
            return website.getString("productStoreId");
        }
        return null;
    }
    
    private static String getCurrencyUom(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productStoreId = getProductStoreId(request);
        GenericValue productStore = null;
        try {
            productStore = delegator.findOne("ProductStore", true, "productStoreId", productStoreId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (productStore != null) {
            return productStore.getString("defaultCurrencyUomId");
        }
        return null;
    }
    
    private static Locale getLocale(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productStoreId = getProductStoreId(request);
        GenericValue productStore = null;
        try {
            productStore = delegator.findOne("ProductStore", true, "productStoreId", productStoreId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (productStore != null) {
            String localeStr = productStore.getString("defaultLocaleString");
            if (localeStr != null) {
                return UtilMisc.parseLocale(localeStr);
            }
        }
        return null;
    }
}
