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
package org.ofbiz.shipment.packing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class PackingServices {

    public static final String module = PackingServices.class.getName();
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    public static Map<String, Object> addPackLine(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        PackingSession session = (PackingSession) context.get("packingSession");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String orderId = (String) context.get("orderId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal weight = (BigDecimal) context.get("weight");
        Integer packageSeq = (Integer) context.get("packageSeq");

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get("pickerPartyId");
        session.setPickerPartyId(pickerPartyId);

        if (quantity == null) {
            quantity = ZERO;
        }

        Debug.log("OrderId [" + orderId + "] ship group [" + shipGroupSeqId + "] Pack input [" + productId + "] @ [" + quantity + "] packageSeq [" + packageSeq + "] weight [" + weight +"]", module);

        if (weight == null) {
            Debug.logWarning("OrderId [" + orderId + "] ship group [" + shipGroupSeqId + "] product [" + productId + "] being packed without a weight, assuming 0", module);
            weight = ZERO;
        }

        List<String> orderItemSeqIds = FastList.newInstance();
        BigDecimal qtyToPack = ZERO;
        BigDecimal qtyToPacked = ZERO;
        BigDecimal packedQuantity = ZERO;
        BigDecimal readyToPackQty = ZERO;
        int counter = 0;
        try {
            // check if entered product is ordered product or not
            if (UtilValidate.isNotEmpty(productId)) {
                List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "productId", productId));
                if (UtilValidate.isNotEmpty(orderItems)) {
                    for (GenericValue orderItem : orderItems) {
                        counter++;
                        if (quantity.compareTo(ZERO) > 0) {
                            BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                            List<GenericValue> shipments = delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId , "statusId", "SHIPMENT_PACKED"));
                            for(GenericValue shipment : shipments) {
                                List<GenericValue> itemIssuances = shipment.getRelatedByAnd("ItemIssuance" , UtilMisc.toMap("shipmentId", shipment.getString("shipmentId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")));
                                for(GenericValue itemIssuance : itemIssuances) {
                                    packedQuantity = packedQuantity.add(itemIssuance.getBigDecimal("quantity"));
                                }
                            }
                            qtyToPack = orderedQuantity.subtract(packedQuantity);
                            if (qtyToPack.compareTo(quantity) > -1) {
                                readyToPackQty = session.getPackedQuantity(orderId, orderItem.getString("orderItemSeqId"), shipGroupSeqId, productId);
                                qtyToPacked =  orderedQuantity.subtract(readyToPackQty);
                                if (qtyToPacked.compareTo(quantity) > -1) {
                                    session.addOrIncreaseLine(orderId, orderItem.getString("orderItemSeqId"), shipGroupSeqId, productId, quantity, packageSeq.intValue(), weight, false);
                                    counter--;
                                    break;
                                } else if (orderItems.size() == counter) {
                                    throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoValidOrderItemFoundForProductWithEnteredQuantity", UtilMisc.toMap("productId", productId, "quantity", quantity), locale));
                                }
                            } else if (orderItems.size() == counter) {
                                throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoValidOrderItemFoundForProductWithEnteredQuantity", UtilMisc.toMap("productId", productId, "quantity", quantity), locale));
                            }
                        }
                    }
                } else {
                    throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoValidOrderItemFoundForProductWithEnteredQuantity", UtilMisc.toMap("productId", productId, "quantity", quantity), locale));
                }
            }
        } catch (Exception ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * <p>Create or update package lines.</p>
     * <p>Context parameters:
     * <ul>
     * <li>selInfo - selected rows</li>
     * <li>iteInfo - orderItemIds</li>
     * <li>prdInfo - productIds</li>
     * <li>pkgInfo - package numbers</li>
     * <li>wgtInfo - weights to pack</li>
     * <li>numPackagesInfo - number of packages to pack per line (>= 1, default: 1)<br/>
     * Packs the same items n times in consecutive packages, starting from the package number retrieved from pkgInfo.</li>
     * </ul>
     * </p>
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> packBulk(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String orderId = (String) context.get("orderId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        Boolean updateQuantity = (Boolean) context.get("updateQuantity");
        if (updateQuantity == null) {
            updateQuantity = Boolean.FALSE;
        }

        // set the instructions -- will clear out previous if now null
        String instructions = (String) context.get("handlingInstructions");
        session.setHandlingInstructions(instructions);

        // set the picker party id -- will clear out previous if now null
        String pickerPartyId = (String) context.get("pickerPartyId");
        session.setPickerPartyId(pickerPartyId);

        Map<String, ?> selInfo = UtilGenerics.checkMap(context.get("selInfo"));
        Map<String, String> iteInfo = UtilGenerics.checkMap(context.get("iteInfo"));
        Map<String, String> prdInfo = UtilGenerics.checkMap(context.get("prdInfo"));
        Map<String, String> qtyInfo = UtilGenerics.checkMap(context.get("qtyInfo"));
        Map<String, String> pkgInfo = UtilGenerics.checkMap(context.get("pkgInfo"));
        Map<String, String> wgtInfo = UtilGenerics.checkMap(context.get("wgtInfo"));
        Map<String, String> numPackagesInfo = UtilGenerics.checkMap(context.get("numPackagesInfo"));

        if (selInfo != null) {
            for (String rowKey: selInfo.keySet()) {
                String orderItemSeqId = iteInfo.get(rowKey);
                String prdStr = prdInfo.get(rowKey);
                if (UtilValidate.isEmpty(prdStr)) {
                    // set the productId to null if empty
                    prdStr = null;
                }

                // base package/quantity/weight strings
                String pkgStr = pkgInfo.get(rowKey);
                String qtyStr = qtyInfo.get(rowKey);
                String wgtStr = wgtInfo.get(rowKey);

                Debug.log("Item: " + orderItemSeqId + " / Product: " + prdStr + " / Quantity: " + qtyStr + " /  Package: " + pkgStr + " / Weight: " + wgtStr, module);

                // array place holders
                String[] quantities;
                String[] packages;
                String[] weights;

                // process the package array
                if (pkgStr.indexOf(",") != -1) {
                    // this is a multi-box update
                    packages = pkgStr.split(",");
                } else {
                    packages = new String[] { pkgStr };
                }

                // check to make sure there is at least one package
                if (packages == null || packages.length == 0) {
                    return ServiceUtil.returnError("No packages defined for processing.");
                }

                // process the quantity array
                if (qtyStr == null) {
                    quantities = new String[packages.length];
                    for (int p = 0; p < packages.length; p++) {
                        quantities[p] = (String) qtyInfo.get(rowKey + ":" + packages[p]);
                    }
                    if (quantities.length != packages.length) {
                        return ServiceUtil.returnError("Packages and quantities do not match.");
                    }
                } else {
                    quantities = new String[] { qtyStr };
                }

                // process the weight array
                if (UtilValidate.isEmpty(wgtStr)) wgtStr = "0";
                weights = new String[] { wgtStr };

                for (int p = 0; p < packages.length; p++) {
                    BigDecimal quantity;
                    int packageSeq;
                    BigDecimal weightSeq;
                    try {
                        quantity = new BigDecimal(quantities[p]);
                        packageSeq = Integer.parseInt(packages[p]);
                        weightSeq = new BigDecimal(weights[p]);
                    } catch (Exception e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    try {
                        String numPackagesStr = numPackagesInfo.get(rowKey);
                        int numPackages = 1;
                        if (numPackagesStr != null) {
                            try {
                                numPackages = Integer.parseInt(numPackagesStr);
                                if (numPackages < 1) {
                                    numPackages = 1;
                                }
                            } catch (NumberFormatException nex) {
                            }
                        }
                        for (int numPackage=0; numPackage<numPackages; numPackage++) {
                            session.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, prdStr, quantity, packageSeq+numPackage, weightSeq, updateQuantity.booleanValue());
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> incrementPackageSeq(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        int nextSeq = session.nextPackageSeq();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("nextPackageSeq", Integer.valueOf(nextSeq));
        return result;
    }

    public static Map<String, Object> clearLastPackage(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        int nextSeq = session.clearLastPackage();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("nextPackageSeq", Integer.valueOf(nextSeq));
        return result;
    }

    public static Map<String, Object> clearPackLine(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        String shipGroupSeqId = (String) context.get("shipGroupSeqId");
        String inventoryItemId = (String) context.get("inventoryItemId");
        String productId = (String) context.get("productId");
        Integer packageSeqId = (Integer) context.get("packageSeqId");

        PackingSessionLine line = session.findLine(orderId, orderItemSeqId, shipGroupSeqId,
                productId, inventoryItemId, packageSeqId.intValue());

        // remove the line
        if (line != null) {
            session.clearLine(line);
        } else {
            return ServiceUtil.returnError("Packing line item not found; cannot clear.");
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearPackAll(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        session.clearAllLines();

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> calcPackSessionAdditionalShippingCharge(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        Map<String, String> packageWeights = UtilGenerics.checkMap(context.get("packageWeights"));
        String weightUomId = (String) context.get("weightUomId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String productStoreId = (String) context.get("productStoreId");

        BigDecimal shippableWeight = setSessionPackageWeights(session, packageWeights);
        BigDecimal estimatedShipCost = session.getShipmentCostEstimate(shippingContactMechId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, productStoreId, null, null, shippableWeight, null);
        session.setAdditionalShippingCharge(estimatedShipCost);
        session.setWeightUomId(weightUomId);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("additionalShippingCharge", estimatedShipCost);
        return result;
    }

    public static Map<String, Object> weightPackage(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        Locale locale = (Locale) context.get("locale");
        String packageLength = (String) context.get("packageLength");
        String packageWidth = (String) context.get("packageWidth");
        String packageHeight = (String) context.get("packageHeight");
        String packageSeqId = (String) context.get("packageSeqId");
        String packageWeight = (String) context.get("packageWeight");
        String shipmentBoxTypeId = (String) context.get("shipmentBoxTypeId");
        String weightPackageSeqId = (String) context.get("weightPackageSeqId");

        // User can either enter all the dimensions or shipment box type, but not both
        if (UtilValidate.isNotEmpty(packageLength) || UtilValidate.isNotEmpty(packageWidth) || UtilValidate.isNotEmpty(packageHeight)) { // Check if user entered any dimensions
            if (UtilValidate.isNotEmpty(shipmentBoxTypeId)) { // check also if user entered shipment box type
                session.setDimensionAndShipmentBoxType(packageSeqId);
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorEnteredBothDimensionAndPackageInputBoxField", locale));
            } else if (!(UtilValidate.isNotEmpty(packageLength) && UtilValidate.isNotEmpty(packageWidth) && UtilValidate.isNotEmpty(packageHeight))) { // check if user does not enter all the dimensions
                session.setDimensionAndShipmentBoxType(packageSeqId);
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNotEnteredAllFieldsInDimension", locale));
            }
        }

        // Check package weight, it must be greater than ZERO
        if (UtilValidate.isEmpty(packageWeight) || new BigDecimal(packageWeight).compareTo(ZERO) <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorPackageWeightCannotBeNullOrZero", locale));
        }

        BigDecimal shippableWeight = ZERO;
        Map<String, Object> response = FastMap.newInstance();

        session.setPackageLength(packageSeqId, packageLength);
        session.setPackageWidth(packageSeqId, packageWidth);
        session.setPackageHeight(packageSeqId, packageHeight);
        session.setShipmentBoxTypeId(packageSeqId, shipmentBoxTypeId);

        if (UtilValidate.isNotEmpty(packageWeight)) {
            BigDecimal packWeight = new BigDecimal(packageWeight);
            session.setPackageWeight(Integer.parseInt(packageSeqId), packWeight);
            shippableWeight = shippableWeight.add(packWeight);
        } else {
            session.setPackageWeight(Integer.parseInt(packageSeqId), null);
        }
        session.setWeightPackageSeqId(packageSeqId, weightPackageSeqId);
        response.put("dimensionSavedInSession", true);
        return response;
    }

    public static Map<String, Object> holdShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        String shipmentId = (String) context.get("shipmentId");
        String dimensionUomId = (String) context.get("dimensionUomId");
        String weightUomId = (String) context.get("weightUomId");
        try {
            session.setDimensionUomId(dimensionUomId);
            session.setWeightUomId(weightUomId);
            session.createPackages(shipmentId);
            session.clearAllLines();
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> completePackage(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        PackingSession session = (PackingSession) context.get("packingSession");
        Locale locale = (Locale) context.get("locale");
        Map<String, String> packageWeights = UtilGenerics.checkMap(context.get("packageWeights"));

        String orderId = (String) context.get("orderId");
        String shipmentId = (String) context.get("shipmentId");
        String invoiceId = (String) context.get("invoiceId");
        String productStoreId = (String) context.get("productStoreId");
        String pickerPartyId = (String) context.get("pickerPartyId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String carrierRoleTypeId = (String) context.get("carrierRoleTypeId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String shipmentMethodTypeId = (String) context.get("shipmentMethodTypeId");
        String dimensionUomId = (String) context.get("dimensionUomId");
        String weightUomId = (String) context.get("weightUomId");
        Boolean forceComplete = (Boolean) context.get("forceComplete");
        List shippableItemInfo = (List) context.get("shippableItemInfo");
        BigDecimal shippableQuantity = (BigDecimal) context.get("shippableQuantity");
        BigDecimal shippableTotal = (BigDecimal) context.get("shippableTotal");

        String shipmentCostEstimateForShipGroup = (String) context.get("shipmentCostEstimateForShipGroup");
        BigDecimal estimatedShipCost = new BigDecimal(shipmentCostEstimateForShipGroup);

        BigDecimal doEstimates = new BigDecimal(UtilProperties.getPropertyValue("shipment.properties", "shipment.default.cost_actual_over_estimated_percent_allowed", "10"));

        Map<String, Object> response = FastMap.newInstance();
        BigDecimal diffInShipCostInPerc = ZERO;

        BigDecimal shippableWeight = setSessionPackageWeights(session, packageWeights);
        FastList<Map<String, Object>> packageInfo = FastList.newInstance();
        try {
            packageInfo = (FastList) session.getPackageInfo();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        BigDecimal newEstimatedShipCost = null;
        if ("UPS".equals(carrierPartyId)) {
            Map<String, Object> upsRateEstimateMap = FastMap.newInstance();
            upsRateEstimateMap.put("shippingContactMechId", shippingContactMechId);
            upsRateEstimateMap.put("shipmentMethodTypeId", shipmentMethodTypeId);
            upsRateEstimateMap.put("carrierPartyId", carrierPartyId);
            upsRateEstimateMap.put("carrierRoleTypeId", carrierRoleTypeId);
            upsRateEstimateMap.put("productStoreId", productStoreId);
            upsRateEstimateMap.put("shippableWeight", shippableWeight);
            upsRateEstimateMap.put("shippableQuantity", shippableQuantity);
            upsRateEstimateMap.put("shippableTotal", shippableTotal);
            upsRateEstimateMap.put("shippableItemInfo", shippableItemInfo);
            upsRateEstimateMap.put("packageInfo", packageInfo);
            try {
                Map<String, Object> upsRateEstimateRespose = dispatcher.runSync("upsRateEstimate", upsRateEstimateMap);
                newEstimatedShipCost = (BigDecimal) upsRateEstimateRespose.get("shippingEstimateAmount");
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            newEstimatedShipCost = session.getShipmentCostEstimate(shippingContactMechId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId, productStoreId, null, null, shippableWeight, null);
        }

        session.setAdditionalShippingCharge(newEstimatedShipCost);
        session.setDimensionUomId(dimensionUomId);
        session.setWeightUomId(weightUomId);
        session.setPickerPartyId(pickerPartyId);
        session.setShipmentId(shipmentId);
        session.setInvoiceId(invoiceId);

        try {
            session.checkPackedQty(orderId, locale);
            List<GenericValue> shipments = FastList.newInstance();
            shipments = delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId, "statusId", "SHIPMENT_PACKED"));
            for (GenericValue shipment : shipments) {
                BigDecimal additionalShippingCharge = shipment.getBigDecimal("additionalShippingCharge");
                if (UtilValidate.isNotEmpty(additionalShippingCharge)) {
                    newEstimatedShipCost = newEstimatedShipCost.add(shipment.getBigDecimal("additionalShippingCharge"));
                }
            }
            if (estimatedShipCost.compareTo(ZERO) == 0) {
                diffInShipCostInPerc = newEstimatedShipCost;
            } else {
                diffInShipCostInPerc = (((newEstimatedShipCost.subtract(estimatedShipCost)).divide(estimatedShipCost, 2, rounding)).multiply(new BigDecimal(100))).abs();
            }
            if (doEstimates.compareTo(diffInShipCostInPerc) == -1) {
                response.put("showWarningForm", true);
            } else {
                if (forceComplete == null) {
                    forceComplete = Boolean.FALSE;
                }
                try {
                    shipmentId = session.complete(forceComplete, orderId, locale);
                } catch (GeneralException e) {
                    return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
                }
                if (UtilValidate.isEmpty(shipmentId)) {
                    response = ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoItemsCurrentlySetToBeShippedCannotComplete", locale));
                } else {
                    response = ServiceUtil.returnSuccess(UtilProperties.getMessage("ProductUiLabels", "FacilityShipmentCreatedAndMarkedAsPacked", UtilMisc.toMap("shipmentId", shipmentId), locale));
                }
                response.put("shipmentId", shipmentId);
                response.put("showWarningForm", false);
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }
        return response;
    }

    public static Map<String, Object> completePack(DispatchContext dctx, Map<String, ? extends Object> context) {
        PackingSession session = (PackingSession) context.get("packingSession");
        Locale locale = (Locale) context.get("locale");

        // set the instructions -- will clear out previous if now null
        String orderId = (String) context.get("orderId");
        String shipmentId = (String) context.get("shipmentId");
        String invoiceId = (String) context.get("invoiceId");
        String instructions = (String) context.get("handlingInstructions");
        String pickerPartyId = (String) context.get("pickerPartyId");
        BigDecimal additionalShippingCharge = (BigDecimal) context.get("additionalShippingCharge");
        Map<String, String> packageWeights = UtilGenerics.checkMap(context.get("packageWeights"));
        String dimensionUomId = (String) context.get("dimensionUomId");
        String weightUomId = (String) context.get("weightUomId");
        session.setShipmentId(shipmentId);
        session.setInvoiceId(invoiceId);
        session.setHandlingInstructions(instructions);
        session.setPickerPartyId(pickerPartyId);
        session.setAdditionalShippingCharge(additionalShippingCharge);
        session.setDimensionUomId(dimensionUomId);
        session.setWeightUomId(weightUomId);
        setSessionPackageWeights(session, packageWeights);

        Boolean force = (Boolean) context.get("forceComplete");
        if (force == null) {
            force = Boolean.FALSE;
        }

        try {
            shipmentId = session.complete(force, orderId, locale);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }

        Map<String, Object> resp;
        if ("EMPTY".equals(shipmentId)) {
            resp = ServiceUtil.returnError("No items currently set to be shipped. Cannot complete!");
        } else {
            resp = ServiceUtil.returnSuccess("Shipment #" + shipmentId + " created and marked as PACKED.");
        }

        resp.put("shipmentId", shipmentId);
        return resp;
    }

    public static BigDecimal setSessionPackageWeights(PackingSession session, Map<String, String> packageWeights) {
        BigDecimal shippableWeight = BigDecimal.ZERO;
        if (! UtilValidate.isEmpty(packageWeights)) {
            for (Map.Entry<String, String> entry: packageWeights.entrySet()) {
                String packageSeqId = entry.getKey();
                String packageWeightStr = entry.getValue();
                if (UtilValidate.isNotEmpty(packageWeightStr)) {
                    BigDecimal packageWeight = new BigDecimal((String)packageWeights.get(packageSeqId));
                    session.setPackageWeight(Integer.parseInt(packageSeqId), packageWeight);
                    shippableWeight = shippableWeight.add(packageWeight);
                } else {
                    session.setPackageWeight(Integer.parseInt(packageSeqId), null);
                }
            }
        }
        return shippableWeight;
    }
}
