/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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

import org.apache.ofbiz.entity.GenericValue

/*
 * Migrate all element present on entity OldPicklistStatusHistory to entity PickListStatus
 * Update service created 2019-09
 */
def migrateOldPicklistStatusHistoryToPickListStatus() {
    List<GenericValue> oldPicklistStatusHistories = delegator.findAll("OldPicklistStatusHistory", false)
    oldPicklistStatusHistories.each {
        GenericValue picklistStatus = makeValue("PicklistStatus")
        picklistStatus.statusId = it.statusId
        picklistStatus.statusIdTo = it.statusIdTo
        picklistStatus.picklistId = it.picklistId
        picklistStatus.changeByUserLoginId = it.changeUserLoginId
        picklistStatus.statusDate = it.changeDate
        picklistStatus.create()
        it.remove()
    }
    return success()
}
