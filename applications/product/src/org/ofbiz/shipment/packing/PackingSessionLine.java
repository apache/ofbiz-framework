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

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class PackingSessionLine implements java.io.Serializable {

    public final String module = PackingSessionLine.class.getName();

    protected String orderId = null;
    protected String orderItemSeqId = null;
    protected String shipGroupSeqId = null;
    protected String productId = null;
    protected String inventoryItemId = null;
    protected String shipmentItemSeqId = null;
    protected double quantity = 0;
    protected double weight = 0;
    protected int packageSeq = 0;

    public PackingSessionLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, double quantity, double weight, int packageSeq) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.inventoryItemId = inventoryItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.weight = weight;
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

    public double getQuantity() {
        return this.quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(double quantity) {
        this.quantity += quantity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void addWeight(double weight) {
        this.weight += weight;
    }

    public int getPackageSeq() {
        return this.packageSeq;
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

    protected void issueItemToShipment(String shipmentId, String picklistBinId, GenericValue userLogin, Double quantity, LocalDispatcher dispatcher) throws GeneralException {
        if (quantity == null) {
            quantity = new Double(this.getQuantity());
        }

        Map issueMap = FastMap.newInstance();
        issueMap.put("shipmentId", shipmentId);
        issueMap.put("orderId", this.getOrderId());
        issueMap.put("orderItemSeqId", this.getOrderItemSeqId());
        issueMap.put("shipGroupSeqId", this.getShipGroupSeqId());
        issueMap.put("inventoryItemId", this.getInventoryItemId());
        issueMap.put("quantity", quantity);
        issueMap.put("userLogin", userLogin);

        Map issueResp = dispatcher.runSync("issueOrderItemShipGrpInvResToShipment", issueMap);
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
            Debug.log("Looking up picklist item for bin ID #" + picklistBinId, module);
            GenericDelegator delegator = dispatcher.getDelegator();
            Map itemLookup = FastMap.newInstance();
            itemLookup.put("picklistBinId", picklistBinId);
            itemLookup.put("orderId", this.getOrderId());
            itemLookup.put("orderItemSeqId", this.getOrderItemSeqId());
            itemLookup.put("shipGroupSeqId", this.getShipGroupSeqId());
            itemLookup.put("inventoryItemId", this.getInventoryItemId());
            GenericValue plItem = delegator.findByPrimaryKey("PicklistItem", itemLookup);
            if (plItem != null) {
                Debug.log("Found picklist bin: " + plItem, module);
                Double itemQty = plItem.getDouble("quantity");
                if (itemQty.doubleValue() == quantity.doubleValue()) {
                    // set to complete
                    itemLookup.put("itemStatusId", "PICKITEM_COMPLETED");
                } else {
                    itemLookup.put("itemStatusId", "PICKITEM_CANCELLED");
                }
                itemLookup.put("userLogin", userLogin);

                Map itemUpdateResp = dispatcher.runSync("updatePicklistItem", itemLookup);
                if (ServiceUtil.isError(itemUpdateResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(issueResp));
                }
            } else {
                Debug.log("No item was found for lookup: " + itemLookup, module);
            }
        } else {
            Debug.logWarning("*** NO Picklist Bin ID set; cannot update picklist status!", module);
        }
    }

    protected void applyLineToPackage(String shipmentId, GenericValue userLogin, LocalDispatcher dispatcher) throws GeneralException {
        // assign item to package
        String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(this.getPackageSeq(), 5);

        Map packageMap = FastMap.newInstance();
        packageMap.put("shipmentId", shipmentId);
        packageMap.put("shipmentItemSeqId", this.getShipmentItemSeqId());
        packageMap.put("quantity", new Double(this.getQuantity()));
        packageMap.put("shipmentPackageSeqId", shipmentPackageSeqId);
        packageMap.put("userLogin", userLogin);
        Map packageResp = dispatcher.runSync("addShipmentContentToPackage", packageMap);

        if (ServiceUtil.isError(packageResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packageResp));
        }
    }
}
