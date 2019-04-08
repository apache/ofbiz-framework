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

package org.apache.ofbiz.ebay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbayHelper {
    private static final String configFileName = "ebayExport.properties";
    private static final String module = EbayHelper.class.getName();
    public static final String resource = "EbayUiLabels";

    public static Map<String, Object> buildEbayConfig(Map<String, Object> context, Delegator delegator) {
        Map<String, Object> buildEbayConfigContext = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        if (UtilValidate.isNotEmpty(productStoreId)) {
            GenericValue eBayConfig = null;
            try {
                eBayConfig = EntityQuery.use(delegator).from("EbayConfig").where(UtilMisc.toMap("productStoreId", productStoreId)).queryOne();
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource, "buildEbayConfig.unableToFindEbayConfig" + e.getMessage(), locale);
                return ServiceUtil.returnError(errMsg);
            }
            if (eBayConfig != null) {
                buildEbayConfigContext.put("devID", eBayConfig.getString("devId"));
                buildEbayConfigContext.put("appID", eBayConfig.getString("appId"));
                buildEbayConfigContext.put("certID", eBayConfig.getString("certId"));
                buildEbayConfigContext.put("token", eBayConfig.getString("token"));
                buildEbayConfigContext.put("compatibilityLevel", eBayConfig.getString("compatibilityLevel"));
                buildEbayConfigContext.put("siteID", eBayConfig.getString("siteId"));
                buildEbayConfigContext.put("xmlGatewayUri", eBayConfig.getString("xmlGatewayUri"));
                buildEbayConfigContext.put("apiServerUrl", eBayConfig.getString("xmlGatewayUri"));
            }
        } else {
            buildEbayConfigContext.put("devID", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.devID", delegator));
            buildEbayConfigContext.put("appID", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.appID", delegator));
            buildEbayConfigContext.put("certID", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.certID", delegator));
            buildEbayConfigContext.put("token", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.token", delegator));
            buildEbayConfigContext.put("compatibilityLevel", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.compatibilityLevel", delegator));
            buildEbayConfigContext.put("siteID", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.siteID", delegator));
            buildEbayConfigContext.put("xmlGatewayUri", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri", delegator));
            buildEbayConfigContext.put("apiServerUrl", EntityUtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri", delegator));
        }
        return buildEbayConfigContext;
    }

    public static void appendRequesterCredentials(Element elem, Document doc, String token) {
        Element requesterCredentialsElem = UtilXml.addChildElement(elem, "RequesterCredentials", doc);
        UtilXml.addChildElementValue(requesterCredentialsElem, "eBayAuthToken", token, doc);
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
        Map<String, Object> result = new HashMap<String, Object>();
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

    public static void setShipmentMethodType(ShoppingCart cart, String shippingService, String productStoreId, Delegator delegator) {
        String partyId = "_NA_";
        String shipmentMethodTypeId = "NO_SHIPPING";
        try {
            GenericValue ebayShippingMethod = EntityQuery.use(delegator).from("EbayShippingMethod").where("shipmentMethodName", shippingService, "productStoreId", productStoreId).queryOne();
            if (ebayShippingMethod != null) {
                partyId = ebayShippingMethod.getString("carrierPartyId");
                shipmentMethodTypeId = ebayShippingMethod.getString("shipmentMethodTypeId");
            } else {
                //Find ebay shipping method on the basis of shipmentMethodName so that we can create new record with productStorId, EbayShippingMethod data is required for atleast one productStore
                ebayShippingMethod = EntityQuery.use(delegator).from("EbayShippingMethod").where("shipmentMethodName", shippingService).queryFirst();
                ebayShippingMethod.put("productStoreId", productStoreId);
                delegator.create(ebayShippingMethod);
                partyId = ebayShippingMethod.getString("carrierPartyId");
                shipmentMethodTypeId = ebayShippingMethod.getString("shipmentMethodTypeId");
            }
        } catch (GenericEntityException e) {
            Debug.logInfo("Unable to find EbayShippingMethod", module);
        }
        cart.setAllCarrierPartyId(partyId);
        cart.setAllShipmentMethodTypeId(shipmentMethodTypeId);
    }

    public static boolean createPaymentFromPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
        String orderId, String externalId, Timestamp orderDate, BigDecimal amount, String partyIdFrom) {
        List<GenericValue> paymentPreferences = null;
        try {
            paymentPreferences = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId, "statusId", "PAYMENT_RECEIVED", "paymentMethodTypeId", "EXT_EBAY").queryList();

            if (UtilValidate.isNotEmpty(paymentPreferences)) {
                Iterator<GenericValue> i = paymentPreferences.iterator();
                while (i.hasNext()) {
                    GenericValue pref = i.next();
                    boolean okay = createPayment(dispatcher, userLogin, pref, orderId, externalId, orderDate, partyIdFrom);
                    if (!okay)
                        return false;
                }
            } else {
                paymentPreferences = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED", "paymentMethodTypeId", "EXT_EBAY").queryList();
                if (UtilValidate.isNotEmpty(paymentPreferences)) {
                    Iterator<GenericValue> i = paymentPreferences.iterator();
                    while (i.hasNext()) {
                        GenericValue pref = i.next();
                        if (UtilValidate.isNotEmpty(amount)) {
                            pref.set("statusId", "PAYMENT_RECEIVED");
                            pref.set("maxAmount", amount);
                            pref.store();
                        }
                        boolean okay = createPayment(dispatcher, userLogin, pref, orderId, externalId, orderDate, partyIdFrom);
                        if (!okay)
                            return false;
                    }
                }
            } 
        } catch (GenericEntityException gee) {
            Debug.logError(gee, "Cannot get payment preferences for order #" + orderId, module);
            return false;
        } catch (Exception e) {
            Debug.logError(e, "Cannot get payment preferences for order #" + orderId, module);
            return false;
        }
        return true;
    }

    public static boolean createPayment(LocalDispatcher dispatcher, GenericValue userLogin,
            GenericValue paymentPreference, String orderId, String externalId, Timestamp orderDate, String partyIdFrom) {
        try {
            Delegator delegator = paymentPreference.getDelegator();

            // create the PaymentGatewayResponse
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse");
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
            Map<String, Object> results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin,
                    "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "paymentFromId",
                    partyIdFrom, "paymentRefNum", externalId, "comments", "Payment receive via eBay"));

            if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
                return false;
            }
            return true;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to create the payment for order " + orderId, module);
            return false;
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to create the payment for order " + orderId, module);
            return false;
        }
    }

    public static GenericValue makeOrderAdjustment(Delegator delegator, String orderAdjustmentTypeId,
            String orderId, String orderItemSeqId, String shipGroupSeqId, double amount, double sourcePercentage) {
        GenericValue orderAdjustment = null;

        try {
            if (UtilValidate.isNotEmpty(orderItemSeqId)) {
                orderItemSeqId = "_NA_";
            }
            if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
                shipGroupSeqId = "_NA_";
            }

            Map<String, Object> inputMap = UtilMisc.toMap("orderAdjustmentTypeId", orderAdjustmentTypeId, "orderId", orderId,
                    "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId, "amount",
                    new BigDecimal(amount));
            if (sourcePercentage != 0) {
                inputMap.put("sourcePercentage", new Double(sourcePercentage));
            }
            orderAdjustment = delegator.makeValue("OrderAdjustment", inputMap);
        } catch (Exception e) {
            Debug.logError(e, "Failed to made order adjustment for order " + orderId, module);
        }
        return orderAdjustment;
    }

    public static String createCustomerParty(LocalDispatcher dispatcher, String name, GenericValue userLogin) {
        String partyId = null;

        try {
            if (UtilValidate.isNotEmpty(name) && userLogin != null) {
                Debug.logVerbose("Creating Customer Party: " + name, module);

                // Try to split the lastname from the firstname
                String firstName = "";
                String lastName = "";
                int pos = name.indexOf(" ");

                if (pos >= 0) {
                    firstName = name.substring(0, pos);
                    lastName = name.substring(pos + 1);
                } else {
                    lastName = name;
                }

                Map<String, Object> summaryResult = dispatcher.runSync("createPerson", UtilMisc.<String, Object> toMap("description",
                        name, "firstName", firstName, "lastName", lastName, "userLogin", userLogin, "comments",
                        "Created via eBay"));
                partyId = (String) summaryResult.get("partyId");
                Debug.logVerbose("Created Customer Party: " + partyId, module);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createPerson", module);
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPerson", module);
        }
        return partyId;
    }

    public static String createAddress(LocalDispatcher dispatcher, String partyId, GenericValue userLogin,
            String contactMechPurposeTypeId, Map<String, Object> address) {
        Debug.logInfo("Creating postal address with input map: " + address, module);
        String contactMechId = null;
        try {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("partyId", partyId);
            context.put("toName", address.get("buyerName"));
            context.put("address1", address.get("shippingAddressStreet1"));
            context.put("address2", address.get("shippingAddressStreet2"));
            context.put("postalCode", address.get("shippingAddressPostalCode"));
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", contactMechPurposeTypeId);

            String country = (String) address.get("shippingAddressCountry");
            String state = (String) address.get("shippingAddressStateOrProvince");
            String city = (String) address.get("shippingAddressCityName");
            correctCityStateCountry(dispatcher, context, city, state, country);

            Map<String, Object> summaryResult = dispatcher.runSync("createPartyPostalAddress", context);
            contactMechId = (String) summaryResult.get("contactMechId");
            // Set also as a billing address
            context = new HashMap<String, Object>();
            context.put("partyId", partyId);
            context.put("contactMechId", contactMechId);
            context.put("contactMechPurposeTypeId", "BILLING_LOCATION");
            context.put("userLogin", userLogin);
            dispatcher.runSync("createPartyContactMechPurpose", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createAddress", module);
        }
        return contactMechId;
    }

    public static void correctCityStateCountry(LocalDispatcher dispatcher, Map<String, Object> map, String city, String state, String country) {
        try {
            String geoCode = null;
            Debug.logInfo("correctCityStateCountry params: " + city + ", " + state + ", " + country, module);
            if (UtilValidate.isEmpty(country)) {
                geoCode = "US";
            }
            country = country.toUpperCase();
            if (country.indexOf("UNITED STATES") > -1 || country.indexOf("USA") > -1) {
                geoCode = "US";
            }
            if (UtilValidate.isEmpty(geoCode)) {
                geoCode = country;
            }
            Debug.logInfo("GeoCode: " + geoCode, module);
            Map<String, Object> outMap = getCountryGeoId(dispatcher.getDelegator(), geoCode);
            String geoId = (String) outMap.get("geoId");
            if (UtilValidate.isEmpty(geoId)) {
                geoId = "USA";
            }
            map.put("countryGeoId", geoId);
            country = geoId;
            Debug.logInfo("Country geoid: " + geoId, module);
            if (geoId.equals("USA") || geoId.equals("CAN")) {
                if (UtilValidate.isNotEmpty(state)) {
                    map.put("stateProvinceGeoId", state.toUpperCase());
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

    public static String createPartyPhone(LocalDispatcher dispatcher, String partyId, String phoneNumber,
            GenericValue userLogin) {
        Map<String, Object> summaryResult = new HashMap<String, Object>();
        Map<String, Object> context = new HashMap<String, Object>();
        String phoneContactMechId = null;

        try {
            context.put("contactNumber", phoneNumber);
            context.put("partyId", partyId);
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", "PHONE_SHIPPING");
            summaryResult = dispatcher.runSync("createPartyTelecomNumber", context);
            phoneContactMechId = (String) summaryResult.get("contactMechId");
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createPartyPhone", module);
        }
        return phoneContactMechId;
    }

    public static String createPartyEmail(LocalDispatcher dispatcher, String partyId, String email, GenericValue userLogin) {
        Map<String, Object> context = new HashMap<String, Object>();
        Map<String, Object> summaryResult = new HashMap<String, Object>();
        String emailContactMechId = null;

        try {
            if (UtilValidate.isNotEmpty(email)) {
                context.clear();
                context.put("emailAddress", email);
                context.put("userLogin", userLogin);
                context.put("contactMechTypeId", "EMAIL_ADDRESS");
                summaryResult = dispatcher.runSync("createEmailAddress", context);
                emailContactMechId = (String) summaryResult.get("contactMechId");

                context.clear();
                context.put("partyId", partyId);
                context.put("contactMechId", emailContactMechId);
                context.put("contactMechPurposeTypeId", "OTHER_EMAIL");
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyContactMech", context);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to createPartyEmail", module);
        }
        return emailContactMechId;
    }

    public static void createEbayCustomer(LocalDispatcher dispatcher, String partyId, String ebayUserIdBuyer, String eias,
            GenericValue userLogin) {
        Map<String, Object> context = new HashMap<String, Object>();
        Map<String, Object> summaryResult = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(eias)) {
            try {
                context.put("partyId", partyId);
                context.put("attrName", "EBAY_BUYER_EIAS");
                context.put("attrValue", eias);
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyAttribute", context);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Failed to create eBay EIAS party attribute");
            }
            context.clear();
            summaryResult.clear();
        }
        if (UtilValidate.isNotEmpty(ebayUserIdBuyer)) {
            try {
                context.put("partyId", partyId);
                context.put("attrName", "EBAY_BUYER_USER_ID");
                context.put("attrValue", ebayUserIdBuyer);
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyAttribute", context);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Failed to create eBay userId party attribute");
            }
        }
    }

    public static Map<String, Object> getCountryGeoId(Delegator delegator, String geoCode) {
        GenericValue geo = null;
        try {
            Debug.logInfo("geocode: " + geoCode, module);

            geo = EntityQuery.use(delegator).from("Geo").where("geoCode", geoCode.toUpperCase(), "geoTypeId", "COUNTRY").queryFirst();
            Debug.logInfo("Found a geo entity " + geo, module);
            if (UtilValidate.isEmpty(geo)) {
                geo = delegator.makeValue("Geo");
                geo.set("geoId", geoCode + "_IMPORTED");
                geo.set("geoTypeId", "COUNTRY");
                geo.set("geoName", geoCode + "_IMPORTED");
                geo.set("geoCode", geoCode + "_IMPORTED");
                geo.set("abbreviation", geoCode + "_IMPORTED");
                delegator.create(geo);
                Debug.logInfo("Creating new geo entity: " + geo, module);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Failed to find/setup geo id";
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("geoId", geo.get("geoId"));
        return result;
    }

    public static String setShippingAddressContactMech(LocalDispatcher dispatcher, Delegator delegator,
            GenericValue party, GenericValue userLogin, Map<String, Object> context) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a shipping location
        // purpose.
        Collection<GenericValue> shippingLocations = ContactHelper.getContactMechByPurpose(party, "SHIPPING_LOCATION", false);

        // check them to see if one matches
        Iterator<GenericValue> shippingLocationsIterator = shippingLocations.iterator();
        while (shippingLocationsIterator.hasNext()) {
            GenericValue shippingLocation = shippingLocationsIterator.next();
            contactMechId = shippingLocation.getString("contactMechId");
            GenericValue postalAddress;
            try {
                // get the postal address for this contact mech
                postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();

                // match values to compare by modifying them the same way they
                // were when they were created
                String country = ((String) context.get("shippingAddressCountry")).toUpperCase();
                String state = ((String) context.get("shippingAddressStateOrProvince")).toUpperCase();
                String city = (String) context.get("shippingAddressCityName");
                correctCityStateCountry(dispatcher, context, city, state, country);

                // TODO: The following comparison does not consider the To Name
                // or Attn: lines of the address.
                //
                // now compare values. If all fields match, that's our shipping
                // address. Return the related contact mech id.
                if (context.get("shippingAddressStreet1").toString().equals((postalAddress.get("address1").toString()))
                        && context.get("city").toString().equals((postalAddress.get("city").toString()))
                        && context.get("stateProvinceGeoId").toString().equals((postalAddress.get("stateProvinceGeoId").toString()))
                        && context.get("countryGeoId").toString().equals((postalAddress.get("countryGeoId").toString()))
                        && context.get("shippingAddressPostalCode").toString().equals((postalAddress.get("postalCode").toString()))) {
                    return contactMechId;
                }
            } catch (Exception e) {
                Debug.logError(e, "Problem with verifying postal addresses for contactMechId " + contactMechId + ".", module);
            }
        }
        // none of the existing contact mechs/postal addresses match (or none
        // were found). Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", context);
    }

    public static String setEmailContactMech(LocalDispatcher dispatcher, Delegator delegator,
            GenericValue party, GenericValue userLogin, Map<String, Object> context) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a email address purpose.
        Collection<GenericValue> emailAddressContactMechs = ContactHelper.getContactMechByPurpose(party, "OTHER_EMAIL", false);

        // check them to see if one matches
        Iterator<GenericValue> emailAddressesContactMechsIterator = emailAddressContactMechs.iterator();
        while (emailAddressesContactMechsIterator.hasNext()) {
            GenericValue emailAddressContactMech = emailAddressesContactMechsIterator.next();
            contactMechId = emailAddressContactMech.getString("contactMechId");
            // now compare values. If one matches, that's our email address.
            // Return the related contact mech id.
            if (context.get("emailBuyer").toString().equals((emailAddressContactMech.get("infoString").toString()))) {
                return contactMechId;
            }
        }
        // none of the existing contact mechs/email addresses match (or none
        // were found). Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createPartyEmail(dispatcher, partyId, (String) context.get("emailBuyer"), userLogin);
    }

    public static String setPhoneContactMech(LocalDispatcher dispatcher, Delegator delegator,
            GenericValue party, GenericValue userLogin, Map<String, Object> context) {
        String contactMechId = null;
        String partyId = (String) party.get("partyId");

        // find all contact mechs for this party with a telecom number purpose.
        Collection<GenericValue> phoneNumbers = ContactHelper.getContactMechByPurpose(party, "PHONE_SHIPPING", false);

        // check them to see if one matches
        Iterator<GenericValue> phoneNumbersIterator = phoneNumbers.iterator();
        while (phoneNumbersIterator.hasNext()) {
            GenericValue phoneNumberContactMech = phoneNumbersIterator.next();
            contactMechId = phoneNumberContactMech.getString("contactMechId");
            GenericValue phoneNumber;
            try {
                // get the phone number for this contact mech
                phoneNumber = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", contactMechId).queryOne();

                // now compare values. If one matches, that's our phone number.
                // Return the related contact mech id.
                if (context.get("shippingAddressPhone").toString()
                        .equals((phoneNumber.get("contactNumber").toString()))) {
                    return contactMechId;
                }
            } catch (GenericEntityException e) {
                Debug.logError("Problem with verifying phone number for contactMechId " + contactMechId + ".", module);
            }
        }
        // none of the existing contact mechs/email addresses match (or none
        // were found). Create a new one and return the related contact mech id.
        Debug.logInfo("Unable to find matching postal address for partyId " + partyId + ". Creating a new one.", module);
        return createPartyPhone(dispatcher, partyId, (String) context.get("shippingAddressPhone"), userLogin);
    }

    public static String retrieveProductIdFromTitle(Delegator delegator, String title) {
        String productId = "";
        try {
            // First try to get an exact match: title == internalName
            List<GenericValue> products = EntityQuery.use(delegator).from("Product").where("internalName", title).queryList();
            if (UtilValidate.isNotEmpty(products) && products.size() == 1) {
                productId = (String) (products.get(0)).get("productId");
            }
            // If it fails, attempt to get the product id from the first word of the title
            if (UtilValidate.isEmpty(productId)) {
                String titleFirstWord = null;
                if (title != null && title.indexOf(' ') != -1) {
                    titleFirstWord = title.substring(0, title.indexOf(' '));
                }
                if (UtilValidate.isNotEmpty(titleFirstWord)) {
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", titleFirstWord).queryOne();
                    if (product != null) {
                        productId = product.getString("productId");
                    }
                }
            }
        } catch (GenericEntityException e) {
            productId = "";
        }
        return productId;
    }
}
