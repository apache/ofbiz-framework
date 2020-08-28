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
package org.apache.ofbiz.shipment.weightPackage;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * The type Weight package session.
 */
@SuppressWarnings("serial")
public class WeightPackageSession implements Serializable {

    private static final String MODULE = WeightPackageSession.class.getName();

    private GenericValue userLogin = null;
    private String dispatcherName = null;
    private String delegatorName = null;
    private String primaryOrderId = null;
    private String primaryShipGrpSeqId = null;
    private String picklistBinId = null;
    private String shipmentId = null;
    private String invoiceId = null;
    private String facilityId = null;
    private String carrierPartyId = null;
    private String dimensionUomId = null;
    private String weightUomId = null;
    private BigDecimal estimatedShipCost = null;
    private BigDecimal actualShipCost = null;
    private int weightPackageSeqId = 1;
    private List<WeightPackageSessionLine> weightPackageLines = null;

    private transient Delegator delegator = null;
    private transient LocalDispatcher dispatcher = null;
    private static final RoundingMode ROUNDING_MODE = UtilNumber.getRoundingMode("invoice.rounding");

    public WeightPackageSession() {
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId, String picklistBinId,
                                String orderId, String shipGrpSeqId) {
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();

        this.delegator = dispatcher.getDelegator();
        this.delegatorName = delegator.getDelegatorName();

        this.primaryOrderId = orderId;
        this.primaryShipGrpSeqId = shipGrpSeqId;
        this.picklistBinId = picklistBinId;
        this.userLogin = userLogin;
        this.facilityId = facilityId;
        this.weightPackageLines = new LinkedList<>();
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null, null);
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null, null);
    }

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    /**
     * Create weight package line.
     * @param orderId the order id
     * @param packageWeight the package weight
     * @param packageLength the package length
     * @param packageWidth the package width
     * @param packageHeight the package height
     * @param shipmentBoxTypeId the shipment box type id
     * @throws GeneralException the general exception
     */
    public void createWeightPackageLine(String orderId, BigDecimal packageWeight, BigDecimal packageLength, BigDecimal packageWidth,
                                        BigDecimal packageHeight, String shipmentBoxTypeId) throws GeneralException {
        weightPackageLines.add(new WeightPackageSessionLine(orderId, packageWeight, packageLength, packageWidth, packageHeight, shipmentBoxTypeId,
                this.weightPackageSeqId));
        this.weightPackageSeqId++;
    }

    /**
     * Gets weight package seq id.
     * @return the weight package seq id
     */
    public int getWeightPackageSeqId() {
        return this.weightPackageSeqId;
    }

    /**
     * Gets facility id.
     * @return the facility id
     */
    public String getFacilityId() {
        return this.facilityId;
    }

    /**
     * Sets facility id.
     * @param facilityId the facility id
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Gets primary order id.
     * @return the primary order id
     */
    public String getPrimaryOrderId() {
        return this.primaryOrderId;
    }

    /**
     * Sets primary order id.
     * @param primaryOrderId the primary order id
     */
    public void setPrimaryOrderId(String primaryOrderId) {
        this.primaryOrderId = primaryOrderId;
    }

    /**
     * Gets primary ship group seq id.
     * @return the primary ship group seq id
     */
    public String getPrimaryShipGroupSeqId() {
        return this.primaryShipGrpSeqId;
    }

    /**
     * Sets primary ship group seq id.
     * @param primaryShipGrpSeqId the primary ship grp seq id
     */
    public void setPrimaryShipGroupSeqId(String primaryShipGrpSeqId) {
        this.primaryShipGrpSeqId = primaryShipGrpSeqId;
    }

    /**
     * Sets picklist bin id.
     * @param picklistBinId the picklist bin id
     */
    public void setPicklistBinId(String picklistBinId) {
        this.picklistBinId = picklistBinId;
    }

    /**
     * Gets picklist bin id.
     * @return the picklist bin id
     */
    public String getPicklistBinId() {
        return this.picklistBinId;
    }

    /**
     * Sets estimated ship cost.
     * @param estimatedShipCost the estimated ship cost
     */
    public void setEstimatedShipCost(BigDecimal estimatedShipCost) {
        this.estimatedShipCost = estimatedShipCost;
    }

    /**
     * Gets estimated ship cost.
     * @return the estimated ship cost
     */
    public BigDecimal getEstimatedShipCost() {
        return this.estimatedShipCost;
    }

    /**
     * Sets actual ship cost.
     * @param actualShipCost the actual ship cost
     */
    public void setActualShipCost(BigDecimal actualShipCost) {
        this.actualShipCost = actualShipCost;
    }

    /**
     * Gets actual ship cost.
     * @return the actual ship cost
     */
    public BigDecimal getActualShipCost() {
        return this.actualShipCost;
    }

    /**
     * Gets shipment id.
     * @return the shipment id
     */
    public String getShipmentId() {
        return this.shipmentId;
    }

    /**
     * Sets shipment id.
     * @param shipmentId the shipment id
     */
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    /**
     * Gets invoice id.
     * @return the invoice id
     */
    public String getInvoiceId() {
        return this.invoiceId;
    }

    /**
     * Sets invoice id.
     * @param invoiceId the invoice id
     */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /**
     * Gets weight uom id.
     * @return the weight uom id
     */
    public String getWeightUomId() {
        return weightUomId;
    }

    /**
     * Sets weight uom id.
     * @param weightUomId the weight uom id
     */
    public void setWeightUomId(String weightUomId) {
        this.weightUomId = weightUomId;
    }

    /**
     * Gets dimension uom id.
     * @return the dimension uom id
     */
    public String getDimensionUomId() {
        return dimensionUomId;
    }

    /**
     * Sets carrier party id.
     * @param carrierPartyId the carrier party id
     */
    public void setCarrierPartyId(String carrierPartyId) {
        this.carrierPartyId = carrierPartyId;
    }

    /**
     * Sets dimension uom id.
     * @param dimensionUomId the dimension uom id
     */
    public void setDimensionUomId(String dimensionUomId) {
        this.dimensionUomId = dimensionUomId;
    }

    /**
     * Gets shippable weight.
     * @param orderId the order id
     * @return the shippable weight
     */
    public BigDecimal getShippableWeight(String orderId) {
        BigDecimal shippableWeight = BigDecimal.ZERO;
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            shippableWeight = shippableWeight.add(packedLine.getPackageWeight());
        }
        return shippableWeight;
    }

    /**
     * Gets packed lines.
     * @return the packed lines
     */
    public List<WeightPackageSessionLine> getPackedLines() {
        return this.weightPackageLines;
    }

    /**
     * Gets packed lines.
     * @param orderId the order id
     * @return the packed lines
     */
    public List<WeightPackageSessionLine> getPackedLines(String orderId) {
        List<WeightPackageSessionLine> packedLines = new LinkedList<>();
        if (UtilValidate.isNotEmpty(orderId)) {
            for (WeightPackageSessionLine packedLine: this.getPackedLines()) {
                if (orderId.equals(packedLine.getOrderId())) {
                    packedLines.add(packedLine);
                }
            }
        }
        return packedLines;
    }

    /**
     * Gets packed line.
     * @param weightPackageSeqId the weight package seq id
     * @return the packed line
     */
    public WeightPackageSessionLine getPackedLine(int weightPackageSeqId) {
        WeightPackageSessionLine packedLine = null;
        if (weightPackageSeqId > 0) {
            for (WeightPackageSessionLine line : this.getPackedLines()) {
                if ((line.getWeightPackageSeqId()) == weightPackageSeqId) {
                    packedLine = line;
                }
            }
        }
        return packedLine;
    }

    /**
     * Sets package weight.
     * @param packageWeight the package weight
     * @param weightPackageSeqId the weight package seq id
     */
    public void setPackageWeight(BigDecimal packageWeight, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine)) {
                packedLine.setPackageWeight(packageWeight);
            }
        }
    }

    /**
     * Sets package length.
     * @param packageLength the package length
     * @param weightPackageSeqId the weight package seq id
     */
    public void setPackageLength(BigDecimal packageLength, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine)) {
                packedLine.setPackageLength(packageLength);
            }
        }
    }

    /**
     * Sets package width.
     * @param packageWidth the package width
     * @param weightPackageSeqId the weight package seq id
     */
    public void setPackageWidth(BigDecimal packageWidth, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine)) {
                packedLine.setPackageWidth(packageWidth);
            }
        }
    }

    /**
     * Sets package height.
     * @param packageHeight the package height
     * @param weightPackageSeqId the weight package seq id
     */
    public void setPackageHeight(BigDecimal packageHeight, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine)) {
                packedLine.setPackageHeight(packageHeight);
            }
        }
    }

    /**
     * Sets shipment box type id.
     * @param shipmentBoxTypeId the shipment box type id
     * @param weightPackageSeqId the weight package seq id
     */
    public void setShipmentBoxTypeId(String shipmentBoxTypeId, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine)) {
                packedLine.setShipmentBoxTypeId(shipmentBoxTypeId);
            }
        }
    }

    /**
     * Delete packed line.
     * @param weightPackageSeqId the weight package seq id
     */
    public void deletePackedLine(int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            this.weightPackageLines.remove(packedLine);
        }
    }

    /**
     * Sets dimension and shipment box type.
     * @param weightPackageSeqId the weight package seq id
     */
    public void setDimensionAndShipmentBoxType(int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            packedLine.setPackageLength(null);
            packedLine.setPackageWidth(null);
            packedLine.setPackageHeight(null);
            packedLine.setShipmentBoxTypeId(null);
        }
    }

    /**
     * Clear packed lines.
     * @param orderId the order id
     */
    public void clearPackedLines(String orderId) {
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            this.weightPackageLines.remove(packedLine);
        }
    }

    /**
     * Complete string.
     * @param orderId the order id
     * @param locale the locale
     * @param calculateOnlineShippingRateFromUps the calculate online shipping rate from ups
     * @return the string
     * @throws GeneralException the general exception
     */
    public String complete(String orderId, Locale locale, String calculateOnlineShippingRateFromUps) throws GeneralException {
        //create the package(s)
        this.createPackages(orderId);
        // calculate the actual shipping charges according to package(s) weight and dimensions
        BigDecimal actualShippingCost;
        // Check if UPS integration is done
        if ("UPS".equals(this.carrierPartyId) && "Y".equals(calculateOnlineShippingRateFromUps)) {
            // call upsShipmentConfirm service, it will calculate the online shipping rate from UPS
            // and save in ShipmentRouteSegment entity in actualCost field
            actualShippingCost = this.upsShipmentConfirm();
        } else {
            // calculate the shipping charges manually
            actualShippingCost = this.getActualShipCost();
        }
        // calculate the difference between estimated shipping charges and actual shipping charges
        if (diffInShipCost(actualShippingCost)) {
            return "showWarningForm";
        } else if ("UPS".equals(this.carrierPartyId) && "Y".equals(calculateOnlineShippingRateFromUps)) {
            // call upsShipmentAccept service, it will made record(s) in ShipmentPackageRouteSeg entity
            this.upsShipmentAccept();
        }
        // change order item(s) status
        this.changeOrderItemStatus(orderId);
        // assign item(s) to package(s)
        this.applyItemsToPackages(orderId);
        // update the ShipmentRouteSegments with total weight and weightUomId
        this.updateShipmentRouteSegments(orderId);
        // set the shipment to packed
        this.setShipmentToPacked();

        return "success";
    }

    /**
     * Complete shipment boolean.
     * @param orderId the order id
     * @param calculateOnlineShippingRateFromUps the calculate online shipping rate from ups
     * @return the boolean
     * @throws GeneralException the general exception
     */
    public boolean completeShipment(String orderId, String calculateOnlineShippingRateFromUps) throws GeneralException {
        // Check if UPS integration is done
        if ("UPS".equals(this.carrierPartyId) && "Y".equals(calculateOnlineShippingRateFromUps)) {
            // call upsShipmentAccept service, it will made record(s) in ShipmentPackageRouteSeg entity
            this.upsShipmentAccept();
        }
        // change order item(s) status
        this.changeOrderItemStatus(orderId);
        // assign item(s) to package(s)
        this.applyItemsToPackages(orderId);
        // update the ShipmentRouteSegments with total weight and weightUomId
        this.updateShipmentRouteSegments(orderId);
        // set the shipment to packed
        this.setShipmentToPacked();

        return true;
    }

    /**
     * Ups shipment confirm big decimal.
     * @return the big decimal
     * @throws GeneralException the general exception
     */
    protected BigDecimal upsShipmentConfirm() throws GeneralException {
        Delegator delegator = this.getDelegator();
        BigDecimal actualCost = BigDecimal.ZERO;
        List<GenericValue> shipmentRouteSegments = EntityQuery.use(delegator).from("ShipmentRouteSegment")
                .where("shipmentId", shipmentId).queryList();
        if (UtilValidate.isNotEmpty(shipmentRouteSegments)) {
            for (GenericValue shipmentRouteSegment : shipmentRouteSegments) {
                Map<String, Object> shipmentRouteSegmentMap = new HashMap<>();
                shipmentRouteSegmentMap.put("shipmentId", shipmentId);
                shipmentRouteSegmentMap.put("shipmentRouteSegmentId", shipmentRouteSegment.getString("shipmentRouteSegmentId"));
                shipmentRouteSegmentMap.put("userLogin", userLogin);
                Map<String, Object> shipmentRouteSegmentResult = this.getDispatcher().runSync("upsShipmentConfirm", shipmentRouteSegmentMap);
                if (ServiceUtil.isError(shipmentRouteSegmentResult)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(shipmentRouteSegmentResult));
                }
                GenericValue shipRouteSeg = EntityQuery.use(delegator).from("ShipmentRouteSegment")
                        .where("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentRouteSegment.getString("shipmentRouteSegmentId"))
                        .queryOne();
                actualCost = actualCost.add(shipRouteSeg.getBigDecimal("actualCost"));
            }
        }
        return actualCost;
    }

    /**
     * Ups shipment accept.
     * @throws GeneralException the general exception
     */
    protected void upsShipmentAccept() throws GeneralException {
        List<GenericValue> shipmentRouteSegments = this.getDelegator().findByAnd("ShipmentRouteSegment",
                UtilMisc.toMap("shipmentId", shipmentId), null, false);
        if (UtilValidate.isNotEmpty(shipmentRouteSegments)) {
            for (GenericValue shipmentRouteSegment : shipmentRouteSegments) {
                Map<String, Object> shipmentRouteSegmentMap = new HashMap<>();
                shipmentRouteSegmentMap.put("shipmentId", shipmentId);
                shipmentRouteSegmentMap.put("shipmentRouteSegmentId", shipmentRouteSegment.getString("shipmentRouteSegmentId"));
                shipmentRouteSegmentMap.put("userLogin", userLogin);
                Map<String, Object> shipmentRouteSegmentResult = this.getDispatcher().runSync("upsShipmentAccept", shipmentRouteSegmentMap);
                if (ServiceUtil.isError(shipmentRouteSegmentResult)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(shipmentRouteSegmentResult));
                }
            }
        }
    }

    /**
     * Diff in ship cost boolean.
     * @param actualShippingCost the actual shipping cost
     * @return the boolean
     * @throws GeneralException the general exception
     */
    protected boolean diffInShipCost(BigDecimal actualShippingCost) throws GeneralException {
        BigDecimal estimatedShipCost = this.getEstimatedShipCost();
        BigDecimal doEstimates = new BigDecimal(UtilProperties.getPropertyValue("shipment",
                "shipment.default.cost_actual_over_estimated_percent_allowed", "10"));
        BigDecimal diffInShipCostInPerc;
        if (estimatedShipCost.compareTo(BigDecimal.ZERO) == 0) {
            diffInShipCostInPerc = actualShippingCost;
        } else {
            diffInShipCostInPerc = (((actualShippingCost.subtract(estimatedShipCost)).divide(estimatedShipCost, 2, ROUNDING_MODE))
                    .multiply(new BigDecimal(100))).abs();
        }
        return doEstimates.compareTo(diffInShipCostInPerc) == -1;
    }

    /**
     * Create packages.
     * @param orderId the order id
     * @throws GeneralException the general exception
     */
    protected void createPackages(String orderId) throws GeneralException {
        int shipPackSeqId = 0;
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(++shipPackSeqId, 5);

            Map<String, Object> shipmentPackageMap = new HashMap<>();
            shipmentPackageMap.put("shipmentId", shipmentId);
            shipmentPackageMap.put("shipmentPackageSeqId", shipmentPackageSeqId);
            shipmentPackageMap.put("weight", packedLine.getPackageWeight());
            shipmentPackageMap.put("boxLength", packedLine.getPackageLength());
            shipmentPackageMap.put("boxWidth", packedLine.getPackageWidth());
            shipmentPackageMap.put("boxHeight", packedLine.getPackageHeight());
            shipmentPackageMap.put("dimensionUomId", getDimensionUomId());
            shipmentPackageMap.put("shipmentBoxTypeId", packedLine.getShipmentBoxTypeId());
            shipmentPackageMap.put("weightUomId", getWeightUomId());
            shipmentPackageMap.put("userLogin", userLogin);

            Map<String, Object> shipmentPackageResult;
            GenericValue shipmentPackage = this.getDelegator().findOne("ShipmentPackage", UtilMisc.toMap("shipmentId", shipmentId,
                    "shipmentPackageSeqId", shipmentPackageSeqId), false);
            if (UtilValidate.isEmpty(shipmentPackage)) {
                shipmentPackageResult = this.getDispatcher().runSync("createShipmentPackage", shipmentPackageMap);
            } else {
                shipmentPackageResult = this.getDispatcher().runSync("updateShipmentPackage", shipmentPackageMap);
            }
            if (ServiceUtil.isError(shipmentPackageResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(shipmentPackageResult));
            }
        }
    }

    /**
     * Change order item status.
     * @param orderId the order id
     * @throws GeneralException the general exception
     */
    protected void changeOrderItemStatus(String orderId) throws GeneralException {
        List<GenericValue> shipmentItems = this.getDelegator().findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId), null, false);
        for (GenericValue shipmentItem : shipmentItems) {
            for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
                packedLine.setShipmentItemSeqId(shipmentItem.getString("shipmentItemSeqId"));
            }
        }
        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId, "statusId", "ITEM_APPROVED"),
                null, false);
        for (GenericValue orderItem : orderItems) {
            List<GenericValue> orderItemShipGrpInvReserves = orderItem.getRelated("OrderItemShipGrpInvRes", null, null, false);
            if (UtilValidate.isEmpty(orderItemShipGrpInvReserves)) {
                Map<String, Object> orderItemStatusMap = new HashMap<>();
                orderItemStatusMap.put("orderId", orderId);
                orderItemStatusMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                orderItemStatusMap.put("userLogin", userLogin);
                orderItemStatusMap.put("statusId", "ITEM_COMPLETED");
                Map<String, Object> orderItemStatusResult = this.getDispatcher().runSync("changeOrderItemStatus", orderItemStatusMap);
                if (ServiceUtil.isError(orderItemStatusResult)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(orderItemStatusResult));
                }
            }
        }
    }

    /**
     * Apply items to packages.
     * @param orderId the order id
     * @throws GeneralException the general exception
     */
    protected void applyItemsToPackages(String orderId) throws GeneralException {
        if (UtilValidate.isNotEmpty(orderId) && UtilValidate.isNotEmpty(this.getPackedLines(orderId))) {
            int shipPackSeqId = 0;
            for (WeightPackageSessionLine line: this.getPackedLines(orderId)) {
                line.applyLineToPackage(shipmentId, userLogin, getDispatcher(), ++shipPackSeqId);
            }
        }
    }

    /**
     * Update shipment route segments.
     * @param orderId the order id
     * @throws GeneralException the general exception
     */
    protected void updateShipmentRouteSegments(String orderId) throws GeneralException {
        if (UtilValidate.isNotEmpty(orderId)) {
            BigDecimal shipmentWeight = getShippableWeight(orderId);
            if (UtilValidate.isNotEmpty(shipmentWeight) && shipmentWeight.compareTo(BigDecimal.ZERO) <= 0) return;
            List<GenericValue> shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment",
                    UtilMisc.toMap("shipmentId", this.getShipmentId()), null, false);
            if (UtilValidate.isNotEmpty(shipmentRouteSegments)) {
                for (GenericValue shipmentRouteSegment : shipmentRouteSegments) {
                    shipmentRouteSegment.set("billingWeight", shipmentWeight);
                    shipmentRouteSegment.set("billingWeightUomId", getWeightUomId());
                }
                getDelegator().storeAll(shipmentRouteSegments);
            }
        }
    }

    /**
     * Sets shipment to packed.
     * @throws GeneralException the general exception
     */
    protected void setShipmentToPacked() throws GeneralException {
        Map<String, Object> shipmentMap = new HashMap<>();
        shipmentMap.put("shipmentId", shipmentId);
        shipmentMap.put("statusId", "SHIPMENT_PACKED");
        shipmentMap.put("userLogin", userLogin);
        Map<String, Object> shipmentResult = this.getDispatcher().runSync("updateShipment", shipmentMap);
        if (UtilValidate.isNotEmpty(shipmentResult) && ServiceUtil.isError(shipmentResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(shipmentResult));
        }
    }

    /**
     * Gets shipment cost estimate.
     * @param orderItemShipGroup the order item ship group
     * @param orderId the order id
     * @param productStoreId the product store id
     * @param shippableItemInfo the shippable item info
     * @param shippableTotal the shippable total
     * @param shippableWeight the shippable weight
     * @param shippableQuantity the shippable quantity
     * @return the shipment cost estimate
     */
    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String orderId, String productStoreId, List<GenericValue>
            shippableItemInfo, BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       orderId, productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }

    /**
     * Gets shipment cost estimate.
     * @param shippingContactMechId the shipping contact mech id
     * @param shipmentMethodTypeId the shipment method type id
     * @param carrierPartyId the carrier party id
     * @param carrierRoleTypeId the carrier role type id
     * @param orderId the order id
     * @param productStoreId the product store id
     * @param shippableItemInfo the shippable item info
     * @param shippableTotal the shippable total
     * @param shippableWeight the shippable weight
     * @param shippableQuantity the shippable quantity
     * @return the shipment cost estimate
     */
    public BigDecimal getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId,
            String carrierRoleTypeId, String orderId, String productStoreId, List<GenericValue> shippableItemInfo, BigDecimal shippableTotal,
                                              BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        BigDecimal shipmentCostEstimate = BigDecimal.ZERO;
        Map<String, Object> shipCostEstimateResult = null;
        try {
            Map<String, Object> shipCostEstimateMap = new HashMap<>();
            shipCostEstimateMap.put("shippingContactMechId", shippingContactMechId);
            shipCostEstimateMap.put("shipmentMethodTypeId", shipmentMethodTypeId);
            shipCostEstimateMap.put("carrierPartyId", carrierPartyId);
            shipCostEstimateMap.put("carrierRoleTypeId", carrierRoleTypeId);
            shipCostEstimateMap.put("productStoreId", productStoreId);
            shipCostEstimateMap.put("shippableItemInfo", shippableItemInfo);
            if (UtilValidate.isEmpty(shippableWeight) && UtilValidate.isNotEmpty(orderId)) {
                shippableWeight = this.getShippableWeight(orderId);
            }
            shipCostEstimateMap.put("shippableWeight", shippableWeight);
            shipCostEstimateMap.put("shippableQuantity", shippableQuantity);
            if (UtilValidate.isEmpty(shippableTotal)) {
                shippableTotal = BigDecimal.ZERO;
            }
            shipCostEstimateMap.put("shippableTotal", shippableTotal);
            shipCostEstimateResult = getDispatcher().runSync("calcShipmentCostEstimate", shipCostEstimateMap);
            if (ServiceUtil.isError(shipCostEstimateResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(shipCostEstimateResult), MODULE);
            }
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
        }
        if (UtilValidate.isNotEmpty(shipCostEstimateResult.get("shippingEstimateAmount"))) {
            shipmentCostEstimate = (BigDecimal) shipCostEstimateResult.get("shippingEstimateAmount");
        }
        return shipmentCostEstimate;
    }

    /**
     * Save packages info.
     * @param orderId the order id
     * @param calculateOnlineShippingRateFromUps the calculate online shipping rate from ups
     * @throws GeneralException the general exception
     */
    protected void savePackagesInfo(String orderId, String calculateOnlineShippingRateFromUps) throws GeneralException {
        //create the package(s)
        this.createPackages(orderId);
        // Check if UPS integration is done
        if ("UPS".equals(this.carrierPartyId) && "Y".equals(calculateOnlineShippingRateFromUps)) {
            // call upsShipmentConfirm service, it will calculate the online shipping rate from UPS and save in
            // ShipmentRouteSegment entity in actualCost field
            this.upsShipmentConfirm();
        }
    }

    /**
     * Gets ordered quantity.
     * @param orderId the order id
     * @return the ordered quantity
     */
    protected Integer getOrderedQuantity(String orderId) {
        BigDecimal orderedQuantity = BigDecimal.ZERO;
        try {
            List<GenericValue> orderItems = getDelegator().findByAnd("OrderItem",
                    UtilMisc.toMap("orderId", orderId, "statusId", "ITEM_APPROVED"), null, false);
            for (GenericValue orderItem : orderItems) {
                orderedQuantity = orderedQuantity.add(orderItem.getBigDecimal("quantity"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return orderedQuantity.intValue();
    }

}
