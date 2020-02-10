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

context.facilityIds = parameters.facilityIds;
context.statusIds = parameters.statusIds;
inventoryCountId = parameters.inventoryCountId;
context.inventoryCountId = inventoryCountId;
progressList = [];
inventoryCountItemsAndVariances = [];

if (parameters.searchResult && parameters.searchResult == "Y") {
    exprBldr = new EntityConditionBuilder();

    cond = exprBldr.AND() {
        if (parameters.facilityIds && "All" != parameters.facilityIds)
            IN("facilityId": ObjectType.simpleTypeOrObjectConvert(parameters.facilityIds, "java.util.List" , null, null))
        if (parameters.locationSeqId)
            LIKE("locationSeqId": parameters.locationSeqId)
        if (parameters.fromDate) {
            GREATER_THAN_EQUAL_TO("createdDate": ObjectType.simpleTypeOrObjectConvert(parameters.fromDate, "Timestamp", null, timeZone, locale, false));
        }
        if (parameters.toDate) {
            LESS_THAN_EQUAL_TO("createdDate": ObjectType.simpleTypeOrObjectConvert(parameters.toDate, "Timestamp", null, timeZone, locale, false));
        }
        IN("itemStatusId": ["INV_COUNT_COMPLETED"]);
    }

    inventoryCountItemsAndVarianceList = from("InventoryCountItemsAndVariance").where(cond).orderBy(["-createdDate"]).queryList();

    countProgressMap = [:];

    inventoryCountItemsAndVarianceList.each { inventoryCountItemsAndVariance ->
        cycleCount = [:]
        cycleCount.putAll(inventoryCountItemsAndVariance);

        inventoryCountId = inventoryCountItemsAndVariance.inventoryCountId;

        facility = from("Facility").where("facilityId", inventoryCountItemsAndVariance?.facilityId).queryOne();
        cycleCount.facilityName = facility?.facilityName;

        statusItem = from("StatusItem").where("statusId", inventoryCountItemsAndVariance?.statusId).queryOne();
        cycleCount.statusDescription = statusItem?.description;

        product = from("Product").where("productId", inventoryCountItemsAndVariance?.productId).queryOne();
        cycleCount.internalName = product?.internalName;
        cycleCount.partDescription = product?.description;

        unitCost = BigDecimal.ZERO;
        totalCost = BigDecimal.ZERO;
        actualCost = BigDecimal.ZERO;
        error = BigDecimal.ZERO;
        unitCost = inventoryCountItemsAndVariance?.unitCost;
        totalCost = inventoryCountItemsAndVariance?.totalCost;
        actualCost = inventoryCountItemsAndVariance?.actualCost;
        costVariance = inventoryCountItemsAndVariance?.costVariance;

        if (costVariance && totalCost != 0) {
            error = costVariance.divide(totalCost, BigDecimal.ROUND_HALF_UP);
            error = error * 100;
        }
        cycleCount.error = error?.setScale(2, BigDecimal.ROUND_HALF_UP);
        cycleCount.unitCost = unitCost?.setScale(2, BigDecimal.ROUND_HALF_UP);
        cycleCount.totalCost = totalCost?.setScale(2, BigDecimal.ROUND_HALF_UP);
        cycleCount.costVariance = costVariance?.setScale(2, BigDecimal.ROUND_HALF_UP);
        cycleCount.actualCost = actualCost?.setScale(2, BigDecimal.ROUND_HALF_UP);

        facilityLocation = from("FacilityLocation").where("facilityId", inventoryCountItemsAndVariance?.facilityId, "locationSeqId", inventoryCountItemsAndVariance?.locationSeqId).queryOne();
        cycleCount.areaId = facilityLocation?.areaId;
        cycleCount.aisleId = facilityLocation?.aisleId;
        cycleCount.sectionId = facilityLocation?.sectionId;
        cycleCount.levelId = facilityLocation?.levelId
        cycleCount.positionId = facilityLocation?.positionId

        description = "Cycle Count Session # " + inventoryCountId;
        inventoryItemDetail = from("InventoryItemDetail").where("inventoryItemId", inventoryCountItemsAndVariance?.inventoryItemId, "description", description).queryFirst()
        cycleCount.varianceCreatedOn = inventoryItemDetail?.effectiveDate

        facilityId = facility.facilityId;
        if (countProgressMap && countProgressMap.containsKey(facilityId)) {
            countProgressMap.each { key, value ->
                if (key == facilityId) {
                    countProgress = [:];
                    def locationSeqIds = [] as Set;
                    def inventoryItems = [] as Set;
                    locationSeqIds = value.locationSeqIds;
                    locationSeqIds.add(inventoryCountItemsAndVariance?.locationSeqId);
                    inventoryItems = value.inventoryItems;
                    inventoryItems.add(inventoryCountItemsAndVariance?.inventoryItemId);
                    countProgress.locationSeqIds = locationSeqIds;
                    countProgress.inventoryItems = inventoryItems;
                    countProgressMap.put(facilityId, countProgress);
                    inventoryCountItemsAndVariances.add(cycleCount);
                }
            }
        } else {
            countProgress = [:];
            def locationSeqIds = [] as Set;
            def inventoryItems = [] as Set;
            locationSeqIds.add(inventoryCountItemsAndVariance?.locationSeqId);
            inventoryItems.add(inventoryCountItemsAndVariance?.inventoryItemId);
            countProgress.locationSeqIds = locationSeqIds;
            countProgress.inventoryItems = inventoryItems;
            countProgressMap.put(facilityId, countProgress);
            inventoryCountItemsAndVariances.add(cycleCount);
        }
    }

    grandTotalLocationCompleted = BigDecimal.ZERO;
    grandTotalInventoryItemCompleted = BigDecimal.ZERO;
    grandTotalLocationCounted = BigDecimal.ZERO;
    grandTotalInventoryItemCounted = BigDecimal.ZERO;
    grandTotalPerLocation = BigDecimal.ZERO;
    grandTotalPerInventoryItem = BigDecimal.ZERO;

    countProgressMap.each { key, value ->
        progress = [:];
        facility = from("Facility").where("facilityId", key).queryOne();
        progress.facilityName = facility?.facilityName;
        progress.facilityId = key;
        totalCountedInventoryItems = BigDecimal.ZERO;
        countedLocations = BigDecimal.ZERO;
        totalInventoryItems = BigDecimal.ZERO;
        totalLocations = BigDecimal.ZERO;

        if (value.inventoryItems.size()) {
            totalCountedInventoryItems = new BigDecimal(value.inventoryItems.size());
            totalCountedInventoryItems = totalCountedInventoryItems.setScale(2,BigDecimal.ROUND_HALF_UP);
            grandTotalInventoryItemCounted = grandTotalInventoryItemCounted + totalCountedInventoryItems;
        }

        if (value.locationSeqIds.size()) {
            countedLocations = new BigDecimal(value.locationSeqIds.size());
            countedLocations = countedLocations.setScale(2,BigDecimal.ROUND_HALF_UP);
            grandTotalLocationCounted = grandTotalLocationCounted + countedLocations;
        }
        progress.totalCountedInventoryItems = totalCountedInventoryItems;
        progress.countedLocations = countedLocations;
        inventoryItemCondition = exprBldr.AND() {
            NOT_EQUAL("locationSeqId": null)
            NOT_EQUAL("quantityOnHandTotal": 0.0)
            EQUALS("facilityId": key)
        }

        inventoryItemCount = from("InventoryItem").where(inventoryItemCondition).queryCount();
        totalInventories = from("InventoryItem").where(inventoryItemCondition).queryCount();
        if (totalInventories) {
            totalInventoryItems = new BigDecimal(totalInventories);
            totalInventoryItems = totalInventoryItems.setScale(2, BigDecimal.ROUND_HALF_UP);
            grandTotalInventoryItemCompleted = grandTotalInventoryItemCompleted + totalInventoryItems;
        }
        progress.totalInventoryItems = totalInventoryItems;

        locations = from("FacilityLocation").where("facilityId", key).queryCount();
        if (locations) {
            totalLocations = new BigDecimal(locations);
            totalLocations = totalLocations.setScale(2, BigDecimal.ROUND_HALF_UP);
            grandTotalLocationCompleted = grandTotalLocationCompleted + totalLocations;
        }
        progress.totalLocations = totalLocations;

        percentLocationCompleted = BigDecimal.ZERO;
        percentInventoryItemCompleted = BigDecimal.ZERO;

        if (totalCountedInventoryItems && totalInventoryItems && totalInventoryItems != 0) {
            percentInventoryItemCompleted = (totalCountedInventoryItems/totalInventoryItems) * 100;
            percentInventoryItemCompleted = percentInventoryItemCompleted.setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        if (countedLocations && totalLocations && totalLocations != 0) {
            percentLocationCompleted = (countedLocations/totalLocations) * 100;
            percentLocationCompleted = percentLocationCompleted.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        progress.percentInventoryItemCompleted = percentInventoryItemCompleted;
        progress.percentLocationCompleted = percentLocationCompleted;

        progressList.add(progress);
    }
    context.grandTotalLocationCompleted = grandTotalLocationCompleted;
    context.grandTotalInventoryItemCompleted = grandTotalInventoryItemCompleted;
    context.grandTotalLocationCounted = grandTotalLocationCounted;
    context.grandTotalInventoryItemCounted = grandTotalInventoryItemCounted;
    if (grandTotalLocationCompleted != 0) {
        grandTotalPerLocation = (grandTotalLocationCounted/grandTotalLocationCompleted) * 100;
    }
    grandTotalPerLocation = grandTotalPerLocation.setScale(2, BigDecimal.ROUND_HALF_UP);
    context.grandTotalPerLocation = grandTotalPerLocation;
    if (grandTotalInventoryItemCompleted != 0) {
        grandTotalPerInventoryItem = (grandTotalInventoryItemCounted/grandTotalInventoryItemCompleted) * 100;
    }
    grandTotalPerInventoryItem = grandTotalPerInventoryItem.setScale(2, BigDecimal.ROUND_HALF_UP);
    context.grandTotalPerInventoryItem = grandTotalPerInventoryItem;
}
context.progressList = progressList;
context.inventoryCountItemsAndVariances = inventoryCountItemsAndVariances;