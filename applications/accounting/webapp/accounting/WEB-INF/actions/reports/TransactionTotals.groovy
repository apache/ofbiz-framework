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
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtil;

import javolution.util.FastList;
import javolution.util.FastMap;

debitTotal = BigDecimal.ZERO;
creditTotal = BigDecimal.ZERO;
openingBalanceCredit = BigDecimal.ZERO;
openingBalanceDebit = BigDecimal.ZERO;

decimals = UtilNumber.getBigDecimalScale("ledger.decimals");
rounding = UtilNumber.getBigDecimalRoundingMode("ledger.rounding");
exprs = [EntityCondition.makeCondition("organizationPartyId", EntityOperator.IN, partyIds)];
if (fromDate) {
    exprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
} else return;

if (!thruDate) {
    thruDate = UtilDateTime.nowTimestamp();
}
exprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));

exprList = FastList.newInstance();
orExprs = new ArrayList();
orExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
orExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
orCond = EntityCondition.makeCondition(orExprs, EntityOperator.OR);

exprList.add(orCond);
exprList.addAll(exprs);

fieldsToSelect = ["glAccountId", "debitCreditFlag", "totalAmount", "isPosted"] as Set;
orderBy = ["glAccountId"];

postedTransTotalList = FastList.newInstance();
unpostedTransTotalList = FastList.newInstance();
postedAndUnpostedTransTotalList = FastList.newInstance();
tempValueMap = [:];
tempValueMap.isPosted = "";
tempValueMap.glAccountId = "000";
tempValueMap.debitCreditFlag = "X";
tempValueMap.totalAmount = 0.00;

allTrans = delegator.findList("GlAccOrgAndAcctgTransAndEntry", EntityCondition.makeCondition(exprList, EntityOperator.AND), fieldsToSelect, orderBy, null, false);
if (allTrans) {
    //PostedTransaction Section
    allPostedTrans = EntityUtil.filterByCondition(allTrans, EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
    if (allPostedTrans)
        getPostedTrans(0, (allPostedTrans.get(0)).glAccountId);

    //UnPostedTransaction Section
    allUnPostedTrans = EntityUtil.filterByCondition(allTrans, EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
    if (allUnPostedTrans)
        getUnpostedTrans(0, (allUnPostedTrans.get(0)).glAccountId);

    //PostedAndUnPostedTransaction Section
    getPostedAndUnpostedTrans(0, (allTrans.get(0)).glAccountId);
}

private void addTransToList(List transectionList, String prevGlAccountId, Map value) {
    if (!prevGlAccountId.equals(value.glAccountId)) {
        if (parameters.selectedMonth){
            resultMap = dispatcher.runSync("calculateGlAccountTrialBalance", 
                    [fromDate : financialYearFromDate, thruDate : fromDate , glAccountId : prevGlAccountId, isPosted : value.isPosted, userLogin : userLogin]);
    
            openingBalanceCredit = resultMap.openingBalanceCredit;
            openingBalanceDebit = resultMap.openingBalanceDebit;
        }
        postedAndUnpostedMap = FastMap.newInstance();
        postedAndUnpostedMap.glAccountId = prevGlAccountId;
        postedAndUnpostedMap.credit = creditTotal.setScale(decimals, rounding);
        postedAndUnpostedMap.debit = debitTotal.setScale(decimals, rounding);
        postedAndUnpostedMap.openingBalanceCredit = openingBalanceCredit.setScale(decimals, rounding);
        postedAndUnpostedMap.openingBalanceDebit = openingBalanceDebit.setScale(decimals, rounding);
        transectionList.add(postedAndUnpostedMap);
        debitTotal = BigDecimal.ZERO;
        creditTotal = BigDecimal.ZERO;
    }
    if ("C".equals(value.debitCreditFlag))
        creditTotal += value.getBigDecimal("totalAmount");
    if ("D".equals(value.debitCreditFlag))
        debitTotal += value.getBigDecimal("totalAmount");
}

private void getPostedTrans(int index, String prevGlAccountId) {
    if (index < allPostedTrans.size())
        value = allPostedTrans.get(index);
    else {
        tempValueMap.isPosted = "Y";
        value = tempValueMap;
    }
    if("Y".equals(value.isPosted)) {
        addTransToList(postedTransTotalList, prevGlAccountId, value);
    }
    if (index < allPostedTrans.size()) {
        index++;
        getPostedTrans(index, value.glAccountId);
    }
    else return;
}

private void getUnpostedTrans(int index, String prevGlAccountId) {
    if (index != allUnPostedTrans.size())
        value = allUnPostedTrans.get(index);
    else {
        tempValueMap.isPosted = "N";
        value = tempValueMap;
    }
    
    if("N".equals(value.isPosted)) {
        addTransToList(unpostedTransTotalList, prevGlAccountId, value);     
    }
    if (index < allUnPostedTrans.size()) {
        index++; 
        getUnpostedTrans(index, value.glAccountId);
    }
    else return;
}

private void getPostedAndUnpostedTrans(int index, String prevGlAccountId) {
    if (index != allTrans.size())
        value = allTrans.get(index);
    value.isPosted = "";
    addTransToList(postedAndUnpostedTransTotalList, prevGlAccountId, value);  
    if (index < allTrans.size()) {
        index++; 
        getPostedAndUnpostedTrans(index, value.glAccountId);
    }
    else return;
}
context.postedTransTotalList = postedTransTotalList;
context.unpostedTransTotalList = unpostedTransTotalList;
context.postedAndUnpostedTransTotalList = postedAndUnpostedTransTotalList;