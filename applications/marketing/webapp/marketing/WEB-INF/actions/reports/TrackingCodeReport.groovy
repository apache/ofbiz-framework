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

import org.ofbiz.entity.transaction.TransactionUtil
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.condition.EntityExpr
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityConditionList
import org.ofbiz.marketing.report.ReportHelper

trackingCodeIdStr = request.getParameter("trackingCodeId");
// query for both number of visits and number of orders

visitConditionList = [] as LinkedList;
orderConditionList = [] as LinkedList;

if (parameters.fromDate) {
    visitConditionList.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, parameters.fromDate));
    orderConditionList.add(EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, parameters.fromDate));
}
if (parameters.thruDate) {
     visitConditionList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, parameters.thruDate));
     orderConditionList.add(EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, parameters.thruDate));
}
if (parameters.trackingCodeId) {
     visitConditionList.add(EntityCondition.makeCondition("trackingCodeId", EntityOperator.EQUALS, parameters.trackingCodeId));
     orderConditionList.add(EntityCondition.makeCondition("trackingCodeId", EntityOperator.EQUALS, parameters.trackingCodeId));
}

visitConditions = EntityCondition.makeCondition(visitConditionList, EntityOperator.AND);
orderConditions = EntityCondition.makeCondition(orderConditionList, EntityOperator.AND);

visits = delegator.findList("TrackingCodeAndVisit", visitConditions, ['trackingCodeId', 'visitId'] as Set, ['trackingCodeId'], null, false);
orders = delegator.findList("TrackingCodeAndOrderHeader", orderConditions, ['trackingCodeId', 'orderId', 'grandTotal'] as Set, ['trackingCodeId'], null, false);

// use this helper to build a List of visits, orders, order totals, and conversion rates
trackingCodeVisitAndOrders = ReportHelper.calcConversionRates(visits, orders, "trackingCodeId");
context.trackingCodeVisitAndOrders = trackingCodeVisitAndOrders;
