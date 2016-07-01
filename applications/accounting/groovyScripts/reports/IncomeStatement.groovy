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

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.party.party.PartyWorker;

if (!fromDate) {
    return;
}
if (!thruDate) {
    thruDate = UtilDateTime.nowTimestamp();
}
if (!glFiscalTypeId) {
    return;
}
organizationPartyId =null
if(context.organizationPartyId) {
    organizationPartyId = context.organizationPartyId;
} else {
    organizationPartyId = parameters.get('ApplicationDecorator|organizationPartyId')
}

// Setup the divisions for which the report is executed
List partyIds = PartyWorker.getAssociatedPartyIdsByRelationshipType(delegator, organizationPartyId, 'GROUP_ROLLUP');
partyIds.add(organizationPartyId);

// Get the group of account classes that will be used to position accounts in the proper section of the financial statement
GenericValue revenueGlAccountClass = from("GlAccountClass").where("glAccountClassId", "REVENUE").cache(true).queryOne();
List revenueAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(revenueGlAccountClass);
GenericValue contraRevenueGlAccountClass = from("GlAccountClass").where("glAccountClassId", "CONTRA_REVENUE").cache(true).queryOne();
List contraRevenueAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(contraRevenueGlAccountClass);
GenericValue incomeGlAccountClass = from("GlAccountClass").where("glAccountClassId", "INCOME").cache(true).queryOne();
List incomeAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(incomeGlAccountClass);
GenericValue expenseGlAccountClass = from("GlAccountClass").where("glAccountClassId", "EXPENSE").cache(true).queryOne();
List expenseAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(expenseGlAccountClass);
GenericValue cogsExpenseGlAccountClass = from("GlAccountClass").where("glAccountClassId", "COGS_EXPENSE").cache(true).queryOne();
List cogsExpenseAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(cogsExpenseGlAccountClass);
GenericValue sgaExpenseGlAccountClass = from("GlAccountClass").where("glAccountClassId", "SGA_EXPENSE").cache(true).queryOne();
List sgaExpenseAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(sgaExpenseGlAccountClass);
GenericValue depreciationGlAccountClass = from("GlAccountClass").where("glAccountClassId", "DEPRECIATION").cache(true).queryOne();
List depreciationAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(depreciationGlAccountClass);

List mainAndExprs = [];
mainAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
mainAndExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
mainAndExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId));
mainAndExprs.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, "PERIOD_CLOSING"));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN, thruDate));

List balanceTotalList = [];

// REVENUE
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List revenueAndExprs = mainAndExprs as LinkedList;
revenueAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, revenueAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(revenueAndExprs).orderBy("glAccountId").queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // revenues are accounts of class CREDIT: the balance is given by credits minus debits
        BigDecimal balance = creditAmount.subtract(debitAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    // revenues are accounts of class CREDIT: the balance is given by credits minus debits
    balanceTotal = balanceTotalCredit.subtract(balanceTotalDebit);
}
context.revenueAccountBalanceList = accountBalanceList;
context.revenueAccountBalanceList.add(UtilMisc.toMap("accountName", "TOTAL REVENUES", "balance", balanceTotal));
context.revenueBalanceTotal = balanceTotal;

// CONTRA REVENUE
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List contraRevenueAndExprs = mainAndExprs as LinkedList;
contraRevenueAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, contraRevenueAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(contraRevenueAndExprs).orderBy("glAccountId").queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // contra revenues are accounts of class DEBIT: the balance is given by debits minus credits
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    // contra revenues are accounts of class DEBIT: the balance is given by debits minus credits
    balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
context.contraRevenueBalanceTotal = balanceTotal;
balanceTotalList.add(UtilMisc.toMap("totalName", "TOTAL CONTRA REVENUE", "balance", balanceTotal));

// EXPENSE
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List expenseAndExprs = mainAndExprs as LinkedList;
expenseAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, expenseAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(expenseAndExprs).queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // expenses are accounts of class DEBIT: the balance is given by debits minus credits
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    // expenses are accounts of class DEBIT: the balance is given by debits minus credits
    balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
context.expenseAccountBalanceList = accountBalanceList;
context.expenseAccountBalanceList.add(UtilMisc.toMap("accountName", "TOTAL EXPENSES", "balance", balanceTotal));
context.expenseBalanceTotal = balanceTotal;

// COST OF GOODS SOLD (COGS_EXPENSE)
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List cogsExpenseAndExprs = mainAndExprs as LinkedList;
cogsExpenseAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, cogsExpenseAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(cogsExpenseAndExprs).orderBy("glAccountId").queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // expenses are accounts of class DEBIT: the balance is given by debits minus credits
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    // expenses are accounts of class DEBIT: the balance is given by debits minus credits
    balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
context.cogsExpense = balanceTotal;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingCostOfGoodsSold", "balance", balanceTotal));

// OPERATING EXPENSES (SGA_EXPENSE)
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List sgaExpenseAndExprs = mainAndExprs as LinkedList;
sgaExpenseAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, sgaExpenseAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(sgaExpenseAndExprs).orderBy("glAccountId").queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // expenses are accounts of class DEBIT: the balance is given by debits minus credits
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
    // expenses are accounts of class DEBIT: the balance is given by debits minus credits
    balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
sgaExpense = balanceTotal;

//DEPRECIATION (DEPRECIATION)
//account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List depreciationAndExprs = mainAndExprs as LinkedList;
depreciationAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, depreciationAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(depreciationAndExprs).orderBy("glAccountId").queryList();
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
       accountMap.put("D", BigDecimal.ZERO);
       accountMap.put("C", BigDecimal.ZERO);
       accountMap.put("balance", BigDecimal.ZERO);
   }
   UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
   if ("D".equals(transactionTotal.debitCreditFlag)) {
       balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
   } else {
       balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
   }
   BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
   BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
   // expenses are accounts of class DEBIT: the balance is given by debits minus credits
   BigDecimal balance = debitAmount.subtract(creditAmount);
   accountMap.put("balance", balance);
   transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
// expenses are accounts of class DEBIT: the balance is given by debits minus credits
balanceTotal = balanceTotalDebit.subtract(balanceTotalCredit);
}
depreciation = balanceTotal;

// INCOME
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List incomeAndExprs = mainAndExprs as LinkedList;
incomeAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, incomeAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(incomeAndExprs).orderBy("glAccountId").queryList();
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
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
            accountMap.put("balance", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, transactionTotal.debitCreditFlag, transactionTotal.amount);
        if ("D".equals(transactionTotal.debitCreditFlag)) {
            balanceTotalDebit = balanceTotalDebit.add(transactionTotal.amount);
        } else {
            balanceTotalCredit = balanceTotalCredit.add(transactionTotal.amount);
        }
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        // income are accounts of class CREDIT: the balance is given by credits minus debits
        BigDecimal balance = creditAmount.subtract(debitAmount);
        accountMap.put("balance", balance);
        transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
    }
    accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));    
    // incomes are accounts of class CREDIT: the balance is given by credits minus debits
    balanceTotal = balanceTotalCredit.subtract(balanceTotalDebit);
}
context.incomeAccountBalanceList = accountBalanceList;
context.incomeAccountBalanceList.add(UtilMisc.toMap("accountName", "TOTAL INCOME", "balance", balanceTotal));
context.incomeBalanceTotal = balanceTotal;

// NET SALES = REVENUES - CONTRA REVENUES
context.netSales = (context.revenueBalanceTotal).subtract(context.contraRevenueBalanceTotal);
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingTotalNetSales", "balance", context.netSales));
// GROSS MARGIN = NET SALES - COSTS OF GOODS SOLD
context.grossMargin = (context.netSales).subtract(context.cogsExpense);
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingGrossMargin", "balance", context.grossMargin));
// OPERATING EXPENSES
context.sgaExpense = sgaExpense;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingOperatingExpenses", "balance", context.sgaExpense));
// DEPRECIATION
context.depreciation = depreciation;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingDepreciation", "balance", context.depreciation));
// INCOME FROM OPERATIONS = GROSS MARGIN - OPERATING EXPENSES
context.incomeFromOperations = (context.grossMargin).subtract(context.sgaExpense);
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingIncomeFromOperations", "balance", context.incomeFromOperations));
// NET INCOME
context.netIncome = (context.netSales).add(context.incomeBalanceTotal).subtract(context.expenseBalanceTotal);
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingNetIncome", "balance", context.netIncome));

context.balanceTotalList = balanceTotalList;
