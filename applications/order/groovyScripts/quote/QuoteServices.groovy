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


import org.apache.ofbiz.service.ExecutionServiceException
import org.apache.ofbiz.service.ServiceUtil

/**
 * Ensures that a workEffort exist and create a QuoteWorkEffort.
 */
def ensureWorkEffortAndCreateQuoteWorkEffort() {
    String workEffortId = parameters.workEffortId
    if (!workEffortId) {
        Map serviceResult = run service: 'createWorkEffort', with: parameters
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        workEffortId = serviceResult.workEffortId
    }
    Map createQuoteWorkEffortInMap = [quoteId: parameters.quoteId, workEffortId: workEffortId]
    Map serviceResult
    try {
        serviceResult = run service: 'createQuoteWorkEffort', with: createQuoteWorkEffortInMap
    } catch (ExecutionServiceException e) {
        serviceResult = ServiceUtil.returnError(e.toString())
    }
    serviceResult.workEffortId = workEffortId
    return serviceResult
}
