/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.order.test

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class OrderTest implements JupiterTestHelper {

    @Test
    @Order(1)
    void testAdminGetNextOrderSeqId() {
        Map serviceCtx = [partyId: 'admin'] // party with no AcctgPref prefix
        Map resp = dispatcher.runSync('getNextOrderId', serviceCtx)
        if (ServiceUtil.isError(resp)) {
            logError(ServiceUtil.getErrorMessage(resp))
            return
        }
        String orderId = resp.orderId
        assert orderId
        assert orderId ==~ /\d{5,}/
    }

    @Test
    @Order(2)
    void testCompanyGetNextOrderSeqId() {
        Map serviceCtx = [partyId: 'Company'] // party with AcctgPref prefix : CO
        Map resp = dispatcher.runSync('getNextOrderId', serviceCtx)
        if (ServiceUtil.isError(resp)) {
            logError(ServiceUtil.getErrorMessage(resp))
            return
        }
        String orderId = resp.orderId
        assert orderId
        assert orderId.startsWith('CO')
    }

    @Test
    @Order(3)
    void testCompleteGetNextOrderSeqId() {
        Map serviceCtx = [
                partyId: 'Company', // party with AcctgPref prefix : CO
                productStoreId: '9000' // prefix WS
        ]
        Map resp = dispatcher.runSync('getNextOrderId', serviceCtx)
        if (ServiceUtil.isError(resp)) {
            logError(ServiceUtil.getErrorMessage(resp))
            return
        }
        String orderId = resp.orderId
        assert orderId
        assert orderId.startsWith('WSCO')
    }

}
