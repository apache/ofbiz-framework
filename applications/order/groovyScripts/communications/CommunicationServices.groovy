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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.service.ServiceUtil

/**
 * Create a new order conversation
 */
def createOrderConversation() {
    Map<String, Object> createCommunicationEventMap = dispatcher.getDispatchContext()
            .makeValidContext('createCommunicationEvent', 'IN', parameters)
    createCommunicationEventMap.entryDate = UtilDateTime.nowTimestamp()
    createCommunicationEventMap.statusId = "COM_ENTERED"
    def result = dispatcher.runSync('createCommunicationEvent', createCommunicationEventMap)
    if (ServiceUtil.isError(result)) return result

    if (parameters.communicationEventPrpTypId) {
        Map<String, Object> createCommunicationEventPurposeMap = dispatcher.getDispatchContext()
                .makeValidContext('createCommunicationEventPurpose', 'IN', parameters)
        createCommunicationEventPurposeMap.communicationEventId = result.communicationEventId
        return dispatcher.runSync('createCommunicationEventPurpose', createCommunicationEventPurposeMap);
    }
}