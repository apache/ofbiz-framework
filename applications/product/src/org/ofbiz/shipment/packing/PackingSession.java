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

import java.util.*;

import javolution.util.FastMap;
import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.product.product.ProductWorker;

public class PackingSession implements java.io.Serializable {

    public static final String module = PackingSession.class.getName();

    protected GenericValue userLogin = null;
    protected String pickerPartyId = null;
    protected String primaryOrderId = null;
    protected String primaryShipGrp = null;
    protected String dispatcherName = null;
    protected String delegatorName = null;
    protected String picklistBinId = null;
    protected String facilityId = null;
    protected String shipmentId = null;
    protected String instructions = null;
    protected String weightUomId = null;
    protected Double additionalShippingCharge = null;
    protected Map packageWeights = null;
    protected List packEvents = null;
    protected List packLines = null;
    protected int packageSeq = -1;
    protected int status = 1;

    private transient GenericDelegator _delegator = null;
    private transient LocalDispatcher _dispatcher = null;

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId, String binId, String orderId, String shipGrp) {
        this._dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();

        this._delegator = _dispatcher.getDelegator();
        this.delegatorName = _delegator.getDelegatorName();

        this.primaryOrderId = orderId;
        this.primaryShipGrp = shipGrp;
        this.picklistBinId = binId;
        this.userLogin = userLogin;
        this.facilityId = facilityId;
        this.packLines = new ArrayList();
        this.packEvents = new ArrayList();
        this.packageSeq = 1;
        this.packageWeights = new HashMap();
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null, null);
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null, null);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, double quantity, int packageSeqId, double weight, boolean update) throws GeneralException {
        // reset the session if we just completed
        if (status == 0) {
            throw new GeneralException("Packing session has been completed; be sure to CLEAR before packing a new order! [000]");
        }

        // do nothing if we are trying to add a quantity of 0
        if (!update && quantity == 0) {
            return;
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
        List reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup, UtilMisc.toList("quantity DESC"));

        // no reservations we cannot add this item
        if (UtilValidate.isEmpty(reservations)) {
            throw new GeneralException("No inventory reservations available; cannot pack this item! [101]");
        }

        // find the inventoryItemId to use
        if (reservations.size() == 1) {
            GenericValue res = EntityUtil.getFirst(reservations);
            int checkCode = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, quantity, packageSeqId, update);
            this.createPackLineItem(checkCode, res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, weight, packageSeqId);
        } else {
            // more than one reservation found
            Map toCreateMap = FastMap.newInstance();
            Iterator i = reservations.iterator();
            double qtyRemain = quantity;

            while (i.hasNext() && qtyRemain > 0) {
                GenericValue res = (GenericValue) i.next();
                double resQty = res.getDouble("quantity").doubleValue();
                double thisQty = resQty > qtyRemain ? qtyRemain : resQty;

                int thisCheck = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, thisQty, packageSeqId, update);
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
                    this.createPackLineItem(2, res, orderId, orderItemSeqId, shipGroupSeqId, productId, qty.doubleValue(), weight, packageSeqId);
                }
            } else {
                throw new GeneralException("Not enough inventory reservation available; cannot pack the item! [103]");
            }
        }

        // run the add events
        this.runEvents(PackingEvent.EVENT_CODE_ADD);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, double quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, null, quantity, packageSeqId, 0, false);
    }

    public void addOrIncreaseLine(String productId, double quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(null, null, null, productId, quantity, packageSeqId, 0, false);
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

    protected void createPackLineItem(int checkCode, GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, double quantity, double weight, int packageSeqId) throws GeneralException {
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
                packLines.add(new PackingSessionLine(orderId, orderItemSeqId, shipGroupSeqId, productId, invItemId, quantity, weight, packageSeqId));
                break;
        }

        // Add the line weight to the package weight
        if (weight > 0) this.addToPackageWeight(packageSeqId, new Double(weight));
        
        // update the package sequence
        if (packageSeqId > packageSeq) {
            this.packageSeq = packageSeqId;
        }
    }

    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, double quantity) throws GeneralException {
        Map lookupMap = FastMap.newInstance();
        lookupMap.put("orderId", orderId);
        lookupMap.put("productId", productId);
        lookupMap.put("statusId", "ITEM_APPROVED");
        lookupMap.put("shipGroupSeqId", shipGroupSeqId);

        List sort = UtilMisc.toList("-quantity");
        List orderItems = this.getDelegator().findByAnd("OrderItemAndShipGroupAssoc", lookupMap, sort);

        String orderItemSeqId = null;
        if (orderItems != null) {
            Iterator i = orderItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();

                // get the reservations for the item
                Map invLookup = FastMap.newInstance();
                invLookup.put("orderId", orderId);
                invLookup.put("orderItemSeqId", item.getString("orderItemSeqId"));
                invLookup.put("shipGroupSeqId", shipGroupSeqId);
                List reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup);
                Iterator resIter = reservations.iterator();
                while (resIter.hasNext()) {
                    GenericValue res = (GenericValue) resIter.next();
                    Double qty = res.getDouble("quantity");
                    if (quantity <= qty.doubleValue()) {
                        orderItemSeqId = item.getString("orderItemSeqId");
                        break;
                    }
                }
            }
        }

        if (orderItemSeqId != null) {
            return orderItemSeqId;
        } else {
            throw new GeneralException("No valid order item found for product [" + productId + "] with quantity: " + quantity);
        }
    }

    protected int checkLineForAdd(GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, double quantity, int packageSeqId, boolean update) {
        // check to see if the reservation can hold the requested quantity amount
        String invItemId = res.getString("inventoryItemId");
        double resQty = res.getDouble("quantity").doubleValue();

        PackingSessionLine line = this.findLine(orderId, orderItemSeqId, shipGroupSeqId, invItemId, packageSeqId);
        double packedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId);

        Debug.log("Packed quantity [" + packedQty + "] + [" + quantity + "]", module);

        if (line == null) {
            double checkQty = packedQty + quantity;
            Debug.log("No current line found testing [" + invItemId + "] R: " + resQty + " / Q: " + checkQty, module);
            if (resQty < checkQty) {
                return 0;
            } else {
                return 2;
            }
        } else {
            double checkQty = update ? ((packedQty - line.getQuantity()) + quantity) : packedQty + quantity;
            double newQty = update ? quantity : (line.getQuantity() + quantity);            
            Debug.log("Existing line found testing [" + invItemId + "] R: " + resQty + " / Q: " + newQty, module);
            if (resQty < checkQty) {
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
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, null, -1);
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, int packageSeq) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, null, packageSeq);
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String inventoryItemId, int packageSeq) {
        double total = 0.0;
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (orderId.equals(line.getOrderId()) && orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId())) {
                if (inventoryItemId == null || inventoryItemId.equals(line.getInventoryItemId())) {
                    if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                        total += line.getQuantity();
                    }
                }
            }
        }
        return total;
    }

    public double getPackedQuantity(String productId, int packageSeq) {
        if (productId != null) {
            try {
                productId = ProductWorker.findProductId(this.getDelegator(), productId);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        double total = 0.0;
        if (productId != null ) {
            List lines = this.getLines();
            Iterator i = lines.iterator();
            while (i.hasNext()) {
                PackingSessionLine line = (PackingSessionLine) i.next();
                if (productId.equals(line.getProductId())) {
                    if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                        total += line.getQuantity();
                    }
                }
            }
        }
        return total;
    }

    public double getPackedQuantity(int packageSeq) {
        double total = 0.0;
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                total += line.getQuantity();
            }
        }
        return total;
    }

    public double getPackedQuantity(String productId) {
        return getPackedQuantity(productId, -1);
    }

    public double getCurrentReservedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        double reserved = -1;
        List res = null;
        try {
            res = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderId,
                    "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (res != null) {
            reserved = 0.0;
            Iterator i = res.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                Double not = v.getDouble("quantityNotAvailable");
                Double qty = v.getDouble("quantity");
                if (not == null) not = new Double(0);
                if (qty == null) qty = new Double(0);
                reserved += (qty.doubleValue() - not.doubleValue());
            }
        }

        return reserved;
    }

    public double getCurrentShippedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        double shipped = 0.0;
        List issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);
        if (issues != null) {
            Iterator i = issues.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                Double qty = v.getDouble("quantity");
                if (qty == null) qty = new Double(0);
                shipped += qty.doubleValue();
            }
        }

        return shipped;
    }

    public List getCurrentShipmentIds(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        Set shipmentIds = FastSet.newInstance();
        List issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);

        if (issues != null) {
            Iterator i = issues.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                shipmentIds.add(v.getString("shipmentId"));
            }
        }

        List retList = FastList.newInstance();
        retList.addAll(shipmentIds);
        return retList;
    }

    public List getCurrentShipmentIds(String orderId, String shipGroupSeqId) {
        return this.getCurrentShipmentIds(orderId, null, shipGroupSeqId);
    }

    public void registerEvent(PackingEvent event) {
        this.packEvents.add(event);
        this.runEvents(PackingEvent.EVENT_CODE_EREG);
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

    public void setPicklistBinId(String binId) {
        this.picklistBinId = binId;
    }

    public String getPicklistBinId() {
        return this.picklistBinId;
    }

    public String getHandlingInstructions() {
        return this.instructions;
    }

    public void setHandlingInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setPickerPartyId(String partyId) {
        this.pickerPartyId = partyId;
    }

    public String getPickerPartyId() {
        return this.pickerPartyId;
    }

    public int clearLastPackage() {
        if (packageSeq == 1) {
            this.clear();
            return packageSeq;
        }
        
        List currentLines = new ArrayList(this.packLines);
        Iterator i = currentLines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (line.getPackageSeq() == packageSeq) {
                this.clearLine(line);
            }
        }
        return --packageSeq;
    }

    public void clearLine(PackingSessionLine line) {
        this.packLines.remove(line);
    }

    public void clearAllLines() {
        this.packLines.clear();
    }

    public void clear() {
        this.packLines.clear();
        this.instructions = null;
        this.pickerPartyId = null;
        this.picklistBinId = null;
        this.primaryOrderId = null;
        this.primaryShipGrp = null;
        this.additionalShippingCharge = null;
        if (this.packageWeights != null) this.packageWeights.clear();
        this.weightUomId = null;
        this.packageSeq = 1;
        this.status = 1;
        this.runEvents(PackingEvent.EVENT_CODE_CLEAR);
    }

    public String complete(boolean force) throws GeneralException {
        // clear out empty lines
        // this.checkEmptyLines(); // removing, this seems to be causeing issues -  mja

        // check to see if there is anything to process
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
        // update ShipmentRouteSegments with total weight and weightUomId
        this.updateShipmentRouteSegments();
        // set the shipment to packed
        this.setShipmentToPacked();
        // set role on picklist
        this.setPickerOnPicklist();
        // run the complete events
        this.runEvents(PackingEvent.EVENT_CODE_COMPLETE);

        return this.shipmentId;
    }

    protected void checkReservations(boolean ignore) throws GeneralException {
        List errors = FastList.newInstance();        
        Iterator i = this.getLines().iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            double reservedQty =  this.getCurrentReservedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId());
            double packedQty = this.getPackedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId());

            if (packedQty != reservedQty) {
                errors.add("Packed amount does not match reserved amount for item (" + line.getProductId() + ") [" + packedQty + " / " + reservedQty + "]");
            }
        }

        if (errors.size() > 0) {
            if (!ignore) {
                throw new GeneralException("Attempt to pack order failed.", errors);
            } else {
                Debug.logWarning("Packing warnings: " + errors, module);
            }
        }
    }

    protected void checkEmptyLines() throws GeneralException {
        List lines = FastList.newInstance();
        lines.addAll(this.getLines());
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine l = (PackingSessionLine) i.next();
            if (l.getQuantity() == 0) {
                this.packLines.remove(l);
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

    protected List getItemIssuances(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        List issues = null;
        if (orderId == null) {
            throw new IllegalArgumentException("Value for orderId is  null");
        }

        Map lookupMap = FastMap.newInstance();
        lookupMap.put("orderId", orderId);
        if (UtilValidate.isNotEmpty(orderItemSeqId)) {
            lookupMap.put("orderItemSeqId", orderItemSeqId);
        }
        if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
            lookupMap.put("shipGroupSeqId", shipGroupSeqId);
        }
        try {
            issues = this.getDelegator().findByAnd("ItemIssuance",  lookupMap);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return issues;
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
        newShipment.put("picklistBinId", picklistBinId);
        newShipment.put("additionalShippingCharge", additionalShippingCharge);
        newShipment.put("userLogin", userLogin);
        Debug.log("Creating new shipment with context: " + newShipment, module);
        Map newShipResp = this.getDispatcher().runSync("createShipment", newShipment);

        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        this.shipmentId = (String) newShipResp.get("shipmentId");
    }

    protected void issueItemsToShipment() throws GeneralException {
        List processedLines = FastList.newInstance();
        List lines = this.getLines();
        Iterator i = lines.iterator();
        while (i.hasNext()) {
            PackingSessionLine line = (PackingSessionLine) i.next();
            if (this.checkLine(processedLines, line)) {
                double totalPacked = this.getPackedQuantity(line.getOrderId(),  line.getOrderItemSeqId(),
                        line.getShipGroupSeqId(), line.getInventoryItemId(), -1);

                line.issueItemToShipment(shipmentId, picklistBinId, userLogin, new Double(totalPacked), getDispatcher());
                processedLines.add(line);
            }
        }
    }

    protected boolean checkLine(List processedLines, PackingSessionLine line) {
        Iterator i = processedLines.iterator();
        while (i.hasNext()) {
            PackingSessionLine l = (PackingSessionLine) i.next();
            if (line.isSameItem(l)) {
                line.setShipmentItemSeqId(l.getShipmentItemSeqId());
                return false;
            }
        }

        return true;
    }
    
    protected void createPackages() throws GeneralException {
        for (int i = 0; i < packageSeq; i++) {
            String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(i+1, 5);

            Map pkgCtx = FastMap.newInstance();
            pkgCtx.put("shipmentId", shipmentId);
            pkgCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
            //pkgCtx.put("shipmentBoxTypeId", "");
            pkgCtx.put("weight", getPackageWeight(i+1));
            pkgCtx.put("weightUomId", getWeightUomId());
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

    protected void updateShipmentRouteSegments() throws GeneralException {
        Double shipmentWeight = new Double(getTotalWeight());
        if (shipmentWeight.doubleValue() <= 0) return;
        List shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", this.getShipmentId()));
        if (! UtilValidate.isEmpty(shipmentRouteSegments)) {
            Iterator srit = shipmentRouteSegments.iterator();
            while (srit.hasNext()) {
                GenericValue shipmentRouteSegment = (GenericValue) srit.next();
                shipmentRouteSegment.set("billingWeight", shipmentWeight);
                shipmentRouteSegment.set("billingWeightUomId", getWeightUomId());
            }
            getDelegator().storeAll(shipmentRouteSegments);
        }
    }
 
    protected void setShipmentToPacked() throws GeneralException {
        Map packedCtx = UtilMisc.toMap("shipmentId", shipmentId, "statusId", "SHIPMENT_PACKED", "userLogin", userLogin);
        Map packedResp = this.getDispatcher().runSync("updateShipment", packedCtx);
        if (packedResp != null && ServiceUtil.isError(packedResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packedResp));
        }
    }

    protected void setPickerOnPicklist() throws GeneralException {
        if (picklistBinId != null) {
            // first find the picklist id
            GenericValue bin = this.getDelegator().findByPrimaryKey("PicklistBin", UtilMisc.toMap("picklistBinId", picklistBinId));
            if (bin != null) {
                Map ctx = FastMap.newInstance();
                ctx.put("picklistId", bin.getString("picklistId"));
                ctx.put("partyId", pickerPartyId);
                ctx.put("roleTypeId", "PICKER");

                // check if the role already exists and is valid
                List currentRoles = this.getDelegator().findByAnd("PicklistRole", ctx);
                currentRoles = EntityUtil.filterByDate(currentRoles);

                // if not; create the role
                if (currentRoles != null && currentRoles.size() > 0) {
                    ctx.put("userLogin", userLogin);
                    Map addRole = this.getDispatcher().runSync("createPicklistRole", ctx);
                    if (ServiceUtil.isError(addRole)) {
                        throw new GeneralException(ServiceUtil.getErrorMessage(addRole));
                    }
                }
            }
        }
    }

    public Double getAdditionalShippingCharge() {
        return additionalShippingCharge;
    }

    public void setAdditionalShippingCharge(Double additionalShippingCharge) {
        this.additionalShippingCharge = additionalShippingCharge;
    }
    
    public double getTotalWeight() {
        double total = 0.0;
        for (int i = 0; i < packageSeq; i++) {
            Double packageWeight = getPackageWeight(i);
            if (! UtilValidate.isEmpty(packageWeight)) {
                total += packageWeight.doubleValue();
            }
        }
        return total;
    }

    public Double getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId, List shippableItemInfo, Double shippableTotal, Double shippableWeight, Double shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"), 
                                       productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }
    
    public Double getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"), 
                                       productStoreId, null, null, null, null);
    }
    
    public Double getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId, String productStoreId, List shippableItemInfo, Double shippableTotal, Double shippableWeight, Double shippableQuantity) {

        Double shipmentCostEstimate = null;
        Map serviceResult = null;
        try {
            Map serviceContext = FastMap.newInstance();
            serviceContext.put("shippingContactMechId", shippingContactMechId);
            serviceContext.put("shipmentMethodTypeId", shipmentMethodTypeId);
            serviceContext.put("carrierPartyId", carrierPartyId);
            serviceContext.put("carrierRoleTypeId", carrierRoleTypeId);
            serviceContext.put("productStoreId", productStoreId);
    
            if (UtilValidate.isEmpty(shippableItemInfo)) {
                shippableItemInfo = FastList.newInstance();
                Iterator lit = getLines().iterator();
                while (lit.hasNext()) {
                    PackingSessionLine line = (PackingSessionLine) lit.next();
                    List oiasgas = getDelegator().findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", line.getOrderId(), "orderItemSeqId", line.getOrderItemSeqId(), "shipGroupSeqId", line.getShipGroupSeqId()));
                    shippableItemInfo.addAll(oiasgas);
                }
            }
            serviceContext.put("shippableItemInfo", shippableItemInfo);

            if (UtilValidate.isEmpty(shippableWeight)) {
                shippableWeight = new Double(getTotalWeight());
            }
            serviceContext.put("shippableWeight", shippableWeight);

            if (UtilValidate.isEmpty(shippableQuantity)) {
                shippableQuantity = new Double(getPackedQuantity(-1));
            }
            serviceContext.put("shippableQuantity", shippableQuantity);

            if (UtilValidate.isEmpty(shippableTotal)) {
                shippableTotal = new Double(0);
            }
            serviceContext.put("shippableTotal", shippableTotal);
    
            serviceResult = getDispatcher().runSync("calcShipmentCostEstimate", serviceContext);
        } catch( GenericEntityException e ) {
            Debug.logError(e, module);
        } catch( GenericServiceException e ) {
            Debug.logError(e, module);
        }
        
        if (! UtilValidate.isEmpty(serviceResult.get("shippingEstimateAmount"))) {
            shipmentCostEstimate = (Double) serviceResult.get("shippingEstimateAmount");
        }
        
        return shipmentCostEstimate;
        
    }
   
    public String getWeightUomId() {
        return weightUomId;
    }

    public void setWeightUomId(String weightUomId) {
        this.weightUomId = weightUomId;
    }
    
    public List getPackageSeqIds() {
        Set packageSeqIds = new TreeSet();
        if (! UtilValidate.isEmpty(this.getLines())) {
            Iterator lit = this.getLines().iterator();
            while (lit.hasNext()) {
                PackingSessionLine line = (PackingSessionLine) lit.next();
                packageSeqIds.add(new Integer(line.getPackageSeq()));
            }
        }
        return new ArrayList(packageSeqIds);
    }
    
    public void setPackageWeight(int packageSeqId, Double packageWeight) {
        if (UtilValidate.isEmpty(packageWeight)) {
            packageWeights.remove(new Integer(packageSeqId));
        } else {
            packageWeights.put(new Integer(packageSeqId), packageWeight);
        }
    }
    
    public Double getPackageWeight(int packageSeqId) {
        if (this.packageWeights == null) return null;
        Double packageWeight = null;
        Object p = packageWeights.get(new Integer(packageSeqId));
        if (p != null) {
            packageWeight = (Double) p;
        }
        return packageWeight;
    }
    
    public void addToPackageWeight(int packageSeqId, Double weight) {
        if (UtilValidate.isEmpty(weight)) return;
        Double packageWeight = getPackageWeight(packageSeqId);
        Double newPackageWeight = UtilValidate.isEmpty(packageWeight) ? weight : new Double(weight.doubleValue() + packageWeight.doubleValue());
        setPackageWeight(packageSeqId, newPackageWeight);
    }
    
}
