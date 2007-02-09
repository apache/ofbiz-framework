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

package org.ofbiz.shipment.thirdparty.fedex;

import org.ofbiz.base.util.*;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Fedex Shipment Services
 * 
 * Implementation of Fedex shipment interface using Ship Manager Direct API
 * 
 * TODO: FDXShipDeleteRequest/Reply (on error and via service call)
 * TODO: FDXCloseRequest/Reply
 * TODO: FDXRateRequest/Reply
 * TODO: FDXTrackRequest/Reply
 * TODO: International shipments
 * TODO: Multi-piece shipments
 * TODO: Freight shipments
 */
public class FedexServices {

    public final static String module = FedexServices.class.getName();
    public final static String shipmentPropertiesFile = "shipment.properties";

    /**
     * Opens a URL to Fedex and makes a request.
     * 
     * @param xmlString XML message to send
     * @return XML string response from FedEx
     * @throws FedexConnectException
     */
    public static String sendFedexRequest(String xmlString) throws FedexConnectException {
        String url = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.connect.url");
        if (url == null) {
            throw new FedexConnectException("Incomplete connection URL; check your Fedex configuration");
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0" encoding="UTF-8" ?> header
        if (! xmlString.matches( "^(?s)<\\?xml\\s+version=\"1\\.0\"\\s+encoding=\"UTF-8\"\\s*\\?>.*")) {
            throw new FedexConnectException("XML header is malformed");
        }

        // prepare the connect string
        url = url.trim();

        String timeOutStr = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Fedex Connect URL : " + url, module);
            Debug.logVerbose("Fedex XML String : " + xmlString, module);
        }

        HttpClient http = new HttpClient(url);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch ( HttpClientException e) {
            Debug.logError(e, "Problem connecting to Fedex server", module);
            throw new FedexConnectException("URL Connection problem", e);
        }

        if (response == null) {
            throw new FedexConnectException("Received a null response");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Fedex Response : " + response, module);
        }

        return response;
    }

    /*
    * Register a Fedex account for shipping by obtaining the meter number
    */
    public static Map fedexSubscriptionRequest( DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        List errorList = new ArrayList();

        Boolean replaceMeterNumber = (Boolean) context.get("replaceMeterNumber");

        if(! replaceMeterNumber.booleanValue()) {
            String meterNumber = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.access.meterNumber");
            if (UtilValidate.isNotEmpty(meterNumber)) {
                return ServiceUtil.returnError("MeterNumber already exists: " + shipmentPropertiesFile + ":shipment.fedex.access.meterNumber=" + meterNumber);
            }
        }

        String companyPartyId = (String) context.get("companyPartyId");
        String contactPartyName = (String) context.get("contactPartyName");

        Map result = new HashMap();

        String accountNumber = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.access.accountNbr");
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError("accountNbr not found for Fedex subscription request.");
        }

        if(UtilValidate.isEmpty(contactPartyName)) {
            return ServiceUtil.returnError("Contact name can't be empty.");
        }

        String companyName = null;
        GenericValue postalAddress = null;
        String phoneNumber = null;
        String faxNumber = null;
        String emailAddress = null;
        try {
            
            // Make sure the company exists
            GenericValue companyParty = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", companyPartyId));
            if (companyParty == null) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not exist";
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            // Get the company name (required by Fedex)
            companyName = PartyHelper.getPartyName(companyParty);
            if (UtilValidate.isEmpty(companyName)) {
                String errorMessage = "Party with partyId " + companyPartyId + " has no name";
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            // Get the contact information for the company
            List partyContactDetails = delegator.findByAnd("PartyContactDetailByPurpose", UtilMisc.toMap("partyId", companyPartyId));
            partyContactDetails = EntityUtil.filterByDate(partyContactDetails);
            partyContactDetails = EntityUtil.filterByDate(partyContactDetails, UtilDateTime.nowTimestamp(), "purposeFromDate", "purposeThruDate", true);

            // Get the first valid postal address (address1, city, postalCode and countryGeoId are required by Fedex)
            List postalAddressConditions = new ArrayList();
            postalAddressConditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"));
            postalAddressConditions.add(new EntityExpr("address1", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(new EntityExpr("address1", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(new EntityExpr("city", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(new EntityExpr("city", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(new EntityExpr("postalCode", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(new EntityExpr("postalCode", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(new EntityExpr("countryGeoId", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(new EntityExpr("countryGeoId", EntityOperator.NOT_EQUAL, ""));
            List postalAddresses = EntityUtil.filterByCondition(partyContactDetails, new EntityConditionList(postalAddressConditions, EntityOperator.AND));

            // Fedex requires USA or Canada addresses to have a state/province ID, so filter out the ones without
            postalAddressConditions.clear();
            postalAddressConditions.add(new EntityExpr("countryGeoId", EntityOperator.IN, UtilMisc.toList("CAN", "USA")));
            postalAddressConditions.add(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, null));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, new EntityConditionList(postalAddressConditions, EntityOperator.AND));
            postalAddressConditions.clear();
            postalAddressConditions.add(new EntityExpr("countryGeoId", EntityOperator.IN, UtilMisc.toList("CAN", "USA")));
            postalAddressConditions.add(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, ""));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, new EntityConditionList(postalAddressConditions, EntityOperator.AND));

            postalAddress = EntityUtil.getFirst(postalAddresses);
            if (UtilValidate.isEmpty(postalAddress)) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not have a current, fully populated postal address";
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }
            GenericValue countryGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", postalAddress.getString("countryGeoId")));
            String countryCode = countryGeo.getString("geoCode");
            String stateOrProvinceCode = null;
            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (countryCode.equals("CA") || countryCode.equals("US")) {
                GenericValue stateProvinceGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", postalAddress.getString("stateProvinceGeoId")));
                stateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }

            // Get the first valid primary phone number (required by Fedex)
            List phoneNumberConditions = new ArrayList();
            phoneNumberConditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER"));
            phoneNumberConditions.add(new EntityExpr("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_PHONE"));
            phoneNumberConditions.add(new EntityExpr("areaCode", EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(new EntityExpr("areaCode", EntityOperator.NOT_EQUAL, ""));
            phoneNumberConditions.add(new EntityExpr("contactNumber", EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(new EntityExpr("contactNumber", EntityOperator.NOT_EQUAL, ""));
            List phoneNumbers = EntityUtil.filterByCondition(partyContactDetails, new EntityConditionList(phoneNumberConditions, EntityOperator.AND));
            GenericValue phoneNumberValue = EntityUtil.getFirst(phoneNumbers);
            if (UtilValidate.isEmpty(phoneNumberValue)) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not have a current, fully populated primary phone number";
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }
            phoneNumber = phoneNumberValue.getString("areaCode") + phoneNumberValue.getString("contactNumber");
            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(phoneNumberValue.getString("countryCode")) && !(countryCode.equals("CA") || countryCode.equals("US"))) {
                phoneNumber = phoneNumberValue.getString("countryCode") + phoneNumber;
            }
            phoneNumber = phoneNumber.replaceAll("[^+\\d]", "");

            // Get the first valid fax number
            List faxNumberConditions = new ArrayList();
            faxNumberConditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER"));
            faxNumberConditions.add(new EntityExpr("contactMechPurposeTypeId", EntityOperator.EQUALS, "FAX_NUMBER"));
            faxNumberConditions.add(new EntityExpr("areaCode", EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(new EntityExpr("areaCode", EntityOperator.NOT_EQUAL, ""));
            faxNumberConditions.add(new EntityExpr("contactNumber", EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(new EntityExpr("contactNumber", EntityOperator.NOT_EQUAL, ""));
            List faxNumbers = EntityUtil.filterByCondition(partyContactDetails, new EntityConditionList(faxNumberConditions, EntityOperator.AND));
            GenericValue faxNumberValue = EntityUtil.getFirst(faxNumbers);
            if(! UtilValidate.isEmpty(faxNumberValue)) {
                faxNumber = faxNumberValue.getString("areaCode") + faxNumberValue.getString("contactNumber");
                // Fedex doesn't want the North American country code
                if (UtilValidate.isNotEmpty(faxNumberValue.getString("countryCode")) && !(countryCode.equals("CA") || countryCode.equals("US"))) {
                    faxNumber = faxNumberValue.getString("countryCode") + faxNumber;
                }
                faxNumber = faxNumber.replaceAll("[^+\\d]", "");
            }

            // Get the first valid email address
            List emailConditions = new ArrayList();
            emailConditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
            emailConditions.add(new EntityExpr("infoString", EntityOperator.NOT_EQUAL, null));
            emailConditions.add(new EntityExpr("infoString", EntityOperator.NOT_EQUAL, ""));
            List emailAddresses = EntityUtil.filterByCondition(partyContactDetails, new EntityConditionList(emailConditions, EntityOperator.AND));
            GenericValue emailAddressValue = EntityUtil.getFirst(emailAddresses);
            if(! UtilValidate.isEmpty(emailAddressValue)) {
                emailAddress = emailAddressValue.getString("infoString");
            }

            // Get the location of the Freemarker (XML) template for the FDXSubscriptionRequest
            String templateLocation = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.template.fedex.subscription.location");
            if (UtilValidate.isEmpty(templateLocation)) {
                return ServiceUtil.returnError("Can't find location for FDXSubscriptionRequest template - should be in " + shipmentPropertiesFile + ":shipment.template.fedex.subscription.location");
            }
            
            // Populate the Freemarker context
            Map subscriptionRequestContext = new HashMap();
            subscriptionRequestContext.put("AccountNumber", accountNumber);
            subscriptionRequestContext.put("PersonName", contactPartyName);
            subscriptionRequestContext.put("CompanyName", companyName);
            subscriptionRequestContext.put("PhoneNumber", phoneNumber);
            if (UtilValidate.isNotEmpty(faxNumber)) {
                subscriptionRequestContext.put("FaxNumber", faxNumber);
            }
            if (UtilValidate.isNotEmpty(emailAddress)) {
                subscriptionRequestContext.put("EMailAddress", emailAddress);
            }
            subscriptionRequestContext.put("Line1", postalAddress.getString("address1"));
            if (UtilValidate.isNotEmpty(postalAddress.getString("address2"))) {
                subscriptionRequestContext.put("Line2", postalAddress.getString("address2"));
            }
            subscriptionRequestContext.put("City", postalAddress.getString("city"));
            if (UtilValidate.isNotEmpty(stateOrProvinceCode)) {
                subscriptionRequestContext.put("StateOrProvinceCode", stateOrProvinceCode);
            }
            subscriptionRequestContext.put("PostalCode", postalAddress.getString("postalCode"));
            subscriptionRequestContext.put("CountryCode", countryCode);

            StringWriter outWriter = new StringWriter();
            try {
                FreeMarkerWorker.renderTemplateAtLocation(templateLocation, subscriptionRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = "Cannot send Fedex subscription request: Failed to render Fedex XML Subscription Request Template [" + templateLocation + "].";
                Debug.logError(e, errorMessage, module);
                return ServiceUtil.returnError(errorMessage + ": " + e.getMessage());
            }
            String fDXSubscriptionRequestString = outWriter.toString();

            // Send the request
            String fDXSubscriptionReplyString = null;
            try {
                fDXSubscriptionReplyString = sendFedexRequest(fDXSubscriptionRequestString);
                Debug.log("Fedex response for FDXSubscriptionRequest:" + fDXSubscriptionReplyString);
            } catch (FedexConnectException e) {
                String errorMessage = "Error sending Fedex request for FDXSubscriptionRequest: " + e.toString();
                Debug.logError(e, errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            Document fDXSubscriptionReplyDocument = null;
            try {
                fDXSubscriptionReplyDocument = UtilXml.readXmlDocument(fDXSubscriptionReplyString, false);
                Debug.log("Fedex response for FDXSubscriptionRequest:" + fDXSubscriptionReplyString);
            } catch ( SAXException se) {
                String errorMessage = "Error parsing the FDXSubscriptionRequest response: " + se.toString();
                Debug.logError(se, errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            } catch ( ParserConfigurationException pce) {
                String errorMessage = "Error parsing the FDXSubscriptionRequest response: " + pce.toString();
                Debug.logError(pce, errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            } catch (IOException ioe) {
                String errorMessage = "Error parsing the FDXSubscriptionRequest response: " + ioe.toString();
                Debug.logError(ioe, errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            Element fedexSubscriptionReplyElement = fDXSubscriptionReplyDocument.getDocumentElement();
            handleErrors(fedexSubscriptionReplyElement, errorList);
    
            if (UtilValidate.isNotEmpty(errorList)) {
                return ServiceUtil.returnError(errorList);
            }
    
            String meterNumber = UtilXml.childElementValue(fedexSubscriptionReplyElement, "MeterNumber");
    
            result.put("meterNumber", meterNumber);

        } catch( GenericEntityException e ) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * 
     * Send a FDXShipRequest via the Ship Manager Direct API
     */
    public static Map fedexShipRequest( DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");        
        Locale locale = (Locale) context.get("locale");        
        Map result = ServiceUtil.returnSuccess();
        
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        // Get the location of the Freemarker (XML) template for the FDXShipRequest
        String templateLocation = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.template.fedex.ship.location");
        if (UtilValidate.isEmpty(templateLocation)) {
            return ServiceUtil.returnError("Can't find location for FDXShipRequest template - should be in " + shipmentPropertiesFile + ":shipment.template.fedex.ship.location");
        }

        // Get the Fedex account number
        String accountNumber = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.access.accountNbr");
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError("accountNbr not found for Fedex ship request.");
        }

        // Get the Fedex meter number
        String meterNumber = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.access.meterNumber");
        if (UtilValidate.isEmpty(meterNumber)) {
            return ServiceUtil.returnError("Meter number not found for Fedex ship request - should be in " + shipmentPropertiesFile + ":shipment.fedex.access.meterNumber (run the fedexSubscriptionRequest service).");
        }

        // Get the weight units to be used in the request
        String weightUomId = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.default.weight.uom");
        if (UtilValidate.isEmpty(weightUomId)) {
            return ServiceUtil.returnError("Default weightUomId not found for Fedex ship request - should be in " + shipmentPropertiesFile + ":shipment.default.weight.uom.");
        } else if (! ("WT_lb".equals(weightUomId) || "WT_kg".equals(weightUomId))) {
            return ServiceUtil.returnError("WeightUomId in " + shipmentPropertiesFile + ":shipment.default.weight.uom must be either WT_lb or WT_kg.");
        }

        // Get the dimension units to be used in the request
        String dimensionsUomId = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.default.dimension.uom");
        if (UtilValidate.isEmpty(dimensionsUomId)) {
            return ServiceUtil.returnError("Default dimensionUomId not found for Fedex ship request - should be in " + shipmentPropertiesFile + ":shipment.default.dimension.uom.");
        } else if (! ("LEN_in".equals(dimensionsUomId) || "LEN_cm".equals(dimensionsUomId))) {
            return ServiceUtil.returnError("WeightUomId in " + shipmentPropertiesFile + ":shipment.default.dimension.uom must be either LEN_in or LEN_cm.");
        }

        // Get the label image type to be returned
        String labelImageType = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.labelImageType");
        if (UtilValidate.isEmpty(labelImageType)) {
            return ServiceUtil.returnError("LabelImageType not found for Fedex ship request - should be in " + shipmentPropertiesFile + ":shipment.fedex.labelImageType.");
        } else if (! ("PDF".equals(labelImageType) || "PNG".equals(labelImageType))) {
            return ServiceUtil.returnError("LabelImageType in " + shipmentPropertiesFile + ":shipment.fedex.labelImageType must be either PDF or PNG.");
        }

        // Get the default dropoff type
        String dropoffType = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.default.dropoffType");
        if (UtilValidate.isEmpty(dropoffType)) {
            return ServiceUtil.returnError("Default dropoff type not found for Fedex ship request - should be in " + shipmentPropertiesFile + ":shipment.fedex.default.dropoffType.");
        }
        
        try {

            Map shipRequestContext = new HashMap();

            // Get the shipment and the shipmentRouteSegment
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                return ServiceUtil.returnError("Shipment not found with ID " + shipmentId);
            }
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                return ServiceUtil.returnError("ShipmentRouteSegment not found with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            
            // Determine the Fedex carrier
            String carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
            if (! "FEDEX".equals(carrierPartyId)) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not Fedex.");
            }
            
            // Check the shipmentRouteSegment's carrier status
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId")) && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be not-set or [SHRSCS_NOT_STARTED] to perform the Fedex Shipment Confirm operation.");
            }
            
            // Translate shipmentMethodTypeId to Fedex service code and carrier code
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId, "partyId", "FEDEX", "roleTypeId", "CARRIER"));
            if (UtilValidate.isEmpty(carrierShipmentMethod)) {
                return ServiceUtil.returnError("No CarrierShipmentMethod entry for carrier Fedex shipmentMethodTypeId " + shipmentMethodTypeId);
            }
            if (UtilValidate.isEmpty(carrierShipmentMethod.getString("carrierServiceCode"))) {
                return ServiceUtil.returnError("No Carrier service code for carrier Fedex shipmentMethodTypeId " + shipmentMethodTypeId);
            }
            String service = carrierShipmentMethod.getString("carrierServiceCode");
            
            // CarrierCode is FDXG only for FEDEXGROUND and GROUNDHOMEDELIVERY services.
            boolean isGroundService = service.equals("FEDEXGROUND") || service.equals("GROUNDHOMEDELIVERY");
            String carrierCode = isGroundService ? "FDXG" : "FDXE";

            // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency, and finally default to USD
            String currencyCode = null;
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                currencyCode = shipmentRouteSegment.getString("currencyUomId");
            } else if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                currencyCode = shipment.getString("currencyUomId");
            } else {
                currencyCode = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
            }
            
            // Get and validate origin postal address
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress");
            if (UtilValidate.isEmpty(originPostalAddress)) {
                return ServiceUtil.returnError("OriginPostalAddress not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            } else if (UtilValidate.isEmpty(originPostalAddress.getString("address1"))      ||
                       UtilValidate.isEmpty(originPostalAddress.getString("city"))          ||
                       UtilValidate.isEmpty(originPostalAddress.getString("postalCode"))    ||
                       UtilValidate.isEmpty(originPostalAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("OriginPostalAddress not complete for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId + " (missing address1, city, postalCode and/or countryGeoId).");
            }
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo");
            if (UtilValidate.isEmpty(originCountryGeo)) {
                return ServiceUtil.returnError("OriginCountryGeo not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            String originAddressCountryCode = originCountryGeo.getString("geoCode");
            String originAddressStateOrProvinceCode = null;
            
            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (originAddressCountryCode.equals("CA") || originAddressCountryCode.equals("US")) {
                if (UtilValidate.isEmpty(originPostalAddress.getString("stateProvinceGeoId"))) {
                    return ServiceUtil.returnError("OriginStateProvinceGeoId required in contactMechId " + originPostalAddress.getString("contactMechId") + " for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                }
                GenericValue stateProvinceGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", originPostalAddress.getString("stateProvinceGeoId")));
                originAddressStateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }
            
            // Get and validate origin telecom number
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber");
            if (UtilValidate.isEmpty(originTelecomNumber)) {
                return ServiceUtil.returnError("OriginTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            String originContactPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            
            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode")) && !(originAddressCountryCode.equals("CA") || originAddressCountryCode.equals("US"))) {
                originContactPhoneNumber = originTelecomNumber.getString("countryCode") + originContactPhoneNumber;
            }
            originContactPhoneNumber = originContactPhoneNumber.replaceAll("[^+\\d]", "");
            
            // Get the origin contact name from the owner of the origin facility
            GenericValue partyFrom = null;
            GenericValue originFacility = shipment.getRelatedOne("OriginFacility");
            if (UtilValidate.isEmpty(originFacility)) {
                return ServiceUtil.returnError("Shipment.originFacilityId is required for Fedex shipments: shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId);
            } else {
                partyFrom = originFacility.getRelatedOne("OwnerParty");
                if (UtilValidate.isEmpty(partyFrom)) {
                    return ServiceUtil.returnError("Facility.ownerPartyId is required for Fedex shipments: shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + ", facilityId " + originFacility.getString("facilityId"));
                }
            }
            
            String originContactKey = "PERSON".equals(partyFrom.getString("partyTypeId")) ? "OriginContactPersonName" : "OriginContactCompanyName";
            String originContactName = PartyHelper.getPartyName(partyFrom, false);
            if (UtilValidate.isEmpty(originContactName)) {
                return ServiceUtil.returnError("partyIdFrom for shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + " has no name (required for Fedex shipments)" );
            }
            
            // Get and validate destination postal address
            GenericValue destinationPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress");
            if (UtilValidate.isEmpty(destinationPostalAddress)) {
                return ServiceUtil.returnError("destinationPostalAddress not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            } else if (UtilValidate.isEmpty(destinationPostalAddress.getString("address1"))      ||
                       UtilValidate.isEmpty(destinationPostalAddress.getString("city"))          ||
                       UtilValidate.isEmpty(destinationPostalAddress.getString("postalCode"))    ||
                       UtilValidate.isEmpty(destinationPostalAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("destinationPostalAddress not complete for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId + " (missing address1, city, postalCode and/or countryGeoId).");
            }
            GenericValue destinationCountryGeo = destinationPostalAddress.getRelatedOne("CountryGeo");
            if (UtilValidate.isEmpty(destinationCountryGeo)) {
                return ServiceUtil.returnError("destinationCountryGeo not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            String destinationAddressCountryCode = destinationCountryGeo.getString("geoCode");
            String destinationAddressStateOrProvinceCode = null;
            
            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if (destinationAddressCountryCode.equals("CA") || destinationAddressCountryCode.equals("US")) {
                if (UtilValidate.isEmpty(destinationPostalAddress.getString("stateProvinceGeoId"))) {
                    return ServiceUtil.returnError("destinationStateProvinceGeoId required in contactMechId " + destinationPostalAddress.getString("contactMechId") + " for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                }
                GenericValue stateProvinceGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", destinationPostalAddress.getString("stateProvinceGeoId")));
                destinationAddressStateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }
            
            // Get and validate destination telecom number
            GenericValue destinationTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber");
            if (UtilValidate.isEmpty(destinationTelecomNumber)) {
                return ServiceUtil.returnError("destinationTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            String destinationContactPhoneNumber = destinationTelecomNumber.getString("areaCode") + destinationTelecomNumber.getString("contactNumber");

            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(destinationTelecomNumber.getString("countryCode")) && !(destinationAddressCountryCode.equals("CA") || destinationAddressCountryCode.equals("US"))) {
                destinationContactPhoneNumber = destinationTelecomNumber.getString("countryCode") + destinationContactPhoneNumber;
            }
            destinationContactPhoneNumber = destinationContactPhoneNumber.replaceAll("[^+\\d]", "");
            
            // Get the destination contact name
            String destinationPartyId = shipment.getString("partyIdTo");
            if (UtilValidate.isEmpty(destinationPartyId)) {
                return ServiceUtil.returnError("Shipment.partyIdTo is required for Fedex shipments: shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            GenericValue partyTo = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", destinationPartyId));
            String destinationContactKey = "PERSON".equals(partyTo.getString("partyTypeId")) ? "DestinationContactPersonName" : "DestinationContactCompanyName";
            String destinationContactName = PartyHelper.getPartyName(partyTo, false);
            if (UtilValidate.isEmpty(destinationContactName)) {
                return ServiceUtil.returnError("partyTo for shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + " has no name (required for Fedex shipments)" );
            }

            String homeDeliveryType = null;
            Timestamp homeDeliveryDate = null;
            if ("GROUNDHOMEDELIVERY".equals(service)) {
                
                // Determine the home-delivery instructions
                homeDeliveryType = shipmentRouteSegment.getString("homeDeliveryType");
                if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                    if (! (homeDeliveryType.equals("DATECERTAIN") || homeDeliveryType.equals("EVENING") || homeDeliveryType.equals("APPOINTMENT"))) {
                        return ServiceUtil.returnError("Invalid homeDeliveryType for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                    }
                }
                homeDeliveryDate = shipmentRouteSegment.getTimestamp("homeDeliveryDate");
                if (UtilValidate.isEmpty(homeDeliveryDate)) {
                    return ServiceUtil.returnError("homeDeliveryDate required for home deliveryType shipments - ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                } else if (homeDeliveryDate.before(UtilDateTime.nowTimestamp())) {
                    return ServiceUtil.returnError("homeDeliveryDate is before the current time for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                }
            }
            
            List shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", UtilMisc.toList("+shipmentPackageSeqId"));
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError("No ShipmentPackageRouteSegs (ie No Packages) found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            if (shipmentPackageRouteSegs.size() != 1) {
               return ServiceUtil.returnError("Cannot confirm shipment: fedexShipRequest service does not currently support more than one package per shipment.");
            }
            
            // TODO: Multi-piece shipments, including logic to cancel packages 1-n if FDXShipRequest n+1 fails

            // Populate the Freemarker context with the non-package-related information
            shipRequestContext.put("AccountNumber", accountNumber);
            shipRequestContext.put("MeterNumber", meterNumber);
            shipRequestContext.put("CarrierCode", carrierCode);
            shipRequestContext.put("ShipDate", UtilDateTime.nowTimestamp());
            shipRequestContext.put("ShipTime", UtilDateTime.nowTimestamp());
            shipRequestContext.put("DropoffType", dropoffType);
            shipRequestContext.put("Service", service);
            shipRequestContext.put("WeightUnits", weightUomId.equals("WT_kg") ? "KGS" : "LBS");
            shipRequestContext.put("CurrencyCode", currencyCode);
            shipRequestContext.put("PayorType", "SENDER");
            shipRequestContext.put(originContactKey, originContactName);
            shipRequestContext.put("OriginContactPhoneNumber", originContactPhoneNumber);
            shipRequestContext.put("OriginAddressLine1", originPostalAddress.getString("address1"));
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                shipRequestContext.put("OriginAddressLine2", originPostalAddress.getString("address2"));
            }
            shipRequestContext.put("OriginAddressCity", originPostalAddress.getString("city"));
            if (UtilValidate.isNotEmpty(originAddressStateOrProvinceCode)) {
                shipRequestContext.put("OriginAddressStateOrProvinceCode", originAddressStateOrProvinceCode);
            }
            shipRequestContext.put("OriginAddressPostalCode", originPostalAddress.getString("postalCode"));
            shipRequestContext.put("OriginAddressCountryCode", originAddressCountryCode);
            shipRequestContext.put(destinationContactKey, destinationContactName);
            shipRequestContext.put("DestinationContactPhoneNumber", destinationContactPhoneNumber);
            shipRequestContext.put("DestinationAddressLine1", destinationPostalAddress.getString("address1"));
            if (UtilValidate.isNotEmpty(destinationPostalAddress.getString("address2"))) {
                shipRequestContext.put("DestinationAddressLine2", destinationPostalAddress.getString("address2"));
            }
            shipRequestContext.put("DestinationAddressCity", destinationPostalAddress.getString("city"));
            if (UtilValidate.isNotEmpty(destinationAddressStateOrProvinceCode)) {
                shipRequestContext.put("DestinationAddressStateOrProvinceCode", destinationAddressStateOrProvinceCode);
            }
            shipRequestContext.put("DestinationAddressPostalCode", destinationPostalAddress.getString("postalCode"));
            shipRequestContext.put("DestinationAddressCountryCode", destinationAddressCountryCode);
            shipRequestContext.put("LabelType", "2DCOMMON"); // Required type for FDXShipRequest. Not directly in the FTL because it shouldn't be changed.
            shipRequestContext.put("LabelImageType", labelImageType);
            if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                shipRequestContext.put("HomeDeliveryType", homeDeliveryType);
            }
            if (homeDeliveryDate != null) {
                shipRequestContext.put("HomeDeliveryDate", homeDeliveryDate);
            }

            // Get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;
            Double billingWeight = shipmentRouteSegment.getDouble("billingWeight");
            String billingWeightUomId = shipmentRouteSegment.getString("billingWeightUomId");
            if ((billingWeight != null) && (billingWeight.doubleValue() > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing billingWeightUomId in shipmentId " + shipmentId + ", assuming default shipment.fedex.weightUomId of " + weightUomId + " from " + shipmentPropertiesFile,  module);
                    billingWeightUomId = weightUomId;
                }
                
                // Convert the weight if necessary
                if (! billingWeightUomId.equals(weightUomId)) {
                    Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", billingWeightUomId, "uomIdTo", weightUomId, "originalValue", billingWeight));
                    if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                        Debug.logWarning("Unable to convert billing weights for shipmentId " + shipmentId , module);
    
                        // Try getting the weight from package instead
                        hasBillingWeight = false;
                    } else {
                        billingWeight = (Double) results.get("convertedValue");
                    }
                }
            }

            // Loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            Iterator shipmentPackageRouteSegIter = shipmentPackageRouteSegs.iterator();
            while (shipmentPackageRouteSegIter.hasNext()) {

                GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegIter.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage");
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType");

                // FedEx requires the packaging type
                String packaging = null;
                if (UtilValidate.isEmpty(shipmentBoxType)) {
                    Debug.logWarning("Package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipment " + shipmentId + " has no packaging type set - defaulting to " + shipmentPropertiesFile + ":shipment.fedex.default.packagingType", module);
                    
                    // Try to get the default packaging type
                    packaging = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.fedex.default.packagingType");
                    if (UtilValidate.isEmpty(packaging)) {
                        return ServiceUtil.returnError("Cannot confirm shipment: Package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipment " + shipmentId + " has no packaging type set, and " + shipmentPropertiesFile + ":shipment.fedex.default.packagingType is not configured");
                    }
                } else {
                    packaging = shipmentBoxType.getString("shipmentBoxTypeId");
                }

                // Make sure that the packaging type is valid for FedEx
                GenericValue carrierShipmentBoxType = delegator.findByPrimaryKey("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "FEDEX", "shipmentBoxTypeId", packaging));
                if (UtilValidate.isEmpty(carrierShipmentBoxType)) {
                    return ServiceUtil.returnError("Cannot confirm shipment: Package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipment " + shipmentId + " has an invalid packaging type for FedEx.");
                } else if (UtilValidate.isEmpty(carrierShipmentBoxType.getString("packagingTypeCode"))) {
                    return ServiceUtil.returnError("Cannot confirm shipment: Package type for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipment " + shipmentId + " is missing packagingTypeCode.");
                }
                packaging = carrierShipmentBoxType.getString("packagingTypeCode");
                 
                // Determine the dimensions of the package
                Double dimensionsLength = null;
                Double dimensionsWidth = null;
                Double dimensionsHeight = null;
                if (shipmentBoxType != null) {
                    dimensionsLength = shipmentBoxType.getDouble("boxLength");
                    dimensionsWidth = shipmentBoxType.getDouble("boxWidth");
                    dimensionsHeight = shipmentBoxType.getDouble("boxHeight");

                    String boxDimensionsUomId = null;
                    GenericValue boxDimensionsUom = shipmentBoxType.getRelatedOne("DimensionUom");
                    if (! UtilValidate.isEmpty(boxDimensionsUom)) {
                        boxDimensionsUomId = boxDimensionsUom.getString("uomId");
                    } else {
                        Debug.logWarning("Packaging type for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId + " is missing dimensionUomId, assuming default shipment.default.dimension.uom of " + dimensionsUomId + " from " + shipmentPropertiesFile,  module);
                        boxDimensionsUomId = dimensionsUomId;
                    }
                    if (dimensionsLength != null && dimensionsLength.doubleValue() > 0) {
                        if (! boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsLength));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert length for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId , module);
                                dimensionsLength = null;
                            } else {
                                dimensionsLength = (Double) results.get("convertedValue");
                            }
                        }
                        
                    }
                    if (dimensionsWidth != null && dimensionsWidth.doubleValue() > 0) {
                        if (! boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsWidth));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert width for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId , module);
                                dimensionsWidth = null;
                            } else {
                                dimensionsWidth = (Double) results.get("convertedValue");
                            }
                        }
                        
                    }
                    if (dimensionsHeight != null && dimensionsHeight.doubleValue() > 0) {
                        if (! boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsHeight));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert height for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId , module);
                                dimensionsHeight = null;
                            } else {
                                dimensionsHeight = (Double) results.get("convertedValue");
                            }
                        }
                        
                    }
                }

                // Determine the package weight (possibly overriden by route segment billing weight)
                Double packageWeight = null;
                if (! hasBillingWeight) {
                    if (UtilValidate.isNotEmpty(shipmentPackage.getString("weight"))) {
                        packageWeight = shipmentPackage.getDouble("weight");
                    } else {

                        // Use default weight if available
                        try {
                            packageWeight = Double.valueOf(UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.default.weight.value"));
                        } catch (NumberFormatException ne) {
                            Debug.logWarning("Default shippable weight not configured (shipment.default.weight.value), assuming 1.0" + weightUomId , module);
                            packageWeight = new Double(1.0);
                        }
                    }
                    
                    // Convert weight if necessary
                    String packageWeightUomId = shipmentPackage.getString("weightUomId");
                    if (UtilValidate.isEmpty(packageWeightUomId)) {
                        Debug.logWarning("Shipment Route Segment missing weightUomId in shipmentId " + shipmentId + ", assuming shipment.default.weight.uom of " + weightUomId + " from " + shipmentPropertiesFile,  module);
                        packageWeightUomId = weightUomId;
                    }
                    if (! packageWeightUomId.equals(weightUomId)) {
                        Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", packageWeightUomId, "uomIdTo", weightUomId, "originalValue", packageWeight));
                        if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                            ServiceUtil.returnError("Unable to convert weight for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId);
                        } else {
                            packageWeight = (Double) results.get("convertedValue");
                        }
                    }
                }
                Double weight = hasBillingWeight ? billingWeight : packageWeight;
                if (weight == null || weight.doubleValue() < 0) {
                    ServiceUtil.returnError("Unable to determine weight for package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId);
                }
                
                // Populate the Freemarker context with package-related information
                shipRequestContext.put("CustomerReference", shipmentId + ":" + shipmentRouteSegmentId + ":" + shipmentPackage.getString("shipmentPackageSeqId"));
                shipRequestContext.put("DropoffType", dropoffType);
                shipRequestContext.put("Packaging", packaging);
                if (UtilValidate.isNotEmpty(dimensionsUomId) &&
                    dimensionsLength != null && Math.round(dimensionsLength.doubleValue()) > 0 &&
                    dimensionsWidth != null && Math.round(dimensionsWidth.doubleValue()) > 0   &&
                    dimensionsHeight != null && Math.round(dimensionsHeight.doubleValue()) > 0 ) {
                        shipRequestContext.put("DimensionsUnits", dimensionsUomId.equals("LEN_in") ? "IN" : "CM");
                        shipRequestContext.put("DimensionsLength", "" + Math.round(dimensionsLength.doubleValue()));
                        shipRequestContext.put("DimensionsWidth", "" + Math.round(dimensionsWidth.doubleValue()));
                        shipRequestContext.put("DimensionsHeight", "" + Math.round(dimensionsHeight.doubleValue()));
                }
                shipRequestContext.put("Weight", new BigDecimal(weight.doubleValue()).setScale(1, BigDecimal.ROUND_UP).toString());
            }
         
            StringWriter outWriter = new StringWriter();
            try {
                FreeMarkerWorker.renderTemplateAtLocation(templateLocation, shipRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = "Cannot confirm Fedex shipment: Failed to render Fedex XML Ship Request Template [" + templateLocation + "].";
                Debug.logError(e, errorMessage, module);
                return ServiceUtil.returnError(errorMessage + ": " + e.getMessage());
            }
            
            // Pass the request string to the sending method
            String fDXShipRequestString = outWriter.toString();
            String fDXShipReplyString = null;
            try {
                fDXShipReplyString = sendFedexRequest(fDXShipRequestString);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(fDXShipReplyString, module);
                }
            } catch (FedexConnectException e) {
                String errorMessage = "Error sending Fedex request for FDXShipRequest: ";
                Debug.logError(e, errorMessage, module);
                return ServiceUtil.returnError(errorMessage + e.toString());
            }
            
            // Pass the reply to the handler method
            return handleFedexShipReply(fDXShipReplyString, shipmentRouteSegment, shipmentPackageRouteSegs);

        } catch ( GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in fedexShipRequest service: " + e.toString());
        } catch ( GenericServiceException se) {
            Debug.logError(se, module);
            return ServiceUtil.returnError("Error in fedexShipRequest service: " + se.toString());
        }
    }

    /**
     * Extract the tracking number and shipping label from the FDXShipReply XML string
     * @param fDXShipReplyString
     * @param shipmentRouteSegment
     * @param shipmentPackageRouteSegs
     * @throws GenericEntityException
     */
    public static Map handleFedexShipReply(String fDXShipReplyString, GenericValue shipmentRouteSegment, List shipmentPackageRouteSegs) throws GenericEntityException {
        List errorList = new ArrayList();
        GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegs.get(0);

        Document fdxShipReplyDocument = null;
        try {
            fdxShipReplyDocument = UtilXml.readXmlDocument(fDXShipReplyString, false);
        } catch (SAXException se) {
            String errorMessage = "Error parsing the FDXShipReply: " + se.toString();
            Debug.logError(se, errorMessage, module);
            // TODO: Cancel the package
        } catch (ParserConfigurationException pe) {
            String errorMessage = "Error parsing the FDXShipReply: " + pe.toString();
            Debug.logError(pe, errorMessage, module);
            // TODO Cancel the package
        } catch (IOException ioe) {
            String errorMessage = "Error parsing the FDXShipReply: " + ioe.toString();
            Debug.logError(ioe, errorMessage, module);
            // TODO Cancel the package
        }
        
        if (UtilValidate.isEmpty(fdxShipReplyDocument)) {
            return ServiceUtil.returnError("Error parsing the FDXShipReply.");
        }
        
        // Tracking number: Tracking/TrackingNumber
        Element rootElement = fdxShipReplyDocument.getDocumentElement();

        handleErrors(rootElement, errorList);

        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }

        Element trackingElement = UtilXml.firstChildElement(rootElement, "Tracking");
        String trackingNumber = UtilXml.childElementValue(trackingElement, "TrackingNumber");
        
        // Label: Labels/OutboundLabel
        Element labelElement = UtilXml.firstChildElement(rootElement, "Labels");
        String encodedImageString = UtilXml.childElementValue(labelElement, "OutboundLabel");
        if (UtilValidate.isEmpty(encodedImageString)) {
            Debug.logError("Cannot find FDXShipReply label. FDXShipReply document is: " + fDXShipReplyString, module);
            return ServiceUtil.returnError("Cannot get FDXShipReply label for shipment package route segment " + shipmentPackageRouteSeg + ".  FedEx response is: " + fDXShipReplyString);
        }
        
        byte[] labelBytes = Base64.base64Decode(encodedImageString.getBytes());
        
        if (labelBytes != null) {
            
            // Store in db blob
            shipmentPackageRouteSeg.setBytes("labelImage", labelBytes);
        } else {
            Debug.log("Failed to either decode returned FedEx label or no data found in Labels/OutboundLabel.");
            // TODO: Cancel the package
        }
        
        shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
        shipmentPackageRouteSeg.set("labelHtml", encodedImageString);
        shipmentPackageRouteSeg.store();
        
        shipmentRouteSegment.set("trackingIdNumber", trackingNumber);
        shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_CONFIRMED");
        shipmentRouteSegment.store();
        
        return ServiceUtil.returnSuccess("FedEx Shipment Confirmed.");

    }
        
    public static void handleErrors(Element rootElement, List errorList) {
        Element errorElement = null;
        if ("Error".equalsIgnoreCase(rootElement.getNodeName())) {
            errorElement = rootElement;
        } else {
            errorElement = UtilXml.firstChildElement(rootElement, "Error");
        }
        if (errorElement != null) {
            Element errorCodeElement = UtilXml.firstChildElement(errorElement, "Code");
            Element errorMessageElement = UtilXml.firstChildElement(errorElement, "Message");
            if (errorCodeElement != null || errorMessageElement != null) {
                String errorCode = UtilXml.childElementValue(errorElement, "Code");
                String errorMessage = UtilXml.childElementValue( errorElement, "Message");
                if (UtilValidate.isNotEmpty(errorCode) || UtilValidate.isNotEmpty(errorMessage)) {
                    String errMsg = "An error occurred [code: " + errorCode + " [Description: " + errorMessage + "].";
                    errorList.add(errMsg);
                }
            }
        }
    }

}

class FedexConnectException extends GeneralException {
    FedexConnectException() {
        super();
    }

    FedexConnectException(String msg) {
        super(msg);
    }

    FedexConnectException(Throwable t) {
        super(t);
    }

    FedexConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}
