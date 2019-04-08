/*
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

// get last request from this user and use that project/task assignment as default on the screen

custRequestList = from("CustRequest").where("fromPartyId", fromPartyId).orderBy("-createdDate").queryList()
if (custRequestList) {
    custReqTaskList = custRequestList.get(0).getRelated("CustRequestWorkEffort", null, null, false)
    if (custReqTaskList) {
        custReqTask = custReqTaskList.get(0).getRelatedOne("WorkEffort", false) // phase
        projectChildWorkEffort = custReqTask.getRelatedOne("ParentWorkEffort", false) // phase name
        if (projectChildWorkEffort) {
            partyList = custReqTask.getRelated("WorkEffortPartyAssignment", null, null, false)
            if (partyList) {
                context.childWorkEffortId = projectChildWorkEffort.workEffortId
                context.partyId= partyList.get(0).partyId
            }
        }
    }
}
