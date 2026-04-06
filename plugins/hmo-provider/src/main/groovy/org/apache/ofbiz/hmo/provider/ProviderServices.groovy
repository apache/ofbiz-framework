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
import org.apache.ofbiz.entity.util.EntityUtil

/**
 * ProviderServices — Groovy service implementations for the hmo-provider plugin.
 *
 * Contains: approvePreAuthorization, denyPreAuthorization,
 *           submitClaim, reviewClaim, approveClaim, rejectClaim, markClaimPaid
 *
 * All status transitions are validated against the OFBiz StatusValidChange table.
 */

/**
 * approvePreAuthorization — transition a pending pre-auth to APPROVED.
 */
def approvePreAuthorization() {
    def delegator   = context.delegator
    def userLogin   = context.userLogin
    String preAuthId     = context.preAuthId
    BigDecimal approvedAmount = context.approvedAmount
    String reviewNotes   = context.reviewNotes

    def preAuth = delegator.findOne("HmoPreAuthorization", [preAuthId: preAuthId], false)
    if (!preAuth) {
        return error("Pre-authorization not found: ${preAuthId}")
    }

    validateStatusTransition(delegator, preAuth.statusId, "HMO_AUTH_APPROVED")

    preAuth.statusId         = "HMO_AUTH_APPROVED"
    preAuth.reviewedByPartyId = userLogin.partyId
    if (approvedAmount != null) preAuth.approvedAmount = approvedAmount
    if (reviewNotes)            preAuth.reviewNotes    = reviewNotes
    preAuth.store()

    return success()
}

/**
 * denyPreAuthorization — transition a pending pre-auth to DENIED.
 */
def denyPreAuthorization() {
    def delegator = context.delegator
    def userLogin = context.userLogin
    String preAuthId   = context.preAuthId
    String reviewNotes = context.reviewNotes

    def preAuth = delegator.findOne("HmoPreAuthorization", [preAuthId: preAuthId], false)
    if (!preAuth) {
        return error("Pre-authorization not found: ${preAuthId}")
    }

    validateStatusTransition(delegator, preAuth.statusId, "HMO_AUTH_DENIED")

    preAuth.statusId          = "HMO_AUTH_DENIED"
    preAuth.reviewedByPartyId  = userLogin.partyId
    if (reviewNotes) preAuth.reviewNotes = reviewNotes
    preAuth.store()

    return success()
}

/**
 * submitClaim — create a new HmoClaim + optional HmoClaimLines.
 */
def submitClaim() {
    def delegator       = context.delegator
    def userLogin       = context.userLogin
    String enrollmentId    = context.enrollmentId
    String providerPartyId = context.providerPartyId
    String claimTypeEnumId = context.claimTypeEnumId
    String preAuthId       = context.preAuthId
    def serviceFromDate    = context.serviceFromDate
    def serviceThruDate    = context.serviceThruDate
    BigDecimal billedAmount = context.billedAmount
    String currencyUomId   = context.currencyUomId
    List claimLines        = context.claimLines ?: []

    // If pre-auth is supplied, verify it is approved and matches the enrollment
    if (preAuthId) {
        def preAuth = delegator.findOne("HmoPreAuthorization", [preAuthId: preAuthId], false)
        if (!preAuth) {
            return error("Pre-authorization not found: ${preAuthId}")
        }
        if (preAuth.statusId != "HMO_AUTH_APPROVED") {
            return error("Pre-authorization is not approved (status: ${preAuth.statusId}).")
        }
    }

    // Create the claim
    String claimId = delegator.getNextSeqId("HmoClaim")
    def claim = delegator.makeValue("HmoClaim")
    claim.claimId          = claimId
    claim.preAuthId        = preAuthId
    claim.enrollmentId     = enrollmentId
    claim.providerPartyId  = providerPartyId
    claim.claimTypeEnumId  = claimTypeEnumId
    claim.serviceFromDate  = serviceFromDate
    claim.serviceThruDate  = serviceThruDate
    claim.submissionDate   = new Timestamp(System.currentTimeMillis())
    claim.statusId         = "HMO_CLM_SUBMITTED"
    claim.billedAmount     = billedAmount
    claim.currencyUomId    = currencyUomId
    delegator.create(claim)

    // Create claim lines
    claimLines.eachWithIndex { line, idx ->
        String seqId = String.format("%05d", idx + 1)
        def claimLine = delegator.makeValue("HmoClaimLine")
        claimLine.claimId        = claimId
        claimLine.claimLineSeqId = seqId
        claimLine.procedureCode  = line.procedureCode
        claimLine.diagnosisCode  = line.diagnosisCode
        claimLine.description    = line.description
        claimLine.quantity       = line.quantity ?: 1
        claimLine.unitAmount     = line.unitAmount ?: 0
        claimLine.lineAmount     = line.lineAmount ?: (line.unitAmount ?: 0) * (line.quantity ?: 1)
        claimLine.currencyUomId  = currencyUomId
        delegator.create(claimLine)
    }

    return success([claimId: claimId])
}

/**
 * reviewClaim — move a submitted claim to Under Review.
 */
def reviewClaim() {
    def delegator = context.delegator
    String claimId    = context.claimId
    String reviewNotes = context.reviewNotes

    def claim = delegator.findOne("HmoClaim", [claimId: claimId], false)
    if (!claim) {
        return error("Claim not found: ${claimId}")
    }

    validateStatusTransition(delegator, claim.statusId, "HMO_CLM_REVIEWING")
    claim.statusId = "HMO_CLM_REVIEWING"
    if (reviewNotes) claim.reviewNotes = reviewNotes
    claim.store()

    return success()
}

/**
 * approveClaim — approve a claim and set the approved amount.
 */
def approveClaim() {
    def delegator = context.delegator
    def userLogin = context.userLogin
    String claimId       = context.claimId
    BigDecimal approvedAmount = context.approvedAmount
    String reviewNotes   = context.reviewNotes

    def claim = delegator.findOne("HmoClaim", [claimId: claimId], false)
    if (!claim) {
        return error("Claim not found: ${claimId}")
    }

    validateStatusTransition(delegator, claim.statusId, "HMO_CLM_APPROVED")
    claim.statusId         = "HMO_CLM_APPROVED"
    claim.approvedAmount   = approvedAmount
    claim.reviewedByPartyId = userLogin.partyId
    if (reviewNotes) claim.reviewNotes = reviewNotes
    claim.store()

    return success()
}

/**
 * rejectClaim — reject a claim.
 */
def rejectClaim() {
    def delegator = context.delegator
    def userLogin = context.userLogin
    String claimId          = context.claimId
    String denialReasonCode = context.denialReasonCode
    String reviewNotes      = context.reviewNotes

    def claim = delegator.findOne("HmoClaim", [claimId: claimId], false)
    if (!claim) {
        return error("Claim not found: ${claimId}")
    }

    validateStatusTransition(delegator, claim.statusId, "HMO_CLM_REJECTED")
    claim.statusId          = "HMO_CLM_REJECTED"
    claim.reviewedByPartyId  = userLogin.partyId
    if (denialReasonCode) claim.denialReasonCode = denialReasonCode
    if (reviewNotes)      claim.reviewNotes      = reviewNotes
    claim.store()

    return success()
}

/**
 * markClaimPaid — record payment of an approved claim.
 */
def markClaimPaid() {
    def delegator = context.delegator
    String claimId       = context.claimId
    BigDecimal paidAmount = context.paidAmount
    String reviewNotes   = context.reviewNotes

    def claim = delegator.findOne("HmoClaim", [claimId: claimId], false)
    if (!claim) {
        return error("Claim not found: ${claimId}")
    }

    validateStatusTransition(delegator, claim.statusId, "HMO_CLM_PAID")
    claim.statusId   = "HMO_CLM_PAID"
    claim.paidAmount = paidAmount
    if (reviewNotes) claim.reviewNotes = reviewNotes
    claim.store()

    return success()
}

/**
 * Validates a status transition using the OFBiz StatusValidChange table.
 * Throws an exception (propagated as an error result) if the transition is invalid.
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
