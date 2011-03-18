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

import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;

BigDecimal calcItemTotal(List headers) {
    BigDecimal total = BigDecimal.ZERO;
    headers.each { header ->
        total = total.plus(header.grandTotal ?: BigDecimal.ZERO);
    }
    return total;
}

double calcItemCount(List items) {
    double count = 0.00;
    items.each { item ->
        count += item.quantity ?: 0.00;
    }
    return count;
}

dayBegin = UtilDateTime.getDayStart(nowTimestamp, timeZone, locale);
weekBegin = UtilDateTime.getWeekStart(nowTimestamp, timeZone, locale);
monthBegin = UtilDateTime.getMonthStart(nowTimestamp, timeZone, locale);
yearBegin = UtilDateTime.getYearStart(nowTimestamp, timeZone, locale);

dayEnd = UtilDateTime.getDayEnd(nowTimestamp, timeZone, locale);
weekEnd = UtilDateTime.getWeekEnd(nowTimestamp, timeZone, locale);
monthEnd = UtilDateTime.getMonthEnd(nowTimestamp, timeZone, locale);
yearEnd = UtilDateTime.getYearEnd(nowTimestamp, timeZone, locale);

// order status report
ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.GREATER_THAN_EQUAL_TO, dayBegin),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, dayEnd)],
                    EntityOperator.AND);
dayList = delegator.findList("OrderStatus", ecl, null, null, null, false);
context.dayOrder = EntityUtil.filterByAnd(dayList, [statusId : "ORDER_CREATED"]);
context.dayApprove = EntityUtil.filterByAnd(dayList, [statusId : "ORDER_APPROVED"]);
context.dayComplete = EntityUtil.filterByAnd(dayList, [statusId : "ORDER_COMPLETED"]);
context.dayCancelled = EntityUtil.filterByAnd(dayList, [statusId : "ORDER_CANCELLED"]);
context.dayRejected = EntityUtil.filterByAnd(dayList, [statusId : "ORDER_REJECTED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.GREATER_THAN_EQUAL_TO, weekBegin),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, weekEnd)],
                    EntityOperator.AND);
weekList = delegator.findList("OrderStatus", ecl, null, null, null, false);
context.weekOrder = EntityUtil.filterByAnd(weekList, [statusId : "ORDER_CREATED"]);
context.weekApprove = EntityUtil.filterByAnd(weekList, [statusId: "ORDER_APPROVED"]);
context.weekComplete = EntityUtil.filterByAnd(weekList, [statusId : "ORDER_COMPLETED"]);
context.weekCancelled = EntityUtil.filterByAnd(weekList, [statusId : "ORDER_CANCELLED"]);
context.weekRejected = EntityUtil.filterByAnd(weekList, [statusId : "ORDER_REJECTED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.GREATER_THAN_EQUAL_TO, monthBegin),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, monthEnd)],
                    EntityOperator.AND);
monthList = delegator.findList("OrderStatus", ecl, null, null, null, false);
context.monthOrder = EntityUtil.filterByAnd(monthList, [statusId : "ORDER_CREATED"]);
context.monthApprove = EntityUtil.filterByAnd(monthList, [statusId : "ORDER_APPROVED"]);
context.monthComplete = EntityUtil.filterByAnd(monthList, [statusId : "ORDER_COMPLETED"]);
context.monthCancelled = EntityUtil.filterByAnd(monthList, [statusId : "ORDER_CANCELLED"]);
context.monthRejected = EntityUtil.filterByAnd(monthList, [statusId : "ORDER_REJECTED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.GREATER_THAN_EQUAL_TO, yearBegin),
                        EntityCondition.makeCondition("statusDatetime", EntityOperator.LESS_THAN_EQUAL_TO, yearEnd)],
                    EntityOperator.AND);
yearList = delegator.findList("OrderStatus", ecl, null, null, null, false);
context.yearOrder = EntityUtil.filterByAnd(yearList, [statusId : "ORDER_CREATED"]);
context.yearApprove = EntityUtil.filterByAnd(yearList, [statusId : "ORDER_APPROVED"]);
context.yearComplete = EntityUtil.filterByAnd(yearList, [statusId : "ORDER_COMPLETED"]);
context.yearCancelled = EntityUtil.filterByAnd(yearList, [statusId : "ORDER_CANCELLED"]);
context.yearRejected = EntityUtil.filterByAnd(yearList, [statusId : "ORDER_REJECTED"]);

// order totals and item counts
ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, dayBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, dayEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
dayItems = delegator.findList("OrderHeaderAndItems", ecl, null, null, null, false);
dayItemsPending = EntityUtil.filterByAnd(dayItems, [itemStatusId : "ITEM_ORDERED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, dayBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, dayEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
dayHeaders = delegator.findList("OrderHeader", ecl, null, null, null, false);
dayHeadersPending = EntityUtil.filterByAnd(dayHeaders, [statusId : "ORDER_CREATED"]);

dayItemTotal = calcItemTotal(dayHeaders);
dayItemCount = calcItemCount(dayItems);
dayItemTotalPending = calcItemTotal(dayHeadersPending);
dayItemCountPending = calcItemCount(dayItemsPending);
dayItemTotalPaid = dayItemTotal - dayItemTotalPending;
dayItemCountPaid = dayItemCount - dayItemCountPending;
context.dayItemTotal = dayItemTotal;
context.dayItemCount = dayItemCount;
context.dayItemTotalPending = dayItemTotalPending;
context.dayItemCountPending = dayItemCountPending;
context.dayItemTotalPaid = dayItemTotalPaid;
context.dayItemCountPaid = dayItemCountPaid;

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, weekBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, weekEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
weekItems = delegator.findList("OrderHeaderAndItems", ecl, null, null, null, false);
weekItemsPending = EntityUtil.filterByAnd(weekItems, [itemStatusId : "ITEM_ORDERED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, weekBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, weekEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
weekHeaders = delegator.findList("OrderHeader", ecl, null, null, null, false);
weekHeadersPending = EntityUtil.filterByAnd(weekHeaders, [statusId : "ORDER_CREATED"]);

weekItemTotal = calcItemTotal(weekHeaders);
weekItemCount = calcItemCount(weekItems);
weekItemTotalPending = calcItemTotal(weekHeadersPending);
weekItemCountPending = calcItemCount(weekItemsPending);
weekItemTotalPaid = weekItemTotal - weekItemTotalPending;
weekItemCountPaid = weekItemCount - weekItemCountPending;
context.weekItemTotal = weekItemTotal;
context.weekItemCount = weekItemCount;
context.weekItemTotalPending = weekItemTotalPending;
context.weekItemCountPending = weekItemCountPending;
context.weekItemTotalPaid = weekItemTotalPaid;
context.weekItemCountPaid = weekItemCountPaid;

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, monthBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, monthEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
monthItems = delegator.findList("OrderHeaderAndItems", ecl, null, null, null, false);
monthItemsPending = EntityUtil.filterByAnd(monthItems, [itemStatusId : "ITEM_ORDERED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, monthBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, monthEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
monthHeaders = delegator.findList("OrderHeader", ecl, null, null, null, false);
monthHeadersPending = EntityUtil.filterByAnd(monthHeaders, [statusId : "ORDER_CREATED"]);

monthItemTotal = calcItemTotal(monthHeaders);
monthItemCount = calcItemCount(monthItems);
monthItemTotalPending = calcItemTotal(monthHeadersPending);
monthItemCountPending = calcItemCount(monthItemsPending);
monthItemTotalPaid = monthItemTotal - monthItemTotalPending;
monthItemCountPaid = monthItemCount - monthItemCountPending;
context.monthItemTotal = monthItemTotal;
context.monthItemCount = monthItemCount;
context.monthItemTotalPending = monthItemTotalPending;
context.monthItemCountPending = monthItemCountPending;
context.monthItemTotalPaid = monthItemTotalPaid;
context.monthItemCountPaid = monthItemCountPaid;

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                        EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, yearBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, yearEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
yearItems = delegator.findList("OrderHeaderAndItems", ecl, null, null, null, false);
yearItemsPending = EntityUtil.filterByAnd(yearItems, [itemStatusId : "ITEM_ORDERED"]);

ecl = EntityCondition.makeCondition([
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                        EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, yearBegin),
                        EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, yearEnd),
                        EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")],
                    EntityOperator.AND);
yearHeaders = delegator.findList("OrderHeader", ecl, null, null, null, false);
yearHeadersPending = EntityUtil.filterByAnd(yearHeaders, [statusId : "ORDER_CREATED"]);

yearItemTotal = calcItemTotal(yearHeaders);
yearItemCount = calcItemCount(yearItems);
yearItemTotalPending = calcItemTotal(yearHeadersPending);
yearItemCountPending = calcItemCount(yearItemsPending);
yearItemTotalPaid = yearItemTotal - yearItemTotalPending;
yearItemCountPaid = yearItemCount - yearItemCountPending;
context.yearItemTotal = yearItemTotal;
context.yearItemCount = yearItemCount;
context.yearItemTotalPending = yearItemTotalPending;
context.yearItemCountPending = yearItemCountPending;
context.yearItemTotalPaid = yearItemTotalPaid;
context.yearItemCountPaid = yearItemCountPaid;

// order state report
waitingPayment = delegator.findByAnd("OrderHeader", [statusId : "ORDER_CREATED", orderTypeId : "SALES_ORDER"]);
context.waitingPayment = waitingPayment.size();

waitingApproval = delegator.findByAnd("OrderHeader", [statusId : "ORDER_PROCESSING", orderTypeId : "SALES_ORDER"]);
context.waitingApproval = waitingApproval.size();

waitingComplete = delegator.findByAnd("OrderHeader", [statusId : "ORDER_APPROVED", orderTypeId : "SALES_ORDER"]);
context.waitingComplete = waitingComplete.size();
