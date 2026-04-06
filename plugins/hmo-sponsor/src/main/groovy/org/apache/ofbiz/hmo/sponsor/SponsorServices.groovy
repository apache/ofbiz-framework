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
import org.apache.ofbiz.entity.util.EntityUtil

/**
 * SponsorServices — Groovy service implementations for the hmo-sponsor plugin.
 *
 * Contains: enrollMember, terminateEnrollment, suspendEnrollment,
 *           reinstateEnrollment, generateSponsorBillingStatement
 */

/**
 * enrollMember — create an HmoEnrollmentRecord for a new employee enrollee.
 *
 * Generates a unique member ID in the format HMO-YYYYMMDD-NNNNNN.
 */
def enrollMember() {
    def delegator            = context.delegator
    String enrolleePartyId   = context.enrolleePartyId
    String sponsorPolicyId   = context.sponsorPolicyId
    String coverageTypeEnumId = context.coverageTypeEnumId
    String primaryEnrolleePartyId = context.primaryEnrolleePartyId
    def fromDate             = context.fromDate ?: new Timestamp(System.currentTimeMillis())
    def thruDate             = context.thruDate
    String groupId           = context.groupId

    // Validate the sponsor policy if supplied
    if (sponsorPolicyId) {
        def policy = delegator.findOne("HmoSponsorPolicy", [sponsorPolicyId: sponsorPolicyId], false)
        if (!policy) {
            return error("Sponsor policy not found: ${sponsorPolicyId}")
        }
        if (policy.statusId == "HMO_ENRL_TERMINATED") {
            return error("Sponsor policy is terminated and cannot accept new enrollments.")
        }
    }

    // Generate a sequential enrollment ID
    String enrollmentId = delegator.getNextSeqId("HmoEnrollmentRecord")

    // Generate a human-readable member card number
    String datePart   = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date())
    String seqPart    = enrollmentId.padLeft(6, '0')
    String memberIdNumber = "HMO-${datePart}-${seqPart}"

    def enrollment = delegator.makeValue("HmoEnrollmentRecord")
    enrollment.enrollmentId           = enrollmentId
    enrollment.sponsorPolicyId        = sponsorPolicyId
    enrollment.enrolleePartyId        = enrolleePartyId
    enrollment.primaryEnrolleePartyId = primaryEnrolleePartyId
    enrollment.coverageTypeEnumId     = coverageTypeEnumId
    enrollment.memberIdNumber         = memberIdNumber
    enrollment.fromDate               = fromDate
    enrollment.thruDate               = thruDate
    enrollment.statusId               = "HMO_ENRL_ACTIVE"
    enrollment.groupId                = groupId
    delegator.create(enrollment)

    return success([enrollmentId: enrollmentId, memberIdNumber: memberIdNumber])
}

/**
 * terminateEnrollment — set enrollment status to TERMINATED.
 */
def terminateEnrollment() {
    def delegator      = context.delegator
    String enrollmentId = context.enrollmentId
    def terminationDate = context.terminationDate ?: new Timestamp(System.currentTimeMillis())
    String reason       = context.reason

    def enrollment = delegator.findOne("HmoEnrollmentRecord", [enrollmentId: enrollmentId], false)
    if (!enrollment) {
        return error("Enrollment record not found: ${enrollmentId}")
    }

    validateStatusTransition(delegator, enrollment.statusId, "HMO_ENRL_TERMINATED")

    enrollment.statusId = "HMO_ENRL_TERMINATED"
    enrollment.thruDate = terminationDate
    enrollment.store()

    return success()
}

/**
 * suspendEnrollment — set enrollment status to SUSPENDED.
 */
def suspendEnrollment() {
    def delegator      = context.delegator
    String enrollmentId = context.enrollmentId

    def enrollment = delegator.findOne("HmoEnrollmentRecord", [enrollmentId: enrollmentId], false)
    if (!enrollment) {
        return error("Enrollment record not found: ${enrollmentId}")
    }

    validateStatusTransition(delegator, enrollment.statusId, "HMO_ENRL_SUSPENDED")

    enrollment.statusId = "HMO_ENRL_SUSPENDED"
    enrollment.store()

    return success()
}

/**
 * reinstateEnrollment — set enrollment status back to ACTIVE from SUSPENDED.
 */
def reinstateEnrollment() {
    def delegator      = context.delegator
    String enrollmentId = context.enrollmentId

    def enrollment = delegator.findOne("HmoEnrollmentRecord", [enrollmentId: enrollmentId], false)
    if (!enrollment) {
        return error("Enrollment record not found: ${enrollmentId}")
    }

    validateStatusTransition(delegator, enrollment.statusId, "HMO_ENRL_ACTIVE")

    enrollment.statusId = "HMO_ENRL_ACTIVE"
    enrollment.store()

    return success()
}

/**
 * generateSponsorBillingStatement
 *
 * Calculates:
 *   - Number of active enrollees under the sponsor's policies
 *   - Total premium for the period
 *   - Total approved/paid claims cost for the period
 */
def generateSponsorBillingStatement() {
    def delegator       = context.delegator
    String sponsorPartyId = context.sponsorPartyId
    def periodFromDate  = context.periodFromDate
    def periodThruDate  = context.periodThruDate

    // Find all active policies for this sponsor
    def policyCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, sponsorPartyId),
        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "HMO_ENRL_TERMINATED")
    ], EntityOperator.AND)
    def policies = delegator.findList("HmoSponsorPolicy", policyCond, null, null, null, false)

    // Collect all active enrollment records across policies
    def allEnrollments = []
    def totalPremium   = BigDecimal.ZERO
    String currencyUomId = null

    policies.each { policy ->
        currencyUomId = policy.currencyUomId
        def enrollCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("sponsorPolicyId", EntityOperator.EQUALS, policy.sponsorPolicyId),
            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_ENRL_ACTIVE")
        ], EntityOperator.AND)
        def enrollments = delegator.findList("HmoEnrollmentRecord", enrollCond, null, null, null, false)
        allEnrollments.addAll(enrollments)
        totalPremium += (policy.premiumAmount ?: 0) * enrollments.size()
    }

    // Sum approved claims submitted by providers for these enrollees in the period
    def totalClaimsCost = BigDecimal.ZERO
    allEnrollments.each { enrollment ->
        def claimCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("enrollmentId",   EntityOperator.EQUALS,         enrollment.enrollmentId),
            EntityCondition.makeCondition("statusId",       EntityOperator.IN,             ["HMO_CLM_APPROVED", "HMO_CLM_PAID"]),
            EntityCondition.makeCondition("submissionDate", EntityOperator.GREATER_THAN_EQUAL_TO, periodFromDate),
            EntityCondition.makeCondition("submissionDate", EntityOperator.LESS_THAN_EQUAL_TO,    periodThruDate)
        ], EntityOperator.AND)
        def claims = delegator.findList("HmoClaim", claimCond, null, null, null, false)
        claims.each { claim ->
            totalClaimsCost += (claim.approvedAmount ?: 0)
        }
    }

    return success([
        activeEnrollees   : allEnrollments.size(),
        totalPremiumAmount: totalPremium,
        totalClaimsCost   : totalClaimsCost,
        currencyUomId     : currencyUomId,
        enrollmentList    : allEnrollments
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
