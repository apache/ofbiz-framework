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


import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.base.util.UtilNumber
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

basePaymentId = parameters.paymentId
basePayment = from("Payment").where("paymentId", basePaymentId).queryOne()

decimals = UtilNumber.getBigDecimalScale("invoice.decimals")
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding")

paymentsMapList = [] // to pass back to the screeen list of unapplied payments

// retrieve payments for the related parties which have not been (fully) applied yet
List payments = null
exprList = []
expr = EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, basePayment.getString("partyIdFrom"))
exprList.add(expr)
expr = EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, basePayment.getString("partyIdTo"))
exprList.add(expr)
expr = EntityCondition.makeCondition("paymentId", EntityOperator.NOT_EQUAL, basePayment.getString("paymentId"))
exprList.add(expr)

// only payments with received and sent
exprListStatus = []
expr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PMNT_RECEIVED")
exprListStatus.add(expr)
expr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PMNT_SENT")
exprListStatus.add(expr)
orCond = EntityCondition.makeCondition(exprListStatus, EntityOperator.OR)
exprList.add(orCond)

topCond = EntityCondition.makeCondition(exprList, EntityOperator.AND)

payments = from("Payment").where(topCond).orderBy("effectiveDate").queryList()

if (payments)    {
    basePaymentApplied = PaymentWorker.getPaymentApplied(basePayment)
    basePaymentAmount = basePayment.getBigDecimal("amount")
    basePaymentToApply = basePaymentAmount.subtract(basePaymentApplied)
    payments.each { payment ->
        if (PaymentWorker.getPaymentNotApplied(payment).signum() == 1) {  // positiv not applied amount?
           // yes, put in the map
           paymentMap = [:]
           paymentMap.paymentId = basePaymentId
           paymentMap.toPaymentId = payment.paymentId
           paymentMap.currencyUomId = payment.currencyUomId
           paymentMap.effectiveDate = payment.effectiveDate.toString().substring(0,10) // list as YYYY-MM-DD
           paymentMap.amount = payment.getBigDecimal("amount")
           paymentMap.amountApplied = PaymentWorker.getPaymentApplied(payment)
           paymentToApply = PaymentWorker.getPaymentNotApplied(payment)
           if (paymentToApply.compareTo(basePaymentToApply) < 0 ) {
                paymentMap.amountToApply = paymentToApply
           } else {
                paymentMap.amountToApply = basePaymentToApply
           }
           paymentsMapList.add(paymentMap)
        }
    }
}
context.payments = paymentsMapList
