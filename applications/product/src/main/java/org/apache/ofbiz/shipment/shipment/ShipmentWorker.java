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
package org.apache.ofbiz.shipment.shipment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ShipmentWorker - Worker methods for Shipment and related entities
 */
public final class ShipmentWorker {

    private static final String MODULE = ShipmentWorker.class.getName();
    private static final MathContext GEN_ROUNDING = new MathContext(10);

    private ShipmentWorker() { }

    /*
     * Returns the value of a given ShipmentPackageContent record.  Calculated by working out the total value (from the OrderItems) of all ItemIssuances
     * for the ShipmentItem then dividing that by the total quantity issued for the same to get an average item value then multiplying that by the package
     * content quantity.
     * Note: No rounding of the calculation is performed so you will need to round it to the accuracy that you require
     */
    public static BigDecimal getShipmentPackageContentValue(GenericValue shipmentPackageContent) {
        BigDecimal quantity = shipmentPackageContent.getBigDecimal("quantity");

        BigDecimal value;

        // lookup the issuance to find the order
        List<GenericValue> issuances = null;
        try {
            GenericValue shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem", false);
            issuances = shipmentItem.getRelated("ItemIssuance", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        BigDecimal totalIssued = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        if (UtilValidate.isNotEmpty(issuances)) {
            for (GenericValue issuance : issuances) {
                // we only need one
                BigDecimal issuanceQuantity = issuance.getBigDecimal("quantity");
                BigDecimal issuanceCancelQuantity = issuance.getBigDecimal("cancelQuantity");
                if (issuanceCancelQuantity != null) {
                    issuanceQuantity = issuanceQuantity.subtract(issuanceCancelQuantity);
                }
                // get the order item
                GenericValue orderItem = null;
                try {
                    orderItem = issuance.getRelatedOne("OrderItem", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }

                if (orderItem != null) {
                    // get the value per unit - (base price * amount)
                    BigDecimal selectedAmount = orderItem.getBigDecimal("selectedAmount");
                    if (selectedAmount == null || selectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        selectedAmount = BigDecimal.ONE;
                    }

                    BigDecimal unitPrice = orderItem.getBigDecimal("unitPrice");
                    BigDecimal itemValue = unitPrice.multiply(selectedAmount);

                    // total value for package (per unit * quantity)
                    totalIssued = totalIssued.add(issuanceQuantity);
                    totalValue = totalValue.add(itemValue.multiply(issuanceQuantity));
                }
            }
        }
        // take the average value of the issuances and multiply it by the shipment package content quantity
        value = totalValue.divide(totalIssued, 10, RoundingMode.HALF_EVEN).multiply(quantity);
        return value;
    }

    public static List<Map<String, BigDecimal>> getPackageSplit(DispatchContext dctx, List<Map<String, Object>> shippableItemInfo, BigDecimal maxWeight) {
        // create the package list w/ the first package
        List<Map<String, BigDecimal>> packages = new LinkedList<>();

        if (UtilValidate.isNotEmpty(shippableItemInfo)) {
            for (Map<String, Object> itemInfo: shippableItemInfo) {
                long pieces = (Long) itemInfo.get("piecesIncluded");
                BigDecimal totalQuantity = (BigDecimal) itemInfo.get("quantity");
                BigDecimal totalWeight = (BigDecimal) itemInfo.get("weight");
                String productId = (String) itemInfo.get("productId");

                // sanity check
                if (pieces < 1) {
                    pieces = 1; // can NEVER be less than one
                }
                BigDecimal weight = totalWeight.divide(BigDecimal.valueOf(pieces), GEN_ROUNDING);
                for (int z = 1; z <= totalQuantity.intValue(); z++) {
                    BigDecimal partialQty = pieces > 1 ? BigDecimal.ONE.divide(BigDecimal.valueOf(pieces), GEN_ROUNDING) : BigDecimal.ONE;
                    for (long x = 0; x < pieces; x++) {
                        if (weight.compareTo(maxWeight) >= 0) {
                            Map<String, BigDecimal> newPackage = new HashMap<>();
                            newPackage.put(productId, partialQty);
                            packages.add(newPackage);
                        } else if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
                            // create the first package
                            if (packages.size() == 0) {
                                packages.add(new HashMap<>());
                            }

                            // package loop
                            boolean addedToPackage = false;
                            for (Map<String, BigDecimal> packageMap: packages) {
                                if (!addedToPackage) {
                                    BigDecimal packageWeight = calcPackageWeight(dctx, packageMap, shippableItemInfo, weight);
                                    if (packageWeight.compareTo(maxWeight) <= 0) {
                                        BigDecimal qty = packageMap.get(productId);
                                        qty = UtilValidate.isEmpty(qty) ? BigDecimal.ZERO : qty;
                                        packageMap.put(productId, qty.add(partialQty));
                                        addedToPackage = true;
                                    }
                                }
                            }
                            if (!addedToPackage) {
                                Map<String, BigDecimal> packageMap = new HashMap<>();
                                packageMap.put(productId, partialQty);
                                packages.add(packageMap);
                            }
                        }
                    }
                }
            }
        }
        return packages;
    }

    public static BigDecimal calcPackageWeight(DispatchContext dctx, Map<String, BigDecimal> packageMap, List<Map<String, Object>> shippableItemInfo, BigDecimal additionalWeight) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        BigDecimal totalWeight = BigDecimal.ZERO;
        String defaultWeightUomId = EntityUtilProperties.getPropertyValue("shipment", "shipment.default.weight.uom", delegator);

        for (Map.Entry<String, BigDecimal> entry: packageMap.entrySet()) {
            String productId = entry.getKey();
            Map<String, Object> productInfo = getProductItemInfo(shippableItemInfo, productId);
            BigDecimal productWeight = (BigDecimal) productInfo.get("productWeight");
            if (productWeight == null) productWeight = BigDecimal.ZERO;
            BigDecimal quantity = packageMap.get(productId);

            String weightUomId = (String) productInfo.get("weightUomId");

            Debug.logInfo("Product Id : " + productId + " Product Weight : " + String.valueOf(productWeight) + " Product UomId : " + weightUomId + " assuming " + defaultWeightUomId + " if null. Quantity : " + String.valueOf(quantity), MODULE);

            if (UtilValidate.isEmpty(weightUomId)) {
                weightUomId = defaultWeightUomId;
            }
            if (!"WT_lb".equals(weightUomId)) {
                // attempt a conversion to pounds
                Map<String, Object> result = new HashMap<>();
                try {
                    result = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", weightUomId, "uomIdTo", "WT_lb", "originalValue", productWeight));
                    if (ServiceUtil.isError(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
                        return totalWeight;
                    }
                } catch (GenericServiceException ex) {
                    Debug.logError(ex, MODULE);
                }
                if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS) && UtilValidate.isNotEmpty(result.get("convertedValue"))) {
                    productWeight = (BigDecimal) result.get("convertedValue");
                } else {
                    Debug.logError("Unsupported weightUom [" + weightUomId + "] for calcPackageWeight running productId " + productId + ", could not find a conversion factor to WT_lb", MODULE);
                }
            }

            totalWeight = totalWeight.add(productWeight.multiply(quantity));
        }
        Debug.logInfo("Package Weight : " + String.valueOf(totalWeight) + " lbs.", MODULE);
        return totalWeight.add(additionalWeight);
    }

    public static Map<String, Object> getProductItemInfo(List<Map<String, Object>> shippableItemInfo, String productId) {
        if (UtilValidate.isNotEmpty(shippableItemInfo)) {
            for (Map<String, Object> itemInfoMap: shippableItemInfo) {
                String compareProductId = (String) itemInfoMap.get("productId");
                if (productId.equals(compareProductId)) {
                    return itemInfoMap;
                }
            }
        }
        return null;
    }
}
