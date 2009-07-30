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
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
if ("Y".equals(parameters.noConditionFind)) {
List paymentCond = [];
if (paymentMethodTypeId) {
    paymentCond.add(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, paymentMethodTypeId));
}
if (fromDate) {
    paymentCond.add(EntityCondition.makeCondition("effectiveDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
}
if (thruDate) {
    paymentCond.add(EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
}
if (partyIdFrom) {
    paymentCond.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyIdFrom));
}
if (organizationPartyId) {
    paymentCond.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, organizationPartyId));
}
if (finAccountId) {
    finAccountTransList = delegator.findList("FinAccountTrans", EntityCondition.makeCondition([finAccountId : finAccountId]), null, null, null, false);
    if (finAccountTransList) {
        finAccountTransIds = EntityUtil.getFieldListFromEntityList(finAccountTransList, "finAccountTransId", true);
        paymentCond.add(EntityCondition.makeCondition("finAccountTransId", EntityOperator.IN, finAccountTransIds));
    }
} else {
    paymentCond.add(EntityCondition.makeCondition("finAccountTransId", EntityOperator.NOT_EQUAL, null));
}
payments = delegator.findList("Payment", EntityCondition.makeCondition(paymentCond, EntityOperator.AND), null, null, null, false);
paymentListWithCreditCard = [];
paymentListWithoutCreditCard = [];
if (payments) {
    payments.each { payment ->
        paymentGroupMembers = EntityUtil.filterByDate(delegator.findList("PaymentGroupMember", EntityCondition.makeCondition([paymentId : payment.paymentId]), null, null, null, false));
        if (!paymentGroupMembers) {
            if (cardType && payment.paymentMethodId) {
                creditCard = delegator.findOne("CreditCard", [paymentMethodId : payment.paymentMethodId], false);
                if (creditCard.cardType == cardType) {
                    paymentListWithCreditCard.add(payment);
                }
            } else if (UtilValidate.isEmpty(cardType)) {
                paymentListWithoutCreditCard.add(payment);
            }
        }
    }
    if (paymentListWithCreditCard) {
        context.paymentList = paymentListWithCreditCard;
    } else {
        context.paymentList = paymentListWithoutCreditCard;
    }
}
}
