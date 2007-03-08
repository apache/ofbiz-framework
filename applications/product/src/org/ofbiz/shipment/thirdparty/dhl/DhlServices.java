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
package org.ofbiz.shipment.thirdparty.dhl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Base64;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.service.ModelService;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
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

    public final static String module = DhlServices.class.getName();
    public final static String shipmentPropertiesFile = "shipment.properties";
    public final static String DHL_WEIGHT_UOM_ID = "WT_lb"; // weight Uom used by DHL

    /**
     * Opens a URL to DHL and makes a request.
     * 
     * @param xmlString
     *            Name of the DHL service to invoke
     * @param xmlString
     *            XML message to send
     * @return XML string response from DHL
     * @throws DhlConnectException
     */
    public static String sendDhlRequest(String xmlString)
            throws DhlConnectException {
        String conStr = UtilProperties.getPropertyValue(shipmentPropertiesFile,
                "shipment.dhl.connect.url");
        if (conStr == null) {
            throw new DhlConnectException(
                    "Incomplete connection URL; check your DHL configuration");
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new DhlConnectException("XML message cannot be null");
        }

        // prepare the connect string
        conStr = conStr.trim();

        String timeOutStr = UtilProperties.getPropertyValue(
                shipmentPropertiesFile, "shipment.dhl.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr
                    + " using default " + timeout);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("DHL Connect URL : " + conStr, module);
            Debug.logVerbose("DHL XML String : " + xmlString, module);
        }

        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem connecting with DHL server", module);
            throw new DhlConnectException("URL Connection problem", e);
        }

        if (response == null) {
            throw new DhlConnectException("Received a null response");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("DHL Response : " + response, module);
        }

        return response;
    }


    /*
     * Service to obtain a rate estimate from DHL for a shipment. Notes: Only one package per shipment currently supported by DHL ShipIT.
     * If this service returns a null shippingEstimateAmount, then the shipment has not been processed
     */
    public static Map dhlRateEstimate(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        // some of these can be refactored
        String upsRateInquireMode = (String) context.get("upsRateInquireMode");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        List shippableItemInfo = (List) context.get("shippableItemInfo");
        Double shippableTotal = (Double) context.get("shippableTotal");
        Double shippableQuantity = (Double) context.get("shippableQuantity");
        Double shippableWeight = (Double) context.get("shippableWeight");
        
        if (shipmentMethodTypeId.equals("NO_SHIPPING")) {
            Map result = ServiceUtil.returnSuccess();
            result.put("shippingEstimateAmount", null);
            return result;
        }

        // translate shipmentMethodTypeId to DHL service code
        String dhlShipmentDetailCode = null;
        try {
            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId,
                    "partyId", carrierPartyId, "roleTypeId", "CARRIER"));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("No CarrierShipmentMethod entry for carrier " + carrierPartyId + ", shipmentMethodTypeId " + shipmentMethodTypeId);
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString("carrierServiceCode");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failed to get rate estimate: " + e.getMessage(), module);
        }

        // shipping credentials (configured in properties)
        String userid = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.dhl.access.userid");
        String password = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.dhl.access.password");
        String shippingKey = UtilProperties.getPropertyValue("shipment", "shipment.dhl.access.shippingKey");
        String accountNbr = UtilProperties.getPropertyValue("shipment", "shipment.dhl.access.accountNbr");
        if ((shippingKey == null) || (accountNbr == null) || (shippingKey.length() == 0) || (accountNbr.length() == 0)) {
            return ServiceUtil.returnError("DHL Shipping Credentials are not configured. (check shipment.dhl.access)");
        }

        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                shipToAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
                if (shipToAddress == null) {
                    return ServiceUtil.returnError("Unable to determine ship-to address");
                }
            }
            catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        if ((shippableWeight == null) || (shippableWeight.doubleValue() <= 0.0)) {
            String tmpValue = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.default.weight.value");
            if (tmpValue != null) {
                try {
                    shippableWeight = new Double(tmpValue);
                } catch (Exception e) {
                    return ServiceUtil.returnError("Cannot get DHL Estimate: Default shippable weight not configured (shipment.default.weight.value)");
                }
            }
        }

        // TODO: if a weight UOM is passed in, use convertUom service to convert it here
        if (shippableWeight.doubleValue() < 1.0) {
            Debug.logWarning("DHL Estimate: Weight is less than 1 lb, submitting DHL minimum of 1 lb for estimate.", module);
            shippableWeight = new Double(1.0);
        }
        if ((dhlShipmentDetailCode.equals("G") && shippableWeight.doubleValue() > 999) || (shippableWeight.doubleValue() > 150)) {
            return ServiceUtil.returnError("Cannot get DHL Estimate: Shippable weight cannot be greater than 999 lbs for ground or 150 lbs for all other services.");
        }
        String weight = (new Integer((int) shippableWeight.longValue())).toString();

        // create AccessRequest XML doc using FreeMarker template
        String templateName = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.template.dhl.rate.estimate");
        if ((templateName == null) || (templateName.trim().length() == 0)) {
            return ServiceUtil.returnError("Cannot get DHL Estimate: DHL Rate template not configured (shipment.template.dhl.rate.estimate");
        }
        StringWriter outWriter = new StringWriter();
        Map inContext = new HashMap();
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
            inContext.put("postalCode", shipToAddress.getString("postalCode").substring(0,5));
        } else {
            inContext.put("postalCode", shipToAddress.getString("postalCode"));
        }
        try {
            ContentWorker.renderContentAsText(dispatcher, delegator, templateName, outWriter, inContext, locale, "text/plain", false);
        } catch (Exception e) {
            Debug.logError(e, "Cannot get DHL Estimate: Failed to render DHL XML Request.", module);
            return ServiceUtil.returnError("Cannot get DHL Estimate: Failed to render DHL XML Request.");
        }
        String requestString = outWriter.toString();
        if (Debug.verboseOn()) {
            Debug.logVerbose(requestString, module);
        }

        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendDhlRequest(requestString);
            if (Debug.verboseOn()) {
                Debug.logVerbose(rateResponseString, module);
            }
        }
        catch (DhlConnectException e) {
            String uceErrMsg = "Error sending DHL request for DHL Service Rate: " + e.toString();
            Debug.logError(e, uceErrMsg, module);
            return ServiceUtil.returnError(uceErrMsg);
        }

        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
            return handleDhlRateResponse(rateResponseDocument);
        }
        catch (SAXException e2) {
            String excErrMsg = "Error parsing the RatingServiceResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        }
        catch (ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        }
        catch (IOException e2) {
            String excErrMsg = "Error parsing the RatingServiceResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        }
    }

    /*
     * Parses an XML document from DHL to get the rate estimate
     */
    public static Map handleDhlRateResponse(Document rateResponseDocument) {
        List errorList = new LinkedList();
        Map dhlRateCodeMap = new HashMap();
        // process RateResponse
        Element rateResponseElement = rateResponseDocument.getDocumentElement();
        DhlServices.handleErrors(rateResponseElement, errorList);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(
                rateResponseElement, "Shipment");
        Element responseResultElement = UtilXml.firstChildElement(
                responseElement, "Result");
        Element responseEstimateDetailElement = UtilXml.firstChildElement(
                responseElement, "EstimateDetail");

        DhlServices.handleErrors(responseElement, errorList);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        String responseStatusCode = UtilXml.childElementValue(
                responseResultElement, "Code");
        String responseStatusDescription = UtilXml.childElementValue(
                responseResultElement, "Desc");

        String dateGenerated = UtilXml.childElementValue(
                responseEstimateDetailElement, "DateGenerated");

        Element responseServiceLevelCommitmentElement = UtilXml
            .firstChildElement(responseEstimateDetailElement,
                    "ServiceLevelCommitment");
        String responseServiceLevelCommitmentDescription = UtilXml
            .childElementValue(responseServiceLevelCommitmentElement,
                    "Desc");

        Element responseRateEstimateElement = UtilXml.firstChildElement(
                responseEstimateDetailElement, "RateEstimate");
        String responseTotalChargeEstimate = UtilXml.childElementValue(
                responseRateEstimateElement, "TotalChargeEstimate");
        Element responseChargesElement = UtilXml.firstChildElement(
                responseRateEstimateElement, "Charges");
        List chargeNodeList = UtilXml.childElementList(responseChargesElement,
                "Charge");

        List chargeList = new ArrayList();
        if (chargeNodeList != null && chargeNodeList.size() > 0) {
            for (int i = 0; chargeNodeList.size() > i; i++) {
                Map charge = new HashMap();

                Element responseChargeElement = (Element) chargeNodeList.get(i);
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
        Double shippingEstimateAmount = new Double(responseTotalChargeEstimate);
        dhlRateCodeMap.put("dateGenerated", dateGenerated);
        dhlRateCodeMap.put("serviceLevelCommitment",
                responseServiceLevelCommitmentDescription);
        dhlRateCodeMap.put("totalChargeEstimate", responseTotalChargeEstimate);
        dhlRateCodeMap.put("chargeList", chargeList);

        Map result = ServiceUtil.returnSuccess();
        result.put("shippingEstimateAmount", shippingEstimateAmount);
        result.put("dhlRateCodeMap", dhlRateCodeMap);
        return result;
    }

    /*
     * Register a DHL account for shipping by obtaining the DHL shipping key
     */
    public static Map dhlRegisterInquire(DispatchContext dctx, Map context) {

        Map result = new HashMap();
        String postalCode = (String) context.get("postalCode");
        String accountNbr = UtilProperties.getPropertyValue("shipment",
                "shipment.dhl.access.accountNbr");
        if (accountNbr == null) {
            return ServiceUtil
                .returnError("accountNbr not found for Register Account.");
        }
        // create AccessRequest XML doc
        Document requestDocument = createAccessRequestDocument();
        String requestString = null;
        Element requesElement = requestDocument.getDocumentElement();

        Element registerRequestElement = UtilXml.addChildElement(requesElement,
                "Register", requestDocument);
        registerRequestElement.setAttribute("version", "1.0");
        registerRequestElement.setAttribute("action", "ShippingKey");
        UtilXml.addChildElementValue(registerRequestElement, "AccountNbr",
                accountNbr, requestDocument);
        UtilXml.addChildElementValue(registerRequestElement, "PostalCode",
                postalCode, requestDocument);

        try {
            requestString = UtilXml.writeXmlDocument(requestDocument);
            Debug.log("AccessRequest XML Document:" + requestString);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: "
                + e.toString();
            Debug.logError(e, ioeErrMsg, module);
            return ServiceUtil.returnError(ioeErrMsg);
        }
        // send the request
        String registerResponseString = null;
        try {
            registerResponseString = sendDhlRequest(requestString);
            Debug.log("DHL request for DHL Register Account:"
                    + registerResponseString);
        } catch (DhlConnectException e) {
            String uceErrMsg = "Error sending DHL request for DHL Register Account: "
                + e.toString();
            Debug.logError(e, uceErrMsg, module);
            return ServiceUtil.returnError(uceErrMsg);
        }

        Document registerResponseDocument = null;
        try {
            registerResponseDocument = UtilXml.readXmlDocument(
                    registerResponseString, false);
            result = handleDhlRegisterResponse(registerResponseDocument);
            Debug.log("DHL response for DHL Register Account:"
                    + registerResponseString);
        } catch (SAXException e2) {
            String excErrMsg = "Error parsing the RegisterAccountServiceSelectionResponse: "
                + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        } catch (ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RegisterAccountServiceSelectionResponse: "
                + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        } catch (IOException e2) {
            String excErrMsg = "Error parsing the RegisterAccountServiceSelectionResponse: "
                + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        }

        return result;
    }

    /*
     * Parse response from DHL registration request to get shipping key
     */
    public static Map handleDhlRegisterResponse(
            Document registerResponseDocument) {
        List errorList = new LinkedList();
        // process RegisterResponse
        Element registerResponseElement = registerResponseDocument
            .getDocumentElement();
        DhlServices.handleErrors(registerResponseElement, errorList);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(
                registerResponseElement, "Register");
        Element responseResultElement = UtilXml.firstChildElement(
                responseElement, "Result");

        DhlServices.handleErrors(responseElement, errorList);
        if (UtilValidate.isNotEmpty(errorList)) {
            return ServiceUtil.returnError(errorList);
        }
        String responseStatusCode = UtilXml.childElementValue(
                responseResultElement, "Code");
        String responseStatusDescription = UtilXml.childElementValue(
                responseResultElement, "Desc");

        String responseShippingKey = UtilXml.childElementValue(responseElement,
                "ShippingKey");
        String responsePostalCode = UtilXml.childElementValue(responseElement,
                "PostalCode");

        Map result = ServiceUtil.returnSuccess();
        result.put("shippingKey", responseShippingKey);
        return result;
    }

    /*
     * Pass a shipment request to DHL via ShipIT and get a tracking number and a label back, among other things
     */

    public static Map dhlShipmentConfirm(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");        
        
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Map result = new HashMap();
        String shipmentConfirmResponseString = null;        
        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (shipment == null) {
                return ServiceUtil.returnError("Shipment not found with ID " + shipmentId);
            }
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError("ShipmentRouteSegment not found with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            
            if (!"DHL".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not DHL.");
            }
            
            // add ShipmentRouteSegment carrierServiceStatusId, check before all DHL services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId")) && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be not-set or [SHRSCS_NOT_STARTED] to perform the DHL Shipment Confirm operation.");
            }
            
            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress");
            if (originPostalAddress == null) {
                return ServiceUtil.returnError("OriginPostalAddress not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber");
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError("OriginTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            String originPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            // don't put on country code if not specified or is the US country code (UPS wants it this way and assuming DHL will accept this)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode")) && !"001".equals(originTelecomNumber.getString("countryCode"))) {
                originPhoneNumber = originTelecomNumber.getString("countryCode") + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, "-", "");
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, " ", "");
            
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo");
            if (originCountryGeo == null) {
                return ServiceUtil.returnError("OriginCountryGeo not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress");
            if (destPostalAddress == null) {
                return ServiceUtil.returnError("DestPostalAddress not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }

            // DHL requires destination phone number, default to sender # if no customer number
            String destPhoneNumber = originPhoneNumber;
            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber");
            if (destTelecomNumber != null) {
                destPhoneNumber = destTelecomNumber.getString("areaCode") + destTelecomNumber.getString("contactNumber");
                // don't put on country code if not specified or is the US country code (UPS wants it this way)
                if (UtilValidate.isNotEmpty(destTelecomNumber.getString("countryCode")) && !"001".equals(destTelecomNumber.getString("countryCode"))) {
                    destPhoneNumber = destTelecomNumber.getString("countryCode") + destPhoneNumber;
                }
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, "-", "");
                destPhoneNumber = StringUtil.replaceString(destPhoneNumber, " ", "");
            }

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne("CountryGeo");
            if (destCountryGeo == null) {
                return ServiceUtil.returnError("DestCountryGeo not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }

            List shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"));
            if (shipmentPackageRouteSegs == null || shipmentPackageRouteSegs.size() == 0) {
                return ServiceUtil.returnError("No ShipmentPackageRouteSegs (ie No Packages) found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            if (shipmentPackageRouteSegs.size() != 1) {
               return ServiceUtil.returnError("Cannot confirm shipment: DHL ShipIT does not currently support more than one package per shipment.");
            }

            // get the weight from the ShipmentRouteSegment first, which overrides all later weight computations
            boolean hasBillingWeight = false;  // for later overrides
            Double billingWeight = shipmentRouteSegment.getDouble("billingWeight");
            String billingWeightUomId = shipmentRouteSegment.getString("billingWeightUomId");
            if ((billingWeight != null) && (billingWeight.doubleValue() > 0)) {
                hasBillingWeight = true;
                if (billingWeightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing billingWeightUomId in shipmentId " + shipmentId,  module);
                    billingWeightUomId = "WT_lb"; // TODO: this should be specified in a properties file
                }
                // convert
                Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", billingWeightUomId, "uomIdTo", DHL_WEIGHT_UOM_ID, "originalValue", billingWeight));
                if (ServiceUtil.isError(results) || (results.get("convertedValue") == null)) {
                    Debug.logWarning("Unable to convert billing weights for shipmentId " + shipmentId , module);
                    // try getting the weight from package instead
                    hasBillingWeight = false;
                } else {
                    billingWeight = (Double) results.get("convertedValue");
                }
            }

            // loop through Shipment segments (NOTE: only one supported, loop is here for future refactoring reference)
            String length = null;
            String width = null;
            String height = null;
            Double packageWeight = null;
            Iterator shipmentPackageRouteSegIter = shipmentPackageRouteSegs.iterator();
            while (shipmentPackageRouteSegIter.hasNext()) {
                GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegIter.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage");
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType");
                List carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "DHL"), null);
                GenericValue carrierShipmentBoxType = null;
                if (carrierShipmentBoxTypes.size() > 0) {
                    carrierShipmentBoxType = (GenericValue) carrierShipmentBoxTypes.get(0); 
                }
                 
                // TODO: determine what default UoM is (assuming inches) - there should be a defaultDimensionUomId in Facility
                if (shipmentBoxType != null) {
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne("DimensionUom");
                    length = shipmentBoxType.get("boxLength").toString();
                    width = shipmentBoxType.get("boxWidth").toString();
                    height = shipmentBoxType.get("boxHeight").toString();
                }
                
                // next step is weight determination, so skip if we have a billing weight
                if (hasBillingWeight) continue;

                // compute total packageWeight (for now, just one package)
                if (shipmentPackage.getString("weight") != null) {
                    packageWeight = Double.valueOf(shipmentPackage.getString("weight"));
                } else {
                    // use default weight if available
                    try {
                        packageWeight = Double.valueOf(UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.default.weight.value"));
                    } catch (NumberFormatException ne) {
                        Debug.logWarning("Default shippable weight not configured (shipment.default.weight.value)", module);
                        packageWeight = new Double(1.0);
                    }
                }
                // convert weight
                String weightUomId = (String) shipmentPackage.get("weightUomId");
                if (weightUomId == null) {
                    Debug.logWarning("Shipment Route Segment missing weightUomId in shipmentId " + shipmentId,  module);
                    weightUomId = "WT_lb"; // TODO: this should be specified in a properties file
                }
                Map results = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", weightUomId, "uomIdTo", DHL_WEIGHT_UOM_ID, "originalValue", packageWeight));
                if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) || (results.get("convertedValue") == null)) {
                    Debug.logWarning("Unable to convert weights for shipmentId " + shipmentId , module);
                    packageWeight = new Double(1.0);
                } else {
                    packageWeight = (Double) results.get("convertedValue");
                }
            }

            // pick which weight to use and round it
            Double weight = null;
            if (hasBillingWeight) { 
                weight = billingWeight;
            } else {
                weight = packageWeight;
            }
            // want the rounded weight as a string, so we use the "" + int shortcut
            String roundedWeight = "" + Math.round(weight.doubleValue());
            
            // translate shipmentMethodTypeId to DHL service code
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String dhlShipmentDetailCode = null;
            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", UtilMisc.toMap("shipmentMethodTypeId", shipmentMethodTypeId,
                    "partyId", "DHL", "roleTypeId", "CARRIER"));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("No CarrierShipmentMethod entry for carrier DHL shipmentMethodTypeId " + shipmentMethodTypeId);
            }
            dhlShipmentDetailCode = carrierShipmentMethod.getString("carrierServiceCode");

            // shipping credentials (configured in properties)
            String userid = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.dhl.access.userid");
            String password = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.dhl.access.password");
            String shippingKey = UtilProperties.getPropertyValue("shipment", "shipment.dhl.access.shippingKey");
            String accountNbr = UtilProperties.getPropertyValue("shipment", "shipment.dhl.access.accountNbr");
            if ((shippingKey == null) || (accountNbr == null) || (shippingKey.length() == 0) || (accountNbr.length() == 0)) {
                return ServiceUtil.returnError("DHL Shipping Credentials are not configured. (check shipment.dhl.access)");
            }
            
            // label image preference (PNG or GIF)
            String labelImagePreference = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.dhl.label.image.format");
            if (labelImagePreference == null) {
                Debug.logInfo("shipment.dhl.label.image.format not specified, assuming PNG", module);
                labelImagePreference="PNG";
            } else if (!(labelImagePreference.equals("PNG") || labelImagePreference.equals("GIF"))) {
                Debug.logError("Illegal shipment.dhl.label.image.format: " + labelImagePreference, module);
                return ServiceUtil.returnError("Unknown DHL Label Image Format: " + labelImagePreference);
            }
            
            // create AccessRequest XML doc using FreeMarker template
            String templateName = UtilProperties.getPropertyValue(shipmentPropertiesFile, "shipment.template.dhl.rate.estimate");
            if ((templateName == null) || (templateName.trim().length() == 0)) {
                return ServiceUtil.returnError("Cannot get DHL Estimate: DHL Rate template not configured (shipment.template.dhl.rate.estimate");
            }
            StringWriter outWriter = new StringWriter();
            Map inContext = new HashMap();
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
		    inContext.put("postalCode", destPostalAddress.getString("postalCode").substring(0,5));
	    } else {
		    inContext.put("postalCode", destPostalAddress.getString("postalCode"));
	    }
            inContext.put("phoneNbr", destPhoneNumber);
            inContext.put("labelImageType", labelImagePreference);
            inContext.put("shipperReference", shipment.getString("primaryOrderId") + "-" + shipment.getString("primaryShipGroupSeqId"));
            
            try {
                ContentWorker.renderContentAsText(dispatcher, delegator, templateName, outWriter, inContext, locale, "text/plain", false);
            } catch (Exception e) {
                Debug.logError(e, "Cannot confirm DHL shipment: Failed to render DHL XML Request.", module);
                return ServiceUtil.returnError("Cannot confirm DHL shipment: Failed to render DHL XML Request.");
            }
            String requestString = outWriter.toString();
            if (Debug.verboseOn()) {
                Debug.logVerbose(requestString, module);
            }

            // send the request
            String responseString = null;
            try {
                responseString = sendDhlRequest(requestString);
                if (Debug.verboseOn()) {
                    Debug.logVerbose(responseString, module);
                }
            } catch (DhlConnectException e) {
                String uceErrMsg = "Error sending DHL request for DHL Service Rate: " + e.toString();
                Debug.logError(e, uceErrMsg, module);
                return ServiceUtil.returnError(uceErrMsg);
            }
	    // pass to handler method
            return handleDhlShipmentConfirmResponse(responseString, shipmentRouteSegment, shipmentPackageRouteSegs);
            
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            if (shipmentConfirmResponseString != null) {
                Debug.logError("Got XML ShipmentConfirmRespose: " + shipmentConfirmResponseString, module);
                return ServiceUtil.returnError(UtilMisc.toList(
                            "Error reading or writing Shipment data for DHL Shipment Confirm: " + e.toString(),
                            "A ShipmentConfirmRespose was received: " + shipmentConfirmResponseString));
            } else {
                return ServiceUtil.returnError("Error reading or writing Shipment data for DHL Shipment Confirm: " + e.toString());
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error reading or writing Shipment data for DHL Shipment Confirm: " + e.toString());
        }
    }

    // NOTE: Must VOID shipments on errors
    public static Map handleDhlShipmentConfirmResponse(String rateResponseString, GenericValue shipmentRouteSegment, 
            List shipmentPackageRouteSegs) throws GenericEntityException {
        Map result = new HashMap();
        GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegs.get(0);
        
        // TODO: figure out how to handle validation on return XML, which can be mangled
        // Ideas: try again right away, let user try again, etc.
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            // TODO: VOID
        } catch (ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            // TODO VOID
        } catch (IOException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            // TODO VOID
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
            Debug.logError("Cannot find response DHL shipment label.  Rate response document is: " + rateResponseString, module);
            return ServiceUtil.returnError("Cannot get response DHL shipment label for shipment package route segment " + shipmentPackageRouteSeg + ".  DHL response is: " + rateResponseString);
        }
        
        // TODO: this is a temporary hack to replace the newlines so that Base64 likes the input This is NOT platform independent
        int size = encodedImageString.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            if (encodedImageString.charAt(i) == '\n')
                continue;
            sb.append(encodedImageString.charAt(i));
        }
        byte[] labelBytes = Base64.base64Decode(sb.toString().getBytes());
        
        if (labelBytes != null) {
            // store in db blob
            shipmentPackageRouteSeg.setBytes("labelImage", labelBytes);
        } else {
            Debug.log("Failed to either decode returned DHL label or no data found in eCommerce/Shipment/Label/Image.");
            // TODO: VOID
        }
        
        shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
        shipmentPackageRouteSeg.set("labelHtml", sb.toString());
        shipmentPackageRouteSeg.store();
        
        shipmentRouteSegment.set("trackingIdNumber", trackingNumber);
        shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_CONFIRMED");
        shipmentRouteSegment.store();
        
        return ServiceUtil.returnSuccess("DHL Shipment Confirmed.");
    }

    private static double getWeight(List shippableItemInfo) {
        double totalWeight = 0;
        if (shippableItemInfo != null) {
            Iterator sii = shippableItemInfo.iterator();
            while (sii.hasNext()) {
                Map itemInfo = (Map) sii.next();
                double weight = ((Double) itemInfo.get("weight")).doubleValue();
                totalWeight = totalWeight + weight;
            }
        }
        return totalWeight;
    }

    public static Document createAccessRequestDocument() {
        return createAccessRequestDocument(shipmentPropertiesFile);
    }

    public static Document createAccessRequestDocument(String props) {
        Document eCommerceRequestDocument = UtilXml
                .makeEmptyXmlDocument("eCommerce");
        Element eCommerceRequesElement = eCommerceRequestDocument
                .getDocumentElement();
        eCommerceRequesElement.setAttribute("version", UtilProperties
                .getPropertyValue(props, "shipment.dhl.head.version"));
        eCommerceRequesElement.setAttribute("action", UtilProperties
                .getPropertyValue(props, "shipment.dhl.head.action"));
        Element requestorRequestElement = UtilXml.addChildElement(
                eCommerceRequesElement, "Requestor", eCommerceRequestDocument);
        UtilXml
                .addChildElementValue(requestorRequestElement, "ID",
                        UtilProperties.getPropertyValue(props,
                                "shipment.dhl.access.userid"),
                        eCommerceRequestDocument);
        UtilXml.addChildElementValue(requestorRequestElement, "Password",
                UtilProperties.getPropertyValue(props,
                        "shipment.dhl.access.password"),
                eCommerceRequestDocument);

        return eCommerceRequestDocument;
    }

    public static void handleErrors(Element responseElement, List errorList) {
        Element faultsElement = UtilXml.firstChildElement(responseElement,
                "Faults");
        List faultElements = UtilXml.childElementList(faultsElement, "Fault");
        if (UtilValidate.isNotEmpty(faultElements)) {
            Iterator errorElementIter = faultElements.iterator();
            while (errorElementIter.hasNext()) {
                StringBuffer errorMessageBuf = new StringBuffer();
                Element errorElement = (Element) errorElementIter.next();

                String errorCode = UtilXml.childElementValue(errorElement,
                        "Code");
                String errorDescription = UtilXml.childElementValue(
                        errorElement, "Desc");
                String errorSource = UtilXml.childElementValue(errorElement,
                        "Source");
                if (UtilValidate.isEmpty(errorSource)) {
                    errorSource = UtilXml.childElementValue(errorElement,
                            "Context");
                }
                errorMessageBuf.append("An error occurred [code:");
                errorMessageBuf.append(errorCode);
                errorMessageBuf.append("] ");
                errorMessageBuf.append(" [Description:");
                errorMessageBuf.append(errorDescription);
                errorMessageBuf.append("] ");
                errorMessageBuf.append(". ");
                if (UtilValidate.isNotEmpty(errorSource)) {
                    errorMessageBuf.append("The error was at Element [");
                    errorMessageBuf.append(errorSource);
                    errorMessageBuf.append("]");
                }
                errorList.add(errorMessageBuf.toString());
            }
        }
    }
}


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
