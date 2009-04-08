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

package org.ofbiz.shipment.verify;

import java.io.Serializable;
import java.math.BigDecimal;

import org.ofbiz.entity.GenericValue;

public class VerifyPickSessionRow implements Serializable {

    protected String orderId = null;
    protected String orderItemSeqId = null;
    protected String shipGroupSeqId = null;
    protected String productId = null;
    protected String facilityId = null;
    protected BigDecimal readyToVerifyQty = BigDecimal.ZERO;
    protected GenericValue orderItem = null;
    protected int rowItem = 0;
    protected String shipmentItemSeqId = null;
    protected String invoiceItemSeqId = null;

    public VerifyPickSessionRow() {
    }

    public VerifyPickSessionRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, String facilityId, GenericValue orderItem, int rowItem) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.productId = productId;
        this.readyToVerifyQty = quantity;
        this.facilityId = facilityId;
        this.orderItem = orderItem;
        this.rowItem = rowItem;
        this.shipmentItemSeqId = null;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderSeqId() {
        return this.orderItemSeqId;
    }

    public String getShipGroupSeqId() {
        return this.shipGroupSeqId;
    }

    public String getProductId() {
        return this.productId;
    }

    public String getFacilityId() {
        return this.facilityId;
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
}