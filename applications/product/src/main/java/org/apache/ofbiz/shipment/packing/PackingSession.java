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
import java.math.RoundingMode;
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

    private static final String MODULE = PackingSession.class.getName();

    private GenericValue userLogin = null;
    private String pickerPartyId = null;
    private String primaryOrderId = null;
    private String primaryShipGrp = null;
    private String dispatcherName = null;
    private String delegatorName = null;
    private String picklistBinId = null;
    private String facilityId = null;
    private String shipmentId = null;
    private String instructions = null;
    private String weightUomId = null;
    private String shipmentBoxTypeId = null;
    private BigDecimal additionalShippingCharge = null;
    private Map<Integer, BigDecimal> packageWeights = null;
    private List<PackingEvent> packEvents = null;
    private List<PackingSessionLine> packLines = null;
    private List<ItemDisplay> itemInfos = null;
    private int packageSeq = -1;
    private int status = 1;
    private Map<Integer, String> shipmentBoxTypes = null;

    private transient Delegator delegator = null;
    private transient LocalDispatcher dispatcher = null;

    /**
     * Instantiates a new Packing session.
     * @param dispatcher the dispatcher
     * @param userLogin  the user login
     * @param facilityId the facility id
     * @param binId      the bin id
     * @param orderId    the order id
     * @param shipGrp    the ship grp
     */
    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId, String binId, String orderId, String shipGrp) {
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();

        this.delegator = dispatcher.getDelegator();
        this.delegatorName = delegator.getDelegatorName();

        this.primaryOrderId = orderId;
        this.primaryShipGrp = shipGrp;
        this.picklistBinId = binId;
        this.userLogin = userLogin;
        this.facilityId = facilityId;
        this.packLines = new LinkedList<>();
        this.packEvents = new LinkedList<>();
        this.itemInfos = new LinkedList<>();
        this.packageSeq = 1;
        this.packageWeights = new HashMap<>();
        this.shipmentBoxTypes = new HashMap<>();
    }

    /**
     * Instantiates a new Packing session.
     * @param dispatcher the dispatcher
     * @param userLogin  the user login
     * @param facilityId the facility id
     */
    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin, String facilityId) {
        this(dispatcher, userLogin, facilityId, null, null, null);
    }

    /**
     * Instantiates a new Packing session.
     * @param dispatcher the dispatcher
     * @param userLogin  the user login
     */
    public PackingSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this(dispatcher, userLogin, null, null, null, null);
    }

    /**
     * Add or increase line.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @param quantity       the quantity
     * @param packageSeqId   the package seq id
     * @param weight         the weight
     * @param update         the update
     * @throws GeneralException the general exception
     */
    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, BigDecimal quantity,
                                  int packageSeqId, BigDecimal weight, boolean update) throws GeneralException {
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
        Map<String, Object> invLookup = new HashMap<>();
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
            BigDecimal resQty = numAvailableItems(res);

            // If reservation has enough for the quantity required
            if (resQty.compareTo(quantity) >= 0) {
                int checkCode = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, packageSeqId, update);
                this.createPackLineItem(checkCode, res, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity, weight, packageSeqId);
            }
        } else {
            // more than one reservation found
            Map<GenericValue, BigDecimal> toCreateMap = new HashMap<>();
            Iterator<GenericValue> i = reservations.iterator();
            BigDecimal qtyRemain = quantity;

            while (i.hasNext() && qtyRemain.compareTo(BigDecimal.ZERO) > 0) {
                GenericValue res = i.next();

                // Check that the inventory item product match with the current product to pack
                if (!productId.equals(res.getRelatedOne("InventoryItem", false).getString("productId"))) {
                    continue;
                }

                BigDecimal resQty = numAvailableItems(res);

                if (resQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal resPackedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId,
                            res.getString("inventoryItemId"), -1);
                    if (resPackedQty.compareTo(resQty) >= 0) {
                        continue;
                    } else if (!update) {
                        resQty = resQty.subtract(resPackedQty);
                    }

                    BigDecimal thisQty = resQty.compareTo(qtyRemain) > 0 ? qtyRemain : resQty;

                    int thisCheck = this.checkLineForAdd(res, orderId, orderItemSeqId, shipGroupSeqId, productId, thisQty, packageSeqId, update);
                    switch (thisCheck) {
                    case 2:
                        Debug.logInfo("Packing check returned '2' - new pack line will be created!", MODULE);
                        toCreateMap.put(res, thisQty);
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 1:
                        Debug.logInfo("Packing check returned '1' - existing pack line has been updated!", MODULE);
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 0:
                        Debug.logInfo("Packing check returned '0' - doing nothing.", MODULE);
                        break;
                    default:
                        Debug.logInfo("Packing check returned '> 2' or '< 0'", MODULE);
                        break;
                    }
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

    private static BigDecimal numAvailableItems(GenericValue res) {
        // In simple situations, the reserved quantity will match the quantity from the order item.
        // If there is a back order, quantity from order may exceed quantity currently reserved and on hand.
        // resQty should never exceed the quantity from the order item, because that quantity was the quantity reserved in the first place.
        BigDecimal notAvailable = res.getBigDecimal("quantityNotAvailable");
        BigDecimal resQty = res.getBigDecimal("quantity");

        if (notAvailable != null) {
            resQty = resQty.subtract(notAvailable);
        }

        return resQty;
    }

    /**
     * Add or increase line.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param quantity       the quantity
     * @param packageSeqId   the package seq id
     * @throws GeneralException the general exception
     */
    public void addOrIncreaseLine(String orderId, String orderItemSeqId, String shipGroupSeqId, BigDecimal quantity, int packageSeqId)
            throws GeneralException {
        this.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, null, quantity, packageSeqId, BigDecimal.ZERO, false);
    }

    /**
     * Add or increase line.
     * @param productId    the product id
     * @param quantity     the quantity
     * @param packageSeqId the package seq id
     * @throws GeneralException the general exception
     */
    public void addOrIncreaseLine(String productId, BigDecimal quantity, int packageSeqId) throws GeneralException {
        this.addOrIncreaseLine(null, null, null, productId, quantity, packageSeqId, BigDecimal.ZERO, false);
    }

    /**
     * Find line packing session line.
     * @param orderId         the order id
     * @param orderItemSeqId  the order item seq id
     * @param shipGroupSeqId  the ship group seq id
     * @param productId       the product id
     * @param inventoryItemId the inventory item id
     * @param packageSeq      the package seq
     * @return the packing session line
     */
    public PackingSessionLine findLine(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId,
                                       int packageSeq) {
        for (PackingSessionLine line: this.getLines()) {
            if (orderId.equals(line.getOrderId())
                    && orderItemSeqId.equals(line.getOrderItemSeqId())
                    && shipGroupSeqId.equals(line.getShipGroupSeqId())
                    && productId.equals(line.getProductId())
                    && inventoryItemId.equals(line.getInventoryItemId())
                    && packageSeq == line.getPackageSeq()) {
                return line;
            }
        }
        return null;
    }

    /**
     * Create pack line item.
     * @param checkCode      the check code
     * @param res            the res
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @param quantity       the quantity
     * @param weight         the weight
     * @param packageSeqId   the package seq id
     * @throws GeneralException the general exception
     */
    protected void createPackLineItem(int checkCode, GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId,
                                      String productId, BigDecimal quantity, BigDecimal weight, int packageSeqId) throws GeneralException {
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
        default:
            throw new GeneralException("value of checkCode different than expected");

        }

        // Add the line weight to the package weight
        if (weight.compareTo(BigDecimal.ZERO) > 0) this.addToPackageWeight(packageSeqId, weight);

        // update the package sequence
        if (packageSeqId > packageSeq) {
            this.packageSeq = packageSeqId;
        }
    }

    /**
     * Find order item seq id string.
     * @param productId      the product id
     * @param orderId        the order id
     * @param shipGroupSeqId the ship group seq id
     * @param quantity       the quantity
     * @return the string
     * @throws GeneralException the general exception
     */
    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, BigDecimal quantity) throws GeneralException {
        Map<String, Object> lookupMap = new HashMap<>();
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
                Map<String, Object> invLookup = new HashMap<>();
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

    /**
     * Check line for add int.
     * @param res            the res
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @param quantity       the quantity
     * @param packageSeqId   the package seq id
     * @param update         the update
     * @return the int
     */
    protected int checkLineForAdd(GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId,
                                  BigDecimal quantity, int packageSeqId, boolean update) {
        // check to see if the reservation can hold the requested quantity amount
        String invItemId = res.getString("inventoryItemId");
        BigDecimal resQty = res.getBigDecimal("quantity");

        PackingSessionLine line = this.findLine(orderId, orderItemSeqId, shipGroupSeqId, productId, invItemId, packageSeqId);
        BigDecimal packedQty = this.getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId);

        Debug.logInfo("Packed quantity [" + packedQty + "] + [" + quantity + "]", MODULE);

        if (line == null) {
            Debug.logInfo("No current line found testing [" + invItemId + "] R: " + resQty + " / Q: " + quantity, MODULE);
            if (resQty.compareTo(quantity) < 0) {
                return 0;
            } else {
                return 2;
            }
        } else {
            BigDecimal newQty = update ? quantity : (line.getQuantity().add(quantity));
            Debug.logInfo("Existing line found testing [" + invItemId + "] R: " + resQty + " / Q: " + newQty, MODULE);
            if (resQty.compareTo(newQty) < 0) {
                return 0;
            } else {
                line.setQuantity(newQty);
                return 1;
            }
        }
    }

    /**
     * Add item info.
     * @param infos the infos
     */
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

    /**
     * Gets item infos.
     * @return the item infos
     */
    public List<ItemDisplay> getItemInfos() {
        return itemInfos;
    }

    /**
     * <p>Delivers all the packing lines grouped by package.</p>
     * Output map:
     * <ul>
     * <li>packageMap - a Map of type {@code Map<Integer, List<PackingSessionLine>>}
     * that maps package sequence ids to the lines that belong in
     * that package</li>
     * <li>sortedKeys - a List of type List&lt;Integer&gt; with the sorted package
     * sequence numbers to index the packageMap</li>
     * </ul>
     * @return result Map with packageMap and sortedKeys
     */
    public Map<Object, Object> getPackingSessionLinesByPackage() {
        Map<Integer, List<PackingSessionLine>> packageMap = new HashMap<>();
        for (PackingSessionLine line : packLines) {
            int pSeq = line.getPackageSeq();
            List<PackingSessionLine> packageLineList = packageMap.get(pSeq);
            if (packageLineList == null) {
                packageLineList = new LinkedList<>();
                packageMap.put(pSeq, packageLineList);
            }
            packageLineList.add(line);
        }
        Object[] keys = packageMap.keySet().toArray();
        java.util.Arrays.sort(keys);
        List<Object> sortedKeys = new LinkedList<>();
        for (Object key : keys) {
            sortedKeys.add(key);
        }
        Map<Object, Object> result = new HashMap<>();
        result.put("packageMap", packageMap);
        result.put("sortedKeys", sortedKeys);
        return result;
    }

    /**
     * Clear item infos.
     */
    public void clearItemInfos() {
        itemInfos.clear();
    }

    /**
     * Gets shipment id.
     * @return the shipment id
     */
    public String getShipmentId() {
        return this.shipmentId;
    }

    /**
     * Gets lines.
     * @return the lines
     */
    public List<PackingSessionLine> getLines() {
        return this.packLines;
    }

    /**
     * Next package seq int.
     * @return the int
     */
    public int nextPackageSeq() {
        return ++packageSeq;
    }

    /**
     * Gets current package seq.
     * @return the current package seq
     */
    public int getCurrentPackageSeq() {
        return packageSeq;
    }

    /**
     * Gets packed quantity.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId, null, -1);
    }

    /**
     * Gets packed quantity.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @param packageSeq     the package seq
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, int packageSeq) {
        return getPackedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId, null, packageSeq);
    }

    /**
     * Gets packed quantity.
     * @param orderId         the order id
     * @param orderItemSeqId  the order item seq id
     * @param shipGroupSeqId  the ship group seq id
     * @param productId       the product id
     * @param inventoryItemId the inventory item id
     * @param packageSeq      the package seq
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId,
                                        int packageSeq) {
        BigDecimal total = BigDecimal.ZERO;
        for (PackingSessionLine line: this.getLines()) {
            if (orderId.equals(line.getOrderId()) && orderItemSeqId.equals(line.getOrderItemSeqId())
                    && shipGroupSeqId.equals(line.getShipGroupSeqId()) && productId.equals(line.getProductId())) {
                if (inventoryItemId == null || inventoryItemId.equals(line.getInventoryItemId())) {
                    if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                        total = total.add(line.getQuantity());
                    }
                }
            }
        }
        return total;
    }

    /**
     * Gets packed quantity.
     * @param productId  the product id
     * @param packageSeq the package seq
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(String productId, int packageSeq) {
        if (productId != null) {
            try {
                productId = ProductWorker.findProductId(this.getDelegator(), productId);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
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

    /**
     * Gets packed quantity.
     * @param packageSeq the package seq
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(int packageSeq) {
        BigDecimal total = BigDecimal.ZERO;
        for (PackingSessionLine line: this.getLines()) {
            if (packageSeq == -1 || packageSeq == line.getPackageSeq()) {
                total = total.add(line.getQuantity());
            }
        }
        return total;
    }

    /**
     * Gets packed quantity.
     * @param productId the product id
     * @return the packed quantity
     */
    public BigDecimal getPackedQuantity(String productId) {
        return getPackedQuantity(productId, -1);
    }

    /**
     * Gets current reserved quantity.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId      the product id
     * @return the current reserved quantity
     */
    public BigDecimal getCurrentReservedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId) {
        BigDecimal reserved = BigDecimal.ONE.negate();
        try {
            GenericValue res = EntityUtil.getFirst(this.getDelegator().findByAnd("OrderItemAndShipGrpInvResAndItemSum",
                    UtilMisc.toMap("orderId", orderId,
                    "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId, "inventoryProductId", productId), null, false));
            reserved = res.getBigDecimal("totQuantityAvailable");
            if (reserved == null) {
                reserved = BigDecimal.ONE.negate();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return reserved;
    }

    /**
     * Gets current shipped quantity.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @return the current shipped quantity
     */
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

    /**
     * Gets current shipment ids.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @return the current shipment ids
     */
    public List<String> getCurrentShipmentIds(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        Set<String> shipmentIds = new HashSet<>();
        List<GenericValue> issues = this.getItemIssuances(orderId, orderItemSeqId, shipGroupSeqId);

        if (issues != null) {
            for (GenericValue v: issues) {
                shipmentIds.add(v.getString("shipmentId"));
            }
        }

        List<String> retList = new LinkedList<>();
        retList.addAll(shipmentIds);
        return retList;
    }

    /**
     * Gets current shipment ids.
     * @param orderId        the order id
     * @param shipGroupSeqId the ship group seq id
     * @return the current shipment ids
     */
    public List<String> getCurrentShipmentIds(String orderId, String shipGroupSeqId) {
        return this.getCurrentShipmentIds(orderId, null, shipGroupSeqId);
    }

    /**
     * Register event.
     * @param event the event
     */
    public void registerEvent(PackingEvent event) {
        this.packEvents.add(event);
        this.runEvents(PackingEvent.EVENT_CODE_EREG);
    }

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    /**
     * Gets user login.
     * @return the user login
     */
    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    /**
     * Gets status.
     * @return the status
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * Gets facility id.
     * @return the facility id
     */
    public String getFacilityId() {
        return this.facilityId;
    }

    /**
     * Sets facility id.
     * @param facilityId the facility id
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Gets primary order id.
     * @return the primary order id
     */
    public String getPrimaryOrderId() {
        return this.primaryOrderId;
    }

    /**
     * Sets primary order id.
     * @param orderId the order id
     */
    public void setPrimaryOrderId(String orderId) {
        this.primaryOrderId = orderId;
    }

    /**
     * Gets primary ship group seq id.
     * @return the primary ship group seq id
     */
    public String getPrimaryShipGroupSeqId() {
        return this.primaryShipGrp;
    }

    /**
     * Sets primary ship group seq id.
     * @param shipGroupSeqId the ship group seq id
     */
    public void setPrimaryShipGroupSeqId(String shipGroupSeqId) {
        this.primaryShipGrp = shipGroupSeqId;
    }

    /**
     * Sets picklist bin id.
     * @param binId the bin id
     */
    public void setPicklistBinId(String binId) {
        this.picklistBinId = binId;
    }

    /**
     * Gets picklist bin id.
     * @return the picklist bin id
     */
    public String getPicklistBinId() {
        return this.picklistBinId;
    }

    /**
     * Gets handling instructions.
     * @return the handling instructions
     */
    public String getHandlingInstructions() {
        return this.instructions;
    }

    /**
     * Sets handling instructions.
     * @param instructions the instructions
     */
    public void setHandlingInstructions(String instructions) {
        this.instructions = instructions;
    }

    /**
     * Sets picker party id.
     * @param partyId the party id
     */
    public void setPickerPartyId(String partyId) {
        this.pickerPartyId = partyId;
    }

    /**
     * Gets picker party id.
     * @return the picker party id
     */
    public String getPickerPartyId() {
        return this.pickerPartyId;
    }

    /**
     * Clear last package int.
     * @return the int
     */
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

    /**
     * Clear line.
     * @param line the line
     */
    public void clearLine(PackingSessionLine line) {
        this.packLines.remove(line);
        BigDecimal packageWeight = this.packageWeights.get(line.getPackageSeq());
        if (packageWeight != null) {
            packageWeight = packageWeight.subtract(line.getWeight());
            if (packageWeight.compareTo(BigDecimal.ZERO) < 0) {
                packageWeight = BigDecimal.ZERO;
            }
            this.packageWeights.put(line.getPackageSeq(), packageWeight);
        }
        if (line.getPackageSeq() == packageSeq && packageSeq > 1) {
            packageSeq--;
        }
    }

    /**
     * Clear all lines.
     */
    public void clearAllLines() {
        this.packLines.clear();
        this.packageWeights.clear();
        this.shipmentBoxTypes.clear();
        this.packageSeq = 1;
    }

    /**
     * Clear.
     */
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

    /**
     * Complete string.
     * @param force the force
     * @return the string
     * @throws GeneralException the general exception
     */
    public String complete(boolean force) throws GeneralException {
        // check to see if there is anything to process
        if (this.getLines().isEmpty()) {
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
        // set picklist to picked
        this.setPicklistToPicked();
        // run the complete events
        this.runEvents(PackingEvent.EVENT_CODE_COMPLETE);

        return this.shipmentId;
    }

    /**
     * Check reservations.
     * @param ignore the ignore
     * @throws GeneralException the general exception
     */
    protected void checkReservations(boolean ignore) throws GeneralException {
        List<String> errors = new LinkedList<>();
        for (PackingSessionLine line: this.getLines()) {
            BigDecimal reservedQty = this.getCurrentReservedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(),
                    line.getProductId());
            BigDecimal packedQty = this.getPackedQuantity(line.getOrderId(), line.getOrderItemSeqId(), line.getShipGroupSeqId(), line.getProductId());

            if (packedQty.compareTo(reservedQty) != 0) {
                errors.add("Packed amount does not match reserved amount for item (" + line.getProductId() + ") [" + packedQty + " / "
                        + reservedQty + "]");
            }
        }

        if (!errors.isEmpty()) {
            if (!ignore) {
                throw new GeneralException("Attempt to pack order failed.", errors);
            } else {
                Debug.logWarning("Packing warnings: " + errors, MODULE);
            }
        }
    }

    /**
     * Check empty lines.
     * @throws GeneralException the general exception
     */
    protected void checkEmptyLines() throws GeneralException {
        List<PackingSessionLine> lines = new LinkedList<>();
        lines.addAll(this.getLines());
        for (PackingSessionLine l: lines) {
            if (l.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                this.packLines.remove(l);
            }
        }
    }

    /**
     * Run events.
     * @param eventCode the event code
     */
    protected void runEvents(int eventCode) {
        if (!this.packEvents.isEmpty()) {
            for (PackingEvent event: this.packEvents) {
                event.runEvent(this, eventCode);
            }
        }
    }

    /**
     * Gets item issuances.
     * @param orderId        the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @return the item issuances
     */
    protected List<GenericValue> getItemIssuances(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        List<GenericValue> issues = null;
        if (orderId == null) {
            throw new IllegalArgumentException("Value for orderId is  null");
        }

        Map<String, Object> lookupMap = new HashMap<>();
        lookupMap.put("orderId", orderId);
        if (UtilValidate.isNotEmpty(orderItemSeqId)) {
            lookupMap.put("orderItemSeqId", orderItemSeqId);
        }
        if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
            lookupMap.put("shipGroupSeqId", shipGroupSeqId);
        }
        try {
            issues = this.getDelegator().findByAnd("ItemIssuance", lookupMap, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        return issues;
    }

    /**
     * Create shipment.
     * @throws GeneralException the general exception
     */
    protected void createShipment() throws GeneralException {
        // first create the shipment
        Delegator delegator = this.getDelegator();
        Map<String, Object> newShipment = new HashMap<>();
        newShipment.put("originFacilityId", this.facilityId);
        newShipment.put("primaryShipGroupSeqId", primaryShipGrp);
        newShipment.put("primaryOrderId", primaryOrderId);
        newShipment.put("shipmentTypeId", "OUTGOING_SHIPMENT");
        newShipment.put("statusId", "SHIPMENT_INPUT");
        newShipment.put("handlingInstructions", instructions);
        newShipment.put("picklistBinId", picklistBinId);
        newShipment.put("additionalShippingCharge", additionalShippingCharge);
        newShipment.put("userLogin", userLogin);
        GenericValue orderRoleShipTo = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId, "roleTypeId",
                "SHIP_TO_CUSTOMER").queryFirst();
        if (UtilValidate.isNotEmpty(orderRoleShipTo)) {
            newShipment.put("partyIdTo", orderRoleShipTo.getString("partyId"));
        }
        String partyIdFrom = null;
        if (primaryOrderId != null) {
            GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", primaryOrderId,
                    "shipGroupSeqId", primaryShipGrp).queryFirst();
            if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("vendorPartyId"))) {
                partyIdFrom = orderItemShipGroup.getString("vendorPartyId");
            } else if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("facilityId"))) {
                GenericValue facility = EntityQuery.use(delegator).from("Facility").where("facilityId",
                        orderItemShipGroup.getString("facilityId")).queryOne();
                if (UtilValidate.isNotEmpty(facility.getString("ownerPartyId"))) {
                    partyIdFrom = facility.getString("ownerPartyId");
                }
            }
            if (UtilValidate.isEmpty(partyIdFrom)) {
                GenericValue orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId,
                        "roleTypeId", "SHIP_FROM_VENDOR").queryFirst();
                if (UtilValidate.isNotEmpty(orderRoleShipFrom)) {
                    partyIdFrom = orderRoleShipFrom.getString("partyId");
                } else {
                    orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", primaryOrderId, "roleTypeId",
                            "BILL_FROM_VENDOR").queryFirst();
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
        Debug.logInfo("Creating new shipment with context: " + newShipment, MODULE);
        Map<String, Object> newShipResp = this.getDispatcher().runSync("createShipment", newShipment);

        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        this.shipmentId = (String) newShipResp.get("shipmentId");
    }

    /**
     * Issue items to shipment.
     * @throws GeneralException the general exception
     */
    protected void issueItemsToShipment() throws GeneralException {
        List<PackingSessionLine> processedLines = new LinkedList<>();
        for (PackingSessionLine line: this.getLines()) {
            if (this.checkLine(processedLines, line)) {
                BigDecimal totalPacked = this.getPackedQuantity(line.getOrderId(), line.getOrderItemSeqId(),
                        line.getShipGroupSeqId(), line.getProductId(), line.getInventoryItemId(), -1);

                line.issueItemToShipment(shipmentId, picklistBinId, userLogin, totalPacked, getDispatcher());
                processedLines.add(line);
            }
        }
    }

    /**
     * Check line boolean.
     * @param processedLines the processed lines
     * @param line           the line
     * @return the boolean
     */
    protected boolean checkLine(List<PackingSessionLine> processedLines, PackingSessionLine line) {
        for (PackingSessionLine l: processedLines) {
            if (line.isSameItem(l)) {
                line.setShipmentItemSeqId(l.getShipmentItemSeqId());
                return false;
            }
        }

        return true;
    }

    /**
     * Create packages.
     * @throws GeneralException the general exception
     */
    protected void createPackages() throws GeneralException {
        for (int i = 0; i < packageSeq; i++) {
            String shipmentPackageSeqId = UtilFormatOut.formatPaddedNumber(i + 1, 5);

            Map<String, Object> pkgCtx = new HashMap<>();
            pkgCtx.put("shipmentId", shipmentId);
            pkgCtx.put("shipmentPackageSeqId", shipmentPackageSeqId);
            pkgCtx.put("shipmentBoxTypeId", getShipmentBoxType(i + 1));
            pkgCtx.put("weight", getPackageWeight(i + 1));
            pkgCtx.put("weightUomId", getWeightUomId());
            pkgCtx.put("userLogin", userLogin);
            Map<String, Object> newPkgResp = this.getDispatcher().runSync("createShipmentPackage", pkgCtx);

            if (ServiceUtil.isError(newPkgResp)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(newPkgResp));
            }
        }
    }

    /**
     * Apply items to packages.
     * @throws GeneralException the general exception
     */
    protected void applyItemsToPackages() throws GeneralException {
        for (PackingSessionLine line: this.getLines()) {
            line.applyLineToPackage(shipmentId, userLogin, getDispatcher());
        }
    }

    /**
     * Update shipment route segments.
     * @throws GeneralException the general exception
     */
    protected void updateShipmentRouteSegments() throws GeneralException {
        BigDecimal shipmentWeight = getTotalWeight();
        if (shipmentWeight.compareTo(BigDecimal.ZERO) <= 0) return;
        List<GenericValue> shipmentRouteSegments = getDelegator().findByAnd("ShipmentRouteSegment", UtilMisc.toMap("shipmentId",
                this.getShipmentId()), null, false);
        if (!UtilValidate.isEmpty(shipmentRouteSegments)) {
            for (GenericValue shipmentRouteSegment: shipmentRouteSegments) {
                shipmentRouteSegment.set("billingWeight", shipmentWeight);
                shipmentRouteSegment.set("billingWeightUomId", getWeightUomId());
            }
            getDelegator().storeAll(shipmentRouteSegments);
        }
    }

    /**
     * Sets shipment to packed.
     * @throws GeneralException the general exception
     */
    protected void setShipmentToPacked() throws GeneralException {
        Map<String, Object> packedCtx = UtilMisc.toMap("shipmentId", shipmentId, "statusId", "SHIPMENT_PACKED", "userLogin", userLogin);
        Map<String, Object> packedResp = this.getDispatcher().runSync("updateShipment", packedCtx);
        if (packedResp != null && ServiceUtil.isError(packedResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(packedResp));
        }
    }

    /**
     * Sets picklist to picked.
     * @throws GeneralException the general exception
     */
    protected void setPicklistToPicked() throws GeneralException {
        Delegator delegator = this.getDelegator();
        if (picklistBinId != null) {
            GenericValue picklist = EntityQuery.use(delegator).from("PicklistAndBin").where("picklistBinId", picklistBinId).queryFirst();
            if (picklist == null) {
                if (!"PICKLIST_PICKED".equals(picklist.getString("statusId")) && !"PICKLIST_COMPLETED".equals(picklist.getString("statusId"))
                        && !"PICKLIST_CANCELLED".equals(picklist.getString("statusId"))) {
                    Map<String, Object> serviceResult = this.getDispatcher().runSync("updatePicklist", UtilMisc.toMap("picklistId",
                            picklist.getString("picklistId"), "statusId", "PICKLIST_PICKED", "userLogin", userLogin));
                    if (!ServiceUtil.isSuccess(serviceResult)) {
                        throw new GeneralException(ServiceUtil.getErrorMessage(serviceResult));
                    }
                }
            }
        } else {
            List<GenericValue> picklistBins = EntityQuery.use(delegator).from("PicklistAndBin").where("primaryOrderId", primaryOrderId).queryList();
            if (UtilValidate.isNotEmpty(picklistBins)) {
                for (GenericValue picklistBin : picklistBins) {
                    if (!"PICKLIST_PICKED".equals(picklistBin.getString("statusId"))
                            && !"PICKLIST_COMPLETED".equals(picklistBin.getString("statusId"))
                            && !"PICKLIST_CANCELLED".equals(picklistBin.getString("statusId"))) {
                        Map<String, Object> serviceResult = this.getDispatcher().runSync("updatePicklist", UtilMisc.toMap("picklistId",
                                picklistBin.getString("picklistId"), "statusId", "PICKLIST_PICKED", "userLogin", userLogin));
                        if (!ServiceUtil.isSuccess(serviceResult)) {
                            throw new GeneralException(ServiceUtil.getErrorMessage(serviceResult));
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets picker on picklist.
     * @throws GeneralException the general exception
     */
    protected void setPickerOnPicklist() throws GeneralException {
        if (picklistBinId != null) {
            // first find the picklist id
            GenericValue bin = this.getDelegator().findOne("PicklistBin", UtilMisc.toMap("picklistBinId", picklistBinId), false);
            if (bin != null) {
                Map<String, Object> ctx = new HashMap<>();
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

    /**
     * Gets additional shipping charge.
     * @return the additional shipping charge
     */
    public BigDecimal getAdditionalShippingCharge() {
        return additionalShippingCharge;
    }

    /**
     * Sets additional shipping charge.
     * @param additionalShippingCharge the additional shipping charge
     */
    public void setAdditionalShippingCharge(BigDecimal additionalShippingCharge) {
        this.additionalShippingCharge = additionalShippingCharge;
    }

    /**
     * Gets total weight.
     * @return the total weight
     */
    public BigDecimal getTotalWeight() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < packageSeq; i++) {
            BigDecimal packageWeight = getPackageWeight(i);
            if (!UtilValidate.isEmpty(packageWeight)) {
                total = total.add(packageWeight);
            }
        }
        return total;
    }

    /**
     * Gets shipment cost estimate.
     * @param orderItemShipGroup the order item ship group
     * @param productStoreId     the product store id
     * @param shippableItemInfo  the shippable item info
     * @param shippableTotal     the shippable total
     * @param shippableWeight    the shippable weight
     * @param shippableQuantity  the shippable quantity
     * @return the shipment cost estimate
     */
    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId, List<GenericValue> shippableItemInfo,
                                              BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       productStoreId, shippableItemInfo, shippableTotal, shippableWeight, shippableQuantity);
    }

    /**
     * Gets shipment cost estimate.
     * @param orderItemShipGroup the order item ship group
     * @param productStoreId     the product store id
     * @return the shipment cost estimate
     */
    public BigDecimal getShipmentCostEstimate(GenericValue orderItemShipGroup, String productStoreId) {
        return getShipmentCostEstimate(orderItemShipGroup.getString("contactMechId"), orderItemShipGroup.getString("shipmentMethodTypeId"),
                                       orderItemShipGroup.getString("carrierPartyId"), orderItemShipGroup.getString("carrierRoleTypeId"),
                                       productStoreId, null, null, null, null);
    }

    /**
     * Gets shipment cost estimate.
     * @param shippingContactMechId the shipping contact mech id
     * @param shipmentMethodTypeId  the shipment method type id
     * @param carrierPartyId        the carrier party id
     * @param carrierRoleTypeId     the carrier role type id
     * @param productStoreId        the product store id
     * @param shippableItemInfo     the shippable item info
     * @param shippableTotal        the shippable total
     * @param shippableWeight       the shippable weight
     * @param shippableQuantity     the shippable quantity
     * @return the shipment cost estimate
     */
    public BigDecimal getShipmentCostEstimate(String shippingContactMechId, String shipmentMethodTypeId, String carrierPartyId,
                                              String carrierRoleTypeId, String productStoreId, List<GenericValue> shippableItemInfo,
                                              BigDecimal shippableTotal, BigDecimal shippableWeight, BigDecimal shippableQuantity) {

        BigDecimal shipmentCostEstimate = null;
        Map<String, Object> serviceResult = null;
        try {
            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.put("shippingContactMechId", shippingContactMechId);
            serviceContext.put("shipmentMethodTypeId", shipmentMethodTypeId);
            serviceContext.put("carrierPartyId", carrierPartyId);
            serviceContext.put("carrierRoleTypeId", carrierRoleTypeId);
            serviceContext.put("productStoreId", productStoreId);

            if (UtilValidate.isEmpty(shippableItemInfo)) {
                shippableItemInfo = new LinkedList<>();
                for (PackingSessionLine line: getLines()) {
                    List<GenericValue> oiasgas = getDelegator().findByAnd("OrderItemAndShipGroupAssoc", UtilMisc.toMap("orderId",
                            line.getOrderId(), "orderItemSeqId", line.getOrderItemSeqId(), "shipGroupSeqId", line.getShipGroupSeqId()), null, false);
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

            if (ServiceUtil.isError(serviceResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(serviceResult), MODULE);
                return shipmentCostEstimate;
            }

        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
        }

        if (UtilValidate.isNotEmpty(serviceResult)) {
            shipmentCostEstimate = (BigDecimal) serviceResult.get("shippingEstimateAmount");
        }

        return shipmentCostEstimate;

    }

    /**
     * Gets weight uom id.
     * @return the weight uom id
     */
    public String getWeightUomId() {
        return weightUomId;
    }

    /**
     * Sets weight uom id.
     * @param weightUomId the weight uom id
     */
    public void setWeightUomId(String weightUomId) {
        this.weightUomId = weightUomId;
    }

    /**
     * Sets shipment box type id.
     * @param shipmentBoxTypeId the shipment box type id
     */
    public void setShipmentBoxTypeId(String shipmentBoxTypeId) {
        this.shipmentBoxTypeId = shipmentBoxTypeId;
    }

    /**
     * Gets package seq ids.
     * @return the package seq ids
     */
    public List<Integer> getPackageSeqIds() {
        Set<Integer> packageSeqIds = new TreeSet<>();
        if (!UtilValidate.isEmpty(this.getLines())) {
            for (PackingSessionLine line: this.getLines()) {
                packageSeqIds.add(line.getPackageSeq());
            }
        }
        return UtilMisc.makeListWritable(packageSeqIds);
    }

    /**
     * Sets package weight.
     * @param packageSeqId  the package seq id
     * @param packageWeight the package weight
     */
    public void setPackageWeight(int packageSeqId, BigDecimal packageWeight) {
        if (UtilValidate.isEmpty(packageWeight)) {
            packageWeights.remove(packageSeqId);
        } else {
            packageWeights.put(packageSeqId, packageWeight);
        }
    }

    /**
     * Gets package weight.
     * @param packageSeqId the package seq id
     * @return the package weight
     */
    public BigDecimal getPackageWeight(int packageSeqId) {
        if (this.packageWeights == null) return null;
        BigDecimal packageWeight = null;
        Object p = packageWeights.get(packageSeqId);
        if (p != null) {
            packageWeight = (BigDecimal) p;
        }
        return packageWeight;
    }

    /**
     * Add to package weight.
     * @param packageSeqId the package seq id
     * @param weight       the weight
     */
    public void addToPackageWeight(int packageSeqId, BigDecimal weight) {
        if (UtilValidate.isEmpty(weight)) return;
        BigDecimal packageWeight = getPackageWeight(packageSeqId);
        BigDecimal newPackageWeight = UtilValidate.isEmpty(packageWeight) ? weight : weight.add(packageWeight);
        setPackageWeight(packageSeqId, newPackageWeight);
    }

    /**
     * Sets shipment box type.
     * @param packageSeqId    the package seq id
     * @param shipmentBoxType the shipment box type
     */
// These methods (setShipmentBoxType and getShipmentBoxType) are added so that each package will have different box type.
    public void setShipmentBoxType(int packageSeqId, String shipmentBoxType) {
        if (UtilValidate.isEmpty(shipmentBoxType)) {
            shipmentBoxTypes.remove(packageSeqId);
        } else {
            shipmentBoxTypes.put(packageSeqId, shipmentBoxType);
        }
    }

    /**
     * Gets shipment box type.
     * @param packageSeqId the package seq id
     * @return the shipment box type
     */
    public String getShipmentBoxType(int packageSeqId) {
        if (this.shipmentBoxTypes == null) return null;
        String shipmentBoxType = null;
        Object p = shipmentBoxTypes.get(packageSeqId);
        if (p != null) {
            shipmentBoxType = (String) p;
        }
        return shipmentBoxType;
    }

    /**
     * The type Item display.
     */
    class ItemDisplay extends AbstractMap<Object, Object> {

        private GenericValue orderItem;
        private BigDecimal quantity;
        private String productId;

        /**
         * Instantiates a new Item display.
         * @param v the v
         */
        ItemDisplay(GenericValue v) {
            if ("PicklistItem".equals(v.getEntityName())) {
                quantity = v.getBigDecimal("quantity").setScale(2, RoundingMode.HALF_UP);
                try {
                    orderItem = v.getRelatedOne("OrderItem", false);
                    productId = v.getRelatedOne("InventoryItem", false).getString("productId");
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            } else {
                // this is an OrderItemAndShipGrpInvResAndItemSum
                orderItem = v;
                productId = v.getString("inventoryProductId");
                quantity = v.getBigDecimal("totQuantityReserved").setScale(2, RoundingMode.HALF_UP);
            }
            Debug.logInfo("created item display object quantity: " + quantity + " (" + productId + ")", MODULE);
        }

        /**
         * Gets order item.
         * @return the order item
         */
        public GenericValue getOrderItem() {
            return orderItem;
        }

        /**
         * Gets quantity.
         * @return the quantity
         */
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
                return (d.productId.equals(productId)
                        && d.getOrderItem().getString("orderItemSeqId").equals(orderItem.getString("orderItemSeqId"))
                        && sameOrderItemProduct);
            } else {
                return false;
            }
        }
    }
}
