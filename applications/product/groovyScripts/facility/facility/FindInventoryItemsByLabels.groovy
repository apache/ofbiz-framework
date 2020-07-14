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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.entity.model.DynamicViewEntity
import org.apache.ofbiz.entity.model.ModelKeyMap
import org.apache.ofbiz.entity.transaction.*
import org.apache.ofbiz.entity.util.EntityFindOptions
import org.apache.ofbiz.entity.util.EntityListIterator

facilityId = parameters.facilityId

numberOfFields = 0
numberOfFieldsStr = parameters.numberOfFields
try {
    numberOfFields = Integer.parseInt(numberOfFieldsStr)
} catch (Exception exc) {
    numberOfFields = 0
}

inventoryItemAndLabelsView = new DynamicViewEntity()
inventoryItemAndLabelsView.addMemberEntity("II", "InventoryItem")
inventoryItemAndLabelsView.addAliasAll("II", null, null)
for (int i = 1; i <= numberOfFields; i++) {
    inventoryItemLabelId = parameters.get("inventoryItemLabelId_" + i)
    if (inventoryItemLabelId) {
        inventoryItemAndLabelsView.addMemberEntity("IL" + i, "InventoryItemLabelAppl")
        inventoryItemAndLabelsView.addViewLink("II", "IL" + i, new Boolean(false), ModelKeyMap.makeKeyMapList("inventoryItemId"))
    }
}
andCondition = [EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId)]
for (int i = 1; i <= numberOfFields; i++) {
    inventoryItemLabelId = parameters.get("inventoryItemLabelId_" + i)
    if (inventoryItemLabelId) {
        inventoryItemAndLabelsView.addAlias("IL" + i, "inventoryItemLabelId" + i, "inventoryItemLabelId", null, null, null, null)
        andCondition.add(EntityCondition.makeCondition("inventoryItemLabelId" + i, EntityOperator.EQUALS, inventoryItemLabelId))
    }
}
EntityListIterator inventoryItemsEli = null
beganTransaction = false
List inventoryItems = null
if (andCondition.size() > 1) {
    try {
        // get the indexes for the partial list
        lowIndex = ((viewIndex * viewSize) + 1)
        highIndex = (viewIndex - 1) * viewSize

        beganTransaction = TransactionUtil.begin()
        inventoryItemsEli = from(inventoryItemAndLabelsView).where(andCondition).cursorScrollInsensitive().distinct().maxRows(highIndex).queryIterator()

        inventoryItemsSize = inventoryItemsEli.getResultsSizeAfterPartialList()
        context.inventoryItemsSize = inventoryItemsSize
        if (highIndex > inventoryItemsSize) {
            highIndex = inventoryItemsSize
        }

        // get the partial list for this page
        inventoryItemsEli.beforeFirst()
        if (inventoryItemsSize > 0) {
            inventoryItems = inventoryItemsEli.getPartialList(lowIndex, viewSize)
        } else {
            inventoryItems = [] as ArrayList
        }

        // close the list iterator
        inventoryItemsEli.close()
    } catch (GenericEntityException e) {
        errMsg = "Failure in operation, rolling back transaction"
        logError(e, errMsg)
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, errMsg, e)
        } catch (GenericEntityException e2) {
            logError(e2, "Could not rollback transaction: " + e2.toString())
        }
        // after rolling back, rethrow the exception
        throw e
    } finally {
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction)
    }
}
context.inventoryItems = inventoryItems
