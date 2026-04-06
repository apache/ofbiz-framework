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

/**
 * getActiveCoverage
 *
 * Returns the active HmoEnrollmentRecord and associated HmoSponsorPolicy for
 * the given enrollee party, together with human-readable descriptions for the
 * plan tier and coverage type. Also returns the list of enrolled dependents.
 *
 * Used by the member self-service portal dashboard and by the sponsor portal
 * enrollment screens.
 */
def getActiveCoverage() {
    def delegator         = context.delegator
    String enrolleePartyId = context.enrolleePartyId

    // Find the active enrollment record for this enrollee
    def cond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("enrolleePartyId", EntityOperator.EQUALS, enrolleePartyId),
        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_ENRL_ACTIVE")
    ], EntityOperator.AND)
    def records = delegator.findList("HmoEnrollmentRecord", cond, null, ["-fromDate"], null, false)
    def enrollment = EntityUtil.getFirst(records)

    if (!enrollment) {
        return success([enrollmentRecord: null])
    }

    def sponsorPolicy = null
    String planTierDescription    = null
    String coverageTypeDescription = null

    if (enrollment.sponsorPolicyId) {
        sponsorPolicy = delegator.findOne("HmoSponsorPolicy", [sponsorPolicyId: enrollment.sponsorPolicyId], false)
        if (sponsorPolicy?.planTierEnumId) {
            def planEnum = delegator.findOne("Enumeration", [enumId: sponsorPolicy.planTierEnumId], true)
            planTierDescription = planEnum?.description
        }
    }

    if (enrollment.coverageTypeEnumId) {
        def covEnum = delegator.findOne("Enumeration", [enumId: enrollment.coverageTypeEnumId], true)
        coverageTypeDescription = covEnum?.description
    }

    // Load dependents: HmoEnrollmentRecords where primaryEnrolleePartyId = enrolleePartyId
    def depCond = EntityCondition.makeCondition([
        EntityCondition.makeCondition("primaryEnrolleePartyId", EntityOperator.EQUALS, enrolleePartyId),
        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "HMO_ENRL_ACTIVE")
    ], EntityOperator.AND)
    def dependents = delegator.findList("HmoEnrollmentRecord", depCond, null, ["fromDate"], null, false)

    return success([
        enrollmentRecord        : enrollment,
        sponsorPolicy           : sponsorPolicy,
        planTierDescription     : planTierDescription,
        coverageTypeDescription : coverageTypeDescription,
        dependents              : dependents ?: []
    ])
}
