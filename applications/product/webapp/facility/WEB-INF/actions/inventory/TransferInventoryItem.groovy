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

import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*

facilityId = request.getParameter("facilityId");

inventoryTransferId = request.getParameter("inventoryTransferId");
context.inventoryTransferId = inventoryTransferId;

inventoryItemId = request.getParameter("inventoryItemId");
inventoryTransfer = null;

if (inventoryTransferId) {
    inventoryTransfer = from("InventoryTransfer").where("inventoryTransferId", inventoryTransferId).queryOne();
    if (inventoryTransfer) {
        context.inventoryTransfer = inventoryTransfer;
        if (!facilityId) {
            facilityId = inventoryTransfer.facilityId;
            parameters.facilityId = facilityId;
        }
        if (!inventoryItemId) {
            inventoryItemId = inventoryTransfer.inventoryItemId;
        }
    }
}

facility = from("Facility").where("facilityId", facilityId).queryOne();
context.facilityId = facilityId;
context.facility = facility;
context.inventoryItemId = inventoryItemId;

if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne();
}

String illegalInventoryItem = null;
if (inventoryItemId) {
    inventoryItem = from("InventoryItem").where("inventoryItemId", inventoryItemId).queryOne();
    if (facilityId && inventoryItem && inventoryItem.facilityId && !inventoryItem.facilityId.equals(facilityId)) {
        illegalInventoryItem = "Inventory item not found for this facility.";
        inventoryItem = null;
    }
    if (inventoryItem) {
        context.inventoryItem = inventoryItem;
        inventoryItemType = inventoryItem.getRelatedOne("InventoryItemType", false);

        if (inventoryItemType) {
            context.inventoryItemType = inventoryItemType;
        }
        if (inventoryItem.statusId) {
            inventoryStatus = inventoryItem.getRelatedOne("StatusItem", false);
            if (inventoryStatus) {
                context.inventoryStatus = inventoryStatus;
            }
        }
    }
}

// facilities
context.facilities = from("Facility").queryList();

// status items
if (inventoryTransfer && inventoryTransfer.statusId) {
    statusChange = from("StatusValidChange").where("statusId", inventoryTransfer.statusId).queryList();
    if (statusChange) {
        statusItems = [] as ArrayList;
        statusChange.each { curStatusChange ->
            curStatusItem = from("StatusItem").where("statusId", curStatusChange.statusIdTo).queryOne();
            if (curStatusItem) {
                statusItems.add(curStatusItem);
            }
        }
        statusItem = EntityUtil.orderBy(statusItems, ['sequenceId']);
        context.statusItems = statusItems;
    }
} else {
    statusItems = from("StatusItem").where("statusTypeId", "INVENTORY_XFER_STTS").orderBy("sequenceId").queryList();
    if (statusItems) {
        context.statusItems = statusItems;
    }
}
