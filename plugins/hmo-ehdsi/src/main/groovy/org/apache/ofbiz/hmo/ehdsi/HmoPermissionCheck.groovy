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
 * hmoPermissionCheck
 *
 * Standard OFBiz-style permission check for the HMO module.
 * A user passes if they hold HMO_ADMIN or the specific permissionId
 * derived from "HMO_" + mainAction (e.g. mainAction="ENROLL" → HMO_ENROLL).
 */
def hmoPermissionCheck() {
    def userLogin = context.userLogin
    def security  = context.security
    def mainAction = context.mainAction

    if (!userLogin) {
        return error("User not logged in")
    }

    // HMO_ADMIN grants everything
    if (security.hasPermission("HMO_ADMIN", userLogin)) {
        return success([hasPermission: true])
    }

    // Check the specific HMO permission
    String permissionId = "HMO_${mainAction}"
    if (security.hasPermission(permissionId, userLogin)) {
        return success([hasPermission: true])
    }

    String failMessage = "You do not have permission to perform this action (${permissionId} required)."
    return success([hasPermission: false, failMessage: failMessage])
}
