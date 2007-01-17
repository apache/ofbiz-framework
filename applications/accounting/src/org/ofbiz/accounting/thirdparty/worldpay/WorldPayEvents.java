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
package org.ofbiz.accounting.thirdparty.worldpay;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;

import com.worldpay.core.ArgumentException;
import com.worldpay.protocols.http.HTTPURL;
import com.worldpay.protocols.http.URLParameters;
import com.worldpay.select.PurchaseToken;
import com.worldpay.select.Select;
import com.worldpay.select.SelectCurrency;
import com.worldpay.select.SelectDefs;
import com.worldpay.select.SelectException;
import com.worldpay.util.Currency;
import com.worldpay.util.CurrencyAmount;

/**
 * WorldPay Select Pro Events/Services
 */
public class WorldPayEvents {
    
    public static final String module = WorldPayEvents.class.getName();
    
    public static String worldPayRequest(HttpServletRequest request, HttpServletResponse response) {
        ServletContext application = ((ServletContext) request.getAttribute("servletContext"));
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
                                 
        // we need the websiteId for the correct properties file
        String webSiteId = CatalogWorker.getWebSiteId(request);
        
        // get the orderId from the request, stored by previous event(s)
        String orderId = (String) request.getAttribute("orderId");
        
        if (orderId == null) {
            Debug.logError("Problems getting orderId, was not found in request", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>OrderID not found, please contact customer service.");
            return "error";
        }
        
        // get the order header for total and other information
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot not get OrderHeader from datasource", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems getting order information, please contact customer service.");
            return "error";
        }
        
        // get the contact address to pass over
        GenericValue contactAddress = null;
        try {
            List addresses = delegator.findByAnd("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechPurposeTypeId", "BILLING_LOCATION"));
            if (addresses == null || addresses.size() == 0)
                addresses = delegator.findByAnd("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechPurposeTypeId", "SHIPPING_LOCATION"));
            GenericValue contactMech = EntityUtil.getFirst(addresses); 
            contactAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", contactMech.getString("contactMechId")));                      
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order contact information", module);
        }
        
        // get the country geoID
        GenericValue countryGeo = null;
        if (contactAddress != null) {
            try {
                countryGeo = contactAddress.getRelatedOne("CountryGeo");
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Problems getting country geo entity", module);                
            }
        }
        
        // string of customer's name
        String name = null;
        if (contactAddress != null) {
            if (contactAddress.get("attnName") != null && contactAddress.getString("attnName").length() > 0)
                name = contactAddress.getString("attnName");
            else if (contactAddress.get("toName") != null && contactAddress.getString("toName").length() > 0)
                name = contactAddress.getString("toName");
        }
        
        // build an address string
        StringBuffer address = null;
        if (contactAddress != null) {
            address = new StringBuffer();
            if (contactAddress.get("address1") != null) {            
                address.append(contactAddress.getString("address1").trim());
            }
            if (contactAddress.get("address2") != null) {
                if (address.length() > 0)
                    address.append("&#10;");
                address.append(contactAddress.getString("address2").trim());                
            }
            if (contactAddress.get("city") != null) {
                if (address.length() > 0)
                    address.append("&#10;");
                address.append(contactAddress.getString("city").trim());                
            }
            if (contactAddress.get("stateProvinceGeoId") != null) {
                if (contactAddress.get("city") != null)
                    address.append(", ");
                address.append(contactAddress.getString("stateProvinceGeoId").trim());
            }            
        }
        
        // get the telephone number to pass over
        String phoneNumber = null;
        GenericValue phoneContact = null;        
        
        // get the email address to pass over
        String emailAddress = null;
        GenericValue emailContact = null;
        try {
            List emails = delegator.findByAnd("OrderContactMech", UtilMisc.toMap("orderId", orderId, "contactMechPurposeTypeId", "ORDER_EMAIL"));
            GenericValue firstEmail = EntityUtil.getFirst(emails);
            emailContact = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", firstEmail.getString("contactMechId")));                        
            emailAddress = emailContact.getString("infoString");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order email address", module);
        }
        
        // get the product store
        GenericValue productStore = null;
        try {
            productStore = orderHeader.getRelatedOne("ProductStore");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStore from OrderHeader", module);
            
        }
        if (productStore == null) {
            Debug.logError("ProductStore is null", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems getting merchant configuration, please contact customer service.");
            return "error";
        }
        
        // get the payment properties file       
        GenericValue paymentConfig = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStore.getString("productStoreId"), "EXT_WORLDPAY", null, true);
        String configString = null;
        if (paymentConfig != null) {
            configString = paymentConfig.getString("paymentPropertiesPath");    
        }
                
        if (configString == null) {
            configString = "payment.properties";
        }
            
        String instId = UtilProperties.getPropertyValue(configString, "payment.worldpay.instId", "NONE");
        String authMode = UtilProperties.getPropertyValue(configString, "payment.worldpay.authMode", "A");
        String testMode = UtilProperties.getPropertyValue(configString, "payment.worldpay.testMode", "100");
        String fixContact = UtilProperties.getPropertyValue(configString, "payment.worldpay.fixContact", "N");
        String hideContact = UtilProperties.getPropertyValue(configString, "payment.worldpay.hideContact", "N");
        String confirmTemplate = UtilProperties.getPropertyValue(configString, "payment.worldpay.confirmTemplate", "");
        String timeout = UtilProperties.getPropertyValue(configString, "payment.worldpay.timeout", "0");
        String company = UtilFormatOut.checkEmpty(productStore.getString("companyName"), "");
        String defCur = UtilFormatOut.checkEmpty(productStore.getString("defaultCurrencyUomId"), "USD");                       
                           
        // order description
        String description = "Order #" + orderId;
        if (company != null && company.length() > 0)
        description = description + " from " + company;
        
        // check the instId - very important
        if (instId == null || instId.equals("NONE")) {
            Debug.logError("Worldpay InstId not found, cannot continue", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems getting merchant configuration, please contact customer service.");
            return "error";
        }  
        
        int instIdInt = 0;
        try {
            instIdInt = Integer.parseInt(instId);
        } catch (NumberFormatException nfe) {
            Debug.logError(nfe, "Problem converting instId string to integer", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems getting merchant configuration, please contact customer service.");
            return "error";
        }
        
        // check the testMode
        int testModeInt = -1;
        if (testMode != null) {
            try {
                testModeInt = Integer.parseInt(testMode);
            } catch (NumberFormatException nfe) {
                Debug.logWarning(nfe, "Problems getting the testMode value, setting to 0", module);
                testModeInt = 0;
            }            
        }
        
        // create the purchase link
        String purchaseURL = null;
        HTTPURL link = null;
        URLParameters linkParms = null;
        try {
            purchaseURL = Select.getPurchaseURL();
            link = new HTTPURL(purchaseURL);
            linkParms = link.getParameters();
        } catch (SelectException e) {
            Debug.logError(e, "Problems creating the purchase url", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problem creating link to WorldPay, please contact customer service.");
            return "error";
        } catch (ArgumentException e) {
            Debug.logError(e, "Problems creating HTTPURL link", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problem creating link to WorldPay, please contact customer service.");
            return "error";
        }                                                      
        
        // create the currency amount
        double orderTotal = orderHeader.getDouble("grandTotal").doubleValue();
        CurrencyAmount currencyAmount = null;         
        try {
            Currency currency = SelectCurrency.getInstanceByISOCode(defCur);
            currencyAmount = new CurrencyAmount(orderTotal, currency);
        } catch (ArgumentException ae) {
            Debug.logError(ae, "Problems building CurrencyAmount", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Merchant Configuration Error, please contact customer service.");
            return "error";
        }
                
        // create a purchase token
        PurchaseToken token = null;   
        try {
            token = new PurchaseToken(instIdInt, currencyAmount, orderId);
        } catch (SelectException e) {
            Debug.logError(e, "Cannot create purchase token", module);
        } catch (ArgumentException e) {
            Debug.logError(e, "Cannot create purchase token", module);
        } 
        if (token == null) {
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems creating a purchase token, please contact customer service.");
            return "error"; 
        }
        
        // set the auth/test modes        
        try {
            token.setAuthorisationMode(authMode);
        } catch (SelectException e) {
            Debug.logWarning(e, "Problems setting the authorization mode", module);
        }
        token.setTestMode(testModeInt);
                        
        // set the token to the purchase link
        try {
            linkParms.setValue(SelectDefs.SEL_purchase, token.produce());
        } catch (SelectException e) {
            Debug.logError(e, "Problems producing token", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems producing purchase token, please contact customer service.");
            return "error";
        }
        
        // set the customer data in the link
        linkParms.setValue(SelectDefs.SEL_desc, description);
        linkParms.setValue(SelectDefs.SEL_name, name != null ? name : "");
        linkParms.setValue(SelectDefs.SEL_address, address != null ? address.toString() : "");
        linkParms.setValue(SelectDefs.SEL_postcode, contactAddress != null ? contactAddress.getString("postalCode") : "");
        linkParms.setValue(SelectDefs.SEL_country, countryGeo.getString("geoCode"));
        linkParms.setValue(SelectDefs.SEL_tel, phoneNumber != null ? phoneNumber : ""); 
        linkParms.setValue(SelectDefs.SEL_email, emailAddress != null ? emailAddress : "");
        
        // set some optional data
        if (fixContact != null && fixContact.toUpperCase().startsWith("Y")) {
            linkParms.setValue(SelectDefs.SEL_fixContact, "Y");
        }
        if (hideContact != null && hideContact.toUpperCase().startsWith("Y")) {
            linkParms.setValue("hideContact", "Y"); // why is this not in SelectDefs??
        }
        
        // now set some send-back parameters
        linkParms.setValue("M_controlPath", (String)request.getAttribute("_CONTROL_PATH_"));
        linkParms.setValue("M_userLoginId", userLogin.getString("userLoginId"));               
        linkParms.setValue("M_dispatchName", dispatcher.getName());
        linkParms.setValue("M_delegatorName", delegator.getDelegatorName());        
        linkParms.setValue("M_webSiteId", webSiteId);        
        linkParms.setValue("M_localLocale", UtilHttp.getLocale(request).toString());               
        linkParms.setValue("M_confirmTemplate", confirmTemplate != null ? confirmTemplate : "");
                    
        // redirect to worldpay
        try {
            response.sendRedirect(link.produce());
        } catch (IOException e) {
            Debug.logError(e, "Problems redirecting to Worldpay", module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>Problems connecting with WorldPay, please contact customer service.");
            return "error";
        }
        
        return "success";
    }

}
