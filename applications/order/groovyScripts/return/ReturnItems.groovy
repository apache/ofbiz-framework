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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.util.EntityFindOptions
import org.apache.ofbiz.order.order.OrderReadHelper
import java.math.BigDecimal

returnId = parameters.returnId
if (!returnId) return
context.returnId = returnId

orderId = parameters.orderId
context.orderId = orderId

returnHeader = from("ReturnHeader").where("returnId", returnId).queryOne()
context.returnHeader = returnHeader

returnHeaderTypeId = returnHeader.returnHeaderTypeId
context.toPartyId = returnHeader.toPartyId

returnItems = from("ReturnItem").where("returnId", returnId).queryList()
context.returnItems = returnItems

// these are just the adjustments not associated directly with a return item--the rest are gotten with a .getRelated on the returnItems in the .FTL
returnAdjustments = from("ReturnAdjustment").where("returnId", returnId, "returnItemSeqId", "_NA_").orderBy("returnItemSeqId", "returnAdjustmentTypeId").queryList()
context.returnAdjustments = returnAdjustments

returnTypes = from("ReturnType").orderBy("sequenceId").queryList()
context.returnTypes = returnTypes

itemStatus = from("StatusItem").where("statusTypeId", "INV_SERIALIZED_STTS").orderBy("statusId", "description").queryList()
context.itemStatus = itemStatus

returnReasons = from("ReturnReason").orderBy("sequenceId").queryList()
context.returnReasons = returnReasons

itemStts = from("StatusItem").where("statusTypeId", "INV_SERIALIZED_STTS").orderBy("sequenceId").queryList()
context.itemStts = itemStts

returnItemTypeMap = from("ReturnItemTypeMap").where("returnHeaderTypeId", returnHeaderTypeId).queryList()
typeMap = [:]
returnItemTypeMap.each { value ->
    typeMap[value.returnItemMapKey] = value.returnItemTypeId
}
context.returnItemTypeMap = typeMap

if (orderId) {
    order = from("OrderHeader").where("orderId", orderId).queryOne()
    returnRes = runService('getReturnableItems', [orderId : orderId])
    context.returnableItems = returnRes.returnableItems

    orh = new OrderReadHelper(order)
    context.orh = orh
    context.orderHeaderAdjustments = orh.getAvailableOrderHeaderAdjustments()

    // get the order shipping amount
    shipRes = runService('getOrderShippingAmount', [orderId : orderId])
    shippingAmount = shipRes.shippingAmount
    context.shippingAmount = shippingAmount
}
roleTypeId = "PLACING_CUSTOMER"
partyId = returnHeader.fromPartyId
if (returnHeaderTypeId == "VENDOR_RETURN") {
    roleTypeId = "BILL_FROM_VENDOR"
    partyId = returnHeader.toPartyId
}
partyOrders = select("orderId","orderDate").from("OrderHeaderItemAndRoles").where("roleTypeId", roleTypeId, "partyId", partyId, "orderItemStatusId", "ITEM_COMPLETED").orderBy("orderId").distinct().queryList()
context.partyOrders = partyOrders
context.partyId = partyId

// get the list of return shipments associated to the return
returnShipmentIds = select("shipmentId").from("ReturnItemShipment").where("returnId", returnId).distinct().cache(true).queryList()
context.returnShipmentIds = returnShipmentIds
