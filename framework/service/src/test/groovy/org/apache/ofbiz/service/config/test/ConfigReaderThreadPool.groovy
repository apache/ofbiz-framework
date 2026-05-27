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
package org.apache.ofbiz.service.config.test

import org.apache.ofbiz.service.config.model.ServiceEngine
import org.junit.Test

class ConfigReaderThreadPool extends BaseServiceConfigReaderTest {

    @Test
    void testThreadPoolFromXml() {
        ServiceEngine engine = getConfig('''
        <thread-pool send-to-pool="pool-test"
                     purge-job-days="10"
                     failed-retry-min="11"
                     ttl="12"
                     jobs="13"
                     min-threads="14"
                     max-threads="15"
                     poll-enabled="true"
                     poll-db-millis="16">
            <run-from-pool name="pool-test1"/>
            <run-from-pool name="pool-test2"/>
        </thread-pool>''', '')
        engine.getThreadPool().with {
            assert getSendToPool() == 'pool-test'
            assert getPurgeJobDays() == 10
            assert getFailedRetryMin() == 11
            assert getTtl() == 12
            assert getJobs() == 13
            assert getMinThreads() == 14
            assert getMaxThreads() == 15
            assert getPollEnabled()
            assert getPollDbMillis() == 16
            List runFroms = getRunFromPools()
            assert runFroms.size() == 2
            assert runFroms.find { it.getName() == 'pool-test1' }
            assert runFroms.find { it.getName() == 'pool-test2' }
        }
    }

    @Test
    void testThreadPoolWithOverride() {
        ServiceEngine engine = getConfig('''
        <thread-pool send-to-pool="pool-test"
                     purge-job-days="10"
                     failed-retry-min="11"
                     ttl="12"
                     jobs="13"
                     min-threads="14"
                     max-threads="15"
                     poll-enabled="true"
                     poll-db-millis="16">
            <run-from-pool name="pool-test1"/>
            <run-from-pool name="pool-test2"/>
        </thread-pool>''', '''"thread-pool": {
                     "send-to-pool": "pool-over",
                     "purge-job-days": "910",
                     "failed-retry-min": "911",
                     "ttl": "912",
                     "jobs": "913",
                     "min-threads": "914",
                     "max-threads": "915",
                     "poll-enabled": "false",
                     "poll-db-millis": "916"
                     "run-from-pool": [{ "name": "pool-over" }]
                      }''')
        engine.getThreadPool().with {
            assert getSendToPool() == 'pool-over'
            assert getPurgeJobDays() == 910
            assert getFailedRetryMin() == 911
            assert getTtl() == 912
            assert getJobs() == 913
            assert getMinThreads() == 914
            assert getMaxThreads() == 915
            assert !getPollEnabled()
            assert getPollDbMillis() == 916

            List runFroms = getRunFromPools()
            assert runFroms.size() == 1
            assert runFroms.find { it.getName() == 'pool-over' }
        }
    }

}
