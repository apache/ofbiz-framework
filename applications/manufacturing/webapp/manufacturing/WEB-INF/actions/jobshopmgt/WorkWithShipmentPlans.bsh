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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.widget.html.*;

delegator = request.getAttribute("delegator");

shipmentId = request.getParameter("shipmentId");
if (UtilValidate.isEmpty(shipmentId)) {
    shipmentId = context.get("shipmentId");
}
action = request.getParameter("action");

shipment = null;
if (UtilValidate.isNotEmpty(shipmentId)) {
    shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
}


// **************************************
// ShipmentPlan list form
// **************************************
shipmentPlans = null;
shipmentPlansIt = null;
rows = new ArrayList();
if (shipment != null) {
    shipmentPlans = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipment.getString("shipmentId")));
}
if (shipmentPlans != null) {
    boolean workInProgress = false;
    shipmentPlansIt = shipmentPlans.iterator();
    while (shipmentPlansIt.hasNext()) {
        shipmentPlan = shipmentPlansIt.next();
        oneRow = new HashMap(shipmentPlan);
        //    oneRow.putAll(shipmentPlan.getRelatedOne("OrderItemInventoryRes"));
        orderItem = shipmentPlan.getRelatedOne("OrderItem");
        oneRow.put("productId", orderItem.getString("productId"));
        oneRow.put("totOrderedQuantity", orderItem.getString("quantity"));
        // Total quantity issued
        issuedQuantity = 0.0;
        qtyIssuedInShipment = new HashMap();
        issuances = orderItem.getRelated("ItemIssuance");
        issuancesIt = issuances.iterator();
        while (issuancesIt.hasNext()) {
            issuance = issuancesIt.next();
            if (issuance.get("quantity") != null) {
                issuedQuantity += issuance.getDouble("quantity");
                if (qtyIssuedInShipment.containsKey(issuance.getString("shipmentId"))) {
                    qtyInShipment = ((Double)qtyIssuedInShipment.get(issuance.getString("shipmentId"))).doubleValue();
                    qtyInShipment += issuance.getDouble("quantity");
                    qtyIssuedInShipment.put(issuance.getString("shipmentId"), qtyInShipment);
                } else {
                    qtyIssuedInShipment.put(issuance.getString("shipmentId"), issuance.getDouble("quantity"));
                }
            }
        }
        oneRow.put("totIssuedQuantity", issuedQuantity);
        // Total quantity planned not issued
        plannedQuantity = 0.0;
        qtyPlannedInShipment = new HashMap();
        plans = delegator.findByAnd("OrderShipment", UtilMisc.toMap("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")));
        plansIt = plans.iterator();
        while (plansIt.hasNext()) {
            plan = plansIt.next();
            if (plan.get("quantity") != null) {
                netPlanQty = plan.getDouble("quantity");
                if (qtyIssuedInShipment.containsKey(plan.getString("shipmentId"))) {
                    qtyInShipment = ((Double)qtyIssuedInShipment.get(plan.getString("shipmentId"))).doubleValue();
                    if (netPlanQty > qtyInShipment) {
                        netPlanQty -= qtyInShipment;
                    } else {
                        netPlanQty = 0;
                    }
                }
                plannedQuantity += netPlanQty;
                if (qtyPlannedInShipment.containsKey(plan.getString("shipmentId"))) {
                    qtyInShipment = ((Double)qtyPlannedInShipment.get(plan.getString("shipmentId"))).doubleValue();
                    qtyInShipment += netPlanQty;
                    qtyPlannedInShipment.put(plan.getString("shipmentId"), qtyInShipment);
                } else {
                    qtyPlannedInShipment.put(plan.getString("shipmentId"), netPlanQty);
                }
            }
        }
        oneRow.put("totPlannedQuantity", plannedQuantity);
        if (qtyIssuedInShipment.containsKey(shipmentId)) {
            oneRow.put("issuedQuantity", qtyIssuedInShipment.get(shipmentId));
        } else {
            oneRow.put("issuedQuantity", "");
        }
        // Reserved and Not Available quantity
        reservedQuantity = 0.0;
        reservedNotAvailable = 0.0;
        reservations = orderItem.getRelated("OrderItemShipGrpInvRes");
        reservationsIt = reservations.iterator();
        while (reservationsIt.hasNext()) {
            reservation = reservationsIt.next();
            if (reservation.get("quantity") != null) {
                reservedQuantity += reservation.getDouble("quantity");
            }
            if (reservation.get("quantityNotAvailable") != null) {
                reservedNotAvailable += reservation.getDouble("quantityNotAvailable");
            }
        }
        oneRow.put("notAvailableQuantity", reservedNotAvailable);
        // Planned Weight and Volume
        product = orderItem.getRelatedOne("Product");
        weight = 0.0;
        quantity = 0.0;
        if (shipmentPlan.getDouble("quantity") != null) {
            quantity = shipmentPlan.getDouble("quantity");
        }
        if (product.getDouble("weight") != null) {
            weight = product.getDouble("weight") * quantity;
        }
        oneRow.put("weight", weight);
        if (product.get("weightUomId") != null) {
            weightUom = delegator.findByPrimaryKeyCache("Uom", UtilMisc.toMap("uomId", product.getString("weightUomId")));
            oneRow.put("weightUom", weightUom.getString("abbreviation"));
        }
        volume = 0.0;
        if (product.getDouble("productHeight") != null &&
            product.getDouble("productWidth") != null &&
            product.getDouble("productDepth") != null) {
                // TODO: check if uom conversion is needed
                volume = product.getDouble("productHeight") *
                         product.getDouble("productWidth") *
                         product.getDouble("productDepth") * 
                         quantity;
        }
        oneRow.put("volume", volume);
        if (product.get("heightUomId") != null &&
            product.get("widthUomId") != null &&
            product.get("depthUomId") != null) {

            heightUom = delegator.findByPrimaryKeyCache("Uom", UtilMisc.toMap("uomId", product.getString("heightUomId")));
            widthUom = delegator.findByPrimaryKeyCache("Uom", UtilMisc.toMap("uomId", product.getString("widthUomId")));
            depthUom = delegator.findByPrimaryKeyCache("Uom", UtilMisc.toMap("uomId", product.getString("depthUomId")));
            oneRow.put("volumeUom", heightUom.getString("abbreviation") + "x" +
                                    widthUom.getString("abbreviation") + "x" +
                                    depthUom.getString("abbreviation"));
        }
        // Select the production runs, if available
        productionRuns = delegator.findByAnd("WorkOrderItemFulfillment", UtilMisc.toMap("orderId", shipmentPlan.getString("orderId"), "orderItemSeqId", shipmentPlan.getString("orderItemSeqId")), UtilMisc.toList("workEffortId")); // TODO: add shipmentId
        if (productionRuns != null && productionRuns.size() > 0) {
            workInProgress = true;
            productionRunsIt = productionRuns.iterator();
            productionRunsId = "";
            while (productionRunsIt.hasNext()) {
                productionRun = productionRunsIt.next();
                productionRunsId = productionRun.getString("workEffortId") + " " + productionRunsId;
            }
            oneRow.put("productionRuns", productionRunsId);
        }

        rows.add(oneRow);
    }
    context.put("workInProgress", workInProgress);
    HtmlFormWrapper listShipmentPlanForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "listShipmentPlan", request, response);
    listShipmentPlanForm.putInContext("shipmentPlan", rows);
    context.put("listShipmentPlanForm", listShipmentPlanForm); // Form for ShipmentPlan list
} else {
    List shipments = new ArrayList();
    List scheduledShipments = delegator.findByAndCache("Shipment", UtilMisc.toMap("shipmentTypeId", "SALES_SHIPMENT", "statusId", "SHIPMENT_SCHEDULED"));
    Iterator scheduledShipmentsIt = scheduledShipments.iterator();
    while (scheduledShipmentsIt.hasNext()) {
        // TODO: put in the list only the shipments with a shipment plan
        shipments.add(scheduledShipmentsIt.next());
    }
    //List confirmedShipments = delegator.findByAndCache("Shipment", UtilMisc.toMap("shipmentTypeId", "SALES_SHIPMENT", "statusId", "SCHEDULED_CONFIRMED"));

    HtmlFormWrapper listShipmentPlansForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "listShipmentPlans", request, response);
    listShipmentPlansForm.putInContext("shipmentPlans", shipments);
    context.put("listShipmentPlansForm", listShipmentPlansForm);
}
context.put("shipmentId", shipmentId);
context.put("shipment", shipment);

