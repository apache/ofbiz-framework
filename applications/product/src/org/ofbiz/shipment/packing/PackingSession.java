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
import java.math.BigDecimal;

import javolution.util.FastMap;
import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
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
    protected Map<Integer, Double> packageWeights = null;
    protected List<PackingEvent> packEvents = null;
    protected List<PackingSessionLine> packLines = null;
    protected List<ItemDisplay> itemInfos = null;
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
        this.packLines = FastList.newInstance();
        this.packEvents = FastList.newInstance();
        this.itemInfos = FastList.newInstance();
        this.packageSeq = 1;
        this.packageWeights = FastMap.newInstance();
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
        Map<String, Object> invLookup = FastMap.newInstance();
        invLookup.put("orderId", orderId);
        invLookup.put("orderItemSeqId", orderItemSeqId);
        invLookup.put("shipGroupSeqId", shipGroupSeqId);
        List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup, UtilMisc.toList("quantity DESC"));

        // no reservations we cannot add this item
        if (UtilValidate.isEmpty(reservations)) {
            throw new GeneralException("No inventory reservations available; cannot pack this item! [101]");
        }

        // find the inventoryItemId to use
        if (reservations.size() == 1) {
            GenericValue res = EntityUtil.getFirst(reservations);
            int checkCode = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, packageSeqId, update);
            this.createPackLineItem(checkCode, res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, weight, packageSeqId);
        } else {
            // more than one reservation found
            Map<GenericValue, Double> toCreateMap = FastMap.newInstance();
            Iterator<GenericValue> i = reservations.iterator();
            double qtyRemain = quantity;

            while (i.hasNext() && qtyRemain > 0) {
                GenericValue res = i.next();

                // Check that the inventory item product match with the current product to pack
                if (!productId.equals(res.getRelatedOne("InventoryItem").getString("productId"))) {
                    continue;
                }

                double resQty = res.getDouble("quantity").doubleValue();
                double resPackedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId, res.getString("inventoryItemId"), -1);
                if (resPackedQty >= resQty) {
                    continue;
                } else if (!update) {
                    resQty -= resPackedQty;
                }
                
                double thisQty = resQty > qtyRemain ? qtyRemain : resQty;

                int thisCheck = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, productId, thisQty, packageSeqId, update);
                switch (thisCheck) {
                    case 2:
                        Debug.log("Packing check returned '2' - new pack line will be created!", module);
                        toCreateMap.put(res, Double.valueOf(thisQty));
                        qtyRemain -= thisQty;
                        break;
                    case 1:
                        Debug.log("Packing check returned '1' - existing pack line has been updated!", module);
                        qtyRemain -= thisQty;
                        break;
                    case 0:
                        Debug.log("Packing check returned '0' - doing nothing.", module);
                        break;
                }
            }

            if (qtyRemain == 0) {
                for (Map.Entry<GenericValue, Double> entry: toCreateMap.entrySet()) {
                    GenericValue res = entry.getKey();
                    Double qty = entry.getValue();
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

    public PackingSessionLine findLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, int packageSeq) {
        for (PackingSessionLine line: this.getLines()) {
            if (orderId.equals(line.getOrderId()) &&
                    orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId()) &&
                    productId.equals(line.getProductId()) &&
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
        if (weight > 0) this.addToPackageWeight(packageSeqId, Double.valueOf(weight));
        
        // update the package sequence
        if (packageSeqId > packageSeq) {
            this.packageSeq = packageSeqId;
        }
    }

    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, double quantity) throws GeneralException {
        Map<String, Object> lookupMap = FastMap.newInstance();
        lookupMap.put("orderId", orderId);
        lookupMap.put("productId", productId);
        lookupMap.put("statusId", "ITEM_APPROVED");
        lookupMap.put("shipGroupSeqId", shipGroupSeqId);

        List<String> sort = UtilMisc.toList("-quantity");
        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItemAndShipGroupAssoc", lookupMap, sort);

        String orderItemSeqId = null;
        if (orderItems != null) {
            for (GenericValue item: orderItems) {
                // get the reservations for the item
                Map<String, Object> invLookup = FastMap.newInstance();
                invLookup.put("orderId", orderId);
                invLookup.put("orderItemSeqId", item.getString("orderItemSeqId"));
                invLookup.put("shipGroupSeqId", shipGroupSeqId);
                List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup);
                for (GenericValue res: reservations) {
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

    protected int checkLineForAdd(GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, double quantity, int packageSeqId, boolean update) {
        // check to see if the reservation can hold the requested quantity amount
        String invItemId = res.getString("inventoryItemId");
        double resQty = res.getDouble("quantity").doubleValue();

        PackingSessionLine line = this.findLine(orderId, orderItemSeqId, shipGroupSeqId, productId, invItemId, packageSeqId);
        double packedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId);

        Debug.log("Packed quantity [" + packedQty + "] + [" + quantity + "]", module);

        if (line == null) {
            Debug.log("No current line found testing [" + invItemId + "] R: " + resQty + " / Q: " + quantity, module);
            if (resQty < quantity) {
                return 0;
            } else {
                return 2;
            }
        } else {
            double newQty = update ? quantity : (line.getQuantity() + quantity);
            Debug.log("Existing line found testing [" + invItemId + "] R: " + resQty + " / Q: " + newQty, module);
            if (resQty < newQty) {
                return 0;
            } else {
                line.setQuantity(newQty);
                return 1;
            }
        }
    }

    public void addItemInfo(List<GenericValue> infos) {
        for (GenericValue v: infos) {
            ItemDisplay newItem = new ItemDisplay(v);
            int currentIdx = itemInfos.indexOf(newItem);
            if (currentIdx != -1) {
                ItemDisplay existingItem = itemInfos.get(currentIdx);
                existingItem.quantity = existingItem.quantity.add(newItem.quantity);
            } else {
                itemInfos.add(newItem);
            }
        }
    }

    public List<ItemDisplay> getItemInfos() {
        return itemInfos;
    }

    public void clearItemInfos() {
        itemInfos.clear();
    }

    public String getShipmentId() {
        return this.shipmentId;
    }

    public List<PackingSessionLine> getLines() {
        return this.packLines;
    }

    public int nextPackageSeq() {
        return ++packageSeq;
    }

    public int getCurrentPackageSeq() {
        return packageSeq;
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId,  productId, null, -1);
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, int packageSeq) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId,  productId, null, packageSeq);
    }

    public double getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, int packageSeq) {
        double total = 0.0;
        for (PackingSessionLine line: this.getLines()) {
            if (orderId.equals(line.getOrderId()) && orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId()) && productId.equals(line.getProductId())) {
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
            for (PackingSessionLine line: this.getLines()) {
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
        for (PackingSessionLine line: this.getLines()) {
            if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                total += line.getQuantity();
            }
        }
        return total;
    }

    public double getPackedQuantity(String productId) {
        return getPackedQuantity(productId, -1);
    }

    public double getCurrentReservedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        double reserved = -1;
        try {
            GenericValue res = EntityUtil.getFirst(this.getDelegator().findByAnd("OrderItemAndShipGrpInvResAndItemSum", UtilMisc.toMap("orderId", orderId,
                    "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId, "inventoryProductId", productId)));
            Double reservedDbl = res.getDouble("totQuantityAvailable");
            if (reservedDbl == null) {
                reservedDbl = Double.valueOf(-1);
            }
            reserved = reservedDbl.doubleValue();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return reserved;
    }

    public double getCurrentShippedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        double shipped = 0.0;
        List<GenericValue> issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);
        if (issues != null) {
            for (GenericValue v: issues) {
                Double qty = v.getDouble("quantity");
                if (qty == null) qty = Double.valueOf(0);
                shipped += qty.doubleValue();
            }
        }

        return shipped;
    }

    public List<String> getCurrentShipmentIds(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        Set<String> shipmentIds = FastSet.newInstance();
        List<GenericValue> issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);

        if (issues != null) {
            for (GenericValue v: issues) {
                shipmentIds.add(v.getString("shipmentId"));
            }
        }

        List<String> retList = FastList.newInstance();
        retList.addAll(shipmentIds);
        return retList;
    }

    public List<String> getCurrentShipmentIds(String orderId, String shipGroupSeqId) {
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
        
        List<PackingSessionLine> currentLines = UtilMisc.makeListWritable(this.packLines);
        for (PackingSessionLine line: currentLines) {
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
        List<String> errors = FastList.newInstance();        
        for (PackingSessionLine line: this.getLines()) {
            double reservedQty =  this.getCurrentReservedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(), line.getProductId());
            double packedQty = this.getPackedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(), line.getProductId());

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
        List<PackingSessionLine> lines = FastList.newInstance();
        lines.addAll(this.getLines());
        for (PackingSessionLine l: lines) {
            if (l.getQuantity() == 0) {
                this.packLines.remove(l);
            }
        }
    }

    protected void runEvents(int eventCode) {
        if (this.packEvents.size() > 0) {
            for (PackingEvent event: this.packEvents) {
                event.runEvent(this, eventCode);
            }
        }
    }

    protected List<GenericValue> getItemIssuances(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        List<GenericValue> issues = null;
        if (orderId == null) {
            throw new IllegalArgumentException("Value for orderId is  null");
        }

        Map<String, Object> lookupMap = FastMap.newInstance();
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
        Map<String, Object> newShipment = FastMap.newInstance();
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
        Map<String, Object> newShipResp = this.getDispatcher().runSync("createShipment", newShipment);

        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        this.shipmentId = (String) newShipResp.get("shipmentId");
    }

    protected void issueItemsToShipment() throws GeneralException {
        List<PackingSessionLine> processedLines = FastList.newInstance();
        for (PackingSessionLine line: this.getLines()) {
            if (this.checkLine(processedLines, line)) {
                double totalPacked = this.getPackedQuantity(line.getOrderId(),  line.getOrderItemSeqId(),
                        line.getShipGroupSeqId(), line.getProductId(), line.getInventoryItemId(), -1);

                line.issueItemToShipment(shipmentId, picklistBinId, userLogin, Double.valueOf(totalPacked), getDispatcher());
                processedLines.add(line);
            }
        }
    }

    protected boolean checkLine(List<PackingSessionLine> processedLines, PackingSessionLine line) {
        for (PackingSessionLine l: processedLines) {
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

            Map<String, Object> pkgCtx = FastMap.newInstance();
            pkgCtx.put("shipmentId", shipmentId);
            pkgCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
            //pkgCtx.put("shipmentBoxTypeId", "");
            pkgCtx.put("weight", getPackageWeight(i+1));
            pkgCtx.put("weightUomId", getWeightUomId());
            pkgCtx.put("userLogin", userLogin);
            Map<String, Object> newPkgResp = this.getDispatcher().runSync("createShipmentPackage", pkgCtx);

            if (ServiceUtil.isError(newPkgResp)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(newPkgResp));
            }
        }
    }

    protected void applyItemsToPackages() throws GeneralException {
        for (PackingSessionLine line: this.getLines()) {
            line.applyLineToPackage(shipmentId, userLogin, getDispatcher());
        }
    }

    protected void updateShipmentRouteSegments() throws GeneralException {
        Double shipmentWeight = Double.valueOf(getTotalWeight());
        if (shipmentWeight.doubleValue() <= 0) return;
        List<GenericValue> shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", this.getShipmentId()));
        if (! UtilValidate.isEmpty(shipmentRouteSegments)) {
            for (GenericValue shipmentRouteSegment: shipmentRouteSegments) {
                shipmentRouteSegment.set("billingWeight", shipmentWeight);
                shipmentRouteSegment.set("billingWeightUomId", getWeightUomId());
            }
            getDelegator().storeAll(shipmentRouteSegments);
        }
    }
 
    protected void setShipmentToPacked() throws GeneralException {
        Map<String, Object> packedCtx = UtilMisc.toMap("shipmentId", shipmentId, "statusId", "SHIPMENT_PACKED", "userLogin", userLogin);
        Map<String, Object> packedResp = this.getDispatcher().runSync("updateShipment", packedCtx);
        if (packedResp != null && ServiceUtil.isError(packedResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packedResp));
        }
    }

    protected void setPickerOnPicklist() throws GeneralException {
        if (picklistBinId != null) {
            // first find the picklist id
            GenericValue bin = this.getDelegator().findByPrimaryKey("PicklistBin", UtilMisc.toMap("picklistBinId", picklistBinId));
            if (bin != null) {
                Map<String, Object> ctx = FastMap.newInstance();
                ctx.put("picklistId", bin.getString("picklistId"));
                ctx.put("partyId", pickerPartyId);
                ctx.put("roleTypeId", "PICKER");

                // check if the role already exists and is valid
                List<GenericValue> currentRoles = this.getDelegator().findByAnd("PicklistRole", ctx);
                currentRoles = EntityUtil.filterByDate(currentRoles);

                // if not; create the role
                if (UtilValidate.isNotEmpty(currentRoles)) {
                    ctx.put("userLogin", userLogin);
                    Map<String, Object> addRole = this.getDispatcher().runSync("createPicklistRole", ctx);
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

    public Double getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId, List<GenericValue> shippableItemInfo, Double shippableTotal, Double shippableWeight, Double shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"), 
                                       productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }
    
    public Double getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"), 
                                       productStoreId, null, null, null, null);
    }
    
    public Double getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId, String productStoreId, List<GenericValue> shippableItemInfo, Double shippableTotal, Double shippableWeight, Double shippableQuantity) {

        Double shipmentCostEstimate = null;
        Map<String, Object> serviceResult = null;
        try {
            Map<String, Object> serviceContext = FastMap.newInstance();
            serviceContext.put("shippingContactMechId", shippingContactMechId);
            serviceContext.put("shipmentMethodTypeId", shipmentMethodTypeId);
            serviceContext.put("carrierPartyId", carrierPartyId);
            serviceContext.put("carrierRoleTypeId", carrierRoleTypeId);
            serviceContext.put("productStoreId", productStoreId);
    
            if (UtilValidate.isEmpty(shippableItemInfo)) {
                shippableItemInfo = FastList.newInstance();
                for (PackingSessionLine line: getLines()) {
                    List<GenericValue> oiasgas = getDelegator().findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", line.getOrderId(), "orderItemSeqId", line.getOrderItemSeqId(), "shipGroupSeqId", line.getShipGroupSeqId()));
                    shippableItemInfo.addAll(oiasgas);
                }
            }
            serviceContext.put("shippableItemInfo", shippableItemInfo);

            if (UtilValidate.isEmpty(shippableWeight)) {
                shippableWeight = Double.valueOf(getTotalWeight());
            }
            serviceContext.put("shippableWeight", shippableWeight);

            if (UtilValidate.isEmpty(shippableQuantity)) {
                shippableQuantity = Double.valueOf(getPackedQuantity(-1));
            }
            serviceContext.put("shippableQuantity", shippableQuantity);

            if (UtilValidate.isEmpty(shippableTotal)) {
                shippableTotal = Double.valueOf(0);
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
    
    public List<Integer> getPackageSeqIds() {
        Set<Integer> packageSeqIds = new TreeSet<Integer>();
        if (! UtilValidate.isEmpty(this.getLines())) {
            for (PackingSessionLine line: this.getLines()) {
                packageSeqIds.add(Integer.valueOf(line.getPackageSeq()));
            }
        }
        return UtilMisc.makeListWritable(packageSeqIds);
    }
    
    public void setPackageWeight(int packageSeqId, Double packageWeight) {
        if (UtilValidate.isEmpty(packageWeight)) {
            packageWeights.remove(Integer.valueOf(packageSeqId));
        } else {
            packageWeights.put(Integer.valueOf(packageSeqId), packageWeight);
        }
    }
    
    public Double getPackageWeight(int packageSeqId) {
        if (this.packageWeights == null) return null;
        Double packageWeight = packageWeights.get(Integer.valueOf(packageSeqId));
        return packageWeight;
    }
    
    public void addToPackageWeight(int packageSeqId, Double weight) {
        if (UtilValidate.isEmpty(weight)) return;
        Double packageWeight = getPackageWeight(packageSeqId);
        Double newPackageWeight = UtilValidate.isEmpty(packageWeight) ? weight : Double.valueOf(weight.doubleValue() + packageWeight.doubleValue());
        setPackageWeight(packageSeqId, newPackageWeight);
    }

    class ItemDisplay extends AbstractMap {

        public GenericValue orderItem;
        public BigDecimal quantity;
        public String productId;

        public ItemDisplay(GenericValue v) {
            if ("PicklistItem".equals(v.getEntityName())) {
                quantity = v.getBigDecimal("quantity").setScale(2, BigDecimal.ROUND_HALF_UP);
                try {
                    orderItem = v.getRelatedOne("OrderItem");
                    productId = v.getRelatedOne("InventoryItem").getString("productId");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            } else {
                // this is an OrderItemAndShipGrpInvResAndItemSum
                orderItem = v;
                productId = v.getString("inventoryProductId");
                quantity = v.getBigDecimal("totQuantityReserved").setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            Debug.log("created item display object quantity: " + quantity + " (" + productId + ")", module);
        }

        public GenericValue getOrderItem() {
            return orderItem;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public Set entrySet() {
            return null;
        }

        public Object get(Object name) {
            if ("orderItem".equals(name.toString())) {
                return orderItem;
            } else if ("quantity".equals(name.toString())) {
                return quantity;
            } else if ("productId".equals(name.toString())) {
                return productId;
            }
            return null;
        }

        public boolean equals(Object o) {
            if (o instanceof ItemDisplay) {
                ItemDisplay d = (ItemDisplay) o;
                boolean sameOrderItemProduct = true;
                if (d.getOrderItem().getString("productId") != null && orderItem.getString("productId") != null) {
                    sameOrderItemProduct = d.getOrderItem().getString("productId").equals(orderItem.getString("productId"));
                } else if (d.getOrderItem().getString("productId") != null || orderItem.getString("productId") != null) {
                    sameOrderItemProduct = false;
                }
                return (d.productId.equals(productId) &&
                        d.getOrderItem().getString("orderItemSeqId").equals(orderItem.getString("orderItemSeqId")) && 
                        sameOrderItemProduct);
            } else {
                return false;
            }            
        }
    }
}
