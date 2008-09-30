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
import java.math.*;
import java.sql.Timestamp;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.securityext.login.*;
import org.ofbiz.common.*;
import org.ofbiz.webapp.control.*;
import org.ofbiz.accounting.invoice.*;
import org.ofbiz.accounting.payment.*;

delegator = parameters.delegator;
partyId = parameters.partyId;
List historyList = new LinkedList();

//get payment totals
BigDecimal totalPaymentsIn = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
BigDecimal totalPaymentsOut = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
tpayments = delegator.findByOr("Payment",["partyIdTo" : partyId,"partyIdFrom" : partyId]);
Iterator pl = tpayments.iterator();
while (pl.hasNext()) {
    payment = (GenericValue) pl.next();
    if (payment.statusId.equals("PMNT_CANCELLED")) continue;
    if (payment.partyIdTo.equals(partyId))
        totalPaymentsIn = totalPaymentsIn.add(payment.getBigDecimal("amount")).setScale(2,BigDecimal.ROUND_HALF_UP);
    else
        totalPaymentsOut = totalPaymentsOut.add(payment.getBigDecimal("amount")).setScale(2,BigDecimal.ROUND_HALF_UP);
}

// totals
BigDecimal totalSalesInvoice = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
BigDecimal totalPurchaseInvoice = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
BigDecimal totalInvoiceApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
BigDecimal totalInvoiceNotApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);

// payment and invoices list which will be updated with the amount applied
// to see what is left over...
List invoices = delegator.findByOr("Invoice",["partyId" : partyId , "partyIdFrom" : partyId] , ["invoiceDate"]);
List payments = delegator.findByOr("Payment",["partyIdTo" : partyId , "partyIdFrom" : partyId] , ["effectiveDate"]);
    
List notAppliedInvoices = new LinkedList(); // to store the not fully applied invoices
    
// start reading from the invoices list
if (invoices != null && invoices.size() > 0) {
    Iterator inv = invoices.iterator();
    while (inv.hasNext())   {
        invoice = (GenericValue) inv.next();
        if (invoice.statusId.equals("INVOICE_CANCELLED")) continue;
        BigDecimal invoiceAmount = InvoiceWorker.getInvoiceTotalBd(invoice).setScale(2,BigDecimal.ROUND_HALF_UP);
        invoiceApplied = InvoiceWorker.getInvoiceAppliedBd(invoice).setScale(2,BigDecimal.ROUND_HALF_UP);
        if (invoice.getString("partyId").equals(partyId)) { //negate for outgoing payments
            invoiceAmount = invoiceAmount.multiply(new BigDecimal("-1"));
            invoiceApplied = invoiceApplied.multiply(new BigDecimal("-1"));
        }
        if (invoice.invoiceTypeId.equals("PURCHASE_INVOICE")) totalPurchaseInvoice = totalPurchaseInvoice.add(invoiceAmount);
        if (invoice.invoiceTypeId.equals("SALES_INVOICE")) totalSalesInvoice = totalSalesInvoice.add(invoiceAmount);
        totalInvoiceApplied = totalInvoiceApplied.add(invoiceApplied);
//      Debug.logInfo("Invoice type: "+ invoice.getString("invoiceTypeId") + "amount: " + invoiceAmount + " applied: " + invoiceApplied,"??");
        if (!invoiceAmount.equals(invoiceApplied))  {
            Map notAppliedInvoice = ["invoiceId" : invoice.invoiceId,
                                     "invoiceTypeId" : invoice.invoiceTypeId.substring(0,1), 
                                     "invoiceDate" : invoice.invoiceDate.toString().substring(0,10),
                                     "invoiceAmount" : invoiceAmount.toString(),
                                     "invoiceNotApplied" : invoiceAmount.subtract(invoiceApplied).toString()];
                notAppliedInvoices.add(notAppliedInvoice);
                totalInvoiceNotApplied = totalInvoiceNotApplied.add(invoiceAmount).subtract(invoiceApplied);
        }
        Map historyItem = ["invoiceId" : invoice.invoiceId,
                           "invoiceTypeId" : invoice.invoiceTypeId.substring(0,1), 
                           "invoiceDate" : invoice.invoiceDate.toString().substring(0,10),
                           "invoiceAmount" : invoiceAmount.toString(),
                           "totInvoiceApplied" : invoiceApplied.toString()
            ];
        // check for applications
        List applications = invoice.getRelated("PaymentApplication",null,["paymentId"]);
        if (applications != null && applications.size() > 0) {
            Iterator appl = applications.iterator();
            oldPaymentId = new String(" ");
            BigDecimal applied = new BigDecimal("0");
            boolean first = true;
            while (appl.hasNext())  {   // read the applications for this invoice
                application = (GenericValue) appl.next();
                paymentId = application.paymentId;

                //reduce the payment amount in the payment list
                pl = payments.iterator();
                while (pl.hasNext()) {
                    payment = (GenericValue) pl.next();
                    if (paymentId.equals(payment.paymentId)) {
                        plInd = payments.indexOf(payment);
                        payment.amount = payment.getBigDecimal("amount").subtract(application.getBigDecimal("amountApplied")).doubleValue();
                        payments.remove(plInd);
                        payments.add(plInd,payment);
                        break;
                    }
                }
                
                // check if the payment number has changed, then we have to output a line....
                if (!first && !paymentId.equals(oldPaymentId)) { // if the payment number has changed, but not the first
                    payment = delegator.findByPrimaryKey("Payment",["paymentId" : oldPaymentId]);
                    BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
                    if (payment.getString("partyIdFrom").equals(partyId)) amount = amount.multiply(new BigDecimal("-1"));
                    historyItem = ["applied" : applied.toString(),
                                   "paymentId" : oldPaymentId,
                                   "amount" : amount.toString(),
                                   "effectiveDate" : payment.effectiveDate.toString().substring(0,10)];  
                    historyList.add(historyItem);
                    historyItem = new HashMap();
                    applied = new BigDecimal("0");
                }
                applied = applied.add(application.getBigDecimal("amountApplied")).setScale(2,BigDecimal.ROUND_HALF_UP);
                oldPaymentId = paymentId;
                first = false;
            }
            if (!applied.equals("0")) {
                payment = delegator.findByPrimaryKey("Payment",["paymentId" : oldPaymentId]);
                if (payment != null) {
                    BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
                    if (payment.getString("partyIdFrom").equals(partyId))   amount = amount.multiply(new BigDecimal("-1"));
                    historyItem = ["invoiceId" : invoice.invoiceId,
                                   "invoiceTypeId" : invoice.invoiceTypeId.substring(0,1),
                                   "invoiceDate" : invoice.invoiceDate.toString().substring(0,10),
                                   "invoiceAmount" : invoiceAmount.toString(),
                                   "totInvoiceApplied" : invoiceApplied.toString(),
                                   "applied" : applied.toString(),
                                   "paymentId" : oldPaymentId,
                                   "amount" : amount.toString(),
                                   "effectiveDate" : payment.effectiveDate.toString().substring(0,10)];
                    historyList.add(historyItem);
                }
            }
        }
    }
    context.historyListInvoices = historyList;
}

// totals
BigDecimal totalPaymentApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
// list of payments where payments are applied
historyList = new LinkedList();
if (payments) {
    pay = payments.iterator();
    while (pay.hasNext())   {
        payment = (GenericValue) pay.next();
        if (payment.statusId.equals("PMNT_CANCELLED")) continue;
        applications = payment.getRelated("PaymentApplication", ["paymentId"]);
        if (!applications) continue; // none found  
        Map historyItem = ["paymentId" : payment.paymentId , "effectiveDate" : payment.effectiveDate.toString().substring(0,10)];
        BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
        if (payment.partyIdFrom.equals(organizationPartyId)) {
            amount = amount.multiply(new BigDecimal("-1"));
        }
        historyItem.amount = amount.toString();
        ap = applications.iterator();
        while (ap.hasNext())    {
            application = (GenericValue) ap.next();
            historyItem = ["toPaymentId" : application.paymentId,
                           "applied" : application.amountApplied];
            toPayment = application.getRelatedOne("Payment");
            historyItem.toEffectiveDate = toPayment.effectiveDate.toString().substring(0,10);
            toAmount = toPayment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
            if (toPayment.partyIdFrom.equals(partyId)) toAmount = toAmount.multiply(new BigDecimal("-1"));
            //reduce the payment amount in the payment list
            pl = payments.iterator();
            while (pl.hasNext()) {
                payment = (GenericValue) pl.next();
                // reduce paymentId
                if (application.paymentId.equals(payment.paymentId)) {
                    plInd = payments.indexOf(payment);
                    payment.amount =
                            payment.getBigDecimal("amount").
                            subtract(application.getBigDecimal("amountApplied")).doubleValue();
                    payments.remove(plInd);
                    payments.add(plInd,payment);
                    totalPaymentApplied = totalPaymentApplied.add(application.getBigDecimal("amountApplied"));
                }
                // reduce toPaymentId
                if (application.toPaymentId.equals(payment.paymentId)) {
                    plInd = payments.indexOf(payment);
                    payment.amount =
                            payment.getBigDecimal("amount").
                            subtract(application.getBigDecimal("amountApplied")).doubleValue();
                    payments.remove(plInd);
                    payments.add(plInd,payment);
                    totalPaymentApplied = totalPaymentApplied.add(application.getBigDecimal("amountApplied"));
                }
            }
        }
    }
    context.historyListPayments = historyList;
}

// check if any invoices left not applied
if (notAppliedInvoices != null && notAppliedInvoices.size() > 0) {
    context.historyListInvoicesN = notAppliedInvoices;
}

/*
// list payments applied to other companies
historyList = new LinkedList();
if (payments) {
    Iterator pm = payments.iterator();
    while (pm.hasNext())    {
        payment = (GenericValue) pm.next();
        // check if payment completely applied
        BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
        if (amount.compareTo(new BigDecimal("0.00")) == 0) 
            continue;
        Debug.logInfo(" other company payments: " + payment.paymentId + " amount:" + payment.getBigDecimal("amount") + " applied:" + PaymentWorker.getPaymentAppliedBd(payment),"??");
        List paymentApplications = payment.getRelated("PaymentApplication");
        Iterator pa = paymentApplications.iterator();
        while (pa.hasNext())    {
            paymentApplication = (GenericValue) pa.next();
            if (paymentApplication.invoiceId != null) {
                invoice = paymentApplication.getRelatedOne("Invoice");
                if (!invoice.partyId.equals(partyId)) {
                    historyItem = new HashMap();
                    Map historyItem = ["paymentId" : payment.paymentId,
                                       "invoiceId" : paymentApplication.invoiceId,
                                       "invoiceItemSeqId" : paymentApplication.invoiceItemSeqId,
                                       "partyId" : invoice.partyIdFrom,
                                       "amount" : amount.toString(),
                                       "applied" : paymentApplication.getBigDecimal("amountApplied").setScale(2,BigDecimal.ROUND_HALF_UP).toString(),
                                       "effectiveDate" : payment.effectiveDate.toString().substring(0,10)];
                    historyList.add(historyItem);
                }
            }
        }
    }
    context.historyListPaymentsO = historyList;
}
*/
// list not applied payments
BigDecimal totalPaymentNotApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
historyList = new LinkedList();
if (payments != null && payments.size() > 0) {
    Iterator pm = payments.iterator();
    while (pm.hasNext())    {
        payment = (GenericValue) pm.next();
        payment = delegator.findByPrimaryKey("Payment",["paymentId" : payment.paymentId]);
        notApplied = payment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentAppliedBd(payment)).setScale(2,BigDecimal.ROUND_HALF_UP);
        // check if payment completely applied
        Debug.logInfo(" payment: " + payment.paymentId + " amount:" + payment.getBigDecimal("amount") + " applied:" + PaymentWorker.getPaymentAppliedBd(payment),"??");  
        if (notApplied.compareTo(new BigDecimal("0.00")) == 0) 
            continue;
        Map historyItem = new HashMap();
        BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
        totalPaymentNotApplied = totalPaymentNotApplied.add(notApplied);
        historyItem = ["paymentId" : payment.paymentId,
                       "amount" : amount.toString(),
                       "notApplied" : notApplied.toString(),
                       "effectiveDate" : payment.effectiveDate.toString().substring(0,10)];
        historyList.add(historyItem);
    }
    context.historyListPaymentsN = historyList;
}

// create totals
finanSummary = ["totalSalesInvoice" : totalSalesInvoice.toString(), 
                "totalPurchaseInvoice" : totalPurchaseInvoice.toString(),   
                "totalPaymentsIn" : totalPaymentsIn.toString(), 
                "totalPaymentsOut" : totalPaymentsOut.toString(),   
                "totalInvoiceApplied" : totalInvoiceApplied.toString(),
                "totalInvoiceNotApplied" : totalInvoiceNotApplied.toString(),
                "totalPaymentNotApplied" : totalPaymentNotApplied.toString(),
                "totalPaymentNotApplied" : totalPaymentNotApplied.toString()];
totalToBePaid = totalSalesInvoice.add(totalPurchaseInvoice).subtract(totalInvoiceApplied).subtract(totalPaymentNotApplied);
if (totalToBePaid.compareTo(new BigDecimal("0.00")) < 0 ) finanSummary.totalToBePaid = totalToBePaid.toString();
else if (totalToBePaid.compareTo(new BigDecimal("0.00")) > 0 ) finanSummary.totalToBeReceived = totalToBePaid.toString();
else    {
    finanSummary.totalToBePaid = "0.00";
    finanSummary.totalToBeReceived = "0.00";
}
 context.finanSummary = finanSummary;
    