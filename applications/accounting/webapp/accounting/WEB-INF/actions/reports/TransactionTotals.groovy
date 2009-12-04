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
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

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

// POSTED
// Posted transactions totals and grand totals
postedTotals = [];
postedTotalDebit = BigDecimal.ZERO;
postedTotalCredit = BigDecimal.ZERO;
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List postedTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
if (postedTransactionTotals) {
    Map postedTransactionTotalsMap = [:]
    postedTransactionTotals.each { postedTransactionTotal ->
        Map accountMap = (Map)postedTransactionTotalsMap.get(postedTransactionTotal.glAccountId);
        if (!accountMap) {
            accountMap = UtilMisc.makeMapWritable(postedTransactionTotal);
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, postedTransactionTotal.debitCreditFlag, postedTransactionTotal.amount);
        postedTransactionTotalsMap.put(postedTransactionTotal.glAccountId, accountMap);
    }
    postedTotals = postedTransactionTotalsMap.values().asList();
}
// Posted grand total for Debits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "D"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List postedDebitTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (postedDebitTransactionTotals) {
    postedDebitTransactionTotal = EntityUtil.getFirst(postedDebitTransactionTotals);
    if (postedDebitTransactionTotal && postedDebitTransactionTotal.amount) {
        postedTotalDebit = postedDebitTransactionTotal.amount;
    }
}
// Posted grand total for Credits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "C"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List postedCreditTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (postedCreditTransactionTotals) {
    postedCreditTransactionTotal = EntityUtil.getFirst(postedCreditTransactionTotals);
    if (postedCreditTransactionTotal && postedCreditTransactionTotal.amount) {
        postedTotalCredit = postedCreditTransactionTotal.amount;
    }
}
postedTotals.add(["D":postedTotalDebit, "C":postedTotalCredit]);
context.postedTransactionTotals = postedTotals;

// UNPOSTED
// Unposted transactions totals and grand totals
unpostedTotals = [];
unpostedTotalDebit = BigDecimal.ZERO;
unpostedTotalCredit = BigDecimal.ZERO;
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List unpostedTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
if (unpostedTransactionTotals) {
    Map unpostedTransactionTotalsMap = [:]
    unpostedTransactionTotals.each { unpostedTransactionTotal ->
        Map accountMap = (Map)unpostedTransactionTotalsMap.get(unpostedTransactionTotal.glAccountId);
        if (!accountMap) {
            accountMap = UtilMisc.makeMapWritable(unpostedTransactionTotal);
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, unpostedTransactionTotal.debitCreditFlag, unpostedTransactionTotal.amount);
        unpostedTransactionTotalsMap.put(unpostedTransactionTotal.glAccountId, accountMap);
    }
    unpostedTotals = unpostedTransactionTotalsMap.values().asList();
}
// Unposted grand total for Debits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "D"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List unpostedDebitTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (unpostedDebitTransactionTotals) {
    unpostedDebitTransactionTotal = EntityUtil.getFirst(unpostedDebitTransactionTotals);
    if (unpostedDebitTransactionTotal && unpostedDebitTransactionTotal.amount) {
        unpostedTotalDebit = unpostedDebitTransactionTotal.amount;
    }
}
// Unposted grand total for Credits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "C"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List unpostedCreditTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (unpostedCreditTransactionTotals) {
    unpostedCreditTransactionTotal = EntityUtil.getFirst(unpostedCreditTransactionTotals);
    if (unpostedCreditTransactionTotal && unpostedCreditTransactionTotal.amount) {
        unpostedTotalCredit = unpostedCreditTransactionTotal.amount;
    }
}
unpostedTotals.add(["D":unpostedTotalDebit, "C":unpostedTotalCredit]);
context.unpostedTransactionTotals = unpostedTotals;

// POSTED AND UNPOSTED
// Posted and unposted transactions totals and grand totals
allTotals = [];
allTotalDebit = BigDecimal.ZERO;
allTotalCredit = BigDecimal.ZERO;
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List allTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount"), UtilMisc.toList("glAccountId"), null, false);
if (allTransactionTotals) {
    Map allTransactionTotalsMap = [:]
    allTransactionTotals.each { allTransactionTotal ->
        Map accountMap = (Map)allTransactionTotalsMap.get(allTransactionTotal.glAccountId);
        if (!accountMap) {
            accountMap = UtilMisc.makeMapWritable(allTransactionTotal);
            accountMap.put("D", BigDecimal.ZERO);
            accountMap.put("C", BigDecimal.ZERO);
        }
        UtilMisc.addToBigDecimalInMap(accountMap, allTransactionTotal.debitCreditFlag, allTransactionTotal.amount);
        allTransactionTotalsMap.put(allTransactionTotal.glAccountId, accountMap);
    }
    allTotals = allTransactionTotalsMap.values().asList();
}
// Posted and unposted grand total for Debits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "D"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List allDebitTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (allDebitTransactionTotals) {
    allDebitTransactionTotal = EntityUtil.getFirst(allDebitTransactionTotals);
    if (allDebitTransactionTotal && allDebitTransactionTotal.amount) {
        allTotalDebit = allDebitTransactionTotal.amount;
    }
}
// Posted and unposted grand total for Credits
andExprs = FastList.newInstance();
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, "C"));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List allCreditTransactionTotals = delegator.findList("AcctgTransEntrySums", andCond, UtilMisc.toSet("amount"), null, null, false);
if (allCreditTransactionTotals) {
    allCreditTransactionTotal = EntityUtil.getFirst(allCreditTransactionTotals);
    if (allCreditTransactionTotal && allCreditTransactionTotal.amount) {
        allTotalCredit = allCreditTransactionTotal.amount;
    }
}
allTotals.add(["D":allTotalDebit, "C":allTotalCredit]);
context.allTransactionTotals = allTotals;
