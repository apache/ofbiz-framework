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

import org.apache.ofbiz.entity.GenericValue


def createBudget() {
    //create new entity and create all the fields
    GenericValue newEntity = makeValue('Budget', parameters)

    //create a non existing ID if not supplied
    newEntity.budgetId = delegator.getNextSeqId('Budget', 1)

    //finally create the record (should not exist already)
    newEntity.create()

    Map setStatusMap = ['budgetId': newEntity.budgetId]
    setStatusMap.statusId = 'BG_CREATED'
    Map result = run service: 'updateBudgetStatus', with: setStatusMap
    result.budgetId = newEntity.budgetId
    return result
}

def updateBudgetStatus() {
    Map result = success()
    List budgetStatuses = from('BudgetStatus').where([budgetId: parameters.budgetId]).orderBy('-statusDate').queryList()
    GenericValue statusValidChange = null
    if (budgetStatuses) {
        budgetStatus = budgetStatuses[0]
        statusValidChange = from('StatusValidChange').where([statusId: budgetStatus.statusId, statusIdTo: parameters.statusId]).queryOne()
    }
    if (! budgetStatuses || budgetStatuses && statusValidChange) {
        result = run service: 'createBudgetStatus', with: parameters
    }
    return result
}
