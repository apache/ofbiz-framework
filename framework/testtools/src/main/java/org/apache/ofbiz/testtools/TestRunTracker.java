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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of runTestSuite-triggered runs, for getTestRunStatus polling. Deliberately
 * not persisted - does not survive a server restart. A completed run is also archived into the
 * durable runtime/test-reports/manifest.json history separately (see TestRunServices); this
 * tracker exists only to answer "is it done yet" while a run is in flight or shortly after.
 */
final class TestRunTracker {

    private final Map<String, TestRunRecord> records = new ConcurrentHashMap<>();

    TestRunRecord register(String runId, String suiteName, String componentName, String triggeredBy,
            Map<String, Object> paramsUsed) {
        // Defensively copies the caller's map: this provides a defense-in-depth isolation layer
        // for the tracker's own stored record. The primary caller (TestRunServices.runTestSuite)
        // already normalizes to an immutable, null-tolerant copy before passing here, protecting
        // both the downstream executor/test (which receives the normalized map as finalTestParams
        // and arms it into JupiterTestExtension.CURRENT_TEST_PARAMS) and the archiver. Using
        // LinkedHashMap+unmodifiableMap instead of Map.copyOf ensures compatibility with callers
        // that may have null param values (Map.copyOf throws NullPointerException on null).
        TestRunRecord record = TestRunRecord.queued(runId, suiteName, componentName, triggeredBy,
                Collections.unmodifiableMap(new LinkedHashMap<>(paramsUsed)));
        records.put(runId, record);
        return record;
    }

    void markRunning(String runId) {
        records.computeIfPresent(runId, (id, record) -> record.running());
    }

    void markPassed(String runId, Map<String, Object> resultSummary) {
        records.computeIfPresent(runId, (id, record) -> record.passed(resultSummary));
    }

    void markFailed(String runId, Map<String, Object> resultSummary) {
        records.computeIfPresent(runId, (id, record) -> record.failed(resultSummary));
    }

    void markError(String runId, Throwable throwable) {
        records.computeIfPresent(runId, (id, record) -> record.error(throwable));
    }

    TestRunRecord get(String runId) {
        return records.get(runId);
    }
}
