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

import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import java.sql.Timestamp

/**
 * checkMemberEligibility
 *
 * Determines whether a member is currently eligible for HMO services by
 * looking up their active HmoEnrollmentRecord. Accepts either the member's
 * card number (memberIdNumber) or the direct enrollmentId.
 *
 * Returns:
 *   isEligible = "Y" | "N"
 *   enrollmentId, enrolleePartyId, statusId, planTierEnumId, coverageTypeEnumId,
 *   annualLimitAmount, fromDate, thruDate — when eligible
 *   ineligibilityReason — when not eligible
 */
def checkMemberEligibility() {
    def delegator = context.delegator
    String memberIdNumber = context.memberIdNumber
    String enrollmentId   = context.enrollmentId

    if (!memberIdNumber && !enrollmentId) {
        return error("Either memberIdNumber or enrollmentId must be supplied.")
    }

    def enrollment = null

    if (enrollmentId) {
        enrollment = delegator.findOne("HmoEnrollmentRecord", [enrollmentId: enrollmentId], false)
    } else {
        // Find by member card number
        def cond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("memberIdNumber", EntityOperator.EQUALS, memberIdNumber)
        ])
        def records = delegator.findList("HmoEnrollmentRecord", cond, null, ["-fromDate"], null, false)
        enrollment = EntityUtil.getFirst(records)
    }

    if (!enrollment) {
        return success([
            isEligible        : "N",
            ineligibilityReason: "No enrollment record found."
        ])
    }

    // Check active status
    if (enrollment.statusId != "HMO_ENRL_ACTIVE") {
        return success([
            isEligible         : "N",
            enrollmentId       : enrollment.enrollmentId,
            enrolleePartyId    : enrollment.enrolleePartyId,
            statusId           : enrollment.statusId,
            ineligibilityReason: "Enrollment is not active (current status: ${enrollment.statusId})."
        ])
    }

    // Check date range
    Timestamp now = new Timestamp(System.currentTimeMillis())
    if (enrollment.thruDate && enrollment.thruDate < now) {
        return success([
            isEligible         : "N",
            enrollmentId       : enrollment.enrollmentId,
            enrolleePartyId    : enrollment.enrolleePartyId,
            statusId           : enrollment.statusId,
            ineligibilityReason: "Enrollment has expired."
        ])
    }

    // Eligible — also pull plan tier from the sponsor policy
    String planTierEnumId = null
    BigDecimal annualLimitAmount = null
    if (enrollment.sponsorPolicyId) {
        def policy = delegator.findOne("HmoSponsorPolicy", [sponsorPolicyId: enrollment.sponsorPolicyId], false)
        if (policy) {
            planTierEnumId    = policy.planTierEnumId
            annualLimitAmount = policy.annualLimitAmount
        }
    }

    return success([
        isEligible          : "Y",
        enrollmentId        : enrollment.enrollmentId,
        enrolleePartyId     : enrollment.enrolleePartyId,
        statusId            : enrollment.statusId,
        planTierEnumId      : planTierEnumId,
        coverageTypeEnumId  : enrollment.coverageTypeEnumId,
        annualLimitAmount   : annualLimitAmount,
        fromDate            : enrollment.fromDate,
        thruDate            : enrollment.thruDate
    ])
}
