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

import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;

taskStatusId = null;
paraBacklogStatusId = backlogStatusId;

orStsExprs = [];
    if (backlogStatusId != "Any") {
        taskStatusId = "STS_CREATED";
        orStsExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REVIEWED"));
    } else {
        orStsExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_REVIEWED"));
        orStsExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CRQ_COMPLETED"));
    }
orCurentExprs = [];
    if (taskStatusId) {
        orCurentExprs.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, taskStatusId));
        orCurentExprs.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, null));
    }
andExprs =  [];
    andExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, parameters.productId));
    andExprs.add(EntityCondition.makeCondition("custRequestTypeId", EntityOperator.EQUALS, "RF_UNPLAN_BACKLOG"));
    andExprs.add(EntityCondition.makeCondition(orStsExprs, EntityOperator.OR));
    andExprs.add(EntityCondition.makeCondition(orCurentExprs, EntityOperator.OR));
unplannedBacklogCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
unplannedBacklogList = from("UnPlannedBacklogsAndTasks").where(unplannedBacklogCond).orderBy("-custRequestId","workEffortTypeId","custSequenceNum").queryList();

context.listIt = unplannedBacklogList;
context.paraBacklogStatusId = paraBacklogStatusId;
