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
package org.apache.ofbiz.accounting.reports

import org.apache.ofbiz.accounting.util.UtilAccounting
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityExpr
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.party.party.PartyWorker

import java.sql.Timestamp

uiLabelMap = UtilProperties.getResourceBundleMap('AccountingUiLabels', locale)

thruDate = thruDate ?: UtilDateTime.nowTimestamp()
if (!glFiscalTypeId) {
    return
}
organizationPartyId = null
if (context.organizationPartyId) {
    organizationPartyId = context.organizationPartyId
} else {
    organizationPartyId = parameters.get('ApplicationDecorator|organizationPartyId')
}
// Setup the divisions for which the report is executed
List partyIds = PartyWorker.getAssociatedPartyIdsByRelationshipType(delegator, organizationPartyId, 'GROUP_ROLLUP')
partyIds.add(organizationPartyId)

// Get the group of account classes that will be used to position accounts in the proper section of the financial statement
GenericValue assetGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'ASSET').cache(true).queryOne()
List assetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(assetGlAccountClass)
GenericValue contraAssetGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'CONTRA_ASSET').cache(true).queryOne()
List contraAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(contraAssetGlAccountClass)
GenericValue liabilityGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'LIABILITY').cache(true).queryOne()
List liabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(liabilityGlAccountClass)
GenericValue equityGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'EQUITY').cache(true).queryOne()
List equityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(equityGlAccountClass)
GenericValue currentAssetGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'CURRENT_ASSET').cache(true).queryOne()
List currentAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentAssetGlAccountClass)
GenericValue longtermAssetGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'LONGTERM_ASSET').cache(true).queryOne()
List longtermAssetAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(longtermAssetGlAccountClass)
GenericValue currentLiabilityGlAccountClass = from('GlAccountClass').where('glAccountClassId', 'CURRENT_LIABILITY').cache(true).queryOne()
List currentLiabilityAccountClassIds = UtilAccounting.getDescendantGlAccountClassIds(currentLiabilityGlAccountClass)

// Find the last closed time period to get the fromDate for the transactions in the current period and the ending balances of the last closed period
Map lastClosedTimePeriodResult = runService('findLastClosedDate',
        ['organizationPartyId': organizationPartyId, 'findDate': thruDate, 'userLogin': userLogin])
Timestamp fromDate = (Timestamp)lastClosedTimePeriodResult.lastClosedDate
if (!fromDate) {
    return
}

GenericValue lastClosedTimePeriod = (GenericValue)lastClosedTimePeriodResult.lastClosedTimePeriod

class AccountBalance {

    String glAccountId
    String accountCode
    String accountName
    BigDecimal balance
    Map asMap() {
        [glAccountId: glAccountId, accountCode: accountCode, accountName: accountName, balance: balance]
    }

}

/**
 * Closure to retrieve a map of AccountBalances for the organization's GL Account which were active during the most
 * recently closed time period - i.e. those accounts for which GlAccountHistory exists.
 *
 * AccountBalances are returned for those accounts which match the accountClassIds parameter.
 *
 * @param accountClassIds The set of GL Account Class IDs to return Ending Balances for.
 *
 * @return Map of GL Account IDs to AccountBalances for the lastClosedTimePeriod, or an empty map if
 *  lastClosedTimePeriod is null
 */
Closure<Map<String, AccountBalance>> getLastPeriodClosingBalancesForAccountClassIds = { List<String> accountClassIds ->
    Map<String, AccountBalance> retVal = [:]

    if (lastClosedTimePeriod) {
        List<EntityExpr> lastPeriodHistoryConditions = [
                EntityCondition.makeCondition('organizationPartyId', EntityOperator.IN, partyIds),
                EntityCondition.makeCondition('glAccountClassId', EntityOperator.IN, accountClassIds),
                EntityCondition.makeCondition('endingBalance', EntityOperator.NOT_EQUAL, BigDecimal.ZERO),
                EntityCondition.makeCondition('customTimePeriodId', EntityOperator.EQUALS,
                        lastClosedTimePeriod.customTimePeriodId)
        ]

        from('GlAccountAndHistory').where(lastPeriodHistoryConditions).queryList().collect {
            history ->
                new AccountBalance(
                        glAccountId: history.glAccountId,
                        accountCode: history.accountCode,
                        accountName: history.accountName,
                        balance: history.getBigDecimal('endingBalance'),
                )
        }.each {
            retVal.put(it.glAccountId, it)
        }
    }

    retVal
}

// Get the opening balances of all the accounts
Map<String, AccountBalance> assetOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(assetAccountClassIds)
Map<String, AccountBalance> contraAssetOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(contraAssetAccountClassIds)
Map<String, AccountBalance> currentAssetOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(currentAssetAccountClassIds)
Map<String, AccountBalance> longtermAssetOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(longtermAssetAccountClassIds)
Map<String, AccountBalance> liabilityOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(liabilityAccountClassIds)
Map<String, AccountBalance> currentLiabilityOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(currentLiabilityAccountClassIds)
Map<String, AccountBalance> equityOpeningBalances = getLastPeriodClosingBalancesForAccountClassIds(equityAccountClassIds)

List balanceTotalList = []

class AccountEntrySum {

    String glAccountId
    String accountCode
    String accountName
    String debitCreditFlag
    BigDecimal amount

}

/**
 * Retrieve a collection of AccountEntrySum objects corresponding to the AcctgTransEntrySums entities query controlled
 * by the given conditions.
 *
 * @param conditions The list of conditions to be ANDed together and form the WHERE clause for the query of
 * AcctgTransEntrySums.
 *
 * @return A collection of AccountEntrySum objects for the conditions.
 */
Closure<List<AccountEntrySum>> getAccountEntrySumsForCondition = { Collection<EntityExpr> conditions ->
    from('AcctgTransEntrySums')
            .where(conditions)
            .orderBy('glAccountId')
            .select('glAccountId', 'accountName', 'accountCode', 'debitCreditFlag', 'amount')
            .queryList()
            .collect { entrySum ->
                new AccountEntrySum(
                        glAccountId: entrySum.glAccountId,
                        accountName: entrySum.accountName,
                        accountCode: entrySum.accountCode,
                        debitCreditFlag: entrySum.debitCreditFlag,
                        amount: entrySum.getBigDecimal('amount')
                )
            }
}

/**
 * Retrieve a collection of AccountEntrySum objects corresponding to the organization's AcctgTransEntrySums entities
 * which match the given collection of Account Class IDs.
 *
 * @param accountClassIds The collection of Account Class IDs to filter by.
 *
 * @return A collection of AccountEntrySum objects corresponding to the given accountClassIds.
 */
Closure<List<AccountEntrySum>> getAccountEntrySumsForClassIds = { Collection<String> accountClassIds ->
    List conditions = [
            EntityCondition.makeCondition('glAccountClassId', EntityOperator.IN, accountClassIds),
            EntityCondition.makeCondition('organizationPartyId', EntityOperator.IN, partyIds),
            EntityCondition.makeCondition('isPosted', EntityOperator.EQUALS, 'Y'),
            EntityCondition.makeCondition('glFiscalTypeId', EntityOperator.EQUALS, glFiscalTypeId),
            EntityCondition.makeCondition('acctgTransTypeId', EntityOperator.NOT_EQUAL, 'PERIOD_CLOSING'),
            EntityCondition.makeCondition('transactionDate', EntityOperator.GREATER_THAN_EQUAL_TO, fromDate),
            EntityCondition.makeCondition('transactionDate', EntityOperator.LESS_THAN, thruDate)
    ]

    getAccountEntrySumsForCondition(conditions)
}

enum RootClass { DEBIT, CREDIT }

/**
 * Calculates balances of the organization's GL Accounts which correspond to the given collection of Account Class IDs.
 * Balances are calculated by taking each account's opening balance from the given Map, and then adding all debit and
 * credit transaction entries from the current time period.
 *
 * @param openingBalances Map of GL Account IDs to AccountBalance objects representing the opening balance of the GL
 * Account for the current time period.
 * @param accountClassIds The collection of Account Class IDs used to define the queried GL Accounts.
 * @param rootClass Define whether the collection of Account Class IDs should be treated as Debit or Credit accounts.
 * This controls how the balance of the account is calculated:
 *  Debit account balance = totalDebits - totalCredits
 *  Credit account balance = totalCredits - totalDebits
 * @param negateBalances Specify whether balances should be negated after they have been calculated according to the
 * debit/credit flag of any accounts for which transaction entries are found.
 */
Closure<Map<String,AccountBalance>> calculateBalances = { Map<String, AccountBalance> openingBalances,
                                                          Collection<String> accountClassIds,
                                                          RootClass rootClass,
                                                          boolean negateBalances = false ->

    Map<String, AccountBalance> accountBalancesByGlAccountId = [*:openingBalances]

    getAccountEntrySumsForClassIds(accountClassIds).each { entrySum ->
        AccountBalance existingAccountBalance = accountBalancesByGlAccountId.getOrDefault(
                entrySum.glAccountId,
                new AccountBalance(
                        glAccountId: entrySum.glAccountId,
                        accountCode: entrySum.accountCode,
                        accountName: entrySum.accountName,
                        balance: 0.0
                ))

        BigDecimal transactionSumsDebitAmount = entrySum.debitCreditFlag == 'D' ? entrySum.amount : 0.0
        BigDecimal transactionSumsCreditAmount = entrySum.debitCreditFlag == 'C' ? entrySum.amount : 0.0

        BigDecimal currentBalance = existingAccountBalance.balance
        BigDecimal combinedBalance = rootClass == RootClass.DEBIT ?
                currentBalance + transactionSumsDebitAmount - transactionSumsCreditAmount :
                currentBalance + transactionSumsCreditAmount - transactionSumsDebitAmount

        accountBalancesByGlAccountId.put(entrySum.glAccountId, new AccountBalance(
                glAccountId: entrySum.glAccountId,
                accountCode: entrySum.accountCode,
                accountName: entrySum.accountName,
                balance: combinedBalance
        ))
    }

    if (negateBalances) {
        accountBalancesByGlAccountId = accountBalancesByGlAccountId.collectEntries {
            glAccountId, accountBalance ->
                [(glAccountId): new AccountBalance(
                        glAccountId: glAccountId,
                        accountCode: accountBalance.accountCode,
                        accountName: accountBalance.accountName,
                        balance: negateBalances ? accountBalance.balance.negate() : accountBalance.balance)]
        } as Map<String, AccountBalance>
    }

    accountBalancesByGlAccountId
}

static List<Map<String, Serializable>> sortAccountBalancesConvertToMaps(Collection<AccountBalance> accountBalances) {
    return accountBalances.sort { a, b -> (a.accountCode <=> b.accountCode) } *.asMap()
}

static BigDecimal sumAccountBalances(Collection<AccountBalance> accountBalances) {
    return accountBalances*.balance.inject(BigDecimal.ZERO) { acc, val -> acc + val }
}

// ASSETS
Map<String, AccountBalance> assetAccountBalances = calculateBalances(assetOpeningBalances, assetAccountClassIds, RootClass.DEBIT)
BigDecimal assetBalanceTotal = sumAccountBalances(assetAccountBalances.values())
List<Map<String, Serializable>> assetAccountBalanceList = sortAccountBalancesConvertToMaps(assetAccountBalances.values())
assetAccountBalanceList.add([accountName: uiLabelMap.AccountingTotalAssets, balance: assetBalanceTotal]
        as LinkedHashMap<String, Serializable>)

// CURRENT ASSETS
Map<String, AccountBalance> currentAssetAccountBalances =
        calculateBalances(currentAssetOpeningBalances, currentAssetAccountClassIds, RootClass.DEBIT)
BigDecimal currentAssetBalanceTotal = sumAccountBalances(currentAssetAccountBalances.values())

// LONGTERM ASSETS
Map<String, AccountBalance> longtermAssetAccountBalances =
        calculateBalances(longtermAssetOpeningBalances, longtermAssetAccountClassIds, RootClass.DEBIT)
BigDecimal longtermAssetBalanceTotal = sumAccountBalances(longtermAssetAccountBalances.values())

// CONTRA ASSETS
// Contra assets are accounts of class CREDIT, but for the purposes of the balance sheet, they will be listed alongside
// regular asset accounts in order to offset the total of all assets. We therefore negate these balances before
// including them in sums with the asset accounts.
Map<String, AccountBalance> contraAssetAccountBalances =
        calculateBalances(contraAssetOpeningBalances, contraAssetAccountClassIds, RootClass.CREDIT, true)
BigDecimal contraAssetBalanceTotal = sumAccountBalances(contraAssetAccountBalances.values())
List<Map<String, Serializable>> contraAssetAccountBalanceList = sortAccountBalancesConvertToMaps(contraAssetAccountBalances.values())
assetAccountBalanceList.addAll(contraAssetAccountBalanceList)
assetAccountBalanceList.add([accountName: uiLabelMap.AccountingTotalAccumulatedDepreciation,
                             balance: contraAssetBalanceTotal]
        as LinkedHashMap<String, Serializable>)

// LIABILITY
Map<String, AccountBalance> liabilityAccountBalances = calculateBalances(liabilityOpeningBalances, liabilityAccountClassIds, RootClass.CREDIT)
BigDecimal liabilityBalanceTotal = sumAccountBalances(liabilityAccountBalances.values())
List<Map<String, Serializable>> liabilityAccountBalanceList = sortAccountBalancesConvertToMaps(liabilityAccountBalances.values())
liabilityAccountBalanceList.add([accountName: uiLabelMap.AccountingTotalLiabilities, balance: liabilityBalanceTotal]
        as LinkedHashMap<String, Serializable>)

// CURRENT LIABILITY
Map<String, AccountBalance> currentLiabilityAccountBalances =
        calculateBalances(currentLiabilityOpeningBalances, currentLiabilityAccountClassIds, RootClass.CREDIT)
BigDecimal currentLiabilityBalanceTotal = sumAccountBalances(currentLiabilityAccountBalances.values())

// EQUITY
Map<String, AccountBalance> equityAccountBalances = calculateBalances(equityOpeningBalances, equityAccountClassIds, RootClass.CREDIT)

// Add the "retained earnings" account
Map netIncomeResult = runService('prepareIncomeStatement',
        [organizationPartyId: organizationPartyId, glFiscalTypeId: glFiscalTypeId,
         fromDate: fromDate, 'thruDate': thruDate, userLogin: userLogin])
BigDecimal netIncome = (BigDecimal)netIncomeResult.totalNetIncome
GenericValue retainedEarningsAccount = from('GlAccountTypeDefault')
        .where('glAccountTypeId', 'RETAINED_EARNINGS', 'organizationPartyId', organizationPartyId).cache(true).queryOne()
if (retainedEarningsAccount) {
    GenericValue retainedEarningsGlAccount = retainedEarningsAccount.getRelatedOne('GlAccount', false)

    AccountBalance retainedEarningsAccountBalance = equityAccountBalances.getOrDefault(retainedEarningsGlAccount.glAccountId, new AccountBalance(
            glAccountId: retainedEarningsGlAccount.glAccountId,
            accountCode: retainedEarningsGlAccount.accountCode,
            accountName: retainedEarningsGlAccount.accountName,
            balance: 0.0
    ))

    retainedEarningsAccountBalance.balance += netIncome

    equityAccountBalances.put(retainedEarningsGlAccount.glAccountId as String, retainedEarningsAccountBalance)
}

BigDecimal equityBalanceTotal = sumAccountBalances(equityAccountBalances.values())
List<Map<String, Serializable>> equityAccountBalanceList = sortAccountBalancesConvertToMaps(equityAccountBalances.values())
equityAccountBalanceList.add(UtilMisc.toMap('accountName', uiLabelMap.AccountingTotalEquities, 'balance', equityBalanceTotal))

context.assetBalanceTotal = assetBalanceTotal
context.assetAccountBalanceList = assetAccountBalanceList

context.currentAssetBalanceTotal = currentAssetBalanceTotal
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingCurrentAssets', 'balance', currentAssetBalanceTotal))

context.longtermAssetBalanceTotal = longtermAssetBalanceTotal
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingLongTermAssets', 'balance', longtermAssetBalanceTotal))

context.contraAssetBalanceTotal = contraAssetBalanceTotal
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingTotalAccumulatedDepreciation', 'balance', contraAssetBalanceTotal))
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingTotalAssets', 'balance',
        (context.currentAssetBalanceTotal + context.longtermAssetBalanceTotal + contraAssetBalanceTotal)))

context.liabilityAccountBalanceList = liabilityAccountBalanceList
context.liabilityBalanceTotal = liabilityBalanceTotal

context.currentLiabilityBalanceTotal = currentLiabilityBalanceTotal
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingCurrentLiabilities', 'balance', currentLiabilityBalanceTotal))

context.equityAccountBalanceList = equityAccountBalanceList
context.equityBalanceTotal = equityBalanceTotal

context.liabilityEquityBalanceTotal = liabilityBalanceTotal + equityBalanceTotal
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingEquities', 'balance', context.equityBalanceTotal))
balanceTotalList.add(UtilMisc.toMap('totalName', 'AccountingTotalLiabilitiesAndEquities', 'balance', context.liabilityEquityBalanceTotal))

context.balanceTotalList = balanceTotalList
