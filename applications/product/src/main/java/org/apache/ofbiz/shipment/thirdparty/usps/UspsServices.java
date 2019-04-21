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

package org.apache.ofbiz.shipment.thirdparty.usps;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.util.Base64;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilIO;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.common.uom.UomWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.shipment.shipment.ShipmentServices;
import org.apache.ofbiz.shipment.shipment.ShipmentWorker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * USPS Webtools API Services
 */
public class UspsServices {

    public final static String module = UspsServices.class.getName();
    public final static String resourceError = "ProductUiLabels";

    private static List<String> domesticCountries = new LinkedList<>();
    // Countries treated as domestic for rate enquiries
    static {
        domesticCountries.add("USA");
        domesticCountries.add("ASM");
        domesticCountries.add("GU");
        domesticCountries = Collections.unmodifiableList(domesticCountries);
    }

    public static Map<String, Object> uspsRateInquire(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");
        
        // check for 0 weight
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");
        if (shippableWeight.compareTo(BigDecimal.ZERO) == 0) {
            // TODO: should we return an error, or $0.00 ?
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsShippableWeightMustGreaterThanZero", locale));
        }

        // get the origination ZIP
        String originationZip = null;
        GenericValue productStore = ProductStoreWorker.getProductStore(((String) context.get("productStoreId")), delegator);
        if (productStore != null && productStore.get("inventoryFacilityId") != null) {
            GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, productStore.getString("inventoryFacilityId"), UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
            if (facilityContactMech != null) {
                try {
                    GenericValue shipFromAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", facilityContactMech.getString("contactMechId")).queryOne();
                    if (shipFromAddress != null) {
                        originationZip = shipFromAddress.getString("postalCode");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }
        if (UtilValidate.isEmpty(originationZip)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnableDetermineOriginationZip", locale));
        }

        // get the destination ZIP
        String destinationZip = null;
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            try {
                GenericValue shipToAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
                if (shipToAddress != null) {
                    if (!domesticCountries.contains(shipToAddress.getString("countryGeoId"))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                                "FacilityShipmentUspsRateInquiryOnlyInUsDestinations", locale));
                    }
                    destinationZip = shipToAddress.getString("postalCode");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (UtilValidate.isEmpty(destinationZip)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnableDetermineDestinationZip", locale));
        }

        // get the service code
        String serviceCode = null;
        try {
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                    .where("shipmentMethodTypeId", context.get("shipmentMethodTypeId"),
                           "partyId", context.get("carrierPartyId"),
                           "roleTypeId", context.get("carrierRoleTypeId"))
                    .queryOne();
            if (carrierShipmentMethod != null) {
                serviceCode = carrierShipmentMethod.getString("carrierServiceCode").toUpperCase(Locale.getDefault());
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isEmpty(serviceCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnableDetermineServiceCode", locale));
        }

        // create the request document
        Document requestDocument = createUspsRequestDocument("RateV2Request", true, delegator, shipmentGatewayConfigId, resource);

        // TODO: 70 lb max is valid for Express, Priority and Parcel only - handle other methods
        BigDecimal maxWeight;
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "maxEstimateWeight", 
                resource, "shipment.usps.max.estimate.weight", "70");
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            Debug.logWarning("Error parsing max estimate weight string [" + maxWeightStr + "], using default instead", module);
            maxWeight = new BigDecimal("70");
        }

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.checkList(context.get("shippableItemInfo"));
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        boolean isOnePackage = packages.size() == 1; // use shippableWeight if there's only one package
        // TODO: Up to 25 packages can be included per request - handle more than 25
        for (ListIterator<Map<String, BigDecimal>> li = packages.listIterator(); li.hasNext();) {
            Map<String, BigDecimal> packageMap = li.next();

            BigDecimal packageWeight = isOnePackage ? shippableWeight : ShipmentWorker.calcPackageWeight(dctx, packageMap, shippableItemInfo, BigDecimal.ZERO);
            if (packageWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
            packageElement.setAttribute("ID", String.valueOf(li.nextIndex() - 1)); // use zero-based index (see examples)

            UtilXml.addChildElementValue(packageElement, "Service", serviceCode, requestDocument);
            UtilXml.addChildElementValue(packageElement, "ZipOrigination", StringUtils.substring(originationZip, 0, 5), requestDocument);
            UtilXml.addChildElementValue(packageElement, "ZipDestination", StringUtils.substring(destinationZip, 0, 5), requestDocument);

            BigDecimal weightPounds = packageWeight.setScale(0, RoundingMode.FLOOR);
            // for Parcel post, the weight must be at least 1 lb
            if ("PARCEL".equals(serviceCode.toUpperCase(Locale.getDefault())) && (weightPounds.compareTo(BigDecimal.ONE) < 0)) {
                weightPounds = BigDecimal.ONE;
                packageWeight = BigDecimal.ZERO;
            }
            // (packageWeight % 1) * 16 (Rounded up to 0 dp)
            BigDecimal weightOunces = packageWeight.remainder(BigDecimal.ONE).multiply(new BigDecimal("16")).setScale(0, RoundingMode.CEILING);
            
            UtilXml.addChildElementValue(packageElement, "Pounds", weightPounds.toPlainString(), requestDocument);
            UtilXml.addChildElementValue(packageElement, "Ounces", weightOunces.toPlainString(), requestDocument);

            // TODO: handle other container types, package sizes, and machinable packages
            // IMPORTANT: Express or Priority Mail will fail if you supply a Container tag: you will get a message like
            // Invalid container type. Valid container types for Priority Mail are Flat Rate Envelope and Flat Rate Box.
            /* This is an official response from the United States Postal Service:
            The <Container> tag is used to specify the flat rate mailing options, or the type of large or oversized package being mailed.
            If you are wanting to get regular Express Mail rates, leave the <Container> tag empty, or do not include it in the request at all.
             */
            if ("Parcel".equalsIgnoreCase(serviceCode)) {
                UtilXml.addChildElementValue(packageElement, "Container", "None", requestDocument);
            }
            UtilXml.addChildElementValue(packageElement, "Size", "REGULAR", requestDocument);
            UtilXml.addChildElementValue(packageElement, "Machinable", "false", requestDocument);
        }

        // send the request
        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("RateV2", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticSendingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (responseDocument == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentRateNotAvailable", locale));
        }

        List<? extends Element> rates = UtilXml.childElementList(responseDocument.getDocumentElement(), "Package");
        if (UtilValidate.isEmpty(rates)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentRateNotAvailable", locale));
        }

        BigDecimal estimateAmount = BigDecimal.ZERO;
        for (Element packageElement: rates) {
            try {
                Element postageElement = UtilXml.firstChildElement(packageElement, "Postage");
                BigDecimal packageAmount = new BigDecimal(UtilXml.childElementValue(postageElement, "Rate"));
                estimateAmount = estimateAmount.add(packageAmount);
            } catch (NumberFormatException e) {
                Debug.logInfo(e, module);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shippingEstimateAmount", estimateAmount);
        return result;
    }

    /*
     * USPS International Service Codes
     * 1 - Express Mail International
     * 2 - Priority Mail International
     * 4 - Global Express Guaranteed (Document and Non-document)
     * 5 - Global Express Guaranteed Document used
     * 6 - Global Express Guaranteed Non-Document Rectangular shape
     * 7 - Global Express Guaranteed Non-Document Non-Rectangular
     * 8 - Priority Mail Flat Rate Envelope
     * 9 - Priority Mail Flat Rate Box
     * 10 - Express Mail International Flat Rate Envelope
     * 11 - Priority Mail Large Flat Rate Box
     * 12 - Global Express Guaranteed Envelope
     * 13 - First Class Mail International Letters
     * 14 - First Class Mail International Flats
     * 15 - First Class Mail International Parcels
     * 16 - Priority Mail Small Flat Rate Box
     * 21 - PostCards
     */
    public static Map<String, Object> uspsInternationalRateInquire(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");

        // check for 0 weight
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");
        if (shippableWeight.compareTo(BigDecimal.ZERO) == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsShippableWeightMustGreaterThanZero", locale));
        }

        // get the destination country
        String destinationCountry = null;
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            try {
                GenericValue shipToAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
                if (domesticCountries.contains(shipToAddress.get("countryGeoId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsRateInternationCannotBeUsedForUsDestinations", locale));
                }
                if (UtilValidate.isNotEmpty(shipToAddress.getString("countryGeoId"))) {
                    GenericValue countryGeo = shipToAddress.getRelatedOne("CountryGeo", false);
                    // TODO: Test against all country geoNames against what USPS expects
                    destinationCountry = countryGeo.getString("geoName");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (UtilValidate.isEmpty(destinationCountry)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnableDetermineDestinationCountry", locale));
        }

        // get the service code
        String serviceCode = null;
        try {
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                    .where("shipmentMethodTypeId", context.get("shipmentMethodTypeId"),
                           "partyId", context.get("carrierPartyId"),
                           "roleTypeId", context.get("carrierRoleTypeId"))
                    .queryOne();
            if (carrierShipmentMethod != null) {
                serviceCode = carrierShipmentMethod.getString("carrierServiceCode");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isEmpty(serviceCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnableDetermineServiceCode", locale));
        }

        BigDecimal maxWeight;
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "maxEstimateWeight", 
                resource, "shipment.usps.max.estimate.weight", "70");
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            Debug.logWarning("Error parsing max estimate weight string [" + maxWeightStr + "], using default instead", module);
            maxWeight = new BigDecimal("70");
        }

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.checkList(context.get("shippableItemInfo"));
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        boolean isOnePackage = packages.size() == 1; // use shippableWeight if there's only one package

        // create the request document
        Document requestDocument = createUspsRequestDocument("IntlRateRequest", false, delegator, shipmentGatewayConfigId, resource);

        // TODO: Up to 25 packages can be included per request - handle more than 25
        for (ListIterator<Map<String, BigDecimal>> li = packages.listIterator(); li.hasNext();) {
            Map<String, BigDecimal> packageMap = li.next();

            Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
            packageElement.setAttribute("ID", String.valueOf(li.nextIndex() - 1)); // use zero-based index (see examples)

            BigDecimal packageWeight = isOnePackage ? shippableWeight : ShipmentWorker.calcPackageWeight(dctx, packageMap, shippableItemInfo, BigDecimal.ZERO);
            if (packageWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Integer[] weightPoundsOunces = convertPoundsToPoundsOunces(packageWeight);
            // for Parcel post, the weight must be at least 1 lb
            if ("PARCEL".equals(serviceCode.toUpperCase(Locale.getDefault())) && (weightPoundsOunces[0] < 1)) {
                weightPoundsOunces[0] = 1;
                weightPoundsOunces[1] = 0;
            }
            UtilXml.addChildElementValue(packageElement, "Pounds", weightPoundsOunces[0].toString(), requestDocument);
            UtilXml.addChildElementValue(packageElement, "Ounces", weightPoundsOunces[1].toString(), requestDocument);

            UtilXml.addChildElementValue(packageElement, "Machinable", "False", requestDocument);
            UtilXml.addChildElementValue(packageElement, "MailType", "Package", requestDocument);

            // TODO: Add package value so that an insurance fee can be returned

            UtilXml.addChildElementValue(packageElement, "Country", destinationCountry, requestDocument);
        }

        // send the request
        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("IntlRate", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateInternationalSendingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (responseDocument == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentRateNotAvailable", locale));
        }

        List<? extends Element> packageElements = UtilXml.childElementList(responseDocument.getDocumentElement(), "Package");
        if (UtilValidate.isEmpty(packageElements)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentRateNotAvailable", locale));
        }

        BigDecimal estimateAmount = BigDecimal.ZERO;
        for (Element packageElement: packageElements) {
            Element errorElement = UtilXml.firstChildElement(packageElement, "Error");
            if (errorElement != null) {
                String errorDescription = UtilXml.childElementValue(errorElement, "Description");
                Debug.logInfo("USPS International Rate Calculation returned a package error: " + errorDescription, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentRateNotAvailable", locale));
            }
            List<? extends Element> serviceElements = UtilXml.childElementList(packageElement, "Service");
            for (Element serviceElement : serviceElements) {
                String respServiceCode = serviceElement.getAttribute("ID");
                if (!serviceCode.equalsIgnoreCase(respServiceCode)) {
                    continue;
                }
                try {
                    BigDecimal packageAmount = new BigDecimal(UtilXml.childElementValue(serviceElement, "Postage"));
                    estimateAmount = estimateAmount.add(packageAmount);
                } catch (NumberFormatException e) {
                    Debug.logInfo("USPS International Rate Calculation returned an unparsable postage amount: " + UtilXml.childElementValue(serviceElement, "Postage"), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentRateNotAvailable", locale));
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("shippingEstimateAmount", estimateAmount);
        return result;
    }

    // lifted from UpsServices with no changes - 2004.09.06 JFE
    /*

    Track/Confirm Samples: (API=TrackV2)

    Request:
    <TrackRequest USERID="xxxxxxxx" PASSWORD="xxxxxxxx">
        <TrackID ID="EJ958083578US"></TrackID>
    </TrackRequest>

    Response:
    <TrackResponse>
        <TrackInfo ID="EJ958083578US">
            <TrackSummary>Your item was delivered at 8:10 am on June 1 in Wilmington DE 19801.</TrackSummary>
            <TrackDetail>May 30 11:07 am NOTICE LEFT WILMINGTON DE 19801.</TrackDetail>
            <TrackDetail>May 30 10:08 am ARRIVAL AT UNIT WILMINGTON DE 19850.</TrackDetail>
            <TrackDetail>May 29 9:55 am ACCEPT OR PICKUP EDGEWATER NJ 07020.</TrackDetail>
        </TrackInfo>
    </TrackResponse>

    */

    public static Map<String, Object> uspsTrackConfirm(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");

        Document requestDocument = createUspsRequestDocument("TrackRequest", true, delegator, shipmentGatewayConfigId, resource);

        Element trackingElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "TrackID", requestDocument);
        trackingElement.setAttribute("ID", (String) context.get("trackingId"));

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("TrackV2", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsTrackingSendingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        Element trackInfoElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "TrackInfo");
        if (trackInfoElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsTrackingIncompleteResponse", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        result.put("trackingSummary", UtilXml.childElementValue(trackInfoElement, "TrackSummary"));

        List<? extends Element> detailElementList = UtilXml.childElementList(trackInfoElement, "TrackDetail");
        if (UtilValidate.isNotEmpty(detailElementList)) {
            List<String> trackingDetailList = new LinkedList<>();
            for (Element detailElement: detailElementList) {
                trackingDetailList.add(UtilXml.elementValue(detailElement));
            }
            result.put("trackingDetailList", trackingDetailList);
        }

        return result;
    }

    /*

    Address Standardization Samples: (API=Verify)

    Request:
    <AddressValidateRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
        <Address ID="0">
            <Address1></Address1>
            <Address2>6406 Ivy Lane</Address2>
            <City>Greenbelt</City>
            <State>MD</State>
            <Zip5></Zip5>
            <Zip4></Zip4>
        </Address>
    </AddressValidateRequest>

    Response:
    <AddressValidateResponse>
        <Address ID="0">
            <Address2>6406 IVY LN</Address2>
            <City>GREENBELT</City>
            <State>MD</State>
            <Zip5>20770</Zip5>
            <Zip4>1440</Zip4>
        </Address>
    </AddressValidateResponse>

    Note:
        The service parameters address1 and addess2 follow the OFBiz naming convention,
        and are converted to USPS conventions internally
        (OFBiz address1 = USPS address2, OFBiz address2 = USPS address1)

    */

    public static Map<String, Object> uspsAddressValidation(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        String state = (String) context.get("state");
        String city = (String) context.get("city");
        String zip5 = (String) context.get("zip5");
        Locale locale = (Locale) context.get("locale");
        if ((UtilValidate.isEmpty(state) && UtilValidate.isEmpty(city) && UtilValidate.isEmpty(zip5)) ||    // No state, city or zip5
             (UtilValidate.isEmpty(zip5) && (UtilValidate.isEmpty(state) || UtilValidate.isEmpty(city)))) {  // Both state and city are required if no zip5
            Debug.logError("USPS address validation requires either zip5 or city and state",  module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsAddressValidationStateAndCityOrZipRqd", locale));
        }

        Document requestDocument = createUspsRequestDocument("AddressValidateRequest", true, delegator, shipmentGatewayConfigId, resource);

        Element addressElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Address", requestDocument);
        addressElement.setAttribute("ID", "0");

        // 38 chars max
        UtilXml.addChildElementValue(addressElement, "FirmName", (String) context.get("firmName"), requestDocument);
        // 38 chars max
        UtilXml.addChildElementValue(addressElement, "Address1", (String) context.get("address2"), requestDocument);
        // 38 chars max
        UtilXml.addChildElementValue(addressElement, "Address2", (String) context.get("address1"), requestDocument);
        // 15 chars max
        UtilXml.addChildElementValue(addressElement, "City", (String) context.get("city"), requestDocument);

        UtilXml.addChildElementValue(addressElement, "State", (String) context.get("state"), requestDocument);
        UtilXml.addChildElementValue(addressElement, "Zip5", (String) context.get("zip5"), requestDocument);
        UtilXml.addChildElementValue(addressElement, "Zip4", (String) context.get("zip4"), requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("Verify", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsAddressValidationSendingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Address");
        if (respAddressElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsAddressValidationIncompleteResponse", locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsAddressValidationIncompleteResponse", 
                    UtilMisc.toMap("errorString", UtilXml.childElementValue(respErrorElement, "Description")), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        // Note: a FirmName element is not returned if empty
        String firmName = UtilXml.childElementValue(respAddressElement, "FirmName");
        if (UtilValidate.isNotEmpty(firmName)) {
            result.put("firmName", firmName);
        }

        // Note: an Address1 element is not returned if empty
        String address1 = UtilXml.childElementValue(respAddressElement, "Address1");
        if (UtilValidate.isNotEmpty(address1)) {
            result.put("address2", address1);
        }

        result.put("address1", UtilXml.childElementValue(respAddressElement, "Address2"));
        result.put("city", UtilXml.childElementValue(respAddressElement, "City"));
        result.put("state", UtilXml.childElementValue(respAddressElement, "State"));
        result.put("zip5", UtilXml.childElementValue(respAddressElement, "Zip5"));
        result.put("zip4", UtilXml.childElementValue(respAddressElement, "Zip4"));
        Element returnTextElement = UtilXml.firstChildElement(respAddressElement, "ReturnText");
        if (returnTextElement != null) {
            result.put("returnText", UtilXml.elementValue(returnTextElement));
        }
        return result;
    }

    /*

    City/State Lookup Samples: (API=CityStateLookup)

    Request:
    <CityStateLookupRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
        <ZipCode ID="0">
            <Zip5>90210</Zip5>
        </ZipCode>
    </CityStateLookupRequest>

    Response:
    <CityStateLookupResponse>
        <ZipCode ID="0">
            <Zip5>90210</Zip5>
            <City>BEVERLY HILLS</City>
            <State>CA</State>
        </ZipCode>
    </CityStateLookupResponse>

    */

    public static Map<String, Object> uspsCityStateLookup(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");

        Document requestDocument = createUspsRequestDocument("CityStateLookupRequest", true, delegator, shipmentGatewayConfigId, resource);

        Element zipCodeElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "ZipCode", requestDocument);
        zipCodeElement.setAttribute("ID", "0");

        String zipCode = ((String) context.get("zip5")).trim(); // trim leading/trailing spaces

        // only the first 5 digits are used, the rest are ignored
        UtilXml.addChildElementValue(zipCodeElement, "Zip5", zipCode, requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("CityStateLookup", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsCityStateLookupSendingError", 
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "ZipCode");
        if (respAddressElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsCityStateLookupIncompleteResponse", locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsCityStateLookupResponseError", 
                    UtilMisc.toMap("errorString", UtilXml.childElementValue(respErrorElement, "Description")), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String city = UtilXml.childElementValue(respAddressElement, "City");
        if (UtilValidate.isEmpty(city)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsCityStateLookupIncompleteCityElement", locale));
        }
        result.put("city", city);

        String state = UtilXml.childElementValue(respAddressElement, "State");
        if (UtilValidate.isEmpty(state)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsCityStateLookupIncompleteStateElement", locale));
        }
        result.put("state", state);

        return result;
    }

    /*

    Service Standards Samples:

    Priority Mail: (API=PriorityMail)

        Request:
        <PriorityMailRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
        </PriorityMailRequest>

        Response:
        <PriorityMailResponse>
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
            <Days>1</Days>
        </PriorityMailResponse>

    Package Services: (API=StandardB)

        Request:
        <StandardBRequest USERID="xxxxxxx" PASSWORD="xxxxxxx">
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
        </StandardBRequest>

        Response:
        <StandardBResponse>
            <OriginZip>4</OriginZip>
            <DestinationZip>4</DestinationZip>
            <Days>2</Days>
        </StandardBResponse>

    Note:
        When submitting ZIP codes, only the first 3 digits are used.
        If a 1- or 2-digit ZIP code is entered, leading zeros are implied.
        If a 4- or 5-digit ZIP code is entered, the last digits  will be ignored.

    */

    public static Map<String, Object> uspsPriorityMailStandard(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        subContext.put("serviceType", "PriorityMail");
        return uspsServiceStandards(dctx, subContext);
    }

    public static Map<String, Object> uspsPackageServicesStandard(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> subContext = UtilMisc.makeMapWritable(context);
        subContext.put("serviceType", "StandardB");
        return uspsServiceStandards(dctx, subContext);
    }

    private static Map<String, Object> uspsServiceStandards(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        String type = (String) context.get("serviceType");
        Locale locale = (Locale) context.get("locale");
        if (!type.matches("PriorityMail|StandardB")) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsUnsupporteServiceType", 
                    UtilMisc.toMap("serviceType", type), locale));
        }

        Document requestDocument = createUspsRequestDocument(type + "Request", true, delegator, shipmentGatewayConfigId, resource);

        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), "OriginZip",
                (String) context.get("originZip"), requestDocument);
        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), "DestinationZip",
                (String) context.get("destinationZip"), requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(type, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsServiceStandardSendingError", 
                    UtilMisc.toMap("serviceType", type, "errorString", e.getMessage()), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String days = UtilXml.childElementValue(responseDocument.getDocumentElement(), "Days");
        if (UtilValidate.isEmpty(days)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsServiceStandardResponseIncompleteDaysElement", 
                    UtilMisc.toMap("serviceType", type), locale));
        }
        result.put("days", days);

        return result;
    }



    /*

    Domestic Rate Calculator Samples: (API=Rate)

    Request:
    <RateRequest USERID="xxxxxx" PASSWORD="xxxxxxx">
        <Package ID="0">
            <Service>Priority</Service>
            <ZipOrigination>20770</ZipOrigination>
            <ZipDestination>09021</ZipDestination>
            <Pounds>5</Pounds>
            <Ounces>1</Ounces>
            <Container>None</Container>
            <Size>Regular</Size>
            <Machinable>False</Machinable>
        </Package>
    </RateRequest>

    Response:
    <RateResponse>
        <Package ID="0">
            <Service>Priority</Service>
            <ZipOrigination>20770</ZipOrigination>
            <ZipDestination>09021</ZipDestination>
            <Pounds>5</Pounds>
            <Ounces>1</Ounces>
            <Container>None</Container>
            <Size>REGULAR</Size>
            <Machinable>FALSE</Machinable>
            <Zone>3</Zone>
            <Postage>7.90</Postage>
            <RestrictionCodes>B-B1-C-D-U</RestrictionCodes>
            <RestrictionDescription>
            B. Form 2976-A is required for all mail weighing 16 ounces or more, with exceptions noted below.
            In addition, mailers must properly complete required customs documentation when mailing any potentially
            dutiable mail addressed to an APO or FPO regardless of weight. B1. Form 2976 or 2976-A is required.
            Articles are liable for customs duty and/or purchase tax unless they are bona fide gifts intended for
            use by military personnel or their dependents. C. Cigarettes and other tobacco products are prohibited.
            D. Coffee is prohibited. U. Parcels must weigh less than 16 ounces when addressed to Box R.
            </RestrictionDescription>
        </Package>
    </RateResponse>

    */

    public static Map<String, Object> uspsDomesticRate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        Locale locale = (Locale) context.get("locale");
 
        Document requestDocument = createUspsRequestDocument("RateRequest", true, delegator, shipmentGatewayConfigId, resource);

        Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
        packageElement.setAttribute("ID", "0");

        UtilXml.addChildElementValue(packageElement, "Service", (String) context.get("service"), requestDocument);
        UtilXml.addChildElementValue(packageElement, "ZipOrigination", (String) context.get("originZip"), requestDocument);
        UtilXml.addChildElementValue(packageElement, "ZipDestination", (String) context.get("destinationZip"), requestDocument);
        UtilXml.addChildElementValue(packageElement, "Pounds", (String) context.get("pounds"), requestDocument);
        UtilXml.addChildElementValue(packageElement, "Ounces", (String) context.get("ounces"), requestDocument);

        String container = (String) context.get("container");
        if (UtilValidate.isEmpty(container)) {
            container = "None";
        }
        UtilXml.addChildElementValue(packageElement, "Container", container, requestDocument);

        String size = (String) context.get("size");
        if (UtilValidate.isEmpty(size)) {
            size = "Regular";
        }
        UtilXml.addChildElementValue(packageElement, "Size", size, requestDocument);

        String machinable = (String) context.get("machinable");
        if (UtilValidate.isEmpty(machinable)) {
            machinable = "False";
        }
        UtilXml.addChildElementValue(packageElement, "Machinable", machinable, requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("Rate", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
        } catch (UspsRequestException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticSendingError", 
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Package");
        if (respPackageElement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticResponseIncompleteElementPackage", locale));
        }

        Element respErrorElement = UtilXml.firstChildElement(respPackageElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticResponseError", 
                    UtilMisc.toMap("errorString", UtilXml.childElementValue(respErrorElement, "Description")), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String zone = UtilXml.childElementValue(respPackageElement, "Zone");
        if (UtilValidate.isEmpty(zone)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticResponseIncompleteElementZone", locale));
        }
        result.put("zone", zone);

        String postage = UtilXml.childElementValue(respPackageElement, "Postage");
        if (UtilValidate.isEmpty(postage)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticResponseIncompleteElementPostage", locale));
        }
        result.put("postage", postage);

        String restrictionCodes = UtilXml.childElementValue(respPackageElement, "RestrictionCodes");
        if (UtilValidate.isNotEmpty(restrictionCodes)) {
            result.put("restrictionCodes", restrictionCodes);
        }

        String restrictionDesc = UtilXml.childElementValue(respPackageElement, "RestrictionDescription");
        if (UtilValidate.isNotEmpty(restrictionCodes)) {
            result.put("restrictionDesc", restrictionDesc);
        }

        return result;
    }

    // Warning: I don't think the following 2 services were completed or fully tested - 2004.09.06 JFE

    /* --- ShipmentRouteSegment services --------------------------------------------------------------------------- */

    public static Map<String, Object> uspsUpdateShipmentRateInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");
        
        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsGatewayNotAvailable", locale));
        }

        try {
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "ProductShipmentRouteSegmentNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // ensure the carrier is USPS
            if (!"USPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsNotRouteSegmentCarrier", 
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentRouteSegmentOriginPostalAddressNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (!"USA".equals(originAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originZip = originAddress.getString("postalCode");
            if (UtilValidate.isEmpty(originZip)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentOriginZipCodeMissing", 
                        UtilMisc.toMap("contactMechId", originAddress.getString("contactMechId")), locale));
            }

            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destinationAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentRouteSegmentDestPostalAddressNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (!"USA".equals(destinationAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String destinationZip = destinationAddress.getString("postalCode");
            if (UtilValidate.isEmpty(destinationZip)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentDestinationZipCodeMissing", 
                        UtilMisc.toMap("contactMechId", destinationAddress.getString("contactMechId")), locale));
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String partyId = shipmentRouteSegment.getString("carrierPartyId");
           
            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod").where("partyId", partyId, "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentMethodTypeId).queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsNoCarrierShipmentMethod", 
                        UtilMisc.toMap("carrierPartyId", partyId, "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            String serviceType = carrierShipmentMethod.getString("carrierServiceCode");
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsNoCarrierServiceCodeFound", 
                        UtilMisc.toMap("carrierPartyId", partyId, "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }

            // get the packages for this shipment route segment
            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentPackageRouteSegsNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            BigDecimal actualTransportCost = BigDecimal.ZERO;

            String carrierDeliveryZone = null;
            String carrierRestrictionCodes = null;
            String carrierRestrictionDesc = null;

            // send a new request for each package
            for (Iterator<GenericValue> i = shipmentPackageRouteSegList.iterator(); i.hasNext();) {

                GenericValue shipmentPackageRouteSeg = i.next();
                Document requestDocument = createUspsRequestDocument("RateRequest", true, delegator, shipmentGatewayConfigId, resource);

                Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
                packageElement.setAttribute("ID", "0");

                UtilXml.addChildElementValue(packageElement, "Service", serviceType, requestDocument);
                UtilXml.addChildElementValue(packageElement, "ZipOrigination", originZip, requestDocument);
                UtilXml.addChildElementValue(packageElement, "ZipDestination", destinationZip, requestDocument);

                GenericValue shipmentPackage = null;
                shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);

                // weight elements - Pounds, Ounces
                String weightStr = shipmentPackage.getString("weight");
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsWeightNotFound", 
                            UtilMisc.toMap("shipmentId", shipmentPackage.getString("shipmentId"), 
                                    "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId")), locale));
                }

                BigDecimal weight = BigDecimal.ZERO;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, module); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString("weightUomId");
                if (UtilValidate.isEmpty(weightUomId)) {
                    weightUomId = "WT_lb"; // assume weight is in pounds
                }
                if (!"WT_lb".equals(weightUomId)) {
                    // attempt a conversion to pounds
                    Map<String, Object> result;
                    try {
                        result = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", weightUomId, "uomIdTo", "WT_lb", "originalValue", weight));
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                    } catch (GenericServiceException ex) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                                "FacilityShipmentUspsWeightConversionError", 
                                UtilMisc.toMap("errorString", ex.getMessage()), locale));
                    }

                    if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS) && result.get("convertedValue") != null) {
                        weight = weight.multiply((BigDecimal) result.get("convertedValue"));
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                                "FacilityShipmentUspsWeightUnsupported", 
                                UtilMisc.toMap("weightUomId", weightUomId, "shipmentId", shipmentPackage.getString("shipmentId"), 
                                        "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                        "weightUom", "WT_lb"), locale));
                    }

                }

                BigDecimal weightPounds = weight.setScale(0, RoundingMode.FLOOR);
                BigDecimal weightOunces = weight.multiply(new BigDecimal("16")).remainder(new BigDecimal("16")).setScale(0, RoundingMode.CEILING);

                DecimalFormat df = new DecimalFormat("#");
                UtilXml.addChildElementValue(packageElement, "Pounds", df.format(weightPounds), requestDocument);
                UtilXml.addChildElementValue(packageElement, "Ounces", df.format(weightOunces), requestDocument);

                // Container element
                GenericValue carrierShipmentBoxType = null;
                List<GenericValue> carrierShipmentBoxTypes = null;
                carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "USPS"), null, false);

                if (carrierShipmentBoxTypes.size() > 0) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                if (carrierShipmentBoxType != null &&
                        UtilValidate.isNotEmpty(carrierShipmentBoxType.getString("packagingTypeCode"))) {
                    UtilXml.addChildElementValue(packageElement, "Container",
                            carrierShipmentBoxType.getString("packagingTypeCode"), requestDocument);
                } else {
                    // default to "None", for customers using their own package
                    UtilXml.addChildElementValue(packageElement, "Container", "None", requestDocument);
                }

                // Size element
                if (carrierShipmentBoxType != null && UtilValidate.isNotEmpty("oversizeCode")) {
                    UtilXml.addChildElementValue(packageElement, "Size",
                            carrierShipmentBoxType.getString("oversizeCode"), requestDocument);
                } else {
                    // default to "Regular", length + girth measurement <= 84 inches
                    UtilXml.addChildElementValue(packageElement, "Size", "Regular", requestDocument);
                }

                // Although only applicable for Parcel Post, this tag is required for all requests
                UtilXml.addChildElementValue(packageElement, "Machinable", "False", requestDocument);

                Document responseDocument = null;
                try {
                    responseDocument = sendUspsRequest("Rate", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
                } catch (UspsRequestException e) {
                    Debug.logInfo(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsRateDomesticSendingError", 
                            UtilMisc.toMap("errorString", e.getMessage()), locale));
                }

                Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Package");
                if (respPackageElement == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsRateDomesticResponseIncompleteElementPackage", locale));
                }

                Element respErrorElement = UtilXml.firstChildElement(respPackageElement, "Error");
                if (respErrorElement != null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsRateDomesticResponseError", 
                            UtilMisc.toMap("errorString", UtilXml.childElementValue(respErrorElement, "Description")), locale));
                }

                // update the ShipmentPackageRouteSeg
                String postageString = UtilXml.childElementValue(respPackageElement, "Postage");
                if (UtilValidate.isEmpty(postageString)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsRateDomesticResponseIncompleteElementPostage", locale));
                }

                BigDecimal postage = BigDecimal.ZERO;
                try {
                    postage = new BigDecimal(postageString);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, module); // TODO: handle exception
                }
                actualTransportCost = actualTransportCost.add(postage);

                shipmentPackageRouteSeg.setString("packageTransportCost", postageString);
                shipmentPackageRouteSeg.store();

                // if this is the last package, get the zone and APO/FPO restrictions for the ShipmentRouteSegment
                if (!i.hasNext()) {
                    carrierDeliveryZone = UtilXml.childElementValue(respPackageElement, "Zone");
                    carrierRestrictionCodes = UtilXml.childElementValue(respPackageElement, "RestrictionCodes");
                    carrierRestrictionDesc = UtilXml.childElementValue(respPackageElement, "RestrictionDescription");
                }
            }

            // update the ShipmentRouteSegment
            shipmentRouteSegment.set("carrierDeliveryZone", carrierDeliveryZone);
            shipmentRouteSegment.set("carrierRestrictionCodes", carrierRestrictionCodes);
            shipmentRouteSegment.set("carrierRestrictionDesc", carrierRestrictionDesc);
            shipmentRouteSegment.setString("actualTransportCost", String.valueOf(actualTransportCost));
            shipmentRouteSegment.store();

        } catch (GenericEntityException gee) {
            Debug.logInfo(gee, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsRateDomesticReadingError",
                    UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /*

    Delivery Confirmation Samples:

    <DeliveryConfirmationV2.0Request USERID="xxxxxxx" PASSWORD="xxxxxxxx">
        <Option>3</Option>
        <ImageParameters></ImageParameters>
        <FromName>John Smith</FromName>
        <FromFirm>ABC Corp.</FromFirm>
        <FromAddress1>Ste  4</FromAddress1>
        <FromAddress2>6406  Ivy Lane</FromAddress2>
        <FromCity>Greenbelt</FromCity>
        <FromState>MD</FromState>
        <FromZip5>20770</FromZip5>
        <FromZip4>4354</FromZip4>
        <ToName>Jane Smith</ToName>
        <ToFirm>XYZ Corp.</ToFirm>
        <ToAddress1>Apt 303</ToAddress1>
        <ToAddress2>4411 Romlon Street</ToAddress2>
        <ToCity>Beltsville</ToCity>
        <ToState>MD</ToState>
        <ToZip5>20705</ToZip5>
        <ToZip4>5656</ToZip4>
        <WeightInOunces>22</WeightInOunces>
        <ServiceType>Parcel Post</ServiceType>
        <ImageType>TIF</ImageType>
    </DeliveryConfirmationV2.0Request>

    <DeliveryConfirmationV2.0Response>
        <DeliveryConfirmationNumber>02805213907052510758</DeliveryConfirmationNumber>
        <DeliveryConfirmationLabel>(Base64 encoded data)</DeliveryConfirmationLabel>
    </DeliveryConfirmationV2.0Response>

    */

    public static Map<String, Object> uspsDeliveryConfirmation(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");
        
        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsGatewayNotAvailable", locale));
        }

        try {
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "ProductShipmentNotFoundId", locale) + shipmentId);
            }

            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "ProductShipmentRouteSegmentNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // ensure the carrier is USPS
            if (!"USPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsNotRouteSegmentCarrier", 
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentRouteSegmentOriginPostalAddressNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (!"USA".equals(originAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            
            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destinationAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentRouteSegmentDestPostalAddressNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            if (!"USA".equals(destinationAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsRouteSegmentOriginCountryGeoNotInUsa", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String partyId = shipmentRouteSegment.getString("carrierPartyId");

            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod").where("partyId", partyId, "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentMethodTypeId).queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsNoCarrierShipmentMethod", 
                        UtilMisc.toMap("carrierPartyId", partyId, "shipmentMethodTypeId", shipmentMethodTypeId), locale));
            }
            String serviceType = carrierShipmentMethod.getString("carrierServiceCode");
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsUnableDetermineServiceCode", locale));
            }

            // get the packages for this shipment route segment
            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentPackageRouteSegsNotFound", 
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegList) {
                Document requestDocument = createUspsRequestDocument("DeliveryConfirmationV2.0Request", true, delegator, shipmentGatewayConfigId, resource);
                Element requestElement = requestDocument.getDocumentElement();

                UtilXml.addChildElementValue(requestElement, "Option", "3", requestDocument);
                UtilXml.addChildElement(requestElement, "ImageParameters", requestDocument);

                // From address
                if (UtilValidate.isNotEmpty(originAddress.getString("attnName"))) {
                    UtilXml.addChildElementValue(requestElement, "FromName", originAddress.getString("attnName"), requestDocument);
                    UtilXml.addChildElementValue(requestElement, "FromFirm", originAddress.getString("toName"), requestDocument);
                } else {
                    UtilXml.addChildElementValue(requestElement, "FromName", originAddress.getString("toName"), requestDocument);
                }
                // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
                UtilXml.addChildElementValue(requestElement, "FromAddress1", originAddress.getString("address2"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "FromAddress2", originAddress.getString("address1"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "FromCity", originAddress.getString("city"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "FromState", originAddress.getString("stateProvinceGeoId"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "FromZip5", originAddress.getString("postalCode"), requestDocument);
                UtilXml.addChildElement(requestElement, "FromZip4", requestDocument);

                // To address
                if (UtilValidate.isNotEmpty(destinationAddress.getString("attnName"))) {
                    UtilXml.addChildElementValue(requestElement, "ToName", destinationAddress.getString("attnName"), requestDocument);
                    UtilXml.addChildElementValue(requestElement, "ToFirm", destinationAddress.getString("toName"), requestDocument);
                } else {
                    UtilXml.addChildElementValue(requestElement, "ToName", destinationAddress.getString("toName"), requestDocument);
                }
                // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
                UtilXml.addChildElementValue(requestElement, "ToAddress1", destinationAddress.getString("address2"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "ToAddress2", destinationAddress.getString("address1"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "ToCity", destinationAddress.getString("city"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "ToState", destinationAddress.getString("stateProvinceGeoId"), requestDocument);
                UtilXml.addChildElementValue(requestElement, "ToZip5", destinationAddress.getString("postalCode"), requestDocument);
                UtilXml.addChildElement(requestElement, "ToZip4", requestDocument);

                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                
                // WeightInOunces
                String weightStr = shipmentPackage.getString("weight");
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsWeightNotFound", 
                            UtilMisc.toMap("shipmentId", shipmentPackage.getString("shipmentId"), 
                                    "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId")), locale));
                }

                BigDecimal weight = BigDecimal.ZERO;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, module); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString("weightUomId");
                if (UtilValidate.isEmpty(weightUomId)) {
                    // assume weight is in pounds for consistency (this assumption is made in uspsDomesticRate also)
                    weightUomId = "WT_lb";
                }
                if (!"WT_oz".equals(weightUomId)) {
                    // attempt a conversion to pounds
                    GenericValue uomConversion = EntityQuery.use(delegator).from("UomConversion").where("uomId", weightUomId, "uomIdTo", "WT_oz").queryOne();
                    if (uomConversion == null || UtilValidate.isEmpty(uomConversion.getString("conversionFactor"))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                                "FacilityShipmentUspsWeightUnsupported", 
                                UtilMisc.toMap("weightUomId", weightUomId, "shipmentId", shipmentPackage.getString("shipmentId"), 
                                        "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"),
                                        "weightUom", "WT_oz"), locale));
                    }
                    weight = weight.multiply(uomConversion.getBigDecimal("conversionFactor"));
                }

                DecimalFormat df = new DecimalFormat("#");
                UtilXml.addChildElementValue(requestElement, "WeightInOunces", df.format(weight.setScale(0, RoundingMode.CEILING)), requestDocument);

                UtilXml.addChildElementValue(requestElement, "ServiceType", serviceType, requestDocument);
                UtilXml.addChildElementValue(requestElement, "ImageType", "TIF", requestDocument);
                UtilXml.addChildElementValue(requestElement, "AddressServiceRequested", "True", requestDocument);

                Document responseDocument = null;
                try {
                    responseDocument = sendUspsRequest("DeliveryConfirmationV2", requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
                } catch (UspsRequestException e) {
                    Debug.logInfo(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsDeliveryConfirmationSendingError", 
                            UtilMisc.toMap("errorString", e.getMessage()), locale));
                }
                Element responseElement = responseDocument.getDocumentElement();

                Element respErrorElement = UtilXml.firstChildElement(responseElement, "Error");
                if (respErrorElement != null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsDeliveryConfirmationResponseError", 
                            UtilMisc.toMap("shipmentId", shipmentPackage.getString("shipmentId"), 
                                    "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId"), 
                                    "errorString", UtilXml.childElementValue(respErrorElement, "Description")), locale));
                }

                String labelImageString = UtilXml.childElementValue(responseElement, "DeliveryConfirmationLabel");
                if (UtilValidate.isEmpty(labelImageString)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsDeliveryConfirmationResponseIncompleteElementDeliveryConfirmationLabel", locale));
                }
                shipmentPackageRouteSeg.setBytes("labelImage", Base64.base64Decode(labelImageString.getBytes(UtilIO.getUtf8())));
                String trackingCode = UtilXml.childElementValue(responseElement, "DeliveryConfirmationNumber");
                if (UtilValidate.isEmpty(trackingCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsDeliveryConfirmationResponsenIncompleteElementDeliveryConfirmationNumber", locale));
                }
                shipmentPackageRouteSeg.set("trackingCode", trackingCode);
                shipmentPackageRouteSeg.store();
            }

        } catch (GenericEntityException gee) {
            Debug.logInfo(gee, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsDeliveryConfirmationReadingError", 
                    UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    /* ------------------------------------------------------------------------------------------------------------- */

    // testing utility service - remove this
    public static Map<String, Object> uspsDumpShipmentLabelImages(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        try {
            String shipmentId = (String) context.get("shipmentId");
            String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();

            List<GenericValue> shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"), false);

            for (GenericValue shipmentPackageRouteSeg: shipmentPackageRouteSegList) {
                byte[] labelImageBytes = shipmentPackageRouteSeg.getBytes("labelImage");

                String outFileName = "UspsLabelImage" + shipmentRouteSegment.getString("shipmentId") + "_" +
                        shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_" +
                        shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".gif";

                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(labelImageBytes);
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        } catch (GenericEntityException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> uspsPriorityMailInternationalLabel(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("configProps");
        GenericValue shipmentRouteSegment = (GenericValue) context.get("shipmentRouteSegment");
        Locale locale = (Locale) context.get("locale");

        // Start the document
        Document requestDocument;
        boolean certify = false;
        String test = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "test", resource, "shipment.usps.test");
        if (!"Y".equalsIgnoreCase(test)) {
            requestDocument = createUspsRequestDocument("PriorityMailIntlRequest", false, delegator, shipmentGatewayConfigId, resource);
        } else {
            requestDocument = createUspsRequestDocument("PriorityMailIntlCertifyRequest", false, delegator, shipmentGatewayConfigId, resource);
            certify = true;
        }
        Element rootElement = requestDocument.getDocumentElement();

        // Retrieve from/to address and package details
        GenericValue originAddress = null;
        GenericValue originTelecomNumber = null;
        GenericValue destinationAddress = null;
        GenericValue destinationProvince = null;
        GenericValue destinationCountry = null;
        GenericValue destinationTelecomNumber = null;
        List<GenericValue> shipmentPackageRouteSegs = null;
        try {
            originAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            destinationAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destinationAddress != null) {
                destinationProvince = destinationAddress.getRelatedOne("StateProvinceGeo", false);
                destinationCountry = destinationAddress.getRelatedOne("CountryGeo", false);
            }
            destinationTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (originAddress == null || originTelecomNumber == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsPriorityMailLabelOriginAddressMissing", locale));
        }

        // Origin Info
        // USPS wants a separate first name and last, best we can do is split the string on the white space, if that doesn't work then default to putting the attnName in both fields
        String fromAttnName = originAddress.getString("attnName");
        String fromFirstName = StringUtils.defaultIfEmpty(StringUtils.substringBefore(fromAttnName, " "), fromAttnName);
        String fromLastName = StringUtils.defaultIfEmpty(StringUtils.substringAfter(fromAttnName, " "), fromAttnName);
        UtilXml.addChildElementValue(rootElement, "FromFirstName", fromFirstName, requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromLastName", fromLastName, requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromFirm", originAddress.getString("toName"), requestDocument);
        // The following 2 assignments are not typos - USPS address1 = OFBiz address2, USPS address2 = OFBiz address1
        UtilXml.addChildElementValue(rootElement, "FromAddress1", originAddress.getString("address2"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromAddress2", originAddress.getString("address1"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromCity", originAddress.getString("city"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromState", originAddress.getString("stateProvinceGeoId"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "FromZip5", originAddress.getString("postalCode"), requestDocument);
        // USPS expects a phone number consisting of area code + contact number as a single numeric string
        String fromPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
        fromPhoneNumber = StringUtil.removeNonNumeric(fromPhoneNumber);
        UtilXml.addChildElementValue(rootElement, "FromPhone", fromPhoneNumber, requestDocument);

        // Destination Info
        UtilXml.addChildElementValue(rootElement, "ToName", destinationAddress.getString("attnName"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToFirm", destinationAddress.getString("toName"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToAddress1", destinationAddress.getString("address1"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToAddress2", destinationAddress.getString("address2"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToCity", destinationAddress.getString("city"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToProvince", destinationProvince.getString("geoName"), requestDocument);
        // TODO: Test these country names, I think we're going to need to maintain a list of USPS names
        UtilXml.addChildElementValue(rootElement, "ToCountry", destinationCountry.getString("geoName"), requestDocument);
        UtilXml.addChildElementValue(rootElement, "ToPostalCode", destinationAddress.getString("postalCode"), requestDocument);
        // TODO: Figure out how to answer this question accurately
        UtilXml.addChildElementValue(rootElement, "ToPOBoxFlag", "N", requestDocument);
        String toPhoneNumber = destinationTelecomNumber.getString("countryCode") + destinationTelecomNumber.getString("areaCode") + destinationTelecomNumber.getString("contactNumber");
        UtilXml.addChildElementValue(rootElement, "ToPhone", toPhoneNumber, requestDocument);
        UtilXml.addChildElementValue(rootElement, "NonDeliveryOption", "RETURN", requestDocument);

        for (GenericValue shipmentPackageRouteSeg : shipmentPackageRouteSegs) {
            Document packageDocument = (Document) requestDocument.cloneNode(true);
            // This is our reference and can be whatever we want.  For lack of a better alternative we'll use shipmentId:shipmentPackageSeqId:shipmentRouteSegmentId
            String fromCustomsReference;
            fromCustomsReference = StringUtils.join(
                    UtilMisc.toList(
                            shipmentRouteSegment.get("shipmentId"),
                            shipmentPackageRouteSeg.get("shipmentPackageSeqId"),
                            shipmentRouteSegment.get("shipmentRouteSegementId")
                   ), ':');
            UtilXml.addChildElementValue(rootElement, "FromCustomsReference", fromCustomsReference, packageDocument);
            // Determine the container type for this package
            String container = "VARIABLE";
            String packageTypeCode = null;
            GenericValue shipmentPackage = null;
            List<GenericValue> shipmentPackageContents = null;
            try {
                shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent", null, null, false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType", false);
                if (shipmentBoxType != null) {
                    GenericValue carrierShipmentBoxType = EntityUtil.getFirst(shipmentBoxType.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "USPS"), null, false));
                    if (carrierShipmentBoxType != null) {
                        packageTypeCode = carrierShipmentBoxType.getString("packageTypeCode");
                        // Supported type codes
                        List<String> supportedPackageTypeCodes = UtilMisc.toList(
                                "LGFLATRATEBOX",
                                "SMFLATRATEBOX",
                                "FLATRATEBOX",
                                "MDFLATRATEBOX",
                                "FLATRATEENV");
                        if (supportedPackageTypeCodes.contains(packageTypeCode)) {
                            container = packageTypeCode;
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            UtilXml.addChildElementValue(rootElement, "Container", container, packageDocument);
            // According to the docs sending an empty postage tag will cause the postage to be calculated
            UtilXml.addChildElementValue(rootElement, "Postage", "", packageDocument);

            BigDecimal packageWeight = shipmentPackage.getBigDecimal("weight");
            String weightUomId = shipmentPackage.getString("weightUomId");
            BigDecimal packageWeightPounds = UomWorker.convertUom(packageWeight, weightUomId, "WT_lb", dispatcher);
            Integer[] packagePoundsOunces = convertPoundsToPoundsOunces(packageWeightPounds);
            UtilXml.addChildElementValue(rootElement, "GrossPounds", packagePoundsOunces[0].toString(), packageDocument);
            UtilXml.addChildElementValue(rootElement, "GrossOunces", packagePoundsOunces[1].toString(), packageDocument);

            UtilXml.addChildElementValue(rootElement, "ContentType", "MERCHANDISE", packageDocument);
            UtilXml.addChildElementValue(rootElement, "Agreement", "N", packageDocument);
            UtilXml.addChildElementValue(rootElement, "ImageType", "PDF", packageDocument);
            // TODO: Try the different layouts
            UtilXml.addChildElementValue(rootElement, "ImageType", "ALLINONEFILE", packageDocument);
            UtilXml.addChildElementValue(rootElement, "CustomerRefNo", fromCustomsReference, packageDocument);

            // Add the shipping contents
            Element shippingContents = UtilXml.addChildElement(rootElement, "ShippingContents", packageDocument);
            for (GenericValue shipmentPackageContent : shipmentPackageContents) {
                Element itemDetail = UtilXml.addChildElement(shippingContents, "ItemDetail", packageDocument);
                GenericValue product = null;
                GenericValue originGeo = null;
                try {
                    GenericValue shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem", false);
                    product = shipmentItem.getRelatedOne("Product", false);
                    originGeo = product.getRelatedOne("OriginGeo", false);
                } catch (GenericEntityException e) {
                    Debug.logInfo(e, module);
                }

                UtilXml.addChildElementValue(itemDetail, "Description", product.getString("productName"), packageDocument);
                UtilXml.addChildElementValue(itemDetail, "Quantity", shipmentPackageContent.getBigDecimal("quantity").setScale(0, RoundingMode.CEILING).toPlainString(), packageDocument);
                String packageContentValue = ShipmentWorker.getShipmentPackageContentValue(shipmentPackageContent).setScale(2, RoundingMode.HALF_UP).toPlainString();
                UtilXml.addChildElementValue(itemDetail, "Value", packageContentValue, packageDocument);
                BigDecimal productWeight = ProductWorker.getProductWeight(product, "WT_lbs", delegator, dispatcher);
                Integer[] productPoundsOunces = convertPoundsToPoundsOunces(productWeight);
                UtilXml.addChildElementValue(itemDetail, "NetPounds", productPoundsOunces[0].toString(), packageDocument);
                UtilXml.addChildElementValue(itemDetail, "NetOunces", productPoundsOunces[1].toString(), packageDocument);
                UtilXml.addChildElementValue(itemDetail, "HSTariffNumber", "", packageDocument);
                UtilXml.addChildElementValue(itemDetail, "CountryOfOrigin", originGeo.getString("geoName"), packageDocument);
            }

            // Send the request
            Document responseDocument = null;
            String api = certify ? "PriorityMailIntlCertify" : "PriorityMailIntl";
            try {
                responseDocument = sendUspsRequest(api, requestDocument, delegator, shipmentGatewayConfigId, resource, locale);
            } catch (UspsRequestException e) {
                Debug.logInfo(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsPriorityMailLabelSendingError", 
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
            Element responseElement = responseDocument.getDocumentElement();

            // TODO: No mention of error returns in the docs

            String labelImageString = UtilXml.childElementValue(responseElement, "LabelImage");
            if (UtilValidate.isEmpty(labelImageString)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsPriorityMailLabelResponseIncompleteElementLabelImage", locale));
            }
            shipmentPackageRouteSeg.setBytes("labelImage", Base64.base64Decode(labelImageString.getBytes(UtilIO.getUtf8())));
            String trackingCode = UtilXml.childElementValue(responseElement, "BarcodeNumber");
            if (UtilValidate.isEmpty(trackingCode)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "FacilityShipmentUspsPriorityMailLabelResponseIncompleteElementBarcodeNumber", locale));
            }
            shipmentPackageRouteSeg.set("trackingCode", trackingCode);
            try {
                shipmentPackageRouteSeg.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

        }
        return ServiceUtil.returnSuccess();
    }

    private static Document createUspsRequestDocument(String rootElement, boolean passwordRequired, Delegator delegator, String shipmentGatewayConfigId, String resource) {
        Document requestDocument = UtilXml.makeEmptyXmlDocument(rootElement);
        Element requestElement = requestDocument.getDocumentElement();
        requestElement.setAttribute("USERID", getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, 
                "accessUserId", resource, "shipment.usps.access.userid", ""));
        if (passwordRequired) {
            requestElement.setAttribute("PASSWORD", getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, 
                    "accessPassword", resource, "shipment.usps.access.password", ""));
        }
        return requestDocument;
    }

    private static Document sendUspsRequest(String requestType, Document requestDocument, Delegator delegator,
            String shipmentGatewayConfigId, String resource, Locale locale) throws UspsRequestException {
        String conUrl = null;
        List<String> labelRequestTypes = UtilMisc.toList("PriorityMailIntl", "PriorityMailIntlCertify");
        if (labelRequestTypes.contains(requestType)) {
            conUrl = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectUrlLabels", resource, "shipment.usps.connect.url.labels");
        } else {
            conUrl = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectUrl", resource, "shipment.usps.connect.url");
        }
        if (UtilValidate.isEmpty(conUrl)) {
            throw new UspsRequestException(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsConnectUrlIncomplete", locale));
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            UtilXml.writeXmlDocument(requestDocument, os, "UTF-8", true, false, 0);
        } catch (TransformerException e) {
            throw new UspsRequestException(
                    UtilProperties.getMessage(resourceError, 
                            "FacilityShipmentUspsSerializingError",
                            UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        String xmlString = new String(os.toByteArray(), UtilIO.getUtf8());

        Debug.logInfo("USPS XML request string: " + xmlString, module);

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectTimeout", 
                resource, "shipment.usps.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        HttpClient http = new HttpClient(conUrl);
        http.setTimeout(timeout * 1000);
        http.setParameter("API", requestType);
        http.setParameter("XML", xmlString);

        String responseString = null;
        try {
            responseString = http.get();
        } catch (HttpClientException e) {
            throw new UspsRequestException(UtilProperties.getMessage(resourceError, 
                    "FacilityShipmentUspsConnectionProblem",
                    UtilMisc.toMap("errorString", e), locale));
        }

        Debug.logInfo("USPS response: " + responseString, module);

        if (UtilValidate.isEmpty(responseString)) {
            return null;
        }

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(responseString, false);
        } catch (Exception e) {
            throw new UspsRequestException(UtilProperties.getMessage(resourceError, "FacilityShipmentUspsResponseError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        // If a top-level error document is returned, throw exception
        // Other request-level errors should be handled by the caller
        Element responseElement = responseDocument.getDocumentElement();
        if ("Error".equals(responseElement.getNodeName())) {
            throw new UspsRequestException(UtilXml.childElementValue(responseElement, "Description"));
        }

        return responseDocument;
    }

    /*
     * Converts decimal pounds to pounds and ounces as an Integer array, ounces are rounded up to the nearest whole number
     */
    public static Integer[] convertPoundsToPoundsOunces(BigDecimal decimalPounds) {
        if (decimalPounds == null) return null;
        Integer[] poundsOunces = new Integer[2];
        poundsOunces[0] = Integer.valueOf(decimalPounds.setScale(0, RoundingMode.FLOOR).toPlainString());
        // (weight % 1) * 16 rounded up to nearest whole number
        poundsOunces[1] = Integer.valueOf(decimalPounds.remainder(BigDecimal.ONE).multiply(new BigDecimal("16")).setScale(0, RoundingMode.CEILING).toPlainString());
        return poundsOunces;
    }
    
    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String shipmentGatewayConfigParameterName, 
            String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                GenericValue usps = EntityQuery.use(delegator).from("ShipmentGatewayUsps").where("shipmentGatewayConfigId", shipmentGatewayConfigId).queryOne();
                if (usps != null) {
                    Object uspsField = usps.get(shipmentGatewayConfigParameterName);
                    if (uspsField != null) {
                        returnValue = uspsField.toString().trim();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            String value = EntityUtilProperties.getPropertyValue(resource, parameterName, delegator);
            if (value != null) {
                returnValue = value.trim();
            }
        }
        return returnValue;
    }
        
    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId, String shipmentGatewayConfigParameterName, 
            String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName, resource, parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}

@SuppressWarnings("serial")
class UspsRequestException extends GeneralException {
    UspsRequestException() {
        super();
    }

    UspsRequestException(String msg) {
        super(msg);
    }

    UspsRequestException(Throwable t) {
        super(t);
    }

    UspsRequestException(String msg, Throwable t) {
        super(msg, t);
    }
}
