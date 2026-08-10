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

import org.junit.jupiter.api.Test;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Exercises reportSuiteExecutionFailure() directly rather than through start()'s for loop, which
 * needs a full ofbiz --test container bootstrap (StartupCommand, a real ModelTestSuite/Delegator,
 * ...) to construct. The loop's own try/catch wiring around suite.run(results) is a single,
 * low-risk control-flow change; this test covers the part that actually has behavior worth
 * verifying - that an escaped exception is turned into a proper suite-level error report instead
 * of a silently-empty, still-"successful" TestResult.
 */
class TestRunContainerTest {

    @Test
    void escapedExceptionIsReportedAsASuiteLevelError() {
        TestSuite suite = new TestSuite("myFakeSuite");
        TestResult results = new TestResult();
        RuntimeException thrown = new RuntimeException("boom");

        TestRunContainer.reportSuiteExecutionFailure(suite, results, thrown);

        // Without this reporting, an exception escaping suite.run() itself (a JUnitException from
        // Jupiter's launcher.execute(), for example) would leave `results` with zero tests recorded
        // and wasSuccessful() still true - the same false-positive "all green" class of bug as a
        // silently-discarded container failure.
        assertThat(results.runCount(), is(1));
        assertThat(results.errorCount(), is(1));
        assertThat(results.wasSuccessful(), is(false));
        assertThat(results.errors().nextElement().thrownException(), is(thrown));
    }

    @Test
    void syntheticFailureMarkerIsNamedAfterTheSuite() {
        TestSuite suite = new TestSuite("myFakeSuite");
        TestResult results = new TestResult();

        TestRunContainer.reportSuiteExecutionFailure(suite, results, new RuntimeException("boom"));

        assertThat(results.errors().nextElement().failedTest().toString(), containsString("myFakeSuite.suiteExecutionError"));
    }
}
