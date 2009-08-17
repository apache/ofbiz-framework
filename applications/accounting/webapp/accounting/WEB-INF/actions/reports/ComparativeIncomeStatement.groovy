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
import java.sql.Timestamp;
import org.ofbiz.entity.util.EntityUtil;

period1FromDate = parameters.period1FromDate;
period1ThruDate = parameters.period1ThruDate;
period2FromDate = parameters.period2FromDate;
period2ThruDate = parameters.period2ThruDate;

if (period1FromDate && period1ThruDate && organizationPartyId && period2FromDate && period2ThruDate) {
    onlyIncludePeriodTypeIdList = [];
    onlyIncludePeriodTypeIdList.add("FISCAL_YEAR");
    glAccountIncomeList = [];
    glAccountExpenseList = [];
    periodExpenses = [];
    periodIncomes = [];
    period1IncomeStatement = getGlAccountTotals(onlyIncludePeriodTypeIdList, Timestamp.valueOf(period1FromDate), Timestamp.valueOf(period1ThruDate), organizationPartyId, parameters.glFiscalTypeId);
    period1Expenses = [];
    period1Incomes = [];
    if (period1IncomeStatement) {
        context.period1TotalNetIncome = period1IncomeStatement.totalNetIncome;
        glAccountTotalsMapForPeriod1 = period1IncomeStatement.glAccountTotalsMap;
        if (glAccountTotalsMapForPeriod1) {
            period1Expenses = glAccountTotalsMapForPeriod1.expenses;
            period1Incomes = glAccountTotalsMapForPeriod1.income;
            if (period1Incomes) 
                periodIncomes.addAll(period1Incomes);
            if (period1Expenses) 
                periodExpenses.addAll(period1Expenses);
        }
    }
    period2IncomeStatement = getGlAccountTotals(onlyIncludePeriodTypeIdList ,Timestamp.valueOf(period2FromDate) ,Timestamp.valueOf(period2ThruDate) ,organizationPartyId , parameters.glFiscalTypeId);
    period2Expenses = [];
    period2Incomes = [];
    if (period2IncomeStatement) {
        context.period2TotalNetIncome = period2IncomeStatement.totalNetIncome;
        glAccountTotalsMapForPeriod2 = period2IncomeStatement.glAccountTotalsMap;
        if (glAccountTotalsMapForPeriod2) {
            period2Expenses = glAccountTotalsMapForPeriod2.expenses;
            period2Incomes = glAccountTotalsMapForPeriod2.income;
            period2Expenses.each { period2Expense ->
                if (!((periodExpenses.glAccountId).contains(period2Expense.glAccountId)))
                    periodExpenses.add(period2Expense);
            }
            period2Incomes.each { period2Income ->
                if (!((periodIncomes.glAccountId).contains(period2Income.glAccountId)))
                    periodIncomes.add(period2Income);
            }
        }        
    }
    periodExpenses.each { periodExpense ->
        period1TotalAmount = BigDecimal.ZERO;
        period2TotalAmount = BigDecimal.ZERO; 
        if ((period1Expenses.glAccountId).contains(periodExpense.glAccountId)) {
            period1Expenses.each { period1Expense ->
                if(periodExpense.glAccountId.equals(period1Expense.glAccountId))
                    period1TotalAmount = period1Expense.totalAmount; 
            }
        }
        if ((period2Expenses.glAccountId).contains(periodExpense.glAccountId)) {
            period2Expenses.each { period2Expense ->
                if(periodExpense.glAccountId.equals(period2Expense.glAccountId))
                    period2TotalAmount = period2Expense.totalAmount; 
            }
        }
        glAccountExpenseList.add([glAccountId : periodExpense.glAccountId , period1TotalAmount : period1TotalAmount , period2TotalAmount : period2TotalAmount]);
        context.glAccountExpenseList = glAccountExpenseList;
    }
    periodIncomes.each { periodIncome ->
        period1TotalAmount = BigDecimal.ZERO;
        period2TotalAmount = BigDecimal.ZERO;
        if ((period1Incomes.glAccountId).contains(periodIncome.glAccountId)) {
            period1Incomes.each { period1Income ->
                if(periodIncome.glAccountId.equals(period1Income.glAccountId))
                    period1TotalAmount = period1Income.totalAmount; 
            }
        }
        if ((period2Incomes.glAccountId).contains(periodIncome.glAccountId)) {
            period2Incomes.each { period2Income ->
                if(periodIncome.glAccountId.equals(period2Income.glAccountId))
                    period2TotalAmount = period2Income.totalAmount; 
            }
        }
        glAccountIncomeList.add([glAccountId : periodIncome.glAccountId , period1TotalAmount : period1TotalAmount , period2TotalAmount : period2TotalAmount]);
        context.glAccountIncomeList = glAccountIncomeList;
    }
}

private Map getGlAccountTotals(List onlyIncludePeriodTypeIdList, Timestamp fromDate, Timestamp thruDate, String organizationPartyId, String glFiscalTypeId) {
    customTimePeriodResult = dispatcher.runSync("findCustomTimePeriods", [findDate : thruDate, organizationPartyId : organizationPartyId, onlyIncludePeriodTypeIdList : onlyIncludePeriodTypeIdList, userLogin : userLogin]);
    if (customTimePeriodResult) {
        customTimePeriod = EntityUtil.getFirst(customTimePeriodResult.customTimePeriodList);
        if (customTimePeriod) {
            customTimePeriodFromDate = new Timestamp((customTimePeriod.fromDate).getTime());
            customTimePeriodThruDate = new Timestamp((customTimePeriod.thruDate).getTime());
            if (customTimePeriodFromDate.compareTo(fromDate) > 0) 
                fromDate =  customTimePeriodFromDate;
            if (customTimePeriodThruDate.compareTo(thruDate) < 0) 
                thruDate =  customTimePeriodThruDate;
            context.financialYearFromDate = customTimePeriodFromDate;
        }
        prepareIncomeStatement = dispatcher.runSync("prepareIncomeStatement",
                [fromDate : fromDate, thruDate : thruDate, organizationPartyId : organizationPartyId, glFiscalTypeId : glFiscalTypeId, userLogin : userLogin]);
        return prepareIncomeStatement;
    }
}