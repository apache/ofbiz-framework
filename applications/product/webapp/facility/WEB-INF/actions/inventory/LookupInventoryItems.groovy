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

orderId = parameters.orderId;
partyId = parameters.partyId;
productId = parameters.productId;

if (orderId && productId) {
    shipmentReceiptAndItems = delegator.findByAnd("ShipmentReceiptAndItem", [orderId : orderId, productId : productId]);
    context.inventoryItemsForPo = shipmentReceiptAndItems;
    context.orderId = orderId;
}

if (partyId && productId) {
    orderRoles = delegator.findByAnd("OrderRole", [partyId : partyId, roleTypeId : "BILL_FROM_VENDOR"]);
    inventoryItemsForSupplier = [];
    orderRoles.each { orderRole ->
        shipmentReceiptAndItems = delegator.findByAnd("ShipmentReceiptAndItem", [productId : productId, orderId : orderRole.orderId]);
        inventoryItemsForSupplier.addAll(shipmentReceiptAndItems);
    }
    context.inventoryItemsForSupplier = inventoryItemsForSupplier;
    context.partyId = partyId;
}

if (productId) {
    inventoryItems = delegator.findByAnd("InventoryItem", [productId : productId]);
    context.inventoryItemsForProduct = inventoryItems;
    context.productId = productId;
    product = delegator.findByPrimaryKey("Product", [productId : productId]);
    context.internalName = product.internalName;
}
