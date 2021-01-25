/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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

import org.apache.ofbiz.entity.GenericValue

/**
 * Computes the total quantity assigned to shipment for a purchase order item
 * @return
 */
def getTotalIssuedQuantityForOrderItem() {
    Map result = success()
    GenericValue orderItem = parameters.orderItem
    BigDecimal totalIssuedQuantity = 0.0
    List orderShipments = from("OrderShipment").where(orderId: orderItem.orderId, orderItemSeqId: orderItem.orderItemSeqId).queryList()
    if (orderShipments) {
        for (GenericValue orderShipment : orderShipments) {
            totalIssuedQuantity +=  orderShipment.quantity
        }
    } else {
        // This is here for backward compatibility only: ItemIssuances are no more created for purchase orders
        List allItemIssuances = from("ItemIssuance").where(orderId: orderItem.orderId, orderItemSeqId: orderItem.orderItemSeqId).queryList()
        for (GenericValue itemIssuance : allItemIssuances) {
            totalIssuedQuantity += itemIssuance.quantity
        }
    }
    result.totalIssuedQuantity = totalIssuedQuantity
    return result
}
