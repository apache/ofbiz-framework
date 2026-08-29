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
package org.apache.ofbizservice.test

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.service.ModelService

Map testTopLevelServiceThatPlansTrackedServices() {
    3.times {
        dispatcher.runAsyncTracked('ping', context.jobTrackerId, [:])
    }
    return success([jobTrackerId: context.jobTrackerId])
}

Map serviceForTestingTracker() {
    20.times {
        dispatcher.runAsyncTracked('ServiceThatWaitsAMoment', context.jobTrackerId, [userLogin: context.userLogin])
    }
    return success()
}

Map serviceThatWaitsAMoment() {
    sleep(2 * 1000) // 10s
    return success([(ModelService.SUCCESS_MESSAGE): "Message at ${UtilDateTime.nowDateString()}"])
}
