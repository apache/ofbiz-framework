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
package org.apache.ofbiz.order.shoppingcart.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.uom.UomWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ShippingEvents - Events used for processing shipping fees
 */
public class ShippingEvents {

    private static final String MODULE = ShippingEvents.class.getName();
    private static final List<String> FIELD_NAME_GEO_IDS = UtilMisc.toList("countryGeoId", "countyGeoId", "stateProvinceGeoId", "municipalityGeoId",
            "postalCodeGeoId");

    public static String getShipEstimate(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = (ShoppingCart) request.getSession().getAttribute("shoppingCart");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        int shipGroups = cart.getShipGroupSize();
        for (int i = 0; i < shipGroups; i++) {
            String shipmentMethodTypeId = cart.getShipmentMethodTypeId(i);
            if (UtilValidate.isEmpty(shipmentMethodTypeId)) {
                continue;
            }
            Map<String, Object> result = getShipGroupEstimate(dispatcher, delegator, cart, i);
            ServiceUtil.getMessages(request, result, null, "", "", "", "", null, null);
            if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return "error";
            }

            BigDecimal shippingTotal = (BigDecimal) result.get("shippingTotal");
            if (shippingTotal == null) {
                shippingTotal = BigDecimal.ZERO;
            }
            cart.setItemShipGroupEstimate(shippingTotal, i);
        }

        ProductPromoWorker.doPromotions(cart, dispatcher);
        // all done
        return "success";
    }

    public static Map<String, Object> getShipGroupEstimate(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart, int groupNo) {
        // check for shippable items
        if (!cart.shippingApplies()) {
            Map<String, Object> responseResult = ServiceUtil.returnSuccess();
            responseResult.put("shippingTotal", BigDecimal.ZERO);
            return responseResult;
        }

        String shipmentMethodTypeId = cart.getShipmentMethodTypeId(groupNo);
        String carrierPartyId = cart.getCarrierPartyId(groupNo);
        String productStoreShipMethId = cart.getProductStoreShipMethId(groupNo);

        return getShipGroupEstimate(dispatcher, delegator, cart.getOrderType(), shipmentMethodTypeId, carrierPartyId, null,
                cart.getShippingContactMechId(groupNo), cart.getProductStoreId(), cart.getSupplierPartyId(groupNo),
                cart.getShippableItemInfo(groupNo),
                cart.getShippableWeight(groupNo), cart.getShippableQuantity(groupNo), cart.getShippableTotal(groupNo), cart.getPartyId(),
                productStoreShipMethId);
    }

    public static Map<String, Object> getShipEstimate(LocalDispatcher dispatcher, Delegator delegator, OrderReadHelper orh, String shipGroupSeqId) {
        // check for shippable items
        if (!orh.shippingApplies()) {
            Map<String, Object> responseResult = ServiceUtil.returnSuccess();
            responseResult.put("shippingTotal", BigDecimal.ZERO);
            return responseResult;
        }

        GenericValue shipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
        String shipmentMethodTypeId = shipGroup.getString("shipmentMethodTypeId");
        String carrierRoleTypeId = shipGroup.getString("carrierRoleTypeId");
        String carrierPartyId = shipGroup.getString("carrierPartyId");
        String supplierPartyId = shipGroup.getString("supplierPartyId");

        GenericValue shipAddr = orh.getShippingAddress(shipGroupSeqId);
        if (shipAddr == null) {
            return UtilMisc.<String, Object>toMap("shippingTotal", BigDecimal.ZERO);
        }

        String contactMechId = shipAddr.getString("contactMechId");
        String partyId = null;
        GenericValue partyObject = orh.getPlacingParty();
        if (partyObject != null) {
            partyId = partyObject.getString("partyId");
        }
        return getShipGroupEstimate(dispatcher, delegator, orh.getOrderTypeId(), shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId,
                contactMechId, orh.getProductStoreId(), supplierPartyId, orh.getShippableItemInfo(shipGroupSeqId),
                orh.getShippableWeight(shipGroupSeqId),
                orh.getShippableQuantity(shipGroupSeqId), orh.getShippableTotal(shipGroupSeqId), partyId, null);
    }

    // version with no support for using the supplier's address as the origin
    public static Map<String, Object> getShipGroupEstimate(LocalDispatcher dispatcher, Delegator delegator, String orderTypeId,
                                                           String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId,
                                                           String shippingContactMechId,
                                                           String productStoreId, List<Map<String, Object>> itemInfo, BigDecimal shippableWeight,
                                                           BigDecimal shippableQuantity,
                                                           BigDecimal shippableTotal, String partyId, String productStoreShipMethId) {
        return getShipGroupEstimate(dispatcher, delegator, orderTypeId, shipmentMethodTypeId, carrierPartyId,
                carrierRoleTypeId, shippingContactMechId, productStoreId, null, itemInfo,
                shippableWeight, shippableQuantity, shippableTotal, partyId, productStoreShipMethId);
    }

    public static Map<String, Object> getShipGroupEstimate(LocalDispatcher dispatcher, Delegator delegator, String orderTypeId,
                                                           String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId,
                                                           String shippingContactMechId,
                                                           String productStoreId, String supplierPartyId, List<Map<String, Object>> itemInfo,
                                                           BigDecimal shippableWeight, BigDecimal shippableQuantity,
                                                           BigDecimal shippableTotal, String partyId, String productStoreShipMethId) {
        return getShipGroupEstimate(dispatcher, delegator, orderTypeId,
                shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, shippingContactMechId,
                productStoreId, supplierPartyId, itemInfo, shippableWeight, shippableQuantity,
                shippableTotal, partyId, productStoreShipMethId, BigDecimal.ZERO);
    }

    public static Map<String, Object> getShipGroupEstimate(LocalDispatcher dispatcher, Delegator delegator, String orderTypeId,
                                                           String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId,
                                                           String shippingContactMechId,
                                                           String productStoreId, String supplierPartyId, List<Map<String, Object>> itemInfo,
                                                           BigDecimal shippableWeight, BigDecimal shippableQuantity,
                                                           BigDecimal shippableTotal, String partyId, String productStoreShipMethId,
                                                           BigDecimal totalAllowance) {
        String standardMessage = "A problem occurred calculating shipping. Fees will be calculated offline.";
        List<String> errorMessageList = new LinkedList<>();

        if ("NO_SHIPPING".equals(shipmentMethodTypeId)) {
            return ServiceUtil.returnSuccess();
        }

        if (shipmentMethodTypeId == null || carrierPartyId == null) {
            if ("SALES_ORDER".equals(orderTypeId)) {
                errorMessageList.add("Please Select Your Shipping Method.");
                return ServiceUtil.returnError(errorMessageList);
            } else {
                return ServiceUtil.returnSuccess();
            }
        }

        if (carrierRoleTypeId == null) {
            carrierRoleTypeId = "CARRIER";
        }

        // if as supplier is associated, then we have a drop shipment and should use the origin shipment address of it
        String shippingOriginContactMechId = null;
        if (supplierPartyId != null) {
            try {
                GenericValue originAddress = getShippingOriginContactMech(delegator, supplierPartyId);
                if (originAddress == null) {
                    return ServiceUtil.returnError("Cannot find the origin shipping address (SHIP_ORIG_LOCATION) for the supplier with ID ["
                            + supplierPartyId + "].  Will not be able to calculate drop shipment estimate.");
                }
                shippingOriginContactMechId = originAddress.getString("contactMechId");
            } catch (GeneralException e) {
                return ServiceUtil.returnError(standardMessage);
            }
        }

        // no shippable items; we won't change any shipping at all
        if (shippableQuantity.compareTo(BigDecimal.ZERO) == 0) {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("shippingTotal", BigDecimal.ZERO);
            return result;
        }

        // check for an external service call
        GenericValue storeShipMethod = ProductStoreWorker.getProductStoreShipmentMethod(delegator, productStoreId,
                shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId);

        if (storeShipMethod == null) {
            errorMessageList.add("No applicable shipment method found.");
            return ServiceUtil.returnError(errorMessageList);
        }

        // the initial amount before manual estimates
        BigDecimal shippingTotal = BigDecimal.ZERO;

        // prepare the service invocation fields
        Map<String, Object> serviceFields = new HashMap<>();
        serviceFields.put("initialEstimateAmt", shippingTotal);
        serviceFields.put("shippableTotal", shippableTotal);
        serviceFields.put("shippableQuantity", shippableQuantity);
        serviceFields.put("shippableWeight", shippableWeight);
        serviceFields.put("shippableItemInfo", itemInfo);
        serviceFields.put("productStoreId", productStoreId);
        serviceFields.put("carrierRoleTypeId", "CARRIER");
        serviceFields.put("carrierPartyId", carrierPartyId);
        serviceFields.put("shipmentMethodTypeId", shipmentMethodTypeId);
        serviceFields.put("shippingContactMechId", shippingContactMechId);
        serviceFields.put("shippingOriginContactMechId", shippingOriginContactMechId);
        serviceFields.put("partyId", partyId);
        serviceFields.put("productStoreShipMethId", productStoreShipMethId);

        // call the external shipping service
        try {
            BigDecimal externalAmt = null;
            if (UtilValidate.isNotEmpty(shippingContactMechId)) {
                externalAmt = getExternalShipEstimate(dispatcher, storeShipMethod, serviceFields);
            }
            if (externalAmt != null) {
                shippingTotal = shippingTotal.add(externalAmt);
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(standardMessage);
        }

        // update the initial amount
        serviceFields.put("initialEstimateAmt", shippingTotal);

        // call the generic estimate service
        try {
            BigDecimal genericAmt = getGenericShipEstimate(dispatcher, storeShipMethod, serviceFields);
            if (genericAmt != null) {
                shippingTotal = shippingTotal.add(genericAmt);
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(standardMessage);
        }

        // Calculate the allowance price(Already included in Product's default/list price)
        // using shippingAllowance percent and deduct it from Actual Shipping Cost.
        if (BigDecimal.ZERO.compareTo(shippingTotal) < 0 && UtilValidate.isNotEmpty(totalAllowance)
                && BigDecimal.ZERO.compareTo(totalAllowance) < 0) {
            BigDecimal shippingAllowancePercent = storeShipMethod.getBigDecimal("allowancePercent") != null ? storeShipMethod.getBigDecimal(
                    "allowancePercent") : BigDecimal.ZERO;
            totalAllowance = totalAllowance.multiply(shippingAllowancePercent.divide(BigDecimal.valueOf(100)));
            shippingTotal = shippingTotal.subtract(totalAllowance);
        }

        // Check if minimum price is set for any Shipping Option, if yes,
        // compare it with total shipping and use greater of the two.
        BigDecimal minimumPrice = storeShipMethod.getBigDecimal("minimumPrice");
        if (UtilValidate.isNotEmpty(minimumPrice) && shippingTotal.compareTo(minimumPrice) < 0) {
            shippingTotal = minimumPrice;
        }

        // return the totals
        Map<String, Object> responseResult = ServiceUtil.returnSuccess();
        responseResult.put("shippingTotal", shippingTotal);
        return responseResult;
    }

    public static BigDecimal getGenericShipEstimate(LocalDispatcher dispatcher, GenericValue storeShipMeth, Map<String, ? extends Object> context)
            throws GeneralException {
        // invoke the generic estimate service next -- append to estimate amount
        Map<String, Object> genericEstimate = null;
        BigDecimal genericShipAmt = null;
        try {
            genericEstimate = dispatcher.runSync("calcShipmentCostEstimate", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Shipment Service Error", MODULE);
            throw new GeneralException();
        }
        if (ServiceUtil.isError(genericEstimate) || ServiceUtil.isFailure(genericEstimate)) {
            Debug.logError(ServiceUtil.getErrorMessage(genericEstimate), MODULE);
            throw new GeneralException();
        } else if (ServiceUtil.isFailure(genericEstimate)) {
            genericShipAmt = BigDecimal.ONE.negate();
        } else {
            genericShipAmt = (BigDecimal) genericEstimate.get("shippingEstimateAmount");
        }
        return genericShipAmt;
    }

    public static String getShipmentCustomMethod(Delegator delegator, String shipmentCustomMethodId) {
        String serviceName = null;
        GenericValue customMethod = null;
        try {
            customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", shipmentCustomMethodId).queryOne();
            if (customMethod != null) {
                serviceName = customMethod.getString("customMethodName");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return serviceName;
    }

    public static BigDecimal getExternalShipEstimate(LocalDispatcher dispatcher, GenericValue storeShipMeth, Map<String, Object> context)
            throws GeneralException {
        String shipmentCustomMethodId = storeShipMeth.getString("shipmentCustomMethodId");
        Delegator delegator = dispatcher.getDelegator();
        String serviceName = "";
        if (UtilValidate.isNotEmpty(shipmentCustomMethodId)) {
            serviceName = getShipmentCustomMethod(dispatcher.getDelegator(), shipmentCustomMethodId);
        }
        if (UtilValidate.isEmpty(serviceName)) {
            serviceName = storeShipMeth.getString("serviceName");
        }
        // invoke the external shipping estimate service
        BigDecimal externalShipAmt = null;
        if (serviceName != null) {
            String doEstimates = EntityUtilProperties.getPropertyValue("shipment", "shipment.doratecheck", "true", delegator);
            //If all estimates are not turned off, check for the individual one
            if ("true".equals(doEstimates)) {
                String dothisEstimate = EntityUtilProperties.getPropertyValue("shipment", "shipment.doratecheck." + serviceName, "true", delegator);
                if ("false".equals(dothisEstimate)) {
                    serviceName = null;
                }
            } else {
                //Rate checks inhibited
                serviceName = null;
            }
        }
        if (serviceName != null) {
            String shipmentGatewayConfigId = storeShipMeth.getString("shipmentGatewayConfigId");
            String configProps = storeShipMeth.getString("configProps");
            if (UtilValidate.isNotEmpty(serviceName)) {
                // prepare the external service context
                context.put("serviceConfigProps", configProps);
                context.put("shipmentCustomMethodId", shipmentCustomMethodId);
                context.put("shipmentGatewayConfigId", shipmentGatewayConfigId);

                // invoke the service
                Map<String, Object> serviceResp = null;
                try {
                    Debug.logInfo("Service : " + serviceName + " / shipmentGatewayConfigId : " + shipmentGatewayConfigId + " / configProps : "
                            + configProps + " -- " + context, MODULE);
                    // because we don't want to blow up too big or rollback the transaction when this happens, always have it run in its own
                    // transaction...
                    serviceResp = dispatcher.runSync(serviceName, context, 0, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Shipment Service Error", MODULE);
                    throw new GeneralException(e);
                }
                if (ServiceUtil.isError(serviceResp)) {
                    String errMsg = "Error getting external shipment cost estimate: " + ServiceUtil.getErrorMessage(serviceResp);
                    Debug.logError(errMsg, MODULE);
                    throw new GeneralException(errMsg);
                } else if (ServiceUtil.isFailure(serviceResp)) {
                    String errMsg = "Failure getting external shipment cost estimate: " + ServiceUtil.getErrorMessage(serviceResp);
                    Debug.logError(errMsg, MODULE);
                    // should not throw an Exception here, otherwise getShipGroupEstimate would return an error, causing all sorts of services like
                    // add or update order item to abort
                } else {
                    externalShipAmt = (BigDecimal) serviceResp.get("shippingEstimateAmount");
                }
            }
        }
        return externalShipAmt;
    }

    /**
     * Attempts to get the supplier's shipping origin address and failing that, the general location.
     */
    public static GenericValue getShippingOriginContactMech(Delegator delegator, String supplierPartyId) throws GeneralException {
        List<EntityCondition> conditions = UtilMisc.toList(
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId),
                EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"),
                EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.IN, UtilMisc.toList("SHIP_ORIG_LOCATION",
                        "GENERAL_LOCATION")),
                EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate"),
                EntityUtil.getFilterByDateExpr("purposeFromDate", "purposeThruDate"));
        EntityConditionList<EntityCondition> ecl = EntityCondition.makeCondition(conditions, EntityOperator.AND);

        List<GenericValue> addresses = delegator.findList("PartyContactWithPurpose", ecl, null, UtilMisc.toList("contactMechPurposeTypeId DESC"),
                null, false);

        GenericValue generalAddress = null;
        GenericValue originAddress = null;
        for (GenericValue address : addresses) {
            if ("GENERAL_LOCATION".equals(address.get("contactMechPurposeTypeId"))) {
                generalAddress = address;
            } else if ("SHIP_ORIG_LOCATION".equals(address.get("contactMechPurposeTypeId"))) {
                originAddress = address;
            }
        }
        return originAddress != null ? originAddress : generalAddress;
    }

    public static GenericValue getShippingOriginContactMechFromFacility(Delegator delegator, String facilityId) throws GeneralException {
        GenericValue address = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION"));
        if (address != null) return address;
        return ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("GENERAL_LOCATION"));
    }

    private static List<String> getGeoIdFromPostalContactMech(Delegator delegator, GenericValue address) {
        List<String> geoIds = new ArrayList<>();
        if (address != null) {
            GenericValue addressGV = null;
            if ("PostalAddress".equals(address.getEntityName())) {
                addressGV = address;
            } else {
                try {
                    addressGV = EntityQuery.use(delegator).from("PostalAddress").where(address).cache().queryOne();
                } catch (GeneralException e) {
                    Debug.logError(e.toString(), MODULE);
                }
            }
            if (addressGV != null) {
                GenericValue finalAddressGV = addressGV;
                geoIds = FIELD_NAME_GEO_IDS.stream()
                        .filter(key -> finalAddressGV.get(key) != null)
                        .map(key -> finalAddressGV.getString(key))
                        .collect(Collectors.toList());
            }
        }
        return geoIds;
    }

    public static List<GenericValue> getShipmentTimeEstimates(Delegator delegator, String shipmentMethodTypeId,
                                                              String partyId, String roleTypeId, GenericValue shippingAddress,
                                                              GenericValue originAddress) {
        //Retrieve origin Geo
        List<String> geoIdFroms = getGeoIdFromPostalContactMech(delegator, originAddress);
        //Retrieve destination Geo
        List<String> geoIdTos = getGeoIdFromPostalContactMech(delegator, shippingAddress);
        return getShipmentTimeEstimates(delegator, shipmentMethodTypeId, partyId, roleTypeId, geoIdFroms, geoIdTos);
    }


    public static List<GenericValue> getShipmentTimeEstimates(Delegator delegator, String shipmentMethodTypeId,
                                                              String partyId, String roleTypeId, List<String> geoIdFroms, List<String> geoIdTos) {

        List<GenericValue> shippingTimeEstimates = new LinkedList<>();
        if ("NO_SHIPPING".equals(shipmentMethodTypeId)) {
            return shippingTimeEstimates;
        }

        List<EntityCondition> conditionList = new ArrayList<>();
        if (UtilValidate.isNotEmpty(shipmentMethodTypeId)) {
            conditionList.add(EntityCondition.makeCondition("shipmentMethodTypeId", shipmentMethodTypeId));
        }
        if (UtilValidate.isNotEmpty(partyId)) {
            conditionList.add(EntityCondition.makeCondition("partyId", partyId));
        }
        if (UtilValidate.isNotEmpty(roleTypeId)) {
            conditionList.add(EntityCondition.makeCondition("roleTypeId", roleTypeId));
        }
        List<EntityCondition> geoConditionList = new ArrayList<>();
        if (geoIdFroms == null) geoIdFroms = new ArrayList<>();
        if (geoIdTos == null) geoIdTos = new ArrayList<>();
        geoConditionList.add(EntityCondition.makeCondition("geoIdFrom", EntityOperator.IN, geoIdFroms));
        geoConditionList.add(EntityCondition.makeCondition("geoIdTo", EntityOperator.IN, geoIdTos));
        geoConditionList.addAll(conditionList);
        EntityCondition condition = EntityCondition.makeCondition(geoConditionList);

        try {
            shippingTimeEstimates = EntityQuery.use(delegator)
                    .from("ShipmentTimeEstimate")
                    .where(condition)
                    .filterByDate()
                    .orderBy("sequenceNumber")
                    .cache()
                    .queryList();
            if (UtilValidate.isEmpty(shippingTimeEstimates)) {
                geoIdFroms.add("_NA_");
                geoIdTos.add("_NA_");
                geoConditionList = new ArrayList<>();
                geoConditionList.add(EntityCondition.makeCondition("geoIdFrom", EntityOperator.IN, geoIdFroms));
                geoConditionList.add(EntityCondition.makeCondition("geoIdTo", EntityOperator.IN, geoIdTos));
                geoConditionList.addAll(conditionList);
                condition = EntityCondition.makeCondition(geoConditionList);
                shippingTimeEstimates = EntityQuery.use(delegator)
                        .from("ShipmentTimeEstimate")
                        .where(condition)
                        .filterByDate()
                        .cache()
                        .orderBy("geoIdFrom", "geoIdTo")
                        .queryList();
            }
        } catch (GenericEntityException e) {
            String errMsg = "Failure getting shipment time estimate: " + e.getLocalizedMessage();
            Debug.logError(errMsg, MODULE);
        }

        return shippingTimeEstimates;
    }

    /**
     * Return the {@link GenericValue} ShipmentTimeEstimate matching the carrier shipment method
     *
     * @param storeCarrierShipMethod ShipmentMethod used for estimation
     * @param shippingTimeEstimates  available configured estimation
     * @return
     */
    public static GenericValue getShippingTimeEstimate(GenericValue storeCarrierShipMethod, List<GenericValue> shippingTimeEstimates) {
        if (shippingTimeEstimates == null) return null;
        List<String> pkFields = storeCarrierShipMethod.getDelegator().getModelEntity("CarrierShipmentMethod").getPkFieldNames();
        Optional<GenericValue> shippingTimeEstimate = shippingTimeEstimates.stream()
                .filter(shippingTimeEstimateAnalyze ->
                        pkFields.stream()
                                .allMatch(k ->
                                        storeCarrierShipMethod.getString(k).equals(
                                                shippingTimeEstimateAnalyze.getString(k))))
                .findFirst();
        return shippingTimeEstimate.orElse(null);
    }

    /**
     * Return the number of days estimated for shipping
     *
     * @param dispatcher
     * @param storeCarrierShipMethod ShipmentMethod used for estimation
     * @param shippingTimeEstimates  available configured estimation
     * @return
     */
    public static Double getShippingTimeEstimateInDay(LocalDispatcher dispatcher, GenericValue storeCarrierShipMethod,
                                                      List<GenericValue> shippingTimeEstimates) {
        GenericValue shippingTimeEstimate = getShippingTimeEstimate(storeCarrierShipMethod, shippingTimeEstimates);
        if (shippingTimeEstimate == null) return null;
        BigDecimal leadTimeConverted = UomWorker.convertUom(shippingTimeEstimate.getBigDecimal("leadTime"),
                shippingTimeEstimate.getString("leadTimeUomId"), "TF_day", dispatcher);
        return leadTimeConverted != null ? leadTimeConverted.setScale(2, RoundingMode.UP).doubleValue() : null;
    }
}

