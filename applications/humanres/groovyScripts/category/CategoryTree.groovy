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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.party.party.PartyHelper

// PLEASE NOTE : The structure of the list of separateRootType function is according to the JSON_DATA plugin of the jsTree.

List completedTree =  []
List completedTreeContext = []
List subTopList = []

//internalOrg list
List partyRelationships = from("PartyRelationship")
        .where(partyIdFrom: partyId,
                partyRelationshipTypeId: "GROUP_ROLLUP")
        .filterByDate()
        .cache()
        .queryList()
if (partyRelationships) {
    //root
    GenericValue partyRoot = from("PartyGroup").where(partyId: partyId).cache().queryOne()
    Map partyRootMap = [partyId  : partyId,
                        groupName: partyRoot.groupName]

    //child
    partyRelationships.each {
        completedTreeContext << [partyId  : it.partyIdTo,
                                 groupName: PartyHelper.getPartyName(delegator, it.partyIdTo, false)]

        subTopList << it.partyIdTo
    }

    partyRootMap.child = completedTreeContext
    completedTree << partyRootMap
}

// The complete tree list for the category tree
context.completedTree = completedTree
context.subtopLists = subTopList
