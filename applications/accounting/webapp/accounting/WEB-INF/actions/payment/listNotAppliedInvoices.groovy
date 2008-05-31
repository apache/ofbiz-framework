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
import java.math.*;
import java.text.NumberFormat;

paymentId = parameters.paymentId;
payment = delegator.findByPrimaryKey("Payment", [paymentId : paymentId]);

decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

// retrieve invoices for the related parties which have not been (fully) applied yet
List invoices = delegator.findByAnd("Invoice", [partyId : payment.partyIdFrom, partyIdFrom : payment.partyIdTo], ["invoiceDate"]);
   
if (invoices)    {
    invoicesList = [];  // to pass back to the screeen list of unapplied invoices
    paymentApplied = PaymentWorker.getPaymentAppliedBd(payment);
    paymentToApply = payment.getBigDecimal("amount").setScale(decimals,rounding).subtract(paymentApplied);
    invoices.each { invoice ->
        invoiceAmount = InvoiceWorker.getInvoiceTotalBd(invoice).setScale(decimals,rounding);
        invoiceApplied = InvoiceWorker.getInvoiceAppliedBd(invoice).setScale(decimals,rounding);
        if (!invoiceAmount.equals(invoiceApplied) && 
                !invoice.statusId.equals("INVOICE_CANCELLED") &&
                !invoice.statusId.equals("INVOICE_IN_PROCESS")) {
            // put in the map
            invoiceToApply = invoiceAmount.subtract(invoiceApplied); 
            invoiceMap = [:];
            invoiceMap.invoiceId = invoice.invoiceId;
            invoiceMap.currencyUomId = invoice.currencyUomId);
            invoiceMap.amount = invoiceAmount;
            invoiceMap.description = invoice.description; 
            invoiceMap.invoiceDate = invoice.invoiceDate.substring(0,10)); // display only YYYY-MM-DD
            invoiceMap.amountApplied = invoiceApplied;
            if (paymentToApply.compareTo(invoiceToApply) < 0 ) {
                invoiceMap.amountToApply = paymentToApply;
            } else {
                invoiceMap.amountToApply = invoiceToApply;
            }
            invoicesList.add(invoiceMap);
        } 
    }
    context.invoices = invoicesList;
}
