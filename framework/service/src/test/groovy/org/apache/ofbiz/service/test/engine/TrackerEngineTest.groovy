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
package org.apache.ofbiz.service.test.engine

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.tracker.JobTracker
import org.apache.ofbiz.service.tracker.JobTrackerFactory
import org.apache.ofbiz.service.tracker.JobTrackerListener
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * First time : ./gradlew 'ofbiz  -l readers=seed,seed-initial -l delegator=test'
 * ./gradlew 'ofbiz -t component=service -t suitename=servicetests -t case=engine-tracker-tests'
 */
@JunitJupiterTest
class TrackerEngineTest implements JupiterTestHelper {

    @Test
    void testTrackedServiceAreTracked() {
        GenericValue sysUser = delegator.findOne('UserLogin', true, 'userLoginId', 'system')
        Map result = dispatcher.runSync('TestTopLevelServiceThatPlansTrackedServices', [userLogin: sysUser])
        assert ServiceUtil.isSuccess(result)
        String trackerId = result.jobTrackerId
        JobTracker tracker = JobTrackerFactory.getJobTracker(dispatcher, trackerId)
        assert tracker.getGenericValue().jobsTotalQty == 3
        JobTrackerListener listener = new JobTrackerListener(delegator, tracker)
        assert listener.state()
    }

}
