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
package org.apache.ofbiz.shipment.packing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
public class PackingSessionLine implements java.io.Serializable {

    public final String module = PackingSessionLine.class.getName();

    protected String orderId = null;
    protected String orderItemSeqId = null;
    protected String shipGroupSeqId = null;
    protected String productId = null;
    protected String inventoryItemId = null;
    protected String shipmentItemSeqId = null;
    protected BigDecimal quantity = BigDecimal.ZERO;
    protected BigDecimal weight = BigDecimal.ZERO;
    protected BigDecimal height = null;
    protected BigDecimal width = null;
    protected BigDecimal length = null;
    protected String shipmentBoxTypeId = null;
    protected String weightPackageSeqId = null;
    protected int packageSeq = 0;

    public PackingSessionLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, BigDecimal quantity, BigDecimal weight, int packageSeq) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.inventoryItemId = inventoryItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.weight = weight;
        this.height = null;
        this.width = null;
        this.length = null;
        this.shipmentBoxTypeId = null;
        this.weightPackageSeqId = null;
        this.packageSeq = packageSeq;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderItemSeqId() {
        return this.orderItemSeqId;
    }

    public String getShipGroupSeqId() {
        return this.shipGroupSeqId;
    }

    public String getInventoryItemId() {
        return this.inventoryItemId;
    }

    public String getProductId() {
        return this.productId;
    }

    public String getShipmentItemSeqId() {
        return this.shipmentItemSeqId;
    }

    public void setShipmentItemSeqId(String shipmentItemSeqId) {
        this.shipmentItemSeqId = shipmentItemSeqId;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(BigDecimal quantity) {
        this.quantity = this.quantity.add(quantity);
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public void addWeight(BigDecimal weight) {
        this.weight = this.weight.add(weight);
    }

    public int getPackageSeq() {
        return this.packageSeq;
    }

    public BigDecimal getLength() {
        return this.length;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public BigDecimal getWidth() {
        return this.width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return this.height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public String getShipmentBoxTypeId() {
        return this.shipmentBoxTypeId;
    }

    public void setShipmentBoxTypeId(String shipmentBoxTypeId) {
        this.shipmentBoxTypeId = shipmentBoxTypeId;
    }

    public String getWeightPackageSeqId() {
        return this.weightPackageSeqId;
    }

    public void setWeightPackageSeqId(String weightPackageSeqId) {
        this.weightPackageSeqId = weightPackageSeqId;
    }

    public boolean isSameItem(PackingSessionLine line) {
        if (this.getInventoryItemId().equals(line.getInventoryItemId())) {
            if (this.getOrderItemSeqId().equals(line.getOrderItemSeqId())) {
                if (this.getOrderId().equals(line.getOrderId())) {
                    if (this.getShipGroupSeqId().equals(line.getShipGroupSeqId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void issueItemToShipment(String shipmentId, String picklistBinId, GenericValue userLogin, BigDecimal quantity, LocalDispatcher dispatcher) throws GeneralException {
        if (quantity == null) {
            quantity = this.getQuantity();
        }

        Map<String, Object> issueMap = new HashMap<String, Object>();
        issueMap.put("shipmentId", shipmentId);
        issueMap.put("orderId", this.getOrderId());
        issueMap.put("orderItemSeqId", this.getOrderItemSeqId());
        issueMap.put("shipGroupSeqId", this.getShipGroupSeqId());
        issueMap.put("inventoryItemId", this.getInventoryItemId());
        issueMap.put("quantity", quantity);
        issueMap.put("userLogin", userLogin);

        Map<String, Object> issueResp = dispatcher.runSync("issueOrderItemShipGrpInvResToShipment", issueMap);
        if (ServiceUtil.isError(issueResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(issueResp));
        }

        String shipmentItemSeqId = (String) issueResp.get("shipmentItemSeqId");
        if (shipmentItemSeqId == null) {
            throw new GeneralException("Issue item did not return a valid shipmentItemSeqId!");
        } else {
            this.setShipmentItemSeqId(shipmentItemSeqId);
        }

        if (picklistBinId != null) {
            // find the pick list item
            Debug.logInfo("Looking up picklist item for bin ID #" + picklistBinId, module);
            Delegator delegator = dispatcher.getDelegator();
            Map<String, Object> itemLookup = new HashMap<String, Object>();
            itemLookup.put("picklistBinId", picklistBinId);
            itemLookup.put("orderId", this.getOrderId());
            itemLookup.put("orderItemSeqId", this.getOrderItemSeqId());
            itemLookup.put("shipGroupSeqId", this.getShipGroupSeqId());
            itemLookup.put("inventoryItemId", this.getInventoryItemId());
            GenericValue plItem = EntityQuery.use(delegator)
                                      .from("PicklistItem")
                                      .where(itemLookup)
                                      .queryOne();
            if (plItem != null) {
                Debug.logInfo("Found picklist bin: " + plItem, module);
                BigDecimal itemQty = plItem.getBigDecimal("quantity");
                if (itemQty.compareTo(quantity) == 0) {
                    // set to complete
                    itemLookup.put("itemStatusId", "PICKITEM_COMPLETED");
                } else {
                    itemLookup.put("itemStatusId", "PICKITEM_CANCELLED");
                }
                itemLookup.put("userLogin", userLogin);

                Map<String, Object> itemUpdateResp = dispatcher.runSync("updatePicklistItem", itemLookup);
                if (ServiceUtil.isError(itemUpdateResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(issueResp));
                }
            } else {
                Debug.logInfo("No item was found for lookup: " + itemLookup, module);
            }
        } else {
            Debug.logWarning("*** NO Picklist Bin ID set; cannot update picklist status!", module);
        }
    }

    protected void applyLineToPackage(String shipmentId, GenericValue userLogin, LocalDispatcher dispatcher) throws GeneralException {
        // assign item to package
        String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(this.getPackageSeq(), 5);

        Map<String, Object> packageMap = new HashMap<String, Object>();
        packageMap.put("shipmentId", shipmentId);
        packageMap.put("shipmentItemSeqId", this.getShipmentItemSeqId());
        packageMap.put("quantity", this.getQuantity());
        packageMap.put("shipmentPackageSeqId", shipmentPackageSeqId);
        packageMap.put("userLogin", userLogin);
        Map<String, Object> packageResp = dispatcher.runSync("addShipmentContentToPackage", packageMap);

        if (ServiceUtil.isError(packageResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packageResp));
        }
    }
}
