/*
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Hans Bakker (h.bakker@antwebsystems.com)
 *@version    $Rev$
 *@since      3.0
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

delegator = parameters.get("delegator");
organizationPartyId = parameters.get("organizationPartyId");
partyId = parameters.get("partyId");
if (partyId == null && organizationPartyId == null) return;
List historyList = new LinkedList();

//get payment totals
BigDecimal totalPaymentsIn = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
BigDecimal totalPaymentsOut = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
tpayments = delegator.findByOr("Payment",[partyIdTo:partyId,partyIdFrom:partyId]);
Iterator pl = tpayments.iterator();
while (pl.hasNext()) {
	payment = pl.next();
	if (payment.getString("statusId").equals("PMNT_CANCELLED")) continue;
	if (payment.getString("partyIdTo").equals(organizationPartyId))
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
List invoices = delegator.findByOr("Invoice",
	UtilMisc.toMap("partyId",partyId,"partyIdFrom",partyId),
	UtilMisc.toList("invoiceDate"));
List payments = delegator.findByOr("Payment",
	UtilMisc.toMap("partyIdTo",partyId,"partyIdFrom",partyId),
	UtilMisc.toList("effectiveDate"));
	
List notAppliedInvoices = new LinkedList(); // to store the not fully applied invoices
	
// start reeading from the invoices list
if (invoices != null && invoices.size() > 0) {
	Iterator inv = invoices.iterator();
	while (inv.hasNext())	{
		invoice = inv.next();
		if (invoice.getString("statusId").equals("INVOICE_CANCELLED")) continue;
		BigDecimal invoiceAmount = InvoiceWorker.getInvoiceTotalBd(invoice).setScale(2,BigDecimal.ROUND_HALF_UP);
		invoiceApplied = InvoiceWorker.getInvoiceAppliedBd(invoice).setScale(2,BigDecimal.ROUND_HALF_UP);
/*		if (invoice.getString("partyId").equals(organizationPartyId)) { //negate for outgoing payments
			invoiceAmount = invoiceAmount.multiply(new BigDecimal("-1"));
			invoiceApplied = invoiceApplied.multiply(new BigDecimal("-1"));
		}
*/		if (invoice.getString("invoiceTypeId").equals("PURCHASE_INVOICE")) totalPurchaseInvoice = totalPurchaseInvoice.add(invoiceAmount);
		if (invoice.getString("invoiceTypeId").equals("SALES_INVOICE")) totalSalesInvoice = totalSalesInvoice.add(invoiceAmount);
		totalInvoiceApplied = totalInvoiceApplied.add(invoiceApplied);
//		Debug.logInfo("Invoice type: "+ invoice.getString("invoiceTypeId") + "amount: " + invoiceAmount + " applied: " + invoiceApplied,"??");
		if (!invoiceAmount.equals(invoiceApplied))	{
			Map notAppliedInvoice = UtilMisc.toMap(
				"invoiceId",invoice.getString("invoiceId"),
				"invoiceTypeId", invoice.getString("invoiceTypeId").substring(0,1), 
				"invoiceDate", invoice.getString("invoiceDate").substring(0,10),
				"invoiceAmount", invoiceAmount.toString(),
				"invoiceNotApplied", invoiceAmount.subtract(invoiceApplied).toString());
				notAppliedInvoices.add(notAppliedInvoice);
				totalInvoiceNotApplied = totalInvoiceNotApplied.add(invoiceAmount).subtract(invoiceApplied);
		}
		Map historyItem = UtilMisc.toMap(
			"invoiceId",invoice.getString("invoiceId"),
			"invoiceTypeId", invoice.getString("invoiceTypeId").substring(0,1), 
			"invoiceDate", invoice.getString("invoiceDate").substring(0,10),
			"invoiceAmount", invoiceAmount.toString(),
			"totInvoiceApplied", invoiceApplied.toString()
			);
			
		// check for applications
		List applications = invoice.getRelated("PaymentApplication",null,UtilMisc.toList("paymentId"));
		if (applications != null && applications.size() > 0) {
			Iterator appl = applications.iterator();
			oldPaymentId = new String(" ");
			BigDecimal applied = new BigDecimal("0");
			boolean first = true;
			while (appl.hasNext())	{	// read the applications for this invoice
				application = appl.next();
				paymentId = application.getString("paymentId");

				//reduce the payment amount in the payment list
				pl = payments.iterator();
				while (pl.hasNext()) {
					payment = pl.next();
					if (paymentId.equals(payment.getString("paymentId"))) {
						plInd = payments.indexOf(payment);
						payment.put("amount",
								payment.getBigDecimal("amount").
								subtract(application.getBigDecimal("amountApplied")).doubleValue());
						payments.remove(plInd);
						payments.add(plInd,payment);
						break;
					}
				}
				
				// check if the payment number has changed, then we have to output a line....
				if (!first && !paymentId.equals(oldPaymentId)) { // if the payment number has changed, but not the first
					historyItem.put("applied", applied.toString());
					historyItem.put("paymentId", oldPaymentId);
					payment = delegator.findByPrimaryKey("Payment",UtilMisc.toMap("paymentId",oldPaymentId));
					BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
//					if (payment.getString("partyIdFrom").equals(organizationPartyId)) amount = amount.multiply(new BigDecimal("-1"));
					historyItem.put("amount",amount.toString());
					historyItem.put("effectiveDate",payment.getString("effectiveDate").substring(0,10));
					historyList.add(historyItem);
					historyItem = new HashMap();
					applied = new BigDecimal("0");
				}
				applied = applied.add(application.getBigDecimal("amountApplied")).setScale(2,BigDecimal.ROUND_HALF_UP);
				oldPaymentId = paymentId;
				first = false;
			}
			if (!applied.equals("0")) {
				historyItem.put("applied", applied.toString());
				historyItem.put("paymentId", oldPaymentId);
				payment = delegator.findByPrimaryKey("Payment",UtilMisc.toMap("paymentId",oldPaymentId));
				if (payment != null) {
					BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
//					if (payment.getString("partyIdFrom").equals(organizationPartyId))	amount = amount.multiply(new BigDecimal("-1"));
					historyItem.put("amount",amount.toString());
					historyItem.put("effectiveDate",payment.getString("effectiveDate").substring(0,10));
					historyList.add(historyItem);
				}
			}
		}
	}
	context.put("historyListInvoices",historyList);
}

// totals
BigDecimal totalPaymentApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
// list of payments where payments are applied
historyList = new LinkedList();
if (payments != null && payments.size() > 0) {
	pay = payments.iterator();
	while (pay.hasNext())	{
		payment = pay.next();
		if (payment.getString("statusId").equals("PMNT_CANCELLED")) continue;
		List applications = payment.getRelated("ToPaymentApplication",null,UtilMisc.toList("paymentId"));
		if (applications == null || applications.size() == 0 ) continue; // none found				
		Map historyItem = UtilMisc.toMap("paymentId",payment.getString("paymentId"),"effectiveDate",payment.getString("effectiveDate").substring(0,10));
		BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
//		if (payment.getString("partyIdFrom").equals(organizationPartyId)) amount = amount.multiply(new BigDecimal("-1"));
		historyItem.put("amount",amount.toString());
		ap = applications.iterator();
		while (ap.hasNext())	{
			GenericValue application = ap.next();
			historyItem.put("toPaymentId",application.getString("paymentId"));
			historyItem.put("applied",application.getString("amountApplied"));
			toPayment = application.getRelatedOne("Payment");
			historyItem.put("toEffectiveDate",toPayment.getString("effectiveDate").substring(0,10));
			toAmount = toPayment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
//			if (toPayment.getString("partyIdFrom").equals(organizationPartyId)) toAmount = toAmount.multiply(new BigDecimal("-1"));
			historyItem.put("toAmount",toAmount.toString());
			historyList.add(historyItem);
			
			//reduce the payment amount in the payment list
			pl = payments.iterator();
			while (pl.hasNext()) {
				payment = pl.next();
				// reduce paymentId
				if (application.getString("paymentId").equals(payment.getString("paymentId"))) {
					plInd = payments.indexOf(payment);
					payment.put("amount",
							payment.getBigDecimal("amount").
							subtract(application.getBigDecimal("amountApplied")).doubleValue());
					payments.remove(plInd);
					payments.add(plInd,payment);
					totalPaymentApplied = totalPaymentApplied.add(application.getBigDecimal("amountApplied"));
				}
				// reduce toPaymentId
				if (application.getString("toPaymentId").equals(payment.getString("paymentId"))) {
					plInd = payments.indexOf(payment);
					payment.put("amount",
							payment.getBigDecimal("amount").
							subtract(application.getBigDecimal("amountApplied")).doubleValue());
					payments.remove(plInd);
					payments.add(plInd,payment);
					totalPaymentApplied = totalPaymentApplied.add(application.getBigDecimal("amountApplied"));
				}
			}
		}
	}
	context.put("historyListPayments",historyList);
}


// check if any invoices left not applied
if (notAppliedInvoices != null && notAppliedInvoices.size() > 0) {
	context.put("historyListInvoicesN",notAppliedInvoices);
}
		

// list payments applied to other companies
historyList = new LinkedList();
if (payments != null && payments.size() > 0) {
	Iterator pm = payments.iterator();
	while (pm.hasNext())	{
		payment = pm.next();
		// check if payment completely applied
		BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
		if (amount.compareTo(new BigDecimal("0.00")) == 0) 
			continue;
		Debug.logInfo(" other company payments: " + payment.getString("paymentId") + " amount:" + payment.getBigDecimal("amount") + " applied:" + PaymentWorker.getPaymentAppliedBd(payment),"??");
		List paymentApplications = payment.getRelated("PaymentApplication");
		Iterator pa = paymentApplications.iterator();
		while (pa.hasNext())	{
			GenericValue paymentApplication = pa.next();
			if (paymentApplication.get("invoiceId") != null) {
				GenericValue invoice = paymentApplication.getRelatedOne("Invoice");
				if (!invoice.getString("partyId").equals(partyId)) {
					historyItem = new HashMap();
					historyItem.put("paymentId",payment.getString("paymentId"));
					historyItem.put("invoiceId",paymentApplication.getString("invoiceId"));
					historyItem.put("invoiceItemSeqId",paymentApplication.getString("invoiceItemSeqId"));
					historyItem.put("partyId",invoice.getString("partyIdFrom"));
					historyItem.put("amount",amount.toString());
					historyItem.put("applied",paymentApplication.getBigDecimal("amountApplied").setScale(2,BigDecimal.ROUND_HALF_UP).toString());
					historyItem.put("effectiveDate",payment.getString("effectiveDate").substring(0,10));
					historyList.add(historyItem);
				}
			}
		}
	}
	context.put("historyListPaymentsO",historyList);
}

// list not applied payments
BigDecimal totalPaymentNotApplied = new BigDecimal("0.00").setScale(2,BigDecimal.ROUND_HALF_UP);
historyList = new LinkedList();
if (payments != null && payments.size() > 0) {
	Iterator pm = payments.iterator();
	while (pm.hasNext())	{
		payment = pm.next();
		payment = delegator.findByPrimaryKey("Payment",UtilMisc.toMap("paymentId",payment.getString("paymentId")));
		notApplied = payment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentAppliedBd(payment)).setScale(2,BigDecimal.ROUND_HALF_UP);
		// check if payment completely applied
		Debug.logInfo(" payment: " + payment.getString("paymentId") + " amount:" + payment.getBigDecimal("amount") + " applied:" + PaymentWorker.getPaymentAppliedBd(payment),"??");  
		if (notApplied.compareTo(new BigDecimal("0.00")) == 0) 
			continue;
		historyItem = new HashMap();
		historyItem.put("paymentId",payment.getString("paymentId"));
		BigDecimal amount = payment.getBigDecimal("amount").setScale(2,BigDecimal.ROUND_HALF_UP);
		totalPaymentNotApplied = totalPaymentNotApplied.add(notApplied);
		historyItem.put("amount",amount.toString());
		historyItem.put("notApplied",notApplied.toString());
		historyItem.put("effectiveDate",payment.getString("effectiveDate").substring(0,10));
		historyList.add(historyItem);
	}
	context.put("historyListPaymentsN",historyList);
}


// create totals

finanSummary = UtilMisc.toMap(
	"totalSalesInvoice",totalSalesInvoice.toString(),	
	"totalPurchaseInvoice",totalPurchaseInvoice.toString(),	
	"totalPaymentsIn",totalPaymentsIn.toString(),	
	"totalPaymentsOut",totalPaymentsOut.toString(),	
	"totalInvoiceApplied",totalInvoiceApplied.toString(),
	"totalInvoiceNotApplied",totalInvoiceNotApplied.toString());
finanSummary.put("totalPaymentNotApplied",totalPaymentNotApplied.toString());
finanSummary.put("totalPaymentNotApplied",totalPaymentNotApplied.toString());
totalToBePaid = totalSalesInvoice.add(totalPurchaseInvoice).subtract(totalInvoiceApplied).subtract(totalPaymentNotApplied);
if (totalToBePaid.compareTo(new BigDecimal("0.00")) < 0 ) finanSummary.put("totalToBePaid",totalToBePaid.toString());
else if (totalToBePaid.compareTo(new BigDecimal("0.00")) > 0 ) finanSummary.put("totalToBeReceived",totalToBePaid.toString());
else	{
	finanSummary.put("totalToBePaid","0.00");
	finanSummary.put("totalToBeReceived","0.00");
}
 context.put("finanSummary",finanSummary);
	