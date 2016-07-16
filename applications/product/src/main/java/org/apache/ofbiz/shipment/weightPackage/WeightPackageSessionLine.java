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

    protected String orderId = null;
    protected BigDecimal packageWeight = BigDecimal.ZERO;
    protected BigDecimal packageLength = null;
    protected BigDecimal packageWidth = null;
    protected BigDecimal packageHeight = null;
    protected String shipmentBoxTypeId = null;
    protected String shipmentItemSeqId = null;
    protected int weightPackageSeqId = 0;

    public WeightPackageSessionLine(String orderId, BigDecimal packageWeight, BigDecimal packageLength, BigDecimal packageWidth, BigDecimal packageHeight, String shipmentBoxTypeId, int weightPackageSeqId) throws GeneralException {
        this.orderId = orderId;
        this.packageWeight = packageWeight;
        this.packageLength = packageLength;
        this.packageWidth = packageWidth;
        this.packageHeight = packageHeight;
        this.shipmentBoxTypeId = shipmentBoxTypeId;
        this.weightPackageSeqId = weightPackageSeqId;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getPackageWeight() {
        return this.packageWeight;
    }

    public void setPackageWeight(BigDecimal packageWeight) {
        this.packageWeight = packageWeight;
    }

    public BigDecimal getPackageLength() {
        return this.packageLength;
    }

    public void setPackageLength(BigDecimal packageLength) {
        this.packageLength = packageLength;
    }

    public BigDecimal getPackageWidth() {
       return this.packageWidth;
    }

    public void setPackageWidth(BigDecimal packageWidth) {
        this.packageWidth = packageWidth;
    }

    public BigDecimal getPackageHeight() {
        return this.packageHeight;
    }

    public void setPackageHeight(BigDecimal packageHeight) {
        this.packageHeight = packageHeight;
    }

    public String getShipmentBoxTypeId() {
        return this.shipmentBoxTypeId;
    }

    public void setShipmentBoxTypeId(String shipmentBoxTypeId) {
        this.shipmentBoxTypeId = shipmentBoxTypeId;
    }

    public int getWeightPackageSeqId() {
        return this.weightPackageSeqId;
    }

    public void setWeightPackageSeqId(int weightPackageSeqId) {
        this.weightPackageSeqId = weightPackageSeqId;
    }

    public String getShipmentItemSeqId() {
        return this.shipmentItemSeqId;
    }

    public void setShipmentItemSeqId(String shipmentItemSeqId) {
        this.shipmentItemSeqId = shipmentItemSeqId;
    }

    protected void applyLineToPackage(String shipmentId, GenericValue userLogin, LocalDispatcher dispatcher, int shipPackSeqId) throws GeneralException {
        String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(shipPackSeqId, 5);

        Map<String, Object> packageMap = new HashMap<String, Object>();
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
