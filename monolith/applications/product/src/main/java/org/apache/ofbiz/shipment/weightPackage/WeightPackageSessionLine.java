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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
public class WeightPackageSessionLine implements java.io.Serializable {

    private String orderId = null;
    private BigDecimal packageWeight = BigDecimal.ZERO;
    private BigDecimal packageLength = null;
    private BigDecimal packageWidth = null;
    private BigDecimal packageHeight = null;
    private String shipmentBoxTypeId = null;
    private String shipmentItemSeqId = null;
    private int weightPackageSeqId = 0;

    public WeightPackageSessionLine(String orderId, BigDecimal packageWeight, BigDecimal packageLength, BigDecimal packageWidth,
                                    BigDecimal packageHeight, String shipmentBoxTypeId, int weightPackageSeqId) throws GeneralException {
        this.orderId = orderId;
        this.packageWeight = packageWeight;
        this.packageLength = packageLength;
        this.packageWidth = packageWidth;
        this.packageHeight = packageHeight;
        this.shipmentBoxTypeId = shipmentBoxTypeId;
        this.weightPackageSeqId = weightPackageSeqId;
    }

    /**
     * Gets order id.
     * @return the order id
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Sets order id.
     * @param orderId the order id
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets package weight.
     * @return the package weight
     */
    public BigDecimal getPackageWeight() {
        return this.packageWeight;
    }

    /**
     * Sets package weight.
     * @param packageWeight the package weight
     */
    public void setPackageWeight(BigDecimal packageWeight) {
        this.packageWeight = packageWeight;
    }

    /**
     * Gets package length.
     * @return the package length
     */
    public BigDecimal getPackageLength() {
        return this.packageLength;
    }

    /**
     * Sets package length.
     * @param packageLength the package length
     */
    public void setPackageLength(BigDecimal packageLength) {
        this.packageLength = packageLength;
    }

    /**
     * Gets package width.
     * @return the package width
     */
    public BigDecimal getPackageWidth() {
        return this.packageWidth;
    }

    /**
     * Sets package width.
     * @param packageWidth the package width
     */
    public void setPackageWidth(BigDecimal packageWidth) {
        this.packageWidth = packageWidth;
    }

    /**
     * Gets package height.
     * @return the package height
     */
    public BigDecimal getPackageHeight() {
        return this.packageHeight;
    }

    /**
     * Sets package height.
     * @param packageHeight the package height
     */
    public void setPackageHeight(BigDecimal packageHeight) {
        this.packageHeight = packageHeight;
    }

    /**
     * Gets shipment box type id.
     * @return the shipment box type id
     */
    public String getShipmentBoxTypeId() {
        return this.shipmentBoxTypeId;
    }

    /**
     * Sets shipment box type id.
     * @param shipmentBoxTypeId the shipment box type id
     */
    public void setShipmentBoxTypeId(String shipmentBoxTypeId) {
        this.shipmentBoxTypeId = shipmentBoxTypeId;
    }

    /**
     * Gets weight package seq id.
     * @return the weight package seq id
     */
    public int getWeightPackageSeqId() {
        return this.weightPackageSeqId;
    }

    /**
     * Sets weight package seq id.
     * @param weightPackageSeqId the weight package seq id
     */
    public void setWeightPackageSeqId(int weightPackageSeqId) {
        this.weightPackageSeqId = weightPackageSeqId;
    }

    /**
     * Gets shipment item seq id.
     * @return the shipment item seq id
     */
    public String getShipmentItemSeqId() {
        return this.shipmentItemSeqId;
    }

    /**
     * Sets shipment item seq id.
     * @param shipmentItemSeqId the shipment item seq id
     */
    public void setShipmentItemSeqId(String shipmentItemSeqId) {
        this.shipmentItemSeqId = shipmentItemSeqId;
    }

    /**
     * Apply line to package.
     * @param shipmentId    the shipment id
     * @param userLogin     the user login
     * @param dispatcher    the dispatcher
     * @param shipPackSeqId the ship pack seq id
     * @throws GeneralException the general exception
     */
    protected void applyLineToPackage(String shipmentId, GenericValue userLogin, LocalDispatcher dispatcher, int shipPackSeqId)
            throws GeneralException {
        String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(shipPackSeqId, 5);

        Map<String, Object> packageMap = new HashMap<>();
        packageMap.put("shipmentId", shipmentId);
        packageMap.put("shipmentItemSeqId", this.getShipmentItemSeqId());
        // quanity given, by defult one because it is a required field
        packageMap.put("quantity", BigDecimal.ONE);
        packageMap.put("shipmentPackageSeqId", shipmentPackageSeqId);
        packageMap.put("userLogin", userLogin);
        Map<String, Object> packageResp = dispatcher.runSync("addShipmentContentToPackage", packageMap);

        if (ServiceUtil.isError(packageResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packageResp));
        }
    }
}
