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
planId = parameters.planId
planName = parameters.planName
planTypeId = parameters.planTypeId
statusId = parameters.statusId
productId = parameters.productId
orderId = parameters.orderId
sortField = parameters.sortField

List exprs = []
if (planId) {
    exprs.add(EntityCondition.makeCondition("planId", EntityOperator.EQUALS, planId))
}
if (planName) {
    exprs.add(EntityCondition.makeCondition("planName", EntityOperator.LIKE, planName))
}
if (planTypeId) {
    exprs.add(EntityCondition.makeCondition("planTypeId", EntityOperator.EQUALS, planTypeId))
}
if (statusId) {
    if (statusId instanceof String) {
        exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId))
    } else {
        exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, statusId))
    }
}
if (productId) {
    exprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId))
}
if (orderId) {
    exprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId))
}
ecl = EntityCondition.makeCondition(exprs, EntityOperator.OR)

if (sortField) {
    allocationPlanItems = from("AllocationPlanAndItem").where(ecl).orderBy(sortField).queryList()
} else {
    allocationPlanItems = from("AllocationPlanAndItem").where(ecl).queryList()
}

allocationPlans = []
allocationPlanItems.each { allocationPlanItem ->
    allocationPlanMap = [:]
    allocationPlanMap.planId = allocationPlanItem.planId
    allocationPlanMap.planItemSeqId = allocationPlanItem.planItemSeqId
    allocationPlanMap.planName = allocationPlanItem.planName
    allocationPlanMap.statusId = allocationPlanItem.statusId
    allocationPlanMap.planTypeId = allocationPlanItem.planTypeId
    allocationPlanMap.productId = allocationPlanItem.productId
    allocationPlanMap.orderId = allocationPlanItem.orderId
    allocationPlanMap.orderItemSeqId = allocationPlanItem.orderItemSeqId
    allocationPlanMap.planMethodEnumId = allocationPlanItem.planMethodEnumId
    allocationPlanMap.allocatedQuantity = allocationPlanItem.allocatedQuantity
    allocationPlans.add(allocationPlanMap)
}
context.allocationPlans = allocationPlans