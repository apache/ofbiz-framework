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

import org.apache.ofbiz.base.util.ObjectType
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

cycleCountMap = [:]
facilityId = parameters.facilityId
showCreateSession = "N"
if (parameters.statusIds && parameters.statusIds.contains("INV_COUNT_REJECTED")) {
    showCreateSession = "Y"
}

inventoryCountId = parameters.inventoryCountId

exprBldr = new EntityConditionBuilder()
cond = exprBldr.AND() {
    if (inventoryCountId)
        LIKE(inventoryCountId: "%" + inventoryCountId.toUpperCase() + "%")
    if (facilityId)
        EQUALS(facilityId: facilityId)
    if (parameters.statusIds)
        IN(statusId: ObjectType.simpleTypeOrObjectConvert(parameters.statusIds, "List", null, null))
    else
        EQUALS(statusId: "INV_COUNT_APPROVED")
    if (parameters.locationSeqId)
        LIKE(locationSeqId: parameters.locationSeqId)
}

inventoryCountAndItems = from("InventoryCountAndItems").where(cond).orderBy("-createdDate").queryList()

inventoryCountAndItems.each { inventoryCountAndItem ->
    cycleCount = [:]
    inventoryCountId = inventoryCountAndItem?.inventoryCountId
    cycleCount.inventoryCountId = inventoryCountAndItem?.inventoryCountId
    cycleCount.facilityId = inventoryCountAndItem?.facilityId
    facility = from("Facility").where("facilityId", inventoryCountAndItem?.facilityId).queryOne()
    cycleCount.facilityName = facility?.facilityName
    statusItem = from("StatusItem").where("statusId", inventoryCountAndItem?.statusId).queryOne()
    cycleCount.statusId = inventoryCountAndItem?.statusId
    cycleCount.statusDescription = statusItem.description
    if (inventoryCountAndItem?.inventoryItemId) {
        if (cycleCountMap && cycleCountMap.containsKey(inventoryCountId)) {
            oldCycleCountMap = cycleCountMap.get(inventoryCountId)
            totalInventoryItems = oldCycleCountMap.totalInventoryItems + 1
            cycleCount.totalInventoryItems = totalInventoryItems
        } else {
            totalInventoryItems = 1
            cycleCount.totalInventoryItems = totalInventoryItems
        }
    } else {
        if (cycleCountMap && cycleCountMap.containsKey(inventoryCountId)) {
            oldCycleCountMap = cycleCountMap.get(inventoryCountId)
            totalInventoryItems = oldCycleCountMap.totalInventoryItems + 0
            cycleCount.totalInventoryItems = totalInventoryItems
        } else {
            totalInventoryItems = 0
            cycleCount.totalInventoryItems = totalInventoryItems
        }
    }

    if (inventoryCountAndItem.locationSeqId) {
        if (cycleCountMap && cycleCountMap.containsKey(inventoryCountId)) {
            oldCycleCountMap = cycleCountMap.get(inventoryCountId)
            locationSeqIds = [] as Set
            locationSeqIds.addAll(oldCycleCountMap.locationSeqIds)
            locationSeqIds.add(inventoryCountAndItem.locationSeqId)
            cycleCount.locationSeqIds = locationSeqIds
        } else {
            locationSeqIds = [] as Set
            locationSeqIds.add(inventoryCountAndItem.locationSeqId)
            cycleCount.locationSeqIds = locationSeqIds
        }
    } else {
        cycleCount.locationSeqIds = null
    }
    cycleCountMap.put(inventoryCountId, cycleCount)
}

context.showCreateSession = showCreateSession
context.facilityId = facilityId
context.statusIds = parameters.statusIds
context.inventoryCountId = inventoryCountId
context.cycleCountMap = cycleCountMap