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

import org.ofbiz.widget.html.HtmlFormWrapper;

shipmentId = parameters.shipmentId ?: context.get("shipmentId");

action = parameters.action;

shipment = null;
if (shipmentId) {
    shipment = delegator.findByPrimaryKey("Shipment", [shipmentId : shipmentId]);
}

// **************************************
// ShipmentPlan list form
// **************************************
shipmentPlans = [];
rows = [];
if (shipment) {
    shipmentPlans = delegator.findByAnd("OrderShipment", [shipmentId : shipment.shipmentId]);
}
if (shipmentPlans) {
    boolean workInProgress = false;
    shipmentPlans.each { shipmentPlan ->
        oneRow = new HashMap(shipmentPlan);
        //    oneRow.putAll(shipmentPlan.getRelatedOne("OrderItemInventoryRes"));
        orderItem = shipmentPlan.getRelatedOne("OrderItem");
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
        issuances = orderItem.getRelated("ItemIssuance");
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
        plans = delegator.findByAnd("OrderShipment", [orderId : orderItem.orderId ,orderItemSeqId : orderItem.orderItemSeqId]);
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
                    qtyPlannedInShipment.plan.shipmentId = qtyInShipment;
                } else {
                    qtyPlannedInShipment.plan.shipmentId = netPlanQty;
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
        reservations = orderItem.getRelated("OrderItemShipGrpInvRes");
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
        product = orderItem.getRelatedOne("Product");
        weight = 0.0;
        quantity = 0.0;
        if (shipmentPlan.quantity) {
            quantity = shipmentPlan.quantity;
        }
        if (product.weight) {
            weight = product.weight * quantity;
        }
        oneRow.weight = weight;
        if (product.weightUomId) {
            weightUom = delegator.findByPrimaryKeyCache("Uom", [uomId : product.weightUomId]);
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

            heightUom = delegator.findByPrimaryKeyCache("Uom", [uomId : product.heightUomId]);
            widthUom = delegator.findByPrimaryKeyCache("Uom", [uomId : product.widthUomId]);
            depthUom = delegator.findByPrimaryKeyCache("Uom", [uomId : product.depthUomId]);
            oneRow.volumeUom = heightUom.abbreviation + "x" +
                                    widthUom.abbreviation + "x" +
                                    depthUom.abbreviation;
        }
        // Select the production runs, if available
        productionRuns = delegator.findByAnd("WorkOrderItemFulfillment", [orderId : shipmentPlan.orderId , orderItemSeqId : shipmentPlan.orderItemSeqId],["workEffortId"]); // TODO: add shipmentId
        if (productionRuns) {
            workInProgress = true;
            productionRuns.each { productionRun ->
                productionRunsId = productionRun.workEffortId + " " + productionRunsId;
            }
            oneRow.productionRuns = productionRunsId;
        }

        rows.add(oneRow);
    }
    context.workInProgress = workInProgress;
    HtmlFormWrapper listShipmentPlanForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "listShipmentPlan", request, response);
    listShipmentPlanForm.putInContext("shipmentPlan", rows);
    context.listShipmentPlanForm = listShipmentPlanForm; // Form for ShipmentPlan list
} else {
    shipments = [];
    scheduledShipments = delegator.findByAndCache("Shipment", [shipmentTypeId : "SALES_SHIPMENT", statusId : "SHIPMENT_SCHEDULED"]);
    scheduledShipments.each { scheduledShipment ->
        shipments.add(scheduledShipment);
    }
    //List confirmedShipments = delegator.findByAndCache("Shipment", UtilMisc.toMap("shipmentTypeId", "SALES_SHIPMENT", "statusId", "SCHEDULED_CONFIRMED"));

    HtmlFormWrapper listShipmentPlansForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "listShipmentPlans", request, response);
    listShipmentPlansForm.putInContext("shipmentPlans", shipments);
    context.listShipmentPlansForm = listShipmentPlansForm;
}
context.shipmentId = shipmentId;
context.shipment = shipment;

