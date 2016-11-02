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

/*
 * This script is also referenced by the ecommerce's screens and
 * should not contain order component's specific code.
 */
import org.apache.ofbiz.party.party.PartyHelper

// Put the result of CategoryWorker.getRelatedCategories into the separateRootType function as attribute.
// The separateRootType function will return the list of category of given catalog.
// PLEASE NOTE : The structure of the list of separateRootType function is according to the JSON_DATA plugin of the jsTree.

completedTree =  []
completedTreeContext = []
subtopLists = []

//internalOrg list
partyRelationships = from("PartyRelationship").where("partyIdFrom", partyId, "partyRelationshipTypeId", "GROUP_ROLLUP").filterByDate().queryList()
if (partyRelationships) {
    //root
    partyRoot = from("PartyGroup").where("partyId", partyId).queryOne()
    partyRootMap = [:]
    partyRootMap.put("partyId", partyId)
    partyRootMap.put("groupName", partyRoot.getString("groupName"))

    //child
    for(partyRelationship in partyRelationships) {
        partyGroupMap = [:]
        partyGroupMap.put("partyId", partyRelationship.getString("partyIdTo"))
        partyGroupMap.put("groupName", PartyHelper.getPartyName(delegator, partyRelationship.getString("partyIdTo"), false))
        completedTreeContext.add(partyGroupMap)

        subtopLists.addAll(partyRelationship.getString("partyIdTo"))
    }

    partyRootMap.put("child", completedTreeContext)
    completedTree.add(partyRootMap)
}

// The complete tree list for the category tree
context.completedTree = completedTree
context.subtopLists = subtopLists
