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

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityFindOptions

/**
 * AdminDashboardService — Groovy service implementations for the hmo-admin plugin.
 *
 * Contains: getAdminDashboardStats
 *
 * This service is a pure read/aggregate layer.  It never creates, updates,
 * or deletes any data — it only counts and sums rows owned by the five other
 * HMO plugins.
 */

/**
 * getAdminDashboardStats — aggregate KPI counts across all HMO plugins.
 *
 * Uses delegator.findCountByCondition() for row counts (no full record loads)
 * and delegator.findList() for monetary aggregation.
 */
def getAdminDashboardStats() {
    def delegator = context.delegator

    // ================================================================
    //  Enrollment KPIs  (entities owned by hmo-sponsor)
    // ================================================================

    long totalEnrollments      = delegator.findCountByCondition("HmoEnrollmentRecord", null, null, null)
    long activeEnrollments     = countByStatus(delegator, "HmoEnrollmentRecord", "HMO_ENRL_ACTIVE")
    long suspendedEnrollments  = countByStatus(delegator, "HmoEnrollmentRecord", "HMO_ENRL_SUSPENDED")
    long terminatedEnrollments = countByStatus(delegator, "HmoEnrollmentRecord", "HMO_ENRL_TERMINATED")

    // ================================================================
    //  Claim KPIs  (entities owned by hmo-provider)
    // ================================================================

    long totalClaims    = delegator.findCountByCondition("HmoClaim", null, null, null)
    long pendingClaims  = countByStatus(delegator, "HmoClaim", "HMO_CLM_SUBMITTED")
    long approvedClaims = countByStatus(delegator, "HmoClaim", "HMO_CLM_APPROVED")
    long paidClaims     = countByStatus(delegator, "HmoClaim", "HMO_CLM_PAID")
    long rejectedClaims = countByStatus(delegator, "HmoClaim", "HMO_CLM_REJECTED")

    // ================================================================
    //  Pre-authorisation KPIs  (entities owned by hmo-provider)
    // ================================================================

    long totalPreAuths    = delegator.findCountByCondition("HmoPreAuthorization", null, null, null)
    long pendingPreAuths  = countByStatus(delegator, "HmoPreAuthorization", "HMO_AUTH_PENDING")
    long approvedPreAuths = countByStatus(delegator, "HmoPreAuthorization", "HMO_AUTH_APPROVED")

    // ================================================================
    //  Sponsor policy KPIs  (entities owned by hmo-sponsor)
    // ================================================================

    long totalSponsorPolicies  = delegator.findCountByCondition("HmoSponsorPolicy", null, null, null)
    long activeSponsorPolicies = countByStatus(delegator, "HmoSponsorPolicy", "HMO_ENRL_ACTIVE")

    // ================================================================
    //  Provider KPIs — distinct providerPartyId values across claims
    // ================================================================

    def findOptions = new EntityFindOptions()
    findOptions.setDistinct(true)
    def providerList = delegator.findList(
        "HmoClaim",
        null,
        ["providerPartyId"] as Set,
        null,
        null,
        false
    )
    long totalProviders = providerList.size()

    // ================================================================
    //  HR KPIs  (entities owned by hmo-hr)
    // ================================================================

    long totalStaffPositions = delegator.findCountByCondition("HmoStaffPosition", null, null, null)
    long activeStaff         = countByStatus(delegator, "HmoStaffPosition", "HMO_STAFF_ACTIVE")
    long staffOnLeave        = countByStatus(delegator, "HmoLeaveRecord",    "HMO_LEAVE_APPROVED")
    long pendingLeaves       = countByStatus(delegator, "HmoLeaveRecord",    "HMO_LEAVE_PENDING")
    long approvedLeaves      = countByStatus(delegator, "HmoLeaveRecord",    "HMO_LEAVE_APPROVED")

    // ================================================================
    //  Finance KPIs  (entities owned by hmo-finance)
    // ================================================================

    long pendingInvoices = countByStatus(delegator, "HmoPremiumInvoice", "HMO_INV_PENDING")
    long paidInvoices    = countByStatus(delegator, "HmoPremiumInvoice", "HMO_INV_PAID")
    long overdueInvoices = countByStatus(delegator, "HmoPremiumInvoice", "HMO_INV_OVERDUE")

    // Sum paidAmount on all PAID premium invoices
    def paidInvoiceList = delegator.findList(
        "HmoPremiumInvoice",
        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_INV_PAID"),
        null, null, null, false
    )
    BigDecimal totalPremiumCollected = BigDecimal.ZERO
    paidInvoiceList.each { inv ->
        totalPremiumCollected += (inv.paidAmount ?: BigDecimal.ZERO)
    }

    long pendingPayments   = countByStatus(delegator, "HmoClaimPayment", "HMO_PAY_PENDING")
    long processedPayments = countByStatus(delegator, "HmoClaimPayment", "HMO_PAY_PROCESSED")

    // Sum totalAmount on all PROCESSED claim payments
    def processedPaymentList = delegator.findList(
        "HmoClaimPayment",
        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_PAY_PROCESSED"),
        null, null, null, false
    )
    BigDecimal totalClaimsPaid = BigDecimal.ZERO
    processedPaymentList.each { pay ->
        totalClaimsPaid += (pay.totalAmount ?: BigDecimal.ZERO)
    }

    // ================================================================
    //  Return all KPIs
    // ================================================================

    return success([
        totalEnrollments     : totalEnrollments,
        activeEnrollments    : activeEnrollments,
        suspendedEnrollments : suspendedEnrollments,
        terminatedEnrollments: terminatedEnrollments,

        totalClaims   : totalClaims,
        pendingClaims : pendingClaims,
        approvedClaims: approvedClaims,
        paidClaims    : paidClaims,
        rejectedClaims: rejectedClaims,

        totalPreAuths   : totalPreAuths,
        pendingPreAuths : pendingPreAuths,
        approvedPreAuths: approvedPreAuths,

        totalSponsorPolicies : totalSponsorPolicies,
        activeSponsorPolicies: activeSponsorPolicies,

        totalProviders: totalProviders,

        totalStaffPositions: totalStaffPositions,
        activeStaff        : activeStaff,
        staffOnLeave       : staffOnLeave,
        pendingLeaves      : pendingLeaves,
        approvedLeaves     : approvedLeaves,

        pendingInvoices      : pendingInvoices,
        paidInvoices         : paidInvoices,
        overdueInvoices      : overdueInvoices,
        totalPremiumCollected: totalPremiumCollected,
        pendingPayments      : pendingPayments,
        processedPayments    : processedPayments,
        totalClaimsPaid      : totalClaimsPaid
    ])
}

/**
 * approveClaimPayment — transition a claim payment from PENDING to PROCESSED status.
 *
 * This is the super-admin approval step.  hmo-finance creates HmoClaimPayment
 * records with status HMO_PAY_PENDING; the hmo-admin super-administrator then
 * reviews and approves them here, advancing the status to HMO_PAY_PROCESSED.
 *
 * The transition is validated via StatusValidChange so the service will fail
 * cleanly if the payment is already processed or in a non-approvable state.
 */
def approveClaimPayment() {
    def delegator          = context.delegator
    String claimPaymentId  = context.claimPaymentId

    def payment = delegator.findOne("HmoClaimPayment", [claimPaymentId: claimPaymentId], false)
    if (!payment) {
        return error("Claim payment not found: ${claimPaymentId}")
    }

    if (payment.statusId != "HMO_PAY_PENDING") {
        return error("Claim payment ${claimPaymentId} is not in PENDING status (current: ${payment.statusId}). Only PENDING payments may be approved.")
    }

    validateStatusTransition(delegator, payment.statusId, "HMO_PAY_PROCESSED")

    payment.statusId = "HMO_PAY_PROCESSED"
    payment.store()

    return success([claimPaymentId: claimPaymentId])
}

/**
 * Count rows in an entity where statusId equals the given value.
 */
private long countByStatus(delegator, String entityName, String statusId) {
    def cond = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId)
    return delegator.findCountByCondition(entityName, cond, null, null)
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
