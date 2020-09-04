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
package org.apache.ofbiz.shipment.thirdparty.ups;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactMechWorker;
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
import org.xml.sax.SAXException;

/**
 * UPS ShipmentServices
 */
public class UpsServices {

    private static final String MODULE = UpsServices.class.getName();
    private static final String RES_ERROR = "ProductUiLabels";
    private static final String RES_ORDER = "OrderUiLabels";

    private static final Map<String, String> UPS_TO_OFBIZ = new HashMap<>();
    private static final Map<String, String> OFBIZ_TO_UPS = new HashMap<>();
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("order.rounding");
    private static final int RET_SERVICE_CODE = 8;
    private static final String DATE_FORMAT = "yyyyMMdd";

    static {
        UPS_TO_OFBIZ.put("LBS", "WT_lb");
        UPS_TO_OFBIZ.put("KGS", "WT_kg");
        for (Map.Entry<String, String> entry : UPS_TO_OFBIZ.entrySet()) {
            OFBIZ_TO_UPS.put(entry.getValue(), entry.getKey());
        }
    }

    public static Map<String, Object> upsShipmentConfirm(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentConfirmResponseString = null;

        try {
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentNotFoundId", locale) + " " + shipmentId);
            }
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("carrierServiceStatusId"))
                    && !"SHRSCS_NOT_STARTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentStatusNotStarted",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId, "shipmentRouteSegmentStatus",
                                shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentRouteSegmentOriginPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentRouteSegmentOriginTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode"))
                    && !"001".equals(originTelecomNumber.getString("countryCode"))) {
                originPhoneNumber = originTelecomNumber.getString("countryCode") + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, "-", "");
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, " ", "");
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo", false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentRouteSegmentOriginCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentRouteSegmentDestPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            if (destTelecomNumber == null) {
                String missingErrMsg = "DestTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and "
                        + "shipmentRouteSegmentId " + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
                // for now we won't require the dest phone number, but is it required?
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne("CountryGeo", false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentRouteSegmentDestCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod").where("partyId",
                    shipmentRouteSegment.get("carrierPartyId"), "roleTypeId", "CARRIER", "shipmentMethodTypeId", shipmentRouteSegment.get(
                     "shipmentMethodTypeId")).queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentCarrierShipmentMethodNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "carrierPartyId",
                                shipmentRouteSegment.get("carrierPartyId"), "shipmentMethodTypeId", shipmentRouteSegment.get("shipmentMethodTypeId")),
                        locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList(
                    "+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            List<GenericValue> itemIssuances = shipment.getRelated("ItemIssuance", null, null, false);
            Set<String> orderIdSet = new TreeSet<>();
            for (GenericValue itemIssuance : itemIssuances) {
                orderIdSet.add(itemIssuance.getString("orderId"));
            }
            String ordersDescription = "";
            if (orderIdSet.size() > 1) {

                StringBuilder odBuf = new StringBuilder(UtilProperties.getMessage(RES_ORDER, "OrderOrders", locale) + " ");
                for (String orderId : orderIdSet) {
                    if (odBuf.length() > 0) {
                        odBuf.append(", ");
                    }
                    odBuf.append(orderId);
                }
                ordersDescription = odBuf.toString();
            } else if (!orderIdSet.isEmpty()) {
                ordersDescription = UtilProperties.getMessage(RES_ORDER, "OrderOrder", locale) + " " + orderIdSet.iterator().next();
            }

            // COD Support
            boolean allowCOD = "true".equalsIgnoreCase(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "codAllowCod", resource,
                    "shipment.ups.cod.allowCOD", "true"));

            // COD only applies if all orders involved with the shipment were paid only with EXT_COD - anything else becomes too complicated
            if (allowCOD) {

                // Get the paymentMethodTypeIds of all the orderPaymentPreferences involved with the shipment
                List<GenericValue> opps = EntityQuery.use(delegator).from("OrderPaymentPreference")
                        .where(EntityCondition.makeCondition("orderId", EntityOperator.IN, orderIdSet)).queryList();
                List<String> paymentMethodTypeIds = EntityUtil.getFieldListFromEntityList(opps, "paymentMethodTypeId", true);

                if (paymentMethodTypeIds.size() > 1 || !paymentMethodTypeIds.contains("EXT_COD")) {
                    allowCOD = false;
                }
            }

            String codSurchargeAmount = null;
            String codSurchargeCurrencyUomId = null;
            String codFundsCode = null;

            boolean codSurchargeApplyToFirstPackage = false;
            boolean codSurchargeApplyToAllPackages = false;
            boolean codSurchargeSplitBetweenPackages = false;
            boolean codSurchargeApplyToNoPackages = false;

            BigDecimal codSurchargePackageAmount = null;

            if (allowCOD) {
                codSurchargeAmount = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "codSurchargeAmount", resource, "shipment"
                        + ".ups.cod.surcharge.amount", "");
                if (UtilValidate.isEmpty(codSurchargeAmount)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsSurchargeAmountIsNotConfigurated",
                            locale));
                }
                codSurchargeCurrencyUomId = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "codSurchargeCurrencyUomId",
                        resource, "shipment.ups.cod.surcharge.currencyUomId", "");
                if (UtilValidate.isEmpty(codSurchargeCurrencyUomId)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsSurchargeCurrencyIsNotConfigurated",
                            locale));
                }
                String codSurchargeApplyToPackages = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId,
                        "codSurchargeApplyToPackage", resource, "shipment.ups.cod.surcharge.applyToPackages", "");
                if (UtilValidate.isEmpty(codSurchargeApplyToPackages)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsApplyToPackagesIsNotConfigured", locale));
                }
                codFundsCode = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "codFundsCode", resource, "shipment.ups.cod"
                        + ".codFundsCode", "");
                if (UtilValidate.isEmpty(codFundsCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsCodFundsCodeIsNotConfigured", locale));
                }

                codSurchargeApplyToFirstPackage = "first".equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeApplyToAllPackages = "all".equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeSplitBetweenPackages = "split".equalsIgnoreCase(codSurchargeApplyToPackages);
                codSurchargeApplyToNoPackages = "none".equalsIgnoreCase(codSurchargeApplyToPackages);

                if (codSurchargeApplyToNoPackages) {
                    codSurchargeAmount = "0";
                }
                codSurchargePackageAmount = new BigDecimal(codSurchargeAmount).setScale(DECIMALS, ROUNDING);
                if (codSurchargeSplitBetweenPackages) {
                    codSurchargePackageAmount = codSurchargePackageAmount.divide(new BigDecimal(shipmentPackageRouteSegs.size()), DECIMALS, ROUNDING);
                }

                if (UtilValidate.isEmpty(destTelecomNumber)) {
                    Debug.logInfo("Voice notification service will not be requested for COD shipmentId " + shipmentId + ", shipmentRouteSegmentId "
                            + shipmentRouteSegmentId + " - missing destination phone number", MODULE);
                }
                if (UtilValidate.isEmpty(shipmentRouteSegment.get("homeDeliveryType"))) {
                    Debug.logInfo("Voice notification service will not be requested for COD shipmentId " + shipmentId + ", shipmentRouteSegmentId "
                            + shipmentRouteSegmentId + " - destination address is not residential", MODULE);
                }
            }

            // Determine the currency by trying the shipmentRouteSegment, then the Shipment, then the framework's default currency, and finally
            // default to USD
            String currencyCode = null;
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                currencyCode = shipmentRouteSegment.getString("currencyUomId");
            } else if (UtilValidate.isNotEmpty(shipment.getString("currencyUomId"))) {
                currencyCode = shipment.getString("currencyUomId");
            } else {
                currencyCode = EntityUtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD", delegator);
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
            Element labelSpecificationElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "LabelSpecification",
                    shipmentConfirmRequestDoc);

            Element labelPrintMethodElement = UtilXml.addChildElement(labelSpecificationElement, "LabelPrintMethod", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelPrintMethodElement, "Code", "GIF", shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(labelSpecificationElement, "HTTPUserAgent", "Mozilla/5.0", shipmentConfirmRequestDoc);

            Element labelImageFormatElement = UtilXml.addChildElement(labelSpecificationElement, "LabelImageFormat", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(labelImageFormatElement, "Code", "GIF", shipmentConfirmRequestDoc);

            // Top Level Element: Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Shipment", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, "Description",
                    "Goods for Shipment " + shipment.get("shipmentId") + " from " + ordersDescription, shipmentConfirmRequestDoc);

            // Child of Shipment: Shipper
            String shipperNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "shipperNumber", resource, "shipment.ups"
                    + ".shipper.number", "");
            Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "Name", UtilValidate.isNotEmpty(originPostalAddress.getString("toName"))
                    ? originPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "AttentionName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "PhoneNumber", originPhoneNumber, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "ShipperNumber", shipperNumber, shipmentConfirmRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "AddressLine1", originPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipperAddressElement, "AddressLine2", originPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipperAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            // How to determine this? Add to data model...? UtilXml.addChildElement(shipperAddressElement, "ResidentialAddress",
            // shipmentConfirmRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, "ShipTo", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "CompanyName", UtilValidate.isNotEmpty(destPostalAddress.getString("toName"))
                    ? destPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "AttentionName", UtilValidate.isNotEmpty(destPostalAddress.getString("attnName"))
                    ? destPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, "PhoneNumber", destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "AddressLine1", destPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipToAddressElement, "AddressLine2", destPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipToAddressElement, "City", destPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "StateProvinceCode", destPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
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
            UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine1", originPostalAddress.getString("address1"),
                    shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine2", originPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "PostalCode", originPostalAddress.getString("postalCode"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: SoldTo
            Element soldToElement = UtilXml.addChildElement(shipmentElement, "SoldTo", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToElement, "CompanyName", UtilValidate.isNotEmpty(destPostalAddress.getString("toName"))
                    ? destPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToElement, "AttentionName", UtilValidate.isNotEmpty(destPostalAddress.getString("attnName"))
                    ? destPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(soldToElement, "PhoneNumber", destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element soldToAddressElement = UtilXml.addChildElement(soldToElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, "AddressLine1", destPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(soldToAddressElement, "AddressLine2", destPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(soldToAddressElement, "City", destPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, "StateProvinceCode", destPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, "PostalCode", destPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(soldToAddressElement, "CountryCode", destCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, "PaymentInformation", shipmentConfirmRequestDoc);

            String thirdPartyAccountNumber = shipmentRouteSegment.getString("thirdPartyAccountNumber");

            if (UtilValidate.isEmpty(thirdPartyAccountNumber)) {

                // Paid by shipper
                Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, "Prepaid", shipmentConfirmRequestDoc);
                Element billShipperElement = UtilXml.addChildElement(prepaidElement, "BillShipper", shipmentConfirmRequestDoc);

                // fill in BillShipper AccountNumber element
                String billShipperAccountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "billShipperAccountNumber",
                        resource, "shipment.ups.bill.shipper.account.number", "");
                UtilXml.addChildElementValue(billShipperElement, "AccountNumber", billShipperAccountNumber, shipmentConfirmRequestDoc);
            } else {

                // Paid by another shipper (may be receiver or not)

                // UPS requires the postal code and country code of the third party
                String thirdPartyPostalCode = shipmentRouteSegment.getString("thirdPartyPostalCode");
                if (UtilValidate.isEmpty(thirdPartyPostalCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentThirdPartyPostalCodeNotFound",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                }
                String thirdPartyCountryGeoCode = shipmentRouteSegment.getString("thirdPartyCountryGeoCode");
                if (UtilValidate.isEmpty(thirdPartyCountryGeoCode)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentThirdPartyCountryNotFound",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
                }

                Element billThirdPartyElement = UtilXml.addChildElement(paymentInformationElement, "BillThirdParty", shipmentConfirmRequestDoc);
                Element billThirdPartyShipperElement = UtilXml.addChildElement(billThirdPartyElement, "BillThirdPartyShipper",
                        shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(billThirdPartyShipperElement, "AccountNumber", thirdPartyAccountNumber, shipmentConfirmRequestDoc);
                Element thirdPartyElement = UtilXml.addChildElement(billThirdPartyShipperElement, "ThirdParty", shipmentConfirmRequestDoc);
                Element addressElement = UtilXml.addChildElement(thirdPartyElement, "Address", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "PostalCode", thirdPartyPostalCode, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(addressElement, "CountryCode", thirdPartyCountryGeoCode, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", shipmentConfirmRequestDoc);
            String carrierServiceCode = carrierShipmentMethod.getString("carrierServiceCode");
            UtilXml.addChildElementValue(serviceElement, "Code", carrierServiceCode, shipmentConfirmRequestDoc);

            // Child of Shipment: ShipmentServiceOptions
            List<String> internationalServiceCodes = UtilMisc.toList("07", "08", "54", "65");
            if (internationalServiceCodes.contains(carrierServiceCode)) {
                Element shipmentServiceOptionsElement = UtilXml.addChildElement(shipmentElement, "ShipmentServiceOptions", shipmentConfirmRequestDoc);
                Element internationalFormsElement = UtilXml.addChildElement(shipmentServiceOptionsElement, "InternationalForms",
                        shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, "FormType", "01", shipmentConfirmRequestDoc);
                List<GenericValue> shipmentItems = shipment.getRelated("ShipmentItem", null, null, false);
                for (GenericValue shipmentItem : shipmentItems) {
                    Element productElement = UtilXml.addChildElement(internationalFormsElement, "Product", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(productElement, "Description", "Product Description", shipmentConfirmRequestDoc);
                    Element unitElement = UtilXml.addChildElement(productElement, "Unit", shipmentConfirmRequestDoc);
                    BigDecimal productQuantity = shipmentItem.getBigDecimal("quantity").setScale(DECIMALS, ROUNDING);
                    UtilXml.addChildElementValue(unitElement, "Number", String.valueOf(productQuantity.intValue()), shipmentConfirmRequestDoc);
                    List<GenericValue> shipmentItemIssuances = shipmentItem.getRelated("ItemIssuance", null, null, false);
                    GenericValue orderItem = EntityUtil.getFirst(shipmentItemIssuances).getRelatedOne("OrderItem", false);
                    UtilXml.addChildElementValue(unitElement, "Value", orderItem.getBigDecimal("unitPrice").toString(), shipmentConfirmRequestDoc);
                    Element unitOfMeasurElement = UtilXml.addChildElement(unitElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(unitOfMeasurElement, "Code", "EA", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(productElement, "OriginCountryCode", "US", shipmentConfirmRequestDoc);
                }
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                String invoiceDate = formatter.format(shipment.getTimestamp("createdDate"));
                UtilXml.addChildElementValue(internationalFormsElement, "InvoiceDate", invoiceDate, shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, "ReasonForExport", "SALE", shipmentConfirmRequestDoc);
                UtilXml.addChildElementValue(internationalFormsElement, "CurrencyCode", currencyCode, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Package
            ListIterator<GenericValue> shipmentPackageRouteSegIter = shipmentPackageRouteSegs.listIterator();
            while (shipmentPackageRouteSegIter.hasNext()) {
                GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType", false);
                List<GenericValue> carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "UPS"),
                        null, false);
                GenericValue carrierShipmentBoxType = null;
                if (!carrierShipmentBoxTypes.isEmpty()) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", shipmentConfirmRequestDoc);
                Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", shipmentConfirmRequestDoc);
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get("packagingTypeCode") != null) {
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", carrierShipmentBoxType.getString("packagingTypeCode"),
                            shipmentConfirmRequestDoc);
                } else {
                    // default to "02", plain old Package
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", "02", shipmentConfirmRequestDoc);
                }
                if (shipmentBoxType != null) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", shipmentConfirmRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne("DimensionUom", false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code",
                                dimensionUom.getString("abbreviation").toUpperCase(Locale.getDefault()), shipmentConfirmRequestDoc);
                    } else {
                        // I guess we'll default to inches...
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", ModelService.IN_PARAM, shipmentConfirmRequestDoc);
                    }
                    BigDecimal boxLength = shipmentBoxType.getBigDecimal("boxLength");
                    BigDecimal boxWidth = shipmentBoxType.getBigDecimal("boxWidth");
                    BigDecimal boxHeight = shipmentBoxType.getBigDecimal("boxHeight");
                    UtilXml.addChildElementValue(dimensionsElement, "Length", UtilValidate.isNotEmpty(boxLength) ? "" + boxLength.intValue() : "",
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Width", UtilValidate.isNotEmpty(boxWidth) ? "" + boxWidth.intValue() : "",
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Height", UtilValidate.isNotEmpty(boxHeight) ? "" + boxHeight.intValue() : "",
                            shipmentConfirmRequestDoc);
                } else if (UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal("boxLength"))
                        && UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal("boxWidth"))
                        && UtilValidate.isNotEmpty(shipmentPackage.getBigDecimal("boxHeight"))) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", shipmentConfirmRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
                    GenericValue dimensionUom = shipmentPackage.getRelatedOne("DimensionUom", false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code",
                                dimensionUom.getString("abbreviation").toUpperCase(Locale.getDefault()), shipmentConfirmRequestDoc);
                    } else {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", ModelService.IN_PARAM, shipmentConfirmRequestDoc);
                    }
                    UtilXml.addChildElementValue(dimensionsElement, "Length", "" + shipmentPackage.getBigDecimal("boxLength").intValue(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Width", "" + shipmentPackage.getBigDecimal("boxWidth").intValue(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Height", "" + shipmentPackage.getBigDecimal("boxHeight").intValue(),
                            shipmentConfirmRequestDoc);
                }

                Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", shipmentConfirmRequestDoc);
                Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, "UnitOfMeasurement",
                        shipmentConfirmRequestDoc);
                String weightUomUps = null;
                if (shipmentPackage.get("weightUomId") != null) {
                    weightUomUps = OFBIZ_TO_UPS.get(shipmentPackage.get("weightUomId"));
                }
                if (weightUomUps != null) {
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", weightUomUps, shipmentConfirmRequestDoc);
                } else {
                    // might as well default to LBS
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", "LBS", shipmentConfirmRequestDoc);
                }

                if (shipmentPackage.getString("weight") == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsWeightValueNotFound",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentPackageSeqId",
                                    shipmentPackage.getString("shipmentPackageSeqId")), locale));
                }
                BigDecimal boxWeight = shipmentPackage.getBigDecimal("weight");
                UtilXml.addChildElementValue(packageWeightElement, "Weight", UtilValidate.isNotEmpty(boxWeight) ? "" + boxWeight.setScale(0,
                        RoundingMode.CEILING) : "", shipmentConfirmRequestDoc);
                // Adding only when order is not an international order
                if (!internationalServiceCodes.contains(carrierServiceCode)) {
                    Element referenceNumberElement = UtilXml.addChildElement(packageElement, "ReferenceNumber", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(referenceNumberElement, "Code", "MK", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(referenceNumberElement, "Value", shipmentPackage.getString("shipmentPackageSeqId"),
                            shipmentConfirmRequestDoc);
                }
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get("oversizeCode") != null) {
                    UtilXml.addChildElementValue(packageElement, "OversizePackage", carrierShipmentBoxType.getString("oversizeCode"),
                            shipmentConfirmRequestDoc);
                }

                Element packageServiceOptionsElement = UtilXml.addChildElement(packageElement, "PackageServiceOptions", shipmentConfirmRequestDoc);

                // Package insured value
                BigDecimal insuredValue = shipmentPackage.getBigDecimal("insuredValue");
                if (!UtilValidate.isEmpty(insuredValue)) {

                    Element insuredValueElement = UtilXml.addChildElement(packageServiceOptionsElement, "InsuredValue", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, "MonetaryValue", insuredValue.setScale(2, RoundingMode.HALF_UP).toString(),
                            shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(insuredValueElement, "CurrencyCode", currencyCode, shipmentConfirmRequestDoc);
                }

                if (allowCOD) {
                    Element codElement = UtilXml.addChildElement(packageServiceOptionsElement, "COD", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codElement, "CODCode", "3", shipmentConfirmRequestDoc); // "3" is the only valid value for
                    // package-level COD
                    UtilXml.addChildElementValue(codElement, "CODFundsCode", codFundsCode, shipmentConfirmRequestDoc);
                    Element codAmountElement = UtilXml.addChildElement(codElement, "CODAmount", shipmentConfirmRequestDoc);
                    UtilXml.addChildElementValue(codAmountElement, "CurrencyCode", currencyCode, shipmentConfirmRequestDoc);

                    // Get the value of the package by going back to the orderItems
                    Map<String, Object> getPackageValueResult = dispatcher.runSync("getShipmentPackageValueFromOrders", UtilMisc.toMap("shipmentId",
                            shipmentId, "shipmentPackageSeqId", shipmentPackage.get("shipmentPackageSeqId"), "currencyUomId", currencyCode,
                            "userLogin", userLogin, "locale", locale));
                    if (ServiceUtil.isError(getPackageValueResult)) return getPackageValueResult;
                    BigDecimal packageValue = (BigDecimal) getPackageValueResult.get("packageValue");

                    // Convert the value of the COD surcharge to the shipment currency, if necessary
                    Map<String, Object> convertUomResult = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId",
                            codSurchargeCurrencyUomId, "uomIdTo", currencyCode, "originalValue", codSurchargePackageAmount));
                    if (ServiceUtil.isError(convertUomResult)) return convertUomResult;
                    if (convertUomResult.containsKey("convertedValue")) {
                        codSurchargePackageAmount = ((BigDecimal) convertUomResult.get("convertedValue")).setScale(DECIMALS, ROUNDING);
                    }

                    // Add the amount of the surcharge for the package, if the surcharge should be on all packages or the first and this is the
                    // first package
                    if (codSurchargeApplyToAllPackages || codSurchargeSplitBetweenPackages || (codSurchargeApplyToFirstPackage
                            && shipmentPackageRouteSegIter.previousIndex() <= 0)) {
                        packageValue = packageValue.add(codSurchargePackageAmount);
                    }

                    UtilXml.addChildElementValue(codAmountElement, "MonetaryValue", packageValue.setScale(DECIMALS, ROUNDING).toString(),
                            shipmentConfirmRequestDoc);
                }
            }

            String shipmentConfirmRequestString = null;
            try {
                shipmentConfirmRequestString = UtilXml.writeXmlDocument(shipmentConfirmRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentConfirmRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorShipmentConfirmRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentConfirmRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmRequest" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            try {
                shipmentConfirmResponseString = sendUpsRequest("ShipConfirm", xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipConfirm: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingShipConfirm",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsShipmentConfirmResponse" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(shipmentConfirmResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            Document shipmentConfirmResponseDocument = null;
            try {
                shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the ShipmentConfirmResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingShipmentConfirm",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }

            return handleUpsShipmentConfirmResponse(shipmentConfirmResponseDocument, shipmentRouteSegment, locale);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentConfirm",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            if (shipmentConfirmResponseString != null) {
                Debug.logError("Got XML ShipmentConfirmRespose: " + shipmentConfirmResponseString, MODULE);
                return ServiceUtil.returnError(UtilMisc.toList(
                        UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentConfirm",
                                UtilMisc.toMap("errorString", e.toString()), locale),
                        UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentConfirmResposeWasReceived",
                                UtilMisc.toMap("shipmentConfirmResponseString", shipmentConfirmResponseString), locale)));
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentConfirm",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
        }
    }

    public static Map<String, Object> handleUpsShipmentConfirmResponse(Document shipmentConfirmResponseDocument, GenericValue shipmentRouteSegment,
                                                                       Locale locale) throws GenericEntityException {
        // process ShipmentConfirmResponse, update data as needed
        Element shipmentConfirmResponseElement = shipmentConfirmResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if ("1".equals(responseStatusCode)) {
            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "ShipmentCharges");

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TransportationCharges");
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, "MonetaryValue");

            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "ServiceOptionsCharges");
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, "MonetaryValue");

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TotalCharges");
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, "CurrencyCode");
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, "MonetaryValue");

            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                } else if (!totalCurrencyCode.equals(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsCurrencyDoesNotMatch",
                            UtilMisc.toMap("currency1", totalCurrencyCode, "currency2", shipmentRouteSegment.getString("currencyUomId")), locale));
                }
            }

            try {
                shipmentRouteSegment.set("actualTransportCost", new BigDecimal(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the transportationMonetaryValue [" + transportationMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingTransportationMonetaryValue",
                        UtilMisc.toMap("transportationMonetaryValue", transportationMonetaryValue, "errorString", e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set("actualServiceCost", new BigDecimal(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the serviceOptionsMonetaryValue [" + serviceOptionsMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue",
                        UtilMisc.toMap("serviceOptionsMonetaryValue", serviceOptionsMonetaryValue, "errorString", e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set("actualCost", new BigDecimal(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the totalMonetaryValue [" + totalMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingTotalMonetaryValue",
                        UtilMisc.toMap("totalMonetaryValue", totalMonetaryValue, "errorString", e.toString()), locale));
            }

            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "BillingWeight");
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, "UnitOfMeasurement");
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, "Code");
            String billingWeight = UtilXml.childElementValue(billingWeightElement, "Weight");
            try {
                shipmentRouteSegment.set("billingWeight", new BigDecimal(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the billingWeight [" + billingWeight + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingBillingWeight",
                        UtilMisc.toMap("billingWeight", billingWeight, "errorString", e.toString()), locale));
            }
            shipmentRouteSegment.set("billingWeightUomId", UPS_TO_OFBIZ.get(billingWeightUnitOfMeasurement));

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
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsShipmentConfirmSucceeded", locale));

            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentConfirmError", locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentConfirmFailed", locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsShipmentAccept(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String shipmentAcceptResponseString = null;

        try {
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_CONFIRMED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentStatusNotConfirmed",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId, "shipmentRouteSegmentStatus",
                                shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList(
                    "+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingDigest"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsTrackingDigestWasNotSet", locale));
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

            UtilXml.addChildElementValue(shipmentAcceptRequestElement, "ShipmentDigest", shipmentRouteSegment.getString("trackingDigest"),
                    shipmentAcceptRequestDoc);


            String shipmentAcceptRequestString = null;
            try {
                shipmentAcceptRequestString = UtilXml.writeXmlDocument(shipmentAcceptRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentAcceptRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentAcceptRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsShipmentAcceptRequest" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            try {
                shipmentAcceptResponseString = sendUpsRequest("ShipAccept", xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipAccept: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingShipAccept",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsShipmentAcceptResponse" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(shipmentAcceptResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            Document shipmentAcceptResponseDocument = null;
            try {
                shipmentAcceptResponseDocument = UtilXml.readXmlDocument(shipmentAcceptResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the ShipmentAcceptResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingShipmentAcceptResponse",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }

            return handleUpsShipmentAcceptResponse(shipmentAcceptResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs, delegator,
                    shipmentGatewayConfigId, resource, context, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentAccept",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsShipmentAcceptResponse(Document shipmentAcceptResponseDocument, GenericValue shipmentRouteSegment,
                                                                      List<GenericValue> shipmentPackageRouteSegs,
                                                                      Delegator delegator, String shipmentGatewayConfigId, String resource,
                                                                      Map<String, ? extends Object> context, Locale locale)
            throws GenericEntityException {
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
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
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if ("1".equals(responseStatusCode)) {
            Element shipmentResultsElement = UtilXml.firstChildElement(shipmentAcceptResponseElement, "ShipmentResults");

            // This information is returned in both the ShipmentConfirmResponse and
            //the ShipmentAcceptResponse. So, we'll go ahead and store it here again
            //and warn of changes or something...


            // handle ShipmentCharges element info
            Element shipmentChargesElement = UtilXml.firstChildElement(shipmentResultsElement, "ShipmentCharges");

            Element transportationChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TransportationCharges");
            String transportationMonetaryValue = UtilXml.childElementValue(transportationChargesElement, "MonetaryValue");

            Element serviceOptionsChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "ServiceOptionsCharges");
            String serviceOptionsMonetaryValue = UtilXml.childElementValue(serviceOptionsChargesElement, "MonetaryValue");

            Element totalChargesElement = UtilXml.firstChildElement(shipmentChargesElement, "TotalCharges");
            String totalCurrencyCode = UtilXml.childElementValue(totalChargesElement, "CurrencyCode");
            String totalMonetaryValue = UtilXml.childElementValue(totalChargesElement, "MonetaryValue");

            if (UtilValidate.isNotEmpty(totalCurrencyCode)) {
                if (UtilValidate.isEmpty(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                } else if (!totalCurrencyCode.equals(shipmentRouteSegment.getString("currencyUomId"))) {
                    shipmentRouteSegment.set("currencyUomId", totalCurrencyCode);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsCurrencyDoesNotMatch",
                            UtilMisc.toMap("currency1", totalCurrencyCode, "currency2", shipmentRouteSegment.getString("currencyUomId")), locale));
                }
            }

            try {
                shipmentRouteSegment.set("actualTransportCost", new BigDecimal(transportationMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the transportationMonetaryValue [" + transportationMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingTransportationMonetaryValue",
                        UtilMisc.toMap("transportationMonetaryValue", transportationMonetaryValue, "errorString", e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set("actualServiceCost", new BigDecimal(serviceOptionsMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the serviceOptionsMonetaryValue [" + serviceOptionsMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue",
                        UtilMisc.toMap("serviceOptionsMonetaryValue", serviceOptionsMonetaryValue, "errorString", e.toString()), locale));
            }
            try {
                shipmentRouteSegment.set("actualCost", new BigDecimal(totalMonetaryValue));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the totalMonetaryValue [" + totalMonetaryValue + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingTotalMonetaryValue",
                        UtilMisc.toMap("totalMonetaryValue", totalMonetaryValue, "errorString", e.toString()), locale));
            }

            // handle BillingWeight element info
            Element billingWeightElement = UtilXml.firstChildElement(shipmentResultsElement, "BillingWeight");
            Element billingWeightUnitOfMeasurementElement = UtilXml.firstChildElement(billingWeightElement, "UnitOfMeasurement");
            String billingWeightUnitOfMeasurement = UtilXml.childElementValue(billingWeightUnitOfMeasurementElement, "Code");
            String billingWeight = UtilXml.childElementValue(billingWeightElement, "Weight");
            try {
                shipmentRouteSegment.set("billingWeight", new BigDecimal(billingWeight));
            } catch (NumberFormatException e) {
                String excErrMsg = "Error parsing the billingWeight [" + billingWeight + "]: " + e.toString();
                Debug.logError(e, excErrMsg, MODULE);
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingBillingWeight",
                        UtilMisc.toMap("billingWeight", billingWeight, "errorString", e.toString()), locale));
            }
            shipmentRouteSegment.set("billingWeightUomId", UPS_TO_OFBIZ.get(billingWeightUnitOfMeasurement));

            // store the ShipmentIdentificationNumber and ShipmentDigest
            String shipmentIdentificationNumber = UtilXml.childElementValue(shipmentResultsElement, "ShipmentIdentificationNumber");
            // should compare to trackingIdNumber, should always be the same right?
            shipmentRouteSegment.set("trackingIdNumber", shipmentIdentificationNumber);

            // set ShipmentRouteSegment carrierServiceStatusId after each UPS service applicable
            shipmentRouteSegment.put("carrierServiceStatusId", "SHRSCS_ACCEPTED");

            // write/store modified value object
            shipmentRouteSegment.store();

            // now process the PackageResults elements
            List<? extends Element> packageResultsElements = UtilXml.childElementList(shipmentResultsElement, "PackageResults");
            Iterator<GenericValue> shipmentPackageRouteSegIter = shipmentPackageRouteSegs.iterator();
            for (Element packageResultsElement : packageResultsElements) {
                String trackingNumber = UtilXml.childElementValue(packageResultsElement, "TrackingNumber");

                Element packageServiceOptionsChargesElement = UtilXml.firstChildElement(packageResultsElement, "ServiceOptionsCharges");
                String packageServiceOptionsCurrencyCode = UtilXml.childElementValue(packageServiceOptionsChargesElement, "CurrencyCode");
                String packageServiceOptionsMonetaryValue = UtilXml.childElementValue(packageServiceOptionsChargesElement, "MonetaryValue");

                Element packageLabelImageElement = UtilXml.firstChildElement(packageResultsElement, "LabelImage");
                //Element packageLabelImageFormatElement = UtilXml.firstChildElement(packageResultsElement, "LabelImageFormat");
                // will be EPL or GIF, should always be GIF since that is what we requested
                String packageLabelGraphicImageString = UtilXml.childElementValue(packageLabelImageElement, "GraphicImage");
                String packageLabelInternationalSignatureGraphicImageString = UtilXml.childElementValue(packageLabelImageElement,
                        "InternationalSignatureGraphicImage");
                String packageLabelHTMLImageString = UtilXml.childElementValue(packageLabelImageElement, "HTMLImage");

                if (!shipmentPackageRouteSegIter.hasNext()) {
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorMorePackageResultsWereReturned",
                            UtilMisc.toMap("trackingNumber", trackingNumber, "ServiceOptionsCharges",
                                    packageServiceOptionsMonetaryValue + packageServiceOptionsCurrencyCode), locale));
                    // NOTE: if this happens much we should just create a new package to store all of the info...
                    continue;
                }

                //NOTE: I guess they come back in the same order we sent them, so we'll get the packages in order and off we go...
                GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                shipmentPackageRouteSeg.set("trackingCode", trackingNumber);
                shipmentPackageRouteSeg.set("boxNumber", "");
                shipmentPackageRouteSeg.set("currencyUomId", packageServiceOptionsCurrencyCode);
                try {
                    shipmentPackageRouteSeg.set("packageServiceCost", new BigDecimal(packageServiceOptionsMonetaryValue));
                } catch (NumberFormatException e) {
                    String excErrMsg = "Error parsing the packageServiceOptionsMonetaryValue [" + packageServiceOptionsMonetaryValue + "] for "
                            + "Package [" + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + "]: " + e.toString();
                    Debug.logError(e, excErrMsg, MODULE);
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingServiceOptionsMonetaryValue",
                            UtilMisc.toMap("serviceOptionsMonetaryValue", serviceOptionsMonetaryValue, "errorString", e.toString()), locale));
                }
                byte[] labelImageBytes = null;
                if (packageLabelGraphicImageString != null) {
                    labelImageBytes = Base64.getMimeDecoder().decode(packageLabelGraphicImageString.getBytes(StandardCharsets.UTF_8));
                    shipmentPackageRouteSeg.setBytes("labelImage", labelImageBytes);
                }
                byte[] labelInternationalSignatureGraphicImageBytes = null;
                if (packageLabelInternationalSignatureGraphicImageString != null) {
                    labelInternationalSignatureGraphicImageBytes =
                            Base64.getMimeDecoder().decode(packageLabelInternationalSignatureGraphicImageString.getBytes(StandardCharsets.UTF_8));
                    shipmentPackageRouteSeg.set("labelIntlSignImage", labelInternationalSignatureGraphicImageBytes);
                }
                String packageLabelHTMLImageStringDecoded =
                        Arrays.toString(Base64.getMimeDecoder().decode(packageLabelHTMLImageString.getBytes(StandardCharsets.UTF_8)));
                shipmentPackageRouteSeg.set("labelHtml", packageLabelHTMLImageStringDecoded);

                if (shipmentUpsSaveCertificationInfo) {
                    if (labelImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/label" + trackingNumber + ".gif";
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(labelImageBytes);
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e,
                                    "Could not save UPS LabelImage GIF file: [[[" + packageLabelGraphicImageString + "]]] to file: "
                                            + outFileName, MODULE);
                        }
                    }
                    if (labelInternationalSignatureGraphicImageBytes != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentLabelIntlSignImage" + "label" + trackingNumber + ".gif";
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(labelInternationalSignatureGraphicImageBytes);
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e,
                                    "Could not save UPS IntlSign LabelImage GIF file: [[["
                                            + packageLabelInternationalSignatureGraphicImageString + "]]] " + "to file: " + outFileName, MODULE);
                        }
                    }
                    if (packageLabelHTMLImageStringDecoded != null) {
                        String outFileName = shipmentUpsSaveCertificationPath + "/UpsShipmentLabelHTMLImage" + shipmentRouteSegment.getString(
                                "shipmentId") + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + "_"
                                + shipmentPackageRouteSeg.getString("shipmentPackageSeqId") + ".html";
                        try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                            fileOut.write(packageLabelHTMLImageStringDecoded.getBytes(StandardCharsets.UTF_8));
                            fileOut.flush();
                        } catch (IOException e) {
                            Debug.logInfo(e, "Could not save UPS LabelImage HTML file: [[[" + packageLabelHTMLImageStringDecoded + "]]] to file: "
                                            + outFileName, MODULE);
                        }
                    }
                }

                shipmentPackageRouteSeg.store();
            }

            if (shipmentPackageRouteSegIter.hasNext()) {
                errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorMorePackageOnThisShipment", locale));

                while (shipmentPackageRouteSegIter.hasNext()) {
                    GenericValue shipmentPackageRouteSeg = shipmentPackageRouteSegIter.next();
                    errorList.add(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorNoPackageResultsWereReturned",
                            UtilMisc.toMap("shipmentPackageSeqId", shipmentPackageRouteSeg.getString("shipmentPackageSeqId")), locale));
                }
            }

            // save the High Value Report image if it exists
            Element controlLogReceiptElement = UtilXml.firstChildElement(shipmentResultsElement, "ControlLogReceipt");
            if (controlLogReceiptElement != null) {
                String fileString = UtilXml.childElementValue(controlLogReceiptElement, "GraphicImage");
                String fileStringDecoded = Arrays.toString(Base64.getMimeDecoder().decode(fileString.getBytes(StandardCharsets.UTF_8)));
                if (fileStringDecoded != null) {
                    shipmentRouteSegment.set("upsHighValueReport", fileStringDecoded);
                    shipmentRouteSegment.store();
                    String outFileName =
                            shipmentUpsSaveCertificationPath + "/HighValueReport" + shipmentRouteSegment.getString("shipmentId")
                                    + "_" + shipmentRouteSegment.getString("shipmentRouteSegmentId") + ".html";
                    try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                        fileOut.write(fileStringDecoded.getBytes(StandardCharsets.UTF_8));
                        fileOut.flush();
                    } catch (IOException e) {
                        Debug.logInfo(e, "Could not save UPS High Value Report data: [[[" + fileStringDecoded + "]]] to file: " + outFileName,
                                MODULE);
                    }
                }
            }

            // -=-=-=- Okay, now done with that, just return any extra info...
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsShipmentAcceptSucceeded", locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsShipmentAcceptError", locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentAcceptFailed", locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsVoidShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String voidShipmentResponseString = null;

        try {
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_CONFIRMED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))
                    && !"SHRSCS_ACCEPTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentStatusMustBeConfirmedOrAccepted",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId, "shipmentRouteSegmentStatus",
                                shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingIdNumber"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsTrackingIdNumberWasNotSet", locale));
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

            UtilXml.addChildElementValue(voidShipmentRequestElement, "ShipmentIdentificationNumber", shipmentRouteSegment.getString(
                    "trackingIdNumber"), voidShipmentRequestDoc);

            String voidShipmentRequestString = null;
            try {
                voidShipmentRequestString = UtilXml.writeXmlDocument(voidShipmentRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the VoidShipmentRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorVoidShipmentRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(voidShipmentRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsVoidShipmentRequest" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            try {
                voidShipmentResponseString = sendUpsRequest("Void", xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service Void: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingVoid",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsVoidShipmentResponse" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(voidShipmentResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            Document voidShipmentResponseDocument = null;
            try {
                voidShipmentResponseDocument = UtilXml.readXmlDocument(voidShipmentResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the VoidShipmentResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingVoidShipmentResponse",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }

            return handleUpsVoidShipmentResponse(voidShipmentResponseDocument, shipmentRouteSegment, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentVoid",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsVoidShipmentResponse(Document voidShipmentResponseDocument, GenericValue shipmentRouteSegment,
                                                                    Locale locale) throws GenericEntityException {
        // process VoidShipmentResponse, update data as needed
        Element voidShipmentResponseElement = voidShipmentResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(voidShipmentResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

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
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentVoidSucceeded",
                    UtilMisc.toMap("statusTypeCode", statusTypeCode, "statusTypeDescription", statusTypeDescription,
                            "statusCodeCode", statusCodeCode, "statusCodeDescription", statusCodeDescription), locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentVoidError", locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentVoidFailed",
                    UtilMisc.toMap("statusTypeCode", statusTypeCode, "statusTypeDescription", statusTypeDescription,
                            "statusCodeCode", statusCodeCode, "statusCodeDescription", statusCodeDescription), locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsTrackShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        String trackResponseString = null;

        try {
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // add ShipmentRouteSegment carrierServiceStatusId, check before all UPS services
            if (!"SHRSCS_ACCEPTED".equals(shipmentRouteSegment.getString("carrierServiceStatusId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentStatusNotAccepted",
                        UtilMisc.toMap("shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId, "shipmentRouteSegmentStatus",
                                shipmentRouteSegment.getString("carrierServiceStatusId")), locale));
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList(
                    "+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsPackageRouteSegsNotFound", UtilMisc.toMap(
                        "shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentRouteSegment.getString("trackingIdNumber"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsTrackingIdNumberWasNotSet", locale));
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

            UtilXml.addChildElementValue(trackRequestElement, "ShipmentIdentificationNumber", shipmentRouteSegment.getString("trackingIdNumber"),
                    trackRequestDoc);

            String trackRequestString = null;
            try {
                trackRequestString = UtilXml.writeXmlDocument(trackRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the TrackRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorTrackRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(trackRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName = shipmentUpsSaveCertificationPath + "/UpsTrackRequest" + shipmentId + "_" + shipmentRouteSegment.getString(
                        "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            try {
                trackResponseString = sendUpsRequest("Track", xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service Track: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingTrack",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsTrackResponseString" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(trackResponseString.getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }

            Document trackResponseDocument = null;
            try {
                trackResponseDocument = UtilXml.readXmlDocument(trackResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the TrackResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingTrackResponse",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }

            return handleUpsTrackShipmentResponse(trackResponseDocument, shipmentRouteSegment, shipmentPackageRouteSegs, locale);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentTrack",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsTrackShipmentResponse(Document trackResponseDocument, GenericValue shipmentRouteSegment,
                                         List<GenericValue> shipmentPackageRouteSegs, Locale locale) throws GenericEntityException {
        // process TrackResponse, update data as needed
        Element trackResponseElement = trackResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(trackResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if ("1".equals(responseStatusCode)) {
            // TODO: handle other response elements
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
            StringBuilder successString = new StringBuilder(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentTrackSucceeded",
                    locale));
            if (!errorList.isEmpty()) {
                // this shouldn't happen much, but handle it anyway
                successString.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentTrackError", locale));
                Iterator<Object> errorListIter = errorList.iterator();
                while (errorListIter.hasNext()) {
                    successString.append(errorListIter.next());
                    if (errorListIter.hasNext()) {
                        successString.append(", ");
                    }
                }
            }
            return ServiceUtil.returnSuccess(successString.toString());
        } else {
            errorList.add(0, UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentTrackFailed", locale));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsRateInquire(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        // prepare the data
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shippingOriginContactMechId = (String) context.get("shippingOriginContactMechId");
        // obtain the ship-to address
        GenericValue shipToAddress = null;
        if (shippingContactMechId != null) {
            try {
                shipToAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingContactMechId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (shipToAddress == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUnableFoundShipToAddresss", locale));
        }

        // obtain the ship from address if provided
        GenericValue shipFromAddress = null;
        if (shippingOriginContactMechId != null) {
            try {
                shipFromAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", shippingOriginContactMechId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUnableFoundShipToAddresssForDropShipping",
                        locale));
            }
        }

        GenericValue destCountryGeo = null;
        try {
            destCountryGeo = shipToAddress.getRelatedOne("CountryGeo", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(destCountryGeo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipToAddresssNoDestionationCountry", locale));
        }
        Map<String, Object> cxt = UtilMisc.toMap("serviceConfigProps", context.get("serviceConfigProps"), "upsRateInquireMode", context.get(
                "upsRateInquireMode"),
                "productStoreId", context.get("productStoreId"), "carrierRoleTypeId", context.get("carrierRoleTypeId"));
        cxt.put("carrierPartyId", context.get("carrierPartyId"));
        cxt.put("shipmentMethodTypeId", context.get("shipmentMethodTypeId"));
        cxt.put("shippingPostalCode", shipToAddress.getString("postalCode"));
        cxt.put("shippingCountryCode", destCountryGeo.getString("geoCode"));
        cxt.put("packageWeights", context.get("packageWeights"));
        cxt.put("shippableItemInfo", context.get("shippableItemInfo"));
        cxt.put("shippableTotal", context.get("shippableTotal"));
        cxt.put("shippableQuantity", context.get("shippableQuantity"));
        cxt.put("shippableWeight", context.get("shippableWeight"));
        cxt.put("isResidentialAddress", context.get("isResidentialAddress"));
        cxt.put("shipFromAddress", shipFromAddress);
        cxt.put("shipmentGatewayConfigId", context.get("shipmentGatewayConfigId"));
        try {
            Map<String, Object> serviceResult = dctx.getDispatcher().runSync("upsRateEstimateByPostalCode", cxt);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
            }
            return serviceResult;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRateEstimateError",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
    }

    private static void splitEstimatePackages(DispatchContext dctx, Document requestDoc, Element shipmentElement,
                                              List<Map<String, Object>> shippableItemInfo,
                                              BigDecimal maxWeight, BigDecimal minWeight, String totalWeightStr) {
        List<Map<String, BigDecimal>> packages = ShipmentWorker.getPackageSplit(dctx, shippableItemInfo, maxWeight);
        if (UtilValidate.isNotEmpty(packages)) {
            for (Map<String, BigDecimal> packageMap : packages) {
                addPackageElement(dctx, requestDoc, shipmentElement, shippableItemInfo, packageMap, minWeight);
            }
        } else {
            // Add a dummy package
            BigDecimal packageWeight = BigDecimal.ONE;
            try {
                packageWeight = new BigDecimal(totalWeightStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, MODULE);
            }
            Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", requestDoc);
            Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", requestDoc);
            UtilXml.addChildElementValue(packagingTypeElement, "Code", "00", requestDoc);
            Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", requestDoc);
            UtilXml.addChildElementValue(packageWeightElement, "Weight", "" + packageWeight, requestDoc);
        }
    }

    private static void addPackageElement(DispatchContext dctx, Document requestDoc, Element shipmentElement,
                                          List<Map<String, Object>> shippableItemInfo, Map<String, BigDecimal> packageMap, BigDecimal minWeight) {
        BigDecimal packageWeight = checkForDefaultPackageWeight(ShipmentWorker.calcPackageWeight(dctx, packageMap, shippableItemInfo,
                BigDecimal.ZERO), minWeight);
        Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Code", "00", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Description", "Unknown PackagingType", requestDoc);
        UtilXml.addChildElementValue(packageElement, "Description", "Package Description", requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, "Weight", packageWeight.toPlainString(), requestDoc);
        //If product is in shippable Package then it we should have one product per packagemap
        if (packageMap.size() == 1) {
            Iterator<String> i = packageMap.keySet().iterator();
            String productId = i.next();
            Map<String, Object> productInfo = ShipmentWorker.getProductItemInfo(shippableItemInfo, productId);
            if (productInfo.get("inShippingBox") != null && "Y".equalsIgnoreCase((String) productInfo.get("inShippingBox"))
                    && productInfo.get("shippingDepth") != null && productInfo.get("shippingWidth") != null
                    && productInfo.get("shippingHeight") != null) {
                Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Length", productInfo.get("shippingDepth").toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Width", productInfo.get("shippingWidth").toString(), requestDoc);
                UtilXml.addChildElementValue(dimensionsElement, "Height", productInfo.get("shippingHeight").toString(), requestDoc);
            }
        }
    }

    private static void addPackageElement(Document requestDoc, Element shipmentElement, BigDecimal packageWeight) {
        Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", requestDoc);
        Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Code", "00", requestDoc);
        UtilXml.addChildElementValue(packagingTypeElement, "Description", "Unknown PackagingType", requestDoc);
        UtilXml.addChildElementValue(packageElement, "Description", "Package Description", requestDoc);
        Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", requestDoc);
        UtilXml.addChildElementValue(packageWeightElement, "Weight", packageWeight.toString(), requestDoc);
    }


    private static BigDecimal checkForDefaultPackageWeight(BigDecimal weight, BigDecimal minWeight) {
        return (weight.compareTo(BigDecimal.ZERO) > 0 && weight.compareTo(minWeight) > 0 ? weight : minWeight);
    }

    public static Map<String, Object> handleUpsRateInquireResponse(Document rateResponseDocument, Locale locale) {
        // process TrackResponse, update data as needed
        Element rateResponseElement = rateResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if ("1".equals(responseStatusCode)) {
            List<? extends Element> rates = UtilXml.childElementList(rateResponseElement, "RatedShipment");
            Map<String, BigDecimal> rateMap = new HashMap<>();
            BigDecimal firstRate = null;
            if (UtilValidate.isEmpty(rates)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentRateNotAvailable", locale));
            } else {
                for (Element element : rates) {
                    // get service
                    Element service = UtilXml.firstChildElement(element, "Service");
                    String serviceCode = UtilXml.childElementValue(service, "Code");

                    // get total
                    Element totalCharges = UtilXml.firstChildElement(element, "TotalCharges");
                    String totalString = UtilXml.childElementValue(totalCharges, "MonetaryValue");

                    rateMap.put(serviceCode, new BigDecimal(totalString));
                    if (firstRate == null) {
                        firstRate = rateMap.get(serviceCode);
                    }
                }
            }

            Debug.logInfo("UPS Rate Map : " + rateMap, MODULE);

            Map<String, Object> resp = ServiceUtil.returnSuccess();
            resp.put("upsRateCodeMap", rateMap);
            resp.put("shippingEstimateAmount", firstRate);
            return resp;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorStatusCode",
                    UtilMisc.toMap("responseStatusCode", responseStatusCode), locale)));
            return ServiceUtil.returnFailure(errorList);
        }
    }

    public static Document createAccessRequestDocument(Delegator delegator, String shipmentGatewayConfigId,
                                                       String serviceConfigProps) {
        Document accessRequestDocument = UtilXml.makeEmptyXmlDocument("AccessRequest");
        Element accessRequestElement = accessRequestDocument.getDocumentElement();
        String accessLicenseNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessLicenseNumber", serviceConfigProps,
                "shipment.ups.access.license.number", "");
        String userId = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessUserId", serviceConfigProps, "shipment.ups.access"
                + ".user.id", "");
        String password = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "accessPassword", serviceConfigProps, "shipment.ups"
                + ".access.password", "");
        UtilXml.addChildElementValue(accessRequestElement, "AccessLicenseNumber", accessLicenseNumber, accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, "UserId", userId, accessRequestDocument);
        UtilXml.addChildElementValue(accessRequestElement, "Password", password, accessRequestDocument);
        return accessRequestDocument;
    }

    public static void handleErrors(Element responseElement, List<Object> errorList, Locale locale) {
        List<? extends Element> errorElements = UtilXml.childElementList(responseElement, "Error");
        for (Element errorElement : errorElements) {
            StringBuilder errorMessageBuf = new StringBuilder();

            String errorSeverity = UtilXml.childElementValue(errorElement, "ErrorSeverity");
            String errorCode = UtilXml.childElementValue(errorElement, "ErrorCode");
            String errorDescription = UtilXml.childElementValue(errorElement, "ErrorDescription");
            String minimumRetrySeconds = UtilXml.childElementValue(errorElement, "MinimumRetrySeconds");

            errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorMessage",
                    UtilMisc.toMap("errorCode", errorCode, "errorSeverity", errorSeverity, "errorDescription", errorDescription), locale));
            if (UtilValidate.isNotEmpty(minimumRetrySeconds)) {
                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorMessageMinimumRetrySeconds",
                        UtilMisc.toMap("minimumRetrySeconds", minimumRetrySeconds), locale));
            } else {
                errorMessageBuf.append(". ");
            }

            List<? extends Element> errorLocationElements = UtilXml.childElementList(errorElement, "ErrorLocation");
            for (Element errorLocationElement : errorLocationElements) {
                String errorLocationElementName = UtilXml.childElementValue(errorLocationElement, "ErrorLocationElementName");
                String errorLocationAttributeName = UtilXml.childElementValue(errorLocationElement, "ErrorLocationAttributeName");

                errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorWasAtElement",
                        UtilMisc.toMap("errorLocationElementName", errorLocationElementName), locale));

                if (UtilValidate.isNotEmpty(errorLocationAttributeName)) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorWasAtElementAttribute",
                            UtilMisc.toMap("errorLocationAttributeName", errorLocationAttributeName), locale));
                }

                List<? extends Element> errorDigestElements = UtilXml.childElementList(errorLocationElement, "ErrorDigest");
                for (Element errorDigestElement : errorDigestElements) {
                    errorMessageBuf.append(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorWasAtElementFullText",
                            UtilMisc.toMap("fullText", UtilXml.elementValue(errorDigestElement)), locale));
                }
            }

            errorList.add(errorMessageBuf.toString());
        }
    }

    /**
     * Opens a URL to UPS and makes a request.
     * @param upsService Name of the UPS service to invoke
     * @param xmlString  XML message to send
     * @return XML string response from UPS
     * @throws UpsConnectException
     */
    public static String sendUpsRequest(String upsService, String xmlString, String shipmentGatewayConfigId,
                                        String resource, Delegator delegator, Locale locale) throws UpsConnectException {

        // need a ups service to call
        if (upsService == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsServiceNameCannotBeNull", locale));
        }

        // xmlString should contain the auth document at the beginning
        // all documents require an <?xml version="1.0"?> header
        if (xmlString == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsXmlMessageCannotBeNull", locale));
        }

        String conStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectUrl", resource, "shipment.ups.connect.url");
        if (UtilValidate.isEmpty(conStr)) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsIncompleteConnectionURL", locale));
        }

        // prepare the connect string
        conStr = conStr.trim();
        if (!conStr.endsWith("/")) {
            conStr = conStr + "/";
        }
        conStr = conStr + upsService;

        String timeOutStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "connectTimeout", resource, "shipment.ups.connect"
                + ".timeout", "60");
        int timeout = 60;
        try {
            timeout = Integer.parseInt(timeOutStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeOutStr + " using default " + timeout);
        }

        HttpClient http = new HttpClient(conStr);
        http.setTimeout(timeout * 1000);
        http.setAllowUntrusted(true);
        String response = null;
        try {
            response = http.post(xmlString);
        } catch (HttpClientException e) {
            Debug.logError(e, "Problem connecting with UPS server [" + conStr + "]", MODULE);
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsURLConnectionProblem",
                    UtilMisc.toMap("exception", e), locale));
        }

        if (response == null) {
            throw new UpsConnectException(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsReceivedNullResponse", locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("UPS Response : " + response, MODULE);
        }

        return response;
    }

    public static Map<String, Object> upsRateInquireByPostalCode(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        // prepare the data
        String serviceConfigProps = (String) context.get("serviceConfigProps");
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String upsRateInquireMode = (String) context.get("upsRateInquireMode");
        String productStoreId = (String) context.get("productStoreId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String shippingPostalCode = (String) context.get("shippingPostalCode");
        String shippingCountryCode = (String) context.get("shippingCountryCode");
        List<BigDecimal> packageWeights = UtilGenerics.cast(context.get("packageWeights"));
        List<Map<String, Object>> shippableItemInfo = UtilGenerics.cast(context.get("shippableItemInfo"));
        String isResidentialAddress = (String) context.get("isResidentialAddress");

        // Important: DO NOT returnError here or you could trigger a transaction rollback and break other services.
        if (UtilValidate.isEmpty(shippingPostalCode)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsCannotRateEstimatePostalCodeMissing", locale));
        }

        if (serviceConfigProps == null) {
            serviceConfigProps = "shipment.properties";
        }
        if (upsRateInquireMode == null || !"Shop".equals(upsRateInquireMode)) {
            // can be either Rate || Shop
            Debug.logWarning("No upsRateInquireMode set, defaulting to 'Rate'", MODULE);
            upsRateInquireMode = "Rate";
        }

        // grab the pickup type; if none is defined we will assume daily pickup
        String pickupType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "shipperPickupType", serviceConfigProps, "shipment"
                + ".ups.shipper.pickup.type", "01");

        // if we're drop shipping from a supplier, then the address is given to us
        GenericValue shipFromAddress = (GenericValue) context.get("shipFromAddress");
        if (shipFromAddress == null) {

            // locate the ship-from address based on the product store's default facility
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            if (productStore != null && productStore.get("inventoryFacilityId") != null) {
                GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, productStore.getString(
                        "inventoryFacilityId"), UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
                if (facilityContactMech != null) {
                    try {
                        shipFromAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", facilityContactMech.getString(
                                "contactMechId")).queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        if (shipFromAddress == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUnableFoundShipToAddresss", locale));
        }

        // locate the service code
        String serviceCode = null;
        if (!"Shop".equals(upsRateInquireMode)) {
            // locate the CarrierShipmentMethod record
            GenericValue carrierShipmentMethod = null;
            try {
                carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                        .where("shipmentMethodTypeId", shipmentMethodTypeId, "partyId", carrierPartyId, "roleTypeId", carrierRoleTypeId)
                        .queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsUnableToLocateShippingMethodRequested",
                        locale));
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
        try {
            //If the warehouse you are shipping from its located in a country other than US, you need to supply its country code to UPS
            UtilXml.addChildElementValue(shipperAddrElement, "CountryCode", shipFromAddress.getRelatedOne("CountryGeo", true).getString("geoCode"),
                    rateRequestDoc);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // ship-to info - (sub of shipment)
        Element shiptoElement = UtilXml.addChildElement(shipmentElement, "ShipTo", rateRequestDoc);
        Element shiptoAddrElement = UtilXml.addChildElement(shiptoElement, "Address", rateRequestDoc);
        UtilXml.addChildElementValue(shiptoAddrElement, "PostalCode", shippingPostalCode, rateRequestDoc);
        if (shippingCountryCode != null && !"".equals(shippingCountryCode)) {
            UtilXml.addChildElementValue(shiptoAddrElement, "CountryCode", shippingCountryCode, rateRequestDoc);
        }

        if (isResidentialAddress != null && "Y".equals(isResidentialAddress)) {
            UtilXml.addChildElement(shiptoAddrElement, "ResidentialAddress", rateRequestDoc);
        }
        // requested service (code) - not used when in Shop mode
        if (serviceCode != null) {
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", rateRequestDoc);
            UtilXml.addChildElementValue(serviceElement, "Code", serviceCode, rateRequestDoc);
        }

        // package info
        String maxWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "maxEstimateWeight", serviceConfigProps, "shipment"
                + ".ups.max.estimate.weight", "99");
        BigDecimal maxWeight;
        try {
            maxWeight = new BigDecimal(maxWeightStr);
        } catch (NumberFormatException e) {
            maxWeight = new BigDecimal("99");
        }
        String minWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "minEstimateWeight", serviceConfigProps, "shipment"
                + ".ups.min.estimate.weight", ".1");
        BigDecimal minWeight;
        try {
            minWeight = new BigDecimal(minWeightStr);
        } catch (NumberFormatException e) {
            minWeight = new BigDecimal("0.1");
        }

        // Passing in a list of package weights overrides the calculation of same via shippableItemInfo
        if (UtilValidate.isEmpty(packageWeights)) {
            String totalWeightStr = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "minEstimateWeight", serviceConfigProps,
                    "shipment.ups.min.estimate.weight", "1");
            splitEstimatePackages(dctx, rateRequestDoc, shipmentElement, shippableItemInfo, maxWeight, minWeight, totalWeightStr);
        } else {
            for (BigDecimal packageWeight : packageWeights) {
                addPackageElement(rateRequestDoc, shipmentElement, packageWeight);
            }
        }

        // service options
        UtilXml.addChildElement(shipmentElement, "ShipmentServiceOptions", rateRequestDoc);

        String rateRequestString = null;
        try {
            rateRequestString = UtilXml.writeXmlDocument(rateRequestDoc);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the RatingServiceSelectionRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsErrorRatingServiceSelectionRequestXmlToString",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // create AccessRequest XML doc
        Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, serviceConfigProps);
        String accessRequestString = null;
        try {
            accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // prepare the access/inquire request string
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(accessRequestString);
        xmlString.append(rateRequestString);
        if (Debug.verboseOn()) {
            Debug.logVerbose(xmlString.toString(), MODULE);
        }
        // send the request
        String rateResponseString = null;
        try {
            rateResponseString = sendUpsRequest("Rate", xmlString.toString(), shipmentGatewayConfigId, serviceConfigProps, delegator, locale);
        } catch (UpsConnectException e) {
            String uceErrMsg = "Error sending UPS request for UPS Service Rate: " + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingRate",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
        Debug.logVerbose(rateResponseString, MODULE);
        Document rateResponseDocument = null;
        try {
            rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingRatingServiceSelectionResponse",
                    UtilMisc.toMap("errorString", e2.toString()), locale));
        }
        return handleUpsRateInquireResponse(rateResponseDocument, locale);
    }

    public static Map<String, Object> upsAddressValidation(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String city = (String) context.get("city");
        String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
        String postalCode = (String) context.get("postalCode");

        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String resource = (String) context.get("serviceConfigProps");

        if (UtilValidate.isEmpty(city) && UtilValidate.isEmpty(postalCode)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsAddressValidationRequireCityOrPostalCode", locale));
        }

        // prepare the XML Document
        Document avRequestDoc = UtilXml.makeEmptyXmlDocument("AddressValidationRequest");
        Element avRequestElement = avRequestDoc.getDocumentElement();
        avRequestElement.setAttribute("xml:lang", "en-US");

        // XML request header
        Element requestElement = UtilXml.addChildElement(avRequestElement, "Request", avRequestDoc);
        Element transactionReferenceElement = UtilXml.addChildElement(requestElement, "TransactionReference", avRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, "CustomerContext", "Rating and Service", avRequestDoc);
        UtilXml.addChildElementValue(transactionReferenceElement, "XpciVersion", "1.0001", avRequestDoc);
        UtilXml.addChildElementValue(requestElement, "RequestAction", "AV", avRequestDoc);

        // Address
        Element addressElement = UtilXml.addChildElement(avRequestElement, "Address", avRequestDoc);
        if (UtilValidate.isNotEmpty(city)) {
            UtilXml.addChildElementValue(addressElement, "City", city, avRequestDoc);
        }
        if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
            UtilXml.addChildElementValue(addressElement, "StateProvinceCode", stateProvinceGeoId, avRequestDoc);
        }
        if (UtilValidate.isNotEmpty(postalCode)) {
            UtilXml.addChildElementValue(addressElement, "PostalCode", postalCode, avRequestDoc);
        }

        String avRequestString = null;
        try {
            avRequestString = UtilXml.writeXmlDocument(avRequestDoc);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AddressValidationRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsErrorAddressValidationRequestXmlToString",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // create AccessRequest XML doc
        Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);

        String accessRequestString = null;
        try {
            accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
        } catch (IOException e) {
            String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
            Debug.logError(e, ioeErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // prepare the request string
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(accessRequestString);
        xmlString.append(avRequestString);
        Debug.logInfo(xmlString.toString(), MODULE);

        // send the request
        String avResponseString = null;
        try {
            avResponseString = sendUpsRequest("AV", xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
        } catch (UpsConnectException e) {
            String uceErrMsg = "Error sending UPS request: " + e.toString();
            Debug.logError(e, uceErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsErrorSendingAddressVerification",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Debug.logInfo(avResponseString, MODULE);

        Document avResponseDocument = null;
        try {
            avResponseDocument = UtilXml.readXmlDocument(avResponseString, false);
        } catch (SAXException | IOException | ParserConfigurationException e2) {
            String excErrMsg = "Error parsing the UPS response: " + e2.toString();
            Debug.logError(e2, excErrMsg, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "FacilityShipmentUpsErrorParsingAddressVerificationResponse",
                    UtilMisc.toMap("errorString", e2.toString()), locale));
        }

        return handleUpsAddressValidationResponse(avResponseDocument, locale);

    }

    public static Map<String, Object> handleUpsAddressValidationResponse(Document rateResponseDocument, Locale locale) {
        Element avResponseElement = rateResponseDocument.getDocumentElement();
        Element responseElement = UtilXml.firstChildElement(avResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");

        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);

        if ("1".equals(responseStatusCode)) {
            List<Map<String, String>> matches = new LinkedList<>();

            List<? extends Element> avResultList = UtilXml.childElementList(avResponseElement, "AddressValidationResult");
            // TODO: return error if there are no matches?
            if (UtilValidate.isNotEmpty(avResultList)) {
                for (Element avResultElement : avResultList) {
                    Map<String, String> match = new HashMap<>();

                    match.put("Rank", UtilXml.childElementValue(avResultElement, "Rank"));
                    match.put("Quality", UtilXml.childElementValue(avResultElement, "Quality"));

                    Element addressElement = UtilXml.firstChildElement(avResultElement, "Address");
                    match.put("City", UtilXml.childElementValue(addressElement, "City"));
                    match.put("StateProvinceCode", UtilXml.childElementValue(addressElement, "StateProvinceCode"));

                    match.put("PostalCodeLowEnd", UtilXml.childElementValue(avResultElement, "PostalCodeLowEnd"));
                    match.put("PostalCodeHighEnd", UtilXml.childElementValue(avResultElement, "PostalCodeHighEnd"));

                    matches.add(match);
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("matches", matches);
            return result;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorStatusCode",
                    UtilMisc.toMap("responseStatusCode", responseStatusCode), locale)));
            return ServiceUtil.returnError(errorList);
        }
    }

    public static Map<String, Object> upsEmailReturnLabel(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }
        boolean shipmentUpsSaveCertificationInfo = "true".equals(getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "saveCertInfo",
                resource, "shipment.ups.save.certification.info", "true"));
        String shipmentUpsSaveCertificationPath = FlexibleStringExpander.expandString(getShipmentGatewayConfigValue(delegator,
                shipmentGatewayConfigId, "saveCertPath", resource, "shipment.ups.save.certification.path", ""), context);
        File shipmentUpsSaveCertificationFile = null;
        if (shipmentUpsSaveCertificationInfo) {
            shipmentUpsSaveCertificationFile = new File(shipmentUpsSaveCertificationPath);
            if (!shipmentUpsSaveCertificationFile.exists()) {
                shipmentUpsSaveCertificationFile.mkdirs();
            }
        }

        //Shipment Confirm request
        String shipmentConfirmResponseString = null;

        try {
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentNotFoundId", locale) + " " + shipmentId);
            }
            GenericValue shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode"))
                    && !"001".equals(originTelecomNumber.getString("countryCode"))) {
                originPhoneNumber = originTelecomNumber.getString("countryCode") + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, "-", "");
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, " ", "");
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo", false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentDestPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            if (destTelecomNumber == null) {
                String missingErrMsg = "DestTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and "
                        + "shipmentRouteSegmentId " + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne("CountryGeo", false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentDestCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            GenericValue carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                    .where("partyId", shipmentRouteSegment.get("carrierPartyId"), "roleTypeId", "CARRIER", "shipmentMethodTypeId",
                            shipmentRouteSegment.get("shipmentMethodTypeId"))
                    .queryOne();
            if (carrierShipmentMethod == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentCarrierShipmentMethodNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "carrierPartyId",
                                shipmentRouteSegment.get("carrierPartyId"), "shipmentMethodTypeId", shipmentRouteSegment.get("shipmentMethodTypeId")),
                        locale));
            }

            Map<String, Object> destEmail = dispatcher.runSync("getPartyEmail", UtilMisc.toMap("partyId", shipment.get("partyIdTo"), "userLogin",
                    userLogin));
            if (ServiceUtil.isError(destEmail)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(destEmail));
            }
            String recipientEmail = null;
            if (UtilValidate.isNotEmpty(destEmail.get("emailAddress"))) {
                recipientEmail = (String) destEmail.get("emailAddress");
            }
            String senderEmail = null;
            Map<String, Object> originEmail = dispatcher.runSync("getPartyEmail", UtilMisc.toMap("partyId", shipment.get("partyIdFrom"),
                    "userLogin", userLogin));
            if (ServiceUtil.isError(originEmail)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(originEmail));
            }
            if (UtilValidate.isNotEmpty(originEmail.get("emailAddress"))) {
                senderEmail = (String) originEmail.get("emailAddress");
            }

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList(
                    "+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Okay, start putting the XML together...
            Document shipmentConfirmRequestDoc = UtilXml.makeEmptyXmlDocument("ShipmentConfirmRequest");
            Element shipmentConfirmRequestElement = shipmentConfirmRequestDoc.getDocumentElement();
            shipmentConfirmRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element requestElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Request", shipmentConfirmRequestDoc);

            UtilXml.addChildElementValue(requestElement, "RequestAction", "ShipConfirm", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(requestElement, "RequestOption", "nonvalidate", shipmentConfirmRequestDoc);

            // Top Level Element: Shipment
            Element shipmentElement = UtilXml.addChildElement(shipmentConfirmRequestElement, "Shipment", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipmentElement, "Description", "Goods for Shipment " + shipment.get("shipmentId"),
                    shipmentConfirmRequestDoc);

            // Child of Shipment: ReturnService
            Element returnServiceElement = UtilXml.addChildElement(shipmentElement, "ReturnService", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(returnServiceElement, "Code", String.valueOf(RET_SERVICE_CODE), shipmentConfirmRequestDoc);

            // Child of Shipment: Shipper
            String shipperNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "shipperNumber", resource, "shipment.ups"
                    + ".shipper.number", "");
            Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "Name", UtilValidate.isNotEmpty(originPostalAddress.getString("toName"))
                    ? originPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "AttentionName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "PhoneNumber", originPhoneNumber, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "ShipperNumber", shipperNumber, shipmentConfirmRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "AddressLine1", originPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipperAddressElement, "AddressLine2", originPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipperAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, "ShipTo", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "CompanyName", UtilValidate.isNotEmpty(destPostalAddress.getString("toName"))
                    ? destPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "AttentionName", UtilValidate.isNotEmpty(destPostalAddress.getString("attnName"))
                    ? destPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, "PhoneNumber", destPhoneNumber, shipmentConfirmRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "AddressLine1", destPostalAddress.getString("address1"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipToAddressElement, "AddressLine2", destPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipToAddressElement, "City", destPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "StateProvinceCode", destPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "PostalCode", destPostalAddress.getString("postalCode"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "CountryCode", destCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("homeDeliveryType"))) {
                UtilXml.addChildElement(shipToAddressElement, "ResidentialAddress", shipmentConfirmRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, "ShipFrom", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "CompanyName", UtilValidate.isNotEmpty(originPostalAddress.getString("toName"))
                    ? originPostalAddress.getString("toName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "AttentionName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "PhoneNumber", originPhoneNumber, shipmentConfirmRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, "Address", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine1", originPostalAddress.getString("address1"),
                    shipmentConfirmRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine2", originPostalAddress.getString("address2"),
                        shipmentConfirmRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, "City", originPostalAddress.getString("city"), shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "PostalCode", originPostalAddress.getString("postalCode"),
                    shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: PaymentInformation
            Element paymentInformationElement = UtilXml.addChildElement(shipmentElement, "PaymentInformation", shipmentConfirmRequestDoc);

            String thirdPartyAccountNumber = shipmentRouteSegment.getString("thirdPartyAccountNumber");

            if (UtilValidate.isEmpty(thirdPartyAccountNumber)) {
                // Paid by shipper
                Element prepaidElement = UtilXml.addChildElement(paymentInformationElement, "Prepaid", shipmentConfirmRequestDoc);
                Element billShipperElement = UtilXml.addChildElement(prepaidElement, "BillShipper", shipmentConfirmRequestDoc);

                // fill in BillShipper AccountNumber element from properties file
                String billShipperAccountNumber = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "billShipperAccountNumber",
                        resource, "shipment.ups.bill.shipper.account.number", "");
                UtilXml.addChildElementValue(billShipperElement, "AccountNumber", billShipperAccountNumber, shipmentConfirmRequestDoc);
            }

            // Child of Shipment: Service
            Element serviceElement = UtilXml.addChildElement(shipmentElement, "Service", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(serviceElement, "Code", carrierShipmentMethod.getString("carrierServiceCode"), shipmentConfirmRequestDoc);

            // Child of Shipment: ShipmentServiceOptions
            String defaultReturnLabelMemo = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "defaultReturnLabelMemo", resource,
                    "shipment.ups.default.returnLabel.memo", "");
            String defaultReturnLabelSubject = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "defaultReturnLabelSubject",
                    resource, "shipment.ups.default.returnLabel.subject", "");
            Element shipmentServiceOptionsElement = UtilXml.addChildElement(shipmentElement, "ShipmentServiceOptions", shipmentConfirmRequestDoc);
            Element labelDeliveryElement = UtilXml.addChildElement(shipmentServiceOptionsElement, "LabelDelivery", shipmentConfirmRequestDoc);
            Element emailMessageElement = UtilXml.addChildElement(labelDeliveryElement, "EMailMessage", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, "EMailAddress", recipientEmail, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, "FromEMailAddress", senderEmail, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, "FromName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, "Memo", defaultReturnLabelMemo, shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(emailMessageElement, "Subject", defaultReturnLabelSubject, shipmentConfirmRequestDoc);

            // Child of Shipment: Package
            Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", shipmentConfirmRequestDoc);
            Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packagingTypeElement, "Code", "02", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageElement, "Description", "Package Description", shipmentConfirmRequestDoc);
            Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", shipmentConfirmRequestDoc);
            Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, "UnitOfMeasurement", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", "LBS", shipmentConfirmRequestDoc);
            UtilXml.addChildElementValue(packageWeightElement, "Weight", EntityUtilProperties.getPropertyValue("shipment", "shipment.default.weight"
                    + ".value", delegator), shipmentConfirmRequestDoc);

            String shipmentConfirmRequestString = null;
            try {
                shipmentConfirmRequestString = UtilXml.writeXmlDocument(shipmentConfirmRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentConfirmRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorShipmentConfirmRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            // send ShipmentConfirmRequest String
            // get ShipmentConfirmResponse String back
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(shipmentConfirmRequestString);
            try {
                shipmentConfirmResponseString = sendUpsRequest("ShipConfirm", xmlString.toString(), shipmentGatewayConfigId, resource, delegator,
                        locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipConfirm: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingShipConfirm",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            Document shipmentConfirmResponseDocument = null;
            try {
                shipmentConfirmResponseDocument = UtilXml.readXmlDocument(shipmentConfirmResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the ShipmentConfirmResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingShipmentConfirm",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }
            Element shipmentConfirmResponseElement = shipmentConfirmResponseDocument.getDocumentElement();
            // handle Response element info
            Element responseElement = UtilXml.firstChildElement(shipmentConfirmResponseElement, "Response");
            String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
            List<Object> errorList = new LinkedList<>();
            UpsServices.handleErrors(responseElement, errorList, locale);
            if (!"1".equals(responseStatusCode)) {
                errorList.add(0, UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsShipmentConfirmFailedForReturnShippingLabel", locale));
                return ServiceUtil.returnError(errorList);
            }

            //Shipment Accept Request follows
            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            String shipmentDigest = UtilXml.childElementValue(shipmentConfirmResponseElement, "ShipmentDigest");
            if (UtilValidate.isEmpty(shipmentDigest)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsTrackingDigestWasNotSet", locale));
            }

            Document shipmentAcceptRequestDoc = UtilXml.makeEmptyXmlDocument("ShipmentAcceptRequest");
            Element shipmentAcceptRequestElement = shipmentAcceptRequestDoc.getDocumentElement();
            shipmentAcceptRequestElement.setAttribute("xml:lang", "en-US");

            // Top Level Element: Request
            Element acceptRequestElement = UtilXml.addChildElement(shipmentAcceptRequestElement, "Request", shipmentAcceptRequestDoc);

            Element acceptTransactionReferenceElement = UtilXml.addChildElement(acceptRequestElement, "TransactionReference",
                    shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptTransactionReferenceElement, "CustomerContext", "ShipAccept / 01", shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptTransactionReferenceElement, "XpciVersion", "1.0001", shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(acceptRequestElement, "RequestAction", "ShipAccept", shipmentAcceptRequestDoc);
            UtilXml.addChildElementValue(acceptRequestElement, "RequestOption", "01", shipmentAcceptRequestDoc);

            UtilXml.addChildElementValue(shipmentAcceptRequestElement, "ShipmentDigest", shipmentDigest, shipmentAcceptRequestDoc);

            String shipmentAcceptRequestString = null;
            try {
                shipmentAcceptRequestString = UtilXml.writeXmlDocument(shipmentAcceptRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the ShipmentAcceptRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsErrorShipmentAcceptRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document acceptAccessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String acceptAccessRequestString = null;
            try {
                acceptAccessRequestString = UtilXml.writeXmlDocument(acceptAccessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // connect to UPS server, send AccessRequest to auth
            StringBuilder acceptXmlString = new StringBuilder();
            acceptXmlString.append(acceptAccessRequestString);
            acceptXmlString.append(shipmentAcceptRequestString);

            if (shipmentUpsSaveCertificationInfo) {
                String outFileName =
                        shipmentUpsSaveCertificationPath + "/UpsShipmentAcceptRequest" + shipmentId + "_" + shipmentRouteSegment.getString(
                         "shipmentRouteSegmentId") + ".xml";
                try (FileOutputStream fileOut = new FileOutputStream(outFileName)) {
                    fileOut.write(xmlString.toString().getBytes(StandardCharsets.UTF_8));
                    fileOut.flush();
                } catch (IOException e) {
                    Debug.logInfo(e, "Could not save UPS XML file: [[[" + xmlString.toString() + "]]] to file: " + outFileName, MODULE);
                }
            }
            try {
                sendUpsRequest("ShipAccept", acceptXmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service ShipAccept: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingShipAccept",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentAccept",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentConfirm",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage("OrderUiLabels", "OrderReturnLabelEmailSuccessful", locale));
    }

    public static Map<String, Object> upsShipmentAlternateRatesInquiry(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        // prepare the data
        String upsRateInquireMode;
        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        Locale locale = (Locale) context.get("locale");
        String rateResponseString = null;
        String productStoreId = (String) context.get("productStoreId");
        List<Map<String, Object>> shippingRates = new LinkedList<>();
        GenericValue shipmentRouteSegment = null;
        Map<String, Object> shipmentGatewayConfig = ShipmentServices.getShipmentGatewayConfigFromShipment(delegator, shipmentId, locale);
        String shipmentGatewayConfigId = (String) shipmentGatewayConfig.get("shipmentGatewayConfigId");
        String resource = (String) shipmentGatewayConfig.get("configProps");
        if (UtilValidate.isEmpty(shipmentGatewayConfigId) && UtilValidate.isEmpty(resource)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsGatewayNotAvailable", locale));
        }

        try {
            if (shipmentRouteSegmentId != null) {
                shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where("shipmentId", shipmentId,
                        "shipmentRouteSegmentId", shipmentRouteSegmentId).queryOne();
            } else {
                shipmentRouteSegment = EntityQuery.use(delegator).from("ShipmentRouteSegment").where(EntityCondition.makeCondition("shipmentId",
                        EntityOperator.EQUALS, shipmentId)).queryFirst();
            }

            if (shipmentRouteSegment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "ProductShipmentRouteSegmentNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            shipmentRouteSegmentId = shipmentRouteSegment.getString("shipmentRouteSegmentId");

            if (!"UPS".equals(shipmentRouteSegment.getString("carrierPartyId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNotRouteSegmentCarrier", UtilMisc.toMap(
                        "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentId", shipmentId), locale));
            }

            // Get Origin Info
            GenericValue originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            if (originPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            GenericValue originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            if (originTelecomNumber == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginTelecomNumberNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            String originPhoneNumber = originTelecomNumber.getString("areaCode") + originTelecomNumber.getString("contactNumber");
            // don't put on country code if not specified or is the US country code (UPS wants it this way)
            if (UtilValidate.isNotEmpty(originTelecomNumber.getString("countryCode"))
                    && !"001".equals(originTelecomNumber.getString("countryCode"))) {
                originPhoneNumber = originTelecomNumber.getString("countryCode") + originPhoneNumber;
            }
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, "-", "");
            originPhoneNumber = StringUtil.replaceString(originPhoneNumber, " ", "");
            // lookup the two letter country code (in the geoCode field)
            GenericValue originCountryGeo = originPostalAddress.getRelatedOne("CountryGeo", false);
            if (originCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentOriginCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // Get Dest Info
            GenericValue destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            if (destPostalAddress == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentDestPostalAddressNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            GenericValue destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            if (destTelecomNumber == null) {
                String missingErrMsg = "DestTelecomNumber not found for ShipmentRouteSegment with shipmentId " + shipmentId + " and "
                        + "shipmentRouteSegmentId " + shipmentRouteSegmentId;
                Debug.logError(missingErrMsg, MODULE);
                // for now we won't require the dest phone number, but is it required?
            }
            String destPhoneNumber = null;
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

            // lookup the two letter country code (in the geoCode field)
            GenericValue destCountryGeo = destPostalAddress.getRelatedOne("CountryGeo", false);
            if (destCountryGeo == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsRouteSegmentDestCountryGeoNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }

            // grab the pickup type; if none is defined we will assume daily pickup
            String pickupType = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "shipperPickupType", resource, "shipment.ups"
                    + ".shipper.pickup.type", "01");

            // grab the customer classification; if none is defined we will assume daily pickup
            String customerClassification = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, "customerClassification", resource,
                    "shipment.ups.customerclassification", "01");

            // should be shop to get estimates for all the possible shipping method of UPS
            upsRateInquireMode = "Shop";

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

            Element customerClassificationElement = UtilXml.addChildElement(rateRequestElement, "CustomerClassification", rateRequestDoc);
            UtilXml.addChildElementValue(customerClassificationElement, "Code", customerClassification, rateRequestDoc);

            // shipment info
            Element shipmentElement = UtilXml.addChildElement(rateRequestElement, "Shipment", rateRequestDoc);
            Element shipperElement = UtilXml.addChildElement(shipmentElement, "Shipper", rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "Name", UtilValidate.isNotEmpty(originPostalAddress.getString("toName"))
                    ? originPostalAddress.getString("toName") : "", rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "AttentionName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "PhoneNumber", originPhoneNumber, rateRequestDoc);
            UtilXml.addChildElementValue(shipperElement, "ShipperNumber", EntityUtilProperties.getPropertyValue("shipment", "shipment.ups.shipper"
                    + ".number", delegator), rateRequestDoc);

            Element shipperAddressElement = UtilXml.addChildElement(shipperElement, "Address", rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "AddressLine1", originPostalAddress.getString("address1"), rateRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipperAddressElement, "AddressLine2", originPostalAddress.getString("address2"), rateRequestDoc);
            }

            UtilXml.addChildElementValue(shipperAddressElement, "City", originPostalAddress.getString("city"), rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), rateRequestDoc);
            UtilXml.addChildElementValue(shipperAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), rateRequestDoc);

            // Child of Shipment: ShipTo
            Element shipToElement = UtilXml.addChildElement(shipmentElement, "ShipTo", rateRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "CompanyName", UtilValidate.isNotEmpty(destPostalAddress.getString("toName"))
                    ? destPostalAddress.getString("toName") : "", rateRequestDoc);
            UtilXml.addChildElementValue(shipToElement, "AttentionName", UtilValidate.isNotEmpty(destPostalAddress.getString("attnName"))
                    ? destPostalAddress.getString("attnName") : "", rateRequestDoc);
            if (UtilValidate.isNotEmpty(destPhoneNumber)) {
                UtilXml.addChildElementValue(shipToElement, "PhoneNumber", destPhoneNumber, rateRequestDoc);
            }
            Element shipToAddressElement = UtilXml.addChildElement(shipToElement, "Address", rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "AddressLine1", destPostalAddress.getString("address1"), rateRequestDoc);
            if (UtilValidate.isNotEmpty(destPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipToAddressElement, "AddressLine2", destPostalAddress.getString("address2"), rateRequestDoc);
            }

            UtilXml.addChildElementValue(shipToAddressElement, "City", destPostalAddress.getString("city"), rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "StateProvinceCode", destPostalAddress.getString("stateProvinceGeoId"),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "PostalCode", destPostalAddress.getString("postalCode"), rateRequestDoc);
            UtilXml.addChildElementValue(shipToAddressElement, "CountryCode", destCountryGeo.getString("geoCode"), rateRequestDoc);
            if (UtilValidate.isNotEmpty(shipmentRouteSegment.getString("homeDeliveryType"))) {
                UtilXml.addChildElement(shipToAddressElement, "ResidentialAddress", rateRequestDoc);
            }

            // Child of Shipment: ShipFrom
            Element shipFromElement = UtilXml.addChildElement(shipmentElement, "ShipFrom", rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "CompanyName", UtilValidate.isNotEmpty(originPostalAddress.getString("toName"))
                    ? originPostalAddress.getString("toName") : "", rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "AttentionName", UtilValidate.isNotEmpty(originPostalAddress.getString("attnName"))
                    ? originPostalAddress.getString("attnName") : "", rateRequestDoc);
            UtilXml.addChildElementValue(shipFromElement, "PhoneNumber", originPhoneNumber, rateRequestDoc);
            Element shipFromAddressElement = UtilXml.addChildElement(shipFromElement, "Address", rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine1", originPostalAddress.getString("address1"), rateRequestDoc);
            if (UtilValidate.isNotEmpty(originPostalAddress.getString("address2"))) {
                UtilXml.addChildElementValue(shipFromAddressElement, "AddressLine2", originPostalAddress.getString("address2"), rateRequestDoc);
            }
            UtilXml.addChildElementValue(shipFromAddressElement, "City", originPostalAddress.getString("city"), rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "StateProvinceCode", originPostalAddress.getString("stateProvinceGeoId"),
                    rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "PostalCode", originPostalAddress.getString("postalCode"), rateRequestDoc);
            UtilXml.addChildElementValue(shipFromAddressElement, "CountryCode", originCountryGeo.getString("geoCode"), rateRequestDoc);

            List<GenericValue> shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, UtilMisc.toList(
                    "+shipmentPackageSeqId"), false);
            if (UtilValidate.isEmpty(shipmentPackageRouteSegs)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsPackageRouteSegsNotFound",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId), locale));
            }
            for (GenericValue shipmentPackageRouteSeg : shipmentPackageRouteSegs) {

                GenericValue shipmentPackage = shipmentPackageRouteSeg.getRelatedOne("ShipmentPackage", false);
                GenericValue shipmentBoxType = shipmentPackage.getRelatedOne("ShipmentBoxType", false);
                List<GenericValue> carrierShipmentBoxTypes = shipmentPackage.getRelated("CarrierShipmentBoxType", UtilMisc.toMap("partyId", "UPS"),
                        null, false);
                GenericValue carrierShipmentBoxType = null;
                if (!carrierShipmentBoxTypes.isEmpty()) {
                    carrierShipmentBoxType = carrierShipmentBoxTypes.get(0);
                }

                Element packageElement = UtilXml.addChildElement(shipmentElement, "Package", rateRequestDoc);
                Element packagingTypeElement = UtilXml.addChildElement(packageElement, "PackagingType", rateRequestDoc);
                if (carrierShipmentBoxType != null && carrierShipmentBoxType.get("packagingTypeCode") != null) {
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", carrierShipmentBoxType.getString("packagingTypeCode"), rateRequestDoc);
                } else {
                    // default to "02", plain old Package
                    UtilXml.addChildElementValue(packagingTypeElement, "Code", "02", rateRequestDoc);
                }
                if (shipmentBoxType != null) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", rateRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, "UnitOfMeasurement", rateRequestDoc);
                    GenericValue dimensionUom = shipmentBoxType.getRelatedOne("DimensionUom", false);
                    if (dimensionUom != null) {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code",
                                dimensionUom.getString("abbreviation").toUpperCase(Locale.getDefault()), rateRequestDoc);
                    } else {
                        UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", ModelService.IN_PARAM, rateRequestDoc);
                    }
                    BigDecimal boxLength = shipmentBoxType.getBigDecimal("boxLength");
                    BigDecimal boxWidth = shipmentBoxType.getBigDecimal("boxWidth");
                    BigDecimal boxHeight = shipmentBoxType.getBigDecimal("boxHeight");
                    UtilXml.addChildElementValue(dimensionsElement, "Length", UtilValidate.isNotEmpty(boxLength) ? "" + boxLength.intValue() : "",
                            rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Width", UtilValidate.isNotEmpty(boxWidth) ? "" + boxWidth.intValue() : "",
                            rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Height", UtilValidate.isNotEmpty(boxHeight) ? "" + boxHeight.intValue() : "",
                            rateRequestDoc);
                } else if (UtilValidate.isNotEmpty(shipmentPackage.get("boxLength"))
                        && UtilValidate.isNotEmpty(shipmentPackage.get("boxWidth"))
                        && UtilValidate.isNotEmpty(shipmentPackage.get("boxHeight"))) {
                    Element dimensionsElement = UtilXml.addChildElement(packageElement, "Dimensions", rateRequestDoc);
                    Element unitOfMeasurementElement = UtilXml.addChildElement(dimensionsElement, "UnitOfMeasurement", rateRequestDoc);
                    UtilXml.addChildElementValue(unitOfMeasurementElement, "Code", ModelService.IN_PARAM, rateRequestDoc);
                    BigDecimal length = (BigDecimal) shipmentPackage.get("boxLength");
                    BigDecimal width = (BigDecimal) shipmentPackage.get("boxWidth");
                    BigDecimal height = (BigDecimal) shipmentPackage.get("boxHeight");
                    UtilXml.addChildElementValue(dimensionsElement, "Length", length.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Width", width.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                    UtilXml.addChildElementValue(dimensionsElement, "Height", height.setScale(DECIMALS, ROUNDING).toString(), rateRequestDoc);
                }

                Element packageWeightElement = UtilXml.addChildElement(packageElement, "PackageWeight", rateRequestDoc);
                Element packageWeightUnitOfMeasurementElement = UtilXml.addChildElement(packageElement, "UnitOfMeasurement", rateRequestDoc);
                String weightUomUps = OFBIZ_TO_UPS.get(shipmentPackage.get("weightUomId"));
                if (weightUomUps != null) {
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", weightUomUps, rateRequestDoc);
                } else {
                    // might as well default to LBS
                    UtilXml.addChildElementValue(packageWeightUnitOfMeasurementElement, "Code", "LBS", rateRequestDoc);
                }

                if (shipmentPackage.getString("weight") == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsWeightValueNotFound",
                            UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentPackageSeqId",
                                    shipmentPackage.getString("shipmentPackageSeqId")), locale));
                }
                BigDecimal boxWeight = shipmentPackage.getBigDecimal("weight");
                UtilXml.addChildElementValue(packageWeightElement, "Weight", UtilValidate.isNotEmpty(boxWeight) ? "" + boxWeight.intValue() : "",
                        rateRequestDoc);
            }

            // service options
            UtilXml.addChildElement(shipmentElement, "ShipmentServiceOptions", rateRequestDoc);
            String rateRequestString = null;
            try {
                rateRequestString = UtilXml.writeXmlDocument(rateRequestDoc);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the RatingServiceSelectionRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsErrorRatingServiceSelectionRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // create AccessRequest XML doc
            Document accessRequestDocument = createAccessRequestDocument(delegator, shipmentGatewayConfigId, resource);
            String accessRequestString = null;
            try {
                accessRequestString = UtilXml.writeXmlDocument(accessRequestDocument);
            } catch (IOException e) {
                String ioeErrMsg = "Error writing the AccessRequest XML Document to a String: " + e.toString();
                Debug.logError(e, ioeErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR,
                        "FacilityShipmentUpsErrorAccessRequestXmlToString",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }

            // prepare the access/inquire request string
            StringBuilder xmlString = new StringBuilder();
            xmlString.append(accessRequestString);
            xmlString.append(rateRequestString);
            if (Debug.verboseOn()) {
                Debug.logVerbose(xmlString.toString(), MODULE);
            }
            // send the request
            try {
                rateResponseString = sendUpsRequest("Rate", xmlString.toString(), shipmentGatewayConfigId, resource, delegator, locale);
            } catch (UpsConnectException e) {
                String uceErrMsg = "Error sending UPS request for UPS Service Rate: " + e.toString();
                Debug.logError(e, uceErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorSendingRate",
                        UtilMisc.toMap("errorString", e.toString()), locale));
            }
            Debug.logVerbose(rateResponseString, MODULE);
            Document rateResponseDocument = null;
            try {
                rateResponseDocument = UtilXml.readXmlDocument(rateResponseString, false);
            } catch (SAXException | IOException | ParserConfigurationException e2) {
                String excErrMsg = "Error parsing the RatingServiceSelectionResponse: " + e2.toString();
                Debug.logError(e2, excErrMsg, MODULE);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorParsingRatingServiceSelectionResponse",
                        UtilMisc.toMap("errorString", e2.toString()), locale));
            }
            Map<String, Object> upsResponse = handleUpsAlternateRatesInquireResponse(rateResponseDocument, locale);
            Map<String, BigDecimal> upsRateCodeMap = UtilGenerics.cast(upsResponse.get("upsRateCodeMap"));
            GenericValue carrierShipmentMethod = null;
            // Filtering out rates of shipping methods which are not configured in ProductStoreShipmentMeth entity.
            try {
                List<GenericValue> productStoreShipmentMethods = EntityQuery.use(delegator).from("ProductStoreShipmentMethView").where(
                        "productStoreId", productStoreId).queryList();
                for (GenericValue productStoreShipmentMethod : productStoreShipmentMethods) {
                    if ("UPS".equals(productStoreShipmentMethod.get("partyId"))) {
                        Map<String, Object> thisUpsRateCodeMap = new HashMap<>();
                        carrierShipmentMethod = EntityQuery.use(delegator).from("CarrierShipmentMethod")
                                .where("shipmentMethodTypeId", productStoreShipmentMethod.getString("shipmentMethodTypeId"), "partyId",
                                        productStoreShipmentMethod.getString("partyId"), "roleTypeId",
                                        productStoreShipmentMethod.getString("roleTypeId"))
                                .queryOne();
                        String serviceCode = carrierShipmentMethod.getString("carrierServiceCode");
                        for (String thisServiceCode : upsRateCodeMap.keySet()) {
                            if (serviceCode.equals(thisServiceCode)) {
                                BigDecimal newRate = upsRateCodeMap.get(serviceCode);
                                thisUpsRateCodeMap.put(serviceCode, newRate);
                                shippingRates.add(thisUpsRateCodeMap);
                            }
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            return UtilMisc.toMap("shippingRates", shippingRates,
                    ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorDataShipmentAlternateRate",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }

    public static Map<String, Object> handleUpsAlternateRatesInquireResponse(Document rateResponseDocument, Locale locale) {
        Element rateResponseElement = rateResponseDocument.getDocumentElement();

        // handle Response element info
        Element responseElement = UtilXml.firstChildElement(rateResponseElement, "Response");
        String responseStatusCode = UtilXml.childElementValue(responseElement, "ResponseStatusCode");
        List<Object> errorList = new LinkedList<>();
        UpsServices.handleErrors(responseElement, errorList, locale);
        String totalRates = null;

        if ("1".equals(responseStatusCode)) {
            List<? extends Element> rates = UtilXml.childElementList(rateResponseElement, "RatedShipment");
            Map<String, BigDecimal> rateMap = new HashMap<>();
            if (UtilValidate.isEmpty(rates)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsNoRateAvailable", locale));
            } else {
                for (Element element : rates) {
                    // get service
                    Element service = UtilXml.firstChildElement(element, "Service");
                    String serviceCode = UtilXml.childElementValue(service, "Code");

                    // get negotiated rates
                    Element negotiatedRates = UtilXml.firstChildElement(element, "NegotiatedRates");
                    if (negotiatedRates != null) {
                        Element netSummaryCharges = UtilXml.firstChildElement(negotiatedRates, "NetSummaryCharges");
                        Element grandTotal = UtilXml.firstChildElement(netSummaryCharges, "GrandTotal");
                        totalRates = UtilXml.childElementValue(grandTotal, "MonetaryValue");
                    } else {
                        // get total rates
                        Element totalCharges = UtilXml.firstChildElement(element, "TotalCharges");
                        totalRates = UtilXml.childElementValue(totalCharges, "MonetaryValue");
                    }
                    rateMap.put(serviceCode, new BigDecimal(totalRates));
                }
            }
            Debug.logInfo("UPS Rate Map : " + rateMap, MODULE);
            Map<String, Object> resp = ServiceUtil.returnSuccess();
            resp.put("upsRateCodeMap", rateMap);
            return resp;
        } else {
            errorList.add(ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "FacilityShipmentUpsErrorStatusCode",
                    UtilMisc.toMap("responseStatusCode", responseStatusCode), locale)));
            return ServiceUtil.returnFailure(errorList);
        }
    }

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
                                                        String shipmentGatewayConfigParameterName,
                                                        String resource, String parameterName) {
        String returnValue = "";
        if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
            try {
                GenericValue ups =
                        EntityQuery.use(delegator).from("ShipmentGatewayUps").where("shipmentGatewayConfigId", shipmentGatewayConfigId).queryOne();
                if (UtilValidate.isNotEmpty(ups)) {
                    Object upsField = ups.get(shipmentGatewayConfigParameterName);
                    if (upsField != null) {
                        returnValue = upsField.toString().trim();
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

    private static String getShipmentGatewayConfigValue(Delegator delegator, String shipmentGatewayConfigId,
                                                        String shipmentGatewayConfigParameterName,
                                                        String resource, String parameterName, String defaultValue) {
        String returnValue = getShipmentGatewayConfigValue(delegator, shipmentGatewayConfigId, shipmentGatewayConfigParameterName, resource,
                parameterName);
        if (UtilValidate.isEmpty(returnValue)) {
            returnValue = defaultValue;
        }
        return returnValue;
    }
}

@SuppressWarnings("serial")
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

=======================================
Address Validation Request/Response
=======================================

<AddressValidationRequest xml:lang="en-US">
    <Request>
        <TransactionReference>
            <CustomerContext>Maryam Dennis-Customer Data</CustomerContext>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <RequestAction>AV</RequestAction>
    </Request>
    <Address>
        <City>MIAMI</City>
        <StateProvinceCode>FL</StateProvinceCode>
    </Address>
</AddressValidationRequest>

=======================================

<AddressValidationResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <AddressValidationResult>
        <Rank>1</Rank>
        <Quality>1.0</Quality>
        <Address>
            <City>TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
</AddressValidationResponse>

=======================================

<AddressValidationResponse>
    <Response>
        <TransactionReference>
            <XpciVersion>1.0001</XpciVersion>
        </TransactionReference>
        <ResponseStatusCode>1</ResponseStatusCode>
        <ResponseStatusDescription>Success</ResponseStatusDescription>
    </Response>
    <AddressValidationResult>
        <Rank>1</Rank>
        <Quality>0.9975000023841858</Quality>
        <Address>
            <City>TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
    <AddressValidationResult>
        <Rank>2</Rank>
        <Quality>0.8299999833106995</Quality>
        <Address>
            <City>LUTHERVILLE TIMONIUM</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
    <AddressValidationResult>
        <Rank>3</Rank>
        <Quality>0.8299999833106995</Quality>
        <Address>
            <City>LUTHERVILLE</City>
            <StateProvinceCode>MD</StateProvinceCode>
        </Address>
        <PostalCodeLowEnd>21093</PostalCodeLowEnd>
        <PostalCodeHighEnd>21094</PostalCodeHighEnd>
    </AddressValidationResult>
</AddressValidationResponse>

 */
