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

import org.apache.ofbiz.entity.condition.EntityConditionBuilder

countSession = from('InventoryCount').where("inventoryCountId", parameters.inventoryCountId).queryOne();

countSessionValue = [:];
if (countSession) {
    countSessionValue.inventoryCountId = countSession?.inventoryCountId;
    statusItem = from("StatusItem").where("statusId", countSession?.statusId).queryOne();
    countSessionValue.statusId = countSession?.statusId;
    countSessionValue.statusDescription = statusItem?.description;
    countSessionValue.facilityId = countSession?.facilityId;
    facility = from("Facility").where("facilityId", countSession?.facilityId).queryOne();
    countSessionValue.facilityName = facility?.facilityName;
    countSessionValue.createdDate = countSession?.createdDate;
    countSessionValue.createdBy = countSession?.createdByUserLogin;
    context.inventoryCountId = countSession?.inventoryCountId;
    context.facilityId = countSession?.facilityId;
}
context.countSessionValue = countSessionValue;

exprBldr = new EntityConditionBuilder();
cond = exprBldr.AND() {
    if (parameters.inventoryCountId)
        EQUALS("inventoryCountId": parameters.inventoryCountId)
}
inventoryCountItemAndVarList = from('InventoryCountItemsAndVariance').where(cond).orderBy("locationSeqId").queryList();

inventoryCountItemAndVariances = [];
inventoryCountItemAndVarList.each { inventoryCountItemAndVar ->
    cycleCountMap = [:];
    cycleCountMap.inventoryCountId = inventoryCountItemAndVar.inventoryCountId;
    cycleCountMap.inventoryCountItemSeqId = inventoryCountItemAndVar.inventoryCountItemSeqId;
    cycleCountMap.locationSeqId = inventoryCountItemAndVar?.locationSeqId;

    facilityLocation = from("FacilityLocation").where("facilityId", inventoryCountItemAndVar?.facilityId, "locationSeqId", inventoryCountItemAndVar?.locationSeqId).queryOne();
    cycleCountMap.areaId = facilityLocation?.areaId;
    cycleCountMap.aisleId = facilityLocation?.aisleId;
    cycleCountMap.sectionId = facilityLocation?.sectionId;
    cycleCountMap.levelId = facilityLocation?.levelId
    cycleCountMap.positionId = facilityLocation?.positionId

    statusItem = from('StatusItem').where("statusId", inventoryCountItemAndVar.itemStatusId).queryOne();
    cycleCountMap.itemStatusId = inventoryCountItemAndVar.itemStatusId;
    cycleCountMap.statusDescription = statusItem?.description;

    facility = from("Facility").where("facilityId", inventoryCountItemAndVar?.facilityId).queryOne();
    cycleCountMap.facilityId = inventoryCountItemAndVar.facilityId;
    cycleCountMap.facilityName = facility?.facilityName;

    product = from("Product").where("productId", inventoryCountItemAndVar.productId).queryOne();
    cycleCountMap.internalName = product?.internalName;
    cycleCountMap.partDescription = product?.description;

    cycleCountMap.inventoryItemId = inventoryCountItemAndVar?.inventoryItemId;
    cycleCountMap.quantity = inventoryCountItemAndVar?.quantity;
    inventoryCountItemAndVariances.add(cycleCountMap);
}

inventoryValue = [:];
errorMessage = '';
if (parameters.inventoryItemId) {
    inventoryItem = from("InventoryItem").where("inventoryItemId", parameters.inventoryItemId).queryOne();
    if (inventoryItem) {
        inventoryValue.inventoryItemId = parameters.inventoryItemId;
        inventoryValue.locationSeqId = inventoryItem.locationSeqId;
        inventoryValue.quantity = parameters.quantity;
        inventoryItem = from("Product").where("productId", parameters.productId).queryOne();
        inventoryValue.internalName = product.internalName;
        inventoryValue.partDescription = product.description;
    } else {
        errorMessage = "Inventory item not found.";
    }
}
context.errorMessage = errorMessage;
context.inventoryValue = inventoryValue;

context.inventoryCountItemAndVariances = inventoryCountItemAndVariances;