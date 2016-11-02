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

import org.apache.ofbiz.entity.condition.*

//default this to true, ie only show active
activeOnly = !"false".equals(request.getParameter("activeOnly"))
context.activeOnly = activeOnly

// if the completeRequested was set, then we'll lookup only requested status
completeRequested = "true".equals(request.getParameter("completeRequested"))
context.completeRequested = completeRequested

// get the 'to' this facility transfers
if (activeOnly) {
    exprsTo = [EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_COMPLETE"),
               EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_CANCELLED")]
} else {
    exprsTo = [EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId)]
}
if (completeRequested) {
    exprsTo = [EntityCondition.makeCondition("facilityIdTo", EntityOperator.EQUALS, facilityId),
               EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "IXF_REQUESTED")]
}
toTransfers = from("InventoryTransfer").where(exprsTo).orderBy("sendDate").queryList()
if (toTransfers) {
    context.toTransfers = toTransfers
}

// get the 'from' this facility transfers
if (activeOnly) {
    exprsFrom = [EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_COMPLETE"),
                 EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "IXF_CANCELLED")]
} else {
    exprsFrom = [EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId)]
}
if (completeRequested) {
    exprsFrom = [EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                 EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "IXF_REQUESTED")]
}
ecl = EntityCondition.makeCondition(exprsFrom, EntityOperator.AND)
fromTransfers = from("InventoryTransfer").where(exprsFrom).orderBy("sendDate").queryList()
if (fromTransfers) {
    context.fromTransfers = fromTransfers
}
