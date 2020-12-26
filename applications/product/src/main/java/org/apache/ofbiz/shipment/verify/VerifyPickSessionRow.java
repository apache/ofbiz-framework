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

    private String orderId = null;
    private String orderItemSeqId = null;
    private String shipGroupSeqId = null;
    private String productId = null;
    private String originGeoId = null;
    private String inventoryItemId = null;
    private BigDecimal readyToVerifyQty = BigDecimal.ZERO;
    private GenericValue orderItem = null;
    private String shipmentItemSeqId = null;
    private String invoiceItemSeqId = null;

    public VerifyPickSessionRow() {
    }

    public VerifyPickSessionRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String originGeoId,
                                String inventoryItemId, BigDecimal quantity) {
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

    /**
     * Gets order id.
     * @return the order id
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Gets order item seq id.
     * @return the order item seq id
     */
    public String getOrderItemSeqId() {
        return this.orderItemSeqId;
    }

    /**
     * Gets ship group seq id.
     * @return the ship group seq id
     */
    public String getShipGroupSeqId() {
        return this.shipGroupSeqId;
    }

    /**
     * Gets product id.
     * @return the product id
     */
    public String getProductId() {
        return this.productId;
    }

    /**
     * Gets origin geo id.
     * @return the origin geo id
     */
    public String getOriginGeoId() {
        return this.originGeoId;
    }

    /**
     * Gets inventory item id.
     * @return the inventory item id
     */
    public String getInventoryItemId() {
        return this.inventoryItemId;
    }

    /**
     * Gets ready to verify qty.
     * @return the ready to verify qty
     */
    public BigDecimal getReadyToVerifyQty() {
        return this.readyToVerifyQty;
    }

    /**
     * Sets ready to verify qty.
     * @param readyToVerifyQty the ready to verify qty
     */
    public void setReadyToVerifyQty(BigDecimal readyToVerifyQty) {
        this.readyToVerifyQty = readyToVerifyQty;
    }

    /**
     * Sets shipment item seq id.
     * @param shipmentItemSeqId the shipment item seq id
     */
    public void setShipmentItemSeqId(String shipmentItemSeqId) {
        this.shipmentItemSeqId = shipmentItemSeqId;
    }

    /**
     * Gets shipment item seq id.
     * @return the shipment item seq id
     */
    public String getShipmentItemSeqId() {
        return this.shipmentItemSeqId;
    }

    /**
     * Sets invoice item seq id.
     * @param invoiceItemSeqId the invoice item seq id
     */
    public void setInvoiceItemSeqId(String invoiceItemSeqId) {
        this.invoiceItemSeqId = invoiceItemSeqId;
    }

    /**
     * Gets invoice item seq id.
     * @return the invoice item seq id
     */
    public String getInvoiceItemSeqId() {
        return this.invoiceItemSeqId;
    }

    /**
     * Gets order item.
     * @return the order item
     */
    public GenericValue getOrderItem() {
        return this.orderItem;
    }

    /**
     * Is same item boolean.
     * @param line the line
     * @return the boolean
     */
    public boolean isSameItem(VerifyPickSessionRow line) {
        return this.getInventoryItemId().equals(line.getInventoryItemId()) && this.getOrderItemSeqId().equals(line.getOrderItemSeqId())
                && this.getOrderId().equals(line.getOrderId()) && this.getShipGroupSeqId().equals(line.getShipGroupSeqId());
    }

    /**
     * Issue item to shipment.
     * @param shipmentId the shipment id
     * @param picklistBinId the picklist bin id
     * @param userLogin the user login
     * @param quantity the quantity
     * @param dispatcher the dispatcher
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    protected void issueItemToShipment(String shipmentId, String picklistBinId, GenericValue userLogin, BigDecimal quantity,
                                       LocalDispatcher dispatcher, Locale locale) throws GeneralException {

        if (quantity == null) {
            quantity = this.getReadyToVerifyQty();
        }

        Map<String, Object> issueOrderItemMap = new HashMap<>();
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
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels",
                    "ProductErrorIssueItemDidNotReturnAValidShipmentItemSeqId", locale));
        } else {
            this.setShipmentItemSeqId(shipmentItemSeqId);
        }

        if (picklistBinId != null) {
            // find the pick list item
            Delegator delegator = dispatcher.getDelegator();
            Map<String, Object> picklistItemMap = new HashMap<>();
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
