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

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

context.userLogin = userLogin

// create the sort order
sort = parameters.sort
sortOrder = ["currentStatusId", "-priority", "orderDate"]
if (sort) {
    if ("name".equals(sort)) {
        sortOrder.add(0, "firstName")
        sortOrder.add(0, "lastName")
    } else if ("grandTotal".equals(sort)) {
        sortOrder.add(0, "-grandTotal")
    } else {
        sortOrder.add(0, sort)
    }
}

partyBase = [EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CAL_ACCEPTED"), EntityCondition.makeCondition("wepaPartyId", EntityOperator.EQUALS, userLogin.partyId)]
partyRole = [EntityCondition.makeCondition("orderRoleTypeId", EntityOperator.EQUALS, "PLACING_CUSTOMER"), EntityCondition.makeCondition("orderRoleTypeId", EntityOperator.EQUALS, "SUPPLIER_AGENT")]
partyExpr = [EntityCondition.makeCondition(partyBase, EntityOperator.AND), EntityCondition.makeCondition(partyRole, EntityOperator.OR)]
partyTasks = from("OrderTaskList").where(partyExpr).orderBy(sortOrder).queryList()

if (partyTasks) partyTasks = EntityUtil.filterByDate(partyTasks)
context.partyTasks = partyTasks

// Build a map of orderId and currency
orderCurrencyMap = [:]
partyTasks.each { ptItem ->
    orderHeader = from("OrderHeader").where("orderId", ptItem.orderId).queryOne()
    orderCurrencyMap[ptItem.orderId] = orderHeader.currencyUom
}

// get this user's roles
partyRoles = from("PartyRole").where("partyId", userLogin.partyId).queryList()

// build the role list
pRolesList = []
partyRoles.each { partyRole ->
    if (!"_NA_".equals(partyRole.roleTypeId))
        pRolesList.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, partyRole.roleTypeId))
}

custList = [EntityCondition.makeCondition("orderRoleTypeId", EntityOperator.EQUALS, "PLACING_CUSTOMER"), EntityCondition.makeCondition("orderRoleTypeId", EntityOperator.EQUALS, "SUPPLIER_AGENT")]
baseList = [EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"), EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"), EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED")]
expressions = []
expressions.add(EntityCondition.makeCondition(custList, EntityOperator.OR))
if (pRolesList) expressions.add(EntityCondition.makeCondition(pRolesList, EntityOperator.OR))
expressions.add(EntityCondition.makeCondition(baseList, EntityOperator.AND))

// invoke the query
roleTasks = from("OrderTaskList").where(expressions).orderBy(sortOrder).queryList()
roleTasks = EntityUtil.filterByAnd(roleTasks, baseList)
roleTasks = EntityUtil.filterByDate(roleTasks)
context.roleTasks = roleTasks

// Add to the map of orderId and currency
roleTasks.each { rtItem ->
    orderHeader = from("OrderHeader").where("orderId", rtItem.orderId).queryOne()
    orderCurrencyMap[rtItem.orderId] = orderHeader.currencyUom
}
context.orderCurrencyMap = orderCurrencyMap

context.now = nowTimestamp

// purchase order schedule
poList = from("OrderHeaderAndRoles").where("partyId", userLogin.partyId, "orderTypeId", "PURCHASE_ORDER").queryList()
poIter = poList.iterator()
listedPoIds = new HashSet()
while (poIter.hasNext()) {
    poGv = poIter.next()
    poOrderId = poGv.orderId
    if (listedPoIds.contains(poOrderId)) {
        poIter.remove()
    } else {
        listedPoIds.add(poOrderId)
    }
}
context.poList = poList

