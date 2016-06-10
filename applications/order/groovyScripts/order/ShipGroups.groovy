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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;

orderId = parameters.orderId;
if (!orderId) return;

shipGroupSeqId = parameters.shipGroupSeqId;

// if a particular ship group is requested, we will limit ourselves to it
findMap = [orderId: orderId];
if (shipGroupSeqId) findMap.shipGroupSeqId = shipGroupSeqId;

shipGroups = from("OrderItemShipGroup").where(findMap).orderBy("shipGroupSeqId").queryList();
context.shipGroups = shipGroups;

// method to expand the marketing packages
LinkedList expandProductGroup(product, quantityInGroup, quantityShipped, quantityOpen, assocType) {
    sublines = [];
    associations = product.getRelated("MainProductAssoc", [productAssocTypeId : assocType], null, false);
    associations = EntityUtil.filterByDate(associations);
    associations.each { association ->
        line = [:];
        line.product = association.getRelatedOne("AssocProduct", false);

        // determine the quantities
        quantityComposed = association.quantity ?: 0;
        line.quantityInGroup = quantityInGroup * quantityComposed;
        line.quantityShipped = quantityShipped * quantityComposed;
        line.quantityOpen = quantityOpen * quantityComposed;

        sublines.add(line);
    }
    return sublines;
}

groupData = [:];
shipGroups.each { shipGroup ->
    data = [:];

    address = shipGroup.getRelatedOne("PostalAddress", false);
    data.address = address;

    phoneNumber = shipGroup.getRelatedOne("TelecomTelecomNumber", false);
    data.phoneNumber = phoneNumber;

    carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod", false);
    if (carrierShipmentMethod) {
        data.carrierShipmentMethod = carrierShipmentMethod;
        data.shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", true);
    }

    // the lines in a page, each line being a row of data to display
    lines = [];

    // process the order item to ship group associations, each being a line item for the group
    orderItemAssocs = shipGroup.getRelated("OrderItemShipGroupAssoc", null, ["orderItemSeqId"], false);
    orderItemAssocs.each { orderItemAssoc ->
        orderItem = orderItemAssoc.getRelatedOne("OrderItem", false);
        product = orderItem.getRelatedOne("Product", false);
        line = [:];

        // the quantity in group
        quantityInGroup = orderItemAssoc.quantity;
        if (orderItemAssoc.cancelQuantity) {
            quantityInGroup -= orderItemAssoc.cancelQuantity;
        }

        // the quantity shipped
        quantityShipped = 0.0;
        issuances = from("ItemIssuance").where("orderId", orderItem.orderId, "orderItemSeqId", orderItem.orderItemSeqId, "shipGroupSeqId", orderItemAssoc.shipGroupSeqId).queryList();
        issuances.each { issuance ->
            quantityShipped += issuance.quantity;
        }

        // the quantity open (ordered - shipped)
        quantityOpen = orderItem.quantity;
        if (orderItem.cancelQuantity) {
            quantityOpen -= orderItem.cancelQuantity;
        }
        quantityOpen -= quantityShipped;

        line.orderItem = orderItem;
        line.product = product;
        line.quantityInGroup = quantityInGroup;
        line.quantityShipped = quantityShipped;
        line.quantityOpen = quantityOpen;

        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG")) {
            assocType = EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG_AUTO") ? "MANUF_COMPONENT" : "PRODUCT_COMPONENT";
            sublines = expandProductGroup(product, quantityInGroup, quantityShipped, quantityOpen, assocType);
            line.expandedList = sublines;
        }

        lines.add(line);
    }
    data.lines = lines;
    groupData[shipGroup.shipGroupSeqId] = data;
}
context.groupData = groupData;
