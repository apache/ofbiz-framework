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

package org.apache.ofbiz.shipment.verify;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
public class VerifyPickSessionRow implements Serializable {

    protected String orderId = null;
    protected String orderItemSeqId = null;
    protected String shipGroupSeqId = null;
    protected String productId = null;
    protected String originGeoId = null;
    protected String inventoryItemId = null;
    protected BigDecimal readyToVerifyQty = BigDecimal.ZERO;
    protected GenericValue orderItem = null;
    protected String shipmentItemSeqId = null;
    protected String invoiceItemSeqId = null;

    public VerifyPickSessionRow() {
    }

    public VerifyPickSessionRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String originGeoId, String inventoryItemId, BigDecimal quantity) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.productId = productId;
        this.originGeoId = originGeoId;
        this.readyToVerifyQty = quantity;
        this.inventoryItemId = inventoryItemId;
        this.shipmentItemSeqId = null;
        this.invoiceItemSeqId = null;
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

    public String getProductId() {
        return this.productId;
    }

    public String getOriginGeoId() {
        return this.originGeoId;
    }

    public String getInventoryItemId() {
        return this.inventoryItemId;
    }

    public BigDecimal getReadyToVerifyQty() {
        return this.readyToVerifyQty;
    }

    public void setReadyToVerifyQty(BigDecimal readyToVerifyQty) {
        this.readyToVerifyQty = readyToVerifyQty;
    }

    public void setShipmentItemSeqId(String shipmentItemSeqId) {
        this.shipmentItemSeqId = shipmentItemSeqId;
    }

    public String getShipmentItemSeqId() {
        return this.shipmentItemSeqId;
    }

    public void setInvoiceItemSeqId(String invoiceItemSeqId) {
        this.invoiceItemSeqId = invoiceItemSeqId;
    }

    public String getInvoiceItemSeqId() {
        return this.invoiceItemSeqId;
    }

    public GenericValue getOrderItem() {
        return this.orderItem;
    }

    public boolean isSameItem(VerifyPickSessionRow line) {
        if (this.getInventoryItemId().equals(line.getInventoryItemId()) && this.getOrderItemSeqId().equals(line.getOrderItemSeqId())
                && this.getOrderId().equals(line.getOrderId()) && this.getShipGroupSeqId().equals(line.getShipGroupSeqId())) {
            return true;
        }
        return false;
    }

    protected void issueItemToShipment(String shipmentId, String picklistBinId, GenericValue userLogin, BigDecimal quantity, LocalDispatcher dispatcher, Locale locale) throws GeneralException {

        if (quantity == null) {
            quantity = this.getReadyToVerifyQty();
        }

        Map<String, Object> issueOrderItemMap = new HashMap<String, Object>();
        issueOrderItemMap.put("shipmentId", shipmentId);
        issueOrderItemMap.put("orderId", this.getOrderId());
        issueOrderItemMap.put("orderItemSeqId", this.getOrderItemSeqId());
        issueOrderItemMap.put("shipGroupSeqId", this.getShipGroupSeqId());
        issueOrderItemMap.put("inventoryItemId", this.getInventoryItemId());
        issueOrderItemMap.put("quantity", quantity);
        issueOrderItemMap.put("userLogin", userLogin);

        Map<String, Object> issueOrderItemResp = dispatcher.runSync("issueOrderItemShipGrpInvResToShipment", issueOrderItemMap);
        if (ServiceUtil.isError(issueOrderItemResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(issueOrderItemResp));
        }

        String shipmentItemSeqId = (String) issueOrderItemResp.get("shipmentItemSeqId");
        if (shipmentItemSeqId == null) {
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorIssueItemDidNotReturnAValidShipmentItemSeqId", locale));
        } else {
            this.setShipmentItemSeqId(shipmentItemSeqId);
        }

        if (picklistBinId != null) {
            // find the pick list item
            Delegator delegator = dispatcher.getDelegator();
            Map<String, Object> picklistItemMap = new HashMap<String, Object>();
            picklistItemMap.put("picklistBinId", picklistBinId);
            picklistItemMap.put("orderId", this.getOrderId());
            picklistItemMap.put("orderItemSeqId", this.getOrderItemSeqId());
            picklistItemMap.put("shipGroupSeqId", this.getShipGroupSeqId());
            picklistItemMap.put("inventoryItemId", this.getInventoryItemId());

            GenericValue picklistItem = EntityQuery.use(delegator).from("PicklistItem").where(picklistItemMap).cache(true).queryOne();
            if (UtilValidate.isNotEmpty(picklistItem)) {
                BigDecimal itemQty = picklistItem.getBigDecimal("quantity");
                if (itemQty.compareTo(quantity) == 0) {
                    // set to complete
                    picklistItemMap.put("itemStatusId", "PICKITEM_COMPLETED");
                } else {
                    picklistItemMap.put("itemStatusId", "PICKITEM_CANCELLED");
                }
                picklistItemMap.put("userLogin", userLogin);
                Map<String, Object> picklistItemResp = dispatcher.runSync("updatePicklistItem", picklistItemMap);
                if (ServiceUtil.isError(picklistItemResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(picklistItemResp));
                }
            }
        }
    }

}
