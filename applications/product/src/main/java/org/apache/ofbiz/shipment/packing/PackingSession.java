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
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
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
    protected String shipmentBoxTypeId = null;
    protected BigDecimal additionalShippingCharge = null;
    protected Map<Integer, BigDecimal> packageWeights = null;
    protected List<PackingEvent> packEvents = null;
    protected List<PackingSessionLine> packLines = null;
    protected List<ItemDisplay> itemInfos = null;
    protected int packageSeq = -1;
    protected int status = 1;
    protected Map<Integer, String> shipmentBoxTypes = null;

    private transient Delegator _delegator = null;
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
        this.packLines = new LinkedList<PackingSessionLine>();
        this.packEvents = new LinkedList<PackingEvent>();
        this.itemInfos = new LinkedList<PackingSession.ItemDisplay>();
        this.packageSeq = 1;
        this.packageWeights = new HashMap<Integer, BigDecimal>();
        this.shipmentBoxTypes = new HashMap<Integer, String>();
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null, null);
    }

    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null, null);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, int packageSeqId, BigDecimal weight, boolean update) throws GeneralException {
        // reset the session if we just completed
        if (status == 0) {
            throw new GeneralException("Packing session has been completed; be sure to CLEAR before packing a new order! [000]");
        }

        // do nothing if we are trying to add a quantity of 0
        if (!update && quantity.compareTo(BigDecimal.ZERO) == 0) {
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
        Map<String, Object> invLookup = new HashMap<String, Object>();
        invLookup.put("orderId", orderId);
        invLookup.put("orderItemSeqId", orderItemSeqId);
        invLookup.put("shipGroupSeqId", shipGroupSeqId);
        List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup, UtilMisc.toList("quantity DESC"), false);

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
            Map<GenericValue, BigDecimal> toCreateMap = new HashMap<GenericValue, BigDecimal>();
            Iterator<GenericValue> i = reservations.iterator();
            BigDecimal qtyRemain = quantity;

            while (i.hasNext() && qtyRemain.compareTo(BigDecimal.ZERO) > 0) {
                GenericValue res = i.next();

                // Check that the inventory item product match with the current product to pack
                if (!productId.equals(res.getRelatedOne("InventoryItem", false).getString("productId"))) {
                    continue;
                }

                BigDecimal resQty = res.getBigDecimal("quantity");
                BigDecimal resPackedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId, res.getString("inventoryItemId"), -1);
                if (resPackedQty.compareTo(resQty) >= 0) {
                    continue;
                } else if (!update) {
                    resQty = resQty.subtract(resPackedQty);
                }

                BigDecimal thisQty = resQty.compareTo(qtyRemain) > 0 ? qtyRemain : resQty;

                int thisCheck = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, productId, thisQty, packageSeqId, update);
                switch (thisCheck) {
                    case 2:
                        Debug.logInfo("Packing check returned '2' - new pack line will be created!", module);
                        toCreateMap.put(res, thisQty);
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 1:
                        Debug.logInfo("Packing check returned '1' - existing pack line has been updated!", module);
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 0:
                        Debug.logInfo("Packing check returned '0' - doing nothing.", module);
                        break;
                }
            }

            if (qtyRemain.compareTo(BigDecimal.ZERO) == 0) {
                for (Map.Entry<GenericValue, BigDecimal> entry: toCreateMap.entrySet()) {
                    GenericValue res = entry.getKey();
                    BigDecimal qty = entry.getValue();
                    this.createPackLineItem(2, res, orderId, orderItemSeqId, shipGroupSeqId, productId, qty, weight, packageSeqId);
                }
            } else {
                throw new GeneralException("Not enough inventory reservation available; cannot pack the item! [103]");
            }
        }

        // run the add events
        this.runEvents(PackingEvent.EVENT_CODE_ADD);
    }

    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, BigDecimal quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, null, quantity, packageSeqId, BigDecimal.ZERO, false);
    }

    public void addOrIncreaseLine(String productId, BigDecimal quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(null, null, null, productId, quantity, packageSeqId, BigDecimal.ZERO, false);
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

    protected void createPackLineItem(int checkCode, GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, BigDecimal weight, int packageSeqId) throws GeneralException {
        // process the result; add new item if necessary
        switch (checkCode) {
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
        if (weight.compareTo(BigDecimal.ZERO) > 0) this.addToPackageWeight(packageSeqId, weight);

        // update the package sequence
        if (packageSeqId > packageSeq) {
            this.packageSeq = packageSeqId;
        }
    }

    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, BigDecimal quantity) throws GeneralException {
        Map<String, Object> lookupMap = new HashMap<String, Object>();
        lookupMap.put("orderId", orderId);
        lookupMap.put("productId", productId);
        lookupMap.put("statusId", "ITEM_APPROVED");
        lookupMap.put("shipGroupSeqId", shipGroupSeqId);

        List<String> sort = UtilMisc.toList("-quantity");
        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItemAndShipGroupAssoc", lookupMap, sort, false);

        String orderItemSeqId = null;
        if (orderItems != null) {
            for (GenericValue item: orderItems) {
                // get the reservations for the item
                Map<String, Object> invLookup = new HashMap<String, Object>();
                invLookup.put("orderId", orderId);
                invLookup.put("orderItemSeqId", item.getString("orderItemSeqId"));
                invLookup.put("shipGroupSeqId", shipGroupSeqId);
                List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", invLookup, null, false);
                for (GenericValue res: reservations) {
                    BigDecimal qty = res.getBigDecimal("quantity");
                    if (quantity.compareTo(qty) <= 0) {
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

    protected int checkLineForAdd(GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity, int packageSeqId, boolean update) {
        // check to see if the reservation can hold the requested quantity amount
        String invItemId = res.getString("inventoryItemId");
        BigDecimal resQty = res.getBigDecimal("quantity");

        PackingSessionLine line = this.findLine(orderId, orderItemSeqId, shipGroupSeqId, productId, invItemId, packageSeqId);
        BigDecimal packedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId);

        Debug.logInfo("Packed quantity [" + packedQty + "] + [" + quantity + "]", module);

        if (line == null) {
            Debug.logInfo("No current line found testing [" + invItemId + "] R: " + resQty + " / Q: " + quantity, module);
            if (resQty.compareTo(quantity) < 0) {
                return 0;
            } else {
                return 2;
            }
        } else {
            BigDecimal newQty = update ? quantity : (line.getQuantity().add(quantity));
            Debug.logInfo("Existing line found testing [" + invItemId + "] R: " + resQty + " / Q: " + newQty, module);
            if (resQty.compareTo(newQty) < 0) {
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

    /**
     * <p>Delivers all the packing lines grouped by package.</p>
     * <p>Output map:
     * <ul>
     * <li>packageMap - a Map of type Map<Integer, List<PackingSessionLine>>
     * that maps package sequence ids to the lines that belong in
     * that package</li>
     * <li>sortedKeys - a List of type List<Integer> with the sorted package
     * sequence numbers to index the packageMap</li>
     * @return result Map with packageMap and sortedKeys
     */
    public Map<Object, Object> getPackingSessionLinesByPackage() {
        Map<Integer, List<PackingSessionLine>> packageMap = new HashMap<Integer, List<PackingSessionLine>>();
        for (PackingSessionLine line : packLines) {
           int pSeq = line.getPackageSeq();
           List<PackingSessionLine> packageLineList = packageMap.get(pSeq);
           if (packageLineList == null) {
               packageLineList = new LinkedList<PackingSessionLine>();
               packageMap.put(pSeq, packageLineList);
           }
           packageLineList.add(line);
        }
        Object[] keys = packageMap.keySet().toArray();
        java.util.Arrays.sort(keys);
        List<Object> sortedKeys = new LinkedList<Object>();
        for (Object key : keys) {
            sortedKeys.add(key);
        }
        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("packageMap", packageMap);
        result.put("sortedKeys", sortedKeys);
        return result;
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

    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId,  productId, null, -1);
    }

    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, int packageSeq) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId,  productId, null, packageSeq);
    }

    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId, int packageSeq) {
        BigDecimal total = BigDecimal.ZERO;
        for (PackingSessionLine line: this.getLines()) {
            if (orderId.equals(line.getOrderId()) && orderItemSeqId.equals(line.getOrderItemSeqId()) &&
                    shipGroupSeqId.equals(line.getShipGroupSeqId()) && productId.equals(line.getProductId())) {
                if (inventoryItemId == null || inventoryItemId.equals(line.getInventoryItemId())) {
                    if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                        total = total.add(line.getQuantity());
                    }
                }
            }
        }
        return total;
    }

    public BigDecimal getPackedQuantity(String productId, int packageSeq) {
        if (productId != null) {
            try {
                productId = ProductWorker.findProductId(this.getDelegator(), productId);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        if (productId != null) {
            for (PackingSessionLine line: this.getLines()) {
                if (productId.equals(line.getProductId())) {
                    if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                        total = total.add(line.getQuantity());
                    }
                }
            }
        }
        return total;
    }

    public BigDecimal getPackedQuantity(int packageSeq) {
        BigDecimal total = BigDecimal.ZERO;
        for (PackingSessionLine line: this.getLines()) {
            if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                total = total.add(line.getQuantity());
            }
        }
        return total;
    }

    public BigDecimal getPackedQuantity(String productId) {
        return getPackedQuantity(productId, -1);
    }

    public BigDecimal getCurrentReservedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        BigDecimal reserved = BigDecimal.ONE.negate();
        try {
            GenericValue res = EntityUtil.getFirst(this.getDelegator().findByAnd("OrderItemAndShipGrpInvResAndItemSum", UtilMisc.toMap("orderId", orderId,
                    "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId, "inventoryProductId", productId), null, false));
            reserved = res.getBigDecimal("totQuantityAvailable");
            if (reserved == null) {
                reserved = BigDecimal.ONE.negate();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return reserved;
    }

    public BigDecimal getCurrentShippedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        BigDecimal shipped = BigDecimal.ZERO;
        List<GenericValue> issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);
        if (issues != null) {
            for (GenericValue v: issues) {
                BigDecimal qty = v.getBigDecimal("quantity");
                if (qty == null) qty = BigDecimal.ZERO;
                shipped = shipped.add(qty);
            }
        }

        return shipped;
    }

    public List<String> getCurrentShipmentIds(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        Set<String> shipmentIds = new HashSet<String>();
        List<GenericValue> issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);

        if (issues != null) {
            for (GenericValue v: issues) {
                shipmentIds.add(v.getString("shipmentId"));
            }
        }

        List<String> retList = new LinkedList<String>();
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
            _dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return _dispatcher;
    }

    public Delegator getDelegator() {
        if (_delegator == null) {
            _delegator = DelegatorFactory.getDelegator(delegatorName);
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
        //return --packageSeq;
        return packageSeq;
    }

    public void clearLine(PackingSessionLine line) {
        this.packLines.remove(line);
        BigDecimal packageWeight = this.packageWeights.get(line.packageSeq);
        if (packageWeight != null) {
            packageWeight = packageWeight.subtract(line.weight);
            if (packageWeight.compareTo(BigDecimal.ZERO) < 0) {
                packageWeight = BigDecimal.ZERO;
            }
            this.packageWeights.put(line.packageSeq, packageWeight);
        }
        if (line.packageSeq == packageSeq && packageSeq > 1) {
            packageSeq--;
        }
    }

    public void clearAllLines() {
        this.packLines.clear();
        this.packageWeights.clear();
        this.shipmentBoxTypes.clear();
        this.packageSeq = 1;
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
        if (this.shipmentBoxTypes != null) this.shipmentBoxTypes.clear();
        this.weightUomId = null;
        this.packageSeq = 1;
        this.status = 1;
        this.runEvents(PackingEvent.EVENT_CODE_CLEAR);
    }

    public String complete(boolean force) throws GeneralException {
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
        List<String> errors = new LinkedList<String>();
        for (PackingSessionLine line: this.getLines()) {
            BigDecimal reservedQty =  this.getCurrentReservedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(), line.getProductId());
            BigDecimal packedQty = this.getPackedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(), line.getProductId());

            if (packedQty.compareTo(reservedQty) != 0) {
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
        List<PackingSessionLine> lines = new LinkedList<PackingSessionLine>();
        lines.addAll(this.getLines());
        for (PackingSessionLine l: lines) {
            if (l.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
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

        Map<String, Object> lookupMap = new HashMap<String, Object>();
        lookupMap.put("orderId", orderId);
        if (UtilValidate.isNotEmpty(orderItemSeqId)) {
            lookupMap.put("orderItemSeqId", orderItemSeqId);
        }
        if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
            lookupMap.put("shipGroupSeqId", shipGroupSeqId);
        }
        try {
            issues = this.getDelegator().findByAnd("ItemIssuance",  lookupMap, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return issues;
    }

    protected void createShipment() throws GeneralException {
        // first create the shipment
        Delegator delegator = this.getDelegator();
        Map<String, Object> newShipment = new HashMap<String, Object>();
        newShipment.put("originFacilityId", this.facilityId);
        newShipment.put("primaryShipGroupSeqId", primaryShipGrp);
        newShipment.put("primaryOrderId", primaryOrderId);
        newShipment.put("shipmentTypeId", "OUTGOING_SHIPMENT");
        newShipment.put("statusId", "SHIPMENT_INPUT");
        newShipment.put("handlingInstructions", instructions);
        newShipment.put("picklistBinId", picklistBinId);
        newShipment.put("additionalShippingCharge", additionalShippingCharge);
        newShipment.put("userLogin", userLogin);
        GenericValue orderRoleShipTo = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId, "roleTypeId", "SHIP_TO_CUSTOMER").queryFirst();
        if (UtilValidate.isNotEmpty(orderRoleShipTo)) {
            newShipment.put("partyIdTo", orderRoleShipTo.getString("partyId"));
        }
        String partyIdFrom = null;
        if (primaryOrderId != null) {
            GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", primaryOrderId, "shipGroupSeqId", primaryShipGrp).queryFirst();
            if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("vendorPartyId"))) {
                partyIdFrom = orderItemShipGroup.getString("vendorPartyId");
            } else if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("facilityId"))) {
                GenericValue facility = EntityQuery.use(delegator).from("Facility").where("facilityId", orderItemShipGroup.getString("facilityId")).queryOne();
                if (UtilValidate.isNotEmpty(facility.getString("ownerPartyId"))) {
                    partyIdFrom = facility.getString("ownerPartyId");
                }
            }
            if (UtilValidate.isEmpty(partyIdFrom)) {
                GenericValue orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId, "roleTypeId", "SHIP_FROM_VENDOR").queryFirst();
                if (UtilValidate.isNotEmpty(orderRoleShipFrom)) {
                    partyIdFrom = orderRoleShipFrom.getString("partyId");
                } else {
                    orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId, "roleTypeId", "BILL_FROM_VENDOR").queryFirst();
                    partyIdFrom = orderRoleShipFrom.getString("partyId");
                }
            }
        } else if (this.facilityId != null) {
            GenericValue facility = EntityQuery.use(delegator).from("Facility").where("facilityId", this.facilityId).queryOne();
            if (UtilValidate.isNotEmpty(facility.getString("ownerPartyId"))) {
                partyIdFrom = facility.getString("ownerPartyId");
            }
        }

        newShipment.put("partyIdFrom", partyIdFrom);
        Debug.logInfo("Creating new shipment with context: " + newShipment, module);
        Map<String, Object> newShipResp = this.getDispatcher().runSync("createShipment", newShipment);

        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        this.shipmentId = (String) newShipResp.get("shipmentId");
    }

    protected void issueItemsToShipment() throws GeneralException {
        List<PackingSessionLine> processedLines = new LinkedList<PackingSessionLine>();
        for (PackingSessionLine line: this.getLines()) {
            if (this.checkLine(processedLines, line)) {
                BigDecimal totalPacked = this.getPackedQuantity(line.getOrderId(),  line.getOrderItemSeqId(),
                        line.getShipGroupSeqId(), line.getProductId(), line.getInventoryItemId(), -1);

                line.issueItemToShipment(shipmentId, picklistBinId, userLogin, totalPacked, getDispatcher());
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

            Map<String, Object> pkgCtx = new HashMap<String, Object>();
            pkgCtx.put("shipmentId", shipmentId);
            pkgCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
            pkgCtx.put("shipmentBoxTypeId", getShipmentBoxType(i+1));
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
        BigDecimal shipmentWeight = getTotalWeight();
        if (shipmentWeight.compareTo(BigDecimal.ZERO) <= 0) return;
        List<GenericValue> shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId", this.getShipmentId()), null, false);
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
            GenericValue bin = this.getDelegator().findOne("PicklistBin", UtilMisc.toMap("picklistBinId", picklistBinId), false);
            if (bin != null) {
                Map<String, Object> ctx = new HashMap<String, Object>();
                ctx.put("picklistId", bin.getString("picklistId"));
                ctx.put("partyId", pickerPartyId);
                ctx.put("roleTypeId", "PICKER");

                // check if the role already exists and is valid
                List<GenericValue> currentRoles = this.getDelegator().findByAnd("PicklistRole", ctx, null, false);
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

    public BigDecimal getAdditionalShippingCharge() {
        return additionalShippingCharge;
    }

    public void setAdditionalShippingCharge(BigDecimal additionalShippingCharge) {
        this.additionalShippingCharge = additionalShippingCharge;
    }

    public BigDecimal getTotalWeight() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < packageSeq; i++) {
            BigDecimal packageWeight = getPackageWeight(i);
            if (! UtilValidate.isEmpty(packageWeight)) {
                total = total.add(packageWeight);
            }
        }
        return total;
    }

    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId, List<GenericValue> shippableItemInfo, BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }

    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       productStoreId, null, null, null, null);
    }

    public BigDecimal getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId, String productStoreId, List<GenericValue> shippableItemInfo, BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {

        BigDecimal shipmentCostEstimate = null;
        Map<String, Object> serviceResult = null;
        try {
            Map<String, Object> serviceContext = new HashMap<String, Object>();
            serviceContext.put("shippingContactMechId", shippingContactMechId);
            serviceContext.put("shipmentMethodTypeId", shipmentMethodTypeId);
            serviceContext.put("carrierPartyId", carrierPartyId);
            serviceContext.put("carrierRoleTypeId", carrierRoleTypeId);
            serviceContext.put("productStoreId", productStoreId);

            if (UtilValidate.isEmpty(shippableItemInfo)) {
                shippableItemInfo = new LinkedList<GenericValue>();
                for (PackingSessionLine line: getLines()) {
                    List<GenericValue> oiasgas = getDelegator().findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId", line.getOrderId(), "orderItemSeqId", line.getOrderItemSeqId(), "shipGroupSeqId", line.getShipGroupSeqId()), null, false);
                    shippableItemInfo.addAll(oiasgas);
                }
            }
            serviceContext.put("shippableItemInfo", shippableItemInfo);

            if (UtilValidate.isEmpty(shippableWeight)) {
                shippableWeight = getTotalWeight();
            }
            serviceContext.put("shippableWeight", shippableWeight);

            if (UtilValidate.isEmpty(shippableQuantity)) {
                shippableQuantity = getPackedQuantity(-1);
            }
            serviceContext.put("shippableQuantity", shippableQuantity);

            if (UtilValidate.isEmpty(shippableTotal)) {
                shippableTotal = BigDecimal.ZERO;
            }
            serviceContext.put("shippableTotal", shippableTotal);

            serviceResult = getDispatcher().runSync("calcShipmentCostEstimate", serviceContext);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(serviceResult)) {
            shipmentCostEstimate = (BigDecimal) serviceResult.get("shippingEstimateAmount");
        }

        return shipmentCostEstimate;

    }

    public String getWeightUomId() {
        return weightUomId;
    }

    public void setWeightUomId(String weightUomId) {
        this.weightUomId = weightUomId;
    }

    public void setShipmentBoxTypeId(String shipmentBoxTypeId) {
        this.shipmentBoxTypeId = shipmentBoxTypeId;
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

    public void setPackageWeight(int packageSeqId, BigDecimal packageWeight) {
        if (UtilValidate.isEmpty(packageWeight)) {
            packageWeights.remove(Integer.valueOf(packageSeqId));
        } else {
            packageWeights.put(Integer.valueOf(packageSeqId), packageWeight);
        }
    }

    public BigDecimal getPackageWeight(int packageSeqId) {
        if (this.packageWeights == null) return null;
        BigDecimal packageWeight = null;
        Object p = packageWeights.get(packageSeqId);
        if (p != null) {
            packageWeight = (BigDecimal) p;
        }
        return packageWeight;
    }

    public void addToPackageWeight(int packageSeqId, BigDecimal weight) {
        if (UtilValidate.isEmpty(weight)) return;
        BigDecimal packageWeight = getPackageWeight(packageSeqId);
        BigDecimal newPackageWeight = UtilValidate.isEmpty(packageWeight) ? weight : weight.add(packageWeight);
        setPackageWeight(packageSeqId, newPackageWeight);
    }

    // These methods (setShipmentBoxType and getShipmentBoxType) are added so that each package will have different box type.
    public void setShipmentBoxType(int packageSeqId, String shipmentBoxType) {
        if (UtilValidate.isEmpty(shipmentBoxType)) {
            shipmentBoxTypes.remove(Integer.valueOf(packageSeqId));
        } else {
            shipmentBoxTypes.put(Integer.valueOf(packageSeqId), shipmentBoxType);
        }
    }

    public String getShipmentBoxType(int packageSeqId) {
        if (this.shipmentBoxTypes == null) return null;
        String shipmentBoxType = null;
        Object p = shipmentBoxTypes.get(packageSeqId);
        if (p != null) {
            shipmentBoxType = (String) p;
        }
        return shipmentBoxType;
    }

    class ItemDisplay extends AbstractMap<Object, Object> {

        public GenericValue orderItem;
        public BigDecimal quantity;
        public String productId;

        public ItemDisplay(GenericValue v) {
            if ("PicklistItem".equals(v.getEntityName())) {
                quantity = v.getBigDecimal("quantity").setScale(2, BigDecimal.ROUND_HALF_UP);
                try {
                    orderItem = v.getRelatedOne("OrderItem", false);
                    productId = v.getRelatedOne("InventoryItem", false).getString("productId");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            } else {
                // this is an OrderItemAndShipGrpInvResAndItemSum
                orderItem = v;
                productId = v.getString("inventoryProductId");
                quantity = v.getBigDecimal("totQuantityReserved").setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            Debug.logInfo("created item display object quantity: " + quantity + " (" + productId + ")", module);
        }

        public GenericValue getOrderItem() {
            return orderItem;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return null;
        }

        @Override
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

        @Override
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
