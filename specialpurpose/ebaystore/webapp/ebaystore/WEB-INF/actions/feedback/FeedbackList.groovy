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
import java.sql.Timestamp;
import java.util.Map;

import javolution.util.FastMap;

partyId = null
inMap = FastMap.newInstance();
inMap.put("productStoreId", parameters.productStoreId);
inMap.put("userLogin", context.get("userLogin"));
resultUser = dispatcher.runSync("getEbayStoreUser", inMap);
ownerUser = resultUser.get("userLoginId");
userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", ownerUser));
if (userLogin) {
    partyId = userLogin.get("partyId");
}
expr = []
cond = null;

contentId = request.getParameter("contentId");
fromDate = request.getParameter("fromDate");
thruDate = request.getParameter("thruDate");
if (contentId) {
    expr.add(EntityCondition.makeCondition("contentId",EntityOperator.EQUALS, contentId));
}
if (fromDate && thruDate) {
    exprSub = [];
    condSub = null;
    exprSub.add(EntityCondition.makeCondition("createdDate",EntityOperator.GREATER_THAN, UtilDateTime.getDayStart(Timestamp.valueOf(fromDate + " 00:00:00.000"))));
    exprSub.add(EntityCondition.makeCondition("createdDate",EntityOperator.LESS_THAN, UtilDateTime.getDayEnd(Timestamp.valueOf(thruDate + " 23:59:59.999"))));
    condSub = EntityCondition.makeCondition(exprSub, EntityOperator.AND);
    expr.add(condSub);
} else if (fromDate && !thruDate) {
    expr.add(EntityCondition.makeCondition("createdDate",EntityOperator.GREATER_THAN, UtilDateTime.getDayStart(Timestamp.valueOf(fromDate + " 00:00:00.000"))));
} else if (!fromDate && thruDate) {
    expr.add(EntityCondition.makeCondition("createdDate",EntityOperator.LESS_THAN, UtilDateTime.getDayEnd(Timestamp.valueOf(thruDate + " 23:59:59.999"))));
}
contentRoles = delegator.findByAnd("ContentRole", UtilMisc.toMap("roleTypeId","OWNER", "partyId", partyId));
contentIds = [];
contentRoles.each{ content ->
    contentIds.add(content.getString("contentId"));
}
expr.add(EntityCondition.makeCondition("contentId", EntityOperator.IN, contentIds));
cond = EntityCondition.makeCondition(expr, EntityOperator.AND);
contents = delegator.findList("Content", cond, null, null, null, false);

recentFeedbackList = [];
ownerUser = null;
commentator = null;
contents.each{ content ->
    commentatorContents = delegator.findByAnd("ContentRole", UtilMisc.toMap("contentId",content.contentId, "roleTypeId","COMMENTATOR"));
    if(commentatorContents){
        commentatorPartyId = commentatorContents.get(0).get("partyId");
        commentatorUsers = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", commentatorPartyId));
        if(commentatorUsers){
            commentator = commentatorUsers.get(0).get("userLoginId");
        }
    }
    entry = [contentId : content.contentId,
             dataResourceId : content.dataResourceId,
             createdDate : content.createdDate,
             ownerUser : ownerUser,
             commentator : commentator];
    recentFeedbackList.add(entry);
}
context.recentFeedbackList = recentFeedbackList;