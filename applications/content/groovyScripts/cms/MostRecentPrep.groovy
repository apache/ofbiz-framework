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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.content.ContentManagementWorker
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator


Debug.logInfo("in mostrecentprep(1):","")
paramMap = UtilHttp.getParameterMap(request)
forumId = ContentManagementWorker.getFromSomewhere("moderatedSiteId", paramMap, request, context)

if (forumId) {
    exprList = [] as ArrayList
    fromDate = UtilDateTime.nowTimestamp()
    thruExpr2 = EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null)
    exprList.add(thruExpr2)
    statusIdExpr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CTNT_IN_PROGRESS")
    exprList.add(statusIdExpr)
    contentIdToExpr = EntityCondition.makeCondition("caContentId", EntityOperator.EQUALS, forumId)
    exprList.add(contentIdToExpr)
    expr = EntityCondition.makeCondition(exprList, EntityOperator.AND)
    entityList = from("ContentAssocViewFrom").where(exprList).orderBy("-caFromDate").queryList()

    Debug.logInfo("in mostrecentprep(1), entityList.size():" + entityList.size(),"")
    Debug.logInfo("in mostrecentprep(1), entityList:" + entityList,"")
    context.mostRecentList = entityList
}
