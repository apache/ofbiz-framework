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
package org.apache.ofbiz.testtools;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable snapshot of one runTestSuite-triggered run's state, held by {@link TestRunTracker}.
 * Each transition method returns a new instance rather than mutating this one, so a
 * ConcurrentHashMap.put() of the new instance is always a safe, atomic state change - no
 * synchronized block needed on the record itself.
 */
final class TestRunRecord {

    enum Status { QUEUED, RUNNING, PASSED, FAILED, ERROR }

    private final String runId;
    private final String suiteName;
    private final String componentName;
    private final Status status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String triggeredBy;
    private final Map<String, Object> paramsUsed;
    private final Map<String, Object> resultSummary;
    private final String errorMessage;

    private TestRunRecord(String runId, String suiteName, String componentName, Status status, Instant startedAt,
            Instant completedAt, String triggeredBy, Map<String, Object> paramsUsed, Map<String, Object> resultSummary,
            String errorMessage) {
        this.runId = runId;
        this.suiteName = suiteName;
        this.componentName = componentName;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.triggeredBy = triggeredBy;
        this.paramsUsed = paramsUsed;
        this.resultSummary = resultSummary;
        this.errorMessage = errorMessage;
    }

    static TestRunRecord queued(String runId, String suiteName, String componentName, String triggeredBy,
            Map<String, Object> paramsUsed) {
        return new TestRunRecord(runId, suiteName, componentName, Status.QUEUED, Instant.now(), null, triggeredBy,
                paramsUsed, null, null);
    }

    TestRunRecord running() {
        return new TestRunRecord(runId, suiteName, componentName, Status.RUNNING, startedAt, null, triggeredBy,
                paramsUsed, null, null);
    }

    TestRunRecord passed(Map<String, Object> resultSummary) {
        return new TestRunRecord(runId, suiteName, componentName, Status.PASSED, startedAt, Instant.now(), triggeredBy,
                paramsUsed, resultSummary, null);
    }

    TestRunRecord failed(Map<String, Object> resultSummary) {
        return new TestRunRecord(runId, suiteName, componentName, Status.FAILED, startedAt, Instant.now(), triggeredBy,
                paramsUsed, resultSummary, null);
    }

    TestRunRecord error(Throwable throwable) {
        return new TestRunRecord(runId, suiteName, componentName, Status.ERROR, startedAt, Instant.now(), triggeredBy,
                paramsUsed, null, throwable.getMessage());
    }

    String runId() {
        return runId;
    }

    String suiteName() {
        return suiteName;
    }

    String componentName() {
        return componentName;
    }

    Status status() {
        return status;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant completedAt() {
        return completedAt;
    }

    String triggeredBy() {
        return triggeredBy;
    }

    Map<String, Object> paramsUsed() {
        return paramsUsed;
    }

    Map<String, Object> resultSummary() {
        return resultSummary;
    }

    String errorMessage() {
        return errorMessage;
    }
}
