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

import org.apache.ofbiz.entity.GenericValue


/**
 * Issues the Inventory for an Order that was Immediately Fulfilled
 * @return
 */
def issueImmediatelyFulfilledOrder() {
    GenericValue orderHeader = from("OrderHeader").where(parameters).queryOne()

    if (orderHeader) {
        GenericValue productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()

        if (orderHeader.needsInventoryIssuance == "Y") {
            List orderItemList = orderHeader.getRelated("OrderItem")
            /*
             * before issuing inventory, check to see if there is inventory information in this database
             * if inventory info is not available for all of the products, then don't do the issuance,
             * ie there has to be at least SOME inventory info in the database to facilitate inventory-less cases
             */
            long iiCount = from("InventoryItem").where(facilityId: orderHeader.originFacilityId).queryCount()

            // now go through each order item and call a service to issue the inventory
            if (iiCount > 0l) {
                for (GenericValue orderItem : orderItemList) {
                    if (orderItem.productId) {
                        Map callSvcMap = orderItem.getAllFields()
                        callSvcMap.orderHeader = orderHeader
                        callSvcMap.orderItem = orderItem
                        callSvcMap.productStore = productStore
                        run service: "issueImmediatelyFulfilledOrderItem", with: callSvcMap
                    }
                }
                // now that the issuance is done, set the needsInventoryIssuance=N
                orderHeader.needsInventoryIssuance = "N"
                orderHeader.store()
                logInfo("Issued inventory for orderId ${orderHeader.orderId}.")
            } else {
                logInfo("Not issuing inventory for orderId ${orderHeader.orderId}," +
                        " no inventory information available.")
            }
        }
    }
    return success()
}

/**
 * Issues the Inventory for an Order Item that was Immediately Fulfilled
 * @return
 */
def issueImmediatelyFulfilledOrderItem() {
    GenericValue lastNonSerInventoryItem
    GenericValue orderItem = parameters.orderItem ?:
            from("OrderItem").where(parameters).queryOne()

    // kind of like the inventory reservation routine (with a few variations...), find InventoryItems to issue from,
    // but instead of doing the reservation just create an issuance and an inventory item detail for the change
    if (orderItem.productId) {
        // NOTE: the inventory will be issued from the OrderHeader.originFacilityId
        GenericValue orderHeader = parameters.orderHeader ?:
                from("OrderHeader").where(parameters).queryOne()

        // get the ProductStore to fund the reserveOrderEnumId
        GenericValue productStore = parameters.productStore ?:
                from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()

        // before we do the find, put together the orderBy list based on which reserveOrderEnumId is specified
        String orderBy = "+datetimeReceived"
        switch (productStore.reserveOrderEnumId) {
            case "INVRO_FIFO_EXP":
                orderBy = "+expireDate"
                break
            case "INVRO_LIFO_EXP":
                orderBy = "-expireDate"
                break
            case "INVRO_LIFO_REC":
                orderBy = "-datetimeReceived"
                break
            default:
                break
        }
        Map lookupFieldMap = [productId: orderItem.productId,
                              facilityId: orderHeader.originFacilityId]
        from("InventoryItem")
                .where(lookupFieldMap)
                .orderBy(orderBy)
                .queryList()
                .each { inventoryItem ->
                    // this is a little trick to get the InventoryItem value object without doing a query, possible
                    // since all fields on InventoryItem are also on InventoryItemAndLocation with the same names
                    GenericValue tmpLastNonSerInventoryItem = issueImmediateForInventoryItemInline(inventoryItem)
                    if (tmpLastNonSerInventoryItem) {
                        lastNonSerInventoryItem = tmpLastNonSerInventoryItem
                    }
                }

        parameters.quantityNotIssued = orderItem.quantity
        // if quantityNotIssued is not 0, then pull it from the last non-serialized inventory item found,
        // in the quantityNotIssued field
        if (parameters.quantityNotIssued != (BigDecimal.ZERO)) {
            BigDecimal availableToPromiseDiff = - parameters.quantityNotIssued
            BigDecimal quantityOnHandDiff = - parameters.quantityNotIssued
            if (lastNonSerInventoryItem) {
                // create ItemIssuance record
                Map serviceResult = run service: "createItemIssuance",
                        with: [orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId,
                               inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                               quantity: parameters.quantityNotIssued]
                String itemIssuanceId = serviceResult.itemIssuanceId

                // subtract from quantityNotIssued from the availableToPromise and quantityOnHand of existing inventory item
                // instead of updating InventoryItem, add an InventoryItemDetail
                run service: "createInventoryItemDetail",
                        with: [inventoryItemId: lastNonSerInventoryItem.inventoryItemId,
                               orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId,
                               itemIssuanceId: itemIssuanceId,
                               availableToPromiseDiff: availableToPromiseDiff.setScale(6),
                               quantityOnHandDiff: quantityOnHandDiff.setScale(6)]
            } else {
                // no non-ser inv item, create a non-ser InventoryItem with availableToPromise = -quantityNotIssued
                Map serviceResult = run service: "createInventoryItem",
                        with: [productId: orderItem.productId,
                               facilityId: orderHeader.originFacilityId,
                               inventoryItemTypeId: "NON_SERIAL_INV_ITEM"]
                String newInventoryItemId = serviceResult.inventoryItemId

                // create ItemIssuance record
                serviceResult = run service: "createItemIssuance",
                        with: [inventoryItemId: newInventoryItemId,
                               orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId,
                               quantity: parameters.quantityNotIssued]
                String itemIssuanceId = serviceResult.itemIssuanceId

                // also create a detail record with the quantities
                run service: "createInventoryItemDetail",
                        with: [inventoryItemId: newInventoryItemId,
                               orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId,
                               itemIssuanceId: itemIssuanceId,
                               availableToPromiseDiff: availableToPromiseDiff.setScale(6),
                               quantityOnHandDiff: quantityOnHandDiff.setScale(6)]
            }
            parameters.quantityNotIssued = 0
        }
    }
    return success()
}

/**
 * Does a issuance for one InventoryItem, meant to be called in-line
 * @return
 */
def issueImmediateForInventoryItemInline(GenericValue inventoryItem) {
    GenericValue lastNonSerInventoryItem
    // only do something with this inventoryItem if there is more inventory to issue
    if (parameters.quantityNotIssued > (BigDecimal.ZERO)) {
        if ("SERIALIZED_INV_ITEM" == inventoryItem.inventoryItemTypeId) {
            if ("INV_AVAILABLE" == inventoryItem.statusId) {
                // change status on inventoryItem
                inventoryItem.statusId = "INV_DELIVERED"
                run service: "updateInventoryItem", with: inventoryItem.getAllFields()

                // create ItemIssuance record
                run service: "createItemIssuance", with: [orderId: parameters.orderId,
                                                          orderItemSeqId: parameters.orderItemSeqId,
                                                          inventoryItemId: inventoryItem.inventoryItemId,
                                                          quantity: BigDecimal.ONE]

                parameters.quantityNotIssued -= BigDecimal.ONE
            }
        }
        if (inventoryItem.inventoryItemTypeId == "NON_SERIAL_INV_ITEM") {
            // reduce atp on inventoryItem if availableToPromise greater than 0,
            // if not the code at the end of this method will handle it
            if ((!inventoryItem.statusId || inventoryItem.statusId == "INV_AVAILABLE") &&
                    inventoryItem.availableToPromiseTotal &&
                    inventoryItem.availableToPromiseTotal > BigDecimal.ZERO) {
                parameters.deductAmount = parameters.quantityNotIssued > inventoryItem.availableToPromiseTotal ?
                        inventoryItem.availableToPromiseTotal :
                        parameters.quantityNotIssued

                // create ItemIssuance record
                Map serviceResult = run service: "createItemIssuance",
                        with: [orderId: parameters.orderId,
                               orderitemSeqId: parameters.orderItemSeqId,
                               inventoryItemId: inventoryItem.inventoryItemId,
                               quantity: parameters.deductAmount]
                String itemIssuanceId = serviceResult.itemIssuanceId

                // instead of updating InventoryItem, add an InventoryItemDetail
                // update availableToPromiseDiff AND quantityOnHandDiff since this is an issuance
                BigDecimal availableToPromiseDiff = - parameters.deductAmount
                BigDecimal quantityOnHandDiff = - parameters.deductAmount
                run service: "createInventoryItemDetail",
                        with: [inventoryItemId: inventoryItem.inventoryItemId,
                               orderId: parameters.orderId,
                               orderItemSeqId: parameters.orderItemSeqId,
                               itemIssuanceId: itemIssuanceId,
                               availableToPromiseDiff: availableToPromiseDiff.setScale(6),
                               quantityOnHandDiff: quantityOnHandDiff.setScale(6)]

                parameters.quantityNotIssued -= parameters.deductAmount

                // keep track of the last non-serialized inventory item for use if inventory is not sufficient for amount already issued
                // use env variable named lastNonSerInventoryItem
                lastNonSerInventoryItem = inventoryItem
            }
        }
    }
    return lastNonSerInventoryItem
}
