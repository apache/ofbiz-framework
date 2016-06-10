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

import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;

facilityId = request.getParameter("facilityId");
returnId = request.getParameter("returnId");

facility = null;
if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne();
}

returnHeader = null;
returnItems = null;
if (returnId) {
    returnHeader = from("ReturnHeader").where("returnId", returnId).queryOne();
    if (returnHeader) {
        if ("RETURN_ACCEPTED".equals(returnHeader.statusId)) {
            returnItems = returnHeader.getRelated("ReturnItem", null, null, false);
        } else if ("RETURN_REQUESTED".equals(returnHeader.statusId)) {
            uiLabelMap = UtilProperties.getResourceBundleMap("ProductErrorUiLabels", locale);
            ProductReturnRequestedOK = uiLabelMap.ProductReturnRequestedOK;
            request.setAttribute("_EVENT_MESSAGE_", ProductReturnRequestedOK + " (#" + returnId.toString() + ")" );
        }  else if ("RETURN_RECEIVED".equals(!returnHeader.statusId)) {
            uiLabelMap = UtilProperties.getResourceBundleMap("ProductErrorUiLabels", locale);
            ProductReturnNotYetAcceptedOrAlreadyReceived = uiLabelMap.ProductReturnNotYetAcceptedOrAlreadyReceived;
            request.setAttribute("_ERROR_MESSAGE_", ProductReturnNotYetAcceptedOrAlreadyReceived + " (#" + returnId.toString() + ")" );
        }
    }
}

receivedQuantities = [:];
if (returnItems) {
    context.firstOrderItem = EntityUtil.getFirst(returnItems);
    context.returnItemsSize = returnItems.size();
    returnItems.each { thisItem ->
        totalReceived = 0.0;
        receipts = thisItem.getRelated("ShipmentReceipt", null, null, false);
        if (receipts) {
            receipts.each { rec ->
                accepted = rec.getDouble("quantityAccepted");
                rejected = rec.getDouble("quantityRejected");
                if (accepted)
                    totalReceived += accepted.doubleValue();
                if (rejected)
                    totalReceived += rejected.doubleValue();
            }
        }
        receivedQuantities[thisItem.returnItemSeqId] = new Double(totalReceived);
    }
}

if (returnHeader) {
    context.receivedItems = from("ShipmentReceiptAndItem").where("returnId", returnId).queryList();
}

// facilities
facilities = from("Facility").queryList();

//all possible inventory item types
inventoryItemTypes = from("InventoryItemType").orderBy("description").cache(true).queryList();

context.facilityId = facilityId;
context.facility = facility;
context.returnHeader = returnHeader;
context.returnItems = returnItems;
context.receivedQuantities = receivedQuantities;
context.facilities = facilities;
context.inventoryItemTypes = inventoryItemTypes;
