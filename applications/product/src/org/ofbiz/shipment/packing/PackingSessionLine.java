/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.shipment.packing;

import java.util.Map;

import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import javolution.util.FastMap;

public class PackingSessionLine implements java.io.Serializable {

    public final String module = PackingSessionLine.class.getName();

    protected String orderId = null;
    protected String orderItemSeqId = null;
    protected String shipGroupSeqId = null;
    protected String productId = null;
    protected String inventoryItemId = null;
    protected String shipmentItemSeqId = null;
    protected double quantity = 0;
    protected int packageSeq = 0;

    public PackingSessionLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, double quantity, int packageSeq) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.inventoryItemId = inventoryItemId;
        this.productId = productId;
        this.quantity = quantity;
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

    public int getPackageSeq() {
        return this.packageSeq;
    }

    protected void issueItemToShipment(String shipmentId, GenericValue userLogin, LocalDispatcher dispatcher) throws GeneralException {
        Map issueMap = FastMap.newInstance();
        issueMap.put("shipmentId", shipmentId);
        issueMap.put("orderId", this.getOrderId());
        issueMap.put("orderItemSeqId", this.getOrderItemSeqId());
        issueMap.put("shipGroupSeqId", this.getShipGroupSeqId());
        issueMap.put("inventoryItemId", this.getInventoryItemId());
        issueMap.put("quantity", new Double(this.getQuantity()));
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
