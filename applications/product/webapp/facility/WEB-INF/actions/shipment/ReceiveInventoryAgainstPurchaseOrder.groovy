/*
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
 */

import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*
import org.ofbiz.service.ServiceUtil
import org.ofbiz.base.util.*

shipmentId = request.getParameter("shipmentId");
orderId = request.getParameter("purchaseOrderId");
shipGroupSeqId = request.getParameter("shipGroupSeqId");
context.shipmentId = shipmentId;
context.shipGroupSeqId = shipGroupSeqId;

// Retrieve the map resident in session which stores order item quantities to receive
itemQuantitiesToReceive = session.getAttribute("purchaseOrderItemQuantitiesToReceive");
if (itemQuantitiesToReceive) {
    sessionShipmentId = itemQuantitiesToReceive._shipmentId;
    sessionOrderId = itemQuantitiesToReceive._orderId;
    if ((sessionShipmentId && !sessionShipmentId.equals(shipmentId)) ||
        ((sessionOrderId && !sessionOrderId.equals(orderId)))        ||
         "Y".equals(request.getParameter("clearAll"))) {

             // Clear the map if the shipmentId or orderId are different than the current ones, or
             // if the clearAll parameter is present
             itemQuantitiesToReceive.clear();
    }
}

shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
context.shipment = shipment;
if (!shipment) {
    return;
}

isPurchaseShipment = "PURCHASE_SHIPMENT".equals(shipment.shipmentTypeId);
context.isPurchaseShipment = isPurchaseShipment;
if (!isPurchaseShipment) {
    return;
}

facility = shipment.getRelatedOne("DestinationFacility", false);
context.facility = facility;
context.facilityId = shipment.destinationFacilityId;
context.now = UtilDateTime.nowTimestamp();

if (!orderId) {
    orderId = shipment.primaryOrderId;
}
if (!shipGroupSeqId) {
    shipGroupSeqId = shipment.primaryShipGroupSeqId;
}
context.orderId = orderId;

if (!orderId) {
    return;
}

orderHeader = from("OrderHeader").where("orderId", orderId).queryOne();
context.orderHeader = orderHeader;
if (!orderHeader) {
    return;
}

isPurchaseOrder = "PURCHASE_ORDER".equals(orderHeader.orderTypeId);
context.isPurchaseOrder = isPurchaseOrder;
if (!isPurchaseOrder) {
    return;
}

// Get the base currency from the facility owner, for currency conversions
baseCurrencyUomId = null;
if (facility) {
    owner = facility.getRelatedOne("OwnerParty", false);
    if (owner) {
        result = runService('getPartyAccountingPreferences', [organizationPartyId : owner.partyId, userLogin : request.getAttribute("userLogin")]);
        if (!ServiceUtil.isError(result) && result.partyAccountingPreference) {
            ownerAcctgPref = result.partyAccountingPreference;
        }
    }
    if (ownerAcctgPref) {
        baseCurrencyUomId = ownerAcctgPref.baseCurrencyUomId;
    }
}

inventoryItemTypes = from("InventoryItemType").queryList();
context.inventoryItemTypes = inventoryItemTypes;

// Populate the tracking map with shipment and order IDs
if (!itemQuantitiesToReceive) {
    itemQuantitiesToReceive = [_shipmentId : shipmentId, _orderId : orderId];
}

oiasgaLimitMap = null;
if (shipGroupSeqId) {
    oiasgaLimitMap = [shipGroupSeqId : shipGroupSeqId];
}

orderItemDatas = [:] as TreeMap;
totalAvailableToReceive = 0;

// Populate the order item data for the FTL
orderItems = orderHeader.getRelated("OrderItemAndShipGroupAssoc", oiasgaLimitMap, ['shipGroupSeqId', 'orderItemSeqId'], false);
orderItems.each { orderItemAndShipGroupAssoc ->
    product = orderItemAndShipGroupAssoc.getRelatedOne("Product", false);

    // Get the order item, since the orderItemAndShipGroupAssoc's quantity field is manipulated in some cases
    orderItem = from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemAndShipGroupAssoc.orderItemSeqId).queryOne();
    orderItemData = [:];

    // Get the item's ordered quantity
    totalOrdered = 0;
    ordered = orderItem.getDouble("quantity");
    if (ordered) {
        totalOrdered += ordered.doubleValue();
    }
    cancelled = orderItem.getDouble("cancelQuantity");
    if (cancelled) {
        totalOrdered -= cancelled.doubleValue();
    }

    // Get the item quantity received from all shipments via the ShipmentReceipt entity
    totalReceived = 0.0;
    receipts = from("ShipmentReceipt").where("orderId", orderId, "orderItemSeqId", orderItem.orderItemSeqId).queryList();
    fulfilledReservations = [] as ArrayList;
    if (receipts) {
        receipts.each { rec ->
            accepted = rec.getDouble("quantityAccepted");
            rejected = rec.getDouble("quantityRejected");
            if (accepted) {
                totalReceived += accepted.doubleValue();
            }
            if (rejected) {
                totalReceived += rejected.doubleValue();
            }
            // Get the reservations related to this receipt
            oisgirs = from("OrderItemShipGrpInvRes").where("inventoryItemId", rec.inventoryItemId).queryList();
            if (oisgirs) {
                fulfilledReservations.addAll(oisgirs);
            }
        }
    }
    orderItemData.fulfilledReservations = fulfilledReservations;

    // Update the unit cost with the converted value, if any
    if (baseCurrencyUomId && orderHeader.currencyUom) {
        if (product) {
            result = runService('convertUom', [uomId : orderHeader.currencyUom, uomIdTo : baseCurrencyUomId, originalValue : orderItem.unitPrice]);
            if (!ServiceUtil.isError(result)) {
                orderItem.unitPrice = result.convertedValue;
            }
        }
    }

    // Retrieve the backordered quantity
    // TODO: limit to a facility? The shipment destination facility is not necessarily the same facility as the inventory
    conditions = [EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.productId),
                  EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.LESS_THAN, BigDecimal.ZERO)];
    negativeInventoryItems = from("InventoryItem").where(conditions).queryList();
    backOrderedQuantity = 0;
    negativeInventoryItems.each { negativeInventoryItem ->
        backOrderedQuantity += negativeInventoryItem.getDouble("availableToPromiseTotal").doubleValue();
    }
    orderItemData.backOrderedQuantity = Math.abs(backOrderedQuantity);

    // Calculate how many units it should be possible to recieve for this purchase order
    availableToReceive = totalOrdered - totalReceived;
    totalAvailableToReceive += availableToReceive;
    orderItemData.availableToReceive = availableToReceive;
    orderItemData.totalQuantityReceived = totalReceived;
    orderItemData.shipGroupSeqId = orderItemAndShipGroupAssoc.shipGroupSeqId;
    orderItemData.orderItem = orderItem;
    orderItemData.product = product;
    orderItemDatas.put(orderItem.orderItemSeqId, orderItemData);
}
context.orderItemDatas = orderItemDatas.values();

// Handle any item product quantities to receive by adding to the map in session
productIdToReceive = request.getParameter("productId");
productQtyToReceive = request.getParameter("quantity");
context.newQuantity = productQtyToReceive;

if (productIdToReceive) {
    List candidateOrderItems = EntityUtil.filterByAnd(orderItems, [productId : productIdToReceive]);

    // If the productId as given isn't found in the order, try any goodIdentifications and use the first match
    if (!candidateOrderItems) {
        goodIdentifications = from("GoodIdentification").where("idValue", productIdToReceive).queryList();
        if (goodIdentifications) {
            giit = goodIdentifications.iterator();
            while (giit.hasNext()) {
                goodIdentification = giit.next();
                candidateOrderItems = EntityUtil.filterByAnd(orderItems, [productId : goodIdentification.productId]);
                if (candidateOrderItems) {
                    productIdToReceive = goodIdentification.productId;
                    break;
                }
            }
        }
    }

    if (candidateOrderItems) {
        quantity = 0;
        if (productQtyToReceive) {
            try {
                quantity = Double.parseDouble(productQtyToReceive);
            } catch (Exception e) {
                // Ignore the quantity update if there's a problem parsing it
            }
        }

        totalQuantityUsed = 0;
        totalQuantityToReceiveBefore = 0;
        pqit = candidateOrderItems.iterator();
        while (pqit.hasNext() && totalQuantityUsed < quantity) {
            candidateOrderItem = pqit.next();
            orderItemSeqId = candidateOrderItem.orderItemSeqId;
            qtyBefore = itemQuantitiesToReceive.containsKey(orderItemSeqId) ? itemQuantitiesToReceive.get(orderItemSeqId) : 0;
            totalQuantityToReceiveBefore += qtyBefore;
            qtyMaxAvailable = orderItemDatas.get(orderItemSeqId).availableToReceive - qtyBefore;

            if (qtyMaxAvailable <= 0) {
                continue;
            }

            qtyUsedForItem  = quantity - totalQuantityUsed >= qtyMaxAvailable ? qtyMaxAvailable : quantity - totalQuantityUsed;
            itemQuantitiesToReceive.put(orderItemSeqId, qtyUsedForItem + qtyBefore);
            totalQuantityUsed += qtyUsedForItem;
        }

        // If there's any quantity to receive left after using as much as possible for every relevant order item, add an error message to the context
        if (quantity > totalQuantityUsed) {
            context.ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive = true;
        }

        // Notify if some or all of the quantity just entered for the product will go to a backorder
        backOrderedQuantity = orderItemDatas.get(EntityUtil.getFirst(candidateOrderItems).orderItemSeqId).backOrderedQuantity - totalQuantityToReceiveBefore;

        if (backOrderedQuantity > 0) {
            totalQtyUsedForBackorders = backOrderedQuantity >= totalQuantityUsed ? totalQuantityUsed : backOrderedQuantity;
            if (totalQtyUsedForBackorders > 0) {
                context.quantityToReceive = totalQuantityUsed;
                context.quantityToBackOrder = totalQtyUsedForBackorders;
                context.ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder = true;
            }
        }
    } else {

        // Add an error message to the context if the productId doesn't exist in this purchase order
        context.ProductReceiveInventoryAgainstPurchaseOrderProductNotFound = true;
    }
}

// Put the tracking map back into the session, in case it has been reconstructed
session.setAttribute("purchaseOrderItemQuantitiesToReceive", itemQuantitiesToReceive);
context.itemQuantitiesToReceive = itemQuantitiesToReceive;
context.totalAvailableToReceive = totalAvailableToReceive;
