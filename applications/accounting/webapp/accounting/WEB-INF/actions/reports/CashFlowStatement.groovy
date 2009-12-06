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
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.party.party.PartyWorker;

import javolution.util.FastList;
import javolution.util.FastMap;

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

// Setup the divisions for which the report is executed
List partyIds = PartyWorker.getAssociatedPartyIdsByRelationshipType(delegator, organizationPartyId, 'GROUP_ROLLUP');
partyIds.add(organizationPartyId);

// Get the group of account classes that will be used to position accounts in the proper section of the financial statement
GenericValue cashEquivalentGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "CASH_EQUIVALENT"), true);
List cashEquivalentAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(cashEquivalentGlAccountClass);
GenericValue nonCashExpanseGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "NON_CASH_EXPENSE"), true);
List nonCashExpanseAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(nonCashExpanseGlAccountClass);
GenericValue inventoryAdjustGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "INVENTORY_ADJUST"), true);
List inventoryAdjustAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(inventoryAdjustGlAccountClass);
GenericValue currentAssetGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "CURRENT_ASSET"), true);
List currentAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentAssetGlAccountClass);
GenericValue currentLiabilityGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "CURRENT_LIABILITY"), true);
List currentLiabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentLiabilityGlAccountClass);
GenericValue longTermAssetGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "LONGTERM_ASSET"), true);
List longTermAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(longTermAssetGlAccountClass);
GenericValue accumDepreciationGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "ACCUM_DEPRECIATION"), true);
List accumDepreciationAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(accumDepreciationGlAccountClass);
GenericValue accumAmoritizationGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "ACCUM_AMORTIZATION"), true);
List accumAmoritizationAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(accumAmoritizationGlAccountClass);
GenericValue longTermLiabilityGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "LONGTERM_LIABILITY"), true);
List longTermLiabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(longTermLiabilityGlAccountClass);
GenericValue ownersEquityGlAccountClass = delegator.findOne("GlAccountClass", UtilMisc.toMap("glAccountClassId", "OWNERS_EQUITY"), true);
List ownersEquityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(ownersEquityGlAccountClass);

List mainAndExprs = FastList.newInstance();
mainAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
mainAndExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
mainAndExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
mainAndExprs.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, "PERIOD_CLOSING"));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN, thruDate));

List cashFlowBalanceTotalList = [];

// CASH BALANCE 
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List cashEquivalentAndExprs = FastList.newInstance(mainAndExprs);
cashEquivalentAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, cashEquivalentAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(cashEquivalentAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
cashEquivalentBalanceTotal = balanceTotal;
context.cashEquivalentBalanceList = accountBalanceList;
context.cashEquivalentBalanceList.add("accountName":uiLabelMap.AccountingTotalCashBalance, "balance":balanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingCashBalance", "balance":balanceTotal);

// OPERATING CASH FLOW BALANCE
// NON_CASH_EXPENSE excluding INVENTORY_ADJUST
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List nonCashExpenseAndExprs = FastList.newInstance(mainAndExprs);
nonCashExpenseAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, nonCashExpanseAccountClassIds));
nonCashExpenseAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.NOT_IN, inventoryAdjustAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(nonCashExpenseAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
context.nonCashExpenseBalanceList = accountBalanceList;
context.nonCashExpenseBalanceList.add("accountName":uiLabelMap.AccountingTotalOperationalCashBalance, "balance":balanceTotal);
nonCashExpenseBalanceTotal = balanceTotal;

// CURRENT_ASSET excluding CASH_EQUIVALENT
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List currentAssetAndExprs = FastList.newInstance(mainAndExprs);
currentAssetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentAssetAccountClassIds));
currentAssetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.NOT_IN, cashEquivalentAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(currentAssetAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
context.currentAssetBalanceList = accountBalanceList;
context.currentAssetBalanceList.add("accountName":uiLabelMap.AccountingTotalCurrentAssetBalance, "balance":balanceTotal);
currentAssetBalanceTotal = balanceTotal;

// CURRENT_LIABILITY
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List currentLiabilityAndExprs = FastList.newInstance(mainAndExprs);
currentLiabilityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentLiabilityAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(currentLiabilityAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
context.currentLiabilityBalanceList = accountBalanceList;
context.currentLiabilityBalanceList.add("accountName":uiLabelMap.AccountingTotalCurrentLiabilityBalance, "balance": balanceTotal);
currentLiabilityBalanceTotal = balanceTotal;

// TOTAL OPERATION CASH FLOW = NET INCOME + CURRENT_LIABILITY - NON_CASH_EXPENSE escluding INVENTORY_ADJUST - CURRENT_ASSET excluding CASH_EQUIVALENT
context.totalOperationsCashBalance = ((context.netIncome.add(currentLiabilityBalanceTotal)).subtract(nonCashExpenseBalanceTotal)).subtract(currentAssetBalanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingOperationsCashBalance", "balance":context.totalOperationsCashBalance);

// INVESTING CASH FLOW AMOUNT
// LONGTERM_ASSET && ! ACCUM_DEPRECIATION && ! ACCUM_AMORTIZATION
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List investingCashAndExprs = FastList.newInstance(mainAndExprs);
investingCashAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, longTermAssetAccountClassIds));
investingCashAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.NOT_IN, accumDepreciationAccountClassIds));
investingCashAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.NOT_IN, accumAmoritizationAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(investingCashAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
investingBalanceTotal = balanceTotal;
context.investingCashBalanceList = accountBalanceList;
context.investingCashBalanceList.add("accountName":uiLabelMap.AccountingTotalInvestingCashBalance, "balance":balanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingInvestmentsBalance", "balance":((BigDecimal.ZERO).subtract(balanceTotal)));

// FINANCING CASH FLOW AMOUNT
// LONGTERM_LIABILITY
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List longTermLiabilityAndExprs = FastList.newInstance(mainAndExprs);
longTermLiabilityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, longTermLiabilityAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(longTermLiabilityAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
longTermLiabillityBalanceTotal = balanceTotal; 
context.longTermLiabilityBalanceList = accountBalanceList;
context.longTermLiabilityBalanceList.add("accountName":uiLabelMap.AccountingTotalLongTermLiabilityBalance, "balance":balanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingLongTermLiabilityBalance", "balance":balanceTotal);

// OWNERS_EQUITY
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List ownersEquityAndExprs = FastList.newInstance(mainAndExprs);
ownersEquityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, ownersEquityAccountClassIds));
transactionTotals = delegator.findList("AcctgTransEntrySums", EntityCondition.makeCondition(ownersEquityAndExprs, EntityOperator.AND), UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
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
    balanceTotal = balanceTotalCredit.subtract(balanceTotalDebit);
}
ownersEquityBalanceTotal = balanceTotal;
context.ownersEquityBalanceList = accountBalanceList;
context.ownersEquityBalanceList.add("accountName":uiLabelMap.AccountingOwnersEquityBalance, "balance":balanceTotal);
cashFlowBalanceTotalList.add("totalName":"AccountingOwnersEquityBalance", "balance":balanceTotal);

// CASH FLOW STATEMENT ENDING BALANCE
// ENDING BALANCE = CASH BALANCE + OPERATING CASH BALANCE + LONG TERM LIABILITY BALANCE + OWNERS EQUITY BALANCE - INVESTMENT BALANCE
cashFlowEndingBalance = cashEquivalentBalanceTotal.add(context.totalOperationsCashBalance).add(longTermLiabillityBalanceTotal).add(ownersEquityBalanceTotal).subtract(investingBalanceTotal)
cashFlowBalanceTotalList.add("totalName":"AccountingCashFlowEndingBalance", "balance":cashFlowEndingBalance);
context.cashFlowBalanceTotalList = cashFlowBalanceTotalList;
