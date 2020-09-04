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
package org.apache.ofbiz.shipment.thirdparty.dhl;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * DHL ShipmentServices
 *
 * <p>Implementation of DHL US domestic shipment interface using DHL ShipIT XML APi.</p>
 *
 * Shipment services not supported in DHL ShipIT 1.1
 * <ul>
 * <li>Multiple Piece shipping (Shipment must not have more than one
 * ShipmentPackage)</li>
 * <li>Dynamic editing of previously submitted shipment (void first then
 * resubmit instead)</li>
 * <li>Label size 4"x6"</li>
 * <li>Out of origin shipping</li>
 * </ul>
 *
 * TODO: International
 */
public class DhlServices {

    private static final String MODULE = DhlServices.class.getName();
    public static final String SHIPMENT_PROPERTIES_FILE = "shipment.properties";
    public static final String DHL_WEIGHT_UOM_ID = "WT_lb"; // weight Uom used by DHL
    private static final String RES_ERROR = "ProductUiLabels";

    /**
     * Opens a URL to DHL and makes a request.
     * @param xmlString Name of the DHL service to invoke
     * @param delegator the delegator
     * @param shipmentGatewayConfigId the shipment gateway config id
     * @param resource the RESOURCE file (i.e. shipment.properties)
     * @param locale locale in use
     * @return XML string response from DHL
     * @throws DhlConnectException
     */
    public static String sendDhlRequest(String xmlString, Delegator delegator, String shipmentGatewayConfigId,
            String resource, Locale locale) throws DhlConnectException {
        String conStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectUrl", resource, "shipment.dhl.connect.url");
        if (conStr.isEmpty()) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlConnectUrlIncomplete", locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlXmlCannotBeNull", locale));
        }

        // prepare the connect string
        conStr = conStr.trim();

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectTimeout",
                resource, "shipment.dhl.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("DHL Connect URL : " + conStr, MODULE);
            Debug.logVerbose("DHL XML String : " + xmlString, MODULE);
        }

        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem connecting with DHL server", MODULE);
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlConnectUrlProblem", UtilMisc.toMap("errorString", e), locale), e);
        }

        if (response == null) {
            throw new DhlConnectException(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlReceivedNullResponse", locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("DHL Response : " + response, MODULE);
        }

        return response;
    }


    /*
     * Service to obtain a rate estimate from DHL for a shipment. Notes: Only one package per shipment currently supported by DHL ShipIT.
     * If this service returns a null shippingEstimateAmount, then the shipment has not been processed
     */
    public static Map<String, Object> dhlRateEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        // some of these can be refactored
        String carrierPartyId = (String) context.get("carrierPartyId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");

        if ("NO_SHIPPING".equals(shipmentMethodTypeId)) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("shippingEstimateAmount", null);
            return result;
        }

        // translate shipmentMethodTypeId to DHL service code
        String dhlShipmentDetailCode = null;
        try {
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                    .where("shipmentMethodTypeId", shipmentMethodTypeId, "partyId", carrierPartyId, "roleTypeId", "CARRIER")
                    .queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlNoCarrierShipmentMethod",
                        UtilMisc.toMap("carrierPartyId", carrierPartyId, "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString("carrierServiceCode");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to get rate estimate: " + e.getMessage(), MODULE);
        }

        String resource = (String) context.get("serviceConfigProps");
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");

        // shipping credentials (configured in properties)
        String userid = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "accessUserId", resource, "shipment.dhl.access.userid");
        String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "accessPassword", resource, "shipment.dhl.access.password");
        String shippingKey = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "accessShippingKey", resource, "shipment.dhl.access.shippingKey");
        String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "accessAccountNbr", resource, "shipment.dhl.access.accountNbr");
        if ((shippingKey.isEmpty()) || (accountNbr.isEmpty())) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlGatewayNotAvailable", locale));
        }

        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                shipToAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
                if (shipToAddress == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentUnableFoundShipToAddresss", locale));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        if ((shippableWeight == null) || (shippableWeight.compareTo(BigDecimal.ZERO) <= 0)) {
            String tmpValue = EntityUtilProperties.getPropertyValue(SHIPMENT_PROPERTIES_FILE, "shipment.default.weight.value", delegator);
            if (tmpValue != null) {
                try {
                    shippableWeight = new BigDecimal(tmpValue);
                } catch (Exception e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentDhlDefaultShippableWeightNotConfigured", locale));
                }
            }
        }

        // TODO: if a weight UOM is passed in, use convertUom service to convert it here
        if (shippableWeight.compareTo(BigDecimal.ONE) < 0) {
            Debug.logWarning("DHL Estimate: Weight is less than 1 lb, submitting DHL minimum of 1 lb for estimate.", MODULE);
            shippableWeight = BigDecimal.ONE;
        }
        if (("G".equals(dhlShipmentDetailCode) && shippableWeight.compareTo(new BigDecimal("999")) > 0)
                || (shippableWeight.compareTo(new BigDecimal("150")) > 0)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlShippableWeightExceed", locale));
        }
        String weight = shippableWeight.toString();

        // create AccessRequest XML doc using FreeMarker template
        String templateName = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "rateEstimateTemplate", resource, "shipment.dhl.template.rate.estimate");
        if (templateName.trim().isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlShipmentTemplateLocationNotFound", locale));
        }
        StringWriter outWriter = new StringWriter();
        Map<String, Object> inContext = new HashMap<>();
        inContext.put("action", "RateEstimate");
        inContext.put("userid", userid);
        inContext.put("password", password);
        inContext.put("accountNbr", accountNbr);
        inContext.put("shippingKey", shippingKey);
        inContext.put("shipDate", UtilDateTime.nowTimestamp());
        inContext.put("dhlShipmentDetailCode", dhlShipmentDetailCode);
        inContext.put("weight", weight);
        inContext.put("state", shipToAddress.getString("stateProvinceGeoId"));
        // DHL ShipIT API does not accept ZIP+4
        if ((shipToAddress.getString("postalCode") != null) && (shipToAddress.getString("postalCode").length() > 5)) {
            inContext.put("postalCode", shipToAddress.getString("postalCode").substring(0, 5));
        } else {
            inContext.put("postalCode", shipToAddress.getString("postalCode"));
        }
        try {
            ContentWorker.renderContentAsText(dispatcher, templateName, outWriter, inContext, locale, "text/plain", null, null, false);
        } catch (Exception e) {
            Debug.logError(e, "Cannot get DHL Estimate: Failed to render DHL XML Request.", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlShipmentTemplateError", locale));
        }
        String requestString = outWriter.toString();
        if (Debug.verboseOn()) {
            Debug.logVerbose(requestString, MODULE);
        }

        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
            if (Debug.verboseOn()) {
                Debug.logVerbose(rateResponseString, MODULE);
            }
        } catch (DhlConnectException e) {
            String uceErrMsg = "Error sending DHL request for DHL Service Rate: " + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlShipmentTemplateSendingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
            return handleDhlRateResponse(rateResponseDocument, locale);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexShipmentTemplateParsingError",
                    UtilMisc.toMap("errorString", e2.toString()), locale));
        }
    }

    /*
     * Parses an XML document from DHL to get the rate estimate
     */
    public static Map<String, Object> handleDhlRateResponse(Document rateResponseDocument, Locale locale) {
        List<Object> errorList = new LinkedList<>();
        Map<String, Object> dhlRateCodeMap = new HashMap<>();
        // process RateResponse
        Element rateResponseElement = rateResponseDocument.getDocumentElement();
        DhlServices.handleErrors(rateResponseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, "Shipment");
        Element responseEstimateDetailElement = UtilXml.firstChildElement(responseElement, "EstimateDetail");

        DhlServices.handleErrors(responseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }

        String dateGenerated = UtilXml.childElementValue(
                responseEstimateDetailElement, "DateGenerated");

        Element responseServiceLevelCommitmentElement = UtilXml.firstChildElement(responseEstimateDetailElement,
                    "ServiceLevelCommitment");
        String responseServiceLevelCommitmentDescription = UtilXml.childElementValue(responseServiceLevelCommitmentElement,
                    "Desc");

        Element responseRateEstimateElement = UtilXml.firstChildElement(
                responseEstimateDetailElement, "RateEstimate");
        String responseTotalChargeEstimate = UtilXml.childElementValue(
                responseRateEstimateElement, "TotalChargeEstimate");
        Element responseChargesElement = UtilXml.firstChildElement(
                responseRateEstimateElement, "Charges");
        List<? extends Element> chargeNodeList = UtilXml.childElementList(responseChargesElement,
                "Charge");

        List<Map<String, String>> chargeList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(chargeNodeList)) {
            for (Element responseChargeElement: chargeNodeList) {
                Map<String, String> charge = new HashMap<>();

                Element responseChargeTypeElement = UtilXml.firstChildElement(
                        responseChargeElement, "Type");

                String responseChargeTypeCode = UtilXml.childElementValue(
                        responseChargeTypeElement, "Code");
                String responseChargeTypeDesc = UtilXml.childElementValue(
                        responseChargeTypeElement, "Desc");
                String responseChargeValue = UtilXml.childElementValue(
                        responseChargeElement, "Value");

                charge.put("chargeTypeCode", responseChargeTypeCode);
                charge.put("chargeTypeDesc", responseChargeTypeDesc);
                charge.put("chargeValue", responseChargeValue);
                chargeList.add(charge);
            }
        }
        BigDecimal shippingEstimateAmount = new BigDecimal(responseTotalChargeEstimate);
        dhlRateCodeMap.put("dateGenerated", dateGenerated);
        dhlRateCodeMap.put("serviceLevelCommitment",
                responseServiceLevelCommitmentDescription);
        dhlRateCodeMap.put("totalChargeEstimate", responseTotalChargeEstimate);
        dhlRateCodeMap.put("chargeList", chargeList);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shippingEstimateAmount", shippingEstimateAmount);
        result.put("dhlRateCodeMap", dhlRateCodeMap);
        return result;
    }

    /*
     * Register a DHL account for shipping by obtaining the DHL shipping key
     */
    public static Map<String, Object> dhlRegisterInquire(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String resource = (String) context.get("serviceConfigProps");
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result;
        String postalCode = (String) context.get("postalCode");
        String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessAccountNbr",
                resource, "shipment.dhl.access.accountNbr");
        if (accountNbr.isEmpty()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlAccessAccountNbrMandotoryForRegisterAccount", locale));
        }
        // create AccessRequest XML doc
        Document requestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
        String requestString = null;
        Element requesElement = requestDocument.getDocumentElement();

        Element registerRequestElement = UtilXml.addChildElement(requesElement, "Register", requestDocument);
        registerRequestElement.setAttribute("version", "1.0");
        registerRequestElement.setAttribute("action", "ShippingKey");
        UtilXml.addChildElementValue(registerRequestElement, "AccountNbr", accountNbr, requestDocument);
        UtilXml.addChildElementValue(registerRequestElement, "PostalCode", postalCode, requestDocument);

        try {
            requestString = UtilXml.writeXmlDocument(requestDocument);
            Debug.logInfo("AccessRequest XML Document:" + requestString, MODULE);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlErrorAccessRequestXmlToString",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
        // send the request
        String registerResponseString = null;
        try {
            registerResponseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
            Debug.logInfo("DHL request for DHL Register Account:" + registerResponseString, MODULE);
        } catch (DhlConnectException e) {
            String uceErrMsg = "Error sending DHL request for DHL Register Account: " + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlErrorSendingRequestRegisterAccount",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Document registerResponseDocument = null;
        try {
            registerResponseDocument = UtilXml.readXmlDocument(registerResponseString, false);
            result = handleDhlRegisterResponse(registerResponseDocument, locale);
            Debug.logInfo("DHL response for DHL Register Account:" + registerResponseString, MODULE);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RegisterAccountServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlErrorParsingRegisterAccountResponse",
                    UtilMisc.toMap("errorString", e2.toString()), locale));
        }

        return result;
    }

    /*
     * Parse response from DHL registration request to get shipping key
     */
    public static Map<String, Object> handleDhlRegisterResponse(Document registerResponseDocument, Locale locale) {
        List<Object> errorList = new LinkedList<>();
        // process RegisterResponse
        Element registerResponseElement = registerResponseDocument.getDocumentElement();
        DhlServices.handleErrors(registerResponseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(registerResponseElement, "Register");
        DhlServices.handleErrors(responseElement, errorList, locale);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        String responseShippingKey = UtilXml.childElementValue(responseElement, "ShippingKey");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shippingKey", responseShippingKey);
        return result;
    }

    /*
     * Pass a shipment request to DHL via ShipIT and get a tracking number and a label back, among other things
     */

    public static Map<String, Object> dhlShipmentConfirm(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlGatewayNotAvailable", locale));
        }

        try {
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "ProductShipmentNotFoundId", locale) + shipmentId);
            }
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            if (!"DHL".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlNotRouteSegmentCarrier",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all DHL services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId"))
                    && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlRouteSegmentStatusNotStarted",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId,
                                "shipmentRouteSegmentStatus", shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            // don't put on country code if not specified or is the US country code (UPS wants it this way and assuming DHL will accept this)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode"))
                    && !"001".equals(originTelecomNumber.getString("countryCode"))) {
                originPhoneNumber = originTelecomNumber.getString("countryCode") + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, "-", "");
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, " ", "");

            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo", false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentOriginCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // DHL requires destination phone number, default to sender # if no customer number
            String destPhoneNumber = originPhoneNumber;
            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            if (destTelecomNumber != null) {
                destPhoneNumber = destTelecomNumber.getString("areaCode") + destTelecomNumber.getString("contactNumber");
                // don't put on country code if not specified or is the US country code (UPS wants it this way)
                if (UtilValidate.isNotEmpty(destTelecomNumber.getString("countryCode"))
                        && !"001".equals(destTelecomNumber.getString("countryCode"))) {
                    destPhoneNumber = destTelecomNumber.getString("countryCode") + destPhoneNumber;
                }
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, "-", "");
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, " ", "");
            }

            String recipientEmail = null;
            Map<String, Object> results = dispatcher.runSync("getPartyEmail", UtilMisc.toMap("partyId",
                    shipment.get("partyIdTo"), "userLogin", userLogin));
            if (ServiceUtil.isError(results)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
            }
            if (results.get("emailAddress") != null) {
                recipientEmail = (String) results.get("emailAddress");
            }

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne("CountryGeo", false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRouteSegmentDestCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg",
                    null, UtilMisc.toList("+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (shipmentPackageRouteSegs.size() != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlMultiplePackagesNotSupported", locale));
            }

            // get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;  // for later overrides
            BigDecimal billingWeight = shipmentRouteSegment.getBigDecimal("billingWeight");
            String billingWeightUomId = shipmentRouteSegment.getString("billingWeightUomId");
            if ((billingWeight != null) && (billingWeight.compareTo(BigDecimal.ZERO) > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing billingWeightUomId in shipmentId " + shipmentId, MODULE);
                    billingWeightUomId = "WT_lb"; // TODO: this should be specified in a properties file
                }
                // convert
                results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", billingWeightUomId, "uomIdTo",
                        DHL_WEIGHT_UOM_ID, "originalValue", billingWeight));
                if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                    Debug.logWarning("Unable to convert billing weights for shipmentId " + shipmentId, MODULE);
                    // try getting the weight from package instead
                    hasBillingWeight = false;
                } else {
                    billingWeight = (BigDecimal) results.get("convertedValue");
                }
            }

            // loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            BigDecimal packageWeight = null;
            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegs) {
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType", false);

                //if (shipmentBoxType != null) {
                    // TODO: determine what default UoM is (assuming inches) - there should be a defaultDimensionUomId in Facility
                //}

                // next step is weight determination, so skip if we have a billing weight
                if (hasBillingWeight) continue;

                // compute total packageWeight (for now, just one package)
                if (shipmentPackage.getString("weight") != null) {
                    packageWeight = new BigDecimal(shipmentPackage.getString("weight"));
                } else {
                    // use default weight if available
                    try {
                        packageWeight = EntityUtilProperties.getPropertyAsBigDecimal(SHIPMENT_PROPERTIES_FILE,
                                "shipment.default.weight.value", BigDecimal.ZERO);
                    } catch (NumberFormatException ne) {
                        Debug.logWarning("Default shippable weight not configured (shipment.default.weight.value)", MODULE);
                        packageWeight = BigDecimal.ONE;
                    }
                }
                // convert weight
                String weightUomId = (String) shipmentPackage.get("weightUomId");
                if (weightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing weightUomId in shipmentId " + shipmentId, MODULE);
                    weightUomId = "WT_lb"; // TODO: this should be specified in a properties file
                }
                results = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", weightUomId, "uomIdTo",
                        DHL_WEIGHT_UOM_ID, "originalValue", packageWeight));
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
                        || (results.get("convertedValue") == null)) {
                    Debug.logWarning("Unable to convert weights for shipmentId " + shipmentId, MODULE);
                    packageWeight = BigDecimal.ONE;
                } else {
                    packageWeight = (BigDecimal) results.get("convertedValue");
                }
            }

            // pick which weight to use and round it
            BigDecimal weight = null;
            if (hasBillingWeight) {
                weight = billingWeight;
            } else {
                weight = packageWeight;
            }
            // want the rounded weight as a string, so we use the "" + int shortcut
            String roundedWeight = weight.setScale(0, RoundingMode.HALF_UP).toPlainString();

            // translate shipmentMethodTypeId to DHL service code
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String dhlShipmentDetailCode = null;
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                    .where("shipmentMethodTypeId", shipmentMethodTypeId, "partyId", "DHL", "roleTypeId", "CARRIER")
                    .queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlNoCarrierShipmentMethod",
                        UtilMisc.toMap("carrierPartyId", "DHL", "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString("carrierServiceCode");

            // shipping credentials (configured in properties)
            String userid = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "accessUserId", resource, "shipment.dhl.access.userid");
            String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "accessPassword", resource, "shipment.dhl.access.password");
            String shippingKey = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "accessShippingKey", resource, "shipment.dhl.access.shippingKey");
            String accountNbr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "accessAccountNbr", resource, "shipment.dhl.access.accountNbr");
            if ((shippingKey.isEmpty()) || (accountNbr.isEmpty())) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlGatewayNotAvailable", locale));
            }

            // label image preference (PNG or GIF)
            String labelImagePreference = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "labelImageFormat", resource, "shipment.dhl.label.image.format");
            if (labelImagePreference.isEmpty()) {
                Debug.logInfo("shipment.dhl.label.image.format not specified, assuming PNG", MODULE);
                labelImagePreference = "PNG";
            } else if (!("PNG".equals(labelImagePreference) || "GIF".equals(labelImagePreference))) {
                Debug.logError("Illegal shipment.dhl.label.image.format: " + labelImagePreference, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlUnknownLabelImageFormat",
                        UtilMisc.toMap("labelImagePreference", labelImagePreference), locale));
            }

            // create AccessRequest XML doc using FreeMarker template
            String templateName = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                    "rateEstimateTemplate", resource, "shipment.dhl.template.rate.estimate");
            if ((templateName.trim().isEmpty())) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentDhlRateEstimateTemplateNotConfigured", locale));
            }
            StringWriter outWriter = new StringWriter();
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("action", "GenerateLabel");
            inContext.put("userid", userid);
            inContext.put("password", password);
            inContext.put("accountNbr", accountNbr);
            inContext.put("shippingKey", shippingKey);
            inContext.put("shipDate", UtilDateTime.nowTimestamp());
            inContext.put("dhlShipmentDetailCode", dhlShipmentDetailCode);
            inContext.put("weight", roundedWeight);
            inContext.put("senderPhoneNbr", originPhoneNumber);
            inContext.put("companyName", destPostalAddress.getString("toName"));
            inContext.put("attnTo", destPostalAddress.getString("attnName"));
            inContext.put("street", destPostalAddress.getString("address1"));
            inContext.put("streetLine2", destPostalAddress.getString("address2"));
            inContext.put("city", destPostalAddress.getString("city"));
            inContext.put("state", destPostalAddress.getString("stateProvinceGeoId"));

            // DHL ShipIT API does not accept ZIP+4
            if ((destPostalAddress.getString("postalCode") != null) && (destPostalAddress.getString("postalCode").length() > 5)) {
                inContext.put("postalCode", destPostalAddress.getString("postalCode").substring(0, 5));
            } else {
                inContext.put("postalCode", destPostalAddress.getString("postalCode"));
            }
            inContext.put("phoneNbr", destPhoneNumber);
            inContext.put("labelImageType", labelImagePreference);
            inContext.put("shipperReference", shipment.getString("primaryOrderId") + "-" + shipment.getString("primaryShipGroupSeqId"));
            inContext.put("notifyEmailAddress", recipientEmail);

            try {
                ContentWorker.renderContentAsText(dispatcher, templateName, outWriter, inContext, locale, "text/plain", null, null, false);
            } catch (Exception e) {
                Debug.logError(e, "Cannot confirm DHL shipment: Failed to render DHL XML Request.", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexRateTemplateRenderingError", locale));
            }
            String requestString = outWriter.toString();
            if (Debug.verboseOn()) {
                Debug.logVerbose(requestString, MODULE);
            }

            // send the request
            String responseString = null;
            try {
                responseString = sendDhlRequest(requestString, delegator, shipmentGatewayConfigId, resource, locale);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(responseString, MODULE);
                }
            } catch (DhlConnectException e) {
                String uceErrMsg = "Error sending DHL request for DHL Service Rate: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentFedexRateTemplateSendingError",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
            // pass to handler method
            return handleDhlShipmentConfirmResponse(responseString, shipmentRouteSegment, shipmentPackageRouteSegs, locale);
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentFedexRateTemplateReadingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    // NOTE: Must VOID shipments on errors
    public static Map<String, Object> handleDhlShipmentConfirmResponse(String rateResponseString, GenericValue shipmentRouteSegment,
            List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegs.get(0);

        // TODO: figure out how to handle validation on return XML, which can be mangled
        // Ideas: try again right away, let user try again, etc.
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            // TODO: VOID
        }

        // tracking number: Shipment/ShipmentDetail/AirbillNbr
        Element rootElement = rateResponseDocument.getDocumentElement();
        Element shipmentElement = UtilXml.firstChildElement(rootElement, "Shipment");
        Element shipmentDetailElement = UtilXml.firstChildElement(shipmentElement, "ShipmentDetail");
        String trackingNumber = UtilXml.childElementValue(shipmentDetailElement, "AirbillNbr");

        // label: Shipment/Label/Image
        Element labelElement = UtilXml.firstChildElement(shipmentElement, "Label");
        String encodedImageString = UtilXml.childElementValue(labelElement, "Image");
        if (encodedImageString == null) {
            Debug.logError("Cannot find response DHL shipment label.  Rate response document is: " + rateResponseString, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentDhlShipmentLabelError",
                    UtilMisc.toMap("shipmentPackageRouteSeg", shipmentPackageRouteSeg,
                            "rateResponseString", rateResponseString), locale));
        }

        // TODO: this is a temporary hack to replace the newlines so that Base64 likes the input This is NOT platform independent
        int size = encodedImageString.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (encodedImageString.charAt(i) == '\n') {
                continue;
            }
            sb.append(encodedImageString.charAt(i));
        }
        byte[] labelBytes = Base64.getMimeDecoder().decode(sb.toString().getBytes(StandardCharsets.UTF_8));

        if (labelBytes != null) {
            // store in db blob
            shipmentPackageRouteSeg.setBytes("labelImage", labelBytes);
        } else {
            Debug.logInfo("Failed to either decode returned DHL label or no data found in eCommerce/Shipment/Label/Image.", MODULE);
            // TODO: VOID
        }

        shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
        shipmentPackageRouteSeg.set("labelHtml", sb.toString());
        shipmentPackageRouteSeg.store();

        shipmentRouteSegment.set("trackingIdNumber", trackingNumber);
        shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_CONFIRMED");
        shipmentRouteSegment.store();

        return ServiceUtil.returnSuccess(UtilProperties.getMessage(RES_ERROR,
                "FacilityShipmentDhlShipmentConfirmed", locale));
    }


    public static Document createAccessRequestDocument(Delegator delegator, String shipmentGatewayConfigId, String resource) {
        Document eCommerceRequestDocument = UtilXml.makeEmptyXmlDocument("eCommerce");
        Element eCommerceRequesElement = eCommerceRequestDocument.getDocumentElement();
        eCommerceRequesElement.setAttribute("version", getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                "headVersion", resource, "shipment.dhl.head.version"));
        eCommerceRequesElement.setAttribute("action", getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "headAction", resource, "shipment.dhl.head.action"));
        Element requestorRequestElement = UtilXml.addChildElement(eCommerceRequesElement, "Requestor", eCommerceRequestDocument);
        UtilXml.addChildElementValue(requestorRequestElement, "ID", getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "accessUserId", resource, "shipment.dhl.access.userid"),
                eCommerceRequestDocument);
        UtilXml.addChildElementValue(requestorRequestElement, "Password", getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "accessPassword", resource, "shipment.dhl.access.password"),
                eCommerceRequestDocument);
        return eCommerceRequestDocument;
    }

    public static void handleErrors(Element responseElement, List<Object> errorList, Locale locale) {
        Element faultsElement = UtilXml.firstChildElement(responseElement,
                "Faults");
        List<? extends Element> faultElements = UtilXml.childElementList(faultsElement, "Fault");
        if (UtilValidate.isNotEmpty(faultElements)) {
            for (Element errorElement: faultElements) {
                StringBuilder errorMessageBuf = new StringBuilder();

                String errorCode = UtilXml.childElementValue(errorElement, "Code");
                String errorDescription = UtilXml.childElementValue(errorElement, "Desc");
                String errorSource = UtilXml.childElementValue(errorElement, "Source");
                if (UtilValidate.isEmpty(errorSource)) {
                    errorSource = UtilXml.childElementValue(errorElement, "Context");
                }
                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentDhlErrorMessage",
                        UtilMisc.toMap("errorCode", errorCode, "errorDescription", errorDescription), locale));
                if (UtilValidate.isNotEmpty(errorSource)) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR,
                            "FacilityShipmentDhlErrorMessageElement",
                            UtilMisc.toMap("errorSource", errorSource), locale));
                }
                errorList.add(errorMessageBuf.toString());
            }
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String
            shipmentGatewayConfigParameterName, String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                GenericValue dhl = EntityQuery.use(delegator).from("ShipmentGatewayDhl").where("shipmentGatewayConfigId",
                        shipmentGatewayConfigId).queryOne();
                if (UtilValidate.isNotEmpty(dhl)) {
                    Object dhlField = dhl.get(shipmentGatewayConfigParameterName);
                    if (dhlField != null) {
                        returnValue = dhlField.toString().trim();
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
            shipmentGatewayConfigParameterName, String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName,
                resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}
@SuppressWarnings("serial")
class DhlConnectException extends GeneralException {
    DhlConnectException() {
        super();
    }

    DhlConnectException(String msg) {
        super(msg);
    }

    DhlConnectException(Throwable t) {
        super(t);
    }

    DhlConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}
