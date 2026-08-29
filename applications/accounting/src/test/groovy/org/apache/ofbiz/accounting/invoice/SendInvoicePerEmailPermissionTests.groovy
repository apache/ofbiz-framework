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
package org.apache.ofbiz.accounting.invoice

import org.apache.ofbiz.service.ServiceAuthException
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * sendInvoicePerEmail declared no permission-service, unlike its siblings in the same file
 * (createInvoice, getInvoice, setInvoiceStatus), so any authenticated user could reach it regardless of
 * accounting permissions. These tests guard the fix that gives it the same VIEW-level
 * acctgInvoicePermissionCheck getInvoice already declares.
 */
@JunitJupiterTest
class SendInvoicePerEmailPermissionTests implements JupiterTestHelper {

    @Test
    void sendInvoicePerEmailDeniesUserWithoutAccountingPermission() {
        // DemoCustomer carries no UserLoginSecurityGroup membership anywhere in the demo data set.
        Map params = [invoiceId: 'DEMO_PERM_TEST_NONEXISTENT',
                       sendFrom: 'nobody@example.org',
                       sendTo: 'nobody@example.org',
                       userLogin: getUserLogin('DemoCustomer')]
        try {
            Map results = getDispatcher().runSync('sendInvoicePerEmail', params)
            assert ServiceUtil.isError(results):
                    'sendInvoicePerEmail must not succeed for a userLogin holding no accounting permission'
        } catch (ServiceAuthException e) {
            assert e != null
        }
    }

    @Test
    void sendInvoicePerEmailAllowsUserWithAccountingViewPermission() {
        Map params = [invoiceId: 'DEMO_PERM_TEST_NONEXISTENT',
                       sendFrom: 'nobody@example.org',
                       sendTo: 'nobody@example.org',
                       userLogin: getUserLogin()]
        Map results = getDispatcher().runSync('sendInvoicePerEmail', params)
        assert ServiceUtil.isSuccess(results):
                'sendInvoicePerEmail must still succeed for a fully-permissioned userLogin'
    }

}
