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

import org.apache.ofbiz.base.util.*

results =  []
ebayAccountList = from("PartyRoleAndPartyDetail").where("roleTypeId", "EBAY_ACCOUNT").queryList()
productStoreRoles = from("ProductStoreRole").where("roleTypeId", "EBAY_ACCOUNT").queryList()

if (productStoreRoles != null && ebayAccountList != null) {
    ebayAccountList.each{ebayAccount->
        partyId = ebayAccount.partyId
        productStoreRoles.each{productStoreRole ->
            if(partyId.equals(productStoreRole.partyId)){
                storeMap = [:]
                storeMap.partyId = ebayAccount.partyId
                storeMap.firstName = ebayAccount.firstName
                storeMap.lastName = ebayAccount.lastName
                storeMap.productStoreId = productStoreRole.productStoreId
                results.add(storeMap)
            }
        }
    }
    context.put("stores",results)
}
