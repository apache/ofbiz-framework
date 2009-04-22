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
import java.util.List;
import java.util.Map;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class VerifyPickSession implements Serializable {

    protected GenericValue userLogin = null;
    protected String dispatcherName = null;
    protected String delegatorName = null;
    protected int rowItems = 1;
    protected List<VerifyPickSessionRow> pickRows = null;

    private transient GenericDelegator _delegator = null;
    private transient LocalDispatcher _dispatcher = null;

    public VerifyPickSession() {
    }

    public VerifyPickSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this._dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();
        this._delegator = _dispatcher.getDelegator();
        this.delegatorName = _delegator.getDelegatorName();
        this.userLogin = userLogin;
        this.pickRows = FastList.newInstance();
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

    public void createRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, String facilityId, GenericValue orderItem) {
        int rowItem = this.getRowNo();
        int counter = 1;
        BigDecimal readyToVerify = null;
        if (rowItem > 1) {
            List<VerifyPickSessionRow> rows = this.getPickRows();
            for (VerifyPickSessionRow row : rows) {
                counter++;
                if ((orderId.equals(row.getOrderId())) && (orderItemSeqId.equals(row.getOrderSeqId()))) {
                    readyToVerify = quantity.add(row.getReadyToVerifyQty());
                    row.setReadyToVerifyQty(readyToVerify);
                    break;
                } else if (counter == rowItem) {
                    this.createRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, facilityId, orderItem, rowItem);
                }
            }
        } else {
            this.createRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, facilityId, orderItem, rowItem);
        }
    }

    public void createRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, String facilityId, GenericValue orderItem, int rowItem) {
        pickRows.add(new VerifyPickSessionRow(orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, facilityId, orderItem, rowItem));
        this.setRowNo();
    }

    public int getRowNo() {
        return this.rowItems;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public void setRowNo() {
        this.rowItems = (this.getRowNo()) + 1;
    }

    public List<VerifyPickSessionRow> getPickRows() {
        return this.pickRows;
    }

    public List<VerifyPickSessionRow> getPickRows(String orderId) {
        List<VerifyPickSessionRow> pickVerifyRows = FastList.newInstance();
        for (VerifyPickSessionRow line: this.getPickRows()) {
            if (orderId.equals(line.getOrderId())) {
                pickVerifyRows.add(line);
            }
        }
        return pickVerifyRows;
    }

    public BigDecimal getReadyToVerifyQuantity(String orderId, String orderSeqId) throws GeneralException {
        BigDecimal readyToVerifyQty = BigDecimal.ZERO;
        for (VerifyPickSessionRow line: this.getPickRows()) {
            if ((orderId.equals(line.getOrderId())) && (orderSeqId.equals(line.getOrderSeqId()))) {
                readyToVerifyQty = readyToVerifyQty.add(line.getReadyToVerifyQty());
            }
        }
        return readyToVerifyQty;
    }

    public void clearAllRows() {
        this.pickRows.clear();
        this.rowItems = 1;
    }

    public String complete(String orderId) throws GeneralException {
        String shipmentItemSeqId = null;
        String invoiceId = null;
        String invoiceItemSeqId = null;
        this.checkVerifiedQty(orderId);
        String shipmentId = this.createShipment((this.getPickRows(orderId)).get(0));
        for (VerifyPickSessionRow line: this.getPickRows(orderId)) {
            shipmentItemSeqId = this.createShipmentItem(line,shipmentId);
            line.setShipmentItemSeqId(shipmentItemSeqId);
        }
        invoiceId = this.createInvoice(orderId);
        for (VerifyPickSessionRow line: this.getPickRows(orderId)) {
            invoiceItemSeqId = this.createInvoiceItem(line, invoiceId, shipmentId);
            line.setInvoiceItemSeqId(invoiceItemSeqId);
        }
        return shipmentId;
    }

    protected void checkVerifiedQty(String orderId) throws GeneralException {
        int counter = 0;
        List<GenericValue> orderItems = null;
        for (VerifyPickSessionRow line : this.getPickRows(orderId)) {
            orderItems = this.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
            for (GenericValue orderItem : orderItems) {
                if ((orderItem.get("orderItemSeqId")).equals(line.getOrderSeqId())) {
                    if (((line.getReadyToVerifyQty()).compareTo(orderItem.getBigDecimal("quantity"))) == 0 ) {
                        counter++;
                    }
                }
            }
        }
        if (counter != (orderItems.size())) {
            throw new GeneralException("All order items are not verified");
        }
    }

    protected String createShipment(VerifyPickSessionRow line) throws GeneralException {
        Map<String, Object> newShipment = FastMap.newInstance();
        newShipment.put("originFacilityId", line.getFacilityId());
        newShipment.put("primaryShipGroupSeqId", line.getShipGroupSeqId());
        newShipment.put("primaryOrderId", line.getOrderId());
        newShipment.put("shipmentTypeId", "OUTGOING_SHIPMENT");
        newShipment.put("statusId", "SHIPMENT_PICKED");
        newShipment.put("userLogin", this.getUserLogin());
        Map<String, Object> newShipResp = this.getDispatcher().runSync("createShipment", newShipment);
        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        String shipmentId = (String) newShipResp.get("shipmentId");
        return shipmentId;
    }

    protected String createShipmentItem(VerifyPickSessionRow line,String shipmentId) throws GeneralException {
        Map<String, Object> newShipmentItem = FastMap.newInstance();
        newShipmentItem.put("shipmentId", shipmentId);
        newShipmentItem.put("productId", line.getProductId());
        newShipmentItem.put("userLogin", this.getUserLogin());
        newShipmentItem.put("quantity", line.getReadyToVerifyQty());
        Map<String, Object> newShipItem = this.getDispatcher().runSync("createShipmentItem", newShipmentItem);
        if (ServiceUtil.isError(newShipItem)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipItem));
        }
        String shipmentItemSeqId = (String) newShipItem.get("shipmentItemSeqId");
        Map<String, Object> newOrderShipment = FastMap.newInstance();
        newOrderShipment.put("shipmentId", shipmentId);
        newOrderShipment.put("shipmentItemSeqId", shipmentItemSeqId);
        newOrderShipment.put("orderId", line.getOrderId());
        newOrderShipment.put("orderItemSeqId", line.getOrderSeqId());
        newOrderShipment.put("quantity", line.getReadyToVerifyQty());
        newOrderShipment.put("userLogin", this.getUserLogin());
        Map<String, Object> newOrderShip = this.getDispatcher().runSync("createOrderShipment", newOrderShipment);
        if (ServiceUtil.isError(newOrderShip)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newOrderShip));
        }
        return shipmentItemSeqId;
    }

    protected String createInvoice(String orderId) throws GeneralException {
        GenericDelegator delegator = this.getDelegator();
        Map createInvoiceContext = FastMap.newInstance();
        GenericValue orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);
        GenericValue billingAccount = orderHeader.getRelatedOne("BillingAccount");
        String billingAccountId = billingAccount != null ? billingAccount.getString("billingAccountId") : null;
        createInvoiceContext.put("partyId", (EntityUtil.getFirst(delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "BILL_TO_CUSTOMER")))).getString("partyId"));
        createInvoiceContext.put("partyIdFrom", (EntityUtil.getFirst(delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "BILL_FROM_VENDOR")))).getString("partyId"));
        createInvoiceContext.put("billingAccountId", billingAccountId);
        createInvoiceContext.put("invoiceTypeId", "SALES_INVOICE");
        createInvoiceContext.put("statusId", "INVOICE_IN_PROCESS");
        createInvoiceContext.put("currencyUomId", orderHeader.getString("currencyUom"));
        createInvoiceContext.put("userLogin", this.getUserLogin());
        Map createInvoiceResult = this.getDispatcher().runSync("createInvoice", createInvoiceContext);
        if (ServiceUtil.isError(createInvoiceResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(createInvoiceResult));
        }
        String invoiceId = (String) createInvoiceResult.get("invoiceId");
        return invoiceId;
    }

    protected String createInvoiceItem(VerifyPickSessionRow line, String invoiceId, String shipmentId) throws GeneralException {
        Map createInvoiceItemContext = FastMap.newInstance();
        createInvoiceItemContext.put("invoiceId", invoiceId);
        createInvoiceItemContext.put("orderId", line.getOrderId());
        createInvoiceItemContext.put("invoiceItemTypeId", "INV_FPROD_ITEM");
        createInvoiceItemContext.put("productId", line.getProductId());
        createInvoiceItemContext.put("quantity", line.getReadyToVerifyQty());
        createInvoiceItemContext.put("userLogin", this.getUserLogin());
        Map createInvoiceItemResult = this.getDispatcher().runSync("createInvoiceItem", createInvoiceItemContext);
        if (ServiceUtil.isError(createInvoiceItemResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(createInvoiceItemResult));
        }
        String invoiceItemSeqId = (String) createInvoiceItemResult.get("invoiceItemSeqId");
        GenericValue shipmentItemBilling =  this.getDelegator().makeValue("ShipmentItemBilling", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
        shipmentItemBilling.put("shipmentId", shipmentId);
        shipmentItemBilling.put("shipmentItemSeqId", line.getShipmentItemSeqId());
        shipmentItemBilling.create();
        return invoiceItemSeqId;
    }
}