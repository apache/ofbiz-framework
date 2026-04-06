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

import java.sql.Timestamp
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

/**
 * FinanceServices — Groovy service implementations for the hmo-finance plugin.
 *
 * Contains: createPremiumInvoice, markPremiumInvoicePaid,
 *           createClaimPayment, generateHmoFinancialReport
 */

/** Default payment terms in days for newly created premium invoices. */
private static final int DEFAULT_PAYMENT_TERM_DAYS = 30

/** Fallback currency used when no currency is set on a claim or schedule. */
private static final String DEFAULT_CURRENCY_UOM_ID = "NGN"

/**
 * createPremiumInvoice — generate a premium invoice for a sponsor policy period.
 *
 * Counts active enrollments under the policy for the billing period,
 * calculates totalAmount = policy.premiumAmount × enrolleeCount, and
 * creates an HmoPremiumInvoice with status HMO_INV_PENDING.
 */
def createPremiumInvoice() {
    def delegator              = context.delegator
    String sponsorPolicyId     = context.sponsorPolicyId
    def billingPeriodFromDate  = context.billingPeriodFromDate
    def billingPeriodThruDate  = context.billingPeriodThruDate

    // Load the sponsor policy
    def policy = delegator.findOne("HmoSponsorPolicy", [sponsorPolicyId: sponsorPolicyId], false)
    if (!policy) {
        return error("Sponsor policy not found: ${sponsorPolicyId}")
    }

    // Count active enrollments under this policy
    def enrollCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("sponsorPolicyId", EntityOperator.EQUALS, sponsorPolicyId),
        EntityCondition.makeCondition("statusId",        EntityOperator.EQUALS, "HMO_ENRL_ACTIVE")
    ], EntityOperator.AND)
    def enrollments = delegator.findList("HmoEnrollmentRecord", enrollCond, null, null, null, false)
    long enrolleeCount = enrollments.size()

    // Calculate total invoice amount
    BigDecimal premiumAmount = policy.premiumAmount ?: BigDecimal.ZERO
    BigDecimal totalAmount   = premiumAmount * enrolleeCount

    // Create the invoice record
    String premiumInvoiceId = delegator.getNextSeqId("HmoPremiumInvoice")
    def now = new Timestamp(System.currentTimeMillis())

    def invoice = delegator.makeValue("HmoPremiumInvoice")
    invoice.premiumInvoiceId      = premiumInvoiceId
    invoice.sponsorPolicyId       = sponsorPolicyId
    invoice.sponsorPartyId        = policy.partyId
    invoice.billingPeriodFromDate = billingPeriodFromDate
    invoice.billingPeriodThruDate = billingPeriodThruDate
    invoice.invoiceDate           = now
    invoice.dueDate               = new Timestamp(now.getTime() + DEFAULT_PAYMENT_TERM_DAYS * 24L * 60 * 60 * 1000)
    invoice.enrolleeCount         = enrolleeCount
    invoice.premiumAmount         = premiumAmount
    invoice.totalAmount           = totalAmount
    invoice.currencyUomId         = policy.currencyUomId
    invoice.statusId              = "HMO_INV_PENDING"
    delegator.create(invoice)

    return success([premiumInvoiceId: premiumInvoiceId, totalAmount: totalAmount])
}

/**
 * markPremiumInvoicePaid — transition a premium invoice to PAID status.
 *
 * Validates the status transition via StatusValidChange then stores paidAmount.
 */
def markPremiumInvoicePaid() {
    def delegator          = context.delegator
    String premiumInvoiceId = context.premiumInvoiceId
    BigDecimal paidAmount  = context.paidAmount
    def paymentDate        = context.paymentDate ?: new Timestamp(System.currentTimeMillis())

    def invoice = delegator.findOne("HmoPremiumInvoice", [premiumInvoiceId: premiumInvoiceId], false)
    if (!invoice) {
        return error("Premium invoice not found: ${premiumInvoiceId}")
    }

    validateStatusTransition(delegator, invoice.statusId, "HMO_INV_PAID")

    invoice.paidAmount = paidAmount
    invoice.statusId   = "HMO_INV_PAID"
    invoice.store()

    return success()
}

/**
 * createClaimPayment — create a disbursement batch for a provider covering
 * one or more approved HmoClaim records.
 *
 * Validates each claim is in APPROVED status, sums approvedAmounts, creates
 * HmoClaimPayment + HmoClaimPaymentItem records, and marks each claim PAID.
 */
def createClaimPayment() {
    def delegator         = context.delegator
    String providerPartyId = context.providerPartyId
    List claimIds         = context.claimIds
    String paymentMethod  = context.paymentMethod
    String referenceNumber = context.referenceNumber
    def paymentDate       = context.paymentDate ?: new Timestamp(System.currentTimeMillis())

    if (!claimIds) {
        return error("No claim IDs provided for payment.")
    }

    // Validate each claim and accumulate total
    BigDecimal totalAmount = BigDecimal.ZERO
    def claimValues = []
    String currencyUomId = null

    for (String claimId : claimIds) {
        def claim = delegator.findOne("HmoClaim", [claimId: claimId], false)
        if (!claim) {
            return error("Claim not found: ${claimId}")
        }
        if (claim.statusId != "HMO_CLM_APPROVED") {
            return error("Claim ${claimId} is not in APPROVED status (current: ${claim.statusId}).")
        }
        BigDecimal approvedAmount = claim.approvedAmount ?: BigDecimal.ZERO
        totalAmount += approvedAmount
        currencyUomId = claim.currencyUomId ?: currencyUomId        claimValues << [claim: claim, allocatedAmount: approvedAmount]
    }

    // Create the payment batch
    String claimPaymentId = delegator.getNextSeqId("HmoClaimPayment")

    def payment = delegator.makeValue("HmoClaimPayment")
    payment.claimPaymentId  = claimPaymentId
    payment.providerPartyId = providerPartyId
    payment.paymentDate     = paymentDate
    payment.paymentMethod   = paymentMethod
    payment.totalAmount     = totalAmount
    payment.currencyUomId   = currencyUomId ?: DEFAULT_CURRENCY_UOM_ID
    payment.referenceNumber = referenceNumber
    payment.statusId        = "HMO_PAY_PENDING"
    delegator.create(payment)

    // Create line items and mark claims paid
    claimValues.each { entry ->
        def item = delegator.makeValue("HmoClaimPaymentItem")
        item.claimPaymentId  = claimPaymentId
        item.claimId         = entry.claim.claimId
        item.allocatedAmount = entry.allocatedAmount
        delegator.create(item)

        // Payment service is authoritative — update claim status directly
        entry.claim.statusId = "HMO_CLM_PAID"
        entry.claim.store()
    }

    return success([claimPaymentId: claimPaymentId, totalAmount: totalAmount])
}

/**
 * generateHmoFinancialReport — produce a financial summary for a date range.
 *
 * Aggregates:
 *   totalPremiumCollected — sum of paidAmount on PAID invoices in the period
 *   totalClaimsPaid       — sum of totalAmount on claim payments in the period
 *   totalCapitationPaid   — stub (capitation payments are recorded externally)
 *   netSurplus            — totalPremiumCollected - totalClaimsPaid - totalCapitationPaid
 */
def generateHmoFinancialReport() {
    def delegator      = context.delegator
    def reportFromDate = context.reportFromDate
    def reportThruDate = context.reportThruDate

    // Sum paid premium invoices whose billing period overlaps the report period
    def invoiceCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("statusId",             EntityOperator.EQUALS,                  "HMO_INV_PAID"),
        EntityCondition.makeCondition("billingPeriodFromDate", EntityOperator.GREATER_THAN_EQUAL_TO,  reportFromDate),
        EntityCondition.makeCondition("billingPeriodFromDate", EntityOperator.LESS_THAN_EQUAL_TO,     reportThruDate)
    ], EntityOperator.AND)
    def paidInvoices = delegator.findList("HmoPremiumInvoice", invoiceCond, null, null, null, false)
    BigDecimal totalPremiumCollected = BigDecimal.ZERO
    String currencyUomId = DEFAULT_CURRENCY_UOM_ID
    paidInvoices.each { inv ->
        totalPremiumCollected += (inv.paidAmount ?: BigDecimal.ZERO)
        if (inv.currencyUomId) currencyUomId = inv.currencyUomId
    }

    // Sum claim payments in the period
    def paymentCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("paymentDate", EntityOperator.GREATER_THAN_EQUAL_TO, reportFromDate),
        EntityCondition.makeCondition("paymentDate", EntityOperator.LESS_THAN_EQUAL_TO,    reportThruDate)
    ], EntityOperator.AND)
    def claimPayments = delegator.findList("HmoClaimPayment", paymentCond, null, null, null, false)
    BigDecimal totalClaimsPaid = BigDecimal.ZERO
    claimPayments.each { pay ->
        totalClaimsPaid += (pay.totalAmount ?: BigDecimal.ZERO)
    }

    // Capitation: iterate active schedules and estimate monthly cost
    BigDecimal totalCapitationPaid = BigDecimal.ZERO
    def capCond = EntityCondition.makeCondition(
        EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, reportThruDate),
        EntityOperator.AND,
        EntityCondition.makeCondition(
            EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null),
            EntityOperator.OR,
            EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, reportFromDate)
        )
    )
    def capSchedules = delegator.findList("HmoCapitationSchedule", capCond, null, null, null, false)
    capSchedules.each { sched ->
        // Count active enrollees on the matching plan tier for this provider
        def memberCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_ENRL_ACTIVE")
        ], EntityOperator.AND)
        def members = delegator.findList("HmoEnrollmentRecord", memberCond, null, null, null, false)
        long memberCount = members.size()
        totalCapitationPaid += (sched.capitationAmountPerMember ?: BigDecimal.ZERO) * memberCount
    }

    BigDecimal netSurplus = totalPremiumCollected - totalClaimsPaid - totalCapitationPaid

    return success([
        totalPremiumCollected: totalPremiumCollected,
        totalClaimsPaid      : totalClaimsPaid,
        totalCapitationPaid  : totalCapitationPaid,
        netSurplus           : netSurplus,
        currencyUomId        : currencyUomId,
        claimPaymentList     : claimPayments
    ])
}

/**
 * Validates a status transition using the OFBiz StatusValidChange table.
 */
private void validateStatusTransition(delegator, String fromStatusId, String toStatusId) {
    def validChange = delegator.findOne("StatusValidChange", [
        statusId  : fromStatusId,
        statusIdTo: toStatusId
    ], true)
    if (!validChange) {
        throw new IllegalStateException(
            "Invalid status transition from '${fromStatusId}' to '${toStatusId}'."
        )
    }
}
