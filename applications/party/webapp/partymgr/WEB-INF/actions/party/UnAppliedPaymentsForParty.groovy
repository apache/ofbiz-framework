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
import org.ofbiz.entity.*;
import org.ofbiz.base.util.Debug;
import org.ofbiz.common.*;
import org.ofbiz.webapp.control.*;
import org.ofbiz.accounting.invoice.*;
import org.ofbiz.accounting.payment.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

Boolean actualCurrency = new Boolean(context.actualCurrency);
if (actualCurrency == null) {
    actualCurrency = true;
}
findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

payExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PMNT_NOTPAID"),
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PMNT_CANCELLED"),
        EntityCondition.makeCondition([
               EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, parameters.partyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, context.defaultOrganizationPartyId)
                ], EntityOperator.AND),
            EntityCondition.makeCondition([
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, context.defaultOrganizationPartyId),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, parameters.partyId)
                ], EntityOperator.AND)
            ], EntityOperator.OR)
        ], EntityOperator.AND);

paymentList = [];
payIterator = from("PaymentAndType").where(payExprs).cursorScrollInsensitive().distinct().queryIterator();

while (payment = payIterator.next()) {
    unAppliedAmount = PaymentWorker.getPaymentNotApplied(payment, actualCurrency).setScale(2,BigDecimal.ROUND_HALF_UP);
    if (unAppliedAmount.signum() == 1) {
        if (actualCurrency.equals(true) && payment.actualCurrencyAmount && payment.actualCurrencyUomId) {
            amount = payment.actualCurrencyAmount;
            paymentCurrencyUomId = payment.actualCurrencyUomId;
        } else {
            amount = payment.amount;
            paymentCurrencyUomId = payment.currencyUomId;
        }
        paymentList.add([paymentId : payment.paymentId,
                         effectiveDate : payment.effectiveDate,
                         unAppliedAmount : unAppliedAmount,
                         amount : amount,
                         paymentCurrencyUomId : paymentCurrencyUomId,
                         paymentTypeId : payment.paymentTypeId,
                         paymentParentTypeId : payment.parentTypeId]);
    }
}
payIterator.close();

context.paymentList = paymentList;
