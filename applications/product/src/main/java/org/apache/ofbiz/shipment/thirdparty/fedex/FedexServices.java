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

package org.apache.ofbiz.shipment.thirdparty.fedex;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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

    private static final String MODULE = FedexServices.class.getName();
    public static final String SHIPMENT_PROPERTIES_FILE = "shipment.properties";
    private static final String RES_ERROR = "ProductUiLabels";

    /**
     * Opens a URL to Fedex and makes a request.
     * @param xmlString XML message to send
     * @param delegator the delegator
     * @param shipmentGatewayConfigId the shipmentGatewayConfigId
     * @param resource RESOURCE file name
     * @return XML string response from FedEx
     * @throws FedexConnectException
     */
    public static String sendFedexRequest(String xmlString, Delegator delegator, String shipmentGatewayConfigId,
            String resource, Locale locale) throws FedexConnectException {
        String url = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectUrl", resource, "shipment.fedex.connect.url");
        if (UtilValidate.isEmpty(url)) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexConnectUrlIncomplete", locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0" encoding="UTF-8" ?> header
        if (!xmlString.matches("^(?s)<\\?xml\\s+version=\"1\\.0\"\\s+encoding=\"UTF-8\"\\s*\\?>.*")) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexXmlHeaderMalformed", locale));
        }

        // prepare the connect string
        url = url.trim();

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectTimeout",
                resource, "shipment.fedex.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Fedex Connect URL : " + url, MODULE);
            Debug.logVerbose("Fedex XML String : " + xmlString, MODULE);
        }

        HttpClient http = new HttpClient(url);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem connecting to Fedex server", MODULE);
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexConnectUrlProblem",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        if (response == null) {
            throw new FedexConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexReceivedNullResponse", locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Fedex Response : " + response, MODULE);
        }

        return response;
    }

    /**
     * Fedex subscription request map. Register a Fedex account for shipping by obtaining the meter number
     * @param dctx the dctx
     * @param context the context
     * @return the map
     */
    public static Map<String, Object> fedexSubscriptionRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");
        List<Object> errorList = new LinkedList<>();

        Boolean replaceMeterNumber = (Boolean) context.get("replaceMeterNumber");

        if (!replaceMeterNumber) {
            String meterNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessMeterNumber",
                    resource, "shipment.fedex.access.meterNumber");
            if (UtilValidate.isNotEmpty(meterNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexMeterNumberAlreadyExists",
                        UtilMisc.toMap("meterNumber", meterNumber), locale));
            }
        }

        String companyPartyId = (String) context.get("companyPartyId");
        String contactPartyName = (String) context.get("contactPartyName");

        Map<String, Object> result = new HashMap<>();

        String accountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessAccountNbr",
                resource, "shipment.fedex.access.accountNbr");
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexAccountNumberNotFound", locale));
        }

        if (UtilValidate.isEmpty(contactPartyName)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexContactNameCannotBeEmpty", locale));
        }

        String companyName = null;
        GenericValue postalAddress = null;
        String phoneNumber = null;
        String faxNumber = null;
        String emailAddress = null;
        try {
            // Make sure the company exists
            GenericValue companyParty = EntityQuery.use(delegator).from("Party").where("partyId", companyPartyId).cache().queryOne();
            if (companyParty == null) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not exist";
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexCompanyPartyDoesNotExists",
                        UtilMisc.toMap("companyPartyId", companyPartyId), locale));
            }

            // Get the company name (required by Fedex)
            companyName = PartyHelper.getPartyName(companyParty);
            if (UtilValidate.isEmpty(companyName)) {
                String errorMessage = "Party with partyId " + companyPartyId + " has no name";
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexCompanyPartyHasNoName",
                        UtilMisc.toMap("companyPartyId", companyPartyId), locale));
            }

            // Get the contact information for the company
            List<GenericValue> partyContactDetails = EntityQuery.use(delegator).from("PartyContactDetailByPurpose")
                    .where("partyId", companyPartyId)
                    .filterByDate(UtilDateTime.nowTimestamp(), "fromDate", "thruDate", "purposeFromDate", "purposeThruDate")
                    .queryList();

            // Get the first valid postal address (address1, city, postalCode and countryGeoId are required by Fedex)
            List<EntityCondition> postalAddressConditions = new LinkedList<>();
            postalAddressConditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"));
            postalAddressConditions.add(EntityCondition.makeCondition("address1", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition("address1", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(EntityCondition.makeCondition("city", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition("city", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(EntityCondition.makeCondition("postalCode", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition("postalCode", EntityOperator.NOT_EQUAL, ""));
            postalAddressConditions.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.NOT_EQUAL, null));
            postalAddressConditions.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.NOT_EQUAL, ""));
            List<GenericValue> postalAddresses = EntityUtil.filterByCondition(partyContactDetails,
                    EntityCondition.makeCondition(postalAddressConditions, EntityOperator.AND));

            // Fedex requires USA or Canada addresses to have a state/province ID, so filter out the ones without
            postalAddressConditions.clear();
            postalAddressConditions.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.IN, UtilMisc.toList("CAN", "USA")));
            postalAddressConditions.add(EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, null));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, EntityCondition.makeCondition(postalAddressConditions,
                    EntityOperator.AND));
            postalAddressConditions.clear();
            postalAddressConditions.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.IN, UtilMisc.toList("CAN", "USA")));
            postalAddressConditions.add(EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, ""));
            postalAddresses = EntityUtil.filterOutByCondition(postalAddresses, EntityCondition.makeCondition(postalAddressConditions,
                    EntityOperator.AND));

            postalAddress = EntityUtil.getFirst(postalAddresses);
            if (UtilValidate.isEmpty(postalAddress)) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not have a current, fully populated postal address";
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexCompanyPartyHasNotPostalAddress",
                        UtilMisc.toMap("companyPartyId", companyPartyId), locale));
            }
            GenericValue countryGeo = EntityQuery.use(delegator).from("Geo").where("geoId", postalAddress.getString("countryGeoId"))
                    .cache().queryOne();
            String countryCode = countryGeo.getString("geoCode");
            String stateOrProvinceCode = null;
            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if ("CA".equals(countryCode) || "US".equals(countryCode)) {
                GenericValue stateProvinceGeo = EntityQuery.use(delegator).from("Geo").where("geoId",
                        postalAddress.getString("stateProvinceGeoId")).cache().queryOne();
                stateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }

            // Get the first valid primary phone number (required by Fedex)
            List<EntityCondition> phoneNumberConditions = new LinkedList<>();
            phoneNumberConditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER"));
            phoneNumberConditions.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_PHONE"));
            phoneNumberConditions.add(EntityCondition.makeCondition("areaCode", EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(EntityCondition.makeCondition("areaCode", EntityOperator.NOT_EQUAL, ""));
            phoneNumberConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.NOT_EQUAL, null));
            phoneNumberConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.NOT_EQUAL, ""));
            List<GenericValue> phoneNumbers = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(phoneNumberConditions,
                    EntityOperator.AND));
            GenericValue phoneNumberValue = EntityUtil.getFirst(phoneNumbers);
            if (UtilValidate.isEmpty(phoneNumberValue)) {
                String errorMessage = "Party with partyId " + companyPartyId + " does not have a current, fully populated primary phone number";
                Debug.logError(errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexCompanyPartyHasNotPrimaryPhoneNumber",
                        UtilMisc.toMap("companyPartyId", companyPartyId), locale));
            }
            phoneNumber = phoneNumberValue.getString("areaCode") + phoneNumberValue.getString("contactNumber");
            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(phoneNumberValue.getString("countryCode")) && !("CA".equals(countryCode) || "US".equals(countryCode))) {
                phoneNumber = phoneNumberValue.getString("countryCode") + phoneNumber;
            }
            phoneNumber = phoneNumber.replaceAll("[^+\\d]", "");

            // Get the first valid fax number
            List<EntityCondition> faxNumberConditions = new LinkedList<>();
            faxNumberConditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER"));
            faxNumberConditions.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "FAX_NUMBER"));
            faxNumberConditions.add(EntityCondition.makeCondition("areaCode", EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(EntityCondition.makeCondition("areaCode", EntityOperator.NOT_EQUAL, ""));
            faxNumberConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.NOT_EQUAL, null));
            faxNumberConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.NOT_EQUAL, ""));
            List<GenericValue> faxNumbers = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(faxNumberConditions,
                    EntityOperator.AND));
            GenericValue faxNumberValue = EntityUtil.getFirst(faxNumbers);
            if (!UtilValidate.isEmpty(faxNumberValue)) {
                faxNumber = faxNumberValue.getString("areaCode") + faxNumberValue.getString("contactNumber");
                // Fedex doesn't want the North American country code
                if (UtilValidate.isNotEmpty(faxNumberValue.getString("countryCode")) && !("CA".equals(countryCode) || "US".equals(countryCode))) {
                    faxNumber = faxNumberValue.getString("countryCode") + faxNumber;
                }
                faxNumber = faxNumber.replaceAll("[^+\\d]", "");
            }

            // Get the first valid email address
            List<EntityCondition> emailConditions = new LinkedList<>();
            emailConditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
            emailConditions.add(EntityCondition.makeCondition("infoString", EntityOperator.NOT_EQUAL, null));
            emailConditions.add(EntityCondition.makeCondition("infoString", EntityOperator.NOT_EQUAL, ""));
            List<GenericValue> emailAddresses = EntityUtil.filterByCondition(partyContactDetails, EntityCondition.makeCondition(emailConditions,
                    EntityOperator.AND));
            GenericValue emailAddressValue = EntityUtil.getFirst(emailAddresses);
            if (!UtilValidate.isEmpty(emailAddressValue)) {
                emailAddress = emailAddressValue.getString("infoString");
            }

            // Get the location of the Freemarker (XML) template for the FDXSubscriptionRequest
            String templateLocation = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "templateSubscription", resource,
                    "shipment.fedex.template.subscription.location");
            if (UtilValidate.isEmpty(templateLocation)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexSubscriptionTemplateLocationNotFound",
                        UtilMisc.toMap("templateLocation", templateLocation), locale));
            }

            // Populate the Freemarker context
            Map<String, Object> subscriptionRequestContext = new HashMap<>();
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
                FreeMarkerWorker.renderTemplate(templateLocation, subscriptionRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = "Cannot send Fedex subscription request: Failed to render Fedex XML Subscription Request Template ["
                        + templateLocation + "].";
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexSubscriptionTemplateError",
                        UtilMisc.toMap("templateLocation", templateLocation, "errorString", e.getMessage()), locale));
            }
            String fDXSubscriptionRequestString = outWriter.toString();

            // Send the request
            String fDXSubscriptionReplyString = null;
            try {
                fDXSubscriptionReplyString = sendFedexRequest(fDXSubscriptionRequestString, delegator, shipmentGatewayConfigId, resource, locale);
                Debug.logInfo("Fedex response for FDXSubscriptionRequest:" + fDXSubscriptionReplyString, MODULE);
            } catch (FedexConnectException e) {
                String errorMessage = "Error sending Fedex request for FDXSubscriptionRequest: " + e.toString();
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexSubscriptionTemplateSendingError",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            Document fDXSubscriptionReplyDocument = null;
            try {
                fDXSubscriptionReplyDocument = UtilXml.readXmlDocument(fDXSubscriptionReplyString, false);
                Debug.logInfo("Fedex response for FDXSubscriptionRequest:" + fDXSubscriptionReplyString, MODULE);
            } catch (SAXException | ParserConfigurationException | IOException e) {
                String errorMessage = "Error parsing the FDXSubscriptionRequest response: " + e.toString();
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexSubscriptionTemplateParsingError",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            Element fedexSubscriptionReplyElement = fDXSubscriptionReplyDocument.getDocumentElement();
            handleErrors(fedexSubscriptionReplyElement, errorList, locale);

            if (UtilValidate.isNotEmpty(errorList)) {
                return ServiceUtil.returnError(errorList);
            }

            String meterNumber = UtilXml.childElementValue(fedexSubscriptionReplyElement, "MeterNumber");

            result.put("meterNumber", meterNumber);

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Send a FDXShipRequest via the Ship Manager Direct API
     */
    public static Map<String, Object> fedexShipRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexGatewayNotAvailable", locale));
        }

        // Get the location of the Freemarker (XML) template for the FDXShipRequest
        String templateLocation = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "templateShipment",
                resource, "shipment.fedex.template.ship.location");
        if (UtilValidate.isEmpty(templateLocation)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexShipmentTemplateLocationNotFound",
                    UtilMisc.toMap("templateLocation", templateLocation), locale));
        }

        // Get the Fedex account number
        String accountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessAccountNbr",
                resource, "shipment.fedex.access.accountNbr");
        if (UtilValidate.isEmpty(accountNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexAccountNumberNotFound", locale));
        }

        // Get the Fedex meter number
        String meterNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessMeterNumber",
                resource, "shipment.fedex.access.meterNumber");
        if (UtilValidate.isEmpty(meterNumber)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexMeterNumberNotFound",
                    UtilMisc.toMap("meterNumber", meterNumber), locale));
        }

        // Get the weight units to be used in the request
        String weightUomId = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, "shipment.default.weight.uom", delegator);
        if (UtilValidate.isEmpty(weightUomId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentDefaultWeightUomIdNotFound", locale));
        } else if (!("WT_lb".equals(weightUomId) || "WT_kg".equals(weightUomId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDefaultWeightUomIdNotValid", locale));
        }

        // Get the dimension units to be used in the request
        String dimensionsUomId = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, "shipment.default.dimension.uom", delegator);
        if (UtilValidate.isEmpty(dimensionsUomId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentDefaultDimensionUomIdNotFound", locale));
        } else if (!("LEN_in".equals(dimensionsUomId) || "LEN_cm".equals(dimensionsUomId))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentDefaultDimensionUomIdNotValid", locale));
        }

        // Get the label image type to be returned
        String labelImageType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "labelImageType",
                resource, "shipment.fedex.labelImageType");
        if (UtilValidate.isEmpty(labelImageType)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexLabelImageTypeNotFound", locale));
        } else if (!("PDF".equals(labelImageType) || "PNG".equals(labelImageType))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexLabelImageTypeNotValid", locale));
        }

        // Get the default dropoff type
        String dropoffType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "defaultDropoffType",
                resource, "shipment.fedex.default.dropoffType");
        if (UtilValidate.isEmpty(dropoffType)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexDropoffTypeNotFound", locale));
        }

        try {
            Map<String, Object> shipRequestContext = new HashMap<>();

            // Get the shipment and the shipmentRouteSegment
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (UtilValidate.isEmpty(shipment)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "ProductShipmentNotFoundId", locale) + shipmentId);
            }
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (UtilValidate.isEmpty(shipmentRouteSegment)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Determine the Fedex carrier
            String carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
            if (!"FEDEX".equals(carrierPartyId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexNotRouteSegmentCarrier",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // Check the shipmentRouteSegment's carrier status
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId"))
                    && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexRouteSegmentStatusNotStarted",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId, "shipmentRouteSegmentStatus",
                                shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            // Translate shipmentMethodTypeId to Fedex service code and carrier code
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod").where("shipmentMethodTypeId",
                    shipmentMethodTypeId, "partyId", "FEDEX", "roleTypeId", "CARRIER").queryOne();
            if (UtilValidate.isEmpty(carrierShipmentMethod)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexRouteSegmentCarrierShipmentMethodNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "carrierPartyId",
                                carrierPartyId, "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            if (UtilValidate.isEmpty(carrierShipmentMethod.getString("carrierServiceCode"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexNoCarrieServiceCode",
                        UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            String service = carrierShipmentMethod.getString("carrierServiceCode");

            // CarrierCode is FDXG only for FEDEXGROUND and GROUNDHOMEDELIVERY services.
            boolean isGroundService = "FEDEXGROUND".equals(service) || "GROUNDHOMEDELIVERY".equals(service);
            String carrierCode = isGroundService ? "FDXG" : "FDXE";

            // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency,
            // and finally default to USD
            String currencyCode = null;
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                currencyCode = shipmentRouteSegment.getString("currencyUomId");
            } else if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                currencyCode = shipment.getString("currencyUomId");
            } else {
                currencyCode = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
            }

            // Get and validate origin postal address
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (UtilValidate.isEmpty(originPostalAddress)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            } else if (UtilValidate.isEmpty(originPostalAddress.getString("address1"))
                       || UtilValidate.isEmpty(originPostalAddress.getString("city"))
                       || UtilValidate.isEmpty(originPostalAddress.getString("postalCode"))
                       || UtilValidate.isEmpty(originPostalAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginPostalAddressNotComplete",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo", false);
            if (UtilValidate.isEmpty(originCountryGeo)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            String originAddressCountryCode = originCountryGeo.getString("geoCode");
            String originAddressStateOrProvinceCode = null;

            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if ("CA".equals(originAddressCountryCode) || "US".equals(originAddressCountryCode)) {
                if (UtilValidate.isEmpty(originPostalAddress.getString("stateProvinceGeoId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentRouteSegmentOriginStateProvinceGeoIdRequired",
                            UtilMisc.toMap("contactMechId", originPostalAddress.getString("contactMechId"),
                                    "shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                }
                GenericValue stateProvinceGeo = EntityQuery.use(delegator).from("Geo").where("geoId", originPostalAddress
                        .getString("stateProvinceGeoId")).cache().queryOne();
                originAddressStateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }

            // Get and validate origin telecom number
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            if (UtilValidate.isEmpty(originTelecomNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originContactPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");

            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode")) && !("CA".equals(originAddressCountryCode)
                    || "US".equals(originAddressCountryCode))) {
                originContactPhoneNumber = originTelecomNumber.getString("countryCode") + originContactPhoneNumber;
            }
            originContactPhoneNumber = originContactPhoneNumber.replaceAll("[^+\\d]", "");

            // Get the origin contact name from the owner of the origin facility
            GenericValue partyFrom = null;
            GenericValue originFacility = shipment.getRelatedOne("OriginFacility", false);
            if (UtilValidate.isEmpty(originFacility)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexOriginFacilityRequired",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            } else {
                partyFrom = originFacility.getRelatedOne("OwnerParty", false);
                if (UtilValidate.isEmpty(partyFrom)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexOwnerPartyRequired",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId,
                                    "facilityId", originFacility.getString("facilityId")), locale));
                }
            }

            String originContactKey = "PERSON".equals(partyFrom.getString("partyTypeId")) ? "OriginContactPersonName" : "OriginContactCompanyName";
            String originContactName = PartyHelper.getPartyName(partyFrom, false);
            if (UtilValidate.isEmpty(originContactName)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexPartyFromHasNoName",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Get and validate destination postal address
            GenericValue destinationPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (UtilValidate.isEmpty(destinationPostalAddress)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            } else if (UtilValidate.isEmpty(destinationPostalAddress.getString("address1"))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString("city"))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString("postalCode"))
                       || UtilValidate.isEmpty(destinationPostalAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestPostalAddressIncomplete",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue destinationCountryGeo = destinationPostalAddress.getRelatedOne("CountryGeo", false);
            if (UtilValidate.isEmpty(destinationCountryGeo)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String destinationAddressCountryCode = destinationCountryGeo.getString("geoCode");
            String destinationAddressStateOrProvinceCode = null;

            // Only add the StateOrProvinceCode element if the address is in USA or Canada
            if ("CA".equals(destinationAddressCountryCode) || "US".equals(destinationAddressCountryCode)) {
                if (UtilValidate.isEmpty(destinationPostalAddress.getString("stateProvinceGeoId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentRouteSegmentDestStateProvinceGeoIdNotFound",
                            UtilMisc.toMap("contactMechId", destinationPostalAddress.getString("contactMechId"),
                                    "shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                }
                GenericValue stateProvinceGeo = EntityQuery.use(delegator).from("Geo").where("geoId",
                        destinationPostalAddress.getString("stateProvinceGeoId")).cache().queryOne();
                destinationAddressStateOrProvinceCode = stateProvinceGeo.getString("geoCode");
            }

            // Get and validate destination telecom number
            GenericValue destinationTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            if (UtilValidate.isEmpty(destinationTelecomNumber)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String destinationContactPhoneNumber = destinationTelecomNumber.getString("areaCode")
                    + destinationTelecomNumber.getString("contactNumber");

            // Fedex doesn't want the North American country code
            if (UtilValidate.isNotEmpty(destinationTelecomNumber.getString("countryCode")) && !("CA".equals(destinationAddressCountryCode)
                    || "US".equals(destinationAddressCountryCode))) {
                destinationContactPhoneNumber = destinationTelecomNumber.getString("countryCode") + destinationContactPhoneNumber;
            }
            destinationContactPhoneNumber = destinationContactPhoneNumber.replaceAll("[^+\\d]", "");

            // Get the destination contact name
            String destinationPartyId = shipment.getString("partyIdTo");
            if (UtilValidate.isEmpty(destinationPartyId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexPartyToRequired",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue partyTo = EntityQuery.use(delegator).from("Party").where("partyId", destinationPartyId).queryOne();
            String destinationContactKey = "PERSON".equals(partyTo.getString("partyTypeId")) ? "DestinationContactPersonName"
                    : "DestinationContactCompanyName";
            String destinationContactName = PartyHelper.getPartyName(partyTo, false);
            if (UtilValidate.isEmpty(destinationContactName)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexPartyToHasNoName",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            String homeDeliveryType = null;
            Timestamp homeDeliveryDate = null;
            if ("GROUNDHOMEDELIVERY".equals(service)) {

                // Determine the home-delivery instructions
                homeDeliveryType = shipmentRouteSegment.getString("homeDeliveryType");
                if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                    if (!("DATECERTAIN".equals(homeDeliveryType) || "EVENING".equals(homeDeliveryType) || "APPOINTMENT".equals(homeDeliveryType))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                "FacilityShipmentFedexHomeDeliveryTypeInvalid",
                                UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                    }
                }
                homeDeliveryDate = shipmentRouteSegment.getTimestamp("homeDeliveryDate");
                if (UtilValidate.isEmpty(homeDeliveryDate)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexHomeDeliveryDateRequired",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                } else if (homeDeliveryDate.before(UtilDateTime.nowTimestamp())) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexHomeDeliveryDateBeforeCurrentDate",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                }
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null,
                    UtilMisc.toList("+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (shipmentPackageRouteSegs.size() != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexMultiplePackagesNotSupported", locale));
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
            shipRequestContext.put("WeightUnits", "WT_kg".equals(weightUomId) ? "KGS" : "LBS");
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
            shipRequestContext.put("LabelType", "2DCOMMON");
            // Required type for FDXShipRequest. Not directly in the FTL because it shouldn't be changed.
            shipRequestContext.put("LabelImageType", labelImageType);
            if (UtilValidate.isNotEmpty(homeDeliveryType)) {
                shipRequestContext.put("HomeDeliveryType", homeDeliveryType);
            }
            if (homeDeliveryDate != null) {
                shipRequestContext.put("HomeDeliveryDate", homeDeliveryDate);
            }

            // Get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;
            BigDecimal billingWeight = shipmentRouteSegment.getBigDecimal("billingWeight");
            String billingWeightUomId = shipmentRouteSegment.getString("billingWeightUomId");
            if ((billingWeight != null) && (billingWeight.compareTo(BigDecimal.ZERO) > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing billingWeightUomId in shipmentId " + shipmentId
                            + ", assuming default shipment.fedex.weightUomId of " + weightUomId + " from " + SHIPMENT_PROPERTIES_FILE, MODULE);
                    billingWeightUomId = weightUomId;
                }

                // Convert the weight if necessary
                if (!billingWeightUomId.equals(weightUomId)) {
                    Map<String, Object> results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                            billingWeightUomId, "uomIdTo", weightUomId, "originalValue", billingWeight));
                    if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                        Debug.logWarning("Unable to convert billing weights for shipmentId " + shipmentId, MODULE);

                        // Try getting the weight from package instead
                        hasBillingWeight = false;
                    } else {
                        billingWeight = (BigDecimal) results.get("convertedValue");
                    }
                }
            }

            // Loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegs) {
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType", false);

                // FedEx requires the packaging type
                String packaging = null;
                if (UtilValidate.isEmpty(shipmentBoxType)) {
                    packaging = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "defaultPackagingType",
                            resource, "shipment.fedex.default.packagingType");
                    if (UtilValidate.isEmpty(packaging)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                "FacilityShipmentFedexPackingTypeNotConfigured",
                                UtilMisc.toMap("shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                        "shipmentId", shipmentId), locale));
                    }
                    Debug.logWarning("Package " + shipmentPackage.getString("shipmentPackageSeqId") + " of shipment " + shipmentId
                            + " has no packaging type set - defaulting to " + packaging, MODULE);
                } else {
                    packaging = shipmentBoxType.getString("shipmentBoxTypeId");
                }

                // Make sure that the packaging type is valid for FedEx
                GenericValue carrierShipmentBoxType = EntityQuery.use(delegator).from("CarrierShipmentBoxType").where("partyId", "FEDEX",
                        "shipmentBoxTypeId", packaging).queryOne();
                if (UtilValidate.isEmpty(carrierShipmentBoxType)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexPackingTypeInvalid",
                            UtilMisc.toMap("shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                    "shipmentId", shipmentId), locale));
                } else if (UtilValidate.isEmpty(carrierShipmentBoxType.getString("packagingTypeCode"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexPackingTypeMissing",
                            UtilMisc.toMap("shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                    "shipmentId", shipmentId), locale));
                }
                packaging = carrierShipmentBoxType.getString("packagingTypeCode");

                // Determine the dimensions of the package
                BigDecimal dimensionsLength = null;
                BigDecimal dimensionsWidth = null;
                BigDecimal dimensionsHeight = null;
                if (shipmentBoxType != null) {
                    dimensionsLength = shipmentBoxType.getBigDecimal("boxLength");
                    dimensionsWidth = shipmentBoxType.getBigDecimal("boxWidth");
                    dimensionsHeight = shipmentBoxType.getBigDecimal("boxHeight");

                    String boxDimensionsUomId = null;
                    GenericValue boxDimensionsUom = shipmentBoxType.getRelatedOne("DimensionUom", false);
                    if (!UtilValidate.isEmpty(boxDimensionsUom)) {
                        boxDimensionsUomId = boxDimensionsUom.getString("uomId");
                    } else {
                        Debug.logWarning("Packaging type for package " + shipmentPackage.getString("shipmentPackageSeqId")
                                + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId
                                + " is missing dimensionUomId, assuming default shipment.default.dimension.uom of " + dimensionsUomId
                                + " from " + SHIPMENT_PROPERTIES_FILE, MODULE);
                        boxDimensionsUomId = dimensionsUomId;
                    }
                    if (dimensionsLength != null && dimensionsLength.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                                    boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsLength));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert length for package " + shipmentPackage.getString("shipmentPackageSeqId")
                                        + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId, MODULE);
                                dimensionsLength = null;
                            } else {
                                dimensionsLength = (BigDecimal) results.get("convertedValue");
                            }
                        }

                    }
                    if (dimensionsWidth != null && dimensionsWidth.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                                    boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsWidth));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert width for package " + shipmentPackage.getString("shipmentPackageSeqId")
                                        + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId, MODULE);
                                dimensionsWidth = null;
                            } else {
                                dimensionsWidth = (BigDecimal) results.get("convertedValue");
                            }
                        }

                    }
                    if (dimensionsHeight != null && dimensionsHeight.compareTo(BigDecimal.ZERO) > 0) {
                        if (!boxDimensionsUomId.equals(dimensionsUomId)) {
                            Map<String, Object> results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                                    boxDimensionsUomId, "uomIdTo", dimensionsUomId, "originalValue", dimensionsHeight));
                            if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                                Debug.logWarning("Unable to convert height for package " + shipmentPackage.getString("shipmentPackageSeqId")
                                        + " of shipmentRouteSegment " + shipmentRouteSegmentId + " of shipment " + shipmentId, MODULE);
                                dimensionsHeight = null;
                            } else {
                                dimensionsHeight = (BigDecimal) results.get("convertedValue");
                            }
                        }

                    }
                }

                // Determine the package weight (possibly overriden by route segment billing weight)
                BigDecimal packageWeight = null;
                if (!hasBillingWeight) {
                    if (UtilValidate.isNotEmpty(shipmentPackage.getString("weight"))) {
                        packageWeight = shipmentPackage.getBigDecimal("weight");
                    } else {

                        // Use default weight if available
                        try {
                            packageWeight = EntityUtilProperties.getPropertyAsBigDecimal(SHIPMENT_PROPERTIES_FILE, "shipment.default.weight.value",
                                    BigDecimal.ZERO);
                        } catch (NumberFormatException ne) {
                            Debug.logWarning("Default shippable weight not configured (shipment.default.weight.value), assuming 1.0"
                                    + weightUomId, MODULE);
                            packageWeight = BigDecimal.ONE;
                        }
                    }

                    // Convert weight if necessary
                    String packageWeightUomId = shipmentPackage.getString("weightUomId");
                    if (UtilValidate.isEmpty(packageWeightUomId)) {
                        Debug.logWarning("Shipment Route Segment missing weightUomId in shipmentId " + shipmentId
                                + ", assuming shipment.default.weight.uom of " + weightUomId + " from " + SHIPMENT_PROPERTIES_FILE, MODULE);
                        packageWeightUomId = weightUomId;
                    }
                    if (!packageWeightUomId.equals(weightUomId)) {
                        Map<String, Object> results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                                packageWeightUomId, "uomIdTo", weightUomId, "originalValue", packageWeight));
                        if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                                    "FacilityShipmentFedexWeightOfPackageCannotBeConverted",
                                    UtilMisc.toMap("shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                            "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
                        } else {
                            packageWeight = (BigDecimal) results.get("convertedValue");
                        }
                    }
                }
                BigDecimal weight = hasBillingWeight ? billingWeight : packageWeight;
                if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentFedexWeightOfPackageNotAvailable",
                            UtilMisc.toMap("shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                    "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
                }

                // Populate the Freemarker context with package-related information
                shipRequestContext.put("CustomerReference", shipmentId + ":" + shipmentRouteSegmentId + ":" + shipmentPackage.getString(
                        "shipmentPackageSeqId"));
                shipRequestContext.put("DropoffType", dropoffType);
                shipRequestContext.put("Packaging", packaging);
                if (UtilValidate.isNotEmpty(dimensionsUomId)
                        && dimensionsLength != null && dimensionsLength.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0
                        && dimensionsWidth != null && dimensionsWidth.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0
                        && dimensionsHeight != null && dimensionsHeight.setScale(0, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) > 0) {
                    shipRequestContext.put("DimensionsUnits", "LEN_in".equals(dimensionsUomId) ? "IN" : "CM");
                    shipRequestContext.put("DimensionsLength", dimensionsLength.setScale(0, RoundingMode.HALF_UP).toString());
                    shipRequestContext.put("DimensionsWidth", dimensionsWidth.setScale(0, RoundingMode.HALF_UP).toString());
                    shipRequestContext.put("DimensionsHeight", dimensionsHeight.setScale(0, RoundingMode.HALF_UP).toString());
                }
                shipRequestContext.put("Weight", weight.setScale(1, RoundingMode.UP).toString());
            }

            StringWriter outWriter = new StringWriter();
            try {
                FreeMarkerWorker.renderTemplate(templateLocation, shipRequestContext, outWriter);
            } catch (Exception e) {
                String errorMessage = "Cannot confirm Fedex shipment: Failed to render Fedex XML Ship Request Template [" + templateLocation + "].";
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexShipmentTemplateError",
                        UtilMisc.toMap("templateLocation", templateLocation, "errorString", e.getMessage()), locale));
            }

            // Pass the request string to the sending method
            String fDXShipRequestString = outWriter.toString();
            String fDXShipReplyString = null;
            try {
                fDXShipReplyString = sendFedexRequest(fDXShipRequestString, delegator, shipmentGatewayConfigId, resource, locale);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(fDXShipReplyString, MODULE);
                }
            } catch (FedexConnectException e) {
                String errorMessage = "Error sending Fedex request for FDXShipRequest: ";
                Debug.logError(e, errorMessage, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexShipmentTemplateSendingError",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // Pass the reply to the handler method
            return handleFedexShipReply(fDXShipReplyString, shipmentRouteSegment, shipmentPackageRouteSegs, locale);

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexShipmentTemplateServiceError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    /**
     * Extract the tracking number and shipping label from the FDXShipReply XML string
     * @param fDXShipReplyString
     * @param shipmentRouteSegment
     * @param shipmentPackageRouteSegs
     * @throws GenericEntityException
     */
    public static Map<String, Object> handleFedexShipReply(String fDXShipReplyString, GenericValue shipmentRouteSegment,
            List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        List<Object> errorList = new LinkedList<>();
        GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegs.get(0);

        Document fdxShipReplyDocument = null;
        try {
            fdxShipReplyDocument = UtilXml.readXmlDocument(fDXShipReplyString, false);
        } catch (Exception e) {
            String errorMessage = "Error parsing the FDXShipReply: " + e.toString();
            Debug.logError(e, errorMessage, MODULE);
            // TODO Cancel the package
        }

        if (UtilValidate.isEmpty(fdxShipReplyDocument)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexShipmentTemplateParsingError", locale));
        }

        // Tracking number: Tracking/TrackingNumber
        Element rootElement = fdxShipReplyDocument.getDocumentElement();

        handleErrors(rootElement, errorList, locale);

        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }

        Element trackingElement = UtilXml.firstChildElement(rootElement, "Tracking");
        String trackingNumber = UtilXml.childElementValue(trackingElement, "TrackingNumber");

        // Label: Labels/OutboundLabel
        Element labelElement = UtilXml.firstChildElement(rootElement, "Labels");
        String encodedImageString = UtilXml.childElementValue(labelElement, "OutboundLabel");
        if (UtilValidate.isEmpty(encodedImageString)) {
            Debug.logError("Cannot find FDXShipReply label. FDXShipReply document is: " + fDXShipReplyString, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexShipmentTemplateLabelNotFound",
                    UtilMisc.toMap("shipmentPackageRouteSeg", shipmentPackageRouteSeg,
                            "fDXShipReplyString", fDXShipReplyString), locale));
        }

        byte[] labelBytes = Base64.getMimeDecoder().decode(encodedImageString.getBytes(StandardCharsets.UTF_8));

        if (labelBytes != null) {

            // Store in db blob
            shipmentPackageRouteSeg.setBytes("labelImage", labelBytes);
        } else {
            Debug.logInfo("Failed to either decode returned FedEx label or no data found in Labels/OutboundLabel.", MODULE);
            // TODO: Cancel the package
        }

        shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
        shipmentPackageRouteSeg.set("labelHtml", encodedImageString);
        shipmentPackageRouteSeg.store();

        shipmentRouteSegment.set("trackingIdNumber", trackingNumber);
        shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_CONFIRMED");
        shipmentRouteSegment.store();

        return ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR,
                "FacilityShipmentFedexShipmentConfirmed", locale));
    }

    public static void handleErrors(Element rootElement, List<Object> errorList, Locale locale) {
        Element errorElement = null;
        if ("Error".equalsIgnoreCase(rootElement.getNodeName())) {
            errorElement = rootElement;
        } else {
            errorElement = UtilXml.firstChildElement(rootElement, "Error");
        }
        if (UtilValidate.isNotEmpty(errorElement)) {
            Element errorCodeElement = UtilXml.firstChildElement(errorElement, "Code");
            Element errorMessageElement = UtilXml.firstChildElement(errorElement, "Message");
            if (errorCodeElement != null || errorMessageElement != null) {
                String errorCode = UtilXml.childElementValue(errorElement, "Code");
                String errorMessage = UtilXml.childElementValue(errorElement, "Message");
                if (UtilValidate.isNotEmpty(errorCode) || UtilValidate.isNotEmpty(errorMessage)) {
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentFedexErrorMessage",
                            UtilMisc.toMap("errorCode", errorCode, "errorMessage", errorMessage), locale));
                }
            }
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
            String shipmentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                GenericValue fedex = EntityQuery.use(delegator).from("ShipmentGatewayFedex").where("shipmentGatewayConfigId",
                        shipmentGatewayConfigId).queryOne();
                if (fedex != null) {
                    Object fedexField = fedex.get(shipmentGatewayConfigParameterName);
                    if (fedexField != null) {
                        returnValue = fedexField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, parameterName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName,
            String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}

@SuppressWarnings("serial")
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
