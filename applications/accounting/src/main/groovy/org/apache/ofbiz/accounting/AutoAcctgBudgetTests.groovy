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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgBudgetTests extends OFBizTestCase {
    public AutoAcctgBudgetTests(String name) {
        super(name)
    }

    void testCreateBudget() {
        Map serviceCtx = [:]
        serviceCtx.budgetTypeId = 'CAPITAL_BUDGET'
        serviceCtx.comments = 'Capital Budget'
        serviceCtx.userLogin = userLogin
        Map result = dispatcher.runSync('createBudget', serviceCtx)
        assert ServiceUtil.isSuccess(result)

        GenericValue budget = from('Budget').where(result).queryOne()
        assert budget
        assert budget.budgetTypeId == 'CAPITAL_BUDGET'
        assert budget.comments == 'Capital Budget'
    }

    void testUpdateBudgetStatus() {
        Map serviceCtx = [:]
        serviceCtx.budgetId = '9999'
        serviceCtx.statusId = 'BG_APPROVED'
        serviceCtx.userLogin = userLogin
        Map result = dispatcher.runSync('updateBudgetStatus', serviceCtx)

        List<GenericValue> budgetStatuses = from('BudgetStatus').where('budgetId', '9999').orderBy('-statusDate').queryList()
        assert ! budgetStatuses?.isEmpty()
        assert budgetStatuses[0].statusId == 'BG_APPROVED'
    }
}
