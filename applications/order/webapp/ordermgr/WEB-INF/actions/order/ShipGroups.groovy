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

import javolution.util.FastMap;
import javolution.util.FastList;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.common.CommonWorkers;
import org.ofbiz.entity.util.EntityUtil;

orderId = parameters.orderId;
if (!orderId) return;

shipGroupSeqId = parameters.shipGroupSeqId;

// if a particular ship group is requested, we will limit ourselves to it
findMap = [orderId: orderId];
if (shipGroupSeqId) findMap.shipGroupSeqId = shipGroupSeqId;

shipGroups = delegator.findByAnd("OrderItemShipGroup", findMap, ["shipGroupSeqId"]);
context.shipGroups = shipGroups;

// method to expand the marketing packages
FastList expandProductGroup(product, quantityInGroup, quantityShipped, quantityOpen, assocType) {
    sublines = FastList.newInstance();
    associations = product.getRelatedByAnd("MainProductAssoc", [productAssocTypeId : assocType]);
    associations = EntityUtil.filterByDate(associations);
    associations.each { association ->
        line = FastMap.newInstance();
        line.product = association.getRelatedOne("AssocProduct");

        // determine the quantities
        quantityComposed = association.quantity ?: 0;
        line.quantityInGroup = quantityInGroup * quantityComposed;
        line.quantityShipped = quantityShipped * quantityComposed;
        line.quantityOpen = quantityOpen * quantityComposed;

        sublines.add(line);
    }
    return sublines;
}

groupData = FastMap.newInstance();
shipGroups.each { shipGroup ->
    data = FastMap.newInstance();

    address = shipGroup.getRelatedOne("PostalAddress");
    data.address = address;

    phoneNumber = shipGroup.getRelatedOne("TelecomTelecomNumber");
    data.phoneNumber = phoneNumber;

    carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod");
    if (carrierShipmentMethod) {
        data.carrierShipmentMethod = carrierShipmentMethod;
        data.shipmentMethodType = carrierShipmentMethod.getRelatedOneCache("ShipmentMethodType");
    }

    // the lines in a page, each line being a row of data to display
    lines = FastList.newInstance();

    // process the order item to ship group associations, each being a line item for the group
    orderItemAssocs = shipGroup.getRelated("OrderItemShipGroupAssoc", ["orderItemSeqId"]);
    orderItemAssocs.each { orderItemAssoc ->
        orderItem = orderItemAssoc.getRelatedOne("OrderItem");
        product = orderItem.getRelatedOne("Product");
        line = FastMap.newInstance();

        // the quantity in group
        quantityInGroup = orderItemAssoc.quantity;
        if (orderItemAssoc.cancelQuantity) {
            quantityInGroup -= orderItemAssoc.cancelQuantity;
        }

        // the quantity shipped
        quantityShipped = 0.0;
        issuances = delegator.findByAnd("ItemIssuance", [orderId : orderItem.orderId, orderItemSeqId : orderItem.orderItemSeqId, shipGroupSeqId : orderItemAssoc.shipGroupSeqId]);
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

        if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG")) {
            assocType = CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG_AUTO") ? "MANUF_COMPONENT" : "PRODUCT_COMPONENT";
            sublines = expandProductGroup(product, quantityInGroup, quantityShipped, quantityOpen, assocType);
            line.expandedList = sublines;
        }

        lines.add(line);
    }
    data.lines = lines;
    groupData[shipGroup.shipGroupSeqId] = data;
}
context.groupData = groupData;
