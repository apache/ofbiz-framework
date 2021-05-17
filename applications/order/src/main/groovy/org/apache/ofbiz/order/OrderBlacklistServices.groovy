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
 * Migrate all elements present on OldOrderBlacklist and OldOrderBlacklistType to respectively OrderDenylist and OrderDenylistType entities
 * Update service created 2021-02
 */
def migrateOldOrderBlacklistAndOldOrderBlacklistType() {
    List<GenericValue> oldOrderBlacklist = delegator.findAll("OldOrderBlacklist", false)
    oldOrderBlacklist.each {
        GenericValue OrderDenylist = makeValue("OrderDenylist")
        OrderDenylist.blacklistString = it.blacklistString
        OrderDenylist.orderBlacklistTypeId = it.orderBlacklistTypeId
        OrderDenylist.create()
        it.remove()
    }

    List<GenericValue> oldOrderBlacklistType = delegator.findAll("OldOrderBlacklistType", false)
    oldOrderBlacklist.each {
        GenericValue OrderDenylistType = makeValue("OrderDenylistType")
        orderBlacklist.orderBlacklistTypeId = it.orderBlacklistTypeId
        OrderDenylistType.description = it.description
        OrderDenylistType.create()
        it.remove()
    }

    return success()
}
