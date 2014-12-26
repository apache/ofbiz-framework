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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

shipmentId = parameters.shipmentId;
items = [];
shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
partyId = shipment.partyIdTo;
shipmentItems = shipment.getRelated("ShipmentItem", null, null, false);
shipmentItems.each { shipmentItem ->
    productId = shipmentItem.productId;
    internalName = shipmentItem.getRelated("Product", null, null, false).internalName;
    EntityCondition cond = EntityCondition.makeCondition([EntityCondition.makeCondition("returnId", shipment.primaryReturnId),
                                   EntityCondition.makeCondition("productId", productId)], EntityOperator.AND);
    returnItem = from("ReturnItem").where("returnId", shipment.primaryReturnId, "productId", productId).cache(true).queryFirst();
    returnQuantity = Double.valueOf(returnItem.returnQuantity);

    shipmentItemQty = Double.valueOf(shipmentItem.quantity);
    itemIssuances = shipmentItem.getRelated("ItemIssuance", [shipmentId : shipmentId, shipmentItemSeqId : shipmentItem.shipmentItemSeqId], ["inventoryItemId"], false);
    totalQtyIssued = 0;
    issuedItems = [];
    itemIssuances.each { itemIssuance ->
        totalQtyIssued = totalQtyIssued + Double.valueOf(itemIssuance.quantity);
        issuedItems.add([inventoryItemId : itemIssuance.inventoryItemId,
                         quantity : itemIssuance.quantity]);
    }
    qtyStillNeedToBeIssued = returnQuantity - totalQtyIssued;
    items.add([shipmentId : shipmentId,
               shipmentItemSeqId : shipmentItem.shipmentItemSeqId,
               returnId : returnItem.returnId,
               returnItemSeqId : returnItem.returnItemSeqId,
               orderId : returnItem.orderId,
               partyId : partyId,
               productId : productId,
               internalName : internalName,
               shipmentItemQty : shipmentItemQty,
               returnQuantity : returnQuantity,
               totalQtyIssued : totalQtyIssued,
               issuedItems : issuedItems,
               qtyStillNeedToBeIssued : qtyStillNeedToBeIssued,
               ]);
}
context.shipmentId = shipmentId;
context.items = items;
