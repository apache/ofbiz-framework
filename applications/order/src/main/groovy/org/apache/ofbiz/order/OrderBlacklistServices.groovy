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
package org.apache.ofbiz.order

import org.apache.ofbiz.entity.GenericValue

/*
 * Migrate all elements present on OldOrderBlacklist and OldOrderBlacklistType to respectively OrderDenylist and OrderDenylistType entities
 * Update service created 2021-02
 */
Map migrateOldOrderBlacklistAndOldOrderBlacklistType() {
    List<GenericValue> oldOrderBlacklistType = delegator.findAll('OldOrderBlacklistType', false)
    List<GenericValue> typesToRemove = []
    oldOrderBlacklistType.each {
        GenericValue orderDenyListType = makeValue('OrderDenylistType')
        orderDenyListType.orderDenylistTypeId = it.orderBlacklistTypeId
        orderDenyListType.description = it.description
        orderDenyListType.create()
        typesToRemove << it
    }

    List<GenericValue> oldOrderBlacklist = delegator.findAll('OldOrderBlacklist', false)
    oldOrderBlacklist.each {
        GenericValue orderDenyList = makeValue('OrderDenylist')
        orderDenyList.denylistString = it.blacklistString
        orderDenyList.orderDenylistTypeId = it.orderBlacklistTypeId
        orderDenyList.create()
        it.remove()
    }

    delegator.removeAll(typesToRemove)

    return success()
}
