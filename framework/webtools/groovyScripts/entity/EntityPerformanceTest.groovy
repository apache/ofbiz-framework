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

import java.text.DecimalFormat
import java.util.*
import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.security.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.*

DecimalFormat decimalFormat = new DecimalFormat("#,##0.#######")

if (security.hasPermission("ENTITY_MAINT", session)) {
    performanceList = []

    calls = 1000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = from("JobSandbox").where("jobId", "PURGE_OLD_JOBS").queryOne();
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime/1000)

    perfRow = [:]
    perfRow.operation = "findOne(false)"
    perfRow.entity = "Large:JobSandbox"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 10000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = from("JobSandbox").where("jobId", "PURGE_OLD_JOBS").cache().queryOne();
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "findOne(true)"
    perfRow.entity = "Large:JobSandbox"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 1000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = from("DataSourceType").where("dataSourceTypeId", "ADMIN_ENTRY").queryOne();
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "findOne(false)"
    perfRow.entity = "Small:DataSourceType"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 10000
    startTime = System.currentTimeMillis()
    for (int i=0; i < calls; i++) {
        dummy = from("DataSourceType").where("dataSourceTypeId", "ADMIN_ENTRY").cache().queryOne()
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "findOne(true)"
    perfRow.entity = "Small:DataSourceType"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    createTestList = []
    calls = 1000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = delegator.makeValue("JobSandbox", [poolId : "pool", jobName : "Initial Name" + i, serviceName : "foo", statusId : "SERVICE_FINISHED", jobId : "_~WRITE_TEST~_" + i])
        createTestList.add(dummy)
        delegator.create(dummy)
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "create"
    perfRow.entity = "Large:JobSandbox"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 1000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = createTestList.get(i)
        dummy.jobName = "This was a test from the performance groovy script"
        dummy.store()
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "update"
    perfRow.entity = "Large:JobSandbox"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 1000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        dummy = createTestList.get(i)
        dummy.remove()
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = (double) calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "remove"
    perfRow.entity = "Large:JobSandbox"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 100000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        ptyMap = [:]
        ptyMap.dataSourceTypeId = "ADMIN_ENTRY"
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "new HashMap"
    perfRow.entity = "N/A"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 100000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        ptyMap = UtilMisc.toMap("dataSourceTypeId", "ADMIN_ENTRY")
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "UtilMisc.toMap"
    perfRow.entity = "N/A"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    utilCache = new UtilCache("test-cache", 0, 0, 0, false, null, "test-cache")
    utilCache.put("testName", "testValue")
    calls = 1000000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        utilCache.get("testName")
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "UtilCache.get(String) - basic settings"
    perfRow.entity = "N/A"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    testPk = delegator.makePK("DataSourceType", [dataSourceTypeId : "ADMIN_ENTRY"])
    utilCache.put(testPk, "testValue")
    calls = 1000000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        utilCache.get(testPk)
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime/1000)

    perfRow = [:]
    perfRow.operation = "UtilCache.get(GenericPK) - basic settings"
    perfRow.entity = "N/A"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    calls = 1000000
    startTime = System.currentTimeMillis()
    for (int i = 0; i < calls; i++) {
        utilCache.put(testPk, "testValue")
    }
    totalTime = System.currentTimeMillis() - startTime
    callsPerSecond = calls / (totalTime / 1000)

    perfRow = [:]
    perfRow.operation = "UtilCache.put(GenericPK) - basic settings"
    perfRow.entity = "N/A"
    perfRow.calls = decimalFormat.format(calls)
    perfRow.seconds = decimalFormat.format(totalTime / 1000)
    perfRow.secsPerCall = decimalFormat.format(1 / callsPerSecond)
    perfRow.callsPerSecond = decimalFormat.format(callsPerSecond)
    performanceList.add(perfRow)

    context.performanceList = performanceList
}
