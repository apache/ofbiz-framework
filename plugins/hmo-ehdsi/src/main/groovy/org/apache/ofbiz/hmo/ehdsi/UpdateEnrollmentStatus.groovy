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

/**
 * updateEnrollmentStatus
 *
 * Transitions an HmoEnrollmentRecord to a new status, validating the
 * transition against the HMO_ENROLL_STATUS StatusValidChange entries.
 *
 * This shared service is called by both hmo-sponsor (sponsor-initiated
 * actions such as termination) and hmo-ehdsi (admin-initiated transitions).
 */
def updateEnrollmentStatus() {
    def delegator   = context.delegator
    def userLogin   = context.userLogin
    String enrollmentId = context.enrollmentId
    String newStatusId  = context.statusId
    String reason       = context.reason

    def enrollment = delegator.findOne("HmoEnrollmentRecord", [enrollmentId: enrollmentId], false)
    if (!enrollment) {
        return error("Enrollment record not found: ${enrollmentId}")
    }

    String currentStatusId = enrollment.statusId

    // Validate transition using StatusValidChange
    def validChange = delegator.findOne("StatusValidChange", [
        statusId  : currentStatusId,
        statusIdTo: newStatusId
    ], true)

    if (!validChange) {
        return error("Invalid status transition from '${currentStatusId}' to '${newStatusId}'.")
    }

    enrollment.statusId = newStatusId
    enrollment.store()

    return success()
}
