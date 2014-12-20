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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.*;
import org.ofbiz.accounting.invoice.*;
import org.ofbiz.accounting.payment.*;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import java.math.*;

invoiceId = parameters.invoiceId;
invoice = from("Invoice").where(invoiceId : invoiceId).queryOne();

decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

exprBldr = new EntityConditionBuilder();
preCurrencyCond = exprBldr.AND() {
    EQUALS(partyIdTo: invoice.partyIdFrom)
    EQUALS(partyIdFrom: invoice.partyId)
    IN(statusId: ["PMNT_NOT_PAID", "PMNT_RECEIVED", "PMNT_SENT"])
}

topCond = exprBldr.AND(preCurrencyCond) {
    EQUALS(currencyUomId: invoice.currencyUomId)
}

topCondActual = exprBldr.AND(preCurrencyCond) {
    EQUALS(actualCurrencyUomId: invoice.currencyUomId)
}

payments = from("Payment").where(topCond).orderBy("effectiveDate").queryList();
context.payments = getPayments(payments, false);
payments = from("Payment").where(topCondActual).orderBy("effectiveDate").queryList();
context.paymentsActualCurrency = getPayments(payments, true);

List getPayments(List payments, boolean actual) {
    if (payments)    {
        paymentList = [];  // to pass back to the screeen list of unapplied payments
        invoiceApplied = InvoiceWorker.getInvoiceApplied(invoice);
        invoiceAmount = InvoiceWorker.getInvoiceTotal(invoice);
        invoiceToApply = InvoiceWorker.getInvoiceNotApplied(invoice);
        payments.each { payment ->
            paymentMap = [:];
            paymentApplied = PaymentWorker.getPaymentApplied(payment, true);
            if (actual) {
                paymentMap.amount = payment.actualCurrencyAmount;
                paymentMap.currencyUomId = payment.actualCurrencyUomId;
                paymentToApply = payment.getBigDecimal("actualCurrencyAmount")?.setScale(decimals,rounding)?.subtract(paymentApplied);
            } else {
                paymentMap.amount = payment.amount;
                paymentMap.currencyUomId = payment.currencyUomId;
                paymentToApply = payment.getBigDecimal("amount")?.setScale(decimals,rounding)?.subtract(paymentApplied);
            }
            if (paymentToApply?.signum() == 1) {
                paymentMap.paymentId = payment.paymentId;
                paymentMap.effectiveDate = payment.effectiveDate;
                if (paymentToApply.compareTo(invoiceToApply) < 0 ) {
                    paymentMap.amountToApply = paymentToApply;
                } else {
                    paymentMap.amountToApply = invoiceToApply;
                }
                paymentList.add(paymentMap);
            }
        }
        return paymentList;
    }
}
