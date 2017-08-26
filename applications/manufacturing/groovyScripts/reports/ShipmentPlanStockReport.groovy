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


inventoryStock = [:]
shipmentId = parameters.shipmentId
shipment = from("Shipment").where("shipmentId", shipmentId).queryOne()

context.shipmentIdPar = shipment.shipmentId
context.estimatedReadyDatePar = shipment.estimatedReadyDate
context.estimatedShipDatePar = shipment.estimatedShipDate
records = []
if (shipment) {
    shipmentPlans = from("OrderShipment").where("shipmentId", shipmentId).queryList()
    shipmentPlans.each { shipmentPlan ->
        orderLine = from("OrderItem").where("orderId", shipmentPlan.orderId , "orderItemSeqId", shipmentPlan.orderItemSeqId).queryOne()
        recordGroup = [:]
        recordGroup.ORDER_ID = shipmentPlan.orderId
        recordGroup.ORDER_ITEM_SEQ_ID = shipmentPlan.orderItemSeqId
        recordGroup.SHIPMENT_ID = shipmentPlan.shipmentId
        recordGroup.SHIPMENT_ITEM_SEQ_ID = shipmentPlan.shipmentItemSeqId

        recordGroup.PRODUCT_ID = orderLine.productId
        recordGroup.QUANTITY = shipmentPlan.quantity
        product = from("Product").where("productId", orderLine.productId).queryOne()
        recordGroup.PRODUCT_NAME = product.internalName

        inputPar = [productId : orderLine.productId,
                                     quantity : shipmentPlan.quantity,
                                     fromDate : "" + new Date(),
                                     userLogin: userLogin]

        result = [:]
        result = runService('getNotAssembledComponents',inputPar)
        if (result) {
            components = (List)result.get("notAssembledComponents")
        }
        components.each { oneComponent ->
            record = new HashMap(recordGroup)
            record.componentId = oneComponent.getProduct().productId
            record.componentName = oneComponent.getProduct().internalName
            record.componentQuantity = new Float(oneComponent.getQuantity())
            facilityId = shipment.originFacilityId
            float qty = 0
            if (facilityId) {
                if (!inventoryStock.containsKey(oneComponent.getProduct().productId)) {
                    serviceInput = [productId : oneComponent.getProduct().productId , facilityId : facilityId]
                    serviceOutput = runService('getInventoryAvailableByFacility',serviceInput)
                    qha = serviceOutput.quantityOnHandTotal ?: 0.0
                    inventoryStock.put(oneComponent.getProduct().productId, qha)
                }
                qty = inventoryStock[oneComponent.getProduct().productId]
                qty = qty - oneComponent.getQuantity()
                inventoryStock.put(oneComponent.getProduct().productId, qty)
            }
            record.componentOnHand = qty
            // Now we get the products qty already reserved by production runs
            serviceInput = [productId : oneComponent.getProduct().productId,
                                          userLogin : userLogin]
            serviceOutput = runService('getProductionRunTotResQty', serviceInput)
            resQty = serviceOutput.reservedQuantity
            record.reservedQuantity = resQty
            records.add(record)
        }
    }
    context.records = records

    // check permission
    hasPermission = false
    if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
        hasPermission = true
    }
    context.hasPermission = hasPermission
}

return "success"
