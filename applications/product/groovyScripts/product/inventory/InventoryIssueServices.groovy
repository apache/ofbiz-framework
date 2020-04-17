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

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
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
            List orderItemList = delegator.getRelated("OrderItem", null, null, orderHeader, false)
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
                        Map callSvcMap = [:]
                        callSvcMap << orderItem
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
                logInfo("Not issuing inventory for orderId ${orderHeader.orderId}, no inventory information available.")
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
    GenericValue orderItem
    GenericValue lastNonSerInventoryItem
    if (!parameters.orderItem) {
        orderItem = from("OrderItem").where(parameters).queryOne()
    } else {
        orderItem = parameters.orderItem
    }
    // kind of like the inventory reservation routine (with a few variations...), find InventoryItems to issue from, but instead of doing the reservation just create an issuance and an inventory item detail for the change
    if (orderItem.productId) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        // NOTE: the inventory will be issued from the OrderHeader.originFacilityId
        GenericValue orderHeader
        if (!parameters.orderHeader) {
            orderHeader = from("OrderHeader").where(parameters).queryOne()
        } else {
            orderHeader = parameters.orderHeader
        }
        // get the ProductStore to fund the reserveOrderEnumId
        GenericValue productStore
        if (!parameters.productStore) {
            productStore = from("ProductStore").where(productStoreId: orderHeader.productStoreId).queryOne()
        } else {
            productStore = parameters.productStore
        }
        // before we do the find, put together the orderBy list based on which reserveOrderEnumId is specified
        String orderByString
        if ("INVRO_FIFO_EXP" == productStore.reserveOrderEnumId) {
            orderByString = "+expireDate"
        } else if ("INVRO_LIFO_EXP" == productStore.reserveOrderEnumId) {
            orderByString = "-expireDate"
        } else if ("INVRO_LIFO_REC" == productStore.reserveOrderEnumId) {
            orderByString = "-datetimeReceived"
        } else {
            // the default reserveOrderEnumId is INVRO_FIFO_REC, ie FIFO based on date received
            orderByString = "+datetimeReceived"
        }
        List orderByList = [orderByString]
        Map lookupFieldMap = [productId: orderItem.productId, facilityId: orderHeader.originFacilityId]
        List inventoryItemList = from("InventoryItem").where(lookupFieldMap).orderBy(orderByList).queryList()
        parameters.quantityNotIssued = orderItem.quantity

        for (GenericValue inventoryItem : inventoryItemList) {
            // this is a little trick to get the InventoryItem value object without doing a query, possible since all fields on InventoryItem are also on InventoryItemAndLocation with the same names
            GenericValue tmpLastNonSerInventoryItem = issueImmediateForInventoryItemInline(inventoryItem)
            if (tmpLastNonSerInventoryItem) {
                lastNonSerInventoryItem = tmpLastNonSerInventoryItem
            }
        }
        // if quantityNotIssued is not 0, then pull it from the last non-serialized inventory item found, in the quantityNotIssued field
        if (parameters.quantityNotIssued != (BigDecimal) 0) {
            if (lastNonSerInventoryItem) {
                // create ItemIssuance record
                Map issuanceCreateMap = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, inventoryItemId: lastNonSerInventoryItem.inventoryItemId, quantity: parameters.quantityNotIssued]
                Map serviceResult = run service: "createItemIssuance", with: issuanceCreateMap
                String itemIssuanceId = serviceResult.itemIssuanceId

                // subtract from quantityNotIssued from the availableToPromise and quantityOnHand of existing inventory item
                // instead of updating InventoryItem, add an InventoryItemDetail
                Map createDetailMap = [inventoryItemId: lastNonSerInventoryItem.inventoryItemId, orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, itemIssuanceId: itemIssuanceId]
                BigDecimal availableToPromiseDiff = parameters.quantityNotIssued * -1
                createDetailMap.availableToPromiseDiff = availableToPromiseDiff.setScale(6)
                BigDecimal quantityOnHandDiff = parameters.quantityNotIssued * -1
                createDetailMap.quantityOnHandDiff = quantityOnHandDiff.setScale(6)
                run service: "createInventoryItemDetail", with: createDetailMap
            } else {
                // no non-ser inv item, create a non-ser InventoryItem with availableToPromise = -quantityNotIssued
                Map createInvItemInMap = [productId: orderItem.productId, facilityId: orderHeader.originFacilityId, inventoryItemTypeId: "NON_SERIAL_INV_ITEM"]
                Map serviceResult = run service: "createInventoryItem", with: createInvItemInMap
                Map createInvItemOutMap = [inventoryItemId: serviceResult.inventoryItemId]

                // create ItemIssuance record
                Map issuanceCreateMap = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, inventoryItemId: createInvItemOutMap.inventoryItemId, quantity: parameters.quantityNotIssued]
                Map serviceResultCII = run service: "createItemIssuance", with: issuanceCreateMap
                String itemIssuanceId = serviceResultCII.itemIssuanceId

                // also create a detail record with the quantities
                Map createDetailMap = [inventoryItemId: createInvItemOutMap.inventoryItemId, orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, itemIssuanceId: itemIssuanceId]
                BigDecimal availableToPromiseDiff = parameters.quantityNotIssued * -1
                createDetailMap.availableToPromiseDiff = availableToPromiseDiff.setScale(6)
                BigDecimal quantityOnHandDiff = parameters.quantityNotIssued * -1
                createDetailMap.quantityOnHandDiff = quantityOnHandDiff.setScale(6)
                run service: "createInventoryItemDetail", with: createDetailMap
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
    Map issuanceCreateMap
    // only do something with this inventoryItem if there is more inventory to issue
    if (parameters.quantityNotIssued > (BigDecimal) 0) {
        if ("SERIALIZED_INV_ITEM" == inventoryItem.inventoryItemTypeId) {
            if ("INV_AVAILABLE" == inventoryItem.statusId) {
                // change status on inventoryItem
                inventoryItem.statusId = "INV_DELIVERED"
                Map inventoryItemMap = [:]
                inventoryItemMap << inventoryItem
                run service: "updateInventoryItem", with: inventoryItemMap

                // create ItemIssuance record
                issuanceCreateMap = [orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, inventoryItemId: inventoryItem.inventoryItemId, quantity: (BigDecimal) 1]
                run service: "createItemIssuance", with: issuanceCreateMap

                parameters.quantityNotIssued -= (BigDecimal) 1
            }
        }
        if (inventoryItem.inventoryItemTypeId == "NON_SERIAL_INV_ITEM") {
            // reduce atp on inventoryItem if availableToPromise greater than 0, if not the code at the end of this method will handle it
            if (((!inventoryItem.statusId) || (inventoryItem.statusId == "INV_AVAILABLE"))
            && (inventoryItem.availableToPromiseTotal)
            && (inventoryItem.availableToPromiseTotal > (BigDecimal) 0)) {
                if (parameters.quantityNotIssued > inventoryItem.availableToPromiseTotal) {
                    parameters.deductAmount = inventoryItem.availableToPromiseTotal
                } else {
                    parameters.deductAmount = parameters.quantityNotIssued
                }
                // create ItemIssuance record
                issuanceCreateMap = [orderId: parameters.orderId, orderitemSeqId: parameters.orderItemSeqId, inventoryItemId: inventoryItem.inventoryItemId, quantity: parameters.deductAmount]
                Map serviceResultCII = run service: "createItemIssuance", with: issuanceCreateMap
                String itemIssuanceId = serviceResultCII.itemIssuanceId

                // instead of updating InventoryItem, add an InventoryItemDetail
                Map createDetailMap = [inventoryItemId: inventoryItem.inventoryItemId, orderId: parameters.orderId, orderItemSeqId: parameters.orderItemSeqId, itemIssuanceId: itemIssuanceId]
                // update availableToPromiseDiff AND quantityOnHandDiff since this is an issuance
                BigDecimal availableToPromiseDiff = parameters.deductAmount * -1
                createDetailMap.availableToPromiseDiff = availableToPromiseDiff.setScale(6)
                BigDecimal quantityOnHandDiff = parameters.deductAmount * -1
                createDetailMap.quantityOnHandDiff = quantityOnHandDiff.setScale(6)
                run service: "createInventoryItemDetail", with: createDetailMap

                parameters.quantityNotIssued -= parameters.deductAmount

                // keep track of the last non-serialized inventory item for use if inventory is not sufficient for amount already issued
                // use env variable named lastNonSerInventoryItem
                lastNonSerInventoryItem = inventoryItem
            }
        }
    }
    return lastNonSerInventoryItem
}
