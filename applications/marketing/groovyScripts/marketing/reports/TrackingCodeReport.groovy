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

import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.marketing.report.ReportHelper

// query for both number of visits and number of orders

visitConditionList = [] as LinkedList
orderConditionList = [] as LinkedList

if (fromDate) {
    visitConditionList.add(EntityCondition.makeCondition('fromDate', EntityOperator.GREATER_THAN_EQUAL_TO, fromDate))
    orderConditionList.add(EntityCondition.makeCondition('orderDate', EntityOperator.GREATER_THAN_EQUAL_TO, fromDate))
}
if (thruDate) {
    visitConditionList.add(EntityCondition.makeCondition('fromDate', EntityOperator.LESS_THAN_EQUAL_TO, thruDate))
    orderConditionList.add(EntityCondition.makeCondition('orderDate', EntityOperator.LESS_THAN_EQUAL_TO, thruDate))
}
if (trackingCodeId) {
    visitConditionList.add(EntityCondition.makeCondition('trackingCodeId', EntityOperator.EQUALS, trackingCodeId))
    orderConditionList.add(EntityCondition.makeCondition('trackingCodeId', EntityOperator.EQUALS, trackingCodeId))
}

visits = select('trackingCodeId', 'visitId').from('TrackingCodeAndVisit').where(visitConditionList).orderBy('trackingCodeId').queryList()
orders = select('trackingCodeId', 'orderId', 'grandTotal').from('TrackingCodeAndOrderHeader').where(orderConditionList).orderBy('trackingCodeId').queryList()

// use this helper to build a List of visits, orders, order totals, and conversion rates
trackingCodeVisitAndOrders = ReportHelper.calcConversionRates(visits, orders, 'trackingCodeId')
context.trackingCodeVisitAndOrders = trackingCodeVisitAndOrders
