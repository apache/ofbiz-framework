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

import org.ofbiz.entity.condition.EntityCondition;

shipmentId = request.getParameter("shipmentId");
orderId = request.getParameter("orderId");
shipGroupSeqId = request.getParameter("shipGroupSeqId");

if (!shipmentId) {
    shipmentId = context.shipmentId;
}
action = request.getParameter("action");

shipment = null;
if (shipmentId) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
}


// **************************************
// Order Items are searched also by shipGroupSeqId and put in orderItemShipGroupAssocs
// **************************************
orderItemShipGroupAssocs = null;
// **************************************
// Search method: search by productId
// **************************************
if (action && orderId) {
    if (shipGroupSeqId) {
        orderItemShipGroupAssocs = from("OrderItemShipGroupAssoc").where("orderId", orderId, "shipGroupSeqId", shipGroupSeqId).queryList();
    } else {
        orderItemShipGroupAssocs = from("OrderItemShipGroupAssoc").where("orderId", orderId).queryList();
    }
}

// **************************************
// ShipmentPlan list form
// **************************************
totWeight = 0;
totVolume = 0;
shipmentPlans = null;
shipmentPlansIt = null;
rows = [] as ArrayList;
if (shipment) {
    shipmentPlans = from("OrderShipment").where("shipmentId", shipment.shipmentId).queryList();
}
if (shipmentPlans) {
    shipmentPlans.each { shipmentPlan ->
        oneRow = new HashMap(shipmentPlan);
        //    oneRow.putAll(shipmentPlan.getRelatedOne("OrderItemShipGrpInvRes", false));
        orderItem = shipmentPlan.getRelatedOne("OrderItem", false);
        oneRow.productId = orderItem.productId;
        orderedQuantity = orderItem.getDouble("quantity");
        canceledQuantity = orderItem.getDouble("cancelQuantity");
        if (canceledQuantity) {
            orderedQuantity = Double.valueOf(orderedQuantity.doubleValue() - canceledQuantity.doubleValue());
        }
        oneRow.totOrderedQuantity = orderedQuantity.intValue();

        // Total quantity issued
        issuedQuantity = 0.0;
        qtyIssuedInShipment = [:];
        issuances = orderItem.getRelated("ItemIssuance", null, null, false);
        issuances.each { issuance ->
            if (issuance.quantity) {
                issuedQuantity += issuance.getDouble("quantity");
                if (issuance.cancelQuantity) {
                    issuedQuantity -= issuance.getDouble("cancelQuantity");
                }
                if (qtyIssuedInShipment.containsKey(issuance.shipmentId)) {
                    qtyInShipment = ((Double)qtyIssuedInShipment.get(issuance.shipmentId)).doubleValue();
                    qtyInShipment += issuance.getDouble("quantity");
                    if (issuance.cancelQuantity) {
                        qtyInShipment -= issuance.getDouble("cancelQuantity");
                    }
                    qtyIssuedInShipment.put(issuance.shipmentId, qtyInShipment);
                } else {
                    qtyInShipment = issuance.getDouble("quantity");
                    if (issuance.cancelQuantity) {
                        qtyInShipment -= issuance.getDouble("cancelQuantity");
                    }
                    qtyIssuedInShipment.put(issuance.shipmentId, qtyInShipment);
                }
            }
        }
        oneRow.totIssuedQuantity = issuedQuantity;
        // Total quantity planned not issued
        plannedQuantity = 0.0;
        qtyPlannedInShipment = [:];
        plans = from("OrderShipment").where("orderId", orderItem.orderId, "orderItemSeqId", orderItem.orderItemSeqId).queryList();
        plans.each { plan ->
            if (plan.quantity) {
                netPlanQty = plan.getDouble("quantity");
                if (qtyIssuedInShipment.containsKey(plan.shipmentId)) {
                    qtyInShipment = ((Double)qtyIssuedInShipment.get(plan.shipmentId)).doubleValue();
                    if (netPlanQty > qtyInShipment) {
                        netPlanQty -= qtyInShipment;
                    } else {
                        netPlanQty = 0;
                    }
                }
                plannedQuantity += netPlanQty;
                if (qtyPlannedInShipment.containsKey(plan.shipmentId)) {
                    qtyInShipment = ((Double)qtyPlannedInShipment.get(plan.shipmentId)).doubleValue();
                    qtyInShipment += netPlanQty;
                    qtyPlannedInShipment.put(plan.shipmentId, qtyInShipment);
                } else {
                    qtyPlannedInShipment.put(plan.shipmentId, netPlanQty);
                }
            }
        }
        oneRow.totPlannedQuantity = plannedQuantity;
        if (qtyIssuedInShipment.containsKey(shipmentId)) {
            oneRow.issuedQuantity = qtyIssuedInShipment.get(shipmentId);
        } else {
            oneRow.issuedQuantity = "";
        }
        // Reserved and Not Available quantity
        reservedQuantity = 0.0;
        reservedNotAvailable = 0.0;
        reservations = orderItem.getRelated("OrderItemShipGrpInvRes", null, null, false);
        reservations.each { reservation ->
            if (reservation.quantity) {
                reservedQuantity += reservation.getDouble("quantity");
            }
            if (reservation.quantityNotAvailable) {
                reservedNotAvailable += reservation.getDouble("quantityNotAvailable");
            }
        }
        oneRow.notAvailableQuantity = reservedNotAvailable;

        // Planned Weight and Volume
        product = orderItem.getRelatedOne("Product", false);
        weight = 0.0;
        quantity = 0.0;
        if (shipmentPlan.getDouble("quantity")) {
            quantity = shipmentPlan.getDouble("quantity");
        }
        if (product.getDouble("productWeight")) {
            weight = product.getDouble("productWeight") * quantity;
        }
        oneRow.weight = weight;
        if (product.weightUomId) {
            weightUom = from("Uom").where("uomId", product.weightUomId).queryOne();
            oneRow.weightUom = weightUom.abbreviation;
        }
        volume = 0.0;
        if (product.getDouble("productHeight") &&
            product.getDouble("productWidth") &&
            product.getDouble("productDepth")) {
                // TODO: check if uom conversion is needed
                volume = product.getDouble("productHeight") *
                         product.getDouble("productWidth") *
                         product.getDouble("productDepth") *
                         quantity;
        }
        oneRow.volume = volume;
        if (product.heightUomId && product.widthUomId && product.depthUomId) {
            heightUom = from("Uom").where("uomId", product.heightUomId).cache(true).queryOne();
            widthUom = from("Uom").where("uomId", product.widthUomId).cache(true).queryOne();
            depthUom = from("Uom").where("uomId", product.depthUomId).cache(true).queryOne();
            oneRow.volumeUom = heightUom.abbreviation + "x" + widthUom.abbreviation + "x" + depthUom.abbreviation;
        }
        totWeight += weight;
        totVolume += volume;
        rows.add(oneRow);
    }
}

// **************************************
// ShipmentPlan add form
// **************************************
addRows = [] as ArrayList;
if (orderItemShipGroupAssocs) {
    orderItemShipGroupAssocs.each { orderItemShipGroupAssoc ->
        orderItem = orderItemShipGroupAssoc.getRelatedOne("OrderItem", false);
        oneRow = [:];
        oneRow.shipmentId = shipmentId;
        oneRow.orderId = orderItemShipGroupAssoc.orderId;
        oneRow.orderItemSeqId = orderItemShipGroupAssoc.orderItemSeqId;
        oneRow.shipGroupSeqId = orderItemShipGroupAssoc.shipGroupSeqId;
        oneRow.productId = orderItem.productId;
        orderedQuantity = orderItemShipGroupAssoc.getDouble("quantity");
        canceledQuantity = orderItemShipGroupAssoc.getDouble("cancelQuantity");
        if (canceledQuantity) {
            orderedQuantity = Double.valueOf(orderedQuantity.doubleValue() - canceledQuantity.doubleValue());
        }
        oneRow.orderedQuantity = orderedQuantity;
        // Total quantity issued
        issuedQuantity = 0.0;
        qtyIssuedInShipment = [:];
        issuances = orderItem.getRelated("ItemIssuance", null, null, false);
        issuances.each { issuance ->
            if (issuance.quantity) {
                issuedQuantity += issuance.getDouble("quantity");
                if (issuance.cancelQuantity) {
                    issuedQuantity -= issuance.getDouble("cancelQuantity");
                }
                if (qtyIssuedInShipment.containsKey(issuance.shipmentId)) {
                    qtyInShipment = ((Double)qtyIssuedInShipment.get(issuance.shipmentId)).doubleValue();
                    qtyInShipment += issuance.getDouble("quantity");
                    qtyIssuedInShipment.put(issuance.shipmentId, qtyInShipment);
                } else {
                    qtyInShipment = issuance.getDouble("quantity");
                    if (issuance.cancelQuantity) {
                        qtyInShipment -= issuance.getDouble("cancelQuantity");
                    }
                    qtyIssuedInShipment.put(issuance.shipmentId, qtyInShipment);
                }
            }
        }
        oneRow.issuedQuantity = issuedQuantity;
        // Total quantity planned not issued
        plannedQuantity = 0.0;
        EntityCondition orderShipmentCondition = null;
        if (shipGroupSeqId) {
            orderShipmentCondition = EntityCondition.makeCondition([orderId : orderItemShipGroupAssoc.orderId, orderItemSeqId : orderItemShipGroupAssoc.orderItemSeqId, shipGroupSeqId : orderItemShipGroupAssoc.shipGroupSeqId]);
        } else {
            orderShipmentCondition = EntityCondition.makeCondition([orderId : orderItemShipGroupAssoc.orderId, orderItemSeqId : orderItemShipGroupAssoc.orderItemSeqId]);
        }
        plans = from("OrderShipment").where(orderShipmentCondition).queryList();
        plans.each { plan ->
            if (plan.quantity) {
                netPlanQty = plan.getDouble("quantity");
                plannedQuantity += netPlanQty;
            }
        }
        oneRow.plannedQuantity = plannedQuantity;

        // (default) quantity for plan
        planQuantity = (orderedQuantity - plannedQuantity - issuedQuantity > 0? orderedQuantity - plannedQuantity - issuedQuantity: 0);
        oneRow.quantity = planQuantity;

        // Planned (unitary) Weight and Volume
        weight = new Double(0);
        product = orderItem.getRelatedOne("Product", false);
        if (product.getDouble("productWeight")) {
            weight = product.getDouble("productWeight");
        }
        oneRow.weight = weight;

        if (product.weightUomId) {
            weightUom = from("Uom").where("uomId", product.weightUomId).cache(true).queryOne();
            oneRow.weightUom = weightUom.abbreviation;
        }
        volume = 0.0;
        if (product.getDouble("productHeight") && product.getDouble("productWidth") && product.getDouble("productDepth")) {
                // TODO: check if uom conversion is needed
                volume = product.getDouble("productHeight") *
                         product.getDouble("productWidth") *
                         product.getDouble("productDepth");
        }

        oneRow.volume = volume;
        if (product.heightUomId && product.widthUomId && product.depthUomId) {
            heightUom = from("Uom").where("uomId", product.heightUomId).cache(true).queryOne();
            widthUom = from("Uom").where("uomId", product.widthUomId).cache(true).queryOne();
            depthUom = from("Uom").where("uomId", product.depthUomId).cache(true).queryOne();
            oneRow.volumeUom = heightUom.abbreviation + "x" + widthUom.abbreviation + "x" + depthUom.abbreviation;
        }
        addRows.add(oneRow);
    }
}

context.listShipmentPlanRows = rows;
context.addToShipmentPlanRows = addRows;
context.rowCount = addRows.size();
context.shipmentId = shipmentId;
context.shipment = shipment;
context.totWeight = totWeight;
context.totVolume = totVolume;
