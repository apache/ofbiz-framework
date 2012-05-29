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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.condition.*;

def module = "BacklogNotifications.groovy";

custRequest = delegator.findOne("CustRequest", ["custRequestId" : custRequestId], false);
person = delegator.findOne("PartyNameView", ["partyId" : partyIdTo], false);
informationMap = [:];
informationMap.internalName = null;
informationMap.productId = null;
informationMap.workEffortName = null;
informationMap.workEffortId = null;

//check in sprint
andExprs = [EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_SPRINT"),
            EntityCondition.makeCondition("custRequestId", EntityOperator.EQUALS, custRequestId)];
backlogCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
backlogList = delegator.findList("ProductBacklog", backlogCond, ["productId", "workEffortId", "custRequestId"] as Set ,null ,null, false);
if (backlogList) {
    product = delegator.findOne("Product", ["productId" : backlogList[0].productId], false);
    sprint = delegator.findOne("WorkEffort", ["workEffortId" : backlogList[0].workEffortId], false);
    informationMap.internalName = product.internalName;
    informationMap.productId = product.productId;
    informationMap.workEffortName = sprint.workEffortName;
    informationMap.workEffortId = sprint.workEffortId;
} else {
    andExprs = [EntityCondition.makeCondition("custRequestId", EntityOperator.EQUALS, custRequestId)];
    backlogCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
    backlogList = delegator.findList("ProductBacklog", backlogCond, ["productId", "workEffortId", "custRequestId"] as Set ,null ,null, false);
    if (backlogList) {
        if (backlogList[0].productId) {
            product = delegator.findOne("Product", ["productId" : backlogList[0].productId], false);
            informationMap.internalName = product.internalName;
            informationMap.productId = product.productId;
        }
    }
}
// check backlog removed from sprint.
removedFromSprint = false;
if ("CRQ_ACCEPTED".equals(custRequest.statusId)) {
    custStatusList = custRequest.getRelated("CustRequestStatus", null, ["-custRequestStatusId"], false);
    if (custStatusList.size() > 2 && "CRQ_REVIEWED".equals(custStatusList[1].statusId)) {
        removedFromSprint = true;
        }
    }

context.custRequest = custRequest;
context.person = person;
context.informationMap = informationMap;
context.removedFromSprint = removedFromSprint;
