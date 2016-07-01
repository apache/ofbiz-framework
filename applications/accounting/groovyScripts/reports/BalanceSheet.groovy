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

import java.sql.Date;
import java.sql.Timestamp;

uiLabelMap = UtilProperties.getResourceBundleMap("AccountingUiLabels", locale);

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
GenericValue assetGlAccountClass = from("GlAccountClass").where("glAccountClassId", "ASSET").cache(true).queryOne();
List assetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(assetGlAccountClass);
GenericValue contraAssetGlAccountClass = from("GlAccountClass").where("glAccountClassId", "CONTRA_ASSET").cache(true).queryOne();
List contraAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(contraAssetGlAccountClass);
GenericValue liabilityGlAccountClass = from("GlAccountClass").where("glAccountClassId", "LIABILITY").cache(true).queryOne();
List liabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(liabilityGlAccountClass);
GenericValue equityGlAccountClass = from("GlAccountClass").where("glAccountClassId", "EQUITY").cache(true).queryOne();
List equityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(equityGlAccountClass);
GenericValue currentAssetGlAccountClass = from("GlAccountClass").where("glAccountClassId", "CURRENT_ASSET").cache(true).queryOne();
List currentAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentAssetGlAccountClass);
GenericValue longtermAssetGlAccountClass = from("GlAccountClass").where("glAccountClassId", "LONGTERM_ASSET").cache(true).queryOne();
List longtermAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(longtermAssetGlAccountClass);
GenericValue currentLiabilityGlAccountClass = from("GlAccountClass").where("glAccountClassId", "CURRENT_LIABILITY").cache(true).queryOne();
List currentLiabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentLiabilityGlAccountClass);
GenericValue accumDepreciationGlAccountClass = from("GlAccountClass").where("glAccountClassId", "ACCUM_DEPRECIATION").cache(true).queryOne();
List accumDepreciationAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(accumDepreciationGlAccountClass);
GenericValue accumAmortizationGlAccountClass = from("GlAccountClass").where("glAccountClassId", "ACCUM_AMORTIZATION").cache(true).queryOne();
List accumAmortizationAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(accumAmortizationGlAccountClass);

// Find the last closed time period to get the fromDate for the transactions in the current period and the ending balances of the last closed period
Map lastClosedTimePeriodResult = runService('findLastClosedDate', ["organizationPartyId": organizationPartyId, "findDate": thruDate,"userLogin": userLogin]);
Timestamp fromDate = (Timestamp)lastClosedTimePeriodResult.lastClosedDate;
if (!fromDate) {
    return;
}
GenericValue lastClosedTimePeriod = (GenericValue)lastClosedTimePeriodResult.lastClosedTimePeriod;
// Get the opening balances of all the accounts
Map assetOpeningBalances = [:];
Map contraAssetOpeningBalances = [:];
Map currentAssetOpeningBalances = [:];
Map longtermAssetOpeningBalances = [:];
Map liabilityOpeningBalances = [:];
Map currentLiabilityOpeningBalances = [:];
Map equityOpeningBalances = [:];
if (lastClosedTimePeriod) {
    List timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, assetAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    List lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        assetOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, contraAssetAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        contraAssetOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, liabilityAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        liabilityOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, equityAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        equityOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentAssetAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        currentAssetOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, longtermAssetAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        longtermAssetOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
    timePeriodAndExprs = [];
    timePeriodAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentLiabilityAccountClassIds));
    timePeriodAndExprs.add(EntityCondition.makeCondition("endingBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));
    timePeriodAndExprs.add(EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriod.customTimePeriodId));
    lastTimePeriodHistories = from("GlAccountAndHistory").where(timePeriodAndExprs).queryList();
    lastTimePeriodHistories.each { lastTimePeriodHistory ->
        Map accountMap = UtilMisc.toMap("glAccountId", lastTimePeriodHistory.glAccountId, "accountCode", lastTimePeriodHistory.accountCode, "accountName", lastTimePeriodHistory.accountName, "balance", lastTimePeriodHistory.getBigDecimal("endingBalance"), "D", lastTimePeriodHistory.getBigDecimal("postedDebits"), "C", lastTimePeriodHistory.getBigDecimal("postedCredits"));
        currentLiabilityOpeningBalances.put(lastTimePeriodHistory.glAccountId, accountMap);
    }
}

List balanceTotalList = [];

List mainAndExprs = [];
mainAndExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
mainAndExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
mainAndExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId));
mainAndExprs.add(EntityCondition.makeCondition("acctgTransTypeId", EntityOperator.NOT_EQUAL, "PERIOD_CLOSING"));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
mainAndExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN, thruDate));

// ASSETS
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List assetAndExprs = mainAndExprs as LinkedList;
assetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, assetAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(assetAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(assetOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // assets are accounts of class DEBIT: the balance is given by debits minus credits
    BigDecimal balance = debitAmount.subtract(creditAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.assetAccountBalanceList = accountBalanceList;
context.assetAccountBalanceList.add(UtilMisc.toMap("accountName", uiLabelMap.AccountingTotalAssets, "balance", balanceTotal));
context.assetBalanceTotal = balanceTotal;

// CURRENT ASSETS
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List currentAssetAndExprs = mainAndExprs as LinkedList;
currentAssetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentAssetAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(currentAssetAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(currentAssetOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // assets are accounts of class DEBIT: the balance is given by debits minus credits
    BigDecimal balance = debitAmount.subtract(creditAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.currentAssetBalanceTotal = balanceTotal;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingCurrentAssets", "balance", balanceTotal));

// LONGTERM ASSETS
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List longtermAssetAndExprs = mainAndExprs as LinkedList;
longtermAssetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, longtermAssetAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(longtermAssetAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(longtermAssetOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // assets are accounts of class DEBIT: the balance is given by debits minus credits
    BigDecimal balance = debitAmount.subtract(creditAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.longtermAssetBalanceTotal = balanceTotal;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingLongTermAssets", "balance", balanceTotal));

// CONTRA ASSETS
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List contraAssetAndExprs = mainAndExprs as LinkedList;
contraAssetAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, contraAssetAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(contraAssetAndExprs).orderBy("glAccountId").queryList();

transactionTotalsMap = [:];
transactionTotalsMap.putAll(contraAssetOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // contra assets are accounts of class CREDIT: the balance is given by credits minus debits
    BigDecimal balance = debitAmount.subtract(creditAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
//context.contraAssetAccountBalanceList = accountBalanceList;
context.assetAccountBalanceList.addAll(accountBalanceList);
context.assetAccountBalanceList.add(UtilMisc.toMap("accountName", uiLabelMap.AccountingTotalAccumulatedDepreciation, "balance", balanceTotal));
context.contraAssetBalanceTotal = balanceTotal;
//balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingLongTermAssetsAtCost", "balance", (context.longtermAssetBalanceTotal - context.contraAssetBalanceTotal)));
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingTotalAccumulatedDepreciation", "balance", balanceTotal));
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingTotalAssets", "balance", (context.currentAssetBalanceTotal + context.longtermAssetBalanceTotal + balanceTotal)));

// LIABILITY
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List liabilityAndExprs = mainAndExprs as LinkedList;
liabilityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, liabilityAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(liabilityAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(liabilityOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // liabilities are accounts of class CREDIT: the balance is given by credits minus debits
    BigDecimal balance = creditAmount.subtract(debitAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.liabilityAccountBalanceList = accountBalanceList;
context.liabilityAccountBalanceList.add(UtilMisc.toMap("accountName", uiLabelMap.AccountingTotalLiabilities, "balance", balanceTotal));
context.liabilityBalanceTotal = balanceTotal;

// CURRENT LIABILITY
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List currentLiabilityAndExprs = mainAndExprs as LinkedList;
currentLiabilityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, currentLiabilityAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(currentLiabilityAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(currentLiabilityOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // liabilities are accounts of class CREDIT: the balance is given by credits minus debits
    BigDecimal balance = creditAmount.subtract(debitAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.currentLiabilityBalanceTotal = balanceTotal;
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingCurrentLiabilities", "balance", balanceTotal));

// EQUITY
// account balances
accountBalanceList = [];
transactionTotals = [];
balanceTotal = BigDecimal.ZERO;
List equityAndExprs = mainAndExprs as LinkedList;
equityAndExprs.add(EntityCondition.makeCondition("glAccountClassId", EntityOperator.IN, equityAccountClassIds));
transactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(equityAndExprs).orderBy("glAccountId").queryList();
transactionTotalsMap = [:];
transactionTotalsMap.putAll(equityOpeningBalances);
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
    BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
    BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
    // equities are accounts of class CREDIT: the balance is given by credits minus debits
    BigDecimal balance = creditAmount.subtract(debitAmount);
    accountMap.put("balance", balance);
    transactionTotalsMap.put(transactionTotal.glAccountId, accountMap);
}
// Add the "retained earnings" account
Map netIncomeResult = runService('prepareIncomeStatement', ["organizationPartyId": organizationPartyId, "glFiscalTypeId": glFiscalTypeId, "fromDate": fromDate, "thruDate": thruDate, "userLogin": userLogin]);
BigDecimal netIncome = (BigDecimal)netIncomeResult.totalNetIncome;
GenericValue retainedEarningsAccount = from("GlAccountTypeDefault").where("glAccountTypeId", "RETAINED_EARNINGS", "organizationPartyId", organizationPartyId).cache(true).queryOne();
if (retainedEarningsAccount) {
    GenericValue retainedEarningsGlAccount = retainedEarningsAccount.getRelatedOne("GlAccount", false);
    transactionTotalsMap.put(retainedEarningsGlAccount.glAccountId, UtilMisc.toMap("glAccountId", retainedEarningsGlAccount.glAccountId,"accountName", retainedEarningsGlAccount.accountName, "accountCode", retainedEarningsGlAccount.accountCode, "balance", netIncome));
}
accountBalanceList = UtilMisc.sortMaps(transactionTotalsMap.values().asList(), UtilMisc.toList("accountCode"));
accountBalanceList.each { accountBalance ->
    balanceTotal = balanceTotal + accountBalance.balance;
}
context.equityAccountBalanceList = accountBalanceList;
context.equityAccountBalanceList.add(UtilMisc.toMap("accountName", uiLabelMap.AccountingTotalEquities, "balance", balanceTotal));
context.equityBalanceTotal = balanceTotal;

context.liabilityEquityBalanceTotal = context.liabilityBalanceTotal + context.equityBalanceTotal
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingEquities", "balance", context.equityBalanceTotal));
balanceTotalList.add(UtilMisc.toMap("totalName", "AccountingTotalLiabilitiesAndEquities", "balance", context.liabilityEquityBalanceTotal));

context.balanceTotalList = balanceTotalList;