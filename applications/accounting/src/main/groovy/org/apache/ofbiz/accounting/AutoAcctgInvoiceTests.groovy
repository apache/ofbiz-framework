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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.GroovyScriptTestCase
class AutoAcctgInvoiceTests extends GroovyScriptTestCase {
    void testCreateInvoiceContent() {
        def userLogin = EntityQuery.use(delegator).from('UserLogin')
            .where('userLoginId', 'system')
            .cache()
            .queryOne()

        Map serviceCtx = [
            invoiceId: '1008',
            contentId: '1000',
            invoiceContentTypeId: 'COMMENTS',
            fromDate: UtilDateTime.nowTimestamp(),
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createInvoiceContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue invoiceContent = EntityQuery.use(delegator).from('InvoiceContent')
            .where('invoiceId', serviceResult.invoiceId,
                   'contentId', serviceResult.contentId,
                   'invoiceContentTypeId', serviceResult.invoiceContentTypeId)
            .queryFirst()

        assert invoiceContent.contentId == serviceResult.contentId
    }
}
