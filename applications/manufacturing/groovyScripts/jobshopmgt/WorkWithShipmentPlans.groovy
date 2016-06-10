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

import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;

// **************************************
// ShipmentPlan list form
// **************************************
shipmentPlans = [];
rows = [];
if (shipment && shipment.shipmentId) {
    shipmentPlans = from("OrderShipment").where("shipmentId", shipment.shipmentId).queryList();
}
if (shipmentPlans) {
    workInProgress = "false";
    shipmentPlans.each { shipmentPlan ->
        oneRow = new HashMap(shipmentPlan);
        //    oneRow.putAll(shipmentPlan.getRelatedOne("OrderItemInventoryRes", false));
        orderItem = shipmentPlan.getRelatedOne("OrderItem", false);
        oneRow.productId = orderItem.productId;
        orderedQuantity = orderItem.quantity;
        canceledQuantity = orderItem.cancelQuantity;
        if (canceledQuantity) {
            orderedQuantity = orderedQuantity - canceledQuantity;
        }
        oneRow.totOrderedQuantity = orderedQuantity.intValue();
        // Total quantity issued
        issuedQuantity = 0.0;
        qtyIssuedInShipment = [:];
        issuances = orderItem.getRelated("ItemIssuance", null, null, false);
        issuances.each { issuance ->
            if (issuance.quantity) {
                issuedQuantity += issuance.quantity;
                if (issuance.cancelQuantity) {
                    issuedQuantity -= issuance.cancelQuantity;
                }
                if (qtyIssuedInShipment.containsKey(issuance.shipmentId)) {
                    qtyInShipment = qtyIssuedInShipment[issuance.shipmentId];
                    qtyInShipment += issuance.quantity;
                    qtyIssuedInShipment.issuance.shipmentId = qtyInShipment;
                } else {
                    qtyInShipment = issuance.quantity;
                    if (issuance.cancelQuantity) {
                        qtyInShipment -= issuance.cancelQuantity;
                    }
                    qtyIssuedInShipment.issuance.shipmentId = qtyInShipment;
                }
            }
        }
        oneRow.totIssuedQuantity = issuedQuantity;
        // Total quantity planned not issued
        plannedQuantity = 0.0;
        qtyPlannedInShipment = [:];
        plans = from("OrderShipment").where("orderId", orderItem.orderId ,"orderItemSeqId", orderItem.orderItemSeqId).queryList();
        plans.each { plan ->
            if (plan.quantity) {
                netPlanQty = plan.quantity;
                if (qtyIssuedInShipment.containsKey(plan.shipmentId)) {
                    qtyInShipment = qtyIssuedInShipment[plan.shipmentId];
                    if (netPlanQty > qtyInShipment) {
                        netPlanQty -= qtyInShipment;
                    } else {
                        netPlanQty = 0.0;
                    }
                }
                plannedQuantity += netPlanQty;
                if (qtyPlannedInShipment.containsKey(plan.shipmentId)) {
                    qtyInShipment = qtyPlannedInShipment[plan.shipmentId];
                    qtyInShipment += netPlanQty;
                    qtyPlannedInShipment[plan.shipmentId] = qtyInShipment;
                } else {
                    qtyPlannedInShipment[plan.shipmentId] = netPlanQty;
                }
            }
        }
        oneRow.totPlannedQuantity = plannedQuantity;
        if (qtyIssuedInShipment.containsKey(shipmentPlan.shipmentId)) {
            oneRow.issuedQuantity = qtyIssuedInShipment.get(shipmentPlan.shipmentId);
        } else {
            oneRow.issuedQuantity = "";
        }
        // Reserved and Not Available quantity
        reservedQuantity = 0.0;
        reservedNotAvailable = 0.0;
        reservations = orderItem.getRelated("OrderItemShipGrpInvRes", null, null, false);
        reservations.each { reservation ->
            if (reservation.quantity) {
                reservedQuantity += reservation.quantity;
            }
            if (reservation.quantityNotAvailable) {
                reservedNotAvailable += reservation.quantityNotAvailable;
            }
        }
        oneRow.notAvailableQuantity = reservedNotAvailable;
        // Planned Weight and Volume
        product = orderItem.getRelatedOne("Product", false);
        weight = 0.0;
        quantity = 0.0;
        if (shipmentPlan.quantity) {
            quantity = shipmentPlan.quantity;
        }
        if (product.productWeight) {
            weight = product.productWeight * quantity;
        }
        oneRow.weight = weight;
        if (product.weightUomId) {
            weightUom = from("Uom").where("uomId", product.weightUomId).cache(true).queryOne();
            oneRow.weightUom = weightUom.abbreviation;
        }
        volume = 0.0;
        if (product.productHeight &&
            product.productWidth &&
            product.productDepth) {
                // TODO: check if uom conversion is needed
                volume = product.productHeight *
                         product.productWidth *
                         product.productDepth *
                         quantity;
        }
        oneRow.volume = volume;
        if (product.heightUomId &&
            product.widthUomId &&
            product.depthUomId) {

            heightUom = from("Uom").where("uomId", product.heightUomId).cache(true).queryOne();
            widthUom = from("Uom").where("uomId", product.widthUomId).cache(true).queryOne();
            depthUom = from("Uom").where("uomId", product.depthUomId).cache(true).queryOne();
            oneRow.volumeUom = heightUom.abbreviation + "x" +
                                    widthUom.abbreviation + "x" +
                                    depthUom.abbreviation;
        }
        rows.add(oneRow);
        // Select the production runs, if available
        productionRuns = from("WorkOrderItemFulfillment").where("orderId", shipmentPlan.orderId, "orderItemSeqId", shipmentPlan.orderItemSeqId, "shipGroupSeqId", shipmentPlan.shipGroupSeqId).orderBy("workEffortId").queryList();
        if (productionRuns) {
            workInProgress = "true";
            productionRunsId = "";
            productionRuns.each { productionRun ->
                productionRunRow = new HashMap();
                productionRunRow.put("productionRunId", productionRun.workEffortId);
                ProductionRun productionRunWrapper = new ProductionRun(productionRun.workEffortId, delegator, dispatcher);
                productionRunRow.put("productionRunEstimatedCompletionDate", productionRunWrapper.getEstimatedCompletionDate());
                productionRunRow.put("productionRunStatusId", productionRunWrapper.getGenericValue().currentStatusId);
                productionRunRow.put("productionRunQuantityProduced", productionRunWrapper.getGenericValue().quantityProduced);
                rows.add(productionRunRow);
            }
        }
    }
    context.workInProgress = workInProgress;
    context.shipmentPlan = rows;
}
