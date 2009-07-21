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

import java.sql.Timestamp;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.calendar.ExpressionUiHelper;


fromDate=null;
thruDate=null;

if (parameters.fromDate && parameters.thruDate) {
  fromDate = Timestamp.valueOf(parameters.fromDate);
  thruDate = Timestamp.valueOf(parameters.thruDate);
} else if (parameters.selectedMonth) {
  selectedMonth = Integer.valueOf(parameters.selectedMonth);
  selectedMonthDate = UtilDateTime.toTimestamp((selectedMonth + 1), 1, UtilDateTime.getYear(UtilDateTime.nowTimestamp(), timeZone, locale), 0, 0, 0);
  fromDate = UtilDateTime.getMonthStart(selectedMonthDate, timeZone, locale);
  thruDate = UtilDateTime.getMonthEnd(selectedMonthDate, timeZone, locale);
} else {
  context.selectedMonth =   UtilDateTime.getMonth(UtilDateTime.nowTimestamp(), timeZone, locale);
}

if(fromDate && thruDate && organizationPartyId) {

  onlyIncludePeriodTypeIdList = [];
  onlyIncludePeriodTypeIdList.add("FISCAL_YEAR");
  customTimePeriodResult = dispatcher.runSync("findCustomTimePeriods", [findDate : thruDate, organizationPartyId : organizationPartyId, onlyIncludePeriodTypeIdList : onlyIncludePeriodTypeIdList, userLogin : userLogin]);

  if (customTimePeriodResult) {
      customTimePeriod = EntityUtil.getFirst(customTimePeriodResult.customTimePeriodList);
      if (customTimePeriod) {
          customTimePeriodFromDate = new Timestamp((customTimePeriod.fromDate).getTime());
          customTimePeriodThruDate = new Timestamp((customTimePeriod.thruDate).getTime());

          if (customTimePeriodFromDate.compareTo(fromDate) > 0) {

              fromDate =  customTimePeriodFromDate;
          }
          if (customTimePeriodThruDate.compareTo(thruDate) < 0) {
              thruDate =  customTimePeriodThruDate;
          }
          context.financialYearFromDate = customTimePeriodFromDate;
      }
  }


  if (parameters.isIncomeStatement) {
      prepareIncomeStatement = dispatcher.runSync("prepareIncomeStatement",
              [fromDate : fromDate, thruDate : thruDate, organizationPartyId : organizationPartyId, glFiscalTypeId : parameters.glFiscalTypeId, userLogin : userLogin]);
      context.glAccountTotalsList = prepareIncomeStatement.glAccountTotalsList;
      context.totalNetIncome = prepareIncomeStatement.totalNetIncome;
  }
}

context.fromDate = fromDate;
context.thruDate = thruDate;
context.monthList = ExpressionUiHelper.getMonthValueList(locale);
