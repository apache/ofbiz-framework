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

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

commonReturnHistoryCond = [EntityCondition.makeCondition("changedEntityName", EntityOperator.EQUALS, "ReturnItem"),
                           EntityCondition.makeCondition("changedFieldName", EntityOperator.EQUALS, entityField),
                           EntityCondition.makeCondition("pkCombinedValueText", EntityOperator.LIKE, returnId + "%"),
                           EntityCondition.makeCondition("newValueText", EntityOperator.NOT_EQUAL, null),
                           EntityCondition.makeCondition("oldValueText", EntityOperator.NOT_EQUAL, null)];

returnHistoryList = from("EntityAuditLog").where(commonReturnHistoryCond).queryList();

orderReturnItemHistories = [];
returnHistoryList.each { returnHistory ->
    if ("returnTypeId".equals(entityField) || "returnReasonId".equals(entityField)) {
        if (returnHistory.newValueText.toString() != returnHistory.oldValueText.toString()) {
            orderReturnItemHistories.add(returnHistory);
        }
    } else if ((Float.valueOf(returnHistory.oldValueText)).compareTo(Float.valueOf(returnHistory.newValueText)) != 0) {
            orderReturnItemHistories.add(returnHistory);
    }
}
context.orderReturnItemHistories = orderReturnItemHistories;
