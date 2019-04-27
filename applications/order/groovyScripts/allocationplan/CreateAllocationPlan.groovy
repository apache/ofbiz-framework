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

import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.party.party.PartyHelper

allocationPlanInfo = [:]
itemList = []
isPlanAlreadyExists = false
productId = parameters.productId
planName = parameters.planName

if (productId) {
    orderedQuantityTotal = 0.0
    orderedValueTotal = 0.0
    reservedQuantityTotal = 0.0

    ecl = EntityCondition.makeCondition([
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                            EntityCondition.makeCondition("statusId", EntityOperator.IN, ["ALLOC_PLAN_CREATED", "ALLOC_PLAN_APPROVED"]),
                            EntityCondition.makeCondition("planTypeId", EntityOperator.EQUALS, "SALES_ORD_ALLOCATION")],
                        EntityOperator.AND)
    allocationPlanHeader = from("AllocationPlanHeader").where(ecl).queryFirst()
    if (allocationPlanHeader == null) {
        ecl = EntityCondition.makeCondition([
                                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                EntityCondition.makeCondition("orderStatusId", EntityOperator.EQUALS, "ORDER_APPROVED"),
                                EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                            EntityOperator.AND)
        orderAndItemList = from("OrderHeaderAndItems").where(ecl).queryList()
        orderAndItemList.each { orderAndItem ->
            itemMap = [:]
            salesChannelEnumId = orderAndItem.salesChannelEnumId
            itemMap.salesChannelEnumId = salesChannelEnumId
            salesChannel = from("Enumeration").where("enumId", salesChannelEnumId).queryOne()
            if (salesChannel) {
                itemMap.salesChannel = salesChannel.description
            }

            orh = new OrderReadHelper(delegator, orderAndItem.orderId)
            placingParty = orh.getPlacingParty()
            if (placingParty != null) {
                itemMap.partyId = placingParty.partyId
                itemMap.partyName = PartyHelper.getPartyName(placingParty)
            }

            itemMap.orderId = orderAndItem.orderId
            itemMap.orderItemSeqId = orderAndItem.orderItemSeqId
            itemMap.estimatedShipDate = orderAndItem.estimatedShipDate

            unitPrice = orderAndItem.unitPrice
            cancelQuantity = orderAndItem.cancelQuantity
            quantity = orderAndItem.quantity
            if (cancelQuantity != null) {
                orderedQuantity = quantity.subtract(cancelQuantity)
            } else {
                orderedQuantity = quantity
            }
            orderedValue = orderedQuantity.multiply(unitPrice)
            orderedQuantityTotal = orderedQuantityTotal.add(orderedQuantity)
            orderedValueTotal = orderedValueTotal.add(orderedValue)
            itemMap.orderedQuantity = orderedQuantity
            itemMap.orderedValue = orderedValue

            // Reserved quantity
            reservedQuantity = 0.0
            reservations = from("OrderItemShipGrpInvRes").where("orderId", orderAndItem.orderId, "orderItemSeqId", orderAndItem.orderItemSeqId).queryList()
            reservations.each { reservation ->
                if (reservation.quantity) {
                    reservedQuantity += reservation.quantity
                }
            }
            reservedQuantityTotal = reservedQuantityTotal.add(reservedQuantity)
            itemMap.reservedQuantity = reservedQuantity
            itemList.add(itemMap)
        }
    } else {
        isPlanAlreadyExists = true
    }
    allocationPlanInfo.orderedQuantityTotal = orderedQuantityTotal
    allocationPlanInfo.orderedValueTotal = orderedValueTotal
    allocationPlanInfo.reservedQuantityTotal = reservedQuantityTotal
}
allocationPlanInfo.isPlanAlreadyExists = isPlanAlreadyExists
allocationPlanInfo.itemList = itemList
context.allocationPlanInfo = allocationPlanInfo