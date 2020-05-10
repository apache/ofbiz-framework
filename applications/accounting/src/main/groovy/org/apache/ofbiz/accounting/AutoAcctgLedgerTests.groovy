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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgLedgerTests extends OFBizTestCase {
    public AutoAcctgLedgerTests(String name) {
        super(name)
    }
    void testCreateAcctgTrans() {
        Map serviceCtx = [:]
        serviceCtx.acctgTransTypeId = 'CREDIT_MEMO'
        serviceCtx.description = 'Test Credit Memo Transaction'
        serviceCtx.transactionDate = UtilDateTime.nowTimestamp()
        serviceCtx.glFiscalTypeId = 'BUDGET'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createAcctgTrans', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue acctgTrans = from('AcctgTrans').where('acctgTransId', serviceResult.acctgTransId).queryOne()
        assert acctgTrans.acctgTransId == serviceResult.acctgTransId
        assert acctgTrans.acctgTransTypeId == 'CREDIT_MEMO'
    }
    void testCreateAcctgTransEntry() {
        Map serviceCtx = [
            acctgTransId: '1000',
            organizationPartyId: 'DEMO_COMPANY',
            debitCreditFlag: 'C',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAcctgTransEntry', serviceCtx)
        GenericValue acctgTransEntry = from('AcctgTransEntry').where('acctgTransId', '1000', 'acctgTransEntrySeqId', serviceResult.acctgTransEntrySeqId).queryOne()
        assert acctgTransEntry != null
    }
}
