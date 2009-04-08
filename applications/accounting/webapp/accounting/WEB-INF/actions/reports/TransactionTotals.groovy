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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.GenericDelegator;

import javolution.util.FastList;
import javolution.util.FastMap;

exprs = [EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId)];
if (fromDate) {
    exprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
}
if (thruDate) {
    exprs.add(EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
}

postedExprs = FastList.newInstance();
postedExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"));
postedExprs.addAll(exprs);
fieldsToSelect = ["glAccountId", "debitCreditFlag", "totalAmount"] as Set;
orderBy = ["glAccountId"];

postedTransTotalList = FastList.newInstance();
postedTrans = delegator.findList("GlAccOrgAndAcctgTransAndEntry", EntityCondition.makeCondition(postedExprs, EntityOperator.AND), fieldsToSelect, orderBy, null, false);
if (postedTrans) {
    postedTrans.each { value ->
        postedMap = FastMap.newInstance();
        postedMap.glAccountId = value.glAccountId;
        if ("C".equals(value.debitCreditFlag)) {
            postedMap.credit = value.getBigDecimal("totalAmount");
            postedMap.debit = BigDecimal.ZERO;
        } else {
            postedMap.credit = BigDecimal.ZERO;
            postedMap.debit = value.getBigDecimal("totalAmount");
        }
        postedTransTotalList.add(postedMap);
    }
}
context.postedTransTotalList = postedTransTotalList;

unpostedExprs = FastList.newInstance();
unpostedExprs.add(EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
unpostedExprs.addAll(exprs);

unpostedTransTotalList = FastList.newInstance();
unpostedTrans = delegator.findList("GlAccOrgAndAcctgTransAndEntry", EntityCondition.makeCondition(unpostedExprs, EntityOperator.AND), fieldsToSelect, orderBy, null, false);
if (unpostedTrans) {
    unpostedTrans.each { value ->
        Map unpostedMap = FastMap.newInstance();
        unpostedMap.glAccountId = value.glAccountId;
        if ("C".equals(value.debitCreditFlag)) {
            unpostedMap.credit = value.getBigDecimal("totalAmount");
            unpostedMap.debit = BigDecimal.ZERO;
        } else {
            unpostedMap.credit = BigDecimal.ZERO;
            unpostedMap.debit = value.getBigDecimal("totalAmount");
        }
        unpostedTransTotalList.add(unpostedMap);
    }
}
context.put("unpostedTransTotalList", unpostedTransTotalList);