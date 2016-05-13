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

import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.party.party.PartyHelper;

partyNameList = [];
parties.each { party ->
    partyName = PartyHelper.getPartyName(party);
    partyNameList.add(partyName);
}
context.partyNameList = partyNameList;

if (parameters.customTimePeriodId) {
    customTimePeriod = from("CustomTimePeriod").where("customTimePeriodId", parameters.customTimePeriodId).cache(true).queryOne();
    exprList = [];
    exprList.add(EntityCondition.makeCondition('organizationPartyId', EntityOperator.IN, partyIds))
    exprList.add(EntityCondition.makeCondition('fromDate', EntityOperator.LESS_THAN, customTimePeriod.getTimestamp('thruDate')))
    exprList.add(EntityCondition.makeCondition(EntityCondition.makeCondition('thruDate', EntityOperator.GREATER_THAN_EQUAL_TO, customTimePeriod.getTimestamp('fromDate')), EntityOperator.OR, EntityCondition.makeCondition('thruDate', EntityOperator.EQUALS, null)))
    List organizationGlAccounts = from("GlAccountOrganizationAndClass").where(exprList).orderBy("accountCode").queryList();

    accountBalances = []
    postedDebitsTotal = 0
    postedCreditsTotal = 0
    organizationGlAccounts.each { organizationGlAccount ->
        accountBalance = [:]
        accountBalance = runService('computeGlAccountBalanceForTimePeriod', [organizationPartyId: organizationGlAccount.organizationPartyId, customTimePeriodId: customTimePeriod.customTimePeriodId, glAccountId: organizationGlAccount.glAccountId]);
        if (accountBalance.postedDebits != 0 || accountBalance.postedCredits != 0) {
            accountBalance.glAccountId = organizationGlAccount.glAccountId
            accountBalance.accountCode = organizationGlAccount.accountCode
            accountBalance.accountName = organizationGlAccount.accountName
            postedDebitsTotal = postedDebitsTotal + accountBalance.postedDebits
            postedCreditsTotal = postedCreditsTotal + accountBalance.postedCredits
            accountBalances.add(accountBalance)
        }
    }
    context.postedDebitsTotal = postedDebitsTotal
    context.postedCreditsTotal = postedCreditsTotal
    context.accountBalances = accountBalances
}
