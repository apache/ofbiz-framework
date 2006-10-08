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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.product.product.ProductWorker;

public class PackingSession implements java.io.Serializable {

    public static final String module = PackingSession.class.getName();

    protected GenericValue userLogin = null;
    protected String primaryOrderId = null;
    protected String primaryShipGrp = null;
    protected String dispatcherName = null;
    protected String delegatorName = null;
    protected String facilityId = null;
    protected String shipmentId = null;
    protected String instructions = null;
    protected List packEvents = null;
    protected List packLines = null;
    protected int packageSeq = -1;
    protected int status = 1;

    private transient GenericDelegator _delegator = null;
    private transient LocalDispatcher _dispatcher = null;

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId, String orderId, String shipGrp) {
        this._dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();

        this._delegator = _dispatcher.getDelegator();
        this.delegatorName = _delegator.getDelegatorName();

        this.primaryOrderId = orderId;
        this.primaryShipGrp = shipGrp;
        this.userLogin = userLogin;
        this.facilityId = facilityId;
        this.packLines = new ArrayList();
        this.packEvents = new ArrayList();
        this.packageSeq = 1;
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null);
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, double quantity, int packageSeqId) throws GeneralException {
        // reset the session if we just completed
        if (status == 0) {
            throw new GeneralException("Packing session has been completed; be sure to CLEAR before packing a new order! [000]");
        }

        // find the actual product ID
        productId = ProductWorker.findProductId(this.getDelegator(), productId);

        // set the default null values - primary is the assumed first item
        if (orderId == null) {
            orderId = primaryOrderId;
        }
        if (shipGroupSeqId == null) {
            shipGroupSeqId = primaryShipGrp;
        }
        if (orderItemSeqId == null && productId != null) {
            orderItemSeqId = this.findOrderItemSeqId(productId, orderId, shipGroupSeqId, quantity);
        }

        // get the reservations for the item
        Map invLookup = FastMap.newInstance();
        invLookup.put("orderId", orderId);
        invLookup.put("orderItemSeqId", orderItemSeqId);
        invLookup.put("shipGroupSeqId", shipGroupSeqId);
        List reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup);

        // no reservations we cannot add this item
        if (UtilValidate.isEmpty(reservations)) {
            throw new GeneralException("No inventory reservations available; cannot pack this item! [101]");
        }

        // find the inventoryItemId to use
        if (reservations.size() == 1) {
            GenericValue res = EntityUtil.getFirst(reservations);
            int checkCode = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, quantity, packageSeqId);
            this.createPackLineItem(checkCode, res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, packageSeqId);
        } else {
            // more than one reservation found
            Map toCreateMap = FastMap.newInstance();
            Iterator i = reservations.iterator();
            double qtyRemain = quantity;

            while (i.hasNext() && qtyRemain > 0) {
                GenericValue res = (GenericValue) i.next();
                double resQty = res.getDouble("quantity").doubleValue();
                double thisQty = resQty > qtyRemain ? qtyRemain : resQty;

                int thisCheck = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, thisQty, packageSeqId);
                switch (thisCheck) {
                    case 2:
                        Debug.log("Packing check returned '2' - new pack line will be created!", module);
                        toCreateMap.put(res, new Double(resQty));
                        qtyRemain -= resQty;
                        break;
                    case 1:
                        Debug.log("Packing check returned '1' - existing pack line has been updated!", module);
                        qtyRemain -= resQty;
                        break;
                    case 0:
                        Debug.log("Packing check returned '0' - doing nothing.", module);
                        break;
                }
            }

            if (qtyRemain == 0) {
                Iterator x = toCreateMap.keySet().iterator();
                while (x.hasNext()) {
                    GenericValue res = (GenericValue) x.next();
                    Double qty = (Double) toCreateMap.get(res);
                    this.createPackLineItem(2, res, orderId, orderItemSeqId, shipGroupSeqId, productId, qty.doubleValue(), packageSeqId);
                }
            } else {
                throw new GeneralException("Not enough inventory reservation available; cannot pack the item! [103]");
            }
        }

        // run the add events
        this.runEvents(PackingEvent.EVENT_CODE_ADD);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, double quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, null, quantity, packageSeqId);
    }

    public void addOrIncreaseLine(String productId, double quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(null, null, null, productId, quantity, packageSeqId);
    }

    public PackingSessionLine findLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String inventoryItemId, int packageSeq) {
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (orderId.equals(line.getOrderId()) &&
                    orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId()) &&
                    inventoryItemId.equals(line.getInventoryItemId()) && 
                    packageSeq == line.getPackageSeq()) {
                return line;
            }
        }
        return null;
    }

    protected void createPackLineItem(int checkCode, GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, double quantity, int packageSeqId) throws GeneralException {
        // process the result; add new item if necessary
        switch(checkCode) {
            case 0:
                // not enough reserved
                throw new GeneralException("Not enough inventory reservation available; cannot pack the item! [201]");
            case 1:
                // we're all good to go; quantity already updated
                break;
            case 2:
                // need to create a new item
                String invItemId = res.getString("inventoryItemId");
                packLines.add(new PackingSessionLine(orderId, orderItemSeqId, shipGroupSeqId, productId, invItemId, quantity, packageSeqId));
                break;
        }

        // update the package sequence
        if (packageSeqId > packageSeq) {
            this.packageSeq = packageSeqId;
        }
    }

    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, double quantity) throws GeneralException {
        Map lookupMap = FastMap.newInstance();
        lookupMap.put("orderId", orderId);
        lookupMap.put("productId", productId);
        lookupMap.put("shipGroupSeqId", shipGroupSeqId);

        List sort = UtilMisc.toList("-quantity");
        List orderItems = this.getDelegator().findByAnd("OrderItemAndShipGroupAssoc", lookupMap, sort);

        String orderItemSeqId = null;
        if (orderItems != null) {
            Iterator i = orderItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                Double qty = item.getDouble("quantity");
                if (quantity <= qty.doubleValue()) {
                    orderItemSeqId = item.getString("orderItemSeqId");
                    break;
                }
            }
        }

        if (orderItemSeqId != null) {
            return orderItemSeqId;
        } else {
            throw new GeneralException("No valid order item found for product [" + productId + "] with quantity: " + quantity);
        }
    }

    protected int checkLineForAdd(GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, double quantity, int packageSeqId) {
        // check to see if the reservation can hold the requested quantity amount
        String invItemId = res.getString("inventoryItemId");
        double resQty = res.getDouble("quantity").doubleValue();

        PackingSessionLine line = this.findLine(orderId, orderItemSeqId, shipGroupSeqId, invItemId, packageSeqId);
        if (line == null) {
            Debug.log("No current line found testing [" + invItemId + "] R: " + resQty + " / Q: " + quantity, module);
            if (resQty < quantity) {
                return 0;
            } else {
                return 2;
            }
        } else {
            double newQty = line.getQuantity() + quantity;
            Debug.log("Existing line found testing [" + invItemId + "] R: " + resQty + " / Q: " + newQty, module);
            if (resQty < newQty) {
                return 0;
            } else {
                line.setQuantity(newQty);
                return 1;
            }
        }
    }

    public String getShipmentId() {
        return this.shipmentId;
    }

    public List getLines() {
        return this.packLines;
    }

    public int nextPackageSeq() {
        return ++packageSeq;
    }

    public int getCurrentPackageSeq() {
        return packageSeq;
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        double total = 0.0;
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (orderId.equals(line.getOrderId()) && orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId())) {
                total += line.getQuantity();
            }
        }
        return total;
    }

    public void registerEvent(PackingEvent event) {
        this.packEvents.add(event);
        this.runEvents(PackingEvent.EVENT_CODE_EREG);
    }

    public LocalDispatcher getDispatcher() {
        if (_dispatcher == null) {
            try {
                _dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, this.getDelegator());
            } catch (GenericServiceException e) {
                throw new RuntimeException(e);
            }
        }
        return _dispatcher;
    }

    public GenericDelegator getDelegator() {
        if (_delegator == null) {
            _delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        return _delegator;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public int getStatus() {
        return this.status;
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

    public void setPrimaryOrderId(String orderId) {
        this.primaryOrderId = orderId;
    }

    public String getPrimaryShipGroupSeqId() {
        return this.primaryShipGrp;
    }

    public void setPrimaryShipGroupSeqId(String shipGroupSeqId) {
        this.primaryShipGrp = shipGroupSeqId;
    }

    public String getHandlingInstructions() {
        return this.instructions;
    }

    public void setHandlingInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void clearLine(PackingSessionLine line) {
        this.packLines.remove(line);
    }

    public void clear() {
        this.packLines.clear();
        this.instructions = null;
        this.primaryOrderId = null;
        this.primaryShipGrp = null;
        this.packageSeq = 1;
        this.status = 1;
        this.runEvents(PackingEvent.EVENT_CODE_CLEAR);
    }

    public String complete(boolean force) throws GeneralException {
        if (this.getLines().size() == 0) {
            return "EMPTY";
        }

        // check for errors
        this.checkReservations(force);
        // set the status to 0
        this.status = 0;
        // create the shipment
        this.createShipment();
        // create the packages
        this.createPackages();
        // issue the items
        this.issueItemsToShipment();
        // assign items to packages
        this.applyItemsToPackages();
        // set the shipment to packed
        this.setShipmentToPacked();
        // run the complete events
        this.runEvents(PackingEvent.EVENT_CODE_COMPLETE);

        return this.shipmentId;
    }

    protected void checkReservations(boolean ignore) throws GeneralException {
        List errors = FastList.newInstance();        
        Iterator i = this.getLines().iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            Map invLookup = FastMap.newInstance();
            invLookup.put("orderId", line.getOrderId());
            invLookup.put("orderItemSeqId", line.getOrderItemSeqId());
            invLookup.put("shipGroupSeqId", line.getShipGroupSeqId());
            invLookup.put("inventoryItemId", line.getInventoryItemId());
            GenericValue res = this.getDelegator().findByPrimaryKey("OrderItemShipGrpInvRes", invLookup);
            Double qty = res.getDouble("quantity");
            if (qty == null) qty = new Double(0);

            double resQty = qty.doubleValue();
            double lineQty = line.getQuantity();

            if (lineQty != resQty) {
                errors.add("Packed amount does not match reserved amount for item (" + line.getProductId() + ") [" + lineQty + " / " + resQty + "]");
            }
        }

        if (errors.size() > 0) {
            if (!ignore) {
                throw new GeneralException("Attempt to pack order failed. Click COMPLETE again to force.", errors);
            } else {
                Debug.logWarning("Packing warnings: " + errors, module);
            }
        }
    }

    protected void runEvents(int eventCode) {
        if (this.packEvents.size() > 0) {
            Iterator i = this.packEvents.iterator();
            while (i.hasNext()) {
                PackingEvent event = (PackingEvent) i.next();
                event.runEvent(this, eventCode);
            }
        }
    }

    protected void createShipment() throws GeneralException {
        // first create the shipment
        Map newShipment = FastMap.newInstance();
        newShipment.put("originFacilityId", this.facilityId);
        newShipment.put("primaryShipGroupSeqId", primaryShipGrp);
        newShipment.put("primaryOrderId", primaryOrderId);
        newShipment.put("shipmentTypeId", "OUTGOING_SHIPMENT");
        newShipment.put("statusId", "SHIPMENT_INPUT");
        newShipment.put("handlingInstructions", instructions);
        newShipment.put("userLogin", userLogin);
        Debug.log("Creating new shipment with context: " + newShipment, module);
        Map newShipResp = this.getDispatcher().runSync("createShipment", newShipment);

        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        this.shipmentId = (String) newShipResp.get("shipmentId");
    }

    protected void issueItemsToShipment() throws GeneralException {
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            line.issueItemToShipment(shipmentId, userLogin, getDispatcher());
        }
    }

    protected void createPackages() throws GeneralException {
        for (int i = 0; i < packageSeq; i++) {
            String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(i+1, 5);

            Map pkgCtx = FastMap.newInstance();
            pkgCtx.put("shipmentId", shipmentId);
            pkgCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
            //pkgCtx.put("shipmentBoxTypeId", "");
            pkgCtx.put("userLogin", userLogin);
            Map newPkgResp = this.getDispatcher().runSync("createShipmentPackage", pkgCtx);

            if (ServiceUtil.isError(newPkgResp)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(newPkgResp));
            }
        }
    }

    protected void applyItemsToPackages() throws GeneralException {
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            line.applyLineToPackage(shipmentId, userLogin, getDispatcher());
        }
    }

    protected void setShipmentToPacked() throws GeneralException {
        Map packedCtx = UtilMisc.toMap("shipmentId", shipmentId, "statusId", "SHIPMENT_PACKED", "userLogin", userLogin);
        Map packedResp = this.getDispatcher().runSync("updateShipment", packedCtx);
        if (packedResp != null && ServiceUtil.isError(packedResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packedResp));
        }
    }
}
