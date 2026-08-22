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
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgLedgerTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateAcctgTrans() {
        String acctgTransTypeId = testParams.acctgTransTypeId ?: 'CREDIT_MEMO'
        Map serviceCtx = [:]
        serviceCtx.acctgTransTypeId = acctgTransTypeId
        serviceCtx.description = testParams.description ?: 'Test Credit Memo Transaction'
        serviceCtx.transactionDate = UtilDateTime.nowTimestamp()
        serviceCtx.glFiscalTypeId = testParams.glFiscalTypeId ?: 'BUDGET'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createAcctgTrans', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans').where('acctgTransId', serviceResult.acctgTransId).queryOne()
        assert acctgTrans.acctgTransId == serviceResult.acctgTransId
        assert acctgTrans.acctgTransTypeId == acctgTransTypeId
    }
    @Test
    @Order(2)
    void testCreateAcctgTransEntry() {
        String acctgTransId = testParams.acctgTransId ?: '1000'
        Map serviceCtx = [
            acctgTransId: acctgTransId,
            organizationPartyId: testParams.organizationPartyId ?: 'DEMO_COMPANY',
            debitCreditFlag: testParams.debitCreditFlag ?: 'C',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAcctgTransEntry', serviceCtx)
        GenericValue acctgTransEntry = from('AcctgTransEntry')
                .where('acctgTransId', acctgTransId, 'acctgTransEntrySeqId', serviceResult.acctgTransEntrySeqId).queryOne()
        assert acctgTransEntry != null
    }

}
