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

import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyWorker;

import java.sql.Date;
import java.sql.Timestamp;

if (!fromDate) {
    return;
}
if (!thruDate) {
    thruDate = UtilDateTime.nowTimestamp();
}
if (!parameters.glFiscalTypeId) {
    parameters.glFiscalTypeId = "ACTUAL";
}

uiLabelMap = UtilProperties.getResourceBundleMap("AccountingUiLabels", locale);
parametersFromDate = fromDate;

// Setup the divisions for which the report is executed
List partyIds = PartyWorker.getAssociatedPartyIdsByRelationshipType(delegator, organizationPartyId, 'GROUP_ROLLUP');
partyIds.add(organizationPartyId);

// Get the group of account classes that will be used to position accounts in the proper section of the  Cash Flow statement
GenericValue glAccountClass = from("GlAccountClass").where("glAccountClassId", "CASH_EQUIVALENT").cache(true).queryOne();
List glAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(glAccountClass);

List cashFlowBalanceTotalList = [];

// Find the last closed time period to get the fromDate for the transactions in the current period and the ending balances of the last closed period 
Map lastClosedTimePeriodResult = runService('findLastClosedDate', ["organizationPartyId":organizationPartyId, "findDate":new Date(parametersFromDate.getTime()),"userLogin":userLogin]);
Timestamp periodClosingFromDate = (Timestamp)lastClosedTimePeriodResult.lastClosedDate;
if (!periodClosingFromDate) {
    return;
}
GenericValue lastClosedTimePeriod = (GenericValue)lastClosedTimePeriodResult.lastClosedTimePeriod;
// Get the opening balances of Cash Account
Map openingCashBalances = [:];
if (lastClosedTimePeriod) {
    List timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, glAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    List lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = ["glAccountId":lastTimePeriodHistory.glAccountId, "accountCode":lastTimePeriodHistory.accountCode, "accountName":lastTimePeriodHistory.accountName, "balance":lastTimePeriodHistory.getBigDecimal("endingBalance"), "D":lastTimePeriodHistory.getBigDecimal("postedDebits"), "C":lastTimePeriodHistory.getBigDecimal("postedCredits")];
        openingCashBalances.(lastTimePeriodHistory.glAccountId) = accountMap;
    }
}
List mainAndExprs = [];
mainAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
mainAndExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
mainAndExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
//mainAndExprs.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, "PERIOD_CLOSING"));
mainAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, glAccountClassIds));

// All GlAccount's transactions (from last closing period to parameter's fromDate) 
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List openingCashBalanceAndExprs = mainAndExprs as LinkedList;
openingCashBalanceAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, periodClosingFromDate));
openingCashBalanceAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN, parametersFromDate));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(openingCashBalanceAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(openingCashBalances);
transactionTotals.each { transactionTotal ->
    Map accountMap = (Map)transactionTotalsMap.get(transactionTotal.glAccountId);
    if (!accountMap) {
        accountMap = UtilMisc.makeMapWritable(transactionTotal);
        accountMap.remove("debitCreditFlag");
        accountMap.remove("amount");
        accountMap.D = BigDecimal.ZERO;
        accountMap.C = BigDecimal.ZERO;
        accountMap.balance = BigDecimal.ZERO;
    }
    if (accountMap.debitCreditFlag && accountMap.amount) {
        accountMap.remove("debitCreditFlag");
        accountMap.remove("amount");
    }
    if (transactionTotal.debitCreditFlag == "C") {
        accountMap.C = ((BigDecimal)accountMap.get("C")).add(transactionTotal.amount);
        accountMap.balance = (accountMap.balance).subtract(transactionTotal.amount);
    } else {
        accountMap.D = ((BigDecimal)accountMap.get("D")).add(transactionTotal.amount);
        accountMap.balance = (accountMap.balance).add(transactionTotal.amount);
    }

    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
glAccountIdList = [];
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal.add(accountBalance.balance);
}
openingCashBalanceTotal = balanceTotal;
context.openingCashBalanceList = accountBalanceList;
cashFlowBalanceTotalList.add("totalName":"AccountingOpeningCashBalance", "balance":balanceTotal);
openingTransactionKeySet = transactionTotalsMap.keySet();

// PERIOD CASH BALANCE 
// GlAccounts from parameter's fromDate to parameter's thruDate.
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List periodCashBalanceAndExprs = mainAndExprs as LinkedList;
periodCashBalanceAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, parametersFromDate));
periodCashBalanceAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN, thruDate));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(periodCashBalanceAndExprs).orderBy("glAccountId").queryList();
if (transactionTotals) {
    Map transactionTotalsMap = [:];
    balanceTotalCredit = BigDecimal.ZERO;
    balanceTotalDebit = BigDecimal.ZERO;
    transactionTotals.each { transactionTotal ->
        Map accountMap = (Map)transactionTotalsMap.get(transactionTotal.glAccountId);
        if (!accountMap) {
            accountMap = UtilMisc.makeMapWritable(transactionTotal);
            accountMap.remove("debitCreditFlag");
            accountMap.remove("amount");
            accountMap.D = BigDecimal.ZERO;
            accountMap.C = BigDecimal.ZERO;
            accountMap.balance = BigDecimal.ZERO;
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.D;
        BigDecimal creditAmount = (BigDecimal)accountMap.C;
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.balance = balance;
        transactionTotalsMap.(transactionTotal.glAccountId) = accountMap;
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
periodCashBalanceTotal = balanceTotal;
context.periodCashBalanceList = accountBalanceList;
context.periodCashBalanceList.add("accountName":uiLabelMap.AccountingTotalPeriodCashBalance, "balance":balanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingPeriodCashBalance", "balance":balanceTotal);

// CLOSING BALANCE 
// GlAccounts from parameter's fromDate to parameter's thruDate.
accountBalanceList = [];
balanceTotal = BigDecimal.ZERO;
List transactionTotals = [];
transactionTotals.addAll(new LinkedList(context.openingCashBalanceList));
transactionTotals.addAll(new LinkedList(context.periodCashBalanceList));
transactionTotals = UtilMisc.sortMaps(transactionTotals, UtilMisc.toList("accountCode"));
closingTransactionKeySet = [];
if (transactionTotals) {
    Map transactionTotalsMap = [:];
    balanceTotalCredit = BigDecimal.ZERO;
    balanceTotalDebit = BigDecimal.ZERO;
    transactionTotals.each { transactionTotal ->
        if (transactionTotal.D != null) {
            if (transactionTotalsMap.(transactionTotal.glAccountId)) {
                totalDebitBalance = (transactionTotal.D).add(transactionTotalsMap.(transactionTotal.glAccountId).D);
                totalCreditBalance = (transactionTotal.C).add(transactionTotalsMap.(transactionTotal.glAccountId).C);
                if (transactionTotalsMap.(transactionTotal.glAccountId).D == 0 && transactionTotalsMap.(transactionTotal.glAccountId).C == 0) {
                    transactionTotalsMap.(transactionTotal.glAccountId).balance = (transactionTotal.balance).add(transactionTotalsMap.(transactionTotal.glAccountId).balance);
                } else {
                    transactionTotalsMap.(transactionTotal.glAccountId).D = totalDebitBalance;
                    transactionTotalsMap.(transactionTotal.glAccountId).C = totalCreditBalance;
                    transactionTotalsMap.(transactionTotal.glAccountId).balance = totalDebitBalance.subtract(totalCreditBalance);
                }
            } else {
                transactionTotalsMap.(transactionTotal.glAccountId) = transactionTotal;
            }
            accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
        }
    }
    closingTransactionKeySet = transactionTotalsMap.keySet();
}
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal.add(accountBalance.balance);
}
//closingCashBalanceTotal = balanceTotal;
context.closingCashBalanceList = accountBalanceList;
context.closingCashBalanceList.add("accountName":uiLabelMap.AccountingTotalClosingCashBalance, "balance":balanceTotal);

// Get differences of glAccount in closing and opening list and then add difference to opening list.
if (closingTransactionKeySet) {
    closingTransactionKeySet.removeAll(openingTransactionKeySet);
    closingTransactionKeySet.each { closingTransactionKey ->
        glAccount = from("GlAccount").where("glAccountId", closingTransactionKey).cache(true).queryOne();
        context.openingCashBalanceList.add(["glAccountId":glAccount.glAccountId, "accountName":glAccount.accountName, accountCode:glAccount.accountCode, balance:BigDecimal.ZERO, D:BigDecimal.ZERO, C:BigDecimal.ZERO]);
    }
}
context.openingCashBalanceList.add(["accountName":uiLabelMap.AccountingTotalOpeningCashBalance, "balance":openingCashBalanceTotal]);

// CASH FLOW STATEMENT ENDING BALANCE
// ENDING BALANCE = OPENING CASH BALANCE + PERIOD CASH BALANCE 
endingCashBalanceTotal = openingCashBalanceTotal.add(periodCashBalanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingEndingCashBalance", "balance":endingCashBalanceTotal);
context.cashFlowBalanceTotalList = cashFlowBalanceTotalList;
