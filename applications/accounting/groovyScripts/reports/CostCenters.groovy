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
postedTotalDebit = BigDecimal.ZERO;
postedTotalCredit = BigDecimal.ZERO;
andExprs = [];
andExprs.add(EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds));
andExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
andExprs.add(EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, parameters.glFiscalTypeId));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
andExprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
andCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
List postedTransactionTotals = select("glAccountId", "accountName", "accountCode", "debitCreditFlag", "amount").from("AcctgTransEntrySums").where(andCond).orderBy("glAccountId").queryList();
if (postedTransactionTotals) {
    glAccountCategories = from("GlAccountCategory").where("glAccountCategoryTypeId", "COST_CENTER").orderBy("glAccountCategoryId").queryList();
    context.glAccountCategories = glAccountCategories;
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
        BigDecimal debitAmount = (BigDecimal)accountMap.get("D");
        BigDecimal creditAmount = (BigDecimal)accountMap.get("C");
        BigDecimal balance = debitAmount.subtract(creditAmount);
        accountMap.put("balance", balance);
        glAccountCategories.each { glAccountCategory ->
            glAccountCategoryMember = from("GlAccountCategoryMember").where("glAccountCategoryId", glAccountCategory.glAccountCategoryId, "glAccountId", postedTransactionTotal.glAccountId).orderBy("glAccountCategoryId").filterByDate().queryFirst();
            if (glAccountCategoryMember) {
                BigDecimal glAccountCategorySharePercentage = glAccountCategoryMember.amountPercentage;
                if (glAccountCategorySharePercentage && glAccountCategorySharePercentage != BigDecimal.ZERO ) {
                    glAccountCategoryShareFraction = glAccountCategorySharePercentage.divide(new BigDecimal("100.00"));
                    BigDecimal glAccountCategoryShare = balance.multiply(glAccountCategoryShareFraction);
                    accountMap.put(glAccountCategory.glAccountCategoryId,glAccountCategoryShare);
                }
            }
        }
    }
    context.glAcctBalancesByCostCenter = postedTransactionTotalsMap.values().asList()
}

