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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgBudgetTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateBudget() {
        String budgetTypeId = testParams.budgetTypeId ?: 'CAPITAL_BUDGET'
        String comments = testParams.comments ?: 'Capital Budget'
        Map serviceCtx = [:]
        serviceCtx.budgetTypeId = budgetTypeId
        serviceCtx.comments = comments
        serviceCtx.userLogin = userLogin
        Map result = dispatcher.runSync('createBudget', serviceCtx)
        assert ServiceUtil.isSuccess(result)

        GenericValue budget = from('Budget').where(result).queryOne()
        assert budget
        assert budget.budgetTypeId == budgetTypeId
        assert budget.comments == comments
    }

    @Test
    @Order(2)
    void testUpdateBudgetStatus() {
        String budgetId = testParams.budgetId ?: '9999'
        String statusId = testParams.statusId ?: 'BG_APPROVED'
        Map serviceCtx = [:]
        serviceCtx.budgetId = budgetId
        serviceCtx.statusId = statusId
        serviceCtx.userLogin = userLogin
        dispatcher.runSync('updateBudgetStatus', serviceCtx)

        List<GenericValue> budgetStatuses = from('BudgetStatus').where('budgetId', budgetId).orderBy('-statusDate').queryList()
        assert ! budgetStatuses?.isEmpty()
        assert budgetStatuses[0].statusId == statusId
    }

}
