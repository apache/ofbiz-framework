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

import org.apache.ofbiz.entity.condition.EntityCondition

// get physicalInventoryAndVarianceDatas if this is a NON_SERIAL_INV_ITEM
if (inventoryItem && "NON_SERIAL_INV_ITEM".equals(inventoryItem.inventoryItemTypeId)) {
    physicalInventoryAndVariances = from("PhysicalInventoryAndVariance").where("inventoryItemId", inventoryItemId).orderBy("-physicalInventoryDate", "-physicalInventoryId").queryList()
    physicalInventoryAndVarianceDatas = new ArrayList(physicalInventoryAndVariances.size())
    physicalInventoryAndVariances.each { physicalInventoryAndVariance ->
        physicalInventoryAndVarianceData = [:]
        physicalInventoryAndVarianceDatas.add(physicalInventoryAndVarianceData)

        physicalInventoryAndVarianceData.physicalInventoryAndVariance = physicalInventoryAndVariance
        physicalInventoryAndVarianceData.varianceReason = physicalInventoryAndVariance.getRelatedOne("VarianceReason", true)
        physicalInventoryAndVarianceData.person = physicalInventoryAndVariance.getRelatedOne("Person", false)
        physicalInventoryAndVarianceData.partyGroup = physicalInventoryAndVariance.getRelatedOne("PartyGroup", false)
    }
    context.physicalInventoryAndVarianceDatas = physicalInventoryAndVarianceDatas
}
