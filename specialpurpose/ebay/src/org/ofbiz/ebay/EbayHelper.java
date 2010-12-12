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
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbayHelper {
    private static final String configFileName = "ebayExport.properties";
    private static final String module = EbayHelper.class.getName();
    public static final String resource = "EbayUiLabels";

    public static Map<String, Object> buildEbayConfig(Map<String, Object> context, Delegator delegator) {
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
                buildEbayConfigContext.put("apiServerUrl", eBayConfig.getString("apiServerUrl"));
            }
        } else {
            buildEbayConfigContext.put("devID", UtilProperties.getPropertyValue(configFileName, "eBayExport.devID"));
            buildEbayConfigContext.put("appID", UtilProperties.getPropertyValue(configFileName, "eBayExport.appID"));
            buildEbayConfigContext.put("certID", UtilProperties.getPropertyValue(configFileName, "eBayExport.certID"));
            buildEbayConfigContext.put("token", UtilProperties.getPropertyValue(configFileName, "eBayExport.token"));
            buildEbayConfigContext.put("compatibilityLevel", UtilProperties.getPropertyValue(configFileName, "eBayExport.compatibilityLevel"));
            buildEbayConfigContext.put("siteID", UtilProperties.getPropertyValue(configFileName, "eBayExport.siteID"));
            buildEbayConfigContext.put("xmlGatewayUri", UtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri"));
            buildEbayConfigContext.put("apiServerUrl", UtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri"));
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
            GenericValue ebayShippingMethod = delegator.findOne("EbayShippingMethod", UtilMisc.toMap("shipmentMethodName", shippingService, "productStoreId", productStoreId), false);
            if (UtilValidate.isNotEmpty(ebayShippingMethod)) {
                partyId = ebayShippingMethod.getString("carrierPartyId");
                shipmentMethodTypeId = ebayShippingMethod.getString("shipmentMethodTypeId");
            }
        } catch (GenericEntityException e) {
            Debug.logInfo("Unable to find EbayShippingMethod", module);
        }
        cart.setCarrierPartyId(partyId);
        cart.setShipmentMethodTypeId(shipmentMethodTypeId);
    }

    public static boolean createPaymentFromPaymentPreferences(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            String orderId, String externalId, Timestamp orderDate, String partyIdFrom) {
        List<GenericValue> paymentPreferences = null;
        try {
            Map<String, String> paymentFields = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_RECEIVED",
                    "paymentMethodTypeId", "EXT_EBAY");
            paymentPreferences = delegator.findByAnd("OrderPaymentPreference", paymentFields);

            if (UtilValidate.isNotEmpty(paymentPreferences)) {
                Iterator<GenericValue> i = paymentPreferences.iterator();
                while (i.hasNext()) {
                    GenericValue pref = i.next();
                    boolean okay = createPayment(dispatcher, userLogin, pref, orderId, externalId, orderDate, partyIdFrom);
                    if (!okay)
                        return false;
                }
            }
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
        } catch (Exception e) {
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
            if (UtilValidate.isNotEmpty(name) && UtilValidate.isNotEmpty(userLogin)) {
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
            Map<String, Object> context = FastMap.newInstance();
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
            context = FastMap.newInstance();
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
        Map<String, Object> summaryResult = FastMap.newInstance();
        Map<String, Object> context = FastMap.newInstance();
        String phoneContactMechId = null;

        try {
            context.put("contactNumber", phoneNumber);
            context.put("partyId", partyId);
            context.put("userLogin", userLogin);
            context.put("contactMechPurposeTypeId", "PHONE_SHIPPING");
            summaryResult = dispatcher.runSync("createPartyTelecomNumber", context);
            phoneContactMechId = (String) summaryResult.get("contactMechId");
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPartyPhone", module);
        }
        return phoneContactMechId;
    }

    public static String createPartyEmail(LocalDispatcher dispatcher, String partyId, String email, GenericValue userLogin) {
        Map<String, Object> context = FastMap.newInstance();
        Map<String, Object> summaryResult = FastMap.newInstance();
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
        } catch (Exception e) {
            Debug.logError(e, "Failed to createPartyEmail", module);
        }
        return emailContactMechId;
    }

    public static void createEbayCustomer(LocalDispatcher dispatcher, String partyId, String ebayUserIdBuyer, String eias,
            GenericValue userLogin) {
        Map<String, Object> context = FastMap.newInstance();
        Map<String, Object> summaryResult = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(eias)) {
            try {
                context.put("partyId", partyId);
                context.put("attrName", "EBAY_BUYER_EIAS");
                context.put("attrValue", eias);
                context.put("userLogin", userLogin);
                summaryResult = dispatcher.runSync("createPartyAttribute", context);
            } catch (Exception e) {
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
            } catch (Exception e) {
                Debug.logError(e, "Failed to create eBay userId party attribute");
            }
        }
    }

    public static Map<String, Object> getCountryGeoId(Delegator delegator, String geoCode) {
        GenericValue geo = null;
        try {
            Debug.logInfo("geocode: " + geoCode, module);

            geo = EntityUtil.getFirst(delegator.findByAnd("Geo", UtilMisc.toMap("geoCode", geoCode.toUpperCase(),
                    "geoTypeId", "COUNTRY")));
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
        } catch (Exception e) {
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
                postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", contactMechId));

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
                phoneNumber = delegator.findByPrimaryKey("TelecomNumber", UtilMisc
                        .toMap("contactMechId", contactMechId));

                // now compare values. If one matches, that's our phone number.
                // Return the related contact mech id.
                if (context.get("shippingAddressPhone").toString()
                        .equals((phoneNumber.get("contactNumber").toString()))) {
                    return contactMechId;
                }
            } catch (Exception e) {
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
            List<GenericValue> products = delegator.findByAnd("Product", UtilMisc.toMap("internalName", title));
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
                    GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", titleFirstWord));
                    if (UtilValidate.isNotEmpty(product)) {
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
