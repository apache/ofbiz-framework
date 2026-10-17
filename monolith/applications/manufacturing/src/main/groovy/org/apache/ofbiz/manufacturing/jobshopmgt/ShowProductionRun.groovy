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
package org.apache.ofbiz.manufacturing.jobshopmgt

// The only required parameter is "productionRunId".

import org.apache.ofbiz.entity.GenericValue

delegator = request.getAttribute('delegator')

productionRunId = request.getParameter('productionRunId') ?: request.getParameter('workEffortId')
if (productionRunId) {
    GenericValue productionRun = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
    if (productionRun) {
        // If this is a task, get the parent production run
        if (productionRun.getString('workEffortTypeId') != null && productionRun.getString('workEffortTypeId') == 'PROD_ORDER_TASK') {
            productionRun = from('WorkEffort').where('workEffortId', productionRun.getString('workEffortParentId')).queryOne()
        }
    }

    if (!productionRun) {
        return 'error'
    }
    return ['PRUN_CREATED', 'PRUN_SCHEDULED', 'PRUN_CANCELLED'].contains(productionRun.currentStatusId) ? 'docs_not_printed' : 'docs_printed'
}

return 'error'
