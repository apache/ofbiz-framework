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

// This script gets shipment items grouped by package for use in the packing slip PDF or any screens that require by-package layout

import org.ofbiz.base.util.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityTypeUtil

// Since this script is run after ViewShipment, we will re-use the shipment in the context
shipment = context.shipment;
if (!shipment) {
    return;
}

// get the packages related to this shipment in order of packages
shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false);

// first we scan the shipment items and count the quantity of each product that is being shipped
quantityShippedByProduct = [:];
quantityInShipmentByProduct = [:];
shipmentItems = shipment.getRelated("ShipmentItem", null, null, false);
shipmentItems.each { shipmentItem ->
    productId = shipmentItem.productId;
    shipped = quantityShippedByProduct.get(productId);
    if (!shipped) {
        shipped = 0 as Double;
    }
    shipped += shipmentItem.getDouble("quantity").doubleValue();
    quantityShippedByProduct.put(productId, shipped);
    quantityInShipmentByProduct.put(productId, shipped);
}

// Add in the total of all previously shipped items
previousShipmentIter = from("Shipment")
                            .where(EntityCondition.makeCondition(
                                            UtilMisc.toList(
                                                EntityCondition.makeCondition("primaryOrderId", EntityOperator.EQUALS, shipment.getString("primaryOrderId")),
                                                EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, "SALES_SHIPMENT"),
                                                EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO,
                                                    ObjectType.simpleTypeConvert(shipment.getString("createdDate"), "Timestamp", null, null))
                                            ),
                                        EntityOperator.AND))
                            .queryIterator();

while (previousShipmentItem = previousShipmentIter.next()) {
    if (!previousShipmentItem.shipmentId.equals(shipment.shipmentId)) {
        previousShipmentItems = previousShipmentItem.getRelated("ShipmentItem", null, null, false);
        previousShipmentItems.each { shipmentItem ->
            productId = shipmentItem.productId;
            shipped = quantityShippedByProduct.get(productId);
            if (!shipped) {
                shipped = new Double(0);
            }
            shipped += shipmentItem.getDouble("quantity").doubleValue();
            quantityShippedByProduct.put(productId, shipped);
        }
    }
}
previousShipmentIter.close();

// next scan the order items (via issuances) to count the quantity of each product requested
quantityRequestedByProduct = [:];
countedOrderItems = [:]; // this map is only used to keep track of the order items already counted
order = shipment.getRelatedOne("PrimaryOrderHeader", false);
issuances = order.getRelated("ItemIssuance", null, null, false);
issuances.each { issuance ->
    orderItem = issuance.getRelatedOne("OrderItem", false);
    productId = orderItem.productId;
    if (!countedOrderItems.containsKey(orderItem.orderId + orderItem.orderItemSeqId)) {
        countedOrderItems.put(orderItem.orderId + orderItem.orderItemSeqId, null);
        requested = quantityRequestedByProduct.get(productId);
        if (!requested) {
            requested = new Double(0);
        }
        cancelQuantity = orderItem.getDouble("cancelQuantity");
        quantity = orderItem.getDouble("quantity");
        requested += quantity.doubleValue() - (cancelQuantity ? cancelQuantity.doubleValue() : 0);
        quantityRequestedByProduct.put(productId, requested);
    }
}

// for each package, we want to list the quantities and details of each product
packages = []; // note we assume that the package number is simply the index + 1 of this list
shipmentPackages.each { shipmentPackage ->
    contents = shipmentPackage.getRelated("ShipmentPackageContent", null, ['shipmentItemSeqId'], false);

    // each line is one logical Product and the quantities associated with it
    lines = [];
    contents.each { content ->
        shipmentItem = content.getRelatedOne("ShipmentItem", false);
        product = shipmentItem.getRelatedOne("Product", false);
        productTypeId = product.get("productTypeId");

        line = [:];
        line.product = product;
        line.quantityRequested = quantityRequestedByProduct.get(product.productId);
        line.quantityInPackage = content.quantity;
        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityInPackage > line.quantityRequested) {
            line.quantityInPackage = line.quantityRequested;
        }
        line.quantityInShipment = quantityInShipmentByProduct.get(product.productId);
        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityInShipment > line.quantityRequested) {
            line.quantityInShipment = line.quantityRequested;
        }
        line.quantityShipped = quantityShippedByProduct.get(product.productId);
        if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", productTypeId, "parentTypeId", "MARKETING_PKG_PICK") && line.quantityShipped > line.quantityRequested) {
            line.quantityShipped = line.quantityRequested;
        }
        lines.add(line);
    }
    packages.add(lines);
}
context.packages = packages;
