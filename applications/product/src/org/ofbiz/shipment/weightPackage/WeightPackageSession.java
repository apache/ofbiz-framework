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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.shipment.weightPackage.WeightPackageSession;
import org.ofbiz.shipment.weightPackage.WeightPackageSessionLine;

public class WeightPackageSession implements Serializable {

    public static final String module = WeightPackageSession.class.getName();

    protected GenericValue userLogin = null;
    protected String dispatcherName = null;
    protected String delegatorName = null;
    protected String primaryOrderId = null;
    protected String primaryShipGrpSeqId = null;
    protected String picklistBinId = null;
    protected String shipmentId = null;
    protected String invoiceId = null;
    protected String facilityId = null;
    protected String dimensionUomId = null;
    protected String weightUomId = null;
    protected BigDecimal estimatedShipCost = null;
    protected int weightPackageSeqId = 1;
    protected List<WeightPackageSessionLine> weightPackageLines = null;

    private transient GenericDelegator _delegator = null;
    private transient LocalDispatcher _dispatcher = null;
    private static BigDecimal ZERO = BigDecimal.ZERO;

    public WeightPackageSession() {
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId, String picklistBinId, String orderId, String shipGrpSeqId) {
        this._dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();

        this._delegator = _dispatcher.getDelegator();
        this.delegatorName = _delegator.getDelegatorName();

        this.primaryOrderId = orderId;
        this.primaryShipGrpSeqId = shipGrpSeqId;
        this.picklistBinId = picklistBinId;
        this.userLogin = userLogin;
        this.facilityId = facilityId;
        this.weightPackageLines = FastList.newInstance();
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null, null);
    }

    public WeightPackageSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null, null);
    }

    public LocalDispatcher getDispatcher() {
        if (_dispatcher == null) {
            _dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return _dispatcher;
    }

    public GenericDelegator getDelegator() {
        if (_delegator == null) {
            _delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        return _delegator;
    }

    public void createWeightPackageLine(String orderId, BigDecimal packageWeight, BigDecimal packageLength, BigDecimal packageWidth, BigDecimal packageHeight, String shipmentBoxTypeId) throws GeneralException {
        weightPackageLines.add(new WeightPackageSessionLine(orderId, packageWeight, packageLength, packageWidth, packageHeight, shipmentBoxTypeId, this.weightPackageSeqId));
        this.weightPackageSeqId++;
    }

    public int getWeightPackageSeqId() {
        return this.weightPackageSeqId;
    }

    public String getFacilityId() {
        return this.facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public String getPrimaryOrderId() {
        return this.primaryOrderId;
    }

    public void setPrimaryOrderId(String primaryOrderId) {
        this.primaryOrderId = primaryOrderId;
    }

    public String getPrimaryShipGroupSeqId() {
        return this.primaryShipGrpSeqId;
    }

    public void setPrimaryShipGroupSeqId(String primaryShipGrpSeqId) {
        this.primaryShipGrpSeqId = primaryShipGrpSeqId;
    }

    public void setPicklistBinId(String picklistBinId) {
        this.picklistBinId = picklistBinId;
    }

    public String getPicklistBinId() {
        return this.picklistBinId;
    }

    public void setEstimatedShipCost(BigDecimal estimatedShipCost) {
        this.estimatedShipCost = estimatedShipCost;
    }

    public BigDecimal getEstimatedShipCost() {
        return this.estimatedShipCost;
    }

    public String getShipmentId() {
        return this.shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getInvoiceId() {
        return this.invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getWeightUomId() {
        return weightUomId;
    }

    public void setWeightUomId(String weightUomId) {
        this.weightUomId = weightUomId;
    }

    public String getDimensionUomId() {
        return dimensionUomId;
    }

    public void setDimensionUomId(String dimensionUomId) {
        this.dimensionUomId = dimensionUomId;
    }

    public BigDecimal getShippableWeight(String orderId) {
        BigDecimal shippableWeight = ZERO;
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            shippableWeight = shippableWeight.add(packedLine.getPackageWeight());
        }
        return shippableWeight;
    }

    public List<WeightPackageSessionLine> getPackedLines() {
        return this.weightPackageLines;
    }

    public List<WeightPackageSessionLine> getPackedLines(String orderId) {
        List<WeightPackageSessionLine> packedLines = FastList.newInstance();
        if (UtilValidate.isNotEmpty(orderId)) {
            for (WeightPackageSessionLine packedLine: this.getPackedLines()) {
               if (orderId.equals(packedLine.getOrderId()))
                    packedLines.add(packedLine);
            }
        }
        return packedLines;
    }

    public WeightPackageSessionLine getPackedLine(int weightPackageSeqId) {
        WeightPackageSessionLine packedLine = null;
        if (weightPackageSeqId > 0) {
            for (WeightPackageSessionLine line : this.getPackedLines()) {
                if ((line.getWeightPackageSeqId()) == weightPackageSeqId)
                    packedLine = line;
            }
        }
        return packedLine;
    }

    public void setPackageWeight(BigDecimal packageWeight, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine))
                packedLine.setPackageWeight(packageWeight);
        }
    }

    public void setPackageLength(BigDecimal packageLength, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine))
                packedLine.setPackageLength(packageLength);
        }
    }

    public void setPackageWidth(BigDecimal packageWidth, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine))
                packedLine.setPackageWidth(packageWidth);
        }
    }

    public void setPackageHeight(BigDecimal packageHeight, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine))
                packedLine.setPackageHeight(packageHeight);
        }
    }

    public void setShipmentBoxTypeId(String shipmentBoxTypeId, int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            if (UtilValidate.isNotEmpty(packedLine))
                packedLine.setShipmentBoxTypeId(shipmentBoxTypeId);
        }
    }

    public void deletePackedLine(int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            this.weightPackageLines.remove(packedLine);
        }
    }

    public void setDimensionAndShipmentBoxType(int weightPackageSeqId) {
        if (weightPackageSeqId > 0) {
            WeightPackageSessionLine packedLine = this.getPackedLine(weightPackageSeqId);
            packedLine.setPackageLength(null);
            packedLine.setPackageWidth(null);
            packedLine.setPackageHeight(null);
            packedLine.setShipmentBoxTypeId(null);
        }
    }

    public void clearPackedLines(String orderId) {
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            this.weightPackageLines.remove(packedLine);
        }
    }

    public boolean complete(String orderId, Locale locale) throws GeneralException {

        this.createPackages(orderId);
        this.changeOrderItemStatus(orderId, shipmentId);
        this.applyItemsToPackages(orderId);
        this.updateShipmentRouteSegments(orderId);
        this.setShipmentToPacked();

        return true;
    }

    protected void createPackages(String orderId) throws GeneralException {
        int shipPackSeqId = 0;
        for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
            String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(++shipPackSeqId, 5);

            Map<String, Object> shipmentPackageMap = FastMap.newInstance();
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

            Map<String, Object> shipmentPackageResult = this.getDispatcher().runSync("createShipmentPackage", shipmentPackageMap);
            if (ServiceUtil.isError(shipmentPackageResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(shipmentPackageResult));
            }
        }
    }

    protected void changeOrderItemStatus(String orderId, String shipmentId) throws GeneralException {
        List<GenericValue> shipmentItems = this.getDelegator().findByAnd("ShipmentItem", UtilMisc.toMap("shipmentId", shipmentId));
        for (GenericValue shipmentItem : shipmentItems) {
            for (WeightPackageSessionLine packedLine : this.getPackedLines(orderId)) {
                packedLine.setShipmentItemSeqId(shipmentItem.getString("shipmentItemSeqId"));
            }
        }
        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
        for (GenericValue orderItem : orderItems) {
            List<GenericValue> orderItemShipGrpInvReserves = orderItem.getRelated("OrderItemShipGrpInvRes");
            if (UtilValidate.isEmpty(orderItemShipGrpInvReserves)) {
                Map<String, Object> orderItemStatusMap = FastMap.newInstance();
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

    protected void applyItemsToPackages(String orderId) throws GeneralException {
        if (UtilValidate.isNotEmpty(orderId) && UtilValidate.isNotEmpty(this.getPackedLines(orderId))) {
            int shipPackSeqId = 0;
            for (WeightPackageSessionLine line: this.getPackedLines(orderId)) {
                line.applyLineToPackage(shipmentId, userLogin, getDispatcher(), ++shipPackSeqId);
            }
        }
    }

    protected void updateShipmentRouteSegments(String orderId) throws GeneralException {
        if (UtilValidate.isNotEmpty(orderId)) {
            BigDecimal shipmentWeight = getShippableWeight(orderId);
            if (UtilValidate.isNotEmpty(shipmentWeight) && shipmentWeight.compareTo(BigDecimal.ZERO) <= 0) return;
            List<GenericValue> shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", this.getShipmentId()));
            if (UtilValidate.isNotEmpty(shipmentRouteSegments)) {
                for (GenericValue shipmentRouteSegment : shipmentRouteSegments) {
                    shipmentRouteSegment.set("billingWeight", shipmentWeight);
                    shipmentRouteSegment.set("billingWeightUomId", getWeightUomId());
                }
                getDelegator().storeAll(shipmentRouteSegments);
            }
        }
    }

    protected void setShipmentToPacked() throws GeneralException {
        Map<String, Object> shipmentMap = FastMap.newInstance();
        shipmentMap.put("shipmentId", shipmentId);
        shipmentMap.put("statusId", "SHIPMENT_PACKED");
        shipmentMap.put("userLogin", userLogin);
        Map<String, Object> shipmentResult = this.getDispatcher().runSync("updateShipment", shipmentMap);
        if (UtilValidate.isNotEmpty(shipmentResult) && ServiceUtil.isError(shipmentResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(shipmentResult));
        }
    }

    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String orderId, String productStoreId, List<GenericValue> shippableItemInfo, BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       orderId, productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }

    public BigDecimal getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId, String orderId, String productStoreId, List<GenericValue> shippableItemInfo, BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        BigDecimal shipmentCostEstimate = ZERO;
        Map<String, Object> shipCostEstimateResult = null;
        try {
            Map<String, Object> shipCostEstimateMap = FastMap.newInstance();
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
        } catch ( GeneralException e ) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(shipCostEstimateResult.get("shippingEstimateAmount"))) {
            shipmentCostEstimate = (BigDecimal) shipCostEstimateResult.get("shippingEstimateAmount");
        }
        return shipmentCostEstimate;
    }
}