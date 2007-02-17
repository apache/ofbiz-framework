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

package org.ofbiz.shipment.thirdparty.usps;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * USPS Webtools API Services
 */
public class UspsServices {

    public final static String module = UspsServices.class.getName();

    public static Map uspsRateInquire(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();

        // check for 0 weight
        Double shippableWeight = (Double) context.get("shippableWeight");
        if (shippableWeight.doubleValue() == 0) {
            // TODO: should we return an error, or $0.00 ?
            return ServiceUtil.returnError("shippableWeight must be greater than 0");
        }

        // get the origination ZIP
        String originationZip = null;
        GenericValue productStore = ProductStoreWorker.getProductStore(((String) context.get("productStoreId")), delegator);
        if (productStore != null && productStore.get("inventoryFacilityId") != null) {
            try {
                List shipLocs = delegator.findByAnd("FacilityContactMechPurpose",
                        UtilMisc.toMap("facilityId", productStore.getString("inventoryFacilityId"),
                                "contactMechPurposeTypeId", "SHIP_ORIG_LOCATION"), UtilMisc.toList("-fromDate"));
                if (UtilValidate.isNotEmpty(shipLocs)) {
                    shipLocs = EntityUtil.filterByDate(shipLocs);
                    GenericValue purp = EntityUtil.getFirst(shipLocs);
                    if (purp != null) {
                        GenericValue shipFromAddress = delegator.findByPrimaryKey("PostalAddress",
                                UtilMisc.toMap("contactMechId", purp.getString("contactMechId")));
                        if (shipFromAddress != null) {
                            originationZip = shipFromAddress.getString("postalCode");
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (UtilValidate.isEmpty(originationZip)) {
            return ServiceUtil.returnError("Unable to determine the origination ZIP");
        }

        // get the destination ZIP
        String destinationZip = null;
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            try {
                GenericValue shipToAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
                if (shipToAddress != null) {
                    destinationZip = shipToAddress.getString("postalCode");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (UtilValidate.isEmpty(destinationZip)) {
            return ServiceUtil.returnError("Unable to determine the destination ZIP");
        }

        // get the service code
        String serviceCode = null;
        try {
            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod",
                    UtilMisc.toMap("shipmentMethodTypeId", (String) context.get("shipmentMethodTypeId"),
                            "partyId", (String) context.get("carrierPartyId"), "roleTypeId", (String) context.get("carrierRoleTypeId")));
            if (carrierShipmentMethod != null) {
                serviceCode = carrierShipmentMethod.getString("carrierServiceCode");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isEmpty(serviceCode)) {
            return ServiceUtil.returnError("Unable to determine the service code");
        }

        // create the request document
        Document requestDocument = createUspsRequestDocument("RateV2Request");

        // TODO: 70 lb max is valid for Express, Priority and Parcel only - handle other methods
        double maxWeight = 70;
        String maxWeightStr = UtilProperties.getPropertyValue((String) context.get("serviceConfigProps"),
                "shipment.usps.max.estimate.weight", "70");
        try {
            maxWeight = Double.parseDouble(maxWeightStr);
        } catch (NumberFormatException e) {
            Debug.logWarning("Error parsing max estimate weight string [" + maxWeightStr + "], using default instead", module);
            maxWeight = 70;
        }

        List shippableItemInfo = (List) context.get("shippableItemInfo");
        List packages = getPackageSplit(dctx, shippableItemInfo, maxWeight);
        // TODO: Up to 25 packages can be included per request - handle more than 25
        for (ListIterator li = packages.listIterator(); li.hasNext();) {
            Map packageMap = (Map) li.next();

            double packageWeight = calcPackageWeight(dctx, packageMap, shippableItemInfo, 0);
            if (packageWeight == 0) {
                continue;
            }

            Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
            packageElement.setAttribute("ID", String.valueOf(li.nextIndex() - 1)); // use zero-based index (see examples)

            UtilXml.addChildElementValue(packageElement, "Service", serviceCode, requestDocument);
            UtilXml.addChildElementValue(packageElement, "ZipOrigination", originationZip.substring(0,5), requestDocument);
            UtilXml.addChildElementValue(packageElement, "ZipDestination", destinationZip.substring(0,5), requestDocument);

            double weightPounds = Math.floor(packageWeight);
            // for Parcel post, the weight must be at least 1 lb
            if ("PARCEL".equals(serviceCode.toUpperCase()) && (weightPounds < 1.0)) {
                weightPounds = 1.0;
                packageWeight = 0.0;
            }
            double weightOunces = Math.ceil(packageWeight * 16 % 16);
            DecimalFormat df = new DecimalFormat("#");  // USPS only accepts whole numbers like 1 and not 1.0
            UtilXml.addChildElementValue(packageElement, "Pounds", df.format(weightPounds), requestDocument);
            UtilXml.addChildElementValue(packageElement, "Ounces", df.format(weightOunces), requestDocument);

            // TODO: handle other container types, package sizes, and machinabile packages
            UtilXml.addChildElementValue(packageElement, "Container", "None", requestDocument);
            UtilXml.addChildElementValue(packageElement, "Size", "Regular", requestDocument);
            UtilXml.addChildElementValue(packageElement, "Machinable", "False", requestDocument);
        }

        // send the request
        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("RateV2", requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS Domestic Rate Calculation service: " + e.getMessage());
        }

        List rates = UtilXml.childElementList(responseDocument.getDocumentElement(), "Package");
        if (UtilValidate.isEmpty(rates)) {
            return ServiceUtil.returnError("No rate available at this time");
        }

        double estimateAmount = 0.00;
        for (Iterator i = rates.iterator(); i.hasNext();) {
            Element packageElement = (Element) i.next();
            try {
                Element postageElement = UtilXml.firstChildElement(packageElement, "Postage");
                double packageAmount = Double.parseDouble(UtilXml.childElementValue(postageElement, "Rate"));
                estimateAmount += packageAmount;
            } catch (NumberFormatException e) {
                Debug.log(e, module);
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("shippingEstimateAmount", new Double(estimateAmount));
        return result;
    }

    private static List getPackageSplit(DispatchContext dctx, List shippableItemInfo, double maxWeight) {
        // create the package list w/ the first pacakge
        List packages = new LinkedList();

        if (shippableItemInfo != null) {
            Iterator sii = shippableItemInfo.iterator();
            while (sii.hasNext()) {
                Map itemInfo = (Map) sii.next();
                long pieces = ((Long) itemInfo.get("piecesIncluded")).longValue();
                double totalQuantity = ((Double) itemInfo.get("quantity")).doubleValue();
                double totalWeight = ((Double) itemInfo.get("weight")).doubleValue();
                String productId = (String) itemInfo.get("productId");

                // sanity check
                if (pieces < 1) {
                    pieces = 1; // can NEVER be less than one
                }
                double weight = totalWeight / pieces;

                for (int z = 1; z <= totalQuantity; z++) {
                    double partialQty = pieces > 1 ? 1.000 / pieces : 1;
                    for (long x = 0; x < pieces; x++) {
                        if (weight >= maxWeight) {
                            Map newPackage = new HashMap();
                            newPackage.put(productId, new Double(partialQty));
                            packages.add(newPackage);
                        } else if (totalWeight > 0) {
                            // create the first package
                            if (packages.size() == 0) {
                                packages.add(new HashMap());
                            }

                            // package loop
                            int packageSize = packages.size();
                            boolean addedToPackage = false;
                            for (int pi = 0; pi < packageSize; pi++) {
                                if (!addedToPackage) {
                                    Map packageMap = (Map) packages.get(pi);
                                    double packageWeight = calcPackageWeight(dctx, packageMap, shippableItemInfo, weight);
                                    if (packageWeight <= maxWeight) {
                                        Double qtyD = (Double) packageMap.get(productId);
                                        double qty = qtyD == null ? 0 : qtyD.doubleValue();
                                        packageMap.put(productId, new Double(qty + partialQty));
                                        addedToPackage = true;
                                    }
                                }
                            }
                            if (!addedToPackage) {
                                Map packageMap = new HashMap();
                                packageMap.put(productId, new Double(partialQty));
                                packages.add(packageMap);
                            }
                        }
                    }
                }
            }
        }
        return packages;
    }

    private static double calcPackageWeight(DispatchContext dctx, Map packageMap, List shippableItemInfo, double additionalWeight) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        double totalWeight = 0.00;
        String defaultWeightUomId = UtilProperties.getPropertyValue("shipment.properties", "shipment.default.weight.uom");
        if (UtilValidate.isEmpty(defaultWeightUomId)) {
            Debug.logWarning("No shipment.default.weight.uom set in shipment.properties, setting it to WT_oz for USPS", module);
            defaultWeightUomId = "WT_oz";
        }
        
        Iterator i = packageMap.keySet().iterator();
        while (i.hasNext()) {
            String productId = (String) i.next();
            Map productInfo = getProductItemInfo(shippableItemInfo, productId);
            double productWeight = ((Double) productInfo.get("weight")).doubleValue();
            double quantity = ((Double) packageMap.get(productId)).doubleValue();

            // DLK - I'm not sure if this line is working. shipment_package seems to leave this value null so???
            String weightUomId = (String) productInfo.get("weight_uom_id");

            Debug.logInfo("Product Id : " + productId.toString() + " Product Weight : " + String.valueOf(productWeight) + " Product UomId : " + weightUomId + " assuming " + defaultWeightUomId + " if null. Quantity : " + String.valueOf(quantity), module);

            if (UtilValidate.isEmpty(weightUomId)) {
                weightUomId = defaultWeightUomId; 
                //  Most shipping modules assume pounds while ProductEvents.java assumes WT_oz. - Line 720 for example.
            }
            if (!"WT_lb".equals(weightUomId)) {
                // attempt a conversion to pounds
                Map result = new HashMap();
                try {
                    result = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", weightUomId, "uomIdTo", "WT_lb", "originalValue", new Double(productWeight)));
                } catch (GenericServiceException ex) {
                    Debug.logError(ex, module);
                }
                    
                if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                    productWeight = ((Double) result.get("convertedValue")).doubleValue();
                } else {
                    Debug.logError("Unsupported weightUom [" + weightUomId + "] for calcPackageWeight running productId " + productId + ", could not find a conversion factor to WT_lb",module);
                }
                    
            }

            totalWeight += (productWeight * quantity);
        }
        Debug.logInfo("Package Weight : " + String.valueOf(totalWeight) + " lbs.", module);
        return totalWeight + additionalWeight;
    }

    // lifted from UpsServices with no changes - 2004.09.06 JFE
    private static Map getProductItemInfo(List shippableItemInfo, String productId) {
        if (shippableItemInfo != null) {
            Iterator i = shippableItemInfo.iterator();
            while (i.hasNext()) {
                Map testMap = (Map) i.next();
                String id = (String) testMap.get("productId");
                if (productId.equals(id)) {
                    return testMap;
                }
            }
        }
        return null;
    }

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

    public static Map uspsTrackConfirm(DispatchContext dctx, Map context) {

        Document requestDocument = createUspsRequestDocument("TrackRequest");

        Element trackingElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "TrackID", requestDocument);
        trackingElement.setAttribute("ID", (String) context.get("trackingId"));

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("TrackV2", requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS Tracking service: " + e.getMessage());
        }

        Element trackInfoElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "TrackInfo");
        if (trackInfoElement == null) {
            return ServiceUtil.returnError("Incomplete response from USPS Tracking service: no TrackInfo element found");
        }

        Map result = ServiceUtil.returnSuccess();

        result.put("trackingSummary", UtilXml.childElementValue(trackInfoElement, "TrackSummary"));

        List detailElementList = UtilXml.childElementList(trackInfoElement, "TrackDetail");
        if (UtilValidate.isNotEmpty(detailElementList)) {
            List trackingDetailList = new ArrayList();
            for (Iterator iter = detailElementList.iterator(); iter.hasNext();) {
                trackingDetailList.add(UtilXml.elementValue((Element) iter.next()));
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

    public static Map uspsAddressValidation(DispatchContext dctx, Map context) {

        Document requestDocument = createUspsRequestDocument("AddressValidateRequest");

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
            responseDocument = sendUspsRequest("Verify", requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS Address Validation service: " + e.getMessage());
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Address");
        if (respAddressElement == null) {
            return ServiceUtil.returnError("Incomplete response from USPS Address Validation service: no Address element found");
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnError("The following error was returned by the USPS Address Validation service: " +
                    UtilXml.childElementValue(respErrorElement, "Description"));
        }

        Map result = ServiceUtil.returnSuccess();

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

    public static Map uspsCityStateLookup(DispatchContext dctx, Map context) {

        Document requestDocument = createUspsRequestDocument("CityStateLookupRequest");

        Element zipCodeElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "ZipCode", requestDocument);
        zipCodeElement.setAttribute("ID", "0");

        String zipCode = ((String) context.get("zip5")).trim(); // trim leading/trailing spaces

        // only the first 5 digits are used, the rest are ignored
        UtilXml.addChildElementValue(zipCodeElement, "Zip5", zipCode, requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest("CityStateLookup", requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS City/State Lookup service: " + e.getMessage());
        }

        Element respAddressElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "ZipCode");
        if (respAddressElement == null) {
            return ServiceUtil.returnError("Incomplete response from USPS City/State Lookup service: no ZipCode element found");
        }

        Element respErrorElement = UtilXml.firstChildElement(respAddressElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnError("The following error was returned by the USPS City/State Lookup service: " +
                    UtilXml.childElementValue(respErrorElement, "Description"));
        }

        Map result = ServiceUtil.returnSuccess();

        String city = UtilXml.childElementValue(respAddressElement, "City");
        if (UtilValidate.isEmpty(city)) {
            return ServiceUtil.returnError("Incomplete response from USPS City/State Lookup service: no City element found");
        }
        result.put("city", city);

        String state = UtilXml.childElementValue(respAddressElement, "State");
        if (UtilValidate.isEmpty(state)) {
            return ServiceUtil.returnError("Incomplete response from USPS City/State Lookup service: no State element found");
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

    public static Map uspsPriorityMailStandard(DispatchContext dctx, Map context) {
        context.put("serviceType", "PriorityMail");
        return uspsServiceStandards(dctx, context);
    }

    public static Map uspsPackageServicesStandard(DispatchContext dctx, Map context) {
        context.put("serviceType", "StandardB");
        return uspsServiceStandards(dctx, context);
    }

    private static Map uspsServiceStandards(DispatchContext dctx, Map context) {

        String type = (String) context.get("serviceType");
        if (!type.matches("PriorityMail|StandardB")) {
            return ServiceUtil.returnError("Unsupported service type: " + type);
        }

        Document requestDocument = createUspsRequestDocument(type + "Request");

        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), "OriginZip",
                (String) context.get("originZip"), requestDocument);
        UtilXml.addChildElementValue(requestDocument.getDocumentElement(), "DestinationZip",
                (String) context.get("destinationZip"), requestDocument);

        Document responseDocument = null;
        try {
            responseDocument = sendUspsRequest(type, requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS " + type + " Service Standards service: " +
                    e.getMessage());
        }

        Map result = ServiceUtil.returnSuccess();

        String days = UtilXml.childElementValue(responseDocument.getDocumentElement(), "Days");
        if (UtilValidate.isEmpty(days)) {
            return ServiceUtil.returnError("Incomplete response from USPS " + type + " Service Standards service: " +
                    "no Days element found");
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

    public static Map uspsDomesticRate(DispatchContext dctx, Map context) {

        Document requestDocument = createUspsRequestDocument("RateRequest");

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
            responseDocument = sendUspsRequest("Rate", requestDocument);
        } catch (UspsRequestException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError("Error sending request for USPS Domestic Rate Calculation service: " + e.getMessage());
        }

        Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Package");
        if (respPackageElement == null) {
            return ServiceUtil.returnError("Incomplete response from USPS Domestic Rate Calculation service: no Package element found");
        }

        Element respErrorElement = UtilXml.firstChildElement(respPackageElement, "Error");
        if (respErrorElement != null) {
            return ServiceUtil.returnError("The following error was returned by the USPS Domestic Rate Calculation service: " +
                    UtilXml.childElementValue(respErrorElement, "Description"));
        }

        Map result = ServiceUtil.returnSuccess();

        String zone = UtilXml.childElementValue(respPackageElement, "Zone");
        if (UtilValidate.isEmpty(zone)) {
            return ServiceUtil.returnError("Incomplete response from USPS Domestic Rate Calculation service: no Zone element found");
        }
        result.put("zone", zone);

        String postage = UtilXml.childElementValue(respPackageElement, "Postage");
        if (UtilValidate.isEmpty(postage)) {
            return ServiceUtil.returnError("Incomplete response from USPS Domestic Rate Calculation service: no Postage element found");
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

    public static Map uspsUpdateShipmentRateInfo(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        // ShipmentRouteSegment identifier - used in error messages
        String srsKeyString = "[" + shipmentId + "," + shipmentRouteSegmentId + "]";

        try {
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment",
                    UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError("ShipmentRouteSegment " + srsKeyString + " not found");
            }

            // ensure the carrier is USPS
            if (!"USPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("The Carrier for ShipmentRouteSegment " + srsKeyString + ", is not USPS");
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress");
            if (originAddress == null) {
                return ServiceUtil.returnError("OriginPostalAddress not found for ShipmentRouteSegment [" +
                        shipmentId + ":" + shipmentRouteSegmentId + "]");
            }
            if (!"USA".equals(originAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("ShipmentRouteSeqment " + srsKeyString + " does not originate from a US address");
            }
            String originZip = originAddress.getString("postalCode");
            if (UtilValidate.isEmpty(originZip)) {
                return ServiceUtil.returnError("ZIP code is missing from the origin postal address" +
                        " (contactMechId " + originAddress.getString("contactMechId") + ")");
            }

            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress");
            if (destinationAddress == null) {
                return ServiceUtil.returnError("DestPostalAddress not found for ShipmentRouteSegment " + srsKeyString);
            }
            if (!"USA".equals(destinationAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("ShipmentRouteSeqment " + srsKeyString + " is not destined for a US address");
            }
            String destinationZip = destinationAddress.getString("postalCode");
            if (UtilValidate.isEmpty(destinationZip)) {
                return ServiceUtil.returnError("ZIP code is missing from the destination postal address" +
                        " (contactMechId " + originAddress.getString("contactMechId") + ")");
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String partyId = shipmentRouteSegment.getString("carrierPartyId");
            String csmKeystring = "[" + shipmentMethodTypeId + "," + partyId + ",CARRIER]";

            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod",
                    UtilMisc.toMap("partyId", partyId, "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentMethodTypeId));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("CarrierShipmentMethod " + csmKeystring +
                        " not found for ShipmentRouteSegment " + srsKeyString);
            }
            String serviceType = carrierShipmentMethod.getString("carrierServiceCode");
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError("carrierServiceCode not found for CarrierShipmentMethod" + csmKeystring);
            }

            // get the packages for this shipment route segment
            List shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null,
                    UtilMisc.toList("+shipmentPackageSeqId"));
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError("No packages found for ShipmentRouteSegment " + srsKeyString);
            }

            double actualTransportCost = 0;

            String carrierDeliveryZone = null;
            String carrierRestrictionCodes = null;
            String carrierRestrictionDesc = null;

            // send a new request for each package
            for (Iterator i = shipmentPackageRouteSegList.iterator(); i.hasNext();) {

                GenericValue shipmentPackageRouteSeg = (GenericValue) i.next();
                String sprsKeyString = "[" + shipmentPackageRouteSeg.getString("shipmentId") + "," +
                        shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + "," +
                        shipmentPackageRouteSeg.getString("shipmentRouteSegmentId") + "]";

                Document requestDocument = createUspsRequestDocument("RateRequest");

                Element packageElement = UtilXml.addChildElement(requestDocument.getDocumentElement(), "Package", requestDocument);
                packageElement.setAttribute("ID", "0");

                UtilXml.addChildElementValue(packageElement, "Service", serviceType, requestDocument);
                UtilXml.addChildElementValue(packageElement, "ZipOrigination", originZip, requestDocument);
                UtilXml.addChildElementValue(packageElement, "ZipDestination", destinationZip, requestDocument);

                GenericValue shipmentPackage = null;
                shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage");

                String spKeyString = "[" + shipmentPackage.getString("shipmentId") + "," +
                        shipmentPackage.getString("shipmentPackageSeqId") + "]";

                // weight elements - Pounds, Ounces
                String weightStr = shipmentPackage.getString("weight");
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError("weight not found for ShipmentPackage " + spKeyString);
                }

                double weight = 0;
                try {
                    weight = Double.parseDouble(weightStr);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace(); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString("weightUomId");
                if (UtilValidate.isEmpty(weightUomId)) {
                    weightUomId = "WT_lb"; // assume weight is in pounds
                }
                if (!"WT_lb".equals(weightUomId)) {
                    // attempt a conversion to pounds
                    Map result = new HashMap();
                    try {
                        result = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", weightUomId, "uomIdTo", "WT_lb", "originalValue", new Double(weight)));
                    } catch (GenericServiceException ex) {
                        return ServiceUtil.returnError(ex.getMessage());
                    }
                    
                    if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                        weight *= ((Double) result.get("convertedValue")).doubleValue();
                    } else {
                        return ServiceUtil.returnError("Unsupported weightUom [" + weightUomId + "] for ShipmentPackage " +
                                spKeyString + ", could not find a conversion factor for WT_lb");
                    }
                    
                }

                double weightPounds = Math.floor(weight);
                double weightOunces = Math.ceil(weight * 16 % 16);

                DecimalFormat df = new DecimalFormat("#");
                UtilXml.addChildElementValue(packageElement, "Pounds", df.format(weightPounds), requestDocument);
                UtilXml.addChildElementValue(packageElement, "Ounces", df.format(weightOunces), requestDocument);

                // Container element
                GenericValue carrierShipmentBoxType = null;
                List carrierShipmentBoxTypes = null;
                carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType",
                        UtilMisc.toMap("partyId", "USPS"), null);

                if (carrierShipmentBoxTypes.size() > 0) {
                    carrierShipmentBoxType = (GenericValue) carrierShipmentBoxTypes.get(0);
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
                    responseDocument = sendUspsRequest("Rate", requestDocument);
                } catch (UspsRequestException e) {
                    Debug.log(e, module);
                    return ServiceUtil.returnError("Error sending request for USPS Domestic Rate Calculation service: " +
                            e.getMessage());
                }

                Element respPackageElement = UtilXml.firstChildElement(responseDocument.getDocumentElement(), "Package");
                if (respPackageElement == null) {
                    return ServiceUtil.returnError("Incomplete response from USPS Domestic Rate Calculation service: " +
                            "no Package element found");
                }

                Element respErrorElement = UtilXml.firstChildElement(respPackageElement, "Error");
                if (respErrorElement != null) {
                    return ServiceUtil.returnError("The following error was returned by the USPS Domestic Rate Calculation " +
                            "service for ShipmentPackage " + spKeyString + ": " +
                            UtilXml.childElementValue(respErrorElement, "Description"));
                }

                // update the ShipmentPackageRouteSeg
                String postageString = UtilXml.childElementValue(respPackageElement, "Postage");
                if (UtilValidate.isEmpty(postageString)) {
                    return ServiceUtil.returnError("Incomplete response from USPS Domestic Rate Calculation service: " +
                            "missing or empty Postage element");
                }

                double postage = 0;
                try {
                    postage = Double.parseDouble(postageString);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace(); // TODO: handle exception
                }
                actualTransportCost += postage;

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
            Debug.log(gee, module);
            return ServiceUtil.returnError("Error reading or writing shipment data for the USPS " +
                    "Domestic Rate Calculation service: " + gee.getMessage());
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

    public static Map uspsDeliveryConfirmation(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        // ShipmentRouteSegment identifier - used in error messages
        String srsKeyString = "[" + shipmentId + "," + shipmentRouteSegmentId + "]";

        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (shipment == null) {
                return ServiceUtil.returnError("Shipment not found with ID " + shipmentId);
            }

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment",
                    UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError("ShipmentRouteSegment not found with shipmentId " + shipmentId +
                        " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }

            // ensure the carrier is USPS
            if (!"USPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("The Carrier for ShipmentRouteSegment " + srsKeyString + ", is not USPS");
            }

            // get the origin address
            GenericValue originAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress");
            if (originAddress == null) {
                return ServiceUtil.returnError("OriginPostalAddress not found for ShipmentRouteSegment [" +
                        shipmentId + ":" + shipmentRouteSegmentId + "]");
            }
            if (!"USA".equals(originAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("ShipmentRouteSeqment " + srsKeyString + " does not originate from a US address");
            }

            // get the destination address
            GenericValue destinationAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress");
            if (destinationAddress == null) {
                return ServiceUtil.returnError("DestPostalAddress not found for ShipmentRouteSegment " + srsKeyString);
            }
            if (!"USA".equals(destinationAddress.getString("countryGeoId"))) {
                return ServiceUtil.returnError("ShipmentRouteSeqment " + srsKeyString + " is not destined for a US address");
            }

            // get the service type from the CarrierShipmentMethod
            String shipmentMethodTypeId = shipmentRouteSegment.getString("shipmentMethodTypeId");
            String partyId = shipmentRouteSegment.getString("carrierPartyId");

            String csmKeystring = "[" + shipmentMethodTypeId + "," + partyId + ",CARRIER]";

            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod",
                    UtilMisc.toMap("partyId", partyId, "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentMethodTypeId));
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("CarrierShipmentMethod " + csmKeystring +
                        " not found for ShipmentRouteSegment " + srsKeyString);
            }
            String serviceType = carrierShipmentMethod.getString("carrierServiceCode");
            if (UtilValidate.isEmpty(serviceType)) {
                return ServiceUtil.returnError("carrierServiceCode not found for CarrierShipmentMethod" + csmKeystring);
            }

            // get the packages for this shipment route segment
            List shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null,
                    UtilMisc.toList("+shipmentPackageSeqId"));
            if (UtilValidate.isEmpty(shipmentPackageRouteSegList)) {
                return ServiceUtil.returnError("No packages found for ShipmentRouteSegment " + srsKeyString);
            }

            for (Iterator i = shipmentPackageRouteSegList.iterator(); i.hasNext();) {

                Document requestDocument = createUspsRequestDocument("DeliveryConfirmationV2.0Request");
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

                GenericValue shipmentPackageRouteSeg = (GenericValue) i.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage");
                String spKeyString = "[" + shipmentPackage.getString("shipmentId") + "," +
                        shipmentPackage.getString("shipmentPackageSeqId") + "]";

                // WeightInOunces
                String weightStr = shipmentPackage.getString("weight");
                if (UtilValidate.isEmpty(weightStr)) {
                    return ServiceUtil.returnError("weight not found for ShipmentPackage " + spKeyString);
                }

                double weight = 0;
                try {
                    weight = Double.parseDouble(weightStr);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace(); // TODO: handle exception
                }

                String weightUomId = shipmentPackage.getString("weightUomId");
                if (UtilValidate.isEmpty(weightUomId)) {
                    // assume weight is in pounds for consistency (this assumption is made in uspsDomesticRate also)
                    weightUomId = "WT_lb";
                }
                if (!"WT_oz".equals(weightUomId)) {
                    // attempt a conversion to pounds
                    GenericValue uomConversion = delegator.findByPrimaryKey("UomConversion",
                            UtilMisc.toMap("uomId", weightUomId, "uomIdTo", "WT_oz"));
                    if (uomConversion == null || UtilValidate.isEmpty(uomConversion.getString("conversionFactor"))) {
                        return ServiceUtil.returnError("Unsupported weightUom [" + weightUomId + "] for ShipmentPackage " +
                                spKeyString + ", could not find a conversion factor for WT_oz");
                    }
                    weight *= uomConversion.getDouble("conversionFactor").doubleValue();
                }

                DecimalFormat df = new DecimalFormat("#");
                UtilXml.addChildElementValue(requestElement, "WeightInOunces", df.format(Math.ceil(weight)), requestDocument);

                UtilXml.addChildElementValue(requestElement, "ServiceType", serviceType, requestDocument);
                UtilXml.addChildElementValue(requestElement, "ImageType", "TIF", requestDocument);
                UtilXml.addChildElementValue(requestElement, "AddressServiceRequested", "True", requestDocument);

                Document responseDocument = null;
                try {
                    responseDocument = sendUspsRequest("DeliveryConfirmationV2", requestDocument);
                } catch (UspsRequestException e) {
                    Debug.log(e, module);
                    return ServiceUtil.returnError("Error sending request for USPS Delivery Confirmation service: " +
                            e.getMessage());
                }
                Element responseElement = responseDocument.getDocumentElement();

                Element respErrorElement = UtilXml.firstChildElement(responseElement, "Error");
                if (respErrorElement != null) {
                    return ServiceUtil.returnError("The following error was returned by the USPS Delivery Confirmation " +
                            "service for ShipmentPackage " + spKeyString + ": " +
                            UtilXml.childElementValue(respErrorElement, "Description"));
                }

                String labelImageString = UtilXml.childElementValue(responseElement, "DeliveryConfirmationLabel");
                if (UtilValidate.isEmpty(labelImageString)) {
                    return ServiceUtil.returnError("Incomplete response from the USPS Delivery Confirmation service: " +
                            "missing or empty DeliveryConfirmationLabel element");
                }
                shipmentPackageRouteSeg.setBytes("labelImage", Base64.base64Decode(labelImageString.getBytes()));
                String trackingCode = UtilXml.childElementValue(responseElement, "DeliveryConfirmationNumber");
                if (UtilValidate.isEmpty(trackingCode)) {
                    return ServiceUtil.returnError("Incomplete response from the USPS Delivery Confirmation service: " +
                            "missing or empty DeliveryConfirmationNumber element");
                }
                shipmentPackageRouteSeg.set("trackingCode", trackingCode);
                shipmentPackageRouteSeg.store();
            }

        } catch (GenericEntityException gee) {
            Debug.log(gee, module);
            return ServiceUtil.returnError("Error reading or writing shipment data for the USPS " +
                    "Delivery Confirmation service: " + gee.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /* ------------------------------------------------------------------------------------------------------------- */

    // testing utility service - remove this
    public static Map uspsDumpShipmentLabelImages(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();

        try {

            String shipmentId = (String) context.get("shipmentId");
            String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment",
                    UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));

            List shipmentPackageRouteSegList = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null,
                    UtilMisc.toList("+shipmentPackageSeqId"));

            for (Iterator i = shipmentPackageRouteSegList.iterator(); i.hasNext();) {
                GenericValue shipmentPackageRouteSeg = (GenericValue) i.next();

                byte[] labelImageBytes = shipmentPackageRouteSeg.getBytes("labelImage");

                String outFileName = "UspsLabelImage" + shipmentRouteSegment.getString("shipmentId") + "_" +
                        shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_" +
                        shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".gif";

                FileOutputStream fileOut = new FileOutputStream(outFileName);
                fileOut.write(labelImageBytes);
                fileOut.flush();
                fileOut.close();
            }

        } catch (GenericEntityException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    private static Document createUspsRequestDocument(String rootElement) {

        Document requestDocument = UtilXml.makeEmptyXmlDocument(rootElement);

        Element requestElement = requestDocument.getDocumentElement();
        requestElement.setAttribute("USERID",
                UtilProperties.getPropertyValue("shipment.properties", "shipment.usps.access.userid"));
        requestElement.setAttribute("PASSWORD",
                UtilProperties.getPropertyValue("shipment.properties", "shipment.usps.access.password"));

        return requestDocument;
    }

    private static Document sendUspsRequest(String requestType, Document requestDocument) throws UspsRequestException {
        String conUrl = UtilProperties.getPropertyValue("shipment.properties", "shipment.usps.connect.url");
        if (UtilValidate.isEmpty(conUrl)) {
            throw new UspsRequestException("Connection URL not specified; please check your configuration");
        }

        OutputStream os = new ByteArrayOutputStream();

        OutputFormat format = new OutputFormat(requestDocument);
        format.setOmitDocumentType(true);
        format.setOmitXMLDeclaration(true);
        format.setIndenting(false);

        XMLSerializer serializer = new XMLSerializer(os, format);
        try {
            serializer.asDOMSerializer();
            serializer.serialize(requestDocument.getDocumentElement());
        } catch (IOException e) {
            throw new UspsRequestException("Error serializing requestDocument: " + e.getMessage());
        }

        String xmlString = os.toString();

        Debug.logInfo("USPS XML request string: " + xmlString, module);

        String timeOutStr = UtilProperties.getPropertyValue("shipment.properties", "shipment.usps.connect.timeout", "60");
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
            throw new UspsRequestException("Problem connecting with USPS server", e);
        }

        Debug.logInfo("USPS response: " + responseString, module);

        Document responseDocument = null;
        try {
            responseDocument = UtilXml.readXmlDocument(responseString, false);
        } catch (SAXException se) {
            throw new UspsRequestException("Error reading request Document from a String: " + se.getMessage());
        } catch (ParserConfigurationException pce) {
            throw new UspsRequestException("Error reading request Document from a String: " + pce.getMessage());
        } catch (IOException xmlReadException) {
            throw new UspsRequestException("Error reading request Document from a String: " + xmlReadException.getMessage());
        }

        // If a top-level error document is returned, throw exception
        // Other request-level errors should be handled by the caller
        Element responseElement = responseDocument.getDocumentElement();
        if ("Error".equals(responseElement.getNodeName())) {
            throw new UspsRequestException(UtilXml.childElementValue(responseElement, "Description"));
        }

        return responseDocument;
    }
}

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
