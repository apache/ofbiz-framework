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
package org.ofbiz.shipment.thirdparty.ups;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.math.BigDecimal;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.velocity.test.MiscTestCase;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.*;
import org.ofbiz.product.store.ProductStoreWorker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * UPS ShipmentServices
 */
public class UpsServices {
    
    public final static String module = UpsServices.class.getName();
    
    public static Map unitsUpsToOfbiz = new HashMap();
    public static Map unitsOfbizToUps = new HashMap();
    static {
        unitsUpsToOfbiz.put("LBS", "WT_lb");
        unitsUpsToOfbiz.put("KGS", "WT_kg");
        
        Iterator unitsUpsToOfbizIter = unitsUpsToOfbiz.entrySet().iterator();
        while (unitsUpsToOfbizIter.hasNext()) {
            Map.Entry entry = (Map.Entry) unitsUpsToOfbizIter.next();
            unitsOfbizToUps.put(entry.getValue(), entry.getKey());
        }
    }
    public static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

    public static Map upsShipmentConfirm(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
        String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

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
            
            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not UPS.");
            }
            
            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId")) && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be not-set or [SHRSCS_NOT_STARTED] to perform the UPS Shipment Confirm operation.");
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
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
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

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber");
            if (destTelecomNumber == null) {
                String missingErrMsg = "DestTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, module);
                // for now we won't require the dest phone number, but is it required?
                //return ServiceUtil.returnError(missingErrMsg);
            }
            String destPhoneNumber = null;
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

            Map findCarrierShipmentMethodMap = UtilMisc.toMap("partyId", shipmentRouteSegment.get("carrierPartyId"), "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentRouteSegment.get("shipmentMethodTypeId"));
            GenericValue carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", findCarrierShipmentMethodMap);
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("CarrierShipmentMethod not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId + "; partyId is " + shipmentRouteSegment.get("carrierPartyId") + " and shipmentMethodTypeId is " + shipmentRouteSegment.get("shipmentMethodTypeId"));
            }

            List shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"));
            if (shipmentPackageRouteSegs == null || shipmentPackageRouteSegs.size() == 0) {
                return ServiceUtil.returnError("No ShipmentPackageRouteSegs (ie No Packages) found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            
            List itemIssuances = shipment.getRelated("ItemIssuance");
            Set orderIdSet = new TreeSet();
            Iterator itemIssuanceIter = itemIssuances.iterator();
            while (itemIssuanceIter.hasNext()) {
                GenericValue itemIssuance = (GenericValue) itemIssuanceIter.next();
                orderIdSet.add(itemIssuance.get("orderId"));
            }
            String ordersDescription = "";
            if (orderIdSet.size() > 1) {
                StringBuffer odBuf = new StringBuffer("Orders ");
                Iterator orderIdIter = orderIdSet.iterator();
                while (orderIdIter.hasNext()) {
                    String orderId = (String) orderIdIter.next();
                    odBuf.append(orderId);
                    if (orderIdIter.hasNext()) {
                        odBuf.append(", ");
                    }
                }
                ordersDescription = odBuf.toString();
            } else if (orderIdSet.size() > 0) {
                ordersDescription = "Order " + (String) orderIdSet.iterator().next();
            }
            
            // COD Support
            boolean applyCODSurcharge = "true".equalsIgnoreCase(UtilProperties.getPropertyValue("shipment", "shipment.ups.cod.applyCODSurcharge"));

            // COD only applies if all orders involved with the shipment were paid only with EXT_COD - anything else becomes too complicated
            if (applyCODSurcharge) {

                // Get the paymentMethodTypeIds of all the orderPaymentPreferences involved with the shipment
                List opps = delegator.findByCondition("OrderPaymentPreference", new EntityExpr("orderId", EntityOperator.IN, orderIdSet), null, null);
                List paymentMethodTypeIds = EntityUtil.getFieldListFromEntityList(opps, "paymentMethodTypeId", true);
                
                if (paymentMethodTypeIds.size() > 1 || ! paymentMethodTypeIds.contains("EXT_COD")) {
                    applyCODSurcharge = false;
                }
            }

            String codSurchargeAmount = null;
            String codSurchargeCurrencyUomId = null;
            String codFundsCode = null;
            
            boolean codSurchargeApplyToFirstPackage = false;
            boolean codSurchargeApplyToAllPackages = false;
            boolean codSurchargeSplitBetweenPackages = false;

            BigDecimal codSurchargePackageAmount = null;
            
            if (applyCODSurcharge) {

                codSurchargeAmount = UtilProperties.getPropertyValue("shipment", "shipment.ups.cod.surcharge.amount");
                if (UtilValidate.isEmpty(codSurchargeAmount)) {
                    return ServiceUtil.returnError("shipment.ups.cod.surcharge.amount is not configured in shipment.properties");
                }
                codSurchargeCurrencyUomId = UtilProperties.getPropertyValue("shipment", "shipment.ups.cod.surcharge.currencyUomId");
                if (UtilValidate.isEmpty(codSurchargeCurrencyUomId)) {
                    return ServiceUtil.returnError("shipment.ups.cod.surcharge.currencyUomId is not configured in shipment.properties");
                }
                String codSurchargeApplyToPackages = UtilProperties.getPropertyValue("shipment", "shipment.ups.cod.surcharge.applyToPackages");
                if (UtilValidate.isEmpty(codSurchargeApplyToPackages)) {
                    return ServiceUtil.returnError("shipment.ups.cod.surcharge.applyToPackages is not configured in shipment.properties");
                }
                codFundsCode = UtilProperties.getPropertyValue("shipment", "shipment.ups.cod.codFundsCode");
                if (UtilValidate.isEmpty(codFundsCode)) {
                    return ServiceUtil.returnError("shipment.ups.cod.codFundsCode is not configured in shipment.properties");
                }

                codSurchargeApplyToFirstPackage = "first".equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeApplyToAllPackages = "all".equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeSplitBetweenPackages = "split".equalsIgnoreCase(codSurchargeApplyToPackages);
                
                codSurchargePackageAmount = new BigDecimal(codSurchargeAmount).setScale(decimals, rounding);
                if (codSurchargeSplitBetweenPackages) {
                    codSurchargePackageAmount = codSurchargePackageAmount.divide(new BigDecimal(shipmentPackageRouteSegs.size()), decimals, rounding);
                }

                if (UtilValidate.isEmpty(destTelecomNumber)) {
                    Debug.logInfo("Voice notification service will not be requested for COD shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + " - missing destination phone number", module);
                }
                if (UtilValidate.isEmpty(shipmentRouteSegment.get("homeDeliveryType"))) {
                    Debug.logInfo("Voice notification service will not be requested for COD shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + " - destination address is not residential", module);
                }
            }

            // Okay, start putting the XML together...
            Document shipmentConfirmRequestDoc = UtilXml.makeEmptyXmlDocument("ShipmentConfirmRequest");
            Element shipmentConfirmRequestElement = shipmentConfirmRequestDoc.getDocumentElement();
            shipmentConfirmRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Request", shipmentConfirmRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "Ship Confirm / nonvalidate", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(requestElement, "RequestAction", "ShipConfirm", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestOption", "nonvalidate", shipmentConfirmRequestDoc);

            // Top Level Element: LabelSpecification
            Element labelSpecificationElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "LabelSpecification", shipmentConfirmRequestDoc);
            
            Element labelPrintMethodElement = UtilXml.addChildElement(labelSpecificationElement, "LabelPrintMethod", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelPrintMethodElement, "Code", "GIF", shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(labelSpecificationElement, "HTTPUserAgent", "Mozilla/5.0", shipmentConfirmRequestDoc);

            Element labelImageFormatElement = UtilXml.addChildElement(labelSpecificationElement, "LabelImageFormat", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelImageFormatElement, "Code", "GIF", shipmentConfirmRequestDoc);
            
            // Top Level Element: Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Shipment", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, "Description", "Goods for Shipment " + shipment.get("shipmentId") + " from " + ordersDescription, shipmentConfirmRequestDoc);
            
            // Child of Shipment: Shipper
            Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "Name", originPostalAddress.getString("toName"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "AttentionName", originPostalAddress.getString("attnName"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "PhoneNumber", originPhoneNumber, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "ShipperNumber", UtilProperties.getPropertyValue("shipment", "shipment.ups.shipper.number"), shipmentConfirmRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "AddressLine1", originPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipperAddressElement, "AddressLine2", originPostalAddress.getString("address2"), shipmentConfirmRequestDoc);
            }
            //UtilXml.addChildElementValue(shipperAddressElement, "AddressLine3", "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            // How to determine this? Add to data model...? UtilXml.addChildElement(shipperAddressElement, "ResidentialAddress", shipmentConfirmRequestDoc);


            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, "ShipTo", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "CompanyName", destPostalAddress.getString("toName"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "AttentionName", destPostalAddress.getString("attnName"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, "PhoneNumber", destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "AddressLine1", destPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipToAddressElement, "AddressLine2", destPostalAddress.getString("address2"), shipmentConfirmRequestDoc);
            }
            //UtilXml.addChildElementValue(shipToAddressElement, "AddressLine3", "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "City", destPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "StateProvinceCode", destPostalAddress.getString("stateProvinceGeoId"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "PostalCode", destPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "CountryCode", destCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("homeDeliveryType"))) {
                UtilXml.addChildElement(shipToAddressElement, "ResidentialAddress", shipmentConfirmRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, "ShipFrom", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "CompanyName", originPostalAddress.getString("toName"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "AttentionName", originPostalAddress.getString("attnName"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "PhoneNumber", originPhoneNumber, shipmentConfirmRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine1", originPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine2", originPostalAddress.getString("address2"), shipmentConfirmRequestDoc);
            }
            //UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine3", "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, "PaymentInformation", shipmentConfirmRequestDoc);
            
            String thirdPartyAccountNumber = shipmentRouteSegment.getString("thirdPartyAccountNumber");

            if (UtilValidate.isEmpty(thirdPartyAccountNumber)) {
                
                // Paid by shipper
                Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, "Prepaid", shipmentConfirmRequestDoc);
                Element billShipperElement = UtilXml.addChildElement(prepaidElement, "BillShipper", shipmentConfirmRequestDoc);

                // fill in BillShipper AccountNumber element from properties file
                UtilXml.addChildElementValue(billShipperElement, "AccountNumber", UtilProperties.getPropertyValue("shipment", "shipment.ups.bill.shipper.account.number"), shipmentConfirmRequestDoc);
            } else {

                // Paid by another shipper (may be receiver or not)
                GenericValue thirdPartyPostalAddress = shipmentRouteSegment.getRelatedOne("ThirdPartyPostalAddress");
    
                // UPS requires the postal code and country code of the third party
                if (UtilValidate.isEmpty(thirdPartyPostalAddress)) {
                    return ServiceUtil.returnError("ThirdPartyPostalAddress not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                }
                GenericValue thirdPartyCountryGeo = thirdPartyPostalAddress.getRelatedOne("CountryGeo");
                if (UtilValidate.isEmpty(thirdPartyCountryGeo)) {
                    return ServiceUtil.returnError("ThirdPartyCountryGeo not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
                }

                Element billThirdPartyElement = UtilXml.addChildElement(paymentInformationElement, "BillThirdParty", shipmentConfirmRequestDoc);
                Element billThirdPartyShipperElement = UtilXml.addChildElement(billThirdPartyElement, "BillThirdPartyShipper", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(billThirdPartyShipperElement, "AccountNumber", thirdPartyAccountNumber, shipmentConfirmRequestDoc);
                Element thirdPartyElement = UtilXml.addChildElement(billThirdPartyShipperElement, "ThirdParty", shipmentConfirmRequestDoc);
                Element addressElement = UtilXml.addChildElement(thirdPartyElement, "Address", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "PostalCode", thirdPartyPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "CountryCode", thirdPartyCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(serviceElement, "Code", carrierShipmentMethod.getString("carrierServiceCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: Package
            ListIterator shipmentPackageRouteSegIter = shipmentPackageRouteSegs.listIterator();
            while (shipmentPackageRouteSegIter.hasNext()) {
                GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegIter.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage");
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType");
                List carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "UPS"), null);
                GenericValue carrierShipmentBoxType = null;
                if (carrierShipmentBoxTypes.size() > 0) {
                    carrierShipmentBoxType = (GenericValue) carrierShipmentBoxTypes.get(0); 
                }
                 
                Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", shipmentConfirmRequestDoc);
                Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", shipmentConfirmRequestDoc);
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get("packagingTypeCode") != null) {
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", carrierShipmentBoxType.getString("packagingTypeCode"), shipmentConfirmRequestDoc);
                } else {
                    // default to "02", plain old Package
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", "02", shipmentConfirmRequestDoc);
                }
                if (shipmentBoxType != null) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", shipmentConfirmRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne("DimensionUom");
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", dimensionUom.getString("abbreviation"), shipmentConfirmRequestDoc);
                    } else {
                        // I guess we'll default to inches...
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", "IN", shipmentConfirmRequestDoc);
                    }
                    UtilXml.addChildElementValue(dimensionsElement, "Length", shipmentBoxType.get("boxLength").toString(), shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Width", shipmentBoxType.get("boxWidth").toString(), shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Height", shipmentBoxType.get("boxHeight").toString(), shipmentConfirmRequestDoc);
                }
                
                Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", shipmentConfirmRequestDoc);
                Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
                String weightUomUps = (String) unitsOfbizToUps.get(shipmentPackage.get("weightUomId"));
                if (weightUomUps != null) {
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", weightUomUps, shipmentConfirmRequestDoc);
                } else {
                    // might as well default to LBS
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", "LBS", shipmentConfirmRequestDoc);
                }
                
                if (shipmentPackage.getString("weight") == null) {
                    return ServiceUtil.returnError("Weight value not found for ShipmentRouteSegment with shipmentId " + shipmentId + ", shipmentRouteSegmentId " + shipmentRouteSegmentId + ", and shipmentPackageSeqId " + shipmentPackage.getString("shipmentPackageSeqId"));
                }
                UtilXml.addChildElementValue(packageWeightElement, "Weight", shipmentPackage.getString("weight"), shipmentConfirmRequestDoc);
                
                Element referenceNumberElement = UtilXml.addChildElement(packageElement, "ReferenceNumber", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(referenceNumberElement, "Code", "MK", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(referenceNumberElement, "Value", shipmentPackage.getString("shipmentPackageSeqId"), shipmentConfirmRequestDoc);

                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get("oversizeCode") != null) {
                    UtilXml.addChildElementValue(packageElement, "OversizePackage", carrierShipmentBoxType.getString("oversizeCode"), shipmentConfirmRequestDoc);
                }
                
                Element packageServiceOptionsElement = UtilXml.addChildElement(packageElement, "PackageServiceOptions", shipmentConfirmRequestDoc);

                // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency, and finally default to USD
                String currencyCode = null;
                if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    currencyCode = shipmentRouteSegment.getString("currencyUomId");
                } else if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    currencyCode = shipment.getString("currencyUomId");
                } else {
                    currencyCode = UtilProperties.getPropertyValue("general.properties", "currency.uom.id.default", "USD");
                }

                // Package insured value
                BigDecimal insuredValue = shipmentPackage.getBigDecimal("insuredValue");
                if (! UtilValidate.isEmpty(insuredValue)) {

                    Element insuredValueElement = UtilXml.addChildElement(packageServiceOptionsElement, "InsuredValue", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, "MonetaryValue", insuredValue.setScale(2, BigDecimal.ROUND_HALF_UP).toString(), shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, "CurrencyCode", currencyCode, shipmentConfirmRequestDoc);
                }

                if (applyCODSurcharge) {
                    Element codElement = UtilXml.addChildElement(packageServiceOptionsElement, "COD", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codElement, "CODCode", "3", shipmentConfirmRequestDoc); // "3" is the only valid value for package-level COD
                    UtilXml.addChildElementValue(codElement, "CODFundsCode", codFundsCode, shipmentConfirmRequestDoc);
                    Element codAmountElement = UtilXml.addChildElement(codElement, "CODAmount", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codAmountElement, "CurrencyCode", currencyCode, shipmentConfirmRequestDoc);

                    // Get the value of the package by going back to the orderItems
                    Map getPackageValueResult = dispatcher.runSync("getShipmentPackageValueFromOrders", UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackage.get("shipmentPackageSeqId"), "currencyUomId", currencyCode, "userLogin", userLogin, "locale", locale));
                    if (ServiceUtil.isError(getPackageValueResult)) return getPackageValueResult;
                    BigDecimal packageValue = (BigDecimal) getPackageValueResult.get("packageValue");
                    
                    // Convert the value of the COD surcharge to the shipment currency, if necessary
                    Map convertUomResult = dispatcher.runSync("convertUom", UtilMisc.toMap("uomId", codSurchargeCurrencyUomId, "uomIdTo", currencyCode, "originalValue", new Double(codSurchargePackageAmount.doubleValue())));
                    if (ServiceUtil.isError(convertUomResult)) return convertUomResult;
                    if (convertUomResult.containsKey("convertedValue")) {
                        codSurchargePackageAmount = new BigDecimal(((Double) convertUomResult.get("convertedValue")).doubleValue()).setScale(decimals, rounding);
                    }
                    
                    // Add the amount of the surcharge for the package, if the surcharge should be on all packages or the first and this is the first package
                    if (codSurchargeApplyToAllPackages || codSurchargeSplitBetweenPackages || (codSurchargeApplyToFirstPackage && shipmentPackageRouteSegIter.previousIndex() <= 0)) {
                        packageValue = packageValue.add(codSurchargePackageAmount);
                    }

                    UtilXml.addChildElementValue(codAmountElement, "MonetaryValue", packageValue.setScale(decimals, rounding).toString(), shipmentConfirmRequestDoc);
                }
            }

            String shipmentConfirmRequestString = null;
            try {
                shipmentConfirmRequestString = UtilXml.writeXmlDocument(shipmentConfirmRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentConfirmRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument();
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuffer xmlString = new StringBuffer();
            // TODO: note that we may have to append <?xml version="1.0"?> before each string
            xmlString.append(accessRequestString);
            xmlString.append(shipmentConfirmRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmRequest" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(xmlString.toString().getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            try {
                shipmentConfirmResponseString = sendUpsRequest("ShipConfirm", xmlString.toString());
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipConfirm: " + e.toString();
                Debug.logError(e, uceErrMsg, module);
                return ServiceUtil.returnError(uceErrMsg);
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmResponse" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml"; 
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(shipmentConfirmResponseString.getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            Document shipmentConfirmResponseDocument = null;
            try {
                shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponseString, false);
            } catch (SAXException e2) {
                String excErrMsg = "Error parsing the ShipmentConfirmResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the ShipmentConfirmResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (IOException e2) {
                String excErrMsg = "Error parsing the ShipmentConfirmResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            }

            return handleUpsShipmentConfirmResponse(shipmentConfirmResponseDocument, shipmentRouteSegment);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error reading or writing Shipment data for UPS Shipment Confirm: " + e.toString());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            if (shipmentConfirmResponseString != null) {
                Debug.logError("Got XML ShipmentConfirmRespose: " + shipmentConfirmResponseString, module);
                return ServiceUtil.returnError(UtilMisc.toList(
                            "Error reading or writing Shipment data for UPS Shipment Confirm: " + e.toString(),
                            "A ShipmentConfirmRespose was received: " + shipmentConfirmResponseString));
            } else {
                return ServiceUtil.returnError("Error reading or writing Shipment data for UPS Shipment Confirm: " + e.toString());
            }
        }
    }

    public static Map handleUpsShipmentConfirmResponse(Document shipmentConfirmResponseDocument, GenericValue shipmentRouteSegment) throws GenericEntityException {
        // process ShipmentConfirmResponse, update data as needed
        Element shipmentConfirmResponseElement = shipmentConfirmResponseDocument.getDocumentElement();
            
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "Response");
        Element responseTransactionReferenceElement = UtilXml.firstChildElement(responseElement, "TransactionReference");
        String responseTransactionReferenceCustomerContext = UtilXml.childElementValue(responseTransactionReferenceElement, "CustomerContext");
        String responseTransactionReferenceXpciVersion = UtilXml.childElementValue(responseTransactionReferenceElement, "XpciVersion");

        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        String responseStatusDescription = UtilXml.childElementValue(responseElement, "ResponseStatusDescription");
        List errorList = new LinkedList();
        UpsServices.handleErrors(responseElement, errorList);

        if ("1".equals(responseStatusCode)) {
            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "ShipmentCharges");

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TransportationCharges");
            String transportationCurrencyCode = UtilXml.childElementValue(transportationChargesElement, "CurrencyCode");
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, "MonetaryValue");
            
            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "ServiceOptionsCharges");
            String serviceOptionsCurrencyCode = UtilXml.childElementValue(serviceOptionsChargesElement, "CurrencyCode");
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, "MonetaryValue");

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TotalCharges");
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, "CurrencyCode");
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, "MonetaryValue");
            
            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                } else if(!totalCurrencyCode.equals(shipmentRouteSegment.getString("currencyUomId"))) {
                    errorList.add("The Currency Unit of Measure returned [" + totalCurrencyCode + "] is not the same as the original [" + shipmentRouteSegment.getString("currencyUomId") + "], setting to the new one.");
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                }
            }
            
            try {
                shipmentRouteSegment.set("actualTransportCost", Double.valueOf(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the transportationMonetaryValue [" + transportationMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            try {
                shipmentRouteSegment.set("actualServiceCost", Double.valueOf(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the serviceOptionsMonetaryValue [" + serviceOptionsMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            try {
                shipmentRouteSegment.set("actualCost", Double.valueOf(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the totalMonetaryValue [" + totalMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            
            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "BillingWeight");
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, "UnitOfMeasurement");
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, "Code");
            String billingWeight = UtilXml.childElementValue(billingWeightElement, "Weight");
            try {
                shipmentRouteSegment.set("billingWeight", Double.valueOf(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the billingWeight [" + billingWeight + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            shipmentRouteSegment.set("billingWeightUomId", unitsUpsToOfbiz.get(billingWeightUnitOfMeasurement));

            // store the ShipmentIdentificationNumber and ShipmentDigest
            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentConfirmResponseElement, "ShipmentIdentificationNumber");
            String shipmentDigest = UtilXml.childElementValue(shipmentConfirmResponseElement, "ShipmentDigest");
            shipmentRouteSegment.set("trackingIdNumber", shipmentIdentificationNumber);
            shipmentRouteSegment.set("trackingDigest", shipmentDigest);
                
            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable 
            shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_CONFIRMED");
            
            // write/store all modified value objects
            shipmentRouteSegment.store();
                
            // -=-=-=- Okay, now done with that, just return any extra info...
            
            StringBuffer successString = new StringBuffer("The UPS ShipmentConfirm succeeded");
            if (errorList.size() > 0) {
                // this shouldn't happen much, but handle it anyway
                successString.append(", but the following occurred: ");
                Iterator errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    String errorMsg = (String) errorListIter.next();
                    successString.append(errorMsg);
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, "The UPS ShipmentConfirm failed");
            return ServiceUtil.returnError(errorList);
        }
    }
    
    public static Map upsShipmentAccept(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        
        boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
        String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentAcceptResponseString = null;

        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not UPS.");
            }
            
            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_CONFIRMED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be [SHRSCS_CONFIRMED] to perform the UPS Shipment Accept operation.");
            }
            
            List shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"));
            if (shipmentPackageRouteSegs == null || shipmentPackageRouteSegs.size() == 0) {
                return ServiceUtil.returnError("No ShipmentPackageRouteSegs found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            
            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingDigest"))) {
                return ServiceUtil.returnError("ERROR: The trackingDigest was not set for this Route Segment, meaning that a UPS shipment confirm has not been done.");
            }

            Document shipmentAcceptRequestDoc = UtilXml.makeEmptyXmlDocument("ShipmentAcceptRequest");
            Element shipmentAcceptRequestElement = shipmentAcceptRequestDoc.getDocumentElement();
            shipmentAcceptRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentAcceptRequestElement, "Request", shipmentAcceptRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "ShipAccept / 01", shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(requestElement, "RequestAction", "ShipAccept", shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestOption", "01", shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(shipmentAcceptRequestElement, "ShipmentDigest", shipmentRouteSegment.getString("trackingDigest"), shipmentAcceptRequestDoc);
            
            
            String shipmentAcceptRequestString = null;
            try {
                shipmentAcceptRequestString = UtilXml.writeXmlDocument(shipmentAcceptRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentAcceptRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument();
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuffer xmlString = new StringBuffer();
            // TODO: note that we may have to append <?xml version="1.0"?> before each string
            xmlString.append(accessRequestString);
            xmlString.append(shipmentAcceptRequestString);
            
            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentAcceptRequest" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(xmlString.toString().getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            try {
                shipmentAcceptResponseString = sendUpsRequest("ShipAccept", xmlString.toString());
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipAccept: " + e.toString();
                Debug.logError(e, uceErrMsg, module);
                return ServiceUtil.returnError(uceErrMsg);
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentAcceptResponse" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(shipmentAcceptResponseString.getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            Document shipmentAcceptResponseDocument = null;
            try {
                shipmentAcceptResponseDocument = UtilXml.readXmlDocument(shipmentAcceptResponseString, false);
            } catch (SAXException e2) {
                String excErrMsg = "Error parsing the ShipmentAcceptResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the ShipmentAcceptResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (IOException e2) {
                String excErrMsg = "Error parsing the ShipmentAcceptResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            }

            return handleUpsShipmentAcceptResponse(shipmentAcceptResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error reading or writing Shipment data for UPS Shipment Accept: " + e.toString());
        }
    }
    
    public static Map handleUpsShipmentAcceptResponse(Document shipmentAcceptResponseDocument, GenericValue shipmentRouteSegment, List shipmentPackageRouteSegs) throws GenericEntityException {
        boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
        String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        // process ShipmentAcceptResponse, update data as needed
        Element shipmentAcceptResponseElement = shipmentAcceptResponseDocument.getDocumentElement();
            
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(shipmentAcceptResponseElement, "Response");
        Element responseTransactionReferenceElement = UtilXml.firstChildElement(responseElement, "TransactionReference");
        String responseTransactionReferenceCustomerContext = UtilXml.childElementValue(responseTransactionReferenceElement, "CustomerContext");
        String responseTransactionReferenceXpciVersion = UtilXml.childElementValue(responseTransactionReferenceElement, "XpciVersion");

        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        String responseStatusDescription = UtilXml.childElementValue(responseElement, "ResponseStatusDescription");
        List errorList = new LinkedList();
        UpsServices.handleErrors(responseElement, errorList);

        if ("1".equals(responseStatusCode)) {
            Element shipmentResultsElement = UtilXml.firstChildElement(shipmentAcceptResponseElement, "ShipmentResults");

            // This information is returned in both the ShipmentConfirmResponse and 
            //the ShipmentAcceptResponse. So, we'll go ahead and store it here again
            //and warn of changes or something...
                

            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentResultsElement, "ShipmentCharges");

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TransportationCharges");
            String transportationCurrencyCode = UtilXml.childElementValue(transportationChargesElement, "CurrencyCode");
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, "MonetaryValue");
            
            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "ServiceOptionsCharges");
            String serviceOptionsCurrencyCode = UtilXml.childElementValue(serviceOptionsChargesElement, "CurrencyCode");
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, "MonetaryValue");

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TotalCharges");
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, "CurrencyCode");
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, "MonetaryValue");
            
            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                } else if(!totalCurrencyCode.equals(shipmentRouteSegment.getString("currencyUomId"))) {
                    errorList.add("The Currency Unit of Measure returned [" + totalCurrencyCode + "] is not the same as the original [" + shipmentRouteSegment.getString("currencyUomId") + "], setting to the new one.");
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                }
            }
            
            try {
                shipmentRouteSegment.set("actualTransportCost", Double.valueOf(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the transportationMonetaryValue [" + transportationMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            try {
                shipmentRouteSegment.set("actualServiceCost", Double.valueOf(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the serviceOptionsMonetaryValue [" + serviceOptionsMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            try {
                shipmentRouteSegment.set("actualCost", Double.valueOf(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the totalMonetaryValue [" + totalMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            
            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentResultsElement, "BillingWeight");
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, "UnitOfMeasurement");
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, "Code");
            String billingWeight = UtilXml.childElementValue(billingWeightElement, "Weight");
            try {
                shipmentRouteSegment.set("billingWeight", Double.valueOf(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the billingWeight [" + billingWeight + "]: " + e.toString();
                Debug.logError(e, excErrMsg, module);
                errorList.add(excErrMsg);
            }
            shipmentRouteSegment.set("billingWeightUomId", unitsUpsToOfbiz.get(billingWeightUnitOfMeasurement));

            // store the ShipmentIdentificationNumber and ShipmentDigest
            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentResultsElement, "ShipmentIdentificationNumber");
            // should compare to trackingIdNumber, should always be the same right?
            shipmentRouteSegment.set("trackingIdNumber", shipmentIdentificationNumber);

            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable 
            shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_ACCEPTED");
            
            // write/store modified value object
            shipmentRouteSegment.store();
                
            // now process the PackageResults elements
            List packageResultsElements = UtilXml.childElementList(shipmentResultsElement, "PackageResults");
            Iterator packageResultsElementIter = packageResultsElements.iterator();
            Iterator shipmentPackageRouteSegIter = shipmentPackageRouteSegs.iterator();
            while (packageResultsElementIter.hasNext()) {
                Element packageResultsElement = (Element) packageResultsElementIter.next();
                    
                String trackingNumber = UtilXml.childElementValue(packageResultsElement, "TrackingNumber");

                Element packageServiceOptionsChargesElement = UtilXml.firstChildElement(packageResultsElement, "ServiceOptionsCharges");
                String packageServiceOptionsCurrencyCode = UtilXml.childElementValue(packageServiceOptionsChargesElement, "CurrencyCode");
                String packageServiceOptionsMonetaryValue = UtilXml.childElementValue(packageServiceOptionsChargesElement, "MonetaryValue");

                Element packageLabelImageElement = UtilXml.firstChildElement(packageResultsElement, "LabelImage");
                Element packageLabelImageFormatElement = UtilXml.firstChildElement(packageResultsElement, "LabelImageFormat");
                // will be EPL or GIF, should always be GIF since that is what we requested
                String packageLabelImageFormatCode = UtilXml.childElementValue(packageLabelImageFormatElement, "Code");
                String packageLabelImageFormatDescription = UtilXml.childElementValue(packageLabelImageFormatElement, "Description");
                String packageLabelGraphicImageString = UtilXml.childElementValue(packageLabelImageElement, "GraphicImage");
                String packageLabelInternationalSignatureGraphicImageString = UtilXml.childElementValue(packageLabelImageElement, "InternationalSignatureGraphicImage");
                String packageLabelHTMLImageString = UtilXml.childElementValue(packageLabelImageElement, "HTMLImage");

                if (!shipmentPackageRouteSegIter.hasNext()) {
                    errorList.add("Error: More PackageResults were returned than there are Packages on this Shipment; the TrackingNumber is [" + trackingNumber + "], the ServiceOptionsCharges were " + packageServiceOptionsMonetaryValue + packageServiceOptionsCurrencyCode);
                    // NOTE: if this happens much we should just create a new package to store all of the info...
                    continue;
                }
                    
                //NOTE: I guess they come back in the same order we sent them, so we'll get the packages in order and off we go...
                GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegIter.next();
                shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
                shipmentPackageRouteSeg.set("boxNumber", "");
                shipmentPackageRouteSeg.set("currencyUomId", packageServiceOptionsCurrencyCode);
                try {
                    shipmentPackageRouteSeg.set("packageServiceCost", Double.valueOf(packageServiceOptionsMonetaryValue));
                } catch (NumberFormatException e) {
                    String excErrMsg = "Error parsing the packageServiceOptionsMonetaryValue [" + packageServiceOptionsMonetaryValue + "] for Package [" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + "]: " + e.toString();
                    Debug.logError(e, excErrMsg, module);
                    errorList.add(excErrMsg);
                }
                    
                byte[] labelImageBytes = null;
                if (packageLabelGraphicImageString != null) {
                    labelImageBytes = Base64.base64Decode(packageLabelGraphicImageString.getBytes());
                    shipmentPackageRouteSeg.setBytes("labelImage", labelImageBytes);
                }
                byte[] labelInternationalSignatureGraphicImageBytes = null;
                if (packageLabelInternationalSignatureGraphicImageString != null) {
                    labelInternationalSignatureGraphicImageBytes = Base64.base64Decode(packageLabelInternationalSignatureGraphicImageString.getBytes());
                    shipmentPackageRouteSeg.set("labelIntlSignImage", labelInternationalSignatureGraphicImageBytes);
                }
                String packageLabelHTMLImageStringDecoded = Base64.base64Decode(packageLabelHTMLImageString);
                shipmentPackageRouteSeg.set("labelHtml", packageLabelHTMLImageStringDecoded);

                if (shipmentUpsSaveCertificationInfo) {
                    if (labelImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentLabelImage" + shipmentRouteSegment.getString("shipmentId") + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".gif";
                        try {
                            FileOutputStream fileOut = new FileOutputStream(outFileName);
                            fileOut.write(labelImageBytes);
                            fileOut.flush();
                            fileOut.close();
                        } catch (IOException e) {
                            Debug.log(e, "Could not save UPS LabelImage GIF file: [[[" + packageLabelGraphicImageString + "]]] to file: " + outFileName, module);
                        }
                    }
                    if (labelInternationalSignatureGraphicImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentLabelIntlSignImage" + shipmentRouteSegment.getString("shipmentId") + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".gif";
                        try {
                            FileOutputStream fileOut = new FileOutputStream(outFileName);
                            fileOut.write(labelInternationalSignatureGraphicImageBytes);
                            fileOut.flush();
                            fileOut.close();
                        } catch (IOException e) {
                            Debug.log(e, "Could not save UPS IntlSign LabelImage GIF file: [[[" + packageLabelInternationalSignatureGraphicImageString + "]]] to file: " + outFileName, module);
                        }
                    }
                    if (packageLabelHTMLImageStringDecoded != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentLabelHTMLImage" + shipmentRouteSegment.getString("shipmentId") + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".html";
                        try {
                            FileOutputStream fileOut = new FileOutputStream(outFileName);
                            fileOut.write(packageLabelHTMLImageStringDecoded.getBytes());
                            fileOut.flush();
                            fileOut.close();
                        } catch (IOException e) {
                            Debug.log(e, "Could not save UPS LabelImage HTML file: [[[" + packageLabelHTMLImageStringDecoded + "]]] to file: " + outFileName, module);
                        }
                    }
                }
            
                shipmentPackageRouteSeg.store();
            }

            if (shipmentPackageRouteSegIter.hasNext()) {
                errorList.add("Error: There are more Packages on this Shipment than there were PackageResults returned from UPS");
                while (shipmentPackageRouteSegIter.hasNext()) {
                    GenericValue shipmentPackageRouteSeg = (GenericValue) shipmentPackageRouteSegIter.next();
                    errorList.add("Error: No PackageResults were returned for the Package [" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + "]");
                }
            }
                
            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuffer successString = new StringBuffer("The UPS ShipmentAccept succeeded");
            if (errorList.size() > 0) {
                // this shouldn't happen much, but handle it anyway
                successString.append(", but the following occurred: ");
                Iterator errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    String errorMsg = (String) errorListIter.next();
                    successString.append(errorMsg);
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, "The UPS ShipmentConfirm failed");
            return ServiceUtil.returnError(errorList);
        }
    }
    
    public static Map upsVoidShipment(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
        String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String voidShipmentResponseString = null;

        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not UPS.");
            }
            
            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_CONFIRMED".equals(shipmentRouteSegment.getString("carrierServiceStatusId")) &&
                    !"SHRSCS_ACCEPTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be [SHRSCS_CONFIRMED] or [SHRSCS_ACCEPTED] to perform the UPS Void Shipment operation.");
            }
            
            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingIdNumber"))) {
                return ServiceUtil.returnError("ERROR: The trackingIdNumber was not set for this Route Segment, meaning that a UPS shipment confirm has not been done.");
            }

            Document voidShipmentRequestDoc = UtilXml.makeEmptyXmlDocument("VoidShipmentRequest");
            Element voidShipmentRequestElement = voidShipmentRequestDoc.getDocumentElement();
            voidShipmentRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(voidShipmentRequestElement, "Request", voidShipmentRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", voidShipmentRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "Void / 1", voidShipmentRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", voidShipmentRequestDoc);

            UtilXml.addChildElementValue(requestElement, "RequestAction", "Void", voidShipmentRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestOption", "1", voidShipmentRequestDoc);

            UtilXml.addChildElementValue(voidShipmentRequestElement, "ShipmentIdentificationNumber", shipmentRouteSegment.getString("trackingIdNumber"), voidShipmentRequestDoc);

            String voidShipmentRequestString = null;
            try {
                voidShipmentRequestString = UtilXml.writeXmlDocument(voidShipmentRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the VoidShipmentRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument();
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuffer xmlString = new StringBuffer();
            // TODO: note that we may have to append <?xml version="1.0"?> before each string
            xmlString.append(accessRequestString);
            xmlString.append(voidShipmentRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsVoidShipmentRequest" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(xmlString.toString().getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            try {
                voidShipmentResponseString = sendUpsRequest("Void", xmlString.toString());
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service Void: " + e.toString();
                Debug.logError(e, uceErrMsg, module);
                return ServiceUtil.returnError(uceErrMsg);
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsVoidShipmentResponse" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(voidShipmentResponseString.getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            Document voidShipmentResponseDocument = null;
            try {
                voidShipmentResponseDocument = UtilXml.readXmlDocument(voidShipmentResponseString, false);
            } catch (SAXException e2) {
                String excErrMsg = "Error parsing the VoidShipmentResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the VoidShipmentResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (IOException e2) {
                String excErrMsg = "Error parsing the VoidShipmentResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            }

            return handleUpsVoidShipmentResponse(voidShipmentResponseDocument, shipmentRouteSegment);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error reading or writing Shipment data for UPS Void Shipment: " + e.toString());
        }
    }

    public static Map handleUpsVoidShipmentResponse(Document voidShipmentResponseDocument, GenericValue shipmentRouteSegment) throws GenericEntityException {
        // process VoidShipmentResponse, update data as needed
        Element voidShipmentResponseElement = voidShipmentResponseDocument.getDocumentElement();
            
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(voidShipmentResponseElement, "Response");
        Element responseTransactionReferenceElement = UtilXml.firstChildElement(responseElement, "TransactionReference");
        String responseTransactionReferenceCustomerContext = UtilXml.childElementValue(responseTransactionReferenceElement, "CustomerContext");
        String responseTransactionReferenceXpciVersion = UtilXml.childElementValue(responseTransactionReferenceElement, "XpciVersion");

        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        String responseStatusDescription = UtilXml.childElementValue(responseElement, "ResponseStatusDescription");
        List errorList = new LinkedList();
        UpsServices.handleErrors(responseElement, errorList);

        // handle other response elements
        Element statusElement = UtilXml.firstChildElement(voidShipmentResponseElement, "Status");

        Element statusTypeElement = UtilXml.firstChildElement(statusElement, "StatusType");
        String statusTypeCode = UtilXml.childElementValue(statusTypeElement, "Code");
        String statusTypeDescription = UtilXml.childElementValue(statusTypeElement, "Description");

        Element statusCodeElement = UtilXml.firstChildElement(statusElement, "StatusCode");
        String statusCodeCode = UtilXml.childElementValue(statusCodeElement, "Code");
        String statusCodeDescription = UtilXml.childElementValue(statusCodeElement, "Description");

        if ("1".equals(responseStatusCode)) {
            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable 
            shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_VOIDED");
            shipmentRouteSegment.store();
            
            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuffer successString = new StringBuffer("The UPS VoidShipment succeeded; the StatusType is: [" + statusTypeCode + ":" + statusTypeDescription + "], the StatusCode is: [" + statusCodeCode + ":" + statusCodeDescription + "]");
            if (errorList.size() > 0) {
                // this shouldn't happen much, but handle it anyway
                successString.append(", but the following occurred: ");
                Iterator errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    String errorMsg = (String) errorListIter.next();
                    successString.append(errorMsg);
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, "The UPS ShipmentConfirm failed; the StatusType is: [" + statusTypeCode + ":" + statusTypeDescription + "], the StatusCode is: [" + statusCodeCode + ":" + statusCodeDescription + "]");
            return ServiceUtil.returnError(errorList);
        }
    }
    
    public static Map upsTrackShipment(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        boolean shipmentUpsSaveCertificationInfo = "true".equals(UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.info"));
        String shipmentUpsSaveCertificationPath = UtilProperties.getPropertyValue("shipment", "shipment.ups.save.certification.path");
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String trackResponseString = null;

        try {
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is not UPS.");
            }
            
            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_ACCEPTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError("ERROR: The Carrier Service Status for ShipmentRouteSegment " + shipmentRouteSegmentId + " of Shipment " + shipmentId + ", is [" + shipmentRouteSegment.getString("carrierServiceStatusId") + "], but must be [SHRSCS_ACCEPTED] to perform the UPS Track Shipment operation.");
            }
            
            List shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList("+shipmentPackageSeqId"));
            if (shipmentPackageRouteSegs == null || shipmentPackageRouteSegs.size() == 0) {
                return ServiceUtil.returnError("No ShipmentPackageRouteSegs found for ShipmentRouteSegment with shipmentId " + shipmentId + " and shipmentRouteSegmentId " + shipmentRouteSegmentId);
            }
            
            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingIdNumber"))) {
                return ServiceUtil.returnError("ERROR: The trackingIdNumber was not set for this Route Segment, meaning that a UPS shipment confirm has not been done.");
            }

            Document trackRequestDoc = UtilXml.makeEmptyXmlDocument("TrackRequest");
            Element trackRequestElement = trackRequestDoc.getDocumentElement();
            trackRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(trackRequestElement, "Request", trackRequestDoc);

            Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", trackRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "Track", trackRequestDoc);
            UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", trackRequestDoc);

            UtilXml.addChildElementValue(requestElement, "RequestAction", "Track", trackRequestDoc);

            UtilXml.addChildElementValue(trackRequestElement, "ShipmentIdentificationNumber", shipmentRouteSegment.getString("trackingIdNumber"), trackRequestDoc);

            String trackRequestString = null;
            try {
                trackRequestString = UtilXml.writeXmlDocument(trackRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the TrackRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument();
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, module);
                return ServiceUtil.returnError(ioeErrMsg);
            }
            
            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuffer xmlString = new StringBuffer();
            // TODO: note that we may have to append <?xml version="1.0"?> before each string
            xmlString.append(accessRequestString);
            xmlString.append(trackRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsTrackRequest" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(xmlString.toString().getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            try {
                trackResponseString = sendUpsRequest("Track", xmlString.toString());
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service Track: " + e.toString();
                Debug.logError(e, uceErrMsg, module);
                return ServiceUtil.returnError(uceErrMsg);
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsTrackResponseString" + shipmentId + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".xml";
                try {
                    FileOutputStream fileOut = new FileOutputStream(outFileName);
                    fileOut.write(trackResponseString.getBytes());
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    Debug.log(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, module);
                }
            }
            
            Document trackResponseDocument = null;
            try {
                trackResponseDocument = UtilXml.readXmlDocument(trackResponseString, false);
            } catch (SAXException e2) {
                String excErrMsg = "Error parsing the TrackResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the TrackResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            } catch (IOException e2) {
                String excErrMsg = "Error parsing the TrackResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, module);
                return ServiceUtil.returnError(excErrMsg);
            }

            return handleUpsTrackShipmentResponse(trackResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error reading or writing Shipment data for UPS Track Shipment: " + e.toString());
        }
    }

    public static Map handleUpsTrackShipmentResponse(Document trackResponseDocument, GenericValue shipmentRouteSegment, List shipmentPackageRouteSegs) throws GenericEntityException {
        // process TrackResponse, update data as needed
        Element trackResponseElement = trackResponseDocument.getDocumentElement();
            
        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(trackResponseElement, "Response");
        Element responseTransactionReferenceElement = UtilXml.firstChildElement(responseElement, "TransactionReference");
        String responseTransactionReferenceCustomerContext = UtilXml.childElementValue(responseTransactionReferenceElement, "CustomerContext");
        String responseTransactionReferenceXpciVersion = UtilXml.childElementValue(responseTransactionReferenceElement, "XpciVersion");

        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        String responseStatusDescription = UtilXml.childElementValue(responseElement, "ResponseStatusDescription");
        List errorList = new LinkedList();
        UpsServices.handleErrors(responseElement, errorList);

        if ("1".equals(responseStatusCode)) {
            // TODO: handle other response elements
            Element shipmentElement = UtilXml.firstChildElement(trackResponseElement, "Shipment");

            Element shipperElement = UtilXml.firstChildElement(shipmentElement, "Shipper");
            String shipperNumber = UtilXml.childElementValue(shipperElement, "ShipperNumber");

            Element serviceElement = UtilXml.firstChildElement(shipmentElement, "Service");
            String serviceCode = UtilXml.childElementValue(serviceElement, "Code");
            String serviceDescription = UtilXml.childElementValue(serviceElement, "Description");

            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentElement, "ShipmentIdentificationNumber");
                
            List packageElements = UtilXml.childElementList(shipmentElement, "Package");
            Iterator packageElementIter = packageElements.iterator();
            while (packageElementIter.hasNext()) {
                Element packageElement = (Element) packageElementIter.next();
            }
/*
        <Package>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <Activity>
                <ActivityLocation>
                    <Address>
                        <City>CLAKVILLE</City>
                        <StateProvinceCode>AK</StateProvinceCode>
                        <PostalCode>99901</PostalCode>
                        <CountryCode>US</CountryCode>
                    </Address>
                    <Code>MG</Code>
                    <Description>MC MAN</Description>
                </ActivityLocation>
                <Status>
                    <StatusType>
                        <Code>D</Code>
                        <Description>DELIVERED</Description>
                    </StatusType>
                    <StatusCode>
                        <Code>FS</Code>
                    </StatusCode>
                </Status>
                <Date>20020930</Date>
                <Time>130900</Time>
            </Activity>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>0.00</Weight>
            </PackageWeight>
        </Package>
 * 
 */


            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuffer successString = new StringBuffer("The UPS TrackShipment succeeded");
            if (errorList.size() > 0) {
                // this shouldn't happen much, but handle it anyway
                successString.append(", but the following occurred: ");
                Iterator errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    String errorMsg = (String) errorListIter.next();
                    successString.append(errorMsg);
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, "The UPS ShipmentConfirm failed");
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map upsRateInquire(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        // prepare the data
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                shipToAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (shipToAddress == null) {
            return ServiceUtil.returnError("Unable to determine ship-to address");
        }

        GenericValue destCountryGeo = null;
        try {
            destCountryGeo = shipToAddress.getRelatedOne("CountryGeo");
        } catch( GenericEntityException e ) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(destCountryGeo)) {
            return ServiceUtil.returnError("Destination CountryGeo not found for ship-to address");
        }
        Map cxt = UtilMisc.toMap("serviceConfigProps", context.get("serviceConfigProps"), "upsRateInquireMode", context.get("upsRateInquireMode"),
                "productStoreId", context.get("productStoreId"), "carrierRoleTypeId", context.get("carrierRoleTypeId"));
        cxt.put("carrierPartyId", context.get("carrierPartyId"));
        cxt.put("shipmentMethodTypeId", context.get("shipmentMethodTypeId"));
        cxt.put("shippingPostalCode", shipToAddress.getString("postalCode"));
        cxt.put("shippingCountryCode",destCountryGeo.getString("geoCode") );
        cxt.put("packageWeights", context.get("packageWeights"));
        cxt.put("shippableItemInfo", context.get("shippableItemInfo"));
        cxt.put("shippableTotal", context.get("shippableTotal"));
        cxt.put("shippableQuantity", context.get("shippableQuantity"));
        cxt.put("shippableWeight", context.get("shippableWeight"));
        cxt.put("isResidentialAddress", context.get("isResidentialAddress"));
        try{
            return dctx.getDispatcher().runSync("upsRateEstimateByPostalCode", cxt);
            
        }catch(GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static void splitEstimatePackages(Document requestDoc, Element shipmentElement, List shippableItemInfo, double maxWeight, double minWeight) {
        List packages = getPackageSplit(shippableItemInfo, maxWeight);
        Iterator i = packages.iterator();
        while (i.hasNext()) {
            Map packageMap = (Map) i.next();
            addPackageElement(requestDoc, shipmentElement, shippableItemInfo, packageMap, minWeight);
        }
    }

    private static void addPackageElement(Document requestDoc, Element shipmentElement, List shippableItemInfo, Map packageMap, double minWeight) {
        double packageWeight = checkForDefaultPackageWeight(calcPackageWeight(packageMap, shippableItemInfo, 0),minWeight);
        Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Code", "00", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Description", "Unknown PackagingType", requestDoc);
        UtilXml.addChildElementValue(packageElement, "Description", "Package Description", requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, "Weight", Double.toString(packageWeight), requestDoc);
        //If product is in shippable Package then it we should have one product per packagemap
        if (packageMap.size() ==1) {
            Iterator i = packageMap.keySet().iterator();
            String productId = (String) i.next();
            Map productInfo = getProductItemInfo(shippableItemInfo, productId);
            if(productInfo.get("inShippingBox") != null &&  ((String) productInfo.get("inShippingBox")).equalsIgnoreCase("Y")
                    && productInfo.get("shippingDepth") !=null && productInfo.get("shippingWidth") !=null && productInfo.get("shippingHeight") !=null ) {
                Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Length", productInfo.get("shippingDepth").toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Width", productInfo.get("shippingWidth").toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Height", productInfo.get("shippingHeight").toString(), requestDoc);
            }
        }
        
    }

    private static void addPackageElement(Document requestDoc, Element shipmentElement, Double packageWeight) {        
        Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Code", "00", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Description", "Unknown PackagingType", requestDoc);
        UtilXml.addChildElementValue(packageElement, "Description", "Package Description", requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, "Weight", packageWeight.toString(), requestDoc);        
    }
    
    
    private static double checkForDefaultPackageWeight(double weight, double minWeight) {
        return (weight > 0 && weight > minWeight ? weight : minWeight);
    }
    
    private static List getPackageSplit(List shippableItemInfo, double maxWeight) {
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
                        if(itemInfo.get("inShippingBox") != null &&  ((String) itemInfo.get("inShippingBox")).equalsIgnoreCase("Y")) {
                            Map newPackage = new HashMap();
                            newPackage.put(productId, new Double(partialQty));
                            packages.add(newPackage);
                        } else if (weight >= maxWeight) {
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
                                    double packageWeight = calcPackageWeight(packageMap, shippableItemInfo, weight);
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

    private static double calcPackageWeight(Map packageMap, List shippableItemInfo, double additionalWeight) {
        double totalWeight = 0.00;
        Iterator i = packageMap.keySet().iterator();
        while (i.hasNext()) {
            String productId = (String) i.next();
            Map productInfo = getProductItemInfo(shippableItemInfo, productId);
            double productWeight = ((Double) productInfo.get("weight")).doubleValue();
            double quantity = ((Double) packageMap.get(productId)).doubleValue();
            totalWeight += (productWeight * quantity);
        }
        return totalWeight + additionalWeight;
    }

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

    public static Map handleUpsRateInquireResponse(Document rateResponseDocument) {
        // process TrackResponse, update data as needed
        Element rateResponseElement = rateResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, "Response");
        Element responseTransactionReferenceElement = UtilXml.firstChildElement(responseElement, "TransactionReference");
        String responseTransactionReferenceCustomerContext = UtilXml.childElementValue(responseTransactionReferenceElement, "CustomerContext");
        String responseTransactionReferenceXpciVersion = UtilXml.childElementValue(responseTransactionReferenceElement, "XpciVersion");

        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        String responseStatusDescription = UtilXml.childElementValue(responseElement, "ResponseStatusDescription");
        List errorList = new LinkedList();
        UpsServices.handleErrors(responseElement, errorList);

        if ("1".equals(responseStatusCode)) {
            List rates = UtilXml.childElementList(rateResponseElement, "RatedShipment");
            Map rateMap = new HashMap();
            Double firstRate = null;
            if (rates == null || rates.size() == 0) {
                return ServiceUtil.returnError("No rates available at this time");
            } else {
                Iterator i = rates.iterator();
                while (i.hasNext()) {
                    Element element = (Element) i.next();

                    // get service
                    Element service = UtilXml.firstChildElement(element, "Service");
                    String serviceCode = UtilXml.childElementValue(service, "Code");

                    // get total
                    Element totalCharges = UtilXml.firstChildElement(element, "TotalCharges");
                    String totalString = UtilXml.childElementValue(totalCharges, "MonetaryValue");

                    rateMap.put(serviceCode, new Double(totalString));
                    if (firstRate == null) {
                        firstRate = (Double) rateMap.get(serviceCode);
                    }
                }
            }

            Debug.log("UPS Rate Map : " + rateMap, module);

            Map resp = ServiceUtil.returnSuccess();
            resp.put("upsRateCodeMap", rateMap);
            resp.put("shippingEstimateAmount", firstRate);
            return resp;
        } else {
            errorList.add("Error status code : " + responseStatusCode);
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Document createAccessRequestDocument() {
        return createAccessRequestDocument("shipment.properties");
    }

    public static Document createAccessRequestDocument(String props) {
        Document accessRequestDocument = UtilXml.makeEmptyXmlDocument("AccessRequest");
        Element accessRequestElement = accessRequestDocument.getDocumentElement();
        UtilXml.addChildElementValue(accessRequestElement, "AccessLicenseNumber", UtilProperties.getPropertyValue(props, "shipment.ups.access.license.number"), accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, "UserId", UtilProperties.getPropertyValue(props, "shipment.ups.access.user.id"), accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, "Password", UtilProperties.getPropertyValue(props, "shipment.ups.access.password"), accessRequestDocument);
        return accessRequestDocument;
    }

    public static void handleErrors(Element responseElement, List errorList) {
        List errorElements = UtilXml.childElementList(responseElement, "Error");
        Iterator errorElementIter = errorElements.iterator();
        while (errorElementIter.hasNext()) {
            StringBuffer errorMessageBuf = new StringBuffer();
            Element errorElement = (Element) errorElementIter.next();

            String errorSeverity = UtilXml.childElementValue(errorElement, "ErrorSeverity");
            String errorCode = UtilXml.childElementValue(errorElement, "ErrorCode");
            String errorDescription = UtilXml.childElementValue(errorElement, "ErrorDescription");
            String minimumRetrySeconds = UtilXml.childElementValue(errorElement, "MinimumRetrySeconds");

            errorMessageBuf.append("An error occurred [code:");
            errorMessageBuf.append(errorCode);
            errorMessageBuf.append("] with severity ");
            errorMessageBuf.append(errorSeverity);
            errorMessageBuf.append(": ");
            errorMessageBuf.append(errorDescription);
            if (UtilValidate.isNotEmpty(minimumRetrySeconds)) {
                errorMessageBuf.append("; you should wait ");
                errorMessageBuf.append(minimumRetrySeconds);
                errorMessageBuf.append(" seconds before retrying. ");
            } else {
                errorMessageBuf.append(". ");
            }

            List errorLocationElements = UtilXml.childElementList(errorElement, "ErrorLocation");
            Iterator errorLocationElementIter = errorLocationElements.iterator();
            while (errorLocationElementIter.hasNext()) {
                Element errorLocationElement = (Element) errorLocationElementIter.next();
                String errorLocationElementName = UtilXml.childElementValue(errorLocationElement, "ErrorLocationElementName");
                String errorLocationAttributeName = UtilXml.childElementValue(errorLocationElement, "ErrorLocationAttributeName");

                errorMessageBuf.append("The error was at Element [");
                errorMessageBuf.append(errorLocationElementName);
                errorMessageBuf.append("]");

                if (UtilValidate.isNotEmpty(errorLocationAttributeName)) {
                    errorMessageBuf.append(" in the attribute [");
                    errorMessageBuf.append(errorLocationAttributeName);
                    errorMessageBuf.append("]");
                }

                List errorDigestElements = UtilXml.childElementList(errorLocationElement, "ErrorDigest");
                Iterator errorDigestElementIter = errorDigestElements.iterator();
                while (errorDigestElementIter.hasNext()) {
                    Element errorDigestElement = (Element) errorDigestElementIter.next();
                    errorMessageBuf.append(" full text: [");
                    errorMessageBuf.append(UtilXml.elementValue(errorDigestElement));
                    errorMessageBuf.append("]");
                }
            }

            errorList.add(errorMessageBuf.toString());
        }
    }

    /**
     * Opens a URL to UPS and makes a request.
     * @param upsService Name of the UPS service to invoke
     * @param xmlString XML message to send
     * @return XML string response from UPS
     * @throws UpsConnectException
     */
    public static String sendUpsRequest(String upsService, String xmlString) throws UpsConnectException {
        String conStr = UtilProperties.getPropertyValue("shipment.properties", "shipment.ups.connect.url");
        if (conStr == null) {
            throw new UpsConnectException("Incomplete connection URL; check your UPS configuration");
        }
        
        // need a ups service to call
        if (upsService == null) {
            throw new UpsConnectException("UPS service name cannot be null");
        }
        
        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new UpsConnectException("XML message cannot be null");
        }
        
        // prepare the connect string
        conStr = conStr.trim();
        if (!conStr.endsWith("/")) {
            conStr = conStr + "/";
        }
        conStr = conStr + upsService;

        String timeOutStr = UtilProperties.getPropertyValue("shipment.properties", "shipment.ups.connect.timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        //Debug.log("UPS Connect URL : " + conStr, module);
        //Debug.log("UPS XML String : " + xmlString, module);
        
        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        String response = null;
        try {            
            response = http.post(xmlString);
        } catch (HttpClientException e) {            
            Debug.logError(e, "Problem connecting with UPS server", module);
            throw new UpsConnectException("URL Connection problem", e);
        }
        
        if (response == null) {
            throw new UpsConnectException("Received a null response");
        }
        if (Debug.verboseOn()) Debug.logVerbose("UPS Response : " + response, module);
        
        return response;
    }
    
    public static Map upsRateInquireByPostalCode(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();
        
        // prepare the data
        String serviceConfigProps = (String) context.get("serviceConfigProps");
        String upsRateInquireMode = (String) context.get("upsRateInquireMode");
        String productStoreId = (String) context.get("productStoreId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
       // String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shippingPostalCode = (String) context.get("shippingPostalCode");
        String shippingCountryCode = (String) context.get("shippingCountryCode");
        List packageWeights = (List) context.get("packageWeights");
        List shippableItemInfo = (List) context.get("shippableItemInfo");
        Double shippableTotal = (Double) context.get("shippableTotal");
        Double shippableQuantity = (Double) context.get("shippableQuantity");
        Double shippableWeight = (Double) context.get("shippableWeight");
        String isResidentialAddress = (String)context.get("isResidentialAddress");
        if (shippableTotal == null) {
            shippableTotal = new Double(0.00);
        }
        if (shippableQuantity == null) {
            shippableQuantity = new Double(0.00);
        }
        if (shippableWeight == null) {
            shippableWeight = new Double(0.00);
        }
        if (serviceConfigProps == null) {
            serviceConfigProps = "shipment.properties";
        }
        if (upsRateInquireMode == null || !"Shop".equals(upsRateInquireMode)) {
            // can be either Rate || Shop
            Debug.logWarning("No upsRateInquireMode set, defaulting to 'Rate'", module);
            upsRateInquireMode = "Rate";
        }
        
        // grab the pickup type; if none is defined we will assume daily pickup
        String pickupType = UtilProperties.getPropertyValue(serviceConfigProps, "shipment.ups.shipper.pickup.type", "01");
        
        // locate the ship-from address based on the product store's default facility
        GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
        
        GenericValue shipFromAddress = null;
        if (productStore != null && productStore.get("inventoryFacilityId") != null) {
            List shipLocs = null;
            try {
                shipLocs = delegator.findByAnd("FacilityContactMechPurpose", UtilMisc.toMap("facilityId",
                        productStore.getString("inventoryFacilityId"), "contactMechPurposeTypeId",
                        "SHIP_ORIG_LOCATION"), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (shipLocs != null) {
                shipLocs = EntityUtil.filterByDate(shipLocs);
                GenericValue purp =  EntityUtil.getFirst(shipLocs);
                if (purp != null) {
                    try {
                        shipFromAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", purp.getString("contactMechId")));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }
        if (shipFromAddress == null) {
            return ServiceUtil.returnError("Unable to determine ship-from address");
        }

        // obtain the ship-to address
        /*GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                shipToAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (shipToAddress == null) {
            return ServiceUtil.returnError("Unable to determine ship-to address");
        }*/

        // locate the service code
        String serviceCode = null;
        if (!"Shop".equals(upsRateInquireMode)) {
            // locate the CarrierShipmentMethod record
            GenericValue carrierShipmentMethod = null;
            try {
                carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", UtilMisc.toMap("shipmentMethodTypeId",
                        shipmentMethodTypeId, "partyId", carrierPartyId, "roleTypeId", carrierRoleTypeId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError("Unable to locate the shipping method requested");
            }

            // service code is 'carrierServiceCode'
            serviceCode = carrierShipmentMethod.getString("carrierServiceCode");
            
        }
        
        // prepare the XML Document
        Document rateRequestDoc = UtilXml.makeEmptyXmlDocument("RatingServiceSelectionRequest");
        Element rateRequestElement = rateRequestDoc.getDocumentElement();
        rateRequestElement.setAttribute("xml:lang", "en-US");
        
        // XML request header
        Element requestElement = UtilXml.addChildElement(rateRequestElement, "Request", rateRequestDoc);
        Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", rateRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "Rating and Service", rateRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", rateRequestDoc);

        // RequestAction is always Rate, but RequestOption can be Rate to get a single rate or Shop for all shipping methods
        UtilXml.addChildElementValue(requestElement, "RequestAction", "Rate", rateRequestDoc);
        UtilXml.addChildElementValue(requestElement, "RequestOption", upsRateInquireMode, rateRequestDoc);

        // set the pickup type
        Element pickupElement = UtilXml.addChildElement(rateRequestElement, "PickupType", rateRequestDoc);
        UtilXml.addChildElementValue(pickupElement, "Code", pickupType, rateRequestDoc);

        // shipment info
        Element shipmentElement = UtilXml.addChildElement(rateRequestElement, "Shipment", rateRequestDoc);

        // shipper info - (sub of shipment)
        Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", rateRequestDoc);
        Element shipperAddrElement = UtilXml.addChildElement(shipperElement, "Address", rateRequestDoc);
        UtilXml.addChildElementValue(shipperAddrElement, "PostalCode", shipFromAddress.getString("postalCode"), rateRequestDoc);

        // ship-to info - (sub of shipment)
        Element shiptoElement = UtilXml.addChildElement(shipmentElement, "ShipTo", rateRequestDoc);
        Element shiptoAddrElement = UtilXml.addChildElement(shiptoElement, "Address", rateRequestDoc);
        UtilXml.addChildElementValue(shiptoAddrElement, "PostalCode", shippingPostalCode, rateRequestDoc);
        if (shippingCountryCode != null && !shippingCountryCode.equals("")) {
            UtilXml.addChildElementValue(shiptoAddrElement, "CountryCode", shippingCountryCode, rateRequestDoc);
        }
        
        if (isResidentialAddress != null && isResidentialAddress.equals("Y")) {
            UtilXml.addChildElement(shiptoAddrElement, "ResidentialAddress", rateRequestDoc);
        }
        // requested service (code) - not used when in Shop mode
        if (serviceCode != null) {
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", rateRequestDoc);
            UtilXml.addChildElementValue(serviceElement, "Code", serviceCode, rateRequestDoc);
        }

        // package info
        String maxWeightStr = UtilProperties.getPropertyValue(serviceConfigProps, "shipment.ups.max.estimate.weight", "99");
        double maxWeight = 99;
        try {
            maxWeight = Double.parseDouble(maxWeightStr);
        } catch (NumberFormatException e) {
            maxWeight = 99;
        }
        String minWeightStr = UtilProperties.getPropertyValue(serviceConfigProps, "shipment.ups.min.estimate.weight", ".1");
        double minWeight = .1;
        try {
            minWeight = Double.parseDouble(minWeightStr);
        } catch (NumberFormatException e) {
            minWeight = .1;
        }
        
        // Passing in a list of package weights overrides the calculation of same via shippableItemInfo
        if (UtilValidate.isEmpty(packageWeights)) {           
                        
            splitEstimatePackages(rateRequestDoc, shipmentElement, shippableItemInfo, maxWeight, minWeight);
        } else {
            Iterator i = packageWeights.iterator();
            while (i.hasNext()) {
                Double packageWeight = (Double) i.next();
                addPackageElement(rateRequestDoc,  shipmentElement, packageWeight);
            }
        }

        // service options
        UtilXml.addChildElement(shipmentElement, "ShipmentServiceOptions", rateRequestDoc);

        String rateRequestString = null;
        try {
            rateRequestString = UtilXml.writeXmlDocument(rateRequestDoc);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the RatingServiceSelectionRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, module);
            return ServiceUtil.returnError(ioeErrMsg);
        }
        
        // create AccessRequest XML doc
        Document accessRequestDocument = createAccessRequestDocument(serviceConfigProps);
        String accessRequestString = null;
        try {
            accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, module);
            return ServiceUtil.returnError(ioeErrMsg);
        }

        // prepare the access/inquire request string
        StringBuffer xmlString = new StringBuffer();
        xmlString.append(accessRequestString);
        xmlString.append(rateRequestString);
        Debug.logInfo(xmlString.toString(), module);       
        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendUpsRequest("Rate", xmlString.toString());
        } catch (UpsConnectException e) {
            String uceErrMsg = "Error sending UPS request for UPS Service Rate: " + e.toString();
            Debug.logError(e, uceErrMsg, module);
            return ServiceUtil.returnError(uceErrMsg);
        }
        Debug.logInfo(rateResponseString, module);       
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        } catch (ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        } catch (IOException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, module);
            return ServiceUtil.returnError(excErrMsg);
        }        
        return handleUpsRateInquireResponse(rateResponseDocument);

    
    }
    
}

class UpsConnectException extends GeneralException {
    UpsConnectException() {
        super();
    }
    
    UpsConnectException(String msg) {
        super(msg);
    }
    
    UpsConnectException(Throwable t) {
        super(t);
    }
    
    UpsConnectException(String msg, Throwable t) {
        super(msg, t);
    }
}


/*
 * UPS Code Reference

UPS Service IDs
ShipConfirm
ShipAccept
Void
Track
Rate

Package Type Code
00 Unknown
01 UPS Letter
02 Package
03 UPS Tube
04 UPS Pak
21 UPS Express Box
24 UPS 25KG Box
25 UPS 10KG Box

Pickup Types
01 Daily Pickup
03 Customer Counter
06 One Time Pickup
07 On Call Air Pickup
19 Letter Center
20 Air Service Center

UPS Service Codes
US Origin
01 UPS Next Day Air
02 UPS 2nd Day Air
03 UPS Ground
07 UPS Worldwide Express
08 UPS Worldwide Expedited
11 UPS Standard
12 UPS 3-Day Select
13 UPS Next Day Air Saver
14 UPS Next Day Air Early AM
54 UPS Worldwide Express Plus
59 UPS 2nd Day Air AM
64 N/A
65 UPS Express Saver

Reference Number Codes
AJ Acct. Rec. Customer Acct.
AT Appropriation Number
BM Bill of Lading Number
9V COD Number
ON Dealer Order Number
DP Department Number
EI Employer's ID Number
3Q FDA Product Code
TJ Federal Taxpayer ID Number
IK Invoice Number
MK Manifest Key Number
MJ Model Number
PM Part Number
PC Production Code
PO Purchase Order No.
RQ Purchase Request No.
RZ Return Authorization No.
SA Salesperson No.
SE Serial No.
SY Social Security No.
ST Store No.
TN Transaction Ref. No.

Error Codes
First note that in the ref guide there are about 21 pages of error codes
Here are some overalls:
1 Success (no error)
01xxxx XML Error
02xxxx Architecture Error
15xxxx Tracking Specific Error

 */


/*
 * Sample XML documents:
 *
<?xml version="1.0"?>
<AccessRequest xml:lang="en-US">
   <AccessLicenseNumber>TEST262223144CAT</AccessLicenseNumber>
   <UserId>REG111111</UserId>
   <Password>REG111111</Password>
</AccessRequest>

=======================================
Shipment Confirm Request/Response
=======================================

<?xml version="1.0"?>
<ShipmentConfirmRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>Ship Confirm / nonvalidate</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>ShipConfirm</RequestAction>
        <RequestOption>nonvalidate</RequestOption>
    </Request>
    <LabelSpecification>
        <LabelPrintMethod>
            <Code>GIF</Code>
        </LabelPrintMethod>
        <HTTPUserAgent>Mozilla/5.0</HTTPUserAgent>
        <LabelImageFormat>
            <Code>GIF</Code>
        </LabelImageFormat>
    </LabelSpecification>
    <Shipment>
        <Description>DescriptionofGoodsTest</Description>
        <Shipper>
            <Name>ShipperName</Name>
            <AttentionName>ShipperName</AttentionName>
            <PhoneNumber>2226267227</PhoneNumber>
            <ShipperNumber>12345E</ShipperNumber>
            <Address>
                <AddressLine1>123 ShipperStreet</AddressLine1>
                <AddressLine2>123 ShipperStreet</AddressLine2>
                <AddressLine3>123 ShipperStreet</AddressLine3>
                <City>ShipperCity</City>
                <StateProvinceCode>foo</StateProvinceCode>
                <PostalCode>03570</PostalCode>
                <CountryCode>DE</CountryCode>
            </Address>
        </Shipper>
        <ShipTo>
            <CompanyName>ShipToCompanyName</CompanyName>
            <AttentionName>ShipToAttnName</AttentionName>
            <PhoneNumber>3336367336</PhoneNumber>
            <Address>
                <AddressLine1>123 ShipToStreet</AddressLine1>
                <PostalCode>DT09</PostalCode>
                <City>Trent</City>
                <CountryCode>GB</CountryCode>
            </Address>
        </ShipTo>
        <ShipFrom>
            <CompanyName>ShipFromCompanyName</CompanyName>
            <AttentionName>ShipFromAttnName</AttentionName>
            <PhoneNumber>7525565064</PhoneNumber>
            <Address>
                <AddressLine1>123 ShipFromStreet</AddressLine1>
                <City>Berlin</City>
                <PostalCode>03570</PostalCode>
                <CountryCode>DE</CountryCode>
            </Address>
        </ShipFrom>
        <PaymentInformation>
            <Prepaid>
                <BillShipper>
                    <AccountNumber>12345E</AccountNumber>
                </BillShipper>
            </Prepaid>
        </PaymentInformation>
        <Service>
            <Code>07</Code>
        </Service>
        <Package>
            <PackagingType>
                <Code>02</Code>
            </PackagingType>
            <Dimensions>
                <UnitOfMeasurement>
                    <Code>CM</Code>
                </UnitOfMeasurement>
                <Length>60</Length>
                <Width>7</Width>
                <Height>5</Height>
            </Dimensions>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>KGS</Code>
                </UnitOfMeasurement>
                <Weight>3.0</Weight>
            </PackageWeight>
            <ReferenceNumber>
                <Code>MK</Code>
                <Value>00001</Value>
            </ReferenceNumber>
        </Package>
    </Shipment>
</ShipmentConfirmRequest>

=======================================

<?xml version="1.0"?>
<ShipmentConfirmResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>ShipConfirmUS</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <ShipmentCharges>
        <TransportationCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>31.38</MonetaryValue>
        </TransportationCharges>
        <ServiceOptionsCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>7.75</MonetaryValue>
        </ServiceOptionsCharges>
        <TotalCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>39.13</MonetaryValue>
        </TotalCharges>
    </ShipmentCharges>
    <BillingWeight>
        <UnitOfMeasurement>
            <Code>LBS</Code>
        </UnitOfMeasurement>
        <Weight>4.0</Weight>
    </BillingWeight>
    <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
    <ShipmentDigest>INSERT SHIPPING DIGEST HERE</ShipmentDigest>
</ShipmentConfirmResponse>

=======================================
Shipment Accept Request/Response
=======================================

<?xml version="1.0"?>
<ShipmentAcceptRequest>
    <Request>
        <TransactionReference>
            <CustomerContext>TR01</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>ShipAccept</RequestAction>
        <RequestOption>01</RequestOption>
    </Request>
    <ShipmentDigest>INSERT SHIPPING DIGEST HERE</ShipmentDigest>
</ShipmentAcceptRequest>

=======================================

<?xml version="1.0"?>
<ShipmentAcceptResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>TR01</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <ShipmentResults>
        <ShipmentCharges>
            <TransportationCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>31.38</MonetaryValue>
            </TransportationCharges>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>7.75</MonetaryValue>
            </ServiceOptionsCharges>
            <TotalCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>39.13</MonetaryValue>
            </TotalCharges>
        </ShipmentCharges>
        <BillingWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>4.0</Weight>
        </BillingWeight>
        <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
        <PackageResults>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>0.00</MonetaryValue>
            </ServiceOptionsCharges>
            <LabelImage>
                <LabelImageFormat>
                    <Code>epl</Code>
                </LabelImageFormat>
                <GraphicImage>INSERT GRAPHIC IMAGE HERE</GraphicImage>
            </LabelImage>
        </PackageResults>
        <PackageResults>
            <TrackingNumber>1Z12345E1512345686</TrackingNumber>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>7.75</MonetaryValue>
            </ServiceOptionsCharges>
            <LabelImage>
                <LabelImageFormat>
                    <Code>epl</Code>
                </LabelImageFormat>
                <GraphicImage>INSERT GRAPHIC IMAGE HERE</GraphicImage>
            </LabelImage>
        </PackageResults>
    </ShipmentResults>
</ShipmentAcceptResponse>

=======================================
Void Shipment Request/Response
=======================================

<?xml version="1.0"?>
<VoidShipmentRequest>
    <Request>
        <TransactionReference>
            <CustomerContext>Void</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>Void</RequestAction>
        <RequestOption>1</RequestOption>
    </Request>
    <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
</VoidShipmentRequest>

=======================================

<?xml version="1.0"?>
<VoidShipmentResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <Status>
        <StatusType>
            <Code>1</Code>
            <Description>Success</Description>
        </StatusType>
        <StatusCode>
            <Code>1</Code>
            <Description>Success</Description>
        </StatusCode>
    </Status>
</VoidShipmentResponse>

=======================================
Track Shipment Request/Response
=======================================

<?xml version="1.0"?>
<TrackRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>sample</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>Track</RequestAction>
    </Request>
    <TrackingNumber>1Z12345E1512345676</TrackingNumber>
</TrackRequest>

=======================================

<?xml version="1.0" encoding="UTF-8"?>
<TrackResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>sample</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <Shipment>
        <Shipper>
            <ShipperNumber>12345E</ShipperNumber>
        </Shipper>
        <Service>
            <Code>15</Code>
            <Description>NDA EAM/EXP EAM</Description>
        </Service>
        <ShipmentIdentificationNumber>1Z12345E1512345676</ShipmentIdentificationNumber>
        <Package>
            <TrackingNumber>1Z12345E1512345676</TrackingNumber>
            <Activity>
                <ActivityLocation>
                    <Address>
                        <City>CLAKVILLE</City>
                        <StateProvinceCode>AK</StateProvinceCode>
                        <PostalCode>99901</PostalCode>
                        <CountryCode>US</CountryCode>
                    </Address>
                    <Code>MG</Code>
                    <Description>MC MAN</Description>
                </ActivityLocation>
                <Status>
                    <StatusType>
                        <Code>D</Code>
                        <Description>DELIVERED</Description>
                    </StatusType>
                    <StatusCode>
                        <Code>FS</Code>
                    </StatusCode>
                </Status>
                <Date>20020930</Date>
                <Time>130900</Time>
            </Activity>
            <PackageWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>0.00</Weight>
            </PackageWeight>
        </Package>
    </Shipment>
</TrackResponse>

=======================================
Rates & Service Request/Response
=======================================

<?xml version="1.0"?>
<RatingServiceSelectionRequest xml:lang="en-US">
  <Request>
    <TransactionReference>
      <CustomerContext>Bare Bones Rate Request</CustomerContext>
      <XpciVersion>1.0</XpciVersion>
    </TransactionReference>
    <RequestAction>Rate</RequestAction>
    <RequestOption>Rate</RequestOption>
  </Request>
  <PickupType>
    <Code>01</Code>
  </PickupType>
  <Shipment>
    <Shipper>
        <Address>
            <PostalCode>44129</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </Shipper>
    <ShipTo>
        <Address>
            <PostalCode>44129</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </ShipTo>
    <ShipFrom>
        <Address>
            <PostalCode>32779</PostalCode>
            <CountryCode>US</CountryCode>
        </Address>
    </ShipFrom>
    <Service>
        <Code>01</Code>
    </Service>
    <Package>
        <PackagingType>
            <Code>02</Code>
        </PackagingType>
        <Dimensions>
            <UnitOfMeasurement>
                <Code>IN</Code>
            </UnitOfMeasurement>
            <Length>20</Length>
            <Width>20</Width>
            <Height>20</Height>
        </Dimensions>
        <PackageWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>23</Weight>
        </PackageWeight>
    </Package>
  </Shipment>
</RatingServiceSelectionRequest>

=======================================

<?xml version="1.0" encoding="UTF-8"?>
<RatingServiceSelectionResponse>
    <Response>
        <TransactionReference>
            <CustomerContext>Bare Bones Rate Request</CustomerContext>
            <XpciVersion>1.0</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <RatedShipment>
        <Service>
            <Code>01</Code>
        </Service>
        <BillingWeight>
            <UnitOfMeasurement>
                <Code>LBS</Code>
            </UnitOfMeasurement>
            <Weight>42.0</Weight>
        </BillingWeight>
        <TransportationCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>108.61</MonetaryValue>
        </TransportationCharges>
        <ServiceOptionsCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>0.00</MonetaryValue>
        </ServiceOptionsCharges>
        <TotalCharges>
            <CurrencyCode>USD</CurrencyCode>
            <MonetaryValue>108.61</MonetaryValue>
        </TotalCharges>
        <GuaranteedDaysToDelivery>1</GuaranteedDaysToDelivery>
        <ScheduledDeliveryTime>10:30 A.M.</ScheduledDeliveryTime>
        <RatedPackage>
            <TransportationCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>108.61</MonetaryValue>
            </TransportationCharges>
            <ServiceOptionsCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>0.00</MonetaryValue>
            </ServiceOptionsCharges>
            <TotalCharges>
                <CurrencyCode>USD</CurrencyCode>
                <MonetaryValue>108.61</MonetaryValue>
            </TotalCharges>
            <Weight>23.0</Weight>
            <BillingWeight>
                <UnitOfMeasurement>
                    <Code>LBS</Code>
                </UnitOfMeasurement>
                <Weight>42.0</Weight>
            </BillingWeight>
        </RatedPackage>
    </RatedShipment>
</RatingServiceSelectionResponse>

 */
