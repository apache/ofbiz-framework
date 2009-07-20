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


context.monthList = ExpressionUiHelper.getMonthValueList(locale);
context.month = (ExpressionUiHelper.getMonthValueList(locale)).get(UtilDateTime.getMonth(UtilDateTime.nowTimestamp(), timeZone, locale));
if (organizationPartyId && parameters.selectedMonth) {
    
    selectedMonth = Integer.valueOf(parameters.selectedMonth);
    selectedMonthDate = UtilDateTime.toTimestamp((selectedMonth + 1), 1, UtilDateTime.getYear(UtilDateTime.nowTimestamp(), timeZone, locale), 0, 0, 0);

    selectedMonthStartDate = UtilDateTime.getMonthStart(selectedMonthDate, timeZone, locale);
    selectedMonthEndDate = UtilDateTime.getMonthEnd(selectedMonthDate, timeZone, locale);
    if (parameters.isIncomeStatement) {
        Timestamp incomeStatementFromDate = null;
        Timestamp incomeStatementThruDate = null;
        if (parameters.fromDate && parameters.thruDate) {
            incomeStatementFromDate = Timestamp.valueOf(parameters.fromDate);
            incomeStatementThruDate = Timestamp.valueOf(parameters.thruDate);
        } else {
            incomeStatementFromDate =  selectedMonthStartDate;
            incomeStatementThruDate = selectedMonthEndDate;
        }
        prepareIncomeStatement = dispatcher.runSync("prepareIncomeStatement", 
                [fromDate : incomeStatementFromDate, thruDate : incomeStatementThruDate, organizationPartyId : organizationPartyId, glFiscalTypeId : parameters.glFiscalTypeId, userLogin : userLogin]);
        if (prepareIncomeStatement) {
            context.glAccountTotalsList = prepareIncomeStatement.glAccountTotalsList;
            context.totalNetIncome = prepareIncomeStatement.totalNetIncome;
        }
    }
    onlyIncludePeriodTypeIdList = [];
    onlyIncludePeriodTypeIdList.add("FISCAL_YEAR");
    customTimePeriodResult = dispatcher.runSync("findCustomTimePeriods", [findDate : selectedMonthDate, organizationPartyId : organizationPartyId, onlyIncludePeriodTypeIdList : onlyIncludePeriodTypeIdList, userLogin : userLogin]);

    if (customTimePeriodResult) {
        customTimePeriod = EntityUtil.getFirst(customTimePeriodResult.customTimePeriodList);
        if (customTimePeriod) {
            fromDate = new Timestamp((customTimePeriod.fromDate).getTime());
            thruDate = new Timestamp((customTimePeriod.thruDate).getTime());
            customTimePeriodFromDate = new Timestamp((customTimePeriod.fromDate).getTime());
            if (selectedMonthStartDate.compareTo(fromDate) > 0) {
                fromDate =  selectedMonthStartDate;
            }
            if (selectedMonthEndDate.compareTo(thruDate) < 0) {
                thruDate =  selectedMonthEndDate;
            }
            context.monthlyTrialBalanceFromDate = fromDate;
            context.monthlyTrialBalanceThruDate = thruDate;
            context.financialYearFromDate = customTimePeriodFromDate;
        }
    }
}
