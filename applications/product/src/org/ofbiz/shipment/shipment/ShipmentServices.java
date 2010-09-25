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
package org.ofbiz.shipment.shipment;

import java.util.*;
import java.math.BigDecimal;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.common.geo.GeoWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.party.party.PartyWorker;

/**
 * ShipmentServices
 */
public class ShipmentServices {

    public static final String module = ShipmentServices.class.getName();

    public static final String resource = "ProductUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";
    public static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    public static Map<String, Object> createShipmentEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> storeAll = FastList.newInstance();

        String productStoreShipMethId = (String)context.get("productStoreShipMethId");

        GenericValue productStoreShipMeth = null;
        try {
            productStoreShipMeth = delegator.findByPrimaryKey("ProductStoreShipmentMeth", UtilMisc.toMap("productStoreShipMethId", productStoreShipMethId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem retrieving ProductStoreShipmentMeth entry with id [" + productStoreShipMethId + "]: " + e.toString());
        }


        // Create the basic entity.
        GenericValue estimate = delegator.makeValue("ShipmentCostEstimate");

        estimate.set("shipmentCostEstimateId", delegator.getNextSeqId("ShipmentCostEstimate"));
        estimate.set("productStoreShipMethId", productStoreShipMethId);
        estimate.set("shipmentMethodTypeId", productStoreShipMeth.getString("shipmentMethodTypeId"));
        estimate.set("carrierPartyId", productStoreShipMeth.getString("partyId"));
        estimate.set("carrierRoleTypeId", "CARRIER");
        estimate.set("productStoreId", productStoreShipMeth.getString("productStoreId"));
        estimate.set("geoIdTo", context.get("toGeo"));
        estimate.set("geoIdFrom", context.get("fromGeo"));
        estimate.set("partyId", context.get("partyId"));
        estimate.set("roleTypeId", context.get("roleTypeId"));
        estimate.set("orderPricePercent", context.get("flatPercent"));
        estimate.set("orderFlatPrice", context.get("flatPrice"));
        estimate.set("orderItemFlatPrice", context.get("flatItemPrice"));
        estimate.set("shippingPricePercent", context.get("shippingPricePercent"));
        estimate.set("productFeatureGroupId", context.get("productFeatureGroupId"));
        estimate.set("oversizeUnit", context.get("oversizeUnit"));
        estimate.set("oversizePrice", context.get("oversizePrice"));
        estimate.set("featurePercent", context.get("featurePercent"));
        estimate.set("featurePrice", context.get("featurePrice"));
        estimate.set("weightBreakId", context.get("weightBreakId"));
        estimate.set("weightUnitPrice", context.get("wprice"));
        estimate.set("weightUomId", context.get("wuom"));
        estimate.set("quantityBreakId", context.get("quantityBreakId"));
        estimate.set("quantityUnitPrice", context.get("qprice"));
        estimate.set("quantityUomId", context.get("quom"));
        estimate.set("priceBreakId", context.get("priceBreakId"));
        estimate.set("priceUnitPrice", context.get("pprice"));
        estimate.set("priceUomId", context.get("puom"));
        storeAll.add(estimate);

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "w", "weight", "Weight")) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "q", "quantity", "Quantity")) {
            return result;
        }

        if (!applyQuantityBreak(context, result, storeAll, delegator, estimate, "p", "price", "Price")) {
            return result;
        }

        try {
            delegator.storeAll(storeAll);
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "Problem reading product features: " + e.toString());
            return result;
        }

        result.put("shipmentCostEstimateId", estimate.get("shipmentCostEstimateId"));
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> removeShipmentEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentCostEstimateId = (String) context.get("shipmentCostEstimateId");

        GenericValue estimate = null;

        try {
            estimate = delegator.findByPrimaryKey("ShipmentCostEstimate", UtilMisc.toMap("shipmentCostEstimateId", shipmentCostEstimateId));
            estimate.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Problem removing entity or related entities (" + e.toString() + ")");
        }
        /* Commented out this code because QuantityBreak may be used by other records
        try {
            if (estimate.get("weightBreakId") != null) {
                delegator.removeRelated("WeightQuantityBreak", estimate);
            }
        } catch (GenericEntityException e) {
            Debug.logInfo("Not removing WeightQuantityBreak records related to ShipmentCostEstimate [" + shipmentCostEstimateId + "] because they are used by other entities.", module);
        }
        try {
            if (estimate.get("quantityBreakId") != null) {
                delegator.removeRelated("QuantityQuantityBreak", estimate);
            }
        } catch (GenericEntityException e) {
            Debug.logInfo("Not removing QuantityQuantityBreak records related to ShipmentCostEstimate [" + shipmentCostEstimateId + "] because they are used by other entities.", module);
        }
        try {
            if (estimate.get("priceBreakId") != null) {
                delegator.removeRelated("PriceQuantityBreak", estimate);
            }
        } catch (GenericEntityException e) {
            Debug.logInfo("Not removing PriceQuantityBreak records related to ShipmentCostEstimate [" + shipmentCostEstimateId + "] because they are used by other entities.", module);
        }
        */
        return ServiceUtil.returnSuccess();
    }

    private static boolean applyQuantityBreak(Map<String, ? extends Object> context, Map<String, Object> result, List<GenericValue> storeAll, Delegator delegator,
                                              GenericValue estimate, String prefix, String breakType, String breakTypeString) {
        BigDecimal min = (BigDecimal) context.get(prefix + "min");
        BigDecimal max = (BigDecimal) context.get(prefix + "max");
        if (min != null || max != null) {
            if (min != null && max != null) {
                if (min.compareTo(max) <= 0 || max.compareTo(BigDecimal.ZERO) == 0) {
                    try {
                        String newSeqId = delegator.getNextSeqId("QuantityBreak");
                        GenericValue weightBreak = delegator.makeValue("QuantityBreak");
                        weightBreak.set("quantityBreakId", newSeqId);
                        weightBreak.set("quantityBreakTypeId", "SHIP_" + breakType.toUpperCase());
                        weightBreak.set("fromQuantity", min);
                        weightBreak.set("thruQuantity", max);
                        estimate.set(breakType + "BreakId", newSeqId);
                        estimate.set(breakType + "UnitPrice", context.get(prefix + "price"));
                        if (context.containsKey(prefix + "uom")) {
                            estimate.set(breakType + "UomId", context.get(prefix + "uom"));
                        }
                        storeAll.add(0, weightBreak);
                    }
                    catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
                else {
                    result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    result.put(ModelService.ERROR_MESSAGE, "Max " + breakTypeString +
                            " must not be less than Min " + breakTypeString + ".");
                    return false;
                }
            }
            else {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, breakTypeString+" Span Requires BOTH Fields.");
                return false;
            }
        }
        return true;
    }

    // ShippingEstimate Calc Service
    public static Map<String, Object> calcShipmentCostEstimate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        // prepare the data
        String productStoreShipMethId = (String) context.get("productStoreShipMethId");
        String productStoreId = (String) context.get("productStoreId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shippingPostalCode = (String) context.get("shippingPostalCode");
        String shippingCountryCode = (String) context.get("shippingCountryCode");

        List<Map<String, Object>> shippableItemInfo = UtilGenerics.checkList(context.get("shippableItemInfo"));
        //Map shippableFeatureMap = (Map) context.get("shippableFeatureMap");
        //List shippableItemSizes = (List) context.get("shippableItemSizes");

        BigDecimal shippableTotal = (BigDecimal) context.get("shippableTotal");
        BigDecimal shippableQuantity = (BigDecimal) context.get("shippableQuantity");
        BigDecimal shippableWeight = (BigDecimal) context.get("shippableWeight");
        BigDecimal initialEstimateAmt = (BigDecimal) context.get("initialEstimateAmt");

        if (shippableTotal == null) {
            shippableTotal = BigDecimal.ZERO;
        }
        if (shippableQuantity == null) {
            shippableQuantity = BigDecimal.ZERO;
        }
        if (shippableWeight == null) {
            shippableWeight = BigDecimal.ZERO;
        }
        if (initialEstimateAmt == null) {
            initialEstimateAmt = BigDecimal.ZERO;
        }

        // get the ShipmentCostEstimate(s)
        Map<String, String> estFields = UtilMisc.toMap("productStoreId", productStoreId, "shipmentMethodTypeId", shipmentMethodTypeId,
                "carrierPartyId", carrierPartyId, "carrierRoleTypeId", carrierRoleTypeId);
        EntityCondition estFieldsCond = EntityCondition.makeCondition(estFields, EntityOperator.AND);

        if (UtilValidate.isNotEmpty(productStoreShipMethId)) {
            // if the productStoreShipMethId field is passed, then also get estimates that have the field set
            List<EntityCondition> condList = UtilMisc.toList(EntityCondition.makeCondition("productStoreShipMethId", EntityOperator.EQUALS, productStoreShipMethId), estFieldsCond);
            estFieldsCond = EntityCondition.makeCondition(condList, EntityOperator.AND);
        }

        Collection<GenericValue> estimates = null;
        try {
            estimates = delegator.findList("ShipmentCostEstimate", estFieldsCond, null, null, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Unable to locate estimates from database");
        }
        if (estimates == null || estimates.size() < 1) {
            if (initialEstimateAmt.compareTo(BigDecimal.ZERO) == 0) {
                Debug.logWarning("Using the passed context : " + context, module);
                Debug.logWarning("No shipping estimates found; the shipping amount returned is 0!", module);
            }

            Map<String, Object> respNow = ServiceUtil.returnSuccess();
            respNow.put("shippingEstimateAmount", BigDecimal.ZERO);
            return respNow;
        }

        // Get the PostalAddress
        GenericValue shipAddress = null;
        try {
            shipAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnFailure("Cannot get shipping address entity");
        }

        if (shippingContactMechId != null) {
            try {
                shipAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", shippingContactMechId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError("Cannot get shipping address entity");
            }
        } else if (shippingPostalCode != null) {
            String countryGeoId = null;
            try {
                EntityCondition cond =EntityCondition.makeCondition(UtilMisc.toMap("geoTypeId", "COUNTRY", "geoCode", shippingCountryCode));
                GenericValue countryGeo = EntityUtil.getFirst(delegator.findList("Geo", cond, null, null, null, true));
                if (countryGeo != null) {
                    countryGeoId = countryGeo.getString("geoId");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            shipAddress = delegator.makeValue("PostalAddress");
            shipAddress.set("countryGeoId", countryGeoId);
            shipAddress.set("postalCodeGeoId", shippingPostalCode);
        }
        // Get the possible estimates.
        List<GenericValue> estimateList = FastList.newInstance();

        for (GenericValue thisEstimate: estimates) {
            String toGeo = thisEstimate.getString("geoIdTo");
            if (UtilValidate.isNotEmpty(toGeo) && shipAddress ==null) {
                // This estimate requires shipping address details. We don't have it so we cannot use this estimate.
                continue;
            }
            List<GenericValue> toGeoList = GeoWorker.expandGeoGroup(toGeo, delegator);
            // Make sure we have a valid GEOID.
            if (UtilValidate.isEmpty(toGeoList) ||
                    GeoWorker.containsGeo(toGeoList, shipAddress.getString("countryGeoId"), delegator) ||
                    GeoWorker.containsGeo(toGeoList, shipAddress.getString("stateProvinceGeoId"), delegator) ||
                    GeoWorker.containsGeo(toGeoList, shipAddress.getString("postalCodeGeoId"), delegator)) {

                /*
                if (toGeo == null || toGeo.equals("") || toGeo.equals(shipAddress.getString("countryGeoId")) ||
                toGeo.equals(shipAddress.getString("stateProvinceGeoId")) ||
                toGeo.equals(shipAddress.getString("postalCodeGeoId"))) {
                 */

                GenericValue wv = null;
                GenericValue qv = null;
                GenericValue pv = null;

                try {
                    wv = thisEstimate.getRelatedOne("WeightQuantityBreak");
                } catch (GenericEntityException e) {
                }
                try {
                    qv = thisEstimate.getRelatedOne("QuantityQuantityBreak");
                } catch (GenericEntityException e) {
                }
                try {
                    pv = thisEstimate.getRelatedOne("PriceQuantityBreak");
                } catch (GenericEntityException e) {
                }
                if (wv == null && qv == null && pv == null) {
                    estimateList.add(thisEstimate);
                } else {
                    // Do some testing.
                    boolean useWeight = false;
                    boolean weightValid = false;
                    boolean useQty = false;
                    boolean qtyValid = false;
                    boolean usePrice = false;
                    boolean priceValid = false;

                    if (wv != null) {
                        useWeight = true;
                        BigDecimal min = BigDecimal.ONE.movePointLeft(4);
                        BigDecimal max = BigDecimal.ONE.movePointLeft(4);

                        try {
                            min = wv.getBigDecimal("fromQuantity");
                            max = wv.getBigDecimal("thruQuantity");
                        } catch (Exception e) {
                        }
                        if (shippableWeight.compareTo(min) >= 0 && (max.compareTo(BigDecimal.ZERO) == 0 || shippableWeight.compareTo(max) <= 0)) {
                            weightValid = true;
                        }
                    }
                    if (qv != null) {
                        useQty = true;
                        BigDecimal min = BigDecimal.ONE.movePointLeft(4);
                        BigDecimal max = BigDecimal.ONE.movePointLeft(4);

                        try {
                            min = qv.getBigDecimal("fromQuantity");
                            max = qv.getBigDecimal("thruQuantity");
                        } catch (Exception e) {
                        }
                        if (shippableQuantity.compareTo(min) >= 0 && (max.compareTo(BigDecimal.ZERO) == 0 || shippableQuantity.compareTo(max) <= 0)) {
                            qtyValid = true;
                        }
                    }
                    if (pv != null) {
                        usePrice = true;
                        BigDecimal min = BigDecimal.ONE.movePointLeft(4);
                        BigDecimal max = BigDecimal.ONE.movePointLeft(4);

                        try {
                            min = pv.getBigDecimal("fromQuantity");
                            max = pv.getBigDecimal("thruQuantity");
                        } catch (Exception e) {
                        }
                        if (shippableTotal.compareTo(min) >= 0 && (max.compareTo(BigDecimal.ZERO) == 0 || shippableTotal.compareTo(max) <= 0)) {
                            priceValid = true;
                        }
                    }
                    // Now check the tests.
                    if ((useWeight && weightValid) || (useQty && qtyValid) || (usePrice && priceValid)) {
                        estimateList.add(thisEstimate);
                    }
                }
            }

        }

        if (estimateList.size() < 1) {
            return ServiceUtil.returnFailure("No shipping estimate found for carrier [" + carrierPartyId + "] and shipment method type [" + shipmentMethodTypeId +"]");
        }

        // make the shippable item size/feature objects
        List<BigDecimal> shippableItemSizes = FastList.newInstance();
        Map<String, BigDecimal> shippableFeatureMap = FastMap.newInstance();
        if (shippableItemInfo != null) {
            for (Map<String, Object> itemMap: shippableItemInfo) {
                // add the item sizes
                BigDecimal itemSize = (BigDecimal) itemMap.get("size");
                if (itemSize != null) {
                    shippableItemSizes.add(itemSize);
                }

                // add the feature quantities
                BigDecimal quantity = (BigDecimal) itemMap.get("quantity");
                Set<String> featureSet = UtilGenerics.checkSet(itemMap.get("featureSet"));
                if (UtilValidate.isNotEmpty(featureSet)) {
                    for (String featureId: featureSet) {
                        BigDecimal featureQuantity = shippableFeatureMap.get(featureId);
                        if (featureQuantity == null) {
                            featureQuantity = BigDecimal.ZERO;
                        }
                        featureQuantity = featureQuantity.add(quantity);
                        shippableFeatureMap.put(featureId, featureQuantity);
                    }
                }

            }
        }

        // Calculate priority based on available data.
        double PRIORITY_PARTY = 9;
        double PRIORITY_ROLE = 8;
        double PRIORITY_GEO = 4;
        double PRIORITY_WEIGHT = 1;
        double PRIORITY_QTY = 1;
        double PRIORITY_PRICE = 1;

        int estimateIndex = 0;

        if (estimateList.size() > 1) {
            TreeMap<Integer, GenericValue> estimatePriority = new TreeMap<Integer, GenericValue>();
            //int estimatePriority[] = new int[estimateList.size()];

            for (GenericValue currentEstimate: estimateList) {
                int prioritySum = 0;
                if (UtilValidate.isNotEmpty(currentEstimate.getString("partyId"))) {
                    prioritySum += PRIORITY_PARTY;
                }
                if (UtilValidate.isNotEmpty(currentEstimate.getString("roleTypeId"))) {
                    prioritySum += PRIORITY_ROLE;
                }
                if (UtilValidate.isNotEmpty(currentEstimate.getString("geoIdTo"))) {
                    prioritySum += PRIORITY_GEO;
                }
                if (UtilValidate.isNotEmpty(currentEstimate.getString("weightBreakId"))) {
                    prioritySum += PRIORITY_WEIGHT;
                }
                if (UtilValidate.isNotEmpty(currentEstimate.getString("quantityBreakId"))) {
                    prioritySum += PRIORITY_QTY;
                }
                if (UtilValidate.isNotEmpty(currentEstimate.getString("priceBreakId"))) {
                    prioritySum += PRIORITY_PRICE;
                }

                // there will be only one of each priority; latest will replace
                estimatePriority.put(Integer.valueOf(prioritySum), currentEstimate);
            }

            // locate the highest priority estimate; or the latest entered
            Object[] estimateArray = estimatePriority.values().toArray();
            estimateIndex = estimateList.indexOf(estimateArray[estimateArray.length - 1]);
        }

        // Grab the estimate and work with it.
        GenericValue estimate = estimateList.get(estimateIndex);

        //Debug.log("[ShippingEvents.getShipEstimate] Working with estimate [" + estimateIndex + "]: " + estimate, module);

        // flat fees
        BigDecimal orderFlat = BigDecimal.ZERO;
        if (estimate.getBigDecimal("orderFlatPrice") != null) {
            orderFlat = estimate.getBigDecimal("orderFlatPrice");
        }

        BigDecimal orderItemFlat = BigDecimal.ZERO;
        if (estimate.getBigDecimal("orderItemFlatPrice") != null) {
            orderItemFlat = estimate.getBigDecimal("orderItemFlatPrice");
        }

        BigDecimal orderPercent = BigDecimal.ZERO;
        if (estimate.getBigDecimal("orderPricePercent") != null) {
            orderPercent = estimate.getBigDecimal("orderPricePercent");
        }

        BigDecimal itemFlatAmount = shippableQuantity.multiply(orderItemFlat);
        BigDecimal orderPercentage = shippableTotal.multiply(orderPercent.movePointLeft(2));

        // flat total
        BigDecimal flatTotal = orderFlat.add(itemFlatAmount).add(orderPercentage);

        // spans
        BigDecimal weightUnit = BigDecimal.ZERO;
        if (estimate.getBigDecimal("weightUnitPrice") != null) {
            weightUnit = estimate.getBigDecimal("weightUnitPrice");
        }

        BigDecimal qtyUnit = BigDecimal.ZERO;
        if (estimate.getBigDecimal("quantityUnitPrice") != null) {
            qtyUnit = estimate.getBigDecimal("quantityUnitPrice");
        }

        BigDecimal priceUnit = BigDecimal.ZERO;
        if (estimate.getBigDecimal("priceUnitPrice") != null) {
            priceUnit = estimate.getBigDecimal("priceUnitPrice");
        }

        BigDecimal weightAmount = shippableWeight.multiply(weightUnit);
        BigDecimal quantityAmount = shippableQuantity.multiply(qtyUnit);
        BigDecimal priceAmount = shippableTotal.multiply(priceUnit);

        // span total
        BigDecimal spanTotal = weightAmount.add(quantityAmount).add(priceAmount);

        // feature surcharges
        BigDecimal featureSurcharge = BigDecimal.ZERO;
        String featureGroupId = estimate.getString("productFeatureGroupId");
        BigDecimal featurePercent = estimate.getBigDecimal("featurePercent");
        BigDecimal featurePrice = estimate.getBigDecimal("featurePrice");
        if (featurePercent == null) {
            featurePercent = BigDecimal.ZERO;
        }
        if (featurePrice == null) {
            featurePrice = BigDecimal.ZERO;
        }

        if (UtilValidate.isNotEmpty(featureGroupId) && shippableFeatureMap != null) {
            for (Map.Entry<String, BigDecimal> entry: shippableFeatureMap.entrySet()) {
                String featureId = entry.getKey();
                BigDecimal quantity = entry.getValue();
                GenericValue appl = null;
                Map<String, String> fields = UtilMisc.toMap("productFeatureGroupId", featureGroupId, "productFeatureId", featureId);
                try {
                    List<GenericValue> appls = delegator.findByAndCache("ProductFeatureGroupAppl", fields);
                    appls = EntityUtil.filterByDate(appls);
                    appl = EntityUtil.getFirst(appls);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to lookup feature/group" + fields, module);
                }
                if (appl != null) {
                    featureSurcharge = featureSurcharge.add(shippableTotal.multiply(featurePercent.movePointLeft(2)).multiply(quantity));
                    featureSurcharge = featureSurcharge.add(featurePrice.multiply(quantity));
                }
            }
        }

        // size surcharges
        BigDecimal sizeSurcharge = BigDecimal.ZERO;
        BigDecimal sizeUnit = estimate.getBigDecimal("oversizeUnit");
        BigDecimal sizePrice = estimate.getBigDecimal("oversizePrice");
        if (sizeUnit != null && sizeUnit.compareTo(BigDecimal.ZERO) > 0) {
            if (shippableItemSizes != null) {
                for (BigDecimal size: shippableItemSizes) {
                    if (size != null && size.compareTo(sizeUnit) >= 0) {
                        sizeSurcharge = sizeSurcharge.add(sizePrice);
                    }
                }
            }
        }

        // surcharges total
        BigDecimal surchargeTotal = featureSurcharge.add(sizeSurcharge);

        // shipping subtotal
        BigDecimal subTotal = spanTotal.add(flatTotal).add(surchargeTotal);

        // percent add-on
        BigDecimal shippingPricePercent = BigDecimal.ZERO;
        if (estimate.getBigDecimal("shippingPricePercent") != null) {
            shippingPricePercent = estimate.getBigDecimal("shippingPricePercent");
        }

        // shipping total
        BigDecimal shippingTotal = subTotal.add((subTotal.add(initialEstimateAmt)).multiply(shippingPricePercent.movePointLeft(2)));

        // prepare the return result
        Map<String, Object> responseResult = ServiceUtil.returnSuccess();
        responseResult.put("shippingEstimateAmount", shippingTotal);
        return responseResult;
    }

    public static Map<String, Object> fillShipmentStagingTables(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");

        GenericValue shipment = null;
        if (shipmentId != null) {
            try {
                shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (shipment == null) {
            return ServiceUtil.returnError("No shipment found!");
        }

        String shipmentStatusId = shipment.getString("statusId");
        if ("SHIPMENT_PACKED".equals(shipmentStatusId)) {
            GenericValue address = null;
            try {
                address = shipment.getRelatedOne("DestinationPostalAddress");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (address == null) {
                return ServiceUtil.returnError("No address found for shipment!");
            }

            List<GenericValue> packages = null;
            try {
                packages = shipment.getRelated("ShipmentPackage") ;
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            if (UtilValidate.isEmpty(packages)) {
                return ServiceUtil.returnError("No packages are available for shipping!");
            }

            List<GenericValue> routeSegs = null;
            try {
                routeSegs = shipment.getRelated("ShipmentRouteSegment");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            GenericValue routeSeg = EntityUtil.getFirst(routeSegs);

            // to store list
            List<GenericValue> toStore = FastList.newInstance();

            //String shipGroupSeqId = shipment.getString("primaryShipGroupSeqId");
            //String orderId = shipment.getString("primaryOrderId");
            //String orderInfoKey = orderId + "/" + shipGroupSeqId;

            // make the staging records
            GenericValue stageShip = delegator.makeValue("OdbcShipmentOut");
            stageShip.set("shipmentId", shipment.get("shipmentId"));
            stageShip.set("partyId", shipment.get("partyIdTo"));
            stageShip.set("carrierPartyId", routeSeg.get("carrierPartyId"));
            stageShip.set("shipmentMethodTypeId", routeSeg.get("shipmentMethodTypeId"));
            stageShip.set("toName", address.get("toName"));
            stageShip.set("attnName", address.get("attnName"));
            stageShip.set("address1", address.get("address1"));
            stageShip.set("address2", address.get("address2"));
            stageShip.set("directions", address.get("directions"));
            stageShip.set("city", address.get("city"));
            stageShip.set("postalCode", address.get("postalCode"));
            stageShip.set("postalCodeExt", address.get("postalCodeExt"));
            stageShip.set("countryGeoId", address.get("countryGeoId"));
            stageShip.set("stateProvinceGeoId", address.get("stateProvinceGeoId"));
            stageShip.set("numberOfPackages", Long.valueOf(packages.size()));
            stageShip.set("handlingInstructions", shipment.get("handlingInstructions"));
            toStore.add(stageShip);


            for (GenericValue shipmentPkg: packages) {
                GenericValue stagePkg = delegator.makeValue("OdbcPackageOut");
                stagePkg.set("shipmentId", shipmentPkg.get("shipmentId"));
                stagePkg.set("shipmentPackageSeqId", shipmentPkg.get("shipmentPackageSeqId"));
                stagePkg.set("orderId", shipment.get("primaryOrderId"));
                stagePkg.set("shipGroupSeqId", shipment.get("primaryShipGroupSeqId"));
                stagePkg.set("shipmentBoxTypeId", shipmentPkg.get("shipmentBoxTypeId"));
                stagePkg.set("weight", shipmentPkg.get("weight"));
                toStore.add(stagePkg);
            }

            try {
                delegator.storeAll(toStore);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            Debug.logWarning("Shipment #" + shipmentId + " is not available for shipment; not setting in staging tables.", module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateShipmentsFromStaging(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List<String> orderBy = UtilMisc.toList("shipmentId", "shipmentPackageSeqId", "voidIndicator");
        Map<String, String> shipmentMap = FastMap.newInstance();

        EntityListIterator eli = null;
        try {
            eli = delegator.find("OdbcPackageIn", null, null, null, orderBy, null);
            GenericValue pkgInfo;
            while ((pkgInfo = eli.next()) != null) {
                String packageSeqId = pkgInfo.getString("shipmentPackageSeqId");
                String shipmentId = pkgInfo.getString("shipmentId");

                // locate the shipment package
                GenericValue shipmentPackage = delegator.findByPrimaryKey("ShipmentPackage",
                        UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", packageSeqId));

                if (shipmentPackage != null) {
                    if ("00001".equals(packageSeqId)) {
                        // only need to do this for the first package
                        GenericValue rtSeg = null;
                        try {
                            rtSeg = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", "00001"));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }

                        if (rtSeg == null) {
                            rtSeg = delegator.makeValue("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", "00001"));
                            try {
                                delegator.create(rtSeg);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                        }

                        rtSeg.set("actualStartDate", pkgInfo.get("shippedDate"));
                        rtSeg.set("billingWeight", pkgInfo.get("billingWeight"));
                        rtSeg.set("actualCost", pkgInfo.get("shippingTotal"));
                        rtSeg.set("trackingIdNumber", pkgInfo.get("trackingNumber"));
                        try {
                            delegator.store(rtSeg);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }

                    Map<String, Object> pkgCtx = FastMap.newInstance();
                    pkgCtx.put("shipmentId", shipmentId);
                    pkgCtx.put("shipmentPackageSeqId", packageSeqId);

                    // first update the weight of the package
                    GenericValue pkg = null;
                    try {
                        pkg = delegator.findByPrimaryKey("ShipmentPackage", pkgCtx);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    if (pkg == null) {
                        return ServiceUtil.returnError("Package not found! - " + pkgCtx);
                    }

                    pkg.set("weight", pkgInfo.get("packageWeight"));
                    try {
                        delegator.store(pkg);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    // need if we are the first package (only) update the route seg info
                    pkgCtx.put("shipmentRouteSegmentId", "00001");
                    GenericValue pkgRtSeg = null;
                    try {
                        pkgRtSeg = delegator.findByPrimaryKey("ShipmentPackageRouteSeg", pkgCtx);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    if (pkgRtSeg == null) {
                        pkgRtSeg = delegator.makeValue("ShipmentPackageRouteSeg", pkgCtx);
                        try {
                            delegator.create(pkgRtSeg);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }

                    pkgRtSeg.set("trackingCode", pkgInfo.get("trackingNumber"));
                    pkgRtSeg.set("boxNumber", pkgInfo.get("shipmentPackageSeqId"));
                    pkgRtSeg.set("packageServiceCost", pkgInfo.get("packageTotal"));
                    try {
                        delegator.store(pkgRtSeg);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                    shipmentMap.put(shipmentId, pkgInfo.getString("voidIndicator"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }

        // update the status of each shipment
        for (Map.Entry<String, String> entry: shipmentMap.entrySet()) {
            String shipmentId = entry.getKey();
            String voidInd = entry.getValue();
            Map<String, Object> shipCtx = FastMap.newInstance();
            shipCtx.put("shipmentId", shipmentId);
            if ("Y".equals(voidInd)) {
                shipCtx.put("statusId", "SHIPMENT_CANCELLED");
            } else {
                shipCtx.put("statusId", "SHIPMENT_SHIPPED");
            }
            shipCtx.put("userLogin", userLogin);
            Map<String, Object> shipResp = null;
            try {
                shipResp = dispatcher.runSync("updateShipment", shipCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(shipResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(shipResp));
            }

            // remove the shipment info
            Map<String, Object> clearResp = null;
            try {
                clearResp = dispatcher.runSync("clearShipmentStaging", UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "userLogin", userLogin));
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(clearResp)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(clearResp));
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearShipmentStagingInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        try {
            delegator.removeByAnd("OdbcPackageIn", UtilMisc.toMap("shipmentId", shipmentId));
            delegator.removeByAnd("OdbcPackageOut", UtilMisc.toMap("shipmentId", shipmentId));
            delegator.removeByAnd("OdbcShipmentOut", UtilMisc.toMap("shipmentId", shipmentId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Whenever a ShipmentReceipt is generated, check the Shipment associated
     * with it to see if all items were received. If so, change its status to
     * PURCH_SHIP_RECEIVED. The check is accomplished by counting the
     * products shipped (from ShipmentAndItem) and matching them with the
     * products received (from ShipmentReceipt).
     */
    public static Map<String, Object> updatePurchaseShipmentFromReceipt(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {

            List<GenericValue> shipmentReceipts = delegator.findByAnd("ShipmentReceipt", UtilMisc.toMap("shipmentId", shipmentId));
            if (shipmentReceipts.size() == 0) return ServiceUtil.returnSuccess();

            // If there are shipment receipts, the shipment must have been shipped, so set the shipment status to PURCH_SHIP_SHIPPED if it's only PURCH_SHIP_CREATED
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if ((! UtilValidate.isEmpty(shipment)) && "PURCH_SHIP_CREATED".equals(shipment.getString("statusId"))) {
                Map<String, Object> updateShipmentMap = dispatcher.runSync("updateShipment", UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "statusId", "PURCH_SHIP_SHIPPED", "userLogin", userLogin));
                if (ServiceUtil.isError(updateShipmentMap)) {
                    return updateShipmentMap;
                }
            }

            List<GenericValue> shipmentAndItems = delegator.findByAnd("ShipmentAndItem", UtilMisc.toMap("shipmentId", shipmentId, "statusId", "PURCH_SHIP_SHIPPED"));
            if (shipmentAndItems.size() == 0) {
                return ServiceUtil.returnSuccess();
            }

            // store the quantity of each product shipped in a hashmap keyed to productId
            Map<String, BigDecimal> shippedCountMap = FastMap.newInstance();
            for (GenericValue item: shipmentAndItems) {
                BigDecimal shippedQuantity = item.getBigDecimal("quantity");
                BigDecimal quantity = shippedCountMap.get(item.getString("productId"));
                quantity = quantity == null ? shippedQuantity : shippedQuantity.add(quantity);
                shippedCountMap.put(item.getString("productId"), quantity);
            }

            // store the quantity of each product received in a hashmap keyed to productId
            Map<String, BigDecimal> receivedCountMap = FastMap.newInstance();
            for (GenericValue item: shipmentReceipts) {
                BigDecimal receivedQuantity = item.getBigDecimal("quantityAccepted");
                BigDecimal quantity = receivedCountMap.get(item.getString("productId"));
                quantity = quantity == null ? receivedQuantity : receivedQuantity.add(quantity);
                receivedCountMap.put(item.getString("productId"), quantity);
            }

            // let Map.equals do all the hard comparison work
            if (!shippedCountMap.equals(receivedCountMap)) {
                return ServiceUtil.returnSuccess();
            }

            // now update the shipment
            dispatcher.runSync("updateShipment", UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "statusId", "PURCH_SHIP_RECEIVED", "userLogin", userLogin));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException se) {
            Debug.logError(se, module);
            return ServiceUtil.returnError(se.getMessage());
        }
        return ServiceUtil.returnSuccess("Intentional error at end to keep from committing.");
    }

    public static Map<String, Object> duplicateShipmentRouteSegment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");

        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            GenericValue shipmentRouteSeg = delegator.findByPrimaryKey("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            if (shipmentRouteSeg == null) {
                return ServiceUtil.returnError("Shipment Route Segment not found for shipment [" + shipmentId + "] route segment [" + shipmentRouteSegmentId + "]");
            }

            Map<String, Object> params = UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "carrierPartyId", shipmentRouteSeg.getString("carrierPartyId"), "shipmentMethodTypeId", shipmentRouteSeg.getString("shipmentMethodTypeId"),
                    "originFacilityId", shipmentRouteSeg.getString("originFacilityId"), "originContactMechId", shipmentRouteSeg.getString("originContactMechId"),
                    "originTelecomNumberId", shipmentRouteSeg.getString("originTelecomNumberId"));
            params.put("destFacilityId", shipmentRouteSeg.getString("destFacilityId"));
            params.put("destContactMechId", shipmentRouteSeg.getString("destContactMechId"));
            params.put("destTelecomNumberId", shipmentRouteSeg.getString("destTelecomNumberId"));
            params.put("billingWeight", shipmentRouteSeg.get("billingWeight"));
            params.put("billingWeightUomId", shipmentRouteSeg.get("billingWeightUomId"));
            params.put("userLogin", userLogin);

            Map<String, Object> tmpResult = dispatcher.runSync("createShipmentRouteSegment", params);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            } else {
                results.put("newShipmentRouteSegmentId", tmpResult.get("shipmentRouteSegmentId"));
                return results;
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }
    /**
     * Service to call a ShipmentRouteSegment.carrierPartyId's confirm shipment method asynchronously
     */
    public static Map<String, Object> quickScheduleShipmentRouteSegment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String shipmentId = (String) context.get("shipmentId");
        String shipmentRouteSegmentId = (String) context.get("shipmentRouteSegmentId");
        String carrierPartyId = null;

        // get the carrierPartyId
        try {
            GenericValue shipmentRouteSegment = delegator.findByPrimaryKeyCache("ShipmentRouteSegment",
                    UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId));
            carrierPartyId = shipmentRouteSegment.getString("carrierPartyId");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // get the shipment label.  This is carrier specific.
        // TODO: This may not need to be done asynchronously.  The reason it's done that way right now is that calling it synchronously means that
        // if we can't confirm a single shipment, then all shipment route segments in a multi-form are rolled back.
        try {
            Map<String, String> input = UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegmentId, "userLogin", userLogin);
            // for DHL, we just need to confirm the shipment to get the label.  Other carriers may have more elaborate requirements.
            if (carrierPartyId.equals("DHL")) {
                dispatcher.runAsync("dhlShipmentConfirm", input);
            } else {
                Debug.logError(carrierPartyId + " is not supported at this time.  Sorry.", module);
            }
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), module);
        }

        // don't return an error
        return ServiceUtil.returnSuccess();
    }

    /**
     * Calculates the total value of a shipment package by totalling the results of the getOrderItemInvoicedAmountAndQuantity
     *  service for the orderItem related to each ShipmentPackageContent, prorated by the quantity of the orderItem issued to the
     *  ShipmentPackageContent. Value is converted according to the incoming currencyUomId.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map
     */
    public static Map<String, Object> getShipmentPackageValueFromOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String shipmentId = (String) context.get("shipmentId");
        String shipmentPackageSeqId = (String) context.get("shipmentPackageSeqId");
        String currencyUomId = (String) context.get("currencyUomId");

        BigDecimal packageTotalValue = ZERO;

        GenericValue shipment = null;
        GenericValue shipmentPackage = null;
        try {

            shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (UtilValidate.isEmpty(shipment)) {
                String errorMessage = UtilProperties.getMessage(resource, "ProductShipmentNotFoundId", locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            shipmentPackage = delegator.findByPrimaryKey("ShipmentPackage", UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackageSeqId));
            if (UtilValidate.isEmpty(shipmentPackage)) {
                String errorMessage = UtilProperties.getMessage(resource, "ProductShipmentPackageNotFound", context, locale);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            List<GenericValue> packageContents = delegator.findByAnd("PackedQtyVsOrderItemQuantity", UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackageSeqId));
            for (GenericValue packageContent: packageContents) {
                String orderId = packageContent.getString("orderId");
                String orderItemSeqId = packageContent.getString("orderItemSeqId");

                // Get the value of the orderItem by calling the getOrderItemInvoicedAmountAndQuantity service
                Map<String, Object> getOrderItemValueResult = dispatcher.runSync("getOrderItemInvoicedAmountAndQuantity", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "userLogin", userLogin, "locale", locale));
                if (ServiceUtil.isError(getOrderItemValueResult)) return getOrderItemValueResult;
                BigDecimal invoicedAmount = (BigDecimal) getOrderItemValueResult.get("invoicedAmount");
                BigDecimal invoicedQuantity = (BigDecimal) getOrderItemValueResult.get("invoicedQuantity");

                // How much of the invoiced quantity does the issued quantity represent?
                BigDecimal issuedQuantity = packageContent.getBigDecimal("issuedQuantity");
                BigDecimal proportionOfInvoicedQuantity = invoicedQuantity.signum() == 0 ? ZERO : issuedQuantity.divide(invoicedQuantity, 10, rounding);

                // Prorate the orderItem's invoiced amount by that proportion
                BigDecimal packageContentValue = proportionOfInvoicedQuantity.multiply(invoicedAmount).setScale(decimals, rounding);

                // Convert the value to the shipment currency, if necessary
                GenericValue orderHeader = packageContent.getRelatedOne("OrderHeader");
                Map<String, Object> convertUomResult = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", orderHeader.getString("currencyUom"), "uomIdTo", currencyUomId, "originalValue", packageContentValue));
                if (ServiceUtil.isError(convertUomResult)) {
                    return convertUomResult;
                }
                if (convertUomResult.containsKey("convertedValue")) {
                    packageContentValue = ((BigDecimal) convertUomResult.get("convertedValue")).setScale(decimals, rounding);
                }

                // Add the value of the packed item to the package's total value
                packageTotalValue = packageTotalValue.add(packageContentValue);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("packageValue", packageTotalValue);
        return result;
    }

    public static Map<String, Object> sendShipmentCompleteNotification(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String shipmentId = (String) context.get("shipmentId");
        String sendTo = (String) context.get("sendTo");
        String screenUri = (String) context.get("screenUri");

        // prepare the shipment information
        Map<String, Object> sendMap = FastMap.newInstance();
        GenericValue shipment = null ;
        GenericValue orderHeader = null;
        try {
            shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", shipment.getString("primaryOrderId")));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting info from database", module);
        }
        GenericValue productStoreEmail = null;
        try {
            productStoreEmail = delegator.findByPrimaryKey("ProductStoreEmailSetting", UtilMisc.toMap("productStoreId", orderHeader.get("productStoreId"), "emailType", "PRDS_ODR_SHIP_COMPLT"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting the ProductStoreEmailSetting for productStoreId =" + orderHeader.get("productStoreId") + " and emailType = PRDS_ODR_SHIP_COMPLT", module);
        }
        if (productStoreEmail == null) {
            return ServiceUtil.returnFailure("No valid email setting for store with productStoreId =" + orderHeader.get("productStoreId") + " and emailType = PRDS_ODR_SHIP_COMPLT");
        }
        // the override screenUri
        if (UtilValidate.isEmpty(screenUri)) {
            String bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
            sendMap.put("bodyScreenUri", bodyScreenLocation);
        } else {
            sendMap.put("bodyScreenUri", screenUri);
        }

        String partyId = shipment.getString("partyIdTo");

        // get the email address
        String emailString = null;
        GenericValue email = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
        if (UtilValidate.isNotEmpty(email)) {
            emailString = email.getString("infoString");
        }
        if (UtilValidate.isEmpty(emailString)) {
            return ServiceUtil.returnError("No sendTo email address found");
        }

        Locale locale = PartyWorker.findPartyLastLocale(partyId, delegator);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("EcommerceUiLabels", locale);
        uiLabelMap.addBottomResourceBundle("OrderUiLabels");
        uiLabelMap.addBottomResourceBundle("CommonUiLabels");

        Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("partyId", partyId, "shipmentId", shipmentId, "orderId", shipment.getString("primaryOrderId"), "userLogin", userLogin, "uiLabelMap", uiLabelMap, "locale", locale);
        sendMap.put("bodyParameters", bodyParameters);
        sendMap.put("userLogin",userLogin);

        sendMap.put("subject", productStoreEmail.getString("subject"));
        sendMap.put("contentType", productStoreEmail.get("contentType"));
        sendMap.put("sendFrom", productStoreEmail.get("fromAddress"));
        sendMap.put("sendCc", productStoreEmail.get("ccAddress"));
        sendMap.put("sendBcc", productStoreEmail.get("bccAddress"));

        if ((sendTo != null) && UtilValidate.isEmail(sendTo)) {
            sendMap.put("sendTo", sendTo);
        } else {
            sendMap.put("sendTo", emailString);
        }
        // send the notification
        Map<String, Object> sendResp = null;
        try {
            sendResp = dispatcher.runSync("sendMailFromScreen", sendMap);
        } catch (Exception e) {
            Debug.logError(e, "Problem sending mail", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderProblemSendingEmail", locale));
        }
        // check for errors
        if (sendResp != null && ServiceUtil.isError(sendResp)) {
            sendResp.put("emailType", "PRDS_ODR_SHIP_COMPLT");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderProblemSendingEmail", locale), null, null, sendResp);
        }
        return sendResp;
    }
}
