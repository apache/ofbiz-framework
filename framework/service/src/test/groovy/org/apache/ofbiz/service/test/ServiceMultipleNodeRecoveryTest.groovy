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
package org.apache.ofbiz.service.test

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.config.ServiceConfigUtil
import org.apache.ofbiz.service.job.JobManager
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

import java.sql.Timestamp

// ./gradlew "ofbiz --test component=service --test suitename=servicetests --test case=service-multiple-node-recovery"

@JunitJupiterTest
class ServiceMultipleNodeRecoveryTest implements JupiterTestHelper {

    private static final Timestamp LAST_HOUR = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.HOUR, -1)
    private static final String POOL_ID = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool()

    @Test
    @Order(1)
    void testMultipleNodeRecoveryAfterOneFail() {
        Timestamp now = UtilDateTime.nowTimestamp()
        long leaseExpiryMillis = ServiceConfigUtil.getServiceEngine().getThreadPool().getLeaseExpiryMillis()
        GenericValue sysUserLogin = delegator.findOne('UserLogin', true, 'userLoginId', 'system')
        String jobIdNodeFailed = 'NodeFailed'
        String jobIdNodeFailedLeaseNull = 'NodeFailedLeaseNull'
        String jobIdNodeRun = 'NodeRun'

        Map serviceResult = dispatcher.runSync('createRuntimeData', [
                runtimeInfo: 'This is a runtimeInfo',
                userLogin: sysUserLogin
        ])
        String runtimeDataId = serviceResult.runtimeDataId

        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdNodeFailed,
                runtimeDataId: runtimeDataId,
                runByInstanceId: 'ofbiz-failed',
                statusId: 'SERVICE_RUNNING',
                startDateTime: LAST_HOUR,
                leaseUpdatedStamp: new Timestamp(now.getTime() - leaseExpiryMillis - 10),
                serviceName: 'sendMail'])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdNodeFailedLeaseNull,
                runtimeDataId: runtimeDataId,
                runByInstanceId: 'ofbiz-failed-null',
                statusId: 'SERVICE_RUNNING',
                startDateTime: LAST_HOUR,
                leaseUpdatedStamp: null,
                serviceName: 'sendMail'])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdNodeRun,
                runtimeDataId: runtimeDataId,
                runByInstanceId: 'ofbiz-run',
                statusId: 'SERVICE_RUNNING',
                startDateTime: LAST_HOUR,
                leaseUpdatedStamp: new Timestamp(now.getTime() - 10),
                serviceName: 'sendMail'])

        JobManager jobManager = JobManager.getInstance(delegator, false)

        assert jobManager.recoverStaleJobs() == 2
        assert EntityQuery.use(delegator).from('JobSandbox')
                .where(jobId: jobIdNodeFailed,
                        runByInstanceId: null,
                        statusId: 'SERVICE_PENDING')
                .queryCount() == 1
        assert EntityQuery.use(delegator).from('JobSandbox')
                .where(jobId: jobIdNodeFailedLeaseNull,
                        runByInstanceId: null,
                        statusId: 'SERVICE_PENDING')
                .queryCount() == 1
        assert EntityQuery.use(delegator).from('JobSandbox')
                .where(jobId: jobIdNodeRun,
                        runByInstanceId: 'ofbiz-run',
                        statusId: 'SERVICE_RUNNING')
                .queryCount() == 1
    }

    @Test
    @Order(2)
    void testMultipleNodeRecoveryHearbeat() {
        Timestamp now = UtilDateTime.nowTimestamp()
        GenericValue sysUserLogin = delegator.findOne('UserLogin', true, 'userLoginId', 'system')
        String jobIdToUpdate1 = 'ToUpdate1'
        String jobIdToUpdate2 = 'ToUpdate2'
        String jobIdToUpdate3 = 'ToUpdate3'
        String jobIdNonUpdate1 = 'NonUpdate1'
        String jobIdNonUpdate2 = 'NonUpdate2'

        Map serviceResult = dispatcher.runSync('createRuntimeData', [
                runtimeInfo: 'This is a runtimeInfo',
                userLogin: sysUserLogin
        ])
        String runtimeDataId = serviceResult.runtimeDataId

        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdToUpdate1,
                runtimeDataId: runtimeDataId,
                runByInstanceId: JobManager.INSTANCE_ID,
                statusId: 'SERVICE_RUNNING',
                startDateTime: LAST_HOUR,
                serviceName: 'sendMail',
        ])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdToUpdate2,
                runtimeDataId: runtimeDataId,
                runByInstanceId: JobManager.INSTANCE_ID,
                statusId: 'SERVICE_RUNNING',
                startDateTime: LAST_HOUR,
                leaseUpdatedStamp: new Timestamp(now.getTime() - 10),
                serviceName: 'sendMail',
        ])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdToUpdate3,
                runtimeDataId: runtimeDataId,
                runByInstanceId: JobManager.INSTANCE_ID,
                statusId: 'SERVICE_QUEUED',
                startDateTime: LAST_HOUR,
                leaseUpdatedStamp: new Timestamp(now.getTime() - 10),
                serviceName: 'sendMail',
        ])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdNonUpdate1,
                runtimeDataId: runtimeDataId,
                runByInstanceId: JobManager.INSTANCE_ID,
                statusId: 'SERVICE_FINISHED',
                startDateTime: LAST_HOUR,
                finishDateTime: LAST_HOUR,
                leaseUpdatedStamp: LAST_HOUR,
                serviceName: 'sendMail',
        ])
        dispatcher.runSync('createJobSandbox', [
                userLogin: sysUserLogin,
                poolId: POOL_ID,
                jobId: jobIdNonUpdate2,
                runtimeDataId: runtimeDataId,
                statusId: 'SERVICE_PENDING',
                serviceName: 'sendMail',
        ])

        JobManager jobManager = JobManager.getInstance(delegator, false)
        jobManager.heartbeatRunningJobs()
        assert EntityQuery.use(delegator).from('JobSandbox')
                .where(new EntityConditionBuilder()
                        .AND {
                            IN(jobId: [jobIdToUpdate1, jobIdToUpdate2, jobIdToUpdate3])
                            GREATER_THAN(leaseUpdatedStamp: now)
                        })
                .queryCount() == 3
        assert EntityQuery.use(delegator).from('JobSandbox')
                .where(new EntityConditionBuilder()
                        .AND {
                            IN(jobId: [jobIdNonUpdate1, jobIdNonUpdate2])
                            GREATER_THAN(leaseUpdatedStamp: now)
                        })
                .queryCount() == 0
    }

}
