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
import java.math.RoundingMode

import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.party.party.PartyHelper

planId = parameters.planId
allocationPlanInfo = [:]
allocationPlanHeader = from("AllocationPlanHeader").where("planId", planId).queryFirst()
if (allocationPlanHeader) {
    allocationPlanInfo.planId = planId
    allocationPlanInfo.planName = allocationPlanHeader.planName
    allocationPlanInfo.statusId = allocationPlanHeader.statusId
    allocationPlanInfo.productId = allocationPlanHeader.productId
    allocationPlanInfo.createdBy = allocationPlanHeader.createdByUserLogin
    allocationPlanInfo.createdDate = allocationPlanHeader.createdStamp

    //Get product information
    product = from("Product").where("productId", allocationPlanHeader.productId).queryOne()
    if (product) {
        allocationPlanInfo.productName = product.internalName
    }

    // Inventory quantity summary by facility: For every warehouse the product's ATP and QOH
    // are obtained (calling the "getInventoryAvailableByFacility" service)
    totalATP = 0
    totalQOH = 0
    facilityList = from("ProductFacility").where("productId", allocationPlanHeader.productId).queryList()
    facilityIterator = facilityList.iterator()
    while (facilityIterator) {
        facility = facilityIterator.next()
        result = runService('getInventoryAvailableByFacility', [productId : allocationPlanHeader.productId, facilityId : facility.facilityId])
        totalATP = totalATP + result.availableToPromiseTotal
        totalQOH = totalQOH + result.quantityOnHandTotal
    }
    allocationPlanInfo.totalATP = totalATP
    allocationPlanInfo.totalQOH = totalQOH

    summaryMap = [:]
    itemList = []
    orderedQuantityTotal = 0.0
    orderedValueTotal = 0.0
    reservedQuantityTotal = 0.0
    allocatedQuantityTotal = 0.0
    allocatedValueTotal = 0.0

    allocationPlanItems = from("AllocationPlanAndItem").where("planId", planId, "productId", allocationPlanInfo.productId).orderBy("prioritySeqId").queryList()
    allocationPlanItems.each { allocationPlanItem ->
        newSummaryMap = [:]
        itemMap = [:]
        orderId = allocationPlanItem.orderId
        orderItemSeqId = allocationPlanItem.orderItemSeqId
        itemMap.orderId = allocationPlanItem.orderId
        itemMap.orderItemSeqId = allocationPlanItem.orderItemSeqId
        itemMap.planId = allocationPlanItem.planId
        itemMap.planItemSeqId = allocationPlanItem.planItemSeqId
        itemMap.productId = allocationPlanItem.productId
        itemMap.statusId = allocationPlanItem.planItemStatusId

        orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
        if (orderHeader) {
            salesChannelEnumId = orderHeader.salesChannelEnumId
            salesChannel = from("Enumeration").where("enumId", salesChannelEnumId).queryOne()
            if (salesChannel) {
                itemMap.salesChannel = salesChannel.description
                newSummaryMap.salesChannel = salesChannel.description
            }
            orh = new OrderReadHelper(delegator, orderId)
            placingParty = orh.getPlacingParty()
            if (placingParty != null) {
                itemMap.partyId = placingParty.partyId
                itemMap.partyName = PartyHelper.getPartyName(placingParty)
            }

            orderItem = from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne()
            unitPrice = 0
            orderedQuantity = 0
            if (orderItem) {
                unitPrice = orderItem.unitPrice
                cancelQuantity = orderItem.cancelQuantity
                quantity = orderItem.quantity
                if (cancelQuantity != null) {
                    orderedQuantity = quantity.subtract(cancelQuantity)
                } else {
                    orderedQuantity = quantity
                }
                orderedValue = orderedQuantity.multiply(unitPrice);
                orderedQuantityTotal = orderedQuantityTotal.add(orderedQuantity)
                orderedValueTotal = orderedValueTotal.add(orderedValue)
                itemMap.orderedQuantity = orderedQuantity
                itemMap.orderedValue = orderedValue
                newSummaryMap.orderedQuantity = orderedQuantity
                newSummaryMap.orderedValue = orderedValue

                // Reserved quantity
                reservedQuantity = 0
                reservations = orderItem.getRelated("OrderItemShipGrpInvRes", null, null, false)
                reservations.each { reservation ->
                    quantityAvailable = reservation.quantity?reservation.quantity:0.0
                    quantityNotAvailable = reservation.quantityNotAvailable?reservation.quantityNotAvailable:0.0
                    reservedQuantity += (quantityAvailable - quantityNotAvailable)
                }
                reservedQuantityTotal = reservedQuantityTotal.add(reservedQuantity)
                itemMap.reservedQuantity = reservedQuantity

                //TODO: Estimated Ship Date, need to check the right way to get it
                itemMap.estimatedShipDate = orderItem.estimatedShipDate
            }
            allocatedQuantity = allocationPlanItem.allocatedQuantity
            if (allocatedQuantity) {
                allocatedValue = allocatedQuantity.multiply(unitPrice);
                allocatedQuantityTotal = allocatedQuantityTotal.add(allocatedQuantity)
                allocatedValueTotal = allocatedValueTotal.add(allocatedValue)
                itemMap.allocatedQuantity = allocatedQuantity
                itemMap.allocatedValue = allocatedValue
                newSummaryMap.allocatedQuantity = allocatedQuantity
                newSummaryMap.allocatedValue = allocatedValue
            }

            allocationPercentage = 0.0
            if (allocatedQuantity && allocatedQuantity != 0 && orderedQuantity != 0) {
                allocationPercentage = (allocatedQuantity.divide(orderedQuantity, 2, RoundingMode.HALF_UP)).multiply(100)
            }
            itemMap.allocationPercentage = allocationPercentage
            newSummaryMap.allocationPercentage = allocationPercentage

            if (summaryMap.containsKey(salesChannelEnumId)) {
                existingSummaryMap = summaryMap.get(salesChannelEnumId)
                existingSummaryMap.orderedQuantity += newSummaryMap.orderedQuantity
                existingSummaryMap.orderedValue += newSummaryMap.orderedValue
                if (existingSummaryMap.allocatedQuantity) {
                    if (!newSummaryMap.allocatedQuantity) {
                        newSummaryMap.allocatedQuantity = 0
                    }
                    existingSummaryMap.allocatedQuantity += newSummaryMap.allocatedQuantity
                } else {
                    existingSummaryMap.allocatedQuantity = newSummaryMap.allocatedQuantity
                }
                if (existingSummaryMap.allocatedValue) {
                    if (!newSummaryMap.allocatedValue) {
                        newSummaryMap.allocatedValue = 0
                    }
                    existingSummaryMap.allocatedValue += newSummaryMap.allocatedValue
                } else {
                    existingSummaryMap.allocatedValue = newSummaryMap.allocatedValue
                }
                allocationPercentage = 0.0
                if (existingSummaryMap.orderedQuantity && existingSummaryMap.orderedQuantity != 0 && existingSummaryMap.allocatedQuantity && existingSummaryMap.allocatedQuantity != 0) {
                    allocationPercentage = (existingSummaryMap.allocatedQuantity.divide(existingSummaryMap.orderedQuantity, 2, RoundingMode.HALF_UP)).multiply(100)
                }
                existingSummaryMap.allocationPercentage = allocationPercentage
                summaryMap.put(salesChannelEnumId, existingSummaryMap)
            } else {
                summaryMap.put(salesChannelEnumId, newSummaryMap)
            }
        }
        itemList.add(itemMap)
    }
    allocationPercentageTotal = 0.0
    if (orderedQuantityTotal != 0.0 && allocatedQuantityTotal != 0.0) {
        allocationPercentageTotal = (allocatedQuantityTotal.divide(orderedQuantityTotal, 2, RoundingMode.HALF_UP)).multiply(100)
    }
    allocationPlanInfo.orderedQuantityTotal = orderedQuantityTotal
    allocationPlanInfo.orderedValueTotal = orderedValueTotal
    allocationPlanInfo.reservedQuantityTotal = reservedQuantityTotal
    allocationPlanInfo.allocatedQuantityTotal = allocatedQuantityTotal
    allocationPlanInfo.allocatedValueTotal = allocatedValueTotal
    allocationPlanInfo.allocationPercentageTotal = allocationPercentageTotal
    allocationPlanInfo.summaryMap = summaryMap
    allocationPlanInfo.itemList = itemList
}
allocationPlanInfo.allocationPlanHeader = allocationPlanHeader
context.allocationPlanInfo = allocationPlanInfo
