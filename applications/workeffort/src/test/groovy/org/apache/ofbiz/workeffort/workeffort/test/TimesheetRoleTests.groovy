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
package org.apache.ofbiz.workeffort.workeffort.test

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class TimesheetRoleTests implements JupiterTestHelper {

    // createTimesheetRole's in-validate eca rejects a party/role combination the party does not
    // already hold, before it reaches ensurePartyRole (which would otherwise silently create a
    // spurious PartyRole). DemoCustomer's seeded PartyRole set (OrderDemoData.xml) is exactly
    // {BILL_TO_CUSTOMER, CONTACT, CUSTOMER, END_USER_CUSTOMER, PLACING_CUSTOMER, SHIP_TO_CUSTOMER} --
    // CARRIER is provably not among them.
    @Test
    @Order(1)
    void testCreateTimesheetRole_rejectsRoleThePartyDoesNotHold() {
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-1'
        Map serviceCtx = [
            timesheetId: timesheetId,
            partyId: 'DemoCustomer',
            roleTypeId: 'CARRIER',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTimesheetRole', serviceCtx)
        assert ServiceUtil.isError(serviceResult)
    }

    @Test
    @Order(2)
    void testCreateTimesheetRole_allowsRoleThePartyAlreadyHolds() {
        String timesheetId = testParams.timesheetId ?: 'TestTimesheet-1'
        Map serviceCtx = [
            timesheetId: timesheetId,
            partyId: 'DemoCustomer',
            roleTypeId: 'CUSTOMER',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTimesheetRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
    }

}
