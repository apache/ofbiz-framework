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
 * HrServices — Groovy service implementations for the hmo-hr plugin.
 *
 * Contains: promoteStaff, approveLeave, rejectLeave, processLeaveReturn
 */

/**
 * promoteStaff — record a promotion event and update the staff position.
 *
 * Creates an HmoPromotion record capturing the before/after grade and job title,
 * then updates the HmoStaffPosition with the new values.
 */
def promoteStaff() {
    def delegator           = context.delegator
    String staffPositionId  = context.staffPositionId
    String newGradeLevelId  = context.newGradeLevelId
    String newJobTitleEnumId = context.newJobTitleEnumId
    def effectiveDate       = context.effectiveDate ?: new Timestamp(System.currentTimeMillis())
    String approvedByPartyId = context.approvedByPartyId
    String comments         = context.comments

    def staffPosition = delegator.findOne("HmoStaffPosition", [staffPositionId: staffPositionId], false)
    if (!staffPosition) {
        return error("Staff position not found: ${staffPositionId}")
    }

    // Validate target grade level exists
    def newGradeLevel = delegator.findOne("HmoGradeLevel", [gradeLevelId: newGradeLevelId], false)
    if (!newGradeLevel) {
        return error("Grade level not found: ${newGradeLevelId}")
    }

    String promotionId = delegator.getNextSeqId("HmoPromotion")

    def promotion = delegator.makeValue("HmoPromotion")
    promotion.promotionId            = promotionId
    promotion.staffPositionId        = staffPositionId
    promotion.partyId                = staffPosition.partyId
    promotion.previousGradeLevelId   = staffPosition.gradeLevelId
    promotion.newGradeLevelId        = newGradeLevelId
    promotion.previousJobTitleEnumId = staffPosition.jobTitleEnumId
    promotion.newJobTitleEnumId      = newJobTitleEnumId
    promotion.effectiveDate          = effectiveDate
    promotion.approvedByPartyId      = approvedByPartyId
    promotion.comments               = comments
    delegator.create(promotion)

    staffPosition.gradeLevelId   = newGradeLevelId
    staffPosition.jobTitleEnumId = newJobTitleEnumId
    staffPosition.store()

    return success([promotionId: promotionId])
}

/**
 * approveLeave — transition a leave record from PENDING to APPROVED.
 */
def approveLeave() {
    def delegator           = context.delegator
    String leaveId          = context.leaveId
    String approvedByPartyId = context.approvedByPartyId

    def leaveRecord = delegator.findOne("HmoLeaveRecord", [leaveId: leaveId], false)
    if (!leaveRecord) {
        return error("Leave record not found: ${leaveId}")
    }

    validateStatusTransition(delegator, leaveRecord.statusId, "HMO_LEAVE_APPROVED")

    leaveRecord.statusId          = "HMO_LEAVE_APPROVED"
    if (approvedByPartyId) {
        leaveRecord.approvedByPartyId = approvedByPartyId
    }
    leaveRecord.store()

    return success()
}

/**
 * rejectLeave — transition a leave record from PENDING to REJECTED.
 */
def rejectLeave() {
    def delegator  = context.delegator
    String leaveId = context.leaveId
    String reason  = context.reason

    def leaveRecord = delegator.findOne("HmoLeaveRecord", [leaveId: leaveId], false)
    if (!leaveRecord) {
        return error("Leave record not found: ${leaveId}")
    }

    validateStatusTransition(delegator, leaveRecord.statusId, "HMO_LEAVE_REJECTED")

    leaveRecord.statusId = "HMO_LEAVE_REJECTED"
    if (reason) {
        leaveRecord.reason = reason
    }
    leaveRecord.store()

    return success()
}

/**
 * processLeaveReturn — transition a leave record from APPROVED to COMPLETED.
 *
 * Optionally updates thruDate to the actual return date if provided.
 */
def processLeaveReturn() {
    def delegator           = context.delegator
    String leaveId          = context.leaveId
    def actualReturnDate    = context.actualReturnDate

    def leaveRecord = delegator.findOne("HmoLeaveRecord", [leaveId: leaveId], false)
    if (!leaveRecord) {
        return error("Leave record not found: ${leaveId}")
    }

    validateStatusTransition(delegator, leaveRecord.statusId, "HMO_LEAVE_COMPLETED")

    leaveRecord.statusId = "HMO_LEAVE_COMPLETED"
    if (actualReturnDate) {
        leaveRecord.thruDate = actualReturnDate
    }
    leaveRecord.store()

    return success()
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
