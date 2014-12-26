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

import org.ofbiz.entity.util.EntityUtil;
 
projectMembers = from("WorkEffortPartyAssignment").where("workEffortId", context.projectId).filterByDate().queryList();

toPartyId = null;
fromPartyId = null;
projectMembers.each {member ->
    if (member.roleTypeId.equals("INTERNAL_ORGANIZATIO")) {
        fromPartyId = member.partyId;
    }
    if (member.roleTypeId.equals("CLIENT_BILLING")) {
        toPartyId = member.partyId;
    }
    if (fromPartyId && toPartyId && fromPartyId.equals(toPartyId)) {
        context.isBillable = false;
    } else if (!toPartyId || !fromPartyId){
        context.isBillable = false;
    } else {    
        context.isBillable = true;
    }
}
