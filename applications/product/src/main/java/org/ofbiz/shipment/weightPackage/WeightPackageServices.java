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
package org.ofbiz.shipment.weightPackage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class WeightPackageServices {

    private static BigDecimal ZERO = BigDecimal.ZERO;

    public static Map<String, Object> setPackageInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");
        Locale locale = (Locale) context.get("locale");

        String orderId = (String) context.get("orderId");
        BigDecimal packageWeight = (BigDecimal) context.get("packageWeight");
        BigDecimal packageLength = (BigDecimal) context.get("packageLength");
        BigDecimal packageWidth = (BigDecimal) context.get("packageWidth");
        BigDecimal packageHeight = (BigDecimal) context.get("packageHeight");
        String shipmentBoxTypeId = (String) context.get("shipmentBoxTypeId");

        // User can either enter all the dimensions or shipment box type, but not both
        if (UtilValidate.isNotEmpty(packageLength) || UtilValidate.isNotEmpty(packageWidth) || UtilValidate.isNotEmpty(packageHeight)) { // Check if user entered any dimensions
            if (UtilValidate.isNotEmpty(shipmentBoxTypeId)) { // check also if user entered shipment box type
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorEnteredBothDimensionAndPackageInputBoxField", locale));
            } else if (!(UtilValidate.isNotEmpty(packageLength) && UtilValidate.isNotEmpty(packageWidth) && UtilValidate.isNotEmpty(packageHeight))) { // check if user does not enter all the dimensions
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNotEnteredAllFieldsInDimension", locale));
            }
        }
        // Check package weight, it must be greater than ZERO
        if (UtilValidate.isEmpty(packageWeight) || packageWeight.compareTo(ZERO) <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorPackageWeightCannotBeNullOrZero", locale));
        }
        try {
            // Checked no of packages, it should not be greater than ordered quantity
            List<GenericValue> orderItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "statusId", "ITEM_APPROVED").queryList();
            BigDecimal orderedItemQty = ZERO;
            for (GenericValue orderItem : orderItems) {
                orderedItemQty = orderedItemQty.add(orderItem.getBigDecimal("quantity"));
            }
            int packageQuantity = weightPackageSession.getPackedLines(orderId).size();
            if ((orderedItemQty.intValue() - packageQuantity) > 0) {
                weightPackageSession.createWeightPackageLine(orderId, packageWeight, packageLength, packageWidth, packageHeight, shipmentBoxTypeId);
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNumberOfPackageCannotBeGreaterThanTheNumberOfOrderedQuantity", locale));
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updatePackedLine(DispatchContext dctx, Map<String, ? extends Object> context) {
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");
        Locale locale = (Locale) context.get("locale");

        BigDecimal packageWeight = (BigDecimal) context.get("packageWeight");
        BigDecimal packageLength = (BigDecimal) context.get("packageLength");
        BigDecimal packageWidth = (BigDecimal) context.get("packageWidth");
        BigDecimal packageHeight = (BigDecimal) context.get("packageHeight");
        String shipmentBoxTypeId = (String) context.get("shipmentBoxTypeId");
        Integer weightPackageSeqId = (Integer) context.get("weightPackageSeqId");

        // User can either enter all the dimensions or shipment box type, but not both
        if (UtilValidate.isNotEmpty(packageLength) || UtilValidate.isNotEmpty(packageWidth) || UtilValidate.isNotEmpty(packageHeight)) { // Check if user entered any dimensions
            if (UtilValidate.isNotEmpty(shipmentBoxTypeId)) { // check also if user entered shipment box type
                weightPackageSession.setDimensionAndShipmentBoxType(weightPackageSeqId);
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorEnteredBothDimensionAndPackageInputBoxField", locale));
            } else if (!(UtilValidate.isNotEmpty(packageLength) && UtilValidate.isNotEmpty(packageWidth) && UtilValidate.isNotEmpty(packageHeight))) { // check if user does not enter all the dimensions
                weightPackageSession.setDimensionAndShipmentBoxType(weightPackageSeqId);
                return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNotEnteredAllFieldsInDimension", locale));
            }
        }

        // Check package weight, it must be greater than ZERO
        if (UtilValidate.isEmpty(packageWeight) || packageWeight.compareTo(ZERO) <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorPackageWeightCannotBeNullOrZero", locale));
        }

        weightPackageSession.setPackageWeight(packageWeight, weightPackageSeqId);
        weightPackageSession.setPackageLength(packageLength, weightPackageSeqId);
        weightPackageSession.setPackageWidth(packageWidth, weightPackageSeqId);
        weightPackageSession.setPackageHeight(packageHeight, weightPackageSeqId);
        weightPackageSession.setShipmentBoxTypeId(shipmentBoxTypeId, weightPackageSeqId);

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> deletePackedLine(DispatchContext dctx, Map<String, ? extends Object> context) {
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");
        Integer weightPackageSeqId = (Integer) context.get("weightPackageSeqId");

        weightPackageSession.deletePackedLine(weightPackageSeqId);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> completePackage(DispatchContext dctx, Map<String, ? extends Object> context) {
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");
        Locale locale = (Locale) context.get("locale");

        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipmentId = (String) context.get("shipmentId");
        String invoiceId = (String) context.get("invoiceId");
        String dimensionUomId = (String) context.get("dimensionUomId");
        String weightUomId = (String) context.get("weightUomId");
        BigDecimal estimatedShippingCost = (BigDecimal) context.get("estimatedShippingCost");
        BigDecimal newEstimatedShippingCost = (BigDecimal) context.get("newEstimatedShippingCost");

        if (UtilValidate.isEmpty(newEstimatedShippingCost)) {
            newEstimatedShippingCost = ZERO;
        }

        weightPackageSession.setDimensionUomId(dimensionUomId);
        weightPackageSession.setWeightUomId(weightUomId);
        weightPackageSession.setShipmentId(shipmentId);
        weightPackageSession.setInvoiceId(invoiceId);
        weightPackageSession.setEstimatedShipCost(estimatedShippingCost);
        weightPackageSession.setActualShipCost(newEstimatedShippingCost);

        Map<String, Object> response = new HashMap<String, Object>();
        try {
            String getActualShippingQuoteFromUps = EntityUtilProperties.getPropertyValue("shipment", "shipment.ups.shipping", "N", delegator);
            String result = weightPackageSession.complete(orderId, locale, getActualShippingQuoteFromUps);
            if ("showWarningForm".equals(result)) {
                response.put("showWarningForm", true);
            } else if ("success".equals(result)) {
                response.put("shipmentId", shipmentId);
            } else {
                response = ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoItemsCurrentlySetToBeShippedCannotComplete", locale));
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }
        return response;
    }

    public static Map<String, Object> completeShipment(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");

        String shipmentId = (String) context.get("shipmentId");
        String orderId = (String) context.get("orderId");

        Map<String, Object> response = new HashMap<String, Object>();
        try {
            String getActualShippingQuoteFromUps = EntityUtilProperties.getPropertyValue("shipment", "shipment.ups.shipping", "N", delegator);
            if (weightPackageSession.completeShipment(orderId, getActualShippingQuoteFromUps)) {
                response.put("shipmentId", shipmentId);
            } else {
                response = ServiceUtil.returnError(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorNoItemsCurrentlySetToBeShippedCannotComplete", locale));
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage(), e.getMessageList());
        }
        return response;
    }

    public static Map<String, Object> savePackagesInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        WeightPackageSession weightPackageSession = (WeightPackageSession) context.get("weightPackageSession");

        String orderId = (String) context.get("orderId");

        String getActualShippingQuoteFromUps = EntityUtilProperties.getPropertyValue("shipment", "shipment.ups.shipping", "N", delegator);
        try {
            weightPackageSession.savePackagesInfo(orderId, getActualShippingQuoteFromUps);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess(UtilProperties.getMessage("ProductUiLabels", "FacilityThereIsProblemOccurredInPaymentCapture", locale));
    }

}
