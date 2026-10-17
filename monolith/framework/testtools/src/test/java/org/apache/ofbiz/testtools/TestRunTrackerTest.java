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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class TestRunTrackerTest {

    @Test
    void registerStartsARunInQueuedState() {
        TestRunTracker tracker = new TestRunTracker();

        tracker.register("run-1", "example-tests", "example", "system", Map.of("exampleName", "custom"));

        TestRunRecord record = tracker.get("run-1");
        assertThat(record, notNullValue());
        assertThat(record.status(), is(TestRunRecord.Status.QUEUED));
        assertThat(record.suiteName(), is("example-tests"));
        assertThat(record.componentName(), is("example"));
        assertThat(record.triggeredBy(), is("system"));
        assertThat(record.paramsUsed(), is(Map.of("exampleName", "custom")));
    }

    @Test
    void markRunningTransitionsFromQueuedToRunning() {
        TestRunTracker tracker = new TestRunTracker();
        tracker.register("run-1", "example-tests", "example", "system", Map.of());

        tracker.markRunning("run-1");

        assertThat(tracker.get("run-1").status(), is(TestRunRecord.Status.RUNNING));
    }

    @Test
    void markPassedRecordsTerminalStateAndSummary() {
        TestRunTracker tracker = new TestRunTracker();
        tracker.register("run-1", "example-tests", "example", "system", Map.of());
        tracker.markRunning("run-1");

        tracker.markPassed("run-1", Map.of("total", 3, "passed", 3, "failed", 0));

        TestRunRecord record = tracker.get("run-1");
        assertThat(record.status(), is(TestRunRecord.Status.PASSED));
        assertThat(record.resultSummary(), is(Map.of("total", 3, "passed", 3, "failed", 0)));
        assertThat(record.completedAt(), notNullValue());
    }

    @Test
    void markFailedRecordsTerminalStateAndSummary() {
        TestRunTracker tracker = new TestRunTracker();
        tracker.register("run-1", "example-tests", "example", "system", Map.of());

        tracker.markFailed("run-1", Map.of("total", 3, "passed", 2, "failed", 1));

        assertThat(tracker.get("run-1").status(), is(TestRunRecord.Status.FAILED));
    }

    @Test
    void markErrorRecordsTerminalStateWithNoSummary() {
        TestRunTracker tracker = new TestRunTracker();
        tracker.register("run-1", "example-tests", "example", "system", Map.of());

        tracker.markError("run-1", new RuntimeException("suite blew up"));

        TestRunRecord record = tracker.get("run-1");
        assertThat(record.status(), is(TestRunRecord.Status.ERROR));
        assertThat(record.errorMessage(), is("suite blew up"));
    }

    @Test
    void getReturnsNullForAnUnknownRunId() {
        TestRunTracker tracker = new TestRunTracker();

        assertThat(tracker.get("no-such-run"), nullValue());
    }

    @Test
    void registerHandlesNullValuesInParamsWithoutThrowingNpe() {
        TestRunTracker tracker = new TestRunTracker();
        Map<String, Object> callerParams = new HashMap<>();
        callerParams.put("exampleName", "value");
        callerParams.put("nullParam", null);

        tracker.register("run-1", "example-tests", "example", "system", callerParams);

        TestRunRecord record = tracker.get("run-1");
        assertThat(record, notNullValue());
        assertThat(record.paramsUsed().get("exampleName"), is("value"));
        assertThat(record.paramsUsed().get("nullParam"), nullValue());
    }

    @Test
    void registerDefensivelyCopiesTheCallersParamsMap() {
        TestRunTracker tracker = new TestRunTracker();
        Map<String, Object> callerParams = new HashMap<>();
        callerParams.put("exampleName", "original");

        tracker.register("run-1", "example-tests", "example", "system", callerParams);
        callerParams.put("exampleName", "mutated-after-register");
        callerParams.put("extraKey", "should-not-appear");

        assertThat(tracker.get("run-1").paramsUsed(), is(Map.of("exampleName", "original")));
    }

    @Test
    void componentNameSurvivesStatusTransitions() {
        TestRunTracker tracker = new TestRunTracker();
        tracker.register("run-1", "example-tests", "example", "system", Map.of());

        tracker.markRunning("run-1");
        tracker.markPassed("run-1", Map.of());

        assertThat(tracker.get("run-1").componentName(), is("example"));
    }
}
